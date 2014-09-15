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
	public static final int DELAY = 500;

	public XmlReconcilingStrategy(XMLEditor editor) {
		xmlEditor = editor;
	}

	/* Update the editor view! */
	private void doReconcile() {

		if (xmlEditor != null) {
			/* Only reconcile if the input comes from the XML editor! */

			guiCanvas = xmlEditor.getGuiCanvas();
			// doc = xmlEditor.getDoc();

			Platform.runLater(new Runnable() {

				@Override
				public void run() {

					// System.out.println("changed! from reconcile");
					IDocumentProvider dp = xmlEditor.getDocumentProvider();

					try {
						editorController = xmlEditor.getController();

						IDocument doc = dp.getDocument(xmlEditor.getEditorInput());
						String content = doc.get();
						guiCanvas.redraw();
						/*	If content comes from the ScenBuilder GUI the content has already been set!
						 * 	Only change if we change the source directly!*/
						if (editorController.getFxmlText().equals(content) == false) {
							editorController.setFxmlText(content);
						} 

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					// 

				}
			});

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