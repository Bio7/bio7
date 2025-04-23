package com.eco.bio7.reditor.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class DeletePlotMarkers extends Action {

	int startline;

	int stopline;

	public DeletePlotMarkers(String text, IWorkbenchWindow window) {
		super(text);

	}

	public IMarker[] findMyMarkers(IResource target) {
		String type = "com.eco.bio7.redit.debugMarker";

		IMarker[] markers = null;
		try {
			markers = target.findMarkers(type, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {

			e.printStackTrace();
		}
		return markers;
	}

	public void run() {

		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		IResource resource = (IResource) editore.getEditorInput().getAdapter(IResource.class);

		IMarker[] markersfind = findMyMarkers(resource);
		for (int i = 0; i < markersfind.length; i++) {
			try {
				markersfind[i].delete();
			} catch (CoreException e) {

				e.printStackTrace();
			}

		}

	}

}