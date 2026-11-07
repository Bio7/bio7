package com.eco.bio7.spatial;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.actions.ActionFactory;

/**
 * Undo and redo of the removed parts of the iso surface (the surface cut open,
 * see IsoSurface.undoRemoval) with the shortcuts of the workbench (Edit - Undo
 * and Redo: Cmd/Ctrl + Z, Shift + Cmd/Ctrl + Z): the global actions of the 3D
 * view and of the Options Space view. On macOS the shortcuts are keys of the
 * menu and don't reach the OpenGL window.
 */
final class SurfaceUndo {

	/* The time of the last undo or redo (a key handled by the menu and the OpenGL window only once) */
	private static long last;

	private SurfaceUndo() {
	}

	/** Sets the undo and redo actions of a view (the global actions of the workbench). */
	static void install(IViewSite site) {
		IActionBars bars = site.getActionBars();
		bars.setGlobalActionHandler(ActionFactory.UNDO.getId(), new Action("Undo Surface Removal") {
			@Override
			public void run() {
				undo();
			}
		});
		bars.setGlobalActionHandler(ActionFactory.REDO.getId(), new Action("Redo Surface Removal") {
			@Override
			public void run() {
				redo();
			}
		});
		bars.updateActionBars();
	}

	/** Undoes the last removal of the surface (once for a key). */
	static synchronized boolean undo() {
		IsoSurface iso = SpatialUtil.getIsoSurface();
		if (iso == null || !once()) {
			return false;
		}
		return iso.undoRemoval();
	}

	/** Redoes the last undone removal of the surface (once for a key). */
	static synchronized boolean redo() {
		IsoSurface iso = SpatialUtil.getIsoSurface();
		if (iso == null || !once()) {
			return false;
		}
		return iso.redoRemoval();
	}

	private static boolean once() {
		long now = System.currentTimeMillis();
		if (now - last < 150) {
			return false;
		}
		last = now;
		return true;
	}
}
