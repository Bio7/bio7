package com.eco.bio7.markdownedit.editors;

import java.util.Collections;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import org.commonmark.Extension;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.parser.Parser;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.IDocumentProvider;
import com.eco.bio7.markdownedit.outline.MarkdownEditorOutlineNode;
import com.eco.bio7.markdownedit.parser.CustomVisitor;
import com.eco.bio7.markdownedit.parser.Yamlheader;

public class RMarkdownReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

	private IDocument fDocument;
	private Vector<MarkdownEditorOutlineNode> editorOldNodes;
	private Stack<MarkdownEditorOutlineNode> methods;
	  private static final Set<Extension> EXTENSIONS = Collections.singleton(YamlFrontMatterExtension.create());
	  private static final Parser PARSER = Parser.builder().extensions(EXTENSIONS).build();

	/** The offset of the next character to be read */
	protected int fOffset;

	/** The end offset of the range to be scanned */
	protected int fRangeEnd;

	private MarkdownEditor markdownEditor;

	public RMarkdownReconcilingStrategy() {

		methods = new Stack<MarkdownEditorOutlineNode>();
	}

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
	 * private int getLineNumber(Element element, int line) { IDocumentProvider
	 * prov = markdownEditor.getDocumentProvider(); IEditorInput inp =
	 * markdownEditor.getEditorInput(); if (prov != null) { IDocument document =
	 * prov.getDocument(inp); if (document != null) {
	 * 
	 * try {
	 * 
	 * line = document.getLineOfOffset(element.getBegin());
	 * 
	 * } catch (BadLocationException e) {
	 * 
	 * }
	 * 
	 * } } return line; }
	 */

	/* Update the HTML Editor view! */
	private void doReconcile() {
		editorOldNodes = markdownEditor.nodes;
		/* Create the category base node for the outline! */
		markdownEditor.createNodes();

		Parser parser = Parser.builder().build();
		IDocumentProvider dp = markdownEditor.getDocumentProvider();
		IDocument doc = dp.getDocument(markdownEditor.getEditorInput());
		String source = doc.get();
		String selSource = "";
		int offset = 0;
		int length = 0;
		int lines = doc.getNumberOfLines();
		for (int i = 0; i < lines; i++) {

			try {
				offset = doc.getLineOffset(i);

				length = doc.getLineLength(i);

				selSource = doc.get(offset, length);
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (selSource.contains("```{r") || selSource.contains("```{R")) {
				methods.push(new MarkdownEditorOutlineNode("{R Chunk}", i + 1, "RMarkdown", markdownEditor.baseNode));
			}
		}
		/*
		 * org.commonmark.node.Node document = parser.parse(source);
		 * HtmlRenderer renderer = HtmlRenderer.builder().build();
		 * renderer.render(document); //
		 */
		
		/*Yamlheader yamlVisitor = new Yamlheader(methods, markdownEditor);
		org.commonmark.node.Node nodey = PARSER.parse(source);
		
		nodey.accept(yamlVisitor);*/

		/*org.commonmark.node.Node node = parser.parse(source);
		CustomVisitor visitor = new CustomVisitor(methods, markdownEditor);
		node.accept(visitor);*/

		//
		/* Update the outline! */

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {

				// editor.updateFoldingStructure(fPositions);

				markdownEditor.outlineInputChanged(editorOldNodes, markdownEditor.nodes);
			}

		});

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
		// System.out.println("parsed!");
		/*
		 * Parse parse=editor.getParser(); parse.parse();
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
		doReconcile();
		/*
		 * Parse parse=editor.getParser(); parse.parse();
		 */

	}

	public void setEditor(MarkdownEditor markdownEditor) {
		this.markdownEditor = markdownEditor;
	}

	/**
	 * next character position - used locally and only valid while
	 * {@link #calculatePositions()} is in progress.
	 */

}
