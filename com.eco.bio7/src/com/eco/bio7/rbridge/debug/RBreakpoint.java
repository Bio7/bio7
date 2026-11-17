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
package com.eco.bio7.rbridge.debug;

import org.eclipse.core.resources.IMarker;

/**
 * A breakpoint of the R editor (the same properties as in the Python editor):
 * a condition, a hit count (stop once, at the n-th time; with a condition: the
 * n-th time it is true) and a log point (print the value, don't stop). Older
 * breakpoints stored one text (the message): x == 5, #10, log: x or an
 * expression with browser().
 *
 * @param legacy an expression with browser() of an older breakpoint (else null)
 */
public record RBreakpoint(String condition, int hitCount, String log, String legacy) {

	/** The marker of a breakpoint of the R editor. */
	public static final String MARKER = "com.eco.bio7.redit.debugMarker";
	/** The attributes (as in the Python editor). */
	public static final String CONDITION = "condition", HIT_COUNT = "hitCount", LOG = "logMessage";

	public RBreakpoint {
		condition = condition == null ? "" : condition.strip();
		log = log == null ? "" : log.strip();
	}

	/** True for a breakpoint without condition, hit count and log. */
	public boolean isPlain() {
		return condition.isEmpty() && hitCount <= 1 && log.isEmpty() && legacy == null;
	}

	/** The breakpoint of a marker (the attributes, else the text of an older breakpoint). */
	public static RBreakpoint of(IMarker m) {
		String cond = m.getAttribute(CONDITION, null);
		int hits = m.getAttribute(HIT_COUNT, 0);
		String log = m.getAttribute(LOG, null);
		if (cond != null || hits > 0 || log != null) {
			return new RBreakpoint(cond, hits, log, null);
		}
		return parse(m.getAttribute(IMarker.MESSAGE, null));
	}

	/** The breakpoint of the text of an older breakpoint (x == 5, #10, log: x, ... browser() ...). */
	public static RBreakpoint parse(String text) {
		String e = text == null ? "" : text.strip();
		if (e.isEmpty()) {
			return new RBreakpoint("", 0, "", null);
		}
		if (e.startsWith("#")) {
			try {
				return new RBreakpoint("", Integer.parseInt(e.substring(1).strip()), "", null);
			} catch (NumberFormatException ex) {
				return new RBreakpoint("", 0, "", null);
			}
		}
		if (e.regionMatches(true, 0, "log:", 0, 4)) {
			return new RBreakpoint("", 0, e.substring(4), null);
		}
		if (e.contains("browser()")) {
			return new RBreakpoint("", 0, "", e);
		}
		return new RBreakpoint(e, 0, "", null);
	}

	/**
	 * The text of the message for older code (e.g., the trace mode of the debugger):
	 * the condition, #n or log: x (several properties: the condition).
	 */
	public String legacyText() {
		if (legacy != null) {
			return legacy;
		}
		if (!condition.isEmpty()) {
			return condition;
		}
		if (hitCount > 1) {
			return "#" + hitCount;
		}
		if (!log.isEmpty()) {
			return "log: " + log;
		}
		return null;
	}
}
