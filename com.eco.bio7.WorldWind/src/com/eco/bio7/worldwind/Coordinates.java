/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
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

package com.eco.bio7.worldwind;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Globe;

import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import gov.nasa.worldwind.view.orbit.OrbitView;

public class Coordinates {
	public static void flyTo(Position latlon) {
		//Position p=new Position(new LatLon(Angle.fromDegrees(32),Angle.fromDegrees(22)),0);
		View view =WorldWindView.getWwd().getView();
		Globe globe = WorldWindView.getWwd().getModel().getGlobe();
		((BasicOrbitView) view).addPanToAnimator(latlon , Angle.ZERO , Angle.ZERO , 3e3, true);
		//view.applyStateIterator(FlyToOrbitViewStateIterator.createPanToIterator((OrbitView) view, globe, latlon , Angle.ZERO , Angle.ZERO , 3e3));
	}

}
