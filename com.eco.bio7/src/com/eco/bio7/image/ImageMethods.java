/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.image;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.discrete.Field;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.image.r.IJTranserResultsTable;
import com.eco.bio7.image.r.TransferImageStack;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import com.swtdesigner.ResourceManager;

/**
 * This class provides some static methods for the analysis and transfer of
 * images inside the Bio7 application.
 * 
 * 
 * @author Bio7
 * 
 */
public class ImageMethods extends ViewPart {
	public ImageMethods() {
	}

	private Combo toimageJCombo;

	private static Combo transferTypeCombo;

	private Text imageMatrixNameFromR;

	private Text imageMatrixNameToR;

	public static final String ID = "com.eco.bio7.image_methods";

	private Composite top = null;

	private Button button = null;

	private Scale scale = null;

	private Scale scale1 = null;

	private Button button1 = null;

	private Button button2 = null;

	private static BufferedImage image = null;

	private Button button3 = null;

	private Button button5 = null;

	private Scale scale2 = null;

	private Button button6 = null;

	private Button button8 = null;

	private Button button9 = null;

	private Label label = null;

	private Label label1 = null;

	private Label label2 = null;

	private Label label3 = null;

	private Label label4 = null;

	private static Spinner spinner = null;

	private static Spinner spinner1 = null;

	private static int fieldx = 1000;// The area of analysis !

	private static int fieldy = 1000;// The area of analysis !

	private Label label51 = null;

	private Label label52 = null;

	private Spinner spinner2 = null;

	private Label label53 = null;

	private Button button4 = null;

	private Label label54 = null;

	private Spinner spinner3 = null;

	private Label label55 = null;

	private static ImageMethods im;

	private static int pointScale = 1;

	private static boolean Centroid = true;

	private Button button7 = null;

	protected boolean canTransfer = true;

	protected boolean canTransferPic = true;

	protected int transferImageType;

	protected int transferIntegers;

	protected boolean transferBackIntegers;

	// private IPreferenceStore store;

	private Button btnAreas;

	private Scale scale_2;

	private Button selectedRoiPixelButton;

	private Button btnPixelRoiStack;

	private Image bildGif;

	private Image deletePicGif;

	private Image regelmaessigGif;

	private Image deletePoints;

	private Image toIJGif;

	private Image rGif;

	private Button btnNewButton_1;

	protected static boolean createMatrix;

