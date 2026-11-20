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
import java.util.List;

/**
 * The state of R at a stop of the debugger (the output of .bio7dbg$state, see
 * DebugSession): the frame, the breakpoint which stopped R, the variables (name,
 * class, dimension, size, value), the values of the watch expressions and the
 * call stack.
 */
final class DebugState {

	/** A call of the call stack: the frame number, its position (file and line, null if unknown) and the call. */
	record Call(int frame, DebugSession.Location location, String text) {
	}

	int frame;
	/* The breakpoint where R stopped, null if none */
	DebugSession.Location breakpoint;
	/* The position where R stopped (the current line of the innermost frame), null if unknown */
	DebugSession.Location stop;
	final List<String[]> variables = new ArrayList<>();
	final List<String> watch = new ArrayList<>();
	final List<Call> stack = new ArrayList<>();

	/*
	 * The current line of each frame: the line of the call of the next frame (the
	 * srcref of a call is its position in the calling function), the innermost
	 * frame: the line where R stopped
	 */
	List<DebugSession.Location> frameLines() {
		List<DebugSession.Location> lines = new ArrayList<>();
		for (int i = 0; i < stack.size(); i++) {
			lines.add(i + 1 < stack.size() ? stack.get(i + 1).location() : stop);
		}
		return lines;
	}

	static DebugState parse(String output) {
		DebugState s = new DebugState();
		if (output == null) {
			return s;
		}
		for (String line : output.split("\\r?\\n")) {
			if (line.startsWith("@@FRAME@@")) {
				s.frame = parseInt(line.substring(9).trim(), 0);
			} else if (line.startsWith("@@BP@@")) {
				s.breakpoint = DebugSession.breakpointLocation(line.substring(6).trim());
			} else if (line.startsWith("@@V@@")) {
				String[] p = line.substring(5).split("\t", 5);
				if (p.length == 5) {
					s.variables.add(p);
				}
			} else if (line.startsWith("@@W@@")) {
				String[] p = line.substring(5).split("\t", 2);
				int i = parseInt(p[0], 0);
				while (s.watch.size() < i) {
					s.watch.add("");
				}
				if (i > 0) {
					s.watch.set(i - 1, p.length > 1 ? p[1] : "");
				}
			} else if (line.startsWith("@@S@@")) {
				String[] p = line.substring(5).split("\t", 3);
				if (p.length == 3) {
					/* "file#line" of R: the position in the source (the copy mapped) */
					s.stack.add(new Call(parseInt(p[0], 0), DebugSession.location(p[1]), p[2]));
				}
			}
		}
		return s;
	}

	private static int parseInt(String s, int fallback) {
		try {
			return Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
