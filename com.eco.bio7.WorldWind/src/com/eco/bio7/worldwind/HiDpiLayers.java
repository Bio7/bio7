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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;

import com.eco.bio7.worldwind.swt.WorldWindowNewtCanvasSWT;

import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import gov.nasa.worldwind.layers.AbstractGraticuleLayer;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.GraticuleRenderingParams;
import gov.nasa.worldwind.layers.GraticuleSupport;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.ScalebarLayer;
import gov.nasa.worldwind.layers.ViewControlsLayer;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.util.measure.MeasureTool;

/**
 * The screen layers (the compass, the scale bar and the view controls), the
 * graticules (the labels and the lines), the place names, the political
 * boundaries and the measure tool (the control points, the label and the line) for high DPI
 * displays: their sizes are in pixels of the GL surface, on a display with a
 * scale (e.g., 2 on a Retina display) they are too small. The sizes are
 * multiplied with the scale of the surface (also for layers added later and
 * after a change of the scale, e.g., the window was moved to another display).
 */
final class HiDpiLayers implements RenderingListener {

	/* The size of a layer without the scale and the applied scale */
	private static final String BASE = "bio7.hiDpi.base";
	private static final String SCALE = "bio7.hiDpi.scale";

	/* The sizes of the measure tool without the scale (see MeasureTool) */
	private static final int CONTROL_POINT = 8;
	private static final int LABEL_FONT = 14;
	private static final int LABEL_WIDTH = 220;
	private static final int LABEL_OFFSET = 10;
	private static final double LINE_WIDTH = 2;

	private final WorldWindowNewtCanvasSWT wwd;
	/* The scaled measure tool (a new tool is created, e.g., after the fullscreen mode) */
	private MeasureTool measureTool;
	private double measureScale;

	private HiDpiLayers(WorldWindowNewtCanvasSWT wwd) {
		this.wwd = wwd;
	}

	static void install(WorldWindowNewtCanvasSWT wwd) {
		wwd.addRenderingListener(new HiDpiLayers(wwd));
	}

	@Override
	public void stageChanged(RenderingEvent event) {
		if (!RenderingEvent.BEFORE_RENDERING.equals(event.getStage()) || wwd.getModel() == null) {
			return;
		}
		float[] surface = wwd.getWindow().getCurrentSurfaceScale(new float[2]);
		double scale = Math.max(1, surface[0]);
		for (Layer layer : wwd.getModel().getLayers()) {
			Object applied = layer.getValue(SCALE);
			if (applied instanceof Double d && d == scale) {
				continue;
			}
			if (scale(layer, scale)) {
				layer.setValue(SCALE, scale);
			}
		}
		MeasureTool tool = WorldWindOptionsView.measureTool;
		if (tool != null && (tool != measureTool || measureScale != scale)) {
			scale(tool, scale);
			measureTool = tool;
			measureScale = scale;
		}
	}

	/* The control points, the label (area, length, etc.) and the line of the measure tool with the scale */
	private static void scale(MeasureTool tool, double scale) {
		AnnotationAttributes points = tool.getControlPointsAttributes();
		int size = (int) Math.round(CONTROL_POINT * scale);
		points.setSize(new Dimension(size, size));
		points.setDrawOffset(new Point(0, -size / 2));
		AnnotationAttributes label = tool.getAnnotationAttributes();
		label.setFont(Font.decode("Arial-Bold-" + Math.round(LABEL_FONT * scale)));
		label.setSize(new Dimension((int) Math.round(LABEL_WIDTH * scale), 0));
		label.setDrawOffset(new Point(0, (int) Math.round(LABEL_OFFSET * scale)));
		tool.setLineWidth(LINE_WIDTH * scale);
	}

	/* The size of the layer with the scale, false for other layers */
	private static boolean scale(Layer layer, double scale) {
		if (layer instanceof CompassLayer compass) {
			double base = (Double) base(layer, compass.getIconScale());
			compass.setIconScale(base * scale);
		} else if (layer instanceof ScalebarLayer scalebar) {
			Object[] base = (Object[]) base(layer, new Object[] { scalebar.getSize(), scalebar.getFont() });
			Dimension size = (Dimension) base[0];
			Font font = (Font) base[1];
			scalebar.setSize(new Dimension((int) Math.round(size.width * scale), (int) Math.round(size.height * scale)));
			scalebar.setFont(font.deriveFont((float) (font.getSize2D() * scale)));
		} else if (layer instanceof ViewControlsLayer controls) {
			double base = (Double) base(layer, controls.getScale());
			controls.setScale(base * scale);
		} else if (layer instanceof AbstractGraticuleLayer graticule) {
			return scaleGraticule(graticule, scale);
		} else if (layer instanceof NaturalEarthPlaceNamesLayer names) {
			names.setScale(scale);
		} else if (layer instanceof NaturalEarthBoundariesLayer boundaries) {
			boundaries.setScale(scale);
		} else {
			return false;
		}
		return true;
	}

	/*
	 * The label fonts and the line widths of all levels of a graticule (e.g., the
	 * lat-lon and the MGRS graticule) with the scale. The rendering parameters are
	 * not public (by reflection), the cached line attributes are created again.
	 */
	@SuppressWarnings("unchecked")
	private static boolean scaleGraticule(AbstractGraticuleLayer layer, double scale) {
		try {
			java.lang.reflect.Field supportField = AbstractGraticuleLayer.class.getDeclaredField("graticuleSupport");
			supportField.setAccessible(true);
			GraticuleSupport support = (GraticuleSupport) supportField.get(layer);
			java.util.Map<String, Object[]> base = (java.util.Map<String, Object[]>) base(layer, new java.util.HashMap<String, Object[]>());
			for (java.util.Map.Entry<String, GraticuleRenderingParams> entry : support.getAllRenderingParams()) {
				GraticuleRenderingParams params = entry.getValue();
				Object[] values = base.computeIfAbsent(entry.getKey(), _ -> new Object[] { params.getLabelFont(), params.getLineWidth() });
				Font font = (Font) values[0];
				if (font != null) {
					params.setLabelFont(font.deriveFont((float) (font.getSize2D() * scale)));
				}
				double width = (Double) values[1];
				if (width > 0) {
					params.setLineWidth(width * scale);
				}
			}
			java.lang.reflect.Field attributes = GraticuleSupport.class.getDeclaredField("namedShapeAttributes");
			attributes.setAccessible(true);
			((java.util.Map<?, ?>) attributes.get(support)).clear();
			return true;
		} catch (ReflectiveOperationException | RuntimeException e) {
			/* Another version of WorldWind: the sizes of the graticule */
			return false;
		}
	}

	/* The size of the layer without the scale (stored when the layer is scaled the first time) */
	private static Object base(Layer layer, Object current) {
		Object base = layer.getValue(BASE);
		if (base == null) {
			base = current;
			layer.setValue(BASE, base);
		}
		return base;
	}
}
