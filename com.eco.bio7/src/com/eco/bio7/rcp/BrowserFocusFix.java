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
package com.eco.bio7.rcp;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * The embedded browser (WebKit, Edge) keeps the keyboard focus: a click into
 * an editor or another view afterwards did not activate it (the actions of the
 * editor stayed disabled until another view was clicked). A click outside of a
 * browser while a browser has the focus (or the browser view is active) gives
 * the focus to the clicked control and activates a clicked editor.
 */
public final class BrowserFocusFix {

	private static boolean installed;

	private BrowserFocusFix() {
	}

	/** Installs the click filter (UI thread, once). */
	public static void install(Display display) {
		if (installed || display == null) {
			return;
		}
		installed = true;
		display.addFilter(SWT.MouseDown, com.eco.bio7.util.SafeEvents.guard("BrowserFocusFix", e -> {
			if (!(e.widget instanceof Control clicked) || inBrowser(clicked)) {
				return;
			}
			IWorkbenchPage page = page();
			IWorkbenchPart active = page == null ? null : page.getActivePart();
			boolean browserActive = active != null && active.getSite() != null && "com.eco.bio7.browser.Browser".equals(active.getSite().getId());
			Control focus = display.getFocusControl();
			if (!browserActive && !(focus != null && inBrowser(focus))) {
				return;
			}
			display.asyncExec(() -> {
				if (clicked.isDisposed()) {
					return;
				}
				if (display.getFocusControl() != clicked && !clicked.setFocus()) {
					clicked.forceFocus();
				}
				/* An editor which contains the clicked control: the active part (its actions are enabled) */
				IWorkbenchPage p = page();
				if (p == null) {
					return;
				}
				for (IEditorReference ref : p.getEditorReferences()) {
					IEditorPart editor = ref.getEditor(false);
					Control control = editor == null ? null : editor.getAdapter(Control.class);
					if (control == null && editor != null && editor.getAdapter(org.eclipse.jface.text.ITextOperationTarget.class) instanceof org.eclipse.jface.text.ITextViewer viewer) {
						/* Text editors (R, Python, ...): the text widget of the viewer */
						control = viewer.getTextWidget();
					}
					if (control != null && contains(control, clicked)) {
						if (p.getActivePart() != editor) {
							p.activate(editor);
						}
						return;
					}
				}
			});
		}));
	}

	private static boolean inBrowser(Control c) {
		for (Control x = c; x != null; x = x.getParent()) {
			if (x instanceof Browser) {
				return true;
			}
		}
		return false;
	}

	private static boolean contains(Control ancestor, Control c) {
		for (Control x = c; x != null; x = x.getParent()) {
			if (x == ancestor) {
				return true;
			}
		}
		return false;
	}

	private static IWorkbenchPage page() {
		IWorkbenchWindow w = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		return w == null ? null : w.getActivePage();
	}
}
