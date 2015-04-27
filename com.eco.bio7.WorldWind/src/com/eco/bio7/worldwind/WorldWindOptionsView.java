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
	// private GUIBuilder builder;
	private static ScrolledComposite scrolledComposite;
	public static Composite composite_4;
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
	private static int takeImagefromIJ=0;

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

		text = new Text(composite_2, SWT.BORDER);
		text.addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(Event e) {
				flyToCoords();
			}
		});
		text.setBounds(10, 56, 182, 23);

		final Button setButton = new Button(composite_2, SWT.NONE);
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
		setButton.setBounds(234, 51, 112, 29);

		final Label coordinatesLabel = new Label(composite_2, SWT.NONE);
		coordinatesLabel.setText("Coordinates");
		coordinatesLabel.setBounds(59, 85, 104, 23);

		final Button iButton = new Button(composite_2, SWT.NONE);
		iButton.setToolTipText("Info!");
		iButton.addSelectionListener(new SelectionAdapter() {
			private ToolTip infoTip;

			public void widgetSelected(final SelectionEvent e) {
				infoTip = new ToolTip(new Shell(), SWT.BALLOON | SWT.ICON_INFORMATION);
				infoTip.setText("Projection");

				infoTip.setMessage("Equirectangular projection \nDatum: WGS 84\nEPSG: 4326\n\n" + "Enter city or coordinates like:\n" + "Street, City\n" + "39.53, -119.816  (Reno, NV)\n"
						+ "21 10 14 N, 86 51 0 W (Cancun)" + "\n-31¡ 59' 43\", 115¡ 45' 32\" (Perth)");
				infoTip.setVisible(true);

			}
		});
		iButton.setText("Info");
		iButton.setBounds(10, 6, 112, 29);

		final Button cacheButton = new Button(composite_2, SWT.NONE);
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
		cacheButton.setBounds(122, 6, 112, 29);

		final Button openCacheButton = new Button(composite_2, SWT.NONE);
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
		openCacheButton.setBounds(234, 6, 112, 29);

		scrolLList = new org.eclipse.swt.widgets.List(composite_2, SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER);
		scrolLList.setBounds(10, 125, 182, 194);
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		String link = store.getString("WorldWindLinks");
		if (link.equals("") == false) {
			String[] links = link.split(";");

			for (int i = 0; i < links.length; i++) {
				scrolLList.add(links[i]);
			}
		}
		scrolLList.addMouseListener(new MouseListener() {

			@Override
			public void mouseDoubleClick(org.eclipse.swt.events.MouseEvent e) {
				if(scrolLList.getItemCount()>0){
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

		final Button addButton = new Button(composite_2, SWT.NONE);
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
		addButton.setBounds(234, 125, 112, 29);

		final Button deleteButton = new Button(composite_2, SWT.NONE);
		deleteButton.setToolTipText("Delete stored coordinates");
		deleteButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IPreferenceStore store = Activator.getDefault().getPreferenceStore();
				if(scrolLList.getItemCount()>0){
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
		deleteButton.setBounds(234, 154, 112, 29);

		final Button saveButton = new Button(composite_2, SWT.NONE);
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
		saveButton.setBounds(234, 212, 112, 29);

		final Button loadButton = new Button(composite_2, SWT.NONE);
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
		loadButton.setBounds(234, 183, 112, 29);

		final ExpandItem newItemExpandItem = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem.setHeight(1550);
		newItemExpandItem.setText("Default Layers");

		composite = new Composite(expandBar, SWT.NONE);
		newItemExpandItem.setControl(composite);

		createLayers();
		composite.layout();
		newItemExpandItem_1 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_1.setHeight(500);
		newItemExpandItem_1.setText("Layers");

		composite_1 = new Composite(expandBar, SWT.NONE);
		composite_1.setLayout(new FormLayout());
		newItemExpandItem_1.setControl(composite_1);

		final Button addImageLayerButton = new Button(composite_1, SWT.NONE);
		final FormData fd_addImageLayerButton = new FormData();
		addImageLayerButton.setLayoutData(fd_addImageLayerButton);
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

		minLat = new Text(composite_1, SWT.BORDER);
		final FormData fd_minLat = new FormData();
		minLat.setLayoutData(fd_minLat);
		minLat.setToolTipText("Min");
		minLat.setText("50.999583");

		minLon = new Text(composite_1, SWT.BORDER);
		final FormData fd_minLon = new FormData();
		minLon.setLayoutData(fd_minLon);
		minLon.setToolTipText("Min");
		minLon.setText("9.999583");

		maxLat = new Text(composite_1, SWT.BORDER);
		fd_addImageLayerButton.bottom = new FormAttachment(maxLat, 35, SWT.BOTTOM);
		fd_addImageLayerButton.top = new FormAttachment(maxLat, 5, SWT.BOTTOM);
		fd_minLon.top = new FormAttachment(maxLat, -30, SWT.TOP);
		fd_minLon.bottom = new FormAttachment(maxLat, -5, SWT.TOP);
		final FormData fd_maxLat = new FormData();
		maxLat.setLayoutData(fd_maxLat);
		maxLat.setToolTipText("Max");
		maxLat.setText("52.00042");

		maxLon = new Text(composite_1, SWT.BORDER);
		final FormData fd_maxLon = new FormData();
		fd_maxLon.top = new FormAttachment(maxLat, -30, SWT.TOP);
		fd_maxLon.bottom = new FormAttachment(maxLat, -5, SWT.TOP);
		maxLon.setLayoutData(fd_maxLon);
		maxLon.setToolTipText("Max");
		maxLon.setText("11.00042");

		Label latitudeLabel;
		latitudeLabel = new Label(composite_1, SWT.NONE);
		final FormData fd_latitudeLabel = new FormData();
		fd_latitudeLabel.bottom = new FormAttachment(minLat, -5, SWT.TOP);
		fd_latitudeLabel.top = new FormAttachment(0, 10);
		fd_latitudeLabel.right = new FormAttachment(0, 168);
		fd_latitudeLabel.left = new FormAttachment(0, 88);
		latitudeLabel.setLayoutData(fd_latitudeLabel);
		latitudeLabel.setAlignment(SWT.CENTER);
		latitudeLabel.setText("Latitude");

		final Label longitudeLabel = new Label(composite_1, SWT.CENTER);
		final FormData fd_longitudeLabel = new FormData();
		fd_longitudeLabel.bottom = new FormAttachment(0, 58);
		fd_longitudeLabel.top = new FormAttachment(0, 35);
		fd_longitudeLabel.right = new FormAttachment(maxLat, 77, SWT.RIGHT);
		fd_longitudeLabel.left = new FormAttachment(maxLat, 5, SWT.RIGHT);
		longitudeLabel.setLayoutData(fd_longitudeLabel);
		longitudeLabel.setAlignment(SWT.CENTER);
		longitudeLabel.setText("Longitude");

		Button removeAllButton;
		removeAllButton = new Button(composite_1, SWT.NONE);
		final FormData fd_removeAllButton = new FormData();
		removeAllButton.setLayoutData(fd_removeAllButton);
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

		Button fromImagejButton;
		fromImagejButton = new Button(composite_1, SWT.NONE);
		fd_addImageLayerButton.left = new FormAttachment(fromImagejButton, -86, SWT.LEFT);
		fd_addImageLayerButton.right = new FormAttachment(fromImagejButton, 0, SWT.LEFT);
		fd_maxLat.top = new FormAttachment(fromImagejButton, -30, SWT.TOP);
		fd_maxLat.bottom = new FormAttachment(fromImagejButton, -5, SWT.TOP);
		fd_maxLat.left = new FormAttachment(fromImagejButton, -84, SWT.RIGHT);
		fd_maxLat.right = new FormAttachment(fromImagejButton, 0, SWT.RIGHT);
		fd_minLon.left = new FormAttachment(fromImagejButton, -85, SWT.LEFT);
		fd_minLon.right = new FormAttachment(fromImagejButton, -5, SWT.LEFT);
		final FormData fd_fromImagejButton = new FormData();
		fd_fromImagejButton.bottom = new FormAttachment(0, 153);
		fd_fromImagejButton.top = new FormAttachment(0, 123);
		fd_fromImagejButton.right = new FormAttachment(0, 176);
		fd_fromImagejButton.left = new FormAttachment(0, 92);
		fromImagejButton.setLayoutData(fd_fromImagejButton);
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

		Button dynamicButton;
		dynamicButton = new Button(composite_1, SWT.NONE);
		fd_removeAllButton.left = new FormAttachment(dynamicButton, 2);
		fd_removeAllButton.bottom = new FormAttachment(dynamicButton, 30, SWT.TOP);
		fd_removeAllButton.top = new FormAttachment(dynamicButton, 0, SWT.TOP);
		final FormData fd_dynamicButton = new FormData();
		fd_dynamicButton.bottom = new FormAttachment(addImageLayerButton, 30, SWT.BOTTOM);
		fd_dynamicButton.top = new FormAttachment(addImageLayerButton, 0, SWT.BOTTOM);
		fd_dynamicButton.right = new FormAttachment(addImageLayerButton, 86, SWT.LEFT);
		fd_dynamicButton.left = new FormAttachment(addImageLayerButton, 0, SWT.LEFT);
		dynamicButton.setLayoutData(fd_dynamicButton);
		dynamicButton.setToolTipText("Add a dynamic layer which executes compiled code");
		dynamicButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				addDynamiclayer();

			}
		});
		dynamicButton.setText("Dyn. Layer");

		scrolledComposite = new ScrolledComposite(composite_1, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		final FormData fd_scrolledComposite = new FormData();
		fd_scrolledComposite.bottom = new FormAttachment(100, -10);
		fd_scrolledComposite.right = new FormAttachment(100, -5);
		fd_scrolledComposite.left = new FormAttachment(0, 0);
		scrolledComposite.setLayoutData(fd_scrolledComposite);
		scrolledComposite.getVerticalBar().setMinimum(100);
		scrolledComposite.getVerticalBar().setMaximum(200);
		scrolledComposite.getHorizontalBar().setMaximum(200);
		scrolledComposite.getHorizontalBar().setMinimum(100);

		composite_4 = new Composite(scrolledComposite, SWT.NONE);
		composite_4.setLayout(new RowLayout());
		composite_4.setSize(305, 203);
		scrolledComposite.setContent(composite_4);

		Button goButton;
		goButton = new Button(composite_1, SWT.NONE);
		fd_minLat.top = new FormAttachment(goButton, -29, SWT.TOP);
		fd_minLat.bottom = new FormAttachment(goButton, -5, SWT.TOP);
		fd_minLat.left = new FormAttachment(goButton, -84, SWT.RIGHT);
		fd_minLat.right = new FormAttachment(goButton, 0, SWT.RIGHT);
		goButton.setToolTipText("Fly to the specified area");
		final FormData fd_goButton = new FormData();
		fd_goButton.top = new FormAttachment(maxLat, -35, SWT.TOP);
		fd_goButton.bottom = new FormAttachment(maxLat, -5, SWT.TOP);
		fd_goButton.left = new FormAttachment(maxLon, -89, SWT.LEFT);
		fd_goButton.right = new FormAttachment(maxLon, -5, SWT.LEFT);
		goButton.setLayoutData(fd_goButton);
		goButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				double meanLat = (Double.parseDouble(minLat.getText()) + Double.parseDouble(maxLat.getText())) / 2;
				double meanLon = (Double.parseDouble(minLon.getText()) + Double.parseDouble(maxLon.getText())) / 2;
				Position p = new Position(new LatLon(Angle.fromDegrees(meanLat), Angle.fromDegrees(meanLon)), 2000);
				Coordinates.flyTo(p);

			}
		});
		goButton.setText("Go to");

		Button videoButton;
		videoButton = new Button(composite_1, SWT.NONE);
		videoButton.setToolTipText("Enables a dynamic ImageJ layer\non top of the globe");
		fd_maxLon.left = new FormAttachment(videoButton, -80, SWT.RIGHT);
		fd_maxLon.right = new FormAttachment(videoButton, 0, SWT.RIGHT);
		videoButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				addImageJDynamicImage(new Double(minLat.getText()), new Double(maxLat.getText()), new Double(minLon.getText()), new Double(maxLon.getText()));
			}
		});
		final FormData fd_videoButton = new FormData();
		fd_videoButton.bottom = new FormAttachment(0, 153);
		fd_videoButton.top = new FormAttachment(0, 123);
		fd_videoButton.right = new FormAttachment(fromImagejButton, 84, SWT.RIGHT);
		fd_videoButton.left = new FormAttachment(fromImagejButton, 0, SWT.RIGHT);
		videoButton.setLayoutData(fd_videoButton);
		videoButton.setText("IJ Dynamic");

		computeButton = new Button(composite_1, SWT.NONE);
		fd_removeAllButton.right = new FormAttachment(computeButton);
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
		final FormData fd_computeButton = new FormData();
		fd_computeButton.left = new FormAttachment(videoButton, -84, SWT.RIGHT);
		fd_computeButton.right = new FormAttachment(videoButton, 0, SWT.RIGHT);
		fd_computeButton.bottom = new FormAttachment(removeAllButton, 30, SWT.TOP);
		fd_computeButton.top = new FormAttachment(removeAllButton, 0, SWT.TOP);
		computeButton.setLayoutData(fd_computeButton);
		computeButton.setText("Compute");

		Button btnNewButton = new Button(composite_1, SWT.NONE);
		fd_scrolledComposite.top = new FormAttachment(btnNewButton, 6);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				final String f = openFile(new String[] { "*.shp", "*" });

				if (f != null) {
					LoadShapefileJob shapeFileJob = new LoadShapefileJob(f, composite_4);
					shapeFileJob.schedule();
				}

			}
		});
		FormData fd_btnNewButton = new FormData();
		fd_btnNewButton.bottom = new FormAttachment(removeAllButton, 30, SWT.BOTTOM);
		fd_btnNewButton.top = new FormAttachment(dynamicButton);
		fd_btnNewButton.right = new FormAttachment(videoButton, -167, SWT.RIGHT);
		fd_btnNewButton.left = new FormAttachment(0, 7);
		btnNewButton.setLayoutData(fd_btnNewButton);
		btnNewButton.setText("Add Shapefile");
		
		final Spinner spinner_1 = new Spinner(composite_1, SWT.BORDER);
		spinner_1.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				
				takeImagefromIJ=spinner_1.getSelection();
			}
		});
		FormData fd_spinner_1 = new FormData();
		fd_spinner_1.bottom = new FormAttachment(btnNewButton, 30);
		fd_spinner_1.right = new FormAttachment(maxLon, 80);
		fd_spinner_1.left = new FormAttachment(computeButton, 0, SWT.LEFT);
		fd_spinner_1.top = new FormAttachment(btnNewButton, 0, SWT.TOP);
		spinner_1.setLayoutData(fd_spinner_1);

		final ExpandItem newItemExpandItem_2 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_2.setHeight(140);
		newItemExpandItem_2.setText("Render");

		final Composite composite_3 = new Composite(expandBar, SWT.NONE);
		newItemExpandItem_2.setControl(composite_3);

		final Button screenshotButton = new Button(composite_3, SWT.NONE);
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
		screenshotButton.setBounds(10, 52, 92, 32);

		capture = new Button(composite_3, SWT.NONE);
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
		capture.setBounds(10, 10, 92, 32);
		capture.setText("Capture");

		countToText = new Text(composite_3, SWT.BORDER);
		countToText.setText("100");
		countToText.setBounds(107, 15, 92, 20);
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

		Label lblNewLabel = new Label(composite_3, SWT.NONE);
		lblNewLabel.setBounds(206, 20, 48, 14);
		lblNewLabel.setText("Frames");

		errorLabel = new Label(composite_3, SWT.NONE);
		errorLabel.setBounds(108, 41, 171, 14);

		Button infoCaptureButton = new Button(composite_3, SWT.NONE);
		infoCaptureButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				captureTip = new ToolTip(new Shell(), SWT.BALLOON | SWT.ICON_INFORMATION);
				captureTip.setText("Info");

				captureTip.setMessage("" + "If the globe is resized within the capture mode the recording\n" + "is interrupted!\n" + "The captured images are added to an ImageJ stack which\n"
						+ "can be saved e.g. as an *.avi or animated *.gif file.");
				captureTip.setVisible(true);
			}
		});
		infoCaptureButton.setBounds(10, 94, 92, 32);
		infoCaptureButton.setText("Info");

		spinner = new Spinner(composite_3, SWT.BORDER);
		spinner.setMaximum(60);
		spinner.setMinimum(1);
		spinner.setSelection(25);
		spinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});
		spinner.setBounds(107, 57, 92, 20);

		Label lblFps = new Label(composite_3, SWT.NONE);
		lblFps.setBounds(206, 60, 49, 14);
		lblFps.setText("FPS");

		final ExpandItem newItemExpandItem_5 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_5.setHeight(500);
		newItemExpandItem_5.setText("Measure");

		final Composite composite_6 = new Composite(expandBar, SWT.NONE);
		newItemExpandItem_5.setControl(composite_6);

		final Button lineButton = new Button(composite_6, SWT.NONE);
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
		lineButton.setBounds(10, 10, 91, 30);

		final Button pointsButton = new Button(composite_6, SWT.NONE);
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
		pointsButton.setBounds(107, 10, 91, 30);

		final Button tooltipButton = new Button(composite_6, SWT.NONE);
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
		tooltipButton.setBounds(204, 10, 91, 30);

		combo_1 = new Combo(composite_6, SWT.READ_ONLY);
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
		combo_1.setBounds(210, 70, 85, 30);

		combo_2 = new Combo(composite_6, SWT.READ_ONLY);
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
		combo_2.setBounds(210, 97, 85, 23);

		combo_3 = new Combo(composite_6, SWT.READ_ONLY);
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
		combo_3.setBounds(210, 124, 85, 23);

		combo_4 = new Combo(composite_6, SWT.READ_ONLY);
		combo_4.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				String item = combo_4.getItem(combo_4.getSelectionIndex());
				measureTool.getUnitsFormat().setShowDMS(item.equals("DMS"));
			}
		});
		combo_4.setItems(new String[] { "DD", "DMS" });
		combo_4.select(0);
		combo_4.setBounds(210, 151, 85, 23);

		final Label shapeLabel = new Label(composite_6, SWT.NONE);
		shapeLabel.setText("Shape:");
		shapeLabel.setBounds(10, 70, 85, 18);

		final Label pathTypeLabel = new Label(composite_6, SWT.NONE);
		pathTypeLabel.setText("Path type:");
		pathTypeLabel.setBounds(10, 97, 85, 18);

		final Label unitsLabel = new Label(composite_6, SWT.NONE);
		unitsLabel.setText("Units:");
		unitsLabel.setBounds(10, 124, 85, 18);

		final Label angleFormatLabel = new Label(composite_6, SWT.NONE);
		angleFormatLabel.setText("Angle Format:");
		angleFormatLabel.setBounds(10, 151, 85, 18);

		final Button followTerrainButton = new Button(composite_6, SWT.CHECK);
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
		followTerrainButton.setBounds(10, 197, 111, 23);

		final Button controlPointsButton = new Button(composite_6, SWT.CHECK);
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
		controlPointsButton.setBounds(127, 197, 111, 23);

		final Button rubberBandButton = new Button(composite_6, SWT.CHECK);
		rubberBandButton.setToolTipText("Enables or disables the \"Rubber band\" option!");
		rubberBandButton.setSelection(true);

		if (measureTool != null) {
			rubberBandButton.setSelection(measureTool.getController().isUseRubberBand());
		}
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
		rubberBandButton.setBounds(10, 225, 111, 23);

		freehandButton = new Button(composite_6, SWT.CHECK);
		freehandButton.setToolTipText("Enables or disables freehand selections!");
		if (measureTool != null) {
			freehandButton.setSelection(measureTool.getController().isFreeHand());
		}
		freehandButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				measureTool.getController().setFreeHand(freehandButton.getSelection());
				if (wC != null) {
					wC.redraw();
				}

			}
		});
		freehandButton.setText("Free Hand");
		freehandButton.setBounds(127, 225, 111, 23);

		final Button tooltipButton_1 = new Button(composite_6, SWT.CHECK);
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
		tooltipButton_1.setBounds(10, 249, 111, 23);

		newButton = new Button(composite_6, SWT.NONE);
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
		newButton.setBounds(10, 278, 91, 30);
		newButton.setEnabled(true);

		pauseButton = new Button(composite_6, SWT.NONE);
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
		pauseButton.setBounds(107, 278, 91, 30);

		endButton = new Button(composite_6, SWT.NONE);
		endButton.setToolTipText("Stop a measurement!");
		endButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				measureTool.setArmed(false);
			}
		});
		endButton.setText("End");
		endButton.setBounds(204, 278, 91, 30);
		endButton.setEnabled(false);

		table = new Table(composite_6, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.setBounds(10, 313, 284, 75);
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

		new TableItem(table, SWT.NONE);

		final Button toSpreadButton = new Button(composite_6, SWT.NONE);
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
		toSpreadButton.setBounds(10, 394, 91, 30);

		final Label coloursLabel = new Label(composite_6, SWT.NONE);
		coloursLabel.setAlignment(SWT.CENTER);
		coloursLabel.setText("Colours");
		coloursLabel.setBounds(123, 46, 62, 23);

		final Label label = new Label(composite_6, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setBounds(10, 46, 275, 21);

		final ExpandItem newItemExpandItem_4 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_4.setHeight(100);
		newItemExpandItem_4.setText("Projection");

		final Composite composite_5 = new Composite(expandBar, SWT.NONE);
		newItemExpandItem_4.setControl(composite_5);

		final Button flatButton = new Button(composite_5, SWT.CHECK);
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
		flatButton.setBounds(10, 10, 64, 21);

		combo = new Combo(composite_5, SWT.READ_ONLY);
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
		combo.setBounds(79, 8, 107, 23);
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
					if (event.getPropertyName().equals(MeasureTool.EVENT_POSITION_ADD) || event.getPropertyName().equals(MeasureTool.EVENT_POSITION_REMOVE)
							|| event.getPropertyName().equals(MeasureTool.EVENT_POSITION_REPLACE)) {

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

				if (layer.getName().equals("MS Virtual Earth Aerial") || layer.getName().equals("Bing Imagery") || layer.getName().equals("MS Virtual Earth Hybrid")
						|| layer.getName().equals("MS Virtual Earth Roads") || layer.getName().equals("OpenStreetMap") || layer.getName().equals("USGS Urban Area")
						|| layer.getName().equals("USDA NAIP") || layer.getName().equals("i-cubed Landsat") || layer.getName().equals("Blue Marble (WMS) 2004")
						|| layer.getName().equals("NASA Blue Marble Image") || layer.getName().equals("USGS Urban Area Ortho")) {

					final Scale scale = new Scale(composite, SWT.NONE);
					scale.setMinimum(1);
					scale.setMaximum(100);
					scale.setSelection(100);
					scale.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(final SelectionEvent e) {
							layer.setOpacity(((double) scale.getSelection()) / 100);

							wC.redraw();

						}
					});

					scale.setBounds(10, ystart + 30, 200, 40);
				}
				b.setBounds(10, ystart, 200, 40);
				ystart = ystart + 65;
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
			LayerComposite lc = new LayerComposite(composite_4, SWT.NONE, si1, layerImages, null);
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
						new ImagePlus("geotiff",image).show();

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
					LayerComposite lc = new LayerComposite(composite_4, SWT.NONE, si1, layerImages, sector);
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

		if (makeDynamicIJImage() != null) {
			RenderableLayer layer = new RenderableLayer();
			layer.setPickEnabled(false);
			layer.setName("IJ Dynamic");

			final SurfaceImage surfaceImage = new SurfaceImage(makeDynamicIJImage(), Sector.fromDegrees(minLatitude, maxLatitude, minLongitude, maxLongitude));
			layer.addRenderable(surfaceImage);
			layer.setOpacity(0.5);
			javax.swing.Timer timer = new javax.swing.Timer(40, new ActionListener() {
				public void actionPerformed(ActionEvent actionEvent) {
					BufferedImage im = makeDynamicIJImage();
					if (im != null) {
						surfaceImage.setImageSource(im, Sector.fromDegrees(minLatitude, maxLatitude, minLongitude, maxLongitude));
						if (wC != null) {
							wC.redraw();
						}
						im = null;
					}
				}
			});
			timer.start();
			LayerCompositeVideo lc = new LayerCompositeVideo(composite_4, SWT.NONE, surfaceImage, layer, timer);
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
		BufferedImage image = null;
		// Graphics2D g = image.createGraphics();
		if (WindowManager.getImageCount() > 0) {
			int selImage=WorldWindOptionsView.getTakeImagefromIJ();
			ImagePlus imp;
			if(selImage>0){
				imp = WindowManager.getImage(selImage);
			}
			
			else{
				imp = WindowManager.getCurrentWindow().getImagePlus();
			}
			
			if (imp != null) {
				ImageProcessor pr = imp.getProcessor();

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
					image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
					WritableRaster raster = image.getRaster();
					raster.setDataElements(0, 0, w, h, cp.getPixels());
				} else {

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
			LayerComposite lc = new LayerComposite(composite_4, SWT.NONE, si1, layerImages, null);
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
				LayerCompositeDynamic lc = new LayerCompositeDynamic(composite_4, SWT.NONE, layer);
				lc.setBounds(10, 10, 300, 60);
				computeScrolledSize();
			}
		});

		if (wC != null) {
			insertBeforeCompass(wC, layer);
		}
	}

	public static void computeScrolledSize() {
		Control co[] = composite_4.getChildren();
		composite_4.setSize(300, 55 * co.length);
		scrolledComposite.setMinSize(300, 55 * co.length);
		composite_4.layout(true);
		scrolledComposite.layout(true);
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
