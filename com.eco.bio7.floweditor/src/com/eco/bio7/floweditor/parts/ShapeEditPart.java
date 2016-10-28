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

import com.eco.bio7.floweditor.commands.ConnectionCreateCommand;
import com.eco.bio7.floweditor.commands.ConnectionReconnectCommand;
import com.eco.bio7.floweditor.figures.Ellipse;
import com.eco.bio7.floweditor.figures.FileFigure;
import com.eco.bio7.floweditor.figures.FlowDecision;
import com.eco.bio7.floweditor.figures.StickyNoteFigure;
import com.eco.bio7.floweditor.figures.Triangle;
import com.eco.bio7.floweditor.figures.TriangleEnd;
import com.eco.bio7.floweditor.model.BeanShellScript;
import com.eco.bio7.floweditor.model.Connection;
import com.eco.bio7.floweditor.model.EllipticalShape;
import com.eco.bio7.floweditor.model.FlowDecisionShape;
import com.eco.bio7.floweditor.model.ModelElement;
import com.eco.bio7.floweditor.model.RectangularShape;
import com.eco.bio7.floweditor.model.Shape;
import com.eco.bio7.floweditor.model.TriangleEndShape;
import com.eco.bio7.floweditor.model.TriangleShape;
import com.eco.bio7.floweditor.shapes.FlowCellEditorLocator;
import com.eco.bio7.floweditor.shapes.FlowDirectEditManager;
import com.eco.bio7.floweditor.shapes.FlowDirectEditPolicy;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.EllipseAnchor;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy;
import org.eclipse.gef.requests.CreateConnectionRequest;
import org.eclipse.gef.requests.ReconnectRequest;

/**
 * EditPart used for Shape instances (more specific for EllipticalShape and
 * RectangularShape instances).
 * <p>
 * This edit part must implement the PropertyChangeListener interface, so it can
 * be notified of property changes in the corresponding model element.
 * </p>
 * 
 * @author Elias Volanakis
 */
class ShapeEditPart extends AbstractGraphicalEditPart implements PropertyChangeListener, NodeEditPart {

	private ConnectionAnchor anchor;

	private FlowDirectEditManager directManager = null;

