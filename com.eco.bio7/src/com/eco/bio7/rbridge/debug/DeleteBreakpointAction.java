/*******************************************************************************
 * Copyright (c) 2007-2014 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.rbridge.debug;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.console.ConsolePageParticipant;

public class DeleteBreakpointAction extends AbstractRulerActionDelegate implements IEditorActionDelegate {

	private class ToggleBreakpointAction extends Action {

		public ToggleBreakpointAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
			super("Remove breakpoints");

		}

		public IMarker[] findMyMarkers(IResource target) {
			String type = "com.eco.bio7.redit.debugMarker";

			IMarker[] markers = null;
			try {
				markers = target.findMarkers(type, true, IResource.DEPTH_INFINITE);
			} catch (CoreException e) {

				e.printStackTrace();
			}
			return markers;
		}

		public void run() {

			IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

			IResource resource = (IResource) editore.getEditorInput().getAdapter(IResource.class);

			IMarker[] markersfind = findMyMarkers(resource);
			for (int i = 0; i < markersfind.length; i++) {
				try {
					String func = (String) markersfind[i].getAttribute(IMarker.TEXT);
					System.out.println(func);
					System.out.println("untrace(" + func + ")");
					ConsolePageParticipant.pipeInputToConsole("untrace(" + func + ")", true, false);
					markersfind[i].delete();

				} catch (CoreException e) {

					e.printStackTrace();
				}

			}
			
			 ConsolePageParticipant inst = ConsolePageParticipant.getConsolePageParticipantInstance();
			 inst.deleteDebugToolbarActions();

		}
	}

	@Override
	protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
		return new ToggleBreakpointAction(editor, rulerInfo);
	}
}
