/*package com.eco.bio7.reditors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.reditor.Bio7REditorPlugin;

public class RPartitionStringCodeScanner extends BufferedRuleBasedScanner {
	public Token string;

	public RPartitionStringCodeScanner(RColorProvider rColorProvider) {
		super(); 
		IPreferenceStore store = Bio7REditorPlugin.getDefault().getPreferenceStore();

		RGB rgbkey2 = PreferenceConverter.getColor(store, "colourkey2");
		FontData f2 = PreferenceConverter.getFontData(store, "colourkeyfont2");
		
		 string = new Token(new TextAttribute(rColorProvider.getColor(rgbkey2), null, 1, new Font(Display.getCurrent(), f2)));
		//System.out.println(string.getData().toString());
		
		
		 List list= new ArrayList(); 
		 
		  // Add rule for tags. 
		 list.add(new MultiLineRule("\"", "\"", string, '\\'));
		list.add(new MultiLineRule("'", "'", string, '\\'));
		 
		  IRule[] result= new IRule[list.size()]; 
		  list.toArray(result); 
		  setRules(result); 
		
		
		
	}

	
}*/