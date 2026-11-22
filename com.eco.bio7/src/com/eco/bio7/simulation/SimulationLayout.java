/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.simulation;

import java.util.List;

import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * The views of a simulation model in the Custom perspective of Bio7: the
 * settings on the left, the display view of the model (the ImageJ-Canvas, the
 * 3D view or the WorldWind globe) on the right. Used for the first setup of a
 * model (see {@link Simulation#arrangeViews}).
 */
public final class SimulationLayout {

	/** The Custom perspective of Bio7. */
	public static final String PERSPECTIVE = "com.eco.bio7.CustomPerspective";

	/** The panel of the settings (a CustomView in the left area of the Custom perspective). */
	public static final String SETTINGS_PANEL = "left";

	/* The areas (part stacks) of the Custom perspective */
	private static final String RIGHT = "MainRight";

	private SimulationLayout() {
	}

	/** The panel of the options view of the display (e.g., Options3D) in the left area. */
	public static final String OPTIONS_PANEL = "subLeftTop";

	/** The panel of the settings below the options view. */
	public static final String SETTINGS_BELOW_OPTIONS = "subLeftBottom";

	/**
	 * The layout of a model: the Custom perspective with the display views of the
	 * model on the right, its options view (e.g., Options3D of the 3D view, the
	 * options of WorldWind) top left (as the 3D swarm example of Bio7) and the
	 * settings left (below the options).
	 */
	static void arrange(Simulation model) {
		String[] options = model.optionsView();
		if (!arrange(model.displayViews())) {
			return;
		}
		if (options != null) {
			com.eco.bio7.collection.Work.moveViewToPanel(new com.eco.bio7.collection.CustomView(), options[0], OPTIONS_PANEL, options[1]);
		}
	}

	/**
	 * The layout of a new model: the Custom perspective is closed first (the
	 * panels of the model before, e.g., its settings and charts, disappear, the
	 * layout starts new) and the images of the simulation before are closed; the
	 * editors stay open.
	 */
	static void arrangeNew(Simulation model) {
		try {
			IWorkbenchWindow window = window();
			IWorkbenchPage page = window == null ? null : window.getActivePage();
			if (page != null) {
				for (org.eclipse.ui.IPerspectiveDescriptor open : page.getOpenPerspectives()) {
					if (PERSPECTIVE.equals(open.getId())) {
						/* Closing the perspective removes the compiled model (Bio7): the new model is set again */
						com.eco.bio7.compile.Model current = com.eco.bio7.methods.Compiled.getModel();
						page.closePerspective(open, false, false);
						if (current == model && com.eco.bio7.methods.Compiled.getModel() != model) {
							com.eco.bio7.methods.Compiled.setModel(model);
						}
						break;
					}
				}
			}
		} catch (RuntimeException e) {
			System.err.println("The Custom perspective could not be closed: " + e.getMessage());
		}
		GridDisplay.closeAll();
		arrange(model);
	}

	/* Closes the panels of the left area which the model does not use (e.g., of the model before) */
	static void closeOtherPanels(Simulation model) {
		try {
			IWorkbenchPage page = window().getActivePage();
			String used = settingsPanel(model);
			for (String panel : new String[] { SETTINGS_PANEL, SETTINGS_BELOW_OPTIONS, OPTIONS_PANEL, ChartDisplay.PANEL }) {
				boolean keep = panel.equals(used) || (panel.equals(OPTIONS_PANEL) && model.optionsView() != null) || (panel.equals(ChartDisplay.PANEL) && (!model.getCharts().isEmpty() || model.usesCharts()));
				if (!keep) {
					org.eclipse.ui.IViewReference ref = page.findViewReference("com.eco.bio7.custom_controls", panel);
					if (ref != null) {
						page.hideView(ref);
					}
				}
			}
		} catch (RuntimeException e) {
			/* No page: nothing to close */
		}
	}

	/** The panel of the settings of the model (in the left area of the Custom perspective). */
	static String settingsPanel(Simulation model) {
		return model.optionsView() != null ? SETTINGS_BELOW_OPTIONS : SETTINGS_PANEL;
	}

	/**
	 * Opens the Custom perspective and moves the views into its right area (the
	 * first view in front). In the thread of the user interface.
	 *
	 * @param viewIds the ids of the display views of the model
	 * @return true if the Custom perspective is active
	 */
	/* The editor area above the display (the right side) and the left side narrower (about a quarter) */
	private static void editorAbove(EModelService models, MPerspective perspective, MPartStack right) {
		try {
			List<org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder> editors = models.findElements(perspective, "org.eclipse.ui.editorss",
					org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder.class, null);
			if (editors.isEmpty()) {
				return;
			}
			org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder editor = editors.get(0);
			if (!inColumnOf(editor, right)) {
				editor.getParent().getChildren().remove(editor);
				models.insert(editor, right, EModelService.ABOVE, 0.42f);
			}
			/* The weights of the columns: the left column and the column of the display */
			List<MPartStack> lefts = models.findElements(perspective, "MainLeft", MPartStack.class, null);
			MUIElement top = topColumn(right);
			if (top != null && top.getParent() != null) {
				for (MUIElement c : top.getParent().getChildren()) {
					boolean left = !lefts.isEmpty() && contains(c, lefts.get(0));
					c.setContainerData(c == top ? "7400" : left ? "2600" : c.getContainerData());
				}
			}
		} catch (RuntimeException e) {
			System.err.println("The editor could not be moved above the display: " + e.getMessage());
		}
	}

	/** Moves the Charts view (its panel) beside the display (right of the display, below the editor). */
	static void chartsBeside() {
		try {
			IWorkbenchWindow window = window();
			EModelService models = window.getService(EModelService.class);
			MPerspective perspective = models.getActivePerspective(window.getService(MWindow.class));
			if (perspective == null || !PERSPECTIVE.equals(perspective.getElementId())) {
				return;
			}
			List<MPartStack> right = models.findElements(perspective, RIGHT, MPartStack.class, null);
			List<MPartStack> charts = models.findElements(perspective, "SubRightBottom", MPartStack.class, null);
			if (right.isEmpty() || charts.isEmpty()) {
				return;
			}
			MPartStack c = charts.get(0), r = right.get(0);
			MUIElement parent = r.getParent();
			if (parent != null && parent == (Object) c.getParent() && parent instanceof org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer sash && sash.isHorizontal()) {
				return;
			}
			c.getParent().getChildren().remove(c);
			models.insert(c, r, EModelService.RIGHT_OF, 0.4f);
		} catch (RuntimeException e) {
			System.err.println("The charts could not be moved beside the display: " + e.getMessage());
		}
	}

	/* true if the element is in a vertical sash (a column) with the stack (directly or nested) */
	private static boolean inColumnOf(MUIElement element, MUIElement stack) {
		MUIElement parent = element.getParent();
		return parent instanceof org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer sash && !sash.isHorizontal() && contains(parent, stack);
	}

	/* The column of the stack: its ancestor which is a child of the outermost sash */
	private static MUIElement topColumn(MUIElement stack) {
		MUIElement e = stack;
		while ((Object) e.getParent() instanceof org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer p
				&& (Object) p.getParent() instanceof org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer) {
			e = p;
		}
		return e;
	}

	private static boolean contains(MUIElement container, MUIElement element) {
		for (MUIElement e = element; e != null; e = e.getParent()) {
			if (e == container) {
				return true;
			}
		}
		return false;
	}

	/* The window of the workbench (also if Bio7 is not the active application) */
	static IWorkbenchWindow window() {
		IWorkbenchWindow w = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (w == null && PlatformUI.getWorkbench().getWorkbenchWindowCount() > 0) {
			w = PlatformUI.getWorkbench().getWorkbenchWindows()[0];
		}
		return w;
	}

	public static boolean arrange(String... viewIds) {
		try {
			IWorkbenchWindow window = window();
			if (window == null) {
				return false;
			}
			IWorkbenchPage page = window.getActivePage();
			if (page == null) {
				return false;
			}
			if (page.getPerspective() == null || !PERSPECTIVE.equals(page.getPerspective().getId())) {
				PlatformUI.getWorkbench().showPerspective(PERSPECTIVE, window);
				page = window.getActivePage();
			}
			EModelService models = window.getService(EModelService.class);
			MWindow mwindow = window.getService(MWindow.class);
			MPerspective perspective = models == null || mwindow == null ? null : models.getActivePerspective(mwindow);
			List<MPartStack> stacks = perspective == null ? List.of() : models.findElements(perspective, RIGHT, MPartStack.class, null);
			IViewPart first = null;
			for (String id : viewIds) {
				IViewPart view = page.showView(id, null, IWorkbenchPage.VIEW_VISIBLE);
				if (first == null) {
					first = view;
				}
				if (!stacks.isEmpty() && view != null) {
					MPart part = view.getSite().getService(MPart.class);
					/* A view of a perspective is a placeholder of a shared part */
					MStackElement element = part != null && part.getCurSharedRef() != null ? part.getCurSharedRef() : part;
					MPartStack right = stacks.get(0);
					if (element != null && (Object) element.getParent() != right) {
						models.move(element, right);
					}
				}
			}
			if (first != null) {
				page.bringToTop(first);
			}
			if (!stacks.isEmpty()) {
				/* The editor above the display, the left side narrower */
				editorAbove(models, perspective, stacks.get(0));
				page.setEditorAreaVisible(true);
			}
			return true;
		} catch (Exception e) {
			System.err.println("The views of the simulation could not be arranged: " + e.getMessage());
			return false;
		}
	}
}
