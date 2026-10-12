/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.image.ui;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

import ij.IJ;
import ij.ImageJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.Menus;
import ij.Prefs;
import ij.gui.ImageWindow;
import ij.swt.CodeEditors;
import ij.swt.ImageJHost;

/**
 * Embeds SWTImageJ in Bio7. Image windows are created inside a holder composite
 * which is displayed in a tab of the ImageJ-Canvas view ({@link CanvasView}) or
 * in a detached view ({@link CustomDetachedImageJView}). The holder composite is
 * the editor parent of the image (see {@link ImagePlus#getEditorParent()}) and
 * is moved, not recreated, when an image is detached.
 *
 * @author M. Austenfeld
 */
public class Bio7ImageJ implements ImageJHost, ImageListener {

	private static Bio7ImageJ instance;

	/* See ij.plugin.Compiler! */
	private static final String COMPILER_TARGET_KEY = "javac.target";

	private static final int COMPILER_TARGET = 25;

	private Bio7ImageJ() {
	}

	/**
	 * Installs ImageJ in Bio7 (host callbacks, plugins path, ImageJ instance). Can
	 * be called several times. Must be called in the SWT UI thread.
	 */
	public static synchronized void install() {
		if (instance != null) {
			return;
		}
		instance = new Bio7ImageJ();
		setPluginsDirectory();
		addBundlesToCompilerClassPath();
		IJ.setHost(instance);
		/* SWTImageJ tools (e.g., an assistant) can read and write the workbench editors! */
		CodeEditors.addProvider(new WorkbenchCodeEditors());
		ImagePlus.addImageListener(instance);
		/* F2 displays the current image in fullscreen mode (ESC closes it)! */
		Util.getDisplay().addFilter(SWT.KeyDown, Fullscreen.KEY_FILTER);
		if (IJ.getInstance() == null) {
			/* The ImageJ shell is never shown. Bio7 displays toolbar and menus! */
			new ImageJ(ImageJ.NO_SHOW);
		}
		setCompilerTarget();
	}

	/*
	 * SWTImageJ uses <plugins.dir>/plugins and <plugins.dir>/macros. The Bio7
	 * preferences store the plugins folder itself.
	 */
	private static void setPluginsDirectory() {
		if (System.getProperty("plugins.dir") != null) {
			return;
		}
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		File plugins = new File(store.getString("PLUGINS_PATH"));
		if (plugins.isDirectory() && plugins.getParentFile() != null) {
			System.setProperty("plugins.dir", plugins.getParentFile().getAbsolutePath());
		} else {
			System.setProperty("plugins.dir", Util.getImageJPath());
		}
	}

	/*
	 * Plugins are compiled for Java 25 by default (SWTImageJ uses Java 8). The
	 * preference must be set after ImageJ has loaded the preferences and before
	 * the compiler class is loaded. A target selected in "Compile and Run" options
	 * (other than the old default 8) is kept!
	 */
	private static void setCompilerTarget() {
		int target = (int) Prefs.get(COMPILER_TARGET_KEY, 0);
		if (target == 0 || target == 8) {
			Prefs.set(COMPILER_TARGET_KEY, COMPILER_TARGET);
		}
	}

