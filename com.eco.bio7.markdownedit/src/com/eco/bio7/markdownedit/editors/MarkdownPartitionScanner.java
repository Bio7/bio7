package com.eco.bio7.markdownedit.editors;

import org.eclipse.jface.text.rules.*;

public class MarkdownPartitionScanner extends RuleBasedPartitionScanner {
	public final static String MARKDOWN_R_CHUNK = "__xml_comment";
	public final static String MARKDOWN_TAG = "__xml_tag";
	//public final static String YAML_HEADER = "__yaml_header";

	public MarkdownPartitionScanner() {

		IToken rChunk = new Token(MARKDOWN_R_CHUNK);
		IToken tag = new Token(MARKDOWN_TAG);
		//IToken yamlHeader = new Token(YAML_HEADER);

		IPredicateRule[] rules = new IPredicateRule[1];

		rules[0] = new MultiLineRule("```", "```", rChunk);
		//rules[1] = new TagRule(tag);
		//rules[2] = new MultiLineRule("---"+System.lineSeparator(), "---"+System.lineSeparator(), yamlHeader);

		setPredicateRules(rules);
	}
}
