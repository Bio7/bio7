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

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.GdalException;
import com.eco.bio7.gdal.GdalLibraryFinder;
import com.eco.bio7.gdal.GdalProbe;

/**
 * Loads GDAL for the user interface: only if it is enabled in the preferences
 * (else the user is asked), a new library is first tested in a separate
 * process (a library which crashes doesn't end the application).
 */
public final class GdalActivation {

	private GdalActivation() {
	}

	/**
	 * Makes sure that GDAL is loaded (UI thread): asks to enable it, tests a new
	 * library, shows the reason if it fails.
	 *
	 * @return true if GDAL is loaded
	 */
	public static boolean ensureLoaded(Shell shell) {
		if (Gdal.isLoaded()) {
			return true;
		}
		if (!GdalPreferences.isEnabled()) {
			int answer = new MessageDialog(shell, "GDAL", null,
					"GDAL is not activated.\n\nGDAL is loaded from an installation of GDAL on this computer (it is tested first in a separate process).",
					MessageDialog.QUESTION, 0, "Activate GDAL", "Preferences...", "Cancel").open();
			if (answer == 1) {
				openPreferences(shell);
				return Gdal.isLoaded();
			}
			if (answer != 0) {
				return false;
			}
			GdalPreferences.store().setValue(GdalPreferences.ENABLED, true);
			GdalPreferences.save();
		}
		java.util.List<Path> libs = libraries();
		if (libs.isEmpty()) {
			int answer = new MessageDialog(shell, "GDAL", null,
					"No GDAL library was found on this computer.\n\nPlease install GDAL (conda, Homebrew, the packages of Linux, OSGeo4W) or select the library in the preferences.",
					MessageDialog.WARNING, 0, "Install GDAL...", "Preferences...", "Cancel").open();
			if (answer == 0) {
				GdalInstaller.open(shell);
			} else if (answer == 1) {
				openPreferences(shell);
			}
			return Gdal.isLoaded();
		}
		String error = load(shell, libs);
		if (error != null) {
			int answer = new MessageDialog(shell, "GDAL", null, error, MessageDialog.ERROR, 0, "Preferences...", "Close").open();
			if (answer == 0) {
				openPreferences(shell);
			}
			return Gdal.isLoaded();
		}
		return true;
	}

	/* Tests the libraries (progress dialog), then loads the first which works: null or the reasons */
	private static String load(Shell shell, java.util.List<Path> libs) {
		String[] error = new String[1];
		try {
			new ProgressMonitorDialog(shell).run(true, false, monitor -> {
				monitor.beginTask("Loading GDAL ...", IProgressMonitor.UNKNOWN);
				error[0] = loadFirstWorking(libs);
				monitor.done();
			});
		} catch (InvocationTargetException | InterruptedException e) {
			return "GDAL could not be loaded: " + e;
		}
		return error[0];
	}

	/**
	 * Loads the library (not in the UI thread): tested first in a separate process
	 * if it didn't pass the test before. Null or the reason.
	 */
	public static String loadTested(Path lib) {
		if (Gdal.isLoaded()) {
			return null;
		}
		String key = lib.toAbsolutePath().normalize().toString();
		if (!key.equals(GdalPreferences.store().getString(GdalPreferences.TESTED))) {
			GdalProbe.Result r = GdalProbe.test(lib);
			if (!r.ok()) {
				return "The GDAL library didn't pass the test (it is not loaded into the application):\n" + lib + "\n\n" + r.message();
			}
			GdalPreferences.store().setValue(GdalPreferences.TESTED, key);
			GdalPreferences.save();
		}
		try {
			Gdal.load(lib);
			return null;
		} catch (GdalException e) {
			return e.getMessage();
		}
	}

	/**
	 * The libraries to try: the library of the preferences, else all libraries of
	 * the automatic search (the first which passes the test is loaded).
	 */
	public static java.util.List<Path> libraries() {
		Path pref = GdalPreferences.library();
		if (pref != null) {
			Path lib = GdalLibraryFinder.resolve(pref);
			return lib == null ? java.util.List.of() : java.util.List.of(lib);
		}
		return GdalLibraryFinder.candidates();
	}

	/**
	 * Loads the first library which passes the test (not in the UI thread): null,
	 * or the reasons of all libraries.
	 */
	public static String loadFirstWorking(java.util.List<Path> libs) {
		StringBuilder reasons = new StringBuilder();
		for (Path lib : libs) {
			String error = loadTested(lib);
			if (error == null) {
				return null;
			}
			reasons.append(reasons.length() > 0 ? "\n\n" : "").append(error);
		}
		return libs.size() == 1 ? reasons.toString() : "No GDAL library of this computer works:\n\n" + reasons;
	}

	/** The library of the preferences or of the automatic search (null if none). */
	public static Path library() {
		Path pref = GdalPreferences.library();
		if (pref != null) {
			return GdalLibraryFinder.resolve(pref);
		}
		return GdalLibraryFinder.find().orElse(null);
	}

	/** Loads GDAL in a background job if it is enabled and to be loaded at startup (the startup of the workbench). */
	public static void loadAtStartup() {
		if (!GdalPreferences.isEnabled() || !GdalPreferences.store().getBoolean(GdalPreferences.LOAD_AT_STARTUP) || Gdal.isLoaded()) {
			return;
		}
		Job job = new Job("Loading GDAL") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				java.util.List<Path> libs = libraries();
				if (libs.isEmpty()) {
					return new Status(IStatus.WARNING, GdalPreferences.PLUGIN_ID, "GDAL: no library found (see the GDAL preferences)");
				}
				String error = loadFirstWorking(libs);
				if (error != null) {
					return new Status(IStatus.WARNING, GdalPreferences.PLUGIN_ID, "GDAL was not loaded: " + error);
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(false);
		job.schedule();
	}

	public static void openPreferences(Shell shell) {
		PreferencesUtil.createPreferenceDialogOn(shell, GdalPreferences.PAGE_ID, null, null).open();
	}
}
