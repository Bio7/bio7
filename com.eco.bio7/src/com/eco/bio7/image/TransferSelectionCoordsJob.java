package com.eco.bio7.image;

import java.awt.Polygon;
import java.io.File;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconst;
import org.gdal.osr.SpatialReference;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RServe;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

public class TransferSelectionCoordsJob extends WorkspaceJob implements IJobChangeListener {

	protected int selection;

	private RConnection c;

	private boolean transferAsList;

	private int selectedType;

	private boolean setCrs;

	private boolean setDf;

	private String crsString;

	private String selDataframe;

	private double[] adfGeoTransform;

	private String proj4String;

	private boolean centroidCoords;

	public TransferSelectionCoordsJob(boolean transfer, int selection, boolean doSetCRS, boolean doSetDf, String crs, String selectedDf,boolean centroids) {
		super("Match progress....");
		this.transferAsList = transfer;
		this.selectedType = selection;
		this.setCrs = doSetCRS;
		this.setDf = doSetDf;
		this.crsString = crs;
		this.selDataframe = selectedDf;
		/*System.out.println(setCrs);
		System.out.println(setDf);
		System.out.println(crsString);
		System.out.println(selDataframe);*/
		this.centroidCoords=centroids;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {

		monitor.beginTask("Transfer selections.....", IProgressMonitor.UNKNOWN);

		if (transferAsList) {
			exportShape();
		} else {

			if (setCrs) {

				/*
				 * Open georeferenced file to extract the georeferenced
				 * information!
				 */

				if (crsString == null || crsString.isBlank()) {
					Bio7Dialog.message("No georeferenced file was selected to add its projection.");
					return Status.OK_STATUS;
				}
				File f = new File(crsString);
				boolean read = false;
				try {
					read = readGeoreferenceWithGdal(f);
				} catch (LinkageError e) {
					/*
					 * The native libraries of GDAL are not available (e.g., on macOS): the
					 * georeference is read with R!
					 */
					System.err.println("GDAL is not available (" + e.getMessage() + "), the georeference is read with R.");
				} catch (Exception e) {
					System.err.println("The georeference could not be read with GDAL: " + e.getMessage());
				}
				String problem = null;
				if (!read) {
					problem = readGeoreferenceWithR(f);
					read = problem == null;
				}
				/* The spatial objects of the transfer are created with the R package sp! */
				String missingSp = read ? missingSpPackage() : null;
				if (missingSp != null) {
					Bio7Dialog.message(missingSp);
				} else if (read) {
					startExport(selectedType);
				} else {
					Bio7Dialog.message("The georeference of the file could not be read:\n" + f.getAbsolutePath() + "\n\nGDAL is not available. " + problem);
				}

			}
			/* Export of selections as a list! */
			else {
				startExport(selectedType);
			}
		}

		return Status.OK_STATUS;
	}

	/*
	 * Reads the transform parameters and the projection (Proj4) of the
	 * georeferenced file with GDAL. Throws a LinkageError if the native libraries
	 * of GDAL are not available!
	 */
	private boolean readGeoreferenceWithGdal(File f) {
		Dataset poDataset = (Dataset) gdal.Open(f.getAbsolutePath(), gdalconst.GA_ReadOnly);
		if (poDataset == null) {
			System.out.println("The image could not be read.");
			return false;
		}
		/* The array which contains the transform parameters! */
		adfGeoTransform = new double[6];
		/*
		 * Put the transform parameters in the given array! See:
		 * http://gdal.org/java/org/gdal/gdal/Dataset.html# GetGeoTransform%28double[]%29
		 */
		poDataset.GetGeoTransform(adfGeoTransform);
		/*
		 * Get the spatial reference, projection and convert from Wkt to Proj4!
		 */
		SpatialReference layerProjection = new SpatialReference();
		layerProjection.ImportFromWkt(poDataset.GetProjectionRef());
		proj4String = layerProjection.ExportToProj4();
		return true;
	}

	/*
	 * Reads the transform parameters (like GDAL: the upper left corner, the pixel
	 * width and the negative pixel height, no rotation) and the projection (Proj4)
	 * of the georeferenced file with the R package terra (or raster). Used if the
	 * native libraries of GDAL are not available! Returns null if the georeference
	 * was read, else the reason for the dialog (e.g., the missing R packages).
	 */
	private String readGeoreferenceWithR(File f) {
		RConnection con = RServe.getConnection();
		if (con == null) {
			return "There is no connection to R (Rserve).";
		}
		String path = f.getAbsolutePath().replace("\\", "/").replace("\"", "\\\"");
		try {
			String pkg;
			if (installed(con, "terra")) {
				pkg = "terra";
			} else if (installed(con, "raster")) {
				pkg = "raster";
			} else {
				return "The R package 'terra' (or 'raster') is not installed. Please install it, e.g., in the \"Packages\" view of Bio7 (tab \"Install Packages\") or in R with:\n\ninstall.packages(\"terra\")";
			}
			con.eval(".bio7GeoFile<-\"" + path + "\"");
			/* The transform parameters and the projection or the error message of R */
			String read = pkg.equals("terra")
					? "local({r<-terra::rast(.bio7GeoFile);e<-as.vector(terra::ext(r));rs<-terra::res(r);list(c(e[1],rs[1],0,e[4],0,-rs[2]),terra::crs(r,proj=TRUE))})"
					: "local({r<-raster::raster(.bio7GeoFile);rs<-raster::res(r);list(c(raster::xmin(r),rs[1],0,raster::ymax(r),0,-rs[2]),raster::projection(r))})";
			con.eval(".bio7GeoResult<-tryCatch(" + read + ",error=function(e) conditionMessage(e))");
			boolean failed = ((REXPLogical) con.eval("is.character(.bio7GeoResult)")).isTRUE()[0];
			if (failed) {
				String message = con.eval(".bio7GeoResult").asString();
				con.eval("rm(.bio7GeoFile,.bio7GeoResult)");
				return "The R package '" + pkg + "' could not read the file:\n\n" + message;
			}
			double[] transform = con.eval(".bio7GeoResult[[1]]").asDoubles();
			String proj = con.eval("as.character(.bio7GeoResult[[2]])").asString();
			con.eval("rm(.bio7GeoFile,.bio7GeoResult)");
			if (transform == null || transform.length != 6) {
				return "The R package '" + pkg + "' returned no georeference for the file.";
			}
			if (proj == null || proj.isBlank()) {
				return "The file has no projection (coordinate reference system).";
			}
			adfGeoTransform = transform;
			proj4String = proj.trim();
			return null;
		} catch (RserveException | org.rosuda.REngine.REXPMismatchException e) {
			return "The georeference could not be read with R: " + e.getMessage();
		}
	}

	/* Returns the message for the dialog if the R package sp is not installed, else null */
	private String missingSpPackage() {
		RConnection con = RServe.getConnection();
		if (con == null) {
			return "There is no connection to R (Rserve).";
		}
		try {
			if (!installed(con, "sp")) {
				return "The R package 'sp' is not installed (needed for the spatial objects of the transfer). Please install it, e.g., in the \"Packages\" view of Bio7 (tab \"Install Packages\") or in R with:\n\ninstall.packages(\"sp\")";
			}
		} catch (RserveException | org.rosuda.REngine.REXPMismatchException e) {
			return "The R package 'sp' could not be checked: " + e.getMessage();
		}
		return null;
	}

	private static boolean installed(RConnection con, String pkg) throws RserveException, org.rosuda.REngine.REXPMismatchException {
		return ((REXPLogical) con.eval("requireNamespace('" + pkg + "', quietly=TRUE)")).isTRUE()[0];
	}

	public double getTransformedCentroidCoordsX(int row, int col) {

		/*
		 * Returns the centroid coordinates of a selection! From the java gdal
		 * API: In a north up image, geoTransformArray[1] is the pixel width,
		 * and geoTransformArray[5] is the pixel height. The upper left corner
		 * of the upper left pixel is at position
		 * (geoTransformArray[0],geoTransformArray[3]).
		 * 
		 */

		double xp = (adfGeoTransform[1] * col) + (adfGeoTransform[2] * row) + (adfGeoTransform[1] * 0.5) + (adfGeoTransform[2] * 0.5) + adfGeoTransform[0];

		return xp;
	}

	public double getTransformedCentroidCoordsY(int row, int col) {

		/*
		 * Returns the centroid coordinates of a selection! From the java gdal
		 * API: In a north up image, geoTransformArray[1] is the pixel width,
		 * and geoTransformArray[5] is the pixel height. The upper left corner
		 * of the upper left pixel is at position
		 * (geoTransformArray[0],geoTransformArray[3]).
		 * 
		 */

		double yp = (adfGeoTransform[4] * col) + (adfGeoTransform[5] * row) + (adfGeoTransform[4] * 0.5) + (adfGeoTransform[5] * 0.5) + adfGeoTransform[3];

		return yp;
	}

	public double getTransformedCoordsX(int row, int col) {

		/*
		 *
		 * From the java gdal API: In a north up image, geoTransformArray[1] is
		 * the pixel width, and geoTransformArray[5] is the pixel height. The
		 * upper left corner of the upper left pixel is at position
		 * (geoTransformArray[0],geoTransformArray[3]).
		 * 
		 */

		double xp = (adfGeoTransform[1] * col) + (adfGeoTransform[2] * row) + adfGeoTransform[0];

		return xp;
	}

	public double getTransformedCoordsY(int row, int col) {

		/*
		 * From the java gdal API: In a north up image, geoTransformArray[1] is
		 * the pixel width, and geoTransformArray[5] is the pixel height. The
		 * upper left corner of the upper left pixel is at position
		 * (geoTransformArray[0],geoTransformArray[3]).
		 * 
		 */

		double yp = (adfGeoTransform[4] * col) + (adfGeoTransform[5] * row) + adfGeoTransform[3];

		return yp;
	}

	/* The export methods for Polygons, Lines and Points! */
	public void exportShape() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp != null) {
			if (RoiManager.getInstance() != null) {
				 /*Only transfer selected ROI's from the ROI Manager or all if no ROI is selected!*/
				Roi[] r = RoiManager.getInstance().getSelectedRoisAsArray();
				c = RServe.getConnection();
				if (RServe.isAlive()) {

					/* Get the image processor of the image ! */
					ImageProcessor ip = imp.getProcessor();

					int w = ip.getWidth();
					int h = ip.getHeight();

					try {
						c.eval("imageSizeY<-" + h);

						c.eval("imageSizeX<-" + w);

					} catch (RserveException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					/*
					 * Get coordinates of all selections and add them to a list
					 * (x,y seperately)!
					 */

					try {

						c.eval("roiXSelectionCoordinates<-list()");
						c.eval("roiYSelectionCoordinates<-list()");

						for (int i = 0; i < r.length; i++) {

							Polygon p = r[i].getPolygon();
							int[] x = p.xpoints;
							int[] y = p.ypoints;

							try {
								c.assign(".xTempVarRoiSelectionCoordinates", x);
								c.assign(".yTempVarRoiSelectionCoordinates", y);
							} catch (REngineException e) {

								e.printStackTrace();
							}

							c.eval("try(roiXSelectionCoordinates[[" + (i + 1) + "]]<-.xTempVarRoiSelectionCoordinates)");
							c.eval("try(roiYSelectionCoordinates[[" + (i + 1) + "]]<-.yTempVarRoiSelectionCoordinates)");

						}
					} catch (RserveException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					Bio7Dialog.message("Values transferred!");
				} else {
					Bio7Dialog.message("No Rserve connection available!");
				}
			} else {
				Bio7Dialog.message("No ROI Manager available!");
			}

		} else {
			Bio7Dialog.message("No image available!");
		}
	}

	/* The export methods for Polygons, Lines and Points! */
	public void exportShapeGeometry(int selection) {

		RConnection c;
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp != null) {
			if (RoiManager.getInstance() != null) {
				/*Only transfer selected ROI's from the ROI Manager or all if no ROI is selected!*/
				Roi[] r = RoiManager.getInstance().getSelectedRoisAsArray();
				c = RServe.getConnection();
				if (RServe.isAlive()) {

					/* Get the image processor of the image ! */
					ImageProcessor ipSize = imp.getProcessor();
					int w = ipSize.getWidth();
					int h = ipSize.getHeight();

					try {
						c.eval("imageSizeY<-" + h);

						c.eval("imageSizeX<-" + w);

					} catch (RserveException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					/* If polygon export is selected! */
					if (selection == 0) {

						/*
						 * try { c.eval("listpolygons<-NULL");
						 * c.eval("listpolygons<-list()"); } catch
						 * (RserveException e) { // TODO Auto-generated catch
						 * block e.printStackTrace(); }
						 */

						try {
							c.eval(".listRoiXPoints<-list()");
							c.eval(".listRoiYPoints<-list()");
						} catch (RserveException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						int u;
						// int length = r.length;
						for (int i = 0; i < r.length; i++) {
							u = i + 1;
							Polygon p = r[i].getPolygon();
							int[] x = p.xpoints;
							int[] y = p.ypoints;

							if (setCrs == false) {

								try {
									c.assign(".xTempVarRoiSelectionCoordinates", x);
									c.assign(".yTempVarRoiSelectionCoordinates", y);
								} catch (REngineException e) {

									e.printStackTrace();
								}
							}

							else {
								/*
								 * Transform the coordinates to the CRS
								 * coordinates!
								 */
								double[] transX = new double[x.length];
								double[] transY = new double[y.length];

								for (int poi = 0; poi < x.length; poi++) {
									/* row=y, column=x ! */
									if (centroidCoords == false) {
										transX[poi] = getTransformedCoordsX(y[poi], x[poi]);
										transY[poi] = getTransformedCoordsY(y[poi], x[poi]);
									}

									else {
										transX[poi] = getTransformedCentroidCoordsX(y[poi], x[poi]);
										transY[poi] = getTransformedCentroidCoordsY(y[poi], x[poi]);
									}
								}
								try {
									c.assign(".xTempVarRoiSelectionCoordinates", transX);
									c.assign(".yTempVarRoiSelectionCoordinates", transY);
								} catch (REngineException e) {

									e.printStackTrace();
								}
							}

							try {
								c.eval("try(.listRoiXPoints[[" + (i + 1) + "]]<-.xTempVarRoiSelectionCoordinates)");
								c.eval("try(.listRoiYPoints[[" + (i + 1) + "]]<-.yTempVarRoiSelectionCoordinates)");
							} catch (RserveException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}

						/*
						 * A function to convert the list of ROIs to
						 * SpatialPolygons!
						 */
						if (setDf == false) {
							try {
								// c.eval("try(spatpolygons<-SpatialPolygons(listpolygons,
								// 1:length(listpolygons)))");
								if (setCrs == false) {
									c.eval("spatialPolygons<-SpatialPolygons(mapply(function(x, y,id) {Polygons(list(Polygon(cbind(x,y))),id)} ,.listRoiXPoints,.listRoiYPoints,as.character(1:length(.listRoiXPoints))))");
								} else {
									c.eval("spatialPolygons<-SpatialPolygons(mapply(function(x, y,id) {Polygons(list(Polygon(cbind(x,y))),id)} ,.listRoiXPoints,.listRoiYPoints,as.character(1:length(.listRoiXPoints))),proj4string=CRS(\"" + proj4String + "\"))");
								}

							} catch (RserveException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else {

							try {
								// c.eval("try(spatpolygons<-SpatialPolygons(listpolygons,
								// 1:length(listpolygons)))");
								if (setCrs == false) {
									c.eval("spatialPolygonsDataFrame<-SpatialPolygonsDataFrame(SpatialPolygons(mapply(function(x, y,id) {Polygons(list(Polygon(cbind(x,y))),id)} ,.listRoiXPoints,.listRoiYPoints,as.character(1:length(.listRoiXPoints))))," + selDataframe + ")");
								} else {
									c.eval("spatialPolygonsDataFrame<-SpatialPolygonsDataFrame(SpatialPolygons(mapply(function(x, y,id) {Polygons(list(Polygon(cbind(x,y))),id)} ,.listRoiXPoints,.listRoiYPoints,as.character(1:length(.listRoiXPoints))),proj4string=CRS(\"" + proj4String + "\"))," + selDataframe + ")");
								}

							} catch (RserveException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}

					}
					/* If line export is selected! */
					else if (selection == 1) {

						try {
							c.eval(".listRoiXPoints<-list()");
							c.eval(".listRoiYPoints<-list()");
						} catch (RserveException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						int u;
						// int length = r.length;
						for (int i = 0; i < r.length; i++) {
							u = i + 1;
							Polygon p = r[i].getPolygon();
							int[] x = p.xpoints;
							int[] y = p.ypoints;

							if (setCrs == false) {

								try {
									c.assign(".xTempVarRoiSelectionCoordinates", x);
									c.assign(".yTempVarRoiSelectionCoordinates", y);

								} catch (REngineException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

							else {
								/*
								 * Transform the coordinates to the CRS
								 * coordinates!
								 */
								double[] transX = new double[x.length];
								double[] transY = new double[y.length];

								for (int poi = 0; poi < x.length; poi++) {
									/* row=y, column=x ! */
									if (centroidCoords == false) {
										transX[poi] = getTransformedCoordsX(y[poi], x[poi]);
										transY[poi] = getTransformedCoordsY(y[poi], x[poi]);
									}

									else {
										transX[poi] = getTransformedCentroidCoordsX(y[poi], x[poi]);
										transY[poi] = getTransformedCentroidCoordsY(y[poi], x[poi]);
									}
								}
								try {
									c.assign(".xTempVarRoiSelectionCoordinates", transX);
									c.assign(".yTempVarRoiSelectionCoordinates", transY);
								} catch (REngineException e) {

									e.printStackTrace();
								}
							}

							try {
								c.eval("try(.listRoiXPoints[[" + (i + 1) + "]]<-.xTempVarRoiSelectionCoordinates)");
								c.eval("try(.listRoiYPoints[[" + (i + 1) + "]]<-.yTempVarRoiSelectionCoordinates)");
							} catch (RserveException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}

						/*
						 * A function to convert the list of ROIs to
						 * SpatialLines!
						 */
						if (setDf == false) {
							try {
								// c.eval("try(spatpolygons<-SpatialPolygons(listpolygons,
								// 1:length(listpolygons)))");
								if (setCrs == false) {
									c.eval("spatialLines<-SpatialLines(mapply(function(x, y,id) {Lines(list(Line(cbind(x,y))),id)} ,.listRoiXPoints,.listRoiYPoints,as.character(1:length(.listRoiXPoints))))");
								} else {
									c.eval("spatialLines<-SpatialLines(mapply(function(x, y,id) {Lines(list(Line(cbind(x,y))),id)} ,.listRoiXPoints,.listRoiYPoints,as.character(1:length(.listRoiXPoints))),proj4string=CRS(\"" + proj4String + "\"))");
								}

							} catch (RserveException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						} else {

							try {
								// c.eval("try(spatpolygons<-SpatialPolygons(listpolygons,
								// 1:length(listpolygons)))");
								if (setCrs == false) {
									c.eval("spatialLinesDataFrame<-SpatialLinesDataFrame(SpatialLines(mapply(function(x, y,id) {Lines(list(Line(cbind(x,y))),id)} ,.listRoiXPoints,.listRoiYPoints,as.character(1:length(.listRoiXPoints))))," + selDataframe + ")");
								} else {
									c.eval("spatialLinesDataFrame<-SpatialLinesDataFrame(SpatialLines(mapply(function(x, y,id) {Lines(list(Line(cbind(x,y))),id)} ,.listRoiXPoints,.listRoiYPoints,as.character(1:length(.listRoiXPoints))),proj4string=CRS(\"" + proj4String + "\"))," + selDataframe + ")");
								}

							} catch (RserveException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						}

					}
					/* If point export is selected! */
					else {

						try {
							c.eval(".xpoints<-NULL");
							c.eval(".ypoints<-NULL");
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						int u;
						// int pointlength = r.length;
						for (int i = 0; i < r.length; i++) {
							u = i + 1;
							Polygon p = r[i].getPolygon();
							int[] x = p.xpoints;
							int[] y = p.ypoints;
							if (setCrs == false) {
								try {
									c.assign(".xTempVarRoiSelectionCoordinates", x);
									c.assign(".yTempVarRoiSelectionCoordinates", y);
									c.eval("try(.xpoints<-append(.xpoints,.xTempVarRoiSelectionCoordinates))");
									c.eval("try(.ypoints<-append(.ypoints,.yTempVarRoiSelectionCoordinates))");
								} catch (RserveException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (REngineException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							} else {

								/*
								 * Transform the coordinates to the CRS
								 * coordinates!
								 */
								double[] transX = new double[x.length];
								double[] transY = new double[y.length];

								for (int poi = 0; poi < x.length; poi++) {
									/* row=y, column=x ! */
									if (centroidCoords == false) {
										transX[poi] = getTransformedCoordsX(y[poi], x[poi]);
										transY[poi] = getTransformedCoordsY(y[poi], x[poi]);
									}

									else {
										transX[poi] = getTransformedCentroidCoordsX(y[poi], x[poi]);
										transY[poi] = getTransformedCentroidCoordsY(y[poi], x[poi]);
									}
								}
								try {
									c.assign(".xTempVarRoiSelectionCoordinates", transX);
									c.assign(".yTempVarRoiSelectionCoordinates", transY);
									c.eval("try(.xpoints<-append(.xpoints,.xTempVarRoiSelectionCoordinates))");
									c.eval("try(.ypoints<-append(.ypoints,.yTempVarRoiSelectionCoordinates))");
								} catch (REngineException e) {

									e.printStackTrace();
								}

							}

						}

						try {
							c.eval("try(.xyTempVarPoints<-cbind(.xpoints,.ypoints))");
							if (setDf == false) {
								if (setCrs == false) {
									c.eval("try(spatialPoints<-SpatialPoints(.xyTempVarPoints))");
								} else {
									c.eval("try(spatialPoints<-SpatialPoints(.xyTempVarPoints,proj4string=CRS(\"" + proj4String + "\")))");
								}
							} else {
								if (setCrs == false) {
									c.eval("try(spatialPointsDataFrame<-SpatialPointsDataFrame(.xyTempVarPoints," + selDataframe + "))");
								} else {
									c.eval("try(spatialPointsDataFrame<-SpatialPointsDataFrame(.xyTempVarPoints," + selDataframe + ",proj4string=CRS(\"" + proj4String + "\")))");
								}
							}
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}

				} else {
					Bio7Dialog.message("No Rserve connection available!");
				}
			} else {
				Bio7Dialog.message("No ROI Manager available!");
			}

		} else {
			Bio7Dialog.message("No image available!");
		}
	}

	public void startExport(int selection) {

		if (RServe.getConnection() != null) {
			c = RServe.getConnection();
			REXPLogical bolExists = null;
			try {
				bolExists = (REXPLogical) c.eval("require(sp)");

			} catch (RserveException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			boolean[] bolE = bolExists.isTRUE();

			if (bolE[0]) {

				try {
					// c.eval("library(maptools)");
					c.eval("library(sp)");
				} catch (RserveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				exportShapeGeometry(selection);

			} else {
				Bio7Dialog.message("Can't load 'sp' package!");
			}
		} else {
			Bio7Dialog.message("No Rserve connection available!");
		}
	}

	public void aboutToRun(IJobChangeEvent event) {

	}

	public void awake(IJobChangeEvent event) {

	}

	public void done(IJobChangeEvent event) {

	}

	public void running(IJobChangeEvent event) {

	}

	public void scheduled(IJobChangeEvent event) {

	}

	public void sleeping(IJobChangeEvent event) {

	}

}
