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

package com.eco.bio7.discrete;

import java.util.ArrayList;

/*A class for counting the states in the Bio7 application!.*/

public class CounterModel {
	int zahl = 0;
	ArrayList<Integer> counterList = new ArrayList<Integer>();

	public void setZahl() {
		zahl++;
	}

	public void reset() {
		zahl = 0;
	}

	public int getCount() {
		return zahl;
	}

	public void addCountList(int c) {
		counterList.add(new Integer(c));
	}

	public void deleteCountList() {
		counterList.clear();
	}

	public ArrayList<Integer> getCounterList() {
		return counterList;
	}

	public int[] getCounterListasArray() {
		int[] arra = new int[counterList.size()];
		for (int i = 0; i < counterList.size(); i++) {
			Integer z = (Integer) counterList.get(i);
			arra[i] = z.intValue();

		}

		return arra;
	}

}
