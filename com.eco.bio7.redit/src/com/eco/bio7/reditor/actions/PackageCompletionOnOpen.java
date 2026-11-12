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

package com.eco.bio7.reditor.actions;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.rosuda.REngine.REXP;

import com.eco.bio7.rbridge.REvaluator;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.antlr.Parse;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.rpreferences.template.CalculateRProposals;

/**
 * Adds the packages of an opened R script (library(), require()) to the code
 * completion, in a background job when R is free, and parses the script again
 * (its markers). Modes (preference {@link #MODE}):
 * <ul>
 * <li>{@link #ATTACH} (default): the packages are attached with library(), as
 * the action "Reload package completion" does</li>
 * <li>{@link #LOAD}: the packages (and their 'Depends') are only loaded
 * (requireNamespace): the search path of the R session doesn't change</li>
 * <li>{@link #OFF}</li>
 * </ul>
 */
public final class PackageCompletionOnOpen {

	/** The preference: what happens with the packages of a script when it is opened. */
	public static final String MODE = "PACKAGES_OF_OPENED_SCRIPT";
	public static final String ATTACH = "ATTACH";
	public static final String LOAD = "LOAD";
	public static final String OFF = "OFF";

	/* A package name (also validates the name for the R code) */
	private static final Pattern PACKAGE_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9.]*");
	/* LOAD: the packages which were already requested (installed or not): not again */
	private static final Set<String> requested = ConcurrentHashMap.newKeySet();
	/* One job at a time (several scripts are opened, e.g., at the start) */
	private static final ISchedulingRule ONE_AT_A_TIME = new ISchedulingRule() {
		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return rule == this;
		}
	};
	/* While R is busy: try again after a delay, at most this often (about 2 minutes) */
	private static final long RETRY_DELAY_MS = 3_000;
	private static final int MAX_ATTEMPTS = 40;
	private static final String PREFIX = "Code completion (opened script): ";

	private PackageCompletionOnOpen() {
	}

	/*
	 * The scripts opened while R was not running (weak: a closed editor is
	 * forgotten); their packages are loaded when R is started
	 */
	private static final Set<Parse> waitingForR = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

	/**
	 * Called when R was started and configured (the R helper of the completion is
	 * defined): the packages of the scripts which were opened before are loaded
	 * now (their current library() and require() calls).
	 */
	public static void rStarted() {
		java.util.List<Parse> scripts;
		synchronized (waitingForR) {
			scripts = new java.util.ArrayList<>(waitingForR);
			waitingForR.clear();
		}
		for (Parse parse : scripts) {
			java.util.Map<String, String> imports = parse.getCurrentPackageImports();
			if (parse.isEditorOpen() && imports != null) {
				scriptOpened(new java.util.ArrayList<>(imports.keySet()), parse);
			}
		}
	}

	/** The selected mode ({@link #ATTACH}, {@link #LOAD} or {@link #OFF}). */
	public static String mode() {
		String m = Bio7REditorPlugin.getDefault().getPreferenceStore().getString(MODE);
		return LOAD.equals(m) || OFF.equals(m) ? m : ATTACH;
	}

	/**
	 * Called with the packages of a script when it was parsed the first time
	 * (opened).
	 *
	 * @param imports the arguments of library() and require() (e.g.,
	 *                "dplyr", "\"dplyr\"", "dplyr,warn.conflicts=FALSE")
	 * @param parse   the parser of the editor (parsed again afterwards) or null
	 */
	public static void scriptOpened(Collection<String> imports, Parse parse) {
		String mode = mode();
		if (imports == null || imports.isEmpty() || OFF.equals(mode)) {
			return;
		}
		Set<String> packages = new LinkedHashSet<>();
		for (String i : imports) {
			String name = packageName(i);
			if (name != null) {
				packages.add(name);
			}
		}
		if (packages.isEmpty()) {
			return;
		}
		/* The proposals the script was parsed with: if they change before this job, it is parsed again */
		long parsedWith = CalculateRProposals.version();
		Job job = new Job("Code completion for the packages of the script") {
			private int attempts;

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				REvaluator c = REditor.getEvaluator();
				if (c == null) {
					/* Caught up when R is started (see rStarted()) */
					if (parse != null) {
						synchronized (waitingForR) {
							waitingForR.add(parse);
						}
					}
					System.out.println(PREFIX + "R is not running, the packages " + packages + " will be loaded when R is started");
					return Status.OK_STATUS;
				}
				if (LOAD.equals(mode)) {
					packages.removeAll(requested);
				}
				if (packages.isEmpty()) {
					reparseIfOutdated();
					return Status.OK_STATUS;
				}
				if (RState.isBusy()) {
					if (++attempts < MAX_ATTEMPTS && !monitor.isCanceled()) {
						schedule(RETRY_DELAY_MS);
					} else {
						System.out.println(PREFIX + "R was busy, the packages " + packages + " were not loaded (use 'Reload package completion' later)");
					}
					return Status.OK_STATUS;
				}
				String vector = rVector(packages);
				boolean changed;
				RState.setBusy(true);
				try {
					if (ATTACH.equals(mode)) {
						changed = attach(c, vector);
					} else {
						changed = load(c, vector);
						if (changed) {
							requested.addAll(packages);
						}
					}
				} catch (Exception e) {
					System.out.println(PREFIX + "the packages " + packages + " could not be loaded: " + e.getMessage());
					return Status.OK_STATUS;
				} finally {
					RState.setBusy(false);
				}
				if (!changed) {
					reparseIfOutdated();
					return Status.OK_STATUS;
				}
				/* Reload the proposals (as the action "Reload package completion") */
				CalculateRProposals.setStartupTemplate(false);
				CalculateRProposals.loadRCodePackageTemplates();
				CalculateRProposals.updateCompletions();
				String[] all = CalculateRProposals.getStatistics();
				System.out.println(PREFIX + (ATTACH.equals(mode) ? "attached " : "loaded ") + packages + ", " + (all == null ? 0 : all.length) + " functions");
				/* The markers of this script with the new proposals */
				if (parse != null) {
					parse.parse();
				}
				return Status.OK_STATUS;
			}

			/*
			 * Nothing new (e.g., already attached, or added by the job of another script): if
			 * the proposals changed after this script was parsed, its markers are outdated
			 */
			private void reparseIfOutdated() {
				if (parse != null && CalculateRProposals.version() != parsedWith) {
					parse.parse();
				}
			}
		};
		job.setSystem(true);
		job.setRule(ONE_AT_A_TIME);
		job.schedule();
	}

	/*
	 * ATTACH: library() for the packages which are not attached yet; then the
	 * function definitions are written. Returns false if none was attached.
	 */
	private static boolean attach(REvaluator c, String vector) throws Exception {
		REXP x = c.eval("local({ p <- " + vector + "; p <- p[!(paste0('package:', p) %in% search())]; "
				+ "ok <- vapply(p, function(n) isTRUE(suppressWarnings(require(n, character.only = TRUE, quietly = TRUE))), logical(1)); "
				+ "as.character(p[ok]) })");
		String[] attached = x == null || x.isNull() ? new String[0] : x.asStrings();
		if (attached == null || attached.length == 0) {
			return false;
		}
		c.eval("try(.bio7WriteFunctionDef())");
		return true;
	}

	/*
	 * LOAD: the packages (and their 'Depends') are only loaded and added to the
	 * function definitions (needs the current R helper).
	 */
	private static boolean load(REvaluator c, String vector) throws Exception {
		REXP current = c.eval("'packages' %in% names(formals(.bio7WriteFunctionDef))");
		if (current == null || current.isNull() || current.asIntegers()[0] != 1) {
			System.out.println(PREFIX + "the R helper is outdated: please restart R (or use the mode 'Attach packages')");
			return false;
		}
		c.eval("try(.bio7WriteFunctionDef(" + vector + "))");
		return true;
	}

	private static String rVector(Set<String> packages) {
		StringBuilder v = new StringBuilder("c(");
		for (String p : packages) {
			v.append(v.length() > 2 ? "," : "").append('"').append(p).append('"');
		}
		return v.append(')').toString();
	}

	/* "dplyr", "\"dplyr\"", "dplyr,warn.conflicts=FALSE" -> dplyr; null if it is no package name */
	private static String packageName(String argument) {
		if (argument == null) {
			return null;
		}
		String name = argument;
		int comma = name.indexOf(',');
		if (comma >= 0) {
			name = name.substring(0, comma);
		}
		name = name.trim();
		if (name.startsWith("package=")) {
			name = name.substring("package=".length()).trim();
		}
		if (name.length() >= 2 && (name.charAt(0) == '"' || name.charAt(0) == '\'') && name.charAt(name.length() - 1) == name.charAt(0)) {
			name = name.substring(1, name.length() - 1);
		}
		return PACKAGE_NAME.matcher(name).matches() ? name : null;
	}
}
