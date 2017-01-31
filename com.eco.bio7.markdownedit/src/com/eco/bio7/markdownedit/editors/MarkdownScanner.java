package com.eco.bio7.markdownedit.editors;

import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.graphics.RGB;

import com.eco.bio7.markdownedit.Activator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.*;

public class MarkdownScanner extends RuleBasedScanner {

	public Token other;

	public MarkdownScanner(ColorManager manager) {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		RGB rgbkey = PreferenceConverter.getColor(store, "colourkey");
		//IToken string = new Token(new TextAttribute(manager.getColor(IMarkdownColorConstants.STRING)));

		List<IRule> rules = new ArrayList<IRule>();

		other = new Token(new TextAttribute(manager.getColor(rgbkey), null, isBold("BOLD_COLOURKEY")));

		/*IToken procInstr = new Token(new TextAttribute(manager.getColor(IMarkdownColorConstants.PROC_INSTR)));

		// Add rule for processing instructions
		rules.add(new SingleLineRule("<?", "?>", procInstr));*/
		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new MarkdownWhitespaceDetector()));

		MarkdownWordDetector wd = new MarkdownWordDetector();
		WordRule wordRule = new WordRule(wd, other);
		rules.add(wordRule);

		IRule[] result = new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
	}

	private int isBold(String string2) {
		int style = 0;
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		if (store.getBoolean(string2)) {
			style = 1;
		}

		return style;
	}

	public MarkdownScanner(MarkdownColorProvider rColorProvider) {
		// TODO Auto-generated constructor stub
	}
}
