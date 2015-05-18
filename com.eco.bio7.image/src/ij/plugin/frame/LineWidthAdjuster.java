package ij.plugin.frame;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.frame.Recorder;
import ij.util.Tools;

/** Adjusts the width of line selections.  */
public class LineWidthAdjuster extends PlugInFrame implements PlugIn,
	Runnable, AdjustmentListener, ItemListener, DocumentListener {

	public static final String LOC_KEY = "line.loc";
	int sliderRange = 300;
	JScrollBar slider;
	int value;
	boolean setText;
	static LineWidthAdjuster instance; 
	Thread thread;
	boolean done;
	JTextField tf;
	JCheckBox checkbox;

	public LineWidthAdjuster() {
		super("Line Width");
		if (instance!=null) {
			WindowManager.toFront(instance);
			return;
		}		
		WindowManager.addWindow(this);
		instance = this;
		slider = new JScrollBar(JScrollBar.HORIZONTAL, Line.getWidth(), 1, 1, sliderRange+1);
		slider.setFocusable(false); // prevents blinking on Windows
				
		JPanel panel = new JPanel();
		int margin = IJ.isMacOSX()?5:0;
		GridBagLayout grid = new GridBagLayout();
		GridBagConstraints c  = new GridBagConstraints();
		panel.setLayout(grid);
		c.gridx = 0; c.gridy = 0;
		c.gridwidth = 1;
		c.ipadx = 100;
		c.insets = new Insets(margin, 15, margin, 5);
		c.anchor = GridBagConstraints.CENTER;
		grid.setConstraints(slider, c);
		panel.add(slider);
		c.ipadx = 0;  // reset
		c.gridx = 1;
		c.insets = new Insets(margin, 5, margin, 15);
		tf = new JTextField(""+Line.getWidth(), 4);
		/*Changed for Bio7! Add a document listener for Swing!*/
		tf.getDocument().addDocumentListener(this);
		grid.setConstraints(tf, c);
    	panel.add(tf);
		
		c.gridx = 2;
		c.insets = new Insets(margin, 25, margin, 5);
		checkbox = new JCheckBox("Spline Fit", isSplineFit());
		checkbox.addItemListener(this);
		panel.add(checkbox);
		
		add(panel, BorderLayout.CENTER);
		slider.addAdjustmentListener(this);
		slider.setUnitIncrement(1);
		
		pack();
		Point loc = Prefs.getLocation(LOC_KEY);
		if (loc!=null)
			setLocation(loc);
		else
			GUI.center(this);
		setResizable(false);
		show();
		thread = new Thread(this, "LineWidthAdjuster");
		thread.start();
		setup();
		addKeyListener(IJ.getInstance());
	}
	
	public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
		value = slider.getValue();
		setText = true;
		notify();
	}
   /*Changed for Bio7!*/
    public  synchronized void textValueChanged(TextEvent e) {
        /*int width = (int)Tools.parseDouble(tf.getText(), -1);
		//IJ.log(""+width);
        if (width==-1) return;
        if (width<0) width=1;
        if (width!=Line.getWidth()) {
			slider.setValue(width);
        	value = width;
        	notify();
        }*/
    }
	void setup() {
	}
	
	// Separate thread that does the potentially time-consuming processing 
	public void run() {
		while (!done) {
			synchronized(this) {
				try {wait();}
				catch(InterruptedException e) {}
				if (done) return;
				Line.setWidth(value);
				if (setText) tf.setText(""+value);
				setText = false;
				updateRoi();
			}
		}
	}
	
	private static void updateRoi() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp!=null) {
			Roi roi = imp.getRoi();
			if (roi!=null && roi.isLine()) {
				roi.updateWideLine(Line.getWidth());
				imp.draw();
				return;
			}
		}
		if (Roi.previousRoi==null) return;
		int id = Roi.previousRoi.getImageID();
		if (id>=0) return;
		imp = WindowManager.getImage(id);
		if (imp==null) return;
		Roi roi = imp.getRoi();
		if (roi!=null && roi.isLine()) {
			roi.updateWideLine(Line.getWidth());
			imp.draw();
		}
	}
	
	boolean isSplineFit() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null) return false;
		Roi roi = imp.getRoi();
		if (roi==null) return false;
		if (!(roi instanceof PolygonRoi)) return false;
		return ((PolygonRoi)roi).isSplineFit();
	}

    /** Overrides close() in PlugInFrame. */
	public void close() {
		super.close();
		instance = null;
		done = true;
		Prefs.saveLocation(LOC_KEY, getLocation());
		synchronized(this) {notify();}
	}

    public void windowActivated(WindowEvent e) {
    	super.windowActivated(e);
    	checkbox.setSelected(isSplineFit());
	}

	public void itemStateChanged(ItemEvent e) {
		boolean selected = e.getStateChange()==ItemEvent.SELECTED;
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null)
			{checkbox.setSelected(false); return;};
		Roi roi = imp.getRoi();
		if (roi==null || !(roi instanceof PolygonRoi))
			{checkbox.setSelected(false); return;};
		int type = roi.getType();
		if (type==Roi.FREEROI || type==Roi.FREELINE)
			{checkbox.setSelected(false); return;};;
		PolygonRoi poly = (PolygonRoi)roi;
		boolean splineFit = poly.isSplineFit();
		if (selected && !splineFit)
			{poly.fitSpline(); imp.draw();}
		else if (!selected && splineFit)
			{poly.removeSplineFit(); imp.draw();}
	}
	
	public static void update() {
		if (instance==null) return;
		instance.checkbox.setSelected(instance.isSplineFit());
		int sliderWidth = instance.slider.getValue();
		int lineWidth = Line.getWidth();
		if (lineWidth!=sliderWidth && lineWidth<=200) {
			instance.slider.setValue(lineWidth);
			instance.tf.setText(""+lineWidth);
		}
	}
   /*Changed for Bio7! Implemented a Swing Document Listener!*/
	@Override
	public void changedUpdate(DocumentEvent arg0) {
		 int width = (int)Tools.parseDouble(tf.getText(), -1);
			//IJ.log(""+width);
	        if (width==-1) return;
	        if (width<0) width=1;
	        if (width!=Line.getWidth()) {
				slider.setValue(width);
	        	value = width;
	        	notify();
	        }
		
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		// TODO Auto-generated method stub
		
	}
	
} 

