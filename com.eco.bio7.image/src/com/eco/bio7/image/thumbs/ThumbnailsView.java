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

package com.eco.bio7.image.thumbs;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.plugin.AVI_Reader;
import ij.process.ImageConverter;
import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.prefs.Preferences;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.ActionEvent;
import org.eclipse.draw2d.ActionListener;
import org.eclipse.draw2d.Button;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.LayeredPane;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.ScalableLayeredPane;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.parts.ScrollableThumbnail;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

public class ThumbnailsView extends ViewPart {

	private double zoomLevel[] = { 0.25, 0.5, 1.0, 1.5, 2.0, 3.0, 4.0 };

	private ScalableLayeredPane scalableLayer;

	private ThumbnailAction menuAction = new ThumbnailAction();

	private FiveInRowAction five = new FiveInRowAction();

	private TenInRowAction ten = new TenInRowAction();

	private FifteenInRowAction fifteen = new FifteenInRowAction();

	private SmallThumbAction small = new SmallThumbAction();

	private MediumThumbAction medium = new MediumThumbAction();

	private LargeThumbAction large = new LargeThumbAction();

	private EnablePreviewAction preview = new EnablePreviewAction();

	private RecursiveAction recursiveAction = new RecursiveAction();

	private String size;

	public ScrollableThumbnail thumbnail;

	private LayeredPane layeredpane;

	private Composite composite;

	private Composite parent;

	private Display display;

	private static ThumbnailsView thumb;

	private int x = 0;

	private int y = 0;

	private int inarow = 15;

	private int thumbsize = 150;

	private int newWidth;

	private int newHeight;

	private Button button;

	private Rectangle constraint;

	private org.eclipse.swt.graphics.Rectangle imageRect;

	private int[] scaled;

	private GC gc;

	public static final String ID = "com.eco.bio7.thumbnails";

	private boolean canceled = false;

	private int width;

	private int height;

	private static boolean thumbSizeSelectable = true;

	private Image stackImageSmall = new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/stack50border.jpg"));
	private Image stackImageMedium = new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/stack100border.jpg"));
	private Image stackImageBig = new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/stack200border.jpg"));
	private Image stackImageSmallVert = new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/stack50bordervert.jpg"));
	private Image stackImageMediumVert = new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/stack100bordervert.jpg"));
	private Image stackImageBigVert = new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/stack200bordervert.jpg"));
	private Image stackImageSmallFlip = new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/stack50borderflip.jpg"));
	private Image stackImageMediumFlip = new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/stack100borderflip.jpg"));
	private Image stackImageBigFlip = new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/stack200borderflip.jpg"));
	private Image stackImageSmallVertFlip = new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/stack50bordervertflip.jpg"));
	private Image stackImageMediumVertFlip = new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/stack100bordervertflip.jpg"));
	private Image stackImageBigVertFlip = new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/stack200bordervertflip.jpg"));

	private boolean showErrorMessage;

	private LightweightSystem lws;

	public SashForm sashForm;
	
	ArrayList listImages=new ArrayList();

	public static boolean isThumbSizeSelectable() {
		return thumbSizeSelectable;
	}

	public static void setThumbSizeSelectable(boolean thumbSizeSelectable) {
		ThumbnailsView.thumbSizeSelectable = thumbSizeSelectable;
	}

	public ThumbnailsView() {

		thumb = this;
		size = "small";

		load_preferences();

	}

	private void load_preferences() {
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		String prefer = prefs.get("SIZE", "");
		String prefer2 = prefs.get("INROW", "");

		if (prefer.equals("small")) {
			small.setChecked(true);
			medium.setChecked(false);
			large.setChecked(false);
			this.size = "small";
		} else if (prefer.equals("medium")) {
			small.setChecked(false);
			medium.setChecked(true);
			large.setChecked(false);
			this.size = "medium";
		} else if (prefer.equals("large")) {
			small.setChecked(false);
			medium.setChecked(false);
			large.setChecked(true);
			this.size = "large";
		}

		if (prefer2.equals("five")) {
			five.setChecked(true);
			this.inarow = 5;
		} else if (prefer2.equals("ten")) {
			ten.setChecked(true);
			this.inarow = 10;
		} else if (prefer2.equals("fifteen")) {
			fifteen.setChecked(true);
			this.inarow = 15;
		}
	}

