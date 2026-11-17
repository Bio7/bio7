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
package com.eco.bio7.rcp;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Starts R or Python in a job (e.g., on a slow computer the start takes
 * seconds): the user interface is not blocked (it is painted, views can be
 * used), the caller continues when the start is finished (as before). A second
 * call during the start waits for the same start.
 */
public final class BackgroundStart {

	private static final Map<String, CompletableFuture<String>> STARTS = new ConcurrentHashMap<>();

	private BackgroundStart() {
	}

	/**
	 * Runs a start (the result: null or an error). In the user interface thread:
	 * as a job while the events are processed; else directly.
	 *
	 * @param name  the name of the job (e.g., "Starting Python")
	 * @param start the start (it may use syncExec)
	 */
	public static String run(String name, Supplier<String> start) {
		Display display = Display.getCurrent();
		if (display == null) {
			return start.get();
		}
		CompletableFuture<String> future = STARTS.computeIfAbsent(name, k -> {
			CompletableFuture<String> f = new CompletableFuture<>();
			Job job = new Job(name) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask(name + " ...", IProgressMonitor.UNKNOWN);
					try {
						f.complete(start.get());
					} catch (Throwable t) {
						f.complete(name + " failed: " + t);
					} finally {
						STARTS.remove(name);
						monitor.done();
						if (!display.isDisposed()) {
							display.wake();
						}
					}
					return Status.OK_STATUS;
				}
			};
			job.setPriority(Job.INTERACTIVE);
			job.schedule();
			return f;
		});
		/* The cursor shows that something starts (the user interface is usable) */
		Shell shell = display.getActiveShell();
		Cursor old = shell == null ? null : shell.getCursor();
		if (shell != null) {
			shell.setCursor(display.getSystemCursor(SWT.CURSOR_APPSTARTING));
		}
		try {
			while (!future.isDone() && !display.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		} finally {
			if (shell != null && !shell.isDisposed()) {
				shell.setCursor(old);
			}
		}
		return future.getNow("Bio7 was closed during: " + name);
	}
}
