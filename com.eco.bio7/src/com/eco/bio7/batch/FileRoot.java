package com.eco.bio7.batch;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
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

	private static String currentCompileDir;

	/**
	 * A method to get the current compile file directory. The directory is set
	 * before the compilation process.
	 * 
	 * @return the directory of the current file which will be compiled.
	 */
	public static String getCurrentCompileDir() {
		return currentCompileDir;
	}

	/**
	 * Sets the current compile directory. For internal use.
	 * 
	 * @param currentCompileDir
	 */
	public static void setCurrentCompileDir(String currentCompileDir) {
		FileRoot.currentCompileDir = currentCompileDir;
	}

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
	/**
	 * Returns the workspace path.
	 * 
	 * @return the path as a string.
	 * 
	 */
	public static String getWorkspacePath() {
		String path = null;
		path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
		return path;
	}

}
