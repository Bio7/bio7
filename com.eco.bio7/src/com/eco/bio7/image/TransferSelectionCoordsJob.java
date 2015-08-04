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

	public TransferSelectionCoordsJob(boolean transfer, int selection, boolean doSetCRS, boolean doSetDf, String crs, String selectedDf) {
		super("Match progress....");
		this.transferAsList = transfer;
		this.selectedType = selection;
		this.setCrs = doSetCRS;
		this.setDf = doSetDf;
		this.crsString = crs;
		this.selDataframe = selectedDf;
		System.out.println(setCrs);
		System.out.println(setDf);
		System.out.println(crsString);
		System.out.println(selDataframe);

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

				File f = new File(crsString);
				gdal.AllRegister();
				Dataset poDataset = null;
				try {
					poDataset = (Dataset) gdal.Open(f.getAbsolutePath(), gdalconst.GA_ReadOnly);
					if (poDataset == null) {
						System.out.println("The image could not be read.");

					}
					
					else{
						/* The array which contains the transform parameters! */
						adfGeoTransform = new double[6];
						/*
						 * Put the transform parameters in the given array! See:
						 * http://gdal.org/java/org/gdal/gdal/Dataset.html#
						 * GetGeoTransform%28double[]%29
						 */
						poDataset.GetGeoTransform(adfGeoTransform);
                        /*Get the spatial reference, projection and convert from Wkt to Proj4!*/
						SpatialReference layerProjection = new SpatialReference();
						layerProjection.ImportFromWkt(poDataset.GetProjectionRef());
						System.out.println(layerProjection.ExportToProj4());
						proj4String = layerProjection.ExportToProj4();

						startExport(selectedType);
					}
				} catch (Exception e) {
					System.err.println("Exception caught.");
					System.err.println(e.getMessage());
					e.printStackTrace();

				}
				
			}
            /*Export of selections as a list!*/
			else {
				startExport(selectedType);
			}
		}

		return Status.OK_STATUS;
	}

	public double getTransformedCoordsX(int row, int col) {

		/*
		 * From the java gdal API: In a north up image, geoTransformArray[1] is
		 * the pixel width, and geoTransformArray[5] is the pixel height. The
		 * upper left corner of the upper left pixel is at position
		 * (geoTransformArray[0],geoTransformArray[3]).
		 * 
		 */

		double xp = (adfGeoTransform[1] * col) + (adfGeoTransform[2] * row) + (adfGeoTransform[1] * 0.5) + (adfGeoTransform[2] * 0.5) + adfGeoTransform[0];

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

		double yp = (adfGeoTransform[4] * col) + (adfGeoTransform[5] * row) + (adfGeoTransform[4] * 0.5) + (adfGeoTransform[5] * 0.5) + adfGeoTransform[3];

		return yp;
	}

	/* The export methods for Polygons, Lines and Points! */
	public void exportShape() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp != null) {
			if (RoiManager.getInstance() != null) {
				Roi[] r = RoiManager.getInstance().getRoisAsArray();
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

						c.eval("lx<-list()");
						c.eval("ly<-list()");

						for (int i = 0; i < r.length; i++) {

							Polygon p = r[i].getPolygon();
							int[] x = p.xpoints;
							int[] y = p.ypoints;

							try {
								c.assign("x", x);
								c.assign("y", y);
							} catch (REngineException e) {

								e.printStackTrace();
							}

							c.eval("try(lx[[" + (i + 1) + "]]<-x)");
							c.eval("try(ly[[" + (i + 1) + "]]<-y)");

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
				Roi[] r = RoiManager.getInstance().getRoisAsArray();
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

					/* If polygon export is selected! */
					if (selection == 0) {

						/*
						 * try { c.eval("listpolygons<-NULL");
						 * c.eval("listpolygons<-list()"); } catch
						 * (RserveException e) { // TODO Auto-generated catch
						 * block e.printStackTrace(); }
						 */

						try {
							c.eval("lx<-list()");
							c.eval("ly<-list()");
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
									c.assign("x", x);
									c.assign("y", y);

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
									transX[poi] = getTransformedCoordsX(y[poi], x[poi]);
									transY[poi] = getTransformedCoordsY(y[poi], x[poi]);
								}
								try {
									c.assign("x", transX);
									c.assign("y", transY);
								} catch (REngineException e) {

									e.printStackTrace();
								}
							}

							try {
								c.eval("try(lx[[" + (i + 1) + "]]<-x)");
								c.eval("try(ly[[" + (i + 1) + "]]<-y)");
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
									c.eval("spatialPolygons<-SpatialPolygons(mapply(function(x, y,id) {Polygons(list(Polygon(cbind(x,y))),id)} ,lx,ly,as.character(1:length(lx))))");
								} else {
									c.eval("spatialPolygons<-SpatialPolygons(mapply(function(x, y,id) {Polygons(list(Polygon(cbind(x,y))),id)} ,lx,ly,as.character(1:length(lx))),proj4string=CRS(\"" + proj4String + "\"))");
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
									c.eval("spatialPolygonsDataFrame<-SpatialPolygonsDataFrame(SpatialPolygons(mapply(function(x, y,id) {Polygons(list(Polygon(cbind(x,y))),id)} ,lx,ly,as.character(1:length(lx))))," + selDataframe + ")");
								} else {
									c.eval("spatialPolygonsDataFrame<-SpatialPolygonsDataFrame(SpatialPolygons(mapply(function(x, y,id) {Polygons(list(Polygon(cbind(x,y))),id)} ,lx,ly,as.character(1:length(lx))),proj4string=CRS(\"" + proj4String + "\"))," + selDataframe + ")");
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
							c.eval("lx<-list()");
							c.eval("ly<-list()");
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
									c.assign("x", x);
									c.assign("y", y);

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
									transX[poi] = getTransformedCoordsX(y[poi], x[poi]);
									transY[poi] = getTransformedCoordsY(y[poi], x[poi]);
								}
								try {
									c.assign("x", transX);
									c.assign("y", transY);
								} catch (REngineException e) {

									e.printStackTrace();
								}
							}

							try {
								c.eval("try(lx[[" + (i + 1) + "]]<-x)");
								c.eval("try(ly[[" + (i + 1) + "]]<-y)");
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
									c.eval("spatialLiness<-SpatialLines(mapply(function(x, y,id) {Lines(list(Line(cbind(x,y))),id)} ,lx,ly,as.character(1:length(lx))))");
								} else {
									c.eval("spatialLines<-SpatialLines(mapply(function(x, y,id) {Lines(list(Line(cbind(x,y))),id)} ,lx,ly,as.character(1:length(lx))),proj4string=CRS(\"" + proj4String + "\"))");
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
									c.eval("spatialLinesDataFrame<-SpatialLinesDataFrame(SpatialLines(mapply(function(x, y,id) {Lines(list(Line(cbind(x,y))),id)} ,lx,ly,as.character(1:length(lx))))," + selDataframe + ")");
								} else {
									c.eval("spatialLinesDataFrame<-SpatialLinesDataFrame(SpatialLines(mapply(function(x, y,id) {Lines(list(Line(cbind(x,y))),id)} ,lx,ly,as.character(1:length(lx))),proj4string=CRS(\"" + proj4String + "\"))," + selDataframe + ")");
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
							c.eval("xp<-NULL");
							c.eval("yp<-NULL");
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
									c.assign("x", x);
									c.assign("y", y);
									c.eval("try(xp<-append(xp,x))");
									c.eval("try(yp<-append(yp,y))");
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
									transX[poi] = getTransformedCoordsX(y[poi], x[poi]);
									transY[poi] = getTransformedCoordsY(y[poi], x[poi]);
								}
								try {
									c.assign("x", transX);
									c.assign("y", transY);
									c.eval("try(xp<-append(xp,x))");
									c.eval("try(yp<-append(yp,y))");
								} catch (REngineException e) {

									e.printStackTrace();
								}

							}

						}

						try {
							c.eval("try(xy<-cbind(xp,yp))");
							if (setDf == false) {
								if (setCrs == false) {
									c.eval("try(spatialPoints<-SpatialPoints(xy))");
								} else {
									c.eval("try(spatialPoints<-SpatialPoints(xy,proj4string=CRS(\"" + proj4String + "\")))");
								}
							} else {
								if (setCrs == false) {
									c.eval("try(spatialPointsDataFrame<-SpatialPointsDataFrame(xy," + selDataframe + "))");
								} else {
									c.eval("try(spatialPointsDataFrame<-SpatialPointsDataFrame(xy," + selDataframe + ",proj4string=CRS(\"" + proj4String + "\")))");
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
					c.eval("library(maptools)");
					c.eval("library(sp)");
				} catch (RserveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				exportShapeGeometry(selection);

			} else {
				Bio7Dialog.message("Can't load 'maptools' package!");
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
