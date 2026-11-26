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
package com.eco.bio7.floweditor.model;

import java.io.File;
import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;

/**
 * The files of the components of a flow. A component stores its file as:
 * <ul>
 * <li>a workspace path (/Project/folder/file.R, a file dropped from the
 * workspace),</li>
 * <li>a path relative to the folder of the flow file (folder/file.R, a file
 * dropped from outside the workspace below the folder of the flow),</li>
 * <li>an absolute path (other files).</li>
 * </ul>
 * The file is searched (in this order): an absolute file which exists, the
 * location of the workspace resource (also a project of a Git repository which
 * is not in the workspace folder or a linked folder), the path relative to the
 * folder of the flow, the workspace path below a folder with the name of the
 * project which contains the flow (the flow and the files in a cloned
 * repository which is not imported). Else the location in the workspace folder
 * (the former default; the file doesn't exist).
 */
public final class FlowPaths {

	private FlowPaths() {
	}

	/**
	 * The file system location of a file of a flow component.
	 *
	 * @param stored the path of the component.
	 * @param flow   the flow file (may be null).
	 * @return the absolute path (the file may not exist).
	 */
	public static File location(String stored, File flow) {
		if (stored == null || stored.isBlank()) {
			return null;
		}
		String text = stored.trim();
		IPath path = Path.fromOSString(text);
		File asFile = new File(text);
		/* An absolute file (e.g., dropped from outside the workspace) */
		if (asFile.isAbsolute() && asFile.exists() && !isWorkspacePath(path)) {
			return asFile;
		}
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		/* A workspace resource: its real location (an imported project of a Git repository, a linked folder) */
		if (path.isAbsolute() && path.segmentCount() > 1 && path.getDevice() == null) {
			IResource member = root.findMember(path);
			if (member != null && member.getLocation() != null) {
				return member.getLocation().toFile();
			}
		}
		File flowDir = flow != null ? flow.getAbsoluteFile().getParentFile() : null;
		if (flowDir != null) {
			/* Relative to the folder of the flow */
			if (!path.isAbsolute()) {
				File relative = new File(flowDir, text);
				if (relative.exists()) {
					return normalize(relative);
				}
			}
			/* /Project/a/b.R: a folder named Project above the flow which contains a/b.R (a cloned repository) */
			if (path.isAbsolute() && path.segmentCount() > 1 && path.getDevice() == null) {
				String project = path.segment(0);
				String rest = path.removeFirstSegments(1).toOSString();
				for (File dir = flowDir; dir != null; dir = dir.getParentFile()) {
					File candidate = new File(dir, rest);
					if (dir.getName().equals(project) && candidate.exists()) {
						return candidate;
					}
				}
				/* The project folder may have another name: up to the folder of the project or repository */
				for (File dir = flowDir; dir != null; dir = dir.getParentFile()) {
					File candidate = new File(dir, rest);
					if (candidate.exists()) {
						return candidate;
					}
					if (new File(dir, ".project").exists() || new File(dir, ".git").exists()) {
						break;
					}
				}
			}
		}
		/* An absolute file which doesn't exist (e.g., not available any more) */
		if (asFile.isAbsolute() && !isWorkspacePath(path)) {
			return asFile;
		}
		/* The former default: the path in the workspace folder */
		return new File(root.getLocation().toFile(), path.isAbsolute() ? text.substring(1) : text);
	}

	/** The location as a string (with '/'), null if no path is stored. */
	public static String locationString(String stored, File flow) {
		File file = location(stored, flow);
		return file != null ? file.getPath().replace('\\', '/') : null;
	}

	/** The workspace file of the location (null if outside the workspace). */
	public static IFile workspaceFile(File location) {
		if (location == null) {
			return null;
		}
		IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(location.toURI());
		for (IFile file : files) {
			if (file.exists()) {
				return file;
			}
		}
		return files.length > 0 ? files[0] : null;
	}

	/** The flow file of an editor input (in the workspace or a file opened from the file system), else null. */
	public static File flowFile(IEditorInput input) {
		if (input instanceof IFileEditorInput fileInput) {
			IPath location = fileInput.getFile().getLocation();
			return location != null ? location.toFile() : null;
		}
		if (input instanceof IURIEditorInput uriInput) {
			URI uri = uriInput.getURI();
			if (uri != null && "file".equals(uri.getScheme())) {
				return new File(uri);
			}
		}
		return null;
	}

	/**
	 * The path which a component stores for a dropped file: the workspace path
	 * (file in the workspace and the flow in the workspace or no flow), else the
	 * path relative to the folder of the flow (the file below it), else the
	 * absolute path.
	 */
	public static String storedPath(String location, File flow, boolean flowInWorkspace) {
		File file = new File(location);
		IFile ifile = workspaceFile(file);
		if (ifile != null && (flowInWorkspace || flow == null)) {
			return ifile.getFullPath().toString();
		}
		if (flow != null) {
			File flowDir = flow.getAbsoluteFile().getParentFile();
			if (flowDir != null && isBelow(file.getAbsoluteFile(), flowDir)) {
				return flowDir.toPath().relativize(file.getAbsoluteFile().toPath()).toString().replace('\\', '/');
			}
		}
		return file.getAbsolutePath();
	}

	/* /Project/... with an existing project (a workspace path, not a file system path) */
	private static boolean isWorkspacePath(IPath path) {
		if (!path.isAbsolute() || path.getDevice() != null || path.segmentCount() < 1) {
			return false;
		}
		return ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0)).exists();
	}

	private static boolean isBelow(File file, File dir) {
		return file.toPath().normalize().startsWith(dir.toPath().normalize());
	}

	private static File normalize(File file) {
		return file.toPath().toAbsolutePath().normalize().toFile();
	}
}
