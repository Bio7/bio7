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
package com.eco.bio7.wizards.java;

import com.eco.bio7.wizards.java.SimulationModelWizardPage.Kind;

/**
 * The templates of the simulation wizard: a small working model of the
 * simulation API (a grid with states, a grid with values or agents) with a
 * comment which explains the API, and an optional settings panel for the
 * WindowBuilder.
 */
public final class SimulationTemplates {

	private SimulationTemplates() {
	}

	/** The source of the model class. */
	public static String model(String name, Kind kind, boolean panel) {
		return model(name, kind, panel, false);
	}

	/**
	 * The source of the model class.
	 *
	 * @param parallel true: step() computes the cells (rows) or agents in parallel
	 *                 threads of Java (IntStream.range(...).parallel()) with the
	 *                 random numbers of randomAt (the same results in any order of
	 *                 the threads and in the browser)
	 */
	public static String model(String name, Kind kind, boolean panel, boolean parallel) {
		StringBuilder b = new StringBuilder();
		b.append(header(name, kind, panel, parallel));
		String code = switch (kind) {
		case GRID -> grid(name, panel);
		case VALUES -> values(name, panel);
		case AGENTS -> agents(name, panel);
		};
		if (parallel) {
			code = parallel(code, kind);
		}
		b.append(code);
		return b.toString();
	}

	/* The parallel version of step() (and the import of IntStream) */
	private static String parallel(String code, Kind kind) {
		String imports = "import java.util.stream.IntStream;\n\n";
		int at = code.indexOf("import com.eco.bio7.simulation.");
		code = code.substring(0, at) + imports + code.substring(at);
		String from, to;
		switch (kind) {
		case GRID -> {
			from = """
							for (int y = 0; y < height; y++) {
								for (int x = 0; x < width; x++) {
									/* The black cells of the 3 x 3 neighborhood (the 8 neighbors and the cell) */
									int black = countNeighbors(x, y, 1) + get(x, y);
									int state = black > 4 ? 1 : 0;
									if (random() < noise) {
										state = randomInt(2);
									}
									setNext(x, y, state);
								}
							}
					""";
			to = """
							/*
							 * The rows in parallel threads: each cell reads the cells and writes only its
							 * own next state. randomAt(index, k) gives the same random numbers in any
							 * order of the threads (random() is a sequence and must not be used here).
							 */
							IntStream.range(0, height).parallel().forEach(y -> {
								for (int x = 0; x < width; x++) {
									int i = y * width + x;
									int black = countNeighbors(x, y, 1) + get(x, y);
									int state = black > 4 ? 1 : 0;
									if (randomAt(i, 0) < noise) {
										state = randomAt(i, 1) < 0.5 ? 1 : 0;
									}
									setNext(x, y, state);
								}
							});
					""";
		}
		case VALUES -> {
			from = """
							for (int y = 0; y < height; y++) {
								for (int x = 0; x < width; x++) {
					""";
			to = """
							/* The rows in parallel threads (each cell writes only its own next value) */
							IntStream.range(0, height).parallel().forEach(y -> {
								for (int x = 0; x < width; x++) {
					""";
			code = code.replace("""
									next[i] = source[i] == 1 ? 1 : v + diffusion * (sum4(x, y) - 4 * v) - decay * v;
								}
							}
					""", """
									next[i] = source[i] == 1 ? 1 : v + diffusion * (sum4(x, y) - 4 * v) - decay * v;
								}
							});
					""");
		}
		default -> {
			from = """
							for (Walker w : agents) {
								/* A random turn and the attraction to the center */
								w.vx += (random() - 0.5) * 0.3 + attraction * (boxWidth / 2 - w.x);
								w.vy += (random() - 0.5) * 0.3 + attraction * (boxHeight / 2 - w.y);
								w.vz += (random() - 0.5) * 0.3 + attraction * (boxDepth / 2 - w.z);
					""";
			to = """
							/*
							 * The walkers in parallel threads: each walker changes only its own fields.
							 * randomAt(index, k) gives the same random numbers in any order of the threads.
							 */
							IntStream.range(0, agents.size()).parallel().forEach(i -> {
								Walker w = agents.get(i);
								w.vx += (randomAt(i, 0) - 0.5) * 0.3 + attraction * (boxWidth / 2 - w.x);
								w.vy += (randomAt(i, 1) - 0.5) * 0.3 + attraction * (boxHeight / 2 - w.y);
								w.vz += (randomAt(i, 2) - 0.5) * 0.3 + attraction * (boxDepth / 2 - w.z);
					""";
			code = code.replace("""
									w.z = Math.max(0, Math.min(boxDepth, w.z));
								}
							}
					""", """
									w.z = Math.max(0, Math.min(boxDepth, w.z));
								}
							});
					""");
		}
		}
		if (!code.contains(from)) {
			throw new IllegalStateException("The template has changed: " + kind);
		}
		return code.replace(from, to);
	}

