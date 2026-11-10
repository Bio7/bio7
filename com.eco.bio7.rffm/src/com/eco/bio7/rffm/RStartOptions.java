/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rffm;

import java.nio.file.Path;
import java.util.List;

/**
 * The options to start embedded R.
 *
 * @param home          the home of R (R.home(), e.g.,
 *                      /Library/Frameworks/R.framework/Resources)
 * @param arguments     additional command line arguments of R (e.g.,
 *                      "--vanilla")
 * @param interactive   true: interactive() is TRUE (menus, prompts)
 * @param allowQuit     false: q() and quit() raise an error instead of ending
 *                      the process (the application)
 * @param processEvents true: R processes the events of its own windows when
 *                      idle (x11(), windows(), Tcl/Tk), every 100 ms
 * @param console       the console (output, prompts)
 */
public record RStartOptions(Path home, List<String> arguments, boolean interactive, boolean allowQuit, boolean processEvents, RConsole console) {

	public RStartOptions {
		arguments = arguments == null ? List.of() : List.copyOf(arguments);
		console = console == null ? RConsole.NONE : console;
	}

	/** The default options for a home of R. */
	public static RStartOptions of(Path home) {
		return new RStartOptions(home, List.of(), true, false, !System.getProperty("os.name", "").toLowerCase().contains("mac"), RConsole.NONE);
	}

	public RStartOptions withConsole(RConsole console) {
		return new RStartOptions(home, arguments, interactive, allowQuit, processEvents, console);
	}

	public RStartOptions withArguments(List<String> arguments) {
		return new RStartOptions(home, arguments, interactive, allowQuit, processEvents, console);
	}

	public RStartOptions withInteractive(boolean interactive) {
		return new RStartOptions(home, arguments, interactive, allowQuit, processEvents, console);
	}

	public RStartOptions withAllowQuit(boolean allowQuit) {
		return new RStartOptions(home, arguments, interactive, allowQuit, processEvents, console);
	}

	public RStartOptions withProcessEvents(boolean processEvents) {
		return new RStartOptions(home, arguments, interactive, allowQuit, processEvents, console);
	}
}
