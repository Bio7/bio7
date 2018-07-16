package com.eco.bio7.markdownedit.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;

public class MarkdownDocumentProvider extends FileDocumentProvider {

	protected IDocument createDocument(Object element) throws CoreException {
		IDocument document = super.createDocument(element);
		if (document != null) {
			IDocumentPartitioner partitioner =
				new FastPartitioner(
					new MarkdownPartitionScanner(),
					new String[] {
						MarkdownPartitionScanner.MARKDOWN_TAG,
						MarkdownPartitionScanner.MARKDOWN_R_CHUNK,
						MarkdownPartitionScanner.MARKDOWN_OTHER_CHUNK
						});
			partitioner.connect(document);
			document.setDocumentPartitioner(partitioner);
			
			
			
			
			
		}
		return document;
	}
}