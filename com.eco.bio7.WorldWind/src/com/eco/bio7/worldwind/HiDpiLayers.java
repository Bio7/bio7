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

import com.eco.bio7.worldwind.swt.WorldWindowNewtCanvasSWT;

import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.ScalebarLayer;
import gov.nasa.worldwind.layers.ViewControlsLayer;

/**
 * The screen layers (the compass, the scale bar and the view controls) for high
 * DPI displays: their sizes are in pixels of the GL surface, on a display with
 * a scale (e.g., 2 on a Retina display) they are too small. The sizes are
 * multiplied with the scale of the surface (also for layers added later and
 * after a change of the scale, e.g., the window was moved to another display).
 */
final class HiDpiLayers implements RenderingListener {

	/* The size of a layer without the scale and the applied scale */
	private static final String BASE = "bio7.hiDpi.base";
	private static final String SCALE = "bio7.hiDpi.scale";

	private final WorldWindowNewtCanvasSWT wwd;

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
		} else {
			return false;
		}
		return true;
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
