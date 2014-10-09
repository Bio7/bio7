package com.eco.bio7.reditor.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

public class REditorRulerQuickFixAction extends AbstractRulerActionDelegate {

	/*
	 * @see AbstractRulerActionDelegate#createAction(ITextEditor,
	 * IVerticalRulerInfo)
	 */
	protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
		int line = rulerInfo.getLineOfLastMouseButtonActivity() + 1;

		
		if (line > 0) {

			IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

			IResource resource = (IResource) editore.getEditorInput().getAdapter(IResource.class);

			IMarker[] markersfind = findMyMarkers(resource);

			IMarker selectedMarker = null;

			for (int i = 0; i < markersfind.length; i++) {
				try {

					/* QuickFix produced in RAssistProcessor! */
					if (markersfind!=null&&markersfind.length > 0) {
						if (((String) markersfind[i].getAttribute(IMarker.TEXT)).startsWith("Err") && line == (int) markersfind[i].getAttribute(IMarker.LINE_NUMBER)) {
							selectedMarker = markersfind[i];
							// System.out.println(i+" "+markersfind[i].getAttribute(IMarker.MESSAGE));
							// System.out.println("Message: " +
							// selectedMarker.getAttribute(IMarker.MESSAGE));
							// System.out.println("Message: " +
							// selectedMarker.getAttribute(IMarker.LOCATION));

							ITextOperationTarget operation = (ITextOperationTarget) editor.getAdapter(ITextOperationTarget.class);

							final int opCode = ISourceViewer.QUICK_ASSIST;

							if (operation != null && operation.canDoOperation(opCode)) {

								try {
									editor.selectAndReveal((int) selectedMarker.getAttribute(IMarker.LOCATION), 1);
								} catch (CoreException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

								operation.doOperation(opCode);

							}
						}
					}
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}

		return null;
	}

	public IMarker[] findMyMarkers(IResource target) {
		String type = "org.eclipse.core.resources.problemmarker";

		IMarker[] markers = null;
		try {
			markers = target.findMarkers(type, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {

			e.printStackTrace();
		}
		return markers;
	}

}