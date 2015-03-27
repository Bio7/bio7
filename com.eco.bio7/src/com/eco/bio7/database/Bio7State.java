package com.eco.bio7.database;

import com.eco.bio7.methods.CurrentStates;

/**
 * A class which provides methods to get templates from the database of Bio7 and
 * to create states inside the Bio7 application.
 * 
 * @author Bio7
 * 
 */
public class Bio7State {

	/**
	 * Creates a state with the given name.
	 * 
	 * @param name
	 *            the visible name of the state.
	 */
	public static void createState(String name) {

		StateTable.setCell(name);

	}

	/**
	 * Creates states from a given String array.
	 * 
	 * @param names
	 *            the  name state array.
	 */
	public static void createStates(String[] names) {
		for (int i = 0; i < names.length; i++) {
			StateTable.setCell(names[i]);
		}

	}

	/*
	 * public static void removeState(String name) {
	 * 
	 * StateTable.unsetCell(name);
	 * 
	 * 
	 * }
	 */

	/**
	 * Returns how many states are present in the Bio7 application.
	 * 
	 * @return the present amount of states.
	 */
	public static int getStateSize() {

		return CurrentStates.getStateList().size();

	}

}
