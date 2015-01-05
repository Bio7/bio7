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
import com.eco.bio7.discrete3d.Quad3dview;
import com.eco.bio7.methods.CurrentStates;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.jogamp.opengl.util.FPSAnimator;

public class MidpointGui extends Shell {

	private Text text_3;
	private MidpointGui shell;

	/**
	 * Launch the application
	 * 
	 * @param args
	 */
	public MidpointGui() {
		try {
			Display display = Display.getDefault();
			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
				shell = new MidpointGui(display, SWT.SHELL_TRIM | SWT.TOOL | SWT.ON_TOP);
			} else {
				shell = new MidpointGui(display, SWT.SHELL_TRIM);

			}
			shell.open();
			shell.layout();

		} catch (Exception e) {
			e.printStackTrace();
		}
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
		setText("Midpoint 2D");
		setSize(244, 377);
		spinner = new Spinner(this, SWT.BORDER);
		spinner.setToolTipText("Maximal number of recursions (n = 2 ^ maxlevel)");
		spinner.setMaximum(12);
		spinner.setSelection(6);
		spinner.setBounds(11, 13, 84, 22);

		Label sizeLabel;
		sizeLabel = new Label(this, SWT.NONE);
		sizeLabel.setBounds(119, 16, 103, 15);
		sizeLabel.setText("Maxlevel");

		text = new Text(this, SWT.BORDER);
		text.setToolTipText("Initial standard deviation");
		text.setBounds(11, 43, 84, 25);
		text.setText("1.0");
		text.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {

				try {
					value = new Double(text.getText());
					text.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
					accepted = true;

				} catch (Exception e) {
					text.setForeground(new Color(Display.getCurrent(), 255, 0, 0));

					accepted = false;
				}
			}

		});

		Label standardDeviationLabel;
		standardDeviationLabel = new Label(this, SWT.NONE);
		standardDeviationLabel.setBounds(119, 46, 100, 15);
		standardDeviationLabel.setText("Standard Deviation");

		text_1 = new Text(this, SWT.BORDER);
		text_1.setToolTipText("Parameter that determines fractal dimension (dd = 3 - h)");
		text_1.setBounds(11, 76, 84, 25);
		text_1.setText("1.0");
		text_1.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {

				try {
					value = new Double(text_1.getText());
					text_1.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
					accepted = true;

				} catch (Exception e) {
					text_1.setForeground(new Color(Display.getCurrent(), 255, 0, 0));

					accepted = false;
				}
			}

		});

		combo = new Combo(this, SWT.NONE);
		combo.setToolTipText("Turns on/off random additions");
		combo.select(0);
		combo.setBounds(11, 109, 84, 23);

		combo.setItems(new String[] { "True", "False" });
		combo.select(0);
		text_2 = new Text(this, SWT.BORDER);
		text_2.setToolTipText("Seed value for random number generator");
		text_2.setBounds(11, 140, 84, 25);
		text_2.setText("-9999");
		text_2.addListener(SWT.Modify, new Listener() {
			private Integer value;

			public void handleEvent(Event event) {

				try {
					value = new Integer(text_2.getText());
					text_2.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
					accepted = true;

				} catch (Exception e) {
					text_2.setForeground(new Color(Display.getCurrent(), 255, 0, 0));

					accepted = false;
				}
			}

		});

		Label fractalDimensionLabel;
		fractalDimensionLabel = new Label(this, SWT.NONE);
		fractalDimensionLabel.setBounds(119, 79, 100, 15);
		fractalDimensionLabel.setText("Fractal Dimension");

		Label randomAdditionsLabel;
		randomAdditionsLabel = new Label(this, SWT.NONE);
		randomAdditionsLabel.setBounds(119, 109, 100, 15);
		randomAdditionsLabel.setText("Random additions");

		Label seedLabel;
		seedLabel = new Label(this, SWT.NONE);
		seedLabel.setBounds(101, 143, 39, 15);
		seedLabel.setText("Seed");

		final Button okButton = new Button(this, SWT.NONE);
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

						if (t.length > 1 && CurrentStates.getStateList().size() > 1) {
							final int pow = (int) Math.pow(2, po);
							/*
							 * Adjust the size of the discrete grids! Stop the
							 * animator of the Quad3D if active for the resize
							 * event!
							 */
							FPSAnimator an = Quad3dview.getAnimator();
							if (an != null) {
								an.stop();
							}
							/* Adjust the fieldsize of the different grids! */
							Field.setSize(pow, pow);
							CreateMidpointJob job = new CreateMidpointJob(po, sigma, fractal, selected, seed, scale, t, createImage);
							// job.setSystem(true);
							job.schedule();
							/* Restart the animator */
							if (an != null) {
								an.start();
							}
						} else {
							Bio7Dialog.message("At least one State and two weights\nhave to be selected!");
						}
					} else {

						Bio7Dialog.message("To many or few weights selected!\nPlease create weights!");

					}
				} else {
					CreateMidpointJob job = new CreateMidpointJob(po, sigma, fractal, selected, seed, scale, t, createImage);
					// job.setSystem(true);
					job.schedule();

				}

			}
		});
		okButton.setText("Ok");
		okButton.setBounds(11, 279, 84, 25);

		final Button cancelButton = new Button(this, SWT.NONE);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				dispose();
			}
		});
		cancelButton.setText("Cancel");
		cancelButton.setBounds(119, 279, 80, 25);

		text_3 = new Text(this, SWT.BORDER);
		text_3.setToolTipText("Weighted states!");
		text_3.setBounds(11, 171, 211, 25);
		text_3.addListener(SWT.Modify, new Listener() {
			private Double value;

			public void handleEvent(Event event) {
				textWeights = text_3.getText().split(",");

				for (int i = 0; i < textWeights.length; i++) {
					try {
						value = new Double(textWeights[i]);
						text_3.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
						accepted = true;

					} catch (Exception e) {
						text_3.setForeground(new Color(Display.getCurrent(), 255, 0, 0));

						accepted = false;
					}
				}

			}

		});
		StringBuffer buff = applyWeights();
		text_3.setText(buff.toString());

		createImageButton = new Button(this, SWT.CHECK);
		createImageButton.setToolTipText("Creates a fractal image in ImageJ");
		createImageButton.setText("Create Image");
		createImageButton.setBounds(11, 246, 93, 16);

		spinner_1 = new Spinner(this, SWT.BORDER);
		spinner_1.setToolTipText("Values of the image*scale!");
		spinner_1.setMinimum(-10000);
		spinner_1.setMaximum(10000);
		spinner_1.setSelection(100);
		spinner_1.setBounds(119, 245, 84, 20);
		{
			Button btnCreateWeights = new Button(this, SWT.NONE);
			btnCreateWeights.setToolTipText("Creates a weight for each state!");
			btnCreateWeights.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					StringBuffer buff = applyWeights();
					text_3.setText(buff.toString());

				}
			});
			btnCreateWeights.setBounds(11, 202, 84, 25);
			btnCreateWeights.setText("Create Weights");
		}
		{
			Label lblScale = new Label(this, SWT.NONE);
			lblScale.setBounds(209, 248, 39, 18);
			lblScale.setText("Scale");
		}

		final Button shuffleButton = new Button(this, SWT.NONE);
		shuffleButton.setToolTipText("Creates a random seed!");
		shuffleButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				text_2.setText("" + (int) (Math.random() * 100000000));
			}
		});
		shuffleButton.setText("Shuffle");
		shuffleButton.setBounds(146, 136, 46, 27);

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
