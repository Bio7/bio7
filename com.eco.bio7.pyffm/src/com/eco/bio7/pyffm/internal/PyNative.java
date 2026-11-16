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
package com.eco.bio7.pyffm.internal;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;

import com.eco.bio7.pyffm.PyException;

/**
 * The binding to the shared library of CPython (libpython) with the Foreign
 * Function and Memory API. Only a few functions of the C API are used (they
 * exist in all versions of Python 3): the data and the code are exchanged by
 * the bootstrap module of Python (ctypes) and upcalls to Java.
 * <p>
 * The functions which run Python code must be called with the GIL held (see
 * PySession).
 */
public final class PyNative {

	public static final Linker LINKER = Linker.nativeLinker();

	public final String os;
	public final Arena arena = Arena.global();
	private final SymbolLookup py;

	final MethodHandle Py_InitializeEx, Py_IsInitialized, PyRun_SimpleStringFlags, PyGILState_Ensure, PyGILState_Release, PyEval_SaveThread,
			PyThreadState_SetAsyncExc;
	/** The address of PyExc_KeyboardInterrupt (a PyObject*). */
	final MemorySegment keyboardInterrupt;

	private PyNative(String os, SymbolLookup py) {
		this.os = os;
		this.py = py;
		Py_InitializeEx = fnVoid("Py_InitializeEx", JAVA_INT);
		Py_IsInitialized = fn("Py_IsInitialized", JAVA_INT);
		PyRun_SimpleStringFlags = fn("PyRun_SimpleStringFlags", JAVA_INT, ADDRESS, ADDRESS);
		PyGILState_Ensure = fn("PyGILState_Ensure", JAVA_INT);
		PyGILState_Release = fnVoid("PyGILState_Release", JAVA_INT);
		PyEval_SaveThread = fn("PyEval_SaveThread", ADDRESS);
		/* unsigned long: 32 bits on Windows */
		PyThreadState_SetAsyncExc = fn("PyThreadState_SetAsyncExc", JAVA_INT, os.equals("Windows") ? JAVA_INT : JAVA_LONG, ADDRESS);
		keyboardInterrupt = py.find("PyExc_KeyboardInterrupt").orElseThrow().reinterpret(ADDRESS.byteSize()).get(ADDRESS, 0);
	}

	public static String os() {
		String name = System.getProperty("os.name", "").toLowerCase();
		return name.startsWith("windows") ? "Windows" : name.startsWith("mac") ? "Mac" : "Linux";
	}

	/**
	 * Loads the library. On Linux and macOS it is opened with RTLD_GLOBAL first:
	 * the extension modules (e.g., numpy) find the symbols of Python in the
	 * process.
	 */
	public static PyNative load(Path library, String home) throws PyException {
		String os = os();
		try {
			if (os.equals("Linux")) {
				/*
				 * The newest C++ runtime of Python (e.g., Anaconda: <prefix>/lib), the system
				 * and the other runtimes (R) first: only one libstdc++ can be loaded per process
				 * (numpy loads it)
				 */
				java.util.List<Path> own = new java.util.ArrayList<>();
				own.add(Path.of(home, "lib"));
				if (library.getParent() != null) {
					own.add(library.getParent());
				}
				CxxRuntime.preloadNewest(own);
			}
			if (!os.equals("Windows")) {
				MethodHandle dlopen = LINKER.downcallHandle(LINKER.defaultLookup().find("dlopen").orElseThrow(), FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));
				int flags = os.equals("Mac") ? 0x2 | 0x8 : 0x2 | 0x100; /* RTLD_NOW | RTLD_GLOBAL */
				try (Arena a = Arena.ofConfined()) {
					MemorySegment handle = (MemorySegment) dlopen.invokeExact(a.allocateFrom(library.toString()), flags);
					if (handle.address() == 0) {
						throw new PyException("The library of Python could not be loaded: " + library);
					}
				}
				/* PYTHONHOME: the standard library (else Python looks beside the executable: java) */
				MethodHandle setenv = LINKER.downcallHandle(LINKER.defaultLookup().find("setenv").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
				try (Arena a = Arena.ofConfined()) {
					int r = (int) setenv.invokeExact(a.allocateFrom("PYTHONHOME"), a.allocateFrom(home), 1);
					if (r != 0) {
						throw new PyException("PYTHONHOME could not be set");
					}
				}
			} else {
				/* The CRT environment (Python reads it with _wgetenv) */
				SymbolLookup ucrt = SymbolLookup.libraryLookup("ucrtbase", Arena.global());
				MethodHandle putenv = LINKER.downcallHandle(ucrt.find("_wputenv_s").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
				try (Arena a = Arena.ofConfined()) {
					int r = (int) putenv.invokeExact(wide(a, "PYTHONHOME"), wide(a, home));
					if (r != 0) {
						throw new PyException("PYTHONHOME could not be set");
					}
				}
				preloadWindows(library);
			}
			SymbolLookup py = SymbolLookup.libraryLookup(library, Arena.global());
			return new PyNative(os, py);
		} catch (PyException e) {
			throw e;
		} catch (Throwable e) {
			throw new PyException("The library of Python could not be loaded: " + library + " (" + e + ")", e);
		}
	}

	/*
	 * Loads the DLL with its folder in the search path of its dependencies
	 * (LOAD_LIBRARY_SEARCH_DLL_LOAD_DIR | LOAD_LIBRARY_SEARCH_DEFAULT_DIRS): the
	 * DLLs beside python3xx.dll (e.g., of Anaconda) aren't found by LoadLibrary,
	 * which SymbolLookup.libraryLookup uses. The lookup then gets the loaded DLL.
	 */
	private static void preloadWindows(Path library) throws Throwable {
		SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", Arena.global());
		Linker.Option lastError = Linker.Option.captureCallState("GetLastError");
		java.lang.foreign.StructLayout state = Linker.Option.captureStateLayout();
		MethodHandle loadLibraryEx = LINKER.downcallHandle(kernel32.find("LoadLibraryExW").orElseThrow(), FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT),
				lastError);
		try (Arena a = Arena.ofConfined()) {
			MemorySegment error = a.allocate(state);
			MemorySegment handle = (MemorySegment) loadLibraryEx.invokeExact(error, wide(a, library.toString()), MemorySegment.NULL, 0x00001000 | 0x00000100);
			if (handle.address() == 0) {
				int code = (int) state.varHandle(java.lang.foreign.MemoryLayout.PathElement.groupElement("GetLastError")).get(error, 0L);
				throw new PyException("The library of Python could not be loaded: " + library + " (Windows error " + code
						+ (code == 126 ? ": the DLL or a DLL it needs was not found" : code == 193 ? ": not a 64-bit DLL" : "") + ")");
			}
		}
	}

