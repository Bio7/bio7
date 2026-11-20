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
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

/**
 * A Bio7 Java project with files of a template (e.g., a class with a main
 * method, or a GUI for R or Python which the WindowBuilder can edit). The
 * class name comes from the file name of the page; the first file opens in the
 * editor.
 */
public abstract class Bio7JavaTemplateProjectWizard extends Wizard implements INewWizard {

	private JavaWindowBuilderBio7ClassWizardPage page;
	private ISelection selection;

	protected Bio7JavaTemplateProjectWizard() {
		setNeedsProgressMonitor(true);
	}

	/** The title of the page. */
	protected abstract String title();

	/** The description of the page. */
	protected abstract String description();

	/** The suggested project name. */
	protected abstract String defaultProject();

	/** The suggested file name (*.java). */
	protected abstract String defaultFile();

	/** The files of the template (file name -> content; the first one opens), className: of the file name. */
	protected abstract Map<String, String> files(String className);

	/** An option of the template (a check box on the page), null: none. */
	protected String option() {
		return null;
	}

	/**
	 * After the files: more files of the template (e.g., a Shiny app). src: the
	 * source folder, option: the option of the page is selected.
	 */
	protected void afterCreate(org.eclipse.core.resources.IProject project, IContainer src, String className, boolean option, org.eclipse.core.runtime.IProgressMonitor monitor)
			throws CoreException {
	}

	@Override
	public void addPages() {
		setWindowTitle(title());
		page = new JavaWindowBuilderBio7ClassWizardPage(selection, title(), description(), defaultProject(), defaultFile());
		page.setOption(option());
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		String project = page.getContainerName();
		String fileName = page.getFileName().endsWith(".java") ? page.getFileName() : page.getFileName() + ".java";
		String className = fileName.substring(0, fileName.length() - ".java".length());
		boolean option = page.isOptionSelected();
		if (!className.matches("[A-Za-z_][A-Za-z0-9_]*")) {
			MessageDialog.openError(getShell(), "Error", "The file name must be a valid Java class name (e.g., My_Gui.java).");
			return false;
		}
		try {
			getContainer().run(true, false, monitor -> {
				try {
					monitor.beginTask("Creating " + project, 2);
					IContainer src = new CreateBio7JdtProject(project, monitor).getSourceFolder();
					IFile first = null;
					for (Map.Entry<String, String> f : files(className).entrySet()) {
						IFile file = src.getFile(new Path(f.getKey()));
						byte[] bytes = f.getValue().getBytes(StandardCharsets.UTF_8);
						if (file.exists()) {
							file.setContents(new ByteArrayInputStream(bytes), true, true, monitor);
						} else {
							file.create(new ByteArrayInputStream(bytes), true, monitor);
						}
						if (first == null) {
							first = file;
						}
					}
					afterCreate(src.getProject(), src, className, option, monitor);
					IFile open = first;
					getShell().getDisplay().asyncExec(() -> {
						try {
							IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), open, true);
						} catch (PartInitException e) {
							/* Created anyway */
						}
					});
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			});
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			MessageDialog.openError(getShell(), "Error", e.getTargetException().getMessage());
			return false;
		}
		return true;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}
