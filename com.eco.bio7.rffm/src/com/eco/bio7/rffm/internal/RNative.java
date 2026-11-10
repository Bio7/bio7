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

package com.eco.bio7.rffm.internal;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The binding to the shared library of R (libR.dylib, libR.so, R.dll) with the
 * Foreign Function and Memory API: the functions and the global variables of the
 * C API of R which are used by the embedded session.
 * <p>
 * All functions must be called in the thread of R (see RThread). The functions
 * of R which can raise an R error (a longjmp) must not be called directly from
 * Java: an error is only safe inside R_tryEval, which catches it in C.
 */
public final class RNative {

	/** SEXP types (Rinternals.h). */
	public static final int NILSXP = 0, SYMSXP = 1, LISTSXP = 2, CLOSXP = 3, ENVSXP = 4, LANGSXP = 6, CHARSXP = 9, LGLSXP = 10, INTSXP = 13,
			REALSXP = 14, CPLXSXP = 15, STRSXP = 16, VECSXP = 19, EXPRSXP = 20, EXTPTRSXP = 22, RAWSXP = 24;
	/** cetype_t: CE_UTF8. */
	public static final int CE_UTF8 = 1;

	public static final Linker LINKER = Linker.nativeLinker();

	public final String os;
	public final Path home;
	public final Arena arena = Arena.global();
	private final SymbolLookup r;
	private final SymbolLookup libc;
	/** The charset of the console (the native encoding of R). */
	public final Charset consoleCharset;

	/* The C API */
	final MethodHandle Rf_initialize_R, setup_Rmainloop, Rf_endEmbeddedR, R_RunExitFinalizers;
	final MethodHandle R_ParseVector, R_tryEval, R_tryEvalSilent, Rf_protect, Rf_unprotect, R_PreserveObject, R_ReleaseObject;
	final MethodHandle Rf_install, Rf_mkCharCE, Rf_allocVector, Rf_lang1, Rf_lang2, Rf_lang3, Rf_lang4, Rf_findVar, Rf_ScalarReal, Rf_ScalarInteger;
	final MethodHandle TYPEOF, Rf_xlength, REAL, INTEGER, LOGICAL, RAW, STRING_ELT, SET_STRING_ELT, VECTOR_ELT, SET_VECTOR_ELT, PRINTNAME;
	final MethodHandle Rf_getAttrib, Rf_setAttrib, Rf_translateCharUTF8, R_CHAR, R_MakeExternalPtr, vmaxget, vmaxset;
	/* The graphics engine */
	final MethodHandle GEcreateDD, GEcreateDevDesc, GEaddDevice2, GEinitDisplayList, GEdeviceNumber, Rf_desc2GEDesc, R_CheckDeviceAvailableBool,
			GEkillDevice;
	/* Windows */
	MethodHandle R_setStartTime, R_DefParamsEx, R_SetParams, R_set_command_line_arguments, getRUser, readconsolecfg;
	/** GA_initapp of Rgraphapp.dll (Windows) or null. */
	public MethodHandle GA_initapp;
	/* libc */
	final MethodHandle calloc;

