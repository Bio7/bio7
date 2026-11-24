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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.compile.Model;

/**
 * The base class of the simulation models of the simulation API (see
 * {@link GridModel}, {@link ValueGridModel} and {@link AgentModel}). A model implements init() (the
 * start state) and step() (one time step). Bio7 calls them with the Setup
 * button and the Play/Pause button (the time interval of the Play/Pause
 * button sets the speed); the model is displayed in an ImageJ image (grids) or
 * in the 3D view (agents).
 * <p>
 * The same model runs in the web browser: "Export Simulation as HTML..." (in
 * the menu of the Play/Pause button) translates the model class to JavaScript.
 * Fields with the annotation {@link Param} become controls of the page and of
 * the "Settings" view in Bio7. The
 * random numbers of {@link #random()} are the same in Java and JavaScript (same
 * seed, same numbers).
 * <p>
 * The translation supports a subset of Java: fields, methods, static nested
 * classes, control flow, arrays, Math, lambdas, ArrayList and String
 * concatenation (see the documentation of the export for the details).
 */
public abstract class Simulation extends Model {

	/** The seed of the random numbers (set it in init() with seed(...)). */
	public static final int DEFAULT_SEED = 42;

	/** The number of the time steps since the setup. */
	public int t;

	private SimRandom random = new SimRandom(DEFAULT_SEED);
	private int seedValue = DEFAULT_SEED;

	/* The seed of the setup (the field Seed of the settings; init() can set another with seed(...)) */
	private volatile int startSeed = DEFAULT_SEED;

	/* stop() in this step: the simulation ends (Bio7 pauses, the page stops, an experiment ends the run) */
	private volatile boolean stopped;

	/* The charts (the first chart has the title "") */
	private final Map<String, ChartData> charts = new LinkedHashMap<>();

	/* The shapes drawn in the current step (cleared before each step) */
	private final List<Shape> drawing = new ArrayList<>();

	/* The images and the 3D models (glTF) of the model: the name and the file */
	private final Map<String, String> imageFiles = new LinkedHashMap<>(), modelFiles = new LinkedHashMap<>();

	private volatile boolean ready;

	/*
	 * A run without a display (an Experiment): no views, no charts view, no
	 * display() of the steps
	 */
	volatile boolean headless;

	/* The values of record(name, value) of the current step (the columns of an Experiment) */
	private final Map<String, Double> records = new LinkedHashMap<>();

	private boolean settingsShown;

	/**
	 * true (default): the first setup arranges the views in the Custom perspective
	 * of Bio7: the display of the model (ImageJ, the 3D view or the WorldWind globe)
	 * on the right, its options (e.g., Options3D) top left and the settings left.
	 * false: the views open in the current perspective.
	 */
	public boolean arrangeViews = true;

	/* The display views of the model (on the right of the Custom perspective) */
	String[] displayViews() {
		return new String[] { "com.eco.bio7.imagej" };
	}

	/* The name of the model (the title of its image and settings), the class name; a script model: the name of the script model */
	String modelName() {
		return getClass().getSimpleName();
	}

	/* The options view of the display (id and the title of its panel), null: none */
	String[] optionsView() {
		return null;
	}

	protected Simulation() {
	}

	/**
	 * Starts the model after its creation by the compiler of Bio7: the setup in
	 * the thread of the user interface. It is called after the constructor has
	 * finished (a setup from the constructor could run before the fields of the
	 * subclass are initialized).
	 */
	public void startAfterCreation() {
		ui(() -> {
			if (!ready) {
				setup();
			}
		}, true);
	}

	/** Creates the start state (called by the Setup button and after the compilation). */
	public abstract void init();

	/** One time step (called by the Play/Pause button). */
	public abstract void step();

	/** Displays the model (ImageJ image or 3D view). */
	protected abstract void display();

	/**
	 * A click of the mouse in the view of the model (override it): for a grid the
	 * position in cells (the cell {@code ((int) x, (int) y)}, y from the top as
	 * in get(x, y)), for an agent model the point on the floor of the box. Called
	 * between the steps (also when the simulation is paused; the view is updated
	 * after the call), in the HTML export with the same values.
	 *
	 * @param x the x position
	 * @param y the y position
	 */
	public void click(double x, double y) {
	}

	/**
	 * A drag of the mouse in the view of the model (override it), e.g., to paint
	 * cells: called for the position where the drag started and for each further
	 * position (a grid: each new cell). In the 2D view a drag with the left mouse
	 * button, in the 3D view with the Shift key (a drag without it rotates the
	 * view). The positions as in {@link #click(double, double)}.
	 *
	 * @param x the x position
	 * @param y the y position
	 */
	public void drag(double x, double y) {
	}

	/**
	 * A key pressed in the view of the model (override it): a character ("a",
	 * "A", "1", " ") or the name of a special key ("ArrowLeft", "ArrowRight",
	 * "ArrowUp", "ArrowDown", "Enter", "Escape", "Backspace", "Delete", "Tab"; the
	 * names of the key of JavaScript). With this method, the view gives the keys to
	 * the model (not to its own shortcuts).
	 *
	 * @param key the key
	 */
	public void key(String key) {
	}

