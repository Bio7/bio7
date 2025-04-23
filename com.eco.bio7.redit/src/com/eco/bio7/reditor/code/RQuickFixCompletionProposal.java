package com.eco.bio7.reditor.code;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditor.actions.RefreshLoadedPackagesForCompletion;
import com.eco.bio7.reditor.images.ImagePool;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.util.Util;

public class RQuickFixCompletionProposal implements ICompletionProposal {

	String text;

	private int offset, contextTextLength;

	private String replace;

	private int lengthToReplace = 1;

	private String contextText;

	private String action;

	private REditor editor;

	RQuickFixCompletionProposal(String text, String contextText, int offset, int contextTextLength, String replace, int lengthToReplace, String action, REditor editor) {

		this.text = text;
		this.contextText = contextText;
		this.lengthToReplace = lengthToReplace;
		this.offset = offset;
		this.contextTextLength = contextTextLength;
		this.replace = replace;
		this.action = action;
		this.editor = editor;

	}

	@Override

	public void apply(IDocument document) {

		try {

			document.replace(offset, lengthToReplace, replace);

		} catch (BadLocationException e) {

			e.printStackTrace();

		}
		if (action != null) {
			if (action.equals("Reload_Package_Completion")) {

				libraryImportAndPackageReload();
			} else if (action.equals("Install_Missing_Packages")) {
                /*Here we use the replacement string as package name for the installation!*/
				installPackage(replace);
			}

		}

	}

	/**
	 * 
	 */
	/*
	 * We have to load the library here. It seems to be that the document replace
	 * function will not set the library in the source code early enough before the
	 * refresh function is called!
	 */
	private void libraryImportAndPackageReload() {
		/* Here we load the library! */
		RConnection con = REditor.getRserveConnection();
		if (con != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Job job = new Job("Evaluate R code") {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						monitor.beginTask("...", IProgressMonitor.UNKNOWN);

						try {
							con.eval(replace);
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						monitor.done();
						return Status.OK_STATUS;
					}

				};
				job.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							RState.setBusy(false);

							if (editor != null) {
								new RefreshLoadedPackagesForCompletion("", editor).run();
							}
						} else {

							RState.setBusy(false);
						}
					}
				});

				job.schedule();
				try {
					job.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				System.out.println("RServer is busy. Can't execute the R script!");
			}
		}

		else {
			System.out.println("No Rserve connection available!");
		}

	}

	private void installPackage(String packageName) {
		/* Here we load the library! */
		RConnection con = REditor.getRserveConnection();
		if (con != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Job job = new Job("Install package") {

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						monitor.beginTask("...", IProgressMonitor.UNKNOWN);
						IPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, "com.eco.bio7");
						String destdir = store.getString("InstallLocation");
						String server = store.getString("R_PACKAGE_SERVER");
						if (Util.getOS().equals("Windows")) {
							/* If a location is given! */

							destdir = destdir.replace("\\", "\\\\");

							String out = null;
							try {
								out = con.eval("try(paste(capture.output(install.packages(\"" + packageName + "\",dependencies=TRUE,repos =\"" + server + "\",lib=\"" + destdir + "\",destdir=\""
										+ destdir + "\")),collapse=\"\\n\"))").asString();
							} catch (REXPMismatchException e) {

								e.printStackTrace();
							} catch (RserveException e) {

								e.printStackTrace();
							}

							System.out.println(out);

						}
						/*
						 * For the packages on Linux and Mac we try the default path if no custom path
						 * is given!
						 */
						else {

							if (destdir.isEmpty() == false) {

								String out = null;
								try {
									out = con.eval("try(paste(capture.output(install.packages(\"" + packageName + "\",dependencies=TRUE,repos =\"" + server + "\",lib=\"" + destdir + "\",destdir=\""
											+ destdir + "\")),collapse=\"\\n\"))").asString();
								} catch (REXPMismatchException e) {

									e.printStackTrace();
								} catch (RserveException e) {

									e.printStackTrace();
								}

								System.out.println(out);

							} else {

								String out = null;
								try {
									out = con.eval("try(paste(capture.output(install.packages(\"" + packageName + "\",dependencies=TRUE,repos =\"" + server + "\")),collapse=\"\\n\"))").asString();
								} catch (REXPMismatchException e) {

									e.printStackTrace();
								} catch (RserveException e) {

									e.printStackTrace();
								}

								System.out.println(out);

							}

						}

						monitor.done();
						return Status.OK_STATUS;
					}

				};
				job.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {
							RState.setBusy(false);

							if (editor != null) {
								new RefreshLoadedPackagesForCompletion("", editor).run();
							}
						} else {

							RState.setBusy(false);
						}
					}
				});

				job.schedule();
				/*try {
					job.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
			} else {
				System.out.println("RServer is busy. Can't execute the R script!");
			}
		}

		else {
			System.out.println("No Rserve connection available!");
		}

	}

	@Override

	public Point getSelection(IDocument document) {

		// TODO Auto-generated method stub

		return null;

	}

	@Override

	public String getAdditionalProposalInfo() {

		// if null no context is generated!

		return contextText;

	}

	@Override

	public String getDisplayString() {

		return text;

	}

	@Override

	public Image getImage() {

		return ImagePool.image;

	}

	@Override

	public IContextInformation getContextInformation() {

		return null;

	}

}