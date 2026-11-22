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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import com.eco.bio7.collection.CustomView;

/**
 * The settings of a simulation model in Bio7: a custom view ("Settings") with
 * a control for each field with the annotation {@link Param} and a button for
 * each method with the annotation {@link Action} (the same controls as the
 * HTML export, below the headings of their groups). A change sets the field of
 * the running model (live) or, with restart = true, sets it and calls the
 * setup.
 * <p>
 * A model can show an own panel, e.g., a Composite designed with the
 * WindowBuilder: {@link Simulation#settings(Composite)} creates it. The
 * widgets of its fields are bound to the model by their names (see
 * {@link #bind(Object, Simulation)}).
 */
public final class SimulationSettings {

	private static final int SCALE = 1000;

	/* The actions without a group (at the end) */
	private static final String END = "\u0000";

	private SimulationSettings() {
	}

	/** The fields with the annotation Param (of the class and its superclasses). */
	static List<Field> paramFields(Class<?> c) {
		List<Field> fields = new ArrayList<>();
		List<Class<?>> classes = new ArrayList<>();
		for (Class<?> k = c; k != null && k != Simulation.class; k = k.getSuperclass()) {
			classes.add(0, k);
		}
		for (Class<?> k : classes) {
			for (Field f : k.getDeclaredFields()) {
				if (f.isAnnotationPresent(Param.class) && !Modifier.isStatic(f.getModifiers())) {
					f.setAccessible(true);
					fields.add(f);
				}
			}
		}
		return fields;
	}

	/** The methods with the annotation Action (sorted by the label). */
	static List<Method> actionMethods(Class<?> c) {
		List<Method> methods = new ArrayList<>();
		for (Class<?> k = c; k != null && k != Simulation.class; k = k.getSuperclass()) {
			for (Method m : k.getDeclaredMethods()) {
				if (m.isAnnotationPresent(Action.class) && m.getParameterCount() == 0 && !Modifier.isStatic(m.getModifiers())) {
					m.setAccessible(true);
					methods.add(m);
				}
			}
		}
		methods.sort(Comparator.comparing(SimulationSettings::label));
		return methods;
	}

	static String label(Method m) {
		String l = m.getAnnotation(Action.class).label();
		return l.isEmpty() ? m.getName() : l;
	}

	/**
	 * The settings and actions in the order of the page: the groups in the order
	 * of their first setting, in a group the settings (in the order of the
	 * fields), then the actions; the actions without a group at the end.
	 */
	static Map<String, List<Object>> groups(Class<?> c) {
		Map<String, List<Object>> groups = new LinkedHashMap<>();
		for (Field f : paramFields(c)) {
			groups.computeIfAbsent(f.getAnnotation(Param.class).group(), k -> new ArrayList<>()).add(f);
		}
		for (Method m : actionMethods(c)) {
			String g = m.getAnnotation(Action.class).group();
			groups.computeIfAbsent(g.isEmpty() ? END : g, k -> new ArrayList<>()).add(m);
		}
		return groups;
	}

