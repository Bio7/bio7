package com.eco.bio7.image;

import com.eco.bio7.methods.CurrentStates;

//import com.eco.bio7.methods.State;

/**
 * This class provides some static methods for the creation of individual
 * properties for the use in the Points panel of the Bio7 application.
 * 
 * @author Bio7
 * 
 */
public class Individual {
	private double x;

	private double y;

	private float alpha;

	private int diameter;

	private int species;

	public Individual(double x, double y, int species, int diameter, float alpha) {
		this.x = x;
		this.y = y;
		this.alpha = alpha;
		this.diameter = diameter;
		exists(species);

	}

	private void exists(int species) {
		if (species < CurrentStates.getStateList().size()) {
			this.species = species;

		} else {
			System.out.println("You selected a state which is not activated or present!");
			this.species = 0;
		}
	}

	/**
	 * Returns the alpha value of the individual.
	 * 
	 * @return a float value.
	 */
	public float getAlpha() {
		return alpha;
	}

	/**
	 * Sets the alpha value of the individual.
	 * 
	 * @param alpha
	 *            a float value.
	 */
	public void setAlpha(float alpha) {
		this.alpha = alpha;
	}

	/**
	 * Returns the diameter of the individual.
	 * 
	 * @return an integer value.
	 */
	public int getDiameter() {
		return diameter;
	}

	/**
	 * Sets the diameter of the individual.
	 * 
	 * @param diamter
	 *            an integer value.
	 */
	public void setDiameter(int diamter) {
		this.diameter = diamter;
	}

	/**
	 * Returns the species number of the individual.
	 * 
	 * @return an integer value.
	 */
	public int getSpecies() {
		return species;
	}

	/**
	 * Sets the species number of the individual.
	 * 
	 * @param species
	 *            an integer value.
	 */
	public void setSpecies(int species) {
		this.species = species;
	}

	/**
	 * Returns the x-coordinate of the individual.
	 * 
	 * @return the x-coordinate as an double value.
	 */
	public double getX() {
		return x;
	}

	/**
	 * Sets the x-coordinate of the individual.
	 * 
	 * @param x
	 *            the x-coordinate as an double value.
	 */
	public void setX(double x) {
		this.x = x;
	}

	/**
	 * Returns the y-coordinate of the individual.
	 * 
	 * @return the y-coordinate as an double value.
	 */
	public double getY() {
		return y;
	}

	/**
	 * Sets the y-coordinate of the individual.
	 * 
	 * @param y
	 *            the y-coordinate as an double value.
	 */
	public void setY(double y) {
		this.y = y;
	}

}
