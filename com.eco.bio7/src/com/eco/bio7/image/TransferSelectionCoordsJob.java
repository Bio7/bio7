package com.eco.bio7.image;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import java.awt.Polygon;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RServe;

public class TransferSelectionCoordsJob extends WorkspaceJob implements IJobChangeListener {

	
	protected int selection;
	
	
	private RConnection c;
	

	public TransferSelectionCoordsJob() {
		super("Match progress....");

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Match search is running.....", IProgressMonitor.UNKNOWN);

		// match(monitor);
		exportShape();

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

					/* Get coordinates of all selections and add them to a list (x,y seperately)! */

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
