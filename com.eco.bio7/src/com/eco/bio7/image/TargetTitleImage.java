package com.eco.bio7.image;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

import com.eco.bio7.Bio7Plugin;

/*
 * The icon of the view tab for the transfer target (R or Python) of the
 * Image-Methods and Geometry Selections views. The images are shared (the image
 * registry of the plugin disposes them), the default icon (R) is the one of
 * plugin.xml.
 */
final class TargetTitleImage {

	private static final String PYTHON = "com.eco.bio7.image.title.python";

	private TargetTitleImage() {
	}

	static Image python() {
		ImageRegistry registry = Bio7Plugin.getDefault().getImageRegistry();
		Image image = registry.get(PYTHON);
		if (image == null) {
			registry.put(PYTHON, Bio7Plugin.getImageDescriptor("/icons/views/pythonaction.svg"));
			image = registry.get(PYTHON);
		}
		return image;
	}
}
