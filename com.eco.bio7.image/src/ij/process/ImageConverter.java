package ij.process;

import java.awt.*;
import java.awt.image.*;
import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.frame.Recorder;

/** This class converts an ImagePlus object to a different type. */
public class ImageConverter {
	private ImagePlus imp;
	private int type;
	//private static boolean doScaling = Prefs.getBoolean(Prefs.SCALE_CONVERSIONS,true);
	private static boolean doScaling = true;

	/** Constructs an ImageConverter based on an ImagePlus object. */
	public ImageConverter(ImagePlus imp) {
		this.imp = imp;
		type = imp.getType();
	}

	/** Converts this ImagePlus to 8-bit grayscale. */
	public synchronized void convertToGray8() {
		if (imp.getStackSize()>1) {
			new StackConverter(imp).convertToGray8();
			return;
		}
		ImageProcessor ip = imp.getProcessor();
		if (type==ImagePlus.GRAY16 || type==ImagePlus.GRAY32) {
			if (Prefs.calibrateConversions) {
				if (type==ImagePlus.GRAY16 && imp.getCalibration().calibrated())
					convertToGray32();
				convertAndCalibrate(imp,"8-bit");
			} else {
				imp.setProcessor(null, ip.convertToByte(doScaling));
				imp.setCalibration(imp.getCalibration()); //update calibration
			}
			record();
		} else if (type==ImagePlus.COLOR_RGB)
	    	imp.setProcessor(null, ip.convertToByte(doScaling));
		else if (ip.isPseudoColorLut()) {
			boolean invertedLut = ip.isInvertedLut();
			ip.setColorModel(LookUpTable.createGrayscaleColorModel(invertedLut));
	    	imp.updateAndDraw();
		} else {
			ip = new ColorProcessor(imp.getImage());
	    	imp.setProcessor(null, ip.convertToByte(doScaling));
	    }
	    ImageProcessor ip2 = imp.getProcessor();
		if (Prefs.useInvertingLut && ip2 instanceof ByteProcessor && !ip2.isInvertedLut()&& !ip2.isColorLut()) {
			ip2.invertLut();
			ip2.invert();
		}
	}

	/** Converts this ImagePlus to 16-bit grayscale. */
	public void convertToGray16() {
		if (type==ImagePlus.GRAY16)
			return;
		if (!(type==ImagePlus.GRAY8||type==ImagePlus.GRAY32||type==ImagePlus.COLOR_RGB))
			throw new IllegalArgumentException("Unsupported conversion");
		if (imp.getStackSize()>1) {
			new StackConverter(imp).convertToGray16();
			return;
		}
		ImageProcessor ip = imp.getProcessor();
		if (type==ImagePlus.GRAY32)
			record();
		imp.trimProcessor();
		if (Prefs.calibrateConversions && type==ImagePlus.GRAY32)
			convertAndCalibrate(imp,"16-bit");
		else {
			imp.setProcessor(null, ip.convertToShort(doScaling));
			imp.setCalibration(imp.getCalibration()); //update calibration
		}
	}
	
