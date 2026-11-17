/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.popup.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.eco.bio7.util.Bio7Dialog;

/** Starts the Shiny app of the R file of the active editor (toolbar button). */
public class StartShinyAppHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) {
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		IFile file = editor != null && editor.getEditorInput() instanceof IFileEditorInput fi ? fi.getFile() : null;
		/* The app is started with the saved files */
		PlatformUI.getWorkbench().saveAllEditors(true);
		String app = ShinyApps.appPath(file);
		if (app == null) {
			Bio7Dialog.message("The R file is not a Shiny app (app.R, ui.R, server.R or a file with shinyApp(...)).");
			return null;
		}
		ShinyApps.start(app);
		return null;
	}
}
