package com.eco.bio7.rbridge.actions;

import java.util.prefs.Preferences;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import com.eco.bio7.rbridge.backend.RBackend;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RConfig;
import com.eco.bio7.rbridge.RBridge;
import com.eco.bio7.rbridge.REval;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.views.RShellView;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.rffm.RException;

public class LoadRWorkspace extends Action {

	private final IWorkbenchWindow window;
	private Preferences prefs;

	public LoadRWorkspace(String text, IWorkbenchWindow window) {

		super(text);
		this.window = window;
		setActionDefinitionId("com.eco.bio7.load_r_workspace");
		setId("com.eco.bio7.load_r_workspace");

	}

	public void run() {
		//IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		// boolean rPipe = store.getBoolean("r_pipe");
		RBackend con = RBridge.getBackend();
		/* Started when it is used (preferences) */
		if (con == null && com.eco.bio7.rcp.AutoStart.startROnUse()) {
			con = RBridge.getBackend();
		}
		if (con == null) {

			String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
			if (selectionConsole.equals("R")) {

				String selected;
				Shell s = new Shell();
				FileDialog fd = new FileDialog(s, SWT.OPEN);
				fd.setText("Open");
				if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
					prefs = Preferences.userNodeForPackage(this.getClass());
					String lastOutputDir = prefs.get("R_FILE_DIR", "");
					fd.setFilterPath(lastOutputDir);
					String[] filterExt = { "*.RData", "*." };
					fd.setFilterExtensions(filterExt);
					selected = fd.open();
					prefs.put("R_FILE_DIR", fd.getFilterPath());

					if (selected != null) {
						selected = selected.replace("\\", "\\\\");
					}
				} else {
					prefs = Preferences.userNodeForPackage(this.getClass());
					String lastOutputDir = prefs.get("R_FILE_DIR", "");
					fd.setFilterPath(lastOutputDir);
					String[] filterExt = { "*.RData", "*." };
					fd.setFilterExtensions(filterExt);
					selected = fd.open();
					prefs.put("R_FILE_DIR", fd.getFilterPath());

				}

				ConsolePageParticipant.pipeInputToConsole("load(file = \"" + selected + "\")", true, true);
				System.out.print("load(file = \"" + selected + "\")");
				System.out.println();

			} else {
				Bio7Dialog.message(com.eco.bio7.rcp.AutoStart.NOT_RUNNING_R);
			}

		} else {

				load(con);
				

			
		}

	}

	public void load(RBackend con) {
		String selected;

		Shell s = new Shell();
		FileDialog fd = new FileDialog(s, SWT.OPEN);
		fd.setText("Open");
		if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
			prefs = Preferences.userNodeForPackage(this.getClass());
			String lastOutputDir = prefs.get("R_FILE_DIR", "");
			fd.setFilterPath(lastOutputDir);
			String[] filterExt = { "*.RData", "*." };
			fd.setFilterExtensions(filterExt);
			selected = fd.open();
			prefs.put("R_FILE_DIR", fd.getFilterPath());

			if (selected != null) {
				selected = selected.replace("\\", "\\\\");
			}

		} else {
			prefs = Preferences.userNodeForPackage(this.getClass());
			String lastOutputDir = prefs.get("R_FILE_DIR", "");
			fd.setFilterPath(lastOutputDir);
			String[] filterExt = { "*.RData", "*." };
			fd.setFilterExtensions(filterExt);
			selected = fd.open();
			prefs.put("R_FILE_DIR", fd.getFilterPath());

		}
		final String filePath = selected;
		if (selected != null) {
			final String load = "try(load(file = .bio7TempRScriptFile))";
			if (RState.isBusy() == false) {
				RState.setBusy(true);

				try {
					con.assign(".bio7TempRScriptFile", selected);
				} catch (RException e) {

					e.printStackTrace();
				}
				RInterpreterJob Do = new RInterpreterJob(load, null);
				Do.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							/*Reload the configuration for R to reload local temp path and display definition!*/
							RConfig.config(con);
							RState.setBusy(false);
							
							System.out.print("try(load(file = " + filePath + "))");
							System.out.println();
							REval.listRObjects();
						} else {
							RState.setBusy(false);
						}
					}
				});

				Do.schedule();

			} else {
				Bio7Dialog.message("R is busy!");
			}
		}
	}
}