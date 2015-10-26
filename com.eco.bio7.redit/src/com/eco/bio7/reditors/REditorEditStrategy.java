package com.eco.bio7.reditors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;

public class REditorEditStrategy implements IAutoEditStrategy {

	public void customizeDocumentCommand(IDocument document, DocumentCommand command)

	{
		String indent;

		switch (command.text) {

		case "{":
			indent = getOffsetAndIdent(document, command);
			command.text = "{" + "\r\n" + indent + "}";
			configureCommand(command);
			break;
		case "\"":
			indent = getOffsetAndIdent(document, command);
			command.text = "\"\"";
			configureCommand(command);
			break;
		case "'":
			indent = getOffsetAndIdent(document, command);
			command.text = "''";
			configureCommand(command);
			break;
		case "[":
			indent = getOffsetAndIdent(document, command);
			command.text = "[]";
			configureCommand(command);
			break;
		case "(":
			indent = getOffsetAndIdent(document, command);
			command.text = "()";
			configureCommand(command);
			break;

		default:
			break;
		}

	}

	private String getOffsetAndIdent(IDocument document, DocumentCommand command) {
		int line = 0;
		try {
			line = document.getLineOfOffset(command.offset);
		} catch (BadLocationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String indent = null;
		try {
			indent = getIndentOfLine(document, line);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return indent;
	}

	public static int findEndOfWhiteSpace(IDocument document, int offset, int end) throws BadLocationException {
		while (offset < end) {
			char c = document.getChar(offset);
			if (c != ' ' & c != '\t') {
				return offset;
			}
			offset++;
		}
		return end;
	}

	public static String getIndentOfLine(IDocument document, int line) throws BadLocationException {
		if (line > -1) {
			int start = document.getLineOffset(line);
			int end = start + document.getLineLength(line) - 1;
			int whiteend = findEndOfWhiteSpace(document, start, end);
			return document.get(start, whiteend - start);
		} else {
			return ";";
		}
	}

	private void configureCommand(DocumentCommand command) {
		// puts the caret between both the quotes

		command.caretOffset = command.offset + 1;
		command.shiftsCaret = false;
	}

}