	/*
	 * The ImageJ compiler (Compile and Run, Script Explorer) uses the
	 * "java.class.path" property which only contains the OSGi launcher. Add the
	 * ImageJ, SWT and Bio7 image bundles so that plugins can be compiled!
	 */
	private static void addBundlesToCompilerClassPath() {
		Bundle self = Activator.getDefault().getBundle();
		for (Bundle bundle : self.getBundleContext().getBundles()) {
			String name = bundle.getSymbolicName();
			if (name == null || !(name.startsWith("org.eclipse.swt") || bundle == self)) {
				continue;
			}
			try {
				File file = FileLocator.getBundleFile(bundle);
				if (file.isDirectory()) {
					/* A bundle in the workspace (development) or an unpacked bundle! */
					boolean classes = false;
					for (String folder : new String[] { "bin", "target/classes" }) {
						File dir = new File(file, folder);
						if (dir.isDirectory()) {
							Menus.addExtraClassPathDirectory(dir.getAbsolutePath());
							classes = true;
						}
					}
					if (!classes) {
						Menus.addExtraClassPathDirectory(file.getAbsolutePath());
					}
				} else {
					Menus.addExtraClassPathDirectory(file.getAbsolutePath());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Returns the holder composite of an image displayed in Bio7 or null.
	 */
	public static Composite getHolder(ImagePlus imp) {
		if (imp == null) {
			return null;
		}
		Composite holder = imp.getEditorParent();
		if (holder == null || holder.isDisposed()) {
			return null;
		}
		return holder;
	}

	/**
	 * Returns the tab of the ImageJ-Canvas view which displays the image or null
	 * (e.g., if the image is displayed in a detached view). Must be called in the
	 * SWT UI thread.
	 */
	public static CTabItem getTab(ImagePlus imp) {
		Composite holder = getHolder(imp);
		if (holder == null || CanvasView.tabFolder == null || CanvasView.tabFolder.isDisposed()) {
			return null;
		}
		for (CTabItem item : CanvasView.tabFolder.getItems()) {
			if (item.getControl() == holder) {
				return item;
			}
		}
		return null;
	}

	/**
	 * Creates a new holder composite for an image in the given parent.
	 */
	static Composite createHolder(Composite parent, ImagePlus imp) {
		Composite holder = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, true);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		holder.setLayout(layout);
		/* Close the image if the holder is disposed (tab or view closed)! */
		holder.addDisposeListener(e -> {
			ImageWindow win = imp.getWindow();
			if (win != null && !win.isClosed() && imp.getEditorParent() == holder) {
				win.close();
			}
		});
		return holder;
	}

	@Override
	public Composite getImageParent(ImagePlus imp) {
		CanvasView view = CanvasView.getCanvas_view();
		if (view == null || CanvasView.tabFolder == null || CanvasView.tabFolder.isDisposed()) {
			showView(CanvasView.ID);
			view = CanvasView.getCanvas_view();
		}
		if (view == null || CanvasView.tabFolder == null || CanvasView.tabFolder.isDisposed()) {
			/* Fallback: ImageJ opens a separate shell! */
			return null;
		}
		return view.createImageTab(imp);
	}

	@Override
	public void titleChanged(ImageWindow win, String title) {
		ImagePlus imp = win.getImagePlus();
		CTabItem item = getTab(imp);
		if (item != null) {
			item.setText(title);
			return;
		}
		CustomDetachedImageJView detached = CustomDetachedImageJView.find(imp);
		if (detached != null) {
			detached.setPartName(title);
		}
	}

	@Override
	public void toFront(ImageWindow win) {
		ImagePlus imp = win.getImagePlus();
		CTabItem item = getTab(imp);
		IWorkbenchPage page = getPage();
		if (item != null) {
			CanvasView.tabFolder.setSelection(item);
			CanvasView.getCanvas_view().tabSelected(item);
			if (page != null) {
				page.bringToTop(CanvasView.getCanvas_view());
			}
			return;
		}
		CustomDetachedImageJView detached = CustomDetachedImageJView.find(imp);
		if (detached != null && page != null) {
			page.bringToTop(detached);
		}
	}

	@Override
	public void showStatus(String message) {
		Display display = Util.getDisplay();
		display.asyncExec(() -> {
			CanvasView view = CanvasView.getCanvas_view();
			if (view != null) {
				view.setstatusline(message);
			}
		});
	}

	@Override
	public void imageOpened(ImagePlus imp) {
	}

	@Override
	public void imageClosed(ImagePlus imp) {
		/* Remove the tab or detached view of a closed image! */
		Util.getDisplay().asyncExec(() -> {
			Composite holder = getHolder(imp);
			if (holder == null) {
				return;
			}
			ImageWindow win = imp.getWindow();
			/* ImageJ also calls this if a window has been replaced (e.g., RGB to stack)! */
			if (win != null && !win.isClosed()) {
				return;
			}
			if (Fullscreen.isFullscreen(holder)) {
				Fullscreen.exit();
			}
			CTabItem item = getTab(imp);
			CustomDetachedImageJView detached = CustomDetachedImageJView.find(imp);
			if (item != null) {
				item.dispose();
			}
			if (detached != null) {
				IWorkbenchPage page = detached.getSite().getPage();
				page.hideView(detached);
			}
			if (!holder.isDisposed()) {
				holder.dispose();
			}
		});
	}

	@Override
	public void imageUpdated(ImagePlus imp) {
		/* Display the image information (size, type, etc.) in the status line! */
		Util.getDisplay().asyncExec(() -> {
			CanvasView view = CanvasView.getCanvas_view();
			ImageWindow win = imp.getWindow();
			if (view != null && win != null && !win.isClosed() && ij.WindowManager.getCurrentImage() == imp) {
				view.setstatusline(win.createSubtitle());
			}
		});
	}

	static IWorkbenchPage getPage() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
			if (windows.length == 0) {
				return null;
			}
			window = windows[0];
		}
		return window.getActivePage();
	}

	private static IViewPart showView(String id) {
		IWorkbenchPage page = getPage();
		if (page == null) {
			return null;
		}
		try {
			return page.showView(id, null, IWorkbenchPage.VIEW_VISIBLE);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		return null;
	}
}
