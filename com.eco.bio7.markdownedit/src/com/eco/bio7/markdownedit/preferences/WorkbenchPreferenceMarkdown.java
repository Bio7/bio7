package com.eco.bio7.markdownedit.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.StringFieldEditor;
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
import com.eco.bio7.markdownedit.Activator;
import com.eco.bio7.markdownedit.editors.MarkdownColorProvider;
import com.eco.bio7.markdownedit.editors.MarkdownEditor;
import com.eco.bio7.markdownedit.editors.MarkdownScanner;

public class WorkbenchPreferenceMarkdown extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private IPreferenceStore store;

	public WorkbenchPreferenceMarkdown() {
		super(GRID);
		store = Activator.getDefault().getPreferenceStore();
		setPreferenceStore(store);
		setDescription("Bio7 Markdown Editor");
	}

	public void createFieldEditors() {

		final Link link = new Link(getFieldEditorParent(), SWT.NONE);
		link.setText("See <a href=\"org.eclipse.ui.preferencePages.ColorsAndFonts\">'Colors and Fonts'</a> to configure the font.");
		link.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));

		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				// create an instance of the custom MyPreference class
				PreferencesUtil.createPreferenceDialogOn(new Shell(Display.getDefault()), "org.eclipse.ui.preferencePages.ColorsAndFonts", null, "selectFont:com.eco.bio7.reditor.markdown.textfont");

			}
		});
		addField(new SpacerFieldEditor(getFieldEditorParent()));

		addField(new ColorFieldEditor("colourkey", "Colour Default:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BOLD_COLOURKEY", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addField(new ColorFieldEditor("colourkey1", "Colour Header:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BOLD_COLOURKEY1", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addField(new ColorFieldEditor("colourkey2", "Colour R Chunk:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("BOLD_COLOURKEY2", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		//addField(new ColorFieldEditor("colourkey3", "Colour YAML Header:", getFieldEditorParent()));
		//addField(new BooleanFieldEditor("BOLD_COLOURKEY3", "Bold", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));

		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Other:", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new BooleanFieldEditor("OPEN_WORD_IN_VIEW", "Open Word in Bio7 view", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new BooleanFieldEditor("RECONCILE_MARKDOWN", "Automatically compile markdown after editor changes", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		/*Here we use a string field editor because a integer field editor throws exceptions each time the value is empty!*/
		addField(new StringFieldEditor("RECONCILE_MARKDOWN_TIME", "Compile markdown interval (in ms after last keystroke - restart necessary!)", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new BooleanFieldEditor("CHECK_SPELLING", "Enable Spell Checking", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		final Link link2 = new Link(getFieldEditorParent(), SWT.NONE);
		link2.setText("See <a href=\"com.eco.bio7.browser.preferences\">'Browser Preferences'</a> to select or configure the browser.");
		link2.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));

		link2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				// create an instance of the custom MyPreference class
				PreferencesUtil.createPreferenceDialogOn(new Shell(Display.getDefault()), "com.eco.bio7.browser.preferences", null, null);

			}
		});

	}

	public void init(IWorkbench workbench) {

		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		PreferenceConverter.setDefault(store, "colourkey", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(store, "colourkey2", new RGB(42, 0, 255));
		
		//PreferenceConverter.setDefault(store, "colourkey3", new RGB(128, 128, 128));

	}

	@Override
	protected void performDefaults() {

		super.performDefaults();
		ScopedPreferenceStore storeWorkbench = new ScopedPreferenceStore(new InstanceScope(), "org.eclipse.ui.workbench");
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor != null && editor instanceof MarkdownEditor) {
			MarkdownEditor markdownEditor = (MarkdownEditor) editor;
			MarkdownColorProvider provider = Activator.getDefault().getMarkdownColorProvider();

			// JFaceResources.getFontRegistry().get("com.eco.bio7.reditor.reditor.textfont").getFontData()

			storeWorkbench.setValue("com.eco.bio7.reditor.markdown.textfont", storeWorkbench.getDefaultString("com.eco.bio7.reditor.markdown.textfont"));
			PreferenceConverter.setValue(store, "colourkeyfont", JFaceResources.getFontRegistry().get("com.eco.bio7.reditor.markdown.textfont").getFontData());
			// rEditor.fontRegistry.put("colourkeyfont",
			// JFaceResources.getFontRegistry().get("com.eco.bio7.reditor.reditor.textfont").getFontData());
			/*
			 * scanner.keyword.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey")), null, 1)); scanner.type.setData(new
			 * TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey1")), null, 1));
			 */
			MarkdownScanner scanner = markdownEditor.getMarkConf().getMarkdownScanner();
			scanner.other.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey")), null, 0));
			scanner.head.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey1")), null, 0));
			/*We now use the R editor colors instead of colorkey2!*/
			markdownEditor.getMarkConf().comment.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey2")), null, 0));
			markdownEditor.getMarkConf().yaml.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey3")), null, 0));

			markdownEditor.invalidateText();
		}
		super.performOk();
	}

	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor != null && editor instanceof MarkdownEditor) {
			MarkdownEditor markdownEditor = (MarkdownEditor) editor;
			IPreferenceStore store = Activator.getDefault().getPreferenceStore();

			if (event.getSource() instanceof ColorFieldEditor) {
				ColorFieldEditor col = (ColorFieldEditor) event.getSource();
				String name = col.getPreferenceName();
				RGB rgb = (RGB) event.getNewValue();
				// Activator fginstance = Activator.getDefault();

				MarkdownScanner scanner = markdownEditor.getMarkConf().getMarkdownScanner();

				MarkdownColorProvider provider = Activator.getDefault().getMarkdownColorProvider();

				switch (name) {

				case "colourkey":

					scanner.other.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY")));
					break;

				case "colourkey1":
					scanner.head.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY1")));
					break;
					/*We now use the R editor colors instead of colorkey2!*/
				case "colourkey2":
					markdownEditor.getMarkConf().comment.att.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY2")));
					break;
				/*case "colourkey3":
					markdownEditor.getMarkConf().yaml.att.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY3")));
					break;*/

				default:
					break;
				}

			}

			else if (event.getSource() instanceof BooleanFieldEditor) {
				BooleanFieldEditor col = (BooleanFieldEditor) event.getSource();
				String name = col.getPreferenceName();

				boolean fontData = (boolean) event.getNewValue();

				MarkdownScanner scanner = markdownEditor.getMarkConf().getMarkdownScanner();

				MarkdownColorProvider provider = Activator.getDefault().getMarkdownColorProvider();

				switch (name) {

				case "BOLD_COLOURKEY":

					if (fontData) {
						scanner.other.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey")), null, SWT.BOLD));
					} else {
						scanner.other.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey")), null, SWT.NORMAL));
					}

					break;

				case "BOLD_COLOURKEY1":
					if (fontData) {
						scanner.head.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey1")), null, SWT.BOLD));
					} else {
						scanner.head.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey1")), null, SWT.NORMAL));
					}

					break;
					/*We now use the R editor colors instead of colorkey2!*/
				case "BOLD_COLOURKEY2":
					if (fontData) {
						markdownEditor.getMarkConf().comment.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey2")), null, SWT.BOLD));
					} else {
						markdownEditor.getMarkConf().comment.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey2")), null, SWT.NORMAL));
					}
					//break;
				/*case "BOLD_COLOURKEY3":
					if (fontData) {
						markdownEditor.getMarkConf().yaml.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey3")), null, SWT.BOLD));
					} else {
						markdownEditor.getMarkConf().yaml.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey3")), null, SWT.NORMAL));
					}
					break;*/

				default:
					break;
				}

			} /*
				 * else if (event.getSource() instanceof IntegerFieldEditor) { IntegerFieldEditor col = (IntegerFieldEditor) event.getSource(); String name = col.getPreferenceName();
				 * System.out.println(name); System.out.println(event.getNewValue()); int reconcileTime = Integer.parseInt((String)event.getNewValue());
				 * rEditor.getMarkConf().getReconciler().setDelay(reconcileTime);
				 * 
				 * }
				 */

			markdownEditor.invalidateText();
		}
		super.performOk();

	}

	private int isBold(String string2) {
		int style = 0;
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
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
