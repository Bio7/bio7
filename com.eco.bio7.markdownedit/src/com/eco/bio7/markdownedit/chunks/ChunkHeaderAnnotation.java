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

	/**
	 * The actions of the bar. The icons are drawn as shapes: a symbol of the text
	 * (e.g., ▶) is missing in many fonts and drawString of Windows doesn't take it
	 * from another font.
	 */
	enum BarAction {
		RUN("Run"), RUN_ABOVE("Run Above"), CLEAR("Clear");

		final String label;

		BarAction(String label) {
			this.label = label;
		}
	}

	private static final int PAD = 6;
	private static final int GAP = 6;

	/* Updated in place (a new annotation would leave the line without its space for a moment) */
	private ChunkOutput output;
	private boolean bar;
	/* The buttons of the bar as last drawn (widget coordinates) */
	private final List<Rectangle> buttons = new ArrayList<>();
	/* The area of the header as last drawn (widget coordinates) */
	private Rectangle area;
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

	/** Returns true if the point is in the drawn header (the output or the bar). */
	boolean contains(org.eclipse.swt.graphics.Point p) {
		Rectangle a = area;
		return a != null && a.contains(p);
	}

	/** Shows another output and/or bar (the header stays in the editor). */
	void update(ChunkOutput newOutput, boolean newBar) {
		output = newOutput;
		bar = newBar;
	}

	boolean hasBar() {
		return bar;
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

	/*
	 * The layout of the output: the shown text lines and the scale of the plots.
	 * The header (with the line below it) fits into the visible editor: else the
	 * editor scrolls to the top of the header when the cursor is in that line.
	 */
	private record Layout(int lines, boolean truncated, double scale, int height) {
	}

	private Layout layout() {
		StyledText t = getTextWidget();
		int lh = lineHeight();
		if (output == null) {
			return new Layout(0, false, 1, 0);
		}
		int fixed = lh / 2 + PAD + PAD + barHeight();
		int n = output.lines.size();
		int raw = 0;
		for (int[] s : output.sizes) {
			raw += s[1];
		}
		int gaps = output.sizes.size() * GAP;
		int natural = fixed + n * lh + raw + gaps;
		int client = t.getClientArea().height;
		int avail = Math.max(lh * 6, client - 2 * lh);
		if (client <= 0 || natural <= avail) {
			return new Layout(n, false, 1, natural - barHeight());
		}
		int room = avail - fixed;
		/* The text gets at most a third of the room if there are plots */
		int textRoom = output.images.isEmpty() ? room : Math.min(n * lh, room / 3);
		int shown = n == 0 ? 0 : Math.max(1, Math.min(n, textRoom / lh));
		boolean truncated = shown < n;
		if (truncated && shown > 1) {
			shown--;
		}
		int textHeight = (shown + (truncated ? 1 : 0)) * lh;
		double scale = raw == 0 ? 1 : Math.max(0.15, Math.min(1, (double) (room - textHeight - gaps) / raw));
		int h = fixed - barHeight() + textHeight + gaps;
		for (int[] s : output.sizes) {
			h += (int) Math.round(s[1] * scale);
		}
		return new Layout(shown, truncated, scale, h);
	}

	private int outputHeight() {
		return layout().height();
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
		area = new Rectangle(0, y, textWidget.getClientArea().width, getHeight());
		RGB b = textWidget.getBackground().getRGB();
		RGB f = textWidget.getForeground().getRGB();
		boolean dark = (b.red + b.green + b.blue) < 384;
		Color fg = textWidget.getForeground();
		gc.setFont(textWidget.getFont());
		if (output != null) {
			Layout lay = layout();
			int top = y + lineHeight / 2;
			int height = lay.height() - lineHeight / 2 - 2;
			Color box = blend(b, f, 0.06);
			Color err = dark ? new Color(240, 110, 100) : new Color(190, 30, 30);
			gc.setBackground(box);
			gc.fillRectangle(left, top, width, height);
			gc.setForeground(blend(b, f, 0.18));
			gc.drawLine(left, top, left, top + height);
			int ty = top + PAD;
			if (output.running) {
				String label = "Running...";
				Point ext = textExtent(gc, label);
				gc.setForeground(blend(b, f, 0.55));
				drawText(gc, label, left + width - ext.x - PAD - 4, top + PAD);
			}
			for (int i = 0; i < lay.lines(); i++) {
				String line = output.lines.get(i);
				boolean isError = output.error || line.startsWith("Error") || line.startsWith("Warning");
				/* The confirmation of a chunk without output is dimmed */
				boolean done = line.startsWith("\u2713 Done");
				gc.setForeground(isError ? err : done ? blend(b, f, 0.55) : fg);
				drawText(gc, line, left + PAD + 4, ty);
				ty += lineHeight;
			}
			if (lay.truncated()) {
				gc.setForeground(blend(b, f, 0.55));
				drawText(gc, "... " + (output.lines.size() - lay.lines()) + " more lines (a larger editor shows more)", left + PAD + 4, ty);
				ty += lineHeight;
			}
			for (int i = 0; i < output.images.size(); i++) {
				Image img = output.images.get(i);
				int[] size = output.sizes.get(i);
				int w = (int) Math.round(size[0] * lay.scale());
				int h = (int) Math.round(size[1] * lay.scale());
				if (!img.isDisposed()) {
					if (lay.scale() >= 1) {
						gc.drawImage(img, left + PAD + 4, ty);
					} else {
						/*
						 * Smaller to fit into the editor. The interpolation switches the GC to advanced
						 * graphics (GDI+ on Windows): it is restored for the following drawing.
						 */
						boolean advanced = gc.getAdvanced();
						gc.setInterpolation(org.eclipse.swt.SWT.HIGH);
						gc.drawImage(img, 0, 0, size[0], size[1], left + PAD + 4, ty, w, h);
						gc.setAdvanced(advanced);
					}
				}
				ty += h + GAP;
			}
		}
		buttons.clear();
		if (bar) {
			int by = y + outputHeight() + 1;
			Color link = dark ? new Color(110, 170, 240) : new Color(30, 100, 190);
			int bx = left + 2;
			/* The icon: about the height of the capital letters */
			int icon = Math.max(6, lineHeight * 9 / 20);
			for (BarAction a : BarAction.values()) {
				Point ext = textExtent(gc, a.label);
				int iconWidth = a == BarAction.RUN_ABOVE ? icon * 2 - icon / 4 : icon;
				int contentWidth = iconWidth + 5 + ext.x;
				Rectangle r = new Rectangle(bx - 3, by, contentWidth + 6, lineHeight + 2);
				gc.setBackground(blend(b, f, 0.08));
				gc.fillRoundRectangle(r.x, r.y, r.width, r.height, 6, 6);
				int iy = by + 1 + (lineHeight - icon) / 2;
				drawIcon(gc, a, bx, iy, icon, link);
				gc.setForeground(link);
				drawText(gc, a.label, bx + iconWidth + 5, by + 1);
				buttons.add(r);
				bx += contentWidth + 14;
			}
		}
		gc.setForeground(fg);
	}

	/* The icon of an action as shapes (the same in all fonts and systems) */
	private static void drawIcon(GC gc, BarAction a, int x, int y, int size, Color color) {
		boolean advanced = gc.getAdvanced();
		int antialias = gc.getAntialias();
		gc.setAntialias(org.eclipse.swt.SWT.ON);
		gc.setBackground(color);
		gc.setForeground(color);
		switch (a) {
		case RUN -> gc.fillPolygon(triangle(x, y, size));
		case RUN_ABOVE -> {
			gc.fillPolygon(triangle(x, y, size));
			gc.fillPolygon(triangle(x + size - size / 4, y, size));
		}
		case CLEAR -> {
			int lw = gc.getLineWidth();
			gc.setLineWidth(Math.max(1, size / 5));
			int m = size / 6;
			gc.drawLine(x + m, y + m, x + size - m, y + size - m);
			gc.drawLine(x + size - m, y + m, x + m, y + size - m);
			gc.setLineWidth(lw);
		}
		}
		/* The text of the editor is drawn as before (antialias and advanced graphics restored) */
		gc.setAntialias(antialias);
		gc.setAdvanced(advanced);
	}

	/* A triangle which points to the right */
	private static int[] triangle(int x, int y, int size) {
		int w = size * 7 / 8;
		return new int[] { x, y, x + w, y + size / 2, x, y + size };
	}

	/*
	 * Draws text with a TextLayout: characters missing in the font of the editor
	 * (e.g., ✓, box drawing of R) are taken from another font. drawString doesn't
	 * do this on Windows (GDI) unless the GC uses advanced graphics.
	 */
	private static void drawText(GC gc, String text, int x, int y) {
		org.eclipse.swt.graphics.TextLayout layout = new org.eclipse.swt.graphics.TextLayout(gc.getDevice());
		try {
			layout.setFont(gc.getFont());
			layout.setText(text);
			layout.draw(gc, x, y);
		} finally {
			layout.dispose();
		}
	}

	/* The size of a text as drawn by drawText */
	private static Point textExtent(GC gc, String text) {
		org.eclipse.swt.graphics.TextLayout layout = new org.eclipse.swt.graphics.TextLayout(gc.getDevice());
		try {
			layout.setFont(gc.getFont());
			layout.setText(text);
			Rectangle r = layout.getBounds();
			return new Point(r.width, r.height);
		} finally {
			layout.dispose();
		}
	}

	private static Color blend(RGB b, RGB f, double t) {
		return new Color((int) Math.round(b.red + (f.red - b.red) * t), (int) Math.round(b.green + (f.green - b.green) * t),
				(int) Math.round(b.blue + (f.blue - b.blue) * t));
	}
}
