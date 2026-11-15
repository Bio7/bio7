package com.eco.bio7.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
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

	private CheckField usePython;
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
			LabelFieldEditor labelFieldEditor = new LabelFieldEditor("Python/Blender:", getFieldEditorParent());
			labelFieldEditor.setLabelText("Python/Blender:");
			addField(labelFieldEditor);
		}
		
		usePython = new CheckField("python_pipe", "Use Python (instead Jython)", getFieldEditorParent());
		addField(usePython);
		addField(new BooleanFieldEditor("IMAGE_METHODS_CLOSE_VIEWS", "Image-Methods: the switch R/Python closes the views of the other language (R-Shell, packages)",
				BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor("python_embedded", "Embedded Python (in the Bio7 process: data transfer, plots; a change of the Python needs a restart)",
				BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor("python_process_extra", "Execute Editor Source in Seperate Python Process (not sent to Bio7 Python Shell)", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		python3 = new CheckField("python_3x", "Python 3 is used to evaluate scripts (only important if sent to Bio7 Python Shell)", getFieldEditorParent());
		addField(python3);
		addField(new RadioGroupFieldEditor("python_select", "Select Interpreter", 1, new String[][] { { "Python", "Python" }, { "Blender", "Blender" } }, getFieldEditorParent(), false));
		pythonPath = new DirectoryFieldEditor("python_pipe_path", "Path Python", getFieldEditorParent());
		addField(pythonPath);
		addField(new ActionButtonFieldEditor("Find Python on this computer:", "Detect Python...", this::detectPython, getFieldEditorParent()));
		addField(new ActionButtonFieldEditor("Install, upgrade, uninstall packages:", "Python Packages...", () -> {
			com.eco.bio7.pybridge.PythonPackagesDialog.open(getShell());
			/* A new Bio7 environment may have been selected */
			pythonPath.load();
			usePython.load();
			python3.load();
		}, getFieldEditorParent()));
		addField(new LabelFieldEditor("Blender:", getFieldEditorParent()));
		addField(new DirectoryFieldEditor("path_blender", "Path Blender", getFieldEditorParent()));

		RadioGroupFieldEditor radioGroupFieldEditor = new RadioGroupFieldEditor("blender_options", "Blender Default Options", 1, new String[][] { { "None (-)", "pnone" },{ "Interactive Shell (--python-console)", "interactive" },
				{ "Python Script (-P)", "pscript" } }, getFieldEditorParent(), false);
		
		addField(radioGroupFieldEditor);
		addField(new StringFieldEditor("blender_args", "Blender Arguments", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));

		addField(new MultiLineTextFieldEditor("before_script_blender", "Before Script Execution (Interactive Shell)", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new MultiLineTextFieldEditor("after_script_blender", "After Script Execution (Interactive Shell)", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		SpacerFieldEditor spacer2 = new SpacerFieldEditor(getFieldEditorParent());
		addField(spacer2);
	}

	/* Searches the Python interpreters (PATH, installation folders) and sets the selected one */
	private void detectPython() {
		java.util.List<com.eco.bio7.console.PythonFinder.Found> found = new java.util.ArrayList<>();
		org.eclipse.swt.custom.BusyIndicator.showWhile(getShell().getDisplay(), () -> found.addAll(com.eco.bio7.console.PythonFinder.detect()));
		if (found.isEmpty()) {
			org.eclipse.jface.dialogs.MessageDialog.openInformation(getShell(), "Detect Python",
					"No Python 3 interpreter was found (PATH, installation folders of Python, Homebrew, pyenv, conda).\n\n"
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
		usePython.select(true);
		python3.select(true);
		org.eclipse.jface.dialogs.MessageDialog.openInformation(getShell(), "Detect Python",
				selected.version() + " was selected:\n" + selected.executable() + "\n\n'Use Python (instead Jython)' is enabled. Press 'Apply' to keep it.");
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
