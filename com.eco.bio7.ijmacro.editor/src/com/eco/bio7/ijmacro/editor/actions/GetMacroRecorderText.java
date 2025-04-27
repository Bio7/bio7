package com.eco.bio7.ijmacro.editor.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import ij.IJ;
import ij.plugin.frame.Recorder;

public class GetMacroRecorderText extends AbstractHandler {

	public GetMacroRecorderText() {

	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		ITextEditor textEditor = (ITextEditor) editor;
		IDocumentProvider dp = textEditor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		ISelectionProvider sp = textEditor.getSelectionProvider();

		ISelection selectionsel = sp.getSelection();

		ITextSelection selection = (ITextSelection) selectionsel;

		int b = selection.getStartLine();
		IRegion reg = null;
		try {
			reg = doc.getLineInformation(b);
		} catch (BadLocationException e1) {

			e1.printStackTrace();
		}

		if (!Recorder.record) {
			IJ.error("Macro recorder not available!");
			return null;
		}

		String text = Recorder.getInstance().getText();

		if (text == null || text.equals("")) {
			IJ.showMessage("Recorder", "A macro cannot be created until at least\none command has been recorded.");
			return null;
		}

		int offset = reg.getOffset();
		try {
			doc.replace(offset, 0, text);
		} catch (BadLocationException e) {// TODO Auto-generated catch block
			e.printStackTrace();
		}

		textEditor.selectAndReveal(offset + reg.getLength(), 0);

		return null;
	}

}