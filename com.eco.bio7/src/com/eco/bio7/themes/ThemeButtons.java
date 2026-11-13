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
package com.eco.bio7.themes;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * The button style of a custom theme ("buttons.style"): the push buttons of the
 * views (in the workbench windows, not in the dialogs) flat or rounded in the
 * button colors of the theme, or native.
 * <p>
 * SWT.FLAT can only be set when a button is created and the CSS colors only the
 * background (the native bezel stays). The buttons are painted over the native
 * drawing (the paint event comes after it): the background of the parent, the
 * box (lighter when the mouse is over it, darker when pressed), the border, the
 * image and the text (dimmed if disabled).
 */
final class ThemeButtons {

	/* The state of a button (see track) */
	private static final String HOVER = "bio7.themes.buttonHover";
	private static final String PRESSED = "bio7.themes.buttonPressed";

	private static boolean installed;
	/* "native", "flat", "rounded" or "round" */
	private static String style = "native";
	private static Color background;
	private static Color hover;
	private static Color pressed;
	private static Color text;
	private static Color disabledText;
	private static Color border;
	/* The background of the groups (macOS: the group is not colored, its box is painted by GroupColors) */
	private static Color groupBackground;

	private ThemeButtons() {
	}

	/** The style and the colors of the theme (null: the native buttons of the default themes). */
	static void apply(CustomTheme theme) {
		Display display = Display.getDefault();
		String newStyle = theme == null ? "native" : theme.getButtonStyle();
		disposeColors();
		if (!"native".equals(newStyle)) {
			RGB bg = theme.getColor("widgetBackground");
			RGB fg = theme.getColor("widgetText");
			background = new Color(display, bg);
			hover = new Color(display, mix(bg, fg, 0.12));
			pressed = new Color(display, mix(bg, fg, 0.24));
			text = new Color(display, fg);
			disabledText = new Color(display, mix(fg, bg, 0.55));
			border = new Color(display, theme.getColor("groupBorder"));
			groupBackground = new Color(display, theme.getColor("groupBackground"));
			install(display);
		}
		boolean changed = !newStyle.equals(style);
		style = newStyle;
		if (changed || !"native".equals(style)) {
			redrawAll(display);
		}
	}

	private static void install(Display display) {
		if (installed) {
			return;
		}
		installed = true;
		display.addFilter(SWT.Paint, ThemeButtons::paint);
		display.addFilter(SWT.MouseEnter, e -> track(e, HOVER, true));
		display.addFilter(SWT.MouseExit, e -> {
			track(e, HOVER, false);
			track(e, PRESSED, false);
		});
		display.addFilter(SWT.MouseDown, e -> {
			if (e.button == 1) {
				track(e, PRESSED, true);
			}
		});
		display.addFilter(SWT.MouseUp, e -> track(e, PRESSED, false));
	}

	/* A push button of a workbench window (the views, not the dialogs) */
	private static boolean styled(Object widget) {
		if ("native".equals(style) || !(widget instanceof Button button) || button.isDisposed()) {
			return false;
		}
		/* The push buttons and the toggle buttons (e.g., play/pause), not the check boxes, radio and arrow buttons */
		if ((button.getStyle() & (SWT.PUSH | SWT.TOGGLE)) == 0) {
			return false;
		}
		return isWorkbenchShell(button.getShell());
	}

