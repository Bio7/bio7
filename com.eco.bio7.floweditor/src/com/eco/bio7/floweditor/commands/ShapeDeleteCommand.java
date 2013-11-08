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
package com.eco.bio7.floweditor.commands;

import com.eco.bio7.floweditor.model.Connection;
import com.eco.bio7.floweditor.model.Shape;
import com.eco.bio7.floweditor.model.ShapesDiagram;
import java.util.Iterator;
import java.util.List;
import org.eclipse.gef.commands.Command;


/**
 * A command to remove a shape from its parent.
 * The command can be undone or redone.
 * @author Elias Volanakis
 */
public class ShapeDeleteCommand extends Command {
/** Shape to remove. */
private final Shape child;

/** ShapeDiagram to remove from. */
private final ShapesDiagram parent;
/** Holds a copy of the outgoing connections of child. */
private List sourceConnections;
/** Holds a copy of the incoming connections of child. */
private List targetConnections;
/** True, if child was removed from its parent. */
private boolean wasRemoved;

/**
 * Create a command that will remove the shape from its parent.
 * @param parent the ShapesDiagram containing the child
 * @param child    the Shape to remove
 * @throws IllegalArgumentException if any parameter is null
 */
public ShapeDeleteCommand(ShapesDiagram parent, Shape child) {
	if (parent == null || child == null) {
		throw new IllegalArgumentException();
	}
	setLabel("shape deletion");
	this.parent = parent;
	this.child = child;
}

/**
 * Reconnects a List of Connections with their previous endpoints.
 * @param connections a non-null List of connections
 */
private void addConnections(List connections) {
	for (Iterator iter = connections.iterator(); iter.hasNext();) {
		Connection conn = (Connection) iter.next();
		conn.reconnect();
	}
}

/* (non-Javadoc)
 * @see org.eclipse.gef.commands.Command#canUndo()
 */
public boolean canUndo() {
	return wasRemoved;
}

/* (non-Javadoc)
 * @see org.eclipse.gef.commands.Command#execute()
 */
public void execute() {
	// store a copy of incoming & outgoing connections before proceeding 
	sourceConnections = child.getSourceConnections();
	targetConnections = child.getTargetConnections();
	redo();
}

/* (non-Javadoc)
 * @see org.eclipse.gef.commands.Command#redo()
 */
public void redo() {
	// remove the child and disconnect its connections
	wasRemoved = parent.removeChild(child);
	if (wasRemoved) {
		removeConnections(sourceConnections);
		removeConnections(targetConnections);
	}
}

/**
 * Disconnects a List of Connections from their endpoints.
 * @param connections a non-null List of connections
 */
private void removeConnections(List connections) {
	for (Iterator iter = connections.iterator(); iter.hasNext();) {
		Connection conn = (Connection) iter.next();
		conn.disconnect();
	}
}

/* (non-Javadoc)
 * @see org.eclipse.gef.commands.Command#undo()
 */
public void undo() {
	// add the child and reconnect its connections
	if (parent.addChild(child)) {
		addConnections(sourceConnections);
		addConnections(targetConnections);
	}
}
}