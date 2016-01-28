package com.eco.bio7.reditor.antlr.refactor;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import com.eco.bio7.reditor.antlr.RFilter;
import com.eco.bio7.reditor.antlr.RLexer;
import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditor.antlr.RParser.ProgContext;

public class RefactorParse {
	
	private CommonTokenStream tokens;
	private BufferedTokenStream bufferTokenStream;
	private RParser parser;
	private ProgContext tree;
	
	public CommonTokenStream getTokens() {
		return tokens;
	}

	public RParser getParser() {
		return parser;
	}

	public ProgContext getTree() {
		return tree;
	}


	/* Here we parse the text and test for possible errors! */
	public boolean parseSource(String fullText) {
		boolean errors;
		ANTLRInputStream input = new ANTLRInputStream(fullText);
		RLexer lexer = new RLexer(input);
		tokens = new CommonTokenStream(lexer);
		bufferTokenStream = new BufferedTokenStream(lexer);
		RFilter filter = new RFilter(tokens);
		filter.removeErrorListeners();
		filter.stream(); // call start rule: stream
		tokens.reset();
		ParseErrorListener parseErrorListener = new ParseErrorListener();
		parser = new RParser(tokens);

		lexer.removeErrorListeners();
		// lexer.addErrorListener(li);
		parser.removeErrorListeners();

		parser.addErrorListener(parseErrorListener);

		tree = parser.prog();
		ParseTreeWalker walker = new ParseTreeWalker(); // create standard
														// walker
		ExtractInterfaceListener extractor = new ExtractInterfaceListener(tokens, parser);
		walker.walk(extractor, tree);

		if (parser.getNumberOfSyntaxErrors() == 0) {
			errors = false;
		} else {
			errors = true;
		}
		return errors;
	}
	
	

}
