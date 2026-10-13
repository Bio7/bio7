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

package com.eco.bio7.collection;

import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;

/**
 * Displays the content of a {@link CustomView} in a fullscreen shell. The
 * content composite of the view is moved into the shell and back to the view
 * when the fullscreen mode is closed. The keys (in a custom view or in the
 * fullscreen mode):
 * <ul>
 * <li>F2: fullscreen on the monitor of the view (or exit)</li>
 * <li>F3: fullscreen on the secondary monitor</li>
 * <li>F4: fullscreen on the tertiary monitor</li>
 * <li>F5: select the monitor</li>
 * <li>ESC: exit</li>
 * </ul>
 * F3, F4 and F5 move the fullscreen mode to another monitor and exit if the
 * view is already displayed on the monitor. The monitors are numbered with the
 * primary monitor first (see {@link #getMonitors(Display)}). All methods must
 * be called in the SWT UI thread!
 */
public class CustomViewFullscreen {

	/* The key of the view in the data of the content composite! */
	static final String VIEW_KEY = "com.eco.bio7.collection.CustomView";

	private static final String MESSAGE = "Displayed in fullscreen mode (press F2 or ESC to exit, F3, F4 or F5 to change the monitor).";

	private static Shell shell;
	private static CustomView view;
	private static Composite content;
	private static Composite oldParent;
	private static Label placeholder;
	private static Monitor shellMonitor;
	/* True when the native fullscreen mode of macOS has been entered (the shell is resized)! */
	private static boolean entered;

	/*
	 * On macOS a shell in the native fullscreen mode must have a trim, else the
	 * shell cannot become the key window (no key input, Cmd+M minimizes the
	 * workbench window).
	 */
	private static final boolean MAC = "cocoa".equals(SWT.getPlatform());
	private static boolean installed = false;

	private static final Listener KEY_FILTER = new Listener() {
		public void handleEvent(Event e) {
			if (!(e.widget instanceof Control) || !e.doit) {
				return;
			}
			Control control = (Control) e.widget;
			boolean inFullscreen = isActive() && control.getShell() == shell;
			if (e.keyCode == SWT.ESC) {
				if (inFullscreen) {
					/* The ESC key is also forwarded to the content! */
					Display.getCurrent().asyncExec(CustomViewFullscreen::exit);
				}
				return;
			}
			int key = functionKey(e.keyCode);
			CustomView v = inFullscreen ? view : (isActive() ? null : viewOf(control));
			if (key > 0 && v != null) {
				e.doit = false;
				/* Not in the filter (F5 opens a dialog)! */
				Display.getCurrent().asyncExec(() -> handleKey(key, v));
			}
		}
	};

	/* Returns 2 to 5 for the keys F2 to F5, else 0! */
	private static int functionKey(int keyCode) {
		switch (keyCode) {
		case SWT.F2:
			return 2;
		case SWT.F3:
			return 3;
		case SWT.F4:
			return 4;
		case SWT.F5:
			return 5;
		default:
			return 0;
		}
	}

	/* The action of the keys F2 to F5 for the view (displayed in fullscreen mode or not)! */
	private static void handleKey(int key, CustomView v) {
		if (isActive() && !isFullscreen(v)) {
			return;
		}
		if (key == 2) {
			toggle(v);
			return;
		}
		Display display = Display.getDefault();
		Monitor[] monitors = getMonitors(display);
		Monitor monitor;
		if (key == 5) {
			monitor = selectMonitor(display);
			if (monitor == null) {
				return;
			}
		} else if (key - 2 < monitors.length) {
			monitor = monitors[key - 2];
		} else {
			/* No secondary or tertiary monitor! */
			display.beep();
			return;
		}
		if (isFullscreen(v) && same(monitor, shellMonitor)) {
			exit();
		} else {
			show(v, monitor);
		}
	}

	/* The keys of embedded AWT/Swing panels are not seen by the SWT filter! */
	private static final KeyEventDispatcher AWT_DISPATCHER = new KeyEventDispatcher() {
		public boolean dispatchKeyEvent(KeyEvent e) {
			int code = e.getKeyCode();
			int key = code >= KeyEvent.VK_F2 && code <= KeyEvent.VK_F5 ? code - KeyEvent.VK_F1 + 1 : 0;
			if (e.getID() != KeyEvent.KEY_PRESSED || (key == 0 && code != KeyEvent.VK_ESCAPE)) {
				return false;
			}
			Window frame = embeddedFrame(e.getComponent());
			if (frame == null) {
				return false;
			}
			Display display = Display.getDefault();
			display.asyncExec(() -> {
				/* The active shell is not reliable if an AWT component has the focus! */
				Control control = embeddingControl(display, frame);
				boolean inFullscreen = isActive() && control != null && control.getShell() == shell;
				if (key == 0) {
					if (inFullscreen) {
						exit();
					}
					return;
				}
				CustomView v = inFullscreen ? view : (isActive() || control == null ? null : viewOf(control));
				if (v != null) {
					handleKey(key, v);
				}
			});
			return false;
		}
	};

