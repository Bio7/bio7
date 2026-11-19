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
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import com.eco.bio7.util.FullscreenMonitors;

/**
 * Displays the content of a {@link CustomView} in a fullscreen shell. The
 * content composite of the view is moved into the shell and back to the view
 * when the fullscreen mode is closed. Several views can be displayed in
 * fullscreen mode at the same time, each on its own monitor (e.g., a custom
 * view on the primary and a plot on the secondary monitor). A view displayed on
 * a monitor which already shows another view replaces it. The keys (in a custom
 * view or in its fullscreen mode):
 * <ul>
 * <li>F2: fullscreen on the primary monitor (or exit)</li>
 * <li>F3: fullscreen on the secondary monitor</li>
 * <li>F4: fullscreen on the tertiary monitor</li>
 * <li>F5: select the monitor</li>
 * <li>ESC: exit</li>
 * </ul>
 * F3, F4 and F5 move the fullscreen mode to another monitor and exit if the
 * view is already displayed on the monitor. The monitors are numbered with the
 * primary monitor first (see {@link #getMonitors(Display)}). The keys are the
 * same for all fullscreen modes of Bio7 (see {@link FullscreenMonitors}). All
 * methods must be called in the SWT UI thread!
 */
public class CustomViewFullscreen {

	/* The key of the view in the data of the content composite! */
	static final String VIEW_KEY = "com.eco.bio7.collection.CustomView";

	private static final String MESSAGE = "Displayed in fullscreen mode (press F2 or ESC to exit, F3, F4 or F5 to change the monitor).";

	/* The fullscreen mode of one view */
	private static final class Session {
		Shell shell;
		CustomView view;
		Composite content;
		Composite oldParent;
		Label placeholder;
		Monitor monitor;
		/* True when the native fullscreen mode of macOS has been entered (the shell is resized)! */
		boolean entered;
	}

