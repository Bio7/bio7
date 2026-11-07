/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
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

package com.eco.bio7.spatial;

import java.awt.Frame;

import javax.swing.SwingUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.swt.SwtAwt;
import com.eco.bio7.util.Util;
import com.eco.bio7.worldwind.NewtFullscreen;
import com.jogamp.newt.Window;
import com.jogamp.newt.swt.NewtCanvasSWT;
import com.jogamp.opengl.awt.GLCanvas;

public class SpatialView extends ViewPart {
	public static final String ID = "com.eco.bio7.spatial";

	private SpatialStructure spat;

	private Fullscreen full;

	private Frame frame;

	private GLCanvas canvas;

	public Composite top;

	private SpatialView instance;

	protected boolean detached = false;

	public SpatialView() {

		instance = this;

		/*
		 * if (Util.getOS().equals("Windows")) { SwingUtilities.invokeLater(new
		 * Runnable() { // !! public void run() {
		 * 
		 * spat = new SpatialStructure(instance);
		 * 
		 * } }); } else { For Linux! spat = new SpatialStructure(instance);
		 * 
		 * }
		 */

	}

	public void createPartControl(Composite parent) {
		/* Workaround to set the images for Bio7 again on MacOSX! */
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		Image[] imagesTemp = Util.getShell().getImages();
		initializeToolBar();
		/* Undo and redo of the removed parts of the iso surface with the shortcuts of the workbench */
		SurfaceUndo.install(getViewSite());
		/* Add a listener to detect detached views! */
		parent.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				/* Detached views have no title! */
				if (Util.getOS().equals("Mac")) {
					boolean isDetached = parent.getShell().getText().length() == 0;
					if (isDetached) {

						if (detached == false) {
							// System.out.println("detach");
							spat.glWindow.setFullscreen(true);
							spat.glWindow.setFullscreen(false);
							detached = true;
						}

					} else {
						if (detached == true) {
							// System.out.println("attach");
							spat.glWindow.setFullscreen(true);
							spat.glWindow.setFullscreen(false);
							detached = false;
						}

					}
				}
			}
		});

		top = parent;// new Composite(parent, SWT.NO_BACKGROUND | SWT.EMBEDDED);

		spat = new SpatialStructure(instance);
		/*
		 * try { System.setProperty("sun.awt.noerasebackground", "true"); } catch
		 * (NoSuchMethodError error) { }
		 * 
		 * Display display = PlatformUI.getWorkbench().getDisplay();
		 * display.syncExec(new Runnable() {
		 * 
		 * public void run() {
		 * 
		 * frame = SWT_AWT.new_Frame(top); SwtAwt.setSwtAwtFocus(frame, top); canvas =
		 * spat.getCanvas(); top.setLayout(new RowLayout()); frame.add(canvas); } });
		 */
		Util.getShell().setImages(imagesTemp);

		IPartListener2 pl = new IPartListener2() {

			@Override
			public void partActivated(IWorkbenchPartReference partRef) {

				if (partRef.getId().equals("com.eco.bio7.spatial")) {

				}
			}

			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals("com.eco.bio7.spatial")) {

				}
			}

			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				// TODO Auto-generated method stub

			}

			@Override
			public void partDeactivated(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals("com.eco.bio7.spatial")) {

				}

			}

			@Override
			public void partOpened(IWorkbenchPartReference partRef) {

			}

			@Override
			public void partHidden(IWorkbenchPartReference partRef) {

				if (partRef.getId().equals("com.eco.bio7.spatial")) {
					if (Util.getOS().equals("Mac")) {
						NewtCanvasSWT scanvas = spat.getCanvas();
						if (scanvas.getNEWTChild() != null) {
							scanvas.getNEWTChild().setVisible(false);
							// spat.getCanvas().getNEWTChild().setPosition(0, 0);
						}
					}
				}

			}

			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals("com.eco.bio7.spatial")) {
					if (Util.getOS().equals("Mac")) {
						if (parent.isDisposed() == false) {
							Rectangle rec = parent.getClientArea();
							Display display = PlatformUI.getWorkbench().getDisplay();
							display.asyncExec(new Runnable() {
								public void run() {
									// spat.getCanvas().setParent(parent);
									spat.getCanvas().getNEWTChild().setVisible(true);
									// System.out.println(spat.getCanvas().getNEWTChild().getBounds());
									// spat.getCanvas().getNEWTChild().setPosition(0, 0);
								}
							});
						}
					}
				}

			}

			@Override
			public void partInputChanged(IWorkbenchPartReference partRef) {
				// TODO Auto-generated method stub

			}

		};
		page.addPartListener(pl);
	}

	/**
	 * The fullscreen mode of the 3D view in monitor i (0: the primary monitor, 1:
	 * the secondary monitor, etc.).
	 */
	public void createFullscreen(int i) {
		NewtFullscreen.enter(spat.glWindow, i, null);
	}

	/**
	 * The fullscreen keys of all Bio7 views (see NewtFullscreen): 2 (F2) the
	 * primary monitor (exits in fullscreen mode), 3 (F3) the secondary, 4 (F4) the
	 * tertiary monitor and 5 (F5) a selected monitor.
	 */
	public void fullscreenKey(int key) {
		NewtFullscreen.handleKey(spat.glWindow, key, this::recreateGLCanvas, null);
	}

	public void recreateGLCanvas() {
		spat.getAnimator().stop();
		spat.glWindow.setFullscreen(false);
		spat.getAnimator().start();

	}

	public void setFullscreen(Fullscreen screen) {
		full = screen;
	}

	public Fullscreen getFullscreen() {
		return full;
	}

	public void setFocus() {

	}

	private void initializeToolBar() {
		IToolBarManager toolBar = getViewSite().getActionBars().getToolBarManager();
		Action clear = new Action("Clear Measurements") {
			public void run() {
				/* The object colors, numbers, selection and picked points */
				Measurement3d measurement = SpatialUtil.getMeasurement();
				if (measurement != null) {
					measurement.clear();
				}
			}
		};
		clear.setToolTipText("Clears the measurements in the 3D view (object colors, numbers and picked points)");
		clear.setImageDescriptor(Bio7Plugin.getImageDescriptor("/icons/views/deleteaction.svg"));
		toolBar.add(clear);

		Action reset = new Action("Reset View") {
			public void run() {
				resetView();
			}
		};
		reset.setToolTipText(
				"Resets the 3D view to the default settings like at the first start (camera, grids, axes,\nlights, colors, step rate, height map). The loaded data is kept.");
		reset.setImageDescriptor(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/counter_reset.svg"));
		toolBar.add(reset);

		/* Exports the plot of the plot API to HTML (visible with Plot3D.setExportAction) */
		Action export = new Action("Export Plot (WebGL, Blender)") {
			public void run() {
				exportPlot();
			}
		};
		export.setToolTipText(
				"Exports the displayed plot to an HTML page with WebGL (rotate and zoom in a web browser)\nor for Blender (a glTF file and a Python script with the texts, the camera and the lights)");
		export.setImageDescriptor(Bio7Plugin.getImageDescriptor("/icons/views/savefile.svg"));
		exportItem = new org.eclipse.jface.action.ActionContributionItem(export);
		exportItem.setVisible(exportActionVisible);
		toolBar.add(exportItem);
	}

	/* The action to export a plot to HTML and if it is visible (for all 3D views) */
	private org.eclipse.jface.action.ActionContributionItem exportItem;
	private static volatile boolean exportActionVisible;

	/**
	 * Shows or hides the action of the toolbar which exports the plot of the plot
	 * API (see Plot3D) to an HTML page.
	 * 
	 * @param visible true to show the action
	 */
	public static void setExportActionVisible(boolean visible) {
		exportActionVisible = visible;
		org.eclipse.swt.widgets.Display display = org.eclipse.ui.PlatformUI.getWorkbench().getDisplay();
		display.asyncExec(() -> {
			SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
			SpatialView view = struc != null ? struc.getView() : null;
			if (view != null && view.exportItem != null) {
				view.exportItem.setVisible(exportActionVisible);
				view.getViewSite().getActionBars().getToolBarManager().update(true);
				view.getViewSite().getActionBars().updateActionBars();
			}
		});
	}

	/** Returns true if the action to export a plot to HTML is visible. */
	public static boolean isExportActionVisible() {
		return exportActionVisible;
	}

	/* Asks for a file, exports the displayed plot and opens it in the web browser (if confirmed) */
	private void exportPlot() {
		org.eclipse.swt.widgets.Shell shell = getSite().getShell();
		Plot3D plot = Plot3D.getCurrent();
		if (plot == null) {
			org.eclipse.jface.dialogs.MessageDialog.openInformation(shell, "Export Plot",
					"No plot is displayed. A plot can be created with the plot API (com.eco.bio7.spatial.Plot3D).");
			return;
		}
		org.eclipse.swt.widgets.FileDialog dialog = new org.eclipse.swt.widgets.FileDialog(shell, org.eclipse.swt.SWT.SAVE);
		dialog.setText("Export Plot (WebGL or Blender)");
		dialog.setFilterExtensions(new String[] { "*.html", "*.glb" });
		dialog.setFilterNames(new String[] { "HTML page with WebGL (*.html)", "Blender: glTF and Python script (*.glb, *.py)" });
		dialog.setOverwrite(true);
		String title = plot.title();
		String name = title != null ? title.split("\n")[0].replaceAll("[^A-Za-z0-9_\\- ]", "").trim().replace(' ', '_') : "";
		dialog.setFileName((name.isEmpty() ? "Plot3D" : name) + ".html");
		String file = dialog.open();
		if (file == null) {
			return;
		}
		String lower = file.toLowerCase();
		/* The format of the extension or of the selected filter */
		boolean blender = lower.endsWith(".glb") || (!lower.endsWith(".html") && !lower.endsWith(".htm") && dialog.getFilterIndex() == 1);
		if (blender && !lower.endsWith(".glb")) {
			file += ".glb";
		} else if (!blender && !lower.endsWith(".html") && !lower.endsWith(".htm")) {
			file += ".html";
		}
		try {
			if (blender) {
				String script = plot.exportBlender(file);
				org.eclipse.jface.dialogs.MessageDialog.openInformation(shell, "Export Plot for Blender",
						"The plot has been exported for Blender:\n" + file + "\n" + script
								+ "\n\nIn Blender run the script (Scripting - Open - Run Script) to import the plot with the texts, the camera and the lights.\n"
								+ "The glTF file can also be imported alone (File - Import - glTF 2.0).");
				return;
			}
			plot.exportHtml(file);
		} catch (java.io.IOException | RuntimeException e) {
			org.eclipse.jface.dialogs.MessageDialog.openError(shell, "Export Plot", "The plot could not be exported:\n" + e.getMessage());
			return;
		}
		if (org.eclipse.jface.dialogs.MessageDialog.openQuestion(shell, "Export Plot to HTML",
				"The plot has been exported to:\n" + file + "\n\nOpen the page in the web browser?")) {
			org.eclipse.swt.program.Program.launch(file);
		}
	}

	/* Resets the 3D view and the stored display settings and creates the options view again! */
	private void resetView() {
		SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
		if (grid == null) {
			return;
		}
		grid.resetToDefaults();
		org.eclipse.jface.preference.IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		for (String key : new String[] { "plotView", "showAxisGizmo", "textureFlipVertically", "axisImageCoordinates",
				"spatialX", "spatialY", "spatialZ" }) {
			store.setToDefault(key);
		}
		AxisLabelsRenderer.setImageCoordinates(store.getBoolean("axisImageCoordinates"));
		/* The controls of the options view show the defaults */
		org.eclipse.ui.IWorkbenchPage page = getSite().getPage();
		org.eclipse.ui.IViewPart options = page.findView(Options3d.ID);
		if (options != null) {
			page.hideView(options);
			try {
				page.showView(Options3d.ID, null, org.eclipse.ui.IWorkbenchPage.VIEW_VISIBLE);
			} catch (org.eclipse.ui.PartInitException e) {
				e.printStackTrace();
			}
		}
	}

	public void dispose() {

		super.dispose();
	}

}
