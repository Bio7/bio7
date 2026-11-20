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
 * A Java project with a GUI for Python: the GUI class (a Composite, editable
 * with the WindowBuilder) and a class which opens it in a view of Bio7. The
 * template GUI sends values to the embedded Python, runs Python code (numpy, a
 * matplotlib plot in the plot view of Bio7) and shows a result of Python.
 */
public class WindowBuilderPythonGuiWizard extends Bio7JavaTemplateProjectWizard {

	@Override
	protected String title() {
		return "WindowBuilder GUI Project for Python";
	}

	@Override
	protected String description() {
		return "This wizard creates a Java project with a GUI for Python (numpy, matplotlib; the GUI class opens with the WindowBuilder: Open With > WindowBuilder Editor).";
	}

	@Override
	protected String defaultProject() {
		return "My_Python_Gui";
	}

	@Override
	protected String defaultFile() {
		return "PythonGui.java";
	}

	@Override
	protected Map<String, String> files(String className) {
		String panel = className + "Panel";
		Map<String, String> files = new LinkedHashMap<>();
		files.put(panel + ".java", WindowBuilderTemplates.panel(panel, "Python", """
						PythonUtil.toPython("n", n);
						PythonUtil.exec("import numpy as np\\nx = " + switch (distribution) {
						case "Uniform" -> "np.random.uniform(size=int(n[0]))";
						case "Exponential" -> "np.random.exponential(size=int(n[0]))";
						default -> "np.random.normal(size=int(n[0]))";
						});
						double[] s = PythonUtil.fromPythonDoubles("np.array([x.mean(), x.std(ddof=1)])");
						PythonUtil.run("import matplotlib.pyplot as plt\\nplt.figure()\\nplt.hist(x, bins=30, color='steelblue')\\nplt.title('" + distribution + "')\\nplt.show()");""", """
						if (!PythonUtil.isAlive()) {
							PythonUtil.start();
						}""", "import com.eco.bio7.pybridge.PythonUtil;"));
		files.put(className + ".java", WindowBuilderTemplates.launcher(className, panel, "Python"));
		return files;
	}
}
