package com.eco.bio7.reditor.outline;

import java.util.Vector;

public class REditorOutlineNode {
  private String name;

  private Vector subCategories;

  private REditorOutlineNode parent;

  public REditorOutlineNode(String name, REditorOutlineNode parent) {
    this.name = name;
    this.parent = parent;
    if (parent != null)
      parent.addSubCategory(this);
  }

  public Vector getSubCategories() {
    return subCategories;
  }

  private void addSubCategory(REditorOutlineNode subcategory) {
    if (subCategories == null)
      subCategories = new Vector();
    if (!subCategories.contains(subcategory))
      subCategories.add(subcategory);
  }

  public String getName() {
    return name;
  }

  public REditorOutlineNode getParent() {
    return parent;
  }
}