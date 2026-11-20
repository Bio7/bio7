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

import com.eco.bio7.windowbuilder.ShinyAppFiles;
import com.eco.bio7.windowbuilder.OutputLocation;
import com.eco.bio7.windowbuilder.WindowBuilderShiny;

/**
 * WindowBuilder Shiny: a Java project with a GUI class (designed with the
 * WindowBuilder) and the Shiny app of it (shiny/<class>: ui.R from the GUI,
 * server.R with example code). Change the GUI and create the app again with
 * Create Shiny App (context menu of the class).
 */
public class WindowBuilderShinyWizard extends Bio7JavaTemplateProjectWizard {

	@Override
	protected String title() {
		return "WindowBuilder Shiny App Project";
	}

	@Override
	protected String description() {
		return "This wizard creates a Java project with a GUI (WindowBuilder) and its Shiny app (shiny/<class>: ui.R from the GUI, server.R with R code).";
	}

	@Override
	protected String defaultProject() {
		return "My_Shiny_App";
	}

	@Override
	protected String defaultFile() {
		return "ShinyGui.java";
	}

	@Override
	protected Map<String, String> files(String className) {
		Map<String, String> files = new LinkedHashMap<>();
		files.put(className + ".java", gui(className));
		return files;
	}

	@Override
	protected String option() {
		return "Create the app in its own project (<project>_Shiny)";
	}

	@Override
	protected void afterCreate(IProject project, IContainer src, String className, boolean ownProject, IProgressMonitor monitor) throws CoreException {
		IFile gui = src.getFile(new org.eclipse.core.runtime.Path(className + ".java"));
		/* The app: in its own project or in the folder shiny/<class> of the project (remembered for the GUI class) */
		IContainer target = ownProject ? project.getWorkspace().getRoot().getProject(project.getName() + "_Shiny") : OutputLocation.inProject(gui, OutputLocation.Kind.SHINY);
		WindowBuilderShiny.Result result = WindowBuilderShiny.convert(gui(className), "Distribution Explorer", gui.getName() + " (" + gui.getFullPath().removeLastSegments(1) + ")");
		ShinyAppFiles.write(target, result, SERVER, monitor);
		OutputLocation.store(gui, OutputLocation.Kind.SHINY, target);
	}

	/* The GUI: inputs, a button, the outputs (a plot, a text, a table) */
	static String gui(String className) {
		return """
				import org.eclipse.swt.SWT;
				import org.eclipse.swt.layout.GridData;
				import org.eclipse.swt.layout.GridLayout;
				import org.eclipse.swt.layout.RowLayout;
				import org.eclipse.swt.widgets.Button;
				import org.eclipse.swt.widgets.Canvas;
				import org.eclipse.swt.widgets.Combo;
				import org.eclipse.swt.widgets.Composite;
				import org.eclipse.swt.widgets.Group;
				import org.eclipse.swt.widgets.Label;
				import org.eclipse.swt.widgets.Scale;
				import org.eclipse.swt.widgets.Table;

				/*
				 * WindowBuilder Shiny: the design of a Shiny app. Open this class with the
				 * WindowBuilder (Open With > WindowBuilder Editor), change the GUI and create the
				 * app again (context menu of this file: Create Shiny App). The app is in
				 * shiny/%1$s: ui.R is created from this GUI, server.R contains the R code.
				 *
				 * The ids of Shiny are the names of the variables (input$scaleN). The widgets:
				 *   Scale, Slider        sliderInput          Spinner           numericInput
				 *   Text                 textInput            Combo, List       selectInput
				 *   Button (check)       checkboxInput        Button (radio)    radioButtons (id: the Group)
				 *   Button (push)        actionButton         DateTime          dateInput
				 *   Canvas               plotOutput           Table             tableOutput
				 *   Label                text (a name "out...": textOutput)
				 *   Group                a panel with a title TabFolder         tabsetPanel
				 * The layouts: GridLayout (rows and columns), FillLayout, RowLayout.
				 */
				public class %1$s extends Composite {

					private Scale scaleN;
					private Combo comboDistribution;
					private Button chkDensity;
					private Group grpColor;
					private Button rbBlue;
					private Button rbGreen;
					private Button btnSample;
					private Canvas canvasPlot;
					private Label outSummary;
					private Table tableStats;

					public %1$s(Composite parent, int style) {
						super(parent, style);
						setLayout(new GridLayout(2, false));

						Label lblN = new Label(this, SWT.NONE);
						lblN.setText("Number of values");

						scaleN = new Scale(this, SWT.NONE);
						scaleN.setMinimum(10);
						scaleN.setMaximum(1000);
						scaleN.setIncrement(10);
						scaleN.setSelection(200);
						scaleN.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

						Label lblDistribution = new Label(this, SWT.NONE);
						lblDistribution.setText("Distribution");

						comboDistribution = new Combo(this, SWT.READ_ONLY);
						comboDistribution.setItems(new String[] { "Normal", "Uniform", "Exponential" });
						comboDistribution.select(0);
						comboDistribution.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

						chkDensity = new Button(this, SWT.CHECK);
						chkDensity.setText("Show the density");

						grpColor = new Group(this, SWT.NONE);
						grpColor.setText("Color");
						grpColor.setLayout(new RowLayout(SWT.HORIZONTAL));

						rbBlue = new Button(grpColor, SWT.RADIO);
						rbBlue.setSelection(true);
						rbBlue.setText("Blue");

						rbGreen = new Button(grpColor, SWT.RADIO);
						rbGreen.setText("Green");

						btnSample = new Button(this, SWT.NONE);
						btnSample.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
						btnSample.setText("New sample");

						canvasPlot = new Canvas(this, SWT.BORDER);
						GridData gdPlot = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
						gdPlot.heightHint = 250;
						canvasPlot.setLayoutData(gdPlot);

						outSummary = new Label(this, SWT.NONE);
						outSummary.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
						outSummary.setText("(the summary)");

						tableStats = new Table(this, SWT.BORDER | SWT.FULL_SELECTION);
						tableStats.setHeaderVisible(true);
						GridData gdTable = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
						gdTable.heightHint = 120;
						tableStats.setLayoutData(gdTable);
					}
				}
				""".formatted(className);
	}

	/* The server of the example (the ids of the GUI) */
	static final String SERVER = """
			# The server of the Shiny app (WindowBuilder Shiny). Bio7 writes this file only
			# once: your code stays when the user interface (ui.R) is created again.
			library(shiny)

			function(input, output, session) {

			  # A new sample when the inputs change or the button is pressed
			  values <- reactive({
			    input$btnSample
			    n <- input$scaleN
			    switch(input$comboDistribution,
			      Uniform = runif(n),
			      Exponential = rexp(n),
			      rnorm(n))
			  })

			  output$canvasPlot <- renderPlot({
			    x <- values()
			    col <- if (identical(input$grpColor, "Green")) "#55A868" else "#4C72B0"
			    hist(x, freq = !isTRUE(input$chkDensity), col = col, border = "white",
			         main = paste(input$comboDistribution, "distribution"), xlab = "x")
			    if (isTRUE(input$chkDensity)) lines(density(x), lwd = 2)
			  })

			  output$outSummary <- renderText({
			    x <- values()
			    sprintf("n = %d, mean = %.3f, standard deviation = %.3f", length(x), mean(x), sd(x))
			  })

			  output$tableStats <- renderTable({
			    s <- summary(values())
			    data.frame(Statistic = names(s), Value = as.numeric(s))
			  })
			}
			""";
}
