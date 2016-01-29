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

public class ExtractVariable implements IEditorActionDelegate {

	private ISelection selection;
	private IEditorPart targetEditor;
	private TokenStreamRewriter rewriter;

	private boolean global;

	public void setActiveEditor(final IAction action, final IEditorPart targetEditor) {
		this.targetEditor = targetEditor;
		action.setActionDefinitionId("com.eco.bio7.reditor.refactor.extract.variable");
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
			String varName = "varName";

			if (text.isEmpty() || text == null) {
				errorMessage("Nothing selected!");
				return;
			}
			RefactorParse parse = new RefactorParse();
			/* First parse of the selection! */
			boolean errors = parse.parseSource(text);
			if (errors == false) {

				RefactorDialog dlg = new RefactorDialog(Display.getCurrent().getActiveShell());
				if (dlg.open() == Window.OK) {
					// User clicked OK; update the label with the input
					varName = dlg.getValue();
					global = dlg.isGlobal();
				} else {
					return;
				}

			} else {
				errorMessage("Parser error occured!\nPlease select valid R expressions!");
				/* Revert changes if parser has errors! */
				// doc.set(docText);
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

					} else if (selectionOffset + selection.getLength() - 1 >= currentToken) {
						stopToken = tempToken;

					}
				}

				if (startToken != null) {
					if (global) {
						/* Set function local! */
						StringBuffer str = new StringBuffer();
						/* if the doc has no linebreak at the end! */

						str.append(varName);
						str.append(" <- ");
						str.append(text);
						str.append(System.getProperty("line.separator"));
						rewriter.insertBefore(parse.getTree().start, str.toString());
						/*
						 * We replace the selected text with the
						 * TokenStreamRewriter!
						 */

					} else {

						int ws = RefactorDocUtil.getLeadingWhitespaceNumber(selectionOffset, doc);
						/* Set function local! */
						StringBuffer str = new StringBuffer();

						if (ws > 0) {
							str.append(String.format("%-" + ws + "s", ""));
						}
						str.append(varName);
						str.append(" <- ");
						str.append(text);
						str.append(System.getProperty("line.separator"));

						/*
						 * Here we calculate the first token in line from the
						 * token stream (note that the doc line starts with 0 so
						 * startToken.getLine()-1 is the correct line in the
						 * document!
						 */
						for (int i = 0; i < tokStream.size(); i++) {
							Token tempToken = tokStream.get(i);
							if (tempToken.getLine() == startToken.getLine()) {
								//System.out.println("ws is: " + ws);
								rewriter.insertBefore(tempToken, str.toString());
								break;
							}
						}

						/*
						 * We replace the selected text with the
						 * TokenStreamRewriter!
						 */

					}

					if (stopToken == null) {
						rewriter.replace(startToken, varName);
					} else {
						rewriter.replace(startToken, stopToken, varName);
					}

				}

			} else {
				errorMessage("Parser error occured!\nPlease select valid R expressions!");
				/* Revert changes if parser has errors! */
				// doc.set(docText);
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
