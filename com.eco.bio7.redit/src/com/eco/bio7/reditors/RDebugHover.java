/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.reditors;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The hover of a variable in a debug session of R: the debugger (another plugin,
 * which depends on this one) installs the description of a variable (its class,
 * size and a preview, null if it is not a variable of the shown frame or no
 * session runs) and the action which opens it (a table, an image, a plot).
 */
public final class RDebugHover {

	/** The first line of a description (the name of the variable follows). */
	public static final String MARK = "@@BIO7DEBUG@@";

	private static volatile Function<String, String> describer;
	private static volatile Consumer<String> opener;

	private RDebugHover() {
	}

	/** Installs the description and the open action of the variables (the debugger). */
	public static void install(Function<String, String> describe, Consumer<String> open) {
		describer = describe;
		opener = open;
	}

	/**
	 * The description of a variable of the debug session (MARK + name, then the
	 * text) or null. Called in the thread of the hover (it may ask R).
	 */
	public static String describe(String name) {
		Function<String, String> d = describer;
		if (d == null || name == null || name.isBlank()) {
			return null;
		}
		String text = d.apply(name);
		return text == null ? null : MARK + name + "\n" + text;
	}

	/** Opens the variable (UI thread). */
	public static void open(String name) {
		Consumer<String> o = opener;
		if (o != null) {
			o.accept(name);
		}
	}
}
