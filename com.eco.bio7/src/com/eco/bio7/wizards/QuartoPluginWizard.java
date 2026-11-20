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
 * Creates a Quarto document (*.qmd) with the format in the YAML header and an
 * example with R chunks and Quarto chunk options. It opens in the R Markdown
 * editor and is rendered with Quarto (https://quarto.org, not part of Bio7).
 */
public class QuartoPluginWizard extends Wizard implements INewWizard {

	private RMarkdownWizardPage page;
	private ISelection selection;

	public QuartoPluginWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle("New Quarto Document");
	}

	@Override
	public void addPages() {
		page = new RMarkdownWizardPage(selection, false, true);
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		String containerName = page.getContainerName();
		String fileName = page.getFileName();
		String title = page.getTextTitle().getText().trim();
		String author = page.getTextAuthor().getText().trim();
		String format = switch (page.getCombo().getText()) {
		case "PDF" -> "pdf";
		case "Word" -> "docx";
		case "Typst (PDF)" -> "typst";
		case "Presentation (Reveal.js)" -> "revealjs";
		case "Presentation (Beamer PDF)" -> "beamer";
		case "Dashboard (HTML)" -> "dashboard";
		default -> "html";
		};
		try {
			getContainer().run(true, false, monitor -> {
				try {
					IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
					IResource resource = root.findMember(new Path(containerName));
					if (!(resource instanceof IContainer container) || !resource.exists()) {
						throw new CoreException(new Status(IStatus.ERROR, "com.eco.bio7", "Container \"" + containerName + "\" does not exist."));
					}
					IFile file = container.getFile(new Path(fileName));
					byte[] bytes = contents(title, author, format).getBytes(StandardCharsets.UTF_8);
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

	/* A template: the YAML header, text, R chunks with Quarto options (#|), a figure and a cross reference */
	static String contents(String title, String author, String format) {
		String nl = System.lineSeparator();
		StringBuilder sb = new StringBuilder();
		sb.append("---").append(nl);
		sb.append("title: \"").append(title.isEmpty() ? "Quarto" : title.replace("\"", "\\\"")).append("\"").append(nl);
		if (!author.isEmpty()) {
			sb.append("author: \"").append(author.replace("\"", "\\\"")).append("\"").append(nl);
		}
		sb.append("date: ").append(LocalDate.now()).append(nl);
		sb.append("format: ").append(format).append(nl);
		sb.append("---").append(nl).append(nl);
		boolean slides = format.equals("revealjs") || format.equals("beamer");
		String[] body = slides
				? new String[] { "## Data", "", "```{r}", "#| echo: true", "x <- rnorm(100)", "summary(x)", "```", "", "## A plot", "", "```{r}",
						"#| label: fig-hist", "#| fig-cap: \"A histogram of x\"", "hist(x)", "```", "" }
				: new String[] { "## Introduction", "", "Text with **bold**, *italic* and `code`. The R chunks are run when the document is rendered",
						"with Quarto; the options of a chunk are the lines `#|` at its start.", "", "```{r}", "#| label: data", "x <- rnorm(100)",
						"summary(x)", "```", "", "## A plot", "", "@fig-hist shows the histogram of `x`.", "", "```{r}", "#| label: fig-hist",
						"#| fig-cap: \"A histogram of x\"", "#| echo: false", "hist(x, col = \"steelblue\", main = \"\")", "```", "",
						"The mean of x is `r round(mean(x), 2)`.", "" };
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