	public void createPartControl(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.thumbnails");

		initializeToolBar();
		
		DeviceData data = new DeviceData();

	    data.tracking = true;

	    Display.getCurrent().setData(data);

	    /*Sleak sleak = new Sleak();

	    sleak.open();*/

		this.parent = parent;

		composite = new Composite(parent, SWT.BORDER);

		composite.setLayout(new FillLayout());

		sashForm = new SashForm(composite, SWT.HORIZONTAL);

		FigureCanvas canvas = new FigureCanvas(sashForm);

		scalableLayer = new ScalableLayeredPane();

		layeredpane = new LayeredPane();

		layeredpane.setLayoutManager(new XYLayout());

		scalableLayer.add(layeredpane);

		canvas.setContents(scalableLayer);

		Canvas thumbCanvas = new Canvas(sashForm, SWT.BORDER);

		lws = new LightweightSystem(thumbCanvas);

		thumbnail = new ScrollableThumbnail();

		thumbnail.setViewport(canvas.getViewport());

		thumbnail.setSource(layeredpane);

		lws.setContents(thumbnail);

	}

	public void setImages(final File files[], IProgressMonitor monitor) {
		x = 0;
		y = 0;

		display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {

				layeredpane.removeAll();

			}
		});
		
		for (int i = 0; i < listImages.size(); i++) {
			Image im=(Image) listImages.get(i);
			if(im!=null){
			im.dispose();
			}
		}
		listImages.clear();

		for (int i = 0; i < files.length; i++) {

			if (monitor.isCanceled()) {
				try {
					throw new InterruptedException();
				} catch (InterruptedException e) {
					canceled = true;

				}
			}
			if (canceled == false) {
				monitor.worked(1);
				monitor.setTaskName("opened: " + i + " from: " + files.length);
				final int u = i;

				if (i > 0 && (i % inarow) == 0) {
					if (size.equals("small")) {
						y = y + (thumbsize + 20);
						x = 0;
					} else if (size.equals("medium")) {
						y = y + (thumbsize + 25);
						x = 0;
					}

					else {
						y = y + (thumbsize + 50);
						x = 0;

					}

				}
				String name = files[i].getName();
				String trimmedName = name.trim();
				int dot = trimmedName.lastIndexOf(".");
				String fileExtension = trimmedName.substring(dot);
                
				Image im = null;

				/* If it is not a.zip etc.! */
				if (!fileExtension.equals(".zip") && !fileExtension.equals(".tif") && !fileExtension.equals(".TIF") && !fileExtension.equals(".TIFF") && !fileExtension.equals(".avi")
						&& !fileExtension.equals(".pnm") && !fileExtension.equals(".dcm") && !fileExtension.equals(".txt") && !fileExtension.equals(".roi")) {
					try {
						im = createThumbnailFromFile(files[i], fileExtension, null, false);
						listImages.add(im);
					} catch (RuntimeException e) {
                       
						imageError();
					}
					if (im != null) {
						button = new Button(im);

					}
				}

				else if (fileExtension.equals(".avi")) {
					boolean error = false;
					ImageData imageData = null;
					AVI_Reader read = new AVI_Reader();
					ImageStack stack = null;
					try {
						stack = read.makeStack(files[i].getAbsolutePath(), 1, 1, true, false, false);
					} catch (Exception e1) {
						imageError();
						read = null;
						stack = null;

					}
					if (stack != null) {
						try {
							ImagePlus plus = new ImagePlus("avi", stack);

							imageData = convertToSWT(plus.getBufferedImage());
							try {
								im = createThumbnailFromFile(files[i], fileExtension, imageData, true);
								listImages.add(im);
							} catch (RuntimeException e) {

								// e.printStackTrace();
							}

							if (im != null) {
								button = new Button(im);
								listImages.add(im);
							}
						} catch (Exception e) {
							imageError();
							// e.printStackTrace();
						}

					}
				}

				else if (fileExtension.equals(".zip") || fileExtension.equals(".dcm") || fileExtension.equals(".pnm") || fileExtension.equals(".tif") || fileExtension.equals(".TIF")
						|| fileExtension.equals(".TIFF")) {
					ImageData imageData = null;
					Opener.setErrorMessage(false);
					ImagePlus plus=null;
					if (fileExtension.equals(".tif") || fileExtension.equals(".TIF") || fileExtension.equals(".TIFF")) {
					 plus = IJ.openImage(files[i].getAbsolutePath(),1);
					}
					else{
						plus = IJ.openImage(files[i].getAbsolutePath());
					}
					Opener.setErrorMessage(true);
					if (plus != null) {

						try {
							if (plus.getStackSize() > 1) {
								/* Convert for SWT else error occurs! */
								new ImageConverter(plus).convertToRGB();
								imageData = convertToSWT(plus.getBufferedImage());
								im = createThumbnailFromFile(files[i], fileExtension, imageData, true);
								listImages.add(im);
							} else {

								if (fileExtension.equals(".tif") || fileExtension.equals(".TIF") || fileExtension.equals(".TIFF")|| fileExtension.equals(".zip")) {
									// IJ.run(plus, "Scale...",
									// "x=0.5 y=0.5 width=100 height=100 interpolation=Bilinear");
									imageData = convertToSWT(plus.getBufferedImage());
									im = createThumbnailFromFile(files[i], fileExtension, imageData, false);
									listImages.add(im);

								} else {

									// new ImageConverter(plus).convertToRGB();
									imageData = convertToSWT(plus.getBufferedImage());
									im = createThumbnailFromFile(files[i], fileExtension, imageData, false);
									listImages.add(im);
								}
							}

						} catch (RuntimeException e) {

							imageError();

						}
					}

					if (im != null) {
						button = new Button(im);

					} else {

						imageError();

					}
					plus = null;
					imageData = null;
					//im = null;

				}

				else if (fileExtension.equals(".txt")) {

					setThumbSize();

					if (thumbsize == 50) {
						Image imTxt=new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/txt50.jpg"));
						button = new Button(imTxt);
						listImages.add(imTxt);
					} else if (thumbsize == 100) {
						Image imTxt=new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/txt100.jpg"));
						button = new Button(imTxt);
						listImages.add(imTxt);
						
					} else {
						Image imTxt=new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/txt200.jpg"));
						button = new Button(imTxt);
						listImages.add(imTxt);
						

					}

				} else if (fileExtension.equals(".roi")) {

					setThumbSize();

					if (thumbsize == 50) {
						Image imRoi=new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/roi50.jpg"));
						button = new Button(imRoi);
						listImages.add(imRoi);
					} else if (thumbsize == 100) {
						Image imRoi=new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/roi100.jpg"));
						button = new Button(imRoi);
						listImages.add(imRoi);
						
					} else {
						Image imRoi=new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/roi200.jpg"));
						button = new Button(imRoi);
						listImages.add(imRoi);
						

					}

				}

				
				info(files, i);
				button.addActionListener(new ActionListener() {

					public void actionPerformed(ActionEvent arg0) {
						if (com.eco.bio7.image.CanvasView.getCanvas_view() != null) {
							Job job = new Job("Open...") {
								@Override
								protected IStatus run(IProgressMonitor monitor) {
									monitor.beginTask("Opening...", IProgressMonitor.UNKNOWN);

									Opener o = new Opener();
									o.open(files[u].toString());

									monitor.done();
									return Status.OK_STATUS;
								}

							};

							//job.setUser(true);
							job.schedule();
						} else {
							MessageDialog.openWarning(new Shell(), "ImageJ", "You probably closed all ImageJ-Views");

						}

					}
				});

				button.addMouseListener(new MouseListener() {

					@Override
					public void mouseDoubleClicked(MouseEvent arg0) {
						// TODO Auto-generated method stub

					}

					@Override
					public void mousePressed(MouseEvent evt) {
						if (evt.button == 3) {
							try {
								Program.launch(files[u].getParent());
							} catch (RuntimeException e) {

								e.printStackTrace();
							}
						}
					}

					@Override
					public void mouseReleased(MouseEvent arg0) {
						// TODO Auto-generated method stub

					}

				});

				constraint = new Rectangle(x, y, -1, -1);

				display = PlatformUI.getWorkbench().getDisplay();
				display.syncExec(new Runnable() {
					public void run() {

						layeredpane.add(button);
						layeredpane.setConstraint(button, constraint);
						Dimension d = button.getSize();
						if (size.equals("small")) {
							x = x + d.width + (thumbsize + 20);
						} else {
							x = x + d.width + (thumbsize + 50);// constraint);
						}
					}
				});

			}

		}

	}

	private void imageError() {
		setThumbSize();
		if (thumbsize == 50) {
			Image imError=new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/error50.jpg"));
			button = new Button(imError);
			listImages.add(imError);
		} else if (thumbsize == 100) {
			Image imError=new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/error100.jpg"));
			button = new Button(imError);
			listImages.add(imError);
		} else {
			Image imError=new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/error200.jpg"));
			button = new Button(imError);
			listImages.add(imError);

		}
	}

	private void info(final File[] files, int i) {
		double r = files[i].length();
		double f = r / 1024;
		double ff = f / 1024;
		double rr;
		int decimalPlace = 2;
		BigDecimal bd = new BigDecimal(f);
		bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
		r = bd.doubleValue();
		BigDecimal bd2 = new BigDecimal(ff);
		bd2 = bd2.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
		rr = bd2.doubleValue();
		button.setToolTip(new org.eclipse.draw2d.Label(" Directory:  " + files[i].getParent() + "\n" + " File name: " + files[i].getName() + "\n" + " File size:    " + r + " kb | " + rr + " mb"
				+ "\n" + " Width:       " + width + "\n" + " Height:      " + height));
	}

	protected void zoomChange(int level) {
		scalableLayer.setScale(zoomLevel[level]);
	}

	@Override
	public void setFocus() {
		composite.setFocus();

	}

	private void initializeToolBar() {
		IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		ZoomCombo zo = new ZoomCombo();
		tbm.add(five);
		tbm.add(ten);
		tbm.add(fifteen);
		tbm.add(small);
		tbm.add(medium);
		tbm.add(large);
		tbm.add(menuAction);
		tbm.add(recursiveAction);
		tbm.add(preview);
		tbm.add(zo);

	}

	class ZoomCombo extends ControlContribution {
		ZoomCombo() {
			super("zoomCombo");

		}

		protected Control createControl(Composite parent) {

			Combo combo = new Combo(parent, SWT.READ_ONLY);

			for (int i = 0; i < zoomLevel.length; i++) {
				DecimalFormat decimal = new DecimalFormat("###%");
				combo.add(decimal.format(zoomLevel[i]));
			}

			combo.addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {
					zoomChange(((Combo) e.getSource()).getSelectionIndex());
				}

				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});

			combo.select(0);
			return combo;
		}
	}

	private Image createThumbnailFromFile(File file, String fileExtension, ImageData data, boolean stack) {
		String filename = file.toString();
		Image thumb = null;
		Image bigImage = null;

		setThumbSize();

		newWidth = thumbsize;
		newHeight = thumbsize;

		if (data != null) {
			bigImage = new Image(composite.getDisplay(), data);
		}

		/*
		 * else if (fileExtension.equals(".tif") ||
		 * fileExtension.equals(".tiff") || fileExtension.equals(".TIF") ||
		 * fileExtension.equals(".TIFF")) { try {
		 * 
		 * bigImage = new Image(composite.getDisplay(), new
		 * ImageData(filename));
		 * 
		 * } catch (SWTException ex) {
		 * 
		 * System.out.println(ex.getMessage());
		 * 
		 * } }
		 */

		else {

			try {

				bigImage = new Image(composite.getDisplay(), filename);

			} catch (SWTException ex) {
				imageError();
				//System.out.println(ex.getMessage());

			}

		}

		if (bigImage != null) {
			imageRect = bigImage.getBounds();
			width = imageRect.width;
			height = imageRect.height;
			scaled = calculateThumb(imageRect.width, imageRect.height, newWidth, newHeight);
			thumb = new Image(display, newWidth, newHeight);
			gc = null;
			try {
				gc = new GC(thumb);
				gc.fillRectangle(0, 0, newWidth, newHeight);
				int centerX = (newWidth - scaled[0]) / 2;
				int centerY = (newHeight - scaled[1]) / 2;

				gc.drawImage(bigImage, 0, 0, imageRect.width, imageRect.height, centerX, centerY, scaled[0], scaled[1]);
				if (stack) {
					float imageRatio = (float) imageRect.width / imageRect.height;
					if (imageRatio > 1) {
						if (size.equals("small")) {
							gc.drawImage(stackImageSmall, 0, 0);
							gc.drawImage(stackImageSmallFlip, 0, 41);
						} else if (size.equals("medium")) {
							gc.drawImage(stackImageMedium, 0, 0);
							gc.drawImage(stackImageMediumFlip, 0, 84);
						} else {
							gc.drawImage(stackImageBig, 0, 0);
							gc.drawImage(stackImageBigFlip, 0, 167);
						}
					} else {
						if (size.equals("small")) {
							gc.drawImage(stackImageSmallVertFlip, 0, 0);
							gc.drawImage(stackImageSmallVert, 41, 0);
						} else if (size.equals("medium")) {
							gc.drawImage(stackImageMediumVertFlip, 0, 0);
							gc.drawImage(stackImageMediumVert, 84, 0);
						} else {
							gc.drawImage(stackImageBigVertFlip, 0, 0);
							gc.drawImage(stackImageBigVert, 167, 0);
						}

					}
				}

			} finally {
				if (gc != null) {
					gc.dispose();
				}
				bigImage.dispose();

			}
		}

		return thumb;
	}

	private void setThumbSize() {
		if (size.equals("small")) {
			thumbsize = 50;
		} else if (size.equals("medium")) {
			thumbsize = 100;
		} else {
			thumbsize = 200;
		}
	}

	private int[] calculateThumb(int widthImage, int heightImage, int recalulatedWidth, int recalculatedHeight) {

		int[] dim = { widthImage, heightImage };
		float imageRatio = (float) widthImage / heightImage;
		if (widthImage < recalulatedWidth && heightImage < recalculatedHeight) {
			return dim;
		}
		if (imageRatio > 1) {

			if (recalculatedHeight < recalulatedWidth) {
				dim[1] = recalculatedHeight;
				dim[0] = (int) (dim[1] * imageRatio);
			} else {
				dim[0] = recalulatedWidth;
				dim[1] = (int) (dim[0] / imageRatio);
			}
		} else {

			if (recalculatedHeight <= recalulatedWidth) {
				dim[1] = recalculatedHeight;
				dim[0] = (int) (dim[1] * imageRatio);
			} else {
				dim[0] = recalulatedWidth;
				dim[1] = (int) (dim[1] / imageRatio);
			}
		}
		return dim;
	}

	public static ThumbnailsView getThumb() {
		return thumb;
	}

	public SmallThumbAction getSmall() {
		return small;
	}

	public MediumThumbAction getMedium() {
		return medium;
	}

	public LargeThumbAction getLarge() {
		return large;
	}

	public String getSize() {
		return size;
	}

	public void setSmall(SmallThumbAction small) {
		this.small = small;
	}

	public void setMedium(MediumThumbAction medium) {
		this.medium = medium;
	}

	public void setLarge(LargeThumbAction large) {
		this.large = large;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public int getInarow() {
		return inarow;
	}

	public void setInarow(int inarow) {
		this.inarow = inarow;
	}

	public void setCanceled(boolean canceled) {
		this.canceled = canceled;
	}

	public FiveInRowAction getFive() {
		return five;
	}

	public void setFive(FiveInRowAction five) {
		this.five = five;
	}

	public TenInRowAction getTen() {
		return ten;
	}

	public void setTen(TenInRowAction ten) {
		this.ten = ten;
	}

	public FifteenInRowAction getFifteen() {
		return fifteen;
	}

	public void setFifteen(FifteenInRowAction fifteen) {
		this.fifteen = fifteen;
	}

	/* If we cannot open the image with swt! */
	static ImageData convertToSWT(BufferedImage bufferedImage) {
		if (bufferedImage.getColorModel() instanceof DirectColorModel) {
			DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
			PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(), colorModel.getBlueMask());
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[3];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
					data.setPixel(x, y, pixel);
				}
			}
			return data;
		} else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
			IndexColorModel colorModel = (IndexColorModel) bufferedImage.getColorModel();
			int size = colorModel.getMapSize();
			byte[] reds = new byte[size];
			byte[] greens = new byte[size];
			byte[] blues = new byte[size];
			colorModel.getReds(reds);
			colorModel.getGreens(greens);
			colorModel.getBlues(blues);
			RGB[] rgbs = new RGB[size];
			for (int i = 0; i < rgbs.length; i++) {
				rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
			}
			PaletteData palette = new PaletteData(rgbs);
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);
			data.transparentPixel = colorModel.getTransparentPixel();
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					data.setPixel(x, y, pixelArray[0]);
				}
			}
			return data;
		}
		return null;
	}

}
