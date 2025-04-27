package ij.plugin.filter;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.Calibration;
import java.awt.*;
import java.awt.image.*;

import com.eco.bio7.image.CanvasView;

/** Implements the Flip and Rotate commands in the Image/Transform submenu. */
public class Transformer implements PlugInFilter {	
	private ImagePlus imp;
	private String arg;
	private Overlay overlay;
	private boolean firstSlice = true;

	public int setup(String arg, ImagePlus imp) {
		this.arg = arg;
		this.imp = imp;
		if (imp!=null)
			overlay = imp.getOverlay();
		if (arg.equals("fliph") || arg.equals("flipv"))
			return IJ.setupDialog(imp, DOES_ALL+NO_UNDO);
		else
			return DOES_ALL+NO_UNDO+NO_CHANGES;
	}

	public void run(ImageProcessor ip) {
		Calibration cal = imp.getCalibration();
		boolean transformOrigin = cal.xOrigin!=0 || cal.yOrigin!=0;
		if (arg.equals("fliph")) {
			ip.flipHorizontal();
			Rectangle r = ip.getRoi();
			if (transformOrigin && r.x==0 && r.y==0 && r.width==ip.getWidth() && r.height==ip.getHeight())
				cal.xOrigin = imp.getWidth()-1 - cal.xOrigin;
			return;
		}
		if (arg.equals("flipv")) {
			ip.flipVertical();
			Rectangle r = ip.getRoi();
			if (transformOrigin && r.x==0 && r.y==0 && r.width==ip.getWidth() && r.height==ip.getHeight())
	    		cal.yOrigin = imp.getHeight()-1 - cal.yOrigin;
			return;
		}
		if (arg.equals("right") || arg.equals("left")) {
	    	StackProcessor sp = new StackProcessor(imp.getStack(), ip);
	    	ImageStack s2 = null;
			if (arg.equals("right")) {
	    		s2 = sp.rotateRight();
				rotateOverlay(90);
	    		if (transformOrigin) {
	    			double xOrigin = imp.getHeight()-1 - cal.yOrigin;
	    			double yOrigin = cal.xOrigin;
	    			cal.xOrigin = xOrigin;
	    			cal.yOrigin = yOrigin;
	    		}
	    	} else {
	    		s2 = sp.rotateLeft();
				rotateOverlay(-90);
	    		if (transformOrigin) {
	    			double xOrigin = cal.yOrigin;
	    			double yOrigin = imp.getWidth()-1 - cal.xOrigin;
	    			cal.xOrigin = xOrigin;
	    			cal.yOrigin = yOrigin;
	    		}
	    	}
	    	imp.setStack(null, s2);
	    	double pixelWidth = cal.pixelWidth;
	    	cal.pixelWidth = cal.pixelHeight;
	    	cal.pixelHeight = pixelWidth;
			if (!cal.getXUnit().equals(cal.getYUnit())) {
				String xUnit = cal.getXUnit();
				cal.setXUnit(cal.getYUnit());
				cal.setYUnit(xUnit);
			}
			/*Changed for Bio7!*/
			CanvasView.getCurrent().doLayout();
			return;
		}
	}
	
	private void rotateOverlay(int angle) {
		if (overlay!=null && firstSlice) {
			double xcenter = imp.getWidth()/2.0;
			double ycenter = imp.getHeight()/2.0;
			double diff1 = xcenter-ycenter;
			double diff2 = ycenter-xcenter;
			Overlay overlay2 = overlay.rotate(angle,xcenter,ycenter);
			overlay2.translate(diff2,diff1);
			imp.setOverlay(overlay2);
		}
		firstSlice = false;
	}
	
}
