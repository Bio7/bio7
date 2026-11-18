/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.rcp;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Several workspaces (as in Eclipse): File > Switch Workspace lists the recent
 * workspaces and "Other..." (a folder dialog); Bio7 restarts in the chosen
 * workspace. The default workspace is ~/Bio7/workspace (Bio7.product:
 * osgi.instance.area.default). The recent workspaces are stored in
 * ~/Bio7/recent_workspaces.txt (outside of the workspaces and the application,
 * so they survive an update of Bio7).
 */
public final class WorkspaceSwitcher {

	private static final int MAX_RECENT = 10;

	private WorkspaceSwitcher() {
	}

	private static Path store() {
		return Path.of(System.getProperty("user.home"), "Bio7", "recent_workspaces.txt");
	}

	/** The current workspace, null if unknown. */
	public static File current() {
		if (Platform.getInstanceLocation() == null || Platform.getInstanceLocation().getURL() == null) {
			return null;
		}
		/* With URIUtil: correct with spaces and on Windows (URL.getFile(): /C:/..., %20) */
		try {
			return new File(org.eclipse.core.runtime.URIUtil.toURI(Platform.getInstanceLocation().getURL())).getAbsoluteFile();
		} catch (Exception e) {
			return new File(Platform.getInstanceLocation().getURL().getFile()).getAbsoluteFile();
		}
	}

	/** The recent workspaces, the last used first (folders which no longer exist are left out). */
	public static List<File> recent() {
		List<File> out = new ArrayList<>();
		try {
			if (Files.isRegularFile(store())) {
				for (String line : Files.readAllLines(store(), StandardCharsets.UTF_8)) {
					File f = new File(line.trim());
					if (!line.isBlank() && f.isDirectory() && !contains(out, f)) {
						out.add(f);
					}
				}
			}
		} catch (IOException e) {
			/* no recent workspaces */
		}
		return out;
	}

	/** Remembers a workspace as the last used one (at the startup: the current workspace). */
	public static void remember(File workspace) {
		if (workspace == null) {
			return;
		}
		List<File> list = recent();
		list.removeIf(f -> same(f, workspace));
		list.add(0, workspace.getAbsoluteFile());
		while (list.size() > MAX_RECENT) {
			list.remove(list.size() - 1);
		}
		try {
			Files.createDirectories(store().getParent());
			List<String> lines = new ArrayList<>();
			for (File f : list) {
				lines.add(f.getPath());
			}
			Files.write(store(), lines, StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.err.println("[Bio7] The recent workspaces could not be saved: " + e.getMessage());
		}
	}

	/** The submenu File > Switch Workspace (filled when it is shown). */
	public static MenuManager createMenu(IWorkbenchWindow window) {
		MenuManager menu = new MenuManager("Switch &Workspace", "com.eco.bio7.switchWorkspace");
		menu.setRemoveAllWhenShown(true);
		menu.addMenuListener(m -> fill(m, window));
		/* An item, so that the submenu is shown (replaced when it opens) */
		menu.add(otherAction(window));
		return menu;
	}

	private static void fill(IMenuManager menu, IWorkbenchWindow window) {
		File current = current();
		int n = 0;
		for (File f : recent()) {
			if (same(f, current)) {
				continue;
			}
			Action a = new Action(label(f)) {
				@Override
				public void run() {
					switchTo(window, f);
				}
			};
			a.setToolTipText(f.getPath());
			menu.add(a);
			n++;
		}
		if (n > 0) {
			menu.add(new Separator());
		}
		menu.add(otherAction(window));
	}

	private static Action otherAction(IWorkbenchWindow window) {
		return new Action("&Other...") {
			@Override
			public void run() {
				File f = choose(window.getShell(), current());
				if (f != null) {
					switchTo(window, f);
				}
			}
		};
	}

	/**
	 * A folder dialog for a workspace (a new folder is created): null if canceled
	 * or the folder can't be used.
	 */
	public static File choose(Shell shell, File start) {
		DirectoryDialog dialog = new DirectoryDialog(shell, SWT.NONE);
		dialog.setText("Select a Workspace");
		dialog.setMessage("Select a folder as Bio7 workspace (an empty or new folder for a new workspace).");
		File base = start != null && start.getParentFile() != null ? start.getParentFile() : new File(System.getProperty("user.home"), "Bio7");
		dialog.setFilterPath(base.getPath());
		String path = dialog.open();
		if (path == null) {
			return null;
		}
		File f = new File(path).getAbsoluteFile();
		if (!f.isDirectory() && !f.mkdirs()) {
			MessageDialog.openError(shell, "Switch Workspace", "The folder can't be created:\n" + f);
			return null;
		}
		if (!f.canWrite()) {
			MessageDialog.openError(shell, "Switch Workspace", "The folder is not writable:\n" + f);
			return null;
		}
		return f;
	}

	/** Restarts Bio7 in the workspace (after a confirmation; the open editors are saved). */
	public static void switchTo(IWorkbenchWindow window, File workspace) {
		if (same(workspace, current())) {
			MessageDialog.openInformation(window.getShell(), "Switch Workspace", "Bio7 already uses this workspace:\n" + workspace);
			return;
		}
		if (!workspace.isDirectory()) {
			MessageDialog.openError(window.getShell(), "Switch Workspace", "The workspace doesn't exist anymore:\n" + workspace);
			return;
		}
		boolean ok = MessageDialog.openConfirm(window.getShell(), "Switch Workspace",
				"Bio7 restarts in the workspace:\n" + workspace + "\n\nThe open editors are saved first.");
		if (!ok) {
			return;
		}
		remember(workspace);
		DebugRestart.restartInWorkspace(window, workspace);
	}

	/* The menu text: the path (the home folder as ~), & escaped for the menu */
	private static String label(File f) {
		String home = System.getProperty("user.home");
		String p = f.getPath();
		if (home != null && p.startsWith(home + File.separator)) {
			p = "~" + p.substring(home.length());
		}
		return p.replace("&", "&&");
	}

	private static boolean contains(List<File> list, File f) {
		return list.stream().anyMatch(x -> same(x, f));
	}

	private static boolean same(File a, File b) {
		if (a == null || b == null) {
			return false;
		}
		try {
			return a.getCanonicalFile().equals(b.getCanonicalFile());
		} catch (IOException e) {
			return a.getAbsoluteFile().equals(b.getAbsoluteFile());
		}
	}
}
