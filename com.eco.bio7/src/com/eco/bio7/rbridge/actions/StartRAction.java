/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.rbridge.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindow;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsoleInterpreterAction;
import com.eco.bio7.rbridge.RBridge;
import com.eco.bio7.rbridge.REval;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.backend.EmbeddedR;
import com.eco.bio7.rbridge.backend.RBackend;
import com.eco.bio7.rffm.RException;

/**
 * The toolbar action "Start R": starts R (embedded R, in the process of Bio7) in
 * the console. The menu interrupts a computation. A file dropped on Bio7 before
 * the start (an R workspace) is loaded after the start.
 */
public class StartRAction extends Action implements IMenuCreator {

	private Menu menu;

	private static boolean fromDragDrop;

	private static String[] fileList;

	public StartRAction(String text, IWorkbenchWindow window) {
		super(text, AS_DROP_DOWN_MENU);
		setId("com.eco.bio7.start_r");
		setActionDefinitionId("com.eco.bio7.Start_R_Action");
		setImageDescriptor(Bio7Plugin.getImageDescriptor("/icons/maintoolbar/r.svg"));
		setMenuCreator(this);
	}

	@Override
	public void run() {
		if (RBridge.isConnected()) {
			Bio7Dialog.message("R is running.\n\nR runs in the process of Bio7: restart Bio7 to restart R.");
			return;
		}
		/* Starts R in the console (in a job, see ConsolePageParticipant.processRCommand) */
		ConsoleInterpreterAction.getInstance().startR();
		if (fromDragDrop) {
			fromDragDrop = false;
			if (RBridge.isConnected()) {
				loadFile();
			}
		}
	}

	/* Loads the dropped R workspace */
	private void loadFile() {
		String[] files = fileList;
		RBackend r = RBridge.getBackend();
		if (files == null || files.length == 0 || r == null) {
			return;
		}
		if (RState.isBusy()) {
			Bio7Dialog.message("R is busy!");
			return;
		}
		try {
			r.assign(".bio7TempRScriptFile", files[0].replace("\\", "/"));
			r.voidEval("try(load(file = .bio7TempRScriptFile)); rm(.bio7TempRScriptFile)");
			REval.listRObjects();
		} catch (RException e) {
			System.err.println("The file could not be loaded: " + e.getMessage());
		}
	}

	public static void setFileList(String[] fileList) {
		StartRAction.fileList = fileList;
	}

	public static String[] getFileList() {
		return fileList;
	}

	public static boolean isFromDragDrop() {
		return fromDragDrop;
	}

	public static void setFromDragDrop(boolean fromDragDrop) {
		StartRAction.fromDragDrop = fromDragDrop;
	}

	@Override
	public void dispose() {
		if (menu != null) {
			menu.dispose();
		}
	}

	@Override
	public Menu getMenu(Control parent) {
		if (menu != null) {
			menu.dispose();
		}
		menu = new Menu(parent);
		Image image = Bio7Plugin.getImageDescriptor("/icons/views/stopraction.svg").createImage();
		menu.addDisposeListener(_ -> image.dispose());

		MenuItem start = new MenuItem(menu, SWT.PUSH);
		start.setText("Start R");
		start.setEnabled(!RBridge.isConnected());
		start.addListener(SWT.Selection, _ -> run());

		MenuItem interrupt = new MenuItem(menu, SWT.PUSH);
		interrupt.setText("Interrupt R");
		interrupt.setImage(image);
		interrupt.setEnabled(RBridge.isConnected());
		interrupt.addListener(SWT.Selection, _ -> EmbeddedR.interrupt());

		new MenuItem(menu, SWT.SEPARATOR);
		MenuItem info = new MenuItem(menu, SWT.PUSH);
		info.setText("R runs in the process of Bio7: restart Bio7 to restart R");
		info.setEnabled(false);
		return menu;
	}

	@Override
	public Menu getMenu(Menu parent) {
		return null;
	}
}
