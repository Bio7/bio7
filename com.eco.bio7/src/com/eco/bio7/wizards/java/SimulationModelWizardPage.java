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

import javax.lang.model.SourceVersion;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * The page of the simulation wizard: the project, the language (Java, Python
 * or R), the class (the model) and the kind of the model (a discrete or
 * continuous 2D grid in ImageJ or agents in the 3D view) and for Java an
 * optional settings panel for the WindowBuilder and parallel threads.
 */
public class SimulationModelWizardPage extends WizardPage {

	/** The kinds of the models. */
	public enum Kind {
		GRID("2D grid with states (discrete, ImageJ)", "Cells with integer states and a color for each state, e.g., a cellular automaton, a forest fire or an epidemic (GridModel)."),
		VALUES("2D grid with values (continuous, ImageJ)", "Cells with double values and a color map, e.g., a diffusion, a reaction-diffusion or a landscape model (ValueGridModel)."),
		AGENTS("3D agents (3D view)", "Individuals with a position in a 3D box, e.g., a swarm, animals or particles (AgentModel).");

		final String label, description;

		Kind(String label, String description) {
			this.label = label;
			this.description = description;
		}
	}

	/** The languages of the models. */
	public enum Language {
		JAVA("Java", "A Java class (compiled in Bio7), the fastest models, an optional WindowBuilder panel and parallel threads."),
		PYTHON("Python", "A Python script with numpy (the embedded Python of Bio7); the page runs it with Pyodide."),
		R("R", "An R script (the embedded R of Bio7); the page runs it with webR.");

		final String label, description;

		Language(String label, String description) {
			this.label = label;
			this.description = description;
		}
	}

	private Text projectText, classText;
	private Kind kind = Kind.GRID;
	private Language language = Language.JAVA;
	private Button settingsButton, parallelButton;
	private Label classLabel, note;

	public SimulationModelWizardPage() {
		super("simulationModelPage");
		setTitle("Simulation Model");
		setDescription("A project with a simulation model in Java, Python or R: runs live in Bio7 and exports as an HTML page.");
	}

	@Override
	public void createControl(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.verticalSpacing = 9;
		c.setLayout(layout);

		new Label(c, SWT.NONE).setText("&Project:");
		projectText = new Text(c, SWT.BORDER | SWT.SINGLE);
		projectText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		projectText.setText(uniqueProject("My_Simulation"));
		projectText.addModifyListener(e -> validate());

		Group languages = new Group(c, SWT.NONE);
		languages.setText("Language");
		languages.setLayout(new GridLayout(3, false));
		languages.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		Label languageNote = new Label(languages, SWT.WRAP);
		for (Language l : Language.values()) {
			Button radio = new Button(languages, SWT.RADIO);
			radio.setText(l.label);
			radio.setSelection(l == language);
			radio.addListener(SWT.Selection, e -> {
				if (radio.getSelection()) {
					language = l;
					languageNote.setText(l.description);
					updateLanguage();
				}
			});
		}
		languageNote.moveBelow(null);
		languageNote.setText(language.description);
		GridData ln = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		ln.widthHint = 420;
		languageNote.setLayoutData(ln);

		classLabel = new Label(c, SWT.NONE);
		classLabel.setText("&Class:");
		classText = new Text(c, SWT.BORDER | SWT.SINGLE);
		classText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		classText.setText("MyModel");
		classText.addModifyListener(e -> validate());

		Group group = new Group(c, SWT.NONE);
		group.setText("Kind of the model");
		group.setLayout(new GridLayout(1, false));
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd.verticalIndent = 6;
		group.setLayoutData(gd);
		for (Kind k : Kind.values()) {
			Button radio = new Button(group, SWT.RADIO);
			radio.setText(k.label);
			radio.setSelection(k == kind);
			radio.addListener(SWT.Selection, e -> {
				if (radio.getSelection()) {
					kind = k;
				}
			});
			Label d = new Label(group, SWT.WRAP);
			d.setText(k.description);
			GridData dd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			dd.horizontalIndent = 22;
			dd.widthHint = 420;
			d.setLayoutData(dd);
		}

		settingsButton = new Button(c, SWT.CHECK);
		settingsButton.setText("Create a settings panel for the WindowBuilder (optional)");
		settingsButton.setToolTipText("A Composite (open it with the WindowBuilder editor) for the settings in Bio7; else the settings are created from the Param fields");
		GridData sd = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		sd.verticalIndent = 6;
		settingsButton.setLayoutData(sd);

		parallelButton = new Button(c, SWT.CHECK);
		parallelButton.setText("Parallel computation in Java (faster for large grids and many agents)");
		parallelButton.setToolTipText("step() computes the rows or agents in parallel threads: IntStream.range(0, n).parallel().forEach(...) with randomAt(index, k) for the random numbers (the same results in any order of the threads and in the browser)");
		parallelButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		note = new Label(c, SWT.WRAP);
		note.setText(noteText());
		GridData nd = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		nd.widthHint = 460;
		nd.verticalIndent = 10;
		note.setLayoutData(nd);

		validate();
		setControl(c);
	}

