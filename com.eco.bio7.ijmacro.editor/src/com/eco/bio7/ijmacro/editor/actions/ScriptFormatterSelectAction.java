package com.eco.bio7.ijmacro.editor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import com.eco.bio7.ijmacro.editor.beautifier.JSBeautifier;
import com.eco.bio7.ijmacro.editor.beautifier.JSBeautifierOptions;

public class ScriptFormatterSelectAction extends Action {

	public ScriptFormatterSelectAction() {
		super("format");
		setId("com.eco.bio7.ijmacro.format.selected");
		setActionDefinitionId("com.eco.bio7.interpret.ijmacro.select.format");
		setText("Format Selected Source");
	}

	public void run() {
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();

		ITextEditor editor2 = (ITextEditor) editor;
		IDocumentProvider dp = editor2.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		ISelectionProvider sp = editor2.getSelectionProvider();
		ISelection selectionsel = sp.getSelection();
		ITextSelection selection = (ITextSelection) selectionsel;

		int offset = selection.getOffset();
		int length = selection.getLength();

		String code = selection.getText();
		IPreferenceStore store = IJMacroEditorPlugin.getDefault().getPreferenceStore();
		String options = store.getString("IJMACRO_EDITOR_FORMAT_OPTIONS");
		/* Code from: https://github.com/sebz/eclipse-javascript-formatter */
		JSBeautifierOptions opts = new JSBeautifierOptions();
		if (options.isEmpty() == false) {
			opts.setValues(options);
		}
		String formattedString = new JSBeautifier().js_beautify(code, opts);

		try {
			doc.replace(offset, length, formattedString);
		} catch (BadLocationException e) {

			e.printStackTrace();
		}
		/* Scroll to the selection! */
		editor2.selectAndReveal(offset, 0);

	}

	public void dispose() {

	}

}