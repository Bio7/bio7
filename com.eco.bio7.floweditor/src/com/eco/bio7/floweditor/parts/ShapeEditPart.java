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
				/* A decision: the side of the start gives the value (left: true, right: false) */
				if (source instanceof FlowDecisionShape && request.getLocation() != null) {
					String value = decisionValue(request.getLocation());
					cmd.setBoolean(value);
					request.getExtendedData().put(DECISION_VALUE, value);
				}
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
		/*
		 * The shapes paint themselves in the colors of the theme (see FlowColors):
		 * start and end as green pills, files as cards with the accent of their type,
		 * loops and decisions in orange, notes in amber
		 */
		IFigure f = null;
		label.setOpaque(false);
		if (getModel() instanceof EllipticalShape) {
			f = new Ellipse();
			f.setLayoutManager(new StackLayout());
			f.setSize(10, 10);
			/* The count below the glyph of the loop */
			label.setFont(com.eco.bio7.floweditor.figures.FlowColors.bold());
			label.setBorder(new org.eclipse.draw2d.MarginBorder(18, 0, 0, 0));
			f.add(label);
		} else if (getModel() instanceof TriangleEndShape) {
			f = new TriangleEnd();
		} else if (getModel() instanceof TriangleShape) {
			f = new Triangle();
		} else if (getModel() instanceof FlowDecisionShape) {
			f = new FlowDecision();
			f.setLayoutManager(new StackLayout());
			label.setFont(com.eco.bio7.floweditor.figures.FlowColors.bold());
			/* The text inside the diamond (beside it: the labels of the outputs) */
			label.setBorder(new org.eclipse.draw2d.AbstractBorder() {
				public org.eclipse.draw2d.geometry.Insets getInsets(IFigure figure) {
					int z = FlowDecision.zone(figure.getBounds().width) + 10;
					return new org.eclipse.draw2d.geometry.Insets(0, z, 0, z);
				}

				public void paint(IFigure figure, org.eclipse.draw2d.Graphics graphics, org.eclipse.draw2d.geometry.Insets insets) {
				}
			});
			f.add(label);
		} else if (getModel() instanceof BeanShellScript) {
			/* The type and the name of the file (the path: tooltip) */
			f = new FileFigure();
		} else if (getModel() instanceof RectangularShape) {
			/* The text of the note, wrapped */
			f = new StickyNoteFigure();
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

	/* The value of a new connection of a decision (in the data of the request) */
	private static final String DECISION_VALUE = "bio7.decision.value";

	/*
	 * The value of a new connection of a decision: the side of the start (left:
	 * true, right: false); the other value if this one is connected already.
	 */
	private String decisionValue(org.eclipse.draw2d.geometry.Point location) {
		org.eclipse.draw2d.geometry.Point p = location.getCopy();
		getFigure().translateToRelative(p);
		String value = p.x < getFigure().getBounds().getCenter().x ? "true" : "false";
		String other = "true".equals(value) ? "false" : "true";
		boolean used = false, otherUsed = false;
		for (Object o : getCastedModel().getSourceConnections()) {
			String b = ((Connection) o).getBoolean();
			used |= value.equals(b);
			otherUsed |= other.equals(b);
		}
		return used && !otherUsed ? other : value;
	}

	/* The output of a decision for a value: the left (true) or right (false) corner */
	private ConnectionAnchor port(boolean value) {
		return new org.eclipse.draw2d.AbstractConnectionAnchor(getFigure()) {
			public org.eclipse.draw2d.geometry.Point getLocation(org.eclipse.draw2d.geometry.Point reference) {
				org.eclipse.draw2d.geometry.Point p = ((FlowDecision) getOwner()).port(value).getCopy();
				getOwner().translateToAbsolute(p);
				return p;
			}
		};
	}

	/* The input of a decision: the top corner */
	private ConnectionAnchor input() {
		return new org.eclipse.draw2d.AbstractConnectionAnchor(getFigure()) {
			public org.eclipse.draw2d.geometry.Point getLocation(org.eclipse.draw2d.geometry.Point reference) {
				org.eclipse.draw2d.geometry.Point p = ((FlowDecision) getOwner()).input().getCopy();
				getOwner().translateToAbsolute(p);
				return p;
			}
		};
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
		/* A decision: the connection starts at the output of its value */
		if (getModel() instanceof FlowDecisionShape && getFigure() instanceof FlowDecision && connection.getModel() instanceof Connection c) {
			return port(!"false".equals(c.getBoolean()));
		}
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
		/* A new connection of a decision: the feedback starts at the output of its value */
		if (getModel() instanceof FlowDecisionShape && getFigure() instanceof FlowDecision && request.getExtendedData().get(DECISION_VALUE) instanceof String value) {
			return port(!"false".equals(value));
		}
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
		if (getFigure() instanceof FlowDecision) {
			return input();
		}
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
		if (getFigure() instanceof FlowDecision) {
			return input();
		}
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
		} else if ("color".equals(prop) || "defaultcolor".equals(prop)) {
			/* The shape is executed (color) or finished (defaultcolor): its highlight */
			boolean running = "color".equals(prop);
			Runnable update = () -> {
				if (getFigure() instanceof com.eco.bio7.floweditor.figures.RunningFigure rf) {
					rf.setRunning(running);
				}
			};
			if (Display.getCurrent() != null) {
				update.run();
			} else {
				PlatformUI.getWorkbench().getDisplay().syncExec(update);
			}
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
			if (!list.isEmpty() && list.get(0) instanceof Label) {
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
		IFigure figure = getFigure();
		if (figure instanceof FileFigure file) {
			file.setPath(text);
		} else if (figure instanceof StickyNoteFigure note) {
			note.setText(text);
		} else {
			label.setText(text);
		}
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

				IWorkbenchPage page = workbenchWindow.getActivePage();
				IEditorPart flowEditor = page.getActiveEditor();
				java.io.File flow = flowEditor != null ? com.eco.bio7.floweditor.model.FlowPaths.flowFile(flowEditor.getEditorInput()) : null;
				/* The file of the component (also in a Git repository or outside of the workspace) */
				java.io.File location = com.eco.bio7.floweditor.model.FlowPaths.location(sh.getText(), flow);
				if (location == null || !location.exists()) {
					System.err.println("Flow: file not found: " + sh.getText() + (location != null ? " (" + location + ")" : ""));
					return;
				}
				IFile iFile = com.eco.bio7.floweditor.model.FlowPaths.workspaceFile(location);
				try {
					if (iFile != null) {
						IEditorDescriptor des = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(iFile.getName());
						/* Try to open the default editors else the text editor! */
						page.openEditor(new FileEditorInput(iFile), des != null ? des.getId() : "org.eclipse.ui.DefaultTextEditor");
					} else {
						/* A file outside of the workspace */
						org.eclipse.ui.ide.IDE.openEditorOnFileStore(page, org.eclipse.core.filesystem.EFS.getLocalFileSystem().getStore(location.toURI()));
					}
				} catch (PartInitException exception) {
					System.out.println(exception.getMessage());
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