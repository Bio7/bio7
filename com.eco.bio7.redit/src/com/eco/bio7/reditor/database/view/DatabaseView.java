/*******************************************************************************
 * Copyright (c) 2005-2017 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.reditor.database.view;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.osgi.framework.Bundle;

import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.util.Bio7Dialog;
import com.eco.bio7.util.Util;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class DatabaseView extends ViewPart {

	public static final String ID = "com.eco.bio7.reditor.database.view.DatabaseView"; //$NON-NLS-1$
	private String finalTemplate;
	private Text userTextfield;
	private Text databaseTextfield;
	private Text passwordTextfield;
	private Text serverTextfield;
	private StyledText previewTextfield;
	protected String dir;
	private Combo combo;
	private Spinner spinner;
	protected ConnectionProfiles profilesXML;
	private Text textDisconnect;
	private List comboProfiles;
	private Text textSshHost;
	private Spinner spinnerSshPort;
	private Text textSshCustomArgs;
	private Text textSshUsername;
	private Spinner spinnerTimeout;
	private FileChooser textSshPrivateKey;
	private Spinner spinnerLocalPort;
	private Button btnUseSshTunnel;
	private XStream xs;
	protected String[] res;
	private DatabaseMethods methods;
	private String textPlink;

	public DatabaseView() {
		methods = new DatabaseMethods();
		xs = new XStream(new DomDriver());
		xs.allowTypesByRegExp(new String[] { ".*" });
		xs.setClassLoader(com.eco.bio7.reditor.database.view.ConnectionProfiles.class.getClassLoader());
	}

	/**
	 * Create contents of the view part.
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		/*
		 * Listener listenerProfile = new Listener() {
		 * 
		 * @Override public void handleEvent(Event event) { comboProfiles.removeAll();
		 * 
		 * File fileStartupScripts = getProfilesFile();
		 * 
		 * Read the stored profiles! try { profilesXML = (ConnectionProfiles)
		 * xs.fromXML(fileStartupScripts); } catch (Exception e1) { // TODO
		 * Auto-generated catch block e1.printStackTrace(); } if (profilesXML != null) {
		 * Iterator<ConnectionProfile> iterator = profilesXML.profiles.iterator(); while
		 * (iterator.hasNext()) { ConnectionProfile prof = iterator.next(); If we have
		 * already a profile with that same!
		 * 
		 * comboProfiles.add(prof.getProfileName());
		 * 
		 * } }
		 * 
		 * } };
		 */
		/* Link the ssh.exe file if we are under Windows! */
		if (Util.getOS().equals("Windows")) {
			String sshPath = getSshWindowsPath();
			textPlink = sshPath;
		}

		CTabFolder tabFolder = new CTabFolder(parent, SWT.BORDER);
		tabFolder.setSelectionBackground(
				Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));

		CTabItem tbtmNewItem_1 = new CTabItem(tabFolder, SWT.NONE);
		tbtmNewItem_1.setText("Connection");
		Composite containerConnection = new Composite(tabFolder, SWT.NONE);
		tbtmNewItem_1.setControl(containerConnection);
		tabFolder.setSelection(tbtmNewItem_1);
		GridLayout gl_containerConnection = new GridLayout(3, true);
		containerConnection.setLayout(gl_containerConnection);
		tabFolder.setSelection(tbtmNewItem_1);
		CTabItem tbtmNewItem = new CTabItem(tabFolder, SWT.NONE);
		tbtmNewItem.setText("Database");
		Composite container = new Composite(tabFolder, SWT.NONE);
		tbtmNewItem.setControl(container);
		container.setLayout(new GridLayout(2, false));

		Label lblStoredProfiles = new Label(containerConnection, SWT.NONE);
		lblStoredProfiles.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, 3, 1));
		lblStoredProfiles.setText("Stored Profiles:");

		comboProfiles = new List(containerConnection, SWT.BORDER | SWT.V_SCROLL);
		comboProfiles.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				int selected = comboProfiles.getSelectionIndex();
				if (selected <0) {
					return;
				}
				String sel = comboProfiles.getItem(selected);
				File fileStartupScripts = getProfilesFile();
				/* Read the stored profiles! */
				try {
					profilesXML = (ConnectionProfiles) xs.fromXML(fileStartupScripts);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (profilesXML != null) {
					Iterator<ConnectionProfile> iterator = profilesXML.profiles.iterator();
					while (iterator.hasNext()) {
						ConnectionProfile prof = iterator.next();
						/* If we have already a profile with that same! */
						if (sel.equals(prof.getProfileName())) {
							/*
							 * First load all available OS profiles to the combobox to check if we have an
							 * available driver We want to avoid an error if the profiles have been shared
							 * on another system!
							 */
							combo.removeAll();

							/*
							 * String[] drivers = methods.rOdbcdrivers(); if (drivers != null) { for (int i
							 * = 0; i < drivers.length; i++) { combo.add(drivers[i], i); } }
							 */
							combo.add(prof.getOdbcDriver());
							combo.getParent().layout();
							// combo.setListVisible(true);

							// Now check if we have a stored profile which matches a OS database driver!
							int countItem = combo.getItemCount();
							for (int i = 0; i < countItem; i++) {
								String it = combo.getItem(i);
								if (it.equals(prof.getOdbcDriver())) {
									combo.select(i);
									break;
								}
							}

							serverTextfield.setText(prof.getServer());
							databaseTextfield.setText(prof.getDatabase());
							spinner.setSelection(prof.getPort());
							userTextfield.setText(prof.getUser());
							passwordTextfield.setText(prof.getPassword());
							textDisconnect.setText(prof.getCustomDisconnect());

							/* SSH connection! */
							btnUseSshTunnel.setSelection(prof.isSshEnabled());
							// textPlink.setText(prof.getSshplinkExePath());
							textSshHost.setText(prof.getSshSshHost());
							spinnerSshPort.setSelection(prof.getSshPort());
							textSshCustomArgs.setText(prof.getSshCustomArgs());
							textSshUsername.setText(prof.getSshUsername());
							spinnerTimeout.setSelection(prof.getSshTimeout());
							textSshPrivateKey.setText(prof.getSshPrivateKey());
							spinnerLocalPort.setSelection(prof.getLocalSshPort());

						}

					}

				}
				previewTextfield.setText(createTemplate(true));
			}
		});

		// comboProfiles.addListener(SWT.MouseDown, listenerProfile);
		comboProfiles.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		GridData gd_comboProfiles = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		gd_comboProfiles.heightHint = 177;
		comboProfiles.setLayoutData(gd_comboProfiles);
		// comboProfiles.setText("Please select a profile...");
		/* Read the stored profiles! */
		File fileStartupScripts = getProfilesFile();
		try {
			profilesXML = (ConnectionProfiles) xs.fromXML(fileStartupScripts);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		if (profilesXML != null) {
			Iterator<ConnectionProfile> iterator = profilesXML.profiles.iterator();
			while (iterator.hasNext()) {
				ConnectionProfile prof = iterator.next();
				/* If we have already a profile with that same! */

				comboProfiles.add(prof.getProfileName());

			}
		}

		Composite composite_2 = new Composite(containerConnection, SWT.NONE);
		GridLayout gl_composite_2 = new GridLayout(2, true);
		gl_composite_2.marginWidth = 0;
		composite_2.setLayout(gl_composite_2);
		composite_2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));

		Button btnAddConnectionAs = new Button(composite_2, SWT.NONE);
		btnAddConnectionAs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		btnAddConnectionAs.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				ConnectionProfiles profiles = new ConnectionProfiles();
				String profileName = null;
				InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(), "Connection Profile",
						"Enter a profile name", "MySQL Connection", null);
				if (dlg.open() == Window.OK) {
					// User clicked OK; update the label with the input

					if (dlg.getValue() != null) {
						profileName = dlg.getValue();
					} else {
						Bio7Dialog.message("Please enter a valid profile name!");
						return;
					}
				} else {
					return;
				}
				File fileStartupScripts = getProfilesFile();
				// comboProfiles.removeAll();
				/* Read the stored profiles! */
				try {
					profilesXML = (ConnectionProfiles) xs.fromXML(fileStartupScripts);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (profilesXML != null) {

					Iterator<ConnectionProfile> iterator = profilesXML.profiles.iterator();
					while (iterator.hasNext()) {
						ConnectionProfile prof = iterator.next();
						/* If we have already a profile with that same! */
						if (prof.getProfileName().equals(profileName)) {
							Bio7Dialog.message("Profile with same name found!");
							return;
						} else {
							/* Load all stored profiles! */

							profiles.addProfile(prof);
							// comboProfiles.add(profileName);
						}
					}
				}
				/* Add a new profile to the stored profiles! */
				ConnectionProfile prof = new ConnectionProfile();
				prof.setProfileName(profileName);
				prof.setOdbcDriver(combo.getText());
				prof.setServer(serverTextfield.getText());
				prof.setDatabase(databaseTextfield.getText());
				prof.setPort(spinner.getSelection());
				prof.setUser(userTextfield.getText());				
				prof.setPassword(passwordTextfield.getText());				
				prof.setCustomDisconnect(textDisconnect.getText());

				/* SSH connection! */
				prof.setSshEnabled(btnUseSshTunnel.getSelection());
				// prof.setSshplinkExePath(textPlink);
				prof.setSshSshHost(textSshHost.getText());
				prof.setSshPort(spinnerSshPort.getSelection());
				prof.setSshCustomArgs(textSshCustomArgs.getText());
				prof.setSshUsername(textSshUsername.getText());
				prof.setSshTimeout(spinnerTimeout.getSelection());
				prof.setSshPrivateKey(textSshPrivateKey.getText());
				prof.setLocalSshPort(spinnerLocalPort.getSelection());

				profiles.addProfile(prof);

				FileWriter fs = null;
				try {
					fs = new FileWriter(fileStartupScripts);
				} catch (IOException e2) {

					e2.printStackTrace();
				}
				xs.toXML(profiles, fs);
				/* Rserve has to be alive to create encrypted credentials! */
				/*if (btnCreateEncryptedPwd.getSelection()) {

					createEncryptedCredentials();

				}*/
				/* Add profile to the combobox! */
				comboProfiles.add(profileName);

				Bio7Dialog.message("Profile stored!");
			}

		});
		btnAddConnectionAs.setText("Add Connection Profile");

		Button btnNewButton = new Button(composite_2, SWT.NONE);
		btnNewButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (comboProfiles.getItemCount() == 1) {
					Bio7Dialog.message("At least one profile has to be available!");
					return;
				}
				String sel = comboProfiles.getItem(comboProfiles.getSelectionIndex());
				if (sel == null) {
					Bio7Dialog.message("No profile selected!");
					return;
				}
				boolean dec = Bio7Dialog.decision("Do you really want to delete the profile?");
				/*
				 * We load all profiles and then save again the profiles without the for
				 * deletion selected!
				 */
				if (dec) {
					ConnectionProfiles profiles = new ConnectionProfiles();
					File fileStartupScripts = getProfilesFile();

					/* Read the stored profiles! */
					try {
						profilesXML = (ConnectionProfiles) xs.fromXML(fileStartupScripts);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					if (profilesXML != null) {
						// comboProfiles.removeAll();
						Iterator<ConnectionProfile> iterator = profilesXML.profiles.iterator();
						while (iterator.hasNext()) {
							ConnectionProfile prof = iterator.next();
							/* If we have already a profile with that same! */

							/* Omit the current selected active profile (stored profiles)! */
							if (sel.equals(prof.getProfileName()) == false) {
								profiles.addProfile(prof);
								/* Add profile to the combobox! */
								// comboProfiles.add(prof.getProfileName());
							}

						}
					}
					/* Store the remaining profiles! */
					FileWriter fs = null;
					try {
						fs = new FileWriter(fileStartupScripts);
					} catch (IOException e2) {

						e2.printStackTrace();
					}
					xs.toXML(profiles, fs);
					// comboProfiles.removeAll();
					// comboProfiles.setText("Please select a profile...");
					comboProfiles.remove(sel);
				}

			}
		});
		btnNewButton.setText("Delete Stored Profile");
		new Label(container, SWT.NONE);

		Button btnAddAvailableOdbc = new Button(container, SWT.NONE);
		btnAddAvailableOdbc.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				combo.removeAll();

				String[] drivers = methods.rOdbcdrivers();
				if (drivers != null) {
					for (int i = 0; i < drivers.length; i++) {
						combo.add(drivers[i], i);
					}

					combo.getParent().layout();
					combo.setListVisible(true);
				}
			}
		});
		btnAddAvailableOdbc.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		btnAddAvailableOdbc.setText("List ODBC Drivers");

		Label lblDriver = new Label(container, SWT.NONE);
		lblDriver.setText("ODBC Driver:");

		combo = new Combo(container, SWT.READ_ONLY);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				previewTextfield.setText(createTemplate(true));

			}
		});

		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		// combo.addListener(SWT.MouseDown, listener);

		btnUseSshTunnel = new Button(containerConnection, SWT.CHECK);
		btnUseSshTunnel.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false, 1, 1));
		btnUseSshTunnel.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				if (previewTextfield != null) {
					previewTextfield.setText(createTemplate(true));
				}
			}
		});
		btnUseSshTunnel.setToolTipText("Configure the SSH Tunnel tab!");
		btnUseSshTunnel.setText("Use SSH Tunnel");
		new Label(containerConnection, SWT.NONE);
		new Label(containerConnection, SWT.NONE);
		new Label(containerConnection, SWT.NONE);

		Label lblPreview = new Label(containerConnection, SWT.NONE);
		lblPreview.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		lblPreview.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));
		lblPreview.setText("Preview:");
		new Label(containerConnection, SWT.NONE);

		Label lblType = new Label(container, SWT.NONE);
		lblType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		lblType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		lblType.setText("Server:");

		previewTextfield = new StyledText(containerConnection,
				SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		previewTextfield.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 1, 2));
		GridData gd_previewTextfield = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		gd_previewTextfield.widthHint = 160;
		gd_previewTextfield.heightHint = 91;
		previewTextfield.setLayoutData(gd_previewTextfield);

		Button btnInsertInEditor = new Button(containerConnection, SWT.NONE);
		btnInsertInEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		btnInsertInEditor.setToolTipText("Inserts the preview code in the active editor!");
		btnInsertInEditor.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				okPressed();
			}
		});
		btnInsertInEditor.setText("Insert in Editor");

		serverTextfield = new Text(container, SWT.BORDER);
		serverTextfield.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		serverTextfield.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				// channel <- odbcConnect("test", uid="ripley", pwd="secret")
				if (previewTextfield != null) {
					previewTextfield.setText(createTemplate(true));
				}
			}
		});
		serverTextfield.setText("localhost");
		serverTextfield.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label dataSource = new Label(container, SWT.NONE);
		dataSource.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		dataSource.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		dataSource.setText("Database:");

		databaseTextfield = new Text(container, SWT.BORDER);
		databaseTextfield.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		databaseTextfield.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (previewTextfield != null) {
					previewTextfield.setText(createTemplate(true));
				}
			}
		});
		databaseTextfield.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblPort = new Label(container, SWT.NONE);
		lblPort.setText("Port:");

		spinner = new Spinner(container, SWT.BORDER);
		spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		spinner.setMaximum(65535);
		spinner.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				previewTextfield.setText(createTemplate(true));
			}
		});
		spinner.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent event) {
				previewTextfield.setText(createTemplate(true));
			}
		});
		spinner.setTextLimit(5);
		spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setText("User:\r\n");

		userTextfield = new Text(container, SWT.BORDER);
		userTextfield.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		userTextfield.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (previewTextfield != null) {
					previewTextfield.setText(createTemplate(true));
				}
			}
		});
		userTextfield.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblPassword = new Label(container, SWT.NONE);
		lblPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		lblPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		lblPassword.setText("Password:");

		passwordTextfield = new Text(container, SWT.BORDER | SWT.PASSWORD);
		passwordTextfield.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		passwordTextfield.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (previewTextfield != null) {
					previewTextfield.setText(createTemplate(true));
				}
			}
		});
		passwordTextfield.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Composite composite = new Composite(containerConnection, SWT.NONE);
		GridLayout gl_composite = new GridLayout(2, true);
		gl_composite.marginWidth = 0;
		composite.setLayout(gl_composite);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));

		Button btnNewButton_1 = new Button(composite, SWT.NONE);
		btnNewButton_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		btnNewButton_1.setToolTipText("Creates a connection to the database!");
		btnNewButton_1.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				/*
				 * if (btnCreateEncryptedPwd.getSelection()) { Write the encrypted files for the
				 * test if enabled! createEncryptedCredentials(); }
				 */
				/* Insert custom connection details (e.g., SSH) before database connection! */

				String testRCode = createTemplate(false);

				String tryCatch;
				
					tryCatch = "out <- tryCatch({\n" + testRCode + "  }, \n" + "  warning = function(w) {\n"
							+ "    message(\"Warning message:\")\n" + "    message(w)\n" + "  }, \n"
							+ "  error = function(e) {\n" + "    message(\"Error message:\")\n" + "    message(e)\n"
							+ "  }, \n" + "  finally = {\n" + "    #cleanup\n" + "})";

				

				methods.evaluateInPrintJob(tryCatch);

			}
		});
		btnNewButton_1.setText("Connect");

		Button btnNewButton_2 = new Button(composite, SWT.NONE);
		btnNewButton_2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				// con.eval("try(dbDisconnect(con))");
				String disconnectCustom = textDisconnect.getText();// Replace;

				methods.evaluateInMultiplePrintJobs(disconnectCustom);

			}
		});
		btnNewButton_2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		btnNewButton_2.setText("Disconnect");
		new Label(containerConnection, SWT.NONE);

		Label lblDisconnectCode = new Label(containerConnection, SWT.NONE);
		lblDisconnectCode.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1));
		lblDisconnectCode.setText("Custom Disconnect Code:");
		new Label(containerConnection, SWT.NONE);

		textDisconnect = new Text(containerConnection, SWT.BORDER);
		textDisconnect.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {

				// previewTextfield.setText(createTemplate(true));
			}
		});
		GridData gd_textDisconnect = new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1);
		gd_textDisconnect.heightHint = 22;
		textDisconnect.setLayoutData(gd_textDisconnect);

		CTabItem tbtmSshTunnel = new CTabItem(tabFolder, SWT.NONE);
		tbtmSshTunnel.setText("SSH Tunnel");

		Composite composite_1 = new Composite(tabFolder, SWT.NONE);
		tbtmSshTunnel.setControl(composite_1);
		GridLayout gl_composite_1 = new GridLayout(3, false);
		composite_1.setLayout(gl_composite_1);

		new Label(composite_1, SWT.NONE);

		Label lblNewLabel_2 = new Label(composite_1, SWT.NONE);
		lblNewLabel_2.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		lblNewLabel_2.setText("SSH Connection");
		new Label(composite_1, SWT.NONE);

		Label lblSshHost = new Label(composite_1, SWT.NONE);
		lblSshHost.setText("SSH Host + Port:");

		textSshHost = new Text(composite_1, SWT.BORDER);
		textSshHost.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (previewTextfield != null) {
					previewTextfield.setText(createTemplate(true));
				}
			}
		});
		GridData gd_textSshHost = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_textSshHost.widthHint = 207;
		gd_textSshHost.heightHint = 20;
		textSshHost.setLayoutData(gd_textSshHost);

		spinnerSshPort = new Spinner(composite_1, SWT.BORDER);
		spinnerSshPort.setMaximum(65535);
		spinnerSshPort.setMinimum(1);
		spinnerSshPort.setSelection(22);
		spinnerSshPort.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (previewTextfield != null) {
					previewTextfield.setText(createTemplate(true));
				}
			}
		});
		spinnerSshPort.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (previewTextfield != null) {
					previewTextfield.setText(createTemplate(true));
				}
			}
		});
		GridData gd_spinnerSshPort = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_spinnerSshPort.heightHint = 20;
		spinnerSshPort.setLayoutData(gd_spinnerSshPort);

		Label lblUsername = new Label(composite_1, SWT.NONE);
		lblUsername.setText("Username:");

		textSshUsername = new Text(composite_1, SWT.BORDER);
		textSshUsername.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (previewTextfield != null) {
					previewTextfield.setText(createTemplate(true));
				}
			}
		});
		GridData gd_textSshUsername = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_textSshUsername.heightHint = 20;
		textSshUsername.setLayoutData(gd_textSshUsername);

		Label lblTimeout = new Label(composite_1, SWT.NONE);
		lblTimeout.setText("Sleep:");

		spinnerTimeout = new Spinner(composite_1, SWT.BORDER);
		spinnerTimeout.setMaximum(100000);
		spinnerTimeout.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (previewTextfield != null) {
					previewTextfield.setText(createTemplate(true));
				}
			}
		});
		spinnerTimeout.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (previewTextfield != null) {
					previewTextfield.setText(createTemplate(true));
				}
			}
		});
		spinnerTimeout.setSelection(20);
		GridData gd_spinnerTimeout = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_spinnerTimeout.heightHint = 20;
		spinnerTimeout.setLayoutData(gd_spinnerTimeout);
		new Label(composite_1, SWT.NONE);

		Label lblPrivateKeyFile = new Label(composite_1, SWT.NONE);
		lblPrivateKeyFile.setText("Private Key file:");

		textSshPrivateKey = new FileChooser(composite_1);
		textSshPrivateKey.getTextControl().setToolTipText(
				"On Linux and Mac. If the file location is not given the default \r\nprivate key location will be used (by omitting the ssh -i argument!)!");
		textSshPrivateKey.getTextControl().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (previewTextfield != null) {
					previewTextfield.setText(createTemplate(true));
				}
			}
		});
		GridLayout gl_textSshPrivateKey = (GridLayout) textSshPrivateKey.getLayout();
		gl_textSshPrivateKey.marginHeight = 0;
		gl_textSshPrivateKey.marginWidth = 0;
		textSshPrivateKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		Label lblLocalPort = new Label(composite_1, SWT.NONE);
		lblLocalPort.setText("Local Port:");

		spinnerLocalPort = new Spinner(composite_1, SWT.BORDER);
		spinnerLocalPort.setToolTipText("The database remote port!");
		spinnerLocalPort.setMaximum(65535);
		spinnerLocalPort.setMinimum(1);
		spinnerLocalPort.setSelection(3306);
		spinnerLocalPort.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (previewTextfield != null) {
					previewTextfield.setText(createTemplate(true));
				}
			}
		});
		spinnerLocalPort.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (previewTextfield != null) {
					previewTextfield.setText(createTemplate(true));
				}
			}
		});
		GridData gd_spinnerLocalPort = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_spinnerLocalPort.heightHint = 20;
		spinnerLocalPort.setLayoutData(gd_spinnerLocalPort);
		new Label(composite_1, SWT.NONE);

		Label lblCustomArguments = new Label(composite_1, SWT.NONE);
		lblCustomArguments.setText("Custom arguments:");

		textSshCustomArgs = new Text(composite_1, SWT.BORDER);
		textSshCustomArgs.setText("-v -f -N -o StrictHostKeyChecking=no");
		textSshCustomArgs.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (previewTextfield != null) {
					previewTextfield.setText(createTemplate(true));
				}
			}
		});
		GridData gd_textSshCustomArgs = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_textSshCustomArgs.heightHint = 20;
		textSshCustomArgs.setLayoutData(gd_textSshCustomArgs);
		new Label(composite_1, SWT.NONE);
		new Label(composite_1, SWT.NONE);
		new Label(composite_1, SWT.NONE);
		new Label(composite_1, SWT.NONE);

		Label lblDownloadWindows = new Label(composite_1, SWT.NONE);
		lblDownloadWindows.setText("Download SSH Windows:");
		new Label(composite_1, SWT.NONE);
		new Label(composite_1, SWT.NONE);

		Link link_1 = new Link(composite_1, SWT.NONE);
		link_1.setText("<a href=\"https://github.com/PowerShell/Win32-OpenSSH/releases\">ssh.exe</a>");
		link_1.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Program.launch("https://github.com/PowerShell/Win32-OpenSSH/releases");
			}

		});
		new Label(composite_1, SWT.NONE);
		new Label(composite_1, SWT.NONE);

		createActions();
		//initializeToolBar();
		initializeMenu();
	}

	protected void okPressed() {

		/* Create the default template with custom code as pr�fix if not empty! */
		finalTemplate = createTemplate(false) + "\n#Close connection!\n" + textDisconnect.getText();

		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();

		ITextEditor editor = (ITextEditor) editore;
		if (editor != null) {
			IDocumentProvider dp = editor.getDocumentProvider();
			IDocument doc = dp.getDocument(editor.getEditorInput());

			ISelectionProvider sp = editor.getSelectionProvider();
			ISelection selectionsel = sp.getSelection();
			ITextSelection selection = (ITextSelection) selectionsel;

			if (finalTemplate != null) {
				try {
					doc.replace(selection.getOffset(), selection.getLength(), finalTemplate);

				} catch (BadLocationException e) {

					e.printStackTrace();
				}

			}
		}

	}

	
    /*Plugin has to be unpacked so that this method works!*/
	private File getProfilesFile() {
		/*Path is calculated in the PreferenceInitializer class!*/
		IPreferenceStore storeR = Bio7REditorPlugin.getDefault().getPreferenceStore();
		String configLoc = storeR.getString("LOCATION_DATABASE_XML_CONFIG");
		File fileR=new File(configLoc);
		/*
		 * Bundle bundle = Platform.getBundle("com.eco.bio7.redit");
		 * 
		 * // String path = bundle.getLocation().split(":")[2]; URL locationUrl =
		 * FileLocator.find(bundle, new Path("/"), null); URL fileUrl = null; try {
		 * fileUrl = FileLocator.toFileURL(locationUrl); } catch (IOException e2) { //
		 * TODO Auto-generated catch block e2.printStackTrace(); } String path =
		 * fileUrl.getFile();
		 * 
		 * // System.out.println(path); File fileStartupScripts = new File(path +
		 * "/store/database_profiles.xml");
		 */
		return fileR;
	}

	public String getSqlTemplate() {
		return finalTemplate;
	}

	private String createTemplate(boolean hiddenPassword) {

		String port = "";
		if (spinner != null) {
			if (spinner.getText() == null) {
				port = "";
			} else {
				port = spinner.getText();
			}
		}
		String driver;
		if (combo.getText() == null) {
			driver = "";
		} else {
			driver = combo.getText();
		}
		String database;
		if (databaseTextfield == null) {
			database = "";
		} else {
			database = databaseTextfield.getText();
		}
		String user;
		if (userTextfield == null) {
			user = "";
		} else {
			user = userTextfield.getText();
		}
		String password;
		if (passwordTextfield == null) {
			password = "";
		} else {
			password = passwordTextfield.getText();
		}
		String server;
		if (serverTextfield == null) {
			server = "";
		} else {
			server = serverTextfield.getText();
		}
		

		if (hiddenPassword) {
			password = "******";
			String template = "library(DBI);" + "\n" + "con <- dbConnect(odbc::odbc(),.connection_string=\"Driver={"
					+ driver + "};Server=" + server + ";Port=" + port + ";Database=" + database + ";UID=" + user
					+ ";PWD=" + password + ";\")";
			if (btnUseSshTunnel.getSelection()) {
				String ssHCon = createSshTemplate(true);
				return ssHCon + template;
			} else {
				return template;
			}

		} else {
			if (password.isEmpty()) {
				Shell shell = new Shell(Util.getDisplay());
				shell.setSize(200, 100);
				PasswordDialog dialog = new PasswordDialog(shell, "Database Login");
				dialog.setUser(user);
				// get the new values from the dialog
				if (dialog.open() == Window.OK) {

					user = dialog.getUser();
					password = dialog.getPassword();

				} else {

					password = "";
				}
			}
			String template = "library(DBI);" + "\n" + "con <- dbConnect(odbc::odbc(),.connection_string=\"Driver={"
					+ driver + "};Server=" + server + ";Port=" + port + ";Database=" + database + ";UID=" + user
					+ ";PWD=" + password + ";\")";

			if (btnUseSshTunnel.getSelection()) {
				String ssHCon = createSshTemplate(false);
				return ssHCon + template;
			} else {
				return template;
			}
		}
	}

	private String createSshTemplate(boolean hidden) {

		String plinkOrSshWin = textPlink;
		String customArgs = textSshCustomArgs.getText();
		String sshKey = textSshPrivateKey.getText();
		String sqlPort = spinner.getText();
		String sqlHost = serverTextfield.getText();
		String sshPort = spinnerSshPort.getText();
		String ssHLinkPort = spinnerLocalPort.getText();
		String user = textSshUsername.getText();

		// String password;
		/*
		 * if (hidden) { password = "*****"; }
		 */
		String host = textSshHost.getText();
		String timeout = spinnerTimeout.getText();

		// String userNameOption = " \" -l " + user;
		// String passwordNameOption = " -pw " + password + "\",\n";
		String identityFileOption = " -i " + sshKey;

		/*
		 * if (user.isEmpty()) { userNameOption = ""; }
		 */
		/*
		 * if (password.isEmpty()) { passwordNameOption = ""; }
		 */
		if (sshKey.isEmpty()) {
			identityFileOption = "";
		}

		String sshCode = null;
		String osname = System.getProperty("os.name");
		if (osname.startsWith("Windows")) {
			/*
			 * if (plinkOrSshWin.endsWith("plink.exe")) { Ask for a password if password
			 * field is empty! if (password.isEmpty()) { Shell shell = new
			 * Shell(Util.getDisplay()); shell.setSize(200, 100);
			 * shell.setText("SSH login"); PasswordDialog dialog = new PasswordDialog(shell,
			 * "SSH Login");
			 * 
			 * dialog.setUser(user); // get the new values from the dialog if (dialog.open()
			 * == Window.OK) {
			 * 
			 * user = dialog.getUser(); password = dialog.getPassword(); userNameOption =
			 * "  \" -l " + user; passwordNameOption = " -pw " + password + "\",\n";
			 * 
			 * } } Create the plink code! sshCode = "#Open a ssh connection with plink!\n" +
			 * "cmd<- paste0(\n" + "  \"" + plinkOrSshWin + " \",\n" +
			 * "  # use key and run in background process\n" + "  \" -i " + sshKey +
			 * " -N -batch  -ssh\",\n" + "  # Custom arguments\n" + "  \" " + customArgs +
			 * "\",\n" + "  # port forwarding\n" + "  \" -L " + sqlPort + ":" + sqlHost +
			 * ":" + ssHLinkPort + "\",\n" + "  \" -P " + sshPort + "\",\n" + userNameOption
			 * + passwordNameOption + "  # location of db\n" + "  \" " + host + "\"\n" +
			 * ")\n" + "shell(cmd, wait=FALSE)" + "\n\n";
			 * 
			 * Create Windows SSH code! } else if (plinkOrSshWin.endsWith("ssh.exe")) {
			 */
			/* Create the ssh code! */
			sshCode = "shell('" + plinkOrSshWin + identityFileOption + " " + customArgs + " " + user + "@" + host
					+ " -L " + sqlPort + ":" + sqlHost + ":" + ssHLinkPort + " -p " + sshPort + " -N sleep " + timeout
					+ "',wait=FALSE)\n\n";
			// }

			return sshCode;
		} else {
			/*
			 * system('ssh -f <server_user>@<server_ip> -L
			 * <unused_local_port>:localhost:<database_remote_port> -N')
			 */
			sshCode = "system2('ssh'," + "'" + identityFileOption + " " + customArgs + " " + user + "@" + host + " -L "
					+ sqlPort + ":" + sqlHost + ":" + ssHLinkPort + " -p " + sshPort + " -N sleep " + timeout
					+ "')\n\n";

			return sshCode;
		}

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
	/*private void initializeToolBar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
	}*/

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

	public static String getSshWindowsPath() {
		Bundle bundle = Platform.getBundle("com.eco.bio7.libs");
		Path path = new Path("ssh_win64/ssh.exe");
		URL locationURL = FileLocator.find(bundle, path, null);
		URL fileUrl = null;
		try {
			fileUrl = FileLocator.toFileURL(locationURL);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		String pathBundle = fileUrl.getFile().replaceFirst("/", "");
		return pathBundle;
	}

}
