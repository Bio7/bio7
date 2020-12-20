package com.eco.bio7.image;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;
import java.awt.Rectangle;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.PlatformUI;

public class ImageMatchTemplateJob extends WorkspaceJob implements IJobChangeListener {

	private Combo combo;
	private ImagePlus imp;
	private ImagePlus impSel;
	private Scale scale;
	protected int selection;
	private boolean cancelled = false;

	public ImageMatchTemplateJob(final Combo combo, final Scale scale) {
		super("Match progress....");
		this.combo = combo;
		this.scale = scale;
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				imp = WindowManager.getCurrentWindow().getImagePlus();
				impSel = WindowManager.getImage(combo.getText());
				selection = scale.getSelection();
			}
		});

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Match search is running.....", imp.getHeight() - impSel.getHeight());

		match(monitor);

		return Status.OK_STATUS;
	}

	private void match(IProgressMonitor monitor) {
		int positionBestRow = 0;
		int positionBestCol = 0;
		double positionBestSAD = 0;
       
		// System.out.println(combo.getText());
		ImageProcessor ip = imp.getProcessor();
		ImageProcessor ipSel = impSel.getProcessor();
		int S_rows = imp.getHeight();
		int S_cols = imp.getWidth();
		int T_rows = impSel.getHeight();
		int T_cols = impSel.getWidth();
		int kT = T_cols * T_rows;
		// Roi roi = imp.getRoi();
		int VALUE_MAX = T_rows * T_cols * 255;
		double minSAD = VALUE_MAX;

		// compute mean and variance of template
		/*
		 * float sumT = 0, sumT2 = 0; for (int j = 0; j < T_rows; j++) { for
		 * (int i = 0; i < T_cols; i++) { float valT = ipSel.getPixelValue(i,
		 * j); sumT += valT; sumT2 += valT * valT; } } float meanT = sumT / kT;
		 * float varianceT = (float) Math.sqrt(sumT2 - kT * meanT * meanT);
		 */

		// loop through the search image
		for (int y = 0; y <= S_rows - T_rows; y++) {
			if (monitor.isCanceled()) {
				try {
					throw new InterruptedException();
				} catch (InterruptedException e) {
					cancelled = true;
				}
			}
			if (cancelled == false) {

				monitor.worked(1);
				for (int x = 0; x <= S_cols - T_cols; x++) {
					double SAD = 0.0;
					// loop through the template image

					for (int i = 0; i < T_rows; i++) {

						for (int j = 0; j < T_cols; j++) {

							int p_SearchIMG = ip.getPixel(x + i, y + j);

							int p_TemplateIMG = ipSel.getPixel(i, j);

							SAD += Math.abs(p_SearchIMG - p_TemplateIMG);
							if (SAD > selection) {
								i = T_rows;
								j = T_cols;
							}

						}
						// System.out.println(SAD);
						// save the best found position
					}
					/*if (SAD < selection) {
						if (cancelled == false) {
							if (RoiManager.getInstance() != null) {

								Roi roiFinal = new Roi(new Rectangle(y, x, impSel.getWidth(), impSel.getHeight()));
								RoiManager roiManager = RoiManager.getInstance();
								roiManager.add(imp, roiFinal, 1);
								roiManager.setVisible(true);
								imp.setRoi(roiFinal);
							} else {
								Roi roiFinal = new Roi(new Rectangle(y, x, impSel.getWidth(), impSel.getHeight()));
								RoiManager roiManager = new RoiManager();
								roiManager.add(imp, roiFinal, 1);
								roiManager.setVisible(true);
							}
						}
						
					}*/
					
					if (minSAD > SAD) {
						minSAD = SAD;

						// give me VALUE_MAX
						positionBestRow = y;
						positionBestCol = x;
						positionBestSAD = SAD;
					}

				}
			}
		}
		if (cancelled == false) {
			if (RoiManager.getInstance() != null) {

				Roi roiFinal = new Roi(new Rectangle(positionBestCol, positionBestRow, impSel.getWidth(), impSel.getHeight()));
				RoiManager roiManager = RoiManager.getInstance();
				roiManager.add(imp, roiFinal, 1);
				roiManager.setVisible(true);
				imp.setRoi(roiFinal);
			} else {
				Roi roiFinal = new Roi(new Rectangle(positionBestCol, positionBestRow, impSel.getWidth(), impSel.getHeight()));
				RoiManager roiManager = new RoiManager();
				roiManager.add(imp, roiFinal, 1);
				roiManager.setVisible(true);
			}
		}
		cancelled = false;
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
