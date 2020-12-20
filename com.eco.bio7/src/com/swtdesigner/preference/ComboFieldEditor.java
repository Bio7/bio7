package com.swtdesigner.preference;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
/**
 * A field editor for a combo box that allows the drop-down selection of one of a list of items.
 * 
 * This class is copied from Eclipse.
 */
public class ComboFieldEditor extends FieldEditor {
	/**
	 * The <code>Combo</code> widget.
	 */
	Combo fCombo;
	/**
	 * The value (not the name) of the currently selected item in the Combo widget.
	 */
	String fValue;
	/**
	 * The names (labels) and underlying values to populate the combo widget.  These should be
	 * arranged as: { {name1, value1}, {name2, value2}, ...}
	 */
	private String[][] fEntryNamesAndValues;
	/**
	 * Create the combo field editor
	 * @param name
	 * @param labelText
	 * @param entryNamesAndValues
	 * @param parent
	 */
	public ComboFieldEditor(String name, String labelText, String[][] entryNamesAndValues, Composite parent) {
		init(name, labelText);
		Assert.isTrue(checkArray(entryNamesAndValues));
		this.fEntryNamesAndValues = entryNamesAndValues;
		createControl(parent);
	}
	/**
	 * Checks whether given <code>String[][]</code> is of "type" 
	 * <code>String[][2]</code>.
	 * @param table 
	 *
	 * @return <code>true</code> if it is ok, and <code>false</code> otherwise
	 */
	private boolean checkArray(String[][] table) {
		if (table == null) {
			return false;
		}
		for (int i = 0; i < table.length; i++) {
			String[] array = table[i];
			if (array == null || array.length != 2) {
				return false;
			}
		}
		return true;
	}
	/**
	 * @see FieldEditor#adjustForNumColumns(int)
	 */
	@Override
	protected void adjustForNumColumns(int numColumns) {
		if (numColumns > 1) {
			Control control = getLabelControl();
			int left = numColumns;
			if (control != null) {
				((GridData) control.getLayoutData()).horizontalSpan = 1;
				left = left - 1;
			}
			((GridData) this.fCombo.getLayoutData()).horizontalSpan = left;
		} else {
			Control control = getLabelControl();
			if (control != null) {
				((GridData) control.getLayoutData()).horizontalSpan = 1;
			}
			((GridData) this.fCombo.getLayoutData()).horizontalSpan = 1;
		}
	}
	/**
	 * @see FieldEditor#doFillIntoGrid(Composite, int)
	 */
	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		int comboC = 1;
		if (numColumns > 1) {
			comboC = numColumns - 1;
		}
		Control control = getLabelControl(parent);
		GridData gd = new GridData();
		gd.horizontalSpan = 1;
		control.setLayoutData(gd);
		control = getComboBoxControl(parent);
		gd = new GridData();
		gd.horizontalSpan = comboC;
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		control.setLayoutData(gd);
		control.setFont(parent.getFont());
	}
	/**
	 * @see FieldEditor#doLoad()
	 */
	@Override
	protected void doLoad() {
		updateComboForValue(getPreferenceStore().getString(getPreferenceName()));
	}
	/**
	 * @see FieldEditor#doLoadDefault()
	 */
	@Override
	protected void doLoadDefault() {
		updateComboForValue(getPreferenceStore().getDefaultString(getPreferenceName()));
	}
	/**
	 * @see FieldEditor#doStore()
	 */
	@Override
	protected void doStore() {
		if (this.fValue == null) {
			getPreferenceStore().setToDefault(getPreferenceName());
			return;
		}
		getPreferenceStore().setValue(getPreferenceName(), this.fValue);
	}
	/**
	 * @see FieldEditor#getNumberOfControls()
	 */
	@Override
	public int getNumberOfControls() {
		return 2;
	}
	/**
	 * Lazily create and return the Combo control.
	 * @param parent 
	 * @return Combo
	 */
	public Combo getComboBoxControl(Composite parent) {
		if (this.fCombo == null) {
			this.fCombo = new Combo(parent, SWT.READ_ONLY);
			this.fCombo.setFont(parent.getFont());
			for (int i = 0; i < this.fEntryNamesAndValues.length; i++) {
				this.fCombo.add(this.fEntryNamesAndValues[i][0], i);
			}
			this.fCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					String oldValue = ComboFieldEditor.this.fValue;
					String name = ComboFieldEditor.this.fCombo.getText();
					ComboFieldEditor.this.fValue = getValueForName(name);
					setPresentsDefaultValue(false);
					fireValueChanged(VALUE, oldValue, ComboFieldEditor.this.fValue);
				}
			});
		}
		return this.fCombo;
	}
	@Override
	protected void setPresentsDefaultValue(boolean b) {
		super.setPresentsDefaultValue(b);
	}
	@Override
	protected void fireValueChanged(String property, Object oldValue, Object newValue) {
		super.fireValueChanged(property, oldValue, newValue);
	}
	/**
	 * Given the name (label) of an entry, return the corresponding value.
	 * @param name String
	 * @return String
	 */
	protected String getValueForName(String name) {
		for (int i = 0; i < this.fEntryNamesAndValues.length; i++) {
			String[] entry = this.fEntryNamesAndValues[i];
			if (name.equals(entry[0])) {
				return entry[1];
			}
		}
		return this.fEntryNamesAndValues[0][0];
	}
	/**
	 * Set the name in the combo widget to match the specified value.
	 * @param value 
	 */
	protected void updateComboForValue(String value) {
		this.fValue = value;
		for (int i = 0; i < this.fEntryNamesAndValues.length; i++) {
			if (value.equals(this.fEntryNamesAndValues[i][1])) {
				this.fCombo.setText(this.fEntryNamesAndValues[i][0]);
				return;
			}
		}
		if (this.fEntryNamesAndValues.length > 0) {
			this.fValue = this.fEntryNamesAndValues[0][1];
		}
	}
}