/*******************************************************************************
 * Copyright (c) 2003, 2024 IBM Corporation and others.
 *
 * Adapted from the GEF Logic example (org.eclipse.gef.examples.logic, MoveGuideCommand)
 * by uniera.org and M. Austenfeld for the Bio7 flow editor.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     uniera.org - adaptation
 *     M. Austenfeld - Bio7
 *******************************************************************************/
package com.eco.bio7.floweditor.ruler;

import java.util.Iterator;

import org.eclipse.draw2d.geometry.Point;

import org.eclipse.gef.commands.Command;


/**
 * 
 * Comment on GuideDeleteCommand here
 * 
 * @author Song Sun
 * 
 */
public class GuideMoveCommand extends Command {

	private int pDelta;

	private DiagramGuide guide;

	public GuideMoveCommand(DiagramGuide guide, int positionDelta) {
		super("Delete Guide");
		this.guide = guide;
		pDelta = positionDelta;
	}

	public void execute() {
		guide.setPosition(guide.getPosition() + pDelta);
		Iterator iter = guide.getParts().iterator();
		while (iter.hasNext()) {
			IArea part = (IArea) iter.next();
			Point location = part.getLocation().getCopy();
			if (guide.isHorizontal()) {
				location.y += pDelta;
			} else {
				location.x += pDelta;
			}
			part.setLocation(location);
		}
	}

	public void undo() {
		guide.setPosition(guide.getPosition() - pDelta);
		Iterator iter = guide.getParts().iterator();
		while (iter.hasNext()) {
			IArea part = (IArea) iter.next();
			Point location = part.getLocation().getCopy();
			if (guide.isHorizontal()) {
				location.y -= pDelta;
			} else {
				location.x -= pDelta;
			}
			part.setLocation(location);
		}
	}

}
