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
package com.eco.bio7.gdal.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.eco.bio7.gdal.GdalLibraryFinder;

/**
 * Installs GDAL with a package manager of the operating system or conda-forge
 * (in a terminal: the output and a password can be seen and entered). After the
 * installation "Detect" in the preferences finds the library.
 */
public final class GdalInstaller extends Dialog {

	/** A way to install GDAL: a command for a terminal or a web page. */
	record Way(String title, String description, String command, String url) {
	}

	/* The conda environment of GDAL (found by the automatic search) */
	static final String CONDA_ENV = "bio7-gdal";

	private final List<Way> ways;
	private Way selected;
	private Text commandText;

	private GdalInstaller(Shell shell, List<Way> ways) {
		super(shell);
		this.ways = ways;
		this.selected = ways.isEmpty() ? null : ways.get(0);
	}

	/** Opens the dialog of the installation (UI thread). */
	public static void open(Shell shell) {
		new GdalInstaller(shell, ways()).open();
	}

	/** The ways to install GDAL on this system (the recommended first). */
	static List<Way> ways() {
		List<Way> w = new ArrayList<>();
		Path conda = conda();
		String condaCommand = conda == null ? null
				: quote(conda.toString()) + " create -y -n " + CONDA_ENV + " -c conda-forge --override-channels libgdal-core";
		if (GdalLibraryFinder.MAC) {
			Path brew = firstExisting(Path.of("/opt/homebrew/bin/brew"), Path.of("/usr/local/bin/brew"));
			if (brew != null) {
				w.add(new Way("Homebrew", "brew install gdal (GDAL with all drivers, updated with brew upgrade)", quote(brew.toString()) + " install gdal",
						null));
			}
			if (condaCommand != null) {
				w.add(new Way("conda-forge", "A conda environment '" + CONDA_ENV + "' with GDAL (independent of other packages)", condaCommand, null));
			}
			if (brew == null) {
				w.add(new Way("Install Homebrew", "Homebrew is not installed: the web page of Homebrew (then 'brew install gdal')", null, "https://brew.sh"));
			}
		} else if (GdalLibraryFinder.WINDOWS) {
			if (condaCommand != null) {
				w.add(new Way("conda-forge", "A conda environment '" + CONDA_ENV + "' with GDAL (independent of other packages)", condaCommand, null));
			}
			w.add(new Way("OSGeo4W", "The OSGeo4W installer (Express install: GDAL); the library is found in C:\\OSGeo4W\\bin or, installed for the "
					+ "current user, in %LOCALAPPDATA%\\Programs\\OSGeo4W\\bin. For exact datum transformations (e.g., NAD27, DHDN) also select the "
					+ "package 'proj-data' (Advanced Install, Libs)", null, "https://trac.osgeo.org/osgeo4w/"));
			w.add(new Way("Miniforge (conda)", "Install Miniforge, then GDAL with conda-forge", null, "https://conda-forge.org/download/"));
		} else {
			if (Files.isExecutable(Path.of("/usr/bin/apt-get"))) {
				w.add(new Way("apt (Debian, Ubuntu)", "The GDAL packages of the distribution (needs sudo)", "sudo apt-get install -y gdal-bin", null));
			}
			if (Files.isExecutable(Path.of("/usr/bin/dnf"))) {
				w.add(new Way("dnf (Fedora, RHEL)", "The GDAL packages of the distribution (needs sudo)", "sudo dnf install -y gdal", null));
			}
			if (Files.isExecutable(Path.of("/usr/bin/zypper"))) {
				w.add(new Way("zypper (openSUSE)", "The GDAL packages of the distribution (needs sudo)", "sudo zypper install -y gdal", null));
			}
			if (Files.isExecutable(Path.of("/usr/bin/pacman"))) {
				w.add(new Way("pacman (Arch)", "The GDAL packages of the distribution (needs sudo)", "sudo pacman -S --noconfirm gdal", null));
			}
			if (condaCommand != null) {
				w.add(new Way("conda-forge", "A conda environment '" + CONDA_ENV + "' with GDAL (independent of other packages, no sudo)", condaCommand, null));
			}
			if (w.isEmpty()) {
				w.add(new Way("Miniforge (conda)", "Install Miniforge, then GDAL with conda-forge", null, "https://conda-forge.org/download/"));
			}
		}
		return w;
	}

