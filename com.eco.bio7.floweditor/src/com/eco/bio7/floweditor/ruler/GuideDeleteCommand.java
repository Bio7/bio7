/*******************************************************************************
 * Copyright (c) 2003, 2024 IBM Corporation and others.
 *
 * Adapted from the GEF Logic example (org.eclipse.gef.examples.logic, DeleteGuideCommand)
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
