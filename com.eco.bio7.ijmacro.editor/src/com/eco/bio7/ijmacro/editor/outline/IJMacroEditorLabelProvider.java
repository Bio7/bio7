package com.eco.bio7.ijmacro.editor.outline;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;

public class IJMacroEditorLabelProvider implements ILabelProvider {
	/*
	 * ISharedImages images = JavaUI.getSharedImages(); Image image =
	 * images.getImage(ISharedImages.IMG_WHATEVER);
	 */
	private Image publicMethodIcon = IJMacroEditorPlugin.getImageDescriptor("/icons/methpub_obj.png").createImage();

	private Image publicFieldIcon = IJMacroEditorPlugin.getImageDescriptor("/icons/field_public_obj.png").createImage();

	private Image globalFieldIcon = IJMacroEditorPlugin.getImageDescriptor("/icons/field_private_obj.png")
			.createImage();

	private Image publicMacroIcon = IJMacroEditorPlugin.getImageDescriptor("/icons/brkp_obj.png").createImage();

	// private Image publicFieldLoopIcon =
	// Bio7REditorPlugin.getImageDescriptor("/icons/field_protected_obj.png").createImage();

	public String getText(Object element) {
		return ((IJMacroEditorOutlineNode) element).getName();
	}

	public Image getImage(Object element) {
		Image im = null;
		if (element instanceof IJMacroEditorOutlineNode) {
			IJMacroEditorOutlineNode cm = (IJMacroEditorOutlineNode) element;
			if (cm.getType().equals("function")) {
				im = publicMethodIcon;
			} else if (cm.getType().equals("variable")) {
				im = publicFieldIcon;
			} else if (cm.getType().equals("globalvariable")) {
				im = globalFieldIcon;
			} else if (cm.getType().equals("macro")) {
				im = publicMacroIcon;
			}

		}

		return im;
	}

	public void addListener(ILabelProviderListener arg0) {
	}

	public void dispose() {

		publicFieldIcon.dispose();

		publicMethodIcon.dispose();

		globalFieldIcon.dispose();

		publicMacroIcon.dispose();

	}

	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

	public void removeListener(ILabelProviderListener arg0) {
	}
}
