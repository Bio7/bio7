package com.eco.bio7.rbridge.views;

import java.io.IOException;
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
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.browser.BrowserView;
import com.eco.bio7.collection.Work;
import com.eco.bio7.documents.JavaFXWebBrowser;
import com.eco.bio7.rbridge.InstallRPackagesJob;
import com.eco.bio7.rbridge.ListRPackagesJob;
import com.eco.bio7.rbridge.LoadRLibrarysJob;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RServeUtil;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.RemoveRLibrarysJob;
import com.eco.bio7.rbridge.UpdateREnvironmentTableJob;
import com.eco.bio7.rbridge.UpdateRPackagesJob;
import com.eco.bio7.rbridge.UpdateSelectedRPackagesJob;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.util.Util;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.custom.SashForm;

public class PackageInstallView extends ViewPart {

	public static final String ID = "com.eco.bio7.rbridge.PackageInstallView"; //$NON-NLS-1$
	private Text text;
	private static List allPackagesList;
	private static Table allInstalledPackagesList;
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
	private TableColumn column1;
	private TableColumn column2;
	private TableColumn column3;
	private Button btnUpdateSelectedPackage;
	private Button btnCheckSelectedPackageButton;
	private Label emptyLabel;
	protected RList out;
	private Tree table;
	private TreeColumn columnEnvironment1;
	private TreeColumn columnEnvironment2;
	private TreeColumn columnEnvironment3;
	private SashForm sashForm;
	private Composite composite_1;
	private Composite composite_2;
	private Composite composite_3;
	protected boolean showFunc;

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
		// initializeToolBar();
		initializeMenu();

