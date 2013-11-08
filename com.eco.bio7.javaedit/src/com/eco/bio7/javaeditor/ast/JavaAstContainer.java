package com.eco.bio7.javaeditor.ast;

/**
 * A helper class for the AST Outline View. The information about the line number and the name will be stored in an
 * ArrayList.
 */
public class JavaAstContainer {

	public String description;
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public int lineNumber;
	
	
}
