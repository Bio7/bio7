package ij.plugin;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.measure.Calibration;
import ij.macro.Interpreter;
import ij.io.FileInfo;
import ij.plugin.frame.Recorder;
import ij.util.Tools;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextListener;
import java.awt.image.ColorModel;


/** Implements the "Stack to HyperStack", "RGB to HyperStack" 
	and "HyperStack to Stack" commands. */
public class HyperStackConverter implements PlugIn {
	public static final int CZT=0, CTZ=1, ZCT=2, ZTC=3, TCZ=4, TZC=5;
	static final int C=0, Z=1, T=2;
	static final String[] orders = {"xyczt(default)", "xyctz", "xyzct", "xyztc", "xytcz", "xytzc"};
	static int ordering = CZT;
	static boolean splitRGB = true;
	static final String[] DIMENSION_LABELS = {"Channels (c):", "Slices (z):", "Frames (t):"};
	static final int MAX_DIMENSIONS = DIMENSION_LABELS.length;
	static int lastAutoCbxIndex = -1;    //remembers which dimension to auto-calculate in Stack to hyperstack

	public void run(String arg) {
		if (arg.equals("new"))
			{newHyperStack(); return;}
		ImagePlus imp = IJ.getImage();
		if (arg.equals("stacktohs"))
			convertStackToHS(imp);
		else if (arg.equals("hstostack"))
			convertHSToStack(imp);
	}

	/** Converts the specified stack into a hyperstack with 'c' channels, 'z' slices and
		 't' frames using the default ordering ("xyczt") and display mode ("Composite"). */
	public static ImagePlus toHyperStack(ImagePlus imp, int c, int z, int t) {
		return toHyperStack(imp, c, z, t, null, null);
	}

	/** Converts the specified stack into a hyperstack with 'c' channels, 'z' slices and
		 't' frames using the default ordering ("xyczt") and the specified display
		 mode ("composite", "color" or "grayscale"). */
	public static ImagePlus toHyperStack(ImagePlus imp, int c, int z, int t, String mode) {
		return toHyperStack(imp, c, z, t, null, mode);
	}

	/** Converts the specified stack into a hyperstack with 'c' channels,
	 *  'z' slices and 't' frames. The default "xyczt" order is used if
	 * 'order' is null. The default "composite" display mode is used
	 * if 'mode' is null.
	 * @param imp the stack to be converted
	 * @param c channels
	 * @param z slices
	 * @param t frames
	 * @param order hyperstack order ("default", "xyctz", "xyzct", "xyztc", "xytcz" or "xytzc")
	 * @param mode display mode ("composite", "color" or "grayscale")
	 * @return the resulting hyperstack
	*/
	public static ImagePlus toHyperStack(ImagePlus imp, int c, int z, int t, String order, String mode) {
		int n = imp.getStackSize();
		if (n==1)
			throw new IllegalArgumentException("Stack required");
		if (imp.getBitDepth()==24 && mode.equalsIgnoreCase("composite"))
			mode = "color";
		if (c*z*t!=n)
			throw new IllegalArgumentException("C*Z*T not equal stack size");
		imp.setDimensions(c, z, t);
		if (order==null || order.equals("default") || order.equals("xyczt"))
			order = orders[0];
		int intOrder = CZT;
		for (int i=0; i<orders.length; i++) {
			if (order.equals(orders[i])) {
				intOrder = i;
				break;
			}
		}
		if (intOrder!=CZT && imp.getStack().isVirtual())
			reorderVirtualStack(imp, intOrder);
		else
			(new HyperStackConverter()).shuffle(imp, intOrder);
		ImagePlus imp2 = imp;
		int intMode = IJ.COMPOSITE;
		if (mode!=null) {
			if (mode.equalsIgnoreCase("color"))
				intMode = IJ.COLOR;
			else if (mode.equalsIgnoreCase("grayscale"))
				intMode = IJ.GRAYSCALE;
		}
		if (c>1) {
			LUT[] luts = imp.getLuts();
			if (luts!=null && luts.length<c)
				luts = null;
			imp2 = new CompositeImage(imp, intMode);
			if (luts!=null)
				((CompositeImage)imp2).setLuts(luts);
		}
		imp2.setOpenAsHyperStack(true);
		imp2.setOverlay(imp.getOverlay());
		return imp2;
	}

	/** Converts the specified hyperstack into a stack. */
	public static void toStack(ImagePlus imp) {
		if (imp.isHyperStack()||imp.isComposite()) {
			imp.setDimensions(1,imp.getStackSize(),1);
			imp.draw();				
		}
	}

