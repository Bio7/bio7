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
package com.eco.bio7.pybridge;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.swt.widgets.Display;

import com.eco.bio7.pyffm.PyValue;

/**
 * The Java API for the embedded Python (bio7.java): classes, objects, methods,
 * constructors and fields by reflection, called in the thread of Python. The
 * Java objects are handles for Python (proxies); numbers, strings and arrays are
 * converted (byte[] and short[] as uint8 and uint16, the pixels of ImageJ).
 */
final class PythonJava {

	/* The objects of the handles of Python (with a count of the handles) */
	private static final Map<Long, Object> OBJECTS = new java.util.HashMap<>();
	private static final Map<Long, int[]> COUNTS = new java.util.HashMap<>();
	private static final IdentityHashMap<Object, Long> IDS = new IdentityHashMap<>();
	private static final AtomicLong NEXT = new AtomicLong(1);

	private PythonJava() {
	}

	/** The functions java_* of PythonCalls. */
	/* The functions of the Java API (bio7.java): reserved for bio7.call */
	static final java.util.Set<String> NAMES = java.util.Set.of("java_type", "java_members", "java_new", "java_call", "java_get", "java_set", "java_iter",
			"java_release");

	static PyValue call(String function, String[] args, PyValue value) throws Exception {
		switch (function) {
		case "java_type":
			return PyValue.of(type(args[0]).getName());
		case "java_members":
			return members(type(args[0]));
		case "java_new": {
			Class<?> cls = type(args[0]);
			List<PyValue> a = arguments(value);
			Constructor<?> c = (Constructor<?>) select(List.of(cls.getConstructors()), a, cls.getName() + " (constructor)");
			Object[] converted = convertArgs(c, a);
			return invoke(args, () -> c.newInstance(converted));
		}
		case "java_call": {
			Class<?> cls = type(args[0]);
			String name = args[1];
			Object target = handleObject(((PyValue.PyDict) value).get("target"));
			List<PyValue> a = arguments(value);
			Class<?> owner = target != null ? target.getClass() : cls;
			List<Method> methods = new ArrayList<>();
			for (Method m : owner.getMethods()) {
				if (m.getName().equals(name) && (target != null || Modifier.isStatic(m.getModifiers()))) {
					methods.add(m);
				}
			}
			if (methods.isEmpty()) {
				throw new IllegalArgumentException(owner.getName() + " has no " + (target == null ? "static " : "") + "method " + name);
			}
			Method m = accessible((Method) select(methods, a, owner.getName() + "." + name));
			Object[] converted = convertArgs(m, a);
			Object t = Modifier.isStatic(m.getModifiers()) ? null : target;
			return invoke(args, () -> m.invoke(t, converted));
		}
		case "java_get": {
			Field f = field(type(args[0]), args[1]);
			Object target = Modifier.isStatic(f.getModifiers()) ? null : handleObject(value);
			return toPy(f.get(target));
		}
		case "java_set": {
			Field f = field(type(args[0]), args[1]);
			PyValue.PyDict d = (PyValue.PyDict) value;
			Object target = Modifier.isStatic(f.getModifiers()) ? null : handleObject(d.get("target"));
			PyValue v = d.get("value");
			if (cost(v, f.getType()) < 0) {
				throw new IllegalArgumentException("The value can't be assigned to " + f.getName() + " (" + f.getType().getSimpleName() + ")");
			}
			f.set(target, convert(v, f.getType()));
			return null;
		}
		case "java_iter":
			return items(handleObject(value));
		case "java_release":
			release(value);
			return null;
		default:
			throw new IllegalArgumentException("Unknown function: " + function);
		}
	}

	/* ---------------- Classes and members ---------------- */

	static Class<?> type(String name) throws ClassNotFoundException {
		ClassNotFoundException first = null;
		for (ClassLoader loader : new ClassLoader[] { PythonJava.class.getClassLoader(), ij.IJ.getClassLoader(), Thread.currentThread().getContextClassLoader() }) {
			if (loader == null) {
				continue;
			}
			try {
				return Class.forName(name, true, loader);
			} catch (ClassNotFoundException e) {
				if (first == null) {
					first = e;
				}
			}
		}
		throw new ClassNotFoundException("Java class not found: " + name);
	}

