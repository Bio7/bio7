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

package com.eco.bio7.rbridge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.RLocator.RInstallation;
import com.eco.bio7.rbridge.RserveInstaller.Artifact;
import com.eco.bio7.rbridge.RserveInstaller.Choice;
import com.eco.bio7.rbridge.RserveInstaller.Manifest;
import com.eco.bio7.util.Util;

/**
 * The setup of R and Rserve: the R installations of the computer (the one for
 * Bio7 can be selected), help to install R, and the installation of Rserve for
 * the cooperative mode (the recommended binary of the Bio7 repository,
 * compiled from the sources, or manually: any file of the repository, a local
 * file, the R commands, the library and the connection settings).
 */
public class RSetupDialog extends Dialog {

	private static RSetupDialog instance;

	private final IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
	private final List<RInstallation> installations = new ArrayList<>();
	private Manifest manifest;
	private Choice choice;

	private Combo rCombo;
	private Label rDetails;
	private Button useR;
	private Button searchR;
	private Label rserveStatus;
	private Label recommended;
	private Button installRecommended;
	private Button compile;
	private Combo repoCombo;
	private Button installSelected;
	private Button installLocal;
	private Button copyCommand;
	private Text libraryText;
	private Text portText;
	private Text argsText;
	private Text compileCommand;
	private Text log;
	private List<RserveInstaller.InstallCommand> installCommands = List.of();
	private Combo commandCombo;
	private Text commandText;
	private final List<Button> actions = new ArrayList<>();
	private boolean busy;
	private int busyCount;
	private String libraryForHome;

	/** Opens the dialog (once) in the UI thread. */
	public static void openSetup() {
		Display display = Display.getDefault();
		Runnable open = () -> {
			if (instance != null && instance.getShell() != null && !instance.getShell().isDisposed()) {
				instance.getShell().setActive();
				return;
			}
			instance = new RSetupDialog(Util.getShell());
			instance.open();
		};
		if (Display.getCurrent() == display) {
			open.run();
		} else {
			display.asyncExec(open);
		}
	}

