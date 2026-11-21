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
package com.eco.bio7.pybridge.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.Bio7Plugin;

/**
 * The view of the Python debugger (as the Debug view of R): the actions
 * (debug, stop, next, step into, step return, continue, run to cursor), the
 * variables of the stop, watch expressions and the call stack.
 */
public class PythonDebugView extends ViewPart {

	public static final String ID = "com.eco.bio7.pybridge.debug.PythonDebugView";

	private static final Set<PythonDebugView> OPEN = ConcurrentHashMap.newKeySet();
	/* The watch expressions (shared by the views, kept while Bio7 runs) */
	private static final List<String> watches = new ArrayList<>();

	private Label status;
	private Table variables;
	private Table watch;
	private Table stack;
	private Text watchInput;

	public PythonDebugView() {
		setPartName("Python Debug");
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		status = new Label(parent, SWT.NONE);
		status.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		CTabFolder folder = new CTabFolder(parent, SWT.TOP | SWT.FLAT);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		CTabItem varTab = new CTabItem(folder, SWT.NONE);
		varTab.setText("Variables");
		variables = table(folder, new String[] { "Name", "Type", "Size", "Value" }, new int[] { 130, 110, 70, 420 });
		varTab.setControl(variables);
		variables.setMenu(variableMenu());
		variables.addListener(SWT.DefaultSelection, _ -> showValue());

		CTabItem watchTab = new CTabItem(folder, SWT.NONE);
		watchTab.setText("Watch");
		Composite wc = new Composite(folder, SWT.NONE);
		wc.setLayout(new GridLayout(3, false));
		watchInput = new Text(wc, SWT.BORDER);
		watchInput.setMessage("Expression, e.g. x * 2 or df.shape");
		watchInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		watchInput.addListener(SWT.DefaultSelection, _ -> addWatch(watchInput.getText()));
		button(wc, "Add", () -> addWatch(watchInput.getText()));
		button(wc, "Remove", this::removeWatch);
		watch = table(wc, new String[] { "Expression", "Value" }, new int[] { 200, 480 });
		watch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		watchTab.setControl(wc);

		CTabItem stackTab = new CTabItem(folder, SWT.NONE);
		stackTab.setText("Call Stack");
		stack = table(folder, new String[] { "#", "Function", "File", "Line" }, new int[] { 40, 180, 200, 60 });
		stackTab.setControl(stack);
		stack.addListener(SWT.Selection, _ -> {
			if (stack.getSelectionCount() == 1 && PythonDebugger.isPaused()) {
				int index = Integer.parseInt(stack.getSelection()[0].getText(0));
				background(() -> {
					PythonDebugger.selectFrame(index);
					refreshData();
				});
			}
		});
		folder.setSelection(varTab);

		IToolBarManager tm = getViewSite().getActionBars().getToolBarManager();
		tm.add(action("Debug the script of the editor (to the first breakpoint, without breakpoints from the first line)", "/icons/rdebug/rundebug.svg",
				() -> PythonDebugger.startActiveEditor(false)));
		stopAction = action("Stop the debugging", "/icons/rdebug/terminatedlaunch_obj.svg", PythonDebugger::terminate);
		tm.add(stopAction);
		tm.add(new Separator());
		tm.add(action("Next (n) - the next line (over function calls)", "/icons/rdebug/stepover_co.svg", PythonDebugger::next));
		tm.add(action("Step Into (s) - into a function call", "/icons/rdebug/stepinto_co.svg", PythonDebugger::stepInto));
		tm.add(action("Step Return (r) - to the end of the function", "/icons/rdebug/stepreturn_co.svg", PythonDebugger::stepReturn));
		tm.add(action("Continue (c) - to the next breakpoint", "/icons/rdebug/stepbystep_co.svg", PythonDebugger::resume));
		tm.add(action("Run to Cursor - to the line of the cursor in the editor", "/icons/rdebug/runtoline_co.svg", this::runToCursor));
		getViewSite().getActionBars().updateActionBars();

		OPEN.add(this);
		parent.addListener(SWT.Dispose, _ -> OPEN.remove(this));
		refresh();
	}

	@Override
	public void setFocus() {
		variables.setFocus();
	}

	private static Table table(Composite parent, String[] names, int[] widths) {
		Table t = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		t.setHeaderVisible(true);
		t.setLinesVisible(true);
		for (int i = 0; i < names.length; i++) {
			TableColumn c = new TableColumn(t, SWT.LEFT);
			c.setText(names[i]);
			c.setWidth(widths[i]);
		}
		return t;
	}

	private static void button(Composite parent, String text, Runnable r) {
		Button b = new Button(parent, SWT.PUSH);
		b.setText(text);
		b.addListener(SWT.Selection, _ -> r.run());
	}

	/* The stop button (disabled without a debug session) */
	private Action stopAction;

	private static Action action(String tip, String icon, Runnable r) {
		Action a = new Action() {
			@Override
			public void run() {
				r.run();
			}
		};
		a.setToolTipText(tip);
		a.setText(tip);
		a.setImageDescriptor(Bio7Plugin.getImageDescriptor(icon));
		return a;
	}

