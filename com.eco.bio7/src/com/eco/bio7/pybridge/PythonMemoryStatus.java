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

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.StatusLineContributionItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.pyffm.PyValue;

/**
 * The memory of the embedded Python in the status line (as the memory of
 * embedded R): the size of the variables of the Python workspace (numpy arrays
 * with their data, pandas objects with their content, lists and dicts with
 * their elements). Python allocates outside of the Java heap, so the Java
 * memory display doesn't contain it. Shown only while the embedded Python runs.
 * <p>
 * Measured when Python is idle: within a second after Python ran code (the
 * console, a script, a transfer), else at the latest every 30 seconds. The UI
 * never waits for Python.
 */
public final class PythonMemoryStatus {

	private static final long POLL_MILLIS = 1000;
	private static final long IDLE_MILLIS = 30000;

	private static StatusLineContributionItem item;
	private static IStatusLineManager manager;
	private static ScheduledExecutorService timer;
	private static volatile boolean dirty = true;
	private static volatile long lastMeasure;
	private static volatile long lastActivity = Long.MIN_VALUE;

	private PythonMemoryStatus() {
	}

	/** Adds the field to the status line and starts the measurements. */
	public static void install(IStatusLineManager statusLine) {
		if (item != null) {
			return;
		}
		manager = statusLine;
		item = new StatusLineContributionItem("com.eco.bio7.pythonMemory", 18) {
			@Override
			public void fill(org.eclipse.swt.widgets.Composite parent) {
				int before = parent.getChildren().length;
				super.fill(parent);
				/* The created controls: a double-click stops the running Python code */
				org.eclipse.swt.widgets.Control[] children = parent.getChildren();
				for (int i = before; i < children.length; i++) {
					children[i].setToolTipText("The memory of the embedded Python. While Python runs: double-click to stop (interrupt)");
					children[i].addListener(org.eclipse.swt.SWT.MouseDoubleClick, e -> {
						if (PythonJobs.runningLabel() != null) {
							EmbeddedPython.interrupt();
						}
					});
				}
			}
		};
		item.setVisible(false);
		statusLine.add(item);
		timer = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "Bio7 Python memory status");
			t.setDaemon(true);
			return t;
		});
		timer.scheduleWithFixedDelay(PythonMemoryStatus::poll, 2000, POLL_MILLIS, TimeUnit.MILLISECONDS);
	}

	private static void poll() {
		try {
			if (!PlatformUI.isWorkbenchRunning()) {
				return;
			}
			PySession py = EmbeddedPython.session();
			if (py == null) {
				show(null);
				return;
			}
			if (py.getActivity() != lastActivity) {
				dirty = true;
			}
			/* A script or a long console line: the running time (double-click stops it) */
			String running = PythonJobs.runningLabel();
			if (running != null && PythonJobs.runningMillis() >= PythonJobs.RUN_DELAY) {
				dirty = true;
				showText("Py: running " + PythonJobs.time(PythonJobs.runningMillis()));
				return;
			}
			if (py.isBusy()) {
				dirty = true;
				return;
			}
			long now = System.currentTimeMillis();
			if (!dirty && now - lastMeasure < IDLE_MILLIS) {
				return;
			}
			dirty = false;
			lastMeasure = now;
			try {
				PyValue v = py.eval("__import__('_bio7')._memory()");
				show(v instanceof PyValue.Array a ? a.asDouble() : null);
				/* After the measurement (it is a call itself) */
				lastActivity = py.getActivity();
			} catch (Exception e) {
				dirty = true;
			}
		} catch (RuntimeException | LinkageError e) {
			/* The workbench is closing (or the embedded Python is not available) */
		}
	}

	private static void showText(String text) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(() -> {
			if (item == null || manager == null || text.equals(item.getText()) && item.isVisible()) {
				return;
			}
			item.setText(text);
			if (!item.isVisible()) {
				item.setVisible(true);
				manager.update(true);
			}
		});
	}

	private static void show(Double mb) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(() -> showNow(mb));
	}

	private static void showNow(Double mb) {
		if (item == null || manager == null) {
			return;
		}
		boolean visible = mb != null && !mb.isNaN();
		String text = visible ? "Py: " + format(mb) : "";
		if (visible == item.isVisible() && text.equals(item.getText())) {
			return;
		}
		item.setText(text);
		if (visible != item.isVisible()) {
			item.setVisible(visible);
			manager.update(true);
		}
	}

	private static String format(double mb) {
		if (mb >= 1024) {
			return String.format(Locale.ROOT, "%.2f GB", mb / 1024);
		}
		if (mb < 10) {
			return String.format(Locale.ROOT, "%.1f MB", mb);
		}
		return String.format(Locale.ROOT, "%.0f MB", mb);
	}
}