	private RNative(String os, Path home, SymbolLookup r, SymbolLookup libc, Charset consoleCharset) {
		this.os = os;
		this.home = home;
		this.r = r;
		this.libc = libc;
		this.consoleCharset = consoleCharset;
		Rf_initialize_R = os.equals("Windows") ? null : fn("Rf_initialize_R", JAVA_INT, JAVA_INT, ADDRESS);
		setup_Rmainloop = fnVoid("setup_Rmainloop");
		Rf_endEmbeddedR = fnVoid("Rf_endEmbeddedR", JAVA_INT);
		R_RunExitFinalizers = fnVoid("R_RunExitFinalizers");
		R_ParseVector = fn("R_ParseVector", ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS);
		R_tryEval = fn("R_tryEval", ADDRESS, ADDRESS, ADDRESS, ADDRESS);
		R_tryEvalSilent = fn("R_tryEvalSilent", ADDRESS, ADDRESS, ADDRESS, ADDRESS);
		Rf_protect = fn("Rf_protect", ADDRESS, ADDRESS);
		Rf_unprotect = fnVoid("Rf_unprotect", JAVA_INT);
		R_PreserveObject = fnVoid("R_PreserveObject", ADDRESS);
		R_ReleaseObject = fnVoid("R_ReleaseObject", ADDRESS);
		Rf_install = fn("Rf_install", ADDRESS, ADDRESS);
		Rf_mkCharCE = fn("Rf_mkCharCE", ADDRESS, ADDRESS, JAVA_INT);
		Rf_allocVector = fn("Rf_allocVector", ADDRESS, JAVA_INT, JAVA_LONG);
		Rf_lang1 = fn("Rf_lang1", ADDRESS, ADDRESS);
		Rf_lang2 = fn("Rf_lang2", ADDRESS, ADDRESS, ADDRESS);
		Rf_lang3 = fn("Rf_lang3", ADDRESS, ADDRESS, ADDRESS, ADDRESS);
		Rf_lang4 = fn("Rf_lang4", ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS);
		Rf_findVar = fn("Rf_findVar", ADDRESS, ADDRESS, ADDRESS);
		Rf_ScalarReal = fn("Rf_ScalarReal", ADDRESS, JAVA_DOUBLE);
		Rf_ScalarInteger = fn("Rf_ScalarInteger", ADDRESS, JAVA_INT);
		TYPEOF = fn("TYPEOF", JAVA_INT, ADDRESS);
		Rf_xlength = fn("Rf_xlength", JAVA_LONG, ADDRESS);
		REAL = fn("REAL", ADDRESS, ADDRESS);
		INTEGER = fn("INTEGER", ADDRESS, ADDRESS);
		LOGICAL = fn("LOGICAL", ADDRESS, ADDRESS);
		RAW = fn("RAW", ADDRESS, ADDRESS);
		STRING_ELT = fn("STRING_ELT", ADDRESS, ADDRESS, JAVA_LONG);
		SET_STRING_ELT = fnVoid("SET_STRING_ELT", ADDRESS, JAVA_LONG, ADDRESS);
		VECTOR_ELT = fn("VECTOR_ELT", ADDRESS, ADDRESS, JAVA_LONG);
		SET_VECTOR_ELT = fn("SET_VECTOR_ELT", ADDRESS, ADDRESS, JAVA_LONG, ADDRESS);
		PRINTNAME = fn("PRINTNAME", ADDRESS, ADDRESS);
		Rf_getAttrib = fn("Rf_getAttrib", ADDRESS, ADDRESS, ADDRESS);
		Rf_setAttrib = fn("Rf_setAttrib", ADDRESS, ADDRESS, ADDRESS, ADDRESS);
		Rf_translateCharUTF8 = fn("Rf_translateCharUTF8", ADDRESS, ADDRESS);
		R_CHAR = fn("R_CHAR", ADDRESS, ADDRESS);
		R_MakeExternalPtr = fn("R_MakeExternalPtr", ADDRESS, ADDRESS, ADDRESS, ADDRESS);
		vmaxget = fn("vmaxget", ADDRESS);
		vmaxset = fnVoid("vmaxset", ADDRESS);
		GEcreateDD = optionalFn("GEcreateDD", FunctionDescriptor.of(ADDRESS));
		GEcreateDevDesc = fn("GEcreateDevDesc", ADDRESS, ADDRESS);
		GEaddDevice2 = fnVoid("GEaddDevice2", ADDRESS, ADDRESS);
		GEinitDisplayList = fnVoid("GEinitDisplayList", ADDRESS);
		GEdeviceNumber = fn("GEdeviceNumber", JAVA_INT, ADDRESS);
		Rf_desc2GEDesc = fn("Rf_desc2GEDesc", ADDRESS, ADDRESS);
		R_CheckDeviceAvailableBool = fn("R_CheckDeviceAvailableBool", JAVA_INT);
		GEkillDevice = fnVoid("GEkillDevice", ADDRESS);
		calloc = LINKER.downcallHandle(libc.find("calloc").orElseThrow(), FunctionDescriptor.of(ADDRESS, JAVA_LONG, JAVA_LONG));
		if (os.equals("Windows")) {
			R_setStartTime = fnVoid("R_setStartTime");
			R_DefParamsEx = fn("R_DefParamsEx", JAVA_INT, ADDRESS, JAVA_INT);
			R_SetParams = fnVoid("R_SetParams", ADDRESS);
			R_set_command_line_arguments = fnVoid("R_set_command_line_arguments", JAVA_INT, ADDRESS);
			getRUser = fn("getRUser", ADDRESS);
			readconsolecfg = fnVoid("readconsolecfg");
		}
	}

