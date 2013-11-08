/**
 * (c)Copyleft uniera.org
 */
package com.eco.bio7.floweditor.ruler;

import org.eclipse.gef.commands.Command;


/**
 * 
 * Comment on GuideDeleteCommand here
 * 
 * @author Song Sun
 * 
 */
public class GuideCreateCommand extends Command {

	private DiagramGuide guide;

	private DiagramRuler parent;

	private int position;

	public GuideCreateCommand(DiagramRuler parent, int position) {
		super("Create Guide");		
		this.parent = parent;
		this.position = position;
	}

	public boolean canUndo() {
		return true;
	}

	public void execute() {
		if (guide == null)
			guide = new DiagramGuide(!parent.isHorizontal());
		guide.setPosition(position);
		parent.addGuide(guide);
	}

	public void undo() {
		parent.removeGuide(guide);
	}

}
