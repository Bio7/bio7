package com.eco.bio7.reditor.outline;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class REditorLabelProvider implements ILabelProvider {
	private Image publicMethodIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/methpub_obj.png"));
	private Image publicFieldIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/field_public_obj.png"));
	private Image publicFieldLoopIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/field_protected_obj.png"));
	private Image publicMethodCallFieldIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/field_private_obj.png"));
	private Image importIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/imp_obj.png"));
	private Image baseNode = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/file_obj.png"));
	private Image s3ClassIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/innerclass_public_obj.png"));
	private Image s4ClassIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/innerclass_private_obj.png"));
	private Image refClassIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/innerclass_protected_obj.png"));
	private Image r6ClassIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/innerclass_default_obj.png"));

	public String getText(Object element) {
		return ((REditorOutlineNode) element).getName();
	}

	public Image getImage(Object element) {
		Image im = null;
		if (element instanceof REditorOutlineNode) {
			REditorOutlineNode cm = (REditorOutlineNode) element;
			if (cm.getType().equals("function")) {
				im = publicMethodIcon;
			} else if (cm.getType().equals("variable")) {
				im = publicFieldIcon;
			} else if (cm.getType().equals("loopVariable")) {
				im = publicFieldLoopIcon;
			} else if (cm.getType().equals("methodCallField")) {
				im = publicMethodCallFieldIcon;
			} else if (cm.getType().equals("s3Class")) {
				im = s3ClassIcon;
			} else if (cm.getType().equals("s4Class")) {
				im = s4ClassIcon;
			} else if (cm.getType().equals("refClass")) {
				im = refClassIcon;
			} else if (cm.getType().equals("r6Class")) {
				im = r6ClassIcon;
			} else if (cm.getType().equals("library")) {
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
		s3ClassIcon.dispose();
		s4ClassIcon.dispose();
		refClassIcon.dispose();
		r6ClassIcon.dispose();
		publicFieldIcon.dispose();
		publicFieldLoopIcon.dispose();
		publicMethodCallFieldIcon.dispose();
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