	public static void convertAndCalibrate(ImagePlus imp, String type) {
		setDoScaling(true);
		Calibration cal = imp.getCalibration();
		ImageProcessor ip = imp.getProcessor();
		int stackSize = imp.getStackSize();
		double min = ip.getMin();
		double max = ip.getMax();
		ip.resetMinAndMax();
		double min2 = ip.getMin();
		double max2 = ip.getMax();
		boolean eightBitConversion = type.equals("8-bit");
		if (stackSize>1) {
			cal.disableDensityCalibration();
			ImageStatistics stats = new StackStatistics(imp);
			min2 = stats.min;
			max2 = stats.max;
			convertStack(imp, eightBitConversion, min2, max2);
		} else {
			ImageProcessor ip2 = null;
			if (eightBitConversion)
				ip2 = ip.convertToByte(true);
			else
				ip2 = ip.convertToShort(true);
			imp.setProcessor(null,ip2);
		}
		int maxRange = eightBitConversion?255:65535;
		double[] x = {0,maxRange};		
		double[] y = {min2,max2};
		CurveFitter cf = new CurveFitter(x,y);
		cf.doFit(CurveFitter.STRAIGHT_LINE, false);
		int np = cf.getNumParams();
		double[] p = cf.getParams();
		double[] parameters = new double[np];
		for (int i=0; i<np; i++)
			parameters[i] = p[i];
		cal.setFunction(Calibration.STRAIGHT_LINE, parameters, type+" converted");
		min = cal.getRawValue(min);
		max = cal.getRawValue(max);
		if (IJ.debugMode) IJ.log("convertAndCalibrate: "+min+" "+max);
		imp.setDisplayRange(min,max);
		imp.updateAndDraw();
	}
	
	private static void convertStack(ImagePlus imp, boolean eightBitConversion, double min, double max) {
		int nSlices = imp.getStackSize();
		int width = imp.getWidth();
		int height = imp.getHeight();
		ImageStack stack1 = imp.getStack();
		ImageStack stack2 = new ImageStack(width, height);
		String label;
	    int inc = nSlices/20;
	    if (inc<1) inc = 1;
	    ImageProcessor ip1, ip2;
		for(int i=1; i<=nSlices; i++) {
			label = stack1.getSliceLabel(1);
			ip1 = stack1.getProcessor(1);
			ip1.setMinAndMax(min, max);
			if (eightBitConversion)
				ip2 = ip1.convertToByte(true);
			else
				ip2 = ip1.convertToShort(true);
			stack1.deleteSlice(1);
			stack2.addSlice(label, ip2);
			if ((i%inc)==0) {
				IJ.showProgress((double)i/nSlices);
				IJ.showStatus("Converting to 16-bits: "+i+"/"+nSlices);
			}
		}
		IJ.showProgress(1.0);
		imp.setStack(null, stack2);
		ImageConverter.record();
	}
	
	public static void record() {
		if (IJ.recording()) {
			if (Prefs.calibrateConversions) {
				boolean state = true;
				if (Recorder.scriptMode())
					Recorder.recordCall("Prefs.calibrateConversions = true;", true);
				else
					Recorder.recordString("setOption(\"CalibrateConversions\", "+state+");\n");
			} else {
				Boolean state = ImageConverter.getDoScaling();
				if (Recorder.scriptMode())
					Recorder.recordCall("ImageConverter.setDoScaling("+state+");", true);
				else
					Recorder.recordString("setOption(\"ScaleConversions\", "+state+");\n");
			}
		}
	}

	/** Converts this ImagePlus to 32-bit grayscale. */
	public void convertToGray32() {
		if (type==ImagePlus.GRAY32)
			return;
		if (!(type==ImagePlus.GRAY8||type==ImagePlus.GRAY16||type==ImagePlus.COLOR_RGB))
			throw new IllegalArgumentException("Unsupported conversion");
		Calibration cal = imp.getCalibration();
		double min = cal.getCValue(imp.getDisplayRangeMin());
		double max = cal.getCValue(imp.getDisplayRangeMax());
		if (imp.getStackSize()>1) {
			new StackConverter(imp).convertToGray32();
			IJ.setMinAndMax(imp, min, max);
			return;
		}
		ImageProcessor ip = imp.getProcessor();
		imp.trimProcessor();
		imp.setProcessor(null, ip.convertToFloat());
		imp.setCalibration(cal); //update calibration
		IJ.setMinAndMax(imp, min, max);
	}

	/** Converts this ImagePlus to RGB. */
	public void convertToRGB() {
		if (imp.getBitDepth()==24)
			return;
		if (imp.getStackSize()>1) {
			new StackConverter(imp).convertToRGB();
			return;
		}
		ImageProcessor ip = imp.getProcessor();
		imp.setProcessor(null, ip.convertToRGB());
		imp.setCalibration(imp.getCalibration()); //update calibration
	}
	
