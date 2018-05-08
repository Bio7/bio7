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

import java.io.File;
import java.util.Stack;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.StatusLineContributionItem;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.registry.ActionSetRegistry;
import org.eclipse.ui.internal.registry.IActionSetDescriptor;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.views.IViewRegistry;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.actions.BeanShellImportAction;
import com.eco.bio7.actions.Compile;
import com.eco.bio7.actions.CounterReset;
import com.eco.bio7.actions.EnableSelection;
import com.eco.bio7.actions.ExecuteImageMacroAction;
import com.eco.bio7.actions.ExecuteScriptAction;
//import com.eco.bio7.actions.FastOpenScriptAction;
import com.eco.bio7.actions.FlowEditorAction;
import com.eco.bio7.actions.FlowEditorTestAction;
import com.eco.bio7.actions.FlowExternalStartAction;
import com.eco.bio7.actions.FlowStopAction;
import com.eco.bio7.actions.HideMainMenus;
import com.eco.bio7.actions.Interpret;
import com.eco.bio7.actions.InterpretPython;
import com.eco.bio7.actions.JavaScriptInterpret;
import com.eco.bio7.actions.LibreOfficeConnection;
import com.eco.bio7.actions.OfficeOpenAction;
import com.eco.bio7.actions.OfficeSendValueAction;
import com.eco.bio7.actions.OfficeValueAction;
import com.eco.bio7.actions.OpenBio7BrowserAction;
import com.eco.bio7.actions.OpenViewMenuAction;
import com.eco.bio7.actions.Random;
import com.eco.bio7.actions.ResetField;
import com.eco.bio7.actions.SetupDiscrete;
import com.eco.bio7.actions.ShowEditorAreaAction;
import com.eco.bio7.actions.ShowMainMenus;
import com.eco.bio7.actions.Start;
import com.eco.bio7.actions.Start3d;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.BeanShellInterpreter;
import com.eco.bio7.compile.CompileClassAndMultipleClasses;
import com.eco.bio7.compile.GroovyInterpreter;
import com.eco.bio7.compile.JavaScriptInterpreter;
import com.eco.bio7.compile.PythonInterpreter;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.documents.LatexSweaveKnitrAction;
import com.eco.bio7.documents.RMarkdownAction;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.actions.ClearRWorkspace;
import com.eco.bio7.rbridge.actions.ClipboardRScipt;
import com.eco.bio7.rbridge.actions.ClipboardRValues;
import com.eco.bio7.rbridge.actions.ExecuteRScriptAction;
import com.eco.bio7.rbridge.actions.ExecuteRTextSelection;
import com.eco.bio7.rbridge.actions.InterpretR;
import com.eco.bio7.rbridge.actions.LoadRWorkspace;
import com.eco.bio7.rbridge.actions.OfficeValueToRAction;
import com.eco.bio7.rbridge.actions.OfficeValueToRHeadAction;
import com.eco.bio7.rbridge.actions.OpenRservePreferencesAction;
import com.eco.bio7.rbridge.actions.PrintExpression;
import com.eco.bio7.rbridge.actions.SaveRWorkspace;
import com.eco.bio7.rbridge.actions.SaveRWorkspaceAndStart;
import com.eco.bio7.rbridge.actions.StartRServe;
import com.eco.bio7.scenebuilder.GenerateControllerAction;
import com.eco.bio7.time.Time;
import com.eco.bio7.util.Util;

public class ApplicationActionBarAdvisor extends ActionBarAdvisor {
	private static StatusLineContributionItem userItem = null;

	private String id[] = new String[] { "org.eclipse.ui.views.ResourceNavigator", "org.eclipse.ui.console.ConsoleView", "com.eco.bio7.RShell", "com.eco.bio7.rbridge.RTable", "com.eco.bio7.imagej",
			"com.eco.bio7.ijtoolbar", "com.eco.bio7.image_methods", "com.eco.bio7.points", "com.eco.bio7.image.TransferGeometryView", "com.eco.bio7.browser.Browser",
			"com.eco.bio7.worldwind.WorldWindView", "com.eco.bio7.worldwind.WorldWindOptionsView", "com.eco.bio7.spatial", "com.eco.bio7.discrete3d.Options3d", "com.eco.bio7.control",
			"com.eco.bio7.spreadsheet", "com.eco.bio7.states", "com.eco.bio7.quadgrid", "com.eco.bio7.hexgrid", "com.eco.bio7.linechart", "com.eco.bio7.piechart" };

