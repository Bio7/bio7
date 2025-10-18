/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
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
	private String[] webAdresses = new String[] { "https://mirror.cricyt.edu.ar/r/", "https://cran.ms.unimelb.edu.au/", "https://cran.at.r-project.org/", "https://www.freestatistics.org/cran/", "https://cran.br.r-project.org/", "https://cran.fiocruz.br/", "https://www.vps.fmvz.usp.br/CRAN/",
			"https://brieger.esalq.usp.br/CRAN/", "https://cran.stat.sfu.ca/", "https://probability.ca/cran/", "https://dirichlet.mat.puc.cl/", "https://june.irb.hr/r/", "https://cran.biokontakt.cz/", "https://cran.dk.r-project.org/", "https://cran.fr.r-project.org/", "https://cran.miroir-francais.fr/",
			"https://ftp5.gwdg.de/pub/misc/cran/", "https://cran.rakanu.com/", "https://cms.unipune.ernet.in/computing/cran/", "https://ftp.heanet.ie/mirrors/cran.r-project.org/", "https://rm.mirror.garr.it/mirrors/CRAN/", "https://cran.stat.unipd.it/", "https://dssm.unipa.it/CRAN/",
			"ftp://ftp.u-aizu.ac.jp/pub/lang/R/CRAN", "ftp://ftp.ecc.u-tokyo.ac.jp/CRAN/", "https://cran.md.tsukuba.ac.jp/", "https://bibs.snu.ac.kr/R/", "https://www2.uaem.mx/r-mirror/", "https://cran.nedmirror.nl/", "https://cran.dsmirror.nl/", "https://cran-mirror.cs.uu.nl/",
			"https://cran.stat.auckland.ac.nz/", "https://cran.ii.uib.no/", "https://piotrkosoft.net/pub/mirrors/CRAN/", "https://r.meteo.uni.wroc.pl/", "https://cran.pt.r-project.org/", "https://www.wsection.com/cran/", "https://cran.za.r-project.org/", "https://cran.es.r-project.org/",
			"https://ftp.sunet.se/pub/lang/CRAN/", "https://cran.ch.r-project.org/", "https://www.imsv.unibe.ch/cran/", "https://cran.cs.pu.edu.tw/", "https://cran.csie.ntu.edu.tw/", "https://mirror.kapook.com/cran/", "https://cran.uk.r-project.org/", "https://cran.cnr.Berkeley.edu",
			"https://cran.stat.ucla.edu/", "https://rh-mirror.linux.iastate.edu/CRAN/", "https://cran.mtu.edu/", "https://cran.wustl.edu/", "https://www.ibiblio.org/pub/languages/R/CRAN/", "https://cran.mirrors.hoobly.com", "https://lib.stat.cmu.edu/R/CRAN/", "https://cran.fhcrc.org/" };
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

		//addField(new BooleanFieldEditor(PreferenceConstants.P_BOOLEAN, "Install path at startup", getFieldEditorParent()));
		selectLinuxShell = new RadioGroupFieldEditor("LINUX_SHELL", "Select Shell (Linux, Mac):", 2, new String[][] { { "xterm (Linux, Mac)", "XTERM" }, { "Gnome (Linux)", "GNOME" } }, getFieldEditorParent(), false);
		addField(selectLinuxShell);
		addField(new BooleanFieldEditor("DETECT_R_PROCESS", "Check at startup for running R process(es)", getFieldEditorParent()));
		addField(new StringFieldEditor("RSERVE_ARGS", "Rserve startup arguments", getFieldEditorParent()));
		addField(new MultiLineTextFieldEditor("R_STARTUP_ARGS", "R startup commands", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new IntegerFieldEditor("RSERVE_CLIENT_CONNECTION_PORT", "Rserve TCP client port", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));

		addField(new LabelFieldEditor("Packages:", getFieldEditorParent()));
		PackageServer = new StringFieldEditor("R_PACKAGE_SERVER", "Package Server", getFieldEditorParent());
		PackageServer.setErrorMessage("Please enter a valid address!");
		PackageServer.setEmptyStringAllowed(false);
		PackageServer.setStringValue("https://cran.r-project.org");
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
		addField(new LabelFieldEditor("R-Shell:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("UPDATE_VAR_RSHELL", "Update R Workspace Objects after evaluation", getFieldEditorParent()));
		addField(new BooleanFieldEditor("STREAM_TO_RSHELL", "Display output/error stream of console", getFieldEditorParent()));
		addField(new LabelFieldEditor("R-Shell code completion:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("RSHELL_TYPED_CODE_COMPLETION", "Open code completion/templates when typing", getFieldEditorParent()));
		addField(new StringFieldEditor("RSHELL_ACTIVATION_CHARS", "Activation chars (Restart to apply!)", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new StringFieldEditor("RSHELL_SEPERATOR_CHARS", "Seperator chars", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new RadioGroupFieldEditor("RSHELL_CODE_COMPLETION_ACTIVATOR_ALTERED", "Use a different key combination for code completion activation", 3, new String[][]{{"Strg+Space", "STRG"}, {"Alt+Space", "ALT"},{"Cmd+Space", "CMD"}}, getFieldEditorParent(), false));
		//addField(new BooleanFieldEditor("RSHELL_CODE_COMPLETION_ACTIVATOR_ALTERED", "Use Key Combination 'ALT+SPACE' to trigger code completion (Restart to apply!)", getFieldEditorParent()));

		{
			LabelFieldEditor labelFieldEditor = new LabelFieldEditor("Close and reopen R-Shell view or restart necessary:", getFieldEditorParent());
			labelFieldEditor.setLabelText("Code Completion Popup");
			addField(labelFieldEditor);
		}
		addField(new IntegerFieldEditor("CODE_COMPLETION_RSHELL_FONT_HEIGHT_CORRECTION", "R-Shell Code completion height correction", getFieldEditorParent()));
		addField(new IntegerFieldEditor("CODE_COMPLETION_POPUP_SIZE_X", "Popup size width (Restart to apply!)", getFieldEditorParent()));
		addField(new IntegerFieldEditor("CODE_COMPLETION_POPUP_SIZE_Y", "Popup size height (Restart to apply!)", getFieldEditorParent()));
		addField(new LabelFieldEditor("R source options:", getFieldEditorParent()));
		addField(new StringFieldEditor("R_SOURCE_OPTIONS", "Options for the source command", getFieldEditorParent()));
		
		addField(new LabelFieldEditor("Bio7 Shutdown (saving all editors first):", getFieldEditorParent()));
		addField(new BooleanFieldEditor("SAVE_R_WORKSPACE_ON_QUIT", "Execute R command on shutdown", getFieldEditorParent()));
		addField(new StringFieldEditor("ON_QUIT_COMMAND", "Options for the source command", getFieldEditorParent()));
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