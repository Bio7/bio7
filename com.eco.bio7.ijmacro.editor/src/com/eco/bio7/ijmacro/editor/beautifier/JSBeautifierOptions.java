/*******************************************************************************
 * Copyright (c)  Sebastien Moran
 * https://github.com/sebz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
/*Code from: https://github.com/sebz/eclipse-javascript-formatter*/
package com.eco.bio7.ijmacro.editor.beautifier;

public class JSBeautifierOptions {

	// brace style
	public static final int BS_COLLAPSE = 0;
	public static final int BS_EXPAND = 1;
	public static final int BS_EXPAND_STRICT = 2;
	public static final int BS_END_EXPAND = 3;

	public int brace_style = BS_COLLAPSE;
	public int indent_size = 4;
	public String indent_char = " ";
	public boolean preserve_newlines = true;
	public boolean break_chained_methods = false;
	public int max_preserve_newlines = 0;
	public boolean jslint_happy = false;
	public boolean keep_array_indentation = false;
	public boolean space_before_conditional = true;
	public boolean unescape_strings = false;
	
   /*Added for the ImageJ macro preferences!*/
	public void setValues(String options) {
		String[] opts = options.split(",");
		brace_style = Integer.parseInt(opts[0]);
		indent_size = Integer.parseInt(opts[1]);
		indent_char = opts[2];
		preserve_newlines = Boolean.parseBoolean(opts[3]);
		break_chained_methods = Boolean.parseBoolean(opts[4]);
		max_preserve_newlines = Integer.parseInt(opts[5]);
		jslint_happy = Boolean.parseBoolean(opts[6]);
		keep_array_indentation = Boolean.parseBoolean(opts[7]);
		space_before_conditional = Boolean.parseBoolean(opts[8]);
		unescape_strings = Boolean.parseBoolean(opts[9]);

	}

}
