/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors: Marcel Austenfeld - initial API and implementation
 *******************************************************************************/

package com.eco.bio7.image;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
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
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.discrete.Field;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.image.r.IJTranserResultsTable;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
 * This class provides some static methods for the analysis and transfer of
 * images inside the Bio7 application.
 * 
 * 
 * @author Bio7
 * 
 */
/*
 * public class PointPanelImageMethodsView extends ViewPart { public
 * PointPanelImageMethodsView() { }
 * 
 * private Combo toimageJCombo;
 * 
 * private static Combo transferTypeCombo;
 * 
 * public static final String ID = "com.eco.bio7.image_methods";
 * 
 * private Composite top = null;
 * 
 * private Button button = null;
 * 
 * private Scale scale = null;
 * 
 * private Scale scale1 = null;
 * 
 * private Button button1 = null;
 * 
 * private Button button2 = null;
 * 
 * private static BufferedImage image = null;
 * 
 * private Button button3 = null;
 * 
 * private Button button5 = null;
 * 
 * private Scale scale2 = null;
 * 
 * private Button button6 = null;
 * 
 * private Button button8 = null;
 * 
 * private Button button9 = null;
 * 
 * private Label label = null;
 * 
 * private Label label1 = null;
 * 
 * private Label label2 = null;
 * 
 * private Label label3 = null;
 * 
 * private Label label4 = null;
 * 
 * private static Spinner spinner = null;
 * 
 * private static Spinner spinner1 = null;
 * 
 * private static int fieldx = 1000;// The area of analysis !
 * 
 * private static int fieldy = 1000;// The area of analysis !
 * 
 * private Label label51 = null;
 * 
 * private Label label52 = null;
 * 
 * private Spinner spinner2 = null;
 * 
 * private Label label53 = null;
 * 
 * private Button button4 = null;
 * 
 * private Label label54 = null;
 * 
 * private Spinner spinner3 = null;
 * 
 * private Label label55 = null;
 * 
 * private static PointPanelImageMethodsView im;
 * 
 * private static int pointScale = 1;
 * 
 * private static boolean Centroid = true;
 * 
 * private Button button7 = null;
 * 
 * protected boolean canTransfer = true;
 * 
 * protected boolean canTransferPic = true;
 * 
 * protected int transferImageType;
 * 
 * protected int transferIntegers;
 * 
 * protected boolean transferBackIntegers;
 * 
 * // private IPreferenceStore store;
 * 
 * private Button btnAreas;
 * 
 * private Scale scale_2;
 * 
 * private Image bildGif;
 * 
 * private Image deletePicGif;
 * 
 * private Image regelmaessigGif;
 * 
 * private Image deletePoints;
 * 
 * private Image toIJGif;
 * 
 * private Image rGif;
 * 
 * private ScrolledComposite scrolledComposite;
 * 
 * protected static boolean createMatrix;
 * 
 * private static ColorProcessor ipColor;
 * 
 * public void createPartControl(Composite parent) { // store =
 * Bio7Plugin.getDefault().getPreferenceStore(); im = this;
 * PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,
 * "com.eco.bio7.imagemethods"); scrolledComposite = new
 * ScrolledComposite(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
 * scrolledComposite.setExpandHorizontal(true);
 * scrolledComposite.setExpandVertical(false); GridData gridData14 = new
 * GridData(SWT.FILL, SWT.FILL, true, false, 1, 1); gridData14.widthHint = 86;
 * gridData14.horizontalIndent = 0; GridData gridData3 = new GridData();
 * gridData3.grabExcessVerticalSpace = false; gridData3.heightHint = 35;
 * gridData3.horizontalAlignment = GridData.FILL;
 * gridData3.grabExcessHorizontalSpace = true; gridData3.verticalAlignment =
 * SWT.FILL; GridData gridData21 = new GridData(); gridData21.heightHint = 35;
 * gridData21.horizontalAlignment = GridData.FILL; gridData21.horizontalIndent =
 * 0; gridData21.grabExcessHorizontalSpace = true;
 * gridData21.grabExcessVerticalSpace = false; gridData21.verticalAlignment =
 * SWT.FILL; GridLayout gridLayout1 = new GridLayout(); gridLayout1.marginHeight
 * = 1; gridLayout1.marginRight = 3; gridLayout1.marginLeft = 3;
 * gridLayout1.makeColumnsEqualWidth = true; gridLayout1.numColumns = 2;
 * gridLayout1.verticalSpacing = 0; gridLayout1.marginWidth = 0;
 * gridLayout1.horizontalSpacing = 2; top = new Composite(scrolledComposite,
 * SWT.NONE); top.setSize(300,500); top.setLayout(gridLayout1);
 * scrolledComposite.setContent(top); label = new Label(top, SWT.NONE);
 * label.setText("Alpha Image"); label.setLayoutData(new GridData(SWT.CENTER,
 * SWT.FILL, true, false, 1, 1)); label1 = new Label(top, SWT.NONE);
 * label1.setText("Alpha Quadgrid"); label1.setLayoutData(new
 * GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1)); scale = new Scale(top,
 * SWT.NONE); scale.setMaximum(255);
 * scale.setToolTipText("Alpha value of the image in the Points panel");
 * scale.setLayoutData(gridData14); scale.setSelection(255); scale1 = new
 * Scale(top, SWT.NONE); scale1.setMaximum(255); scale1.setSelection(255);
 * scale1.setToolTipText("Alpha value for the Quadgrid"); GridData gd_scale1 =
 * new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1); gd_scale1.widthHint =
 * 78; scale1.setLayoutData(gd_scale1);
 * 
 * scale1.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
 * public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
 * alphaQuadgrid(); }
 * 
 * }); button = new Button(top, SWT.NONE); bildGif =
 * Bio7Plugin.getImageDescriptor("/icons/views/addimageaction.png").createImage(
 * ); // bildGif = new Image(Display.getCurrent(), //
 * getClass().getResourceAsStream("/pics/bild.gif")); button.setImage(bildGif);
 * button.setToolTipText("Open an image in the Points panel");
 * button.setLayoutData(gridData21); button5 = new Button(top, SWT.NONE);
 * deletePicGif =
 * Bio7Plugin.getImageDescriptor("/icons/views/deleteimageaction.png").
 * createImage(); button5.setImage(deletePicGif);
 * button5.setLayoutData(gridData3);
 * button5.setToolTipText("Remove the current image of the Points panel");
 * GridData gridData1 = new GridData(SWT.FILL, SWT.FILL, true, false);
 * gridData1.heightHint = 35; gridData1.horizontalIndent = 0; button1 = new
 * Button(top, SWT.NONE); regelmaessigGif =
 * Bio7Plugin.getImageDescriptor("/icons/views/2dperspview.png").createImage();
 * button1.setImage(regelmaessigGif); button1.
 * setToolTipText("Set or unset the Quadgrid as a layer in the Points panel");
 * button1.setLayoutData(gridData1); button1.addSelectionListener(new
 * org.eclipse.swt.events.SelectionAdapter() { public void
 * widgetSelected(org.eclipse.swt.events.SelectionEvent e) { loadField(); //
 * widgetSelected() }
 * 
 * }); GridData gridData20 = new GridData(); gridData20.grabExcessVerticalSpace
 * = false; gridData20.heightHint = 35; gridData20.horizontalAlignment =
 * GridData.FILL; gridData20.grabExcessHorizontalSpace = true;
 * gridData20.verticalAlignment = SWT.FILL; button3 = new Button(top, SWT.NONE);
 * deletePoints =
 * Bio7Plugin.getImageDescriptor("/icons/views/deletepointsaction.png").
 * createImage(); button3.setImage(deletePoints);
 * button3.setLayoutData(gridData20);
 * button3.setToolTipText("Delete points in the Points panel");
 * button3.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
 * public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
 * deletePoints(); if (PointPanel.showVoronoi == true) {
 * PointPanel.cleanVoronoi();
 * 
 * } if (PointPanel.showDelauney == true) { PointPanel.cleanDelauney();
 * 
 * }
 * 
 * }
 * 
 * });
 * 
 * final Label resizeQuadsLabel = new Label(top, SWT.NONE);
 * resizeQuadsLabel.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true,
 * false, 1, 1)); resizeQuadsLabel.setText("Resize Quads"); label2 = new
 * Label(top, SWT.NONE); label2.setText("Resize Points Panel");
 * label2.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, 1, 1));
 * 
 * final Scale scale_1 = new Scale(top, SWT.NONE); scale_1.
 * setToolTipText("If Quads are enabled this slider resizes the quads from the Quads view"
 * ); scale_1.setMinimum(1); scale_1.setSelection(100);
 * scale_1.addSelectionListener(new SelectionAdapter() { public void
 * widgetSelected(final SelectionEvent e) { scale_1.addSelectionListener(new
 * SelectionAdapter() { public void widgetSelected(final SelectionEvent e) { if
 * (Quad2d.getQuad2dInstance() != null) { if (PointPanel.isQuad2d_visible()) {
 * Quad2d.getQuad2dInstance().quadResize(scale_1.getSelection());
 * PointPanel.doPaint(); } } } }); } }); GridData gd_scale_1 = new
 * GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1); gd_scale_1.widthHint = 84;
 * scale_1.setLayoutData(gd_scale_1); scale2 = new Scale(top, SWT.NONE);
 * scale2.setMinimum(1); scale2.setToolTipText("Scale down the Points panel");
 * scale2.setPageIncrement(10000); scale2.setIncrement(10000); GridData
 * gd_scale2 = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
 * gd_scale2.widthHint = 66; scale2.setLayoutData(gd_scale2);
 * scale2.setMaximum(10000); scale2.setSelection(10000);
 * scale2.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
 * public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
 * scaleImage();
 * 
 * }
 * 
 * }); GridData gridData10 = new GridData();
 * gridData10.grabExcessHorizontalSpace = true;
 * gridData10.grabExcessVerticalSpace = false; gridData10.horizontalAlignment =
 * GridData.CENTER; gridData10.verticalAlignment = SWT.FILL; label51 = new
 * Label(top, SWT.NONE); label51.setText("Fieldsize X");
 * label51.setLayoutData(gridData10); GridData gridData9 = new GridData();
 * gridData9.grabExcessHorizontalSpace = true; gridData9.grabExcessVerticalSpace
 * = false; gridData9.horizontalAlignment = GridData.CENTER;
 * gridData9.verticalAlignment = SWT.FILL; label52 = new Label(top, SWT.NONE);
 * label52.setText("Fieldsize Y"); label52.setLayoutData(gridData9); GridData
 * gridData15 = new GridData(); gridData15.grabExcessHorizontalSpace = true;
 * gridData15.grabExcessVerticalSpace = false; gridData15.horizontalAlignment =
 * GridData.FILL; gridData15.verticalAlignment = SWT.FILL; spinner = new
 * Spinner(top, SWT.BORDER); spinner.setMaximum(1000000);
 * spinner.setLayoutData(gridData15);
 * spinner.setToolTipText("Select field size in x direction");
 * spinner.setSelection(fieldx); spinner.setMinimum(1);
 * spinner.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
 * public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
 * resizePointpanel2(); }
 * 
 * }); GridData gridData2 = new GridData(); gridData2.grabExcessHorizontalSpace
 * = true; gridData2.grabExcessVerticalSpace = false;
 * gridData2.horizontalAlignment = GridData.FILL; gridData2.verticalAlignment =
 * SWT.FILL; spinner1 = new Spinner(top, SWT.BORDER);
 * spinner1.setMaximum(1000000); spinner1.setLayoutData(gridData2);
 * spinner1.setToolTipText("Select the field size in y direction");
 * spinner1.setSelection(fieldy); spinner1.setMinimum(1);
 * 
 * spinner1.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
 * 
 * public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
 * resizePointpanel(); }
 * 
 * }); GridData gridData121 = new GridData();
 * gridData121.grabExcessHorizontalSpace = true;
 * gridData121.grabExcessVerticalSpace = false; gridData121.horizontalAlignment
 * = GridData.CENTER; gridData121.verticalAlignment = SWT.FILL; label53 = new
 * Label(top, SWT.NONE); label53.setText("Point Size");
 * label53.setLayoutData(gridData121);
 * 
 * GridData gridData23 = new GridData(); gridData23.grabExcessHorizontalSpace =
 * true; gridData23.grabExcessVerticalSpace = false;
 * gridData23.horizontalAlignment = GridData.CENTER;
 * gridData23.verticalAlignment = SWT.FILL; label55 = new Label(top, SWT.NONE);
 * label55.setText("Alpha"); label55.setLayoutData(gridData23); GridData
 * gridData111 = new GridData(); gridData111.grabExcessHorizontalSpace = true;
 * gridData111.grabExcessVerticalSpace = false; gridData111.horizontalAlignment
 * = GridData.FILL; gridData111.verticalAlignment = SWT.FILL; spinner2 = new
 * Spinner(top, SWT.NONE);
 * spinner2.setToolTipText("Adjust the point size in the Points panel");
 * 
 * spinner2.setMinimum(1); spinner2.setLayoutData(gridData111);
 * spinner2.setSelection(5); spinner2.setMaximum(100000);
 * spinner2.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
 * public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
 * adjustDiamterPoints(); }
 * 
 * }); GridData gridData11 = new GridData();
 * gridData11.grabExcessHorizontalSpace = true;
 * gridData11.grabExcessVerticalSpace = false; gridData11.horizontalAlignment =
 * GridData.FILL; gridData11.verticalAlignment = SWT.FILL; spinner3 = new
 * Spinner(top, SWT.NONE); spinner3.
 * setToolTipText("Adjust the alpha value of the points in the Points panel");
 * spinner3.setMaximum(255); spinner3.setSelection(255); spinner3.setMinimum(0);
 * spinner3.setLayoutData(gridData11); spinner3.addSelectionListener(new
 * org.eclipse.swt.events.SelectionAdapter() { public void
 * widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
 * setPointsPanelAlpha(); }
 * 
 * });
 * 
 * CLabel lblNewLabel_3 = new CLabel(top, SWT.NONE);
 * lblNewLabel_3.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false,
 * 2, 1)); lblNewLabel_3.setText("Voronoi / Delauney");
 * 
 * final Button voronoiButton = new Button(top, SWT.NONE); voronoiButton.
 * setToolTipText("Creates a Voronoi Diagramm from the Points in the Points Panel"
 * ); voronoiButton.addSelectionListener(new SelectionAdapter() { public void
 * widgetSelected(final SelectionEvent e) { PointPanel.createVoronoi();
 * PointPanel.showVoronoi = !PointPanel.showVoronoi; if (PointPanel.showVoronoi
 * == false) { PointPanel.cleanVoronoi(); btnAreas.setSelection(false);
 * PointPanel.showAreas = false; } PointPanel.doPaint();
 * 
 * } }); final GridData gd_voronoiButton = new GridData(SWT.FILL, SWT.FILL,
 * true, false); gd_voronoiButton.heightHint = 35;
 * voronoiButton.setLayoutData(gd_voronoiButton);
 * voronoiButton.setText("Voronoi");
 * 
 * final Button delauneyButton = new Button(top, SWT.NONE); delauneyButton.
 * setToolTipText("Creates a Delauney Triangulation from the Points in the Points Panel"
 * ); delauneyButton.addSelectionListener(new SelectionAdapter() { public void
 * widgetSelected(final SelectionEvent e) { PointPanel.createDelauney();
 * PointPanel.showDelauney = !PointPanel.showDelauney; if
 * (PointPanel.showDelauney == false) { PointPanel.cleanDelauney(); }
 * PointPanel.doPaint();
 * 
 * } }); final GridData gd_delauneyButton = new GridData(SWT.FILL, SWT.FILL,
 * true, false); gd_delauneyButton.heightHint = 35;
 * delauneyButton.setLayoutData(gd_delauneyButton);
 * delauneyButton.setText("Delauney");
 * 
 * final Button dynamicButton = new Button(top, SWT.CHECK); GridData
 * gd_dynamicButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
 * gd_dynamicButton.heightHint = 30;
 * dynamicButton.setLayoutData(gd_dynamicButton);
 * dynamicButton.setToolTipText("Dynamic Voronoi visualization");
 * dynamicButton.setSelection(true); dynamicButton.addSelectionListener(new
 * SelectionAdapter() { public void widgetSelected(final SelectionEvent e) {
 * PointPanel p = PointPanel.getPointPanel(); if (p != null) { p.dynamicVoronoi
 * = !p.dynamicVoronoi; PointPanel.doPaint(); } } });
 * dynamicButton.setText("Dynamic Voro.");
 * 
 * final Button drawAreasButton = new Button(top, SWT.CHECK); GridData
 * gd_drawAreasButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
 * gd_drawAreasButton.heightHint = 30;
 * drawAreasButton.setLayoutData(gd_drawAreasButton);
 * drawAreasButton.setToolTipText("Clip Voronoi Areas");
 * drawAreasButton.addSelectionListener(new SelectionAdapter() { public void
 * widgetSelected(final SelectionEvent e) {
 * 
 * if (drawAreasButton.getSelection()) {
 * DelaunayAndVoronoiApp.setIntersection(true); PointPanel.cleanVoronoi();
 * PointPanel.doPaint();
 * 
 * } else { DelaunayAndVoronoiApp.setIntersection(false);
 * PointPanel.cleanVoronoi();
 * 
 * } PointPanel.createVoronoi(); PointPanel.doPaint(); } });
 * drawAreasButton.setText("Clip Areas");
 * 
 * Button btnDynamic = new Button(top, SWT.CHECK); GridData gd_btnDynamic = new
 * GridData(SWT.FILL, SWT.FILL, true, false, 1, 1); gd_btnDynamic.heightHint =
 * 30; btnDynamic.setLayoutData(gd_btnDynamic);
 * btnDynamic.setToolTipText("Dynamic Delauney visualization");
 * btnDynamic.addSelectionListener(new SelectionAdapter() {
 * 
 * public void widgetSelected(SelectionEvent e) { PointPanel p =
 * PointPanel.getPointPanel(); if (p != null) { p.dynamicDelauney =
 * !p.dynamicDelauney; PointPanel.doPaint(); } } });
 * btnDynamic.setText("Dynamic Del.");
 * 
 * btnAreas = new Button(top, SWT.CHECK); GridData gd_btnAreas = new
 * GridData(SWT.FILL, SWT.FILL, true, false, 1, 1); gd_btnAreas.heightHint = 30;
 * btnAreas.setLayoutData(gd_btnAreas);
 * btnAreas.setToolTipText("Draw Voronoi Areas");
 * btnAreas.addSelectionListener(new SelectionAdapter() {
 * 
 * @Override public void widgetSelected(SelectionEvent e) { if
 * (btnAreas.getSelection()) { PointPanel.showAreas = true; } else {
 * PointPanel.showAreas = false; } PointPanel.doPaint();
 * 
 * } }); btnAreas.setText("Draw Areas");
 * 
 * Label lblAreas = new Label(top, SWT.NONE); lblAreas.setAlignment(SWT.CENTER);
 * lblAreas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
 * lblAreas.setText("Draw Area Size");
 * 
 * scale_2 = new Scale(top, SWT.NONE); scale_2.setPageIncrement(1);
 * scale_2.addSelectionListener(new SelectionAdapter() {
 * 
 * public void widgetSelected(SelectionEvent e) {
 * 
 * PointPanel.setDrawAreaScaled((1.0 - ((10.0 - (double)
 * scale_2.getSelection())) / 10)); PointPanel.doPaint(); } });
 * scale_2.setMaximum(10); scale_2.setMinimum(1); scale_2.setSelection(10);
 * GridData gd_scale_2 = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
 * gd_scale_2.widthHint = 71; scale_2.setLayoutData(gd_scale_2);
 * 
 * CLabel lblNewLabel_4 = new CLabel(top, SWT.NONE);
 * lblNewLabel_4.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false,
 * 2, 1)); lblNewLabel_4.setText("ImageJ And Points Panel"); GridData gridData22
 * = new GridData(); gridData22.grabExcessVerticalSpace = false;
 * gridData22.heightHint = 35; gridData22.horizontalAlignment = GridData.FILL;
 * gridData22.grabExcessHorizontalSpace = true; gridData22.verticalAlignment =
 * SWT.FILL; button6 = new Button(top, SWT.NONE); //
 * button6.setFont(SWTResourceManager.getFont("Courier New", 9, // SWT.BOLD));
 * button6.setText("Pic->     "); toIJGif =
 * Bio7Plugin.getImageDescriptor("/icons/views/imagejview.png").createImage();
 * button6.setImage(toIJGif); button6.setLayoutData(gridData22); button6.
 * setToolTipText("Send an image from the ImageJ-Canvas to the Points panel");
 * button6.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
 * public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
 * plusToBuffered();
 * 
 * }
 * 
 * }); GridData gridData12 = new GridData(); gridData12.grabExcessVerticalSpace
 * = false; gridData12.heightHint = 35; gridData12.horizontalAlignment =
 * GridData.FILL; gridData12.grabExcessHorizontalSpace = true;
 * gridData12.verticalAlignment = SWT.FILL; // button6.setFont(new
 * Font(Display.getDefault(), "Tahoma", 8, // SWT.BOLD));
 * 
 * button9 = new Button(top, SWT.NONE); //
 * button9.setFont(SWTResourceManager.getFont("Courier New", 9, // SWT.BOLD));
 * button9.setText("Points    "); button9.setImage(toIJGif); //
 * button9.setFont(new Font(Display.getDefault(), "Tahoma", 8, // SWT.BOLD));
 * button9.setLayoutData(gridData12); button9.
 * setToolTipText("Transfer Particle values from ImageJ to the Points panel");
 * button9.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
 * public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
 * 
 * IJTranserResultsTable.addParticleValues(); if (PointPanel.showVoronoi ==
 * true) { PointPanel.createVoronoi(); } if (PointPanel.showDelauney == true) {
 * PointPanel.createDelauney(); } PointPanel.doPaint(); } }); GridData
 * gridData31 = new GridData(SWT.FILL, SWT.FILL, true, false);
 * gridData31.heightHint = 35; button8 = new Button(top, SWT.NONE); //
 * button8.setFont(SWTResourceManager.getFont("Courier New", 9, // SWT.BOLD));
 * button8.setText("Pic<-     ");
 * 
 * button8.
 * setToolTipText("Send an image from the Points panel to the ImageJ-Canvas");
 * button8.setImage(toIJGif); button8.setLayoutData(gridData31);
 * button8.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
 * public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
 * sendImageTo(); }
 * 
 * });
 * 
 * GridData gridData13 = new GridData(SWT.FILL, SWT.FILL, true, false);
 * gridData13.heightHint = 35; // button8.setFont(new Font(Display.getDefault(),
 * "Tahoma", 8, // SWT.BOLD)); button7 = new Button(top, SWT.NONE); //
 * button7.setFont(SWTResourceManager.getFont("Courier New", 9, // SWT.BOLD));
 * button7.setText("Pixel      "); button7.setLayoutData(gridData13);
 * 
 * // button7.setFont(new Font(Display.getDefault(), "Tahoma", 8, // SWT.BOLD));
 * button7.setImage(toIJGif);
 * button7.setToolTipText("Transfer threshold pixels to the Quadgrid");
 * button7.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
 * public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
 * setAllPixels(); } });
 * 
 * Label label_1 = new Label(top, SWT.SEPARATOR | SWT.HORIZONTAL); GridData
 * gd_label_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
 * gd_label_1.heightHint = 35; label_1.setLayoutData(gd_label_1);
 * button5.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
 * public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
 * 
 * deletePicPointpanel(); }
 * 
 * }); scale.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter()
 * { public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
 * alphaPic(); }
 * 
 * });
 * 
 * button.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
 * public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
 * loadPic();
 * 
 * }
 * 
 * }); //initializeToolBar();
 * 
 * }
 * 
 * private void setPointsPanelAlpha() {
 * 
 * int sel = spinner3.getSelection();
 * 
 * PointPanel.setCompos3(sel);
 * 
 * }
 * 
 * private void alphaPic() { PointPanel.setCompos(scale.getSelection());
 * 
 * PointPanel Jp = PointPanelView.getJp(); Jp.repaint(); }
 * 
 * private void alphaQuadgrid() { PointPanel.setCompos2(scale1.getSelection());
 * 
 * PointPanel Jp = PointPanelView.getJp(); Jp.repaint(); }
 * 
 * private void adjustDiamterPoints() { PointPanel Jp = PointPanelView.getJp();
 * Jp.set_Diameter(spinner2.getSelection()); }
 * 
 * private void deletePicPointpanel() { image = null;
 * PointPanel.setBuffNull();// set Buffered Image to null! PointPanel Jp =
 * PointPanelView.getJp(); if (Jp != null) { Jp.repaint(); System.gc(); } }
 * 
 *//**
	 * Deletes all point values, alpha values, ellipse values and species values in
	 * the Points panel and repaints the Points panel.
	 */
