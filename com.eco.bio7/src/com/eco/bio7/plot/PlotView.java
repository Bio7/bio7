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
package com.eco.bio7.plot;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

/**
 * The plot of data of the R-Shell or the Python-Shell with options (a view, one
 * for each language, the secondary id): the data (the variables and columns of
 * the workspace for the roles of the plot type), the type, style and palette,
 * the texts and font sizes, the axes (limits, log, legend) and the marks.
 * Plot draws with the options (the view stays open for changes); the source of
 * the plot can be copied or inserted into an R or Python editor for a
 * customization. The last options are kept.
 */
public class PlotView extends ViewPart {

	public static final String ID = "com.eco.bio7.plot.PlotView";

	/** The shell of a language: the variables, the plot and its source. */
	public interface Target {
		/** The variables and columns of the workspace (UI thread). */
		List<String> variables();

		/** Plots the data with the options. */
		void plot(PlotOptions options, PlotData data);

		/** The source of the plot. */
		String source(PlotOptions options, PlotData data);
	}

	private Target target;
	private String language = "";

	private Combo kind, style, palette, lineStyle, pointShape, legend;
	private Text title, subtitle, caption, xLabel, yLabel, xMin, xMax, yMin, yMax;
	private Button colorButton, grid, logY, horizontal, valueLabels, observations;
	private Spinner fontSize, titleSize, opacity, bins, lineWidth, pointSize;
	private Combo dataX, dataGroup, dataZ;
	private org.eclipse.swt.widgets.List dataY;
	private Label dataHelp;
	private RGB rgb;
	private Color swatch;
	private ScrolledComposite scrolled;
	private Composite content;

