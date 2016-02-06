package com.eco.bio7.reditor.antlr;

import java.util.ArrayList;

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
	String quickFix = null;
	private ArrayList <ErrorWarnStore>errWarn=new ArrayList<ErrorWarnStore>();
	

	public ArrayList<ErrorWarnStore> getErrWarn() {
		return errWarn;
	}

	public UnderlineListener(REditor editor) {
		this.editor = editor;
		
	}

	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
			String msg, RecognitionException e) {

		
		
			// System.err.println("line "+line+":"+charPositionInLine+" "+msg);
			//underlineError(recognizer, (Token) offendingSymbol, line, charPositionInLine);
		
		// msg=msg.replace("'", "");
       errWarn.add(new ErrorWarnStore(offendingSymbol,line,charPositionInLine,msg));
	}
}