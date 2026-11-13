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

package com.eco.bio7.rbridge.views;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;

/**
 * The input of the R-Shell: a StyledText (single line) with the methods of the
 * SWT Text which the R-Shell uses ({@link #getCaretPosition()},
 * {@link #getText(int, int)} and {@link #insert(String)} as for a Text), the
 * highlighting of the matching (or enclosing) brackets as in the R editor and
 * the selection of the enclosing brackets ({@link #selectEnclosing()}).
 * <p>
 * Brackets in strings and comments are ignored.
 */
public class RShellText extends StyledText {

	/* The offsets of the highlighted brackets or null */
	private int[] highlighted;
	/* An update of the brackets is scheduled (see scheduleBrackets) */
	private boolean scheduled;

	public RShellText(Composite parent, int style) {
		super(parent, style);
		/*
		 * A light theme: white like the text fields (no CSS rule of Text or StyledText
		 * matches this subclass, it had the grey background of the view). The CSS of a
		 * dark or custom theme sets its colors later (see CustomTheme and e4-dark_bio7.css).
		 */
		if (!com.eco.bio7.util.Util.isDarkTheme()) {
			setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
		}
		addCaretListener(e -> scheduleBrackets());
		/* After an edit the styles moved with the text: set again */
		addModifyListener(e -> {
			highlighted = null;
			scheduleBrackets();
		});
		/*
		 * Single line: Return doesn't traverse (to a default button), the key event
		 * reaches the listeners (evaluation of the input)
		 */
		addListener(SWT.Traverse, e -> {
			if (e.detail == SWT.TRAVERSE_RETURN) {
				e.doit = false;
			}
		});
	}

	@Override
	protected void checkSubclass() {
		/* Subclassing of StyledText is allowed here */
	}

	/* ---------------- The methods of Text ---------------- */

	/** The caret offset (as Text.getCaretPosition()). */
	public int getCaretPosition() {
		return getCaretOffset();
	}

	/**
	 * The text from start to end (inclusive) as Text.getText(int, int): the range is
	 * clamped to the text, an empty range gives "" (no exception).
	 */
	@Override
	public String getText(int start, int end) {
		int length = getCharCount();
		int s = Math.max(0, start);
		int e = Math.min(length - 1, end);
		if (s > e) {
			return "";
		}
		return super.getText(s, e);
	}

	/** Replaces the selection with the string; the caret is behind it (as Text.insert(String)). */
	@Override
	public void insert(String string) {
		int start = getSelection().x;
		super.insert(string);
		setSelection(start + string.length());
	}

	/* ---------------- Brackets ---------------- */

	/**
	 * The offsets of the brackets at the caret: the bracket before (or after) the
	 * caret and its partner, else the innermost brackets which enclose the caret;
	 * null if there are none.
	 */
	public int[] getBracketPair() {
		return bracketsAt(getText(), getCaretOffset());
	}

	/**
	 * Selects the content of the innermost brackets which enclose the selection; if
	 * the content is already selected, the brackets too (repeated: the next outer
	 * ones).
	 *
	 * @return false if there are no enclosing brackets
	 */
	public boolean selectEnclosing() {
		String s = getText();
		int[] partner = partners(s);
		Point sel = getSelection();
		int bestStart = -1;
		int bestEnd = -1;
		for (int open = 0; open < s.length(); open++) {
			int close = partner[open];
			if (close <= open) {
				continue;
			}
			/* The content and the brackets with the content */
			int[][] regions = { { open + 1, close }, { open, close + 1 } };
			for (int[] r : regions) {
				boolean contains = r[0] <= sel.x && sel.y <= r[1];
				boolean larger = r[1] - r[0] > sel.y - sel.x;
				if (contains && larger && (bestStart < 0 || r[1] - r[0] < bestEnd - bestStart)) {
					bestStart = r[0];
					bestEnd = r[1];
				}
			}
		}
		if (bestStart < 0) {
			return false;
		}
		setSelection(bestStart, bestEnd);
		return true;
	}

