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
package com.eco.bio7.floweditor.parts;

import com.eco.bio7.floweditor.commands.ShapeCreateCommand;
import com.eco.bio7.floweditor.commands.ShapeSetConstraintCommand;
import com.eco.bio7.floweditor.model.BeanShellScript;
import com.eco.bio7.floweditor.model.EllipticalShape;
import com.eco.bio7.floweditor.model.FlowDecisionShape;
import com.eco.bio7.floweditor.model.ModelElement;
import com.eco.bio7.floweditor.model.RectangularShape;
import com.eco.bio7.floweditor.model.Shape;
import com.eco.bio7.floweditor.model.ShapesDiagram;
import com.eco.bio7.floweditor.model.TriangleEndShape;
import com.eco.bio7.floweditor.model.TriangleShape;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.draw2d.AutomaticRouter;
import org.eclipse.draw2d.BendpointConnectionRouter;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.FanRouter;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ManhattanConnectionRouter;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.ShortestPathConnectionRouter;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.CompoundSnapToHelper;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.SnapToGuides;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.RootComponentEditPolicy;
import org.eclipse.gef.editpolicies.SnapFeedbackPolicy;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.rulers.RulerProvider;

/**
 * EditPart for the a ShapesDiagram instance.
 * <p>
 * This edit part server as the main diagram container, the white area where
 * everything else is in. Also responsible for the container's layout (the way
 * the container rearanges is contents) and the container's capabilities (edit
 * policies).
 * </p>
 * <p>
 * This edit part must implement the PropertyChangeListener interface, so it can
 * be notified of property changes in the corresponding model element.
 * </p>
 * 
 * @author Elias Volanakis
 */
