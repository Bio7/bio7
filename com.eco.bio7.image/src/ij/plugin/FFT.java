package ij.plugin;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.ContrastEnhancer;
import ij.measure.Calibration;
import ij.util.Tools;
import ij.plugin.frame.Recorder;
import java.awt.*;
import java.util.*;

/** 
This class implements the FFT, Inverse FFT and Redisplay Power Spectrum commands 
in the Process/FFT submenu. It is based on Arlo Reeves'	 
Pascal implementation of the Fast Hartley Transform from NIH Image 
(http://imagej.net/ij/docs/ImageFFT/).
The Fast Hartley Transform was restricted by U.S. Patent No. 4,646,256, but was placed 
in the public domain by Stanford University in 1995 and is now freely available.

Version 2008-08-25 inverse transform: mask is always symmetrized
*/
public class FFT implements PlugIn, Measurements {

	// static settings
	public static boolean displayRawPS;
	public static boolean displayFHT;
	public static boolean displayComplex;
	private static boolean displayFFT = true;
	private static boolean doFFT;
	private static boolean reuseWindow;
	public static String fileName;
	
	// settings as instance variables
	private boolean iDisplayRawPS;
	private boolean iDisplayFHT;
	private boolean iDisplayComplex;
	private boolean iDisplayFFT;
	private boolean iDoFFT;

	private ImagePlus imp, imp2;
	private boolean padded;
	private int originalWidth;
	private int originalHeight;
	private int stackSize = 1;
	private int slice = 1;
	private boolean showOutput = true;
	
		
	public void run(String arg) {
		if (arg.equals("options")) {
			showDialog();
			if (iDoFFT)
				arg="fft";
			else
				return;
		}
		if (imp==null)
			imp = IJ.getImage();
		if (arg.equals("fft") && imp.isComposite()) {
			if (!GUI.showCompositeAdvisory(imp,"FFT"))
				return;
		}
		if (arg.equals("redisplay")) {
			redisplayPowerSpectrum();
			return;
		}
		if (arg.equals("swap"))	 {
			if (imp.getWidth()==imp.getHeight()) {
				swapQuadrants(imp.getStack());
				imp.updateAndDraw();
			} else
				IJ.error("Swap Quadrants","Image must be square");
			return;
		}
	   if (arg.equals("inverse")) {
			if (imp.getTitle().startsWith("FHT of")) {
				doFHTInverseTransform();
				return;
			}
			if (imp.getStackSize()==2) {
				doComplexInverseTransform();
				return;
			}
		}
		ImageProcessor ip = imp.getProcessor();
		Object obj = imp.getProperty("FHT");
		FHT fht = (obj instanceof FHT)?(FHT)obj:null;
		stackSize = imp.getStackSize();
		boolean inverse;
		if (fht==null && arg.equals("inverse")) {
			IJ.error("FFT", "Frequency domain image required");
			return;
		}
		if (fht!=null) {
			inverse = true;
			imp.deleteRoi();
		} else {
			if (imp.getRoi()!=null)
				ip = ip.crop();
			fht = newFHT(ip);
			inverse = false;
		}
		if (inverse)
			doInverseTransform(fht);
		else {
			fileName = imp.getTitle();
			doForwardTransform(fht);   
		}	 
		IJ.showProgress(1.0);
		if (IJ.recording()) {
			if (inverse)
   				Recorder.recordCall("imp = FFT.inverse(imp);");
   			else
   				Recorder.recordCall("imp = FFT.forward(imp); //see Help/Examples/JavaScript/FFT Filter");
   		}
	}

	/**
	 * Performs a forward FHT transform.
	 * @param imp  A spatial  domain image, which is not modified
	 * @return	A frequency domain version of the input image
	 * @see #filter
	 * @see #inverse
	*/
	public static ImagePlus forward(ImagePlus imp) {
		FFT fft = new FFT();
		fft.imp = imp;
		fft.showOutput = false;
		fft.run("forward");
		return fft.imp2;
	}