		tabFolder = new CTabFolder(parent, SWT.BORDER);
		tabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent event) {
				if (tabFolder.getSelectionIndex() == 1) {
					// if(allInstalledPackagesList.getItemCount()==0) {
					packageInstallUpdateJob();
					// }
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
										if (ApplicationWorkbenchWindowAdvisor.isThemeBlack()) { //

											org.jsoup.nodes.Document document = null;
											try {
												document = Jsoup.connect(packageInfoSite).get();
											} catch (IOException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}

											Elements links = document.select("body");
											links.attr("style", "background: #252525; color: #CCCCCC;");
											Elements a = document.select("a");
											a.attr("style", "background: #252525;color: #FFFFFF;");
											String html = document.html();
											BrowserView b = BrowserView.getBrowserInstance();
											b.browser.setText(html, true);

										} else {
											BrowserView b = BrowserView.getBrowserInstance();
											b.setLocation(packageInfoSite);
										}

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
		lblSearch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		lblSearch.setText("Search");
		new Label(container, SWT.NONE);

		text = new Text(container, SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		text.addListener(SWT.Modify, new Listener() {
			public void handleEvent(Event event) {
				if (btnContextSensitive.getSelection()) {
					allPackagesList.deselectAll();
					String sel = text.getText();

					for (int i = 0; i < allPackagesList.getItemCount(); i++) {
						String it = allPackagesList.getItem(i);

						if (it.startsWith(sel)) {
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
					String sel = text.getText();

					for (int i = 0; i < allPackagesList.getItemCount(); i++) {

						String it = allPackagesList.getItem(i).toLowerCase();
						if (it.startsWith(sel.toLowerCase())) {
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
		btnContextSensitive.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		btnContextSensitive.setText("Context Sensitive");
		new Label(container, SWT.NONE);

		btnCheckButton = new Button(container, SWT.CHECK);
		btnCheckButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
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
								Bio7Dialog.message("Update action executed!\nSee console for details!");
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

		allInstalledPackagesList = createInstalledPackagesTable(composite);
		allInstalledPackagesList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		btnCheckSelectedPackageButton = new Button(composite, SWT.CHECK);
		GridData gd_btnCheckSelectedPackageButton = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_btnCheckSelectedPackageButton.heightHint = 40;
		btnCheckSelectedPackageButton.setLayoutData(gd_btnCheckSelectedPackageButton);
		btnCheckSelectedPackageButton.setText("Open Package Description");

		btnUpdate = new Button(composite, SWT.NONE);
		btnUpdate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (RServe.isAliveDialog()) {

					packageInstallUpdateJob();
				}
			}

		});
		GridData gd_btnUpdate = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_btnUpdate.heightHint = 40;
		btnUpdate.setLayoutData(gd_btnUpdate);
		btnUpdate.setText("Refresh Packages List");

		btnUpdateSelectedPackage = new Button(composite, SWT.NONE);
		btnUpdateSelectedPackage.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem[] tableItems = PackageInstallView.getAllInstalledPackagesList().getSelection();
				if (tableItems.length == 0) {
					Bio7Dialog.message("No package(s) selected!");
					return;
				}
				String[] selectePackages = new String[tableItems.length];
				for (int i = 0; i < tableItems.length; i++) {
					selectePackages[i] = tableItems[i].getText(0);
					if (selectePackages[i].equals("Rserve")) {
						Bio7Dialog.message(
								"The update of the Rserve package is disabled for Bio7.\nPlease deselect the Rserve package!");
						return;
					}
				}

				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						RState.setBusy(true);
						UpdateSelectedRPackagesJob Do = new UpdateSelectedRPackagesJob(selectePackages);
						Do.addJobChangeListener(new JobChangeAdapter() {
							public void done(IJobChangeEvent event) {
								if (event.getResult().isOK()) {
									RState.setBusy(false);
									/* Reload the installed packages view. Also running in a job! */
									packageInstallUpdateJob();

									Bio7Dialog.message("Update action executed!\nSee console for details!");
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
			}

		});
		GridData gd_btnUpdateSelectedPackage = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_btnUpdateSelectedPackage.heightHint = 40;
		btnUpdateSelectedPackage.setLayoutData(gd_btnUpdateSelectedPackage);
		btnUpdateSelectedPackage.setText("Update Selected Package");

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
		btnAddLibraryDeclaration.setText("Add Selected To R Editor");

		allInstalledPackagesList.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {

				if (btnCheckSelectedPackageButton.getSelection()) {
					int[] selection = allInstalledPackagesList.getSelectionIndices();
					if (selection.length > 0) {
						String text = allInstalledPackagesList.getItem(selection[0]).getText(0);
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
										if (ApplicationWorkbenchWindowAdvisor.isThemeBlack()) { //

											org.jsoup.nodes.Document document = null;
											try {
												document = Jsoup.connect(packageInfoSite).get();
											} catch (IOException e) {
												// TODO Auto-generated catch block
												e.printStackTrace();
											}

											Elements links = document.select("body");
											links.attr("style", "background: #252525; color: #CCCCCC;");
											Elements a = document.select("a");
											a.attr("style", "background: #252525;color: #FFFFFF;");
											String html = document.html();
											BrowserView b = BrowserView.getBrowserInstance();
											b.browser.setText(html, true);

										} else {
											BrowserView b = BrowserView.getBrowserInstance();
											b.setLocation(packageInfoSite);
										}

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

		composite_1 = new Composite(tabFolder, SWT.NONE);
		packagesTabItem.setControl(composite_1);
		composite_1.setLayout(new GridLayout(2, false));

		sashForm = new SashForm(composite_1, SWT.NONE);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		composite_2 = new Composite(sashForm, SWT.NONE);
		FillLayout fl_composite_2 = new FillLayout(SWT.VERTICAL);
		composite_2.setLayout(fl_composite_2);

		tree = new Tree(composite_2, SWT.BORDER);
		final Menu menuList = new Menu(tree);

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
		new MenuItem(menuList, SWT.SEPARATOR);
		MenuItem showFunctionOption = new MenuItem(menuList, SWT.CHECK);

		showFunctionOption.setText("Selected Package List Functions");
		showFunctionOption.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				if (showFunctionOption.getSelection()) {
					showFunctionOption.setSelection(true);
					showFunc = true;
				}

				else {
					showFunctionOption.setSelection(false);
					showFunc = false;
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		tree.setMenu(menuList);

		tree.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				/* Only consider the left button! */
				if (event.button > 1) {
					return;
				}
				if (showFunc) {

					Point point = new Point(event.x, event.y);
					TreeItem item = tree.getItem(point);
					if (item != null) {

						String packageItem = item.getText();
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							UpdateREnvironmentTableJob Do = new UpdateREnvironmentTableJob(packageItem, table, true);
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

				} else {
					Point point = new Point(event.x, event.y);
					TreeItem item = tree.getItem(point);
					if (item != null) {

						String packageItem = item.getText();
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							UpdateREnvironmentTableJob Do = new UpdateREnvironmentTableJob(packageItem, table, false);
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
				}
			}
		});

		composite_3 = new Composite(sashForm, SWT.NONE);
		sashForm.setWeights(new int[] { 1, 3 });
		packagesTabItem.setControl(composite_1);

		table = createEnvironmentPackagesTable(composite_3);

		table.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				Point point = new Point(e.x, e.y);
				TreeItem item = table.getItem(point);
				if (item != null) {

					String text = item.getText(0);
					/* Check if item exists */
					if (item.getItemCount() >= 1 || item.getParentItem() != null) {
						return;
					}
					if (text.isEmpty() == false) {
						/* Open function body on click! */
						if (showFunc) {
							// RServeUtil.evalR("print(force(" + text + "))", null);
							// RServeUtil.listRObjects();
						}
						/* Load data on click! */
						else {
							// RServeUtil.evalR("try(force(" + text + "))", null);
							String head = "NA";
							String str = "NA";
							int ind = text.indexOf("(");
							if (ind > -1) {
								text = text.substring(0, ind - 1);
							}
							try {
								String evalHead = "tryCatch({class(" + text + ")},error=function(error_message) {return(\"Dataset not available!\")})";
								head = RServeUtil.fromR(evalHead).asString();
								String evalStr = "try(capture.output(tryCatch({str(" + text + ")},error=function(error_message) {return(\"Load data first!\")})))";
								str = RServeUtil.fromR(evalStr).asString();
							} catch (REXPMismatchException e1) {
								// TODO Auto-generated catch block
								
								e1.printStackTrace();
							}

							TreeItem subItem = new TreeItem(item, SWT.NONE);
							subItem.setText(new String[] { head, str });
							item.setExpanded(true);

						}
					}

				}

			}

			public void mouseDoubleClick(final MouseEvent e) {
				// int[] selection = table.getSelectionIndices();
				Point point = new Point(e.x, e.y);
				TreeItem item = table.getItem(point);
				if (item != null) {

					String text = item.getText(0);
					if (text.isEmpty() == false) {
						/* Open function body on click! */
						if (showFunc) {
							RServeUtil.evalR("print(force(" + text + "))", null);
							// RServeUtil.listRObjects();
						}
						/* Load data on click! */
						else {
							if (item.getParentItem() != null) {
								/* Load the data also from an expanded tree item! */
								text = item.getParentItem().getText();

							}

							int ind = text.indexOf("(");
							if (ind > -1) {

								text = text.substring(0, ind - 1);
							}
							RServeUtil.evalR("try(data(" + text + "))", null);

							RServeUtil.evalR("try(print(summary(" + text + ")))", null);

							RServeUtil.listRObjects();
						}
					}

				}
			}
		});

		btnNewButton = new Button(composite_1, SWT.NONE);
		btnNewButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				createAttachedPackageTree();
			}
		});
		btnNewButton.setText("Refresh Packages List");

		map.clear();
		map.put("spatstat",
				new String[] {
						"A package for the statistical analysis of spatial data, \n mainly spatial point patterns.",
						"https://www.spatstat.org" });
		map.put("raster",
				new String[] {
						"A package for reading, writing, manipulating, analyzing and modeling of gridded spatial data.",
						"https://cran.r-project.org/web/packages/raster/index.html" });
		map.put("maptools", new String[] { "Tools for reading and handling spatial objects",
				"https://cran.r-project.org/web/packages/maptools/index.html" });
		map.put("gstat", new String[] {
				"Gstat is an open source (GPL) computer code for multivariable \n geostatistical modelling, prediction and simulation",
				"https://www.gstat.org/" });
		map.put("maps", new String[] {
				"Display of maps. Projection code and larger maps are in separate packages (mapproj and mapdata).",
				"https://cran.r-project.org/web/packages/maps/index.html" });
		map.put("rgdal", new String[] {
				"Provides bindings to Frank Warmerdam's Geospatial Data Abstraction Library (GDAL) (>= 1.3.1)\n and access to projection/transformation operations from the PROJ.4 library.",
				"https://cran.r-project.org/web/packages/rgdal/index.html" });
		map.put("PBSmapping", new String[] {
				"This software has evolved from fisheries research conducted at the Pacific Biological Station (PBS) in Nanaimo, British Columbia, Canada.\n It extends the R language to include two-dimensional plotting features similar to those commonly available in a Geographic Information System (GIS).",
				"https://cran.r-project.org/web/packages/PBSmapping/index.html" });
		map.put("shapefiles", new String[] { "Functions to read and write ESRI shapefiles.",
				"https://cran.r-project.org/web/packages/shapefiles/index.html" });
		map.put("RSAGA", new String[] {
				"RSAGA provides access to geocomputing and terrain analysis functions of SAGA from within R\n by running the command line version of SAGA.",
				"https://cran.r-project.org/web/packages/RSAGA/index.html" });
		map.put("geoR",
				new String[] { "Geostatistical analysis including traditional, likelihood-based and Bayesian methods.",
						"https://leg.ufpr.br/geoR/" });
		map.put("geoRglm", new String[] {
				"Functions for inference in generalised linear spatial models. \nThe posterior and predictive inference is based on Markov chain Monte Carlo methods.",
				"https://gbi.agrsci.dk/~ofch/geoRglm/" });
		map.put("SDMTools", new String[] {
				"This packages provides a set of tools for post processing the outcomes\nof species distribution modeling exercises. It includes novel methods for comparing models\nand tracking changes in distributions through time.\nIt further includes methods for visualizing outcomes, selecting thresholds, calculating measures\nof accuracy and landscape fragmentation statistics, etc.",
				"https://cran.r-project.org/web/packages/SDMTools/" });

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
	/*
	 * private void initializeToolBar() { IToolBarManager toolbarManager =
	 * getViewSite().getActionBars().getToolBarManager(); }
	 */

	/**
	 * Initialize the menu.
	 */
	private void initializeMenu() {
		//IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
	}

	@Override
	public void setFocus() {
		// Set the focus
	}

	public static List getAllList() {
		return allPackagesList;
	}

	public void packageInstallUpdateJob() {
		if (RServe.isAlive()) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Job job = new Job("Update R Package View") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						monitor.beginTask("Update R Package View ...", IProgressMonitor.UNKNOWN);

						packageInstallUpdate();

						monitor.done();
						return Status.OK_STATUS;
					}

				};
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
				// job.setSystem(true);
				job.schedule();
			} else {
				Bio7Dialog.message("Rserve is busy!");
			}
		}
	}

	public void packageInstallUpdate() {

		RConnection c = REditor.getRserveConnection();
		if (c != null) {
			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			String server = store.getString("R_PACKAGE_SERVER");
			try {

				/*
				 * Here we get information about the installed packages, versions and new
				 * available repo versions! First we create a dataframe (will be sorted at the
				 * end) with a fixed number of rows (n) which we fill with the information
				 * extracted from the current installed packages and the latest online versions!
				 */

				out = c.eval("try(.calculate_version_update<-function(){\n" + "  new_versions <- old.packages(repos =\""
						+ server + "\")\n" + "  new_versions_repo_vers <- new_versions[, \"ReposVer\"]\n"
						+ "  installed_packages <- installed.packages()\n"
						+ "  installed_package_names <- as.character(installed_packages[, \"Package\"])\n"
						+ "  version_installed_package_names <- as.character(installed_packages[, \"Version\"])\n"
						+ "  n<-length(installed_package_names)\n"
						+ "  df_packages <- data.frame(Name = character(n), Current_Version = character(n), Latest_Version = character(n), \n"
						+ "      stringsAsFactors = FALSE)\n" + "  for (x in 1:length(installed_package_names)) {\n"
						+ "      if (isTRUE(installed_package_names[x] %in% new_versions[, \"Package\"])) {\n"
						+ "       \n" + "          df_packages$Name[x] <- installed_package_names[x]\n"
						+ "          df_packages$Current_Version[x] <- version_installed_package_names[x]\n"
						+ "          df_packages$Latest_Version[x] <- new_versions_repo_vers[installed_package_names[x]]\n"
						+ "          \n" + "      } else {\n" + "         \n"
						+ "          df_packages$Name[x] <- installed_package_names[x]\n"
						+ "          df_packages$Current_Version[x] <- version_installed_package_names[x]\n"
						+ "          df_packages$Latest_Version[x] <- version_installed_package_names[x]\n"
						+ "      }\n" + "  }\n" + "df_packages<-df_packages[order(df_packages$Name),]\n"
						+ "  return(df_packages)\n" + "})\n" + "try(.calculate_version_update())").asList();

			} catch (REXPMismatchException e) {

				e.printStackTrace();
			} catch (RserveException e) {

				e.printStackTrace();
			}
		}

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				/*
				 * Store the selected top index of the table to scroll to it after an update of
				 * the table!
				 */
				int tempIndex = allInstalledPackagesList.getTopIndex();
				int[] selIndex = allInstalledPackagesList.getSelectionIndices();

				allInstalledPackagesList.setVisible(false);

				String[] packageNames = null;
				String[] currentVersions = null;
				String[] latestVersions = null;
				/* Get the dataframe columns as string lists! */
				try {
					packageNames = out.at(0).asStrings();
					currentVersions = out.at(1).asStrings();
					latestVersions = out.at(2).asStrings();
				} catch (REXPMismatchException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (allInstalledPackagesList != null) {
					if (allInstalledPackagesList.isDisposed() == false) {
						allInstalledPackagesList.removeAll();
						for (int i = 0; i < packageNames.length; i++) {

							// panel.setLine(i, markedVariables[i]);
							TableItem it = new TableItem(allInstalledPackagesList, SWT.NONE);
							it.setText(new String[] { packageNames[i], currentVersions[i], latestVersions[i] });
							if (currentVersions[i].equals(latestVersions[i]) == false) {
								if (Util.isThemeBlack()) {
									it.setBackground(Util.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
								} else {
									it.setBackground(Util.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
								}
							}

						}
					}

				}

				allInstalledPackagesList.setVisible(true);
				allInstalledPackagesList.setSelection(selIndex);
				allInstalledPackagesList.setTopIndex(tempIndex);

				packageNames = null;
				currentVersions = null;
				latestVersions = null;
				out = null;
			}
		});

	}

	private void setInDocument(Table aList) {
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		if (editor != null && editor instanceof REditor) {
			TableItem[] tableSelection = aList.getSelection();
			String[] items = new String[aList.getSelection().length];
			for (int i = 0; i < tableSelection.length; i++) {
				items[i] = tableSelection[i].getText(0);
			}
			// String[] items = aList.getSelection();
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

	public static Table getAllInstalledPackagesList() {
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
		if (pack == null) {
			return;
		}
		try {

			v = pack.asStrings();
		} catch (REXPMismatchException e1) {

			e1.printStackTrace();
		}
		RServeUtil.evalR("try(rm(workspaceRPackages,envir=.bio7TempVarEnvironment))", null);

		TreeItem packages = new TreeItem(tree, 0);
		packages.setText("Attached:");
		// packages.removeAll();
		for (int i = 0; i < v.length; i++) {

			// lsf.str("package:MASS")
			TreeItem treeItem = new TreeItem(packages, 0);
			treeItem.setText(v[i]);

		}
		packages.setExpanded(true);

	}

	public Table createInstalledPackagesTable(Composite parent) {
		Table grid = new Table(parent,
				SWT.BORDER | SWT.V_SCROLL | SWT.SCROLL_LINE | SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);

		{
			column1 = new TableColumn(grid, SWT.CENTER);
			column1.setText("Library");
			column1.setToolTipText(
					"Use double-click to load libraries and update the code completion API information!");
			column1.setWidth(100);
		}
		{
			column2 = new TableColumn(grid, SWT.CENTER);
			column2.setText("Installed Version");
			column2.setToolTipText(
					"Use double-click to load libraries and update the code completion API information!");
			column2.setWidth(100);
		}

		{
			column3 = new TableColumn(grid, SWT.CENTER);
			column3.setText("Latest Version");
			column3.setToolTipText(
					"Use double-click to load libraries and update the code completion API information!");
			column3.setWidth(100);
		}

		grid.setHeaderVisible(true);
		grid.setLinesVisible(true);

		// Show row header

		final TableEditor editor = new TableEditor(grid);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;

		// createActions();
		// initializeToolBar();
		// initializeMenu();

		/* Resize column width if shell changes! */
		parent.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				if (grid.isDisposed() == false) {
					Rectangle area = parent.getClientArea();
					Point preferredSize = grid.computeSize(SWT.DEFAULT, SWT.DEFAULT);
					int width = area.width - 2 * grid.getBorderWidth();
					if (preferredSize.y > area.height + grid.getHeaderHeight()) {
						// Subtract the scrollbar width from the total column width
						// if a vertical scrollbar will be required
						Point vBarSize = grid.getVerticalBar().getSize();
						width -= vBarSize.x;
					}
					Point oldSize = grid.getSize();
					if (oldSize.x > area.width) {
						// table is getting smaller so make the columns
						// smaller first and then resize the table to
						// match the client area width
						column1.setWidth(width / 3);
						column2.setWidth(width / 3);
						column3.setWidth(width / 3);

						grid.setSize(area.width, area.height);
					} else {
						// table is getting bigger so make the table
						// bigger first and then make the columns wider
						// to match the client area width
						grid.setSize(area.width, area.height);
						column1.setWidth(width / 3);
						column2.setWidth(width / 3);
						column3.setWidth(width / 3);

					}
				}
				parent.layout();
			}
		});
		return grid;
	}

	public Tree createEnvironmentPackagesTable(Composite parent) {
		GridLayout gl_composite_3 = new GridLayout(1, true);
		gl_composite_3.marginWidth = 0;
		gl_composite_3.marginHeight = 0;
		parent.setLayout(gl_composite_3);
		Tree grid = new Tree(parent,
				SWT.BORDER | SWT.V_SCROLL | SWT.SCROLL_LINE | SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL);
		grid.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

		{
			columnEnvironment1 = new TreeColumn(grid, SWT.LEFT);
			columnEnvironment1.setText("Dataset");
			// columnEnvironment1.setToolTipText("Use double-click to load libraries and
			// update the code completion API information!");
			columnEnvironment1.setWidth(100);
		}
		{
			columnEnvironment2 = new TreeColumn(grid, SWT.LEFT);
			columnEnvironment2.setText("Dataset Description");
			// columnEnvironment2.setToolTipText("Use double-click to load libraries and
			// update the code completion API information!");
			columnEnvironment2.setWidth(100);
		}

		/*
		 * { columnEnvironment3 = new TableColumn(grid, SWT.CENTER);
		 * columnEnvironment3.setText(""); // columnEnvironment3.setToolTipText("Use
		 * double-click to load libraries and // update the code completion API
		 * information!"); columnEnvironment3.setWidth(100); }
		 */

		grid.setHeaderVisible(true);
		grid.setLinesVisible(true);

		// Show row header

		final TreeEditor editor = new TreeEditor(grid);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;

		// createActions();
		// initializeToolBar();
		// initializeMenu();

		/* Resize column width if shell changes! */
		parent.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				if (grid.isDisposed() == false) {
					Rectangle area = parent.getClientArea();
					Point preferredSize = grid.computeSize(SWT.DEFAULT, SWT.DEFAULT);
					int width = area.width - 2 * grid.getBorderWidth();
					if (preferredSize.y > area.height + grid.getHeaderHeight()) {
						// Subtract the scrollbar width from the total column width
						// if a vertical scrollbar will be required
						Point vBarSize = grid.getVerticalBar().getSize();
						width -= vBarSize.x;
					}
					Point oldSize = grid.getSize();
					if (oldSize.x > area.width) {
						// table is getting smaller so make the columns
						// smaller first and then resize the table to
						// match the client area width
						columnEnvironment1.setWidth(width / 2);
						columnEnvironment2.setWidth(width / 2);
						// columnEnvironment3.setWidth(width / 3);

						grid.setSize(area.width, area.height);
					} else {
						// table is getting bigger so make the table
						// bigger first and then make the columns wider
						// to match the client area width
						grid.setSize(area.width, area.height);
						columnEnvironment1.setWidth(width / 2);
						columnEnvironment2.setWidth(width / 2);
						// columnEnvironment3.setWidth(width / 3);

					}
				}
				parent.layout();
			}
		});
		return grid;
	}

}
