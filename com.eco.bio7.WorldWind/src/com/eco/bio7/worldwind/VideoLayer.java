/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.worldwind;

import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL2.GL_CURRENT_BIT;
import static com.jogamp.opengl.GL2.GL_ENABLE_BIT;
import static com.jogamp.opengl.GL2.GL_LIGHTING_BIT;
import static com.jogamp.opengl.GL2.GL_TEXTURE_BIT;
import static com.jogamp.opengl.GL2.GL_TRANSFORM_BIT;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_AMBIENT;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_COLOR_MATERIAL;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_DIFFUSE;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_LIGHT0;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_LIGHT1;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_LIGHTING;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_NORMALIZE;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_POSITION;
import static com.jogamp.opengl.fixedfunc.GLLightingFunc.GL_SPECULAR;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;




/**
 */
public class VideoLayer implements Renderable {

	private boolean propertyChanged = false; // Indicates if some parameter of
	// the object has changed.
	
	
	private static long period = 1000;
	public PickSupport pickSupport = new PickSupport();

	public static void setPeriod(long period) {
		VideoLayer.period = period;
	}

	private boolean hold;
	
	private static VideoLayer dynamicLayer;
	private static boolean step;
	

	

	public VideoLayer() {
		dynamicLayer = this;
	}

	
	

	/**
	 * Indicates if some property of the geometry has changed.
	 */
	public boolean isPropertyChanged() {
		return propertyChanged;
	}

	/**
	 * Sets if some property of the object has changed.
	 */
	public void setPropertyChanged(boolean propertyChanged) {
		this.propertyChanged = propertyChanged;
	}

	/**
	 * Renders the object.
	 * 
	 * @param dc
	 *            draw context reference.
	 */
	@Override
	public void render(DrawContext dc) {
		GL gl = dc.getGL();
		GLU glu = dc.getGLU();

		
		}

		



	

	

	public void begin(DrawContext dc) {
		GL2 gl = dc.getGL().getGL2();
		Vec4 cameraPosition = dc.getView().getEyePoint();

		if (dc.isPickingMode()) {
			this.pickSupport.beginPicking(dc);

			gl.glPushAttrib(GL_ENABLE_BIT | GL_CURRENT_BIT | GL_TRANSFORM_BIT);
			gl.glDisable(GL_TEXTURE_2D);
			gl.glDisable(GL_COLOR_MATERIAL);
		} else {
			gl.glPushAttrib(GL_TEXTURE_BIT | GL_ENABLE_BIT | GL_CURRENT_BIT | GL_LIGHTING_BIT | GL_TRANSFORM_BIT);
			gl.glDisable(GL.GL_TEXTURE_2D);

			float[] lightPosition = { (float) (cameraPosition.x * 2), (float) (cameraPosition.y / 2), (float) (cameraPosition.z), 0.0f };
			float[] lightDiffuse = { 1.0f, 1.0f, 1.0f, 1.0f };
			float[] lightAmbient = { 1.0f, 1.0f, 1.0f, 1.0f };
			float[] lightSpecular = { 1.0f, 1.0f, 1.0f, 1.0f };

			gl.glDisable(GL_COLOR_MATERIAL);

			gl.glLightfv(GL_LIGHT1, GL_POSITION, lightPosition, 0);
			gl.glLightfv(GL_LIGHT1, GL_DIFFUSE, lightDiffuse, 0);
			gl.glLightfv(GL_LIGHT1, GL_AMBIENT, lightAmbient, 0);
			gl.glLightfv(GL_LIGHT1, GL_SPECULAR, lightSpecular, 0);

			gl.glDisable(GL_LIGHT0);
			gl.glEnable(GL_LIGHT1);
			gl.glEnable(GL_LIGHTING);
			gl.glEnable(GL_NORMALIZE);
		}

		gl.glMatrixMode(GL_MODELVIEW);
		gl.glPushMatrix();
	}

