package com.eco.bio7.markdownedit.editors;

import org.eclipse.jface.text.rules.*;

public class MarkdownPartitionScanner extends RuleBasedPartitionScanner {
	public final static String MARKDOWN_COMMENT = "__xml_comment";
	public final static String MARKDOWN_TAG = "__xml_tag";

	public MarkdownPartitionScanner() {

		IToken xmlComment = new Token(MARKDOWN_COMMENT);
		IToken tag = new Token(MARKDOWN_TAG);

		IPredicateRule[] rules = new IPredicateRule[2];

		rules[0] = new MultiLineRule("```", "```", xmlComment);
		rules[1] = new TagRule(tag);

		setPredicateRules(rules);
	}
}
