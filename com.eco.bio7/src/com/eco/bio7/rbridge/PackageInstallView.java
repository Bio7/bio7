package com.eco.bio7.rbridge;

import java.util.HashMap;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.browser.BrowserView;
import com.eco.bio7.collection.Work;
import com.eco.bio7.documents.JavaFXWebBrowser;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.util.Util;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;

public class PackageInstallView extends ViewPart {

	public static final String ID = "com.eco.bio7.rbridge.PackageInstallView"; //$NON-NLS-1$
	private Text text;
	private static List allPackagesList;
	private static List allInstalledPackagesList;
	private HashMap<String, String[]> map = new HashMap<String, String[]>();
	private Button btnContextSensitive;
	private Label lblSearch;
	private Button btnCheckButton;
	protected Job job;
	private Button updatePackagesButton;
	private CTabFolder tabFolder;
	private CTabItem tbtmUpdatePackages;
	private CTabItem tbtmInstalledPackages;
	private Composite composite;
	private IEditorPart editor;
	private Button btnUpdate;
	private Tree tree;
	private Button btnNewButton;

	public PackageInstallView() {
		editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

	}

	/**
	 * Create contents of the view part.
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {

		createActions();
		initializeToolBar();
		initializeMenu();

		tabFolder = new CTabFolder(parent, SWT.BORDER);
		tabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
				if (tabFolder.getSelectionIndex() == 1) {
					packageInstall();
				} else if (tabFolder.getSelectionIndex() == 2) {
					if (RServe.isAlive()) {
						createAttachedPackageTree();
					}

				}

			}
		});
		tabFolder.setSelectionBackground(
				Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		tbtmUpdatePackages = new CTabItem(tabFolder, SWT.NONE);
		tabFolder.setSelection(tbtmUpdatePackages);
		tbtmUpdatePackages.setText("Install Packages");
		Composite container = new Composite(tabFolder, SWT.NONE);
		tbtmUpdatePackages.setControl(container);
		container.setLayout(new GridLayout(2, true));

		allPackagesList = new List(container, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		allPackagesList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 6));

		allPackagesList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (btnCheckButton.getSelection()) {
					int[] selection = allPackagesList.getSelectionIndices();
					if (selection.length > 0) {
						String text = allPackagesList.getItem(selection[0]);
						if (text.isEmpty() == false) {
							IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
							String openInJavaFXBrowser = store.getString("BROWSER_SELECTION");
							String installPackagesDescritpionUrl = store.getString("INSTALL_R_PACKAGES_DESCRPTION_URL");
							String packageInfoSite = installPackagesDescritpionUrl + text + "/index.html";
							if (openInJavaFXBrowser.equals("SWT_BROWSER")) {
								Display display = Util.getDisplay();
								display.asyncExec(new Runnable() {

									public void run() {
										Work.openView("com.eco.bio7.browser.Browser");

										BrowserView b = BrowserView.getBrowserInstance();
										b.browser.setJavascriptEnabled(true);

										b.setLocation(packageInfoSite);
									}
								});
							}

							else {
								Display display = Util.getDisplay();
								display.asyncExec(new Runnable() {

									public void run() {
										JavaFXWebBrowser br = new JavaFXWebBrowser(true);
										br.setDarkCssIfDarkTheme(true);
										br.createBrowser(packageInfoSite, "R Package Description");

									}
								});
							}
						}
					}

				}
			}
		});

		lblSearch = new Label(container, SWT.NONE);
		lblSearch.setText("Search");
		new Label(container, SWT.NONE);

		text = new Text(container, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		text.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event event) {
				if (btnContextSensitive.getSelection()) {
					allPackagesList.deselectAll();
					text.getText();

					for (int i = 0; i < allPackagesList.getItemCount(); i++) {
						String it = allPackagesList.getItem(i);

						if (it.startsWith(text.getText())) {
							/*
							 * We don't want a flickery search results each time we type a character. So we
							 * update the info after a certain time interval with a job!
							 */
							loadPackageDescriptionHtml(event, i);
							return;
						}
					}
				}

				else {
					allPackagesList.deselectAll();
					text.getText();

					for (int i = 0; i < allPackagesList.getItemCount(); i++) {

						String it = allPackagesList.getItem(i).toLowerCase();
						if (it.startsWith(text.getText().toLowerCase())) {
							/*
							 * We don't want a flickery search results each time we type a character. So we
							 * update the info after a certain time interval with a job!
							 */
							loadPackageDescriptionHtml(event, i);

							return;
						}
					}

				}

			}

		});

		btnContextSensitive = new Button(container, SWT.CHECK);
		btnContextSensitive.setText("Context Sensitive");
		new Label(container, SWT.NONE);

		btnCheckButton = new Button(container, SWT.CHECK);
		btnCheckButton.setText("Open Package Description");
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);

		final Button updateButton = new Button(container, SWT.NONE);
		GridData gd_updateButton = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_updateButton.heightHint = 40;
		updateButton.setLayoutData(gd_updateButton);
		updateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog() == false) {
					return;
				}
				if (RState.isBusy() == false) {
					RState.setBusy(true);
					ListRPackagesJob Do = new ListRPackagesJob();
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

					Bio7Dialog.message("Rserve is busy!");

				}

			}

		});
		updateButton.setText("Load Packages List");

		final Button installButton = new Button(container, SWT.NONE);
		GridData gd_installButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_installButton.heightHint = 40;
		installButton.setLayoutData(gd_installButton);
		installButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RState.isBusy() == false) {
					RState.setBusy(true);
					InstallRPackagesJob Do = new InstallRPackagesJob();
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

					Bio7Dialog.message("Rserve is busy!");

				}
			}
		});
		installButton.setText("Install Selected");

		updatePackagesButton = new Button(container, SWT.NONE);
		updatePackagesButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (RState.isBusy() == false) {
					RState.setBusy(true);
					UpdateRPackagesJob Do = new UpdateRPackagesJob();
					Do.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent event) {
							if (event.getResult().isOK()) {
								RState.setBusy(false);
								Bio7Dialog.message("All packages were updated!");
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
		});
		GridData gd_updatePackagesButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_updatePackagesButton.heightHint = 40;
		updatePackagesButton.setLayoutData(gd_updatePackagesButton);
		updatePackagesButton.setText("Update Installed Packages");

		tbtmInstalledPackages = new CTabItem(tabFolder, SWT.NONE);
		tbtmInstalledPackages.setText("Load Packages");

		composite = new Composite(tabFolder, SWT.NONE);

		composite.setLayout(new GridLayout(2, false));
		GridData gd_composite = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
		gd_composite.heightHint = 376;
		gd_composite.widthHint = 278;
		composite.setLayoutData(gd_composite);

		allInstalledPackagesList = new List(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		allInstalledPackagesList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		btnUpdate = new Button(composite, SWT.NONE);
		btnUpdate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (RServe.isAliveDialog()) {

					packageInstall();
				}
			}

		});
		GridData gd_btnUpdate = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_btnUpdate.heightHint = 40;
		btnUpdate.setLayoutData(gd_btnUpdate);
		btnUpdate.setText("Refresh Packages List");

		final Button uninstallButton = new Button(composite, SWT.NONE);
		uninstallButton.setSize(269, 27);
		GridData gd_uninstallButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_uninstallButton.heightHint = 40;
		gd_uninstallButton.widthHint = 133;
		uninstallButton.setLayoutData(gd_uninstallButton);
		uninstallButton
				.setToolTipText("Removes installed packages/bundles and updates index information as necessary. ");
		uninstallButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAlive()) {
					if (RState.isBusy() == false) {
						RState.setBusy(true);
						RemoveRLibrarysJob Do = new RemoveRLibrarysJob();
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

						Bio7Dialog.message("Rserve is busy!");

					}
				} else {
					System.out.println("No Rserve connection available !");
				}

			}
		});
		uninstallButton.setText("Remove Selected Package");

		Button btnAddLibraryDeclaration = new Button(composite, SWT.NONE);
		btnAddLibraryDeclaration.setSize(269, 27);
		GridData gd_btnAddLibraryDeclaration = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_btnAddLibraryDeclaration.heightHint = 40;
		btnAddLibraryDeclaration.setLayoutData(gd_btnAddLibraryDeclaration);
		btnAddLibraryDeclaration.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				setInDocument(allInstalledPackagesList);
			}
		});
		btnAddLibraryDeclaration.setToolTipText("Add selected package items as library declaration to R editor source");
		btnAddLibraryDeclaration.setText("Add selected to R editor");

		allInstalledPackagesList.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent e) {
				if (RServe.isAlive()) {
					if (RState.isBusy() == false) {
						RState.setBusy(true);
						LoadRLibrarysJob Do = new LoadRLibrarysJob(editor);
						Do.addJobChangeListener(new JobChangeAdapter() {
							public void done(IJobChangeEvent event) {
								if (event.getResult().isOK()) {
									RState.setBusy(false);
									/* Also reload the R-Shell code completion in an extra job! */
									RShellView rShellView = RShellView.getInstance();
									if (rShellView != null) {
										rShellView.getShellCompletion().update();
									}
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
				} else {
					System.out.println("No Rserve connection available !");
				}

			}
		});
		allInstalledPackagesList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

			}
		});

		tbtmInstalledPackages.setControl(composite);

		/*
		 * Composite container2 = new Composite(tabFolder, SWT.NONE);
		 * tbtmUpdatePackages.setControl(container2); container2.setLayout(new
		 * GridLayout(2, true));
		 */

		CTabItem packagesTabItem = new CTabItem(tabFolder, SWT.NONE);
		packagesTabItem.setText("Attached Packages");

		Composite composite_2 = new Composite(tabFolder, SWT.NONE);
		packagesTabItem.setControl(composite_2);
		composite_2.setLayout(new GridLayout(2, true));

		tree = new Tree(composite_2, SWT.BORDER);
		GridData gd_tree = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd_tree.heightHint = 277;
		gd_tree.widthHint = 107;
		tree.setLayoutData(gd_tree);
		final Menu menuList = new Menu(tree);
		tree.setMenu(menuList);
		menuList.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				// Get rid of existing menu items
				MenuItem[] items = menuList.getItems();
				for (int i = 0; i < items.length; i++) {
					((MenuItem) items[i]).dispose();
				}
				MenuItem refreshItem = new MenuItem(menuList, SWT.NONE);

				refreshItem.setText("Refresh");
				refreshItem.addSelectionListener(new SelectionListener() {

					public void widgetSelected(SelectionEvent e) {

						createAttachedPackageTree();

					}

					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});
				// Add menu items for current selection
				MenuItem detachItem = new MenuItem(menuList, SWT.NONE);

				detachItem.setText("Detach");
				detachItem.addSelectionListener(new SelectionListener() {

					public void widgetSelected(SelectionEvent e) {

						if (tree.getSelection().length > 0) {
							String selectedPackage = tree.getSelection()[0].getText();
							RServeUtil.evalR("try(detach(package:" + selectedPackage + ", unload=TRUE))", null);
							createAttachedPackageTree();

						}

					}

					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});

			}
		});

		tree.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				Point point = new Point(event.x, event.y);
				TreeItem item = tree.getItem(point);
				if (item != null) {

				}
			}
		});
		packagesTabItem.setControl(composite_2);

		btnNewButton = new Button(composite_2, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				createAttachedPackageTree();
			}
		});
		GridData gd_btnNewButton = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
		gd_btnNewButton.heightHint = 40;
		btnNewButton.setLayoutData(gd_btnNewButton);
		btnNewButton.setText("Refresh Packages List");

		map.clear();
		map.put("spatstat",
				new String[] {
						"A package for the statistical analysis of spatial data, \n mainly spatial point patterns.",
						"http://www.spatstat.org" });
		map.put("raster",
				new String[] {
						"A package for reading, writing, manipulating, analyzing and modeling of gridded spatial data.",
						"https://cran.r-project.org/web/packages/raster/index.html" });
		map.put("maptools", new String[] { "Tools for reading and handling spatial objects",
				"http://cran.r-project.org/web/packages/maptools/index.html" });
		map.put("gstat", new String[] {
				"Gstat is an open source (GPL) computer code for multivariable \n geostatistical modelling, prediction and simulation",
				"http://www.gstat.org/" });
		map.put("maps", new String[] {
				"Display of maps. Projection code and larger maps are in separate packages (mapproj and mapdata).",
				"http://cran.r-project.org/web/packages/maps/index.html" });
		map.put("rgdal", new String[] {
				"Provides bindings to Frank Warmerdam's Geospatial Data Abstraction Library (GDAL) (>= 1.3.1)\n and access to projection/transformation operations from the PROJ.4 library.",
				"http://cran.r-project.org/web/packages/rgdal/index.html" });
		map.put("PBSmapping", new String[] {
				"This software has evolved from fisheries research conducted at the Pacific Biological Station (PBS) in Nanaimo, British Columbia, Canada.\n It extends the R language to include two-dimensional plotting features similar to those commonly available in a Geographic Information System (GIS).",
				"http://cran.r-project.org/web/packages/PBSmapping/index.html" });
		map.put("shapefiles", new String[] { "Functions to read and write ESRI shapefiles.",
				"http://cran.r-project.org/web/packages/shapefiles/index.html" });
		map.put("RSAGA", new String[] {
				"RSAGA provides access to geocomputing and terrain analysis functions of SAGA from within R\n by running the command line version of SAGA.",
				"http://cran.r-project.org/web/packages/RSAGA/index.html" });
		map.put("geoR",
				new String[] { "Geostatistical analysis including traditional, likelihood-based and Bayesian methods.",
						"http://leg.ufpr.br/geoR/" });
		map.put("geoRglm", new String[] {
				"Functions for inference in generalised linear spatial models. \nThe posterior and predictive inference is based on Markov chain Monte Carlo methods.",
				"http://gbi.agrsci.dk/~ofch/geoRglm/" });
		map.put("SDMTools", new String[] {
				"This packages provides a set of tools for post processing the outcomes\nof species distribution modeling exercises. It includes novel methods for comparing models\nand tracking changes in distributions through time.\nIt further includes methods for visualizing outcomes, selecting thresholds, calculating measures\nof accuracy and landscape fragmentation statistics, etc.",
				"http://cran.r-project.org/web/packages/SDMTools/" });

	}

	/*
	 * We don't want a flickery search results each time we type a character. So we
	 * update the info after a certain time interval with a job!
	 */
	private void loadPackageDescriptionHtml(Event event, int i) {
		final int count = i;
		allPackagesList.select(count);
		allPackagesList.showSelection();
		if (job != null) {
			job.cancel();
		}

		job = new Job("Load") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Load...", IProgressMonitor.UNKNOWN);

				Display display = PlatformUI.getWorkbench().getDisplay();
				display.syncExec(new Runnable() {

					public void run() {

						allPackagesList.notifyListeners(SWT.Selection, event);

					}
				});

				monitor.done();
				return Status.OK_STATUS;
			}

		};

		job.schedule(500);
	}

	/**
	 * Create the actions.
	 */
	private void createActions() {
		// Create the actions
	}

	/**
	 * Initialize the toolbar.
	 */
	private void initializeToolBar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
	}

	/**
	 * Initialize the menu.
	 */
	private void initializeMenu() {
		IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
	}

	@Override
	public void setFocus() {
		// Set the focus
	}

	public static List getAllList() {
		return allPackagesList;
	}

	public void packageInstall() {
		RConnection c = REditor.getRserveConnection();
		if (c != null) {

			String[] listPackages = null;

			RServeUtil.evalR("try(.bio7ListOfWebPackages <- list(sort(.packages(all.available = TRUE))))", null);

			try {
				listPackages = RServeUtil.fromR(".bio7ListOfWebPackages[[1]]").asStrings();
			} catch (REXPMismatchException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			allInstalledPackagesList.setItems(listPackages);

		}

	}

	private void setInDocument(List aList) {
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		if (editor != null && editor instanceof REditor) {

			String[] items = aList.getSelection();
			StringBuffer buff = new StringBuffer();
			for (int i = 0; i < items.length; i++) {
				buff.append("library(");
				buff.append(items[i]);
				buff.append(")");
				buff.append(System.lineSeparator());
			}
			ITextEditor editor2 = (ITextEditor) editor;

			IDocumentProvider dp = editor2.getDocumentProvider();
			IDocument doc = dp.getDocument(editor.getEditorInput());

			ISelectionProvider sp = editor2.getSelectionProvider();
			ISelection selectionsel = sp.getSelection();
			ITextSelection selection = (ITextSelection) selectionsel;

			int off = selection.getOffset();

			try {
				doc.replace(off, 0, buff.toString());
			} catch (BadLocationException e1) {
				e1.printStackTrace();
			}

		}
	}

	public static List getAllPackagesList() {
		return allPackagesList;
	}

	public static List getAllInstalledPackagesList() {
		return allInstalledPackagesList;
	}

	public void createAttachedPackageTree() {
		if (RServe.isAliveDialog() == false) {
			return;
		}
		REXP pack = null;
		tree.removeAll();
		/* Just to avoid the dialog from RServeUtil! */

		String[] v = null;
		// List all variables in the R workspace!

		RServeUtil.evalR(
				".bio7TempVarEnvironment <- new.env();try(.bio7TempVarEnvironment$workspaceRPackages<-.packages())",
				null);
		pack = RServeUtil.fromR("try(.bio7TempVarEnvironment$workspaceRPackages)");
		try {
			v = pack.asStrings();
		} catch (REXPMismatchException e1) {

			e1.printStackTrace();
		}
		RServeUtil.evalR("try(rm(workspaceRPackages,envir=.bio7TempVarEnvironment))", null);

		TreeItem packages = new TreeItem(tree, 0);
		packages.setText("Attached Packages:");
		// packages.removeAll();
		for (int i = 0; i < v.length; i++) {

			// lsf.str("package:MASS")
			TreeItem treeItem = new TreeItem(packages, 0);
			treeItem.setText(v[i]);

		}
		packages.setExpanded(true);

	}

}
