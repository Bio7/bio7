package com.eco.bio7.ijmacro.editor.actions.debug;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.ijmacro.editor.toolbar.debug.DebugMarkerAction;
import com.eco.bio7.ijmacro.editor.toolbar.debug.DebugTraceAction;
import com.eco.bio7.ijmacro.editor.toolbar.debug.DebugVariablesView;
import com.eco.bio7.ijmacro.editors.IJMacroEditor;

import ij.macro.Interpreter;

/**
 * @noreference This class is not intended to be referenced by clients.
 */
final public class DebugStepHandler extends AbstractHandler {

	public DebugStepHandler() {

	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		IJMacroEditor editor = (IJMacroEditor) editore;
		Interpreter interp = Interpreter.getInstance();
		if (interp == null) {

			DebugVariablesView debugVariablesViewInstance = DebugVariablesView.getInstance();

			if (debugVariablesViewInstance != null) {
				debugVariablesViewInstance.getDebugStopAction().setEnabled(false);
			}
			ITextSelection selection = editor.getTextSelection(editor);
			editor.selectAndReveal(selection.getOffset(), 0);
			DebugMarkerAction.setMarkerCount(0);
			editor.setMarkerExpression(null);
			DebugTraceAction.setFastTrace(false);
			

		}
		editor.setDebugMode(ij.macro.Debugger.STEP);

		return null;
	}

}