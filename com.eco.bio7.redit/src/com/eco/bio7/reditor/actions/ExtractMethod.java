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
import org.eclipse.swt.widgets.Shell;
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
import com.eco.bio7.reditor.antlr.refactor.RefactorDialog;
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
	private boolean global;

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

				RefactorDialog dlg = new RefactorDialog(Display.getCurrent().getActiveShell());
				if (dlg.open() == Window.OK) {
					// User clicked OK; update the label with the input
					functionName = dlg.getValue();
					global = dlg.isGlobal();
				} else {
					return;
				}

				/*
				 * try { org.eclipse.jface.text.IRegion line=
				 * doc.getLineInformationOfOffset(selection.getOffset()); //int
				 * lineOffset= line.getOffset(); int lengthOff =
				 * line.getLength(); int
				 * endLi=findEndOfWhiteSpace(doc,selection.getOffset(),selection
				 * .getOffset()+lengthOff); int length = selection.getLength();
				 * System.out.println(endLi); System.out.println(length);
				 * doc.replace(endLi, length, "");
				 * 
				 * } catch (BadLocationException e) {
				 * 
				 * e.printStackTrace(); }
				 */

				/* Delete the text of the selection! */
				/*
				 * int start = selection.getOffset();
				 * 
				 * try {
				 * 
				 * int length = selection.getLength(); doc.replace(start,
				 * length, "");
				 * 
				 * } catch (BadLocationException e) {
				 * 
				 * e.printStackTrace(); }
				 */

			} else {
				errorMessage("Parser error occured!\nPlease select valid R expressions!");
				/* Revert changes if parser has errors! */
				doc.set(docText);
				// System.out.println("How many errors1: " +
				// parser.getNumberOfSyntaxErrors());
				return;
			}
			/* Second parse of the whole text without the selected text! */
			String fullTextWithoutSel = doc.get();
			boolean error = parseSource(fullTextWithoutSel);

			/* extract selection offset! */
			int selectionOffset = selection.getOffset();
			if (error == false) {
				rewriter = new TokenStreamRewriter(tokens);

				// Token token = tokens.get(0);
				Token startToken = null;
				Token stopToken = null;
				TokenStream tokStream = rewriter.getTokenStream();
				/* Search for the token at offset! */
				for (int i = 0; i < tokStream.size(); i++) {
					Token tempToken = tokStream.get(i);
					int currentToken = tempToken.getStartIndex();
					
					if (selectionOffset >= currentToken) {

						startToken = tempToken;
						// System.out.println("token start");;

					} else if (selectionOffset + selection.getLength() >= currentToken) {
						stopToken = tempToken;
						// System.out.println("token end");;

					}
				}

				/*
				 * If we have found the token with the required offset we
				 * insert!
				 */
				int defaultIndent = 2;
				int numWhite = getLeadingWhitespaceNumber(selectionOffset, doc);
				int numWhiteGlobalTemp=numWhite;
				if(global){
					numWhite=0;
					
				}
				StringBuffer buff = new StringBuffer();

				String[] lines = text.split(System.getProperty("line.separator"));
				/*
				 * Calculate the leading whitespaces in the first line
				 * (selection could be with whitespaces)!
				 */
				int count = lines[0].indexOf(lines[0].trim());
				// First line remove leading whitespace from selection text!
				//if(global==false){
					lines[0] = lines[0].trim();
				//}
				
				/*else{
					for (int i = 0; i < lines.length; i++) {
						lines[i] = lines[i].trim();
					}
				}*/
				
				/*
				 * If the first line is indented and the selection is not
				 * precise (including counted whitespaces)
				 */
				if (numWhite > 0 && count > 0) {
					buff.append(String.format("%-" + numWhite + "s", ""));
				}

				buff.append(functionName);
				buff.append("<-function(){");
				buff.append(System.lineSeparator());

				if (numWhite > 0) {
					buff.append(String.format("%-" + (numWhite + defaultIndent) + "s", ""));
				} else {
					buff.append(String.format("%-" + defaultIndent + "s", ""));
				}
				buff.append(lines[0]);
				// rest of lines
				buff.append(System.lineSeparator());
				/*
				 * Write the text starting at the second line which is already
				 * formatted (whitespaces are in the text) and indent it with
				 * whitespaces (due to the selection)!
				 */
				for (int i = 1; i < lines.length; i++) {
					buff.append(String.format("%-" + defaultIndent + "s", ""));
					buff.append(lines[i]);

					buff.append(System.lineSeparator());
				}
				/* Calculate the closing parentheses! */
				if (numWhite > 0) {
					buff.append(String.format("%-" + numWhite + "s", ""));
				}
				buff.append("}");
				if(global==false){
				buff.append(System.lineSeparator());
				if (numWhite > 0) {
					buff.append(String.format("%-" + numWhite + "s", ""));
				}
				/* Write the function call! */
				
				buff.append(functionName);
				buff.append("()");
				buff.append(System.lineSeparator());
				}

				
				
				if (startToken != null) {
					if(global==false){
						/* Set function local! */
						rewriter.insertBefore(startToken, buff.toString());
					}
					else{
						StringBuffer buff2=new StringBuffer();
						//buff2.append(System.lineSeparator());
						if (numWhiteGlobalTemp > 0&& count > 0) {
							buff2.append(String.format("%-" + numWhiteGlobalTemp + "s", ""));
						}
						buff2.append(functionName);
						buff2.append("()");
						buff2.append(System.lineSeparator());
						rewriter.insertBefore(startToken, buff2.toString());	
						rewriter.insertAfter(tree.stop, buff.toString());
						}
					}
				

				
				/* We delete the selected text! */
				rewriter.delete(startToken, stopToken);
				// System.out.println("myFunc<-function(){\n\t"+rewriter.getText()+"\n}");
				// System.out.println(rewriter.getText());
			} else {
				errorMessage("Parser error occured!\nPlease select valid R expressions!");
				/* Revert changes if parser has errors! */
				doc.set(docText);
				// System.out.println("How many errors2: " +
				// parser.getNumberOfSyntaxErrors());
				return;
			}
			/* Third parse for the final result! */
			boolean errorNewText = parseSource(rewriter.getText());

			if (errorNewText == false) {
				/* Write to the editor! */
				doc.set(rewriter.getText());
				/* Scroll to the selection! */
				editor.selectAndReveal(selectionOffset, 0);
			}

			else {
				errorMessage("Parser error occured!\nPlease select valid R expressions!");
				// System.out.println("How many errors3: " +
				// parser.getNumberOfSyntaxErrors());
				// System.out.println("final" + rewriter.getText());
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
	/*
	 * The following methods from class: XmlDocumentFormatter in package:
	 * org.eclipse.ant.internal.ui.editor.formatter;
	 * *************************************************************************
	 * ****** Copyright (c) 2004, 2006s John-Mason P. Shackelford and others.
	 * All rights reserved. This program and the accompanying materials are made
	 * available under the terms of the Eclipse Public License v1.0 which
	 * accompanies this distribution, and is available at
	 * http://www.eclipse.org/legal/epl-v10.html
	 * 
	 * Contributors: John-Mason P. Shackelford - initial API and implementation
	 * IBM Corporation - bug fixes
	 *******************************************************************************/

	public static StringBuffer getLeadingWhitespace(int offset, IDocument document) {
		StringBuffer indent = new StringBuffer();
		try {
			org.eclipse.jface.text.IRegion line = document.getLineInformationOfOffset(offset);
			int lineOffset = line.getOffset();
			int nonWS = findEndOfWhiteSpace(document, lineOffset, lineOffset + line.getLength());
			indent.append(document.get(lineOffset, nonWS - lineOffset));
			return indent;
		} catch (BadLocationException e) {
			return indent;
		}
	}

	public static int getLeadingWhitespaceNumber(int offset, IDocument document) {
		int indent = 0;
		try {
			org.eclipse.jface.text.IRegion line = document.getLineInformationOfOffset(offset);
			int lineOffset = line.getOffset();
			int nonWS = findEndOfWhiteSpace(document, lineOffset, lineOffset + line.getLength());
			indent = nonWS - lineOffset;
			return indent;
		} catch (BadLocationException e) {
			return indent;
		}
	}

	public static int findEndOfWhiteSpace(IDocument document, int offset, int end) throws BadLocationException {
		while (offset < end) {
			char c = document.getChar(offset);
			if (c != ' ' && c != '\t') {
				return offset;
			}
			offset++;
		}
		return end;
	}

}
