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
package com.eco.bio7.reditor.antlr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.antlr.v4.runtime.Token;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * Creates the markers of the R code analysis for a file without an R editor
 * (e.g., the R chunks of a markdown document): the errors and warnings with the
 * messages and ranges of the R editor, as markers of the given type (the former
 * ones of this type are replaced, other markers of the file stay).
 */
public class DocumentMarkers extends WorkspaceJob {

	/* The packages of a file which were passed to the code completion */
	private static final java.util.Map<IResource, java.util.Set<String>> LOADED_IMPORTS = new java.util.concurrent.ConcurrentHashMap<>();

	private final IResource resource;
	private final IDocument document;
	private final List<ErrorWarnStore> errors;
	private final String markerType;
	private final BooleanSupplier current;

	/**
	 * @param document   the document of the offsets (the lines of the markers)
	 * @param markerType a subtype of the problem marker
	 * @param current    false if a newer analysis replaced this one (then the job
	 *                   does nothing)
	 */
	public DocumentMarkers(IResource resource, IDocument document, List<ErrorWarnStore> errors, String markerType, BooleanSupplier current) {
		super("R Markers");
		this.resource = resource;
		this.document = document;
		this.errors = errors;
		this.markerType = markerType;
		this.current = current;
		setRule(resource);
		setSystem(true);
	}

	/**
	 * Parses the R code and replaces the markers of the type (in a job). The code
	 * has the offsets of the file (e.g., the R chunks of a markdown document, the
	 * other text replaced by spaces). Only plain types for other plugins. Called in
	 * a background thread.
	 */
	public static void analyze(IResource resource, String code, String markerType, BooleanSupplier current) {
		analyze(resource, code, null, markerType, current);
	}

	/**
	 * As analyze(IResource, String, String, BooleanSupplier) with the parts of the
	 * code (e.g., the R chunks): each part is checked for syntax errors on its own.
	 *
	 * @param parts the [start, end) offsets of the parts or null
	 */
	public static void analyze(IResource resource, String code, List<int[]> parts, String markerType, BooleanSupplier current) {
		List<ErrorWarnStore> errors;
		try {
			if (code.isBlank()) {
				errors = List.of();
			} else {
				DocumentParse.Result r = parts != null && !parts.isEmpty() ? DocumentParse.parseParts(code, parts) : DocumentParse.parseAll(code);
				errors = r.errorsAndWarnings();
				/*
				 * The packages of the library() calls for the code completion (as for an R
				 * script, see the preferences): their functions are known, the analysis is
				 * repeated when they are loaded
				 */
				java.util.Map<String, String> imports = r.ref().getCurrentPackageImports();
				java.util.Set<String> packages = imports == null ? java.util.Set.of() : new java.util.TreeSet<>(imports.keySet());
				if (!packages.isEmpty() && !packages.equals(LOADED_IMPORTS.get(resource))) {
					LOADED_IMPORTS.put(resource, packages);
					com.eco.bio7.reditor.actions.PackageCompletionOnOpen.scriptOpened(packages, null);
				}
			}
		} catch (RuntimeException e) {
			/* The analysis must not disturb the editor */
			return;
		}
		new DocumentMarkers(resource, new org.eclipse.jface.text.Document(code), errors, markerType, current).schedule();
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		if (!resource.exists() || (current != null && !current.getAsBoolean())) {
			return Status.OK_STATUS;
		}
		resource.deleteMarkers(markerType, false, IResource.DEPTH_ZERO);
		for (ErrorWarnStore e : errors) {
			try {
				Map<String, Object> attributes = attributes(e);
				if (attributes != null) {
					resource.createMarker(markerType, attributes);
				}
			} catch (CoreException | RuntimeException ex) {
				/* The next one */
			}
		}
		return Status.OK_STATUS;
	}

	/* The attributes of a marker (as ErrorWarnMarkerCreation of the R editor) */
	private Map<String, Object> attributes(ErrorWarnStore e) {
		String msg = e.getMsg();
		if (msg == null) {
			return null;
		}
		msg = msg.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\t", "\t");
		boolean warn = false;
		String quickFix = null;
		String replacement = "";
		if (msg.startsWith("Err") || msg.startsWith("Warn")) {
			warn = msg.startsWith("Warn");
			String[] split = msg.split("####");
			quickFix = split[0];
			msg = split.length > 1 ? split[1] : msg;
			replacement = split.length > 2 ? split[2] : "";
		}
		int line = e.getLine();
		int lineOffset = 0;
		if (line - 1 >= 0 && line - 1 < document.getNumberOfLines()) {
			try {
				lineOffset = document.getLineOffset(line - 1);
			} catch (BadLocationException ex) {
				/* 0 */
			}
		}
		Map<String, Object> a = new HashMap<>();
		a.put(IMarker.SEVERITY, warn ? IMarker.SEVERITY_WARNING : IMarker.SEVERITY_ERROR);
		a.put(IMarker.MESSAGE, msg.trim());
		a.put(IMarker.LINE_NUMBER, line);
		a.put(IMarker.LOCATION, "line " + line);
		a.put(IMarker.PRIORITY, quickFix != null ? IMarker.PRIORITY_HIGH : IMarker.PRIORITY_LOW);
		/* The quick fix (as the markers of the R editor) */
		a.put(IMarker.TEXT, quickFix != null ? quickFix : "NA");
		a.put("REPLACEMENT_TEXT", replacement);
		if (e.getOffendingSymbol() instanceof Token tok && tok.getText() != null) {
			a.put("TOKEN_TEXT", tok.getText());
		}
		if (e.getOffendingSymbol() instanceof Token t) {
			int start = t.getStartIndex();
			int end = t.getStopIndex() + 1;
			if (t.getText() != null && t.getText().equals(System.lineSeparator()) && start > 0) {
				/* A line break: the end of the line before */
				start--;
				end--;
			}
			if (start >= 0 && end > start) {
				a.put(IMarker.CHAR_START, start);
				a.put(IMarker.CHAR_END, end);
			}
		} else {
			a.put(IMarker.CHAR_START, lineOffset + Math.max(0, e.getCharPositionInLine()));
			a.put(IMarker.CHAR_END, lineOffset + Math.max(0, e.getCharPositionInLine()) + 1);
		}
		return a;
	}
}
