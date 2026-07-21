/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
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
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
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
	 * Moves an Eclipse workbench view into a custom Bio7 panel by reparenting its
	 * SWT widget tree.
	 * <p>
	 * The view is first opened the regular way in the Eclipse workbench
	 * ({@link Work#openView(String)}). Asynchronously on the UI thread, the method
	 * then retrieves the view's e4 part model ({@code MPart}) from the part site
	 * and takes its widget — the composite the workbench passed to
	 * {@code createPartControl()}. All children of that composite (i.e. the view's
	 * actual UI) are reparented into the target Bio7 panel, a layout pass is
	 * forced, and the now-empty original view tab is hidden in the workbench.
	 * <p>
	 * Note: Reparenting relies on {@link Control#setParent(Composite)}, which
	 * requires platform support for reparenting. Requires a dependency on the e4
	 * workbench model bundle ({@code org.eclipse.e4.ui.model.workbench}).
	 *
	 * @param customView    the Bio7 {@code CustomView} providing the target panels;
	 *                      if {@code null}, the method returns without effect
	 * @param viewId        the Eclipse view ID of the view to embed (e.g.
	 *                      {@code "org.eclipse.ui.console.ConsoleView"}); must not
	 *                      be {@code null}
	 * @param panelLocation the identifier of the target panel inside the
	 *                      CustomView; must not be {@code null}
	 * @param newTitle      the new title for the panel's tab; may be {@code null}
	 *                      to leave the tab title unchanged
	 */
	public static void moveViewToPanel(final CustomView customView, final String viewId, final String panelLocation,
			String newTitle) {
		if (customView == null || viewId == null || panelLocation == null) {
			return;
		}

		// Get the target composite from Bio7 and set the focus
		final Composite targetParent = customView.getComposite(panelLocation);
		if (targetParent == null) {
			System.err.println("Bio7 panel '" + panelLocation + "' could not be found.");
			return;
		}
		renamePanelTab(targetParent, newTitle);
		targetParent.setFocus();

		// First open the view the classic way in the Eclipse background
		Work.openView(viewId);

		// Defer to the UI thread asynchronously so the widgets have time to render
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				if (targetParent.isDisposed()) {
					return;
				}

				try {
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					IViewPart targetView = page.findView(viewId);

					if (targetView != null) {
						// Retrieve the e4 part model of the view; its widget is the
						// composite that hosts everything created in createPartControl()
						MPart partModel = targetView.getSite().getService(MPart.class);
						Composite partComposite = null;

						if (partModel != null && partModel.getWidget() instanceof Composite) {
							partComposite = (Composite) partModel.getWidget();
						}

						if (partComposite != null && !partComposite.isDisposed()) {
							// REPARENTING: Move the view's actual UI (the children of the
							// part composite) into the Bio7 panel. The part composite
							// itself stays with the workbench so it can be disposed
							// safely when the view is hidden below.
							for (Control child : partComposite.getChildren()) {
								child.setParent(targetParent);
								child.setVisible(true);
							}

							// Force a layout update so the elements adapt to the panel
							targetParent.layout(true, true);

							// Close the empty, unused Eclipse background tab of the original view
							page.hideView(targetView);
						} else {
							System.err.println("Could not locate the UI components of view '" + viewId + "'.");
						}
					}
				} catch (Exception e) {
					System.err.println("Error while moving view '" + viewId + "': " + e.getMessage());
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Changes the tab title of the CustomView panel that contains the given
	 * composite.
	 * <p>
	 * The method climbs up the SWT widget tree starting from the given panel
	 * composite until it finds the enclosing tab container. Both {@link CTabFolder}
	 * (used by the Bio7 CustomView) and the classic {@link TabFolder} are
	 * supported. Within the folder, the tab item whose control is the panel
	 * composite itself — or an ancestor wrapper of it — is located and its text is
	 * updated.
	 * <p>
	 * If the tab items report no control (i.e. {@code getControl()} returns
	 * {@code null}), the currently selected tab of the folder is renamed as a
	 * fallback. If no tab container is found at all, an error message is printed
	 * and the title remains unchanged.
	 * <p>
	 * Note: This method must be called from the SWT UI thread, since it accesses
	 * and modifies widgets.
	 *
	 * @param panelComposite the composite of the CustomView panel whose tab should
	 *                       be renamed (as returned by
	 *                       {@code CustomView.getComposite(String)}); if
	 *                       {@code null} or disposed, the method returns without
	 *                       effect
	 * @param newTitle       the new text for the tab; if {@code null}, the method
	 *                       returns without effect and the title remains unchanged
	 */
	public static void renamePanelTab(final Composite panelComposite, final String newTitle) {
		if (panelComposite == null || panelComposite.isDisposed() || newTitle == null) {
			return;
		}

		// Climb up the widget tree until we reach the CTabFolder of the CustomView
		Composite parent = panelComposite.getParent();
		Control child = panelComposite;

		while (parent != null) {
			if (parent instanceof CTabFolder) {
				CTabFolder folder = (CTabFolder) parent;
				for (CTabItem item : folder.getItems()) {
					// Find the CTabItem whose control is our panel (or contains it)
					if (item.getControl() == child || item.getControl() == panelComposite) {
						item.setText(newTitle);
						return;
					}
				}
				// Fallback: if getControl() returns null for the items,
				// rename the currently selected tab instead
				if (folder.getSelection() != null) {
					folder.getSelection().setText(newTitle);
				}
				return;
			}
			// Also support the classic (non-custom) TabFolder
			if (parent instanceof TabFolder) {
				TabFolder folder = (TabFolder) parent;
				for (TabItem item : folder.getItems()) {
					if (item.getControl() == child || item.getControl() == panelComposite) {
						item.setText(newTitle);
						return;
					}
				}
				return;
			}
			child = parent;
			parent = parent.getParent();
		}

		System.err.println("No tab container found for the panel - title unchanged.");
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
		IHandlerService handlerService = (IHandlerService) (IHandlerService) PlatformUI.getWorkbench()
				.getService(IHandlerService.class);
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
