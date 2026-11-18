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
package com.eco.bio7.gdal.internal;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * The functions of the C API of GDAL (gdal.h, ogr_api.h, ogr_srs_api.h,
 * cpl_*.h, gdal_utils.h) as method handles (Foreign Function and Memory API).
 * Functions which are missing in older versions of GDAL are null (the API
 * reports them as not available). Internal: see the package com.eco.bio7.gdal.
 */
public final class GdalNative {

	public static final Linker LINKER = Linker.nativeLinker();
	/* A C string or other pointer which may be read (unknown length) */
	public static final AddressLayout C_POINTER = ADDRESS.withTargetLayout(java.lang.foreign.MemoryLayout.sequenceLayout(Long.MAX_VALUE, ValueLayout.JAVA_BYTE));

	private final SymbolLookup lookup;
	public final Path library;

	/** The loaded library (set by Gdal.load), null if GDAL is not loaded (also for the org.gdal compatibility classes). */
	public static volatile GdalNative loaded;

	/* For the classes of org.gdal (compatibility with the Java bindings of GDAL) */
	public final MethodHandle GDALReprojectImage, GDALCreateCopy, GDALFillRaster, GDALSetRasterStatistics, GDALGetRasterMinimum,
			GDALGetRasterMaximum, GDALGetOverviewCount, GDALGetOverview, GDALGetRasterBandXSize, GDALGetRasterBandYSize,
			GDALSetRasterColorInterpretation, GDALSetRasterColorTable, GDALCreateColorTable, GDALSetColorEntry, GDALDestroyColorTable,
			OSRImportFromWkt, OSRImportFromProj4, OSRIsLocal, OSRIsSame, OSRGetAttrValue, CPLPushErrorHandler, CPLPopErrorHandler;

	/* Writing vectors, filters and SQL */
	public final MethodHandle GDALDatasetCreateLayer, OGR_Fld_Create, OGR_Fld_Destroy, OGR_L_CreateField, OGR_F_Create, OGR_F_SetFieldString,
			OGR_F_SetFieldDouble, OGR_F_SetFieldInteger64, OGR_F_SetFieldNull, OGR_F_SetGeometryDirectly, OGR_L_CreateFeature, OGR_FD_GetFieldIndex,
			OGR_G_CreateFromWkt, OGR_G_DestroyGeometry, OGR_L_SyncToDisk, OGR_L_SetAttributeFilter, OGR_L_SetSpatialFilterRect,
			OGR_L_SetSpatialFilter, GDALDatasetExecuteSQL, GDALDatasetReleaseResultSet;
	/* gdaldem, gdalbuildvrt, gdal_rasterize (gdal_utils.h), contours and polygons (gdal_alg.h) */
	public final MethodHandle GDALDEMProcessingOptionsNew, GDALDEMProcessingOptionsFree, GDALDEMProcessing, GDALBuildVRTOptionsNew,
			GDALBuildVRTOptionsFree, GDALBuildVRT, GDALRasterizeOptionsNew, GDALRasterizeOptionsFree, GDALRasterize, GDALContourGenerateEx,
			GDALPolygonize, GDALFPolygonize, GDALGetMaskBand;

