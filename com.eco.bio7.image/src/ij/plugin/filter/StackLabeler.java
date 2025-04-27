package ij.plugin.filter;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.util.Tools;
import ij.measure.Measurements;
import java.awt.*;


/** This plugin implements the Image/Stacks/Label command. */
public class StackLabeler implements ExtendedPlugInFilter, DialogListener {
	private static final String[] formats = {"0", "0000", "00:00", "00:00:00", "Text","Label"};
	private static final int NUMBER=0, ZERO_PADDED_NUMBER=1, MIN_SEC=2, HOUR_MIN_SEC=3, TEXT=4, LABEL=5;
	private static int format = (int)Prefs.get("label.format", NUMBER);
	private int flags = DOES_ALL;
	private ImagePlus imp;
	private static int x = 5;
	private static int y = 20;
	private static int fontSize = 18;
	private int maxWidth;
	private Font font;
	private static double start = 0;
	private static double interval = 1;
	private static String text = "";
	private static int decimalPlaces = 0;
	private static boolean useOverlay;
	private static boolean useTextToolFont;
	private int fieldWidth;
	private Color color;
	private int firstFrame, lastFrame, defaultLastFrame;
	private Overlay overlay;
	private Overlay baseOverlay;
	private boolean previewing; 
	private boolean virtualStack; 
	private int yoffset;

