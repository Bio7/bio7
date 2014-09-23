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


package com.eco.bio7.spatial;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.spatial.preferences.Bio7PrefConverterSpatial;
import com.eco.bio7.spatial.preferences.Point3d;

public class Options3d extends ViewPart {
	public Options3d() {
	}

	private ExpandItem newItemExpandItem;
	private Text worldExtent;
	private Combo combo;
	private Text text_11;
	private Text lookAtZ;
	private Text lookAtY;
	private Text lookAtX;
	private Text splitCamZ;
	private Text splitCamY;
	private Text splitCamX;
	private Text modelScale;
	private Text modelRotZ;
	private Text modelRotY;
	private Text modelRotX;
	private Text modelPosZ;
	private Text modelPosY;
	private Text modelPosX;
	private Text text_2;
	private static Text text;
	private IPreferenceStore store;
	private static String objectPath;
	private static String objectName;
	public static final String ID = "com.eco.bio7.discrete3d.Options3d"; //$NON-NLS-1$
	private Label sizeXLabel;
	private Label sizeYLabel;
	private Label sizeZLabel;
	private Scale scale_2;
	private Scale scale_1;
	private Scale scale;
	private Label errorLabel;
	private Label errorLabelSplit;
	private Button fromImagejButton_2;
	private DisplayMode[] d;
	private Button showGridButton;
	private Button showAxesButton;
	private Button octantButton;
	private Button showQuadButton;
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
	private static Integer stackSizeCount;
	private int fixedFps;

