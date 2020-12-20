package com.eco.bio7.floweditor.actions;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.floweditor.shapes.ShapesEditor;

public class GridSettings extends Dialog {

	protected Object result;
	protected Shell shell;
	public Spinner spinner;

	/**
	 * Create the dialog.
	 * 
	 * @param parent
	 * @param style
	 */
	public GridSettings(Shell parent, int style) {
		super(parent, style);
		setText("Settings");
	}

	/**
	 * Open the dialog.
	 * 
	 * @return the result
	 */
	public Object open() {
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	private void createContents() {
		shell = new Shell(getParent(), SWT.SHELL_TRIM);
		shell.setSize(164, 92);
		shell.setText(getText());

		spinner = new Spinner(shell, SWT.BORDER);
		spinner.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int value = spinner.getSelection();
				IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
				ShapesEditor shapeeditor = (ShapesEditor) editor;

				shapeeditor.getviewer().setProperty(SnapToGrid.PROPERTY_GRID_SPACING, new Dimension(value, value));

			}
		});
		spinner.setMaximum(1000);
		spinner.setMinimum(1);
		spinner.setSelection(5);
		spinner.setBounds(10, 10, 138, 21);

	}
}