	private static PyValue members(Class<?> cls) {
		TreeSet<String> methods = new TreeSet<>(), staticMethods = new TreeSet<>(), fields = new TreeSet<>(), staticFields = new TreeSet<>(),
				classes = new TreeSet<>();
		for (Method m : cls.getMethods()) {
			(Modifier.isStatic(m.getModifiers()) ? staticMethods : methods).add(m.getName());
		}
		for (Field f : cls.getFields()) {
			(Modifier.isStatic(f.getModifiers()) ? staticFields : fields).add(f.getName());
		}
		for (Class<?> c : cls.getClasses()) {
			classes.add(c.getSimpleName());
		}
		/* An instance can also call the static methods and read the static fields */
		methods.addAll(staticMethods);
		fields.addAll(staticFields);
		Map<String, PyValue> m = new LinkedHashMap<>();
		m.put("methods", strings(methods));
		m.put("static_methods", strings(staticMethods));
		m.put("fields", strings(fields));
		m.put("static_fields", strings(staticFields));
		m.put("classes", strings(classes));
		m.put("interface", PyValue.of(cls.isInterface()));
		return new PyValue.PyDict(m);
	}

	private static PyValue strings(Collection<String> s) {
		List<PyValue> l = new ArrayList<>();
		for (String e : s) {
			l.add(PyValue.of(e));
		}
		return new PyValue.PyList(l);
	}

