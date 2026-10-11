/*******************************************************************************
 * Copyright (c) 2004-2026 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.image.ui;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.action.SubContributionManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.eco.bio7.ImageJPluginActions.ImageJAnalyzeAction;
import com.eco.bio7.ImageJPluginActions.ImageJEditAction;
import com.eco.bio7.ImageJPluginActions.ImageJFileAction;
import com.eco.bio7.ImageJPluginActions.ImageJHelp;
import com.eco.bio7.ImageJPluginActions.ImageJImageAction;
import com.eco.bio7.ImageJPluginActions.ImageJPluginsAction;
import com.eco.bio7.ImageJPluginActions.ImageJProcessAction;
import com.eco.bio7.ImageJPluginActions.ImageJWindowAction;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.plugin.CommandLister;
import ij.plugin.DragAndDrop;

/**
 * The ImageJ-Canvas view. Each image is displayed in a tab. The control of a
 * tab is the holder composite of the image (see {@link Bio7ImageJ}) and the tab
 * data is the {@link ImagePlus}.
 */
public class CanvasView extends ViewPart {
	public static int insertMark = -1;

	public static final String ID = "com.eco.bio7.imagej";

	public static Composite parent2;

	private ImageJFileAction file;

	private ImageJEditAction edit;

	private ImageJImageAction image;

	private ImageJProcessAction process;

	private ImageJAnalyzeAction analyze;

	private ImageJPluginsAction plugins;

	private ImageJWindowAction window;

	private ImageJHelp help;

	protected String[] fileList;

	protected ImagePlus plu;

	protected ImageWindow win;

	private static CanvasView canvas_view;

	public static CTabFolder tabFolder;

	public CanvasView() {
		super();
		canvas_view = this;
	}

	/**
	 * Lays out the view (kept for compatibility).
	 */
	public void updatePlotCanvas() {
		recalculateLayout();
	}

	/**
	 * Lays out the view and the displayed image.
	 */
	public void recalculateLayout() {
		Util.getDisplay().syncExec(() -> {
			if (parent2 != null && !parent2.isDisposed()) {
				parent2.layout(true, true);
			}
		});
	}

	public void layoutParent(Composite parent) {
		Util.getDisplay().syncExec(() -> {
			if (!parent.isDisposed()) {
				parent.layout(true, true);
			}
		});
	}

