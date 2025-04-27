
package com.eco.bio7.ijmacro.editor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import com.eco.bio7.ijmacro.editor.beautifier.JSBeautifier;
import com.eco.bio7.ijmacro.editor.beautifier.JSBeautifierOptions;
import com.eco.bio7.ijmacro.editors.IJMacroEditor;

public class ScriptFormatterAction extends Action {

	public ScriptFormatterAction() {
		super("format");
		setId("com.eco.bio7.ijmacro.format");
		setActionDefinitionId("com.eco.bio7.interpret.ijmacro.format");

		setText("Format Source");
	}

	public void run() {
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput());
		ITextEditor editor2 = (ITextEditor) editor;
		ISelectionProvider sp = editor2.getSelectionProvider();
		ISelection selectionsel = sp.getSelection();
		ITextSelection selection = (ITextSelection) selectionsel;
		int offset = selection.getOffset();
		String code = doc.get();
		IPreferenceStore store = IJMacroEditorPlugin.getDefault().getPreferenceStore();
		String options = store.getString("IJMACRO_EDITOR_FORMAT_OPTIONS");
		/* Code from: https://github.com/sebz/eclipse-javascript-formatter */
		JSBeautifierOptions opts = new JSBeautifierOptions();
		if (options.isEmpty() == false) {
			opts.setValues(options);
		}
		String result = new JSBeautifier().js_beautify(code, opts);

		doc.set(result);
		/* Scroll to the selection! */
		editor2.selectAndReveal(offset, 0);
	}

	public void dispose() {

	}

}