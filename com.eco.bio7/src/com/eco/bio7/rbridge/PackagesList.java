/*******************************************************************************
 * Copyright (c) 2005-2016 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rbridge;

import java.util.HashMap;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.util.Util;

public class PackagesList extends Shell {

	private Text text;
	private static List allPackagesList;
	private HashMap<String, String[]> map = new HashMap<String, String[]>();

	public PackagesList(Display display, int style) {
		super(display, style);
		createContents();

		// setImage(SWTResourceManager.getImage(PackagesList.class,
		// "/pics/logo.gif"));

		allPackagesList = new List(this, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		allPackagesList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 6));

		allPackagesList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				String[] items = allPackagesList.getSelection();

			}
		});

		Label lblSearch = new Label(this, SWT.NONE);
		lblSearch.setText("Search");

		Button btnContextSensitive = new Button(this, SWT.CHECK);
		btnContextSensitive.setText("Context Sensitive");

		text = new Text(this, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		text.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event event) {
				if (btnContextSensitive.getSelection()) {
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

				else {
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
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);

		final Button updateButton = new Button(this, SWT.NONE);
		GridData gd_updateButton = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_updateButton.heightHint = 40;
		updateButton.setLayoutData(gd_updateButton);
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

					Bio7Dialog.message("Rserve is busy!");

				}

			}

		});
		updateButton.setText("Get List");

		final Button installButton = new Button(this, SWT.NONE);
		GridData gd_installButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_installButton.heightHint = 40;
		installButton.setLayoutData(gd_installButton);
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

					Bio7Dialog.message("Rserve is busy!");

				}
			}
		});
		installButton.setText("Install Selected");
		map.clear();
		map.put("spatstat", new String[] { "A package for the statistical analysis of spatial data, \n mainly spatial point patterns.", "http://www.spatstat.org" });
		map.put("raster", new String[] { "A package for reading, writing, manipulating, analyzing and modeling of gridded spatial data.", "https://cran.r-project.org/web/packages/raster/index.html" });
		map.put("maptools", new String[] { "Tools for reading and handling spatial objects", "http://cran.r-project.org/web/packages/maptools/index.html" });
		map.put("gstat", new String[] { "Gstat is an open source (GPL) computer code for multivariable \n geostatistical modelling, prediction and simulation", "http://www.gstat.org/" });
		map.put("maps", new String[] { "Display of maps. Projection code and larger maps are in separate packages (mapproj and mapdata).", "http://cran.r-project.org/web/packages/maps/index.html" });
		map.put("rgdal", new String[] { "Provides bindings to Frank Warmerdam's Geospatial Data Abstraction Library (GDAL) (>= 1.3.1)\n and access to projection/transformation operations from the PROJ.4 library.", "http://cran.r-project.org/web/packages/rgdal/index.html" });
		map.put("PBSmapping",
				new String[] {
						"This software has evolved from fisheries research conducted at the Pacific Biological Station (PBS) in Nanaimo, British Columbia, Canada.\n It extends the R language to include two-dimensional plotting features similar to those commonly available in a Geographic Information System (GIS).",
						"http://cran.r-project.org/web/packages/PBSmapping/index.html" });
		map.put("shapefiles", new String[] { "Functions to read and write ESRI shapefiles.", "http://cran.r-project.org/web/packages/shapefiles/index.html" });
		map.put("RSAGA", new String[] { "RSAGA provides access to geocomputing and terrain analysis functions of SAGA from within R\n by running the command line version of SAGA.", "http://cran.r-project.org/web/packages/RSAGA/index.html" });
		map.put("geoR", new String[] { "Geostatistical analysis including traditional, likelihood-based and Bayesian methods.", "http://leg.ufpr.br/geoR/" });
		map.put("geoRglm", new String[] { "Functions for inference in generalised linear spatial models. \nThe posterior and predictive inference is based on Markov chain Monte Carlo methods.", "http://gbi.agrsci.dk/~ofch/geoRglm/" });
		map.put("SDMTools",
				new String[] {
						"This packages provides a set of tools for post processing the outcomes\nof species distribution modeling exercises. It includes novel methods for comparing models\nand tracking changes in distributions through time.\nIt further includes methods for visualizing outcomes, selecting thresholds, calculating measures\nof accuracy and landscape fragmentation statistics, etc.",
						"http://cran.r-project.org/web/packages/SDMTools/" });

	}

	/**
	 * Create contents of the window
	 */
	protected void createContents() {
		setText("Packages");
		setSize(306, 730);
		setLayout(new GridLayout(2, true));
       // this.setBackground(Util.getSWTBackgroundColor());
		//
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

	public static List getAllList() {
		return allPackagesList;
	}

}
