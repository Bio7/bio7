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

import java.net.MalformedURLException;
import java.net.URL;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.mercator.BasicMercatorTiledImageLayer;
import gov.nasa.worldwind.layers.mercator.MercatorSector;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileUrlBuilder;

/**
 * The standard map of OpenStreetMap ((c) OpenStreetMap contributors, the tile
 * server of the OpenStreetMap Foundation) as Web Mercator tiles: the tiles are
 * conformal (not distorted like a map of OpenStreetMap in geographic
 * coordinates). The tiles are split until a pixel of the map has the size of a
 * pixel of the display (sharp, also on a high DPI display). Bio7 identifies
 * itself (the user agent of the WorldWind retrievals) as required by the tile
 * usage policy of OpenStreetMap (https://operations.osmfoundation.org/policies/tiles/).
 */
public class OSMTileLayer extends BasicMercatorTiledImageLayer {

	private static final String SERVICE = "https://tile.openstreetmap.org/";
	private static final int TILE_SIZE = 256;
	/* The tile levels: level 0 is the zoom level 3 of the web maps (8 x 8 tiles), up to zoom level 19 */
	private static final int FIRST_ZOOM = 3;

	public OSMTileLayer() {
		super(makeLevels());
		setName("OpenStreetMap");
		setUseTransparentTextures(false);
	}
	/*
	 * The split scale of the Mercator layer for the pixel size of the display: the
	 * layer splits a tile while (pi / 20) * tile height > distance * 10^-splitScale,
	 * a pixel of the map has the size of a pixel of the display if the tile height
	 * is 256 * pixel size (pixel size = distance * pixel size at distance 1).
	 */
	@Override
	public void render(DrawContext dc) {
		double pixelSize = dc.getView().computePixelSizeAtDistance(1);
		if (pixelSize > 0) {
			setSplitScale(-Math.log10(Math.PI / 20 * TILE_SIZE * pixelSize));
		}
		super.render(dc);
	}

	private static LevelSet makeLevels() {
		AVList params = new AVListImpl();
		params.setValue(AVKey.TILE_WIDTH, TILE_SIZE);
		params.setValue(AVKey.TILE_HEIGHT, TILE_SIZE);
		params.setValue(AVKey.DATA_CACHE_NAME, "Earth/OpenStreetMap-Tiles");
		params.setValue(AVKey.SERVICE, SERVICE);
		params.setValue(AVKey.DATASET_NAME, "osm");
		params.setValue(AVKey.FORMAT_SUFFIX, ".png");
		params.setValue(AVKey.NUM_LEVELS, 17);
		params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
		params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(22.5d), Angle.fromDegrees(45d)));
		params.setValue(AVKey.SECTOR, new MercatorSector(-1.0, 1.0, Angle.NEG180, Angle.POS180));
		params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder());
		return new LevelSet(params);
	}

	/* The URL of a tile: zoom/x/y of the web maps */
	private static class URLBuilder implements TileUrlBuilder {
		public URL getURL(Tile tile, String imageFormat) throws MalformedURLException {
			int zoom = tile.getLevelNumber() + FIRST_ZOOM;
			int y = (1 << zoom) - 1 - tile.getRow();
			return new URL(tile.getLevel().getService() + zoom + "/" + tile.getColumn() + "/" + y + ".png");
		}
	}
}
