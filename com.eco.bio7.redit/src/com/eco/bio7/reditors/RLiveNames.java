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
package com.eco.bio7.reditors;

import java.util.Set;

/**
 * The names which exist in R (the workspace and the attached packages, e.g.,
 * pi, letters, iris or a variable created in the console) for the code analysis
 * of the R editor: such a name is not marked as a missing variable. Updated in
 * the background by Bio7 (embedded R, after an evaluation); the parser only
 * looks into the set and never waits for R. Without R a list of base names is
 * used.
 */
public final class RLiveNames {

	/* Base constants and datasets (R not connected) */
	private static final Set<String> BASE = Set.of("pi", "letters", "LETTERS", "month.name", "month.abb", "T", "F", ".Machine",
			".Platform", "R.version", "R.version.string", "version", ".GlobalEnv", ".libPaths", ".Last.value", "iris", "iris3", "mtcars", "airquality",
			"cars", "faithful", "ToothGrowth", "PlantGrowth", "trees", "women", "quakes", "volcano", "precip", "rivers", "islands", "state.name",
			"state.abb", "warpbreaks", "chickwts", "ChickWeight", "esoph", "infert", "sleep", "swiss", "USArrests", "attitude", "stackloss",
			"anscombe", "Titanic", "HairEyeColor", "UCBAdmissions", "AirPassengers", "Nile", "co2", "lynx", "LakeHuron", "sunspots", "pressure",
			"beaver1", "beaver2", "longley", "morley", "mdeaths", "fdeaths", "ldeaths", "nottem", "Orange", "OrchardSprays", "rock",
			"InsectSprays", "Loblolly", "Indometh", "Puromycin", "Theoph", "CO2", "DNase", "BOD", "Formaldehyde", "freeny", "eurodist",
			"euro", "UKgas", "USAccDeaths", "WorldPhones", "VADeaths", "uspop", "presidents", "discoveries", "nhtemp", "lh", "randu");

	/* The API of Bio7 for R (attached environment "bio7" of the embedded R, see bio7_r_api.R) */
	private static final Set<String> BIO7_API = Set.of("bio7_image", "bio7_show_image", "bio7_ij_macro", "bio7_ij_macro_file", "bio7_ij_run",
			"bio7_results_table", "bio7_roi", "bio7_browse", "bio7_python", "bio7_to_python", "J", "bio7_java_new", "bio7_java_ui", "is_java",
			"bio7_api");

	private static volatile Set<String> names;

	private RLiveNames() {
	}

	private static final java.util.List<Runnable> LISTENERS = new java.util.concurrent.CopyOnWriteArrayList<>();

	/** Called (in a background thread) when the names of R changed (e.g., a package was attached). */
	public static void addListener(Runnable listener) {
		LISTENERS.add(listener);
	}

	public static void removeListener(Runnable listener) {
		LISTENERS.remove(listener);
	}

	/** The names of R (null: R is not connected, the base names are used). */
	public static void set(Set<String> live) {
		Set<String> old = names;
		names = live;
		if (old == null ? live != null : !old.equals(live)) {
			for (Runnable r : LISTENERS) {
				try {
					r.run();
				} catch (RuntimeException e) {
					/* The next one */
				}
			}
		}
	}

	/** Returns true if the name exists in R (or is a base name if R is not connected). */
	public static boolean exists(String name) {
		Set<String> live = names;
		return live != null ? live.contains(name) || BASE.contains(name) || BIO7_API.contains(name) : BASE.contains(name) || BIO7_API.contains(name);
	}
}
