package com.eco.bio7.methods;

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


/**
 * From this class the plant objects are created.
 * 
 * @author Bio7
 * 
 */
public class Plant {

	private int x;

	private int y;

	private String species;

	private String synonym;

	private String lifeform;

	private String lifespan;

	private String anthesisstart;

	private String anthesesisend;

	private int sheightmin;

	private int sheightmax;

	private int swidthmin;

	private int swidthmax;

	private int ssurface;

	private int rdepthmin;

	private int rdepthmax;

	private int rwidthmin;

	private int rwidthmax;

	private int rsurface;

	private boolean vegetative;

	private boolean sexual;

	private int dispersalmax;

	private int dispersalmin;

	private int spreadafter;

	private int established;

	private boolean near;

	private boolean far;

	private boolean seedbank;

	private int light;

	private int temperature;

	private int kontinental;

	private int moist;

	private int reaction;

	private int n;

	private int salt;

	private int age = 0; // the age of the plant

	
	 /*Every time this class get instantiated it copies the choosen species from
	 the database !!*/
	 

	/**
	 * @param x the x-coordinate.
	 * @param y the y-coordinate.
	 * @param p the plant template.
	 */
	public Plant() {// This class copies the
		// Objects
		// from the database !

		/*this.x = x;
		this.y = y;
		this.species = p.getSpecies(); // p.getAmount();
		this.synonym = p.getSynonym();
		this.lifeform = p.getLifeform();
		this.lifespan = p.getLifespan();
		this.anthesisstart = p.getAnthesisstart();
		this.anthesesisend = p.getAnthesesisend();
		this.sheightmin = p.getSheightmin();
		this.sheightmax = p.getSheightmax();
		this.swidthmin = p.getSwidthmin();
		this.swidthmax = p.getSwidthmax();
		this.ssurface = p.getSsurface();
		this.rdepthmin = p.getRdepthmin();
		this.rdepthmax = p.getRdepthmax();
		this.rwidthmin = p.getRwidthmin();
		this.rwidthmax = p.getRwidthmax();
		this.rsurface = p.getRsurface();
		this.vegetative = p.isVegetative();
		this.sexual = p.isSexual();
		this.dispersalmax = p.getDispersalmax();
		this.dispersalmin = p.getDispersalmin();
		this.spreadafter = p.getSpreadafter();
		this.established = p.getEstablished();
		this.near = p.isNear();
		this.far = p.isFar();
		this.seedbank = p.isSeedbank();
		this.light = p.getLight();
		this.temperature = p.getTemperature();
		this.kontinental = p.getKontinental();
		this.moist = p.getMoist();
		this.reaction = p.getReaction();
		this.n = p.getN();
		this.salt = p.getSalt();
		this.age = p.getAge();*/

	}

	/**
	 * Returns the current age of the plant.
	 * 
	 * @return an integer value.
	 */
	public int getAge() {
		return age;
	}

	/**
	 * Sets the age of the plant.
	 * 
	 * @param age
	 *            an integer value.
	 * 
	 */
	public void setAge(int age) {
		this.age = age;
	}

	/**
	 * Returns the month when the anthesis of the plant ends.
	 * 
	 * @return a string representation.
	 */
	public String getAnthesesisend() {
		return anthesesisend;
	}

	/**
	 * Sets the month when the anthesis of the plant ends.
	 * 
	 * @param anthesesisend
	 *            a string representation.
	 */
	public void setAnthesesisend(String anthesesisend) {
		this.anthesesisend = anthesesisend;
	}

	/**
	 * Returns the anthesis start of the plant.
	 * 
	 * @return a string representation.
	 */
	public String getAnthesisstart() {
		return anthesisstart;
	}

	/**
	 * Sets the anthesis start of the plant.
	 * 
	 * @param anthesisstart
	 *            a string representation.
	 */
	public void setAnthesisstart(String anthesisstart) {
		this.anthesisstart = anthesisstart;
	}

	/**
	 * Returns the maximum distance of the seed dispersal or amount of seeds of
	 * the plant.
	 * 
	 * @return an integer value.
	 */
	public int getDispersalmax() {
		return dispersalmax;
	}

