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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Removes the markers which tools placed with the {@link ij.swt.CodeEditor} API
 * from the active editor (see {@link WorkbenchCodeMarkers}). The markers of the
 * editor itself are not removed.
 *
 * @author M. Austenfeld
 */
public class RemoveCodeMarkersHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) {
		ITextEditor editor = textEditor(HandlerUtil.getActiveEditor(event));
		WorkbenchCodeMarkers markers = editor == null ? null : WorkbenchCodeMarkers.of(editor, false);
		if (markers != null) {
			markers.clear();
		}
		return null;
	}

	static ITextEditor textEditor(Object part) {
		if (part instanceof ITextEditor) {
			return (ITextEditor) part;
		}
		return part instanceof IEditorPart ? ((IEditorPart) part).getAdapter(ITextEditor.class) : null;
	}
}