	/**
	 * Loads R of a home folder: the libraries R depends on (also if the
	 * installation was moved), libR (global symbols for the packages) and sets the
	 * environment (R_HOME and the folders of the R start script).
	 */
	public static RNative load(Path home) throws IOException {
		String os = os();
		Path lib = libraryOf(home, os);
		if (lib == null) {
			throw new IOException("The shared library of R (libR) was not found in " + home
					+ ". R must be built as a shared library (e.g., ./configure --enable-R-shlib).");
		}
		Arena arena = Arena.global();
		SymbolLookup libc = os.equals("Windows") ? SymbolLookup.libraryLookup("ucrtbase.dll", arena) : LINKER.defaultLookup();
		/* The environment before R is loaded */
		setenv(libc, os, "R_HOME", home.toString());
		if (!os.equals("Windows")) {
			/* A UTF-8 locale (e.g., an application started from the Finder has no LANG): else R runs in the C locale */
			if (System.getenv("LANG") == null && System.getenv("LC_ALL") == null && System.getenv("LC_CTYPE") == null) {
				Locale l = Locale.getDefault();
				String country = l.getCountry().isEmpty() ? "US" : l.getCountry();
				String language = l.getLanguage().isEmpty() ? "en" : l.getLanguage();
				setenv(libc, os, "LANG", language + "_" + country + ".UTF-8");
			}
			for (var entry : scriptDirectories(home).entrySet()) {
				if (System.getenv(entry.getKey()) == null) {
					setenv(libc, os, entry.getKey(), entry.getValue());
				}
			}
		}
		/* The libraries of R first (found by their names when libR is loaded) */
		preload(arena, os, lib.getParent(), libc, false);
		dlopenGlobal(libc, os, lib);
		/* The libraries which need R (LAPACK): found by their names by the packages (e.g., stats) */
		preload(arena, os, lib.getParent(), libc, true);
		SymbolLookup r = SymbolLookup.libraryLookup(lib, arena);
		String nativeEncoding = System.getProperty("native.encoding");
		Charset charset = os.equals("Windows") && nativeEncoding != null && Charset.isSupported(nativeEncoding) ? Charset.forName(nativeEncoding)
				: java.nio.charset.StandardCharsets.UTF_8;
		RNative n = new RNative(os, home, r, libc, charset);
		if (os.equals("Windows")) {
			/* GA_initapp is in Rgraphapp.dll */
			Path graphapp = lib.getParent().resolve("Rgraphapp.dll");
			if (Files.exists(graphapp)) {
				SymbolLookup ga = SymbolLookup.libraryLookup(graphapp, arena);
				n.GA_initapp = LINKER.downcallHandle(ga.find("GA_initapp").orElseThrow(), FunctionDescriptor.of(JAVA_INT, JAVA_INT, ADDRESS));
			}
		}
		return n;
	}

