/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.eco.bio7.Bio7Plugin;

/**
 * The preferences of the Python Plot view (the matplotlib figures of the
 * embedded Python), as "Preferences R Plots" for R. Applied at once to a running
 * Python (the open figures are drawn again, a style applies to new figures).
 */
public class PythonPlotPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public PythonPlotPreferences() {
		super(GRID);
		setPreferenceStore(Bio7Plugin.getDefault().getPreferenceStore());
		setDescription("Python Plot (matplotlib figures in the Python Plot view)");
	}

	@Override
	public void createFieldEditors() {
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Plot:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("PY_PLOT_FIT", "Draw the figures in the size of the view (else in their size, figsize)", getFieldEditorParent()));
		addField(new BooleanFieldEditor("PY_PLOT_HIDPI", "Draw with the resolution of the screen (sharp on HiDPI/Retina screens)", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Style (for new figures):", getFieldEditorParent()));
		addField(new ComboFieldEditor("PY_PLOT_STYLE", "matplotlib style:",
				new String[][] { { "Default of matplotlib", "" }, { "ggplot", "ggplot" }, { "seaborn", "seaborn-v0_8" },
						{ "seaborn (white grid)", "seaborn-v0_8-whitegrid" }, { "bmh", "bmh" }, { "fivethirtyeight", "fivethirtyeight" },
						{ "classic", "classic" }, { "grayscale", "grayscale" }, { "dark background", "dark_background" },
						{ "colorblind (tableau)", "tableau-colorblind10" }, { "Solarize light", "Solarize_Light2" } },
				getFieldEditorParent()));
		IntegerFieldEditor font = new IntegerFieldEditor("PY_PLOT_FONT_SIZE", "Font size (points, 0: the default of the style)", getFieldEditorParent());
		font.setValidRange(0, 72);
		addField(font);
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Save:", getFieldEditorParent()));
		IntegerFieldEditor dpi = new IntegerFieldEditor("PY_PLOT_SAVE_DPI", "Resolution of saved PNG images (dpi, 0: the resolution of the figure)",
				getFieldEditorParent());
		dpi.setValidRange(0, 2400);
		addField(dpi);
	}

	@Override
	public void init(IWorkbench workbench) {
	}
}
