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
package com.eco.bio7.reditor.actions;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import com.eco.bio7.util.Util;

public class SaveFileCreateSourceTemplate {

	private String filePath;
	private String extension;
	private String var;

	public String getExtension() {
		return extension;
	}

	public String getFilePath() {
		return filePath;
	}

	public SaveFileCreateSourceTemplate(IDocument doc, int offset, int length,String rShellVar) {
		if(rShellVar!=null) {
			var=rShellVar;
		}
		else {
			var="";
		}
		FileDialog fd = new FileDialog(Util.getShell(), SWT.SAVE);
		fd.setText("Save");

		/* For multiple extensions for one filetype, semicolon can be used! */
		String[] filterExt = { "*.*", "*.RData", "*.txt", "*.csv", "*.xls", "*.xlsx", "*.sas", "*.xml", "*.sav",
				"*.dta", "*.syd", "*.arff", "*.mat", "*.dbf", "*.geotiff", "*.shapefile", "*.Rhistory" };
		fd.setFilterExtensions(filterExt);
		String selected = fd.open();
		if (selected != null) {
			selected = selected.replace("\\", "/");
			String fileNameTemp = FilenameUtils.removeExtension(fd.getFileName());
			extension = "*." + FilenameUtils.getExtension(fd.getFileName());
			String fileWithoutExt = fd.getFilterPath() + File.separator + fileNameTemp;
			fileWithoutExt = fileWithoutExt.replace("\\", "/");
			String dirPath = fd.getFilterPath().replace("\\", "/");
           
			try {
				//int selFilter = fd.getFilterIndex();

				if (extension.equals(filterExt[0])) {
					doc.replace(offset, length, selected);
				}

				else if (extension.equals(filterExt[1])) {
					doc.replace(offset, length, "save.image(file=\"" + selected + "\")");

				}

				else if (extension.equals(filterExt[2])) {
					doc.replace(offset, length, "write("+var+",file=\"" + selected + "\")");

				} else if (extension.equals(filterExt[3])) {
					doc.replace(offset, length, "write.csv("+var+",file=\"" + selected + "\",row.names = FALSE)");

				} else if (extension.equals(filterExt[4])) {
					doc.replace(offset, length, "library(XLConnect);saveWorkbook("+var+",\"" + selected + "\")");

				} else if (extension.equals(filterExt[5])) {
					doc.replace(offset, length,
							"library(XLConnect);writeWorksheetToFile(\"" + selected + "\",data="+var+",sheet = \"mySheet\")");

				} else if (extension.equals(filterExt[6])) {
					doc.replace(offset, length, "library(foreign);write.foreign("+var+",\"" + fileWithoutExt + ".txt\",\""
							+ fileWithoutExt + ".sas\",package=\"SAS\")");

				} else if (extension.equals(filterExt[7])) {
					doc.replace(offset, length, "library(XML);saveXML("+var+",file=\"" + selected + "\")");

				} else if (extension.equals(filterExt[8])) {
					doc.replace(offset, length, "library(foreign);write.foreign("+var+",\"" + fileWithoutExt + ".txt\",\""
							+ fileWithoutExt + ".sps\",package=\"SPSS\")");

				} else if (extension.equals(filterExt[9])) {
					doc.replace(offset, length, "library(foreign);write.foreign("+var+",\"" + selected + "\")");

				} else if (extension.equals(filterExt[10])) {
					doc.replace(offset, length, "library(foreign);write.foreign("+var+",\"" + fileWithoutExt + ".txt\",\""
							+ fileWithoutExt + ".syd\",package=\"Systat\")");

				} else if (extension.equals(filterExt[11])) {
					doc.replace(offset, length, "library(foreign);write.arff("+var+",\"" + selected + "\")");

				} else if (extension.equals(filterExt[12])) {
					doc.replace(offset, length, "library(R.matlab);writeMat(\"" + selected + "\",x=as.matrix("+var+"))");

				} else if (extension.equals(filterExt[13])) {
					doc.replace(offset, length, "library(foreign);write.dbf("+var+",\"" + selected + "\")");

				} else if (extension.equals(filterExt[14])) {
					doc.replace(offset, length, "library(rgdal);writeGDAL("+var+", fname =\"" + fileWithoutExt
							+ ".tif\" , drivername = \"GTiff\", type = \"Float32\")");

				} else if (extension.equals(filterExt[15])) {
					doc.replace(offset, length, "library(sf);sfobject<-st_as_sf("+var+");st_write(sfobject,\"" +  fileWithoutExt+".shp\")");

				} else if (extension.equals(filterExt[16])) {
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
