/*package com.eco.bio7.reditors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.FileDocumentProvider;

import com.eco.bio7.reditor.Bio7REditorPlugin;

public class RDocumentProvider extends FileDocumentProvider {

	protected IDocument createDocument(Object element) throws CoreException {
		IDocument document = super.createDocument(element);
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3 = (IDocumentExtension3) document;
			IDocumentPartitioner partitioner = new FastPartitioner(Bio7REditorPlugin.getDefault().getRPartitionScanner(),RPartitionScanner.R_PARTITION_TYPES);
			extension3.setDocumentPartitioner(Bio7REditorPlugin.R_PARTITIONING, partitioner);
			partitioner.connect(document);
			
			
		}
		return document;
	}
}*/