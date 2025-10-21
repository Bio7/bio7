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

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.antlr.refactor.RefactorParse;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.util.Util;

public class RFormatSelectionAction implements IObjectActionDelegate, IEditorActionDelegate {
  
	private IProject iproj;
	protected String str;
	private IPreferenceStore store;
	protected boolean styler;
	protected String[] out;
	private String stylerArguments;
	private String charset;

	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		action.setActionDefinitionId("com.eco.bio7.reditor.rserve.format.selection");
		if (targetEditor != null)
			targetEditor.getSite().getKeyBindingService().registerAction(action);
	}

	public void run(IAction action) {
		store = Bio7REditorPlugin.getDefault().getPreferenceStore();
		styler = store.getBoolean("STYLER_PACKAGE");
		stylerArguments = store.getString("STYLER_PACKAGE_ARGUMENTS");
		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		IEditorPart[] dirtyEditors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getDirtyEditors();
		for (IEditorPart iEditorPart : dirtyEditors) {
			iEditorPart.doSave(null);
		}
		/* Give editor time to save (since it is a non blocking operation!) */
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ITextEditor editor = (ITextEditor) editore;

		/*
		 * String selText = null; try { selText = doc.get(startOffset, selLength); }
		 * catch (BadLocationException e1) { // TODO Auto-generated catch block
		 * e1.printStackTrace(); }
		 */
		IEditorInput editorInput = editor.getEditorInput();
		IFile aFile = null;

		if (editorInput instanceof IFileEditorInput) {
			aFile = ((IFileEditorInput) editorInput).getFile();
			try {
				charset = aFile.getCharset();
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			iproj = aFile.getProject();
		}

		ITextEditor editor2 = (ITextEditor) editor;

		IDocumentProvider dp = editor2.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		ISelectionProvider sp = editor2.getSelectionProvider();
		ISelection selectionsel = sp.getSelection();
		ITextSelection selection = (ITextSelection) selectionsel;
		String toFormat = selection.getText();
		int off = selection.getOffset();
		int selectionLength = selection.getLength();
		/* We use the refactor parser to parse for errors in the selection! */
		RefactorParse parse = new RefactorParse();
		boolean errors = parse.parseSource(toFormat, false);
		if (errors == true) {

			errorMessage("Parser error occured!\nPlease select valid R expressions!");
			return;

		}

		/*
		 * try { doc.replace(off, 0, items[0]+System.lineSeparator()); } catch
		 * (BadLocationException e1) { e1.printStackTrace(); }
		 */

		RConnection con = REditor.getRserveConnection();
		if (con != null) {
			Job job = new Job("formatR") {

				REXPLogical bol = null;
				String fileOutput;

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Format R source ...", IProgressMonitor.UNKNOWN);

					if (con != null) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							if (styler == false) {
								try {
									bol = (REXPLogical) con.eval("require(formatR)");
								} catch (RserveException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								if (bol.isTRUE()[0]) {
									String comments = String.valueOf(store.getBoolean("FORMAT_COMMENTS")).toUpperCase();
									String blankLines = String.valueOf(store.getBoolean("FORMAT_BLANK_LINES")).toUpperCase();
									String replaceAssig = String.valueOf(store.getBoolean("FORMAT_REPLACE_ASSIGNMENT")).toUpperCase();
									String bracesNewline = String.valueOf(store.getBoolean("FORMAT_BRACES_NEWLINE")).toUpperCase();
									int numSpaces = store.getInt("FORMAT_NUMBER_SPACES");
									int minLine = store.getInt("FORMAT_MINIMUM_LINE");

									try {
										con.eval(".temprformatfile<-tempfile()");
										con.assign(".temprormattext", toFormat);
										con.eval("library(formatR);try(tidy_source(text = c(.temprormattext)" + ",comment = " + comments + ",blank = " + blankLines + ",arrow = " + replaceAssig
												+ ",brace.newline = " + bracesNewline + ",indent = " + numSpaces + ",width.cutoff = " + minLine + ",file=.temprformatfile))");
										fileOutput = (String) con.eval(".temprformatfile").asString();
									} catch (RserveException | REXPMismatchException e2) {
										// TODO Auto-generated catch block
										e2.printStackTrace();
									}

									File tempFile = new File(fileOutput);
									str = "";
									try {
										str = FileUtils.readFileToString(tempFile, "UTF-8");

									} catch (IOException e) {

										e.printStackTrace();
									}
									tempFile.delete();
									Display display = Util.getDisplay();
									display.asyncExec(new Runnable() {

										public void run() {
											try {
												doc.replace(off, selectionLength, str);
											} catch (BadLocationException e1) {
												e1.printStackTrace();
											}
										}
									});

								} else {
									message("Library 'formatR' required!\nPlease install the 'formatR' package!");
								}
							} else {

								if (charset.equals("UTF-8")) {

									try {
										bol = (REXPLogical) con.eval("require(styler)");
									} catch (RserveException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
									if (bol.isTRUE()[0]) {

										try {

											con.assign(".temprormattext", toFormat.replace("\r", ""));
											con.eval("library(styler)");
											try {
												out = con.eval("try(style_text(.temprormattext" + stylerArguments + "))").asStrings();
											} catch (REXPMismatchException e) {

												e.printStackTrace();
											}

										} catch (RserveException e2) {

											e2.printStackTrace();
										}
										StringBuffer buff = new StringBuffer();
										for (int i = 0; i < out.length; i++) {
											buff.append(out[i]);
											if (i < out.length - 1) {
												buff.append(System.getProperty("line.separator"));
											}
										}
										String output = buff.toString();
										buff.delete(0, buff.length() - 1);
										Display display = Util.getDisplay();
										display.asyncExec(new Runnable() {

											public void run() {
												try {
													doc.replace(off, selectionLength, output);
												} catch (BadLocationException e1) {
													e1.printStackTrace();
												}
											}
										});

									} else {
										message("Library 'styler' required!\nPlease install the 'styler' package!");
									}

								} else {
									message("Text encoding 'UTF-8' is required for the styler package!");
								}
							}

						} else {
							message("Rserve is busy!");
						}
					}

					monitor.done();
					return Status.OK_STATUS;
				}

			};
			job.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {

						RState.setBusy(false);
					} else {

					}
				}
			});
			// job.setSystem(true);
			job.schedule();
		} else {
			message("Rserve is not alive!");
		}

	}

	/**
	 * Opens a message Dialog with the given text.
	 * 
	 * @param text
	 *            the text for the dialog.
	 */
	public void message(final String text) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				MessageBox messageBox = new MessageBox(Util.getShell(),

						SWT.ICON_WARNING);
				messageBox.setText("Info!");
				messageBox.setMessage(text);
				messageBox.open();
			}
		});
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {

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