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
package com.eco.bio7.windowbuilder;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

/**
 * Where the app of a GUI class is created (WindowBuilder Shiny, WindowBuilder
 * JavaScript): a new project, a folder of the project of the GUI class or a
 * folder of the workspace. The choice is stored as a property of the GUI file
 * (the next runs write to the same place without asking).
 */
public final class OutputLocation {

	/** The kinds of apps. */
	public enum Kind {
		SHINY("shiny", "_Shiny", "Shiny App"), WEB("web", "_Web", "JavaScript App");

		final String folder;
		final String suffix;
		final String label;

		Kind(String folder, String suffix, String label) {
			this.folder = folder;
			this.suffix = suffix;
			this.label = label;
		}

		QualifiedName key() {
			return new QualifiedName("com.eco.bio7.windowbuilder", "target." + folder);
		}
	}

	private OutputLocation() {
	}

	/** The folder in the project of the GUI class (shiny/<class>, web/<class>). */
	public static IFolder inProject(IFile gui, Kind kind) {
		return gui.getProject().getFolder(kind.folder).getFolder(className(gui));
	}

	/** The name of a new project for the app (<class>_Shiny, <class>_Web). */
	public static String newProjectName(IFile gui, Kind kind) {
		return className(gui) + kind.suffix;
	}

	static String className(IFile gui) {
		return gui.getName().replaceFirst("\\.java$", "");
	}

	/** The stored target of a GUI class, null if none (or it doesn't exist anymore). */
	public static IContainer stored(IFile gui, Kind kind) {
		try {
			String path = gui.getPersistentProperty(kind.key());
			if (path == null) {
				return null;
			}
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IPath p = new Path(path);
			if (p.segmentCount() == 1) {
				IProject project = root.getProject(p.segment(0));
				return project.exists() ? project : null;
			}
			IResource r = root.findMember(p);
			if (r instanceof IContainer c) {
				return c;
			}
			/* A folder which was removed: created again */
			return root.getFolder(p).getProject().exists() ? root.getFolder(p) : null;
		} catch (CoreException e) {
			return null;
		}
	}

	/** Stores the target of a GUI class. */
	public static void store(IFile gui, Kind kind, IContainer target) throws CoreException {
		gui.setPersistentProperty(kind.key(), target.getFullPath().toString());
	}

	/**
	 * The target of the app of a GUI class: the stored one, else (or with ask) the
	 * choice of the dialog; null if the dialog was canceled. A new project or
	 * folder is created.
	 */
	public static IContainer target(IFile gui, Kind kind, Shell shell, boolean ask) throws CoreException {
		IContainer stored = stored(gui, kind);
		if (stored != null && !ask) {
			ensure(stored, null);
			return stored;
		}
		LocationDialog d = new LocationDialog(shell, gui, kind, stored);
		if (d.open() != IDialogConstants.OK_ID || d.result == null) {
			return null;
		}
		ensure(d.result, null);
		store(gui, kind, d.result);
		return d.result;
	}

	/** Creates the project or the folders of a target (if needed). */
	public static void ensure(IContainer target, IProgressMonitor monitor) throws CoreException {
		if (target instanceof IProject p) {
			if (!p.exists()) {
				p.create(monitor);
			}
			if (!p.isOpen()) {
				p.open(monitor);
			}
		} else if (target instanceof IFolder f && !f.exists()) {
			ensure(f.getParent(), monitor);
			f.create(true, true, monitor);
		}
	}

	/* The dialog: a new project, a folder of the project, a folder of the workspace */
	private static final class LocationDialog extends TitleAreaDialog {

		private final IFile gui;
		private final Kind kind;
		private final IContainer current;
		private Button rbProject;
		private Button rbInProject;
		private Button rbFolder;
		private Text txtProject;
		private Text txtFolder;
		IContainer result;

		LocationDialog(Shell shell, IFile gui, Kind kind, IContainer current) {
			super(shell);
			this.gui = gui;
			this.kind = kind;
			this.current = current;
		}

		@Override
		public void create() {
			super.create();
			setTitle("Location of the " + kind.label);
			setMessage("Where is the " + kind.label + " of " + gui.getName() + " created? Bio7 remembers the choice for this GUI class.");
			getShell().setText("WindowBuilder " + (kind == Kind.SHINY ? "Shiny" : "JavaScript"));
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite area = (Composite) super.createDialogArea(parent);
			Composite c = new Composite(area, SWT.NONE);
			c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			c.setLayout(new GridLayout(3, false));

			rbProject = new Button(c, SWT.RADIO);
			rbProject.setText("A new project (to edit and share the app on its own):");
			rbProject.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
			new Label(c, SWT.NONE).setText("    Project");
			txtProject = new Text(c, SWT.BORDER);
			txtProject.setText(newProjectName(gui, kind));
			txtProject.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

			rbInProject = new Button(c, SWT.RADIO);
			rbInProject.setText("A folder in the project of the GUI class: " + inProject(gui, kind).getProjectRelativePath());
			rbInProject.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));

			rbFolder = new Button(c, SWT.RADIO);
			rbFolder.setText("A folder of the workspace:");
			rbFolder.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
			new Label(c, SWT.NONE).setText("    Folder");
			txtFolder = new Text(c, SWT.BORDER);
			txtFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			Button browse = new Button(c, SWT.PUSH);
			browse.setText("Browse...");
			browse.addListener(SWT.Selection, e -> {
				ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), ResourcesPlugin.getWorkspace().getRoot(), true,
						"The folder of the app (the files are created in it)");
				if (dialog.open() == ContainerSelectionDialog.OK && dialog.getResult().length == 1) {
					txtFolder.setText(dialog.getResult()[0].toString());
					select(rbFolder);
				}
			});

			/* The current choice (else: a new project) */
			if (current instanceof IProject p) {
				txtProject.setText(p.getName());
				select(rbProject);
			} else if (current != null && current.equals(inProject(gui, kind))) {
				select(rbInProject);
			} else if (current != null) {
				txtFolder.setText(current.getFullPath().toString());
				select(rbFolder);
			} else {
				select(rbProject);
			}
			txtProject.addListener(SWT.Modify, e -> select(rbProject));
			txtFolder.addListener(SWT.Modify, e -> select(rbFolder));
			return area;
		}

		private void select(Button b) {
			for (Button x : new Button[] { rbProject, rbInProject, rbFolder }) {
				x.setSelection(x == b);
			}
			validate();
		}

		private void validate() {
			String error = null;
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			if (rbProject.getSelection()) {
				String name = txtProject.getText().trim();
				if (name.isEmpty() || !ResourcesPlugin.getWorkspace().validateName(name, IResource.PROJECT).isOK()) {
					error = "Please enter a valid project name.";
				}
			} else if (rbFolder.getSelection()) {
				IPath p = new Path(txtFolder.getText().trim());
				if (p.isEmpty() || !root.getProject(p.segment(0)).exists()) {
					error = "Please select a folder of an existing project.";
				}
			}
			setErrorMessage(error);
			Button ok = getButton(IDialogConstants.OK_ID);
			if (ok != null) {
				ok.setEnabled(error == null);
			}
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
			validate();
		}

		@Override
		protected void okPressed() {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			if (rbProject.getSelection()) {
				result = root.getProject(txtProject.getText().trim());
			} else if (rbInProject.getSelection()) {
				result = inProject(gui, kind);
			} else {
				IPath p = new Path(txtFolder.getText().trim());
				result = p.segmentCount() == 1 ? root.getProject(p.segment(0)) : root.getFolder(p);
			}
			super.okPressed();
		}
	}
}
