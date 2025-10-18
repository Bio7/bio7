/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.rbridge.debug;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.util.Util;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;

import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.reditors.REditor;

public class REditorListener {

	public IPartListener2 listen() {

		IPartListener2 list = new IPartListener2() {

			@Override
			public void partActivated(IWorkbenchPartReference partRef) {

				ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();
				if (con != null) {

					IEditorPart editor = partRef.getPage().getActiveEditor();
					/*
					 * A workaround for MacOSX to give an editor hoover focus by creating an
					 * invisible shell!
					 */
					if (Util.isMac()) {
						if (editor != null) {
							REditor.activateEditorPage(editor);
						}
					}
					if (editor instanceof REditor) {
						REditor reditor = (REditor) editor;
						IResource resource = (IResource) reditor.getEditorInput().getAdapter(IResource.class);
						String type = "com.eco.bio7.redit.debugMarker";

						IMarker[] markers = null;
						try {
							markers = resource.findMarkers(type, true, IResource.DEPTH_INFINITE);
						} catch (CoreException e) {
							e.printStackTrace();
						}

						if (markers.length > 0) {

							con.setRDebugToolbarActions();
						} else {
							con.deleteDebugToolbarActions();
						}

					} else if (editor != null) {
						if (con != null) {
							con.deleteDebugToolbarActions();
						}

					}
				}

			}

			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {

			}

			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				IEditorPart editor = partRef.getPage().getActiveEditor();
				if (editor == null) {

					ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();
					if (con != null) {
						con.deleteDebugToolbarActions();
					}
				}

			}

			@Override
			public void partDeactivated(IWorkbenchPartReference partRef) {

			}

			@Override
			public void partOpened(IWorkbenchPartReference partRef) {

			}

			@Override
			public void partHidden(IWorkbenchPartReference partRef) {

			}

			@Override
			public void partVisible(IWorkbenchPartReference partRef) {

			}

			@Override
			public void partInputChanged(IWorkbenchPartReference partRef) {

			}

		};

		return list;

	}
}
