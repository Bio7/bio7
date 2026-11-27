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
package com.eco.bio7.util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;

/**
 * Listeners and filters which never throw. Many SWT events are sent from a
 * callback of the operating system (on macOS: painting, a change of the focus
 * or of the size, the activation of a window - also while the workbench closes
 * and its widgets are disposed). An exception which leaves such a callback can
 * corrupt the state of Cocoa: Bio7 crashes later (e.g., when an autorelease
 * pool is released). The guarded listeners skip disposed widgets and report an
 * error once (on stderr) instead of throwing it.
 */
public final class SafeEvents {

	/* The errors reported already (one report per listener and type of the error) */
	private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

	private SafeEvents() {
	}

	/**
	 * A listener (or display filter) which skips disposed widgets and doesn't
	 * throw.
	 *
	 * @param name     the name of the listener in the report of an error
	 * @param listener the listener
	 * @return the guarded listener (keep it to remove a filter again)
	 */
	public static Listener guard(String name, Listener listener) {
		return event -> {
			if (event.widget != null && event.widget.isDisposed()) {
				return;
			}
			try {
				listener.handleEvent(event);
			} catch (RuntimeException | LinkageError e) {
				report(name, e);
			}
		};
	}

	/**
	 * A paint listener which skips disposed widgets and doesn't throw: painting
	 * runs in the drawing callback of the operating system (on macOS drawRect, with
	 * a saved graphics state which an exception would not restore).
	 *
	 * @param name     the name of the listener in the report of an error
	 * @param listener the paint listener
	 * @return the guarded paint listener (keep it to remove it again)
	 */
	public static PaintListener guardPaint(String name, PaintListener listener) {
		return event -> {
			if (event.widget != null && event.widget.isDisposed()) {
				return;
			}
			try {
				listener.paintControl(event);
			} catch (RuntimeException | LinkageError e) {
				report(name, e);
			}
		};
	}

	/**
	 * The exceptions of the runnables of timerExec (and of a syncExec in the
	 * thread of the user interface) are written to the error log instead of being
	 * thrown again: on macOS a timer runs in a callback of the operating system,
	 * an exception leaving it can corrupt the state of Cocoa. Errors (e.g.,
	 * OutOfMemoryError) are still thrown.
	 *
	 * @param display the display of the application
	 */
	public static void installExceptionHandler(Display display) {
		display.setRuntimeExceptionHandler(e -> {
			try {
				ILog.of(SafeEvents.class).log(new Status(IStatus.ERROR, "com.eco.bio7.util", "Unhandled exception in a timer or a runnable of the user interface", e));
			} catch (RuntimeException | LinkageError logError) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * Runs the code of a listener (e.g., of a part listener which the workbench
	 * calls when the focus changes) without throwing.
	 *
	 * @param name the name of the listener in the report of an error
	 * @param code the code
	 */
	public static void run(String name, Runnable code) {
		try {
			code.run();
		} catch (RuntimeException | LinkageError e) {
			report(name, e);
		}
	}

	/** True if the workbench closes (or is not running): listeners have nothing to do then. */
	public static boolean closing() {
		return !PlatformUI.isWorkbenchRunning() || PlatformUI.getWorkbench().isClosing();
	}

	/** Reports an error of a listener once (the error is not thrown). */
	public static void report(String name, Throwable error) {
		if (REPORTED.add(name + ":" + error.getClass().getName())) {
			System.err.println("Bio7: an error in the event handling of " + name + " was caught (it is reported once):");
			error.printStackTrace();
		}
	}
}
