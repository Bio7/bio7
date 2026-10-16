/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
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

package com.eco.bio7.time;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.action.StatusLineContributionItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.compile.Model;
import com.eco.bio7.info.InfoView;
import com.eco.bio7.methods.Compiled;
import com.eco.bio7.rcp.ApplicationActionBarAdvisor;

/**
 * Calls the run method of the compiled model in the interval of
 * {@link Time#getInterval()} while the pause is not active.
 * <ul>
 * <li>The steps have a fixed rate: the run time of the model is part of the
 * interval. If the model is slower than the interval, the next step starts
 * immediately (no steps are made up later).</li>
 * <li>Start, stop and a changed interval take effect immediately, also during
 * a long interval. In the pause the thread waits without polling.</li>
 * <li>The displays are updated asynchronously, at most 25 times per second, so
 * that the user interface does not slow down the calculation.</li>
 * <li>An error of the model removes the model, the thread keeps running.</li>
 * </ul>
 */
public class CalculationThread extends Thread {

	/* The minimum time between two display updates (25 per second)! */
	private static final long DISPLAY_PERIOD = TimeUnit.MILLISECONDS.toNanos(40);

	/* The measured frames per second (smoothed), 0 in the pause! */
	private static volatile double measuredFps = 0;

	private final AtomicBoolean displayPending = new AtomicBoolean(false);

	private long lastDisplay = 0;

	private long lastStep = 0;

	public CalculationThread() {
		super("Bio7 Calculation Thread");
		/* The thread does not prevent the exit of the application! */
		setDaemon(true);
	}

	/**
	 * Returns the measured frame rate (calculation steps per second).
	 * 
	 * @return the frames per second, 0 if the calculation is paused.
	 */
	public static double getMeasuredFps() {
		return measuredFps;
	}

	public void run() {
		/* The planned start of the last step (0 after the pause) and of the next step! */
		long planned = 0;
		long next = System.nanoTime();
		while (true) {
			try {
				synchronized (Time.LOCK) {
					if (Time.isPause()) {
						measuredFps = 0;
						lastStep = 0;
						planned = 0;
						updateDisplays(true);
						while (Time.isPause()) {
							Time.LOCK.wait();
						}
						/* After the pause the first step starts immediately! */
						next = System.nanoTime();
					}
					/* Wait until the next step (a changed interval wakes up the thread)! */
					long wait;
					while (!Time.isPause() && (wait = next - System.nanoTime()) > 0) {
						TimeUnit.NANOSECONDS.timedWait(Time.LOCK, wait);
						/* The interval might have been changed during the wait (shorter or longer)! */
						if (planned != 0) {
							next = planned + Time.getIntervalNanos();
						}
					}
					if (Time.isPause()) {
						continue;
					}
				}
				long start = System.nanoTime();
				step();
				measure(start);
				/*
				 * The next step is planned from the planned (not the actual) start, so that
				 * the delays of the wait do not add up!
				 */
				planned = next;
				next = planned + Time.getIntervalNanos();
				/* A model slower than the interval: no steps are made up later! */
				long now = System.nanoTime();
				if (next < now) {
					planned = now - Time.getIntervalNanos();
					next = now;
				}
				updateDisplays(false);
			} catch (InterruptedException e) {
				return;
			} catch (Throwable t) {
				/* The thread must not end, e.g., after an error in a display update! */
				t.printStackTrace();
			}
		}
	}

	private void step() {
		Model model = Compiled.getModel();
		if (model != null) {
			try {
				model.run();
			} catch (Throwable e) {
				/* Also errors (e.g., a StackOverflowError) of the model! */
				System.err.println("The run method of the compiled model failed, the model is removed:");
				e.printStackTrace();
				Compiled.setModel(null);
			}
		}
		Time.count();
		Time.Timeupdate();
	}

	/* Smoothed frames per second from the time between the starts of two steps! */
	private void measure(long start) {
		if (lastStep != 0) {
			double rate = 1e9 / Math.max(1, start - lastStep);
			measuredFps = measuredFps == 0 ? rate : measuredFps * 0.8 + rate * 0.2;
		}
		lastStep = start;
	}

	/* Updates the displays asynchronously, at most 25 times per second (always if forced)! */
	private void updateDisplays(boolean force) {
		long now = System.nanoTime();
		if (!force && now - lastDisplay < DISPLAY_PERIOD) {
			return;
		}
		if (!PlatformUI.isWorkbenchRunning() || !displayPending.compareAndSet(false, true)) {
			return;
		}
		lastDisplay = now;
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (display.isDisposed()) {
			return;
		}
		display.asyncExec(() -> {
			displayPending.set(false);
			StatusLineContributionItem ic = ApplicationActionBarAdvisor.getUserItem();
			if (ic != null) {
				ic.setText("Time steps: " + Time.getCounter());
			}
			Label lt = InfoView.getLblTimesteps();
			if (lt != null && lt.isDisposed() == false) {
				lt.setText("Time steps: " + Time.getTime());
			}
			InfoView.updateRate();
		});
	}

}
