package com.eco.bio7.rbridge;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.rosuda.REngine.Rserve.RConnection;

public abstract class RScriptExecuter implements RScriptExecuterInterface {

	@Override
	public void evaluate(RConnection con, IProgressMonitor monitor) {
		// TODO Auto-generated method stub
	}

	public void done(IJobChangeEvent event) {
		// TODO Auto-generated method stub
	}

}
