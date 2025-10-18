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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;

public class DebugStopAction extends Action {

	public DebugStopAction() {
		super("Stop");

		setId("Stop");
		setText("Stop (Q) - Halt execution and jump to the top-level immediately.");

		//ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/terminatedlaunch_obj.gif")));
		ImageDescriptor desc = Bio7Plugin.getImageDescriptor("/icons/rdebug/terminatedlaunch_obj.png");
		this.setImageDescriptor(desc);
	}

	public void run() {
		/*Restore a Rserve connection if it was alive before debugging start!*/
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		boolean wasRserveAlive=store.getBoolean("RSERVE_ALIVE_DEBUG");
		if(wasRserveAlive){
			Bio7Action.callRserve();
		}

		String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

		if (selectionConsole.equals("R")) {

			ConsolePageParticipant.pipeInputToConsole("Q", true, false);
			System.out.println("Q");
			IEditorPart edit = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

			ITextEditor editor = (ITextEditor) edit;
			IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
			try {
				resource.deleteMarkers("com.eco.bio7.reditor.debugrulermark", false, IResource.DEPTH_ZERO);
			} catch (CoreException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					Grid debugGrid = DebugVariablesView.getDebugVariablesGrid();

					if (debugGrid!=null&&debugGrid.isDisposed()==false) {
						int itemCount = debugGrid.getItemCount();
						if (itemCount > 0) {

							debugGrid.remove(0, itemCount - 1);

						}
					}

				}

			});

			// inst.toolBarManager.update(true);

		} else {
			Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
		}

	}

}