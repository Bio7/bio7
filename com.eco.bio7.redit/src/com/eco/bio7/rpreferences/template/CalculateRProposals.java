package com.eco.bio7.rpreferences.template;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import static com.eco.bio7.rpreferences.template.RCompletionProcessor.*;

/*In this class we calculate the R proposals for the R editor which will be added after the templates.
 *If a new package is loaded by means of the Bio7 R GUI interface a new proposal file will be created
 *which includes the proposals (help file excerpts) for the package. The proposals are calculated with 
 *an R script 'calculateRCompletion.R' and stored in the Bio7 temporary directory of the main Bio7 plugin!
 *At startup the default proposals are used!*/

public class CalculateRProposals {

	private static String[] statistics;

	private static String[] statisticsContext;

	private static String[] statisticsSet;

	private static boolean startupTemplate = true;

	/* Published complete (a parser in another thread never sees a half loaded list) */
	public static volatile HashMap<Integer, String> stat;

	private static HashMap<Integer, String> statContext;

	private static HashMap<Integer, String> statSet;

	/* The function names of the proposals for a fast lookup (the parser checks each call) */
	private static volatile java.util.Set<String> functionNames;

	/* Incremented with each (re)load of the proposals */
	private static final java.util.concurrent.atomic.AtomicLong version = new java.util.concurrent.atomic.AtomicLong();

	/**
	 * Returns true if a function of the loaded packages has this name, false if
	 * not, null if no proposals are loaded yet.
	 */
	private static final java.util.List<Runnable> RELOAD_LISTENERS = new java.util.concurrent.CopyOnWriteArrayList<>();

	/** Called (in the thread of the reload) after the proposals were reloaded (e.g., the functions of a loaded package). */
	public static void addReloadListener(Runnable listener) {
		RELOAD_LISTENERS.add(listener);
	}

	public static void removeReloadListener(Runnable listener) {
		RELOAD_LISTENERS.remove(listener);
	}

	private static volatile java.util.Set<String> knownPackages;
	private static volatile long knownPackagesVersion = -1;

	/**
	 * The packages of the current proposals (e.g., "ggplot2"): a package attached
	 * in R which is not among them is missing in the code completion.
	 */
	public static java.util.Set<String> knownPackages() {
		long v = version.get();
		java.util.Set<String> known = knownPackages;
		if (known != null && knownPackagesVersion == v) {
			return known;
		}
		java.util.Set<String> set = new java.util.HashSet<>();
		HashMap<Integer, String> context = statContext;
		if (context != null) {
			java.util.regex.Pattern p = java.util.regex.Pattern.compile("package:([A-Za-z0-9._]+)");
			for (String c : context.values()) {
				if (c != null) {
					java.util.regex.Matcher m = p.matcher(c);
					if (m.find()) {
						set.add(m.group(1));
					}
				}
			}
		}
		knownPackages = set;
		knownPackagesVersion = v;
		return set;
	}

	public static Boolean isKnownFunction(String name) {
		java.util.Set<String> names = functionNames;
		return names == null ? null : names.contains(name);
	}

	/** The number of the current proposals (changes with each reload). */
	public static long version() {
		return version.get();
	}

	public static String[] getStatistics() {
		return statistics;
	}

	public static String[] getStatisticsSet() {
		return statisticsSet;
	}

	public static String[] getStatisticsContext() {
		return statisticsContext;
	}

