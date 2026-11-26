/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.simulation;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Runs a model several times without a display (batch runs): a sweep of the
 * parameters (all combinations of the given values), repetitions with other
 * seeds and a number of steps. The runs are computed in parallel threads (one
 * model for each run). The result is a table with a row for each recorded step
 * of each run: the run, the seed, the values of the swept parameters, the step,
 * the values of the line charts in this step (chart(...): the first chart by the
 * name of the series, other charts as "chart: series") and the values of
 * record(name, value).
 *
 * <pre>
 * Experiment.Table t = new Experiment(SheepWolvesField.class)
 * 		.param("growth", 0.02, 0.04, 0.06)
 * 		.param("wolves", 4, 8, 12)
 * 		.steps(500).repetitions(5).every(10)
 * 		.run();
 * t.save("experiment.csv");   // a file of the project (relative path) or an absolute path
 * t.toR("experiment");        // a data frame in R (if R runs)
 * t.toPython("experiment");   // a pandas DataFrame in Python (if Python runs)
 * </pre>
 *
 * The seed of repetition k is seed + k (the same seeds for all parameter
 * values: common random numbers). A run ends early if the model calls stop(). A model which sets its own seed in init()
 * (seed(...)) gets the same numbers in all repetitions. Models with static
 * fields which change should run in one thread (threads(1)).
 */
public final class Experiment {

	private final Class<? extends Simulation> type;
	private final Simulation template;
	private final Map<String, Object[]> sweep = new LinkedHashMap<>();
	/* Given parameter sets (a design: the names, a row of values for each set), or null */
	private String[] designNames;
	private double[][] designRows;
	private int steps = 100, repetitions = 1, seed = Simulation.DEFAULT_SEED, every = 1;
	private int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
	private BiConsumer<Integer, Integer> progress;
	private final AtomicBoolean cancelled = new AtomicBoolean();

	/** An experiment with a model class (the parameters: the defaults of its fields). */
	public Experiment(Class<? extends Simulation> model) {
		this.type = model;
		this.template = null;
	}

	/**
	 * An experiment with the class of a model: the parameters (Param fields) of
	 * this model are the start values of each run (e.g., the settings of the
	 * model in Bio7).
	 */
	public Experiment(Simulation model) {
		this.type = model.getClass();
		this.template = model;
		/* The seed of the model in Bio7 (the settings): the first repetition is the run of Bio7 */
		this.seed = model.getStartSeed();
	}

	/** The values of a parameter (a Param field or another field of the model) in the sweep. */
	public Experiment param(String name, double... values) {
		Object[] v = new Object[values.length];
		for (int i = 0; i < values.length; i++) {
			v[i] = values[i];
		}
		sweep.put(name, v);
		return this;
	}

	/** The values of a parameter of any type (e.g., booleans or the options of a String). */
	public Experiment paramValues(String name, Object... values) {
		sweep.put(name, values.clone());
		return this;
	}

	/**
	 * Given parameter sets instead of (or combined with) the sweep, e.g. a Latin
	 * hypercube or the samples of a sensitivity analysis (created in R or Python):
	 * the names of the parameters and a row of values for each set. Each row is
	 * run with the repetitions; the column "point" of the table is the row of the
	 * design (0, 1, ...). Values of int fields are rounded.
	 *
	 * <pre>
	 * new Experiment(NetworkEpidemic.class).design(new String[] { "infection", "recovery" },
	 * 		new double[][] { { 0.02, 0.04 }, { 0.05, 0.03 }, { 0.08, 0.06 } }).repetitions(5).run();
	 * </pre>
	 */
	public Experiment design(String[] names, double[][] rows) {
		for (double[] r : rows) {
			if (r.length != names.length) {
				throw new IllegalArgumentException("Each row of the design needs " + names.length + " values");
			}
		}
		designNames = names.clone();
		designRows = new double[rows.length][];
		for (int i = 0; i < rows.length; i++) {
			designRows[i] = rows[i].clone();
		}
		return this;
	}

	/** The number of steps of each run (default 100). */
	public Experiment steps(int steps) {
		this.steps = Math.max(0, steps);
		return this;
	}