	/**
	 * Sets the maximum distance of the seed dispersal or amount of seeds of the
	 * plant.
	 * 
	 * @param dispersalmax
	 *            an integer value.
	 */
	public void setDispersalmax(int dispersalmax) {
		this.dispersalmax = dispersalmax;
	}

	/**
	 * Returns the minimum dispersal distance or amount of seeds.
	 * 
	 * @return an integer value.
	 */
	public int getDispersalmin() {
		return dispersalmin;
	}

	/**
	 * Sets the minimum dispersal distance or amount of seeds.
	 * 
	 * @param dispersalmin
	 *            an integer value.
	 */
	public void setDispersalmin(int dispersalmin) {
		this.dispersalmin = dispersalmin;
	}

	/**
	 * Returns the establishment rate of the plant.
	 * 
	 * @return an integer value.
	 */
	public int getEstablished() {
		return established;
	}

	/**
	 * Sets the establishment rate of the plant.
	 * 
	 * @param established
	 *            an integer value.
	 */
	public void setEstablished(int established) {
		this.established = established;
	}

	/**
	 * Returns if the plant disperses seeds far.
	 * 
	 * @return a boolean value.
	 */
	public boolean isFar() {
		return far;
	}

	/**
	 * Sets the dispersal to far which means that the plant can disperse seeds
	 * to far distances.
	 * 
	 * @param far
	 *            a boolean value.
	 */
	public void setFar(boolean far) {
		this.far = far;
	}

	/**
	 * Returns the kontinental number.
	 * 
	 * @return an integer value.
	 */
	public int getKontinental() {
		return kontinental;
	}

	/**
	 * Sets the kontinental number.
	 * 
	 * @param kontinental
	 *            an integer value.
	 */
	public void setKontinental(int kontinental) {
		this.kontinental = kontinental;
	}

	/**
	 * Returns the lifeform of the plant.
	 * 
	 * @return a string representation.
	 */
	public String getLifeform() {
		return lifeform;
	}

	/**
	 * Sets the lifeform of the plant.
	 * 
	 * @param lifeform
	 *            a string representation.
	 */
	public void setLifeform(String lifeform) {
		this.lifeform = lifeform;
	}

	/**
	 * Returns the lifespan of the plant.
	 * 
	 * @return a string representation.
	 */
	public String getLifespan() {
		return lifespan;
	}

	/**
	 * Sets the lifespan of the plant.
	 * 
	 * @param lifespan
	 *            a string representation.
	 */
	public void setLifespan(String lifespan) {
		this.lifespan = lifespan;
	}

	/**
	 * Returns the Ellenberg number of the light.
	 * 
	 * @return an integer value.
	 */
	public int getLight() {
		return light;
	}

	/**
	 * Sets the Ellenberg number of the light.
	 * 
	 * @param light
	 *            an integer value.
	 */
	public void setLight(int light) {
		this.light = light;
	}

	/**
	 * Returns the Ellenberg number of the moist.
	 * 
	 * @return an integer value.
	 */
	public int getMoist() {
		return moist;
	}

	/**
	 * Sets the Ellenberg number for the moist.
	 * 
	 * @param moist
	 *            an integer value.
	 */
	public void setMoist(int moist) {
		this.moist = moist;
	}

	/**
	 * Returns the Ellenberg number of nitrate.
	 * 
	 * @return an integer value.
	 */
	public int getN() {
		return n;
	}

	/**
	 * Sets the Ellenberg number of nitrate.
	 * 
	 * @param n
	 *            an integer value.
	 */
	public void setN(int n) {
		this.n = n;
	}

	/**
	 * Returns if the plant disperses seeds in the near surrounding.
	 * 
	 * @return a boolean value.
	 */
	public boolean isNear() {
		return near;
	}

	/**
	 * Sets the information that the plant can disperse seeds in the near
	 * surrounding.
	 * 
	 * @param near
	 *            a boolean value.
	 */
	public void setNear(boolean near) {
		this.near = near;
	}