	/* The conda (or mamba) executable of an installation */
	static Path conda() {
		String exe = System.getenv("CONDA_EXE");
		if (exe != null && Files.isExecutable(Path.of(exe))) {
			return Path.of(exe);
		}
		String home = System.getProperty("user.home", "");
		List<Path> roots = new ArrayList<>();
		for (String name : List.of("miniforge3", "mambaforge", "miniconda3", "anaconda3", "Miniforge3", "Miniconda3", "Anaconda3")) {
			roots.add(Path.of(home, name));
		}
		if (GdalLibraryFinder.WINDOWS) {
			String programData = System.getenv().getOrDefault("ProgramData", "C:\\ProgramData");
			for (String name : List.of("miniforge3", "miniconda3", "anaconda3")) {
				roots.add(Path.of(programData, name));
			}
		} else {
			roots.add(Path.of("/opt/homebrew/Caskroom/miniforge/base"));
			roots.add(Path.of("/opt/conda"));
			roots.add(Path.of("/opt/anaconda3"));
		}
		for (Path r : roots) {
			Path c = GdalLibraryFinder.WINDOWS ? firstExisting(r.resolve("Scripts").resolve("conda.exe"), r.resolve("condabin").resolve("conda.bat"))
					: firstExisting(r.resolve("bin").resolve("conda"), r.resolve("condabin").resolve("conda"));
			if (c != null) {
				return c;
			}
		}
		return null;
	}

	private static Path firstExisting(Path... paths) {
		for (Path p : paths) {
			if (Files.exists(p)) {
				return p;
			}
		}
		return null;
	}

	private static String quote(String s) {
		return s.contains(" ") ? "\"" + s + "\"" : s;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Install GDAL");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite c = (Composite) super.createDialogArea(parent);
		c.setLayout(new GridLayout(1, false));
		Label intro = new Label(c, SWT.WRAP);
		intro.setText("GDAL is installed with a package manager (in a terminal: you see the output and can enter a password).\n"
				+ "Afterwards press 'Detect' in the GDAL preferences (or restart: the library is found automatically).");
		intro.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		if (ways.isEmpty()) {
			new Label(c, SWT.NONE).setText("No package manager was found on this system.");
			return c;
		}
		for (Way way : ways) {
			Button b = new Button(c, SWT.RADIO);
			b.setText(way.title() + ": " + way.description());
			b.setSelection(way == selected);
			b.addListener(SWT.Selection, _ -> {
				if (b.getSelection()) {
					selected = way;
					commandText.setText(way.command() != null ? way.command() : way.url());
				}
			});
		}
		new Label(c, SWT.NONE).setText("Command (or web page):");
		commandText = new Text(c, SWT.BORDER | SWT.READ_ONLY);
		commandText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		commandText.setText(selected.command() != null ? selected.command() : selected.url());
		return c;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CLIENT_ID, "Copy Command", false);
		createButton(parent, IDialogConstants.OK_ID, "Install", true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
	}

	@Override
	protected void buttonPressed(int id) {
		if (id == IDialogConstants.CLIENT_ID && selected != null) {
			Clipboard cb = new Clipboard(getShell().getDisplay());
			try {
				String text = selected.command() != null ? selected.command() : selected.url();
				cb.setContents(new Object[] { text }, new Transfer[] { TextTransfer.getInstance() });
			} finally {
				cb.dispose();
			}
			return;
		}
		if (id == IDialogConstants.OK_ID && selected != null) {
			if (selected.url() != null) {
				Program.launch(selected.url());
			} else {
				String error = runInTerminal(selected.command());
				if (error != null) {
					MessageDialog.openInformation(getShell(), "Install GDAL", error + "\n\nPlease run the command in a terminal:\n" + selected.command());
					return;
				}
			}
		}
		super.buttonPressed(id);
	}

	/* Runs the command in a terminal window (null or the reason why it failed) */
	static String runInTerminal(String command) {
		String done = "echo; echo 'Finished. Press Detect in the GDAL preferences.'";
		try {
			if (GdalLibraryFinder.MAC) {
				Path script = Files.createTempFile("install_gdal", ".command");
				Files.writeString(script, "#!/bin/bash\n" + command + "\n" + done + "\n");
				script.toFile().setExecutable(true);
				new ProcessBuilder("open", "-a", "Terminal", script.toString()).start();
				return null;
			}
			if (GdalLibraryFinder.WINDOWS) {
				new ProcessBuilder("cmd.exe", "/c", "start", "\"Install GDAL\"", "cmd.exe", "/k", command).start();
				return null;
			}
			String shell = command + "; " + done + "; read -p 'Press Enter to close' x";
			for (List<String> term : List.of(List.of("x-terminal-emulator", "-e", "bash", "-c", shell), List.of("gnome-terminal", "--", "bash", "-c", shell),
					List.of("konsole", "-e", "bash", "-c", shell), List.of("xfce4-terminal", "-x", "bash", "-c", shell), List.of("xterm", "-e", "bash", "-c", shell))) {
				try {
					new ProcessBuilder(term).start();
					return null;
				} catch (IOException e) {
					/* The next terminal */
				}
			}
			return "No terminal was found.";
		} catch (IOException e) {
			return "The terminal could not be opened: " + e.getMessage();
		}
	}
}
