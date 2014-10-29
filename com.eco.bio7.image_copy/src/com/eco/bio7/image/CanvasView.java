/*******************************************************************************
 * Copyright (c) 2007-2013 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.image;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.plugin.DragAndDrop;

import java.io.File;
import java.util.UUID;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.eclipse.albireo.core.AwtEnvironment;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Listener;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.eco.bio7.ImageJPluginActions.ImageJAnalyzeAction;
import com.eco.bio7.ImageJPluginActions.ImageJEditAction;
import com.eco.bio7.ImageJPluginActions.ImageJFileAction;
import com.eco.bio7.ImageJPluginActions.ImageJImageAction;
import com.eco.bio7.ImageJPluginActions.ImageJPluginsAction;
import com.eco.bio7.ImageJPluginActions.ImageJProcessAction;
import com.eco.bio7.ImageJPluginActions.ImageJWindowAction;
import com.eco.bio7.util.PlaceholderLabel;

public class CanvasView extends ViewPart {
	public static int insertMark = -1;

	public static final String ID = "com.eco.bio7.imagej";

	public static Composite parent2;

	private static JPanel current;

	private ImageJFileAction file;

	private ImageJEditAction edit;

	private ImageJImageAction image;

	private ImageJProcessAction process;

	private ImageJAnalyzeAction analyze;

	private ImageJPluginsAction plugins;

	private ImageJWindowAction imagej_window;

	protected String[] fileList;

	protected ImagePlus plu;

	protected ImageWindow win;

	private static CanvasView canvas_view;

	public static CTabFolder tabFolder;

	public CanvasView() {
		super();
		canvas_view = this;

		this.getViewSite();
	}

	public void createPartControl(Composite parent) {
		AwtEnvironment awt = new AwtEnvironment(parent.getDisplay());

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.imagej");
		getViewSite().getPage().addPartListener(new IPartListener() {
			public void partActivated(IWorkbenchPart part) {
				if (part instanceof CanvasView) {
					// setFocus();
				}
			}

			public void partBroughtToTop(IWorkbenchPart part) {

			}

			public void partClosed(IWorkbenchPart part) {
				if (part instanceof CanvasView) {
					CTabItem[] items = tabFolder.getItems();

					for (int i = 0; i < items.length; i++) {
						Vector ve = (Vector) items[i].getData();
						if (ve.size() > 0) {
							final ImageWindow win = (ImageWindow) ve.get(1);
							/*
							 * Execute on the event dispatching thread!
							 * Important for WorldWind which uses ImageJ! (else
							 * deadlock situation occurs!!!)
							 */
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									win.bio7TabClose();
								}
							});

						}

					}
					canvas_view = null;
				}
			}

			public void partDeactivated(IWorkbenchPart part) {
				if (part instanceof CanvasView) {

				}
			}

			public void partOpened(IWorkbenchPart part) {
				if (part instanceof CanvasView) {

				}
			}
		});
		Display display = Display.getDefault();
		this.parent2 = parent;

		new ImageJ();

		file = new ImageJFileAction();
		edit = new ImageJEditAction();
		image = new ImageJImageAction();
		process = new ImageJProcessAction();
		analyze = new ImageJAnalyzeAction();
		plugins = new ImageJPluginsAction();
		imagej_window = new ImageJWindowAction();

		initializeToolBar();
		tabFolder = new CTabFolder(parent, SWT.TOP);
		DropTarget dt = new DropTarget(tabFolder, DND.DROP_DEFAULT | DND.DROP_MOVE);
		dt.setTransfer(new Transfer[] { FileTransfer.getInstance() });
		dt.addDropListener(new DropTargetAdapter() {
			public void drop(DropTargetEvent event) {

				FileTransfer ft = FileTransfer.getInstance();
				if (ft.isSupportedType(event.currentDataType)) {
					fileList = (String[]) event.data;
					for (int i = 0; i < fileList.length; i++) {

						final int x = i;
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {

								/*
								 * Opener o = new Opener();
								 * o.open(fileList[x].toString());
								 */
								openFile(new File(fileList[x].toString()));
							}
						});
					}

				}
			}
		});
		tabFolder.setBorderVisible(true);
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
		tabFolder.setSimple(false);

		tabFolder.setSelectionBackground(new Color[] { display.getSystemColor(SWT.COLOR_DARK_GREEN), display.getSystemColor(SWT.COLOR_DARK_GREEN) }, new int[] { 90 }, true);
		tabFolder.setSelectionForeground(display.getSystemColor(SWT.COLOR_WHITE));

		tabFolder.addCTabFolder2Listener(new CTabFolder2Listener() {

			public void close(final CTabFolderEvent event) {

				Vector ve = (Vector) event.item.getData();
				ImagePlus plu = (ImagePlus) ve.get(0);

				final ImageWindow win = (ImageWindow) ve.get(1);
				SwingUtilities.invokeLater(new Runnable() {
					// !!
					public void run() {
						win.bio7TabClose();
					}
				});

			}

			public void maximize(CTabFolderEvent event) {

			}

			public void minimize(CTabFolderEvent event) {

			}

			public void restore(CTabFolderEvent event) {

			}

			public void showList(CTabFolderEvent event) {

			}

		});
		tabFolder.addSelectionListener(new SelectionListener() {
			public void itemClosed(CTabFolderEvent event) {

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

			public void widgetSelected(SelectionEvent e) {
				Vector ve = (Vector) e.item.getData();
				plu = (ImagePlus) ve.get(0);

				win = (ImageWindow) ve.get(1);

				WindowManager.setTempCurrentImage(plu);
				WindowManager.setCurrentWindow(win);

				/* import to set current Panel! */
				current = (JPanel) ve.get(2);
				// current.requestFocus();

			}

		});
		tabFolder.addMouseListener(new MouseAdapter() {

			public void mouseDown(MouseEvent mouseevent)

			{
				/*
				 * Important to select the correct image and window when
				 * creating the new ImageJ view!The listener for the right-click
				 * on the tabitem will care about that!
				 */
				if (mouseevent.count == 1) {

					CTabFolder ctab = (CTabFolder) mouseevent.widget;

					if (ctab.getItemCount() > 0) {
						Vector ve = (Vector) ctab.getSelection().getData();
						plu = (ImagePlus) ve.get(0);

						win = (ImageWindow) ve.get(1);
						WindowManager.setTempCurrentImage(plu);
						WindowManager.setCurrentWindow(win);

						// important to set current Panel!
						current = (JPanel) ve.get(2); // current.requestFocus();
					}

				} else if (mouseevent.count == 2 && mouseevent.button == 1) {

					IJ.getInstance().doCommand("Rename...");
				}
			}

		});
		tabFolder.addMouseListener(new MouseAdapter() {

			public void mouseDown(MouseEvent mouseevent)

			{
				if (mouseevent.button == 3) {
					CTabFolder ctab = (CTabFolder) mouseevent.widget;
					if (ctab.getItemCount() > 0) {
						Vector ve = (Vector) ctab.getSelection().getData();
						ImagePlus plu = (ImagePlus) ve.get(0);

						ImageWindow win = (ImageWindow) ve.get(1);
						// JPanel current = (JPanel) ve.get(2);

						CustomView custom = new CustomView();
						/* Create ImageJ view with unique ID! */
						custom.setPanel(current, UUID.randomUUID().toString());
						custom.setData(plu, win);
						IJTabs.hideTab();
					}

				}
			}

		});

	}

	public void setstatusline(String message) {
		IActionBars bars = getViewSite().getActionBars();
		bars.getStatusLineManager().setMessage(message);

	}

	private void initializeToolBar() {
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(file);
		tbm.add(edit);
		tbm.add(image);
		tbm.add(process);
		tbm.add(analyze);
		tbm.add(plugins);
		tbm.add(imagej_window);	
		tbm.add(new PlaceholderLabel().getPlaceholderLabel());

	}

	public void setFocus() {

	}

	public void dispose() {

	}

	public static CanvasView getCanvas_view() {
		return canvas_view;
	}

	public static JPanel getCurrent() {
		return current;
	}

	public static void setCurrent(JPanel current) {
		CanvasView.current = current;
	}

	public static Composite getParent2() {
		return parent2;
	}

	/**
	 * Open a file. If it's a directory, ask to open all images as a sequence in
	 * a stack or individually.
	 */
	private void openFile(File f) {
		try {
			if (null == f)
				return;
			String path = f.getCanonicalPath();
			if (f.exists()) {
				if (f.isDirectory())
					openDirectory(f, path);
				else {
					(new Opener()).openAndAddToRecent(path);
					OpenDialog.setLastDirectory(f.getParent() + File.separator);
					OpenDialog.setLastName(f.getName());
				}
			} else {
				IJ.log("File not found: " + path);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void openDirectory(File f, String path) {
		String[] names = f.list();
		String msg = "Open all " + names.length + " images in \"" + f.getName() + "\" as a stack?";
		GenericDialog gd = new GenericDialog("Open Folder");
		gd.setInsets(10, 5, 0);
		gd.addMessage(msg);
		gd.setInsets(15, 35, 0);
		gd.addCheckbox("Convert to RGB", DragAndDrop.convertToRGB);
		gd.setInsets(0, 35, 0);
		gd.addCheckbox("Use Virtual Stack", DragAndDrop.virtualStack);
		gd.enableYesNoCancel();
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		if (gd.wasOKed()) {
			DragAndDrop.convertToRGB = gd.getNextBoolean();
			DragAndDrop.virtualStack = gd.getNextBoolean();
			String options = " sort";
			if (DragAndDrop.convertToRGB)
				options += " convert_to_rgb";
			if (DragAndDrop.virtualStack)
				options += " use";
			IJ.run("Image Sequence...", "open=[" + path + "/]" + options);
		} else {
			for (int k = 0; k < names.length; k++) {
				IJ.redirectErrorMessages();
				if (!names[k].startsWith("."))
					(new Opener()).open(path + "/" + names[k]);
			}
		}
		IJ.register(DragAndDrop.class);
	}

}
