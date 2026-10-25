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
	private Scale scale_2;
	private Scale scale_1;
	private Scale scale;
	private Label errorLabelSplit;
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
		composite.setLayout(new GridLayout(3, true));

		scroll.setMinSize(200, 700);
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

		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		new Label(composite, SWT.NONE);
		sizeXLabel = new Label(composite, SWT.NONE);
		GridData gd_sizeXLabel = new GridData(SWT.CENTER, SWT.CENTER, true, false, 3, 1);
		gd_sizeXLabel.heightHint = 25;
		gd_sizeXLabel.widthHint = 100;
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
		gd_sizeYLabel.widthHint = 100;
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
		gd_sizeZLabel.widthHint = 100;
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

		Button btnDrawnumbers = new Button(composite, SWT.CHECK);
		btnDrawnumbers.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				SpatialStructure st = SpatialStructure.getSpatialStructureInstance();

				AxisLabelsRenderer axisLabelsRenderer = st.getAxisLabelsRenderer();
				if (axisLabelsRenderer.isEnabled()) {
					axisLabelsRenderer.setEnabled(false);
				} else {
					axisLabelsRenderer.setEnabled(true);
				}

			}
		});
		btnDrawnumbers.setText("Draw Axis Numbers");
		/* Label the x and y axes with the pixel coordinates of a displayed texture! */
		AxisLabelsRenderer.setImageCoordinates(store.getBoolean("axisImageCoordinates"));
		final Button imageCoordinatesButton = new Button(composite, SWT.CHECK);
		imageCoordinatesButton.setToolTipText(
				"Displays the axis numbers as pixel coordinates of the texture image.\nThe y coordinates increase from the top of the image like in ImageJ.");
		imageCoordinatesButton.setSelection(AxisLabelsRenderer.isImageCoordinates());
		imageCoordinatesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				AxisLabelsRenderer.setImageCoordinates(imageCoordinatesButton.getSelection());
				store.setValue("axisImageCoordinates", imageCoordinatesButton.getSelection());
			}
		});
		imageCoordinatesButton.setText("Image Coordinates");
		new Label(composite, SWT.NONE);

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
					/* Fitted to a displayed texture! */
					struc.setDoz(struc.getDefaultViewDistance());
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

		createModelTab(expandBar);

		createTextureTab(expandBar);

		final CTabItem newItemExpandItem_4 = new CTabItem(expandBar, SWT.NONE);
		// newItemExpandItem_4.setHeight(300);
		newItemExpandItem_4.setText("Height Map");

		ScrolledComposite scroll_3 = new ScrolledComposite(expandBar, SWT.V_SCROLL);
		final Composite composite_3 = new Composite(scroll_3, SWT.NONE);
		composite_3.setToolTipText("Enables the height map. Visible if a texture is active!");
		newItemExpandItem_4.setControl(scroll_3);
		composite_3.setLayout(new GridLayout(2, true));
		scroll_3.setMinSize(200, 300);
		scroll_3.setContent(composite_3);
		scroll_3.setExpandVertical(true);
		scroll_3.setExpandHorizontal(true);
		scroll_3.setAlwaysShowScrollBars(true);

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
		composite_4.setLayout(new GridLayout(3, true));
		scroll_4.setMinSize(200, 500);
		scroll_4.setContent(composite_4);
		scroll_4.setExpandVertical(true);
		scroll_4.setExpandHorizontal(true);
		scroll_4.setAlwaysShowScrollBars(true);

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
		newItemExpandItem_8.setText("Image");
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
		text.setToolTipText("The *.obj file of the model");
		final Text pathText = text;
		pathText.addModifyListener(e -> store.setValue("objectFile", pathText.getText()));
		final Button browseButton = new Button(compositeModel, SWT.NONE);
		browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		browseButton.setText("Browse...");
		browseButton.setToolTipText("Selects an *.obj file");
		browseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				FileDialog fd = new FileDialog(browseButton.getShell(), SWT.OPEN);
				fd.setText("Load");
				fd.setFilterExtensions(new String[] { "*.obj", "*.*" });
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
		loadButton.setToolTipText("Loads (or reloads) the *.obj model (with materials and textures)");
		loadButton.moveAbove(showButton);
		loadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SpatialStructure grid = SpatialStructure.getSpatialStructureInstance();
				if (grid == null) {
					return;
				}
				if (!new java.io.File(pathText.getText()).isFile()) {
					infoLabel.setText("The *.obj file doesn't exist!");
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
	private void createTextureTab(CTabFolder tabFolder) {
		final CTabItem textureItem = new CTabItem(tabFolder, SWT.NONE);
		textureItem.setText("Texture");
		ScrolledComposite scrollTexture = new ScrolledComposite(tabFolder, SWT.V_SCROLL);
		final Composite compositeTexture = new Composite(scrollTexture, SWT.NONE);
		textureItem.setControl(scrollTexture);
		compositeTexture.setLayout(new GridLayout(2, true));
		scrollTexture.setMinSize(200, 300);
		scrollTexture.setContent(compositeTexture);
		scrollTexture.setExpandVertical(true);
		scrollTexture.setExpandHorizontal(true);
		scrollTexture.setAlwaysShowScrollBars(true);

		final Label infoLabel = new Label(compositeTexture, SWT.WRAP);
		GridData gd_infoLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_infoLabel.heightHint = 40;
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

		final Label sourceLabel = new Label(compositeTexture, SWT.NONE);
		sourceLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		sourceLabel.setText("Source:");
		final Combo sourceCombo = new Combo(compositeTexture, SWT.READ_ONLY);
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

		final Text pathText = new Text(compositeTexture, SWT.BORDER);
		pathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		pathText.setText(store.getString("spatialImage"));
		pathText.setToolTipText("The image file of the texture");
		pathText.addModifyListener(e -> store.setValue("spatialImage", pathText.getText()));
		final Button browseButton = new Button(compositeTexture, SWT.NONE);
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

		final Button showButton = new Button(compositeTexture, SWT.CHECK);

		final Button loadButton = new Button(compositeTexture, SWT.NONE);
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

		final Button removeButton = new Button(compositeTexture, SWT.NONE);
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
		showButton.setToolTipText("Displays the texture (and the height map, see the Height Map tab)");
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
		final Button flipTextureButton = new Button(compositeTexture, SWT.CHECK);
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

		final Label smoothingLabel = createVolumeScaleLabel(compositeIso, "Smoothing: 1.0");
		final Scale smoothingScale = createVolumeScale(compositeIso, 0, 30, 10,
				"The Gaussian smoothing (sigma in voxels) of the stack before the surface is computed");
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
		scrollMeasure.setMinSize(200, 580);
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

		Label smoothLabel = new Label(compositeMeasure, SWT.NONE);
		smoothLabel.setText("Smoothing (Voxels):");
		final Spinner smoothSpinner = new Spinner(compositeMeasure, SWT.BORDER);
		smoothSpinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		smoothSpinner.setDigits(1);
		smoothSpinner.setMinimum(0);
		smoothSpinner.setMaximum(50);
		smoothSpinner.setSelection(0);
		smoothSpinner.setToolTipText(
				"The Gaussian smoothing of the stack for the measurements (0 = the data at the iso value).\nThe smoothing of the display is not used for the measurements.");

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
				measurement.setMeasureSmoothing(smoothSpinner.getSelection() / 10f);
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
				measurement.setMeasureSmoothing(smoothSpinner.getSelection() / 10f);
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
