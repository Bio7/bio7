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

	public UnderlineListener(REditor editor) {
		this.editor = editor;
	}

	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
		
		 /*Token offendingToken=(Token)offendingSymbol;
         CommonTokenStream tokens = (CommonTokenStream) recognizer.getInputStream();
			String input = tokens.getTokenSource().getInputStream().toString();
			String[] lines = input.split("\n");
			String errorLine = lines[line - 1];
			//System.err.println(errorLine);
			
			int start = offendingToken.getStartIndex();
			int stop = offendingToken.getStopIndex();
			
			System.out.println(start+" "+stop);*/

		if (editor != null) {

			resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
			IDocumentProvider provider = editor.getDocumentProvider();
			IDocument document = provider.getDocument(editor.getEditorInput());
			int lineOffsetStart = 0;
			

			try {
				lineOffsetStart = document.getLineOffset(line - 1);

			} catch (BadLocationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			IMarker marker;
			try {
				marker = resource.createMarker(IMarker.PROBLEM);
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
				//marker.setAttribute(IMarker.MESSAGE, "line " + line + ":" + charPositionInLine + " " + msg);
				marker.setAttribute(IMarker.MESSAGE, msg);
				marker.setAttribute(IMarker.LINE_NUMBER, line);
				marker.setAttribute(IMarker.LOCATION, lineOffsetStart + charPositionInLine);

				marker.setAttribute(IMarker.CHAR_START, lineOffsetStart + charPositionInLine);
				marker.setAttribute(IMarker.CHAR_END, (lineOffsetStart + charPositionInLine) + 1);
			} catch (CoreException ex) {

				ex.printStackTrace();
			}
		}

	}
	

	
}