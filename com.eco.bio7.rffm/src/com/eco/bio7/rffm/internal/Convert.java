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

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.eco.bio7.rffm.RValue;

/**
 * Converts R objects (SEXP) to RValue and back (in the thread of R). The
 * vectors are copied with one memory copy each.
 */
public final class Convert {

	/** Allocates a vector in R (with the error caught by R). */
	public interface Allocator {
		/** @return the new vector (not protected) */
		MemorySegment alloc(int type, long length);
	}

	private static final String[] ATTRIBUTES = { "names", "dim", "dimnames", "class", "levels", "row.names" };

	private final RCore c;
	private final Allocator allocator;

	public Convert(RCore core, Allocator allocator) {
		this.c = core;
		this.allocator = allocator;
	}

	/* ---------------- R to Java ---------------- */

	/** A copy of an R object. */
	public RValue toJava(MemorySegment x) {
		MemorySegment vmax = c.vmaxget();
		try {
			return convert(x, 0);
		} finally {
			/* translateCharUTF8 allocates on the transient stack of R */
			c.vmaxset(vmax);
		}
	}

	private RValue convert(MemorySegment x, int depth) {
		if (depth > 64) {
			return new RValue.Other("nested too deeply", Map.of());
		}
		int type = c.typeOf(x);
		if (type == RNative.NILSXP) {
			return RValue.Null.INSTANCE;
		}
		long n = c.length(x);
		if (n > Integer.MAX_VALUE - 8 && isVector(type)) {
			throw new IllegalArgumentException("The vector is too long for a Java array: " + n);
		}
		int len = (int) n;
		Map<String, RValue> attributes = attributes(x, depth);
		return switch (type) {
		case RNative.LGLSXP -> new RValue.Logical(c.data(x, type).toArray(JAVA_INT), attributes);
		case RNative.INTSXP -> new RValue.Int(c.data(x, type).toArray(JAVA_INT), attributes);
		case RNative.REALSXP -> new RValue.Num(c.data(x, type).toArray(JAVA_DOUBLE), attributes);
		case RNative.RAWSXP -> new RValue.Raw(c.data(x, type).toArray(JAVA_BYTE), attributes);
		case RNative.STRSXP -> {
			String[] s = new String[len];
			for (int i = 0; i < len; i++) {
				s[i] = c.charUTF8(c.stringElt(x, i));
			}
			yield new RValue.Str(s, attributes);
		}
		case RNative.VECSXP, RNative.EXPRSXP -> {
			List<RValue> list = new ArrayList<>(len);
			for (int i = 0; i < len; i++) {
				list.add(convert(c.vectorElt(x, i), depth + 1));
			}
			yield new RValue.List(list, attributes);
		}
		case RNative.SYMSXP -> new RValue.Other("symbol:" + c.symbolName(x), Map.of());
		default -> new RValue.Other(typeName(type), attributes);
		};
	}

	private static boolean isVector(int type) {
		return type == RNative.LGLSXP || type == RNative.INTSXP || type == RNative.REALSXP || type == RNative.RAWSXP || type == RNative.STRSXP
				|| type == RNative.VECSXP || type == RNative.EXPRSXP;
	}

	private Map<String, RValue> attributes(MemorySegment x, int depth) {
		Map<String, RValue> map = null;
		for (String name : ATTRIBUTES) {
			MemorySegment a = c.getAttrib(x, symbol(name));
			if (a.address() != c.nil.address()) {
				if (map == null) {
					map = new LinkedHashMap<>();
				}
				map.put(name, convert(a, depth + 1));
			}
		}
		return map == null ? Map.of() : map;
	}

	private MemorySegment symbol(String name) {
		return switch (name) {
		case "names" -> c.namesSym;
		case "dim" -> c.dimSym;
		case "dimnames" -> c.dimNamesSym;
		case "class" -> c.classSym;
		case "levels" -> c.levelsSym;
		default -> c.rowNamesSym;
		};
	}

	static String typeName(int type) {
		return switch (type) {
		case RNative.CLOSXP -> "closure";
		case RNative.ENVSXP -> "environment";
		case RNative.LANGSXP -> "language";
		case RNative.LISTSXP -> "pairlist";
		case RNative.CPLXSXP -> "complex";
		case RNative.EXTPTRSXP -> "externalptr";
		case 7 -> "promise";
		case 8 -> "special";
		case 25 -> "S4";
		default -> "type " + type;
		};
	}

	/* ---------------- Java to R ---------------- */