	private static String header(String name, Kind kind, boolean panel, boolean parallel) {
		String display = kind == Kind.AGENTS ? "the 3D view" : "an ImageJ image (the ImageJ-Canvas view)";
		String api = switch (kind) {
		case GRID -> """
				 * GridModel: a grid of cells with integer states
				 * - size(w, h) creates the grid, width and height are its size.
				 * - cells: the states (index y * width + x), get(x, y) and set(x, y, state).
				 * - setNext(x, y, state) sets the next state, swap() makes the next states the
				 *   current ones (all cells change at the same time).
				 * - countNeighbors(x, y, state): the number of the 8 neighbors with the state,
				 *   count(state): the number of the cells with the state.
				 * - palette("#ffffff", "#000000", ...): the colors of the states 0, 1, ...
				 * - wrap = true: the grid is a torus (false: the cells outside are 0).
				""";
		case VALUES -> """
				 * ValueGridModel: a grid of cells with double values
				 * - size(w, h) creates the grid, width and height are its size.
				 * - values: the values (index y * width + x), get(x, y) and set(x, y, value).
				 * - setNext(x, y, value) sets the next value, swap() makes the next values the
				 *   current ones (all cells change at the same time).
				 * - sum4(x, y), sum8(x, y): the sum of the 4 or 8 neighbors (e.g., a
				 *   diffusion: sum4(x, y) - 4 * get(x, y)), mean(): the mean of all values.
				 * - range(min, max) and colorMap("#000000", "#ffffff", ...): the colors.
				 * - wrap = true: the grid is a torus (false: the cells outside are 0).
				""";
		case AGENTS -> """
				 * AgentModel: agents in a 3D box (displayed as spheres in the 3D view)
				 * - box(w, h, d) sets the box (x from 0 to boxWidth, y to boxHeight, z to
				 *   boxDepth, z is the height).
				 * - agents: an ArrayList of the agents. An agent is an object of a static
				 *   nested class which extends Agent (x, y, z, color, e.g., "#d62728") and has
				 *   its own fields (e.g., a velocity) and methods.
				 * - agentRadius and agentColor: the size and the color of the spheres.
				 * - The HTML page shows the agents with a WebGL view (spheres, cubes, cones or
				 *   gliders in the direction of the movement, shadows, backgrounds).
				""";
		};
		String settings = panel ? """
				 * - The settings in Bio7 are the panel %1$sSettings (open it with the
				 *   WindowBuilder editor): its widgets are bound to the Param fields and the
				 *   Action methods by their names (e.g., a Scale "noise" to the field noise, a
				 *   Label "noiseValue" shows the value, a Button "setup" calls the setup).
				""".formatted(name) : "";
		String threads = parallel ? """
				 * Parallel computation
				 * - step() computes the rows (or the agents) in parallel threads of Java:
				 *   IntStream.range(0, n).parallel().forEach(i -> { ... }). Each thread may only
				 *   write its own cells (setNext) or its own agent; reading is free.
				 * - Use randomAt(index, k) for the random numbers in the threads: it is
				 *   computed from the seed, the time step, the index and k (no common sequence),
				 *   so the results are the same in any order of the threads. random() must not be
				 *   used in the threads. chart(...) and count(...) after the parallel part.
				 * - The browser runs the same code as a normal loop (the same results).
				 *
				""" : "";
		return """
				/*
				 * %1$s: a simulation model of the simulation API of Bio7 (com.eco.bio7.simulation).
				 *
				 * How the model runs
				 * - init() creates the start state, step() calculates one time step.
				 * - Compile the class ("Compile Java" of the editor): the model is created and
				 *   displayed in %2$s with a "Settings" view.
				 * - The Play/Pause button of the toolbar calls step() repeatedly (the menu of the
				 *   button sets the time interval). The Setup button calls init() again.
				 *
				 * Settings and actions
				 * - A field with @Param is a setting: a slider (min and max), a number field, a
				 *   checkbox (boolean), a choice (a String with options) or a text field (String).
				 *   restart = true calls init() after a change, group = "..." shows a heading.
				 * - A public method without parameters with @Action is a button.
				%3$s *
				 * The API
				%4$s * - random() and randomInt(n): random numbers (the same numbers in Bio7 and in
				 *   the web browser), seed(n) sets the seed (in init()).
				 * - chart("Name", value): a value of a chart series (one value per step, shown
				 *   as a chart in the HTML page). t is the number of the time step.
				 *
				%5$s * Export as an HTML page
				 * - "Export Simulation as HTML..." (the menu of the Play/Pause button or the
				 *   context menu of this editor) translates the model to JavaScript: the page
				 *   runs the same model in the web browser (with the settings and a chart).
				 * - The translation supports a subset of Java: fields, methods (no overloading),
				 *   static nested classes, the statements (no try/catch), arrays, Math, lambdas,
				 *   ArrayList, HashMap and String methods. Not supported: java.util.Random (use
				 *   random()), Math.random(), anonymous classes, enums and other libraries. The
				 *   errors are marked in the editor.
				 *
				 * More examples: Bio7_Java_Examples/src/simulation (Game of Life, forest fire,
				 * epidemic, Langton's ant, heat diffusion, reaction-diffusion, swarm and
				 * predator-prey).
				 */
				""".formatted(name, display, settings, api, threads);
	}

