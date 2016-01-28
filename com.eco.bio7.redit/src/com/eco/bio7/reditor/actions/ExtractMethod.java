package com.eco.bio7.reditor.actions;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
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
import com.eco.bio7.reditor.antlr.refactor.RefactorDialog;
import com.eco.bio7.reditor.antlr.refactor.RefactorDocUtil;
import com.eco.bio7.reditor.antlr.refactor.RefactorParse;
import com.eco.bio7.util.Util;

public class ExtractMethod implements IEditorActionDelegate {

	private ISelection selection;
	private IEditorPart targetEditor;
	private TokenStreamRewriter rewriter;
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
			RefactorParse parse=new RefactorParse();
			/*int startLine = selection.getStartLine();
			int selLength = selection.getLength();
			StringBuffer tempBuff = new StringBuffer();
			We need to handle the selected lines differentely to calculate indention, etc.!
			String[] linesPrep = text.split(System.getProperty("line.separator"));
			
			int enOffsetWs = 0;
			int offStart = 0;
			org.eclipse.jface.text.IRegion line = null;
			
			for (int i = 0; i < linesPrep.length; i++) {
				//System.out.println(linesPrep[i]);
				try {
					offStart=doc.getLineOffset(startLine+i);
					line = doc.getLineInformationOfOffset(offStart);
					
					enOffsetWs=findEndOfWhiteSpace(doc,offStart,line.getLength());
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				//int WsResult=enOffsetWs-offStart;
				linesPrep[i] = linesPrep[i].trim();
				System.out.println("linelength: "+line.getLength()+" starline: "+(startLine+i)+" "+"linetrim: "+linesPrep[i]+" offstart: "+offStart+" endOffset: "+enOffsetWs);
				System.out.println(linesPrep[i]);
					
					//tempBuff.append(String.format("%-" + enOffsetWs + "s", ""));
					tempBuff.append(linesPrep[i]);
					tempBuff.append(System.lineSeparator());
				
			}
			//System.out.println(tempBuff.toString());
			try {
				doc.replace(doc.getLineOffset(startLine), selLength, tempBuff.toString());
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/

			//String text = selection.getText();
			
			

			/* First parse of the selection! */
			boolean errors = parse.parseSource(text);
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
				//doc.set(docText);
				// System.out.println("How many errors1: " +
				// parser.getNumberOfSyntaxErrors());
				return;
			}
			/* Second parse of the whole text without the selected text! */
			String fullTextWithoutSel = doc.get();
			boolean error = parse.parseSource(fullTextWithoutSel);

			/* Extract selection offset! */
			int selectionOffset = selection.getOffset();
			if (error == false) {
				rewriter = new TokenStreamRewriter(parse.getTokens());

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

					} else if (selectionOffset + selection.getLength() >= currentToken) {
						stopToken = tempToken;

					}
				}

				/*
				 * If we have found the token with the required offset we
				 * insert!
				 */
				int defaultIndent = 2;//default indention after function wrapper!
				int numWhite = RefactorDocUtil.getLeadingWhitespaceNumber(selectionOffset, doc);//calculate the indention of the first selected line!
				int numWhiteGlobalTemp = numWhite;//Create a temporary variable for the global method!
				if (global) {
					numWhite = 0;//For global methods we start at 0!

				}
				StringBuffer buff = new StringBuffer();//StringBuffer preserves the whitespace of the selection!
                /*We need to handle the selected lines differentely to calculate indention, etc.!*/
				String[] lines = text.split(System.getProperty("line.separator"));
				/*
				 * Calculate the leading whitespaces in the first line
				 * (selection could be with whitespaces so we need the correct value here)!
				 */
				int count = lines[0].indexOf(lines[0].trim());
				/*First line remove leading whitespace from selection text!*/

				lines[0] = lines[0].trim();

				/*
				 * If the first line is indented and the selection is not
				 * precise (including counted whitespaces)
				 */
				if (numWhite > 0 && count > 0) {
					buff.append(String.format("%-" + numWhite + "s", ""));
				}
				/* if the doc has no linebreak at the end! */
				if (global) {
					buff.append(System.lineSeparator());
				}
				/*Wrap in function!*/
				buff.append(functionName);
				buff.append("<-function(){");
				buff.append(System.lineSeparator());

				if (numWhite > 0) {
					buff.append(String.format("%-" + (numWhite + defaultIndent) + "s", ""));
				} else {
					buff.append(String.format("%-" + defaultIndent + "s", ""));
				}
				/* Write the content of the the first line!*/
				buff.append(lines[0]);				
				buff.append(System.lineSeparator());
				/*
				 * Write the text starting at the second line which is already
				 * formatted (whitespaces are in the text) and indent it with
				 * whitespaces (due to the selection)!
				 */

				if (global == false) {
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
				}

				else {

					for (int i = 1; i < lines.length; i++) {

						int amountWs = lines[i].indexOf(lines[i].trim());
						/*
						 * Calculate the difference from the first selected line
						 * to indent properly for global methods. Preserve the
						 * indention of the following lines relative to the
						 * first line!
						 */
						int res = amountWs - numWhiteGlobalTemp;
						/* Remove all whitespaces! */
						lines[i] = lines[i].trim();
						/*
						 * If the following lines are indented relative to the
						 * first sel line write the difference as whitespaces!
						 */
						if (res > 0) {
							buff.append(String.format("%-" + (res + defaultIndent) + "s", ""));
						} else {
							buff.append(String.format("%-" + defaultIndent + "s", ""));
						}
						/* Write line the content! */
						buff.append(lines[i]);

						buff.append(System.lineSeparator());
					}
					/* Calculate the closing parentheses! */
					/*
					 * if (numWhiteGlobalTemp > 0) {
					 * buff.append(String.format("%-" + numWhiteGlobalTemp +
					 * "s", "")); }
					 */
					buff.append("}");

				}

				if (global == false) {
					buff.append(System.lineSeparator());
					if (numWhite > 0) {
						buff.append(String.format("%-" + numWhite + "s", ""));
					}
					/* Write the function call! */

					buff.append(functionName);
					buff.append("()");
					buff.append(System.lineSeparator());
				}
               /*Use the TokenStreamRewriter of ANTLR to insert the changes!*/
				if (startToken != null) {
					if (global == false) {
						/* Set function local! */
						rewriter.insertBefore(startToken, buff.toString());
					} else {
						StringBuffer buff2 = new StringBuffer();

						if (numWhiteGlobalTemp > 0 && count > 0) {
							buff2.append(String.format("%-" + numWhiteGlobalTemp + "s", ""));
						}
						buff2.append(functionName);
						buff2.append("()");
						buff2.append(System.lineSeparator());
						rewriter.insertBefore(startToken, buff2.toString());
						rewriter.insertAfter(parse.getTree().stop, buff.toString());
					}
				}

				/* We delete the selected text with the TokenStreamRewriter!*/
				rewriter.delete(startToken, stopToken);

			} else {
				errorMessage("Parser error occured!\nPlease select valid R expressions!");
				/* Revert changes if parser has errors! */
				//doc.set(docText);
				return;
			}
			/* Third parse for the final result! */
			boolean errorNewText = parse.parseSource(rewriter.getText());

			if (errorNewText == false) {
				/* Write to the editor! */
				doc.set(rewriter.getText());
				/* Scroll to the selection! */
				editor.selectAndReveal(selectionOffset, 0);
			}

			else {
				errorMessage("Parser error occured!\nPlease select valid R expressions!");
				doc.set(docText);
				return;
			}

		}

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
