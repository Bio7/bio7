/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rffm;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.eco.bio7.rffm.graphics.Java2DGraphicsDevice;
import com.eco.bio7.rffm.graphics.RDeviceFactory;
import com.eco.bio7.rffm.graphics.RGraphicsDevice;
import com.eco.bio7.rffm.internal.Convert;
import com.eco.bio7.rffm.internal.DeviceBridge;
import com.eco.bio7.rffm.internal.RCore;
import com.eco.bio7.rffm.internal.RNative;

/**
 * R embedded in the Java process with the Foreign Function and Memory API (no
 * no compiled native code): an existing installation of R (built as a
 * shared library) is loaded.
 * <p>
 * R runs in its own thread; all methods can be called from any thread and wait
 * for R (calls from the thread of R, e.g., in a device callback, run directly).
 * R is single-threaded: while R evaluates (or waits for console input in
 * readline()), the calls wait.
 * <p>
 * Limits of embedding: one R per process, which can't be started again after
 * {@link #close()}; a crash of R (e.g., of a package) ends the process. R errors
 * are always caught (R_tryEval), so they don't reach Java.
 */
public final class RSession implements AutoCloseable {

	private static volatile RSession current;

	private final RStartOptions options;
	private final LinkedBlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();
	/* A console line and its number (a capture waits for the prompt after its own line) */
	private record ConsoleLine(String text, long seq) {
	}

	private final LinkedBlockingQueue<ConsoleLine> consoleInput = new LinkedBlockingQueue<>();
	private final StringBuilder pendingInput = new StringBuilder();
	private final Map<String, MemorySegment> helpers = new HashMap<>();
	private MemorySegment helperEnv;
	private final Map<Integer, RDeviceFactory> deviceFactories = new HashMap<>();
	private final Map<Integer, String> deviceNames = new HashMap<>();
	private volatile RConsole console;
	private volatile boolean alive;
	private volatile boolean busy;
	private volatile boolean readingConsole;
	private volatile boolean interruptRequested;
	private Thread thread;
	private RNative n;
	private RCore c;
	private Convert convert;
	private DeviceBridge devices;
	private MemorySegment deviceCallPointer;
	private MemorySegment javaCallPointer;
	private final Map<Integer, RFunction> functions = new java.util.concurrent.ConcurrentHashMap<>();
	private String version;

	private RSession(RStartOptions options) {
		this.options = options;
		this.console = options.console();
	}

