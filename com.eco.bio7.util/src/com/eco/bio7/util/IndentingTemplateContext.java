/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.util;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateVariable;

/**
 * A template context which inserts a template with the indentation of the
 * current line (all lines after the first) and the line delimiter of the
 * document; line breaks at the end of the template are removed. Needed for
 * multi-line templates in indented code (e.g., in a Python function).
 */
public class IndentingTemplateContext extends DocumentTemplateContext {

	public IndentingTemplateContext(TemplateContextType type, IDocument document, int offset, int length) {
		super(type, document, offset, length);
	}

	@Override
	public TemplateBuffer evaluate(Template template) throws BadLocationException, TemplateException {
		TemplateBuffer buffer = super.evaluate(template);
		if (buffer == null) {
			return null;
		}
		IDocument doc = getDocument();
		IRegion line = doc.getLineInformationOfOffset(getStart());
		String lineText = doc.get(line.getOffset(), line.getLength());
		int ws = 0;
		while (ws < lineText.length() && (lineText.charAt(ws) == ' ' || lineText.charAt(ws) == '\t')) {
			ws++;
		}
		String indent = lineText.substring(0, ws);
		String delimiter = TextUtilities.getDefaultLineDelimiter(doc);
		String s = buffer.getString();
		/* Without line breaks at the end (an indented last line, e.g., of the cursor, stays) */
		int end = s.length();
		while (end > 0 && (s.charAt(end - 1) == '\n' || s.charAt(end - 1) == '\r')) {
			end--;
		}
		/* The new text and the new offsets of the old positions */
		StringBuilder out = new StringBuilder();
		int[] map = new int[s.length() + 1];
		int i = 0;
		while (i < s.length()) {
			map[i] = out.length();
			char c = s.charAt(i);
			if (i >= end) {
				i++;
				continue;
			}
			if (c == '\r' || c == '\n') {
				if (c == '\r' && i + 1 < s.length() && s.charAt(i + 1) == '\n') {
					map[i + 1] = out.length();
					i++;
				}
				out.append(delimiter);
				/* Blank lines of the template stay empty */
				int next = i + 1;
				if (next < end && s.charAt(next) != '\n' && s.charAt(next) != '\r' || next >= end && end == s.length()) {
					out.append(indent);
				}
			} else {
				out.append(c);
			}
			i++;
		}
		map[s.length()] = out.length();
		TemplateVariable[] variables = buffer.getVariables();
		for (TemplateVariable v : variables) {
			int[] offsets = v.getOffsets();
			int[] moved = new int[offsets.length];
			for (int k = 0; k < offsets.length; k++) {
				moved[k] = map[Math.min(offsets[k], s.length())];
			}
			v.setOffsets(moved);
		}
		buffer.setContent(out.toString(), variables);
		return buffer;
	}
}