	/**
	 * Creates an R object (protected once: the caller unprotects it).
	 *
	 * @throws IllegalArgumentException if the attributes don't fit the value
	 *                                  (checked before R is called)
	 */
	public MemorySegment toR(RValue v) {
		validate(v, 0);
		protectedCount = 0;
		try {
			MemorySegment x = create(v);
			protectedCount = 0;
			return x;
		} catch (RuntimeException e) {
			/* e.g., R could not allocate a vector: the protection stack stays balanced */
			if (protectedCount > 0) {
				c.unprotect(protectedCount);
			}
			protectedCount = 0;
			throw e;
		}
	}

	private int protectedCount;

	private MemorySegment p(MemorySegment x) {
		MemorySegment r = c.protect(x);
		protectedCount++;
		return r;
	}

	private void u() {
		c.unprotect(1);
		protectedCount--;
	}

	private MemorySegment create(RValue v) {
		MemorySegment x = switch (v) {
		case RValue.Null nul -> p(c.nil);
		case RValue.Logical l -> copyInts(RNative.LGLSXP, l.values());
		case RValue.Int i -> copyInts(RNative.INTSXP, i.values());
		case RValue.Num d -> {
			MemorySegment s = p(allocator.alloc(RNative.REALSXP, d.values().length));
			MemorySegment.copy(d.values(), 0, c.data(s, RNative.REALSXP), JAVA_DOUBLE, 0, d.values().length);
			yield s;
		}
		case RValue.Raw r -> {
			MemorySegment s = p(allocator.alloc(RNative.RAWSXP, r.values().length));
			MemorySegment.copy(r.values(), 0, c.data(s, RNative.RAWSXP), JAVA_BYTE, 0, r.values().length);
			yield s;
		}
		case RValue.Str s -> {
			MemorySegment x2 = p(allocator.alloc(RNative.STRSXP, s.values().length));
			for (int i = 0; i < s.values().length; i++) {
				c.setStringElt(x2, i, c.mkChar(s.values()[i]));
			}
			yield x2;
		}
		case RValue.List l -> {
			MemorySegment x2 = p(allocator.alloc(RNative.VECSXP, l.values().size()));
			for (int i = 0; i < l.values().size(); i++) {
				MemorySegment e = create(l.values().get(i));
				c.setVectorElt(x2, i, e);
				u();
			}
			yield x2;
		}
		case RValue.Other o -> throw new IllegalArgumentException("A value of type " + o.type() + " can't be transferred to R");
		};
		if (!(v instanceof RValue.Null)) {
			for (var e : v.attributes().entrySet()) {
				MemorySegment a = create(e.getValue());
				c.setAttrib(x, symbol(e.getKey()), a);
				u();
			}
		}
		return x;
	}

	private MemorySegment copyInts(int type, int[] values) {
		MemorySegment s = p(allocator.alloc(type, values.length));
		MemorySegment.copy(values, 0, c.data(s, type), JAVA_INT, 0, values.length);
		return s;
	}

	/* The attributes are checked here: R would raise an error (a longjmp) in setAttrib */
	private static void validate(RValue v, int depth) {
		if (depth > 64) {
			throw new IllegalArgumentException("nested too deeply");
		}
		int n = v.length();
		for (var e : v.attributes().entrySet()) {
			String name = e.getKey();
			RValue a = e.getValue();
			switch (name) {
			case "names" -> require(a instanceof RValue.Str s && s.length() == n, "'names' must be a character vector of length " + n);
			case "dim" -> {
				require(a instanceof RValue.Int, "'dim' must be an integer vector");
				long product = 1;
				for (int d : ((RValue.Int) a).values()) {
					require(d >= 0, "'dim' must not be negative");
					product *= d;
				}
				require(product == n, "the product of 'dim' must be the length " + n);
			}
			case "dimnames" -> {
				int[] dim = v.dim();
				require(dim != null && a instanceof RValue.List l && l.length() == dim.length, "'dimnames' must be a list with one entry per dimension");
				RValue.List l = (RValue.List) a;
				for (int i = 0; i < dim.length; i++) {
					RValue d = l.values().get(i);
					require(d instanceof RValue.Null || (d instanceof RValue.Str s && s.length() == dim[i]), "'dimnames' entry " + (i + 1) + " must have length " + dim[i]);
				}
			}
			case "class", "levels" -> require(a instanceof RValue.Str, "'" + name + "' must be a character vector");
			case "row.names" -> require(a instanceof RValue.Int || a instanceof RValue.Str, "'row.names' must be an integer or character vector");
			default -> throw new IllegalArgumentException("The attribute '" + name + "' is not supported");
			}
			validate(a, depth + 1);
		}
		if (v instanceof RValue.List l) {
			for (RValue e : l.values()) {
				validate(e, depth + 1);
			}
		}
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new IllegalArgumentException(message);
		}
	}
}
