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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.eco.bio7.windowbuilder.WebAppFiles;
import com.eco.bio7.windowbuilder.OutputLocation;
import com.eco.bio7.windowbuilder.WindowBuilderShiny;

/**
 * WindowBuilder JavaScript: a Java project with a GUI class (designed with the
 * WindowBuilder) and its HTML page with JavaScript (web/<class>: index.html from
 * the GUI, app.js with a simulation of random walks, style.css). Change the GUI
 * and create the page again with Create JavaScript App (context menu of the
 * class).
 */
public class WindowBuilderJavaScriptWizard extends Bio7JavaTemplateProjectWizard {

	@Override
	protected String title() {
		return "WindowBuilder JavaScript App Project";
	}

	@Override
	protected String description() {
		return "This wizard creates a Java project with a GUI (WindowBuilder) and its HTML page with JavaScript (web/<class>: index.html from the GUI, app.js).";
	}

	@Override
	protected String defaultProject() {
		return "My_JavaScript_App";
	}

	@Override
	protected String defaultFile() {
		return "RandomWalkGui.java";
	}

	@Override
	protected Map<String, String> files(String className) {
		Map<String, String> files = new LinkedHashMap<>();
		files.put(className + ".java", gui(className));
		return files;
	}

	@Override
	protected String option() {
		return "Create the app in its own project (<project>_Web)";
	}

	@Override
	protected void afterCreate(IProject project, IContainer src, String className, boolean ownProject, IProgressMonitor monitor) throws CoreException {
		IFile gui = src.getFile(new org.eclipse.core.runtime.Path(className + ".java"));
		/* The app: in its own project or in the folder web/<class> of the project (remembered for the GUI class) */
		IContainer target = ownProject ? project.getWorkspace().getRoot().getProject(project.getName() + "_Web") : OutputLocation.inProject(gui, OutputLocation.Kind.WEB);
		WindowBuilderShiny.WebResult result = WindowBuilderShiny.convertWeb(gui(className), "Random Walks", gui.getName() + " (" + gui.getFullPath().removeLastSegments(1) + ")");
		WebAppFiles.write(target, result, JS, monitor);
		OutputLocation.store(gui, OutputLocation.Kind.WEB, target);
	}

	/* The GUI: two sliders, a check box, a button, a canvas and a text output */
	static String gui(String className) {
		return """
				import org.eclipse.swt.SWT;
				import org.eclipse.swt.layout.GridData;
				import org.eclipse.swt.layout.GridLayout;
				import org.eclipse.swt.widgets.Button;
				import org.eclipse.swt.widgets.Canvas;
				import org.eclipse.swt.widgets.Composite;
				import org.eclipse.swt.widgets.Label;
				import org.eclipse.swt.widgets.Scale;

				/*
				 * WindowBuilder JavaScript: the design of an HTML page with JavaScript. Open this
				 * class with the WindowBuilder (Open With > WindowBuilder Editor), change the GUI
				 * and create the page again (context menu of this file: Create JavaScript App).
				 * The page is in web/%1$s: index.html is created from this GUI, app.js
				 * contains the JavaScript code.
				 *
				 * The ids of the HTML elements are the names of the variables (scaleWalkers).
				 * The widgets: Scale, Slider (a range input), Spinner (a number input), Text,
				 * Combo, List (select), Button (button, check box, radio buttons: the name of
				 * the Group), DateTime, Canvas (canvas), Table (table), Label (text; a name
				 * "out...": an output), Group (fieldset), TabFolder (tabs). The layouts:
				 * GridLayout (a CSS grid), FillLayout, RowLayout.
				 */
				public class %1$s extends Composite {

					private Scale scaleWalkers;
					private Scale scaleSteps;
					private Button chkEndPoints;
					private Button btnRun;
					private Canvas canvasWalks;
					private Label outSummary;

					public %1$s(Composite parent, int style) {
						super(parent, style);
						setLayout(new GridLayout(2, false));

						Label lblWalkers = new Label(this, SWT.NONE);
						lblWalkers.setText("Walkers");

						scaleWalkers = new Scale(this, SWT.NONE);
						scaleWalkers.setMinimum(1);
						scaleWalkers.setMaximum(200);
						scaleWalkers.setSelection(20);
						scaleWalkers.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

						Label lblSteps = new Label(this, SWT.NONE);
						lblSteps.setText("Steps");

						scaleSteps = new Scale(this, SWT.NONE);
						scaleSteps.setMinimum(10);
						scaleSteps.setMaximum(2000);
						scaleSteps.setIncrement(10);
						scaleSteps.setSelection(500);
						scaleSteps.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

						chkEndPoints = new Button(this, SWT.CHECK);
						chkEndPoints.setSelection(true);
						chkEndPoints.setText("Show the end points");

						btnRun = new Button(this, SWT.NONE);
						btnRun.setText("New walks");

						canvasWalks = new Canvas(this, SWT.BORDER);
						GridData gdCanvas = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
						gdCanvas.heightHint = 400;
						gdCanvas.widthHint = 600;
						canvasWalks.setLayoutData(gdCanvas);

						outSummary = new Label(this, SWT.NONE);
						outSummary.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
						outSummary.setText("(the summary)");
					}
				}
				""".formatted(className);
	}

