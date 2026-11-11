/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import ij.Executer;
import ij.plugin.frame.RoiManager;

/**
 * Docks the ROI Manager of SWTImageJ into a Bio7 custom view (option of the
 * Bio7 ImageJ preferences). When the shell of the ROI Manager is opened (by the
 * menu, the key "t", the popup menu, a macro or a plugin), its controls are
 * moved into the custom view and the empty shell stays hidden (as
 * {@link AssistantDocking}). The ROI Manager still uses the hidden shell, e.g.,
 * for its dialogs, which are centered on the view. Closing the view closes the
 * ROI Manager.
 */
public final class RoiManagerDocking {

	/** The preference (store of com.eco.bio7.image) which enables the docking. */
	public static final String PREFERENCE = "ROI_MANAGER_AS_VIEW";
	private static final String TITLE = "ROI Manager";
	private static final String COMMAND = "ROI Manager...";
	private static final String DOCKED = "com.eco.bio7.roimanager.docked";
	private static final String UNDOCKED = "com.eco.bio7.roimanager.undocked";

	private static boolean installed;
	/* The docked shell and the view composite (null if not docked)! */
	private static Shell shell;
	private static Composite content;
	private static Image icon;

	private RoiManagerDocking() {
	}

	/** Installs the docking (in the UI thread, once); it applies if the preference is enabled. */
	public static void install(Display display) {
		if (installed) {
			return;
		}
		installed = true;
		/*
		 * Show is sent before the shell is displayed: the controls are moved at once,
		 * the shell is transparent (and outside of the screens where the transparency
		 * is not supported); the empty shell is hidden later!
		 */
		display.addFilter(SWT.Show, e -> {
			if (e.widget instanceof Shell s && isRoiManager(s) && isEnabled()) {
				if (s.getData(DOCKED) == null && s.getData(UNDOCKED) == null) {
					s.setData(DOCKED, Boolean.TRUE);
					s.setAlpha(0);
					Composite parent = moveControls(s);
					if (parent != null) {
						if (!"cocoa".equals(SWT.getPlatform())) {
							Rectangle screens = display.getBounds();
							s.setBounds(screens.x + screens.width + 500, screens.y + screens.height + 500, 1, 1);
						}
						display.asyncExec(() -> dock(s, parent));
					}
				}
			}
		});
		/* The menu command of an open (docked) ROI Manager shows the view (the hidden shell is not activated)! */
		try {
			Executer.addCommandListener(command -> {
				if (COMMAND.equals(command) && shell != null && !shell.isDisposed() && content != null) {
					display.asyncExec(RoiManagerDocking::showView);
				}
				return command;
			});
		} catch (RuntimeException | LinkageError e) {
			/* Without ImageJ the view is not shown again by the command! */
		}
	}

	private static boolean isEnabled() {
		try {
			return com.eco.bio7.image.ui.Activator.getDefault().getPreferenceStore().getBoolean(PREFERENCE);
		} catch (RuntimeException e) {
			return false;
		}
	}

	/* The shell of the current ROI Manager (not a hidden one of the batch mode, not a dialog)! */
	private static boolean isRoiManager(Shell s) {
		if (s.isDisposed() || s.getParent() != null || !TITLE.equals(s.getText())) {
			return false;
		}
		RoiManager rm = RoiManager.getInstance();
		return rm != null && rm.getShell() == s;
	}

	/* The Bio7 ImageJ icon (as the tab of the ImageJ-Canvas view, recolored like it) for the tab of the view! */
	private static Image icon(Display display) {
		if (icon == null || icon.isDisposed()) {
			try {
				var descriptor = com.eco.bio7.util.IconColors.imageDescriptorFromPlugin("com.eco.bio7.image", "pics/imagejview.svg");
				icon = descriptor != null ? descriptor.createImage(false, display) : null;
			} catch (RuntimeException e) {
				icon = null;
			}
		}
		return icon;
	}