	/** Converts an RGB image to an RGB (red, green and blue) stack. */
	public void convertToRGBStack() {
		if (type!=ImagePlus.COLOR_RGB)
			throw new IllegalArgumentException("Image must be RGB");

		//convert to RGB Stack
		ColorProcessor cp;
		if (imp.getType()==ImagePlus.COLOR_RGB)
			cp = (ColorProcessor)imp.getProcessor();
		else
			cp = new ColorProcessor(imp.getImage());
		int width = imp.getWidth();
		int height = imp.getHeight();
		byte[] R = new byte[width*height];
		byte[] G = new byte[width*height];
		byte[] B = new byte[width*height];
		cp.getRGB(R, G, B);
		imp.trimProcessor();
		
		// Create stack and select Red channel
		ColorModel cm = LookUpTable.createGrayscaleColorModel(false);
		ImageStack stack = new ImageStack(width, height, cm);
		stack.addSlice("Red", R);
		stack.addSlice("Green", G);
		stack.addSlice("Blue", B);
		imp.setStack(null, stack);
		imp.setDimensions(3, 1, 1);
		if (imp.isComposite())
			((CompositeImage)imp).setMode(IJ.GRAYSCALE);
	}

	/** Converts an RGB image to a HSB (hue, saturation and brightness) stack. */
	public void convertToHSB() {
		if (type!=ImagePlus.COLOR_RGB)
			throw new IllegalArgumentException("Image must be RGB");;
		ColorProcessor cp;
		if (imp.getType()==ImagePlus.COLOR_RGB)
			cp = (ColorProcessor)imp.getProcessor();
		else
			cp = new ColorProcessor(imp.getImage());
		ImageStack stack = cp.getHSBStack();
		imp.trimProcessor();
		imp.setStack(null, stack);
		imp.setDimensions(3, 1, 1);
	}
	
	/** Converts an RGB image to a 32-bit HSB (hue, saturation and brightness) stack. */
	public void convertToHSB32() {
		if (type!=ImagePlus.COLOR_RGB)
			throw new IllegalArgumentException("Image must be RGB");;
		ColorProcessor cp;
		if (imp.getType()==ImagePlus.COLOR_RGB)
			cp = (ColorProcessor)imp.getProcessor();
		else
			cp = new ColorProcessor(imp.getImage());
		ImageStack stack = cp.getHSB32Stack();
		imp.trimProcessor();
		imp.setStack(null, stack);
		imp.setDimensions(3, 1, 1);
	}

	/** Converts an RGB image to a Lab stack. */
	public void convertToLab() {
		if (type!=ImagePlus.COLOR_RGB)
			throw new IllegalArgumentException("Image must be RGB");
		ColorSpaceConverter converter = new ColorSpaceConverter();
  		ImagePlus imp2 = converter.RGBToLab(imp);
  		Point loc = null;
  		ImageWindow win = imp.getWindow();
		if (win!=null)
			loc = win.getLocation();
		ImageWindow.setNextLocation(loc);
		imp2.show();
		imp.hide();
  		imp2.copyAttributes(imp);
  		imp.changes = false;
  		imp.close();
	}

	/** Converts a 2 or 3 slice 8-bit stack to RGB. */
	public void convertRGBStackToRGB() {
		int stackSize = imp.getStackSize();
		if (stackSize<2 || stackSize>3 || type!=ImagePlus.GRAY8)
			throw new IllegalArgumentException("2 or 3 slice 8-bit stack required");
		int width = imp.getWidth();
		int height = imp.getHeight();
		ImageStack stack = imp.getStack();
		byte[] R = (byte[])stack.getPixels(1);
		byte[] G = (byte[])stack.getPixels(2);
		byte[] B;
		if (stackSize>2)
			B = (byte[])stack.getPixels(3);
		else
			B = new byte[width*height];
		imp.trimProcessor();
		ColorProcessor cp = new ColorProcessor(width, height);
		cp.setRGB(R, G, B);
		if (imp.isInvertedLut())
			cp.invert();
		imp.setImage(cp.createImage());
		imp.killStack();
		if (IJ.isLinux())
			imp.setTitle(imp.getTitle());
	}

