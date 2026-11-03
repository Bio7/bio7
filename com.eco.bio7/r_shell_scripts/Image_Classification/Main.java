
/********************************************************************************
 * Copyright (c) 2020 Marcel Austenfeld
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 ********************************************************************************/

import static com.eco.bio7.image.ImageMethods.imageFeatureStackToR;
import static com.eco.bio7.rbridge.RServeUtil.evalRScript;
import static com.eco.bio7.rbridge.RServeUtil.listRObjects;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.batch.FileRoot;
import com.eco.bio7.collection.CustomView;
import com.eco.bio7.collection.Work;
import com.eco.bio7.image.RImageMethodsView;
import com.eco.bio7.util.Util;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import _util.Filters;
import _util.Haralick;
import _util.Texture;
import _util.ImageToRTransfer;
import _util.Kuwahara_Filter;
import _util.Lipschitz_;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.ImageRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.RoiListener;
import ij.plugin.ChannelSplitter;
import ij.plugin.Duplicator;
import ij.plugin.filter.GaussianBlur;
import ij.plugin.filter.RankFilters;
import ij.plugin.frame.RoiManager;
import ij.process.ColorProcessor;
import ij.process.ColorSpaceConverter;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

/*
 * A Bio7 Graphical User Interface for supervised and unsupervised classification.
 */
public class Main extends com.eco.bio7.compile.Model {

	private String[] files;
	private _ModelGui gui;
	protected int transferType;
	protected String currentFilePathMultipleDialog;
	protected IProgressMonitor actionMonitor;
	private boolean jobDone = true;
	private Job previewJob;
	protected static String classifyDir;

	public Main() {
		/*
		 * Call a method at startup to remove old ROI listeners if you recompile this
		 * plugin and a listener is still active!
		 */
		removeRoiListenerAtStartup();
		/* Create the GUI interface! */
		CustomView view = new CustomView();

		Display display = Util.getDisplay();

		display.syncExec(() -> {
			Composite parent = view.getComposite("Classification");
			/*
			 * Create the GUI and transfer a reference to this class that the GUI can
			 * execute methods from this class!
			 */
			gui = new _ModelGui(parent, Main.this, SWT.NONE);

			parent.layout(true);
		});
	}

