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
package com.eco.bio7.reditor.actions;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import com.eco.bio7.reditor.antlr.Parse;
import com.eco.bio7.reditor.antlr.RBaseListen;
import com.eco.bio7.reditor.antlr.RErrorStrategy;
import com.eco.bio7.reditor.antlr.RFilter;
import com.eco.bio7.reditor.antlr.RLexer;
import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditor.antlr.UnderlineListener;
import com.eco.bio7.reditor.antlr.ref.RRefPhaseListen;
import com.eco.bio7.reditor.antlr.ref.Scope;
import com.eco.bio7.reditor.antlr.refactor.CoordinatesUtil;
import com.eco.bio7.reditor.antlr.refactor.DeleteBlockingExitPolicy;
import com.eco.bio7.reditor.antlr.refactor.RenameVarListener;
import com.eco.bio7.reditor.antlr.refactor.TextUtil;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.util.Util;

public class RefactorRename implements IEditorActionDelegate {

	private ISelection selection;
	private IEditorPart targetEditor;
	// private TokenStreamRewriter rewriter;
	// private boolean global;
	// private String args;
	private RRefPhaseListen ref;
	private REditor rEditor;
	private Point wordOffsetAndLen;

	public void setActiveEditor(final IAction action, final IEditorPart targetEditor) {
		this.targetEditor = targetEditor;
		action.setActionDefinitionId("com.eco.bio7.reditor.refactor.rename");
		if (targetEditor != null)
			targetEditor.getSite().getKeyBindingService().registerAction(action);
	}

