/*******************************************************************************
 * Copyright (c) 2007-2013 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.worldwind;

import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.StackWindow;
import ij.process.ImageProcessor;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class ScreenRecording {
	private ImageStack s;
	private int countTo = 0;
	private Button captureButton;
	private Robot robot;
	private Composite top;
	protected org.eclipse.swt.graphics.Point p;
	protected org.eclipse.swt.graphics.Rectangle parent;
	private int frameCount;
	private int fps = 25;
	private static ScreenRecording instance;
	private static boolean capture = false;

	public ScreenRecording(Button captureButton) {
		instance = this;
		this.captureButton = captureButton;
	}

	/** Captures the active image window and returns it as an ImagePlus. */
	public ImagePlus captureImage() {

		ImagePlus imp2 = null;
		try {

			Rectangle r = getCaptureRectangle();
			Robot robot = new Robot();
			Image img = robot.createScreenCapture(r);
			if (img != null) {
				String title = WindowManager.getUniqueName("Screen Capture");
				imp2 = new ImagePlus(title, img);
			}
		} catch (Exception e) {
		}
		return imp2;
	}

	public ImagePlus setupCaptureImages() {

		ImagePlus imp2 = null;

		Rectangle r = getCaptureRectangle();

		try {
			robot = new Robot();
		} catch (AWTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Image img = robot.createScreenCapture(r);

		ImagePlus imp = new ImagePlus("Spatial", img);
		ImageProcessor ip = imp.getProcessor();
		s = new ImageStack(imp.getWidth(), imp.getHeight());

		s.addSlice(null, ip);
		s.addSlice(null, ip);
		ImagePlus plus = new ImagePlus("Stack", s);
		// We make it visible!
		plus.show();

		return imp2;
	}

	public void addCapturedImage() {
		if (s != null) {
			if (s.getSize() < countTo) {

				Rectangle bounds = getCaptureRectangle();

				if (s.getWidth() == bounds.getWidth() && s.getHeight() == bounds.getHeight()) {

					Image img = robot.createScreenCapture(bounds);

					ImagePlus imp = new ImagePlus("Spatial", img);

					ImageProcessor ip = imp.getProcessor();

					s.addSlice(null, ip);
					StackWindow sw = (StackWindow) WindowManager.getCurrentWindow();
					if (sw != null) {
						sw.updateSliceSelector();
					}
				}

				else {
					endCapture();

				}
			} else {
				endCapture();
			}
		}

	}

	private Rectangle getCaptureRectangle() {
		top = WorldWindView.getInstance().getTopComposite();
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				parent = top.getBounds();

				p = top.toDisplay(parent.x, parent.y);
			}
		});

		Rectangle bounds = new Rectangle(p.x, p.y, parent.width, parent.height - 15);
		return bounds;
	}

	private void endCapture() {
		capture = false;
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				captureButton.setText("Capture");
			}
		});
	}

	public void setCount(int countTo) {
		this.countTo = countTo;

	}

	public static boolean doCapture() {
		return capture;
	}

	public static void setCapture(boolean capture) {
		ScreenRecording.capture = capture;
	}

	public static ScreenRecording getInstance() {
		return instance;
	}

	public int getCountTo() {
		return countTo;
	}

	/* A job which runs the capture method to hold the GUI responsive! */
	public void docaptureAnimationJob() {

		Job job = new Job("Capture WorldWind Canvas") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Capture WorldWind Canvas ...", countTo);

				captureAnimation(monitor);

				monitor.done();
				return Status.OK_STATUS;
			}

		};
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {

					frameCount = 0;
				} else {

				}
			}
		});

		job.schedule();

	}

	/*
	 * Adapted source for this implementation: http://www.koonsolo.com/news/dewitters-gameloop/
	 */
	public void captureAnimation(IProgressMonitor monitor) {

		int framesPerSecond = fps;
		int skipTicks = 1000 / framesPerSecond;

		double nextFrameTick = System.currentTimeMillis();

		int sleepTime = 0;

		while (capture == true && frameCount <= countTo) {
			if (ScreenRecording.doCapture()) {

				addCapturedImage();
				monitor.worked(1);
				frameCount++;
			}

			nextFrameTick += skipTicks;
			sleepTime = (int) (nextFrameTick - System.currentTimeMillis());
			if (sleepTime >= 0) {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {

					e.printStackTrace();
				}
			} else {

			}
		}

	}

	public void setFps(int selection) {
		this.fps = selection;

	}

}