	public void end(DrawContext dc) {
		GL2 gl = dc.getGL().getGL2();

		gl.glMatrixMode(GL_MODELVIEW);
		gl.glPopMatrix();

		if (dc.isPickingMode()) {
			this.pickSupport.endPicking(dc);
		} else {
			gl.glDisable(GL_LIGHT1);
			gl.glEnable(GL_LIGHT0);
			gl.glDisable(GL_LIGHTING);
			gl.glDisable(GL_NORMALIZE);
		}

		gl.glPopAttrib();
	}

	public void compute(DrawContext dc, GL gl) {

		// To compute rotation of the arrow axis toward the proper heading, find
		// a second point in that direction.
		/*
		 * Position pos = dc.getGlobe().computePointFromPosition(new
		 * Position(Angle.fromDegrees(51.0), Angle.fromDegrees(9.0), 1200));
		 * LatLon p2ll = LatLon.greatCircleEndPosition(pos, marker.getHeading(),
		 * Angle.fromDegrees(.1)); Vec4 p2 =
		 * dc.getGlobe().computePointFromPosition(p2ll.getLatitude(),
		 * p2ll.getLongitude(), pos.getElevation());
		 * 
		 * // This method then performs the same operation as Vec4.axisAngle()
		 * but with a "v2" of <0, 0, 1>. Vec4 p1p2 =
		 * p2.subtract3(point).normalize3(); // Compute rotation angle Angle
		 * directionAngle = Angle.fromRadians(Math.acos(p1p2.z)); // Compute the
		 * direction cosine factors that define the rotation axis double A =
		 * -p1p2.y; double B = p1p2.x; double L = Math.sqrt(A A + B B);
		 * 
		 * // Compute rotation angle on z (roll) to keep the arrow plane
		 * parallel to the ground Vec4 horizontalVector =
		 * dc.getGlobe().computeSurfaceNormalAtPoint(point).cross3(p1p2); Vec4
		 * rotatedX =
		 * Vec4.UNIT_X.transformBy3(Matrix.fromAxisAngle(directionAngle, A / L,
		 * B / L, 0)); Angle rollAngle =
		 * rotatedX.angleBetween3(horizontalVector); // Find out which way to do
		 * the roll double rollDirection =
		 * Math.signum(-horizontalVector.cross3(rotatedX).dot3(p1p2));
		 * 
		 * gl.glRotated(directionAngle.degrees, A / L, B / L, 0); // point arrow
		 * toward p2 gl.glRotated(rollAngle.degrees, 0, 0, rollDirection); //
		 * roll arrow to keep it parallel to the ground double scale =
		 * attrs.getHeadingScale(); gl.glScaled(scale, scale, scale); // scale
		 * on top of parent sphere radius
		 * dc.getGL().glCallList(this.glArrowListId); // draw
		 */}

	/**
	 * Converts position in spherical coordinates (lat/lon/altitude) to
	 * cartesian (XYZ) coordinates.
	 * 
	 * @param latitude
	 *            Latitude in decimal degrees
	 * @param longitude
	 *            Longitude in decimal degrees
	 * @param radius
	 *            Radius
	 * @return the corresponding Point
	 */
	public static Vec4 SphericalToCartesian(double latitude, double longitude, double radius) {
		latitude *= Math.PI / 180.0f;
		longitude *= Math.PI / 180.0f;

		double radCosLat = radius * Math.cos(latitude);

		return new Vec4(radCosLat * Math.sin(longitude), radius * Math.sin(latitude), radCosLat * Math.cos(longitude));
	}

	/**
	 * Converts position in cartesian coordinates (XYZ) to spherical (radius,
	 * lat, lon) coordinates.
	 * 
	 * @param x
	 *            X coordinate
	 * @param y
	 *            Y coordinate
	 * @param z
	 *            Z coordinate
	 * @return a <code>Vec4</code> point for the spherical coordinates {radius,
	 *         lat, lon}
	 */
	public static Vec4 CartesianToSpherical(double x, double y, double z) {
		double rho = Math.sqrt(x * x + y * y + z * z);
		double longitude = Math.atan2(x, z);
		double latitude = Math.asin(y / rho);

		return new Vec4(rho, latitude, longitude);
	}

	

}