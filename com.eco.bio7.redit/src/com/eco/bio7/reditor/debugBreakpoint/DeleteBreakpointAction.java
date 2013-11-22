package com.eco.bio7.reditor.debugBreakpoint;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

public class DeleteBreakpointAction extends AbstractRulerActionDelegate implements IEditorActionDelegate {

	private class ToggleBreakpointAction extends Action {

		private final IVerticalRulerInfo mRulerInfo;
		private final ITextEditor mEditor;
		int startline;

		int stopline;

		IMarker[] markers = null;

		int marked[] = null;

		public ToggleBreakpointAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
			super("Remove breakpoints");
			mEditor = editor;
			mRulerInfo = rulerInfo;
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

			IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage().getActiveEditor();

			IResource resource = (IResource) editore.getEditorInput().getAdapter(
					IResource.class);

			IMarker[] markersfind = findMyMarkers(resource);
			for (int i = 0; i < markersfind.length; i++) {
				try {
					markersfind[i].delete();
				} catch (CoreException e) {

					e.printStackTrace();
				}

			}

			int offset = 0;
			;

		}
	}

	@Override
	protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
		return new ToggleBreakpointAction(editor, rulerInfo);
	}
}

	