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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.ListDialog;

import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.GdalLibraryFinder;
import com.eco.bio7.gdal.GdalProbe;

/** The preferences of GDAL: activation, loading at startup, the library, its test and the installation. */
public class GdalPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private FileFieldEditor libraryField;
	private Label status;

	public GdalPreferencePage() {
		super(GRID);
		setPreferenceStore(GdalPreferences.store());
		setDescription("GDAL (raster and vector formats, coordinate systems) of an installation on this computer, called with the Foreign Function and Memory API.\n"
				+ "GDAL is only loaded if it is activated; a new library is tested first in a separate process.");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
		addField(new BooleanFieldEditor(GdalPreferences.ENABLED, "Activate GDAL (loaded when it is used)", parent));
		addField(new BooleanFieldEditor(GdalPreferences.LOAD_AT_STARTUP, "Load GDAL at startup (in the background)", parent));
		libraryField = new FileFieldEditor(GdalPreferences.LIBRARY, "GDAL library (empty: automatic search):", parent);
		addField(libraryField);

		Composite buttons = new Composite(parent, SWT.NONE);
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));
		buttons.setLayout(new GridLayout(3, false));
		button(buttons, "Detect...", "Search the GDAL libraries of this computer (conda, Homebrew, the packages of Linux, QGIS, OSGeo4W, also installed for the current user)", this::detect);
		button(buttons, "Test", "Test the library in a separate process (without loading it into the application)", this::test);
		button(buttons, "Install GDAL...", "Install GDAL with a package manager", () -> GdalInstaller.open(getShell()));

		status = new Label(parent, SWT.WRAP);
		status.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1));
		updateStatus();
	}

	private static void button(Composite parent, String text, String tip, Runnable action) {
		Button b = new Button(parent, SWT.PUSH);
		b.setText(text);
		b.setToolTipText(tip);
		b.addListener(SWT.Selection, _ -> action.run());
	}

	private void updateStatus() {
		Path lib;
		String text;
		if (Gdal.isLoaded()) {
			lib = Gdal.library();
			text = "Loaded: GDAL " + Gdal.version() + "\n" + lib;
		} else {
			lib = GdalActivation.library();
			text = "Not loaded. " + (lib != null ? "Library: " + lib : "No GDAL library was found.");
		}
		if (lib != null) {
			text += "\n\n" + datumHint(lib);
		}
		status.setText(text);
		status.getParent().layout(true, true);
	}

	/*
	 * The datum grids of PROJ: without them datum transformations (e.g., NAD27 or
	 * DHDN to WGS84) use approximate parameters (about 10 m, partly more)
	 */
	static String datumHint(Path lib) {
		Path proj = GdalLibraryFinder.projData(lib);
		Path user = GdalLibraryFinder.userProjData();
		int n = GdalLibraryFinder.datumGrids(proj, user).size();
		if (n > 0) {
			return "Datum grids of PROJ: " + n + " (exact datum transformations, about 1 m).";
		}
		String install = GdalLibraryFinder.WINDOWS && lib.toString().toLowerCase(java.util.Locale.ROOT).contains("osgeo4w")
				? "the package 'proj-data' in the OSGeo4W installer (Advanced Install, Libs)"
				: lib.toString().toLowerCase(java.util.Locale.ROOT).contains("conda") || lib.toString().toLowerCase(java.util.Locale.ROOT).contains("forge")
						? "the conda package 'proj-data' (conda install -c conda-forge proj-data)"
						: "the package of the datum grids of PROJ (e.g., 'proj-data')";
		return "No datum grids of PROJ found: datum transformations (e.g., NAD27 or DHDN to WGS84) use approximate parameters "
				+ "(about 10 m, partly more). For exact transformations install " + install
				+ ", or download single grids from https://cdn.proj.org (e.g., us_noaa_conus.tif for NAD27 in the USA) into\n" + user;
	}

	/* The libraries of this computer: one is selected (tested in a separate process) */
	private void detect() {
		List<Path> found = new ArrayList<>();
		BusyIndicator.showWhile(getShell().getDisplay(), () -> found.addAll(GdalLibraryFinder.candidates()));
		if (found.isEmpty()) {
			if (MessageDialog.openQuestion(getShell(), "GDAL", "No GDAL library was found on this computer.\n\nInstall GDAL?")) {
				GdalInstaller.open(getShell());
			}
			return;
		}
		ListDialog dialog = new ListDialog(getShell());
		dialog.setTitle("GDAL Libraries");
		dialog.setMessage("Select the GDAL library (it is tested in a separate process):");
		dialog.setContentProvider(ArrayContentProvider.getInstance());
		dialog.setLabelProvider(new LabelProvider());
		dialog.setInput(found);
		dialog.setInitialSelections(found.get(0));
		dialog.setWidthInChars(80);
		if (dialog.open() != Window.OK || dialog.getResult() == null || dialog.getResult().length == 0) {
			return;
		}
		Path lib = (Path) dialog.getResult()[0];
		GdalProbe.Result r = probe(lib);
		if (r.ok()) {
			libraryField.setStringValue(lib.toString());
			GdalPreferences.store().setValue(GdalPreferences.TESTED, lib.toAbsolutePath().normalize().toString());
			MessageDialog.openInformation(getShell(), "GDAL", "GDAL " + r.version() + " works:\n" + lib + "\n\nPress 'Apply' to use it.");
		} else {
			MessageDialog.openError(getShell(), "GDAL", "The library doesn't work:\n" + lib + "\n\n" + r.message());
		}
	}

	/* Tests the library of the field (or the automatic search) */
	private void test() {
		String s = libraryField.getStringValue().strip();
		Path lib = s.isEmpty() ? GdalLibraryFinder.find().orElse(null) : GdalLibraryFinder.resolve(Path.of(s));
		if (lib == null) {
			MessageDialog.openInformation(getShell(), "GDAL", "No GDAL library " + (s.isEmpty() ? "was found." : "at " + s));
			return;
		}
		GdalProbe.Result r = probe(lib);
		if (r.ok()) {
			GdalPreferences.store().setValue(GdalPreferences.TESTED, lib.toAbsolutePath().normalize().toString());
			MessageDialog.openInformation(getShell(), "GDAL", "GDAL " + r.version() + " works:\n" + lib);
		} else {
			MessageDialog.openError(getShell(), "GDAL", "The library doesn't work:\n" + lib + "\n\n" + r.message());
		}
	}

	private GdalProbe.Result probe(Path lib) {
		GdalProbe.Result[] r = new GdalProbe.Result[1];
		BusyIndicator.showWhile(getShell().getDisplay(), () -> r[0] = GdalProbe.test(lib));
		return r[0];
	}

	@Override
	public boolean performOk() {
		String before = GdalPreferences.store().getString(GdalPreferences.LIBRARY);
		boolean ok = super.performOk();
		GdalPreferences.save();
		String after = GdalPreferences.store().getString(GdalPreferences.LIBRARY);
		if (Gdal.isLoaded() && !before.equals(after)) {
			MessageDialog.openInformation(getShell(), "GDAL", "GDAL is loaded (" + Gdal.library() + ").\n\nThe selected library is used after a restart.");
		}
		return ok;
	}
}
