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

import com.eco.bio7.floweditor.model.ModelElement;
import com.eco.bio7.floweditor.model.Shape;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractTreeEditPart;

/**
 * TreeEditPart used for Shape instances (more specific for EllipticalShape and
 * RectangularShape instances). This is used in the Outline View of the
 * ShapesEditor.
 * <p>
 * This edit part must implement the PropertyChangeListener interface, so it can
 * be notified of property changes in the corresponding model element.
 * </p>
 * 
 * @author Elias Volanakis
 */
class ShapeTreeEditPart extends AbstractTreeEditPart implements PropertyChangeListener {

	/**
	 * Create a new instance of this edit part using the given model element.
	 * 
	 * @param model
	 *            a non-null Shapes instance
	 */
	ShapeTreeEditPart(Shape model) {
		super(model);
	}

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
	 * @see org.eclipse.gef.editparts.AbstractTreeEditPart#createEditPolicies()
	 */
	protected void createEditPolicies() {
		// allow removal of the associated model element
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ShapeComponentEditPolicy());
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

	private Shape getCastedModel() {
		return (Shape) getModel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.editparts.AbstractTreeEditPart#getImage()
	 */
	protected Image getImage() {
		return getCastedModel().getIcon();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.editparts.AbstractTreeEditPart#getText()
	 */
	protected String getText() {
		/* Files: the name and the type; notes: the beginning of the text; loops: the count */
		Shape s = getCastedModel();
		String text = s.getText() == null ? "" : s.getText().trim();
		if (s instanceof com.eco.bio7.floweditor.model.BeanShellScript) {
			return com.eco.bio7.floweditor.figures.FlowColors.fileName(text) + "  \u00b7  " + com.eco.bio7.floweditor.figures.FlowColors.typeName(text);
		}
		if (s instanceof com.eco.bio7.floweditor.model.RectangularShape) {
			String line = text.replaceAll("\\s+", " ");
			return line.isEmpty() ? "Note" : line.length() > 48 ? line.substring(0, 47) + "\u2026" : line;
		}
		if (s instanceof com.eco.bio7.floweditor.model.EllipticalShape) {
			return text.isEmpty() ? "Loop" : "Loop \u00d7 " + text;
		}
		if (s instanceof com.eco.bio7.floweditor.model.FlowDecisionShape && !text.isEmpty()) {
			return "Decision: " + text;
		}
		return s.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.
	 * PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				/* A new connection changes the order of the flow in the outline */
				String p = evt.getPropertyName();
				if ((Shape.SOURCE_CONNECTIONS_PROP.equals(p) || Shape.TARGET_CONNECTIONS_PROP.equals(p)) && getParent() != null) {
					getParent().refresh();
				}
				refreshVisuals(); // this will cause an invocation of getImage()
									// and
									// getText(), see below
			}
		});
	}
}