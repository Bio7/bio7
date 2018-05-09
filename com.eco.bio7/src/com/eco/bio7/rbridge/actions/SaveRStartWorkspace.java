package com.eco.bio7.rbridge.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.rosuda.REngine.Rserve.RConnection;

import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.rbridge.RServe;

public class SaveRStartWorkspace extends Action {

	private final IWorkbenchWindow window;

	public SaveRStartWorkspace(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;

		setId("com.eco.bio7.save_r_workspace");

	}

	public void run() {
		RConnection d = RServe.getConnection();

		if (d != null) {

			saver(d);

		} else {

			MessageBox messageBox = new MessageBox(new Shell(),

			SWT.ICON_WARNING);
			messageBox
					.setMessage("RServer connection failed - Server is not running !");
			messageBox.open();

		}

	}

	public void saver(RConnection d) {
		String selected;

		Shell s = new Shell();
		FileDialog fd = new FileDialog(s, SWT.SAVE);
		fd.setText("Save");
		fd.setFilterPath("C:\\");
		String[] filterExt = { ".RData" };
		fd.setFilterExtensions(filterExt);
		selected = fd.open();
		selected = selected.replace("\\", "\\\\");
		selected = selected.replace("ä", "ae");
		selected = selected.replace("ü", "ue");
		selected = selected.replace("ö", "oe");

		if (selected != null) {

			//System.out.println("" + selected);
			String save = "save.image(file =\"" + selected
					+ "\", version = NULL, ascii = FALSE)";
			RInterpreterJob Do = new RInterpreterJob(save,null);
			Do.setUser(true);
			Do.schedule();

		}

	}
}