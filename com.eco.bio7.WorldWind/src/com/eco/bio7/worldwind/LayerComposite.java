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

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;

import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwindx.examples.util.ExampleUtil;

public class LayerComposite extends Composite {

	private int number;
	private Sector sector;

	/**
	 * Create the composite
	 * 
	 * @param parent
	 * @param style
	 */
	public LayerComposite(Composite parent, int style, final SurfaceImage si, final RenderableLayer layerImages, final Sector sector) {
		super(parent, style);
		this.sector = sector;
		
		final Scale scale = new Scale(this, SWT.NONE);
		scale.setMinimum(1);
		scale.setMaximum(100);
		scale.setSelection(100);
		scale.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				si.setOpacity(((double) scale.getSelection()) / 100);
				WorldWindView.getWwd().redraw();
			}
		});
		scale.setBounds(78, 0, 120, 49);

		final Button r = new Button(this, SWT.NONE);
		//r.setText("X");
		r.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/delete.gif")));
		r.setSelection(true);
		r.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				LayerList layers = WorldWindView.getWwd().getModel().getLayers();
				layers.remove(layerImages);

				dispose();
				WorldWindOptionsView.computeScrolledSize();
				WorldWindView.getWwd().redraw();

			}
		});
		r.setBounds(204, 12, 41, 25);
		final Button goTo = new Button(this, SWT.NONE);
		goTo.setText("Loc");
		goTo.setSelection(true);
		goTo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (sector != null) {
					WorldWindOptionsView.setMinLat(String.valueOf(sector.getMinLatitude().degrees));
					WorldWindOptionsView.setMinLon(String.valueOf(sector.getMinLongitude().degrees));
					WorldWindOptionsView.setMaxLat(String.valueOf(sector.getMaxLatitude().degrees));
					WorldWindOptionsView.setMaxLon(String.valueOf(sector.getMaxLongitude().degrees));
					ExampleUtil.goTo(WorldWindView.getWwd(), sector);
				}

				else {
					MessageBox messageBox = new MessageBox(new Shell(),

							SWT.ICON_WARNING);
							messageBox.setText("Info!");
							messageBox.setMessage("No georeference available!");
							messageBox.open();

				}

				WorldWindOptionsView.computeScrolledSize();
				WorldWindView.getWwd().redraw();

			}
		});
		goTo.setBounds(244, 12, 41, 25);
		final Button b = new Button(this, SWT.CHECK);
		b.setText(layerImages.getName());
		b.setToolTipText(layerImages.getName());
		b.setSelection(true);
		b.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				if (b.getSelection()) {
					layerImages.setEnabled(true);
					WorldWindView.getWwd().redraw();

				} else {
					layerImages.setEnabled(false);
					WorldWindView.getWwd().redraw();
				}

				WorldWindView.getWwd().redraw();
			}
		});
		b.setBounds(10, 4, 68, 40);
		//
	}

	public void addComponents(final SurfaceImage si) {

	}

	public void setListNumber(int number) {
		this.number = number;
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

}