	/* cpl */
	public final MethodHandle CPLSetConfigOption, CPLGetLastErrorMsg, CPLGetLastErrorNo, CPLErrorReset, CPLSetErrorHandler, VSIFree, CSLDestroy;
	public final MemorySegment CPLQuietErrorHandler;
	/* gdal */
	public final MethodHandle GDALAllRegister, GDALVersionInfo, GDALOpenEx, GDALClose, GDALGetRasterXSize, GDALGetRasterYSize, GDALGetRasterCount,
			GDALGetRasterBand, GDALGetGeoTransform, GDALSetGeoTransform, GDALGetProjectionRef, GDALSetProjection, GDALGetDatasetDriver,
			GDALGetDriverShortName, GDALGetDriverLongName, GDALGetDriverByName, GDALGetDriverCount, GDALGetDriver, GDALGetMetadataItem,
			GDALGetMetadata, GDALGetDescription, GDALCreate, GDALFlushCache, GDALGetRasterDataType, GDALGetRasterNoDataValue,
			GDALSetRasterNoDataValue, GDALGetRasterColorInterpretation, GDALRasterIO, GDALComputeRasterMinMax, GDALGetRasterColorTable,
			GDALGetColorEntryCount, GDALGetColorEntry, GDALGetRasterScale, GDALGetRasterOffset, GDALSetDescription, GDALGetFileList;
	/* gdal_utils.h (GDAL 2.1+) */
	public final MethodHandle GDALTranslateOptionsNew, GDALTranslateOptionsFree, GDALTranslate, GDALWarpAppOptionsNew, GDALWarpAppOptionsFree,
			GDALWarp, GDALVectorTranslateOptionsNew, GDALVectorTranslateOptionsFree, GDALVectorTranslate, GDALInfoOptionsNew, GDALInfoOptionsFree,
			GDALInfo;
	/* ogr */
	public final MethodHandle GDALDatasetGetLayerCount, GDALDatasetGetLayer, OGR_L_GetName, OGR_L_GetFeatureCount, OGR_L_ResetReading,
			OGR_L_GetNextFeature, OGR_L_GetLayerDefn, OGR_L_GetSpatialRef, OGR_L_GetGeomType, OGR_L_GetExtent, OGR_FD_GetFieldCount,
			OGR_FD_GetFieldDefn, OGR_Fld_GetNameRef, OGR_Fld_GetType, OGR_F_IsFieldSetAndNotNull, OGR_F_GetFieldAsString,
			OGR_F_GetFieldAsDouble, OGR_F_GetFieldAsInteger64, OGR_F_GetGeometryRef, OGR_F_GetFID, OGR_F_Destroy, OGR_G_ExportToWkt,
			OGR_G_ExportToJson;
	/* osr */
	public final MethodHandle OSRNewSpatialReference, OSRDestroySpatialReference, OSRClone, OSRImportFromEPSG, OSRSetFromUserInput,
			OSRExportToWkt, OSRExportToPrettyWkt, OSRExportToProj4, OSRGetAuthorityName, OSRGetAuthorityCode, OSRGetName, OSRIsGeographic,
			OSRIsProjected, OSRSetAxisMappingStrategy, OSRSetPROJSearchPaths, OCTNewCoordinateTransformation, OCTDestroyCoordinateTransformation,
			OCTTransform;

	/** Loads the library (and its dependencies from its folder on Windows). */
	public GdalNative(Path library) {
		this.library = library;
		if (System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).startsWith("windows")) {
			/* The DLLs which GDAL needs (PROJ, GEOS, ...) are in the folder of gdal*.dll */
			setDllDirectory(library.getParent());
		}
		this.lookup = SymbolLookup.libraryLookup(library, Arena.global());
		FunctionDescriptor pv = FunctionDescriptor.ofVoid(ADDRESS);
		FunctionDescriptor pp = FunctionDescriptor.of(C_POINTER, ADDRESS);
		FunctionDescriptor ip = FunctionDescriptor.of(JAVA_INT, ADDRESS);

