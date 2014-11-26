package com.eco.bio7.rbridge.actions;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RSession;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.console.ConsoleInterpreterAction;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RConnectionJob;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.TerminateRserve;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.worldwind.WorldWindView;

public class StartRServe extends Action {

	private final IWorkbenchWindow window;

	private RConnectionJob job;

	private boolean remoteJob;

	private boolean reserveDetach = false;

	private RSession rSession = null;

	private static boolean fromDragDrop;

	private static String[] fileList;

	public StartRServe(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;

		setId("com.eco.bio7.start_rserve");

		setActionDefinitionId("com.eco.bio7.Start_RServe_Action");
		setImageDescriptor(com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/R.gif"));

	}

	public void run() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		boolean remote = store.getBoolean("REMOTE");
		if (remote == false) {

			if (store.getBoolean("RSERVE_NATIVE_START") == false) {

				if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
					if (RConnectionJob.getProc() != null) {
						RConnectionJob.getProc().destroy();
						RServe.setConnection(null);
						WorldWindView.setRConnection(null);
					}
					TerminateRserve.killProcessWindows();

				}

				else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {
					if (RServe.getConnection() != null) {
						RServe.getConnection().close();
						RServe.setConnection(null);
						WorldWindView.setRConnection(null);
					}

					TerminateRserve.killProcessLinux();

				} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Mac")) {
					if (RServe.getConnection() != null) {
						RServe.getConnection().close();
						RServe.setConnection(null);
						WorldWindView.setRConnection(null);
					}

					TerminateRserve.killProcessMac();

				}
				/* Establish a new Rserve connection! */
				if (RServe.isRrunning() == false) {
					RConnectionJob.setStore(Bio7Plugin.getDefault().getPreferenceStore());
					job = new RConnectionJob();
					job.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent event) {
							if (event.getResult().isOK()) {
								/* This will only be executed on a drag event! */
								if (fromDragDrop) {
									loadFile();
									fromDragDrop = false;

								}

							}
						}
					});

					job.setUser(true);
					job.schedule();

				}
				/* Shutdown Rserve! */
				else {

					RConnectionJob.setCanceled(true);

					RServe.setRrunning(false);

					RConnectionJob.getProc().destroy();

					RServe.setConnection(null);

					WorldWindView.setRConnection(null);
					/*
					 * if (RCompletionShell.getShellInstance() != null) {
					 * RCompletionShell.getShellInstance().dispose(); }
					 */

					// the following wrapped for BeanShell !
					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {

						public void run() {
							MessageDialog.openInformation(new Shell(), "R", "R-Server shutdown!");
						}
					});

				}
			}

			/* This actions starts Rserve by default in the native shell! */
			else {

				ConsoleInterpreterAction inst = ConsoleInterpreterAction.getInstance();
				/* Start the native R process! */
				inst.startR();

				RConnection con = RServe.getConnection();

				/* Close an existing Rserve connection! */
				if (con != null) {
					try {
						con.shutdown();
					} catch (RserveException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					RConnectionJob.setCanceled(true);
					con.close();
					RServe.setConnection(null);

					WorldWindView.setRConnection(null);
				}
				/* Start a new Rserve connection! */
				if (RServe.isRrunning() == false) {
					RConnectionJob.setStore(Bio7Plugin.getDefault().getPreferenceStore());
					job = new RConnectionJob();
					job.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent event) {
							if (event.getResult().isOK()) {
								/* This will only be executed on a drag event! */
								if (fromDragDrop) {
									loadFile();
									fromDragDrop = false;

								}

							}
						}
					});

					job.setUser(true);
					job.schedule();

				} else {
					RConnectionJob.setCanceled(true);

					RServe.setRrunning(false);

					RServe.setConnection(null);

					WorldWindView.setRConnection(null);

				}

				RServe.setRrunning(false);
				if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {

					ConsolePageParticipant.pipeInputToConsole("options(device='windows')", true, true);

				} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {

					ConsolePageParticipant.pipeInputToConsole("options(device='x11')", true, true);

				} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Mac")) {

					ConsolePageParticipant.pipeInputToConsole("options(device='cairo')", true, true);
				}
				// options(device='windows')
			}
		}

		else {// If it is a remote connection!

			if (RServe.isRrunning() == false) {
				RConnectionJob.setStore(Bio7Plugin.getDefault().getPreferenceStore());
				if (remoteJob == false) {
					remoteJob = true;

					job = new RConnectionJob();

					job.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent event) {
							if (event.getResult().isOK()) {
								remoteJob = false;
								/* This will only be executed on a drag event! */
								if (fromDragDrop) {
									loadFile();
									fromDragDrop = false;

								}

							}
						}
					});

					job.setUser(true);
					job.schedule();
				} else {
					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {

						public void run() {
							MessageDialog.openInformation(new Shell(), "R", "This is a remote Rserve connection\n" + "Please wait until the job has finished!\nA long running connection operation\n"
									+ "could be caused by an unavailable server!");
						}
					});

				}

			}

			else {

				if (RServe.getConnection() != null) {
					// If we want to resume a remote session!
					if (reserveDetach) {
						try {
							RSession rs = RServe.getConnection().detach();
							RState.setRSession(rs);
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else {
						// If we want to close a remote session!
						RServe.getConnection().close();
					}
				}

				RConnectionJob.setCanceled(true);

				RServe.setRrunning(false);

				RServe.setConnection(null);

				WorldWindView.setRConnection(null);
				/*
				 * if (RCompletionShell.getShellInstance() != null) {
				 * RCompletionShell.getShellInstance().dispose(); }
				 */

				// the following wrapped for BeanShell !
				Display display = PlatformUI.getWorkbench().getDisplay();
				display.syncExec(new Runnable() {

					public void run() {
						MessageDialog.openInformation(new Shell(), "R", "R-Server shutdown!\n" + "This is a remote Rserve connection\n" + "Please wait until the job has finished!\n");
					}
				});

			}

		}
	}

	private void loadFile() {
		String file;

		if (Bio7Dialog.getOS().equals("Windows")) {
			file = getFileList()[0].replace("\\", "\\\\");
		} else {
			file = getFileList()[0];
		}
		RConnection con = RServe.getConnection();
		if (con != null) {

			try {
				con.assign(".bio7TempRScriptFile", file);
			} catch (RserveException e) {

				e.printStackTrace();
			}

		}
		String load = "try(load(file=.bio7TempRScriptFile))";
		RInterpreterJob Do = new RInterpreterJob(load, false, null);
		Do.setUser(true);
		Do.schedule();

	}

	public static void setFileList(String[] fileList) {
		StartRServe.fileList = fileList;
	}

	public static String[] getFileList() {
		return fileList;
	}

	public static boolean isFromDragDrop() {
		return fromDragDrop;
	}

	public static void setFromDragDrop(boolean fromDragDrop) {
		StartRServe.fromDragDrop = fromDragDrop;
	}

}
