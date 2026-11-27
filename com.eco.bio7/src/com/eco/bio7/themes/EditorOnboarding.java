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
package com.eco.bio7.themes;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

/**
 * The onboarding controls of the empty editor area: Eclipse creates a container
 * in the middle of an empty editor stack for an image and a text of the
 * perspective (empty in Bio7). Its color is set once in the code, the
 * background of the editor folder when it is created (then still the color of
 * the views: a small rectangle in the empty editor area of the custom themes).
 * Here the controls get the current background of their editor folder: when
 * they are resized (Eclipse sizes them with the editor area), shown or painted,
 * after the CSS engine styled them again (a perspective switch: SWT.Skin) and
 * when a theme is activated.
 */
final class EditorOnboarding {

	/* The widget data of the onboarding controls (StackRenderer: EditorStack.OnboardingContainer, ...Composite, ...Image, ...Text) */
	private static final String ONBOARDING = "EditorStack.Onboarding";

	private static boolean installed;

	private EditorOnboarding() {
	}

	/* The onboarding controls get the color of their folder when they are resized or shown */
	static void install(Display display) {
		if (installed || display == null || display.isDisposed()) {
			return;
		}
		installed = true;
		display.addFilter(SWT.Resize, com.eco.bio7.util.SafeEvents.guard("EditorOnboarding", EditorOnboarding::handle));
		display.addFilter(SWT.Show, com.eco.bio7.util.SafeEvents.guard("EditorOnboarding", EditorOnboarding::handle));
		/* Before they are painted with another color (e.g., after a perspective switch) */
		display.addFilter(SWT.Paint, com.eco.bio7.util.SafeEvents.guard("EditorOnboarding", EditorOnboarding::handle));
		/* The CSS engine styles in a listener (after this filter): afterwards */
		display.addFilter(SWT.Skin, com.eco.bio7.util.SafeEvents.guard("EditorOnboarding", e -> {
			if (e.widget instanceof Control c && (isOnboarding(c) || c instanceof CTabFolder)) {
				e.display.asyncExec(() -> {
					if (c.isDisposed()) {
						return;
					}
					if (isOnboarding(c)) {
						color(c);
					} else {
						/* The onboarding container is a child of the editor folder */
						for (Control child : ((CTabFolder) c).getChildren()) {
							if (isOnboarding(child)) {
								color(child);
							}
						}
					}
				});
			}
		}));
		/* The existing ones (created before this filter) */
		apply(display);
	}

	private static void handle(Event e) {
		if (e.widget instanceof Control c && isOnboarding(c)) {
			color(c);
		}
	}

	/* The existing onboarding controls with the color of their folder (e.g., after a theme was activated) */
	static void apply(Display display) {
		if (display == null || display.isDisposed()) {
			return;
		}
		/* After the styling of the theme (the folder has its new color then) */
		display.asyncExec(() -> {
			for (Shell shell : display.getShells()) {
				walk(shell);
			}
		});
	}

	private static void walk(Control control) {
		if (control.isDisposed()) {
			return;
		}
		if (isOnboarding(control)) {
			color(control);
			return;
		}
		if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				walk(child);
			}
		}
	}

	private static boolean isOnboarding(Control control) {
		return !control.isDisposed() && control.getData("id") instanceof String id && id.startsWith(ONBOARDING);
	}

	/* The control and its children with the background of the editor folder */
	private static void color(Control control) {
		Control parent = control.getParent();
		while (parent != null && !(parent instanceof CTabFolder)) {
			parent = parent.getParent();
		}
		if (parent == null) {
			return;
		}
		set(control, parent.getBackground());
	}

	private static void set(Control control, Color background) {
		if (control.isDisposed()) {
			return;
		}
		if (!background.equals(control.getBackground())) {
			control.setBackground(background);
		}
		if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				set(child, background);
			}
		}
	}
}
