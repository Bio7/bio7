/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.reditors;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.FastPartitioner;

import com.eco.bio7.reditor.Bio7REditorPlugin;

/**
 * 
 */
public class RDocumentSetupParticipant implements IDocumentSetupParticipant {

	/**
	 */
	public RDocumentSetupParticipant() {
	}

	public void setup(IDocument document) {
		if (document instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3 = (IDocumentExtension3) document;
			IDocumentPartitioner partitioner = new FastPartitioner(Bio7REditorPlugin.getDefault().getRPartitionScanner(),null);
			extension3.setDocumentPartitioner(Bio7REditorPlugin.R_PARTITIONING, partitioner);
			partitioner.connect(document);
		}
	}
}
