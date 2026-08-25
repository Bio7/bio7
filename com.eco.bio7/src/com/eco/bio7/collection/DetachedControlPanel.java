package com.eco.bio7.collection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import com.eco.bio7.compile.Model;
import com.eco.bio7.methods.Compiled;

/**
 * A standalone, perspective-independent floating control panel.
 * <p>
 * Unlike a Bio7 {@link CustomView} tab or an e4-detached view, this is a plain
 * top-level SWT {@link Shell}. It is not tied to any perspective or part model,
 * so it remains visible regardless of which Bio7 perspective is currently
 * active, and survives perspective switches untouched.
 * <p>
 * Must be created and used on the SWT UI thread.
 * 
 * Example:
 * 
 * private DetachedControlPanel controlPanel;
 * 
 * private void createGui() { Display d = Display.getDefault(); d.syncExec(() ->
 * { Shell parentShell = Display.getDefault().getActiveShell(); MonitorChooser
 * chooser = new MonitorChooser(parentShell); Monitor monitor =
 * chooser.chooseMonitor(); if (monitor == null) { monitor =
 * Display.getDefault().getPrimaryMonitor(); // fallback if canceled }
 * 
 * controlPanel = new DetachedControlPanel("Predator-Prey Control Panel", null,
 * monitor, 900, 600); Composite parent = controlPanel.getComposite(); new
 * PredatorPreySettings(parent, PredatorPrey3D.this); controlPanel.open(); }); }
 * 
 * Closing automatically with the custom view:
 * 
 * @Override public void close() { if (controlPanel != null &&
 *           !controlPanel.isDisposed()) { Display.getDefault().asyncExec(() ->
 *           controlPanel.close()); } super.close(); }
 */
public class DetachedControlPanel {

	private final Shell shell;

	/**
	 * Creates and opens a floating control panel, centered on the given monitor.
	 *
	 * @param title   the window title
	 * @param image   optional window icon, may be {@code null}
	 * @param monitor the monitor on which the window should appear
	 * @param width   desired window width, in pixels
	 * @param height  desired window height, in pixels
	 */
	public DetachedControlPanel(String title, Image image, Monitor monitor, int width, int height) {
		this.shell = new Shell(Display.getDefault(), SWT.SHELL_TRIM);
		shell.setText(title);
		if (image != null) {
			shell.setImage(image);
		}
		shell.setLayout(new FillLayout());

		positionOnMonitor(monitor, width, height);

		// Ensure the model tied to this session is closed when the panel is closed,
		// mirroring the behavior of CustomView's dispose listener.
		shell.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				Model model = Compiled.getModel();
				if (model != null) {
					try {
						model.close();
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		});
	}

	/**
	 * Returns the composite to add controls to. Callers should set a layout on it
	 * (or rely on the shell's default {@link FillLayout} if adding a single child)
	 * and populate it exactly as they would the composite returned by
	 * {@code CustomView.getComposite(...)}.
	 *
	 * @return the shell itself, usable as the parent {@link Composite} for further
	 *         widgets
	 */
	public Composite getComposite() {
		return shell;
	}

	/** Opens (shows) the floating window and gives it focus. */
	public void open() {
		shell.open();
	}

	/** Brings the floating window to the front, without changing focus behavior. */
	public void bringToFront() {
		if (!shell.isDisposed()) {
			shell.setActive();
			shell.forceActive();
		}
	}

	/** Closes and disposes the floating window. */
	public void close() {
		if (!shell.isDisposed()) {
			shell.close();
		}
	}

	public boolean isDisposed() {
		return shell.isDisposed();
	}

	/**
	 * Moves the window to the given monitor, centered, with the given size.
	 *
	 * @param monitor the target monitor
	 * @param width   desired window width, in pixels
	 * @param height  desired window height, in pixels
	 */
	public void positionOnMonitor(Monitor monitor, int width, int height) {
		Rectangle bounds = monitor.getBounds();
		int x = bounds.x + Math.max(0, (bounds.width - width) / 2);
		int y = bounds.y + Math.max(0, (bounds.height - height) / 2);
		shell.setBounds(x, y, width, height);
	}
}