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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.batch.Bio7Dialog;
import com.thoughtworks.xstream.XStream;

/**
 * Saves a sheet of the Bio7 table as a Bio7 XML archive (*.bgjar): one file
 * (zip) with the sheet as XML (the values, the fonts and colors of the cells,
 * the widths of the columns and the heights of the rows) and the images of the
 * cells as PNG files (entries "image_row_column.png"). The archive is written
 * directly (no temporary files in the folder). Only the colors and fonts which
 * are set for a cell are stored (not the colors of the theme).
 */
public class SaveGridXMLJob extends WorkspaceJob {

	protected String file;
	protected Grid grid;

	/**
	 * @param file the archive (*.bgjar)
	 * @param path not used (formerly the folder of the temporary files)
	 * @param grid the sheet
	 */
	public SaveGridXMLJob(String file, String path, Grid grid) {
		super("Save as Bio7 XML (*.bgjar)");
		this.file = file;
		this.grid = grid;
	}

	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Save Bio7 XML", IProgressMonitor.UNKNOWN);
		try {
			saveXml(file);
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}

	private void saveXml(String save) {
		if (grid == null) {
			return;
		}
		GridXmlObject gridX = new GridXmlObject();
		/* The images of the cells (PNG) with the names of the entries */
		Map<String, byte[]> images = new LinkedHashMap<>();
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(() -> {
			if (grid.isDisposed()) {
				return;
			}
			int columnCount = grid.getColumnCount();
			int itemCount = grid.getItemCount();
			String[][] values = new String[columnCount][itemCount];
			GridXMLFontObject[][] fontObject = new GridXMLFontObject[columnCount][itemCount];
			int[][] cellHeight = new int[columnCount][itemCount];
			int[][] cellWidth = new int[columnCount][itemCount];
			Color[][] cellColour = new Color[columnCount][itemCount];
			for (int items = 0; items < itemCount; items++) {
				GridItem item = grid.getItem(items);
				for (int columns = 0; columns < columnCount; columns++) {
					values[columns][items] = item.getText(columns);
					/* A font which is set for the cell (not the default of the sheet) */
					Font f = item.getFont(columns);
					if (f != null && !f.isDisposed() && !f.equals(item.getFont())) {
						FontData fontData = f.getFontData()[0];
						GridXMLFontObject fo = new GridXMLFontObject();
						fo.setHeight(fontData.getHeight());
						fo.setStyle(fontData.getStyle());
						fo.setLocale(fontData.getLocale());
						fo.setName(fontData.getName());
						fontObject[columns][items] = fo;
					}
					cellHeight[columns][items] = item.getHeight();
					cellWidth[columns][items] = grid.getColumn(columns).getWidth();
					/* A color which is set for the cell (not the color of the theme) */
					org.eclipse.swt.graphics.Color swtColor = item.getBackground(columns);
					if (swtColor != null && !swtColor.equals(item.getBackground())) {
						cellColour[columns][items] = new Color(swtColor.getRed(), swtColor.getGreen(), swtColor.getBlue());
					}
					Image im = item.getImage(columns);
					if (im != null && !im.isDisposed()) {
						ImageLoader loader = new ImageLoader();
						loader.data = new ImageData[] { im.getImageData() };
						ByteArrayOutputStream png = new ByteArrayOutputStream();
						loader.save(png, SWT.IMAGE_PNG);
						images.put("image_" + items + "_" + columns + ".png", png.toByteArray());
					}
				}
			}
			gridX.setGridValues(values);
			gridX.setFontObject(fontObject);
			gridX.setCellWidth(cellWidth);
			gridX.setCellHeight(cellHeight);
			gridX.setCellColor(cellColour);
			org.eclipse.swt.graphics.Color co = grid.getLineColor();
			gridX.setGridLineColor(co != null ? new Color(co.getRed(), co.getGreen(), co.getBlue()) : new Color(255, 255, 255));
		});
		if (gridX.getGridValues() == null) {
			return;
		}
		try {
			/* The archive in memory, then the file at once (no half written file) */
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
				zip.putNextEntry(new ZipEntry("sheet.xml"));
				Writer w = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
				new XStream().toXML(gridX, w);
				w.flush();
				zip.closeEntry();
				for (Map.Entry<String, byte[]> e : images.entrySet()) {
					zip.putNextEntry(new ZipEntry(e.getKey()));
					zip.write(e.getValue());
					zip.closeEntry();
				}
			}
			Path target = Path.of(save);
			Path temp = Files.createTempFile("bio7", ".bgjar");
			Files.write(temp, bytes.toByteArray());
			Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			Bio7Dialog.message("Cannot write the file:\n" + e.getMessage());
		}
	}
}
