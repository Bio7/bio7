package com.eco.bio7.ijmacro.editor.actions.debug;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.ijmacro.editor.toolbar.debug.DebugMarkerAction;
import com.eco.bio7.ijmacro.editor.toolbar.debug.DebugTraceAction;
import com.eco.bio7.ijmacro.editor.toolbar.debug.DebugVariablesView;
import com.eco.bio7.ijmacro.editors.IJMacroEditor;

/**
 * @noreference This class is not intended to be referenced by clients.
 */
final public class RunToCompletionHandler extends AbstractHandler {

	public RunToCompletionHandler() {

	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		IJMacroEditor editor = (IJMacroEditor) editore;
		editor.setDebugMode(ij.macro.Debugger.RUN_TO_COMPLETION);
		DebugVariablesView debugVariablesViewInstance = DebugVariablesView.getInstance();

		if (debugVariablesViewInstance != null) {
			debugVariablesViewInstance.getDebugStopAction().setEnabled(false);
		}
		editor.setMarkerExpression(null);
		DebugMarkerAction.setMarkerCount(0);
		DebugTraceAction.setFastTrace(false);

		IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
		try {
			resource.deleteMarkers("com.eco.bio7.ijmacroeditor.debugrulermarkarrow", false, IResource.DEPTH_ZERO);
		} catch (CoreException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		/*
		 * int numLines = editor.getDocument().getNumberOfLines(); int lineOffset = 0;
		 * try { lineOffset = editor.getDocument().getLineOffset(numLines - 1); } catch
		 * (BadLocationException e) { e.printStackTrace(); }
		 * editor.selectAndReveal(lineOffset, 0);
		 */

		return null;
	}

}