	/**
	 * Returns the maximum root depth of the plant.
	 * 
	 * @return an integer value.
	 */
	public int getRdepthmax() {
		return rdepthmax;
	}

	/**
	 * Sets the maximum root depth of the plant.
	 * 
	 * @param rdepthmax
	 *            an integer value.
	 */
	public void setRdepthmax(int rdepthmax) {
		this.rdepthmax = rdepthmax;
	}

	/**
	 * Returns the minimum root depth of the plant.
	 * 
	 * @return an integer value.
	 */
	public int getRdepthmin() {
		return rdepthmin;
	}

	/**
	 * Sets the minimum root depth of the plant.
	 * 
	 * @param rdepthmin
	 *            an integer value.
	 */
	public void setRdepthmin(int rdepthmin) {
		this.rdepthmin = rdepthmin;
	}

	/**
	 * Returns the Ellenberg reaction number of the plant.
	 * 
	 * @return an integer value.
	 */
	public int getReaction() {
		return reaction;
	}

	/**
	 * Sets the Ellenberg reaction number of the plant.
	 * 
	 * @param reaction
	 *            an integer value.
	 */
	public void setReaction(int reaction) {
		this.reaction = reaction;
	}

	/**
	 * Returns the root surface of the plant.
	 * 
	 * @return an integer value.
	 */
	public int getRsurface() {
		return rsurface;
	}

	/**
	 * Sets the root surface of the plant.
	 * 
	 * @param rsurface
	 *            an integer value.
	 */
	public void setRsurface(int rsurface) {
		this.rsurface = rsurface;
	}

	/**
	 * Returns the maximum width of the root architecture of the plant.
	 * 
	 * @return an integer value.
	 */
	public int getRwidthmax() {
		return rwidthmax;
	}

	/**
	 * Sets the maximum width of the root architecture of the plant.
	 * 
	 * @param rwidthmax
	 *            an integer value.
	 */
	public void setRwidthmax(int rwidthmax) {
		this.rwidthmax = rwidthmax;
	}

	/**
	 * Returns the minimum width of the root architecture of the plant.
	 * 
	 * @return an integer value.
	 */
	public int getRwidthmin() {
		return rwidthmin;
	}

	/**
	 * Sets the minimum width of the root architecture of the plant.
	 * 
	 * @param rwidthmin
	 *            an integer value.
	 */
	public void setRwidthmin(int rwidthmin) {
		this.rwidthmin = rwidthmin;
	}

	/**
	 * Returns the Ellenberg number for salt.
	 * 
	 * @return an integer value.
	 */
	public int getSalt() {
		return salt;
	}

	/**
	 * Sets the Ellenberg number for salt.
	 * 
	 * @param salt
	 *            an integer value.
	 */
	public void setSalt(int salt) {
		this.salt = salt;
	}

	/**
	 * Returns if the plant has the ability to invest into a seed bank.
	 * 
	 * @return a boolean value.
	 */
	public boolean isSeedbank() {
		return seedbank;
	}

	/**
	 * Sets the ability of the plant to invest into a seed bank.
	 * 
	 * @param seedbank
	 *            a boolean value.
	 */
	public void setSeedbank(boolean seedbank) {
		this.seedbank = seedbank;
	}

	/**
	 * Returns if the plant reproduces itself sexual.
	 * 
	 * @return a boolean value.
	 */
	public boolean isSexual() {
		return sexual;
	}

	/**
	 * Sets the plant reproduction to sexual.
	 * 
	 * @param sexuell
	 *            a boolean value
	 */
	public void setSexual(boolean sexuell) {
		this.sexual = sexuell;
	}

	/**
	 * Returns the maximum shoot height of the plant.
	 * 
	 * @return an integer value.
	 */
	public int getSheightmax() {
		return sheightmax;
	}

	/**
	 * Sets the maximum shoot height of the plant.
	 * 
	 * @param sheightmax
	 *            an integer value.
	 */
	public void setSheightmax(int sheightmax) {
		this.sheightmax = sheightmax;
	}

	/**
	 * Returns the minimum shoot height of the plant.
	 * 
	 * @return an integer value.
	 */
	public int getSheightmin() {
		return sheightmin;
	}

