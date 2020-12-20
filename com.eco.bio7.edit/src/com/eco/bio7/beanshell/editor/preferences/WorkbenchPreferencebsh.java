package com.eco.bio7.beanshell.editor.preferences;

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
import com.eco.bio7.editor.BeanshellEditorPlugin;
import com.eco.bio7.editors.BeanshellEditor;
import com.eco.bio7.editors.ScriptCodeScanner;
import com.eco.bio7.editors.ScriptColorProvider;

public class WorkbenchPreferencebsh extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	private IPreferenceStore store;

	public WorkbenchPreferencebsh() {
		super(GRID);
		store = BeanshellEditorPlugin.getDefault().getPreferenceStore();
		setPreferenceStore(BeanshellEditorPlugin.getDefault().getPreferenceStore());
		setDescription("Bio7 BeanShell/Grovvy Editor");
	}

	public void createFieldEditors() {

		final Link link = new Link(getFieldEditorParent(), SWT.NONE);
		link.setText("See <a href=\"org.eclipse.ui.preferencePages.ColorsAndFonts\">'Colors and Fonts'</a> to configure the font.");
		link.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));

		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				PreferencesUtil.createPreferenceDialogOn(new Shell(Display.getDefault()), "org.eclipse.ui.preferencePages.ColorsAndFonts", null, "selectFont:com.eco.bio7.beanshelleditor.textfont");

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

		addField(new ColorFieldEditor("colourkey6", "Braces:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BOLD_COLOURKEY6", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addField(new ColorFieldEditor("colourkey7", "Numbers:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BOLD_COLOURKEY7", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

	}

	public void init(IWorkbench workbench) {

		IPreferenceStore store = BeanshellEditorPlugin.getDefault().getPreferenceStore();
		boolean isDark = Display.isSystemDarkTheme();

		if (isDark) {

			PreferenceConverter.setDefault(store, "colourkey", new RGB(167, 236, 33));
			PreferenceConverter.setDefault(store, "colourkey1", new RGB(177, 102, 218));
			PreferenceConverter.setDefault(store, "colourkey2", new RGB(23, 198, 163));
			PreferenceConverter.setDefault(store, "colourkey3", new RGB(128, 128, 128));
			PreferenceConverter.setDefault(store, "colourkey4", new RGB(219, 176, 78));
			PreferenceConverter.setDefault(store, "colourkey5", new RGB(230, 230, 250));
			PreferenceConverter.setDefault(store, "colourkey6", new RGB(250, 243, 243));
			PreferenceConverter.setDefault(store, "colourkey7", new RGB(104, 151, 187));

		} else {
			PreferenceConverter.setDefault(store, "colourkey", new RGB(127, 0, 85));
			PreferenceConverter.setDefault(store, "colourkey1", new RGB(127, 0, 85));
			PreferenceConverter.setDefault(store, "colourkey2", new RGB(42, 0, 255));
			PreferenceConverter.setDefault(store, "colourkey3", new RGB(128, 128, 128));
			PreferenceConverter.setDefault(store, "colourkey4", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(store, "colourkey5", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(store, "colourkey6", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(store, "colourkey7", new RGB(0, 0, 0));
		}

	}

	@Override
	protected void performDefaults() {

		super.performDefaults();
		ScopedPreferenceStore storeWorkbench = new ScopedPreferenceStore(new InstanceScope(), "org.eclipse.ui.workbench");
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor != null && editor instanceof BeanshellEditor) {
			BeanshellEditor beanShellEditor = (BeanshellEditor) editor;
			BeanshellEditorPlugin fginstance = BeanshellEditorPlugin.getDefault();
			ScriptCodeScanner scanner = (ScriptCodeScanner) fginstance.getScriptCodeScanner();
			ScriptColorProvider provider = BeanshellEditorPlugin.getDefault().getScriptColorProvider();

			storeWorkbench.setValue("com.eco.bio7.beanshelleditor.textfont", storeWorkbench.getDefaultString("com.eco.bio7.beanshelleditor.textfont"));
			PreferenceConverter.setValue(store, "colourkeyfont", JFaceResources.getFontRegistry().get("com.eco.bio7.beanshelleditor.textfont").getFontData());

			scanner.keyword.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey")), null, 1));
			scanner.type.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey1")), null, 1));
			scanner.string.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey2")), null, 0));
			scanner.comment.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey3")), null, 0));
			scanner.other.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey4")), null, 0));
			scanner.operators.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey5")), null, 0));
			scanner.braces.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey6")), null, 0));
			scanner.numbers.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey7")), null, 0));
			beanShellEditor.invalidateText();
		}
		super.performOk();
	}

	public void propertyChange(org.eclipse.jface.util.PropertyChangeEvent event) {
		super.propertyChange(event);
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor != null && editor instanceof BeanshellEditor) {
			BeanshellEditor beanshellEditor = (BeanshellEditor) editor;
			IPreferenceStore store = BeanshellEditorPlugin.getDefault().getPreferenceStore();

			if (event.getSource() instanceof ColorFieldEditor) {
				ColorFieldEditor col = (ColorFieldEditor) event.getSource();
				String name = col.getPreferenceName();
				RGB rgb = (RGB) event.getNewValue();
				BeanshellEditorPlugin fginstance = BeanshellEditorPlugin.getDefault();
				ScriptCodeScanner scanner = (ScriptCodeScanner) fginstance.getScriptCodeScanner();
				ScriptColorProvider provider = BeanshellEditorPlugin.getDefault().getScriptColorProvider();

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
					scanner.other.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY4")));
					break;
				case "colourkey5":
					scanner.operators.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY5")));
					break;
				case "colourkey6":
					scanner.braces.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY6")));
					break;
				case "colourkey7":
					scanner.numbers.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY7")));
					break;
				default:
					break;
				}

			}

			else if (event.getSource() instanceof BooleanFieldEditor) {
				BooleanFieldEditor col = (BooleanFieldEditor) event.getSource();
				String name = col.getPreferenceName();

				boolean fontData = (boolean) event.getNewValue();

				BeanshellEditorPlugin fginstance = BeanshellEditorPlugin.getDefault();
				ScriptCodeScanner scanner = (ScriptCodeScanner) fginstance.getScriptCodeScanner();
				ScriptColorProvider provider = BeanshellEditorPlugin.getDefault().getScriptColorProvider();

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
						scanner.other.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey4")), null, SWT.BOLD));
					} else {
						scanner.other.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey4")), null, SWT.NORMAL));
					}
					break;
				case "BOLD_COLOURKEY5":
					if (fontData) {
						scanner.operators.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey5")), null, SWT.BOLD));
					} else {
						scanner.operators.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey5")), null, SWT.NORMAL));
					}
					break;
				case "BOLD_COLOURKEY6":
					if (fontData) {
						scanner.braces.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey6")), null, SWT.BOLD));
					} else {
						scanner.braces.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey6")), null, SWT.NORMAL));
					}
					break;
				case "BOLD_COLOURKEY7":
					if (fontData) {
						scanner.numbers.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey7")), null, SWT.BOLD));
					} else {
						scanner.numbers.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey7")), null, SWT.NORMAL));
					}
					break;

				default:
					break;
				}

			}

			beanshellEditor.invalidateText();
		}
		super.performOk();

	}

	private int isBold(String string2) {
		int style = 0;
		IPreferenceStore store = BeanshellEditorPlugin.getDefault().getPreferenceStore();
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