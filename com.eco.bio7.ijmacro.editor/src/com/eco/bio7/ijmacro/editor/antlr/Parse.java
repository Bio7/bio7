/*******************************************************************************
 * Copyright (c) 2005-2017 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.ijmacro.editor.antlr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.IDocumentProvider;
import com.eco.bio7.ijmacro.editor.outline.IJMacroEditorOutlineNode;
import com.eco.bio7.ijmacro.editors.IJMacroEditor;

/**
 * This class is the main parsing class for the ImageJ Macro editor source.
 * 
 * @author M. Austenfeld
 *
 */
public class Parse {
	/** holds the calculated positions */
	protected final ArrayList<Position> fPositions = new ArrayList<Position>();
	private IJMacroEditor editor;
	private CommonTokenStream tokens;
	private int numberOfMainParseErrors;

	public HashMap<Integer, String> map;

	public HashMap<Integer, String> getMap() {
		return map;
	}

	public Parse(IJMacroEditor editor) {

		this.editor = editor;

	}

	public CommonTokenStream getTokens() {
		return tokens;
	}

	/**
	 * A method to return the number of parse errors of the main parse process.
	 * 
	 * @return the number of errors of the main parsing process when the editor
	 *         source is changed!
	 */
	public int getNumberOfMainParseErrors() {
		return numberOfMainParseErrors;
	}

	/**
	 * A method to parse the editor code with an ANTLR generated R parser. Called,
	 * e.g., from the editor reconciler.
	 */

	public void parse() {

		Vector<IJMacroEditorOutlineNode> editorOldNodes = editor.nodes;
		/* Create the category base node for the outline! */
		editor.createNodes();
		/*
		 * ErrorWarnMarkerDeletion deleteMarkerJob = new
		 * ErrorWarnMarkerDeletion("Delete Markers", editor);
		 * 
		 * deleteMarkerJob.addJobChangeListener(new JobChangeAdapter() { public void
		 * done(IJobChangeEvent event) { if (event.getResult().isOK()) {
		 * 
		 * } else {
		 * 
		 * } } }); deleteMarkerJob.setUser(true); deleteMarkerJob.schedule();
		 */

		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());
		CodePointCharStream input = CharStreams.fromString(doc.get());
		ImageJMacroLexer lexer = new ImageJMacroLexer(input);

		CommonTokenStream tokens = new CommonTokenStream(lexer);

		UnderlineListener li = new UnderlineListener();

		ImageJMacroParser parser = new ImageJMacroParser(tokens);

		parser.removeErrorListeners();
		parser.addErrorListener(li);

		ParseTreeWalker walker = new ParseTreeWalker();

		RuleContext tree = parser.program();
		/* Create the listener to create the outline, etc. */
		ImageJMacroBaseListen list = new ImageJMacroBaseListen(editor);

		// list.startStop.clear();
		walker.walk(list, tree);

		fPositions.clear();

		for (int i = 0; i < list.startStop.size(); i++) {

			String pos = (String) list.startStop.get(i);
			String[] val = pos.split(",");
			int pos1 = Integer.parseInt(val[0]);
			int pos2 = Integer.parseInt(val[1]);
			if (pos1 >= 0 && pos2 >= 0) {
				Position posit = new Position(pos1, pos2);
				fPositions.add(posit);
			}

		}

		/* Update the outline if no errors in the listener are counted! */
		numberOfMainParseErrors = li.getNumberOfListenSyntaxErrors();
		if (numberOfMainParseErrors == 0) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {

					editor.updateFoldingStructure(fPositions);

					editor.outlineInputChanged(editorOldNodes, editor.nodes);
				}

			});
		}

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

	public ImageJMacroBaseListen parseFromOffset(int offset) {

		map = new HashMap<Integer, String>();

		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());
		CodePointCharStream input = CharStreams.fromString(doc.get());
		ImageJMacroLexer lexer = new ImageJMacroLexer(input);

		CommonTokenStream tokens = new CommonTokenStream(lexer);
		tokens.fill();
		for (int index = 0; index < tokens.size(); index++) {
			Token token = tokens.get(index);
           /*Filter out comments on channel 2!*/
			if (token.getChannel() == 2) {
				if (index + 1 < tokens.size()) {
					//Get the token after the comment!
					Token tokenNext = tokens.get(index + 1);
					//Get the line number of the following method!
					int methodLineNumber = tokenNext.getLine() + 1;
					
					String out = token.getText();
					/*Put the comment text and the line number in a map!*/
					map.put(methodLineNumber, out);
				}

			}

		}

		//UnderlineListener li = new UnderlineListener();

		ImageJMacroParser parser = new ImageJMacroParser(tokens);

		parser.removeErrorListeners();
		//parser.addErrorListener(li);

		ParseTreeWalker walker = new ParseTreeWalker();

		RuleContext tree = parser.program();
		/* Create the listener to create the outline, etc. */
		ImageJMacroBaseListen listener = new ImageJMacroBaseListen(editor, offset);

		// list.startStop.clear();
		walker.walk(listener, tree);

		return listener;

	}

}
