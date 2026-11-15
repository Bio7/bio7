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
package com.eco.bio7.wizards;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
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
 * Creates a plain markdown file (*.md): it is converted to HTML in Java, no R
 * is needed (the R Markdown wizard creates *.Rmd files with R chunks).
 */
public class MarkdownPluginWizard extends Wizard implements INewWizard {

	private RMarkdownWizardPage page;
	private ISelection selection;

	public MarkdownPluginWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle("New Markdown File");
	}

	@Override
	public void addPages() {
		page = new RMarkdownWizardPage(selection, true);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		String containerName = page.getContainerName();
		String fileName = page.getFileName();
		String title = page.getTextTitle().getText().trim();
		String author = page.getTextAuthor().getText().trim();
		try {
			getContainer().run(true, false, monitor -> {
				try {
					IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
					IResource resource = root.findMember(new Path(containerName));
					if (!(resource instanceof IContainer container) || !resource.exists()) {
						throw new CoreException(new Status(IStatus.ERROR, "com.eco.bio7", "Container \"" + containerName + "\" does not exist."));
					}
					IFile file = container.getFile(new Path(fileName));
					byte[] bytes = contents(title, author).getBytes(StandardCharsets.UTF_8);
					if (file.exists()) {
						file.setContents(new ByteArrayInputStream(bytes), true, true, monitor);
					} else {
						file.create(new ByteArrayInputStream(bytes), true, monitor);
					}
					getShell().getDisplay().asyncExec(() -> {
						try {
							IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), file, true);
						} catch (PartInitException e) {
							/* Created anyway */
						}
					});
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
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

	/* A short template which shows the markdown syntax */
	private static String contents(String title, String author) {
		String nl = System.lineSeparator();
		StringBuilder sb = new StringBuilder();
		sb.append("---").append(nl);
		sb.append("title: ").append(title.isEmpty() ? "Markdown" : title).append(nl);
		if (!author.isEmpty()) {
			sb.append("author: ").append(author).append(nl);
		}
		sb.append("date: ").append(LocalDate.now()).append(nl);
		sb.append("---").append(nl).append(nl);
		String[] body = { "## Introduction", "", "Text with **bold**, *italic*, `code` and a [link](https://bio7.org).", "", "- A list item",
				"- Another item", "  - A nested item", "", "1. First", "2. Second", "", "> A quote.", "", "| Column A | Column B |",
				"|----------|----------|", "| 1        | 2        |", "", "```", "Code block", "```", "", "- [x] A done task", "- [ ] An open task",
				"" };
		for (String line : body) {
			sb.append(line).append(nl);
		}
		return sb.toString();
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}
