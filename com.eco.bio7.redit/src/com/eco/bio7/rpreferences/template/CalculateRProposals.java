package com.eco.bio7.rpreferences.template;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/*In this class we calculate the R proposals for the R editor which will be added after the templates.
 *If a new package is loaded by means of the Bio7 R GUI interface a new proposal file will be created
 *which includes the proposals (help file excerpts) for the package. The proposals are calculated with 
 *an R script 'calculateRCompletion.R' and stored in the Bio7 temporary directory of the main Bio7 plugin!
 *At startup the the default proposals are used!*/

public class CalculateRProposals {

	public static String[] statistics;

	public static String[] statisticsContext;

	public static String[] statisticsSet;

	private static boolean startupTemplate = true;

	public static HashMap<Integer, String> stat;

	private static HashMap<Integer, String> statContext;

	private static HashMap<Integer, String> statSet;

	public static void loadRCodePackageTemplates() {
		IPreferenceStore s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "com.eco.bio7");
		/* Temporary lists to load the proposals! */
		/*The HashMap is choosen for a fast lookup of the R parser!*/
		stat = new HashMap<Integer, String>();
		statContext = new HashMap<Integer, String>();
		statSet = new HashMap<Integer, String>();
		String tempPath;
		/* Load the new calculated templates else use the default templates! */
		if (startupTemplate == false) {
			tempPath = s.getString("pathTempR") + "rproposals.txt";
		} else {
			tempPath = s.getString("pathTempR") + "rproposalsDefault.txt";
		}
		tempPath = tempPath.replace("\\", "/");
		// System.out.println(tempPath);
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
       /*Create arrays!*/
		statistics = new String[stat.size()];
		statisticsContext = new String[statContext.size()];
		statisticsSet = new String[stat.size()];
        /*Copy values from HashMap to array!*/
		for (int i = 0; i < stat.size(); i++) {
			statistics[i] = stat.get(i);
			statisticsContext[i] = statContext.get(i);
			statisticsSet[i] = statSet.get(i);
		}

		// stat = null;
		// statContext = null;
		// statSet = null;

	}

	public static void setStartupTemplate(boolean startupTemplates) {
		startupTemplate = startupTemplates;
	}
}
