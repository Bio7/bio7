package com.eco.bio7.image;

import java.awt.Rectangle;

import javax.swing.SwingUtilities;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.process.ImageProcessor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.ui.PlatformUI;

public class MatchingDialoge extends Dialog {
	private Scale scale_1;
	private Text text;
	private Text text_1;
	private Scale scale;
	private Combo combo;
	private Button btnPointSelection;
	protected ImagePlus imp;
	protected ImagePlus impSel;
	ImageProcessor ip;
	ImageProcessor ipSel;
	protected int T_rows;
	protected int T_cols;
	protected int VALUE_MAX;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public MatchingDialoge(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.SHELL_TRIM | SWT.TOOL | SWT.ON_TOP);
		
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new FormLayout());
		{
			scale_1 = new Scale(container, SWT.NONE);
			scale_1.setMaximum(255);
			{
				FormData formData = new FormData();
				formData.top = new FormAttachment(0, 53);
				formData.left = new FormAttachment(0, 10);
				scale_1.setLayoutData(formData);
			}
		}
		{

			scale = new Scale(container, SWT.NONE);
			scale.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent e) {
					text_1.setText("" + scale.getSelection());
				}
			});
			scale.setMaximum(255);
			
			{
				FormData formData = new FormData();
				formData.top = new FormAttachment(scale_1, 6);
				formData.left = new FormAttachment(scale_1, 0, SWT.LEFT);
				scale.setLayoutData(formData);
			}
		}
		{
			text = new Text(container, SWT.BORDER);
			{
				FormData formData = new FormData();
				formData.width = 105;
				formData.top = new FormAttachment(0, 65);
				formData.left = new FormAttachment(scale_1, 6);
				text.setLayoutData(formData);
			}
		}
		{
			text_1 = new Text(container, SWT.BORDER);
			{
				FormData formData = new FormData();
				formData.right = new FormAttachment(text, 0, SWT.RIGHT);
				formData.top = new FormAttachment(text, 29);
				formData.left = new FormAttachment(scale, 6);
				text_1.setLayoutData(formData);
			}
		}
		{
			Button btnMatch = new Button(container, SWT.NONE);
			btnMatch.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {

					ImageMatchTemplateJob job = new ImageMatchTemplateJob(combo, scale);
					//job.setSystem(true);
					job.schedule();

				}
			});
			{
				FormData formData = new FormData();
				formData.bottom = new FormAttachment(100, -10);
				formData.left = new FormAttachment(scale_1, 0, SWT.LEFT);
				formData.height = 29;
				formData.width = 170;
				btnMatch.setLayoutData(formData);
			}
			btnMatch.setText("Match");
		}
		{
			btnPointSelection = new Button(container, SWT.CHECK);
			{
				FormData formData = new FormData();
				formData.top = new FormAttachment(text_1, 20);
				formData.left = new FormAttachment(text, 0, SWT.LEFT);
				btnPointSelection.setLayoutData(formData);
			}
			btnPointSelection.setSelection(true);
			btnPointSelection.setText("Create Point Selection");
		}
		{
			combo = new Combo(container, SWT.NONE);
			combo.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent e) {
					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {

						public void run() {
							imp = WindowManager.getCurrentWindow().getImagePlus();
							impSel = WindowManager.getImage(combo.getText());
							
						}
					});
					ip = imp.getProcessor();
					ipSel = impSel.getProcessor();
					T_rows = impSel.getHeight();
					T_cols = impSel.getWidth();

					VALUE_MAX = T_rows * T_cols * 255;
					scale.setMinimum(0);
					scale.setMaximum(VALUE_MAX);
				}
			});
			{
				FormData formData = new FormData();
				formData.right = new FormAttachment(scale_1, 0, SWT.RIGHT);
				formData.width = 170;
				formData.top = new FormAttachment(0, 10);
				formData.left = new FormAttachment(scale_1, 0, SWT.LEFT);
				combo.setLayoutData(formData);

			}
		}
		{
			Label lblTemplate = new Label(container, SWT.NONE);
			{
				FormData formData = new FormData();
				formData.right = new FormAttachment(text, 0, SWT.RIGHT);
				formData.bottom = new FormAttachment(combo, 0, SWT.BOTTOM);
				formData.width = 68;
				formData.top = new FormAttachment(combo, 0, SWT.TOP);
				formData.left = new FormAttachment(combo, 16);
				lblTemplate.setLayoutData(formData);
			}
			lblTemplate.setText("Image Template");
		}
		{
			Label lblMin = new Label(container, SWT.NONE);
			{
				FormData formData = new FormData();
				formData.width = 39;
				formData.height = 19;
				formData.top = new FormAttachment(text, 0, SWT.TOP);
				formData.left = new FormAttachment(text, 6);
				lblMin.setLayoutData(formData);
			}
			lblMin.setText("Min");
		}
		{
			Label lblMax = new Label(container, SWT.NONE);
			{
				FormData formData = new FormData();
				formData.right = new FormAttachment(0, 405);
				formData.width = 54;
				formData.height = 19;
				formData.top = new FormAttachment(text_1, 0, SWT.TOP);
				formData.left = new FormAttachment(text_1, 6);
				lblMax.setLayoutData(formData);
			}
			lblMax.setText("Ignore x<");
		}
		CTabItem[] items = CanvasView.getCanvas_view().tabFolder.getItems();
		for (int i = 0; i < items.length; i++) {

			combo.add(items[i].getText(), i);

		}
		

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}

}