	private static void reorderVirtualStack(ImagePlus imp, int order) {
		if (!(imp.getStack() instanceof VirtualStack))
			return;
		int[] indexes = shuffleVirtual(imp, order);
		VirtualStack vstack = (VirtualStack)imp.getStack();
		vstack.setIndexes(indexes);
	}

	//String msg = "This is a custom VirtualStack. To switch to "+orders[order]+" order,\n"
	//	+"save in TIFF format, import as a TIFF Virtual Stack and\n"
	//	+"again use the \"Stack to Hyperstack\" command.";

	private static int[] shuffleVirtual(ImagePlus imp, int order) {
		int n = imp.getStackSize();
		int nChannels = imp.getNChannels();
		int nSlices = imp.getNSlices();
		int nFrames = imp.getNFrames();
		int first=C, middle=Z, last=T;
		int nFirst=nChannels, nMiddle=nSlices, nLast=nFrames;
		switch (order) {
			case CTZ: first=C; middle=T; last=Z;
				nFirst=nChannels; nMiddle=nFrames; nLast=nSlices;
				break;
			case ZCT: first=Z; middle=C; last=T;
				nFirst=nSlices; nMiddle=nChannels; nLast=nFrames;
				break;
			case ZTC: first=Z; middle=T; last=C;
				nFirst=nSlices; nMiddle=nFrames; nLast=nChannels;
				break;
			case TCZ: first=T; middle=C; last=Z;
				nFirst=nFrames; nMiddle=nChannels; nLast=nSlices;
				break;
			case TZC: first=T; middle=Z; last=C;
				nFirst=nFrames; nMiddle=nSlices; nLast=nChannels;
				break;
		}
		int[] indexes1 = new int[n];
		int[] indexes2 = new int[n];
		for (int i=0; i<n; i++) {
			indexes1[i] = i;
			indexes2[i] = i;
		}
		int[] index = new int[3];
		for (index[2]=0; index[2]<nFrames; ++index[2]) {
			for (index[1]=0; index[1]<nSlices; ++index[1]) {
				for (index[0]=0; index[0]<nChannels; ++index[0]) {
					int dstIndex = index[0] + index[1]*nChannels + index[2]*nChannels*nSlices;
					int srcIndex = index[first] + index[middle]*nFirst + index[last]*nFirst*nMiddle;
					indexes1[dstIndex] = indexes2[srcIndex];
				}
			}
		}
		return indexes1;
	}

