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
package com.eco.bio7.rbridge.debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridEditor;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.util.Util;

/**
 * The state of R at a stop of the debugger in three tabs:
 * <ul>
 * <li>Variables: name, class, dimension (or length), size and a short value; the
 * values which changed since the last stop are highlighted. A double-click opens
 * the variable in a table (the context menu: a numeric matrix as an image), the
 * value can be edited (embedded R: the frame of R).</li>
 * <li>Watch: expressions evaluated at every stop.</li>
 * <li>Call Stack: the calls; a click shows the variables of the frame and its
 * line.</li>
 * </ul>
 */
public class DebugVariablesView extends ViewPart {

	public static final String ID = "com.eco.bio7.rbridge.debug.DebugVariablesView"; //$NON-NLS-1$

	private static DebugVariablesView instance;
	private static Grid grid;

	private Grid watchGrid;
	private Grid stackGrid;
	private CTabFolder folder;
	private CTabItem variablesTab;
	private Color changed;
	private Font bold;
	/* The values of the last stop (frame 0): the changed values are highlighted */
	private final Map<String, String> previous = new HashMap<>();
	private int frame;

	/** The grid of the variables (DebugStopAction clears it). */
	public static Grid getDebugVariablesGrid() {
		return grid;
	}

	/*
	 * The actions of the debugger in the toolbar of the view (like the debug window
	 * of ImageJ macros)
	 */
	private void createToolbar() {
		IToolBarManager tm = getViewSite().getActionBars().getToolBarManager();
		tm.add(new DebugRScript());
		stopAction = new DebugStopAction();
		tm.add(stopAction);
		tm.add(new Separator());
		tm.add(new DebugNextAction());
		tm.add(new DebugStepIntoAction());
		tm.add(new DebugStepFinishAction());
		tm.add(new DebugContinueAction());
		tm.add(new DebugRunToCursorAction());
		tm.add(new Separator());
		tm.add(new DebugInfoAction());
		getViewSite().getActionBars().updateActionBars();
		updateStop();
	}

	/* The stop button (disabled without a debug session) */
	private DebugStopAction stopAction;

	/*
	 * Stop only while R is debugged: a debug session of Bio7 or R waits in
	 * browser() (also a browser() called in the console)
	 */
	private void updateStop() {
		if (stopAction != null) {
			stopAction.setEnabled(DebugProgress.isSessionActive() || com.eco.bio7.rbridge.backend.EmbeddedR.isAtBrowsePrompt());
		}
	}

	/** Updates the actions of the open view (e.g., the debug session has started or ended). Any thread. */
	public static void updateActions() {
		org.eclipse.swt.widgets.Display display = PlatformUI.getWorkbench().getDisplay();
		display.asyncExec(() -> {
			if (instance != null) {
				instance.updateStop();
			}
		});
	}

