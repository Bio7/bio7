/*******************************************************************************
 * Copyright (c) 2004-2018 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.init;

import java.util.ArrayList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.discrete.Hexagon;
import com.eco.bio7.discrete.StateButtonView;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.image.PointPanel;
import com.eco.bio7.methods.CurrentStates;
import com.eco.bio7.plot.LineChartView;
import com.eco.bio7.plot.PieChartView;

public class ButtonContainer {

	private ArrayList<Button> buttcont = new ArrayList<Button>();

	private Button butt;

	private int stateSel;

	private int re;

	private String pic;

	private Composite top = null;

	private Display dis;

	public static ButtonContainer buttoncontainer;

	public boolean disposed = false;

	public ButtonContainer(Composite top) {
		buttcont.clear();// Delete all Buttons!
		buttoncontainer = this;// Set the reference!
		disposed = false;

		this.top = top;
		this.dis = top.getDisplay();
		for (int i = 0; i < CurrentStates.getStateList().size(); i++) {
			String plant = (String) CurrentStates.getStateList().get(i);
			addButton(plant);

		}

	}

	public void addButton(final String plant) { // Button in a ArrayList...
		if (!disposed) {
			pic = plant;
			stateSel = CurrentStates.getIndexStateName(plant);

			dis.syncExec(new Runnable() {
				// Important to wrap Swing Event!
				public void run() {

					buttcont.add(new Button(top, SWT.FLAT));
					butt = (Button) buttcont.get(stateSel);

					butt.setBounds(new org.eclipse.swt.graphics.Rectangle(8, 10, 38, 23));
					butt.setData(new String(pic));// Get the Name of the
													// species !

					Image im;
					try {
						im = Bio7Plugin.getImageDescriptor("/icons/" + pic + ".png").createImage();
						butt.setImage(im);
						butt.setText(plant);
						butt.setToolTipText(plant);
					} catch (RuntimeException e1) {
						butt.setText(plant);
						butt.setToolTipText(plant);
						//im = new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/state.png"));
						im = Bio7Plugin.getImageDescriptor("/icons/views/state.png").createImage();
						
						butt.setImage(im);

					}

					top.layout(true);
					butt.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
						public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
							String in = ((String) (e.widget.getData()));
							stateSel = CurrentStates.getIndexStateName(in);
							Shell shell = new Shell();
							ColorDialog dialog = new ColorDialog(shell, SWT.APPLICATION_MODAL);

							int[] co = CurrentStates.getRGB(stateSel);

							dialog.setRGB(new RGB(co[0], co[1], co[2]));

							RGB color = dialog.open();
							if (color != null) {

								CurrentStates.getR().set(stateSel, Integer.valueOf(color.red));
								CurrentStates.getG().set(stateSel, Integer.valueOf(color.green));
								CurrentStates.getB().set(stateSel, Integer.valueOf(color.blue));
								Quad2d quad = Quad2d.getQuad2dInstance();
								quad.setValue(stateSel);
								Hexagon hex = Hexagon.getHexagonInstance();
								if (hex != null) {
									hex.value = stateSel;
								}
								LineChartView.linechart.renderer();
								/* Change the colour of the linechart! */
								quad.repaint();

								if (hex != null) {
									hex.repaint();
								}
								PieChartView.getPiechart().plotter();
								/* Change the colour of the piechart! */
								PointPanel.setPlantIndexPanel(stateSel);
								/*
								 * Set the selected plant for spatial analysis !
								 */

							}

						}

					});
					Point p = butt.getLocation();

					StateButtonView.getSc().setMinSize(top.computeSize(200, p.y + 50));
					/* Resize the scrolled composite! */

				}

			});

		}
	}

	public void removeButton(String in) {
		re = CurrentStates.getIndexStateName(in);

		dis.syncExec(new Runnable() { // important to wrap Swing Event!
			public void run() {

				if (buttcont != null && buttcont.isEmpty() == false) {

					butt = (Button) buttcont.get(re);
                    Image im=butt.getImage();
                    if(im!=null){
                    	im.dispose();
                    }
					butt.dispose();// Removes the widget!
					buttcont.remove(re);// Remove Button from Vector!

					top.layout(true);// Important to set the Layout!

					if (buttcont.size() > 1) {
						butt = (Button) buttcont.get(buttcont.size() - 1);
						Point p = butt.getLocation();
						StateButtonView.getSc().setMinSize(top.computeSize(200, p.y + 50));
						/* Resize the scrolled composite! */
					}

				}

			}
		});
	}

	public static ButtonContainer getinstance() {
		return buttoncontainer;
	}

	public ArrayList getButtcont() {
		return buttcont;
	}

}