	/* The input of the views: one after the other, between the steps (Java models; scripts in the thread of the script) */
	private static final java.util.concurrent.ExecutorService INPUT = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "Bio7 simulation input");
		t.setDaemon(true);
		return t;
	});

	/* A drag which waits (fast drags of the 3D view: only the last position) */
	private final java.util.concurrent.atomic.AtomicReference<double[]> pendingDrag = new java.util.concurrent.atomic.AtomicReference<>();

	/* True if the model handles the input ("click", "drag" or "key"): its method is overridden (a script: defined) */
	boolean handles(String event) {
		if (this instanceof ScriptSimulation.Host h) {
			return h.script().inputs().contains(event);
		}
		try {
			java.lang.reflect.Method m = "key".equals(event) ? getClass().getMethod("key", String.class) : getClass().getMethod(event, double.class, double.class);
			return m.getDeclaringClass() != Simulation.class;
		} catch (NoSuchMethodException | SecurityException e) {
			return false;
		}
	}

	/*
	 * An input of a view: "click", "drag" (x, y) or "key" (the key). Queued and
	 * called between the steps; coalesce: a drag replaces a drag which still waits
	 */
	void input(String event, double x, double y, String key, boolean coalesce) {
		if (!ready || headless || !handles(event)) {
			return;
		}
		boolean merge = coalesce && "drag".equals(event);
		if (merge && pendingDrag.getAndSet(new double[] { x, y }) != null) {
			return;
		}
		java.util.function.Supplier<double[]> position = () -> {
			if (merge) {
				double[] p = pendingDrag.getAndSet(null);
				if (p != null) {
					return p;
				}
			}
			return new double[] { x, y };
		};
		if (this instanceof ScriptSimulation.Host h) {
			h.script().input(this, () -> {
				if ("key".equals(event)) {
					return "key\u001f" + key;
				}
				double[] p = position.get();
				return event + "\u001f" + p[0] + "\u001f" + p[1];
			});
			return;
		}
		INPUT.execute(() -> {
			try {
				synchronized (this) {
					if ("key".equals(event)) {
						key(key);
					} else {
						double[] p = position.get();
						if ("click".equals(event)) {
							click(p[0], p[1]);
						} else {
							drag(p[0], p[1]);
						}
					}
				}
				display();
				ChartDisplay.update(this);
			} catch (RuntimeException e) {
				System.err.println("The input " + event + " of the model failed:");
				e.printStackTrace();
			}
		});
	}

	/* The name of a key (as KeyboardEvent.key of JavaScript): a character or a special key; null for keys which are not given to the model */
	static String keyName(char c, String special) {
		if (special != null) {
			/* "": a modifier key (Shift, Ctrl, ...) */
			return special.isEmpty() ? null : special;
		}
		if (c >= ' ' && c != 127 && c != 0xFFFF) {
			return String.valueOf(c);
		}
		return null;
	}

	/** Prepares the display before init() (called in the thread of the user interface). */
	protected void prepareDisplay() {
	}

	@Override
	public void setup() {
		/* The first setup: the Custom perspective with the views of the model */
		boolean first = !settingsShown && hasDisplay();
		if (first && arrangeViews) {
			if (Display.getCurrent() != null) {
				SimulationLayout.arrangeNew(this);
			} else {
				PlatformUI.getWorkbench().getDisplay().syncExec(() -> SimulationLayout.arrangeNew(this));
			}
		}
		synchronized (this) {
			t = 0;
			random = new SimRandom(startSeed);
			seedValue = startSeed;
			stopped = false;
			synchronized (charts) {
				charts.clear();
			}
			synchronized (drawing) {
				drawing.clear();
			}
			prepareDisplay();
			init();
			ready = true;
		}
		ChartDisplay.update(this);
		/* The settings of the Param fields (once for the model) */
		if (!settingsShown && hasDisplay()) {
			settingsShown = true;
			ui(() -> SimulationSettings.show(this));
		}
		display();
	}

	/* The setup of a run without a display (an Experiment): the seed of the run */
	void setupHeadless(int seed) {
		headless = true;
		synchronized (this) {
			t = 0;
			stopped = false;
			random = new SimRandom(seed);
			seedValue = seed;
			synchronized (charts) {
				charts.clear();
			}
			synchronized (drawing) {
				drawing.clear();
			}
			records.clear();
			seriesBefore.clear();
			prepareDisplay();
			init();
			ready = true;
		}
	}

	/* The lengths of the line chart series before the step (a value of the step: the series is longer) */
	private final Map<String, Integer> seriesBefore = new java.util.HashMap<>();

	/* A step of a run without a display */
	void stepHeadless() {
		seriesBefore.clear();
		synchronized (charts) {
			for (ChartData c : charts.values()) {
				for (Map.Entry<String, List<double[]>> e : c.series.entrySet()) {
					seriesBefore.put(c.title + "\u001f" + e.getKey(), e.getValue().size());
				}
			}
		}
		synchronized (this) {
			synchronized (drawing) {
				drawing.clear();
			}
			stopped = false;
			step();
			t++;
		}
	}

	/**
	 * Ends the simulation after this step, e.g., {@code if (infected == 0) stop();}
	 * in step(): Bio7 pauses the simulation (Play continues it), the HTML page
	 * stops, an Experiment ends the run (its last step is recorded).
	 */
	public void stop() {
		stopped = true;
	}

	/** True if stop() was called in the last step (or in init()). */
	public boolean isStopped() {
		return stopped;
	}

	/** The seed of the setup (default 42; the field Seed of the settings in Bio7). */
	public int getStartSeed() {
		return startSeed;
	}

	/**
	 * Sets the seed of the next setup (the field Seed of the settings). A seed set
	 * in init() with seed(...) has priority.
	 */
	public void setStartSeed(int seed) {
		startSeed = seed;
	}

	/**
	 * Records a value of the current step for an Experiment (a column of its
	 * table, besides the values of the line charts). Without an Experiment, the
	 * value is kept but not shown.
	 */
	public void record(String name, double value) {
		synchronized (records) {
			records.put(name, value);
		}
	}

	/*
	 * The values of the step for an Experiment: the values of the line charts in
	 * this step (the first chart: the name of the series, the others "chart:
	 * series") and the recorded values
	 */
	Map<String, Double> measures() {
		Map<String, Double> m = new LinkedHashMap<>();
		synchronized (charts) {
			for (ChartData c : charts.values()) {
				if (!ChartData.LINE.equals(c.kind)) {
					continue;
				}
				for (Map.Entry<String, List<double[]>> e : c.series.entrySet()) {
					List<double[]> s = e.getValue();
					Integer before = seriesBefore.get(c.title + "\u001f" + e.getKey());
					boolean added = !s.isEmpty() && (before == null || s.size() > before);
					m.put(c.title.isEmpty() ? e.getKey() : c.title + ": " + e.getKey(), added ? s.get(s.size() - 1)[1] : Double.NaN);
				}
			}
		}
		synchronized (records) {
			m.putAll(records);
		}
		return m;
	}

	@Override
	public void run() {
		if (!ready || headless) {
			return;
		}
		synchronized (this) {
			synchronized (drawing) {
				drawing.clear();
			}
			stopped = false;
			step();
			t++;
		}
		display();
		ChartDisplay.update(this);
		/* stop() in the step: the simulation pauses (the Play button continues it) */
		if (stopped && hasDisplay()) {
			/* At once (thread-safe; its listeners update the user interface): no further step of the timer */
			com.eco.bio7.time.Time.setPause(true);
		}
	}

	/**
	 * An own panel for the settings in Bio7 (null: the settings of the Param
	 * fields and Action methods are created). Create, e.g., a Composite designed
	 * with the WindowBuilder: {@code return new MySettings(parent, SWT.NONE);}.
	 * Its widgets are bound to the model by the names of its fields (a Scale
	 * "feed" to the Param field feed, a push Button "addSeed" to the Action
	 * method addSeed(), see {@link SimulationSettings#bind(Object, Simulation)}).
	 * The HTML export creates the controls of the Param fields and Action
	 * methods (this method is not exported).
	 *
	 * @param parent the parent of the panel (the "Settings" view)
	 * @return the panel or null
	 */
	public Composite settings(Composite parent) {
		return null;
	}

	/** Sets the seed of the random numbers (e.g., in init()). */
	public void seed(int seed) {
		random = new SimRandom(seed);
		seedValue = seed;
	}

	/**
	 * A random number in [0, 1) for an index (e.g., of a cell or an agent) in this
	 * time step: computed from the seed, the time step and the index (no common
	 * sequence), so it can be used in parallel threads (e.g.,
	 * IntStream.range(0, height).parallel().forEach(...)) and gives the same
	 * numbers in any order of the threads, in Java and in JavaScript.
	 */
	public double randomAt(int index) {
		return randomAt(index, 0);
	}

	/** A random number in [0, 1) for an index and a number k of the draw (several numbers for one index in a step). */
	public double randomAt(int index, int k) {
		int h = seedValue * 0x9E3779B1 ^ t * 0x85EBCA77 ^ index * 0xC2B2AE3D ^ k * 0x27D4EB2F;
		h ^= h >>> 16;
		h *= 0x7FEB352D;
		h ^= h >>> 15;
		h *= 0x846CA68B;
		h ^= h >>> 16;
		return (h & 0xFFFFFFFFL) / 4294967296.0;
	}

	/* ===================== The API ===================== */

	/** A random number in [0, 1) (the same numbers in Java and in the HTML export). */
	public double random() {
		return random.nextDouble();
	}

	/** A random integer in [0, n). */
	public int randomInt(int n) {
		return random.nextInt(n);
	}

	/* ===================== Random numbers ===================== */

	/** A random number in [min, max). */
	public double random(double min, double max) {
		return min + random.nextDouble() * (max - min);
	}

	/** A random integer in [min, max) (min included, max excluded). */
	public int randomInt(int min, int max) {
		return min + random.nextInt(max - min);
	}

	/** true with the probability p (e.g., chance(0.1): in 10 % of the calls). */
	public boolean chance(double p) {
		return random.nextDouble() < p;
	}

	/** A normally distributed random number (mean 0, standard deviation 1). */
	public double randomGaussian() {
		/* The polar method of Marsaglia (the same random numbers in Java and JavaScript) */
		double u, v, s;
		do {
			u = 2 * random.nextDouble() - 1;
			v = 2 * random.nextDouble() - 1;
			s = u * u + v * v;
		} while (s >= 1 || s == 0);
		return u * Math.sqrt(-2 * detLog(s) / s);
	}

	/** A normally distributed random number with the mean and the standard deviation. */
	public double randomGaussian(double mean, double sd) {
		return mean + sd * randomGaussian();
	}

	/** An exponentially distributed random number (e.g., a waiting time) with the rate. */
	public double randomExponential(double rate) {
		return -detLog(1 - random.nextDouble()) / rate;
	}

	/** A Poisson distributed random integer (e.g., the number of events) with the mean lambda. */
	public int randomPoisson(double lambda) {
		if (lambda > 30) {
			return (int) Math.max(0, Math.round(lambda + Math.sqrt(lambda) * randomGaussian()));
		}
		double limit = detExp(-lambda), p = 1;
		int k = 0;
		do {
			k++;
			p *= random.nextDouble();
		} while (p > limit);
		return k - 1;
	}

	/** A random element of the list (null if empty). */
	public <T> T randomElement(List<T> list) {
		return list.isEmpty() ? null : list.get(random.nextInt(list.size()));
	}

	/** Shuffles the list randomly (e.g., the agents for a random order of the updates). */
	public void shuffle(List<?> list) {
		for (int i = list.size() - 1; i > 0; i--) {
			int j = random.nextInt(i + 1);
			java.util.Collections.swap(list, i, j);
		}
	}

	/*
	 * The logarithm and the exponential function with series of the same
	 * operations in Java and JavaScript (the random distributions give the same
	 * numbers in Bio7 and in the browser; the error is a few units of the last
	 * digit).
	 */
	static double detLog(double x) {
		if (!(x > 0)) {
			return x == 0 ? Double.NEGATIVE_INFINITY : Double.NaN;
		}
		if (x == Double.POSITIVE_INFINITY) {
			return x;
		}
		int e = 0;
		double m = x;
		while (m >= 1.4142135623730951) {
			m *= 0.5;
			e++;
		}
		while (m < 0.7071067811865476) {
			m *= 2;
			e--;
		}
		double z = (m - 1) / (m + 1), z2 = z * z, t = 0;
		for (int k = 27; k >= 1; k -= 2) {
			t = t * z2 + 1.0 / k;
		}
		return 2 * z * t + e * 0.6931471805599453;
	}

	static double detExp(double x) {
		if (x != x) {
			return x;
		}
		if (x > 709.7) {
			return Double.POSITIVE_INFINITY;
		}
		if (x < -745.2) {
			return 0;
		}
		double k = Math.floor(x / 0.6931471805599453 + 0.5);
		/* ln 2 in two parts (Cody and Waite): k * LN2_HI is exact */
		double r = (x - k * 0.6931471803691238) - k * 1.9082149292705877e-10, t = 1;
		for (int n = 20; n >= 1; n--) {
			t = 1 + t * r / n;
		}
		return t * Math.pow(2, k);
	}

	/* ===================== Mathematics ===================== */

	/** The value limited to [min, max]. */
	public static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	/** The linear interpolation between a and b (t = 0: a, t = 1: b). */
	public static double lerp(double a, double b, double t) {
		return a + (b - a) * t;
	}

	/** The value of the range [inMin, inMax] in the range [outMin, outMax] (linear). */
	public static double mapRange(double value, double inMin, double inMax, double outMin, double outMax) {
		return outMin + (value - inMin) * (outMax - outMin) / (inMax - inMin);
	}

	/** The value on a torus of the size (e.g., wrapAround(-1, 100) = 99). */
	public static double wrapAround(double value, double size) {
		return ((value % size) + size) % size;
	}

	/** The square of the value. */
	public static double sq(double value) {
		return value * value;
	}

	/** The distance of two points in 2D. */
	public static double distance(double x1, double y1, double x2, double y2) {
		return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
	}

	/** The distance of two points in 3D. */
	public static double distance3(double x1, double y1, double z1, double x2, double y2, double z2) {
		return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1));
	}

	/** The logistic function 1 / (1 + e^-x) (e.g., a smooth switch from 0 to 1). */
	public static double logistic(double x) {
		return 1 / (1 + detExp(-x));
	}

	/** The logistic growth of a population per time unit: r * n * (1 - n / k). */
	public static double logisticGrowth(double n, double r, double k) {
		return r * n * (1 - n / k);
	}

	/* ===================== Statistics ===================== */

	/** The sum of the values. */
	public static double sum(double[] values) {
		double s = 0;
		for (int i = 0; i < values.length; i++) {
			s += values[i];
		}
		return s;
	}

	/** The sum of the values. */
	public static double sum(int[] values) {
		double s = 0;
		for (int i = 0; i < values.length; i++) {
			s += values[i];
		}
		return s;
	}

	/** The mean of the values (0 if empty). */
	public static double mean(double[] values) {
		return values.length == 0 ? 0 : sum(values) / values.length;
	}

	/** The mean of the values (0 if empty). */
	public static double mean(int[] values) {
		return values.length == 0 ? 0 : sum(values) / values.length;
	}

	/** The variance of the values (n - 1, 0 for less than two values). */
	public static double variance(double[] values) {
		int n = values.length;
		if (n < 2) {
			return 0;
		}
		double m = mean(values), s = 0;
		for (int i = 0; i < n; i++) {
			s += (values[i] - m) * (values[i] - m);
		}
		return s / (n - 1);
	}

	/** The standard deviation of the values. */
	public static double sd(double[] values) {
		return Math.sqrt(variance(values));
	}

	/** The minimum of the values (Infinity if empty). */
	public static double min(double[] values) {
		double m = Double.POSITIVE_INFINITY;
		for (int i = 0; i < values.length; i++) {
			m = Math.min(m, values[i]);
		}
		return m;
	}

	/** The maximum of the values (-Infinity if empty). */
	public static double max(double[] values) {
		double m = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < values.length; i++) {
			m = Math.max(m, values[i]);
		}
		return m;
	}

	/* ===================== Differential equations ===================== */

	/** The derivatives of a system of differential equations: dy/dt = f(t, y). */
	@FunctionalInterface
	public interface Derivatives {
		double[] apply(double t, double[] y);
	}

	/**
	 * One step of the Runge-Kutta method (4th order) for dy/dt = f(t, y), e.g., a
	 * Lotka-Volterra model:
	 *
	 * <pre>
	 * y = rk4((t, v) -&gt; new double[] { a * v[0] - b * v[0] * v[1], c * v[0] * v[1] - d * v[1] }, t, y, 0.01);
	 * </pre>
	 *
	 * @return the values at t + dt (a new array)
	 */
	public static double[] rk4(Derivatives f, double t, double[] y, double dt) {
		int n = y.length;
		double[] k1 = f.apply(t, y);
		double[] y2 = new double[n];
		for (int i = 0; i < n; i++) {
			y2[i] = y[i] + dt / 2 * k1[i];
		}
		double[] k2 = f.apply(t + dt / 2, y2);
		double[] y3 = new double[n];
		for (int i = 0; i < n; i++) {
			y3[i] = y[i] + dt / 2 * k2[i];
		}
		double[] k3 = f.apply(t + dt / 2, y3);
		double[] y4 = new double[n];
		for (int i = 0; i < n; i++) {
			y4[i] = y[i] + dt * k3[i];
		}
		double[] k4 = f.apply(t + dt, y4);
		double[] r = new double[n];
		for (int i = 0; i < n; i++) {
			r[i] = y[i] + dt / 6 * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]);
		}
		return r;
	}

	/* ===================== Mathematics (also in the HTML export) ===================== */

	/** The dot product of two vectors. */
	public static double dot(double[] a, double[] b) {
		double s = 0;
		for (int i = 0; i < Math.min(a.length, b.length); i++) {
			s += a[i] * b[i];
		}
		return s;
	}

	/** The length of a vector. */
	public static double norm(double[] a) {
		return Math.sqrt(dot(a, a));
	}

	/** The product of two matrices (rows of columns). */
	public static double[][] matMul(double[][] a, double[][] b) {
		int n = a.length, m = b[0].length, k = b.length;
		double[][] r = new double[n][m];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < m; j++) {
				double s = 0;
				for (int q = 0; q < k; q++) {
					s += a[i][q] * b[q][j];
				}
				r[i][j] = s;
			}
		}
		return r;
	}

	/** The product of a matrix and a vector (e.g., a Leslie matrix and the age classes). */
	public static double[] matVec(double[][] a, double[] v) {
		double[] r = new double[a.length];
		for (int i = 0; i < a.length; i++) {
			r[i] = dot(a[i], v);
		}
		return r;
	}

	/** The transposed matrix. */
	public static double[][] transpose(double[][] a) {
		double[][] r = new double[a[0].length][a.length];
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[0].length; j++) {
				r[j][i] = a[i][j];
			}
		}
		return r;
	}

	/** The solution x of A x = b (Gaussian elimination with partial pivoting; NaN if A is singular). */
	public static double[] solve(double[][] a, double[] b) {
		int n = b.length;
		double[][] m = new double[n][n + 1];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				m[i][j] = a[i][j];
			}
			m[i][n] = b[i];
		}
		for (int c = 0; c < n; c++) {
			int p = c;
			for (int r = c + 1; r < n; r++) {
				if (Math.abs(m[r][c]) > Math.abs(m[p][c])) {
					p = r;
				}
			}
			double[] tmp = m[c];
			m[c] = m[p];
			m[p] = tmp;
			if (m[c][c] == 0) {
				double[] nan = new double[n];
				java.util.Arrays.fill(nan, Double.NaN);
				return nan;
			}
			for (int r = c + 1; r < n; r++) {
				double f = m[r][c] / m[c][c];
				for (int j = c; j <= n; j++) {
					m[r][j] -= f * m[c][j];
				}
			}
		}
		double[] x = new double[n];
		for (int i = n - 1; i >= 0; i--) {
			double s = m[i][n];
			for (int j = i + 1; j < n; j++) {
				s -= m[i][j] * x[j];
			}
			x[i] = s / m[i][i];
		}
		return x;
	}

	/** The inverse matrix (NaN if the matrix is singular). */
	public static double[][] inverse(double[][] a) {
		int n = a.length;
		double[][] r = new double[n][n];
		for (int j = 0; j < n; j++) {
			double[] e = new double[n];
			e[j] = 1;
			double[] col = solve(a, e);
			for (int i = 0; i < n; i++) {
				r[i][j] = col[i];
			}
		}
		return r;
	}

	/** The determinant of a matrix. */
	public static double determinant(double[][] a) {
		int n = a.length;
		double[][] m = new double[n][];
		for (int i = 0; i < n; i++) {
			m[i] = a[i].clone();
		}
		double det = 1;
		for (int c = 0; c < n; c++) {
			int p = c;
			for (int r = c + 1; r < n; r++) {
				if (Math.abs(m[r][c]) > Math.abs(m[p][c])) {
					p = r;
				}
			}
			if (m[p][c] == 0) {
				return 0;
			}
			if (p != c) {
				double[] tmp = m[c];
				m[c] = m[p];
				m[p] = tmp;
				det = -det;
			}
			det *= m[c][c];
			for (int r = c + 1; r < n; r++) {
				double f = m[r][c] / m[c][c];
				for (int j = c; j < n; j++) {
					m[r][j] -= f * m[c][j];
				}
			}
		}
		return det;
	}

	/** The linear regression y = a + b x: the intercept a, the slope b and the coefficient of determination r². */
	public static double[] linearRegression(double[] x, double[] y) {
		int n = Math.min(x.length, y.length);
		double mx = 0, my = 0;
		for (int i = 0; i < n; i++) {
			mx += x[i];
			my += y[i];
		}
		mx /= n;
		my /= n;
		double sxy = 0, sxx = 0, syy = 0;
		for (int i = 0; i < n; i++) {
			sxy += (x[i] - mx) * (y[i] - my);
			sxx += (x[i] - mx) * (x[i] - mx);
			syy += (y[i] - my) * (y[i] - my);
		}
		double b = sxx == 0 ? 0 : sxy / sxx;
		return new double[] { my - b * mx, b, sxx == 0 || syy == 0 ? 0 : sxy * sxy / (sxx * syy) };
	}

	/** The correlation coefficient of Pearson. */
	public static double correlation(double[] x, double[] y) {
		double[] r = linearRegression(x, y);
		return r[1] < 0 ? -Math.sqrt(r[2]) : Math.sqrt(r[2]);
	}

	/** The quantile p (0 - 1) of the values (linear interpolation of the sorted values). */
	public static double quantile(double[] values, double p) {
		if (values.length == 0) {
			return Double.NaN;
		}
		double[] v = values.clone();
		java.util.Arrays.sort(v);
		double h = (v.length - 1) * Math.max(0, Math.min(1, p));
		int lo = (int) Math.floor(h);
		int hi = Math.min(v.length - 1, lo + 1);
		return v[lo] + (h - lo) * (v[hi] - v[lo]);
	}

	/** The median of the values. */
	public static double median(double[] values) {
		return quantile(values, 0.5);
	}

	/** The density of the normal distribution. */
	public static double normalPdf(double x, double mean, double sd) {
		double z = (x - mean) / sd;
		return detExp(-0.5 * z * z) / (sd * 2.5066282746310002);
	}

	/** The distribution function of the normal distribution (an accuracy of about 1e-7). */
	public static double normalCdf(double x, double mean, double sd) {
		double z = (x - mean) / (sd * 1.4142135623730951);
		double a = Math.abs(z), t = 1 / (1 + 0.5 * a);
		double erfc = t * detExp(-a * a - 1.26551223 + t * (1.00002368 + t * (0.37409196 + t * (0.09678418 + t * (-0.18628806 + t * (0.27886807 + t * (-1.13520398
				+ t * (1.48851587 + t * (-0.82215223 + t * 0.17087277)))))))));
		return z >= 0 ? 1 - 0.5 * erfc : 0.5 * erfc;
	}

	/* A hash of the corners of the noise */
	private static double corner(int x, int y) {
		int h = x * 374761393 + y * 668265263;
		h = (h ^ (h >>> 13)) * 1274126177;
		h = h ^ (h >>> 16);
		return (h & 0xffffff) / 16777216.0;
	}

	/** A smooth noise (0 - 1) at x, y (value noise: e.g., a landscape, the grass of a pasture; the same in the browser). */
	public static double noise(double x, double y) {
		int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
		double fx = x - ix, fy = y - iy;
		double sx = fx * fx * (3 - 2 * fx), sy = fy * fy * (3 - 2 * fy);
		double a = corner(ix, iy), b = corner(ix + 1, iy), c = corner(ix, iy + 1), d = corner(ix + 1, iy + 1);
		return a + (b - a) * sx + (c - a) * sy + (a - b - c + d) * sx * sy;
	}

	/** A fractal noise (0 - 1): the sum of octaves of noise (finer details with more octaves). */
	public static double fractalNoise(double x, double y, int octaves) {
		double sum = 0, amplitude = 1, total = 0, f = 1;
		for (int k = 0; k < octaves; k++) {
			sum += amplitude * noise(x * f + k * 17.3, y * f + k * 11.1);
			total += amplitude;
			amplitude *= 0.5;
			f *= 2;
		}
		return sum / total;
	}

	/** The cosine of an angle in degrees (the same digits in Bio7 and in the browser, e.g., for a movement in the heading). */
	public static double cosDeg(double deg) {
		return GeoAgentModel.cosDeg(deg);
	}

	/** The sine of an angle in degrees (the same digits in Bio7 and in the browser). */
	public static double sinDeg(double deg) {
		return GeoAgentModel.sinDeg(deg);
	}

	/** The heading of a movement (dx, dy) in degrees: 0 the direction of y, clockwise (as the angle of drawImage and the heading of the agents). */
	public static double headingOf(double dx, double dy) {
		double h = Math.toDegrees(Math.atan2(dx, dy));
		return h < 0 ? h + 360 : h;
	}

	/** A random number of successes of n trials with the probability p. */
	public int randomBinomial(int n, double p) {
		int k = 0;
		for (int i = 0; i < n; i++) {
			if (random() < p) {
				k++;
			}
		}
		return k;
	}

	/** A random index chosen with the weights (e.g., a cell with more grass is chosen more often), -1 if all are 0. */
	public int randomWeighted(double[] weights) {
		double total = 0;
		for (double w : weights) {
			total += Math.max(0, w);
		}
		if (total <= 0) {
			return -1;
		}
		double r = random() * total;
		for (int i = 0; i < weights.length; i++) {
			r -= Math.max(0, weights[i]);
			if (r < 0) {
				return i;
			}
		}
		return weights.length - 1;
	}

	/* ===================== Charts ===================== */

	/** A value of a series of the first chart (one value per step; the Charts view of Bio7 and the HTML page). */
	public void chart(String name, double value) {
		synchronized (charts) {
			charts.computeIfAbsent("", k -> new ChartData("", ChartData.LINE)).add(name, t, value);
		}
	}

	/**
	 * A value of a series of another chart with a title (a second, third, ...
	 * chart, one value per step).
	 */
	public void chart(String chart, String name, double value) {
		synchronized (charts) {
			charts.computeIfAbsent(chart, k -> new ChartData(chart, ChartData.LINE)).add(name, t, value);
		}
	}

	/** A point (x, y) of a series of an XY chart with a title (e.g., a phase plot of two populations). */
	public void chartXY(String chart, String name, double x, double y) {
		synchronized (charts) {
			charts.computeIfAbsent(chart, k -> new ChartData(chart, ChartData.XY)).add(name, x, y);
		}
	}

	/** A bar chart with a title: the labels and the values (replaced in each call, e.g., a histogram). */
	public void chartBars(String chart, String[] labels, double[] values) {
		synchronized (charts) {
			ChartData c = charts.computeIfAbsent(chart, k -> new ChartData(chart, ChartData.BARS));
			c.labels = labels.clone();
			c.values = values.clone();
		}
	}

	private Boolean usesCharts;

	/**
	 * true if the model draws charts (calls chart, chartXY or chartBars): the
	 * Charts view is shown at the start (not only after the first values).
	 */
	boolean usesCharts() {
		if (usesCharts == null) {
			usesCharts = callsCharts(getClass());
		}
		return usesCharts;
	}

	/* The class (or a nested class) calls a chart method: the names in its class files, else in its source */
	private static boolean callsCharts(Class<?> c) {
		java.util.function.Predicate<String> charts = t -> t.contains("chart") && java.util.regex.Pattern.compile("\\bchart(XY|Bars)?\\b").matcher(t).find();
		try {
			ClassLoader loader = c.getClassLoader();
			java.util.List<Class<?>> classes = new ArrayList<>(java.util.List.of(c));
			classes.addAll(java.util.List.of(c.getDeclaredClasses()));
			boolean read = false;
			for (Class<?> k : classes) {
				try (java.io.InputStream in = loader == null ? null : loader.getResourceAsStream(k.getName().replace('.', '/') + ".class")) {
					if (in != null) {
						read = true;
						if (charts.test(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1))) {
							return true;
						}
					}
				}
			}
			if (read) {
				return false;
			}
		} catch (java.io.IOException | RuntimeException e) {
			/* The source */
		}
		try {
			String suffix = "/" + c.getName().replace('.', '/') + ".java";
			List<org.eclipse.core.resources.IFile> found = new ArrayList<>();
			org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().accept(r -> {
				if (r instanceof org.eclipse.core.resources.IFile f && r.getFullPath().toString().endsWith(suffix)) {
					found.add(f);
				}
				return r.getType() != org.eclipse.core.resources.IResource.FILE && (r.getType() != org.eclipse.core.resources.IResource.PROJECT || r.getProject().isOpen());
			});
			for (org.eclipse.core.resources.IFile f : found) {
				try (java.io.InputStream in = f.getContents()) {
					if (charts.test(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8))) {
						return true;
					}
				}
			}
		} catch (Exception e) {
			/* No workspace */
		}
		return false;
	}

	/** The values of the series of the first chart (name: [step, value] pairs). */
	public Map<String, List<double[]>> getSeries() {
		synchronized (charts) {
			ChartData c = charts.get("");
			return c == null ? new LinkedHashMap<>() : c.copy().series;
		}
	}

	/** Copies of all charts (the first chart has the title ""). */
	public List<ChartData> getCharts() {
		synchronized (charts) {
			List<ChartData> list = new ArrayList<>();
			for (ChartData c : charts.values()) {
				list.add(c.copy());
			}
			return list;
		}
	}

	/* ===================== Drawing ===================== */

	/*
	 * The shapes are drawn in the coordinates of the model: the cells of a grid (x
	 * to the right, y down, fractions between the cells), the box of an agent
	 * model (2D shapes on the floor or at a height z, the 3D shapes in the box) or
	 * longitude and latitude of a geospatial agent model (the radius of a circle
	 * in km, the size of a text or an image in pixels). A color null or "" is no
	 * line (or no fill). The shapes are drawn again in each step (cleared before
	 * step()).
	 */

	void clearDrawing() {
		synchronized (drawing) {
			drawing.clear();
		}
	}

	void addShape(Shape s) {
		/* 3D shapes in a 2D view (a grid): not drawn, a message once */
		if (!threeD() && !warned3d && THREE_D_SHAPES.contains(s.kind)) {
			warned3d = true;
			System.err.println("The model " + getClass().getSimpleName() + " draws a 3D shape (" + s.kind
					+ "): 3D shapes are drawn only in the 3D view of an agent model (the 2D view of a grid ignores them)");
		}
		synchronized (drawing) {
			drawing.add(s);
		}
	}

	/* The shapes which only the 3D view draws */
	private static final java.util.Set<String> THREE_D_SHAPES = java.util.Set.of("box", "sphere", "point3", "path3", "polygon3", "pillar");
	private boolean warned3d;

	/* True for a model of the 3D view (an agent model) or of the globe (it decides itself which shapes it shows) */
	boolean threeD() {
		return this instanceof AgentModel || this instanceof GeoAgentModel;
	}

	/** A line (the width in pixels). */
	public void drawLine(double x1, double y1, double x2, double y2, String color, double width) {
		drawLine(x1, y1, x2, y2, color, width, 0);
	}

	/** A line at the height z above the floor (an agent model). */
	public void drawLine(double x1, double y1, double x2, double y2, String color, double width, double z) {
		addShape(new Shape("line", new double[] { x1, y1, x2, y2 }, color, null, null, width, 0, z));
	}

	/** A line in 3D (an agent model: x, y, z of the box). */
	public void drawLine3(double x1, double y1, double z1, double x2, double y2, double z2, String color, double width) {
		addShape(new Shape("line3", new double[] { x1, y1, z1, x2, y2, z2 }, color, null, null, width, 0));
	}

	/**
	 * A pillar in 3D (an agent model: a vertical cylinder from the floor at x, y of
	 * the box with the radius and the height, e.g., an obstacle; color: e.g.,
	 * "#9aa6b8").
	 */
	public void drawPillar(double x, double y, double radius, double height, String color) {
		addShape(new Shape("pillar", new double[] { x, y, radius, height }, null, color, null, 1, 0));
	}

	/** A line through the points (e.g., the track of an agent). */
	public void drawPath(double[] xs, double[] ys, String color, double width) {
		drawPath(xs, ys, color, width, 0);
	}

	/** A line through the points at the height z above the floor (an agent model). */
	public void drawPath(double[] xs, double[] ys, String color, double width, double z) {
		addShape(new Shape("path", interleave(xs, ys), color, null, null, width, 0, z));
	}

	/** A polygon (the fill and the color of the line, null: none; e.g., a pasture or a territory). */
	public void drawPolygon(double[] xs, double[] ys, String fill, String stroke) {
		drawPolygon(xs, ys, fill, stroke, 0);
	}

	/** A polygon at the height z above the floor (an agent model). */
	public void drawPolygon(double[] xs, double[] ys, String fill, String stroke, double z) {
		addShape(new Shape("polygon", interleave(xs, ys), stroke, fill, null, 1, 0, z));
	}

	/** A circle (a geospatial agent model: the radius in km). */
	public void drawCircle(double x, double y, double radius, String fill, String stroke) {
		drawCircle(x, y, radius, fill, stroke, 0);
	}

	/** A circle at the height z above the floor (an agent model). */
	public void drawCircle(double x, double y, double radius, String fill, String stroke, double z) {
		addShape(new Shape("circle", new double[] { x, y, radius }, stroke, fill, null, 1, 0, z));
	}

	/** A rectangle (x, y: the upper left corner of a grid). */
	public void drawRect(double x, double y, double w, double h, String fill, String stroke) {
		drawRect(x, y, w, h, fill, stroke, 0);
	}

	/** A rectangle at the height z above the floor (an agent model). */
	public void drawRect(double x, double y, double w, double h, String fill, String stroke, double z) {
		addShape(new Shape("rect", new double[] { x, y, w, h }, stroke, fill, null, 1, 0, z));
	}

	/** A text (the size in cells of a grid, in pixels on a map or the globe). */
	public void drawText(double x, double y, String text, String color, double size) {
		drawText(x, y, text, color, size, 0);
	}

	/** A text at the height z above the floor (an agent model, e.g., a label above an object). */
	public void drawText(double x, double y, String text, String color, double size, double z) {
		addShape(new Shape("text", new double[] { x, y }, color, null, text, size, 0, z));
	}

	/**
	 * An image (loaded with loadImage) centered at x, y: the size (the larger
	 * side, in cells of a grid, in pixels on a map or the globe) and the angle in
	 * degrees (clockwise, e.g., the heading of an animal).
	 */
	public void drawImage(String name, double x, double y, double size, double angle) {
		drawImage(name, x, y, size, angle, 0);
	}

	/** An image at the height z above the floor (an agent model: the lower edge of the image, standing). */
	public void drawImage(String name, double x, double y, double size, double angle, double z) {
		addShape(new Shape("image", new double[] { x, y }, null, null, name, size, angle, z));
	}

	/* The 3D shapes of an agent model (x, y, z of the box; lit solids) */

	/** A point in 3D (an agent model): a small sphere with the diameter size, e.g., a marker. */
	public void drawPoint3(double x, double y, double z, double size, String color) {
		addShape(new Shape("point3", new double[] { x, y, z, size }, null, color, null, 1, 0));
	}

	/** A line through the points in 3D (an agent model; the width in pixels), e.g., a flight path. */
	public void drawPath3(double[] xs, double[] ys, double[] zs, String color, double width) {
		addShape(new Shape("path3", interleave3(xs, ys, zs), color, null, null, width, 0));
	}

	/**
	 * A box in 3D (an agent model): the corner x, y, z (the lowest values), the
	 * width (x), the height (y) and the depth (z, upwards) as in box(...); fill:
	 * the color of the sides, stroke: the color of the edges (null: none), e.g., a
	 * house or a tower.
	 */
	public void drawBox(double x, double y, double z, double w, double h, double d, String fill, String stroke) {
		addShape(new Shape("box", new double[] { x, y, z, w, h, d }, stroke, fill, null, 1, 0));
	}

	/** A sphere in 3D (an agent model): the center and the radius, e.g., a balloon or a tree crown. */
	public void drawSphere(double x, double y, double z, double radius, String fill) {
		addShape(new Shape("sphere", new double[] { x, y, z, radius }, null, fill, null, 1, 0));
	}

	/**
	 * A flat polygon in 3D (an agent model; the points should lie in a plane): the
	 * fill and the color of the outline (null: none), e.g., a roof or a ramp.
	 */
	public void drawPolygon3(double[] xs, double[] ys, double[] zs, String fill, String stroke) {
		addShape(new Shape("polygon3", interleave3(xs, ys, zs), stroke, fill, null, 1, 0));
	}

	/** The shapes of the current step (a copy). */
	public List<Shape> getDrawing() {
		synchronized (drawing) {
			return new ArrayList<>(drawing);
		}
	}

	private static double[] interleave(double[] xs, double[] ys) {
		int n = Math.min(xs.length, ys.length);
		double[] p = new double[n * 2];
		for (int i = 0; i < n; i++) {
			p[i * 2] = xs[i];
			p[i * 2 + 1] = ys[i];
		}
		return p;
	}

	private static double[] interleave3(double[] xs, double[] ys, double[] zs) {
		int n = Math.min(xs.length, Math.min(ys.length, zs.length));
		double[] p = new double[n * 3];
		for (int i = 0; i < n; i++) {
			p[i * 3] = xs[i];
			p[i * 3 + 1] = ys[i];
			p[i * 3 + 2] = zs[i];
		}
		return p;
	}

	/* ===================== Images and 3D models ===================== */

	/**
	 * Loads an image (PNG with transparency, JPEG) for drawImage and the agents
	 * (agent.image = name). A relative path is searched in the project of the
	 * model; the HTML export embeds the image.
	 */
	public void loadImage(String name, String file) {
		imageFiles.put(name, resolve(file));
	}

	/**
	 * Loads a 3D model (glTF: .glb or .gltf, also animated; Wavefront OBJ with
	 * the materials and textures of its .mtl file, y up; STL, binary or ASCII, z
	 * up) for the agents (agent.model = name, agent.animation = the name of an
	 * animation) and placeModel. A relative path is searched in the project of
	 * the model; the HTML export embeds the model (OBJ and STL converted to
	 * glTF).
	 */
	public void loadModel(String name, String file) {
		modelFiles.put(name, resolve(file));
	}

	/** The images loaded with loadImage (the name and the file). */
	public Map<String, String> getImageFiles() {
		return new LinkedHashMap<>(imageFiles);
	}

	/** The 3D models loaded with loadModel (the name and the file). */
	public Map<String, String> getModelFiles() {
		return new LinkedHashMap<>(modelFiles);
	}

	/* A path relative to the project of the model (else the working folder) */
	String resolve(String file) {
		java.io.File f = new java.io.File(file);
		if (f.isAbsolute()) {
			return f.getPath();
		}
		if (hasDisplay()) {
			try {
				String suffix = "/" + getClass().getName().replace('.', '/') + ".java";
				List<org.eclipse.core.resources.IResource> found = new ArrayList<>();
				org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().accept(r -> {
					if (r.getType() == org.eclipse.core.resources.IResource.FILE && r.getFullPath().toString().endsWith(suffix)) {
						found.add(r);
					}
					return r.getType() != org.eclipse.core.resources.IResource.FILE && (r.getType() != org.eclipse.core.resources.IResource.PROJECT || r.getProject().isOpen());
				});
				for (org.eclipse.core.resources.IResource r : found) {
					java.io.File candidate = new java.io.File(r.getProject().getLocation().toFile(), file);
					if (candidate.exists()) {
						return candidate.getPath();
					}
				}
			} catch (Exception e) {
				/* The workspace is not available: the working folder */
			}
		}
		String base = System.getProperty("bio7.sim.data");
		return base != null ? new java.io.File(base, file).getPath() : f.getAbsolutePath();
	}

	/** Parses a color like #1f77b4 (RGB as an int). */
	static int rgb(String color) {
		if (color == null || (color.length() != 7 && color.length() != 9) || color.charAt(0) != '#') {
			return 0x808080;
		}
		try {
			/* #rrggbb or #rrggbbaa (the alpha is used in the HTML export, e.g., over a map) */
			return Integer.parseInt(color.substring(1, 7), 16);
		} catch (NumberFormatException e) {
			return 0x808080;
		}
	}

	/* The thread of the repeated displays (a display which was skipped while the last one was shown) */
	private static final java.util.concurrent.ExecutorService REDISPLAY = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "Bio7 simulation display");
		t.setDaemon(true);
		return t;
	});

	/*
	 * Displays the model again (e.g., the image of the last step): not in the
	 * thread of the user interface, which would be busy without a break at a high
	 * speed (the image of the next step is built while it is shown)
	 */
	static void redisplay(Runnable display) {
		REDISPLAY.execute(() -> {
			try {
				display.run();
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		});
	}

	/** Runs the code in the thread of the user interface (asynchronously, now if already in it). */
	static void ui(Runnable code) {
		ui(code, false);
	}

	private static void ui(Runnable code, boolean later) {
		if (!hasDisplay()) {
			return;
		}
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (display.isDisposed()) {
			return;
		}
		if (!later && Display.getCurrent() == display) {
			code.run();
		} else {
			display.asyncExec(code);
		}
	}

	/** true if Bio7 runs with its user interface (false: e.g., a test without the workbench). */
	static boolean hasDisplay() {
		try {
			return PlatformUI.isWorkbenchRunning();
		} catch (Throwable e) {
			return false;
		}
	}
}