	private static MemorySegment wide(Arena a, String s) {
		return a.allocateFrom(s + "\0", java.nio.charset.StandardCharsets.UTF_16LE);
	}

	private MethodHandle fn(String name, ValueLayout result, java.lang.foreign.MemoryLayout... args) {
		return LINKER.downcallHandle(py.find(name).orElseThrow(() -> new IllegalStateException("Not found in the library of Python: " + name)),
				FunctionDescriptor.of(result, args));
	}

	private MethodHandle fnVoid(String name, java.lang.foreign.MemoryLayout... args) {
		return LINKER.downcallHandle(py.find(name).orElseThrow(() -> new IllegalStateException("Not found in the library of Python: " + name)),
				FunctionDescriptor.ofVoid(args));
	}

	/* ---------------- The calls ---------------- */

	private static PyException fail(String what, Throwable e) {
		return e instanceof PyException p ? p : new PyException(what + ": " + e, e);
	}

	public void initialize() throws PyException {
		try {
			/* 0: no signal handlers of Python (the JVM owns the signals) */
			Py_InitializeEx.invokeExact(0);
		} catch (Throwable e) {
			throw fail("Py_InitializeEx", e);
		}
	}

	public boolean isInitialized() throws PyException {
		try {
			return (int) Py_IsInitialized.invokeExact() != 0;
		} catch (Throwable e) {
			throw fail("Py_IsInitialized", e);
		}
	}

	/** Runs code in __main__ (GIL held); returns false if an exception was printed. */
	public boolean runSimple(String code) throws PyException {
		try (Arena a = Arena.ofConfined()) {
			return (int) PyRun_SimpleStringFlags.invokeExact(a.allocateFrom(code), MemorySegment.NULL) == 0;
		} catch (Throwable e) {
			throw fail("PyRun_SimpleString", e);
		}
	}

	public int ensureGil() throws PyException {
		try {
			return (int) PyGILState_Ensure.invokeExact();
		} catch (Throwable e) {
			throw fail("PyGILState_Ensure", e);
		}
	}

	public void releaseGil(int state) throws PyException {
		try {
			PyGILState_Release.invokeExact(state);
		} catch (Throwable e) {
			throw fail("PyGILState_Release", e);
		}
	}

	/** Releases the GIL of the thread after the initialization. */
	public void saveThread() throws PyException {
		try {
			MemorySegment unused = (MemorySegment) PyEval_SaveThread.invokeExact();
		} catch (Throwable e) {
			throw fail("PyEval_SaveThread", e);
		}
	}

	/** Raises KeyboardInterrupt (exc true) in a thread of Python or clears a pending one (GIL held). */
	public void asyncExc(long threadId, boolean exc) throws PyException {
		MemorySegment e = exc ? keyboardInterrupt : MemorySegment.NULL;
		try {
			if (os.equals("Windows")) {
				int unused = (int) PyThreadState_SetAsyncExc.invokeExact((int) threadId, e);
			} else {
				int unused = (int) PyThreadState_SetAsyncExc.invokeExact(threadId, e);
			}
		} catch (Throwable t) {
			throw fail("PyThreadState_SetAsyncExc", t);
		}
	}
}
