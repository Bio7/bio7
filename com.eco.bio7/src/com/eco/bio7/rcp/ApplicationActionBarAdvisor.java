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
import java.io.FilenameFilter;

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
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.actions.ActivateRPlots;
import com.eco.bio7.actions.BeanShellClearAction;
import com.eco.bio7.actions.BeanShellImportAction;
import com.eco.bio7.actions.ClearRWorkspace;
import com.eco.bio7.actions.ClipboardRScipt;
import com.eco.bio7.actions.ClipboardRValues;
import com.eco.bio7.actions.CounterReset;
import com.eco.bio7.actions.EnableSelection;
import com.eco.bio7.actions.ExecuteImageMacroAction;
import com.eco.bio7.actions.ExecuteRScriptAction;
import com.eco.bio7.actions.ExecuteRTextSelection;
import com.eco.bio7.actions.ExecuteScriptAction;
import com.eco.bio7.actions.FlowExternalStartAction;
import com.eco.bio7.actions.FlowStopAction;
import com.eco.bio7.actions.InstallRPackage;
import com.eco.bio7.actions.LoadRLibrary;
import com.eco.bio7.actions.LoadRWorkspace;
import com.eco.bio7.actions.OfficeOpenAction;
import com.eco.bio7.actions.OfficeSendValueAction;
import com.eco.bio7.actions.OfficeValueAction;
import com.eco.bio7.actions.OfficeValueToRAction;
import com.eco.bio7.actions.OfficeValueToRHeadAction;
import com.eco.bio7.actions.OpenBio7BrowserAction;
import com.eco.bio7.actions.LibreOfficeConnection;
import com.eco.bio7.actions.OpenRservePreferencesAction;
import com.eco.bio7.actions.PrintExpression;
import com.eco.bio7.actions.Random;
import com.eco.bio7.actions.ResetField;
import com.eco.bio7.actions.SaveRWorkspace;
import com.eco.bio7.actions.SaveRWorkspaceand_Start;
import com.eco.bio7.actions.SetupDiscrete;
import com.eco.bio7.actions.ShowEditorAreaAction;
import com.eco.bio7.actions.Start;
import com.eco.bio7.actions.Start3d;
import com.eco.bio7.actions.StartRServe;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.time.Time;

public class ApplicationActionBarAdvisor extends ActionBarAdvisor {
	private static StatusLineContributionItem userItem = null;

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

	private IWorkbenchAction findAction;

	private IWorkbenchAction helpAction;

	private static Action start3d;

	private Action resetfield;

	private static Action counterreset;

	private Action random;

	private static Action startrserve;

	private Action setup;

	private static Action flowstop;

	private IContributionItem re;

	private IContributionItem perspect;

	private IWorkbenchWindow window2;

	//private static NutrientAction nutrientaction;

	private IPreferenceStore store;

	private OfficeOpenAction officeopenspread;

	private OfficeValueToRAction officevaluetor;

	private OfficeValueToRHeadAction officevaluetorhead;

	private OfficeValueAction officevalue;

	private OfficeSendValueAction officepopdata;

	private BeanShellClearAction bshclearaction;

	private static BeanShellImportAction bshimportaction;

	private IWorkbenchAction saveperspectiveas;

	private PrintExpression printexpression;

	private static FlowExternalStartAction flowexternalstartaction;

	private ExecuteRTextSelection rselect;

	private ClipboardRScipt clipboardRScript;

	private ClipboardRValues clipboardRValues;

	private InstallRPackage installRPackage;

	private LoadRLibrary loadRLibrary;

	private OpenRservePreferencesAction openRservePref;

	private LoadRWorkspace loadR;

	private SaveRWorkspace saveR;

	private OpenBio7BrowserAction openBrowser;

	private MenuManager helpMenu;

	private ActivateRPlots enableRPlots;

