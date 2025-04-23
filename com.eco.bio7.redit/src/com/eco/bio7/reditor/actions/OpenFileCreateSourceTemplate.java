/*******************************************************************************
 * Copyright (c) 2005-2017 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.reditor.actions;

import java.io.File;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IWorkbenchWindow;
import com.eco.bio7.util.Util;

public class OpenFileCreateSourceTemplate {
	private IWorkbenchWindow window;
	private String extension = null;
	private String filePath = null;

	public String getExtension() {
		return extension;
	}

	public String getFilePath() {
		return filePath;
	}

	public OpenFileCreateSourceTemplate(IDocument doc, int offset, int length) {

		FileDialog fd = new FileDialog(Util.getShell(), SWT.MULTI);
		fd.setText("Open");

		/* For multiple extensions for one filetype, semicolon can be used! */
		String[] filterExt = { "*.*", "*.RData", "*.txt", "*.csv", "*.xls", "*.xlsx", "*.json", "*.xml", "*.sav",
				"*.dta", "*.syd", "*.arff", "*.mat", "*.mtp", "*.dbf", "*.tif;*.tiff;*.jpg;*.jpeg;*.TIFF;*.png","*.shp","*.Rhistory" };
		fd.setFilterExtensions(filterExt);

		String selected = fd.open();

		if (fd.getFileNames().length > 1) {

			String[] names = fd.getFileNames();
			StringBuffer buf = new StringBuffer();
			buf.append("dataFiles <- c(");
			for (int i = 0, n = names.length; i < n; i++) {

				if (i < n - 1) {
					buf.append("\"");
					buf.append(fd.getFilterPath().replace("\\", "/"));
					buf.append("/");
					buf.append(names[i]);
					buf.append("\"");
					buf.append(",");
				} else {
					buf.append("\"");
					buf.append(fd.getFilterPath().replace("\\", "/"));
					buf.append("/");
					buf.append(names[i]);
					buf.append("\"");
				}
			}
			buf.append(")");
			try {
				doc.replace(offset, length, buf.toString());
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		else {

			if (selected != null) {
				selected = selected.replace("\\", "/");
				String fileNameTemp = FilenameUtils.removeExtension(fd.getFileName());
				extension = "*."+FilenameUtils.getExtension(fd.getFileName());
				String fileWithoutExt = fd.getFilterPath() + File.separator + fileNameTemp;
				fileWithoutExt = fileWithoutExt.replace("\\", "/");
				//String dirPath = fd.getFilterPath().replace("\\", "/");
				;
				try {
					//int selFilter = fd.getFilterIndex();
					if (extension.equals(filterExt[0])) {
						doc.replace(offset, length, selected);
					} 
					
					else if (extension.equals(filterExt[1])) {
						doc.replace(offset, length, "load(\"" + selected + "\")");

					}

					else if (extension.equals(filterExt[2])) {
						doc.replace(offset, length, "dataTable <- read.table(\"" + selected + "\",header = FALSE)");

					} else if (extension.equals(filterExt[3])) {
						doc.replace(offset, length, "dataCsv <- read.csv(\"" + selected + "\",header = FALSE)");

					} else if (extension.equals(filterExt[4])) {
						doc.replace(offset, length, "library(XLConnect);\ndataExcel <- readWorksheetFromFile(\""
								+ selected + "\",sheet=1)");
					} else if (extension.equals(filterExt[5])) {
						doc.replace(offset, length,
								"library(XLConnect);dataExcel <- readWorksheetFromFile(\"" + selected + "\",sheet=1)");
					} else if (extension.equals(filterExt[6])) {
						doc.replace(offset, length, "library(rjson);dataJson <- fromJSON(file =\"" + selected + "\")");

					} else if (extension.equals(filterExt[7])) {
						doc.replace(offset, length, "library(XML);dataXml <- xmlTreeParse(\"" + selected + "\")");

					} else if (extension.equals(filterExt[8])) {
						doc.replace(offset, length, "library(foreign);dataSpss <- read.spss(\"" + selected
								+ "\",to.data.frame=TRUE,use.value.labels=FALSE)");
					} else if (extension.equals(filterExt[9])) {
						doc.replace(offset, length, "library(foreign);dataStata <- read.dta(\"" + selected + "\")");

					} else if (extension.equals(filterExt[10])) {
						doc.replace(offset, length,
								"library(foreign);dataSystat <- read.sysstat(\"" + selected + "\")");
					} else if (extension.equals(filterExt[11])) {
						doc.replace(offset, length, "library(foreign);dataWeka <- read.arff(\"" + selected + "\")");

					} else if (extension.equals(filterExt[12])) {
						doc.replace(offset, length, "library(foreign);dataOctave <- read.octave(\"" + selected + "\")");

					} else if (extension.equals(filterExt[13])) {
						doc.replace(offset, length, "library(foreign);dataFile <- read.mtp(\"" + selected + "\")");

					} else if (extension.equals(filterExt[14])) {
						doc.replace(offset, length,
								"library(foreign);dataDbf <- read.dbf(\"" + selected + "\",as.is = FALSE)");
					/*Multi display in dialog array : filterExt[15]*/	
					} else if (extension.equals("*.tif")||extension.equals("*.tiff")||extension.equals("*.TIFF")||extension.equals("*.jpg")||extension.equals("*.png")||extension.equals("*.jpeg")) {
						doc.replace(offset, length, "library(rgdal);dataGdal <- readGDAL(\"" + selected + "\")");

					} else if (extension.equals(filterExt[16])) {
						doc.replace(offset, length, "library(rgdal);dataGdal <- readOGR(\"" + selected + "\")");

					} else if (extension.equals(filterExt[17])) {
						extension = "*.Rhistory";
						filePath = selected;
					}
					else {
						return;
					}

				} catch (BadLocationException e) {

					e.printStackTrace();
				}

			}
		}

	}
}