	/** Displays the specified stack in a HyperStack window. Based on the 
		Stack_to_Image5D class in Joachim Walter's Image5D plugin. */
	void convertStackToHS(ImagePlus imp) {
		final int stackSize = imp.getImageStackSize();
		final boolean rgb = imp.getBitDepth()==24;
		final int[] dimensions = {imp.getNChannels(), imp.getNSlices(), imp.getNFrames()};
		if (stackSize==1) {
			IJ.error("Stack to HyperStack", "Stack required");
			return;
		}
		final String[] modes = {"Composite", "Color", "Grayscale"};
		final Checkbox[] autoCbxs = new Checkbox[MAX_DIMENSIONS]; //checkboxes for auto calculation
		final GenericDialog gd = new GenericDialog("Convert to HyperStack");
		gd.addChoice("Order:", orders, orders[ordering]);
		for (int i=0; i<MAX_DIMENSIONS; i++) {              //Channels C, Slices Z, Times T
			gd.addNumericField(DIMENSION_LABELS[i], dimensions[i], 0);
			if (rgb && i==C)
				gd.getLabel().setVisible(false);            //channels input not allowed for RGB (thus make label invisible)
			if (!GraphicsEnvironment.isHeadless()) {	    //add 'auto' checkbox
				Checkbox cb = new Checkbox("auto", i==lastAutoCbxIndex);
				autoCbxs[i] = cb;
				Panel panel = new Panel();
				panel.add(cb);
				gd.addToSameRow();
				gd.addPanel(panel);
			}
		}
		gd.addChoice("Display Mode:", modes, modes[2]);
		if (rgb) {
			gd.setInsets(15, 0, 0);
			gd.addCheckbox("Convert RGB to 3 Channel Hyperstack", splitRGB);
			autoCbxs[C].setVisible(false);                  //channels input not allowed for RGB (TextField & checkbox invisible)
			autoCbxs[C].setState(false);
			((TextField)(gd.getNumericFields().elementAt(C))).setVisible(false);
		}

		if (!GraphicsEnvironment.isHeadless()) {
			final DialogListener dialogListener = new DialogListener() { // checks the validity of the input and calculates 'auto' dimension
				public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
					gd.resetCounters();
					int autoCbxIndex = -1;
					for (int i=0; i<MAX_DIMENSIONS; i++) {  //determine currently active #auto checkbox
						if (autoCbxs[i].getState()) {
							autoCbxIndex = i;
							break;
						}
					}

					int numFieldIndex = e==null ?           //numeric field triggering the callback or -1
							-1 : gd.getNumericFields().indexOf(e.getSource());
					//IJ.log("autoCbx="+autoCbxIndex+" numSrc="+numFieldIndex+" e="+e);
					if (numFieldIndex >= 0 && numFieldIndex == autoCbxIndex) {
						autoCbxs[autoCbxIndex].setState(false); //if manual input into 'auto' field: disable 'auto'
						autoCbxIndex = -1;
					}
					double sizeProduct = 1;
					for (int i=0; i<MAX_DIMENSIONS; i++) {
						double num = gd.getNextNumber();
						if (i==autoCbxIndex) continue;
						if (!(num > 0 && num == (int)num)) {
							IJ.showStatus(DIMENSION_LABELS[i]+" Invalid");
							return false;
						}
						sizeProduct *= num;
					}
					if (autoCbxIndex >= 0) {                //calculate the 'auto' dimension
						double autoValue = stackSize/sizeProduct;
						boolean autoOk = (autoValue == (int)autoValue);
						String autoFieldText = autoOk ? Integer.toString((int)autoValue) : "invalid";
						final TextField autoField = (TextField)(gd.getNumericFields().elementAt(autoCbxIndex)); //input field with auto-calculated dimension
						final TextListener[] listeners = autoField.getTextListeners();
						for (TextListener l:listeners)      //disable callback when we set the field text
							autoField.removeTextListener(l);
						autoField.setText(autoFieldText);
						EventQueue.invokeLater(new Runnable() {
							public void run() {             //re-enable listeners later, after the change is really done (otherwise race condition)
								for (TextListener l:listeners)
									autoField.addTextListener(l);
							}
						});
						if (!autoOk) {
							IJ.showStatus(DIMENSION_LABELS[autoCbxIndex]+" No integer result");
							return false;
						}
					} else if (sizeProduct != stackSize) {
						IJ.showStatus("channels * slices * frames = " + (int)sizeProduct + " differs from stack size (" + stackSize + ")");
						return false;
					}
					lastAutoCbxIndex = autoCbxIndex;
					IJ.showStatus("");
					return true;
				}
			}; //end of new DialogListener
			gd.addDialogListener(dialogListener);

			for (final Checkbox cb : autoCbxs) {            //listener: enabling an 'auto' checkbox should trigger calculation
				cb.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent e) {
						if (cb.getState()) {
							for (Checkbox cb2 : autoCbxs)
								if (cb2 != e.getSource())
									cb2.setState(false);    //radio-button like, never more than one checkbox on
							gd.getButtons()[0].setEnabled(dialogListener.dialogItemChanged(gd, e)); //auto calculation; enable ok button if success
						}
					}
				});
			}
			dialogListener.dialogItemChanged(gd, null);     //enables/diables fields
		}

		gd.showDialog();
		if (gd.wasCanceled()) return;
		ordering = gd.getNextChoiceIndex();
		int nChannels = (int) gd.getNextNumber();
		int nSlices = (int) gd.getNextNumber();
		int nFrames = (int) gd.getNextNumber();
		int mode = gd.getNextChoiceIndex();
		if (rgb)
			splitRGB = gd.getNextBoolean();
