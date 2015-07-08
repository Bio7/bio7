package com.eco.bio7.wizards;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

public class WizardUtil {
	public static IContainer getContainer(final String text) {
		final Path path = new Path(text);

		final IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);

		if (resource instanceof IContainer) {
			return (IContainer) resource;
		} else {
			return null;
		}

	}

}
