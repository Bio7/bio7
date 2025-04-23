package com.eco.bio7.reditor.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.reditors.REditor;

/**
 * @noreference This class is not intended to be referenced by clients.
 */
final public class TextZoomInHandler extends AbstractHandler {

	public TextZoomInHandler() {
		
		
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		REditor reditor=(REditor)editore;
		reditor.increase();
		return null;
	}

}