	/** The shared library of R in a home folder or null. */
	public static Path libraryOf(Path home, String os) {
		List<Path> candidates = switch (os) {
		case "Windows" -> List.of(home.resolve("bin/x64/R.dll"), home.resolve("bin/R.dll"));
		case "Mac" -> List.of(home.resolve("lib/libR.dylib"));
		default -> List.of(home.resolve("lib/libR.so"));
		};
		return candidates.stream().filter(Files::isRegularFile).findFirst().orElse(null);
	}

	/* The libraries of R in its folder: loaded in an order which satisfies their dependencies */
	private static void preload(Arena arena, String os, Path dir, SymbolLookup libc, boolean afterR) {
		List<Path> pending = new ArrayList<>();
		try (Stream<Path> files = Files.list(dir)) {
			files.filter(Files::isRegularFile).forEach(f -> {
				String name = f.getFileName().toString().toLowerCase(Locale.ROOT);
				boolean library = os.equals("Windows") ? name.endsWith(".dll") : os.equals("Mac") ? name.endsWith(".dylib") : name.contains(".so");
				/* Not R itself (loaded afterwards), not the alternative BLAS of macOS, not LAPACK (needs R) */
				if (library && !name.startsWith("libr.") && !name.equals("r.dll") && !name.contains("veclib") && name.contains("lapack") == afterR) {
					pending.add(f);
				}
			});
		} catch (IOException e) {
			return;
		}
		boolean progress = true;
		while (!pending.isEmpty() && progress) {
			progress = false;
			for (var it = pending.iterator(); it.hasNext();) {
				Path f = it.next();
				try {
					dlopenGlobal(libc, os, f);
					it.remove();
					progress = true;
				} catch (RuntimeException | IOException e) {
					/* Retried after the others (a dependency is missing) */
				}
			}
		}
	}

