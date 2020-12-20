package com.eco.bio7.rbridge;

import org.eclipse.core.runtime.IProgressMonitor;
import org.rosuda.REngine.Rserve.RConnection;

public interface RScriptExecuterInterface {
	
	 void evaluate(RConnection con,IProgressMonitor monitor);

}
