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
package com.eco.bio7.reditor.actions;

import java.util.List;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.reditor.antlr.Parse;
import com.eco.bio7.reditor.antlr.RParser.E19DefFunctionContext;
import com.eco.bio7.reditor.antlr.RParser.E20CallFunctionContext;
import com.eco.bio7.reditor.antlr.RParser.ExprContext;
import com.eco.bio7.reditor.antlr.RParser.FormContext;
import com.eco.bio7.reditor.antlr.RParser.SubContext;
import com.eco.bio7.reditor.antlr.RParser.SublistContext;
import com.eco.bio7.reditor.antlr.ref.RRefPhaseListen;
import com.eco.bio7.reditor.antlr.refactor.RefactorDocUtil;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.util.Util;

public class InsertRoxygenComment implements IEditorActionDelegate {

	private IEditorPart targetEditor;
	private TokenStreamRewriter rewriter;

	private REditor rEditor;

	public void setActiveEditor(final IAction action, final IEditorPart targetEditor) {
		this.targetEditor = targetEditor;
		action.setActionDefinitionId("com.eco.bio7.reditor.insert.roxygen");

		if (targetEditor != null)
			targetEditor.getSite().getKeyBindingService().registerAction(action);

	}

	public void run(final IAction action) {

		IResource resource = (IResource) targetEditor.getEditorInput().getAdapter(IResource.class);
		if (resource != null) {

			ITextEditor editor = (ITextEditor) targetEditor;
			rEditor = (REditor) editor;
			IDocumentProvider dp = editor.getDocumentProvider();
			IDocument doc = dp.getDocument(editor.getEditorInput());
			ISelectionProvider sp = editor.getSelectionProvider();
			ISelection selectionsel = sp.getSelection();
			ITextSelection selection = (ITextSelection) selectionsel;

			/* Extract selection offset! */
			int selectionOffset = selection.getOffset();
			Parse parse = new Parse(rEditor);

			RRefPhaseListen ref = parse.parseFromOffset(selectionOffset);

			rewriter = new TokenStreamRewriter(parse.getTokens());

			Object inst = ref.getSelectedFunOffsetContext();

			if (inst instanceof E20CallFunctionContext) {
				E20CallFunctionContext ctx = (E20CallFunctionContext) inst;

				/* Extract the class name and class variables! */
				Token startToken = ctx.getStart();
				String className = "";
				SublistContext subList = ctx.sublist();
				className = subList.start.getText().replace("\"", "");
				String slotField[] = null;
				String slotFieldDescription[] = null;
				List<SubContext> subL = subList.sub();
				String type = "";
				String line = System.getProperty("line.separator");
				StringBuffer str = new StringBuffer();
				String classType = ctx.expr().getChild(2).getText();

				if (classType.equals("setClass")) {
					str.append("#' A S4 class implementation of " + className);
					str.append(line);
				} else if (classType.equals("setRefClass")) {
					str.append("#' A reference class implementation of " + className);
					str.append(line);
				}

				else if (classType.equals("R6Class")) {
					str.append("#' An R6 class implementation of " + className);
					str.append(line);
				} else {
					str.append("#' A class implementation of " + className);
					str.append(line);

				}

				int ws = RefactorDocUtil.getLeadingWhitespaceNumber(startToken.getStartIndex(), doc);

				for (int x = 0; x < subL.size(); x++) {
					if (subL.get(x) instanceof SubContext) {
						SubContext su = (SubContext) subL.get(x);

						if (su.expr() != null) {
							type = su.expr().getStart().getText();
							/* We try to extract the public variables form the S4, R5 and R6 classes! */
							if (type.equals("slots") || type.equals("fields") || type.equals("public")) {

								ExprContext ex = su.expr();
								ParseTree tree = ex.getChild(2);
								if (tree != null && tree instanceof SublistContext) {
									SublistContext ex2 = (SublistContext) tree;
									List<SubContext> sub2 = ex2.sub();
									int size = sub2.size();
									slotField = new String[size];
									slotFieldDescription = new String[size];
									for (int j = 0; j < size; j++) {

										SubContext slotIds = (SubContext) sub2.get(j);
										slotField[j] = slotIds.start.getText();
										if (slotIds.expr().getChild(2) != null) {
											ParseTree child = slotIds.expr().getChild(2);
											/* We omit functions and lists here! */
											if (child instanceof E19DefFunctionContext || child instanceof SublistContext) {

												slotFieldDescription[j] = null;

											} else {

												slotFieldDescription[j] = slotIds.expr().getChild(2).getChild(0).getText();

											}
										} else {
											slotFieldDescription[j] = null;
										}

									}

								}

								/* Set function local! */
								str.append("#' ");
								str.append(line);

								if (slotField != null && slotField.length > 0) {
									for (int i = 0; i < slotField.length; i++) {
										if (slotFieldDescription != null && slotFieldDescription.length > 0) {

											if (type.equals("slots")) {
												if (slotFieldDescription[i] != null) {
													str.append("#' @slot " + slotField[i] + " " + slotFieldDescription[i].replace("\"", ""));
													str.append(line);
												}
											} else if (type.equals("fields")) {
												if (slotFieldDescription[i] != null) {
													str.append("#' @field " + slotField[i] + " " + slotFieldDescription[i].replace("\"", ""));
													str.append(line);
												}
											} else if (type.equals("public")) { // || type.equals("private")) {

												if (slotFieldDescription[i] != null) {
													str.append("#' @field " + slotField[i] + " The public field " + slotField[i] + "."); // + " " + slotFieldDescription[i].replace("\"", ""));
													str.append(line);
												}
												/*
												 * } else { //str.append("#' @private " + slotField[i] + " " + slotFieldDescription[i].replace("\"", "")); }
												 */
											}
											else if (type.equals("private")) {
												str.append("#' @private " + slotField[i] + " " + slotFieldDescription[i].replace("\"", ""));
											}

										} else {
											if (type.equals("slots")) {
												str.append("#' @slot " + slotField[i] + "");
											} else if (type.equals("fields")) {
												str.append("#' @field " + slotField[i] + "");
											} else if (type.equals("public") || type.equals("private")) {
												if (type.equals("public")) {
													str.append("#' @public " + slotField[i] + "");
												} /*
													 * else { str.append("#' @private " + slotField[i] + ""); }
													 */
											}
											str.append(line);
										}

									}
								}

							}

						}
					}
				}
				str.append("#'");
				str.append(line);
				str.append("#' @return");
				str.append(line);
				str.append("#' @export");
				str.append(line);
				str.append("#'");
				str.append(line);
				str.append("#' @examples");
				str.append(line);
				if (ws > 0) {
					str.append(String.format("%-" + ws + "s", ""));
				}
				rewriter.insertBefore(startToken, str.toString());

				/* Write to the editor! */
				doc.set(rewriter.getText());
				/* Scroll to the selection! */
				editor.selectAndReveal(startToken.getStartIndex(), 0);

			} else if (inst instanceof E19DefFunctionContext) {
				E19DefFunctionContext ctx = (E19DefFunctionContext) inst;
				Token st = ref.getTokens().get(ctx.getParent().getChild(0).getSourceInterval().a);
				/*
				 * We store the selected function offset to create roxygen comments!
				 */

				String[] selFunctionCallVars;
				/*
				 * Add the function arguments!
				 */
				if (ctx.formlist() != null) {
					List<FormContext> formList = ctx.formlist().form();
					int functionDefSize = formList.size();
					selFunctionCallVars = new String[functionDefSize];
					for (int i = 0; i < functionDefSize; i++) {
						FormContext fo = formList.get(i);

						TerminalNode ar = fo.ID();
						/* If we have an ID or ID assignment! */
						if (ar != null) {
							selFunctionCallVars[i] = ar.getText();

						} else {
							/* If we hav an ellipsis '...' as argument! */
							selFunctionCallVars[i] = fo.getText();
						}

					}
				} else {
					selFunctionCallVars = null;
				}

				Token startToken = st;
				if (startToken != null) {

					int ws = RefactorDocUtil.getLeadingWhitespaceNumber(startToken.getStartIndex(), doc);
					/* Set function local! */
					StringBuffer str = new StringBuffer();

					String line = System.getProperty("line.separator");
					str.append("#' @title");
					str.append(line);
					/*If we have an indented function, e.g.,  in a class!*/
					if (ws > 0) {
						str.append(String.format("%-" + ws + "s", ""));
						str.append("#' ");
						str.append(String.format("%-" + ws + "s", ""));
						str.append(line);
						if (selFunctionCallVars != null && selFunctionCallVars.length > 0) {
							for (int i = 0; i < selFunctionCallVars.length; i++) {
								if (selFunctionCallVars[i].equals("...")) {
									str.append(String.format("%-" + ws + "s", ""));
									str.append("#' @param " + selFunctionCallVars[i] + " Additional arguments.");
									str.append(line);
								} else {
									str.append(String.format("%-" + ws + "s", ""));
									str.append("#' @param " + selFunctionCallVars[i] + " The " + selFunctionCallVars[i] + " argument.");
									str.append(line);
								}
							}
						}
						str.append(String.format("%-" + ws + "s", ""));
						str.append("#'");
						str.append(line);
						str.append(String.format("%-" + ws + "s", ""));
						str.append("#' @return");
						str.append(line);
						str.append(String.format("%-" + ws + "s", ""));
						str.append("#' @export");
						str.append(line);
						str.append(String.format("%-" + ws + "s", ""));
						str.append("#'");
						str.append(line);
						str.append(String.format("%-" + ws + "s", ""));
						str.append("#' @examples");
						str.append(line);
						str.append(String.format("%-" + ws + "s", ""));
					} else {
						str.append("#' ");
						str.append(line);
						if (selFunctionCallVars != null && selFunctionCallVars.length > 0) {
							for (int i = 0; i < selFunctionCallVars.length; i++) {
								if (selFunctionCallVars[i].equals("...")) {
									str.append("#' @param " + selFunctionCallVars[i] + " Additional arguments.");
									str.append(line);
								} else {
									str.append("#' @param " + selFunctionCallVars[i] + " The " + selFunctionCallVars[i] + " argument.");
									str.append(line);
								}
							}
						}
						str.append("#'");
						str.append(line);
						str.append("#' @return");
						str.append(line);
						str.append("#' @export");
						str.append(line);
						str.append("#'");
						str.append(line);
						str.append("#' @examples");
						str.append(line);
					}
					/*if (ws > 0) {
						str.append(String.format("%-" + ws + "s", ""));
					}*/
					/* Rewrite and insert the text with ANTLR! */
					rewriter.insertBefore(startToken, str.toString());

					/* Write to the editor! */
					doc.set(rewriter.getText());
					/* Scroll to the selection! */
					editor.selectAndReveal(startToken.getStartIndex(), 0);

				}

			}

		}

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

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

}
