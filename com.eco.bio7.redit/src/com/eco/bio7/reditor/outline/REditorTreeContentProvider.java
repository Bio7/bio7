package com.eco.bio7.reditor.outline;

import java.util.Vector;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class REditorTreeContentProvider implements ITreeContentProvider{
  public Object[] getChildren(Object parentElement) {
    Vector subcats = ((REditorOutlineNode) parentElement).getSubCategories();
    return subcats == null ? new Object[0] : subcats.toArray();
  }

  public Object getParent(Object element) {
    return ((REditorOutlineNode) element).getParent();
  }

  public boolean hasChildren(Object element) {
    return ((REditorOutlineNode) element).getSubCategories() != null;
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