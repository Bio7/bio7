package com.eco.bio7.image;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

/**
 * 
 * @author Sun Ning/SNDA
 * @since 2010-3-3
 */
public class DelaunayAndVoronoiApp {

	private static boolean intersection = false;

	public static boolean isIntersection() {
		return intersection;
	}

	public static void setIntersection(boolean intersection) {
		DelaunayAndVoronoiApp.intersection = intersection;
	}

	/**
	 * create some predefined sites
	 * 
	 * @return
	 */
	public static Collection<Coordinate> getPredefinedSites() {
		// double[][] coords = {{100,27},{28, 50},{29, 40},{32, 90}, {12, 26}};
		Point2D.Double[] ps = PointPanel.getPoints();
		ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>(ps.length);

		for (int i = 0; i < ps.length; i++) {
			coordinates.add(new Coordinate(ps[i].getX(), ps[i].getY()));
		}
		ps = null;
		return coordinates;
	}

	/**
	 * 
	 * @param coords
	 * @return a geometry collection of triangulations
	 */
	public static Geometry buildDelaunayTriangulation(Collection<Coordinate> coords) {
		DelaunayTriangulationBuilder builder = new DelaunayTriangulationBuilder();
		builder.setSites(coords);
		return builder.getTriangles(new GeometryFactory());
	}

	/**
	 * 
	 * @param coords
	 * @return a collection of polygons
	 */
	public static Geometry buildVoronoiDiagram(Collection<Coordinate> coords) {
		VoronoiDiagramBuilder builder = new VoronoiDiagramBuilder();

		// builder.setClipEnvelope(new Envelope(0,100,0,100));

		builder.setSites(coords);

		// Coordinate[] coordinates = new Coordinate[] { new Coordinate(0, 0),
		// new Coordinate(1000, 0), new Coordinate(1000, 1000), new
		// Coordinate(0, 1000), new Coordinate(0, 0) };
		Coordinate[] coordinates = new Coordinate[] { new Coordinate(0, 0), new Coordinate(ImageMethods.getFieldX(), 0), new Coordinate(ImageMethods.getFieldX(), ImageMethods.getFieldY()),
				new Coordinate(0, ImageMethods.getFieldY()), new Coordinate(0, 0) };

		GeometryFactory factory = new GeometryFactory();

		/*
		 * WKTReader reader = new WKTReader( geometryFactory );
		 * 
		 * Polygon polygon = null; try { polygon = (Polygon)
		 * reader.read("POLYGON((0 0, 1000 1000, 0 0, 1000 1000))"); } catch
		 * (ParseException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 */

		LinearRing ring = factory.createLinearRing(coordinates);
		Polygon polygon = factory.createPolygon(ring, null);

		if (intersection == true) {

			return builder.getDiagram(new GeometryFactory()).intersection(polygon);
		} else {
			return builder.getDiagram(new GeometryFactory());
		}
	}

}