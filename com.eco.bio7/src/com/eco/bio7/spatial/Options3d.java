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
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;

public class Options3d extends ViewPart {
	public static final String ID = "com.eco.bio7.discrete3d.Options3d"; //$NON-NLS-1$

	private ExpandItem newItemExpandItem;
	private Text worldExtent;
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
	private static int stackSizeCount=100;
	private static int fpsScreenCapture=30;
	private int fixedFps;

	public Options3d() {

	}

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
		// expandBar.setBackground(parent.getBackground());

		newItemExpandItem = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem.setHeight(700);
		newItemExpandItem.setText("Space");

		final Composite composite = new Composite(expandBar, SWT.BORDER);
		newItemExpandItem.setControl(composite);
		composite.setLayout(new GridLayout(3, true));
		GraphicsDevice graphicDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		DisplayMode defaultMode = graphicDevice.getDisplayMode();
		SpatialUtil.setFullscreenOptions(defaultMode.getWidth(), defaultMode.getHeight(), defaultMode.getBitDepth(),
				defaultMode.getRefreshRate());
		d = graphicDevice.getDisplayModes();

		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		sizeXLabel = new Label(composite, SWT.NONE);
		GridData gd_sizeXLabel = new GridData(SWT.CENTER, SWT.CENTER, true, false, 3, 1);
		gd_sizeXLabel.heightHint = 25;
		sizeXLabel.setLayoutData(gd_sizeXLabel);

		scale = new Scale(composite, SWT.NONE);
		scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

		scale.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure sg = SpatialStructure.getSpatialStructureInstance();
				if (sg != null) {
					sg.setSizeX(scale.getSelection());
				}