	private static boolean isWorkbenchShell(Shell shell) {
		if (!PlatformUI.isWorkbenchRunning()) {
			return false;
		}
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			if (window.getShell() == shell) {
				return true;
			}
		}
		return false;
	}

	private static void track(Event event, String key, boolean value) {
		if (!styled(event.widget)) {
			return;
		}
		Button button = (Button) event.widget;
		if (Boolean.valueOf(value).equals(button.getData(key))) {
			return;
		}
		button.setData(key, value ? Boolean.TRUE : null);
		button.redraw();
	}

	private static void paint(Event event) {
		if (!styled(event.widget) || background == null) {
			return;
		}
		Button button = (Button) event.widget;
		GC gc = event.gc;
		Point size = button.getSize();
		Rectangle area = new Rectangle(0, 0, size.x, size.y);
		if (area.width <= 2 || area.height <= 2) {
			return;
		}
		int oldAntialias = gc.getAntialias();
		int oldTextAntialias = gc.getTextAntialias();
		gc.setAntialias(SWT.ON);
		gc.setTextAntialias(SWT.ON);
		/*
		 * Over the native bezel (also its margins): the background of the parent. A
		 * button directly in a group: the color of the groups (the group itself has the
		 * color of its parent or the system color, macOS paints the box, see GroupColors)
		 */
		Color parentBackground = button.getParent() instanceof org.eclipse.swt.widgets.Group && groupBackground != null ? groupBackground
				: button.getParent().getBackground();
		gc.setBackground(parentBackground);
		gc.fillRectangle(area);
		/* The box (a small inset like the native buttons, the rows of buttons keep their gaps) */
		Rectangle box = new Rectangle(area.x + 2, area.y + 2, area.width - 4, area.height - 4);
		/* Rounded: small corners. Round: the ends are half circles (the height of the box) */
		int arc = "round".equals(style) ? box.height : "rounded".equals(style) ? Math.min(10, box.height / 2) : 0;
		boolean enabled = button.isEnabled();
		/* A toggle button which is switched on: pressed */
		boolean on = (button.getStyle() & SWT.TOGGLE) != 0 && button.getSelection();
		Color fill = !enabled ? background : button.getData(PRESSED) != null || on ? pressed : button.getData(HOVER) != null ? hover : background;
		gc.setBackground(fill);
		gc.setForeground(border);
		if (arc > 0) {
			gc.fillRoundRectangle(box.x, box.y, box.width, box.height, arc, arc);
			gc.drawRoundRectangle(box.x, box.y, box.width - 1, box.height - 1, arc, arc);
		} else {
			gc.fillRectangle(box);
			gc.drawRectangle(box.x, box.y, box.width - 1, box.height - 1);
		}
		/* The focus: an inner line in the text color */
		if (button.isFocusControl()) {
			gc.setForeground(text);
			gc.setLineStyle(SWT.LINE_DOT);
			gc.drawRectangle(box.x + 3, box.y + 3, box.width - 7, box.height - 7);
			gc.setLineStyle(SWT.LINE_SOLID);
		}
		drawContent(gc, button, box, enabled);
		gc.setAntialias(oldAntialias);
		gc.setTextAntialias(oldTextAntialias);
	}

	/* The image and the text in the center of the box */
	private static void drawContent(GC gc, Button button, Rectangle box, boolean enabled) {
		String label = button.getText() == null ? "" : button.getText().replace("&&", "\u0000").replace("&", "").replace("\u0000", "&");
		Image image = button.getImage();
		gc.setFont(button.getFont());
		Point textSize = label.isEmpty() ? new Point(0, 0) : gc.textExtent(label, SWT.DRAW_TRANSPARENT);
		Rectangle imageBounds = image == null || image.isDisposed() ? null : image.getBounds();
		int gap = imageBounds != null && !label.isEmpty() ? 4 : 0;
		int width = textSize.x + gap + (imageBounds != null ? imageBounds.width : 0);
		int x = box.x + Math.max(2, (box.width - width) / 2);
		if (imageBounds != null) {
			int y = box.y + (box.height - imageBounds.height) / 2;
			if (enabled) {
				gc.drawImage(image, x, y);
			} else {
				Image gray = new Image(gc.getDevice(), image, SWT.IMAGE_DISABLE);
				gc.drawImage(gray, x, y);
				gray.dispose();
			}
			x += imageBounds.width + gap;
		}
		if (!label.isEmpty()) {
			gc.setForeground(enabled ? text : disabledText);
			gc.drawText(label, x, box.y + (box.height - textSize.y) / 2, SWT.DRAW_TRANSPARENT);
		}
	}

	private static void redrawAll(Display display) {
		for (Shell shell : display.getShells()) {
			if (isWorkbenchShell(shell)) {
				redraw(shell);
			}
		}
	}

	private static void redraw(Control control) {
		if (control.isDisposed()) {
			return;
		}
		if (control instanceof Button) {
			control.redraw();
		}
		if (control instanceof org.eclipse.swt.widgets.Composite composite) {
			for (Control child : composite.getChildren()) {
				redraw(child);
			}
		}
	}

	private static RGB mix(RGB a, RGB b, double f) {
		return new RGB((int) Math.round(a.red * (1 - f) + b.red * f), (int) Math.round(a.green * (1 - f) + b.green * f),
				(int) Math.round(a.blue * (1 - f) + b.blue * f));
	}

	private static void disposeColors() {
		for (Color c : new Color[] { background, hover, pressed, text, disabledText, border, groupBackground }) {
			if (c != null && !c.isDisposed()) {
				c.dispose();
			}
		}
		background = hover = pressed = text = disabledText = border = groupBackground = null;
	}
}
