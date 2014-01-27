package com.eco.bio7.reditor.outline;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

public class REditorLabelProvider implements ILabelProvider {
  public String getText(Object element) {
    return ((REditorOutlineNode) element).getName();
  }

  public Image getImage(Object arg0) {
    return null;
  }

  public void addListener(ILabelProviderListener arg0) {
  }

  public void dispose() {
  }

  public boolean isLabelProperty(Object arg0, String arg1) {
    return false;
  }

  public void removeListener(ILabelProviderListener arg0) {
  }
}
