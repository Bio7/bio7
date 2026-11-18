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
package org.gdal.gdalconst;

/** The constants of GDAL (compatible with the Java bindings of GDAL). */
public interface gdalconstConstants {
	int GA_ReadOnly = 0;
	int GA_Update = 1;

	int CE_None = 0;
	int CE_Debug = 1;
	int CE_Warning = 2;
	int CE_Failure = 3;
	int CE_Fatal = 4;

	int GDT_Unknown = 0;
	int GDT_Byte = 1;
	int GDT_UInt16 = 2;
	int GDT_Int16 = 3;
	int GDT_UInt32 = 4;
	int GDT_Int32 = 5;
	int GDT_Float32 = 6;
	int GDT_Float64 = 7;
	int GDT_CInt16 = 8;
	int GDT_CInt32 = 9;
	int GDT_CFloat32 = 10;
	int GDT_CFloat64 = 11;
	int GDT_UInt64 = 12;
	int GDT_Int64 = 13;
	int GDT_Int8 = 14;
	int GDT_TypeCount = 15;

	int GCI_Undefined = 0;
	int GCI_GrayIndex = 1;
	int GCI_PaletteIndex = 2;
	int GCI_RedBand = 3;
	int GCI_GreenBand = 4;
	int GCI_BlueBand = 5;
	int GCI_AlphaBand = 6;
	int GCI_HueBand = 7;
	int GCI_SaturationBand = 8;
	int GCI_LightnessBand = 9;
	int GCI_CyanBand = 10;
	int GCI_MagentaBand = 11;
	int GCI_YellowBand = 12;
	int GCI_BlackBand = 13;

	int GPI_Gray = 0;
	int GPI_RGB = 1;
	int GPI_CMYK = 2;
	int GPI_HLS = 3;

	int GRA_NearestNeighbour = 0;
	int GRA_Bilinear = 1;
	int GRA_Cubic = 2;
	int GRA_CubicSpline = 3;
	int GRA_Lanczos = 4;
	int GRA_Average = 5;
	int GRA_Mode = 6;

	int GF_Read = 0;
	int GF_Write = 1;
}
