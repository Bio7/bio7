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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.eco.bio7.collection.Work;
import com.eco.bio7.worldwind.WorldWindView;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.SurfaceImage;

/**
 * The display of a geospatial model on the WorldWind globe of Bio7: a layer
 * "Simulation" with the grid as a surface image (with the alpha of the colors)
 * and the agents as points. The updates are made in the thread of the user
 * interface (at most one waits, the last state is displayed).
 */
final class GlobeDisplay {

	private static final String LAYER = "Simulation";

	/** The view of the WorldWind globe and its options. */
	static final String VIEW = WorldWindView.ID, OPTIONS_VIEW = "com.eco.bio7.worldwind.WorldWindOptionsView";

	private RenderableLayer layer;
	private SurfaceImage image;
	private final List<PointPlacemark> points = new ArrayList<>();
	private final Map<String, PointPlacemarkAttributes> attributes = new HashMap<>();
	private final AtomicBoolean pending = new AtomicBoolean();
	private volatile Runnable again;

	/** Opens the WorldWind perspective (in the thread of the user interface). */
	static void open() {
		if (Simulation.hasDisplay() && WorldWindView.getWwd() == null) {
			Work.openPerspective("com.eco.bio7.WorldWind.3dglobe");
		}
	}

	/** Shows the globe (in front of the other views of its stack) and flies to the center of the area. */
	void flyTo(double south, double west, double north, double east) {
		flyTo(south, west, north, east, 0);
	}

	/** Flies to the area with a pitch of the view (0: from above, e.g. 60: an oblique view of 3D shapes). */
	void flyTo(double south, double west, double north, double east, double pitch) {
		Simulation.ui(() -> flyNow(south, west, north, east, pitch, 30));
	}

	private void flyNow(double south, double west, double north, double east, double pitch, int tries) {
		WorldWindow wwd = WorldWindView.getWwd();
		/* The view has a globe after its first rendering */
		if (wwd == null || wwd.getView().getGlobe() == null) {
			if (tries > 0) {
				org.eclipse.swt.widgets.Display.getCurrent().timerExec(300, () -> flyNow(south, west, north, east, pitch, tries - 1));
			}
			return;
		}
		bringToTop();
		/* Again after the other views of the model (e.g., the settings or ImageJ) are opened */
		org.eclipse.swt.widgets.Display.getCurrent().timerExec(800, GlobeDisplay::bringToTop);
		double km = Math.max((north - south) * 111.0, (east - west) * 111.0 * Math.cos(Math.toRadians((south + north) / 2)));
		wwd.getView().stopAnimations();
		Position center = Position.fromDegrees((south + north) / 2, (west + east) / 2, 0);
		double zoom = Math.max(5000, km * 1000 * 1.8);
		if (pitch > 0 && wwd.getView() instanceof gov.nasa.worldwind.view.orbit.BasicOrbitView orbit) {
			orbit.addPanToAnimator(center, gov.nasa.worldwind.geom.Angle.ZERO, gov.nasa.worldwind.geom.Angle.fromDegrees(pitch), zoom * 0.8);
		} else {
			wwd.getView().goTo(center, zoom);
		}
	}

	/* The globe in front of the other views of its stack */
	private static void bringToTop() {
		try {
			org.eclipse.ui.IWorkbenchPage page = org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			org.eclipse.ui.IViewPart view = page.findView(WorldWindView.ID);
			if (view != null) {
				page.bringToTop(view);
			}
		} catch (RuntimeException e) {
			/* No page: the globe is not brought to the front */
		}
	}

	private RenderableLayer layer(WorldWindow wwd) {
		if (layer == null || !wwd.getModel().getLayers().contains(layer)) {
			/* A former layer of a simulation (e.g., of the last compilation) is removed */
			for (Layer l : new ArrayList<>(wwd.getModel().getLayers())) {
				if (LAYER.equals(l.getName())) {
					wwd.getModel().getLayers().remove(l);
				}
			}
			layer = new RenderableLayer();
			layer.setName(LAYER);
			layer.setPickEnabled(false);
			WorldWindView.insertBeforeCompass(wwd, layer);
			image = null;
			points.clear();
			shapes.clear();
			shapeKind = null;
			drawn.clear();
			models = null;
		}
		return layer;
	}