	/* Installs the key listeners once (called when a custom view is created)! */
	static void install(Display display) {
		if (installed) {
			return;
		}
		installed = true;
		display.addFilter(SWT.KeyDown, KEY_FILTER);
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(AWT_DISPATCHER);
	}

	/* Returns the embedded frame (SWT_AWT) of the component or null! */
	private static Window embeddedFrame(java.awt.Component component) {
		Window window = component instanceof Window ? (Window) component : (component == null ? null : javax.swing.SwingUtilities.getWindowAncestor(component));
		for (Class<?> c = window == null ? null : window.getClass(); c != null; c = c.getSuperclass()) {
			if (c.getName().equals("sun.awt.EmbeddedFrame")) {
				return window;
			}
		}
		return null;
	}

	/* Returns the SWT composite of the embedded frame (fullscreen shell first) or null! */
	private static Control embeddingControl(Display display, Window frame) {
		if (isActive()) {
			Control c = embeddingControl(shell, frame);
			if (c != null) {
				return c;
			}
		}
		for (Shell s : display.getShells()) {
			Control c = embeddingControl(s, frame);
			if (c != null) {
				return c;
			}
		}
		return null;
	}

	private static Control embeddingControl(Composite parent, Window frame) {
		if (parent.isDisposed()) {
			return null;
		}
		if ((parent.getStyle() & SWT.EMBEDDED) != 0 && SWT_AWT.getFrame(parent) == frame) {
			return parent;
		}
		for (Control child : parent.getChildren()) {
			if (child instanceof Composite) {
				Control c = embeddingControl((Composite) child, frame);
				if (c != null) {
					return c;
				}
			}
		}
		return null;
	}

	/* Returns the custom view of the control or null! */
	private static CustomView viewOf(Control control) {
		for (Control c = control; c != null; c = c.getParent()) {
			Object v = c.getData(VIEW_KEY);
			if (v instanceof CustomView) {
				return (CustomView) v;
			}
		}
		return null;
	}

	/** Returns true if a custom view is displayed in fullscreen mode. */
	public static boolean isActive() {
		return shell != null && !shell.isDisposed();
	}

	/** Returns true if the given custom view is displayed in fullscreen mode. */
	public static boolean isFullscreen(CustomView customView) {
		return isActive() && view == customView;
	}

	/**
	 * Returns the monitors with the primary monitor first and the other monitors
	 * in the order of the system.
	 */
	public static Monitor[] getMonitors(Display display) {
		List<Monitor> list = new ArrayList<Monitor>();
		Monitor primary = display.getPrimaryMonitor();
		if (primary != null) {
			list.add(primary);
		}
		for (Monitor m : display.getMonitors()) {
			if (!same(m, primary)) {
				list.add(m);
			}
		}
		return list.toArray(new Monitor[0]);
	}

	/* The handles of the monitors can change (e.g., on macOS), so the bounds are compared! */
	private static boolean same(Monitor a, Monitor b) {
		return a != null && b != null && a.getBounds().equals(b.getBounds());
	}

