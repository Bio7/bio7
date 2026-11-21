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
package com.eco.bio7.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * The creation of the projects of the wizards in one workspace operation: the
 * views (e.g., the Project Explorer) get one change with the finished project
 * instead of the steps (created, opened, a nature added, folders, classpath).
 * Single steps can make the Project Explorer show a project twice (e.g., as a
 * resource and again as a Java project).
 */
public final class ProjectCreation {

	private ProjectCreation() {
	}

	/** Runs the code as one workspace operation (one change for the views). */
	public static void run(IWorkspaceRunnable code, IProgressMonitor monitor) throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.run(code, workspace.getRoot(), IWorkspace.AVOID_UPDATE, monitor);
	}

	/**
	 * Creates and opens a project with its natures (null or empty: none) in one
	 * workspace operation.
	 */
	public static IProject createAndOpen(String name, String[] natures, IProgressMonitor monitor) throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject(name);
		run(m -> {
			IProjectDescription description = workspace.newProjectDescription(name);
			if (natures != null && natures.length > 0) {
				description.setNatureIds(natures);
			}
			project.create(description, m);
			project.open(m);
		}, monitor);
		return project;
	}
}
