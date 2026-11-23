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

package com.eco.bio7.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

/**
 * The message box of Bio7: a drop-in replacement of the SWT MessageBox (the
 * same methods, style bits and return values). By default a JFace message dialog
 * (styled by the theme, e.g., dark in a dark theme on every OS), else the native
 * message box of the OS (see {@link ThemedDialogs#MESSAGE}).
 */
public class ThemedMessageBox {

	private final Shell parent;
	private final int style;
	private String title = "";
	private String message = "";

	public ThemedMessageBox(Shell parent) {
		this(parent, SWT.OK | SWT.ICON_INFORMATION | SWT.APPLICATION_MODAL);
	}

	public ThemedMessageBox(Shell parent, int style) {
		this.parent = parent;
		this.style = style;
	}

	public Shell getParent() {
		return parent;
	}

	public int getStyle() {
		return style;
	}

	public String getText() {
		return title;
	}

	public void setText(String text) {
		this.title = text == null ? "" : text;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message == null ? "" : message;
	}

	/**
	 * Opens the message box: the button chosen, as SWT.OK, SWT.CANCEL, SWT.YES,
	 * SWT.NO, SWT.RETRY, SWT.ABORT or SWT.IGNORE (like MessageBox.open).
	 */
	public int open() {
		if (!ThemedDialogs.isEnabled(ThemedDialogs.MESSAGE)) {
			MessageBox box = new MessageBox(parent, style);
			box.setText(title);
			box.setMessage(message);
			return box.open();
		}
		/* The buttons of the style, in the order of MessageBox */
		List<String> labels = new ArrayList<>();
		List<Integer> codes = new ArrayList<>();
		int buttons = style & (SWT.OK | SWT.CANCEL | SWT.YES | SWT.NO | SWT.RETRY | SWT.ABORT | SWT.IGNORE);
		if ((buttons & SWT.ABORT) != 0) {
			add(labels, codes, IDialogConstants.ABORT_LABEL, SWT.ABORT);
		}
		if ((buttons & SWT.RETRY) != 0) {
			add(labels, codes, IDialogConstants.RETRY_LABEL, SWT.RETRY);
		}
		if ((buttons & SWT.IGNORE) != 0) {
			add(labels, codes, IDialogConstants.IGNORE_LABEL, SWT.IGNORE);
		}
		if ((buttons & SWT.YES) != 0) {
			add(labels, codes, IDialogConstants.YES_LABEL, SWT.YES);
		}
		if ((buttons & SWT.NO) != 0) {
			add(labels, codes, IDialogConstants.NO_LABEL, SWT.NO);
		}
		if ((buttons & SWT.OK) != 0 || labels.isEmpty() && (buttons & SWT.CANCEL) == 0) {
			add(labels, codes, IDialogConstants.OK_LABEL, SWT.OK);
		}
		if ((buttons & SWT.CANCEL) != 0) {
			add(labels, codes, IDialogConstants.CANCEL_LABEL, SWT.CANCEL);
		}
		int kind = (style & SWT.ICON_ERROR) != 0 ? MessageDialog.ERROR
				: (style & SWT.ICON_WARNING) != 0 ? MessageDialog.WARNING
						: (style & SWT.ICON_QUESTION) != 0 ? MessageDialog.QUESTION
								: (style & (SWT.ICON_INFORMATION | SWT.ICON_WORKING)) != 0 ? MessageDialog.INFORMATION : MessageDialog.NONE;
		Shell shell = parent != null && !parent.isDisposed() ? parent : null;
		MessageDialog dialog = new MessageDialog(shell, title, null, message, kind, 0, labels.toArray(new String[0]));
		int index = dialog.open();
		if (index >= 0 && index < codes.size()) {
			return codes.get(index);
		}
		/* Closed (Esc or the close button): like MessageBox, Cancel, else No, else OK */
		return codes.contains(SWT.CANCEL) ? SWT.CANCEL : codes.contains(SWT.NO) ? SWT.NO : codes.contains(SWT.OK) ? SWT.OK : codes.get(codes.size() - 1);
	}

	private static void add(List<String> labels, List<Integer> codes, String label, int code) {
		labels.add(label);
		codes.add(code);
	}
}