	/**
	 * Multiplies a Fourier domain image by a filter
	 * @param imp A frequency domain image, which is modified.
	 * @param filter  The filter, 32-bits (0-1) or 8-bits (0-255)
	 * @see #forward
	 * @see #inverse
	 * @see #filter
	*/
	public static void multiply(ImagePlus imp, ImageProcessor filter) {
		Object obj = imp.getProperty("FHT");
		FHT fht = obj!=null&&(obj instanceof FHT)?(FHT)obj:null;
		if (fht==null)
			return;
		int size = fht.getWidth();
		boolean isFloat = filter.getBitDepth()==32;
		if (!isFloat)
			filter =  filter.convertToByte(true);					
		filter = filter.resize(size, size);
		swapQuadrants(filter);
		float[] fhtPixels = (float[])fht.getPixels();
		for (int i=0; i<fhtPixels.length; i++) {
			if (isFloat)
				fhtPixels[i] = fhtPixels[i]*filter.getf(i);
			else
				fhtPixels[i] = (float)(fhtPixels[i]*(filter.get(i)/255.0));
		}
	}
	
	/**
	 * Performs an inverse FHT transform.
	 * @param imp  A frequency domain image
	 * @return	A spatial  domain version of the input image
	 * @see #forward
	 * @see #filter
	*/
	public static ImagePlus inverse(ImagePlus imp) {
		FFT fft = new FFT();
		fft.imp = imp;
		fft.showOutput = false;
		fft.run("inverse");
		return fft.imp2;
	}

	/**
	 * Does frequency domain fitering of the speciified image
	 * @param imp The image to be filtered.
	 * @param filter  The filter, 32-bits (0-1) or 8-bits (0-255)
	 * @see #forward
	 * @see #multiply
	 * @see #inverse
	*/
	public static void filter(ImagePlus imp, ImageProcessor filter) {
		Object obj = imp.getProperty("FHT");
		FHT fht = obj!=null&&(obj instanceof FHT)?(FHT)obj:null;
		if (fht!=null) {
			FFT.multiply(imp, filter);
			return;
		}
		ImagePlus imp2 = FFT.forward(imp);
		FFT.multiply(imp2, filter);
		imp.setProcessor(FFT.inverse(imp2).getProcessor());
	}

	/** Version of filter() that accepts an ImagePlus for the filter. */
	public static void filter(ImagePlus imp, ImagePlus filter) {
		filter(imp, filter.getProcessor());
	}
	
	void doInverseTransform(FHT fht) {
		fht = fht.getCopy();
		doMasking(fht);
		showStatus("Inverse transform");
		fht.inverseTransform();
		if (fht.quadrantSwapNeeded)
			fht.swapQuadrants();
		fht.resetMinAndMax();
		ImageProcessor ip2 = fht;
		if (fht.originalWidth>0) {
			fht.setRoi(0, 0, fht.originalWidth, fht.originalHeight);
			ip2 = fht.crop();
		}
		int bitDepth = fht.originalBitDepth>0?fht.originalBitDepth:imp.getBitDepth();
		if (!showOutput && bitDepth!=24)
			bitDepth = 32;
		switch (bitDepth) {
			case 8: ip2 = ip2.convertToByte(false); break;
			case 16: ip2 = ip2.convertToShort(false); break;
			case 24:
				showStatus("Setting brightness");
				if (fht.rgb==null || ip2==null) {
					IJ.error("FFT", "Unable to set brightness");
					return;
				}
				ColorProcessor rgb = (ColorProcessor)fht.rgb.duplicate();
				rgb.setBrightness((FloatProcessor)ip2);
				ip2 = rgb; 
				fht.rgb = null;
				break;
			case 32: break;
		}
		if (bitDepth!=24 && fht.originalColorModel!=null)
			ip2.setColorModel(fht.originalColorModel);
		String title = imp.getTitle();
		if (title.startsWith("FFT of "))
			title = title.substring(7, title.length());
		ImagePlus imp2 = new ImagePlus("Inverse FFT of "+title, ip2);
		imp2.setCalibration(imp.getCalibration());
		if (showOutput)
			imp2.show();
		else
			this.imp2 = imp2;
	}

