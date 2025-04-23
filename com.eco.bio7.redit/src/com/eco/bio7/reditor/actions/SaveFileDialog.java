package com.eco.bio7.reditor.actions;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.util.Util;

public class SaveFileDialog extends Action {

	private IWorkbenchWindow window;

	public SaveFileDialog(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;
		setActionDefinitionId("com.eco.bio7.r_editor_save_file_dialog");

	}

	public void run() {

		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		ITextEditor editor = (ITextEditor) editore;
		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		ISelectionProvider sp = editor.getSelectionProvider();
		ISelection selectionsel = sp.getSelection();
		ITextSelection selection = (ITextSelection) selectionsel;
		int offset = selection.getOffset();
		int length = selection.getLength();
		
		new SaveFileCreateSourceTemplate(doc, offset, length,null);

	}

}