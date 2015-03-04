package com.eco.bio7.reditors;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.reditor.Bio7REditorPlugin;

public class RPartitionStringCodeScanner extends BufferedRuleBasedScanner {
	public RPartitionStringCodeScanner(RColorProvider rColorProvider) {
		// IToken commentToken = new Token(new TextAttribute(...));
		IPreferenceStore store = Bio7REditorPlugin.getDefault().getPreferenceStore();

		RGB rgbkey2 = PreferenceConverter.getColor(store, "colourkey2");
		FontData f2 = PreferenceConverter.getFontData(store, "colourkeyfont2");
		RGB rgbkey3 = PreferenceConverter.getColor(store, "colourkey3");
		FontData f3 = PreferenceConverter.getFontData(store, "colourkeyfont3");

		IToken comment = new Token(new TextAttribute(rColorProvider.getColor(rgbkey3), null, 1, new Font(Display.getCurrent(), f3)));
		IToken string = new Token(new TextAttribute(rColorProvider.getColor(rgbkey2), null, 1, new Font(Display.getCurrent(), f2)));
		IRule[] rules = new IRule[] { new MultiLineRule("\"", "\"", string), new MultiLineRule("'", "'", string) };
		setRules(rules);
	}

	
}