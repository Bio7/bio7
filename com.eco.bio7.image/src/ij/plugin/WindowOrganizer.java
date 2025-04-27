package ij.plugin;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.frame.ThresholdAdjuster;
import java.awt.*;

/** This class implements the Window menu's "Show All", "Main Window", "Cascade" and "Tile" commands. */
public class WindowOrganizer implements PlugIn {

	private static final int XSTART=4, YSTART=94, XOFFSET=8, YOFFSET=24,MAXSTEP=200,GAP=2;
	private int titlebarHeight = IJ.isMacintosh()?40:20;

	public void run(String arg) {
		if (arg.equals("imagej"))
			{showImageJ(); return;}
		int[] wList = WindowManager.getIDList();
		if (arg.equals("show"))
			{showAll(wList); return;}
		if (wList==null) {
			IJ.noImage();
			return;
		}
		if (arg.equals("tile"))
			tileWindows(wList);
		else
			cascadeWindows(wList);
	}
	
	void tileWindows(int[] wList) {
		Rectangle screen = GUI.getMaxWindowBounds(IJ.getInstance());
		int minWidth = Integer.MAX_VALUE;
		int minHeight = Integer.MAX_VALUE;
		boolean allSameSize = true;
		int width=0, height=0;
		double totalWidth = 0;
		double totalHeight = 0;
		for (int i=0; i<wList.length; i++) {
			ImageWindow win = getWindow(wList[i]);
			if (win==null)
				continue;
			if (win instanceof PlotWindow && !((PlotWindow)win).getPlot().isFrozen()) {
				IJ.error("Tile", "Unfrozen plot windows cannot be tiled.");
				return;
			}
			Dimension d = win.getSize();
			int w = d.width;
			int h = d.height + titlebarHeight;
			if (i==0) {
				width = w;
				height = h;
			}
			if (w!=width || h!=height)
				allSameSize = false;
			if (w<minWidth)
				minWidth = w;
			if (h<minHeight)
				minHeight = h;
			totalWidth += w;
			totalHeight += h;
		}
		int nPics = wList.length;
		double averageWidth = totalWidth/nPics;
		double averageHeight = totalHeight/nPics;
		int tileWidth = (int)averageWidth;
		int tileHeight = (int)averageHeight;
 		int hspace = screen.width - 2 * GAP;
		if (tileWidth>hspace)
			tileWidth = hspace;
		int vspace = screen.height - YSTART;
		if (tileHeight>vspace)
			tileHeight = vspace;
		int hloc, vloc;
		boolean theyFit;
		do {
			hloc = XSTART;
			vloc = YSTART;
			theyFit = true;
			int i = 0;
			do {
				i++;
				if (hloc+tileWidth>screen.width) {
					hloc = XSTART;
					vloc = vloc + tileHeight;
					if (vloc+tileHeight> screen.height)
						theyFit = false;
				}
				hloc = hloc + tileWidth + GAP;
			} while (theyFit && (i<nPics));
			if (!theyFit) {
				tileWidth = (int)(tileWidth*0.98 +0.5);
				tileHeight = (int)(tileHeight*0.98+0.5);
			}
		} while (!theyFit);
		int nColumns = (screen.width-XSTART)/(tileWidth+GAP);
		if (nColumns<=0)
			nColumns = 1;
		int nRows = nPics/nColumns;
		if ((nPics%nColumns)!=0)
			nRows++;
		hloc = XSTART;
		vloc = YSTART;
		
		for (int i=0; i<nPics; i++) {
			if (hloc+tileWidth>screen.width) {
				hloc = XSTART;
				vloc = vloc + tileHeight;
			}
			ImageWindow win = getWindow(wList[i]);
			if (win!=null) {
				win.setExtendedState(Frame.NORMAL);
				win.setLocation(hloc + screen.x, vloc + screen.y);
				ImageCanvas canvas = win.getCanvas();
				while (win.getSize().width*0.85>=tileWidth && canvas.getMagnification()>0.03125)
					canvas.zoomOut(0, 0);
				win.toFront();
				ImagePlus imp = win.getImagePlus();
				if (imp!=null) imp.setIJMenuBar(i==nPics-1);
			}
			hloc += tileWidth + GAP;
		}
	}

	ImageWindow getWindow(int id) {
		ImageWindow win = null;
		ImagePlus imp = WindowManager.getImage(id);
		if (imp!=null)
			win = imp.getWindow();
		return win;
	}		
			
	void cascadeWindows(int[] wList) {
		Rectangle screen = GUI.getMaxWindowBounds(IJ.getInstance());
		int x = XSTART;
		int y = YSTART;
		int xstep = 0;
		int xstart = XSTART;
		for (int i=0; i<wList.length; i++) {
			ImageWindow win = getWindow(wList[i]);
			if (win==null)
				continue;
			win.setExtendedState(Frame.NORMAL);
			Dimension d = win.getSize();
			if (i==0) {
				xstep = (int)(d.width*0.8);
				if (xstep>MAXSTEP)
					xstep = MAXSTEP;
			}
			if (y+d.height*0.67>screen.height) {
				xstart += xstep;
				if (xstart+d.width*0.67>screen.width)
					xstart = XSTART+XOFFSET;
				x = xstart;
				y = YSTART;
			}
			win.setLocation(x + screen.x, y + screen.y);
			win.toFront();
				x += XOFFSET;
			y += YOFFSET;
			ImagePlus imp = win.getImagePlus();
			if (imp!=null) imp.setIJMenuBar(i==wList.length-1);
		}
	}
	
	void showImageJ() {
		ImageJ ij = IJ.getInstance();
		if (ij!=null)
			ij.toFront();
	}

	void showAll(int[] wList) {
		if (wList!=null) {
			for (int i=0; i<wList.length; i++) {
				ImageWindow win = getWindow(wList[i]);
				if (win!=null)
					WindowManager.toFront(win);
				
			}
		}
		Window[] windows = WindowManager.getAllNonImageWindows();
		if (windows!=null) {
			for (int i=0; i<windows.length; i++)
					WindowManager.toFront(windows[i]);
		}
		IJ.getInstance().toFront();
	}

}
