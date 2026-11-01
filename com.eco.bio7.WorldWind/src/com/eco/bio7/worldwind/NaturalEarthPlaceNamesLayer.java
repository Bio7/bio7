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
import java.awt.Font;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GeographicText;
import gov.nasa.worldwind.render.GeographicTextRenderer;
import gov.nasa.worldwind.render.UserFacingText;

/**
 * The place names (the populated places) of Natural Earth (public domain,
 * https://www.naturalearthdata.com), bundled with Bio7: the WFS of NASA
 * (worldwind22.arc.nasa.gov) is not available. The names are drawn as text
 * (sharp at every display scale): the larger places (a lower rank of Natural
 * Earth) from far away, more places when the eye is closer. Overlapping names
 * are culled by their priority (rank and population).
 */
public class NaturalEarthPlaceNamesLayer extends AbstractLayer {

	private static final String PATH = "/data/naturalearth/ne_10m_populated_places.csv";
	/* The highest rank for the eye altitudes (meters): far away only the world cities */
	private static final double[] ALTITUDES = { 1.2e7, 6e6, 3e6, 1.5e6, 8e5, 4e5, 2e5, 0 };
	private static final int[] MAX_RANKS = { 1, 2, 3, 4, 6, 7, 8, 10 };

	private record Place(UserFacingText text, int rank, double lat, double lon, int baseSize, int style) {
	}

	private final GeographicTextRenderer renderer = new GeographicTextRenderer();
	private List<Place> places;
	private double scale = 1;
	private double appliedScale = -1;

	public NaturalEarthPlaceNamesLayer() {
		setName("Place Names");
		setPickEnabled(false);
		renderer.setCullTextEnabled(true);
		renderer.setCullTextMargin(2);
		renderer.setEffect(AVKey.TEXT_EFFECT_OUTLINE);
		renderer.setDistanceMinScale(1);
		renderer.setDistanceMaxScale(1);
		renderer.setDistanceMinOpacity(1);
	}

	/** The scale of the fonts (e.g., 2 on a high DPI display). */
	public void setScale(double scale) {
		this.scale = scale;
	}

	@Override
	protected void doRender(DrawContext dc) {
		if (places == null) {
			places = load();
		}
		if (appliedScale != scale) {
			appliedScale = scale;
			for (Place p : places) {
				p.text().setFont(new Font(Font.SANS_SERIF, p.style(), (int) Math.round(p.baseSize() * scale)));
			}
		}
		double altitude = dc.getView().getEyePosition().getElevation();
		int maxRank = MAX_RANKS[MAX_RANKS.length - 1];
		for (int i = 0; i < ALTITUDES.length; i++) {
			if (altitude > ALTITUDES[i]) {
				maxRank = MAX_RANKS[i];
				break;
			}
		}
		Sector visible = dc.getVisibleSector();
		List<GeographicText> text = new ArrayList<>();
		for (Place p : places) {
			if (p.rank() <= maxRank && (visible == null || visible.containsDegrees(p.lat(), p.lon()))) {
				text.add(p.text());
			}
		}
		renderer.render(dc, text);
	}

	/* The places of the bundled CSV file (name, lat, lon, rank, capital, pop) */
	private static List<Place> load() {
		List<Place> list = new ArrayList<>();
		try (InputStream in = NaturalEarthPlaceNamesLayer.class.getResourceAsStream(PATH)) {
			if (in == null) {
				return list;
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			reader.readLine();
			String line;
			while ((line = reader.readLine()) != null) {
				String[] f = split(line);
				if (f.length < 6) {
					continue;
				}
				double lat = Double.parseDouble(f[1]);
				double lon = Double.parseDouble(f[2]);
				int rank = (int) Double.parseDouble(f[3]);
				boolean capital = "1".equals(f[4]);
				double pop = f[5].isEmpty() || "NA".equals(f[5]) ? 0 : Double.parseDouble(f[5]);
				UserFacingText t = new UserFacingText(f[0], Position.fromDegrees(lat, lon, 0));
				t.setColor(Color.WHITE);
				t.setBackgroundColor(Color.BLACK);
				/* The important places first (culling of overlapping names) */
				t.setPriority((10 - rank) * 10 + Math.min(9, Math.log10(pop + 1)));
				int size = rank <= 1 ? 15 : rank <= 4 ? 13 : 12;
				int style = capital || rank <= 2 ? Font.BOLD : Font.PLAIN;
				list.add(new Place(t, rank, lat, lon, size, style));
			}
		} catch (Exception e) {
			System.err.println("The place names could not be loaded: " + e.getMessage());
		}
		return list;
	}

	/* A line of the CSV file (quoted fields with commas) */
	private static String[] split(String line) {
		List<String> fields = new ArrayList<>();
		StringBuilder field = new StringBuilder();
		boolean quoted = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '"') {
				if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
					field.append('"');
					i++;
				} else {
					quoted = !quoted;
				}
			} else if (c == ',' && !quoted) {
				fields.add(field.toString());
				field.setLength(0);
			} else {
				field.append(c);
			}
		}
		fields.add(field.toString());
		return fields.toArray(new String[0]);
	}
}
