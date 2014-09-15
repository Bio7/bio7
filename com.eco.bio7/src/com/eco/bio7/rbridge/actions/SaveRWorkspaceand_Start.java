package com.eco.bio7.rbridge.actions;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.osgi.framework.Bundle;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.RInterpreterJobStartGui;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;

public class SaveRWorkspaceand_Start extends Action {

	private final IWorkbenchWindow window;

	public SaveRWorkspaceand_Start(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;
		setId("com.eco.bio7.save_r_start_workspace");

	}

	public void run() {
		RConnection d = RServe.getConnection();

		if (d != null) {
			if (RState.isBusy() == false) {

				saver(d);
			} else {
				Bio7Dialog.message("RServer is busy!");
			}

		} else {

			MessageBox messageBox = new MessageBox(new Shell(),

			SWT.ICON_WARNING);
			messageBox.setMessage("RServer connection failed - Server is not running !");
			messageBox.open();

		}

	}

	public void saver(RConnection d) {
		Bundle bundle = Platform.getBundle("com.eco.bio7");

		// String path = bundle.getLocation().split(":")[2];
		URL locationUrl = FileLocator.find(bundle, new Path("/"), null);
		URL fileUrl = null;
		try {
			fileUrl = FileLocator.toFileURL(locationUrl);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		String pathpluginloc = fileUrl.getFile();
		StringBuffer buff = new StringBuffer();

		buff.append(pathpluginloc);
		buff.deleteCharAt(0);

		IPreferenceStore store = null;
		String path = null;
		try {
			store = Bio7Plugin.getDefault().getPreferenceStore();
		} catch (RuntimeException e) {

			e.printStackTrace();
		}
		if (store != null) {
			path = store.getString(PreferenceConstants.P_TEMP_R);
		}
		if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {

			String selected = path + "\\tempR.RData";
			selected = selected.replace("\\", "\\\\");

			if (selected != null) {

				try {
					d.voidEval("try(save.image(file =" + "\"" + selected + "\"" + ", version = NULL, ascii = FALSE))");
				} catch (RserveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				RInterpreterJobStartGui Do = new RInterpreterJobStartGui(selected);
				Do.setUser(true);
				Do.schedule();

			}
		} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {

			String selected = path + "/tempR.RData";

			if (selected != null) {

				try {
					d.voidEval("save.image(" + "\"" + selected + "\"" + ", version = NULL, ascii = FALSE)");
				} catch (RserveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				RInterpreterJobStartGui Do = new RInterpreterJobStartGui(selected);
				Do.setUser(true);
				Do.schedule();

			}
		}
		else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Mac")) {

			String selected = path + "/tempR.RData";

			if (selected != null) {

				try {
					d.voidEval("save.image(" + "\"" + selected + "\"" + ", version = NULL, ascii = FALSE)");
				} catch (RserveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				RInterpreterJobStartGui Do = new RInterpreterJobStartGui(selected);
				Do.setUser(true);
				Do.schedule();

			}
		}
	}
}