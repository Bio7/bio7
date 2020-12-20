package com.eco.bio7.rbridge.actions;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.rosuda.REngine.Rserve.RConnection;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.libreoffice.LibreOfficeValueToRHeadJob;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RServeUtil;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.views.RShellView;

public class OfficeValueToRHeadAction extends Action {

	private final IWorkbenchWindow window;

	public OfficeValueToRHeadAction(String text, IWorkbenchWindow window) {

		super(text);
		this.window = window;
		setId("com.eco.bio7.office_value_to_R_Head");

	}

	public void run() {
		RConnection d = RServe.getConnection();

		if (d != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				LibreOfficeValueToRHeadJob job = new LibreOfficeValueToRHeadJob();
				job.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {

							RState.setBusy(false);
							RServeUtil.listRObjects();
						} else {
							RState.setBusy(false);
						}
					}
				});
				job.setUser(true);
				job.schedule();

			} else {

				Bio7Dialog.message("Rserve is busy!");
			}
		} else {

			MessageBox messageBox = new MessageBox(new Shell(),

			SWT.ICON_WARNING);
			messageBox.setMessage("RServer connection failed - Server is not running !");
			messageBox.open();

		}

	}

}