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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.zip.util.*;
import com.thoughtworks.xstream.XStream;

public class SaveGridXMLJob extends WorkspaceJob {

	protected String file;
	protected Grid grid;
	private String[][] values;
	private GridXMLFontObject[][] fontObject;
	private int[][] cellWidth;
	private int[][] cellHeight;
	private Color[][] cellColour;
	private String path;
	private ArrayList<String> list = new ArrayList<String>();
	private org.eclipse.swt.graphics.Color co;

	public SaveGridXMLJob(String file, String path, Grid grid) {
		super("Save as Bio7 XML (*.xml)");
		this.file = file;
		this.grid = grid;
		this.path = path;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Save Bio7 XML", IProgressMonitor.UNKNOWN);

		if (monitor.isCanceled()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {

			}
		}

		saveXml(file);
		monitor.done();
		return Status.OK_STATUS;

	}

	private void saveXml(String save) {

		if (grid != null) {

			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					co = grid.getLineColor();
					int columnCount = grid.getColumnCount();
					int itemCount = grid.getItemCount();

					values = new String[columnCount][itemCount];
					fontObject = new GridXMLFontObject[columnCount][itemCount];
					cellHeight = new int[columnCount][itemCount];
					cellWidth = new int[columnCount][itemCount];
					cellColour = new Color[columnCount][itemCount];

					for (int items = 0; items < itemCount; items++) {

						for (int columns = 0; columns < columnCount; columns++) {

							String s = grid.getItem(items).getText(columns);
							values[columns][items] = s;
							Font f = grid.getItem(items).getFont(columns);
							GridXMLFontObject fo = null;
							if (f != null) {
								FontData[] fontData = f.getFontData();

								fo = new GridXMLFontObject();

								fo.setHeight(fontData[0].getHeight());
								fo.setStyle(fontData[0].getStyle());
								fo.setLocale(fontData[0].getLocale());
								fo.setName(fontData[0].getName());
							}
							fontObject[columns][items] = fo;

							cellHeight[columns][items] = grid.getItem(items).getHeight();
							cellWidth[columns][items] = grid.getColumn(columns).getWidth();

							org.eclipse.swt.graphics.Color swtColor = grid.getItem(items).getBackground(columns);
							if (swtColor != null) {

								cellColour[columns][items] = new Color(swtColor.getRed(), swtColor.getGreen(), swtColor.getBlue());
							} else {
								cellColour[columns][items] = new Color(255, 255, 255);
							}
							Image im = grid.getItem(items).getImage(columns);

							if (im != null) {
								/* Generate unique prefix! */
								String prefix = String.valueOf(UUID.randomUUID());
								ImageLoader loader = new ImageLoader();
								loader.data = new ImageData[] { im.getImageData() };
								String p = path + "/" + prefix + "_" + items + "_" + columns + ".png";
								loader.save(p, SWT.IMAGE_PNG);
								list.add(p);

							}

						}
					}
				}
			});

		}

		XStream xstream = new XStream();
		xstream.allowTypesByRegExp(new String[] { ".*" });
		GridXmlObject gridX = new GridXmlObject();
		gridX.setGridValues(values);
		gridX.setFontObject(fontObject);
		gridX.setCellWidth(cellWidth);
		gridX.setCellHeight(cellHeight);
		gridX.setCellColor(cellColour);

		if (co != null) {

			gridX.setGridLineColor(new Color(co.getRed(), co.getGreen(), co.getBlue()));
		} else {

			gridX.setGridLineColor(new Color(255, 255, 255));

		}

		FileWriter fs = null;
		/* Generate unique prefix! */
		String prefix = String.valueOf(UUID.randomUUID());
		String p = path + "/" + prefix + ".xml";
		try {
			fs = new FileWriter(p);
			list.add(p);
		} catch (IOException e) {

			e.printStackTrace();
		}
		xstream.toXML(gridX, fs);
		try {
			fs.close();
		} catch (IOException e1) {

			e1.printStackTrace();
		}

		String[] ar = (String[]) list.toArray(new String[list.size()]);
		list.clear();
		try {
			FolderZiper.zipFolder(ar, path, file);
		} catch (Exception e) {

			e.printStackTrace();
		}
		ar = null;

	}

}
