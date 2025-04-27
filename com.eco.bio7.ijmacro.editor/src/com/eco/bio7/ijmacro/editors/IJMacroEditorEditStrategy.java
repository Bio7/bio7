package com.eco.bio7.ijmacro.editors;


import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;

public class IJMacroEditorEditStrategy implements IAutoEditStrategy {

	public void customizeDocumentCommand(IDocument document, DocumentCommand command)

	{
		String indent;
		IPreferenceStore store = IJMacroEditorPlugin.getDefault().getPreferenceStore();
		if (store.getBoolean("IJMACRO_EDITOR_EDIT_INDENT")) {
			if (command.length == 0 && command.text != null && endsWithDelimiter(document, command.text)) {
				smartIndentAfterNewLine(document, command);
			} else if ("}".equals(command.text)) { //$NON-NLS-1$
				smartInsertAfterBracket(document, command);
			}
		}
		switch (command.text) {

		case "{":
			if (store.getBoolean("CLOSE_BRACES")) {
				indent = getOffsetAndIdent(document, command);
				if (store.getBoolean("CLOSE_BRACES_LINEBREAK")) {
					command.text = "{" + "\r\n" + indent + "}";
				} else {
					command.text = "{" + "}";
				}
				configureCommand(command);
			}
			break;
		case "\"":
			if (store.getBoolean("CLOSE_DOUBLE_QUOTE")) {
				indent = getOffsetAndIdent(document, command);
				command.text = "\"\"";
				configureCommand(command);
			}
			break;
		case "'":
			if (store.getBoolean("CLOSE_SINGLEQUOTE")) {
				indent = getOffsetAndIdent(document, command);
				command.text = "''";
				configureCommand(command);
			}
			break;
		case "[":
			if (store.getBoolean("CLOSE_BRACKETS")) {
				indent = getOffsetAndIdent(document, command);
				command.text = "[]";
				configureCommand(command);
			}
			break;
		case "(":
			if (store.getBoolean("CLOSE_PARENTHESES")) {
				indent = getOffsetAndIdent(document, command);
				command.text = "()";
				configureCommand(command);
			}
			break;

		default:
			break;
		}

	}

