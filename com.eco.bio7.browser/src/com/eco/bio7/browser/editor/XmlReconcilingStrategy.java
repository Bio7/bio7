package com.eco.bio7.browser.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;


//import com.eco.bio7.browser.BrowserEditorNewView;

public class XmlReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

	private XMLEditor xmlEditor;
	/**
	 * How long the reconciler will wait for further text changes before
	 * reconciling
	 */
	public static final int DELAY = 500;

	public XmlReconcilingStrategy(XMLEditor editor) {
		xmlEditor = editor;
	}

	/* Update the HTML Editor view! */
	private void doReconcile() {
		/*final BrowserView bv = BrowserView.getBrowserInstance();

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				if (MultiPageEditor.multiEditor!= null && bv != null) {
					MultiPageEditor.multiEditor.doSave(null);
				}
				try {
					if (MultiPageEditor.multiEditor.ifile != null && bv != null) {
						bv.browser.setUrl(MultiPageEditor.multiEditor.ifile.getLocationURI().toURL().toString());
						bv.txt.setText(MultiPageEditor.multiEditor.ifile.getLocationURI().toURL().toString());
					}
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});*/

		/*
		 * if (xmlEditor != null) {
		 * 
		 * if (xmlEditor instanceof XMLEditor) { Display display =
		 * PlatformUI.getWorkbench().getDisplay(); display.asyncExec(new
		 * Runnable() {
		 * 
		 * public void run() { ITextEditor editor2 = (ITextEditor) xmlEditor;
		 * 
		 * IDocumentProvider dp = editor2.getDocumentProvider(); IDocument doc =
		 * dp.getDocument(xmlEditor.getEditorInput());
		 * if(BrowserEditorNewView.htmlEditor!=null){
		 * BrowserEditorNewView.htmlEditor.setHtmlText(doc.get()); } } });
		 * 
		 * 
		 * }
		 * 
		 * }
		 */

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