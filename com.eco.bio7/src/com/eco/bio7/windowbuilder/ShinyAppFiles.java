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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The files of a WindowBuilder Shiny app (in the folder or project of its
 * location, see OutputLocation): ui.R (written each time) and server.R (written
 * once, the code of the user stays).
 */
public final class ShinyAppFiles {

	private ShinyAppFiles() {
	}

	/**
	 * Writes the app of a GUI class into a folder or project (created if needed).
	 * server: the code of server.R if it doesn't exist (null: the template of the
	 * converter). Returns the elements which server.R has no code for (if it
	 * existed).
	 */
	public static List<String> write(IContainer app, WindowBuilderShiny.Result result, String server, IProgressMonitor monitor) throws CoreException {
		OutputLocation.ensure(app, monitor);
		put(app.getFile(new org.eclipse.core.runtime.Path("ui.R")), result.ui(), monitor);
		IFile serverFile = app.getFile(new org.eclipse.core.runtime.Path("server.R"));
		List<String> missing = new ArrayList<>();
		if (!serverFile.exists()) {
			put(serverFile, server != null ? server : result.server(), monitor);
		} else {
			/* The new elements of the GUI without code in server.R */
			String code;
			try (java.io.InputStream in = serverFile.getContents(true)) {
				code = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			} catch (java.io.IOException e) {
				code = "";
			}
			for (String b : result.buttons()) {
				if (!code.contains("input$" + b)) {
					missing.add(WindowBuilderShiny.stub(b, null));
				}
			}
			for (Map.Entry<String, String> o : result.outputs().entrySet()) {
				if (!code.contains("output$" + o.getKey())) {
					missing.add(WindowBuilderShiny.stub(o.getKey(), o.getValue()));
				}
			}
		}
		return missing;
	}

	private static void put(IFile file, String text, IProgressMonitor monitor) throws CoreException {
		ByteArrayInputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
		if (file.exists()) {
			file.setContents(in, true, true, monitor);
		} else {
			file.create(in, true, monitor);
		}
	}
}
