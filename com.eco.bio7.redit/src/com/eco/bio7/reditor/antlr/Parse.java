/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
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
package com.eco.bio7.reditor.antlr;

import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import com.eco.bio7.reditor.antlr.ref.RRefPhaseListen;
import com.eco.bio7.reditor.antlr.refactor.ExtractInterfaceListener;
import com.eco.bio7.reditor.antlr.refactor.ParseErrorListener;
import com.eco.bio7.reditor.outline.REditorOutlineNode;
import com.eco.bio7.reditors.REditor;

/**
 * This class is the main parsing class for the R editor source.
 * 
 * @author M. Austenfeld
 *
 */
public class Parse {
	/** holds the calculated positions */
	protected final ArrayList<Position> fPositions = new ArrayList<Position>();
	private REditor editor;
	private CommonTokenStream tokens;
	private int numberOfMainParseErrors;
	private boolean isInFunctionCall = false;
	private String funcName = "";
	private boolean isInMatrixBracketCall;
	private boolean isInMatrixDoubleBracketCall;
	private String bracketMatrixName;
	private int matrixArgState;
	private int bracketCommaCount;
	private Map<String, String> currentPackageImports;
	private Map<String, String> shellCurrentPackageImports;
	private boolean isInPipeFunction;
	private String currentPipeData;

	public Map<String, String> getShellCurrentPackageImports() {
		return shellCurrentPackageImports;
	}

	public boolean isInPipeFunction() {
		return isInPipeFunction;
	}

	public String getCurrentPipeData() {
		return currentPipeData;
	}

	/* For the use in the Quickfix package import! */
	public Map<String, String> getCurrentPackageImports() {
		return currentPackageImports;
	}

	public int getBracketCommaCount() {
		return bracketCommaCount;
	}

	public int getMatrixArgState() {
		return matrixArgState;
	}

	public boolean isInMatrixBracketCall() {
		return isInMatrixBracketCall;
	}

	public boolean isInMatrixDoubleBracketCall() {
		return isInMatrixDoubleBracketCall;
	}

	public String getBracketMatrixName() {
		return bracketMatrixName;
	}

	public Parse(REditor editor) {

		this.editor = editor;

	}

	public boolean isInFunctionCall() {
		return isInFunctionCall;
	}

