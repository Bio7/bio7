/*******************************************************************************
 * Copyright (c) 2005-2016 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.reditor.antlr;

import java.util.ArrayList;
import java.util.Vector;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.reditor.antlr.ref.RRefPhaseListen;
import com.eco.bio7.reditor.outline.REditorOutlineNode;
import com.eco.bio7.reditors.REditor;

/**
 * This class is the main parsing class for the R editor source.
 */
public class Parse {
	/** holds the calculated positions */
	protected final ArrayList<Position> fPositions = new ArrayList<Position>();
	private REditor editor;

	public Parse(REditor editor) {

		this.editor = editor;

	}

	/**
	 * A method to parse the editor code with an ANTLR generated R parser.
	 * Called, e.g., from the editor reconciler.
	 */
	public void parse() {

		Vector<REditorOutlineNode> editorOldNodes = editor.nodes;
		/* Create the category base node for the outline! */
		editor.createNodes();
		/* Create all collected markers in a job! */
		ErrorWarnMarkerDeletion deleteMarkerJob = new ErrorWarnMarkerDeletion("Delete Markers", editor);

		deleteMarkerJob.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {

				} else {

				}
			}
		});
		deleteMarkerJob.setUser(true);
		deleteMarkerJob.schedule();
		

		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

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
		/*
		 * Add some modified error messages by implementing a custom error
		 * strategy!
		 */
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
		/* Create all collected markers in a job! */
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

	

	public RRefPhaseListen parseFromOffset(int offset) {

		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

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
		/*
		 * Add some modified error messages by implementing a custom error
		 * strategy!
		 */
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

		RRefPhaseListen ref = new RRefPhaseListen(tokens, list, parser, offset);
		walker.walk(ref, tree);
		return ref;
	}

}
