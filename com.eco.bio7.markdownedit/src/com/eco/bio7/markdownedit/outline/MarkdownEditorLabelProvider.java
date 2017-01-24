package com.eco.bio7.markdownedit.outline;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import com.eco.bio7.markdownedit.Activator;

public class MarkdownEditorLabelProvider implements ILabelProvider {

	private Image rMarkdownIcon = Activator.getImageDescriptor("/icons/template_obj.png").createImage();

	public String getText(Object element) {
		return ((MarkdownEditorOutlineNode) element).getName();
	}

	/* Not used at the moment! */
	public Image getImage(Object element) {
		Image im = null;
		if (element instanceof MarkdownEditorOutlineNode) {
			MarkdownEditorOutlineNode cm = (MarkdownEditorOutlineNode) element;

			if (cm.getType().equals("RMarkdown")) {
				im = rMarkdownIcon;

			}

			else {
				im = null;
			}

		}

		return im;
		// return null;
	}

	public void addListener(ILabelProviderListener arg0) {
	}

	public void dispose() {
		rMarkdownIcon.dispose();
		/*
		 * publicMethodIcon.dispose(); importIcon.dispose(); baseNode.dispose();
		 */
	}

	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

	public void removeListener(ILabelProviderListener arg0) {
	}
}
