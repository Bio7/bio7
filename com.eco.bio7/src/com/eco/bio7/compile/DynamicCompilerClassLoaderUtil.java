package com.eco.bio7.compile;

import org.eclipse.jface.preference.IPreferenceStore;
import com.eco.bio7.javaeditor.Bio7EditorPlugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for creating a URLClassLoader from the Bio7 "dynamic compiler libraries" preference.
 */
public class DynamicCompilerClassLoaderUtil {

    /**
     * Get the dynamic compiler library paths as an ordered list from preferences.
     * Libraries are stored as a semicolon-separated string.
     */
    public static List<String> getDynamicCompilerLibraries() {
        IPreferenceStore store = Bio7EditorPlugin.getDefault().getPreferenceStore();
        String libsPref = store.getString("JAVA_LIBS"); // use your actual pref key!
        List<String> jarPaths = new ArrayList<>();
        if (libsPref != null && !libsPref.trim().isEmpty()) {
            String[] parts = libsPref.split(";");
            for (String path : parts) {
                path = path.trim();
                if (!path.isEmpty()) {
                    jarPaths.add(path);
                }
            }
        }
        return jarPaths;
    }

    /**
     * Build a URLClassLoader from a list of jar paths and a parent class loader.
     */
    public static URLClassLoader buildLibraryClassLoader(List<String> jarFilePaths, ClassLoader parent) throws Exception {
        URL[] jarURLs = new URL[jarFilePaths.size()];
        for (int i = 0; i < jarFilePaths.size(); i++) {
            jarURLs[i] = new java.io.File(jarFilePaths.get(i)).toURI().toURL();
        }
        return new URLClassLoader(jarURLs, parent);
    }
}