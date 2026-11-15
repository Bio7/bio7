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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.console.ConsoleInterpreterAction;

/**
 * The action of the main toolbar (beside "Start R"): starts the embedded
 * Python (the Python of the preferences) and opens the Python console. If
 * Python runs, the console is switched to Python.
 */
public class StartPython extends Action {

	private final IWorkbenchWindow window;

	public StartPython(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;
		setId("com.eco.bio7.start_python");
		setToolTipText("Start Python (embedded in Bio7: data transfer with ImageJ and R, matplotlib plots)");
		setImageDescriptor(Bio7Plugin.getImageDescriptor("/icons/views/pythonaction.svg"));
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
		ConsoleInterpreterAction.getInstance().startPython();
		if (EmbeddedPython.isRunning()) {
			/* The Python perspective (Python-Shell, packages, console, table) */
			try {
				window.getWorkbench().showPerspective(PythonPerspective.ID, window);
			} catch (org.eclipse.ui.WorkbenchException e) {
				PythonShellView.show();
			}
		}
		if (!EmbeddedPython.isRunning()) {
			boolean open = MessageDialog.openQuestion(window.getShell(), "Python",
					"Python could not be started (see the console).\n\nOpen the Python preferences ('Detect Python...')?");
			if (open) {
				Display.getDefault().asyncExec(() -> PreferencesUtil.createPreferenceDialogOn(window.getShell(), "com.eco.bio7.python", null, null).open());
			}
		}
	}
}
