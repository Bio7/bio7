/*******************************************************************************
 * Copyright (c) 2004, 2005 Elias Volanakis and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Elias Volanakis - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.floweditor.model;

import org.eclipse.ui.views.properties.ComboBoxPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertyDescriptor;

import org.eclipse.draw2d.Graphics;

/**
 * A connection between two distinct shapes.
 * 
 * @author Elias Volanakis
 */
public class Connection extends ModelElement {
	/**
	 * Used for indicating that a Connection with solid line style should be
	 * created.
	 * 
	 * @see com.eco.bio7.floweditor.parts.ShapeEditPart#createEditPolicies()
	 */
	public static final Integer SOLID_CONNECTION = new Integer(
			Graphics.LINE_SOLID);
	/**
	 * Used for indicating that a Connection with dashed line style should be
	 * created.
	 * 
	 * @see com.eco.bio7.floweditor.parts.ShapeEditPart#createEditPolicies()
	 */
	public static final Integer DASHED_CONNECTION = new Integer(
			Graphics.LINE_DASH);
	/** Property ID to use when the line style of this connection is modified. */
	public static final String LINESTYLE_PROP = "LineStyle";
	public static final String BOOLEAN_PROP = "Boolean";
	private static final IPropertyDescriptor[] descriptors = new IPropertyDescriptor[2];
	private static final String SOLID_STR = "Solid";
	private static final String DASHED_STR = "Dashed";
	private static final String TRUE_STR = "true";
	private static final String FALSE_STR = "false";

	private static final long serialVersionUID = 1;

	/** True, if the connection is attached to its endpoints. */
	private boolean isConnected;
	/** Line drawing style for this connection. */
	private int lineStyle = Graphics.LINE_SOLID;
	private String booleanvalue = "true";
	/** Connection's source endpoint. */
	private Shape source;
	/** Connection's target endpoint. */
	private Shape target;

	static {
		descriptors[0] = new ComboBoxPropertyDescriptor(LINESTYLE_PROP,
				LINESTYLE_PROP, new String[] { SOLID_STR, DASHED_STR });
		descriptors[1] = new ComboBoxPropertyDescriptor(BOOLEAN_PROP,
				BOOLEAN_PROP, new String[] { FALSE_STR, TRUE_STR });
	}

	/**
	 * Create a (solid) connection between two distinct shapes.
	 * 
	 * @param source
	 *            a source endpoint for this connection (non null)
	 * @param target
	 *            a target endpoint for this connection (non null)
	 * @throws IllegalArgumentException
	 *             if any of the parameters are null or source == target
	 * @see #setLineStyle(int)
	 */
	public Connection(Shape source, Shape target) {
		reconnect(source, target);
	}

	/**
	 * Disconnect this connection from the shapes it is attached to.
	 */
	public void disconnect() {
		if (isConnected) {
			source.removeConnection(this);
			target.removeConnection(this);
			isConnected = false;
		}
	}

	/**
	 * Returns the line drawing style of this connection.
	 * 
	 * @return an int value (Graphics.LINE_DASH or Graphics.LINE_SOLID)
	 */
	public int getLineStyle() {
		return lineStyle;
	}

	public String getBoolean() {
		return booleanvalue;
	}

	/**
	 * Returns the descriptor for the lineStyle property
	 * 
	 * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyDescriptors()
	 */
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return descriptors;
	}

	/**
	 * Returns the lineStyle as String for the Property Sheet
	 * 
	 * @see org.eclipse.ui.views.properties.IPropertySource#getPropertyValue(java.lang.Object)
	 */
	public Object getPropertyValue(Object id) {
		if (id.equals(LINESTYLE_PROP)) {
			if (getLineStyle() == Graphics.LINE_DASH)
				// Dashed is the second value in the combo dropdown
				return new Integer(1);
			// Solid is the first value in the combo dropdown
			return new Integer(0);
		}

		else if (id.equals(BOOLEAN_PROP)) {
			if (getBoolean().equals("true")) {
				return new Integer(1);
			}

			return new Integer(0);

		}
		return super.getPropertyValue(id);
	}

	/**
	 * Returns the source endpoint of this connection.
	 * 
	 * @return a non-null Shape instance
	 */
	public Shape getSource() {
		return source;
	}

	/**
	 * Returns the target endpoint of this connection.
	 * 
	 * @return a non-null Shape instance
	 */
	public Shape getTarget() {
		return target;
	}

	/**
	 * Reconnect this connection. The connection will reconnect with the shapes
	 * it was previously attached to.
	 */
	public void reconnect() {
		if (!isConnected) {
			source.addConnection(this);
			target.addConnection(this);
			isConnected = true;
		}
	}

	/**
	 * Reconnect to a different source and/or target shape. The connection will
	 * disconnect from its current attachments and reconnect to the new source
	 * and target.
	 * 
	 * @param newSource
	 *            a new source endpoint for this connection (non null)
	 * @param newTarget
	 *            a new target endpoint for this connection (non null)
	 * @throws IllegalArgumentException
	 *             if any of the paramers are null or newSource == newTarget
	 */
	public void reconnect(Shape newSource, Shape newTarget) {
		if (newSource == null || newTarget == null || newSource == newTarget) {
			throw new IllegalArgumentException();
		}
		disconnect();
		this.source = newSource;
		this.target = newTarget;
		reconnect();
	}

	/**
	 * Set the line drawing style of this connection.
	 * 
	 * @param lineStyle
	 *            one of following values: Graphics.LINE_DASH or
	 *            Graphics.LINE_SOLID
	 * @see Graphics#LINE_DASH
	 * @see Graphics#LINE_SOLID
	 * @throws IllegalArgumentException
	 *             if lineStyle does not have one of the above values
	 */
	public void setLineStyle(int lineStyle) {
		if (lineStyle != Graphics.LINE_DASH && lineStyle != Graphics.LINE_SOLID) {
			throw new IllegalArgumentException();
		}
		this.lineStyle = lineStyle;
		firePropertyChange(LINESTYLE_PROP, null, new Integer(this.lineStyle));
	}

	public void setBoolean(String bool) {
		this.booleanvalue = bool;
		firePropertyChange(BOOLEAN_PROP, null, new String(this.booleanvalue));
	}

	/**
	 * Sets the lineStyle based on the String provided by the PropertySheet
	 * 
	 * @see org.eclipse.ui.views.properties.IPropertySource#setPropertyValue(java.lang.Object,
	 *      java.lang.Object)
	 */
	public void setPropertyValue(Object id, Object value) {
		if (id.equals(LINESTYLE_PROP))
			setLineStyle(new Integer(1).equals(value) ? Graphics.LINE_DASH
					: Graphics.LINE_SOLID);

		else if (id.equals(BOOLEAN_PROP)) {
			setBoolean(new Integer(1).equals(value) ? "true" : "false");

		}

		else
			super.setPropertyValue(id, value);
	}

}