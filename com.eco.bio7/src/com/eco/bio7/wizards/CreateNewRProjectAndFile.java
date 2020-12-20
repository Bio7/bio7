package com.eco.bio7.wizards;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.actions.ToolbarCreateProjectAndFiles;
import com.eco.bio7.image.Activator;
import com.eco.bio7.util.Bio7Dialog;
import com.eco.bio7.util.Util;

public class CreateNewRProjectAndFile {

	String author;
	String title;
	Combo combo;
	String format = "";
	String selectedFormat;
	IFile fileMd = null;

	public CreateNewRProjectAndFile(String containerName, IProgressMonitor monitor, NewRProjectWizardAndFilePage page) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(containerName);
		try {
			project.create(monitor);
			project.open(monitor);
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Util.getDisplay().syncExec(new Runnable() {
			public void run() {
				try {

					// IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
					IResource resource = root.findMember(new Path(containerName));
					if (!resource.exists() || !(resource instanceof IContainer)) {
						throwCoreException("Container \"" + containerName + "\" does not exist.");
					}
					IContainer container = (IContainer) resource;

					monitor.worked(1);
					String selection = ToolbarCreateProjectAndFiles.selection;
					if (selection.equals("python")) {
						fileMd = container.getFile(new Path("Untitled.py"));

					} else if (selection.equals("jython")) {
						fileMd = container.getFile(new Path("Untitled.py"));

					} else if (selection.equals("r")) {
						fileMd = container.getFile(new Path("Untitled.R"));
					} else if (selection.equals("rmarkdown")) {
						fileMd = container.getFile(new Path("Untitled.rmd"));
					} else if (selection.equals("shiny")) {
						fileMd = container.getFile(new Path("app.R"));
					} else if (selection.equals("ijmacro")) {
						fileMd = container.getFile(new Path("Untitled.ijm"));
					} else if (selection.equals("groovy")) {
						fileMd = container.getFile(new Path("Untitled.groovy"));
					} else if (selection.equals("javascript")) {
						fileMd = container.getFile(new Path("Untitled.js"));
					} else if (selection.equals("latex")) {
						return;
					}

					String anR = "template";
					int indexR = fileMd.getName().lastIndexOf('.');
					if (indexR > 0 && indexR <= fileMd.getName().length() - 2) {
						anR = fileMd.getName().substring(0, indexR);
					}

					try {
						InputStream stream = openContentRStream(anR, selection);
						if (fileMd.exists()) {
							fileMd.setContents(stream, true, true, monitor);
						} else {
							fileMd.create(stream, true, monitor);
						}
						stream.close();
					} catch (IOException e) {
					}

					monitor.setTaskName("Opening file for editing...");
					Util.getDisplay().asyncExec(new Runnable() {
						public void run() {
							IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
							try {
								IDE.openEditor(page, fileMd, true);
								// IDE.openEditor(page, file,
								// "org.eclipse.ui.DefaultTextEditor");

							} catch (PartInitException e) {
							}
						}
					});
					monitor.worked(1);

					if (selection.equals("python")) {

						IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
						store.setValue("python_pipe", true);
						System.out.println("Changed preferences and execution to Python Interpreter!");
					} else if (selection.equals("jython")) {

						IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
						store.setValue("python_pipe", false);
						System.out.println("Changed preferences and execution to Jython Interpreter!");

					}

				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

	}

	private InputStream openContentRStream(String filename, String selection) {
		String contents = "";
		if (selection.equals("rmarkdown")) {
			contents = "---\n" + "title: \n" + "author: \n" + "date:\n" + "output: html_document\n" + "---\n" + "\n" + "## R Markdown\n" + "\n" + "Example R code:\n" + "\n" + "```{R}\n"
					+ "plot(runif(100))\n" + "```\n" + "";
		} else if (selection.equals("shiny")) {

			contents = "library(shiny)\n" + "ui <- fluidPage(\n" + "  sliderInput(\n" + "    \"points\",\n" + "    \"Number of random points:\",\n" + "    min = 10,\n" + "    max = 10000,\n"
					+ "    value = 100\n" + "  ),\n" + "  plotOutput(outputId = \"points\")\n" + ")\n" + "server <- function(input, output) {\n" + "  output$points <- renderPlot({\n"
					+ "    plot(runif(input$points))\n" + "  })\n" + "}\n" + "shinyApp(ui = ui, server = server)";

		}

		else {
			contents = "";
		}

		return new ByteArrayInputStream(contents.getBytes());
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, "com.eco.bio7", IStatus.OK, message, null);
		throw new CoreException(status);
	}

}