	void doForwardTransform(FHT fht) {
		showStatus("Forward transform");
		long t0 = System.currentTimeMillis();
		fht.transform();
		if (iDisplayRawPS || (displayRawPS&&!IJ.isMacro())) {
			ImageProcessor rawps = fht.getRawPowerSpectrum();
			if (rawps!=null) {
				swapQuadrants(rawps);
				ImagePlus imp2 = new ImagePlus("PS of "+fileName, rawps);
				enhanceContrast(imp2);
				imp2.show();
			}
		}
		if (iDisplayFHT || (displayFHT&&!IJ.isMacro())) {
			ImageProcessor ip2 = fht.duplicate();
			swapQuadrants(ip2);
			ImagePlus imp2 = new ImagePlus("FHT of "+FFT.fileName, ip2);
			enhanceContrast(imp2);
			setImageProperties(imp2, "Fast Hartley Transform");
			imp2.show();
		}
		if (iDisplayComplex || (displayComplex&&!IJ.isMacro())) {
			ImageStack ct = fht.getComplexTransform();
			ImagePlus imp2 = new ImagePlus("Complex of "+FFT.fileName, ct);
			enhanceContrast(imp2);
			setImageProperties(imp2, "Complex Fourier Transform");
			imp2.show();
		}
		if (!(iDisplayFHT || iDisplayComplex || iDisplayRawPS))
			iDisplayFFT = true;
		if (iDisplayFFT) {
			showStatus("Calculating power spectrum");
			ImageProcessor ps = fht.getPowerSpectrum();
			String title = "FFT of "+imp.getTitle();
			ImagePlus imp2 = new ImagePlus(title, ps);
			if (showOutput) {
				ImagePlus fftImage = reuseWindow?WindowManager.getImage(title):null;
				if (fftImage!=null)
					fftImage.setImage(imp2);
				else
					imp2.show((System.currentTimeMillis()-t0)+" ms");
			}
			fht.powerSpectrumMean = ps.getStats().mean;
			imp2.setProperty("FHT", fht);
			imp2.setCalibration(imp.getCalibration());
			String properties = "Fast Hartley Transform\n";
			properties += "width: "+fht.originalWidth + "\n";
			properties += "height: "+fht.originalHeight + "\n";
			properties += "bitdepth: "+fht.originalBitDepth + "\n";
			imp2.setProperty("Info", properties);
			if (!showOutput)
				this.imp2 = imp2;
		}
	}
	
	private void setImageProperties(ImagePlus imp, String type) {
		imp.setProp(" ", type);
		imp.setProp("Original width", originalWidth);
		imp.setProp("Original height", originalHeight);
	}
	
	private void enhanceContrast(ImagePlus imp) {
		IJ.run(imp, "Enhance Contrast", "saturated=0.35");
	}
	
	FHT newFHT(ImageProcessor ip) {
		FHT fht;
		if (ip instanceof ColorProcessor) {
			showStatus("Extracting brightness");
			ImageProcessor ip2 = ((ColorProcessor)ip).getBrightness();
			fht = new FHT(pad(ip2));
			fht.rgb = (ColorProcessor)ip.duplicate(); // save so we can later update the brightness
		} else
			fht = new FHT(pad(ip));
		if (padded) {
			fht.originalWidth = originalWidth;
			fht.originalHeight = originalHeight;
		}
		int bitDepth = imp.getBitDepth();
		fht.originalBitDepth = bitDepth;
		if (bitDepth!=24)
			fht.originalColorModel = ip.getColorModel();
		return fht;
	}
	
