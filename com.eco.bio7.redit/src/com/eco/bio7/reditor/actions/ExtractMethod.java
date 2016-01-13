package com.eco.bio7.reditor.actions;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.reditor.antlr.RFilter;
import com.eco.bio7.reditor.antlr.RLexer;
import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditor.antlr.RParser.ProgContext;
import com.eco.bio7.reditor.antlr.refactor.ExtractInterfaceListener;
import com.eco.bio7.reditor.antlr.refactor.ParseErrorListener;
import com.eco.bio7.util.Util;
import org.antlr.v4.runtime.TokenStream;

public class ExtractMethod implements IEditorActionDelegate {

	private ISelection selection;
	private IEditorPart targetEditor;
	private CommonTokenStream tokens;
	private RParser parser;
	private TokenStreamRewriter rewriter;
	private ProgContext tree;
	private BufferedTokenStream bufferTokenStream;

	public void setActiveEditor(final IAction action, final IEditorPart targetEditor) {
		this.targetEditor = targetEditor;
		action.setActionDefinitionId("com.eco.bio7.reditor.refactor.extract.method");
		if (targetEditor != null)
			targetEditor.getSite().getKeyBindingService().registerAction(action);
	}

	public void run(final IAction action) {

		IResource resource = (IResource) targetEditor.getEditorInput().getAdapter(IResource.class);
		if (resource != null) {

			ITextEditor editor = (ITextEditor) targetEditor;
			IDocumentProvider dp = editor.getDocumentProvider();
			IDocument doc = dp.getDocument(editor.getEditorInput());
			String docText = doc.get();
			ISelectionProvider sp = editor.getSelectionProvider();
			ISelection selectionsel = sp.getSelection();
			ITextSelection selection = (ITextSelection) selectionsel;
			String text = selection.getText();
			String functionName = "varName";

			if (text.isEmpty() || text == null) {
				errorMessage("Nothing selected!");
				return;
			}

			/* First parse of the selection! */
			boolean errors = parseSource(text);
			if (errors == false) {

				InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(), "", "Enter Name", functionName, null);
				if (dlg.open() == Window.OK) {
					// User clicked OK; update the label with the input

					functionName = dlg.getValue();
				} else {
					return;
				}
				
				/*try {
					org.eclipse.jface.text.IRegion line= doc.getLineInformationOfOffset(selection.getOffset());
					//int lineOffset= line.getOffset();
					int lengthOff = line.getLength();
					int endLi=findEndOfWhiteSpace(doc,selection.getOffset(),selection.getOffset()+lengthOff);
					int length = selection.getLength();
					System.out.println(endLi);
					System.out.println(length);
					doc.replace(endLi, length, "");

				} catch (BadLocationException e) {

					e.printStackTrace();
				}*/

				/* Delete the text of the selection! */
				int start = selection.getOffset();

				try {

					int length = selection.getLength();
					doc.replace(start, length, "");

				} catch (BadLocationException e) {

					e.printStackTrace();
				}

			} else {
				errorMessage("Parser error occured!\nPlease select valid R expressions!");
				doc.set(docText);
				// System.out.println("How many errors1: " +
				// parser.getNumberOfSyntaxErrors());
				return;
			}
			/* Second parse of the whole text without the selected text! */
			String fullTextWithoutSel = doc.get();
			boolean error = parseSource(fullTextWithoutSel);

			/* Selection */
			if (error == false) {
				rewriter = new TokenStreamRewriter(tokens);
				// Token token = tokens.get(0);
				Token selectedToken = null;
				TokenStream tokStream = rewriter.getTokenStream();
				/* Search for the token at offset!*/
				for (int i = 0; i < tokStream.size(); i++) {
					Token tempToken = rewriter.getTokenStream().get(i);
					int start = tempToken.getStartIndex();
					if (selection.getOffset() == start) {

						selectedToken = rewriter.getTokenStream().get(i);
					}
				}

				/*
				 * If we have found the token with the required offset we
				 * insert!
				 */
				int startLine=selection.getStartLine();
				int intEndLine=selection.getEndLine();
				StringBuffer buffWhite=getLeadingWhitespace(selection.getOffset(), doc);
				String whitesp=buffWhite.toString();
				
				StringBuffer buff = new StringBuffer();
				//buff.append(System.lineSeparator());
				buff.append(functionName);
				buff.append("<-function(){");
				buff.append(System.lineSeparator());
				buff.append(whitesp+"\t"+text);
				buff.append(System.lineSeparator());
				buff.append(whitesp+"}");
				buff.append(System.lineSeparator());
				buff.append(whitesp+functionName);
				buff.append("()");
				buff.append(System.lineSeparator());
                /*Set function local!*/
				if (selectedToken != null) {
					rewriter.insertBefore(selectedToken, buff.toString());
				} else {
					rewriter.insertAfter(tree.stop, buff.toString());
				}
				
				// System.out.println("myFunc<-function(){\n\t"+rewriter.getText()+"\n}");
				// System.out.println(rewriter.getText());
			} else {
				errorMessage("Parser error occured!\nPlease select valid R expressions!");
				doc.set(docText);
				//System.out.println("How many errors2: " + parser.getNumberOfSyntaxErrors());
				return;
			}
			/* Third parse for the final result! */
			boolean errorNewText = parseSource(rewriter.getText());

			if (errorNewText == false) {
				/*Write to the editor!*/
				doc.set(rewriter.getText());
				/*Scroll to the selection!*/
				editor.selectAndReveal(selection.getOffset(), 0);
			}

			else {
				errorMessage("Parser error occured!\nPlease select valid R expressions!");
				//System.out.println("How many errors3: " + parser.getNumberOfSyntaxErrors());
				//System.out.println("final" + rewriter.getText());
				doc.set(docText);
				return;
			}

		}

	}

	/* Here we parse the text and test for possible errors! */
	private boolean parseSource(String fullText) {
		boolean errors;
		ANTLRInputStream input = new ANTLRInputStream(fullText);
		RLexer lexer = new RLexer(input);
		tokens = new CommonTokenStream(lexer);
		bufferTokenStream = new BufferedTokenStream(lexer);
		RFilter filter = new RFilter(tokens);
		filter.removeErrorListeners();
		filter.stream(); // call start rule: stream
		tokens.reset();
		ParseErrorListener parseErrorListener = new ParseErrorListener();
		parser = new RParser(tokens);

		lexer.removeErrorListeners();
		// lexer.addErrorListener(li);
		parser.removeErrorListeners();
		// parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);
		parser.addErrorListener(parseErrorListener);

		tree = parser.prog();
		ParseTreeWalker walker = new ParseTreeWalker(); // create standard
														// walker
		ExtractInterfaceListener extractor = new ExtractInterfaceListener(tokens, parser);
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
	/*The following methods from class: XmlDocumentFormatter in package: org.eclipse.ant.internal.ui.editor.formatter;
	 * *******************************************************************************
 * Copyright (c) 2004, 2006s John-Mason P. Shackelford and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     John-Mason P. Shackelford - initial API and implementation
 * 	   IBM Corporation - bug fixes
 *******************************************************************************/
	
	public static StringBuffer getLeadingWhitespace(int offset, IDocument document) {
		StringBuffer indent= new StringBuffer();
		try {
			org.eclipse.jface.text.IRegion line= document.getLineInformationOfOffset(offset);
			int lineOffset= line.getOffset();
			int nonWS= findEndOfWhiteSpace(document, lineOffset, lineOffset + line.getLength());
			indent.append(document.get(lineOffset, nonWS - lineOffset));
			return indent;
		} catch (BadLocationException e) {
			return indent;
		}
	}
	public static int findEndOfWhiteSpace(IDocument document, int offset, int end) throws BadLocationException {
		while (offset < end) {
			char c= document.getChar(offset);
			if (c != ' ' && c != '\t') {
				return offset;
			}
			offset++;
		}
		return end;
	}

}