/*
 * public static void deletePoints() { PointPanel.getVe().clear();
 * PointPanel.get_Points().clear(); PointPanel.get_Species().clear();
 * PointPanel.get_Alpha().clear(); PointPanel Jp = PointPanelView.getJp(); if
 * (Jp != null) { Jp.repaint(); } }
 * 
 * private void resizePointpanel() { PointPanel Jp = PointPanelView.getJp(); if
 * (Jp != null) { Jp.setPreferredSize(new Dimension((int)
 * (spinner.getSelection() * Jp.getTransformx()), (int) (spinner1.getSelection()
 * * Jp.getTransformy()))); PointPanelView.getScroll().setViewportView(Jp);
 * fieldx = spinner.getSelection(); fieldy = spinner1.getSelection(); }
 * 
 * }
 * 
 * private void resizePointpanel2() { PointPanel Jp = PointPanelView.getJp(); if
 * (Jp != null) { Jp.setPreferredSize(new Dimension((int)
 * (spinner.getSelection() * Jp.getTransformx()), (int) (spinner1.getSelection()
 * * Jp.getTransformy()))); PointPanelView.getScroll().setViewportView(Jp);
 * fieldx = spinner.getSelection(); fieldy = spinner1.getSelection(); }
 * 
 * }
 * 
 * private void loadField() {
 * 
 * PointPanel.setQuad2d_visible(!PointPanel.isQuad2d_visible()); if
 * (PointPanel.isQuad2d_visible() == true) {
 * spinner.setSelection(Field.getQuadSize() * Field.getWidth());
 * spinner1.setSelection(Field.getQuadSize() * Field.getHeight()); PointPanel Jp
 * = PointPanelView.getJp(); if (Jp != null) { Jp.setPreferredSize(new
 * Dimension((int) (spinner.getSelection() * Jp.getTransformx()), (int)
 * (spinner1.getSelection() * Jp.getTransformy())));
 * PointPanelView.getScroll().setViewportView(Jp); fieldx =
 * spinner.getSelection(); fieldy = spinner1.getSelection(); } } PointPanel Jp =
 * PointPanelView.getJp(); if (Jp != null) { Jp.repaint(); } }
 * 
 * private void scaleImage() { final PointPanel Jp = PointPanelView.getJp(); if
 * (Jp != null) { int select = scale2.getSelection();
 * 
 * double sc = select; double factor = 10000; double scaled = sc / 10000;
 * 
 * Jp.setSx(scaled); Jp.setSy(scaled);
 * 
 * Jp.setPreferredSize(new Dimension((int) (spinner.getSelection() * scaled),
 * (int) (spinner1.getSelection() * scaled))); Jp.repaint(); try {
 * SwingUtilities.invokeAndWait(new Runnable() { // !! public void run() {
 * 
 * PointPanelView.getScroll().setViewportView(Jp); } }); } catch
 * (InterruptedException e1) {
 * 
 * e1.printStackTrace(); } catch (InvocationTargetException e1) {
 * 
 * e1.printStackTrace(); } } }
 * 
 * private void sendImageTo() { PointPanel Jp = PointPanelView.getJp(); if (Jp
 * != null) { BufferedImage sprite = null; if (image != null) { sprite = new
 * BufferedImage((int) (fieldx * Jp.getTransformx()), (int) (fieldy *
 * Jp.getTransformy()), BufferedImage.TYPE_INT_RGB); } else { sprite = new
 * BufferedImage((int) (fieldx * Jp.getTransformx()), (int) (fieldy *
 * Jp.getTransformy()), BufferedImage.TYPE_INT_RGB); }
 * 
 * Graphics g2D = sprite.createGraphics();
 * 
 * Jp.paintComponent(g2D);
 * 
 * // java.awt.Image awtimage = toImage(sprite); ImagePlus imp = new
 * ImagePlus("Bio7", sprite); imp.show(); } }
 * 
 * private void plusToBuffered() {
 * 
 * // ImagePlus imp = WindowManager.getCurrentWindow().getImagePlus(); ImagePlus
 * imp = WindowManager.getCurrentImage(); if (imp != null) { java.awt.Image
 * imageawt = imp.getImage(); BufferedImage bufferdimage =
 * createBufferedImage(imageawt); image = bufferdimage; PointPanel Jp =
 * PointPanelView.getJp(); if (Jp != null) { Jp.setPreferredSize(new
 * Dimension((int) (bufferdimage.getWidth() * Jp.getTransformx()), (int)
 * (bufferdimage.getHeight() * Jp.getTransformy())));
 * PointPanelView.getScroll().setViewportView(Jp); Jp.setBuff(bufferdimage);
 * fieldx = bufferdimage.getWidth(); fieldy = bufferdimage.getHeight();
 * spinner.setSelection(bufferdimage.getWidth());
 * spinner1.setSelection(bufferdimage.getHeight()); } } else {
 * Bio7Dialog.message("No image availabe in the ImageJ-Canvas view!"); } }
 * 
 * private void loadPic() {
 * 
 * Shell s = new Shell(); FileDialog fd = new FileDialog(s, SWT.OPEN);
 * fd.setText("Open");
 * 
 * String[] filterExt = { "*.*", "*.jpg", "*.JPG", ".jpeg", ".png", ".gif" };
 * fd.setFilterExtensions(filterExt); String selected = fd.open(); if (selected
 * != null) { File file = new File(selected);
 * 
 * try { image = ImageIO.read(file); } catch (IOException e1) {
 * 
 * e1.printStackTrace(); }
 * 
 * PointPanel Jp = PointPanelView.getJp(); if (Jp != null) {
 * Jp.setPreferredSize(new Dimension((int) (image.getWidth() *
 * Jp.getTransformx()), (int) (image.getHeight() * Jp.getTransformy())));
 * PointPanelView.getScroll().setViewportView(Jp); Jp.setBuff(image);
 * 
 * Jp.setBuff(image); fieldx = image.getWidth(); fieldy = image.getHeight();
 * spinner.setSelection(image.getWidth());
 * spinner1.setSelection(image.getHeight()); }
 * 
 * } }
 * 
 * public void setFocus() { top.setFocus(); }
 * 
 * private BufferedImage createBufferedImage(java.awt.Image image) {
 * 
 * int w = image.getWidth(null); int h = image.getHeight(null); BufferedImage bi
 * = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB); Graphics2D g =
 * bi.createGraphics(); g.drawImage(image, 0, 0, null); g.dispose(); return bi;
 * }
 * 
 * private static java.awt.Image toImage(BufferedImage bufferedImage) { return
 * Toolkit.getDefaultToolkit().createImage(bufferedImage.getSource()); }
 * 
 * private void startImageJ() {
 * 
 * if (IJ.getInstance() == null) {
 * 
 * } else { IJ.getInstance().setVisible(true); }
 * 
 * }
 * 
 * private static void setAllPixels() {
 * 
 * ImagePlus imp = WindowManager.getCurrentImage(); if (imp != null) { Get the
 * image processor of the image! ImageProcessor ip = imp.getProcessor(); int w =
 * ip.getWidth(); int h = ip.getHeight();
 * 
 * We proof if the image is an 8-bit image! if (ip instanceof ByteProcessor) {
 * for (int v = 0; v < h; v++) { for (int u = 0; u < w; u++) {
 * 
 * int i = ip.getPixel(u, v);
 * 
 * Now we get the threshold interval! double min = ip.getMinThreshold(); double
 * max = ip.getMaxThreshold();
 * 
 * All values which match the threshold are assigned! if (i >= min && i <= max)
 * {
 * 
 * pixelToQuad(u, v);
 * 
 * }
 * 
 * } } } else { Bio7Dialog.message("Requires an 8-bit image !"); }
 * 
 * } else { Bio7Dialog.message("No image opened !");
 * 
 * } PointPanel.doPaint(); }
 * 
 * private static void pixelToQuad(double cmx, double cmy) {
 * 
 * if (PointPanel.isQuad2d_visible()) { if (cmx < (Field.getWidth() *
 * Field.getQuadSize()) && cmy < (Field.getHeight() * Field.getQuadSize())) {
 * try { Quad2d.getQuad2dInstance().setquads(cmx, cmy); } catch
 * (RuntimeException e) {
 * 
 * e.printStackTrace(); } } }
 * 
 * }
 * 
 * private static PointPanelImageMethodsView getImageMethods() { return im; }
 * 
 *//**
	 * Transfers the current image from the Points panel to the ImageJ panel.
	 */
