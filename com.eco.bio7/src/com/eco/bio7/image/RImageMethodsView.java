/*******************************************************************************
 * Copyright (c) 2007-2016 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.image;

import java.awt.image.BufferedImage;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import org.rosuda.REngine.Rserve.RConnection;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.collection.Work;
import com.eco.bio7.image.r.IJTranserResultsTable;
import com.eco.bio7.image.r.TransferImageStack;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RServeUtil;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.util.Util;
import ij.ImagePlus;
import ij.WindowManager;

/**
 * This class provides some static methods for the analysis and transfer of
 * images inside the Bio7 application.
 * 
 * 
 * @author Bio7
 * 
 */
public class RImageMethodsView extends ViewPart {
	public RImageMethodsView() {
	}

	private Combo toimageJCombo;

	private static Combo transferTypeCombo;

	private Text imageMatrixNameFromR;

	private Text imageMatrixNameToR;

	public static final String ID = "com.eco.bio7.rimage.methods.view";

	private Composite top = null;

	private Button button2 = null;

	private static BufferedImage image = null;

	private Button button4 = null;

	private static RImageMethodsView im;

	private static int pointScale = 1;

	private static boolean Centroid = true;

	protected boolean canTransfer = true;

	protected boolean canTransferPic = true;

	protected int transferImageType;

	protected int transferIntegers;

	protected boolean transferBackIntegers;

	private Button selectedRoiPixelButton;

	private Button btnPixelRoiStack;

	private Image rGif;

	private Button btnNewButton_1;

	private Button btnNewButton;

	private Button selectedPixelsButton;

	private Button matchingButton;

	private Button picToRButton;

	private Button picButton;

	private Button clusterImageButton;

	private Button pcaButton;

	private ScrolledComposite scrolledComposite;

	protected static boolean createMatrix;

