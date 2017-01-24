package com.eco.bio7.markdownedit.editors;

import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.*;

public class MarkdownScanner extends RuleBasedScanner {

	public MarkdownScanner(ColorManager manager) {
		IToken procInstr =
			new Token(
				new TextAttribute(
					manager.getColor(IMarkdownColorConstants.PROC_INSTR)));

		IRule[] rules = new IRule[2];
		//Add rule for processing instructions
		rules[0] = new SingleLineRule("<?", "?>", procInstr);
		// Add generic whitespace rule.
		rules[1] = new WhitespaceRule(new MarkdownWhitespaceDetector());

		setRules(rules);
	}

	public MarkdownScanner(MarkdownColorProvider rColorProvider) {
		// TODO Auto-generated constructor stub
	}
}