	/* Called from the GUI class! */
	public void executeSelection(int choice) {

		Job job = new Job("Classification Process") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				actionMonitor = monitor;
				monitor.beginTask("Started selected action ...", IProgressMonitor.UNKNOWN);
				/* We create a feature stack. R connection not necessary! */
				if (choice == 2) {
					action(choice, monitor);
				}

				else {
					if (RServe.isAliveDialog()) {
						if (RState.isBusy() == false) {
							/* Notify that R is busy! */
							RState.setBusy(true);
							action(choice, monitor);
						} else {
							System.out.println("RServer is busy. Can't execute the R script!");
						}

					}
				}
				monitor.done();
				return Status.OK_STATUS;
			}

		};
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {

					RState.setBusy(false);
					/* Update the R-Shell view workspace objects! */
					if (RServe.isAlive()) {
						listRObjects();
					}
				} else {

					RState.setBusy(false);
				}
			}
		});

		job.schedule();

	}

	public void action(int choice, IProgressMonitor monitor) {
		/*
		 * Important call to get the features and feature settings from the GUI
		 * (syncExec wrapped for SWT)!
		 */
		gui.getFeatureOptions();

		/* Create feature stack! */
		if (choice == 1) {
			// String files = Bio7Dialog.openFile();
			// if (files != null) {
			ImagePlus image = WindowManager.getCurrentImage();
			if (image == null) {
				Bio7Dialog.message("Please open an image in ImageJ!");
				return;
			}

			/*
			 * Here we add a dialog to check if really only the ROI Manager selections
			 * should be transferred! It happens that a selection is present in the ROI
			 * Manager but it was intended to transfer all ROI's. Here a little workflow
			 * help!
			 */

			RoiManager mInstance = RoiManager.getInstance();
			if (mInstance == null) {
				/* If ROI Manager isn't active! */
				Bio7Dialog.message("Please open and add selections to the ROI Manager!");
				return;
			}
			Roi[] r = mInstance.getSelectedRoisAsArray();

			if (r.length < 1) {
				Bio7Dialog.message("NO ROI's available in ROI Manager!");
				return;
			}
			Roi[] r2 = mInstance.getRoisAsArray();
			if (r.length < r2.length) {
				boolean all = Bio7Dialog.decision("Selections in ROI Manager exists!\n"
						+ "Only the selected ROI's in the ROI Manager will be transferred!\n\n"
						+ "Press 'Yes' if you want to transfer the selected ROI's only!\n"
						+ "Press 'No' to transfer all ROI's in the ROI Manager!");
				if (all == false) {
					mInstance.deselect();
				}
			}

			ImagePlus imPlus = createStackFeatures(null, image, monitor);
			if (gui.openStack) {
				imPlus.show();
				gui.layout();
			}
			monitor.setTaskName("Transfer Feature data to R");
			int typeTransfer = gui.transferType;
			if (gui.useGroups) {
				/* Special method to transfer ROI Manager ROI's with a signature to R! */
				ImageToRTransfer.imageFeatureStackSelectionToR(imPlus, typeTransfer, 1, imPlus.getStackSize(), true);
			} else {
				ImageToRTransfer.imageFeatureStackSelectionToR(imPlus, typeTransfer, 1, imPlus.getStackSize(), false);

			}

			// }

		}
		/* Create ROI Classes! */
		else if (choice == 2) {
			ImagePlus image = WindowManager.getCurrentImage();
			if (image == null) {
				Bio7Dialog.message("Please open an image in ImageJ to create feature selections!");
				return;
			}
			Bio7Dialog.message("Add selections (ROI's = specific class) to the ROI Manager.\n\n"
					+ "To set a class signature:\n\n" + "Option 1 (default):\n"
					+ "Rename the ROI's to identify the classes - at the end of the string use an underscore followed by the class"
					+ "number (class_1, class_2 or cell_1, cell_2..., etc.).\n\n" + "Option 2:\n"
					+ "Enable the 'Use Group Signature' option in the 'Settings tab' to set signatures according "
					+ "to the group membership (set ROI Groups, e.g., with the ImageJ toolbar action)!");
			/* Opens the ROI Manager of ImageJ! */
			IJ.run("ROI Manager...", "");

		}

		/* Train Classifier with external script! */
		else if (choice == 3) {
			/*
			 * We have set the busy variable to false to use the R-Shell selection in this
			 * job!
			 */
			RState.setBusy(false);
			Work.openView("com.eco.bio7.RShell");
			Bio7Dialog.selection("Select training features (classes) in R-Shell!\n\n"
					+ "Select multiple with STRG (CMD)+MouseClick or SHIFT+MouseClick!\n\nPress 'OK' when selected to execute the training R script!");
			/* Set the busy variable again to true because now we call R! */
			RState.setBusy(true);
			monitor.setTaskName("Apply Training Script");
			String path = gui.getPathTrainingScript();
			/* Execute R script! */
			if (path.endsWith(".R")) {
				System.out.println("");
				evalRScript(path);
			}

		}

		/* Classify selected images with external Script! */
		else if (choice == 4) {
			gui.interruptBatch = false;
			if (gui.useDirectoryDialog) {
				String dirSelection = directory("Select the base directory");
				if (dirSelection == null) {
					return;
				}
				File dir = new File(dirSelection);
				/* If you need only certain image types! */
				// final String[] ext = { "tif", "tiff", "dcm", "png" };
				List<File> files = (List<File>) FileUtils.listFiles(dir, null, true);
				// Only directories:
				// FileUtils.listFilesAndDirs(new File(dir), new
				// NotFileFilter(TrueFileFilter.INSTANCE), DirectoryFileFilter.DIRECTORY)
				for (int i = 0; i < files.size(); i++) {
					if (gui.interruptBatch) {
						break;
					}
					File file = files.get(i);

					String currentFile = null;
					try {
						currentFile = file.getCanonicalPath();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					ImagePlus imPlus = createStackFeatures(currentFile, null, monitor);
					// System.out.println(choice);

					classify(monitor, imPlus);
				}
			} else {
				String[] files = openMultipleFiles();
				if (files == null) {
					return;
				}
				for (int i = 0; i < files.length; i++) {

					if (gui.interruptBatch) {
						break;
					}

					ImagePlus imPlus = createStackFeatures(files[i], null, monitor);

					classify(monitor, imPlus);

				}

			}

		}

	}

	private void classify(IProgressMonitor monitor, ImagePlus imPlus) {
		/* Correct some image names for R! */
		String name = imPlus.getTitle();
		String nameCorrected = "current_feature_stack";
		/* Set the data type for the R transfer from the Bio7 GUI! */
		int transferType = getRDataTransferType();
		/* Transfer the feature stack to R in the selected datatype! */
		imageFeatureStackToR(nameCorrected, transferType, imPlus);
		imPlus = null;
		monitor.setTaskName("Apply Classification Script");
		/* Predict in R (evalRScript is a custom method) with the randomForest model! */
		String path = gui.getPathClassificationScript();
		if (path.endsWith(".R")) {
			evalRScript(path);
			// imageFromR(3, "imageMatrix", 1);
			if (gui.showClassifiedInImageJ) {
				ImagePlus imageClassified = ImageToRTransfer.imageFromR("imageMatrix");
				if (imageClassified != null) {
					imageClassified.setTitle(name + "_Classified");
					imageClassified.show();
					gui.layout();
				}
			}
		}
		if (gui.applyPostImageJMacro) {
			String macroPath = gui.getPathImageJMacroPostScript();
			IJ.runMacroFile(macroPath);
		}

	}

	public void classifyPreview(IProgressMonitor monitor, ImagePlus imPlus, Roi roi) {
		/* Correction of name here not necessary! */
		// String name = imPlus.getTitle();
		String nameCorrected = "current_feature_stack";
		/* Set the data type for the R transfer from the Bio7 GUI! */
		int transferType = getRDataTransferType();
		/* Transfer the feature stack to R in the selected datatype! */
		imageFeatureStackToR(nameCorrected, transferType, imPlus);
		imPlus = null;
		if (monitor != null) {
			monitor.setTaskName("Apply Classification Preview");
		}
		/* Predict in R (evalRScript is a custom method) with the randomForest model! */
		String path = gui.getPathClassificationScript();
		if (path.endsWith(".R")) {
			evalRScript(path);

			if (roi != null) {
				Rectangle bounds = roi.getBounds();
				int x = bounds.x;
				int y = bounds.y;
				ImagePlus impOverlay = ImageToRTransfer.imageFromR("imageMatrix");
				if (impOverlay != null) {
					/* Apply the LUT option! */
					IJ.run(impOverlay, gui.lutOption, "");
					ImageRoi imageRoi = new ImageRoi(x, y, impOverlay.getProcessor());

					/* Parse and apply the opacity option! */
					imageRoi.setOpacity(Double.parseDouble(gui.opacityOption));
					// imageRoi.setZeroTransparent(true);
					Overlay overlay = new Overlay(imageRoi);
					ImagePlus iDisplayed = WindowManager.getCurrentImage();
					iDisplayed.setOverlay(overlay);
					// Font font = new Font("Arial", Font.PLAIN, 18);
					// TextRoi roiText = new TextRoi(x, y, "Preview", font);
					// roiText.setStrokeColor(new Color(1, 0, 0));
					// overlay.add(roiText);
					// impOverlay.show();
					// iDisplayed.setRoi(imageRoi);
				}
			}

		}

	}

	public ImagePlus createStackFeatures(String files, ImagePlus sourceImage, IProgressMonitor monitor) {
		ImagePlus imPlus = null;
		ImagePlus image = null;
		ImageStack stack = null;

		/*
		 * If we want to use an import macro, e.g., using the BioFormats library
		 * commands which can be recorded with the ImageJ macro recorder!
		 */
		if (gui.useImportMacro) {
			/* Multiple files selected! */
			if (files != null) {
				/* If we used the directory dialog to open all files! */
				if (gui.useDirectoryDialog) {
					IJ.runMacroFile(gui.getMacroTextOption(), files);
				} else {
					/*
					 * currentFilePathMultipleDialog is the path to the directory which we need for
					 * the multiple files dialog which returns only the filenames!
					 */
					IJ.runMacroFile(gui.getMacroTextOption(), currentFilePathMultipleDialog + "/" + files);
				}

			}
			// else {
			/* Call ImageJ macro with option (file path)! */
			// IJ.runMacroFile(gui.getMacroTextOption(), singleFile);
			// }

		} else {
			/* Multiple files selected! */
			if (files != null) {
				/* If we used the directory dialog to open all files! */
				if (gui.useDirectoryDialog) {
					image = IJ.openImage(files);
				} else {
					/*
					 * currentFilePathMultipleDialog is the path to the directory which we need for
					 * the multiple files dialog which returns only the filenames!
					 */
					image = IJ.openImage(currentFilePathMultipleDialog + "/" + files);
				}

			} else {
				// if (singleFile != null) {
				// image = IJ.openImage(singleFile);
				// } else {
				image = sourceImage;
				// }
			}
		}
		/*
		 * We must avoid a null reference when using the BioFormats library with the
		 * macro import option!
		 */
		if (gui.useImportMacro && image == null) {
			image = WindowManager.getCurrentImage();
		}

		/* If we have a RGB! */
		if (image.getProcessor() instanceof ColorProcessor) {
			/* Convert to HSB! */
			if (gui.toHsb) {
				if (monitor != null) {
					monitor.setTaskName("Convert RGB To HSB Color Space");
				}
				/* Duplicate the image! */
				Duplicator duplicator = new Duplicator();
				/* Duplicate original for the HSB channels! */
				ImagePlus impToHasb = duplicator.run(image);
				ImageConverter con = new ImageConverter(impToHasb);
				con.convertToHSB32();

				String opt = gui.channelOption;
				String[] channelToInclude = opt.split(",");
				ImageStack hsbStack = impToHasb.getStack();

				if (opt.isEmpty() == false && channelToInclude.length > 0) {
					/* Create a feature stack from selected HSB channels! */
					stack = new ImageStack(impToHasb.getWidth(), impToHasb.getHeight());

					for (int j = 0; j < channelToInclude.length; j++) {
						/* Add selected HSB float channels to the stack! */
						int sel = Integer.parseInt(channelToInclude[j]);
						/* Use selected slices! */
						ImageProcessor floatProcessor = hsbStack.getProcessor(sel);
						stack.addSlice("Channel_" + j, floatProcessor);
					}
				} else {
					/* Use all slices. Already Float! */
					stack = impToHasb.getStack();
				}

			}
			/* Convert to LAB! */
			else if (gui.toLab) {
				if (monitor != null) {
					monitor.setTaskName("Convert RGB To LAB Color Space");
				}
				/* Duplicate the image! */
				Duplicator duplicator = new Duplicator();
				/* Duplicate original for the LAB channels! */
				ImagePlus impToLab = duplicator.run(image);
				ColorSpaceConverter converter = new ColorSpaceConverter();
				ImagePlus imp = converter.RGBToLab(impToLab);
				// imp.show();
				// image.hide();
				imp.copyAttributes(impToLab);
				// image.changes = false;
				// image.close();
				String opt = gui.channelOption;
				String[] channelToInclude = opt.split(",");
				ImageStack labStack = imp.getStack();

				if (opt.isEmpty() == false && channelToInclude.length > 0) {
					/* Create a LAB stack from selected channels! */
					stack = new ImageStack(imp.getWidth(), imp.getHeight());
					for (int j = 0; j < channelToInclude.length; j++) {
						/* Add LAB channels to the stack. LAB stack are already float images! */
						int sel = Integer.parseInt(channelToInclude[j]);
						/* Use selected slices! */
						ImageProcessor floatProcessor = labStack.getProcessor(sel);
						stack.addSlice("Channel_" + j, floatProcessor);
					}
				} else {
					/* Use all slices. LAB stack are already float images! */
					stack = imp.getStack();
				}

			}

			else {

				/* Split original to R,G,B channels! */
				ImagePlus[] channels = ChannelSplitter.split(image);

				String opt = gui.channelOption;
				String[] channelToInclude = opt.split(",");

				/* Create a feature stack from all available channels (e.g., R,G,B) images! */
				stack = new ImageStack(image.getWidth(), image.getHeight());
				if (opt.isEmpty() == false && channelToInclude.length > 0) {
					for (int j = 0; j < channelToInclude.length; j++) {
						/* Add selected RGB channels to the new stack! */
						/* Convert original to float to have a float image stack for the filters! */
						int sel = Integer.parseInt(channelToInclude[j]) - 1;// Channels index starts with 0 so we
																			// correct here with -1!
						ImageProcessor floatProcessor = channels[sel].getProcessor().convertToFloat();
						stack.addSlice("Channel_" + j, floatProcessor);
					}

				} else {
					for (int j = 0; j < channels.length; j++) {
						/* Add RGB channels to the stack! */
						/* Convert original to float to have a float image stack for the filters! */
						ImageProcessor floatProcessor = channels[j].getProcessor().convertToFloat();
						stack.addSlice("Channel_" + j, floatProcessor);
					}
				}
			}
		} else {/* Grayscale images (8-bit, 16-bit, 32-bit) */
			/* If we have a grayscale stack! */
			if (image.getStackSize() > 1) {
				String opt = gui.channelOption;
				String[] channelToInclude = opt.split(",");
				/* Check if we only want to include certain slices! */
				if (opt.isEmpty() == false && channelToInclude.length > 0) {
					stack = new ImageStack(image.getWidth(), image.getHeight());
					for (int j = 0; j < channelToInclude.length; j++) {
						/* Add selected slices to a new stack! */
						int sel = Integer.parseInt(channelToInclude[j]);// Stack starts with index 1 no correction
																		// necessary!
						stack.addSlice("Grayscale_Layer_" + j, image.getStack().getProcessor(sel).convertToFloat());
					}
				} else {
					/* Convert original to float to have a float image stack for the filters! */
					stack = image.getStack().convertToFloat();
				}
			} else {

				stack = new ImageStack(image.getWidth(), image.getHeight());
				/* Convert original to float to have a float image stack for the filters! */
				stack.addSlice("Grayscale", image.getProcessor().convertToFloat());
			}
		}

		/*
		 * Duplicate the base stack as basis for the different filters! the filtered
		 * images will be added to the base stack!
		 */

		ImageStack tempStack = stack.duplicate();
		/* The quantization ranges of the base layers for the Haralick texture features */
		double[][] ranges = quantizationRanges(image, tempStack);
		/*
		 * The filters are independent tasks which are computed in parallel. The
		 * layers are added in the order of the tasks (the same order and names as
		 * before)!
		 */
		List<FeatureTask> tasks = new ArrayList<FeatureTask>();
		int stackSize = tempStack.getSize();
		/*
		 * See:
		 * https://imagej.nih.gov/ij/developer/api/ij/plugin/filter/GaussianBlur.html#
		 * blur-ij.process.ImageProcessor-double-
		 */
		if (gui.gaussian) {
			/* Split the gaussian option to get all sigmas! */
			String[] gaussianSigma = gui.gaussianOption.split(",");
			for (int i = 1; i <= stackSize; i++) {
				for (int j = 0; j < gaussianSigma.length; j++) {
					ImageProcessor base = tempStack.getProcessor(i);
					double sigma = Double.parseDouble(gaussianSigma[j].trim());
					tasks.add(task("Gaussian_" + "Sigma_" + sigma + "Layer_" + i, () -> blur(base, 0.4 * sigma)));
				}
			}
		}

		if (gui.diffOfGaussian) {
			/* See if we have several Gaussian Difference filter settings! */
			String[] gaussianDiffOptionSet = gui.diffGaussianOption.split(";");
			for (int i = 1; i <= stackSize; i++) {
				for (int j = 0; j < gaussianDiffOptionSet.length; j++) {
					/* Split the gaussian option to get all sigmas (two sigmas for the difference)! */
					String[] gaussianSigma = gaussianDiffOptionSet[j].split(",");
					ImageProcessor base = tempStack.getProcessor(i);
					double sigma1 = Double.parseDouble(gaussianSigma[0].trim());
					double sigma2 = Double.parseDouble(gaussianSigma[1].trim());
					tasks.add(task("DiffOfGaussian_Set_" + j + "_Layer" + i, () -> Filters.subtract(blur(base, sigma1), blur(base, sigma2))));
				}
			}
		}

		if (gui.median) {
			addRankTasks(tasks, tempStack, gui.medianOption, RankFilters.MEDIAN, "Median_" + "Radius_", "Layer_");
		}

		if (gui.mean) {
			addRankTasks(tasks, tempStack, gui.meanOption, RankFilters.MEAN, "Mean_" + "Radius_", "Layer_");
		}

		if (gui.variance) {
			addRankTasks(tasks, tempStack, gui.varianceOption, RankFilters.VARIANCE, "Variance_" + "Radius_", "Layer_");
		}

		if (gui.maximum) {
			addRankTasks(tasks, tempStack, gui.maximumOption, RankFilters.MAX, "Maximum_" + "Radius_", "Layer_");
		}

		if (gui.minimum) {
			addRankTasks(tasks, tempStack, gui.minimumOption, RankFilters.MIN, "Minimum_" + "Radius_", "Layer_");
		}

		if (gui.gradientHessian) {
			/* The Sobel gradient in x and y direction (optional of Gaussian blurred images)! */
			String[] gradientHessian = gui.gradientHessianOption.split(",");
			for (int i = 1; i <= stackSize; i++) {
				ImageProcessor base = tempStack.getProcessor(i);
				if (gradientHessian[0].trim().isEmpty() == false) {
					for (int j = 0; j < gradientHessian.length; j++) {
						double sigma = Double.parseDouble(gradientHessian[j].trim());
						tasks.add(new FeatureTask(() -> {
							FloatProcessor[] g = Filters.sobel(blur(base, 0.4 * sigma));
							return List.of(new Layer("Gradient_Sobel X", g[0]), new Layer("Gradient_Sobel Y", g[1]));
						}));
					}
				} else {
					tasks.add(new FeatureTask(() -> {
						FloatProcessor[] g = Filters.sobel(base);
						return List.of(new Layer("Gradient_Sobel X", g[0]), new Layer("Gradient_Sobel Y", g[1]));
					}));
				}
			}
		}

		if (gui.laplacian) {
			String[] laplacianSigma = gui.laplacianOption.split(",");
			for (int i = 1; i <= stackSize; i++) {
				ImageProcessor base = tempStack.getProcessor(i);
				/* Apply a Gaussian blur if we have double arguments! */
				if (laplacianSigma[0].trim().isEmpty() == false) {
					for (int j = 0; j < laplacianSigma.length; j++) {
						double sigma = Double.parseDouble(laplacianSigma[j].trim());
						tasks.add(task("Laplacian_Derivative_From_Gaussian_" + sigma + "_Layer" + i, () -> Filters.laplacian(blur(base, 0.4 * sigma))));
					}
				} else {
					tasks.add(task("Laplacian Derivative_" + "Layer_" + i, () -> Filters.laplacian(base)));
				}
			}
		}

		if (gui.edges) {
			// see:
			// https://imagejdocu.tudor.lu/faq/technical/what_is_the_algorithm_used_in_find_edges
			String[] edgesSigma = gui.edgesOption.split(",");
			for (int i = 1; i <= stackSize; i++) {
				ImageProcessor base = tempStack.getProcessor(i);
				/* Apply a Gaussian blur if we have double arguments! */
				if (edgesSigma[0].trim().isEmpty() == false) {
					for (int j = 0; j < edgesSigma.length; j++) {
						double sigma = Double.parseDouble(edgesSigma[j].trim());
						tasks.add(task("Edges_Layer_From_Gaussian_" + j + "_Layer_" + i, () -> {
							ImageProcessor ip = blur(base, 0.4 * sigma);
							ip.findEdges();
							return ip;
						}));
					}
				} else {
					tasks.add(task("Edges_" + "Layer_" + i, () -> {
						ImageProcessor ip = base.duplicate();
						ip.findEdges();
						return ip;
					}));
				}
			}
		}

		if (gui.lipschitz) {
			/* See if we have several Lipschitz filter settings! */
			String[] libschitzOptionsSet = gui.lipschitzOption.split(";");
			for (int i = 1; i <= stackSize; i++) {
				ImageProcessor base = tempStack.getProcessor(i);
				for (int j = 0; j < libschitzOptionsSet.length; j++) {
					/* Split the Lipschitz set for one filter! */
					String[] lipschitzOptions = libschitzOptionsSet[j].split(",");
					tasks.add(task("Lipschitz_Set_" + j + "_Layer_" + i, () -> {
						ImageProcessor ip = base.duplicate().convertToByte(true);
						Lipschitz_ filter = new Lipschitz_();
						filter.setDownHatFilter(Boolean.parseBoolean(lipschitzOptions[0].trim()));
						filter.setTopHatFilter(Boolean.parseBoolean(lipschitzOptions[1].trim()));
						filter.setSlopeFilter(Double.parseDouble(lipschitzOptions[2].trim()));
						filter.Lipschitz2D(ip);
						return ip.convertToFloat();
					}));
				}
			}
		}

		if (gui.gabor) {
			/* See if we have several Gabor filter settings! */
			String[] gaborOptionsSet = gui.gaborOption.split(";");
			for (int i = 1; i <= stackSize; i++) {
				ImageProcessor base = tempStack.getProcessor(i);
				for (int j = 0; j < gaborOptionsSet.length; j++) {
					/* Split the Gabor set for one filter: size, wavelength, orientation, phase offset, sigma, aspect ratio! */
					String[] o = gaborOptionsSet[j].split(",");
					tasks.add(task("Gabor_Set_" + j + "_Layer_" + i,
							() -> Filters.gabor(base, Integer.parseInt(o[0].trim()), Double.parseDouble(o[1].trim()), Double.parseDouble(o[2].trim()),
									Double.parseDouble(o[3].trim()), Double.parseDouble(o[4].trim()), Double.parseDouble(o[5].trim()))));
				}
			}
		}

		if (gui.topHat) {
			addRankTasks(tasks, tempStack, gui.topHatOption, RankFilters.TOP_HAT, "Top_Hat_", "_Layer_");
		}

		if (gui.kuwahara) {
			String[] kuwaharaOptions = gui.kuwaharaOption.split(",");
			for (int i = 1; i <= stackSize; i++) {
				ImageProcessor base = tempStack.getProcessor(i);
				for (int j = 0; j < kuwaharaOptions.length; j++) {
					int radius = Integer.parseInt(kuwaharaOptions[j].trim());
					tasks.add(task("Kuwahara_" + radius + "_Layer_" + i, () -> {
						ImageProcessor ip = base.duplicate();
						Kuwahara_Filter kuw = new Kuwahara_Filter();
						kuw.size = radius;
						kuw.filter(ip);
						return ip;
					}));
				}
			}
		}

		if (gui.convolve) {
			String[] matrices = gui.convolveOption.split(";");
			for (int i = 0; i < matrices.length; i++) {
				String matrix = matrices[i];
				for (int u = 1; u <= stackSize; u++) {
					ImageProcessor base = tempStack.getProcessor(u);
					tasks.add(task("Convolved_" + i + "_Layer_" + u, () -> Filters.convolve(base, matrix)));
				}
			}
		}

		/* Haralick texture features of a Gray-Level Co-occurrence Matrix (the selected features) */
		int[] haralickFeatures = IntStream.range(0, gui.haralickSelection.length).filter(f -> gui.haralickSelection[f]).toArray();
		if (haralickFeatures.length > 0) {
			int levels = Integer.parseInt(gui.haralickLevelsOption.trim());
			/* A set of layers for each combination of the windows and distances */
			for (String windowOption : gui.haralickWindowOption.split(",")) {
				for (String distanceOption : gui.haralickDistanceOption.split(",")) {
					int window = Integer.parseInt(windowOption.trim());
					int distance = Integer.parseInt(distanceOption.trim());
					for (int i = 1; i <= stackSize; i++) {
						ImageProcessor base = tempStack.getProcessor(i);
						double[] range = ranges[i - 1];
						String suffix = "_W" + window + "_D" + distance + "_L" + levels + "_Layer_" + i;
						tasks.add(new FeatureTask(() -> {
							FloatProcessor[] layers = Haralick.compute(base, range[0], range[1], window, distance, levels, haralickFeatures,
									() -> monitor != null && monitor.isCanceled());
							List<Layer> result = new ArrayList<Layer>();
							for (int f = 0; f < haralickFeatures.length; f++) {
								result.add(new Layer("Haralick_" + Haralick.FEATURES[haralickFeatures[f]] + suffix, layers[f]));
							}
							return result;
						}));
					}
				}
			}
		}

		/* The texture measures */
		if (gui.localEntropy) {
			for (String r : gui.localEntropyOption.split(",")) {
				int radius = Integer.parseInt(r.trim());
				for (int i = 1; i <= stackSize; i++) {
					ImageProcessor base = tempStack.getProcessor(i);
					double[] range = ranges[i - 1];
					tasks.add(task("Local_Entropy_R" + radius + "_Layer_" + i, () -> Texture.localEntropy(base, range[0], range[1], radius, 64)));
				}
			}
		}

		if (gui.lbp) {
			for (String r : gui.lbpOption.split(",")) {
				double radius = Double.parseDouble(r.trim());
				for (int i = 1; i <= stackSize; i++) {
					ImageProcessor base = tempStack.getProcessor(i);
					tasks.add(task("LBP_R" + r.trim() + "_Layer_" + i, () -> Texture.lbp(base, radius)));
				}
			}
		}

		if (gui.laws) {
			for (String r : gui.lawsOption.split(",")) {
				int radius = Integer.parseInt(r.trim());
				for (int i = 1; i <= stackSize; i++) {
					ImageProcessor base = tempStack.getProcessor(i);
					String suffix = "_R" + radius + "_Layer_" + i;
					tasks.add(new FeatureTask(() -> {
						FloatProcessor[] maps = Texture.laws(base, radius);
						List<Layer> result = new ArrayList<Layer>();
						for (int m = 0; m < maps.length; m++) {
							result.add(new Layer("Laws_" + Texture.LAWS[m] + suffix, maps[m]));
						}
						return result;
					}));
				}
			}
		}

		if (gui.structureTensor) {
			for (String set : gui.structureTensorOption.split(";")) {
				String[] o = set.split(",");
				double gradientSigma = Double.parseDouble(o[0].trim());
				double integrationSigma = Double.parseDouble((o.length > 1 ? o[1] : o[0]).trim());
				for (int i = 1; i <= stackSize; i++) {
					ImageProcessor base = tempStack.getProcessor(i);
					String suffix = "_" + gradientSigma + "_" + integrationSigma + "_Layer_" + i;
					tasks.add(new FeatureTask(() -> {
						FloatProcessor[] t = Texture.structureTensor(base, gradientSigma, integrationSigma);
						return List.of(new Layer("StructureTensor_Largest" + suffix, t[0]), new Layer("StructureTensor_Smallest" + suffix, t[1]),
								new Layer("StructureTensor_Coherence" + suffix, t[2]));
					}));
				}
			}
		}

		if (gui.hessian) {
			for (String s : gui.hessianOption.split(",")) {
				double sigma = Double.parseDouble(s.trim());
				for (int i = 1; i <= stackSize; i++) {
					ImageProcessor base = tempStack.getProcessor(i);
					String suffix = "_" + sigma + "_Layer_" + i;
					tasks.add(new FeatureTask(() -> {
						FloatProcessor[] t = Texture.hessian(base, sigma);
						return List.of(new Layer("Hessian_Largest" + suffix, t[0]), new Layer("Hessian_Smallest" + suffix, t[1]),
								new Layer("Hessian_Determinant" + suffix, t[2]));
					}));
				}
			}
		}

		runFeatureTasks(tasks, stack, monitor);

		String name = image.getShortTitle();
		imPlus = new ImagePlus(name, stack);
		image = null;
		stack = null;
		tempStack = null;
		return imPlus;
	}

	/* A named layer of the feature stack */
	private record Layer(String name, ImageProcessor ip) {
	}

	/* A filter which computes one or more layers (a task of the parallel computation) */
	private static final class FeatureTask {
		final Supplier<List<Layer>> filter;

		FeatureTask(Supplier<List<Layer>> filter) {
			this.filter = filter;
		}
	}

	/* A task with one layer */
	private static FeatureTask task(String name, Supplier<ImageProcessor> filter) {
		return new FeatureTask(() -> List.of(new Layer(name, filter.get())));
	}

	/* A Gaussian blurred copy of the image */
	private static ImageProcessor blur(ImageProcessor base, double sigma) {
		ImageProcessor ip = base.duplicate();
		new GaussianBlur().blurGaussian(ip, sigma, sigma, 0.0002);
		return ip;
	}

	/* The tasks of a rank filter (e.g., median) for all radii and layers */
	private static void addRankTasks(List<FeatureTask> tasks, ImageStack base, String option, int type, String prefix, String layerPrefix) {
		String[] radii = option.split(",");
		for (int i = 1; i <= base.getSize(); i++) {
			ImageProcessor layer = base.getProcessor(i);
			for (int j = 0; j < radii.length; j++) {
				double radius = Double.parseDouble(radii[j].trim());
				tasks.add(task(prefix + radius + layerPrefix + i, () -> {
					ImageProcessor ip = layer.duplicate();
					new RankFilters().rank(ip, radius, type);
					return ip;
				}));
			}
		}
	}

	/*
	 * Computes the tasks in parallel and adds the layers to the stack in the
	 * order of the tasks!
	 */
	private static void runFeatureTasks(List<FeatureTask> tasks, ImageStack stack, IProgressMonitor monitor) {
		if (tasks.isEmpty()) {
			return;
		}
		List<List<Layer>> results = new ArrayList<List<Layer>>(Collections.nCopies(tasks.size(), null));
		AtomicInteger done = new AtomicInteger();
		if (monitor != null) {
			monitor.setTaskName("Apply filters (" + tasks.size() + " tasks in parallel)");
		}
		IntStream.range(0, tasks.size()).parallel().forEach(t -> {
			if (monitor != null && monitor.isCanceled()) {
				return;
			}
			results.set(t, tasks.get(t).filter.get());
			if (monitor != null) {
				monitor.subTask("Filter " + done.incrementAndGet() + " of " + tasks.size());
			}
		});
		if (monitor != null && monitor.isCanceled()) {
			throw new OperationCanceledException();
		}
		for (List<Layer> layers : results) {
			for (Layer layer : layers) {
				stack.addSlice(layer.name(), layer.ip());
			}
		}
	}

	/*
	 * The quantization ranges of the base layers for the Haralick features. Fixed
	 * ranges (independent of the content) that a preview of a selection and the
	 * complete image have the same gray levels: RGB channels and 8-bit images 0 -
	 * 255, HSB 0 - 1, LAB (L: 0 - 100, a, b: -128 - 128) and else the display
	 * range of the image (or the range of the layer)!
	 */
	private double[][] quantizationRanges(ImagePlus image, ImageStack base) {
		int n = base.getSize();
		double[][] ranges = new double[n][];
		String opt = gui.channelOption;
		String[] channels = opt.isEmpty() ? new String[0] : opt.split(",");
		boolean rgb = image.getProcessor() instanceof ColorProcessor;
		for (int i = 0; i < n; i++) {
			/* The channel of the layer (0 based) */
			int channel = i < channels.length ? Integer.parseInt(channels[i].trim()) - 1 : i;
			if (rgb && gui.toHsb) {
				ranges[i] = new double[] { 0, 1 };
			} else if (rgb && gui.toLab) {
				ranges[i] = channel == 0 ? new double[] { 0, 100 } : new double[] { -128, 128 };
			} else if (rgb || image.getBitDepth() == 8) {
				ranges[i] = new double[] { 0, 255 };
			} else if (image.getDisplayRangeMax() > image.getDisplayRangeMin()) {
				ranges[i] = new double[] { image.getDisplayRangeMin(), image.getDisplayRangeMax() };
			} else {
				ImageProcessor ip = base.getProcessor(i + 1);
				ip.resetMinAndMax();
				ranges[i] = new double[] { ip.getMin(), ip.getMax() };
			}
		}
		return ranges;
	}

	/* A method to get the Bio7 GUI R data transfer type */
	private int getRDataTransferType() {
		/* Wrap to avoid an invalid thread access! */
		Display display = Util.getDisplay();
		display.syncExec(() -> transferType = RImageMethodsView.getTransferTypeCombo().getSelectionIndex());
		return transferType;
	}

	public String[] openMultipleFiles() {
		files = null;

		Display display = Util.getDisplay();
		display.syncExec(() -> {
			Shell shell = new Shell(display);
			FileDialog dlg = new FileDialog(shell, SWT.MULTI);
			dlg.setFilterPath(null);
			String f = dlg.open();
			if (f != null) {

				files = dlg.getFileNames();
				currentFilePathMultipleDialog = dlg.getFilterPath();
			}

		});

		return files;
	}

	/*
	 * If the ImageJ ROI has changed this method will be called for preview. A job
	 * avoids a recall if the job is busy to update the preview!
	 */
	public void roiModified(ImagePlus imp, int id) {
		/* Each time the job has finished we start a new job! */
		if (jobDone == false) {
			return;
		}
		/*
		 * Or we wait until dragging finished! if (job != null) { job.cancel(); }
		 */
		jobDone = false;
		previewJob = new Job("Generate Preview") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Load...", IProgressMonitor.UNKNOWN);
				ImagePlus image = WindowManager.getCurrentImage();
				if (image.getRoi() != null) {
					Roi roi = image.getRoi();
					int roiWidth = roi.getBounds().width;
					int roiHeight = roi.getBounds().height;
					if (roiWidth >= 5 && roiHeight >= 5) {
						/* Get all settings before the workflow for preview starts! */
						if (RServe.isAlive()) {
							if (RState.isBusy() == false) {
								/* Notify that R is busy! */
								RState.setBusy(true);
								gui.getFeatureOptions();
								/* Classify a preview from a selection! */
								if (gui.retrainPreview) {
									String path = gui.getPathTrainingScript();
									/* Execute R script! */
									if (path.endsWith(".R")) {
										evalRScript(path);
									}
								}
								ImagePlus roiImage;
								if (image.getStackSize() == 1) {
									roiImage = image.crop();
								} else {
									roiImage = image.crop("stack");
								}
								/* Call method in main class! */
								ImagePlus imPlus = createStackFeatures(null, roiImage, monitor);
								classifyPreview(monitor, imPlus, roi);
							}
						}

					} else {
						System.out.println("Selection (width and height) must be >= 5px");
					}

				}
				monitor.done();
				return Status.OK_STATUS;
			}

		};
		previewJob.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {

					RState.setBusy(false);
					/* Update the R-Shell view workspace objects! */
					if (RServe.isAlive()) {
						listRObjects();
					}
					jobDone = true;
				} else {

					RState.setBusy(false);
					jobDone = true;
				}
			}
		});

		previewJob.schedule(1000);

	}

	/*
	 * A method to clean all RoiListeners on startup and recompilation coming from
	 * the ModelGui class!
	 */
	private void removeRoiListenerAtStartup() {
		Vector listen = Roi.getListeners();
		if (listen.size() > 0) {
			for (int i = 0; i < listen.size(); i++) {
				RoiListener rl = (RoiListener) listen.get(i);
				String className = rl.getClass().getName().toString();
				System.out.println(className);
				if (className.equals("_ModelGui")) {
					Roi.removeRoiListener(rl);
					IJ.run("Remove Overlay", "");
				}

			}

		}
	}

	/*
	 * We overwrite a close method of the super class 'Model' which this class
	 * extends!
	 */
	public void close() {
		Roi.removeRoiListener(gui);
		IJ.run("Remove Overlay", "");
	}

	public static String directory(final String text) {

		Display display = Util.getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				// Shell must be created with style SWT.NO_TRIM
				Shell shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);

				DirectoryDialog dlg = new DirectoryDialog(shell);
                //Set the workspace path!
				dlg.setFilterPath(FileRoot.getFileRoot());

				dlg.setText(text);

				dlg.setMessage(text);

				classifyDir = dlg.open();
				/* We print the path of the selected folder! */

			}
		});
		return classifyDir;
	}

	/*
	 * Only implemented to avoid a console warning! Constructor of this class will
	 * be called after compilation and instantiation!
	 */
	public static void main(String[] args) {

	}
}
