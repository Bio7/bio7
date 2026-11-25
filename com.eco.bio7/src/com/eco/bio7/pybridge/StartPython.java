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
package com.eco.bio7.pybridge;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.console.ConsoleInterpreterAction;

/**
 * The action of the main toolbar (beside "Start R"): starts the embedded
 * Python (the Python of the preferences) and opens the Python console. If
 * Python runs, the console is switched to Python. The menu interrupts a
 * computation (as "Start R").
 */
public class StartPython extends Action implements IMenuCreator {

	private final IWorkbenchWindow window;
	private Menu menu;

	public StartPython(String text, IWorkbenchWindow window) {
		super(text, AS_DROP_DOWN_MENU);
		this.window = window;
		setId("com.eco.bio7.start_python");
		setToolTipText("Start Python (embedded in Bio7: Python runs in the process of Bio7, restart Bio7 to restart Python)");
		setImageDescriptor(Bio7Plugin.getImageDescriptor("/icons/views/pythonaction.svg"));
		setMenuCreator(this);
	}

	@Override
	public void run() {
		var store = Bio7Plugin.getDefault().getPreferenceStore();
		if (!EmbeddedPython.isSelected()) {
			boolean enable = MessageDialog.openQuestion(window.getShell(), "Python",
					"The embedded Python is not selected in the preferences (Python is used as a separate process).\n\nUse the embedded Python?");
			if (!enable) {
				ConsoleInterpreterAction.getInstance().startPython();
				return;
			}
			store.setValue(EmbeddedPython.PREF_EMBEDDED, true);
		}
		/* Python starts in the current perspective (the Python perspective: Window > Perspective) */
		ConsoleInterpreterAction.getInstance().startPython();
		if (!EmbeddedPython.isRunning()) {
			boolean open = MessageDialog.openQuestion(window.getShell(), "Python",
					"Python could not be started (see the console).\n\nOpen the Python preferences ('Detect Python...')?");
			if (open) {
				Display.getDefault().asyncExec(() -> PreferencesUtil.createPreferenceDialogOn(window.getShell(), "com.eco.bio7.python", null, null).open());
			}
		}
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
		/* If Python runs: the console is switched to Python */
		start.setText(EmbeddedPython.isRunning() ? "Python Console" : "Start Python");
		start.addListener(SWT.Selection, _ -> run());

		MenuItem interrupt = new MenuItem(menu, SWT.PUSH);
		interrupt.setText("Interrupt Python");
		interrupt.setImage(image);
		interrupt.setEnabled(EmbeddedPython.isRunning());
		interrupt.addListener(SWT.Selection, _ -> EmbeddedPython.interrupt());
		return menu;
	}

	@Override
	public Menu getMenu(Menu parent) {
		return null;
	}
}
