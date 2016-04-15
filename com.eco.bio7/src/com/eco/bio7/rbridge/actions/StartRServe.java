package com.eco.bio7.rbridge.actions;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.ISharedImages;
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
import com.eco.bio7.util.Util;
import com.eco.bio7.worldwind.WorldWindView;

public class StartRServe extends Action implements IMenuCreator {

	private final IWorkbenchWindow window;

	private Menu fMenu;

	private RConnectionJob job;

	private boolean remoteJob;

	private boolean reserveDetach = false;

	private static boolean fromDragDrop;

	private static String[] fileList;

	public StartRServe(String text, IWorkbenchWindow window) {
		super(text, AS_DROP_DOWN_MENU);
		this.window = window;

		setId("com.eco.bio7.start_rserve");

		setActionDefinitionId("com.eco.bio7.Start_RServe_Action");
		setImageDescriptor(com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/R.gif"));
		setMenuCreator(this);

	}

	public void run() {
		startStopRserve();
	}

	private void startStopRserve() {
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
								/*
								 * This will only be executed on a drag event!
								 */
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
							MessageDialog.openInformation(Util.getShell(), "R", "R-Server shutdown!");
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
								/*
								 * This will only be executed on a drag event!
								 */
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
								/*
								 * This will only be executed on a drag event!
								 */
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
							MessageDialog.openInformation(Util.getShell(), "R", "This is a remote Rserve connection\n" + "Please wait until the job has finished!\nA long running connection operation\n" + "could be caused by an unavailable server!");
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
						MessageDialog.openInformation(Util.getShell(), "R", "R-Server shutdown!\n" + "This is a remote Rserve connection\n" + "Please wait until the job has finished!\n");
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

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);
		
		MenuItem menuItem = new MenuItem(fMenu, SWT.PUSH);
		menuItem.setText("Start Rserve/Stop Rserve (within R)");
		//Image image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);

		menuItem.setImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/run_tool.png")));
		menuItem.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				startStopRserve();
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});

		MenuItem terminateRProcessMenuItem = new MenuItem(fMenu, SWT.PUSH);
		terminateRProcessMenuItem.setText("Terminate R");
		terminateRProcessMenuItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
		terminateRProcessMenuItem.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

				ConsolePageParticipant cpp = ConsolePageParticipant.getConsolePageParticipantInstance();
				String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
				new Thread() {
					@Override
					public void run() {

						if (selectionConsole.equals("R")) {

							if (cpp.getRProcess() != null) {
								try {
									cpp.getRProcess().destroy();
									RConnection con = RServe.getConnection();
									if (con != null) {
										con.close();
										RServe.setConnection(null);
										WorldWindView.setRConnection(null);
										RServe.setRrunning(false);
									}
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								cpp.setRProcess(null);
							}
						}
					}
				}.start();

				Bio7Dialog.message("Process terminated!");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});
		MenuItem menuItemFractal = new MenuItem(fMenu, SWT.PUSH);
		menuItemFractal.setText("Kill All R Processes");
		menuItemFractal.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE));
		menuItemFractal.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				boolean destroy = Bio7Dialog.decision("Should all running Rterm (Windows) or R (Linux, Mac) processes be destroyed?\n" + "If you you confirm all Rterm, R processes on the OS will be terminated!\n" + "Use only if no other Rterm, R instances are running!");

				if (destroy) {

					if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {

						TerminateRserve.killProcessRtermWindows();
					}

					else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {
						TerminateRserve.killProcessRtermLinux();
					}

					else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Mac")) {
						TerminateRserve.killProcessRtermMac();
					}

					ConsolePageParticipant cpp = ConsolePageParticipant.getConsolePageParticipantInstance();
					String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

					if (selectionConsole.equals("R")) {

						if (cpp.getRProcess() != null) {
							try {
								cpp.getRProcess().destroy();
								RConnection con = RServe.getConnection();
								if (con != null) {
									con.close();
									RServe.setConnection(null);
									WorldWindView.setRConnection(null);
									RServe.setRrunning(false);
								}
							} catch (Exception e2) {
								// TODO Auto-generated catch block
								e2.printStackTrace();
							}
							cpp.setRProcess(null);
						}
						Bio7Dialog.message("Terminated R process!");
					} else {
						Bio7Dialog.message("Please select the R console to terminate the process");
					}

				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});

		return fMenu;
	}

	@Override
	public Menu getMenu(Menu parent) {
		// TODO Auto-generated method stub
		return null;
	}

}