	/* Moves the controls of the shell into a custom view. Returns the composite of the view or null if it stays a window! */
	private static Composite moveControls(Shell s) {
		Control[] children = s.getChildren();
		boolean movable = shell == null || shell.isDisposed();
		for (Control c : children) {
			movable &= c.isReparentable();
		}
		Composite parent = null;
		if (movable) {
			try {
				parent = new CustomView().getComposite(TITLE, icon(s.getDisplay()));
			} catch (RuntimeException e) {
				/* The view could not be created: the ROI Manager stays a window */
				parent = null;
			}
		}
		if (parent == null || parent.isDisposed()) {
			s.setData(UNDOCKED, Boolean.TRUE);
			s.setData(DOCKED, null);
			s.setAlpha(255);
			return null;
		}
		Layout layout = s.getLayout();
		parent.setLayout(layout);
		s.setLayout(null);
		for (Control c : children) {
			c.setParent(parent);
		}
		parent.layout(true, true);
		return parent;
	}

	/* Hides the empty shell and connects the view with the shell of the ROI Manager! */
	private static void dock(Shell s, Composite parent) {
		if (s.isDisposed() || parent.isDisposed()) {
			return;
		}
		s.setVisible(false);
		shell = s;
		content = parent;
		Listener sync = e -> syncBounds();
		parent.addListener(SWT.Resize, sync);
		parent.addListener(SWT.Move, sync);
		syncBounds();
		/*
		 * Closing the view closes the ROI Manager (as its window: the list, the overlay
		 * question). The Dispose event comes before the controls are disposed: they go
		 * back into the hidden shell, the ROI Manager is closed after the workbench has
		 * removed the view. Its close may open a dialog ("Save the displayed ROIs as an
		 * overlay?"), which must not run while the view is disposed!
		 */
		Listener closeManager = e -> {
			Shell docked = shell;
			shell = null;
			content = null;
			if (docked == null || docked.isDisposed()) {
				return;
			}
			docked.setLayout(parent.getLayout());
			for (Control c : parent.getChildren()) {
				c.setParent(docked);
			}
			docked.getDisplay().asyncExec(() -> {
				if (!docked.isDisposed()) {
					docked.close();
					if (!docked.isDisposed()) {
						docked.dispose();
					}
				}
			});
		};
		parent.addListener(SWT.Dispose, closeManager);
		/* The ROI Manager is closed by ImageJ (e.g., a macro): close the view! */
		s.addListener(SWT.Dispose, e -> {
			if (content == parent) {
				shell = null;
				content = null;
				parent.removeListener(SWT.Dispose, closeManager);
				if (!parent.isDisposed()) {
					closeView(parent);
				}
			}
		});
		/* A tool window: no fullscreen mode and no window action in the view (the ROI Manager window is the option of the preferences) */
		if (parent.getData(CustomViewFullscreen.VIEW_KEY) instanceof CustomView view) {
			view.setFullscreenEnabled(false);
		}
	}

	/* The hidden shell has the bounds of the view, so that the dialogs of the ROI Manager are centered on the view! */
	private static void syncBounds() {
		if (shell == null || shell.isDisposed() || content == null || content.isDisposed()) {
			return;
		}
		Rectangle r = content.getClientArea();
		Point p = content.toDisplay(r.x, r.y);
		shell.setBounds(p.x, p.y, Math.max(1, r.width), Math.max(1, r.height));
	}

	/* Brings the view of the docked ROI Manager to front! */
	private static void showView() {
		if (content == null || content.isDisposed()) {
			return;
		}
		if (content.getData(CustomViewFullscreen.VIEW_KEY) instanceof CustomView view) {
			view.getSite().getPage().activate(view);
		}
	}

	private static void closeView(Composite parent) {
		if (parent.getData(CustomViewFullscreen.VIEW_KEY) instanceof CustomView view && PlatformUI.isWorkbenchRunning()) {
			IWorkbenchPage page = view.getSite().getPage();
			if (page != null) {
				page.hideView(view);
			}
		}
	}
}
