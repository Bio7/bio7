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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.collection.BrowserUtil;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.util.Bio7Dialog;

/**
 * Shiny apps: the detection of an R file of a Shiny app (the toolbar button of
 * the R editor) and the start (the button and the context menu of a folder or
 * project).
 * <ul>
 * <li>app.R, ui.R or server.R: the app of the folder</li>
 * <li>another R file which calls shinyApp(...): the file (runApp accepts it)</li>
 * </ul>
 */
public final class ShinyApps {

	/* A call of shinyApp( or shiny::shinyApp( (not in a comment) */
	private static final Pattern SHINY_APP_CALL = Pattern.compile("(?m)^[^#\\n]*\\bshinyApp\\s*\\(");
	/* Larger files are not read for the detection */
	private static final long MAX_SIZE = 2_000_000;

	private ShinyApps() {
	}

	/** Returns true if the R file is (a part of) a Shiny app. */
	public static boolean isShinyApp(IFile file) {
		return appPath(file) != null;
	}

	/**
	 * The path for runApp(): the folder of a multi-file app (app.R, ui.R,
	 * server.R) or the file of a single-file app; null if the file is no app.
	 */
	public static String appPath(IFile file) {
		if (file == null || file.getLocation() == null || !"r".equalsIgnoreCase(file.getFileExtension())) {
			return null;
		}
		Path path = file.getLocation().toFile().toPath();
		String name = file.getName().toLowerCase(java.util.Locale.ROOT);
		if (name.equals("app.r") || name.equals("ui.r") || name.equals("server.r")) {
			return path.getParent().toString().replace("\\", "/");
		}
		try {
			if (Files.size(path) > MAX_SIZE) {
				return null;
			}
			String code = Files.readString(path, StandardCharsets.UTF_8);
			return SHINY_APP_CALL.matcher(code).find() ? path.toString().replace("\\", "/") : null;
		} catch (IOException | RuntimeException e) {
			return null;
		}
	}

	/**
	 * Starts the Shiny app (a folder or an app file) in the R console and opens it
	 * in the browser (the preferences: Bio7 browser or the system browser).
	 */
	public static void start(String appPath) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		if (!"R".equals(ConsolePageParticipant.getInterpreterSelection())) {
			Bio7Dialog.message("Please start R in the Bio7 console!");
			return;
		}
		int port = store.getInt("SHINY_PORT");
		String app = appPath.replace("\\", "/").replace("\"", "\\\"");
		Job job = new Job("Shiny Server") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Start Shiny Server...", IProgressMonitor.UNKNOWN);
				/* runApp blocks R until the app is stopped (the stop button of the console) */
				ConsolePageParticipant.pipeInputToConsole("library(shiny);runApp(\"" + app + "\", port=" + port + ", launch.browser=FALSE)", true, true);
				/* The server needs a moment to start */
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				Display.getDefault().asyncExec(() -> {
					String url = "http://127.0.0.1:" + port;
					if (store.getBoolean("PDF_USE_BROWSER")) {
						BrowserUtil.setLocationWithJS(url);
					} else {
						Program.launch(url);
					}
				});
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
}
