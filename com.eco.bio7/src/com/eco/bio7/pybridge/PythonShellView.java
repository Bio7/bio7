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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.part.ViewPart;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.pyffm.PyException;
import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.pyffm.PyValue;

/**
 * The Python-Shell view (as the R-Shell): evaluates Python code in the embedded
 * Python (the workspace of the console and the scripts), lists the objects and
 * shows, summarizes, converts and transfers them (table, R, ImageJ, plots).
 */
public class PythonShellView extends ViewPart {

	public static final String ID = "com.eco.bio7.PythonShell";
	private static final String R_SHELL = "com.eco.bio7.RShell";
	private static final String FONT_PREFERENCE = "PYTHON_SHELL_FONT";

	private com.eco.bio7.rbridge.views.RShellText input;
	private org.eclipse.swt.widgets.List objectList;
	private StyledText output;
	private final List<String> names = new ArrayList<>();
	private final List<String> history = new ArrayList<>();
	private int historyIndex;
	private String filter = "all";
	private Font font;
	private PythonShellCompletion completion;
	/* The calls of the view run one after another (not in the UI thread) */
	private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "Python-Shell");
		t.setDaemon(true);
		return t;
	});

	@Override
	public void createPartControl(Composite parent) {
		/* The layout of the R-Shell: 6 equal columns, the input, two rows of buttons, list and output */
		parent.setLayout(new GridLayout(6, true));
		input = new com.eco.bio7.rbridge.views.RShellText(parent, SWT.SINGLE | SWT.BORDER);
		GridData gdInput = new GridData(SWT.FILL, SWT.FILL, true, false, 6, 1);
		input.setLayoutData(gdInput);
		input.setToolTipText("Python code (Enter: evaluate, Up/Down: history, " + PythonShellCompletion.activationKey().format() + ": code completion)");
		/* Before the key listener: an open popup takes Return and the arrow keys */
		completion = new PythonShellCompletion(input);
		input.addListener(SWT.KeyDown, e -> {
			if (!e.doit || completion.isOpen()) {
				return;
			}
			if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
				e.doit = false;
				evaluate();
			} else if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN) {
				e.doit = false;
				browseHistory(e.keyCode == SWT.ARROW_UP ? -1 : 1);
			}
		});

		button(parent, "Eval", "Evaluate the Python code (Enter)", this::evaluate);
		button(parent, "Load", "Import data to Python (numpy .npy/.npz, .csv, .tsv, .xlsx, .pkl) or run a .py file", this::load);
		button(parent, "Save", "Export the selected object (.npy, .csv, .xlsx, .pkl) or save the history (.py)", this::save);
		button(parent, "Py Doc", "Open the Python documentation (docs.python.org)", () -> org.eclipse.swt.program.Program.launch("https://docs.python.org/3/"));
		button(parent, "?", "The help of the selected object or of the name in the text field", this::doc);
		Button stop = button(parent, "", "Interrupt the Python execution (KeyboardInterrupt)", EmbeddedPython::interrupt);
		stop.setImage(image("/icons/views/deleteaction.svg"));

		Label separator = new Label(parent, SWT.HORIZONTAL | SWT.SEPARATOR);
		separator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 6, 1));

		button(parent, "List", "List the objects of the Python workspace", this::refresh);
		button(parent, "Del", "Delete the selected objects from the workspace", this::delete);
		button(parent, "Gc", "Causes a garbage collection to take place", () -> runShow("import gc as _gc; print('Collected objects:', _gc.collect()); del _gc"));
		button(parent, "Clear", "Clear the text field and the output", () -> {
			input.setText("");
			output.setText("");
		});
		button(parent, "Font", "Changes the fonts for the list and \nthe results view.\nChanges are stored", this::chooseFont);
		Button info = button(parent, "", "Information about the Python-Shell", this::about);
		info.setImage(image("/icons/views/helpaction.svg"));

		SashForm sash = new SashForm(parent, SWT.NONE);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 6, 1));
		/* The list in its own sash (as in the R-Shell: no focus ring around the list) */
		SashForm listSash = new SashForm(sash, SWT.VERTICAL);
		objectList = new org.eclipse.swt.widgets.List(listSash, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		listSash.setWeights(1);
		objectList.addListener(SWT.Selection, _ -> describeSelected());
		objectList.addListener(SWT.DefaultSelection, _ -> showSelected("show"));
		objectList.setMenu(createMenu());
		output = new StyledText(sash, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.READ_ONLY);
		sash.setWeights(233, 319);
		applyFont(loadFont());
		OPEN.add(this);
		parent.addListener(SWT.Dispose, _ -> {
			OPEN.remove(this);
			worker.shutdownNow();
			if (font != null) {
				font.dispose();
			}
			for (Image i : images) {
				i.dispose();
			}
		});
		if (EmbeddedPython.isRunning()) {
			refresh();
		}
	}

	/* The icons of the buttons (disposed with the view) */
	private final List<Image> images = new ArrayList<>();

	private Image image(String path) {
		Image i = Bio7Plugin.getImageDescriptor(path).createImage();
		images.add(i);
		return i;
	}

	@Override
	public void setFocus() {
		input.setFocus();
		/* The objects of scripts and the console */
		if (EmbeddedPython.isRunning() && !EmbeddedPython.isBusy()) {
			refresh();
		}
	}

	/* A button of the R-Shell layout: a cell of the grid, 40 pixels high */
	private static Button button(Composite parent, String text, String tip, Runnable action) {
		Button b = new Button(parent, SWT.PUSH);
		b.setText(text);
		b.setToolTipText(tip);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd.heightHint = 40;
		b.setLayoutData(gd);
		b.addListener(SWT.Selection, _ -> action.run());
		return b;
	}

	/* ---------------- Python ---------------- */

	/* Starts the embedded Python if necessary and loads the functions of the view */
	private PySession python() throws Exception {
		PySession py = EmbeddedPython.session();
		if (py == null) {
			String[] error = new String[1];
			Display.getDefault().syncExec(() -> error[0] = PythonImageTransfer.ensurePython() ? null : "Python is not running");
			if (error[0] != null) {
				throw new PyException(error[0]);
			}
			py = EmbeddedPython.session();
		}
		loadModule(py);
		return py;
	}

	/* Loads the functions of the view (the module _bio7shell) if necessary (in the thread of Python) */
	static void loadModule(PySession py) throws Exception {
		PyValue loaded = py.eval("'_bio7shell' in __import__('sys').modules");
		if (!(loaded instanceof PyValue.Array a && a.getDouble(0) != 0)) {
			String source;
			try (InputStream in = PythonShellView.class.getResourceAsStream("python_shell.py")) {
				source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
			py.assign("_bio7_src", PyValue.of(source));
			py.exec("import types as _t, sys as _s\n_m = _t.ModuleType('_bio7shell')\nexec(compile(_bio7_src, '<_bio7shell>', 'exec'), _m.__dict__)\n"
					+ "_s.modules['_bio7shell'] = _m\ndel _t, _s, _m, _bio7_src");
		}
	}

	/**
	 * The cells of an object of the workspace for the table (mode: head, frame,
	 * matrix, rows, columns). In the thread of Python (e.g., a query of the
	 * debugger).
	 */
	public static List<String[]> tableRows(PySession py, String name, String mode) throws Exception {
		loadModule(py);
		String argsName = PythonArgs.assign(py, List.of(PyValue.of(name), PyValue.of(mode)));
		PyValue v = py.eval("__import__('_bio7shell').cells(*" + argsName + ")");
		List<String[]> rows = new ArrayList<>();
		if (v instanceof PyValue.PyList l) {
			for (PyValue r : l.values()) {
				rows.add(r instanceof PyValue.Str st ? st.values() : new String[] { text(r) });
			}
		}
		return rows;
	}

	/* Calls a function of the view module with string arguments */
	private PyValue call(String function, String... args) throws Exception {
		PySession py = python();
		List<PyValue> list = new ArrayList<>();
		for (String a : args) {
			list.add(PyValue.of(a));
		}
		String argsName = PythonArgs.assign(py, list);
		return py.eval("__import__('_bio7shell')." + function + "(*" + argsName + ")");
	}

	private static String text(PyValue v) {
		return v instanceof PyValue.Str s ? s.asString() : String.valueOf(v);
	}

	/* Runs a task in the worker thread; errors in the output */
	private void async(TaskBody body) {
		worker.submit(() -> {
			try {
				body.run();
			} catch (Exception e) {
				print(e.getMessage() + "\n");
			}
		});
	}

	@FunctionalInterface
	private interface TaskBody {
		void run() throws Exception;
	}

	private void print(String text) {
		Display.getDefault().asyncExec(() -> {
			if (output != null && !output.isDisposed()) {
				output.append(text);
				output.setTopIndex(output.getLineCount() - 1);
			}
		});
	}

	private void setOutput(String text) {
		Display.getDefault().asyncExec(() -> {
			if (output != null && !output.isDisposed()) {
				output.setText(text);
			}
		});
	}

	/* ---------------- Evaluation ---------------- */

	private void evaluate() {
		String code = input.getText();
		if (code.isBlank()) {
			return;
		}
		history.remove(code);
		history.add(code);
		historyIndex = history.size();
		input.setText("");
		print(">>> " + code + "\n");
		runShow(code);
	}

	/* Runs code, prints the output and lists the objects again */
	private void runShow(String code) {
		async(() -> {
			PyValue out = call("run", code);
			print(text(out));
			refreshNow();
		});
	}

	private void browseHistory(int step) {
		if (history.isEmpty()) {
			return;
		}
		historyIndex = Math.max(0, Math.min(history.size(), historyIndex + step));
		input.setText(historyIndex < history.size() ? history.get(historyIndex) : "");
		input.setCaretOffset(input.getCharCount());
	}

	/* ---------------- The objects ---------------- */

	/* The open views (updated after the console, scripts and transfers) */
	private static final java.util.Set<PythonShellView> OPEN = java.util.concurrent.ConcurrentHashMap.newKeySet();
	/* An update is queued (several changes give one update) */
	private final java.util.concurrent.atomic.AtomicBoolean refreshPending = new java.util.concurrent.atomic.AtomicBoolean();

	/** Lists the objects of the Python workspace (e.g., after a script). */
	public void refresh() {
		if (refreshPending.compareAndSet(false, true)) {
			async(() -> {
				refreshPending.set(false);
				refreshNow();
			});
		}
	}

	/**
	 * Updates the lists of the open Python-Shell views (after a console line, a
	 * script or a transfer of data). Any thread; returns at once.
	 */
	public static void refreshOpenViews() {
		for (PythonShellView v : OPEN) {
			v.refresh();
		}
	}

	private void refreshNow() throws Exception {
		PyValue v = call("objects", filter);
		List<String[]> rows = new ArrayList<>();
		if (v instanceof PyValue.PyList l) {
			for (PyValue e : l.values()) {
				if (e instanceof PyValue.Str s && s.length() == 2) {
					rows.add(s.values());
				}
			}
		}
		Display.getDefault().asyncExec(() -> {
			if (objectList == null || objectList.isDisposed()) {
				return;
			}
			List<String> selected = selectedNames();
			names.clear();
			objectList.removeAll();
			for (String[] r : rows) {
				names.add(r[0]);
				/* Only the name (as in the R-Shell): the description follows the selection */
				objectList.add(r[0]);
			}
			for (int i = 0; i < names.size(); i++) {
				if (selected.contains(names.get(i))) {
					objectList.select(i);
				}
			}
		});
	}

	private List<String> selectedNames() {
		List<String> s = new ArrayList<>();
		for (int i : objectList.getSelectionIndices()) {
			if (i < names.size()) {
				s.add(names.get(i));
			}
		}
		return s;
	}

	private String selectedName() {
		List<String> s = selectedNames();
		if (s.isEmpty()) {
			MessageDialog.openInformation(getSite().getShell(), "Python-Shell", "Please select an object in the list.");
			return null;
		}
		return s.get(0);
	}

	private void describeSelected() {
		List<String> s = selectedNames();
		if (s.size() == 1) {
			String name = s.get(0);
			async(() -> setOutput(text(call("describe", name))));
		}
	}

	private void showSelected(String function) {
		String name = selectedName();
		if (name != null) {
			async(() -> setOutput(text(call(function, name))));
		}
	}

	private void delete() {
		List<String> s = selectedNames();
		if (s.isEmpty() || !MessageDialog.openConfirm(getSite().getShell(), "Remove objects", "Delete " + String.join(", ", s) + "?")) {
			return;
		}
		runShow("del " + String.join(", ", s));
	}

	/* ---------------- The menu of the list ---------------- */

	private Menu createMenu() {
		Menu menu = new Menu(objectList);
		item(menu, "Show", () -> showSelected("show"));
		item(menu, "Summary", () -> showSelected("summary"));
		Menu filterMenu = submenu(menu, "Filter");
		for (String[] f : new String[][] { { "All", "all" }, { "Arrays", "array" }, { "DataFrames", "DataFrame" }, { "Series", "Series" }, { "Lists", "list" },
				{ "Dicts", "dict" }, { "Functions", "function" } }) {
			item(filterMenu, f[0], () -> {
				filter = f[1];
				refresh();
			});
		}
		Menu convert = submenu(menu, "Convert");
		for (String[] c : new String[][] { { "To Array (numpy)", "array" }, { "To DataFrame", "DataFrame" }, { "To List", "list" }, { "To Float", "float" },
				{ "To Categorical", "categorical" }, { "To String", "str" } }) {
			item(convert, c[0], () -> {
				String name = selectedName();
				if (name != null) {
					async(() -> {
						print(name + ": " + text(call("convert", name, c[1])) + "\n");
						refreshNow();
					});
				}
			});
		}
		Menu table = submenu(menu, "Transfer to Table");
		item(table, "DataFrame with Head", () -> toTable("head"));
		item(table, "DataFrame", () -> toTable("frame"));
		item(table, "Matrix", () -> toTable("matrix"));
		item(table, "Vector to Rows", () -> toTable("rows"));
		item(table, "Vector to Columns", () -> toTable("columns"));
		new MenuItem(table, SWT.SEPARATOR);
		/* Several selected objects (x, y, ...) and the elements of a list: a column or row each, the names as header */
		item(table, "Selected as Columns", () -> toTableMany("columns"));
		item(table, "Selected as Rows", () -> toTableMany("rows"));
		item(table, "List Elements as Columns", () -> toTableMany("list_columns"));
		item(table, "List Elements as Rows", () -> toTableMany("list_rows"));
		new MenuItem(menu, SWT.SEPARATOR);
		item(menu, "To R", this::toR);
		item(menu, "To ImageJ", this::toImageJ);
		/* Plots of the selected variables (several together), with attributes in a dialog (as in the R-Shell) */
		Menu plot = submenu(menu, "Plot");
		for (String[] k : com.eco.bio7.plot.PlotOptions.KINDS) {
			item(plot, k[1], () -> plotSelected(com.eco.bio7.plot.PlotOptions.last().withKind(k[0])));
		}
		item(plot, "Image", () -> plot("image"));
		new MenuItem(plot, SWT.SEPARATOR);
		item(plot, "Plot with Options...", () -> {
			List<String> sel = selectedNames();
			/* The plot view (Python): the data of the workspace for the roles of the plot type, the options, the source */
			com.eco.bio7.plot.PlotView.open("Python", new com.eco.bio7.plot.PlotView.Target() {
				public List<String> variables() {
					return plotWorkspaceVariables();
				}

				public void plot(com.eco.bio7.plot.PlotOptions o, com.eco.bio7.plot.PlotData d) {
					runShow(PythonShellPlots.code(d, o) + "del bio7_plot\n");
				}

				public String source(com.eco.bio7.plot.PlotOptions o, com.eco.bio7.plot.PlotData d) {
					return PythonShellPlots.code(d, o);
				}
			}, com.eco.bio7.plot.PlotData.of(sel));
		});
		new MenuItem(menu, SWT.SEPARATOR);
		item(menu, "Delete", this::delete);
		/* The scripts of the folder of the preferences (as in the R-Shell): created each time the menu opens */
		int fixed = menu.getItemCount();
		menu.addListener(SWT.Show, _ -> {
			MenuItem[] items = menu.getItems();
			for (int i = fixed; i < items.length; i++) {
				items[i].dispose();
			}
			File folder = new File(Bio7Plugin.getDefault().getPreferenceStore().getString(PreferenceConstants.D_PYTHON_SHELL_SCRIPTS));
			if (PythonShellScripts.hasScripts(folder)) {
				new MenuItem(menu, SWT.SEPARATOR);
				PythonShellScripts.createMenus(menu, folder, this::runScript);
			}
		});
		return menu;
	}

	/*
	 * Runs a script of the menu: the names of the selected objects are in the
	 * list bio7_shell_vars (Python scripts), the other script types run with their
	 * interpreter.
	 */
	private void runScript(File file) {
		if (!file.getName().endsWith(".py")) {
			PythonShellScripts.run(file);
			return;
		}
		List<PyValue> sel = new ArrayList<>();
		for (String n : selectedNames()) {
			sel.add(PyValue.of(n));
		}
		async(() -> {
			python().assign("bio7_shell_vars", new PyValue.PyList(sel));
			EmbeddedPython.runFile(file.getAbsolutePath());
			EmbeddedPython.prompt();
		});
	}

	private static void item(Menu menu, String text, Runnable action) {
		MenuItem item = new MenuItem(menu, SWT.PUSH);
		item.setText(text);
		item.addListener(SWT.Selection, _ -> action.run());
	}

	private static Menu submenu(Menu menu, String text) {
		MenuItem item = new MenuItem(menu, SWT.CASCADE);
		item.setText(text);
		Menu sub = new Menu(menu);
		item.setMenu(sub);
		return sub;
	}

	/* ---------------- Transfers ---------------- */

	/* The object into a new sheet of the Bio7 table */
	private void toTable(String mode) {
		String name = selectedName();
		if (name == null) {
			return;
		}
		async(() -> {
			List<String[]> rows = tableRows(python(), name, mode);
			Display.getDefault().asyncExec(() -> fillTable(name, rows));
		});
	}

	/*
	 * Several selected objects (columns, rows: one each) or the elements of the
	 * selected list (list_columns, list_rows) into a new sheet, the names as header
	 */
	private void toTableMany(String mode) {
		List<String> sel = selectedNames();
		if (sel.isEmpty()) {
			MessageDialog.openInformation(getSite().getShell(), "Python-Shell", "Please select one or more objects in the list.");
			return;
		}
		boolean list = mode.startsWith("list");
		String arg = list ? sel.get(0) : String.join("\n", sel);
		String sheet = list ? sel.get(0) : com.eco.bio7.rbridge.TableTransfer.sheetName(sel);
		async(() -> {
			PyValue v = call("cells_many", arg, mode);
			List<String[]> rows = new ArrayList<>();
			if (v instanceof PyValue.PyList l) {
				for (PyValue r : l.values()) {
					rows.add(r instanceof PyValue.Str st ? st.values() : new String[] { text(r) });
				}
			}
			Display.getDefault().asyncExec(() -> fillTable(sheet, rows));
		});
	}

	/** Shows the rows in a new sheet of the Bio7 table (UI thread). */
	public static void fillTable(String name, List<String[]> rows) {
		String error = com.eco.bio7.rbridge.TableTransfer.show(name, rows);
		if (error != null) {
			EmbeddedPython.print(error + "\n", true);
		}
	}

	private void toR() {
		List<String> s = selectedNames();
		if (s.isEmpty()) {
			return;
		}
		async(() -> {
			for (String name : s) {
				PythonUtil.pythonToR(name, name);
				print("Transferred to R: " + name + "\n");
			}
		});
	}

	private void toImageJ() {
		String name = selectedName();
		if (name != null) {
			async(() -> PythonUtil.imageFromPython(name, name));
		}
	}

	/* The objects of the workspace and the columns of the DataFrames (df['col']) for the plot view */
	private List<String> plotWorkspaceVariables() {
		List<String> list = new ArrayList<>();
		try {
			PyValue v = call("plot_variables");
			if (v instanceof PyValue.Str s) {
				list.addAll(List.of(s.values()));
			} else if (v instanceof PyValue.PyList l) {
				for (PyValue e : l.values()) {
					if (e instanceof PyValue.Str s) {
						list.add(s.asString());
					}
				}
			}
		} catch (Exception e) {
			/* Python busy: the names of the list */
		}
		if (list.isEmpty()) {
			list.addAll(names);
		}
		return list;
	}

	/* Plots the selected objects (matplotlib, the plot view) */
	private void plotSelected(com.eco.bio7.plot.PlotOptions o) {
		List<String> sel = selectedNames();
		if (sel.isEmpty()) {
			MessageDialog.openInformation(getSite().getShell(), "Python-Shell", "Please select one or more objects in the list.");
			return;
		}
		plotObjects(sel, o);
	}

	/* The source of the plot in __main__ (output and errors in the shell); the function of the source is removed */
	private void plotObjects(List<String> sel, com.eco.bio7.plot.PlotOptions o) {
		runShow(PythonShellPlots.code(sel, o) + "del bio7_plot\n");
	}

	private void plot(String kind) {
		String name = selectedName();
		if (name == null) {
			return;
		}
		String code = switch (kind) {
		case "hist" -> "import matplotlib.pyplot as _plt, numpy as _np\n_plt.figure()\n_plt.hist(_np.asarray(" + name
				+ ", dtype=float).ravel()[~_np.isnan(_np.asarray(" + name + ", dtype=float).ravel())], bins=30)\n_plt.title('" + name + "')\n_plt.show()\ndel _plt, _np";
		case "image" -> "import matplotlib.pyplot as _plt\n_plt.figure()\n_plt.imshow(" + name + ", cmap='gray')\n_plt.colorbar()\n_plt.title('" + name + "')\n_plt.show()\ndel _plt";
		default -> "import matplotlib.pyplot as _plt\n_v = " + name + "\nif hasattr(_v, 'plot'):\n    _v.plot()\nelse:\n    _plt.figure()\n    _plt.plot(_v)\n_plt.title('"
				+ name + "')\n_plt.show()\ndel _plt, _v";
		};
		runShow(code);
	}

	/* ---------------- Load, save, documentation ---------------- */

	private void load() {
		FileDialog d = new FileDialog(getSite().getShell(), SWT.OPEN);
		d.setFilterExtensions(new String[] { "*.npy;*.npz;*.csv;*.tsv;*.txt;*.xlsx;*.pkl;*.py", "*" });
		String file = d.open();
		if (file == null) {
			return;
		}
		String lower = file.toLowerCase(Locale.ROOT);
		String base = new java.io.File(file).getName().replaceAll("\\.[^.]*$", "");
		String name = PythonFromSpreadAction.pythonName(base);
		String path = file.replace("\\", "\\\\").replace("'", "\\'");
		String code;
		if (lower.endsWith(".py")) {
			EmbeddedPython.runFile(file);
			print("Running " + file + " (output in the console)\n");
			async(() -> {
				Thread.sleep(500);
				refreshNow();
			});
			return;
		} else if (lower.endsWith(".npy")) {
			code = "import numpy as _np\n" + name + " = _np.load('" + path + "')\ndel _np\n" + name + ".shape";
		} else if (lower.endsWith(".npz")) {
			code = "import numpy as _np\n" + name + " = dict(_np.load('" + path + "'))\ndel _np\nlist(" + name + ")";
		} else if (lower.endsWith(".xlsx")) {
			code = "import pandas as _pd\n" + name + " = _pd.read_excel('" + path + "')\ndel _pd\n" + name + ".shape";
		} else if (lower.endsWith(".pkl")) {
			code = "import pickle as _p\nwith open('" + path + "', 'rb') as _f:\n    " + name + " = _p.load(_f)\ndel _p, _f\ntype(" + name + ").__name__";
		} else {
			code = "import pandas as _pd\n" + name + " = _pd.read_csv('" + path + "', sep=None, engine='python')\ndel _pd\n" + name + ".shape";
		}
		print(">>> load " + file + " as " + name + "\n");
		runShow(code);
	}

	private void save() {
		List<String> s = selectedNames();
		FileDialog d = new FileDialog(getSite().getShell(), SWT.SAVE);
		d.setOverwrite(true);
		if (s.isEmpty()) {
			/* The history as Python script */
			d.setFileName("history.py");
			String file = d.open();
			if (file != null) {
				try {
					java.nio.file.Files.writeString(java.nio.file.Path.of(file), String.join("\n", history) + "\n");
					print("Saved the history: " + file + "\n");
				} catch (IOException e) {
					print(e.getMessage() + "\n");
				}
			}
			return;
		}
		String name = s.get(0);
		d.setFileName(name + ".npy");
		d.setFilterExtensions(new String[] { "*.npy", "*.csv", "*.xlsx", "*.pkl" });
		String file = d.open();
		if (file == null) {
			return;
		}
		String lower = file.toLowerCase(Locale.ROOT);
		String path = file.replace("\\", "\\\\").replace("'", "\\'");
		String code;
		if (lower.endsWith(".csv")) {
			code = "import pandas as _pd\n(" + name + " if isinstance(" + name + ", _pd.DataFrame) else _pd.DataFrame(" + name + ")).to_csv('" + path + "', index=False)\ndel _pd";
		} else if (lower.endsWith(".xlsx")) {
			code = "import pandas as _pd\n(" + name + " if isinstance(" + name + ", _pd.DataFrame) else _pd.DataFrame(" + name + ")).to_excel('" + path + "', index=False)\ndel _pd";
		} else if (lower.endsWith(".pkl")) {
			code = "import pickle as _p\nwith open('" + path + "', 'wb') as _f:\n    _p.dump(" + name + ", _f)\ndel _p, _f";
		} else {
			code = "import numpy as _np\n_np.save('" + path + "', _np.asarray(" + name + "))\ndel _np";
		}
		runShow(code + "\nprint('Saved: " + path + "')");
	}

	private void doc() {
		List<String> s = selectedNames();
		String name = !s.isEmpty() ? s.get(0) : input.getText().strip();
		if (name.isEmpty()) {
			MessageDialog.openInformation(getSite().getShell(), "Python-Shell", "Select an object or enter a name (e.g., numpy.mean) in the text field.");
			return;
		}
		async(() -> setOutput(text(call("doc", name))));
	}

	private void about() {
		setOutput("""
				Python-Shell (embedded Python of Bio7)

				Text field: Python code, Enter evaluates (the value of an expression is shown),
				Up/Down: the history. The workspace is shared with the Python console and the scripts.

				List: the objects (select: description, double click: show).
				Menu of the list: Show, Summary, Filter, Convert, Transfer to Table,
				To R, To ImageJ, Plot, Delete.

				Buttons: Eval, Load (npy, npz, csv, tsv, xlsx, pkl, run py), Save (the selected
				object or the history), Py Doc (docs.python.org), ? (the help of the selected object
				or of a name, e.g., numpy.mean), the cross (interrupt), List, Del, Gc, Clear, Font.
				""");
	}

	/* ---------------- Font ---------------- */

	/* The font of the Python-Shell, else the font of the R-Shell (the same look) */
	private Font loadFont() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		if (store.contains(FONT_PREFERENCE) && !store.getString(FONT_PREFERENCE).isEmpty()) {
			return new Font(Display.getDefault(), PreferenceConverter.getFontDataArray(store, FONT_PREFERENCE));
		}
		if (store.contains("RShellFonts") || !store.getDefaultString("RShellFonts").isEmpty()) {
			return new Font(Display.getDefault(), PreferenceConverter.getFontData(store, "RShellFonts"));
		}
		return null;
	}

	private void chooseFont() {
		com.eco.bio7.util.ThemedFontDialog d = new com.eco.bio7.util.ThemedFontDialog(getSite().getShell());
		if (font != null) {
			d.setFontList(font.getFontData());
		}
		FontData data = d.open();
		if (data == null) {
			return;
		}
		PreferenceConverter.setValue(Bio7Plugin.getDefault().getPreferenceStore(), FONT_PREFERENCE, new FontData[] { data });
		applyFont(new Font(Display.getDefault(), data));
	}

	private void applyFont(Font f) {
		Font old = font;
		font = f;
		Font use = f != null ? f : JFaceResources.getTextFont();
		objectList.setFont(use);
		output.setFont(use);
		/* The height of the input from the font (as in the R-Shell) */
		com.eco.bio7.util.Util.updateTextHeight(input, use, input.getParent());
		if (old != null) {
			old.dispose();
		}
	}

	/* ---------------- Opening ---------------- */

	/**
	 * Shows the view as a tab beside the R-Shell view (if the perspective shows
	 * the R-Shell), else where the workbench places it. UI thread.
	 */
	public static void show() {
		PythonViews.showBeside(ID, R_SHELL, false);
	}
}
