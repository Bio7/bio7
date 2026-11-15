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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.console.PythonFinder;
import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.pyffm.PyValue;

/**
 * The packages of the Python of Bio7 (used by the Python Packages view and
 * dialog): the Python and its environment, the tabs "Install Packages",
 * "Installed Packages" and "Imported Modules" and the output of pip. pip runs in
 * a separate process, never in the embedded Python.
 */
public class PythonPackagesPanel {

	/* The bundles of packages for Bio7 */
	static final Map<String, String> BUNDLES = new LinkedHashMap<>();
	static {
		BUNDLES.put("Scientific", "numpy scipy pandas matplotlib");
		BUNDLES.put("Images", "scikit-image tifffile imageio");
		BUNDLES.put("StarDist", "stardist");
		BUNDLES.put("Cellpose", "cellpose");
		/* The code analysis of the Python editor: completion (jedi), rules, quick fixes and formatting (ruff) */
		BUNDLES.put("Editor tools", "jedi ruff");
	}

	private final Composite root;
	private CTabFolder tabFolder;
	private String executable;
	private PythonEnvironment.Info info;
	private List<PythonEnvironment.Package> packages = new ArrayList<>();
	private Map<String, String> outdated = new HashMap<>();

	private Label pythonLabel;
	private Label warningLabel;
	private Text filter;
	private Table installedTable;
	private Table modulesTable;
	private Button stdlibButton;
	private Text installText;
	private Label pypiLabel;
	private Text log;
	private Button cancelButton;
	private final List<Control> actions = new ArrayList<>();
	private final Process[] running = new Process[1];
	private volatile boolean busy;

	public PythonPackagesPanel(Composite parent) {
		root = new Composite(parent, SWT.NONE);
		root.setLayout(new GridLayout(1, false));

		SashForm sash = new SashForm(root, SWT.VERTICAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		CTabFolder tabs = new CTabFolder(sash, SWT.BORDER);
		tabFolder = tabs;
		createInstallTab(tabs);
		createInstalledTab(tabs);
		createModulesTab(tabs);
		tabs.setSelection(1);
		tabs.addListener(SWT.Selection, _ -> {
			if (tabs.getSelectionIndex() == 2) {
				refreshModules();
			}
		});

		/* The output of pip */
		Composite out = new Composite(sash, SWT.NONE);
		GridLayout ol = new GridLayout(2, false);
		ol.marginWidth = 0;
		out.setLayout(ol);
		Label outLabel = new Label(out, SWT.NONE);
		outLabel.setText("Output:");
		outLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		cancelButton = button(out, "Cancel", "Cancels the running pip", () -> {
			Process p = running[0];
			if (p != null && p.isAlive()) {
				p.destroy();
				appendLog("Canceled.");
			}
		});
		cancelButton.setEnabled(false);
		log = new Text(out, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
		log.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		log.setFont(org.eclipse.jface.resource.JFaceResources.getTextFont());
		sash.setWeights(70, 30);

		/* The Python and its environment (below the tabs, as in the R packages view) */
		Composite head = new Composite(root, SWT.NONE);
		head.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));
		GridLayout hl = new GridLayout(2, false);
		hl.marginWidth = hl.marginHeight = 0;
		head.setLayout(hl);
		pythonLabel = new Label(head, SWT.WRAP);
		GridData pl = new GridData(SWT.FILL, SWT.CENTER, true, false);
		pl.widthHint = 200;
		pythonLabel.setLayoutData(pl);
		pythonLabel.setText("Python: ...");
		action(button(head, "Create Bio7 Environment...", "Creates a virtual environment (venv) for Bio7 and selects it in the preferences", this::createEnvironment));
		warningLabel = new Label(head, SWT.WRAP);
		GridData wd = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		wd.widthHint = 200;
		warningLabel.setLayoutData(wd);


		root.addListener(SWT.Dispose, _ -> {
			Process p = running[0];
			if (p != null && p.isAlive()) {
				p.destroy();
			}
		});
		executable = PythonEnvironment.executable();
		Display.getCurrent().asyncExec(() -> refresh(false));
	}

	/** The control of the panel. */
	public Composite getControl() {
		return root;
	}

