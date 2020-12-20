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

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.handlers.IHandlerService;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.editor.BeanshellEditorPlugin;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import com.eco.bio7.markdownedit.Activator;
import com.eco.bio7.pythonedit.PythonEditorPlugin;
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.util.Bio7Dialog;

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
	 * Activates the view with the specified id.
	 * 
	 * @param id the id as a string value.
	 */
	public static void activateView(final String id) {

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {

				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				IViewPart viewPart = page.findView(id);
				if (viewPart != null) {
					page.activate(viewPart);
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

	public static void openPerspectiveEditorInJob(final String id) {
		Job job = new Job("Open Perspective") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Open Perspective...", IProgressMonitor.UNKNOWN);
				openPerspective(id);
				monitor.done();
				return Status.OK_STATUS;
			}

		};
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {

					boolean dec = Bio7Dialog.decision("Open the editor in this perspective?");
					if (dec) {
						Work.showHide();
					}

				} else {

				}
			}
		});

		job.schedule();
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
	 * Show or hides the editor area according to a boolean value.
	 * 
	 * @param visible the boolean value
	 */
	public static void setEditorAreaVisible(boolean visible) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				activePage.setEditorAreaVisible(visible);
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

	/**
	 * A method to get the main preferences store for Bio7.
	 * 
	 * @param editor A string value to get the store (value options = bio7plugin).
	 * @return the preference store of the given editor.
	 */
	public static IPreferenceStore getStore(String storePref) {
		IPreferenceStore store = null;
		switch (storePref) {
		case "bio7plugin":
			store = Bio7Plugin.getDefault().getPreferenceStore();
			break;
		}
		return store;
	}

	/**
	 * Execute an registered command.
	 * 
	 * @param commandID the command identifier as String.
	 */
	public static void executeCommand(String commandID) {
		IHandlerService handlerService = (IHandlerService) (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					handlerService.executeCommand(commandID, null);
				} catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
}
