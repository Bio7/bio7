package com.eco.bio7.reditor.actions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.reditors.REditor;

public class RFormatAction extends Action {

	private int startOffset;
	private int selLength;
	private Process proc;
	private Thread processThread;

	public RFormatAction() {
		super("Format");

	}

	public void run() {

		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		ITextEditor editor = (ITextEditor) editore;
		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		ISelectionProvider sp = editor.getSelectionProvider();
		ISelection selectionsel = sp.getSelection();
		ITextSelection selection = (ITextSelection) selectionsel;

		startOffset = selection.getOffset();
		selLength = selection.getLength();

		String text = Platform.getPreferencesService().getString("com.eco.bio7", "r", "null", null);
		System.out.println(text);
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

		//System.out.println(loc);
		String pathR = text + "/bin/x64/r";

		//String send = "library(formatR);tidy.source(source ='\"\"" + loc + "\"\"',file = '\"clipboard\"');";
		RConnection rcon = REditor.getRserveConnection();
		if (rcon != null) {
			try {
				rcon.eval("library(formatR);tidy.source(source = \"" + loc + "\",file = \"clipboard\")");
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

		/*
		 * List<String> args = new ArrayList<String>(); args.add(pathR);
		 * args.add("-e"); args.add(send);
		 * //System.out.println("try(library(formatR));tidy.source(text = '"
		 * +selText+"'),file = \"clipboard\")"); ProcessBuilder pb = new
		 * ProcessBuilder(args); pb.redirectErrorStream(); try { proc =
		 * pb.start();
		 * 
		 * } catch (IOException e) {
		 * 
		 * e.printStackTrace(); }
		 * 
		 * processThread = new Thread(new ProcessGrabber());
		 * processThread.start();
		 */

		// a<-tidy.source(text = c("a<-1+1;a  # print the value",
		// "matrix(rnorm(10),5)"),file = "clipboard")

		/*
		 * try { doc.replace(startOffset, selLength, "Huiiiii"); } catch
		 * (BadLocationException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 */

	}

	class ProcessGrabber implements Runnable {
		final InputStream inp = proc.getInputStream();

		public void run() {

			try {

				InputStreamReader inr = new InputStreamReader(inp);

				int ch;
				while ((ch = inr.read()) != -1) {

					System.out.print((char) ch);

				}

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (inp != null) {
					try {
						inp.close();

					} catch (IOException e) {

					}
				}
			}

		}
	}

}