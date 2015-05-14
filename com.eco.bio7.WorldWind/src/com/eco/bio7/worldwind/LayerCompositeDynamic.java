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

package com.eco.bio7.worldwind;

import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Scale;

import com.eco.bio7.worldwind.DynamicLayer;
import com.eco.bio7.worldwind.SurfaceImage;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
public class LayerCompositeDynamic extends Composite {

	private int number;

	/**
	 * Create the composite
	 * @param parent
	 * @param style
	 */
	public LayerCompositeDynamic(Composite parent, int style,final RenderableLayer layer) {
		super(parent, style);
		/*Important to set the layout data for this composite to scale relative to the parent!*/
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		setLayout(new GridLayout(5, true));
		final Button b = new Button(this, SWT.CHECK);
		b.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		b.setText("Dynamic");
		b.setSelection(true);
		b.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				if (b.getSelection()) {
					layer.setEnabled(true);
					WorldWindView.getWwd().redraw();

				} else {
					layer.setEnabled(false);
					WorldWindView.getWwd().redraw();
				}

				WorldWindView.getWwd().redraw();
			}
		});
		final Scale scale = new Scale(this, SWT.NONE);
		GridData gd_scale = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_scale.heightHint = 43;
		scale.setLayoutData(gd_scale);
		scale.setMinimum(1);
		scale.setMaximum(1000);
		scale.setSelection(1000);
		scale.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				DynamicLayer.setPeriod(scale.getSelection());
				
			}
		});
				

				
				final Button r = new Button(this, SWT.NONE);
				GridData gd_r = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
				gd_r.heightHint = 50;
				r.setLayoutData(gd_r);
				//r.setText("X");
				r.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/delete.gif")));
				r.setSelection(true);
				r.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(final SelectionEvent e) {
						
						WorldWindowGLCanvas wwd = WorldWindView.getWwd();
						if(wwd!=null){
						LayerList layers = wwd.getModel().getLayers();
						layers.remove(layer);
						
						dispose();
						WorldWindOptionsView.optionsInstance.computeScrolledSize();
						
						
						
						wwd.redraw();
						}

					}
				});
		
				final Button setupButton = new Button(this, SWT.NONE);
				GridData gd_setupButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
				gd_setupButton.heightHint = 50;
				setupButton.setLayoutData(gd_setupButton);
				setupButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(final SelectionEvent e) {
						DynamicLayer.invokeSetup();
					}
				});
				setupButton.setText("Setup");
		//
	}
	
	public void addComponents(final SurfaceImage si){
		
	}
	public void setListNumber(int number){
		this.number=number;
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

}
