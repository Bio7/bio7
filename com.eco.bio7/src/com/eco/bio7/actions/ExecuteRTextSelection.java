package com.eco.bio7.actions;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.rosuda.REngine.Rserve.RConnection;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.jobs.LineSelectionJob;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditors.REditor;

public class ExecuteRTextSelection extends Action {

	private final IWorkbenchWindow window;
	private Clipboard cb;
	private TextTransfer textTransfer;
	protected boolean canEvaluate = true;

	public ExecuteRTextSelection(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;

		setId("com.eco.bio7.rserve_selection");
		setActionDefinitionId("com.eco.bio7.execute_r_select");
	}

	public void run() {
		if (canEvaluate) {

			IEditorPart rEditor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			//IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			//boolean rPipe = store.getBoolean("r_pipe");
			RConnection con = RServe.getConnection();
			if (con==null) {
				if (rEditor instanceof REditor) {
					String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
					if (selectionConsole.equals("R")) {

						String inhalt = getTextAndForwardCursor(rEditor);

						ConsolePageParticipant.pipeInputToConsole(inhalt);
						System.out.println(inhalt);
					} else {
						Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
					}
				}

			}

			else {

					if (rEditor instanceof REditor) {

						canEvaluate = false;
						String inhalt = getTextAndForwardCursor(rEditor);

						if (inhalt.equals("") == false && inhalt != null) {
							cb = new Clipboard(Display.getDefault());
							textTransfer = TextTransfer.getInstance();

							cb.setContents(new Object[] { inhalt },

							new Transfer[] { textTransfer });
						}

						if (RState.isBusy() == false) {
							RState.setBusy(true);
							LineSelectionJob Do = new LineSelectionJob();
							Do.addJobChangeListener(new JobChangeAdapter() {
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {
										canEvaluate = true;
										int countDev = RServe.getDisplayNumber();
										RState.setBusy(false);
										if (countDev > 0) {
											RServe.closeAndDisplay();
										}
									} else {
										RState.setBusy(false);
									}
								}
							});
							Do.setUser(true);
							Do.schedule();
						} else {
							canEvaluate = true;
							Bio7Dialog.message("RServer is busy!");
						}

					} else {

						MessageBox messageBox = new MessageBox(new Shell(),

						SWT.ICON_WARNING);
						messageBox.setMessage("There is no Bio7 editor available !");
						messageBox.open();

					}

				 /*else {
					MessageBox messageBox = new MessageBox(new Shell(),

					SWT.ICON_WARNING);
					messageBox.setMessage("RServer connection failed - Server is not running !");
					messageBox.open();

				}*/
			}
		}
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