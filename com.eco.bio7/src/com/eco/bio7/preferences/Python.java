package com.eco.bio7.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.eco.bio7.Bio7Plugin;

public class Python extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/* A check box which can be set by the detection of Python */
	private static final class CheckField extends BooleanFieldEditor {
		private final Composite parent;

		CheckField(String name, String label, Composite parent) {
			super(name, label, BooleanFieldEditor.DEFAULT, parent);
			this.parent = parent;
		}

		void select(boolean value) {
			boolean old = getBooleanValue();
			getChangeControl(parent).setSelection(value);
			valueChanged(old, value);
		}
	}

	private CheckField python3;
	private DirectoryFieldEditor pythonPath;

	/**
	 * Create the preference page.
	 */
	public Python() {
		super(FieldEditorPreferencePage.GRID);
		setPreferenceStore(Bio7Plugin.getDefault().getPreferenceStore());
		setDescription("");
	}

	/**
	 * Create contents of the preference page.
	 */
	@Override
	protected void createFieldEditors() {
		SpacerFieldEditor spacer1 = new SpacerFieldEditor(getFieldEditorParent());
		addField(spacer1);
		
		{
			LabelFieldEditor labelFieldEditor = new LabelFieldEditor("Python:", getFieldEditorParent());
			labelFieldEditor.setLabelText("Python:");
			addField(labelFieldEditor);
		}
		
		addField(new BooleanFieldEditor("IMAGE_METHODS_CLOSE_VIEWS", "Image-Methods: the switch R/Python closes the views of the other language (R-Shell, packages)",
				BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		BooleanFieldEditor embedded = new BooleanFieldEditor("python_embedded", "Embedded Python (in the Bio7 process: data transfer, plots; a change of the Python needs a restart)",
				BooleanFieldEditor.DEFAULT, getFieldEditorParent());
		addField(embedded);
		addField(new BooleanFieldEditor("python_process_extra", "Execute Editor Source in Seperate Python Process (not sent to Bio7 Python Shell)", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		python3 = new CheckField("python_3x", "Python 3 is used to evaluate scripts (only important if sent to Bio7 Python Shell)", getFieldEditorParent());
		addField(python3);
		pythonPath = new DirectoryFieldEditor("python_pipe_path", "Path Python", getFieldEditorParent());
		addField(pythonPath);
		addField(new ActionButtonFieldEditor("Find Python on this computer:", "Detect Python...", this::detectPython, getFieldEditorParent()));
		addField(new ActionButtonFieldEditor("Install, upgrade, uninstall packages:", "Python Packages...", () -> {
			com.eco.bio7.pybridge.PythonPackagesDialog.open(getShell());
			/* A new Bio7 environment may have been selected (else 'Apply' writes the former values back) */
			pythonPath.load();
			python3.load();
			embedded.load();
		}, getFieldEditorParent()));
		addField(new BooleanFieldEditor(com.eco.bio7.pybridge.PythonEnvironment.PREF_USE_UV,
				"Install packages with uv instead of pip (if uv is installed: faster, the environment needs no pip)", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		/* As the R-Shell code completion (Preferences > R) */
		addField(new LabelFieldEditor("Python-Shell code completion (close and reopen the Python-Shell view or restart to apply):", getFieldEditorParent()));
		addField(new BooleanFieldEditor("PYSHELL_TYPED_CODE_COMPLETION", "Open code completion when typing", getFieldEditorParent()));
		addField(new StringFieldEditor("PYSHELL_ACTIVATION_CHARS", "Activation chars", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new RadioGroupFieldEditor("PYSHELL_CODE_COMPLETION_ACTIVATOR_ALTERED", "Key combination for code completion activation", 3,
				new String[][] { { "Strg+Space", "STRG" }, { "Alt+Space", "ALT" }, { "Cmd+Space", "CMD" } }, getFieldEditorParent(), false));
		addField(new IntegerFieldEditor("PYSHELL_POPUP_SIZE_X", "Popup size width", getFieldEditorParent()));
		addField(new IntegerFieldEditor("PYSHELL_POPUP_SIZE_Y", "Popup size height", getFieldEditorParent()));
		SpacerFieldEditor spacer2 = new SpacerFieldEditor(getFieldEditorParent());
		addField(spacer2);
	}

	/* Searches the Python interpreters (PATH, installation folders) and sets the selected one */
	private void detectPython() {
		java.util.List<com.eco.bio7.console.PythonFinder.Found> found = new java.util.ArrayList<>();
		org.eclipse.swt.custom.BusyIndicator.showWhile(getShell().getDisplay(), () -> found.addAll(com.eco.bio7.console.PythonFinder.detect()));
		/* The preferences store a folder: versioned executables (e.g., /usr/bin/python3.12) can't be selected here */
		found.removeIf(f -> !com.eco.bio7.console.PythonFinder.selectableAsFolder(f));
		if (found.isEmpty()) {
			org.eclipse.jface.dialogs.MessageDialog.openInformation(getShell(), "Detect Python",
					"No Python 3 interpreter was found (PATH, registry, installation folders of Python, Homebrew, pyenv, uv, conda).\n\n"
							+ "Install Python 3 (e.g., from python.org) or select the folder of the interpreter.");
			return;
		}
		com.eco.bio7.console.PythonFinder.Found selected = found.get(0);
		if (found.size() > 1) {
			org.eclipse.ui.dialogs.ListDialog dialog = new org.eclipse.ui.dialogs.ListDialog(getShell());
			dialog.setTitle("Detect Python");
			dialog.setMessage("Select the Python interpreter:");
			dialog.setContentProvider(org.eclipse.jface.viewers.ArrayContentProvider.getInstance());
			dialog.setLabelProvider(new org.eclipse.jface.viewers.LabelProvider());
			dialog.setInput(found);
			dialog.setInitialSelections(selected);
			dialog.setWidthInChars(70);
			if (dialog.open() != org.eclipse.jface.window.Window.OK || dialog.getResult() == null || dialog.getResult().length == 0) {
				return;
			}
			selected = (com.eco.bio7.console.PythonFinder.Found) dialog.getResult()[0];
		}
		pythonPath.setStringValue(selected.folder());
		python3.select(true);
		org.eclipse.jface.dialogs.MessageDialog.openInformation(getShell(), "Detect Python",
				selected.version() + " was selected:\n" + selected.executable() + "\n\nPress 'Apply' to keep it.");
	}

	@Override
	public boolean performOk() {
		String before = getPreferenceStore().getString("python_pipe_path");
		boolean ok = super.performOk();
		String after = getPreferenceStore().getString("python_pipe_path");
		/* The embedded Python can't be changed while Bio7 runs */
		if (!before.equals(after) && com.eco.bio7.pybridge.EmbeddedPython.isRunning()) {
			org.eclipse.jface.dialogs.MessageDialog.openInformation(getShell(), "Python",
					"The embedded Python is running (" + com.eco.bio7.pybridge.EmbeddedPython.session().executable() + ").\n\n"
							+ "The selected Python is used after a restart of Bio7.");
		}
		return ok;
	}

	/**
	 * Initialize the preference page.
	 */
	public void init(IWorkbench workbench) {
		// Initialize the preference page
	}

}