class DiagramEditPart extends AbstractGraphicalEditPart implements
		PropertyChangeListener {

	/**
	 * Upon activation, attach to the model element as a property change
	 * listener.
	 */
	public void activate() {
		if (!isActive()) {
			super.activate();
			((ModelElement) getModel()).addPropertyChangeListener(this);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.editparts.AbstractEditPart#createEditPolicies()
	 */
	protected void createEditPolicies() {
		// disallows the removal of this edit part from its parent
		installEditPolicy(EditPolicy.COMPONENT_ROLE,
				new RootComponentEditPolicy());
		// handles constraint changes (e.g. moving and/or resizing) of model
		// elements
		// and creation of new model elements
		installEditPolicy(EditPolicy.LAYOUT_ROLE,
				new ShapesXYLayoutEditPolicy());
		installEditPolicy("Snap Feedback", new SnapFeedbackPolicy()); //$NON-NLS-1$

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#createFigure()
	 */
	protected IFigure createFigure() {
		Figure f = new FreeformLayer();

		f.setBorder(new MarginBorder(3));
		f.setLayoutManager(new FreeformLayout());

		// Create the static router for the connection layer
		ConnectionLayer connLayer = (ConnectionLayer) getLayer(LayerConstants.CONNECTION_LAYER);
		connLayer.setAntialias(1);// set Antialiasing (on) for the connections
		connLayer.setConnectionRouter(new ShortestPathConnectionRouter(f));

		return f;
	}

	public Object getAdapter(Class adapter) {
		if (adapter == SnapToHelper.class) {
			List snapStrategies = new ArrayList();
			Boolean val = (Boolean) getViewer().getProperty(
					RulerProvider.PROPERTY_RULER_VISIBILITY);
			if (val != null && val.booleanValue())
				snapStrategies.add(new SnapToGuides(this));
			val = (Boolean) getViewer().getProperty(
					SnapToGeometry.PROPERTY_SNAP_ENABLED);
			if (val != null && val.booleanValue())
				snapStrategies.add(new SnapToGeometry(this));
			val = (Boolean) getViewer().getProperty(
					SnapToGrid.PROPERTY_GRID_ENABLED);
			if (val != null && val.booleanValue())
				snapStrategies.add(new SnapToGrid(this));

			if (snapStrategies.size() == 0)
				return null;
			if (snapStrategies.size() == 1)
				return snapStrategies.get(0);

			SnapToHelper ss[] = new SnapToHelper[snapStrategies.size()];
			for (int i = 0; i < snapStrategies.size(); i++)
				ss[i] = (SnapToHelper) snapStrategies.get(i);
			return new CompoundSnapToHelper(ss);
		}
		return super.getAdapter(adapter);
	}

	/**
	 * Upon deactivation, detach from the model element as a property change
	 * listener.
	 */
	public void deactivate() {
		if (isActive()) {
			super.deactivate();
			((ModelElement) getModel()).removePropertyChangeListener(this);
		}
	}

	private ShapesDiagram getCastedModel() {
		return (ShapesDiagram) getModel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.editparts.AbstractEditPart#getModelChildren()
	 */
	protected List getModelChildren() {
		return getCastedModel().getChildren(); // return a list of shapes
	}

	ShapesDiagram getDiagram() {
		return (ShapesDiagram) getModel();
	}

	public void refreshVisuals() {
		ConnectionLayer cLayer = (ConnectionLayer) getLayer(LayerConstants.CONNECTION_LAYER);
		// cLayer.setAntialias(SWT.ON);

		if (getDiagram().getConnectionRouter().equals(
				ShapesDiagram.ROUTER_MANUAL)) {
			AutomaticRouter router = new FanRouter();
			router.setNextRouter(new BendpointConnectionRouter());
			cLayer.setConnectionRouter(router);
		} else if (getDiagram().getConnectionRouter().equals(
				ShapesDiagram.ROUTER_MANHATTAN)) {

			cLayer.setConnectionRouter(new ManhattanConnectionRouter());

		} else {
			cLayer.setConnectionRouter(new ShortestPathConnectionRouter(
					getFigure()));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.beans.PropertyChangeListener#propertyChange(PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();
		refreshVisuals();
		// these properties are fired when Shapes are added into or removed from
		// the ShapeDiagram instance and must cause a call of refreshChildren()
		// to update the diagram's contents.
		if (ShapesDiagram.CHILD_ADDED_PROP.equals(prop)
				|| ShapesDiagram.CHILD_REMOVED_PROP.equals(prop)) {
			refreshChildren();
		}
	}

	/**
	 * EditPolicy for the Figure used by this edit part. Children of
	 * XYLayoutEditPolicy can be used in Figures with XYLayout.
	 * 
	 * @author Elias Volanakis
	 */
	private static class ShapesXYLayoutEditPolicy extends XYLayoutEditPolicy {

		/*
		 * (non-Javadoc)
		 * 
		 * @see ConstrainedLayoutEditPolicy#createChangeConstraintCommand(ChangeBoundsRequest,
		 *      EditPart, Object)
		 */
		protected Command createChangeConstraintCommand(
				ChangeBoundsRequest request, EditPart child, Object constraint) {
			if (child instanceof ShapeEditPart
					&& constraint instanceof Rectangle) {
				// return a command that can move and/or resize a Shape
				return new ShapeSetConstraintCommand((Shape) child.getModel(),
						request, (Rectangle) constraint);
			}
			return super.createChangeConstraintCommand(request, child,
					constraint);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see ConstrainedLayoutEditPolicy#createChangeConstraintCommand(EditPart,
		 *      Object)
		 */
		protected Command createChangeConstraintCommand(EditPart child,
				Object constraint) {
			// not used in this example
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see LayoutEditPolicy#getCreateCommand(CreateRequest)
		 */
		protected Command getCreateCommand(CreateRequest request) {
			Object childClass = request.getNewObjectType();
			if (childClass == EllipticalShape.class
					|| childClass == RectangularShape.class
					|| childClass == BeanShellScript.class
					|| childClass == TriangleShape.class
					|| childClass == FlowDecisionShape.class
					|| childClass == TriangleEndShape.class) {
				// return a command that can add a Shape to a ShapesDiagram
				return new ShapeCreateCommand((Shape) request.getNewObject(),
						(ShapesDiagram) getHost().getModel(),
						(Rectangle) getConstraintFor(request));
			}
			return null;
		}

	}

}