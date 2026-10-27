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

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import java.awt.image.ColorModel;

import ij.ImagePlus;
import ij.WindowManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.info.LinearSlider;
import com.eco.bio7.info.SpeedSlider;
import com.eco.bio7.spatial.preferences.Bio7PrefConverterSpatial;
import com.eco.bio7.spatial.preferences.Point3d;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;

public class Options3d extends ViewPart {
	public static final String ID = "com.eco.bio7.discrete3d.Options3d"; //$NON-NLS-1$

	private CTabItem newItemExpandItem;
	private Text worldExtent;
	private Text text_11;
	private Text lookAtZ;
	private Text lookAtY;
	private Text lookAtX;
	private Text splitCamZ;
	private Text splitCamY;
	private Text splitCamX;
	private static Text text;
	private IPreferenceStore store;
	private static String objectPath;
	private static String objectName;
	private Label sizeXLabel;
	private Label sizeYLabel;
	private Label sizeZLabel;
	private LinearSlider scale_2;
	private LinearSlider scale_1;
	private LinearSlider scale;
	private Label errorLabelSplit;
	private DisplayMode[] d;
	private Button showGridButton;
	private Button showAxesButton;
	private Button octantButton;
	private Button showQuadButton;
	/* The axis numbers (enabled with the plot view) */
	private Button btnDrawnumbers;
	/* The grid planes XY, XZ, YZ of the plot view (see updatePlotGrids) */
	private static final Button[] plotGridButtons = new Button[3];
	private Point3d p;
	private Point3d p2;
	private Point3d p3;
	private boolean fixed;
	private Label worldExtentError;
	private double we = 0;
	private double halfExtent;// Dimension for the grid!
	private Spinner multiplyDragCameraSpeedSpinner;
	private static Spinner setFixedFPS;
	private static Button playPauseButton;
	private static Button fixedFpsButton;
	private static boolean renderImageJFrames = true;
	private static int stackSizeCount = 100;
	private static int fpsScreenCapture = 30;
	private int fixedFps;

	private Image imagePlayPause;

	private Image imageSetup;

	/* The parent of the content in the view and the composite of the part (its parent)! */
	private Composite viewParent;
	private Composite partComposite;

	public Options3d() {

	}

