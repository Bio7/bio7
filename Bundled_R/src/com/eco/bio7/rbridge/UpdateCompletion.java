package com.eco.bio7.rbridge;

/**
 * An interface to set an variable instance of the ShellCompletion class to the REditor class. Both the R editor and the ShellCompletion class
 * implement this interface so that the class is known in the R editor (to avoid cyclic dependencies - a call from the R editor plugin is not possible) 
 * which then can calls the implemented interface method trigger to update the code completion!
 *
 */
public interface UpdateCompletion {
	
	public void trigger();

}
