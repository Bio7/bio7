package com.eco.bio7.jobs;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;

public class LoadData {
	
	public static void load(String source){
		LoadWorkspaceJob load = new LoadWorkspaceJob(source);
		load.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					
					
				}
			}
		});
		load.setUser(true);
		load.schedule();
	}
	

}