	/*
	 * Loaded into new lists which replace the current ones when they are complete
	 * (synchronized: one load at a time)
	 */
	public static synchronized void loadRCodePackageTemplates() {
		// IPreferenceStore s = new ScopedPreferenceStore(InstanceScope.INSTANCE,
		// "com.eco.bio7");
		/* Temporary lists to load the proposals! */
		/* The HashMap is choosen for a fast lookup of the R parser! */
		Bundle bundle = Platform.getBundle("com.eco.bio7");
		HashMap<Integer, String> stat = new HashMap<Integer, String>();
		HashMap<Integer, String> statContext = new HashMap<Integer, String>();
		HashMap<Integer, String> statSet = new HashMap<Integer, String>();
		String tempPath;
		/* Load the new calculated templates else use the default templates! */
		if (startupTemplate == false) {
			URL url = FileLocator.find(bundle, new Path("rcompletion/rproposals.txt"), null);		
			File file = null;			
			try {
				URL fileUrl = FileLocator.toFileURL(url);
				
				file = new File(fileUrl.getPath());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			//URL fileURL = bundle.getEntry("rcompletion/rproposals.txt");
			//File file = null;
			/*
			 * try { //See:
			 * https://stackoverflow.com/questions/4571346/how-to-encode-url-to-avoid-
			 * special-characters-in-java/4571518#4571518 URL url =
			 * FileLocator.resolve(fileURL); URI uri = new URI(url.getProtocol(),
			 * url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
			 * url.getQuery(), url.getRef()); file = new File(uri.toASCIIString()); } catch
			 * (URISyntaxException e1) { e1.printStackTrace(); } catch (IOException e1) {
			 * e1.printStackTrace(); }
			 */
			tempPath = file.getAbsolutePath();

			// tempPath = s.getString("pathTempR") + "rproposals.txt";
		} else {
			URL url = FileLocator.find(bundle, new Path("rcompletion/rproposalsDefault.txt"), null);		
			File file = null;			
			try {
				URL fileUrl = FileLocator.toFileURL(url);
				
				file = new File(fileUrl.getPath());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			/*
			 * URL fileURL = bundle.getEntry("rcompletion/rproposalsDefault.txt"); tempPath
			 * = fileURL.toExternalForm(); File file = null; //See:
			 * https://stackoverflow.com/questions/4571346/how-to-encode-url-to-avoid-
			 * special-characters-in-java/4571518#4571518 try { URL url =
			 * FileLocator.resolve(fileURL); URI uri = new URI(url.getProtocol(),
			 * url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
			 * url.getQuery(), url.getRef());
			 * 
			 * file = new File(uri.toASCIIString()); } catch (URISyntaxException e1) {
			 * e1.printStackTrace(); } catch (IOException e1) { e1.printStackTrace(); }
			 */
			tempPath = file.getAbsolutePath();

			// tempPath = s.getString("pathTempR") + "rproposalsDefault.txt";
		}
		tempPath = tempPath.replace("\\", "/");
		BufferedReader br = null;
		try {
			File rPropFile = new File(tempPath);
			if (rPropFile.exists()) {

				br = new BufferedReader(new FileReader(rPropFile));
			}
		} catch (FileNotFoundException e) {
			System.out.println("The proposals file in the temporary directory can't be found!");
			e.printStackTrace();
		}
		if (br == null) {
			System.out.println(
					"Proposals file could not be loaded!\nPlease move the file 'rproposals.txt'\nto the 'Path to temporary R folder' (see R preferences)!");
			return;
		}
		String line;
		int count = 0;
		try {
			while ((line = br.readLine()) != null) {
				/* Split the string to get the seperated values! */
				String[] theline = line.split("####");
				// System.out.println(theline.length);

				try {
					stat.put(count, theline[0]);
					statContext.put(count, theline[1]);
					statSet.put(count, theline[2]);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

				}
				count++;
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/* Create arrays! */
		String[] statistics = new String[stat.size()];
		String[] statisticsContext = new String[statContext.size()];
		String[] statisticsSet = new String[stat.size()];
		/* Copy values from HashMap to array! */
		for (int i = 0; i < stat.size(); i++) {
			statistics[i] = stat.get(i);
			statisticsContext[i] = statContext.get(i);
			statisticsSet[i] = statSet.get(i);
		}
		/* Now the complete lists replace the current ones */
		CalculateRProposals.statistics = statistics;
		CalculateRProposals.statisticsContext = statisticsContext;
		CalculateRProposals.statisticsSet = statisticsSet;
		CalculateRProposals.statContext = statContext;
		CalculateRProposals.statSet = statSet;
		CalculateRProposals.functionNames = new java.util.HashSet<>(stat.values());
		CalculateRProposals.stat = stat;
		version.incrementAndGet();
		/* E.g., the R analysis of markdown documents is updated */
		for (Runnable r : RELOAD_LISTENERS) {
			try {
				r.run();
			} catch (RuntimeException e) {
				/* The next one */
			}
		}

		// stat = null;
		// statContext = null;
		// statSet = null;

	}

	public static void setStartupTemplate(boolean startupTemplates) {
		startupTemplate = startupTemplates;
	}

	public static void updateCompletions() {
		setStatistics(getStatistics());
		setStatisticsContext(getStatisticsContext());
		setStatisticsSet(getStatisticsSet());

	}
}
