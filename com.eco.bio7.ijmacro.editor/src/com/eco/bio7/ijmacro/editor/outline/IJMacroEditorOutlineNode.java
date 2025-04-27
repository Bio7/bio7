package com.eco.bio7.ijmacro.editor.outline;

import java.util.Vector;

public class IJMacroEditorOutlineNode {
	private String name;

	private Vector subCategories;

	private IJMacroEditorOutlineNode parent;

	private int lineNumber;

	private String type;

	public IJMacroEditorOutlineNode(String name, int lineNumber,String type, IJMacroEditorOutlineNode parent) {
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

	private void addSubCategory(IJMacroEditorOutlineNode subcategory) {
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

	public IJMacroEditorOutlineNode getParent() {
		return parent;
	}
}