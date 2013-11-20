/*package com.eco.bio7.reditor.actions;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class DebugRScriptAction extends Action {

	private final IWorkbenchWindow window;

	int startline;

	int stopline;

	IMarker[] markers = null;

	int marked[] = null;

	public DebugRScriptAction(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;

	}

	public IMarker[] findMyMarkers(IResource target) {
		String type = "com.eco.bio7.redit.debugMarker";

		try {
			markers = target.findMarkers(type, true, IResource.DEPTH_ZERO);

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
		if (resource != null) {
			try {

				ITextEditor editor = (ITextEditor) editore;
				ISelectionProvider sp = editor.getSelectionProvider();
				ISelection selectionsel = sp.getSelection();
				ITextSelection selection = (ITextSelection) selectionsel;

				if (!selection.isEmpty()) {
					int start = selection.getOffset();

					int length = selection.getLength();
					startline = selection.getStartLine() + 1;
					stopline = selection.getEndLine() + 1;

					IMarker[] marker = new IMarker[(stopline - startline) + 1];

					for (int i = 0; startline <= stopline; i++) {
						marker[i] = resource.createMarker(IMarker.TASK);
						marker[i].setAttribute(IMarker.LINE_NUMBER,
								new Integer(startline));
						marker[i].setAttribute(IMarker.MESSAGE,
								"Plot marker R");
						startline++;

					}

					IMarker[] markersfind = findMyMarkers(resource);
					for (int i = 0; i < markersfind.length; i++) {
						Integer a = (Integer) markersfind[i]
								.getAttribute(IMarker.LINE_NUMBER);
						int b = a.intValue();

						IEditorPart editore2 = (IEditorPart) PlatformUI
								.getWorkbench().getActiveWorkbenchWindow()
								.getActivePage().getActiveEditor();
						ITextEditor editor2 = (ITextEditor) editore2;
						IDocumentProvider prov = editor2.getDocumentProvider();
						IDocument doc = prov.getDocument(editor2
								.getEditorInput());

						String inhalt = doc.get();
						
						
						IRegion reg = null;
						try {
							reg = doc.getLineInformation(b - 1);
						} catch (BadLocationException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						try {
							inhalt = doc.get(reg.getOffset(), reg.getLength());
						} catch (BadLocationException e) {
							
							e.printStackTrace();
						}

					}

				}

			} catch (CoreException e) {

			}
		}

	}

}*/