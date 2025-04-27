package com.eco.bio7.ijmacro.editor.actions.debug;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.ijmacro.editor.toolbar.debug.DebugVariablesView;
import com.eco.bio7.ijmacro.editors.IJMacroEditor;

/**
 * @noreference This class is not intended to be referenced by clients.
 */
final public class DebugHandler extends AbstractHandler {

	public DebugHandler() {

	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		IJMacroEditor editor = (IJMacroEditor) editore;
		editor.enableDebugging();
		/*
		 * Changes Mac OS 9 (CR) and Windows (CRLF) line separators to line feeds (LF).
		 */
		editor.fixLineEndings();
		editor.runMacro(true);
		if (editor.isDirty()) {
			editor.doSave(new NullProgressMonitor());
		}
		DebugVariablesView debugVariablesViewInstance = DebugVariablesView.getInstance();

		if (debugVariablesViewInstance != null) {
			debugVariablesViewInstance.getDebugStopAction().setEnabled(true);
		}
		return null;
	}

}