	private static String settingsMethod(String name, boolean panel) {
		if (!panel) {
			return "";
		}
		return """

					/* The settings in Bio7: the panel of the WindowBuilder (the HTML page creates the controls of the annotations) */
					@Override
					public Composite settings(Composite parent) {
						return new %1$sSettings(parent, SWT.NONE);
					}
				""".formatted(name);
	}

	private static String swtImports(boolean panel) {
		return panel ? "import org.eclipse.swt.SWT;\nimport org.eclipse.swt.widgets.Composite;\n\n" : "";
	}

	private static String grid(String name, boolean panel) {
		return swtImports(panel) + """
				import com.eco.bio7.simulation.Action;
				import com.eco.bio7.simulation.GridModel;
				import com.eco.bio7.simulation.Param;

				/**
				 * A majority rule: a cell takes the state of the majority of its neighborhood
				 * (with some noise). Black and white regions form and grow.
				 */
				public class %1$s extends GridModel {

					@Param(label = "Grid size (cells)", min = 20, max = 300, restart = true, group = "World")
					int gridSize = 100;

					@Param(label = "Black cells at the start", min = 0, max = 1, step = 0.01, restart = true, group = "World")
					double share = 0.5;

					@Param(label = "Noise (a random state)", min = 0, max = 0.1, step = 0.001, group = "Rule")
					double noise = 0.002;

					public void init() {
						size(gridSize, gridSize);
						palette("#ffffff", "#1d2433");
						for (int i = 0; i < cells.length; i++) {
							cells[i] = random() < share ? 1 : 0;
						}
					}

					public void step() {
						for (int y = 0; y < height; y++) {
							for (int x = 0; x < width; x++) {
								/* The black cells of the 3 x 3 neighborhood (the 8 neighbors and the cell) */
								int black = countNeighbors(x, y, 1) + get(x, y);
								int state = black > 4 ? 1 : 0;
								if (random() < noise) {
									state = randomInt(2);
								}
								setNext(x, y, state);
							}
						}
						swap();
						chart("Black cells", count(1));
					}

					@Action(label = "Add a black square")
					public void addSquare() {
						int cx = randomInt(width), cy = randomInt(height);
						for (int y = -5; y <= 5; y++) {
							for (int x = -5; x <= 5; x++) {
								set((cx + x + width) %% width, (cy + y + height) %% height, 1);
							}
						}
					}
				%2$s}
				""".formatted(name, settingsMethod(name, panel));
	}

