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
package com.eco.bio7.image.ui;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Tests if an editor has markers which tools placed with the
 * {@link ij.swt.CodeEditor} API (property "com.eco.bio7.image.hasCodeMarkers").
 *
 * @author M. Austenfeld
 */
public class CodeMarkersPropertyTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (!"hasCodeMarkers".equals(property)) {
			return false;
		}
		ITextEditor editor = RemoveCodeMarkersHandler.textEditor(receiver);
		return editor != null && WorkbenchCodeMarkers.hasMarkers(editor);
	}
}