	/** Shows the tab "Install Packages" with a package (e.g., a quick fix of the Python editor). */
	public void prepareInstall(String pkg) {
		if (!isOpen()) {
			return;
		}
		tabFolder.setSelection(0);
		installText.setText(pkg);
		installText.setFocus();
		pypiInfo();
	}

	/* ---------------- The tabs ---------------- */

	private void createInstallTab(CTabFolder tabs) {
		CTabItem tab = new CTabItem(tabs, SWT.NONE);
		tab.setText("Install Packages");
		Composite c = new Composite(tabs, SWT.NONE);
		tab.setControl(c);
		c.setLayout(new GridLayout(3, false));
		new Label(c, SWT.NONE).setText("Packages:");
		installText = new Text(c, SWT.BORDER);
		installText.setMessage("e.g.: numpy pandas \"scikit-image>=0.22\"");
		installText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		installText.addListener(SWT.DefaultSelection, _ -> install(installText.getText()));
		action(button(c, "Info", "Shows the description and the latest version of the first package (PyPI)", this::pypiInfo));
		pypiLabel = new Label(c, SWT.WRAP);
		GridData pd = new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1);
		pd.widthHint = 200;
		/* Shown with the information of PyPI */
		pd.exclude = true;
		pypiLabel.setLayoutData(pd);
		pypiLabel.setVisible(false);
		Button install = action(button(c, "Install", "Installs the packages with pip", () -> install(installText.getText())));
		install.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

