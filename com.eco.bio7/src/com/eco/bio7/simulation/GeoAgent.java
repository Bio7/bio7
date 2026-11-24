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
package com.eco.bio7.simulation;

/**
 * An agent of a {@link GeoAgentModel}: a geographic position (longitude and
 * latitude in degrees, WGS84), a color (e.g., "#d62728", null: the color of
 * the model), a size (the radius of the point in pixels, 0: the size of the
 * model), an optional label, an altitude and a heading (for the 3D shapes on
 * the WorldWind globe). Subclasses add their own fields and methods.
 */
public class GeoAgent {

	/** The position: longitude and latitude in degrees (WGS84). */
	public double lon, lat;

	/** The color (e.g., "#d62728"; null: the color of the model, agentColor). */
	public String color;

	/** The radius of the point in pixels (0: the size of the model, agentSize). */
	public double size;

	/** A text beside the agent (null: none). */
	public String label;

	/** The altitude above the ground in meters (for the 3D shapes on the globe). */
	public double altitude;

	/** The heading in degrees (0 north, 90 east; NaN: the direction of the last movement). */
	public double heading = Double.NaN;

	/** An image (loaded with loadImage) instead of the point (its size: size in pixels; null: none). */
	public String image;

	/** A 3D model (glTF, loaded with loadModel) on the globe (its height: agentSizeMeters; null: none). */
	public String model;

	/** The animation of the model (its name or number, null: the first animation, "none": still). */
	public String animation;

	/** An agent at longitude 0, latitude 0. */
	public GeoAgent() {
	}

	/** An agent at the position (degrees). */
	public GeoAgent(double lon, double lat) {
		this.lon = lon;
		this.lat = lat;
	}
}
