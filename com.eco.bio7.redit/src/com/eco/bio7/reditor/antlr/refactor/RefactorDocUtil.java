/*
	 * The following methods from class: XmlDocumentFormatter in package:
	 * org.eclipse.ant.internal.ui.editor.formatter;
	 * *************************************************************************
	 * ****** Copyright (c) 2004, 2006s John-Mason P. Shackelford and others.
	 * All rights reserved. This program and the accompanying materials are made
	 * available under the terms of the Eclipse Public License v1.0 which
	 * accompanies this distribution, and is available at
	 * http://www.eclipse.org/legal/epl-v10.html
	 * 
	 * Contributors: John-Mason P. Shackelford - initial API and implementation
	 * IBM Corporation - bug fixes
	 *******************************************************************************/
package com.eco.bio7.reditor.antlr.refactor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

public class RefactorDocUtil {
	

	public static StringBuffer getLeadingWhitespace(int offset, IDocument document) {
		StringBuffer indent = new StringBuffer();
		try {
			org.eclipse.jface.text.IRegion line = document.getLineInformationOfOffset(offset);
			int lineOffset = line.getOffset();
			int nonWS = findEndOfWhiteSpace(document, lineOffset, lineOffset + line.getLength());
			indent.append(document.get(lineOffset, nonWS - lineOffset));
			return indent;
		} catch (BadLocationException e) {
			return indent;
		}
	}

	public static int getLeadingWhitespaceNumber(int offset, IDocument document) {
		int indent = 0;
		try {
			org.eclipse.jface.text.IRegion line = document.getLineInformationOfOffset(offset);
			int lineOffset = line.getOffset();
			int nonWS = findEndOfWhiteSpace(document, lineOffset, lineOffset + line.getLength());
			indent = nonWS - lineOffset;
			return indent;
		} catch (BadLocationException e) {
			return indent;
		}
	}

	public static int findEndOfWhiteSpace(IDocument document, int offset, int end) throws BadLocationException {
		while (offset < end) {
			char c = document.getChar(offset);
			if (c != ' ' && c != '\t') {
				return offset;
			}
			offset++;
		}
		return end;
	}


}
