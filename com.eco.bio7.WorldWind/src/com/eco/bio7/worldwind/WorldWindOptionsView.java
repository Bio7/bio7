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

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.cache.BasicDataFileStore;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.data.BufferedImageRaster;
import gov.nasa.worldwind.data.DataRaster;
import gov.nasa.worldwind.data.DataRasterReader;
import gov.nasa.worldwind.data.DataRasterReaderFactory;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.coords.MGRSCoord;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.LatLonGraticuleLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.SurfaceImageLayer;
import gov.nasa.worldwind.layers.ViewControlsLayer;
import gov.nasa.worldwind.layers.ViewControlsSelectListener;
import gov.nasa.worldwind.layers.Earth.MGRSGraticuleLayer;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;
import gov.nasa.worldwind.poi.BasicPointOfInterest;
import gov.nasa.worldwind.poi.Gazetteer;
import gov.nasa.worldwind.poi.PointOfInterest;
import gov.nasa.worldwind.poi.YahooGazetteer;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.UnitsFormat;
import gov.nasa.worldwind.util.measure.MeasureTool;
import gov.nasa.worldwind.util.measure.MeasureToolController;
import gov.nasa.worldwind.view.orbit.OrbitView;
import gov.nasa.worldwindx.examples.util.ExampleUtil;
import gov.nasa.worldwindx.examples.util.FileStoreDataSet;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.OvalRoi;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import worldw.Activator;

import com.eco.bio7.image.CanvasView;
import com.swtdesigner.SWTResourceManager;

import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;

public class WorldWindOptionsView extends ViewPart {

	private List list;
	private Table table;
	private Combo combo_4;
	private Combo combo_3;
	private Combo combo_2;
	private Combo combo_1;
	private Combo combo;
	private static Text maxLon;
	private static Text maxLat;
	private static Text minLon;
	private static Text minLat;
	private static Text text;
	public static final String ID = "com.eco.bio7.worldwind.WorldWindOptionsView"; //$NON-NLS-1$
	private Composite composite;
	private ArrayList<Button> checkBoxes = new ArrayList<Button>();
	public Button visibleButton;
	private Layer layer;
	private ToolTip tip;
	private String file;
	private Composite composite_1;
	private int scalestart = 207;
	private ExpandItem newItemExpandItem_1;
	private ExpandBar expandBar;
	private LayerList layers;
	public static Composite compositeLayers;
	String[] measureTitle = { "Length", "Area", "Width", "Height", "Heading", "Center" };
	public static MeasureTool measureTool;
	private Button freehandButton;
	private int count = 0;
	private String s;
	private Button pauseButton;
	private Button newButton;
	private Button endButton;
	protected List<FileStoreDataSet> dataSets;
	public static WorldWindOptionsView optionsInstance;
	private static ArrayList<Position> LINE = new ArrayList<Position>();
	private static ArrayList<Position> PATH = new ArrayList<Position>();
	private static ArrayList<Position> POLYGON = new ArrayList<Position>();
	private long counter;
	private long start = System.currentTimeMillis();
	public static Image quad2dImage;
	private static final int IMAGE_SIZE = 512;
	private Gazetteer gazeteer;
	protected int mouseCount;
	protected boolean selectionButtonEnabled;
	private Button computeButton;
	private static org.eclipse.swt.widgets.List scrolLList;

	public static org.eclipse.swt.widgets.List getScrolLList() {
		return scrolLList;
	}

	protected Position upperLeft;
	protected Position lowerLeft;
	private ViewControlsLayer viewControlsLayer;
	protected File cacheRoot;
	private WorldWindowGLCanvas wC;
	private Text countToText;
	private Button capture;
	private Label errorLabel;
	protected ToolTip captureTip;
	private Spinner spinner;
	private BufferedImage image;
	private Button imageAlphaButton;
	protected boolean imageAlpha;
	private static int takeImagefromIJ = 0;

	// These corners do not form a Sector, so SurfaceImage must generate a
	// texture rather than simply using the source
	// image.

	static {
		LINE.add(Position.fromDegrees(44, 7, 0));
		LINE.add(Position.fromDegrees(45, 8, 0));

		PATH.addAll(LINE);
		PATH.add(Position.fromDegrees(46, 6, 0));
		PATH.add(Position.fromDegrees(47, 5, 0));
		PATH.add(Position.fromDegrees(45, 6, 0));

		POLYGON.addAll(PATH);
		POLYGON.add(Position.fromDegrees(44, 7, 0));
	}

	public WorldWindOptionsView() {
		optionsInstance = this;
	}

	/**
	 * Create contents of the view part
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		wC = WorldWindView.getWwd();

		checkBoxes.clear();
		createMeasureTool();
		if (wC != null) {
			wC.getInputHandler().addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent mouseEvent) {

					if (selectionButtonEnabled) {

						if (SwingUtilities.isRightMouseButton(mouseEvent)) {

						}
						Display display = PlatformUI.getWorkbench().getDisplay();
						final Position curPos = WorldWindView.getWwd().getCurrentPosition();

						if (curPos == null)
							return;
						/*
						 * Counts the amounts of clicks to set the data in the
						 * min max textfields!
						 */
						if (mouseCount == 1) {

							display.syncExec(new Runnable() {

								public void run() {

									minLat.setText(String.valueOf(curPos.latitude.degrees));
									minLon.setText(String.valueOf(curPos.longitude.degrees));
								}
							});

							mouseCount = 2;
						}

