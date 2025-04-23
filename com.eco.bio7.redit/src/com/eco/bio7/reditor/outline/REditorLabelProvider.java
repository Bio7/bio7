package com.eco.bio7.reditor.outline;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import com.eco.bio7.reditor.Bio7REditorPlugin;

public class REditorLabelProvider implements ILabelProvider {
	/*
	 * ISharedImages images = JavaUI.getSharedImages(); Image image =
	 * images.getImage(ISharedImages.IMG_WHATEVER);
	 */
	private Image publicMethodIcon = Bio7REditorPlugin.getImageDescriptor("/icons/methpub_obj.png").createImage();
	
	private Image publicAnonymousMethodIcon = Bio7REditorPlugin.getImageDescriptor("/icons/methpro_obj.png").createImage();

	private Image publicFieldIcon = Bio7REditorPlugin.getImageDescriptor("/icons/field_public_obj.png").createImage();

	private Image publicFieldLoopIcon = Bio7REditorPlugin.getImageDescriptor("/icons/field_protected_obj.png").createImage();

	private Image publicMethodCallFieldIcon = Bio7REditorPlugin.getImageDescriptor("/icons/field_private_obj.png").createImage();

	private Image importIcon = Bio7REditorPlugin.getImageDescriptor("/icons/imp_obj.png").createImage();
	
	private Image baseNode = Bio7REditorPlugin.getImageDescriptor("/icons/file_obj.png").createImage();
	
	private Image s3ClassIcon = Bio7REditorPlugin.getImageDescriptor("/icons/innerclass_public_obj.png").createImage();

	private Image s4ClassIcon = Bio7REditorPlugin.getImageDescriptor("/icons/innerclass_private_obj.png").createImage();

	private Image refClassIcon = Bio7REditorPlugin.getImageDescriptor("/icons/innerclass_protected_obj.png").createImage();

	private Image r6ClassIcon = Bio7REditorPlugin.getImageDescriptor("/icons/innerclass_default_obj.png").createImage();

	public String getText(Object element) {
		return ((REditorOutlineNode) element).getName();
	}

	public Image getImage(Object element) {
		Image im = null;
		if (element instanceof REditorOutlineNode) {
			REditorOutlineNode cm = (REditorOutlineNode) element;
			if (cm.getType().equals("function")) {
				im = publicMethodIcon;
			} else if (cm.getType().equals("anonymousFunction")) {
				im = publicAnonymousMethodIcon;
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
		publicAnonymousMethodIcon.dispose();
	}

	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

	public void removeListener(ILabelProviderListener arg0) {
	}
}