	public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
		super(configurer);
		store = Bio7Plugin.getDefault().getPreferenceStore();
	}

	protected void makeActions(final IWorkbenchWindow window) {
		window2 = window;

		New = ActionFactory.NEW.create(window);
		New.setText("New");
		register(New);

		officeopenspread = new OfficeOpenAction("Open Spreadsheet", window2);

		officevaluetor = new OfficeValueToRAction("Data to R", window2);

		officevaluetorhead = new OfficeValueToRHeadAction("Data with head to R ", window2);

		officevalue = new OfficeValueAction("Data to Beanshell", window2);

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

		exitAction = ActionFactory.QUIT.create(window);
		register(exitAction);

		aboutAction = ActionFactory.ABOUT.create(window);
		register(aboutAction);

		newWindowAction = ActionFactory.OPEN_NEW_WINDOW.create(window);
		register(newWindowAction);

		libreofficeconnection = new LibreOfficeConnection("Open LibreOffice Connection", window);
		register(libreofficeconnection);

		/*bshclearaction = new BeanShellClearAction("Clear", window);
		register(bshclearaction);

		bshimportaction = new BeanShellImportAction("Import", window);
		register(bshimportaction);*/

		start = new Start("Start", window);
		register(start);

		start3d = new Start3d("Start/Stop Animation", window);
		register(start3d);

		//nutrientaction = new NutrientAction("nutrient_action", window);
		//register(nutrientaction);

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
		/*
		 * savepattern.setToolTipText("Save a pattern to a file" + '\n' +
		 * "Bio7"); loadpattern = new LoadPattern("Load Pattern", window);
		 * register(loadpattern);
		 * loadpattern.setToolTipText("Load a pattern from a file" + '\n' +
		 * "Bio7");
		 */

		re = ContributionItemFactory.VIEWS_SHORTLIST.create(window);
		perspect = ContributionItemFactory.PERSPECTIVES_SHORTLIST.create(window);

		flowexternalstartaction = new FlowExternalStartAction("Flowexternalstart", window);
		register(flowexternalstartaction);

		flowstop = new FlowStopAction("Flowstop", window);
		register(flowstop);

		rselect = new ExecuteRTextSelection("Evaluate Selection", window);
		register(rselect);

		clipboardRScript = new ClipboardRScipt("Evaluate Clipboard", window);
		register(clipboardRScript);

		clipboardRValues = new ClipboardRValues("Get Clipboard Data", window);
		register(clipboardRValues);
		
		enableRPlots=new ActivateRPlots("Activate R Plots", window);
		register(enableRPlots);

		installRPackage = new InstallRPackage("Install package(s)", window);
		register(installRPackage);

		loadRLibrary = new LoadRLibrary("Load/Remove package(s)", window);
		register(loadRLibrary);

		openRservePref = new OpenRservePreferencesAction();
		register(openRservePref);

		loadR = new LoadRWorkspace("Load Workspace", window);
		register(loadR);

		saveR = new SaveRWorkspace("Save Workspace", window);
		register(saveR);

		openBrowser = new OpenBio7BrowserAction("Bio7 Internet", window);
		register(openBrowser);
	}

	protected void fillMenuBar(IMenuManager menuBar) {
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

		MenuManager rMenu = new MenuManager("R");
		MenuManager OpenOfficeMenu = new MenuManager("LibreOffice");
		//MenuManager BeanShellMenu = new MenuManager("Bsh");

		MenuManager WindowMenu = new MenuManager("&Window");
		helpMenu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);// p2
		MenuManager showViewMenu = new MenuManager("Show View");
		MenuManager openPerspectiveMenu = new MenuManager("Open Perspective");
		MenuManager scriptMenu = new MenuManager("&Scripts");
		//
		MenuManager generalMenu = new MenuManager("&General-Scripts");
		generalMenu.add(new ExecuteScriptAction("Empty", window2, new File("")));
		generalMenu.setRemoveAllWhenShown(true);
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager m) {
				File files = new File(store.getString(PreferenceConstants.D_SCRIPT_GENERAL));
				File[] fil = ListFilesDirectory(files, new String[] { ".bsh", ".groovy",".py" });

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

		generalMenu.addMenuListener(listener);
		MenuManager spatialMenu = new MenuManager("&Spatial-Scripts");
		spatialMenu.add(new ExecuteScriptAction("Empty", window2, new File("")));
		spatialMenu.setRemoveAllWhenShown(true);
		IMenuListener listener4 = new IMenuListener() {
			public void menuAboutToShow(IMenuManager m) {
				File files = new File(store.getString(PreferenceConstants.D_SCRIPT_SPATIAL));
				File[] fil = ListFilesDirectory(files, new String[] { ".bsh", ".groovy",".py" });

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

		spatialMenu.addMenuListener(listener4);

		MenuManager macrosImageJMenu = new MenuManager("&ImageJ-Macros");
		macrosImageJMenu.add(new ExecuteImageMacroAction("Empty", window2, new File("")));
		macrosImageJMenu.setRemoveAllWhenShown(true);
		IMenuListener listener5 = new IMenuListener() {
			public void menuAboutToShow(IMenuManager m) {
				File files = new File(store.getString(PreferenceConstants.D_SCRIPT_IMAGE));
				File[] fil = ListFileDirectory(files, ".txt");

				if (fil.length > 0) {
					int a;
					for (int i = 0; i < fil.length; i++) {

						m.add(new ExecuteImageMacroAction(fil[i].getName().substring(0, fil[i].getName().lastIndexOf(".")), window2, fil[i]));
						a = i + 1;
						if (a % 5 == 0) {
							m.add(new Separator());
						}
					}

				}

				else {
					m.add(new ExecuteImageMacroAction("Empty", window2, new File("")));
				}

			}

		};

		macrosImageJMenu.addMenuListener(listener5);

		MenuManager rScriptMenu = new MenuManager("&R-Scripts");
		rScriptMenu.add(new ExecuteScriptAction("Empty", window2, new File("")));
		rScriptMenu.setRemoveAllWhenShown(true);
		IMenuListener listener6 = new IMenuListener() {
			public void menuAboutToShow(IMenuManager m) {
				File files = new File(store.getString(PreferenceConstants.D_SCRIPT_R));
				File[] fil = ListFileDirectory(files, ".R");

				if (fil.length > 0) {
					int a;
					for (int i = 0; i < fil.length; i++) {

						m.add(new ExecuteRScriptAction(fil[i].getName().substring(0, fil[i].getName().lastIndexOf(".")), window2, fil[i]));
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

		rScriptMenu.addMenuListener(listener6);

		MenuManager importScriptMenu = new MenuManager("&Import-Scripts");
		importScriptMenu.add(new ExecuteScriptAction("Empty", window2, new File("")));
		importScriptMenu.setRemoveAllWhenShown(true);
		IMenuListener listener2 = new IMenuListener() {
			public void menuAboutToShow(IMenuManager m) {
				File files = new File(store.getString(PreferenceConstants.D_IMPORT));
				File[] fil = ListFilesDirectory(files, new String[] { ".bsh", ".groovy",".py" });

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
				File[] fil = ListFilesDirectory(files, new String[] { ".bsh", ".groovy",".py" });

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

		showViewMenu.add(re);
		openPerspectiveMenu.add(perspect);
		prefMenu.add(propAction);

		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(findMenu);
		menuBar.add(prefMenu);
		menuBar.add(scriptMenu);
		menuBar.add(rMenu);
		menuBar.add(OpenOfficeMenu);
		//menuBar.add(BeanShellMenu);
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
		fileMenu.add(new Separator());
		fileMenu.add(Import);
		fileMenu.add(Export);
		fileMenu.add(new Separator());
		fileMenu.add(exitAction);

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

		// findMenu.add(findAction);

		scriptMenu.add(generalMenu);
		scriptMenu.add(new Separator());
		scriptMenu.add(rScriptMenu);
		scriptMenu.add(new Separator());
		scriptMenu.add(spatialMenu);
		scriptMenu.add(new Separator());
		scriptMenu.add(macrosImageJMenu);

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
		rMenu.add(new ClearRWorkspace("Remove all objects", window2));
		rMenu.add(new Separator());
		rMenu.add(installRPackage);
		rMenu.add(loadRLibrary);
		rMenu.add(new Separator());
		if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
			rMenu.add(new SaveRWorkspaceand_Start("Start RGui with Workspace", window2));
		} else {
			rMenu.add(new SaveRWorkspaceand_Start("Start R Shell", window2));
		}
		rMenu.add(new Separator());
		rMenu.add(openRservePref);

		OpenOfficeMenu.add(officeopenspread);
		OpenOfficeMenu.add(new Separator());
		OpenOfficeMenu.add(officevaluetor);
		OpenOfficeMenu.add(officevaluetorhead);
		OpenOfficeMenu.add(officevalue);
		OpenOfficeMenu.add(new Separator());
		OpenOfficeMenu.add(officepopdata);

		/*BeanShellMenu.add(bshclearaction);
		BeanShellMenu.add(new Separator());
		BeanShellMenu.add(bshimportaction);*/

		WindowMenu.add(showViewMenu); // Displays the show menu.
		WindowMenu.add(openPerspectiveMenu);
		WindowMenu.add(new Separator());
		WindowMenu.add(new ShowEditorAreaAction("Show/Hide Editor", window2));
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
		IToolBarManager imagebar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);

		coolBar.add(new ToolBarContributionItem(toolbar, "main"));
		coolBar.add(new ToolBarContributionItem(imagebar, "image"));

		toolbar.add(start);
		toolbar.add(setup);
		toolbar.add(resetfield);
		toolbar.add(counterreset);
		toolbar.add(random);
		toolbar.add(libreofficeconnection);
		toolbar.add(startrserve);
		toolbar.add(print);

	}

	public static File[] ListFileDirectory(File filedirectory, final String extension) {
		File dir = filedirectory;

		String[] children = dir.list();
		if (children == null) {

		} else {
			for (int i = 0; i < children.length; i++) {
				// Get filename of the file or directory inside Bio7.
				String filename = children[i];
			}
		}

		// Filter the extension of the file.
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.endsWith(extension));
			}
		};

		File[] files = dir.listFiles(filter);

		return files;
	}

	public static File[] ListFilesDirectory(File filedirectory, final String[] extensions) {
		File dir = filedirectory;

		String[] children = dir.list();
		if (children == null) {

		} else {
			for (int i = 0; i < children.length; i++) {
				// Get filename of the file or directory inside Bio7.
				String filename = children[i];
			}
		}

		// Filter the extension of the file.
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.endsWith(extensions[0]) || name.endsWith(extensions[1]));
			}
		};

		File[] files = dir.listFiles(filter);

		return files;
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

	
}
