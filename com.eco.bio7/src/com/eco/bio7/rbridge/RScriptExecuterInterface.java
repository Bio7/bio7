package com.eco.bio7.rbridge;

import org.eclipse.core.runtime.IProgressMonitor;

public interface RScriptExecuterInterface {


	/** The evaluation with the connection of Bio7 to R. */
	void evaluate(com.eco.bio7.rbridge.backend.RBackend backend, IProgressMonitor monitor);

}