	ImageProcessor pad(ImageProcessor ip) {
		originalWidth = ip.getWidth();
		originalHeight = ip.getHeight();
		int maxN = Math.max(originalWidth, originalHeight);
		int i = 2;
		while(i<maxN) i *= 2;
		if (i==maxN && originalWidth==originalHeight) {
			padded = false;
			return ip;
		}
		maxN = i;
		showStatus("Padding to "+ maxN + "x" + maxN);
		if (maxN>=65536) {
			IJ.error("FFT", "Padded image is too large ("+maxN+"x"+maxN+")");
			return null;
		}
		ImageStatistics stats = ImageStatistics.getStatistics(ip, MEAN, null);
		ImageProcessor ip2 = ip.createProcessor(maxN, maxN);
		ip2.setValue(stats.mean);
		ip2.fill();
		ip2.insert(ip, 0, 0);
		padded = true;
		Undo.reset();
		return ip2;
	}
	
	void showStatus(String msg) {
		if (stackSize>1)
			IJ.showStatus("FFT: " + slice+"/"+stackSize);
		else
			IJ.showStatus(msg);
	}
	
	void doMasking(FHT ip) {
		if (stackSize>1)
			return;
		float[] fht = (float[])ip.getPixels();
		ImageProcessor mask = imp.getProcessor();
		int bitDepth = mask.getBitDepth();
		mask = mask.convertToByte(false);
		if (mask.getWidth()!=ip.getWidth() || mask.getHeight()!=ip.getHeight())
			return;
		mask.resetRoi();
		ImageStatistics stats = mask.getStats();
		if (stats.histogram[0]==0 && stats.histogram[255]==0) {
			if (bitDepth==8 && ip.powerSpectrumMean!=stats.mean)
				IJ.showMessage("Inverse FFT", "No pixels have been set to 0 (black) or\n255 (white) so filtering will not be done.");
			return;
		}
		boolean passMode = stats.histogram[255]!=0;
		IJ.showStatus("Masking: "+(passMode?"pass":"filter"));
		mask = mask.duplicate();
		if (passMode)
			changeValuesAndSymmetrize(mask, (byte)255, (byte)0); //0-254 become 0
		else
			changeValuesAndSymmetrize(mask, (byte)0, (byte)255); //1-255 become 255
		for (int i=0; i<3; i++)
			smooth(mask);
		if (IJ.debugMode || IJ.altKeyDown())
			new ImagePlus("mask", mask.duplicate()).show();
		swapQuadrants(mask);
		byte[] maskPixels = (byte[])mask.getPixels();
		for (int i=0; i<fht.length; i++) {
			fht[i] = (float)(fht[i]*(maskPixels[i]&255)/255.0);
		}
	}

	// Change pixels not equal to v1 to the new value v2.
	// For pixels equal to v1, also the symmetry-equivalent pixel is set to v1
	// Requires a quadratic 8-bit image.
	void changeValuesAndSymmetrize(ImageProcessor ip, byte v1, byte v2) {
		byte[] pixels = (byte[])ip.getPixels();
		int n = ip.getWidth();
		for (int i=0; i<pixels.length; i++) {
			if (pixels[i] == v1) {	//pixel has been edited for pass or filter, set symmetry-equivalent
				if (i%n==0) {		//left edge
					if (i>0) pixels[n*n-i] = v1;
				} else if (i<n)		//top edge
					pixels[n-i] = v1;
				else				//no edge
					pixels[n*(n+1)-i] = v1;
			} else
				pixels[i] = v2;		//reset all other pixel values
		}
	}

