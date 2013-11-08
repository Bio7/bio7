package com.eco.bio7.batch;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jface.preference.IPreferenceStore;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.floweditor.shapes.ShapesPlugin;
import com.eco.bio7.preferences.PreferenceConstants;

/**
 * This class provides static methods to get the Workspace, R, OpenOffice and
 * script path location of the Bio7 application.
 * 
 * @author Bio7
 * 
 */
public class FileRoot {

	/**
	 * Returns the path of the workspace location of the Bio7 application.
	 * 
	 * @return the path as a string.
	 */
	public static String getFileRoot() {
		IWorkspaceRoot root = ShapesPlugin.getWorkspace().getRoot();
		String fileroot = root.getLocation().toString();// get the absolute path
		return fileroot;
	}

	/**
	 * Returns the R path from the preferences.
	 * 
	 * @return the path as a string.
	 */
	public static String getRLocation() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String fileroot = store.getString(PreferenceConstants.PATH_R);
		return fileroot;

	}

	/**
	 * Returns the OpenOffice path from the preferences.
	 * 
	 * @return the path as a string.
	 */
	public static String getOOLocation() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String fileroot = store.getString(PreferenceConstants.PATH_LIBREOFFICE);
		return fileroot;

	}

	/**
	 * Returns the statistic script path from the preferences.
	 * 
	 * @return the path as a string.
	 */
	public static String getStatisticScriptLocation() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String fileroot = store.getString(PreferenceConstants.D_SCRIPT_SPATIAL);
		return fileroot;

	}

	/**
	 * Returns the general script path from the preferences.
	 * 
	 * @return the path as a string.
	 * 
	 */
	public static String getGeneralScriptLocation() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String fileroot = store.getString(PreferenceConstants.D_SCRIPT_GENERAL);
		return fileroot;

	}

	/**
	 * Returns the image script path from the preferences.
	 * 
	 * @return the path as a string.
	 */
	public static String getImageScriptLocation() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String fileroot = store.getString(PreferenceConstants.D_SCRIPT_IMAGE);
		return fileroot;

	}

	/**
	 * Returns the R script path from the preferences.
	 * 
	 * @return the path as a string.
	 */
	public static String getRScriptLocation() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String fileroot = store.getString(PreferenceConstants.D_SCRIPT_R);
		return fileroot;

	}

	/**
	 * Returns the import script path from the preferences.
	 * 
	 * @return the path as a string.
	 * 
	 */
	public static String getImportScriptLocation() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String fileroot = store.getString(PreferenceConstants.D_IMPORT);
		return fileroot;

	}

	/**
	 * Returns the export script path from the preferences.
	 * 
	 * @return the path as a string.
	 * 
	 */
	public static String getExportScriptLocation() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String fileroot = store.getString(PreferenceConstants.D_EXPORT);
		return fileroot;

	}

	/**
	 * Returns the startup script path from the preferences.
	 * 
	 * @return the path as a string.
	 * 
	 */
	public static String getStartupScriptLocation() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String fileroot = store.getString(PreferenceConstants.D_STRING);
		return fileroot;

	}
	/**
	 * Returns the grid script path from the preferences.
	 * 
	 * @return the path as a string.
	 * 
	 */
	public static String getGridScriptLocation() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String fileroot = store.getString(PreferenceConstants.D_GRID_SCRIPTS);
		return fileroot;

	}
	
	/**
	 * Returns the R shell script path from the preferences.
	 * 
	 * @return the path as a string.
	 * 
	 */
	public static String getRShellScriptLocation() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String fileroot = store.getString(PreferenceConstants.D_RSHELL_SCRIPTS);
		return fileroot;

	}

}
