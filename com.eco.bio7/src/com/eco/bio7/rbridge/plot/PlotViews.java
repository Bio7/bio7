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

package com.eco.bio7.rbridge.plot;

import java.util.List;

import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Shows the view of an R plot device in the current perspective (as the screen
 * devices of R come to the front with a new plot). Views are shared by the
 * perspectives of a window: the same view (with the plot) is shown.
 */
final class PlotViews {

	/* The ImageJ-Canvas view */
	private static final String CANVAS_VIEW = "com.eco.bio7.imagej";

	private PlotViews() {
	}

	/**
	 * Moves a view (just opened in the current perspective) into the folder of the
	 * ImageJ-Canvas view, as another tab, if the perspective shows that view (e.g.,
	 * "R ImageJ Analysis"). Like a drag and drop of the tab: later the user can
	 * move it elsewhere. UI thread.
	 */
	static void dockBesideImageJCanvas(String viewId, String secondaryId) {
		try {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window == null) {
				return;
			}
			MWindow mwindow = window.getService(MWindow.class);
			EModelService models = window.getService(EModelService.class);
			if (mwindow == null || models == null) {
				return;
			}
			MPerspective perspective = models.getActivePerspective(mwindow);
			if (perspective == null) {
				return;
			}
			String plotId = secondaryId == null ? viewId : viewId + ":" + secondaryId;
			List<MPlaceholder> canvas = models.findElements(perspective, CANVAS_VIEW, MPlaceholder.class, null);
			List<MPlaceholder> plot = models.findElements(perspective, plotId, MPlaceholder.class, null);
			if (canvas.isEmpty() || plot.isEmpty()) {
				return;
			}
			MPlaceholder canvasHolder = canvas.get(0);
			MPlaceholder plotHolder = plot.get(0);
			Object container = canvasHolder.getParent();
			if (!(container instanceof MPartStack stack) || !canvasHolder.isToBeRendered() || !stack.isToBeRendered()
					|| plotHolder.getParent() == container) {
				return;
			}
			int index = stack.getChildren().indexOf(canvasHolder) + 1;
			@SuppressWarnings({ "unchecked", "rawtypes" })
			MElementContainer<MUIElement> target = (MElementContainer) stack;
			models.move(plotHolder, target, index);
			stack.setSelectedElement(plotHolder);
		} catch (RuntimeException e) {
			/* The view stays where the workbench opened it */
		}
	}

	/**
	 * Shows the view in the current perspective if it is not visible there (without
	 * the focus). UI thread.
	 *
	 * @return true if the view is visible
	 */
	static boolean showInPerspective(String viewId, String secondaryId) {
		if (!PlatformUI.isWorkbenchRunning()) {
			return false;
		}
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
			window = windows.length > 0 ? windows[0] : null;
		}
		IWorkbenchPage page = window != null ? window.getActivePage() : null;
		if (page == null) {
			return false;
		}
		IViewReference ref = page.findViewReference(viewId, secondaryId);
		IViewPart view = ref != null ? ref.getView(false) : null;
		if (view != null && page.isPartVisible(view)) {
			return true;
		}
		try {
			page.showView(viewId, secondaryId, IWorkbenchPage.VIEW_VISIBLE);
			/* New in this perspective: the plot views go to the folder of the ImageJ-Canvas view */
			if (ref == null && !CANVAS_VIEW.equals(viewId)) {
				dockBesideImageJCanvas(viewId, secondaryId);
			}
			return true;
		} catch (PartInitException | RuntimeException e) {
			return false;
		}
	}
}
