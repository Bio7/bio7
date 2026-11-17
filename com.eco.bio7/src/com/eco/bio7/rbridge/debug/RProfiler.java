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
package com.eco.bio7.rbridge.debug;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.collection.BrowserUtil;
import com.eco.bio7.rbridge.RBridge;
import com.eco.bio7.rbridge.RTools;
import com.eco.bio7.rbridge.REval;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.RWait;
import com.eco.bio7.rbridge.backend.RBackend;
import com.eco.bio7.reditor.code.RProfileMarks;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The profiler of the R editor: runs the script (or the selection) with the
 * profiler of R (Rprof with line profiling, r_profiler.R), shows the report in
 * the browser view (the same report as for Python) and the times of the lines
 * as marks in the editor.
 */
public final class RProfiler {

	private RProfiler() {
	}

	/** Profiles the script of the active R editor (selection: only the selected lines). UI thread. */
	public static void profileActiveEditor(boolean selection) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IEditorPart editor = window == null ? null : window.getActivePage().getActiveEditor();
		IFile file = editor == null ? null : editor.getEditorInput().getAdapter(IFile.class);
		if (file == null || file.getLocation() == null || !"r".equalsIgnoreCase(file.getFileExtension()) || !(editor instanceof ITextEditor te)) {
			Bio7Dialog.message("Please open an R script (.R) in the editor.");
			return;
		}
		String code = null;
		int firstLine = 1;
		if (selection) {
			if (!(te.getSelectionProvider().getSelection() instanceof ITextSelection sel) || sel.getLength() == 0) {
				Bio7Dialog.message("Please select the code to profile.");
				return;
			}
			IDocument doc = te.getDocumentProvider().getDocument(te.getEditorInput());
			try {
				/* Complete lines */
				int start = doc.getLineOffset(sel.getStartLine());
				int end = doc.getLineOffset(sel.getEndLine()) + doc.getLineLength(sel.getEndLine());
				code = doc.get(start, end - start);
				firstLine = sel.getStartLine() + 1;
			} catch (BadLocationException e) {
				return;
			}
		}
		/* The report shows the file: the lines of the editor */
		if (editor.isDirty()) {
			editor.doSave(null);
		}
		if (!RTools.isAliveDialog()) {
			return;
		}
		String path = file.getLocation().toOSString(), selected = code;
		int first = firstLine;
		RWait.whenFree("the profile", () -> {
			RState.setBusy(true);
			RProfileMarks.clear(file);
			System.out.println("Profiling " + file.getName() + (selected != null ? " (selection from line " + first + ")" : "") + " ...");
			Job job = new Job("Profile " + file.getName()) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Profile " + file.getName() + " ...", IProgressMonitor.UNKNOWN);
					try {
						RBackend r = RBridge.getBackend();
						r.assign(".bio7_prof_src", source());
						r.eval("eval(parse(text = .bio7_prof_src), envir = globalenv()); rm(.bio7_prof_src, envir = globalenv())");
						r.assign(".bio7_prof_path", path.replace("\\", "/"));
						if (selected != null) {
							r.assign(".bio7_prof_code", selected);
						}
						String json = r.eval(".bio7prof$profile(.bio7_prof_path, " + (selected != null ? ".bio7_prof_code" : "NULL") + ", " + first
								+ "L)").firstString();
						r.eval("rm(list = intersect(c('.bio7_prof_path', '.bio7_prof_code'), ls(globalenv(), all.names = TRUE)), envir = globalenv())");
						show(file, json);
					} catch (Exception e) {
						Bio7Dialog.message("The profile could not be created: " + e.getMessage());
					} finally {
						RState.setBusy(false);
						monitor.done();
						REval.listRObjects();
					}
					return Status.OK_STATUS;
				}

				@Override
				protected void canceling() {
					/* The stop button: R is interrupted, the report shows the result up to the stop */
					try {
						com.eco.bio7.rbridge.backend.EmbeddedR.interrupt();
					} catch (LinkageError e) {
						/* Not embedded */
					}
				}
			};
			job.setUser(true);
			job.schedule();
		});
	}

	/* The report in the browser view and the marks in the editor */
	private static void show(IFile file, String json) throws Exception {
		JsonNode n = new ObjectMapper().readTree(json);
		double total = n.path("sampled").asDouble() * 1000;
		List<RProfileMarks.Line> lines = new ArrayList<>();
		for (JsonNode l : n.path("marks")) {
			lines.add(new RProfileMarks.Line(l.get(0).asInt(), l.get(1).asDouble(), l.get(2).asDouble()));
		}
		String title = ("Profile: " + file.getName()).replace("&", "&amp;").replace("<", "&lt;");
		String html = com.eco.bio7.pybridge.debug.PythonProfiler.reportTemplate().replace("__TITLE__", title).replace("__DATA__", json.replace("</", "<\\/"));
		Path out = Files.createTempFile("bio7_profile_", ".html");
		Files.writeString(out, html, StandardCharsets.UTF_8);
		out.toFile().deleteOnExit();
		System.out.println(String.format(java.util.Locale.ROOT, "Profile of %s: %.3f s (%d samples)%s", file.getName(), n.path("elapsed").asDouble(),
				n.path("samples").asInt(), n.path("message").isNull() ? "" : " - " + n.path("message").asText()));
		Display.getDefault().asyncExec(() -> {
			BrowserUtil.setLocationWithJS(out.toUri().toString());
			RProfileMarks.show(file, lines, total);
		});
	}

	private static String source() throws java.io.IOException {
		try (InputStream in = RProfiler.class.getResourceAsStream("r_profiler.R")) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
