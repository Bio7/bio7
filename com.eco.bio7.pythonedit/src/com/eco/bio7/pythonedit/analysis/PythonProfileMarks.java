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
package com.eco.bio7.pythonedit.analysis;

import java.util.List;
import java.util.Locale;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * The marks of a profile in the Python editor: the lines with the most time
 * (an icon in the ruler and the overview ruler, the times in the hover of the
 * ruler). Not saved with the file.
 */
public final class PythonProfileMarks {

	/** The marker of a line with its time. */
	public static final String PROFILE = "com.eco.bio7.pythonedit.profileline";

	/** A line of the profile: the line (from 1), the time with the called functions and the time of the line itself (ms). */
	public record Line(int line, double millis, double selfMillis) {
	}

	private PythonProfileMarks() {
	}

	/** Replaces the marks of the file (total: the profiled time in ms). */
	public static void show(IFile file, List<Line> lines, double totalMillis) {
		clear(file);
		for (Line l : lines) {
			try {
				IMarker m = file.createMarker(PROFILE);
				m.setAttribute(IMarker.LINE_NUMBER, l.line());
				m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
				String pct = totalMillis > 0 ? String.format(Locale.ROOT, " (%.1f %%)", 100 * l.millis() / totalMillis) : "";
				String self = l.selfMillis() > 0 && l.selfMillis() < l.millis() - 0.05 ? ", the line itself " + time(l.selfMillis()) : "";
				m.setAttribute(IMarker.MESSAGE, "Profile: " + time(l.millis()) + pct + self);
			} catch (CoreException e) {
				/* Not marked */
			}
		}
	}

	/** Removes the marks of the file. */
	public static void clear(IFile file) {
		try {
			file.deleteMarkers(PROFILE, false, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			/* Not removed */
		}
	}

	private static String time(double ms) {
		return ms >= 1000 ? String.format(Locale.ROOT, "%.2f s", ms / 1000) : ms >= 10 ? String.format(Locale.ROOT, "%.0f ms", ms) : String.format(Locale.ROOT, "%.1f ms", ms);
	}
}