	public RSetupDialog(Shell parent) {
		super(parent);
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MODELESS);
		setBlockOnOpen(false);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("R and Rserve Setup");
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CLOSE_ID, IDialogConstants.CLOSE_LABEL, true);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CLOSE_ID) {
			close();
			return;
		}
		super.buttonPressed(buttonId);
	}

	@Override
	public boolean close() {
		instance = null;
		return super.close();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		area.setLayout(new GridLayout(1, false));

		/* R */
		Composite rGroup = group(area, "R", 3, HELP_R);
		rCombo = new Combo(rGroup, SWT.READ_ONLY);
		GridData rComboData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		/* Not as wide as the longest entry */
		rComboData.widthHint = 380;
		rCombo.setLayoutData(rComboData);
		rCombo.setItems("Searching for R installations...");
		rCombo.select(0);
		rCombo.addListener(SWT.Selection, _ -> updateSelection());
		searchR = button(rGroup, "Search Again", () -> searchR());
		useR = button(rGroup, "Use for Bio7", () -> useSelectedR());
		rDetails = wrapLabel(rGroup, 3);
		createInstallR(rGroup);

		/* Rserve (automatic) */
		Composite rserveGroup = group(area, "Rserve (cooperative mode)", 3, HELP_RSERVE);
		rserveStatus = wrapLabel(rserveGroup, 3);
		recommended = wrapLabel(rserveGroup, 3);
		recommended.setText("Loading the list of the Bio7 repository...");
		installRecommended = button(rserveGroup, "Install Recommended Binary", () -> installRecommended());
		compile = button(rserveGroup, "Compile from Source...", () -> compileFromSource());
		new Label(rserveGroup, SWT.NONE);

		/* Manual */
		Composite manualGroup = group(area, "Manual installation and configuration", 4, HELP_MANUAL);
		label(manualGroup, "Repository file:", TIP_REPOSITORY);
		repoCombo = new Combo(manualGroup, SWT.READ_ONLY);
		repoCombo.setToolTipText(TIP_REPOSITORY);
		GridData repoData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		repoData.widthHint = 380;
		repoCombo.setLayoutData(repoData);
		new Label(manualGroup, SWT.NONE);
		installSelected = button(manualGroup, "Install Selected File", () -> installSelectedFile());
		installLocal = button(manualGroup, "Install Local File...", () -> installLocalFile());
		copyCommand = button(manualGroup, "Copy R Command", () -> copyRCommand());

		label(manualGroup, "Install into library:", TIP_LIBRARY);
		libraryText = new Text(manualGroup, SWT.BORDER);
		libraryText.setToolTipText(TIP_LIBRARY);
		libraryText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		button(manualGroup, "Browse...", () -> {
			DirectoryDialog d = new DirectoryDialog(getShell());
			d.setFilterPath(libraryText.getText());
			String dir = d.open();
			if (dir != null) {
				libraryText.setText(dir);
			}
		});

		label(manualGroup, "Compile manually:", TIP_COMPILE);
		compileCommand = new Text(manualGroup, SWT.BORDER | SWT.READ_ONLY);
		GridData compileData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		compileData.widthHint = 380;
		compileCommand.setLayoutData(compileData);
		compileCommand.setText(RserveInstaller.manualCompileCommand(null));

		label(manualGroup, "Rserve port:", TIP_PORT);
		portText = new Text(manualGroup, SWT.BORDER);
		portText.setToolTipText(TIP_PORT);
		GridData portData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
		portData.widthHint = 80;
		portText.setLayoutData(portData);
		portText.setText(Integer.toString(store.getInt("RSERVE_CLIENT_CONNECTION_PORT")));
		label(manualGroup, "Rserve arguments:", TIP_ARGS);
		argsText = new Text(manualGroup, SWT.BORDER);
		argsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		argsText.setText(store.getString("RSERVE_ARGS"));
		argsText.setToolTipText("Arguments of run.Rserve(), e.g.: remote=FALSE (a port here replaces the port above)");
		button(manualGroup, "Save Settings", () -> saveSettings());

		/* Log */
		Composite logGroup = group(area, "Log", 1, HELP_LOG);
		/* Not SWT.READ_ONLY: a read-only multi-line text gets no theme color on macOS (black in the dark theme) */
		log = new Text(logGroup, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		log.setEditable(false);
		GridData logData = new GridData(SWT.FILL, SWT.FILL, true, true);
		logData.heightHint = 160;
		logData.widthHint = 560;
		log.setLayoutData(logData);

		searchR();
		loadManifest();
		return area;
	}

	@Override
	public int open() {
		int result = super.open();
		/* The colors of a styled text field (after the theme was applied), also for the log */
		getShell().getDisplay().asyncExec(() -> {
			if (log != null && !log.isDisposed() && !libraryText.isDisposed()) {
				log.setForeground(libraryText.getForeground());
				log.setBackground(libraryText.getBackground());
			}
		});
		return result;
	}

	/*
	 * The installation of R: the commands for this system (run in the Terminal of
	 * Bio7, sudo can ask for the password there), the download page, winget
	 */
	private void createInstallR(Composite parent) {
		Composite install = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		install.setLayout(layout);
		install.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));
		installCommands = RserveInstaller.rInstallCommands();
		label(install, "Install R:", "Commands to install R on this system");
		if (installCommands.isEmpty()) {
			Label hint = wrapLabel(install, 1);
			hint.setText(RserveInstaller.rInstallHint());
		} else {
			commandCombo = new Combo(install, SWT.READ_ONLY);
			GridData comboData = new GridData(SWT.FILL, SWT.CENTER, true, false);
			comboData.widthHint = 380;
			commandCombo.setLayoutData(comboData);
			for (RserveInstaller.InstallCommand c : installCommands) {
				commandCombo.add(c.label());
			}
			new Label(install, SWT.NONE);
			/* The command (selectable, e.g., to copy a part) */
			commandText = new Text(install, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
			commandText.setEditable(false);
			GridData textData = new GridData(SWT.FILL, SWT.TOP, true, false);
			textData.widthHint = 380;
			textData.heightHint = commandText.getLineHeight() * 4 + 6;
			commandText.setLayoutData(textData);
			commandCombo.addListener(SWT.Selection, _ -> commandText.setText(installCommands.get(commandCombo.getSelectionIndex()).command()));
			commandCombo.select(RserveInstaller.preferredInstallCommand(installCommands));
			commandText.setText(installCommands.get(commandCombo.getSelectionIndex()).command());
		}
		new Label(install, SWT.NONE);
		Composite buttons = new Composite(install, SWT.NONE);
		GridLayout buttonLayout = new GridLayout(4, false);
		buttonLayout.marginWidth = 0;
		buttons.setLayout(buttonLayout);
		if (!installCommands.isEmpty()) {
			button(buttons, "Run in Terminal", () -> runInTerminal());
			button(buttons, "Copy Command", () -> {
				copyToClipboard(commandText.getText());
				logger.accept("Copied the command (paste it into a terminal).");
			});
		}
		button(buttons, "Open R Download Page", () -> Program.launch(RserveInstaller.rDownloadPage()));
		if (RserveInstaller.canInstallRWithWinget()) {
			button(buttons, "Install R with winget", () -> installRWithWinget());
		}
	}

	/* Runs the selected command in the Terminal view of Bio7 (as the Terminal of the toolbar) */
	private void runInTerminal() {
		RserveInstaller.InstallCommand command = installCommands.get(commandCombo.getSelectionIndex());
		if (org.eclipse.core.runtime.Platform.getBundle("org.eclipse.terminal.view.ui") == null) {
			logger.accept("The Terminal is not available. Please copy the command and run it in a terminal.");
			return;
		}
		try {
			java.io.File script = RserveInstaller.writeTerminalScript(command);
			new com.eco.bio7.console.ShellUtil().execShellCommand("Install R", "/bin/bash", script.getAbsolutePath());
			logger.accept("Started in the Terminal: " + command.label() + ". Enter your password there if asked, then click 'Search Again'.");
		} catch (Exception e) {
			logger.accept("The Terminal could not be started: " + e.getMessage());
		}
	}

	private void copyToClipboard(String text) {
		Clipboard clipboard = new Clipboard(getShell().getDisplay());
		clipboard.setContents(new Object[] { text }, new Transfer[] { TextTransfer.getInstance() });
		clipboard.dispose();
	}

	/* ---------------- The help ---------------- */

	private static final String HELP_R = """
			Bio7 starts R in its console and connects to it with Rserve.

			The list shows the R installations found on this computer: the path of the preferences, R_HOME, the PATH, the Windows registry and the usual installation folders. "[Rserve ...]" marks an R that already has Rserve.

			Search Again: searches again (e.g., after you installed R).

			Use for Bio7: sets the selected R as the "Path to R" of the preferences. It is used the next time R starts in the console.

			Install R: the list contains commands to install R for this system (macOS: the installer package of CRAN, Homebrew, rig; Linux: the package managers of the distributions, the current R of CRAN for Ubuntu, rig). The command for this system is preselected. Run in Terminal runs it in the Terminal of Bio7: enter your password there if asked (sudo). Copy Command copies it for another terminal. Open R Download Page opens the download page of CRAN (on Windows you can also install R with winget). After the installation of R click Search Again.""";

	private static final String HELP_RSERVE = """
			Rserve connects Bio7 to the R of the console. Bio7 needs Rserve built for the cooperative mode: then Bio7 and the console share one R workspace. (The usual Rserve of CRAN creates a copy of R for each connection on macOS and Linux; Bio7 detects this and closes the connection.)

			The first line shows the Rserve installed in the selected R.

			Install Recommended Binary: downloads the file of the Bio7 repository (github.com/Bio7/Rserve-Cooperative) for this system, processor and version of R, checks it (SHA-256) and installs it. On Windows the Rserve of CRAN is used (there it always works cooperatively).

			Compile from Source: compiles the current Rserve of CRAN for the cooperative mode. This needs the build tools (macOS: Xcode command line tools, Windows: Rtools, Linux: gcc and make); Bio7 checks for them first.

			After an installation restart R in the console.""";

	private static final String HELP_MANUAL = """
			For installing a specific file yourself and for the connection settings.

			Repository file: all files of the Bio7 repository (the recommended one is preselected). Install Selected File installs it. Copy R Command copies the install.packages() command, so you can run it in the R console yourself.

			Install Local File: installs a package file you have already downloaded (.tgz, .zip or .tar.gz). A source archive is compiled for the cooperative mode.

			Install into library: the folder the Rserve package is installed into (an R "library" is a folder with R packages). Bio7 suggests the package install location of the Bio7 preferences, else the first library R can write to, else your personal library (R_LIBS_USER). R must find the package there later: use a folder of .libPaths() in R, or the package install location of the Bio7 preferences (Bio7 adds it when R starts). The system library of R often needs administrator rights.

			Compile manually: the shell command to compile an Rserve source archive for the cooperative mode yourself.

			Rserve port: the local port Bio7 uses to connect to Rserve (default 6311). If it is taken, Bio7 uses a free port automatically.

			Rserve arguments: additional arguments of run.Rserve() (e.g., remote=FALSE). A port given here is always used (no automatic port).

			Save Settings stores the port and the arguments in the Bio7 preferences.""";

	private static final String HELP_LOG = """
			Shows the steps and the output of R while searching and installing.

			If an installation fails, the cause is usually at the end of the output (e.g., a missing compiler, a download error or a library without write permission).""";

	private static final String TIP_REPOSITORY = "All Rserve files of the Bio7 repository (system, processor, R version they were built for)";
	private static final String TIP_LIBRARY = "The folder (R library) Rserve is installed into; R must find it (.libPaths() or the package install location of the preferences)";
	private static final String TIP_COMPILE = "Shell command to compile an Rserve source archive for the cooperative mode yourself";
	private static final String TIP_PORT = "Local port for Rserve (default 6311); a free port is used if it is taken";
	private static final String TIP_ARGS = "Additional arguments of run.Rserve(), e.g., remote=FALSE; a port here is always used";

	private static void label(Composite parent, String text, String tip) {
		Label l = new Label(parent, SWT.NONE);
		l.setText(text);
		l.setToolTipText(tip);
	}

	/* ---------------- The widgets ---------------- */

	/*
	 * A section: a bold header with a line and the content (not a Group: its title
	 * is drawn with a small font on macOS)
	 */
	private Composite group(Composite parent, String text, int columns, String help) {
		boolean fill = text.equals("Log");
		Composite header = new Composite(parent, SWT.NONE);
		GridLayout headerLayout = new GridLayout(3, false);
		headerLayout.marginWidth = 0;
		headerLayout.marginHeight = 0;
		header.setLayout(headerLayout);
		header.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		Label title = new Label(header, SWT.NONE);
		title.setText(text);
		title.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
		new Label(header, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		/* The help of the section */
		ToolBar bar = new ToolBar(header, SWT.FLAT);
		ToolItem item = new ToolItem(bar, SWT.PUSH);
		item.setImage(JFaceResources.getImage(Dialog.DLG_IMG_HELP));
		item.setToolTipText("Help: " + text);
		item.addListener(SWT.Selection, _ -> MessageDialog.openInformation(getShell(), "Help: " + text, help));
		Composite g = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(columns, false);
		layout.marginLeft = 8;
		layout.marginBottom = 6;
		g.setLayout(layout);
		g.setLayoutData(new GridData(SWT.FILL, fill ? SWT.FILL : SWT.TOP, true, fill));
		return g;
	}

	private Button button(Composite parent, String text, Runnable action) {
		Button b = new Button(parent, SWT.PUSH);
		b.setText(text);
		b.addListener(SWT.Selection, _ -> action.run());
		actions.add(b);
		return b;
	}

	private static Label wrapLabel(Composite parent, int span) {
		Label l = new Label(parent, SWT.WRAP);
		GridData d = new GridData(SWT.FILL, SWT.CENTER, true, false, span, 1);
		d.widthHint = 600;
		l.setLayoutData(d);
		return l;
	}

	/* Counts the running jobs (a job can start another one) */
	private void setBusy(boolean b) {
		busyCount = Math.max(0, busyCount + (b ? 1 : -1));
		busy = busyCount > 0;
		b = busy;
		if (getShell() == null || getShell().isDisposed()) {
			return;
		}
		for (Button button : actions) {
			if (!button.isDisposed()) {
				button.setEnabled(!b);
			}
		}
		if (!b) {
			updateSelection();
		}
	}

	/* Appends a line to the log (any thread) */
	private final Consumer<String> logger = line -> ui(() -> {
		if (!log.isDisposed()) {
			log.append(line + Text.DELIMITER);
		}
	});

	private void ui(Runnable r) {
		Display display = Display.getDefault();
		display.asyncExec(() -> {
			if (getShell() != null && !getShell().isDisposed()) {
				r.run();
			}
		});
	}

	private RInstallation selectedR() {
		int i = rCombo.getSelectionIndex();
		return i >= 0 && i < installations.size() ? installations.get(i) : null;
	}

	private Artifact selectedArtifact() {
		int i = repoCombo.getSelectionIndex();
		return manifest != null && i >= 0 && i < manifest.files().size() ? manifest.files().get(i) : null;
	}

	/* Updates the labels and buttons for the selected R */
	private void updateSelection() {
		RInstallation r = selectedR();
		boolean hasR = r != null;
		useR.setEnabled(!busy && hasR);
		installRecommended.setEnabled(false);
		compile.setEnabled(!busy && hasR);
		installSelected.setEnabled(!busy && hasR && selectedArtifact() != null);
		installLocal.setEnabled(!busy && hasR);
		copyCommand.setEnabled(selectedArtifact() != null);
		if (!hasR) {
			rDetails.setText(installations.isEmpty() && !busy ? "No R installation was found. Please install R (see below) and search again." : "");
			rserveStatus.setText("");
			relayout();
			return;
		}
		String current = store.getString(PreferenceConstants.PATH_R);
		rDetails.setText("Home: " + r.home() + "\nPlatform: " + r.platform() + "\nLibraries: " + String.join(", ", r.libraryPaths())
				+ (r.home().equals(current) ? "\n(This R is used by Bio7.)" : ""));
		rserveStatus.setText((r.isRserveUsable() && !r.isRserveBuiltForOtherR() ? "OK: " : "") + r.describeRserve()
				+ (r.rserveLibrary() != null ? " in " + r.rserveLibrary() : ""));
		if (manifest != null) {
			choice = RserveInstaller.choose(manifest, r);
			recommended.setText("Recommended: " + choice.text());
			installRecommended.setEnabled(!busy && choice.artifact() != null);
			/* Select the recommended file in the list of the repository */
			if (choice.artifact() != null && repoCombo.getSelectionIndex() < 0) {
				repoCombo.select(manifest.files().indexOf(choice.artifact()));
				installSelected.setEnabled(!busy);
				copyCommand.setEnabled(true);
			}
		}
		if (libraryText.getText().isBlank() || !r.home().equals(libraryForHome)) {
			libraryForHome = r.home();
			String lib = RserveInstaller.targetLibrary(r, store.getString("InstallLocation"));
			libraryText.setText(lib == null ? "" : lib);
		}
		compileCommand.setText(RserveInstaller.manualCompileCommand(libraryText.getText().isBlank() ? null : libraryText.getText()));
		relayout();
	}

	private void relayout() {
		getShell().layout(true, true);
	}

	/* ---------------- The actions ---------------- */

	private void searchR() {
		String preferred = store.getString(PreferenceConstants.PATH_R);
		run("Searching for R installations", monitor -> {
			List<RInstallation> found = RLocator.find(preferred);
			ui(() -> {
				installations.clear();
				installations.addAll(found);
				rCombo.removeAll();
				int select = 0;
				for (int i = 0; i < found.size(); i++) {
					RInstallation r = found.get(i);
					rCombo.add("R " + r.version() + " (" + r.arch() + ") " + r.home() + (r.hasRserve() ? "  [Rserve " + r.rserveVersion() + "]" : ""));
					if (r.home().equals(preferred)) {
						select = i;
					}
				}
				if (found.isEmpty()) {
					rCombo.add("No R installation found");
				}
				rCombo.select(select);
				logger.accept(found.isEmpty() ? "No R installation was found." : found.size() + " R installation(s) found.");
			});
		});
	}

	private void loadManifest() {
		Job job = new Job("Loading the Rserve list of the Bio7 repository") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Manifest m = RserveInstaller.loadManifest(logger);
				ui(() -> {
					manifest = m;
					repoCombo.removeAll();
					for (Artifact a : m.files()) {
						repoCombo.add(a.isCran() ? "Windows: Rserve of CRAN" : a.os() + " " + a.arch() + ", R " + a.r() + ", Rserve " + a.rserve() + ": " + a.fileName());
					}
					updateSelection();
				});
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	private void useSelectedR() {
		RInstallation r = selectedR();
		if (r == null) {
			return;
		}
		store.setValue(PreferenceConstants.PATH_R, r.home());
		logger.accept("The path of R is now " + r.home() + " (used at the next start of R in the console).");
		updateSelection();
	}

	private void saveSettings() {
		try {
			int port = Integer.parseInt(portText.getText().trim());
			if (port < 1 || port > 65535) {
				throw new NumberFormatException();
			}
			store.setValue("RSERVE_CLIENT_CONNECTION_PORT", port);
		} catch (NumberFormatException e) {
			MessageDialog.openWarning(getShell(), "Rserve", "Please enter a port between 1 and 65535.");
			return;
		}
		store.setValue("RSERVE_ARGS", argsText.getText().trim());
		logger.accept("Saved: port " + portText.getText().trim() + ", Rserve arguments: " + (argsText.getText().isBlank() ? "(none)" : argsText.getText().trim()));
	}

	private void copyRCommand() {
		Artifact a = selectedArtifact();
		if (a == null) {
			return;
		}
		String lib = libraryText.getText().isBlank() ? null : libraryText.getText().trim();
		String command = RserveInstaller.manualCommands(a, lib, cranRepository());
		Clipboard clipboard = new Clipboard(getShell().getDisplay());
		clipboard.setContents(new Object[] { command }, new Transfer[] { TextTransfer.getInstance() });
		clipboard.dispose();
		logger.accept("Copied (paste it into the R console; restart R afterwards if Rserve was loaded): " + command);
	}

	private void installRecommended() {
		if (choice == null || choice.artifact() == null) {
			return;
		}
		if (!choice.exact() && !MessageDialog.openQuestion(getShell(), "Rserve",
				"The binary was built for another version of R and may not work:\n" + choice.text() + "\n\nInstall it anyway?")) {
			return;
		}
		installArtifact(choice.artifact());
	}

	private void installSelectedFile() {
		Artifact a = selectedArtifact();
		if (a != null) {
			installArtifact(a);
		}
	}

	private void installArtifact(Artifact artifact) {
		RInstallation r = selectedR();
		String library = library(r);
		if (library == null || !prepareInstall()) {
			return;
		}
		String repo = cranRepository();
		install("Installing Rserve", r, monitor -> RserveInstaller.installBinary(r, artifact, library, repo, logger, monitor));
	}

	private void installLocalFile() {
		RInstallation r = selectedR();
		FileDialog d = new FileDialog(getShell(), SWT.OPEN);
		d.setText("Select the Rserve package (binary or source archive)");
		d.setFilterExtensions(new String[] { "*.tgz;*.tar.gz;*.zip", "*.*" });
		String file = d.open();
		if (file == null) {
			return;
		}
		String library = library(r);
		if (library == null || !prepareInstall()) {
			return;
		}
		install("Installing Rserve", r, monitor -> RserveInstaller.installFile(r, file, library, logger, monitor));
	}

	private void compileFromSource() {
		RInstallation r = selectedR();
		String library = library(r);
		if (library == null) {
			return;
		}
		Manifest m = manifest;
		String repo = cranRepository();
		run("Checking the build tools", monitor -> {
			RLocator.BuildTools tools = RLocator.checkBuildTools(r);
			logger.accept(tools.detail());
			ui(() -> {
				if (!tools.available()) {
					boolean mac = r.os().equals("mac");
					MessageDialog dialog = new MessageDialog(getShell(), "Build tools", null, tools.detail() + "\n\n" + tools.help(), MessageDialog.WARNING, 0,
							mac ? new String[] { "Install Command Line Tools", "Close" } : new String[] { "Close" });
					if (dialog.open() == 0 && mac) {
						/* Opens the installer of Apple */
						run("Installing the command line tools", mon -> RserveInstaller.runProcess(List.of("xcode-select", "--install"), java.util.Map.of(), logger, mon));
					}
					return;
				}
				if (!prepareInstall()) {
					return;
				}
				install("Compiling Rserve", r, mon -> RserveInstaller.compile(r, m == null ? new Manifest(List.of(), null, null, false) : m, library, repo, logger, mon));
			});
		});
	}

	private void installRWithWinget() {
		run("Installing R with winget", monitor -> {
			int exit = RserveInstaller.installRWithWinget(logger, monitor);
			logger.accept(exit == 0 ? "R was installed. Searching again..." : "winget ended with code " + exit + ".");
			if (exit == 0) {
				ui(this::searchR);
			}
		});
	}

	/* The library of the text field (checked) or null */
	private String library(RInstallation r) {
		if (r == null) {
			return null;
		}
		String lib = libraryText.getText().trim();
		if (lib.isEmpty()) {
			MessageDialog.openWarning(getShell(), "Rserve", "No writable R library was found. Please select a folder for the installation.");
			return null;
		}
		return lib;
	}

	/*
	 * The loaded Rserve library can't be replaced on Windows: R of the console has
	 * to be stopped first. Returns false if canceled.
	 */
	private boolean prepareInstall() {
		if (!RserveLauncher.isConsoleRRunning()) {
			return true;
		}
		if (RLocator.os().equals("Windows")) {
			int answer = new MessageDialog(getShell(), "Rserve", null,
					"R is running in the Bio7 console. On Windows it has to be stopped to replace the Rserve package.\n\nStop R now?", MessageDialog.QUESTION, 0,
					"Stop R and Install", "Cancel").open();
			if (answer != 0) {
				return false;
			}
			RserveLauncher.stopConsoleR();
			logger.accept("R of the console was stopped.");
		}
		return true;
	}

	private String cranRepository() {
		String repo = store.getString("R_PACKAGE_SERVER");
		return repo == null || repo.isBlank() ? "https://cloud.r-project.org" : repo.trim();
	}

	/* ---------------- The jobs ---------------- */

	@FunctionalInterface
	private interface Task {
		void run(IProgressMonitor monitor) throws Exception;
	}

	@FunctionalInterface
	private interface InstallTask {
		boolean run(IProgressMonitor monitor) throws Exception;
	}

	/* Runs an installation, then checks the R installation again */
	private void install(String name, RInstallation r, InstallTask task) {
		run(name, monitor -> {
			boolean ok = task.run(monitor);
			RInstallation updated = RLocator.probe(r.home());
			ui(() -> {
				int i = installations.indexOf(r);
				if (i >= 0 && updated.isValid()) {
					installations.set(i, updated);
				}
				updateSelection();
				if (ok) {
					logger.accept("Rserve was installed: " + updated.describeRserve());
					String text = "Rserve was installed (" + updated.describeRserve() + ").";
					if (RserveLauncher.isConsoleRRunning()) {
						int answer = new MessageDialog(getShell(), "Rserve", null,
								text + "\n\nR in the Bio7 console still uses the previous state. Stop R now? (Start R/Rserve again afterwards.)", MessageDialog.QUESTION,
								0, "Stop R", "Later").open();
						if (answer == 0) {
							RserveLauncher.stopConsoleR();
							logger.accept("R of the console was stopped.");
						}
					} else {
						MessageDialog.openInformation(getShell(), "Rserve", text + "\nStart R and Rserve in Bio7 to use it.");
					}
				} else {
					logger.accept("The installation failed (see above).");
					MessageDialog.openError(getShell(), "Rserve", "The installation failed. Please see the log of the dialog.");
				}
			});
		});
	}

	private void run(String name, Task task) {
		setBusy(true);
		logger.accept("== " + name);
		Job job = new Job(name) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					task.run(monitor);
				} catch (InterruptedException e) {
					logger.accept("Canceled.");
				} catch (Exception e) {
					logger.accept("Error: " + e.getMessage());
				} finally {
					ui(() -> setBusy(false));
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}
}