		Label bundlesLabel = new Label(c, SWT.NONE);
		bundlesLabel.setText("Bundles for Bio7:");
		bundlesLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		Composite bundles = new Composite(c, SWT.NONE);
		bundles.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		GridLayout bl = new GridLayout(2, true);
		bl.marginWidth = bl.marginHeight = 0;
		bundles.setLayout(bl);
		for (Map.Entry<String, String> b : BUNDLES.entrySet()) {
			Button bb = action(button(bundles, b.getKey(), "Installs: " + b.getValue(), () -> install(b.getValue())));
			bb.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		}
		Button req = action(button(c, "Install from requirements.txt...", "Installs the packages of a requirements.txt", this::importRequirements));
		req.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		Button all = action(button(c, "Update Installed Packages", "Upgrades all packages with a newer version (PyPI)", this::upgradeAll));
		all.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
	}

	private void createInstalledTab(CTabFolder tabs) {
		CTabItem tab = new CTabItem(tabs, SWT.NONE);
		tab.setText("Installed Packages");
		Composite c = new Composite(tabs, SWT.NONE);
		tab.setControl(c);
		c.setLayout(new GridLayout(2, true));
		filter = new Text(c, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		filter.setMessage("Filter");
		filter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		filter.addListener(SWT.Modify, _ -> fillInstalled());
		installedTable = table(c, new String[][] { { "Package", "160" }, { "Version", "90" }, { "Latest Version", "90" } });
		installedTable.addListener(SWT.DefaultSelection, _ -> addImportsToEditor());
		gridButton(c, "Refresh Packages List", "Reads the installed packages again", () -> refresh(false));
		gridButton(c, "Check Updates", "Asks PyPI for newer versions (internet)", () -> refresh(true));
		gridButton(c, "Update Selected", "Upgrades the selected packages", () -> selectedPip("install", "--upgrade"));
		gridButton(c, "Remove Selected", "Uninstalls the selected packages", this::uninstall);
		gridButton(c, "Add Selected To Editor", "Adds the selected packages as import statements to the Python editor (double click)", this::addImportsToEditor);
		gridButton(c, "Export requirements.txt...", "Saves the installed packages as requirements.txt", this::export);
	}

	private void createModulesTab(CTabFolder tabs) {
		CTabItem tab = new CTabItem(tabs, SWT.NONE);
		tab.setText("Imported Modules");
		tab.setToolTipText("The modules imported in the embedded Python of Bio7");
		Composite c = new Composite(tabs, SWT.NONE);
		tab.setControl(c);
		c.setLayout(new GridLayout(2, true));
		stdlibButton = new Button(c, SWT.CHECK);
		stdlibButton.setText("Show the standard library");
		stdlibButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		stdlibButton.addListener(SWT.Selection, _ -> refreshModules());
		modulesTable = table(c, new String[][] { { "Module", "150" }, { "Version", "80" }, { "File", "250" } });
		modulesTable.addListener(SWT.DefaultSelection, _ -> listFunctions());
		gridButton(c, "Refresh", "Lists the imported modules again", this::refreshModules);
		gridButton(c, "List Functions", "Lists the functions and classes of the selected module (double click)", this::listFunctions);
		gridButton(c, "Reload Selected", "Reloads the selected modules (importlib.reload, e.g., after a change of an own module)", this::reloadModules);
		new Label(c, SWT.NONE);
	}

	private static Table table(Composite parent, String[][] columns) {
		Table t = new Table(parent, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		t.setHeaderVisible(true);
		t.setLinesVisible(true);
		t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		for (String[] col : columns) {
			TableColumn tc = new TableColumn(t, SWT.LEFT);
			tc.setText(col[0]);
			tc.setWidth(Integer.parseInt(col[1]));
		}
		return t;
	}

	private void gridButton(Composite parent, String text, String tip, Runnable r) {
		Button b = action(button(parent, text, tip, r));
		b.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	private <T extends Control> T action(T c) {
		actions.add(c);
		return c;
	}

	private static Button button(Composite parent, String text, String tip, Runnable r) {
		Button b = new Button(parent, SWT.PUSH);
		b.setText(text);
		b.setToolTipText(tip);
		b.addListener(SWT.Selection, _ -> r.run());
		return b;
	}

	private Shell shell() {
		return root.getShell();
	}

	private boolean isOpen() {
		return log != null && !log.isDisposed();
	}

	private void appendLog(String line) {
		Display.getDefault().asyncExec(() -> {
			if (isOpen()) {
				log.append(line + "\n");
			}
		});
	}

	private void setBusy(boolean b) {
		busy = b;
		Display.getDefault().asyncExec(() -> {
			if (!isOpen()) {
				return;
			}
			for (Control c : actions) {
				c.setEnabled(!b);
			}
			cancelButton.setEnabled(b);
			root.setCursor(b ? root.getDisplay().getSystemCursor(SWT.CURSOR_APPSTARTING) : null);
		});
	}

	/* Runs a task in a thread (one at a time) */
	private void task(String name, ThrowingRunnable r) {
		if (busy) {
			return;
		}
		setBusy(true);
		Thread t = new Thread(() -> {
			try {
				r.run();
			} catch (Exception e) {
				appendLog(name + ": " + e.getMessage());
			} finally {
				running[0] = null;
				setBusy(false);
			}
		}, "Python packages");
		t.setDaemon(true);
		t.start();
	}

	@FunctionalInterface
	private interface ThrowingRunnable {
		void run() throws Exception;
	}

	/* ---------------- Installed packages ---------------- */

	/** Reads the installed packages again (with the updates of PyPI if selected). */
	public void refresh(boolean withUpdates) {
		if (executable == null) {
			pythonLabel.setText("Python: not found");
			warningLabel.setText("No Python 3 was found. Select it in the Python preferences ('Detect Python...') or create a Bio7 environment.");
			return;
		}
		String exe = executable;
		task("Python", () -> {
			PythonEnvironment.Info i = PythonEnvironment.info(exe);
			List<PythonEnvironment.Package> list = i.pip() == null ? List.of() : PythonEnvironment.list(exe);
			Map<String, String> updates = new HashMap<>();
			if (withUpdates && i.pip() != null) {
				appendLog("Checking for updates (PyPI) ...");
				updates = PythonEnvironment.outdated(exe);
				appendLog(updates.isEmpty() ? "All packages are up to date." : updates.size() + " packages can be upgraded.");
			}
			Map<String, String> u = updates;
			Display.getDefault().asyncExec(() -> {
				if (!isOpen()) {
					return;
				}
				info = i;
				packages = list;
				if (withUpdates) {
					outdated = new HashMap<>();
					u.forEach((k, v) -> outdated.put(k.toLowerCase(Locale.ROOT), v));
				}
				showInfo();
				fillInstalled();
			});
		});
	}

	private void showInfo() {
		String kind = switch (info.kind()) {
		case "venv" -> "virtual environment";
		case "conda" -> "conda environment";
		default -> "system Python";
		};
		pythonLabel.setText("Python " + info.version() + " (" + kind + (EmbeddedPython.isRunning() ? ", running in Bio7" : "") + "): " + info.executable());
		String warning = "";
		if (info.pip() == null) {
			warning = "pip is not available for this Python. Create a Bio7 environment (it includes pip).";
		} else if (info.externallyManaged()) {
			warning = "The packages of this Python are managed by the system (e.g., Homebrew or Linux): pip can't install into it. "
					+ "Create a Bio7 environment (a virtual environment with this or another Python).";
		} else if (info.kind().equals("conda")) {
			warning = "A conda environment: pip installs the packages which conda doesn't provide.";
		}
		warningLabel.setText(warning);
		warningLabel.setForeground(info.canInstall() ? null : root.getDisplay().getSystemColor(SWT.COLOR_RED));
		root.layout(true, true);
	}

	private void fillInstalled() {
		if (!isOpen()) {
			return;
		}
		String f = filter.getText().trim().toLowerCase(Locale.ROOT);
		installedTable.removeAll();
		for (PythonEnvironment.Package p : packages) {
			if (!f.isEmpty() && !p.name().toLowerCase(Locale.ROOT).contains(f)) {
				continue;
			}
			TableItem item = new TableItem(installedTable, SWT.NONE);
			String latest = outdated.get(p.name().toLowerCase(Locale.ROOT));
			item.setText(new String[] { p.name(), p.version(), latest == null ? "" : latest });
			item.setData(p.name());
		}
	}

	private List<String> selected(Table t) {
		List<String> names = new ArrayList<>();
		for (TableItem item : t.getSelection()) {
			names.add((String) item.getData());
		}
		return names;
	}

	/* ---------------- pip ---------------- */

	private boolean checkInstallable() {
		if (info == null) {
			return false;
		}
		if (!info.canInstall()) {
			if (MessageDialog.openQuestion(shell(), "Python Packages", warningLabel.getText() + "\n\nCreate a Bio7 environment now?")) {
				createEnvironment();
			}
			return false;
		}
		return true;
	}

	private void install(String text) {
		List<String> args = PythonEnvironment.packageArgs(text);
		if (args.isEmpty() || !checkInstallable()) {
			return;
		}
		List<String> pip = new ArrayList<>(List.of("install"));
		pip.addAll(args);
		runPip(pip, args);
	}

	private void selectedPip(String command, String option) {
		List<String> names = selected(installedTable);
		if (names.isEmpty()) {
			MessageDialog.openInformation(shell(), "Python Packages", "Please select packages in the list.");
			return;
		}
		if (!checkInstallable()) {
			return;
		}
		List<String> pip = new ArrayList<>(List.of(command, option));
		pip.addAll(names);
		runPip(pip, names);
	}

	private void uninstall() {
		List<String> names = selected(installedTable);
		if (names.isEmpty()) {
			MessageDialog.openInformation(shell(), "Python Packages", "Please select packages in the list.");
			return;
		}
		if (!checkInstallable() || !MessageDialog.openConfirm(shell(), "Python Packages", "Uninstall " + String.join(", ", names) + "?")) {
			return;
		}
		List<String> pip = new ArrayList<>(List.of("uninstall", "-y"));
		pip.addAll(names);
		runPip(pip, names);
	}

	/* Upgrades all outdated packages (asks PyPI first) */
	private void upgradeAll() {
		if (!checkInstallable()) {
			return;
		}
		String exe = executable;
		task("Update", () -> {
			appendLog("Checking for updates (PyPI) ...");
			Map<String, String> updates = PythonEnvironment.outdated(exe);
			updates.keySet().removeIf(n -> n.equalsIgnoreCase("pip") || n.equalsIgnoreCase("setuptools"));
			if (updates.isEmpty()) {
				appendLog("All packages are up to date.");
				return;
			}
			boolean[] ok = new boolean[1];
			Display.getDefault().syncExec(() -> ok[0] = MessageDialog.openQuestion(shell(), "Python Packages",
					"Upgrade " + updates.size() + " packages?\n\n" + String.join(", ", updates.keySet())
							+ "\n\nNote: upgrades can break packages which need an older version of another package."));
			if (ok[0]) {
				List<String> pip = new ArrayList<>(List.of("install", "--upgrade"));
				pip.addAll(updates.keySet());
				pipNow(exe, pip, new ArrayList<>(updates.keySet()));
			}
		});
	}

	/* Runs pip; then the list again, the import caches of the embedded Python and a hint if loaded modules changed */
	private void runPip(List<String> args, List<String> changed) {
		String exe = executable;
		task("pip", () -> pipNow(exe, args, changed));
	}

	private void pipNow(String exe, List<String> args, List<String> changed) throws Exception {
		appendLog("\n$ python -m pip " + String.join(" ", args));
		PythonEnvironment.Result r = PythonEnvironment.pip(exe, args, this::appendLog, running);
		appendLog(r.exit() == 0 ? "Done." : "pip ended with an error (" + r.exit() + ").");
		boolean modified = r.output().contains("Successfully installed") || r.output().contains("Successfully uninstalled");
		if (modified) {
			/* The code analysis of the Python editor finds the new tools and modules */
			try {
				com.eco.bio7.pythonedit.analysis.AnalysisClient.get().stop();
			} catch (LinkageError e) {
				/* No Python editor */
			}
		}
		List<String> loaded = modified ? afterChange(changed) : List.of();
		List<PythonEnvironment.Package> list = PythonEnvironment.list(exe);
		Display.getDefault().asyncExec(() -> {
			if (!isOpen()) {
				return;
			}
			packages = list;
			fillInstalled();
			if (!loaded.isEmpty()) {
				MessageDialog.openInformation(shell(), "Python Packages",
						"The changed packages " + loaded + " are already loaded in the running Python of Bio7.\nPlease restart Bio7 to use the new versions.");
			}
		});
	}

	/* The embedded Python finds the new packages (import caches); returns the changed packages already imported */
	private List<String> afterChange(List<String> changed) {
		PySession s = EmbeddedPython.session();
		List<String> loaded = new ArrayList<>();
		if (s == null || !s.executable().toString().equals(executable)) {
			return loaded;
		}
		try {
			s.exec("import importlib as _il; _il.invalidate_caches(); del _il");
			PyValue v = s.eval("sorted({m.split('.')[0].lower() for m in list(__import__('sys').modules)})");
			if (v instanceof PyValue.Str names) {
				java.util.Set<String> modules = new java.util.HashSet<>(java.util.Arrays.asList(names.values()));
				for (String p : changed) {
					String base = p.split("[<>=!~\\[; ]", 2)[0].toLowerCase(Locale.ROOT).replace('-', '_');
					if (modules.contains(base) || modules.contains(base.replace("scikit_", "sk")) || modules.contains(base.replace("python_", ""))) {
						loaded.add(p);
					}
				}
			}
		} catch (Exception e) {
			/* Python busy: no hint */
		}
		return loaded;
	}

	private void pypiInfo() {
		List<String> args = PythonEnvironment.packageArgs(installText.getText());
		if (args.isEmpty()) {
			return;
		}
		String name = args.get(0);
		task("PyPI", () -> {
			String[] i = PythonEnvironment.pypiInfo(name);
			String text = i == null ? "'" + name + "' was not found on PyPI." : i[0] + " " + i[1] + ": " + i[2] + (i[3].isEmpty() ? "" : "  (Python " + i[3] + ")");
			Display.getDefault().asyncExec(() -> {
				if (isOpen()) {
					pypiLabel.setText(text);
					((GridData) pypiLabel.getLayoutData()).exclude = false;
					pypiLabel.setVisible(true);
					root.layout(true, true);
				}
			});
		});
	}

	/* The import statements of the selected packages in the active Python editor */
	private void addImportsToEditor() {
		List<String> names = selected(installedTable);
		if (names.isEmpty()) {
			return;
		}
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (!(editor instanceof ITextEditor te)) {
			MessageDialog.openInformation(shell(), "Python Packages", "Please open a Python editor.");
			return;
		}
		String exe = executable;
		task("Editor", () -> {
			/* The import names of the distributions (e.g., scikit-image: skimage) */
			StringBuilder code = new StringBuilder("import importlib.metadata as m\nfor n in " + pythonList(names) + ":\n");
			code.append("    try:\n        t = (m.distribution(n).read_text('top_level.txt') or '').split()\n    except Exception:\n        t = []\n");
			code.append("    t = [x for x in t if not x.startswith('_')] or [n.replace('-', '_').lower()]\n    print(t[0])\n");
			PythonEnvironment.Result r = PythonEnvironment.run(List.of(exe, "-c", code.toString()), null, running);
			StringBuilder imports = new StringBuilder();
			for (String line : r.output().strip().split("\\R")) {
				if (line.matches("[A-Za-z_][A-Za-z0-9_]*")) {
					imports.append("import ").append(line).append('\n');
				}
			}
			Display.getDefault().asyncExec(() -> {
				IDocument doc = te.getDocumentProvider().getDocument(te.getEditorInput());
				try {
					doc.replace(0, 0, imports.toString());
				} catch (BadLocationException e) {
					appendLog(e.getMessage());
				}
			});
		});
	}

	private static String pythonList(List<String> names) {
		StringBuilder b = new StringBuilder("[");
		for (String n : names) {
			b.append('\'').append(n.replace("\\", "").replace("'", "")).append("',");
		}
		return b.append(']').toString();
	}

	/* ---------------- requirements.txt ---------------- */

	private void export() {
		FileDialog d = new FileDialog(shell(), SWT.SAVE);
		d.setFileName("requirements.txt");
		d.setFilterExtensions(new String[] { "*.txt" });
		d.setOverwrite(true);
		String file = d.open();
		if (file == null) {
			return;
		}
		String exe = executable;
		task("Export", () -> {
			PythonEnvironment.Result r = PythonEnvironment.pip(exe, List.of("freeze"), null, running);
			if (r.exit() != 0) {
				throw new Exception(r.output().strip());
			}
			Files.writeString(Path.of(file), r.output());
			appendLog("Saved: " + file);
		});
	}

	private void importRequirements() {
		if (!checkInstallable()) {
			return;
		}
		FileDialog d = new FileDialog(shell(), SWT.OPEN);
		d.setFilterExtensions(new String[] { "*.txt", "*" });
		String file = d.open();
		if (file != null) {
			runPip(List.of("install", "-r", file), List.of());
		}
	}

	/* ---------------- Imported modules (embedded Python) ---------------- */

	private void refreshModules() {
		PySession s = EmbeddedPython.session();
		if (s == null) {
			modulesTable.removeAll();
			TableItem item = new TableItem(modulesTable, SWT.NONE);
			item.setText(new String[] { "(Python is not running)", "", "" });
			return;
		}
		boolean stdlib = stdlibButton.getSelection();
		task("Modules", () -> {
			PyValue v = s.eval("[[n, str(getattr(m, '__version__', '')), str(getattr(m, '__file__', '') or '')] for n, m in sorted(__import__('sys').modules.items())"
					+ " if '.' not in n and not n.startswith('_') and m is not None" + (stdlib ? "" : " and n not in getattr(__import__('sys'), 'stdlib_module_names', ()) and n not in __import__('sys').builtin_module_names and n != 'bio7'") + "]");
			List<String[]> rows = new ArrayList<>();
			if (v instanceof PyValue.PyList l) {
				for (PyValue e : l.values()) {
					if (e instanceof PyValue.Str st && st.length() == 3) {
						rows.add(st.values());
					}
				}
			}
			Display.getDefault().asyncExec(() -> {
				if (!isOpen()) {
					return;
				}
				modulesTable.removeAll();
				for (String[] r : rows) {
					TableItem item = new TableItem(modulesTable, SWT.NONE);
					item.setText(r);
					item.setData(r[0]);
				}
			});
		});
	}

	private void listFunctions() {
		List<String> names = selected(modulesTable);
		PySession s = EmbeddedPython.session();
		if (names.isEmpty() || s == null) {
			return;
		}
		String name = names.get(0);
		task("Functions", () -> {
			PyValue v = s.eval("[n + ('()' if callable(getattr(__import__('sys').modules['" + name + "'], n, None)) else '') for n in sorted(dir(__import__('sys').modules['" + name
					+ "'])) if not n.startswith('_')]");
			if (v instanceof PyValue.Str st) {
				appendLog("\nThe names of " + name + " (" + st.length() + "):");
				appendLog(String.join("  ", st.values()));
			}
		});
	}

	private void reloadModules() {
		List<String> names = selected(modulesTable);
		PySession s = EmbeddedPython.session();
		if (names.isEmpty() || s == null) {
			return;
		}
		task("Reload", () -> {
			for (String n : names) {
				try {
					s.exec("import importlib as _il, sys as _s\n_il.reload(_s.modules['" + n + "'])\ndel _il, _s");
					appendLog("Reloaded: " + n);
				} catch (Exception e) {
					appendLog(n + ": " + e.getMessage());
				}
			}
		});
	}

	/* ---------------- The Bio7 environment ---------------- */

	private void createEnvironment() {
		List<PythonFinder.Found> found = new ArrayList<>();
		org.eclipse.swt.custom.BusyIndicator.showWhile(root.getDisplay(), () -> found.addAll(PythonFinder.detect()));
		if (found.isEmpty()) {
			MessageDialog.openInformation(shell(), "Bio7 Environment", "No Python 3 was found. Please install Python 3 (e.g., from python.org).");
			return;
		}
		ListDialog choose = new ListDialog(shell());
		choose.setTitle("Bio7 Environment");
		choose.setMessage("The Python of the new environment (the packages are installed into the environment only):");
		choose.setContentProvider(ArrayContentProvider.getInstance());
		choose.setLabelProvider(new LabelProvider());
		choose.setInput(found);
		choose.setInitialSelections(found.get(0));
		choose.setWidthInChars(80);
		if (choose.open() != Window.OK || choose.getResult() == null || choose.getResult().length == 0) {
			return;
		}
		PythonFinder.Found base = (PythonFinder.Found) choose.getResult()[0];
		InputDialog folderDialog = new InputDialog(shell(), "Bio7 Environment", "The folder of the environment:", PythonEnvironment.DEFAULT_ENV,
				s -> s.isBlank() ? "A folder is needed" : null);
		if (folderDialog.open() != Window.OK) {
			return;
		}
		Path folder = Path.of(folderDialog.getValue().trim());
		boolean exists = Files.exists(PythonEnvironment.binFolder(folder));
		if (exists && !MessageDialog.openQuestion(shell(), "Bio7 Environment", "An environment exists in " + folder + ".\n\nUse it?")) {
			return;
		}
		boolean bundle = MessageDialog.openQuestion(shell(), "Bio7 Environment", "Install the scientific packages (" + BUNDLES.get("Scientific") + ") into the new environment?");
		task("Environment", () -> {
			String exe;
			if (exists) {
				exe = PythonFinder.executableIn(PythonEnvironment.binFolder(folder).toString());
				if (exe == null) {
					throw new Exception("No Python in " + PythonEnvironment.binFolder(folder));
				}
			} else {
				appendLog("\n$ " + base.executable() + " -m venv " + folder);
				Files.createDirectories(folder.getParent() == null ? folder : folder.getParent());
				exe = PythonEnvironment.createEnvironment(base.executable(), folder, this::appendLog, running);
				appendLog("Created: " + exe);
			}
			PythonEnvironment.select(PythonEnvironment.binFolder(folder));
			executable = exe;
			appendLog("Selected in the Python preferences: " + PythonEnvironment.binFolder(folder));
			if (bundle) {
				appendLog("\n$ python -m pip install " + BUNDLES.get("Scientific"));
				PythonEnvironment.Result r = PythonEnvironment.pip(exe, PythonEnvironment.packageArgs(BUNDLES.get("Scientific")), this::appendLog, running);
				appendLog(r.exit() == 0 ? "Done." : "pip ended with an error (" + r.exit() + ").");
			}
			Display.getDefault().asyncExec(() -> {
				if (EmbeddedPython.isRunning() && isOpen()) {
					MessageDialog.openInformation(shell(), "Bio7 Environment", "The new environment is selected. The running Python of Bio7 uses it after a restart of Bio7.");
				}
			});
		});
		/* The list of the new environment (after the task) */
		Thread t = new Thread(() -> {
			while (busy) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					return;
				}
			}
			Display.getDefault().asyncExec(() -> {
				if (isOpen()) {
					refresh(false);
				}
			});
		}, "Python packages refresh");
		t.setDaemon(true);
		t.start();
	}
}