	/** Shows the view (with the debug actions), e.g., when a breakpoint is set; it is not activated. */
	public static void showDebugView() {
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (page != null && page.findView(ID) == null) {
				page.showView(ID, null, IWorkbenchPage.VIEW_VISIBLE);
			}
		} catch (PartInitException | RuntimeException e) {
			/* No window */
		}
	}

	public DebugVariablesView() {
		setPartName("Debug");
	}

	@Override
	public void createPartControl(Composite parent) {
		instance = this;
		folder = new CTabFolder(parent, SWT.TOP | SWT.FLAT);
		changed = new Color(235, 145, 30);
		FontData[] fd = parent.getFont().getFontData();
		for (FontData d : fd) {
			d.setStyle(SWT.BOLD);
		}
		bold = new Font(parent.getDisplay(), fd);

		/* Variables */
		variablesTab = new CTabItem(folder, SWT.NONE);
		variablesTab.setText("Variables");
		grid = createGrid(folder, new String[] { "Name", "Class", "Dim / Length", "Size", "Value" }, new int[] { 130, 90, 90, 70, 400 });
		variablesTab.setControl(grid);
		installValueEditor();
		installVariableMenu();
		grid.addListener(SWT.MouseDoubleClick, e -> {
			GridItem item = itemAt(grid, e.x, e.y);
			if (item != null && cellColumn(item, e.x, e.y) < 4) {
				DebugProgress.openVariable(item.getText(0));
			}
		});

		/* Watch */
		CTabItem watchTab = new CTabItem(folder, SWT.NONE);
		watchTab.setText("Watch");
		Composite watch = new Composite(folder, SWT.NONE);
		GridLayout gl = new GridLayout(3, false);
		gl.marginWidth = gl.marginHeight = 2;
		watch.setLayout(gl);
		watchGrid = createGrid(watch, new String[] { "Expression", "Value" }, new int[] { 200, 450 });
		watchGrid.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		Text expression = new Text(watch, SWT.BORDER);
		expression.setMessage("An R expression, e.g. nrow(df) or mean(x)");
		expression.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Button add = new Button(watch, SWT.PUSH);
		add.setText("Add");
		Button remove = new Button(watch, SWT.PUSH);
		remove.setText("Remove");
		Runnable addExpression = () -> {
			String x = expression.getText().trim();
			if (!x.isEmpty()) {
				List<String> list = new ArrayList<>(DebugSession.watchExpressions());
				list.add(x);
				DebugSession.setWatchExpressions(list);
				expression.setText("");
				fillWatch(new ArrayList<>());
				DebugProgress.refreshWatch();
			}
		};
		add.addListener(SWT.Selection, _ -> addExpression.run());
		expression.addListener(SWT.DefaultSelection, _ -> addExpression.run());
		remove.addListener(SWT.Selection, _ -> {
			int[] rows = selectedRows(watchGrid);
			if (rows.length == 0) {
				return;
			}
			List<String> list = new ArrayList<>(DebugSession.watchExpressions());
			for (int i = rows.length - 1; i >= 0; i--) {
				if (rows[i] < list.size()) {
					list.remove(rows[i]);
				}
			}
			DebugSession.setWatchExpressions(list);
			fillWatch(new ArrayList<>());
			DebugProgress.refreshWatch();
		});
		watchTab.setControl(watch);
		fillWatch(new ArrayList<>());

		/* Call stack */
		CTabItem stackTab = new CTabItem(folder, SWT.NONE);
		stackTab.setText("Call Stack");
		stackGrid = createGrid(folder, new String[] { "Frame", "Call", "Line" }, new int[] { 60, 420, 110 });
		stackGrid.addListener(SWT.Selection, _ -> {
			int[] rows = selectedRows(stackGrid);
			if (rows.length == 1) {
				GridItem item = stackGrid.getItem(rows[0]);
				Object f = item.getData();
				if (f instanceof Integer fr) {
					/* The current frame is the last one: frame 0 */
					boolean current = rows[0] == stackGrid.getItemCount() - 1;
					DebugProgress.selectFrame(current ? 0 : fr);
				}
				if (item.getData("location") instanceof DebugSession.Location loc) {
					DebugProgress.showLine(loc);
				}
			}
		});
		stackTab.setControl(stackGrid);

		folder.setSelection(variablesTab);
		createToolbar();
		parent.addListener(SWT.Dispose, _ -> {
			changed.dispose();
			bold.dispose();
			if (instance == this) {
				instance = null;
				grid = null;
			}
		});
	}

	private static Grid createGrid(Composite parent, String[] titles, int[] widths) {
		Grid g = new Grid(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		g.setBackground(Util.getSWTBackgroundColor());
		for (int i = 0; i < titles.length; i++) {
			GridColumn c = new GridColumn(g, SWT.NONE);
			c.setText(titles[i]);
			c.setWidth(widths[i]);
		}
		g.setHeaderVisible(true);
		/*
		 * No row numbers: the width of the row header is not computed again when the
		 * rows are replaced (the numbers were painted over the first column)
		 */
		g.setRowHeaderVisible(false);
		g.setCellSelectionEnabled(true);
		return g;
	}

	/* The shown variables by name (read by the hover of the R editor in its thread) */
	private static final Map<String, String[]> shown = new java.util.concurrent.ConcurrentHashMap<>();

	/** The frame of the shown variables (0: the frame where R stopped). */
	public static int currentFrame() {
		return instance == null ? 0 : instance.frame;
	}

	/** A shown variable (name, class, dimension, size, value) or null. */
	public static String[] shownVariable(String name) {
		return name == null ? null : shown.get(name);
	}

	/** Shows the state of a stop (UI thread); newStop: R has stepped (the changed values are highlighted). */
	static void show(DebugState s, boolean newStop) {
		/* For the hover of the R editor (also if this view is closed) */
		shown.clear();
		for (String[] v : s.variables) {
			shown.put(v[0], v);
		}
		if (instance == null || grid == null || grid.isDisposed()) {
			return;
		}
		instance.showState(s, newStop);
	}

	/** Clears the views (the session has ended). */
	public static void clear() {
		shown.clear();
		if (instance == null || grid == null || grid.isDisposed()) {
			return;
		}
		instance.previous.clear();
		instance.frame = 0;
		replaceItems(grid, new ArrayList<>(), null);
		replaceItems(instance.stackGrid, new ArrayList<>(), null);
		instance.fillWatch(new ArrayList<>());
		instance.variablesTab.setText("Variables");
	}

	private void showState(DebugState s, boolean newStop) {
		frame = s.frame;
		variablesTab.setText(s.frame > 0 ? "Variables (frame " + s.frame + ")" : "Variables");
		/* Changed since the last stop (only in the frame where R stopped) */
		boolean compare = newStop && s.frame == 0 && !previous.isEmpty();
		List<Boolean> marks = new ArrayList<>();
		for (String[] v : s.variables) {
			String before = previous.get(v[0]);
			marks.add(compare && (before == null || !before.equals(v[4])));
		}
		replaceItems(grid, s.variables, marks);
		if (s.frame == 0) {
			previous.clear();
			for (String[] v : s.variables) {
				previous.put(v[0], v[4]);
			}
		}
		fillWatch(s.watch);
		if (newStop || !s.stack.isEmpty()) {
			List<String[]> rows = new ArrayList<>();
			List<DebugSession.Location> lines = s.frameLines();
			for (int i = 0; i < s.stack.size(); i++) {
				DebugState.Call c = s.stack.get(i);
				rows.add(new String[] { String.valueOf(c.frame()), c.text(), lineText(lines.get(i)) });
			}
			replaceItems(stackGrid, rows, null);
			for (int i = 0; i < stackGrid.getItemCount() && i < s.stack.size(); i++) {
				stackGrid.getItem(i).setData(s.stack.get(i).frame());
				stackGrid.getItem(i).setData("location", lines.get(i));
				boolean shown = s.frame == 0 ? i == s.stack.size() - 1 : s.stack.get(i).frame() == s.frame;
				if (shown) {
					for (int c = 0; c < 3; c++) {
						stackGrid.getItem(i).setFont(c, bold);
					}
				}
			}
		}
	}

	/* The line of a frame: the line in the script, "file:line" in another file */
	private static String lineText(DebugSession.Location loc) {
		if (loc == null) {
			return "";
		}
		return loc.file().equals(DebugSession.script()) ? String.valueOf(loc.line()) : loc.name() + ":" + loc.line();
	}

	private void fillWatch(List<String> values) {
		List<String> expressions = DebugSession.watchExpressions();
		List<String[]> rows = new ArrayList<>();
		for (int i = 0; i < expressions.size(); i++) {
			rows.add(new String[] { expressions.get(i), i < values.size() ? values.get(i) : "" });
		}
		replaceItems(watchGrid, rows, null);
	}

	/* All rows at once: no redraw in between (the former rows are not painted over the new ones) */
	private static void replaceItems(Grid g, List<String[]> rows, List<Boolean> marks) {
		if (g == null || g.isDisposed()) {
			return;
		}
		g.setRedraw(false);
		try {
			g.disposeAllItems();
			for (int r = 0; r < rows.size(); r++) {
				GridItem item = new GridItem(g, SWT.NONE);
				String[] row = rows.get(r);
				for (int c = 0; c < row.length && c < g.getColumnCount(); c++) {
					item.setText(c, row[c] == null ? "" : row[c]);
				}
				if (marks != null && r < marks.size() && marks.get(r) && instance != null) {
					for (int c = 0; c < g.getColumnCount(); c++) {
						item.setForeground(c, instance.changed);
					}
				}
			}
		} finally {
			g.setRedraw(true);
			g.redraw();
		}
	}

	/* A click into the value: an editor; Return assigns the new value (the frame where R stopped) */
	private void installValueEditor() {
		final GridEditor editor = new GridEditor(grid);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		grid.addListener(SWT.MouseDown, event -> {
			if (event.button != 1 || event.count > 1) {
				return;
			}
			GridItem item = itemAt(grid, event.x, event.y);
			if (item == null || cellColumn(item, event.x, event.y) != 4) {
				return;
			}
			final Text text = new Text(grid, SWT.NONE);
			text.addListener(SWT.FocusOut, _ -> text.dispose());
			text.addListener(SWT.Traverse, e -> {
				if (e.detail == SWT.TRAVERSE_RETURN) {
					String value = text.getText();
					if (frame == 0 && ConsolePageParticipant.getInterpreterSelection().equals("R")) {
						ConsolePageParticipant.getConsolePageParticipantInstance().pipeToRConsole(item.getText(0) + " <- " + value);
						item.setText(4, value);
					}
					text.dispose();
					e.doit = false;
				} else if (e.detail == SWT.TRAVERSE_ESCAPE) {
					text.dispose();
					e.doit = false;
				}
			});
			editor.setEditor(text, item, 4);
			text.setText(item.getText(4));
			text.selectAll();
			text.setFocus();
		});
	}

	private void installVariableMenu() {
		Menu menu = new Menu(grid);
		MenuItem open = new MenuItem(menu, SWT.PUSH);
		open.setText("Open in Table");
		open.addListener(SWT.Selection, _ -> {
			String name = selectedName();
			if (name != null) {
				DebugProgress.openVariable(name);
			}
		});
		MenuItem image = new MenuItem(menu, SWT.PUSH);
		image.setText("Open Matrix as Image");
		image.addListener(SWT.Selection, _ -> {
			String name = selectedName();
			if (name != null) {
				DebugProgress.openVariableAsImage(name);
			}
		});
		MenuItem watch = new MenuItem(menu, SWT.PUSH);
		watch.setText("Add to Watch");
		watch.addListener(SWT.Selection, _ -> {
			String name = selectedName();
			if (name != null) {
				List<String> list = new ArrayList<>(DebugSession.watchExpressions());
				list.add(name);
				DebugSession.setWatchExpressions(list);
				fillWatch(new ArrayList<>());
				DebugProgress.refreshWatch();
			}
		});
		MenuItem copy = new MenuItem(menu, SWT.PUSH);
		copy.setText("Copy");
		copy.addListener(SWT.Selection, _ -> {
			int[] rows = selectedRows(grid);
			StringBuilder sb = new StringBuilder();
			for (int r : rows) {
				GridItem item = grid.getItem(r);
				for (int c = 0; c < grid.getColumnCount(); c++) {
					sb.append(c > 0 ? "\t" : "").append(item.getText(c));
				}
				sb.append('\n');
			}
			if (sb.length() > 0) {
				Clipboard cb = new Clipboard(grid.getDisplay());
				cb.setContents(new Object[] { sb.toString() }, new Transfer[] { TextTransfer.getInstance() });
				cb.dispose();
			}
		});
		grid.setMenu(menu);
	}

	private String selectedName() {
		int[] rows = selectedRows(grid);
		return rows.length > 0 ? grid.getItem(rows[0]).getText(0) : null;
	}

	/* The rows of the selected cells */
	private static int[] selectedRows(Grid g) {
		Point[] cells = g.getCellSelection();
		return java.util.Arrays.stream(cells).mapToInt(p -> p.y).filter(y -> y >= 0 && y < g.getItemCount()).distinct().sorted().toArray();
	}

	private static GridItem itemAt(Grid g, int x, int y) {
		return g.getItem(new Point(x, y));
	}

	private static int cellColumn(GridItem item, int x, int y) {
		for (int i = 0; i < item.getParent().getColumnCount(); i++) {
			Rectangle r = item.getBounds(i);
			if (r.contains(x, y)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public void setFocus() {
		if (grid != null && !grid.isDisposed()) {
			grid.setFocus();
		}
	}
}
