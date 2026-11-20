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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The files of a WindowBuilder JavaScript app (in the folder or project of its
 * location, see OutputLocation): index.html (written each time), app.js and
 * style.css (written once, the code of the user stays).
 */
public final class WebAppFiles {

	private WebAppFiles() {
	}

	/**
	 * Writes the app of a GUI class into a folder or project (created if needed).
	 * js: the code of app.js if it doesn't exist (null: the template). Returns the
	 * code for the new elements which app.js doesn't use yet (if it existed).
	 */
	public static List<String> write(IContainer app, WindowBuilderShiny.WebResult result, String js, IProgressMonitor monitor) throws CoreException {
		OutputLocation.ensure(app, monitor);
		put(app.getFile(new org.eclipse.core.runtime.Path("index.html")), result.html(), monitor);
		IFile css = app.getFile(new org.eclipse.core.runtime.Path("style.css"));
		if (!css.exists()) {
			put(css, result.css(), monitor);
		}
		IFile jsFile = app.getFile(new org.eclipse.core.runtime.Path("app.js"));
		List<String> missing = new ArrayList<>();
		if (!jsFile.exists()) {
			put(jsFile, js != null ? js : result.js(), monitor);
		} else {
			String code;
			try (java.io.InputStream in = jsFile.getContents(true)) {
				code = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			} catch (java.io.IOException e) {
				code = "";
			}
			for (String id : result.ids()) {
				if (!code.contains("\"" + id + "\"") && !code.contains("'" + id + "'")) {
					missing.add("const " + id + " = document.getElementById(\"" + id + "\");\n");
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

	/** Opens the page (the Bio7 browser or the system browser, as in the preferences). */
	public static void open(IFile index) {
		String url = index.getLocationURI().toString();
		if (com.eco.bio7.Bio7Plugin.getDefault().getPreferenceStore().getBoolean("PDF_USE_BROWSER")) {
			com.eco.bio7.collection.BrowserUtil.setLocationWithJS(url);
		} else {
			org.eclipse.swt.program.Program.launch(url);
		}
	}
}
