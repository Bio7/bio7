/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.spatial;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import com.eco.bio7.swt.SwtAwt;
import com.eco.bio7.util.Util;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.Screen;
import com.jogamp.opengl.awt.GLCanvas;

public class SpatialView extends ViewPart {
	public static final String ID = "com.eco.bio7.spatial";

	private SpatialStructure spat;

	private Fullscreen full;

	private Frame frame;

	private GLCanvas canvas;

	public Composite top;

	private SpatialView instance;

	public SpatialView() {

		instance = this;

		/*
		 * if (Util.getOS().equals("Windows")) { SwingUtilities.invokeLater(new
		 * Runnable() { // !! public void run() {
		 * 
		 * spat = new SpatialStructure(instance);
		 * 
		 * } }); } else { For Linux! spat = new SpatialStructure(instance);
		 * 
		 * }
		 */

	}

	public void createPartControl(Composite parent) {

		initializeToolBar();

		top = parent;// new Composite(parent, SWT.NO_BACKGROUND | SWT.EMBEDDED);

		spat = new SpatialStructure(instance);
		/*
		 * try { System.setProperty("sun.awt.noerasebackground", "true"); } catch
		 * (NoSuchMethodError error) { }
		 * 
		 * Display display = PlatformUI.getWorkbench().getDisplay();
		 * display.syncExec(new Runnable() {
		 * 
		 * public void run() {
		 * 
		 * frame = SWT_AWT.new_Frame(top); SwtAwt.setSwtAwtFocus(frame, top); canvas =
		 * spat.getCanvas(); top.setLayout(new RowLayout()); frame.add(canvas); } });
		 */
	}

	/*
	 * JavaFX performant?
	 * 
	 * top = new Composite(parent, SWT.NONE); top.setLayout(new FillLayout());
	 * SwingFxSwtView view=new SwingFxSwtView(); canvas = spat.getCanvas();
	 * view.embedd(top,canvas);
	 */

	public void createFullscreen(int i) {
		spat.getAnimator().stop();
		Screen screen = spat.glWindow.getScreen();
		List<MonitorDevice> monitorDevices = new ArrayList<>();
		MonitorDevice dev = screen.getMonitorDevices().get(i);
		if (dev != null) {
			monitorDevices.add(dev);
			spat.glWindow.setFullscreen(monitorDevices);
		}

		spat.getAnimator().start();

	}

	public void recreateGLCanvas() {
		spat.getAnimator().stop();
		spat.glWindow.setFullscreen(false);
		spat.getAnimator().start();

	}

	public void setFullscreen(Fullscreen screen) {
		full = screen;
	}

	public Fullscreen getFullscreen() {
		return full;
	}

	public void setFocus() {

	}

	private void initializeToolBar() {

	}

	public void dispose() {

		

		super.dispose();
	}

}