	/* Lets the user select a monitor (null if canceled)! */
	private static Monitor selectMonitor(Display display) {
		Monitor[] monitors = getMonitors(display);
		Shell parent = isActive() ? shell : display.getActiveShell();
		ListDialog dialog = new ListDialog(parent);
		dialog.setTitle("Fullscreen");
		dialog.setMessage("Select the monitor for the fullscreen mode:");
		dialog.setContentProvider(ArrayContentProvider.getInstance());
		dialog.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				Monitor m = (Monitor) element;
				int index = java.util.Arrays.asList(monitors).indexOf(m);
				boolean current = isActive() && same(m, shellMonitor);
				Rectangle b = m.getBounds();
				String name = "Monitor " + (index + 1) + (index == 0 ? " (primary)" : "");
				return name + ": " + b.width + " x " + b.height + " at " + b.x + ", " + b.y + (current ? " - current" : "");
			}
		});
		dialog.setInput(monitors);
		Monitor selected = monitors[0];
		for (Monitor m : monitors) {
			if (isActive() && same(m, shellMonitor)) {
				selected = m;
			}
		}
		dialog.setInitialSelections(new Object[] { selected });
		dialog.setHeightInChars(Math.max(3, monitors.length));
		if (dialog.open() != org.eclipse.jface.window.Window.OK || dialog.getResult() == null || dialog.getResult().length == 0) {
			return null;
		}
		return (Monitor) dialog.getResult()[0];
	}

	/**
	 * Displays the content of the custom view in fullscreen mode on the monitor of
	 * the view. Only one view can be displayed in fullscreen mode at a time.
	 */
	public static void enter(CustomView customView) {
		enter(customView, null);
	}

	/**
	 * Displays the content of the custom view in fullscreen mode on the given
	 * monitor (the monitor of the view if null). Only one view can be displayed in
	 * fullscreen mode at a time.
	 */
	public static void enter(CustomView customView, Monitor monitor) {
		if (isActive() || customView == null) {
			return;
		}
		Composite c = customView.getCustomViewParent();
		if (c == null || c.isDisposed()) {
			return;
		}
		view = customView;
		content = c;
		oldParent = c.getParent();
		shell = new Shell(c.getDisplay(), MAC ? SWT.SHELL_TRIM : SWT.NO_TRIM | SWT.ON_TOP);
		shell.setText(customView.getPartName());
		shell.setLayout(new FillLayout());
		shell.addListener(SWT.Close, e -> restore());
		/* A minimized fullscreen shell would stay open in the background! */
		shell.addListener(SWT.Iconify, e -> e.display.asyncExec(CustomViewFullscreen::exit));
		if (MAC) {
			/* The native fullscreen mode can also be closed by macOS (green button, Ctrl+Cmd+F)! */
			entered = false;
			Shell s = shell;
			shell.addListener(SWT.Resize, e -> {
				if (s.getFullScreen()) {
					entered = true;
				} else if (entered && s == shell) {
					e.display.asyncExec(CustomViewFullscreen::exit);
				}
			});
		}
		content.setParent(shell);
		/* Shown in the view while the content is displayed in fullscreen mode! */
		placeholder = new Label(oldParent, SWT.CENTER | SWT.WRAP);
		placeholder.setText(MESSAGE);
		placeholder.addListener(SWT.MouseDoubleClick, e -> exit());
		oldParent.layout(true, true);
		shellMonitor = monitor != null ? monitor : oldParent.getMonitor();
		/* The native fullscreen mode of macOS uses the monitor of the shell! */
		shell.setBounds(shellMonitor.getBounds());
		if (MAC) {
			shell.open();
			shell.setFullScreen(true);
		} else {
			shell.setFullScreen(true);
			shell.open();
		}
		shell.layout(true, true);
		shell.forceActive();
		content.setFocus();
	}

	/**
	 * Displays the custom view in fullscreen mode on the given monitor or moves
	 * the fullscreen mode of the view to the monitor.
	 */
	public static void show(CustomView customView, Monitor monitor) {
		if (!isActive()) {
			enter(customView, monitor);
		} else if (isFullscreen(customView) && monitor != null && !same(monitor, shellMonitor)) {
			if (MAC) {
				/* A native fullscreen shell cannot be moved to another monitor! */
				exit();
				Display.getDefault().asyncExec(() -> enter(customView, monitor));
				return;
			}
			shellMonitor = monitor;
			shell.setFullScreen(false);
			shell.setBounds(monitor.getBounds());
			shell.setFullScreen(true);
			shell.layout(true, true);
			shell.forceActive();
			content.setFocus();
		}
	}

	/** Closes the fullscreen mode. */
	public static void exit() {
		if (isActive()) {
			shell.close();
		}
	}

	/** Toggles the fullscreen mode of the custom view. */
	public static void toggle(CustomView customView) {
		if (isFullscreen(customView)) {
			exit();
		} else if (!isActive()) {
			enter(customView);
		}
	}

	/*
	 * Called when the view is closed (before the children are disposed). The
	 * content is moved back to be disposed with the view. SWT_AWT removes its
	 * listeners from the shell of the content when it is disposed!
	 */
	static void viewDisposed(CustomView customView) {
		if (isFullscreen(customView)) {
			if (content != null && !content.isDisposed() && oldParent != null && !oldParent.isDisposed()) {
				content.setParent(oldParent);
			} else {
				oldParent = null;
			}
			content = null;
			shell.close();
		}
	}

	/* Moves the content back to the view! */
	private static void restore() {
		if (placeholder != null && !placeholder.isDisposed()) {
			placeholder.dispose();
		}
		if (content != null && !content.isDisposed()) {
			if (oldParent != null && !oldParent.isDisposed()) {
				content.setParent(oldParent);
				oldParent.layout(true, true);
			} else {
				/* The view has been closed! */
				content.dispose();
			}
		}
		view = null;
		content = null;
		oldParent = null;
		placeholder = null;
		shellMonitor = null;
		shell = null;
	}
}
