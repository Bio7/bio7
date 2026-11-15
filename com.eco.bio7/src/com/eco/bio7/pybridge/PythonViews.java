/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.pybridge;

import java.util.List;

import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/** Opens the views of Python as tabs beside their R views (e.g., the Python-Shell beside the R-Shell). */
public final class PythonViews {

	private PythonViews() {
	}

	/**
	 * Shows a view; a view opened the first time in the perspective is moved into
	 * the folder of another view (another tab), if the perspective shows it. UI
	 * thread.
	 */
	static void showBeside(String viewId, String besideId) {
		showBeside(viewId, besideId, true);
	}

	/** As showBeside; activate false: the view is shown without the focus. */
	static void showBeside(String viewId, String besideId, boolean activate) {
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window == null) {
				return;
			}
			IWorkbenchPage page = window.getActivePage();
			boolean open = page.findView(viewId) != null;
			page.showView(viewId, null, activate ? IWorkbenchPage.VIEW_ACTIVATE : IWorkbenchPage.VIEW_VISIBLE);
			if (!open) {
				dock(window, viewId, besideId);
			}
		} catch (Exception e) {
			/* Not shown */
		}
	}

	private static final String R_SHELL = "com.eco.bio7.RShell";
	private static final String R_PACKAGES = "com.eco.bio7.rbridge.PackageInstallView";
	private static final String R_DATABASE = "com.eco.bio7.reditor.database.view.DatabaseView";
	/* The further views closed by a switch (opened again by the switch back) */
	private static final java.util.Set<String> closedR = new java.util.LinkedHashSet<>();
	private static final java.util.Set<String> closedPython = new java.util.LinkedHashSet<>();

	/** The preference: the switch of the Image-Methods view closes the views of the other language. */
	public static final String PREF_CLOSE_VIEWS = "IMAGE_METHODS_CLOSE_VIEWS";

	/**
	 * The views of Python or R (e.g., after a switch of the transfers of the
	 * Image-Methods view): the Python-Shell and Python Packages in the folders of
	 * the R-Shell and the R packages, or back. The views of the other language are
	 * closed (preference) and the shell of the language is activated. UI thread.
	 */
	public static void exchange(boolean python) {
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IWorkbenchPage page = window == null ? null : window.getActivePage();
			if (page == null) {
				return;
			}
			boolean close = com.eco.bio7.Bio7Plugin.getDefault().getPreferenceStore().getBoolean(PREF_CLOSE_VIEWS);
			String shell = python ? PythonShellView.ID : R_SHELL;
			String packages = python ? PythonPackagesView.ID : R_PACKAGES;
			String otherShell = python ? R_SHELL : PythonShellView.ID;
			String otherPackages = python ? R_PACKAGES : PythonPackagesView.ID;
			/* In the folders of the views of the other language (before they are closed) */
			showBeside(packages, otherPackages, false);
			showBeside(shell, otherShell, false);
			/* The views of the language which were closed by the last switch (e.g., the R database view) */
			for (String id : python ? closedPython : closedR) {
				showBeside(id, shell, false);
			}
			(python ? closedPython : closedR).clear();
			if (close) {
				List<String> others = python ? List.of(otherShell, otherPackages, R_DATABASE) : List.of(otherShell, otherPackages);
				for (String id : others) {
					var ref = page.findViewReference(id);
					if (ref != null) {
						page.hideView(ref);
						/* Opened again by the switch back (the shell and the packages are always opened) */
						if (!id.equals(otherShell) && !id.equals(otherPackages)) {
							(python ? closedR : closedPython).add(id);
						}
					}
				}
			}
			/* The shell of the language is active */
			page.showView(shell, null, IWorkbenchPage.VIEW_ACTIVATE);
		} catch (Exception e) {
			/* Not changed */
		}
	}

	/**
	 * Moves an open view into the folder of another view (another tab). UI thread.
	 *
	 * @param viewId   the id of the view (with a secondary id: "id:secondary")
	 * @return true if the view is in the folder of the other view
	 */
	static boolean dockInto(String viewId, String besideId) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		return window != null && dock(window, viewId, besideId);
	}

	private static boolean dock(IWorkbenchWindow window, String viewId, String besideId) {
		try {
			MWindow mwindow = window.getService(MWindow.class);
			EModelService models = window.getService(EModelService.class);
			MPerspective perspective = models.getActivePerspective(mwindow);
			List<MPlaceholder> beside = models.findElements(perspective, besideId, MPlaceholder.class, null);
			List<MPlaceholder> view = models.findElements(perspective, viewId, MPlaceholder.class, null);
			Object parent = beside.isEmpty() ? null : beside.get(0).getParent();
			if (view.isEmpty() || !(parent instanceof MPartStack stack) || !beside.get(0).isToBeRendered()) {
				return false;
			}
			if ((Object) view.get(0).getParent() != stack) {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				MElementContainer<MUIElement> target = (MElementContainer) stack;
				models.move(view.get(0), target);
			}
			stack.setSelectedElement(view.get(0));
			return true;
		} catch (RuntimeException e) {
			/* The view stays where the workbench opened it */
			return false;
		}
	}
}
