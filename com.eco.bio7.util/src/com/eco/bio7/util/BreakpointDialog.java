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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

/**
 * The properties of a breakpoint of the R and the Python editor: a condition,
 * a hit count (stop once, at the n-th time) and a log point (print a value
 * instead of stopping). All empty: a simple breakpoint.
 */
public class BreakpointDialog extends TitleAreaDialog {

	private final String language;
	private final int line;
	private final String code;
	private String condition;
	private int hitCount;
	private String log;
	private Text conditionText;
	private Button useHits;
	private Spinner hits;
	private Button useLog;
	private Text logText;

	/**
	 * @param language "R" or "Python" (the examples)
	 * @param line     the line of the breakpoint (from 1)
	 * @param code     the code of the line (shown) or ""
	 */
	public BreakpointDialog(Shell parent, String language, int line, String code, String condition, int hitCount, String log) {
		super(parent);
		this.language = language;
		this.line = line;
		this.code = code == null ? "" : code;
		this.condition = condition == null ? "" : condition;
		this.hitCount = hitCount;
		this.log = log == null ? "" : log;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Breakpoint Properties");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		boolean r = "R".equals(language);
		setTitle("Breakpoint at line " + line);
		setMessage("The debugger stops if the condition is true (empty: always).");
		Composite c = new Composite(area, SWT.NONE);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 10;
		layout.marginHeight = 10;
		c.setLayout(layout);

		if (!code.isEmpty()) {
			new Label(c, SWT.NONE).setText("Line:");
			Label l = new Label(c, SWT.NONE);
			l.setText(code.length() > 80 ? code.substring(0, 80) + " ..." : code);
			l.setFont(JFaceResources.getTextFont());
		}
		new Label(c, SWT.NONE).setText("Condition:");
		conditionText = new Text(c, SWT.BORDER | SWT.SINGLE);
		conditionText.setText(condition);
		conditionText.setFont(JFaceResources.getTextFont());
		conditionText.setMessage(r ? "e.g., i == 10 || x > 0.5 && name == \"a\"" : "e.g., i == 10 or x > 0.5 and name == 'a'");
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.widthHint = 380;
		conditionText.setLayoutData(gd);

		useHits = new Button(c, SWT.CHECK);
		useHits.setText("Stop at hit:");
		hits = new Spinner(c, SWT.BORDER);
		hits.setMinimum(1);
		hits.setMaximum(1000000);
		hits.setSelection(Math.max(1, hitCount));
		useHits.setSelection(hitCount > 1);
		hits.setEnabled(hitCount > 1);
		useHits.addListener(SWT.Selection, _ -> hits.setEnabled(useHits.getSelection()));

		useLog = new Button(c, SWT.CHECK);
		useLog.setText("Log, don't stop:");
		logText = new Text(c, SWT.BORDER | SWT.SINGLE);
		logText.setText(log);
		logText.setFont(JFaceResources.getTextFont());
		logText.setMessage(r ? "e.g., mean(x)" : "e.g., x.mean() or len(items)");
		logText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		useLog.setSelection(!log.isEmpty());
		logText.setEnabled(!log.isEmpty());
		useLog.addListener(SWT.Selection, _ -> {
			logText.setEnabled(useLog.getSelection());
			if (useLog.getSelection()) {
				logText.setFocus();
			}
		});

		Label help = new Label(c, SWT.WRAP);
		help.setText("The condition is evaluated in the frame of the line before it runs. "
				+ "With a hit count the debugger stops once, at the n-th time the line is reached (with a condition: the n-th time it is true). "
				+ "A log point prints the value of the expression in the console instead of stopping. "
				+ "All empty: a simple breakpoint.");
		GridData hd = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		hd.widthHint = 460;
		hd.verticalIndent = 8;
		help.setLayoutData(hd);
		conditionText.setFocus();
		return area;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Set", true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void okPressed() {
		condition = conditionText.getText().strip();
		hitCount = useHits.getSelection() ? hits.getSelection() : 0;
		log = useLog.getSelection() ? logText.getText().strip() : "";
		super.okPressed();
	}

	/** The condition ("" if none). */
	public String condition() {
		return condition;
	}

	/** The hit count (0 or 1: none). */
	public int hitCount() {
		return hitCount;
	}

	/** The expression of a log point ("" if none: the debugger stops). */
	public String log() {
		return log;
	}

	/** A short description of breakpoint properties (e.g., for the hover of the ruler). */
	public static String describe(int line, String condition, int hitCount, String log) {
		StringBuilder b = new StringBuilder("Breakpoint (line " + line + ")");
		if (condition != null && !condition.isBlank()) {
			b.append(" if ").append(condition.strip());
		}
		if (hitCount > 1) {
			b.append(", stops at hit ").append(hitCount);
		}
		if (log != null && !log.isBlank()) {
			b.append(", logs ").append(log.strip());
		}
		return b.toString();
	}
}
