/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.rbridge;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.StatusLineContributionItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.rbridge.backend.RBackend;

/**
 * The memory used by embedded R in the status line, next to the memory of Java:
 * embedded R runs in the process of Bio7 but allocates its memory outside of the
 * Java heap, so the Java memory display doesn't contain it. Shown only while
 * embedded R runs (not for Rserve).
 * <p>
 * R is asked with gc() (the memory of the R objects, cons cells and vectors)
 * when R is idle: within a second after an evaluation (also a short one like
 * rm(x), see RSession.getActivity) or a transfer, else at the latest every 30
 * seconds. gc() collects the garbage of R, so it is not called
 * more often. The call runs in a background thread with a time limit (the R
 * session queues it), the UI never waits for R.
 */
public final class RMemoryStatus {

	private static final String CODE = "sum(gc(verbose = FALSE)[, 2])";
	private static final long POLL_MILLIS = 1000;
	private static final long IDLE_MILLIS = 30000;
	private static final long TIMEOUT_MILLIS = 3000;

	private static StatusLineContributionItem item;
	private static IStatusLineManager manager;
	private static ScheduledExecutorService timer;
	/* R was busy since the last measurement (the memory may have changed) */
	private static volatile boolean dirty = true;
	private static volatile long lastMeasure;
	private static volatile RBackend lastBackend;
	/* The activity of R (finished evaluations) at the last measurement */
	private static volatile long lastActivity = Long.MIN_VALUE;

	private RMemoryStatus() {
	}

	/** Adds the field to the status line and starts the measurements. */
	public static void install(IStatusLineManager statusLine) {
		if (item != null) {
			return;
		}
		manager = statusLine;
		item = new StatusLineContributionItem("com.eco.bio7.rMemory", 14);
		item.setVisible(false);
		statusLine.add(item);
		timer = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r, "Bio7 R memory status");
			t.setDaemon(true);
			return t;
		});
		timer.scheduleWithFixedDelay(RMemoryStatus::poll, 2000, POLL_MILLIS, TimeUnit.MILLISECONDS);
	}

	private static void poll() {
		try {
			if (!PlatformUI.isWorkbenchRunning()) {
				return;
			}
			RBackend backend = RBridge.getBackend();
			if (backend == null || !backend.isEmbedded()) {
				/* No R or Rserve: the field is hidden */
				lastBackend = null;
				show(null);
				return;
			}
			if (backend != lastBackend) {
				/* Embedded R started (again) */
				lastBackend = backend;
				dirty = true;
			}
			/* R has evaluated something since the last measurement (also a short command like rm(x)) */
			if (backend.activity() != lastActivity) {
				dirty = true;
			}
			if (RState.isBusy()) {
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
				show(backend.withTimeout(TIMEOUT_MILLIS).eval(CODE).asDouble());
				/* After the measurement (it is an evaluation itself) */
				lastActivity = backend.activity();
			} catch (Exception e) {
				/* Busy or ended in the meantime: the next poll */
				dirty = true;
			}
		} catch (RuntimeException | LinkageError e) {
			/* The workbench is closing (or embedded R is not available) */
		}
	}

	/* The memory in MB or null (no embedded R: the field is hidden) */
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
		String text = visible ? "R: " + format(mb) : "";
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
		return String.format(Locale.ROOT, "%.0f MB", mb);
	}
}
