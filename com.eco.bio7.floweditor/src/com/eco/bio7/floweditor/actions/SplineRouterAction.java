/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.floweditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.floweditor.model.ShapesDiagram;
import com.eco.bio7.floweditor.shapes.ShapesEditor;

/** The connections of the flow as curves (splines). */
public class SplineRouterAction extends Action {

	public SplineRouterAction(String text, IWorkbenchWindow window) {
		super(text, AS_RADIO_BUTTON);
		setId("Spline");
	}

	public void run() {
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor instanceof ShapesEditor shapes) {
			shapes.diagram.setConnectionRouter(ShapesDiagram.ROUTER_SPLINE);
		}
	}
}
