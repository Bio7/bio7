/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
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

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.activities.IActivityManager;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.EditorAreaDropAdapter;
import org.eclipse.ui.internal.ide.IDEInternalWorkbenchImages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.model.WorkbenchAdapterBuilder;
import org.eclipse.ui.part.EditorInputTransfer;
import org.eclipse.ui.part.MarkerTransfer;
import org.eclipse.ui.part.ResourceTransfer;
import org.osgi.framework.Bundle;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.actions.StartRServe;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.GroovyInterpreter;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.editor.BeanshellEditorPlugin;
import com.eco.bio7.javaeditor.Bio7EditorPlugin;
import com.eco.bio7.javaeditors.JavaEditor;
import com.eco.bio7.jobs.LoadData;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.preferences.Reg;
import com.eco.bio7.pythonedit.PythonEditorPlugin;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.debug.REditorListener;
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.scenebuilder.editor.MultiPageEditor;
import com.eco.bio7.time.CalculationThread;
import com.eco.bio7.preferences.RServePrefs;

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	private static String OS;
	// protected String[] fileList;
	private boolean x11ErrorHandlerFixInstalled = false;
	private IExecutionListener executionListener;
	private IPartListener2 partListener;

	public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
		super(configurer);

		WorkbenchAdapterBuilder.registerAdapters();
		declareWorkbenchImages();
	}

	public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
		return new ApplicationActionBarAdvisor(configurer);
	}

	public void preWindowOpen() {

		IWorkbenchActivitySupport workbenchActivitySupport = PlatformUI.getWorkbench().getActivitySupport();
		IActivityManager activityManager = workbenchActivitySupport.getActivityManager();

		String osname = System.getProperty("os.name");
		if (osname.startsWith("Windows")) {
			OS = "Windows";
		} else if (osname.equals("Linux")) {
			OS = "Linux";
		} else if (osname.startsWith("Mac")) {
			OS = "Mac";
		}

		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		// configurer.setSaveAndRestore( false );
		configurer.setInitialSize(new Point(1024, 768));
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
		 * preferenceManager.remove("org.eclipse.help.ui.browsersPreferencePage") ;preferenceManager.remove( "org.eclipse.update.internal.ui.preferences.MainPreferencePage");
		 */

		// Listen to changed perspective !!!!
		configurer.getWindow().addPerspectiveListener(new PerspectiveAdapter() {
			public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {

				if (perspective.getId().equals("com.eco.bio7.perspective_image")) {

				}

			}

			public void perspectiveSavedAs(IWorkbenchPage page, IPerspectiveDescriptor oldPerspective, IPerspectiveDescriptor newPerspective) {

			}

			public void perspectiveDeactivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
				if (perspective.getId().equals("com.eco.bio7.perspective_image")) {

				}
			}
		});

		/* Listen to the R editor if debugging actions should be added to the console toolbar! */
		
		configurer.getWindow().getPartService().addPartListener(new REditorListener().listen());

		PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_SYSTEM_JOBS, false);
		PlatformUI.getPreferenceStore().setDefault(IWorkbenchPreferenceConstants.DOCK_PERSPECTIVE_BAR, IWorkbenchPreferenceConstants.TOP_LEFT);
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
		File fileImportScripts = new File(path + "/importscripts");
		File fileExportScripts = new File(path + "/export_scripts");
		File fileGeneralScripts = new File(path + "/scripts");
		File fileSpatialScripts = new File(path + "/spatial_scripts");
		File fileImageScripts = new File(path + "/image_scripts");
		File fileRScripts = new File(path + "/r_scripts");
		File fileRShellScripts = new File(path + "/r_shell_scripts");
		File fileGridScripts = new File(path + "/grid_scripts");

		/* Folder for the temp *.RData file! */
		File fileTempR = new File(fileUrl.getPath());
		String pathTempR = fileTempR.toString();

		String reg1 = "";
		String reg2 = "";

		if (getOS().equals("Windows")) {
			reg1 = Reg.setPrefReg(PreferenceConstants.PATH_R);
			reg2 = Reg.setPrefReg(PreferenceConstants.PATH_LIBREOFFICE);
			if (reg1 != null) {
				store.setDefault(PreferenceConstants.PATH_R, reg1);
				/* Default install location for the packages! */
				store.setDefault("InstallLocation", reg1 + "\\site-library");
				store.setDefault("SweaveScriptLocation", reg1 + "/share/texmf/tex/latex");
				store.setDefault("pdfLatex", "C:/");
				store.setDefault("RSERVE_ARGS", "");

				if (is64BitVM()) {
					store.setDefault("r_pipe_path", reg1 + "\\bin\\x64");
				} else {
					store.setDefault("r_pipe_path", reg1 + "\\bin\\i386");
				}

			}
			if (reg2 != null) {
				store.setDefault(PreferenceConstants.PATH_LIBREOFFICE, reg2);
			} else {
				/*
				 * If the path cannot be found in the reg. it will be set to C:\ -> see com.eco.bio7.preferences.Reg.java!
				 */
				store.setDefault(PreferenceConstants.PATH_LIBREOFFICE, "C:\\");
			}
		} else if (getOS().equals("Linux")) {
			store.setDefault(PreferenceConstants.PATH_LIBREOFFICE, reg2);
			reg1 = "/usr/lib/R";
			reg2 = "/usr/lib/libreoffice/program";
			store.setDefault(PreferenceConstants.PATH_R, reg1);
			store.setDefault(PreferenceConstants.PATH_LIBREOFFICE, reg2);
			store.setDefault("InstallLocation", "/usr/lib/R/site-library");
			store.setDefault("SweaveScriptLocation", "/usr/share/R/share/texmf/tex/latex");
			store.setDefault("pdfLatex", "/usr/bin");
			store.setDefault("RSERVE_ARGS", "");

			if (is64BitVM()) {
				store.setDefault("r_pipe_path", reg1 + "/bin");
			} else {
				store.setDefault("r_pipe_path", reg1 + "/bin");
			}

		} else if (getOS().equals("Mac")) {
			reg2 = "/usr/lib/openoffice/program";
			store.setDefault(PreferenceConstants.PATH_LIBREOFFICE, reg2);

		}

		store.setDefault("datatablesize", 100);
		store.setDefault(PreferenceConstants.D_STRING, fileStartupScripts.getAbsolutePath());
		store.setDefault(PreferenceConstants.D_IMPORT, fileImportScripts.getAbsolutePath());
		store.setDefault(PreferenceConstants.D_EXPORT, fileExportScripts.getAbsolutePath());
		store.setDefault(PreferenceConstants.D_SCRIPT_GENERAL, fileGeneralScripts.getAbsolutePath());
		store.setDefault(PreferenceConstants.D_SCRIPT_SPATIAL, fileSpatialScripts.getAbsolutePath());
		store.setDefault(PreferenceConstants.D_SCRIPT_IMAGE, fileImageScripts.getAbsolutePath());
		store.setDefault(PreferenceConstants.D_SCRIPT_R, fileRScripts.getAbsolutePath());
		store.setDefault(PreferenceConstants.D_RSHELL_SCRIPTS, fileRShellScripts.getAbsolutePath());
		store.setDefault(PreferenceConstants.D_GRID_SCRIPTS, fileGridScripts.getAbsolutePath());

		if (getOS().equals("Windows")) {
			String pathTempR2 = pathTempR + "\\bio7temp\\";
			// String pathTempR3 = pathTempR2.replace("\\", "\\\\");
			store.setDefault(PreferenceConstants.P_TEMP_R, pathTempR2);
			store.setDefault("Console_Encoding", "CP850");
			store.setDefault("DEVICE_DEFINITION", "bio7Device <- function(filename = \"" + pathTempR2 + "tempDevicePlot%05d.tiff" + "\") { tiff(filename,width = 480, height = 480, units = \"px\")}; options(device=\"bio7Device\")");
			store.setDefault("DEVICE_FILENAME", "");
			store.setDefault("PLOT_DEVICE_SELECTION", "PLOT_IMAGE");
		} else if (getOS().equals("Linux")) {
			pathTempR = pathTempR + "/bio7temp/";
			store.setDefault(PreferenceConstants.P_TEMP_R, pathTempR);
			store.setDefault("Console_Encoding", "UTF-8");
			store.setDefault("shell_arguments", "");
			store.setDefault("DEVICE_DEFINITION", "bio7Device <- function(filename = \"" + pathTempR + "tempDevicePlot%05d.tiff" + "\") { tiff(filename,width = 480, height = 480, units = \"px\")}; options(device=\"bio7Device\")");
			store.setDefault("DEVICE_FILENAME", "");
			store.setDefault("PLOT_DEVICE_SELECTION", "PLOT_IMAGE");
		} else if (getOS().equals("Mac")) {
			pathTempR = pathTempR + "/bio7temp/";
			store.setDefault(PreferenceConstants.P_TEMP_R, pathTempR);
			store.setDefault("Console_Encoding", "UTF-8");
			store.setDefault("shell_arguments", "export TERM=xterm");
			store.setDefault("DEVICE_DEFINITION", "bio7Device <- function(filename = \"" + pathTempR + "tempDevicePlot%05d.tiff" + "\") { tiff(filename,width = 480, height = 480, type=\"cairo\")}; options(device=\"bio7Device\")");
			store.setDefault("DEVICE_FILENAME", "");
			store.setDefault("PLOT_DEVICE_SELECTION", "PLOT_IMAGE");
		}
		store.setDefault("RSERVE_AUTOSTART", false);
		store.setDefault(PreferenceConstants.PACKAGE_R_SERVER, "http://cran.r-project.org");
		if (getOS().equals("Linux")) {
			store.setDefault(PreferenceConstants.D_OPENOFFICE_HEAD, "Ã„, ,Ã¤,Ã–,Ã¶,Ãœ,Ã¼,+,!,Â§,$,%,&,/,(,),=,?,[,],Â°,^,;,:,>,<,|,*,Âµ,\\,@,\",â€œ,Â¸,`,~,#,},{,Â¹,Â²,Â³,_,-");
		} else if (getOS().equals("Mac")) {
			store.setDefault(PreferenceConstants.D_OPENOFFICE_HEAD, "Ã„, ,Ã¤,Ã–,Ã¶,Ãœ,Ã¼,+,!,Â§,$,%,&,/,(,),=,?,[,],Â°,^,;,:,>,<,|,*,Âµ,\\,@,\",â€œ,Â¸,`,~,#,},{,Â¹,Â²,Â³,_,-");
		} else {
			store.setDefault(PreferenceConstants.D_OPENOFFICE_HEAD, "Ä, ,ä,Ö,ö,Ü,ü,+,!,ü,§,$,%,&,/,(,),=,?,[,],°,^,;,:,>,<,|,*,µ,\\,”,@,\",“,”,´,`,~,#,},{,²,³,_,-");
		}

		store.setDefault("RSERVE_NATIVE_START", false);

		store.setDefault("LINUX_SHELL", "GNOME");
		store.setDefault("PDF_READER", "ACROBAT");

		store.setDefault("USE_CUSTOM_DEVICE", true);

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
		store.setDefault("STARTUP_SCRIPTS", false);
		store.setDefault("blender_options", "interactive");
		store.setDefault("before_script_blender", "import bpy;bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete()");
		store.setDefault("after_script_blender", "bpy.ops.render.render();bpy.ops.wm.redraw_timer(type='DRAW_WIN_SWAP', iterations=1)");

		/* A default FPS setting for the 3d view! */
		store.setDefault("fixedFps", 60);

		/*
		 * Default Colours for the Bio7 editors!
		 */
		String font = null;
		int fsize = 10;

		if (getOS().equals("Windows")) {
			PreferenceConverter.setDefault(store, "RShellFonts", new FontData("Courier New", 9, SWT.NONE));
			PreferenceConverter.setDefault(store, "Bio7ShellFonts", new FontData("Courier New", 10, SWT.NONE));
			font = "Courier New";
			fsize = 10;
		} else if (getOS().equals("Linux")) {
			PreferenceConverter.setDefault(store, "RShellFonts", new FontData("Courier New", 9, SWT.NONE));
			PreferenceConverter.setDefault(store, "Bio7ShellFonts", new FontData("Courier New", 10, SWT.NONE));
			font = "Courier New";
			fsize = 10;

		} else if (getOS().equals("Mac")) {
			PreferenceConverter.setDefault(store, "RShellFonts", new FontData("Monaco", 11, SWT.NONE));
			PreferenceConverter.setDefault(store, "Bio7ShellFonts", new FontData("Monaco", 11, SWT.NONE));
			font = "Monaco";
			fsize = 11;

		}

		/*IPreferenceStore storeBsh = BeanshellEditorPlugin.getDefault().getPreferenceStore();
		PreferenceConverter.setDefault(storeBsh, "colourkey", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(storeBsh, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(storeBsh, "colourkey2", new RGB(42, 0, 255));
		PreferenceConverter.setDefault(storeBsh, "colourkey3", new RGB(128, 128, 128));
		PreferenceConverter.setDefault(storeBsh, "colourkey4", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storeBsh, "colourkey5", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storeBsh, "colourkey6", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storeBsh, "colourkey7", new RGB(0, 0, 0));
		// PreferenceConverter.setDefault(storeBsh, "colourkey8", new RGB(50,
		// 150, 150));

		PreferenceConverter.setDefault(storeBsh, "colourkeyfont", new FontData(font, fsize, 1));
		PreferenceConverter.setDefault(storeBsh, "colourkeyfont1", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeBsh, "colourkeyfont2", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeBsh, "colourkeyfont3", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeBsh, "colourkeyfont4", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeBsh, "colourkeyfont5", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeBsh, "colourkeyfont6", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeBsh, "colourkeyfont7", new FontData(font, fsize, 0));*/
		// PreferenceConverter.setDefault(storeBsh, "colourkeyfont8", new
		// FontData("Courier New", 10, 0));

		/*IPreferenceStore storePython = PythonEditorPlugin.getDefault().getPreferenceStore();
		PreferenceConverter.setDefault(storePython, "colourkey", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(storePython, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(storePython, "colourkey2", new RGB(42, 0, 255));
		PreferenceConverter.setDefault(storePython, "colourkey3", new RGB(128, 128, 128));
		PreferenceConverter.setDefault(storePython, "colourkey4", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storePython, "colourkey5", new RGB(0, 0, 0));
		// PreferenceConverter.setDefault(storePython, "colourkey6", new RGB(0,
		// 0, 0));
		PreferenceConverter.setDefault(storePython, "colourkey7", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storePython, "colourkey8", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storePython, "colourkey9", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storePython, "colourkey10", new RGB(0, 0, 0));
		// PreferenceConverter.setDefault(storePython, "colourkey11", new RGB(0,
		// 0, 0));
		PreferenceConverter.setDefault(storePython, "colourkey12", new RGB(0, 0, 0));

		PreferenceConverter.setDefault(storePython, "colourkeyfont", new FontData(font, fsize, 1));
		PreferenceConverter.setDefault(storePython, "colourkeyfont1", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont2", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont3", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont4", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont5", new FontData(font, fsize, 0));
		// PreferenceConverter.setDefault(storePython, "colourkeyfont6", new
		// FontData("Courier New", 10, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont7", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont8", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont9", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont10", new FontData(font, fsize, 0));
		// PreferenceConverter.setDefault(storePython, "colourkeyfon11", new
		// FontData("Courier New", 10, 0));
		PreferenceConverter.setDefault(storePython, "colourkeyfont12", new FontData(font, fsize, 0));*/

		/*IPreferenceStore storeR = Bio7REditorPlugin.getDefault().getPreferenceStore();
		PreferenceConverter.setDefault(storeR, "colourkey", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(storeR, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(storeR, "colourkey2", new RGB(42, 0, 255));
		PreferenceConverter.setDefault(storeR, "colourkey3", new RGB(128, 128, 128));
		PreferenceConverter.setDefault(storeR, "colourkey4", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storeR, "colourkey5", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storeR, "colourkey6", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storeR, "colourkey7", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storeR, "colourkey8", new RGB(0, 0, 0));

		PreferenceConverter.setDefault(storeR, "colourkeyfont", new FontData(font, fsize, 1));
		PreferenceConverter.setDefault(storeR, "colourkeyfont1", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeR, "colourkeyfont2", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeR, "colourkeyfont3", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeR, "colourkeyfont4", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeR, "colourkeyfont5", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeR, "colourkeyfont6", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeR, "colourkeyfont7", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeR, "colourkeyfont8", new FontData(font, fsize, 0));*/

		IPreferenceStore storeJava = Bio7EditorPlugin.getDefault().getPreferenceStore();

		storeJava.setDefault("classbody", true);
		storeJava.setDefault("compiler_version", 1.7);
		storeJava.setDefault("compiler_debug", false);
		storeJava.setDefault("compiler_verbose", false);
		storeJava.setDefault("compiler_warnings", false);

		/*PreferenceConverter.setDefault(storeJava, "colourkey", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(storeJava, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(storeJava, "colourkey2", new RGB(42, 0, 255));
		PreferenceConverter.setDefault(storeJava, "colourkey3", new RGB(128, 128, 128));
		PreferenceConverter.setDefault(storeJava, "colourkey4", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storeJava, "colourkey5", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storeJava, "colourkey6", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(storeJava, "colourkey7", new RGB(0, 0, 0));
		// PreferenceConverter.setDefault(storeJava, "colourkey8", new RGB(0,
		// 150, 150));

		PreferenceConverter.setDefault(storeJava, "colourkeyfont", new FontData(font, fsize, 1));
		PreferenceConverter.setDefault(storeJava, "colourkeyfont1", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeJava, "colourkeyfont2", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeJava, "colourkeyfont3", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeJava, "colourkeyfont4", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeJava, "colourkeyfont5", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeJava, "colourkeyfont6", new FontData(font, fsize, 0));
		PreferenceConverter.setDefault(storeJava, "colourkeyfont7", new FontData(font, fsize, 0));*/
		// PreferenceConverter.setDefault(storeJava, "colourkeyfont8", new
		// FontData("Courier New", 10, 0));

		final String pathTo = store.getString(PreferenceConstants.P_TEMP_R);
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
									con.eval("options(device=\"bio7Device\")");
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

					RServePrefs prefsRserve = RServePrefs.getInstance();

					if (sel.equals("PLOT_IMAGE")) {

						prefsRserve.mult.setStringValue("bio7Device <- function(filename = \"" + pathTo + "tempDevicePlot%05d.tiff" + "\") { tiff(filename,width = 480, height = 480, units = \"px\")}; options(device=\"bio7Device\")");
						prefsRserve.deviceFilename.setStringValue("");
						prefsRserve.deviceFilename.setEnabled(false, prefsRserve.getFieldEditorParentControl());
					} else if (sel.equals("PLOT_CAIRO")) {

						prefsRserve.mult.setStringValue("bio7Device <- function(filename = \"" + pathTo + "tempDevicePlot%05d.tiff" + "\") { tiff(filename,width = 480, height = 480, type=\"cairo\")}; options(device=\"bio7Device\")");
						prefsRserve.deviceFilename.setStringValue("");
						prefsRserve.deviceFilename.setEnabled(false, prefsRserve.getFieldEditorParentControl());
					}

					else if (sel.equals("PLOT_PRINT")) {

						prefsRserve.mult.setStringValue("bio7Device <- function(filename = \"" + pathTo + "tempDevicePlot%05d.tiff" + "\") { tiff(filename,width = 6, height = 6, units=\"in\",res=600)}; options(device=\"bio7Device\")");
						prefsRserve.deviceFilename.setStringValue("");
						prefsRserve.deviceFilename.setEnabled(false, prefsRserve.getFieldEditorParentControl());
					} else if (sel.equals("PLOT_PDF")) {

						prefsRserve.mult.setStringValue("bio7Device <- function(filename = \"" + pathTo + "tempDevicePlot.pdf" + "\") { pdf(filename)}; options(device=\"bio7Device\")");
						prefsRserve.deviceFilename.setStringValue("tempDevicePlot.pdf");
						prefsRserve.deviceFilename.setEnabled(true, prefsRserve.getFieldEditorParentControl());
					}

					else if (sel.equals("PLOT_SVG")) {

						prefsRserve.mult.setStringValue("bio7Device <- function(filename = \"" + pathTo + "tempDevicePlot.svg" + "\") { svg(filename)}; options(device=\"bio7Device\")");
						prefsRserve.deviceFilename.setStringValue("tempDevicePlot.svg");
						prefsRserve.deviceFilename.setEnabled(true, prefsRserve.getFieldEditorParentControl());

					} else if (sel.equals("PLOT_POSTSCRIPT")) {

						prefsRserve.mult.setStringValue("bio7Device <- function(filename = \"" + pathTo + "tempDevicePlot.eps" + "\") { postscript(filename)}; options(device=\"bio7Device\")");
						prefsRserve.deviceFilename.setStringValue("tempDevicePlot.eps");
						prefsRserve.deviceFilename.setEnabled(true, prefsRserve.getFieldEditorParentControl());
					}

				}
			}

		});

		// Initialize important classes for Bio7!!!
		initBio7();

	}

	private void setComponentFont() {

		Display dis = Display.getDefault();

		assert EventQueue.isDispatchThread(); // On AWT event thread

		FontData fontData = dis.getSystemFont().getFontData()[0];

		int resolution = Toolkit.getDefaultToolkit().getScreenResolution();

		int awtFontSize = (int) Math.round((double) fontData.getHeight() * resolution / 72.0);
		java.awt.Font awtFont = null;
		/* Font size Linux! */
		if (getOS().equals("Linux")) {
			awtFont = new java.awt.Font(fontData.getName(), fontData.getStyle(), awtFontSize);
		}

		else {
			awtFont = new java.awt.Font(fontData.getName(), fontData.getStyle(), awtFontSize);
		}

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
		UIManager.put("PopupMenu.font", fontResource); //$NON-NLS-1

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

	}

	private void startupScripts() {
		String startupDirectory = null;
		IPreferenceStore store = null;

		try {
			store = Bio7Plugin.getDefault().getPreferenceStore();
		} catch (RuntimeException e) {

			e.printStackTrace();
		}
		if (store != null) {
			startupDirectory = store.getString(PreferenceConstants.D_STRING);
		}

		if (startupDirectory != null && startupDirectory != "") {
			File[] files = ListFileDirectory(new File(startupDirectory));
			if (files.length > 0) {
				for (int i = 0; i < files.length; i++) {

					GroovyInterpreter.interpretJob(null, files[i].getAbsolutePath());

				}

			}
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

		IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
		try {

			configurer.getWorkbenchConfigurer().getWorkbench().showPerspective("com.eco.bio7.perspective_image", configurer.getWindow());

			configurer.getWorkbenchConfigurer().getWorkbench().showPerspective("com.eco.bio7.rbridge.RPerspective", configurer.getWindow());

			configurer.getWorkbenchConfigurer().getWorkbench().showPerspective("com.eco.bio7.bio7resource", configurer.getWindow());
			// *************************************
			new StartBio7Utils();
			// Start console and output!!
			StartBio7Utils.getConsoleInstance().startutils();
			// *************************************************
			/*
			 * If Bio7 should be customized at startup the startup scripts have to be enabled! The startup is faster without!
			 */
			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			if (store.getBoolean("STARTUP_SCRIPTS")) {
				startupScripts();
			}

			if (store.getBoolean("RSERVE_AUTOSTART")) {
				Bio7Action.callRserve();
			}

		} catch (WorkbenchException e) {
			e.printStackTrace();
		}

		dragDropR();

		addExecutionListener();

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
		DropTarget dt = new DropTarget(sh.getShell(), DND.DROP_DEFAULT | DND.DROP_MOVE);
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
								 * Now we can start the server. Variable setFromDragDrop will be set to false in the StartRserve class after job has finished!
								 */
								Bio7Action.callRserve();

							} else {

								loadFile(fileListR);
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

	private void loadFile(String[] fileList) {
		String file;

		if (Bio7Dialog.getOS().equals("Windows")) {
			file = fileList[0].replace("\\", "\\\\");
		} else {
			file = fileList[0];
		}

		String load = "try(load(file =BIO7_TEMP))";
		if (RState.isBusy() == false) {
			RState.setBusy(true);
			try {
				RServe.getConnection().assign("BIO7_TEMP", file);
			} catch (RserveException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			RInterpreterJob Do = new RInterpreterJob(load, false, null);
			Do.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {
						RState.setBusy(false);
					} else {
						RState.setBusy(false);
					}
				}
			});
			Do.setUser(true);
			Do.schedule();
		} else {

			Bio7Dialog.message("RServer is busy!");

		}

	}

	public void openIntro() {

	}

	private void declareWorkbenchImages() {

		final String ICONS_PATH = "$nl$/icons/full/";
		final String BIO7_PATH = "/icons/workbench/";
		final String PATH_ELOCALTOOL = ICONS_PATH + "elcl16/";
		// Enabled toolbar icons.
		final String PATH_ETOOL = ICONS_PATH + "etool16/"; // Enabled
		// toolbar icons.
		final String PATH_DTOOL = ICONS_PATH + "dtool16/"; // Disabled
		// toolbar icons.
		final String PATH_OBJECT = ICONS_PATH + "obj16/"; // Model
		// object icons
		final String PATH_WIZBAN = ICONS_PATH + "wizban/"; // Wizard
		// icons

		Bundle ideBundle = Platform.getBundle(IDEWorkbenchPlugin.IDE_WORKBENCH);

		Bundle bio7 = Platform.getBundle("com.eco.bio7");
		/* Image for the projects! */
		declareWorkbenchImage(bio7, IDE.SharedImages.IMG_OBJ_PROJECT, BIO7_PATH + "prj_obj.gif", false);
		declareWorkbenchImage(bio7, IDE.SharedImages.IMG_OBJ_PROJECT_CLOSED, BIO7_PATH + "file.gif", false);

		declareWorkbenchImage(bio7, IDEInternalWorkbenchImages.IMG_WIZBAN_NEWFOLDER_WIZ, BIO7_PATH + "resource_persp.gif", false);
		/* Image for the printer! */
		declareWorkbenchImage(bio7, org.eclipse.ui.ISharedImages.IMG_ETOOL_PRINT_EDIT_DISABLED, BIO7_PATH + "print_edit.gif", false);
		declareWorkbenchImage(bio7, org.eclipse.ui.ISharedImages.IMG_ETOOL_PRINT_EDIT, BIO7_PATH + "print_edit.gif", false);
		/* Image for the folders! */
		declareWorkbenchImage(bio7, org.eclipse.ui.ISharedImages.IMG_OBJ_FOLDER, BIO7_PATH + "prj_obj.gif", false);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_NEWPRJ_WIZ, PATH_WIZBAN + "newprj_wiz.gif", false);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_NEWFOLDER_WIZ, BIO7_PATH + "ordner_zu.gif", false);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_NEWFILE_WIZ, PATH_WIZBAN + "newfile_wiz.gif", false);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_IMPORTDIR_WIZ, PATH_WIZBAN + "importdir_wiz.gif", false);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_IMPORTZIP_WIZ, PATH_WIZBAN + "importzip_wiz.gif", false);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_EXPORTDIR_WIZ, PATH_WIZBAN + "exportdir_wiz.gif", false);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_WIZBAN_EXPORTZIP_WIZ, PATH_WIZBAN + "exportzip_wiz.gif", false);

		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_DLGBAN_SAVEAS_DLG, PATH_WIZBAN + "saveas_wiz.gif", false);

		declareWorkbenchImage(ideBundle, IDE.SharedImages.IMG_OBJ_PROJECT_CLOSED, PATH_OBJECT + "cprj_obj.gif", true);
		declareWorkbenchImage(ideBundle, IDE.SharedImages.IMG_OPEN_MARKER, PATH_ELOCALTOOL + "gotoobj_tsk.gif", true);

		declareWorkbenchImage(ideBundle, IDE.SharedImages.IMG_OBJS_TASK_TSK, PATH_OBJECT + "taskmrk_tsk.gif", true);
		declareWorkbenchImage(ideBundle, IDE.SharedImages.IMG_OBJS_BKMRK_TSK, PATH_OBJECT + "bkmrk_tsk.gif", true);

		String string = IDEInternalWorkbenchImages.IMG_OBJS_COMPLETE_TSK;
		declareWorkbenchImage(ideBundle, string, PATH_OBJECT + "complete_tsk.gif", true);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_INCOMPLETE_TSK, PATH_OBJECT + "incomplete_tsk.gif", true);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_WELCOME_ITEM, PATH_OBJECT + "welcome_item.gif", true);
		declareWorkbenchImage(ideBundle, IDEInternalWorkbenchImages.IMG_OBJS_WELCOME_BANNER, PATH_OBJECT + "welcome_banner.gif", true);

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

}
