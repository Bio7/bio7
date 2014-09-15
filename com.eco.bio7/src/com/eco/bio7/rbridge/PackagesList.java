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


package com.eco.bio7.rbridge;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wb.swt.layout.grouplayout.GroupLayout;
import org.eclipse.wb.swt.layout.grouplayout.LayoutStyle;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RState;
import com.swtdesigner.SWTResourceManager;


public class PackagesList extends Shell {

	private Text text;
	private static List spatialList;
	private static List allPackagesList;
	private HashMap<String, String[]> map = new HashMap<String, String[]>();
	private Button btnContextSensitive;

	public PackagesList(Display display, int style) {
		super(display, style);
		createContents();
		setLayout(new FillLayout());
		
		setImage(SWTResourceManager.getImage(PackagesList.class, "/pics/logo.gif"));
		map.clear();
		map.put("spatstat", new String[] { "A library for the statistical analysis of spatial data, \n mainly spatial point patterns.", "http://www.spatstat.org" });
		map.put("maptools", new String[] { "Tools for reading and handling spatial objects", "http://cran.r-project.org/web/packages/maptools/index.html" });
		map.put("gstat", new String[] { "Gstat is an open source (GPL) computer code for multivariable \n geostatistical modelling, prediction and simulation", "http://www.gstat.org/" });
		map.put("maps", new String[] { "Display of maps. Projection code and larger maps are in separate packages (mapproj and mapdata).", "http://cran.r-project.org/web/packages/maps/index.html" });
		map.put("rgdal", new String[] {
				"Provides bindings to Frank Warmerdam's Geospatial Data Abstraction Library (GDAL) (>= 1.3.1)\n and access to projection/transformation operations from the PROJ.4 library.",
				"http://cran.r-project.org/web/packages/rgdal/index.html" });
		map
				.put(
						"PBSmapping",
						new String[] {
								"This software has evolved from fisheries research conducted at the Pacific Biological Station (PBS) in Nanaimo, British Columbia, Canada.\n It extends the R language to include two-dimensional plotting features similar to those commonly available in a Geographic Information System (GIS).",
								"http://cran.r-project.org/web/packages/PBSmapping/index.html" });
		map.put("shapefiles", new String[] { "Functions to read and write ESRI shapefiles.", "http://cran.r-project.org/web/packages/shapefiles/index.html" });
		map.put("RSAGA", new String[] { "RSAGA provides access to geocomputing and terrain analysis functions of SAGA from within R\n by running the command line version of SAGA.",
				"http://cran.r-project.org/web/packages/RSAGA/index.html" });
		map.put("geoR", new String[] { "Geostatistical analysis including traditional, likelihood-based and Bayesian methods.", "http://leg.ufpr.br/geoR/" });
		map.put("geoRglm", new String[] { "Functions for inference in generalised linear spatial models. \nThe posterior and predictive inference is based on Markov chain Monte Carlo methods.",
				"http://gbi.agrsci.dk/~ofch/geoRglm/"});
		map.put("SDMTools", new String[] {"This packages provides a set of tools for post processing the outcomes\nof species distribution modeling exercises. It includes novel methods for comparing models\nand tracking changes in distributions through time.\nIt further includes methods for visualizing outcomes, selecting thresholds, calculating measures\nof accuracy and landscape fragmentation statistics, etc.","http://cran.r-project.org/web/packages/SDMTools/"});

	}

