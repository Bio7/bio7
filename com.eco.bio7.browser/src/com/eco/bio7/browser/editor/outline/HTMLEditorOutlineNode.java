package com.eco.bio7.browser.editor.outline;

import java.util.Vector;

public class HTMLEditorOutlineNode {
	private String name;

	private Vector subCategories;

	private HTMLEditorOutlineNode parent;

	private int lineNumber;

	private String type;

	public HTMLEditorOutlineNode(String name, int lineNumber,String type, HTMLEditorOutlineNode parent) {
		this.name = name;
		this.type=type;
		this.lineNumber = lineNumber;
		this.parent = parent;
		if (parent != null)
			parent.addSubCategory(this);
	}

	public Vector getSubCategories() {
		return subCategories;
	}

	private void addSubCategory(HTMLEditorOutlineNode subcategory) {
		if (subCategories == null)
			subCategories = new Vector();
		if (!subCategories.contains(subcategory))
			subCategories.add(subcategory);
	}

	public String getName() {
		return name;
	}
	
	public String getType() {
		return type;
	}
	
	public int getLineNumber() {
		return lineNumber;
	}

	public HTMLEditorOutlineNode getParent() {
		return parent;
	}
}