/*package com.eco.bio7.reditors;

*//*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     
 *     M.Austenfeld - Minor changes for the Bio7 application.
 *******************************************************************************//*

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import com.eco.bio7.reditor.Bio7REditorPlugin;

*//**
 * A R code scanner.
 *//*
public class RCodeStringScanner extends RuleBasedScanner {

	public Token string;

	*//**
	 * Creates a R code scanner with the given color provider.
	 * 
	 * @param provider
	 *            the color provider
	 *//*
	public RCodeStringScanner(RColorProvider provider) {

		IPreferenceStore store = Bio7REditorPlugin.getDefault().getPreferenceStore();

		RGB rgbkey2 = PreferenceConverter.getColor(store, "colourkey2");

		FontData f2 = PreferenceConverter.getFontData(store, "colourkeyfont2");

		string = new Token(new TextAttribute(provider.getColor(rgbkey2), null, 1, new Font(Display.getCurrent(), f2)));

		List<MultiLineRule> rules = new ArrayList<MultiLineRule>();

		rules.add(new MultiLineRule("\"", "\"", string, '\\'));
		rules.add(new MultiLineRule("'", "'", string, '\\'));

		IRule[] result = new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
	}

}
*/