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

import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;

/**
 * Typed access to the C API of R (only in the thread of R). The methods which
 * can raise an R error are used only for small objects (or the R error is
 * caught by tryEval).
 */
public final class RCore {

	public final RNative n;
	/* Constants (valid after the start of R) */
	public MemorySegment nil, globalEnv, baseEnv, naString, unbound;
	public MemorySegment namesSym, dimSym, dimNamesSym, classSym, levelsSym, rowNamesSym;

	public RCore(RNative n) {
		this.n = n;
	}

	/** Reads the constants of R (after setup_Rmainloop). */
	public void initConstants() {
		nil = n.sexpVariable("R_NilValue");
		globalEnv = n.sexpVariable("R_GlobalEnv");
		baseEnv = n.sexpVariable("R_BaseEnv");
		naString = n.sexpVariable("R_NaString");
		unbound = n.sexpVariable("R_UnboundValue");
		namesSym = n.sexpVariable("R_NamesSymbol");
		dimSym = n.sexpVariable("R_DimSymbol");
		dimNamesSym = n.sexpVariable("R_DimNamesSymbol");
		classSym = n.sexpVariable("R_ClassSymbol");
		levelsSym = n.sexpVariable("R_LevelsSymbol");
		rowNamesSym = n.sexpVariable("R_RowNamesSymbol");
	}

	/* The handles are invoked with invoke (not invokeExact): the checked wrapper keeps the call sites short */
	private static RuntimeException fail(Throwable t) {
		return t instanceof RuntimeException r ? r : new IllegalStateException(t);
	}

