/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rbridge;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.views.RShellView;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.util.Util;

public class RLibraryList extends Shell {

	private static List allPackagesList;

	public RLibraryList(Display display, int style) {
		super(display, style);

		// setImage(SWTResourceManager.getImage(RLibraryList.class,
		// "/pics/logo.gif"));
		/* Reparse the document! */
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		allPackagesList = new List(this, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		allPackagesList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 4));

		allPackagesList.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent e) {
				if (RServe.isAlive()) {
					if (RState.isBusy() == false) {
						RState.setBusy(true);
						LoadRLibrarysJob Do = new LoadRLibrarysJob(editor);
						Do.addJobChangeListener(new JobChangeAdapter() {
							public void done(IJobChangeEvent event) {
								if (event.getResult().isOK()) {
									RState.setBusy(false);
									/*Also reload the R-Shell code completion in an extra job!*/
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
		allPackagesList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

			}
		});

		final Button uninstallButton = new Button(this, SWT.NONE);
		uninstallButton.setToolTipText("Removes installed packages/bundles and updates index information as necessary. ");
		GridData gd_uninstallButton = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_uninstallButton.heightHint = 40;
		uninstallButton.setLayoutData(gd_uninstallButton);
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
		uninstallButton.setText("Remove");
		createContents();
	}

	/**
	 * Create contents of the window
	 */
	protected void createContents() {
		setText("Libraries");
		setSize(323, 743);
		setLayout(new GridLayout(2, true));

		Button btnAddLibraryDeclaration = new Button(this, SWT.NONE);
		btnAddLibraryDeclaration.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				setInDocument(allPackagesList);
			}
		});
		btnAddLibraryDeclaration.setToolTipText("Add selected package items as library declaration to R editor source");
		GridData gd_btnAddLibraryDeclaration = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_btnAddLibraryDeclaration.widthHint = 301;
		gd_btnAddLibraryDeclaration.heightHint = 40;
		btnAddLibraryDeclaration.setLayoutData(gd_btnAddLibraryDeclaration);
		btnAddLibraryDeclaration.setText("Add selected to R editor");

		RConnection c = REditor.getRserveConnection();
		if (c != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Display display = Util.getDisplay();
				display.syncExec(() -> {

					if (c != null) {
						String[] listPackages = null;
						try {
							c.eval("try(.bio7ListOfWebPackages <- list(sort(.packages(all.available = TRUE))))");

							try {
								listPackages = c.eval(".bio7ListOfWebPackages[[1]]").asStrings();
							} catch (REXPMismatchException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}

						} catch (RserveException e2) {

							e2.printStackTrace();
						}

						allPackagesList.setItems(listPackages);

					}

				});
				RState.setBusy(false);
			} else {
				System.out.println("Rserve is busy!");
			}
		}

		else {
			System.out.println("No Rserve connection available!");
		}

		//
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

	public static List getAllList() {
		return allPackagesList;
	}

	public static List getAllPackagesList() {
		return allPackagesList;
	}

	private void setInDocument(List aList) {
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
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

}