	/** Shows the grid (ARGB pixels, row 0 is the north) on the area. */
	void showGrid(int w, int h, int[] argb, double south, double west, double north, double east, Runnable redo) {
		showGrid(w, h, argb, south, west, north, east, redo, null);
	}

	/** Shows the grid and the shapes of the model (drawLine, drawImage, ... in cells, converted by the geometry). */
	void showGrid(int w, int h, int[] argb, double south, double west, double north, double east, Runnable redo, Drawing drawing) {
		if (!begin(redo)) {
			return;
		}
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		img.setRGB(0, 0, w, h, argb, 0, w);
		Simulation.ui(() -> {
			try {
				WorldWindow wwd = WorldWindView.getWwd();
				if (wwd == null) {
					return;
				}
				RenderableLayer l = layer(wwd);
				Sector sector = Sector.fromDegrees(south, north, west, east);
				if (image == null) {
					image = new SurfaceImage(img, sector);
					l.addRenderable(image);
				} else {
					image.setImageSource(img, sector);
				}
				draw(l, drawing);
				wwd.redraw();
			} finally {
				end();
			}
		});
	}

	/** Shows the agents as points (lon, lat, the color, the size in pixels and a label or null). */
	void showPoints(double[] lon, double[] lat, String[] colors, double[] sizes, String[] labels, Runnable redo) {
		if (!begin(redo)) {
			return;
		}
		Simulation.ui(() -> {
			try {
				WorldWindow wwd = WorldWindView.getWwd();
				if (wwd == null) {
					return;
				}
				RenderableLayer l = layer(wwd);
				for (gov.nasa.worldwind.render.RigidShape r : shapes) {
					l.removeRenderable(r);
				}
				shapes.clear();
				shapeKind = null;
				int n = lon.length;
				while (points.size() < n) {
					PointPlacemark p = new PointPlacemark(Position.fromDegrees(0, 0, 0));
					p.setAltitudeMode(gov.nasa.worldwind.WorldWind.CLAMP_TO_GROUND);
					p.setValue(AVKey.DISPLAY_NAME, "Agent");
					points.add(p);
					l.addRenderable(p);
				}
				while (points.size() > n) {
					l.removeRenderable(points.remove(points.size() - 1));
				}
				for (int i = 0; i < n; i++) {
					PointPlacemark p = points.get(i);
					p.setPosition(Position.fromDegrees(lat[i], lon[i], 0));
					p.setAttributes(attributes(colors[i], sizes[i]));
					p.setLabelText(labels[i]);
				}
				wwd.redraw();
			} finally {
				end();
			}
		});
	}

	private final List<gov.nasa.worldwind.render.RigidShape> shapes = new ArrayList<>();
	private String shapeKind;
	private final Map<String, gov.nasa.worldwind.render.ShapeAttributes> shapeAttributes = new HashMap<>();

