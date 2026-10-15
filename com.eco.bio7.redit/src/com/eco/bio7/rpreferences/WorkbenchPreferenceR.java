package com.eco.bio7.rpreferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditors.RCodeScanner;
import com.eco.bio7.reditors.RColorProvider;
import com.eco.bio7.reditors.REditor;

public class WorkbenchPreferenceR extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private IPreferenceStore store;

	public WorkbenchPreferenceR() {
		super(GRID);
		store = Bio7REditorPlugin.getDefault().getPreferenceStore();
		setPreferenceStore(store);
		setDescription("Bio7 R Editor");
	}

	public void createFieldEditors() {

		final Link link = new Link(getFieldEditorParent(), SWT.NONE);
		link.setText("See <a href=\"org.eclipse.ui.preferencePages.ColorsAndFonts\">'Colors and Fonts'</a> to configure the font.");
		link.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));

		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				// create an instance of the custom MyPreference class
				PreferencesUtil.createPreferenceDialogOn(new Shell(Display.getDefault()), "org.eclipse.ui.preferencePages.ColorsAndFonts", null, "selectFont:com.eco.bio7.reditor.reditor.textfont");

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

		addField(new ColorFieldEditor("colourkey8", "Assignment:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BOLD_COLOURKEY8", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

	}

	public void init(IWorkbench workbench) {

		IPreferenceStore store = Bio7REditorPlugin.getDefault().getPreferenceStore();
		PreferenceInitializer.setColorDefaults(store, com.eco.bio7.util.Util.isDarkTheme());

	}

	@Override
	protected void performDefaults() {

		super.performDefaults();
		ScopedPreferenceStore storeWorkbench = new ScopedPreferenceStore(new InstanceScope(), "org.eclipse.ui.workbench");
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor != null && editor instanceof REditor) {
			REditor rEditor = (REditor) editor;
			Bio7REditorPlugin fginstance = Bio7REditorPlugin.getDefault();
			RCodeScanner scanner = (RCodeScanner) fginstance.getRCodeScanner();
			RColorProvider provider = Bio7REditorPlugin.getDefault().getRColorProvider();

			// JFaceResources.getFontRegistry().get("com.eco.bio7.reditor.reditor.textfont").getFontData()

			storeWorkbench.setValue("com.eco.bio7.reditor.reditor.textfont", storeWorkbench.getDefaultString("com.eco.bio7.reditor.reditor.textfont"));
			PreferenceConverter.setValue(store, "colourkeyfont", JFaceResources.getFontRegistry().get("com.eco.bio7.reditor.reditor.textfont").getFontData());
			// rEditor.fontRegistry.put("colourkeyfont",
			// JFaceResources.getFontRegistry().get("com.eco.bio7.reditor.reditor.textfont").getFontData());
			scanner.keyword.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey")), null, 1));
			scanner.type.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey1")), null, 1));
			rEditor.getRconf().single.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey2")), null, 0));
			rEditor.getRconf().comment.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey3")), null, 0));
			scanner.other.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey4")), null, 0));
			scanner.operators.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey5")), null, 0));
			scanner.braces.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey6")), null, 0));
			scanner.numbers.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey7")), null, 0));
			scanner.assignment.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey8")), null, 0));
			rEditor.invalidateText();
		}
		super.performOk();
	}

	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor != null && editor instanceof REditor) {
			REditor rEditor = (REditor) editor;
			IPreferenceStore store = Bio7REditorPlugin.getDefault().getPreferenceStore();

			if (event.getSource() instanceof ColorFieldEditor) {
				ColorFieldEditor col = (ColorFieldEditor) event.getSource();
				String name = col.getPreferenceName();
				RGB rgb = (RGB) event.getNewValue();
				Bio7REditorPlugin fginstance = Bio7REditorPlugin.getDefault();
				RCodeScanner scanner = (RCodeScanner) fginstance.getRCodeScanner();
				RColorProvider provider = Bio7REditorPlugin.getDefault().getRColorProvider();

				switch (name) {
				case "colourkey":
					scanner.keyword.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY")));
					break;
				case "colourkey1":
					scanner.type.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY1")));
					break;
				case "colourkey2":
					rEditor.getRconf().single.att.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY2")));
					break;
				case "colourkey3":
					rEditor.getRconf().comment.att.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY3")));
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
				case "colourkey8":
					scanner.assignment.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY8")));
					break;
				default:
					break;
				}

			}

			else if (event.getSource() instanceof BooleanFieldEditor) {
				BooleanFieldEditor col = (BooleanFieldEditor) event.getSource();
				String name = col.getPreferenceName();

				boolean fontData = (boolean) event.getNewValue();

				Bio7REditorPlugin fginstance = Bio7REditorPlugin.getDefault();
				RCodeScanner scanner = (RCodeScanner) fginstance.getRCodeScanner();
				RColorProvider provider = Bio7REditorPlugin.getDefault().getRColorProvider();

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
						rEditor.getRconf().single.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey2")), null, SWT.BOLD));
					} else {
						rEditor.getRconf().single.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey2")), null, SWT.NORMAL));
					}
					break;
				case "BOLD_COLOURKEY3":
					if (fontData) {
						rEditor.getRconf().comment.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey3")), null, SWT.BOLD));
					} else {
						rEditor.getRconf().comment.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey3")), null, SWT.NORMAL));
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
				case "BOLD_COLOURKEY8":
					if (fontData) {
						scanner.assignment.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey8")), null, SWT.BOLD));
					} else {
						scanner.assignment.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey8")), null, SWT.NORMAL));
					}
					break;

				default:
					break;
				}

			}

			rEditor.invalidateText();
		}
		super.performOk();

	}

	private int isBold(String string2) {
		int style = 0;
		IPreferenceStore store = Bio7REditorPlugin.getDefault().getPreferenceStore();
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
	 * Updates the colors of the open R editors from the preferences (e.g., after
	 * the theme has been changed between light and dark).
	 */
	public static void updateEditorColors() {
		IPreferenceStore store = Bio7REditorPlugin.getDefault().getPreferenceStore();
		RCodeScanner scanner = (RCodeScanner) Bio7REditorPlugin.getDefault().getRCodeScanner();
		RColorProvider provider = Bio7REditorPlugin.getDefault().getRColorProvider();
		java.util.function.BiFunction<String, String, TextAttribute> attribute = (key, bold) -> new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, key)), null,
				store.getBoolean(bold) ? SWT.BOLD : SWT.NORMAL);
		scanner.keyword.setData(attribute.apply("colourkey", "BOLD_COLOURKEY"));
		scanner.type.setData(attribute.apply("colourkey1", "BOLD_COLOURKEY1"));
		scanner.other.setData(attribute.apply("colourkey4", "BOLD_COLOURKEY4"));
		scanner.operators.setData(attribute.apply("colourkey5", "BOLD_COLOURKEY5"));
		scanner.braces.setData(attribute.apply("colourkey6", "BOLD_COLOURKEY6"));
		scanner.numbers.setData(attribute.apply("colourkey7", "BOLD_COLOURKEY7"));
		scanner.assignment.setData(attribute.apply("colourkey8", "BOLD_COLOURKEY8"));
		for (REditor editor : com.eco.bio7.util.Util.getOpenEditors(REditor.class)) {
			editor.getRconf().single.att.setData(attribute.apply("colourkey2", "BOLD_COLOURKEY2"));
			editor.getRconf().comment.att.setData(attribute.apply("colourkey3", "BOLD_COLOURKEY3"));
			editor.invalidateText();
		}
	}

}
