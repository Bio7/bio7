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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import com.eco.bio7.swt.SwtAwt;
import com.eco.bio7.util.Util;
import com.eco.bio7.worldwind.swt.NewtCanvasSWT;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
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
		/* Workaround to set the images for Bio7 again on MacOSX! */
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

		Image[] imagesTemp = Util.getShell().getImages();
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
		Util.getShell().setImages(imagesTemp);

		IPartListener2 pl = new IPartListener2() {

			@Override
			public void partActivated(IWorkbenchPartReference partRef) {

				if (partRef.getId().equals("com.eco.bio7.spatial")) {

				}
			}

			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals("com.eco.bio7.spatial")) {

				}
			}

			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
				// TODO Auto-generated method stub

			}

			@Override
			public void partDeactivated(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals("com.eco.bio7.spatial")) {

				}

			}

			@Override
			public void partOpened(IWorkbenchPartReference partRef) {

			}

			@Override
			public void partHidden(IWorkbenchPartReference partRef) {

				if (partRef.getId().equals("com.eco.bio7.spatial")) {

					/*
					 * NewtCanvasSWT scanvas = spat.getCanvas();
					 * 
					 * 
					 * if(scanvas.getNEWTChild()!=null) scanvas.getNEWTChild().setSize(1, 1);
					 */

				}

			}

			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals("com.eco.bio7.spatial")) {
					/*
					 * if (parent.isDisposed() == false) {
					 * 
					 * Rectangle rec = parent.getClientArea();
					 * 
					 * spat.getCanvas().getNEWTChild().setSize(rec.width, rec.height);
					 * 
					 * }
					 */

				}

			}

			@Override
			public void partInputChanged(IWorkbenchPartReference partRef) {
				// TODO Auto-generated method stub

			}

		};
		page.addPartListener(pl);
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