	/**
	 * Starts R (once per process).
	 *
	 * @throws RException if R can't be loaded or started, or R was started before
	 */
	public static synchronized RSession start(RStartOptions options) throws RException {
		if (current != null) {
			throw new RException(current.alive ? "R already runs in this process" : "R was stopped and can't be started again in this process");
		}
		RSession s = new RSession(options);
		CompletableFuture<Void> started = new CompletableFuture<>();
		/* A large stack: R recurses deeply (the stack check of R is off) */
		s.thread = new Thread(null, () -> s.run(started), "R (embedded)", 256L << 20);
		s.thread.setDaemon(true);
		s.thread.start();
		try {
			started.get(120, TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			throw new RException("R could not be started: " + e.getCause().getMessage(), e.getCause());
		} catch (TimeoutException e) {
			throw new RException("R did not start within 120 seconds");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RException("interrupted");
		}
		current = s;
		return s;
	}

	/** The session of this process or null. */
	public static RSession current() {
		return current;
	}

	/** Returns true if a session was started in this process (also if it was closed). */
	public static boolean wasStarted() {
		return current != null;
	}

	/** Returns true if R runs. */
	public boolean isAlive() {
		return alive;
	}

	/** Returns true if R waits for console input within an evaluation (browser(), readline()). */
	public boolean isWaitingForConsoleInput() {
		return readingConsole;
	}

	/** Returns true if R evaluates console input (or console input is queued). */
	public boolean isBusy() {
		return busy;
	}

	/** Returns true if the current thread is the thread of R. */
	public boolean isRThread() {
		return Thread.currentThread() == thread;
	}

	/** The version of R (e.g., 4.6.0). */
	public String version() {
		return version;
	}

	/** Replaces the console. */
	public void setConsole(RConsole console) {
		this.console = console == null ? RConsole.NONE : console;
	}

	/* ---------------- The thread of R ---------------- */

	private void run(CompletableFuture<Void> started) {
		try {
			n = RNative.load(options.home());
			c = new RCore(n);
			if (n.os.equals("Windows")) {
				startWindows();
			} else {
				startUnix();
			}
			c.initConstants();
			devices = new DeviceBridge(c);
			defineHelpers();
			convert = new Convert(c, this::alloc);
			MemorySegment v = evalOnRThread("as.character(getRversion())");
			version = ((RValue.Str) convert.toJava(v)).values()[0];
			c.unprotect(1);
			alive = true;
			started.complete(null);
		} catch (Throwable t) {
			started.completeExceptionally(t);
			return;
		}
		console.prompt("> ");
		while (alive) {
			try {
				Runnable task = tasks.poll(100, TimeUnit.MILLISECONDS);
				if (task instanceof Wake) {
					/* A wake-up of a finished call of Java: no work (and no reset of an interrupt) */
					continue;
				}
				if (task != null) {
					interruptRequested = false;
					/*
					 * An interrupt R didn't handle in the previous evaluation (e.g., requested
					 * when it had just ended) must not cancel this one (a plot would be lost)
					 */
					clearPendingInterrupt();
					task.run();
				} else if (options.processEvents()) {
					idle();
				}
			} catch (InterruptedException e) {
				/* Continue until closed */
			} catch (Throwable t) {
				console.write("Embedded R: " + t + "\n", true);
			}
		}
	}

	private MemorySegment[] argv(Arena arena, List<String> args) {
		MemorySegment array = arena.allocate(ADDRESS, args.size() + 1);
		for (int i = 0; i < args.size(); i++) {
			array.setAtIndex(ADDRESS, i, arena.allocateFrom(args.get(i)));
		}
		return new MemorySegment[] { array };
	}

	private List<String> arguments() {
		List<String> args = new ArrayList<>(List.of("R", "--no-save", "--no-restore", "--quiet", "--no-readline"));
		if (options.interactive() && !n.os.equals("Windows")) {
			args.add("--interactive");
		}
		args.addAll(options.arguments());
		return args;
	}

	private void startUnix() throws Throwable {
		Arena arena = n.arena;
		/* The JVM needs its own signal handlers (before R is initialized) */
		n.setInt("R_SignalHandlers", 0);
		List<String> args = arguments();
		MemorySegment av = argv(arena, args)[0];
		var init = RNative.LINKER.downcallHandle(n.symbolAddress("Rf_initialize_R"), FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS));
		int unused = (int) init.invokeExact(args.size(), av);
		/*
		 * The waits of R (e.g., Sys.sleep) wake up every 100 ms: an interrupt
		 * (R_interrupts_pending, set without a signal) is handled then. Without it
		 * R_checkActivity waits the whole time (Sys.sleep(30) couldn't be interrupted).
		 */
		n.setInt("R_wait_usec", 100_000);
		/* The console of R through the callbacks */
		n.setPointer("R_Outputfile", MemorySegment.NULL);
		n.setPointer("R_Consolefile", MemorySegment.NULL);
		n.setPointer("ptr_R_WriteConsole", MemorySegment.NULL);
		n.setPointer("ptr_R_WriteConsoleEx", stub("writeConsoleEx", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT)));
		n.setPointer("ptr_R_ReadConsole", stub("readConsole", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT)));
		n.setPointer("ptr_R_ShowMessage", stub("showMessage", FunctionDescriptor.ofVoid(ADDRESS)));
		n.setPointer("ptr_R_Busy", stub("busyCallback", FunctionDescriptor.ofVoid(JAVA_INT)));
		n.setInt("R_Interactive", options.interactive() ? 1 : 0);
		/* The stack of a Java thread is unknown to R */
		n.setLong("R_CStackLimit", -1L);
		n.numericLocaleC();
		var setup = RNative.LINKER.downcallHandle(n.symbolAddress("setup_Rmainloop"), FunctionDescriptor.ofVoid());
		setup.invokeExact();
	}

	/* The committed stack of the thread of R (Windows): its upper end and size, 0 if not committed */
	private long stackHigh, stackCommitted;

	/* The part of the stack of the thread of R which is committed at the start (as the stack of Rterm) */
	private static final long R_STACK = 64L << 20;

	/*
	 * Windows: commits the upper part of the stack of the thread of R. Windows
	 * commits a stack page by page when the guard page below the used stack is
	 * touched; native code (R) probes its stack page by page. The upcall stubs of
	 * the JVM (R -> Java: console output, busy, functions) probe 32 KB below the
	 * stack pointer at once: deep in R (e.g., knitr/evaluate with rlang) that
	 * address was reserved but not committed, an access violation which ended the
	 * process. Committed, the probes are always valid; R gets the limit of this
	 * region (R_CStackLimit) and stops a too deep recursion with an R error.
	 */
	private void commitWindowsStack() {
		try {
			SymbolLookup k32 = SymbolLookup.libraryLookup("kernel32", Arena.global());
			var limits = RNative.LINKER.downcallHandle(k32.find("GetCurrentThreadStackLimits").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
			var virtualAlloc = RNative.LINKER.downcallHandle(k32.find("VirtualAlloc").orElseThrow(),
					FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_LONG, JAVA_INT, JAVA_INT));
			try (Arena a = Arena.ofConfined()) {
				MemorySegment lo = a.allocate(JAVA_LONG);
				MemorySegment hi = a.allocate(JAVA_LONG);
				limits.invokeExact(lo, hi);
				long low = lo.get(JAVA_LONG, 0);
				long high = hi.get(JAVA_LONG, 0);
				/* The bottom of the reservation stays as it is (the guard zones of the JVM) */
				long size = Math.min(R_STACK, high - low - (8L << 20)) & ~0xFFFFL;
				if (size <= 0) {
					return;
				}
				MemorySegment r = (MemorySegment) virtualAlloc.invokeExact(MemorySegment.ofAddress(high - size), size, 0x1000 /* MEM_COMMIT */, 0x04 /* PAGE_READWRITE */);
				if (r.address() == 0) {
					return;
				}
				/*
				 * The former guard page in the region: read-write (touched later it would set
				 * the stack limit of the thread back up)
				 */
				var virtualProtect = RNative.LINKER.downcallHandle(k32.find("VirtualProtect").orElseThrow(),
						FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, JAVA_INT, ADDRESS));
				MemorySegment old = a.allocate(JAVA_INT);
				int protectedOk = (int) virtualProtect.invokeExact(MemorySegment.ofAddress(high - size), size, 0x04 /* PAGE_READWRITE */, old);
				/*
				 * The stack limit of the thread (TEB NtTib.StackLimit): the unwinding of
				 * longjmp (RtlUnwind, used by R for errors) checks the frames against it.
				 * Windows only moves it when the guard page is touched, which doesn't happen in
				 * a committed region: without it an R error ended the process (STATUS_BAD_STACK).
				 */
				MemorySegment teb = currentTeb();
				if (protectedOk == 0 || teb == null) {
					return;
				}
				teb.reinterpret(0x18).set(JAVA_LONG, 0x10, high - size);
				stackHigh = high;
				stackCommitted = size;
			}
		} catch (Throwable t) {
			/* The stack grows as before (the stack check of R stays off) */
		}
	}

	/* The TEB of the current thread (Windows, NtQueryInformationThread: ThreadBasicInformation), null if unknown */
	private static MemorySegment currentTeb() {
		try {
			SymbolLookup ntdll = SymbolLookup.libraryLookup("ntdll", Arena.global());
			var query = RNative.LINKER.downcallHandle(ntdll.find("NtQueryInformationThread").orElseThrow(),
					FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
			try (Arena a = Arena.ofConfined()) {
				/* THREAD_BASIC_INFORMATION: ExitStatus, TebBaseAddress at 8, ClientId, AffinityMask, priorities (48 bytes) */
				MemorySegment info = a.allocate(48, 8);
				/* GetCurrentThread(): the pseudo handle -2 */
				int status = (int) query.invokeExact(MemorySegment.ofAddress(-2L), 0, info, 48, MemorySegment.NULL);
				long teb = info.get(JAVA_LONG, 8);
				return status == 0 && teb != 0 ? MemorySegment.ofAddress(teb) : null;
			}
		} catch (Throwable t) {
			return null;
		}
	}

	/* The stack check of R for the committed stack (else off: the stack of a Java thread is unknown to R) */
	private void setRStackLimit() {
		if (!n.has("R_CStackLimit")) {
			return;
		}
		if (stackCommitted > 0 && n.has("R_CStackStart") && n.has("R_CStackDir")) {
			n.setLong("R_CStackStart", stackHigh);
			/* The stack grows down */
			n.setInt("R_CStackDir", 1);
			/* A margin for native code between the checks of R and for the probes of the JVM */
			n.setLong("R_CStackLimit", stackCommitted - (4L << 20));
		} else {
			n.setLong("R_CStackLimit", -1L);
		}
	}

	private void startWindows() throws Throwable {
		/* First: R may use the stack deeply already at its start */
		commitWindowsStack();
		Arena arena = n.arena;
		var call = RNative.LINKER;
		call.downcallHandle(n.symbolAddress("R_setStartTime"), FunctionDescriptor.ofVoid()).invokeExact();
		/* structRstart (R_ext/RStartup.h, RSTART_VERSION 1), offsets of R 4.2 and later */
		MemorySegment rp = arena.allocate(512, 8);
		int defParams = (int) call.downcallHandle(n.symbolAddress("R_DefParamsEx"), FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT)).invokeExact(rp, 1);
		if (defParams != 0) {
			/* This R doesn't accept the start structure (version 1) of the offsets below: no start */
			throw new IllegalStateException("This version of R doesn't support the start structure of embedded R (R_DefParamsEx returned " + defParams + ")");
		}
		MemorySegment user = (MemorySegment) call.downcallHandle(n.symbolAddress("getRUser"), FunctionDescriptor.of(ADDRESS)).invokeExact();
		rp.set(JAVA_INT, 0, 1); // R_Quiet
		rp.set(JAVA_INT, 8, options.interactive() ? 1 : 0); // R_Interactive
		rp.set(JAVA_INT, 28, 0); // RestoreAction: SA_NORESTORE
		rp.set(JAVA_INT, 32, 3); // SaveAction: SA_NOSAVE
		rp.set(ADDRESS, 88, arena.allocateFrom(options.home().toString()));
		rp.set(ADDRESS, 96, user);
		rp.set(ADDRESS, 104, stub("readConsole", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT)));
		rp.set(ADDRESS, 112, MemorySegment.NULL); // WriteConsole: WriteConsoleEx is used
		rp.set(ADDRESS, 120, stub("callback", FunctionDescriptor.ofVoid()));
		rp.set(ADDRESS, 128, stub("showMessage", FunctionDescriptor.ofVoid(ADDRESS)));
		rp.set(ADDRESS, 136, stub("yesNoCancel", FunctionDescriptor.of(JAVA_INT, ADDRESS)));
		rp.set(ADDRESS, 144, stub("busyCallback", FunctionDescriptor.ofVoid(JAVA_INT)));
		rp.set(JAVA_INT, 152, 2); // CharacterMode: LinkDLL
		rp.set(ADDRESS, 160, stub("writeConsoleEx", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT)));
		rp.set(JAVA_INT, 168, 0); // EmitEmbeddedUTF8
		call.downcallHandle(n.symbolAddress("R_SetParams"), FunctionDescriptor.ofVoid(ADDRESS)).invokeExact(rp);
		List<String> args = arguments();
		MemorySegment av = argv(arena, args)[0];
		call.downcallHandle(n.symbolAddress("R_set_command_line_arguments"), FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS)).invokeExact(args.size(), av);
		setRStackLimit();
		if (n.GA_initapp != null) {
			int unused2 = (int) n.GA_initapp.invokeExact(0, MemorySegment.NULL);
		}
		call.downcallHandle(n.symbolAddress("readconsolecfg"), FunctionDescriptor.ofVoid()).invokeExact();
		n.numericLocaleC();
		call.downcallHandle(n.symbolAddress("setup_Rmainloop"), FunctionDescriptor.ofVoid()).invokeExact();
		/* Again after the setup (it may set the stack variables of R) */
		setRStackLimit();
		/* R runs in UTF-8 (LC_CTYPE=.UTF-8, see RNative.load): the console text is UTF-8 */
		if (n.isUtf8Locale()) {
			n.consoleCharset = java.nio.charset.StandardCharsets.UTF_8;
		}
	}

	private MemorySegment stub(String method, FunctionDescriptor fd) {
		try {
			var h = MethodHandles.lookup().findVirtual(RSession.class, method, fd.toMethodType()).bindTo(this);
			return RNative.LINKER.upcallStub(h, fd, n.arena);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	/* The helper functions in R: all parsing and evaluation (and their errors) stay in R */
	private static final String HELPERS = """
			local({
			  e <- new.env(parent = globalenv())
			  e$lastError <- ""
			  e$errorText <- function(err, wrapper) {
			    cl <- conditionCall(err)
			    msg <- conditionMessage(err)
			    if (is.null(cl) || identical(cl, wrapper) || identical(cl, quote(parse(text = code, keep.source = FALSE)))) msg
			    else paste0(paste(deparse(cl, nlines = 1L), collapse = " "), ": ", msg)
			  }
			  e$evalString <- function(code) {
			    e$lastError <- ""
			    withCallingHandlers({
			      r <- NULL
			      for (x in parse(text = code, keep.source = FALSE)) r <- eval(x, globalenv())
			      r
			    }, error = function(err) e$lastError <- e$errorText(err, quote(eval(x, globalenv()))))
			  }
			  e$printWarnings <- function(ws) {
			    fmt <- function(w) {
			      cl <- conditionCall(w)
			      paste0(if (!is.null(cl)) paste0("In ", paste(deparse(cl, nlines = 1L), collapse = " "), " : ") else "", conditionMessage(w))
			    }
			    txt <- if (length(ws) == 1L) paste0("Warning message:\\n", fmt(ws[[1L]]), "\\n")
			           else paste0("Warning messages:\\n", paste0(seq_along(ws), ": ", vapply(ws, fmt, ""), "\\n", collapse = ""))
			    cat(paste0("\\003", txt, "\\004"), file = stderr())
			  }
			  e$evalPrint <- function(expr) {
			    ws <- list()
			    wl <- getOption("warn", 0)
			    r <- withCallingHandlers(withVisible(eval(expr, globalenv())),
			      error = function(err) {
			        cl <- conditionCall(err)
			        msg <- conditionMessage(err)
			        txt <- if (is.null(cl) || identical(cl, quote(eval(expr, globalenv())))) paste0("Error: ", msg)
			               else paste0("Error in ", paste(deparse(cl, nlines = 1L), collapse = " "), " : ", msg)
			        cat(paste0("\\002", txt, "\\n\\004"), file = stderr())
			      },
			      warning = function(w) if (wl == 0) {
			        ws[[length(ws) + 1L]] <<- w
			        invokeRestart("muffleWarning")
			      })
			    if (r$visible) print(r$value)
			    if (length(ws)) e$printWarnings(ws)
			    invisible(NULL)
			  }
			  e$consoleParse <- function(code) {
			    if (exists("Sys.setLanguage", baseenv())) {
			      old <- suppressWarnings(Sys.setLanguage("en"))
			      on.exit(suppressWarnings(Sys.setLanguage(old)))
			    }
			    tryCatch(parse(text = code, keep.source = FALSE), error = function(err)
			      if (grepl("end of input|INCOMPLETE_STRING", conditionMessage(err))) NULL else list(err))
			  }
			  e$parseError <- function(code) tryCatch(parse(text = code, keep.source = FALSE),
			    error = function(err) cat(paste0("\\002Error: ", conditionMessage(err), "\\n\\004"), file = stderr()))
			  e$assign <- function(name, value) assign(name, value, envir = globalenv())
			  e$get <- function(name) get(name, envir = globalenv())
			  e$alloc <- function(mode, n) vector(mode, n)
			  e$idle <- function() Sys.sleep(0)
			  e$replay <- function(n) {
			    cur <- dev.cur()
			    if (!(n %in% dev.list())) return(invisible(FALSE))
			    if (cur != n) {
			      dev.set(n)
			      on.exit(if (cur %in% dev.list()) dev.set(cur))
			    }
			    p <- recordPlot()
			    if (length(p[[1L]])) replayPlot(p)
			    invisible(TRUE)
			  }
			  e$history <- new.env()
			  e$historyRecord <- function() {
			    key <- as.character(dev.cur())
			    s <- e$history[[key]]
			    if (is.null(s)) return(invisible())
			    if (s$replayed) s$replayed <- FALSE
			    else {
			      p <- recordPlot()
			      if (length(p[[1L]])) s$plots <- utils::tail(c(s$plots, list(p)), s$max)
			    }
			    s$index <- 0L
			    assign(key, s, envir = e$history)
			    invisible()
			  }
			  e$historyEnable <- function(n, max) {
			    # The plots of closed devices are released (and a reused device number starts empty)
			    open <- as.character(dev.list())
			    stale <- setdiff(ls(e$history), setdiff(open, as.character(n)))
			    if (length(stale)) rm(list = stale, envir = e$history)
			    if (max <= 0L) return(invisible())
			    assign(as.character(n), list(plots = list(), index = 0L, replayed = FALSE, max = max), envir = e$history)
			    if (is.null(e$hooked)) {
			      setHook("before.plot.new", function() e$historyRecord())
			      setHook("before.grid.newpage", function() e$historyRecord())
			      e$hooked <- TRUE
			    }
			    invisible()
			  }
			  e$historyMove <- function(n, step) {
			    key <- as.character(n)
			    s <- e$history[[key]]
			    if (is.null(s) || !(n %in% dev.list())) return(c(0L, 0L))
			    if (step == 0L) return(c(s$index, length(s$plots)))
			    cur <- dev.cur()
			    if (cur != n) {
			      dev.set(n)
			      on.exit(if (cur %in% dev.list()) dev.set(cur))
			    }
			    if (s$index == 0L) {
			      if (step > 0L) return(c(0L, length(s$plots)))
			      p <- recordPlot()
			      if (length(p[[1L]])) s$plots <- utils::tail(c(s$plots, list(p)), s$max)
			      s$index <- length(s$plots)
			    }
			    s$index <- max(1L, min(length(s$plots), s$index + step))
			    if (length(s$plots)) {
			      replayPlot(s$plots[[s$index]])
			      s$replayed <- TRUE
			    }
			    assign(key, s, envir = e$history)
			    c(s$index, length(s$plots))
			  }
			  e$deviceEnv <- function() {
			    if (!("bio7ffm" %in% search())) attach(NULL, name = "bio7ffm", warn.conflicts = FALSE)
			    as.environment("bio7ffm")
			  }
			  e$defineDevice <- function(name, id, ptr) {
			    env <- e$deviceEnv()
			    assign(".newDevice", ptr, envir = env)
			    f <- function(width = 7, height = 7, pointsize = 12) {
			      r <- .Call(.newDevice, ID, as.double(width), as.double(height), as.double(pointsize))
			      if (is.character(r)) stop(r, call. = FALSE)
			      invisible(dev.cur())
			    }
			    body(f) <- do.call(substitute, list(body(f), list(ID = as.integer(id))))
			    environment(f) <- env
			    assign(name, f, envir = env)
			  }
			  e$defineFunction <- function(name, id, ptr) {
			    env <- e$deviceEnv()
			    assign(".javaCall", ptr, envir = env)
			    f <- function(...) {
			      r <- .Call(.javaCall, ID, list(...))
			      if (inherits(r, "bio7JavaError")) stop(unclass(r), call. = FALSE)
			      r
			    }
			    body(f) <- do.call(substitute, list(body(f), list(ID = as.integer(id))))
			    environment(f) <- env
			    assign(name, f, envir = env)
			  }
			  e$disableQuit <- function() {
			    f <- function(save = "default", status = 0, runLast = TRUE)
			      stop("quit() is not available in embedded R: please close R in the application", call. = FALSE)
			    for (n in c("q", "quit")) {
			      unlockBinding(n, baseenv())
			      assign(n, f, envir = baseenv())
			      lockBinding(n, baseenv())
			    }
			  }
			  e
			})
			""";

	private void defineHelpers() throws Throwable {
		MemorySegment text = c.protect(c.mkString(HELPERS));
		MemorySegment status = n.arena.allocate(JAVA_INT);
		var parse = RNative.LINKER.downcallHandle(n.symbolAddress("R_ParseVector"), FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS));
		MemorySegment exprs = c.protect((MemorySegment) parse.invokeExact(text, -1, status, c.nil));
		if (status.get(JAVA_INT, 0) != 1) {
			throw new IllegalStateException("The helper functions could not be parsed");
		}
		MemorySegment env = c.tryEval(c.vectorElt(exprs, 0), c.globalEnv, false);
		if (env == null) {
			throw new IllegalStateException("The helper functions could not be defined");
		}
		c.preserve(env);
		helperEnv = env;
		c.unprotect(2);
		/*
		 * The helpers are called by their names (symbols), evaluated in the environment
		 * of the helpers (helperEval): a call with the function object itself (an
		 * inlined closure, never made by R code) crashes R when rlang makes a backtrace
		 * of it (e.g., knitr for each error of a chunk: rlang::trace_back()).
		 */
		for (String name : new String[] { "evalString", "evalPrint", "consoleParse", "parseError", "assign", "get", "alloc", "idle", "replay", "defineDevice",
				"defineFunction", "disableQuit", "historyEnable", "historyMove" }) {
			if (c.typeOf(c.findVar(c.install(name), env)) != RNative.CLOSXP) {
				throw new IllegalStateException("The helper function " + name + " is missing");
			}
			helpers.put(name, c.install(name));
		}
		if (!options.allowQuit()) {
			MemorySegment call = c.protect(c.lang1(helpers.get("disableQuit")));
			c.tryEval(call, helperEnv, true);
			c.unprotect(1);
		}
		/* mclapply() and other forks would copy the whole Java process: sequential by default */
		MemorySegment mc = c.protect(c.mkString("options(mc.cores = 1L)"));
		MemorySegment mcCall = c.protect(c.lang2(helpers.get("evalString"), mc));
		c.tryEval(mcCall, helperEnv, true);
		c.unprotect(2);
	}

	/* Allocates a vector by R code: an allocation error is an R error caught by R */
	private MemorySegment alloc(int type, long length) {
		String mode = switch (type) {
		case RNative.REALSXP -> "double";
		case RNative.INTSXP -> "integer";
		case RNative.LGLSXP -> "logical";
		case RNative.STRSXP -> "character";
		case RNative.VECSXP -> "list";
		case RNative.RAWSXP -> "raw";
		default -> throw new IllegalArgumentException("type " + type);
		};
		MemorySegment m = c.protect(c.mkString(mode));
		MemorySegment len = c.protect(c.scalarReal(length));
		MemorySegment call = c.protect(c.lang3(helpers.get("alloc"), m, len));
		MemorySegment v = c.tryEval(call, helperEnv, true);
		c.unprotect(3);
		if (v == null) {
			throw new IllegalStateException("R could not allocate a vector of " + length + " elements: " + errorMessage());
		}
		return v;
	}

	/* Evaluates code (thread of R) and returns the value protected once */
	private MemorySegment evalOnRThread(String code) throws RException {
		MemorySegment s = c.protect(c.mkString(code));
		MemorySegment call = c.protect(c.lang2(helpers.get("evalString"), s));
		MemorySegment v = c.tryEval(call, helperEnv, true);
		c.unprotect(2);
		if (v == null) {
			throw new RException(errorMessage());
		}
		return c.protect(v);
	}

	private String errorMessage() {
		if (interruptRequested) {
			return "interrupted";
		}
		/* The message of evalString (without the call of the helper), else geterrmessage() */
		MemorySegment last = helperEnv == null ? null : c.findVar(c.install("lastError"), helperEnv);
		if (last != null && c.typeOf(last) == RNative.STRSXP && c.length(last) > 0) {
			String text = c.charUTF8(c.stringElt(last, 0));
			if (text != null && !text.isBlank()) {
				return text.strip();
			}
		}
		MemorySegment call = c.protect(c.lang1(c.install("geterrmessage")));
		MemorySegment v = c.tryEval(call, c.globalEnv, true);
		String msg = "an error occurred in R";
		if (v != null && c.typeOf(v) == RNative.STRSXP && c.length(v) > 0) {
			msg = c.charUTF8(c.stringElt(v, 0)).strip();
		}
		c.unprotect(1);
		return msg;
	}

	private void idle() {
		MemorySegment call = c.protect(c.lang1(helpers.get("idle")));
		c.tryEval(call, helperEnv, true);
		c.unprotect(1);
	}

	/* ---------------- The callbacks of R (no R calls, no exceptions) ---------------- */

	/* The marked stream of the console output (0: none) */
	private char markedStream;

	/* consoleCapture: the captured output, if it is shown too, and the input requests of R */
	private volatile StringBuilder capture;
	private volatile boolean captureEcho;
	/* consoleCapture with a filter: the output is shown once, filtered, before the next prompt */
	private volatile java.util.function.UnaryOperator<String> captureFilter;
	private volatile boolean captureFlushed;
	/* The line of the running capture, its buffer and echo, the input requests when R took it (-1: not yet) */
	private long captureSeq = -1;
	private StringBuilder pendingCapture;
	private String captureEchoLine;
	private long captureStart = -1;
	private long lineSeq;
	/* The last line typed in the console (not a capture) and its output, for the observer (thread of R) */
	private String userLine;
	private final StringBuilder userOutput = new StringBuilder();
	private volatile java.util.function.BiConsumer<String, String> consoleObserver;
	private static final java.util.concurrent.ExecutorService OBSERVER_EXECUTOR = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "R console observer");
		t.setDaemon(true);
		return t;
	});

	/**
	 * Called (in a background thread) after R has evaluated a line typed in the
	 * console (not a consoleCapture) and asks for the next input: the line and its
	 * output. E.g., a debugger follows the steps typed in the console.
	 */
	public void setConsoleObserver(java.util.function.BiConsumer<String, String> observer) {
		consoleObserver = observer;
	}

	/* Queues a console line (numbered) */
	private ConsoleLine enqueue(String text) {
		synchronized (inputSignal) {
			ConsoleLine l = new ConsoleLine(text == null ? "" : text, ++lineSeq);
			consoleInput.add(l);
			return l;
		}
	}

	/* R takes a console line (thread of R): the capture starts with its own line */
	private String taken(ConsoleLine l) {
		String echo = null;
		synchronized (inputSignal) {
			if (l.seq() == captureSeq) {
				captureStart = inputRequests;
				capture = pendingCapture;
				echo = captureEchoLine;
				userLine = null;
			} else if (capture == null) {
				userLine = l.text();
				userOutput.setLength(0);
			}
		}
		if (echo != null) {
			/* As typed after the prompt */
			console.write(echo + "\n", false);
		}
		return l.text();
	}

	/* Shows the filtered output of a capture (thread of R, before the next prompt) */
	private void flushCapture() {
		StringBuilder sb = capture;
		var filter = captureFilter;
		if (sb == null || filter == null || captureFlushed) {
			return;
		}
		captureFlushed = true;
		String text;
		synchronized (sb) {
			text = sb.toString();
		}
		if (!text.isEmpty()) {
			String shown;
			try {
				shown = filter.apply(text);
			} catch (Throwable t) {
				/* A filter must not disturb the input of R: the text unfiltered */
				shown = text;
			}
			console.write(shown, false);
		}
	}
	private final Object inputSignal = new Object();
	private long inputRequests;

	/* R asks for the next console input (in browser() or at the top level) */
	private void signalInputRequest() {
		String line;
		String out;
		synchronized (inputSignal) {
			inputRequests++;
			inputSignal.notifyAll();
			line = userLine;
			out = userOutput.toString();
			userLine = null;
			userOutput.setLength(0);
		}
		var observer = consoleObserver;
		if (line != null && observer != null) {
			OBSERVER_EXECUTOR.execute(() -> {
				try {
					observer.accept(line, out);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			});
		}
	}

	/* Output during consoleCapture (thread of R) */
	private boolean captured(String text) {
		StringBuilder sb = capture;
		if (sb == null) {
			return false;
		}
		synchronized (sb) {
			sb.append(text);
		}
		return !captureEcho;
	}

	/**
	 * Sends a console line (as console(String)) and returns the output of R until R
	 * asks for the next input: the prompt of browser() or of the console. E.g., for
	 * a debugger: a step ("n") and its "debug at" line, then the variables.
	 *
	 * @param echo true: the output is shown in the console too
	 */
	public String consoleCapture(String line, long timeoutMillis, boolean echo) throws RException {
		return consoleCapture(line, timeoutMillis, echo, null);
	}

	/**
	 * As consoleCapture(String, long, boolean); the output shown in the console
	 * passes the filter (e.g., to shorten paths), shown once before the next prompt.
	 */
	public String consoleCapture(String line, long timeoutMillis, boolean echo, java.util.function.UnaryOperator<String> filter) throws RException {
		if (!alive) {
			throw new RException("R is not running");
		}
		StringBuilder sb = new StringBuilder();
		/*
		 * The output is collected from the moment R takes this line (a line typed in
		 * the console before may still run) up to the next input request
		 */
		synchronized (inputSignal) {
			/* With a filter the output is collected and shown at the end */
			captureEcho = echo && filter == null;
			captureFilter = echo ? filter : null;
			captureFlushed = false;
			captureEchoLine = echo ? line : null;
			pendingCapture = sb;
			captureStart = -1;
			captureSeq = lineSeq + 1;
			console(line);
		}
		try {
			long deadline = System.currentTimeMillis() + timeoutMillis;
			synchronized (inputSignal) {
				while (captureStart < 0 || inputRequests == captureStart) {
					long wait = deadline - System.currentTimeMillis();
					if (wait <= 0) {
						break;
					}
					inputSignal.wait(wait);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			synchronized (inputSignal) {
				/* Not taken yet (time limit): the line runs as console input */
				captureSeq = -1;
				pendingCapture = null;
				captureEchoLine = null;
				capture = null;
				captureFilter = null;
			}
		}
		synchronized (sb) {
			return sb.toString();
		}
	}

	void writeConsoleEx(MemorySegment buf, int len, int otype) {
		try {
			byte[] b = buf.reinterpret(len).toArray(JAVA_BYTE);
			String text = new String(b, n.consoleCharset);
			String plain = text.replace("\u0002", "").replace("\u0003", "").replace("\u0004", "");
			if (captured(plain)) {
				return;
			}
			if (consoleObserver != null) {
				synchronized (inputSignal) {
					if (userLine != null && userOutput.length() < 65536) {
						userOutput.append(plain);
					}
				}
			}
			/*
			 * The errors (\u0002) and warnings (\u0003) of the console are marked up to
			 * \u0004 (else messages and errors share the error stream; R may write a text in
			 * several parts)
			 */
			while (!text.isEmpty()) {
				char first = text.charAt(0);
				if (first == '\u0002' || first == '\u0003') {
					markedStream = first;
					text = text.substring(1);
					continue;
				}
				int end = markedStream == 0 ? -1 : text.indexOf('\u0004');
				String part = markedStream == 0 ? text : end < 0 ? text : text.substring(0, end);
				if (!part.isEmpty()) {
					if (markedStream == '\u0002') {
						console.writeError(part);
					} else if (markedStream == '\u0003') {
						console.writeWarning(part);
					} else {
						console.write(part, otype != 0);
					}
				}
				if (markedStream != 0 && end >= 0) {
					markedStream = 0;
					text = text.substring(end + 1);
				} else {
					text = "";
				}
			}
		} catch (Throwable t) {
			/* ignored */
		}
	}

	int readConsole(MemorySegment prompt, MemorySegment buf, int len, int hist) {
		try {
			flushCapture();
			/* The prompt after a hidden capture is not shown (the visible one is already there) */
			if (capture == null || captureEcho || captureFilter != null) {
				console.prompt(RNative.cString(prompt, n.consoleCharset));
			}
			console.busy(false);
			readingConsole = true;
			signalInputRequest();
			String line = taken(consoleInput.take());
			console.busy(true);
			byte[] b = (line + "\n").getBytes(n.consoleCharset);
			int count = Math.min(b.length, len - 1);
			MemorySegment out = buf.reinterpret(len);
			MemorySegment.copy(b, 0, out, JAVA_BYTE, 0, count);
			out.set(JAVA_BYTE, count, (byte) 0);
			return 1;
		} catch (Throwable t) {
			return 0;
		} finally {
			readingConsole = false;
		}
	}

	void showMessage(MemorySegment msg) {
		try {
			console.message(RNative.cString(msg, n.consoleCharset));
		} catch (Throwable t) {
			/* ignored */
		}
	}

	void busyCallback(int which) {
		try {
			console.busy(which != 0);
		} catch (Throwable t) {
			/* ignored */
		}
	}

	void callback() {
	}

	int yesNoCancel(MemorySegment question) {
		/* -1: No (e.g., "Save workspace image?") */
		return -1;
	}

	/* .Call(.newDevice, id, width, height, pointsize) of the device function in R */
	MemorySegment deviceCall(MemorySegment id, MemorySegment width, MemorySegment height, MemorySegment pointsize) {
		try {
			int key = c.data(id, RNative.INTSXP).get(JAVA_INT, 0);
			double w = c.data(width, RNative.REALSXP).get(JAVA_DOUBLE, 0);
			double h = c.data(height, RNative.REALSXP).get(JAVA_DOUBLE, 0);
			double ps = c.data(pointsize, RNative.REALSXP).get(JAVA_DOUBLE, 0);
			RDeviceFactory factory = deviceFactories.get(key);
			RDeviceFactory.DeviceSpec spec = factory.create(w, h, ps);
			int number = devices.open(deviceNames.get(key), spec.device(), spec.dpi(), spec.pointsize());
			{
				/* From the first page on (0: no history); the R error, if any, is caught by tryEval in C */
				MemorySegment num = c.protect(c.scalarInteger(number));
				MemorySegment mx = c.protect(c.scalarInteger(Math.max(0, spec.history())));
				MemorySegment call = c.protect(c.lang3(helpers.get("historyEnable"), num, mx));
				c.tryEval(call, helperEnv, true);
				c.unprotect(3);
			}
			return c.nil;
		} catch (Throwable t) {
			try {
				return c.mkString("The device could not be opened: " + t.getMessage());
			} catch (Throwable t2) {
				return c.nil;
			}
		}
	}

	/* ---------------- Calls from R into Java ---------------- */

	/**
	 * A function of Java called from R (see {@link RSession#registerFunction}): the
	 * arguments of the call in R, the result for R (null: NULL). An exception
	 * becomes an error in R.
	 */
	@FunctionalInterface
	public interface RFunction {
		RValue call(List<RValue> args) throws Exception;
	}

	/**
	 * Defines a function in R (in the attached environment "bio7ffm") which calls
	 * Java: name(...) passes its arguments to the function. The function runs in
	 * another thread; meanwhile R runs the calls of other threads (e.g., the
	 * function may evaluate R code with this session). Console input waits.
	 */
	public void registerFunction(String rFunctionName, RFunction function) throws RException {
		call(() -> {
			if (javaCallPointer == null) {
				MemorySegment stub = stub("javaCall", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
				javaCallPointer = c.makeExternalPtr(stub, c.install("native symbol"));
				c.preserve(javaCallPointer);
			}
			int id = functions.size() + 1;
			functions.put(id, function);
			MemorySegment nm = c.protect(c.mkString(rFunctionName));
			MemorySegment idv = c.protect(c.scalarInteger(id));
			MemorySegment full = c.protect(c.lang4(helpers.get("defineFunction"), nm, idv, javaCallPointer));
			MemorySegment r = c.tryEval(full, helperEnv, true);
			c.unprotect(3);
			if (r == null) {
				throw new RException(errorMessage());
			}
			return null;
		});
	}

	/* .Call of a function of Java (thread of R) */
	MemorySegment javaCall(MemorySegment id, MemorySegment args) {
		try {
			RFunction f = functions.get(c.data(id, RNative.INTSXP).get(JAVA_INT, 0));
			RValue a = convert.toJava(args);
			List<RValue> list = a instanceof RValue.List l ? l.values() : List.of(a);
			CompletableFuture<RValue> result = new CompletableFuture<>();
			/* Wakes the thread of R when the function has finished (else it noticed it only after the poll: about 20 ms per call) */
			Wake wake = new Wake();
			Thread worker = Thread.ofVirtual().name("R calls Java").start(() -> {
				try {
					result.complete(f.call(list));
				} catch (Throwable t) {
					result.completeExceptionally(t);
				} finally {
					tasks.offer(wake);
				}
			});
			/* The calls of other threads (also of the function itself) run meanwhile; console input waits */
			List<Runnable> deferred = new ArrayList<>();
			while (!result.isDone()) {
				Runnable task = tasks.poll(20, TimeUnit.MILLISECONDS);
				if (task instanceof Wake) {
					/* Only wakes up (this call or a finished nested one): the loop checks its result */
					continue;
				}
				if (task instanceof FutureTask<?>) {
					task.run();
				} else if (task != null) {
					deferred.add(task);
				}
			}
			worker.join();
			/* The wake of this call if it wasn't taken (it must not reach the main loop) */
			tasks.remove(wake);
			tasks.addAll(deferred);
			RValue v;
			try {
				v = result.get();
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				return javaError(cause.getMessage() == null ? cause.toString() : cause.getMessage());
			}
			return v == null ? c.nil : unprotected(convert.toR(v));
		} catch (Throwable t) {
			return javaError(String.valueOf(t));
		}
	}

	/* A wake-up of the thread of R waiting for a function of Java (never run) */
	private static final class Wake implements Runnable {
		@Override
		public void run() {
			/* Nothing: only wakes up the waiting thread */
		}
	}

	/* toR returns the value protected: returned to R at once (no allocation before) */
	private MemorySegment unprotected(MemorySegment x) {
		c.unprotect(1);
		return x;
	}

	private MemorySegment javaError(String message) {
		try {
			return unprotected(convert.toR(new RValue.Str(new String[] { message }, Map.of("class", new RValue.Str("bio7JavaError")))));
		} catch (Throwable t) {
			return c.nil;
		}
	}

	/* ---------------- Calls into R ---------------- */

	/** Code which runs in the thread of R. */
	@FunctionalInterface
	private interface RTask<T> {
		T run() throws Exception;
	}

	private <T> T call(RTask<T> task) throws RException {
		return call(task, 0);
	}

	/*
	 * The number of finished calls and console inputs: a change shows that R has
	 * worked (e.g., for a display of the memory of R)
	 */
	private final java.util.concurrent.atomic.AtomicLong activity = new java.util.concurrent.atomic.AtomicLong();

	/**
	 * Returns a counter of the finished calls and console inputs. It changes after
	 * every evaluation, also after short ones which are over before a poll of
	 * isBusy() could see them.
	 */
	public long getActivity() {
		return activity.get();
	}

	/* timeoutMillis <= 0: no limit */
	private <T> T call(RTask<T> task, long timeoutMillis) throws RException {
		try {
			return callCounted(task, timeoutMillis);
		} finally {
			activity.incrementAndGet();
		}
	}

	private <T> T callCounted(RTask<T> task, long timeoutMillis) throws RException {
		if (!alive) {
			throw new RException("R is not running");
		}
		if (Thread.currentThread() == thread) {
			try {
				return task.run();
			} catch (RException e) {
				throw e;
			} catch (Exception e) {
				throw new RException(e.getMessage(), e);
			}
		}
		/*
		 * R waits for console input within an evaluation (browser(), the debugger,
		 * readline()): a call would wait until that ends (e.g., the UI would freeze)
		 */
		if (readingConsole) {
			throw new RException("R waits for console input (e.g., browser() or readline()): please finish it in the console first");
		}
		/* A limited thread (e.g., the UI thread) waits at most waitLimitMillis until R starts the call */
		long startLimit = 0;
		java.util.function.BooleanSupplier limited = waitLimitedThread;
		boolean limitedCaller = limited != null && limited.getAsBoolean();
		if (limitedCaller && waitLimitMillis > 0) {
			startLimit = waitLimitMillis;
		}
		return await(task, timeoutMillis, startLimit, limitedCaller);
	}

	/* R runs a call for which a limited thread (e.g., the UI thread) waits (thread of R) */
	private volatile boolean runningForLimited;

	/**
	 * Returns true if R runs a call for which a limited thread (e.g., the UI
	 * thread, see setWaitLimit) waits: a callback of R must not wait for that
	 * thread then (e.g., locator() waiting for a click in the UI).
	 */
	public boolean isLimitedThreadWaiting() {
		return runningForLimited;
	}

	private <T> T await(RTask<T> task, long timeoutMillis, long startLimit, boolean limitedCaller) throws RException {
		/* 0: queued, 1: started, 2: withdrawn (R skips it) */
		java.util.concurrent.atomic.AtomicInteger state = new java.util.concurrent.atomic.AtomicInteger();
		FutureTask<T> f = new FutureTask<>((Callable<T>) () -> {
			if (!state.compareAndSet(0, 1)) {
				return null;
			}
			runningForLimited = limitedCaller;
			try {
				return task.run();
			} finally {
				runningForLimited = false;
			}
		});
		tasks.add(f);
		long now = System.currentTimeMillis();
		long end = timeoutMillis > 0 ? now + timeoutMillis : Long.MAX_VALUE;
		long startEnd = startLimit > 0 ? now + startLimit : Long.MAX_VALUE;
		try {
			while (true) {
				long until = state.get() == 0 ? Math.min(end, startEnd) : end;
				if (until == Long.MAX_VALUE) {
					return f.get();
				}
				try {
					return f.get(Math.max(1, until - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
				} catch (TimeoutException e) {
					if (state.compareAndSet(0, 2)) {
						/* Not started: withdrawn, it never runs */
						f.cancel(false);
						throw new RException(System.currentTimeMillis() >= end ? "R did not answer within " + timeoutMillis + " ms (busy)"
								: "R is busy (another evaluation runs): please try again when it has finished");
					}
					if (System.currentTimeMillis() >= end) {
						/* Started: R finishes it, the result is dropped */
						throw new RException("R did not answer within " + timeoutMillis + " ms (busy)");
					}
					/* Started within the wait limit: wait for the result (up to the timeout, if any) */
				}
			}
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			throw cause instanceof RException r ? r : new RException(String.valueOf(cause.getMessage()), cause);
		} catch (InterruptedException e) {
			state.compareAndSet(0, 2);
			f.cancel(false);
			Thread.currentThread().interrupt();
			throw new RException("interrupted");
		}
	}

	private volatile java.util.function.BooleanSupplier waitLimitedThread;
	private volatile long waitLimitMillis;

	/**
	 * Calls from a thread for which isLimited returns true (e.g., the UI thread)
	 * wait at most waitMillis until R starts them (R busy with another evaluation):
	 * else they are withdrawn and fail with an RException. Once started, they run
	 * as long as they need. null or waitMillis &lt;= 0: no limit.
	 */
	public void setWaitLimit(java.util.function.BooleanSupplier isLimited, long waitMillis) {
		this.waitLimitedThread = isLimited;
		this.waitLimitMillis = waitMillis;
	}

	/** Evaluates R code (all expressions) and returns the value of the last one. */
	public RValue eval(String code) throws RException {
		return eval(code, 0);
	}

	/**
	 * As eval(String), but waits at most timeoutMillis for R (e.g., for the hover or
	 * code completion of an editor while R is busy); &lt;= 0: no limit.
	 *
	 * @throws RException also if R didn't answer in time
	 */
	public RValue eval(String code, long timeoutMillis) throws RException {
		return call(() -> {
			MemorySegment v = evalOnRThread(code);
			try {
				return convert.toJava(v);
			} finally {
				c.unprotect(1);
			}
		}, timeoutMillis);
	}

	/** Evaluates R code without transferring the result. */
	public void voidEval(String code) throws RException {
		voidEval(code, 0);
	}

	/** As voidEval(String), but waits at most timeoutMillis for R; &lt;= 0: no limit. */
	public void voidEval(String code, long timeoutMillis) throws RException {
		call(() -> {
			evalOnRThread(code);
			c.unprotect(1);
			return null;
		}, timeoutMillis);
	}

	/** Assigns a value to a variable of the global environment. */
	public void assign(String name, RValue value) throws RException {
		call(() -> {
			MemorySegment x;
			try {
				x = convert.toR(value);
			} catch (IllegalArgumentException | IllegalStateException e) {
				throw new RException(e.getMessage(), e);
			}
			MemorySegment nm = c.protect(c.mkString(name));
			MemorySegment call = c.protect(c.lang3(helpers.get("assign"), nm, x));
			MemorySegment r = c.tryEval(call, helperEnv, true);
			c.unprotect(3);
			if (r == null) {
				throw new RException(errorMessage());
			}
			return null;
		});
	}

	public void assign(String name, double... values) throws RException {
		assign(name, new RValue.Num(values));
	}

	public void assign(String name, int... values) throws RException {
		assign(name, new RValue.Int(values));
	}

	public void assign(String name, String... values) throws RException {
		assign(name, new RValue.Str(values));
	}

	public void assign(String name, boolean... values) throws RException {
		assign(name, new RValue.Logical(values));
	}

	public void assign(String name, byte[] values) throws RException {
		assign(name, new RValue.Raw(values));
	}

	/** Assigns a matrix (rows of a Java array). */
	public void assign(String name, double[][] matrix) throws RException {
		assign(name, RValue.Num.matrix(matrix));
	}

	/** The value of a variable of the global environment. */
	public RValue get(String name) throws RException {
		return call(() -> {
			MemorySegment nm = c.protect(c.mkString(name));
			MemorySegment call = c.protect(c.lang2(helpers.get("get"), nm));
			MemorySegment v = c.tryEval(call, helperEnv, true);
			if (v == null) {
				c.unprotect(2);
				throw new RException(errorMessage());
			}
			c.protect(v);
			try {
				return convert.toJava(v);
			} finally {
				c.unprotect(3);
			}
		});
	}

	/** Evaluates code and returns the result as doubles (e.g., "rnorm(10)"). */
	public double[] evalDoubles(String code) throws RException {
		RValue v = eval(code);
		return switch (v) {
		case RValue.Num d -> d.values();
		case RValue.Int i -> java.util.Arrays.stream(i.values()).mapToDouble(x -> x == RValue.NA_INTEGER ? RValue.NA_REAL : x).toArray();
		default -> throw new RException("not numeric: " + v.getClass().getSimpleName());
		};
	}

	/** Evaluates code and returns the result as strings. */
	public String[] evalStrings(String code) throws RException {
		RValue v = eval(code);
		if (v instanceof RValue.Str s) {
			return s.values();
		}
		throw new RException("not a character vector: " + v.getClass().getSimpleName());
	}

	/* ---------------- The console ---------------- */

	/**
	 * Input of the console (one line, like typed): evaluated with printing like
	 * the console of R; an incomplete expression waits for the next line.
	 */
	public void console(String line) {
		/* R reads it itself if it waits in readline() */
		boolean queue = !readingConsole;
		if (queue) {
			/*
			 * Busy from now on (not only when the task starts): a call queued behind the
			 * console input would wait until the evaluation ends
			 */
			busy = true;
		}
		enqueue(line);
		if (queue) {
			tasks.add(this::drainConsole);
		}
	}

	private void drainConsole() {
		ConsoleLine line;
		while ((line = consoleInput.poll()) != null) {
			consoleStep(taken(line));
		}
		/* The input may have been read by readline() of another evaluation */
		busy = false;
	}

	private void consoleStep(String line) {
		busy = true;
		console.busy(true);
		try {
			pendingInput.append(line).append('\n');
			MemorySegment code = c.protect(c.mkString(pendingInput.toString()));
			MemorySegment call = c.protect(c.lang2(helpers.get("consoleParse"), code));
			MemorySegment exprs = c.tryEval(call, helperEnv, true);
			if (exprs == null) {
				c.unprotect(2);
				pendingInput.setLength(0);
				console.prompt("> ");
				return;
			}
			c.protect(exprs);
			int type = c.typeOf(exprs);
			if (type == RNative.NILSXP) {
				/* Incomplete: the next line continues it */
				c.unprotect(3);
				console.prompt("+ ");
				return;
			}
			pendingInput.setLength(0);
			if (type == RNative.EXPRSXP) {
				long count = c.length(exprs);
				MemorySegment quote = c.install("quote");
				for (long i = 0; i < count; i++) {
					MemorySegment q = c.protect(c.lang2(quote, c.vectorElt(exprs, i)));
					MemorySegment e = c.protect(c.lang2(helpers.get("evalPrint"), q));
					MemorySegment r = c.tryEval(e, helperEnv, true);
					c.unprotect(2);
					if (r == null) {
						if (interruptRequested) {
							console.write("\n", true);
						}
						break;
					}
				}
			} else {
				MemorySegment e = c.protect(c.lang2(helpers.get("parseError"), code));
				c.tryEval(e, helperEnv, true);
				c.unprotect(1);
			}
			c.unprotect(3);
			flushCapture();
			console.prompt("> ");
		} finally {
			busy = false;
			activity.incrementAndGet();
			console.busy(false);
			signalInputRequest();
		}
	}

	/** Returns true if an interrupt was requested for the current evaluation (e.g., to end a wait in a callback). */
	public boolean isInterruptRequested() {
		return interruptRequested;
	}

	/**
	 * Records the plots of a device (a history of up to max plots, before each new
	 * page of base graphics and grid).
	 */
	public void enableHistory(RGraphicsDevice device, int max) throws RException {
		call(() -> {
			int n = devices.number(device);
			if (n < 1) {
				throw new RException("The device is not open in R");
			}
			MemorySegment num = c.protect(c.scalarInteger(n));
			MemorySegment mx = c.protect(c.scalarInteger(Math.max(1, max)));
			MemorySegment call = c.protect(c.lang3(helpers.get("historyEnable"), num, mx));
			MemorySegment r = c.tryEval(call, helperEnv, true);
			c.unprotect(3);
			if (r == null) {
				throw new RException(errorMessage());
			}
			return null;
		});
	}

	/**
	 * Moves in the history of a device (-1 back, +1 forward, 0 only the state) and
	 * draws that plot.
	 *
	 * @return the position (1 = the oldest plot, 0 = the current plot) and the
	 *         number of recorded plots
	 */
	public int[] history(RGraphicsDevice device, int step) throws RException {
		return call(() -> {
			int n = devices.number(device);
			if (n < 1) {
				return new int[] { 0, 0 };
			}
			MemorySegment num = c.protect(c.scalarInteger(n));
			MemorySegment st = c.protect(c.scalarInteger(step));
			MemorySegment call = c.protect(c.lang3(helpers.get("historyMove"), num, st));
			MemorySegment r = c.tryEval(call, helperEnv, true);
			c.unprotect(3);
			if (r == null) {
				throw new RException(errorMessage());
			}
			c.protect(r);
			try {
				return ((RValue.Int) convert.toJava(r)).values();
			} finally {
				c.unprotect(1);
			}
		});
	}

	/* Clears the interrupt flag of R (thread of R) */
	private void clearPendingInterrupt() {
		n.setInt(n.os.equals("Windows") ? "UserBreak" : "R_interrupts_pending", 0);
	}

	/** Interrupts the evaluation (like Ctrl+C in R). */
	public void interrupt() {
		if (!alive) {
			return;
		}
		interruptRequested = true;
		n.setInt(n.os.equals("Windows") ? "UserBreak" : "R_interrupts_pending", 1);
	}

	/* ---------------- Graphics devices ---------------- */

	/**
	 * Defines an R function which opens a Java device (e.g.,
	 * registerDevice("bio7Device", factory) and options(device = "bio7Device")).
	 * The function is in the environment "bio7ffm" of the search path; its
	 * arguments are width, height (inches) and pointsize.
	 */
	public void registerDevice(String rFunctionName, RDeviceFactory factory) throws RException {
		call(() -> {
			if (deviceCallPointer == null) {
				MemorySegment stub = stub("deviceCall", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
				deviceCallPointer = c.makeExternalPtr(stub, c.install("native symbol"));
				c.preserve(deviceCallPointer);
			}
			int id = deviceFactories.size() + 1;
			deviceFactories.put(id, factory);
			deviceNames.put(id, rFunctionName);
			MemorySegment nm = c.protect(c.mkString(rFunctionName));
			MemorySegment idv = c.protect(c.scalarInteger(id));
			MemorySegment full = c.protect(c.lang4(helpers.get("defineDevice"), nm, idv, deviceCallPointer));
			MemorySegment r = c.tryEval(full, helperEnv, true);
			c.unprotect(3);
			if (r == null) {
				throw new RException(errorMessage());
			}
			return null;
		});
	}

	/**
	 * Opens a Java device in R and makes it the current device.
	 *
	 * @return the number of the device in R
	 */
	public int openDevice(String name, RGraphicsDevice device, double dpi, double pointsize) throws RException {
		return call(() -> devices.open(name, device, dpi, pointsize));
	}

	/** The number of a Java device in R (as dev.cur()) or -1 if it is closed. */
	public int deviceNumber(RGraphicsDevice device) throws RException {
		return call(() -> devices.number(device));
	}

	/**
	 * Resizes a device (e.g., a Java2D or a recording device) and draws the plot
	 * again in the new size (asynchronous: returns at once, R replays when it is
	 * idle).
	 */
	public void resizeDevice(com.eco.bio7.rffm.graphics.ResizableGraphicsDevice device, int width, int height) {
		if (!alive) {
			return;
		}
		/*
		 * Only the latest size is drawn, when the size didn't change for a moment
		 * (dragging a window border sends many sizes): one replay per resize
		 */
		pendingResize.put(device, new int[] { width, height });
		var previous = resizeTimers.put(device, resizeTimer.schedule(() -> queueReplay(device), RESIZE_DELAY_MS, TimeUnit.MILLISECONDS));
		if (previous != null) {
			previous.cancel(false);
		}
	}

	/** The time without a size change before a resized device is drawn again (milliseconds). */
	public static final long RESIZE_DELAY_MS = 250;

	private final Map<com.eco.bio7.rffm.graphics.ResizableGraphicsDevice, java.util.concurrent.ScheduledFuture<?>> resizeTimers = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.concurrent.ScheduledExecutorService resizeTimer = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "R device resize");
		t.setDaemon(true);
		return t;
	});

	private void queueReplay(com.eco.bio7.rffm.graphics.ResizableGraphicsDevice device) {
		resizeTimers.remove(device);
		if (!alive) {
			return;
		}
		tasks.add(() -> {
			int[] size = pendingResize.remove(device);
			if (size == null) {
				return;
			}
			/* One frame for the replay (not the intermediate drawing) */
			device.holdflush(1);
			try {
				int oldWidth = (int) device.width();
				int oldHeight = (int) device.height();
				device.resize(size[0], size[1]);
				if (!replay(device)) {
					/* The plot doesn't fit (e.g., "figure margins too large"): back to the previous size */
					device.resize(oldWidth, oldHeight);
					replay(device);
				}
			} finally {
				device.holdflush(-1);
			}
		});
	}

	private final Map<com.eco.bio7.rffm.graphics.ResizableGraphicsDevice, int[]> pendingResize = new java.util.concurrent.ConcurrentHashMap<>();

	/** Draws the plot of a device again (e.g., after its size changed), asynchronously. */
	public void replayDevice(RGraphicsDevice device) {
		if (alive) {
			tasks.add(() -> {
				replay(device);
			});
		}
	}

	/* Draws the plot again in the current size of the device; false if R raised an error */
	private boolean replay(RGraphicsDevice device) {
		if (!devices.resize(device)) {
			return true;
		}
		int number = devices.number(device);
		if (number < 1) {
			return true;
		}
		MemorySegment num = c.protect(c.scalarInteger(number));
		MemorySegment call = c.protect(c.lang2(helpers.get("replay"), num));
		MemorySegment r = c.tryEval(call, helperEnv, true);
		c.unprotect(2);
		return r != null;
	}

	/* ---------------- End ---------------- */

	/**
	 * Ends R (the finalizers run, the devices close). R can't be started again in
	 * this process.
	 */
	@Override
	public void close() {
		if (!alive) {
			return;
		}
		FutureTask<Void> f = new FutureTask<>(() -> {
			try {
				RNative.LINKER.downcallHandle(n.symbolAddress("Rf_endEmbeddedR"), FunctionDescriptor.ofVoid(JAVA_INT)).invokeExact(0);
			} catch (Throwable t) {
				/* ignored */
			}
			alive = false;
			return null;
		});
		/*
		 * A running computation is interrupted; R waiting in browser() (a debug
		 * session) leaves it with Q: an empty line would be "next" for browser() and R
		 * would wait at the next statement until the time limit
		 */
		if (busy && !readingConsole) {
			interrupt();
		}
		tasks.add(f);
		enqueue(readingConsole ? "Q" : "");
		try {
			f.get(15, TimeUnit.SECONDS);
		} catch (Exception e) {
			alive = false;
		}
	}

	/** The text of a native string (UTF-8). */
	static String utf8(MemorySegment s) {
		return s.reinterpret(Integer.MAX_VALUE).getString(0, StandardCharsets.UTF_8);
	}

	/** The type of an argument of a callback (for the method lookup). */
	static MethodType type(FunctionDescriptor fd) {
		return fd.toMethodType();
	}
}
