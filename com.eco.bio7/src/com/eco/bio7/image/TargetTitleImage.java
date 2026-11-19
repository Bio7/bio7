package com.eco.bio7.image;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchPart;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.rcp.IconColorModel;
import com.eco.bio7.util.IconColors;

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

	/* The Python icon, recolored with the icon colors of the theme (if selected) */
	static Image python() {
		java.net.URL recolored = IconColors.recolored("com.eco.bio7", "/icons/views/pythonaction.svg");
		String key = recolored == null ? PYTHON : PYTHON + "." + recolored;
		ImageRegistry registry = Bio7Plugin.getDefault().getImageRegistry();
		Image image = registry.get(key);
		if (image == null) {
			registry.put(key, recolored == null ? Bio7Plugin.getImageDescriptor("/icons/views/pythonaction.svg") : ImageDescriptor.createFromURL(recolored));
			image = registry.get(key);
		}
		return image;
	}

	/*
	 * The R icon: the default icon of the view, recolored with the icon colors of
	 * the theme (if selected), else the given default icon (of plugin.xml).
	 */
	static Image r(IWorkbenchPart part, Image defaultImage) {
		Image recolored = IconColorModel.recoloredTabIcon(part);
		return recolored != null ? recolored : defaultImage;
	}
}
