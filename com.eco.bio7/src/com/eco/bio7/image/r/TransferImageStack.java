package com.eco.bio7.image.r;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.image.ImageMethods;
import com.eco.bio7.image.RImageMethodsView;

public class TransferImageStack extends WorkspaceJob {

	protected int type = 0;
	private RConnection c;
	private boolean convert;
	private REXPLogical bolExists;
	private boolean[] bolE;

	public TransferImageStack(String name, RConnection c, boolean convert) {
		super(name);
		this.c = c;
		this.convert = convert;
		// TODO Auto-generated constructor stub
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {

		ImagePlus imp = WindowManager.getCurrentImage();
		int size = 1;
		if (imp != null) {
			size = imp.getStackSize();
		}
		if (size > 1) {
			monitor.beginTask("Transfer Image Stack.....", size);
			transferStack(monitor,imp);
		}
		return Status.OK_STATUS;
	}

	public void transferStack(IProgressMonitor monitor,ImagePlus imp) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				type = RImageMethodsView.getTransferTypeCombo().getSelectionIndex();
			}
		});

		int slice = 1;

		// Calculate the stack size!
		
		if (imp != null) {
			int size = imp.getStackSize();
			int width = imp.getWidth();
			int height = imp.getHeight();

			if (convert == true) {
				if (type == 2) {

					Bio7Dialog.message("Raw type not supported in raster!");

				} else {
					try {
						bolExists = (REXPLogical) c.eval("try(require(raster))");
						bolE = bolExists.isTRUE();

					} catch (RserveException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					if (bolE[0] == true) {

						try {
							
							c.eval("rasterStackFromIJ<-stack()");
						} catch (RserveException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						while (slice <= size) {
							
							monitor.setTaskName("Transfer image number: " + slice);
							IJ.run("Set Slice...", "slice=" + slice);
							/*
							 * Transfer as integer! ->0=double, 1=integer,
							 * 2=byte, 3=RGB byte
							 */

							ImageMethods.imageToR("imageMatrix", true, type, null);
							try {

								if (type == 3) {
									// If a RGB transfer!
									c.eval("try(themat1<-matrix(imageMatrix[,1],nrow=" + width + ", ncol=" + height + "))");
									c.eval("try(rasterStackFromIJ <- stack(rasterStackFromIJ,raster(themat1)))");

									c.eval("try(themat2<-matrix(imageMatrix[,2],nrow=" + width + ", ncol=" + height + "))");
									c.eval("try(rasterStackFromIJ <- stack(rasterStackFromIJ,raster(themat2)))");

									c.eval("try(themat3<-matrix(imageMatrix[,3],nrow=" + width + ", ncol=" + height + "))");
									c.eval("try(rasterStackFromIJ <- stack(rasterStackFromIJ,raster(themat3)))");

									// c.eval("try(rasterStackFromIJ <- stack(rasterStackFromIJ,raster(themat1),raster(themat2),raster(themat3)))");

								} else {

									c.eval("try(rasterStackFromIJ <- stack( rasterStackFromIJ , raster(imageMatrix)))");
								}
								// converts the matrix list to a raster list and
								// then
								// the list to a raster stack!
								// c.eval("rasterStackFromIJ <- do.call(stack,lapply(matrixList,raster))");
							} catch (RserveException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							slice++;
							monitor.worked(1);
						}

					} else {

						Bio7Dialog.message("Raster package not loaded!");
						return;
					}

				}
			}

			else {

				try {

					c.eval("matrixList <- list()");

				} catch (RserveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// Calculate the stack size!

				while (slice <= size) {
					monitor.setTaskName("Transfer image number: " + slice);
					IJ.run("Set Slice...", "slice=" + slice);
					/*
					 * Transfer as integer! ->0=double, 1=integer, 2=byte, 3=RGB
					 * byte
					 */

					ImageMethods.imageToR("imageMatrix", true, type, null);
					
						try {
							// RServe.print("typeof(matrix)");
							c.eval("try(matrixList[[" + (slice) + "]]<-imageMatrix)");
							// lapply(matrixList,image) //plot the list of
							// matrices

						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					

					slice++;
					monitor.worked(1);
				}

			}
		} else {
			Bio7Dialog.message("No image opened in ImageJ!");
		}
	}

}
