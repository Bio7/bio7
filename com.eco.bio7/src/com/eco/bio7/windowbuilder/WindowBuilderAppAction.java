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

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.eco.bio7.popup.actions.ShinyApps;

/**
 * Creates the app of a GUI class (a Composite designed with the WindowBuilder):
 * a Shiny app or an HTML page with JavaScript, at the location of the GUI class
 * (asked the first time, see OutputLocation). The generated file (ui.R,
 * index.html) is written each time, the code of the user (server.R, app.js)
 * once.
 */
public abstract class WindowBuilderAppAction implements IObjectActionDelegate {

	private IFile file;
	private Shell shell;
	private final OutputLocation.Kind kind;
	private final boolean ask;

	/** ask: the location is chosen again (Change Location). */
	protected WindowBuilderAppAction(OutputLocation.Kind kind, boolean ask) {
		this.kind = kind;
		this.ask = ask;
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		file = selection instanceof IStructuredSelection ss && ss.getFirstElement() instanceof IFile f ? f : null;
	}

	@Override
	public void run(IAction action) {
		if (file == null) {
			return;
		}
		String title = "WindowBuilder " + (kind == OutputLocation.Kind.SHINY ? "Shiny" : "JavaScript");
		try {
			String source;
			try (java.io.InputStream in = file.getContents(true)) {
				source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
			/* The GUI file in the header of the generated file (also from another project) */
			String origin = file.getName() + " (" + file.getFullPath().removeLastSegments(1) + ")";
			boolean shiny = kind == OutputLocation.Kind.SHINY;
			WindowBuilderShiny.Result shinyResult = shiny ? WindowBuilderShiny.convert(source, null, origin) : null;
			WindowBuilderShiny.WebResult webResult = shiny ? null : WindowBuilderShiny.convertWeb(source, null, origin);
			boolean empty = shiny ? shinyResult.inputs().isEmpty() && shinyResult.buttons().isEmpty() && shinyResult.outputs().isEmpty() : webResult.ids().isEmpty();
			if (empty) {
				MessageDialog.openInformation(shell, title, "No widgets found in " + file.getName() + ".\n\nThe class must be a GUI (a Composite) designed with the WindowBuilder.");
				return;
			}
			IContainer previous = OutputLocation.stored(file, kind);
			IContainer target = OutputLocation.target(file, kind, shell, ask);
			if (target == null) {
				return;
			}
			List<String> missing = shiny ? ShinyAppFiles.write(target, shinyResult, null, null) : WebAppFiles.write(target, webResult, null, null);
			target.refreshLocal(IResource.DEPTH_INFINITE, null);
			IFile generated = target.getFile(new Path(shiny ? "ui.R" : "index.html"));
			IFile code = target.getFile(new Path(shiny ? "server.R" : "app.js"));
			try {
				IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), code, true);
			} catch (PartInitException e) {
				/* Created anyway */
			}
			StringBuilder msg = new StringBuilder("The " + (shiny ? "Shiny app" : "app") + " is in " + target.getFullPath() + " ("
					+ generated.getName() + " from the GUI, " + code.getName() + " with your code).");
			if (previous != null && !previous.equals(target)) {
				msg.append("\n\nThe files at the former location (").append(previous.getFullPath()).append(") were not removed.");
			}
			List<String> notes = shiny ? shinyResult.notes() : webResult.notes();
			if (!notes.isEmpty()) {
				msg.append("\n\nNot converted: ").append(String.join("; ", notes));
			}
			if (!missing.isEmpty()) {
				msg.append(shiny ? "\n\nserver.R has no code yet for these new elements (copy it into the server function):\n" : "\n\napp.js doesn't use these new elements yet:\n")
						.append(String.join("", missing));
			}
			msg.append(shiny ? "\n\nStart the app now?" : "\n\nOpen the app now?");
			if (MessageDialog.openQuestion(shell, title, msg.toString())) {
				if (shiny) {
					ShinyApps.start(target.getLocation().toString());
				} else {
					WebAppFiles.open(generated);
				}
			}
		} catch (Exception e) {
			MessageDialog.openError(shell, title, "The app could not be created:\n" + e.getMessage());
		}
	}
}
