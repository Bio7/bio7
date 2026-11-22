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

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.eco.bio7.wizards.java.SimulationModelWizardPage.Kind;

/**
 * Creates a Java project with a simulation model of the simulation API
 * (com.eco.bio7.simulation): a 2D grid with states or values (ImageJ) or agents
 * (3D view), with a comment which explains the API, and an optional settings
 * panel for the WindowBuilder.
 */
public class SimulationModelWizard extends Wizard implements INewWizard {

	private SimulationModelWizardPage page;

	public SimulationModelWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle("New Simulation Project");
	}

	@Override
	public void addPages() {
		page = new SimulationModelWizardPage();
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		final String project = page.getProjectName();
		final String name = page.getClassName();
		final Kind kind = page.getKind();
		final boolean panel = page.isSettingsPanel();
		final boolean parallel = page.isParallel();
		final SimulationModelWizardPage.Language language = page.getLanguage();
		IRunnableWithProgress op = monitor -> {
			try {
				if (language == SimulationModelWizardPage.Language.JAVA) {
					create(project, name, kind, panel, parallel, monitor);
				} else {
					createScript(project, name, kind, language, monitor);
				}
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			} finally {
				monitor.done();
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			MessageDialog.openError(getShell(), "Error", e.getTargetException().getMessage());
			return false;
		}
		return true;
	}

	/** Creates the project and the files (also for scripts and tests). */
	public static IFile create(String project, String name, Kind kind, boolean panel, IProgressMonitor monitor) throws CoreException {
		return create(project, name, kind, panel, false, monitor);
	}

	/** Creates the project and the files; parallel: step() with parallel threads of Java. */
	public static IFile create(String project, String name, Kind kind, boolean panel, boolean parallel, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Creating the simulation project", 3);
		CreateBio7JdtProject jdt = new CreateBio7JdtProject(project, monitor);
		monitor.worked(1);
		IContainer src = jdt.getSourceFolder();
		IFile file = src.getFile(new Path(name + ".java"));
		put(file, SimulationTemplates.model(name, kind, panel, parallel), monitor);
		if (panel) {
			put(src.getFile(new Path(name + "Settings.java")), SimulationTemplates.panel(name, kind), monitor);
		}
		monitor.worked(1);
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			IWorkbenchPage p = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			try {
				IDE.openEditor(p, file, true);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		});
		monitor.worked(1);
		return file;
	}

	/** Creates a project with a model of Python or R (a script of the simulation API). */
	public static IFile createScript(String project, String name, Kind kind, SimulationModelWizardPage.Language language, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Creating the simulation project", 3);
		org.eclipse.core.resources.IProject p = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().getProject(project);
		if (!p.exists()) {
			p.create(monitor);
		}
		if (!p.isOpen()) {
			p.open(monitor);
		}
		monitor.worked(1);
		boolean python = language == SimulationModelWizardPage.Language.PYTHON;
		IFile file = p.getFile(ScriptTemplates.fileName(name) + (python ? ".py" : ".R"));
		put(file, python ? ScriptTemplates.python(name, kind) : ScriptTemplates.r(name, kind), monitor);
		monitor.worked(1);
		PlatformUI.getWorkbench().getDisplay().asyncExec(() -> {
			IWorkbenchPage wp = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			try {
				IDE.openEditor(wp, file, true);
			} catch (PartInitException e) {
				e.printStackTrace();
			}
		});
		monitor.worked(1);
		return file;
	}

	private static void put(IFile file, String text, IProgressMonitor monitor) throws CoreException {
		ByteArrayInputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
		if (file.exists()) {
			file.setContents(in, true, true, monitor);
		} else {
			file.create(in, true, monitor);
		}
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
	}
}
