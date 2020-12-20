/*******************************************************************************
 * Copyright (c) 2004, 2005 Elias Volanakis and others.
  * All rights reserved. This program and the accompanying materials
  * are made available under the terms of the Eclipse Public License v1.0
  * which accompanies this distribution, and is available at
  * http://www.eclipse.org/legal/epl-v10.html
  *
  * Contributors:
  *    Elias Volanakis - initial API and implementation
 *    M.Austenfeld - Changes for the Bio7 application.
  *******************************************************************************/
package com.eco.bio7.floweditor.model;

import com.eco.bio7.floweditor.shapes.ShapesPlugin;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * 
 * @see com.eco.bio7.floweditor.model.RectangularShape
 * @see com.eco.bio7.floweditor.model.EllipticalShape
 * @author Elias Volanakis.
 * 
 * M.Austenfeld - Changes for the Bio7 application.
 */
public class Shape extends ModelElement implements IPropertySource {

	/**
	 * A static array of property descriptors. There is one IPropertyDescriptor
	 * entry per editable property.
	 * 
	 * @see #getPropertyDescriptors()
	 * @see #getPropertyValue(Object)
	 * @see #setPropertyValue(Object, Object)
	 */
	public static final String P_CONSTRAINT = "_constraint";

	public static IPropertyDescriptor[] descriptors;

	/**
	 * ID for the Height property value (used for by the corresponding property
	 * descriptor).
	 */
	private static final String HEIGHT_PROP = "Shape.Height";

	/** Property ID to use when the location of this shape is modified. */
	public static final String LOCATION_PROP = "Shape.Location";

	private static final long serialVersionUID = 1;

	/** Property ID to use then the size of this shape is modified. */
	public static final String SIZE_PROP = "Shape.Size";

	/** Property ID to use when the list of outgoing connections is modified. */
	public static final String SOURCE_CONNECTIONS_PROP = "Shape.SourceConn";

	/** Property ID to use when the list of incoming connections is modified. */
	public static final String TARGET_CONNECTIONS_PROP = "Shape.TargetConn";

	/**
	 * ID for the Width property value (used for by the corresponding property
	 * descriptor).
	 */
	private static final String WIDTH_PROP = "Shape.Width";

	/**
	 * ID for the X property value (used for by the corresponding property
	 * descriptor).
	 */
	private static final String XPOS_PROP = "Shape.xPos";

	/**
	 * ID for the Y property value (used for by the corresponding property
	 * descriptor).
	 */
	private static final String YPOS_PROP = "Shape.yPos";

	public static String FILE_PROP = "File"; // The name of the File for the
												// batch

	public static String FILE_TYPE = "-";

	public static String LOOP_COUNT = "0";

	public static String INIT_LOOP = " ";

	private Rectangle constraint;
	
	 
	/*
	 * Initializes the property descriptors array.
	 * 
	 * @see #getPropertyDescriptors()
	 * @see #getPropertyValue(Object)
	 * @see #setPropertyValue(Object, Object)
	 */
	static {
		descriptors = new IPropertyDescriptor[] {
				new TextPropertyDescriptor(INIT_LOOP, "Initloop"),
				new TextPropertyDescriptor(XPOS_PROP, "X"), // id and
				// description pair
				new TextPropertyDescriptor(YPOS_PROP, "Y"), new TextPropertyDescriptor(WIDTH_PROP, "Width"), new TextPropertyDescriptor(HEIGHT_PROP, "Height"),
				new TextPropertyDescriptor(FILE_PROP, "File/Loop/Info/Decision"), new TextPropertyDescriptor(FILE_TYPE, "Filetype"),
				new TextPropertyDescriptor(LOOP_COUNT, "Loop count"), };
		// use a custom cell editor validator for all four array entries

		/*
		 * for (int i = 0; i < descriptors.length; i++) { ((PropertyDescriptor)
		 * descriptors[i]).setValidator(new ICellEditorValidator() { public
		 * String isValid(Object value) { int intValue = -1;
		 * 
		 * 
		 * try { intValue = Integer.parseInt((String) value); } catch
		 * (NumberFormatException exc) { return "Not a number"; } return
		 * (intValue >= 0) ? null : "Value must be >= 0"; } }); }
		 */
	}