	/** Converts a 3-slice (hue, saturation, brightness) 8-bit stack to RGB. */
	public void convertHSBToRGB() {
		if (imp.getStackSize()!=3)
			throw new IllegalArgumentException("3-slice 8-bit stack required");
		ImageStack stack = imp.getStack();
		byte[] H = (byte[])stack.getPixels(1);
		byte[] S = (byte[])stack.getPixels(2);
		byte[] B = (byte[])stack.getPixels(3);
		int width = imp.getWidth();
		int height = imp.getHeight();
		imp.trimProcessor();
		ColorProcessor cp = new ColorProcessor(width, height);
		cp.setHSB(H, S, B);
		imp.setImage(cp.createImage());
		imp.killStack();
		if (IJ.isLinux())
			imp.setTitle(imp.getTitle());
	}
	
	/** Converts a 3-slice (hue, saturation, brightness) 32-bit stack to RGB. */
	public void convertHSB32ToRGB() {
		if (imp.getStackSize()!=3)
			throw new IllegalArgumentException("3-slice 8-bit stack required");
		ImageStack stack = imp.getStack();
		float[] H = (float[])stack.getPixels(1);
		float[] S = (float[])stack.getPixels(2);
		float[] B = (float[])stack.getPixels(3);
		int width = imp.getWidth();
		int height = imp.getHeight();
		imp.trimProcessor();
		ColorProcessor cp = new ColorProcessor(width, height);
		cp.setHSB(H, S, B);
		imp.setImage(cp.createImage());
		imp.killStack();
		if (IJ.isLinux())
			imp.setTitle(imp.getTitle());
	}

	/** Converts a Lab stack to RGB. */
	public void convertLabToRGB() {
		if (imp.getStackSize()!=3)
			throw new IllegalArgumentException("3-slice 32-bit stack required");
		ColorSpaceConverter converter = new ColorSpaceConverter();
		ImagePlus imp2 = converter.LabToRGB(imp);
		imp2.setCalibration(imp.getCalibration());
		imp.setImage(imp2);
	}

	/** Converts an RGB image to 8-bits indexed color. 'nColors' must
		be greater than 1 and less than or equal to 256. */
	public void convertRGBtoIndexedColor(int nColors) {
		if (type!=ImagePlus.COLOR_RGB)
			throw new IllegalArgumentException("Image must be RGB");
		if (nColors<2) nColors = 2;
		if (nColors>256) nColors = 256;
		
		// get RGB pixels
		IJ.showProgress(0.1);
		IJ.showStatus("Grabbing pixels");
		int width = imp.getWidth();
		int height = imp.getHeight();
		ImageProcessor ip = imp.getProcessor();
	 	ip.snapshot();
		int[] pixels = (int[])ip.getPixels();
		imp.trimProcessor();
		
		// convert to 8-bits
		long start = System.currentTimeMillis();
		MedianCut mc = new MedianCut(pixels, width, height);
		ImageProcessor ip2 = mc.convertToByte(nColors);
	    imp.setProcessor(null, ip2);
	    imp.setTypeToColor256();
	}
	
	/** Set true to scale to 0-255 when converting short to byte or float
		to byte and to 0-65535 when converting float to short. */
	public static void setDoScaling(boolean scaleConversions) {
		doScaling = scaleConversions;
		IJ.register(ImageConverter.class); 
	}

	/** Returns true if scaling is enabled. */
	public static boolean getDoScaling() {
		return doScaling;
	}
}
