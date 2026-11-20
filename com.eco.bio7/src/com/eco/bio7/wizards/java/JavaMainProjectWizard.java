/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.wizards.java;

import java.util.LinkedHashMap;
import java.util.Map;

/** A Java project with a class with a main method (run in Bio7 with the Run action of the Java editor). */
public class JavaMainProjectWizard extends Bio7JavaTemplateProjectWizard {

	@Override
	protected String title() {
		return "Java Project with Main Class";
	}

	@Override
	protected String description() {
		return "This wizard creates a Java project with a class with a main method (Bio7 compiles it and calls main).";
	}

	@Override
	protected String defaultProject() {
		return "My_Java_Project";
	}

	@Override
	protected String defaultFile() {
		return "Main.java";
	}

	@Override
	protected Map<String, String> files(String className) {
		Map<String, String> files = new LinkedHashMap<>();
		files.put(className + ".java", """
				/*
				 * A Java program: run it with the Run action of the Java editor (Bio7
				 * compiles the class and calls main). The output is shown in the console.
				 */
				public class %s {

					public static void main(String[] args) {
						System.out.println("Hello from Bio7!");
						double sum = 0;
						for (int i = 1; i <= 10; i++) {
							sum += i * i;
						}
						System.out.println("The sum of the squares of 1 to 10: " + sum);
					}
				}
				""".formatted(className));
		return files;
	}
}
