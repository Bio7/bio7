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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * The font dialog of Bio7: a drop-in replacement of the SWT FontDialog (the same
 * methods). By default the Bio7 font dialog (SWT widgets, styled by the theme,
 * e.g., dark in a dark theme on every OS): the fonts with a search field and a
 * filter for fixed-width fonts, the style, the size and a preview with code,
 * else the native font dialog of the OS (see {@link ThemedDialogs#FONT}).
 */
public class ThemedFontDialog {

	private static final String[] STYLES = { "Regular", "Bold", "Italic", "Bold Italic" };
	private static final int[] STYLE_BITS = { SWT.NORMAL, SWT.BOLD, SWT.ITALIC, SWT.BOLD | SWT.ITALIC };
	private static final int[] SIZES = { 6, 7, 8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 22, 24, 28, 32, 36, 48, 72 };
	private static final String SAMPLE = "AaBbCcXxYyZz 0123456789 il1I|O0 {}[]()\nx <- c(1, 2, 3); y = [v * 2 for v in x]";
	/* Fixed width or not, per font name (measured once) */
	private static final Map<String, Boolean> MONOSPACE = new HashMap<>();

	private final Shell parent;
	private final int style;
	private String title = "Font";
	private FontData[] fontList;
	private RGB rgb;
	private boolean effectsVisible = true;

	/* The state of the open dialog */
	private Shell shell;
	private List<String> families;
	private org.eclipse.swt.widgets.List familyList, styleList, sizeList;
	private Text filter, sizeText;
	private Button monospaceOnly;
	private Canvas preview;
	private Font previewFont;
	private String family;
	private int fontStyle;
	private int size;
	private FontData result;

	public ThemedFontDialog(Shell parent) {
		this(parent, SWT.APPLICATION_MODAL);
	}

	public ThemedFontDialog(Shell parent, int style) {
		this.parent = parent;
		this.style = style;
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

	public FontData[] getFontList() {
		return fontList;
	}

	public void setFontList(FontData[] fontData) {
		this.fontList = fontData;
	}

	/** @deprecated like FontDialog: use {@link #getFontList()} */
	@Deprecated
	public FontData getFontData() {
		return fontList == null || fontList.length == 0 ? null : fontList[0];
	}

	/** @deprecated like FontDialog: use {@link #setFontList(FontData[])} */
	@Deprecated
	public void setFontData(FontData fontData) {
		this.fontList = fontData == null ? null : new FontData[] { fontData };
	}

	/** The color of the native dialog (its effects), not shown by the Bio7 dialog. */
	public RGB getRGB() {
		return rgb;
	}

	public void setRGB(RGB rgb) {
		this.rgb = rgb;
	}

	public boolean getEffectsVisible() {
		return effectsVisible;
	}

	public void setEffectsVisible(boolean visible) {
		this.effectsVisible = visible;
	}

	/** Opens the dialog: the chosen font or null if canceled (like FontDialog.open). */
	public FontData open() {
		if (!ThemedDialogs.isEnabled(ThemedDialogs.FONT)) {
			FontDialog dialog = new FontDialog(parent, style);
			dialog.setText(title);
			if (fontList != null) {
				dialog.setFontList(fontList);
			}
			if (rgb != null) {
				dialog.setRGB(rgb);
			}
			dialog.setEffectsVisible(effectsVisible);
			FontData chosen = dialog.open();
			if (chosen != null) {
				fontList = dialog.getFontList();
				rgb = dialog.getRGB();
			}
			return chosen;
		}
		return openBio7();
	}

	/* ######################## The Bio7 dialog ######################## */

	private FontData openBio7() {
		Display display = parent != null && !parent.isDisposed() ? parent.getDisplay() : Display.getDefault();
		FontData initial = fontList != null && fontList.length > 0 ? fontList[0] : display.getSystemFont().getFontData()[0];
		family = initial.getName();
		fontStyle = initial.getStyle() & (SWT.BOLD | SWT.ITALIC);
		size = Math.max(1, initial.getHeight());
		result = null;
		families = fontNames(display);
		if (!containsIgnoreCase(families, family)) {
			families.add(0, family);
		}

		shell = parent != null && !parent.isDisposed() ? new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL)
				: new Shell(display, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
		shell.setText(title);
		GridLayout layout = new GridLayout(3, false);
		layout.marginWidth = 12;
		layout.marginHeight = 12;
		layout.horizontalSpacing = 10;
		shell.setLayout(layout);

		label(shell, "Font:");
		label(shell, "Style:");
		label(shell, "Size:");
		filter = new Text(shell, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		filter.setMessage("Search");
		filter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		filter.addListener(SWT.Modify, _ -> fillFamilies());
		new Label(shell, SWT.NONE);
		sizeText = new Text(shell, SWT.BORDER);
		sizeText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		sizeText.addListener(SWT.Modify, _ -> {
			try {
				int s = Integer.parseInt(sizeText.getText().trim());
				if (s >= 1 && s <= 400 && s != size) {
					size = s;
					selectSize();
					updatePreview();
				}
			} catch (NumberFormatException e) {
				/* Not a size (yet) */
			}
		});

		familyList = list(shell, 240, 220);
		familyList.addListener(SWT.Selection, _ -> {
			int i = familyList.getSelectionIndex();
			if (i >= 0) {
				family = familyList.getItem(i);
				updatePreview();
			}
		});
		styleList = list(shell, 110, 220);
		styleList.setItems(STYLES);
		styleList.addListener(SWT.Selection, _ -> {
			int i = styleList.getSelectionIndex();
			if (i >= 0) {
				fontStyle = STYLE_BITS[i];
				updatePreview();
			}
		});
		sizeList = list(shell, 60, 220);
		for (int s : SIZES) {
			sizeList.add(Integer.toString(s));
		}
		sizeList.addListener(SWT.Selection, _ -> {
			int i = sizeList.getSelectionIndex();
			if (i >= 0) {
				size = SIZES[i];
				sizeText.setText(Integer.toString(size));
				updatePreview();
			}
		});

		monospaceOnly = new Button(shell, SWT.CHECK);
		monospaceOnly.setText("Fixed-width fonts only (e.g., for code)");
		monospaceOnly.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		monospaceOnly.setSelection(isMonospace(display, family));
		monospaceOnly.addListener(SWT.Selection, _ -> fillFamilies());

		Label previewLabel = new Label(shell, SWT.NONE);
		previewLabel.setText("Preview:");
		previewLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		preview = new Canvas(shell, SWT.BORDER | SWT.DOUBLE_BUFFERED);
		GridData pd = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		pd.heightHint = 86;
		preview.setLayoutData(pd);
		preview.addListener(SWT.Paint, e -> {
			if (previewFont != null && !previewFont.isDisposed()) {
				e.gc.setFont(previewFont);
			}
			e.gc.setForeground(preview.getForeground());
			Rectangle area = preview.getClientArea();
			e.gc.setClipping(area);
			e.gc.drawText(SAMPLE, 8, 6, true);
		});

		createButtons();
		shell.addListener(SWT.Dispose, _ -> {
			if (previewFont != null && !previewFont.isDisposed()) {
				previewFont.dispose();
			}
		});

		fillFamilies();
		styleList.select(styleIndex(fontStyle));
		sizeText.setText(Integer.toString(size));
		selectSize();
		updatePreview();
		shell.pack();
		Point s = shell.getSize();
		shell.setSize(Math.max(s.x, 460), s.y);
		center(shell);
		shell.open();
		filter.setFocus();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		if (result != null) {
			fontList = new FontData[] { result };
		}
		return result;
	}

	private void createButtons() {
		Composite buttons = new Composite(shell, SWT.NONE);
		GridLayout gl = new GridLayout(2, true);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		buttons.setLayout(gl);
		GridData gd = new GridData(SWT.END, SWT.CENTER, true, false, 3, 1);
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
			result = new FontData(family, size, fontStyle);
			shell.close();
		});
		cancel.addListener(SWT.Selection, _ -> shell.close());
		shell.setDefaultButton(ok);
	}

	/* The fonts of the list: the search text and the filter (fixed-width) */
	private void fillFamilies() {
		String query = filter.getText().trim().toLowerCase(Locale.ROOT);
		boolean mono = monospaceOnly.getSelection();
		List<String> shown = new ArrayList<>();
		Runnable fill = () -> {
			for (String name : families) {
				if (!query.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(query)) {
					continue;
				}
				if (mono && !isMonospace(shell.getDisplay(), name)) {
					continue;
				}
				shown.add(name);
			}
		};
		if (mono) {
			/* The first time all fonts are measured */
			BusyIndicator.showWhile(shell.getDisplay(), fill);
		} else {
			fill.run();
		}
		familyList.setItems(shown.toArray(new String[0]));
		for (int i = 0; i < shown.size(); i++) {
			if (shown.get(i).equalsIgnoreCase(family)) {
				familyList.select(i);
				familyList.showSelection();
				return;
			}
		}
	}

	private void selectSize() {
		sizeList.deselectAll();
		for (int i = 0; i < SIZES.length; i++) {
			if (SIZES[i] == size) {
				sizeList.select(i);
				sizeList.showSelection();
			}
		}
	}

	private void updatePreview() {
		Font old = previewFont;
		previewFont = new Font(shell.getDisplay(), new FontData(family, size, fontStyle));
		preview.redraw();
		if (old != null && !old.isDisposed()) {
			old.dispose();
		}
	}

	private static int styleIndex(int bits) {
		for (int i = 0; i < STYLE_BITS.length; i++) {
			if (STYLE_BITS[i] == bits) {
				return i;
			}
		}
		return 0;
	}

	/* The installed fonts (scalable and bitmap), sorted, without the vertical fonts of Windows (@...) */
	private static List<String> fontNames(Display display) {
		TreeSet<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (boolean scalable : new boolean[] { true, false }) {
			for (FontData fd : display.getFontList(null, scalable)) {
				String n = fd.getName();
				if (n != null && !n.isEmpty() && !n.startsWith("@")) {
					names.add(n);
				}
			}
		}
		return new ArrayList<>(names);
	}

	/* A fixed-width font: "iiiii" and "WWWWW" have the same width */
	private static boolean isMonospace(Display display, String name) {
		Boolean known = MONOSPACE.get(name);
		if (known != null) {
			return known;
		}
		boolean mono = false;
		Font font = new Font(display, name, 12, SWT.NORMAL);
		GC gc = new GC(display);
		try {
			gc.setFont(font);
			mono = gc.textExtent("iiiii").x == gc.textExtent("WWWWW").x && gc.textExtent("iiiii").x > 0;
		} finally {
			gc.dispose();
			font.dispose();
		}
		MONOSPACE.put(name, mono);
		return mono;
	}

	private static boolean containsIgnoreCase(List<String> list, String s) {
		for (String x : list) {
			if (x.equalsIgnoreCase(s)) {
				return true;
			}
		}
		return false;
	}

	private static org.eclipse.swt.widgets.List list(Composite parentComposite, int width, int height) {
		org.eclipse.swt.widgets.List l = new org.eclipse.swt.widgets.List(parentComposite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, width > 100, true);
		gd.widthHint = width;
		gd.heightHint = height;
		l.setLayoutData(gd);
		return l;
	}

	private static void label(Composite parentComposite, String text) {
		Label l = new Label(parentComposite, SWT.NONE);
		l.setText(text);
	}

	private void center(Shell s) {
		Rectangle area = parent != null && !parent.isDisposed() && parent.isVisible() ? parent.getBounds()
				: s.getDisplay().getPrimaryMonitor().getClientArea();
		Point sz = s.getSize();
		s.setLocation(area.x + (area.width - sz.x) / 2, area.y + Math.max(0, (area.height - sz.y) / 3));
	}
}
