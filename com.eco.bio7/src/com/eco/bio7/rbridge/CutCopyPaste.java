package com.eco.bio7.rbridge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

public class CutCopyPaste {

	private Clipboard cb = new Clipboard(Display.getDefault());

	// CopyImagesGrid cig=new CopyImagesGrid();

	public void cut() {

		copy();
		delete("Cut");

	}

	public void copyImageToClipboard() {
		Grid grid = RTable.getGrid();
		Point[] selection = grid.getCellSelection();
		Image im = null;
		if (grid.getItem(selection[0].y).getImage(selection[0].x) != null) {

			im = grid.getItem(selection[0].y).getImage(selection[0].x);
			// CopyImagesGrid.getImageList().add(new
			// ClipboardImageObject(selection.x,selection.y,im));

		} else {
			im = null;
		}
		if (im != null) {
			ImageTransfer imageTransfer = ImageTransfer.getInstance();
			cb.setContents(new Object[] { im.getImageData() }, new Transfer[] { imageTransfer });
		}

	}

	public void pasteImageFromClipboard() {
		Grid grid = RTable.getGrid();
		ImageData imageData = (ImageData) cb.getContents(ImageTransfer.getInstance());
		int plusC = Spread.getSelectedColumnMin();
		int plusR = Spread.getSelectedRowMin();
		if (imageData != null && grid != null && plusR < grid.getItemCount() && plusC < grid.getColumnCount()) {
			Image im = new Image(Display.getDefault(), imageData);
			/* The image changes the size of the row and the column: a snapshot step! */
			GridHistory.of(grid).recordGrid("Paste Image", () -> {
				grid.getItem(plusR).setImage(plusC, im);
				grid.getItem(plusR).setHeight(imageData.height);
				grid.getColumn(plusC).setWidth(imageData.width);
			});

		}
	}

	public void copy() {
		Grid grid = RTable.getGrid();
		StringBuffer str = new StringBuffer();
		Image im = null;
		if (grid != null) {

			Point[] sel = grid.getCellSelection();

			if (sel == null) {
				return;
			}
			if (sel.length <= 0) {
				return;
			}

			int temp = sel[0].y;
			// int columncount = 0;// Temp variable for columncounts!
			// storecount = 0;// A counter for the amount of columns!
			for (Point selection : sel) {

				/*
				 * Linebreak in data because y data is higher than the current
				 * row etc.
				 */

				if (selection.y > temp) {

					/* Delete space at the end of a line */
					str.deleteCharAt(str.length() - 1);
					str.append(System.getProperty("line.separator"));
					/* Temp gets the value of the current row! */
					temp = selection.y;
				}

				if (grid.getItem(selection.y).getText(selection.x).equals("")) {

					str.append(" ");
				} else {
					str.append(grid.getItem(selection.y).getText(selection.x));
				}

				str.append("\t");// Empty space!
				/*
				 * if (grid.getItem(selection.y).getImage(selection.x) != null)
				 * {
				 * 
				 * im = grid.getItem(selection.y).getImage(selection.x);
				 * //CopyImagesGrid.getImageList().add(new
				 * ClipboardImageObject(selection.x,selection.y,im));
				 * 
				 * } else { im = null; }
				 */

			}
			/* Delete space at the end of the last line again */
			str.deleteCharAt(str.length() - 1);
			str.append(System.getProperty("line.separator"));
			String textData = str.toString();
			TextTransfer textTransfer = TextTransfer.getInstance();
			// ImageTransfer imageTransfer = ImageTransfer.getInstance();

			/*
			 * if (im != null) { cb.setContents(new Object[] { textData,
			 * im.getImageData() }, new Transfer[] { textTransfer, imageTransfer
			 * }); } else {
			 */
			cb.setContents(new Object[] { textData }, new Transfer[] { textTransfer });
			// }
		}

	}

	public void paste() {

		Grid grid = RTable.getGrid();

		if (grid != null) {
			TextTransfer transfer = TextTransfer.getInstance();

			String data = (String) cb.getContents(transfer);

			if (data == null) {
				data = " ";
			}
			StringTokenizer st1 = new StringTokenizer(data, System.getProperty("line.separator"));
			int plusC = Spread.getSelectedColumnMin();
			int plusR = Spread.getSelectedRowMin();
			/* The pasted cells (x = column, y = row) and their values! */
			List<Point> cells = new ArrayList<Point>();
			List<String> values = new ArrayList<String>();
			boolean grows = false;
			for (int i = 0; st1.hasMoreTokens(); i++) {
				StringTokenizer st2 = new StringTokenizer(st1.nextToken(), "\t");
				for (int j = 0; st2.hasMoreTokens(); j++) {
					Point cell = new Point(j + plusC, i + plusR);
					cells.add(cell);
					values.add(st2.nextToken());
					if (cell.y >= grid.getItemCount() || cell.x >= grid.getColumnCount()) {
						grows = true;
					}
				}
			}
			Runnable paste = () -> {
				for (int k = 0; k < cells.size(); k++) {
					Point cell = cells.get(k);
					/* Create rows or columns if the grid is too small! */
					while (cell.y >= grid.getItemCount()) {
						Bio7Grid.createRow(grid.getItemCount(), grid.getItemHeight());
					}
					while (cell.x >= grid.getColumnCount()) {
						Bio7Grid.createColumn(grid.getColumnCount(), 50, "C" + (grid.getColumnCount() + 1));
					}
					grid.getItem(cell.y).setText(cell.x, values.get(k));
				}
			};
			/* Only the pasted cells are stored, a snapshot if the grid grows! */
			if (grows) {
				GridHistory.of(grid).recordGrid("Paste", paste);
			} else {
				GridHistory.of(grid).recordCells("Paste", cells, paste);
			}

		}

	}

	public void delete() {
		delete("Delete");
	}

	private void delete(String label) {
		Grid grid = RTable.getGrid();

		if (grid != null) {
			Point[] sel = grid.getCellSelection();
			if (sel == null || sel.length == 0) {
				return;
			}
			GridHistory.of(grid).recordCells(label, Arrays.asList(sel), () -> {
				for (Point selection : sel) {
					grid.getItem(selection.y).setText(selection.x, "");
					/* Only if there is an image: the first setImage call of a grid computes the row heights again! */
					if (grid.getItem(selection.y).getImage(selection.x) != null) {
						grid.getItem(selection.y).setImage(selection.x, null);
					}
				}
			});

		}

	}

}
