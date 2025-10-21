/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
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

package com.eco.bio7.worldwind;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwindx.examples.util.ExampleUtil;
import worldw.Activator;
public class LayerCompositeShapefile extends Composite {

	private int number;
	private Sector secto;
	

	/**
	 * Create the composite
	 * @param parent
	 * @param style
	 * @param sect 
	 */
	public LayerCompositeShapefile(Composite parent, int style,final Layer layer, final Sector sect) {
		super(parent, style);
		this.setBackground(parent.getBackground());
		this.secto=sect;
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		setLayout(new GridLayout(5, true));
		final Button b = new Button(this, SWT.CHECK);
		b.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		b.setText(layer.getName());
		b.setToolTipText(layer.getName());
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
		scale.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		scale.setMinimum(1);
		scale.setMaximum(100);
		scale.setSelection(100);
		scale.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				layer.setOpacity(((double) scale.getSelection()) / 100);
				WorldWindView.getWwd().redraw();
			}
		});
		

		
		final Button r = new Button(this, SWT.NONE);
		GridData gd_r = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_r.heightHint = 50;
		r.setLayoutData(gd_r);
		//r.setText("X");
		r.setImage(Activator.getImageDescriptor("/pics/deleteaction.png").createImage());
		r.setSelection(true);
		r.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				
				LayerList layers = WorldWindView.getWwd().getModel().getLayers();
				layers.remove(layer);
				
				dispose();
				WorldWindOptionsView.getOptionsInstance().computeScrolledSize();
				WorldWindView.getWwd().redraw();

			}
		});
		final Button goTo = new Button(this, SWT.NONE);
		GridData gd_goTo = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_goTo.heightHint = 50;
		goTo.setLayoutData(gd_goTo);
		//goTo.setText("Loc");
		goTo.setImage(Activator.getImageDescriptor("/pics/worldwindpersp.png").createImage());
		goTo.setToolTipText("Move to location");
		goTo.setSelection(true);
		goTo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (secto != null) {
					WorldWindOptionsView.setMinLat(String.valueOf(secto.getMinLatitude().degrees));
					WorldWindOptionsView.setMinLon(String.valueOf(secto.getMinLongitude().degrees));
					WorldWindOptionsView.setMaxLat(String.valueOf(secto.getMaxLatitude().degrees));
					WorldWindOptionsView.setMaxLon(String.valueOf(secto.getMaxLongitude().degrees));
					ExampleUtil.goTo(WorldWindView.getWwd(), secto);
				}

				else {
					MessageBox messageBox = new MessageBox(new Shell(),

							SWT.ICON_WARNING);
							messageBox.setText("Info!");
							messageBox.setMessage("No georeference available!");
							messageBox.open();

				}

				WorldWindOptionsView.getOptionsInstance().computeScrolledSize();
				WorldWindView.getWwd().redraw();

			}
		});
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);
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