	private static String values(String name, boolean panel) {
		return swtImports(panel) + """
				import com.eco.bio7.simulation.Action;
				import com.eco.bio7.simulation.Param;
				import com.eco.bio7.simulation.ValueGridModel;

				/**
				 * A diffusion: sources keep a value of 1, the value spreads to the neighbors and
				 * decays.
				 */
				public class %1$s extends ValueGridModel {

					@Param(label = "Grid size (cells)", min = 20, max = 250, restart = true, group = "World")
					int gridSize = 100;

					@Param(label = "Diffusion", min = 0, max = 0.25, step = 0.005, group = "Process")
					double diffusion = 0.2;

					@Param(label = "Decay", min = 0, max = 0.02, step = 0.0005, group = "Process")
					double decay = 0.002;

					/* The sources (1: a source) */
					int[] source;

					public void init() {
						size(gridSize, gridSize);
						wrap = false;
						range(0, 1);
						source = new int[width * height];
						addSource();
					}

					public void step() {
						for (int y = 0; y < height; y++) {
							for (int x = 0; x < width; x++) {
								int i = y * width + x;
								double v = values[i];
								/* The diffusion to the 4 neighbors and the decay */
								next[i] = source[i] == 1 ? 1 : v + diffusion * (sum4(x, y) - 4 * v) - decay * v;
							}
						}
						swap();
						chart("Mean value", mean());
					}

					@Action(label = "Add a source")
					public void addSource() {
						int x = 2 + randomInt(width - 4), y = 2 + randomInt(height - 4);
						source[y * width + x] = 1;
						values[y * width + x] = 1;
					}
				%2$s}
				""".formatted(name, settingsMethod(name, panel));
	}

	private static String agents(String name, boolean panel) {
		return swtImports(panel) + """
				import com.eco.bio7.simulation.Action;
				import com.eco.bio7.simulation.Agent;
				import com.eco.bio7.simulation.AgentModel;
				import com.eco.bio7.simulation.Param;

				/**
				 * Walkers in a box: each walker changes its direction randomly, is attracted to
				 * the center of the box and turns back at the walls.
				 */
				public class %1$s extends AgentModel<%1$s.Walker> {

					@Param(label = "Number of walkers", min = 10, max = 500, restart = true, group = "Start")
					int walkers = 100;

					@Param(label = "Speed", min = 0.1, max = 3, step = 0.1, group = "Movement")
					double speed = 1;

					@Param(label = "Attraction to the center", min = 0, max = 0.05, step = 0.001, group = "Movement")
					double attraction = 0.005;

					/** A walker: the position (Agent) and the velocity. */
					static class Walker extends Agent {
						double vx, vy, vz;

						Walker(double x, double y, double z) {
							super(x, y, z);
						}
					}

					public void init() {
						box(100, 100, 60);
						agentRadius = 1.2;
						addWalkers(walkers);
					}

					void addWalkers(int n) {
						for (int i = 0; i < n; i++) {
							Walker w = new Walker(random() * boxWidth, random() * boxHeight, random() * boxDepth);
							w.vx = random() - 0.5;
							w.vy = random() - 0.5;
							w.vz = random() - 0.5;
							agents.add(w);
						}
					}

					public void step() {
						for (Walker w : agents) {
							/* A random turn and the attraction to the center */
							w.vx += (random() - 0.5) * 0.3 + attraction * (boxWidth / 2 - w.x);
							w.vy += (random() - 0.5) * 0.3 + attraction * (boxHeight / 2 - w.y);
							w.vz += (random() - 0.5) * 0.3 + attraction * (boxDepth / 2 - w.z);
							double len = Math.sqrt(w.vx * w.vx + w.vy * w.vy + w.vz * w.vz);
							if (len > 0) {
								w.vx = w.vx / len * speed;
								w.vy = w.vy / len * speed;
								w.vz = w.vz / len * speed;
							}
							/* The walls: the walker turns back */
							w.x += w.vx;
							w.y += w.vy;
							w.z += w.vz;
							if (w.x < 0 || w.x > boxWidth) {
								w.vx = -w.vx;
								w.x = Math.max(0, Math.min(boxWidth, w.x));
							}
							if (w.y < 0 || w.y > boxHeight) {
								w.vy = -w.vy;
								w.y = Math.max(0, Math.min(boxHeight, w.y));
							}
							if (w.z < 0 || w.z > boxDepth) {
								w.vz = -w.vz;
								w.z = Math.max(0, Math.min(boxDepth, w.z));
							}
						}
						double height = 0;
						for (Walker w : agents) {
							height += w.z;
						}
						chart("Mean height", height / agents.size());
					}

					@Action(label = "Add 10 walkers")
					public void moreWalkers() {
						addWalkers(10);
					}
				%2$s}
				""".formatted(name, settingsMethod(name, panel));
	}