	/**
	 * Shows the agents as 3D shapes (OpenGL, lighted): "sphere", "cone" (pointing in
	 * the heading), "box" or "cylinder", the size (radius) in meters and the altitude
	 * in meters above the ground.
	 */
	void showShapes(String kind, double[] lon, double[] lat, double[] altitude, double[] heading, String[] colors, double[] meters, Runnable redo) {
		if (!begin(redo)) {
			return;
		}
		Simulation.ui(() -> {
			try {
				WorldWindow wwd = WorldWindView.getWwd();
				if (wwd == null) {
					return;
				}
				RenderableLayer l = layer(wwd);
				if (!kind.equals(shapeKind)) {
					for (gov.nasa.worldwind.render.RigidShape r : shapes) {
						l.removeRenderable(r);
					}
					shapes.clear();
					shapeKind = kind;
				}
				for (PointPlacemark p : points) {
					l.removeRenderable(p);
				}
				points.clear();
				int n = lon.length;
				while (shapes.size() < n) {
					Position p = Position.fromDegrees(0, 0, 0);
					gov.nasa.worldwind.render.RigidShape r = switch (kind) {
					case "cone" -> new gov.nasa.worldwind.render.Cone(p, 1, 1, 1);
					case "box" -> new gov.nasa.worldwind.render.Box(p, 1, 1, 1);
					case "cylinder" -> new gov.nasa.worldwind.render.Cylinder(p, 1, 1, 1);
					default -> new gov.nasa.worldwind.render.Ellipsoid(p, 1, 1, 1);
					};
					r.setAltitudeMode(gov.nasa.worldwind.WorldWind.RELATIVE_TO_GROUND);
					shapes.add(r);
					l.addRenderable(r);
				}
				while (shapes.size() > n) {
					l.removeRenderable(shapes.remove(shapes.size() - 1));
				}
				for (int i = 0; i < n; i++) {
					gov.nasa.worldwind.render.RigidShape r = shapes.get(i);
					double m = meters[i];
					r.setCenterPosition(Position.fromDegrees(lat[i], lon[i], altitude[i] + m));
					if (kind.equals("cone")) {
						/* A cone along the heading: the axis tilted to the horizon */
						r.setNorthSouthRadius(m * 0.45);
						r.setEastWestRadius(m * 0.45);
						r.setVerticalRadius(m * 1.2);
						r.setTilt(gov.nasa.worldwind.geom.Angle.fromDegrees(90));
						r.setHeading(gov.nasa.worldwind.geom.Angle.fromDegrees(heading[i]));
					} else if (kind.equals("sphere")) {
						/* A balloon-like ellipsoid (a little higher than wide) */
						r.setNorthSouthRadius(m);
						r.setEastWestRadius(m);
						r.setVerticalRadius(m * 1.2);
					} else {
						r.setNorthSouthRadius(m);
						r.setEastWestRadius(m);
						r.setVerticalRadius(m);
						r.setHeading(gov.nasa.worldwind.geom.Angle.fromDegrees(heading[i]));
					}
					r.setAttributes(shapeAttributes.computeIfAbsent(colors[i], c -> {
						gov.nasa.worldwind.render.BasicShapeAttributes a = new gov.nasa.worldwind.render.BasicShapeAttributes();
						a.setInteriorMaterial(new Material(new java.awt.Color(Simulation.rgb(c))));
						a.setEnableLighting(true);
						a.setDrawOutline(false);
						return a;
					}));
				}
				wwd.redraw();
			} finally {
				end();
			}
		});
	}

	/* ===================== Agents with images and 3D models, shapes ===================== */

	/** The shapes of a model and the conversion of its coordinates to longitude and latitude. */
	record Drawing(List<Shape> shapes, Map<String, String> imageFiles, java.util.function.BiFunction<Double, Double, double[]> lonLat, double metersPerUnit) {
	}

	/** The agents of a frame (arrays of the same length; images and models may be null per agent). */
	record Agents(double[] lon, double[] lat, String[] colors, double[] sizes, String[] labels, double[] altitude, double[] heading, double[] meters, String[] images, String[] models,
			String[] animations, String shape, Map<String, String> imageFiles, Map<String, String> modelFiles) {
	}

	private final List<gov.nasa.worldwind.render.Renderable> drawn = new ArrayList<>();
	private volatile Models models;
	private boolean animating;

