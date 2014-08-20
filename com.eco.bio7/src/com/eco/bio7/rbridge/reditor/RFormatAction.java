package com.eco.bio7.rbridge.reditor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;

public class RFormatAction extends Action implements IObjectActionDelegate {

	private int startOffset;
	private int selLength;
	private Socket debugSocket;

	public RFormatAction() {
		super("Format");

	}

	public void run(IAction action) {

		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		NullProgressMonitor monitor = new NullProgressMonitor();
		IEditorPart[] dirtyEditors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getDirtyEditors();
		for (IEditorPart iEditorPart : dirtyEditors) {
			iEditorPart.doSave(monitor);
		}

		ITextEditor editor = (ITextEditor) editore;
		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		ISelectionProvider sp = editor.getSelectionProvider();
		ISelection selectionsel = sp.getSelection();
		ITextSelection selection = (ITextSelection) selectionsel;

		startOffset = selection.getOffset();
		selLength = selection.getLength();

		/*String selText = null;
		try {
			selText = doc.get(startOffset, selLength);
		} catch (BadLocationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/
		IEditorInput editorInput = editor.getEditorInput();
		IFile aFile = null;

		if (editorInput instanceof IFileEditorInput) {
			aFile = ((IFileEditorInput) editorInput).getFile();
		}
		String loc = aFile.getLocation().toString();

		// System.out.println(loc);
		// String pathR = text + "/bin/x64/r";

		if (RServe.getConnection() != null) {
			if (RState.isBusy() == false) {
				RConnection c = RServe.getConnection();
				REXPLogical bol = null;
				if (c != null) {

					try {
						bol = (REXPLogical) c.eval("require(formatR)");
					} catch (RserveException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					if (bol.isTRUE()[0]) {

						try {
							c.eval("library(formatR);try(tidy.source(source = \"" + loc + "\",file = \"clipboard\"))");
							// rcon.eval("tidy.source(source = \""+loc+"\",file = \"clipboard\")");
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						setClipboardData(doc);
					} else {
						Bio7Dialog.message("Library 'formatR' required!\nPlease install the 'formatR' package!");
					}
				}

			} else {
				Bio7Dialog.message("RServer is busy!");
			}

		}

		else {
			BufferedReader input = null;
			/* try with the console! */
			String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			int port = store.getInt("R_DEBUG_PORT");
			if (selectionConsole.equals("R")) {
				ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();
				con.pipeToRConsole("options(prompt=\" \")");
				con.pipeToRConsole("con1 <- socketConnection(port = " + port + ", server = TRUE)");
				con.pipeToRConsole("library(formatR);tidy.source(source = \"" + loc + "\",file = con1)");
				/*We use sockets here to wait for the clipboard data to be present (avoid parallel execution of R and Java commands caused by the threaded shell!)*/
				//con.pipeToRConsole("con1 <- socketConnection(port = " + port + ", server = TRUE)");
				// con.pipeToRConsole("writeLines(.bio7tempenv$bio7tempVar[[1]]$name, con1)");
				// con.pipeToRConsole("writeLines(as.character(.bio7tempenv$bio7tempVar[[1]]$line), con1)");

				con.pipeToRConsole("close(con1)");
				con.pipeToRConsole("writeLines(\"\")");
				con.pipeToRConsole("options(prompt=\"> \")");
               /*The following code just waits for the handshake (executed R shell code)!*/
				try {
					debugSocket = new Socket("127.0.0.1", port);

					
					try {
						input = new BufferedReader(new InputStreamReader(debugSocket.getInputStream()));
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					String strLine;
					StringBuffer str=new StringBuffer();
					try {
						while((strLine = input.readLine())!= null)
						  {
						   str.append(strLine+System.getProperty("line.separator"));
						  }
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						if(str.length()>0){
						doc.replace(0, doc.getLength(), str.toString());
						}
						else{
							Bio7Dialog.message("Library 'formatR' required!\nPlease install the 'formatR' package!");
						}
					} catch (BadLocationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					debugSocket.close();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
                 /*Clipboard data should be available!*/
				//setClipboardData(doc);
			}

			else {
				System.out.println("The R console is not accessible!");
			}
		}

	}

	private void setClipboardData(IDocument doc) {
		Clipboard cb = new Clipboard(Display.getDefault());
		TextTransfer transfer = TextTransfer.getInstance();
		String data = (String) cb.getContents(transfer);
		try {
			doc.replace(0, doc.getLength(), data);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// TODO Auto-generated method stub

	}

}