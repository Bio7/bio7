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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.RLocator.RInstallation;
import com.eco.bio7.util.Util;

/**
 * The setup of R: the R installations of the computer (the one for Bio7 can be
 * selected) and help to install R.
 */
public class RSetupDialog extends Dialog {

	private static RSetupDialog instance;

	private final IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
	private final List<RInstallation> installations = new ArrayList<>();

	private Combo rCombo;
	private Label rDetails;
	private Button useR;
	private Button searchR;
	private Text log;
	/* A plain text field: its colors (of the theme) are used for the log */
	private Text colorProbe;
	private List<RInstaller.InstallCommand> installCommands = List.of();
	private Combo commandCombo;
	private Text commandText;
	private final List<Button> actions = new ArrayList<>();
	private boolean busy;
	private int busyCount;

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
		shell.setText("R Setup");
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

		/* Log */
		Composite logGroup = group(area, "Log", 1, HELP_LOG);
		/* Not SWT.READ_ONLY: a read-only multi-line text gets no theme color on macOS (black in the dark theme) */
		log = new Text(logGroup, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		log.setEditable(false);
		GridData logData = new GridData(SWT.FILL, SWT.FILL, true, true);
		logData.heightHint = 160;
		logData.widthHint = 560;
		log.setLayoutData(logData);
		colorProbe = new Text(logGroup, SWT.BORDER);
		GridData probeData = new GridData();
		probeData.exclude = true;
		colorProbe.setLayoutData(probeData);
		colorProbe.setVisible(false);

		searchR();
		return area;
	}

	@Override
	public int open() {
		int result = super.open();
		/* The colors of a text field (after the theme was applied), also for the log */
		getShell().getDisplay().asyncExec(() -> {
			if (log != null && !log.isDisposed() && !colorProbe.isDisposed()) {
				log.setForeground(colorProbe.getForeground());
				log.setBackground(colorProbe.getBackground());
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
		installCommands = RInstaller.rInstallCommands();
		label(install, "Install R:", "Commands to install R on this system");
		if (installCommands.isEmpty()) {
			Label hint = wrapLabel(install, 1);
			hint.setText(RInstaller.rInstallHint());
		} else {
			commandCombo = new Combo(install, SWT.READ_ONLY);
			GridData comboData = new GridData(SWT.FILL, SWT.CENTER, true, false);
			comboData.widthHint = 380;
			commandCombo.setLayoutData(comboData);
			for (RInstaller.InstallCommand c : installCommands) {
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
			commandCombo.select(RInstaller.preferredInstallCommand(installCommands));
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
		button(buttons, "Open R Download Page", () -> Program.launch(RInstaller.rDownloadPage()));
		if (RInstaller.canInstallRWithWinget()) {
			button(buttons, "Install R with winget", () -> installRWithWinget());
		}
	}

	/* ---------------- The help ---------------- */

	private static final String HELP_R = """
			Bio7 starts R in its console, in the process of Bio7 (embedded R).

			The list shows the R installations found on this computer: the path of the preferences, R_HOME, the PATH, the Windows registry and the usual installation folders.

			Search Again: searches again (e.g., after you installed R).

			Use for Bio7: sets the selected R as the "Path to R" of the preferences. It is used the next time Bio7 starts R (restart Bio7 if R is already running).

			Install R: the list contains commands to install R for this system (macOS: the installer package of CRAN, Homebrew, rig; Linux: the package managers of the distributions, the current R of CRAN for Ubuntu, rig). The command for this system is preselected. Run in Terminal runs it in the Terminal of Bio7: enter your password there if asked (sudo). Copy Command copies it for another terminal. Open R Download Page opens the download page of CRAN (on Windows you can also install R with winget). After the installation of R click Search Again.""";

	private static final String HELP_LOG = """
			Shows the steps and the output of R while searching for R and installing it.

			If an installation fails, the cause is usually at the end of the output (e.g., a download error).""";

	/* ---------------- The widgets ---------------- */

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

	private static void label(Composite parent, String text, String tip) {
		Label l = new Label(parent, SWT.NONE);
		l.setText(text);
		l.setToolTipText(tip);
	}

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

	private void relayout() {
		getShell().layout(true, true);
	}

	private void useSelectedR() {
		RInstallation r = selectedR();
		if (r == null) {
			return;
		}
		store.setValue(PreferenceConstants.PATH_R, r.home());
		logger.accept("The path of R is now " + r.home() + " (used at the next start of R).");
		updateSelection();
	}

	private void installRWithWinget() {
		run("Installing R with winget", monitor -> {
			int exit = RInstaller.installRWithWinget(logger, monitor);
			logger.accept(exit == 0 ? "R was installed. Searching again..." : "winget ended with code " + exit + ".");
			if (exit == 0) {
				ui(this::searchR);
			}
		});
	}

	private void runInTerminal() {
		RInstaller.InstallCommand command = installCommands.get(commandCombo.getSelectionIndex());
		if (org.eclipse.core.runtime.Platform.getBundle("org.eclipse.terminal.view.ui") == null) {
			logger.accept("The Terminal is not available. Please copy the command and run it in a terminal.");
			return;
		}
		try {
			java.io.File script = RInstaller.writeTerminalScript(command);
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

	/* Appends a line to the log (any thread) */
	private final Consumer<String> logger = line -> ui(() -> {
		if (!log.isDisposed()) {
			log.append(line + Text.DELIMITER);
		}
	});

	/* Updates the labels and buttons for the selected R */
	private void updateSelection() {
		RInstallation r = selectedR();
		boolean hasR = r != null;
		useR.setEnabled(!busy && hasR);
		if (!hasR) {
			rDetails.setText(installations.isEmpty() && !busy ? "No R installation was found. Please install R (see below) and search again." : "");
			relayout();
			return;
		}
		String current = store.getString(PreferenceConstants.PATH_R);
		rDetails.setText("Home: " + r.home() + "\nPlatform: " + r.platform() + "\nLibraries: " + String.join(", ", r.libraryPaths())
				+ (r.home().equals(current) ? "\n(This R is used by Bio7.)" : ""));
		relayout();
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
					rCombo.add("R " + r.version() + " (" + r.arch() + ") " + r.home());
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

	/* ---------------- The jobs ---------------- */

	@FunctionalInterface
	private interface Task {
		void run(IProgressMonitor monitor) throws Exception;
	}
}
