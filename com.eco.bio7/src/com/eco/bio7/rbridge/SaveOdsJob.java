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

package com.eco.bio7.rbridge;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.batch.Bio7Dialog;

/**
 * Exports all sheets of the Bio7 table as a spreadsheet of LibreOffice (*.ods,
 * see OdsFile): numbers as numbers, the other cells as texts, with the colors
 * and fonts of the cells, the images and the widths of the columns and the
 * heights of the rows.
 */
public class SaveOdsJob extends WorkspaceJob {

	private final String file;

	public SaveOdsJob(String file) {
		super("Export as LibreOffice (ODS)");
		this.file = file;
	}

	/*
	 * The format of a cell: the colors and the font which differ from the defaults
	 * of the row (the colors of the theme are not exported)
	 */
	private static OdsFile.Format format(GridItem row, int column) {
		int background = -1, foreground = -1;
		Color bg = row.getBackground(column), fg = row.getForeground(column);
		if (bg != null && !bg.equals(row.getBackground())) {
			background = rgb(bg);
		}
		if (fg != null && !fg.equals(row.getForeground())) {
			foreground = rgb(fg);
		}
		String name = null;
		double size = 0;
		boolean bold = false, italic = false;
		Font font = row.getFont(column);
		if (font != null && !font.isDisposed() && !font.equals(row.getFont())) {
			FontData fd = font.getFontData()[0];
			name = fd.getName();
			size = fd.getHeight();
			bold = (fd.getStyle() & SWT.BOLD) != 0;
			italic = (fd.getStyle() & SWT.ITALIC) != 0;
		}
		OdsFile.Format f = new OdsFile.Format(background, foreground, name, size, bold, italic);
		return f.isDefault() ? null : f;
	}

	private static int rgb(Color c) {
		return (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
	}

	/* An image as PNG */
	private static byte[] png(ImageData data) {
		ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] { data };
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		loader.save(out, SWT.IMAGE_PNG);
		return out.toByteArray();
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Export as LibreOffice (ODS)", IProgressMonitor.UNKNOWN);
		try {
			List<OdsFile.Sheet> sheets = new ArrayList<>();
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(() -> {
				CTabFolder tabFolder = RTable.getTabFolder();
				if (tabFolder == null) {
					return;
				}
				for (CTabItem item : tabFolder.getItems()) {
					Control control = item.getControl();
					if (!(control instanceof Composite) || ((Composite) control).getChildren().length == 0
							|| !(((Composite) control).getChildren()[0] instanceof Grid)) {
						continue;
					}
					Grid grid = (Grid) ((Composite) control).getChildren()[0];
					/* Pixels of the screen in points (macOS: 72 dpi, the pixels are points) */
					double points = 72.0 / Math.max(1, grid.getDisplay().getDPI().x);
					int rows = grid.getItemCount(), columns = grid.getColumnCount();
					String[][] cells = new String[rows][columns];
					OdsFile.Format[][] formats = new OdsFile.Format[rows][columns];
					OdsFile.Picture[][] pictures = new OdsFile.Picture[rows][columns];
					double[] widths = new double[columns], heights = new double[rows];
					for (int j = 0; j < columns; j++) {
						widths[j] = grid.getColumn(j).getWidth() * points;
					}
					for (int i = 0; i < rows; i++) {
						GridItem row = grid.getItem(i);
						heights[i] = row.getHeight() * points;
						for (int j = 0; j < columns; j++) {
							cells[i][j] = row.getText(j);
							formats[i][j] = format(row, j);
							Image image = row.getImage(j);
							if (image != null && !image.isDisposed()) {
								ImageData data = image.getImageData();
								pictures[i][j] = new OdsFile.Picture(png(data), data.width * points, data.height * points);
							}
						}
					}
					sheets.add(new OdsFile.Sheet(item.getText(), cells, formats, pictures, widths, heights));
				}
			});
			if (sheets.isEmpty()) {
				Bio7Dialog.message("No sheet to export!");
				return Status.OK_STATUS;
			}
			try {
				OdsFile.write(new File(file), sheets);
			} catch (IOException e) {
				Bio7Dialog.message("Cannot write the file (probably opened by another application?):\n" + e.getMessage());
			}
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}
}
