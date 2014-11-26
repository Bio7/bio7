package com.eco.bio7.rbridge.actions;

import java.util.prefs.Preferences;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.rosuda.REngine.Rserve.RConnection;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;

public class SaveRWorkspace extends Action {

	private final IWorkbenchWindow window;
	private Preferences prefs;

	public SaveRWorkspace(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;
		setId("com.eco.bio7.save_r_workspace2");

	}

	public void run() {

		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		RConnection con = RServe.getConnection();
		//boolean rPipe = store.getBoolean("r_pipe");
		if (con==null) {
			
			String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
			if (selectionConsole.equals("R")) {
				String selected = null;

				Shell s = new Shell();
				FileDialog fd = new FileDialog(s, SWT.SAVE);
				
				if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
					prefs = Preferences.userNodeForPackage(this.getClass());
					String lastOutputDir = prefs.get("R_FILE_DIR", "");
					fd.setFilterPath(lastOutputDir);

					String[] filterExt = { "*.RData" };
					fd.setFilterExtensions(filterExt);
					fd.setOverwrite(true);
					selected = fd.open();
					prefs.put("R_FILE_DIR", fd.getFilterPath());
					if (selected != null) {
						selected = selected.replace("\\", "\\\\");
						//selected = selected.replace("ä", "ae");
						//selected = selected.replace("ü", "ue");
						//selected = selected.replace("ö", "oe");

						ConsolePageParticipant.pipeInputToConsole("save.image(file =\"" + selected + "\", version = NULL, ascii = FALSE)",true,true);
						System.out.print("save.image(file =\"" + selected + "\", version = NULL, ascii = FALSE)");
						System.out.println();
					}
				} else {
					prefs = Preferences.userNodeForPackage(this.getClass());
					String lastOutputDir = prefs.get("R_FILE_DIR", "");
					fd.setFilterPath(lastOutputDir);
					String[] filterExt = { "*.RData" };
					fd.setFilterExtensions(filterExt);
					fd.setOverwrite(true);
					selected = fd.open();
					prefs.put("R_FILE_DIR", fd.getFilterPath());

					if (selected != null) {
						ConsolePageParticipant.pipeInputToConsole("save.image(file =\"" + selected + "\", version = NULL, ascii = FALSE)",true,true);
						System.out.print("save.image(file =\"" + selected + "\", version = NULL, ascii = FALSE)");
						System.out.println();
					}
				}
				

				ConsolePageParticipant.pipeInputToConsole("load(file = \"" + selected + "\")",true,true);

			} else {
				Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console or the Rserve connection!");
			}

		} else {


				saveRWorkspace();

		
		}
	}

	public void saveRWorkspace() {
		String selected = null;

		Shell s = new Shell();
		FileDialog fd = new FileDialog(s, SWT.SAVE);

		fd.setText("Save");
		if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
			prefs = Preferences.userNodeForPackage(this.getClass());
			String lastOutputDir = prefs.get("R_FILE_DIR", "");
			fd.setFilterPath(lastOutputDir);

			String[] filterExt = { "*.RData" };
			fd.setFilterExtensions(filterExt);
			fd.setOverwrite(true);
			selected = fd.open();
			prefs.put("R_FILE_DIR", fd.getFilterPath());
			if (selected != null) {
				selected = selected.replace("\\", "\\\\");
				selected = selected.replace("ä", "ae");
				selected = selected.replace("ü", "ue");
				selected = selected.replace("ö", "oe");

				saveRData(selected);
			}
		} else {
			prefs = Preferences.userNodeForPackage(this.getClass());
			String lastOutputDir = prefs.get("R_FILE_DIR", "");
			fd.setFilterPath(lastOutputDir);
			String[] filterExt = { "*.RData" };
			fd.setFilterExtensions(filterExt);
			fd.setOverwrite(true);
			selected = fd.open();
			prefs.put("R_FILE_DIR", fd.getFilterPath());

			if (selected != null) {
				saveRData(selected);
			}
		}

	}

	private void saveRData(String selected) {
		final String save = "save.image(file =\"" + selected + "\", version = NULL, ascii = FALSE)";
		if (RState.isBusy() == false) {
			RState.setBusy(true);

			RInterpreterJob Do = new RInterpreterJob(save, false, null);

			Do.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {
						RState.setBusy(false);
						System.out.print(save);
						System.out.println();
					} else {
						RState.setBusy(false);
					}
				}
			});

			Do.setUser(true);
			Do.schedule();

		} else {
			Bio7Dialog.message("Rserve is busy!");
		}
	}
}