	/**
	 * Create contents of the view part
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
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

		ExpandBar expandBar;
		expandBar = new ExpandBar(container, SWT.V_SCROLL);
		expandBar.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

		newItemExpandItem = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem.setHeight(600);
		newItemExpandItem.setText("Space");

		final Composite composite = new Composite(expandBar, SWT.BORDER);
		newItemExpandItem.setControl(composite);
		sizeXLabel = new Label(composite, SWT.NONE);
		sizeXLabel.setBounds(92, 36, 139, 23);

		sizeYLabel = new Label(composite, SWT.NONE);
		sizeYLabel.setBounds(92, 107, 139, 20);

		sizeZLabel = new Label(composite, SWT.NONE);
		sizeZLabel.setBounds(92, 173, 139, 20);

		scale = new Scale(composite, SWT.NONE);

		scale.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure sg = SpatialStructure.getSpatialStructureInstance();
				if (sg != null) {
					sg.setSizeX(scale.getSelection());
				}

				store.setValue("spatialX", scale.getSelection());
				sizeXLabel.setText("Size X: " + scale.getSelection() * 2);
				scale.setToolTipText("-"+scale.getSelection()+" to +"+scale.getSelection());
			}
		});
		scale.setBounds(5, 55, 192, 42);

		scale.setMaximum((int) halfExtent);

		scale.setSelection(1000);
		sizeXLabel.setText("Size X: " + scale.getSelection() * 2);
		scale_1 = new Scale(composite, SWT.NONE);

		scale_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure sg = SpatialStructure.getSpatialStructureInstance();
				if (sg != null) {
					sg.setSizeY(scale_1.getSelection());
				}

				store.setValue("spatialY", scale_1.getSelection());
				sizeYLabel.setText("Size Y: " + scale_1.getSelection() * 2);
				scale_1.setToolTipText("-"+scale_1.getSelection()+" to +"+scale_1.getSelection());
			}
		});
		scale_1.setBounds(5, 122, 192, 42);

		scale_1.setMaximum((int) halfExtent);
		scale_1.setSelection(1000);
		sizeYLabel.setText("Size Y: " + scale_1.getSelection() * 2);
		scale_2 = new Scale(composite, SWT.NONE);

		scale_2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure sg = SpatialStructure.getSpatialStructureInstance();
				if (sg != null) {
					sg.setSizeZ(scale_2.getSelection());
				}

				store.setValue("spatialZ", scale_2.getSelection());
				sizeZLabel.setText("Size Z: " + scale_2.getSelection() * 2);
				scale_2.setToolTipText("-"+scale_2.getSelection()+" to +"+scale_2.getSelection());
			}
		});
		scale_2.setBounds(5, 189, 192, 42);
		scale_2.setMaximum((int) halfExtent);
		scale_2.setSelection(1000);
		sizeZLabel.setText("Size Z: " + scale_2.getSelection() * 2);

		octantButton = new Button(composite, SWT.CHECK);
		octantButton.setToolTipText("Changes the visualization to the first octant.");
		octantButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (SpatialStructure.hMap.isCartesian()) {
					SpatialStructure.hMap.setCartesian(false);
				} else {
					SpatialStructure.hMap.setCartesian(true);
				}
			}
		});
		octantButton.setText("Octant");
		octantButton.setBounds(10, 254, 67, 16);

		showAxesButton = new Button(composite, SWT.CHECK);
		showAxesButton.setSelection(true);
		showAxesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure sg = SpatialStructure.getSpatialStructureInstance();
				if (sg != null) {
					if (showAxesButton.getSelection()) {
						sg.showAxes = true;
					} else {
						sg.showAxes = false;
					}
				}

			}
		});
		showAxesButton.setText("Show Axes");
		showAxesButton.setBounds(92, 254, 94, 16);

		showQuadButton = new Button(composite, SWT.CHECK);
		showQuadButton.setSelection(true);
		showQuadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure sg = SpatialStructure.getSpatialStructureInstance();
				if (sg != null) {
					if (showQuadButton.getSelection()) {
						sg.showQuad = true;
					} else {
						sg.showQuad = false;
					}
				}

			}
		});
		showQuadButton.setText("Show Quad");
		showQuadButton.setBounds(192, 254, 101, 16);

		showGridButton = new Button(composite, SWT.CHECK);
		showGridButton.setSelection(true);
		showGridButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (showGridButton.getSelection()) {
						grid.showGrid = true;
					} else {
						grid.showGrid = false;
					}
				}
			}
		});
		showGridButton.setText("Show Grid");
		showGridButton.setBounds(10, 295, 101, 24);

		final Spinner spinner_12 = new Spinner(composite, SWT.BORDER);
		spinner_12.setSelection(50);
		spinner_12.setMaximum(1000);
		spinner_12.setMinimum(1);
		spinner_12.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.gridSize = spinner_12.getSelection();
				}
			}
		});
		spinner_12.setBounds(120, 297, 47, 23);

		final Label girdSizeLabel = new Label(composite, SWT.NONE);
		girdSizeLabel.setText("Size");
		girdSizeLabel.setBounds(173, 300, 45, 18);

		final Button xyButton = new Button(composite, SWT.CHECK);
		xyButton.setSelection(true);
		xyButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (xyButton.getSelection()) {
						grid.layerXYGrid = true;
					} else {
						grid.layerXYGrid = false;

					}
				}
			}
		});
		xyButton.setText("XY");
		xyButton.setBounds(10, 325, 67, 34);

		final Button xzButton = new Button(composite, SWT.CHECK);
		xzButton.setText("XZ");
		xzButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (xzButton.getSelection()) {
						grid.layerXZGrid = true;
					} else {
						grid.layerXZGrid = false;

					}
				}
			}
		});
		xzButton.setBounds(83, 325, 85, 34);

		final Button yzButton = new Button(composite, SWT.CHECK);
		yzButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (yzButton.getSelection()) {
						grid.layerYZGrid = true;
					} else {
						grid.layerYZGrid = false;

					}
				}
			}

		});
		yzButton.setText("YZ");
		yzButton.setBounds(173, 324, 85, 35);

		final Label label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setBounds(5, 276, 263, 13);

		final Label label_2 = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		label_2.setBounds(5, 365, 263, 13);
		label_2.setText("Label");

		final Button colourButton = new Button(composite, SWT.NONE);
		colourButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				RGB color = getColour();
				if (color != null) {
					SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
					if (grid != null) {
						float r = (float) (color.red) / 255;
						float g = (float) (color.green) / 255;
						float b = (float) (color.blue) / 255;

						grid.setColorBackground(new float[] { r, g, b, 1.0f });
						grid.backgroundColour = true;
					}
				}

			}
		});
		colourButton.setText("Colour Space");
		colourButton.setBounds(5, 384, 106, 28);

		final Button colourLinesButton = new Button(composite, SWT.NONE);
		colourLinesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				RGB color = getColour();
				if (color != null) {
					SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
					if (grid != null) {
						float r = (float) (color.red) / 255;
						float g = (float) (color.green) / 255;
						float b = (float) (color.blue) / 255;

						grid.getScene().setColorCubeLines(new float[] { r, g, b, 1.0f });
					}
				}

			}
		});
		colourLinesButton.setText("Colour Lines");
		colourLinesButton.setBounds(120, 384, 106, 28);

		final Label label_3 = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		label_3.setBounds(5, 235, 263, 13);
		label_3.setText("Label");

		final Label quadSizeLabel = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		quadSizeLabel.setBounds(5, 21, 263, 13);
		quadSizeLabel.setText("Quad size");

		combo = new Combo(composite, SWT.NONE);
		combo.setToolTipText("Resolution x, Resolution y, Colour depth, Refresh rate");
		GraphicsDevice graphicDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		DisplayMode defaultMode = graphicDevice.getDisplayMode();
		SpatialUtil.setFullscreenOptions(defaultMode.getWidth(), defaultMode.getHeight(), defaultMode.getBitDepth(), defaultMode.getRefreshRate());
		d = graphicDevice.getDisplayModes();
		for (int i = 0; i < d.length; i++) {

			combo.add(d[i].getWidth() + " " + d[i].getHeight() + " " + d[i].getBitDepth() + " " + d[i].getRefreshRate());
			if (d[i].getWidth() == defaultMode.getWidth() && d[i].getHeight() == defaultMode.getHeight() && d[i].getBitDepth() == defaultMode.getBitDepth()
					&& d[i].getRefreshRate() == defaultMode.getRefreshRate()) {
				combo.select(i);
			}
		}
		combo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int i = combo.getSelectionIndex();
				SpatialUtil.setFullscreenOptions(d[i].getWidth(), d[i].getHeight(), d[i].getBitDepth(), d[i].getRefreshRate());

			}
		});
		combo.setBounds(5, 456, 164, 23);

		final Label fullscreenLabel = new Label(composite, SWT.NONE);
		fullscreenLabel.setText("Fullscreen Settings");
		fullscreenLabel.setBounds(5, 435, 134, 21);

		final Label label_2_1 = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		label_2_1.setBounds(5, 422, 263, 13);
		label_2_1.setText("Label");
		worldExtentError = new Label(composite, SWT.NONE);
		worldExtentError.setBounds(183, 500, 180, 25);

		worldExtent = new Text(composite, SWT.BORDER);
		worldExtent.setToolTipText("Set the world extent");
		worldExtent.setText("100000.0");

		if (we != 0) {
			worldExtent.setText("" + we);
		}
		worldExtent.setBounds(183, 462, 67, 23);
		worldExtent.addListener(SWT.Modify, new Listener() {

			public void handleEvent(Event event) {

				try {
					double extent = new Double(worldExtent.getText());
					store.setValue("spatialExtent", extent);
					worldExtent.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
					worldExtentError.setText("");
				} catch (Exception e) {
					worldExtent.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
					worldExtentError.setText("Number input incorrect!");
				}
			}

		});

		final Label worldExtentLabel = new Label(composite, SWT.NONE);
		worldExtentLabel.setText("World Extent");
		worldExtentLabel.setBounds(183, 435, 110, 21);
		
		multiplyDragCameraSpeedSpinner = new Spinner(composite, SWT.BORDER);
		multiplyDragCameraSpeedSpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SpatialStructure.setMultiplyDragCameraSpeed(multiplyDragCameraSpeedSpinner.getSelection());
			}
		});
		multiplyDragCameraSpeedSpinner.setMaximum(10000);
		multiplyDragCameraSpeedSpinner.setMinimum(1);
		multiplyDragCameraSpeedSpinner.setSelection(1);
		multiplyDragCameraSpeedSpinner.setBounds(5, 522, 163, 23);
		
		Label lblSpeedDistance = new Label(composite, SWT.NONE);
		lblSpeedDistance.setBounds(5, 494, 134, 19);
		lblSpeedDistance.setText("Speed Drag Distance");
		
		Button btnResetView = new Button(composite, SWT.NONE);
		btnResetView.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SpatialStructure struc=SpatialStructure.getSpatialStructureInstance();
				if(struc!=null){
					struc. setDoz(2000);
				}
				
			}
		});
		btnResetView.setBounds(5, 551, 163, 28);
		btnResetView.setText("Reset View Distance");

		final ExpandItem newItemExpandItem_1 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_1.setHeight(250);
		newItemExpandItem_1.setText("Time");

		final Composite composite_2 = new Composite(expandBar, SWT.BORDER);
		newItemExpandItem_1.setControl(composite_2);

		final Scale scale_7 = new Scale(composite_2, SWT.NONE);
		scale_7.setToolTipText("The speed of the canStep() function can be adjusted with this slider");

		scale_7.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure.setPeriod(scale_7.getSelection() * 1000000);
			}
		});
		scale_7.setMaximum(1000);
		scale_7.setBounds(3, 91, 235, 42);
		scale_7.setSelection(1000);

		final Label framerateLabel = new Label(composite_2, SWT.NONE);
		framerateLabel.setText("Step rate");
		framerateLabel.setBounds(13, 62, 83, 23);

		final Button haltButton = new Button(composite_2, SWT.CHECK);
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
		haltButton.setBounds(13, 139, 98, 23);

		final Button showFramerateButton = new Button(composite_2, SWT.CHECK);
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
		showFramerateButton.setBounds(13, 214, 122, 21);

		final Button showUfpsButton = new Button(composite_2, SWT.CHECK);
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
		showUfpsButton.setBounds(13, 185, 132, 23);

		final Button setupButton = new Button(composite_2, SWT.NONE);
		setupButton.setToolTipText("Triggers the setup method of the compiled java code");
		setupButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.setup = true;
				}

			}
		});
		setupButton.setText("Setup");
		setupButton.setBounds(88, 10, 68, 32);

		playPauseButton = new Button(composite_2, SWT.TOGGLE);
		playPauseButton.setToolTipText("Enables the triggering of the run method with the default Jogl timer.\n The timer fps determines the speed of triggering");
		playPauseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (grid.isFromOpenGl()) {
						grid.setFromOpenGl(false);
					} else {
						grid.setFromOpenGl(true);
					}
				}
			}
		});
		playPauseButton.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/playneu.gif")));// "/pics/r.gif"));
		playPauseButton.setBounds(14, 10, 68, 32);

		final Label label_4 = new Label(composite_2, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_4.setBounds(3, 49, 235, 16);

		final Label label_4_1 = new Label(composite_2, SWT.HORIZONTAL | SWT.SEPARATOR);
		label_4_1.setBounds(3, 168, 235, 13);
		label_4_1.setText("Label");

		fixedFpsButton = new Button(composite_2, SWT.CHECK);
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
		fixedFpsButton.setToolTipText("If not selected:\n Sets a default 60 FPS for the OpenGL\ntimer for unnecessary loops.");
		fixedFpsButton.setText("Fixed fps");
		fixedFpsButton.setBounds(151, 185, 93, 23);
		
		setFixedFPS = new Spinner(composite_2, SWT.BORDER);
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
		setFixedFPS.setBounds(151, 214, 87, 21);

		final ExpandItem newItemExpandItem_2 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_2.setHeight(320);
		newItemExpandItem_2.setText("Model");

		final Composite composite_1 = new Composite(expandBar, SWT.BORDER);
		newItemExpandItem_2.setControl(composite_1);

		text = new Text(composite_1, SWT.BORDER);
		text.setBounds(10, 10, 173, 25);
		text.setText(store.getString("objectFile"));
		final Button pathButton = new Button(composite_1, SWT.NONE);
		pathButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Shell s = new Shell(SWT.ON_TOP);
				FileDialog fd = new FileDialog(s, SWT.OPEN);
				fd.setText("Load");

				String[] filterExt = { "*.obj", "*.*" };
				fd.setFilterExtensions(filterExt);
				String path = fd.open();
				objectPath = fd.getFilterPath() + "\\";
				objectName = fd.getFileName();
                if(path!=null){
				text.setText(path);
				store.setValue("objectFile", path);
                }
			}
		});
		pathButton.setText("Set Path");
		pathButton.setBounds(204, 10, 80, 28);

		final Button activateButton = new Button(composite_1, SWT.CHECK);
		activateButton.setToolTipText("Activates the model");
		activateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (activateButton.getSelection()) {
						grid.setLoadModel(true);
					} else {
						grid.setLoadModel(false);
					}
				}
			}
		});
		activateButton.setText("Activate");
		activateButton.setBounds(10, 56, 90, 22);

		final Button reloadButton = new Button(composite_1, SWT.NONE);
		reloadButton.setToolTipText("Load or reload the model");
		reloadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure.setReloadModel(true);
			}
		});
		reloadButton.setText("Reload");
		reloadButton.setBounds(204, 55, 80, 28);

		errorLabel = new Label(composite_1, SWT.NONE);
		errorLabel.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
		errorLabel.setBounds(10, 105, 259, 15);

		modelPosX = new Text(composite_1, SWT.BORDER);
		modelPosX.setText("" + p.x);
		modelPosX.setBounds(10, 122, 80, 25);
		modelPosX.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					try {
						value = new Double(modelPosX.getText());
						modelPosX.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
						grid.objModel1Position[0] = value;
						errorLabel.setText("");
					} catch (Exception e) {
						modelPosX.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
						errorLabel.setText("Number input incorrect!");
					}
				}
			}
		});

		modelPosY = new Text(composite_1, SWT.BORDER);
		modelPosY.setText("" + p.y);
		modelPosY.setBounds(96, 122, 80, 25);
		modelPosY.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					try {
						value = new Double(modelPosY.getText());
						modelPosY.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
						grid.objModel1Position[1] = value;
						errorLabel.setText("");
					} catch (Exception e) {
						modelPosY.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
						errorLabel.setText("Number input incorrect!");
					}
				}
			}
		});

		modelPosZ = new Text(composite_1, SWT.BORDER);
		modelPosZ.setText("" + p.z);
		modelPosZ.setBounds(179, 122, 80, 25);
		modelPosZ.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					try {
						value = new Double(modelPosZ.getText());
						modelPosZ.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
						grid.objModel1Position[2] = value;
						errorLabel.setText("");
					} catch (Exception e) {
						modelPosZ.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
						errorLabel.setText("Number input incorrect!");
					}
				}
			}
		});

		final Label positionLabel = new Label(composite_1, SWT.NONE);
		positionLabel.setText("Position: x");
		positionLabel.setBounds(10, 153, 80, 20);

		final Label positionYLabel = new Label(composite_1, SWT.NONE);
		positionYLabel.setText("Position: y");
		positionYLabel.setBounds(96, 153, 80, 20);

		final Label positionZLabel = new Label(composite_1, SWT.NONE);
		positionZLabel.setText("Position: z");
		positionZLabel.setBounds(179, 153, 90, 20);

		final Label label_6 = new Label(composite_1, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_6.setBounds(10, 84, 249, 15);

		modelRotX = new Text(composite_1, SWT.BORDER);
		modelRotX.setBounds(10, 174, 80, 25);
		modelRotX.setText("" + p2.x);
		modelRotX.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					try {
						value = new Double(modelRotX.getText());
						modelRotX.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
						grid.objModel1Rotation[0] = value;
						errorLabel.setText("");
					} catch (Exception e) {
						modelRotX.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
						errorLabel.setText("Number input incorrect!");
					}
				}
			}
		});

		modelRotY = new Text(composite_1, SWT.BORDER);
		modelRotY.setBounds(96, 174, 80, 25);
		modelRotY.setText("" + p2.y);
		modelRotY.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					try {
						value = new Double(modelRotY.getText());
						modelRotY.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
						grid.objModel1Rotation[1] = value;
						errorLabel.setText("");
					} catch (Exception e) {
						modelRotY.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
						errorLabel.setText("Number input incorrect!");
					}
				}
			}
		});

		modelRotZ = new Text(composite_1, SWT.BORDER);
		modelRotZ.setBounds(179, 174, 80, 25);
		modelRotZ.setText("" + p2.z);
		modelRotZ.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					try {
						value = new Double(modelRotZ.getText());
						modelRotZ.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
						grid.objModel1Rotation[2] = value;
						errorLabel.setText("");
					} catch (Exception e) {
						modelRotZ.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
						errorLabel.setText("Number input incorrect!");
					}
				}
			}
		});

		final Label positionLabel_1 = new Label(composite_1, SWT.NONE);
		positionLabel_1.setBounds(10, 205, 80, 20);
		positionLabel_1.setText("Rotation: x");

		final Label positionYLabel_1 = new Label(composite_1, SWT.NONE);
		positionYLabel_1.setBounds(96, 205, 80, 20);
		positionYLabel_1.setText("Rotation: y");

		final Label positionZLabel_1 = new Label(composite_1, SWT.NONE);
		positionZLabel_1.setBounds(179, 205, 80, 20);
		positionZLabel_1.setText("Rotation: z");

		modelScale = new Text(composite_1, SWT.BORDER);
		modelScale.setText("" + p3.x);
		modelScale.setBounds(10, 226, 80, 25);
		modelScale.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					try {
						value = new Double(modelScale.getText());
						modelScale.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
						grid.objModel1Scale = value;
						errorLabel.setText("");
					} catch (Exception e) {
						modelScale.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
						errorLabel.setText("Number input incorrect!");
					}
				}
			}
		});

		final Label scaleLabel = new Label(composite_1, SWT.NONE);
		scaleLabel.setText("Scale");
		scaleLabel.setBounds(10, 257, 74, 15);

		final Button lightenedButton_1 = new Button(composite_1, SWT.CHECK);
		lightenedButton_1.setToolTipText("Enables the lightening of  the model with the activated lamps");
		lightenedButton_1.setSelection(true);
		lightenedButton_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (lightenedButton_1.getSelection()) {
						grid.lightenedObjModel = true;
					} else {
						grid.lightenedObjModel = false;
					}
				}

			}
		});
		lightenedButton_1.setText("Lighted");
		lightenedButton_1.setBounds(121, 56, 77, 22);

		final Button storeButton_1 = new Button(composite_1, SWT.NONE);
		storeButton_1.setToolTipText("Store the position, rotation and scale (only as integer!)");
		storeButton_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.getPref3d().storeModelLocation(new Double(modelPosX.getText()), new Double(modelPosY.getText()), new Double(modelPosZ.getText()));
					grid.getPref3d().storeModelRotation(new Double(modelRotX.getText()), new Double(modelRotY.getText()), new Double(modelRotZ.getText()));
					grid.getPref3d().storeModelScale(new Double(modelScale.getText()), 0, 0);
				}

			}
		});
		storeButton_1.setText("Store");
		storeButton_1.setBounds(96, 270, 80, 28);

		final ExpandItem newItemExpandItem_3 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_3.setHeight(280);
		newItemExpandItem_3.setText("Texture");

		final Composite composite_5 = new Composite(expandBar, SWT.BORDER);
		newItemExpandItem_3.setControl(composite_5);

		final Button pathButton_2 = new Button(composite_5, SWT.NONE);
		pathButton_2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Shell s = new Shell(SWT.ON_TOP);
				FileDialog fd = new FileDialog(s, SWT.OPEN);
				fd.setText("Load");

				String[] filterExt = { "*.*" };
				fd.setFilterExtensions(filterExt);
				String path = fd.open();

				text_2.setText(path);
				store.setValue("spatialImage", path);

			}
		});
		pathButton_2.setText("Path");
		pathButton_2.setBounds(173, 102, 68, 28);

		text_2 = new Text(composite_5, SWT.BORDER);
		text_2.setBounds(10, 105, 157, 25);
		text_2.setText(store.getString("spatialImage"));
		final Button fromImagejButton_1 = new Button(composite_5, SWT.RADIO);
		fromImagejButton_1.setToolTipText("This option creates a dynamic texture from the current ImageJ image");
		fromImagejButton_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

			}
		});
		fromImagejButton_1.setText("From ImageJ");
		fromImagejButton_1.setBounds(134, 182, 118, 25);

		final Button fromFileButton_1 = new Button(composite_5, SWT.RADIO);
		fromFileButton_1.setToolTipText("From file will create a non dynamic texture");
		fromFileButton_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

			}
		});
		fromFileButton_1.setText("From File");
		fromFileButton_1.setBounds(10, 71, 107, 25);

		final Button fromQuad2dButton = new Button(composite_5, SWT.RADIO);
		fromQuad2dButton.setToolTipText("This options creates a dynamic texture from the Quads view");
		fromQuad2dButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

			}
		});
		fromQuad2dButton.setText("From Quad2d");
		fromQuad2dButton.setBounds(10, 182, 118, 25);

		final Button enableTextureButton = new Button(composite_5, SWT.CHECK);
		enableTextureButton.setToolTipText("Enable the texture will draw a textured plane");
		enableTextureButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (enableTextureButton.getSelection()) {
						grid.textures.setShowTexture(true);

					} else {
						grid.textures.setShowTexture(false);
					}
				}
			}
		});
		enableTextureButton.setText("Enable Texture");
		enableTextureButton.setBounds(10, 10, 157, 16);

		final Label label_5 = new Label(composite_5, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_5.setBounds(10, 32, 235, 16);

		final Label label_5_1 = new Label(composite_5, SWT.HORIZONTAL | SWT.SEPARATOR);
		label_5_1.setBounds(10, 138, 235, 16);
		label_5_1.setText("Label");

		final Label dynamicLabel = new Label(composite_5, SWT.NONE);
		dynamicLabel.setText("Dynamic Texture:");
		dynamicLabel.setBounds(10, 160, 120, 16);

		final Button setButton = new Button(composite_5, SWT.NONE);
		setButton.setToolTipText("Changes the texture selection");
		setButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (fromImagejButton_1.getSelection()) {
						/* Creating a new texture! */
						grid.textures.createNewTexture = true;

