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
import com.eco.bio7.rbridge.backend.RBackend;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RBridge;
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
			RBackend con = RBridge.getBackend();
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
				/* While selections run, further ones are queued; else R is busy with something else */
				if (RState.isBusy() && !isSelectionRunning()) {
					System.out.println("R is busy. Can't execute the R script!");
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

							/* Queued (no join: the UI stays free); the file is deleted when it was evaluated */
							if (temp != null) {
								submit(null, temp, true);
							}
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
		/* The selections not yet evaluated are dropped */
		clearQueue();
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
	 * Evaluates a script (or the script file loc) in R in a job. The call returns
	 * at once (the UI stays free); selections are evaluated one after another in
	 * the order of the calls.
	 *
	 * @param script a script.
	 * @param loc    the script location.
	 */
	public static void evalRSelection(String script, String loc) {
		submit(script, loc == null ? null : new File(loc), false);
	}

	/* ---------------- The queue of the selections ---------------- */

	private record Selection(String script, File file, boolean deleteFile) {
	}

	private static final java.util.ArrayDeque<Selection> queue = new java.util.ArrayDeque<>();
	/* A selection job runs (RState is busy because of it) */
	private static boolean running;

	private static synchronized boolean isSelectionRunning() {
		return running;
	}

	private static void submit(String script, File file, boolean deleteFile) {
		if (!RServe.isAliveDialog()) {
			if (deleteFile && file != null) {
				file.delete();
			}
			return;
		}
		synchronized (ExecuteRTextSelection.class) {
			queue.add(new Selection(script, file, deleteFile));
		}
		startNext();
	}

	private static void clearQueue() {
		synchronized (ExecuteRTextSelection.class) {
			for (Selection s : queue) {
				if (s.deleteFile() && s.file() != null) {
					s.file().delete();
				}
			}
			queue.clear();
		}
	}

	/* Starts the next selection if none runs and R is free; else it waits (retry) */
	private static void startNext() {
		Selection next;
		synchronized (ExecuteRTextSelection.class) {
			if (running || queue.isEmpty()) {
				return;
			}
			if (RState.isBusy()) {
				/* R is busy with something else (e.g., the object list): try again soon */
				retryLater();
				return;
			}
			next = queue.poll();
			running = true;
			RState.setBusy(true);
		}
		RInterpreterJob job = new RInterpreterJob(next.script(), next.file() == null ? null : next.file().getAbsolutePath());
		job.setUser(true);
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (next.deleteFile() && next.file() != null) {
					next.file().delete();
				}
				boolean ok = event.getResult().isOK();
				int countDev = ok ? RServe.getDisplayNumber() : 0;
				/* R free first, then not running: a selection in between is queued, not refused */
				RState.setBusy(false);
				boolean more;
				synchronized (ExecuteRTextSelection.class) {
					running = false;
					more = !queue.isEmpty();
				}
				if (countDev > 0) {
					RServe.closeAndDisplayNoJoin();
				}
				if (ok && !more) {
					/* The object list after the last queued selection */
					RServeUtil.listRObjects();
				}
				startNext();
			}
		});
		job.schedule();
	}

	private static void retryLater() {
		org.eclipse.core.runtime.jobs.Job retry = new org.eclipse.core.runtime.jobs.Job("Evaluate R selection (waiting)") {
			@Override
			protected org.eclipse.core.runtime.IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
				startNext();
				return org.eclipse.core.runtime.Status.OK_STATUS;
			}
		};
		retry.setSystem(true);
		retry.schedule(200);
	}
}