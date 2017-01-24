package com.eco.bio7.markdownedit.preferences;

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
				//create an instance of the custom MyPreference class
				PreferencesUtil.createPreferenceDialogOn(new Shell(Display.getDefault()),"org.eclipse.ui.preferencePages.ColorsAndFonts", null, "selectFont:com.eco.bio7.reditor.markdown.textfont");

				
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

		

	}

	public void init(IWorkbench workbench) {

		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		PreferenceConverter.setDefault(store, "colourkey", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(store, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(store, "colourkey2", new RGB(42, 0, 255));
		PreferenceConverter.setDefault(store, "colourkey3", new RGB(128, 128, 128));
		
	}

	@Override
	protected void performDefaults() {

		super.performDefaults();
		ScopedPreferenceStore storeWorkbench = new ScopedPreferenceStore(new InstanceScope(), "org.eclipse.ui.workbench");
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		MarkdownEditor rEditor = (MarkdownEditor) editor;
		Activator fginstance = Activator.getDefault();
		MarkdownScanner scanner = (MarkdownScanner) fginstance.getMarkdownScanner();
		MarkdownColorProvider provider = Activator.getDefault().getRColorProvider();

		// JFaceResources.getFontRegistry().get("com.eco.bio7.reditor.reditor.textfont").getFontData()

		storeWorkbench.setValue("com.eco.bio7.reditor.markdown.textfont", storeWorkbench.getDefaultString("com.eco.bio7.reditor.markdown.textfont"));
		PreferenceConverter.setValue(store, "colourkeyfont", JFaceResources.getFontRegistry().get("com.eco.bio7.reditor.markdown.textfont").getFontData());
		// rEditor.fontRegistry.put("colourkeyfont",
		// JFaceResources.getFontRegistry().get("com.eco.bio7.reditor.reditor.textfont").getFontData());
		/*scanner.keyword.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey")), null, 1));
		scanner.type.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey1")), null, 1));*/
		rEditor.getMarkConf().comment.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey2")), null, 0));
		rEditor.getMarkConf().yaml.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getDefaultColor(store, "colourkey3")), null, 0));
		
		rEditor.invalidateText();
		super.performOk();
	}

	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		MarkdownEditor rEditor = (MarkdownEditor) editor;
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		if (event.getSource() instanceof ColorFieldEditor) {
			ColorFieldEditor col = (ColorFieldEditor) event.getSource();
			String name = col.getPreferenceName();
			RGB rgb = (RGB) event.getNewValue();
			Activator fginstance = Activator.getDefault();
			MarkdownScanner scanner = (MarkdownScanner) fginstance.getMarkdownScanner();
			MarkdownColorProvider provider = Activator.getDefault().getRColorProvider();

			switch (name) {
			/*case "colourkey":
				scanner.keyword.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY")));
				break;
			case "colourkey1":
				scanner.type.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY1")));
				break;*/
			case "colourkey2":
				rEditor.getMarkConf().comment.att.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY2")));
				break;
			case "colourkey3":
				rEditor.getMarkConf().yaml.att.setData(new TextAttribute(provider.getColor(rgb), null, isBold("BOLD_COLOURKEY3")));
				break;
			
			default:
				break;
			}

		}

		else if (event.getSource() instanceof BooleanFieldEditor) {
			BooleanFieldEditor col = (BooleanFieldEditor) event.getSource();
			String name = col.getPreferenceName();

			boolean fontData = (boolean) event.getNewValue();

			Activator fginstance = Activator.getDefault();
			MarkdownScanner scanner = (MarkdownScanner) fginstance.getMarkdownScanner();
			MarkdownColorProvider provider = Activator.getDefault().getRColorProvider();

			switch (name) {
			/*case "BOLD_COLOURKEY":

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

				break;*/
			case "BOLD_COLOURKEY2":
				if (fontData) {
					rEditor.getMarkConf().comment.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey2")), null, SWT.BOLD));
				} else {
					rEditor.getMarkConf().comment.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey2")), null, SWT.NORMAL));
				}
				break;
			case "BOLD_COLOURKEY3":
				if (fontData) {
					rEditor.getMarkConf().yaml.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey3")), null, SWT.BOLD));
				} else {
					rEditor.getMarkConf().yaml.att.setData(new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey3")), null, SWT.NORMAL));
				}
				break;
			

			default:
				break;
			}

		}

		rEditor.invalidateText();
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