	/**
	 * Create contents of the view part
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		viewParent = parent;
		partComposite = parent.getParent();
		createContents(parent);
		/*
		 * The content can be moved into another composite (e.g., into a custom view
		 * of a model), then it is created again for this view!
		 */
		getSite().getPage().addPartListener(partListener);
		createActions();
		// initializeToolBar();
		initializeMenu();
	}

	/*
	 * Creates the content again if it has been moved into another composite or
	 * disposed (e.g., with the custom view of a model). The composite of the view
	 * itself can be moved, too: then a new one is created in the composite of the
	 * part.
	 */
	private void ensureContents() {
		if (partComposite == null || partComposite.isDisposed()) {
			return;
		}
		boolean moved = viewParent == null || viewParent.isDisposed() || viewParent.getParent() != partComposite;
		if (!moved && viewParent.getChildren().length > 0) {
			return;
		}
		if (moved) {
			viewParent = new Composite(partComposite, SWT.NONE);
		}
		/* A new instance, so that the moved content keeps its own fields (its listeners work)! */
		new Options3d().createContents(viewParent);
		partComposite.layout(true, true);
	}

	private final IPartListener2 partListener = new IPartListener2() {
		@Override
		public void partVisible(IWorkbenchPartReference ref) {
			if (ref.getPart(false) == Options3d.this) {
				ensureContents();
			}
		}

		@Override
		public void partActivated(IWorkbenchPartReference ref) {
			if (ref.getPart(false) == Options3d.this) {
				ensureContents();
			}
		}
	};

	/* The content of the view (in the parent with a FillLayout)! */
	private void createContents(Composite parent) {
		parent.setLayout(new FillLayout());
		store = Bio7Plugin.getDefault().getPreferenceStore();
		/* Get the preferences for the model! */
		p = Bio7PrefConverterSpatial.getPoint(store, "modelLocation");
		p2 = Bio7PrefConverterSpatial.getPoint(store, "modelRotation");
		p3 = Bio7PrefConverterSpatial.getPoint(store, "modelScale");
		store.setDefault("fps", true);
		fixed = store.getBoolean("fps");
		we = store.getDouble("spatialExtent");
		/* Avoid division with zero! */
		if (we > 0) {
			halfExtent = we / 2;
		} else {
			halfExtent = 50000.0;
		}
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FillLayout());

		CTabFolder expandBar;
		expandBar = new CTabFolder(container, SWT.V_SCROLL);
		// expandBar.setBackground(parent.getBackground());

		newItemExpandItem = new CTabItem(expandBar, SWT.NONE);
		// newItemExpandItem.setHeight(700);
		newItemExpandItem.setText("Space");

		ScrolledComposite scroll = new ScrolledComposite(expandBar, SWT.V_SCROLL);
		final Composite composite = new Composite(scroll, SWT.NONE);
		newItemExpandItem.setControl(scroll);
		composite.setLayout(new GridLayout(2, true));

		scroll.setMinSize(200, 760);
		scroll.setContent(composite);
		scroll.setExpandVertical(true);
		scroll.setExpandHorizontal(true);
		scroll.setAlwaysShowScrollBars(true);

		expandBar.setSelection(newItemExpandItem);
		GraphicsDevice graphicDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		/*
		 * DisplayMode defaultMode = graphicDevice.getDisplayMode();
		 * SpatialUtil.setFullscreenOptions(defaultMode.getWidth(),
		 * defaultMode.getHeight(), defaultMode.getBitDepth(),
		 * defaultMode.getRefreshRate());
		 */
		d = graphicDevice.getDisplayModes();

		/* The size of the space (the axes from -size to +size) */
		final Group sizeGroup = createGroup(composite, "Space Size", 3, false);
		scale = sizeSlider(sizeGroup, "X", "spatialX", v -> {
			SpatialStructure sg = SpatialStructure.getSpatialStructureInstance();
			if (sg != null) {
				sg.setSizeX(v);
			}
		});
		sizeXLabel = (Label) sizeGroup.getChildren()[sizeGroup.getChildren().length - 1];
		scale_1 = sizeSlider(sizeGroup, "Y", "spatialY", v -> {
			SpatialStructure sg = SpatialStructure.getSpatialStructureInstance();
			if (sg != null) {
				sg.setSizeY(v);
			}
		});
		sizeYLabel = (Label) sizeGroup.getChildren()[sizeGroup.getChildren().length - 1];
		scale_2 = sizeSlider(sizeGroup, "Z", "spatialZ", v -> {
			SpatialStructure sg = SpatialStructure.getSpatialStructureInstance();
			if (sg != null) {
				sg.setSizeZ(v);
			}
		});
		sizeZLabel = (Label) sizeGroup.getChildren()[sizeGroup.getChildren().length - 1];

		/* The display of the space */
		final Group displayGroup = createGroup(composite, "Display");
		octantButton = check(displayGroup, "Octant", "Changes the visualization to the first octant.");
		octantButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure.hMap.setCartesian(!SpatialStructure.hMap.isCartesian());
			}
		});
		showAxesButton = check(displayGroup, "Show Axes", "Shows the x, y and z axes");
		showAxesButton.setSelection(true);
		showAxesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure sg = SpatialStructure.getSpatialStructureInstance();
				if (sg != null) {
					sg.showAxes = showAxesButton.getSelection();
				}
			}
		});
		showQuadButton = check(displayGroup, "Show Box", "Shows the box fitted to the displayed data (Cartesian mode)");
		showQuadButton.setSelection(true);
		showQuadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure sg = SpatialStructure.getSpatialStructureInstance();
				if (sg != null) {
					sg.showQuad = showQuadButton.getSelection();
				}
			}
		});
		/* The axis orientation overlay in the top right corner (stored in the preferences)! */
		final Button axisGizmoButton = check(displayGroup, "Axis Orientation",
				"Shows the orientation of the x, y and z axes in the top right corner of the 3D view");
		store.setDefault("showAxisGizmo", true);
		axisGizmoButton.setSelection(store.getBoolean("showAxisGizmo"));
		axisGizmoButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				store.setValue("showAxisGizmo", axisGizmoButton.getSelection());
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.showAxisGizmo = axisGizmoButton.getSelection();
				}
			}
		});
		btnDrawnumbers = check(displayGroup, "Draw Axis Numbers", "Draws the numbers and ticks of the axes");
		btnDrawnumbers.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SpatialStructure st = SpatialStructure.getSpatialStructureInstance();
				if (st != null && st.getAxisLabelsRenderer() != null) {
					st.getAxisLabelsRenderer().setEnabled(btnDrawnumbers.getSelection());
				}
			}
		});
		SpatialStructure current3d = SpatialStructure.getSpatialStructureInstance();
		btnDrawnumbers.setSelection(current3d != null && current3d.getAxisLabelsRenderer() != null
				? current3d.getAxisLabelsRenderer().isEnabled() : store.getBoolean("plotView"));
		/* Label the x and y axes with the pixel coordinates of a displayed texture! */
		AxisLabelsRenderer.setImageCoordinates(store.getBoolean("axisImageCoordinates"));
		final Button imageCoordinatesButton = check(displayGroup, "Image Coordinates",
				"Displays the axis numbers as pixel coordinates of the texture image.\nThe y coordinates increase from the top of the image like in ImageJ.");
		imageCoordinatesButton.setSelection(AxisLabelsRenderer.isImageCoordinates());
		imageCoordinatesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				AxisLabelsRenderer.setImageCoordinates(imageCoordinatesButton.getSelection());
				store.setValue("axisImageCoordinates", imageCoordinatesButton.getSelection());
			}
		});

		/* The grids: through the origin, at the far sides of the box and of the plot view */
		final Group gridGroup = createGroup(composite, "Grid", 3, true);
		showGridButton = check(gridGroup, "Show Grid", "Shows the grids");
		showGridButton.setSelection(true);
		showGridButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.showGrid = showGridButton.getSelection();
				}
			}
		});
		final Label girdSizeLabel = new Label(gridGroup, SWT.NONE);
		girdSizeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
		girdSizeLabel.setText("Grid Size");
		final Spinner spinner_12 = new Spinner(gridGroup, SWT.BORDER);
		spinner_12.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		spinner_12.setMaximum(1000);
		spinner_12.setMinimum(1);
		spinner_12.setSelection(50);
		spinner_12.setToolTipText("The distance of the grid lines");
		spinner_12.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.gridSize = spinner_12.getSelection();
				}
			}
		});
		String[] planes = { "XY", "XZ", "YZ" };
		/* The grids through the origin (XY on by default) */
		for (int k = 0; k < 3; k++) {
			final int plane = k;
			final Button button = check(gridGroup, planes[k], "The " + planes[k] + " grid through the origin");
			button.setSelection(k == 0);
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent e) {
					SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
					if (grid != null) {
						boolean selected = button.getSelection();
						if (plane == 0) {
							grid.layerXYGrid = selected;
						} else if (plane == 1) {
							grid.layerXZGrid = selected;
						} else {
							grid.layerYZGrid = selected;
						}
					}
				}
			});
		}
		/* The grids at the far sides of the box (fitted to the displayed rendering)! */
		String[] maxNames = { "XY (Z Max)", "XZ (Y Max)", "YZ (X Max)" };
		for (int k = 0; k < 3; k++) {
			final int plane = k;
			final Button button = check(gridGroup, maxNames[k],
					"The " + planes[k] + " grid at the far side (" + maxNames[k].substring(4, 5).toLowerCase() + " max) of the box");
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent e) {
					SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
					if (grid != null) {
						boolean selected = button.getSelection();
						if (plane == 0) {
							grid.layerXYGridMax = selected;
						} else if (plane == 1) {
							grid.layerXZGridMax = selected;
						} else {
							grid.layerYZGridMax = selected;
						}
					}
				}
			});
		}
		/* The grid planes of the plot view (see the Heightmap tab), behind the data! */
		for (int k = 0; k < 3; k++) {
			final int plane = k;
			final Button button = check(gridGroup, planes[k] + " (Plot)",
					"The " + planes[k] + " grid of the plot view (behind the data, see 'Plot View' in the Heightmap tab)");
			/* The floor (XY) is hidden by the height map by default */
			SpatialStructure current = SpatialStructure.getSpatialStructureInstance();
			button.setSelection(current != null ? new boolean[] { current.plotGridXY, current.plotGridXZ, current.plotGridYZ }[k] : k != 0);
			plotGridButtons[k] = button;
			button.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent e) {
					SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
					if (grid != null) {
						boolean selected = button.getSelection();
						if (plane == 0) {
							grid.plotGridXY = selected;
						} else if (plane == 1) {
							grid.plotGridXZ = selected;
						} else {
							grid.plotGridYZ = selected;
						}
					}
				}
			});
		}

		/* The colors of the background and the lines */
		final Group colorGroup = createGroup(composite, "Colors");
		final Button colourButton = new Button(colorGroup, SWT.NONE);
		colourButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		colourButton.setText("Background Color...");
		colourButton.setToolTipText("The background color of the space");
		colourButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				RGB color = getColour();
				if (color != null) {
					SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
					if (grid != null) {
						grid.setColorBackground(new float[] { color.red / 255f, color.green / 255f, color.blue / 255f, 1.0f });
						grid.backgroundColour = true;
					}
				}
			}
		});
		final Button colourLinesButton = new Button(colorGroup, SWT.NONE);
		colourLinesButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		colourLinesButton.setText("Line Color...");
		colourLinesButton.setToolTipText("The color of the grids, axes and the box");
		colourLinesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				RGB color = getColour();
				if (color != null) {
					SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
					if (grid != null) {
						grid.getScene().setColorCubeLines(new float[] { color.red / 255f, color.green / 255f, color.blue / 255f, 1.0f });
					}
				}
			}
		});

		/* The camera: the drag speed, the extent of the world and the view distance */
		final Group cameraGroup = createGroup(composite, "Camera");
		final Label lblSpeedDistance = new Label(cameraGroup, SWT.NONE);
		lblSpeedDistance.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		lblSpeedDistance.setText("Speed Drag Distance");
		multiplyDragCameraSpeedSpinner = new Spinner(cameraGroup, SWT.BORDER);
		multiplyDragCameraSpeedSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		multiplyDragCameraSpeedSpinner.setToolTipText("A factor for the speed of the zoom with the mouse");
		multiplyDragCameraSpeedSpinner.setMaximum(10000);
		multiplyDragCameraSpeedSpinner.setMinimum(1);
		multiplyDragCameraSpeedSpinner.setSelection(1);
		multiplyDragCameraSpeedSpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SpatialStructure.setMultiplyDragCameraSpeed(multiplyDragCameraSpeedSpinner.getSelection());
			}
		});
		final Label worldExtentLabel = new Label(cameraGroup, SWT.NONE);
		worldExtentLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		worldExtentLabel.setText("World Extent");
		worldExtent = new Text(cameraGroup, SWT.BORDER);
		worldExtent.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		worldExtent.setToolTipText("Set the world extent");
		worldExtent.setText("100000.0");
		worldExtent.addListener(SWT.Modify, new Listener() {

			public void handleEvent(Event event) {

				try {
					double extent = Double.parseDouble(worldExtent.getText());
					store.setValue("spatialExtent", extent);
					worldExtent.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
					worldExtentError.setText("");
				} catch (Exception e) {
					worldExtent.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
					worldExtentError.setText("Number input incorrect!");
				}
			}

		});
		worldExtentError = new Label(cameraGroup, SWT.NONE);
		worldExtentError.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		Button btnResetView = new Button(cameraGroup, SWT.NONE);
		btnResetView.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		btnResetView.setText("Reset View Distance");
		btnResetView.setToolTipText("The default view distance (fitted to the displayed data)");
		btnResetView.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
				if (struc != null) {
					/* Fitted to a displayed texture! */
					struc.setDoz(struc.getDefaultViewDistance());
				}
			}
		});

		if (we != 0) {
			worldExtent.setText("" + we);
		}

		final CTabItem newItemExpandItem_1 = new CTabItem(expandBar, SWT.NONE);
		// newItemExpandItem_1.setHeight(350);
		newItemExpandItem_1.setText("Time");
		ScrolledComposite scroll_2 = new ScrolledComposite(expandBar, SWT.V_SCROLL);
		final Composite composite_2 = new Composite(scroll_2, SWT.NONE);
		newItemExpandItem_1.setControl(scroll_2);
		composite_2.setLayout(new GridLayout(2, true));

		scroll_2.setMinSize(200, 350);
		scroll_2.setContent(composite_2);
		scroll_2.setExpandVertical(true);
		scroll_2.setExpandHorizontal(true);
		scroll_2.setAlwaysShowScrollBars(true);

		playPauseButton = new Button(composite_2, SWT.TOGGLE);
		GridData gd_playPauseButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_playPauseButton.heightHint = 40;
		playPauseButton.setLayoutData(gd_playPauseButton);
		playPauseButton.setToolTipText(
				"Play/Pause: Invokes the compiled run method with the default Jogl timer.\n The timer fps determines the speed of invocation");
		/* Selected if the run method is invoked (the current state, e.g., started by a script) */
		SpatialStructure running = SpatialStructure.getSpatialStructureInstance();
		playPauseButton.setSelection(running != null && running.isFromOpenGl());
		playPauseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.setFromOpenGl(playPauseButton.getSelection());
				} else {
					playPauseButton.setSelection(false);
				}
			}
		});
		imagePlayPause = Bio7Plugin.getImageDescriptor("/icons/maintoolbar/play_pause.svg").createImage();
		playPauseButton.setImage(imagePlayPause);
		/* Disposed with the button (the button can be moved into another view)! */
		final Image playPauseImage = imagePlayPause;
		playPauseButton.addDisposeListener(_ -> playPauseImage.dispose());

		final Button setupButton = new Button(composite_2, SWT.NONE);
		GridData gd_setupButton = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_setupButton.heightHint = 40;
		setupButton.setLayoutData(gd_setupButton);
		setupButton.setToolTipText("Setup: Invokes the setup method of the compiled java code");
		setupButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.setup = true;
				}

			}
		});
		imageSetup = Bio7Plugin.getImageDescriptor("/icons/maintoolbar/setup.svg").createImage();
		setupButton.setImage(imageSetup);
		final Image setupImage = imageSetup;
		setupButton.addDisposeListener(_ -> setupImage.dispose());
		// setupButton.setText("Setup");
		new Label(composite_2, SWT.NONE);
		new Label(composite_2, SWT.NONE);

		final Label label_4 = new Label(composite_2, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gd_label_4 = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
		gd_label_4.heightHint = 25;
		label_4.setLayoutData(gd_label_4);

		final Label framerateLabel = new Label(composite_2, SWT.NONE);
		framerateLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		framerateLabel.setText("Speed (canStep)");

		/* A logarithmic speed slider like in the control view of Bio7 (steps per second)! */
		final SpeedSlider stepRateSlider = new SpeedSlider(composite_2, SWT.NONE, 0.1, 1000);
		stepRateSlider.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		stepRateSlider.setBackground(composite_2.getBackground());
		stepRateSlider.setValue(1e9 / Math.max(1, SpatialStructure.getPeriod()));
		stepRateSlider.setToolTipText("The speed of the canStep() function in steps (frames) per second (at most the frame rate of the view):\n"
				+ "left slower, right faster. Keys: Left/Right one step (8%), Shift+Left/Right or PageDown/PageUp 10 steps,\nHome/End minimum/maximum.");
		final Label stepRateInfo = new Label(composite_2, SWT.NONE);
		stepRateInfo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		final Runnable updateStepRate = () -> {
			double rate = stepRateSlider.getValue();
			double ms = 1000 / rate;
			/* Like the control view: the fps and the milliseconds per step */
			String millis = ms >= 10 ? String.format(java.util.Locale.ROOT, "%.0f", ms)
					: String.format(java.util.Locale.ROOT, "%.2f", ms).replaceAll("0+$", "").replaceAll("\\.$", "");
			stepRateInfo.setText("Speed: " + SpeedSlider.format(rate) + " fps (" + millis + " ms per step)");
		};
		updateStepRate.run();
		stepRateSlider.addListener(SWT.Selection, _ -> {
			SpatialStructure.setPeriod(Math.round(1e9 / stepRateSlider.getValue()));
			updateStepRate.run();
		});

		final Button haltButton = new Button(composite_2, SWT.CHECK);
		GridData gd_haltButton = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_haltButton.heightHint = 25;
		haltButton.setLayoutData(gd_haltButton);
		haltButton.setToolTipText("Sets the canStep() function to the boolean value 'false'");
		haltButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (haltButton.getSelection()) {
						grid.hold = true;
					} else {
						grid.hold = false;
					}
				}
			}
		});
		haltButton.setText("No step");
		new Label(composite_2, SWT.NONE);

		final Label label_4_1 = new Label(composite_2, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridData gd_label_4_1 = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
		gd_label_4_1.heightHint = 25;
		label_4_1.setLayoutData(gd_label_4_1);
		label_4_1.setText("Label");

		final Button showUfpsButton = new Button(composite_2, SWT.CHECK);
		GridData gd_showUfpsButton = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_showUfpsButton.heightHint = 25;
		showUfpsButton.setLayoutData(gd_showUfpsButton);
		showUfpsButton.setToolTipText("Shows the fps of the canStep() function");
		showUfpsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (showUfpsButton.getSelection()) {
						grid.showUpdateFramerate = true;
					} else {
						grid.showUpdateFramerate = false;
					}
				}
			}
		});
		showUfpsButton.setText("Show step fps");

		fixedFpsButton = new Button(composite_2, SWT.CHECK);
		GridData gd_fixedFpsButton = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_fixedFpsButton.heightHint = 25;
		fixedFpsButton.setLayoutData(gd_fixedFpsButton);
		fixedFpsButton.setSelection(fixed);
		fixedFpsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (fixedFpsButton.getSelection()) {
						grid.getPref3d().setFixedFps(true);
					} else {
						grid.getPref3d().setFixedFps(false);
					}

				}

			}
		});
		fixedFpsButton.setToolTipText(
				"If not selected:\n Sets a default 60 FPS for the OpenGL\ntimer for unnecessary loops.");
		fixedFpsButton.setText("Fixed fps");

		final Button showFramerateButton = new Button(composite_2, SWT.CHECK);
		GridData gd_showFramerateButton = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_showFramerateButton.heightHint = 25;
		showFramerateButton.setLayoutData(gd_showFramerateButton);
		showFramerateButton.setToolTipText("Shows the fps of the timer");
		showFramerateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (showFramerateButton.getSelection()) {
						grid.showFramerate = true;
					} else {
						grid.showFramerate = false;
					}
				}
			}
		});
		showFramerateButton.setText("Show fps");

		setFixedFPS = new Spinner(composite_2, SWT.BORDER);
		GridData gd_setFixedFPS = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_setFixedFPS.heightHint = 25;
		setFixedFPS.setLayoutData(gd_setFixedFPS);
		setFixedFPS.setToolTipText("Sets the Rendering speed to the selected FPS!");
		setFixedFPS.setSelection(60);
		setFixedFPS.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				store.setValue("fixedFps", setFixedFPS.getSelection());
			}
		});
		setFixedFPS.setMaximum(10000);
		setFixedFPS.setMinimum(1);

		final Button newestOpenGlButton = new Button(composite_2, SWT.CHECK);
		GridData gd_newestOpenGlButton = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
		gd_newestOpenGlButton.heightHint = 25;
		newestOpenGlButton.setLayoutData(gd_newestOpenGlButton);
		store.setDefault("openglMaxCompatible", true);
		newestOpenGlButton.setSelection(store.getBoolean("openglMaxCompatible"));
		newestOpenGlButton.setText("Newest OpenGL profile");
		newestOpenGlButton.setToolTipText(
				"Uses the newest OpenGL compatibility profile for the 3D view\n(e.g., OpenGL 4 on Windows and Linux, OpenGL 2.1 on macOS).\nIf not selected: the default profile of the system.");
		newestOpenGlButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				store.setValue("openglMaxCompatible", newestOpenGlButton.getSelection());
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.recreateSceneContext();
				}
			}
		});

		createModelTab(expandBar);

		/* The texture and in addition the height map (surface plot) in one tab! */
		Composite compositeTexture = createTextureTab(expandBar);
		final Group composite_3 = createGroup(compositeTexture, "Height Map (Surface Plot)");
		composite_3.setToolTipText("The heights of the active ImageJ image. Visible if a texture is displayed!");

		final Button enableHeightMapButton = check(composite_3, "Enable Height Map", "Enables the height map (visible if a texture is displayed)");
		enableHeightMapButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.setHeightMap(enableHeightMapButton.getSelection());
				}
			}
		});

		/* A plot view: the grids behind the data and the axes at the box (stored in the preferences)! */
		final Button plotViewButton = check(composite_3, "Plot View",
				"A 3D plot of the height map (or volume, iso surface): the grids at the sides of the box\nbehind the data and the axes (with the numbers, see 'Draw Axis Numbers') at the front edges.\nThe grid planes can be selected in the Space tab. The setting is stored.");
		store.setDefault("plotView", false);
		plotViewButton.setSelection(store.getBoolean("plotView"));
		plotViewButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				store.setValue("plotView", plotViewButton.getSelection());
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.plotView = plotViewButton.getSelection();
					/* A plot view with the axis numbers */
					if (grid.plotView && grid.getAxisLabelsRenderer() != null) {
						grid.getAxisLabelsRenderer().setEnabled(true);
						if (btnDrawnumbers != null && !btnDrawnumbers.isDisposed()) {
							btnDrawnumbers.setSelection(true);
						}
					}
				}
			}
		});

		final Button heightMapTrianglesButton = check(composite_3, "Show Triangles",
				"Draws the triangles of the height map as lines over the surface (see 'Level of Detail')");
		heightMapTrianglesButton.setSelection(HeightMaps.isWireframe());
		heightMapTrianglesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				HeightMaps.setWireframe(heightMapTrianglesButton.getSelection());
			}
		});

		final Button lightenedButton = check(composite_3, "Lighted", "Enables the lighting of the height map");
		lightenedButton.setSelection(true);
		lightenedButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.lightenedHeightMap = lightenedButton.getSelection();
				}
			}
		});

		final Button staticHeightButton = check(composite_3, "Dynamic Heights",
				"If selected the heights are updated directly from the active ImageJ image");
		staticHeightButton.setSelection(true);
		staticHeightButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (staticHeightButton.getSelection()) {
					HeightMaps.setDynamicMap(true);
				} else {
					HeightMaps.setCreateMap(true);
					HeightMaps.setDynamicMap(false);
				}
			}
		});

		final Button geoButton = check(composite_3, "Negative Heights (Geo)",
				"Scales the heights negative for a correct geographic view");
		geoButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				HeightMaps.setInverse(geoButton.getSelection());
			}
		});

		/* The level of detail: the distance of the vertices in pixels (left: more triangles) */
		final Label levelOfDetailLabel_1 = new Label(composite_3, SWT.NONE);
		levelOfDetailLabel_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		final LinearSlider scale_8 = new LinearSlider(composite_3, SWT.NONE);
		scale_8.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		scale_8.setMinimum(1);
		scale_8.setMaximum(100);
		scale_8.setSelection(SpatialStructure.hMap != null ? SpatialStructure.hMap.detail : 16);
		scale_8.setToolTipText("The distance of the vertices of the height map in pixels: left more triangles (finer), right less.\n"
				+ "Keys: Left/Right one pixel, Shift+Left/Right or PageDown/PageUp one major tick, Home/End minimum/maximum.");
		final Runnable updateDetail = () -> levelOfDetailLabel_1
				.setText("Level of Detail: " + scale_8.getSelection() + (scale_8.getSelection() == 1 ? " pixel" : " pixels"));
		updateDetail.run();
		scale_8.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				HeightMaps height = SpatialStructure.getHeightMapInstance();
				if (height != null) {
					height.setDetail(scale_8.getSelection());
				}
				updateDetail.run();
			}
		});

		/* A logarithmic height factor (e.g., low for large values like spectra) */
		final Label factorHeightLabel = new Label(composite_3, SWT.NONE);
		factorHeightLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		final SpeedSlider scaleHeight = new SpeedSlider(composite_3, SWT.NONE, 0.000001, 100);
		scaleHeight.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		scaleHeight.setValue(SpatialStructure.hMap != null ? SpatialStructure.hMap.getScale() : 1);
		scaleHeight.setToolTipText("A scaling factor for the heights (low for large values): left lower, right higher.\n"
				+ "Keys: Left/Right one step (8%), Shift+Left/Right or PageDown/PageUp 10 steps, Home/End minimum/maximum.");
		final Runnable updateFactor = () -> factorHeightLabel.setText("Height Factor: " + SpeedSlider.format(scaleHeight.getValue()));
		updateFactor.run();
		scaleHeight.addListener(SWT.Selection, _ -> {
			SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
			if (grid != null) {
				grid.hMap.setScale((float) scaleHeight.getValue());
			}
			updateFactor.run();
		});
		final Button fitHeightButton = new Button(composite_3, SWT.NONE);
		fitHeightButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		fitHeightButton.setText("Fit Height");
		fitHeightButton.setToolTipText(
				"Sets the height factor so that the range of the heights is half of the longer side of the image");
		fitHeightButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				ImagePlus imp = WindowManager.getCurrentImage();
				if (grid == null || imp == null) {
					return;
				}
				/* The range of the heights without the factor (with the height map enabled) */
				float[] range = grid.hMap.getHeightRange();
				float scale = grid.hMap.getScale();
				if (range == null || scale == 0) {
					factorHeightLabel.setText("Enable the height map to fit the height!");
					return;
				}
				double heights = (range[1] - range[0]) / Math.abs(scale);
				if (heights <= 0) {
					return;
				}
				double factor = 0.5 * Math.max(imp.getWidth(), imp.getHeight()) / heights;
				scaleHeight.setValue(factor);
				grid.hMap.setScale((float) scaleHeight.getValue());
				updateFactor.run();
			}
		});

		final Button gcButton = check(composite_3, "Texture Workaround (Gc)",
				"A special switch for the texture: for some graphics cards it seems to be necessary\nto assign the texture differently!");
		gcButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				HeightMaps.setGraphicsCardSwitch(gcButton.getSelection());
			}
		});

		final Group exportGroup = createGroup(compositeTexture, "Export");

		/* The export of the height map (with the colors of the image) as a mesh, e.g., for Blender! */
		final Button exportTextureButton = new Button(exportGroup, SWT.CHECK);
		GridData gd_exportTextureButton = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_exportTextureButton.heightHint = 25;
		exportTextureButton.setLayoutData(gd_exportTextureButton);
		exportTextureButton.setText("Export the Image as Texture");
		exportTextureButton.setToolTipText(
				"Exports the image (with the LUT) as a texture of the height map:\n*.glb (embedded) or *.obj (a *.png file next to the *.obj file, see the *.mtl file).\nThe other formats contain the colors of the vertices.");
		exportTextureButton.setSelection(true);
		final Button exportHeightMapButton = new Button(exportGroup, SWT.NONE);
		GridData gd_exportHeightMapButton = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_exportHeightMapButton.heightHint = 40;
		exportHeightMapButton.setLayoutData(gd_exportHeightMapButton);
		exportHeightMapButton.setText("Export Height Map...");
		exportHeightMapButton.setToolTipText(
				"Exports the height map of the active ImageJ image as a 3D mesh with the colors of the image (LUT):\nglTF binary (*.glb, with the texture, e.g., for Blender), OBJ (with the texture as *.png),\nPLY (with vertex colors), STL (3D printing) or VTK. z is the height multiplied by the height factor.");
		final Label exportHeightMapLabel = new Label(exportGroup, SWT.WRAP);
		GridData gd_exportHeightMapLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_exportHeightMapLabel.heightHint = 40;
		exportHeightMapLabel.setLayoutData(gd_exportHeightMapLabel);
		exportHeightMapButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				final ImagePlus imp = WindowManager.getCurrentImage();
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (imp == null || grid == null) {
					exportHeightMapLabel.setText("No image opened in ImageJ!");
					return;
				}
				FileDialog dialog = new FileDialog(exportHeightMapButton.getShell(), SWT.SAVE);
				dialog.setFilterExtensions(new String[] { "*.glb", "*.ply", "*.obj", "*.stl", "*.vtk" });
				dialog.setFilterNames(new String[] { "glTF binary (*.glb)", "PLY binary (*.ply)",
						"Wavefront OBJ (*.obj)", "STL binary (*.stl)", "VTK legacy (*.vtk)" });
				dialog.setFileName("heightmap.glb");
				final boolean withTexture = exportTextureButton.getSelection();
				dialog.setOverwrite(true);
				String path = dialog.open();
				if (path == null) {
					return;
				}
				/* Without a known extension the extension of the selected file type! */
				String[] formats = { "glb", "ply", "obj", "stl", "vtk" };
				String lower = path.toLowerCase();
				boolean known = false;
				for (String format : formats) {
					known |= lower.endsWith("." + format);
				}
				if (!known) {
					path = path + "." + formats[Math.max(0, dialog.getFilterIndex())];
				}
				final String file = path;
				final HeightMaps heightMaps = grid.hMap;
				exportHeightMapLabel.setText("Exporting...");
				/* Large images: export in the background! */
				Thread export = new Thread(() -> {
					String message;
					try {
						message = "Exported: " + heightMaps.export(imp, new java.io.File(file), 1, withTexture);
					} catch (java.io.IOException | RuntimeException ex) {
						message = "Export failed: " + ex.getMessage();
					}
					final String text = message;
					Display.getDefault().asyncExec(() -> {
						if (!exportHeightMapLabel.isDisposed()) {
							exportHeightMapLabel.setText(text);
						}
					});
				}, "Bio7 Height Map Export");
				export.setDaemon(true);
				export.start();
			}
		});

		createVolumeTab(expandBar);
		createIsoSurfaceTab(expandBar);
		createClippingTab(expandBar);
		createMeasureTab(expandBar);

		final CTabItem newItemExpandItem_5 = new CTabItem(expandBar, SWT.NONE);
		// newItemExpandItem_5.setHeight(500);
		newItemExpandItem_5.setText("Camera");
		ScrolledComposite scroll_4 = new ScrolledComposite(expandBar, SWT.V_SCROLL);
		final Composite composite_4 = new Composite(scroll_4, SWT.NONE);
		newItemExpandItem_5.setControl(scroll_4);
		composite_4.setLayout(new GridLayout(2, true));
		scroll_4.setMinSize(200, 560);
		scroll_4.setContent(composite_4);
		scroll_4.setExpandVertical(true);
		scroll_4.setExpandHorizontal(true);
		scroll_4.setAlwaysShowScrollBars(true);

		/* The walkthrough (arrow keys) and the flythrough with the speeds */
		final Group walkGroup = createGroup(composite_4, "Walkthrough");
		final Button enableWalkthroughButton = check(walkGroup, "Enable Walkthrough",
				"Moves the camera with the arrow keys (Ctrl: sideways)");
		enableWalkthroughButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure.setWalkthrough(enableWalkthroughButton.getSelection());
			}
		});
		final Button enableFlythroughButton = check(walkGroup, "Enable Flythrough",
				"The walkthrough also moves the camera up and down in the view direction");
		enableFlythroughButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.flythrough = enableFlythroughButton.getSelection();
				}
			}
		});
		speedRow(walkGroup, "Horizontal Speed", "The horizontal speed of the walkthrough", 2, v -> {
			SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
			if (grid != null) {
				grid.SPEED = v;
			}
		});
		speedRow(walkGroup, "Vertical Speed", "The vertical speed of the flythrough", 0.1, v -> {
			SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
			if (grid != null) {
				grid.heightSpeed = v;
			}
		});

		/* The split view: a second camera (position and look at) in a part of the view */
		final Group splitGroup = createGroup(composite_4, "Split View", 4, true);
		final Button splitViewButton = check(splitGroup, "Split View", "Shows a second camera in the bottom left corner of the view");
		((GridData) splitViewButton.getLayoutData()).horizontalSpan = 4;
		splitViewButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.setSplitView(splitViewButton.getSelection());
				}
			}
		});
		new Label(splitGroup, SWT.NONE);
		for (String axis : new String[] { "x", "y", "z" }) {
			Label header = new Label(splitGroup, SWT.CENTER);
			header.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			header.setText(axis);
		}
		/* The stored camera of the split view */
		Point3d splitLocation = Bio7PrefConverterSpatial.getPoint(store, "splitCameraLoc");
		Point3d splitLookAt = Bio7PrefConverterSpatial.getPoint(store, "splitCameraLookAt");
		Label positionLabel = new Label(splitGroup, SWT.NONE);
		positionLabel.setText("Position");
		splitCamX = coordinateText(splitGroup, splitLocation.x, v -> SpatialStructure.getSpatialStructureInstance().xCamSplit = v);
		splitCamY = coordinateText(splitGroup, splitLocation.y, v -> SpatialStructure.getSpatialStructureInstance().yCamSplit = v);
		splitCamZ = coordinateText(splitGroup, splitLocation.z, v -> SpatialStructure.getSpatialStructureInstance().zCamSplit = v);
		Label lookAtLabel = new Label(splitGroup, SWT.NONE);
		lookAtLabel.setText("Look At");
		lookAtX = coordinateText(splitGroup, splitLookAt.x, v -> SpatialStructure.getSpatialStructureInstance().xSplitLookAt = v);
		lookAtY = coordinateText(splitGroup, splitLookAt.y, v -> SpatialStructure.getSpatialStructureInstance().ySplitLookAt = v);
		lookAtZ = coordinateText(splitGroup, splitLookAt.z, v -> SpatialStructure.getSpatialStructureInstance().zSplitLookAt = v);
		errorLabelSplit = new Label(splitGroup, SWT.NONE);
		errorLabelSplit.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		errorLabelSplit.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		final Button storeButton = new Button(splitGroup, SWT.NONE);
		storeButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		storeButton.setText("Store");
		storeButton.setToolTipText("Stores the position and the look at of the split view camera");
		storeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					try {
						grid.getPref3d().storeSplitViewCameraLocation(Double.parseDouble(splitCamX.getText()),
								Double.parseDouble(splitCamY.getText()), Double.parseDouble(splitCamZ.getText()));
						grid.getPref3d().storeSplitViewCameraLookAt(Double.parseDouble(lookAtX.getText()),
								Double.parseDouble(lookAtY.getText()), Double.parseDouble(lookAtZ.getText()));
					} catch (NumberFormatException ex) {
						errorLabelSplit.setText("Number input incorrect!");
					}
				}
			}
		});

		/* The camera modes */
		final Group modesGroup = createGroup(composite_4, "Camera Modes");
		final Button customCameraButton = check(modesGroup, "Custom Camera",
				"Uses the camera coordinates of the custom camera (e.g., set by a script)");
		customCameraButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.customCamera = customCameraButton.getSelection();
				}
			}
		});
		final Button stereoButton = check(modesGroup, "Stereo", "An anaglyph stereo view (red/cyan glasses)");
		stereoButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.stereo = stereoButton.getSelection();
				}
			}
		});

		final CTabItem newItemExpandItem_6 = new CTabItem(expandBar, SWT.NONE);
		// newItemExpandItem_6.setHeight(350);
		newItemExpandItem_6.setText("Light");
		ScrolledComposite scroll_6 = new ScrolledComposite(expandBar, SWT.V_SCROLL);
		final Composite composite_6 = new Composite(scroll_6, SWT.NONE);
		newItemExpandItem_6.setControl(scroll_6);
		composite_6.setLayout(new GridLayout(4, true));

		scroll_6.setMinSize(200, 350);
		scroll_6.setContent(composite_6);
		scroll_6.setExpandVertical(true);
		scroll_6.setExpandHorizontal(true);
		scroll_6.setAlwaysShowScrollBars(true);

		/* Switch for the first light! */

		final Button localLightButton = new Button(composite_6, SWT.CHECK);
		GridData gd_localLightButton = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		gd_localLightButton.heightHint = 25;
		localLightButton.setLayoutData(gd_localLightButton);
		localLightButton.setSelection(true);
		localLightButton.setToolTipText(
				"If enabled the lights will rotate with the arcball interface (like a fixed lamp on a plane). ");
		localLightButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (localLightButton.getSelection()) {
						grid.localLight = true;

					} else {
						grid.localLight = false;
					}
				}
			}
		});
		localLightButton.setText("Local Light");
		new Label(composite_6, SWT.NONE);

		final Label posXLabel = new Label(composite_6, SWT.NONE);
		GridData gd_posXLabel = new GridData(SWT.CENTER, SWT.FILL, true, false, 1, 1);
		gd_posXLabel.heightHint = 25;
		posXLabel.setLayoutData(gd_posXLabel);
		posXLabel.setText("x");

		final Label yLabel_1 = new Label(composite_6, SWT.NONE);
		GridData gd_yLabel_1 = new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1);
		gd_yLabel_1.heightHint = 25;
		yLabel_1.setLayoutData(gd_yLabel_1);
		yLabel_1.setText("y");

		final Label zLabel_1 = new Label(composite_6, SWT.NONE);
		GridData gd_zLabel_1 = new GridData(SWT.CENTER, SWT.FILL, true, false, 1, 1);
		gd_zLabel_1.heightHint = 25;
		zLabel_1.setLayoutData(gd_zLabel_1);
		zLabel_1.setText("z");
		final Button enabledButton = new Button(composite_6, SWT.CHECK);
		GridData gd_enabledButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_enabledButton.heightHint = 25;
		enabledButton.setLayoutData(gd_enabledButton);
		enabledButton.setSelection(true);
		enabledButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (enabledButton.getSelection()) {
						grid.light1 = true;
					} else {
						grid.light1 = false;
					}
					// grid.getRenderer().switchLight();
				}
			}
		});
		enabledButton.setText("Enabled");
		/* Switch for the second light! */

		final Spinner spinner = new Spinner(composite_6, SWT.BORDER);
		GridData gd_spinner = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_spinner.heightHint = 25;
		spinner.setLayoutData(gd_spinner);
		spinner.setMinimum(-1000000);
		spinner.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {

					grid.setLightPos1(spinner.getSelection(), grid.getLightPos1()[1], grid.getLightPos1()[2]);

				}
			}
		});
		spinner.setMaximum(1000000);

		final Spinner spinner_1 = new Spinner(composite_6, SWT.BORDER);
		GridData gd_spinner_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_spinner_1.heightHint = 25;
		spinner_1.setLayoutData(gd_spinner_1);
		spinner_1.setMinimum(-1000000);

		spinner_1.setMaximum(1000000);
		spinner_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {

					grid.setLightPos1(grid.getLightPos1()[0], spinner_1.getSelection(), grid.getLightPos1()[2]);

				}
			}
		});
		spinner_1.setSelection(1000);

		final Spinner spinner_2 = new Spinner(composite_6, SWT.BORDER);
		GridData gd_spinner_2 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_spinner_2.heightHint = 25;
		spinner_2.setLayoutData(gd_spinner_2);
		spinner_2.setMinimum(-1000000);

		spinner_2.setMaximum(1000000);
		spinner_2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {

					grid.setLightPos1(grid.getLightPos1()[0], grid.getLightPos1()[1], spinner_2.getSelection());

				}
			}
		});
		spinner_2.setSelection(1000);

		final Label light1Label = new Label(composite_6, SWT.NONE);
		GridData gd_light1Label = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_light1Label.heightHint = 25;
		light1Label.setLayoutData(gd_light1Label);
		light1Label.setText("Light 1");
		new Label(composite_6, SWT.NONE);
		new Label(composite_6, SWT.NONE);
		new Label(composite_6, SWT.NONE);
		final Button enabledButton_1 = new Button(composite_6, SWT.CHECK);
		GridData gd_enabledButton_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_enabledButton_1.heightHint = 25;
		enabledButton_1.setLayoutData(gd_enabledButton_1);
		enabledButton_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (enabledButton_1.getSelection()) {
						grid.light2 = true;
					} else {
						grid.light2 = false;
					}
					// grid.getRenderer().switchLight();
				}
			}
		});
		enabledButton_1.setText("Enabled");

		final Spinner spinner_3 = new Spinner(composite_6, SWT.BORDER);
		GridData gd_spinner_3 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_spinner_3.heightHint = 25;
		spinner_3.setLayoutData(gd_spinner_3);
		spinner_3.setMinimum(-1000000);
		spinner_3.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {

					grid.setLightPos2(spinner_3.getSelection(), grid.getLightPos2()[1], grid.getLightPos2()[2]);

				}
			}
		});
		spinner_3.setMaximum(1000000);

		final Spinner spinner_4 = new Spinner(composite_6, SWT.BORDER);
		GridData gd_spinner_4 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_spinner_4.heightHint = 25;
		spinner_4.setLayoutData(gd_spinner_4);
		spinner_4.setMinimum(-1000000);
		spinner_4.setMaximum(1000000);
		spinner_4.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {

					grid.setLightPos2(grid.getLightPos2()[0], spinner_4.getSelection(), grid.getLightPos2()[2]);

				}
			}
		});

		final Spinner spinner_5 = new Spinner(composite_6, SWT.BORDER);
		GridData gd_spinner_5 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_spinner_5.heightHint = 25;
		spinner_5.setLayoutData(gd_spinner_5);
		spinner_5.setMinimum(-1000000);
		spinner_5.setMaximum(1000000);
		spinner_5.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {

					grid.setLightPos2(grid.getLightPos2()[0], grid.getLightPos2()[1], spinner_5.getSelection());

				}
			}
		});

		final Label light2Label = new Label(composite_6, SWT.NONE);
		GridData gd_light2Label = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_light2Label.heightHint = 25;
		light2Label.setLayoutData(gd_light2Label);
		light2Label.setText("Light 2");
		new Label(composite_6, SWT.NONE);
		new Label(composite_6, SWT.NONE);
		new Label(composite_6, SWT.NONE);

		final Button enabledButton_2 = new Button(composite_6, SWT.CHECK);
		GridData gd_enabledButton_2 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_enabledButton_2.heightHint = 25;
		enabledButton_2.setLayoutData(gd_enabledButton_2);
		enabledButton_2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (enabledButton_2.getSelection()) {
						grid.light3 = true;
					} else {
						grid.light3 = false;
					}
					// grid.getRenderer().switchLight();
				}
			}
		});
		enabledButton_2.setText("Enabled");

		final Spinner spinner_6 = new Spinner(composite_6, SWT.BORDER);
		GridData gd_spinner_6 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_spinner_6.heightHint = 25;
		spinner_6.setLayoutData(gd_spinner_6);
		spinner_6.setMinimum(-1000000);
		spinner_6.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {

					grid.setLightPos3(spinner_6.getSelection(), grid.getLightPos3()[1], grid.getLightPos3()[2]);

				}
			}
		});
		spinner_6.setMaximum(1000000);

		final Spinner spinner_7 = new Spinner(composite_6, SWT.BORDER);
		GridData gd_spinner_7 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_spinner_7.heightHint = 25;
		spinner_7.setLayoutData(gd_spinner_7);
		spinner_7.setMinimum(-1000000);
		spinner_7.setMaximum(1000000);
		spinner_7.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {

					grid.setLightPos3(grid.getLightPos3()[0], spinner_7.getSelection(), grid.getLightPos3()[2]);

				}
			}
		});

		final Spinner spinner_8 = new Spinner(composite_6, SWT.BORDER);
		GridData gd_spinner_8 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_spinner_8.heightHint = 25;
		spinner_8.setLayoutData(gd_spinner_8);
		spinner_8.setMinimum(-1000000);
		spinner_8.setMaximum(1000000);
		spinner_8.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {

					grid.setLightPos3(grid.getLightPos3()[0], grid.getLightPos3()[1], spinner_8.getSelection());

				}
			}
		});

		final Label light3Label = new Label(composite_6, SWT.NONE);
		GridData gd_light3Label = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_light3Label.heightHint = 25;
		light3Label.setLayoutData(gd_light3Label);
		light3Label.setText("Light 3");
		new Label(composite_6, SWT.NONE);
		new Label(composite_6, SWT.NONE);
		new Label(composite_6, SWT.NONE);

		final Button enabledButton_3 = new Button(composite_6, SWT.CHECK);
		GridData gd_enabledButton_3 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_enabledButton_3.heightHint = 25;
		enabledButton_3.setLayoutData(gd_enabledButton_3);
		enabledButton_3.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (enabledButton_3.getSelection()) {
						grid.light4 = true;
					} else {
						grid.light4 = false;
					}
					// grid.getRenderer().switchLight();
				}
			}
		});
		enabledButton_3.setText("Enabled");

		final Spinner spinner_9 = new Spinner(composite_6, SWT.BORDER);
		GridData gd_spinner_9 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_spinner_9.heightHint = 25;
		spinner_9.setLayoutData(gd_spinner_9);
		spinner_9.setMinimum(-1000000);
		spinner_9.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {

					grid.setLightPos4(spinner_9.getSelection(), grid.getLightPos4()[1], grid.getLightPos4()[2]);

				}
			}
		});
		spinner_9.setMaximum(1000000);

		final Spinner spinner_10 = new Spinner(composite_6, SWT.BORDER);
		GridData gd_spinner_10 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_spinner_10.heightHint = 25;
		spinner_10.setLayoutData(gd_spinner_10);
		spinner_10.setMinimum(-1000000);
		spinner_10.setMaximum(1000000);
		spinner_10.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {

					grid.setLightPos4(grid.getLightPos4()[0], spinner_10.getSelection(), grid.getLightPos4()[2]);

				}
			}
		});

		final Spinner spinner_11 = new Spinner(composite_6, SWT.BORDER);
		GridData gd_spinner_11 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_spinner_11.heightHint = 25;
		spinner_11.setLayoutData(gd_spinner_11);
		spinner_11.setMinimum(-1000000);
		spinner_11.setMaximum(1000000);
		spinner_11.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {

					grid.setLightPos4(grid.getLightPos4()[0], grid.getLightPos4()[1], spinner_11.getSelection());

				}
			}
		});

		final Label light4Label = new Label(composite_6, SWT.NONE);
		GridData gd_light4Label = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_light4Label.heightHint = 25;
		light4Label.setLayoutData(gd_light4Label);
		light4Label.setText("Light 4");
		new Label(composite_6, SWT.NONE);
		new Label(composite_6, SWT.NONE);
		new Label(composite_6, SWT.NONE);

		final CTabItem newItemExpandItem_8 = new CTabItem(expandBar, SWT.NONE);
		// newItemExpandItem_8.setHeight(130);
		newItemExpandItem_8.setText("Render");
		ScrolledComposite scroll_8 = new ScrolledComposite(expandBar, SWT.V_SCROLL);
		final Composite composite_8 = new Composite(scroll_8, SWT.NONE);
		newItemExpandItem_8.setControl(scroll_8);
		composite_8.setLayout(new GridLayout(1, true));

		scroll_8.setMinSize(200, 130);
		scroll_8.setContent(composite_8);
		scroll_8.setExpandVertical(true);
		scroll_8.setExpandHorizontal(true);
		scroll_8.setAlwaysShowScrollBars(true);

		/*
		 * final Button getImageButton = new Button(composite_8, SWT.NONE); GridData
		 * gd_getImageButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		 * gd_getImageButton.heightHint = 40;
		 * getImageButton.setLayoutData(gd_getImageButton);
		 * getImageButton.setToolTipText("Creates a screenshot image in ImageJ");
		 * getImageButton.addSelectionListener(new SelectionAdapter() { public void
		 * widgetSelected(final SelectionEvent e) { SpatialStructure grid =
		 * SpatialStructure.getSpatialStructureInstance(); if (grid != null) {
		 * grid.takeScreenshot = true; }
		 * 
		 * } }); getImageButton.setText("Get Image");
		 */

		final Label numberLabel = new Label(composite_8, SWT.NONE);
		GridData gd_numberLabel = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_numberLabel.heightHint = 30;
		numberLabel.setLayoutData(gd_numberLabel);
		numberLabel.setForeground(new Color(Display.getCurrent(), 255, 0, 0));

		final Button renderFramesButton = new Button(composite_8, SWT.NONE);
		GridData gd_renderFramesButton = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_renderFramesButton.heightHint = 40;
		renderFramesButton.setLayoutData(gd_renderFramesButton);
		renderFramesButton.setToolTipText(
				"Starts and stops rendering - \r\ncreates a stack in Imagej and renders the current\r\nframe of the spatial view to the stack until \r\nrendering is stopped or the max. numbers of frames\r\n(specified in the textfield) has been reached.");
		renderFramesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					ScreenRecording screen = new ScreenRecording(renderFramesButton);
					if (screen.doCapture() == false) {
						renderFramesButton.setText("Rec.");
						screen.setCount(stackSizeCount);
						screen.setFps(fpsScreenCapture);
						// screen.setupCaptureImages();
						screen.setCapture(true);
						screen.docaptureAnimationJob();
					}

					else {
						screen.setCapture(false);
						renderFramesButton.setText("Capture");
					}

				}
			}
		});
		renderFramesButton.setText("Render Frames");
		text_11 = new Text(composite_8, SWT.BORDER);
		text_11.setToolTipText("Number of frames to render!");
		GridData gd_text_11 = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_text_11.heightHint = 30;
		text_11.setLayoutData(gd_text_11);
		text_11.setText(Integer.toString(stackSizeCount));
		text_11.addListener(SWT.Modify, new Listener() {

			SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();

			public void handleEvent(Event event) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					try {
						stackSizeCount = Integer.parseInt(text_11.getText());
						text_11.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
						grid.setRenderImageTo(stackSizeCount);
						numberLabel.setText("");
					} catch (Exception e) {
						text_11.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
						numberLabel.setText("Number input incorrect!");
					}
				}
			}
		});

	}

	@Override
	public void dispose() {
		/* The images are disposed with their buttons (which can be moved into another view)! */
		IWorkbenchPage page = getSite().getPage();
		if (page != null) {
			page.removePartListener(partListener);
		}
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
	/*
	 * private void initializeToolBar() { IToolBarManager toolbarManager =
	 * getViewSite().getActionBars().getToolBarManager(); }
	 */

	/**
	 * Initialize the menu
	 */
	private void initializeMenu() {
		IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
	}

	@Override
	public void setFocus() {
		ensureContents();

	}

	public static Text getText() {
		return text;
	}

	public static void setText(Text text) {
		Options3d.text = text;
	}

	public static String getObjectPath() {
		return objectPath;
	}

	public static String getObjectName() {
		return objectName;
	}

	public RGB getColour() {
		Shell shell = new Shell();
		ColorDialog dialog = new ColorDialog(shell, SWT.APPLICATION_MODAL);

		RGB color = dialog.open();
		return color;
	}

	public static Integer getStackSizeCount() {
		return stackSizeCount;
	}

	public static void setStackSizeCount(Integer stackSizeCount) {
		Options3d.stackSizeCount = stackSizeCount;
	}

	public static boolean isRenderImageJFrames() {
		return renderImageJFrames;
	}

	public static void setRenderImageJFrames(boolean renderImageJFrames) {
		Options3d.renderImageJFrames = renderImageJFrames;
	}

	public Button getShowGridButton() {
		return showGridButton;
	}

	public Button getShowAxesButton() {
		return showAxesButton;
	}

	public Button getCartesianButton() {
		return octantButton;
	}

	public Button getShowQuadButton() {
		return showQuadButton;
	}

	public static Button getPlayPauseButton() {
		return playPauseButton;
	}

	/**
	 * Selects the play/pause button if the run method of the compiled model is
	 * invoked (see SpatialStructure.setFromOpenGl). Can be called from any thread.
	 */
	public static void updatePlayPause() {
		Display display = Display.getDefault();
		if (display.isDisposed()) {
			return;
		}
		display.asyncExec(() -> {
			SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
			Button button = playPauseButton;
			if (button != null && !button.isDisposed()) {
				button.setSelection(grid != null && grid.isFromOpenGl());
			}
		});
	}

	public static int getSetFixedFPS() {
		return setFixedFPS.getSelection();
	}

	/*
	 * The tab for an *.obj model (loaded from a file, centered and scaled to a
	 * size of 1000 units)!
	 */
	private void createModelTab(CTabFolder tabFolder) {
		final CTabItem modelItem = new CTabItem(tabFolder, SWT.NONE);
		modelItem.setText("Model");
		ScrolledComposite scrollModel = new ScrolledComposite(tabFolder, SWT.V_SCROLL);
		final Composite compositeModel = new Composite(scrollModel, SWT.NONE);
		modelItem.setControl(scrollModel);
		compositeModel.setLayout(new GridLayout(4, true));
		scrollModel.setMinSize(200, 400);
		scrollModel.setContent(compositeModel);
		scrollModel.setExpandVertical(true);
		scrollModel.setExpandHorizontal(true);
		scrollModel.setAlwaysShowScrollBars(true);

		final SpatialStructure current = SpatialStructure.getSpatialStructureInstance();
		final Label infoLabel = new Label(compositeModel, SWT.WRAP);
		GridData gd_infoLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		gd_infoLabel.heightHint = 40;
		infoLabel.setLayoutData(gd_infoLabel);
		/* Shows the size of a loaded model! */
		final Runnable updateInfo = () -> Display.getDefault().asyncExec(() -> {
			SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
			if (!infoLabel.isDisposed() && grid != null) {
				infoLabel.setText(grid.getModelInfo());
			}
		});
		infoLabel.setText(current != null ? current.getModelInfo() : "No model loaded");
		if (current != null) {
			current.setModelListener(updateInfo);
		}

		text = new Text(compositeModel, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		text.setText(store.getString("objectFile"));
		text.setToolTipText("The mesh file of the model (*.obj with materials, *.stl, *.ply, *.off)");
		final Text pathText = text;
		pathText.addModifyListener(e -> store.setValue("objectFile", pathText.getText()));
		final Button browseButton = new Button(compositeModel, SWT.NONE);
		browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		browseButton.setText("Browse...");
		browseButton.setToolTipText("Selects a mesh file (*.obj, *.stl, *.ply, *.off)");
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				FileDialog fd = new FileDialog(browseButton.getShell(), SWT.OPEN);
				fd.setText("Load");
				fd.setFilterExtensions(new String[] { "*.obj;*.stl;*.ply;*.off", "*.obj", "*.stl", "*.ply", "*.off", "*.*" });
				fd.setFilterNames(new String[] { "Mesh Files (*.obj, *.stl, *.ply, *.off)", "OBJ (*.obj)", "STL (*.stl)",
						"PLY (*.ply)", "OFF (*.off)", "All Files" });
				String path = fd.open();
				if (path != null) {
					objectPath = fd.getFilterPath() + java.io.File.separator;
					objectName = fd.getFileName();
					pathText.setText(path);
				}
			}
		});

		final Button showButton = new Button(compositeModel, SWT.CHECK);

		final Button loadButton = new Button(compositeModel, SWT.NONE);
		GridData gd_loadButton = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_loadButton.heightHint = 40;
		loadButton.setLayoutData(gd_loadButton);
		loadButton.setText("Load Model");
		loadButton.setToolTipText(
				"Loads (or reloads) the model: *.obj (with materials and textures), *.stl, *.ply (with colors), *.off");
		loadButton.moveAbove(showButton);
		loadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid == null) {
					return;
				}
				java.io.File file = new java.io.File(pathText.getText());
				if (!file.isFile()) {
					infoLabel.setText("The file doesn't exist!");
					return;
				}
				if (!com.eco.bio7.loader3d.MeshReader.isSupported(file)) {
					infoLabel.setText("Unknown format (supported: *.obj, *.stl, *.ply, *.off)!");
					return;
				}
				grid.setModelListener(updateInfo);
				infoLabel.setText("Loading...");
				/* Loaded in the OpenGL thread! */
				grid.setLoadModel(true);
				SpatialStructure.setReloadModel(true);
				showButton.setSelection(true);
			}
		});

		final Button removeButton = new Button(compositeModel, SWT.NONE);
		GridData gd_removeButton = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_removeButton.heightHint = 40;
		removeButton.setLayoutData(gd_removeButton);
		removeButton.setText("Remove");
		removeButton.setToolTipText("Removes the model");
		removeButton.moveAbove(showButton);
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.setLoadModel(false);
					grid.model = null;
					showButton.setSelection(false);
					infoLabel.setText(grid.getModelInfo());
				}
			}
		});

		showButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		showButton.setText("Show Model");
		showButton.setToolTipText("Displays the loaded model");
		showButton.setSelection(current != null && current.isLoadModel());
		showButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.setLoadModel(showButton.getSelection());
					/* Not loaded yet! */
					if (showButton.getSelection() && grid.model == null) {
						grid.setModelListener(updateInfo);
						SpatialStructure.setReloadModel(true);
					}
					infoLabel.setText(grid.getModelInfo());
				}
			}
		});

		final Button lightedButton = new Button(compositeModel, SWT.CHECK);
		lightedButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		lightedButton.setText("Lighted");
		lightedButton.setToolTipText("Enables the lighting of the model with the activated lamps");
		lightedButton.setSelection(current == null || current.lightenedObjModel);
		lightedButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.lightenedObjModel = lightedButton.getSelection();
				}
			}
		});

		/* The values of the model or the stored values! */
		double[] position = current != null ? current.objModel1Position.clone() : new double[] { p.x, p.y, p.z };
		double[] rotation = current != null ? current.objModel1Rotation.clone() : new double[] { p2.x, p2.y, p2.z };
		double scale = current != null ? current.objModel1Scale : Math.max(1, p3.x);

		createModelRowLabel(compositeModel, "Position:");
		final Spinner[] positionSpinners = new Spinner[3];
		for (int i = 0; i < 3; i++) {
			final int axis = i;
			positionSpinners[i] = createModelSpinner(compositeModel, 1, -10000000, 10000000, 100, position[i],
					"The position (" + "xyz".charAt(i) + ") of the center of the model");
			positionSpinners[i].addModifyListener(e -> {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.objModel1Position[axis] = spinnerValue(positionSpinners[axis]);
				}
			});
		}
		createModelRowLabel(compositeModel, "Rotation:");
		final Spinner[] rotationSpinners = new Spinner[3];
		for (int i = 0; i < 3; i++) {
			final int axis = i;
			rotationSpinners[i] = createModelSpinner(compositeModel, 0, -360, 360, 5, rotation[i],
					"The rotation (degrees) around the " + "xyz".charAt(i) + " axis");
			rotationSpinners[i].addModifyListener(e -> {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.objModel1Rotation[axis] = spinnerValue(rotationSpinners[axis]);
				}
			});
		}
		createModelRowLabel(compositeModel, "Scale:");
		final Spinner scaleSpinner = createModelSpinner(compositeModel, 2, 1, 100000, 10, scale,
				"The scale of the model (1 = the longest side of the model is 1000 units)");
		scaleSpinner.addModifyListener(e -> {
			SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
			if (grid != null) {
				grid.objModel1Scale = spinnerValue(scaleSpinner);
			}
		});
		new Label(compositeModel, SWT.NONE);
		new Label(compositeModel, SWT.NONE);

		/* Sets the spinners (and the model) to the given values! */
		final java.util.function.BiConsumer<double[], double[]> setValues = (pos, rot) -> {
			for (int i = 0; i < 3; i++) {
				setSpinnerValue(positionSpinners[i], pos[i]);
				setSpinnerValue(rotationSpinners[i], rot[i]);
			}
		};

		final Button fitButton = new Button(compositeModel, SWT.NONE);
		GridData gd_fitButton = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_fitButton.heightHint = 40;
		fitButton.setLayoutData(gd_fitButton);
		fitButton.setText("Fit to Scene");
		fitButton.setToolTipText(
				"Centers and scales the model to the grid (or to a displayed texture, volume or iso surface)");
		fitButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid == null) {
					return;
				}
				float ax = grid.getAxisSizeX();
				float ay = grid.getAxisSizeY();
				float az = grid.getAxisSizeZ();
				double[] center;
				double size;
				if (HeightMaps.isCartesian()) {
					/* The axes are from -size to +size! */
					center = new double[] { 0, 0, 0 };
					size = 2 * Math.max(ax, ay);
				} else {
					center = new double[] { ax / 2.0, ay / 2.0, az / 2.0 };
					size = Math.max(ax, ay);
				}
				setValues.accept(center, grid.objModel1Rotation.clone());
				/* The longest side of the model is 1000 units (scale 1)! */
				setSpinnerValue(scaleSpinner, size / 1000.0);
			}
		});

		final Button resetButton = new Button(compositeModel, SWT.NONE);
		GridData gd_resetButton = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_resetButton.heightHint = 40;
		resetButton.setLayoutData(gd_resetButton);
		resetButton.setText("Reset");
		resetButton.setToolTipText("Resets the position, rotation and scale");
		resetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				setValues.accept(new double[3], new double[3]);
				setSpinnerValue(scaleSpinner, 1);
			}
		});

		final Button storeButton = new Button(compositeModel, SWT.NONE);
		GridData gd_storeButton = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		gd_storeButton.heightHint = 40;
		storeButton.setLayoutData(gd_storeButton);
		storeButton.setText("Store as Default");
		storeButton.setToolTipText("Stores the position, rotation and scale for the next start");
		storeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.getPref3d().storeModelTransform(grid.objModel1Position, grid.objModel1Rotation,
							grid.objModel1Scale);
				}
			}
		});

		/* The model as an iso surface (measurements, clipping, colors and export) */
		final Label measureLabel = new Label(compositeModel, SWT.WRAP);
		GridData gd_measureLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		gd_measureLabel.verticalIndent = 10;
		measureLabel.setLayoutData(gd_measureLabel);
		measureLabel.setText("Measure the Model (as Iso Surface):");
		createModelRowLabel(compositeModel, "Unit:");
		final Text unitText = new Text(compositeModel, SWT.BORDER);
		unitText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		unitText.setText("unit");
		unitText.setToolTipText("The unit of the measurements (e.g., mm or micron)");
		Label factorLabel = new Label(compositeModel, SWT.NONE);
		factorLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		factorLabel.setText("Scale:");
		final Text factorText = new Text(compositeModel, SWT.BORDER);
		factorText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		factorText.setText("1");
		factorText.setToolTipText(
				"The size of one unit of the file in the unit (e.g., 1000 for a file in mm and the unit micron)");
		final Button measureButton = new Button(compositeModel, SWT.NONE);
		GridData gd_measureButton = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		gd_measureButton.heightHint = 40;
		measureButton.setLayoutData(gd_measureButton);
		measureButton.setText("Use as Iso Surface (Measure)");
		measureButton.setToolTipText(
				"Displays the model (duplicate vertices merged) as iso surface for the measurements (Measure tab), the clipping, the colors and the export");
		measureButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				IsoSurface iso = isoSurface();
				if (grid == null || iso == null || grid.model == null) {
					infoLabel.setText("Load a model first!");
					return;
				}
				double factor;
				try {
					factor = Double.parseDouble(factorText.getText().trim().replace(',', '.'));
				} catch (NumberFormatException ex) {
					infoLabel.setText("The scale is not a number!");
					return;
				}
				if (!(factor > 0)) {
					infoLabel.setText("The scale must be larger than 0!");
					return;
				}
				com.eco.bio7.loader3d.MeshModel model = grid.model;
				iso.setMesh(model.getWeldedMesh(), factor, unitText.getText().trim());
				iso.setShow(true);
				/* The model is displayed as iso surface (not twice) */
				grid.setLoadModel(false);
				showButton.setSelection(false);
				com.eco.bio7.loader3d.MeshData.Report report = model.getReport();
				infoLabel.setText(model.getName() + " is displayed as iso surface (Measure tab)"
						+ (report.isWatertight() ? "." : ". Not watertight: the volumes are not reliable!"));
			}
		});
		scrollModel.setMinSize(200, 560);
	}

	private static void createModelRowLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		label.setText(text);
	}

	private static Spinner createModelSpinner(Composite parent, int digits, int min, int max, int increment,
			double value, String toolTip) {
		Spinner spinner = new Spinner(parent, SWT.BORDER);
		spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		spinner.setDigits(digits);
		spinner.setMinimum(min);
		spinner.setMaximum(max);
		spinner.setIncrement(increment);
		spinner.setPageIncrement(increment * 10);
		setSpinnerValue(spinner, value);
		spinner.setToolTipText(toolTip);
		return spinner;
	}

	private static double spinnerValue(Spinner spinner) {
		return spinner.getSelection() / Math.pow(10, spinner.getDigits());
	}

	private static void setSpinnerValue(Spinner spinner, double value) {
		spinner.setSelection((int) Math.round(value * Math.pow(10, spinner.getDigits())));
	}

	/*
	 * The tab for the texture (a static texture from an ImageJ image or a file or
	 * a dynamic texture from the active ImageJ image)!
	 */
	private Composite createTextureTab(CTabFolder tabFolder) {
		final CTabItem textureItem = new CTabItem(tabFolder, SWT.NONE);
		textureItem.setText("Heightmap");
		ScrolledComposite scrollTexture = new ScrolledComposite(tabFolder, SWT.V_SCROLL);
		final Composite compositeTexture = new Composite(scrollTexture, SWT.NONE);
		textureItem.setControl(scrollTexture);
		compositeTexture.setLayout(new GridLayout(2, true));
		scrollTexture.setMinSize(200, 760);
		scrollTexture.setContent(compositeTexture);
		scrollTexture.setExpandVertical(true);
		scrollTexture.setExpandHorizontal(true);
		scrollTexture.setAlwaysShowScrollBars(true);

		/* The texture, the height map and the export in separate groups! */
		final Group textureGroup = createGroup(compositeTexture, "Texture");

		final Label infoLabel = new Label(textureGroup, SWT.WRAP);
		GridData gd_infoLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_infoLabel.heightHint = 34;
		infoLabel.setLayoutData(gd_infoLabel);
		/* Shows the source and the size of a created texture! */
		final Runnable updateInfo = () -> Display.getDefault().asyncExec(() -> {
			Textures textures = textures();
			if (!infoLabel.isDisposed() && textures != null) {
				infoLabel.setText(textures.getInfo());
			}
		});
		Textures current = textures();
		infoLabel.setText(current != null ? current.getInfo() : "No texture displayed");
		if (current != null) {
			current.setUpdateListener(updateInfo);
		}

		final Label sourceLabel = new Label(textureGroup, SWT.NONE);
		sourceLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		sourceLabel.setText("Source:");
		final Combo sourceCombo = new Combo(textureGroup, SWT.READ_ONLY);
		sourceCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		sourceCombo.setItems("ImageJ Image (Static)", "ImageJ Image (Dynamic)", "Image File");
		sourceCombo.setToolTipText(
				"ImageJ Image (Static): a texture of the active ImageJ image (load again after changes, e.g., of the LUT)\nImageJ Image (Dynamic): the texture is updated with the active ImageJ image\nImage File: a texture from an image file");
		sourceCombo.select(0);
		if (current != null) {
			if (!current.isRenderImage()) {
				sourceCombo.select(1);
			} else if (!current.isStaticImageImageJ() && current.isShowTexture()) {
				sourceCombo.select(2);
			}
		}

		final Text pathText = new Text(textureGroup, SWT.BORDER);
		pathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		pathText.setText(store.getString("spatialImage"));
		pathText.setToolTipText("The image file of the texture");
		pathText.addModifyListener(e -> store.setValue("spatialImage", pathText.getText()));
		final Button browseButton = new Button(textureGroup, SWT.NONE);
		browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		browseButton.setText("Browse...");
		browseButton.setToolTipText("Selects an image file for the texture");
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				FileDialog fd = new FileDialog(browseButton.getShell(), SWT.OPEN);
				fd.setText("Load");
				fd.setFilterExtensions(new String[] { "*.png;*.jpg;*.jpeg;*.gif;*.bmp", "*.*" });
				String path = fd.open();
				if (path != null) {
					pathText.setText(path);
				}
			}
		});
		final Runnable updateFileControls = () -> {
			boolean file = sourceCombo.getSelectionIndex() == 2;
			pathText.setEnabled(file);
			browseButton.setEnabled(file);
		};
		updateFileControls.run();
		sourceCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				updateFileControls.run();
			}
		});

		final Button showButton = new Button(textureGroup, SWT.CHECK);

		final Button loadButton = new Button(textureGroup, SWT.NONE);
		GridData gd_loadButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_loadButton.heightHint = 40;
		loadButton.setLayoutData(gd_loadButton);
		loadButton.setText("Load Texture");
		loadButton.setToolTipText("Creates the texture from the selected source");
		loadButton.moveAbove(showButton);
		loadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid == null) {
					return;
				}
				Textures textures = grid.textures;
				textures.setUpdateListener(updateInfo);
				int source = sourceCombo.getSelectionIndex();
				if (source != 2 && WindowManager.getCurrentImage() == null) {
					infoLabel.setText("No image opened in ImageJ!");
					return;
				}
				if (source == 2 && !new java.io.File(pathText.getText()).isFile()) {
					infoLabel.setText("The image file doesn't exist!");
					return;
				}
				if (source == 1) {
					/* Dynamic texture from ImageJ! */
					textures.setRenderFromFile(false);
					DynamicTexture.setImagejEnabled(true);
				} else {
					textures.setRenderFromFile(true);
					textures.setStaticImageImageJ(source == 0);
				}
				/* Created in the OpenGL thread! */
				textures.createNewTexture = true;
				textures.setShowTexture(true);
				showButton.setSelection(true);
			}
		});

		final Button removeButton = new Button(textureGroup, SWT.NONE);
		GridData gd_removeButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_removeButton.heightHint = 40;
		removeButton.setLayoutData(gd_removeButton);
		removeButton.setText("Remove");
		removeButton.setToolTipText("Removes the texture");
		removeButton.moveAbove(showButton);
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Textures textures = textures();
				if (textures != null) {
					textures.setShowTexture(false);
					showButton.setSelection(false);
					infoLabel.setText(textures.getInfo());
				}
			}
		});

		showButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		showButton.setText("Show Texture");
		showButton.setToolTipText("Displays the texture (and the height map, see below)");
		showButton.setSelection(current != null && current.isShowTexture());
		showButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Textures textures = textures();
				if (textures != null) {
					textures.setShowTexture(showButton.getSelection());
					infoLabel.setText(textures.getInfo());
				}
			}
		});

		/* The texture and height map like the image in ImageJ (0,0 at the top left) or flipped! */
		HeightMaps.setFlipVertically(store.getBoolean("textureFlipVertically"));
		final Button flipTextureButton = new Button(textureGroup, SWT.CHECK);
		flipTextureButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		flipTextureButton.setToolTipText(
				"Displays the texture, height map, volume and iso surface vertically flipped.\nBy default the image is displayed like in ImageJ (0,0 at the top left).");
		flipTextureButton.setSelection(HeightMaps.isFlipVertically());
		flipTextureButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				HeightMaps.setFlipVertically(flipTextureButton.getSelection());
				store.setValue("textureFlipVertically", flipTextureButton.getSelection());
			}
		});
		flipTextureButton.setText("Flip Vertically");
		return compositeTexture;
	}

	/* A row with a name, a logarithmic speed slider (0.1 - 100) and the value! */
	private static void speedRow(Composite parent, String name, String toolTip, double value,
			java.util.function.DoubleConsumer apply) {
		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		final SpeedSlider slider = new SpeedSlider(parent, SWT.NONE, 0.1, 100);
		slider.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		slider.setValue(value);
		slider.setToolTipText(toolTip + " in units per frame: left slower, right faster.\n"
				+ "Keys: Left/Right one step (8%), Shift+Left/Right or PageDown/PageUp 10 steps, Home/End minimum/maximum.");
		final Runnable update = () -> label.setText(name + ": " + SpeedSlider.format(slider.getValue()) + " units per frame");
		update.run();
		apply.accept(slider.getValue());
		slider.addListener(SWT.Selection, _ -> {
			apply.accept(slider.getValue());
			update.run();
		});
	}

	/* A text field for a coordinate of the split view camera (red if it is not a number)! */
	private Text coordinateText(Composite parent, double value, java.util.function.DoubleConsumer apply) {
		final Text text = new Text(parent, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		text.setText("" + value);
		text.addListener(SWT.Modify, _ -> {
			try {
				double v = Double.parseDouble(text.getText());
				if (SpatialStructure.getSpatialStructureInstance() != null) {
					apply.accept(v);
				}
				text.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
				errorLabelSplit.setText("");
			} catch (NumberFormatException e) {
				text.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				errorLabelSplit.setText("Number input incorrect!");
			}
		});
		return text;
	}

	/**
	 * Updates the check boxes of the grid planes of the plot view (e.g., after a
	 * script changed them, see SpatialUtil.setPlotGrids). Can be called from any
	 * thread.
	 */
	public static void updatePlotGrids() {
		Display display = Display.getDefault();
		display.asyncExec(() -> {
			SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
			if (grid == null) {
				return;
			}
			boolean[] selected = { grid.plotGridXY, grid.plotGridXZ, grid.plotGridYZ };
			for (int k = 0; k < 3; k++) {
				Button button = plotGridButtons[k];
				if (button != null && !button.isDisposed()) {
					button.setSelection(selected[k]);
				}
			}
		});
	}

	/* A group over the whole width with the columns! */
	private static Group createGroup(Composite parent, String text, int columns, boolean equalWidth) {
		Group group = createGroup(parent, text);
		GridLayout layout = (GridLayout) group.getLayout();
		layout.numColumns = columns;
		layout.makeColumnsEqualWidth = equalWidth;
		return group;
	}

	/*
	 * A row for the size of an axis: the name, a slider (the half size, the labels
	 * show the size from -size to +size) and the size. Returns the slider, the last
	 * child of the parent is the label of the size!
	 */
	private LinearSlider sizeSlider(Composite parent, String axis, String key, java.util.function.IntConsumer apply) {
		Label name = new Label(parent, SWT.NONE);
		name.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		name.setText(axis);
		final LinearSlider slider = new LinearSlider(parent, SWT.NONE);
		slider.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		slider.setMinimum(0);
		slider.setMaximum((int) halfExtent);
		slider.setLabelFactor(2);
		slider.setSelection(1000);
		final Label value = new Label(parent, SWT.RIGHT);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd.widthHint = 60;
		value.setLayoutData(gd);
		final Runnable update = () -> {
			value.setText(String.valueOf(slider.getSelection() * 2));
			slider.setToolTipText("The size of the " + axis.toLowerCase() + " axis: -" + slider.getSelection() + " to +"
					+ slider.getSelection() + "\nKeys: Left/Right one unit, Shift+Left/Right or PageDown/PageUp one major tick, Home/End minimum/maximum.");
		};
		update.run();
		slider.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				apply.accept(slider.getSelection());
				store.setValue(key, slider.getSelection());
				update.run();
			}
		});
		return slider;
	}

	/* A group with two columns over the whole width (a section of a tab)! */
	private static Group createGroup(Composite parent, String text) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(text);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		GridLayout layout = new GridLayout(2, true);
		layout.marginHeight = 8;
		layout.verticalSpacing = 6;
		group.setLayout(layout);
		return group;
	}

	/* A check box in one column! */
	private static Button check(Composite parent, String text, String toolTip) {
		Button button = new Button(parent, SWT.CHECK);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		button.setText(text);
		button.setToolTipText(toolTip);
		return button;
	}

	private static Textures textures() {
		SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
		return grid != null ? grid.textures : null;
	}

	/*
	 * The tab for the volume rendering of an ImageJ stack (see VolumeRenderer)!
	 */
	private void createVolumeTab(CTabFolder tabFolder) {
		final CTabItem volumeItem = new CTabItem(tabFolder, SWT.NONE);
		volumeItem.setText("Volume");
		ScrolledComposite scrollVolume = new ScrolledComposite(tabFolder, SWT.V_SCROLL);
		final Composite compositeVolume = new Composite(scrollVolume, SWT.NONE);
		volumeItem.setControl(scrollVolume);
		compositeVolume.setLayout(new GridLayout(2, true));
		scrollVolume.setMinSize(200, 420);
		scrollVolume.setContent(compositeVolume);
		scrollVolume.setExpandVertical(true);
		scrollVolume.setExpandHorizontal(true);
		scrollVolume.setAlwaysShowScrollBars(true);

		final Label infoLabel = new Label(compositeVolume, SWT.WRAP);
		GridData gd_infoLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_infoLabel.heightHint = 40;
		infoLabel.setLayoutData(gd_infoLabel);
		VolumeRenderer current = volumeRenderer();
		infoLabel.setText(current != null ? current.getInfo() : "No stack loaded");

		final Button loadButton = new Button(compositeVolume, SWT.NONE);
		GridData gd_loadButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_loadButton.heightHint = 40;
		loadButton.setLayoutData(gd_loadButton);
		loadButton.setText("Load Stack");
		loadButton.setToolTipText(
				"Loads the active ImageJ stack as a volume (with the current LUT and display range).\nLoad again after changes of the stack!");
		loadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				VolumeRenderer volume = volumeRenderer();
				if (volume == null) {
					return;
				}
				ImagePlus imp = WindowManager.getCurrentImage();
				if (imp == null) {
					infoLabel.setText("No image opened in ImageJ!");
				} else if (volume.load(imp)) {
					infoLabel.setText(volume.getInfo());
				}
			}
		});

		final Button clearButton = new Button(compositeVolume, SWT.NONE);
		GridData gd_clearButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_clearButton.heightHint = 40;
		clearButton.setLayoutData(gd_clearButton);
		clearButton.setText("Remove");
		clearButton.setToolTipText("Removes the volume");
		clearButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				VolumeRenderer volume = volumeRenderer();
				if (volume != null) {
					volume.clear();
					infoLabel.setText(volume.getInfo());
				}
			}
		});

		final Button showButton = new Button(compositeVolume, SWT.CHECK);
		showButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		showButton.setText("Show Volume");
		showButton.setSelection(true);
		showButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				VolumeRenderer volume = volumeRenderer();
				if (volume != null) {
					volume.setShow(showButton.getSelection());
				}
			}
		});

		final Button lutButton = new Button(compositeVolume, SWT.CHECK);
		lutButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		lutButton.setText("LUT Colors");
		lutButton.setToolTipText("Uses the colors of the LUT, else gray values");
		lutButton.setSelection(true);
		lutButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				VolumeRenderer volume = volumeRenderer();
				if (volume != null) {
					volume.setLutColors(lutButton.getSelection());
					volume.reload();
				}
			}
		});

		final Label modeLabel = new Label(compositeVolume, SWT.NONE);
		modeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		modeLabel.setText("Mode:");
		final Combo modeCombo = new Combo(compositeVolume, SWT.READ_ONLY);
		modeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		modeCombo.setItems("Alpha Blending", "Maximum Intensity");
		/* The current mode of the volume (or the default) */
		VolumeRenderer currentVolume = volumeRenderer();
		modeCombo.select(currentVolume != null ? currentVolume.getMode() : VolumeRenderer.MODE_ALPHA_BLENDING);
		modeCombo.setToolTipText(
				"Alpha Blending: transparent voxels (see Opacity)\nMaximum Intensity: the maximum along the view direction");
		modeCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				VolumeRenderer volume = volumeRenderer();
				if (volume != null) {
					volume.setMode(modeCombo.getSelectionIndex());
				}
			}
		});

		/* The transfer function (colors and opacities of the voxels) */
		final Button tfButton = new Button(compositeVolume, SWT.CHECK);
		tfButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		tfButton.setText("Transfer Function");
		tfButton.setToolTipText("Colors and opacities of the voxels from a transfer function (see Edit...)");
		final Button tfEditButton = new Button(compositeVolume, SWT.PUSH);
		tfEditButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		tfEditButton.setText("Edit...");
		tfEditButton.setToolTipText("Opens the transfer function editor (opacity curve and color map)");
		tfButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				VolumeRenderer volume = volumeRenderer();
				if (volume != null) {
					volume.setTransferFunction(tfButton.getSelection() ? volumeTransferFunction(volume) : null);
				}
			}
		});
		tfEditButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				VolumeRenderer volume = volumeRenderer();
				if (volume == null) {
					return;
				}
				TransferFunction tf = volumeTransferFunction(volume);
				tfButton.setSelection(true);
				volume.setTransferFunction(tf);
				TransferFunctionEditor.openDialog(tfEditButton.getShell(), "Volume Transfer Function", tf,
						volume.getHistogram(), volume.getValueMin(), volume.getValueMax(), volume.getColorModel(), t -> {
							VolumeRenderer v = volumeRenderer();
							if (v != null && !tfButton.isDisposed() && tfButton.getSelection()) {
								v.setTransferFunction(t);
							}
						});
			}
		});

		final Label opacityLabel = createVolumeScaleLabel(compositeVolume, "Opacity: 0.05");
		final Scale opacityScale = createVolumeScale(compositeVolume, 1, 100, 5,
				"The opacity of the voxels (Alpha Blending)");
		opacityScale.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				float value = opacityScale.getSelection() / 100.0f;
				opacityLabel.setText("Opacity: " + String.format("%.2f", value));
				VolumeRenderer volume = volumeRenderer();
				if (volume != null) {
					volume.setOpacity(value);
				}
			}
		});

		final Label thresholdLabel = createVolumeScaleLabel(compositeVolume, "Threshold: 10");
		final Scale thresholdScale = createVolumeScale(compositeVolume, 0, 255, 10,
				"Voxels with an intensity (0-255, scaled to the display range) below the threshold are not displayed");
		thresholdScale.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				thresholdLabel.setText("Threshold: " + thresholdScale.getSelection());
				VolumeRenderer volume = volumeRenderer();
				if (volume != null) {
					volume.setThreshold(thresholdScale.getSelection() / 255.0f);
				}
			}
		});

		final Label samplingLabel = createVolumeScaleLabel(compositeVolume, "Sampling: 1.0x");
		final Scale samplingScale = createVolumeScale(compositeVolume, 1, 8, 2,
				"The number of slices relative to the number of voxels (more slices are smoother but slower)");
		samplingScale.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				float value = samplingScale.getSelection() / 2.0f;
				samplingLabel.setText("Sampling: " + String.format("%.1f", value) + "x");
				VolumeRenderer volume = volumeRenderer();
				if (volume != null) {
					volume.setSampling(value);
				}
			}
		});

		final Label zScaleLabel = new Label(compositeVolume, SWT.NONE);
		zScaleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		zScaleLabel.setText("Z Scale:");
		final Spinner zScaleSpinner = new Spinner(compositeVolume, SWT.BORDER);
		zScaleSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		zScaleSpinner.setDigits(2);
		zScaleSpinner.setMinimum(1);
		zScaleSpinner.setMaximum(10000);
		zScaleSpinner.setIncrement(10);
		zScaleSpinner.setSelection(100);
		zScaleSpinner.setToolTipText("Scales the depth of the volume (the calibrated voxel depth is used by default)");
		zScaleSpinner.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				VolumeRenderer volume = volumeRenderer();
				if (volume != null) {
					volume.setZScale(zScaleSpinner.getSelection() / 100.0f);
				}
			}
		});

		final Label maxSizeLabel = new Label(compositeVolume, SWT.NONE);
		maxSizeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		maxSizeLabel.setText("Max Size:");
		final Spinner maxSizeSpinner = new Spinner(compositeVolume, SWT.BORDER);
		maxSizeSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		maxSizeSpinner.setMinimum(16);
		maxSizeSpinner.setMaximum(2048);
		maxSizeSpinner.setIncrement(32);
		maxSizeSpinner.setSelection(256);
		maxSizeSpinner.setToolTipText(
				"The maximum number of voxels of a dimension (larger stacks are reduced).\nA loaded stack is loaded again.");
		final Runnable reloadVolume = () -> {
			VolumeRenderer volume = volumeRenderer();
			if (volume != null && volume.getImage() != null) {
				runInBackground(volume::reload, "Bio7 Volume Reload");
			}
		};
		maxSizeSpinner.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				VolumeRenderer volume = volumeRenderer();
				if (volume != null) {
					volume.setMaxSize(maxSizeSpinner.getSelection());
					/* Loaded again after a short delay (not for every step of the spinner) */
					Display display = maxSizeSpinner.getDisplay();
					display.timerExec(-1, reloadVolume);
					display.timerExec(500, reloadVolume);
				}
			}
		});
	}

	/*
	 * The tab for the iso surface of an ImageJ stack (see IsoSurface)!
	 */
	private void createIsoSurfaceTab(CTabFolder tabFolder) {
		final CTabItem isoItem = new CTabItem(tabFolder, SWT.NONE);
		isoItem.setText("Iso Surface");
		ScrolledComposite scrollIso = new ScrolledComposite(tabFolder, SWT.V_SCROLL);
		final Composite compositeIso = new Composite(scrollIso, SWT.NONE);
		isoItem.setControl(scrollIso);
		compositeIso.setLayout(new GridLayout(2, true));
		scrollIso.setMinSize(200, 600);
		scrollIso.setContent(compositeIso);
		scrollIso.setExpandVertical(true);
		scrollIso.setExpandHorizontal(true);
		scrollIso.setAlwaysShowScrollBars(true);

		final Label infoLabel = new Label(compositeIso, SWT.WRAP);
		GridData gd_infoLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_infoLabel.heightHint = 40;
		infoLabel.setLayoutData(gd_infoLabel);
		/* Shows the size of the computed surface! */
		final Runnable updateInfo = () -> Display.getDefault().asyncExec(() -> {
			IsoSurface iso = isoSurface();
			if (!infoLabel.isDisposed() && iso != null) {
				infoLabel.setText(iso.getInfo());
			}
		});
		IsoSurface current = isoSurface();
		infoLabel.setText(current != null ? current.getInfo() : "No stack loaded");
		if (current != null) {
			current.setUpdateListener(updateInfo);
		}

		final Label isoValueLabel = new Label(compositeIso, SWT.NONE);
		final Scale isoValueScale = new Scale(compositeIso, SWT.NONE);

		final Button loadButton = new Button(compositeIso, SWT.NONE);
		GridData gd_loadButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_loadButton.heightHint = 40;
		loadButton.setLayoutData(gd_loadButton);
		loadButton.setText("Load Stack");
		loadButton.setToolTipText(
				"Loads the active ImageJ stack and computes the iso surface.\nThe default iso value is the threshold (if set) or the center of the display range.");
		loadButton.moveAbove(isoValueLabel);

		final Button clearButton = new Button(compositeIso, SWT.NONE);
		GridData gd_clearButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_clearButton.heightHint = 40;
		clearButton.setLayoutData(gd_clearButton);
		clearButton.setText("Remove");
		clearButton.setToolTipText("Removes the iso surface");
		clearButton.moveAbove(isoValueLabel);
		clearButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IsoSurface iso = isoSurface();
				if (iso != null) {
					iso.clear();
				}
			}
		});

		final Button showButton = new Button(compositeIso, SWT.CHECK);
		showButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		showButton.setText("Show Surface");
		showButton.setSelection(true);
		showButton.moveAbove(isoValueLabel);
		showButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IsoSurface iso = isoSurface();
				if (iso != null) {
					iso.setShow(showButton.getSelection());
				}
			}
		});

		final Button headLightButton = new Button(compositeIso, SWT.CHECK);
		headLightButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		headLightButton.setText("Head Light");
		headLightButton.setToolTipText("A light from the viewer (else the lights of the scene)");
		headLightButton.setSelection(true);
		headLightButton.moveAbove(isoValueLabel);
		headLightButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IsoSurface iso = isoSurface();
				if (iso != null) {
					iso.setHeadLight(headLightButton.getSelection());
				}
			}
		});

		final Button wireframeButton = new Button(compositeIso, SWT.CHECK);
		wireframeButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		wireframeButton.setText("Show Triangles");
		wireframeButton.setToolTipText("Draws the triangles of the surface as lines over the surface");
		wireframeButton.moveAbove(isoValueLabel);
		wireframeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IsoSurface iso = isoSurface();
				if (iso != null) {
					iso.setWireframe(wireframeButton.getSelection());
				}
			}
		});

		isoValueLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		isoValueLabel.setText("Iso Value:");
		isoValueScale.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		isoValueScale.setMinimum(0);
		isoValueScale.setMaximum(1000);
		isoValueScale.setSelection(500);
		isoValueScale.setToolTipText("The iso value (the display range of the image)");
		isoValueScale.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IsoSurface iso = isoSurface();
				if (iso != null) {
					double min = iso.getDisplayMin();
					double max = iso.getDisplayMax();
					double value = min + (max - min) * isoValueScale.getSelection() / 1000.0;
					isoValueLabel.setText("Iso Value: " + String.format("%.4g", value));
					iso.setIsoValue(value);
				}
			}
		});

		loadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IsoSurface iso = isoSurface();
				if (iso == null) {
					return;
				}
				ImagePlus imp = WindowManager.getCurrentImage();
				iso.setUpdateListener(updateInfo);
				if (imp == null) {
					infoLabel.setText("No image opened in ImageJ!");
				} else if (iso.load(imp)) {
					infoLabel.setText(iso.getInfo());
					double min = iso.getDisplayMin();
					double max = iso.getDisplayMax();
					double range = max > min ? max - min : 1;
					isoValueScale.setSelection((int) Math.round((iso.getIsoValue() - min) / range * 1000));
					isoValueLabel.setText("Iso Value: " + String.format("%.4g", iso.getIsoValue()));
				}
			}
		});

		final Label smoothingLabel = createVolumeScaleLabel(compositeIso, "Smoothing: 0.0");
		final Scale smoothingScale = createVolumeScale(compositeIso, 0, 30, 0,
				"The Gaussian smoothing (sigma in voxels) of the stack before the surface is computed.\nThe measurements use the same surface (0 = the data at the iso value).");
		smoothingScale.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				float value = smoothingScale.getSelection() / 10.0f;
				smoothingLabel.setText("Smoothing: " + String.format("%.1f", value));
				IsoSurface iso = isoSurface();
				if (iso != null) {
					iso.setSmoothing(value);
				}
			}
		});

		final Label meshSmoothingLabel = createVolumeScaleLabel(compositeIso, "Mesh Smoothing: 5");
		final Scale meshSmoothingScale = createVolumeScale(compositeIso, 0, 30, 5,
				"The number of smoothing iterations of the mesh (Taubin smoothing, the surface doesn't shrink)");
		meshSmoothingScale.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				meshSmoothingLabel.setText("Mesh Smoothing: " + meshSmoothingScale.getSelection());
				IsoSurface iso = isoSurface();
				if (iso != null) {
					iso.setMeshSmoothing(meshSmoothingScale.getSelection());
				}
			}
		});

		final Label opacityLabel = createVolumeScaleLabel(compositeIso, "Opacity: 1.00");
		final Scale opacityScale = createVolumeScale(compositeIso, 5, 100, 100, "The opacity of the surface");
		opacityScale.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				float value = opacityScale.getSelection() / 100.0f;
				opacityLabel.setText("Opacity: " + String.format("%.2f", value));
				IsoSurface iso = isoSurface();
				if (iso != null) {
					iso.setOpacity(value);
				}
			}
		});

		final Button lutColorButton = new Button(compositeIso, SWT.CHECK);
		lutColorButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		lutColorButton.setText("LUT Color");
		lutColorButton.setToolTipText("Uses the color of the LUT at the iso value (else the selected color)");
		lutColorButton.setSelection(true);
		lutColorButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IsoSurface iso = isoSurface();
				if (iso != null) {
					iso.setLutColor(lutColorButton.getSelection());
				}
			}
		});
		final Button colorButton = new Button(compositeIso, SWT.NONE);
		colorButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		colorButton.setText("Color...");
		colorButton.setToolTipText("Selects the color of the surface");
		colorButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IsoSurface iso = isoSurface();
				if (iso == null) {
					return;
				}
				ColorDialog dialog = new ColorDialog(colorButton.getShell());
				float[] c = iso.getColor();
				dialog.setRGB(new RGB(Math.round(c[0] * 255), Math.round(c[1] * 255), Math.round(c[2] * 255)));
				RGB rgb = dialog.open();
				if (rgb != null) {
					iso.setColor(rgb.red / 255f, rgb.green / 255f, rgb.blue / 255f);
					iso.setLutColor(false);
					lutColorButton.setSelection(false);
				}
			}
		});

		final Label colorModeLabel = new Label(compositeIso, SWT.NONE);
		colorModeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		colorModeLabel.setText("Colors:");
		final Combo colorModeCombo = new Combo(compositeIso, SWT.READ_ONLY);
		colorModeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		colorModeCombo.setItems("Single Color", "Intensity (LUT)", "Depth (LUT)", "Image Colors");
		/* The current color mode of the surface (or the default) */
		IsoSurface currentIso = isoSurface();
		colorModeCombo.select(currentIso != null ? currentIso.getColorMode() : IsoSurface.COLOR_SINGLE);
		colorModeCombo.setToolTipText(
				"Single Color: the LUT color at the iso value or the selected color\nIntensity (LUT): the intensity of the stack (or of the color channel) inside of the surface\nDepth (LUT): the z position (Viridis for a gray LUT)\nImage Colors: the displayed colors of the image (RGB, composite or LUT colors)");
		final Label colorChannelLabel = new Label(compositeIso, SWT.NONE);
		colorChannelLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		colorChannelLabel.setText("Color Channel:");
		final Spinner colorChannelSpinner = new Spinner(compositeIso, SWT.BORDER);
		colorChannelSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		colorChannelSpinner.setMinimum(0);
		colorChannelSpinner.setMaximum(64);
		colorChannelSpinner.setSelection(0);
		colorChannelSpinner.setEnabled(colorModeCombo.getSelectionIndex() == IsoSurface.COLOR_INTENSITY);
		colorChannelSpinner.setToolTipText(
				"The channel of a multichannel stack for the intensity colors (0 = the channel of the surface)");
		colorModeCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IsoSurface iso = isoSurface();
				if (iso != null) {
					iso.setColorMode(colorModeCombo.getSelectionIndex());
				}
				colorChannelSpinner.setEnabled(colorModeCombo.getSelectionIndex() == IsoSurface.COLOR_INTENSITY);
			}
		});
		colorChannelSpinner.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IsoSurface iso = isoSurface();
				if (iso != null) {
					iso.setUpdateListener(updateInfo);
					iso.setColorChannel(colorChannelSpinner.getSelection());
				}
			}
		});

		/* The transfer function (colors and the opacity mapping) */
		final Button isoTfButton = new Button(compositeIso, SWT.CHECK);
		isoTfButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		isoTfButton.setText("Transfer Function");
		isoTfButton.setToolTipText("Colors (and opacities) of the surface from a transfer function (see Edit...)");
		final Button isoTfEditButton = new Button(compositeIso, SWT.PUSH);
		isoTfEditButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		isoTfEditButton.setText("Edit...");
		isoTfEditButton.setToolTipText("Opens the transfer function editor (color map and opacity curve)");
		final Button opacityMappingButton = new Button(compositeIso, SWT.CHECK);
		opacityMappingButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		opacityMappingButton.setText("Opacity Mapping");
		opacityMappingButton.setToolTipText(
				"Uses the opacity curve of the transfer function for the surface (transparent where the opacity is low)");
		isoTfButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IsoSurface iso = isoSurface();
				if (iso != null) {
					iso.setTransferFunction(isoTfButton.getSelection() ? isoTransferFunction(iso) : null);
					if (isoTfButton.getSelection()) {
						colorModeCombo.select(iso.getColorMode());
						colorChannelSpinner.setEnabled(iso.getColorMode() == IsoSurface.COLOR_INTENSITY);
					}
				}
			}
		});
		opacityMappingButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IsoSurface iso = isoSurface();
				if (iso != null) {
					iso.setOpacityMapping(opacityMappingButton.getSelection());
				}
			}
		});
		isoTfEditButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IsoSurface iso = isoSurface();
				if (iso == null) {
					return;
				}
				TransferFunction tf = isoTransferFunction(iso);
				isoTfButton.setSelection(true);
				iso.setTransferFunction(tf);
				colorModeCombo.select(iso.getColorMode());
				colorChannelSpinner.setEnabled(iso.getColorMode() == IsoSurface.COLOR_INTENSITY);
				/* The histogram of the values on the surface and the position of the iso value */
				TransferFunctionEditor.openDialog(isoTfEditButton.getShell(), "Iso Surface Transfer Function", tf,
						iso.getSurfaceHistogram(), iso.getColorMin(), iso.getColorMax(), iso.getColorModel(),
						iso.getIsoPosition(), "iso value", t -> {
							IsoSurface s = isoSurface();
							if (s != null && !isoTfButton.isDisposed() && isoTfButton.getSelection()) {
								s.setTransferFunction(t);
							}
						});
			}
		});

		final Label colorDepthLabel = new Label(compositeIso, SWT.NONE);
		colorDepthLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		colorDepthLabel.setText("Color Depth:");
		final Spinner colorDepthSpinner = new Spinner(compositeIso, SWT.BORDER);
		colorDepthSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		colorDepthSpinner.setDigits(1);
		colorDepthSpinner.setMinimum(0);
		colorDepthSpinner.setMaximum(300);
		colorDepthSpinner.setIncrement(5);
		colorDepthSpinner.setSelection(10);
		colorDepthSpinner.setToolTipText(
				"The distance (voxels) inside of the surface where the intensity for the colors is sampled.\nThe values at the surface are close to the iso value, a larger depth shows the structure inside.");
		colorDepthSpinner.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IsoSurface iso = isoSurface();
				if (iso != null) {
					iso.setColorSampleDepth(colorDepthSpinner.getSelection() / 10f);
				}
			}
		});

		final Label zScaleLabel = new Label(compositeIso, SWT.NONE);
		zScaleLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		zScaleLabel.setText("Z Scale:");
		final Spinner zScaleSpinner = new Spinner(compositeIso, SWT.BORDER);
		zScaleSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		zScaleSpinner.setDigits(2);
		zScaleSpinner.setMinimum(1);
		zScaleSpinner.setMaximum(10000);
		zScaleSpinner.setIncrement(10);
		zScaleSpinner.setSelection(100);
		zScaleSpinner.setToolTipText("Scales the depth of the surface (the calibrated voxel depth is used by default)");
		zScaleSpinner.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IsoSurface iso = isoSurface();
				if (iso != null) {
					iso.setZScale(zScaleSpinner.getSelection() / 100.0f);
				}
			}
		});

		/* The reduced display (optional, the measurements use the full resolution) */
		final Button reduceButton = new Button(compositeIso, SWT.CHECK);
		reduceButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		reduceButton.setText("Reduce Display (Max Size)");
		reduceButton.setSelection(false);
		reduceButton.setToolTipText(
				"Reduces the displayed surface of large stacks to the maximum size (faster).\nThe measurements always use the full resolution.");
		final Label maxSizeLabel = new Label(compositeIso, SWT.NONE);
		maxSizeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		maxSizeLabel.setText("Max Size:");
		final Spinner maxSizeSpinner = new Spinner(compositeIso, SWT.BORDER);
		maxSizeSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		maxSizeSpinner.setMinimum(16);
		maxSizeSpinner.setMaximum(2048);
		maxSizeSpinner.setIncrement(32);
		maxSizeSpinner.setSelection(256);
		maxSizeSpinner.setEnabled(false);
		maxSizeSpinner.setToolTipText(
				"The maximum number of voxels of a dimension of the reduced display.\nA loaded stack is loaded again (the measurements use the full resolution).");
		final Runnable reloadIso = () -> {
			IsoSurface iso = isoSurface();
			if (iso != null && iso.getImage() != null) {
				runInBackground(iso::reload, "Bio7 Iso Surface Reload");
			}
		};
		maxSizeSpinner.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IsoSurface iso = isoSurface();
				if (iso != null) {
					iso.setMaxSize(maxSizeSpinner.getSelection());
					/* Loaded again after a short delay (not for every step of the spinner) */
					Display display = maxSizeSpinner.getDisplay();
					display.timerExec(-1, reloadIso);
					display.timerExec(500, reloadIso);
				}
			}
		});
		reduceButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				maxSizeSpinner.setEnabled(reduceButton.getSelection());
				IsoSurface iso = isoSurface();
				if (iso != null) {
					iso.setReduceDisplay(reduceButton.getSelection());
					iso.setMaxSize(maxSizeSpinner.getSelection());
					reloadIso.run();
				}
			}
		});

		final Button exportButton = new Button(compositeIso, SWT.NONE);
		GridData gd_exportButton = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_exportButton.heightHint = 40;
		exportButton.setLayoutData(gd_exportButton);
		exportButton.setText("Export...");
		exportButton.setToolTipText(
				"Exports the iso surface (the coordinates are calibrated):\nOBJ (with a *.mtl file for the color), STL (3D printing),\nPLY (with normals and colors), glTF binary (*.glb, with a material) or VTK (e.g., ParaView)");
		exportButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IsoSurface iso = isoSurface();
				if (iso == null) {
					return;
				}
				FileDialog dialog = new FileDialog(exportButton.getShell(), SWT.SAVE);
				dialog.setFilterExtensions(new String[] { "*.obj", "*.stl", "*.ply", "*.glb", "*.vtk" });
				dialog.setFilterNames(new String[] { "Wavefront OBJ (*.obj)", "STL binary (*.stl)",
						"PLY binary (*.ply)", "glTF binary (*.glb)", "VTK legacy (*.vtk)" });
				dialog.setFileName("isosurface.obj");
				dialog.setOverwrite(true);
				String path = dialog.open();
				if (path == null) {
					return;
				}
				/* Without a known extension the extension of the selected file type! */
				String lower = path.toLowerCase();
				boolean known = false;
				for (String format : IsoSurface.EXPORT_FORMATS) {
					known |= lower.endsWith("." + format);
				}
				if (!known) {
					int index = Math.max(0, dialog.getFilterIndex());
					path = path + "." + IsoSurface.EXPORT_FORMATS[index];
				}
				final String file = path;
				infoLabel.setText(iso.getInfo() + "\nExporting...");
				/* Large surfaces: export in the background! */
				Thread export = new Thread(() -> {
					String message;
					try {
						iso.export(new java.io.File(file));
						message = iso.getInfo() + "\nExported: " + file;
					} catch (java.io.IOException ex) {
						message = "Export failed: " + ex.getMessage();
					}
					final String text = message;
					Display.getDefault().asyncExec(() -> {
						if (!infoLabel.isDisposed()) {
							infoLabel.setText(text);
						}
					});
				}, "Bio7 Iso Surface Export");
				export.setDaemon(true);
				export.start();
			}
		});
	}

	private static IsoSurface isoSurface() {
		SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
		return grid != null ? grid.getIsoSurface() : null;
	}

	/* The edited transfer functions (kept while the view is opened) */
	private TransferFunction volumeTf;
	private TransferFunction isoTf;

	/* The transfer function of the volume (a new one with the LUT of the stack)! */
	private TransferFunction volumeTransferFunction(VolumeRenderer volume) {
		if (volumeTf == null) {
			ColorModel cm = volume.getColorModel();
			volumeTf = cm != null ? TransferFunction.fromColorModel(cm) : new TransferFunction();
		}
		return volumeTf;
	}

	/* The transfer function of the iso surface (a new one with the LUT of the stack)! */
	private TransferFunction isoTransferFunction(IsoSurface iso) {
		if (isoTf == null) {
			ColorModel cm = iso.getColorModel();
			isoTf = cm != null ? TransferFunction.fromColorModel(cm) : new TransferFunction();
			// Opaque surfaces by default (see the opacity mapping)
			isoTf.setConstantOpacity(1);
		}
		return isoTf;
	}

	/*
	 * The tab for the clipping of the volume and the iso surface (see Clipping)!
	 */
	private void createClippingTab(CTabFolder tabFolder) {
		final CTabItem clipItem = new CTabItem(tabFolder, SWT.NONE);
		clipItem.setText("Clipping");
		ScrolledComposite scrollClip = new ScrolledComposite(tabFolder, SWT.V_SCROLL);
		final Composite compositeClip = new Composite(scrollClip, SWT.NONE);
		clipItem.setControl(scrollClip);
		compositeClip.setLayout(new GridLayout(3, false));
		scrollClip.setMinSize(200, 800);
		scrollClip.setContent(compositeClip);
		scrollClip.setExpandVertical(true);
		scrollClip.setExpandHorizontal(true);
		scrollClip.setAlwaysShowScrollBars(true);

		final Label infoLabel = new Label(compositeClip, SWT.WRAP);
		GridData gd_infoLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		gd_infoLabel.heightHint = 40;
		infoLabel.setLayoutData(gd_infoLabel);
		infoLabel.setText("Clips the volume and the iso surface (box and cutting plane).");

		/* The clipping box (min and max of every axis) */
		final Button boxButton = new Button(compositeClip, SWT.CHECK);
		boxButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		boxButton.setText("Clipping Box");
		boxButton.setToolTipText("Removes the parts outside of the box");
		boxButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Clipping clip = SpatialUtil.getClipping();
				if (clip != null) {
					clip.setBoxEnabled(boxButton.getSelection());
				}
			}
		});
		final Scale[] boxScales = new Scale[6];
		String[] axes = { "X", "Y", "Z" };
		for (int a = 0; a < 3; a++) {
			Label label = new Label(compositeClip, SWT.NONE);
			label.setText(axes[a] + ":");
			for (int m = 0; m < 2; m++) {
				final int axis = a;
				final boolean max = m == 1;
				Scale scale = new Scale(compositeClip, SWT.NONE);
				scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
				scale.setMinimum(0);
				scale.setMaximum(1000);
				scale.setSelection(max ? 1000 : 0);
				scale.setToolTipText((max ? "Maximum" : "Minimum") + " of the " + axes[a] + " axis");
				scale.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(final SelectionEvent e) {
						Clipping clip = SpatialUtil.getClipping();
						if (clip != null) {
							clip.setBoxLimit(axis, max, scale.getSelection() / 1000.0);
							clip.setBoxEnabled(true);
							boxButton.setSelection(true);
						}
					}
				});
				boxScales[a * 2 + m] = scale;
			}
		}

		/* The cutting plane */
		final Button planeButton = new Button(compositeClip, SWT.CHECK);
		planeButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		planeButton.setText("Cutting Plane");
		planeButton.setToolTipText("Removes the part in front of the plane (see Flip Side)");
		final Scale azimuthScale = createClipScale(compositeClip, "Azimuth:", 0, 360, 0,
				"The direction of the plane normal around the vertical axis (degree)");
		final Scale elevationScale = createClipScale(compositeClip, "Elevation:", 0, 180, 90,
				"The elevation of the plane normal (-90 - 90 degree)");
		final Scale offsetScale = createClipScale(compositeClip, "Offset:", 0, 200, 100,
				"The position of the plane along its normal (the center is in the middle)");
		final Runnable applyPlane = () -> {
			Clipping clip = SpatialUtil.getClipping();
			if (clip != null) {
				clip.setPlane(azimuthScale.getSelection(), elevationScale.getSelection() - 90,
						(offsetScale.getSelection() - 100) / 100.0);
				clip.setPlaneEnabled(true);
				planeButton.setSelection(true);
			}
		};
		for (Scale scale : new Scale[] { azimuthScale, elevationScale, offsetScale }) {
			scale.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent e) {
					applyPlane.run();
				}
			});
		}
		planeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Clipping clip = SpatialUtil.getClipping();
				if (clip != null) {
					if (planeButton.getSelection()) {
						applyPlane.run();
					} else {
						clip.setPlaneEnabled(false);
					}
				}
			}
		});

		final Button flipButton = new Button(compositeClip, SWT.CHECK);
		flipButton.setText("Flip Side");
		flipButton.setToolTipText("Removes the other side of the plane");
		flipButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Clipping clip = SpatialUtil.getClipping();
				if (clip != null) {
					clip.setFlip(flipButton.getSelection());
				}
			}
		});
		final Button outlineButton = new Button(compositeClip, SWT.CHECK);
		outlineButton.setText("Outline");
		outlineButton.setSelection(true);
		outlineButton.setToolTipText("Draws the outline of the cutting plane");
		outlineButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Clipping clip = SpatialUtil.getClipping();
				if (clip != null) {
					clip.setOutline(outlineButton.getSelection());
				}
			}
		});
		final Button sliceButton = new Button(compositeClip, SWT.CHECK);
		sliceButton.setText("Slice");
		sliceButton.setToolTipText(
				"Displays the cross-section of the plane with the data (the colors of the Volume tab: LUT or transfer function)");
		sliceButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Clipping clip = SpatialUtil.getClipping();
				VolumeRenderer volume = volumeRenderer();
				IsoSurface iso = isoSurface();
				if (clip == null || volume == null) {
					return;
				}
				if (sliceButton.getSelection() && !volume.isLoaded()) {
					/* The slice needs the data of the volume: the stack of the iso surface (not displayed) */
					if (iso != null && iso.getImage() != null) {
						volume.load(iso.getImage());
						volume.setShow(false);
						infoLabel.setText("The slice uses the stack of the iso surface (the volume is not displayed).");
					} else {
						infoLabel.setText("Load a stack in the Volume or Iso Surface tab for the slice!");
					}
				}
				clip.setSlice(sliceButton.getSelection());
			}
		});

		/* The cut of the iso surface mesh (closed with caps, exported) */
		final Button cutMeshButton = new Button(compositeClip, SWT.CHECK);
		cutMeshButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		cutMeshButton.setText("Cut Iso Surface Mesh (Closed, Export)");
		cutMeshButton.setToolTipText(
				"Cuts the mesh of the iso surface with the box and the plane (closed with caps, the export contains the cut surface)");
		cutMeshButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Clipping clip = SpatialUtil.getClipping();
				if (clip != null) {
					clip.setCutMesh(cutMeshButton.getSelection());
				}
			}
		});

		/* The cuts with a selection or a mask image (removed voxels) */
		final Label cutLabel = new Label(compositeClip, SWT.NONE);
		cutLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		cutLabel.setText("Cut with the Selection of the Current Image:");
		Label sliceLabel = new Label(compositeClip, SWT.NONE);
		sliceLabel.setText("Slices:");
		final Spinner firstSpinner = new Spinner(compositeClip, SWT.BORDER);
		firstSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		firstSpinner.setMinimum(1);
		firstSpinner.setMaximum(100000);
		firstSpinner.setSelection(1);
		firstSpinner.setToolTipText("The first slice of the cut");
		final Spinner lastSpinner = new Spinner(compositeClip, SWT.BORDER);
		lastSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		lastSpinner.setMinimum(1);
		lastSpinner.setMaximum(100000);
		lastSpinner.setSelection(100000);
		lastSpinner.setToolTipText("The last slice of the cut (larger values are the last slice)");
		final Runnable[] showMask = new Runnable[1];
		for (int i = 0; i < 2; i++) {
			final boolean inside = i == 0;
			Button cutButton = new Button(compositeClip, SWT.PUSH);
			cutButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, i == 0 ? 2 : 1, 1));
			cutButton.setText(inside ? "Remove Inside" : "Remove Outside");
			cutButton.setToolTipText("Removes the voxels " + (inside ? "inside" : "outside")
					+ " of the selection (area) of the current image in the slices");
			cutButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent e) {
					Clipping clip = SpatialUtil.getClipping();
					if (clip == null) {
						return;
					}
					ImagePlus imp = WindowManager.getCurrentImage();
					if (clip.cutRoi(imp, inside, firstSpinner.getSelection(), lastSpinner.getSelection())) {
						showMask[0].run();
					} else {
						infoLabel.setText("The current image has no area selection!");
					}
				}
			});
		}
		final Button maskButton = new Button(compositeClip, SWT.PUSH);
		maskButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		maskButton.setText("Mask Image...");
		maskButton.setToolTipText("Removes the voxels with the value 0 of a mask image (binary stack)");
		maskButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				/* A menu of the open images */
				Menu menu = new Menu(maskButton);
				int[] ids = WindowManager.getIDList();
				if (ids == null) {
					infoLabel.setText("No image is open!");
					return;
				}
				for (int id : ids) {
					ImagePlus imp = WindowManager.getImage(id);
					if (imp == null) {
						continue;
					}
					MenuItem item = new MenuItem(menu, SWT.PUSH);
					item.setText(imp.getTitle());
					item.addListener(SWT.Selection, _ -> {
						Clipping clip = SpatialUtil.getClipping();
						if (clip != null) {
							clip.setMask(imp);
							showMask[0].run();
						}
					});
				}
				menu.addListener(SWT.Hide, _ -> maskButton.getDisplay().asyncExec(menu::dispose));
				menu.setLocation(maskButton.toDisplay(0, maskButton.getSize().y));
				menu.setVisible(true);
			}
		});
		final Button invertButton = new Button(compositeClip, SWT.PUSH);
		invertButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		invertButton.setText("Invert");
		invertButton.setToolTipText("Inverts the removed voxels");
		final Button undoButton = new Button(compositeClip, SWT.PUSH);
		undoButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		undoButton.setText("Undo Cut");
		undoButton.setToolTipText("Undoes the last cut");
		final Button clearButton = new Button(compositeClip, SWT.PUSH);
		clearButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		clearButton.setText("Clear Cuts");
		clearButton.setToolTipText("Removes all cuts (the mask)");
		showMask[0] = () -> {
			Clipping clip = SpatialUtil.getClipping();
			Clipping.Mask mask = clip != null ? clip.getMask() : null;
			infoLabel.setText(mask == null ? "No voxels are removed."
					: "Removed voxels: " + mask.getRemovedCount() + " (mask " + mask.getWidth() + " x "
							+ mask.getHeight() + " x " + mask.getDepth() + ")");
		};
		invertButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Clipping clip = SpatialUtil.getClipping();
				if (clip != null) {
					clip.invertMask();
					showMask[0].run();
				}
			}
		});
		undoButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Clipping clip = SpatialUtil.getClipping();
				if (clip != null) {
					clip.undoCut();
					showMask[0].run();
				}
			}
		});
		clearButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Clipping clip = SpatialUtil.getClipping();
				if (clip != null) {
					clip.clearMask();
					showMask[0].run();
				}
			}
		});

		/* The mouse tools in the 3D view (left mouse button) */
		Label toolLabel = new Label(compositeClip, SWT.NONE);
		toolLabel.setText("Mouse:");
		final Combo toolCombo = new Combo(compositeClip, SWT.READ_ONLY);
		toolCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		toolCombo.setItems("Rotate View", "Move Plane (Shift: Rotate)", "Erase Voxels (Shift: Restore)");
		toolCombo.select(0);
		toolCombo.setToolTipText("The tool of the left mouse button in the 3D view");
		Label eraserLabel = new Label(compositeClip, SWT.NONE);
		eraserLabel.setText("Eraser Size:");
		final Spinner eraserSpinner = new Spinner(compositeClip, SWT.BORDER);
		eraserSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		eraserSpinner.setMinimum(1);
		eraserSpinner.setMaximum(200);
		eraserSpinner.setSelection(5);
		eraserSpinner.setToolTipText("The radius of the eraser (voxels)");
		eraserSpinner.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Clipping clip = SpatialUtil.getClipping();
				if (clip != null) {
					clip.setEraserSize(eraserSpinner.getSelection());
				}
			}
		});
		/* Displays the changes of the mouse tools (plane and removed voxels) */
		final Clipping[] observed = new Clipping[1];
		final Runnable syncTools = () -> {
			Clipping clip = observed[0];
			if (clip == null || planeButton.isDisposed()) {
				return;
			}
			double[] plane = clip.getPlane();
			int tool = clip.getTool();
			toolCombo.select(tool == Clipping.TOOL_PLANE ? 1 : tool == Clipping.TOOL_ERASER ? 2 : 0);
			planeButton.setSelection(clip.isPlaneEnabled());
			azimuthScale.setSelection((int) Math.round(plane[0]));
			elevationScale.setSelection((int) Math.round(plane[1] + 90));
			offsetScale.setSelection((int) Math.round(plane[2] * 100 + 100));
			showMask[0].run();
		};
		final Runnable[] toolListener = new Runnable[1];
		toolListener[0] = () -> {
			Display display = compositeClip.isDisposed() ? null : compositeClip.getDisplay();
			if (display == null || display.isDisposed()) {
				Clipping clip = observed[0];
				if (clip != null) {
					clip.removeChangeListener(toolListener[0]);
				}
				return;
			}
			display.asyncExec(syncTools);
		};
		toolCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Clipping clip = SpatialUtil.getClipping();
				if (clip == null) {
					return;
				}
				if (observed[0] != clip) {
					if (observed[0] != null) {
						observed[0].removeChangeListener(toolListener[0]);
					}
					observed[0] = clip;
					clip.addChangeListener(toolListener[0]);
				}
				clip.setEraserSize(eraserSpinner.getSelection());
				int index = toolCombo.getSelectionIndex();
				clip.setTool(index == 1 ? Clipping.TOOL_PLANE : index == 2 ? Clipping.TOOL_ERASER : Clipping.TOOL_NONE);
				if (index == 1) {
					infoLabel.setText("Drag the plane with the left mouse button (Shift: rotate the plane).");
				} else if (index == 2) {
					infoLabel.setText("Drag to remove the voxels along the view direction (Shift: restore).");
				}
			}
		});

		final Button resetButton = new Button(compositeClip, SWT.PUSH);
		GridData gd_resetButton = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		gd_resetButton.heightHint = 40;
		resetButton.setLayoutData(gd_resetButton);
		resetButton.setText("Reset");
		resetButton.setToolTipText("Removes the clipping and the cuts");
		resetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Clipping clip = SpatialUtil.getClipping();
				if (clip != null) {
					clip.reset();
				}
				boxButton.setSelection(false);
				planeButton.setSelection(false);
				flipButton.setSelection(false);
				sliceButton.setSelection(false);
				cutMeshButton.setSelection(false);
				toolCombo.select(0);
				showMask[0].run();
				for (int i = 0; i < 6; i++) {
					boxScales[i].setSelection(i % 2 == 1 ? 1000 : 0);
				}
				azimuthScale.setSelection(0);
				elevationScale.setSelection(90);
				offsetScale.setSelection(100);
			}
		});
	}

	private void createMeasureTab(CTabFolder tabFolder) {
		final CTabItem measureItem = new CTabItem(tabFolder, SWT.NONE);
		measureItem.setText("Measure");
		ScrolledComposite scrollMeasure = new ScrolledComposite(tabFolder, SWT.V_SCROLL);
		final Composite compositeMeasure = new Composite(scrollMeasure, SWT.NONE);
		measureItem.setControl(scrollMeasure);
		compositeMeasure.setLayout(new GridLayout(2, false));
		scrollMeasure.setMinSize(200, 540);
		scrollMeasure.setContent(compositeMeasure);
		scrollMeasure.setExpandVertical(true);
		scrollMeasure.setExpandHorizontal(true);
		scrollMeasure.setAlwaysShowScrollBars(true);

		final Label infoLabel = new Label(compositeMeasure, SWT.WRAP);
		GridData gd_infoLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_infoLabel.heightHint = 80;
		infoLabel.setLayoutData(gd_infoLabel);
		infoLabel.setText("Measures the iso surface and the volume (results in the Bio7 table).");

		Label minLabel = new Label(compositeMeasure, SWT.NONE);
		minLabel.setText("Min. Object Size (Voxels):");
		final Spinner minSpinner = new Spinner(compositeMeasure, SWT.BORDER);
		minSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		minSpinner.setMinimum(0);
		minSpinner.setMaximum(100000000);
		minSpinner.setSelection(10);
		minSpinner.setToolTipText("Smaller objects of the iso surface are not measured (e.g., noise)");

		Label maxLabel = new Label(compositeMeasure, SWT.NONE);
		maxLabel.setText("Max. Object Size (Voxels, 0 = All):");
		final Spinner maxSpinner = new Spinner(compositeMeasure, SWT.BORDER);
		maxSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		maxSpinner.setMinimum(0);
		maxSpinner.setMaximum(Integer.MAX_VALUE);
		maxSpinner.setSelection(0);
		maxSpinner.setToolTipText("Larger objects of the iso surface are not measured (0 measures all objects)");

		final Button objectsButton = new Button(compositeMeasure, SWT.PUSH);
		GridData gd_objectsButton = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_objectsButton.heightHint = 35;
		objectsButton.setLayoutData(gd_objectsButton);
		objectsButton.setText("Measure Iso Surface Objects");
		objectsButton.setToolTipText(
				"Volume, surface area, sphericity, centroid, ellipsoid axes, Feret diameter and holes of the objects of the iso surface");
		objectsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Measurement3d measurement = SpatialUtil.getMeasurement();
				if (measurement == null) {
					return;
				}
				measurement.setMinObjectSize(minSpinner.getSelection());
				measurement.setMaxObjectSize(maxSpinner.getSelection());
				infoLabel.setText("Measuring at full resolution...");
				Display display = objectsButton.getDisplay();
				/* Large stacks: in the background */
				runInBackground(() -> {
					double[][] result = measurement.measureIsoSurface(true);
					display.asyncExec(() -> {
						if (!infoLabel.isDisposed()) {
							infoLabel.setText(result == null ? "No iso surface available!"
									: "Objects: " + result.length + reductionNote());
						}
					});
				}, "Bio7 Measure Objects");
			}
		});

		final Button distanceButton = new Button(compositeMeasure, SWT.PUSH);
		GridData gd_distanceButton = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_distanceButton.heightHint = 35;
		distanceButton.setLayoutData(gd_distanceButton);
		distanceButton.setText("Measure Distances (Nearest Neighbors)");
		distanceButton.setToolTipText(
				"Nearest neighbors of the objects (centroids and surfaces edge to edge), the distance matrices and the Clark-Evans index");
		distanceButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Measurement3d measurement = SpatialUtil.getMeasurement();
				if (measurement == null) {
					return;
				}
				measurement.setMinObjectSize(minSpinner.getSelection());
				measurement.setMaxObjectSize(maxSpinner.getSelection());
				infoLabel.setText("Measuring the distances...");
				Display display = distanceButton.getDisplay();
				/* Many objects: in the background */
				Thread thread = new Thread(() -> {
					double[][][] result = measurement.measureDistances(true);
					display.asyncExec(() -> {
						if (!infoLabel.isDisposed()) {
							infoLabel.setText(result == null ? "No iso surface available!"
									: "Distances of " + result[0].length + " objects" + reductionNote());
						}
					});
				}, "Bio7 Measure Distances");
				thread.setDaemon(true);
				thread.start();
			}
		});

		final Button highlightButton = new Button(compositeMeasure, SWT.CHECK);
		highlightButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		highlightButton.setText("Highlight Objects (Colors and Numbers)");
		highlightButton.setSelection(true);
		highlightButton.setToolTipText(
				"Colors the measured objects and displays their numbers (the colors and numbers of the table)");
		highlightButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Measurement3d measurement = SpatialUtil.getMeasurement();
				if (measurement != null) {
					measurement.setHighlightObjects(highlightButton.getSelection());
				}
			}
		});

		final Button regionButton = new Button(compositeMeasure, SWT.PUSH);
		GridData gd_regionButton = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_regionButton.heightHint = 35;
		regionButton.setLayoutData(gd_regionButton);
		regionButton.setText("Measure Region (Clipping)");
		regionButton.setToolTipText(
				"Statistics of the voxels in the region of the clipping (box, plane and cuts) and the volume above the iso value");
		regionButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Measurement3d measurement = SpatialUtil.getMeasurement();
				if (measurement == null) {
					return;
				}
				infoLabel.setText("Measuring...");
				Display display = regionButton.getDisplay();
				/* Large stacks: in the background */
				Thread thread = new Thread(() -> {
					double[] result = measurement.measureRegion(true);
					display.asyncExec(() -> {
						if (!infoLabel.isDisposed()) {
							infoLabel.setText(result == null ? "No stack available!"
									: "Region: " + (long) result[0] + " voxels");
						}
					});
				}, "Bio7 Measure Region");
				thread.setDaemon(true);
				thread.start();
			}
		});

		final Button pickButton = new Button(compositeMeasure, SWT.CHECK);
		pickButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		pickButton.setText("Pick Points with the Mouse (Ruler)");
		pickButton.setToolTipText(
				"A click on the iso surface or the volume adds a point (position, value and distance) to the table");
		final Clipping[] observed = new Clipping[1];
		final Runnable[] toolListener = new Runnable[1];
		toolListener[0] = () -> {
			if (pickButton.isDisposed()) {
				if (observed[0] != null) {
					observed[0].removeChangeListener(toolListener[0]);
				}
				return;
			}
			pickButton.getDisplay().asyncExec(() -> {
				if (!pickButton.isDisposed() && observed[0] != null) {
					pickButton.setSelection(observed[0].getTool() == Clipping.TOOL_PICK);
				}
			});
		};
		pickButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Clipping clip = SpatialUtil.getClipping();
				if (clip == null) {
					return;
				}
				if (observed[0] != clip) {
					observed[0] = clip;
					clip.addChangeListener(toolListener[0]);
				}
				clip.setTool(pickButton.getSelection() ? Clipping.TOOL_PICK : Clipping.TOOL_NONE);
				if (pickButton.getSelection()) {
					infoLabel.setText("Click on the iso surface or the volume (the left mouse button).");
				}
			}
		});
		final Button clearButton = new Button(compositeMeasure, SWT.PUSH);
		clearButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		clearButton.setText("Clear Points");
		clearButton.setToolTipText("Removes the picked points (the next point starts a new sheet)");
		clearButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Measurement3d measurement = SpatialUtil.getMeasurement();
				if (measurement != null) {
					measurement.clearPoints();
				}
			}
		});

		final Button clearAllButton = new Button(compositeMeasure, SWT.PUSH);
		GridData gd_clearAllButton = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_clearAllButton.heightHint = 35;
		clearAllButton.setLayoutData(gd_clearAllButton);
		clearAllButton.setText("Clear Measurements");
		clearAllButton.setToolTipText(
				"Clears the measurements in the 3D view: the object colors, numbers and selection and the picked points");
		clearAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Measurement3d measurement = SpatialUtil.getMeasurement();
				if (measurement != null) {
					measurement.clear();
					infoLabel.setText("The measurements in the 3D view have been cleared.");
				}
			}
		});
	}

	/* A note if the display is reduced (the measurements use the full resolution) */
	private static String reductionNote() {
		IsoSurface iso = isoSurface();
		String reduction = iso != null ? iso.getReduction() : null;
		return reduction == null ? ""
				: "\nMeasured at full resolution. The display is reduced (" + reduction
						+ "), the colors of small objects can be inexact.";
	}

	private static void runInBackground(Runnable runnable, String name) {
		Thread thread = new Thread(runnable, name);
		thread.setDaemon(true);
		thread.start();
	}

	private static Scale createClipScale(Composite parent, String text, int min, int max, int selection,
			String toolTip) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(text);
		Scale scale = new Scale(parent, SWT.NONE);
		scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		scale.setMinimum(min);
		scale.setMaximum(max);
		scale.setSelection(selection);
		scale.setToolTipText(toolTip);
		return scale;
	}

	private static VolumeRenderer volumeRenderer() {
		SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
		return grid != null ? grid.getVolumeRenderer() : null;
	}

	private static Label createVolumeScaleLabel(Composite parent, String text) {
		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		label.setText(text);
		return label;
	}

	private static Scale createVolumeScale(Composite parent, int min, int max, int selection, String toolTip) {
		Scale scale = new Scale(parent, SWT.NONE);
		scale.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		scale.setMinimum(min);
		scale.setMaximum(max);
		scale.setSelection(selection);
		scale.setToolTipText(toolTip);
		return scale;
	}
}
