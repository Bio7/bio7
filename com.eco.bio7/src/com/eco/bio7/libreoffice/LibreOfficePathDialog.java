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
		paths = new Text(area, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL);
		paths.setText(text.toString().trim());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 420;
		gd.heightHint = paths.getLineHeight() * Math.min(6, found.size() + other.size()) + 8;
		paths.setLayoutData(gd);

		Button copy = new Button(area, SWT.PUSH);
		copy.setText("Copy");
		copy.setToolTipText("Copy the selected path (or all paths) to the clipboard");
		copy.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		copy.addListener(SWT.Selection, e -> copy());

		Label hint = new Label(area, SWT.WRAP);
		hint.setText(found.isEmpty() ? "None of these folders exists on this computer, LibreOffice might be installed elsewhere."
				: "Found on this computer: " + String.join(", ", found));
		GridData hd = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		hd.widthHint = 420;
		hint.setLayoutData(hd);
		return area;
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