	protected static Image createImage(String name) {
		InputStream stream = ShapesPlugin.class.getResourceAsStream(name);
		Image image = new Image(null, stream);
		try {
			stream.close();
		} catch (IOException ioe) {
		}
		return image;
	}

	/** Location of this shape. */
	private Point location = new Point(0, 0);

	/** Size of this shape. */
	private Dimension size = new Dimension(50, 50);

	/** List of outgoing Connections. */
	private List sourceConnections = new ArrayList();

	/** List of incoming Connections. */
	private List targetConnections = new ArrayList();

	private String file = " ";

	private String filetype = " ";

	private String initloop = " ";

	private String loopcount = "0";

	/**
	 * Add an incoming or outgoing connection to this shape.
	 * 
	 * @param conn
	 *            a non-null connection instance
	 * @throws IllegalArgumentException
	 *             if the connection is null or has not distinct endpoints
	 */
	void addConnection(Connection conn) {
		if (conn == null || conn.getSource() == conn.getTarget()) {
			throw new IllegalArgumentException();

		}
		if (conn.getSource() == this) {
			sourceConnections.add(conn);
			firePropertyChange(SOURCE_CONNECTIONS_PROP, null, conn);
		} else if (conn.getTarget() == this) {
			targetConnections.add(conn);
			firePropertyChange(TARGET_CONNECTIONS_PROP, null, conn);
		}
	}

	/**
	 * Return a pictogram (small icon) describing this model element. Children
	 * should override this method and return an appropriate Image.
	 * 
	 * @return a 16x16 Image or null
	 */
	public Image getIcon() {
		return null;
	}

	/**
	 * Return the Location of this shape.
	 * 
	 * @return a non-null location instance
	 */
	public Point getLocation() {
		return location.getCopy();
	}

	/**
	 * Returns an array of IPropertyDescriptors for this shape.
	 * <p>
	 * The returned array is used to fill the property view, when the edit-part
	 * corresponding to this model element is selected.
	 * </p>
	 * 
	 * @see #descriptors
	 * @see #getPropertyValue(Object)
	 * @see #setPropertyValue(Object, Object)
	 */
	public IPropertyDescriptor[] getPropertyDescriptors() {
		IPropertyDescriptor[] descriptors_visible = new IPropertyDescriptor[] { descriptors[5], descriptors[6], descriptors[1], descriptors[2], descriptors[3], descriptors[4] };

		return descriptors_visible;
	}

	/**
	 * Return the property value for the given propertyId, or null.
	 * <p>
	 * The property view uses the IDs from the IPropertyDescriptors array to
	 * obtain the value of the corresponding properties.
	 * </p>
	 * 
	 * @see #descriptors
	 * @see #getPropertyDescriptors()
	 */
	public Object getPropertyValue(Object propertyId) {
		if (XPOS_PROP.equals(propertyId)) {
			return Integer.toString(location.x);
		}
		if (YPOS_PROP.equals(propertyId)) {
			return Integer.toString(location.y);
		}
		if (HEIGHT_PROP.equals(propertyId)) {
			return Integer.toString(size.height);
		}
		if (WIDTH_PROP.equals(propertyId)) {
			return Integer.toString(size.width);
		}
		if (FILE_PROP.equals(propertyId)) {

			return file;
		}
		if (FILE_TYPE.equals(propertyId)) {

			return filetype;
		}
		if (INIT_LOOP.equals(propertyId)) {

			return initloop;
		}
		if (LOOP_COUNT.equals(propertyId)) {

			return loopcount;
		}

		return super.getPropertyValue(propertyId);
	}

	/**
	 * Return the Size of this shape.
	 * 
	 * @return a non-null Dimension instance
	 */
	public Dimension getSize() {
		return size.getCopy();
	}

	public String getText() {
		return file;

	}

	public String getTextFiletype() {
		return filetype;

	}

	public String getTextInitloop() {
		return initloop;

	}

	public String getTextLoopcount() {
		return loopcount;

	}

	public void setText(String text) {
		this.file = text;

		firePropertyChange(FILE_PROP, null, file); // ?
	}

	public void setTextFiletype(String text) {
		this.filetype = text;

		firePropertyChange(FILE_TYPE, null, filetype); // ?
	}

	public void setTextInitloop(String text) {
		this.initloop = text;

		firePropertyChange(INIT_LOOP, null, initloop); // ?

	}

