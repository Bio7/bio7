/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
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

package com.eco.bio7.rbridge;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.batch.Bio7Dialog;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.ArrayTypePermission;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;

/**
 * Loads a Bio7 XML archive (*.bgjar, see SaveGridXMLJob) as a new sheet of the
 * Bio7 table: the XML and the images are read directly from the archive (no
 * files are extracted). Also the archives of former Bio7 versions are read.
 * Only the classes of the format are created from the XML (a file from
 * somewhere can't create other objects).
 */
public class LoadGridXMLJob extends WorkspaceJob {

	protected String file;
	protected Grid grid;

	public LoadGridXMLJob(String files) {
		super("Load Bio7 Jar");
		this.file = files;
	}

	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Load Bio7 Jar", IProgressMonitor.UNKNOWN);
		try {
			load();
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	private void load() {
		File archive = new File(file);
		if (!archive.exists()) {
			Bio7Dialog.message("File not found!");
			return;
		}
		GridXmlObject gridXml = null;
		/* The images: row, column and the image data */
		List<Object[]> images = new ArrayList<>();
		try (ZipFile zip = new ZipFile(archive)) {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			List<ZipEntry> imageEntries = new ArrayList<>();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				String name = new File(entry.getName()).getName();
				if (entry.isDirectory()) {
					continue;
				}
				if (name.toLowerCase().endsWith(".xml") && gridXml == null) {
					try (InputStream in = zip.getInputStream(entry)) {
						gridXml = (GridXmlObject) xstream().fromXML(new InputStreamReader(in, StandardCharsets.UTF_8));
					}
				} else if (name.toLowerCase().endsWith(".png")) {
					imageEntries.add(entry);
				}
			}
			for (ZipEntry entry : imageEntries) {
				/* The name ends with _row_column.png (former versions: a prefix before) */
				String name = new File(entry.getName()).getName();
				String[] parts = name.substring(0, name.length() - 4).split("_");
				if (parts.length < 3) {
					continue;
				}
				try (InputStream in = zip.getInputStream(entry)) {
					int row = Integer.parseInt(parts[parts.length - 2]);
					int column = Integer.parseInt(parts[parts.length - 1]);
					images.add(new Object[] { row, column, new ImageData(in) });
				} catch (RuntimeException e) {
					/* Not an image of a cell */
				}
			}
		} catch (Exception e) {
			Bio7Dialog.message("The file can't be read as a Bio7 XML archive (*.bgjar):\n" + e.getMessage());
			return;
		}
		if (gridXml == null || gridXml.getGridValues() == null || gridXml.getGridValues().length == 0) {
			Bio7Dialog.message("The file contains no sheet!");
			return;
		}
		GridXmlObject g = gridXml;
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(() -> {
			if (RTable.getTabFolder() == null) {
				return;
			}
			String[][] values = g.getGridValues();
			GridXMLFontObject[][] fonts = g.getFontObject();
			int[][] cellHeights = g.getCellHeight();
			int[][] cellWidths = g.getCellWidth();
			Color[][] colors = g.getCellColor();
			FontRegistry fontRegistry = JFaceResources.getFontRegistry();
			ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
			grid = new Spread().spread(RTable.getTabFolder(), values.length, values[0].length, new File(file).getName());
			if (grid == null) {
				return;
			}
			Color co = g.getGridLineColor();
			if (co != null) {
				grid.setLineColor(color(colorRegistry, co));
			}
			RTable.setGrid(grid);
			for (int items = 0; items < grid.getItemCount(); items++) {
				GridItem item = grid.getItem(items);
				for (int columns = 0; columns < grid.getColumnCount(); columns++) {
					item.setText(columns, values[columns][items] != null ? values[columns][items] : "");
					GridXMLFontObject gxml = fonts != null ? fonts[columns][items] : null;
					if (gxml != null && gxml.getName() != null) {
						String key = "bio7.grid.font." + gxml.getName() + "." + gxml.getHeight() + "." + gxml.getStyle();
						if (!fontRegistry.hasValueFor(key)) {
							fontRegistry.put(key, new FontData[] { new FontData(gxml.getName(), gxml.getHeight(), gxml.getStyle()) });
						}
						item.setFont(columns, fontRegistry.get(key));
					}
					/* No color: the color of the theme */
					Color cob = colors != null ? colors[columns][items] : null;
					if (cob != null) {
						item.setBackground(columns, color(colorRegistry, cob));
					}
					if (cellHeights != null) {
						item.setHeight(cellHeights[columns][items]);
					}
					if (cellWidths != null) {
						grid.getColumn(columns).setWidth(cellWidths[columns][items]);
					}
				}
			}
			for (Object[] im : images) {
				int row = (Integer) im[0], column = (Integer) im[1];
				if (row < grid.getItemCount() && column < grid.getColumnCount()) {
					grid.getItem(row).setImage(column, new Image(display, (ImageData) im[2]));
				}
			}
		});
	}

	/* The XML reader: only the classes of the format */
	private static XStream xstream() {
		XStream xs = new XStream(new DomDriver());
		xs.addPermission(NoTypePermission.NONE);
		xs.addPermission(NullPermission.NULL);
		xs.addPermission(PrimitiveTypePermission.PRIMITIVES);
		xs.addPermission(ArrayTypePermission.ARRAYS);
		xs.allowTypes(new Class[] { GridXmlObject.class, GridXMLFontObject.class, Color.class, String.class });
		xs.setClassLoader(GridXmlObject.class.getClassLoader());
		return xs;
	}

	private static org.eclipse.swt.graphics.Color color(ColorRegistry registry, Color c) {
		String key = "RGB {" + c.getRed() + ", " + c.getGreen() + ", " + c.getBlue() + "}";
		if (!registry.hasValueFor(key)) {
			registry.put(key, new RGB(c.getRed(), c.getGreen(), c.getBlue()));
		}
		return registry.get(key);
	}
}
