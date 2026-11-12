package com.eco.bio7.rpreferences;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.format.RFormatterSupport;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;

public class FormatPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, PropertyChangeListener {

	/**
	 * Create the preference page.
	 */
	public FormatPreferences() {
		super(GRID);
		setPreferenceStore(Bio7REditorPlugin.getDefault().getPreferenceStore());
		setTitle("Preferences Formatting");
	}

	/**
	 * Create contents of the preference page.
	 */
	@Override
	protected void createFieldEditors() {
		addField(new ComboFieldEditor(RFormatterSupport.FORMATTER, "Formatter:",
				new String[][] { { "Bio7 (built-in, no R needed)", RFormatterSupport.JAVA }, { "formatR (R package)", RFormatterSupport.FORMATR },
						{ "styler (R package)", RFormatterSupport.STYLER } },
				getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Bio7 (built-in):", getFieldEditorParent()));
		IntegerFieldEditor indentSize = new IntegerFieldEditor(RFormatterSupport.INDENT_SIZE, "Spaces per indentation level:", getFieldEditorParent());
		indentSize.setValidRange(0, 16);
		addField(indentSize);
		addField(new BooleanFieldEditor(RFormatterSupport.USE_TABS, "Indent with tabs", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		IntegerFieldEditor continuation = new IntegerFieldEditor(RFormatterSupport.CONTINUATION_INDENT, "Continuation indentation (levels):", getFieldEditorParent());
		continuation.setValidRange(0, 4);
		addField(continuation);
		addField(new BooleanFieldEditor(RFormatterSupport.SPACE_OPERATORS, "Spaces around operators ('x <- a + b')", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor(RFormatterSupport.SPACE_AFTER_COMMA, "Space after a comma", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor(RFormatterSupport.SPACE_KEYWORD_PARENTHESIS, "Space after 'if', 'for', 'while' ('if (x)')", BooleanFieldEditor.DEFAULT,
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(RFormatterSupport.REPLACE_ASSIGNMENT, "Replace the assignment '=' with '<-'", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor(RFormatterSupport.BRACE_NEW_LINE, "Set '{' on a new line", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		IntegerFieldEditor blankLines = new IntegerFieldEditor(RFormatterSupport.MAX_BLANK_LINES, "Maximum blank lines in a row:", getFieldEditorParent());
		blankLines.setValidRange(0, 99);
		addField(blankLines);
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("styler (R package):", getFieldEditorParent()));
		addField(new StringFieldEditor("STYLER_PACKAGE_ARGUMENTS", "styler package arguments\r\n", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("formatR (R package):", getFieldEditorParent()));
		addField(new BooleanFieldEditor("FORMAT_COMMENTS", "Keep comments", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor("FORMAT_BLANK_LINES", "Keep blank lines", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor("FORMAT_REPLACE_ASSIGNMENT", "Replace '=' with '<-'", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor("FORMAT_BRACES_NEWLINE", "Set '{' on a new line", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new IntegerFieldEditor("FORMAT_NUMBER_SPACES", "Spaces indention:", getFieldEditorParent()));
		addField(new IntegerFieldEditor("FORMAT_MINIMUM_LINE", "Line width:", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		final Link link = new Link(getFieldEditorParent(), SWT.NONE);
		link.setText("See <a href=\"http://yihui.name/formatR\">'formatR'</a> website for more information!");
		link.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));

		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {

				try {
					// Open the default browser!
					PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(e.text));
				} catch (PartInitException ex) {
					// TODO Auto-generated catch block
					ex.printStackTrace();
				} catch (MalformedURLException ex) {
					// TODO Auto-generated catch block
					ex.printStackTrace();
				}

			}
		});
	}

	/**
	 * Initialize the preference page.
	 */
	public void init(IWorkbench workbench) {
		// Initialize the preference page
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		// TODO Auto-generated method stub

	}

}
