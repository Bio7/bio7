/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
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
package org.gdal.gdal;

import java.awt.Color;
import java.awt.image.IndexColorModel;

/** A color table of a band (compatible with the Java bindings of GDAL): the entries as colors. */
public class ColorTable implements Cloneable {

	private final int[] argb;

	public ColorTable() {
		this(new int[0]);
	}

	ColorTable(int[] argb) {
		this.argb = argb;
	}

	int[] argb() {
		return argb;
	}

	public int GetCount() {
		return argb.length;
	}

	public Color GetColorEntry(int index) {
		int c = argb[index];
		return new Color(c >> 16 & 0xff, c >> 8 & 0xff, c & 0xff, c >>> 24);
	}

	/** The color model for indexed pixels of the given number of bits (e.g., 8). */
	public IndexColorModel getIndexColorModel(int bits) {
		int n = Math.min(argb.length, 1 << Math.min(16, Math.max(1, bits)));
		byte[] r = new byte[n], g = new byte[n], b = new byte[n], a = new byte[n];
		for (int i = 0; i < n; i++) {
			r[i] = (byte) (argb[i] >> 16);
			g[i] = (byte) (argb[i] >> 8);
			b[i] = (byte) argb[i];
			a[i] = (byte) (argb[i] >>> 24);
		}
		return new IndexColorModel(bits, n, r, g, b, a);
	}

	@Override
	public ColorTable clone() {
		return new ColorTable(argb.clone());
	}

	public void delete() {
	}
}
