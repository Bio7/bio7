package com.eco.bio7.rpreferences.template;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
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

	public static void loadRCodePackageTemplates() {
		IPreferenceStore s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "com.eco.bio7");
		/* Temporary lists to load the proposals! */
		ArrayList<String> dataTemp1 = new ArrayList<String>();
		ArrayList<String> dataTemp2 = new ArrayList<String>();
		ArrayList<String> dataTemp3 = new ArrayList<String>();
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
		try {
			while ((line = br.readLine()) != null) {
				/* Split the string to get the seperated values! */
				String[] theline = line.split("####");
				// System.out.println(theline.length);

				try {
					dataTemp1.add(theline[0]);
					dataTemp2.add(theline[1]);
					dataTemp3.add(theline[2]);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

				}

			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (dataTemp1.size() == dataTemp2.size() && dataTemp1.size() == dataTemp3.size()) {
			statistics = dataTemp1.toArray(new String[dataTemp1.size()]);
			statisticsContext = dataTemp2.toArray(new String[dataTemp2.size()]);
			statisticsSet = dataTemp3.toArray(new String[dataTemp3.size()]);
		} else {
			statistics = new String[] { "Error in Template file!" };
			statisticsContext = new String[] { "Error in Template file!" };
			statisticsSet = new String[] { "Error in Template file!" };
		}
		dataTemp1 = null;
		dataTemp2 = null;
		dataTemp3 = null;

	}

	public static void setStartupTemplate(boolean startupTemplates) {
		startupTemplate = startupTemplates;
	}
}
