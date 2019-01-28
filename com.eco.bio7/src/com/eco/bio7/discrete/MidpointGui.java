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

package com.eco.bio7.discrete;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.methods.CurrentStates;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.util.Util;

public class MidpointGui extends Shell {

	private Text text_3;
	private MidpointGui shell;

	/**
	 * Launch the application
	 * 
	 * @param args
	 */
	public MidpointGui() {

		/* Remove the if clause to edit in the WindowBuilder!!!!! */
		Display display = Display.getDefault();
		if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
			shell = new MidpointGui(display, SWT.SHELL_TRIM | SWT.TOOL
					| SWT.ON_TOP);
		} else {
			shell = new MidpointGui(display, SWT.SHELL_TRIM);

		}
		shell.open();
		shell.layout();

	}

	private Spinner spinner;
	private Text text;
	private Text text_1;
	private Combo combo;
	private Text text_2;
	private Button createImageButton;
	private Spinner spinner_1;
	protected boolean accepted;
	protected String[] textWeights;
	private Label fractalDimensionLabel;
	private Label randomAdditionsLabel;
	private Label seedLabel;
	private Button okButton;
	private Button cancelButton;
	private Label lblScale;
	private Button shuffleButton;
	private Label standardDeviationLabel;

	/**
	 * Create the shell
	 * 
	 * @param display
	 * @param style
	 */
	public MidpointGui(Display display, int style) {
		super(display, style);
		createContents();
	}

	/**
	 * Create contents of the window
	 */
	protected void createContents() {
		this.setBackground(Util.getSWTBackgroundColor());
		setText("Midpoint 2D");
		setSize(417, 493);
		setLayout(new GridLayout(3, true));
		spinner = new Spinner(this, SWT.BORDER);
		GridData gd_spinner = new GridData(SWT.FILL, SWT.FILL, true, false, 1,
				1);
		gd_spinner.heightHint = 25;
		spinner.setLayoutData(gd_spinner);
		spinner.setToolTipText("Maximal number of recursions (n = 2 ^ maxlevel)");
		spinner.setMaximum(12);
		spinner.setSelection(6);

		Label sizeLabel;
		sizeLabel = new Label(this, SWT.NONE);
		GridData gd_sizeLabel = new GridData(SWT.FILL, SWT.FILL, false, false,
				1, 1);
		gd_sizeLabel.heightHint = 30;
		sizeLabel.setLayoutData(gd_sizeLabel);
		sizeLabel.setText("Maxlevel");
		new Label(this, SWT.NONE);

		text = new Text(this, SWT.BORDER);
		GridData gd_text = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_text.heightHint = 25;
		text.setLayoutData(gd_text);
		text.setToolTipText("Initial standard deviation");
		text.setText("1.0");
		text.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {

				try {
					value = new Double(text.getText());
					text.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
					accepted = true;

				} catch (Exception e) {
					text.setForeground(new Color(Display.getCurrent(), 255, 0,
							0));

					accepted = false;
				}
			}

		});
		standardDeviationLabel = new Label(this, SWT.NONE);
		standardDeviationLabel.setText("Standard Deviation");

		Label standardDeviationLabel;
		new Label(this, SWT.NONE);

		text_1 = new Text(this, SWT.BORDER);
		GridData gd_text_1 = new GridData(SWT.FILL, SWT.FILL, false, false, 1,
				1);
		gd_text_1.heightHint = 25;
		text_1.setLayoutData(gd_text_1);
		text_1.setToolTipText("Parameter that determines fractal dimension (dd = 3 - h)");
		text_1.setText("1.0");
		text_1.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {

				try {
					value = new Double(text_1.getText());
					text_1.setForeground(new Color(Display.getCurrent(), 0, 0,
							0));
					accepted = true;

				} catch (Exception e) {
					text_1.setForeground(new Color(Display.getCurrent(), 255,
							0, 0));

					accepted = false;
				}
			}

		});

		fractalDimensionLabel = new Label(this, SWT.NONE);
		fractalDimensionLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
				false, false, 1, 1));
		fractalDimensionLabel.setText("Fractal Dimension");
		new Label(this, SWT.NONE);

		combo = new Combo(this, SWT.NONE);
		GridData gd_combo = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_combo.heightHint = 25;
		combo.setLayoutData(gd_combo);
		combo.setToolTipText("Turns on/off random additions");
		combo.select(0);

		combo.setItems(new String[] { "True", "False" });
		combo.select(0);

		randomAdditionsLabel = new Label(this, SWT.NONE);
		randomAdditionsLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL,
				false, false, 1, 1));
		randomAdditionsLabel.setText("Random additions");
		new Label(this, SWT.NONE);
		text_2 = new Text(this, SWT.BORDER);
		GridData gd_text_2 = new GridData(SWT.FILL, SWT.FILL, false, false, 1,
				1);
		gd_text_2.heightHint = 25;
		text_2.setLayoutData(gd_text_2);
		text_2.setToolTipText("Seed value for random number generator");
		text_2.setText("-9999");
		text_2.addListener(SWT.Modify, new Listener() {
			private Integer value;

			public void handleEvent(Event event) {

				try {
					value = new Integer(text_2.getText());
					text_2.setForeground(new Color(Display.getCurrent(), 0, 0,
							0));
					accepted = true;

				} catch (Exception e) {
					text_2.setForeground(new Color(Display.getCurrent(), 255,
							0, 0));

					accepted = false;
				}
			}

		});

		seedLabel = new Label(this, SWT.NONE);
		seedLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false,
				false, 1, 1));
		seedLabel.setText("Seed");

		shuffleButton = new Button(this, SWT.NONE);
		GridData gd_shuffleButton = new GridData(SWT.FILL, SWT.FILL, false,
				false, 1, 1);
		gd_shuffleButton.heightHint = 35;
		shuffleButton.setLayoutData(gd_shuffleButton);
		shuffleButton.setToolTipText("Creates a random seed!");
		shuffleButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				text_2.setText("" + (int) (Math.random() * 100000000));
			}
		});
		shuffleButton.setText("Shuffle");

		text_3 = new Text(this, SWT.BORDER);
		GridData gd_text_3 = new GridData(SWT.FILL, SWT.FILL, false, false, 2,
				1);
		gd_text_3.heightHint = 25;
		text_3.setLayoutData(gd_text_3);
		text_3.setToolTipText("Weighted states!");
		text_3.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {
				textWeights = text_3.getText().split(",");

				for (int i = 0; i < textWeights.length; i++) {
					try {
						value = new Double(textWeights[i]);
						text_3.setForeground(new Color(Display.getCurrent(), 0,
								0, 0));
						accepted = true;

					} catch (Exception e) {
						text_3.setForeground(new Color(Display.getCurrent(),
								255, 0, 0));

						accepted = false;
					}
				}

			}

		});
		StringBuffer buff = applyWeights();
		text_3.setText(buff.toString());
		new Label(this, SWT.NONE);
		{
			Button btnCreateWeights = new Button(this, SWT.NONE);
			GridData gd_btnCreateWeights = new GridData(SWT.FILL, SWT.FILL,
					false, false, 1, 1);
			gd_btnCreateWeights.heightHint = 35;
			btnCreateWeights.setLayoutData(gd_btnCreateWeights);
			btnCreateWeights.setToolTipText("Creates a weight for each state!");
			btnCreateWeights.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					StringBuffer buff = applyWeights();
					text_3.setText(buff.toString());

				}
			});
			btnCreateWeights.setText("Create Weights");
		}
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);

		createImageButton = new Button(this, SWT.CHECK);
		GridData gd_createImageButton = new GridData(SWT.FILL, SWT.FILL, false,
				false, 1, 1);
		gd_createImageButton.heightHint = 30;
		createImageButton.setLayoutData(gd_createImageButton);
		createImageButton.setToolTipText("Creates a fractal image in ImageJ");
		createImageButton.setText("Create Image");

		spinner_1 = new Spinner(this, SWT.BORDER);
		GridData gd_spinner_1 = new GridData(SWT.FILL, SWT.FILL, false, false,
				1, 1);
		gd_spinner_1.heightHint = 25;
		spinner_1.setLayoutData(gd_spinner_1);
		spinner_1.setToolTipText("Values of the image*scale!");
		spinner_1.setMinimum(-10000);
		spinner_1.setMaximum(10000);
		spinner_1.setSelection(100);
		{
			lblScale = new Label(this, SWT.NONE);
			lblScale.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false,
					false, 1, 1));
			lblScale.setText("Scale");
		}
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);

		okButton = new Button(this, SWT.NONE);
		GridData gd_okButton = new GridData(SWT.FILL, SWT.FILL, false, false,
				1, 1);
		gd_okButton.heightHint = 35;
		okButton.setLayoutData(gd_okButton);
		okButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int po = spinner.getSelection();
				double sigma = Double.parseDouble(text.getText());
				double fractal = Double.parseDouble(text_1.getText());
				int selected = combo.getSelectionIndex();

				int seed = Integer.parseInt(text_2.getText());
				int scale = spinner_1.getSelection();
				String[] t = text_3.getText().split(",");
				boolean createImage = createImageButton.getSelection();
				if (createImage == false) {

					if (t.length == CurrentStates.getStateList().size()) {

						if (t.length > 1
								&& CurrentStates.getStateList().size() > 1) {
							final int pow = (int) Math.pow(2, po);
							/*
							 * Adjust the size of the discrete grids! Stop the
							 * animator of the Quad3D if active for the resize
							 * event!
							 */
							/*
							 * FPSAnimator an = Quad3dview.getAnimator(); if (an != null) { an.stop(); }
							 */
							/* Adjust the fieldsize of the different grids! */
							Field.setSize(pow, pow);
							CreateMidpointJob job = new CreateMidpointJob(po,
									sigma, fractal, selected, seed, scale, t,
									createImage);
							// job.setSystem(true);
							job.schedule();
							/* Restart the animator */
							/*
							 * if (an != null) { an.start(); }
							 */
						} else {
							Bio7Dialog
									.message("At least one State and two weights\nhave to be selected!");
						}
					} else {

						Bio7Dialog
								.message("To many or few weights selected!\nPlease create weights!");

					}
				} else {
					CreateMidpointJob job = new CreateMidpointJob(po, sigma,
							fractal, selected, seed, scale, t, createImage);
					// job.setSystem(true);
					job.schedule();

				}

			}
		});
		okButton.setText("Ok");

		cancelButton = new Button(this, SWT.NONE);
		GridData gd_cancelButton = new GridData(SWT.FILL, SWT.FILL, false,
				false, 1, 1);
		gd_cancelButton.heightHint = 35;
		cancelButton.setLayoutData(gd_cancelButton);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				dispose();
			}
		});
		cancelButton.setText("Cancel");
		new Label(this, SWT.NONE);

	}

	private StringBuffer applyWeights() {
		double si = CurrentStates.getStateList().size();
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < si; i++) {
			if (i == 0) {
				buff.append("" + 1.0 / si);
			} else {
				buff.append("," + 1.0 / si);
			}
		}
		return buff;
	}

	//

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}
}