	/* The JavaScript of the example: random walks on a canvas */
	static final String JS = """
			// The code of the app (WindowBuilder JavaScript). Bio7 writes this file only
			// once: your code stays when index.html is created again.

			// The elements of the page (the ids are the names of the variables of the GUI class)
			const scaleWalkers = document.getElementById("scaleWalkers");
			const scaleSteps = document.getElementById("scaleSteps");
			const chkEndPoints = document.getElementById("chkEndPoints");
			const btnRun = document.getElementById("btnRun");
			const canvasWalks = document.getElementById("canvasWalks");
			const outSummary = document.getElementById("outSummary");

			let walks = [];

			// Random walks on a lattice: each step one cell up, down, left or right
			function simulate() {
			  const n = Number(scaleWalkers.value);
			  const steps = Number(scaleSteps.value);
			  walks = [];
			  for (let w = 0; w < n; w++) {
			    let x = 0, y = 0;
			    const path = [[0, 0]];
			    for (let s = 0; s < steps; s++) {
			      const d = Math.floor(Math.random() * 4);
			      if (d === 0) x++; else if (d === 1) x--; else if (d === 2) y++; else y--;
			      path.push([x, y]);
			    }
			    walks.push(path);
			  }
			  draw();
			}

			function draw() {
			  const g = canvasWalks.getContext("2d");
			  const w = canvasWalks.width, h = canvasWalks.height;
			  g.clearRect(0, 0, w, h);
			  const steps = Number(scaleSteps.value);
			  /* The scale: 3 standard deviations of the end points (sqrt(steps)) fit */
			  const scale = Math.min(w, h) / (6 * Math.sqrt(steps));
			  let sum = 0;
			  walks.forEach((path, i) => {
			    g.strokeStyle = "hsl(" + (i * 137.5 % 360) + ", 60%, 50%)";
			    g.lineWidth = 1;
			    g.beginPath();
			    path.forEach(([x, y], k) => {
			      const px = w / 2 + x * scale, py = h / 2 - y * scale;
			      if (k === 0) g.moveTo(px, py); else g.lineTo(px, py);
			    });
			    g.stroke();
			    const [ex, ey] = path[path.length - 1];
			    sum += ex * ex + ey * ey;
			    if (chkEndPoints.checked) {
			      g.fillStyle = g.strokeStyle;
			      g.beginPath();
			      g.arc(w / 2 + ex * scale, h / 2 - ey * scale, 4, 0, 2 * Math.PI);
			      g.fill();
			    }
			  });
			  const msd = sum / walks.length;
			  outSummary.textContent = walks.length + " walks of " + steps + " steps: mean square distance " + msd.toFixed(1)
			    + " (expected: " + steps + ")";
			}

			btnRun.addEventListener("click", simulate);
			scaleWalkers.addEventListener("change", simulate);
			scaleSteps.addEventListener("change", simulate);
			chkEndPoints.addEventListener("change", draw);
			simulate();
			""";
}
