/*******************************************************************************
 * Copyright (c) 2004-2018 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.image;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.UUID;
import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.action.SubContributionManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
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
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import com.eco.bio7.ImageJPluginActions.ImageJAnalyzeAction;
import com.eco.bio7.ImageJPluginActions.ImageJEditAction;
import com.eco.bio7.ImageJPluginActions.ImageJExtensionAction;
import com.eco.bio7.ImageJPluginActions.ImageJFileAction;
import com.eco.bio7.ImageJPluginActions.ImageJHelp;
import com.eco.bio7.ImageJPluginActions.ImageJImageAction;
import com.eco.bio7.ImageJPluginActions.ImageJPluginsAction;
import com.eco.bio7.ImageJPluginActions.ImageJProcessAction;
import com.eco.bio7.ImageJPluginActions.ImageJWindowAction;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.Macro;
import ij.Menus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.Plot;
import ij.gui.PlotCanvas;
import ij.gui.PlotWindow;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.plugin.CommandLister;
import ij.plugin.DragAndDrop;
import ij.plugin.FileInfoVirtualStack;
import ij.plugin.FolderOpener;
import ij.plugin.TextReader;
import ij.plugin.frame.Recorder;
import ij.process.ImageProcessor;
import com.eco.bio7.image.swtawtutil.*;

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

	private ImageJWindowAction window;

	protected String[] fileList;

	protected ImagePlus plu;

	protected ImageWindow win;

	private ImageJHelp help;

	private String osname;

	private boolean isWin;

	private boolean isMac;

	private boolean isLinux;

	private static CanvasView canvas_view;

	public static CTabFolder tabFolder;

	// private ArrayList<String> detachedSecViewIDs = new ArrayList<String>();

	public CanvasView() {
		super();
		canvas_view = this;

		this.getViewSite();

	}

	public void updatePlotCanvas() {
		Display dis = Util.getDisplay();
		dis.syncExec(new Runnable() {

			public void run() {
				/* Call parent layout before the plot layout! */
				parent2.layout();

			}
		});
		current.doLayout();
	}

	public void recalculateLayout() {
		Display dis = Util.getDisplay();
		dis.syncExec(new Runnable() {
			public void run() {
				/*
				 * Call parent layout before the plot layout! Here we add a workaround to layout
				 * the parent on high dpi displays and the SWT_AWT bridge by resizing the parent
				 * forth and back which seem to retrigger a layout Without HighDPI it also
				 * retriggers the layout!!
				 **/
				int x = parent2.getSize().x;
				int y = parent2.getSize().y;
				parent2.setSize(x - 1, y - 1);
				parent2.setSize(x, y);

				/*
				 * For the following see:
				 * https://www.eclipse.org/eclipse/news/4.6/platform_isv.php#swt-requestlayout
				 */
				// parent2.requestLayout();

			}
		});
	}

	public void layoutParent(Composite parent) {
		// Wrap to avoid deadlock of awt frame access!
		Display dis = Util.getDisplay();
		dis.syncExec(new Runnable() {

			public void run() {
				/* Call parent layout before the plot layout! */
				/*
				 * The layout of tab items plots will be changed in the tab selection listener
				 * (see tab listener below)! This is by the way not so expansive as to relayout
				 * all PlotWindows. Only the visible tab item PlotWindows will be relayouted!
				 */
				parent.layout();

			}
		});
	}

	void resizePlotWindow(Composite parent, ImageWindow win) {
		if (parent.isDisposed() == false) {

			if (win != null) {

				// plotWindowResize(win,current);

				int ids[] = WindowManager.getIDList();
				if (ids != null) {
					for (int i = 0; i < ids.length; i++) {
						ImagePlus ip = WindowManager.getImage(ids[i]);
						// JPanel panel=(JPanel)win.getCanvas().getParent();
						if (ip.getWindow() instanceof PlotWindow) {
							JPanel panel = (JPanel) ip.getCanvas().getParent();
							plotWindowResize(ip.getWindow(), panel);

						}

					}
				}
				/*
				 * IWorkbenchPage page =
				 * PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				 * IViewReference[] ref = page.getViewReferences(); for (int i = 0; i <
				 * ref.length; i++) { String secondaryId = ref[i].getSecondaryId(); if
				 * (secondaryId != null) { IViewPart part=ref[i].getView(false);
				 * CustomDetachedImageJView customView=(CustomDetachedImageJView) part;
				 * if(customView.isDetached()==false) { if(customView.plus!=null) { JPanel
				 * panel=(JPanel)WindowManager.getImage(secondaryId).getCanvas().getParent();
				 * System.out.println(WindowManager.getImage(secondaryId).getTitle());
				 * plotWindowResize(win,panel); }
				 * 
				 * }
				 * 
				 * } }
				 */
				// Rectangle rec = parent.getClientArea();

			}
		}
	}

	private void plotWindowResize(ImageWindow win, JPanel panel) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {

				if (win instanceof PlotWindow) {
					PlotWindow plo = (PlotWindow) win;

					if (plo != null) {
						Dimension rec = panel.getSize();
						Plot plot = plo.getPlot();
						if (plot != null) {
							int correctionX = plot.leftMargin + plot.rightMargin;
							int correctionY = plot.topMargin + plot.bottomMargin;
							plot.getImagePlus().getCanvas().setSize(rec.width + correctionX, rec.height + correctionY);
							plot.setFrameSize(rec.width, rec.height);
							plot.setSize(rec.width - correctionX, rec.height - correctionY);
							panel.doLayout();
						}
					}
				}

				// plo.setLocationAndSize(rec.x, rec.y, rec.width, rec.height);
			}
		});
	}

	public void createPartControl(Composite parent) {

		osname = System.getProperty("os.name");
		isWin = osname.startsWith("Windows");
		isMac = !isWin && osname.startsWith("Mac");
		isLinux = osname.startsWith("Linux");
		/*
		 * A a dialog listener for modal dialogs. Works only for Windows. On Mac dialogs
		 * are modal in maximized mode! On Linux modal dialogs are not available by
		 * default!
		 */
		if (isWin) {
			new AwtDialogListener(Util.getDisplay());
		}
		setComponentFont(parent.getDisplay());
		if (isWin) {

			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				if (Util.isThemeBlack()) {
					if (Bio7DarkThemeAdditions.isSet() == false) {
						Color colBackgr = Util.getSWTBackgroundToAWT(parent);
						Color colForegr = Util.getSWTForegroundToAWT(parent);
						new Bio7DarkThemeAdditions().applyWinBlackThemeAdditions(colBackgr, colForegr);
					}
				}
			} catch (ClassNotFoundException e) {

				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {

				e.printStackTrace();
			} catch (UnsupportedLookAndFeelException e) {
				e.printStackTrace();
			}
		} else if (isMac) {

			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				if (Util.isThemeBlack()) {
					if (Bio7DarkThemeAdditions.isSet() == false) {
						Color colBackgr = Util.getSWTBackgroundToAWT(parent);
						Color colForegr = Util.getSWTForegroundToAWT(parent);
						new Bio7DarkThemeAdditions().applyMacBlackThemeAdditions(colBackgr, colForegr);
					}
				}
			} catch (ClassNotFoundException e) {

				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {

				e.printStackTrace();
			} catch (UnsupportedLookAndFeelException e) {
				e.printStackTrace();
			}

		}
		/* If Linux is the OS! */
		else if (isLinux) {
			if (Bio7LinuxTheme.isSet() == false) {
				MetalTheme theme = new Bio7LinuxTheme();

				MetalLookAndFeel.setCurrentTheme(theme);

				try {
					UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel"); //$NON-NLS-1$
					UIManager.getLookAndFeelDefaults().put("Panel.background", Util.getSWTBackgroundToAWT(parent));
					UIManager.getLookAndFeelDefaults().put("Panel.foreground", Util.getSWTForegroundToAWT(parent));

					if (Util.isThemeBlack()) {
						if (Bio7DarkThemeAdditions.isSet() == false) {
							Color colBackgr = Util.getSWTBackgroundToAWT(parent);
							Color colForegr = Util.getSWTForegroundToAWT(parent);
							new Bio7DarkThemeAdditions().applyLinuxBlackThemeAdditions(colBackgr, colForegr);
						}
					}
					// SwingUtilities.updateComponentTreeUI(this);
				} catch (Exception e) {
					System.out.println(e);
				}
			}
		}

		parent.addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {

				/* Save the ImageJ preferences when the view is closed! */
				try {
					ij.Prefs.savePreferences();
				} catch (RuntimeException ex) {

					ex.printStackTrace();
				}

			}
		});

		// AwtEnvironment awt = new AwtEnvironment(parent.getDisplay());

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.imagej");

		parent.addControlListener(new ControlAdapter() {
			@Override
			public synchronized void controlResized(final ControlEvent e) {
				/*
				 * Here we write the values in the com.eco.bio7 plugin preferences with the help
				 * of scoped preferences!
				 */
				Rectangle rec = parent.getClientArea();

				IPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, "com.eco.bio7");
				if (store != null) {
					String selection = store.getString("PLOT_DEVICE_SELECTION");
					String pathTo = store.getString("pathTempR");
					int correction = 0;
					if (tabFolder.isDisposed() == false && tabFolder != null) {
						/* Height correction for the plot! */
						correction = CanvasView.tabFolder.getTabHeight() + 40;
					}

					if (selection.equals("PLOT_IMAGEJ_DISPLAYSIZE_CAIRO")) {

						store.setValue("DEVICE_DEFINITION", ".bio7Device <- function(filename = \"" + pathTo + "tempDevicePlot%05d.tiff" + "\") { tiff(filename,width = " + (rec.width - 20)
								+ ", height = " + (rec.height - correction) + ", type=\"cairo\")}; options(device=\".bio7Device\")");
					} else if (selection.equals("PLOT_IMAGEJ_DISPLAYSIZE")) {
						store.setValue("DEVICE_DEFINITION", ".bio7Device <- function(filename = \"" + pathTo + "tempDevicePlot%05d.tiff" + "\") { tiff(filename,width =  " + (rec.width - 20)
								+ ", height = " + (rec.height - correction) + ", units = \"px\")}; options(device=\".bio7Device\")");

					}
				}
				/* Here we resize the ImageJ plot window! */
				// ImageWindow currentPlotWindow = WindowManager.getCurrentWindow();
				if (win != null) {
					// if (win instanceof PlotWindow) {
					/* Avoid the resizing of the CanvasView if a detached view is resized! */
					layoutParent(parent);
					SwingUtilities.invokeLater(new Runnable() {
						// !!
						public void run() {
							resizePlotWindow(parent, win);
						}
					});

					// }
				}
			}

		});

		getViewSite().getPage().addPartListener(new IPartListener() {
			public void partActivated(IWorkbenchPart part) {
				/*
				 * Focus necessary to made the key shortcuts work without an opened image.
				 * Wrapped in invokeLater to not deadlock when drag and drop many images on the
				 * GUI. syncExec to access the tabFolder!
				 */
				if (part instanceof CanvasView) {
					if (Util.getOS().equals("Mac")) {

						// Wrap to avoid deadlock of awt frame access!
						java.awt.EventQueue.invokeLater(new Runnable() {
							public void run() {
								if (win != null) {
									Frame frameSwtAwt = win.getSwtAwtMain().getFrame();
									if (frameSwtAwt != null) {

										if (frameSwtAwt != null)
											// frameSwtAwt.dispatchEvent(new WindowEvent(frameSwtAwt,
											// WindowEvent.WINDOW_ACTIVATED));
											frameSwtAwt.dispatchEvent(new WindowEvent(frameSwtAwt, WindowEvent.WINDOW_ACTIVATED));

									}
								}
							}
						});
						if (CanvasView.tabFolder != null && CanvasView.tabFolder.isDisposed() == false) {
							CanvasView.tabFolder.setVisible(false);

							CanvasView.tabFolder.setVisible(true);
						}
					}

					/*
					 * CTabItem ciTemp = new CTabItem(CanvasView.tabFolder, SWT.CLOSE,
					 * CanvasView.insertMark + 1); // CanvasView.tabFolder.showItem(ci); CTabItem
					 * selItem = CanvasView.tabFolder.getSelection();
					 * CanvasView.tabFolder.setSelection(ciTemp);
					 * 
					 * ciTemp.dispose(); if (selItem != null) {
					 * CanvasView.tabFolder.showItem(selItem);
					 * CanvasView.tabFolder.setSelection(selItem); }
					 */

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
							 * Execute on the event dispatching thread! Important for WorldWind which uses
							 * ImageJ! (else deadlock situation occurs!!!)
							 */
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									win.bio7TabClose(true);
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
		// Display display = Display.getDefault();
		this.parent2 = parent;

		new ImageJ();

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
		DropTarget dt = new DropTarget(tabFolder, DND.DROP_DEFAULT | DND.DROP_MOVE);
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

								final int x = i;

								if (Util.getOS().equals("Mac") == false) {
									dragAndDrop.openFile(new File(fileList[x].toString()));
									recalculateLayout();
								} else {

									dragAndDrop.openFile(new File(fileList[x].toString()));

								}
								
								monitor.worked(1);

							}
							monitor.done();
							return Status.OK_STATUS;
						}

					};
					job.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent event) {
							if (event.getResult().isOK()) {

							} else {

							}
						}
					});
					// job.setUser(true);
					job.schedule();

				}
			}
		});

		CommandLister hotkeys = new CommandLister();
		String[] shortcuts = hotkeys.getShortcuts();
		for (int i = 0; i < shortcuts.length; i++) {
			if (shortcuts[i].contains("\t^")) {
				shortcuts[i] += " (macro)";
			}

		}

		tabFolder.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {

				for (int i = 0; i < shortcuts.length; i++) {

					String[] splitShortcut = shortcuts[i].split("\t");
					splitShortcut[0] = splitShortcut[0].trim();
					if (splitShortcut[0].equals("" + e.character)) {
						IJ.doCommand(splitShortcut[1]);
					}
					/* Also allow the F keys for a shortcut! */
					if (splitShortcut[0].equals("F1") && e.keyCode == SWT.F1) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F2") && e.keyCode == SWT.F2) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F3") && e.keyCode == SWT.F3) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F4") && e.keyCode == SWT.F4) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F5") && e.keyCode == SWT.F5) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F6") && e.keyCode == SWT.F6) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F7") && e.keyCode == SWT.F7) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F8") && e.keyCode == SWT.F8) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F9") && e.keyCode == SWT.F9) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F10") && e.keyCode == SWT.F10) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F11") && e.keyCode == SWT.F11) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F12") && e.keyCode == SWT.F12) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F13") && e.keyCode == SWT.F13) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F14") && e.keyCode == SWT.F14) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F15") && e.keyCode == SWT.F15) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F16") && e.keyCode == SWT.F16) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F17") && e.keyCode == SWT.F17) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F18") && e.keyCode == SWT.F18) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F19") && e.keyCode == SWT.F19) {
						IJ.doCommand(splitShortcut[1]);
					} else if (splitShortcut[0].equals("F20") && e.keyCode == SWT.F20) {
						IJ.doCommand(splitShortcut[1]);
					}

				}

			}
		});

		tabFolder.setBorderVisible(true);
		// tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
		tabFolder.setSimple(false);

		// tabFolder.setSelectionBackground(new Color[] {
		// display.getSystemColor(SWT.COLOR_DARK_GREEN),
		// display.getSystemColor(SWT.COLOR_DARK_GREEN) }, new int[] { 90 },
		// true);
		// tabFolder.setSelectionForeground(display.getSystemColor(SWT.COLOR_WHITE));

		tabFolder.addCTabFolder2Listener(new CTabFolder2Listener() {

			public void close(final CTabFolderEvent event) {

				Vector ve = (Vector) event.item.getData();
				ImagePlus plu = (ImagePlus) ve.get(0);

				final ImageWindow win = (ImageWindow) ve.get(1);
				

				java.awt.EventQueue.invokeLater(new Runnable() {
					public void run() {
						win.bio7TabClose(true);
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
				Vector<?> ve = (Vector<?>) e.item.getData();
				plu = (ImagePlus) ve.get(0);

				win = (ImageWindow) ve.get(1);
				// Wrap to avoid deadlock of awt frame access!
				java.awt.EventQueue.invokeLater(new Runnable() {
					public void run() {
						WindowManager.setTempCurrentImage(plu);
						WindowManager.setCurrentWindow(win);
					}
				});

				/* import to set current Panel! */
				current = (JPanel) ve.get(2);
				// current.requestFocus();
				/*
				 * Here we resize a PlotWindow in thhe tabFolder when a tabItem has been
				 * selected the item plot windows have the same size!
				 */
				if (win instanceof PlotWindow) {

					layoutParent(parent);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							resizePlotWindow(parent, win);
						}
					});
				}

			}

		});
		tabFolder.addMouseListener(new MouseAdapter() {

			public void mouseDown(MouseEvent mouseevent)

			{

				/*
				 * Important to select the correct image and window when creating the new ImageJ
				 * view!The listener for the right-click on the tabitem will care about that!
				 */
				if (mouseevent.count == 1) {

					CTabFolder ctab = (CTabFolder) mouseevent.widget;

					if (ctab.getItemCount() <= 0) {

						CanvasView.tabFolder.setFocus();

					}

					else if (ctab.getItemCount() > 0) {
						Vector ve = (Vector) ctab.getSelection().getData();
						plu = (ImagePlus) ve.get(0);

						win = (ImageWindow) ve.get(1);

						// Wrap to avoid deadlock of awt frame access!
						java.awt.EventQueue.invokeLater(new Runnable() {
							public void run() {
								WindowManager.setTempCurrentImage(plu);
								WindowManager.setCurrentWindow(win);
							}
						});

						// important to set current Panel!
						current = (JPanel) ve.get(2); // current.requestFocus();
						plu.getCanvas().repaint();
						/* To show the status message! */
						win.repaint();

					}

				} else if (mouseevent.count == 2 && mouseevent.button == 1) {

					// IJ.getInstance().doCommand("Rename...");
				} else if (mouseevent.button == 3 && mouseevent.count > 1) {
					CTabFolder ctab = (CTabFolder) mouseevent.widget;
					if (ctab.getItemCount() > 0) {
						Vector ve = (Vector) ctab.getSelection().getData();
						ImagePlus plu = (ImagePlus) ve.get(0);

						ImageWindow win = (ImageWindow) ve.get(1);
						// JPanel current = (JPanel) ve.get(2);

						CustomDetachedImageJView custom = new CustomDetachedImageJView();
						/* Create ImageJ view with unique ID! */
						// String id = UUID.randomUUID().toString();
						/* Create ImageJ view with unique image ID of the ImagePlus image! */
						String id = Integer.toString(plu.getID());
						// System.out.println(id);
						// detachedSecViewIDs.add(id);
						custom.setPanel(current, id, plu.getTitle());
						custom.setData(plu, win);
						/*
						 * Only hide the tab without to close the ImagePlus object!
						 */
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

					// cLabel.setFont(null);
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
		// tbm.add(new PlaceholderLabel().getPlaceholderLabel());
		/* If we have a plugin which installs a main menu item! */
		Menus.updateBio7ImageJMenu();
	}

	public void setFocus() {

	}

	public void dispose() {
		//IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
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

	/*
	 * public ArrayList<String> getDetachedSecViewIDs() { return detachedSecViewIDs;
	 * }
	 */
	private void setComponentFont(Display dis) {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		boolean antialiasedFonts = false;
		antialiasedFonts = store.getBoolean("FONT_ANTIALIASED");
		if (antialiasedFonts) {
			System.setProperty("awt.useSystemAAFontSettings", "on");
			System.setProperty("swing.aatext", "true");
		}

		assert EventQueue.isDispatchThread(); // On AWT event thread

		FontData fontData = dis.getSystemFont().getFontData()[0];

		int resolution = Toolkit.getDefaultToolkit().getScreenResolution();

		int awtFontSize = (int) Math.round((double) fontData.getHeight() * dis.getDPI().y / 72.0);
		java.awt.Font awtFont = null;

		int fontSizeCorrection = 0;
		fontSizeCorrection = store.getInt("FONT_SIZE_CORRECTION");
		/* Font size correction! */

		awtFont = new java.awt.Font(fontData.getName(), fontData.getStyle(), awtFontSize + fontSizeCorrection);

		// Update the look and feel defaults to use new font.
		updateLookAndFeel(awtFont);

	}

	private void updateLookAndFeel(java.awt.Font awtFont) {
		assert awtFont != null;
		assert EventQueue.isDispatchThread(); // On AWT event thread

		FontUIResource fontResource = new FontUIResource(awtFont);

		UIManager.put("Button.font", fontResource); //$NON-NLS-1$
		UIManager.put("CheckBox.font", fontResource); //$NON-NLS-1$
		UIManager.put("ComboBox.font", fontResource); //$NON-NLS-1$
		UIManager.put("EditorPane.font", fontResource); //$NON-NLS-1$
		UIManager.put("Label.font", fontResource); //$NON-NLS-1$
		UIManager.put("List.font", fontResource); //$NON-NLS-1$
		UIManager.put("Panel.font", fontResource); //$NON-NLS-1$
		UIManager.put("ProgressBar.font", fontResource); //$NON-NLS-1$
		UIManager.put("RadioButton.font", fontResource); //$NON-NLS-1$
		UIManager.put("ScrollPane.font", fontResource); //$NON-NLS-1$
		UIManager.put("TabbedPane.font", fontResource); //$NON-NLS-1$
		UIManager.put("Table.font", fontResource); //$NON-NLS-1$
		UIManager.put("TableHeader.font", fontResource); //$NON-NLS-1$
		UIManager.put("TextField.font", fontResource); //$NON-NLS-1$
		UIManager.put("TextPane.font", fontResource); //$NON-NLS-1$
		UIManager.put("TitledBorder.font", fontResource); //$NON-NLS-1$
		UIManager.put("ToggleButton.font", fontResource); //$NON-NLS-1$
		UIManager.put("TreeFont.font", fontResource); //$NON-NLS-1$
		UIManager.put("ViewportFont.font", fontResource); //$NON-NLS-1$
		UIManager.put("MenuItem.font", fontResource); //$NON-NLS-1$
		UIManager.put("CheckboxMenuItem.font", fontResource); //$NON-NLS-1$
		UIManager.put("PopupMenu.font", fontResource); // $NON-NLS-1

		java.util.Enumeration keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof javax.swing.plaf.FontUIResource)
				UIManager.put(key, fontResource);
		}

	}

}
