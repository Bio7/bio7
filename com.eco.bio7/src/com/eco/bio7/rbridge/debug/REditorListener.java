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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;

import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.reditors.REditor;

public class REditorListener {
	
	
	
	public IPartListener2 listen(){
		
	
		IPartListener2 list=new IPartListener2() {

		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			
			ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();
			if (con != null) {

				IEditorPart editor = partRef.getPage().getActiveEditor();
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

				} else if (editor !=null) {
					if(con!=null){
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

}}
