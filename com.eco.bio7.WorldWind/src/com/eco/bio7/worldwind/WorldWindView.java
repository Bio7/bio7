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

package com.eco.bio7.worldwind;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.rosuda.REngine.Rserve.RConnection;

import com.eco.bio7.image.Util;
import com.eco.bio7.worldwind.swt.NewtInputHandlerSWT;
import com.eco.bio7.worldwind.swt.StatusBar;
import com.eco.bio7.worldwind.swt.WorldWindowNewtAutoDrawableSWT;
import com.eco.bio7.worldwind.swt.WorldWindowNewtCanvasSWT;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.Screen;
import com.jogamp.newt.opengl.GLWindow;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.globes.EarthFlat;
import gov.nasa.worldwind.globes.FlatGlobe;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.SkyColorLayer;
import gov.nasa.worldwind.layers.SkyGradientLayer;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.nasa.worldwind.view.orbit.FlatOrbitView;

public class WorldWindView extends ViewPart {

	private static WorldWindView viewInstance;
	private static int projectioMode;

	public WorldWindView() {
		viewInstance = this;
	}

	private static final String WWJ_SPLASH_PATH = "images/400x230-splash-nww.png";
	private static final String GEORSS_ICON_PATH = "images/georss.png";
	private static final String NASA_ICON_PATH = "images/32x32-icon-nasa.png";
	public static final String ID = "com.eco.bio7.worldwind.WorldWindView"; //$NON-NLS-1$
	private static WorldWindowNewtCanvasSWT worldCanvas;
	private static RConnection rConnection;
	private ConcurrentHashMap<String, SurfaceImage> imageTable = new ConcurrentHashMap<String, SurfaceImage>();
	private Frame worldFrame;
	private SurfaceImage si;
	private RenderableLayer layer;
	public Text combo;
	public Button flyToButton;
	private ToolTip tip;
	public ToolBar infoButton;
	private Fullscreen full;
	private StatusBar statusBar;
	private Composite top;
	private Panel panel;
	private WorldWindView view;
	protected int tempSizeY;
	protected int tempSizeX;
	protected boolean detached = false;
	private static Earth roundEarthModel;
	private static EarthFlat flatEarthModel;

	public static WorldWindowNewtCanvasSWT getWwd() {
		WorldWindowNewtCanvasSWT canvas;
		if (worldCanvas != null) {
			canvas = worldCanvas;
		} else {
			canvas = null;
		}
		return canvas;
	}

	public Fullscreen getFull() {
		return full;
	}

	public void setFull(Fullscreen full) {
		this.full = full;
	}

	public static void setFullscreen() {
		setFullscreen(0);
	}

