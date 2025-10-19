/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.scriptengines;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.python.core.Options;

/**
 * @author Bio7 A class to return the Java Scripting connections of Bio7.
 */
public class ScriptEngineConnection {
	static {
		/*
		 * Jython Workaround from:
		 * http://python.6.x6.nabble.com/2-7b3-JSR-223-ScriptEngine-null-b2-works-
		 * td5074755.html
		 */
		Options.importSite = false;
	}
	private static ScriptEngine groovyShell = new ScriptEngineManager().getEngineByName("groovy");
	private static ScriptEngine scriptingEnginePython = new ScriptEngineManager().getEngineByName("python");
	private static ScriptEngine scriptingEngineBeanShell = new ScriptEngineManager().getEngineByName("beanshell");
	private static ScriptEngine scriptingEngineJavaScript = new ScriptEngineManager().getEngineByName("Nashorn");

	/**
	 * Get the current Bio7 Groovy connection.
	 * 
	 * @return the Groovy Interpreter
	 */
	public static ScriptEngine getScriptingEngineGroovy() {

		return groovyShell;
	}

	/**
	 * Get the current Bio7 Jython connection.
	 * 
	 * @return the Jython connection
	 */
	public static ScriptEngine getScriptingEnginePython() {
		return scriptingEnginePython;
	}

	/**
	 * Get the current Bio7 BeanShell connection.
	 * 
	 * @return the BeanShell connection
	 */
	public static ScriptEngine getScriptingEngineBeanShell() {
		return scriptingEngineBeanShell;
	}

	/**
	 * Get the current Bio7 BeanShell connection.
	 * 
	 * @return the BeanShell connection
	 */
	public static ScriptEngine getScriptingEngineJavaScript() {
		return scriptingEngineJavaScript;
	}

	/**
	 * Reinitializes the BeanShell interpreter.
	 */
	public static void reinitializeBeanShell() {
		scriptingEngineBeanShell = new ScriptEngineManager().getEngineByName("beanshell");
	}

	/**
	 * Reinitializes the Groovy interpreter.
	 */
	public static void reinitializeGroovy() {
		groovyShell = new ScriptEngineManager().getEngineByName("groovy");
	}

	/**
	 * Reinitializes the Jython interpreter.
	 */
	public static void reinitializeJython() {
		scriptingEnginePython = new ScriptEngineManager().getEngineByName("python");
	}

	/**
	 * Reinitializes the Jython interpreter.
	 */
	public static void reinitializeJavaScript() {
		scriptingEngineJavaScript = new ScriptEngineManager().getEngineByName("JavaScript");
	}

}
