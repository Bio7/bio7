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

import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.eco.bio7.pyffm.PyValue;

/**
 * Converts values between Java and the bootstrap of Python: a header (JSON,
 * the structure) and buffers in native memory (the data of the arrays).
 */
public final class Codec {

	private Codec() {
	}

	/** An encoded value: the header and the table of the buffers (address, length pairs). */
	public record Encoded(String header, MemorySegment pointers, int count) {
	}

	/** Encodes a value into native memory of the arena. */
	public static Encoded encode(PyValue value, Arena arena) {
		List<MemorySegment> buffers = new ArrayList<>();
		Object header = enc(value, arena, buffers);
		MemorySegment pointers = arena.allocate(JAVA_LONG, Math.max(1, buffers.size() * 2L));
		for (int i = 0; i < buffers.size(); i++) {
			pointers.setAtIndex(JAVA_LONG, 2L * i, buffers.get(i).address());
			pointers.setAtIndex(JAVA_LONG, 2L * i + 1, buffers.get(i).byteSize());
		}
		return new Encoded(Json.write(header), pointers, buffers.size());
	}

	private static Map<String, Object> obj(Object... kv) {
		Map<String, Object> m = new LinkedHashMap<>();
		for (int i = 0; i + 1 < kv.length; i += 2) {
			m.put((String) kv[i], kv[i + 1]);
		}
		return m;
	}

	private static Object enc(PyValue v, Arena arena, List<MemorySegment> buffers) {
		return switch (v) {
		case null -> obj("t", "none");
		case PyValue.None n -> obj("t", "none");
		case PyValue.Array a -> {
			MemorySegment seg = segment(a.data(), arena);
			buffers.add(seg);
			yield obj("t", "nd", "dtype", a.dtype(), "shape", a.shape(), "b", buffers.size() - 1);
		}
		case PyValue.Str s -> obj("t", "str", "v", java.util.Arrays.asList(s.values()));
		case PyValue.PyList l -> {
			List<Object> items = new ArrayList<>();
			for (PyValue e : l.values()) {
				items.add(enc(e, arena, buffers));
			}
			yield obj("t", "list", "v", items);
		}
		case PyValue.PyDict d -> {
			Map<String, Object> items = new LinkedHashMap<>();
			for (Map.Entry<String, PyValue> e : d.values().entrySet()) {
				items.put(e.getKey(), enc(e.getValue(), arena, buffers));
			}
			yield obj("t", "dict", "v", items);
		}
		case PyValue.Frame f -> {
			List<Object> cols = new ArrayList<>();
			for (Map.Entry<String, PyValue> e : f.columns().entrySet()) {
				cols.add(List.of(e.getKey(), enc(e.getValue(), arena, buffers)));
			}
			yield obj("t", "df", "cols", cols, "index", f.index() == null ? null : java.util.Arrays.asList(f.index()));
		}
		case PyValue.Other o -> throw new IllegalArgumentException("A Python object can't be transferred back: " + o.type());
		};
	}

	/* The data in native memory (native byte order, as numpy) */
	private static MemorySegment segment(Object data, Arena arena) {
		return switch (data) {
		case double[] d -> arena.allocateFrom(JAVA_DOUBLE, d);
		case float[] f -> arena.allocateFrom(JAVA_FLOAT, f);
		case long[] l -> arena.allocateFrom(JAVA_LONG, l);
		case int[] n -> arena.allocateFrom(JAVA_INT, n);
		case short[] s -> arena.allocateFrom(JAVA_SHORT, s);
		case byte[] b -> arena.allocateFrom(JAVA_BYTE, b);
		case boolean[] b -> {
			MemorySegment seg = arena.allocate(Math.max(1, b.length));
			for (int i = 0; i < b.length; i++) {
				seg.set(JAVA_BYTE, i, (byte) (b[i] ? 1 : 0));
			}
			yield seg.asSlice(0, b.length);
		}
		default -> throw new IllegalArgumentException("Unsupported data: " + data.getClass());
		};
	}

	/** Decodes a header and the buffers (the data is copied). */
	@SuppressWarnings("unchecked")
	public static PyValue decode(String header, MemorySegment pointers, long count) {
		long[] table = new long[(int) (2 * count)];
		if (count > 0) {
			MemorySegment p = pointers.reinterpret(16 * count);
			for (int i = 0; i < table.length; i++) {
				table[i] = p.getAtIndex(JAVA_LONG, i);
			}
		}
		return dec((Map<String, Object>) Json.parse(header), table);
	}

	@SuppressWarnings("unchecked")
	private static PyValue dec(Map<String, Object> h, long[] table) {
		String t = (String) h.get("t");
		switch (t) {
		case "nd": {
			int b = ((Number) h.get("b")).intValue();
			MemorySegment seg = MemorySegment.ofAddress(table[2 * b]).reinterpret(table[2 * b + 1]);
			String dtype = (String) h.get("dtype");
			List<Object> shapeList = (List<Object>) h.get("shape");
			int[] shape = new int[shapeList.size()];
			for (int i = 0; i < shape.length; i++) {
				shape[i] = ((Number) shapeList.get(i)).intValue();
			}
			Object data = switch (dtype) {
			case "f8" -> seg.toArray(JAVA_DOUBLE);
			case "f4" -> seg.toArray(JAVA_FLOAT);
			case "i8", "u8" -> seg.toArray(JAVA_LONG);
			case "i4", "u4" -> seg.toArray(JAVA_INT);
			case "i2", "u2" -> seg.toArray(JAVA_SHORT);
			case "i1", "u1" -> seg.toArray(JAVA_BYTE);
			case "b1" -> {
				byte[] raw = seg.toArray(JAVA_BYTE);
				boolean[] z = new boolean[raw.length];
				for (int i = 0; i < raw.length; i++) {
					z[i] = raw[i] != 0;
				}
				yield z;
			}
			default -> throw new IllegalArgumentException("Unknown dtype: " + dtype);
			};
			return new PyValue.Array(data, dtype, shape);
		}
		case "str": {
			List<Object> v = (List<Object>) h.get("v");
			String[] s = new String[v.size()];
			for (int i = 0; i < s.length; i++) {
				s[i] = (String) v.get(i);
			}
			return new PyValue.Str(s);
		}
		case "list": {
			List<PyValue> items = new ArrayList<>();
			for (Object e : (List<Object>) h.get("v")) {
				items.add(dec((Map<String, Object>) e, table));
			}
			return new PyValue.PyList(items);
		}
		case "dict": {
			Map<String, PyValue> items = new LinkedHashMap<>();
			for (Map.Entry<String, Object> e : ((Map<String, Object>) h.get("v")).entrySet()) {
				items.put(e.getKey(), dec((Map<String, Object>) e.getValue(), table));
			}
			return new PyValue.PyDict(items);
		}
		case "df": {
			Map<String, PyValue> cols = new LinkedHashMap<>();
			for (Object c : (List<Object>) h.get("cols")) {
				List<Object> pair = (List<Object>) c;
				cols.put((String) pair.get(0), dec((Map<String, Object>) pair.get(1), table));
			}
			String[] index = null;
			if (h.get("index") instanceof List<?> idx) {
				index = idx.stream().map(String::valueOf).toArray(String[]::new);
			}
			return new PyValue.Frame(cols, index);
		}
		case "none":
			return PyValue.None.INSTANCE;
		default:
			return new PyValue.Other(String.valueOf(h.get("type")), String.valueOf(h.get("repr")));
		}
	}
}