	/** Shows the agents (points, images, 3D shapes or glTF models) and the shapes of the model. */
	void showAgents(Agents a, Drawing drawing, Runnable redo) {
		if (!begin(redo)) {
			return;
		}
		Simulation.ui(() -> {
			try {
				WorldWindow wwd = WorldWindView.getWwd();
				if (wwd == null) {
					return;
				}
				RenderableLayer l = layer(wwd);
				int n = a.lon.length;
				List<Integer> pts = new ArrayList<>(), rigid = new ArrayList<>(), mod = new ArrayList<>();
				for (int i = 0; i < n; i++) {
					if (a.models[i] != null && Assets.model(a.modelFiles.get(a.models[i])) != null) {
						mod.add(i);
					} else if (a.images[i] != null || a.shape == null || a.shape.equals("point")) {
						pts.add(i);
					} else {
						rigid.add(i);
					}
				}
				/* The points and the images */
				while (points.size() < pts.size()) {
					PointPlacemark p = new PointPlacemark(Position.fromDegrees(0, 0, 0));
					p.setAltitudeMode(gov.nasa.worldwind.WorldWind.CLAMP_TO_GROUND);
					p.setValue(AVKey.DISPLAY_NAME, "Agent");
					points.add(p);
					l.addRenderable(p);
				}
				while (points.size() > pts.size()) {
					l.removeRenderable(points.remove(points.size() - 1));
				}
				for (int k = 0; k < pts.size(); k++) {
					int i = pts.get(k);
					PointPlacemark p = points.get(k);
					p.setPosition(Position.fromDegrees(a.lat[i], a.lon[i], 0));
					p.setAttributes(a.images[i] != null ? imageAttributes(a.imageFiles.get(a.images[i]), a.sizes[i] * 4, a.heading[i]) : attributes(a.colors[i], a.sizes[i]));
					p.setLabelText(a.labels[i]);
				}
				/* The 3D shapes */
				String kind = a.shape == null ? "sphere" : a.shape;
				if (!kind.equals(shapeKind)) {
					for (gov.nasa.worldwind.render.RigidShape r : shapes) {
						l.removeRenderable(r);
					}
					shapes.clear();
					shapeKind = kind;
				}
				while (shapes.size() < rigid.size()) {
					Position p = Position.fromDegrees(0, 0, 0);
					gov.nasa.worldwind.render.RigidShape r = switch (kind) {
					case "cone" -> new gov.nasa.worldwind.render.Cone(p, 1, 1, 1);
					case "box" -> new gov.nasa.worldwind.render.Box(p, 1, 1, 1);
					case "cylinder" -> new gov.nasa.worldwind.render.Cylinder(p, 1, 1, 1);
					default -> new gov.nasa.worldwind.render.Ellipsoid(p, 1, 1, 1);
					};
					r.setAltitudeMode(gov.nasa.worldwind.WorldWind.RELATIVE_TO_GROUND);
					shapes.add(r);
					l.addRenderable(r);
				}
				while (shapes.size() > rigid.size()) {
					l.removeRenderable(shapes.remove(shapes.size() - 1));
				}
				for (int k = 0; k < rigid.size(); k++) {
					int i = rigid.get(k);
					gov.nasa.worldwind.render.RigidShape r = shapes.get(k);
					double m = a.meters[i];
					r.setCenterPosition(Position.fromDegrees(a.lat[i], a.lon[i], a.altitude[i] + m));
					r.setNorthSouthRadius(kind.equals("cone") ? m * 0.45 : m);
					r.setEastWestRadius(kind.equals("cone") ? m * 0.45 : m);
					r.setVerticalRadius(kind.equals("cone") ? m * 1.2 : kind.equals("sphere") ? m * 1.2 : m);
					if (kind.equals("cone")) {
						r.setTilt(gov.nasa.worldwind.geom.Angle.fromDegrees(90));
					}
					if (!kind.equals("sphere")) {
						r.setHeading(gov.nasa.worldwind.geom.Angle.fromDegrees(a.heading[i]));
					}
					r.setAttributes(shapeAttributes.computeIfAbsent(a.colors[i], c -> {
						gov.nasa.worldwind.render.BasicShapeAttributes at = new gov.nasa.worldwind.render.BasicShapeAttributes();
						at.setInteriorMaterial(new Material(new java.awt.Color(Simulation.rgb(c))));
						at.setEnableLighting(true);
						at.setDrawOutline(false);
						return at;
					}));
				}
				/* The glTF models */
				Models ms = new Models();
				for (int i : mod) {
					ms.add(Assets.model(a.modelFiles.get(a.models[i])), a.animations[i], a.lon[i], a.lat[i], a.altitude[i], a.heading[i], a.meters[i] * 2, i);
				}
				if (models != null) {
					l.removeRenderable(models);
				}
				models = ms.isEmpty() ? null : ms;
				if (models != null) {
					l.addRenderable(models);
				}
				animate(wwd);
				draw(l, drawing);
				wwd.redraw();
			} finally {
				end();
			}
		});
	}