	/** The number of runs for each combination of the parameters (default 1). */
	public Experiment repetitions(int repetitions) {
		this.repetitions = Math.max(1, repetitions);
		return this;
	}

	/** The seed of the first repetition (default 42, the seed of the model in Bio7). */
	public Experiment seed(int seed) {
		this.seed = seed;
		return this;
	}

	/** A row every n steps (default 1: each step; the step 0 and the last step are always recorded). */
	public Experiment every(int n) {
		this.every = Math.max(1, n);
		return this;
	}

	/** The number of parallel threads (default: the processors - 1). */
	public Experiment threads(int n) {
		this.threads = Math.max(1, n);
		return this;
	}

	/** Called after each run with the number of finished runs and of all runs (from the threads of the runs). */
	public Experiment progress(BiConsumer<Integer, Integer> progress) {
		this.progress = progress;
		return this;
	}

	/** Stops the experiment (the runs which have started end after their current step). */
	public void cancel() {
		cancelled.set(true);
	}

	/** The number of runs (the combinations of the values times the repetitions). */
	public int runs() {
		return combinations(null).size() * repetitions;
	}

	/* The names of the parameters of a run: the design, then the sweep */
	private String[] names() {
		List<String> n = new ArrayList<>();
		if (designNames != null) {
			n.addAll(java.util.Arrays.asList(designNames));
		}
		n.addAll(sweep.keySet());
		return n.toArray(String[]::new);
	}

	/* The parameter sets: each row of the design (its index in points) with each combination of the sweep */
	private List<Object[]> combinations(List<Integer> points) {
		List<Object[]> grid = sweepCombinations();
		if (designRows == null) {
			return grid;
		}
		List<Object[]> out = new ArrayList<>();
		for (int p = 0; p < designRows.length; p++) {
			for (Object[] g : grid) {
				Object[] c = new Object[designNames.length + g.length];
				for (int i = 0; i < designNames.length; i++) {
					c[i] = designRows[p][i];
				}
				System.arraycopy(g, 0, c, designNames.length, g.length);
				out.add(c);
				if (points != null) {
					points.add(p);
				}
			}
		}
		return out;
	}

	/* All combinations of the swept values (the last parameter changes fastest) */
	private List<Object[]> sweepCombinations() {
		List<Object[]> out = new ArrayList<>();
		out.add(new Object[0]);
		for (Object[] values : sweep.values()) {
			List<Object[]> next = new ArrayList<>();
			for (Object[] c : out) {
				for (Object v : values) {
					Object[] d = java.util.Arrays.copyOf(c, c.length + 1);
					d[c.length] = v;
					next.add(d);
				}
			}
			out = next;
		}
		return out;
	}

	/** Runs the experiment (blocks until all runs are finished) and returns the table. */
	public Table run() throws Exception {
		cancelled.set(false);
		List<Integer> points = new ArrayList<>();
		List<Object[]> combos = combinations(points);
		String[] names = names();
		int total = combos.size() * repetitions;
		List<List<Map<String, Object>>> results = new ArrayList<>();
		for (int i = 0; i < total; i++) {
			results.add(null);
		}
		AtomicInteger done = new AtomicInteger();
		ExecutorService pool = Executors.newFixedThreadPool(Math.min(threads, Math.max(1, total)), r -> {
			Thread th = new Thread(r, "Bio7 experiment");
			th.setDaemon(true);
			return th;
		});
		try {
			List<Future<?>> futures = new ArrayList<>();
			for (int c = 0; c < combos.size(); c++) {
				for (int k = 0; k < repetitions; k++) {
					int run = c * repetitions + k, rep = k;
					Object[] combo = combos.get(c);
					int point = designRows == null ? -1 : points.get(c);
					futures.add(pool.submit(() -> {
						results.set(run, runOne(run + 1, point, seed + rep, names, combo));
						if (progress != null) {
							progress.accept(done.incrementAndGet(), total);
						}
						return null;
					}));
				}
			}
			for (Future<?> f : futures) {
				try {
					f.get();
				} catch (java.util.concurrent.ExecutionException e) {
					Throwable cause = e.getCause() != null ? e.getCause() : e;
					throw cause instanceof Exception ex ? ex : new RuntimeException(cause);
				}
			}
		} finally {
			pool.shutdownNow();
		}
		Table t = new Table();
		for (List<Map<String, Object>> rows : results) {
			if (rows != null) {
				t.rows.addAll(rows);
			}
		}
		return t;
	}

