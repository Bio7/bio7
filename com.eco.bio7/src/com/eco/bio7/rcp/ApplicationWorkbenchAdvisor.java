package com.eco.bio7.rcp;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import com.eco.bio7.collection.Work;
import com.eco.bio7.database.StateTable;
import com.eco.bio7.util.Util;

import javafx.application.Platform;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	private static final String PERSPECTIVE_ID = "com.eco.bio7.perspective_2d";

	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		return new ApplicationWorkbenchWindowAdvisor(configurer);
	}

	public void initialize(IWorkbenchConfigurer configurer) {// speichert die
																// Konfiguration
																// !!
		super.initialize(configurer);
		configurer.setSaveAndRestore(false);
	}

	/* Refresh e.g. the Project Explorer when opening! */
	public IAdaptable getDefaultPageInput() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	public String getInitialWindowPerspectiveId() {
		return PERSPECTIVE_ID;
	}

	public String getMainPreferencePageId() {

		return "com.eco.bio7.preferences.Bio7PreferencePage";
	}

	/* Workaround for MacOSX related to JavaFX and a shutdown error! */
	public boolean preShutdown() {
		boolean close = super.preShutdown();
		if (close) {
			close = true;
			/* Save all editors before shutdown! */
			if (Util.getOS().equals("Mac")) {
				NullProgressMonitor monitor = new NullProgressMonitor();
				IEditorPart[] dirtyEditors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getDirtyEditors();
				for (IEditorPart iEditorPart : dirtyEditors) {
					iEditorPart.doSave(monitor);
				}

			}

		}
		return close;
	}

}