	/* The globe is drawn again while animated models are shown (about 30 frames per second) */
	private void animate(WorldWindow wwd) {
		if (animating || models == null) {
			return;
		}
		animating = true;
		org.eclipse.swt.widgets.Display display = org.eclipse.ui.PlatformUI.getWorkbench().getDisplay();
		Runnable tick = new Runnable() {
			@Override
			public void run() {
				if (models == null || WorldWindView.getWwd() != wwd) {
					animating = false;
					return;
				}
				wwd.redraw();
				display.timerExec(33, this);
			}
		};
		display.timerExec(33, tick);
	}

	/* The glTF models of the agents on the globe (OpenGL in the frame of the surface: x east, y north, z up) */
	private static final class Models implements gov.nasa.worldwind.render.Renderable {
		private record Instance(Gltf model, String animation, double lon, double lat, double altitude, double heading, double meters, int phase) {
		}

		private final List<Instance> instances = new ArrayList<>();

		void add(Gltf model, String animation, double lon, double lat, double altitude, double heading, double meters, int phase) {
			instances.add(new Instance(model, animation, lon, lat, altitude, Double.isNaN(heading) ? 0 : heading, meters, phase));
		}

		boolean isEmpty() {
			return instances.isEmpty();
		}

		@Override
		public void render(gov.nasa.worldwind.render.DrawContext dc) {
			if (dc.isPickingMode() || instances.isEmpty()) {
				return;
			}
			com.jogamp.opengl.GL2 gl = dc.getGL().getGL2();
			gov.nasa.worldwind.globes.Globe globe = dc.getGlobe();
			gov.nasa.worldwind.geom.Matrix view = dc.getView().getModelviewMatrix();
			double seconds = System.nanoTime() / 1e9;
			Map<String, List<Gltf.Mesh>> frame = new HashMap<>();
			gl.glPushAttrib(com.jogamp.opengl.GL2.GL_ALL_ATTRIB_BITS);
			gl.glPushClientAttrib((int) com.jogamp.opengl.GL2.GL_CLIENT_ALL_ATTRIB_BITS);
			gl.glMatrixMode(com.jogamp.opengl.GL2.GL_MODELVIEW);
			gl.glPushMatrix();
			try {
				gl.glEnable(com.jogamp.opengl.GL.GL_DEPTH_TEST);
				gl.glDepthMask(true);
				gl.glDisable(com.jogamp.opengl.GL.GL_CULL_FACE);
				gl.glEnable(com.jogamp.opengl.GL2.GL_LIGHTING);
				gl.glEnable(com.jogamp.opengl.GL2.GL_LIGHT0);
				gl.glEnable(com.jogamp.opengl.GL2.GL_COLOR_MATERIAL);
				gl.glColorMaterial(com.jogamp.opengl.GL.GL_FRONT_AND_BACK, com.jogamp.opengl.GL2.GL_AMBIENT_AND_DIFFUSE);
				gl.glEnable(com.jogamp.opengl.GL2.GL_NORMALIZE);
				gl.glLightModeli(com.jogamp.opengl.GL2.GL_LIGHT_MODEL_TWO_SIDE, 1);
				/* A light from above (in the coordinates of the eye) */
				gl.glLoadIdentity();
				gl.glLightfv(com.jogamp.opengl.GL2.GL_LIGHT0, com.jogamp.opengl.GL2.GL_POSITION, new float[] { 0.3f, 0.8f, 0.6f, 0 }, 0);
				gl.glLightfv(com.jogamp.opengl.GL2.GL_LIGHT0, com.jogamp.opengl.GL2.GL_DIFFUSE, new float[] { 0.75f, 0.75f, 0.75f, 1 }, 0);
				gl.glLightfv(com.jogamp.opengl.GL2.GL_LIGHT0, com.jogamp.opengl.GL2.GL_AMBIENT, new float[] { 0.45f, 0.45f, 0.45f, 1 }, 0);
				for (Instance in : instances) {
					double ground = globe.getElevation(gov.nasa.worldwind.geom.Angle.fromDegrees(in.lat), gov.nasa.worldwind.geom.Angle.fromDegrees(in.lon)) * dc.getVerticalExaggeration();
					gov.nasa.worldwind.geom.Matrix m = globe.computeSurfaceOrientationAtPosition(gov.nasa.worldwind.geom.Angle.fromDegrees(in.lat), gov.nasa.worldwind.geom.Angle.fromDegrees(in.lon),
							ground + in.altitude);
					gov.nasa.worldwind.geom.Matrix mv = view.multiply(m).multiply(gov.nasa.worldwind.geom.Matrix.fromRotationZ(gov.nasa.worldwind.geom.Angle.fromDegrees(-in.heading)))
							.multiply(gov.nasa.worldwind.geom.Matrix.fromScale(in.meters));
					gl.glLoadMatrixd(mv.toArray(new double[16], 0, false), 0);
					ModelGl.meshes(gl, Assets.pose(in.model, in.animation, in.phase, seconds, frame));
				}
			} finally {
				gl.glPopMatrix();
				gl.glPopClientAttrib();
				gl.glPopAttrib();
			}
		}
	}

