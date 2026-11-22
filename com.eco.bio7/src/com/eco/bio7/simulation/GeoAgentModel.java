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

import java.util.ArrayList;

/**
 * A simulation of agents on the Earth (e.g., migrating animals, cities in an
 * epidemic, ships): the agents ({@link GeoAgent}) have a longitude and a
 * latitude and are displayed as points on the WorldWind globe of Bio7 (the
 * WorldWind perspective is opened) and on a map in the HTML export (online
 * base maps).
 *
 * <pre>
 * public class Walkers extends GeoAgentModel&lt;GeoAgent&gt; {
 * 	public void init() {
 * 		area(51.6, 10.5, 51.9, 10.9);
 * 		for (int i = 0; i &lt; 50; i++)
 * 			agents.add(new GeoAgent(random(10.5, 10.9), random(51.6, 51.9)));
 * 	}
 *
 * 	public void step() {
 * 		for (GeoAgent a : agents)
 * 			move(a, random(-0.5, 0.5), random(-0.5, 0.5)); // km to the east and north
 * 	}
 * }
 * </pre>
 *
 * @param <A> the class of the agents
 */
public abstract class GeoAgentModel<A extends GeoAgent> extends Simulation {

	/** The mean radius of the Earth in km. */
	public static final double EARTH_RADIUS = 6371.0088;

	/** The agents. */
	public ArrayList<A> agents = new ArrayList<>();

	/** The area of the view (WGS84 degrees). */
	public double south = -60, west = -180, north = 75, east = 180;

	/** The radius of the points in pixels (agents without an own size). */
	public double agentSize = 4;

	/** The color of the agents without an own color. */
	public String agentColor = "#d62728";

	/**
	 * The display of the agents on the globe: "point" (default), or a 3D shape
	 * (OpenGL, lighted, in the altitude of the agent): "sphere", "cone" (pointing in
	 * the heading), "box" or "cylinder". The HTML map shows points.
	 */
	public String agentShape = "point";

	/** The radius of the 3D shapes in meters. */
	public double agentSizeMeters = 300;

	/** The pitch of the view of the globe in degrees (0: from above, e.g. 60: an oblique view). */
	public double viewPitch = 0;

	private final GlobeDisplay globe = new GlobeDisplay();
	private final java.util.IdentityHashMap<GeoAgent, double[]> last = new java.util.IdentityHashMap<>();
	private boolean flown;

	/** Sets the area of the view (the globe flies to it, the map shows it). */
	public void area(double south, double west, double north, double east) {
		this.south = south;
		this.west = west;
		this.north = north;
		this.east = east;
		flown = false;
	}

	/** The distance of two agents in km (great circle). */
	public double distanceKm(GeoAgent a, GeoAgent b) {
		return distanceKm(a.lon, a.lat, b.lon, b.lat);
	}

	/**
	 * The distance of two positions (longitude, latitude in degrees) in km (great
	 * circle; with the trigonometric functions of Java or the browser, the last
	 * digits can differ in the HTML export).
	 */
	public static double distanceKm(double lon1, double lat1, double lon2, double lat2) {
		double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
		double dp = p2 - p1, dl = Math.toRadians(lon2 - lon1);
		double h = StrictMath.sin(dp / 2) * StrictMath.sin(dp / 2) + StrictMath.cos(p1) * StrictMath.cos(p2) * StrictMath.sin(dl / 2) * StrictMath.sin(dl / 2);
		return 2 * EARTH_RADIUS * StrictMath.asin(Math.min(1, Math.sqrt(h)));
	}

	/* The coefficients of the cosine series (1 / (2k)! with the sign) */
	private static final double[] COS = { 1.0, -0.5, 0.041666666666666664, -0.001388888888888889, 2.48015873015873e-05, -2.755731922398589e-07, 2.08767569878681e-09, -1.1470745597729725e-11, 4.779477332387385e-14, -1.5619206968586225e-16, 4.110317623312165e-19, -8.896791392450574e-22 };

	/**
	 * The cosine of a latitude in degrees (-90 to 90) with a series of the same
	 * operations in Java and JavaScript (the same digits in Bio7 and the browser,
	 * the error is below 1e-15).
	 */
	public static double cosLat(double latDeg) {
		double x = latDeg * (Math.PI / 180), x2 = x * x, c = COS[11];
		for (int k = 10; k >= 0; k--) {
			c = c * x2 + COS[k];
		}
		return c;
	}