/*
 * public static void toImageJ() {
 * 
 * PointPanel Jp = PointPanelView.getJp(); BufferedImage sprite = null;
 * 
 * sprite = new BufferedImage((int) (fieldx * Jp.getTransformx()), (int) (fieldy
 * * Jp.getTransformy()), BufferedImage.TYPE_INT_RGB);
 * 
 * Graphics g2D = sprite.createGraphics();
 * 
 * Jp.paintComponent(g2D);
 * 
 * java.awt.Image awtimage = toImage(sprite); ImagePlus imp = new
 * ImagePlus("Bio7", awtimage); imp.show();
 * 
 * }
 * 
 *//**
	 * Returns the current image in the Points panel.
	 * 
	 * @return an ImagePlus object.
	 */
/*
 * public static ImagePlus getPanelImage() {
 * 
 * PointPanel Jp = PointPanelView.getJp(); BufferedImage sprite = null;
 * 
 * sprite = new BufferedImage((int) (fieldx * Jp.getTransformx()), (int) (fieldy
 * * Jp.getTransformy()), BufferedImage.TYPE_INT_RGB);
 * 
 * Graphics g2D = sprite.createGraphics();
 * 
 * Jp.paintComponent(g2D);
 * 
 * java.awt.Image awtimage = toImage(sprite); ImagePlus imp = new
 * ImagePlus("Bio7", awtimage);
 * 
 * return imp; }
 * 
 * The size of the Area of Analysis!
 *//**
	 * Returns the x-size of the Area of Analysis.
	 * 
	 * @return the width of the area of analysis.
	 */
