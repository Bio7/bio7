package com.eco.bio7.markdownedit.editors;

import org.eclipse.jface.text.rules.IWhitespaceDetector;

public class MarkdownWhitespaceDetector implements IWhitespaceDetector {

	public boolean isWhitespace(char c) {
		return (c == ' ' || c == '\t' || c == '\n' || c == '\r');
	}
}
