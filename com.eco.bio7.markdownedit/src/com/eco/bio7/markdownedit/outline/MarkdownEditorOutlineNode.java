package com.eco.bio7.markdownedit.outline;

import java.util.Vector;

public class MarkdownEditorOutlineNode {
	private String name;

	private Vector subCategories;

	private MarkdownEditorOutlineNode parent;

	private int lineNumber;

	private String type;

	public MarkdownEditorOutlineNode(String name, int lineNumber,String type, MarkdownEditorOutlineNode parent) {
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

	private void addSubCategory(MarkdownEditorOutlineNode subcategory) {
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

	public MarkdownEditorOutlineNode getParent() {
		return parent;
	}
}