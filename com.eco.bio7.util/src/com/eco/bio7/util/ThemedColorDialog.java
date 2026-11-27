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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

/**
 * The color dialog of Bio7: a drop-in replacement of the SWT ColorDialog (the
 * same methods). By default it is the Bio7 color dialog (SWT widgets, styled by
 * the theme, e.g., dark in a dark theme on every OS): a color field with a hue
 * bar, the new and the current color, hex, RGB and HSV values, basic colors,
 * recent colors (kept) and a color picker for the screen. The native dialog of
 * the OS is used if it is selected in the preferences (see {@link #isEnabled()}).
 */
public class ThemedColorDialog {

	/** The preference (node com.eco.bio7): true (default) for the Bio7 color dialog, false for the native dialog. */
	public static final String PREFERENCE = ThemedDialogs.COLOR;
	private static final String RECENT = "BIO7_COLOR_DIALOG_RECENT";
	private static final int RECENT_COUNT = 12;
	private static final String[] BASIC = { "000000", "404040", "808080", "bfbfbf", "ffffff", "c92a2a", "e8590c", "f08c00", "fab005", "82c91e", "2b8a3e",
			"0b7285", "1971c2", "364fc7", "5f3dc4", "862e9c", "c2255c", "ffc9c9", "ffd8a8", "fff3bf", "d3f9d8", "c5f6fa", "d0ebff", "e5dbff" };
	private static final int SV_W = 256, SV_H = 200, HUE_W = 20, CELL = 24;

	private final Shell parent;
	private final int style;
	private String title = "Color";
	private RGB rgb;
	private RGB[] rgbs;

	/* The state of the open dialog */
	private Shell shell;
	private float hue, sat, val;
	private RGB current;
	private RGB result;
	private boolean updating;
	private Canvas svCanvas, hueCanvas, preview, recentCanvas;
	private Image svImage, hueImage;
	private float svImageHue = -1;
	private Text hexText, hsvText;
	private Spinner red, green, blue;
	private final List<RGB> recent = new ArrayList<>();

	public ThemedColorDialog(Shell parent) {
		this(parent, SWT.APPLICATION_MODAL);
	}

	public ThemedColorDialog(Shell parent, int style) {
		this.parent = parent;
		this.style = style;
	}

	/** True if the Bio7 color dialog is used (preference, default true), else the native dialog. */
	public static boolean isEnabled() {
		return ThemedDialogs.isEnabled(PREFERENCE);
	}

	public Shell getParent() {
		return parent;
	}

	public int getStyle() {
		return style;
	}

	public String getText() {
		return title;
	}

	public void setText(String text) {
		this.title = text == null ? "" : text;
	}

	public RGB getRGB() {
		return rgb;
	}

	public void setRGB(RGB rgb) {
		this.rgb = rgb;
	}

	/** The custom colors (the recent colors of the Bio7 dialog). */
	public RGB[] getRGBs() {
		return rgbs;
	}

	public void setRGBs(RGB[] rgbs) {
		this.rgbs = rgbs;
	}

	/** Opens the dialog: the chosen color or null if canceled (like ColorDialog.open). */
	public RGB open() {
		if (!isEnabled()) {
			ColorDialog dialog = new ColorDialog(parent, style);
			dialog.setText(title);
			if (rgb != null) {
				dialog.setRGB(rgb);
			}
			if (rgbs != null) {
				dialog.setRGBs(rgbs);
			}
			RGB chosen = dialog.open();
			if (chosen != null) {
				rgb = chosen;
			}
			rgbs = dialog.getRGBs();
			return chosen;
		}
		return openBio7();
	}

	/* ######################## The Bio7 dialog ######################## */

