/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.worldwind;

import java.awt.Color;
import java.io.InputStream;

import gov.nasa.worldwind.formats.shapefile.Shapefile;
import gov.nasa.worldwind.formats.shapefile.ShapefilePolylines;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.ShapeAttributes;

/**
 * The political boundaries (the land boundaries of the countries) of Natural
 * Earth (public domain, https://www.naturalearthdata.com), bundled with Bio7:
 * the WMS of NASA (worldwind22.arc.nasa.gov) is not available. The lines are
 * drawn as vectors (sharp at every zoom and display scale), the shapefile is
 * loaded when the layer is displayed the first time.
 */
public class NaturalEarthBoundariesLayer extends RenderableLayer {

	private static final String PATH = "/data/naturalearth/ne_10m_admin_0_boundary_lines_land";
	private static final Color COLOR = new Color(255, 214, 90);
	private static final double WIDTH = 1.5;

	private ShapefilePolylines lines;
	private boolean loaded;
	private double scale = 1;

	public NaturalEarthBoundariesLayer() {
		setName("Political Boundaries");
		setPickEnabled(false);
	}

	/** The scale of the line width (e.g., 2 on a high DPI display). */
	public void setScale(double scale) {
		this.scale = scale;
		applyAttributes();
	}

	@Override
	protected void doRender(DrawContext dc) {
		if (!loaded) {
			loaded = true;
			load();
		}
		super.doRender(dc);
	}

	private void load() {
		try (InputStream shp = stream(".shp"); InputStream shx = stream(".shx"); InputStream dbf = stream(".dbf")) {
			if (shp == null || shx == null || dbf == null) {
				return;
			}
			lines = new ShapefilePolylines(new Shapefile(shp, shx, dbf), attributes(), null, null);
			addRenderable(lines);
		} catch (Exception e) {
			System.err.println("The political boundaries could not be loaded: " + e.getMessage());
		}
	}

	private InputStream stream(String suffix) {
		return NaturalEarthBoundariesLayer.class.getResourceAsStream(PATH + suffix);
	}

	private ShapeAttributes attributes() {
		ShapeAttributes attrs = new BasicShapeAttributes();
		attrs.setDrawInterior(false);
		attrs.setDrawOutline(true);
		attrs.setOutlineMaterial(new Material(COLOR));
		attrs.setOutlineWidth(WIDTH * scale);
		return attrs;
	}

	/* The line width with the scale for all lines */
	private void applyAttributes() {
		if (lines == null) {
			return;
		}
		ShapeAttributes attrs = attributes();
		for (int i = 0; i < lines.getRecordCount(); i++) {
			lines.getRecord(i).setAttributes(attrs);
		}
	}
}
