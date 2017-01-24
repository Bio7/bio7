package com.eco.bio7.markdownedit.editors;

import org.eclipse.jface.text.rules.*;

public class MarkdownPartitionScanner extends RuleBasedPartitionScanner {
	public final static String MARKDOWN_COMMENT = "__xml_comment";
	public final static String MARKDOWN_TAG = "__xml_tag";
	public final static String YAML_HEADER = "__yaml_header";

	public MarkdownPartitionScanner() {

		IToken rChunk = new Token(MARKDOWN_COMMENT);
		IToken tag = new Token(MARKDOWN_TAG);
		IToken yamlHeader = new Token(YAML_HEADER);

		IPredicateRule[] rules = new IPredicateRule[3];

		rules[0] = new MultiLineRule("```", "```", rChunk);
		rules[1] = new TagRule(tag);
		rules[2] = new MultiLineRule("---", "---", yamlHeader);

		setPredicateRules(rules);
	}
}
