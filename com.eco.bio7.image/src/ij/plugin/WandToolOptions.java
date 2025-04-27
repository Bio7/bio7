package ij.plugin;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.io.*;
import ij.plugin.filter.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

/** This plugin implements the Edit/Options/Wand Tool command. */
public class WandToolOptions implements PlugIn, DialogListener {
	private static final String[] modes = {"Legacy", "4-connected", "8-connected"};
	private static String mode = modes[0];
	private static double tolerance;
	private ImagePlus imp;
	private boolean showCheckbox;
	private static int startx, starty;
	private static int ID;

 	public void run(String arg) {
 		imp = WindowManager.getCurrentImage();
 		Roi roi = imp!=null?imp.getRoi():null;
 		boolean selection = roi!=null && (roi.getType()==Roi.TRACED_ROI||roi.getType()==Roi.POLYGON);
 		if (imp==null || (ID!=0&&imp.getID()!=ID) || !selection)
 			startx = starty = 0;
 		ID = imp!=null?imp.getID():0;
 		double sliderMax = 255;
 		int depth = imp!=null?imp.getBitDepth():0;
 		if (depth==16 || depth==32) {
 			sliderMax = imp.getProcessor().getMax();
 			if (depth==32) sliderMax+=0.0000000001;
 		}
 		showCheckbox = imp!=null && depth!=24 && WindowManager.getFrame("Threshold")==null && !imp.isThreshold();
		GenericDialog gd = new GenericDialog("Wand Tool");
		gd.addSlider("Tolerance: ", 0, sliderMax, tolerance);
		gd.addChoice("Mode:", modes, mode);
		if (showCheckbox)
			gd.addCheckbox("Enable Thresholding", false);
		gd.addCheckbox("Smooth if thresholded", Prefs.smoothWand);
		if (showCheckbox) {
			gd.setInsets(2,0,0);
			gd.addMessage("Thresholded objects are traced and \"Tolerance\"\nis ignored when thresholding is enabled.");
		}
		gd.addDialogListener(this); 
		gd.showDialog();
	}
	
	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		if (gd.wasCanceled())
			return false;
		tolerance = gd.getNextNumber();
		mode = gd.getNextChoice();
		if (showCheckbox) {
			if (gd.getNextBoolean()) {
				imp.deleteRoi();
				IJ.run("Threshold...");
			}
		}
		Prefs.smoothWand = gd.getNextBoolean();
		if (startx>0||starty>0)
			IJ.doWand(startx, starty, tolerance, mode+(Prefs.smoothWand?" smooth":""));
		return true;
	}

	public static String getMode() {
		return mode;
	}

	public static double getTolerance() {
		return tolerance;
	}
	
	public static final void setStart(int x, int y) {
		startx = x;
		starty = y;
	}

}