	/* dlopen(RTLD_NOW | RTLD_GLOBAL): the symbols of R for the packages (System.load on Windows) */
	private static void dlopenGlobal(SymbolLookup libc, String os, Path lib) throws IOException {
		if (os.equals("Windows")) {
			try {
				System.load(lib.toString());
			} catch (UnsatisfiedLinkError e) {
				throw new IOException(e.getMessage());
			}
			return;
		}
		try (Arena a = Arena.ofConfined()) {
			MethodHandle dlopen = LINKER.downcallHandle(libc.find("dlopen").orElseThrow(), FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));
			int flags = 2 | (os.equals("Mac") ? 0x8 : 0x100);
			MemorySegment handle = (MemorySegment) dlopen.invokeExact(a.allocateFrom(lib.toString()), flags);
			if (handle.address() == 0) {
				MethodHandle dlerror = LINKER.downcallHandle(libc.find("dlerror").orElseThrow(), FunctionDescriptor.of(ADDRESS));
				MemorySegment msg = (MemorySegment) dlerror.invokeExact();
				throw new IOException(msg.address() == 0 ? "dlopen failed: " + lib : msg.reinterpret(Integer.MAX_VALUE).getString(0));
			}
		} catch (IOException e) {
			throw e;
		} catch (Throwable t) {
			throw new IOException(t);
		}
	}

	/* The folders the R start script (bin/R) exports, e.g., R_SHARE_DIR=/usr/share/R/share on Debian */
	static java.util.Map<String, String> scriptDirectories(Path home) {
		java.util.Map<String, String> map = new java.util.LinkedHashMap<>();
		Path script = home.resolve("bin/R");
		String text = "";
		try {
			text = Files.readString(script, java.nio.charset.StandardCharsets.ISO_8859_1);
		} catch (IOException e) {
			/* Defaults below */
		}
		for (String key : new String[] { "R_SHARE_DIR", "R_INCLUDE_DIR", "R_DOC_DIR" }) {
			Matcher m = Pattern.compile("(?m)^\\s*" + key + "=\"?([^\"\\n;]+)\"?").matcher(text);
			String value = m.find() ? m.group(1).trim() : "${R_HOME}/" + key.substring(2, key.length() - 4).toLowerCase(Locale.ROOT);
			value = value.replace("${R_HOME}", home.toString()).replace("$R_HOME", home.toString());
			if (Files.isDirectory(Path.of(value))) {
				map.put(key, value);
			}
		}
		return map;
	}

	private static void setenv(SymbolLookup libc, String os, String key, String value) throws IOException {
		try (Arena a = Arena.ofConfined()) {
			if (os.equals("Windows")) {
				MethodHandle putenv = LINKER.downcallHandle(libc.find("_putenv_s").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
				int unused = (int) putenv.invokeExact(a.allocateFrom(key), a.allocateFrom(value));
			} else {
				MethodHandle setenv = LINKER.downcallHandle(libc.find("setenv").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
				int unused = (int) setenv.invokeExact(a.allocateFrom(key), a.allocateFrom(value), 1);
			}
		} catch (Throwable t) {
			throw new IOException("The environment variable " + key + " could not be set", t);
		}
	}

	public static String os() {
		String name = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		return name.contains("win") ? "Windows" : name.contains("mac") ? "Mac" : "Linux";
	}

	/* ---------------- Symbols ---------------- */

	private MethodHandle fn(String name, java.lang.foreign.MemoryLayout result, java.lang.foreign.MemoryLayout... args) {
		return LINKER.downcallHandle(symbol(name), FunctionDescriptor.of(result, args));
	}

	private MethodHandle fnVoid(String name, java.lang.foreign.MemoryLayout... args) {
		return LINKER.downcallHandle(symbol(name), FunctionDescriptor.ofVoid(args));
	}

	private MethodHandle optionalFn(String name, FunctionDescriptor descriptor) {
		Optional<MemorySegment> s = r.find(name);
		return s.map(m -> LINKER.downcallHandle(m, descriptor)).orElse(null);
	}

	/** The address of a symbol of R (a function or a variable). */
	public MemorySegment symbolAddress(String name) {
		return symbol(name);
	}

	MemorySegment symbol(String name) {
		return r.find(name).orElseThrow(() -> new UnsatisfiedLinkError("R does not export " + name));
	}

	/**
	 * Sets LC_NUMERIC to "C" (R requires it; the JVM may have set the locale of the
	 * user, then R would print a decimal comma).
	 */
	public void numericLocaleC() {
		try (Arena a = Arena.ofConfined()) {
			int lcNumeric = os.equals("Linux") ? 1 : 4;
			MethodHandle setlocale = LINKER.downcallHandle(libc.find("setlocale").orElseThrow(), FunctionDescriptor.of(ADDRESS, JAVA_INT, ADDRESS));
			MemorySegment unused = (MemorySegment) setlocale.invokeExact(lcNumeric, a.allocateFrom("C"));
		} catch (Throwable t) {
			/* R warns itself */
		}
	}

	/** Returns true if R exports the symbol. */
	public boolean has(String name) {
		return r.find(name).isPresent();
	}

	/** The value of a global variable of type SEXP (e.g., R_GlobalEnv). */
	public MemorySegment sexpVariable(String name) {
		return symbol(name).reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0);
	}

	/** Sets a global variable of type int (e.g., R_SignalHandlers). */
	public void setInt(String name, int value) {
		symbol(name).reinterpret(4).set(JAVA_INT, 0, value);
	}

	/** Sets a global variable of a pointer type (e.g., ptr_R_WriteConsoleEx, R_Outputfile). */
	public void setPointer(String name, MemorySegment value) {
		symbol(name).reinterpret(ADDRESS.byteSize()).set(ADDRESS, 0, value);
	}

	/** Sets a global variable of type uintptr_t/size_t (R_CStackLimit). */
	public void setLong(String name, long value) {
		symbol(name).reinterpret(8).set(JAVA_LONG, 0, value);
	}

	/** Zeroed memory of the C heap (R frees it, e.g., a DevDesc). */
	public MemorySegment calloc(long size) {
		try {
			return ((MemorySegment) calloc.invokeExact(1L, size)).reinterpret(size);
		} catch (Throwable t) {
			throw new IllegalStateException(t);
		}
	}
}
