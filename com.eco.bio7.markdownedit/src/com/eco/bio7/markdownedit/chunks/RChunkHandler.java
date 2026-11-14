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
package com.eco.bio7.markdownedit.chunks;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.eco.bio7.markdownedit.editors.MarkdownEditor;

/**
 * The actions of the inline output of R chunks in the markdown editor: run the
 * chunk at the cursor, run all chunks, clear the output (the parameter of the
 * handler in plugin.xml).
 */
public class RChunkHandler extends AbstractHandler {

	public static final String RUN = "com.eco.bio7.markdownedit.chunks.run";
	public static final String RUN_ALL = "com.eco.bio7.markdownedit.chunks.runAll";
	public static final String CLEAR = "com.eco.bio7.markdownedit.chunks.clear";

	@Override
	public Object execute(ExecutionEvent event) {
		IEditorPart part = HandlerUtil.getActiveEditor(event);
		if (!(part instanceof MarkdownEditor editor) || editor.getViewer() == null) {
			return null;
		}
		InlineChunkOutputs outputs = InlineChunkOutputs.of(editor, editor.getViewer());
		switch (event.getCommand().getId()) {
		case RUN -> outputs.runChunkAtCursor((ITextSelection) editor.getSelectionProvider().getSelection());
		case RUN_ALL -> outputs.runAll();
		case CLEAR -> outputs.clear();
		default -> {
		}
		}
		return null;
	}
}
