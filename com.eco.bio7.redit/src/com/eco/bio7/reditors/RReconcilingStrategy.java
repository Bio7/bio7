package com.eco.bio7.reditors;

/*******************************************************************************
 * Copyright (c) 2005 Prashant Deva and Gerd Castan
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License - v 1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Used as a basis for a R editor.
 * Extended with ANTLR parser and R editor context.
 *(c) 2014 - 2016 Marcel Austenfeld
 *******************************************************************************/

import java.util.ArrayList;
import java.util.Vector;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.swt.widgets.Display;
import com.eco.bio7.reditor.antlr.ErrorWarnMarkerCreation;
import com.eco.bio7.reditor.antlr.RBaseListen;
import com.eco.bio7.reditor.antlr.RErrorStrategy;
import com.eco.bio7.reditor.antlr.RFilter;
import com.eco.bio7.reditor.antlr.RLexer;
import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditor.antlr.UnderlineListener;
import com.eco.bio7.reditor.antlr.ref.RRefPhaseListen;
import com.eco.bio7.reditor.outline.REditorOutlineNode;

public class RReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

	private REditor editor;

	private IDocument fDocument;

	/** holds the calculated positions */
	protected final ArrayList<Position> fPositions = new ArrayList<Position>();

	/** The offset of the next character to be read */
	protected int fOffset;

	/** The end offset of the range to be scanned */
	protected int fRangeEnd;

	/**
	 * @return Returns the editor.
	 */
	public REditor getEditor() {
		return editor;
	}

	public void setEditor(REditor editor) {
		this.editor = editor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org.
	 * eclipse.jface.text.IDocument)
	 */
	public void setDocument(IDocument document) {
		this.fDocument = document;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.
	 * eclipse.jface.text.reconciler.DirtyRegion,
	 * org.eclipse.jface.text.IRegion)
	 */
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		initialReconcile();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.
	 * eclipse.jface.text.IRegion)
	 */
	public void reconcile(IRegion partition) {
		initialReconcile();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#
	 * setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void setProgressMonitor(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#
	 * initialReconcile()
	 */
	public void initialReconcile() {
		fOffset = 0;
		fRangeEnd = fDocument.getLength();
		calculatePositions();

	}

	/**
	 * next character position - used locally and only valid while
	 * {@link #calculatePositions()} is in progress.
	 */

	protected void calculatePositions() {

		Vector<REditorOutlineNode> editorOldNodes = editor.nodes;
		/* Create the category base node for the outline! */
		editor.createNodes();

		if (editor != null) {

			IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);

			if (resource != null) {
				try {
					resource.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
				} catch (CoreException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				/* Delete all problem markers! */
				/*
				 * IMarker[] markers = findMyMarkers(resource); int lineNumb =
				 * -1; for (int i = 0; i < markers.length; i++) {
				 * 
				 * try { lineNumb = (int)
				 * markers[i].getAttribute(IMarker.LINE_NUMBER);
				 * 
				 * if (lineNumb == line) { markers[i].delete(); //
				 * System.out.println(recognizer.getRuleNames()[i]); } } catch
				 * (CoreException e1) { // TODO Auto-generated catch block
				 * e1.printStackTrace(); } }
				 */
			}
		}

		IDocument doc = fDocument;

		ANTLRInputStream input = new ANTLRInputStream(doc.get());
		RLexer lexer = new RLexer(input);

		CommonTokenStream tokens = new CommonTokenStream(lexer);

		UnderlineListener li = new UnderlineListener(editor);
		RFilter filter = new RFilter(tokens);
		/*
		 * We have to remove the filter, too! Else we get error messages on the
		 * console!
		 */
		filter.removeErrorListeners();
		// filter.addErrorListener(li);

		filter.stream(); // call start rule: stream
		tokens.reset();

		RParser parser = new RParser(tokens);
		parser.removeErrorListeners();
		/*Add some modified error messages by implementing a custom error strategy!*/
		parser.setErrorHandler(new RErrorStrategy());
		parser.setBuildParseTree(true);

		lexer.removeErrorListeners();
		// lexer.addErrorListener(li);
		parser.removeErrorListeners();
		// parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);
		parser.addErrorListener(li);

		ParseTreeWalker walker = new ParseTreeWalker();

		RuleContext tree = parser.prog();
		/* Create the listener to create the outline, etc. */
		RBaseListen list = new RBaseListen(tokens, editor, parser);

		list.startStop.clear();
		walker.walk(list, tree);

		RRefPhaseListen ref = new RRefPhaseListen(tokens, list, parser);
		walker.walk(ref, tree);

		/*
		 * long startTime = System.currentTimeMillis();
		 * 
		 * long stopTime = System.currentTimeMillis(); long elapsedTime =
		 * stopTime - startTime; System.out.println(elapsedTime);
		 */

		fPositions.clear();

		for (int i = 0; i < list.startStop.size(); i++) {

			String pos = (String) list.startStop.get(i);
			String[] val = pos.split(",");

			fPositions.add(new Position(Integer.parseInt(val[0]), Integer.parseInt(val[1])));

		}
		/* Update the outline! */

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {

				editor.updateFoldingStructure(fPositions);

				editor.outlineInputChanged(editorOldNodes, editor.nodes);
			}

		});

		ErrorWarnMarkerCreation markerJob = new ErrorWarnMarkerCreation("Create Markers", editor, li.getErrWarn());

		markerJob.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {

				} else {

				}
			}
		});
		markerJob.setUser(true);
		markerJob.schedule();

	}

	public IMarker[] findMyMarkers(IResource target) {
		String type = "org.eclipse.core.resources.problemmarker";

		IMarker[] markers = null;
		try {
			markers = target.findMarkers(type, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {

			e.printStackTrace();
		}
		return markers;
	}

}
