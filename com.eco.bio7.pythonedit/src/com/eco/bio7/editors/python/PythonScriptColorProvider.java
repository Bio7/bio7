/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.editors.python;

import java.util.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import com.eco.bio7.pythonedit.PythonEditorPlugin;

/**
 * Manager for colors used in the Java editor
 */
public class PythonScriptColorProvider {
	IPreferenceStore store = PythonEditorPlugin.getDefault()
			.getPreferenceStore();

	public static final RGB MULTI_LINE_COMMENT = new RGB(63,127,95);

	protected Map fColorTable = new HashMap(10);

	/**
	 * Release all of the color resources held onto by the receiver.
	 */
	public void dispose() {
		Iterator e = fColorTable.values().iterator();
		while (e.hasNext())
			((Color) e.next()).dispose();
	}

	/**
	 * Return the color that is stored in the color table under the given RGB
	 * value.
	 * 
	 * @param rgb
	 *            the RGB value
	 * @return the color stored in the color table for the given RGB value
	 */
	/*Slightly changed. We get no colour from the table
	 * Colour table is not in use inside Bio7!*/
	public Color getColor(RGB rgb) {
		Color color = (Color) fColorTable.get(rgb);
		if (color == null) {
			color = new Color(Display.getCurrent(), rgb);
			fColorTable.put(rgb, color);
		}
		return color;
	}
}
