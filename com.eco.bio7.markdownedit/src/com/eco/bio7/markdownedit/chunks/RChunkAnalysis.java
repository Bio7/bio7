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
package com.eco.bio7.markdownedit.chunks;

import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.reditor.antlr.DocumentMarkers;

/**
 * The code analysis of the R editor for the R chunks of a markdown editor: the
 * errors and warnings as markers of the file (underlined, in the rulers and the
 * Problems view), after a pause in typing. The R parser runs in a background
 * job.
 */
public final class RChunkAnalysis {

	/** The marker type of the R problems of markdown documents (a problem marker). */
	public static final String MARKER = "com.eco.bio7.markdownedit.rproblem";
	private static final int DELAY = 600;

	private final ISourceViewer viewer;
	private final IResource resource;
	private final AtomicLong generation = new AtomicLong();
	private boolean pending;
	private final Runnable analyze = () -> {
		pending = false;
		analyze();
	};

	private RChunkAnalysis(ITextEditor editor, ISourceViewer viewer) {
		this.viewer = viewer;
		this.resource = editor.getEditorInput().getAdapter(IResource.class);
		IDocument doc = viewer.getDocument();
		IDocumentListener listener = new IDocumentListener() {
			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
			}

			@Override
			public void documentChanged(DocumentEvent event) {
				schedule();
			}
		};
		doc.addDocumentListener(listener);
		/*
		 * Again when R changed (e.g., a chunk attached a package: its functions exist)
		 * or the functions of the code completion were reloaded (the packages of the
		 * library() calls)
		 */
		StyledText text = viewer.getTextWidget();
		Runnable again = () -> {
			if (!text.isDisposed()) {
				text.getDisplay().asyncExec(this::schedule);
			}
		};
		com.eco.bio7.reditors.RLiveNames.addListener(again);
		com.eco.bio7.rpreferences.template.CalculateRProposals.addReloadListener(again);
		text.addDisposeListener(_ -> {
			doc.removeDocumentListener(listener);
			com.eco.bio7.reditors.RLiveNames.removeListener(again);
			com.eco.bio7.rpreferences.template.CalculateRProposals.removeReloadListener(again);
		});
	}

	/** Starts the analysis of the R chunks of the editor (a file of the workspace). UI thread. */
	public static void install(ITextEditor editor, ISourceViewer viewer) {
		if (viewer == null || viewer.getDocument() == null || editor.getEditorInput().getAdapter(IResource.class) == null) {
			return;
		}
		RChunkAnalysis a = new RChunkAnalysis(editor, viewer);
		a.analyze();
	}

	private void schedule() {
		StyledText text = viewer.getTextWidget();
		if (!pending && text != null && !text.isDisposed()) {
			pending = true;
			text.getDisplay().timerExec(DELAY, analyze);
		}
	}

	/* The R code of the chunks (UI thread), the parser in a job, then the markers */
	private void analyze() {
		IDocument doc = viewer.getDocument();
		StyledText text = viewer.getTextWidget();
		if (doc == null || text == null || text.isDisposed()) {
			return;
		}
		boolean chunks = RChunkCode.hasRChunks(doc);
		String code = chunks ? RChunkCode.virtualCode(doc) : "";
		/* Each chunk is checked for syntax errors on its own (as knitr parses it) */
		java.util.List<int[]> ranges = chunks ? RChunkCode.ranges(doc) : java.util.List.of();
		long gen = generation.incrementAndGet();
		Job job = new Job("R Chunk Analysis") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (gen != generation.get()) {
					return Status.OK_STATUS;
				}
				DocumentMarkers.analyze(resource, code, ranges, MARKER, () -> gen == generation.get());
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}
}
