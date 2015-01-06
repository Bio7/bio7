/*******************************************************************************
 * Copyright (c) 2007-2014 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rbridge;

import java.awt.Color;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.zip.util.ZipUtil;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class LoadGridXMLJob extends WorkspaceJob {

	protected String file;
	protected Grid grid;
	private GridXmlObject gridXml;
	private String[][] values;
	public boolean debugOn = false;
	private String[] names;
	private GridXMLFontObject[][] fonts;
	private int[][] cellHeights;
	private int[][] cellWidths;
	private Color[][] colors;
	private GridXMLFontObject gxml;
	private FontData fontData = new FontData();
	private FontRegistry fontRegistry;
	private Display display;

	public LoadGridXMLJob(String files) {
		super("Load Bio7 Jar");
		this.file = files;
		try {
			names = new ZipUtil().processFile(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		display = Display.getCurrent();

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Load Bio7 Jar", IProgressMonitor.UNKNOWN);

		if (monitor.isCanceled()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {

			}
		}
		/* First we load the *.xml file to create the sized grid! */
		for (int i = 0; i < names.length; i++) {
			String[] sp = names[i].split("\\.");

			if (sp[1].equals("xml")) {

				loadXml((new File(file).getParent()) + "/" + names[i]);
			}

		}

		/* Now we load the images! */
		for (int i = 0; i < names.length; i++) {
			String[] sp = names[i].split("\\.");

			if (sp[1].equals("png")) {

				String[] coords = sp[0].split("_");
				/* UUID prefix is at [0]! */
				int row = Integer.parseInt(coords[1]);
				int column = Integer.parseInt(coords[2]);
				loadImage(row, column, (new File(file).getParent()) + "/" + names[i]);
			}
		}

		monitor.done();
		return Status.OK_STATUS;
	}

	private void loadXml(final String fileXml) {

		XStream xs = new XStream(new DomDriver());
		xs.setClassLoader(com.eco.bio7.jobs.DataDescriptorGrids.class.getClassLoader());

		try {
			gridXml = (GridXmlObject) xs.fromXML(new FileReader(fileXml));
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		values = gridXml.getGridValues();
		fonts = gridXml.getFontObject();
		cellHeights = gridXml.getCellHeight();
		cellWidths = gridXml.getCellWidth();
		colors = gridXml.getCellColor();

		final File fil = new File(fileXml);

		if (fil.exists()) {

			display.syncExec(new Runnable() {

				public void run() {
					fontRegistry = new FontRegistry(display);
					grid = new Spread().spread(RTable.getTabFolder(), values.length, values[0].length, new File(file).getName());
					Color co = gridXml.getGridLineColor();
					ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
					String colorAsString="RGB {"+co.getRed()+", "+co.getGreen()+", "+co.getBlue()+"}";
					org.eclipse.swt.graphics.Color col=colorRegistry.get(colorAsString);
					if(col==null){
						colorRegistry.put(colorAsString, new org.eclipse.swt.graphics.RGB(co.getRed(),co.getGreen(),co.getBlue()));
					}
					grid.setLineColor(colorRegistry.get(colorAsString));
					if (grid != null) {
						RTable.setGrid(grid);
						for (int items = 0; items < grid.getItemCount(); items++) {

							for (int columns = 0; columns < grid.getColumnCount(); columns++) {

								GridItem item = grid.getItem(items);
								item.setText(columns, values[columns][items]);
								gxml = fonts[columns][items];
								if (gxml != null) {
									fontData.setHeight(gxml.getHeight());
									fontData.setLocale(gxml.getLocale());
									fontData.setName(gxml.getName());
									fontData.setStyle(gxml.getStyle());
									fontRegistry.put("c", new FontData[] { new FontData(gxml.getName(), gxml.getHeight(), gxml.getStyle()) });

									item.setFont(columns, fontRegistry.get("c"));
								}
								Color cob=colors[columns][items];
								int r=cob.getRed();
								int g=cob.getGreen();
								int b=cob.getBlue();
								String colBackgrString="RGB {"+r+", "+g+", "+b+"}";
								org.eclipse.swt.graphics.Color colBack=colorRegistry.get(colBackgrString);
								if(colBack==null){
									colorRegistry.put(colBackgrString, new RGB(r, g, b));
								}
								item.setBackground(columns,colorRegistry.get(colBackgrString));
								item.setHeight(cellHeights[columns][items]);

								grid.getColumn(columns).setWidth(cellWidths[columns][items]);

							}
						}

					}
				}

			});

		} else {

			Bio7Dialog.message("File not found!");
		}
		boolean success = new File(fileXml).delete();

	}

	private void loadImage(final int row, final int column, final String file) {

		if (grid != null) {
			final Image image = new Image(Display.getCurrent(), file);

			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {

					grid.getItem(row).setImage(column, image);

				}

			});

		}
		boolean success = new File(file).delete();
	}

}