	public String getFuncName() {
		return funcName;
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

		IDocumentProvider dp = editor.getDocumentProvider();
		if (dp==null) {
			return;
		}
		IEditorInput inp = editor.getEditorInput();
		if (inp == null) {
			return;
		}
		
		if (dp.getDocument(inp) == null) {
			return;
		}
		Vector<REditorOutlineNode> editorOldNodes = editor.nodes;
		/* Create the category base node for the outline! */
		editor.createNodes();
		/* Create all collected markers in a job! */
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
		
		IDocument doc = dp.getDocument(inp);

		CodePointCharStream input =  CharStreams.fromString(doc.get());
		RLexer lexer = new RLexer(input);

		CommonTokenStream tokens = new CommonTokenStream(lexer);

		UnderlineListener li = new UnderlineListener();
		RFilter filter = new RFilter(tokens);
		/*
		 * We have to remove the filter, too! Else we get error messages on the console!
		 */
		filter.removeErrorListeners();
		// filter.addErrorListener(li);

		filter.stream(); // call start rule: stream
		tokens.seek(0);

		RParser parser = new RParser(tokens);
		parser.removeErrorListeners();
		/*
		 * Add some modified error messages by implementing a custom error strategy!
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
		/* For the use in the Quickfix package import! */
		currentPackageImports = ref.getCurrentPackageImports();
		/*
		 * long startTime = System.currentTimeMillis();
		 * 
		 * long stopTime = System.currentTimeMillis(); long elapsedTime = stopTime -
		 * startTime; System.out.println(elapsedTime);
		 */

		fPositions.clear();

		for (int i = 0; i < list.startStop.size(); i++) {

			String pos = (String) list.startStop.get(i);
			String[] val = pos.split(",");

			fPositions.add(new Position(Integer.parseInt(val[0]), Integer.parseInt(val[1])));

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

		/*
		 * markerJob.addJobChangeListener(new JobChangeAdapter() { public void
		 * done(IJobChangeEvent event) { if (event.getResult().isOK()) {
		 * 
		 * } else {
		 * 
		 * } } });
		 */
		markerJob.setUser(true);
		markerJob.schedule();

	}

	/**
	 * A method to parse the code at a specific offset to calculate completion
	 * proposals in scope, roxygen comments and the refactor rename scope.
	 * 
	 * @param offset
	 *            the offset in the document
	 * @return the RefBaseListen class which holds the scope vars, etc.
	 */
	public RRefPhaseListen parseFromOffset(int offset) {
		Vector<REditorOutlineNode> editorOldNodes = editor.nodes;
		/* Create the category base node for the outline! */
		editor.createNodes();

		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		CodePointCharStream input =  CharStreams.fromString(doc.get());
		RLexer lexer = new RLexer(input);

		tokens = new CommonTokenStream(lexer);

		UnderlineListener li = new UnderlineListener();
		RFilter filter = new RFilter(tokens);
		/*
		 * We have to remove the filter, too! Else we get error messages on the console!
		 */
		filter.removeErrorListeners();
		// filter.addErrorListener(li);

		filter.stream(); // call start rule: stream
		tokens.seek(0);

		RParser parser = new RParser(tokens);
		parser.removeErrorListeners();
		/*
		 * Add some modified error messages by implementing a custom error strategy!
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
		/* For the use in the Quickfix package import! */
		currentPackageImports = ref.getCurrentPackageImports();
		/* Important to update the code folding positions! */
		fPositions.clear();

		for (int i = 0; i < list.startStop.size(); i++) {

			String pos = (String) list.startStop.get(i);
			String[] val = pos.split(",");

			fPositions.add(new Position(Integer.parseInt(val[0]), Integer.parseInt(val[1])));

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
		/* We don't need error marker creation here! */

		return ref;
	}

	/* Here we parse the text and test for possible errors! */
	/**
	 * A special parse process for the R-Shell!
	 * 
	 * @param fullText
	 *            the shell evaluate text.
	 * @return a boolean indicating errors.
	 */
	public boolean parseShellSource(String fullText, int offset) {
		boolean errors;
		CodePointCharStream input =  CharStreams.fromString(fullText);
		RLexer lexer = new RLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		// bufferTokenStream = new BufferedTokenStream(lexer);
		RFilter filter = new RFilter(tokens);
		filter.removeErrorListeners();
		filter.stream(); // call start rule: stream
		tokens.seek(0);
		ParseErrorListener parseErrorListener = new ParseErrorListener();
		RParser parser = new RParser(tokens);

		lexer.removeErrorListeners();
		// lexer.addErrorListener(li);
		parser.removeErrorListeners();
		/*
		 * Here we add a custom error listener instead of the default Underline listener
		 * because we only want to capture the standard ANTLR errors for the console!
		 */
		parser.addErrorListener(parseErrorListener);

		RuleContext tree = parser.prog();
		ParseTreeWalker walker = new ParseTreeWalker(); // create standard
														// walker
		/*
		 * In this case the offset is only used for the R-Shell to calculate if we are
		 * in a method call!
		 */
		ExtractInterfaceListener extractor = new ExtractInterfaceListener(tokens, parser, false, offset);
		walker.walk(extractor, tree);
		isInFunctionCall = extractor.isInVarCall();
		isInMatrixBracketCall = extractor.isInMatrixBracketCall();
		isInMatrixDoubleBracketCall = extractor.isInMatrixDoubleBracketCall();
		bracketMatrixName = extractor.getBracketMatrixName();
		funcName = extractor.getFuncName();
		matrixArgState = extractor.getMatrixArgState();
		bracketCommaCount = extractor.getBracketCommaCount();
		isInPipeFunction = extractor.isInPipeFunction();
		currentPipeData = extractor.getCurrentPipeData();
		shellCurrentPackageImports = extractor.getCurrentPackageImports();

		// System.out.println(funcName);
		if (parser.getNumberOfSyntaxErrors() == 0) {
			errors = false;
		} else {
			errors = true;
		}

		return errors;
	}

}
