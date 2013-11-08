/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/


package com.eco.bio7.batch;

import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.time.Time;

/**
 * This class provides some methods for the use in the Bio7 flow editor.
 * 
 * 
 * @author Bio7
 * 
 */
public class Bio7Flow {

	/**
	 * Returns the string representation of a decision value from a Bio7 flow.
	 * 
	 * @return a boolean value as a string.
	 */
	public static String getDecision() {
		return BatchModel.getDecision();
	}

	/**
	 * Set the decision value of a Bio7 flow.
	 * 
	 * @param decision
	 *            the boolean value as a string.
	 */

	public static void setDecision(String decision) {
		BatchModel.setDecision(decision);
	}

	/**
	 * Returns if a flow is canceled.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isCancel() {
		return BatchModel.isCancel();
	}

	/**
	 * Cancel the Bio7 flow.
	 * 
	 * @param cancel
	 *            a boolean value.
	 */
	public static void setCancel(boolean cancel) {
		BatchModel.setCancel(cancel);
	}

	/**
	 * Get the argument value of a Bio7 flow.
	 * 
	 * @return a string representing the argument value.
	 */
	public static String getArgument() {
		return BatchModel.getArgument();
	}

	/**
	 * Set the argument of a Bio7 flow.
	 * 
	 * @param argument
	 *            a string representing the argument.
	 */

	public static void setArgument(String argument) {
		BatchModel.setArgument(argument);
	}

	/**
	 * Pauses for the defined milliseconds.
	 * 
	 * @param milli
	 *            a pause in milliseconds.
	 */
	public static void setPause(int milli) {
		try {
			Thread.sleep(milli);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Starts the calculation thread and runs to the specified count. Works as
	 * long as a flow is not cancelled.
	 * 
	 * @param count
	 *            the count value at which the calculation thread stops.
	 */
	public static synchronized void runTo(int count) {
		Bio7Action.startCalculation();
		
		while (Time.getCounter() < count && isCancel() == false) {

		}

		Bio7Action.stopCalculation();

	}

	/**
	 * Opens the specified folder in the Bio7 flow if set to true. If this value
	 * is set to false no external application will be executed!
	 * 
	 * @param value
	 */
	public static void openFolder(boolean value) {

		BatchModel.setOpenFolder(value);
	}

	/**
	 * Returns the path of the folder in the Bio7 flow.
	 * 
	 * @return a string representation of the path.
	 */
	public static String getFolder() {

		return BatchModel.getFolder();

	}

	/**
	 * Sets the macro function to open the specified directory instead of the
	 * internal ImageJ macro method "getDirectory()".
	 * 
	 * @param directory
	 */
	public static void setImageJDirectory(String directory) {
		//ij.macro.Functions.setFileListBio7(directory);
	}

}