	// Smooth an 8-bit square image with periodic boundary conditions
	// by averaging over 3x3 pixels
	// Requires a quadratic 8-bit image.
	static void smooth(ImageProcessor ip) {
		byte[] pixels = (byte[])ip.getPixels();
		byte[] pixels2 = (byte[])pixels.clone();
		int n = ip.getWidth();
		int[] iMinus = new int[n];	//table of previous index modulo n
		int[] iPlus = new int[n];	//table of next index modulo n
		for (int i=0; i<n; i++) {	//creating the tables in advance is faster calculating each time
			iMinus[i] = (i-1+n)%n;
			iPlus[i] = (i+1)%n;
		}
		for (int y=0; y<n; y++) {
			int offset1 = n*iMinus[y];
			int offset2 = n*y;
			int offset3 = n*iPlus[y];
			for (int x=0; x<n; x++) {
				int sum = (pixels2[offset1+iMinus[x]]&255)
						+ (pixels2[offset1+x]&255)
						+ (pixels2[offset1+iPlus[x]]&255)
						+ (pixels2[offset2+iMinus[x]]&255)
						+ (pixels2[offset2+x]&255)
						+ (pixels2[offset2+iPlus[x]]&255)
						+ (pixels2[offset3+iMinus[x]]&255)
						+ (pixels2[offset3+x]&255)
						+ (pixels2[offset3+iPlus[x]]&255);
				pixels[offset2 + x] = (byte)((sum+4)/9);
			}
		}
	}

	void redisplayPowerSpectrum() {
		FHT fht = (FHT)imp.getProperty("FHT");
		if (fht==null)
			{IJ.error("FFT", "Frequency domain image required"); return;}
		ImageProcessor ps = fht.getPowerSpectrum();
		imp.setProcessor(null, ps);
	}
	
	public static void swapQuadrants(ImageProcessor ip) {
 		long time0 = System.currentTimeMillis();
 		ImageProcessor t1, t2;
		int size = ip.getWidth()/2;
		ip.setRoi(size,0,size,size);
		t1 = ip.crop();
  		ip.setRoi(0,size,size,size);
		t2 = ip.crop();
		ip.insert(t1,0,size);
		ip.insert(t2,size,0);
		ip.setRoi(0,0,size,size);
		t1 = ip.crop();
  		ip.setRoi(size,size,size,size);
		t2 = ip.crop();
		ip.insert(t1,size,size);
		ip.insert(t2,0,0);
		ip.resetRoi();
		long time1 = System.currentTimeMillis();
		//IJ.log(""+(time1-time0)+" "+ip);
	}

	void swapQuadrants(ImageStack stack) {
		FHT fht = new FHT(new FloatProcessor(1, 1));
		for (int i=1; i<=stack.size(); i++)
			swapQuadrants(stack.getProcessor(i));
	}
	
	void showDialog() {
		if (!IJ.isMacro()) {
			iDisplayRawPS = displayRawPS;
			iDisplayFHT = displayFHT;
			iDisplayComplex = displayComplex;
			iDisplayFFT = displayFFT;
			iDoFFT = doFFT;
		}
		GenericDialog gd = new GenericDialog("FFT Options");
		gd.setInsets(0, 20, 0);
		gd.addMessage("Display:");
		gd.setInsets(5, 35, 0);
		gd.addCheckbox("FFT (\"FFT of...\") window", iDisplayFFT);
		gd.setInsets(0, 35, 0);
		gd.addCheckbox("Raw power spectrum", iDisplayRawPS);
		gd.setInsets(0, 35, 0);
		gd.addCheckbox("Fast Hartley Transform", iDisplayFHT);
		gd.setInsets(0, 35, 0);
		gd.addCheckbox("Complex Fourier Transform", iDisplayComplex);
		gd.setInsets(8, 20, 0);
		gd.addCheckbox("Reuse \"FFT of...\" window", reuseWindow);
		gd.addCheckbox("Do forward transform", iDoFFT);
		gd.addHelp(IJ.URL2+"/docs/menus/process.html#fft-options");
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		iDisplayFFT = gd.getNextBoolean();
		iDisplayRawPS = gd.getNextBoolean();
		iDisplayFHT = gd.getNextBoolean();
		iDisplayComplex = gd.getNextBoolean();
		reuseWindow = gd.getNextBoolean();
		iDoFFT = gd.getNextBoolean();
		if (!IJ.isMacro()) {
			displayRawPS = iDisplayRawPS;
			displayFHT = iDisplayFHT;
			displayComplex = iDisplayComplex;
			displayFFT = iDisplayFFT;
			doFFT = iDoFFT;
		}
	}
	