		CPLSetConfigOption = need("CPLSetConfigOption", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
		CPLGetLastErrorMsg = need("CPLGetLastErrorMsg", FunctionDescriptor.of(C_POINTER));
		CPLGetLastErrorNo = need("CPLGetLastErrorNo", FunctionDescriptor.of(JAVA_INT));
		CPLErrorReset = need("CPLErrorReset", FunctionDescriptor.ofVoid());
		CPLSetErrorHandler = need("CPLSetErrorHandler", FunctionDescriptor.of(ADDRESS, ADDRESS));
		CPLQuietErrorHandler = lookup.find("CPLQuietErrorHandler").orElse(null);
		VSIFree = need("VSIFree", pv);
		CSLDestroy = need("CSLDestroy", pv);

		GDALAllRegister = need("GDALAllRegister", FunctionDescriptor.ofVoid());
		GDALVersionInfo = need("GDALVersionInfo", pp);
		GDALOpenEx = need("GDALOpenEx", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
		/* GDALClose returns CPLErr since 3.7 (void before): the result is not used */
		GDALClose = need("GDALClose", pv);
		GDALGetRasterXSize = need("GDALGetRasterXSize", ip);
		GDALGetRasterYSize = need("GDALGetRasterYSize", ip);
		GDALGetRasterCount = need("GDALGetRasterCount", ip);
		GDALGetRasterBand = need("GDALGetRasterBand", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));
		GDALGetGeoTransform = need("GDALGetGeoTransform", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		GDALSetGeoTransform = need("GDALSetGeoTransform", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		GDALGetProjectionRef = need("GDALGetProjectionRef", pp);
		GDALSetProjection = need("GDALSetProjection", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		GDALGetDatasetDriver = need("GDALGetDatasetDriver", FunctionDescriptor.of(ADDRESS, ADDRESS));
		GDALGetDriverShortName = need("GDALGetDriverShortName", pp);
		GDALGetDriverLongName = need("GDALGetDriverLongName", pp);
		GDALGetDriverByName = need("GDALGetDriverByName", FunctionDescriptor.of(ADDRESS, ADDRESS));
		GDALGetDriverCount = need("GDALGetDriverCount", FunctionDescriptor.of(JAVA_INT));
		GDALGetDriver = need("GDALGetDriver", FunctionDescriptor.of(ADDRESS, JAVA_INT));
		GDALGetMetadataItem = need("GDALGetMetadataItem", FunctionDescriptor.of(C_POINTER, ADDRESS, ADDRESS, ADDRESS));
		GDALGetMetadata = need("GDALGetMetadata", FunctionDescriptor.of(C_POINTER, ADDRESS, ADDRESS));
		GDALGetDescription = need("GDALGetDescription", pp);
		GDALSetDescription = need("GDALSetDescription", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
		GDALCreate = need("GDALCreate", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS));
		/* GDALFlushCache returns CPLErr since 3.7 (void before) */
		GDALFlushCache = need("GDALFlushCache", pv);
		GDALGetFileList = need("GDALGetFileList", pp);
		GDALGetRasterDataType = need("GDALGetRasterDataType", ip);
		GDALGetRasterNoDataValue = need("GDALGetRasterNoDataValue", FunctionDescriptor.of(JAVA_DOUBLE, ADDRESS, ADDRESS));
		GDALSetRasterNoDataValue = need("GDALSetRasterNoDataValue", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_DOUBLE));
		GDALGetRasterColorInterpretation = need("GDALGetRasterColorInterpretation", ip);
		GDALRasterIO = need("GDALRasterIO", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS,
				JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT));
		/* GDALComputeRasterMinMax returns CPLErr since 3.7 (void before): the result is not used */
		GDALComputeRasterMinMax = need("GDALComputeRasterMinMax", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS));
		GDALGetRasterColorTable = need("GDALGetRasterColorTable", FunctionDescriptor.of(ADDRESS, ADDRESS));
		GDALGetColorEntryCount = need("GDALGetColorEntryCount", ip);
		GDALGetColorEntry = need("GDALGetColorEntry", FunctionDescriptor.of(C_POINTER, ADDRESS, JAVA_INT));
		GDALGetRasterScale = need("GDALGetRasterScale", FunctionDescriptor.of(JAVA_DOUBLE, ADDRESS, ADDRESS));
		GDALGetRasterOffset = need("GDALGetRasterOffset", FunctionDescriptor.of(JAVA_DOUBLE, ADDRESS, ADDRESS));

		FunctionDescriptor optionsNew = FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS);
		GDALTranslateOptionsNew = optional("GDALTranslateOptionsNew", optionsNew);
		GDALTranslateOptionsFree = optional("GDALTranslateOptionsFree", pv);
		GDALTranslate = optional("GDALTranslate", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
		GDALWarpAppOptionsNew = optional("GDALWarpAppOptionsNew", optionsNew);
		GDALWarpAppOptionsFree = optional("GDALWarpAppOptionsFree", pv);
		GDALWarp = optional("GDALWarp", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
		GDALVectorTranslateOptionsNew = optional("GDALVectorTranslateOptionsNew", optionsNew);
		GDALVectorTranslateOptionsFree = optional("GDALVectorTranslateOptionsFree", pv);
		GDALVectorTranslate = optional("GDALVectorTranslate", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
		GDALInfoOptionsNew = optional("GDALInfoOptionsNew", optionsNew);
		GDALInfoOptionsFree = optional("GDALInfoOptionsFree", pv);
		GDALInfo = optional("GDALInfo", FunctionDescriptor.of(C_POINTER, ADDRESS, ADDRESS));

		GDALDatasetGetLayerCount = need("GDALDatasetGetLayerCount", ip);
		GDALDatasetGetLayer = need("GDALDatasetGetLayer", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));
		OGR_L_GetName = need("OGR_L_GetName", pp);
		OGR_L_GetFeatureCount = need("OGR_L_GetFeatureCount", FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT));
		OGR_L_ResetReading = need("OGR_L_ResetReading", pv);
		OGR_L_GetNextFeature = need("OGR_L_GetNextFeature", FunctionDescriptor.of(ADDRESS, ADDRESS));
		OGR_L_GetLayerDefn = need("OGR_L_GetLayerDefn", FunctionDescriptor.of(ADDRESS, ADDRESS));
		OGR_L_GetSpatialRef = need("OGR_L_GetSpatialRef", FunctionDescriptor.of(ADDRESS, ADDRESS));
		OGR_L_GetGeomType = need("OGR_L_GetGeomType", ip);
		OGR_L_GetExtent = need("OGR_L_GetExtent", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
		OGR_FD_GetFieldCount = need("OGR_FD_GetFieldCount", ip);
		OGR_FD_GetFieldDefn = need("OGR_FD_GetFieldDefn", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));
		OGR_Fld_GetNameRef = need("OGR_Fld_GetNameRef", pp);
		OGR_Fld_GetType = need("OGR_Fld_GetType", ip);
		OGR_F_IsFieldSetAndNotNull = need("OGR_F_IsFieldSetAndNotNull", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
		OGR_F_GetFieldAsString = need("OGR_F_GetFieldAsString", FunctionDescriptor.of(C_POINTER, ADDRESS, JAVA_INT));
		OGR_F_GetFieldAsDouble = need("OGR_F_GetFieldAsDouble", FunctionDescriptor.of(JAVA_DOUBLE, ADDRESS, JAVA_INT));
		OGR_F_GetFieldAsInteger64 = need("OGR_F_GetFieldAsInteger64", FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT));
		OGR_F_GetGeometryRef = need("OGR_F_GetGeometryRef", FunctionDescriptor.of(ADDRESS, ADDRESS));
		OGR_F_GetFID = need("OGR_F_GetFID", FunctionDescriptor.of(JAVA_LONG, ADDRESS));
		OGR_F_Destroy = need("OGR_F_Destroy", pv);
		OGR_G_ExportToWkt = need("OGR_G_ExportToWkt", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		OGR_G_ExportToJson = need("OGR_G_ExportToJson", pp);

		OSRNewSpatialReference = need("OSRNewSpatialReference", FunctionDescriptor.of(ADDRESS, ADDRESS));
		OSRDestroySpatialReference = need("OSRDestroySpatialReference", pv);
		OSRClone = need("OSRClone", FunctionDescriptor.of(ADDRESS, ADDRESS));
		OSRImportFromEPSG = need("OSRImportFromEPSG", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
		OSRSetFromUserInput = need("OSRSetFromUserInput", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		OSRExportToWkt = need("OSRExportToWkt", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		OSRExportToPrettyWkt = need("OSRExportToPrettyWkt", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
		OSRExportToProj4 = need("OSRExportToProj4", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		OSRGetAuthorityName = need("OSRGetAuthorityName", FunctionDescriptor.of(C_POINTER, ADDRESS, ADDRESS));
		OSRGetAuthorityCode = need("OSRGetAuthorityCode", FunctionDescriptor.of(C_POINTER, ADDRESS, ADDRESS));
		OSRGetName = optional("OSRGetName", pp);
		OSRIsGeographic = need("OSRIsGeographic", ip);
		OSRIsProjected = need("OSRIsProjected", ip);
		/* GDAL 3.0+: the axis order of lat/lon reference systems (traditional: x = longitude) */
		OSRSetAxisMappingStrategy = optional("OSRSetAxisMappingStrategy", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
		OSRSetPROJSearchPaths = optional("OSRSetPROJSearchPaths", pv);
		OCTNewCoordinateTransformation = need("OCTNewCoordinateTransformation", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
		OCTDestroyCoordinateTransformation = need("OCTDestroyCoordinateTransformation", pv);
		OCTTransform = need("OCTTransform", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS));

		GDALReprojectImage = need("GDALReprojectImage", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, JAVA_DOUBLE,
				JAVA_DOUBLE, ADDRESS, ADDRESS, ADDRESS));
		GDALCreateCopy = need("GDALCreateCopy", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
		GDALFillRaster = need("GDALFillRaster", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_DOUBLE, JAVA_DOUBLE));
		GDALSetRasterStatistics = need("GDALSetRasterStatistics", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE));
		GDALGetRasterMinimum = need("GDALGetRasterMinimum", FunctionDescriptor.of(JAVA_DOUBLE, ADDRESS, ADDRESS));
		GDALGetRasterMaximum = need("GDALGetRasterMaximum", FunctionDescriptor.of(JAVA_DOUBLE, ADDRESS, ADDRESS));
		GDALGetOverviewCount = need("GDALGetOverviewCount", ip);
		GDALGetOverview = need("GDALGetOverview", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));
		GDALGetRasterBandXSize = need("GDALGetRasterBandXSize", ip);
		GDALGetRasterBandYSize = need("GDALGetRasterBandYSize", ip);
		GDALSetRasterColorInterpretation = need("GDALSetRasterColorInterpretation", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT));
		GDALSetRasterColorTable = need("GDALSetRasterColorTable", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		GDALCreateColorTable = need("GDALCreateColorTable", FunctionDescriptor.of(ADDRESS, JAVA_INT));
		GDALSetColorEntry = need("GDALSetColorEntry", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS));
		GDALDestroyColorTable = need("GDALDestroyColorTable", pv);
		OSRImportFromWkt = need("OSRImportFromWkt", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		OSRImportFromProj4 = need("OSRImportFromProj4", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		OSRIsLocal = need("OSRIsLocal", ip);
		OSRIsSame = need("OSRIsSame", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		OSRGetAttrValue = need("OSRGetAttrValue", FunctionDescriptor.of(C_POINTER, ADDRESS, ADDRESS, JAVA_INT));
		CPLPushErrorHandler = need("CPLPushErrorHandler", pv);
		CPLPopErrorHandler = need("CPLPopErrorHandler", FunctionDescriptor.ofVoid());

		GDALDatasetCreateLayer = need("GDALDatasetCreateLayer", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS));
		OGR_Fld_Create = need("OGR_Fld_Create", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));
		OGR_Fld_Destroy = need("OGR_Fld_Destroy", pv);
		OGR_L_CreateField = need("OGR_L_CreateField", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT));
		OGR_F_Create = need("OGR_F_Create", FunctionDescriptor.of(ADDRESS, ADDRESS));
		OGR_F_SetFieldString = need("OGR_F_SetFieldString", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS));
		OGR_F_SetFieldDouble = need("OGR_F_SetFieldDouble", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_DOUBLE));
		OGR_F_SetFieldInteger64 = need("OGR_F_SetFieldInteger64", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_LONG));
		OGR_F_SetFieldNull = optional("OGR_F_SetFieldNull", FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT));
		OGR_F_SetGeometryDirectly = need("OGR_F_SetGeometryDirectly", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		OGR_L_CreateFeature = need("OGR_L_CreateFeature", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		OGR_FD_GetFieldIndex = need("OGR_FD_GetFieldIndex", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		OGR_G_CreateFromWkt = need("OGR_G_CreateFromWkt", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
		OGR_G_DestroyGeometry = need("OGR_G_DestroyGeometry", pv);
		OGR_L_SyncToDisk = need("OGR_L_SyncToDisk", ip);
		OGR_L_SetAttributeFilter = need("OGR_L_SetAttributeFilter", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
		OGR_L_SetSpatialFilterRect = need("OGR_L_SetSpatialFilterRect", FunctionDescriptor.ofVoid(ADDRESS, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE, JAVA_DOUBLE));
		OGR_L_SetSpatialFilter = need("OGR_L_SetSpatialFilter", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
		GDALDatasetExecuteSQL = need("GDALDatasetExecuteSQL", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
		GDALDatasetReleaseResultSet = need("GDALDatasetReleaseResultSet", FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));

		FunctionDescriptor optsNew = FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS);
		GDALDEMProcessingOptionsNew = optional("GDALDEMProcessingOptionsNew", optsNew);
		GDALDEMProcessingOptionsFree = optional("GDALDEMProcessingOptionsFree", pv);
		GDALDEMProcessing = optional("GDALDEMProcessing", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
		GDALBuildVRTOptionsNew = optional("GDALBuildVRTOptionsNew", optsNew);
		GDALBuildVRTOptionsFree = optional("GDALBuildVRTOptionsFree", pv);
		GDALBuildVRT = optional("GDALBuildVRT", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
		GDALRasterizeOptionsNew = optional("GDALRasterizeOptionsNew", optsNew);
		GDALRasterizeOptionsFree = optional("GDALRasterizeOptionsFree", pv);
		GDALRasterize = optional("GDALRasterize", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
		/* GDAL 2.4+ */
		GDALContourGenerateEx = optional("GDALContourGenerateEx", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS));
		GDALPolygonize = need("GDALPolygonize", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
		GDALFPolygonize = need("GDALFPolygonize", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
		GDALGetMaskBand = need("GDALGetMaskBand", FunctionDescriptor.of(ADDRESS, ADDRESS));
	}

	private MethodHandle need(String name, FunctionDescriptor fd) {
		Optional<MemorySegment> s = lookup.find(name);
		if (s.isEmpty()) {
			throw new UnsatisfiedLinkError("The function " + name + " is missing in " + library + " (not a GDAL library?)");
		}
		return LINKER.downcallHandle(s.get(), fd);
	}

	private MethodHandle optional(String name, FunctionDescriptor fd) {
		return lookup.find(name).map(s -> LINKER.downcallHandle(s, fd)).orElse(null);
	}

	/* Windows: the folder of the dependencies of the library */
	private static void setDllDirectory(Path folder) {
		try {
			SymbolLookup kernel = SymbolLookup.libraryLookup("kernel32", Arena.global());
			MethodHandle set = LINKER.downcallHandle(kernel.find("SetDllDirectoryW").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS));
			try (Arena a = Arena.ofConfined()) {
				byte[] utf16 = (folder.toString() + "\0").getBytes(StandardCharsets.UTF_16LE);
				MemorySegment s = a.allocate(utf16.length);
				s.copyFrom(MemorySegment.ofArray(utf16));
				int unused = (int) set.invokeExact(s);
			}
		} catch (Throwable t) {
			/* Then the folder must be in the PATH */
		}
	}

	/* ---------------- Helpers ---------------- */

	/** A C string of a native pointer (null for NULL). */
	public static String string(MemorySegment p) {
		return p == null || p.equals(MemorySegment.NULL) ? null : p.getString(0, StandardCharsets.UTF_8);
	}

	/** A C string in the arena (null: NULL). */
	public static MemorySegment cstring(Arena arena, String s) {
		return s == null ? MemorySegment.NULL : arena.allocateFrom(s, StandardCharsets.UTF_8);
	}

	/**
	 * A C string in the arena, NULL for null or "" (as a method: a conditional
	 * expression as argument of invokeExact has the static type Object).
	 */
	public static MemorySegment cstringOrNull(Arena arena, String s) {
		return s == null || s.isEmpty() ? MemorySegment.NULL : arena.allocateFrom(s, StandardCharsets.UTF_8);
	}

	/** A NULL terminated list of C strings (char**), NULL for null or an empty list. */
	public static MemorySegment stringList(Arena arena, List<String> values) {
		if (values == null || values.isEmpty()) {
			return MemorySegment.NULL;
		}
		MemorySegment list = arena.allocate(ADDRESS, values.size() + 1L);
		for (int i = 0; i < values.size(); i++) {
			list.setAtIndex(ADDRESS, i, cstring(arena, values.get(i)));
		}
		list.setAtIndex(ADDRESS, values.size(), MemorySegment.NULL);
		return list;
	}

	/** The strings of a NULL terminated list (char**) of GDAL. */
	public static List<String> strings(MemorySegment list) {
		List<String> out = new java.util.ArrayList<>();
		if (list == null || list.equals(MemorySegment.NULL)) {
			return out;
		}
		MemorySegment l = list.reinterpret(Long.MAX_VALUE);
		for (long i = 0;; i++) {
			MemorySegment s = l.getAtIndex(ADDRESS, i);
			if (s.equals(MemorySegment.NULL)) {
				break;
			}
			out.add(s.reinterpret(Long.MAX_VALUE).getString(0, StandardCharsets.UTF_8));
		}
		return out;
	}

	public static boolean isNull(MemorySegment p) {
		return p == null || p.equals(MemorySegment.NULL);
	}
}
