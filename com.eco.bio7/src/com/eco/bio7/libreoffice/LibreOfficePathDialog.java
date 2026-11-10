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

package com.eco.bio7.libreoffice;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;

/**
 * The dialog when the LibreOffice executable was not found. It lists the usual
 * installation paths for the operating system (existing ones first), which can
 * be copied, and opens the preference page with the LibreOffice path.
 */
public class LibreOfficePathDialog extends MessageDialog {

	/** The preference page with the LibreOffice path. */
	public static final String PREFERENCE_PAGE = "com.eco.bio7.preferences.Bio7PreferencePage";

	private Text paths;
	private Label hint;

	private LibreOfficePathDialog(Shell parent, String message) {
		super(parent, "LibreOffice", null, message, MessageDialog.WARNING, 0, "Open LibreOffice Preferences...", IDialogConstants.CLOSE_LABEL);
	}

	/** Opens the dialog (in the UI thread) and, if requested, the preference page. */
	public static void open(Shell parent) {
		String current = Bio7Plugin.getDefault().getPreferenceStore().getString(PreferenceConstants.PATH_LIBREOFFICE);
		String message = "The LibreOffice executable (soffice, LibreOffice version > 4.0) was not found.\n\n" + "Current path: "
				+ (current == null || current.isBlank() ? "(not set)" : current) + "\n\n"
				+ "Please set the folder which contains 'soffice' in the Bio7 preferences. Usual locations:";
		LibreOfficePathDialog dialog = new LibreOfficePathDialog(parent, message);
		if (dialog.open() == 0) {
			openPreferences(parent);
		}
	}

	/** Opens the Bio7 preference page with the LibreOffice path. */
	public static void openPreferences(Shell parent) {
		PreferenceDialog pref = PreferencesUtil.createPreferenceDialogOn(parent, PREFERENCE_PAGE, null, null);
		if (pref != null) {
			pref.open();
		}
	}

	@Override
	protected Control createCustomArea(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 0;
		area.setLayout(layout);
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		List<String> found = new ArrayList<String>();
		List<String> other = new ArrayList<String>();
		for (String p : suggestions()) {
			(new File(p, executable()).exists() ? found : other).add(p);
		}
		StringBuilder text = new StringBuilder();
		for (String p : found) {
			text.append(p).append('\n');
		}
		for (String p : other) {
			text.append(p).append('\n');
		}
		/* Not SWT.READ_ONLY: a read-only multi-line text gets no theme color on macOS (black in the dark theme) */
		paths = new Text(area, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL);
		paths.setEditable(false);
		paths.setText(text.toString().trim());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 420;
		/* At least two lines and the horizontal scrollbar */
		int lines = Math.max(2, Math.min(6, found.size() + other.size()));
		gd.heightHint = paths.getLineHeight() * lines + 8 + (paths.getHorizontalBar() != null ? paths.getHorizontalBar().getSize().y : 0);
		paths.setLayoutData(gd);

		Composite buttons = new Composite(area, SWT.NONE);
		GridLayout buttonLayout = new GridLayout(1, true);
		buttonLayout.marginWidth = 0;
		buttonLayout.marginHeight = 0;
		buttons.setLayout(buttonLayout);
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		Button use = new Button(buttons, SWT.PUSH);
		use.setText("Use Path");
		use.setToolTipText("Set the path of the line with the cursor (else the first path found) as the LibreOffice path of the preferences");
		use.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		use.addListener(SWT.Selection, e -> usePath(found));
		Button copy = new Button(buttons, SWT.PUSH);
		copy.setText("Copy");
		copy.setToolTipText("Copy the selected path (or all paths) to the clipboard");
		copy.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		copy.addListener(SWT.Selection, e -> copy());

		hint = new Label(area, SWT.WRAP);
		hint.setText(found.isEmpty() ? "LibreOffice was not found in these folders on this computer. It might be installed elsewhere (select its folder in the preferences)."
				: "Found on this computer: " + String.join(", ", found));
		GridData hd = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		hd.widthHint = 420;
		hint.setLayoutData(hd);
		return area;
	}

	/* Sets the path of the line with the cursor (else the first path found) in the preferences */
	private void usePath(List<String> found) {
		String[] lines = paths.getText().split("\\R");
		int line = paths.getCaretLineNumber();
		/* The cursor stays on its line when the button gets the focus (the first line is a path found, if any) */
		String path = line >= 0 && line < lines.length ? lines[line].trim() : (found.isEmpty() ? "" : found.get(0));
		if (path.isEmpty()) {
			hint.setText("Please click into the line of a path first.");
			return;
		}
		if (!new File(path, executable()).exists()) {
			hint.setText("'" + executable() + "' does not exist in " + path + ".");
			return;
		}
		Bio7Plugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.PATH_LIBREOFFICE, path);
		hint.setText("The LibreOffice path is now " + path + ". Please connect to LibreOffice again.");
		hint.getParent().layout(true);
	}

	/* Copies the selection or, without a selection, all paths! */
	private void copy() {
		String s = paths.getSelectionText();
		if (s == null || s.isEmpty()) {
			s = paths.getText();
		}
		Clipboard cb = new Clipboard(paths.getDisplay());
		try {
			cb.setContents(new Object[] { s }, new Transfer[] { TextTransfer.getInstance() });
		} finally {
			cb.dispose();
		}
	}

	private static String executable() {
		return "Windows".equals(ApplicationWorkbenchWindowAdvisor.getOS()) ? "soffice.exe" : "soffice";
	}

	/* The usual installation folders of LibreOffice for this operating system! */
	private static List<String> suggestions() {
		List<String> list = new ArrayList<String>();
		String os = ApplicationWorkbenchWindowAdvisor.getOS();
		if ("Windows".equals(os)) {
			for (String env : new String[] { "ProgramFiles", "ProgramFiles(x86)", "ProgramW6432" }) {
				String base = System.getenv(env);
				if (base != null) {
					String p = base + "\\LibreOffice\\program";
					if (!list.contains(p)) {
						list.add(p);
					}
				}
			}
			if (list.isEmpty()) {
				list.add("C:\\Program Files\\LibreOffice\\program");
				list.add("C:\\Program Files (x86)\\LibreOffice\\program");
			}
		} else if ("Mac".equals(os)) {
			list.add("/Applications/LibreOffice.app/Contents/MacOS");
			list.add(System.getProperty("user.home") + "/Applications/LibreOffice.app/Contents/MacOS");
		} else {
			list.add("/usr/lib/libreoffice/program");
			list.add("/usr/lib64/libreoffice/program");
			list.add("/opt/libreoffice/program");
			list.add("/snap/libreoffice/current/lib/libreoffice/program");
			list.add("/var/lib/flatpak/app/org.libreoffice.LibreOffice/current/active/files/libreoffice/program");
			list.add(System.getProperty("user.home") + "/.local/share/flatpak/app/org.libreoffice.LibreOffice/current/active/files/libreoffice/program");
			list.add("/usr/bin");
		}
		return list;
	}

	/** The parent shell for the dialog (the workbench window or a new shell). */
	static Shell shell() {
		if (PlatformUI.isWorkbenchRunning() && PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
			return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		}
		return new Shell();
	}
}