	void doFHTInverseTransform() {
		FHT fht = new FHT(imp.getProcessor().duplicate(), true);
		swapQuadrants(fht);
		fht.inverseTransform();
		fht.resetMinAndMax();
		String name = WindowManager.getUniqueName(imp.getTitle().substring(7));
		IJ.showProgress(1.0);
		ImagePlus img = new ImagePlus(name, fht);
		img = unpad(img);
		img.show();
	}

	void doComplexInverseTransform() {
		ImageStack stack = imp.getStack();
		if (!stack.getSliceLabel(1).equals("Real"))
			return;
		int maxN = imp.getWidth();
		swapQuadrants(stack);
		float[] rein = (float[])stack.getPixels(1);
		float[] imin = (float[])stack.getPixels(2);
		float[] reout= new float[maxN*maxN];
		float[] imout = new float[maxN*maxN];
		c2c2DFFT(rein, imin, maxN, reout, imout);
		ImageStack stack2 = new ImageStack(maxN, maxN);
		swapQuadrants(stack);
		stack2.addSlice("Real", reout);
		stack2.addSlice("Imaginary", imout);
		String name = WindowManager.getUniqueName(imp.getTitle().substring(10));
		ImagePlus imp2 = new ImagePlus(name, stack2);
		imp2 = unpad(imp2);
		imp2.getProcessor().resetMinAndMax();
		imp2.show();
	}
	
	private ImagePlus unpad(ImagePlus img) {
		int width = (int)imp.getNumericProp("Original width");
		int height = (int)imp.getNumericProp("Original height");
		if (width==0 || height==0 || (width==img.getWidth()&&height==img.getHeight()))
			return img;
		int i=2;
		while (i<width) i *= 2;
		if (i==width && width==height)
			return img;
		img.setRoi(0, 0, width, height);
		return img.crop("stack");
	}
	
	/** Complex to Complex Inverse Fourier Transform
	*	Author: Joachim Wesner
	*/
	void c2c2DFFT(float[] rein, float[] imin, int maxN, float[] reout, float[] imout) {
			FHT fht = new FHT(new FloatProcessor(maxN,maxN));
			float[] fhtpixels = (float[])fht.getPixels();
			// Real part of inverse transform
			for (int iy = 0; iy < maxN; iy++)
				  cplxFHT(iy, maxN, rein, imin, false, fhtpixels);
			fht.inverseTransform();
			// Save intermediate result, so we can do a "in-place" transform
			float[] hlp = new float[maxN*maxN];
			System.arraycopy(fhtpixels, 0, hlp, 0, maxN*maxN);
			// Imaginary part of inverse transform
			for (int iy = 0; iy < maxN; iy++)
				  cplxFHT(iy, maxN, rein, imin, true, fhtpixels);
			fht.inverseTransform();
			System.arraycopy(hlp, 0, reout, 0, maxN*maxN);
			System.arraycopy(fhtpixels, 0, imout, 0, maxN*maxN);
	  }

	/** Build FHT input for equivalent inverse FFT
	*	Author: Joachim Wesner
	*/
	void cplxFHT(int row, int maxN, float[] re, float[] im, boolean reim, float[] fht) {
			int base = row*maxN;
			int offs = ((maxN-row)%maxN) * maxN;
			if (!reim) {
				  for (int c=0; c<maxN; c++) {
						int l =	 offs + (maxN-c)%maxN;
						fht[base+c] = ((re[base+c]+re[l]) - (im[base+c]-im[l]))*0.5f;
				  }
			} else {
				  for (int c=0; c<maxN; c++) {
						int l = offs + (maxN-c)%maxN;
						fht[base+c] = ((im[base+c]+im[l]) + (re[base+c]-re[l]))*0.5f;
				  }
			}
	  }
	  
}