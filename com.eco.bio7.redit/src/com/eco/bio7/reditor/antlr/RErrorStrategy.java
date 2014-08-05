package com.eco.bio7.reditor.antlr;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.misc.NotNull;

public class RErrorStrategy extends DefaultErrorStrategy {

	@Override
	public void reportNoViableAlternative(Parser parser, NoViableAltException e) throws RecognitionException {
		// ANTLR generates Parser subclasses from grammars and
		// Parser extends Recognizer. Parameter parser is a
		// pointer to the parser that detected the error
		String msg = "can't choose between alternatives"; // nonstandard msg
		parser.notifyErrorListeners(e.getOffendingToken(), msg, e);
	}

}