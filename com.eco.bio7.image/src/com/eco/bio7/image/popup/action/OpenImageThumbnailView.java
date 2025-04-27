package com.eco.bio7.image.popup.action;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.image.Util;
import com.eco.bio7.image.thumbs.ThumbnailAction;

public class OpenImageThumbnailView extends Action implements IObjectActionDelegate {

	private IPreferenceStore store;

	public OpenImageThumbnailView() {
		super();
		setId("com.eco.bio7.image.open.thumbnail.view");
		setActionDefinitionId("com.eco.bio7.image.open.thumbnail.view");
		
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {
		PlatformUI.getWorkbench().saveAllEditors(true);

		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService()
				.getSelection();
		IStructuredSelection strucSelection = null;
		if (selection instanceof IStructuredSelection) {
			strucSelection = (IStructuredSelection) selection;

			Object selectedObj = strucSelection.getFirstElement();

			if (selectedObj instanceof IFolder || selectedObj instanceof IProject) {
				IFolder selectedFolder = null;
				String loc;
				if (selectedObj instanceof IProject) {
					IProject proj = (IProject) selectedObj;
					loc = proj.getLocation().toOSString();

				} else {
					selectedFolder = (IFolder) selectedObj;
					loc = selectedFolder.getLocation().toString();

				}

				String cleanedPath = loc.replace("\\", "/");
				
				Util.openView("com.eco.bio7.thumbnails");
				
				ThumbnailAction thumbAction=new ThumbnailAction();
				thumbAction.loadDirectoryJob(cleanedPath);

			}
		}

	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
