package com.eco.bio7.browser.editor.outline;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class HTMLEditorLabelProvider implements ILabelProvider {
	private Image publicMethodIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/logo.gif"));
	private Image publicFieldIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/logo.gif"));
	private Image importIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/logo.gif"));
	private Image baseNode = new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/logo.gif"));

	public String getText(Object element) {
		return ((HTMLEditorOutlineNode) element).getName();
	}

	public Image getImage(Object element) {
		Image im = null;
		if (element instanceof HTMLEditorOutlineNode) {
			HTMLEditorOutlineNode cm = (HTMLEditorOutlineNode) element;
			if (cm.getType().equals("function")) {
				im = publicMethodIcon;

			}

			else if (cm.getType().equals("variable")) {
				im = publicFieldIcon;
			}

			else if (cm.getType().equals("library")) {
				im = importIcon;
			} else if (cm.getType().equals("base")) {
				im = baseNode;
			}

		}

		return im;
	}

	public void addListener(ILabelProviderListener arg0) {
	}

	public void dispose() {
		publicFieldIcon.dispose();
		publicMethodIcon.dispose();
		importIcon.dispose();
		baseNode.dispose();
	}

	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

	public void removeListener(ILabelProviderListener arg0) {
	}
}