				store.setValue("spatialX", scale.getSelection());
				sizeXLabel.setText("Size X: " + scale.getSelection() * 2);
				scale.setToolTipText("-" + scale.getSelection() + " to +" + scale.getSelection());
			}
		});

		scale.setMaximum((int) halfExtent);

		scale.setSelection(1000);
		sizeXLabel.setText("Size X: " + scale.getSelection() * 2);
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);

		sizeYLabel = new Label(composite, SWT.NONE);
		GridData gd_sizeYLabel = new GridData(SWT.CENTER, SWT.FILL, true, false, 3, 1);
		gd_sizeYLabel.heightHint = 25;
		sizeYLabel.setLayoutData(gd_sizeYLabel);
		scale_1 = new Scale(composite, SWT.NONE);
		scale_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

		scale_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure sg = SpatialStructure.getSpatialStructureInstance();
				if (sg != null) {
					sg.setSizeY(scale_1.getSelection());
				}

				store.setValue("spatialY", scale_1.getSelection());
				sizeYLabel.setText("Size Y: " + scale_1.getSelection() * 2);
				scale_1.setToolTipText("-" + scale_1.getSelection() + " to +" + scale_1.getSelection());
			}
		});

		scale_1.setMaximum((int) halfExtent);
		scale_1.setSelection(1000);

		sizeZLabel = new Label(composite, SWT.NONE);
		GridData gd_sizeZLabel = new GridData(SWT.CENTER, SWT.FILL, true, false, 3, 1);
		gd_sizeZLabel.heightHint = 25;
		sizeZLabel.setLayoutData(gd_sizeZLabel);
		scale_2 = new Scale(composite, SWT.NONE);
		scale_2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

		scale_2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure sg = SpatialStructure.getSpatialStructureInstance();
				if (sg != null) {
					sg.setSizeZ(scale_2.getSelection());
				}

				store.setValue("spatialZ", scale_2.getSelection());
				sizeZLabel.setText("Size Z: " + scale_2.getSelection() * 2);
				scale_2.setToolTipText("-" + scale_2.getSelection() + " to +" + scale_2.getSelection());
			}
		});
		scale_2.setMaximum((int) halfExtent);
		scale_2.setSelection(1000);

		final Label label_3 = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridData gd_label_3 = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
		gd_label_3.heightHint = 25;
		label_3.setLayoutData(gd_label_3);
		label_3.setText("Label");

		octantButton = new Button(composite, SWT.CHECK);
		GridData gd_octantButton = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		gd_octantButton.heightHint = 25;
		octantButton.setLayoutData(gd_octantButton);
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

		showAxesButton = new Button(composite, SWT.CHECK);
		GridData gd_showAxesButton = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		gd_showAxesButton.heightHint = 25;
		showAxesButton.setLayoutData(gd_showAxesButton);
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

		showQuadButton = new Button(composite, SWT.CHECK);
		GridData gd_showQuadButton = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_showQuadButton.heightHint = 25;
		showQuadButton.setLayoutData(gd_showQuadButton);
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

		final Label label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gd_label = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
		gd_label.heightHint = 25;
		label.setLayoutData(gd_label);

		showGridButton = new Button(composite, SWT.CHECK);
		GridData gd_showGridButton = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		gd_showGridButton.heightHint = 25;
		showGridButton.setLayoutData(gd_showGridButton);
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

		final Spinner spinner_12 = new Spinner(composite, SWT.BORDER);
		GridData gd_spinner_12 = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_spinner_12.heightHint = 25;
		spinner_12.setLayoutData(gd_spinner_12);
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

		final Label girdSizeLabel = new Label(composite, SWT.NONE);
		GridData gd_girdSizeLabel = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_girdSizeLabel.heightHint = 25;
		girdSizeLabel.setLayoutData(gd_girdSizeLabel);
		girdSizeLabel.setText("Size");

		final Button xzButton = new Button(composite, SWT.CHECK);
		GridData gd_xzButton = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		gd_xzButton.heightHint = 25;
		xzButton.setLayoutData(gd_xzButton);
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

		final Button yzButton = new Button(composite, SWT.CHECK);
		GridData gd_yzButton = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		gd_yzButton.heightHint = 25;
		yzButton.setLayoutData(gd_yzButton);
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

		final Button xyButton = new Button(composite, SWT.CHECK);
		GridData gd_xyButton = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		gd_xyButton.heightHint = 25;
		xyButton.setLayoutData(gd_xyButton);
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

		final Label label_2 = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridData gd_label_2 = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		gd_label_2.heightHint = 25;
		label_2.setLayoutData(gd_label_2);
		label_2.setText("Label");
		sizeYLabel.setText("Size Y: " + scale_1.getSelection() * 2);

		final Button colourButton = new Button(composite, SWT.NONE);
		GridData gd_colourButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_colourButton.heightHint = 40;
		colourButton.setLayoutData(gd_colourButton);
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

		final Button colourLinesButton = new Button(composite, SWT.NONE);
		GridData gd_colourLinesButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_colourLinesButton.heightHint = 40;
		colourLinesButton.setLayoutData(gd_colourLinesButton);
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
		new Label(composite, SWT.NONE);

		final Label label_2_1 = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridData gd_label_2_1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		gd_label_2_1.heightHint = 25;
		label_2_1.setLayoutData(gd_label_2_1);
		label_2_1.setText("Label");

		Label lblSpeedDistance = new Label(composite, SWT.NONE);
		GridData gd_lblSpeedDistance = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_lblSpeedDistance.heightHint = 25;
		lblSpeedDistance.setLayoutData(gd_lblSpeedDistance);
		lblSpeedDistance.setText("Speed Drag Distance");

		final Label worldExtentLabel = new Label(composite, SWT.NONE);
		GridData gd_worldExtentLabel = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		gd_worldExtentLabel.heightHint = 25;
		worldExtentLabel.setLayoutData(gd_worldExtentLabel);
		worldExtentLabel.setText("World Extent");

		new Label(composite, SWT.NONE);

		multiplyDragCameraSpeedSpinner = new Spinner(composite, SWT.BORDER);
		GridData gd_multiplyDragCameraSpeedSpinner = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_multiplyDragCameraSpeedSpinner.heightHint = 25;
		multiplyDragCameraSpeedSpinner.setLayoutData(gd_multiplyDragCameraSpeedSpinner);
		multiplyDragCameraSpeedSpinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SpatialStructure.setMultiplyDragCameraSpeed(multiplyDragCameraSpeedSpinner.getSelection());
			}
		});
		multiplyDragCameraSpeedSpinner.setMaximum(10000);
		multiplyDragCameraSpeedSpinner.setMinimum(1);
		multiplyDragCameraSpeedSpinner.setSelection(1);

		worldExtent = new Text(composite, SWT.BORDER);
		GridData gd_worldExtent = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_worldExtent.heightHint = 25;
		worldExtent.setLayoutData(gd_worldExtent);
		worldExtent.setToolTipText("Set the world extent");
		worldExtent.setText("100000.0");
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
		new Label(composite, SWT.NONE);

		Button btnResetView = new Button(composite, SWT.NONE);
		GridData gd_btnResetView = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_btnResetView.heightHint = 40;
		btnResetView.setLayoutData(gd_btnResetView);
		btnResetView.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
				if (struc != null) {
					struc.setDoz(2000);
				}

			}
		});
		btnResetView.setText("Reset View Distance");
		sizeZLabel.setText("Size Z: " + scale_2.getSelection() * 2);
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		worldExtentError = new Label(composite, SWT.NONE);
		worldExtentError.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		new Label(composite, SWT.NONE);

		if (we != 0) {
			worldExtent.setText("" + we);
		}

		final ExpandItem newItemExpandItem_1 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_1.setHeight(350);
		newItemExpandItem_1.setText("Time");

		final Composite composite_2 = new Composite(expandBar, SWT.BORDER);
		newItemExpandItem_1.setControl(composite_2);
		composite_2.setLayout(new GridLayout(2, true));

		playPauseButton = new Button(composite_2, SWT.TOGGLE);
		GridData gd_playPauseButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_playPauseButton.heightHint = 40;
		playPauseButton.setLayoutData(gd_playPauseButton);
		playPauseButton.setToolTipText(
				"Play/Pause: Invokes the compiled run method with the default Jogl timer.\n The timer fps determines the speed of invocation");
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
		playPauseButton.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/play_pause.png").createImage());

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
		setupButton.setImage(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/setup.png").createImage());
		// setupButton.setText("Setup");
		new Label(composite_2, SWT.NONE);
		new Label(composite_2, SWT.NONE);

		final Label label_4 = new Label(composite_2, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gd_label_4 = new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1);
		gd_label_4.heightHint = 25;
		label_4.setLayoutData(gd_label_4);

		final Label framerateLabel = new Label(composite_2, SWT.NONE);
		GridData gd_framerateLabel = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_framerateLabel.heightHint = 25;
		framerateLabel.setLayoutData(gd_framerateLabel);
		framerateLabel.setText("Step rate");
		new Label(composite_2, SWT.NONE);

		final Scale scale_7 = new Scale(composite_2, SWT.NONE);
		scale_7.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		scale_7.setToolTipText("The speed of the canStep() function can be adjusted with this slider");

		scale_7.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure.setPeriod(scale_7.getSelection() * 1000000);
			}
		});
		scale_7.setMaximum(1000);
		scale_7.setSelection(1000);
		new Label(composite_2, SWT.NONE);
		new Label(composite_2, SWT.NONE);

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

		final ExpandItem newItemExpandItem_2 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_2.setHeight(600);
		newItemExpandItem_2.setText("Model");

		final Composite composite_1 = new Composite(expandBar, SWT.BORDER);
		newItemExpandItem_2.setControl(composite_1);
		composite_1.setLayout(new GridLayout(3, true));

		text = new Text(composite_1, SWT.BORDER);
		GridData gd_text = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_text.heightHint = 25;
		text.setLayoutData(gd_text);
		text.setText(store.getString("objectFile"));
		final Button pathButton = new Button(composite_1, SWT.NONE);
		GridData gd_pathButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_pathButton.heightHint = 40;
		pathButton.setLayoutData(gd_pathButton);
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
				if (path != null) {
					text.setText(path);
					store.setValue("objectFile", path);
				}
			}
		});
		pathButton.setText("Set Path");

		final Button activateButton = new Button(composite_1, SWT.CHECK);
		GridData gd_activateButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_activateButton.heightHint = 25;
		activateButton.setLayoutData(gd_activateButton);
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

		final Button lightenedButton_1 = new Button(composite_1, SWT.CHECK);
		GridData gd_lightenedButton_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lightenedButton_1.heightHint = 25;
		lightenedButton_1.setLayoutData(gd_lightenedButton_1);
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

		final Button reloadButton = new Button(composite_1, SWT.NONE);
		GridData gd_reloadButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_reloadButton.heightHint = 40;
		reloadButton.setLayoutData(gd_reloadButton);
		reloadButton.setToolTipText("Load or reload the model");
		reloadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure.setReloadModel(true);
			}
		});
		reloadButton.setText("Reload");

		final Label label_6 = new Label(composite_1, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gd_label_6 = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		gd_label_6.heightHint = 25;
		label_6.setLayoutData(gd_label_6);

		errorLabel = new Label(composite_1, SWT.NONE);
		GridData gd_errorLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		gd_errorLabel.heightHint = 25;
		errorLabel.setLayoutData(gd_errorLabel);
		errorLabel.setForeground(new Color(Display.getCurrent(), 255, 0, 0));

		modelPosX = new Text(composite_1, SWT.BORDER);
		GridData gd_modelPosX = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_modelPosX.heightHint = 25;
		modelPosX.setLayoutData(gd_modelPosX);
		modelPosX.setText("" + p.x);
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
		GridData gd_modelPosY = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_modelPosY.heightHint = 25;
		modelPosY.setLayoutData(gd_modelPosY);
		modelPosY.setText("" + p.y);
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
		GridData gd_modelPosZ = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_modelPosZ.heightHint = 25;
		modelPosZ.setLayoutData(gd_modelPosZ);
		modelPosZ.setText("" + p.z);
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
		GridData gd_positionLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_positionLabel.heightHint = 25;
		positionLabel.setLayoutData(gd_positionLabel);
		positionLabel.setText("Position: x");

		final Label positionYLabel = new Label(composite_1, SWT.NONE);
		GridData gd_positionYLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_positionYLabel.heightHint = 25;
		positionYLabel.setLayoutData(gd_positionYLabel);
		positionYLabel.setText("Position: y");

		final Label positionZLabel = new Label(composite_1, SWT.NONE);
		GridData gd_positionZLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_positionZLabel.heightHint = 25;
		positionZLabel.setLayoutData(gd_positionZLabel);
		positionZLabel.setText("Position: z");

		modelRotX = new Text(composite_1, SWT.BORDER);
		GridData gd_modelRotX = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_modelRotX.heightHint = 25;
		modelRotX.setLayoutData(gd_modelRotX);
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
		GridData gd_modelRotY = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_modelRotY.heightHint = 25;
		modelRotY.setLayoutData(gd_modelRotY);
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
		GridData gd_modelRotZ = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_modelRotZ.heightHint = 25;
		modelRotZ.setLayoutData(gd_modelRotZ);
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
		GridData gd_positionLabel_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_positionLabel_1.heightHint = 25;
		positionLabel_1.setLayoutData(gd_positionLabel_1);
		positionLabel_1.setText("Rotation: x");

		final Label positionYLabel_1 = new Label(composite_1, SWT.NONE);
		GridData gd_positionYLabel_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_positionYLabel_1.heightHint = 25;
		positionYLabel_1.setLayoutData(gd_positionYLabel_1);
		positionYLabel_1.setText("Rotation: y");

		final Label positionZLabel_1 = new Label(composite_1, SWT.NONE);
		GridData gd_positionZLabel_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_positionZLabel_1.heightHint = 25;
		positionZLabel_1.setLayoutData(gd_positionZLabel_1);
		positionZLabel_1.setText("Rotation: z");

		modelScale = new Text(composite_1, SWT.BORDER);
		GridData gd_modelScale = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_modelScale.heightHint = 25;
		modelScale.setLayoutData(gd_modelScale);
		modelScale.setText("" + p3.x);
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
		new Label(composite_1, SWT.NONE);
		new Label(composite_1, SWT.NONE);

		final Label scaleLabel = new Label(composite_1, SWT.NONE);
		GridData gd_scaleLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_scaleLabel.heightHint = 25;
		scaleLabel.setLayoutData(gd_scaleLabel);
		scaleLabel.setText("Scale");
		new Label(composite_1, SWT.NONE);
		new Label(composite_1, SWT.NONE);

		final Button storeButton_1 = new Button(composite_1, SWT.NONE);
		GridData gd_storeButton_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		gd_storeButton_1.heightHint = 40;
		storeButton_1.setLayoutData(gd_storeButton_1);
		storeButton_1.setToolTipText("Store the position, rotation and scale (only as integer!)");
		storeButton_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.getPref3d().storeModelLocation(new Double(modelPosX.getText()),
							new Double(modelPosY.getText()), new Double(modelPosZ.getText()));
					grid.getPref3d().storeModelRotation(new Double(modelRotX.getText()),
							new Double(modelRotY.getText()), new Double(modelRotZ.getText()));
					grid.getPref3d().storeModelScale(new Double(modelScale.getText()), 0, 0);
				}

			}
		});
		storeButton_1.setText("Store");

		final ExpandItem newItemExpandItem_3 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_3.setHeight(400);
		newItemExpandItem_3.setText("Texture");

		final Composite composite_5 = new Composite(expandBar, SWT.BORDER);
		newItemExpandItem_3.setControl(composite_5);
		composite_5.setLayout(new GridLayout(2, true));

		final Button enableTextureButton = new Button(composite_5, SWT.CHECK);
		GridData gd_enableTextureButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_enableTextureButton.heightHint = 25;
		enableTextureButton.setLayoutData(gd_enableTextureButton);
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
		new Label(composite_5, SWT.NONE);
		new Label(composite_5, SWT.NONE);
		new Label(composite_5, SWT.NONE);

		final Label label_5 = new Label(composite_5, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gd_label_5 = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		gd_label_5.heightHint = 25;
		label_5.setLayoutData(gd_label_5);

		final Label staticTextureLabel = new Label(composite_5, SWT.NONE);
		GridData gd_staticTextureLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_staticTextureLabel.heightHint = 25;
		staticTextureLabel.setLayoutData(gd_staticTextureLabel);
		staticTextureLabel.setText("Static Texture:");
		new Label(composite_5, SWT.NONE);
		new Label(composite_5, SWT.NONE);
		new Label(composite_5, SWT.NONE);

		final Button fromFileButton_1 = new Button(composite_5, SWT.RADIO);
		GridData gd_fromFileButton_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_fromFileButton_1.heightHint = 25;
		fromFileButton_1.setLayoutData(gd_fromFileButton_1);
		fromFileButton_1.setToolTipText("From file will create a non dynamic texture");
		fromFileButton_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

			}
		});
		fromFileButton_1.setText("From File");

		fromImagejButton_2 = new Button(composite_5, SWT.RADIO);
		GridData gd_fromImagejButton_2 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_fromImagejButton_2.heightHint = 25;
		fromImagejButton_2.setLayoutData(gd_fromImagejButton_2);
		fromImagejButton_2
				.setToolTipText("From ImageJ will create a non dynamic texture from an enabled image in ImageJ");
		fromImagejButton_2.setText("From ImageJ");

		text_2 = new Text(composite_5, SWT.BORDER);
		GridData gd_text_2 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_text_2.heightHint = 25;
		text_2.setLayoutData(gd_text_2);
		text_2.setText(store.getString("spatialImage"));

		final Button pathButton_2 = new Button(composite_5, SWT.NONE);
		GridData gd_pathButton_2 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_pathButton_2.heightHint = 40;
		pathButton_2.setLayoutData(gd_pathButton_2);
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

		final Label label_5_1 = new Label(composite_5, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridData gd_label_5_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_label_5_1.heightHint = 25;
		label_5_1.setLayoutData(gd_label_5_1);
		label_5_1.setText("Label");

		final Label dynamicLabel = new Label(composite_5, SWT.NONE);
		GridData gd_dynamicLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_dynamicLabel.heightHint = 25;
		dynamicLabel.setLayoutData(gd_dynamicLabel);
		dynamicLabel.setText("Dynamic Texture:");
		new Label(composite_5, SWT.NONE);

		final Button fromQuad2dButton = new Button(composite_5, SWT.RADIO);
		GridData gd_fromQuad2dButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_fromQuad2dButton.heightHint = 25;
		fromQuad2dButton.setLayoutData(gd_fromQuad2dButton);
		fromQuad2dButton.setToolTipText("This options creates a dynamic texture from the Quads view");
		fromQuad2dButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

			}
		});
		fromQuad2dButton.setText("From Quad2d");
		final Button fromImagejButton_1 = new Button(composite_5, SWT.RADIO);
		GridData gd_fromImagejButton_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_fromImagejButton_1.heightHint = 25;
		fromImagejButton_1.setLayoutData(gd_fromImagejButton_1);
		fromImagejButton_1.setToolTipText("This option creates a dynamic texture from the current ImageJ image");
		fromImagejButton_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

			}
		});
		fromImagejButton_1.setText("From ImageJ");

		final Label label_5_1_1 = new Label(composite_5, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridData gd_label_5_1_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_label_5_1_1.heightHint = 25;
		label_5_1_1.setLayoutData(gd_label_5_1_1);
		label_5_1_1.setText("Label");

		final Button setButton = new Button(composite_5, SWT.NONE);
		GridData gd_setButton = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_setButton.heightHint = 40;
		setButton.setLayoutData(gd_setButton);
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

		final ExpandItem newItemExpandItem_4 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_4.setHeight(300);
		newItemExpandItem_4.setText("Height Map");

		final Composite composite_3 = new Composite(expandBar, SWT.BORDER);
		composite_3.setToolTipText("Enables the height map. Visible if a texture is active!");
		newItemExpandItem_4.setControl(composite_3);
		composite_3.setLayout(new GridLayout(2, true));

		final Button enableHeightMapButton = new Button(composite_3, SWT.CHECK);
		GridData gd_enableHeightMapButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_enableHeightMapButton.heightHint = 25;
		enableHeightMapButton.setLayoutData(gd_enableHeightMapButton);
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

		final Button gcButton = new Button(composite_3, SWT.CHECK);
		GridData gd_gcButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_gcButton.heightHint = 25;
		gcButton.setLayoutData(gd_gcButton);
		gcButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (gcButton.getSelection()) {
					HeightMaps.setGraphicsCardSwitch(true);
				} else {
					HeightMaps.setGraphicsCardSwitch(false);
				}
			}
		});
		gcButton.setToolTipText(
				"This is a special switch for the texture.\n For some graphics cards it seems to be necessary\n to assign the texture differently!");
		gcButton.setText("Gc");
		new Label(composite_3, SWT.NONE);

		final Button geoButton = new Button(composite_3, SWT.CHECK);
		GridData gd_geoButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_geoButton.heightHint = 25;
		geoButton.setLayoutData(gd_geoButton);
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

		final Button staticHeightButton = new Button(composite_3, SWT.CHECK);
		GridData gd_staticHeightButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_staticHeightButton.heightHint = 25;
		staticHeightButton.setLayoutData(gd_staticHeightButton);
		staticHeightButton.setToolTipText(
				"If dynamic is selected the height values are updated directly from the active ImageJ image");
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

		final Button lightenedButton = new Button(composite_3, SWT.CHECK);
		GridData gd_lightenedButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lightenedButton.heightHint = 25;
		lightenedButton.setLayoutData(gd_lightenedButton);
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
		new Label(composite_3, SWT.NONE);
		new Label(composite_3, SWT.NONE);

		final Scale scale_8 = new Scale(composite_3, SWT.NONE);
		scale_8.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		scale_8.setToolTipText("Adjusts the amount of triangles for the height map.");
		scale_8.setSelection(15);
		scale_8.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				HeightMaps height = SpatialStructure.getHeightMapInstance();

				height.setDetail(scale_8.getSelection());
			}
		});
		scale_8.setMinimum(1);

		final Label levelOfDetailLabel_1 = new Label(composite_3, SWT.NONE);
		GridData gd_levelOfDetailLabel_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_levelOfDetailLabel_1.heightHint = 25;
		levelOfDetailLabel_1.setLayoutData(gd_levelOfDetailLabel_1);
		levelOfDetailLabel_1.setText("Level of Detail");

		final Scale scaleHeight = new Scale(composite_3, SWT.NONE);
		GridData gd_scaleHeight = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_scaleHeight.widthHint = 200;
		scaleHeight.setLayoutData(gd_scaleHeight);
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

		final Label factorHeightLabel = new Label(composite_3, SWT.NONE);
		GridData gd_factorHeightLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_factorHeightLabel.heightHint = 25;
		factorHeightLabel.setLayoutData(gd_factorHeightLabel);
		factorHeightLabel.setText("Factor Height");

		final ExpandItem newItemExpandItem_5 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_5.setHeight(500);
		newItemExpandItem_5.setText("Camera");

		final Composite composite_4 = new Composite(expandBar, SWT.BORDER);
		newItemExpandItem_5.setControl(composite_4);
		composite_4.setLayout(new GridLayout(3, true));

		final Button enableWalkthroughButton = new Button(composite_4, SWT.CHECK);
		GridData gd_enableWalkthroughButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_enableWalkthroughButton.heightHint = 25;
		enableWalkthroughButton.setLayoutData(gd_enableWalkthroughButton);
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

		final Scale scale_3 = new Scale(composite_4, SWT.NONE);
		scale_3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
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

		final Button enableFlythroughButton = new Button(composite_4, SWT.CHECK);
		GridData gd_enableFlythroughButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_enableFlythroughButton.heightHint = 25;
		enableFlythroughButton.setLayoutData(gd_enableFlythroughButton);
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

		final Scale scale_3_1 = new Scale(composite_4, SWT.NONE);
		scale_3_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		scale_3_1.setToolTipText("Vertical speed");
		scale_3_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.heightSpeed = scale_3_1.getSelection() / 10;

				}

			}
		});
		scale_3_1.setSelection(1);
		scale_3_1.setMinimum(1);
		scale_3_1.setMaximum(1000);

		final Label label_1_1 = new Label(composite_4, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridData gd_label_1_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		gd_label_1_1.heightHint = 25;
		label_1_1.setLayoutData(gd_label_1_1);
		label_1_1.setText("Label");

		final Button customCameraButton = new Button(composite_4, SWT.CHECK);
		GridData gd_customCameraButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_customCameraButton.heightHint = 25;
		customCameraButton.setLayoutData(gd_customCameraButton);
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
		new Label(composite_4, SWT.NONE);
		new Label(composite_4, SWT.NONE);

		final Button splitViewButton = new Button(composite_4, SWT.CHECK);
		GridData gd_splitViewButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_splitViewButton.heightHint = 25;
		splitViewButton.setLayoutData(gd_splitViewButton);
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
		new Label(composite_4, SWT.NONE);
		new Label(composite_4, SWT.NONE);

		final Label coordinatesLabel = new Label(composite_4, SWT.NONE);
		GridData gd_coordinatesLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_coordinatesLabel.heightHint = 25;
		coordinatesLabel.setLayoutData(gd_coordinatesLabel);
		coordinatesLabel.setText("x");

		final Label yLabel = new Label(composite_4, SWT.NONE);
		GridData gd_yLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_yLabel.heightHint = 25;
		yLabel.setLayoutData(gd_yLabel);
		yLabel.setText("y");

		final Label zLabel = new Label(composite_4, SWT.NONE);
		GridData gd_zLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_zLabel.heightHint = 25;
		zLabel.setLayoutData(gd_zLabel);
		zLabel.setText("z");

		splitCamX = new Text(composite_4, SWT.BORDER);
		GridData gd_splitCamX = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_splitCamX.heightHint = 25;
		splitCamX.setLayoutData(gd_splitCamX);
		splitCamX.setText("" + p.x);
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
		GridData gd_splitCamY = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_splitCamY.heightHint = 25;
		splitCamY.setLayoutData(gd_splitCamY);
		splitCamY.setText("" + p.y);
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
		GridData gd_splitCamZ = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_splitCamZ.heightHint = 25;
		splitCamZ.setLayoutData(gd_splitCamZ);
		splitCamZ.setText("" + p.z);
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

		final Label positionLabel_2 = new Label(composite_4, SWT.NONE);
		GridData gd_positionLabel_2 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_positionLabel_2.heightHint = 25;
		positionLabel_2.setLayoutData(gd_positionLabel_2);
		positionLabel_2.setText("Position");
		new Label(composite_4, SWT.NONE);
		new Label(composite_4, SWT.NONE);

		lookAtX = new Text(composite_4, SWT.BORDER);
		GridData gd_lookAtX = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lookAtX.heightHint = 25;
		lookAtX.setLayoutData(gd_lookAtX);
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
		GridData gd_lookAtY = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lookAtY.heightHint = 25;
		lookAtY.setLayoutData(gd_lookAtY);
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
		GridData gd_lookAtZ = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lookAtZ.heightHint = 25;
		lookAtZ.setLayoutData(gd_lookAtZ);
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

		final Label lookatLabel = new Label(composite_4, SWT.NONE);
		GridData gd_lookatLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lookatLabel.heightHint = 25;
		lookatLabel.setLayoutData(gd_lookatLabel);
		lookatLabel.setText("LookAt");

		final Button storeButton = new Button(composite_4, SWT.NONE);
		GridData gd_storeButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_storeButton.heightHint = 40;
		storeButton.setLayoutData(gd_storeButton);
		storeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid != null) {
					grid.getPref3d().storeSplitViewCameraLocation(new Double(splitCamX.getText()),
							new Double(splitCamY.getText()), new Double(splitCamZ.getText()));
					grid.getPref3d().storeSplitViewCameraLookAt(new Double(lookAtX.getText()),
							new Double(lookAtY.getText()), new Double(lookAtZ.getText()));
				}
			}
		});
		storeButton.setText("Store");
		new Label(composite_4, SWT.NONE);

		errorLabelSplit = new Label(composite_4, SWT.NONE);
		GridData gd_errorLabelSplit = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		gd_errorLabelSplit.heightHint = 25;
		errorLabelSplit.setLayoutData(gd_errorLabelSplit);
		errorLabelSplit.setForeground(new Color(Display.getCurrent(), 255, 0, 0));

		final Label label_1 = new Label(composite_4, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gd_label_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		gd_label_1.heightHint = 25;
		label_1.setLayoutData(gd_label_1);

		final Button stereoButton = new Button(composite_4, SWT.CHECK);
		GridData gd_stereoButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_stereoButton.heightHint = 25;
		stereoButton.setLayoutData(gd_stereoButton);
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
		new Label(composite_4, SWT.NONE);
		new Label(composite_4, SWT.NONE);
		Point3d p = Bio7PrefConverterSpatial.getPoint(store, "splitCameraLoc");
		Point3d p2 = Bio7PrefConverterSpatial.getPoint(store, "splitCameraLookAt");

		final ExpandItem newItemExpandItem_6 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_6.setHeight(350);
		newItemExpandItem_6.setText("Light");

		final Composite composite_6 = new Composite(expandBar, SWT.BORDER);
		newItemExpandItem_6.setControl(composite_6);
		composite_6.setLayout(new GridLayout(4, true));
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

		final ExpandItem newItemExpandItem_8 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_8.setHeight(130);
		newItemExpandItem_8.setText("Image");

		final Composite composite_8 = new Composite(expandBar, SWT.BORDER);
		newItemExpandItem_8.setControl(composite_8);
		composite_8.setLayout(new GridLayout(1, true));

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
						//screen.setupCaptureImages();
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
