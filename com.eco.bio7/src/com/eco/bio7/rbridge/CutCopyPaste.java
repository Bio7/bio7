package com.eco.bio7.rbridge;

import java.util.ArrayList;
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
		delete();

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
		if (imageData != null) {
			Image im = new Image(Display.getDefault(), imageData);
			grid.getItem(plusR).setImage(plusC, im);
			grid.getItem(plusR).setHeight(imageData.height);
			grid.getColumn(plusC).setWidth(imageData.width);

		}
	}

	public void copy() {
		Grid grid = RTable.getGrid();
		StringBuffer str = new StringBuffer();
		Image im = null;
		if (grid != null) {

			Point[] sel = grid.getCellSelection();

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

			// ImageData imageData = (ImageData)
			// cb.getContents(ImageTransfer.getInstance());

			String data = (String) cb.getContents(transfer);

			String rowstring;
			String value;

			if (data == null) {
				data = " ";
			}
			StringTokenizer st1 = new StringTokenizer(data, System.getProperty("line.separator"));
			int plusC = Spread.getSelectedColumnMin();
			int plusR = Spread.getSelectedRowMin();

			for (int i = 0; st1.hasMoreTokens(); i++) {
				rowstring = st1.nextToken();
				StringTokenizer st2 = new StringTokenizer(rowstring, "\t");
				for (int j = 0; st2.hasMoreTokens(); j++) {
					value = (String) st2.nextToken();
					// System.out.println("y:"+i+" x:"+j+" value:"+value);
					int row = i + plusR;
					int column = j + plusC;
					if (row < grid.getItemCount() && column < grid.getColumnCount()) {
						grid.getItem(row).setText(column, value);
					}

					/*
					 * if(CopyImagesGrid.getImageList().size()>0){ Image
					 * imFromList=CopyImagesGrid.getImageList().get(i);
					 * grid.getItem(i + plusR).setImage(j + plusC, imFromList);
					 * grid.getItem(i +
					 * plusR).setHeight(imFromList.getImageData().height);
					 * grid.getColumn(j +
					 * plusC).setWidth(imFromList.getImageData().width); }
					 */
				}
			}
			// System.out.println(CopyImagesGrid.getImageList().size());

			/*
			 * ArrayList<ClipboardImageObject> arl=
			 * CopyImagesGrid.getImageList(); ClipboardImageObject
			 * co=arl.get(0); int x1=co.x; int y1=co.y; for (int i = 0; i <
			 * CopyImagesGrid.getImageList().size(); i++) {
			 * 
			 * 
			 * ClipboardImageObject co2=arl.get(i);
			 * grid.getItem(co2.y-y1+plusR).setImage(co2.x-x1+plusC,co2.im); }
			 */

		}
		// CopyImagesGrid.getImageList().clear();
	}

	public void delete() {
		Grid grid = RTable.getGrid();

		if (grid != null) {
			Point[] sel = grid.getCellSelection();

			int temp = sel[0].y;
			for (Point selection : sel) {

				grid.getItem(selection.y);
				grid.getColumn(selection.x);
				if (selection.y > temp) {

					temp = selection.y;
				}

				grid.getItem(selection.y).setText(selection.x, "");
				grid.getItem(selection.y).setImage(selection.x, null);

			}

		}

	}

}