	/* ---------------- Updates ---------------- */

	/** Updates the open views (after a stop, a step, the end). Any thread. */
	public static void refreshAll() {
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			for (PythonDebugView v : OPEN) {
				v.refresh();
			}
		});
	}

	private boolean isOpen() {
		return variables != null && !variables.isDisposed();
	}

	private void refresh() {
		if (!isOpen()) {
			return;
		}
		/* Stop only while a script is debugged (running or stopped) */
		stopAction.setEnabled(PythonDebugger.isRunning());
		PythonDebugger.Stop s = PythonDebugger.stop();
		if (s == null) {
			status.setText(PythonDebugger.isRunning() ? "Running ..." : "Not debugging. Set breakpoints with a double click on the ruler of the Python editor.");
			variables.removeAll();
			stack.removeAll();
			for (TableItem item : watch.getItems()) {
				item.setText(1, "");
			}
			fillWatch(null);
			return;
		}
		status.setText("Stopped in " + s.function() + " (line " + s.line() + ")" + (s.message() != null ? ": " + s.message() : ""));
		stack.removeAll();
		for (int i = s.stack().size() - 1; i >= 0; i--) {
			String[] r = s.stack().get(i);
			TableItem item = new TableItem(stack, SWT.NONE);
			item.setText(new String[] { r[0], r[1], r[2], r[3] });
		}
		background(this::refreshData);
	}

	/* The variables and the watch values (queries in the thread of Python) */
	private void refreshData() throws Exception {
		List<String[]> vars = PythonDebugger.variables();
		List<String> values = new ArrayList<>();
		for (String w : List.copyOf(watches)) {
			values.add(PythonDebugger.evaluate(w));
		}
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			if (!isOpen()) {
				return;
			}
			variables.removeAll();
			for (String[] r : vars) {
				new TableItem(variables, SWT.NONE).setText(r);
			}
			fillWatch(values);
		});
	}

	private void fillWatch(List<String> values) {
		watch.removeAll();
		for (int i = 0; i < watches.size(); i++) {
			new TableItem(watch, SWT.NONE).setText(new String[] { watches.get(i), values == null || i >= values.size() ? "" : values.get(i) });
		}
	}

	private interface Task {
		void run() throws Exception;
	}

	private static void background(Task t) {
		Thread thread = new Thread(() -> {
			try {
				t.run();
			} catch (Exception e) {
				/* Not stopped (any more) */
			}
		}, "Python debug view");
		thread.setDaemon(true);
		thread.start();
	}

	/* ---------------- Actions ---------------- */

	private void addWatch(String expression) {
		String e = expression.strip();
		if (e.isEmpty() || watches.contains(e)) {
			return;
		}
		watches.add(e);
		watchInput.setText("");
		for (PythonDebugView v : OPEN) {
			v.refresh();
		}
	}

	private void removeWatch() {
		for (TableItem item : watch.getSelection()) {
			watches.remove(item.getText(0));
		}
		refresh();
	}

	private void runToCursor() {
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor instanceof ITextEditor te && te.getSelectionProvider().getSelection() instanceof ITextSelection sel) {
			/* The file of the editor (also a module of the script, e.g., a class in another file) */
			org.eclipse.core.resources.IFile file = editor.getEditorInput().getAdapter(org.eclipse.core.resources.IFile.class);
			String path = file != null && file.getLocation() != null ? file.getLocation().toOSString() : null;
			PythonDebugger.runToLine(path, sel.getStartLine() + 1);
		}
	}

	private String selectedName() {
		return variables.getSelectionCount() == 0 ? null : variables.getSelection()[0].getText(0);
	}

	private Menu variableMenu() {
		Menu menu = new Menu(variables);
		item(menu, "Show Value", this::showValue);
		item(menu, "Add to Watch", () -> {
			String n = selectedName();
			if (n != null) {
				addWatch(n);
			}
		});
		item(menu, "Copy to Python Workspace", () -> {
			String n = selectedName();
			if (n != null) {
				background(() -> PythonDebugger.toWorkspace(n));
			}
		});
		item(menu, "Copy Value", () -> {
			if (variables.getSelectionCount() > 0) {
				Clipboard cb = new Clipboard(variables.getDisplay());
				cb.setContents(new Object[] { variables.getSelection()[0].getText(3) }, new Transfer[] { TextTransfer.getInstance() });
				cb.dispose();
			}
		});
		return menu;
	}

	private static void item(Menu menu, String text, Runnable r) {
		MenuItem i = new MenuItem(menu, SWT.PUSH);
		i.setText(text);
		i.addListener(SWT.Selection, _ -> r.run());
	}

	/* The complete value of a variable */
	private void showValue() {
		String n = selectedName();
		if (n == null || !PythonDebugger.isPaused()) {
			return;
		}
		background(() -> {
			String value = PythonDebugger.console(n);
			PlatformUI.getWorkbench().getDisplay().asyncExec(() -> MessageDialog.openInformation(getSite().getShell(), n, value.length() > 20000 ? value.substring(0, 20000) + " ..." : value));
		});
	}
}
