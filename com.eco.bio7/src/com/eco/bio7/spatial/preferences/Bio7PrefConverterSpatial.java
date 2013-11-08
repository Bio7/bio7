package com.eco.bio7.spatial.preferences;

/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.DataFormatException;

/**
 * A utility class for dealing with preferences whose values are common SWT
 * objects (color, points, rectangles, and font data). The static methods on
 * this class handle the conversion between the SWT objects and their string
 * representations.
 * <p>
 * Usage:
 * 
 * <pre>
 * IPreferenceStore store = ...;
 * PreferenceConverter.setValue(store, &quot;bg&quot;, new RGB(127,127,127));
 * ...
 * RBG bgColor = PreferenceConverter.getValue(store, &quot;bg&quot;);
 * </pre>
 * 
 * </p>
 * <p>
 * This class contains static methods and fields only and cannot be
 * instantiated.
 * </p>
 * Note: touching this class has the side effect of creating a display (static
 * initializer).
 */
public class Bio7PrefConverterSpatial {

	/**
	 * The default-default value for point preferences (the origin,
	 * <code>(0,0)</code>).
	 */
	public static final Point3d POINT_DEFAULT_DEFAULT = new Point3d(0, 0, 0);

	private static final String ENTRY_SEPARATOR = ";"; //$NON-NLS-1$

	/*
	 * (non-Javadoc) private constructor to prevent instantiation.
	 */
	private Bio7PrefConverterSpatial() {
		// no-op
	}

	/**
	 * Helper method to construct a point from the given string.
	 * 
	 * @param value
	 * @return Point
	 */
	private static Point3d basicGetPoint(String value) {
		Point3d dp = new Point3d(POINT_DEFAULT_DEFAULT.x,
				POINT_DEFAULT_DEFAULT.y, POINT_DEFAULT_DEFAULT.z);
		if (IPreferenceStore.STRING_DEFAULT_DEFAULT.equals(value)) {
			return dp;
		}
		return asPoint(value, dp);
	}

	public static Point3d asPoint(String value) throws DataFormatException {
		if (value == null) {
			throw new DataFormatException(
					"Null doesn't represent a valid point"); //$NON-NLS-1$
		}
		StringTokenizer stok = new StringTokenizer(value, ","); //$NON-NLS-1$
		String x = stok.nextToken();
		String y = stok.nextToken();
		String z = stok.nextToken();
		int xval = 0, yval = 0, zval = 0;
		;
		try {
			xval = Integer.parseInt(x);
			yval = Integer.parseInt(y);
			zval = Integer.parseInt(z);
		} catch (NumberFormatException e) {
			throw new DataFormatException(e.getMessage());
		}
		return new Point3d(xval, yval, zval);
	}

	public static Point3d asPoint(String value, Point3d dflt) {
		try {
			return asPoint(value);
		} catch (DataFormatException e) {
			return dflt;
		}
	}

	/**
	 * Returns the default value for the point-valued preference with the given
	 * name in the given preference store. Returns the default-default value (<code>POINT_DEFAULT_DEFAULT</code>)
	 * is no default preference with the given name, or if the default value
	 * cannot be treated as a point.
	 * 
	 * @param store
	 *            the preference store
	 * @param name
	 *            the name of the preference
	 * @return the default value of the preference
	 */
	public static Point3d getDefaultPoint(IPreferenceStore store, String name) {
		return basicGetPoint(store.getDefaultString(name));
	}

	/**
	 * Returns the current value of the point-valued preference with the given
	 * name in the given preference store. Returns the default-default value (<code>POINT_DEFAULT_DEFAULT</code>)
	 * if there is no preference with the given name, or if the current value
	 * cannot be treated as a point.
	 * 
	 * @param store
	 *            the preference store
	 * @param name
	 *            the name of the preference
	 * @return the point-valued preference
	 */
	public static Point3d getPoint(IPreferenceStore store, String name) {
		return basicGetPoint(store.getString(name));
	}

	/**
	 * Sets the default value of the preference with the given name in the given
	 * preference store.
	 * 
	 * @param store
	 *            the preference store
	 * @param name
	 *            the name of the preference
	 * @param value
	 *            the new default value of the preference
	 */
	public static void setDefault(IPreferenceStore store, String name,
			Point3d value) {
		store.setDefault(name, asString(value));
	}

	public static String asString(Point3d value) {
		Assert.isNotNull(value);
		StringBuffer buffer = new StringBuffer();
		buffer.append(value.x);
		buffer.append(',');
		buffer.append(value.y);
		buffer.append(',');
		buffer.append(value.z);
		return buffer.toString();
	}

	/**
	 * Sets the current value of the preference with the given name in the given
	 * preference store.
	 * 
	 * @param store
	 *            the preference store
	 * @param name
	 *            the name of the preference
	 * @param value
	 *            the new current value of the preference
	 */
	public static void setValue(IPreferenceStore store, String name,
			Point3d value) {
		
		Point3d oldValue = getPoint(store, name);
		if (oldValue == null || !oldValue.equals(value)) {
			store.putValue(name, asString(value));
			store.firePropertyChangeEvent(name, oldValue, value);
		}
	}

}
