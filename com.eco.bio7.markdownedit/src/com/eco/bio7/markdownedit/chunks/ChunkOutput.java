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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.Position;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.widgets.Display;

/**
 * The output of a chunk (text and plots) and its chunk: the position of the
 * closing fence (updated with the document); the output is drawn above the
 * next line.
 */
final class ChunkOutput {

	static final int MAX_LINES = 60;

	/* The closing fence of the chunk */
	final Position fence;
	final List<String> lines = new ArrayList<>();
	final boolean error;
	/* A new run of the chunk: the output is kept until its result is shown */
	boolean running;
	final List<Image> images = new ArrayList<>();
	final List<int[]> sizes = new ArrayList<>();

	ChunkOutput(Position fence, Display display, String text, List<Path> plots, boolean error) {
		this.fence = fence;
		this.error = error;
		String t = text == null ? "" : text.replace("\r", "").replace("\t", "    ");
		while (t.endsWith("\n")) {
			t = t.substring(0, t.length() - 1);
		}
		if (!t.isEmpty()) {
			String[] all = t.split("\n", -1);
			for (int i = 0; i < all.length && i < MAX_LINES; i++) {
				lines.add(all[i]);
			}
			if (all.length > MAX_LINES) {
				lines.add("... " + (all.length - MAX_LINES) + " more lines");
			}
		}
		for (Path p : plots) {
			try {
				ImageData full = new ImageData(p.toString());
				int w = Math.max(1, full.width / 2);
				int h = Math.max(1, full.height / 2);
				/* Rendered at twice the size: sharp on HiDPI screens */
				ImageDataProvider provider = zoom -> zoom >= 200 ? full : full.scaledTo(Math.max(1, w * zoom / 100), Math.max(1, h * zoom / 100));
				images.add(new Image(display, provider));
				sizes.add(new int[] { w, h });
			} catch (RuntimeException e) {
				lines.add("(The plot could not be shown: " + e.getMessage() + ")");
			}
		}
		if (lines.isEmpty() && images.isEmpty()) {
			lines.add("(no output)");
		}
	}

	void dispose() {
		for (Image img : images) {
			if (!img.isDisposed()) {
				img.dispose();
			}
		}
		images.clear();
	}
}
