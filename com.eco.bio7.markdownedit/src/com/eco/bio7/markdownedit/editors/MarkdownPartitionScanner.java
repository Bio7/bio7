package com.eco.bio7.markdownedit.editors;

import org.eclipse.jface.text.rules.*;

public class MarkdownPartitionScanner extends RuleBasedPartitionScanner {
	public final static String MARKDOWN_R_CHUNK = "__r_chunk";
	public final static String MARKDOWN_TAG = "__markdown_";
	public final static String MARKDOWN_OTHER_CHUNK = "__markdown_code_chunk";
	//public final static String YAML_HEADER = "__yaml_header";

	public MarkdownPartitionScanner() {

		IToken rChunk = new Token(MARKDOWN_R_CHUNK);
		IToken otherChunk = new Token(MARKDOWN_OTHER_CHUNK);
		//IToken tag = new Token(MARKDOWN_TAG);
		//IToken yamlHeader = new Token(YAML_HEADER);

		IPredicateRule[] rules = new IPredicateRule[3];

		rules[0] = new MultiLineRule("```{r", "```", rChunk);
		rules[1] = new MultiLineRule("```{R", "```", rChunk);
		rules[2] = new MultiLineRule("```{", "```", otherChunk);
		//rules[2] = new EndOfLineRule("#", rChunk);
		//rules[2] = new MultiLineRule("---"+System.lineSeparator(), "---"+System.lineSeparator(), yamlHeader);

		setPredicateRules(rules);
	}
}
