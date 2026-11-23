/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/

package com.eco.bio7.util;

import java.lang.reflect.Field;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * A ColorFieldEditor whose button opens the color dialog of Bio7
 * ({@link ThemedColorDialog}): a drop-in replacement for the preference pages.
 */
public class ThemedColorFieldEditor extends ColorFieldEditor {

	public ThemedColorFieldEditor(String name, String labelText, Composite parent) {
		super(name, labelText, parent);
	}

	/*
	 * The editor keeps its ColorSelector in a private field and creates it here:
	 * the field gets a ThemedColorSelector first (with the listener the editor
	 * would add), then the editor uses it. Another version of JFace: the normal
	 * selector (the native dialog).
	 */
	@Override
	protected Button getChangeControl(Composite parent) {
		try {
			Field field = ColorFieldEditor.class.getDeclaredField("colorSelector");
			field.setAccessible(true);
			if (field.get(this) == null) {
				ColorSelector selector = new ThemedColorSelector(parent);
				selector.addListener(event -> {
					fireValueChanged(event.getProperty(), event.getOldValue(), event.getNewValue());
					setPresentsDefaultValue(false);
				});
				field.set(this, selector);
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			/* The normal ColorSelector */
		}
		return super.getChangeControl(parent);
	}
}