	private IWorkbenchWindow window;

	private IWorkbenchAction exitAction;

	private IWorkbenchAction print;

	private IWorkbenchAction New;

	private IWorkbenchAction saveall;

	private IWorkbenchAction Import;

	private IWorkbenchAction Export;

	private IWorkbenchAction undo;

	private IWorkbenchAction redo;

	private IWorkbenchAction cut;

	private IWorkbenchAction copy;

	private IWorkbenchAction paste;

	private IWorkbenchAction delete;

	private IWorkbenchAction selectall;

	private IWorkbenchAction find;

	private IWorkbenchAction save;

	private IWorkbenchAction saveas;

	private IWorkbenchAction dynamichelp;

	private IWorkbenchAction aboutAction;

	private IWorkbenchAction newWindowAction;

	private IWorkbenchAction introAction;

	private static Action libreofficeconnection;

	private static Action start;

	private Action enableselection;

	private IWorkbenchAction propAction;

	private IWorkbenchAction helpAction;

	private static Action start3d;

	private Action resetfield;

	private static Action counterreset;

	private Action random;

	private static Action startrserve;

	private Action setup;

	private static Action flowstop;

	private IContributionItem viewShortlist;

	private IContributionItem perspect;

	private IWorkbenchWindow window2;

	private IPreferenceStore store;

	private OfficeOpenAction officeopenspread;

	private OfficeValueToRAction officevaluetor;

	private OfficeValueToRHeadAction officevaluetorhead;

	//private OfficeValueAction officevalue;

	private OfficeSendValueAction officepopdata;

	private static BeanShellImportAction bshimportaction;

	private IWorkbenchAction saveperspectiveas;

	private PrintExpression printexpression;

	private static FlowExternalStartAction flowexternalstartaction;

	private ExecuteRTextSelection rselect;

	private ClipboardRScipt clipboardRScript;

	private ClipboardRValues clipboardRValues;

	private OpenRservePreferencesAction openRservePref;

	private LoadRWorkspace loadR;

	private SaveRWorkspace saveR;

	private OpenBio7BrowserAction openBrowser;

	private MenuManager helpMenu;

	private InterpretR interpretR;

	private Compile compileJava;

	private FlowEditorAction flowAction;

	private FlowEditorTestAction debugFlowAction;

	private Interpret interpretGrovvyBeanShell;

	private InterpretPython interpretPython;

	private LatexSweaveKnitrAction knitrAction;

	private RMarkdownAction markdownAction;

	private GenerateControllerAction generateControllerAction;

	private ClearRWorkspace clearWorkspace;

	private IContributionItem showViewItem;

	// private IWorkbenchAction switchWorkspaceAction;

	private IWorkbenchAction refreshAction;

	private JavaScriptInterpret javaScriptInterpret;

	private ShowMainMenus showMainMenu;

	private HideMainMenus hideMainMenu;

	private IWorkbenchAction toggleCoolBar;

	protected Stack<MenuManager> menuStack;

