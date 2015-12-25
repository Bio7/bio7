package com.eco.bio7.reditor.antlr;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.IDocumentProvider;
import com.eco.bio7.reditors.REditor;

public class UnderlineListener extends BaseErrorListener {

	public static IResource resource;
	private REditor editor;
	private boolean warn = false;
	private Token offSymbol;
	private int offSymbolTokenLength = 0;

	public UnderlineListener(REditor editor) {
		this.editor = editor;
	}

	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
			String msg, RecognitionException e) {
		String quickFix = null;
		{
			System.err.println("line "+line+":"+charPositionInLine+" "+msg); 
			underlineError(recognizer,(Token)offendingSymbol, line, charPositionInLine);
			}


		if (offendingSymbol != null) {
			offSymbol = (Token) offendingSymbol;
			offSymbolTokenLength = offSymbol.getText().length();
		}
		// System.out.println(offSymbol.getText());
		if (msg.startsWith("Err")) {
			warn = false;
			String[] split = msg.split(":");
			quickFix = split[0];
			msg = split[1];

			// System.out.println(msg);
		} else if (msg.startsWith("Warn")) {
			warn = true;
			String[] split = msg.split(":");
			quickFix = split[0];
			msg = split[1];
		}

		if (editor != null) {

			resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
			IDocumentProvider provider = editor.getDocumentProvider();
			IDocument document = provider.getDocument(editor.getEditorInput());
			int lineOffsetStart = 0;

			// System.out.println("Char is at:" + charPositionInLine);

			try {

				lineOffsetStart = document.getLineOffset(line - 1);

			} catch (BadLocationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			IMarker marker;
			if (warn) {

				try {
					marker = resource.createMarker(IMarker.PROBLEM);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
					// marker.setAttribute(IMarker.MESSAGE, "line " + line + ":"
					// +
					// charPositionInLine + " " + msg);
					marker.setAttribute(IMarker.MESSAGE, msg);
					marker.setAttribute(IMarker.LINE_NUMBER, line);
					marker.setAttribute(IMarker.LOCATION, lineOffsetStart + charPositionInLine);
					if (quickFix != null) {
						marker.setAttribute(IMarker.TEXT, quickFix);
					}

					else {
						marker.setAttribute(IMarker.TEXT, "NA");
					}
					/* Correct the underline error if it is */
					if ((lineOffsetStart + charPositionInLine) + 1 + offSymbolTokenLength > document.getLength()) {
						// marker.setAttribute(IMarker.CHAR_START,
						// (lineOffsetStart + charPositionInLine) - 1);
						// marker.setAttribute(IMarker.CHAR_END,
						// (lineOffsetStart + charPositionInLine));

					} else {
						marker.setAttribute(IMarker.CHAR_START, (lineOffsetStart + charPositionInLine));
						marker.setAttribute(IMarker.CHAR_END,
								(lineOffsetStart + charPositionInLine) + 1 + offSymbolTokenLength);
					}
				} catch (CoreException ex) {

					ex.printStackTrace();
				}
				warn = false;// reset warning flag!
			} else {
				try {
					marker = resource.createMarker(IMarker.PROBLEM);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					// marker.setAttribute(IMarker.MESSAGE, "line " + line + ":"
					// +
					// charPositionInLine + " " + msg);
					marker.setAttribute(IMarker.MESSAGE, msg);
					marker.setAttribute(IMarker.LINE_NUMBER, line);
					marker.setAttribute(IMarker.LOCATION, lineOffsetStart + charPositionInLine);
					if (quickFix != null) {
						marker.setAttribute(IMarker.TEXT, quickFix);
					}

					else {
						marker.setAttribute(IMarker.TEXT, "NA");
					}
					/* Correct the underline error if it is */
					if (offSymbol.getStartIndex()==offSymbol.getStopIndex()) {
						marker.setAttribute(IMarker.CHAR_START, offSymbol.getStartIndex());
						marker.setAttribute(IMarker.CHAR_END, offSymbol.getStopIndex()+1);
						// marker.setAttribute(IMarker.CHAR_START,
						// (lineOffsetStart + charPositionInLine) - 1);
						// marker.setAttribute(IMarker.CHAR_END,
						// (lineOffsetStart + charPositionInLine));

					} else {
						System.out.println(offSymbol.getStartIndex()+" "+offSymbol.getStopIndex());
						marker.setAttribute(IMarker.CHAR_START, offSymbol.getStartIndex()-1);
						marker.setAttribute(IMarker.CHAR_END, offSymbol.getStopIndex()-1);
					}
				} catch (CoreException ex) {

					ex.printStackTrace();
				}
			}
		}

	}

	protected void underlineError(Recognizer recognizer, Token offendingToken, int line, int charPositionInLine) {
		CommonTokenStream tokens = (CommonTokenStream) recognizer.getInputStream();
		String input = tokens.getTokenSource().getInputStream().toString();
		String[] lines = input.split("\n");
		if(line>0&&line<=lines.length-1){
		String errorLine = lines[line - 1];
		System.err.println(errorLine);
		for (int i = 0; i < charPositionInLine; i++)
			System.err.print(" ");
		int start = offendingToken.getStartIndex();
		int stop = offendingToken.getStopIndex();
		if (start >= 0 && stop >= 0) {
			for (int i = start; i <= stop; i++)
				System.err.print("^");
		}
		System.err.println();
		}
	}

}