						else if (mouseCount == 2) {

							display.syncExec(new Runnable() {

								public void run() {
									maxLat.setText(String.valueOf(curPos.latitude.degrees));
									maxLon.setText(String.valueOf(curPos.longitude.degrees));

								}
							});

							mouseCount = 1;

						}

					}
				}

				public void mouseReleased(MouseEvent mouseEvent) {

				}

				public void mouseClicked(MouseEvent mouseEvent) {

				}
			});
		}
		this.gazeteer = new YahooGazetteer();// to resolve names!

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FillLayout());

		expandBar = new ExpandBar(container, SWT.V_SCROLL);
		expandBar.setBackground(SWTResourceManager.getColor(255, 255, 255));

		final ExpandItem newItemExpandItem_3 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_3.setHeight(330);
		newItemExpandItem_3.setText("Control");

		final Composite composite_2 = new Composite(expandBar, SWT.NONE);
		newItemExpandItem_3.setControl(composite_2);
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		String link = store.getString("WorldWindLinks");
		if (link.equals("") == false) {
			String[] links = link.split(";");

			for (int i = 0; i < links.length; i++) {
				scrolLList.add(links[i]);
			}
		}
		composite_2.setLayout(new GridLayout(1, true));

		Composite composite_8 = new Composite(composite_2, SWT.NONE);
		GridData gd_composite_8 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_composite_8.heightHint = 322;
		gd_composite_8.widthHint = 634;
		composite_8.setLayoutData(gd_composite_8);
		composite_8.setLayout(new GridLayout(3, true));

		final Button iButton = new Button(composite_8, SWT.NONE);
		GridData gd_iButton = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_iButton.heightHint = 40;
		iButton.setLayoutData(gd_iButton);
		iButton.setToolTipText("Info!");
		iButton.addSelectionListener(new SelectionAdapter() {
			private ToolTip infoTip;

			public void widgetSelected(final SelectionEvent e) {
				infoTip = new ToolTip(new Shell(), SWT.BALLOON | SWT.ICON_INFORMATION);
				infoTip.setText("Projection");

				infoTip.setMessage("Equirectangular projection \nDatum: WGS 84\nEPSG: 4326\n\n" + "Enter city or coordinates like:\n" + "Street, City\n" + "39.53, -119.816  (Reno, NV)\n" + "21 10 14 N, 86 51 0 W (Cancun)" + "\n-31¡ 59' 43\", 115¡ 45' 32\" (Perth)");
				infoTip.setVisible(true);

			}
		});
		iButton.setText("Info");

		final Button cacheButton = new Button(composite_8, SWT.NONE);
		GridData gd_cacheButton = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_cacheButton.heightHint = 40;
		cacheButton.setLayoutData(gd_cacheButton);
		cacheButton.setToolTipText("Clear the WorldWind cache");
		cacheButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent e) {
				FileStore store = new BasicDataFileStore();
				cacheRoot = store.getWriteLocation();

				Job job = new Job("Delete...") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						monitor.beginTask("Delete... ", IProgressMonitor.UNKNOWN);
						dataSets = FileStoreDataSet.getDataSets(cacheRoot);
						for (FileStoreDataSet ds : dataSets) {
							/* False - do not print the deleted folders! */
							ds.delete(false);
							monitor.worked(1);
						}

						monitor.done();
						return Status.OK_STATUS;
					}

				};

				// job.setSystem(true);
				job.schedule();

			}
		});
		cacheButton.setText("Clear Cache");

		final Button openCacheButton = new Button(composite_8, SWT.NONE);
		GridData gd_openCacheButton = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_openCacheButton.heightHint = 40;
		openCacheButton.setLayoutData(gd_openCacheButton);
		openCacheButton.setToolTipText("Open the WorldWind cache location!");
		openCacheButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				FileStore store = new BasicDataFileStore();
				File cacheRoot = store.getWriteLocation();
				DirectoryDialog dlg = new DirectoryDialog(new Shell());

				dlg.setFilterPath(cacheRoot.getAbsolutePath());

				dlg.setText("Bio7");

				dlg.setMessage("Delete Folders!");

				dlg.open();

			}
		});
		openCacheButton.setText("Open Cache");

		text = new Text(composite_8, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		text.addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(Event e) {
				flyToCoords();
			}
		});
		
				final Button setButton = new Button(composite_8, SWT.NONE);
				GridData gd_setButton = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
				gd_setButton.heightHint = 40;
				setButton.setLayoutData(gd_setButton);
				setButton.setToolTipText("Fly to the coordinates");
				setButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(final SelectionEvent e) {
						/*
						 * LatLon latLon = computeLatLonFromString(text.getText(),
						 * WorldWindView.getWwd().getModel().getGlobe());
						 * updateResult(latLon); if (latLon != null) { OrbitView view =
						 * (OrbitView) WorldWindView.getWwd().getView(); Globe globe =
						 * WorldWindView.getWwd().getModel().getGlobe();
						 * 
						 * ((BasicOrbitView) view).addPanToAnimator(new Position(latLon,
						 * 0), view.getHeading(), view.getPitch(), view.getZoom(),
						 * true); }
						 */
						flyToCoords();

					}
				});
				setButton.setText("Set");

		final Label coordinatesLabel = new Label(composite_8, SWT.NONE);
		coordinatesLabel.setText("Coordinates");
		new Label(composite_8, SWT.NONE);
		new Label(composite_8, SWT.NONE);

		scrolLList = new org.eclipse.swt.widgets.List(composite_8, SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER);
		GridData gd_scrolLList = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 4);
		gd_scrolLList.heightHint = 44;
		scrolLList.setLayoutData(gd_scrolLList);
		scrolLList.addMouseListener(new MouseListener() {

			@Override
			public void mouseDoubleClick(org.eclipse.swt.events.MouseEvent e) {
				if (scrolLList.getItemCount() > 0) {
					String pos = scrolLList.getItem(scrolLList.getSelectionIndex());
					/* Left out the name! */
					String[] p = pos.split(",");
					text.setText(p[1] + "," + p[2]);
				}
			}

			@Override
			public void mouseDown(org.eclipse.swt.events.MouseEvent e) {

			}

			@Override
			public void mouseUp(org.eclipse.swt.events.MouseEvent e) {

			}

		});

		final Button addButton = new Button(composite_8, SWT.NONE);
		GridData gd_addButton = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_addButton.heightHint = 40;
		addButton.setLayoutData(gd_addButton);
		addButton.setToolTipText("Add coordinates which should be stored");
		addButton.addSelectionListener(new SelectionAdapter() {
			IPreferenceStore store = Activator.getDefault().getPreferenceStore();

			public void widgetSelected(final SelectionEvent e) {
				InputDialog dialog = new InputDialog(new Shell(), "Enter Description", "Please enter a description!", "", new IInputValidator() {

					public String isValid(String text) {
						if (text.length() == 0) {
							return "Please enter a description!";
						}
						return null;
					}

				});

				if (dialog.open() == Window.OK) {

					Rectangle r = WorldWindView.getWwd().getBounds();
					View view = WorldWindView.getWwd().getView();

					if (view != null) {

						Position eye = view.getEyePosition();

						String eLat = String.valueOf(eye.latitude.degrees);
						String eLon = String.valueOf(eye.longitude.degrees);

						scrolLList.add(dialog.getValue() + "," + eLat + "," + eLon);

					}
					StringBuffer buff = new StringBuffer();
					for (int i = 0; i < scrolLList.getItemCount(); i++) {
						buff.append(scrolLList.getItem(i) + ";");
					}
					store.setValue("WorldWindLinks", buff.toString());
					buff = null;

				}

			}
		});
		addButton.setText("Add");

		final Button deleteButton = new Button(composite_8, SWT.NONE);
		GridData gd_deleteButton = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_deleteButton.heightHint = 40;
		deleteButton.setLayoutData(gd_deleteButton);
		deleteButton.setToolTipText("Delete stored coordinates");
		deleteButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IPreferenceStore store = Activator.getDefault().getPreferenceStore();
				if (scrolLList.getItemCount() > 0) {
					scrolLList.remove(scrolLList.getSelectionIndex());
				}
				StringBuffer buff = new StringBuffer();
				for (int i = 0; i < scrolLList.getItemCount(); i++) {
					buff.append(scrolLList.getItem(i) + ";");
				}
				store.setValue("WorldWindLinks", buff.toString());
				buff = null;

			}
		});
		deleteButton.setText("Delete");

		final Button loadButton = new Button(composite_8, SWT.NONE);
		GridData gd_loadButton = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_loadButton.heightHint = 40;
		loadButton.setLayoutData(gd_loadButton);
		loadButton.setToolTipText("Load coordinates from a *.txt file");
		loadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IPreferenceStore store = Activator.getDefault().getPreferenceStore();

				Shell s = new Shell(SWT.ON_TOP);
				FileDialog fd = new FileDialog(s, SWT.OPEN);
				fd.setText("Load");

				file = fd.open();

				File fil = new File(file);

				try {

					FileReader fileReader = new FileReader(fil);
					BufferedReader reader = new BufferedReader(fileReader);
					String str;

					while ((str = reader.readLine()) != null) {

						scrolLList.add(str);

					}
					reader.close();

				} catch (IOException ex) {

				}
				StringBuffer buff = new StringBuffer();
				for (int i = 0; i < scrolLList.getItemCount(); i++) {
					buff.append(scrolLList.getItem(i) + ";");
				}
				store.setValue("WorldWindLinks", buff.toString());
				buff = null;
			}
		});
		loadButton.setText("Load");

		final Button saveButton = new Button(composite_8, SWT.NONE);
		GridData gd_saveButton = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_saveButton.heightHint = 40;
		saveButton.setLayoutData(gd_saveButton);
		saveButton.setToolTipText("Save coordinates to a *.txt file");
		saveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				StringBuffer buffer = new StringBuffer();
				for (int i = 0; i < scrolLList.getItemCount(); i++) {
					buffer.append(scrolLList.getItem(i));
					buffer.append(System.getProperty("line.separator"));
				}
				Shell s = new Shell(SWT.ON_TOP);
				FileDialog fd = new FileDialog(s, SWT.SAVE);
				fd.setText("Save");

				String file = fd.open();
				File fil = new File(file);
				FileWriter fileWriter = null;
				try {
					fileWriter = new FileWriter(fil);

					BufferedWriter buffWriter = new BufferedWriter(fileWriter);

					String write = buffer.toString();
					buffWriter.write(write, 0, write.length());
					buffWriter.close();
				} catch (IOException ex) {

				} finally {
					try {
						fileWriter.close();
					} catch (IOException ex) {

					}
				}
			}
		});
		saveButton.setText("Save");

		final ExpandItem newItemExpandItem = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem.setHeight(1550);
		newItemExpandItem.setText("Default Layers");

		composite = new Composite(expandBar, SWT.NONE);
		newItemExpandItem.setControl(composite);
		composite.setLayout(new GridLayout(1, true));

		createLayers();
		composite.layout();
		newItemExpandItem_1 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_1.setHeight(1200);
		newItemExpandItem_1.setText("Layers");

		composite_1 = new Composite(expandBar, SWT.NONE);
		composite_1.setLayout(new FormLayout());
		newItemExpandItem_1.setControl(composite_1);

		Label latitudeLabel;

		Button removeAllButton;

		Button fromImagejButton;

		Button dynamicButton;

		Button goButton;

		Button videoButton;

		Composite composite_7 = new Composite(composite_1, SWT.NONE);
		composite_7.setLayout(new GridLayout(3, true));
		FormData fd_composite_7 = new FormData();
		fd_composite_7.top = new FormAttachment(0, 10);
		fd_composite_7.right = new FormAttachment(100, -5);
		fd_composite_7.left = new FormAttachment(0);
		composite_7.setLayoutData(fd_composite_7);
		new Label(composite_7, SWT.NONE);
		latitudeLabel = new Label(composite_7, SWT.NONE);
		GridData gd_latitudeLabel = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_latitudeLabel.heightHint = 25;
		latitudeLabel.setLayoutData(gd_latitudeLabel);
		latitudeLabel.setAlignment(SWT.CENTER);
		latitudeLabel.setText("Latitude");
		new Label(composite_7, SWT.NONE);
		new Label(composite_7, SWT.NONE);

		minLat = new Text(composite_7, SWT.BORDER);
		GridData gd_minLat = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_minLat.heightHint = 25;
		minLat.setLayoutData(gd_minLat);
		minLat.setToolTipText("Min");
		minLat.setText("50.999583");

		final Label longitudeLabel = new Label(composite_7, SWT.CENTER);
		GridData gd_longitudeLabel = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_longitudeLabel.heightHint = 25;
		longitudeLabel.setLayoutData(gd_longitudeLabel);
		longitudeLabel.setAlignment(SWT.CENTER);
		longitudeLabel.setText("Longitude");

		minLon = new Text(composite_7, SWT.BORDER);
		GridData gd_minLon = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_minLon.heightHint = 25;
		minLon.setLayoutData(gd_minLon);
		minLon.setToolTipText("Min");
		minLon.setText("9.999583");
		goButton = new Button(composite_7, SWT.NONE);
		GridData gd_goButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_goButton.heightHint = 40;
		goButton.setLayoutData(gd_goButton);
		goButton.setToolTipText("Fly to the specified area");
		goButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				double meanLat = (Double.parseDouble(minLat.getText()) + Double.parseDouble(maxLat.getText())) / 2;
				double meanLon = (Double.parseDouble(minLon.getText()) + Double.parseDouble(maxLon.getText())) / 2;
				Position p = new Position(new LatLon(Angle.fromDegrees(meanLat), Angle.fromDegrees(meanLon)), 2000);
				Coordinates.flyTo(p);

			}
		});
		goButton.setText("Go to");

		maxLon = new Text(composite_7, SWT.BORDER);
		GridData gd_maxLon = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_maxLon.heightHint = 25;
		maxLon.setLayoutData(gd_maxLon);
		maxLon.setToolTipText("Max");
		maxLon.setText("11.00042");
		new Label(composite_7, SWT.NONE);

		maxLat = new Text(composite_7, SWT.BORDER);
		GridData gd_maxLat = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_maxLat.heightHint = 25;
		maxLat.setLayoutData(gd_maxLat);
		maxLat.setToolTipText("Max");
		maxLat.setText("52.00042");
		new Label(composite_7, SWT.NONE);
		new Label(composite_7, SWT.NONE);
		new Label(composite_7, SWT.NONE);
		new Label(composite_7, SWT.NONE);

		final Button addImageLayerButton = new Button(composite_7, SWT.NONE);
		GridData gd_addImageLayerButton = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_addImageLayerButton.heightHint = 40;
		addImageLayerButton.setLayoutData(gd_addImageLayerButton);
		addImageLayerButton.setToolTipText("Add an image from a file location");
		addImageLayerButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				final String f = openFile(new String[] { "*.tif", "*" });

				if (f != null) {
					// addImages(f, new Double(minLat.getText()), new
					// Double(maxLat.getText()), new Double(minLon.getText()),
					// new Double(maxLon.getText()));
					Job job = new Job("Open Tiff") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							monitor.beginTask("Open Tiff file ...", IProgressMonitor.UNKNOWN);

							importImagery(f);

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
					// job.setSystem(true);
					job.schedule();

				}

			}
		});
		addImageLayerButton.setText("Add GeoTIFF");
		fromImagejButton = new Button(composite_7, SWT.NONE);
		GridData gd_fromImagejButton = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_fromImagejButton.heightHint = 40;
		fromImagejButton.setLayoutData(gd_fromImagejButton);
		fromImagejButton.setToolTipText("Add an active ImageJ image \nfrom the ImageJ view");
		fromImagejButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				ImagePlus imp = null;

				CanvasView canvasView = CanvasView.getCanvas_view();
				CTabItem[] items = canvasView.tabFolder.getItems();
				if (items.length > 0) {
					imp = WindowManager.getCurrentWindow().getImagePlus();

					BufferedImage image = imp.getBufferedImage();
					addBufferedImages(image, imp.getTitle(), new Double(minLat.getText()), new Double(maxLat.getText()), new Double(minLon.getText()), new Double(maxLon.getText()));
				} else {
					MessageBox messageBox = new MessageBox(new Shell(),

					SWT.ICON_WARNING);
					messageBox.setText("Info!");
					messageBox.setMessage("No image available!");
					messageBox.open();
				}

			}
		});
		fromImagejButton.setText("IJ image");
		videoButton = new Button(composite_7, SWT.NONE);
		GridData gd_videoButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_videoButton.heightHint = 40;
		videoButton.setLayoutData(gd_videoButton);
		videoButton.setToolTipText("Enables a dynamic ImageJ layer\non top of the globe");
		videoButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				addImageJDynamicImage(new Double(minLat.getText()), new Double(maxLat.getText()), new Double(minLon.getText()), new Double(maxLon.getText()));
			}
		});
		videoButton.setText("IJ Dynamic");
		dynamicButton = new Button(composite_7, SWT.NONE);
		GridData gd_dynamicButton = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_dynamicButton.heightHint = 40;
		dynamicButton.setLayoutData(gd_dynamicButton);
		dynamicButton.setToolTipText("Add a dynamic layer which executes compiled code");
		dynamicButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				addDynamiclayer();

			}
		});
		dynamicButton.setText("Dyn. Layer");
		removeAllButton = new Button(composite_7, SWT.NONE);
		GridData gd_removeAllButton = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_removeAllButton.heightHint = 40;
		removeAllButton.setLayoutData(gd_removeAllButton);
		removeAllButton.setToolTipText("Add an area to the text fields.\nThe variables have to be defined in the current R workspace");
		removeAllButton.addSelectionListener(new SelectionAdapter() {
			private REXPLogical bolExistsMaxLon;
			private REXPLogical bolExistsMinLon;
			private REXPLogical bolExistsMaxLat;
			private REXPLogical bolExistsMinLat;

			public void widgetSelected(final SelectionEvent e) {
				RConnection c = WorldWindView.getRConnection();
				if (c != null) {
					try {
						bolExistsMaxLon = (REXPLogical) c.eval("try(exists(\"maxLon\"))");
						bolExistsMinLon = (REXPLogical) c.eval("try(exists(\"minLon\"))");
						bolExistsMaxLat = (REXPLogical) c.eval("try(exists(\"maxLat\"))");
						bolExistsMinLat = (REXPLogical) c.eval("try(exists(\"minLat\"))");

						boolean[] bolEMaxLon = bolExistsMaxLon.isTrue();
						boolean[] bolEMinLon = bolExistsMinLon.isTrue();
						boolean[] bolEMaxLat = bolExistsMaxLat.isTrue();
						boolean[] bolEMinLat = bolExistsMinLat.isTrue();
						if (bolEMaxLon[0] || bolEMinLon[0] || bolEMaxLat[0] || bolEMinLat[0]) {
							double maxLo = c.eval("maxLon").asDouble();
							maxLon.setText("" + maxLo);

							double minLo = c.eval("minLon").asDouble();
							minLon.setText("" + minLo);

							double maxLa = c.eval("maxLat").asDouble();
							maxLat.setText("" + maxLa);

							double minLa = c.eval("minLat").asDouble();
							minLat.setText("" + minLa);
						} else {
							MessageBox messageBox = new MessageBox(new Shell(),

							SWT.ICON_WARNING);
							messageBox.setText("Info!");
							messageBox.setMessage("Lat-Lon variables are not available\n" + "in R workspace!");
							messageBox.open();
						}

					} catch (REXPMismatchException e1) {

						e1.printStackTrace();
					} catch (RserveException e1) {

						e1.printStackTrace();
					}

				} else {
					MessageBox messageBox = new MessageBox(new Shell(),

					SWT.ICON_WARNING);
					messageBox.setText("Info!");
					messageBox.setMessage("Rserve is not Alive!");
					messageBox.open();

				}
			}
		});
		removeAllButton.setText("Get Area");

		computeButton = new Button(composite_7, SWT.NONE);
		GridData gd_computeButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_computeButton.heightHint = 40;
		computeButton.setLayoutData(gd_computeButton);
		computeButton.setToolTipText("Computes coordinates for the Lat, Lon \ntextfields alternately (min, max!)");
		computeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				mouseCount = 1;
				if (selectionButtonEnabled) {
					if (wC != null) {
						((Component) wC).setCursor(Cursor.getDefaultCursor());
					}
					selectionButtonEnabled = false;

					for (int j = 0; j < layers.size(); j++) {

						if (j < 14) {
							layers.get(j).setPickEnabled(false);

						}

					}

					computeButton.setText("Compute");
					/* SWT Text Color Buttons on Windows not working! */
					// computeButton.setForeground(new
					// org.eclipse.swt.graphics.Color(Display.getCurrent(),255,255,255));
				} else if (selectionButtonEnabled == false) {
					if (wC != null) {
						((Component) wC).setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
					}

					computeButton.setText("Armed");
					/* SWT Text Color Buttons on Windows not working! */
					// computeButton.setForeground(new
					// org.eclipse.swt.graphics.Color(Display.getCurrent(),155,155,155));
					layers = WorldWindView.getWwd().getModel().getLayers();
					for (int j = 0; j < layers.size(); j++) {
						// System.out.println(j+": "+layers.get(j).getName());
						if (j < 11) {
							layers.get(j).setPickEnabled(true);
						}

					}
					selectionButtonEnabled = true;
				}
				/*
				 * Rectangle r=WorldWindView.getWwd().getBounds(); View view =
				 * WorldWindView.getWwd().getView();
				 * 
				 * Position upperLeft; Position lowerLeft; if(view!=null){
				 * upperLeft = view.computePositionFromScreenPoint(0, 0);
				 * lowerLeft = view.computePositionFromScreenPoint(r.width,
				 * r.height);
				 * 
				 * 
				 * 
				 * 
				 * 
				 * 
				 * minLat.setText(String.valueOf(upperLeft.latitude.degrees)) ;
				 * minLon.setText(String.valueOf(upperLeft.longitude.degrees ));
				 * maxLat.setText(String.valueOf(lowerLeft.latitude.degrees ));
				 * maxLon.setText(String.valueOf(lowerLeft.longitude.degrees ));
				 * //System.out.println(r.x+" "+r.y+" "+r.width+" "+ r.height);
				 * System.out.println(upperLeft.latitude);
				 */

			}
		});
		computeButton.setText("Compute");

		Button btnNewButton = new Button(composite_7, SWT.NONE);
		GridData gd_btnNewButton = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_btnNewButton.heightHint = 40;
		btnNewButton.setLayoutData(gd_btnNewButton);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final String f = openFile(new String[] { "*.shp", "*" });

				if (f != null) {
					LoadShapefileJob shapeFileJob = new LoadShapefileJob(f, compositeLayers);
					shapeFileJob.schedule();
				}

			}
		});
		btnNewButton.setText("Add Shapefile");

		imageAlphaButton = new Button(composite_7, SWT.CHECK);
		imageAlphaButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		imageAlphaButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (imageAlphaButton.getSelection()) {
					image = null;
					imageAlpha = true;

				} else {
					image = null;
					imageAlpha = false;
				}
			}
		});
		imageAlphaButton.setToolTipText("If this button is checked image\r\npixels with value 0 (RGB, greyscale, float, etc.) \r\nwill rendered transparent.");
		imageAlphaButton.setText("Image Alpha");

		final Spinner spinner_1 = new Spinner(composite_7, SWT.BORDER);
		spinner_1.setToolTipText("Select the layer (image tab) which will be rendered to ImageJ.\r\nThis action allows the rendering and simulation simultanously\r\nwith WorldWind and ImageJ.");
		GridData gd_spinner_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_spinner_1.heightHint = 22;
		spinner_1.setLayoutData(gd_spinner_1);

		compositeLayers = new Composite(composite_1, SWT.NONE);
		fd_composite_7.bottom = new FormAttachment(compositeLayers, -6);
		compositeLayers.setLayout(new GridLayout(1, true));
		FormData fd_composite_4 = new FormData();
		fd_composite_4.bottom = new FormAttachment(100, -10);
		fd_composite_4.top = new FormAttachment(0, 342);
		fd_composite_4.right = new FormAttachment(100, -5);
		fd_composite_4.left = new FormAttachment(0);
		compositeLayers.setLayoutData(fd_composite_4);
		compositeLayers.setSize(305, 203);
		spinner_1.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {

				takeImagefromIJ = spinner_1.getSelection();
			}
		});

		final ExpandItem newItemExpandItem_2 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_2.setHeight(200);
		newItemExpandItem_2.setText("Render");

		final Composite composite_3 = new Composite(expandBar, SWT.NONE);
		newItemExpandItem_2.setControl(composite_3);
		composite_3.setLayout(new GridLayout(1, true));

		Composite composite_9 = new Composite(composite_3, SWT.NONE);
		GridData gd_composite_9 = new GridData(SWT.FILL, SWT.CENTER, true, true, 3, 1);
		gd_composite_9.heightHint = 150;
		gd_composite_9.widthHint = 215;
		composite_9.setLayoutData(gd_composite_9);
		composite_9.setLayout(new GridLayout(3, true));

		capture = new Button(composite_9, SWT.NONE);
		GridData gd_capture = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_capture.heightHint = 40;
		capture.setLayoutData(gd_capture);
		capture.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				int countTo = Integer.parseInt(countToText.getText());

				ScreenRecording screen = new ScreenRecording(capture);
				if (screen.doCapture() == false) {
					capture.setText("Rec.");
					screen.setCount(countTo);
					screen.setFps(spinner.getSelection());
					screen.setupCaptureImages();
					screen.setCapture(true);
					screen.docaptureAnimationJob();
				}

				else {
					screen.setCapture(false);
					capture.setText("Capture");
				}

			}
		});
		capture.setText("Capture");

		countToText = new Text(composite_9, SWT.BORDER);
		countToText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		countToText.setText("100");

		Label lblNewLabel = new Label(composite_9, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		lblNewLabel.setText("Frames");

		final Button screenshotButton = new Button(composite_9, SWT.NONE);
		GridData gd_screenshotButton = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_screenshotButton.heightHint = 40;
		screenshotButton.setLayoutData(gd_screenshotButton);
		screenshotButton.setToolTipText("Creates a screenshot image in ImageJ");
		screenshotButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {

					public void run() {

						ScreenRecording screen = new ScreenRecording(capture);
						ImagePlus ip = screen.captureImage();
						ip.show();

						// WorldWindowGLAutoDrawable.screenshot = true;

					}
				});

			}
		});
		screenshotButton.setText("Screenshot");

		spinner = new Spinner(composite_9, SWT.BORDER);
		spinner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		spinner.setMaximum(60);
		spinner.setMinimum(1);
		spinner.setSelection(25);
		spinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});

		Label lblFps = new Label(composite_9, SWT.NONE);
		lblFps.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		lblFps.setText("FPS");

		Button infoCaptureButton = new Button(composite_9, SWT.NONE);
		GridData gd_infoCaptureButton = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_infoCaptureButton.heightHint = 40;
		infoCaptureButton.setLayoutData(gd_infoCaptureButton);
		infoCaptureButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				captureTip = new ToolTip(new Shell(), SWT.BALLOON | SWT.ICON_INFORMATION);
				captureTip.setText("Info");

				captureTip.setMessage("" + "If the globe is resized within the capture mode the recording\n" + "is interrupted!\n" + "The captured images are added to an ImageJ stack which\n" + "can be saved e.g. as an *.avi or animated *.gif file.");
				captureTip.setVisible(true);
			}
		});
		infoCaptureButton.setText("Info");

		errorLabel = new Label(composite_9, SWT.NONE);
		errorLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 2));
		new Label(composite_9, SWT.NONE);
		countToText.addListener(SWT.Modify, new Listener() {
			private Integer value;

			public void handleEvent(Event event) {

				try {

					value = new Integer(countToText.getText());
					countToText.setForeground(new org.eclipse.swt.graphics.Color(Display.getCurrent(), 0, 0, 0));
					errorLabel.setText("");
				} catch (Exception e) {
					countToText.setForeground(new org.eclipse.swt.graphics.Color(Display.getCurrent(), 255, 0, 0));
					value = new Integer(0);
					errorLabel.setText("Number input incorrect!");
				}
			}

		});

		final ExpandItem newItemExpandItem_5 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_5.setHeight(600);
		newItemExpandItem_5.setText("Measure");

		final Composite composite_6 = new Composite(expandBar, SWT.NONE);
		newItemExpandItem_5.setControl(composite_6);
		composite_6.setLayout(new GridLayout(3, true));

		final Button lineButton = new Button(composite_6, SWT.NONE);
		GridData gd_lineButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lineButton.heightHint = 40;
		lineButton.setLayoutData(gd_lineButton);
		lineButton.setToolTipText("Colour for the line(s)!");
		lineButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Shell shell = new Shell();
				ColorDialog dialog = new ColorDialog(shell, SWT.APPLICATION_MODAL);

				RGB color = dialog.open();
				Color c = null;
				if (color != null) {
					c = new Color(new Integer(color.red), new Integer(color.green), new Integer(color.blue));
				}

				if (c != null) {

					measureTool.setLineColor(c);
					Color fill = new Color(c.getRed() / 255f * .5f, c.getGreen() / 255f * .5f, c.getBlue() / 255f * .5f, .5f);
					measureTool.setFillColor(fill);
				}

			}
		});
		lineButton.setText("Coulor Line");

		final Button pointsButton = new Button(composite_6, SWT.NONE);
		GridData gd_pointsButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_pointsButton.heightHint = 40;
		pointsButton.setLayoutData(gd_pointsButton);
		pointsButton.setToolTipText("Colour for the points!");
		pointsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Shell shell = new Shell();
				ColorDialog dialog = new ColorDialog(shell, SWT.APPLICATION_MODAL);

				RGB color = dialog.open();
				Color c = null;
				if (color != null) {
					c = new Color(new Integer(color.red), new Integer(color.green), new Integer(color.blue));
				}

				if (c != null) {

					measureTool.getControlPointsAttributes().setBackgroundColor(c);
				}

			}
		});
		pointsButton.setText("Colour Points");

		final Button tooltipButton = new Button(composite_6, SWT.NONE);
		GridData gd_tooltipButton = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_tooltipButton.heightHint = 40;
		tooltipButton.setLayoutData(gd_tooltipButton);
		tooltipButton.setToolTipText("Colour for the tooltips!");
		tooltipButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				Shell shell = new Shell();
				ColorDialog dialog = new ColorDialog(shell, SWT.APPLICATION_MODAL);

				RGB color = dialog.open();
				Color c = null;
				if (color != null) {
					c = new Color(new Integer(color.red), new Integer(color.green), new Integer(color.blue));
				}

				if (c != null) {

					measureTool.getAnnotationAttributes().setTextColor(c);
				}

			}
		});
		tooltipButton.setText("Colour Tooltip");

		Label label = new Label(composite_6, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gd_label = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_label.widthHint = 133;
		label.setLayoutData(gd_label);
		label.setText("Colours");

		final Label coloursLabel = new Label(composite_6, SWT.NONE);
		coloursLabel.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, true, 1, 1));
		coloursLabel.setAlignment(SWT.CENTER);
		coloursLabel.setText("Colours");

		final Label lblColours = new Label(composite_6, SWT.SEPARATOR | SWT.HORIZONTAL);
		lblColours.setText("Colours");
		GridData gd_lblColours = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_lblColours.widthHint = 133;
		lblColours.setLayoutData(gd_lblColours);
		new Label(composite_6, SWT.NONE);
		new Label(composite_6, SWT.NONE);
		new Label(composite_6, SWT.NONE);

		final Label shapeLabel = new Label(composite_6, SWT.NONE);
		shapeLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		shapeLabel.setText("Shape:");
		new Label(composite_6, SWT.NONE);

		combo_1 = new Combo(composite_6, SWT.READ_ONLY);
		combo_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		combo_1.setToolTipText("Selects the measure type!");
		combo_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				String item = combo_1.getItem(combo_1.getSelectionIndex());
				if (item.equals("Line"))
					measureTool.setMeasureShapeType(MeasureTool.SHAPE_LINE);
				else if (item.equals("Path"))
					measureTool.setMeasureShapeType(MeasureTool.SHAPE_PATH);
				else if (item.equals("Polygon"))
					measureTool.setMeasureShapeType(MeasureTool.SHAPE_POLYGON);
				else if (item.equals("Circle"))
					measureTool.setMeasureShapeType(MeasureTool.SHAPE_CIRCLE);
				else if (item.equals("Ellipse"))
					measureTool.setMeasureShapeType(MeasureTool.SHAPE_ELLIPSE);
				else if (item.equals("Square"))
					measureTool.setMeasureShapeType(MeasureTool.SHAPE_SQUARE);
				else if (item.equals("Rectangle"))
					measureTool.setMeasureShapeType(MeasureTool.SHAPE_QUAD);

			}
		});
		combo_1.setItems(new String[] { "Line", "Path", "Polygon", "Circle", "Ellipse", "Square", "Rectangle" });
		combo_1.select(0);

		final Label pathTypeLabel = new Label(composite_6, SWT.NONE);
		pathTypeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		pathTypeLabel.setText("Path type:");
		new Label(composite_6, SWT.NONE);

		combo_2 = new Combo(composite_6, SWT.READ_ONLY);
		combo_2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		combo_2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				String item = combo_2.getItem(combo_2.getSelectionIndex());
				if (item.equals("Linear"))
					measureTool.setPathType(AVKey.LINEAR);
				else if (item.equals("Rhumb"))
					measureTool.setPathType(AVKey.RHUMB_LINE);
				else if (item.equals("Great circle"))
					measureTool.setPathType(AVKey.GREAT_CIRCLE);
			}
		});
		combo_2.setItems(new String[] { "Linear", "Rhumb", "Great circle" });
		combo_2.select(0);

		final Label unitsLabel = new Label(composite_6, SWT.NONE);
		unitsLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		unitsLabel.setText("Units:");
		new Label(composite_6, SWT.NONE);

		combo_3 = new Combo(composite_6, SWT.READ_ONLY);
		GridData gd_combo_3 = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_combo_3.heightHint = 39;
		combo_3.setLayoutData(gd_combo_3);
		combo_3.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				String item = combo_3.getItem(combo_3.getSelectionIndex());
				if (item.equals("M/M\u00b2")) {
					measureTool.getUnitsFormat().setLengthUnits(UnitsFormat.METERS);
					measureTool.getUnitsFormat().setAreaUnits(UnitsFormat.SQUARE_METERS);
				} else if (item.equals("KM/KM\u00b2")) {
					measureTool.getUnitsFormat().setLengthUnits(UnitsFormat.KILOMETERS);
					measureTool.getUnitsFormat().setAreaUnits(UnitsFormat.SQUARE_KILOMETERS);
				} else if (item.equals("KM/Hectare")) {
					measureTool.getUnitsFormat().setLengthUnits(UnitsFormat.KILOMETERS);
					measureTool.getUnitsFormat().setAreaUnits(UnitsFormat.HECTARE);
				} else if (item.equals("Feet/Feet\u00b2")) {
					measureTool.getUnitsFormat().setLengthUnits(UnitsFormat.FEET);
					measureTool.getUnitsFormat().setAreaUnits(UnitsFormat.SQUARE_FEET);
				} else if (item.equals("Miles/Miles\u00b2")) {
					measureTool.getUnitsFormat().setLengthUnits(UnitsFormat.MILES);
					measureTool.getUnitsFormat().setAreaUnits(UnitsFormat.SQUARE_MILES);
				} else if (item.equals("Nm/Miles\u00b2")) {
					measureTool.getUnitsFormat().setLengthUnits(UnitsFormat.NAUTICAL_MILES);
					measureTool.getUnitsFormat().setAreaUnits(UnitsFormat.SQUARE_MILES);
				} else if (item.equals("Yards/Acres")) {
					measureTool.getUnitsFormat().setLengthUnits(UnitsFormat.YARDS);
					measureTool.getUnitsFormat().setAreaUnits(UnitsFormat.ACRE);
				}

			}
		});
		combo_3.setItems(new String[] { "M/M^2", "KM/KM^2", "KM/Hectare", "Feet/Feet^2", "Miles/Miles^2", "Nm/Miles^2", "Yards/Acres" });
		combo_3.select(0);

		final Label angleFormatLabel = new Label(composite_6, SWT.NONE);
		angleFormatLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		angleFormatLabel.setText("Angle Format:");
		new Label(composite_6, SWT.NONE);

		combo_4 = new Combo(composite_6, SWT.READ_ONLY);
		GridData gd_combo_4 = new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1);
		gd_combo_4.heightHint = 40;
		combo_4.setLayoutData(gd_combo_4);
		combo_4.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				String item = combo_4.getItem(combo_4.getSelectionIndex());
				measureTool.getUnitsFormat().setShowDMS(item.equals("DMS"));
			}
		});
		combo_4.setItems(new String[] { "DD", "DMS" });
		combo_4.select(0);
		new Label(composite_6, SWT.NONE);
		new Label(composite_6, SWT.NONE);
		new Label(composite_6, SWT.NONE);

		final Button followTerrainButton = new Button(composite_6, SWT.CHECK);
		followTerrainButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		followTerrainButton.setToolTipText("If enabled lines, circles or \nsegments will follow the terrain!");
		followTerrainButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				measureTool.setFollowTerrain(followTerrainButton.getSelection());
				if (wC != null) {
					wC.redraw();
				}
			}
		});
		followTerrainButton.setText("Follow terrain");

		final Button controlPointsButton = new Button(composite_6, SWT.CHECK);
		controlPointsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		controlPointsButton.setToolTipText("Enables or disables the control points!");
		controlPointsButton.setSelection(true);
		controlPointsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				measureTool.setShowControlPoints(controlPointsButton.getSelection());
				if (wC != null) {
					wC.redraw();
				}
			}
		});
		controlPointsButton.setText("Control points");
		new Label(composite_6, SWT.NONE);

		final Button rubberBandButton = new Button(composite_6, SWT.CHECK);
		rubberBandButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		rubberBandButton.setToolTipText("Enables or disables the \"Rubber band\" option!");
		rubberBandButton.setSelection(true);
		rubberBandButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				measureTool.getController().setUseRubberBand(rubberBandButton.getSelection());
				freehandButton.setEnabled(rubberBandButton.getSelection());
				if (wC != null) {
					wC.redraw();
				}
			}
		});
		rubberBandButton.setText("Rubber band");

		freehandButton = new Button(composite_6, SWT.CHECK);
		freehandButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		freehandButton.setToolTipText("Enables or disables freehand selections!");
		freehandButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				measureTool.getController().setFreeHand(freehandButton.getSelection());
				if (wC != null) {
					wC.redraw();
				}

			}
		});
		freehandButton.setText("Free Hand");
		new Label(composite_6, SWT.NONE);

		final Button tooltipButton_1 = new Button(composite_6, SWT.CHECK);
		tooltipButton_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 1));
		tooltipButton_1.setToolTipText("Enables or disables the tooltips!");
		tooltipButton_1.setSelection(true);
		tooltipButton_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				measureTool.setShowAnnotation(tooltipButton_1.getSelection());
				if (wC != null) {
					wC.redraw();
				}
			}
		});
		tooltipButton_1.setText("Tooltip");
		new Label(composite_6, SWT.NONE);
		new Label(composite_6, SWT.NONE);
		new Label(composite_6, SWT.NONE);
		new Label(composite_6, SWT.NONE);
		new Label(composite_6, SWT.NONE);

		newButton = new Button(composite_6, SWT.NONE);
		newButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		newButton.setToolTipText("Start a new measurement!");
		newButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				Display display = PlatformUI.getWorkbench().getDisplay();
				display.syncExec(new Runnable() {

					public void run() {
						new TableItem(table, SWT.NONE);
					}
				});
				SwingUtilities.invokeLater(new Runnable() {
					// !!
					public void run() {
						measureTool.clear();
						measureTool.setArmed(true);
					}
				});

			}
		});
		newButton.setText("New Measur.");
		newButton.setEnabled(true);

		pauseButton = new Button(composite_6, SWT.NONE);
		pauseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		pauseButton.setToolTipText("Edit a measurement!");
		pauseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				measureTool.setArmed(!measureTool.isArmed());
				pauseButton.setText(!measureTool.isArmed() ? "Resume" : "Pause");
				pauseButton.setEnabled(true);
				if (wC != null) {
					((Component) wC).setCursor(!measureTool.isArmed() ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				}
			}
		});
		pauseButton.setEnabled(false);
		pauseButton.setText("Edit");

		endButton = new Button(composite_6, SWT.NONE);
		endButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		endButton.setToolTipText("Stop a measurement!");
		endButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				measureTool.setArmed(false);
			}
		});
		endButton.setText("End");
		endButton.setEnabled(false);

		table = new Table(composite_6, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL);
		GridData gd_table = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 3);
		gd_table.heightHint = 99;
		table.setLayoutData(gd_table);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);

		new TableItem(table, SWT.NONE);

		if (measureTool != null) {
			rubberBandButton.setSelection(measureTool.getController().isUseRubberBand());
		}
		if (measureTool != null) {
			freehandButton.setSelection(measureTool.getController().isFreeHand());
		}

		final Button toSpreadButton = new Button(composite_6, SWT.NONE);
		GridData gd_toSpreadButton = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_toSpreadButton.heightHint = 40;
		toSpreadButton.setLayoutData(gd_toSpreadButton);
		toSpreadButton.setToolTipText("Adds the current measurments to a dataframe.\nDelete the dataframe to start a new measurement series!");
		toSpreadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				RConnection con = WorldWindView.getRConnection();
				if (con != null) {

					String val1 = table.getItem(0).getText(0);
					String val2 = table.getItem(0).getText(1);
					String val3 = table.getItem(0).getText(2);
					String val4 = table.getItem(0).getText(3);
					String val5 = table.getItem(0).getText(4);
					String val6 = table.getItem(0).getText(5);
					String[] v = { val1, val2, val3, val4, val5, val6 };

					try {
						con.assign("meas", v);
					} catch (REngineException e1) {

						e1.printStackTrace();
					}
					try {

						con.eval("df<-cbind(df,meas)");
						con.eval("df[,1]<-c('Length','Area','Width','Height','Heading','Center')");
					} catch (RserveException e1) {

						e1.printStackTrace();
					}
					MessageBox messageBox = new MessageBox(new Shell(),

					SWT.ICON_INFORMATION);
					messageBox.setText("Info!");
					messageBox.setMessage("Value(s) transferred!");
					messageBox.open();

				} else {
					MessageBox messageBox = new MessageBox(new Shell(),

					SWT.ICON_WARNING);
					messageBox.setText("Info!");
					messageBox.setMessage("Rserve is not Alive!");
					messageBox.open();

				}

			}
		});
		toSpreadButton.setText("Values->R");
		new Label(composite_6, SWT.NONE);
		new Label(composite_6, SWT.NONE);
		Listener sortListener = new Listener() {
			public void handleEvent(Event e) {
				GridColumn column = (GridColumn) e.widget;

			}
		};
		for (int i = 0; i < measureTitle.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.addListener(SWT.Selection, sortListener);
			column.setText(measureTitle[i]);
			column.setWidth(70);

		}

		final ExpandItem newItemExpandItem_4 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_4.setHeight(100);
		newItemExpandItem_4.setText("Projection");

		final Composite composite_5 = new Composite(expandBar, SWT.NONE);
		newItemExpandItem_4.setControl(composite_5);
		composite_5.setLayout(new GridLayout(3, true));

		final Button flatButton = new Button(composite_5, SWT.CHECK);
		flatButton.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false, 1, 1));
		flatButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (WorldWindView.isFlatProjection()) {
					WorldWindView.getInstance().setFlatProjection(false);
				} else {
					WorldWindView.getInstance().setFlatProjection(true);
				}
			}
		});
		flatButton.setText("Flat");

		combo = new Combo(composite_5, SWT.READ_ONLY);
		GridData gd_combo = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_combo.heightHint = 37;
		combo.setLayoutData(gd_combo);
		combo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				WorldWindView.setProjectionMode(combo.getSelectionIndex());

				if (flatButton.getSelection()) {
					WorldWindView.getInstance().setFlatProjection(false);
					WorldWindView.getInstance().setFlatProjection(true);
				}
			}
		});
		combo.setItems(new String[] { "Lat-Lon", "Mercator", "Modified-Sinosidal", "Sinodisal" });
		combo.select(0);
		new Label(composite_5, SWT.NONE);
		new Label(composite_5, SWT.NONE);
		new Label(composite_5, SWT.NONE);
		new Label(composite_5, SWT.NONE);
		//
		createActions();
		initializeToolBar();
		initializeMenu();

	}

	public void dispose() {

		super.dispose();

	}

	public void createMeasureTool() {

		if (wC != null) {
			measureTool = new MeasureTool(wC);
			measureTool.setController(new MeasureToolController());
			measureTool.addPropertyChangeListener(new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					// Add, remove or change positions
					if (event.getPropertyName().equals(MeasureTool.EVENT_POSITION_ADD) || event.getPropertyName().equals(MeasureTool.EVENT_POSITION_REMOVE) || event.getPropertyName().equals(MeasureTool.EVENT_POSITION_REPLACE)) {

					}
					// The tool was armed / disarmed
					else if (event.getPropertyName().equals(MeasureTool.EVENT_ARMED)) {
						if (measureTool.isArmed()) {
							Display display = PlatformUI.getWorkbench().getDisplay();
							display.syncExec(new Runnable() {

								public void run() {
									newButton.setEnabled(false);
									pauseButton.setText("Pause");
									pauseButton.setEnabled(true);
									endButton.setEnabled(true);
								}
							});
							((Component) wC).setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
						} else {
							Display display = PlatformUI.getWorkbench().getDisplay();
							display.syncExec(new Runnable() {

								public void run() {
									newButton.setEnabled(true);
									pauseButton.setText("Pause");
									pauseButton.setEnabled(false);
									endButton.setEnabled(false);
								}
							});
							((Component) wC).setCursor(Cursor.getDefaultCursor());
						}

					}

					// Metric changed - sent after each render frame
					/*
					 * With syncExec error occurred after perspective
					 * switch!!!!!
					 */
					else if (event.getPropertyName().equals(MeasureTool.EVENT_METRIC_CHANGED)) {
						Display display = PlatformUI.getWorkbench().getDisplay();
						display.asyncExec(new Runnable() {

							public void run() {
								if (table.isDisposed() == false) {
									updateMetric();
								}
							}
						});

					}

				}
			});
		}
	}

	public static void setMaxLon(final String max) {
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				maxLon.setText(max);
			}
		});
	}

	public static void setMinLon(final String min) {
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				minLon.setText(min);
			}
		});
	}

	public static void setMaxLat(final String max) {
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				maxLat.setText(max);
			}
		});
	}

	public static void setMinLat(final String min) {
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {

				minLat.setText(min);
			}
		});
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
		// Set the focus
	}

	public void createLayers() {

		int ystart = 38;
		if (wC != null) {
			layers = wC.getModel().getLayers();
			// Add view controls layer and select listener
			viewControlsLayer = new ViewControlsLayer();
			insertBeforePlacenames(wC, viewControlsLayer);
			viewControlsLayer.setEnabled(false);

			LatLonGraticuleLayer gratLatLon = new LatLonGraticuleLayer();
			gratLatLon.setEnabled(false);
			insertBeforePlacenames(wC, gratLatLon);

			MGRSGraticuleLayer mgrsLayer = new MGRSGraticuleLayer();
			mgrsLayer.setEnabled(false);
			insertBeforePlacenames(WorldWindView.getWwd(), mgrsLayer);

			wC.addSelectListener(new ViewControlsSelectListener(wC, viewControlsLayer));
			for (int i = 0; i < layers.size(); i++) {
				final Layer layer = layers.get(i);
				// System.out.println(layer.getName());
				checkBoxes.add(new Button(composite, SWT.CHECK));
				final Button b = checkBoxes.get(i);
				GridData gd_btnCheckButton = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
				gd_btnCheckButton.heightHint = 40;
				b.setLayoutData(gd_btnCheckButton);
				b.setText(layer.getName());
				if (layer.getName().equals("Lat-Lon Graticule")) {
					b.setSelection(false);
				} else if (layer.getName().equals("MGRS Graticule")) {
					b.setSelection(false);

				} else if (layer.getName().equals("View Controls")) {
					b.setSelection(false);

				} else if (layer.getName().equals("Compass")) {
					b.setSelection(true);
				} else if (layer.getName().equals("World Map")) {
					b.setSelection(true);
				} else if (layer.getName().equals("Scale Bar")) {
					b.setSelection(true);
				}

				else if (layer.getName().equals("OpenStreetMap")) {

					layer.setEnabled(false);
					b.setSelection(false);
				}

				else if (layer.getName().equals("MS Virtual Earth Aerial")) {
					b.setSelection(true);
				} else if (layer.getName().equals("MS Virtual Earth Hybrid")) {
					b.setSelection(false);
				} else if (layer.getName().equals("Bing Imagery")) {
					b.setSelection(false);

				} else if (layer.getName().equals("MS Virtual Earth Roads")) {
					b.setSelection(false);
				} else if (layer.getName().equals("USDA NAIP")) {
					layer.setEnabled(false);
					b.setSelection(false);
				} else if (layer.getName().equals("USGS Urban Area")) {
					layer.setEnabled(false);
					b.setSelection(false);
				} else if (layer.getName().equals("Place Names")) {
					layer.setEnabled(false);
					b.setSelection(false);
				} else if (layer.getName().equals("Political Boundaries")) {
					layer.setEnabled(false);
					b.setSelection(false);
				}

				else {

					b.setSelection(true);
				}
				b.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(final SelectionEvent e) {

						if (b.getSelection()) {
							layer.setEnabled(true);

						} else {
							layer.setEnabled(false);

						}

						wC.redraw();

					}
				});

				if (layer.getName().equals("MS Virtual Earth Aerial") || layer.getName().equals("Bing Imagery") || layer.getName().equals("MS Virtual Earth Hybrid") || layer.getName().equals("MS Virtual Earth Roads") || layer.getName().equals("OpenStreetMap") || layer.getName().equals("USGS Urban Area") || layer.getName().equals("USDA NAIP") || layer.getName().equals("i-cubed Landsat")
						|| layer.getName().equals("Blue Marble (WMS) 2004") || layer.getName().equals("NASA Blue Marble Image") || layer.getName().equals("USGS Urban Area Ortho")) {

					final Scale scale = new Scale(composite, SWT.NONE);
					scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
					scale.setMinimum(1);
					scale.setMaximum(100);
					scale.setSelection(100);
					scale.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(final SelectionEvent e) {
							layer.setOpacity(((double) scale.getSelection()) / 100);

							wC.redraw();

						}
					});

					//scale.setBounds(10, ystart + 30, 200, 40);
				}
				//b.setBounds(10, ystart, 200, 40);
				//ystart = ystart + 65;
			}
		}
	}

	private void updateResult(LatLon latLon) {
		tip = new ToolTip(new Shell(), SWT.BALLOON | SWT.ICON_INFORMATION);
		tip.setText("Coordinates");

		if (latLon != null) {
			text.setText(text.getText().toUpperCase());

			tip.setMessage(String.format("Lat %7.4f\u00B0 Lon %7.4f\u00B0", latLon.getLatitude().degrees, latLon.getLongitude().degrees));
			tip.setVisible(true);

		} else
			tip.setMessage("Invalid coordinates");
		tip.setVisible(true);

	}

	/**
	 * Tries to extract a latitude and a longitude from the given text string.
	 * 
	 * @param coordString
	 *            the input string.
	 * @param globe
	 *            the current <code>Globe</code>.
	 * @return the corresponding <code>LatLon</code> or <code>null</code>.
	 */
	private static LatLon computeLatLonFromString(String coordString, Globe globe) {
		if (coordString == null) {
			String msg = Logging.getMessage("nullValue.StringIsNull");
			Logging.logger().severe(msg);
			throw new IllegalArgumentException(msg);
		}

		Angle lat = null;
		Angle lon = null;
		coordString = coordString.trim();
		String regex;
		String separators = "(\\s*|,|,\\s*)";
		Pattern pattern;
		Matcher matcher;

		// Try MGRS - allow spaces
		regex = "\\d{1,2}[A-Za-z]\\s*[A-Za-z]{2}\\s*\\d{1,5}\\s*\\d{1,5}";
		if (coordString.matches(regex)) {
			try {
				MGRSCoord MGRS = MGRSCoord.fromString(coordString, globe);
				// NOTE: the MGRSCoord does not always report errors with
				// invalide strings,
				// but will have lat and lon set to zero
				if (MGRS.getLatitude().degrees != 0 || MGRS.getLatitude().degrees != 0) {
					lat = MGRS.getLatitude();
					lon = MGRS.getLongitude();
				} else
					return null;
			} catch (IllegalArgumentException e) {
				return null;
			}
		}

		// Try to extract a pair of signed decimal values separated by a space,
		// ',' or ', '
		// Allow E, W, S, N sufixes
		if (lat == null || lon == null) {
			regex = "([-|\\+]?\\d+?(\\.\\d+?)??\\s*[N|n|S|s]??)";
			regex += separators;
			regex += "([-|\\+]?\\d+?(\\.\\d+?)??\\s*[E|e|W|w]??)";
			pattern = Pattern.compile(regex);
			matcher = pattern.matcher(coordString);
			if (matcher.matches()) {
				String sLat = matcher.group(1).trim(); // Latitude
				int signLat = 1;
				char suffix = sLat.toUpperCase().charAt(sLat.length() - 1);
				if (!Character.isDigit(suffix)) {
					signLat = suffix == 'N' ? 1 : -1;
					sLat = sLat.substring(0, sLat.length() - 1);
					sLat = sLat.trim();
				}

				String sLon = matcher.group(4).trim(); // Longitude
				int signLon = 1;
				suffix = sLon.toUpperCase().charAt(sLon.length() - 1);
				if (!Character.isDigit(suffix)) {
					signLon = suffix == 'E' ? 1 : -1;
					sLon = sLon.substring(0, sLon.length() - 1);
					sLon = sLon.trim();
				}

				lat = Angle.fromDegrees(Double.parseDouble(sLat) * signLat);
				lon = Angle.fromDegrees(Double.parseDouble(sLon) * signLon);
			}
		}

		// Try to extract two degrees minute seconds blocks separated by a
		// space, ',' or ', '
		// Allow S, N, W, E suffixes and signs.
		// eg: -123° 34' 42" +45° 12' 30"
		// eg: 123° 34' 42"S 45° 12' 30"W
		if (lat == null || lon == null) {
			regex = "([-|\\+]?\\d{1,3}[d|D|\u00B0|\\s](\\s*\\d{1,2}['|\u2019|\\s])?(\\s*\\d{1,2}[\"|\u201d])?\\s*[N|n|S|s]?)";
			regex += separators;
			regex += "([-|\\+]?\\d{1,3}[d|D|\u00B0|\\s](\\s*\\d{1,2}['|\u2019|\\s])?(\\s*\\d{1,2}[\"|\u201d])?\\s*[E|e|W|w]?)";
			pattern = Pattern.compile(regex);
			matcher = pattern.matcher(coordString);
			if (matcher.matches()) {
				lat = parseDMSString(matcher.group(1));
				lon = parseDMSString(matcher.group(5));
			}
		}

		if (lat == null || lon == null)
			return null;

		if (lat.degrees >= -90 && lat.degrees <= 90 && lon.degrees >= -180 && lon.degrees <= 180)
			return new LatLon(lat, lon);

		return null;
	}

	/**
	 * Parse a Degrees, Minute, Second coordinate string.
	 * 
	 * @param dmsString
	 *            the string to parse.
	 * @return the corresponding <code>Angle</code> or null.
	 */
	private static Angle parseDMSString(String dmsString) {
		// Replace degree, min and sec signs with space
		dmsString = dmsString.replaceAll("[D|d|\u00B0|'|\u2019|\"|\u201d]", " ");
		// Replace multiple spaces with single ones
		dmsString = dmsString.replaceAll("\\s+", " ");
		dmsString = dmsString.trim();

		// Check for sign prefix and suffix
		int sign = 1;
		char suffix = dmsString.toUpperCase().charAt(dmsString.length() - 1);
		if (!Character.isDigit(suffix)) {
			sign = (suffix == 'N' || suffix == 'E') ? 1 : -1;
			dmsString = dmsString.substring(0, dmsString.length() - 1);
			dmsString = dmsString.trim();
		}
		char prefix = dmsString.charAt(0);
		if (!Character.isDigit(prefix)) {
			sign *= (prefix == '-') ? -1 : 1;
			dmsString = dmsString.substring(1, dmsString.length());
		}

		// Process degrees, minutes and seconds
		String[] DMS = dmsString.split(" ");
		double d = Integer.parseInt(DMS[0]);
		double m = DMS.length > 1 ? Integer.parseInt(DMS[1]) : 0;
		double s = DMS.length > 2 ? Integer.parseInt(DMS[2]) : 0;

		if (m >= 0 && m <= 60 && s >= 0 && s <= 60)
			return Angle.fromDegrees(d * sign + m / 60 * sign + s / 3600 * sign);

		return null;
	}

	public void addImages(String path, double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {
		scalestart = scalestart + 40;
		try {

			final SurfaceImage si1 = new SurfaceImage(path, Sector.fromDegrees(minLatitude, maxLatitude, minLongitude, maxLongitude));

			final RenderableLayer layerImages = new RenderableLayer();
			layerImages.setName(new File(path).getName());
			layerImages.setPickEnabled(false);
			layerImages.addRenderable(si1);
			LayerComposite lc = new LayerComposite(compositeLayers, SWT.NONE, si1, layerImages, null);
			lc.setBounds(10, 10, 300, 60);
			computeScrolledSize();
			if (wC != null) {

				insertBeforePlacenames(wC, layerImages);
			}

		} catch (Exception e) {
			e.printStackTrace();

		}

	}

	protected void importImagery(String IMAGE_PATH) {
		try {

			// Read the data and save it in a temp file.
			File sourceFile = ExampleUtil.saveResourceToTempFile(IMAGE_PATH, ".tif");

			// Create a raster reader to read this type of file. The reader is
			// created from the currently
			// configured factory. The factory class is specified in the
			// Configuration, and a different one can be
			// specified there.
			DataRasterReaderFactory readerFactory = (DataRasterReaderFactory) WorldWind.createConfigurationComponent(AVKey.DATA_RASTER_READER_FACTORY_CLASS_NAME);
			DataRasterReader reader = readerFactory.findReaderFor(sourceFile, null);

			// Before reading the raster, verify that the file contains imagery.
			AVList metadata = reader.readMetadata(sourceFile, null);
			if (metadata == null || !AVKey.IMAGE.equals(metadata.getStringValue(AVKey.PIXEL_FORMAT)))
				throw new Exception("Not an image file.");

			// Read the file into the raster. read() returns potentially several
			// rasters if there are multiple
			// files, but in this case there is only one so just use the first
			// element of the returned array.
			DataRaster[] rasters = reader.read(sourceFile, null);
			if (rasters == null || rasters.length == 0)
				throw new Exception("Can't read the image file.");

			DataRaster raster = rasters[0];

			// Determine the sector covered by the image. This information is in
			// the GeoTIFF file or auxiliary
			// files associated with the image file.
			final Sector sector = (Sector) raster.getValue(AVKey.SECTOR);
			if (sector == null) {
				throw new Exception("No location specified with image.");
			} else {
				Display display = PlatformUI.getWorkbench().getDisplay();
				display.syncExec(new Runnable() {

					public void run() {
						minLat.setText(String.valueOf(sector.getMinLatitude().degrees));
						minLon.setText(String.valueOf(sector.getMinLongitude().degrees));
						maxLat.setText(String.valueOf(sector.getMaxLatitude().degrees));
						maxLon.setText(String.valueOf(sector.getMaxLongitude().degrees));
					}
				});

			}

			// Request a sub-raster that contains the whole image. This step is
			// necessary because only sub-rasters
			// are reprojected (if necessary); primary rasters are not.
			int width = raster.getWidth();
			int height = raster.getHeight();

			// getSubRaster() returns a sub-raster of the size specified by
			// width and height for the area indicated
			// by a sector. The width, height and sector need not be the full
			// width, height and sector of the data,
			// but we use the full values of those here because we know the full
			// size isn't huge. If it were huge
			// it would be best to get only sub-regions as needed or install it
			// as a tiled image layer rather than
			// merely import it.
			DataRaster subRaster = raster.getSubRaster(width, height, sector, null);

			// Tne primary raster can be disposed now that we have a sub-raster.
			// Disposal won't affect the
			// sub-raster.
			raster.dispose();

			// Verify that the sub-raster can create a BufferedImage, then
			// create one.
			if (!(subRaster instanceof BufferedImageRaster))
				throw new Exception("Cannot get BufferedImage.");
			final BufferedImage image = ((BufferedImageRaster) subRaster).getBufferedImage();

			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {

					MessageBox message = new MessageBox(new Shell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
					message.setMessage("Do you want to open the image in ImageJ, too?");
					message.setText("Bio7");
					int response = message.open();
					if (response == SWT.YES) {
						new ImagePlus("geotiff", image).show();

					} else {

					}

				}
			});

			// The sub-raster can now be disposed. Disposal won't affect the
			// BufferedImage.
			subRaster.dispose();

			// Create a SurfaceImage to display the image over the specified
			// sector.
			final SurfaceImage si1 = new SurfaceImage(image, sector);

			// On the event-dispatch thread, add the imported data as an
			// SurfaceImageLayer.

			// Add the SurfaceImage to a layer.
			final RenderableLayer layerImages = new RenderableLayer();
			layerImages.setName(new File(IMAGE_PATH).getName());
			layerImages.setPickEnabled(false);
			layerImages.addRenderable(si1);

			display.syncExec(new Runnable() {

				public void run() {
					LayerComposite lc = new LayerComposite(compositeLayers, SWT.NONE, si1, layerImages, sector);
					lc.setBounds(10, 10, 300, 60);
					computeScrolledSize();
				}
			});
			if (wC != null) {

				insertBeforePlacenames(wC, layerImages);
			}
			// ExampleUtil.goTo(WorldWindView.getWwd(), sector);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addImageJDynamicImage(final double minLatitude, final double maxLatitude, final double minLongitude, final double maxLongitude) {
		image = null;
		if (makeDynamicIJImage() != null) {
			RenderableLayer layer = new RenderableLayer();
			layer.setPickEnabled(false);
			layer.setName("IJ Dynamic");

			final SurfaceImage surfaceImage = new SurfaceImage(makeDynamicIJImage(), Sector.fromDegrees(minLatitude, maxLatitude, minLongitude, maxLongitude));
			layer.addRenderable(surfaceImage);
			layer.setOpacity(0.5);
			javax.swing.Timer timer = new javax.swing.Timer(20, new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					BufferedImage im = makeDynamicIJImage();
					if (im != null) {
						surfaceImage.setImageSource(im, Sector.fromDegrees(minLatitude, maxLatitude, minLongitude, maxLongitude));
						if (wC != null) {
							wC.redraw();
						}
						// im = null;
					}
				}
			});
			timer.start();
			LayerCompositeVideo lc = new LayerCompositeVideo(compositeLayers, SWT.NONE, surfaceImage, layer, timer);
			lc.setBounds(10, 10, 300, 60);
			computeScrolledSize();
			if (wC != null) {

				insertBeforePlacenames(wC, layer);
			}

		} else {
			MessageBox messageBox = new MessageBox(new Shell(),

			SWT.ICON_WARNING);
			messageBox.setText("Info!");
			messageBox.setMessage("No ImageJ image opened!");
			messageBox.open();
		}

	}

	/* Create a dynamic texture from the ImageJ view! */
	private BufferedImage makeDynamicIJImage() {

		if (WindowManager.getImageCount() > 0) {
			int selImage = WorldWindOptionsView.getTakeImagefromIJ();
			ImagePlus imp;
			if (selImage > 0) {
				imp = WindowManager.getImage(selImage);
			}

			else {
				imp = WindowManager.getCurrentWindow().getImagePlus();
			}

			if (imp != null) {
				ImageProcessor pr = imp.getProcessor();
				/*
				 * If the image is a color image convert it to RGBA buffered
				 * image. If pixel RGB values are 0 alpha value becomes
				 * transparent (0)!
				 */
				if (imageAlpha) {
					if (pr instanceof ColorProcessor) {
						ColorProcessor cp = (ColorProcessor) pr;
						int w = pr.getWidth();
						int h = pr.getHeight();

						ByteProcessor alpha = cp.getChannel(4, null);
						for (int i = 0; i < w; i++) {

							for (int u = 0; u < h; u++) {
								int[] rgb = cp.getPixel(i, u, null);
								if (rgb[0] == 0 && rgb[1] == 0 && rgb[2] == 0) {

									alpha.set(i, u, 0);

								}

							}
						}

						cp.setChannel(4, alpha);
						if (image == null) {
							image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
						}
						WritableRaster raster = image.getRaster();
						if (raster.getWidth() == w && raster.getHeight() == h) {
							raster.setDataElements(0, 0, w, h, cp.getPixels());
						}
					}

					/*
					 * If the image is a greyscale image convert to RGBA
					 * buffered image. If greyscale value is 0 the pixel becomes
					 * transparent!
					 */
					else if (pr instanceof ByteProcessor) {

						ByteProcessor byteProc = (ByteProcessor) pr;
						int w = pr.getWidth();
						int h = pr.getHeight();
						if (image == null) {
							image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

						}

						for (int i = 0; i < w; i++) {

							for (int u = 0; u < h; u++) {

								int grey = byteProc.getPixel(i, u);
								if (grey == 0) {

									int r = 0; // red
									int g = 0;// green
									int b = 0; // blue
									int a = 0; // alpha

									int col = (a << 24) | (r << 16) | (g << 8) | b;
									if (image != null)
										image.setRGB(i, u, col);

								} else {
									int r = grey; // red
									int g = grey;// green
									int b = grey; // blue
									int a = 255; // alpha

									int col = (a << 24) | (r << 16) | (g << 8) | b;

									if (byteProc.getWidth() == w && byteProc.getHeight() == h) {
										if (image != null)
											image.setRGB(i, u, col);
									}

								}

							}
						}

					} else if (pr instanceof FloatProcessor) {

						FloatProcessor floatProc = (FloatProcessor) pr;
						int w = pr.getWidth();
						int h = pr.getHeight();
						if (image == null) {
							image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

						}

						for (int i = 0; i < w; i++) {

							for (int u = 0; u < h; u++) {

								int grey = floatProc.getPixel(i, u);
								if (grey == 0) {

									int r = 0; // red
									int g = 0;// green
									int b = 0; // blue
									int a = 0; // alpha

									int col = (a << 24) | (r << 16) | (g << 8) | b;
									if (image != null)
										image.setRGB(i, u, col);

								} else {
									int r = grey; // red
									int g = grey;// green
									int b = grey; // blue
									int a = 255; // alpha

									int col = (a << 24) | (r << 16) | (g << 8) | b;
									if (floatProc.getWidth() == w && floatProc.getHeight() == h) {
										if (image != null)
											image.setRGB(i, u, col);

									}
								}

							}
						}

					}
				}
				/* Non transparent conversion to buffered image (faster!) */
				else {

					image = imp.getBufferedImage();
				}
			}
		}
		return image;
	}

	public void addBufferedImages(BufferedImage image, final String name, double minLatitude, double maxLatitude, double minLongitude, double maxLongitude) {

		try {

			final SurfaceImage si1 = new SurfaceImage(image, Sector.fromDegrees(minLatitude, maxLatitude, minLongitude, maxLongitude));
			si1.setImageSource(image, Sector.fromDegrees(minLatitude, maxLatitude, minLongitude, maxLongitude));
			final RenderableLayer layerImages = new RenderableLayer();
			layerImages.setName(name);
			layerImages.setPickEnabled(false);
			layerImages.addRenderable(si1);
			LayerComposite lc = new LayerComposite(compositeLayers, SWT.NONE, si1, layerImages, null);
			lc.setBounds(10, 10, 300, 60);
			computeScrolledSize();
			if (wC != null) {
				insertBeforePlacenames(wC, layerImages);
			}

		} catch (Exception e) {
			e.printStackTrace();

		}

	}

	public void addAllImages() {

	}

	public void addDynamiclayer() {
		final RenderableLayer layer = new RenderableLayer();

		DynamicLayer po = new DynamicLayer();

		layer.addRenderable(po);
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				LayerCompositeDynamic lc = new LayerCompositeDynamic(compositeLayers, SWT.NONE, layer);
				lc.setBounds(10, 10, 300, 60);
				computeScrolledSize();
			}
		});

		if (wC != null) {
			insertBeforeCompass(wC, layer);
		}
	}

	public static void computeScrolledSize() {

		/*
		 * Control children[] = compositeLayers.getChildren();
		 * 
		 * if (children.length == 0){
		 * 
		 * }
		 * 
		 * else{
		 * 
		 * 
		 * for (Control tmp : children) { // The check below will not work
		 * because x, y and the control's bounds could be // relative to
		 * different parents... Better to convert all coordinates to display //
		 * by using Control.toDisplay() and then compare below
		 * System.out.println( tmp.getBounds().y);
		 * 
		 * 
		 * if(tmp.getBounds().y>200){
		 * 
		 * } }
		 * 
		 * } // scrolledComposite.setMinSize(300, 55 * co.length);
		 */compositeLayers.layout(true);
		// scrolledComposite.layout(true);
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

	public static void insertBeforePlacenames(WorldWindow wwd, Layer layer) {
		// Insert the layer into the layer list just before the placenames.
		int compassPosition = 0;
		LayerList layers = wwd.getModel().getLayers();
		for (Layer l : layers) {
			if (l instanceof PlaceNameLayer)
				compassPosition = layers.indexOf(l);
		}
		layers.add(compassPosition, layer);
	}

	public static void insertAfterPlacenames(WorldWindow wwd, Layer layer) {
		// Insert the layer into the layer list just after the placenames.
		int compassPosition = 0;
		LayerList layers = wwd.getModel().getLayers();
		for (Layer l : layers) {
			if (l instanceof PlaceNameLayer)
				compassPosition = layers.indexOf(l);
		}
		layers.add(compassPosition + 1, layer);
	}

	public static void insertBeforeLayerName(WorldWindow wwd, Layer layer, String targetName) {
		// Insert the layer into the layer list just before the target layer.
		int targetPosition = 0;
		LayerList layers = wwd.getModel().getLayers();
		for (Layer l : layers) {
			if (l.getName().indexOf(targetName) != -1) {
				targetPosition = layers.indexOf(l);
				break;
			}
		}
		layers.add(targetPosition, layer);
	}

	/**
	 * Opens a file-open dialog which displays the file with the given
	 * extensions.
	 * 
	 * @param extension
	 *            the extensions as a String array which should be displayed.
	 * @return a file path as a string from the file dialog.
	 */
	public String openFile(final String[] extension) {
		file = null;
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				Shell s = new Shell(SWT.ON_TOP);
				FileDialog fd = new FileDialog(s, SWT.OPEN);
				fd.setText("Load");

				fd.setFilterExtensions(extension);
				file = fd.open();
			}
		});
		return file;
	}

	private void updateMetric() {

		// Update length label
		double value = measureTool.getLength();

		if (value <= 0)
			s = "na";
		else if (value < 1000)
			s = String.format("%,7.1f m", value);
		else
			s = String.format("%,7.3f km", value / 1000);

		table.getItem(count).setText(0, s);

		// lengthLabel.setText(s);

		// Update area label
		value = measureTool.getArea();
		if (value < 0)
			s = "na";
		else if (value < 1e6)
			s = String.format("%,7.1f m2", value);

		else
			s = String.format("%,7.3f km2", value / 1e6);

		table.getItem(count).setText(1, s);

		// areaLabel.setText(s);

		// Update width label
		value = measureTool.getWidth();
		if (value < 0)
			s = "na";
		else if (value < 1000)
			s = String.format("%,7.1f m", value);
		else
			s = String.format("%,7.3f km", value / 1000);

		table.getItem(count).setText(2, s);

		// widthLabel.setText(s);

		// Update height label
		value = measureTool.getHeight();
		if (value < 0)
			s = "na";
		else if (value < 1000)
			s = String.format("%,7.1f m", value);
		else
			s = String.format("%,7.3f km", value / 1000);

		table.getItem(count).setText(3, s);

		// heightLabel.setText(s);

		// Update heading label
		Angle angle = measureTool.getOrientation();
		if (angle != null)
			s = String.format("%,6.2f\u00B0", angle.degrees);
		else
			s = "na";

		table.getItem(count).setText(4, s);

		// headingLabel.setText(s);

		// Update center label
		Position center = measureTool.getCenterPosition();
		if (center != null)
			s = String.format("%,7.4f\u00B0 %,7.4f\u00B0", center.getLatitude().degrees, center.getLongitude().degrees);
		else
			s = "na";

		table.getItem(count).setText(5, s);

		// centerLabel.setText(s);
	}

	private void flyToCoords() {
		String lookupString = text.getText();
		if (lookupString == null || lookupString.length() < 1)
			return;

		java.util.List<PointOfInterest> poi = parseSearchValues(lookupString);

		if (poi != null) {
			if (poi.size() == 1) {
				moveToLocation(poi.get(0));

			}

			/*
			 * else { resultsBox.removeAllItems(); for ( PointOfInterest p:poi)
			 * { resultsBox.addItem(p); } resultsPanel.setVisible(true); }
			 */
		}
	}

	public static WorldWindOptionsView getOptionsInstance() {
		return optionsInstance;
	}

	public static void addDynamicLayer() {
		optionsInstance.addDynamiclayer();
	}

	/*
	 * Sample inputs Coordinate formats: 39.53, -119.816 (Reno, NV) 21 10 14 N,
	 * 86 51 0 W (Cancun)
	 */
	public java.util.List<PointOfInterest> parseSearchValues(String searchStr) {
		String sepRegex = "[,]"; // other separators??
		searchStr = searchStr.trim();
		String[] searchValues = searchStr.split(sepRegex);
		if (searchValues.length == 1) {
			return queryService(searchValues[0].trim());
		} else if (searchValues.length == 2) // possible coordinates
		{
			// any numbers at all?
			String regex = "[0-9]";
			Pattern pattern = Pattern.compile(regex);
			Matcher matcher = pattern.matcher(searchValues[1]); // Street
																// Address may
																// have numbers
																// in first
																// field so use
																// 2nd
			if (matcher.find()) {
				java.util.List<PointOfInterest> list = new ArrayList<PointOfInterest>();
				list.add(parseCoordinates(searchValues));
				return list;
			} else {
				return queryService(searchValues[0].trim() + "+" + searchValues[1].trim());
			}
		} else {
			// build search string and send to service
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < searchValues.length; i++) {
				sb.append(searchValues[i].trim());
				if (i < searchValues.length - 1)
					sb.append("+");
			}

			return queryService(sb.toString());
		}
	}

	public java.util.List<PointOfInterest> queryService(String queryString) {
		java.util.List<PointOfInterest> results = this.gazeteer.findPlaces(queryString);
		if (results == null || results.size() == 0)
			return null;
		else
			return results;
	}

	// throws IllegalArgumentException
	private PointOfInterest parseCoordinates(String coords[]) {
		if (isDecimalDegrees(coords)) {
			Double d1 = Double.parseDouble(coords[0].trim());
			Double d2 = Double.parseDouble(coords[1].trim());

			return new BasicPointOfInterest(LatLon.fromDegrees(d1, d2));
		} else // may be in DMS
		{
			Angle aLat = Angle.fromDMS(coords[0].trim());
			Angle aLon = Angle.fromDMS(coords[1].trim());

			return new BasicPointOfInterest(LatLon.fromDegrees(aLat.getDegrees(), aLon.getDegrees()));
		}
	}

	private boolean isDecimalDegrees(String[] coords) {
		try {
			Double.parseDouble(coords[0].trim());
			Double.parseDouble(coords[1].trim());
		} catch (NumberFormatException nfe) {
			return false;
		}

		return true;
	}

	public void moveToLocation(PointOfInterest location) {
		// Use a PanToIterator to iterate view to target position
		if (wC != null) {
			wC.getView().goTo(new Position(location.getLatlon(), 0), 25e3);
		}
	}

	public void moveToLocation(Sector sector, Double altitude) {
		if (wC != null) {

			OrbitView view = (OrbitView) wC.getView();

			Globe globe = wC.getModel().getGlobe();

			if (altitude == null || altitude == 0) {
				double t = sector.getDeltaLonRadians() > sector.getDeltaLonRadians() ? sector.getDeltaLonRadians() : sector.getDeltaLonRadians();
				double w = 0.5 * t * 6378137.0;
				altitude = w / wC.getView().getFieldOfView().tanHalfAngle();
			}

			if (globe != null && view != null) {
				wC.getView().goTo(new Position(sector.getCentroid(), 0), altitude);
			}
		}
	}

	public static int getTakeImagefromIJ() {
		return takeImagefromIJ;
	}
}