	/* One run: a new model, the parameters, the setup and the steps */
	private List<Map<String, Object>> runOne(int run, int point, int runSeed, String[] names, Object[] combo) throws Exception {
		Simulation m = type.getDeclaredConstructor().newInstance();
		m.arrangeViews = false;
		if (template != null) {
			copyParams(template, m);
		}
		for (int i = 0; i < names.length; i++) {
			set(m, names[i], combo[i]);
		}
		m.setupHeadless(runSeed);
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(row(m, run, point, runSeed, names, combo));
		for (int s = 1; s <= steps && !cancelled.get() && !m.isStopped(); s++) {
			m.stepHeadless();
			/* The last step of the run: also a step with stop() */
			if (s % every == 0 || s == steps || m.isStopped()) {
				rows.add(row(m, run, point, runSeed, names, combo));
			}
		}
		return rows;
	}

	private static Map<String, Object> row(Simulation m, int run, int point, int seed, String[] names, Object[] combo) {
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("run", run);
		/* The row of the design (-1: no design) */
		if (point >= 0) {
			r.put("point", point);
		}
		r.put("seed", seed);
		for (int i = 0; i < names.length; i++) {
			/* The value of the field (its type, e.g., an int) */
			r.put(names[i], get(m, names[i], combo[i]));
		}
		r.put("step", m.t);
		r.putAll(m.measures());
		return r;
	}

