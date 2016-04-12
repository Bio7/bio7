package com.eco.bio7.reditor.antlr.refactor;

import java.util.HashSet;

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
	private String[] id;
	
	public String[] getId() {
		return id;
	}

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
	public boolean parseSource(String fullText,boolean captureId) {
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
		ExtractInterfaceListener extractor = new ExtractInterfaceListener(tokens, parser,captureId);
		walker.walk(extractor, tree);
        /*Using HashSet to avoid duplicates!*/
        HashSet<String> hashSet=extractor.capId;
		id=hashSet.toArray(new String[hashSet.size()]);
		
		if (parser.getNumberOfSyntaxErrors() == 0) {
			errors = false;
		} else {
			errors = true;
		}
		return errors;
	}
	
	

}