						grid.textures.setRenderFromFile(false);
						/* Dynamic texture from ImageJ! */
						DynamicTexture.setImagejEnabled(true);

					} else if (fromQuad2dButton.getSelection()) {
						grid.textures.createNewTexture = true;

						grid.textures.setRenderFromFile(false);
						/* Dynamic texture from Quadgrid! */
						DynamicTexture.setImagejEnabled(false);

					}

					else {

						grid.textures.createNewTexture = true;

						grid.textures.setRenderFromFile(true);
						/* Texture from file! */
						if (fromFileButton_1.getSelection()) {
							grid.textures.setStaticImageImageJ(false);
						} else if (fromImagejButton_2.getSelection()) {
							grid.textures.setStaticImageImageJ(true);
						}
						grid.textures.createImageTexture(grid.getGl().getGL2());

					}
				}
			}
		});
		setButton.setText("Change");
		setButton.setBounds(10, 232, 120, 28);

		final Label label_5_1_1 = new Label(composite_5, SWT.HORIZONTAL | SWT.SEPARATOR);
		label_5_1_1.setBounds(10, 210, 235, 16);
		label_5_1_1.setText("Label");

		fromImagejButton_2 = new Button(composite_5, SWT.RADIO);
		fromImagejButton_2.setToolTipText("From ImageJ will create a non dynamic texture from an enabled image in ImageJ");
		fromImagejButton_2.setText("From ImageJ");
		fromImagejButton_2.setBounds(123, 71, 120, 25);

		final Label staticTextureLabel = new Label(composite_5, SWT.NONE);
		staticTextureLabel.setText("Static Texture:");
		staticTextureLabel.setBounds(10, 50, 94, 15);

		final ExpandItem newItemExpandItem_4 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_4.setHeight(230);
		newItemExpandItem_4.setText("Height Map");

		final Composite composite_3 = new Composite(expandBar, SWT.BORDER);
		composite_3.setToolTipText("Enables the height map. Visible if a texture is active!");
		newItemExpandItem_4.setControl(composite_3);

		final Scale scale_8 = new Scale(composite_3, SWT.NONE);
		scale_8.setToolTipText("Adjusts the amount of triangles for the height map.");
		scale_8.setSelection(15);
		scale_8.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				HeightMaps height = SpatialStructure.getHeightMapInstance();

				height.setDetail(scale_8.getSelection());
			}
		});
		scale_8.setMinimum(1);
		scale_8.setBounds(0, 78, 242, 42);

		final Button enableHeightMapButton = new Button(composite_3, SWT.CHECK);
		enableHeightMapButton.setToolTipText("Enable the height map");
		enableHeightMapButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (enableHeightMapButton.getSelection()) {
						grid.setHeightMap(true);
					} else {
						grid.setHeightMap(false);
					}
				}
			}
		});
		enableHeightMapButton.setText("Enable Height Map");
		enableHeightMapButton.setBounds(8, 0, 156, 28);

		final Scale scaleHeight = new Scale(composite_3, SWT.NONE);
		scaleHeight.setToolTipText("A scaling factor for the height map");
		scaleHeight.setSelection(100);
		scaleHeight.setPageIncrement(1);
		scaleHeight.setMinimum(1);
		scaleHeight.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					int selection = scaleHeight.getSelection();
					float scale = selection;
					float h = 1 / (100 / scale);
					scaleHeight.setToolTipText("scale: " + h);
					grid.hMap.setScale(h);
				}
			}
		});
		scaleHeight.setBounds(0, 155, 232, 42);

		final Label levelOfDetailLabel_1 = new Label(composite_3, SWT.NONE);
		levelOfDetailLabel_1.setText("Level of Detail");
		levelOfDetailLabel_1.setBounds(79, 126, 113, 31);

		final Label factorHeightLabel = new Label(composite_3, SWT.NONE);
		factorHeightLabel.setText("Factor Height");
		factorHeightLabel.setBounds(79, 203, 113, 22);

		final Button staticHeightButton = new Button(composite_3, SWT.CHECK);
		staticHeightButton.setToolTipText("If dynamic is selected the height values are updated directly from the active ImageJ image");
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
		staticHeightButton.setText("Dynamic");
		staticHeightButton.setBounds(8, 55, 99, 22);

		final Button lightenedButton = new Button(composite_3, SWT.CHECK);
		lightenedButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (lightenedButton.getSelection()) {
						grid.lightenedHeightMap = true;
					} else {
						grid.lightenedHeightMap = false;
					}
				}
			}
		});
		lightenedButton.setToolTipText("Enables the lightening for the height map");
		lightenedButton.setSelection(true);
		lightenedButton.setText("Lighted");
		lightenedButton.setBounds(170, 57, 93, 18);

		final Button gcButton = new Button(composite_3, SWT.CHECK);
		gcButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (gcButton.getSelection()) {
					HeightMaps.setGraphicsCardSwitch(true);
				} else {
					HeightMaps.setGraphicsCardSwitch(false);
				}
			}
		});
		gcButton.setToolTipText("This is a special switch for the texture.\n For some graphics cards it seems to be necessary\n to assign the texture differently!");
		gcButton.setText("Gc");
		gcButton.setBounds(170, 5, 93, 18);

		final Button geoButton = new Button(composite_3, SWT.CHECK);
		geoButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (geoButton.getSelection()) {
					HeightMaps.setInverse(true);
				} else {
					HeightMaps.setInverse(false);
				}
			}
		});
		geoButton.setToolTipText("Scales the values negative for a correct \ngeographic view");
		geoButton.setText("Geo");
		geoButton.setBounds(170, 31, 93, 18);

		final ExpandItem newItemExpandItem_5 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_5.setHeight(350);
		newItemExpandItem_5.setText("Camera");

		final Composite composite_4 = new Composite(expandBar, SWT.BORDER);
		newItemExpandItem_5.setControl(composite_4);

		final Button enableWalkthroughButton = new Button(composite_4, SWT.CHECK);
		enableWalkthroughButton.setToolTipText("Enable the walkthrough");
		enableWalkthroughButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (enableWalkthroughButton.getSelection()) {
					SpatialStructure.setWalkthrough(true);
				} else {
					SpatialStructure.setWalkthrough(false);
				}
			}
		});
		enableWalkthroughButton.setText("Enable Walkthrough");
		enableWalkthroughButton.setBounds(10, 23, 150, 27);

		final Button splitViewButton = new Button(composite_4, SWT.CHECK);
		splitViewButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (splitViewButton.getSelection()) {
						grid.setSplitView(true);
					} else {
						grid.setSplitView(false);
					}
				}
			}
		});
		splitViewButton.setText("Split View");
		splitViewButton.setBounds(10, 132, 150, 25);

		final Button enableFlythroughButton = new Button(composite_4, SWT.CHECK);
		enableFlythroughButton.setToolTipText("Enable flythrough");
		enableFlythroughButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (enableFlythroughButton.getSelection()) {
						grid.flythrough = true;
					} else {
						grid.flythrough = false;
					}
				}
			}
		});
		enableFlythroughButton.setText("Enable Flythrough");
		enableFlythroughButton.setBounds(10, 68, 150, 27);

		final Button customCameraButton = new Button(composite_4, SWT.CHECK);
		customCameraButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (customCameraButton.getSelection()) {
						grid.customCamera = true;

					} else {
						grid.customCamera = false;
					}
				}
			}
		});
		customCameraButton.setText("Custom Camera");
		customCameraButton.setBounds(10, 101, 150, 25);

		final Label label_1 = new Label(composite_4, SWT.SEPARATOR | SWT.HORIZONTAL);
		label_1.setBounds(10, 290, 262, 12);

		final Button stereoButton = new Button(composite_4, SWT.CHECK);
		stereoButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (stereoButton.getSelection()) {
						grid.stereo = true;
					} else {
						grid.stereo = false;
					}
				}
			}
		});
		stereoButton.setText("Stereo");
		stereoButton.setBounds(10, 308, 124, 27);

		final Label label_1_1 = new Label(composite_4, SWT.HORIZONTAL | SWT.SEPARATOR);
		label_1_1.setBounds(10, 94, 262, 12);
		label_1_1.setText("Label");

		errorLabelSplit = new Label(composite_4, SWT.NONE);
		errorLabelSplit.setBounds(165, 132, 150, 16);
		errorLabelSplit.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
		Point3d p = Bio7PrefConverterSpatial.getPoint(store, "splitCameraLoc");
		Point3d p2 = Bio7PrefConverterSpatial.getPoint(store, "splitCameraLookAt");

		splitCamX = new Text(composite_4, SWT.BORDER);
		splitCamX.setText("" + p.x);
		splitCamX.setBounds(10, 171, 80, 25);
		splitCamX.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					try {
						value = new Double(splitCamX.getText());
						grid.xCamSplit = value;
						splitCamX.setForeground(new Color(Display.getCurrent(), 0, 0, 0));

						errorLabelSplit.setText("");
					} catch (Exception e) {
						splitCamX.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
						errorLabelSplit.setText("Number input incorrect!");
					}
				}
			}
		});

		splitCamY = new Text(composite_4, SWT.BORDER);
		splitCamY.setText("" + p.y);
		splitCamY.setBounds(96, 171, 80, 25);
		splitCamY.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					try {
						value = new Double(splitCamY.getText());
						grid.yCamSplit = value;
						splitCamY.setForeground(new Color(Display.getCurrent(), 0, 0, 0));

						errorLabelSplit.setText("");
					} catch (Exception e) {
						splitCamY.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
						errorLabelSplit.setText("Number input incorrect!");
					}
				}
			}
		});

		splitCamZ = new Text(composite_4, SWT.BORDER);
		splitCamZ.setText("" + p.z);
		splitCamZ.setBounds(182, 171, 80, 25);
		splitCamZ.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					try {
						value = new Double(splitCamZ.getText());
						grid.zCamSplit = value;
						splitCamZ.setForeground(new Color(Display.getCurrent(), 0, 0, 0));

						errorLabelSplit.setText("");
					} catch (Exception e) {
						splitCamZ.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
						errorLabelSplit.setText("Number input incorrect!");
					}
				}
			}
		});

		final Button storeButton = new Button(composite_4, SWT.NONE);
		storeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.getPref3d().storeSplitViewCameraLocation(new Double(splitCamX.getText()), new Double(splitCamY.getText()), new Double(splitCamZ.getText()));
					grid.getPref3d().storeSplitViewCameraLookAt(new Double(lookAtX.getText()), new Double(lookAtY.getText()), new Double(lookAtZ.getText()));
				}
			}
		});
		storeButton.setText("Store");
		storeButton.setBounds(96, 254, 80, 28);

		lookAtX = new Text(composite_4, SWT.BORDER);
		lookAtX.setBounds(10, 223, 80, 25);
		lookAtX.setText("" + p2.x);
		lookAtX.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					try {
						value = new Double(lookAtX.getText());
						grid.xSplitLookAt = value;
						lookAtX.setForeground(new Color(Display.getCurrent(), 0, 0, 0));

						errorLabelSplit.setText("");
					} catch (Exception e) {
						lookAtX.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
						errorLabelSplit.setText("Number input incorrect!");
					}
				}
			}
		});

		lookAtY = new Text(composite_4, SWT.BORDER);
		lookAtY.setBounds(96, 223, 80, 25);
		lookAtY.setText("" + p2.y);
		lookAtY.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					try {
						value = new Double(lookAtY.getText());
						grid.ySplitLookAt = value;
						lookAtY.setForeground(new Color(Display.getCurrent(), 0, 0, 0));

						errorLabelSplit.setText("");
					} catch (Exception e) {
						lookAtY.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
						errorLabelSplit.setText("Number input incorrect!");
					}
				}
			}
		});

		lookAtZ = new Text(composite_4, SWT.BORDER);
		lookAtZ.setBounds(182, 223, 80, 25);
		lookAtZ.setText("" + p2.z);
		lookAtZ.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					try {
						value = new Double(lookAtZ.getText());
						grid.zSplitLookAt = value;
						lookAtZ.setForeground(new Color(Display.getCurrent(), 0, 0, 0));

						errorLabelSplit.setText("");
					} catch (Exception e) {
						lookAtZ.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
						errorLabelSplit.setText("Number input incorrect!");
					}
				}
			}
		});

		final Label coordinatesLabel = new Label(composite_4, SWT.NONE);
		coordinatesLabel.setText("x");
		coordinatesLabel.setBounds(39, 154, 22, 15);

		final Label yLabel = new Label(composite_4, SWT.NONE);
		yLabel.setText("y");
		yLabel.setBounds(132, 154, 28, 15);

		final Label zLabel = new Label(composite_4, SWT.NONE);
		zLabel.setText("z");
		zLabel.setBounds(216, 154, 28, 15);

		final Label positionLabel_2 = new Label(composite_4, SWT.NONE);
		positionLabel_2.setText("Position");
		positionLabel_2.setBounds(10, 202, 80, 15);

		final Label lookatLabel = new Label(composite_4, SWT.NONE);
		lookatLabel.setText("LookAt");
		lookatLabel.setBounds(10, 254, 80, 15);

		final Scale scale_3 = new Scale(composite_4, SWT.NONE);
		scale_3.setToolTipText("Horizontal speed");
		scale_3.setMaximum(1000);
		scale_3.setSelection(2);
		scale_3.setMinimum(2);
		scale_3.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.SPEED = scale_3.getSelection() / 10;

				}
			}
		});
		scale_3.setBounds(162, 14, 129, 38);

		final Scale scale_3_1 = new Scale(composite_4, SWT.NONE);
		scale_3_1.setToolTipText("Vertical speed");
		scale_3_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.heightSpeed = scale_3_1.getSelection() / 10;

				}

			}
		});
		scale_3_1.setBounds(162, 55, 129, 38);
		scale_3_1.setSelection(1);
		scale_3_1.setMinimum(1);
		scale_3_1.setMaximum(1000);

		final ExpandItem newItemExpandItem_6 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_6.setHeight(300);
		newItemExpandItem_6.setText("Light");

		final Composite composite_6 = new Composite(expandBar, SWT.BORDER);
		newItemExpandItem_6.setControl(composite_6);

		final Spinner spinner = new Spinner(composite_6, SWT.BORDER);
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
		spinner.setBounds(89, 54, 56, 27);

		final Spinner spinner_1 = new Spinner(composite_6, SWT.BORDER);
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
		spinner_1.setBounds(151, 54, 56, 27);
		spinner_1.setSelection(1000);

		final Spinner spinner_2 = new Spinner(composite_6, SWT.BORDER);
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
		spinner_2.setBounds(213, 54, 56, 27);
		spinner_2.setSelection(1000);
		/* Switch for the first light! */
		final Button enabledButton = new Button(composite_6, SWT.CHECK);
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
					//grid.getRenderer().switchLight();
				}
			}
		});
		enabledButton.setText("Enabled");
		enabledButton.setBounds(10, 55, 73, 16);
		/* Switch for the second light! */
		final Button enabledButton_1 = new Button(composite_6, SWT.CHECK);
		enabledButton_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (enabledButton_1.getSelection()) {
						grid.light2 = true;
					} else {
						grid.light2 = false;
					}
					//grid.getRenderer().switchLight();
				}
			}
		});
		enabledButton_1.setBounds(10, 107, 73, 16);
		enabledButton_1.setText("Enabled");

		final Spinner spinner_3 = new Spinner(composite_6, SWT.BORDER);
		spinner_3.setMinimum(-1000000);
		spinner_3.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {

					grid.setLightPos2(spinner_3.getSelection(), grid.getLightPos2()[1], grid.getLightPos2()[2]);

				}
			}
		});
		spinner_3.setBounds(89, 106, 56, 27);
		spinner_3.setMaximum(1000000);

		final Spinner spinner_4 = new Spinner(composite_6, SWT.BORDER);
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
		spinner_4.setBounds(151, 106, 56, 27);

		final Spinner spinner_5 = new Spinner(composite_6, SWT.BORDER);
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
		spinner_5.setBounds(213, 106, 56, 27);

		final Button enabledButton_2 = new Button(composite_6, SWT.CHECK);
		enabledButton_2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (enabledButton_2.getSelection()) {
						grid.light3 = true;
					} else {
						grid.light3 = false;
					}
					//grid.getRenderer().switchLight();
				}
			}
		});
		enabledButton_2.setBounds(10, 161, 73, 16);
		enabledButton_2.setText("Enabled");

		final Spinner spinner_6 = new Spinner(composite_6, SWT.BORDER);
		spinner_6.setMinimum(-1000000);
		spinner_6.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {

					grid.setLightPos3(spinner_6.getSelection(), grid.getLightPos3()[1], grid.getLightPos3()[2]);

				}
			}
		});
		spinner_6.setBounds(89, 160, 56, 27);
		spinner_6.setMaximum(1000000);

		final Spinner spinner_7 = new Spinner(composite_6, SWT.BORDER);
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
		spinner_7.setBounds(151, 160, 56, 27);

		final Spinner spinner_8 = new Spinner(composite_6, SWT.BORDER);
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
		spinner_8.setBounds(213, 160, 56, 27);

		final Button enabledButton_3 = new Button(composite_6, SWT.CHECK);
		enabledButton_3.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (enabledButton_3.getSelection()) {
						grid.light4 = true;
					} else {
						grid.light4 = false;
					}
					//grid.getRenderer().switchLight();
				}
			}
		});
		enabledButton_3.setBounds(10, 214, 73, 16);
		enabledButton_3.setText("Enabled");

		final Spinner spinner_9 = new Spinner(composite_6, SWT.BORDER);
		spinner_9.setMinimum(-1000000);
		spinner_9.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {

					grid.setLightPos4(spinner_9.getSelection(), grid.getLightPos4()[1], grid.getLightPos4()[2]);

				}
			}
		});
		spinner_9.setBounds(89, 213, 56, 27);
		spinner_9.setMaximum(1000000);

		final Spinner spinner_10 = new Spinner(composite_6, SWT.BORDER);
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
		spinner_10.setBounds(151, 213, 56, 27);

		final Spinner spinner_11 = new Spinner(composite_6, SWT.BORDER);
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
		spinner_11.setBounds(213, 213, 56, 27);

		final Button localLightButton = new Button(composite_6, SWT.CHECK);
		localLightButton.setSelection(true);
		localLightButton.setToolTipText("If enabled the lights will rotate with the arcball interface (like a fixed lamp on a plane). ");
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
		localLightButton.setBounds(10, 0, 135, 22);

		final Label posXLabel = new Label(composite_6, SWT.NONE);
		posXLabel.setText("x");
		posXLabel.setBounds(108, 28, 29, 15);

		final Label yLabel_1 = new Label(composite_6, SWT.NONE);
		yLabel_1.setText("y");
		yLabel_1.setBounds(172, 28, 28, 15);

		final Label zLabel_1 = new Label(composite_6, SWT.NONE);
		zLabel_1.setText("z");
		zLabel_1.setBounds(230, 28, 28, 15);

		final Label light1Label = new Label(composite_6, SWT.NONE);
		light1Label.setText("Light 1");
		light1Label.setBounds(10, 77, 63, 22);

		final Label light2Label = new Label(composite_6, SWT.NONE);
		light2Label.setText("Light 2");
		light2Label.setBounds(10, 129, 63, 22);

		final Label light3Label = new Label(composite_6, SWT.NONE);
		light3Label.setText("Light 3");
		light3Label.setBounds(10, 183, 63, 22);

		final Label light4Label = new Label(composite_6, SWT.NONE);
		light4Label.setText("Light 4");
		light4Label.setBounds(10, 236, 63, 22);

		final ExpandItem newItemExpandItem_8 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_8.setHeight(100);
		newItemExpandItem_8.setText("Image");

		final Composite composite_8 = new Composite(expandBar, SWT.BORDER);
		newItemExpandItem_8.setControl(composite_8);

		final Button getImageButton = new Button(composite_8, SWT.NONE);
		getImageButton.setToolTipText("Creates a screenshot image in ImageJ");
		getImageButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.takeScreenshot = true;
				}

			}
		});
		getImageButton.setText("Get Image");
		getImageButton.setBounds(10, 10, 125, 28);

		final Button renderFramesButton = new Button(composite_8, SWT.NONE);
		renderFramesButton.setToolTipText("Starts and stops rendering - \n" + "Creates a stack in Imagej and renders the current\n" + " frame of the Spatial view to the stack until \n"
				+ "rendering is stopped or the max. numbers of frames\n (specified in the textfield) have been reached");
		renderFramesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					if (renderImageJFrames) {
						grid.createBufferedImage = true;

						renderImageJFrames = false;
						grid.renderToImageJ = true;

					} else {
						renderImageJFrames = true;
						grid.renderToImageJ = false;

					}

				}
			}
		});
		renderFramesButton.setText("Render Frames");
		renderFramesButton.setBounds(10, 51, 125, 28);

		final Label numberLabel = new Label(composite_8, SWT.NONE);
		numberLabel.setBounds(141, 30, 171, 15);
		numberLabel.setForeground(new Color(Display.getCurrent(), 255, 0, 0));
		text_11 = new Text(composite_8, SWT.BORDER);
		text_11.setText("500");
		text_11.setBounds(178, 51, 74, 25);
		text_11.addListener(SWT.Modify, new Listener() {

			SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();

			public void handleEvent(Event event) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					try {
						stackSizeCount = new Integer(text_11.getText());
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

		final Label toLabel = new Label(composite_8, SWT.NONE);
		toLabel.setText("To:");
		toLabel.setBounds(141, 57, 31, 15);

		//
		createActions();
		initializeToolBar();
		initializeMenu();
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

	@Override
	public void setFocus() {

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

	public static int getSetFixedFPS() {
		return setFixedFPS.getSelection();
	}
}
