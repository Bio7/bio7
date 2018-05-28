package com.eco.bio7.javapreferences;

/*Source: http://www.eclipse.org/articles/Article-Field-Editors/field_editors.html*/

import java.util.StringTokenizer;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.javaeditor.Bio7EditorPlugin;

/**
 * A preference page for the error-handling of a simple HTML editor.
 */
public class ErrorsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final int VERTICAL_DIALOG_UNITS_PER_CHAR = 8;
	private static final int LIST_HEIGHT_IN_CHARS = 10;
	private static final int LIST_HEIGHT_IN_DLUS = LIST_HEIGHT_IN_CHARS * VERTICAL_DIALOG_UNITS_PER_CHAR;
	private String file;

	private List exemptTagsList;

	private Button removeTag;
	private String[] files;
	private String currentFilePath;

	public ErrorsPreferencePage() {
		super();

		// Set the preference store for the preference page.
		IPreferenceStore store = Bio7EditorPlugin.getDefault().getPreferenceStore();
		setPreferenceStore(store);
	}

	/**
	 * @see org.eclipse.jface.preference. PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite top = new Composite(parent, SWT.LEFT);

		// Sets the layout data for the top composite's
		// place in its parent's layout.
		top.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Sets the layout for the top composite's
		// children to populate.
		top.setLayout(new GridLayout());

		Label listLabel = new Label(top, SWT.NONE);
		listLabel.setText("Add Selected Libraries");

		exemptTagsList = new List(top, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		exemptTagsList.setItems(getExemptTagsPreference());
		// Create a data that takes up the extra space
		// in the dialog and spans both columns.
		GridData listData = new GridData(GridData.FILL_HORIZONTAL);
		listData.heightHint = convertVerticalDLUsToPixels(LIST_HEIGHT_IN_DLUS);
		exemptTagsList.setLayoutData(listData);

		exemptTagsList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selectionChanged();
			}
		});

		Composite addRemoveGroup = new Composite(top, SWT.NONE);
		addRemoveGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout addRemoveLayout = new GridLayout();
		addRemoveLayout.numColumns = 2;
		addRemoveLayout.marginHeight = 0;
		addRemoveLayout.marginWidth = 0;
		addRemoveGroup.setLayout(addRemoveLayout);

		// Create a composite for the add and remove buttons.
		Composite buttonGroup = new Composite(addRemoveGroup, SWT.NONE);
		GridLayout buttonLayout = new GridLayout();
		buttonLayout.marginHeight = 0;
		buttonLayout.marginWidth = 0;
		buttonGroup.setLayout(buttonLayout);

		Button addTag = new Button(buttonGroup, SWT.NONE);
		addTag.setText("Add Selected");
		addTag.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addTag();
			}
		});
		GridData addTagData = new GridData(GridData.FILL_HORIZONTAL);
		addTagData.heightHint = convertVerticalDLUsToPixels(IDialogConstants.BUTTON_HEIGHT);
		addTagData.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		addTag.setLayoutData(addTagData);

		removeTag = new Button(addRemoveGroup, SWT.NONE);
		removeTag.setText("&Remove Selected Libraries");
		removeTag.setEnabled(false);
		removeTag.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				int[] selection = exemptTagsList.getSelectionIndices();

				exemptTagsList.remove(selection);

				selectionChanged();
			}
		});

		return top;

	}

	/**
	 * @see IWorkbenchPreferencePage#init
	 */
	public void init(IWorkbench wb) {
	}

	/*
	 * The user has pressed "Restore defaults". Restore all default preferences.
	 */
	protected void performDefaults() {
		// errors.loadDefault();
		exemptTagsList.setItems(

				getDefaultExemptTagsPreference());
		// getDefaultExemptTagsPreference() is a convenience
		// method which retrieves the default preference from
		// the preference store.
		super.performDefaults();
	}

	/*
	 * The user has pressed Ok or Apply. Store/apply this page's values
	 * appropriately.
	 */
	public boolean performOk() {
		// errors.store();

		setExemptTagsPreference(exemptTagsList.getItems());

		return super.performOk();
	}

	private void addTag() {
		String[] tag = openMultipleFiles(new String[] { "*.jar", "*.class", "*" });
		if (tag != null && tag.length > 0) {
			for (int i = 0; i < tag.length; i++) {
				currentFilePath=currentFilePath.replace("\\", "/");
				String lib=currentFilePath+"/"+tag[i];
				exemptTagsList.add(lib);
			}

		}
		// textField.setText("");
	}

	/*
	 * Sets the enablement of the remove button depending on the selection in the
	 * list.
	 */
	private void selectionChanged() {
		int index = exemptTagsList.getSelectionIndex();
		removeTag.setEnabled(index >= 0);
	}

	public String[] getDefaultExemptTagsPreference() {
		return convert(Bio7EditorPlugin.getDefault().getPreferenceStore().getDefaultString("javaLibs"));
	}

	public String[] getExemptTagsPreference() {
		return convert(Bio7EditorPlugin.getDefault().getPreferenceStore().getString("javaLibs"));
	}

	private String[] convert(String preferenceValue) {
		StringTokenizer tokenizer = new StringTokenizer(preferenceValue, ";");
		int tokenCount = tokenizer.countTokens();
		String[] elements = new String[tokenCount];

		for (int i = 0; i < tokenCount; i++) {
			elements[i] = tokenizer.nextToken();
		}

		return elements;
	}

	public void setExemptTagsPreference(String[] elements) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < elements.length; i++) {
			buffer.append(elements[i]);
			buffer.append(";");
		}
		Bio7EditorPlugin.getDefault().getPreferenceStore().setValue("javaLibs", buffer.toString());
	}

	/**
	 * Opens a file-open dialog which displays the file with the given extensions.
	 * 
	 * @param extension
	 *            the extensions as a String array which should be displayed.
	 * @return a file path as a string from the file dialog.
	 */
	public String openFile(final String[] extension) {
		file = null;
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				Shell s = new Shell(SWT.ON_TOP);
				FileDialog fd = new FileDialog(s, SWT.OPEN);
				fd.setText("Select Libraries");

				fd.setFilterExtensions(extension);
				file = fd.open();
			}
		});
		return file;
	}

	/**
	 * Opens a file-open dialog for multiple selections.
	 * 
	 * @return a String array with the file paths of the selected files.
	 */
	public String[] openMultipleFiles(final String[] extension) {

		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				Shell shell = new Shell(display);
				FileDialog dlg = new FileDialog(shell, SWT.MULTI);
				dlg.setFilterPath(null);
				dlg.setFilterExtensions(extension);
				dlg.setText("Select *.class or *.jar files");
				String f = dlg.open();
				if (f != null) {

					files = dlg.getFileNames();
					
					currentFilePath = dlg.getFilterPath();
				}

			}
		});

		return files;
	}

}