	/**
	 * Returns whether or not the given text ends with one of the documents legal line delimiters.
	 * 
	 * @param d
	 *            the document
	 * @param txt
	 *            the text
	 * @return <code>true</code> if <code>txt</code> ends with one of the document's line delimiters, <code>false</code> otherwise
	 */
	private boolean endsWithDelimiter(IDocument d, String txt) {
		String[] delimiters = d.getLegalLineDelimiters();
		if (delimiters != null)
			return TextUtilities.endsWith(delimiters, txt) > -1;
		return false;
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
			/* Avoid a bad location exception! */
			if ((whiteend - start) >= 0) {
				return document.get(start, whiteend - start);
			} else {
				return document.get(start, 0);
			}
		} else {
			return ";";
		}
	}

	/**
	 * Set the indent of a new line based on the command provided in the supplied document.
	 * 
	 * @param document
	 *            - the document being parsed
	 * @param command
	 *            - the command being performed
	 */
	protected void smartIndentAfterNewLine(IDocument document, DocumentCommand command) {

		int docLength = document.getLength();
		if (command.offset == -1 || docLength == 0)
			return;

		try {
			int p = (command.offset == docLength ? command.offset - 1 : command.offset);
			int line = document.getLineOfOffset(p);

			StringBuffer buf = new StringBuffer(command.text);
			if (command.offset < docLength && document.getChar(command.offset) == '}') {
				int indLine = findMatchingOpenBracket(document, line, command.offset, 0);
				if (indLine == -1) {
					indLine = line;
				}
				buf.append(getIndentOfLine(document, indLine));
			} else {
				int start = document.getLineOffset(line);
				int whiteend = findEndOfWhiteSpace(document, start, command.offset);
				buf.append(document.get(start, whiteend - start));
				if (getBracketCount(document, start, command.offset, true) > 0) {
					buf.append('\t');
				}
			}
			command.text = buf.toString();

		} catch (BadLocationException excp) {
			// System.out.println(ScriptEditorMessages.getString("AutoIndent.error.bad_location_1")); //$NON-NLS-1$
		}
	}

	/**
	 * Returns the line number of the next bracket after end.
	 * 
	 * @param document
	 *            - the document being parsed
	 * @param line
	 *            - the line to start searching back from
	 * @param end
	 *            - the end position to search back from
	 * @param closingBracketIncrease
	 *            - the number of brackets to skip
	 * @return the line number of the next matching bracket after end
	 * @throws BadLocationException
	 *             in case the line numbers are invalid in the document
	 */
	protected int findMatchingOpenBracket(IDocument document, int line, int end, int closingBracketIncrease) throws BadLocationException {

		int start = document.getLineOffset(line);
		int brackcount = getBracketCount(document, start, end, false) - closingBracketIncrease;

		// sum up the brackets counts of each line (closing brackets count negative,
		// opening positive) until we find a line the brings the count to zero
		while (brackcount < 0) {
			line--;
			if (line < 0) {
				return -1;
			}
			start = document.getLineOffset(line);
			end = start + document.getLineLength(line) - 1;
			brackcount += getBracketCount(document, start, end, false);
		}
		return line;
	}

	/**
	 * Returns the end position of a comment starting at the given <code>position</code>.
	 * 
	 * @param document
	 *            - the document being parsed
	 * @param position
	 *            - the start position for the search
	 * @param end
	 *            - the end position for the search
	 * @return the end position of a comment starting at the given <code>position</code>
	 * @throws BadLocationException
	 *             in case <code>position</code> and <code>end</code> are invalid in the document
	 */
	/*
	 * private int getCommentEnd(IDocument document, int position, int end) throws BadLocationException { int currentPosition = position; while (currentPosition < end) { char curr=
	 * document.getChar(currentPosition); currentPosition++; if (curr == '*') { if (currentPosition < end && document.getChar(currentPosition) == '/') { return currentPosition + 1; } } } return end; }
	 */
	/**
	 * Returns the bracket value of a section of text. Closing brackets have a value of -1 and open brackets have a value of 1.
	 * 
	 * @param document
	 *            - the document being parsed
	 * @param start
	 *            - the start position for the search
	 * @param end
	 *            - the end position for the search
	 * @param ignoreCloseBrackets
	 *            - whether or not to ignore closing brackets in the count
	 * @return the bracket value of a section of text
	 * @throws BadLocationException
	 *             in case the positions are invalid in the document
	 */
	private int getBracketCount(IDocument document, int start, int end, boolean ignoreCloseBrackets) throws BadLocationException {

		int begin = start;
		int bracketcount = 0;
		while (begin < end) {
			char curr = document.getChar(begin);
			begin++;
			switch (curr) {
			/*
			 * case '/' : if (begin < end) { char next= document.getChar(begin); if (next == '*') { // a comment starts, advance to the comment end begin= getCommentEnd(document, begin + 1, end); }
			 * else if (next == '/') { // '//'-comment: nothing to do anymore on this line begin= end; } } break; case '*' : if (begin < end) { char next= document.getChar(begin); if (next == '/') {
			 * // we have been in a comment: forget what we read before bracketcount= 0; begin++; } } break;
			 */
			case '{':
				bracketcount++;
				ignoreCloseBrackets = false;
				break;
			case '}':
				if (!ignoreCloseBrackets) {
					bracketcount--;
				}
				break;
			case '"':
			case '\'':
				begin = getStringEnd(document, begin, end, curr);
				break;
			default:
			}
		}
		return bracketcount;
	}

	/**
	 * Returns the position of the <code>character</code> in the <code>document</code> after <code>position</code>.
	 * 
	 * @param document
	 *            - the document being parsed
	 * @param position
	 *            - the position to start searching from
	 * @param end
	 *            - the end of the document
	 * @param character
	 *            - the character you are trying to match
	 * @return the next location of <code>character</code>
	 * @throws BadLocationException
	 *             in case <code>position</code> is invalid in the document
	 */
	private int getStringEnd(IDocument document, int position, int end, char character) throws BadLocationException {
		int currentPosition = position;
		while (currentPosition < end) {
			char currentCharacter = document.getChar(currentPosition);
			currentPosition++;
			if (currentCharacter == '\\') {
				// ignore escaped characters
				currentPosition++;
			} else if (currentCharacter == character) {
				return currentPosition;
			}
		}
		return end;
	}

	private void configureCommand(DocumentCommand command) {
		// puts the caret between both the quotes

		command.caretOffset = command.offset + 1;
		command.shiftsCaret = false;
	}

	/**
	 * Set the indent of a bracket based on the command provided in the supplied document.
	 * 
	 * @param document
	 *            - the document being parsed
	 * @param command
	 *            - the command being performed
	 */
	protected void smartInsertAfterBracket(IDocument document, DocumentCommand command) {
		if (command.offset == -1 || document.getLength() == 0)
			return;

		try {
			int p = (command.offset == document.getLength() ? command.offset - 1 : command.offset);
			int line = document.getLineOfOffset(p);
			int start = document.getLineOffset(line);
			int whiteend = findEndOfWhiteSpace(document, start, command.offset);

			// shift only when line does not contain any text up to the closing bracket
			if (whiteend == command.offset) {
				// evaluate the line with the opening bracket that matches out closing bracket
				int indLine = findMatchingOpenBracket(document, line, command.offset, 1);
				if (indLine != -1 && indLine != line) {
					// take the indent of the found line
					StringBuffer replaceText = new StringBuffer(getIndentOfLine(document, indLine));
					// add the rest of the current line including the just added close bracket
					replaceText.append(document.get(whiteend, command.offset - whiteend));
					replaceText.append(command.text);
					// modify document command
					command.length = command.offset - start;
					command.offset = start;
					command.text = replaceText.toString();
				}
			}
		} catch (BadLocationException excp) {
			// System.out.println(ScriptEditorMessages.getString("AutoIndent.error.bad_location_2")); //$NON-NLS-1$
		}
	}

}