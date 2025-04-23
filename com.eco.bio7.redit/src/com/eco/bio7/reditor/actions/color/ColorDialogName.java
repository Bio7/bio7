package com.eco.bio7.reditor.actions.color;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import com.eco.bio7.reditor.control.RColors;

/*This class is used in the RColorInformationControl! Only defined as a command!*/
public class ColorDialogName extends Dialog {
	private Table table;
	private String colorName;
	private String selColorName;
	private Color colors[] = new Color[RColors.colorNames.length];

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 * @param selColorName
	 */
	public ColorDialogName(Shell parentShell, String selColorName) {
		super(parentShell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		this.selColorName = selColorName;
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);

		table = new Table(container, SWT.BORDER | SWT.FULL_SELECTION);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		TableColumn tblclmnNewColumn = new TableColumn(table, SWT.NONE);
		tblclmnNewColumn.setWidth(450);
		tblclmnNewColumn.setText("Color Name");
		int numSel = 0;
		String[] names = RColors.colorNames;

		for (int i = 0; i < names.length; i++) {
			TableItem tableItem = new TableItem(table, SWT.NONE);
			tableItem.setText(names[i]);
			Color col = hex2Rgb(parent.getDisplay(), RColors.hexValues[i]);
			colors[i] = col;
			tableItem.setBackground(col);
			if (selColorName != null) {
				if (selColorName.equals(names[i])) {
					numSel = i;
					table.showItem(tableItem);
				}
			}
		}

		table.select(numSel);

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button button = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);

		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 500);
	}

	public Color hex2Rgb(Display dis, String colorStr) {
		return new Color(dis, Integer.valueOf(colorStr.substring(1, 3), 16), Integer.valueOf(colorStr.substring(3, 5), 16), Integer.valueOf(colorStr.substring(5, 7), 16));
	}

	@Override
	protected void okPressed() {
		int selected = table.getSelectionIndex();
		colorName = RColors.colorNames[selected];
		/*Dispose all colors!*/
		for (int i = 0; i < colors.length; i++) {
			colors[i].dispose();
		}
		super.okPressed();
	}

	public String getColorByName() {
		return colorName;
	}

}
