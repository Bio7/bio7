/**
 * (c)Copyleft uniera.org
 */
package com.eco.bio7.floweditor.ruler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.gef.commands.Command;

/**
 * 
 * Comment on GuideDeleteCommand here
 * Original author: Song Sun
 * @author Song Sun
 * Adapted for Bio7
 */
public class GuideDeleteCommand extends Command {

	private DiagramRuler parent;

	private IGuide guide;

	private Map oldParts;

	public GuideDeleteCommand(IGuide guide, DiagramRuler parent) {
		super("Delete Guide");
		this.guide = guide;
		this.parent = parent;
	}

	public boolean canUndo() {
		return true;
	}

	public void execute() {
		oldParts = new HashMap(guide.getMap());
		Iterator iter = oldParts.keySet().iterator();
		while (iter.hasNext()) {
			guide.detachPart((IArea) iter.next());
		}
		parent.removeGuide(guide);
	}

	public void undo() {
		parent.addGuide(guide);
		Iterator iter = oldParts.keySet().iterator();
		while (iter.hasNext()) {
			IArea part = (IArea) iter.next();
			guide.attachPart(part, ((Integer) oldParts.get(part)).intValue());
		}
	}
}
