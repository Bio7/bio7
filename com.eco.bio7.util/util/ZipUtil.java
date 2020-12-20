package com.eco.bio7.zip.util;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class ZipUtil {
	
	/*
	 * Source from: http://www.java2s.com/Code/Java/File-Input-Output/
	 * classforexplodingjarzipfilesontothefilesystem.htm
	 * 
	 * Copyright (c) 2004, 2008 IBM Corporation. All rights reserved. This
	 * program and the accompanying materials are made available under the terms
	 * of the Eclipse Public License v1.0 which accompanies this distribution,
	 * and is available at http://www.eclipse.org/legal/epl-v10.html
	 * 
	 * Contributors: IBM Corporation - initial API and implementation
	 * 
	 * @author Barry Feigenbaum
	 * **************************************************
	 * ***************************
	 */

	private String[] names;

	public void processFile(String zipName) throws IOException {
		boolean sortNames = true;
		String source = zipName;
		String dest = new File(zipName).getParent();
		ZipFile f = null;
		try {
			f = new ZipFile(source);
			Map fEntries = getEntries(f);
			names = (String[]) fEntries.keySet().toArray(new String[] {});
			if (sortNames) {
				Arrays.sort(names);
			}
			// copy all files
			for (int i = 0; i < names.length; i++) {
				String name = names[i];
				ZipEntry e = (ZipEntry) fEntries.get(name);

				copyFileEntry(dest, f, e);
			}
		} catch (IOException ioe) {
			String msg = ioe.getMessage();
			if (msg.indexOf(zipName) < 0) {
				msg += " - " + zipName;
			}
			throw new IOException(msg);
		} finally {
			if (f != null) {
				try {
					f.close();
				} catch (IOException ioe) {
				}
			}
		}
	}

	/** Get all the entries in a ZIP file. */
	protected Map getEntries(ZipFile zf) {
		Enumeration e = zf.entries();
		Map m = new HashMap();
		while (e.hasMoreElements()) {
			ZipEntry ze = (ZipEntry) e.nextElement();
			m.put(ze.getName(), ze);
		}
		return m;
	}

	/**
	 * copy a single entry from the archive
	 * 
	 * @param destDir
	 * @param zf
	 * @param ze
	 * @throws IOException
	 */
	public void copyFileEntry(String destDir, ZipFile zf, ZipEntry ze) throws IOException {
		DataInputStream dis = new DataInputStream(new BufferedInputStream(zf.getInputStream(ze)));
		try {
			copyFileEntry(destDir, ze.isDirectory(), ze.getName(), dis);
		} finally {
			try {
				dis.close();
			} catch (IOException ioe) {
			}
		}
	}

	protected void copyFileEntry(String destDir, boolean destIsDir, String destFile, DataInputStream dis) throws IOException {
		byte[] bytes = readAllBytes(dis);
		File file = new File(destFile);
		String parent = file.getParent();
		if (parent != null && parent.length() > 0) {
			File dir = new File(destDir, parent);
			if (dir != null) {
				dir.mkdirs();
			}
		}
		File outFile = new File(destDir, destFile);
		if (destIsDir) {
			outFile.mkdir();
		} else {
			FileOutputStream fos = new FileOutputStream(outFile);
			try {
				fos.write(bytes, 0, bytes.length);
			} finally {
				try {
					fos.close();
				} catch (IOException ioe) {
				}
			}
		}
	}

	/** Read all the bytes in a ZIPed file */
	protected byte[] readAllBytes(DataInputStream is) throws IOException {
		byte[] bytes = new byte[0];
		for (int len = is.available(); len > 0; len = is.available()) {
			byte[] xbytes = new byte[len];
			int count = is.read(xbytes);
			if (count > 0) {
				byte[] nbytes = new byte[bytes.length + count];
				System.arraycopy(bytes, 0, nbytes, 0, bytes.length);
				System.arraycopy(xbytes, 0, nbytes, bytes.length, count);
				bytes = nbytes;
			} else if (count < 0) {
				// accommodate apparent bug in IBM JVM where
				// available() always returns positive value on some files
				break;
			}
		}
		return bytes;
	}

	protected void print(String s) {
		System.out.print(s);
	}

}