	/*
	 * After the event: the caret event is also sent during setText() (in
	 * StyledText.reset(), before the layout and the styles are reset), then
	 * setStyleRanges() uses the old layout with the new content (e.g.,
	 * IllegalArgumentException in StyledTextRenderer.hasLink: an offset of the old
	 * text). Several events give one update.
	 */
	private void scheduleBrackets() {
		if (scheduled || isDisposed()) {
			return;
		}
		scheduled = true;
		getDisplay().asyncExec(() -> {
			scheduled = false;
			updateBrackets();
		});
	}

	/*
	 * The brackets get a soft background (no frame on the characters: readable,
	 * also for an empty pair like '()'); the input has no other styles
	 */
	private void updateBrackets() {
		if (isDisposed()) {
			return;
		}
		int[] old = highlighted;
		int[] now = getBracketPair();
		if (Arrays.equals(old, now)) {
			return;
		}
		highlighted = now;
		if (now == null) {
			setStyleRanges(new StyleRange[0]);
			return;
		}
		Color background = bracketBackground();
		StyleRange first = new StyleRange(now[0], 1, null, background);
		StyleRange second = new StyleRange(now[1], 1, null, background);
		setStyleRanges(new StyleRange[] { first, second });
	}

	/* The background of the field, a bit lighter (dark theme) or darker (light theme) */
	private Color bracketBackground() {
		RGB bg = getBackground().getRGB();
		boolean dark = (bg.red * 299 + bg.green * 587 + bg.blue * 114) / 1000 < 128;
		if (dark) {
			return new Color(blend(bg.red, 255, 0.22), blend(bg.green, 255, 0.22), blend(bg.blue, 255, 0.22));
		}
		return new Color(blend(bg.red, 0, 0.14), blend(bg.green, 0, 0.14), blend(bg.blue, 0, 0.14));
	}

	private static int blend(int from, int to, double amount) {
		return (int) Math.round(from + (to - from) * amount);
	}

	/* The bracket pair at the caret (see getBracketPair()) */
	static int[] bracketsAt(String s, int caret) {
		int[] partner = partners(s);
		if (caret > 0 && caret <= s.length() && partner[caret - 1] >= 0) {
			return sorted(caret - 1, partner[caret - 1]);
		}
		if (caret >= 0 && caret < s.length() && partner[caret] >= 0) {
			return sorted(caret, partner[caret]);
		}
		/* The innermost brackets around the caret */
		for (int i = Math.min(caret, s.length()) - 1; i >= 0; i--) {
			char c = s.charAt(i);
			if ((c == '(' || c == '[' || c == '{') && partner[i] >= caret) {
				return sorted(i, partner[i]);
			}
		}
		return null;
	}

	private static int[] sorted(int a, int b) {
		return a < b ? new int[] { a, b } : new int[] { b, a };
	}

	/* For each bracket the offset of its partner (-1: none, or in a string or comment) */
	static int[] partners(String s) {
		int n = s.length();
		int[] partner = new int[n];
		Arrays.fill(partner, -1);
		Deque<Integer> open = new ArrayDeque<>();
		int i = 0;
		while (i < n) {
			char c = s.charAt(i);
			if (c == '#') {
				while (i < n && s.charAt(i) != '\n' && s.charAt(i) != '\r') {
					i++;
				}
				continue;
			}
			if (c == '"' || c == '\'' || c == '`') {
				i++;
				while (i < n) {
					char d = s.charAt(i);
					if (d == '\\' && c != '`') {
						i += 2;
						continue;
					}
					i++;
					if (d == c) {
						break;
					}
				}
				continue;
			}
			if (c == '(' || c == '[' || c == '{') {
				open.push(i);
			} else if (c == ')' || c == ']' || c == '}') {
				if (!open.isEmpty() && matches(s.charAt(open.peek()), c)) {
					int o = open.pop();
					partner[o] = i;
					partner[i] = o;
				}
			}
			i++;
		}
		return partner;
	}

	private static boolean matches(char open, char close) {
		return (open == '(' && close == ')') || (open == '[' && close == ']') || (open == '{' && close == '}');
	}
}
