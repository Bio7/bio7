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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * The dialog "Experiment" of the settings of a model: runs the model several
 * times without a display (a sweep of the parameters, repetitions, steps) and
 * saves the table as a CSV file in the project of the model (and as a data
 * frame in R). Java models run in parallel threads ({@link Experiment}), Python
 * and R models in their interpreter (experiment() of bio7sim).
 */
final class ExperimentDialog {

	private final Simulation model;
	private final ScriptSimulation.Script script;
	private final Map<String, Text> values = new LinkedHashMap<>();
	private Text steps, repetitions, seed, every, file, rName;
	private Button toR, run, cancel;
	private ProgressBar bar;
	private Label status;
	private volatile Experiment running;
	/* Cancel of an experiment of Python or R (an interrupt) */
	private volatile boolean cancelled;

	private boolean isR() {
		return script != null && "r".equals(script.spec().language);
	}

	private ExperimentDialog(Simulation model) {
		this.model = model;
		this.script = model instanceof ScriptSimulation.Host h ? h.script() : null;
	}

	/** Opens the dialog for the model. */
	static void open(Shell parent, Simulation model) {
		new ExperimentDialog(model).create(parent);
	}

	private void create(Shell parent) {
		Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setText("Experiment: " + model.modelName());
		shell.setLayout(new GridLayout(2, false));
		Label intro = new Label(shell, SWT.WRAP);
		intro.setText("Runs the model several times without a display. The values of a parameter: one value, a list (0.1, 0.2, 0.5) or a range (from:to:step). All combinations are run.");
		GridData id = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		id.widthHint = 460;
		intro.setLayoutData(id);
		Label heading = new Label(shell, SWT.NONE);
		heading.setText("Parameters");
		heading.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		for (Map.Entry<String, String[]> p : params().entrySet()) {
			new Label(shell, SWT.NONE).setText(p.getValue()[0]);
			Text t = new Text(shell, SWT.BORDER);
			t.setText(p.getValue()[1]);
			t.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			values.put(p.getKey(), t);
		}
		Label line = new Label(shell, SWT.SEPARATOR | SWT.HORIZONTAL);
		line.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		steps = field(shell, "Steps of a run", "200");
		repetitions = field(shell, "Repetitions (seeds)", "3");
		seed = field(shell, "Seed of the first repetition", Integer.toString(model.getStartSeed()));
		every = field(shell, "A row every n steps", "1");
		file = field(shell, "CSV file (in the project)", "experiments/" + model.modelName() + ".csv");
		toR = new Button(shell, SWT.CHECK);
		toR.setText("Data frame in R:");
		toR.setSelection(script != null && "r".equals(script.spec().language) || rRunning());
		rName = new Text(shell, SWT.BORDER);
		rName.setText("experiment");
		rName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		bar = new ProgressBar(shell, SWT.HORIZONTAL);
		bar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		status = new Label(shell, SWT.WRAP);
		GridData sd = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		sd.widthHint = 460;
		status.setLayoutData(sd);
		Composite buttons = new Composite(shell, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false, 2, 1));
		buttons.setLayout(new GridLayout(3, true));
		run = new Button(buttons, SWT.PUSH);
		run.setText("Run");
		run.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cancel = new Button(buttons, SWT.PUSH);
		cancel.setText("Cancel");
		cancel.setEnabled(false);
		cancel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button close = new Button(buttons, SWT.PUSH);
		close.setText("Close");
		close.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		run.addListener(SWT.Selection, e -> start(shell));
		cancel.addListener(SWT.Selection, e -> {
			Experiment x = running;
			if (x != null) {
				x.cancel();
			} else if (script != null) {
				/* Python and R: an interrupt of the interpreter (as the stop button of the job in the Progress view) */
				cancelled = true;
				if (isR()) {
					com.eco.bio7.rffm.RSession s = com.eco.bio7.rffm.RSession.current();
					if (s != null) {
						s.interrupt();
					}
				} else {
					com.eco.bio7.pybridge.EmbeddedPython.interrupt();
				}
			}
		});
		close.addListener(SWT.Selection, e -> shell.close());
		shell.setDefaultButton(run);
		shell.pack();
		shell.setSize(Math.max(520, shell.getSize().x), shell.getSize().y);
		shell.open();
	}

	private static Text field(Shell shell, String label, String value) {
		new Label(shell, SWT.NONE).setText(label);
		Text t = new Text(shell, SWT.BORDER);
		t.setText(value);
		t.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return t;
	}

	/* The parameters: the name -> the label and the current value */
	private Map<String, String[]> params() {
		Map<String, String[]> out = new LinkedHashMap<>();
		if (script != null) {
			for (JavaToJs.ParamInfo p : script.spec().params) {
				out.put(p.name, new String[] { p.label, text(p.value) });
			}
			return out;
		}
		List<Class<?>> chain = new ArrayList<>();
		for (Class<?> k = model.getClass(); k != null && k != Simulation.class; k = k.getSuperclass()) {
			chain.add(0, k);
		}
		for (Class<?> k : chain) {
			for (Field f : k.getDeclaredFields()) {
				if (f.isAnnotationPresent(Param.class) && !Modifier.isStatic(f.getModifiers())) {
					try {
						f.setAccessible(true);
						Param p = f.getAnnotation(Param.class);
						out.put(f.getName(), new String[] { p.label().isEmpty() ? f.getName() : p.label(), text(f.get(model)) });
					} catch (IllegalAccessException e) {
						/* not shown */
					}
				}
			}
		}
		return out;
	}

	private static String text(Object v) {
		if (v instanceof Double d && d == Math.rint(d) && Math.abs(d) < 1e15) {
			return Long.toString(d.longValue());
		}
		return String.valueOf(v);
	}

	private static boolean rRunning() {
		try {
			com.eco.bio7.rffm.RSession s = com.eco.bio7.rffm.RSession.current();
			return s != null && s.isAlive();
		} catch (Throwable e) {
			return false;
		}
	}

	/* Starts the experiment in another thread */
	private void start(Shell shell) {
		int nSteps, nRep, nSeed, nEvery;
		Map<String, Object[]> sweep = new LinkedHashMap<>();
		try {
			nSteps = Integer.parseInt(steps.getText().trim());
			nRep = Integer.parseInt(repetitions.getText().trim());
			nSeed = Integer.parseInt(seed.getText().trim());
			nEvery = Integer.parseInt(every.getText().trim());
			for (Map.Entry<String, Text> e : values.entrySet()) {
				Object[] v = Experiment.values(e.getValue().getText());
				if (v.length == 0) {
					throw new IllegalArgumentException("No value for " + e.getKey());
				}
				sweep.put(e.getKey(), v);
			}
		} catch (RuntimeException e) {
			status.setText("Error: " + e.getMessage());
			return;
		}
		/* The simulation of the views pauses (the model of the views is not used by the experiment of Java and Python) */
		com.eco.bio7.time.Time.setPause(true);
		File project = projectFolder();
		File out = new File(project, file.getText().trim());
		/* Shown: the path in the project (the full path if the file is outside) */
		String shown = out.toPath().normalize().startsWith(project.toPath().normalize()) ? project.getName() + "/" + project.toPath().normalize().relativize(out.toPath().normalize()).toString().replace('\\', '/') : out.getPath();
		boolean r = toR.getSelection();
		String frame = rName.getText().trim().isEmpty() ? "experiment" : rName.getText().trim();
		run.setEnabled(false);
		Display display = shell.getDisplay();
		cancel.setEnabled(true);
		cancelled = false;
		/* Python and R: the progress of experiment() (bio7.progress, bio7_progress) also in the bar of the dialog */
		String progressKey = script == null ? null : isR() ? "r:progress" : "progress";
		if (progressKey != null) {
			com.eco.bio7.pybridge.PythonJobs.setListener(progressKey, (done, total) -> display.asyncExec(() -> {
				if (!bar.isDisposed() && total > 0) {
					bar.setMaximum((int) Math.round(total));
					bar.setSelection((int) Math.round(done));
					status.setText("Run " + Math.round(done) + " of " + Math.round(total));
				}
			}));
		}
		bar.setSelection(0);
		status.setText("Running...");
		Runnable work = () -> {
			String message;
			try {
				int rows = script == null ? runJava(sweep, nSteps, nRep, nSeed, nEvery, out, r, frame, display) : runScript(sweep, nSteps, nRep, nSeed, nEvery, out, r, frame);
				message = rows + " rows saved to " + shown + (r ? (rRunning() ? " and as the data frame " + frame + " in R." : " (R is not running: no data frame).") : ".");
				refresh(out);
			} catch (Throwable e) {
				Throwable c = e instanceof ScriptSimulation.ScriptException && e.getCause() != null ? e.getCause() : e;
				if (cancelled) {
					message = "Cancelled.";
				} else {
					message = "Error: " + c.getMessage();
					c.printStackTrace();
				}
				/* R: the experiment ran with the model of the views: its setup again */
				if (isR()) {
					try {
						model.setup();
					} catch (RuntimeException ex) {
						/* shown by the model */
					}
				}
			} finally {
				if (progressKey != null) {
					com.eco.bio7.pybridge.PythonJobs.setListener(progressKey, null);
					com.eco.bio7.pybridge.PythonJobs.finishTask(progressKey);
				}
			}
			String m = message;
			display.asyncExec(() -> {
				running = null;
				if (shell.isDisposed()) {
					return;
				}
				run.setEnabled(true);
				cancel.setEnabled(false);
				bar.setSelection(bar.getMaximum());
				status.setText(m);
				shell.layout(true, true);
			});
		};
		if (script != null) {
			script.later(work);
		} else {
			Thread t = new Thread(work, "Bio7 experiment");
			t.setDaemon(true);
			t.start();
		}
	}

	private int runJava(Map<String, Object[]> sweep, int nSteps, int nRep, int nSeed, int nEvery, File out, boolean r, String frame, Display display) throws Exception {
		Experiment x = new Experiment(model).steps(nSteps).repetitions(nRep).seed(nSeed).every(nEvery);
		for (Map.Entry<String, Object[]> e : sweep.entrySet()) {
			x.paramValues(e.getKey(), e.getValue());
		}
		x.progress((done, total) -> display.asyncExec(() -> {
			if (!bar.isDisposed()) {
				bar.setMaximum(total);
				bar.setSelection(done);
				status.setText("Run " + done + " of " + total);
			}
		}));
		running = x;
		Experiment.Table t = x.run();
		t.save(out.getPath());
		if (r) {
			t.toR(frame);
		}
		return t.size();
	}

	/* Python and R: experiment() of bio7sim in the interpreter (in the thread of the script) */
	private int runScript(Map<String, Object[]> sweep, int nSteps, int nRep, int nSeed, int nEvery, File out, boolean r, String frame) throws Exception {
		out.getParentFile().mkdirs();
		String path = out.getAbsolutePath().replace('\\', '/');
		if ("r".equals(script.spec().language)) {
			StringBuilder p = new StringBuilder("list(");
			int k = 0;
			for (Map.Entry<String, Object[]> e : sweep.entrySet()) {
				p.append(k++ > 0 ? ", " : "").append('`').append(e.getKey()).append("` = c(");
				for (int i = 0; i < e.getValue().length; i++) {
					p.append(i > 0 ? ", " : "").append(rValue(e.getValue()[i]));
				}
				p.append(')');
			}
			p.append(')');
			String name = r ? frame : ".bio7sim_experiment";
			String code = name + " <- experiment(get('.bio7sim_model', envir = globalenv()), params = " + p + ", steps = " + nSteps + ", repetitions = " + nRep + ", seed = "
					+ nSeed + ", every = " + nEvery + "); utils::write.csv(" + name + ", " + JavaToJs.quote(path) + ", row.names = FALSE, na = 'NA')";
			com.eco.bio7.rffm.RSession s = com.eco.bio7.rffm.RSession.current();
			if (s == null) {
				throw new IllegalStateException("The embedded R is not running");
			}
			s.voidEval(code);
			double[] n = s.evalDoubles("nrow(" + name + ")");
			/* The model ran in the experiment: its setup again */
			model.setup();
			return n.length > 0 ? (int) n[0] : 0;
		}
		StringBuilder json = new StringBuilder("{\"params\": {");
		int k = 0;
		for (Map.Entry<String, Object[]> e : sweep.entrySet()) {
			json.append(k++ > 0 ? ", " : "").append(JavaToJs.quote(e.getKey())).append(": [");
			for (int i = 0; i < e.getValue().length; i++) {
				Object v = e.getValue()[i];
				json.append(i > 0 ? ", " : "").append(v instanceof String ? JavaToJs.quote((String) v) : String.valueOf(v));
			}
			json.append(']');
		}
		json.append("}, \"steps\": ").append(nSteps).append(", \"repetitions\": ").append(nRep).append(", \"seed\": ").append(nSeed).append(", \"every\": ").append(nEvery)
				.append(", \"file\": ").append(JavaToJs.quote(path)).append('}');
		ScriptSimulation.State st = script.run("experiment", json.toString());
		if (r && rRunning()) {
			com.eco.bio7.rffm.RSession.current().voidEval(frame + " <- utils::read.csv(" + JavaToJs.quote(path) + ", check.names = FALSE, stringsAsFactors = FALSE)");
		}
		return (int) st.number("rows", 0);
	}

	private static String rValue(Object v) {
		if (v instanceof Boolean b) {
			return b ? "TRUE" : "FALSE";
		}
		if (v instanceof Number n) {
			return Double.toString(n.doubleValue()).replace("Infinity", "Inf");
		}
		return JavaToJs.quote(String.valueOf(v));
	}

	/* The folder of the project of the model (the script or the Java source), else the workspace */
	private File projectFolder() {
		try {
			org.eclipse.core.resources.IWorkspaceRoot root = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot();
			if (script != null && script.sourceFile() != null) {
				for (org.eclipse.core.resources.IFile f : root.findFilesForLocationURI(script.sourceFile().toURI())) {
					return f.getProject().getLocation().toFile();
				}
				return script.sourceFile().getAbsoluteFile().getParentFile();
			}
			String suffix = "/" + model.getClass().getName().replace('.', '/') + ".java";
			List<org.eclipse.core.resources.IResource> found = new ArrayList<>();
			root.accept(r -> {
				if (found.isEmpty() && r.getType() == org.eclipse.core.resources.IResource.FILE && r.getFullPath().toString().endsWith(suffix)) {
					found.add(r);
				}
				return found.isEmpty() && (r.getType() != org.eclipse.core.resources.IResource.PROJECT || r.getProject().isOpen());
			});
			if (!found.isEmpty()) {
				return found.get(0).getProject().getLocation().toFile();
			}
			return root.getLocation().toFile();
		} catch (Exception e) {
			return new File(System.getProperty("user.home"));
		}
	}

	/* The new file in the workspace (the Navigator) */
	private static void refresh(File out) {
		try {
			for (org.eclipse.core.resources.IFile f : org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(out.toURI())) {
				/* The first folder which the workspace knows (the folder of the file may be new) */
				org.eclipse.core.resources.IContainer c = f.getParent();
				while (c != null && !c.exists()) {
					c = c.getParent();
				}
				if (c != null) {
					c.refreshLocal(org.eclipse.core.resources.IResource.DEPTH_INFINITE, null);
				}
			}
		} catch (Exception e) {
			/* not refreshed */
		}
	}
}
