package com.eco.bio7.rbridge.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.rosuda.REngine.Rserve.RConnection;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RServeUtil;
import com.eco.bio7.reditor.antlr.Parse;
import com.eco.bio7.reditors.REditor;

public class ExecuteRTextSelection extends Action {

	private final IWorkbenchWindow window;
	protected boolean canEvaluate = true;
	private StringBuffer buff;
	private String code;
	private boolean error;
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

			IEditorPart rEditor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
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

				if (rEditor instanceof REditor) {

					// canEvaluate = false;
					String inhalt = getTextAndForwardCursor(rEditor);
					if (inhalt.isEmpty() == false) {
						buff.append(inhalt);
						buff.append("\n");
						Parse parse = new Parse(null);
						code = buff.toString().replaceAll("\r", "");
						error = parse.parseShellSource(code, 0);
						if (error == false) {
							System.out.println(code);
							RServeUtil.evalR("try(" + code + ")", null);
							buff.setLength(0); // clear buffer!
						} else {
							/*Data will be appended: Buffer will not be cleared until we have valid r code or an interrupt
							 *signal!*/
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

				/*
				 * else { MessageBox messageBox = new MessageBox(new Shell(),
				 * 
				 * SWT.ICON_WARNING);
				 * messageBox.setMessage("RServer connection failed - Server is not running !");
				 * messageBox.open();
				 * 
				 * }
				 */
			}
		}
	}

	public void stopEvaluation() {
		error = false;
		RServeUtil.evalR("try(" + code + ")", null);
		buff.setLength(0); // clear buffer!
	}

	private String getTextAndForwardCursor(IEditorPart rEditor) {
		ITextEditor editor = (ITextEditor) rEditor;
		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		ISelectionProvider sp = editor.getSelectionProvider();

		ISelection selectionsel = sp.getSelection();

		ITextSelection selection = (ITextSelection) selectionsel;

		int b = selection.getStartLine();
		String inhalt = null;
		IRegion reg = null;
		try {
			reg = doc.getLineInformation(b);
		} catch (BadLocationException e1) {

			e1.printStackTrace();
		}

		try {
			inhalt = doc.get(reg.getOffset(), reg.getLength());
		} catch (BadLocationException e) {

			e.printStackTrace();
		}

		editor.selectAndReveal(reg.getOffset() + 1 + reg.getLength() + 1, 0);
		return inhalt;
	}
}