	/** Shows the settings of the model (in the thread of the user interface). */
	public static void show(Simulation model) {
		if (!Simulation.hasDisplay()) {
			return;
		}
		/* A model of Python or R: the settings and actions of the script */
		ScriptSimulation.Script script = model instanceof ScriptSimulation.Host host ? host.script() : null;
		Map<String, List<Object>> groups = script != null ? script.groups() : groups(model.getClass());
		Composite parent = null;
		Composite own = null;
		boolean wantsOwn = hasOwnSettings(model);
		if (groups.isEmpty() && !wantsOwn) {
			return;
		}
		if (model.arrangeViews) {
			/* The left area of the Custom perspective (below the options of the display); the panels of another model are closed */
			SimulationLayout.closeOtherPanels(model);
			parent = new CustomView().getComposite(SimulationLayout.settingsPanel(model));
			com.eco.bio7.collection.Work.renamePanelTab(parent, "Settings");
		} else {
			parent = new CustomView().getComposite("Settings");
		}
		if (parent == null || parent.isDisposed()) {
			return;
		}
		parent.setLayout(new FillLayout());
		if (wantsOwn) {
			/* The panel of the model (e.g., designed with the WindowBuilder) below the controls of the simulation */
			Composite box = new Composite(parent, SWT.NONE);
			GridLayout bl0 = new GridLayout(2, false);
			bl0.marginWidth = 10;
			bl0.marginHeight = 10;
			box.setLayout(bl0);
			controls(box, model);
			own = model.settings(box);
			if (own != null) {
				own.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
				bind(own, model);
				parent.layout(true, true);
				return;
			}
			box.dispose();
		}
		ScrolledComposite scroll = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		Composite c = new Composite(scroll, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		layout.verticalSpacing = 8;
		c.setLayout(layout);
		Label head = new Label(c, SWT.NONE);
		head.setText(model.modelName());
		head.setFont(org.eclipse.jface.resource.JFaceResources.getBannerFont());
		head.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		/* Play/Pause, Step, Setup and the speed (as the toolbar and the Control view) */
		controls(c, model);
		for (Map.Entry<String, List<Object>> g : groups.entrySet()) {
			if (!g.getKey().isEmpty() && !g.getKey().equals(END)) {
				Label heading = new Label(c, SWT.NONE);
				heading.setText(g.getKey());
				heading.setFont(org.eclipse.jface.resource.JFaceResources.getHeaderFont());
				GridData hd = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
				hd.verticalIndent = 6;
				heading.setLayoutData(hd);
			}
			for (Object o : g.getValue()) {
				try {
					if (o instanceof Field f) {
						control(c, model, f);
					} else if (o instanceof JavaToJs.ParamInfo p) {
						scriptControl(c, model, script, p);
					} else if (o instanceof JavaToJs.ActionInfo a) {
						Button b = new Button(c, SWT.PUSH);
						b.setText(a.label);
						b.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
						b.addListener(SWT.Selection, e -> script.action(model, a.name));
					} else {
						Method m = (Method) o;
						Button b = new Button(c, SWT.PUSH);
						b.setText(label(m));
						b.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
						b.addListener(SWT.Selection, e -> action(model, m));
					}
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		Label line = new Label(c, SWT.SEPARATOR | SWT.HORIZONTAL);
		line.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		Composite buttons = new Composite(c, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		GridLayout bl = new GridLayout(1, true);
		bl.marginWidth = 0;
		buttons.setLayout(bl);
		Button export = new Button(buttons, SWT.PUSH);
		export.setText("Export HTML...");
		export.setToolTipText("Export the simulation as an HTML page (runs in the web browser)");
		export.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		export.addListener(SWT.Selection, e -> com.eco.bio7.actions.ExportSimulation.run(export.getShell()));
		scroll.setContent(c);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		scroll.setMinSize(c.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		parent.layout(true, true);
	}

	/* The icons of the controls (created once) */
	private static final Map<String, org.eclipse.swt.graphics.Image> ICONS = new java.util.HashMap<>();

	private static org.eclipse.swt.graphics.Image icon(String name) {
		return ICONS.computeIfAbsent(name, n -> com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/maintoolbar/" + n + ".svg").createImage());
	}

	/*
	 * The controls of the simulation at the top of the settings: Play/Pause (the
	 * state as the button of the toolbar), Step (one time step while paused), Setup
	 * (a new start, the counter at 0) and the speed (steps per second, as the
	 * Control view)
	 */
	private static void controls(Composite c, Simulation model) {
		Composite bar = new Composite(c, SWT.NONE);
		GridLayout gl = new GridLayout(3, true);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		bar.setLayout(gl);
		bar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		Button play = new Button(bar, SWT.PUSH);
		play.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button step = new Button(bar, SWT.PUSH);
		step.setText("Step");
		step.setToolTipText("One time step (while the simulation is paused)");
		step.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button setup = new Button(bar, SWT.PUSH);
		setup.setText("Setup");
		setup.setToolTipText("A new start of the simulation (init; the counter of the time steps starts at 0)");
		setup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Runnable state = () -> {
			if (play.isDisposed()) {
				return;
			}
			boolean paused = com.eco.bio7.time.Time.isPause();
			play.setText(paused ? "Play" : "Pause");
			play.setImage(icon(paused ? "play" : "pause"));
			play.setToolTipText(paused ? "Start the simulation" : "Pause the simulation (it runs)");
			step.setEnabled(paused);
			bar.layout(true, true);
		};
		state.run();
		Runnable pauseListener = () -> c.getDisplay().asyncExec(state);
		com.eco.bio7.time.Time.addPauseListener(pauseListener);
		play.addListener(SWT.Dispose, e -> com.eco.bio7.time.Time.removePauseListener(pauseListener));
		play.addListener(SWT.Selection, e -> com.eco.bio7.time.Time.setPause(!com.eco.bio7.time.Time.isPause()));
		step.addListener(SWT.Selection, e -> {
			/* In another thread (a model of Python or R must not run in the thread of the user interface) */
			Thread t = new Thread(() -> {
				if (com.eco.bio7.methods.Compiled.getModel() == model && com.eco.bio7.time.Time.isPause()) {
					model.run();
					com.eco.bio7.time.Time.count();
					showCounter(c.getDisplay());
				}
			}, "Bio7 simulation step");
			t.setDaemon(true);
			t.start();
		});
		setup.addListener(SWT.Selection, e -> {
			com.eco.bio7.time.Time.Timereset();
			com.eco.bio7.time.Time.setCounter(0);
			showCounter(c.getDisplay());
			model.setup();
		});
		/* The speed: the logarithmic slider of the Control view */
		Composite speed = new Composite(c, SWT.NONE);
		GridLayout sl = new GridLayout(3, false);
		sl.marginWidth = 0;
		sl.marginHeight = 2;
		speed.setLayout(sl);
		speed.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		new Label(speed, SWT.NONE).setText("Speed");
		com.eco.bio7.info.SpeedSlider slider = new com.eco.bio7.info.SpeedSlider(speed, SWT.NONE, com.eco.bio7.time.Time.MIN_FPS, com.eco.bio7.time.Time.MAX_FPS);
		slider.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		slider.setValue(com.eco.bio7.time.Time.getFps());
		slider.setToolTipText("Time steps per second: left slower, right faster (also in the Control view)");
		Label shown = new Label(speed, SWT.RIGHT);
		GridData sd = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		sd.widthHint = 80;
		shown.setLayoutData(sd);
		Runnable showSpeed = () -> {
			if (!shown.isDisposed()) {
				shown.setText(com.eco.bio7.info.SpeedSlider.format(com.eco.bio7.time.Time.getFps()) + " steps/s");
			}
		};
		showSpeed.run();
		slider.addListener(SWT.Selection, e -> {
			com.eco.bio7.time.Time.setFps(slider.getValue());
			showSpeed.run();
		});
		Runnable intervalListener = () -> c.getDisplay().asyncExec(() -> {
			if (!slider.isDisposed() && Math.abs(slider.getValue() - com.eco.bio7.time.Time.getFps()) > 1e-9) {
				slider.setValue(com.eco.bio7.time.Time.getFps());
			}
			showSpeed.run();
		});
		com.eco.bio7.time.Time.addIntervalListener(intervalListener);
		slider.addListener(SWT.Dispose, e -> com.eco.bio7.time.Time.removeIntervalListener(intervalListener));
		Label line = new Label(c, SWT.SEPARATOR | SWT.HORIZONTAL);
		line.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
	}

	/* The counter of the time steps in the status line */
	private static void showCounter(org.eclipse.swt.widgets.Display display) {
		display.asyncExec(() -> {
			if (com.eco.bio7.rcp.ApplicationActionBarAdvisor.getUserItem() != null) {
				com.eco.bio7.rcp.ApplicationActionBarAdvisor.getUserItem().setText("Time steps: " + com.eco.bio7.time.Time.getCounter());
			}
		});
	}

	/* true if the class of the model overrides settings(Composite) */
	private static boolean hasOwnSettings(Simulation model) {
		try {
			return model.getClass().getMethod("settings", Composite.class).getDeclaringClass() != Simulation.class;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}

	private static void control(Composite c, Simulation model, Field f) throws IllegalAccessException {
		Param p = f.getAnnotation(Param.class);
		String label = p.label().isEmpty() ? f.getName() : p.label();
		Class<?> t = f.getType();
		boolean integer = isInteger(t);
		if (t == boolean.class) {
			Button check = new Button(c, SWT.CHECK);
			check.setText(label);
			check.setSelection(f.getBoolean(model));
			check.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			check.addListener(SWT.Selection, e -> apply(model, f, check.getSelection()));
			return;
		}
		Label name = new Label(c, SWT.NONE);
		name.setText(label);
		name.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		if (t == String.class) {
			String value = String.valueOf(f.get(model));
			if (p.options().length > 0) {
				Combo combo = new Combo(c, SWT.READ_ONLY);
				combo.setItems(p.options());
				combo.select(Math.max(0, java.util.Arrays.asList(p.options()).indexOf(value)));
				combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
				combo.addListener(SWT.Selection, e -> apply(model, f, combo.getText()));
			} else {
				Text text = new Text(c, SWT.BORDER);
				text.setText(value);
				text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
				text.addListener(SWT.DefaultSelection, e -> apply(model, f, text.getText()));
				text.addListener(SWT.FocusOut, e -> apply(model, f, text.getText()));
			}
			return;
		}
		double value = ((Number) f.get(model)).doubleValue();
		if (hasRange(p)) {
			Scale scale = new Scale(c, SWT.HORIZONTAL);
			scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			Label shown = new Label(c, SWT.RIGHT);
			GridData gd = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
			gd.widthHint = 64;
			shown.setLayoutData(gd);
			bindRange(scale, model, f, shown);
		} else {
			Text text = new Text(c, SWT.BORDER);
			text.setText(format(value, integer ? 1 : 1e-6, integer));
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			bindNumberText(text, model, f);
		}
	}

	/* A control of a setting of a script model (Python or R): the value is sent to the script */
	private static void scriptControl(Composite c, Simulation model, ScriptSimulation.Script script, JavaToJs.ParamInfo p) {
		boolean integer = p.kind.equals("int");
		if (p.kind.equals("boolean")) {
			Button check = new Button(c, SWT.CHECK);
			check.setText(p.label);
			check.setSelection(Boolean.TRUE.equals(p.value));
			check.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			check.addListener(SWT.Selection, e -> script.set(model, p, check.getSelection()));
			return;
		}
		Label name = new Label(c, SWT.NONE);
		name.setText(p.label);
		name.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		if (p.kind.equals("string")) {
			String value = String.valueOf(p.value);
			if (p.options.length > 0) {
				Combo combo = new Combo(c, SWT.READ_ONLY);
				combo.setItems(p.options);
				combo.select(Math.max(0, java.util.Arrays.asList(p.options).indexOf(value)));
				combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
				combo.addListener(SWT.Selection, e -> script.set(model, p, combo.getText()));
			} else {
				Text text = new Text(c, SWT.BORDER);
				text.setText(value);
				text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
				text.addListener(SWT.DefaultSelection, e -> script.set(model, p, text.getText()));
				text.addListener(SWT.FocusOut, e -> {
					if (!text.getText().equals(String.valueOf(p.value))) {
						script.set(model, p, text.getText());
					}
				});
			}
			return;
		}
		double value = ((Number) p.value).doubleValue();
		if (!Double.isNaN(p.min) && !Double.isNaN(p.max) && p.max > p.min) {
			double st = !Double.isNaN(p.step) && p.step > 0 ? p.step : integer ? 1 : (p.max - p.min) / 100;
			int steps = (int) Math.max(1, Math.round((p.max - p.min) / st));
			Scale scale = new Scale(c, SWT.HORIZONTAL);
			scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			Label shown = new Label(c, SWT.RIGHT);
			GridData gd = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
			gd.widthHint = 64;
			shown.setLayoutData(gd);
			scale.setMinimum(0);
			scale.setMaximum(steps);
			scale.setPageIncrement(Math.max(1, steps / 10));
			scale.setSelection((int) Math.round((value - p.min) / st));
			shown.setText(format(value, st, integer));
			java.util.function.DoubleSupplier current = () -> Math.min(p.max, p.min + scale.getSelection() * st);
			Runnable send = () -> {
				double v = current.getAsDouble();
				Object o = integer ? (Object) (int) Math.round(v) : (Object) v;
				if (!o.equals(p.value)) {
					script.set(model, p, o);
				}
			};
			scale.addListener(SWT.Selection, e -> {
				shown.setText(format(current.getAsDouble(), st, integer));
				/* A restart setting after the mouse or the key is released (not for each value while dragging) */
				if (!p.restart) {
					send.run();
				}
			});
			scale.addListener(SWT.MouseUp, e -> send.run());
			scale.addListener(SWT.KeyUp, e -> send.run());
		} else {
			Text text = new Text(c, SWT.BORDER);
			text.setText(format(value, integer ? 1 : 1e-6, integer));
			text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			Runnable send = () -> {
				try {
					double v = Double.parseDouble(text.getText().trim().replace(',', '.'));
					script.set(model, p, integer ? (Object) (int) Math.round(v) : (Object) v);
				} catch (NumberFormatException ex) {
					text.setText(format(((Number) p.value).doubleValue(), integer ? 1 : 1e-6, integer));
				}
			};
			text.addListener(SWT.DefaultSelection, e -> send.run());
			text.addListener(SWT.FocusOut, e -> send.run());
		}
	}

	private static boolean isInteger(Class<?> t) {
		return t == int.class || t == long.class || t == short.class || t == byte.class;
	}

	private static boolean hasRange(Param p) {
		return !Double.isNaN(p.min()) && !Double.isNaN(p.max()) && p.max() > p.min();
	}

	private static double step(Param p, boolean integer) {
		return !Double.isNaN(p.step()) ? p.step() : integer ? 1 : (p.max() - p.min()) / 100;
	}

	/* ===================== The binding of widgets (e.g., of a WindowBuilder panel) ===================== */

	/**
	 * Binds the widgets of a panel (e.g., a Composite designed with the
	 * WindowBuilder) to the model by the names of the fields of the panel:
	 * <ul>
	 * <li>a Scale or Slider with the name of a Param field: the range of the Param
	 * (min, max, step), else the value of the selection</li>
	 * <li>a Spinner: the value (with its digits), a Text: the value as text</li>
	 * <li>a check Button: a boolean Param, a Combo: a String Param (the options of
	 * the Param if the Combo has no items)</li>
	 * <li>a Label with the name of a Param and "Value" (e.g., feedValue): shows the
	 * value</li>
	 * <li>a push Button with the name of an Action method: calls the method; the
	 * names setup and export: the setup and the HTML export</li>
	 * </ul>
	 * Called automatically for the panel of {@link Simulation#settings(Composite)}.
	 *
	 * @param panel the object with the widgets as fields
	 * @param model the simulation model
	 * @return the number of the bound widgets
	 */
	public static int bind(Object panel, Simulation model) {
		Map<String, Field> params = new HashMap<>();
		for (Field f : paramFields(model.getClass())) {
			params.put(f.getName(), f);
		}
		Map<String, Method> actions = new HashMap<>();
		for (Method m : actionMethods(model.getClass())) {
			actions.put(m.getName(), m);
		}
		/* The widgets of the panel (the fields of its class and superclasses up to the SWT classes) */
		Map<String, Widget> widgets = new LinkedHashMap<>();
		for (Class<?> k = panel.getClass(); k != null && !k.getName().startsWith("org.eclipse.swt."); k = k.getSuperclass()) {
			for (Field f : k.getDeclaredFields()) {
				if (Widget.class.isAssignableFrom(f.getType()) && !Modifier.isStatic(f.getModifiers())) {
					try {
						f.setAccessible(true);
						Object w = f.get(panel);
						if (w instanceof Widget widget && !widget.isDisposed()) {
							widgets.putIfAbsent(f.getName(), widget);
						}
					} catch (IllegalAccessException | RuntimeException e) {
						/* Not accessible (e.g., a module): not bound */
					}
				}
			}
		}
		int bound = 0;
		for (Map.Entry<String, Widget> e : widgets.entrySet()) {
			String name = e.getKey();
			Widget w = e.getValue();
			Field f = params.get(name);
			Label shown = widgets.get(name + "Value") instanceof Label l ? l : null;
			try {
				if (f != null) {
					if (w instanceof Scale || w instanceof Slider) {
						bindRange((Control) w, model, f, shown);
					} else if (w instanceof Spinner sp) {
						bindSpinner(sp, model, f, shown);
					} else if (w instanceof Button b && (b.getStyle() & (SWT.CHECK | SWT.TOGGLE)) != 0 && f.getType() == boolean.class) {
						b.setSelection(f.getBoolean(model));
						b.addListener(SWT.Selection, ev -> apply(model, f, b.getSelection()));
					} else if (w instanceof Combo combo) {
						Param p = f.getAnnotation(Param.class);
						if (combo.getItemCount() == 0 && p.options().length > 0) {
							combo.setItems(p.options());
						}
						int index = combo.indexOf(String.valueOf(f.get(model)));
						if (index >= 0) {
							combo.select(index);
						}
						combo.addListener(SWT.Selection, ev -> apply(model, f, convert(f, combo.getText())));
					} else if (w instanceof Text text) {
						if (f.getType() == String.class) {
							text.setText(String.valueOf(f.get(model)));
							text.addListener(SWT.DefaultSelection, ev -> apply(model, f, text.getText()));
							text.addListener(SWT.FocusOut, ev -> apply(model, f, text.getText()));
						} else {
							text.setText(format(((Number) f.get(model)).doubleValue(), isInteger(f.getType()) ? 1 : 1e-6, isInteger(f.getType())));
							bindNumberText(text, model, f);
						}
					} else {
						continue;
					}
					bound++;
				} else if (w instanceof Button b && (b.getStyle() & SWT.PUSH) != 0) {
					Method m = actions.get(name);
					if (m != null) {
						b.addListener(SWT.Selection, ev -> action(model, m));
					} else if (name.equals("setup")) {
						b.addListener(SWT.Selection, ev -> model.setup());
					} else if (name.equals("export") || name.equals("exportHtml")) {
						b.addListener(SWT.Selection, ev -> com.eco.bio7.actions.ExportSimulation.run(b.getShell()));
					} else {
						continue;
					}
					bound++;
				}
			} catch (IllegalAccessException ex) {
				ex.printStackTrace();
			}
		}
		return bound;
	}

	/* A Scale or Slider: the range of the Param (else the selection is the value) */
	private static void bindRange(Control w, Simulation model, Field f, Label shown) throws IllegalAccessException {
		Param p = f.getAnnotation(Param.class);
		boolean integer = isInteger(f.getType());
		double value = ((Number) f.get(model)).doubleValue();
		boolean range = hasRange(p);
		double min = range ? p.min() : 0, max = range ? p.max() : 0;
		double step = range ? step(p, integer) : 1;
		if (range) {
			setRange(w, 0, SCALE);
			setSelection(w, (int) Math.round((value - min) / (max - min) * SCALE));
		} else {
			setSelection(w, (int) Math.round(value));
		}
		if (shown != null) {
			shown.setText(format(value, step, integer));
		}
		w.addListener(SWT.Selection, e -> {
			double v;
			if (range) {
				v = min + selection(w) / (double) SCALE * (max - min);
				/* The value on the steps of the slider (as the HTML page) */
				v = Math.min(max, Math.max(min, min + Math.round((v - min) / step) * step));
			} else {
				v = selection(w);
			}
			if (shown != null && !shown.isDisposed()) {
				shown.setText(format(v, step, integer));
				shown.getParent().layout();
			}
			apply(model, f, v);
		});
	}

	private static void setRange(Control w, int min, int max) {
		if (w instanceof Scale s) {
			s.setMinimum(min);
			s.setMaximum(max);
			s.setPageIncrement(max / 10);
		} else if (w instanceof Slider s) {
			s.setValues(s.getSelection(), min, max + s.getThumb(), s.getThumb(), 1, max / 10);
		}
	}

	private static void setSelection(Control w, int v) {
		if (w instanceof Scale s) {
			s.setSelection(v);
		} else if (w instanceof Slider s) {
			s.setSelection(v);
		}
	}

	private static int selection(Control w) {
		return w instanceof Scale s ? s.getSelection() : ((Slider) w).getSelection();
	}

	/* A Spinner: the value with the digits of the Spinner (and the range of the Param) */
	private static void bindSpinner(Spinner sp, Simulation model, Field f, Label shown) throws IllegalAccessException {
		Param p = f.getAnnotation(Param.class);
		int digits = sp.getDigits();
		double factor = Math.pow(10, digits);
		if (hasRange(p)) {
			sp.setMinimum((int) Math.round(p.min() * factor));
			sp.setMaximum((int) Math.round(p.max() * factor));
		}
		double value = ((Number) f.get(model)).doubleValue();
		sp.setSelection((int) Math.round(value * factor));
		if (shown != null) {
			shown.setText(format(value, 1 / factor, isInteger(f.getType())));
		}
		sp.addListener(SWT.Selection, e -> {
			double v = sp.getSelection() / factor;
			if (shown != null && !shown.isDisposed()) {
				shown.setText(format(v, 1 / factor, isInteger(f.getType())));
			}
			apply(model, f, v);
		});
	}

	private static void bindNumberText(Text text, Simulation model, Field f) {
		Runnable set = () -> {
			try {
				apply(model, f, Double.parseDouble(text.getText().trim()));
				text.setForeground(null);
			} catch (NumberFormatException ex) {
				text.setForeground(text.getDisplay().getSystemColor(SWT.COLOR_RED));
			}
		};
		text.addListener(SWT.DefaultSelection, e -> set.run());
		text.addListener(SWT.FocusOut, e -> set.run());
	}

	private static Object convert(Field f, String text) {
		if (f.getType() == String.class) {
			return text;
		}
		if (f.getType() == boolean.class) {
			return Boolean.parseBoolean(text);
		}
		try {
			return Double.parseDouble(text);
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	static String format(double v, double step, boolean integer) {
		if (integer) {
			return String.valueOf(Math.round(v));
		}
		int digits = step >= 1 ? 0 : Math.min(6, (int) Math.ceil(-Math.log10(step) - 1e-9));
		String s = String.format(Locale.ROOT, "%." + digits + "f", v);
		return step < 1e-5 ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
	}

	/* Calls an action of the model and displays the model */
	private static void action(Simulation model, Method m) {
		try {
			synchronized (model) {
				m.invoke(model);
			}
			model.display();
		} catch (IllegalAccessException | InvocationTargetException e) {
			Throwable cause = e instanceof InvocationTargetException ite ? ite.getCause() : e;
			System.err.println("The action " + m.getName() + " failed:");
			cause.printStackTrace();
		}
	}

	/* Sets the field (and calls the setup with restart, else displays the model) */
	private static void apply(Simulation model, Field f, Object value) {
		boolean restart = f.getAnnotation(Param.class).restart();
		try {
			Class<?> t = f.getType();
			synchronized (model) {
				if (t == boolean.class) {
					f.setBoolean(model, (Boolean) value);
				} else if (t == String.class) {
					f.set(model, String.valueOf(value));
				} else {
					double d = ((Number) value).doubleValue();
					if (t == int.class) {
						f.setInt(model, (int) Math.round(d));
					} else if (t == long.class) {
						f.setLong(model, Math.round(d));
					} else if (t == short.class) {
						f.setShort(model, (short) Math.round(d));
					} else if (t == byte.class) {
						f.setByte(model, (byte) Math.round(d));
					} else if (t == float.class) {
						f.setFloat(model, (float) d);
					} else {
						f.setDouble(model, d);
					}
				}
			}
			if (restart) {
				model.setup();
			} else {
				model.display();
			}
		} catch (IllegalAccessException | IllegalArgumentException e) {
			e.printStackTrace();
		}
	}
}
