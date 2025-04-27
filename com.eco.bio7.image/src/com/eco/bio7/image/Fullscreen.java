package com.eco.bio7.image;

import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.Plot;
import ij.gui.PlotWindow;

public class Fullscreen extends JFrame {

	private static final long serialVersionUID = 1L;
	private boolean fullscreen = false;
	private boolean displayChanged = false;
	private GraphicsEnvironment graphicEnvironment = null;
	private GraphicsDevice graphicDevice = null;
	private DisplayMode currentMode, oldMode;
	private JPanel panel;
	private static int refreshRate = 60;
	private static Fullscreen screen;
	private static int bit = 32;
	private static int monitorHeight = 768;
	private static int monitorWidth = 1024;
	private Dimension temPlotSize;

	public Fullscreen(JPanel panel) {
		this.panel = panel;
		screen = this;
		panel.requestFocus();
		graphicEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		graphicDevice = graphicEnvironment.getDefaultScreenDevice();
		oldMode = graphicDevice.getDisplayMode();
		currentMode = oldMode;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				init();

			}
		});
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {

				exit();

			}
		});
		add(panel);

		// this.pack();

	}

	public boolean init() {
		temPlotSize = CanvasView.getCurrent().getSize();
		setUndecorated(true);
		if (graphicDevice.isFullScreenSupported()) {

			try {
				graphicDevice.setFullScreenWindow(this);
				fullscreen = true;

				ImageWindow win = WindowManager.getCurrentWindow();
				if (win instanceof PlotWindow) {
					PlotWindow plo = (PlotWindow) win;
					if (plo != null) {
						Plot plot = plo.getPlot();
						if (plot != null) {
							Dimension dim = panel.getSize();
							plot.setFrameSize(dim.width, dim.height);
							int correctionX = plot.leftMargin + plot.rightMargin;
							int correctionY = plot.topMargin + plot.bottomMargin;
							plot.setSize(dim.width - correctionX, dim.height - correctionY);
							CanvasView.getCurrent().doLayout();
						}
					}
				}

			} catch (Exception e) {
				graphicDevice.setFullScreenWindow(null);
				fullscreen = false;
			}
			/* Change the settings if it is supported! */

			// switchDisplayMode();

		}
		return fullscreen;
	}

	/* Switch to a different display mode! */
	private void switchDisplayMode() {
		if (fullscreen && graphicDevice.isDisplayChangeSupported()) { // Change

			try {

				graphicDevice.setDisplayMode(new DisplayMode(monitorWidth, monitorHeight, bit, refreshRate));
				displayChanged = true;
			} catch (RuntimeException e) {
				graphicDevice.setDisplayMode(oldMode);
				displayChanged = false;

				e.printStackTrace();
			}

		}
	}

	public void exit() {

		if (fullscreen) {

			GraphicsEnvironment graphicEnvironment = null;
			GraphicsDevice graphicsDevice = null;
			graphicEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
			graphicsDevice = graphicEnvironment.getDefaultScreenDevice();
			if (graphicsDevice.isFullScreenSupported()) {
				if (displayChanged) {
					graphicDevice.setDisplayMode(oldMode);
				}

				fullscreen = false;

				graphicsDevice.setFullScreenWindow(null);

			}
		}
		CanvasView.setCurrent(panel);

		this.remove(panel);
		dispose();

		ImageWindow win = WindowManager.getCurrentWindow();
		if (win instanceof PlotWindow) {
			PlotWindow plo = (PlotWindow) win;
			if (plo != null) {
				Plot plot = plo.getPlot();
				if (plot != null) {
					if (temPlotSize != null) {
						Dimension dim = temPlotSize;
						plot.setFrameSize(dim.width, dim.height);
						int correctionX = plot.leftMargin + plot.rightMargin;
						int correctionY = plot.topMargin + plot.bottomMargin;
						plot.setSize(dim.width - correctionX, dim.height - correctionY);
					      //CanvasView.getCurrent().doLayout();
					}
				}
			}
		}
	}

	public void setBit(int bit) {
		this.bit = bit;
	}

	public void setMonitorHeight(int monitorHeight) {
		this.monitorHeight = monitorHeight;
	}

	public void setMonitorWidth(int monitorWidth) {
		this.monitorWidth = monitorWidth;
	}

	public static void setFullscreenOptions(int x, int y, int bitDepth, int refresh) {
		bit = bitDepth;
		monitorHeight = y;
		monitorWidth = x;
		refreshRate = refresh;
	}

}