	/**
	 * Create contents of the window
	 */
	protected void createContents() {
		setText("Packages");
		setSize(221, 581);

		final ExpandBar expandBar = new ExpandBar(this, SWT.V_SCROLL);
		expandBar.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		final ExpandItem newItemExpandItem = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem.setExpanded(true);
		newItemExpandItem.setHeight(400);
		newItemExpandItem.setText("All");

		final Composite composite = new Composite(expandBar, SWT.NONE);
		composite.setLayout(new FillLayout());
		newItemExpandItem.setControl(composite);

		allPackagesList = new List(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		allPackagesList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				String[] items = allPackagesList.getSelection();

			}
		});

		final ExpandItem expandItem = new ExpandItem(expandBar, SWT.NONE);
		expandItem.setHeight(60);
		expandItem.setText("Search");

		final Composite composite_3 = new Composite(expandBar, SWT.NONE);
		expandItem.setControl(composite_3);

		text = new Text(composite_3, SWT.BORDER);
		
		 btnContextSensitive = new Button(composite_3, SWT.CHECK);
		 btnContextSensitive.setText("Case Sensitive");
		 GroupLayout gl_composite_3 = new GroupLayout(composite_3);
		 gl_composite_3.setHorizontalGroup(
		 	gl_composite_3.createParallelGroup(GroupLayout.LEADING)
		 		.add(btnContextSensitive, GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE)
		 		.add(text, GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE)
		 );
		 gl_composite_3.setVerticalGroup(
		 	gl_composite_3.createParallelGroup(GroupLayout.LEADING)
		 		.add(gl_composite_3.createSequentialGroup()
		 			.add(text, GroupLayout.PREFERRED_SIZE, 23, GroupLayout.PREFERRED_SIZE)
		 			.addPreferredGap(LayoutStyle.UNRELATED)
		 			.add(btnContextSensitive)
		 			.add(9))
		 );
		 composite_3.setLayout(gl_composite_3);
		text.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event event) {
				if(btnContextSensitive.getSelection()){
				allPackagesList.deselectAll();
				text.getText();
				
				for (int i = 0; i < allPackagesList.getItemCount(); i++) {
					String it = allPackagesList.getItem(i);
					
					if (it.startsWith(text.getText())) {

						allPackagesList.select(i);
						allPackagesList.showSelection();
						return;
					}
				}	
				}
				
				else{
					allPackagesList.deselectAll();
					text.getText();
					
					for (int i = 0; i < allPackagesList.getItemCount(); i++) {
						 
						 String it = allPackagesList.getItem(i).toLowerCase();
							if (it.startsWith(text.getText().toLowerCase())) {

								allPackagesList.select(i);
								allPackagesList.showSelection();
								return;
							}
					}	
					
				}

				
			}
		});

		final ExpandItem newItemExpandItem_1 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_1.setHeight(150);
		newItemExpandItem_1.setText("Spatial");

		final Composite composite_1 = new Composite(expandBar, SWT.NONE);
		composite_1.setLayout(new FillLayout());
		newItemExpandItem_1.setControl(composite_1);

		spatialList = new List(composite_1, SWT.BORDER);
		spatialList.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent e) {
				String[] items = spatialList.getSelection();
				String[] info = map.get(items[0]);
				// Get the URL!

				URL url = null;
				try {
					url = new URL(info[1]);
				} catch (MalformedURLException e1) {

					e1.printStackTrace();
				}
				try {
					PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(url);
				} catch (PartInitException e1) {

					e1.printStackTrace();
				}

			}
		});
		spatialList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				String[] items = spatialList.getSelection();
				String[] info = map.get(items[0]);
				// Get the information!
				spatialList.setToolTipText(info[0]);

			}
		});
		spatialList.add("spatstat");
		spatialList.add("maptools");
		spatialList.add("gstat");
		spatialList.add("maps");
		spatialList.add("rgdal");
		spatialList.add("PBSmapping");
		spatialList.add("shapefiles");
		spatialList.add("RSAGA");
		spatialList.add("geoR");
		spatialList.add("geoRglm");
		spatialList.add("SDMTools");

		final ExpandItem newItemExpandItem_2 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_2.setHeight(30);
		newItemExpandItem_2.setExpanded(true);
		newItemExpandItem_2.setText("Update List");

		final Composite composite_2 = new Composite(expandBar, SWT.NONE);
		composite_2.setLayout(new FillLayout());
		newItemExpandItem_2.setControl(composite_2);

		final Button updateButton = new Button(composite_2, SWT.NONE);
		updateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RState.isBusy() == false) {
					RState.setBusy(true);
					ListRPackagesJob Do = new ListRPackagesJob();
					Do.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent event) {
							if (event.getResult().isOK()) {
								RState.setBusy(false);
							} else {
								RState.setBusy(false);
							}
						}
					});
					Do.setUser(true);
					Do.schedule();

				} else {

					Bio7Dialog.message("RServer is busy!");

				}

			}

		});
		updateButton.setText("Get List");

		final Button installButton = new Button(composite_2, SWT.NONE);
		installButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RState.isBusy() == false) {
					RState.setBusy(true);
					InstallRPackagesJob Do = new InstallRPackagesJob();
					Do.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent event) {
							if (event.getResult().isOK()) {
								RState.setBusy(false);
							} else {
								RState.setBusy(false);
							}
						}
					});
					Do.setUser(true);
					Do.schedule();

				} else {

					Bio7Dialog.message("RServer is busy!");

				}
			}
		});
		installButton.setText("Install Selected");

		//
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

	public static List getAllList() {
		return allPackagesList;
	}

	public static List getSpatialList() {
		return spatialList;
	}

}
