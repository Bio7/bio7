package ij.plugin.frame;
import java.awt.*;
import java.awt.event.*;

import javax.swing.JFrame;

import com.eco.bio7.image.Util;

import ij.*;
import ij.plugin.*;

/**  This is a closeable window that plugins can extend. */
public class PlugInFrame extends JFrame implements PlugIn, WindowListener, FocusListener {

	String title;
	
	public PlugInFrame(String title) {
		super(title);
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		this.title = title;
		ImageJ ij = IJ.getInstance();
		addWindowListener(this);
 		addFocusListener(this);
 		/*Changed for Bio7!*/
		//if (IJ.isLinux())
			setBackground(ImageJ.backgroundColor);
		if (ij!=null && !IJ.isMacOSX()) {
			Image img = ij.getIconImage();
			if (img!=null)
				try {setIconImage(img);} catch (Exception e) {}
		}
		/*
		 * Changed for Bio7! AWT must be colored per component. All JPanels will get the
		 * SWT background from the definition of the CanvasView (UIManager)!
		 */

		if (Util.isThemeBlack()) {
			if (Util.getOS().equals("Linux")||Util.getOS().equals("Mac")) {
			setBackground(Util.getSWTBackgroundToAWT());
			setForeground(Util.getSWTForegroundToAWT());

			}
		}
	}
	
	public void run(String arg) {
	}
	
    public void windowClosing(WindowEvent e) {
    	if (e.getSource()==this) {
    		close();
    		if (IJ.recording())
    			Recorder.record("run", "Close");
    	}
    }
    
    /** Closes this window. */
    public void close() {
		//setVisible(false);
		dispose();
		WindowManager.removeWindow(this);
    }

    public void windowActivated(WindowEvent e) {
		WindowManager.setWindow(this);
	}

	public void focusGained(FocusEvent e) {
		WindowManager.setWindow(this);
	}

    public void windowOpened(WindowEvent e) {}
    public void windowClosed(WindowEvent e) {}
    public void windowIconified(WindowEvent e) {}
    public void windowDeiconified(WindowEvent e) {}
    public void windowDeactivated(WindowEvent e) {}
	public void focusLost(FocusEvent e) {}
}
