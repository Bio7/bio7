/*******************************************************************************
 * Copyright (c) 2007-2017 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.worldwind;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import gov.nasa.worldwind.formats.shapefile.ShapefileLayerFactory;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecord;
import gov.nasa.worldwind.formats.shapefile.ShapefileRenderable;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwindx.examples.util.RandomShapeAttributes;

public class LoadShapefileJob extends Job {

	protected String[] items;
	private String source;
	private Composite composite_4;
	private ShapefileLayerFactory factory;

	public LoadShapefileJob(String source, Composite composite) {
		super("Load Shapefile...");
		this.source = source;
		this.composite_4 = composite;

	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Load Shapefile..", IProgressMonitor.UNKNOWN);

		factory = new ShapefileLayerFactory();

		// Specify an attribute delegate to assign random attributes to each
		// shapefile record.
		final RandomShapeAttributes randomAttrs = new RandomShapeAttributes();
		factory.setAttributeDelegate(new ShapefileRenderable.AttributeDelegate() {
			@Override
			public void assignAttributes(ShapefileRecord shapefileRecord, ShapefileRenderable.Record renderableRecord) {
				renderableRecord.setAttributes(randomAttrs.nextAttributes().asShapeAttributes());
			}
		});

		factory.createFromShapefileSource(source, new ShapefileLayerFactory.CompletionCallback() {
			@Override
			public void completion(Object result) {

				final Layer layer = (Layer) result; // the result is the layer
													// the factory created
				layer.setPickEnabled(false);
				layer.setName(WWIO.getFilename(layer.getName()));

				Display display = PlatformUI.getWorkbench().getDisplay();
				display.syncExec(new Runnable() {

					public void run() {

						LayerCompositeShapefile lc;

						lc = new LayerCompositeShapefile(composite_4, SWT.NONE, layer, factory.getSect());

						lc.setBounds(10, 10, 260, 60);
						WorldWindOptionsView.getOptionsInstance().computeScrolledSize();
						WorldWindOptionsView.insertBeforePlacenames(WorldWindView.getWwd(), layer);

					}
				});

			}

			@Override
			public void exception(Exception e) {
				Logging.logger().log(java.util.logging.Level.SEVERE, e.getMessage(), e);
			}
		});

		return Status.OK_STATUS;
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
