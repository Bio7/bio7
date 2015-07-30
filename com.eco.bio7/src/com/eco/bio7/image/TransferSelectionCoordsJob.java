package com.eco.bio7.image;

import java.awt.Polygon;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
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

	public TransferSelectionCoordsJob(boolean transfer, int selection,boolean doSetCRS,boolean doSetDf,String crs,String selectedDf) {
		super("Match progress....");
		this.transferAsList = transfer;
		this.selectedType = selection;
		this.setCrs=doSetCRS;
		this.setDf=doSetDf;
		this.crsString=crs;
		this.selDataframe=selectedDf;
		System.out.println(setCrs);
		System.out.println(setDf);
		System.out.println(crsString);
		System.out.println(selDataframe);
		
	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {

		monitor.beginTask("Match search is running.....", IProgressMonitor.UNKNOWN);

		if (transferAsList) {
			exportShape();
		} else {
			startExport(selectedType);
		}

		return Status.OK_STATUS;
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
				
						/*try {
							c.eval("listpolygons<-NULL");
							c.eval("listpolygons<-list()");
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}*/
						
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

							try {
								c.assign("x", x);
								c.assign("y", y);
								
							} catch (REngineException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							try {
								c.eval("try(lx[[" + (i + 1) + "]]<-x)");
								c.eval("try(ly[[" + (i + 1) + "]]<-y)");
							} catch (RserveException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							/* Close the ring which is not default by ImageJ! */
							/*try {
								//c.eval("try(x<-append(x,x[1]))");

								//c.eval("try(y<-append(y,y[1]))");

								c.eval("p" + u + "<-Polygon(cbind(x,y))");
								c.eval("try(polygons<-Polygons(list(p" + u + "),\"" + u + "\"))");

								c.eval("try(listpolygons[[" + u + "]]<-polygons)");

								c.eval("remove(p" + u + ")");
								c.eval("remove(polygons" + u + ")");
							} catch (RserveException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
*/
						}
						
						/*A function to convert the list of ROIs to SpatialPolygons!*/
                        if(setDf==false){
						try {
							//c.eval("try(spatpolygons<-SpatialPolygons(listpolygons, 1:length(listpolygons)))");
							c.eval("spatialPolygons<-SpatialPolygons(mapply(function(x, y,id) {Polygons(list(Polygon(cbind(x,y))),id)} ,lx,ly,as.character(1:length(lx))))");
							
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
                        }
                        else{
                        	
                        	try {
    							//c.eval("try(spatpolygons<-SpatialPolygons(listpolygons, 1:length(listpolygons)))");
    							c.eval("spatialPolygonsDataFrame<-SpatialPolygonsDataFrame(SpatialPolygons(mapply(function(x, y,id) {Polygons(list(Polygon(cbind(x,y))),id)} ,lx,ly,as.character(1:length(lx)))),"+selDataframe+")");
    							
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

							try {
								c.assign("x", x);
								c.assign("y", y);
								
							} catch (REngineException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							try {
								c.eval("try(lx[[" + (i + 1) + "]]<-x)");
								c.eval("try(ly[[" + (i + 1) + "]]<-y)");
							} catch (RserveException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

						

						}
						
						/*A function to convert the list of ROIs to SpatialLines!*/
						 if(setDf==false){
						try {
							//c.eval("try(spatpolygons<-SpatialPolygons(listpolygons, 1:length(listpolygons)))");
							c.eval("spatialLines<-SpatialLines(mapply(function(x, y,id) {Lines(list(Line(cbind(x,y))),id)} ,lx,ly,as.character(1:length(lx))))");
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						 }
						 
						 else{
							 try {
									//c.eval("try(spatpolygons<-SpatialPolygons(listpolygons, 1:length(listpolygons)))");
								 c.eval("spatialLinesDataFrame<-SpatialLinesDataFrame(SpatialLines(mapply(function(x, y,id) {Lines(list(Line(cbind(x,y))),id)} ,lx,ly,as.character(1:length(lx)))),"+selDataframe+")");
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
						//int pointlength = r.length;
						for (int i = 0; i < r.length; i++) {
							u = i + 1;
							Polygon p = r[i].getPolygon();
							int[] x = p.xpoints;
							int[] y = p.ypoints;

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

						}
						try {
							c.eval("try(xy<-cbind(xp,yp))");
							 if(setDf==false){
							c.eval("try(spatialPoints<-SpatialPoints(xy))");
							 }
							 else{
								 c.eval("try(spatialPointsDataFrame<-SpatialPointsDataFrame(xy,"+selDataframe+"))");
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
