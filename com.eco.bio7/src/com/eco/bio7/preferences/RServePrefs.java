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

package com.eco.bio7.preferences;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FontFieldEditor;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import com.eco.bio7.Bio7Plugin;
import com.swtdesigner.preference.ComboFieldEditor;

public class RServePrefs extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, PropertyChangeListener {

	private StringFieldEditor PackageServer;
	private String[] webAdresses = new String[] { "http://mirror.cricyt.edu.ar/r/", "http://cran.ms.unimelb.edu.au/", "http://cran.at.r-project.org/", "http://www.freestatistics.org/cran/", "http://cran.br.r-project.org/", "http://cran.fiocruz.br/", "http://www.vps.fmvz.usp.br/CRAN/",
			"http://brieger.esalq.usp.br/CRAN/", "http://cran.stat.sfu.ca/", "http://probability.ca/cran/", "http://dirichlet.mat.puc.cl/", "http://june.irb.hr/r/", "http://cran.biokontakt.cz/", "http://cran.dk.r-project.org/", "http://cran.fr.r-project.org/", "http://cran.miroir-francais.fr/",
			"http://ftp5.gwdg.de/pub/misc/cran/", "http://cran.rakanu.com/", "http://cms.unipune.ernet.in/computing/cran/", "http://ftp.heanet.ie/mirrors/cran.r-project.org/", "http://rm.mirror.garr.it/mirrors/CRAN/", "http://cran.stat.unipd.it/", "http://dssm.unipa.it/CRAN/",
			"ftp://ftp.u-aizu.ac.jp/pub/lang/R/CRAN", "ftp://ftp.ecc.u-tokyo.ac.jp/CRAN/", "http://cran.md.tsukuba.ac.jp/", "http://bibs.snu.ac.kr/R/", "http://www2.uaem.mx/r-mirror/", "http://cran.nedmirror.nl/", "http://cran.dsmirror.nl/", "http://cran-mirror.cs.uu.nl/",
			"http://cran.stat.auckland.ac.nz/", "http://cran.ii.uib.no/", "http://piotrkosoft.net/pub/mirrors/CRAN/", "http://r.meteo.uni.wroc.pl/", "http://cran.pt.r-project.org/", "http://www.wsection.com/cran/", "http://cran.za.r-project.org/", "http://cran.es.r-project.org/",
			"http://ftp.sunet.se/pub/lang/CRAN/", "http://cran.ch.r-project.org/", "http://www.imsv.unibe.ch/cran/", "http://cran.cs.pu.edu.tw/", "http://cran.csie.ntu.edu.tw/", "http://mirror.kapook.com/cran/", "http://cran.uk.r-project.org/", "http://cran.cnr.Berkeley.edu",
			"http://cran.stat.ucla.edu/", "http://rh-mirror.linux.iastate.edu/CRAN/", "http://cran.mtu.edu/", "http://cran.wustl.edu/", "http://www.ibiblio.org/pub/languages/R/CRAN/", "http://cran.mirrors.hoobly.com", "http://lib.stat.cmu.edu/R/CRAN/", "http://cran.fhcrc.org/" };
	private FontFieldEditor f;
	public MultiLineTextFieldEditor mult;
	public StringFieldEditor deviceFilename;
	public RadioGroupFieldEditor selectionDevice;
	public RadioGroupFieldEditor selectLinuxShell;
	// private RadioGroupFieldEditor selectPDFReader;
	private static RServePrefs instance;

	public static RServePrefs getInstance() {
		return instance;
	}

	public Composite getFieldEditorParentControl() {
		return getFieldEditorParent();
	}

	public RServePrefs() {
		super(GRID);
		setPreferenceStore(Bio7Plugin.getDefault().getPreferenceStore());
		instance = this;

	}

	public void createFieldEditors() {

		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("General:", getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceConstants.PATH_R, "&Path to R:", getFieldEditorParent()));
		addField(new DirectoryFieldEditor("InstallLocation", "Package install location", getFieldEditorParent()));
		addField(new DirectoryFieldEditor(PreferenceConstants.P_TEMP_R, "Path to temporary R folder", getFieldEditorParent()));

