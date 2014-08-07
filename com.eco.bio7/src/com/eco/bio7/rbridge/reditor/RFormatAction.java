package com.eco.bio7.rbridge.reditor;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
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

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;

public class RFormatAction extends Action implements IObjectActionDelegate {

	private int startOffset;
	private int selLength;
	private Process proc;
	private Thread processThread;

	public RFormatAction() {
		super("Format");

	}

	public void run(IAction action) {

		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		ITextEditor editor = (ITextEditor) editore;
		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		ISelectionProvider sp = editor.getSelectionProvider();
		ISelection selectionsel = sp.getSelection();
		ITextSelection selection = (ITextSelection) selectionsel;

		startOffset = selection.getOffset();
		selLength = selection.getLength();

		String selText = null;
		try {
			selText = doc.get(startOffset, selLength);
		} catch (BadLocationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		IEditorInput editorInput = editor.getEditorInput();
		IFile aFile = null;

		if (editorInput instanceof IFileEditorInput) {
			aFile = ((IFileEditorInput) editorInput).getFile();
		}
		String loc = aFile.getLocation().toString();

		// System.out.println(loc);
		// String pathR = text + "/bin/x64/r";


		if (RServe.isAliveDialog()) {
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
					if(bol.isTRUE()[0]){
						
					
					try {
						c.eval("library(formatR);tidy.source(source = \"" + loc + "\",file = \"clipboard\")");
						// rcon.eval("tidy.source(source = \""+loc+"\",file = \"clipboard\")");
					} catch (RserveException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
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
					else{
						Bio7Dialog.message("Library 'formatR' required!\nPlease install the 'formatR' package!");
					}
				}

			} else {
				Bio7Dialog.message("RServer is busy!");
			}
			
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