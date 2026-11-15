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
package com.eco.bio7.pyffm;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

import com.eco.bio7.pyffm.internal.Codec;
import com.eco.bio7.pyffm.internal.Json;
import com.eco.bio7.pyffm.internal.PyNative;
import com.eco.bio7.pyffm.internal.PyProbe;

/**
 * An embedded CPython in the Java process (Foreign Function and Memory API):
 * one per process, it can't be stopped and started again. All Python code runs
 * in one thread ("Python"); the calls of other threads wait for it.
 *
 * <pre>
 * PySession py = PySession.start(PyStartOptions.of(Path.of("/usr/bin/python3")).withConsole((t, e) -> System.out.print(t)));
 * py.assign("x", PyValue.of(1.0, 2.0, 3.0));
 * PyValue mean = py.eval("float(sum(x) / len(x))");
 * py.console("print(x)");
 * </pre>
 */
public final class PySession {

	private static final Pattern NAME = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
	private static volatile PySession current;
	private static volatile boolean started;

	private final PyNative nat;
	private final PyProbe.Installation installation;
	private final PyConsole console;
	private final ExecutorService executor;
	private volatile Thread thread;
	private final AtomicInteger running = new AtomicInteger();
	/* The number of the calls of Python (also short ones): a change of the workspace is possible */
	private final java.util.concurrent.atomic.AtomicLong activity = new java.util.concurrent.atomic.AtomicLong();
	private volatile boolean interruptRequested;
	private long mainIdent;
	private volatile PyPlotListener plotListener;
	private volatile PyCallHandler callHandler;
	private volatile BooleanSupplier waitLimited;
	private volatile long waitLimitMillis;
	private volatile int[] openFigures = new int[0];

	/* The reply of the current call (Python thread) */
	private PyValue reply;
	private String replyError;