	/**
	 * Shows the view of the language (R or Python) with the data of the shell
	 * (e.g., its selection). UI thread.
	 */
	public static void open(String language, Target target, PlotData initial) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			IViewPart part = page.showView(ID, language, IWorkbenchPage.VIEW_ACTIVATE);
			if (part instanceof PlotView view) {
				view.connect(language, target, initial);
			}
		} catch (PartInitException e) {
			MessageDialog.openError(page.getWorkbenchWindow().getShell(), "Plot", "The plot view could not be opened: " + e.getMessage());
		}
	}

	@Override
	public void createPartControl(org.eclipse.swt.widgets.Composite parent) {
		scrolled = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);
		content = new Composite(scrolled, SWT.NONE);
		scrolled.setContent(content);
		GridLayout layout = new GridLayout(2, true);
		layout.marginWidth = 8;
		layout.horizontalSpacing = 8;
		content.setLayout(layout);
		PlotOptions o = PlotOptions.last();

		createButtons(content);
		createData(content);

		Group gPlot = group(content, "Plot");
		kind = combo(gPlot, "Type:", PlotOptions.KINDS, o.kind());
		kind.addListener(SWT.Selection, _ -> updateData());
		style = combo(gPlot, "Style:", PlotOptions.STYLES, o.style());
		style.setToolTipText("Modern: a left aligned bold title with subtitle and caption, a light grid behind the data,\nno box, points with a white border\nClassic: the defaults of R or matplotlib");
		palette = combo(gPlot, "Palette:", PlotOptions.PALETTES, o.palette());
		palette.addListener(SWT.Selection, _ -> {
			/* The first color of the palette is the color of the plot */
			rgb = parse(PlotOptions.colors(PlotOptions.PALETTES[palette.getSelectionIndex()][0])[0]);
			updateSwatch();
		});
		label(gPlot, "Color:");
		rgb = parse(o.color());
		colorButton = new Button(gPlot, SWT.PUSH);
		colorButton.setToolTipText("The color (several variables or categories: the next colors of the palette)");
		colorButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		updateSwatch();
		colorButton.addListener(SWT.Selection, _ -> {
			com.eco.bio7.util.ThemedColorDialog cd = new com.eco.bio7.util.ThemedColorDialog(getSite().getShell());
			cd.setRGB(rgb);
			RGB chosen = cd.open();
			if (chosen != null) {
				rgb = chosen;
				updateSwatch();
			}
		});
		opacity = spinner(gPlot, "Opacity (%):", (int) Math.round(o.opacity() * 100), 5, 100, 0);

		Group gText = group(content, "Text");
		title = text(gText, "Title:", o.title());
		subtitle = text(gText, "Subtitle:", o.subtitle());
		subtitle.setToolTipText("Modern style: a gray line below the title");
		caption = text(gText, "Caption:", o.caption());
		caption.setToolTipText("Modern style: a small text at the bottom right (e.g., the source of the data)");
		xLabel = text(gText, "X label:", o.xLabel());
		yLabel = text(gText, "Y label:", o.yLabel());
		fontSize = spinner(gText, "Font size (pt):", (int) Math.round(o.fontSize()), 5, 40, 0);
		titleSize = spinner(gText, "Title size (pt):", (int) Math.round(o.titleSize()), 5, 60, 0);

		Group gAxes = group(content, "Axes and Legend");
		xMin = text(gAxes, "X minimum:", o.xMin());
		xMax = text(gAxes, "X maximum:", o.xMax());
		yMin = text(gAxes, "Y minimum:", o.yMin());
		yMax = text(gAxes, "Y maximum:", o.yMax());
		for (Text t : new Text[] { xMin, xMax, yMin, yMax }) {
			t.setMessage("auto");
			t.setToolTipText("A number (both limits of an axis are needed); empty: automatic");
		}
		legend = combo(gAxes, "Legend:", PlotOptions.LEGENDS, o.legend());
		grid = check(gAxes, "Grid", o.grid());
		logY = check(gAxes, "Logarithmic y axis", o.logY());
		horizontal = check(gAxes, "Horizontal (box, bar)", o.horizontal());

		Group gMarks = group(content, "Lines, Points and Bars");
		lineWidth = spinner(gMarks, "Line width:", (int) Math.round(o.lineWidth() * 10), 1, 100, 1);
		lineStyle = combo(gMarks, "Line style:", PlotOptions.LINES, o.lineStyle());
		pointSize = spinner(gMarks, "Point size:", (int) Math.round(o.pointSize() * 10), 1, 100, 1);
		pointShape = combo(gMarks, "Point shape:", PlotOptions.SHAPES, o.pointShape());
		bins = spinner(gMarks, "Bins (histogram):", o.bins(), 2, 500, 0);
		valueLabels = check(gMarks, "Value labels (bars, pie)", o.valueLabels());
		observations = check(gMarks, "Observations (box, violin)", o.observations());

		content.addListener(SWT.Dispose, _ -> {
			if (swatch != null) {
				swatch.dispose();
			}
		});
		/* The minimum size of the content: the width of the view and the height for this width */
		scrolled.addListener(SWT.Resize, _ -> fit());
		updateData();
		fit();
		/* A view restored with the workbench: the language of the secondary id (the shell connects later) */
		String secondary = getViewSite().getSecondaryId();
		if (secondary != null) {
			language = secondary;
			setPartName("Plot Options (" + language + ")");
		}
	}

	private void fit() {
		if (scrolled == null || scrolled.isDisposed()) {
			return;
		}
		/* At least the width of two columns of the groups, else a horizontal scroll bar */
		int minWidth = 460;
		int width = Math.max(minWidth, scrolled.getClientArea().width);
		scrolled.setMinSize(minWidth, content.computeSize(width, SWT.DEFAULT).y);
	}

	/* The shell of the view and its data (the selection is preselected) */
	private void connect(String lang, Target t, PlotData initial) {
		this.language = lang;
		this.target = t;
		setPartName("Plot Options (" + lang + ")");
		setTitleToolTip("The plot of the variables of the " + ("R".equals(lang) ? "R-Shell" : "Python-Shell") + " with options");
		fillVariables(initial);
	}

	/* The variables of the workspace in the lists (the selection or the former choice kept) */
	private void fillVariables(PlotData selection) {
		List<String> items = new ArrayList<>(target == null ? List.of() : target.variables());
		PlotData keep = selection != null ? selection : data();
		for (String name : keep.ys()) {
			if (!items.contains(name)) {
				items.add(0, name);
			}
		}
		dataY.setItems(items.toArray(String[]::new));
		for (String name : keep.ys()) {
			int i = items.indexOf(name);
			if (i >= 0) {
				dataY.select(i);
			}
		}
		dataY.showSelection();
		for (Combo c : new Combo[] { dataX, dataGroup, dataZ }) {
			String text = c.getText();
			c.removeAll();
			c.add("");
			for (String i : items) {
				c.add(i);
			}
			c.setText(text);
		}
		if (selection != null) {
			dataX.setText(selection.x());
			dataGroup.setText(selection.group());
			dataZ.setText(selection.z());
		}
	}

	/* The buttons: Plot, the source, the defaults and the variables */
	private void createButtons(Composite c) {
		Composite bar = new Composite(c, SWT.NONE);
		bar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		GridLayout bl = new GridLayout(5, false);
		bl.marginWidth = bl.marginHeight = 0;
		bar.setLayout(bl);
		Button plotButton = button(bar, "Plot", "Plots the data with the options (the view stays open for changes)");
		plotButton.addListener(SWT.Selection, _ -> run(Action.PLOT));
		button(bar, "Copy Source", "Copies the source of the plot to the clipboard").addListener(SWT.Selection, _ -> run(Action.COPY));
		button(bar, "Open in Editor", "Inserts the source of the plot into the opened R or Python editor (at the cursor) for a customization")
				.addListener(SWT.Selection, _ -> run(Action.EDITOR));
		button(bar, "Defaults", "The default options (the type and the data stay)").addListener(SWT.Selection, _ -> defaults());
		button(bar, "Refresh", "Reads the variables of the workspace again").addListener(SWT.Selection, _ -> {
			if (target != null) {
				fillVariables(null);
			}
		});
	}

	private enum Action {
		PLOT, COPY, EDITOR
	}

	private void run(Action action) {
		if (target == null) {
			MessageDialog.openInformation(getSite().getShell(), "Plot",
					"Please open the plot options from the R-Shell or the Python-Shell (context menu of the variables: Plot > Plot with Options...).");
			return;
		}
		PlotData d = data();
		if (d.ys().isEmpty() && d.z().isEmpty()) {
			MessageDialog.openInformation(getSite().getShell(), "Plot",
					"Please select one or more variables for Y" + (PlotData.usesZ(id(PlotOptions.KINDS, kind)) ? " (or a matrix for Z)." : "."));
			return;
		}
		PlotOptions o = options();
		PlotOptions.setLast(o);
		switch (action) {
		case PLOT -> target.plot(o, d);
		case COPY -> {
			PlotSource.copy(target.source(o, d));
			MessageDialog.openInformation(getSite().getShell(), "Plot Source", "The " + language + " source of the plot was copied to the clipboard.");
		}
		case EDITOR -> MessageDialog.openInformation(getSite().getShell(), "Plot Source", PlotSource.insert(target.source(o, d), language));
		}
	}

	/* The data: Y (several), X, Group and Z from the variables and columns of the workspace */
	private void createData(Composite c) {
		Group g = new Group(c, SWT.NONE);
		g.setText("Data (variables and columns of the workspace)");
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		Label ly = new Label(g, SWT.NONE);
		ly.setText("Y (values):");
		ly.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		dataY = new org.eclipse.swt.widgets.List(g, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		dataY.setToolTipText("One or more variables or columns (Ctrl/Cmd + click: several)");
		GridData gy = new GridData(SWT.FILL, SWT.FILL, true, true);
		gy.heightHint = 100;
		gy.widthHint = 200;
		dataY.setLayoutData(gy);
		dataX = dataCombo(g, "X:", "The x values (line, area, scatter) or the values of the x axis (contour, surface); empty: the index");
		dataGroup = dataCombo(g, "Group:", "A factor (or values) which splits the values into groups, colors a scatter plot or gives the categories of bars and pies");
		dataZ = dataCombo(g, "Z (matrix):", "A matrix or data frame (heatmap, contour, surface)");
		dataHelp = new Label(g, SWT.WRAP);
		GridData gh = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		gh.widthHint = 200;
		dataHelp.setLayoutData(gh);
	}

	private static Combo dataCombo(Composite g, String label, String tip) {
		label(g, label);
		Combo c = new Combo(g, SWT.DROP_DOWN);
		c.setToolTipText(tip + "\n(an expression can be typed, too)");
		c.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return c;
	}

	/* The fields of the data used by the plot type */
	private void updateData() {
		String k = id(PlotOptions.KINDS, kind);
		dataX.setEnabled(PlotData.usesX(k));
		dataGroup.setEnabled(PlotData.usesGroup(k));
		dataZ.setEnabled(PlotData.usesZ(k));
		dataHelp.setText(PlotData.help(k));
		content.layout(true, true);
		fit();
	}

	private PlotData data() {
		String k = id(PlotOptions.KINDS, kind);
		return new PlotData(PlotData.usesX(k) ? dataX.getText() : "", List.of(dataY.getSelection()), PlotData.usesGroup(k) ? dataGroup.getText() : "",
				PlotData.usesZ(k) ? dataZ.getText() : "");
	}

	private PlotOptions options() {
		return new PlotOptions(id(PlotOptions.KINDS, kind), id(PlotOptions.STYLES, style), id(PlotOptions.PALETTES, palette), title.getText().trim(),
				subtitle.getText().trim(), caption.getText().trim(), xLabel.getText().trim(), yLabel.getText().trim(), fontSize.getSelection(),
				titleSize.getSelection(), String.format("#%02X%02X%02X", rgb.red, rgb.green, rgb.blue), opacity.getSelection() / 100.0, bins.getSelection(),
				lineWidth.getSelection() / 10.0, id(PlotOptions.LINES, lineStyle), pointSize.getSelection() / 10.0, id(PlotOptions.SHAPES, pointShape),
				id(PlotOptions.LEGENDS, legend), xMin.getText().trim(), xMax.getText().trim(), yMin.getText().trim(), yMax.getText().trim(), grid.getSelection(),
				logY.getSelection(), horizontal.getSelection(), valueLabels.getSelection(), observations.getSelection());
	}

	/* The default options (the type and the data stay) */
	private void defaults() {
		PlotOptions d = PlotOptions.DEFAULT;
		select(style, PlotOptions.STYLES, d.style());
		select(palette, PlotOptions.PALETTES, d.palette());
		for (Text t : new Text[] { title, subtitle, caption, xLabel, yLabel, xMin, xMax, yMin, yMax }) {
			t.setText("");
		}
		fontSize.setSelection((int) d.fontSize());
		titleSize.setSelection((int) d.titleSize());
		rgb = parse(d.color());
		updateSwatch();
		opacity.setSelection((int) Math.round(d.opacity() * 100));
		bins.setSelection(d.bins());
		lineWidth.setSelection((int) Math.round(d.lineWidth() * 10));
		select(lineStyle, PlotOptions.LINES, d.lineStyle());
		pointSize.setSelection((int) Math.round(d.pointSize() * 10));
		select(pointShape, PlotOptions.SHAPES, d.pointShape());
		select(legend, PlotOptions.LEGENDS, d.legend());
		grid.setSelection(d.grid());
		logY.setSelection(d.logY());
		horizontal.setSelection(d.horizontal());
		valueLabels.setSelection(d.valueLabels());
		observations.setSelection(d.observations());
	}

	private void updateSwatch() {
		if (swatch != null) {
			swatch.dispose();
		}
		swatch = new Color(rgb);
		colorButton.setBackground(swatch);
		colorButton.setText(String.format("#%02X%02X%02X", rgb.red, rgb.green, rgb.blue));
	}

	@Override
	public void setFocus() {
		if (dataY != null && !dataY.isDisposed()) {
			dataY.setFocus();
		}
	}

	private static String id(String[][] items, Combo c) {
		return items[Math.max(0, c.getSelectionIndex())][0];
	}

	private static void select(Combo c, String[][] items, String id) {
		for (int i = 0; i < items.length; i++) {
			if (items[i][0].equals(id)) {
				c.select(i);
				return;
			}
		}
		c.select(0);
	}

	private static RGB parse(String hex) {
		try {
			int v = Integer.parseInt(hex.replace("#", ""), 16);
			return new RGB((v >> 16) & 0xff, (v >> 8) & 0xff, v & 0xff);
		} catch (RuntimeException e) {
			return new RGB(0x2B, 0x5C, 0x8F);
		}
	}

	private static Button button(Composite c, String text, String tip) {
		Button b = new Button(c, SWT.PUSH);
		b.setText(text);
		b.setToolTipText(tip);
		return b;
	}

	private static Group group(Composite c, String text) {
		Group g = new Group(c, SWT.NONE);
		g.setText(text);
		g.setLayout(new GridLayout(2, false));
		g.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		return g;
	}

	private static void label(Composite c, String text) {
		Label l = new Label(c, SWT.NONE);
		l.setText(text);
	}

	private static Combo combo(Composite c, String label, String[][] items, String value) {
		label(c, label);
		Combo combo = new Combo(c, SWT.READ_ONLY);
		for (String[] i : items) {
			combo.add(i[1]);
		}
		select(combo, items, value);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return combo;
	}

	private static Text text(Composite c, String label, String value) {
		label(c, label);
		Text t = new Text(c, SWT.BORDER);
		t.setText(value);
		GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd.widthHint = 100;
		t.setLayoutData(gd);
		return t;
	}

	private static Spinner spinner(Composite c, String label, int value, int min, int max, int digits) {
		label(c, label);
		Spinner s = new Spinner(c, SWT.BORDER);
		s.setDigits(digits);
		s.setMinimum(min);
		s.setMaximum(max);
		s.setSelection(value);
		s.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return s;
	}

	private static Button check(Composite c, String text, boolean selected) {
		Button b = new Button(c, SWT.CHECK);
		b.setText(text);
		b.setSelection(selected);
		b.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		return b;
	}
}