	/** The source of the settings panel (a Composite for the WindowBuilder). */
	public static String panel(String name, Kind kind) {
		String param = switch (kind) {
		case GRID -> "noise";
		case VALUES -> "diffusion";
		case AGENTS -> "speed";
		};
		String label = switch (kind) {
		case GRID -> "Noise (a random state)";
		case VALUES -> "Diffusion";
		case AGENTS -> "Speed";
		};
		String action = switch (kind) {
		case GRID -> "addSquare";
		case VALUES -> "addSource";
		case AGENTS -> "moreWalkers";
		};
		String actionLabel = switch (kind) {
		case GRID -> "Add a black square";
		case VALUES -> "Add a source";
		case AGENTS -> "Add 10 walkers";
		};
		return """
				import org.eclipse.swt.SWT;
				import org.eclipse.swt.layout.GridData;
				import org.eclipse.swt.layout.GridLayout;
				import org.eclipse.swt.widgets.Button;
				import org.eclipse.swt.widgets.Composite;
				import org.eclipse.swt.widgets.Label;
				import org.eclipse.swt.widgets.Scale;

				/**
				 * The settings panel of %1$s in Bio7 (open it with the WindowBuilder editor
				 * and add widgets). The widgets are bound to the model by the names of the
				 * fields (see SimulationSettings.bind): a Scale, Slider, Spinner, Text, check
				 * Button or Combo with the name of a Param field, a Label with the name and
				 * "Value" shows the value, a push Button with the name of an Action method calls
				 * it, the Buttons "setup" and "export" call the setup and the HTML export.
				 * Change the names of the variables in the properties of the WindowBuilder
				 * ("Variable") and convert them to fields ("Convert local to field").
				 */
				public class %1$sSettings extends Composite {

					private Scale %2$s;
					private Label %2$sValue;
					private Button %4$s;
					private Button setup;
					private Button export;

					public %1$sSettings(Composite parent, int style) {
						super(parent, style);
						setLayout(new GridLayout(2, false));

						Label lbl = new Label(this, SWT.NONE);
						lbl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
						lbl.setText("%3$s");

						%2$s = new Scale(this, SWT.NONE);
						%2$s.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

						%2$sValue = new Label(this, SWT.RIGHT);
						GridData gdValue = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
						gdValue.widthHint = 50;
						%2$sValue.setLayoutData(gdValue);

						%4$s = new Button(this, SWT.NONE);
						%4$s.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
						%4$s.setText("%5$s");

						setup = new Button(this, SWT.NONE);
						setup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
						setup.setText("Setup");

						export = new Button(this, SWT.NONE);
						export.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
						export.setText("Export HTML...");
					}

					@Override
					protected void checkSubclass() {
						/* Allows the subclass of a SWT component */
					}
				}
				""".formatted(name, param, label, action, actionLabel);
	}
}
