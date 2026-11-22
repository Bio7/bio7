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
package com.eco.bio7.simulation;

/**
 * A small seeded random number generator (mulberry32) with the same numbers in
 * Java and in JavaScript (the HTML export uses the same algorithm): a
 * simulation started with the same seed gives the same results in Bio7 and in
 * the web browser.
 */
public final class SimRandom {

	private int state;

	public SimRandom(int seed) {
		state = seed;
	}

	/** A random number in [0, 1). */
	public double nextDouble() {
		state += 0x6D2B79F5;
		int t = state;
		t = (t ^ (t >>> 15)) * (t | 1);
		t ^= t + (t ^ (t >>> 7)) * (t | 61);
		return ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / 4294967296.0;
	}

	/** A random integer in [0, n). */
	public int nextInt(int n) {
		return (int) (nextDouble() * n);
	}
}
