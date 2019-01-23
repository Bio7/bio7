/*******************************************************************************
 * Copyright (c) 2007-2015 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rcp;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jdt.launching.VMStandin;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener3;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.activities.IActivityManager;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.EditorAreaDropAdapter;
import org.eclipse.ui.internal.ide.IDEInternalWorkbenchImages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.part.EditorInputTransfer;
import org.eclipse.ui.part.MarkerTransfer;
import org.eclipse.ui.part.ResourceTransfer;
import org.osgi.framework.Bundle;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.collection.Work;
import com.eco.bio7.compile.BeanShellInterpreter;
import com.eco.bio7.compile.CompileClassAndMultipleClasses;
import com.eco.bio7.compile.GroovyInterpreter;
import com.eco.bio7.compile.JavaScriptInterpreter;
import com.eco.bio7.compile.PythonInterpreter;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.compile.utils.ScanClassPath;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.image.CanvasView;
import com.eco.bio7.javaeditor.Bio7EditorPlugin;
import com.eco.bio7.jobs.LoadData;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.preferences.RServePlotPrefs;
import com.eco.bio7.preferences.Reg;
import com.eco.bio7.rbridge.RConfig;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RServeUtil;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.actions.StartRServe;
import com.eco.bio7.rbridge.debug.REditorListener;
import com.eco.bio7.reditor.actions.OpenHelpBrowserAction;
import com.eco.bio7.time.CalculationThread;
import com.eco.bio7.util.Util;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	private static String OS;
	// protected String[] fileList;
	private boolean x11ErrorHandlerFixInstalled = false;
	private IExecutionListener executionListener;
	private IPartListener2 partListener;
	private static boolean themeBlack;
	private static final Point DEFAULT_SIZE = new Point(1024, 768);

	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);

		// WorkbenchAdapterBuilder.registerAdapters();
		declareWorkbenchImages();
		IDE.registerAdapters();
	}

	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}

	private Point getDisplaySize() {
		try {
			Display display = Display.getCurrent();
			Monitor monitor = display.getPrimaryMonitor();
			Rectangle rect = monitor.getBounds();
			return new Point(rect.width, rect.height);
		} catch (Throwable ignore) {
			return DEFAULT_SIZE;
		}
	}

	private void recalculateClasspath(IProject project, IProgressMonitor monitor) {
		IJavaProject javaProject = JavaCore.create(project);

		IFolder sourceFolder = project.getFolder("src");
		IPackageFragmentRoot fragRoot = javaProject.getPackageFragmentRoot(sourceFolder);

		List<IClasspathEntry> entriesJre = new ArrayList<IClasspathEntry>();

		IVMInstallType installType = JavaRuntime
				.getVMInstallType("org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType");

		VMStandin vmStandin = new VMStandin(installType, "Bio7 Bundled OpenJDK");
		vmStandin.setName("Bio7 Bundled OpenJDK");

		String path = Platform.getInstallLocation().getURL().getPath();
		/*
		 * Extra path for the different MacOSX installation paths!
		 */
		if (OS.equals("Mac")) {
			vmStandin.setInstallLocation(new File(path + "../MacOS/jdk/Contents/Home/"));

		} else {
			vmStandin.setInstallLocation(new File(path + "/jdk"));
		}

		IVMInstall vmInstall = vmStandin.convertToRealVM();

		// ‚IVMInstall
		// vmInstall =
		// JavaRuntime.getDefaultVMInstall();

		LibraryLocation[] locations = JavaRuntime.getLibraryLocations(vmInstall);
		for (LibraryLocation element : locations) {

			entriesJre.add(JavaCore.newLibraryEntry(element.getSystemLibraryPath(), null, null));
		}
		IClasspathEntry[] newEntries = new ScanClassPath().scanForJDT();

		IClasspathEntry[] oldEntries = entriesJre.toArray(new IClasspathEntry[entriesJre.size()]);

		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
		newEntries[oldEntries.length] = JavaCore.newSourceEntry(fragRoot.getPath());

		try {
			javaProject.setRawClasspath(newEntries, monitor);
		} catch (JavaModelException e) {
			// Auto-generated
			// catch block
			// e.printStackTrace();
			System.out.println(
					"Minor error! Please check the classpath of the project and if necessary calculate again!");
		}

		// Bio7Dialog.message("Java
		// Bio7 Project
		// Libraries
		// Recalculated!");
	}

	public void preWindowOpen() {
		String osname = System.getProperty("os.name");
		if (osname.startsWith("Windows")) {
			OS = "Windows";
		} else if (osname.equals("Linux")) {
			OS = "Linux";
		} else if (osname.startsWith("Mac")) {
			OS = "Mac";
		}
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IResourceChangeListener listener = new IResourceChangeListener() {
			public void resourceChanged(IResourceChangeEvent event) {

				if (event == null || event.getDelta() == null) {

					return;
				}
				if (event.getType() != IResourceChangeEvent.POST_CHANGE)
					return;

				else {
					try {
						event.getDelta().accept(new IResourceDeltaVisitor() {
							boolean decision = false;

							public boolean visit(IResourceDelta delta) throws CoreException {
								if (delta.getKind() == IResourceDelta.ADDED) {

									final IResource resource = delta.getResource();
									if (resource.getType() == 4) {

										IProject project = resource.getProject();
										if (project.isOpen()) {

											if (project.hasNature(JavaCore.NATURE_ID)) {

												Job job = new Job("Recalculate Classpath Job") {
													protected IStatus run(IProgressMonitor monitor) {
														monitor.beginTask("Recalculate classpath.....",
																IProgressMonitor.UNKNOWN);
														javafx.application.Platform.runLater(new Runnable() {
															@Override
															public void run() {
																try {
																	Thread.sleep(200);
																} catch (InterruptedException e) {
																	// TODO Auto-generated catch block
																	e.printStackTrace();
																}
																recalculateClasspath(project, monitor);
															}
														});

														return Status.OK_STATUS;

													}
												};
												job.addJobChangeListener(new JobChangeAdapter() {
													public void done(IJobChangeEvent event) {
														if (event.getResult().isOK()) {
															System.out.println(
																	"Java Bio7 Project Libraries Recalculated!");
														}

														else {

														}

													}
												});
												// job.setSystem(true);
												job.schedule(); // start as soon as possible

											}
										}
									}

								}
								return true;
							}
						});
					} catch (CoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}

		};

		workspace.addResourceChangeListener(listener);

		// ... some time later one ...
		// workspace.removeResourceChangeListener(listener);

		IWorkbenchActivitySupport workbenchActivitySupport = PlatformUI.getWorkbench().getActivitySupport();
		IActivityManager activityManager = workbenchActivitySupport.getActivityManager();

		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();

		// Display display = configurer.getWindow().getWorkbench().getDisplay();

		Shell shell = configurer.getWindow().getShell();

		// configurer.setSaveAndRestore( false );
		configurer.setInitialSize(getDisplaySize());
		shell.setLocation(0, 0);

		configurer.setShowCoolBar(true);
		configurer.setShowStatusLine(true);
		configurer.setShowFastViewBars(true);
		configurer.setShowPerspectiveBar(true);
		configurer.setShowProgressIndicator(true);
		// Drag and drop support for the editor area!
		configurer.addEditorAreaTransfer(EditorInputTransfer.getInstance());
		configurer.addEditorAreaTransfer(ResourceTransfer.getInstance());
		configurer.addEditorAreaTransfer(FileTransfer.getInstance());
		configurer.addEditorAreaTransfer(MarkerTransfer.getInstance());
		configurer.configureEditorAreaDropListener(new EditorAreaDropAdapter(configurer.getWindow()));

		// PreferenceManager preferenceManager =
		// configurer.getWorkbenchConfigurer().getWorkbench().getPreferenceManager
		// ();

		// Remove unused preference pages by ID:
		/*
		 * preferenceManager.remove( "org.eclipse.help.ui.browsersPreferencePage")
		 * ;preferenceManager.remove(
		 * "org.eclipse.update.internal.ui.preferences.MainPreferencePage");
		 */

		// Listen to changed perspective !!!!
		configurer.getWindow().addPerspectiveListener(new IPerspectiveListener3() {

			@Override
			public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective,
					IWorkbenchPartReference partRef, String changeId) {
				// TODO Auto-generated method stub

			}

			public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
				/*
				 * // Workaround a bug on MacOSX when closing a SWT_AWT perspective // 3D and
				 * WorldWind! if (OS.equals("Mac")) { if
				 * (perspective.getId().equals("com.eco.bio7.perspective_3d")) {
				 * 
				 * Work.openView("com.eco.bio7.spatial"); }
				 * 
				 * else if (perspective.getId().equals("com.eco.bio7.WorldWind.3dglobe")) {
				 * Work.openView("com.eco.bio7.worldwind.WorldWindView");
				 * 
				 * } }
				 */

			}

			@Override
			public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
				// TODO Auto-generated method stub

			}

			@Override
			public void perspectiveOpened(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
				// TODO Auto-generated method stub

			}

			@Override
			public void perspectiveClosed(IWorkbenchPage page, IPerspectiveDescriptor perspective) {

			}

			@Override
			public void perspectiveDeactivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
				// Workaround a bug on MacOSX when closing a SWT_AWT perspective
				// 3D and WorldWind!
				if (OS.equals("Mac")) {
					if (perspective.getId().equals("com.eco.bio7.perspective_3d")) {

						Work.closeView("com.eco.bio7.spatial");
					}

					else if (perspective.getId().equals("com.eco.bio7.WorldWind.3dglobe")) {
						Work.closeView("com.eco.bio7.worldwind.WorldWindView");

					}
				}

			}

			@Override
			public void perspectiveSavedAs(IWorkbenchPage page, IPerspectiveDescriptor oldPerspective,
					IPerspectiveDescriptor newPerspective) {

			}

		});

		/*
		 * Listen to the R editor if debugging actions should be added to the console
		 * toolbar! Also creates a (not visible) shell for MacOSX to get editor focus
		 * after an ImageJ event!
		 */

		configurer.getWindow().getPartService().addPartListener(new REditorListener().listen());

		PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_SYSTEM_JOBS, false);
		PlatformUI.getPreferenceStore().setDefault(IWorkbenchPreferenceConstants.DOCK_PERSPECTIVE_BAR,
				IWorkbenchPreferenceConstants.TOP_LEFT);
		PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS, false);
		PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_INTRO, true);
		PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_MEMORY_MONITOR, true);
		// Option for Bio7 1.6!
		// WorkbenchPlugin.getDefault().getPreferenceStore().setValue("RUN_IN_BACKGROUND",
		// true);

		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();

		IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();
		registry.setDefaultEditor("*.Rnw", "org.eclipse.ui.DefaultTextEditor");

		// Important to set the path for the database.
		// **********************************************************************
		// ****

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
		String path = fileUrl.getFile();

		// System.out.println(path);
		File fileStartupScripts = new File(path + "/startup_scripts");
		File fileGeneralScripts = new File(path + "/scripts");
		File fileRShellScripts = new File(path + "/r_shell_scripts");
		File fileGridScripts = new File(path + "/grid_scripts");

		/* Folder for the temp *.RData file! */
		File fileTempR = new File(fileUrl.getPath());
		String pathTempR = fileTempR.toString();

		if (getOS().equals("Windows")) {
			String reg1 = "";
			String reg2 = "";
			reg1 = Reg.setPrefReg(PreferenceConstants.PATH_R);
			reg2 = Reg.setPrefReg(PreferenceConstants.PATH_LIBREOFFICE);
			if (reg1 != null) {
				store.setDefault(PreferenceConstants.PATH_R, reg1);
				/* Default install location for the packages! */
				store.setDefault("InstallLocation", reg1 + "\\site-library");
				store.setDefault("SweaveScriptLocation", reg1 + "/share/texmf/tex/latex");
				store.setDefault("pdfLatex", "");
				store.setDefault("RSERVE_ARGS", "");

			}
			if (reg2 != null) {
				store.setDefault(PreferenceConstants.PATH_LIBREOFFICE, reg2);
			} else {
				/*
				 * If the path cannot be found in the reg. it will be set to C:\ -> see
				 * com.eco.bio7.preferences.Reg.java!
				 */
				store.setDefault(PreferenceConstants.PATH_LIBREOFFICE, "C:\\");
			}
		} else if (getOS().equals("Linux")) {
			// store.setDefault(PreferenceConstants.PATH_LIBREOFFICE, reg2);
			// reg1 = "/usr/lib/R";
			// reg2 = "/usr/lib/libreoffice/program";
			/*
			 * Now leave the R path empty by default to grab the systems path!
			 */
			store.setDefault(PreferenceConstants.PATH_R, "");
			store.setDefault(PreferenceConstants.PATH_LIBREOFFICE, "");
			/*
			 * For the packages on Linux we try the default path if no custom path is given!
			 */
			store.setDefault("InstallLocation", "");
			store.setDefault("SweaveScriptLocation", "");
			store.setDefault("pdfLatex", "");
			store.setDefault("RSERVE_ARGS", "");

			/*
			 * if (is64BitVM()) { store.setDefault("r_pipe_path", reg1 + "/bin"); } else {
			 * store.setDefault("r_pipe_path", reg1 + "/bin"); }
			 */

		} else if (getOS().equals("Mac")) {
			/*
			 * Bundle bundlenew = Platform.getBundle("Bundled_R");
			 * 
			 * URL locationUrlMac = FileLocator.find(bundlenew, new Path("/R"), null); URL
			 * fileUrlMac = null; try { fileUrlMac = FileLocator.toFileURL(locationUrlMac);
			 * } catch (IOException e2) {
			 * 
			 * e2.printStackTrace(); }
			 * 
			 * File file = new File(fileUrlMac.getFile()); path = file.getAbsolutePath();
			 */
			/*
			 * We adjust the R default path for MacOSX!
			 */
			store.setDefault(PreferenceConstants.PATH_R, "/Library/Frameworks/R.framework/Resources");
			store.setDefault(PreferenceConstants.PATH_LIBREOFFICE, "");
			/*
			 * For the packages on MacOSX we try the default path if no custom path is
			 * given!
			 */
			store.setDefault("InstallLocation", "");
			store.setDefault("SweaveScriptLocation", "");
			store.setDefault("pdfLatex", "");
			store.setDefault("RSERVE_ARGS", "");
			// reg2 = "";
			// reg2 = "/Applications/LibreOffice.app/Contents/MacOS";
			// store.setDefault(PreferenceConstants.PATH_LIBREOFFICE, reg2);

		}

		// store.setDefault("R_STARTUP_ARGS","try(options(max.print=5000)\n)");
		store.setDefault("SHOW_JDT_GUI", false);
		store.setDefault("datatablesize", 100);
		store.setDefault(PreferenceConstants.D_STRING, fileStartupScripts.getAbsolutePath());
		store.setDefault(PreferenceConstants.D_SCRIPT_GENERAL, fileGeneralScripts.getAbsolutePath());
		store.setDefault(PreferenceConstants.D_RSHELL_SCRIPTS, fileRShellScripts.getAbsolutePath());
		store.setDefault(PreferenceConstants.D_GRID_SCRIPTS, fileGridScripts.getAbsolutePath());
		store.setDefault("SAVE_R_WORKSPACE_ON_QUIT", false);
		store.setDefault("ON_QUIT_COMMAND", "save.image(file='session.RData')");

		if (getOS().equals("Windows")) {
			store.setDefault("SAVE_ALL_EDITORS", false);
			String pathTempR2 = pathTempR + "\\bio7temp\\";
			// String pathTempR3 = pathTempR2.replace("\\", "\\\\");
			store.setDefault(PreferenceConstants.P_TEMP_R, pathTempR2);
			store.setDefault("Console_Encoding", "CP850");
			store.setDefault("DEVICE_DEFINITION", ".bio7Device <- function(filename = \"" + pathTempR2
					+ "tempDevicePlot%05d.tiff"
					+ "\") { tiff(filename,width = 480, height = 480, units = \"px\")}; options(device=\".bio7Device\")");
			store.setDefault("DEVICE_FILENAME", "");
			store.setDefault("PLOT_DEVICE_SELECTION", "PLOT_IMAGE");
			store.setDefault("PDF_READER", "ACROBAT");
		} else if (getOS().equals("Linux")) {
			store.setDefault("SAVE_ALL_EDITORS", false);
			pathTempR = pathTempR + "/bio7temp/";
			store.setDefault(PreferenceConstants.P_TEMP_R, pathTempR);
			store.setDefault("Console_Encoding", "UTF-8");
			store.setDefault("shell_arguments", "");
			store.setDefault("DEVICE_DEFINITION", ".bio7Device <- function(filename = \"" + pathTempR
					+ "tempDevicePlot%05d.tiff"
					+ "\") { tiff(filename,width = 480, height = 480, units = \"px\")}; options(device=\".bio7Device\")");
			store.setDefault("DEVICE_FILENAME", "");
			store.setDefault("PLOT_DEVICE_SELECTION", "PLOT_IMAGE");
			store.setDefault("PDF_READER", "OKULAR");
		} else if (getOS().equals("Mac")) {
			store.setDefault("SAVE_ALL_EDITORS", true);
			pathTempR = pathTempR + "/bio7temp/";
			store.setDefault(PreferenceConstants.P_TEMP_R, pathTempR);
			store.setDefault("Console_Encoding", "UTF-8");
			store.setDefault("shell_arguments", "export TERM=xterm");
			store.setDefault("DEVICE_DEFINITION",
					".bio7Device <- function(filename = \"" + pathTempR + "tempDevicePlot%05d.tiff"
							+ "\") { tiff(filename,width = 480, height = 480)}; options(device=\".bio7Device\")");
			store.setDefault("DEVICE_FILENAME", "");
			store.setDefault("PLOT_DEVICE_SELECTION", "PLOT_IMAGE");
			store.setDefault("PDF_READER", "ACROBAT");
		}

		// store.setDefault("RSERVE_AUTOSTART", false);
		store.setDefault("R_PACKAGE_SERVER", "http://cran.r-project.org");
		if (getOS().equals("Linux")) {
			store.setDefault("knitroptions",
					"opts_chunk$set(dev=\"png\",echo=TRUE, dev.args=list(type=\"cairo\"),dpi=96)");

			// store.setDefault(PreferenceConstants.D_OPENOFFICE_HEAD, "[^a-zA-Z0-9_.]");
		} else if (getOS().equals("Mac")) {
			store.setDefault("knitroptions",
					"opts_chunk$set(dev=\"png\",echo=TRUE, dev.args=list(type=\"quartz\"),dpi=96)");

			// store.setDefault(PreferenceConstants.D_OPENOFFICE_HEAD, "[^a-zA-Z0-9_.]");
		} else {
			store.setDefault("knitroptions",
					"opts_chunk$set(dev=\"png\",echo=TRUE, dev.args=list(type=\"cairo\"),dpi=96)");

			// store.setDefault(PreferenceConstants.D_OPENOFFICE_HEAD, "[^a-zA-Z0-9_.]");
		}
		store.setDefault("BROWSER_SELECTION", "JAVAFX_BROWSER");
		store.setDefault("OPEN_BOWSER_IN_EXTRA_VIEW", false);
		store.setDefault("COPY_PDF_PATH_TO_CLIP", true);
		store.setDefault("REQUEST_EDITOR_FOCUS", true);
		store.setDefault("INTERPRET_JAVASCRIPT_IN_BROWSER", false);
		store.setDefault("INSTALL_R_PACKAGES_DESCRPTION_URL", "https://cran.r-project.org/web/packages/");
		store.setDefault("DETECT_R_PROCESS", true);
		store.setDefault("R_DEBUG_PORT", 21555);
		store.setDefault("RSERVE_CLIENT_CONNECTION_PORT", 6311);
		store.setDefault("SHINY_PORT", 5559);
		store.setDefault("LATEX_ENGINE", "pdflatex");
		store.setDefault("BIBTEX_ENGINE", "bibtex");
		store.setDefault("STREAM_TO_RSHELL", false);
		store.setDefault("R_SOURCE_OPTIONS", "echo=F");
		store.setDefault("RSHELL_TYPED_CODE_COMPLETION", true);
		store.setDefault("RSHELL_ACTIVATION_CHARS", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz.@$+-.:");
		store.setDefault("RSHELL_SEPERATOR_CHARS", ";(,[=-+ ");
		store.setDefault("CODE_COMPLETION_POPUP_SIZE_X", 600);
		store.setDefault("CODE_COMPLETION_POPUP_SIZE_Y", 400);
		store.setDefault("RSHELL_CODE_COMPLETION_ACTIVATOR_ALTERED", "STRG");

		store.setDefault("LATEX_CLEAN_FILES", false);
		store.setDefault("LATEX_FILES_EXT_DELETE", "out,log,nav,aux,bbl,blg,dvi,toc,.synctex.gz,snm");
		store.setDefault("LINUX_SHELL", "GNOME");
		store.setDefault("USE_CUSTOM_DEVICE", true);
		store.setDefault("PDF_USE_BROWSER", false);
		store.setDefault("IMPORT_R_PLOT_VIRTUAL", false);
		store.setDefault("IMAGEJ_CREATE_SINGLE_PLOTS", false);

		store.setDefault("ENABLE_JAVAFXWEBKIT_SCROLLBAR", false);
		store.setDefault("SCROLL_TO_DOCUMENT_END", false);
		store.setDefault("ENABLE_BROWSER_LOG", false);
		store.setDefault("ENABLE_BROWSER_SCROLLBARS", true);

		store.setDefault("CLOSE_DEVICE", "if(length(dev.list())>0) dev.off()");

		store.setDefault("blender_args", "--window-geometry 0 0 600 600");
		store.setDefault("rcmdinstall", "--build");
		store.setDefault("REMOTE", false);
		store.setDefault("HOST", "127.0.0.1");
		store.setDefault("TCP", 6311);
		store.setDefault("USERNAME", "bio7");
		store.setDefault("PASSWORD", "admin");

		store.setDefault("DEFAULT_DIGITS", 15);
		store.setDefault("TRANSFER_METHOD", false);

		store.setDefault("REPAINT_QUAD", true);
		store.setDefault("REPAINT_HEX", true);
		store.setDefault("RECORD_VALUES", true);
		store.setDefault("QUAD_PANEL_SCROLLBAR", false);
		store.setDefault("POINTS_PANEL_SCROLLBAR", false);
		store.setDefault("STARTUP_SCRIPTS", false);
		store.setDefault("python_3x", false);
		store.setDefault("python_process_extra", true);
		store.setDefault("blender_options", "interactive");
		store.setDefault("before_script_blender",
				"import bpy;bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete()");
		store.setDefault("after_script_blender",
				"bpy.ops.render.render();bpy.ops.wm.redraw_timer(type='DRAW_WIN_SWAP', iterations=1)");

		/* A default FPS setting for the 3d view! */
		store.setDefault("fixedFps", 60);

		/*
		 * Default Colours for the Bio7 editors!
		 */

		Font terminalFont = JFaceResources.getFont(JFaceResources.TEXT_FONT);
		if (getOS().equals("Windows")) {
			PreferenceConverter.setDefault(store, "RShellFonts", terminalFont.getFontData());
			PreferenceConverter.setDefault(store, "Bio7ShellFonts", terminalFont.getFontData());

		} else if (getOS().equals("Linux")) {
			PreferenceConverter.setDefault(store, "RShellFonts", terminalFont.getFontData());
			PreferenceConverter.setDefault(store, "Bio7ShellFonts", terminalFont.getFontData());

		} else if (getOS().equals("Mac")) {
			PreferenceConverter.setDefault(store, "RShellFonts", terminalFont.getFontData());
			PreferenceConverter.setDefault(store, "Bio7ShellFonts", terminalFont.getFontData());

		}
		// Monitor []mon = Display.getDefault().getMonitors(); // returns an
		// array of monitors attached to device and 0 fetches first one.

		// System.out.println(mon[0].getBounds());//Rectangle {0, 0, 1920, 1080}
		// or:
		/*
		 * Device device = Display.getDefault();
		 * 
		 * System.out.println("getBounds(): " + device.getBounds());
		 * System.out.println("getClientArea(): " + device.getClientArea());
		 * System.out.println("getDepth(): " + device.getDepth());
		 * System.out.println("getDPI(): " + device.getDPI());
		 */
		// IPreferenceStore storeJava =
		// Bio7EditorPlugin.getDefault().getPreferenceStore();

		// storeJava.setDefault("classbody", false);

		final String pathTo;
		if (getOS().equals("Windows")) {
			pathTo = store.getString(PreferenceConstants.P_TEMP_R) + "\\";
		} else {
			pathTo = store.getString(PreferenceConstants.P_TEMP_R) + "/";
		}
		store.addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {

				if (event.getProperty() == "DEVICE_DEFINITION") {

					String value = event.getNewValue().toString();
					if (getOS().equals("Windows")) {

						value = value.replace("\\", "/");
					}
					if (RState.isBusy() == false) {
						RConnection con = RServe.getConnection();
						if (con != null) {
							try {
								con.eval(value);

							} catch (RserveException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					} else {
						System.out.println("Couldn't change R preferences-> Rserve is busy!");
					}
				} else if (event.getProperty() == "USE_CUSTOM_DEVICE") {
					String val = event.getNewValue().toString();
					if (RState.isBusy() == false) {
						RConnection con = RServe.getConnection();
						if (con != null) {
							try {
								if (val.equals("false")) {
									if (getOS().equals("Windows")) {
										con.eval("options(device=\"windows\")");
									} else if (getOS().equals("Linux")) {
										con.eval("options(device=\"x11\")");
									} else if (getOS().equals("Mac")) {
										con.eval("options(device=\"quartz\")");
									}
								} else {
									con.eval("options(device=\".bio7Device\")");
								}

							} catch (RserveException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					} else {
						System.out.println("Couldn't change R preferences-> Rserve is busy!");
					}

				}

				else if (event.getProperty() == "PLOT_DEVICE_SELECTION") {

					IPreferenceStore store2 = Bio7Plugin.getDefault().getPreferenceStore();

					String sel = store2.getString("PLOT_DEVICE_SELECTION");

					RServePlotPrefs prefsPlotRserve = RServePlotPrefs.getInstance();

					if (sel.equals("PLOT_IMAGE")) {

						prefsPlotRserve.mult.setStringValue(".bio7Device <- function(filename = \"" + pathTo
								+ "tempDevicePlot%05d.tiff"
								+ "\") { tiff(filename,width = 480, height = 480, units = \"px\")}; options(device=\".bio7Device\")");
						prefsPlotRserve.deviceFilename.setStringValue("");
						prefsPlotRserve.deviceFilename.setEnabled(false, prefsPlotRserve.getFieldEditorParentControl());
					} else if (sel.equals("PLOT_CAIRO")) {

						if (getOS().equals("Mac")) {

							Bio7Dialog.message("Cairo not supported on MacOSX!");

						} else {
							prefsPlotRserve.mult.setStringValue(".bio7Device <- function(filename = \"" + pathTo
									+ "tempDevicePlot%05d.tiff"
									+ "\") { tiff(filename,width = 480, height = 480, type=\"cairo\")}; options(device=\".bio7Device\")");
							prefsPlotRserve.deviceFilename.setStringValue("");
							prefsPlotRserve.deviceFilename.setEnabled(false,
									prefsPlotRserve.getFieldEditorParentControl());
						}
					}

					else if (sel.equals("PLOT_PRINT")) {

						prefsPlotRserve.mult.setStringValue(".bio7Device <- function(filename = \"" + pathTo
								+ "tempDevicePlot%05d.tiff"
								+ "\") { tiff(filename,width = 6, height = 6, units=\"in\",res=600)}; options(device=\".bio7Device\")");
						prefsPlotRserve.deviceFilename.setStringValue("");
						prefsPlotRserve.deviceFilename.setEnabled(false, prefsPlotRserve.getFieldEditorParentControl());
					} else if (sel.equals("PLOT_PDF")) {

						prefsPlotRserve.mult.setStringValue(".bio7Device <- function(filename = \"" + pathTo
								+ "tempDevicePlot.pdf" + "\") { pdf(filename)}; options(device=\".bio7Device\")");
						prefsPlotRserve.deviceFilename.setStringValue("tempDevicePlot.pdf");
						prefsPlotRserve.deviceFilename.setEnabled(true, prefsPlotRserve.getFieldEditorParentControl());
					}

					else if (sel.equals("PLOT_SVG")) {

						prefsPlotRserve.mult.setStringValue(".bio7Device <- function(filename = \"" + pathTo
								+ "tempDevicePlot.svg" + "\") { svg(filename)}; options(device=\".bio7Device\")");
						prefsPlotRserve.deviceFilename.setStringValue("tempDevicePlot.svg");
						prefsPlotRserve.deviceFilename.setEnabled(true, prefsPlotRserve.getFieldEditorParentControl());

					} else if (sel.equals("PLOT_POSTSCRIPT")) {

						prefsPlotRserve.mult
								.setStringValue(".bio7Device <- function(filename = \"" + pathTo + "tempDevicePlot.eps"
										+ "\") { postscript(filename)}; options(device=\".bio7Device\")");
						prefsPlotRserve.deviceFilename.setStringValue("tempDevicePlot.eps");
						prefsPlotRserve.deviceFilename.setEnabled(true, prefsPlotRserve.getFieldEditorParentControl());
					}

					else if (sel.equals("PLOT_IMAGEJ_IMAGESIZE")) {

						prefsPlotRserve.mult.setStringValue(".bio7Device <- function(filename = \"" + pathTo
								+ "tempDevicePlot%05d.tiff" + "\") {"
								+ " tryCatch(tiff(filename,width = imageSizeX, height = imageSizeY, units = \"px\"),error = function (x) {tiff(filename,width = 512, height = 512, units = \"px\"); print('ImageSizeX and ImageSizeY variables not defined in R Workspace. Applied default values!')})"
								+ "}; options(device=\".bio7Device\")");
						prefsPlotRserve.deviceFilename.setStringValue("");
						prefsPlotRserve.deviceFilename.setEnabled(false, prefsPlotRserve.getFieldEditorParentControl());
					}

					else if (sel.equals("PLOT_IMAGEJ_IMAGESIZE_CAIRO")) {

						if (getOS().equals("Mac")) {
							Bio7Dialog.message("Cairo not supported on MacOSX!");
						} else {

							prefsPlotRserve.mult.setStringValue(".bio7Device <- function(filename = \"" + pathTo
									+ "tempDevicePlot%05d.tiff" + "\") { "
									+ "tryCatch(tiff(filename,width = imageSizeX, height = imageSizeY, units = \"px\",type=\"cairo\"),error = function (x) {tiff(filename,width = 512, height = 512, units = \"px\",type=\"cairo\"); print('ImageSizeX and ImageSizeY variables not defined in R Workspace. Applied default values!')})"
									+ "}; options(device=\".bio7Device\")");
							prefsPlotRserve.deviceFilename.setStringValue("");
							prefsPlotRserve.deviceFilename.setEnabled(false,
									prefsPlotRserve.getFieldEditorParentControl());
						}
					}

					else if (sel.equals("PLOT_IMAGEJ_DISPLAYSIZE_CAIRO")) {
						if (getOS().equals("Mac")) {
							Bio7Dialog.message("Cairo not supported on MacOSX!");
						} else {
							CanvasView view = CanvasView.getCanvas_view();
							/* Height correction for the plot! */
							int correction = 0;
							if (CanvasView.tabFolder.isDisposed() == false && CanvasView.tabFolder != null) {
								correction = CanvasView.tabFolder.getTabHeight();
							}

							if (view != null) {
								Rectangle rec = view.getParent2().getClientArea();
								if (rec.width > 0 && rec.height > correction) {
									prefsPlotRserve.mult.setStringValue(".bio7Device <- function(filename = \"" + pathTo
											+ "tempDevicePlot%05d.tiff" + "\") { tiff(filename,width = " + rec.width
											+ ", height = " + (rec.height - 100)
											+ ", type=\"cairo\")}; options(device=\".bio7Device\")");
								} else {
									prefsPlotRserve.mult.setStringValue(
											".bio7Device <- function(filename = \"" + pathTo + "tempDevicePlot%05d.tiff"
													+ "\") { tiff(filename,width = " + 512 + ", height = " + 512
													+ ", type=\"cairo\")}; options(device=\".bio7Device\")");

								}
								prefsPlotRserve.deviceFilename.setStringValue("");
								prefsPlotRserve.deviceFilename.setEnabled(false,
										prefsPlotRserve.getFieldEditorParentControl());
							}
						}

					} else if (sel.equals("PLOT_IMAGEJ_DISPLAYSIZE")) {

						CanvasView view = CanvasView.getCanvas_view();
						/* Height correction for the plot! */
						int correction = 0;
						if (CanvasView.tabFolder.isDisposed() == false && CanvasView.tabFolder != null) {
							correction = CanvasView.tabFolder.getTabHeight();
						}
						if (view != null) {
							Rectangle rec = view.getParent2().getClientArea();
							if (rec.width > 0 && rec.height > correction) {
								prefsPlotRserve.mult.setStringValue(".bio7Device <- function(filename = \"" + pathTo
										+ "tempDevicePlot%05d.tiff" + "\") { tiff(filename,width =  " + rec.width
										+ ", height = " + (rec.height - correction)
										+ ", units = \"px\")}; options(device=\".bio7Device\")");
							} else {
								prefsPlotRserve.mult.setStringValue(".bio7Device <- function(filename = \"" + pathTo
										+ "tempDevicePlot%05d.tiff" + "\") { tiff(filename,width =  " + 512
										+ ", height = " + 512 + ", units = \"px\")}; options(device=\".bio7Device\")");

							}
							prefsPlotRserve.deviceFilename.setStringValue("");
							prefsPlotRserve.deviceFilename.setEnabled(false,
									prefsPlotRserve.getFieldEditorParentControl());
						}
					}

				}
			}

		});

		// Initialize important classes for Bio7!!!
		initBio7();

	}

	private void setComponentFont() {
		IPreferenceStore store = com.eco.bio7.image.Activator.getDefault().getPreferenceStore();
		boolean antialiasedFonts = false;
		antialiasedFonts = store.getBoolean("FONT_ANTIALIASED");
		if (antialiasedFonts) {
			System.setProperty("awt.useSystemAAFontSettings", "on");
			System.setProperty("swing.aatext", "true");
		}

		Display dis = Util.getDisplay();

		assert EventQueue.isDispatchThread(); // On AWT event thread

		FontData fontData = dis.getSystemFont().getFontData()[0];

		int resolution = Toolkit.getDefaultToolkit().getScreenResolution();

		// int dpi = Util.getDisplay().getDPI().x;

		int awtFontSize = (int) Math.round((double) fontData.getHeight() * resolution / 72.0);
		// int awtFontSize = (int) Math.round((double) fontData.getHeight() * resolution
		// / dpi);
		java.awt.Font awtFont = null;

		int fontSizeCorrection = 0;
		fontSizeCorrection = store.getInt("FONT_SIZE_CORRECTION");
		/* Font size correction! */

		awtFont = new java.awt.Font(fontData.getName(), fontData.getStyle(), awtFontSize + fontSizeCorrection);
		// System.out.println("DPI: "+dpi+" fonsize:"+awtFontSize );
		// Update the look and feel defaults to use new font.
		updateLookAndFeel(awtFont);

	}

	private void updateLookAndFeel(java.awt.Font awtFont) {
		assert awtFont != null;
		assert EventQueue.isDispatchThread(); // On AWT event thread

		FontUIResource fontResource = new FontUIResource(awtFont);

		UIManager.put("Button.font", fontResource); //$NON-NLS-1$
		UIManager.put("CheckBox.font", fontResource); //$NON-NLS-1$
		UIManager.put("ComboBox.font", fontResource); //$NON-NLS-1$
		UIManager.put("EditorPane.font", fontResource); //$NON-NLS-1$
		UIManager.put("Label.font", fontResource); //$NON-NLS-1$
		UIManager.put("List.font", fontResource); //$NON-NLS-1$
		UIManager.put("Panel.font", fontResource); //$NON-NLS-1$
		UIManager.put("ProgressBar.font", fontResource); //$NON-NLS-1$
		UIManager.put("RadioButton.font", fontResource); //$NON-NLS-1$
		UIManager.put("ScrollPane.font", fontResource); //$NON-NLS-1$
		UIManager.put("TabbedPane.font", fontResource); //$NON-NLS-1$
		UIManager.put("Table.font", fontResource); //$NON-NLS-1$
		UIManager.put("TableHeader.font", fontResource); //$NON-NLS-1$
		UIManager.put("TextField.font", fontResource); //$NON-NLS-1$
		UIManager.put("TextPane.font", fontResource); //$NON-NLS-1$
		UIManager.put("TitledBorder.font", fontResource); //$NON-NLS-1$
		UIManager.put("ToggleButton.font", fontResource); //$NON-NLS-1$
		UIManager.put("TreeFont.font", fontResource); //$NON-NLS-1$
		UIManager.put("ViewportFont.font", fontResource); //$NON-NLS-1$
		UIManager.put("MenuItem.font", fontResource); //$NON-NLS-1$
		UIManager.put("CheckboxMenuItem.font", fontResource); //$NON-NLS-1$
		UIManager.put("PopupMenu.font", fontResource); // $NON-NLS-1

		java.util.Enumeration keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof javax.swing.plaf.FontUIResource)
				UIManager.put(key, fontResource);
		}

	}

	private void initBio7() {
		setComponentFont();
		/* Set the system look and feel! */
		if (getOS().equals("Windows")) {

			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException e) {

				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {

				e.printStackTrace();
			} catch (UnsupportedLookAndFeelException e) {
				e.printStackTrace();
			}
		} else if (getOS().equals("Mac")) {

			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException e) {

				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {

				e.printStackTrace();
			} catch (UnsupportedLookAndFeelException e) {
				e.printStackTrace();
			}
		}
		/* If Linux is the OS! */
		else {
			String lookAndFeel = Messages.getString("Swing.2");
			int lookSelection = Integer.parseInt(lookAndFeel);
			if (lookSelection == 2) {

				try {
					UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel"); //$NON-NLS-1$
					// SwingUtilities.updateComponentTreeUI(this);
				} catch (Exception e) {
					System.out.println(e);
				}

			} else {

				MetalTheme theme = new Bio7LinuxTheme();

				MetalLookAndFeel.setCurrentTheme(theme);

				try {
					UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel"); //$NON-NLS-1$
					// SwingUtilities.updateComponentTreeUI(this);
				} catch (Exception e) {
					System.out.println(e);
				}
			}
		}

		/* Create the instance of the discrete grid! */
		if (Quad2d.getQuad2dInstance() == null) {
			new Quad2d();
		}

		/* Start the calculation thread of the Bio7 application! */
		CalculationThread m = new CalculationThread();
		m.start();
		/*
		 * Important to set, see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=422258
		 */
		javafx.application.Platform.setImplicitExit(false);

		dragDropR();

	}

	private void startupScripts() {
		String startupDirectory = null;
		String startupCommands = null;
		IPreferenceStore store = null;

		try {
			store = Bio7Plugin.getDefault().getPreferenceStore();
		} catch (RuntimeException e) {

			e.printStackTrace();
		}
		if (store != null) {
			startupDirectory = store.getString(PreferenceConstants.D_STRING);
			startupCommands = store.getString("BIO7_STARTUP_COMMANDS");
		}

		if (startupDirectory != null && startupDirectory != "") {

			File[] files = new Util().ListFilesDirectory(new File(startupDirectory),
					new String[] { ".java", ".r", ".R", ".bsh", ".groovy", ".py", ".js" });
			// System.out.println(files.length);
			if (files.length > 0) {
				for (int i = 0; i < files.length; i++) {
					// System.out.println(files[i].getName());
					String fileName = files[i].getName();

					int lastIndexOf = fileName.lastIndexOf(".");
					if (lastIndexOf > 0 == false) {
						return;
					}

					if (fileName.endsWith(".R") || fileName.endsWith(".r")) {

						RServeUtil.evalR(null, files[i].toString());
					}

					else if (fileName.endsWith(".bsh")) {

						BeanShellInterpreter.interpretJob(null, files[i].toString());

					} else if (fileName.endsWith(".groovy")) {

						GroovyInterpreter.interpretJob(null, files[i].toString());

					} else if (fileName.endsWith(".py")) {

						PythonInterpreter.interpretJob(null, files[i].toString());

					} else if (fileName.endsWith(".js")) {

						JavaScriptInterpreter.interpretJob(null, files[i].toString());

					}

					else if (fileName.endsWith(".java")) {

						final int count = i;

						Job job = new Job("Compile Java") {
							@Override
							protected IStatus run(IProgressMonitor monitor) {
								monitor.beginTask("Compile Java...", IProgressMonitor.UNKNOWN);
								String name = files[count].getName().replaceFirst("[.][^.]+$", "");
								// IWorkspace workspace =
								// ResourcesPlugin.getWorkspace();
								IPath location = Path.fromOSString(files[count].getAbsolutePath());

								// IFile ifile =
								// workspace.getRoot().getFileForLocation(location);
								CompileClassAndMultipleClasses cp = new CompileClassAndMultipleClasses();
								try {
									cp.compileAndLoad(new File(location.toOSString()),
											new File(location.toOSString()).getParent(), name, null, true);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									// Bio7Dialog.message(e.getMessage());
								}

								monitor.done();
								return Status.OK_STATUS;
							}

						};
						job.addJobChangeListener(new JobChangeAdapter() {
							public void done(IJobChangeEvent event) {
								if (event.getResult().isOK()) {

								} else {

								}
							}
						});
						// job.setSystem(true);
						job.schedule();

					}

				}

			}
		}
		/* Evaluate a startup command from the preferences dialog in Groovy! */
		if (startupCommands != null && startupCommands.isEmpty() == false) {
			GroovyInterpreter.interpretJob(startupCommands, null);
		}

	}

	public File[] ListFileDirectory(File filedirectory) {
		File dir = filedirectory;

		String[] children = dir.list();
		if (children == null) {

		} else {
			for (int i = 0; i < children.length; i++) {

				String filename = children[i];
			}
		}

		// Filter the extension with the specified string.
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.endsWith(".groovy"));
			}
		};

		File[] files = dir.listFiles(filter);

		return files;
	}

	public void postWindowOpen() {

		super.postWindowOpen();

		/*
		 * We use a black style if the CSS is the dark theme or the darkest dark theme!
		 */
		if (Util.isThemeBlack()) {

			Bundle bundle = Platform.getBundle("com.eco.bio7.themes");
			URL fileURL = bundle.getEntry("javafx/ModenaBlack.css");

			String path = fileURL.toExternalForm();

			// System.out.println(path);
			// javafx.application.Application.setUserAgentStylesheet(null);

			javafx.application.Application.setUserAgentStylesheet(path);

			themeBlack = true;
			OpenHelpBrowserAction.isThemeBlack = true;

		} else {

			Bundle bundle = Platform.getBundle("com.eco.bio7.themes");
			URL fileURL = bundle.getEntry("javafx/Bio7Default.css");

			String path = fileURL.toExternalForm();

			// System.out.println(path);
			// javafx.application.Application.setUserAgentStylesheet(null);

			javafx.application.Application.setUserAgentStylesheet(path);

			themeBlack = false;
			/* Set the R editor help browser in code completion popup! */
			OpenHelpBrowserAction.isThemeBlack = false;

		}

		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		try {

			configurer.getWorkbenchConfigurer().getWorkbench().showPerspective("com.eco.bio7.perspective_image",
					configurer.getWindow());

			configurer.getWorkbenchConfigurer().getWorkbench().showPerspective("com.eco.bio7.rbridge.RPerspective",
					configurer.getWindow());

			configurer.getWorkbenchConfigurer().getWorkbench()
					.showPerspective("com.eco.bio7.browser.SceneBuilderPerspective", configurer.getWindow());

			configurer.getWorkbenchConfigurer().getWorkbench()
					.showPerspective("com.eco.bio7.document.DocumentPerspective", configurer.getWindow());

			configurer.getWorkbenchConfigurer().getWorkbench().showPerspective(
					"com.eco.bio7.ijmacro.editor.perspectives.ImageJEditPerspective", configurer.getWindow());

			configurer.getWorkbenchConfigurer().getWorkbench().showPerspective("com.eco.bio7.bio7resource",
					configurer.getWindow());

			// *************************************
			new StartBio7Utils();
			// Start console and output!!
			StartBio7Utils.getConsoleInstance().startutils();
			// *************************************************
			/* Select the R perspective after all perspectives have been set! */
			// IWorkbenchWindow window = getViewSite().getWorkbenchWindow();
			IPerspectiveRegistry registry = configurer.getWorkbenchConfigurer().getWorkbench().getPerspectiveRegistry();
			IWorkbenchPage page = configurer.getWindow().getActivePage();
			page.setPerspective(registry.findPerspectiveWithId("com.eco.bio7.rbridge.RPerspective"));
			/*
			 * If Bio7 should be customized at startup the startup scripts have to be
			 * enabled! The startup is faster without!
			 */
			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			if (store.getBoolean("STARTUP_SCRIPTS")) {
				startupScripts();
			}

			/*
			 * if (store.getBoolean("RSERVE_AUTOSTART")) { Bio7Action.callRserve(); }
			 */

		} catch (WorkbenchException e) {
			e.printStackTrace();
		}

		addExecutionListener();
		/*
		 * Start Bio7 maximized. Works for Windows and Mac. However will cause coolbar
		 * repaint problems on Linux!
		 */
		if (getOS().equals("Windows") || getOS().equals("Mac")) {
			configurer.getWindow().getShell().setMaximized(true);
		}
		/* Turn off all log4j loggers! */
		List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
		loggers.add(LogManager.getRootLogger());
		for (Logger logger : loggers) {
			logger.setLevel(Level.OFF);
		}

	}

	/* The listener for save events of the Java editor! */
	public void addExecutionListener() {
		executionListener = new IExecutionListener() {
			public void notHandled(String commandId, NotHandledException exception) {

			}

			public void postExecuteFailure(String commandId, ExecutionException exception) {

			}

			public void postExecuteSuccess(String commandId, Object returnValue) {
				if (commandId.equals("org.eclipse.ui.file.save")) {

				}

			}

			public void preExecute(String commandId, ExecutionEvent event) {

			}
		};
		getCommandService().addExecutionListener(executionListener);

	}

	private ICommandService getCommandService() {
		return (ICommandService) PlatformUI.getWorkbench().getAdapter(ICommandService.class);
	}

	private void dragDropR() {
		Shell sh = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		DropTarget dt = new DropTarget(sh, DND.DROP_DEFAULT | DND.DROP_MOVE);
		dt.setTransfer(new Transfer[] { FileTransfer.getInstance() });
		dt.addDropListener(new DropTargetAdapter() {

			public void drop(DropTargetEvent event) {

				FileTransfer ft = FileTransfer.getInstance();
				if (ft.isSupportedType(event.currentDataType)) {
					String[] fileListR = (String[]) event.data;
					if (fileListR[0].endsWith("RData")) {
						IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
						boolean rPipe = store.getBoolean("r_pipe");

						if (rPipe == true) {
							String fileR;
							System.out.println(fileListR[0]);
							if (Bio7Dialog.getOS().equals("Windows")) {
								fileR = fileListR[0].replace("\\", "\\\\");
							} else {
								fileR = fileListR[0];
							}

							String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
							if (selectionConsole.equals("R")) {

								ConsolePageParticipant.pipeInputToConsole("load(file =\"" + fileR + "\")", true, true);
							} else {
								Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
							}

						}

						else {

							if (RServe.isAlive() == false) {

								StartRServe.setFileList(fileListR);
								StartRServe.setFromDragDrop(true);
								/*
								 * Now we can start the server. Variable setFromDragDrop will be set to false in
								 * the StartRserve class after job has finished!
								 */
								Bio7Action.callRserve();

							} else {
								RConnection con = RServe.getConnection();
								loadFile(fileListR, con);

							}

						}
					}
					/*
					 * Load an xml file for the discrete grid and quick compilation!
					 */
					else if (fileListR[0].endsWith("exml")) {
						LoadData.load(fileListR[0].toString());
					}

					else {
						MessageBox messageBox = new MessageBox(new Shell(),

								SWT.ICON_WARNING);
						messageBox.setMessage("File is not drag and \ndrop supported file for Bio7!");
						messageBox.open();

					}
				}
			}

		});

	}

	private void loadFile(String[] fileList, RConnection con) {
		String file;

		if (Bio7Dialog.getOS().equals("Windows")) {
			file = fileList[0].replace("\\", "\\\\");
		} else {
			file = fileList[0];
		}

		String load = "try(load(file =.bio7TempRScriptFile))";
		if (RState.isBusy() == false) {
			RState.setBusy(true);
			try {
				con.assign(".bio7TempRScriptFile", file);
			} catch (RserveException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			RInterpreterJob Do = new RInterpreterJob(load, null);
			Do.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {
						/*
						 * Reload the configuration for R to reload local temp path and display
						 * definition!
						 */
						RConfig.config(con);
						RState.setBusy(false);
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

	public void openIntro() {

	}

	private void declareWorkbenchImages() {

		final String ICONS_PATH = "$nl$/icons/full/";
		final String BIO7_PATH = "/icons/workbench/";
		final String BIO7_TOOLBAR_PATH = "/icons/maintoolbar/";
		final String PATH_ELOCALTOOL = ICONS_PATH + "elcl16/";
		// Enabled toolbar icons.
		final String PATH_ETOOL = ICONS_PATH + "etool16/"; // Enabled
		// toolbar icons.
		final String PATH_DTOOL = ICONS_PATH + "dtool16/"; // Disabled
		// toolbar icons.
		final String PATH_OBJECT = ICONS_PATH + "obj16/"; // Model
		// object icons
		final String PATH_WIZBAN = ICONS_PATH + "wizban/"; // Wizard
		final String PATH_EVIEW = ICONS_PATH + "eview16/"; // View icons
		// final String PATH_DLOCALTOOL = ICONS_PATH + "dlcl16/"; // Disabled

		Bundle ideBundle = Platform.getBundle(IDEWorkbenchPlugin.IDE_WORKBENCH);

		// Bio7 custom icons!

		Bundle bio7 = Platform.getBundle("com.eco.bio7");

		/*
		 * ISharedImages images = JavaUI.getSharedImages(); Image image =
		 * images.getImage(ISharedImages.);
		 */

		/* Image for the projects! */
		declareWorkbenchImage(bio7, IDE.SharedImages.IMG_OBJ_PROJECT, BIO7_PATH + "folderopened.png", false);

		declareWorkbenchImage(bio7, IDE.SharedImages.IMG_OBJ_PROJECT_CLOSED, BIO7_PATH + "folderclosed.png", false);

		declareWorkbenchImage(bio7, IDEInternalWorkbenchImages.IMG_WIZBAN_NEWFOLDER_WIZ,
				BIO7_PATH + "resourcepersp.png", false);
		/* Image for the printer! */
		declareWorkbenchImage(bio7, org.eclipse.ui.ISharedImages.IMG_ETOOL_PRINT_EDIT_DISABLED,
				BIO7_TOOLBAR_PATH + "print_file.png", false);
		declareWorkbenchImage(bio7, org.eclipse.ui.ISharedImages.IMG_ETOOL_PRINT_EDIT,
				BIO7_TOOLBAR_PATH + "print_file.png", false);
		/* Image for the folders! */
		declareWorkbenchImage(bio7, org.eclipse.ui.ISharedImages.IMG_OBJ_FOLDER, BIO7_PATH + "folderopened.png", false);
		/* Get all JDT images! */
		// JavaPluginImages.get(JavaPluginImages.IMG_OBJS_QUICK_ASSIST);

		// declareWorkbenchImage(ideBundle,
		// IDE.SharedImages.IMG_OBJ_PROJECT_CLOSED, PATH_OBJECT +
		// "cprj_obj.gif", true);

		/* Here we set a Bio7 fieldassist image! */
		FieldDecorationRegistry.getDefault().registerFieldDecoration("DEC_ERROR",
				JFaceResources.getString("FieldDecorationRegistry.errorMessage"),
				Bio7Plugin.getImageDescriptor("/icons/workbench/error_ovr.png").createImage());

		declareWorkbenchImage(ideBundle, IDE.SharedImages.IMG_OPEN_MARKER, PATH_ELOCALTOOL + "gotoobj_tsk.png", true);

		declareWorkbenchImage(ideBundle, IDE.SharedImages.IMG_OBJS_TASK_TSK, PATH_OBJECT + "taskmrk_tsk.png", true);
		declareWorkbenchImage(ideBundle, IDE.SharedImages.IMG_OBJS_BKMRK_TSK, PATH_OBJECT + "bkmrk_tsk.png", true);

		String string = IDEInternalWorkbenchImages.IMG_OBJS_COMPLETE_TSK;
		declareWorkbenchImage(ideBundle, string, PATH_OBJECT + "complete_tsk.png", true);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_INCOMPLETE_TSK,
				PATH_OBJECT + "incomplete_tsk.png", true);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_WELCOME_ITEM,
				PATH_OBJECT + "welcome_item.png", true);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_WELCOME_BANNER,
				PATH_OBJECT + "welcome_banner.png", true);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_BUILD_EXEC, PATH_ETOOL + "build_exec.png",
				false);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_BUILD_EXEC_HOVER,
				PATH_ETOOL + "build_exec.png", false);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_BUILD_EXEC_DISABLED,
				PATH_DTOOL + "build_exec.png", false);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_SEARCH_SRC, PATH_ETOOL + "search_src.png",
				false);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_SEARCH_SRC_HOVER,
				PATH_ETOOL + "search_src.png", false);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_SEARCH_SRC_DISABLED,
				PATH_DTOOL + "search_src.png", false);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_NEXT_NAV, PATH_ETOOL + "next_nav.png",
				false);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_PREVIOUS_NAV, PATH_ETOOL + "prev_nav.png",
				false);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_NEWPRJ_WIZ,
				PATH_WIZBAN + "newprj_wiz.png", false);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_NEWFILE_WIZ,
				PATH_WIZBAN + "newfile_wiz.png", false);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_IMPORTDIR_WIZ,
				PATH_WIZBAN + "importdir_wiz.png", false);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_IMPORTZIP_WIZ,
				PATH_WIZBAN + "importzip_wiz.png", false);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_EXPORTDIR_WIZ,
				PATH_WIZBAN + "exportdir_wiz.png", false);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_EXPORTZIP_WIZ,
				PATH_WIZBAN + "exportzip_wiz.png", false);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_RESOURCEWORKINGSET_WIZ,
				PATH_WIZBAN + "workset_wiz.png", false);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_DLGBAN_SAVEAS_DLG,
				PATH_WIZBAN + "saveas_wiz.png", false);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_DLGBAN_QUICKFIX_DLG,
				PATH_WIZBAN + "quick_fix.png", false);

		declareWorkbenchImage(ideBundle, IDE.SharedImages.IMG_OPEN_MARKER, PATH_ELOCALTOOL + "gotoobj_tsk.png", true);

		declareWorkbenchImage(ideBundle, IDE.SharedImages.IMG_OBJS_TASK_TSK, PATH_OBJECT + "taskmrk_tsk.png", true);
		declareWorkbenchImage(ideBundle, IDE.SharedImages.IMG_OBJS_BKMRK_TSK, PATH_OBJECT + "bkmrk_tsk.png", true);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_COMPLETE_TSK,
				PATH_OBJECT + "complete_tsk.png", true);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_INCOMPLETE_TSK,
				PATH_OBJECT + "incomplete_tsk.png", true);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_WELCOME_ITEM,
				PATH_OBJECT + "welcome_item.png", true);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_WELCOME_BANNER,
				PATH_OBJECT + "welcome_banner.png", true);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_ERROR_PATH, PATH_OBJECT + "error_tsk.png",
				true);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_WARNING_PATH, PATH_OBJECT + "warn_tsk.png",
				true);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_INFO_PATH, PATH_OBJECT + "info_tsk.png",
				true);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_LCL_FLAT_LAYOUT,
				PATH_ELOCALTOOL + "flatLayout.png", true);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_LCL_HIERARCHICAL_LAYOUT,
				PATH_ELOCALTOOL + "hierarchicalLayout.png", true);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEM_CATEGORY,
				PATH_ETOOL + "problem_category.png", true);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW,
				PATH_EVIEW + "problems_view.png", true);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW_ERROR,
				PATH_EVIEW + "problems_view_error.png", true);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_ETOOL_PROBLEMS_VIEW_WARNING,
				PATH_EVIEW + "problems_view_warning.png", true);

	}

	private void declareWorkbenchImage(Bundle ideBundle, String symbolicName, String path, boolean shared) {
		URL url = Platform.find(ideBundle, new Path(path));
		ImageDescriptor desc = ImageDescriptor.createFromURL(url);
		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		configurer.getWorkbenchConfigurer().declareImage(symbolicName, desc, shared);
	}

	public static String getOS() {
		return OS;
	}

	/* From: http://www.rgagnon.com/javadetails/java-0565.html */
	public static boolean is64BitVM() {
		String bits = System.getProperty("sun.arch.data.model", "?");
		if (bits.equals("64")) {
			return true;
		}
		if (bits.equals("?")) {
			// probably sun.arch.data.model isn't available
			// maybe not a Sun JVM?
			// try with the vm.name property
			return System.getProperty("java.vm.name").toLowerCase().indexOf("64") >= 0;
		}
		// probably 32bit
		return false;
	}

	public void initialize(IWorkbenchConfigurer configurer) {
		configurer.setSaveAndRestore(false);
		declareWorkbenchImages();
	}

	public static boolean isThemeBlack() {
		return themeBlack;
	}

}