	public static void setFullscreen(int i) {
		GLWindow window = worldCanvas.getWindow();
		if (window.isFullscreen()) {
			window.setFullscreen(false);
		} else {

			Job job = new Job("Fullscreen") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Fullscreen mode ...", 20);
					Screen screen = window.getScreen();
					List<MonitorDevice> monitorDevices = new ArrayList<>();
					// MonitorDevice dev = screen.getMonitorDevices().get(i);
					while (screen.getMonitorDevices() == null && monitor.isCanceled() == false) {
						monitor.worked(1);
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					MonitorDevice dev = screen.getMonitorDevices().get(i);
					if (dev != null) {
						monitorDevices.add(dev);
						/* Workaround for windows to correctly set fullscreen coordinates! */
						if (Util.getOS().equals("Windows")) {
							window.setVisible(false);
							window.setFullscreen(monitorDevices);
							window.setFullscreen(false);
							window.setFullscreen(monitorDevices);
							window.setVisible(true);
						}

						else {

							window.setFullscreen(monitorDevices);

						}

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
			job.schedule();

		}

	}

	public static WorldWindView getInstance() {
		return viewInstance;
	}

	/**
	 * Create contents of the view part
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {

		Shell sh = new Shell(Util.getDisplay());
		sh.setVisible(false);
		/* Create a WorldWind instance */
		/* Workaround to set the images for Bio7 again on MacOSX! */
		Image[] imagesTemp = Util.getShell().getImages();
		top = parent;
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		Configuration.setValue(AVKey.INPUT_HANDLER_CLASS_NAME, NewtInputHandlerSWT.class.getName());
		Configuration.setValue(AVKey.WORLD_WINDOW_CLASS_NAME, WorldWindowNewtAutoDrawableSWT.class.getName());

		worldCanvas = new WorldWindowNewtCanvasSWT(parent, SWT.NONE, null);
		// worldCanvas = new WorldWindowGLCanvas();

		initWorldWindLayerModel();

		parent.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				/* Detached views have not title! */
				if (Util.getOS().equals("Mac")) {
					boolean isDetached = parent.getShell().getText().length() == 0;
					if (isDetached) {

						if (detached == false) {
							// System.out.println("detach");
							worldCanvas.getNEWTChild().setFullscreen(true);
							worldCanvas.getNEWTChild().setFullscreen(false);
							detached = true;
						}

					} else {
						if (detached == true) {
							// System.out.println("attach");
							worldCanvas.getNEWTChild().setFullscreen(true);
							worldCanvas.getNEWTChild().setFullscreen(false);
							detached = false;
						}

					}
				}
			}
		});

		IPartListener2 pl = new IPartListener2() {

			@Override
			public void partActivated(IWorkbenchPartReference partRef) {

				if (partRef.getId().equals("com.eco.bio7.worldwind.WorldWindView")) {

				}
			}

			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals("com.eco.bio7.worldwind.WorldWindView")) {

				}
			}

			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				// TODO Auto-generated method stub

			}

			@Override
			public void partDeactivated(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals("com.eco.bio7.worldwind.WorldWindView")) {

				}

			}

			@Override
			public void partOpened(IWorkbenchPartReference partRef) {

			}

			@Override
			public void partHidden(IWorkbenchPartReference partRef) {

				if (partRef.getId().equals("com.eco.bio7.worldwind.WorldWindView")) {
					if (Util.getOS().equals("Mac")) {
						if (worldCanvas.getNEWTChild() != null) {
							worldCanvas.getNEWTChild().setVisible(false);
							// spat.getCanvas().getNEWTChild().setPosition(0, 0);
						}
					}
				}

			}

			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals("com.eco.bio7.worldwind.WorldWindView")) {
					/*
					 * if (parent.isDisposed() == false) { Rectangle rec = parent.getClientArea();
					 * worldCanvas.getNEWTChild().setSize(rec.width, rec.height); }
					 */
					if (Util.getOS().equals("Mac")) {
						if (parent.isDisposed() == false) {
							// Rectangle rec = parent.getClientArea();
							Display display = PlatformUI.getWorkbench().getDisplay();
							display.asyncExec(new Runnable() {
								public void run() {
									// spat.getCanvas().setParent(parent);
									worldCanvas.getNEWTChild().setVisible(true);
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

		try {
			page.showView("com.eco.bio7.worldwind.WorldWindOptionsView");
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		page.addPartListener(pl);

		/*
		 * Composite composite = new Composite(shell, SWT.EMBEDDED); Frame frame =
		 * SWT_AWT.new_Frame(composite); frame.setLayout(new BorderLayout());
		 * Configuration.setValue(AVKey.INPUT_HANDLER_CLASS_NAME,
		 * NewtInputHandlerAWT.class.getName());
		 * Configuration.setValue(AVKey.WORLD_WINDOW_CLASS_NAME,
		 * WorldWindowNewtAutoDrawableAWT.class.getName()); WorldWindowNewtCanvasAWT wwd
		 * = new WorldWindowNewtCanvasAWT(); frame.add(wwd, BorderLayout.CENTER);
		 */

		/*
		 * Model m = (Model)
		 * WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
		 * worldCanvas.setModel(m);
		 */

		// initWorldWindLayerModel();

		roundEarthModel = new Earth();
		flatEarthModel = new EarthFlat();
		/*
		 * statusBar = new StatusBar(this); // Set color for dark theme necessary!
		 * statusBar.setEventSource(worldCanvas);
		 */
		/*
		 * top = new Composite(parent, SWT.NO_BACKGROUND | SWT.EMBEDDED); try {
		 * System.setProperty("sun.awt.noerasebackground", "true"); } catch
		 * (NoSuchMethodError error) { } top.setLayoutData(new
		 * GridData(GridData.FILL_BOTH));
		 * 
		 * // Swing Frame and Panel worldFrame = SWT_AWT.new_Frame(top);
		 * SwtAwt.setSwtAwtFocus(worldFrame, top); panel = new java.awt.Panel(new
		 * java.awt.BorderLayout());
		 * 
		 * worldFrame.add(panel);
		 * 
		 * // Add the WWJ 3D OpenGL Canvas to the Swing Panel panel.add(worldCanvas,
		 * BorderLayout.CENTER);
		 * 
		 * parent.setLayoutData(new GridData(GridData.FILL_BOTH)); statusBar = new
		 * StatusBar(); Set color for dark theme necessary!
		 * statusBar.setBackground(Util.getSWTBackgroundToAWT());
		 * statusBar.setForeground(Util.getSWTForegroundToAWT());
		 * worldFrame.add(statusBar, BorderLayout.PAGE_END);
		 * statusBar.setEventSource(worldCanvas); initializeToolBar();
		 * 
		 * roundEarthModel = new Earth(); flatEarthModel = new EarthFlat();
		 */
		Util.getShell().setImages(imagesTemp);

	}

	public void setStatusline(String message) {
		IActionBars bars = getViewSite().getActionBars();
		bars.getStatusLineManager().setMessage(message);

	}

	public static boolean isFlatProjection() {
		return worldCanvas.getModel().getGlobe() instanceof FlatGlobe;
	}

	public void setFlatProjection(boolean flat) {
		boolean isFlat = worldCanvas.getModel().getGlobe() instanceof FlatGlobe;

		if (flat == isFlat)
			return;

		if (!flat) {

			worldCanvas.getModel().setGlobe(roundEarthModel);
			FlatOrbitView flatOrbitView = (FlatOrbitView) worldCanvas.getView();
			BasicOrbitView orbitView = new BasicOrbitView();

			orbitView.setCenterPosition(flatOrbitView.getCenterPosition());
			orbitView.setZoom(flatOrbitView.getZoom());
			orbitView.setHeading(flatOrbitView.getHeading());
			orbitView.setPitch(flatOrbitView.getPitch());

			LayerList layers = worldCanvas.getModel().getLayers();
			for (int i = 0; i < layers.size(); i++) {
				if (layers.get(i) instanceof SkyColorLayer)
					layers.set(i, new SkyGradientLayer());
			}

			worldCanvas.setView(orbitView);

		} else {

			worldCanvas.getModel().setGlobe(flatEarthModel);
			FlatOrbitView flatOrbitView = new FlatOrbitView();
			switch (projectioMode) {
			case 0:
				flatEarthModel.setProjection(FlatGlobe.PROJECTION_LAT_LON);

				break;
			case 1:
				flatEarthModel.setProjection(FlatGlobe.PROJECTION_MERCATOR);
				break;
			case 2:
				flatEarthModel.setProjection(FlatGlobe.PROJECTION_MODIFIED_SINUSOIDAL);
				break;
			case 3:
				flatEarthModel.setProjection(FlatGlobe.PROJECTION_SINUSOIDAL);
				break;

			default:
				break;
			}

			BasicOrbitView orbitView = (BasicOrbitView) worldCanvas.getView();

			flatOrbitView.setCenterPosition(orbitView.getCenterPosition());
			flatOrbitView.setZoom(orbitView.getZoom());
			flatOrbitView.setHeading(orbitView.getHeading());
			flatOrbitView.setPitch(orbitView.getPitch());

			LayerList layers = worldCanvas.getModel().getLayers();
			for (int i = 0; i < layers.size(); i++) {
				if (layers.get(i) instanceof SkyGradientLayer)
					layers.set(i, new SkyColorLayer());
			}

			worldCanvas.setView(flatOrbitView);
			worldCanvas.redraw();
			// worldCanvas.setInputHandler(worldCanvas.getInputHandler());
		}
	}

	public static void setProjectionMode(int proj) {
		projectioMode = proj;

	}

	public void createFullscreen(int i) {

		WorldWindOptionsView.measureTool.getLayer().removeAllRenderables();
		GLWindow window = worldCanvas.getWindow();

		Screen screen = window.getScreen();
		List<MonitorDevice> monitorDevices = new ArrayList<>();
		List<MonitorDevice> monitorDevicesAvailable = screen.getMonitorDevices();
		if (i < monitorDevicesAvailable.size()) {
			MonitorDevice dev = monitorDevicesAvailable.get(i);
			if (dev != null) {
				monitorDevices.add(dev);
				/* Workaround for windows to correctly set fullscreen coordinates! */
				if (Util.getOS().equals("Windows")) {
					window.setVisible(false);
					window.setFullscreen(monitorDevices);
					window.setFullscreen(false);
					window.setFullscreen(monitorDevices);
					window.setVisible(true);
				}

				else {

					window.setFullscreen(monitorDevices);

				}
				window.setFullscreen(true);
			}
		}

	}

	public void recreateGLCanvas() {
		GLWindow window = worldCanvas.getWindow();
		window.setFullscreen(false);
		// worldFrame.add(worldCanvas);
		// worldFrame.add(statusBar, BorderLayout.PAGE_END);
		// worldFrame.validate();
		WorldWindOptionsView.optionsInstance.createMeasureTool();

	}

	public void addImage(String imagePath) throws IOException {
		if (imagePath == null) {
			String message = Logging.getMessage("nullValue.ImageSourceIsNull");
			Logging.logger().severe(message);
			throw new IllegalArgumentException(message);
		}

		File imageFile = new File(imagePath);
		BufferedImage image = ImageIO.read(imageFile);

		File worldFile = getWorldFile(imageFile.getAbsoluteFile());
		if (worldFile == null || !worldFile.exists()) {
			System.out.println("World file for " + imagePath + "does not exist"); // TODO
			si = new SurfaceImage(image, Sector.fromDegrees(35, 45, -115, -95));
		} else {
			Sector sector = decodeWorldFile(worldFile, image.getWidth(), image.getHeight());
			if (sector == null) {
				System.out.println("World file for " + imagePath + "can not be decoded"); // TODO
				si = new SurfaceImage(image, Sector.fromDegrees(35, 45, -115, -95));
			} else {
				si = new SurfaceImage(image, sector);

			}
		}
		if (imageTable.contains(imagePath))
			removeImage(imagePath);
		layer = new RenderableLayer();
		layer.setName("Surface Images");
		layer.addRenderable(si);
		insertBeforeCompass(worldCanvas, layer);

		imageTable.put(imagePath, si);
	}

	public void removeImage(String imagePath) {
		SurfaceImage si = this.imageTable.get(imagePath);
		if (si != null) {
			layer.removeRenderable(si);
			imageTable.remove(imagePath);
		}
	}

	private static File getWorldFile(File imageFile) {
		File dir = imageFile.getParentFile();
		final String base = WWIO.replaceSuffix(imageFile.getName(), "");

		File[] wfiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.startsWith(base) && name.toLowerCase().endsWith("w");
			}
		});

		return (wfiles != null && wfiles.length > 0) ? wfiles[0] : null;
	}

	private static Sector decodeWorldFile(File wf, int imageWidth, int imageHeight) throws FileNotFoundException {
		Scanner scanner = new Scanner(wf);
		System.out.print(wf);
		double[] values = new double[6];

		for (int i = 0; i < 6; i++) {
			if (scanner.hasNextDouble()) {
				values[i] = scanner.nextDouble();
				System.out.print(values[i]);
			} else {
				System.out.println("World file missing value at line " + (i + 1));
				return null;
			}

		}

		Sector sector = parseDegrees(values, imageWidth, imageHeight);

		return sector;
	}

	private static Sector parseDegrees(double[] values, int imageWidth, int imageHeight) {
		Angle latOrigin = Angle.fromDegrees(values[5]);
		Angle latOffset = latOrigin.addDegrees(values[3] * imageHeight);
		Angle lonOrigin = Angle.fromDegrees(values[4]);
		Angle lonOffset = lonOrigin.addDegrees(values[0] * imageWidth);

		Angle minLon, maxLon;
		if (lonOrigin.degrees < lonOffset.degrees) {
			minLon = lonOrigin;
			maxLon = lonOffset;
		} else {
			minLon = lonOffset;
			maxLon = lonOrigin;
		}

		Angle minLat, maxLat;
		if (lonOrigin.degrees < lonOffset.degrees) {
			minLat = latOrigin;
			maxLat = latOffset;
		} else {
			minLat = latOffset;
			maxLat = latOrigin;
		}

		return new Sector(minLat, maxLat, minLon, maxLon);
	}

	public static void insertBeforeCompass(WorldWindow wwd, Layer layer) {
		// Insert the layer into the layer list just before the compass.
		int compassPosition = 0;
		LayerList layers = wwd.getModel().getLayers();
		for (Layer l : layers) {
			if (l instanceof CompassLayer)
				compassPosition = layers.indexOf(l);
		}
		layers.add(compassPosition, layer);
	}

	/*
	 * Initialize WW model with default layers
	 */

	static void initWorldWindLayerModel() {
		Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);

		worldCanvas.setModel(m);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
	}

	@Override
	public void dispose() {

		worldCanvas.shutdown();
		worldCanvas = null;

		Display display = PlatformUI.getWorkbench().getDisplay();

		display.syncExec(new Runnable() {
			public void run() {
				IWorkbenchPage wbp = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

				wbp.hideView(wbp.findView("com.eco.bio7.worldwind.WorldWindOptionsView"));

			}

		});

		super.dispose();
	}

	/**
	 * Create the actions
	 */
	private void createActions() {
		// Create the actions
	}

	/**
	 * Initialize the toolbar
	 */
	private void initializeToolBar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();

	}

	/**
	 * Initialize the menu
	 */
	private void initializeMenu() {
		IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
	}

	/**
	 * WindowBuilder generated method.<br>
	 * Please don't remove this method or its invocations.<br>
	 * It used by WindowBuilder to associate the {@link javax.swing.JPopupMenu} with
	 * parent.
	 */
	private static void addPopup(Component component, final JPopupMenu popup) {
		component.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger())
					showMenu(e);
			}

			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger())
					showMenu(e);
			}

			private void showMenu(MouseEvent e) {
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		});
	}

	public static void setRConnection(RConnection c) {
		rConnection = c;

	}

	public static RConnection getRConnection() {
		return rConnection;

	}

	public Panel getPanel() {
		return panel;
	}

	public Composite getTopComposite() {
		return top;
	}

}