	/* The shapes of the model on the globe (paths, surface polygons and circles, texts, images) */
	private void draw(RenderableLayer l, Drawing d) {
		for (gov.nasa.worldwind.render.Renderable r : drawn) {
			l.removeRenderable(r);
		}
		drawn.clear();
		if (d == null) {
			return;
		}
		for (Shape s : d.shapes) {
			double[] p = s.p;
			java.awt.Color stroke = Assets.awt(s.stroke), fill = Assets.awt(s.fill);
			gov.nasa.worldwind.render.BasicShapeAttributes at = new gov.nasa.worldwind.render.BasicShapeAttributes();
			at.setDrawOutline(stroke != null);
			if (stroke != null) {
				at.setOutlineMaterial(new Material(stroke));
				at.setOutlineOpacity(stroke.getAlpha() / 255.0);
				at.setOutlineWidth(Math.max(1, s.width));
			}
			at.setDrawInterior(fill != null);
			if (fill != null) {
				at.setInteriorMaterial(new Material(fill));
				at.setInteriorOpacity(fill.getAlpha() / 255.0);
			}
			gov.nasa.worldwind.render.Renderable r = null;
			switch (s.kind) {
			case "line", "path" -> {
				List<Position> ps = new ArrayList<>();
				for (int i = 0; i + 1 < p.length; i += 2) {
					double[] ll = d.lonLat.apply(p[i], p[i + 1]);
					ps.add(Position.fromDegrees(ll[1], ll[0], 0));
				}
				gov.nasa.worldwind.render.Path path = new gov.nasa.worldwind.render.Path(ps);
				path.setAltitudeMode(gov.nasa.worldwind.WorldWind.CLAMP_TO_GROUND);
				path.setFollowTerrain(true);
				path.setAttributes(at);
				r = path;
			}
			case "line3" -> {
				double[] a = d.lonLat.apply(p[0], p[1]), b = d.lonLat.apply(p[3], p[4]);
				gov.nasa.worldwind.render.Path path = new gov.nasa.worldwind.render.Path(Position.fromDegrees(a[1], a[0], p[2]), Position.fromDegrees(b[1], b[0], p[5]));
				path.setAltitudeMode(gov.nasa.worldwind.WorldWind.RELATIVE_TO_GROUND);
				path.setAttributes(at);
				r = path;
			}
			case "polygon", "rect" -> {
				double[] q = AgentModel.outline(s);
				List<gov.nasa.worldwind.geom.LatLon> ps = new ArrayList<>();
				for (int i = 0; i + 1 < q.length; i += 2) {
					double[] ll = d.lonLat.apply(q[i], q[i + 1]);
					ps.add(gov.nasa.worldwind.geom.LatLon.fromDegrees(ll[1], ll[0]));
				}
				r = new gov.nasa.worldwind.render.SurfacePolygon(at, ps);
			}
			case "circle" -> {
				double[] ll = d.lonLat.apply(p[0], p[1]);
				r = new gov.nasa.worldwind.render.SurfaceCircle(at, gov.nasa.worldwind.geom.LatLon.fromDegrees(ll[1], ll[0]), Math.max(1, p[2] * d.metersPerUnit));
			}
			case "text", "image" -> {
				double[] ll = d.lonLat.apply(p[0], p[1]);
				PointPlacemark pm = new PointPlacemark(Position.fromDegrees(ll[1], ll[0], 0));
				pm.setAltitudeMode(gov.nasa.worldwind.WorldWind.CLAMP_TO_GROUND);
				if (s.kind.equals("image")) {
					pm.setAttributes(imageAttributes(d.imageFiles.get(s.text), s.width, s.angle));
				} else {
					PointPlacemarkAttributes pa = new PointPlacemarkAttributes();
					pa.setImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
					pa.setLabelMaterial(new Material(stroke != null ? stroke : java.awt.Color.WHITE));
					pa.setLabelScale(Math.max(0.5, s.width / 12));
					pm.setAttributes(pa);
					pm.setLabelText(s.text);
				}
				r = pm;
			}
			default -> {
			}
			}
			if (r != null) {
				drawn.add(r);
				l.addRenderable(r);
			}
		}
	}