	public int setup(String arg, ImagePlus imp) {
		if (imp!=null) {
			virtualStack = imp.getStack().isVirtual();
			if (virtualStack) useOverlay = true;
			baseOverlay = imp.getOverlay();
			flags += virtualStack?0:DOES_STACKS;
			firstFrame=1; lastFrame=defaultLastFrame=imp.getStackSize();
		}
		this.imp = imp;
		return flags;
	}

    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
		ImageProcessor ip = imp.getProcessor();
		Rectangle roi = ip.getRoi();
		if (roi.width<ip.getWidth() || roi.height<ip.getHeight()) {
			x = roi.x;
			y = roi.y+roi.height;
			fontSize = (int) ((roi.height - 1.10526)/0.934211);	
			if (fontSize<7) fontSize = 7;
			if (fontSize>80) fontSize = 80;
		}
		if (IJ.macroRunning()) {
			format = NUMBER;
			decimalPlaces = 0;
		    interval=1;
			text = "";
			start = 0;
			useOverlay = false;
			useTextToolFont = false;
			String options = Macro.getOptions();
			if (options!=null) {
				if (options.indexOf("interval=0")!=-1 && options.indexOf("format=")==-1)
					format = TEXT;
				if (options.indexOf(" slice=")!=-1) {
					options = options.replaceAll(" slice=", " range=");
					Macro.setOptions(options);
				}
			}
		}
		if (format<0||format>LABEL) format = NUMBER;
		int defaultLastFrame = imp.getStackSize();
		if (imp.isHyperStack()) {
			if (imp.getNFrames()>1)
				defaultLastFrame = imp.getNFrames();
			else if (imp.getNSlices()>1)
				defaultLastFrame = imp.getNSlices();
		}
		GenericDialog gd = new GenericDialog("Label Stacks");
		gd.setInsets(2, 5, 0);
		gd.addChoice("Format:", formats, formats[format]);
		gd.addStringField("Starting value:", IJ.d2s(start,decimalPlaces));
		gd.addStringField("Interval:", ""+IJ.d2s(interval,decimalPlaces));
		gd.addNumericField("X location:", x, 0);
		gd.addNumericField("Y location:", y, 0);
		gd.addNumericField("Font size:", fontSize, 0);
		gd.addStringField("Text:", text, 10);
        addRange(gd, "Range:", 1, defaultLastFrame);
		gd.setInsets(10,20,0);
        gd.addCheckbox(" Use overlay", useOverlay);
        gd.addCheckbox(" Use_text tool font", useTextToolFont);
        gd.addPreviewCheckbox(pfr);
        gd.addHelp(IJ.URL2+"/docs/menus/image.html#label");
        gd.addDialogListener(this);
        previewing = true;
		gd.showDialog();
		previewing = false;
		if (gd.wasCanceled())
        	return DONE;
        else
        	return flags;
    }

	void addRange(GenericDialog gd, String label, int start, int end) {
		gd.addStringField(label, start+"-"+end);
	}
	
	double[] getRange(GenericDialog gd, int start, int end) {
		String[] range = Tools.split(gd.getNextString(), " -");
		double d1 = Tools.parseDouble(range[0]);
		double d2 = range.length==2?Tools.parseDouble(range[1]):Double.NaN;
		double[] result = new double[2];
		result[0] = Double.isNaN(d1)?1:(int)d1;
		result[1] = Double.isNaN(d2)?end:(int)d2;
		if (result[0]<start) result[0] = start;
		if (result[1]>end) result[1] = end;
		if (result[0]>result[1]) {
			result[0] = start;
			result[1] = end;
		}
		return result;
	}

	public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
		format = gd.getNextChoiceIndex();
		start = Tools.parseDouble(gd.getNextString());
 		String str = gd.getNextString();
 		interval = Tools.parseDouble(str);
		x = (int)gd.getNextNumber();
		y = (int)gd.getNextNumber();
		fontSize = (int)gd.getNextNumber();
		text = gd.getNextString();
		double[] range = getRange(gd, 1, defaultLastFrame);
		useOverlay = gd.getNextBoolean();
		useTextToolFont = gd.getNextBoolean();
		if (virtualStack) useOverlay = true;
		firstFrame=(int)range[0]; lastFrame=(int)range[1];
		int index = str.indexOf(".");
		if (index!=-1)
			decimalPlaces = str.length()-index-1;
		else
			decimalPlaces = 0;
		if (gd.invalidNumber()) return false;
		if (useTextToolFont)
			font = new Font(TextRoi.getDefaultFontName(), TextRoi.getDefaultFontStyle(), fontSize);
		else
			font = new Font("SansSerif", Font.PLAIN, fontSize);
		if (y<fontSize) y = fontSize+5;
		ImageProcessor ip = imp.getProcessor();
		ip.setFont(font);
		int size = defaultLastFrame;
		maxWidth = ip.getStringWidth(getString(size, interval, format));
		fieldWidth = 1;
		if (size>=10) fieldWidth = 2;
		if (size>=100) fieldWidth = 3;
		if (size>=1000) fieldWidth = 4;
		if (size>=10000) fieldWidth = 5;
		Prefs.set("label.format", format);
        return true;
    }
	
	public void run(ImageProcessor ip) {
		int image = ip.getSliceNumber();
		int n = image - 1;
		if (imp.isHyperStack()) n = updateIndex(n);
		if (virtualStack) {
			int nSlices = imp.getStackSize();
			if (previewing) nSlices = 1;
			for (int i=1; i<=nSlices; i++) {
				image=i; n=i-1;
				if (imp.isHyperStack()) n = updateIndex(n);
				drawLabel(ip, image, n);
			}
		} else {
			if (previewing && overlay!=null) {
				imp.setOverlay(baseOverlay);
				overlay = null;
			}
			drawLabel(ip, image, n);
		}
	}
	
	int updateIndex(int n) {
		if (imp.getNFrames()>1)
			return (int)(n*((double)(imp.getNFrames())/imp.getStackSize()));
		else if (imp.getNSlices()>1)
			return (int)(n*((double)(imp.getNSlices())/imp.getStackSize()));
		else
			return n;
	}
	
	void drawLabel(ImageProcessor ip, int image, int n) {
		String s = getString(n, interval, format);
		ip.setFont(font);
		int textWidth = ip.getStringWidth(s);
		if (color==null) {
			color = Toolbar.getForegroundColor();
			if ((color.getRGB()&0xffffff)==0) {
				ip.setRoi(x, y-fontSize, maxWidth+textWidth, fontSize);
				double mean = ImageStatistics.getStatistics(ip, Measurements.MEAN, null).mean;
				if (mean<50.0 && !ip.isInvertedLut()) color=Color.white;
				ip.resetRoi();
			}
		}
		int frame = image;
		int[] pos = new int[]{0, 0, 0};
		if (imp.isHyperStack()) {
			pos = imp.convertIndexToPosition(image);
			if (imp.getNFrames()>1)
				frame = pos[2];
			else if (imp.getNSlices()>1)
				frame = pos[1];
		}
		if (useOverlay) {
			if (image==1 || previewing) {
				overlay = new Overlay();
				if (baseOverlay!=null) {
					for (int i=0; i<baseOverlay.size(); i++)
						overlay.add(baseOverlay.get(i));
				}
				Roi roi = imp.getRoi();
				Rectangle r = roi!=null?roi.getBounds():null;
				yoffset = r!=null?r.height:fontSize;
			}
			if (frame>=firstFrame&&frame<=lastFrame) {
				int xloc = format==LABEL?x:x+maxWidth-textWidth;
				Roi roi = new TextRoi(xloc, y-yoffset, s, font);
				roi.setStrokeColor(color);
				roi.setNonScalable(true);
				if (imp.isHyperStack())
					roi.setPosition(pos[0], pos[1], pos[2]);
				else
					roi.setPosition(image);
				overlay.add(roi);
			}
			if (image==imp.getStackSize()||previewing)
				imp.setOverlay(overlay);
		} else if (frame>=firstFrame&&frame<=lastFrame) {
			ip.setColor(color); 
			ip.setAntialiasedText(fontSize>=18);
			int xloc = format==LABEL?x:x+maxWidth-textWidth;
			ip.moveTo(xloc, y);
			ip.drawString(s);
		}
	}
	
	String getString(int index, double interval, int format) {
		double time = start + (index+1-firstFrame)*interval;
		int itime = (int)Math.floor(time);
		int sign = 1;
		if (itime < 0) sign = -1;
		itime = itime*sign;
		String str = "";
		switch (format) {
			case NUMBER: str=IJ.d2s(time, decimalPlaces)+" "+text; break;
			case ZERO_PADDED_NUMBER:
				if (decimalPlaces==0)
					str=zeroFill((int)time); 
				else
					str=IJ.d2s(time, decimalPlaces);
				str = text +" " + str;
				break;
			case MIN_SEC:
				str=pad((int)Math.floor((itime/60)%60))+":"+pad(itime%60)+" "+text;
				if (sign == -1) str = "-"+str;
				break;
			case HOUR_MIN_SEC:
				str=pad((int)Math.floor(itime/3600))+":"+pad((int)Math.floor((itime/60)%60))+":"+pad(itime%60)+" "+text;
				if (sign == -1) str = "-"+str;
				break;
			case TEXT: 
				str=text; 
				break;
			case LABEL:
				if (0<=index && index<imp.getStackSize()) {
					str = imp.getStack().getShortSliceLabel(index+1);
					str = str==null?"null slice label ("+(index+1)+")":str;
				} else
					str="void";
				break;
		}
		return str;
	}

	String pad(int n) {
		String str = ""+n;
		if (str.length()==1) str="0"+str;
		return str;
	}
	
	String  zeroFill(int n) {
		String str = ""+n;
		while (str.length()<fieldWidth)
			str = "0" + str;
		return str;
	}
		
	public void setNPasses (int nPasses) {}

}
