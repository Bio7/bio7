package com.eco.bio7.image.r;

import java.awt.Polygon;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.discrete.Field;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.image.ImageMethods;
import com.eco.bio7.image.PointPanel;
import com.eco.bio7.image.PointPanelView;
import com.eco.bio7.rbridge.RServe;

/**
 * @author M. Austenfeld A class to transfer the results of an ImageJ Particle
 *         analysis.
 */
public class IJTranserResultsTable {

	public void transferResultsTable(RConnection con, boolean dialog) {
		ResultsTable rt = Analyzer.getResultsTable();
		if (rt == null || ResultsTable.getResultsWindow() == null) {
			String mess = "No ImageJ Results Table";
			if (dialog) {

				Bio7Dialog.message(mess);
			} else {
				System.out.println(mess);

			}

		} else {

			if (rt.getColumn(0) != null) {
				try {
					con.eval("try(IJResultsTable<-data.frame(1:" + rt.getColumn(0).length + "))");
				} catch (RserveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				int size = rt.getLastColumn();
				for (int i = 0; i <= size; i++) {
					if (rt.getColumn(i) != null) {
						// System.out.println(rt.getColumnHeading(i) +
						// "::::::::");
						double[] col = rt.getColumnAsDoubles(i);
						String columnName = rt.getColumnHeading(i);
						if (columnName.equals("%Area")) {
							columnName = columnName.replace("%Area", "Area_Prozent");
						}	
						
						/*Replace all special characters except '_' and '.'!
						 *This is necessary for customized Results table columns!*/
						columnName =columnName.replaceAll("[^a-zA-Z0-9_.]+","_");
						try {
							con.assign(columnName, col);
						} catch (REngineException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						try {
							con.eval("try(IJResultsTable<-data.frame(IJResultsTable," + columnName + "))");
							con.eval("remove(" + columnName + ")");
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						for (int j = 0; j < col.length; j++) {
							// System.out.println("column Nr: "+i+":"+col[j]+": ");
						}
					}
				}

				try {
					con.eval("try(IJResultsTable[1]<-NULL)");
				} catch (RserveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				// Bio7Dialog.message("No Results! Image Thresholded");
			}

		}
	}

	public void pointsToR(RConnection d) {
		Roi roi = null;

		if (d != null) {
			ImagePlus plu=WindowManager.getCurrentImage();
			double xydia[][] = PointPanel.pointToArray();
			if (plu != null) {
				
				/* Get the image processor of the image ! */
				ImageProcessor ip = plu.getProcessor();
				int w = ip.getWidth();
				int h = ip.getHeight();

				try {
					d.eval("imageSizeY<-" + h);

					d.eval("imageSizeX<-" + w);

				} catch (RserveException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				
				
				roi = plu.getRoi();
				int[] roix = null;
				int[] roiy = null;
				if (roi != null) {
					Polygon po = roi.getPolygon();
					roix = po.xpoints;
					roiy = po.ypoints;

					try {
						d.assign("selectionx", roix);

						d.assign("selectiony", roiy);
					} catch (REngineException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}

			try {
				d.assign("x", xydia[0]);

				d.assign("y", xydia[1]);
				d.assign("diameter_panel", xydia[2]);
				d.assign("species", xydia[3]);
				d.assign("alpha", xydia[4]);
			} catch (REngineException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				d.voidEval("fieldx<-" + ImageMethods.getFieldX() + "");

				d.voidEval("fieldy<-" + ImageMethods.getFieldY() + "");
			} catch (RserveException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// }

			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {

				public void run() {
					MessageBox messageBox = new MessageBox(new Shell(),

					SWT.ICON_INFORMATION);
					messageBox.setMessage("Transferred values to R");
					messageBox.open();
				}
			});
			xydia = null;

		} else {

			MessageBox messageBox = new MessageBox(new Shell(),

			SWT.ICON_INFORMATION);
			messageBox.setMessage("RServer connection failed - Server is not running !");
			messageBox.open();

		}

	}

	public static void addParticleValues() {
		PointPanel jp = PointPanelView.getJp();
		MessageBox message = new MessageBox(new Shell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		message.setMessage("Do you want to delete plants in the Points panel?");
		message.setText("Delete Plants");
		int response = message.open();
		if (response == SWT.YES) {

			jp.getVe().clear();
			jp.get_Points().clear();
			jp.get_Species().clear();
			jp.get_Alpha().clear();

			//
		}

		SwingUtilities.invokeLater(new Runnable() {
			// !!
			public void run() {

				setParticles(jp);
			}
		});
		jp.repaint();
		// Bio7ImageJAnalyse.clearList(); Options in class Analyzer regarded!
	}

	private static void setParticles(PointPanel jp) {

		Double cmx = 0.0;

		Double cmy = 0.0;

		IJ.runMacro("run(\"Analyze Particles...\")");

		// IJ.runMacro("run(\"Analyze Particles...\", \"size=0-Infinity circularity=0.00-1.00 show=Nothing display clear stack\")");
		// System.out.println(rt.getColumnHeadings());
		ResultsTable rt = Analyzer.getResultsTable();
		if (rt == null) {
			Bio7Dialog.message("No ImageJ Results Table");
		} else {

			if (rt.getColumn(0) != null) {

				// System.out.println(rt.getColumnHeading(i) +
				// "::::::::");
				int x = rt.getColumnIndex("X");
				int y = rt.getColumnIndex("Y");

				double[] xcol = rt.getColumnAsDoubles(x);
				double[] ycol = rt.getColumnAsDoubles(y);
				if (xcol != null && ycol != null) {
					//System.out.println("xxxxxx   " + x);

					for (int j = 0; j < xcol.length; j++) {
						//System.out.println(xcol[j]);
						cmx = xcol[j] * ImageMethods.getPointScale();
						/* Values with scale for precision ! */
						cmy = ycol[j] * ImageMethods.getPointScale();

						jp.getVe().add(new Ellipse2D.Double(cmx - (int) (jp.get_Diameter() / 2), cmy - (int) (jp.get_Diameter() / 2), jp.get_Diameter(), jp.get_Diameter()));
						jp.get_Points().add(new Point2D.Double(cmx, cmy));

						jp.get_Species().add(PointPanel.getPlantIndexPanel());

						jp.get_Alpha().add(PointPanel.getCompos3());
						jp.setCount(jp.getCount() + 1);
						if (PointPanel.isQuad2d_visible()) {
							if (cmx < (Field.getWidth() * Field.getQuadSize()) && cmy < (Field.getHeight() * Field.getQuadSize())) {
								try {
									Quad2d.getQuad2dInstance().setquads(cmx, cmy);
								} catch (RuntimeException e) {

									e.printStackTrace();
								}
							}
						}

					}
					if (ResultsTable.getResultsWindow() != null) {
						ResultsTable.getResultsWindow().close(false);
					}

				} else {
					if (ResultsTable.getResultsWindow() != null) {
						ResultsTable.getResultsWindow().close(false);
					}
					Bio7Dialog.message("Please select 'Centroid' in the 'Set Measurements' dialog");
				}
			}
		}
		PointPanel.doPaint();
	}

	public void particledescriptors() {
		Roi roi = null;
		RConnection d = RServe.getConnection();
		ImagePlus currentImage = WindowManager.getCurrentImage();
		if (currentImage != null) {
			if (d != null) {

				try {
					d.voidEval("imageSizeY<-" + currentImage.getHeight() + "");
					d.voidEval("imageSizeX<-" + currentImage.getWidth() + "");
				} catch (RserveException e1) {

				}
				roi = currentImage.getRoi();
				int[] roix = null;
				int[] roiy = null;
				if (roi != null) {
					Polygon po = roi.getPolygon();
					roix = po.xpoints;
					roiy = po.ypoints;

					try {
						d.assign("selectionx", roix);

						d.assign("selectiony", roiy);
					} catch (REngineException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				runParticleAnalysis(d, null);
				Bio7Dialog.message("Particles transferred!");
			} else {

				Bio7Dialog.message("Rserve is not alive!");
			}
		} else {
			Bio7Dialog.message("No image opened!");
		}

	}

	/**
	 * A method to transfer results from a particle analysis in ImageJ.
	 * 
	 * @param con
	 *            a Rserve connection.
	 * @param macro
	 *            an optional macro.
	 */
	public static void runParticleAnalysis(RConnection con, String macro) {
		if (macro == null) {
			IJ.runMacro("run(\"Analyze Particles...\")");
		} else {

			IJ.runMacro(macro);
		}
		// IJ.runMacro("run(\"Analyze Particles...\", \"size=0-Infinity circularity=0.00-1.00 show=Nothing display clear stack\")");
		// System.out.println(rt.getColumnHeadings());
		ResultsTable rt = Analyzer.getResultsTable();
		TextWindow resultsWindow = ResultsTable.getResultsWindow();
		if (rt == null || resultsWindow == null) {
			System.out.println("No ImageJ Results Table");
		} else {

			if (rt.getColumn(0) != null) {
				try {
					con.eval("try(Particles<-data.frame(1:" + rt.getColumn(0).length + "))");
				} catch (RserveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				int size = rt.getLastColumn();
				for (int i = 0; i <= size; i++) {
					if (rt.getColumn(i) != null) {
						// System.out.println(rt.getColumnHeading(i) +
						// "::::::::");
						double[] col = rt.getColumnAsDoubles(i);
						String columnName = rt.getColumnHeading(i);
						if (columnName.equals("%Area")) {
							columnName = columnName.replace("%Area", "Area_Prozent");
						}
						try {
							con.assign(columnName, col);
						} catch (REngineException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						try {
							con.eval("try(Particles<-data.frame(Particles," + columnName + "))");
							con.eval("remove(" + columnName + ")");
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						for (int j = 0; j < col.length; j++) {
							// System.out.println("column Nr: "+i+":"+col[j]+": ");
						}
					}
				}
				if (resultsWindow != null) {
					resultsWindow.close(false);
				}
				try {
					con.eval("try(Particles[1]<-NULL)");
				} catch (RserveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				// Bio7Dialog.message("No Results! Image Thresholded");
			}

		}
	}

}
