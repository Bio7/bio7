/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rbridge;

import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * This class gives access to the Table component (based on the Nebula Grid
 * Widget) of Bio7.
 * 
 * @author Bio7
 * 
 */
public class Bio7Grid {

	private static Grid grid;
	private static String value;
	private static String[][] values;
	private static int colCount;
	private static int itemCount;
	protected static GridItem[] items;
	private static String[] dataVector;

	/**
	 * Returns the current Bio7 table component for a custom sheet.
	 * 
	 * @return a grid component.
	 */
	public static Grid getGrid() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				grid = RTable.getGrid();

			}
		});

		return grid;
	}
	
	/**
	 * Creates a new grid and fills the grid with the given 2d array.
	 * 
	 * @param val
	 *            a 2d-array of type String.
	 * @param name the name of the grid.        
	 */
	public static void setValues(final String[][] val,final String name) {
		final Grid grid = RTable.getGrid();
		if (grid != null) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					Grid grid = RTable.getGrid();
					if (grid != null) {
						grid = new Spread().spread(RTable.getTabFolder(), val[0].length, val.length, name);
						RTable.setGrid(grid);
					}
					for (int i = 0; i < val.length; i++) {
						for (int j = 0; j < val[0].length; j++) {
							grid.getItem(i).setText(j, val[i][j]);
						}
					}
				}
			});
		}
	}

	/**
	 * Creates a new sheet from the specified columns, rows.
	 * 
	 * @param columns
	 *            the amount of columns.
	 * @param rows
	 *            the amount of rows.
	 * @param name
	 *            the name of the sheet.
	 */
	public static void createSheet(final int columns, final int rows, final String name) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				Grid grid = RTable.getGrid();
				if (grid != null) {
					grid = new Spread().spread(RTable.getTabFolder(), columns, rows, name);
					RTable.setGrid(grid);
				}
			}
		});
	}

	/**
	 * Sets the values in the current active sheet.
	 * 
	 * @param val
	 *            a 2d-array of type String.
	 */
	public static void setValues(final String[][] val) {
		final Grid grid = RTable.getGrid();
		if (grid != null) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					for (int i = 0; i < val.length; i++) {
						for (int j = 0; j < val[0].length; j++) {
							grid.getItem(i).setText(j, val[i][j]);
						}
					}
				}
			});
		}
	}

	/**
	 * Returns the values of the current active sheet.
	 * 
	 * @return the sheet values of type String.
	 */
	public static String[][] getValues() {
		final Grid grid = RTable.getGrid();
		if (grid != null) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					values = new String[grid.getItemCount()][grid.getColumnCount()];
					for (int i = 0; i < grid.getItemCount(); i++) {
						for (int j = 0; j < grid.getColumnCount(); j++) {
							values[i][j] = grid.getItem(i).getText(j);
						}
					}
				}
			});
		}
		return values;
	}

	/**
	 * Set the value at the specified item (row, column).
	 * 
	 * @param row
	 *            the row.
	 * @param column
	 *            the column.
	 * @param val
	 *            the value of type String
	 */
	public static void setValue(final int row, final int column, final String val) {
		final Grid grid = RTable.getGrid();

		if (grid != null) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					grid.getItem(row).setText(column, val);
				}
			});
		}
	}

	/**
	 * Returns the sheet value from the specified item (row, column).
	 * 
	 * @param row
	 *            the row index.
	 * @param column
	 *            the column index.
	 * @return the value.
	 */
	public static String getValue(final int row, final int column) {
		final Grid grid = RTable.getGrid();
		if (grid != null) {

			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					value = grid.getItem(row).getText(column);

				}
			});
		}
		return value;

	}

	/**
	 * Returns the amount of columns.
	 * 
	 * @return an integer value.
	 */
	public static int getColumnCount() {
		final Grid grid = RTable.getGrid();
		if (grid != null) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					colCount = grid.getColumnCount();

				}
			});
		}
		return colCount;

	}

	/**
	 * Returns the amount of rows.
	 * 
	 * @return an integer value.
	 */
	public static int getItemCount() {

		final Grid grid = RTable.getGrid();
		if (grid != null) {

			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					itemCount = grid.getItemCount();

				}
			});
		}
		return itemCount;
	}

	/**
	 * Sets the background colors in the current active sheet.
	 * 
	 * @param row
	 *            the row
	 * @param column
	 *            the column
	 * @param r
	 *            r the red component.
	 * @param g
	 *            g the green component.
	 * @param b
	 *            b the blue component.
	 * 
	 */
	public static void setColor(final int row, final int column, final int r, final int g, final int b) {
		final Grid grid = RTable.getGrid();
		if (grid != null) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
					String colorAsString="RGB {"+r+", "+g+", "+b+"}";
					Color col=colorRegistry.get(colorAsString);
					if(col==null){
						colorRegistry.put(colorAsString, new RGB(r,g,b));
					}
					grid.getItem(row).setBackground(column, col);

				}
			});
		}
	}

	/**
	 * Returns the selection as a String array of the active sheet.
	 * 
	 * @return a String array.
	 */
	public static String[] getSelection() {
		final Grid grid = RTable.getGrid();
		if (grid != null) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					Point[] sel = grid.getCellSelection();

					dataVector = new String[sel.length];

					for (int i = 0; i < sel.length; i++) {

						dataVector[i] = grid.getItem(sel[i].y).getText(sel[i].x);

					}
				}
			});
		}
		return dataVector;
	}

	/**
	 * Creates a row in the current grid.
	 * 
	 * @param index
	 *            the index of the row.
	 * @param height
	 *            the height of the row.
	 */
	public static void createRow(final int index, final int height) {
		final Grid grid = RTable.getGrid();
		if (grid != null) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					new GridItem(grid, SWT.NONE, index).setHeight(height);
					grid.layout();
				}
			});
		}

	}

	/**
	 * Creates a column in the current grid.
	 * 
	 * @param index
	 *            the index for the column.
	 * @param width
	 *            the width of the column.
	 * @param titel
	 *            the title for the column.
	 * 
	 */

	public static void createColumn(final int index, final int width, final String titel) {
		final Grid grid = RTable.getGrid();
		if (grid != null) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					GridColumn column = null;
					column = new GridColumn(grid, SWT.NONE, grid.getColumnCount());
					column.setWidth(width);
					column.setText(titel);

				}
			});
		}

	}

	/**
	 * Creates an image in the selected grid cell.
	 * 
	 * @param row
	 *            the row.
	 * @param column
	 *            the column.
	 * @param image
	 *            the SWT image.
	 */
	public static void setImage(final int row, final int column, final Image image) {
		final Grid grid = RTable.getGrid();
		if (grid != null) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					GridItem item = grid.getItem(row);
					item.setImage(column, image);

				}
			});
		}

	}

	private static ImageData convertToSWT(BufferedImage bufferedImage) {
		if (bufferedImage.getColorModel() instanceof DirectColorModel) {
			DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
			PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(), colorModel.getBlueMask());
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[3];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
					data.setPixel(x, y, pixel);
				}
			}
			return data;
		} else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
			IndexColorModel colorModel = (IndexColorModel) bufferedImage.getColorModel();
			int size = colorModel.getMapSize();
			byte[] reds = new byte[size];
			byte[] greens = new byte[size];
			byte[] blues = new byte[size];
			colorModel.getReds(reds);
			colorModel.getGreens(greens);
			colorModel.getBlues(blues);
			RGB[] rgbs = new RGB[size];
			for (int i = 0; i < rgbs.length; i++) {
				rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
			}
			PaletteData palette = new PaletteData(rgbs);
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);
			data.transparentPixel = colorModel.getTransparentPixel();
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					data.setPixel(x, y, pixelArray[0]);
				}
			}
			return data;
		}
		return null;
	}

	/**
	 * Creates a SWT image from a Java BufferedImage.
	 * 
	 * @param bufferedImage
	 *            the image.
	 * @return returns a SWT image.
	 */
	public static Image toSWTImage(BufferedImage bufferedImage) {

		return new Image(Display.getCurrent(), convertToSWT(bufferedImage));
	}

}
