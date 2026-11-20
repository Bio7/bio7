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
package com.eco.bio7.wizards.java;

/** The Java code of the GUI templates (WindowBuilder) for R and Python. */
final class WindowBuilderTemplates {

	private WindowBuilderTemplates() {
	}

	/* The lines of code indented with tabs (in the method of the template) */
	private static String tabs(String code, int n) {
		StringBuilder sb = new StringBuilder();
		for (String line : code.stripTrailing().split("\n")) {
			if (sb.length() > 0) {
				sb.append('\n');
			}
			sb.append("\t".repeat(n)).append(line);
		}
		return sb.toString();
	}

	/** The class which opens the GUI in a view of Bio7 (run it with the Run action of the Java editor). */
	static String launcher(String className, String panel, String language) {
		return """
				import org.eclipse.swt.SWT;
				import org.eclipse.swt.layout.FillLayout;
				import org.eclipse.swt.widgets.Composite;
				import org.eclipse.swt.widgets.Display;

				import com.eco.bio7.collection.CustomView;

				/*
				 * Opens the GUI (%2$s, editable with the WindowBuilder: Open With >
				 * WindowBuilder Editor) in a view of Bio7. Run this class with the Run action
				 * of the Java editor.
				 */
				public class %1$s {

					public static void main(String[] args) {
						CustomView view = new CustomView();
						Display.getDefault().syncExec(() -> {
							Composite parent = view.getComposite("%3$s GUI");
							parent.setLayout(new FillLayout());
							new %2$s(parent, SWT.NONE);
							parent.layout(true);
						});
					}
				}
				""".formatted(className, panel, language);
	}

	/**
	 * The GUI (a Composite for the WindowBuilder): the number of values, a
	 * distribution, a button and the result. compute: the code of the language
	 * (variables n, distribution; it sets double[] s = mean, sd), check: before it
	 * (may return a message), imports: of the bridge.
	 */
	static String panel(String panel, String language, String compute, String check, String imports) {
		return """
				import java.util.Locale;

				import org.eclipse.swt.SWT;
				import org.eclipse.swt.events.SelectionAdapter;
				import org.eclipse.swt.events.SelectionEvent;
				import org.eclipse.swt.layout.GridData;
				import org.eclipse.swt.layout.GridLayout;
				import org.eclipse.swt.widgets.Button;
				import org.eclipse.swt.widgets.Combo;
				import org.eclipse.swt.widgets.Composite;
				import org.eclipse.swt.widgets.Display;
				import org.eclipse.swt.widgets.Label;
				import org.eclipse.swt.widgets.Spinner;

				%5$s

				/*
				 * A GUI for %2$s, editable with the WindowBuilder (Open With > WindowBuilder
				 * Editor): add widgets in the Design tab, the code of an action in its
				 * listener. The computation runs in a background thread (the GUI stays
				 * responsive); the result is shown in the GUI thread.
				 */
				public class %1$s extends Composite {

					private Spinner spinnerN;
					private Combo comboDistribution;
					private Button btnRun;
					private Label lblResult;

					public %1$s(Composite parent, int style) {
						super(parent, style);
						setLayout(new GridLayout(2, false));

						Label lblN = new Label(this, SWT.NONE);
						lblN.setText("Number of values");

						spinnerN = new Spinner(this, SWT.BORDER);
						spinnerN.setMaximum(100000);
						spinnerN.setMinimum(10);
						spinnerN.setSelection(1000);
						spinnerN.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

						Label lblDistribution = new Label(this, SWT.NONE);
						lblDistribution.setText("Distribution");

						comboDistribution = new Combo(this, SWT.READ_ONLY);
						comboDistribution.setItems(new String[] { "Normal", "Uniform", "Exponential" });
						comboDistribution.select(0);
						comboDistribution.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

						btnRun = new Button(this, SWT.NONE);
						btnRun.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent e) {
								run();
							}
						});
						btnRun.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
						btnRun.setText("Plot with %2$s");

						lblResult = new Label(this, SWT.WRAP);
						lblResult.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
						lblResult.setText("Choose the values and press the button.");
					}

					/* The values of the GUI to %2$s, the computation in a background thread */
					private void run() {
						int n = spinnerN.getSelection();
						String distribution = comboDistribution.getText();
						Display display = getDisplay();
						btnRun.setEnabled(false);
						new Thread(() -> {
							String text = compute(n, distribution);
							display.asyncExec(() -> {
								if (!isDisposed()) {
									lblResult.setText(text);
									btnRun.setEnabled(true);
									layout(true);
								}
							});
						}, "%2$s GUI").start();
					}

					/* In the background thread: the computation of %2$s, the text of the result */
					private String compute(int n, String distribution) {
						try {
				%4$s
				%3$s
							return String.format(Locale.ROOT, "%%s: mean %%.3f, standard deviation %%.3f (n = %%d)", distribution, s[0], s[1], n);
						} catch (Exception e) {
							return "Error: " + e.getMessage();
						}
					}
				}
				""".formatted(panel, language, tabs(compute, 3), tabs(check, 3), imports);
	}
}
