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

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.util.WWIO;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class LoadShapefileJob extends Job {

	protected String[] items;
	private String source;
	private Composite composite_4;
	private ShapefileLoader loader;
	private boolean openStreeMap;

	public LoadShapefileJob(String source, Composite composite) {
		super("Load Shapefile...");
		this.source = source;
		this.composite_4 = composite;

	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Load Shapefile..", IProgressMonitor.UNKNOWN);

		try {
			final List<Layer> layers = this.makeShapefileLayers();
			for (int i = 0; i < layers.size(); i++) {
				String name = this.makeDisplayName(this.source);
				layers.get(i).setName(i == 0 ? name : name + "-" + Integer.toString(i));
				layers.get(i).setPickEnabled(false);

			}

			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					LayerCompositeShapefile lc;
					for (Layer layer : layers) {
						if (openStreeMap) {
							lc = new LayerCompositeShapefile(composite_4, SWT.NONE, layer, OpenStreetMapShapefileLoader.getSect());
						} else {
							lc = new LayerCompositeShapefile(composite_4, SWT.NONE, layer, loader.getSect());
						}
						lc.setBounds(10, 10, 260, 60);
						WorldWindOptionsView.getOptionsInstance().computeScrolledSize();
						WorldWindOptionsView.insertBeforePlacenames(WorldWindView.getWwd(), layer);
						// appFrame.layers.add(layer);
					}

					// appFrame.layerPanel.update(appFrame.getWwd());
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Status.OK_STATUS;
	}

	protected List<Layer> makeShapefileLayers() {
		if (OpenStreetMapShapefileLoader.isOSMPlacesSource(this.source)) {
			openStreeMap = true;
			Layer layer = OpenStreetMapShapefileLoader.makeLayerFromOSMPlacesSource(source);
			List<Layer> layers = new ArrayList<Layer>();
			layers.add(layer);
			return layers;
		} else {
			openStreeMap = false;
			loader = new ShapefileLoader();

			// System.out.println(sect);
			return loader.createLayersFromSource(this.source);
		}
	}

	protected String makeDisplayName(Object source) {
		String name = WWIO.getSourcePath(source);
		if (name != null)
			name = WWIO.getFilename(name);
		if (name == null)
			name = "Shapefile";

		return name;
	}

}
