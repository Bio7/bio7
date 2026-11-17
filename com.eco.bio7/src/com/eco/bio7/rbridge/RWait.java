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
package com.eco.bio7.rbridge;

import org.eclipse.swt.widgets.Display;

import com.eco.bio7.batch.Bio7Dialog;

/**
 * Runs an action of R when R is free: at once, else after a wait in the
 * background (e.g., R loads the workspace at the start or finishes a short
 * task) of at most {@link #LIMIT_MS}; then the message "R is busy!". The user
 * interface is not blocked.
 */
public final class RWait {

	/** The longest wait for R. */
	public static final long LIMIT_MS = 30_000;

	private RWait() {
	}

	/**
	 * Runs the action in the user interface thread when R is free.
	 *
	 * @param what   what waits (for the messages, e.g., "The R script")
	 * @param action the action (it sets R busy itself, e.g., with a job)
	 */
	public static void whenFree(String what, Runnable action) {
		Display display = Display.getDefault();
		if (!RState.isBusy()) {
			if (display.getThread() == Thread.currentThread()) {
				action.run();
			} else {
				display.asyncExec(action);
			}
			return;
		}
		System.out.println("R is busy: " + what + " starts when R is free.");
		Thread waiter = new Thread(() -> {
			long end = System.currentTimeMillis() + LIMIT_MS;
			while (RState.isBusy() && System.currentTimeMillis() < end) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					return;
				}
			}
			display.asyncExec(() -> {
				if (RState.isBusy()) {
					Bio7Dialog.message("R is busy!\n" + what + " was not started (R was busy for more than " + LIMIT_MS / 1000 + " s).");
				} else {
					action.run();
				}
			});
		}, "Bio7 waits for R");
		waiter.setDaemon(true);
		waiter.start();
	}
}
