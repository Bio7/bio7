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

import com.eco.bio7.floweditor.ruler.DiagramRuler;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.draw2d.PositionConstants;

/**
 * A container for multiple shapes. This is the "root" of the model data
 * structure.
 * 
 * @author Elias Volanakis
 */
public class ShapesDiagram extends ModelElement {

	/** Property ID to use when a child is added to this diagram. */
	public static final String CHILD_ADDED_PROP = "ShapesDiagram.ChildAdded";
	/** Property ID to use when a child is removed from this diagram. */
	public static final String CHILD_REMOVED_PROP = "ShapesDiagram.ChildRemoved";
	
	private static final long serialVersionUID = 1;
	
	private List shapes = new ArrayList();

	private boolean rulersVisibility = true;
	
	private boolean snapToGeometry = false;
	
	private boolean gridEnabled = false;
	
	public static String ID_ROUTER = "router";

	public static Integer ROUTER_MANUAL = new Integer(0);

	public static Integer ROUTER_MANHATTAN = new Integer(1);

	public static Integer ROUTER_SHORTEST_PATH = new Integer(2);

	protected DiagramRuler leftRuler, topRuler;

	public Integer connectionRouter = ROUTER_SHORTEST_PATH;

	public ShapesDiagram() {
		createRulers();
	}

	/**
	 * Add a shape to this diagram.
	 * 
	 * @param s
	 *            a non-null shape instance
	 * @return true, if the shape was added, false otherwise
	 */
	public Integer getConnectionRouter() {
		if (connectionRouter == null) {
			connectionRouter = ROUTER_SHORTEST_PATH;
		}
		return connectionRouter;
	}

	public void setConnectionRouter(Integer router) {
		Integer oldConnectionRouter = connectionRouter;
		connectionRouter = router;
		firePropertyChange(ID_ROUTER, oldConnectionRouter, connectionRouter);
	}

	public boolean addChild(Shape s) {
		if (s != null && shapes.add(s)) {// a shape for the vector
			firePropertyChange(CHILD_ADDED_PROP, null, s);
			return true;
		}
		return false;
	}

	/**
	 * Return a List of Shapes in this diagram. The returned List should not be
	 * modified.
	 */
	public List getChildren() {
		return shapes;
	}

	protected void createRulers() {
		leftRuler = new DiagramRuler(true);
		topRuler = new DiagramRuler(true);
	}

	public DiagramRuler getRuler(int orientation) {
		DiagramRuler result = null;
		switch (orientation) {
		case PositionConstants.NORTH:
			result = topRuler;
			break;
		case PositionConstants.WEST:
			result = leftRuler;
			break;
		}
		return result;
	}

	public boolean getRulerVisibility() {
		return rulersVisibility;
	}

	public boolean isGridEnabled() {
		return gridEnabled;
	}

	public boolean isSnapToGeometryEnabled() {
		return snapToGeometry;
	}

	public void setRulerVisibility(boolean newValue) {
		rulersVisibility = newValue;
	}

	public void setGridEnabled(boolean isEnabled) {
		gridEnabled = isEnabled;
	}

	public void setSnapToGeometry(boolean isEnabled) {
		snapToGeometry = isEnabled;
	}

	/**
	 * Remove a shape from this diagram.
	 * 
	 * @param s
	 *            a non-null shape instance;
	 * @return true, if the shape was removed, false otherwise
	 */
	public boolean removeChild(Shape s) {
		if (s != null && shapes.remove(s)) {
			firePropertyChange(CHILD_REMOVED_PROP, null, s);
			return true;
		}
		return false;
	}
}