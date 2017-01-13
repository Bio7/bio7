package com.eco.bio7.reditor.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import com.eco.bio7.editors.python.PythonEditor;
import com.eco.bio7.editors.python.PythonScriptColorProvider;
import com.eco.bio7.pythonedit.PythonEditorPlugin;
import com.eco.bio7.pythoneditors.ScriptCodeScanner;

public class WorkbenchPreferencePython extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private IPreferenceStore store;

	public WorkbenchPreferencePython() {
		super(GRID);
		store = PythonEditorPlugin.getDefault().getPreferenceStore();
		setPreferenceStore(PythonEditorPlugin.getDefault().getPreferenceStore());
		setDescription("Bio7 Jython Editor");
	}

	public void createFieldEditors() {
		final Link link = new Link(getFieldEditorParent(), SWT.NONE);
		link.setText("See <a href=\"org.eclipse.ui.preferencePages.ColorsAndFonts\">'Colors and Fonts'</a> to configure the font.");
		link.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));

		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				PreferencesUtil.createPreferenceDialogOn(new Shell(Display.getDefault()), "org.eclipse.ui.preferencePages.ColorsAndFonts", null, "selectFont:com.eco.bio7.pythoneditor.textfont");

			}
		});
		addField(new SpacerFieldEditor(getFieldEditorParent()));

		addField(new ColorFieldEditor("colourkey", "Colour Keywords:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BOLD_COLOURKEY", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addField(new ColorFieldEditor("colourkey1", "Colour Type:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BOLD_COLOURKEY1", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addField(new ColorFieldEditor("colourkey2", "Colour String:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BOLD_COLOURKEY2", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addField(new ColorFieldEditor("colourkey3", "Colour Single Comment:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BOLD_COLOURKEY3", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addField(new ColorFieldEditor("colourkey4", "Colour Default:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BOLD_COLOURKEY4", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addField(new ColorFieldEditor("colourkey5", "Operators:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BOLD_COLOURKEY5", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addField(new ColorFieldEditor("colourkey7", "Numbers:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BOLD_COLOURKEY7", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addField(new ColorFieldEditor("colourkey8", "Decorators:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BOLD_COLOURKEY8", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addField(new ColorFieldEditor("colourkey9", "Class Name:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BOLD_COLOURKEY9", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addField(new ColorFieldEditor("colourkey10", "Function Name", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BOLD_COLOURKEY10", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addField(new ColorFieldEditor("colourkey12", "Parenthesis:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BOLD_COLOURKEY12", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

	}

	public void init(IWorkbench workbench) {

		IPreferenceStore store = PythonEditorPlugin.getDefault().getPreferenceStore();
		PreferenceConverter.setDefault(store, "colourkey", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(store, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(store, "colourkey2", new RGB(42, 0, 255));
		PreferenceConverter.setDefault(store, "colourkey3", new RGB(63, 127, 95));
		PreferenceConverter.setDefault(store, "colourkey4", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, "colourkey5", new RGB(0, 0, 0));

		PreferenceConverter.setDefault(store, "colourkey7", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, "colourkey8", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, "colourkey9", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, "colourkey10", new RGB(0, 0, 0));

		PreferenceConverter.setDefault(store, "colourkey12", new RGB(0, 0, 0));

	}

	@Override
	protected void performDefaults() {

		super.performDefaults();
		ScopedPreferenceStore storeWorkbench = new ScopedPreferenceStore(new InstanceScope(), "org.eclipse.ui.workbench");
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		PythonEditor pythonEditor = (PythonEditor) editor;
		PythonEditorPlugin fginstance = PythonEditorPlugin.getDefault();
		ScriptCodeScanner scanner = (ScriptCodeScanner) fginstance.getScriptCodeScanner();
		PythonScriptColorProvider provider = PythonEditorPlugin.getDefault().getScriptColorProvider();
		storeWorkbench.setValue("com.eco.bio7.pythoneditor.textfont", storeWorkbench.getDefaultString("com.eco.bio7.pythoneditor.textfont"));
		PreferenceConverter.setValue(store, "colourkeyfont", JFaceResources.getFontRegistry().get("com.eco.bio7.pythoneditor.textfont").getFontData());
		scanner.keyword.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey")), null, 1));
		scanner.type.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey1")), null, 1));
		scanner.string.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey2")), null, 0));
		scanner.comment.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey3")), null, 0));
		scanner.defaultOther.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey4")), null, 0));
		scanner.operators.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey5")), null, 0));
		scanner.numbers.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey7")), null, 0));
		scanner.decorator.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey8")), null, 0));
		scanner.className.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey9")), null, 0));
		scanner.funcName.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey10")), null, 0));
		scanner.parenthesis.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey12")), null, 0));
		pythonEditor.invalidateText();
		super.performOk();
	}

	public void propertyChange(org.eclipse.jface.util.PropertyChangeEvent event) {
		super.propertyChange(event);
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		PythonEditor pythonEditor = (PythonEditor) editor;
		IPreferenceStore store = PythonEditorPlugin.getDefault().getPreferenceStore();

		if (event.getSource() instanceof ColorFieldEditor) {
			ColorFieldEditor col = (ColorFieldEditor) event.getSource();
			String name = col.getPreferenceName();
			RGB rgb = (RGB) event.getNewValue();
			PythonEditorPlugin fginstance = PythonEditorPlugin.getDefault();
			ScriptCodeScanner scanner = (ScriptCodeScanner) fginstance.getScriptCodeScanner();
			PythonScriptColorProvider provider = PythonEditorPlugin.getDefault().getScriptColorProvider();

			switch (name) {
			case "colourkey":
				scanner.keyword.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY")));
				break;
			case "colourkey1":
				scanner.type.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY1")));
				break;

			case "colourkey2":
				scanner.string.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY2")));
				break;
			case "colourkey3":
				scanner.comment.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY3")));
				break;
			case "colourkey4":
				scanner.defaultOther.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY4")));
				break;
			case "colourkey5":
				scanner.operators.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY5")));
				break;

			case "colourkey7":
				scanner.numbers.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY7")));
				break;
			case "colourkey8":
				scanner.decorator.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY8")));
				break;
			case "colourkey9":
				scanner.className.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY9")));
				break;
			case "colourkey10":
				scanner.funcName.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY10")));
				break;

			case "colourkey12":
				scanner.parenthesis.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY12")));
				break;
			default:
				break;
			}

		}

		else if (event.getSource() instanceof BooleanFieldEditor) {
			BooleanFieldEditor col = (BooleanFieldEditor) event.getSource();
			String name = col.getPreferenceName();

			boolean fontData = (boolean) event.getNewValue();

			PythonEditorPlugin fginstance = PythonEditorPlugin.getDefault();
			ScriptCodeScanner scanner = (ScriptCodeScanner) fginstance.getScriptCodeScanner();
			PythonScriptColorProvider provider = PythonEditorPlugin.getDefault().getScriptColorProvider();

			switch (name) {

			case "BOLD_COLOURKEY":

				if (fontData) {
					scanner.keyword.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey")), null, SWT.BOLD));
				} else {
					scanner.keyword.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey")), null, SWT.NORMAL));
				}

				break;
			case "BOLD_COLOURKEY1":
				if (fontData) {
					scanner.type.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey1")), null, SWT.BOLD));
				} else {
					scanner.type.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey1")), null, SWT.NORMAL));
				}

				break;

			case "BOLD_COLOURKEY2":
				if (fontData) {
					scanner.string.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey2")), null, SWT.BOLD));
				} else {
					scanner.string.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey2")), null, SWT.NORMAL));
				}
				break;
			case "BOLD_COLOURKEY3":
				if (fontData) {
					scanner.comment.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey3")), null, SWT.BOLD));
				} else {
					scanner.comment.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey3")), null, SWT.NORMAL));
				}
				break;
			case "BOLD_COLOURKEY4":
				if (fontData) {
					scanner.defaultOther.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey4")), null, SWT.BOLD));
				} else {
					scanner.defaultOther.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey4")), null, SWT.NORMAL));
				}
				break;
			case "BOLD_COLOURKEY5":
				if (fontData) {
					scanner.operators.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey5")), null, SWT.BOLD));
				} else {
					scanner.operators.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey5")), null, SWT.NORMAL));
				}
				break;

			case "BOLD_COLOURKEY7":
				if (fontData) {
					scanner.numbers.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey7")), null, SWT.BOLD));
				} else {
					scanner.numbers.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey7")), null, SWT.NORMAL));
				}
				break;
			case "BOLD_COLOURKEY8":
				if (fontData) {
					scanner.decorator.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey8")), null, SWT.BOLD));
				} else {
					scanner.decorator.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey8")), null, SWT.NORMAL));
				}
				break;
			case "BOLD_COLOURKEY9":
				if (fontData) {
					scanner.className.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey9")), null, SWT.BOLD));
				} else {
					scanner.className.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey9")), null, SWT.NORMAL));
				}
				break;
			case "BOLD_COLOURKEY10":
				if (fontData) {
					scanner.funcName.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey10")), null, SWT.BOLD));
				} else {
					scanner.funcName.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey10")), null, SWT.NORMAL));
				}
				break;

			case "BOLD_COLOURKEY12":
				if (fontData) {
					scanner.parenthesis.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey12")), null, SWT.BOLD));
				} else {
					scanner.parenthesis.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey12")), null, SWT.NORMAL));
				}
				break;

			default:
				break;
			}

		}

		pythonEditor.invalidateText();
		super.performOk();

	}

	private int isBold(String string2) {
		int style = 0;
		IPreferenceStore store = PythonEditorPlugin.getDefault().getPreferenceStore();
		if (store.getBoolean(string2)) {
			style = 1;
		}

		return style;
	}

	public boolean performCancel() {

		return super.performCancel();
	}

	public boolean performOk() {

		return super.performOk();
	}

}
