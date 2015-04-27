/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package com.eco.bio7.worldwind;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.formats.shapefile.Shapefile;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecord;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecordMultiPoint;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecordPoint;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecordPolygon;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecordPolyline;
import gov.nasa.worldwind.formats.shapefile.ShapefileUtils;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.ExtrudedPolygon;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfacePolygons;
import gov.nasa.worldwind.render.SurfacePolylines;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.VecBuffer;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.WWMath;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwindx.examples.util.RandomShapeAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts Shapefile geometry into World Wind renderable objects. Shapefile
 * geometries are mapped to World Wind objects as follows:
 * <table>
 * <tr>
 * <th>Shapefile Geometry</th>
 * <th>World Wind Object</th>
 * </tr>
 * <tr>
 * <td>Point</td>
 * <td>{@link gov.nasa.worldwind.render.WWIcon}</td>
 * </tr>
 * <tr>
 * <td>MultiPoint</td>
 * <td>List of {@link gov.nasa.worldwind.render.WWIcon}</td>
 * </tr>
 * <tr>
 * <td>Polyline</td>
 * <td>{@link gov.nasa.worldwind.render.SurfacePolylines}</td>
 * </tr>
 * <tr>
 * <td>Polygon</td>
 * <td>{@link gov.nasa.worldwind.render.SurfacePolygons}</td>
 * </tr>
 * </table>
 * <p/>
 * Shapefiles do not contain a standard definition for color and other visual
 * attributes. Though some Shapefiles contain color information in each record's
 * key-value attributes, ShapefileLoader does not attempt to interpret that
 * information. Instead, the World Wind renderable objects created by
 * ShapefileLoader are assigned a random color. Callers can replace or extend
 * this behavior by defining a subclass of ShapefileLoader and overriding the
 * following methods:
 * <ul>
 * <li>
 * {@link #createPointAttributes(gov.nasa.worldwind.formats.shapefile.ShapefileRecord)}
 * </li>
 * <li>
 * {@link #createPolylineAttributes(gov.nasa.worldwind.formats.shapefile.ShapefileRecord)}
 * </li>
 * <li>
 * {@link #createPolygonAttributes(gov.nasa.worldwind.formats.shapefile.ShapefileRecord)}
 * </li>
 * </ul>.
 *
 * @author dcollins
 * @version $Id: ShapefileLoader.java 1532 2013-08-06 23:54:50Z dcollins $
 */
public class ShapefileLoader {
	protected static final RandomShapeAttributes randomAttrs = new RandomShapeAttributes();

	/**
	 * Indicates the maximum number of polygons to place in a layer before
	 * creating an additional layer.
	 */
	protected int numPolygonsPerLayer = 5000;

	/** Constructs a ShapefileLoader, but otherwise does nothing. */
	private  Sector sect;

	public  Sector getSect() {
		return sect;
	}

	/** Constructs a ShapefileLoader, but otherwise does nothing. */
	public ShapefileLoader() {
	}

	/**
	 * Creates a {@link gov.nasa.worldwind.layers.Layer} from a general
	 * Shapefile source. The source type may be one of the following:
	 * <ul>
	 * <li>{@link java.io.InputStream}</li>
	 * <li>{@link java.net.URL}</li>
	 * <li>{@link java.io.File}</li>
	 * <li>{@link String} containing a valid URL description or a file or
	 * resource name available on the classpath.</li>
	 * </ul>
	 *
	 * @param source
	 *            the source of the Shapefile.
	 *
	 * @return a Layer that renders the Shapefile's contents on the surface of
	 *         the Globe.
	 *
	 * @throws IllegalArgumentException
	 *             if the source is null or an empty string, or if the
	 *             Shapefile's primitive type is unrecognized.
	 */
	public Layer createLayerFromSource(Object source) {
		if (WWUtil.isEmpty(source)) {
			String message = Logging.getMessage("nullValue.SourceIsNull");
			Logging.logger().severe(message);
			throw new IllegalArgumentException(message);
		}

		Shapefile shp = null;
		Layer layer = null;
		try {
			shp = new Shapefile(source);
			sect = Sector.fromDegrees((shp.getBoundingRectangle()));
			if (sect == null) {

			} else {

				WorldWindOptionsView.setMinLat(String.valueOf(sect.getMinLatitude().degrees));
				WorldWindOptionsView.setMinLon(String.valueOf(sect.getMinLongitude().degrees));
				WorldWindOptionsView.setMaxLat(String.valueOf(sect.getMaxLatitude().degrees));
				WorldWindOptionsView.setMaxLon(String.valueOf(sect.getMaxLongitude().degrees));

			}
			// System.out.println(sect);
			layer = this.createLayerFromShapefile(shp);
		} finally {
			WWIO.closeStream(shp, source.toString());
		}

		return layer;
	}

	/**
	 * Creates a list of {@link gov.nasa.worldwind.layers.Layer}s containing
	 * shapes from a Shapefile.
	 * <p/>
	 * For polygon shapes, the maximum number of polygons to add to a layer can
	 * be specified. By default it's 5000. If there are more polygons in the
	 * shapefile than the maximum, new layers are created as needed, with each
	 * holding no more than the maximum. All but the first layer is disabled to
	 * avoid bogging down the display trying to render an excessive number of
	 * shapes. The application can enable the layers selectively to maintain
	 * performance.
	 * <p/>
	 * The source type may be one of the following:
	 * <ul>
	 * <li>{@link java.io.InputStream}</li>
	 * <li>{@link java.net.URL}</li>
	 * <li>{@link java.io.File}</li>
	 * <li>{@link String} containing a valid URL description or a file or
	 * resource name available on the classpath.</li>
	 * </ul>
	 *
	 * @param source
	 *            the source of the Shapefile.
	 *
	 * @return a Layer that renders the Shapefile's contents on the surface of
	 *         the Globe.
	 *
	 * @throws IllegalArgumentException
	 *             if the source is null or an empty string, or if the
	 *             Shapefile's primitive type is unrecognized.
	 */
	public List<Layer> createLayersFromSource(Object source) {
		if (WWUtil.isEmpty(source)) {
			String message = Logging.getMessage("nullValue.SourceIsNull");
			Logging.logger().severe(message);
			throw new IllegalArgumentException(message);
		}

		Shapefile shp = null;

		try {
			shp = new Shapefile(source);
			sect = Sector.fromDegrees((shp.getBoundingRectangle()));
			if (sect == null) {

			} else {

				WorldWindOptionsView.setMinLat(String.valueOf(sect.getMinLatitude().degrees));
				WorldWindOptionsView.setMinLon(String.valueOf(sect.getMinLongitude().degrees));
				WorldWindOptionsView.setMaxLat(String.valueOf(sect.getMaxLatitude().degrees));
				WorldWindOptionsView.setMaxLon(String.valueOf(sect.getMaxLongitude().degrees));

			}

			
			return this.createLayersFromShapefile(shp);
		} finally {
			WWIO.closeStream(shp, source.toString());
		}
	}

	/**
	 * Creates a {@link gov.nasa.worldwind.layers.Layer} from a general
	 * Shapefile.
	 *
	 * @param shp
	 *            the Shapefile to create a layer for.
	 *
	 * @return a Layer that renders the Shapefile's contents on the surface of
	 *         the Globe.
	 *
	 * @throws IllegalArgumentException
	 *             if the Shapefile is null, or if the Shapefile's primitive
	 *             type is unrecognized.
	 */
	public Layer createLayerFromShapefile(Shapefile shp) {
		if (shp == null) {
			String message = Logging.getMessage("nullValue.ShapefileIsNull");
			Logging.logger().severe(message);
			throw new IllegalArgumentException(message);
		}

		Layer layer = null;

		if (Shapefile.isPointType(shp.getShapeType())) {
			layer = new RenderableLayer();
			this.addRenderablesForPoints(shp, (RenderableLayer) layer);
		} else if (Shapefile.isMultiPointType(shp.getShapeType())) {
			layer = new RenderableLayer();
			this.addRenderablesForMultiPoints(shp, (RenderableLayer) layer);
		} else if (Shapefile.isPolylineType(shp.getShapeType())) {
			layer = new RenderableLayer();
			this.addRenderablesForPolylines(shp, (RenderableLayer) layer);
		} else if (Shapefile.isPolygonType(shp.getShapeType())) {
			List<Layer> layers = new ArrayList<Layer>();
			this.addRenderablesForPolygons(shp, layers);
			layer = layers.get(0);
		} else {
			Logging.logger().warning(Logging.getMessage("generic.UnrecognizedShapeType", shp.getShapeType()));
		}

		return layer;
	}

	/**
	 * Creates a list of {@link gov.nasa.worldwind.layers.Layer}s containing
	 * shapes from a Shapefile.
	 * <p/>
	 * For polygon shapes, the maximum number of polygons to add to a layer can
	 * be specified. By default it's 5000. If there are more polygons in the
	 * shapefile than the maximum, new layers are created as needed, with each
	 * holding no more than the maximum. All but the first layer is disabled to
	 * avoid bogging down the display trying to render an excessive number of
	 * shapes. The application can enable the layers selectively to maintain
	 * performance.
	 * <p/>
	 *
	 * @param shp
	 *            the source of the Shapefile.
	 *
	 * @return a Layer that renders the Shapefile's contents on the surface of
	 *         the Globe.
	 *
	 * @throws IllegalArgumentException
	 *             if the shapefile is null or an empty string, or if the
	 *             Shapefile's primitive type is unrecognized.
	 */
	public List<Layer> createLayersFromShapefile(Shapefile shp) {
		if (shp == null) {
			String message = Logging.getMessage("nullValue.ShapefileIsNull");
			Logging.logger().severe(message);
			throw new IllegalArgumentException(message);
		}

		List<Layer> layers = new ArrayList<Layer>();

		if (Shapefile.isPointType(shp.getShapeType())) {
			Layer layer = new RenderableLayer();
			this.addRenderablesForPoints(shp, (RenderableLayer) layer);
			layers.add(layer);
		} else if (Shapefile.isMultiPointType(shp.getShapeType())) {
			Layer layer = new RenderableLayer();
			this.addRenderablesForMultiPoints(shp, (RenderableLayer) layer);
			layers.add(layer);
		} else if (Shapefile.isPolylineType(shp.getShapeType())) {
			Layer layer = new RenderableLayer();
			this.addRenderablesForPolylines(shp, (RenderableLayer) layer);
			layers.add(layer);
		} else if (Shapefile.isPolygonType(shp.getShapeType())) {
			this.addRenderablesForPolygons(shp, layers);
		} else {
			Logging.logger().warning(Logging.getMessage("generic.UnrecognizedShapeType", shp.getShapeType()));
		}

		return layers;
	}

	/**
	 * Indicates the maximum number of polygon renderables to place in a single
	 * layer. If this limit is exceeded an additional layer is added.
	 *
	 * @return the maximum number of polygons to place in a layer.
	 */
	public int getNumPolygonsPerLayer() {
		return this.numPolygonsPerLayer;
	}

	/**
	 * Specifies the maximum number of polygon renderables to place in a single
	 * layer. If this limit is exceeded an additional layer is added.
	 *
	 * @param numPolygonsPerLayer
	 *            the maximum number of polygons to place in a layer.
	 *
	 * @throws IllegalArgumentException
	 *             if the number is less than 1.
	 */
	public void setNumPolygonsPerLayer(int numPolygonsPerLayer) {
		if (numPolygonsPerLayer < 1) {
			String message = Logging.getMessage("generic.InvalidSize", numPolygonsPerLayer);
			Logging.logger().severe(message);
			throw new IllegalArgumentException(message);
		}

		this.numPolygonsPerLayer = numPolygonsPerLayer;
	}

	// **************************************************************//
	// ******************** Geometry Conversion *******************//
	// **************************************************************//

	protected void addRenderablesForPoints(Shapefile shp, RenderableLayer layer) {
		PointPlacemarkAttributes attrs = this.createPointAttributes(null);

		while (shp.hasNext()) {
			ShapefileRecord record = shp.nextRecord();

			if (!Shapefile.isPointType(record.getShapeType()))
				continue;

			double[] point = ((ShapefileRecordPoint) record).getPoint();
			layer.addRenderable(this.createPoint(record, point[1], point[0], attrs));
		}
	}

	protected void addRenderablesForMultiPoints(Shapefile shp, RenderableLayer layer) {
		PointPlacemarkAttributes attrs = this.createPointAttributes(null);

		while (shp.hasNext()) {
			ShapefileRecord record = shp.nextRecord();

			if (!Shapefile.isMultiPointType(record.getShapeType()))
				continue;

			Iterable<double[]> iterable = ((ShapefileRecordMultiPoint) record).getPoints(0);

			for (double[] point : iterable) {
				layer.addRenderable(this.createPoint(record, point[1], point[0], attrs));
			}
		}
	}

	protected void addRenderablesForPolylines(Shapefile shp, RenderableLayer layer) {
		// Reads all records from the Shapefile, but ignores each records unique
		// information. We do this to create one
		// WWJ object representing the entire shapefile, which as of 8/10/2010
		// is required to display very large
		// polyline Shapefiles. To create one WWJ object for each Shapefile
		// record, replace this method's contents with
		// the following:
		//
		// while (shp.hasNext())
		// {
		// ShapefileRecord record = shp.nextRecord();
		//
		// if (!Shapefile.isPolylineType(record.getShapeType()))
		// continue;
		//
		// ShapeAttributes attrs = this.createPolylineAttributes(record);
		// layer.addRenderable(this.createPolyline(record, attrs));
		// }

		while (shp.hasNext()) {
			shp.nextRecord();
		}

		ShapeAttributes attrs = this.createPolylineAttributes(null);
		layer.addRenderable(this.createPolyline(shp, attrs));
	}

	/**
	 * Creates renderables for all the polygons in the shapefile. Polygon is the
	 * only shape that potentially returns a more than one layer, which it does
	 * when the polygons per layer limit is exceeded.
	 *
	 * @param shp
	 *            the shapefile to read
	 * @param layers
	 *            a list in which to place the layers created. May not be null.
	 */
	protected void addRenderablesForPolygons(Shapefile shp, List<Layer> layers) {
		RenderableLayer layer = new RenderableLayer();
		layers.add(layer);

		int recordNumber = 0;
		while (shp.hasNext()) {
			try {
				ShapefileRecord record = shp.nextRecord();
				recordNumber = record.getRecordNumber();

				if (!Shapefile.isPolygonType(record.getShapeType()))
					continue;

				ShapeAttributes attrs = this.createPolygonAttributes(record);
				this.createPolygon(record, attrs, layer);

				if (layer.getNumRenderables() > this.numPolygonsPerLayer) {
					layer = new RenderableLayer();
					layer.setEnabled(false);
					layers.add(layer);
				}
			} catch (Exception e) {
				Logging.logger().warning(Logging.getMessage("SHP.ExceptionAttemptingToConvertShapefileRecord", recordNumber, e));
				// continue with the remaining records
			}
		}
	}

	// **************************************************************//
	// ******************** Primitive Geometry Construction *******//
	// **************************************************************//

	@SuppressWarnings({ "UnusedDeclaration" })
	protected Renderable createPoint(ShapefileRecord record, double latDegrees, double lonDegrees, PointPlacemarkAttributes attrs) {
		PointPlacemark placemark = new PointPlacemark(Position.fromDegrees(latDegrees, lonDegrees, 0));
		placemark.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
		placemark.setAttributes(attrs);

		return placemark;
	}

	protected Renderable createPolyline(ShapefileRecord record, ShapeAttributes attrs) {
		SurfacePolylines shape = new SurfacePolylines(Sector.fromDegrees(((ShapefileRecordPolyline) record).getBoundingRectangle()), record.getCompoundPointBuffer());
		shape.setAttributes(attrs);

		return shape;
	}

	protected Renderable createPolyline(Shapefile shp, ShapeAttributes attrs) {
		SurfacePolylines shape = new SurfacePolylines(Sector.fromDegrees(shp.getBoundingRectangle()), shp.getPointBuffer());
		shape.setAttributes(attrs);

		return shape;
	}

	protected void createPolygon(ShapefileRecord record, ShapeAttributes attrs, RenderableLayer layer) {
		Double height = this.getHeight(record);
		if (height != null) // create extruded polygons
		{
			ExtrudedPolygon ep = new ExtrudedPolygon(height);
			ep.setAttributes(attrs);
			layer.addRenderable(ep);

			for (int i = 0; i < record.getNumberOfParts(); i++) {
				// Although the shapefile spec says that inner and outer
				// boundaries can be listed in any order, it's
				// assumed here that inner boundaries are at least listed
				// adjacent to their outer boundary, either
				// before or after it. The below code accumulates inner
				// boundaries into the extruded polygon until an
				// outer boundary comes along. If the outer boundary comes
				// before the inner boundaries, the inner
				// boundaries are added to the polygon until another outer
				// boundary comes along, at which point a new
				// extruded polygon is started.

				VecBuffer buffer = record.getCompoundPointBuffer().subBuffer(i);
				if (WWMath.computeWindingOrderOfLocations(buffer.getLocations()).equals(AVKey.CLOCKWISE)) {
					if (!ep.getOuterBoundary().iterator().hasNext()) // has no
																		// outer
																		// boundary
																		// yet
					{
						ep.setOuterBoundary(buffer.getLocations());
					} else {
						ep = new ExtrudedPolygon();
						ep.setAttributes(attrs);
						ep.setOuterBoundary(record.getCompoundPointBuffer().getLocations());
						layer.addRenderable(ep);
					}
				} else {
					ep.addInnerBoundary(buffer.getLocations());
				}
			}
		} else // create surface polygons
		{
			SurfacePolygons shape = new SurfacePolygons(Sector.fromDegrees(((ShapefileRecordPolygon) record).getBoundingRectangle()), record.getCompoundPointBuffer());
			shape.setAttributes(attrs);
			// Configure the SurfacePolygons as a single large polygon.
			// Configure the SurfacePolygons to correctly interpret the
			// Shapefile polygon record. Shapefile polygons may
			// have rings defining multiple inner and outer boundaries. Each
			// ring's winding order defines whether it's an
			// outer boundary or an inner boundary: outer boundaries have a
			// clockwise winding order. However, the
			// arrangement of each ring within the record is not significant;
			// inner rings can precede outer rings and vice
			// versa.
			//
			// By default, SurfacePolygons assumes that the sub-buffers are
			// arranged such that each outer boundary precedes
			// a set of corresponding inner boundaries. SurfacePolygons
			// traverses the sub-buffers and tessellates a new
			// polygon each time it encounters an outer boundary. Outer
			// boundaries are sub-buffers whose winding order
			// matches the SurfacePolygons' windingRule property.
			//
			// This default behavior does not work with Shapefile polygon
			// records, because the sub-buffers of a Shapefile
			// polygon record can be arranged arbitrarily. By calling
			// setPolygonRingGroups(new int[]{0}), the
			// SurfacePolygons interprets all sub-buffers as boundaries of a
			// single tessellated shape, and configures the
			// GLU tessellator's winding rule to correctly interpret outer and
			// inner boundaries (in any arrangement)
			// according to their winding order. We set the SurfacePolygons'
			// winding rule to clockwise so that sub-buffers
			// with a clockwise winding ordering are interpreted as outer
			// boundaries.
			shape.setWindingRule(AVKey.CLOCKWISE);
			shape.setPolygonRingGroups(new int[] { 0 });
			shape.setPolygonRingGroups(new int[] { 0 });
			layer.addRenderable(shape);
		}
	}

	/**
	 * Get the height of a record.
	 *
	 * @param record
	 *            shape for which to find height
	 *
	 * @return the height of the shape, or {@code null} if there is no height.
	 */
	protected Double getHeight(ShapefileRecord record) {
		return ShapefileUtils.extractHeightAttribute(record);
	}

	// **************************************************************//
	// ******************** Attribute Construction ****************//
	// **************************************************************//

	@SuppressWarnings({ "UnusedDeclaration" })
	protected PointPlacemarkAttributes createPointAttributes(ShapefileRecord record) {
		return randomAttrs.nextPointAttributes();
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	protected ShapeAttributes createPolylineAttributes(ShapefileRecord record) {
		return randomAttrs.nextPolylineAttributes();
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	protected ShapeAttributes createPolygonAttributes(ShapefileRecord record) {
		return randomAttrs.nextPolygonAttributes();
	}
}
