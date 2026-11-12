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

package com.eco.bio7.rbridge.completion;

import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.fieldassist.IControlContentAdapter2;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;

/**
 * The content adapter of the field assist (code completion, history) for a
 * StyledText (as the TextContentAdapter of JFace for a Text).
 */
public class StyledTextContentAdapter implements IControlContentAdapter, IControlContentAdapter2 {

	@Override
	public String getControlContents(Control control) {
		return ((StyledText) control).getText();
	}

	@Override
	public void setControlContents(Control control, String text, int cursorPosition) {
		StyledText t = (StyledText) control;
		t.setText(text);
		t.setSelection(Math.min(cursorPosition, t.getCharCount()));
	}

	@Override
	public void insertControlContents(Control control, String text, int cursorPosition) {
		StyledText t = (StyledText) control;
		Point selection = t.getSelection();
		t.replaceTextRange(selection.x, selection.y - selection.x, text);
		/* Insert the caret at the cursor position within the inserted text */
		int offset = cursorPosition <= text.length() ? cursorPosition : text.length();
		t.setSelection(selection.x + offset);
	}

	@Override
	public int getCursorPosition(Control control) {
		return ((StyledText) control).getCaretOffset();
	}

	@Override
	public Rectangle getInsertionBounds(Control control) {
		StyledText t = (StyledText) control;
		Point caret = t.getLocationAtOffset(t.getCaretOffset());
		Rectangle client = t.getClientArea();
		return new Rectangle(caret.x + client.x, caret.y + client.y + 3, 1, t.getLineHeight());
	}

	@Override
	public void setCursorPosition(Control control, int index) {
		((StyledText) control).setSelection(index);
	}

	@Override
	public Point getSelection(Control control) {
		return ((StyledText) control).getSelection();
	}

	@Override
	public void setSelection(Control control, Point range) {
		((StyledText) control).setSelection(range.x, range.y);
	}
}
