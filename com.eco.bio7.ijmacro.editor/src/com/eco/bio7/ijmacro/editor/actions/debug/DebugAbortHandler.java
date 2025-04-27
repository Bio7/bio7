package com.eco.bio7.ijmacro.editor.actions.debug;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.ijmacro.editor.toolbar.debug.DebugMarkerAction;
import com.eco.bio7.ijmacro.editor.toolbar.debug.DebugTraceAction;
import com.eco.bio7.ijmacro.editor.toolbar.debug.DebugVariablesView;
import com.eco.bio7.ijmacro.editors.IJMacroEditor;
import ij.IJ;
import ij.macro.Interpreter;

/**
 * @noreference This class is not intended to be referenced by clients.
 */
final public class DebugAbortHandler extends AbstractHandler {

	public DebugAbortHandler() {

	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		Interpreter.abort();
		IJ.beep();
		DebugVariablesView debugVariablesViewInstance = DebugVariablesView.getInstance();

		if (debugVariablesViewInstance != null) {
			debugVariablesViewInstance.getDebugStopAction().setEnabled(false);
		}
		DebugTraceAction.setFastTrace(false);
		DebugMarkerAction.setMarkerCount(0);
		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		if (editore != null) {
			IJMacroEditor editor = (IJMacroEditor) editore;
			ITextSelection selection = editor.getTextSelection(editor);
			editor.selectAndReveal(selection.getOffset(), 0);
			editor.setMarkerExpression(null);
			IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
			try {
				resource.deleteMarkers("com.eco.bio7.ijmacroeditor.debugrulermarkarrow", false, IResource.DEPTH_ZERO);
			} catch (CoreException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		/*if (table != null) {
			table.removeAll();
		}*/
		return null;
	}

}