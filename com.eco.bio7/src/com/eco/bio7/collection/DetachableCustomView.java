package com.eco.bio7.collection;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Extension of Bio7's {@link CustomView} that can additionally open its tab as
 * a detached (floating) window, optionally placed on a specific monitor.
 * 
 * Example: 
 * DetachableCustomView view = new DetachableCustomView(); Monitor
 * secondMonitor = Display.getDefault().getMonitors()[1]; // or user-selected
 * Display.getDefault().syncExec(() -> { Composite parent =
 * view.getComposite("Predator-Prey Control Panel", null, secondMonitor, 900,
 * 600); parent.setLayout(new FillLayout()); new PredatorPreySettings(parent,
 * PredatorPrey3D.this); parent.layout(true); });
 */

public class DetachableCustomView extends CustomView {

	public DetachableCustomView() {
		super();
	}

	public DetachableCustomView(String name) {
		super(name);
	}

	/**
	 * Creates the tab as usual (docked in the Bio7 workbench), then immediately
	 * detaches it into its own floating window, centered on the given monitor.
	 *
	 * @param id      the id/name of the tab, same meaning as in
	 *                {@link CustomView#getComposite(String, Image)}
	 * @param image   optional tab image, may be {@code null}
	 * @param monitor the target monitor on which the detached window should appear
	 * @param width   desired width of the detached window, in pixels
	 * @param height  desired height of the detached window, in pixels
	 * @return the composite of the tab (still valid to add controls to, exactly as
	 *         with the regular docked tab)
	 */
	public Composite getComposite(String id, Image image, final Monitor monitor, final int width, final int height) {
		Composite composite = super.getComposite(id, image);

		// Defer the detach until the tab/part is fully created and laid out
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				detach(id, monitor, width, height);
			}
		});

		return composite;
	}

	/**
	 * Detaches the tab identified by the given secondary id into a floating window
	 * on the specified monitor.
	 * <p>
	 * Must run on the SWT UI thread. Relies on {@link EModelService#detach} (public
	 * e4 API), which places a part into its own top-level shell at the given screen
	 * coordinates.
	 *
	 * @param secondaryId the tab id, as passed to {@code getComposite}
	 * @param monitor     the target monitor
	 * @param width       desired window width, in pixels
	 * @param height      desired window height, in pixels
	 */
	private void detach(String secondaryId, Monitor monitor, int width, int height) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();

		IViewReference ref = page.findViewReference("com.eco.bio7.custom_controls", secondaryId);
		if (ref == null) {
			System.err.println("Could not find view reference to detach for id '" + secondaryId + "'.");
			return;
		}

		IViewPart viewPart = ref.getView(false);
		if (viewPart == null) {
			System.err.println("View part for id '" + secondaryId + "' is not initialized yet.");
			return;
		}

		MPart part = viewPart.getSite().getService(MPart.class);
		if (part == null) {
			System.err.println("Could not resolve the e4 MPart for tab '" + secondaryId + "'.");
			return;
		}

		EModelService modelService = window.getService(EModelService.class);
		if (modelService == null) {
			System.err.println("EModelService is not available.");
			return;
		}

		// Center the detached window on the target monitor
		Rectangle bounds = monitor.getBounds();
		int x = bounds.x + Math.max(0, (bounds.width - width) / 2);
		int y = bounds.y + Math.max(0, (bounds.height - height) / 2);

		modelService.detach(part, x, y, width, height);
	}
}