	public void run(final IAction action) {

		IResource resource = (IResource) targetEditor.getEditorInput().getAdapter(IResource.class);
		if (resource != null) {

			ITextEditor editor = (ITextEditor) targetEditor;
			rEditor = (REditor) editor;
			ProjectionViewer viewer = rEditor.getViewer();
			if (viewer != null) {
				try {
					startEditing(viewer);
				} catch (ExecutionException e) {

					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * Mostly based on code from {@link org.eclipse.jdt.internal.ui.text.correction.proposals.LinkedNamesAssistProposal}
	 */
	private void startEditing(ISourceViewer viewer) throws ExecutionException {
		Point selOffsetAndLen = viewer.getSelectedRange();
		int selStart = CoordinatesUtil.fromOffsetAndLengthToStartAndEnd(selOffsetAndLen).x;

		IDocument document = viewer.getDocument();
		try {
			String selectedText = null;
			if (selOffsetAndLen.y == 0) { // no characters selected
				String documentText = document.get();
				wordOffsetAndLen = TextUtil.findWordSurrounding(documentText, selStart);
				if (wordOffsetAndLen != null) {
					selectedText = document.get(wordOffsetAndLen.x, wordOffsetAndLen.y);
				}

			} else {
				ISelectionProvider sp = rEditor.getSelectionProvider();
				ISelection selectionsel = sp.getSelection();
				ITextSelection selection = (ITextSelection) selectionsel;
				selectedText = selection.getText();
				int offset = selection.getOffset();
				wordOffsetAndLen = new Point(offset, (selection.getLength() + offset) - offset);

				// selectedText = document.get(selOffsetAndLen.x,
				// selOffsetAndLen.y);
			}

			if (selectedText == null) {
				return;
			}

			Parse parse = new Parse(rEditor);

			ref = parse.parseFromOffset(selOffsetAndLen.x);

			LinkedPositionGroup linkedPositionGroup = new LinkedPositionGroup();

			/*
			 * if (ref.getSelectedRefactorScope() instanceof RGlobalScope) { parse.parseRefactorRename(linkedPositionGroup,selectedText,ref. getGlobals(),wordOffsetAndLen); }
			 */
			// else{
			// System.out.println(ref.getSelectedRefactorScope().getScopeName());

			/* Second parse to search for scope and select the variables! */
			Scope selectedRefactorScope = ref.getSelectedRefactorScope();
			int selectedScopeNumber = ref.getSelectedScopeNumber();

			parseRefactorRename(linkedPositionGroup, selectedText, selectedRefactorScope, selectedScopeNumber, wordOffsetAndLen);
			// parse.parseRefactorRename(linkedPositionGroup,selectedText,0,0,ref.getGlobals());
			// }

			/*
			 * FindReplaceDocumentAdapter findReplaceAdaptor = new FindReplaceDocumentAdapter(document); IRegion matchingRegion = findReplaceAdaptor.find(0, selectedText, true, true, true, false);
			 * 
			 * while (matchingRegion != null) {
			 * 
			 * if (ref.getTempCodeStartIndex() == 0 && ref.getTempCodeStopIndex() == 0) {
			 * 
			 * linkedPositionGroup.addPosition(new LinkedPosition(document, matchingRegion.getOffset(), matchingRegion.getLength())); } else if (matchingRegion.getOffset() >=
			 * ref.getTempCodeStartIndex() && (matchingRegion.getOffset()) <= ref.getTempCodeStopIndex()) { linkedPositionGroup.addPosition(new LinkedPosition(document, matchingRegion.getOffset(),
			 * matchingRegion.getLength())); }
			 * 
			 * matchingRegion = findReplaceAdaptor.find(matchingRegion.getOffset() + matchingRegion.getLength(), selectedText, true, true, true, false);
			 * 
			 * }
			 */
			if (linkedPositionGroup.isEmpty()) {
				return;
			}
			LinkedModeModel model = new LinkedModeModel();
			model.addGroup(linkedPositionGroup);
			model.forceInstall();

			LinkedModeUI ui = new EditorLinkedModeUI(model, viewer);
			ui.setExitPolicy(new DeleteBlockingExitPolicy(document));
			ui.enter();

			// by default the text being edited is selected so restore original
			// selection
			viewer.setSelectedRange(selOffsetAndLen.x, selOffsetAndLen.y);
		} catch (BadLocationException e) {
			throw new ExecutionException("Editing failed", e);
		}
	}

	/* Here we parse the text for the rename refactoring! */
	public boolean parseRefactorRename(LinkedPositionGroup linkedPositionGroup, String selectedText, Scope currentScope, int selectedScopeNumber, Point wordOffsetAndLen) {
		/*
		 * boolean errors; IDocumentProvider dp = editor.getDocumentProvider(); IDocument doc = dp.getDocument(editor.getEditorInput()); ANTLRInputStream input = new ANTLRInputStream(doc.get());
		 * RLexer lexer = new RLexer(input); CommonTokenStream tokens = new CommonTokenStream(lexer); //bufferTokenStream = new BufferedTokenStream(lexer); RFilter filter = new RFilter(tokens);
		 * filter.removeErrorListeners(); filter.stream(); // call start rule: stream tokens.reset(); ParseErrorListener parseErrorListener = new ParseErrorListener(); RParser parser = new
		 * RParser(tokens);
		 * 
		 * lexer.removeErrorListeners(); // lexer.addErrorListener(li); parser.removeErrorListeners();
		 * 
		 * parser.addErrorListener(parseErrorListener);
		 * 
		 * RuleContext tree = parser.prog(); ParseTreeWalker walker = new ParseTreeWalker(); // create standard // walker RenameVarListener extractor = new RenameVarListener(tokens,
		 * parser,false,linkedPositionGroup,doc,selectedText,scopeStart,scopeStop); walker.walk(extractor, tree);
		 * 
		 * 
		 * if (parser.getNumberOfSyntaxErrors() == 0) { errors = false; } else { errors = true; } return errors;
		 */
		boolean errors;
		IDocumentProvider dp = rEditor.getDocumentProvider();
		IDocument doc = dp.getDocument(rEditor.getEditorInput());

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
		/* Create the listener! */
		RBaseListen list = new RBaseListen(tokens, rEditor, parser);

		list.startStop.clear();
		walker.walk(list, tree);

		RenameVarListener extractor = new RenameVarListener(tokens, list, parser, doc, selectedText, linkedPositionGroup, currentScope, selectedScopeNumber, wordOffsetAndLen);
		walker.walk(extractor, tree);
		if (parser.getNumberOfSyntaxErrors() == 0) {
			errors = false;
		} else {
			errors = true;
		}
		return errors;
	}

	public void selectionChanged(final IAction action, final ISelection selection) {
		this.selection = selection;
	}

	/**
	 * Opens a message Dialog with the given text.
	 * 
	 * @param text
	 *            the text for the dialog.
	 */
	public static void errorMessage(final String text) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				MessageBox messageBox = new MessageBox(Util.getShell(),

						SWT.ICON_ERROR);
				messageBox.setText("Error!");
				messageBox.setMessage(text);
				messageBox.open();
			}
		});
	}

}
