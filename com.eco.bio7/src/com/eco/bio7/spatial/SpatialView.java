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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
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
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.swt.NewtCanvasSWT;
import com.jogamp.opengl.awt.GLCanvas;

public class SpatialView extends ViewPart {
	public static final String ID = "com.eco.bio7.spatial";

	private SpatialStructure spat;

	private Fullscreen full;

	private Frame frame;

	private GLCanvas canvas;

	public Composite top;

	private SpatialView instance;

	protected boolean detached = false;

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
		/* Add a listener to detect detached views! */
		parent.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				/* Detached views have no title! */
				if (Util.getOS().equals("Mac")) {
					boolean isDetached = parent.getShell().getText().length() == 0;
					if (isDetached) {

						if (detached == false) {
							// System.out.println("detach");
							spat.glWindow.setFullscreen(true);
							spat.glWindow.setFullscreen(false);
							detached = true;
						}

					} else {
						if (detached == true) {
							// System.out.println("attach");
							spat.glWindow.setFullscreen(true);
							spat.glWindow.setFullscreen(false);
							detached = false;
						}

					}
				}
			}
		});

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
					if (Util.getOS().equals("Mac")) {
						NewtCanvasSWT scanvas = spat.getCanvas();
						if (scanvas.getNEWTChild() != null) {
							scanvas.getNEWTChild().setVisible(false);
							// spat.getCanvas().getNEWTChild().setPosition(0, 0);
						}
					}
				}

			}

			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals("com.eco.bio7.spatial")) {
					if (Util.getOS().equals("Mac")) {
						if (parent.isDisposed() == false) {
							Rectangle rec = parent.getClientArea();
							Display display = PlatformUI.getWorkbench().getDisplay();
							display.asyncExec(new Runnable() {
								public void run() {
									// spat.getCanvas().setParent(parent);
									spat.getCanvas().getNEWTChild().setVisible(true);
									// System.out.println(spat.getCanvas().getNEWTChild().getBounds());
									// spat.getCanvas().getNEWTChild().setPosition(0, 0);
								}
							});
						}
					}
				}

			}

			@Override
			public void partInputChanged(IWorkbenchPartReference partRef) {
				// TODO Auto-generated method stub

			}

		};
		page.addPartListener(pl);
	}

	public void createFullscreen(int i) {

		Job job = new Job("Fullscreen") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Fullscreen mode ...", 20);

				Screen screen = spat.glWindow.getScreen();
				List<MonitorDevice> monitorDevices = new ArrayList<>();
				// MonitorDevice dev = screen.getMonitorDevices().get(i);
				while (screen.getMonitorDevices() == null && monitor.isCanceled() == false) {
					monitor.worked(1);
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				List<MonitorDevice> monitorDevicesAvailable = screen.getMonitorDevices();
				if (i < monitorDevicesAvailable.size()) {
					MonitorDevice dev = monitorDevicesAvailable.get(i);
					if (dev != null) {
						monitorDevices.add(dev);
						/* Workaround for windows to correctly set fullscreen coordinates! */
						if (Util.getOS().equals("Windows")) {
							spat.glWindow.setVisible(false);
							spat.glWindow.setFullscreen(monitorDevices);
							spat.glWindow.setFullscreen(false);
							spat.glWindow.setFullscreen(monitorDevices);
							spat.glWindow.setVisible(true);
						}

						else {

							spat.glWindow.setFullscreen(monitorDevices);

						}

					}
				}
				monitor.done();
				return Status.OK_STATUS;

			}

		};
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {

				} else {

				}
			}
		});
		job.schedule();

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