/*
 * public static int getFieldX() { return fieldx; }
 * 
 * The size of the Area of Analysis!
 *//**
	 * Returns the y-size of the Area of Analysis.
	 * 
	 * @return the height of the area of analysis.
	 */
/*
 * public static int getFieldY() { return fieldy; }
 * 
 *//**
	 * Sets the field size in the Points panel(Area of Analysis).
	 * 
	 * @param x the x-size.
	 * @param y the y-size.
	 */
/*
 * public static void setFieldSize(final int x, final int y) { final PointPanel
 * Jp = PointPanelView.getJp(); Jp.setPreferredSize(new Dimension((int) (x *
 * Jp.getTransformx()), (int) (y * Jp.getTransformy())));
 * SwingUtilities.invokeLater(new Runnable() { // !! public void run() {
 * PointPanelView.getScroll().setViewportView(Jp);
 * 
 * } }); fieldx = x; fieldy = y;
 * 
 * Display display = PlatformUI.getWorkbench().getDisplay();
 * display.syncExec(new Runnable() {
 * 
 * public void run() { if (spinner != null && spinner1 != null &&
 * !spinner.isDisposed() && !spinner1.isDisposed()) { spinner.setSelection(x);
 * spinner1.setSelection(y); } } });
 * 
 * }
 * 
 *//**
	 * Returns the scale factor for the coordinates of the points from the particle
	 * analysis.
	 * 
	 * @return an integer value of the scale factor.
	 */
