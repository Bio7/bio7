package com.eco.bio7.rcp;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.util.Util;

public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

	private static final String PERSPECTIVE_ID = "com.eco.bio7.perspective_2d";

	public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		return new ApplicationWorkbenchWindowAdvisor(configurer);
	}

	/* Save the configuration! */
	public void initialize(IWorkbenchConfigurer configurer) {
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

	public boolean preShutdown() {
		boolean close = super.preShutdown();
		if (close) {
			close = true;
			/* Save all editors before shutdown! */
			//SAVE_ALL_EDITORS
			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			if (store.getBoolean("SAVE_ALL_EDITORS")) {

				NullProgressMonitor monitor = new NullProgressMonitor();
				IEditorPart[] dirtyEditors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getDirtyEditors();
				for (IEditorPart iEditorPart : dirtyEditors) {
					iEditorPart.doSave(monitor);
				}
				// Save the workspace!
				// final MultiStatus status = new MultiStatus(...);
				IRunnableWithProgress runnable = new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						IWorkspace ws = ResourcesPlugin.getWorkspace();
						
						try {
							// status.merge(ws.save(true, monitor));
							ws.save(true, monitor);
						} catch (CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};
				try {
					new ProgressMonitorDialog(null).run(false, false, runnable);
				} catch (InvocationTargetException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				/*
				 * IWorkbench workbench = PlatformUI.getWorkbench(); final IWorkbenchWindow
				 * window = workbench.getActiveWorkbenchWindow();
				 * 
				 * IPerspectiveRegistry perspectiveRegistry = window.getWorkbench()
				 * .getPerspectiveRegistry(); IPerspectiveDescriptor
				 * personalPerspectiveDescriptor = perspectiveRegistry
				 * .findPerspectiveWithId("com.eco.bio7.WorldWind.3dglobe");
				 * IPerspectiveDescriptor personalPerspectiveDescriptor2 = perspectiveRegistry
				 * .findPerspectiveWithId("com.eco.bio7.perspective_3d");
				 * 
				 * IWorkbenchPage wbp =
				 * PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				 * 
				 * //wbp.hideView(wbp.findView("com.eco.bio7.worldwind.WorldWindOptionsView"));
				 * 
				 * wbp.closePerspective(personalPerspectiveDescriptor, false, true);
				 * 
				 * wbp.closePerspective(personalPerspectiveDescriptor2, false, true);
				 */

			}
			//RServe.print("q(save='yes')");

		}
		return close;
	}

}