	public void createPartControl(Composite parent) {
		// store = Bio7Plugin.getDefault().getPreferenceStore();
		im = this;
		scrolledComposite = new ScrolledComposite(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(false);
		//PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.imagemethods");
		GridData gridData14 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gridData14.widthHint = 86;
		gridData14.horizontalIndent = 0;
		GridData gridData3 = new GridData();
		gridData3.grabExcessVerticalSpace = true;
		gridData3.heightHint = 35;
		gridData3.horizontalAlignment = GridData.FILL;
		gridData3.grabExcessHorizontalSpace = true;
		gridData3.verticalAlignment = SWT.FILL;
		GridData gridData21 = new GridData();
		gridData21.heightHint = 35;
		gridData21.horizontalAlignment = GridData.FILL;
		gridData21.horizontalIndent = 0;
		gridData21.grabExcessHorizontalSpace = true;
		gridData21.grabExcessVerticalSpace = true;
		gridData21.verticalAlignment = SWT.FILL;
		GridLayout gridLayout1 = new GridLayout();
		gridLayout1.marginHeight = 1;
		gridLayout1.marginRight = 3;
		gridLayout1.marginLeft = 3;
		gridLayout1.makeColumnsEqualWidth = true;
		gridLayout1.numColumns = 2;
		gridLayout1.verticalSpacing = 0;
		gridLayout1.marginWidth = 0;
		gridLayout1.horizontalSpacing = 2;
		top = new Composite(scrolledComposite, SWT.NONE);
		top.setLayout(gridLayout1);
		top.setSize(300,400);
		scrolledComposite.setContent(top);
		

		CLabel lblNewLabel = new CLabel(top, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, 2, 1));
		lblNewLabel.setText("Transfer Selected Data To R");

		btnNewButton = new Button(top, SWT.NONE);
		btnNewButton.setToolTipText("Transfers the ImageJ \"Results Table\" data\r\nas a dataframe to R (datatype double).");
		rGif = Bio7Plugin.getImageDescriptor("/icons/views/raction.png").createImage();
		btnNewButton.setImage(rGif);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						RState.setBusy(true);
						Job job = new Job("Transfer to R") {
							@Override
							protected IStatus run(IProgressMonitor monitor) {
								monitor.beginTask("Transfer Data ...", IProgressMonitor.UNKNOWN);

								new IJTranserResultsTable().transferResultsTable(RServe.getConnection(), true);

								monitor.done();
								return Status.OK_STATUS;
							}

						};
						job.addJobChangeListener(new JobChangeAdapter() {
							public void done(IJobChangeEvent event) {
								if (event.getResult().isOK()) {

									RState.setBusy(false);
									RServeUtil.listRObjects();
								} else {

									RState.setBusy(false);
								}
							}
						});
						// job.setSystem(true);
						job.schedule();
					} else {
						Bio7Dialog.message("Rserve is busy!");
					}
				}

			}
		});
		GridData gd_btnNewButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_btnNewButton.heightHint = 35;
		btnNewButton.setLayoutData(gd_btnNewButton);
		btnNewButton.setText("IJ RT        ");

		btnNewButton_1 = new Button(top, SWT.NONE);
		btnNewButton_1.setToolTipText("Transfers an opened ImageJ stack (pixel data)\r\nas a matrix list or RasterStack to R.\r\nThe datatype can be selected, too!");
		rGif = Bio7Plugin.getImageDescriptor("/icons/views/raction.png").createImage();
		btnNewButton_1.setImage(rGif);
		btnNewButton_1.addSelectionListener(new SelectionAdapter() {
			boolean convertToRaster;

			@Override
			public void widgetSelected(SelectionEvent e) {
				RConnection con = RServe.getConnection();

				if (con != null) {
					if (RState.isBusy() == false) {
						RState.setBusy(true);

						MessageBox message = new MessageBox(Util.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
						message.setMessage("Should a Raster Stack be created?");
						message.setText("Raster?");
						int response = message.open();
						if (response == SWT.YES) {

							convertToRaster = true;
						}

						else {
							convertToRaster = false;
						}
						TransferImageStack job = new TransferImageStack("Transfer ImageJ Stack", con, convertToRaster);
						job.addJobChangeListener(new JobChangeAdapter() {
							public void done(IJobChangeEvent event) {
								if (event.getResult().isOK()) {

									RState.setBusy(false);
									RServeUtil.listRObjects();
								} else {

									RState.setBusy(false);
								}
							}
						});
						// picjob.setSystem(true);
						job.schedule();

					} else {
						Bio7Dialog.message("Rserve is busy!");
					}

				} else {
					Bio7Dialog.message("No Rserve connection available!");
				}

			}
		});
		GridData gd_btnNewButton_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_btnNewButton_1.heightHint = 35;
		btnNewButton_1.setLayoutData(gd_btnNewButton_1);
		btnNewButton_1.setText("IJ RasterStack    ");
		GridData gridData = new org.eclipse.swt.layout.GridData(SWT.FILL, SWT.FILL, true, false);
		gridData.heightHint = 35;
		button2 = new Button(top, SWT.NONE);
		// button2.setFont(SWTResourceManager.getFont("Courier New", 9,
		// SWT.BOLD));
		button2.setText("ROI         ");
		rGif = Bio7Plugin.getImageDescriptor("/icons/views/raction.png").createImage();
		button2.setImage(rGif);
		button2.setToolTipText("Transfer the current ROI selection coordinates\nto R ");

		// button2.setFont(new Font(Display.getDefault(), "Tahoma", 8,
		// SWT.BOLD));
		button2.setLayoutData(gridData);
		button2.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {

				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						RState.setBusy(true);
						Job job = new Job("Transfer to R") {
							@Override
							protected IStatus run(IProgressMonitor monitor) {
								monitor.beginTask("Transfer Data ...", IProgressMonitor.UNKNOWN);

								new IJTranserResultsTable().pointsToR(RServe.getConnection());

								monitor.done();
								return Status.OK_STATUS;
							}

						};
						job.addJobChangeListener(new JobChangeAdapter() {
							public void done(IJobChangeEvent event) {
								if (event.getResult().isOK()) {

									RState.setBusy(false);
									RServeUtil.listRObjects();
									Bio7Dialog.message("ROI transferred!");
								} else {

									RState.setBusy(false);
								}
							}
						});
						// job.setSystem(true);
						job.schedule();
					} else {
						Bio7Dialog.message("Rserve is busy!");
					}
				}

			}
		});
		GridData gridData131 = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData131.heightHint = 35;
		button4 = new Button(top, SWT.NONE);
		// button4.setFont(SWTResourceManager.getFont("Courier New", 9,
		// SWT.BOLD));
		button4.setText("Particles            ");
		button4.setImage(rGif);
		// button4.setFont(new Font(Display.getDefault(), "Tahoma", 8,
		// SWT.BOLD));
		button4.setLayoutData(gridData131);
		button4.setToolTipText(
				"Transfers a particle measurement to R.\r\nThe image has to be thresholded and in\r\nthe ImageJ \"Analyze Particles\" dialog the option\r\n\"Display results\" has to be selected.\r\nSimply automates an ImageJ \"Results\r\nTable\" transfer workflow (see \"IJ RT\" action).");
		button4.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {

				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						RState.setBusy(true);
						Job job = new Job("Transfer to R") {
							@Override
							protected IStatus run(IProgressMonitor monitor) {
								monitor.beginTask("Transfer Data ...", IProgressMonitor.UNKNOWN);

								new IJTranserResultsTable().particledescriptors();

								monitor.done();
								return Status.OK_STATUS;
							}

						};
						job.addJobChangeListener(new JobChangeAdapter() {
							public void done(IJobChangeEvent event) {
								if (event.getResult().isOK()) {

									RState.setBusy(false);
									RServeUtil.listRObjects();
									//Bio7Dialog.message("Particles action executed!");
								} else {

									RState.setBusy(false);
								}
							}
						});
						// job.setSystem(true);
						job.schedule();
					} else {
						Bio7Dialog.message("Rserve is busy!");
					}
				}

			}

		});

		selectedPixelsButton = new Button(top, SWT.NONE);
		selectedPixelsButton
				.setToolTipText("" + "Transfers the selected pixels (Freehand, Rectangular etc.)\n" + "with or without a signature as a matrix to R.\n" + "The transfer type can be selected, too!");
		selectedPixelsButton.setImage(Bio7Plugin.getImageDescriptor("/icons/views/raction.png").createImage());
		// selectedPixelsButton.setFont(SWTResourceManager.getFont("Courier New",
		// 9, SWT.BOLD));
		selectedPixelsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAlive()) {
					ImagePlus imp = WindowManager.getCurrentImage();
					if (imp != null) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);

							ImageSelectionTransferJob job = new ImageSelectionTransferJob(transferTypeCombo.getSelectionIndex());
							job.addJobChangeListener(new JobChangeAdapter() {
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {
										RState.setBusy(false);
										RServeUtil.listRObjects();
										Bio7Dialog.message("Selected Pixels transferred to R!");
									} else {
										RState.setBusy(false);
									}
								}
							});
							job.schedule();
						} else {

							Bio7Dialog.message("Rserve is busy!");

						}
					} else {

						Bio7Dialog.message("No image available!");

					}
				} else {
					Bio7Dialog.message("No Rserve connection available!");
				}
			}
		});

		final GridData gd_selectedPixelsButton = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd_selectedPixelsButton.heightHint = 35;
		selectedPixelsButton.setLayoutData(gd_selectedPixelsButton);
		selectedPixelsButton.setText("Pixel       ");

		matchingButton = new Button(top, SWT.NONE);
		matchingButton.setToolTipText("Transfers collected selection coordinates\n as a List of Lists (the single selections) to R.\nThe ROI Manager has to be available!");
		matchingButton.setImage(rGif);
		// matchingButton.setFont(SWTResourceManager.getFont("Courier New", 9,
		// SWT.BOLD));
		matchingButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent e) {

				/*
				 * TransferSelectionCoordsDialog dialog = new
				 * TransferSelectionCoordsDialog(Util.getShell());
				 * 
				 * dialog.create(); dialog.open();
				 */
				Work.openView("com.eco.bio7.image.TransferGeometryView");

			}
		});
		final GridData gd_matchingButton = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd_matchingButton.heightHint = 35;
		matchingButton.setLayoutData(gd_matchingButton);
		matchingButton.setText("Selection RM    ");

		selectedRoiPixelButton = new Button(top, SWT.NONE);
		selectedRoiPixelButton.setImage(rGif);
		selectedPixelsButton.setImage(rGif);
		selectedRoiPixelButton.setToolTipText("" + "Transfers the selected pixels (Freehand, Rectangular etc.)\n" + "with or without a signature as a matrix to R.\n"
				+ "The transfer type can be selected, too!\n" + "The ROI Manager with ROI selections has to be active.\n" + "All selections for all layers (opened tabs in the ImageJ view)\n"
				+ "are transferred automatically with an incremental signature!\n" + "If a stack is among the opened images only the first slice (layer)\n" + "will be transferred!");
		selectedRoiPixelButton.setText("Pixel RM");

		selectedRoiPixelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				if (RServe.isAlive()) {
					ImagePlus imp = WindowManager.getCurrentImage();
					if (imp != null) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);

							ImageRoiSelectionTransferJob job = new ImageRoiSelectionTransferJob(transferTypeCombo.getSelectionIndex());
							// job.setSystem(true);
							job.addJobChangeListener(new JobChangeAdapter() {
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {

										RState.setBusy(false);
										RServeUtil.listRObjects();
										//Bio7Dialog.message("Selected Pixels transferred to R!");

									} else {

										RState.setBusy(false);
									}
								}
							});
							job.schedule();
						}
					} else {

						Bio7Dialog.message("No image available!");

					}
				} else {
					Bio7Dialog.message("No Rserve connection available!");
				}

			}
		});
		GridData gd_selectedRoiPixelButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_selectedRoiPixelButton.heightHint = 35;
		selectedRoiPixelButton.setLayoutData(gd_selectedRoiPixelButton);

		btnPixelRoiStack = new Button(top, SWT.NONE);
		btnPixelRoiStack.setImage(rGif);
		btnPixelRoiStack.setToolTipText("" + "Transfers the selected pixels of the selected ImageJ\n" + "selections (Freehand, Rectangular etc.)in a stack \n"
				+ "with or without a signature as a matrix to R.\n" + "The transfer type can be selected, too!\n" + "The ROI Manager with selections has to be active\n"
				+ "and the selected image must be a stack!\n" + "All selections for all slices are transferred\n" + "automatically with an selected or incremental signature!");
		btnPixelRoiStack.setText("Pixel RM Stack");
		selectedPixelsButton.setImage(rGif);
		btnPixelRoiStack.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (RServe.isAlive()) {
					ImagePlus imp = WindowManager.getCurrentImage();
					if (imp != null) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							ImageStackRoiSelectionTransferJob job = new ImageStackRoiSelectionTransferJob(transferTypeCombo.getSelectionIndex());
							// job.setSystem(true);
							job.addJobChangeListener(new JobChangeAdapter() {
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {

										RState.setBusy(false);
										RServeUtil.listRObjects();
										//Bio7Dialog.message("Selected Pixels transferred to R!");
									} else {

										RState.setBusy(false);
									}
								}
							});
							job.schedule();
						}
					} else {

						Bio7Dialog.message("No image available!");

					}
				} else {
					Bio7Dialog.message("No Rserve connection available!");
				}

			}
		});
		GridData gd_btnPixelRoiStack = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_btnPixelRoiStack.heightHint = 35;
		btnPixelRoiStack.setLayoutData(gd_btnPixelRoiStack);

		CLabel lblNewLabel_1 = new CLabel(top, SWT.NONE);
		lblNewLabel_1.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, 2, 1));
		lblNewLabel_1.setText("Transfer Image Data From and To R");

		picToRButton = new Button(top, SWT.NONE);
		// picToRButton.setFont(SWTResourceManager.getFont("Courier New", 9,
		// SWT.BOLD));
		picToRButton.setText("Pic<-      ");

		picToRButton.setToolTipText("Transfer image data to R");
		picToRButton.setImage(rGif);
		picToRButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAlive()) {
					if (RState.isBusy() == false) {
						RState.setBusy(true);
						if (canTransferPic) {
							canTransferPic = false;
							TransferPicJob picjob = new TransferPicJob(imageMatrixNameToR.getText(), transferTypeCombo.getSelectionIndex());
							picjob.addJobChangeListener(new JobChangeAdapter() {
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {
										canTransferPic = true;
										RState.setBusy(false);
										RServeUtil.listRObjects();
									} else {
										canTransferPic = true;
										RState.setBusy(false);
									}
								}
							});
							// picjob.setSystem(true);
							picjob.schedule();

						}
					} else {
						Bio7Dialog.message("Rserve is busy!");

					}
				} else {
					Bio7Dialog.message("No Rserve connection available!");
				}
			}
		});

		final GridData gd_picToRButton = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd_picToRButton.heightHint = 35;
		picToRButton.setLayoutData(gd_picToRButton);

		picButton = new Button(top, SWT.NONE);
		// picButton.setFont(SWTResourceManager.getFont("Courier New", 9,
		// SWT.BOLD));
		picButton.setText("Pic->           ");

		picButton.setToolTipText("Create an image from the R data");
		picButton.setImage(rGif);
		picButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAlive()) {
					if (transferTypeCombo.getSelectionIndex() == 3) {
						Bio7Dialog.message("RGB transfer to ImageJ is not supported!\n Please use e.g. byte transfer for the R ,G ,B components\n which can be merged with ImageJ!");
						return;
					}
					if (RState.isBusy() == false) {
						RState.setBusy(true);
						if (canTransfer) {
							canTransfer = false;
							int select = toimageJCombo.getSelectionIndex();
							if (select == 0) {
								transferImageType = 0;
							} else if (select == 1) {
								transferImageType = 1;
							} else if (select == 2) {
								transferImageType = 2;
							} else if (select == 3) {// Short type
								transferImageType = 3;
							}
							PicFromRJob picFromRJob = new PicFromRJob(transferImageType, imageMatrixNameFromR.getText(), transferTypeCombo.getSelectionIndex());
							picFromRJob.addJobChangeListener(new JobChangeAdapter() {
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {
										canTransfer = true;
										RState.setBusy(false);
									} else {
										canTransfer = true;
										RState.setBusy(false);
									}
								}
							});
							// picFromRJob.setSystem(true);
							picFromRJob.schedule();

						}
					} else {
						Bio7Dialog.message("Rserve is busy!");

					}
				} else {
					Bio7Dialog.message("No Rserve connection available!");
				}
			}
		});
		final GridData gd_picButton = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd_picButton.heightHint = 35;
		picButton.setLayoutData(gd_picButton);

		transferTypeCombo = new Combo(top, SWT.READ_ONLY);
		transferTypeCombo.select(0);
		transferTypeCombo.setToolTipText("The datatype to transfer in both directions!");
		transferTypeCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				transferTypeCombo.getSelectionIndex();
			}
		});
		transferTypeCombo.setItems(new String[] { "Double", "Integer", "Byte", "RGB Byte" });
		transferTypeCombo.setText("Double");
		transferTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		toimageJCombo = new Combo(top, SWT.READ_ONLY);
		toimageJCombo.setToolTipText("Selects the image type which will be created in Imagej");
		toimageJCombo.select(0);
		toimageJCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

			}
		});
		toimageJCombo.setItems(new String[] { "Colour", "Greyscale", "Float", "Short" });
		toimageJCombo.setText("Float");
		toimageJCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		// greyscaleButton.setToolTipText(
		// "Create from the image data in R a greyscale image (ByteProcessor) in ImageJ"
		// );
		// rgbButton.setToolTipText(
		// "Create from the image data in R a RGB image (ColourProcessor) in ImageJ"
		// );
		// floatButton.setToolTipText(
		// "Create from the image data in R an image in ImageJ with the FloatProcessor"
		// );
		// shortButton.setToolTipText(
		// "Create from the image data in R an image in ImageJ with the ShortProcessor"
		// );

		imageMatrixNameToR = new Text(top, SWT.BORDER);

		imageMatrixNameToR.setToolTipText("The name for the data which will be created if selected");
		imageMatrixNameToR.setText("imageMatrix");
		imageMatrixNameToR.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		imageMatrixNameFromR = new Text(top, SWT.BORDER);
		imageMatrixNameFromR.setToolTipText("The name of the data which will be used to create the image in ImageJ");
		imageMatrixNameFromR.setText("imageMatrix");
		imageMatrixNameFromR.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		CLabel lblNewLabel_2 = new CLabel(top, SWT.NONE);
		lblNewLabel_2.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, 2, 1));
		lblNewLabel_2.setText("Multivariate Image Analysis");

		clusterImageButton = new Button(top, SWT.NONE);
		// clusterImageButton.setFont(SWTResourceManager.getFont("Courier New",
		// 9, SWT.BOLD));
		clusterImageButton.setText("Cluster Pic");
		clusterImageButton.setToolTipText("Performs a cluster analysis and creates\na new image from the assigned pixels");
		// clusterImageButton.setImage(ResourceManager.getPluginImage(Bio7Plugin.getDefault(),
		// "bin/pics/cluster.gif"));

		// clusterImageButton.setFont(SWTResourceManager.getFont("", 9,
		// SWT.BOLD));
		// clusterImageButton.setFont(SWTResourceManager.getFont("", 9,
		// SWT.BOLD));
		clusterImageButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				if (RServe.getConnection() != null) {
					CTabItem[] items = CanvasView.getCanvas_view().tabFolder.getItems();
					if (items.length > 0) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							ClusterDialog co = new ClusterDialog(Util.getShell());
							co.open();
							ClusterJob clusterjob = new ClusterJob();
							clusterjob.addJobChangeListener(new JobChangeAdapter() {
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {
										canTransfer = true;
										RState.setBusy(false);
										RServeUtil.listRObjects();
									} else {
										canTransfer = true;
										RState.setBusy(false);
									}
								}
							});
							// clusterjob.setSystem(true);
							clusterjob.schedule();
						} else {
							Bio7Dialog.message("Rserve is busy!");
						}
					} else {
						Bio7Dialog.message("No image available!");
					}
				} else {
					RServe.isAliveDialog();
				}
			}
		});
		final GridData gd_clusterImageButton = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd_clusterImageButton.heightHint = 35;
		clusterImageButton.setLayoutData(gd_clusterImageButton);

		pcaButton = new Button(top, SWT.NONE);
		// pcaButton.setFont(SWTResourceManager.getFont("Courier New", 9,
		// SWT.BOLD));
		pcaButton.setText("PCA");
		pcaButton.setToolTipText("Performs a Principal components analysis\nand creates new image(s) from the components");
		// pcaButton.setImage(ResourceManager.getPluginImage(Bio7Plugin.getDefault(),
		// "bin/pics/pca.gif"));

		// pcaButton.setFont(SWTResourceManager.getFont("", 9, SWT.BOLD));
		// pcaButton.setFont(SWTResourceManager.getFont("", 9, SWT.BOLD));
		pcaButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.getConnection() != null) {
					CTabItem[] items = CanvasView.getCanvas_view().tabFolder.getItems();
					if (items.length > 0) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							PcaDialog pca = new PcaDialog(Util.getShell());
							pca.open();
							PcaJob pcaJob = new PcaJob();
							pcaJob.addJobChangeListener(new JobChangeAdapter() {
								public void done(IJobChangeEvent event) {
									if (event.getResult().isOK()) {
										canTransfer = true;
										RState.setBusy(false);
										RServeUtil.listRObjects();
									} else {
										canTransfer = true;
										RState.setBusy(false);
									}
								}
							});
							// pcaJob.setSystem(true);
							pcaJob.schedule();
						} else {
							Bio7Dialog.message("Rserve is busy!");
						}
					} else {
						Bio7Dialog.message("No image available!");
					}
				} else {
					RServe.isAliveDialog();
				}

			}
		});
		final GridData gd_pcaButton = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd_pcaButton.heightHint = 35;
		pcaButton.setLayoutData(gd_pcaButton);

		//initializeToolBar(); causing WindowBuilder error!

	}

	public void setFocus() {
		top.setFocus();
	}

	/**
	 * Returns the scale factor for the coordinates of the points from the particle
	 * analysis.
	 * 
	 * @return an integer value of the scale factor.
	 */
	public static int getPointScale() {
		return pointScale;
	}

	/**
	 * Sets the scale factor for the coordinates of the points from the particle
	 * analysis.
	 * 
	 * @param pointScale an integer value of the scale factor.
	 */
	public static void setPointScale(int pointScale) {
		RImageMethodsView.pointScale = pointScale;
	}

	/**
	 * Returns if the centroid method is selected for the points.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isCentroid() {
		return Centroid;
	}

	/**
	 * Sets the method for adjusting the points in the Points panel (default =
	 * centroid for the particle analysis).
	 * 
	 * @param centroid a boolean value.
	 */
	public static void setCentroid(boolean centroid) {
		Centroid = centroid;
	}

	private void initializeToolBar() {
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
	}

	public static BufferedImage getImage() {
		return image;
	}

	/**
	 * This methods transfers image data from ImageJ to R by means of Rserve. Matrix
	 * or vector data is created and variables for the width, height and the name of
	 * the image will be created.
	 * 
	 * 
	 * @param name             the name for the data.
	 * @param matrix           a boolean, if true automatically a data matrix will
	 *                         be created (only for double values - in case of an
	 *                         RGB transfer an integer matrix will be created!).
	 * @param transferDataType an integer value for the data type which will be
	 *                         transfered. (0=double, 1=integer, 2=byte, 3=RGB as
	 *                         single byte vectors or integer matrix)
	 * @param impPlus          an optional ImagePlus object as the default image
	 *                         source.
	 * 
	 */

	public static void imageToR(String name, boolean matrix, int transferDataType, ImagePlus impPlus) {
		ImageMethods.imageToR(name, matrix, transferDataType, impPlus);
	}

	/**
	 * This method creates an ImageJ image from the given named data (matrix or
	 * vector) in R. This method expects the variables imageSizeX, imageSizeY to be
	 * present in the R workspace if vector data is transferred.
	 * 
	 * @param type             an integer which represents an ImageJ image
	 *                         type.(0=ColourProcessor, 1=ByteProcessor,
	 *                         2=FloatProcessor, 3=ShortProcessor)
	 * @param name             a string identifier for the R data.
	 * 
	 * @param transferDataType the data type as transfer type (0=double, 1=integer,
	 *                         2=byte).
	 * 
	 */

	public static void imageFromR(int type, String name, int transferDataType) {
		ImageMethods.imageFromR(type, name, transferDataType);
	}

	/*
	 * Method for cluster analysis and PCR with fixed imageData as name!
	 * 0=double,1=integer, 2=byte
	 */
	protected static void imagePlusToR(ImagePlus imp, String name, boolean matrix, int transferDataType) {
		ImageMethods.imagePlusToR(imp, name, matrix, transferDataType);
	}

	/* Used in Cluster and Pca job */
	public static Combo getTransferTypeCombo() {
		return transferTypeCombo;
	}

	protected static void setTransferTypeCombo(Combo transferTypeCombo) {
		RImageMethodsView.transferTypeCombo = transferTypeCombo;
	}

	/**
	 * A method to transfer byte information directly to an image as one container.
	 * If no image is present a new one will be created. This method expects the
	 * variables imageSizeX, imageSizeY to be present in the R workspace.
	 * 
	 * @param dataName the name of the R vector.
	 * @param type     the type of the image (1 = byte, 2 = float).
	 */
	public static void transferImageInPlace(String dataName, int type) {

		ImageMethods.transferImageInPlace(dataName, type);

	}

	/**
	 * A method to transfer Color information (as datatype raw) directly to an image
	 * as one container. If no image is present a new one will be created. This
	 * method expects the variables imageSizeX, imageSizeY to be present in the R
	 * workspace.
	 * 
	 * @param dataName the name of the raw R vector.
	 * @param type     the type of the image.
	 */
	public static void transferRGBImageInPlace(String dataName) {
		ImageMethods.transferRGBImageInPlace(dataName);
	}

	public void dispose() {

		super.dispose();
	}
}
