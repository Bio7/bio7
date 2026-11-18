/*******************************************************************************
 * Copyright (c) 2003, 2024 IBM Corporation and others.
 *
 * Adapted from the GEF Logic example (org.eclipse.gef.examples.logic, CreateGuideCommand)
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
