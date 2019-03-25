package com.eco.bio7.rbridge.actions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.rosuda.REngine.Rserve.RConnection;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RServeUtil;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditor.antlr.Parse;
import com.eco.bio7.reditors.REditor;

public class ExecuteRTextSelection extends Action {

	private final IWorkbenchWindow window;
	protected boolean canEvaluate = true;
	private StringBuffer buff;
	private String code;
	private boolean error;
	private boolean interrupt = false;
	private static ExecuteRTextSelection instance;

	public static ExecuteRTextSelection getInstance() {
		return instance;
	}

	public ExecuteRTextSelection(String text, IWorkbenchWindow window) {
		super(text);
		instance = this;
		this.window = window;
		buff = new StringBuffer();

		setId("com.eco.bio7.rserve_selection");
		setActionDefinitionId("com.eco.bio7.execute_r_select");
	}

	public void run() {
		if (canEvaluate) {
			/*
			 * IWorkbenchPage page = window.getActivePage(); IEditorReference[] editors =
			 * page.getEditorReferences(); for (int i = 0; i < editors.length; i++) {
			 * 
			 * if (editors[i].getId().equals("")) {
			 * 
			 * page.activate(editors[i].getEditor(true));
			 * 
			 * } }
			 */

			IEditorPart rEditor = (IEditorPart) window.getActivePage().getActiveEditor();
			// IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			// boolean rPipe = store.getBoolean("r_pipe");
			RConnection con = RServe.getConnection();
			if (con == null) {
				if (rEditor instanceof REditor) {
					String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
					if (selectionConsole.equals("R")) {

						String inhalt = getTextAndForwardCursor(rEditor);

						ConsolePageParticipant.pipeInputToConsole(inhalt, true, true);
						System.out.println(inhalt);
					} else {
						Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
					}
				}

			}

			else {
				if (RState.isBusy()) {
					return;
				}

				if (rEditor instanceof REditor) {
					if (interrupt) {
						interrupt = false;
						return;
					}

					// canEvaluate = false;
					String inhalt = getTextAndForwardCursor(rEditor);
					inhalt.replace(System.lineSeparator(), "");
					/*
					 * Avoid commented lines (as the first character!). We evaluate R commands in a
					 * try() statement!
					 */
					if (inhalt.startsWith("#")) {
						return;
					}
					if (inhalt.isEmpty() == false) {
						buff.append(inhalt);
						buff.append("\n");
						Parse parse = new Parse(null);
						code = buff.toString();
						error = parse.parseShellSource(code, 0);
						File temp = null;
						if (error == false) {
							System.out.println(code);

							try {

								// Work with a temporary file avoids a deadlock!
								temp = File.createTempFile("tempfile", ".tmp");

								// write it
								BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
								bw.write(code);
								bw.close();

							} catch (IOException e) {

								e.printStackTrace();

							}

							// RServe.printJobJoin(code);

							evalRSelection(null, temp.getAbsolutePath());

							temp.delete();
							buff.setLength(0); // clear buffer!
						} else {
							/*
							 * Data will be appended: Buffer will not be cleared until we have valid r code
							 * or an interrupt signal!
							 */
							String[] output = code.split("\n");
							for (int i = 0; i < output.length; i++) {
								System.out.println("+ " + output[i]);
							}

						}
					}

				} else {

					MessageBox messageBox = new MessageBox(new Shell(),

							SWT.ICON_WARNING);
					messageBox.setMessage("There is no Bio7 editor available !");
					messageBox.open();

				}
			}
		}
	}

	public void stopEvaluation() {
		interrupt = true;
		// RServeUtil.evalR("try(try(" + code + "))", null);
		buff.setLength(0); // clear buffer!
	}

	private String getTextAndForwardCursor(IEditorPart rEditor) {
		ITextEditor editor = (ITextEditor) rEditor;
		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		ISelectionProvider sp = editor.getSelectionProvider();

		ISelection selectionsel = sp.getSelection();

		ITextSelection selection = (ITextSelection) selectionsel;

		int startLine = selection.getStartLine();
		int endLine = selection.getEndLine();
		String inhalt = null;
		IRegion reg = null;

		try {
			/*If we have a selection we interpret the selection form the offset and length of the selection!*/
			if (selection.getLength() > 0) {
				try {
					reg = doc.getLineInformation(endLine);
				} catch (BadLocationException e1) {

					e1.printStackTrace();
				}
				inhalt = doc.get(selection.getOffset(), selection.getLength());
				editor.selectAndReveal(reg.getOffset() + 1 + reg.getLength() + 1, 0);
			} else {
				/*If we have only the line information we interpret the line from the region offset and length!*/
				try {
					reg = doc.getLineInformation(startLine);
				} catch (BadLocationException e1) {

					e1.printStackTrace();
				}
				inhalt = doc.get(reg.getOffset(), reg.getLength());
				editor.selectAndReveal(reg.getOffset() + 1 + reg.getLength() + 1, 0);
			}
		} catch (BadLocationException e) {

			e.printStackTrace();
		}

		return inhalt;
	}
	/**
	 * Evaluates a script in R running in a job without using join for the plot
	 * job!.
	 * 
	 * @param script a script.
	 * @param loc    the script location.
	 */
	public static void evalRSelection(String script, String loc) {
		if (RServe.isAliveDialog()) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				RInterpreterJob Do = new RInterpreterJob(script, loc);
				Do.setUser(true);
				Do.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							int countDev = RServe.getDisplayNumber();
							RState.setBusy(false);
							if (countDev > 0) {
								RServe.closeAndDisplayNoJoin();
							}
							RServeUtil.listRObjects();
							// BatchModel.resumeFlow();

						} else {
							RState.setBusy(false);
						}
					}
				});

				Do.schedule();
				try {
					Do.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				System.out.println("Rserve is busy. Can't execute the R script!");
			}
		}

	}
}