	private String noteText() {
		return switch (language) {
		case JAVA -> "Compile the class (the model opens in ImageJ or in the 3D view with a \"Settings\" view), start it with the Play/Pause button and export it with \"Export Simulation as HTML...\". The comment at the start of the class explains the API.";
		case PYTHON -> "Run the script in the Python editor (the model opens in ImageJ or in the 3D view with a \"Settings\" view), start it with the Play/Pause button and export it with \"Export Simulation as HTML...\". The comment at the start of the script explains the API.";
		case R -> "Source the script in the R editor (the model opens in ImageJ or in the 3D view with a \"Settings\" view), start it with the Play/Pause button and export it with \"Export Simulation as HTML...\". The comment at the start of the script explains the API.";
		};
	}

	/* The options of Java (the WindowBuilder panel, parallel threads) only for Java */
	private void updateLanguage() {
		boolean java = language == Language.JAVA;
		settingsButton.setEnabled(java);
		parallelButton.setEnabled(java);
		if (!java) {
			settingsButton.setSelection(false);
			parallelButton.setSelection(false);
		}
		classLabel.setText(java ? "&Class:" : "&Model:");
		note.setText(noteText());
		note.getParent().layout(true, true);
		validate();
	}

	private static String uniqueProject(String base) {
		String name = base;
		for (int i = 2; ResourcesPlugin.getWorkspace().getRoot().getProject(name).exists(); i++) {
			name = base + "_" + i;
		}
		return name;
	}

	private void validate() {
		String project = getProjectName(), cls = getClassName();
		String error = null;
		if (project.isEmpty()) {
			error = "Enter the name of the project";
		} else if (!ResourcesPlugin.getWorkspace().validateName(project, IResource.PROJECT).isOK()) {
			error = "The name of the project is not valid";
		} else {
			IResource existing = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(project));
			if (existing != null && existing.exists()) {
				error = "A project with this name already exists";
			} else if (cls.isEmpty()) {
				error = "Enter the name of the class";
			} else if (!SourceVersion.isIdentifier(cls) || SourceVersion.isKeyword(cls) || (language != Language.JAVA && !cls.matches("[A-Za-z][A-Za-z0-9_]*"))) {
				error = language == Language.JAVA ? "The name of the class must be a Java name (e.g., ForestModel)" : "The name of the model must be a name of letters and digits (e.g., ForestModel)";
			} else if (!Character.isUpperCase(cls.charAt(0))) {
				error = "The name of the class should start with an upper case letter";
			}
		}
		setErrorMessage(error);
		setPageComplete(error == null);
	}

	public String getProjectName() {
		return projectText.getText().trim();
	}

	public String getClassName() {
		return classText.getText().trim();
	}

	public Language getLanguage() {
		return language;
	}

	public Kind getKind() {
		return kind;
	}

	public boolean isParallel() {
		return parallelButton.getSelection();
	}

	public boolean isSettingsPanel() {
		return settingsButton.getSelection();
	}
}