	/* The attributes of an image (its larger side: size pixels; the heading clockwise from the north) */
	private PointPlacemarkAttributes imageAttributes(String file, double size, double heading) {
		BufferedImage img = Assets.image(file);
		long h = Double.isNaN(heading) ? -1 : Math.round(heading);
		return attributes.computeIfAbsent("image/" + file + "/" + size + "/" + h, k -> {
			PointPlacemarkAttributes a = new PointPlacemarkAttributes();
			if (img != null) {
				a.setImage(img);
				a.setScale(size / Math.max(img.getWidth(), img.getHeight()));
				a.setImageOffset(gov.nasa.worldwind.render.Offset.CENTER);
			}
			if (h >= 0) {
				a.setHeading((double) h);
				a.setHeadingReference(AVKey.RELATIVE_TO_GLOBE);
			}
			a.setLabelMaterial(Material.WHITE);
			a.setLabelScale(0.8);
			return a;
		});
	}

	private PointPlacemarkAttributes attributes(String color, double size) {
		return attributes.computeIfAbsent(color + "/" + size, k -> {
			PointPlacemarkAttributes a = new PointPlacemarkAttributes();
			a.setUsePointAsDefaultImage(true);
			int c = Simulation.rgb(color);
			a.setLineMaterial(new Material(new java.awt.Color(c)));
			a.setScale(size * 2);
			a.setLabelMaterial(Material.WHITE);
			a.setLabelScale(0.8);
			return a;
		});
	}

	private boolean begin(Runnable redo) {
		if (!Simulation.hasDisplay()) {
			return false;
		}
		if (!pending.compareAndSet(false, true)) {
			again = redo;
			return false;
		}
		again = null;
		return true;
	}

	private void end() {
		pending.set(false);
		Runnable r = again;
		again = null;
		if (r != null) {
			/* Not in the thread of the user interface (it stays free, e.g., for the speed slider) */
			Simulation.redisplay(r);
		}
	}

	/** The ARGB value of a color #rrggbb or #rrggbbaa. */
	static int argb(String color) {
		int rgb = Simulation.rgb(color);
		int alpha = 255;
		if (color != null && color.length() == 9) {
			try {
				alpha = Integer.parseInt(color.substring(7, 9), 16);
			} catch (NumberFormatException e) {
				alpha = 255;
			}
		}
		return (alpha << 24) | rgb;
	}

	/** The ARGB color of a value on a ramp (with the alpha of the colors, NaN: transparent). */
	static int rampArgb(String[] colors, double value, double lo, double span) {
		if (value != value) {
			return 0;
		}
		int n = colors.length;
		double t = (value - lo) / span;
		if (!(t >= 0)) {
			t = 0;
		}
		if (t > 1) {
			t = 1;
		}
		double p = t * (n - 1);
		int a = Math.min(n - 2, (int) Math.floor(p));
		double w = p - a;
		int c0 = argb(colors[a]), c1 = argb(colors[a + 1]);
		int result = 0;
		for (int shift = 0; shift <= 24; shift += 8) {
			int x = (c0 >>> shift) & 255, y = (c1 >>> shift) & 255;
			result |= ((int) (x + w * (y - x)) & 255) << shift;
		}
		return result;
	}
}
