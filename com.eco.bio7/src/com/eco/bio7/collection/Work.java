/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.actions.ActionFactory;

import com.eco.bio7.editor.BeanshellEditorPlugin;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import com.eco.bio7.markdownedit.Activator;
import com.eco.bio7.pythonedit.PythonEditorPlugin;
import com.eco.bio7.reditor.Bio7REditorPlugin;

/**
 * This class offers some methods for the control of the views, perspectives and
 * the editor area of the Bio7 application.
 * 
 * 
 * @author Bio7
 * 
 *
 */
public class Work {

	/**
	 * Opens the view with the specified id.
	 * 
	 * @param id the id as a string value.
	 */
	public static void openView(final String id) {

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				try {
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					page.showView(id);
				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});

	}

	/**
	 * Opens the perspective with the specified id.
	 * 
	 * @param id the id as a string value.
	 */
	public static void openPerspective(final String id) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {

				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				try {
					PlatformUI.getWorkbench().showPerspective(id, window);
				} catch (WorkbenchException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});

	}

	/**
	 * Opens the specified view and maximizes it.
	 * 
	 * @param id the id as a string value.
	 */
	public static void openMaxView(final String id) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				try {
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					page.showView(id);
					IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					ActionFactory.IWorkbenchAction maximize = ActionFactory.MAXIMIZE.create(window);
					maximize.run();

					maximize.dispose();
				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});

	}

	/**
	 * Shows or hides the editor area.
	 */
	public static void showHide() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {

				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				ActionFactory.IWorkbenchAction maximize = ActionFactory.SHOW_EDITOR.create(window);
				maximize.run();

				maximize.dispose();
			}

		});

	}

	/**
	 * Closes the view with the specified id.
	 * 
	 * @param id the id as a string value.
	 */
	public static void closeView(final String id) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				IWorkbenchPage wbp = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

				wbp.hideView(wbp.findView(id));

			}

		});

	}

	/**
	 * Closes the current perspective.
	 */
	public static void closeCurrentPerspective() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {

				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				ActionFactory.IWorkbenchAction pclose = ActionFactory.CLOSE_PERSPECTIVE.create(window);
				pclose.run();

				pclose.dispose();
			}

		});

	}

	/**
	 * Refreshes all workspaces.
	 */
	public static void refreshAllWorkspaces() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			try {
				projects[i].refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * A method to get the preferences store for different custom Bio7 editors.
	 * 
	 * @param editor A string value to get the store (value options = r, groovy,
	 *               ijmacro, python, markdown).
	 * @return the preference store of the given editor.
	 */
	public static IPreferenceStore getEditorStore(String editor) {
		IPreferenceStore store = null;
		switch (editor) {
		case "r":
			store = Bio7REditorPlugin.getDefault().getPreferenceStore();
			break;
		case "groovy":
			store = BeanshellEditorPlugin.getDefault().getPreferenceStore();
			break;
		case "ijmacro":
			store = IJMacroEditorPlugin.getDefault().getPreferenceStore();
			break;
		case "python":
			store = PythonEditorPlugin.getDefault().getPreferenceStore();
			break;
		case "markdown":
			store = Activator.getDefault().getPreferenceStore();
			break;
		}
		return store;
	}
}