	Label label = new Label();

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
		// allow removal of the associated model element
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ShapeComponentEditPolicy());
		installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new FlowDirectEditPolicy());

		// allow the creation of connections and
		// and the reconnection of connections between Shape instances
		installEditPolicy(EditPolicy.GRAPHICAL_NODE_ROLE, new GraphicalNodeEditPolicy() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy#
			 * getConnectionCompleteCommand
			 * (org.eclipse.gef.requests.CreateConnectionRequest)
			 */
			protected Command getConnectionCompleteCommand(CreateConnectionRequest request) {
				ConnectionCreateCommand cmd = (ConnectionCreateCommand) request.getStartCommand();
				cmd.setTarget((Shape) getHost().getModel());
				return cmd;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy#
			 * getConnectionCreateCommand
			 * (org.eclipse.gef.requests.CreateConnectionRequest)
			 */
			protected Command getConnectionCreateCommand(CreateConnectionRequest request) {
				Shape source = (Shape) getHost().getModel();
				int style = ((Integer) request.getNewObjectType()).intValue();
				ConnectionCreateCommand cmd = new ConnectionCreateCommand(source, style);
				request.setStartCommand(cmd);
				return cmd;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy#
			 * getReconnectSourceCommand
			 * (org.eclipse.gef.requests.ReconnectRequest)
			 */
			protected Command getReconnectSourceCommand(ReconnectRequest request) {
				Connection conn = (Connection) request.getConnectionEditPart().getModel();
				Shape newSource = (Shape) getHost().getModel();
				ConnectionReconnectCommand cmd = new ConnectionReconnectCommand(conn);
				cmd.setNewSource(newSource);
				return cmd;
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.gef.editpolicies.GraphicalNodeEditPolicy#
			 * getReconnectTargetCommand
			 * (org.eclipse.gef.requests.ReconnectRequest)
			 */
			protected Command getReconnectTargetCommand(ReconnectRequest request) {
				Connection conn = (Connection) request.getConnectionEditPart().getModel();
				Shape newTarget = (Shape) getHost().getModel();
				ConnectionReconnectCommand cmd = new ConnectionReconnectCommand(conn);
				cmd.setNewTarget(newTarget);
				return cmd;
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.editparts.AbstractGraphicalEditPart#createFigure()
	 */
	protected IFigure createFigure() {
		IFigure f = null;

		if (getModel() instanceof EllipticalShape) {
			f = new Ellipse();
			f.setOpaque(true);
			Display display = Display.getCurrent();

			f.setBackgroundColor(new Color(display,  99, 141, 67));
			StackLayout layout = new StackLayout();

			f.setLayoutManager(layout);
			f.setSize(10, 10);
			label.setBackgroundColor(new Color(display,  99, 141, 67));
			label.setOpaque(false);
			f.add(label);

		}

		else if (getModel() instanceof TriangleEndShape) {
			f = new TriangleEnd();

			f.setOpaque(true);
			Display display = Display.getCurrent();

			f.setBackgroundColor(new Color(display,  99, 141, 67));

			StackLayout layout = new StackLayout();

			f.setLayoutManager(layout);

			label.setBackgroundColor(ColorConstants.white);
			label.setOpaque(false);

			f.add(label);

		}

		else if (getModel() instanceof TriangleShape) {
			f = new Triangle();

			f.setOpaque(true);
			Display display = Display.getCurrent();

			f.setBackgroundColor(new Color(display,  99, 141, 67));
			StackLayout layout = new StackLayout();

			f.setLayoutManager(layout);

			label.setOpaque(false);

			f.add(label);

		} else if (getModel() instanceof FlowDecisionShape) {
			f = new FlowDecision();

			f.setOpaque(true);
			Display display = Display.getCurrent();

			f.setBackgroundColor(new Color(display,  99, 141, 67));
			StackLayout layout = new StackLayout();

			f.setLayoutManager(layout);

			label.setOpaque(false);

			label.setBackgroundColor(ColorConstants.white);

			f.add(label);

		}

		else if (getModel() instanceof BeanShellScript) {
			f = new FileFigure();

			f.setOpaque(true);
			Display display = Display.getCurrent();

			f.setBackgroundColor(new Color(display,  99, 141, 67));

			f.setLayoutManager(new ToolbarLayout(false));

			label.setOpaque(true);

			f.add(label);

		}

		else if (getModel() instanceof RectangularShape) {
			f = new StickyNoteFigure();

			f.setOpaque(true);
			Display display = Display.getCurrent();

			f.setBackgroundColor(new Color(display, 99, 141, 67));

			StackLayout layout = new StackLayout();

			f.setLayoutManager(layout);

			label.setOpaque(false);

			f.add(label);

		}

		return f;

	}

	/**
	 * Return a IFigure depending on the instance of the current model element.
	 * This allows this EditPart to be used for both sublasses of Shape.
	 */

	// Creation of the different models
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
		Shape e = (Shape) getModel();

		return (Shape) getModel();
	}

	protected ConnectionAnchor getConnectionAnchor() {
		if (anchor == null) {
			if (getModel() instanceof EllipticalShape)
				anchor = new EllipseAnchor(getFigure());
			else if (getModel() instanceof RectangularShape)
				anchor = new ChopboxAnchor(getFigure());

			else if (getModel() instanceof TriangleShape)
				anchor = new ChopboxAnchor(getFigure());
			else if (getModel() instanceof FlowDecisionShape)
				anchor = new ChopboxAnchor(getFigure());
			else if (getModel() instanceof BeanShellScript)
				anchor = new ChopboxAnchor(getFigure());

			else if (getModel() instanceof TriangleEndShape)
				anchor = new ChopboxAnchor(getFigure());

			else
				// if Shapes gets extended the conditions above must be updated
				throw new IllegalArgumentException("unexpected model");
		}
		return anchor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.editparts.AbstractGraphicalEditPart#getModelSourceConnections
	 * ()
	 */
	protected List getModelSourceConnections() {
		return getCastedModel().getSourceConnections();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.editparts.AbstractGraphicalEditPart#getModelTargetConnections
	 * ()
	 */
	protected List getModelTargetConnections() {
		return getCastedModel().getTargetConnections();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.NodeEditPart#getSourceConnectionAnchor(org.eclipse.gef
	 * .ConnectionEditPart)
	 */
	public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connection) {
		return getConnectionAnchor();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.NodeEditPart#getSourceConnectionAnchor(org.eclipse.gef
	 * .Request)
	 */
	public ConnectionAnchor getSourceConnectionAnchor(Request request) {
		return getConnectionAnchor();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.NodeEditPart#getTargetConnectionAnchor(org.eclipse.gef
	 * .ConnectionEditPart)
	 */
	public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connection) {
		return getConnectionAnchor();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.gef.NodeEditPart#getTargetConnectionAnchor(org.eclipse.gef
	 * .Request)
	 */
	public ConnectionAnchor getTargetConnectionAnchor(Request request) {
		return getConnectionAnchor();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.
	 * PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		String prop = evt.getPropertyName();

		if (Shape.SIZE_PROP.equals(prop) || Shape.LOCATION_PROP.equals(prop)) {
			refreshVisuals();
		} else if (Shape.SOURCE_CONNECTIONS_PROP.equals(prop)) {
			refreshSourceConnections();
		} else if (Shape.TARGET_CONNECTIONS_PROP.equals(prop)) {
			refreshTargetConnections();
		} else if ("color".equals(prop)) {
			IFigure ifigure = getFigure();
			ifigure.setBackgroundColor(ColorConstants.white);

			ifigure.repaint();
		} else if ("defaultcolor".equals(prop)) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					IFigure ifigure = getFigure();
					ifigure.setBackgroundColor(new Color(null,  99, 141, 67));

					ifigure.repaint();
				}
			});
		}

		else if (Shape.FILE_PROP.equals(prop)) {

			refreshVisuals();

			IFigure ifigure = getFigure();
			List list = ifigure.getChildren();

		} else if (Shape.INIT_LOOP.equals(prop)) {
			refreshVisuals();
			IFigure ifigure = getFigure();
			List list = ifigure.getChildren();
			/* Avoid error for label (information) Figure! */
			if (list.get(0) instanceof Label) {
				Label label = (Label) list.get(0);

				label.setText((String) evt.getNewValue());
			}

		} else if (Shape.TARGET_CONNECTIONS_PROP.equals(prop)) {
			refreshTargetConnections();
		}
	}

	protected void refreshVisuals() {
		// notify parent container of changed position & location
		// if this line is removed, the XYLayoutManager used by the parent
		// container
		// (the Figure of the ShapesDiagramEditPart), will not know the bounds
		// of this figure
		// and will not draw it correctly.
		Rectangle bounds = new Rectangle(getCastedModel().getLocation(), getCastedModel().getSize());
		((GraphicalEditPart) getParent()).setLayoutConstraint(this, getFigure(), bounds);

		String text = getCastedModel().getText();
		// Here I get the text of the instance
		label.setText(text);
	}

	public void performRequest(Request req) {

		if (req.getType().equals(RequestConstants.REQ_DIRECT_EDIT)) {
			performDirectEdit();
			return;
		}
		// Opens the file associated with the shape !!
		else if (req.getType().equals(RequestConstants.REQ_OPEN)) {
			// Get the relative file location and convert !!
			Shape sh = getCastedModel();
			/*
			 * Open only the shape associated with java, BeanShell, Groovy, R
			 * etc.!
			 */
			if (sh instanceof BeanShellScript) {
				IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

				IPath location = Path.fromOSString(sh.getText());

				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IFile iFile = null;
				try {
					iFile = workspace.getRoot().getFile(location);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				IWorkbenchPage page = workbenchWindow.getActivePage();
				IEditorDescriptor des = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(iFile.getFullPath().toString());
				String id = null;
				/* Try to open the default editors else the text editor! */
				if (des != null) {
					id = des.getId();

					try {
						IEditorPart par = page.openEditor(new FileEditorInput(iFile), id);

					} catch (PartInitException exception) {

						System.out.println(exception.getMessage());
					}

				} else {
					try {
						page.openEditor(new FileEditorInput(iFile), "org.eclipse.ui.DefaultTextEditor");
					} catch (PartInitException e) {

						e.printStackTrace();
					}
				}

			}
			return;

		}

		super.performRequest(req);
	}

	private void performDirectEdit() {
		if (directManager == null) {
			directManager = new FlowDirectEditManager(this, TextCellEditor.class, new FlowCellEditorLocator(getFigure()));
		}
		directManager.show();
	}
}