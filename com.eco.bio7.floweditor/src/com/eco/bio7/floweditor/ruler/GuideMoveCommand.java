/**
 * (c)Copyleft uniera.org
 */
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
