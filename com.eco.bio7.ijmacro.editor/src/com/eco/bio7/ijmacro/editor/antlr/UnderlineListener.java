package com.eco.bio7.ijmacro.editor.antlr;

import java.util.ArrayList;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.eclipse.core.resources.IResource;


public class UnderlineListener extends BaseErrorListener {

	public static IResource resource;
	
	private ArrayList<ErrorWarnStore> errWarn = new ArrayList<ErrorWarnStore>();

	private int countErrors=0;
    /*Return the counted errors!*/
	public int getNumberOfListenSyntaxErrors() {
		return countErrors;
	}

	public ArrayList<ErrorWarnStore> getErrWarn() {
		return errWarn;
	}

	public UnderlineListener() {
		//this.editor = editor;

	}

	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
			String msg, RecognitionException e) {

		// System.err.println("line "+line+":"+charPositionInLine+" "+msg);
		// underlineError(recognizer, (Token) offendingSymbol, line,
		// charPositionInLine);
		/*Here we count the default ANTLR errors and custom added (e.g. Quickfix) errors!*/
		if (msg.startsWith("Warn")==false) {
			countErrors++;
		}
		// msg=msg.replace("'", "");
		errWarn.add(new ErrorWarnStore(offendingSymbol, line, charPositionInLine, msg));
	}
}