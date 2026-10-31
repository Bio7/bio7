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
package com.eco.bio7.worldwind;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;

/**
 * The sources and the licenses of the data of the default layers (e.g., the
 * imagery and the elevations of the globe) which have to be cited: as the
 * tooltip of a layer and in a dialog with the links of the sources.
 */
public final class LayerSources {

	/**
	 * The source of a layer.
	 *
	 * @param layers  the names of the layers (or null for data without a layer,
	 *                e.g., the elevations)
	 * @param title   the title in the dialog
	 * @param credit  the citation of the source
	 * @param license the license or the terms of use
	 * @param url     the link to the source
	 */
	private record Source(String[] layers, String title, String credit, String license, String url) {
	}

	private static final Source[] SOURCES = {
			new Source(new String[] { "Sentinel-2 cloudless 2016 (EOX)" }, "Sentinel-2 cloudless 2016 (EOX)",
					"Sentinel-2 cloudless by EOX IT Services GmbH (Contains modified Copernicus Sentinel data 2016)",
					"Creative Commons Attribution 4.0 International (CC BY 4.0)", "https://cloudless.eox.at"),
			new Source(new String[] { "OpenStreetMap (terrestris)" }, "OpenStreetMap (terrestris)",
					"\u00A9 OpenStreetMap contributors, WMS by terrestris GmbH & Co. KG",
					"Open Database License (ODbL), see https://www.openstreetmap.org/copyright",
					"https://www.terrestris.de/en/openstreetmap-wms/"),
			new Source(new String[] { "Blue Marble (WMS) 2004" }, "Blue Marble (WMS) 2004",
					"NASA Earth Observatory Blue Marble Next Generation, provided by NASA's Global Imagery Browse Services (GIBS), part of NASA's Earth Science Data and Information System (ESDIS)",
					"NASA imagery, free to use (please cite NASA as the source)", "https://www.earthdata.nasa.gov/gibs"),
			new Source(new String[] { "NASA Blue Marble Image" }, "NASA Blue Marble Image",
					"NASA Earth Observatory Blue Marble (included in NASA WorldWind)",
					"NASA imagery, free to use (please cite NASA as the source)", "https://earthobservatory.nasa.gov/features/BlueMarble"),
			new Source(new String[] { "Bing Imagery" }, "Bing Imagery", "\u00A9 Microsoft Corporation (Bing Maps), served by NASA WorldWind",
					"The terms of use of Bing Maps", "https://learn.microsoft.com/en-us/bingmaps/"),
			new Source(new String[] { "USGS NAIP Plus", "USGS Urban Area Ortho" }, "USGS NAIP Plus, USGS Urban Area Ortho",
					"U.S. Geological Survey (USGS), U.S. Department of Agriculture (USDA)", "Public domain (U.S. Government)",
					"https://www.usgs.gov"),
			new Source(null, "Elevations of the globe",
					"NASA WorldWind elevation service: ASTER GDEM v2 (METI and NASA), USGS National Elevation Dataset (NED), NASA SRTM30 and the GEBCO grid (GEBCO Compilation Group)",
					"Free to use (please cite the sources)", "https://worldwind.arc.nasa.gov"),
			new Source(new String[] { "Place Names", "Political Boundaries" }, "Place Names, Political Boundaries",
					"NASA WorldWind", "Free to use (NASA Open Source Agreement / Apache License 2.0)",
					"https://worldwind.arc.nasa.gov") };

	private LayerSources() {
	}

	/** The citation of the source of the layer (e.g., for a tooltip) or null. */
	public static String credit(String layerName) {
		for (Source source : SOURCES) {
			if (source.layers() == null) {
				continue;
			}
			for (String layer : source.layers()) {
				if (layer.equals(layerName)) {
					return "Source: " + source.credit() + "\nLicense: " + source.license();
				}
			}
		}
		return null;
	}

	/** Opens a dialog with the sources and the licenses of the layers (non-modal). */
	public static void open(Shell parent) {
		Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MODELESS);
		shell.setText("Layer Sources and Licenses");
		shell.setLayout(new GridLayout(1, false));
		/* The background of the shell (e.g., of a dark theme) for all children */
		shell.setBackgroundMode(SWT.INHERIT_DEFAULT);
		ScrolledComposite scroll = new ScrolledComposite(shell, SWT.V_SCROLL);
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Composite content = new Composite(scroll, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.verticalSpacing = 2;
		content.setLayout(layout);
		Label intro = new Label(content, SWT.WRAP);
		intro.setText("Please cite the sources of the layers in your publications, e.g., when you publish a screenshot of the globe.");
		intro.setLayoutData(width(new GridData(SWT.FILL, SWT.TOP, true, false)));
		FontData[] data = parent.getDisplay().getSystemFont().getFontData();
		for (FontData d : data) {
			d.setStyle(SWT.BOLD);
		}
		Font bold = new Font(parent.getDisplay(), data);
		shell.addDisposeListener(_ -> bold.dispose());
		for (Source source : SOURCES) {
			Label title = new Label(content, SWT.NONE);
			title.setText(source.title());
			title.setFont(bold);
			GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
			gd.verticalIndent = 10;
			title.setLayoutData(gd);
			Link link = new Link(content, SWT.WRAP);
			link.setText(source.credit().replace("&", "&&") + "\nLicense: " + source.license().replace("&", "&&") + "\n<a href=\""
					+ source.url() + "\">" + source.url() + "</a>");
			link.setLayoutData(width(new GridData(SWT.FILL, SWT.TOP, true, false)));
			link.addListener(SWT.Selection, e -> Program.launch(e.text));
		}
		scroll.setContent(content);
		scroll.setExpandHorizontal(true);
		scroll.setExpandVertical(true);
		scroll.setMinSize(content.computeSize(520, SWT.DEFAULT));
		Button close = new Button(shell, SWT.PUSH);
		close.setText("Close");
		close.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		close.addListener(SWT.Selection, _ -> shell.close());
		shell.setSize(600, 560);
		Rectangle p = parent.getBounds();
		Rectangle s = shell.getBounds();
		shell.setLocation(p.x + (p.width - s.width) / 2, p.y + (p.height - s.height) / 3);
		shell.open();
	}

	/* A wrapping text with the width of the dialog */
	private static GridData width(GridData gd) {
		gd.widthHint = 520;
		return gd;
	}
}