		addField(new BooleanFieldEditor(PreferenceConstants.P_BOOLEAN, "Install path at startup", getFieldEditorParent()));
		selectLinuxShell = new RadioGroupFieldEditor("LINUX_SHELL", "Select Shell (Linux, Mac):", 2, new String[][] { { "xterm (Linux, Mac)", "XTERM" }, { "Gnome (Linux)", "GNOME" } }, getFieldEditorParent(), false);
		addField(selectLinuxShell);
		addField(new BooleanFieldEditor("DETECT_R_PROCESS", "Check at startup for running R process(es)", getFieldEditorParent()));
		addField(new StringFieldEditor("RSERVE_ARGS", "Rserve startup arguments", getFieldEditorParent()));
		addField(new MultiLineTextFieldEditor("R_STARTUP_ARGS", "R startup commands", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new IntegerFieldEditor("RSERVE_CLIENT_CONNECTION_PORT", "Rserve TCP client port", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));

		addField(new LabelFieldEditor("Packages:", getFieldEditorParent()));
		PackageServer = new StringFieldEditor(PreferenceConstants.PACKAGE_R_SERVER, "Package Server", getFieldEditorParent());
		PackageServer.setErrorMessage("Please enter a valid address!");
		PackageServer.setEmptyStringAllowed(false);
		PackageServer.setStringValue("http://cran.r-project.org");
		addField(PackageServer);

		Composite fieldEditorParent = getFieldEditorParent();
		final ComboFieldEditor comboFieldEditor = new ComboFieldEditor("Server", "Select Server", new String[][] { new String[] { "", "" } }, fieldEditorParent);
		final Combo combo = comboFieldEditor.getComboBoxControl(fieldEditorParent);
		combo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				PackageServer.setStringValue(webAdresses[combo.getSelectionIndex()]);
			}
		});

		combo.setItems(new String[] { "Argentina", "Australia", "Austria", "Belgium", "Brazil (PR)", "Brazil (RJ)", "Brazil (SP 1)", "Brazil (SP 2)", "Canada (BC)", "Canada (ON)", "Chile (Santiago)", "Croatia", "Czech Republic", "Denmark", "France (Toulouse)", "France (Paris)",
				"Germany (Goettingen)", "Germany (Muenchen)", "India", "Ireland", "Italy (Milano)", "Italy (Padua)", "Italy (Palermo)", "Japan (Aizu)", "Japan (Tokyo)", "Japan (Tsukuba)", "Korea", "Mexico", "Netherlands (Amsterdam 2)", "Netherlands (Amsterdam)", "Netherlands (Utrecht)",
				"New Zealand", "Norway", "Poland (Oswiecim)", "Poland (Wroclaw)", "Portugal", "Slovenia (Ljubljana)", "South Africa", "Spain (Madrid)", "Sweden", "Switzerland (Zuerich)", "Switzerland (Bern)", "Taiwan (Taichung)", "Taiwan (Taipeh)", "Thailand", "UK (Bristol)", "USA (CA 1)",
				"USA (CA 3)", "USA (IA)", "USA (MI)", "USA (MO)", "USA (NC)", "USA (PA 2)", "USA (PA)", "USA (WA)"

		});
		combo.select(1);

		addField(comboFieldEditor);

		addField(new SpacerFieldEditor(getFieldEditorParent()));

		addField(new LabelFieldEditor("Transfer to Table options:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("TRANSFER_METHOD", "Transfer with \"format\" to table (default is method \"as.character\")", getFieldEditorParent()));

		final IntegerFieldEditor integerFieldEditor = new IntegerFieldEditor("DEFAULT_DIGITS", "Digits", getFieldEditorParent());
		integerFieldEditor.setErrorMessage("Please select an integer value!");

		integerFieldEditor.setValidRange(1, 100);
		addField(integerFieldEditor);
		
		addField(new BooleanFieldEditor("RSHELL_TYPED_CODE_COMPLETION", "R-Shell: Open code completion/templates when typing", getFieldEditorParent()));

	}

	public void init(IWorkbench workbench) {

	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		//String property = event.getPropertyName();

	}

	/*
	 * public boolean performOk() {
	 * 
	 * 
	 * return super.performOk(); }
	 */
}