	// private FastOpenScriptAction fastOpenScriptAction;

	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
		store = Bio7Plugin.getDefault().getPreferenceStore();
	}

	protected void makeActions(final IWorkbenchWindow window) {
		window2 = window;

		New = ActionFactory.NEW.create(window);
		New.setText("New");
		register(New);

		toggleCoolBar = ActionFactory.TOGGLE_COOLBAR.create(window);
		register(toggleCoolBar);

		showMainMenu = new ShowMainMenus("Show all menus", window2);
		register(showMainMenu);

		hideMainMenu = new HideMainMenus("Hide all Menus", window2);
		register(hideMainMenu);

		officeopenspread = new OfficeOpenAction("Open Spreadsheet", window2);

		officevaluetor = new OfficeValueToRAction("Data to R", window2);

		officevaluetorhead = new OfficeValueToRHeadAction("Data with head to R ", window2);

		//officevalue = new OfficeValueAction("Data to Beanshell", window2);

		officepopdata = new OfficeSendValueAction("Send Bio7 counts", window2);

		save = ActionFactory.SAVE.create(window);
		register(save);

		saveall = ActionFactory.SAVE_ALL.create(window);
		register(saveall);

		saveas = ActionFactory.SAVE_AS.create(window);
		register(saveas);

		saveperspectiveas = ActionFactory.SAVE_PERSPECTIVE.create(window);
		register(saveperspectiveas);

		Import = ActionFactory.IMPORT.create(window);

		register(Import);

		Export = ActionFactory.EXPORT.create(window);
		register(Export);

		helpAction = ActionFactory.HELP_CONTENTS.create(window);
		register(helpAction);

		/*
		 * findAction = ActionFactory.FIND.create(window); register(findAction);
		 */

		print = ActionFactory.PRINT.create(window);
		register(print);

		find = ActionFactory.FIND.create(window);
		register(find);

		cut = ActionFactory.CUT.create(window);
		register(cut);

		copy = ActionFactory.COPY.create(window);
		register(copy);

		delete = ActionFactory.DELETE.create(window);
		register(delete);

		paste = ActionFactory.PASTE.create(window);
		register(paste);

		selectall = ActionFactory.SELECT_ALL.create(window);
		register(selectall);

		undo = ActionFactory.UNDO.create(window);
		register(undo);

		redo = ActionFactory.REDO.create(window);
		register(redo);

		dynamichelp = ActionFactory.DYNAMIC_HELP.create(window);
		register(dynamichelp);

		introAction = ActionFactory.INTRO.create(window);
		register(introAction);

		propAction = ActionFactory.PREFERENCES.create(window);
		register(propAction);
		/* Avoid buggy action on MacOSX */
		if (!Util.getOS().equals("Mac")) {
			exitAction = ActionFactory.QUIT.create(window);
			register(exitAction);
		}

		aboutAction = ActionFactory.ABOUT.create(window);
		register(aboutAction);

		newWindowAction = ActionFactory.OPEN_NEW_WINDOW.create(window);
		register(newWindowAction);

		showViewItem = ContributionItemFactory.VIEWS_SHORTLIST.create(window);

		libreofficeconnection = new LibreOfficeConnection("Open LibreOffice Connection", window);
		register(libreofficeconnection);

		start = new Start("Start", window);
		register(start);

		start3d = new Start3d("Start/Stop Animation", window);
		register(start3d);

		resetfield = new ResetField("Reset Field", window);
		register(resetfield);

		counterreset = new CounterReset("Counter Reset", window);
		register(counterreset);

		enableselection = new EnableSelection("EnableSelection", window);
		register(enableselection);

		random = new Random("Random", window);
		register(random);

		startrserve = new StartRServe("Start Rserve", window);
		register(startrserve);

		printexpression = new PrintExpression("PrintExpression", window);
		register(printexpression);

		setup = new SetupDiscrete("Save Pattern", window);
		register(setup);

		viewShortlist = ContributionItemFactory.VIEWS_SHORTLIST.create(window);

		perspect = ContributionItemFactory.PERSPECTIVES_SHORTLIST.create(window);

		flowexternalstartaction = new FlowExternalStartAction("Flowexternalstart", window);
		register(flowexternalstartaction);

		flowstop = new FlowStopAction("Flowstop", window);
		register(flowstop);

		rselect = new ExecuteRTextSelection("Evaluate Selected Line", window);
		register(rselect);

		clipboardRScript = new ClipboardRScipt("Evaluate Clipboard", window);
		register(clipboardRScript);

		clipboardRValues = new ClipboardRValues("Get Clipboard Data", window);
		register(clipboardRValues);

		openRservePref = new OpenRservePreferencesAction();
		register(openRservePref);

		loadR = new LoadRWorkspace("Load Workspace", window);
		register(loadR);

		saveR = new SaveRWorkspace("Save Workspace", window);
		register(saveR);

		clearWorkspace = new ClearRWorkspace("Remove all objects", window);
		register(clearWorkspace);

		openBrowser = new OpenBio7BrowserAction("Bio7 Internet", window);
		register(openBrowser);

		interpretR = new InterpretR("Interpret R", window);
		register(interpretR);

		compileJava = new Compile("Compile Java");
		register(compileJava);

		flowAction = new FlowEditorAction("Execute Flow");
		register(flowAction);

		debugFlowAction = new FlowEditorTestAction("Debug Flow");
		register(debugFlowAction);

		interpretGrovvyBeanShell = new Interpret("Interpret Groovy/BeanShell");
		register(interpretGrovvyBeanShell);

		interpretPython = new InterpretPython("Interpret Python");
		register(interpretPython);

		javaScriptInterpret = new JavaScriptInterpret("Interpret JavaScript");
		register(javaScriptInterpret);

		knitrAction = new LatexSweaveKnitrAction();
		register(knitrAction);

		markdownAction = new RMarkdownAction();
		register(markdownAction);

		generateControllerAction = new GenerateControllerAction("Generate Controller Class");
		register(generateControllerAction);

		/*
		 * switchWorkspaceAction = IDEActionFactory.OPEN_WORKSPACE.create(window);
		 * register(switchWorkspaceAction);
		 */

		/*
		 * fastOpenScriptAction = new FastOpenScriptAction("Script");
		 * register(fastOpenScriptAction);
		 */

		refreshAction = ActionFactory.REFRESH.create(window);
		register(refreshAction);

	}

	protected void fillMenuBar(IMenuManager menuBar) {
		/*
		 * Remove actions tip from: http://random-eclipse-tips.blogspot.de/2009/02
		 * /eclipse-rcp-removing-unwanted_02.html
		 */
		// SHOW_JDT_GUI
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		boolean showJDTGui = store.getBoolean("SHOW_JDT_GUI");
		if (showJDTGui == false) {
			final ActionSetRegistry reg = WorkbenchPlugin.getDefault().getActionSetRegistry();
			final IActionSetDescriptor[] actionSets = reg.getActionSets();
			final String[] removeActionSets = new String[] { "org.eclipse.search.searchActionSet", "org.eclipse.ui.cheatsheets.actionSet", "org.eclipse.ui.actionSet.keyBindings",
					"org.eclipse.ui.edit.text.actionSet.navigation", "org.eclipse.ui.edit.text.actionSet.annotationNavigation", "org.eclipse.ui.edit.text.actionSet.convertLineDelimitersTo",
					"org.eclipse.ui.edit.text.actionSet.openExternalFile", "org.eclipse.ui.externaltools.ExternalToolsSet", "org.eclipse.ui.externaltools.ExternalToolsSet",
					"org.eclipse.ui.WorkingSetActionSet", "org.eclipse.update.ui.softwareUpdates", "org.eclipse.ui.actionSet.openFiles", "org.eclipse.mylyn.tasks.ui.navigation",
					"org.eclipse.debug.ui.launchActionSet", "org.eclipse.jdt.ui.JavaElementCreationActionSet", "org.eclipse.jdt.ui.JavaActionSet", "org.eclipse.ui.edit.text.actionSet.presentation",
					"org.eclipse.ui.cheatsheets.actionSet", "org.eclipse.ui.externaltools.ExternalToolsSet", "org.eclipse.jdt.ui.text.java.actionSet.presentation",
					"org.eclipse.debug.ui.breakpointActionSet", "org.eclipse.wb.core.ui.actionset" };

			for (int i = 0; i < actionSets.length; i++) {
				boolean found = false;
				for (int j = 0; j < removeActionSets.length; j++) {
					if (removeActionSets[j].equals(actionSets[i].getId())) {

						found = true;
					}
				}
				if (!found) {
					continue;
				}
				final IExtension ext = actionSets[i].getConfigurationElement().getDeclaringExtension();

				reg.removeExtension(ext, new Object[] { actionSets[i] });
			}
		}

		MenuManager fileMenu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);

		MenuManager editMenu = new MenuManager("&Edit", IWorkbenchActionConstants.M_EDIT);
		editMenu.add(new GroupMarker(IWorkbenchActionConstants.FIND_EXT)); // should
																			// resolve
																			// it

		MenuManager findMenu = new MenuManager("&Search", IWorkbenchActionConstants.FIND_EXT);
		findMenu.add(new GroupMarker(IWorkbenchActionConstants.FIND_EXT)); // should
																			// resolve
																			// it

		MenuManager prefMenu = new MenuManager("Preferences");
		/* Set not an references extra menu for MacOSX! */
		prefMenu.setVisible(!Util.getOS().equals("Mac"));
		MenuManager rMenu = new MenuManager("R");
		MenuManager OpenOfficeMenu = new MenuManager("LibreOffice");

		MenuManager WindowMenu = new MenuManager("&Window");
		helpMenu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);// p2
		MenuManager showViewMenuMgr = new MenuManager("Show View", "showView");

		MenuManager openPerspectiveMenu = new MenuManager("Open Perspective");
		MenuManager scriptMenu = new MenuManager("&Scripts");

		//
		/*
		 * MenuManager generalMenu = new MenuManager("&General-Scripts");
		 * generalMenu.add(new ExecuteScriptAction("Empty", window2, new File("")));
		 * generalMenu.setRemoveAllWhenShown(true); IMenuListener listener = new
		 * IMenuListener() { public void menuAboutToShow(IMenuManager m) { File files =
		 * new File(store.getString(PreferenceConstants.D_SCRIPT_GENERAL)); File[] fil =
		 * new Util().ListFilesDirectory(files, new String[] { ".java", ".bsh",
		 * ".groovy", ".py",".js" });
		 * 
		 * if (fil.length > 0) { int a; for (int i = 0; i < fil.length; i++) {
		 * 
		 * m.add(new ExecuteScriptAction(fil[i].getName().substring(0,
		 * fil[i].getName().lastIndexOf(".")), window2, fil[i])); a = i + 1; if (a % 5
		 * == 0) { m.add(new Separator()); }
		 * 
		 * }
		 * 
		 * }
		 * 
		 * else { m.add(new ExecuteScriptAction("Empty", window2, new File(""))); }
		 * 
		 * }
		 * 
		 * };
		 * 
		 * 
		 * generalMenu.addMenuListener(listener); MenuManager spatialMenu = new
		 * MenuManager("&Spatial-Scripts"); spatialMenu.add(new
		 * ExecuteScriptAction("Empty", window2, new File("")));
		 * spatialMenu.setRemoveAllWhenShown(true); IMenuListener listener4 = new
		 * IMenuListener() { public void menuAboutToShow(IMenuManager m) { File files =
		 * new File(store.getString(PreferenceConstants.D_SCRIPT_SPATIAL)); File[] fil =
		 * new Util().ListFilesDirectory(files, new String[] { ".java", ".bsh",
		 * ".groovy", ".py",".js" });
		 * 
		 * if (fil.length > 0) { int a; for (int i = 0; i < fil.length; i++) {
		 * 
		 * m.add(new ExecuteScriptAction(fil[i].getName().substring(0,
		 * fil[i].getName().lastIndexOf(".")), window2, fil[i])); a = i + 1; if (a % 5
		 * == 0) { m.add(new Separator()); }
		 * 
		 * }
		 * 
		 * }
		 * 
		 * else { m.add(new ExecuteScriptAction("Empty", window2, new File(""))); }
		 * 
		 * }
		 * 
		 * };
		 * 
		 * spatialMenu.addMenuListener(listener4);
		 * 
		 * MenuManager macrosImageJMenu = new MenuManager("&ImageJ-Macros");
		 * macrosImageJMenu.add(new ExecuteImageMacroAction("Empty", window2, new
		 * File(""))); macrosImageJMenu.setRemoveAllWhenShown(true); IMenuListener
		 * listener5 = new IMenuListener() { public void menuAboutToShow(IMenuManager m)
		 * { File files = new File(store.getString(PreferenceConstants.D_SCRIPT_IMAGE));
		 * File[] fil = new Util().ListFileDirectory(files, ".txt");
		 * 
		 * if (fil.length > 0) { int a; for (int i = 0; i < fil.length; i++) {
		 * 
		 * m.add(new ExecuteImageMacroAction(fil[i].getName().substring(0,
		 * fil[i].getName().lastIndexOf(".")), window2, fil[i])); a = i + 1; if (a % 5
		 * == 0) { m.add(new Separator()); } }
		 * 
		 * }
		 * 
		 * else { m.add(new ExecuteImageMacroAction("Empty", window2, new File(""))); }
		 * 
		 * }
		 * 
		 * };
		 * 
		 * macrosImageJMenu.addMenuListener(listener5);
		 * 
		 * MenuManager rScriptMenu = new MenuManager("&R-Scripts"); rScriptMenu.add(new
		 * ExecuteScriptAction("Empty", window2, new File("")));
		 * rScriptMenu.setRemoveAllWhenShown(true); IMenuListener listener6 = new
		 * IMenuListener() { public void menuAboutToShow(IMenuManager m) { File files =
		 * new File(store.getString(PreferenceConstants.D_SCRIPT_R)); File[] fil = new
		 * Util().ListFileDirectory(files, ".R");
		 * 
		 * if (fil.length > 0) { int a; for (int i = 0; i < fil.length; i++) {
		 * 
		 * m.add(new ExecuteRScriptAction(fil[i].getName().substring(0,
		 * fil[i].getName().lastIndexOf(".")), window2, fil[i])); a = i + 1; if (a % 5
		 * == 0) { m.add(new Separator()); }
		 * 
		 * }
		 * 
		 * }
		 * 
		 * else { m.add(new ExecuteScriptAction("Empty", window2, new File(""))); }
		 * 
		 * }
		 * 
		 * };
		 * 
		 * rScriptMenu.addMenuListener(listener6);
		 */

		MenuManager importScriptMenu = new MenuManager("&Import-Scripts");
		importScriptMenu.add(new ExecuteScriptAction("Empty", window2, new File("")));
		importScriptMenu.setRemoveAllWhenShown(true);
		IMenuListener listener2 = new IMenuListener() {
			public void menuAboutToShow(IMenuManager m) {
				File files = new File(store.getString(PreferenceConstants.D_IMPORT));
				File[] fil = new Util().ListFilesDirectory(files, new String[] { ".java", ".bsh", ".groovy", ".py", ".js" });

				if (fil.length > 0) {
					int a;
					for (int i = 0; i < fil.length; i++) {

						m.add(new ExecuteScriptAction(fil[i].getName().substring(0, fil[i].getName().lastIndexOf(".")), window2, fil[i]));
						a = i + 1;
						if (a % 5 == 0) {
							m.add(new Separator());
						}
					}

				}

				else {
					m.add(new ExecuteScriptAction("Empty", window2, new File("")));
				}

			}

		};

		importScriptMenu.addMenuListener(listener2);

		MenuManager exportScriptMenu = new MenuManager("&Export-Scripts");
		exportScriptMenu.add(new ExecuteScriptAction("Empty", window2, new File("")));
		exportScriptMenu.setRemoveAllWhenShown(true);
		IMenuListener listener3 = new IMenuListener() {
			public void menuAboutToShow(IMenuManager m) {
				File files = new File(store.getString(PreferenceConstants.D_EXPORT));
				File[] fil = new Util().ListFilesDirectory(files, new String[] { ".java", ".bsh", ".groovy", ".py", ".js" });

				if (fil.length > 0) {
					int a;
					for (int i = 0; i < fil.length; i++) {

						m.add(new ExecuteScriptAction(fil[i].getName().substring(0, fil[i].getName().lastIndexOf(".")), window2, fil[i]));
						a = i + 1;
						if (a % 5 == 0) {
							m.add(new Separator());
						}

					}

				}

				else {
					m.add(new ExecuteScriptAction("Empty", window2, new File("")));
				}
			}

		};

		exportScriptMenu.addMenuListener(listener3);

		/* Implement a custom show view action! */

		MenuManager viewMenu = new MenuManager("&Show Selected View");
		viewMenu.add(new ExecuteScriptAction("Empty", window2, new File("")));
		viewMenu.setRemoveAllWhenShown(true);
		IMenuListener listenerView = new IMenuListener() {
			public void menuAboutToShow(IMenuManager m) {

				IViewRegistry viewRegistry = PlatformUI.getWorkbench().getViewRegistry();
				IViewDescriptor[] views = viewRegistry.getViews();
				for (int i = 0; i < id.length; i++) {
					if (i == 2 || i == 4 || i == 9 || i == 10 || i == 12 || i == 14) {
						viewMenu.add(new Separator());
					}
					for (int j = 0; j < views.length; j++) {

						if (id[i].equals(views[j].getId()))
							viewMenu.add(new OpenViewMenuAction(views[j].getLabel(), views[j].getId()));

					}

				}

			}

		};

		viewMenu.addMenuListener(listenerView);

		// showViewMenu.add(re);

		/* Implement a custom show view action! */
		MenuManager viewMenuAll = new MenuManager("&Show All Views (Unordered)");
		viewMenuAll.add(new ExecuteScriptAction("Empty", window2, new File("")));
		viewMenuAll.setRemoveAllWhenShown(true);
		IMenuListener listenerViewAll = new IMenuListener() {
			public void menuAboutToShow(IMenuManager m) {
				IViewRegistry viewRegistry = PlatformUI.getWorkbench().getViewRegistry();
				IViewDescriptor[] views = viewRegistry.getViews();
				for (IViewDescriptor iViewDescriptor : views) {

					viewMenuAll.add(new OpenViewMenuAction(iViewDescriptor.getLabel(), iViewDescriptor.getId()));

				}
			}

		};

		viewMenuAll.addMenuListener(listenerViewAll);

		// showViewMenu.add(re);

		openPerspectiveMenu.add(perspect);
		prefMenu.add(propAction);

		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(findMenu);
		menuBar.add(prefMenu);
		menuBar.add(scriptMenu);
		menuBar.add(rMenu);
		menuBar.add(OpenOfficeMenu);
		// menuBar.add(BeanShellMenu);
		menuBar.add(WindowMenu);
		menuBar.add(helpMenu);

		// Add a group marker indicating where action set menus will appear.
		menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

		fileMenu.add(New);
		fileMenu.add(new Separator());
		// fileMenu.add(loadpattern);
		// fileMenu.add(savepattern);
		fileMenu.add(new Separator());
		fileMenu.add(importScriptMenu);
		fileMenu.add(exportScriptMenu);
		fileMenu.add(new Separator());
		fileMenu.add(new Separator());
		fileMenu.add(save);
		fileMenu.add(saveall);
		fileMenu.add(saveas);
		fileMenu.add(refreshAction);
		// fileMenu.add(switchWorkspaceAction);
		fileMenu.add(new Separator());
		fileMenu.add(Import);
		fileMenu.add(Export);
		fileMenu.add(new Separator());
		/* Avoid buggy action on MacOSX */
		if (!Util.getOS().equals("Mac")) {
			fileMenu.add(exitAction);
		}

		editMenu.add(undo);
		editMenu.add(redo);
		editMenu.add(new Separator());
		editMenu.add(cut);
		editMenu.add(copy);
		editMenu.add(paste);
		editMenu.add(new Separator());
		editMenu.add(delete);
		editMenu.add(new Separator());
		editMenu.add(find);
		editMenu.add(new Separator());
		editMenu.add(selectall);
		editMenu.add(new Separator());
		editMenu.add(print);

		/* Here we create the submenus with actions for the scripts recursively! */
		scriptMenu.setRemoveAllWhenShown(true);

		IMenuListener ScriptListener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager m) {

				menuStack = new Stack<MenuManager>();
				menuStack.push(scriptMenu);
				String loc = store.getString(PreferenceConstants.D_SCRIPT_GENERAL);
				createSubMenus(loc);

			}

		};

		scriptMenu.addMenuListener(ScriptListener);

		rMenu.add(startrserve);
		rMenu.add(new Separator());
		rMenu.add(loadR);
		rMenu.add(saveR);
		rMenu.add(new Separator());
		rMenu.add(new PrintExpression("Evaluate Expression", window2));
		rMenu.add(rselect);
		rMenu.add(clipboardRScript);
		rMenu.add(new Separator());
		rMenu.add(clipboardRValues);
		rMenu.add(new Separator());
		rMenu.add(clearWorkspace);
		rMenu.add(new Separator());
		rMenu.add(new Separator());
		if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
			rMenu.add(new SaveRWorkspaceAndStart("Start RGui with Workspace", window2));
		} else {
			// No R shell for Bio7 Linux and Mac.
			// rMenu.add(new SaveRWorkspaceand_Start("Start R Shell", window2));
		}
		rMenu.add(new Separator());
		rMenu.add(openRservePref);

		OpenOfficeMenu.add(officeopenspread);
		OpenOfficeMenu.add(new Separator());
		OpenOfficeMenu.add(officevaluetor);
		OpenOfficeMenu.add(officevaluetorhead);
		//OpenOfficeMenu.add(officevalue);
		OpenOfficeMenu.add(new Separator());
		OpenOfficeMenu.add(officepopdata);

		/*
		 * BeanShellMenu.add(bshclearaction); BeanShellMenu.add(new Separator());
		 * BeanShellMenu.add(bshimportaction);
		 */
		WindowMenu.add(viewMenu);
		WindowMenu.add(viewMenuAll);
		showViewMenuMgr.add(showViewItem);
		WindowMenu.add(showViewMenuMgr);
		// WindowMenu.add(showViewMenu); // Displays the show menu.

		WindowMenu.add(openPerspectiveMenu);
		WindowMenu.add(new Separator());
		WindowMenu.add(new ShowEditorAreaAction("Show/Hide Editor", window2));
		WindowMenu.add(new Separator());
		// WindowMenu.add(hideMainMenu);
		// WindowMenu.add(showMainMenu);
		// WindowMenu.add(toggleCoolBar);
		WindowMenu.add(new Separator());
		WindowMenu.add(saveperspectiveas);
		helpMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

		if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
			helpMenu.add(introAction);
		}

		helpMenu.add(new Separator());
		helpMenu.add(helpAction);
		helpMenu.add(dynamichelp);
		helpMenu.add(new Separator());
		helpMenu.add(openBrowser);
		helpMenu.add(new Separator());
		// helpMenu.add(update_action);
		// helpMenu.add(add_extension);
		helpMenu.add(new Separator());
		helpMenu.add(aboutAction);

	}

	public void fillStatusLine(IStatusLineManager statusLineMgr) {
		super.fillStatusLine(statusLineMgr);

		userItem = new StatusLineContributionItem("Timesteps");

		userItem.setText("Timesteps: " + Time.getCounter());

		statusLineMgr.add(userItem);

	}

	protected void fillCoolBar(ICoolBarManager coolBar) {
		IToolBarManager toolbar = new ToolBarManager(SWT.FLAT | SWT.LEFT);
		// IToolBarManager imagebar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		// coolBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

		coolBar.add(new ToolBarContributionItem(toolbar, "mainbio7"));
		// coolBar.add(new ToolBarContributionItem(imagebar, "image"));
		toolbar.add(start);
		toolbar.add(setup);
		toolbar.add(resetfield);
		toolbar.add(counterreset);
		toolbar.add(random);
		toolbar.add(libreofficeconnection);
		toolbar.add(startrserve);
		toolbar.add(print);
		// toolbar.add(fastOpenScriptAction);

	}

	public static Action getStart() {
		return start;
	}

	public static Action getStartrserve() {
		return startrserve;
	}

	public static Action getOpenofficeconnection() {
		return libreofficeconnection;
	}

	public static Action getFlowstop() {
		return flowstop;
	}

	public static FlowExternalStartAction getFlowexternalstartaction() {
		return flowexternalstartaction;
	}

	public static BeanShellImportAction getBshimportaction() {
		return bshimportaction;
	}

	public static Action getCounterreset() {
		return counterreset;
	}

	public static Action getStart3d() {
		return start3d;
	}

	public static StatusLineContributionItem getUserItem() {
		return userItem;
	}

	protected MenuManager createSubMenu(String text, String id, ContributionItemFactory factory) {
		MenuManager submenu = new MenuManager(text, id);
		submenu.add(factory.create(this.window));
		return submenu;
	}

	protected MenuManager createSubMenu(String text, String id, Object... items) {
		MenuManager submenu = new MenuManager(text, id);
		for (Object i : items) {
			if (i instanceof IContributionItem)
				submenu.add((IContributionItem) i);
			else if (i instanceof IWorkbenchAction)
				submenu.add((IWorkbenchAction) i);
		}
		return submenu;
	}

	/* List files and folders recursively! */
	public void createSubMenus(String directoryName) {
		File directory = new File(directoryName);
		File[] fList = directory.listFiles();
		for (File file : fList) {
			if (file.isFile()) {
				createScriptSubmenus(file);
			} else if (file.isDirectory()) {
				MenuManager men = menuStack.peek();
				MenuManager submenu = new MenuManager(file.getName());
				// submenu.setRemoveAllWhenShown(true);
				menuStack.push(submenu);
				men.add(submenu);
				createSubMenus(file.getAbsolutePath());
			}

		}
		/* Leave the menu! */
		menuStack.pop();

	}

	/* Create a menu item and action for the different files! */
	public void createScriptSubmenus(File file) {

		MenuManager submenu = menuStack.peek();

		submenu.add(new ExecuteScriptAction(file.getName().substring(0, file.getName().lastIndexOf(".")), window2, file));

	}

}