	public int typeOf(MemorySegment x) {
		try {
			return (int) n.TYPEOF.invokeExact(x);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public long length(MemorySegment x) {
		try {
			return (long) n.Rf_xlength.invokeExact(x);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public MemorySegment protect(MemorySegment x) {
		try {
			return (MemorySegment) n.Rf_protect.invokeExact(x);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public void unprotect(int count) {
		try {
			n.Rf_unprotect.invokeExact(count);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public void preserve(MemorySegment x) {
		try {
			n.R_PreserveObject.invokeExact(x);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public void release(MemorySegment x) {
		try {
			n.R_ReleaseObject.invokeExact(x);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public MemorySegment install(String name) {
		try (Arena a = Arena.ofConfined()) {
			return (MemorySegment) n.Rf_install.invokeExact(a.allocateFrom(name));
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	/** A CHARSXP in UTF-8 (NA for null). */
	public MemorySegment mkChar(String s) {
		if (s == null) {
			return naString;
		}
		try (Arena a = Arena.ofConfined()) {
			return (MemorySegment) n.Rf_mkCharCE.invokeExact(a.allocateFrom(s, StandardCharsets.UTF_8), RNative.CE_UTF8);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	/** A vector (small vectors only: an allocation error of R would not be caught, see RSession.alloc). */
	public MemorySegment allocVector(int type, long length) {
		try {
			return (MemorySegment) n.Rf_allocVector.invokeExact(type, length);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	/** A character vector of length 1 (UTF-8), not protected. */
	public MemorySegment mkString(String s) {
		MemorySegment x = protect(allocVector(RNative.STRSXP, 1));
		setStringElt(x, 0, mkChar(s));
		unprotect(1);
		return x;
	}

	public MemorySegment scalarReal(double v) {
		try {
			return (MemorySegment) n.Rf_ScalarReal.invokeExact(v);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public MemorySegment scalarInteger(int v) {
		try {
			return (MemorySegment) n.Rf_ScalarInteger.invokeExact(v);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public MemorySegment lang1(MemorySegment f) {
		try {
			return (MemorySegment) n.Rf_lang1.invokeExact(f);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public MemorySegment lang2(MemorySegment f, MemorySegment a) {
		try {
			return (MemorySegment) n.Rf_lang2.invokeExact(f, a);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public MemorySegment lang3(MemorySegment f, MemorySegment a, MemorySegment b) {
		try {
			return (MemorySegment) n.Rf_lang3.invokeExact(f, a, b);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public MemorySegment lang4(MemorySegment f, MemorySegment a, MemorySegment b, MemorySegment d) {
		try {
			return (MemorySegment) n.Rf_lang4.invokeExact(f, a, b, d);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public MemorySegment findVar(MemorySegment sym, MemorySegment env) {
		try {
			return (MemorySegment) n.Rf_findVar.invokeExact(sym, env);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	/** The data of a vector (REAL, INTEGER, LOGICAL, RAW) as a segment of its size. */
	public MemorySegment data(MemorySegment x, int type) {
		try {
			long len = length(x);
			MemorySegment p = switch (type) {
			case RNative.REALSXP -> (MemorySegment) n.REAL.invokeExact(x);
			case RNative.INTSXP -> (MemorySegment) n.INTEGER.invokeExact(x);
			case RNative.LGLSXP -> (MemorySegment) n.LOGICAL.invokeExact(x);
			case RNative.RAWSXP -> (MemorySegment) n.RAW.invokeExact(x);
			default -> throw new IllegalArgumentException("no data vector: " + type);
			};
			long size = type == RNative.REALSXP ? 8 : type == RNative.RAWSXP ? 1 : 4;
			return p.reinterpret(len * size);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public MemorySegment stringElt(MemorySegment x, long i) {
		try {
			return (MemorySegment) n.STRING_ELT.invokeExact(x, i);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public void setStringElt(MemorySegment x, long i, MemorySegment c) {
		try {
			n.SET_STRING_ELT.invokeExact(x, i, c);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public MemorySegment vectorElt(MemorySegment x, long i) {
		try {
			return (MemorySegment) n.VECTOR_ELT.invokeExact(x, i);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public void setVectorElt(MemorySegment x, long i, MemorySegment v) {
		try {
			MemorySegment unused = (MemorySegment) n.SET_VECTOR_ELT.invokeExact(x, i, v);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public MemorySegment getAttrib(MemorySegment x, MemorySegment sym) {
		try {
			return (MemorySegment) n.Rf_getAttrib.invokeExact(x, sym);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public void setAttrib(MemorySegment x, MemorySegment sym, MemorySegment v) {
		try {
			MemorySegment unused = (MemorySegment) n.Rf_setAttrib.invokeExact(x, sym, v);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	/** The text of a CHARSXP in UTF-8 (null for NA). */
	public String charUTF8(MemorySegment c) {
		if (c.address() == naString.address()) {
			return null;
		}
		try {
			MemorySegment s = (MemorySegment) n.Rf_translateCharUTF8.invokeExact(c);
			return s.reinterpret(Integer.MAX_VALUE).getString(0, StandardCharsets.UTF_8);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	/** The name of a symbol. */
	public String symbolName(MemorySegment sym) {
		try {
			return charUTF8((MemorySegment) n.PRINTNAME.invokeExact(sym));
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public MemorySegment vmaxget() {
		try {
			return (MemorySegment) n.vmaxget.invokeExact();
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public void vmaxset(MemorySegment v) {
		try {
			n.vmaxset.invokeExact(v);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	public MemorySegment makeExternalPtr(MemorySegment address, MemorySegment tag) {
		try {
			return (MemorySegment) n.R_MakeExternalPtr.invokeExact(address, tag, nil);
		} catch (Throwable t) {
			throw fail(t);
		}
	}

	/**
	 * Evaluates a call; an R error is caught by R (in C).
	 *
	 * @param silent false: R prints the error message (like the console)
	 * @return the value or null if an error occurred
	 */
	public MemorySegment tryEval(MemorySegment call, MemorySegment env, boolean silent) {
		try (Arena a = Arena.ofConfined()) {
			MemorySegment error = a.allocate(JAVA_INT);
			MemorySegment value = silent ? (MemorySegment) n.R_tryEvalSilent.invokeExact(call, env, error)
					: (MemorySegment) n.R_tryEval.invokeExact(call, env, error);
			return error.get(JAVA_INT, 0) != 0 ? null : value;
		} catch (Throwable t) {
			throw fail(t);
		}
	}
}