	/* The views in fullscreen mode (in the order they were entered) */
	private static final Map<CustomView, Session> sessions = new LinkedHashMap<>();

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
			Session s = sessionOf(control.getShell());
			if (e.keyCode == SWT.ESC) {
				if (s != null) {
					/* The ESC key is also forwarded to the content! */
					CustomView v = s.view;
					Display.getCurrent().asyncExec(() -> exit(v));
				}
				return;
			}
			int key = FullscreenMonitors.functionKey(e.keyCode);
			CustomView v = s != null ? s.view : viewOf(control);
			if (key > 0 && v != null) {
				e.doit = false;
				/* Not in the filter (F5 opens a dialog)! */
				Display.getCurrent().asyncExec(() -> handleKey(key, v));
			}
		}
	};

	/* The action of the keys F2 to F5 for the view (displayed in fullscreen mode or not)! */
	private static void handleKey(int key, CustomView v) {
		Session s = sessions.get(v);
		if (s == null && !v.isFullscreenEnabled()) {
			return;
		}
		/* F2: exit (in fullscreen mode) or the primary monitor */
		if (key == FullscreenMonitors.KEY_PRIMARY && s != null) {
			exit(v);
			return;
		}
		Display display = Display.getDefault();
		Monitor monitor = FullscreenMonitors.monitorForKey(key, display, s != null ? s.shell : null, s != null ? s.monitor : null);
		if (monitor == null) {
			return;
		}
		if (s != null && same(monitor, s.monitor)) {
			exit(v);
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
				if (control == null) {
					return;
				}
				Session s = sessionOf(control.getShell());
				if (key == 0) {
					if (s != null) {
						exit(s.view);
					}
					return;
				}
				CustomView v = s != null ? s.view : viewOf(control);
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

	/* Returns the SWT composite of the embedded frame (all shells, the fullscreen shells, too) or null! */
	private static Control embeddingControl(Display display, Window frame) {
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

	/* The fullscreen mode of the shell or null */
	private static Session sessionOf(Shell shell) {
		for (Session s : sessions.values()) {
			if (s.shell == shell) {
				return s;
			}
		}
		return null;
	}

	/** Returns true if a custom view is displayed in fullscreen mode. */
	public static boolean isActive() {
		return !sessions.isEmpty();
	}

	/** Returns true if the given custom view is displayed in fullscreen mode. */
	public static boolean isFullscreen(CustomView customView) {
		Session s = customView == null ? null : sessions.get(customView);
		return s != null && s.shell != null && !s.shell.isDisposed();
	}

	/**
	 * Returns the monitors with the primary monitor first and the other monitors
	 * in the order of the system.
	 */
	public static Monitor[] getMonitors(Display display) {
		return FullscreenMonitors.getMonitors(display);
	}

	/* The handles of the monitors can change (e.g., on macOS), so the bounds are compared! */
	private static boolean same(Monitor a, Monitor b) {
		return FullscreenMonitors.same(a, b);
	}

	/* Closes the fullscreen mode of another view on the monitor (one view per monitor) */
	private static void freeMonitor(Monitor monitor, CustomView except) {
		for (Session s : new ArrayList<>(sessions.values())) {
			if (s.view != except && same(s.monitor, monitor)) {
				exit(s.view);
			}
		}
	}

	/**
	 * Displays the content of the custom view in fullscreen mode on the monitor of
	 * the view. Another view in fullscreen mode on this monitor is closed.
	 */
	public static void enter(CustomView customView) {
		enter(customView, null);
	}

	/**
	 * Displays the content of the custom view in fullscreen mode on the given
	 * monitor (the monitor of the view if null). Another view in fullscreen mode on
	 * this monitor is closed.
	 */
	public static void enter(CustomView customView, Monitor monitor) {
		if (customView == null || isFullscreen(customView)) {
			return;
		}
		Composite c = customView.getCustomViewParent();
		if (c == null || c.isDisposed()) {
			return;
		}
		Monitor target = monitor != null ? monitor : c.getParent().getMonitor();
		freeMonitor(target, customView);
		Session s = new Session();
		s.view = customView;
		s.content = c;
		s.oldParent = c.getParent();
		s.monitor = target;
		Shell shell = new Shell(c.getDisplay(), MAC ? SWT.SHELL_TRIM : SWT.NO_TRIM | SWT.ON_TOP);
		s.shell = shell;
		sessions.put(customView, s);
		shell.setText(customView.getPartName());
		shell.setLayout(new FillLayout());
		shell.addListener(SWT.Close, _ -> restore(s));
		/* A minimized fullscreen shell would stay open in the background! */
		shell.addListener(SWT.Iconify, e -> e.display.asyncExec(() -> exit(customView)));
		if (MAC) {
			/* The native fullscreen mode can also be closed by macOS (green button, Ctrl+Cmd+F)! */
			shell.addListener(SWT.Resize, e -> {
				if (shell.getFullScreen()) {
					s.entered = true;
				} else if (s.entered && sessions.get(customView) == s) {
					e.display.asyncExec(() -> exit(customView));
				}
			});
		}
		s.content.setParent(shell);
		/* Shown in the view while the content is displayed in fullscreen mode! */
		s.placeholder = new Label(s.oldParent, SWT.CENTER | SWT.WRAP);
		s.placeholder.setText(MESSAGE);
		s.placeholder.addListener(SWT.MouseDoubleClick, _ -> exit(customView));
		s.oldParent.layout(true, true);
		/* The native fullscreen mode of macOS uses the monitor of the shell! */
		shell.setBounds(target.getBounds());
		if (MAC) {
			shell.open();
			shell.setFullScreen(true);
		} else {
			shell.setFullScreen(true);
			shell.open();
		}
		shell.layout(true, true);
		shell.forceActive();
		s.content.setFocus();
	}

	/**
	 * Displays the custom view in fullscreen mode on the given monitor or moves
	 * the fullscreen mode of the view to the monitor.
	 */
	public static void show(CustomView customView, Monitor monitor) {
		Session s = sessions.get(customView);
		if (s == null) {
			enter(customView, monitor);
		} else if (monitor != null && !same(monitor, s.monitor)) {
			freeMonitor(monitor, customView);
			if (MAC) {
				/* A native fullscreen shell cannot be moved to another monitor! */
				exit(customView);
				Display.getDefault().asyncExec(() -> enter(customView, monitor));
				return;
			}
			s.monitor = monitor;
			s.shell.setFullScreen(false);
			s.shell.setBounds(monitor.getBounds());
			s.shell.setFullScreen(true);
			s.shell.layout(true, true);
			s.shell.forceActive();
			s.content.setFocus();
		}
	}

	/** Closes the fullscreen mode of all views. */
	public static void exit() {
		for (CustomView v : new ArrayList<>(sessions.keySet())) {
			exit(v);
		}
	}

	/** Closes the fullscreen mode of the view. */
	public static void exit(CustomView customView) {
		Session s = sessions.get(customView);
		if (s != null) {
			if (s.shell != null && !s.shell.isDisposed()) {
				s.shell.close();
			} else {
				restore(s);
			}
		}
	}

	/** Toggles the fullscreen mode of the custom view (on the primary monitor like F2). */
	public static void toggle(CustomView customView) {
		if (isFullscreen(customView)) {
			exit(customView);
		} else {
			Monitor[] monitors = getMonitors(Display.getDefault());
			enter(customView, monitors.length > 0 ? monitors[0] : null);
		}
	}

	/*
	 * Called when the view is closed (before the children are disposed). The
	 * content is moved back to be disposed with the view. SWT_AWT removes its
	 * listeners from the shell of the content when it is disposed!
	 */
	static void viewDisposed(CustomView customView) {
		Session s = sessions.get(customView);
		if (s != null) {
			if (s.content != null && !s.content.isDisposed() && s.oldParent != null && !s.oldParent.isDisposed()) {
				s.content.setParent(s.oldParent);
			} else {
				s.oldParent = null;
			}
			s.content = null;
			exit(customView);
		}
	}

	/* Moves the content back to the view! */
	private static void restore(Session s) {
		if (sessions.get(s.view) == s) {
			sessions.remove(s.view);
		}
		if (s.placeholder != null && !s.placeholder.isDisposed()) {
			s.placeholder.dispose();
		}
		if (s.content != null && !s.content.isDisposed()) {
			if (s.oldParent != null && !s.oldParent.isDisposed()) {
				s.content.setParent(s.oldParent);
				s.oldParent.layout(true, true);
			} else {
				/* The view has been closed! */
				s.content.dispose();
			}
		}
		s.content = null;
		s.oldParent = null;
		s.placeholder = null;
	}
}