	/** The cosine of an angle in degrees (the same digits in Java and JavaScript). */
	public static double cosDeg(double deg) {
		double d = deg % 360;
		if (d > 180) {
			d -= 360;
		} else if (d < -180) {
			d += 360;
		}
		d = Math.abs(d);
		return d <= 90 ? cosLat(d) : -cosLat(180 - d);
	}

	/** The sine of an angle in degrees (the same digits in Java and JavaScript). */
	public static double sinDeg(double deg) {
		return cosDeg(90 - deg);
	}

	/** Moves the agent by km to the east and to the north. */
	public void move(GeoAgent a, double eastKm, double northKm) {
		a.lat += northKm / 110.574;
		a.lon += eastKm / (111.320 * cosLat(a.lat));
		if (a.lon > 180) {
			a.lon -= 360;
		} else if (a.lon < -180) {
			a.lon += 360;
		}
		a.lat = Math.max(-89.9, Math.min(89.9, a.lat));
	}

	/**
	 * Moves the agent towards the position by at most km (on the straight line of
	 * the map, which is precise enough for short steps).
	 *
	 * @return true if the agent reached the position
	 */
	public boolean moveTowards(GeoAgent a, double lon, double lat, double km) {
		double eastKm = (lon - a.lon) * 111.320 * cosLat((lat + a.lat) / 2);
		double northKm = (lat - a.lat) * 110.574;
		double d = Math.sqrt(eastKm * eastKm + northKm * northKm);
		if (d <= km) {
			a.lon = lon;
			a.lat = lat;
			return true;
		}
		move(a, eastKm / d * km, northKm / d * km);
		return false;
	}

	/** The other agents within the radius (km) of the agent. */
	public ArrayList<A> neighborsKm(A agent, double km) {
		ArrayList<A> found = new ArrayList<>();
		for (int i = 0; i < agents.size(); i++) {
			A o = agents.get(i);
			if (o != agent && distanceKm(agent, o) <= km) {
				found.add(o);
			}
		}
		return found;
	}

	@Override
	String[] displayViews() {
		return new String[] { GlobeDisplay.VIEW };
	}

	@Override
	String[] optionsView() {
		return new String[] { GlobeDisplay.OPTIONS_VIEW, "Options WorldWind" };
	}

	@Override
	protected void prepareDisplay() {
		agents = new ArrayList<>();
		flown = false;
		last.clear();
		if (!arrangeViews) {
			GlobeDisplay.open();
		}
	}

	@Override
	protected void display() {
		if (!flown) {
			flown = true;
			globe.flyTo(south, west, north, east, viewPitch);
		}
		int n = agents.size();
		double[] lon = new double[n], lat = new double[n], sizes = new double[n], altitude = new double[n], heading = new double[n], meters = new double[n];
		String[] colors = new String[n], labels = new String[n], images = new String[n], models = new String[n], animations = new String[n];
		java.util.IdentityHashMap<GeoAgent, double[]> now = new java.util.IdentityHashMap<>();
		for (int i = 0; i < n; i++) {
			A a = agents.get(i);
			lon[i] = a.lon;
			lat[i] = a.lat;
			colors[i] = a.color == null ? agentColor : a.color;
			sizes[i] = a.size > 0 ? a.size : agentSize;
			labels[i] = a.label;
			images[i] = a.image;
			models[i] = a.model;
			animations[i] = a.animation;
			altitude[i] = a.altitude;
			meters[i] = agentSizeMeters;
			/* The heading: given, else the direction of the last movement (only for the display) */
			double[] before = last.get(a);
			double h = before == null ? 0 : before[2];
			if (before != null && (before[0] != a.lon || before[1] != a.lat)) {
				h = Math.toDegrees(Math.atan2((a.lon - before[0]) * Math.cos(Math.toRadians(a.lat)), a.lat - before[1]));
			}
			heading[i] = Double.isNaN(a.heading) ? h : a.heading;
			now.put(a, new double[] { a.lon, a.lat, heading[i] });
		}
		synchronized (last) {
			last.clear();
			last.putAll(now);
		}
		/* The agents (points, images, 3D shapes, glTF models) and the shapes (longitude and latitude, the radius in km) */
		GlobeDisplay.Agents shown = new GlobeDisplay.Agents(lon, lat, colors, sizes, labels, altitude, heading, meters, images, models, animations, agentShape, getImageFiles(),
				getModelFiles());
		globe.showAgents(shown, new GlobeDisplay.Drawing(getDrawing(), getImageFiles(), (x, y) -> new double[] { x, y }, 1000), this::display);
	}
}
