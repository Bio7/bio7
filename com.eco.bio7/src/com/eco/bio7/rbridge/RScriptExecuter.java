package com.eco.bio7.rbridge;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;

public abstract class RScriptExecuter implements RScriptExecuterInterface {

	@Override
	public void evaluate(com.eco.bio7.rbridge.backend.RBackend backend, IProgressMonitor monitor) {
	}

	public void done(IJobChangeEvent event) {
		// TODO Auto-generated method stub
	}

}