	public void createPartControl(Composite parent) {
		parent2 = parent;
		/* Creates the ImageJ instance and registers Bio7 as the ImageJ host! */
		Bio7ImageJ.install();

		parent.addDisposeListener(e -> {
			/* Save the ImageJ preferences when the view is closed! */
			try {
				ij.Prefs.savePreferences();
			} catch (RuntimeException ex) {
				ex.printStackTrace();
			}
		});

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.imagej");

		parent.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(final ControlEvent e) {
				updatePlotDeviceSize(parent.getClientArea(), tabFolder != null && !tabFolder.isDisposed() ? tabFolder.getTabHeight() + 40 : 0);
			}
		});

		file = new ImageJFileAction();
		edit = new ImageJEditAction();
		image = new ImageJImageAction();
		process = new ImageJProcessAction();
		analyze = new ImageJAnalyzeAction();
		plugins = new ImageJPluginsAction();
		window = new ImageJWindowAction();
		help = new ImageJHelp();

		initializeToolBar();
		tabFolder = new CTabFolder(parent, SWT.TOP);
		/* Get access to the ImageJ drag and drop methods! */
		DragAndDrop dragAndDrop = new DragAndDrop();
		DropTarget dt = new DropTarget(tabFolder, DND.DROP_DEFAULT | DND.DROP_MOVE | DND.DROP_COPY);
		dt.setTransfer(new Transfer[] { FileTransfer.getInstance() });
		dt.addDropListener(new DropTargetAdapter() {
			public void drop(DropTargetEvent event) {
				FileTransfer ft = FileTransfer.getInstance();
				if (ft.isSupportedType(event.currentDataType)) {
					fileList = (String[]) event.data;
					Job job = new Job("Open...") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							monitor.beginTask("Opening...", fileList.length);
							for (int i = 0; i < fileList.length; i++) {
								dragAndDrop.openFile(new File(fileList[i]));
								monitor.worked(1);
							}
							monitor.done();
							return Status.OK_STATUS;
						}
					};
					job.schedule();
				}
			}
		});

		CommandLister hotkeys = new CommandLister();
		String[] shortcuts = hotkeys.getShortcuts();

		tabFolder.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				runShortcut(shortcuts, e);
			}
		});

		tabFolder.setBorderVisible(true);

		tabFolder.addCTabFolder2Listener(new CTabFolder2Adapter() {
			public void close(final CTabFolderEvent event) {
				/* Disposing the holder closes the image (see Bio7ImageJ)! */
				Control holder = ((CTabItem) event.item).getControl();
				if (holder != null && !holder.isDisposed()) {
					holder.dispose();
				}
			}
		});
		tabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				tabSelected((CTabItem) e.item);
			}
		});
		tabFolder.addMouseListener(new MouseAdapter() {

			public void mouseDown(MouseEvent mouseevent) {
				CTabFolder ctab = (CTabFolder) mouseevent.widget;
				if (mouseevent.count == 1) {
					if (ctab.getItemCount() <= 0) {
						tabFolder.setFocus();
					} else if (ctab.getSelection() != null) {
						tabSelected(ctab.getSelection());
					}
				} else if (mouseevent.button == 3 && mouseevent.count > 1) {
					/* Detach the image of the selected tab in a view! */
					CTabItem item = ctab.getSelection();
					if (item != null && item.getData() instanceof ImagePlus) {
						CustomDetachedImageJView.detach((ImagePlus) item.getData(), null);
					}
				}
			}
		});
	}

	/*
	 * Here we write the values in the com.eco.bio7 plugin preferences with the help
	 * of scoped preferences (R plots are created with the size of the view)!
	 */
	static void updatePlotDeviceSize(Rectangle rec, int correction) {
		IPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, "com.eco.bio7");
		String selection = store.getString("PLOT_DEVICE_SELECTION");
		String pathTo = store.getString("pathTempR");
		if (selection.equals("PLOT_IMAGEJ_DISPLAYSIZE_CAIRO")) {
			store.setValue("DEVICE_DEFINITION", ".bio7Device <- function(filename = \"" + pathTo + "tempDevicePlot%05d.tiff" + "\") { tiff(filename,width = " + (rec.width - 20)
					+ ", height = " + (rec.height - correction) + ", type=\"cairo\")}; options(device=\".bio7Device\")");
		} else if (selection.equals("PLOT_IMAGEJ_DISPLAYSIZE")) {
			store.setValue("DEVICE_DEFINITION", ".bio7Device <- function(filename = \"" + pathTo + "tempDevicePlot%05d.tiff" + "\") { tiff(filename,width =  " + (rec.width - 20)
					+ ", height = " + (rec.height - correction) + ", units = \"px\")}; options(device=\".bio7Device\")");
		}
	}

	private void runShortcut(String[] shortcuts, KeyEvent e) {
		for (int i = 0; i < shortcuts.length; i++) {
			String[] splitShortcut = shortcuts[i].split("\t");
			if (splitShortcut.length < 2) {
				continue;
			}
			String key = splitShortcut[0].trim();
			if (key.equals("" + e.character)) {
				IJ.doCommand(splitShortcut[1]);
			}
			/* Also allow the F keys for a shortcut! */
			for (int f = 1; f <= 20; f++) {
				if (key.equals("F" + f) && e.keyCode == SWT.F1 + f - 1) {
					IJ.doCommand(splitShortcut[1]);
				}
			}
		}
	}

	/**
	 * Creates a new tab for the given image and returns the holder composite in
	 * which the image window is created.
	 */
	Composite createImageTab(ImagePlus imp) {
		CTabItem item = new CTabItem(tabFolder, SWT.CLOSE, Math.max(0, Math.min(insertMark + 1, tabFolder.getItemCount())));
		Composite holder = Bio7ImageJ.createHolder(tabFolder, imp);
		item.setControl(holder);
		item.setData(imp);
		item.setText(imp.getTitle());
		tabFolder.showItem(item);
		tabFolder.setSelection(item);
		plu = imp;
		return holder;
	}

	/**
	 * Makes the image of the given tab the current ImageJ image.
	 */
	public void tabSelected(CTabItem item) {
		if (item == null || !(item.getData() instanceof ImagePlus)) {
			return;
		}
		plu = (ImagePlus) item.getData();
		win = plu.getWindow();
		if (win != null && !win.isClosed()) {
			WindowManager.setTempCurrentImage(plu);
			WindowManager.setCurrentWindow(win);
			setstatusline(win.createSubtitle());
		}
	}

	public void setstatusline(String message) {
		IActionBars bars = getViewSite().getActionBars();
		bars.getStatusLineManager().setMessage(message);
	}

	public void setStatusLineColor(String message, org.eclipse.swt.graphics.Color col) {
		IActionBars bars = getViewSite().getActionBars();
		bars.getStatusLineManager().setMessage(message);
		IStatusLineManager statusLineManager = bars.getStatusLineManager();
		if (statusLineManager instanceof SubContributionManager) {
			SubContributionManager sub = (SubContributionManager) statusLineManager;
			StatusLineManager parent = (StatusLineManager) sub.getParent();
			Composite composite = (Composite) parent.getControl();
			Control[] controls = composite.getChildren();
			for (Control control : controls) {
				if (control instanceof CLabel) {
					CLabel cLabel = (CLabel) control;
					cLabel.setForeground(col);
					break;
				}
			}
		}
	}

	private void initializeToolBar() {
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.add(file);
		tbm.add(edit);
		tbm.add(image);
		tbm.add(process);
		tbm.add(analyze);
		tbm.add(plugins);
		tbm.add(window);
		tbm.add(help);
		/* If we have a plugin which installs a main menu item! */
		updateBio7ImageJMenu();
	}

	private static boolean isDefaultMenu(String name) {
		return name.equals("File") || name.equals("Edit") || name.equals("Image") || name.equals("Process") || name.equals("Analyze") || name.equals("Plugins") || name.equals("Window")
				|| name.equals("Help");
	}

	/**
	 * Adds the additional ImageJ main menus (e.g., installed by a plugin) to the
	 * toolbar of the view.
	 */
	public static void updateBio7ImageJMenu() {
		Util.getDisplay().asyncExec(() -> {
			CanvasView canvasView = getCanvas_view();
			org.eclipse.swt.widgets.Menu menuBar = ij.Menus.getMenuBar();
			if (canvasView == null || menuBar == null || menuBar.isDisposed()) {
				return;
			}
			IToolBarManager tbm = canvasView.getViewSite().getActionBars().getToolBarManager();
			for (org.eclipse.jface.action.IContributionItem item : tbm.getItems()) {
				if (item.getId() != null && !isDefaultMenu(item.getId())) {
					tbm.remove(item);
				}
			}
			for (org.eclipse.swt.widgets.MenuItem item : menuBar.getItems()) {
				String name = item.getText().replace("&", "");
				if (!name.isEmpty() && !isDefaultMenu(name)) {
					tbm.add(new com.eco.bio7.ImageJPluginActions.ImageJExtensionAction(name));
				}
			}
			tbm.update(true);
		});
	}

	public void setFocus() {
		if (tabFolder != null && !tabFolder.isDisposed()) {
			tabFolder.setFocus();
		}
	}

	public void dispose() {
		if (canvas_view == this) {
			canvas_view = null;
		}
		super.dispose();
	}

	public static CanvasView getCanvas_view() {
		return canvas_view;
	}

	/**
	 * Returns the holder composite of the selected tab or null.
	 *
	 * @return the composite which embeds the selected image.
	 */
	public static Composite getCurrent() {
		if (tabFolder == null || tabFolder.isDisposed()) {
			return null;
		}
		Display display = tabFolder.getDisplay();
		Composite[] current = new Composite[1];
		display.syncExec(() -> {
			if (!tabFolder.isDisposed() && tabFolder.getSelection() != null) {
				current[0] = (Composite) tabFolder.getSelection().getControl();
			}
		});
		return current[0];
	}

	public static Composite getParent2() {
		return parent2;
	}
}
