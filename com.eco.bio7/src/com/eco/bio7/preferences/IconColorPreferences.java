/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/

package com.eco.bio7.preferences;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.util.IconColors;

/**
 * The preferences of the icon colors. The Bio7 *.svg icons are displayed with
 * the color of the light or dark theme (applied after a restart). A preview
 * shows some icons with the selected colors.
 */
public class IconColorPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/* Some icons for the preview! */
	private static final String[] PREVIEW = { "icons/maintoolbar/play_pause.svg", "icons/maintoolbar/setup.svg", "icons/maintoolbar/r.svg",
			"icons/maintoolbar/create_project_file.svg", "icons/views/rview.svg", "icons/editor/java.svg", "icons/views/tableview.svg",
			"icons/views/console.svg", "icons/perspective/rimageanalysispersp.svg", "icons/views/deleteaction.svg" };

	/* Size of a preview row, distance of the icons and margin! */
	private static final int ROW = 32;
	private static final int STEP = 26;
	private static final int PAD = 10;

	private BooleanFieldEditor enabled;
	private ColorFieldEditor light;
	private ColorFieldEditor dark;
	private Canvas preview;
	private final List<Image> lightImages = new ArrayList<Image>();
	private final List<Image> darkImages = new ArrayList<Image>();
	private int previewCount;

	public IconColorPreferences() {
		super(GRID);
		setPreferenceStore(IconColors.getPreferenceStore());
		setDescription("The color of the Bio7 icons (*.svg). The Eclipse icons and the Bio7 logo keep their colors.\nA change is applied after a restart of Bio7.");
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		Composite parent = getFieldEditorParent();
		enabled = new BooleanFieldEditor(IconColors.ENABLED, "Use the icon colors", parent);
		addField(enabled);
		light = new ColorFieldEditor(IconColors.COLOR_LIGHT, "Icon color (light theme)", parent);
		addField(light);
		dark = new ColorFieldEditor(IconColors.COLOR_DARK, "Icon color (dark theme)", parent);
		addField(dark);
		Label label = new Label(parent, SWT.NONE);
		label.setText("Preview:");
		GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1);
		gd.verticalIndent = 12;
		label.setLayoutData(gd);
		/* A canvas, so that the light and dark background is not changed by the theme (CSS)! */
		preview = new Canvas(parent, SWT.DOUBLE_BUFFERED);
		GridData cd = new GridData(SWT.LEFT, SWT.TOP, false, false, 3, 1);
		cd.widthHint = PAD * 2 + PREVIEW.length * STEP - (STEP - 16);
		cd.heightHint = 2 * ROW;
		preview.setLayoutData(cd);
		preview.addPaintListener(e -> paintPreview(e.gc));
		preview.addDisposeListener(e -> disposeImages());
	}

	@Override
	protected void initialize() {
		super.initialize();
		updatePreview();
	}

	/* The icons with the light color on a light and with the dark color on a dark background! */
	private void paintPreview(GC gc) {
		Point size = preview.getSize();
		gc.setBackground(new Color(preview.getDisplay(), 0xf5, 0xf5, 0xf5));
		gc.fillRectangle(0, 0, size.x, ROW);
		gc.setBackground(new Color(preview.getDisplay(), 0x1e, 0x1e, 0x1e));
		gc.fillRectangle(0, ROW, size.x, ROW);
		for (int i = 0; i < lightImages.size(); i++) {
			gc.drawImage(lightImages.get(i), PAD + i * STEP, (ROW - 16) / 2);
		}
		for (int i = 0; i < darkImages.size(); i++) {
			gc.drawImage(darkImages.get(i), PAD + i * STEP, ROW + (ROW - 16) / 2);
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		super.propertyChange(event);
		updatePreview();
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		updatePreview();
	}

	/* Creates the preview icons with the selected colors! */
	private void updatePreview() {
		if (preview == null || preview.isDisposed()) {
			return;
		}
		boolean on = enabled.getBooleanValue();
		disposeImages();
		RGB lightColor = on ? light.getColorSelector().getColorValue() : IconColors.BIO7_GREEN;
		RGB darkColor = on ? dark.getColorSelector().getColorValue() : IconColors.BIO7_GREEN;
		for (String path : PREVIEW) {
			add(lightImages, previewImage(path, lightColor));
			add(darkImages, previewImage(path, darkColor));
		}
		light.setEnabled(on, getFieldEditorParent());
		dark.setEnabled(on, getFieldEditorParent());
		preview.redraw();
	}

	private static void add(List<Image> list, Image image) {
		if (image != null) {
			list.add(image);
		}
	}

	/* The recolored icon (a temporary file, so that the icon is sharp on high resolution displays)! */
	private Image previewImage(String path, RGB color) {
		URL url = FileLocator.find(Bio7Plugin.getDefault().getBundle(), IPath.fromPortableString(path));
		if (url == null) {
			return null;
		}
		try (InputStream in = url.openStream()) {
			String svg = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			File dir = Bio7Plugin.getDefault().getStateLocation().append("iconpreview").toFile();
			dir.mkdirs();
			File file = new File(dir, (previewCount++) + "_" + IconColors.hex(color) + ".svg");
			Files.writeString(file.toPath(), IconColors.recolor(svg, color), StandardCharsets.UTF_8);
			file.deleteOnExit();
			return ImageDescriptor.createFromURL(file.toURI().toURL()).createImage(false);
		} catch (IOException | RuntimeException e) {
			return null;
		}
	}

	private void disposeImages() {
		for (Image image : lightImages) {
			image.dispose();
		}
		for (Image image : darkImages) {
			image.dispose();
		}
		lightImages.clear();
		darkImages.clear();
	}

	@Override
	public boolean performOk() {
		boolean ok = super.performOk();
		boolean darkTheme = com.eco.bio7.util.Util.isDarkTheme();
		if (!IconColors.getPreferenceColor(darkTheme).equals(IconColors.getColor())) {
			if (MessageDialog.openQuestion(getShell(), "Icon Colors", "The icon colors are applied after a restart. Restart Bio7 now?")) {
				getShell().getDisplay().asyncExec(() -> PlatformUI.getWorkbench().restart());
			}
		}
		return ok;
	}
}
