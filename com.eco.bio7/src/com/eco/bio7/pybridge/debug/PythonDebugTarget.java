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
package com.eco.bio7.pybridge.debug;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

/**
 * The script which the debugger starts for the active Python file. A module
 * which only defines classes and functions (e.g., one class per file) does
 * nothing on its own, so the one script of the folder (else of the project)
 * which imports it (also through other modules) is started; else the file
 * itself, with a hint if the session ends without a stop.
 */
final class PythonDebugTarget {

	/** The script to debug, a note for the console and a hint for the end of a session without a stop. */
	record Choice(IFile script, String note, String hint) {
	}

	/*
	 * Statements which only define something (not code which runs): also an
	 * assignment of a value without a call (a constant)
	 */
	private static final Pattern DEFINITION = Pattern.compile(
			"^(def |async def |class |import |from |@|#|\"|'|[)\\]}]|try:|except|else:|finally:|if TYPE_CHECKING|if typing\\.TYPE_CHECKING).*"
					+ "|^[A-Za-z_][\\w.]*(\\s*,\\s*[A-Za-z_][\\w.]*)*\\s*(:[^=]*)?=(?!=)[^(]*$");

	private PythonDebugTarget() {
	}

	static Choice choose(IFile active) {
		if (active == null || active.getLocation() == null || isScript(active)) {
			return new Choice(active, null, null);
		}
		List<IFile> scripts = scriptsImporting(active, active.getParent(), IResource.DEPTH_ONE);
		if (scripts.isEmpty() && active.getProject() != null) {
			scripts = scriptsImporting(active, active.getProject(), IResource.DEPTH_INFINITE);
		}
		if (scripts.size() == 1) {
			IFile script = scripts.get(0);
			return new Choice(script, "Debugging " + script.getName() + " (it imports " + active.getName() + ", which only defines classes and functions)", null);
		}
		String from = scripts.isEmpty() ? "the script which imports it"
				: scripts.stream().map(IFile::getName).collect(Collectors.joining(", ")) + " (the scripts which import it)";
		return new Choice(active, null, active.getName() + " only defines classes and functions, nothing calls them.\nStart the debugger from " + from + ".");
	}

	/* The file runs code at the top level (a call, a loop, if __name__ == '__main__', ...) */
	static boolean isScript(IFile file) {
		List<String> lines;
		try {
			lines = Files.readAllLines(file.getLocation().toFile().toPath(), StandardCharsets.UTF_8);
		} catch (IOException | RuntimeException e) {
			return true;
		}
		boolean inString = false;
		for (String line : lines) {
			boolean startsInString = inString;
			int quotes = count(line, "\"\"\"") + count(line, "'''");
			if (quotes % 2 == 1) {
				inString = !inString;
			}
			if (startsInString || line.isBlank() || Character.isWhitespace(line.charAt(0))) {
				continue;
			}
			if (!DEFINITION.matcher(line).matches()) {
				return true;
			}
		}
		return false;
	}

	/* The scripts which import the module, directly or through other modules of the container */
	private static List<IFile> scriptsImporting(IFile module, IContainer container, int depth) {
		List<IFile> files = pythonFiles(container, depth);
		Set<IFile> scripts = new LinkedHashSet<>();
		Set<IFile> seen = new LinkedHashSet<>();
		Deque<IFile> queue = new ArrayDeque<>();
		queue.add(module);
		seen.add(module);
		while (!queue.isEmpty()) {
			IFile m = queue.poll();
			Pattern imports = importOf(moduleName(m));
			for (IFile f : files) {
				if (seen.contains(f) || !imports.matcher(read(f)).find()) {
					continue;
				}
				seen.add(f);
				if (isScript(f)) {
					scripts.add(f);
				} else {
					queue.add(f);
				}
			}
		}
		return new ArrayList<>(scripts);
	}

	private static Pattern importOf(String module) {
		String m = Pattern.quote(module);
		return Pattern.compile("(?m)^\\s*(from\\s+\\.?" + m + "\\s+import\\b|import\\s+[^#\\n]*\\b" + m + "\\b)");
	}

	private static String moduleName(IFile f) {
		String n = f.getName();
		return n.substring(0, n.length() - (f.getFileExtension() == null ? 0 : f.getFileExtension().length() + 1));
	}

	private static List<IFile> pythonFiles(IContainer container, int depth) {
		List<IFile> out = new ArrayList<>();
		if (container == null) {
			return out;
		}
		try {
			container.accept(r -> {
				if (r.getType() == IResource.FILE && r.getLocation() != null && r.getName().toLowerCase(Locale.ROOT).endsWith(".py")) {
					out.add((IFile) r);
				}
				return true;
			}, depth, false);
		} catch (CoreException e) {
			/* The files found so far */
		}
		return out;
	}

	private static String read(IFile f) {
		try {
			return Files.readString(f.getLocation().toFile().toPath(), StandardCharsets.UTF_8);
		} catch (IOException | RuntimeException e) {
			return "";
		}
	}

	private static int count(String s, String sub) {
		int n = 0;
		for (int i = s.indexOf(sub); i >= 0; i = s.indexOf(sub, i + sub.length())) {
			n++;
		}
		return n;
	}
}
