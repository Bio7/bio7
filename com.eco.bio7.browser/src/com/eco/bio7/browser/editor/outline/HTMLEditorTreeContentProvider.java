package com.eco.bio7.browser.editor.outline;

import java.util.Vector;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class HTMLEditorTreeContentProvider implements ITreeContentProvider{
  public Object[] getChildren(Object parentElement) {
    Vector subcats = ((HTMLEditorOutlineNode) parentElement).getSubCategories();
    return subcats == null ? new Object[0] : subcats.toArray();
  }

  public Object getParent(Object element) {
    return ((HTMLEditorOutlineNode) element).getParent();
  }

  public boolean hasChildren(Object element) {
    return ((HTMLEditorOutlineNode) element).getSubCategories() != null;
  }

  public Object[] getElements(Object inputElement) {
    if (inputElement != null && inputElement instanceof Vector) {
      return ((Vector) inputElement).toArray();
    }
    return new Object[0];
  }

  public void dispose() {
  }

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
  }
}