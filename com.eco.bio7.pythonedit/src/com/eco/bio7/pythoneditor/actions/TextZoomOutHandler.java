package com.eco.bio7.pythoneditor.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.editors.python.PythonEditor;

/**
 * @noreference This class is not intended to be referenced by clients.
 */
final public class TextZoomOutHandler extends AbstractHandler {

	public TextZoomOutHandler() {

	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		PythonEditor editor = (PythonEditor) editore;
		editor.decrease();
		return null;
	}

}