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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Java project with a GUI for R: the GUI class (a Composite, editable with
 * the WindowBuilder) and a class which opens it in a view of Bio7. The template
 * GUI sends values to R, runs R code (a plot in the plot view of Bio7) and
 * shows a result of R.
 */
public class WindowBuilderRGuiWizard extends Bio7JavaTemplateProjectWizard {

	@Override
	protected String title() {
		return "WindowBuilder GUI Project for R";
	}

	@Override
	protected String description() {
		return "This wizard creates a Java project with a GUI for R (the GUI class opens with the WindowBuilder: Open With > WindowBuilder Editor).";
	}

	@Override
	protected String defaultProject() {
		return "My_R_Gui";
	}

	@Override
	protected String defaultFile() {
		return "RGui.java";
	}

	@Override
	protected Map<String, String> files(String className) {
		String panel = className + "Panel";
		Map<String, String> files = new LinkedHashMap<>();
		files.put(panel + ".java", WindowBuilderTemplates.panel(panel, "R", """
						RUtil.toR(".n", n);
						RUtil.exec(".x <- " + switch (distribution) {
						case "Uniform" -> "runif(.n)";
						case "Exponential" -> "rexp(.n)";
						default -> "rnorm(.n)";
						} + "; hist(.x, col = 'steelblue', main = '" + distribution + "', xlab = 'x')");
						double[] s = RUtil.fromRDoubles("c(mean(.x), sd(.x))");""", """
						if (!RUtil.isAlive()) {
							return "Please start R first (the R button of the toolbar).";
						}""", "import com.eco.bio7.rbridge.RUtil;"));
		files.put(className + ".java", WindowBuilderTemplates.launcher(className, panel, "R"));
		return files;
	}
}
