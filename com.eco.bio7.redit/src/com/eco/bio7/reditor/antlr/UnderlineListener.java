package com.eco.bio7.reditor.antlr;

import java.util.ArrayList;
import java.util.HashMap;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
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

		// System.err.println(msg);

		if (editor != null) {

			resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
			IDocumentProvider provider = editor.getDocumentProvider();
			IDocument document = provider.getDocument(editor.getEditorInput());
			int lineOffsetStart = 0;

			IMarker[] markers = findMyMarkers(resource);
			int lineNumb = -1;
			for (int i = 0; i < markers.length; i++) {

				try {
					lineNumb = (int) markers[i].getAttribute(IMarker.LINE_NUMBER);

					if (lineNumb == line) {
						markers[i].delete();
						//System.out.println(recognizer.getRuleNames()[i]);
					}
				} catch (CoreException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			//System.out.println("Line Error is:" + line);

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
				// marker.setAttribute(IMarker.MESSAGE, "line " + line + ":" + charPositionInLine + " " + msg);
				marker.setAttribute(IMarker.MESSAGE, msg);
				marker.setAttribute(IMarker.LINE_NUMBER, line);
				marker.setAttribute(IMarker.LOCATION, lineOffsetStart + charPositionInLine);
				/*Correct the underline error if it is*/
				if ((lineOffsetStart + charPositionInLine) + 1 > document.getLength()) {
					marker.setAttribute(IMarker.CHAR_START, (lineOffsetStart + charPositionInLine) - 1);
					marker.setAttribute(IMarker.CHAR_END, (lineOffsetStart + charPositionInLine));
				} else {
					marker.setAttribute(IMarker.CHAR_START, (lineOffsetStart + charPositionInLine));
					marker.setAttribute(IMarker.CHAR_END, (lineOffsetStart + charPositionInLine) + 1);
				}
			} catch (CoreException ex) {

				ex.printStackTrace();
			}

		}

	}

	public IMarker[] findMyMarkers(IResource target) {
		String type = "org.eclipse.core.resources.problemmarker";

		IMarker[] markers = null;
		try {
			markers = target.findMarkers(type, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {

			e.printStackTrace();
		}
		return markers;
	}

}