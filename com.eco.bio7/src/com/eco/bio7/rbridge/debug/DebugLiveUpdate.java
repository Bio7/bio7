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
package com.eco.bio7.rbridge.debug;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

/**
 * Changes during an R debug session: a breakpoint is set, changed or removed, or
 * an R file is saved. The changed files are collected and applied when R waits
 * in the browser (DebugProgress): a new debugged copy, its functions and classes
 * are defined again (the running code keeps the old version).
 */
final class DebugLiveUpdate {

	private static final Set<Path> PENDING = ConcurrentHashMap.newKeySet();
	private static IResourceChangeListener listener;

	private DebugLiveUpdate() {
	}

	static synchronized void start() {
		PENDING.clear();
		if (listener != null) {
			return;
		}
		listener = DebugLiveUpdate::changed;
		ResourcesPlugin.getWorkspace().addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
	}

	static synchronized void stop() {
		if (listener != null) {
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(listener);
			listener = null;
		}
		PENDING.clear();
	}

	/* The changed files (and removes them from the pending ones) */
	static List<Path> take() {
		List<Path> out = new ArrayList<>(PENDING);
		PENDING.removeAll(out);
		return out;
	}

	/* The breakpoints of an R file of the workspace: line -> breakpoint */
	static Map<Integer, RBreakpoint> breakpoints(Path file) {
		Map<Integer, RBreakpoint> map = new TreeMap<>();
		for (IFile f : ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(file.toUri())) {
			try {
				for (IMarker m : f.findMarkers(RBreakpoint.MARKER, false, IResource.DEPTH_ZERO)) {
					int line = m.getAttribute(IMarker.LINE_NUMBER, -1);
					if (line > 0) {
						map.put(line, RBreakpoint.of(m));
					}
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return map;
	}

	private static void changed(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		if (delta == null) {
			return;
		}
		boolean any = false;
		try {
			List<Path> found = new ArrayList<>();
			delta.accept(d -> {
				IResource r = d.getResource();
				if (r.getType() != IResource.FILE) {
					return true;
				}
				String ext = r.getFileExtension() == null ? "" : r.getFileExtension().toLowerCase(java.util.Locale.ROOT);
				if (!(ext.equals("r") || ext.equals("rmd") || ext.equals("qmd") || ext.equals("rmarkdown")) || r.getLocation() == null) {
					return false;
				}
				boolean breakpoint = false;
				for (IMarkerDelta m : d.getMarkerDeltas()) {
					if (RBreakpoint.MARKER.equals(m.getType())) {
						breakpoint = true;
					}
				}
				boolean content = d.getKind() == IResourceDelta.CHANGED && (d.getFlags() & IResourceDelta.CONTENT) != 0;
				if (breakpoint || content) {
					found.add(r.getLocation().toFile().toPath().toAbsolutePath());
				}
				return false;
			});
			any = !found.isEmpty();
			PENDING.addAll(found);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		if (any) {
			DebugProgress.liveUpdateLater();
		}
	}
}
