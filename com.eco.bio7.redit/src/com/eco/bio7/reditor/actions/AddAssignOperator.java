package com.eco.bio7.reditor.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import com.eco.bio7.reditors.REditor;

/**
 * @noreference This class is not intended to be referenced by clients.
 */
final public class AddAssignOperator extends AbstractHandler {

	public AddAssignOperator() {

	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		REditor reditor = (REditor) editore;
		IDocumentProvider dp = reditor.getDocumentProvider();
		IDocument doc = dp.getDocument(reditor.getEditorInput());

		ISelectionProvider sp = reditor.getSelectionProvider();
		ISelection selectionsel = sp.getSelection();
		ITextSelection selection = (ITextSelection) selectionsel;
		try {
			doc.replace(selection.getOffset(), selection.getLength(), " <- ");
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/* Scroll to the selection! */
		reditor.selectAndReveal(selection.getOffset()+4, 0);
		return null;
	}

}