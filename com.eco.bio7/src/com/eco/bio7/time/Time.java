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

package com.eco.bio7.time;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.info.InfoView;

/**
 * This class provides methods to access the time and the counter of the Bio7
 * application.
 * 
 * @author Bio7
 * 
 */
public class Time {
	private static String month[] = { "July", "August", "September", "October", "November", "Dezember", "January", "February", "March", "April", "May", "June" };

	private static int monthnumber[] = { 7, 8, 9, 10, 11, 12, 1, 2, 3, 4, 5, 6 };

	// We begin in summer, so we need the order of the months !

	private static volatile int monthcount = 0;// Count from 1-12

	private static volatile int time = 0;// The global time

	private static volatile int year = 0;// The count for one year

	private static volatile int counter = 0;//Volatile avoids caching of the object!

	private static volatile int modtime = 12;// For the modulo operation of time!

	private static volatile int nutrienttime = 0;
	/*
	 * A counter for the abiotic factors for the plots of the abiotic factors
	 * from BeanShell.
	 */

	private static int interval = 500; // The speed of update-> or of one
										// calculation!

	private static boolean pause = true; // Pause the calculation thread!

	public static void Timeupdate() {

		monthcount = time % modtime;
		nutrienttime = (time % modtime) + 1;// +1 To get the right month!
		// !

		time++;
		if (monthcount == 0) {
			year++;
		}

	}

	/**
	 * Resets the time.
	 * 
	 */
	public static void Timereset() {
		counter = 0;
		time = 0;
		year = 0;
		monthcount = 0;
		nutrienttime = 0;
		//InfoView.getPan().repaint();
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {

				Label lblTimesteps = InfoView.getLblTimesteps();
				if (lblTimesteps != null) {
					lblTimesteps.setText("Time steps: " + Time.getTime());
				}
				Label lblMonth = InfoView.getLblMonth();
				if (lblMonth != null) {
					lblMonth.setText(InfoView.getMonth() + Time.getMonth());
				}
				Label lblYear = InfoView.getLblYear();
				if (lblYear != null) {
					lblYear.setText(InfoView.getYear() + Time.getYear());
				}
			}
		});

	}

	/**
	 * Returns the month.
	 * 
	 * @return the month as string value.
	 */
	public static String getMonth() {
		String the_month;
		the_month = month[monthcount];
		return the_month;
	}

	/**
	 * Returns the year.
	 * 
	 * @return the year as an integer value.
	 */
	public static int getYear() {
		return year;
	}

	/**
	 * Returns the month.
	 * 
	 * @return the month as an integer value.
	 */
	public static int getMonthcount() {
		return monthcount + 1;
	}

	/**
	 * Returns the number of the month.
	 * 
	 * @return the number of the month as an integer value.
	 */
	public static int getMonthNumber() {
		return monthnumber[monthcount];
	}

	/**
	 * 
	 * Method for the abiotic factors panel.
	 * 
	 * @return the nutrient time as an integer value.
	 */
	public static int getNutrienttime() {
		return nutrienttime;
	}

	/**
	 * Method for the abiotic factors panel.
	 * 
	 * @param time the nutrient time as an integer value.
	 */
	public static void setNutrienttime(int time) {
		Time.nutrienttime = time;
	}

	/**
	 * Returns the Bio7 counter.
	 * 
	 * @return the counter as an integer value.
	 */
	public static int getCounter() {
		return counter;
	}

	/**
	 * Sets the Bio7 counter.
	 * 
	 * @param counter the counter as an integer value.
	 */
	public static void setCounter(int counter) {
		Time.counter = counter;
	}

	/**
	 * Increments the Bio7 counter.
	 */
	public static void count() {
		Time.counter++;
	}

	/**
	 * Returns if the pause is activated.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isPause() {
		return pause;
	}

	/**
	 * Sets or unsets the pause.
	 * 
	 * @param pause a boolean value.
	 */
	public static void setPause(boolean pause) {
		Time.pause = pause;
	}

	/**
	 * Returns the current modulo operator.
	 * 
	 * @return the modulo operator as an integer value.
	 */
	public static int getModtime() {
		return modtime;
	}

	/**
	 * Changes the current modulo operator (time % operator).
	 * 
	 * @param modtime the modulo operator as an integer value.
	 */
	public static void setModtime(int modtime) {
		Timereset();
		Time.modtime = modtime;
	}

	/**
	 * A method to rearrange the sequence of the month. This method has to be used
	 * with the setMonthnumber method!
	 * 
	 * @param month the month values as a string array.
	 */
	public static void setMonth(String[] month) {
		Timereset();
		Time.month = month;
	}

	/**
	 * A method which will change the sequence of the month as a number. This method
	 * has to be used with the setMonth method!
	 * 
	 * @param month the sequence as an integer array.
	 */
	public static void setMonthnumber(int[] month) {
		Timereset();
		Time.monthnumber = month;
	}

	/**
	 * Sets the year.
	 * 
	 * @param year the year as an integer value.
	 */
	public static void setYear(int year) {
		Time.year = year;
	}

	/**
	 * Returns the time.
	 * 
	 * @return the time as an integer value.
	 */
	public static int getTime() {
		return time;
	}

	/**
	 * Sets the speed of the calculation thread.
	 * 
	 * @param value the speed in milliseconds.
	 */
	public static void setInterval(int value) {
		Time.interval = value;
	}

	/**
	 * Returns the speed of the calculation thread.
	 * 
	 * @return the speed in milliseconds.
	 */
	public static int getInterval() {
		return interval;
	}

	/**
	 * Changes the label of the default month label in the Control panel.
	 * 
	 * @param label a string as the new label.
	 */
	public static void changeMonthLabel(final String label) {

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				InfoView.setMonth(label);
				InfoView.getLblMonth().setText(label + Time.getMonth());
			}
		});
	}

	/**
	 * Changes the label of the default year label in the Control panel.
	 * 
	 * @param label a string as the new label.
	 */
	public static void changeYearLabel(final String label) {

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				InfoView.setYear(label);
				InfoView.getLblYear().setText(label + Time.getYear());
			}
		});

	}

}
