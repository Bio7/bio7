/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/

package com.eco.bio7.themes;

import java.lang.reflect.Method;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Protects the frames of the toolbars and minimized view stacks (the
 * ImageBasedFrame of e4 with the CSS properties handle-image and frame-image)
 * against a theme switch.
 * <p>
 * The theme engine disposes the images of the old theme. A frame whose image
 * is disposed calls reskin(SWT.ALL) in every paint, and e4 replaces the images
 * only if the new theme defines both images for the control (e.g., not the dark
 * theme with handle-image: none). Then every paint restyles the frame and its
 * toolbars again and paints them again: an endless loop which uses a core of
 * the CPU and (Windows) delays the runnables of asyncExec and timerExec, e.g.,
 * the restart after the theme switch, until the user moves the window.
 * <p>
 * Before a theme switch each frame gets its own copies of its images (disposed
 * with the frame), so the frame keeps valid images (the new theme draws its
 * frames after the restart).
 */
public final class FrameImages {

	private static final String FRAME_CLASS = "org.eclipse.e4.ui.widgets.ImageBasedFrame";
	private static final String FRAME_IMAGE = "frameImage";
	private static final String HANDLE_IMAGE = "handleImage";
	/* The copies of this class (disposed when the frame is disposed or protected again) */
	private static final String COPIES = "bio7.themes.frameImageCopies";

	private FrameImages() {
	}

	/** Gives all frames of all shells copies of their images. Called before a theme switch (UI thread). */
	public static void protect() {
		Display display = Display.getCurrent();
		if (display == null || display.isDisposed()) {
			return;
		}
		install(display);
		for (Shell shell : display.getShells()) {
			protect(shell);
		}
	}

	private static void protect(Control control) {
		if (control.isDisposed()) {
			return;
		}
		if (FRAME_CLASS.equals(control.getClass().getName())) {
			copyImages(control);
		}
		if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				protect(child);
			}
		}
	}

	private static void copyImages(Control frame) {
		Image frameImage = valid(frame.getData(FRAME_IMAGE));
		Image handleImage = valid(frame.getData(HANDLE_IMAGE));
		if (frameImage == null && handleImage == null) {
			return;
		}
		try {
			Method setImages = frame.getClass().getMethod("setImages", Image.class, Integer[].class, Image.class);
			Image frameCopy = frameImage == null ? null : new Image(frame.getDisplay(), frameImage, SWT.IMAGE_COPY);
			Image handleCopy = handleImage == null ? null : new Image(frame.getDisplay(), handleImage, SWT.IMAGE_COPY);
			/* The cuts (null) are kept: the copies have the size of the images */
			setImages.invoke(frame, frameCopy, null, handleCopy);
			Image[] old = (Image[]) frame.getData(COPIES);
			frame.setData(COPIES, new Image[] { frameCopy, handleCopy });
			if (old == null) {
				frame.addListener(SWT.Dispose, _ -> dispose((Image[]) frame.getData(COPIES)));
			} else {
				dispose(old);
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			/* Another version of e4 (the frame keeps its images) */
		}
	}

	private static boolean installed;

	/**
	 * The safety net (e.g., a theme switch of the Eclipse preferences): a frame
	 * which is painted with disposed images (e4 did not replace them) gets valid
	 * images: the drag handle of e4 and a transparent frame. Installed once (UI
	 * thread).
	 */
	public static void install(Display display) {
		if (installed) {
			return;
		}
		installed = true;
		/* A filter: before the paint listener of the frame (which would call reskin) */
		display.addFilter(SWT.Paint, event -> {
			if (event.widget instanceof Control frame && FRAME_CLASS.equals(frame.getClass().getName())) {
				repair(frame);
			}
		});
	}

	private static void repair(Control frame) {
		boolean frameDisposed = disposed(frame.getData(FRAME_IMAGE));
		boolean handleDisposed = disposed(frame.getData(HANDLE_IMAGE));
		if (!frameDisposed && !handleDisposed) {
			return;
		}
		try {
			Class<?> c = frame.getClass();
			Image frameCopy = null;
			if (frameDisposed) {
				/* A transparent frame with the size of the old image (the cuts are kept) */
				int w = field(c, frame, "w1") + field(c, frame, "w2") + field(c, frame, "w3");
				int h = field(c, frame, "h1") + field(c, frame, "h2") + field(c, frame, "h3");
				frameCopy = transparent(frame.getDisplay(), w, h);
			}
			Image handleCopy = null;
			if (handleDisposed) {
				java.lang.reflect.Field vertical = c.getDeclaredField("vertical");
				vertical.setAccessible(true);
				Image dragHandle = org.eclipse.jface.resource.JFaceResources
						.getImage(vertical.getBoolean(frame) ? "org.eclipse.e4.ui.workbench.swt.DRAG_HANDLE-ROTATED" : "org.eclipse.e4.ui.workbench.swt.DRAG_HANDLE");
				handleCopy = dragHandle != null && !dragHandle.isDisposed() ? new Image(frame.getDisplay(), dragHandle, SWT.IMAGE_COPY)
						: transparent(frame.getDisplay(), 1, 1);
			}
			c.getMethod("setImages", Image.class, Integer[].class, Image.class).invoke(frame, frameCopy, null, handleCopy);
			Image[] old = (Image[]) frame.getData(COPIES);
			Image[] copies = { frameCopy != null ? frameCopy : old != null ? old[0] : null, handleCopy != null ? handleCopy : old != null ? old[1] : null };
			frame.setData(COPIES, copies);
			if (old == null) {
				frame.addListener(SWT.Dispose, _ -> dispose((Image[]) frame.getData(COPIES)));
			} else {
				for (int i = 0; i < 2; i++) {
					if (old[i] != null && old[i] != copies[i] && !old[i].isDisposed()) {
						old[i].dispose();
					}
				}
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			/* Another version of e4 */
		}
	}

	private static int field(Class<?> c, Object o, String name) throws ReflectiveOperationException {
		java.lang.reflect.Field f = c.getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(o);
	}

	private static Image transparent(Display display, int width, int height) {
		org.eclipse.swt.graphics.ImageData data = new org.eclipse.swt.graphics.ImageData(Math.max(1, width), Math.max(1, height), 24,
				new org.eclipse.swt.graphics.PaletteData(0xFF0000, 0xFF00, 0xFF));
		data.alphaData = new byte[data.width * data.height];
		return new Image(display, data);
	}

	private static boolean disposed(Object image) {
		return image instanceof Image i && i.isDisposed();
	}

	private static Image valid(Object image) {
		return image instanceof Image i && !i.isDisposed() ? i : null;
	}

	private static void dispose(Image[] images) {
		if (images == null) {
			return;
		}
		for (Image image : images) {
			if (image != null && !image.isDisposed()) {
				image.dispose();
			}
		}
	}
}