	private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
		return cls.getField(name);
	}

	/*
	 * A public method of a class which is not public (e.g., an iterator of a
	 * collection): the same method of a public class or interface
	 */
	private static Method accessible(Method m) {
		if (Modifier.isPublic(m.getDeclaringClass().getModifiers())) {
			return m;
		}
		for (Class<?> c = m.getDeclaringClass(); c != null; c = c.getSuperclass()) {
			Method found = publicMethod(c, m);
			if (found != null) {
				return found;
			}
		}
		return m;
	}

	private static Method publicMethod(Class<?> c, Method m) {
		if (Modifier.isPublic(c.getModifiers())) {
			try {
				return c.getMethod(m.getName(), m.getParameterTypes());
			} catch (NoSuchMethodException e) {
				/* Not here */
			}
		}
		for (Class<?> i : c.getInterfaces()) {
			Method found = publicMethod(i, m);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	/* ---------------- Invocation ---------------- */

	@FunctionalInterface
	private interface Invocation {
		Object run() throws Exception;
	}

	/* args[2] == "ui": in the UI thread (SWT widgets) */
	private static PyValue invoke(String[] args, Invocation invocation) throws Exception {
		boolean ui = args.length > 2 && "ui".equals(args[2]);
		Object result;
		if (ui && Display.getDefault().getThread() != Thread.currentThread()) {
			Object[] r = new Object[1];
			Exception[] error = new Exception[1];
			Display.getDefault().syncExec(() -> {
				try {
					r[0] = invocation.run();
				} catch (Exception e) {
					error[0] = e;
				}
			});
			if (error[0] != null) {
				throw unwrap(error[0]);
			}
			result = r[0];
		} else {
			try {
				result = invocation.run();
			} catch (Exception e) {
				throw unwrap(e);
			}
		}
		return toPy(result);
	}

	/* The exception of the Java code: its class and message for Python */
	private static Exception unwrap(Exception e) {
		Throwable t = e instanceof InvocationTargetException ite && ite.getCause() != null ? ite.getCause() : e;
		String msg = t.getMessage();
		return new RuntimeException(t.getClass().getName() + (msg == null ? "" : ": " + msg), t);
	}

	private static List<PyValue> arguments(PyValue value) {
		List<PyValue> list = new ArrayList<>();
		if (value instanceof PyValue.PyDict d && d.get("args") instanceof PyValue.PyDict a) {
			/* The arguments by position ("0", "1", ...): a list of Python numbers would be one array */
			for (int i = 0; i < a.values().size(); i++) {
				list.add(a.get(String.valueOf(i)));
			}
		}
		return list;
	}

	/* The method or constructor whose parameters fit the arguments best */
	private static Executable select(List<? extends Executable> candidates, List<PyValue> args, String what) {
		Executable best = null;
		int bestCost = Integer.MAX_VALUE;
		for (Executable e : candidates) {
			int c = cost(e, args);
			if (c >= 0 && c < bestCost) {
				best = e;
				bestCost = c;
			}
		}
		if (best == null) {
			StringBuilder sig = new StringBuilder();
			for (Executable e : candidates) {
				sig.append("\n  ").append(signature(e));
			}
			throw new IllegalArgumentException("No " + what + " for the arguments (" + describe(args) + "). Available:" + sig);
		}
		return best;
	}

	private static String signature(Executable e) {
		StringBuilder s = new StringBuilder(e instanceof Constructor ? e.getDeclaringClass().getSimpleName() : e.getName()).append('(');
		Class<?>[] p = e.getParameterTypes();
		for (int i = 0; i < p.length; i++) {
			s.append(i > 0 ? ", " : "").append(p[i].getSimpleName());
		}
		return s.append(')').toString();
	}

	private static String describe(List<PyValue> args) {
		StringBuilder s = new StringBuilder();
		for (PyValue v : args) {
			if (s.length() > 0) {
				s.append(", ");
			}
			s.append(switch (v) {
			case null -> "None";
			case PyValue.None n -> "None";
			case PyValue.Array a -> a.shape().length == 0 ? scalarKind(a) : "array " + a.dtype() + java.util.Arrays.toString(a.shape());
			case PyValue.Str t -> t.length() == 1 ? "str" : "list of str";
			case PyValue.PyDict d when d.get("__java__") != null -> {
				Object o = handleObject(d);
				yield o == null ? "null" : o.getClass().getSimpleName();
			}
			case PyValue.PyDict d -> "dict";
			case PyValue.PyList l -> "list";
			default -> v.getClass().getSimpleName();
			});
		}
		return s.toString();
	}

	/* The cost of the conversion of all arguments (-1: impossible), with varargs */
	private static int cost(Executable e, List<PyValue> args) {
		Class<?>[] p = e.getParameterTypes();
		if (!e.isVarArgs() || (args.size() == p.length && cost(args.get(p.length - 1), p[p.length - 1]) >= 0)) {
			if (args.size() != p.length) {
				return -1;
			}
			int total = 0;
			for (int i = 0; i < p.length; i++) {
				int c = cost(args.get(i), p[i]);
				if (c < 0) {
					return -1;
				}
				total += c;
			}
			return total;
		}
		/* Varargs: the last arguments as array */
		if (args.size() < p.length - 1) {
			return -1;
		}
		int total = 3;
		for (int i = 0; i < p.length - 1; i++) {
			int c = cost(args.get(i), p[i]);
			if (c < 0) {
				return -1;
			}
			total += c;
		}
		Class<?> component = p[p.length - 1].getComponentType();
		for (int i = p.length - 1; i < args.size(); i++) {
			int c = cost(args.get(i), component);
			if (c < 0) {
				return -1;
			}
			total += c;
		}
		return total;
	}

	private static Object[] convertArgs(Executable e, List<PyValue> args) {
		Class<?>[] p = e.getParameterTypes();
		Object[] out = new Object[p.length];
		boolean packed = e.isVarArgs() && !(args.size() == p.length && cost(args.get(p.length - 1), p[p.length - 1]) >= 0);
		int fixed = packed ? p.length - 1 : p.length;
		for (int i = 0; i < fixed; i++) {
			out[i] = convert(args.get(i), p[i]);
		}
		if (packed) {
			Class<?> component = p[p.length - 1].getComponentType();
			Object array = Array.newInstance(component, args.size() - fixed);
			for (int i = fixed; i < args.size(); i++) {
				Array.set(array, i - fixed, convert(args.get(i), component));
			}
			out[p.length - 1] = array;
		}
		return out;
	}

	/* ---------------- Python -> Java ---------------- */

	private static String scalarKind(PyValue.Array a) {
		char k = a.dtype().charAt(0);
		return k == 'b' ? "bool" : k == 'f' ? "float" : "int";
	}

	private static boolean isScalar(PyValue v) {
		return v instanceof PyValue.Array a && a.shape().length == 0;
	}

	private static boolean isHandle(PyValue v) {
		return v instanceof PyValue.PyDict d && d.get("__java__") != null;
	}

	private static boolean isClassHandle(PyValue v) {
		return v instanceof PyValue.PyDict d && d.get("__javaclass__") != null;
	}

	/* The conversion cost of a value to a type: 0 exact, higher worse, -1 impossible */
	static int cost(PyValue v, Class<?> t) {
		if (v == null || v instanceof PyValue.None) {
			return t.isPrimitive() ? -1 : 1;
		}
		if (t == PyValue.class) {
			return 4;
		}
		if (isHandle(v)) {
			Object o = handleObject(v);
			if (o == null) {
				return t.isPrimitive() ? -1 : 1;
			}
			Class<?> boxed = box(t);
			if (!boxed.isInstance(o)) {
				return -1;
			}
			/* The nearer the type, the better */
			int d = 0;
			for (Class<?> c = o.getClass(); c != null && c != boxed; c = c.getSuperclass()) {
				d++;
			}
			return Math.min(d, 8) + (t.isPrimitive() ? 1 : 0);
		}
		if (isClassHandle(v)) {
			return t == Class.class ? 0 : t == Object.class ? 5 : -1;
		}
		if (isScalar(v)) {
			String k = scalarKind((PyValue.Array) v);
			Class<?> b = box(t);
			switch (k) {
			case "bool":
				return b == Boolean.class ? 0 : t == Object.class ? 6 : -1;
			case "int":
				if (b == Integer.class) {
					return 0;
				}
				if (b == Long.class) {
					return 1;
				}
				if (b == Double.class) {
					return 2;
				}
				if (b == Float.class) {
					return 3;
				}
				if (b == Short.class || b == Byte.class) {
					return 4;
				}
				return b == Number.class || t == Object.class ? 6 : -1;
			default:
				if (b == Double.class) {
					return 0;
				}
				if (b == Float.class) {
					return 1;
				}
				return b == Number.class || t == Object.class ? 6 : -1;
			}
		}
		if (v instanceof PyValue.Str s) {
			if (s.length() == 1) {
				if (t == String.class) {
					return 0;
				}
				if (t == CharSequence.class) {
					return 1;
				}
				if ((t == char.class || t == Character.class) && s.values()[0] != null && s.values()[0].length() == 1) {
					return 2;
				}
				if (t == String[].class) {
					return 4;
				}
				return t == Object.class ? 6 : -1;
			}
			if (t == String[].class) {
				return 0;
			}
			if (t == List.class || t == Collection.class || t == Iterable.class) {
				return 2;
			}
			return t == Object.class || t == Object[].class ? 6 : -1;
		}
		if (v instanceof PyValue.Array a) {
			int dims = a.shape().length;
			int td = dimensions(t);
			Class<?> base = baseComponent(t);
			if (td == 0) {
				return t == Object.class ? 6 : (t == List.class || t == Collection.class) && dims == 1 ? 4 : -1;
			}
			if (td != dims || !(base.isPrimitive() || base == Object.class || Number.class.isAssignableFrom(box(base)))) {
				return -1;
			}
			if (!base.isPrimitive()) {
				return 5;
			}
			if (base == boolean.class) {
				return a.dtype().equals("b1") ? 0 : -1;
			}
			return base == natural(a.dtype()) ? 0 : 2;
		}
		if (v instanceof PyValue.PyList l) {
			if (t.isArray()) {
				Class<?> c = t.getComponentType();
				int total = 1;
				for (PyValue e : l.values()) {
					int ce = cost(e, c);
					if (ce < 0) {
						return -1;
					}
					total += ce;
				}
				return Math.min(total, 9);
			}
			if (t == List.class || t == Collection.class || t == Iterable.class) {
				return 2;
			}
			return t == Object.class ? 6 : -1;
		}
		if (v instanceof PyValue.PyDict) {
			return t == Map.class ? 1 : t == Object.class ? 6 : -1;
		}
		return t == Object.class ? 8 : -1;
	}

	private static int dimensions(Class<?> t) {
		int d = 0;
		while (t.isArray()) {
			t = t.getComponentType();
			d++;
		}
		return d;
	}

	private static Class<?> baseComponent(Class<?> t) {
		while (t.isArray()) {
			t = t.getComponentType();
		}
		return t;
	}

	/* The Java type of a numpy dtype (unsigned as the signed type of the same size) */
	private static Class<?> natural(String dtype) {
		return switch (dtype) {
		case "f8" -> double.class;
		case "f4" -> float.class;
		case "i8", "u8" -> long.class;
		case "i4", "u4" -> int.class;
		case "i2", "u2" -> short.class;
		case "i1", "u1" -> byte.class;
		case "b1" -> boolean.class;
		default -> double.class;
		};
	}

	private static Class<?> box(Class<?> t) {
		if (!t.isPrimitive()) {
			return t;
		}
		return t == int.class ? Integer.class
				: t == long.class ? Long.class
						: t == double.class ? Double.class
								: t == float.class ? Float.class
										: t == boolean.class ? Boolean.class
												: t == short.class ? Short.class : t == byte.class ? Byte.class : t == char.class ? Character.class : Void.class;
	}

	static Object convert(PyValue v, Class<?> t) {
		if (v == null || v instanceof PyValue.None) {
			return null;
		}
		if (t == PyValue.class) {
			return v;
		}
		if (isHandle(v)) {
			return handleObject(v);
		}
		if (isClassHandle(v)) {
			try {
				return type(((PyValue.Str) ((PyValue.PyDict) v).get("__javaclass__")).asString());
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(e.getMessage());
			}
		}
		if (isScalar(v)) {
			PyValue.Array a = (PyValue.Array) v;
			if (a.dtype().equals("b1")) {
				return ((boolean[]) a.data())[0];
			}
			double d = a.getDouble(0);
			Class<?> b = box(t);
			if (b == Integer.class) {
				return (int) d;
			}
			if (b == Long.class) {
				return a.data() instanceof long[] l ? l[0] : (long) d;
			}
			if (b == Double.class) {
				return d;
			}
			if (b == Float.class) {
				return (float) d;
			}
			if (b == Short.class) {
				return (short) d;
			}
			if (b == Byte.class) {
				return (byte) d;
			}
			/* Number or Object: an integer as Integer (if it fits), else Long; a float as Double */
			if (scalarKind(a).equals("int")) {
				long l = a.data() instanceof long[] ls ? ls[0] : (long) d;
				return l == (int) l ? (Object) (int) l : (Object) l;
			}
			return d;
		}
		if (v instanceof PyValue.Str s) {
			if (s.length() == 1 && (t == char.class || t == Character.class)) {
				return s.values()[0].charAt(0);
			}
			if (t == String[].class || t == Object[].class) {
				return s.values().clone();
			}
			if (t == List.class || t == Collection.class || t == Iterable.class) {
				return new ArrayList<>(List.of(s.values()));
			}
			return s.length() == 1 ? s.values()[0] : s.values().clone();
		}
		if (v instanceof PyValue.Array a) {
			if (!t.isArray()) {
				if (t == List.class || t == Collection.class) {
					/* A list: the numbers boxed */
					Object prim = toArray(a, natural(a.dtype()), 1);
					List<Object> list = new ArrayList<>();
					for (int i = 0; i < Array.getLength(prim); i++) {
						list.add(Array.get(prim, i));
					}
					return list;
				}
				/* Object (e.g., the pixels of ImageProcessor.setPixels): the primitive array of the dtype */
				return toArray(a, natural(a.dtype()), a.shape().length);
			}
			return toArray(a, baseComponent(t), dimensions(t));
		}
		if (v instanceof PyValue.PyList l) {
			if (t.isArray()) {
				Class<?> c = t.getComponentType();
				Object array = Array.newInstance(c, l.values().size());
				for (int i = 0; i < l.values().size(); i++) {
					Array.set(array, i, convert(l.values().get(i), c));
				}
				return array;
			}
			List<Object> list = new ArrayList<>();
			for (PyValue e : l.values()) {
				list.add(convert(e, Object.class));
			}
			return list;
		}
		if (v instanceof PyValue.PyDict d) {
			Map<String, Object> map = new LinkedHashMap<>();
			for (Map.Entry<String, PyValue> e : d.values().entrySet()) {
				map.put(e.getKey(), convert(e.getValue(), Object.class));
			}
			return map;
		}
		return v;
	}

	/* A numpy array as Java array of the base type with the number of dimensions (row-major) */
	private static Object toArray(PyValue.Array a, Class<?> base, int dims) {
		int[] shape = a.shape();
		int n = a.length();
		Class<?> prim = base.isPrimitive() ? base : box(natural(a.dtype())) == base ? natural(a.dtype()) : null;
		Object flat;
		Class<?> natural = natural(a.dtype());
		if (prim != null && prim == natural) {
			flat = a.data();
		} else {
			Class<?> c = prim != null ? prim : base;
			flat = Array.newInstance(c, n);
			for (int i = 0; i < n; i++) {
				double d = a.dtype().equals("b1") ? (((boolean[]) a.data())[i] ? 1 : 0) : a.getDouble(i);
				Array.set(flat, i, number(d, c));
			}
		}
		if (dims <= 1 || shape.length <= 1) {
			return flat;
		}
		return nest(flat, shape, 0, 0);
	}

	private static Object number(double d, Class<?> c) {
		Class<?> b = box(c);
		if (b == Integer.class) {
			return (int) d;
		}
		if (b == Long.class) {
			return (long) d;
		}
		if (b == Float.class) {
			return (float) d;
		}
		if (b == Short.class) {
			return (short) d;
		}
		if (b == Byte.class) {
			return (byte) d;
		}
		if (b == Boolean.class) {
			return d != 0;
		}
		return d;
	}

	private static Object nest(Object flat, int[] shape, int dim, int offset) {
		Class<?> component = flat.getClass().getComponentType();
		if (dim == shape.length - 1) {
			Object row = Array.newInstance(component, shape[dim]);
			System.arraycopy(flat, offset, row, 0, shape[dim]);
			return row;
		}
		int step = 1;
		for (int i = dim + 1; i < shape.length; i++) {
			step *= shape[i];
		}
		Object sample = Array.newInstance(component, new int[shape.length - dim - 1]);
		Object out = Array.newInstance(sample.getClass(), shape[dim]);
		for (int i = 0; i < shape[dim]; i++) {
			Array.set(out, i, nest(flat, shape, dim + 1, offset + i * step));
		}
		return out;
	}

	/* ---------------- Java -> Python ---------------- */

	static PyValue toPy(Object o) {
		switch (o) {
		case null:
			return PyValue.None.INSTANCE;
		case String s:
			return PyValue.of(s);
		case Character c:
			return PyValue.of(String.valueOf(c));
		case Boolean b:
			return new PyValue.Array(new boolean[] { b }, "b1", new int[0]);
		case Byte b:
			return new PyValue.Array(new long[] { b }, "i8", new int[0]);
		case Short s:
			return new PyValue.Array(new long[] { s }, "i8", new int[0]);
		case Integer i:
			return new PyValue.Array(new long[] { i }, "i8", new int[0]);
		case Long l:
			return new PyValue.Array(new long[] { l }, "i8", new int[0]);
		case Float f:
			return PyValue.scalar(f);
		case Double d:
			return PyValue.scalar(d);
		case byte[] b:
			/* The pixels of ImageJ (8-bit): unsigned */
			return new PyValue.Array(b, "u1", null);
		case short[] s:
			/* The pixels of ImageJ (16-bit): unsigned */
			return new PyValue.Array(s, "u2", null);
		case int[] i:
			return new PyValue.Array(i, "i4", null);
		case long[] l:
			return new PyValue.Array(l, "i8", null);
		case float[] f:
			return new PyValue.Array(f, "f4", null);
		case double[] d:
			return new PyValue.Array(d, "f8", null);
		case boolean[] b:
			return new PyValue.Array(b, "b1", null);
		case char[] c:
			return PyValue.of(new String(c));
		default:
			break;
		}
		Class<?> cls = o.getClass();
		if (cls.isArray()) {
			PyValue matrix = matrix(o);
			if (matrix != null) {
				return matrix;
			}
			List<PyValue> list = new ArrayList<>();
			for (int i = 0; i < Array.getLength(o); i++) {
				list.add(toPy(Array.get(o, i)));
			}
			return new PyValue.PyList(list);
		}
		return handle(o);
	}

	/* A rectangular 2-d primitive array (e.g., float[][]) as 2-d numpy array */
	private static PyValue matrix(Object o) {
		Class<?> c = o.getClass().getComponentType();
		if (!c.isArray() || !c.getComponentType().isPrimitive() || c.getComponentType() == char.class) {
			return null;
		}
		int rows = Array.getLength(o);
		if (rows == 0) {
			return null;
		}
		int cols = Array.getLength(Array.get(o, 0));
		for (int i = 1; i < rows; i++) {
			if (Array.get(o, i) == null || Array.getLength(Array.get(o, i)) != cols) {
				return null;
			}
		}
		Object flat = Array.newInstance(c.getComponentType(), rows * cols);
		for (int i = 0; i < rows; i++) {
			System.arraycopy(Array.get(o, i), 0, flat, i * cols, cols);
		}
		PyValue.Array a = (PyValue.Array) toPy(flat);
		return new PyValue.Array(a.data(), a.dtype(), new int[] { rows, cols });
	}

	/* The elements of an Iterable, a Map (its keys), an Iterator or an Enumeration */
	private static PyValue items(Object o) {
		List<PyValue> list = new ArrayList<>();
		if (o instanceof Iterable<?> it) {
			for (Object e : it) {
				list.add(toPy(e));
			}
		} else if (o instanceof Map<?, ?> m) {
			for (Object e : m.keySet()) {
				list.add(toPy(e));
			}
		} else if (o instanceof java.util.Iterator<?> it) {
			while (it.hasNext()) {
				list.add(toPy(it.next()));
			}
		} else if (o instanceof java.util.Enumeration<?> en) {
			while (en.hasMoreElements()) {
				list.add(toPy(en.nextElement()));
			}
		} else {
			throw new IllegalArgumentException("Not iterable: " + (o == null ? "null" : o.getClass().getName()));
		}
		return new PyValue.PyList(list);
	}

	/* ---------------- Handles ---------------- */

	private static synchronized PyValue handle(Object o) {
		Long id = IDS.get(o);
		if (id == null) {
			id = NEXT.getAndIncrement();
			IDS.put(o, id);
			OBJECTS.put(id, o);
			COUNTS.put(id, new int[] { 0 });
		}
		COUNTS.get(id)[0]++;
		Map<String, PyValue> m = new LinkedHashMap<>();
		m.put("__java__", new PyValue.Array(new long[] { id }, "i8", new int[0]));
		m.put("class", PyValue.of(o.getClass().getName()));
		return new PyValue.PyDict(m);
	}

	private static synchronized Object handleObject(PyValue v) {
		if (!(v instanceof PyValue.PyDict d) || !(d.get("__java__") instanceof PyValue.Array a)) {
			return null;
		}
		long id = (long) a.getDouble(0);
		Object o = OBJECTS.get(id);
		if (o == null) {
			throw new IllegalStateException("The Java object was released");
		}
		return o;
	}

	/* The handles of the proxies which Python has released */
	private static synchronized void release(PyValue v) {
		if (!(v instanceof PyValue.Array a)) {
			return;
		}
		for (int i = 0; i < a.length(); i++) {
			long id = (long) a.getDouble(i);
			int[] count = COUNTS.get(id);
			if (count != null && --count[0] <= 0) {
				COUNTS.remove(id);
				Object o = OBJECTS.remove(id);
				if (o != null) {
					IDS.remove(o);
				}
			}
		}
	}

	/** The number of Java objects held for Python (for tests). */
	static synchronized int size() {
		return OBJECTS.size();
	}
}
