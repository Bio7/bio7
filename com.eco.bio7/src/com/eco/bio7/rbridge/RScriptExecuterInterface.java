package com.eco.bio7.rbridge;

import org.eclipse.core.runtime.IProgressMonitor;
import org.rosuda.REngine.Rserve.RConnection;

public interface RScriptExecuterInterface {

	/**
	 * The evaluation with an Rserve connection (null with embedded R: implement
	 * evaluate(RBackend, IProgressMonitor) for both).
	 */
	void evaluate(RConnection con, IProgressMonitor monitor);

	/**
	 * The evaluation with the connection of Bio7 (Rserve or embedded R). By default
	 * the Rserve variant is called; override this method to support embedded R.
	 */
	default void evaluate(com.eco.bio7.rbridge.backend.RBackend backend, IProgressMonitor monitor) {
		evaluate(backend == null ? null : backend.rserveConnection(), monitor);
	}

}
