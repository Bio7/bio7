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
package com.eco.bio7.pythonedit.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Analyzes the Python code of the editor (after a change) and sets the markers
 * (errors, warnings) with their quick fixes.
 */
public class PythonReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

	private static final long TIMEOUT_MILLIS = 15000;
	private final ITextEditor editor;
	private IDocument document;
	private final AtomicLong generation = new AtomicLong();

	public PythonReconcilingStrategy(ITextEditor editor) {
		this.editor = editor;
	}

	@Override
	public void setDocument(IDocument document) {
		this.document = document;
	}

	@Override
	public void setProgressMonitor(IProgressMonitor monitor) {
	}

	@Override
	public void initialReconcile() {
		analyze();
	}

	@Override
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		analyze();
	}

	@Override
	public void reconcile(IRegion partition) {
		analyze();
	}

	private IFile file() {
		IEditorInput input = editor.getEditorInput();
		return input instanceof IFileEditorInput fi ? fi.getFile() : null;
	}

	@SuppressWarnings("unchecked")
	private void analyze() {
		IDocument doc = document;
		IFile file = file();
		if (doc == null || file == null || !AnalysisClient.isAvailable()) {
			return;
		}
		long gen = generation.incrementAndGet();
		String text = doc.get();
		Map<String, Object> args = new LinkedHashMap<>();
		args.put("source", text);
		args.put("path", file.getLocation() == null ? file.getName() : file.getLocation().toOSString());
		args.put("known", new ArrayList<>(PythonEditorSupport.liveNames()));
		Map<String, Object> result = AnalysisClient.get().request("analyze", args, TIMEOUT_MILLIS);
		if (result == null || gen != generation.get() || !text.equals(doc.get())) {
			/* No Python, or the text changed in the meantime (the next analysis follows) */
			return;
		}
		/* The outline (only of code which can be parsed: else the last one stays) */
		if (result.get("outline") != null && editor instanceof OutlineTarget target) {
			target.outlineChanged(result.get("outline"));
		}
		List<Map<String, Object>> markers = new ArrayList<>();
		if (result.get("diagnostics") instanceof List<?> diags) {
			for (Object o : diags) {
				Map<String, Object> d = (Map<String, Object>) o;
				try {
					int start = PythonFixes.offset(doc, num(d, "line"), num(d, "col"));
					int end = PythonFixes.offset(doc, num(d, "endLine"), num(d, "endCol"));
					if (end <= start) {
						end = Math.min(doc.getLength(), start + 1);
					}
					Map<String, Object> m = new HashMap<>();
					m.put(IMarker.CHAR_START, start);
					m.put(IMarker.CHAR_END, end);
					m.put(IMarker.LINE_NUMBER, num(d, "line"));
					String severity = String.valueOf(d.get("severity"));
					m.put(IMarker.SEVERITY, severity.equals("error") ? IMarker.SEVERITY_ERROR : severity.equals("info") ? IMarker.SEVERITY_INFO : IMarker.SEVERITY_WARNING);
					String code = String.valueOf(d.getOrDefault("code", ""));
					m.put(IMarker.MESSAGE, String.valueOf(d.get("message")) + (code.matches("[A-Z]+[0-9]+") ? " (" + code + ")" : ""));
					m.put("bio7.code", code);
					Object fixes = d.get("fixes");
					m.put(PythonFixes.FIXES, fixes instanceof List<?> l && !l.isEmpty() ? Json.write(fixes) : "");
					markers.add(m);
				} catch (BadLocationException e) {
					/* The document changed */
				}
			}
		}
		WorkspaceJob job = new WorkspaceJob("Python code analysis") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				if (!file.exists() || gen != generation.get()) {
					return Status.OK_STATUS;
				}
				file.deleteMarkers(PythonFixes.MARKER, false, IResource.DEPTH_ZERO);
				for (Map<String, Object> m : markers) {
					file.createMarker(PythonFixes.MARKER, m);
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(file);
		job.setSystem(true);
		job.schedule();
	}

	private static int num(Map<String, Object> d, String key) {
		return d.get(key) instanceof Number n ? n.intValue() : 0;
	}
}