	private RGB openBio7() {
		Display display = parent != null ? parent.getDisplay() : Display.getDefault();
		current = rgb != null ? rgb : new RGB(47, 143, 214);
		loadRecent();
		setFromRgb(current);
		result = null;
		/* Modal like the native dialog (the dialog blocks until it is closed) */
		shell = parent != null && !parent.isDisposed() ? new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL)
				: new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		shell.setText(title);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 12;
		layout.marginHeight = 12;
		layout.horizontalSpacing = 16;
		shell.setLayout(layout);
		createLeft(shell);
		createRight(shell);
		createButtons(shell);
		shell.addListener(SWT.Dispose, _ -> disposeImages());
		updateAll(null);
		shell.pack();
		center(shell);
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		if (result != null) {
			rgb = result;
			addRecent(result);
			rgbs = recent.toArray(new RGB[0]);
		}
		return result;
	}

	private void createLeft(Composite parentComposite) {
		Composite left = new Composite(parentComposite, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.horizontalSpacing = 10;
		left.setLayout(gl);
		left.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

		svCanvas = new Canvas(left, SWT.DOUBLE_BUFFERED);
		svCanvas.setLayoutData(size(SV_W + 2, SV_H + 2));
		svCanvas.addListener(SWT.Paint, com.eco.bio7.util.SafeEvents.guard("ThemedColorDialog painting", this::paintSv));
		Listener svMouse = e -> {
			if (e.type == SWT.MouseDown || (e.type == SWT.MouseMove && (e.stateMask & SWT.BUTTON1) != 0)) {
				sat = clamp((e.x - 1) / (float) SV_W);
				val = 1 - clamp((e.y - 1) / (float) SV_H);
				updateAll(null);
			}
		};
		svCanvas.addListener(SWT.MouseDown, svMouse);
		svCanvas.addListener(SWT.MouseMove, svMouse);
		svCanvas.setCursor(svCanvas.getDisplay().getSystemCursor(SWT.CURSOR_CROSS));

		hueCanvas = new Canvas(left, SWT.DOUBLE_BUFFERED);
		hueCanvas.setLayoutData(size(HUE_W + 2, SV_H + 2));
		hueCanvas.addListener(SWT.Paint, com.eco.bio7.util.SafeEvents.guard("ThemedColorDialog painting", this::paintHue));
		Listener hueMouse = e -> {
			if (e.type == SWT.MouseDown || (e.type == SWT.MouseMove && (e.stateMask & SWT.BUTTON1) != 0)) {
				hue = Math.min(359.9f, clamp((e.y - 1) / (float) SV_H) * 360f);
				updateAll(null);
			}
		};
		hueCanvas.addListener(SWT.MouseDown, hueMouse);
		hueCanvas.addListener(SWT.MouseMove, hueMouse);
		hueCanvas.setCursor(hueCanvas.getDisplay().getSystemCursor(SWT.CURSOR_HAND));

		label(left, "Basic colors", 2);
		List<RGB> basic = new ArrayList<>();
		for (String h : BASIC) {
			basic.add(fromHex(h));
		}
		swatches(left, basic, 12);
		label(left, "Recent colors", 2);
		recentCanvas = swatches(left, recent, RECENT_COUNT);
	}

	private void createRight(Composite parentComposite) {
		Composite right = new Composite(parentComposite, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.verticalSpacing = 8;
		right.setLayout(gl);
		right.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		label(right, "New / Current", 2);
		preview = new Canvas(right, SWT.DOUBLE_BUFFERED);
		GridData pd = size(170, 56);
		pd.horizontalSpan = 2;
		preview.setLayoutData(pd);
		preview.setToolTipText("Left: the new color, right: the current color (click to restore it)");
		preview.addListener(SWT.Paint, com.eco.bio7.util.SafeEvents.guard("ThemedColorDialog painting", this::paintPreview));
		preview.addListener(SWT.MouseDown, e -> {
			if (e.x > preview.getSize().x / 2) {
				setFromRgb(current);
				updateAll(null);
			}
		});

		label(right, "Hex", 1);
		hexText = new Text(right, SWT.BORDER);
		hexText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		hexText.setTextLimit(7);
		hexText.addListener(SWT.Modify, _ -> {
			if (updating) {
				return;
			}
			RGB c = parseHex(hexText.getText());
			if (c != null) {
				setFromRgb(c);
				updateAll(hexText);
			}
		});
		red = spinner(right, "Red");
		green = spinner(right, "Green");
		blue = spinner(right, "Blue");
		label(right, "HSV", 1);
		hsvText = new Text(right, SWT.BORDER | SWT.READ_ONLY);
		hsvText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		if (canPickFromScreen()) {
			Button pick = new Button(right, SWT.PUSH);
			pick.setText("Pick from Screen");
			pick.setToolTipText("Picks a color from the screen (Esc cancels)");
			GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gd.horizontalSpan = 2;
			pick.setLayoutData(gd);
			pick.addListener(SWT.Selection, _ -> pickFromScreen());
		}
	}

	private void createButtons(Composite parentComposite) {
		Composite buttons = new Composite(parentComposite, SWT.NONE);
		GridLayout gl = new GridLayout(2, true);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		buttons.setLayout(gl);
		GridData gd = new GridData(SWT.END, SWT.CENTER, true, false);
		gd.horizontalSpan = 2;
		gd.verticalIndent = 8;
		buttons.setLayoutData(gd);
		/* The order of the platform (OK left on Windows and Linux, right on macOS) */
		boolean okFirst = !"cocoa".equals(SWT.getPlatform());
		Button first = new Button(buttons, SWT.PUSH);
		Button second = new Button(buttons, SWT.PUSH);
		Button ok = okFirst ? first : second;
		Button cancel = okFirst ? second : first;
		ok.setText("OK");
		cancel.setText("Cancel");
		for (Button b : new Button[] { ok, cancel }) {
			GridData bd = new GridData(SWT.FILL, SWT.CENTER, true, false);
			bd.widthHint = Math.max(88, b.computeSize(SWT.DEFAULT, SWT.DEFAULT).x + 16);
			b.setLayoutData(bd);
		}
		ok.addListener(SWT.Selection, _ -> {
			result = rgbNow();
			shell.close();
		});
		cancel.addListener(SWT.Selection, _ -> shell.close());
		shell.setDefaultButton(ok);
	}

	/* ######################## Painting ######################## */

	private void paintSv(Event e) {
		if (svImage == null || svImage.isDisposed() || svImageHue != hue) {
			if (svImage != null && !svImage.isDisposed()) {
				svImage.dispose();
			}
			ImageData data = new ImageData(SV_W, SV_H, 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
			for (int y = 0; y < SV_H; y++) {
				float v = 1 - y / (float) (SV_H - 1);
				for (int x = 0; x < SV_W; x++) {
					RGB c = hsvToRgb(hue, x / (float) (SV_W - 1), v);
					data.setPixel(x, y, (c.red << 16) | (c.green << 8) | c.blue);
				}
			}
			svImage = new Image(svCanvas.getDisplay(), data);
			svImageHue = hue;
		}
		GC gc = e.gc;
		gc.drawImage(svImage, 1, 1);
		border(gc, svCanvas);
		int mx = 1 + Math.round(sat * (SV_W - 1));
		int my = 1 + Math.round((1 - val) * (SV_H - 1));
		gc.setAntialias(SWT.ON);
		gc.setLineWidth(1);
		gc.setForeground(svCanvas.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		gc.drawOval(mx - 7, my - 7, 14, 14);
		gc.setLineWidth(2);
		gc.setForeground(svCanvas.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		gc.drawOval(mx - 6, my - 6, 12, 12);
	}

	private void paintHue(Event e) {
		if (hueImage == null || hueImage.isDisposed()) {
			ImageData data = new ImageData(HUE_W, SV_H, 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
			for (int y = 0; y < SV_H; y++) {
				RGB c = hsvToRgb(y / (float) SV_H * 360f, 1, 1);
				for (int x = 0; x < HUE_W; x++) {
					data.setPixel(x, y, (c.red << 16) | (c.green << 8) | c.blue);
				}
			}
			hueImage = new Image(hueCanvas.getDisplay(), data);
		}
		GC gc = e.gc;
		gc.drawImage(hueImage, 1, 1);
		border(gc, hueCanvas);
		int y = 1 + Math.round(hue / 360f * SV_H);
		gc.setForeground(hueCanvas.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		gc.drawRectangle(0, y - 3, HUE_W + 1, 5);
		gc.setForeground(hueCanvas.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		gc.drawRectangle(1, y - 2, HUE_W - 1, 3);
	}

	private void paintPreview(Event e) {
		GC gc = e.gc;
		Point s = preview.getSize();
		Color now = new Color(preview.getDisplay(), rgbNow());
		Color cur = new Color(preview.getDisplay(), current);
		gc.setBackground(now);
		gc.fillRectangle(1, 1, s.x / 2 - 1, s.y - 2);
		gc.setBackground(cur);
		gc.fillRectangle(s.x / 2, 1, s.x - s.x / 2 - 1, s.y - 2);
		border(gc, preview);
	}

	private Canvas swatches(Composite parentComposite, List<RGB> colors, int columns) {
		Canvas canvas = new Canvas(parentComposite, SWT.DOUBLE_BUFFERED);
		int rows = colors == recent ? 1 : (colors.size() + columns - 1) / columns;
		GridData gd = size(columns * CELL, rows * CELL);
		gd.horizontalSpan = 2;
		canvas.setLayoutData(gd);
		canvas.addListener(SWT.Paint, com.eco.bio7.util.SafeEvents.guard("ThemedColorDialog painting", e -> {
			for (int i = 0; i < rows * columns; i++) {
				int x = (i % columns) * CELL, y = (i / columns) * CELL;
				Rectangle r = new Rectangle(x + 1, y + 1, CELL - 4, CELL - 4);
				if (i < colors.size()) {
					Color c = new Color(canvas.getDisplay(), colors.get(i));
					e.gc.setBackground(c);
					e.gc.fillRectangle(r);
				}
				e.gc.setForeground(borderColor(canvas));
				e.gc.drawRectangle(r);
			}
		}));
		canvas.addListener(SWT.MouseDown, e -> {
			int i = (e.y / CELL) * columns + e.x / CELL;
			if (i >= 0 && i < colors.size()) {
				setFromRgb(colors.get(i));
				updateAll(null);
			}
		});
		canvas.addListener(SWT.MouseDoubleClick, e -> {
			int i = (e.y / CELL) * columns + e.x / CELL;
			if (i >= 0 && i < colors.size()) {
				result = colors.get(i);
				shell.close();
			}
		});
		canvas.addListener(SWT.MouseHover, e -> {
			int i = (e.y / CELL) * columns + e.x / CELL;
			canvas.setToolTipText(i >= 0 && i < colors.size() ? toHex(colors.get(i)) : null);
		});
		return canvas;
	}

	private static void border(GC gc, Canvas canvas) {
		Point s = canvas.getSize();
		gc.setForeground(borderColor(canvas));
		gc.drawRectangle(0, 0, s.x - 1, s.y - 1);
	}

	/* A border which is visible on light and dark backgrounds */
	private static Color borderColor(Canvas canvas) {
		RGB bg = canvas.getBackground().getRGB();
		boolean dark = (bg.red * 299 + bg.green * 587 + bg.blue * 114) / 1000 < 128;
		return new Color(canvas.getDisplay(), dark ? new RGB(110, 110, 110) : new RGB(150, 150, 150));
	}

	/* ######################## The values ######################## */

	private void updateAll(Object source) {
		updating = true;
		try {
			RGB c = rgbNow();
			if (source != hexText) {
				hexText.setText(toHex(c));
			}
			if (source != red) {
				red.setSelection(c.red);
			}
			if (source != green) {
				green.setSelection(c.green);
			}
			if (source != blue) {
				blue.setSelection(c.blue);
			}
			hsvText.setText(Math.round(hue) + "°, " + Math.round(sat * 100) + "%, " + Math.round(val * 100) + "%");
		} finally {
			updating = false;
		}
		svCanvas.redraw();
		hueCanvas.redraw();
		preview.redraw();
	}

	private RGB rgbNow() {
		return hsvToRgb(hue, sat, val);
	}

	private void setFromRgb(RGB c) {
		float[] hsb = c.getHSB();
		/* The hue of a grey stays (else the color field would jump to red) */
		if (hsb[1] > 0) {
			hue = hsb[0] >= 360 ? 0 : hsb[0];
		}
		sat = hsb[1];
		val = hsb[2];
	}

	private Spinner spinner(Composite parentComposite, String name) {
		label(parentComposite, name, 1);
		Spinner s = new Spinner(parentComposite, SWT.BORDER);
		s.setValues(0, 0, 255, 0, 1, 16);
		s.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		s.addListener(SWT.Modify, _ -> {
			if (updating) {
				return;
			}
			setFromRgb(new RGB(red.getSelection(), green.getSelection(), blue.getSelection()));
			updateAll(s);
		});
		return s;
	}

	private static Label label(Composite parentComposite, String text, int span) {
		Label l = new Label(parentComposite, SWT.NONE);
		l.setText(text);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd.horizontalSpan = span;
		l.setLayoutData(gd);
		return l;
	}

	private static GridData size(int w, int h) {
		GridData gd = new GridData(SWT.LEFT, SWT.TOP, false, false);
		gd.widthHint = w;
		gd.heightHint = h;
		return gd;
	}

	private void center(Shell s) {
		Rectangle area = parent != null && !parent.isDisposed() && parent.isVisible() ? parent.getBounds()
				: s.getDisplay().getPrimaryMonitor().getClientArea();
		Point size = s.getSize();
		s.setLocation(area.x + (area.width - size.x) / 2, area.y + Math.max(0, (area.height - size.y) / 3));
	}

	private void disposeImages() {
		for (Image i : new Image[] { svImage, hueImage }) {
			if (i != null && !i.isDisposed()) {
				i.dispose();
			}
		}
	}

	/* ######################## Recent colors (kept) ######################## */

	private void loadRecent() {
		recent.clear();
		if (rgbs != null) {
			for (RGB c : rgbs) {
				if (c != null && recent.size() < RECENT_COUNT && !recent.contains(c)) {
					recent.add(c);
				}
			}
		}
		String stored;
		try {
			stored = Platform.getPreferencesService().getString("com.eco.bio7.util", RECENT, "", null);
		} catch (RuntimeException e) {
			/* Without the preferences (e.g., in a test) */
			stored = "";
		}
		for (String h : stored.split(",")) {
			RGB c = parseHex(h);
			if (c != null && recent.size() < RECENT_COUNT && !recent.contains(c)) {
				recent.add(c);
			}
		}
	}

	private void addRecent(RGB c) {
		recent.remove(c);
		recent.add(0, c);
		while (recent.size() > RECENT_COUNT) {
			recent.remove(recent.size() - 1);
		}
		StringBuilder sb = new StringBuilder();
		for (RGB r : recent) {
			sb.append(sb.length() > 0 ? "," : "").append(toHex(r));
		}
		try {
			IEclipsePreferences node = InstanceScope.INSTANCE.getNode("com.eco.bio7.util");
			node.put(RECENT, sb.toString());
			node.flush();
		} catch (Exception | LinkageError e) {
			/* Written at shutdown (or no preferences, e.g., in a test) */
		}
	}

	/* ######################## Pick from the screen ######################## */

	/* Not on Wayland (no screen capture by the application) */
	private static boolean canPickFromScreen() {
		return !"wayland".equalsIgnoreCase(System.getenv("XDG_SESSION_TYPE"));
	}

	/*
	 * Picks a color from the screen: a nearly invisible shell over all monitors
	 * catches the mouse, the real screen stays visible (sharp, no screenshot) and
	 * the dialog shows the color under the cursor while it moves; a click takes it,
	 * Esc cancels. Without transparent shells (e.g., Linux without a compositor) a
	 * screenshot is shown instead. macOS: needs the permission for screen
	 * recording (else the colors are empty).
	 */
	private void pickFromScreen() {
		Display display = shell.getDisplay();
		Rectangle bounds = display.getBounds();
		RGB before = rgbNow();
		Shell overlay = new Shell(shell, SWT.NO_TRIM | SWT.ON_TOP);
		overlay.setBounds(bounds);
		overlay.setBackground(display.getSystemColor(SWT.COLOR_BLACK));
		overlay.setAlpha(1);
		if (overlay.getAlpha() != 1) {
			/* No transparent shells */
			overlay.dispose();
			pickFromScreenshot();
			return;
		}
		overlay.setCursor(display.getSystemCursor(SWT.CURSOR_CROSS));
		boolean[] picked = { false };
		overlay.addListener(SWT.MouseMove, _ -> {
			/* The overlay darkens the screen by 1/255 at most (the live preview) */
			RGB c = screenColor(display, display.getCursorLocation());
			if (c != null) {
				setFromRgb(c);
				updateAll(null);
			}
		});
		overlay.addListener(SWT.MouseDown, _ -> {
			Point p = display.getCursorLocation();
			picked[0] = true;
			overlay.close();
			/* The exact color without the overlay (after it is gone from the screen) */
			display.timerExec(80, () -> {
				if (shell.isDisposed()) {
					return;
				}
				RGB c = screenColor(display, p);
				if (c != null) {
					setFromRgb(c);
				}
				updateAll(null);
			});
		});
		overlay.addListener(SWT.KeyDown, e -> {
			if (e.keyCode == SWT.ESC) {
				overlay.close();
			}
		});
		overlay.addListener(SWT.Dispose, _ -> {
			if (!picked[0] && !shell.isDisposed()) {
				/* Canceled: the color before */
				setFromRgb(before);
				updateAll(null);
			}
			if (!shell.isDisposed()) {
				shell.forceActive();
			}
		});
		overlay.open();
		overlay.forceFocus();
	}

	/* The color of the screen at the point (display coordinates) or null */
	private static RGB screenColor(Display display, Point p) {
		Image pixel = new Image(display, 1, 1);
		GC gc = new GC(display);
		try {
			gc.copyArea(pixel, p.x, p.y);
			ImageData data = pixel.getImageData();
			return data.palette.getRGB(data.getPixel(0, 0));
		} catch (RuntimeException e) {
			return null;
		} finally {
			gc.dispose();
			pixel.dispose();
		}
	}

	/* The fallback: a screenshot of all monitors in a shell over the screen, a click picks the color */
	private void pickFromScreenshot() {
		Display display = shell.getDisplay();
		Rectangle bounds = display.getBounds();
		shell.setVisible(false);
		/* The dialog is hidden before the screenshot */
		display.readAndDispatch();
		display.update();
		Image shot = new Image(display, bounds.width, bounds.height);
		GC gc = new GC(display);
		try {
			gc.copyArea(shot, bounds.x, bounds.y);
		} finally {
			gc.dispose();
		}
		ImageData pixels = shot.getImageData();
		Shell overlay = new Shell(shell, SWT.NO_TRIM | SWT.ON_TOP | SWT.APPLICATION_MODAL);
		overlay.setBounds(bounds);
		overlay.setCursor(display.getSystemCursor(SWT.CURSOR_CROSS));
		overlay.addListener(SWT.Paint, com.eco.bio7.util.SafeEvents.guard("ThemedColorDialog painting", e -> e.gc.drawImage(shot, 0, 0)));
		overlay.addListener(SWT.MouseDown, e -> {
			int x = Math.max(0, Math.min(pixels.width - 1, e.x * pixels.width / Math.max(1, bounds.width)));
			int y = Math.max(0, Math.min(pixels.height - 1, e.y * pixels.height / Math.max(1, bounds.height)));
			setFromRgb(pixels.palette.getRGB(pixels.getPixel(x, y)));
			overlay.close();
		});
		overlay.addListener(SWT.KeyDown, e -> {
			if (e.keyCode == SWT.ESC) {
				overlay.close();
			}
		});
		overlay.addListener(SWT.Dispose, _ -> {
			shot.dispose();
			if (!shell.isDisposed()) {
				shell.setVisible(true);
				shell.forceActive();
				updateAll(null);
			}
		});
		overlay.open();
		overlay.forceFocus();
	}

	/* ######################## Colors ######################## */

	private static float clamp(float f) {
		return Math.max(0, Math.min(1, f));
	}

	private static RGB hsvToRgb(float h, float s, float v) {
		return new RGB(h % 360f, clamp(s), clamp(v));
	}

	static String toHex(RGB c) {
		return String.format("#%02x%02x%02x", c.red, c.green, c.blue);
	}

	private static RGB fromHex(String h) {
		int n = Integer.parseInt(h, 16);
		return new RGB((n >> 16) & 255, (n >> 8) & 255, n & 255);
	}

	private static RGB parseHex(String text) {
		String t = text == null ? "" : text.trim();
		if (t.startsWith("#")) {
			t = t.substring(1);
		}
		if (t.length() == 3) {
			t = "" + t.charAt(0) + t.charAt(0) + t.charAt(1) + t.charAt(1) + t.charAt(2) + t.charAt(2);
		}
		if (!t.matches("[0-9a-fA-F]{6}")) {
			return null;
		}
		return fromHex(t);
	}
}
