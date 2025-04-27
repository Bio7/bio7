package com.eco.bio7.ijmacro.editor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class ImageJForumCopy extends Action {

	public ImageJForumCopy() {
		super("ImageJForumCopy");
		setActionDefinitionId("com.eco.bio7.ijmacro.editor.copy.imagej.forum");

		setText("Copy to ImageJ Forum");
	}

	public void run() {
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		ITextEditor editor2 = (ITextEditor) editor;
		ISelectionProvider sp = editor2.getSelectionProvider();
		ISelection selectionsel = sp.getSelection();
		ITextSelection selection = (ITextSelection) selectionsel;
		String code = selection.getText();
		StringBuffer buff = new StringBuffer();
		buff.append("```javascript"+System.lineSeparator());
		buff.append(code);
		buff.append(System.lineSeparator()+"```");
		String resultedCode = buff.toString();
		Clipboard clipboard = new Clipboard(getDisplay());
		TextTransfer textTransfer = TextTransfer.getInstance();
		clipboard.setContents(new String[] { resultedCode }, new Transfer[] { textTransfer });
		clipboard.dispose();
	}

	/**
	 * Returns a default display.
	 * 
	 * @return a display
	 */
	public static Display getDisplay() {
		Display display = Display.getCurrent();
		// may be null if outside the UI thread
		if (display == null)
			display = Display.getDefault();
		return display;
	}

	public void dispose() {

	}

}