/*		if (rgb && splitRGB==true) {
			new CompositeConverter().run(mode==0?"composite":"color");
			return;
		}*/
		if (rgb && nChannels>1) {
			IJ.error("HyperStack Converter", "RGB stacks are limited to one channel");
			return;
		}
		if (nChannels*nSlices*nFrames!=stackSize) {
			IJ.error("HyperStack Converter", "channels x slices x frames <> stack size");
			return;
		}
		imp.setDimensions(nChannels, nSlices, nFrames);
		if (ordering!=CZT && imp.getStack().isVirtual())
			reorderVirtualStack(imp, ordering);
		else
			shuffle(imp, ordering);
		ImagePlus imp2 = imp;
		if (nChannels>1 && imp.getBitDepth()!=24) {
			LUT[] luts = imp.getLuts();
			if (luts!=null && luts.length<nChannels) luts = null;
			imp2 = new CompositeImage(imp, mode+1);
			if (luts!=null)
				((CompositeImage)imp2).setLuts(luts);
		} else if (imp.getClass().getName().indexOf("Image5D")!=-1) {
			imp2 = imp.createImagePlus();
			imp2.setStack(imp.getTitle(), imp.getImageStack());
			imp2.setDimensions(imp.getNChannels(), imp.getNSlices(), imp.getNFrames());
			imp2.getProcessor().resetMinAndMax();
		}
		imp2.setOpenAsHyperStack(true);
		if (imp.getWindow()!=null || imp!=imp2) {
			if (Interpreter.isBatchMode())
				imp2.show();
			else
				new StackWindow(imp2);
		}
		if (imp!=imp2) {
			imp2.setOverlay(imp.getOverlay());
			imp.hide();
			WindowManager.setCurrentWindow(imp2.getWindow());
		}
		if (rgb && splitRGB==true) {
			new CompositeConverter().run(mode==0?"composite":"color");
			return;
		}

		if (IJ.recording() && Recorder.scriptMode()) {
			String order = orders[ordering];
			if (order.equals(orders[0])) { // default order
				Recorder.recordCall("imp2 = HyperStackConverter.toHyperStack(imp, "+nChannels+", "+
					nSlices+", "+nFrames+", \""+modes[mode]+"\");");
			} else {
				Recorder.recordCall("imp2 = HyperStackConverter.toHyperStack(imp, "+nChannels+", "+
					nSlices+", "+nFrames+", \""+order+"\", \""+modes[mode]+"\");");
			}
		}
	}

	/** Changes the dimension order of a 4D or 5D stack from 
		the specified order (CTZ, ZCT, ZTC, TCZ or TZC) to 
		the XYCZT order used by ImageJ. */
	public void shuffle(ImagePlus imp, int order) {
		int nChannels = imp.getNChannels();
		int nSlices = imp.getNSlices();
		int nFrames = imp.getNFrames();
		int first=C, middle=Z, last=T;
		int nFirst=nChannels, nMiddle=nSlices, nLast=nFrames;
		switch (order) {
			case CTZ: first=C; middle=T; last=Z;
				nFirst=nChannels; nMiddle=nFrames; nLast=nSlices;
				break;
			case ZCT: first=Z; middle=C; last=T;
				nFirst=nSlices; nMiddle=nChannels; nLast=nFrames;
				break;
			case ZTC: first=Z; middle=T; last=C;
				nFirst=nSlices; nMiddle=nFrames; nLast=nChannels;
				break;
			case TCZ: first=T; middle=C; last=Z;
				nFirst=nFrames; nMiddle=nChannels; nLast=nSlices;
				break;
			case TZC: first=T; middle=Z; last=C;
				nFirst=nFrames; nMiddle=nSlices; nLast=nChannels;
				break;
		}
		if (order!=CZT) {
			ImageStack stack = imp.getImageStack();
			Object[] images1 = stack.getImageArray();
			Object[] images2 = new Object[images1.length];
			System.arraycopy(images1, 0, images2, 0, images1.length);
			String[] labels1 = stack.getSliceLabels();
			String[] labels2 = new String[labels1.length];
			System.arraycopy(labels1, 0, labels2, 0, labels1.length);
			int[] index = new int[3];
			for (index[2]=0; index[2]<nFrames; ++index[2]) {
				for (index[1]=0; index[1]<nSlices; ++index[1]) {
					for (index[0]=0; index[0]<nChannels; ++index[0]) {
						int dstIndex = index[0] + index[1]*nChannels + index[2]*nChannels*nSlices;
						int srcIndex = index[first] + index[middle]*nFirst + index[last]*nFirst*nMiddle;
						images1[dstIndex] = images2[srcIndex];
						labels1[dstIndex] = labels2[srcIndex];
					}
				}
			}
		}
	}

	void convertHSToStack(ImagePlus imp) {
		if (!(imp.isHyperStack()||imp.isComposite()))
			return;
		ImagePlus imp2 = imp;
		if (imp.isComposite()) {
			ImageStack stack = imp.getStack();
			imp2 = imp.createImagePlus();
			imp2.setStack(imp.getTitle(), stack);
			int[] dim = imp.getDimensions();
			imp2.setDimensions(dim[2], dim[3], dim[4]);
			ImageProcessor ip2 = imp2.getProcessor();
			ip2.setColorModel(ip2.getDefaultColorModel());
		}
		imp2.setOpenAsHyperStack(false);
		if (imp.getWindow()!=null || imp!=imp2)
			new StackWindow(imp2);
		if (imp!=imp2) {
			imp2.setOverlay(imp.getOverlay());
			Calibration cal = imp2.getCalibration();
			if (cal.frameInterval>0)
				imp2.setDimensions(1,1,imp2.getStackSize());
			else
				imp2.setDimensions(1,imp2.getStackSize(),1);
			imp.hide();
		}
		if (IJ.recording() && Recorder.scriptMode())
			Recorder.recordCall("HyperStackConverter.toStack(imp);");
	}

	void newHyperStack() {
		IJ.runMacroFile("ij.jar:HyperStackMaker", "");
	}
	
}