	/**
	 * Sets the minimum shoot height of the plant.
	 * 
	 * @param sheightmin
	 *            an integer value.
	 */
	public void setSheightmin(int sheightmin) {
		this.sheightmin = sheightmin;
	}

	/**
	 * Returns the name of the species.
	 * 
	 * @return a string representation.
	 */
	public String getSpecies() {
		return species;
	}

	/**
	 * Sets the name of the species.
	 * 
	 * @param species
	 *            a string representation.
	 */
	public void setSpecies(String species) {
		this.species = species;
	}

	/**
	 * Returns the amount of time after which a plant is able to spread.
	 * 
	 * @return an integer value.
	 */
	public int getSpreadafter() {
		return spreadafter;
	}

	/**
	 * Sets the amount of time after which a plant is able to spread.
	 * 
	 * @param spreadafter
	 *            an integer value.
	 */
	public void setSpreadafter(int spreadafter) {
		this.spreadafter = spreadafter;
	}

	/**
	 * Returns the shoot surface of the plant.
	 * 
	 * @return an integer value.
	 */
	public int getSsurface() {
		return ssurface;
	}

	/**
	 * Sets the shoot surface of the plant.
	 * 
	 * @param ssurface
	 *            an integer value.
	 */
	public void setSsurface(int ssurface) {
		this.ssurface = ssurface;
	}

	/**
	 * Returns the maximum width of the shoot of the plant.
	 * 
	 * @return an integer value.
	 */
	public int getSwidthmax() {
		return swidthmax;
	}

	/**
	 * Sets the maximum width of the shoot of the plant.
	 * 
	 * @param swidthmax
	 *            an integer value.
	 */
	public void setSwidthmax(int swidthmax) {
		this.swidthmax = swidthmax;
	}

	/**
	 * Returns the minimum width of the shoot of the plant.
	 * 
	 * @return an integer value.
	 */
	public int getSwidthmin() {
		return swidthmin;
	}

	/**
	 * Sets the minimum width of the shoot of the plant.
	 * 
	 * @param swidthmin
	 *            an integer value.
	 */
	public void setSwidthmin(int swidthmin) {
		this.swidthmin = swidthmin;
	}

	/**
	 * Returns the synonym of the plant species scientific name.
	 * 
	 * @return a string representation.
	 */
	public String getSynonym() {
		return synonym;
	}

	/**
	 * Sets the synonym of the plant species scientific name.
	 * 
	 * @param synonym
	 *            a string representation.
	 */
	public void setSynonym(String synonym) {
		this.synonym = synonym;
	}

	/**
	 * Returns the Ellenberg number of the temperature.
	 * 
	 * @return an integer value.
	 */
	public int getTemperature() {
		return temperature;
	}

	/**
	 * Sets the Ellenberg number of the temperature.
	 * 
	 * @param temperature
	 *            an integer value.
	 */
	public void setTemperature(int temperature) {
		this.temperature = temperature;
	}

	/**
	 * Returns if a vegetative plant reproduction can occur.
	 * 
	 * @return a boolean value.
	 */
	public boolean isVegetative() {
		return vegetative;
	}

	/**
	 * Sets the plant reproduction to vegetative.
	 * 
	 * @param vegetative
	 *            a boolean value.
	 */
	public void setVegetative(boolean vegetative) {
		this.vegetative = vegetative;
	}

	/**
	 * Returns the x-coordinate of the plant.
	 * 
	 * @return the x-coordinate.
	 */
	public int getX() {
		return x;
	}

	/**
	 * Sets the x-coordinate of the plant.
	 * 
	 * @param x
	 *            the x-coordinate.
	 */
	public void setX(int x) {
		this.x = x;
	}

	/**
	 * Returns the y-coordinate of the plant.
	 * 
	 * @return the y-coordinate.
	 */
	public int getY() {
		return y;
	}

	/**
	 * Sets the y-coordinate of the plant.
	 * 
	 * @param y
	 *            the y-coordinate.
	 */
	public void setY(int y) {
		this.y = y;
	}

}