	private PySession(PyNative nat, PyProbe.Installation installation, PyConsole console) {
		this.nat = nat;
		this.installation = installation;
		this.console = console;
		this.executor = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(null, r, "Python", 16L << 20);
			t.setDaemon(true);
			thread = t;
			return t;
		});
	}

	/**
	 * Starts the embedded Python (once per process).
	 *
	 * @throws PyException if Python can't be found or started
	 */
	public static synchronized PySession start(PyStartOptions options) throws PyException {
		if (current != null) {
			return current;
		}
		if (started) {
			throw new PyException("The embedded Python can only be started once per process.");
		}
		PyProbe.Installation inst = PyProbe.probe(options.executable());
		PyNative nat = PyNative.load(inst.library(), inst.home());
		started = true;
		PySession session = new PySession(nat, inst, options.console());
		session.call(() -> {
			session.initialize();
			return null;
		}, 0);
		current = session;
		return session;
	}

	/** The running session or null. */
	public static PySession current() {
		return current;
	}

	/** Returns true if the embedded Python was started in this process. */
	public static boolean wasStarted() {
		return started;
	}

	/** The version of Python (e.g., "3.12.4"). */
	public String version() {
		return installation.version();
	}

	/** The executable of the selected Python. */
	public Path executable() {
		return installation.executable();
	}

	/** The shared library of Python. */
	public Path library() {
		return installation.library();
	}

	/**
	 * The activity of Python: the number of the calls so far (e.g., to update a
	 * display after Python ran code).
	 */
	public long getActivity() {
		return activity.get();
	}

	/** Returns true if Python evaluates (a call, a console line, a script). */
	public boolean isBusy() {
		return running.get() > 0;
	}

	/** Returns true in the thread of Python (e.g., in a call of Python to Java). */
	public boolean isPythonThread() {
		return Thread.currentThread() == thread;
	}

	/**
	 * Limits the time a thread waits for Python (e.g., the UI thread): the call
	 * fails with an exception instead of blocking longer.
	 */
	public void setWaitLimit(BooleanSupplier isLimited, long millis) {
		this.waitLimited = isLimited;
		this.waitLimitMillis = millis;
	}

	public void setPlotListener(PyPlotListener listener) {
		this.plotListener = listener;
	}

	public void setCallHandler(PyCallHandler handler) {
		this.callHandler = handler;
	}

	/** The numbers of the open figures (matplotlib). */
	public int[] openFigures() {
		return openFigures.clone();
	}

	/* ---------------- The initialization ---------------- */

	private void initialize() throws Exception {
		nat.initialize();
		if (!nat.isInitialized()) {
			throw new PyException("Python could not be initialized");
		}
		String source;
		try (InputStream in = PySession.class.getResourceAsStream("internal/bio7_bootstrap.py")) {
			if (in == null) {
				throw new PyException("The bootstrap of Python is missing");
			}
			source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new PyException("The bootstrap of Python could not be read", e);
		}
		Arena g = nat.arena;
		MemorySegment src = g.allocateFrom(source);
		boolean ok = nat.runSimple("import ctypes as _c, types as _t, sys as _s\n_m = _t.ModuleType('_bio7')\n_s.modules['_bio7'] = _m\n"
				+ "exec(compile(_c.string_at(" + src.address() + ", " + (src.byteSize() - 1) + ").decode('utf-8'), '<_bio7>', 'exec'), _m.__dict__)\n"
				+ "del _c, _t, _s, _m\n");
		if (!ok) {
			throw new PyException("The bootstrap of Python failed");
		}
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		MemorySegment write = PyNative.LINKER.upcallStub(lookup.findStatic(PySession.class, "upWrite", MethodType.methodType(void.class, int.class, MemorySegment.class, long.class)),
				FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS, JAVA_LONG), g);
		MemorySegment replyStub = PyNative.LINKER.upcallStub(
				lookup.findStatic(PySession.class, "upReply", MethodType.methodType(void.class, int.class, MemorySegment.class, long.class, MemorySegment.class, long.class)),
				FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS, JAVA_LONG, ADDRESS, JAVA_LONG), g);
		MemorySegment callStub = PyNative.LINKER.upcallStub(
				lookup.findStatic(PySession.class, "upCall",
						MethodType.methodType(int.class, MemorySegment.class, long.class, MemorySegment.class, long.class, MemorySegment.class, long.class)),
				FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG, ADDRESS, JAVA_LONG, ADDRESS, JAVA_LONG), g);
		MemorySegment plotStub = PyNative.LINKER.upcallStub(
				lookup.findStatic(PySession.class, "upPlot", MethodType.methodType(void.class, int.class, MemorySegment.class, int.class, int.class, double.class)),
				FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_DOUBLE), g);
		MemorySegment figsStub = PyNative.LINKER.upcallStub(lookup.findStatic(PySession.class, "upFigures", MethodType.methodType(void.class, MemorySegment.class, long.class)),
				FunctionDescriptor.ofVoid(ADDRESS, JAVA_LONG), g);
		Map<String, Object> cfg = new LinkedHashMap<>();
		cfg.put("path", installation.sysPath());
		cfg.put("executable", installation.executable().toString());
		MemorySegment c = g.allocateFrom(Json.write(cfg));
		current = this;
		ok = nat.runSimple("__import__('_bio7')._init(" + write.address() + ", " + replyStub.address() + ", " + callStub.address() + ", " + plotStub.address() + ", "
				+ figsStub.address() + ", " + c.address() + ", " + (c.byteSize() - 1) + ")\n");
		if (!ok) {
			current = null;
			throw new PyException("The initialization of Python failed");
		}
		/* The id of the thread for an interrupt (threading.get_ident) */
		reply = null;
		nat.runSimple("__import__('_bio7')._reply(__import__('_bio7')._main_ident)\n");
		mainIdent = reply instanceof PyValue.Array a && a.data() instanceof long[] l ? l[0] : 0;
		/* The GIL is taken by each call (also by the threads of Python) */
		nat.saveThread();
	}

	/* ---------------- The thread of Python ---------------- */

	/* Runs a task with the GIL (in the thread of Python) */
	private <T> T withGil(Callable<T> task) throws Exception {
		activity.incrementAndGet();
		running.incrementAndGet();
		int gil = nat.isInitialized() ? nat.ensureGil() : -1;
		try {
			return task.call();
		} finally {
			if (gil >= 0) {
				clearInterrupt();
				nat.releaseGil(gil);
			}
			running.decrementAndGet();
		}
	}

	/*
	 * Runs in the thread of Python with the GIL (directly in a call of Python to
	 * Java: ctypes released the GIL for the upcall, it is taken again)
	 */
	private <T> T call(Callable<T> task, long timeoutMillis) throws PyException {
		if (isPythonThread()) {
			try {
				return withGil(task);
			} catch (PyException e) {
				throw e;
			} catch (Throwable e) {
				throw new PyException(String.valueOf(e), e);
			}
		}
		Future<T> f = executor.submit(() -> withGil(task));
		try {
			BooleanSupplier limited = waitLimited;
			if (timeoutMillis <= 0 && limited != null && limited.getAsBoolean() && waitLimitMillis > 0) {
				timeoutMillis = waitLimitMillis;
			}
			return timeoutMillis > 0 ? f.get(timeoutMillis, TimeUnit.MILLISECONDS) : f.get();
		} catch (TimeoutException e) {
			throw new PyException("Python is busy (timeout)");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new PyException("Interrupted");
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof PyException p) {
				throw p;
			}
			throw new PyException(String.valueOf(cause), cause);
		}
	}

	/* Queues a task (plots, prompt): returns at once */
	private void submit(Callable<?> task) {
		submit(task, false);
	}

	/* Queues a task; user code (console, scripts) notifies the activity listener afterwards */
	private void submit(Callable<?> task, boolean userCode) {
		executor.submit(() -> {
			try {
				withGil(task);
			} catch (PyException e) {
				write(e.getMessage() + "\n", true);
			} catch (Throwable e) {
				write(e + "\n", true);
			}
			if (userCode) {
				Runnable l = activityListener;
				if (l != null) {
					try {
						l.run();
					} catch (Throwable e) {
						/* The listener must not stop the queue */
					}
				}
			}
			return null;
		});
	}

	private volatile Runnable activityListener;

	/**
	 * Sets a listener called after user code ran (a console line, a script, code
	 * of runAsync), e.g., to update a list of the variables. Called in the thread
	 * of Python (after the code, without the GIL): must return at once.
	 */
	public void setActivityListener(Runnable listener) {
		this.activityListener = listener;
	}

	private void clearInterrupt() throws PyException {
		if (interruptRequested) {
			interruptRequested = false;
			nat.asyncExc(mainIdent, false);
		}
	}

	/* Runs code of the bootstrap: the text is passed in native memory (no quoting) */
	private void run(String mode, String text) throws PyException {
		try (Arena a = Arena.ofConfined()) {
			MemorySegment s = a.allocateFrom(text);
			reply = null;
			replyError = null;
			boolean ok = nat.runSimple("__import__('_bio7')._run(" + s.address() + ", " + (s.byteSize() - 1) + ", '" + mode + "')\n");
			if (replyError != null) {
				String e = replyError;
				replyError = null;
				throw new PyException(e.strip());
			}
			if (!ok) {
				throw new PyException("Python failed (see the console)");
			}
		}
	}

	/* ---------------- The API ---------------- */

	/** Executes statements in __main__ (raises an exception with the traceback). */
	public void exec(String code) throws PyException {
		call(() -> {
			run("exec", code);
			return null;
		}, 0);
	}

	/** Evaluates an expression in __main__ and returns its value. */
	public PyValue eval(String expression) throws PyException {
		return call(() -> {
			run("eval", expression);
			PyValue v = reply;
			reply = null;
			return v == null ? PyValue.None.INSTANCE : v;
		}, 0);
	}

	/** The value of a variable of __main__. */
	public PyValue get(String name) throws PyException {
		return eval(checkName(name));
	}

	/** Assigns a value to a variable of __main__ (numbers as numpy arrays). */
	public void assign(String name, PyValue value) throws PyException {
		checkName(name);
		call(() -> {
			put(name, value, 0);
			return null;
		}, 0);
	}

	public void assign(String name, double... values) throws PyException {
		assign(name, PyValue.of(values));
	}

	public void assign(String name, int... values) throws PyException {
		assign(name, PyValue.of(values));
	}

	public void assign(String name, String... values) throws PyException {
		assign(name, PyValue.of(values));
	}

	public void assign(String name, boolean... values) throws PyException {
		assign(name, PyValue.of(values));
	}

	/** A 2-d array (rows). */
	public void assign(String name, double[][] matrix) throws PyException {
		assign(name, PyValue.matrix(matrix));
	}

	/* Python thread: target 0 __main__, 1 the result of a call of Python */
	private void put(String name, PyValue value, int target) throws PyException {
		try (Arena a = Arena.ofConfined()) {
			Codec.Encoded e = Codec.encode(value, a);
			MemorySegment h = a.allocateFrom(e.header());
			replyError = null;
			nat.runSimple("__import__('_bio7')._put('" + name + "', " + h.address() + ", " + (h.byteSize() - 1) + ", " + e.pointers().address() + ", " + e.count() + ", "
					+ target + ")\n");
			if (replyError != null) {
				String err = replyError;
				replyError = null;
				throw new PyException(err.strip());
			}
		}
	}

	/** Deletes a variable of __main__ (if it exists). */
	public void remove(String name) throws PyException {
		exec("globals().pop('" + checkName(name) + "', None)");
	}

	/**
	 * A line typed in the console (statements of several lines are completed with
	 * the next lines): the output and the prompt go to the console. Returns at
	 * once.
	 */
	public void console(String line) {
		submit(() -> {
			run("console", line);
			return null;
		}, true);
	}

	/** Writes the prompt of the console. */
	public void prompt() {
		submit(() -> {
			nat.runSimple("__import__('_bio7')._prompt(False)\n");
			return null;
		});
	}

	/**
	 * Runs a script file in __main__ (errors are printed to the console), then
	 * draws the new figures. Returns at once.
	 */
	public void runFile(Path file) {
		submit(() -> {
			run("file", file.toAbsolutePath().toString());
			return null;
		}, true);
	}

	/** Runs code in __main__ like a script (errors printed to the console). Returns at once. */
	public void runAsync(String code) {
		submit(() -> {
			try {
				run("exec", code);
			} catch (PyException e) {
				write(e.getMessage() + "\n", true);
			}
			nat.runSimple("__import__('_bio7')._flush()\n");
			return null;
		}, true);
	}

	/**
	 * Interrupts the running Python code (KeyboardInterrupt), as Ctrl+C. Has no
	 * effect if Python is idle.
	 */
	public void interrupt() {
		if (!isBusy() || mainIdent == 0) {
			return;
		}
		Thread t = new Thread(() -> {
			try {
				int gil = nat.ensureGil();
				try {
					if (isBusy()) {
						interruptRequested = true;
						nat.asyncExc(mainIdent, true);
					}
				} finally {
					nat.releaseGil(gil);
				}
			} catch (Throwable e) {
				/* Not interrupted */
			}
		}, "Python interrupt");
		t.setDaemon(true);
		t.start();
	}

	/* ---------------- The plot device (matplotlib) ---------------- */

	/**
	 * Sets the size of the plot view and draws the figure again in that size (in
	 * the thread of Python, after the running code).
	 *
	 * @param figure the figure to draw (0: none)
	 */
	public void resizePlot(int figure, int width, int height, double scale) {
		submit(() -> {
			nat.runSimple("__import__('_bio7')._resize(" + figure + ", " + width + ", " + height + ", " + scale + ")\n");
			return null;
		});
	}

	/** Draws a figure again (e.g., to show it in the view). */
	public void renderFigure(int figure) {
		submit(() -> {
			nat.runSimple("__import__('_bio7')._render(" + figure + ")\n");
			return null;
		});
	}

	/** Closes a figure (-1: all). */
	public void closeFigure(int figure) {
		submit(() -> {
			nat.runSimple("__import__('_bio7')._close(" + figure + ")\n");
			return null;
		});
	}

	/**
	 * Saves a figure (matplotlib savefig).
	 *
	 * @param format png, svg, pdf, eps, jpg, tif
	 * @param dpi    the resolution (0: the one of the figure)
	 */
	public void saveFigure(int figure, Path file, String format, int dpi) throws PyException {
		assign("_bio7_save_path", PyValue.of(file.toAbsolutePath().toString()));
		exec("__import__('_bio7')._save(" + figure + ", _bio7_save_path, '" + format.replaceAll("[^a-z]", "") + "', " + dpi + ")\ndel _bio7_save_path");
	}

	/* ---------------- Upcalls (from Python) ---------------- */

	private void write(String text, boolean error) {
		PyConsole c = console;
		if (c != null) {
			c.write(text, error);
		} else {
			(error ? System.err : System.out).print(text);
		}
	}

	private static String utf8(MemorySegment p, long len) {
		return len <= 0 ? "" : new String(p.reinterpret(len).toArray(JAVA_BYTE), StandardCharsets.UTF_8);
	}

	static void upWrite(int stream, MemorySegment text, long len) {
		try {
			PySession s = current;
			if (s != null) {
				s.write(utf8(text, len), stream == 1);
			}
		} catch (Throwable e) {
			/* Never into the native frames */
		}
	}

	static void upReply(int kind, MemorySegment header, long len, MemorySegment pointers, long count) {
		PySession s = current;
		if (s == null) {
			return;
		}
		try {
			if (kind == 1) {
				s.replyError = utf8(header, len);
			} else {
				s.reply = Codec.decode(utf8(header, len), pointers, count);
			}
		} catch (Throwable e) {
			s.replyError = "The value could not be transferred: " + e;
		}
	}

	static int upCall(MemorySegment request, long len, MemorySegment header, long hlen, MemorySegment pointers, long count) {
		PySession s = current;
		if (s == null) {
			return 1;
		}
		try {
			String[] parts = utf8(request, len).split("\u0001", -1);
			String function = parts[0];
			String[] args = new String[parts.length - 1];
			System.arraycopy(parts, 1, args, 0, args.length);
			PyValue value = hlen > 0 ? Codec.decode(utf8(header, hlen), pointers, count) : null;
			PyCallHandler h = s.callHandler;
			if (h == null) {
				throw new IllegalStateException("No functions of the application are available");
			}
			PyValue result = h.call(function, args, value);
			s.withGil(() -> {
				s.put("", result == null ? PyValue.None.INSTANCE : result, 1);
				return null;
			});
			return 0;
		} catch (Throwable e) {
			try {
				String message = e.getMessage() != null ? e.getMessage() : e.toString();
				s.withGil(() -> {
					s.put("", PyValue.of(message), 1);
					return null;
				});
			} catch (Throwable ignored) {
				/* No message */
			}
			return 1;
		}
	}

	static void upPlot(int figure, MemorySegment rgba, int width, int height, double scale) {
		try {
			PySession s = current;
			PyPlotListener l = s == null ? null : s.plotListener;
			if (l != null && width > 0 && height > 0) {
				byte[] data = rgba.reinterpret((long) width * height * 4).toArray(JAVA_BYTE);
				l.figure(figure, data, width, height, scale);
			}
		} catch (Throwable e) {
			/* Never into the native frames */
		}
	}

	@SuppressWarnings("unchecked")
	static void upFigures(MemorySegment json, long len) {
		try {
			PySession s = current;
			if (s == null) {
				return;
			}
			List<Object> nums = (List<Object>) Json.parse(utf8(json, len));
			int[] open = new int[nums.size()];
			for (int i = 0; i < open.length; i++) {
				open[i] = ((Number) nums.get(i)).intValue();
			}
			s.openFigures = open;
			PyPlotListener l = s.plotListener;
			if (l != null) {
				l.figuresChanged(open.clone());
			}
		} catch (Throwable e) {
			/* Never into the native frames */
		}
	}

	private static String checkName(String name) {
		if (name == null || !NAME.matcher(name).matches()) {
			throw new IllegalArgumentException("Not a valid Python name: " + name);
		}
		return name;
	}

	/** The variables of __main__ (name and type), e.g., for an object browser. */
	public List<String[]> variables() throws PyException {
		PyValue v = eval("[[k, type(v).__name__] for k, v in globals().items() if not k.startswith('_') and type(v).__name__ not in ('module', 'function', 'type', 'builtin_function_or_method')]");
		List<String[]> result = new ArrayList<>();
		if (v instanceof PyValue.PyList l) {
			for (PyValue e : l.values()) {
				if (e instanceof PyValue.Str s && s.length() == 2) {
					result.add(s.values());
				}
			}
		}
		return result;
	}
}
