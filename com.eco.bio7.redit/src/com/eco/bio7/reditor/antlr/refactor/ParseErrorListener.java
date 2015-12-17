package com.eco.bio7.reditor.antlr.refactor;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;


public class ParseErrorListener extends BaseErrorListener {

	

	public ParseErrorListener() {
		
	}

	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
	   
	   System.out.println("error occured");
	
	}
	
	

	

}