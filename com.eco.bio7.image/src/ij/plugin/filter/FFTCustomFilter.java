package ij.plugin.filter;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.ContrastEnhancer;
import ij.plugin.frame.Recorder;
import java.awt.*;
import java.util.*;


/** This class implements the Process/FFT/Custom Filter command. */
public class FFTCustomFilter implements  PlugInFilter, Measurements {

	private ImagePlus imp;
	private static int filterIndex = 1;
	private int slice;
	private int stackSize;	
	private ImageProcessor filter;
	private static boolean processStack;
	private boolean padded;
	private int originalWidth;
	private int originalHeight;
	private Rectangle rect = new Rectangle();

	public int setup(String arg, ImagePlus imp) {
 		this.imp = imp;
 		if (imp==null) {
 			IJ.noImage();
 			return DONE;
 		}
 		this.stackSize = imp.getStackSize();
		filter = getFilter();
		if (filter==null)
			return DONE;
		if (imp.getProperty("FHT")!=null) {
			IJ.error("FFT Custom Filter", "Spatial domain (non-FFT) image required");
			return DONE;
		} else
			return processStack?DOES_ALL+DOES_STACKS:DOES_ALL;
	}

	public void run(ImageProcessor ip) {
		slice++;
		FHT fht = newFHT(ip);
		if (slice==1)
			filter = resizeFilter(filter, fht.getWidth());
		((FHT)fht).transform();
		customFilter(fht);		
		doInverseTransform(fht, ip);
		if (slice==1)
			ip.resetMinAndMax();
		if (slice==stackSize) {
			new ContrastEnhancer().stretchHistogram(imp, 0.0);
			imp.updateAndDraw();
		}
		IJ.showProgress(1.0);
		if (IJ.recording() && slice==1)
			Recorder.recordCall("FFT.filter(imp,filter); //see Help/Examples/JavaScript/FFT Filter");
	}
	
	void doInverseTransform(FHT fht, ImageProcessor ip) {
		showStatus("Inverse transform");
		fht.inverseTransform();
		//if (fht.quadrantSwapNeeded)
		//	fht.swapQuadrants();
		fht.resetMinAndMax();
		ImageProcessor ip2 = fht;
		fht.setRoi(rect.x, rect.y, rect.width, rect.height);
		ip2 = fht.crop();
		int bitDepth = fht.originalBitDepth>0?fht.originalBitDepth:imp.getBitDepth();
		switch (bitDepth) {
			case 8: ip2 = ip2.convertToByte(true); break;
			case 16: ip2 = ip2.convertToShort(true); break;
			case 24:
				showStatus("Setting brightness");
				fht.rgb.setBrightness((FloatProcessor)ip2);
				ip2 = fht.rgb; 
				fht.rgb = null;
				break;
			case 32: break;
		}
		ip.insert(ip2, 0, 0);
	}

	FHT newFHT(ImageProcessor ip) {
		FHT fht;
		int width = ip.getWidth();
		int height = ip.getHeight();
		int maxN = Math.max(width, height);
		int size = 2;
		while (size<1.5*maxN) size *= 2;		
		rect.x = (int)Math.round((size-width)/2.0);
		rect.y = (int)Math.round((size-height)/2.0);
		rect.width = width;
		rect.height = height;
		FFTFilter fftFilter = new FFTFilter();
		if (ip instanceof ColorProcessor) {
			showStatus("Extracting brightness");
			ImageProcessor ip2 = ((ColorProcessor)ip).getBrightness();
			fht = new FHT(fftFilter.tileMirror(ip2, size, size, rect.x, rect.y));
			fht.rgb = (ColorProcessor)ip.duplicate(); // save so we can later update the brightness
		} else
			fht = new FHT(fftFilter.tileMirror(ip, size, size, rect.x, rect.y));
		fht.originalWidth = originalWidth;
		fht.originalHeight = originalHeight;
		fht.originalBitDepth = imp.getBitDepth();
		return fht;
	}
	
	void showStatus(String msg) {
		if (stackSize>1)
			IJ.showStatus("FFT: " + slice+"/"+stackSize);
		else
			IJ.showStatus(msg);
	}
		
	void customFilter(FHT fht) {
		int size = fht.getWidth();
		showStatus("Filtering");
		fht.swapQuadrants(filter);
		float[] fhtPixels = (float[])fht.getPixels();
		boolean isFloat = filter.getBitDepth()==32;
		for (int i=0; i<fhtPixels.length; i++) {
			if (isFloat)
				fhtPixels[i] = fhtPixels[i]*filter.getf(i);
			else
				fhtPixels[i] = (float)(fhtPixels[i]*(filter.get(i)/255.0));
		}
		fht.swapQuadrants(filter);
	}
	
	ImageProcessor getFilter() {
		int[] wList = WindowManager.getIDList();
		if (wList==null || wList.length<2) {
			IJ.error("FFT", "A filter (as an open image) is required.");
			return null;
		}
		String[] titles = new String[wList.length];
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			titles[i] = imp!=null?imp.getTitle():"";
		}
		if (filterIndex<0 || filterIndex>=titles.length)
			filterIndex = 1;
		GenericDialog gd = new GenericDialog("FFT Filter");
		gd.addChoice("Filter:", titles, titles[filterIndex]);
		if (stackSize>1)
			gd.addCheckbox("Process entire stack", processStack);
		gd.addHelp(IJ.URL2+"/docs/menus/process.html#fft-filter");
		gd.showDialog();
		if (gd.wasCanceled())
			return null;
		filterIndex = gd.getNextChoiceIndex();
		if (stackSize>1)
			processStack = gd.getNextBoolean();
		ImagePlus filterImp = WindowManager.getImage(wList[filterIndex]);
		if (filterImp==imp) {
			IJ.error("FFT", "The filter cannot be the same as the image being filtered.");
			return null;
		}		
		if (filterImp.getStackSize()>1) {
			IJ.error("FFT", "The filter cannot be a stack.");
			return null;
		}		
		ImageProcessor filter = filterImp.getProcessor();
		if (filter!=null && filter.getBitDepth()!=32)		
			filter =  filter.convertToByte(true);		
		return filter;
	}
	
	ImageProcessor resizeFilter(ImageProcessor ip, int maxN) {
		int width = ip.getWidth();
		int height = ip.getHeight();
		if (width==maxN && height==maxN)
			return ip;
		showStatus("Scaling filter to "+ maxN + "x" + maxN);
		return ip.resize(maxN, maxN);
	}
		
}