	public void setTextLoopcount(String text) {
		this.loopcount = text;

		firePropertyChange(LOOP_COUNT, null, loopcount); // ?

	}

	/**
	 * Return a List of outgoing Connections.
	 */
	public List getSourceConnections() {
		return new ArrayList(sourceConnections);
	}

	/**
	 * Return a List of incoming Connections.
	 */
	public List getTargetConnections() {
		return new ArrayList(targetConnections);
	}

	/**
	 * Remove an incoming or outgoing connection from this shape.
	 * 
	 * @param conn
	 *            a non-null connection instance
	 * @throws IllegalArgumentException
	 *             if the parameter is null
	 */
	void removeConnection(Connection conn) {
		if (conn == null) {
			throw new IllegalArgumentException();
		}
		if (conn.getSource() == this) {
			sourceConnections.remove(conn);
			firePropertyChange(SOURCE_CONNECTIONS_PROP, null, conn);
		} else if (conn.getTarget() == this) {
			targetConnections.remove(conn);
			firePropertyChange(TARGET_CONNECTIONS_PROP, null, conn);
		}
	}

	public void setColor() {
		firePropertyChange("color", null, null);

	}

	public void setDefaultColor() {
		firePropertyChange("defaultcolor", null, null);
	}

	/**
	 * Set the Location of this shape.
	 * 
	 * @param newLocation
	 *            a non-null Point instance
	 * @throws IllegalArgumentException
	 *             if the parameter is null
	 */
	public void setLocation(Point newLocation) {
		if (newLocation == null) {
			throw new IllegalArgumentException();
		}
		location.setLocation(newLocation);
		firePropertyChange(LOCATION_PROP, null, location);
	}

	public boolean isPropertySet(Object id) {
		if (id.equals(FILE_PROP))
			return true;

		else
			return false;
	}

	public void resetPropertyValue(Object id) {
	}

	public Rectangle getConstraint() {
		return constraint;
	}

	public void setConstraint(Rectangle rect) {
		constraint = rect;

		firePropertyChange(P_CONSTRAINT, null, constraint);
	}

	/**
	 * Set the property value for the given property id. If no matching id is
	 * found, the call is forwarded to the superclass.
	 * <p>
	 * The property view uses the IDs from the IPropertyDescriptors array to set
	 * the values of the corresponding properties.
	 * </p>
	 * 
	 * @see #descriptors
	 * @see #getPropertyDescriptors()
	 */
	public void setPropertyValue(Object propertyId, Object value) {

		if (XPOS_PROP.equals(propertyId)) {
			int x = Integer.parseInt((String) value);
			setLocation(new Point(x, location.y));
		} else if (YPOS_PROP.equals(propertyId)) {
			int y = Integer.parseInt((String) value);
			setLocation(new Point(location.x, y));
		} else if (HEIGHT_PROP.equals(propertyId)) {
			int height = Integer.parseInt((String) value);
			setSize(new Dimension(size.width, height));
		} else if (WIDTH_PROP.equals(propertyId)) {
			int width = Integer.parseInt((String) value);
			setSize(new Dimension(width, size.height));
		} else if (FILE_PROP.equals(propertyId)) {

			file = (String) value;
			setText((String) value);
			setTextInitloop((String) value);
			setTextLoopcount((String) value);

		} else if (FILE_TYPE.equals(propertyId)) {

			filetype = (String) value;
			setTextFiletype((String) value);
		} else if (INIT_LOOP.equals(propertyId)) {

			initloop = (String) value;
			setTextInitloop((String) value);
		} else if (LOOP_COUNT.equals(propertyId)) {

			loopcount = (String) value;
			setTextLoopcount((String) value);
		}

		else {
			super.setPropertyValue(propertyId, value);
		}
	}

	/**
	 * Set the Size of this shape. Will not modify the size if newSize is null.
	 * 
	 * @param newSize
	 *            a non-null Dimension instance or null
	 */
	public void setSize(Dimension newSize) {
		if (newSize != null) {
			size.setSize(newSize);
			firePropertyChange(SIZE_PROP, null, size);
		}
	}

	public static String getFILE_PROP() {
		return FILE_PROP;
	}

	public static String getFILE_TYPE() {
		return FILE_TYPE;
	}

	public static String getINIT_LOOP() {
		return INIT_LOOP;
	}

	public static String getLOOP_COUNT() {
		return LOOP_COUNT;
	}

}