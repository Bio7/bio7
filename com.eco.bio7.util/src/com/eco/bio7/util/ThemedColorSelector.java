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

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;

/**
 * A ColorSelector (the color button of JFace) which opens the color dialog of
 * Bio7 ({@link ThemedColorDialog}: styled by the theme, or the native dialog
 * if selected in the preferences).
 */
public class ThemedColorSelector extends ColorSelector {

	public ThemedColorSelector(Composite parent) {
		super(parent);
	}

	@Override
	public void open() {
		if (!ThemedColorDialog.isEnabled()) {
			super.open();
			return;
		}
		ThemedColorDialog dialog = new ThemedColorDialog(getButton().getShell());
		RGB old = getColorValue();
		dialog.setRGB(old);
		RGB chosen = dialog.open();
		if (chosen == null) {
			return;
		}
		setColorValue(chosen);
		/* Like ColorSelector.open: the listeners get the change */
		PropertyChangeEvent event = new PropertyChangeEvent(this, PROP_COLORCHANGE, old, chosen);
		for (Object listener : getListeners()) {
			((IPropertyChangeListener) listener).propertyChange(event);
		}
	}
}
