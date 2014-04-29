package com.eco.bio7.scenebuilder.xmleditor;

import java.io.IOException;
import javafx.application.Platform;
import javafx.embed.swt.FXCanvas;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;


public class XmlReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

	private XMLEditor xmlEditor;
	// private IDocument doc;
	private EditorController editorController;
	private FXCanvas guiCanvas;
	/**
	 * How long the reconciler will wait for further text changes before
	 * reconciling
	 */
	public static final int DELAY = 1000;

	public XmlReconcilingStrategy(XMLEditor editor) {
		xmlEditor = editor;
	}

	/* Update the editor view! */
	private void doReconcile() {
        if(xmlEditor!=null){
		editorController = xmlEditor.getController();
		guiCanvas = xmlEditor.getGuiCanvas();
		// doc = xmlEditor.getDoc();
		if (editorController != null) {
			Platform.runLater(new Runnable() {

				@Override
				public void run() {

					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {
						public void run() {

							IDocumentProvider dp = xmlEditor.getDocumentProvider();
							final IDocument doc = dp.getDocument(xmlEditor.getEditorInput());

							try {
								editorController.setFxmlText(doc.get());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							guiCanvas.redraw();
							//System.out.println("changed!");
						}
					});

				}
			});
		}
        }

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.
	 * eclipse.jface.text.IRegion)
	 */
	public void reconcile(IRegion partition) {
		 doReconcile();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.
	 * eclipse.jface.text.reconciler.DirtyRegion,
	 * org.eclipse.jface.text.IRegion)
	 */
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {

		doReconcile();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org
	 * .eclipse.jface.text.IDocument)
	 */
	public void setDocument(IDocument document) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#
	 * setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void setProgressMonitor(IProgressMonitor monitor) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#
	 * initialReconcile()
	 */
	public void initialReconcile() {
		doReconcile();
	}
}