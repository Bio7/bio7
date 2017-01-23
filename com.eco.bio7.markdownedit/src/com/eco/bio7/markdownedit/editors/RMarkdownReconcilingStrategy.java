package com.eco.bio7.markdownedit.editors;

import org.commonmark.ext.front.matter.YamlFrontMatterVisitor;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.ui.texteditor.IDocumentProvider;
import com.eco.bio7.markdownedit.parser.*;
import org.w3c.dom.Node;

public class RMarkdownReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

	
	private IDocument fDocument;

	

	/** The offset of the next character to be read */
	protected int fOffset;

	/** The end offset of the range to be scanned */
	protected int fRangeEnd;



	private MarkdownEditor markdownEditor;

	/**
	 * @return Returns the editor.
	 */
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org.
	 * eclipse.jface.text.IDocument)
	 */
	public void setDocument(IDocument document) {
		this.fDocument = document;

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
		//System.out.println("parsed!");
		/*Parse parse=editor.getParser();
		parse.parse();*/
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.
	 * eclipse.jface.text.IRegion)
	 */
	public void reconcile(IRegion partition) {
		
		Parser parser = Parser.builder().build();
		IDocumentProvider dp = markdownEditor.getDocumentProvider();
		IDocument doc = dp.getDocument(markdownEditor.getEditorInput());
		String source=doc.get();
		/*org.commonmark.node.Node document = parser.parse(source);
		HtmlRenderer renderer = HtmlRenderer.builder().build();
		renderer.render(document);  //*/		
		org.commonmark.node.Node nodey = parser.parse(source);
		Yamlheader yam = new Yamlheader();
		nodey.accept(yam);
		
		org.commonmark.node.Node node = parser.parse(source);
		CustomVisitor visitor = new CustomVisitor();
		node.accept(visitor);
		
		initialReconcile();
	}
	
	
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#
	 * setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void setProgressMonitor(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#
	 * initialReconcile()
	 */
	public void initialReconcile() {
		/*Parse parse=editor.getParser();
		parse.parse();*/

	}

	public void setEditor(MarkdownEditor markdownEditor) {
		this.markdownEditor=markdownEditor;
	}

	/**
	 * next character position - used locally and only valid while
	 * {@link #calculatePositions()} is in progress.
	 */

	

}