	/* The Param fields of the model (the settings in Bio7) to another model of the class */
	private static void copyParams(Simulation from, Simulation to) throws IllegalAccessException {
		for (Class<?> k = from.getClass(); k != null && k != Simulation.class; k = k.getSuperclass()) {
			for (Field f : k.getDeclaredFields()) {
				if (f.isAnnotationPresent(Param.class) && !Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers())) {
					f.setAccessible(true);
					f.set(to, f.get(from));
				}
			}
		}
	}

	/* Sets a field of the model (converted to its type) */
	static void set(Object m, String name, Object value) throws IllegalAccessException {
		Field f = field(m.getClass(), name);
		if (f == null) {
			throw new IllegalArgumentException("The model " + m.getClass().getSimpleName() + " has no field " + name);
		}
		f.setAccessible(true);
		Class<?> t = f.getType();
		if (t == int.class || t == Integer.class) {
			f.set(m, (int) Math.round(number(value)));
		} else if (t == long.class || t == Long.class) {
			f.set(m, Math.round(number(value)));
		} else if (t == double.class || t == Double.class) {
			f.set(m, number(value));
		} else if (t == float.class || t == Float.class) {
			f.set(m, (float) number(value));
		} else if (t == boolean.class || t == Boolean.class) {
			f.set(m, value instanceof Boolean b ? b : value instanceof Number n ? n.doubleValue() != 0 : Boolean.parseBoolean(String.valueOf(value).trim()));
		} else if (t == String.class) {
			f.set(m, String.valueOf(value));
		} else {
			throw new IllegalArgumentException("The field " + name + " can't be set by an experiment (type " + t.getSimpleName() + ")");
		}
	}

	private static Object get(Object m, String name, Object otherwise) {
		try {
			Field f = field(m.getClass(), name);
			if (f != null) {
				f.setAccessible(true);
				return f.get(m);
			}
		} catch (IllegalAccessException e) {
			/* the given value */
		}
		return otherwise;
	}

	private static double number(Object v) {
		if (v instanceof Number n) {
			return n.doubleValue();
		}
		if (v instanceof Boolean b) {
			return b ? 1 : 0;
		}
		return Double.parseDouble(String.valueOf(v).trim());
	}

	private static Field field(Class<?> k, String name) {
		for (; k != null; k = k.getSuperclass()) {
			try {
				return k.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				/* the superclass */
			}
		}
		return null;
	}

	/**
	 * The values of a parameter from a text (e.g., of a dialog): "0.1, 0.2, 0.5",
	 * a range "0.1:0.5:0.1" (from:to:step) or "true, false".
	 */
	public static Object[] values(String text) {
		String s = text.trim();
		if (s.matches("[-+0-9.eE]+\\s*:\\s*[-+0-9.eE]+\\s*:\\s*[-+0-9.eE]+")) {
			String[] p = s.split(":");
			double from = Double.parseDouble(p[0].trim()), to = Double.parseDouble(p[1].trim()), step = Math.abs(Double.parseDouble(p[2].trim()));
			if (step == 0) {
				throw new IllegalArgumentException("The step of the range " + s + " is 0");
			}
			List<Object> v = new ArrayList<>();
			int n = (int) Math.floor(Math.abs(to - from) / step + 1e-9);
			for (int i = 0; i <= n; i++) {
				/* Rounded to the digits of the step (no 0.30000000000000004) */
				v.add(Double.parseDouble(String.format(Locale.ROOT, "%.10g", from + Math.signum(to - from) * i * step)));
			}
			return v.toArray();
		}
		List<Object> v = new ArrayList<>();
		for (String part : s.split("[,;]")) {
			String q = part.trim();
			if (q.isEmpty()) {
				continue;
			}
			if (q.equalsIgnoreCase("true") || q.equalsIgnoreCase("false")) {
				v.add(Boolean.parseBoolean(q));
			} else {
				try {
					v.add(Double.parseDouble(q));
				} catch (NumberFormatException e) {
					v.add(q.replaceAll("^\"|\"$", ""));
				}
			}
		}
		return v.toArray();
	}

	/** True if the name is a name of a Python variable (letters, digits, _; not a keyword). */
	public static boolean isPythonName(String name) {
		return name != null && name.matches("[A-Za-z_][A-Za-z0-9_]*") && !PYTHON_KEYWORDS.contains(name);
	}

	private static final java.util.Set<String> PYTHON_KEYWORDS = java.util.Set.of("False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif", "else", "except",
			"finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with", "yield");

	/**
	 * Reads a CSV file of an experiment (NA, TRUE, FALSE) into a variable of the
	 * embedded Python (the namespace of the console, __main__): a pandas DataFrame,
	 * a list of dicts without pandas. Returns false if Python is not running.
	 */
	public static boolean csvToPython(File csv, String name) throws Exception {
		if (!isPythonName(name)) {
			throw new IllegalArgumentException("Not a name of a Python variable: " + name);
		}
		com.eco.bio7.pyffm.PySession s = com.eco.bio7.pybridge.EmbeddedPython.session();
		if (s == null) {
			return false;
		}
		String path = JavaToJs.quote(csv.getAbsolutePath().replace('\\', '/'));
		s.exec("""
				def _bio7_experiment(path, name):
				    import __main__
				    try:
				        import pandas as pd
				        value = pd.read_csv(path, na_values=["NA"], keep_default_na=False, true_values=["TRUE"], false_values=["FALSE"])
				    except ImportError:
				        import csv
				        def cell(x):
				            if x == "NA":
				                return None
				            if x in ("TRUE", "FALSE"):
				                return x == "TRUE"
				            for convert in (int, float):
				                try:
				                    return convert(x)
				                except ValueError:
				                    pass
				            return x
				        with open(path, newline="", encoding="utf-8") as f:
				            value = [{k: cell(v) for k, v in row.items()} for row in csv.DictReader(f)]
				    setattr(__main__, name, value)
				""" + "_bio7_experiment(" + path + ", " + JavaToJs.quote(name) + ")\ndel _bio7_experiment\n");
		return true;
	}

	/** The result of an experiment: a row for each recorded step of each run. */
	public static final class Table {

		final List<Map<String, Object>> rows = new ArrayList<>();

		/** The columns (in the order of their first appearance). */
		public List<String> columns() {
			Set<String> c = new LinkedHashSet<>();
			for (Map<String, Object> r : rows) {
				c.addAll(r.keySet());
			}
			return new ArrayList<>(c);
		}

		/** The rows (column -> value; a missing value: null). */
		public List<Map<String, Object>> rows() {
			return java.util.Collections.unmodifiableList(rows);
		}

		/** The number of rows. */
		public int size() {
			return rows.size();
		}

		/** The values of a numeric column (NaN: missing or not a number). */
		public double[] column(String name) {
			double[] v = new double[rows.size()];
			for (int i = 0; i < v.length; i++) {
				Object o = rows.get(i).get(name);
				v[i] = o instanceof Number n ? n.doubleValue() : o instanceof Boolean b ? (b ? 1 : 0) : Double.NaN;
			}
			return v;
		}

		/** The table as CSV (comma separated, a header, NA for missing values). */
		public String csv() {
			List<String> cols = columns();
			StringBuilder b = new StringBuilder();
			for (int i = 0; i < cols.size(); i++) {
				b.append(i > 0 ? "," : "").append(quote(cols.get(i)));
			}
			b.append('\n');
			for (Map<String, Object> r : rows) {
				for (int i = 0; i < cols.size(); i++) {
					Object o = r.get(cols.get(i));
					b.append(i > 0 ? "," : "");
					if (o == null || o instanceof Double d && d.isNaN()) {
						b.append("NA");
					} else if (o instanceof Number || o instanceof Boolean) {
						b.append(o instanceof Boolean bo ? (bo ? "TRUE" : "FALSE") : number((Number) o));
					} else {
						b.append(quote(String.valueOf(o)));
					}
				}
				b.append('\n');
			}
			return b.toString();
		}

		private static String number(Number n) {
			if (n instanceof Integer || n instanceof Long) {
				return n.toString();
			}
			double d = n.doubleValue();
			return d == Math.rint(d) && Math.abs(d) < 1e15 ? Long.toString((long) d) : Double.toString(d);
		}

		private static String quote(String s) {
			return "\"" + s.replace("\"", "\"\"") + "\"";
		}

		/**
		 * Saves the table as a CSV file: an absolute path or a path relative to the
		 * project of the model (the folder of the workspace for Bio7 models). Returns
		 * the file.
		 */
		public File save(String file) throws IOException {
			File f = new File(file);
			if (!f.isAbsolute()) {
				f = new File(baseFolder(), file);
			}
			if (f.getParentFile() != null) {
				f.getParentFile().mkdirs();
			}
			Files.writeString(f.toPath(), csv(), StandardCharsets.UTF_8);
			return f;
		}

		private static File baseFolder() {
			try {
				return org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
			} catch (Throwable e) {
				return new File(System.getProperty("user.dir"));
			}
		}

		/**
		 * The table as a data frame with the name in R (Bio7 with a running R; the
		 * names of the columns are kept). Returns false if R is not running.
		 */
		public boolean toR(String name) throws Exception {
			com.eco.bio7.rffm.RSession s = com.eco.bio7.rffm.RSession.current();
			if (s == null || !s.isAlive()) {
				return false;
			}
			File tmp = File.createTempFile("bio7experiment", ".csv");
			try {
				Files.writeString(tmp.toPath(), csv(), StandardCharsets.UTF_8);
				s.voidEval(name + " <- utils::read.csv(" + JavaToJs.quote(tmp.getAbsolutePath().replace('\\', '/')) + ", check.names = FALSE, stringsAsFactors = FALSE)");
			} finally {
				tmp.delete();
			}
			return true;
		}

		/**
		 * The table as a variable with the name in the embedded Python (Bio7 with a
		 * running Python): a pandas DataFrame, a list of dicts without pandas.
		 * Returns false if Python is not running.
		 */
		public boolean toPython(String name) throws Exception {
			File tmp = File.createTempFile("bio7experiment", ".csv");
			try {
				Files.writeString(tmp.toPath(), csv(), StandardCharsets.UTF_8);
				return csvToPython(tmp, name);
			} finally {
				tmp.delete();
			}
		}

		@Override
		public String toString() {
			return rows.size() + " rows, columns " + columns();
		}
	}
}
