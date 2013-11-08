package com.eco.bio7.discrete;

import org.rosuda.REngine.REngineException;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RServe;

/**
 * This class provides a static method for the transfer of simulation values to
 * the R application.
 * 
 * @author Bio7
 * 
 */
public class CountValues {

	/**
	 * A method to transfer the Bio7 values from the discrete grid directly to R
	 * by means of Rserve. If this method is called all collected values from
	 * the given states to this time will be transferred to R.
	 * 
	 * @param identifier
	 *            - An optional run identifier as String.
	 * @param states
	 *            - The states which should be assigned.
	 * @param titles
	 *            - The names of the states.
	 */
	public static void transferCountsToR(String identifier, int[] states, String[] titles) {
		if (RServe.isRrunning()) {

			for (int i = 0; i < states.length; i++) {

				int val = states[i];

				CounterModel zahl = (CounterModel) Quad2d.getQuad2dInstance().zaehlerlist.get(val);// get

				int z[] = zahl.getCounterListasArray();

				try {
					RServe.getConnection().assign(identifier + titles[i], z);
				} catch (REngineException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		} else {
			Bio7Dialog.message("There is no R connection available");
		}
	}

}