/*
 * public static int getPointScale() { return pointScale; }
 * 
 *//**
	 * Sets the scale factor for the coordinates of the points from the particle
	 * analysis.
	 * 
	 * @param pointScale an integer value of the scale factor.
	 */
/*
 * public static void setPointScale(int pointScale) {
 * PointPanelImageMethodsView.pointScale = pointScale; }
 * 
 *//**
	 * Returns if the centroid method is selected for the points.
	 * 
	 * @return a boolean value.
	 */
/*
 * public static boolean isCentroid() { return Centroid; }
 * 
 *//**
	 * Sets the method for adjusting the points in the Points panel (default =
	 * centroid for the particle analysis).
	 * 
	 * @param centroid a boolean value.
	 *//*
		 * public static void setCentroid(boolean centroid) { Centroid = centroid; }
		 * 
		 * private void initializeToolBar() { IToolBarManager toolBarManager =
		 * getViewSite().getActionBars().getToolBarManager(); }
		 * 
		 * public static BufferedImage getImage() { return image; }
		 * 
		 * Used in Cluster and Pca job public static Combo getTransferTypeCombo() {
		 * return transferTypeCombo; }
		 * 
		 * protected static void setTransferTypeCombo(Combo transferTypeCombo) {
		 * PointPanelImageMethodsView.transferTypeCombo = transferTypeCombo; }
		 * 
		 * public void dispose() {
		 * 
		 * bildGif.dispose();
		 * 
		 * deletePicGif.dispose();
		 * 
		 * regelmaessigGif.dispose();
		 * 
		 * deletePoints.dispose();
		 * 
		 * // toIJGif.dispose();
		 * 
		 * // rGif.dispose();
		 * 
		 * super.dispose(); } }
		 */