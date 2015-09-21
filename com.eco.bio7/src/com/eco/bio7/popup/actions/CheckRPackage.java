package com.eco.bio7.popup.actions;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;

import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;

public class CheckRPackage implements IObjectActionDelegate {

	public CheckRPackage() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(IAction action) {
		ConsolePageParticipant.setNativeInterpreterSelection("shell");
		String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
		if (selectionConsole.equals("shell")) {

			ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
			IStructuredSelection strucSelection = null;
			if (selection instanceof IStructuredSelection) {
				strucSelection = (IStructuredSelection) selection;

				Object selectedObj = strucSelection.getFirstElement();
				IResource resource = (IResource) strucSelection.getFirstElement();
				final IProject activeProject = resource.getParent().getProject();
				if (selectedObj instanceof IFolder||selectedObj instanceof IProject) {
					IFolder selectedFolder = null;
					 String loc;
					if (selectedObj instanceof IProject){
						IProject proj=(IProject)selectedObj;
						loc = proj.getLocation().toOSString();
						
						
					}
					else{
					 selectedFolder = (IFolder) selectedObj;
					  loc = selectedFolder.getLocation().toString();
					}
					// System.out.println(selectedFolder.getName());
					// System.out.println(selectedFolder.getLocation());
					IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
					String pathR = store.getString(PreferenceConstants.PATH_R);
					String optionsCheck = store.getString("rcmdcheck");
					if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
						 loc = loc.replace("/", "\\");
						ConsolePageParticipant.pipeInputToConsole("cd /d " + "\"" + loc + "\"",true,false);
						if (ApplicationWorkbenchWindowAdvisor.is64BitVM()) {
							ConsolePageParticipant.pipeInputToConsole("\"" + pathR + "/bin/x64/R\"" + " CMD check " + optionsCheck + " " + "\"" + loc + "\"",true,false);
						} else {
							ConsolePageParticipant.pipeInputToConsole("\"" + pathR + "/bin/i386/R\"" + " CMD check " + optionsCheck + " " + "\"" + loc + "\"",true,false);
						}
						//ConsolePageParticipant.pipeInputToConsole("\"" + pathR + "/bin/R\"" + " CMD check " + optionsCheck + " " + "\"" + loc + "\"");

					} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {
						//String loc = selectedFolder.getLocation().toString();
						ConsolePageParticipant.pipeInputToConsole("cd " + "\"" + loc + "\"",true,false);

						ConsolePageParticipant.pipeInputToConsole(pathR + "/bin/R CMD check " + optionsCheck + " " + "\"" + loc + "\"",true,false);

					}

					else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Mac")) {
						//String loc = selectedFolder.getLocation().toString();
						ConsolePageParticipant.pipeInputToConsole("cd " + "\"" + loc + "\"",true,false);

						ConsolePageParticipant.pipeInputToConsole(pathR + "/bin/R CMD check " + optionsCheck + " " + "\"" + loc + "\"",true,false);

					}
					IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
					if ((selectedObj instanceof IProject) == false) {
						IProject proj = root.getProject(activeProject.getName());
						try {
							proj.refreshLocal(IResource.DEPTH_INFINITE, null);
						} catch (CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						try {
							IProject proj = (IProject) selectedObj;
							proj.refreshLocal(IResource.DEPTH_INFINITE, null);
						} catch (CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

				}

			}
		} else {
			Bio7Dialog.message("Please start the \"Shell\" in the Bio7 Console");
		}

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// TODO Auto-generated method stub

	}

}
