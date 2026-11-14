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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.inlined.LineHeaderAnnotation;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Drawn above a line of the markdown editor (the document is not changed): the
 * output of the chunk which ends above the line and/or the action bar of the
 * chunk which starts at the line (Run, Run Above, Clear).
 */
final class ChunkHeaderAnnotation extends LineHeaderAnnotation {

	/** The actions of the bar. */
	enum BarAction {
		RUN("▶ Run"), RUN_ABOVE("▶▶ Run Above"), CLEAR("✕ Clear");

		final String label;

		BarAction(String label) {
			this.label = label;
		}
	}

	private static final int PAD = 6;
	private static final int GAP = 6;

	private final ChunkOutput output;
	private final boolean bar;
	/* The buttons of the bar as last drawn (widget coordinates) */
	private final List<Rectangle> buttons = new ArrayList<>();
	private final Consumer<MouseEvent> hover;
	private final Consumer<MouseEvent> click;
	private final Consumer<MouseEvent> out;

	ChunkHeaderAnnotation(Position position, ISourceViewer viewer, ChunkOutput output, boolean bar, Consumer<MouseEvent> onClick,
			Consumer<MouseEvent> onHover, Consumer<MouseEvent> onOut) {
		super(position, viewer);
		this.hover = onHover;
		this.out = onOut;
		this.click = onClick;
		this.output = output;
		this.bar = bar;
	}

	/* The cursor follows the buttons: a hand only over a button (no redraw) */
	@Override
	public void onMouseHover(MouseEvent e) {
		hover.accept(e);
	}

	@Override
	public void onMouseMove(MouseEvent e) {
		hover.accept(e);
	}

	@Override
	public void onMouseOut(MouseEvent e) {
		out.accept(e);
	}

	/* A click on a button of the bar (the support calls the action on mouse up) */
	@Override
	public Consumer<MouseEvent> getAction(MouseEvent e) {
		return buttonAt(e.x, e.y) != null ? click : null;
	}

	ChunkOutput output() {
		return output;
	}

	/** The button at a point of the widget or null. */
	BarAction buttonAt(int x, int y) {
		for (int i = 0; i < buttons.size(); i++) {
			if (buttons.get(i).contains(x, y)) {
				return BarAction.values()[i];
			}
		}
		return null;
	}

	private int lineHeight() {
		return getTextWidget().getLineHeight();
	}

	private int outputHeight() {
		if (output == null) {
			return 0;
		}
		int h = lineHeight() / 2 + PAD + output.lines.size() * lineHeight();
		for (int[] s : output.sizes) {
			h += s[1] + GAP;
		}
		return h + PAD;
	}

	private int barHeight() {
		return bar ? lineHeight() + 4 : 0;
	}

	@Override
	public int getHeight() {
		return Math.max(1, outputHeight() + barHeight());
	}

	@Override
	public void draw(GC gc, StyledText textWidget, int widgetOffset, int length, Color color, int x, int y) {
		int lineHeight = textWidget.getLineHeight();
		int left = textWidget.getLeftMargin() - textWidget.getHorizontalPixel();
		int width = Math.max(200, textWidget.getClientArea().width - left - 8);
		RGB b = textWidget.getBackground().getRGB();
		RGB f = textWidget.getForeground().getRGB();
		boolean dark = (b.red + b.green + b.blue) < 384;
		Color fg = textWidget.getForeground();
		gc.setFont(textWidget.getFont());
		if (output != null) {
			int top = y + lineHeight / 2;
			int height = outputHeight() - lineHeight / 2 - 2;
			Color box = blend(b, f, 0.06);
			Color err = dark ? new Color(240, 110, 100) : new Color(190, 30, 30);
			gc.setBackground(box);
			gc.fillRectangle(left, top, width, height);
			gc.setForeground(blend(b, f, 0.18));
			gc.drawLine(left, top, left, top + height);
			int ty = top + PAD;
			for (String line : output.lines) {
				boolean isError = output.error || line.startsWith("Error") || line.startsWith("Warning");
				gc.setForeground(isError ? err : fg);
				gc.drawString(line, left + PAD + 4, ty, true);
				ty += lineHeight;
			}
			for (int i = 0; i < output.images.size(); i++) {
				Image img = output.images.get(i);
				if (!img.isDisposed()) {
					gc.drawImage(img, left + PAD + 4, ty);
				}
				ty += output.sizes.get(i)[1] + GAP;
			}
		}
		buttons.clear();
		if (bar) {
			int by = y + outputHeight() + 1;
			Color link = dark ? new Color(110, 170, 240) : new Color(30, 100, 190);
			int bx = left + 2;
			for (BarAction a : BarAction.values()) {
				Point ext = gc.textExtent(a.label);
				Rectangle r = new Rectangle(bx - 3, by, ext.x + 6, lineHeight + 2);
				gc.setBackground(blend(b, f, 0.08));
				gc.fillRoundRectangle(r.x, r.y, r.width, r.height, 6, 6);
				gc.setForeground(link);
				gc.drawString(a.label, bx, by + 1, true);
				buttons.add(r);
				bx += ext.x + 14;
			}
		}
		gc.setForeground(fg);
	}

	private static Color blend(RGB b, RGB f, double t) {
		return new Color((int) Math.round(b.red + (f.red - b.red) * t), (int) Math.round(b.green + (f.green - b.green) * t),
				(int) Math.round(b.blue + (f.blue - b.blue) * t));
	}
}
