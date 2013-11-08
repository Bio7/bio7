package com.eco.bio7.javaeditors;

import java.util.ArrayList;
import java.util.List;
import com.eco.bio7.javaeditor.ast.JavaAstContainer;

public class ClassModel {

	public ArrayList<JavaAstContainer> methodNames = new ArrayList<JavaAstContainer>();
	public ArrayList<JavaAstContainer> fieldNames = new ArrayList<JavaAstContainer>();
	public ArrayList<JavaAstContainer> importNames = new ArrayList<JavaAstContainer>();

	protected List<MainClass> getCategories() {
		List<MainClass> categories = new ArrayList<MainClass>();

		MainClass category = new MainClass();
		category.setName("Imports");
		categories.add(category);
		if (importNames.size() > 0) {
			for (int i = 0; i < importNames.size(); i++) {

				ClassMembers cm = new ClassMembers();
				cm.setSummary(importNames.get(i).getDescription());
				cm.setLineNumber(importNames.get(i).getLineNumber());
				cm.setClasstype("import");
				category.getTodos().add(cm);
			}
		}

		category = new MainClass();
		category.setName("Fields");
		categories.add(category);
		if (fieldNames.size() > 0) {

			for (int i = 0; i < fieldNames.size(); i++) {

				ClassMembers cm = new ClassMembers();
				cm.setSummary(fieldNames.get(i).getDescription());
				cm.setLineNumber(fieldNames.get(i).getLineNumber());
				cm.setClasstype("field");
				category.getTodos().add(cm);
			}
		}

		category = new MainClass();
		category.setName("Methods");
		categories.add(category);
		if (methodNames.size() > 0) {

			if (methodNames.size() > 0) {
				for (int i = 0; i < methodNames.size(); i++) {

					ClassMembers cm = new ClassMembers();
					cm.setSummary(methodNames.get(i).getDescription());
					cm.setLineNumber(methodNames.get(i).getLineNumber());
					cm.setClasstype("method");
					category.getTodos().add(cm);
				}
			}

		}
		methodNames.clear();
		fieldNames.clear();
		importNames.clear();
		return categories;
	}

}