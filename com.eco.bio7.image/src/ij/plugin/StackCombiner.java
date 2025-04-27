package ij.plugin;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.FileInfo;
import java.awt.*;

/**
	This plugin implements the Image/Stacks/Combine command.
	It combines two stacks (w1xh1xd1 and w2xh2xd2) to create a new
	w1+w2 x max(h1,h2) x max(d1,d2) stack. For example, a 256x256x40
	and a 256x256x30 stack would be combined into one 512x256x40 stack.
	If "Vertical" is checked, create a new max(w1+w2) x (h1+h2) x max(d1,d2) stack.
	Unused areas in the combined stack are filled with the background color.
*/
public class StackCombiner implements PlugIn {
		ImagePlus imp1;
		ImagePlus imp2;
		static boolean vertical;

	public void run(String arg) {
		if (!showDialog())
			return;
		if (imp1.getBitDepth()!=imp2.getBitDepth()) {
			error();
			return;
		}
		imp1.setDisplayMode(IJ.COLOR);
		imp2.setDisplayMode(IJ.COLOR);
		if (LUT.LutsDiffer(imp1,imp2)) {
			RGBStackConverter.convertToRGB(imp1);
			RGBStackConverter.convertToRGB(imp2);
		}
		int[] dim1 = imp1.getDimensions();
		int[] dim2 = imp2.getDimensions();
		boolean isHyperStack1 = imp1.isHyperStack() || imp1.isComposite();
		boolean isHyperStack2 = imp2.isHyperStack() || imp2.isComposite();
		if (imp1.isHyperStack() || imp2.isHyperStack()) {
			if (dim1[2]!=dim2[2] || dim1[3]!=dim2[3] || dim1[4]!=dim2[4]) {
				IJ.error("StackCombiner", "Hyperstacks must have identical CZT dimensions");	
				return;
			}
		}
		LUT[] luts = null;
		if (imp1.isComposite())
			luts = imp1.getLuts();
		ImageStack stack1 = imp1.getStack();
		ImageStack stack2 = imp2.getStack();
		ImageStack stack3 = vertical?combineVertically(stack1, stack2):combineHorizontally(stack1, stack2);
		imp1.changes = false;
		imp1.close();
		imp2.changes = false;
		imp2.close();
		ImagePlus imp3 = imp1.createImagePlus();
		imp3.setStack(stack3);
		if (isHyperStack1)
			imp3.setDimensions(dim1[2],dim1[3],dim1[4]);
		if (imp1.isComposite()) {
			imp3 = new CompositeImage(imp3, imp1.getCompositeMode());
			imp3.setDimensions(dim1[2],dim1[3],dim1[4]);
			if (luts!=null)
				((CompositeImage)imp3).setLuts(luts);
			((CompositeImage)imp3).resetDisplayRanges();
		}
		imp3.setTitle("Combined Stacks");
		imp3.show();
	}
		
	public ImageStack combineHorizontally(ImageStack stack1, ImageStack stack2) {
		int d1 = stack1.getSize();
		int d2 = stack2.getSize();
		int d3 = Math.max(d1, d2);
		int w1 = stack1.getWidth();
		int h1 = stack1.getHeight();
 		int w2 = stack2.getWidth();
		int h2 = stack2.getHeight();
		int w3 = w1 + w2;
		int h3 = Math.max(h1, h2);
		ImageStack stack3 = new ImageStack(w3, h3, stack1.getColorModel());
		ImageProcessor ip = stack1.getProcessor(1);
		ImageProcessor ip1, ip2, ip3;
		Color background = Toolbar.getBackgroundColor();
 		for (int i=1; i<=d3; i++) {
 			IJ.showProgress((double)i/d3);
 			ip3 = ip.createProcessor(w3, h3);
 			if (h1!=h2) {
 				ip3.setColor(background);
 				ip3.fill();
 			}
 			if  (i<=d1) {
				ip3.insert(stack1.getProcessor(1),0,0);
				if (stack2!=stack1)
					stack1.deleteSlice(1);
			}
			if  (i<=d2) {
				ip3.insert(stack2.getProcessor(1),w1,0);
				stack2.deleteSlice(1);
			}
			stack3.addSlice(null, ip3);
		}
		return stack3;
	}
	
	public ImageStack combineVertically(ImageStack stack1, ImageStack stack2) {
		int d1 = stack1.getSize();
		int d2 = stack2.getSize();
		int d3 = Math.max(d1, d2);
		int w1 = stack1.getWidth();
		int h1 = stack1.getHeight();
 		int w2 = stack2.getWidth();
		int h2 = stack2.getHeight();
		int w3 = Math.max(w1, w2);
		int h3 = h1 + h2;
		ImageStack stack3 = new ImageStack(w3, h3, stack1.getColorModel());
		ImageProcessor ip = stack1.getProcessor(1);
		ImageProcessor ip1, ip2, ip3;
 		Color background = Toolbar.getBackgroundColor();
 		for (int i=1; i<=d3; i++) {
 			IJ.showProgress((double)i/d3);
 			ip3 = ip.createProcessor(w3, h3);
			if (w1!=w2) {
 				ip3.setColor(background);
 				ip3.fill();
			}
			if  (i<=d1) {
				ip3.insert(stack1.getProcessor(1),0,0);
				if (stack2!=stack1)
					stack1.deleteSlice(1);
			}
			if  (i<=d2) {
				ip3.insert(stack2.getProcessor(1),0,h1);
				stack2.deleteSlice(1);
			}
		stack3.addSlice(null, ip3);
		}
		return stack3;
	}

	boolean showDialog() {
		int[] wList = WindowManager.getIDList();
		if (wList==null || wList.length<2) {
			error();
			return false;
		}
		String[] titles = new String[wList.length];
		for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			titles[i] = imp!=null?imp.getTitle():"";
		}
		GenericDialog gd = new GenericDialog("Combiner");
		gd.addChoice("Stack1:", titles, titles[0]);
		gd.addChoice("Stack2:", titles, titles[1]);
		gd.addCheckbox("Combine vertically", false);
        gd.addHelp(IJ.URL2+"/docs/menus/image.html#combine");
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		int[] index = new int[3];
		int index1 = gd.getNextChoiceIndex();
		int index2 = gd.getNextChoiceIndex();
		imp1 = WindowManager.getImage(wList[index1]);
		imp2 = WindowManager.getImage(wList[index2]);
		vertical = gd.getNextBoolean();
		return true;
	}

	
	void error() {
		IJ.error("StackCombiner", "This command requires two stacks\n"
			+"that are the same data type.");
	}

}

