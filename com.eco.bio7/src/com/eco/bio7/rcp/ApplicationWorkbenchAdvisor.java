package com.eco.bio7.rcp;

import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;


public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {
	
	private static final String PERSPECTIVE_ID = "com.eco.bio7.perspective_2d";

    public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        return new ApplicationWorkbenchWindowAdvisor(configurer);
    }
    public void initialize(IWorkbenchConfigurer configurer) {// speichert die Konfiguration !!
    	super.initialize(configurer);
    	configurer.setSaveAndRestore(false);
    }
    
	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	} 
	
}