	public void createPartControl(Composite parent) {
		// store = Bio7Plugin.getDefault().getPreferenceStore();
		im = this;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.imagemethods");
		GridData gridData14 = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gridData14.widthHint = 86;
		gridData14.horizontalIndent = 0;
		GridData gridData3 = new GridData();
		gridData3.horizontalAlignment = GridData.FILL;
		gridData3.grabExcessHorizontalSpace = true;
		gridData3.verticalAlignment = GridData.CENTER;
		GridData gridData21 = new GridData();
		gridData21.horizontalAlignment = GridData.FILL;
		gridData21.horizontalIndent = 0;
		gridData21.grabExcessHorizontalSpace = true;
		gridData21.grabExcessVerticalSpace = false;
		gridData21.verticalAlignment = GridData.CENTER;
		GridLayout gridLayout1 = new GridLayout();
		gridLayout1.marginRight = 3;
		gridLayout1.marginLeft = 3;
		gridLayout1.makeColumnsEqualWidth = true;
		gridLayout1.numColumns = 2;
		gridLayout1.verticalSpacing = 0;
		gridLayout1.marginWidth = 0;
		gridLayout1.horizontalSpacing = 2;
		top = new Composite(parent, SWT.NONE);
		top.setLayout(gridLayout1);
		label = new Label(top, SWT.NONE);
		label.setText("Alpha Image");
		label.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		label1 = new Label(top, SWT.NONE);
		label1.setText("Alpha Quadgrid");
		label1.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		scale = new Scale(top, SWT.NONE);
		scale.setMaximum(255);
		scale.setToolTipText("Alpha value of the image in the Points panel");
		scale.setLayoutData(gridData14);
		scale.setSelection(255);
		scale1 = new Scale(top, SWT.NONE);
		scale1.setMaximum(255);
		scale1.setSelection(255);
		scale1.setToolTipText("Alpha value for the Quadgrid");
		GridData gd_scale1 = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_scale1.widthHint = 78;
		scale1.setLayoutData(gd_scale1);

		scale1.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				alphaQuadgrid();
			}

		});
		button = new Button(top, SWT.NONE);
		bildGif = new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/bild.gif"));
		button.setImage(bildGif);
		button.setToolTipText("Open an image in the Points panel");
		button.setLayoutData(gridData21);
		button5 = new Button(top, SWT.NONE);
		deletePicGif = new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/deletepic.gif"));
		button5.setImage(deletePicGif);
		button5.setLayoutData(gridData3);
		button5.setToolTipText("Remove the current image of the Points panel");
		GridData gridData1 = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData1.horizontalIndent = 0;
		button1 = new Button(top, SWT.NONE);
		regelmaessigGif = new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/regelmaessig.gif"));
		button1.setImage(regelmaessigGif);
		button1.setToolTipText("Set or unset the Quadgrid as a layer in the Points panel");
		button1.setLayoutData(gridData1);
		button1.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				loadField();
				// widgetSelected()
			}

		});
		GridData gridData20 = new GridData();
		gridData20.horizontalAlignment = GridData.FILL;
		gridData20.grabExcessHorizontalSpace = true;
		gridData20.verticalAlignment = SWT.FILL;
		button3 = new Button(top, SWT.NONE);
		deletePoints = new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/deletepoints.gif"));
		button3.setImage(deletePoints);
		button3.setLayoutData(gridData20);
		button3.setToolTipText("Delete points in the Points panel");
		button3.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				deletePoints();
				if (PointPanel.showVoronoi == true) {
					PointPanel.cleanVoronoi();

				}
				if (PointPanel.showDelauney == true) {
					PointPanel.cleanDelauney();

				}

			}

		});

		final Label resizeQuadsLabel = new Label(top, SWT.NONE);
		resizeQuadsLabel.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, 1, 1));
		resizeQuadsLabel.setText("Resize Quads");
		label2 = new Label(top, SWT.NONE);
		label2.setText("Resize Points Panel");
		label2.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));

		final Scale scale_1 = new Scale(top, SWT.NONE);
		scale_1.setToolTipText("If Quads are enabled this slider resizes the quads from the Quads view");
		scale_1.setMinimum(1);
		scale_1.setSelection(100);
		scale_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				scale_1.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(final SelectionEvent e) {
						if (Quad2d.getQuad2dInstance() != null) {
							if (PointPanel.isQuad2d_visible()) {
								Quad2d.getQuad2dInstance().quadResize(scale_1.getSelection());
								PointPanel.doPaint();
							}
						}
					}
				});
			}
		});
		GridData gd_scale_1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_scale_1.widthHint = 84;
		scale_1.setLayoutData(gd_scale_1);
		scale2 = new Scale(top, SWT.NONE);
		scale2.setMinimum(1);
		scale2.setToolTipText("Scale down the Points panel");
		scale2.setPageIncrement(10000);
		scale2.setIncrement(10000);
		GridData gd_scale2 = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_scale2.widthHint = 66;
		scale2.setLayoutData(gd_scale2);
		scale2.setMaximum(10000);
		scale2.setSelection(10000);
		scale2.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				scaleImage();

			}

		});
		GridData gridData10 = new GridData();
		gridData10.horizontalAlignment = GridData.CENTER;
		gridData10.verticalAlignment = GridData.CENTER;
		label51 = new Label(top, SWT.NONE);
		label51.setText("Fieldsize X");
		label51.setLayoutData(gridData10);
		GridData gridData9 = new GridData();
		gridData9.horizontalAlignment = GridData.CENTER;
		gridData9.verticalAlignment = GridData.CENTER;
		label52 = new Label(top, SWT.NONE);
		label52.setText("Fieldsize Y");
		label52.setLayoutData(gridData9);
		GridData gridData15 = new GridData();
		gridData15.horizontalAlignment = GridData.FILL;
		gridData15.verticalAlignment = GridData.CENTER;
		spinner = new Spinner(top, SWT.BORDER);
		spinner.setMaximum(1000000);
		spinner.setLayoutData(gridData15);
		spinner.setToolTipText("Select field size in x direction");
		spinner.setSelection(fieldx);
		spinner.setMinimum(1);
		spinner.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				resizePointpanel2();
			}

		});
		GridData gridData2 = new GridData();
		gridData2.horizontalAlignment = GridData.FILL;
		gridData2.verticalAlignment = GridData.CENTER;
		spinner1 = new Spinner(top, SWT.BORDER);
		spinner1.setMaximum(1000000);
		spinner1.setLayoutData(gridData2);
		spinner1.setToolTipText("Select the field size in y direction");
		spinner1.setSelection(fieldy);
		spinner1.setMinimum(1);

		spinner1.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {

			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				resizePointpanel();
			}

		});
		GridData gridData121 = new GridData();
		gridData121.horizontalAlignment = GridData.CENTER;
		gridData121.verticalAlignment = GridData.CENTER;
		label53 = new Label(top, SWT.NONE);
		label53.setText("Point Size");
		label53.setLayoutData(gridData121);

		GridData gridData23 = new GridData();
		gridData23.horizontalAlignment = GridData.CENTER;
		gridData23.verticalAlignment = GridData.CENTER;
		label55 = new Label(top, SWT.NONE);
		label55.setText("Alpha");
		label55.setLayoutData(gridData23);
		GridData gridData111 = new GridData();
		gridData111.horizontalAlignment = GridData.FILL;
		gridData111.verticalAlignment = GridData.CENTER;
		spinner2 = new Spinner(top, SWT.NONE);
		spinner2.setToolTipText("Adjust the point size in the Points panel");

		spinner2.setMinimum(1);
		spinner2.setLayoutData(gridData111);
		spinner2.setSelection(5);
		spinner2.setMaximum(100000);
		spinner2.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				adjustDiamterPoints();
			}

		});
		GridData gridData11 = new GridData();
		gridData11.horizontalAlignment = GridData.FILL;
		gridData11.verticalAlignment = GridData.CENTER;
		spinner3 = new Spinner(top, SWT.NONE);
		spinner3.setToolTipText("Adjust the alpha value of the points in the Points panel");
		spinner3.setMaximum(255);
		spinner3.setSelection(255);
		spinner3.setMinimum(0);
		spinner3.setLayoutData(gridData11);
		spinner3.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				setPointsPanelAlpha();
			}

		});

		CLabel lblNewLabel_3 = new CLabel(top, SWT.NONE);
		lblNewLabel_3.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 2, 1));
		lblNewLabel_3.setText("Voronoi / Delauney");

		final Button voronoiButton = new Button(top, SWT.NONE);
		voronoiButton.setToolTipText("Creates a Voronoi Diagramm from the Points in the Points Panel");
		voronoiButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				PointPanel.createVoronoi();
				PointPanel.showVoronoi = !PointPanel.showVoronoi;
				if (PointPanel.showVoronoi == false) {
					PointPanel.cleanVoronoi();
					btnAreas.setSelection(false);
					PointPanel.showAreas = false;
				}
				PointPanel.doPaint();

			}
		});
		final GridData gd_voronoiButton = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd_voronoiButton.heightHint = 30;
		voronoiButton.setLayoutData(gd_voronoiButton);
		voronoiButton.setText("Voronoi");

		final Button delauneyButton = new Button(top, SWT.NONE);
		delauneyButton.setToolTipText("Creates a Delauney Triangulation from the Points in the Points Panel");
		delauneyButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				PointPanel.createDelauney();
				PointPanel.showDelauney = !PointPanel.showDelauney;
				if (PointPanel.showDelauney == false) {
					PointPanel.cleanDelauney();
				}
				PointPanel.doPaint();

			}
		});
		final GridData gd_delauneyButton = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd_delauneyButton.heightHint = 30;
		delauneyButton.setLayoutData(gd_delauneyButton);
		delauneyButton.setText("Delauney");

		final Button dynamicButton = new Button(top, SWT.CHECK);
		dynamicButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		dynamicButton.setToolTipText("Dynamic Voronoi visualization");
		dynamicButton.setSelection(true);
		dynamicButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				PointPanel p = PointPanel.getPointPanel();
				p.dynamicVoronoi = !p.dynamicVoronoi;
				PointPanel.doPaint();
			}
		});
		dynamicButton.setText("Dynamic Voro.");

		final Button drawAreasButton = new Button(top, SWT.CHECK);
		drawAreasButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		drawAreasButton.setToolTipText("Clip Voronoi Areas");
		drawAreasButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				if (drawAreasButton.getSelection()) {
					DelaunayAndVoronoiApp.setIntersection(true);
					PointPanel.cleanVoronoi();
					PointPanel.doPaint();

				} else {
					DelaunayAndVoronoiApp.setIntersection(false);
					PointPanel.cleanVoronoi();

				}
				PointPanel.createVoronoi();
				PointPanel.doPaint();
			}
		});
		drawAreasButton.setText("Clip Areas");

		Button btnDynamic = new Button(top, SWT.CHECK);
		btnDynamic.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnDynamic.setToolTipText("Dynamic Delauney visualization");
		btnDynamic.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				PointPanel p = PointPanel.getPointPanel();
				p.dynamicDelauney = !p.dynamicDelauney;

				PointPanel.doPaint();
			}
		});
		btnDynamic.setText("Dynamic Del.");

		btnAreas = new Button(top, SWT.CHECK);
		btnAreas.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnAreas.setToolTipText("Draw Voronoi Areas");
		btnAreas.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (btnAreas.getSelection()) {
					PointPanel.showAreas = true;
				} else {
					PointPanel.showAreas = false;
				}
				PointPanel.doPaint();

			}
		});
		btnAreas.setText("Draw Areas");

		Label lblAreas = new Label(top, SWT.NONE);
		lblAreas.setAlignment(SWT.CENTER);
		lblAreas.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		lblAreas.setText("Draw Area Size");

		scale_2 = new Scale(top, SWT.NONE);
		scale_2.setPageIncrement(1);
		scale_2.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				PointPanel.setDrawAreaScaled((1.0 - ((10.0 - (double) scale_2.getSelection())) / 10));
				PointPanel.doPaint();
			}
		});
		scale_2.setMaximum(10);
		scale_2.setMinimum(1);
		scale_2.setSelection(10);
		GridData gd_scale_2 = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
		gd_scale_2.widthHint = 71;
		scale_2.setLayoutData(gd_scale_2);

		CLabel lblNewLabel_4 = new CLabel(top, SWT.NONE);
		lblNewLabel_4.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 2, 1));
		lblNewLabel_4.setText("ImageJ And Points Panel");
		GridData gridData22 = new GridData();
		gridData22.heightHint = 30;
		gridData22.horizontalAlignment = GridData.FILL;
		gridData22.grabExcessHorizontalSpace = false;
		gridData22.verticalAlignment = GridData.CENTER;
		button6 = new Button(top, SWT.NONE);
		// button6.setFont(SWTResourceManager.getFont("Courier New", 9,
		// SWT.BOLD));
		button6.setText("Pic->     ");
		toIJGif = ResourceManager.getPluginImage(Bio7Plugin.getDefault(), "bin/pics/toij.gif");
		button6.setImage(toIJGif);
		button6.setLayoutData(gridData22);
		button6.setToolTipText("Send an image from the ImageJ-Canvas to the Points panel");
		button6.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				plusToBuffered();

			}

		});
		GridData gridData12 = new GridData();
		gridData12.heightHint = 30;
		gridData12.horizontalAlignment = GridData.FILL;
		gridData12.grabExcessHorizontalSpace = true;
		gridData12.verticalAlignment = GridData.CENTER;
		// button6.setFont(new Font(Display.getDefault(), "Tahoma", 8,
		// SWT.BOLD));

		button9 = new Button(top, SWT.NONE);
		// button9.setFont(SWTResourceManager.getFont("Courier New", 9,
		// SWT.BOLD));
		button9.setText("Points    ");
		button9.setImage(toIJGif);
		// button9.setFont(new Font(Display.getDefault(), "Tahoma", 8,
		// SWT.BOLD));
		button9.setLayoutData(gridData12);
		button9.setToolTipText("Transfer Particle values from ImageJ to the Points panel");
		button9.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {

				IJTranserResultsTable.addParticleValues();
				if (PointPanel.showVoronoi == true) {
					PointPanel.createVoronoi();
				}
				if (PointPanel.showDelauney == true) {
					PointPanel.createDelauney();
				}
				PointPanel.doPaint();
			}
		});
		GridData gridData31 = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gridData31.heightHint = 30;
		button8 = new Button(top, SWT.NONE);
		// button8.setFont(SWTResourceManager.getFont("Courier New", 9,
		// SWT.BOLD));
		button8.setText("Pic<-     ");

		button8.setToolTipText("Send an image from the Points panel to the ImageJ-Canvas");
		button8.setImage(toIJGif);
		button8.setLayoutData(gridData31);
		button8.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				sendImageTo();
			}

		});

		GridData gridData13 = new GridData(SWT.FILL, SWT.FILL, false, false);
		gridData13.heightHint = 30;
		// button8.setFont(new Font(Display.getDefault(), "Tahoma", 8,
		// SWT.BOLD));
		button7 = new Button(top, SWT.NONE);
		// button7.setFont(SWTResourceManager.getFont("Courier New", 9,
		// SWT.BOLD));
		button7.setText("Pixel      ");
		button7.setLayoutData(gridData13);

		// button7.setFont(new Font(Display.getDefault(), "Tahoma", 8,
		// SWT.BOLD));
		button7.setImage(toIJGif);
		button7.setToolTipText("Transfer threshold pixels to the Quadgrid");
		button7.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				setAllPixels();
			}
		});

		CLabel lblNewLabel = new CLabel(top, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 2, 1));
		lblNewLabel.setText("Transfer Selected Data To R");

		Button btnNewButton = new Button(top, SWT.NONE);
		rGif = org.eclipse.wb.swt.ResourceManager.getPluginImage("com.eco.bio7", "bin/pics/r.gif");
		btnNewButton.setImage(rGif);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RConnection con = RServe.getConnection();

				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						new IJTranserResultsTable().transferResultsTable(con, true);
					} else {
						Bio7Dialog.message("Rserve is busy!");
					}

				}
			}
		});
		GridData gd_btnNewButton = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_btnNewButton.heightHint = 30;
		btnNewButton.setLayoutData(gd_btnNewButton);
		btnNewButton.setText("IJ RT       ");

		btnNewButton_1 = new Button(top, SWT.NONE);
		rGif = org.eclipse.wb.swt.ResourceManager.getPluginImage("com.eco.bio7", "bin/pics/r.gif");
		btnNewButton_1.setImage(rGif);
		btnNewButton_1.addSelectionListener(new SelectionAdapter() {
			boolean convertToRaster;
			@Override
			public void widgetSelected(SelectionEvent e) {
				RConnection con = RServe.getConnection();

				if (con != null) {
					if (RState.isBusy() == false) {
						RState.setBusy(true);
						
								MessageBox message = new MessageBox(new Shell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
								message.setMessage("Should a Raster Stack be created?");
								message.setText("Raster?");
								int response = message.open();
								if (response == SWT.YES) {

									convertToRaster=true;
								}
								
								else{
									convertToRaster=false;
								}
								TransferImageStack job=	new TransferImageStack("Transfer ImageJ Stack",con,convertToRaster);
								job.addJobChangeListener(new JobChangeAdapter() {
									public void done(IJobChangeEvent event) {
										if (event.getResult().isOK()) {
											
											RState.setBusy(false);
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
		GridData gd_btnNewButton_1 = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_btnNewButton_1.heightHint = 30;
		btnNewButton_1.setLayoutData(gd_btnNewButton_1);
		btnNewButton_1.setText("IJ RasterStack");
		GridData gridData = new org.eclipse.swt.layout.GridData(SWT.FILL, SWT.FILL, false, false);
		gridData.heightHint = 30;
		button2 = new Button(top, SWT.NONE);
		// button2.setFont(SWTResourceManager.getFont("Courier New", 9,
		// SWT.BOLD));
		button2.setText("Points     ");
		rGif = org.eclipse.wb.swt.ResourceManager.getPluginImage("com.eco.bio7", "bin/pics/r.gif");
		button2.setImage(rGif);
		button2.setToolTipText("Transfer data from the Points panel \nto R and (or) the selection coordinates\nto R ");

		// button2.setFont(new Font(Display.getDefault(), "Tahoma", 8,
		// SWT.BOLD));
		button2.setLayoutData(gridData);
		button2.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				RConnection con = RServe.getConnection();

				if (con != null) {
					if (RState.isBusy() == false) {
						new IJTranserResultsTable().pointsToR(con);
					} else {
						Bio7Dialog.message("Rserve is busy!");
					}

				}
			}
		});
		GridData gridData131 = new GridData(SWT.FILL, SWT.FILL, true, false);
		gridData131.heightHint = 30;
		button4 = new Button(top, SWT.NONE);
		// button4.setFont(SWTResourceManager.getFont("Courier New", 9,
		// SWT.BOLD));
		button4.setText("Particles        ");
		button4.setImage(rGif);
		// button4.setFont(new Font(Display.getDefault(), "Tahoma", 8,
		// SWT.BOLD));
		button4.setLayoutData(gridData131);
		button4.setToolTipText("Transfer particle measurement to R");
		button4.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				if (RState.isBusy() == false) {
					SwingUtilities.invokeLater(new Runnable() {
						// !!
						public void run() {
							new IJTranserResultsTable().particledescriptors();
						}
					});
				} else {
					Bio7Dialog.message("Rserve is busy!");
				}
			}

		});

		final Button selectedPixelsButton = new Button(top, SWT.NONE);
		selectedPixelsButton.setToolTipText("" + "Transfers the selected pixels (Freehand, Rectangular etc.)\n" + "with or without a signature as a matrix to R.\n"
				+ "The transfer type can be selected, too!");
		selectedPixelsButton.setImage(ResourceManager.getPluginImage(Bio7Plugin.getDefault(), "bin/pics/r.gif"));
		// selectedPixelsButton.setFont(SWTResourceManager.getFont("Courier New",
		// 9, SWT.BOLD));
		selectedPixelsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAlive()) {
					ImagePlus imp = WindowManager.getCurrentImage();
					if (imp != null) {

						ImageSelectionTransferJob job = new ImageSelectionTransferJob(transferTypeCombo.getSelectionIndex());
						// job.setSystem(true);
						job.schedule();
					} else {

						Bio7Dialog.message("No image available!");

					}
				} else {
					Bio7Dialog.message("No Rserve connection available!");
				}
			}
		});

		final GridData gd_selectedPixelsButton = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd_selectedPixelsButton.heightHint = 30;
		selectedPixelsButton.setLayoutData(gd_selectedPixelsButton);
		selectedPixelsButton.setText("Pixel       ");

		final Button matchingButton = new Button(top, SWT.NONE);
		matchingButton.setToolTipText("Transfers collected selection coordinates\n as a List of Lists (the single selections) to R.\nThe ROI Manager has to be available!");
		matchingButton.setImage(rGif);
		// matchingButton.setFont(SWTResourceManager.getFont("Courier New", 9,
		// SWT.BOLD));
		matchingButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent e) {
				TransferSelectionCoordsJob job = new TransferSelectionCoordsJob();
				// job.setSystem(true);
				job.schedule();
				/*
				 * MatchingDialoge m=new MatchingDialoge(new Shell()); m.open()
				 */;

			}
		});
		final GridData gd_matchingButton = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd_matchingButton.heightHint = 30;
		matchingButton.setLayoutData(gd_matchingButton);
		matchingButton.setText("Selection       ");

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

						ImageRoiSelectionTransferJob job = new ImageRoiSelectionTransferJob(transferTypeCombo.getSelectionIndex());
						// job.setSystem(true);
						job.schedule();
					} else {

						Bio7Dialog.message("No image available!");

					}
				} else {
					Bio7Dialog.message("No Rserve connection available!");
				}

			}
		});
		GridData gd_selectedRoiPixelButton = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_selectedRoiPixelButton.heightHint = 30;
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

						ImageStackRoiSelectionTransferJob job = new ImageStackRoiSelectionTransferJob(transferTypeCombo.getSelectionIndex());
						// job.setSystem(true);
						job.schedule();
					} else {

						Bio7Dialog.message("No image available!");

					}
				} else {
					Bio7Dialog.message("No Rserve connection available!");
				}

			}
		});
		GridData gd_btnPixelRoiStack = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_btnPixelRoiStack.heightHint = 30;
		btnPixelRoiStack.setLayoutData(gd_btnPixelRoiStack);

		CLabel lblNewLabel_1 = new CLabel(top, SWT.NONE);
		lblNewLabel_1.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 2, 1));
		lblNewLabel_1.setText("Transfer Image Data From and To R");

		final Button picToRButton = new Button(top, SWT.NONE);
		// picToRButton.setFont(SWTResourceManager.getFont("Courier New", 9,
		// SWT.BOLD));
		picToRButton.setText("Pic<-      ");

		picToRButton.setToolTipText("Transfer image data to R");
		picToRButton.setImage(rGif);
		picToRButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
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
			}
		});

		final GridData gd_picToRButton = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd_picToRButton.heightHint = 30;
		picToRButton.setLayoutData(gd_picToRButton);

		final Button picButton = new Button(top, SWT.NONE);
		// picButton.setFont(SWTResourceManager.getFont("Courier New", 9,
		// SWT.BOLD));
		picButton.setText("Pic->           ");

		picButton.setToolTipText("Create an image from the R data");
		picButton.setImage(rGif);
		picButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
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
			}
		});
		final GridData gd_picButton = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd_picButton.heightHint = 30;
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
		transferTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		toimageJCombo = new Combo(top, SWT.READ_ONLY);
		toimageJCombo.setToolTipText("Selects the image type which will be created in Imagej");
		toimageJCombo.select(0);
		toimageJCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

			}
		});
		toimageJCombo.setItems(new String[] { "Colour", "Greyscale", "Float", "Short" });
		toimageJCombo.setText("Float");
		toimageJCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
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
		imageMatrixNameToR.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		imageMatrixNameFromR = new Text(top, SWT.BORDER);
		imageMatrixNameFromR.setToolTipText("The name of the data which will be used to create the image in ImageJ");
		imageMatrixNameFromR.setText("imageMatrix");
		imageMatrixNameFromR.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		CLabel lblNewLabel_2 = new CLabel(top, SWT.NONE);
		lblNewLabel_2.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 2, 1));
		lblNewLabel_2.setText("Multivariate Image Analysis");

		final Button clusterImageButton = new Button(top, SWT.NONE);
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
							ClusterDialog co = new ClusterDialog(new Shell());
							co.open();
							ClusterJob clusterjob = new ClusterJob();
							clusterjob.addJobChangeListener(new JobChangeAdapter() {
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
		final GridData gd_clusterImageButton = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gd_clusterImageButton.heightHint = 30;
		clusterImageButton.setLayoutData(gd_clusterImageButton);

		final Button pcaButton = new Button(top, SWT.NONE);
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
							PcaDialog pca = new PcaDialog(new Shell());
							pca.open();
							PcaJob pcaJob = new PcaJob();
							pcaJob.addJobChangeListener(new JobChangeAdapter() {
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
		final GridData gd_pcaButton = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd_pcaButton.heightHint = 30;
		pcaButton.setLayoutData(gd_pcaButton);
		button5.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {

				deletePicPointpanel();
			}

		});
		scale.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				alphaPic();
			}

		});

		button.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				loadPic();

			}

		});
		initializeToolBar();

	}

	private void setPointsPanelAlpha() {

		int sel = spinner3.getSelection();

		PointPanel.setCompos3(sel);

	}

	private void alphaPic() {
		PointPanel.setCompos(scale.getSelection());

		PointPanel Jp = PointPanelView.getJp();
		Jp.repaint();
	}

	private void alphaQuadgrid() {
		PointPanel.setCompos2(scale1.getSelection());

		PointPanel Jp = PointPanelView.getJp();
		Jp.repaint();
	}

	private void adjustDiamterPoints() {
		PointPanel Jp = PointPanelView.getJp();
		Jp.set_Diameter(spinner2.getSelection());
	}

	private void deletePicPointpanel() {
		image = null;
		PointPanel.setBuffNull();// set Buffered Image to null!
		PointPanel Jp = PointPanelView.getJp();
		Jp.repaint();
		System.gc();
	}

	/**
	 * Deletes all point values, alpha values, ellipse values and species values
	 * in the Points panel and repaints the Points panel.
	 */
	public static void deletePoints() {
		PointPanel.getVe().clear();
		PointPanel.get_Points().clear();
		PointPanel.get_Species().clear();
		PointPanel.get_Alpha().clear();
		PointPanel Jp = PointPanelView.getJp();
		Jp.repaint();
	}

	private void resizePointpanel() {
		PointPanel Jp = PointPanelView.getJp();
		Jp.setPreferredSize(new Dimension((int) (spinner.getSelection() * Jp.getTransformx()), (int) (spinner1.getSelection() * Jp.getTransformy())));
		PointPanelView.getScroll().setViewportView(Jp);
		fieldx = spinner.getSelection();
		fieldy = spinner1.getSelection();

	}

	private void resizePointpanel2() {
		PointPanel Jp = PointPanelView.getJp();
		Jp.setPreferredSize(new Dimension((int) (spinner.getSelection() * Jp.getTransformx()), (int) (spinner1.getSelection() * Jp.getTransformy())));
		PointPanelView.getScroll().setViewportView(Jp);
		fieldx = spinner.getSelection();
		fieldy = spinner1.getSelection();

	}

	private void loadField() {

		PointPanel.setQuad2d_visible(!PointPanel.isQuad2d_visible());
		if (PointPanel.isQuad2d_visible() == true) {
			spinner.setSelection(Field.getQuadSize() * Field.getWidth());
			spinner1.setSelection(Field.getQuadSize() * Field.getHeight());
			PointPanel Jp = PointPanelView.getJp();
			Jp.setPreferredSize(new Dimension((int) (spinner.getSelection() * Jp.getTransformx()), (int) (spinner1.getSelection() * Jp.getTransformy())));
			PointPanelView.getScroll().setViewportView(Jp);
			fieldx = spinner.getSelection();
			fieldy = spinner1.getSelection();
		}
		PointPanel Jp = PointPanelView.getJp();
		Jp.repaint();
	}

	private void scaleImage() {
		final PointPanel Jp = PointPanelView.getJp();

		int select = scale2.getSelection();

		double sc = select;
		double factor = 10000;
		double scaled = sc / 10000;

		Jp.setSx(scaled);
		Jp.setSy(scaled);

		Jp.setPreferredSize(new Dimension((int) (spinner.getSelection() * scaled), (int) (spinner1.getSelection() * scaled)));
		Jp.repaint();
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				// !!
				public void run() {

					PointPanelView.getScroll().setViewportView(Jp);
				}
			});
		} catch (InterruptedException e1) {

			e1.printStackTrace();
		} catch (InvocationTargetException e1) {

			e1.printStackTrace();
		}
	}

	private void sendImageTo() {
		PointPanel Jp = PointPanelView.getJp();
		BufferedImage sprite = null;
		if (image != null) {
			sprite = new BufferedImage((int) (fieldx * Jp.getTransformx()), (int) (fieldy * Jp.getTransformy()), BufferedImage.TYPE_INT_RGB);
		} else {
			sprite = new BufferedImage((int) (fieldx * Jp.getTransformx()), (int) (fieldy * Jp.getTransformy()), BufferedImage.TYPE_INT_RGB);
		}

		Graphics g2D = sprite.createGraphics();

		Jp.paintComponent(g2D);

		// java.awt.Image awtimage = toImage(sprite);
		ImagePlus imp = new ImagePlus("Bio7", sprite);
		imp.show();
	}

	private void plusToBuffered() {

		// ImagePlus imp = WindowManager.getCurrentWindow().getImagePlus();
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp != null) {
			java.awt.Image imageawt = imp.getImage();
			BufferedImage bufferdimage = createBufferedImage(imageawt);
			image = bufferdimage;
			PointPanel Jp = PointPanelView.getJp();

			Jp.setPreferredSize(new Dimension((int) (bufferdimage.getWidth() * Jp.getTransformx()), (int) (bufferdimage.getHeight() * Jp.getTransformy())));
			PointPanelView.getScroll().setViewportView(Jp);
			Jp.setBuff(bufferdimage);
			fieldx = bufferdimage.getWidth();
			fieldy = bufferdimage.getHeight();
			spinner.setSelection(bufferdimage.getWidth());
			spinner1.setSelection(bufferdimage.getHeight());
		} else {
			Bio7Dialog.message("No image availabe in the ImageJ-Canvas view!");
		}
	}

	private void loadPic() {

		Shell s = new Shell();
		FileDialog fd = new FileDialog(s, SWT.OPEN);
		fd.setText("Open");

		String[] filterExt = { "*.*", "*.jpg", "*.JPG", ".jpeg", ".png", ".gif" };
		fd.setFilterExtensions(filterExt);
		String selected = fd.open();
		if (selected != null) {
			File file = new File(selected);

			try {
				image = ImageIO.read(file);
			} catch (IOException e1) {

				e1.printStackTrace();
			}

			PointPanel Jp = PointPanelView.getJp();
			Jp.setPreferredSize(new Dimension((int) (image.getWidth() * Jp.getTransformx()), (int) (image.getHeight() * Jp.getTransformy())));
			PointPanelView.getScroll().setViewportView(Jp);
			Jp.setBuff(image);

			Jp.setBuff(image);
			fieldx = image.getWidth();
			fieldy = image.getHeight();
			spinner.setSelection(image.getWidth());
			spinner1.setSelection(image.getHeight());

		}
	}

	public void setFocus() {
		top.setFocus();
	}

	private BufferedImage createBufferedImage(java.awt.Image image) {

		int w = image.getWidth(null);
		int h = image.getHeight(null);
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = bi.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return bi;
	}

	private static java.awt.Image toImage(BufferedImage bufferedImage) {
		return Toolkit.getDefaultToolkit().createImage(bufferedImage.getSource());
	}

	private void startImageJ() {

		if (IJ.getInstance() == null) {

		} else {
			IJ.getInstance().setVisible(true);
		}

	}

	private static void setAllPixels() {

		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp != null) {
			/* Get the image processor of the image! */
			ImageProcessor ip = imp.getProcessor();
			int w = ip.getWidth();
			int h = ip.getHeight();

			/* We proof if the image is an 8-bit image! */
			if (ip instanceof ByteProcessor) {
				for (int v = 0; v < h; v++) {
					for (int u = 0; u < w; u++) {

						int i = ip.getPixel(u, v);

						/* Now we get the threshold interval! */
						double min = ip.getMinThreshold();
						double max = ip.getMaxThreshold();

						/* All values which match the threshold are assigned! */
						if (i >= min && i <= max) {

							pixelToQuad(u, v);

						}

					}
				}
			} else {
				Bio7Dialog.message("Requires an 8-bit image !");
			}

		} else {
			Bio7Dialog.message("No image opened !");

		}
		PointPanel.doPaint();
	}

	private static void pixelToQuad(double cmx, double cmy) {

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

	private static ImageMethods getImageMethods() {
		return im;
	}

	/**
	 * Transfers the current image from the Points panel to the ImageJ panel.
	 */
	public static void toImageJ() {

		PointPanel Jp = PointPanelView.getJp();
		BufferedImage sprite = null;

		sprite = new BufferedImage((int) (fieldx * Jp.getTransformx()), (int) (fieldy * Jp.getTransformy()), BufferedImage.TYPE_INT_RGB);

		Graphics g2D = sprite.createGraphics();

		Jp.paintComponent(g2D);

		java.awt.Image awtimage = toImage(sprite);
		ImagePlus imp = new ImagePlus("Bio7", awtimage);
		imp.show();

	}

	/**
	 * Returns the current image in the Points panel.
	 * 
	 * @return an ImagePlus object.
	 */
	public static ImagePlus getPanelImage() {

		PointPanel Jp = PointPanelView.getJp();
		BufferedImage sprite = null;

		sprite = new BufferedImage((int) (fieldx * Jp.getTransformx()), (int) (fieldy * Jp.getTransformy()), BufferedImage.TYPE_INT_RGB);

		Graphics g2D = sprite.createGraphics();

		Jp.paintComponent(g2D);

		java.awt.Image awtimage = toImage(sprite);
		ImagePlus imp = new ImagePlus("Bio7", awtimage);

		return imp;
	}

	/* The size of the Area of Analysis! */
	/**
	 * Returns the x-size of the Area of Analysis.
	 * 
	 * @return the width of the area of analysis.
	 */
	public static int getFieldX() {
		return fieldx;
	}

	/* The size of the Area of Analysis! */
	/**
	 * Returns the y-size of the Area of Analysis.
	 * 
	 * @return the height of the area of analysis.
	 */
	public static int getFieldY() {
		return fieldy;
	}

	/**
	 * Sets the field size in the Points panel(Area of Analysis).
	 * 
	 * @param x
	 *            the x-size.
	 * @param y
	 *            the y-size.
	 */
	public static void setFieldSize(final int x, final int y) {
		final PointPanel Jp = PointPanelView.getJp();
		Jp.setPreferredSize(new Dimension((int) (x * Jp.getTransformx()), (int) (y * Jp.getTransformy())));
		SwingUtilities.invokeLater(new Runnable() {
			// !!
			public void run() {
				PointPanelView.getScroll().setViewportView(Jp);

			}
		});
		fieldx = x;
		fieldy = y;

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				if (spinner != null && spinner1 != null && !spinner.isDisposed() && !spinner1.isDisposed()) {
					spinner.setSelection(x);
					spinner1.setSelection(y);
				}
			}
		});

	}

	/**
	 * Returns the scale factor for the coordinates of the points from the
	 * particle analysis.
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
	 * @param pointScale
	 *            an integer value of the scale factor.
	 */
	public static void setPointScale(int pointScale) {
		ImageMethods.pointScale = pointScale;
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
	 * @param centroid
	 *            a boolean value.
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
	 * This methods transfers image data from ImageJ to R by means of Rserve.
	 * Matrix or vector data is created and variables for the width, height and
	 * the name of the image will be created.
	 * 
	 * 
	 * @param name
	 *            the name for the data.
	 * @param matrix
	 *            a boolean, if true automatically a data matrix will be created
	 *            (only for double values - in case of an RGB transfer an
	 *            integer matrix will be created!).
	 * @param transferDataType
	 *            an integer value for the data type which will be transfered.
	 *            (0=double, 1=integer, 2=byte, 3=RGB as single byte vectors or
	 *            integer matrix)
	 * @param impPlus an optional ImagePlus object as the default image source.
	 * 
	 */

	public static void imageToR(String name, boolean matrix, int transferDataType, ImagePlus impPlus) {
		double[] pDouble = null;
		int[] pInt = null;
		byte[] pByte = null;

		byte[] pRByte = null;
		byte[] pGByte = null;
		byte[] pBByte = null;
		int y = 0;
		int x = 0;
		ImagePlus imp = null;
		if (RServe.isAlive()) {
			RConnection c = RServe.getConnection();
			/* Get the active image ! */

			if (impPlus == null) {
				imp = WindowManager.getCurrentImage();
			} else {
				imp = impPlus;
			}
			if (imp != null) {

				//if (transferDataType == 0 || transferDataType == 3) {
					createMatrix = matrix;
				//} else {
					//createMatrix = false;
				//}

				/* Get the image processor of the image ! */
				ImageProcessor ip = imp.getProcessor();
				int w = ip.getWidth();
				int h = ip.getHeight();

				try {
					c.eval("imageSizeY<-" + h);

					c.eval("imageSizeX<-" + w);
					c.eval("imageDataName<-'" + name + "'");
				} catch (RserveException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				/* Double transfer type */
				if (transferDataType == 0) {
					/* From Float images we transfer the scaled values! */
					if (ip instanceof FloatProcessor) {

						pDouble = new double[w * h];

						for (int z = 0; z < h * w; z++) {

							if (x > (w - 1)) {
								y++;
								x = 0;
							}
							int v = ip.getPixel(x, y);
							pDouble[z] = Float.intBitsToFloat(v);

							if (x < w) {
								x++;
							}
						}

					}
					/* Normal transfer of double values! */
					else {
						pDouble = new double[w * h];

						for (int z = 0; z < h * w; z++) {

							if (x > (w - 1)) {
								y++;
								x = 0;
							}
							pDouble[z] = (double) ip.getPixel(x, y);

							if (x < w) {
								x++;
							}
						}
					}
					/* We transfer the values to R! */

					try {
						c.assign(name, pDouble);
					} catch (REngineException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				/* Integer transfer type! */
				else if (transferDataType == 1) {
					pInt = new int[w * h];

					for (int z = 0; z < h * w; z++) {

						if (x > (w - 1)) {
							y++;
							x = 0;
						}
						pInt[z] = ip.getPixel(x, y);

						if (x < w) {
							x++;
						}
					}

					/* We transfer the values to R! */

					try {
						c.assign(name, pInt);
					} catch (REngineException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				/* Byte transfer type! */
				else if (transferDataType == 2) {
					// pByte = (byte[])ip.getPixels();

					/*
					 * The above method does not work and causes an error when a
					 * float image is transfered! This does not happen if every
					 * single value is converted to a byte!
					 */
					pByte = new byte[w * h];
					for (int z = 0; z < h * w; z++) {

						if (x > (w - 1)) {
							y++;
							x = 0;
						}
						pByte[z] = (byte) ip.getPixel(x, y);

						if (x < w) {
							x++;
						}
					}

					/* We transfer the values to R! */

					try {
						c.assign(name, pByte);
					} catch (REngineException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
				/* R,G,B Byte transfer type! */
				else if (transferDataType == 3) {
					// pByte = (byte[])ip.getPixels();

					/*
					 * The above method does not work and causes an error when a
					 * float image is transfered! This does not happen if every
					 * single value is converted to a byte!
					 */
					pRByte = new byte[w * h];
					pGByte = new byte[w * h];
					pBByte = new byte[w * h];
					for (int z = 0; z < h * w; z++) {

						if (x > (w - 1)) {
							y++;
							x = 0;
						}
						int RGB = ip.getPixel(x, y);

						pRByte[z] = (byte) ((RGB >> 16) & 0xff);
						pGByte[z] = (byte) ((RGB >> 8) & 0xff);
						pBByte[z] = (byte) (RGB & 0xff);

						if (x < w) {
							x++;
						}
					}

					/* We transfer the values to R! */

					try {
						c.assign(name + "_R", pRByte);
						c.assign(name + "_G", pGByte);
						c.assign(name + "_B", pBByte);
					} catch (REngineException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

				/*
				 * We create a matrix with the width and height from the array
				 * or list. Only in double mode!
				 */

				if (createMatrix) {
					if (transferDataType == 3) {// Not used at the moment!
						try {

							c.eval("try(" + name + "<-cbind(as.integer(" + name + "_R" + ")))");
							c.eval("try(remove(" + name + "_R" + "))");
							c.eval("try(" + name + "<-cbind(" + name + ",as.integer(" + name + "_G" + ")))");
							c.eval("try(remove(" + name + "_G" + "))");
							c.eval("try(" + name + "<-cbind(" + name + ",as.integer(" + name + "_B" + ")))");
							c.eval("try(remove(" + name + "_B" + "))");

						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					} else {

						try {
							c.eval("try(" + name + "<-matrix(" + name + "," + w + "," + h + "))");

							// c.eval("try(remove(imageData))");
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

			} else {
				System.out.println("No image available!");
			}
			pDouble = null;
			pInt = null;
			pByte = null;
			pRByte = null;
			pGByte = null;
			pBByte = null;

		}
	}

	/**
	 * This method creates an ImageJ image from the given named data (matrix or
	 * vector) in R. This method expects the variables imageSizeX, imageSizeY to
	 * be present in the R workspace if vector data is transferred.
	 * 
	 * @param type
	 *            an integer which represents an ImageJ image
	 *            type.(0=ColourProcessor, 1=ByteProcessor, 2=FloatProcessor,
	 *            3=ShortProcessor)
	 * @param name
	 *            a string identifier for the R data.
	 * 
	 * @param transferDataType
	 *            the data type as transfer type (0=double, 1=integer, 2=byte).
	 * 
	 */

	public static void imageFromR(int type, String name, int transferDataType) {
		ImagePlus imp;
		double[][] matrix = null;
		REXPLogical bolXExists = null;
		REXPLogical bolYExists = null;
		REXPLogical bolDoubleExists = null;
		REXPLogical bolExists = null;
		REXPLogical bolRawExists = null;
		REXPLogical isNumeric = null;
		ImageProcessor ip;
		RConnection c = RServe.getConnection();
		if (RServe.isAliveDialog()) {
			if (name.isEmpty() == false && name != null) {

				try {
					bolExists = (REXPLogical) c.eval("try(exists(\"" + name + "\"))");

				} catch (RserveException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				boolean[] bolE = bolExists.isTrue();
				/*
				 * if(bolE[0]){
				 */
				if (bolE[0] && name.equals("image") == false) {

					try {
						isNumeric = (REXPLogical) c.eval("try(is.numeric(" + name + ")||is.raw(" + name + "))");
					} catch (RserveException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					boolean[] bolNumeric = isNumeric.isTrue();

					if (bolNumeric[0]) {

						/*****************************************************************************************/
						/* Transfer doubles! */
						if (transferDataType == 0) {

							REXPLogical bol = null;
							try {
								bol = (REXPLogical) c.eval("try(is.matrix(" + name + "))");
							} catch (RserveException e1) {

								e1.printStackTrace();
							}
							boolean[] bo = bol.isTrue();
							/************************ as matrix! **************************/
							if (bo[0]) {
								try {
									matrix = c.eval("try(" + name + ")").asDoubleMatrix();
								} catch (REXPMismatchException e) {

									e.printStackTrace();
								} catch (RserveException e) {

									e.printStackTrace();
								}

								if (matrix != null) {
									if (type == 0) {
										ip = new ColorProcessor(matrix.length, matrix[0].length);
										for (int i = 0; i < matrix.length; i++) {
											for (int u = 0; u < matrix[0].length; u++) {
												int value = (int) (matrix[i][u]);
												ip.putPixel(i, u, value);

											}

										}
									} else if (type == 1) {
										ip = new ByteProcessor(matrix.length, matrix[0].length);
										for (int i = 0; i < matrix.length; i++) {
											for (int u = 0; u < matrix[0].length; u++) {
												int value = (int) (matrix[i][u]);
												ip.putPixel(i, u, value);

											}

										}
									} else if (type == 2) {
										ip = new FloatProcessor(matrix.length, matrix[0].length);
										for (int i = 0; i < matrix.length; i++) {
											for (int u = 0; u < matrix[0].length; u++) {
												double value = (matrix[i][u]);
												ip.putPixelValue(i, u, value);

											}

										}
										ip.resetMinAndMax();
									} else {
										ip = new ShortProcessor(matrix.length, matrix[0].length);
										for (int i = 0; i < matrix.length; i++) {
											for (int u = 0; u < matrix[0].length; u++) {
												double value = (matrix[i][u]);
												ip.putPixelValue(i, u, value);

											}

										}
										ip.resetMinAndMax();
									}

									imp = new ImagePlus(name, ip);
									imp.show();
									matrix = null;
								}
							}
							/************************ as list! **************************/
							else {
								try {
									bolXExists = (REXPLogical) c.eval("try(exists(\"imageSizeX\"))");
									bolYExists = (REXPLogical) c.eval("try(exists(\"imageSizeY\"))");
								} catch (RserveException e1) {

									e1.printStackTrace();
								}
								boolean[] bolX = bolXExists.isTrue();
								boolean[] bolY = bolYExists.isTrue();
								if (bolX[0] == true && bolY[0] == true) {
									double[] imageData = null;
									int imagesSizeY;
									try {
										imagesSizeY = (int) c.eval("try(imageSizeY)").asDouble();

										int imagesSizeX = (int) c.eval("try(imageSizeX)").asDouble();
										/*
										 * We can proof for integer as transfer
										 * data type so we don't have to proof
										 * for byte, too! integers can be
										 * transfered as doubles!
										 */
										bolDoubleExists = (REXPLogical) c.eval("try(is.double(" + name + "))");
										boolean[] bolInteger = bolDoubleExists.isTrue();
										if (bolInteger[0]) {
											imageData = c.eval("try(" + name + ")").asDoubles();
										} else {
											/*
											 * Again we only need to convert the
											 * data to integers if it raw data
											 * e.g.!
											 */
											imageData = c.eval("try(as.integer(" + name + "))").asDoubles();
										}

										if (imageData != null) {
											if (type == 0) {
												ip = new ColorProcessor(imagesSizeX, imagesSizeY);

												for (int i = 0; i < imagesSizeY; i++) {

													for (int u = 0; u < imagesSizeX; u++) {
														double value = (imageData[i * imagesSizeX + u]);
														ip.putPixel(u, i, (int) value);

													}

												}
											} else if (type == 1) {
												ip = new ByteProcessor(imagesSizeX, imagesSizeY);
												for (int i = 0; i < imagesSizeY; i++) {

													for (int u = 0; u < imagesSizeX; u++) {
														double value = (imageData[i * imagesSizeX + u]);

														ip.putPixelValue(u, i, value);

													}

												}
											} else if (type == 2) {
												ip = new FloatProcessor(imagesSizeX, imagesSizeY);
												for (int i = 0; i < imagesSizeY; i++) {

													for (int u = 0; u < imagesSizeX; u++) {
														double value = (imageData[i * imagesSizeX + u]);
														ip.putPixelValue(u, i, value);

													}

												}
												ip.resetMinAndMax();
											} else {
												ip = new ShortProcessor(imagesSizeX, imagesSizeY);
												for (int i = 0; i < imagesSizeY; i++) {

													for (int u = 0; u < imagesSizeX; u++) {
														double value = (imageData[i * imagesSizeX + u]);
														ip.putPixelValue(u, i, value);

													}

												}
												ip.resetMinAndMax();
											}

											imp = new ImagePlus(name, ip);
											imp.show();

										}

									} catch (REXPMismatchException e) {

										e.printStackTrace();
									} catch (RserveException e) {

										e.printStackTrace();
									}
									imageData = null;

								} else {
									Bio7Dialog.message("The size variables (imageSizeX, imageSizeY)\n are not available!");
								}
							}
							/************************************* Transfer integers! *******************************************/

						} else if (transferDataType == 1) {

							try {
								bolXExists = (REXPLogical) c.eval("try(exists(\"imageSizeX\"))");
								bolYExists = (REXPLogical) c.eval("try(exists(\"imageSizeY\"))");
							} catch (RserveException e1) {

								e1.printStackTrace();
							}
							boolean[] bolX = bolXExists.isTrue();
							boolean[] bolY = bolYExists.isTrue();
							if (bolX[0] == true && bolY[0] == true) {
								int[] imageData = null;
								int imagesSizeY;
								try {
									imagesSizeY = (int) c.eval("try(imageSizeY)").asDouble();

									int imagesSizeX = (int) c.eval("try(imageSizeX)").asDouble();
									bolDoubleExists = (REXPLogical) c.eval("try(is.integer(" + name + "))");
									boolean[] bolInteger = bolDoubleExists.isTrue();
									if (bolInteger[0]) {
										imageData = c.eval("try(" + name + ")").asIntegers();
									} else {
										imageData = c.eval("try(as.integer(" + name + "))").asIntegers();
									}

									if (imageData != null) {
										if (type == 0) {
											ip = new ColorProcessor(imagesSizeX, imagesSizeY);

											for (int i = 0; i < imagesSizeY; i++) {

												for (int u = 0; u < imagesSizeX; u++) {
													int value = (imageData[i * imagesSizeX + u]);
													ip.putPixel(u, i, value);

												}

											}
										} else if (type == 1) {
											ip = new ByteProcessor(imagesSizeX, imagesSizeY);
											for (int i = 0; i < imagesSizeY; i++) {

												for (int u = 0; u < imagesSizeX; u++) {
													int value = (imageData[i * imagesSizeX + u]);

													ip.putPixel(u, i, value);

												}

											}
										} else if (type == 2) {
											ip = new FloatProcessor(imagesSizeX, imagesSizeY);
											for (int i = 0; i < imagesSizeY; i++) {

												for (int u = 0; u < imagesSizeX; u++) {
													int value = (imageData[i * imagesSizeX + u]);
													ip.putPixel(u, i, value);

												}

											}
											ip.resetMinAndMax();
										} else {
											ip = new ShortProcessor(imagesSizeX, imagesSizeY);
											for (int i = 0; i < imagesSizeY; i++) {

												for (int u = 0; u < imagesSizeX; u++) {
													int value = (imageData[i * imagesSizeX + u]);
													ip.putPixel(u, i, value);

												}

											}
											ip.resetMinAndMax();
										}

										imp = new ImagePlus(name, ip);
										imp.show();

									}

								} catch (REXPMismatchException e) {

									e.printStackTrace();
								} catch (RserveException e) {

									e.printStackTrace();
								}
								imageData = null;
							} else {
								Bio7Dialog.message("The size variables (imageSizeX, imageSizeY)\n are not available!");
							}
						}
						/************************************** Transfer Bytes! **************************************************************/

						else if (transferDataType == 2) {

							byte[] imageData;
							int imagesSizeY;

							try {
								bolRawExists = (REXPLogical) c.eval("try(is.raw(" + name + "))");
								bolXExists = (REXPLogical) c.eval("try(exists(\"imageSizeX\"))");
								bolYExists = (REXPLogical) c.eval("try(exists(\"imageSizeY\"))");

							} catch (RserveException e1) {
								e1.printStackTrace();
							}
							boolean[] bolRaw = bolRawExists.isTrue();
							boolean[] bolX = bolXExists.isTrue();
							boolean[] bolY = bolYExists.isTrue();
							if (bolRaw[0] == true) {
								if (bolX[0] == true && bolY[0] == true) {
									try {
										imagesSizeY = (int) c.eval("try(imageSizeY)").asDouble();

										int imagesSizeX = (int) c.eval("try(imageSizeX)").asDouble();
										imageData = c.eval("try(" + name + ")").asBytes();

										if (imageData != null) {
											if (type == 0) {
												ip = new ColorProcessor(imagesSizeX, imagesSizeY);

												for (int i = 0; i < imagesSizeY; i++) {

													for (int u = 0; u < imagesSizeX; u++) {
														int value = (imageData[i * imagesSizeX + u]);
														ip.putPixel(u, i, value & 0xff);

													}

												}
											} else if (type == 1) {
												ip = new ByteProcessor(imagesSizeX, imagesSizeY);
												for (int i = 0; i < imagesSizeY; i++) {

													for (int u = 0; u < imagesSizeX; u++) {
														byte value = (imageData[i * imagesSizeX + u]);

														ip.putPixel(u, i, value & 0xff);

													}

												}
											} else if (type == 2) {
												ip = new FloatProcessor(imagesSizeX, imagesSizeY);
												for (int i = 0; i < imagesSizeY; i++) {

													for (int u = 0; u < imagesSizeX; u++) {
														int value = (imageData[i * imagesSizeX + u]);
														ip.putPixel(u, i, value & 0xff);

													}

												}
												ip.resetMinAndMax();
											} else {
												ip = new ShortProcessor(imagesSizeX, imagesSizeY);
												for (int i = 0; i < imagesSizeY; i++) {

													for (int u = 0; u < imagesSizeX; u++) {
														int value = (imageData[i * imagesSizeX + u]);
														ip.putPixel(u, i, value & 0xff);

													}

												}
												ip.resetMinAndMax();
											}

											imp = new ImagePlus(name, ip);
											imp.show();

										}
									} catch (REXPMismatchException e) {

										System.out.println("An transfer error occured!\n" + "Please select the correct image type!\n" + "For a byte transfer a list has to be present!\n");
									} catch (RserveException e) {

										e.printStackTrace();
									}
									imageData = null;

								} else {
									System.out.println("The size variables (imageSizeX, imageSizeY)\n are not available!");
								}
							} else {

								System.out.println("Raw data (as.raw(yourData)) is required for the byte transfer!");
							}

						}

					} else {
						System.out.println("Specified image data is not numeric!");
					}
				} else {
					System.out.println("Specified image data not existent\n" + "in the R workspace!\n (The word image is also forbidden for\n a transfer to ImageJ!)");
				}
			} else {
				System.out.println("No image data name specified!");
			}
		} else {
			System.out.println("Rserve is not alive!");
		}
	}

	/*
	 * Method for cluster analysis and PCR with fixed imageData as name!
	 * 0=double,1=integer, 2=byte
	 */
	protected static void imagePlusToR(ImagePlus imp, String name, boolean matrix, int transferDataType) {

		double[] pDouble = null;
		int[] pInt = null;
		byte[] pByte = null;
		int y = 0;
		int x = 0;
		if (RServe.isAlive()) {
			RConnection c = RServe.getConnection();
			/* Get the active image ! */

			if (imp != null) {

				/* Get the image processor of the image ! */
				ImageProcessor ip = imp.getProcessor();

				int w = ip.getWidth();
				int h = ip.getHeight();

				try {
					c.eval("imageSizeY<-" + h);

					c.eval("imageSizeX<-" + w);
					c.eval("imageDataName<-'" + name + "'");
				} catch (RserveException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				/* Double transfer type */
				if (transferDataType == 0) {
					/* From Float images we transfer the scaled values! */
					if (ip instanceof FloatProcessor) {

						pDouble = new double[w * h];

						for (int z = 0; z < h * w; z++) {

							if (x > (w - 1)) {
								y++;
								x = 0;
							}
							int v = ip.getPixel(x, y);
							pDouble[z] = Float.intBitsToFloat(v);

							if (x < w) {
								x++;
							}
						}

					}
					/* Normal transfer of double values! */
					else {
						pDouble = new double[w * h];

						for (int z = 0; z < h * w; z++) {

							if (x > (w - 1)) {
								y++;
								x = 0;
							}
							pDouble[z] = (double) ip.getPixel(x, y);

							if (x < w) {
								x++;
							}
						}
					}
					/* We transfer the values to R! */

					try {
						c.assign("imageData", pDouble);
					} catch (REngineException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					pDouble = null;
				}
				/* Integer transfer type! */
				else if (transferDataType == 1) {
					pInt = new int[w * h];

					for (int z = 0; z < h * w; z++) {

						if (x > (w - 1)) {
							y++;
							x = 0;
						}
						pInt[z] = ip.getPixel(x, y);

						if (x < w) {
							x++;
						}
					}

					/* We transfer the values to R! */

					try {
						c.assign("imageData", pInt);
					} catch (REngineException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					pInt = null;
				}
				/* Byte transfer type! */
				else if (transferDataType == 2) {
					pByte = new byte[w * h];

					for (int z = 0; z < h * w; z++) {

						if (x > (w - 1)) {
							y++;
							x = 0;
						}
						pByte[z] = (byte) ip.getPixel(x, y);

						if (x < w) {
							x++;
						}
					}

					/* We transfer the values to R! */

					try {
						c.assign("imageData", pByte);
					} catch (REngineException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					pByte = null;
				}

			} else {

				System.out.println("No image available!");

			}

		} else {
			System.out.println("RServer connection failed - Server is not running !");
		}
	}

	/* Used in Cluster and Pca job */
	public static Combo getTransferTypeCombo() {
		return transferTypeCombo;
	}

	protected static void setTransferTypeCombo(Combo transferTypeCombo) {
		ImageMethods.transferTypeCombo = transferTypeCombo;
	}

	/**
	 * A method to transfer byte information directly to an image as one
	 * container. If no image is present a new one will be created. This method
	 * expects the variables imageSizeX, imageSizeY to be present in the R
	 * workspace.
	 * 
	 * @param dataName
	 *            the name of the R vector.
	 * @param type
	 *            the type of the image (1 = byte, 2 = float).
	 */
	public static void transferImageInPlace(String dataName, int type) {

		ImagePlus imp = WindowManager.getCurrentImage();
		ByteProcessor ip;
		FloatProcessor ipFloat;
		int imagesSizeY = 0;
		int imagesSizeX = 0;

		try {
			imagesSizeY = (int) RServe.getConnection().eval("try(imageSizeY)").asDouble();
			imagesSizeX = (int) RServe.getConnection().eval("try(imageSizeX)").asDouble();
		} catch (REXPMismatchException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (RserveException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (type == 1) {
			if (imp == null) {

				/* If no image is present we create one with random pixels! */
				ip = new ByteProcessor(imagesSizeX, imagesSizeY);
				ip.setColor(java.awt.Color.white);

				ip.fill();

				byte[] imageData = null;

				try {

					imageData = RServe.getConnection().eval("try(" + dataName + ")").asBytes();

				} catch (REXPMismatchException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (RserveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				ip = new ByteProcessor(imagesSizeX, imagesSizeY);
				for (int i = 0; i < imagesSizeY; i++) {

					for (int u = 0; u < imagesSizeX; u++) {
						byte value = (imageData[i * imagesSizeX + u]);
						ip.putPixel(u, i, value & 0xff);

					}

				}
				imp = new ImagePlus(dataName, ip);
				imp.show();
			} else {
				ip = (ByteProcessor) imp.getProcessor();
				byte[] imageData = null;
				if (ip.getWidth() == imagesSizeX && ip.getHeight() == imagesSizeY) {

					try {
						imageData = RServe.getConnection().eval("try(" + dataName + ")").asBytes();
					} catch (REXPMismatchException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (RserveException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					for (int i = 0; i < imagesSizeY; i++) {

						for (int u = 0; u < imagesSizeX; u++) {
							byte value = (imageData[i * imagesSizeX + u]);
							ip.putPixel(u, i, value & 0xff);

						}

					}

				}
				imp.updateAndDraw();
			}
		} else {
			if (imp == null) {

				/* If no image is present we create one with random pixels! */
				ipFloat = new FloatProcessor(imagesSizeX, imagesSizeY);
				ipFloat.setColor(java.awt.Color.white);

				ipFloat.fill();

				double[] imageDataFloat = null;

				try {

					imageDataFloat = RServe.getConnection().eval("try(" + dataName + ")").asDoubles();

				} catch (REXPMismatchException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (RserveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				ipFloat = new FloatProcessor(imagesSizeX, imagesSizeY);
				for (int i = 0; i < imagesSizeY; i++) {

					for (int u = 0; u < imagesSizeX; u++) {
						double value = (imageDataFloat[i * imagesSizeX + u]);
						ipFloat.putPixelValue(u, i, value);

					}

				}
				ipFloat.resetMinAndMax();
				imp = new ImagePlus(dataName, ipFloat);
				imp.show();
			} else {
				ipFloat = (FloatProcessor) imp.getProcessor();
				double[] imageDataFloat = null;
				if (ipFloat.getWidth() == imagesSizeX && ipFloat.getHeight() == imagesSizeY) {

					try {
						imageDataFloat = RServe.getConnection().eval("try(" + dataName + ")").asDoubles();
					} catch (REXPMismatchException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (RserveException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					for (int i = 0; i < imagesSizeY; i++) {

						for (int u = 0; u < imagesSizeX; u++) {
							double value = (imageDataFloat[i * imagesSizeX + u]);
							ipFloat.putPixelValue(u, i, value);

						}

					}
					ipFloat.resetMinAndMax();

				}
				imp.updateAndDraw();
			}

		}
	}

	private ArrayList<Double> getROIPixels(ImagePlus imp, Roi roi) {
		ImageProcessor ip = imp.getProcessor();
		ImageProcessor mask = roi != null ? roi.getMask() : null;
		Rectangle r = roi != null ? roi.getBounds() : new Rectangle(0, 0, ip.getWidth(), ip.getHeight());
		double sum = 0;
		int count = 0;
		ArrayList<Double> values = new ArrayList<Double>();
		for (int y = 0; y < r.height; y++) {
			for (int x = 0; x < r.width; x++) {
				if (mask == null || mask.getPixel(x, y) != 0) {
					count++;
					System.out.println(ip.getPixelValue(x + r.x, y + r.y));
					// ip.set(x + r.x, y + r.y, 0);
					values.add(new Double(ip.getPixelValue(x + r.x, y + r.y)));
				}
			}
		}
		return values;
	}

	public void dispose() {

		bildGif.dispose();

		deletePicGif.dispose();

		regelmaessigGif.dispose();

		deletePoints.dispose();

		// toIJGif.dispose();

		// rGif.dispose();

		super.dispose();
	}
}
