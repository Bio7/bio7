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
		setDescription("Bio7 Python Editor");
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
		/* The hovers: Preferences Python Editor Code */
		addField(new SpacerFieldEditor(getFieldEditorParent()));

		/* The color schemes (all colors below; then a single color can be changed) */
		addField(new org.eclipse.jface.preference.RadioGroupFieldEditor(com.eco.bio7.util.ColorSchemes.LIGHT_PREFERENCE, "Color scheme (light theme)", 4,
				com.eco.bio7.util.ColorSchemes.comboEntries(false), getFieldEditorParent(), true));
		addField(new org.eclipse.jface.preference.RadioGroupFieldEditor(com.eco.bio7.util.ColorSchemes.DARK_PREFERENCE, "Color scheme (dark theme)", 3,
				com.eco.bio7.util.ColorSchemes.comboEntries(true), getFieldEditorParent(), true));
		addField(new SpacerFieldEditor(getFieldEditorParent()));

		addStyleField(new ColorFieldEditor("colourkey", "Colour Keywords:", getFieldEditorParent()));
		addStyleField(new BooleanFieldEditor("BOLD_COLOURKEY", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addStyleField(new ColorFieldEditor("colourkey1", "Colour Type:", getFieldEditorParent()));
		addStyleField(new BooleanFieldEditor("BOLD_COLOURKEY1", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addStyleField(new ColorFieldEditor("colourkey2", "Colour String:", getFieldEditorParent()));
		addStyleField(new BooleanFieldEditor("BOLD_COLOURKEY2", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addStyleField(new ColorFieldEditor("colourkey3", "Colour Single Comment:", getFieldEditorParent()));
		addStyleField(new BooleanFieldEditor("BOLD_COLOURKEY3", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addStyleField(new ColorFieldEditor("colourkey4", "Colour Default:", getFieldEditorParent()));
		addStyleField(new BooleanFieldEditor("BOLD_COLOURKEY4", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addStyleField(new ColorFieldEditor("colourkey5", "Operators:", getFieldEditorParent()));
		addStyleField(new BooleanFieldEditor("BOLD_COLOURKEY5", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addStyleField(new ColorFieldEditor("colourkey7", "Numbers:", getFieldEditorParent()));
		addStyleField(new BooleanFieldEditor("BOLD_COLOURKEY7", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addStyleField(new ColorFieldEditor("colourkey8", "Decorators:", getFieldEditorParent()));
		addStyleField(new BooleanFieldEditor("BOLD_COLOURKEY8", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addStyleField(new ColorFieldEditor("colourkey9", "Class Name:", getFieldEditorParent()));
		addStyleField(new BooleanFieldEditor("BOLD_COLOURKEY9", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addStyleField(new ColorFieldEditor("colourkey10", "Function Name", getFieldEditorParent()));
		addStyleField(new BooleanFieldEditor("BOLD_COLOURKEY10", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addStyleField(new ColorFieldEditor("colourkey12", "Parenthesis:", getFieldEditorParent()));
		addStyleField(new BooleanFieldEditor("BOLD_COLOURKEY12", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addStyleField(new ColorFieldEditor("colourkey13", "Function calls:", getFieldEditorParent()));
		addStyleField(new BooleanFieldEditor("BOLD_COLOURKEY13", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

	}

	/* The color and bold fields (reloaded when a color scheme is selected) */
	private final java.util.List<org.eclipse.jface.preference.FieldEditor> styleFields = new java.util.ArrayList<>();

	private void addStyleField(org.eclipse.jface.preference.FieldEditor field) {
		styleFields.add(field);
		addField(field);
	}

	/* A color scheme was selected: all colors are set to it (the editors follow) */
	private void applyScheme(String preference, String scheme) {
		IPreferenceStore st = PythonEditorPlugin.getDefault().getPreferenceStore();
		st.setValue(preference, scheme);
		boolean dark = com.eco.bio7.util.Util.isDarkTheme();
		if (preference.equals(dark ? com.eco.bio7.util.ColorSchemes.DARK_PREFERENCE : com.eco.bio7.util.ColorSchemes.LIGHT_PREFERENCE)) {
			PreferenceInitializer.setColorDefaults(st, dark);
			com.eco.bio7.util.ColorSchemes.resetToDefaults(st, PreferenceInitializer.COLOR_ROLES);
			for (org.eclipse.jface.preference.FieldEditor f : styleFields) {
				f.load();
			}
			updateEditorColors();
		}
	}

	public void init(IWorkbench workbench) {

		IPreferenceStore store = PythonEditorPlugin.getDefault().getPreferenceStore();

		PreferenceInitializer.setColorDefaults(store, com.eco.bio7.util.Util.isDarkTheme());

	}

	@Override
	protected void performDefaults() {
		/* The classic color schemes: their colors are the defaults loaded below */
		IPreferenceStore st = PythonEditorPlugin.getDefault().getPreferenceStore();
		st.setToDefault(com.eco.bio7.util.ColorSchemes.LIGHT_PREFERENCE);
		st.setToDefault(com.eco.bio7.util.ColorSchemes.DARK_PREFERENCE);
		PreferenceInitializer.setColorDefaults(st, com.eco.bio7.util.Util.isDarkTheme());

		super.performDefaults();
		ScopedPreferenceStore storeWorkbench = new ScopedPreferenceStore(new InstanceScope(), "org.eclipse.ui.workbench");
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor != null && editor instanceof PythonEditor) {
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
		}
		super.performOk();
	}

	public void propertyChange(org.eclipse.jface.util.PropertyChangeEvent event) {
		super.propertyChange(event);
		if (event.getSource() instanceof org.eclipse.jface.preference.RadioGroupFieldEditor radio
				&& org.eclipse.jface.preference.FieldEditor.VALUE.equals(event.getProperty())) {
			applyScheme(radio.getPreferenceName(), String.valueOf(event.getNewValue()));
			return;
		}
		/* The colors of the functions (live) */
		if (event.getSource() instanceof org.eclipse.jface.preference.FieldEditor fe && fe.getPreferenceName().matches("(BOLD_COLOURKEY|colourkey)(13)")) {
			IPreferenceStore st = PythonEditorPlugin.getDefault().getPreferenceStore();
			String name = fe.getPreferenceName();
			String number = name.replaceAll("\\D", "");
			RGB rgb = name.startsWith("colourkey") ? (RGB) event.getNewValue() : PreferenceConverter.getColor(st, "colourkey" + number);
			boolean bold = name.startsWith("BOLD") ? (boolean) event.getNewValue() : st.getBoolean("BOLD_COLOURKEY" + number);
			ScriptCodeScanner scanner = (ScriptCodeScanner) PythonEditorPlugin.getDefault().getScriptCodeScanner();
			TextAttribute attribute = new TextAttribute(PythonEditorPlugin.getDefault().getScriptColorProvider().getColor(rgb), null, bold ? SWT.BOLD : SWT.NORMAL);
			switch (number) {
			case "13" -> scanner.functionCall.setData(attribute);
			default -> {
			}
			}
			for (PythonEditor e : com.eco.bio7.util.Util.getOpenEditors(PythonEditor.class)) {
				e.invalidateText();
			}
			return;
		}
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor != null && editor instanceof PythonEditor) {
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
		}
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

	/**
	 * Updates the colors of the open Python editors from the preferences (e.g., after
	 * the theme has been changed between light and dark).
	 */
	public static void updateEditorColors() {
		IPreferenceStore store = PythonEditorPlugin.getDefault().getPreferenceStore();
		ScriptCodeScanner scanner = (ScriptCodeScanner) PythonEditorPlugin.getDefault().getScriptCodeScanner();
		PythonScriptColorProvider provider = PythonEditorPlugin.getDefault().getScriptColorProvider();
		java.util.function.BiFunction<String, String, TextAttribute> attribute = (key, bold) -> new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, key)), null,
				store.getBoolean(bold) ? SWT.BOLD : SWT.NORMAL);
		scanner.keyword.setData(attribute.apply("colourkey", "BOLD_COLOURKEY"));
		scanner.type.setData(attribute.apply("colourkey1", "BOLD_COLOURKEY1"));
		scanner.string.setData(attribute.apply("colourkey2", "BOLD_COLOURKEY2"));
		scanner.comment.setData(attribute.apply("colourkey3", "BOLD_COLOURKEY3"));
		scanner.defaultOther.setData(attribute.apply("colourkey4", "BOLD_COLOURKEY4"));
		scanner.operators.setData(attribute.apply("colourkey5", "BOLD_COLOURKEY5"));
		scanner.numbers.setData(attribute.apply("colourkey7", "BOLD_COLOURKEY7"));
		scanner.decorator.setData(attribute.apply("colourkey8", "BOLD_COLOURKEY8"));
		scanner.className.setData(attribute.apply("colourkey9", "BOLD_COLOURKEY9"));
		scanner.funcName.setData(attribute.apply("colourkey10", "BOLD_COLOURKEY10"));
		scanner.parenthesis.setData(attribute.apply("colourkey12", "BOLD_COLOURKEY12"));
		scanner.functionCall.setData(attribute.apply("colourkey13", "BOLD_COLOURKEY13"));
		for (PythonEditor editor : com.eco.bio7.util.Util.getOpenEditors(PythonEditor.class)) {
			editor.invalidateText();
		}
	}

}
