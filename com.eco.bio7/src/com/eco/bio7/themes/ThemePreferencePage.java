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

package com.eco.bio7.themes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

import com.eco.bio7.util.IconColors;

import com.eco.bio7.Bio7Plugin;

/**
 * The preferences of the custom themes: the list of the themes (the Bio7 light
 * and dark default themes can not be changed), the colors, the toolbar artwork,
 * a preview and the buttons to create, duplicate, delete, import, export and
 * activate themes.
 */
public class ThemePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private static final String LIGHT_LABEL = "Bio7 Light (default)";
	private static final String DARK_LABEL = "Bio7 Dark (default)";
	private static final String[] PREVIEW_ICONS = { "icons/maintoolbar/play_pause.svg", "icons/maintoolbar/setup.svg", "icons/maintoolbar/r.svg",
			"icons/maintoolbar/create_project_file.svg", "icons/views/console.svg" };

	private Table table;
	private final List<CustomTheme> themes = new ArrayList<CustomTheme>();
	private CustomTheme current;
	private boolean dirty;
	private boolean updating;

	private Composite editor;
	private Text name;
	private Text author;
	private Combo base;
	private Combo style;
	private final Map<String, ColorSelector> selectors = new HashMap<String, ColorSelector>();
	private Label artworkLabel;
	private Combo mode;
	private Spinner tileSize;
	private Scale opacity;
	private Combo fade;
	private Scale fadeWidth;
	private Canvas preview;
	private Button duplicate, delete, export, activate;

	private Image previewImage;
	private final List<Image> iconImages = new ArrayList<Image>();
	private String iconKey = "";

	public ThemePreferencePage() {
		setDescription("Custom themes: based on the Bio7 light or dark default theme (the default themes are not changed). A theme can be exported and shared.");
		noDefaultAndApplyButton();
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite area = new Composite(parent, SWT.NONE);
		area.setLayout(new GridLayout(2, false));
		area.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		table = new Table(area, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		GridData td = new GridData(SWT.FILL, SWT.FILL, true, false);
		td.heightHint = 80;
		table.setLayoutData(td);
		table.addListener(SWT.Selection, e -> select());

		Composite buttons = new Composite(area, SWT.NONE);
		GridLayout bl = new GridLayout(1, true);
		bl.marginWidth = 0;
		buttons.setLayout(bl);
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		button(buttons, "New", e -> newTheme());
		duplicate = button(buttons, "Duplicate", e -> duplicateTheme());
		delete = button(buttons, "Delete", e -> deleteTheme());
		button(buttons, "Import...", e -> importTheme());
		export = button(buttons, "Export...", e -> exportTheme());
		activate = button(buttons, "Activate", e -> activateSelected());

		/* A canvas, so that the preview is not changed by the active theme (CSS)! */
		preview = new Canvas(area, SWT.DOUBLE_BUFFERED);
		GridData pd = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		pd.heightHint = 84;
		preview.setLayoutData(pd);
		preview.addPaintListener(e -> paintPreview(e.gc));
		preview.addListener(SWT.Resize, e -> {
			disposePreview();
			preview.redraw();
		});

		editor = new Composite(area, SWT.NONE);
		editor.setLayout(new GridLayout(4, false));
		editor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));

		new Label(editor, SWT.NONE).setText("Name:");
		name = new Text(editor, SWT.BORDER);
		name.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		name.addListener(SWT.Modify, e -> changed());
		new Label(editor, SWT.NONE).setText("Author:");
		author = new Text(editor, SWT.BORDER);
		author.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		author.addListener(SWT.Modify, e -> changed());
		new Label(editor, SWT.NONE).setText("Based on:");
		base = new Combo(editor, SWT.READ_ONLY);
		base.setItems("Bio7 Light", "Bio7 Dark");
		base.addListener(SWT.Selection, e -> changeBase());
		new Label(editor, SWT.NONE).setText("Toolbar:");
		style = new Combo(editor, SWT.READ_ONLY);
		style.setItems(CustomTheme.STYLE_LABELS);
		style.addListener(SWT.Selection, e -> changed());

		Group colors = new Group(editor, SWT.NONE);
		colors.setText("Colors");
		colors.setLayout(new GridLayout(8, false));
		colors.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 4, 1));
		for (String[] slot : CustomTheme.SLOTS) {
			new Label(colors, SWT.NONE).setText(slot[1] + ":");
			ColorSelector selector = new ColorSelector(colors);
			selector.addListener(e -> changed());
			selectors.put(slot[0], selector);
		}
		Button reset = new Button(colors, SWT.PUSH);
		reset.setText("Reset Colors");
		reset.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
		reset.addListener(SWT.Selection, e -> resetColors());

		Group art = new Group(editor, SWT.NONE);
		art.setText("Toolbar Artwork (SVG, PNG, JPG)");
		art.setLayout(new GridLayout(6, false));
		art.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 4, 1));
		new Label(art, SWT.NONE).setText("Image:");
		artworkLabel = new Label(art, SWT.NONE);
		artworkLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		button(art, "Choose...", e -> chooseArtwork());
		button(art, "Remove", e -> {
			current.setArtwork("");
			changed();
		});
		new Label(art, SWT.NONE).setText("Mode:");
		mode = new Combo(art, SWT.READ_ONLY);
		mode.setItems("Tile (pattern)", "Stretch (image)");
		mode.addListener(SWT.Selection, e -> changed());
		new Label(art, SWT.NONE).setText("Tile size (%):");
		tileSize = new Spinner(art, SWT.BORDER);
		tileSize.setValues(100, 10, 400, 0, 10, 50);
		tileSize.addListener(SWT.Modify, e -> changed());
		new Label(art, SWT.NONE).setText("Opacity:");
		opacity = new Scale(art, SWT.HORIZONTAL);
		scale(opacity, 0, 100);
		opacity.addListener(SWT.Selection, e -> changed());
		new Label(art, SWT.NONE).setText("Fade:");
		fade = new Combo(art, SWT.READ_ONLY);
		fade.setItems(CustomTheme.FADE_LABELS);
		fade.addListener(SWT.Selection, e -> changed());
		fade.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
		new Label(art, SWT.NONE).setText("Fade width:");
		fadeWidth = new Scale(art, SWT.HORIZONTAL);
		scale(fadeWidth, 5, 100);
		fadeWidth.addListener(SWT.Selection, e -> changed());

		area.addDisposeListener(e -> {
			disposePreview();
			disposeIcons();
		});

		fillTable(ThemeStore.activeFolder());
		return area;
	}

	private static void scale(Scale scale, int min, int max) {
		scale.setMinimum(min);
		scale.setMaximum(max);
		scale.setIncrement(1);
		scale.setPageIncrement(10);
		scale.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	private Button button(Composite parent, String text, org.eclipse.swt.widgets.Listener listener) {
		Button b = new Button(parent, SWT.PUSH);
		b.setText(text);
		b.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		b.addListener(SWT.Selection, listener);
		return b;
	}

	/* ######################## List ######################## */

	private void fillTable(String selectFolder) {
		themes.clear();
		themes.addAll(ThemeStore.themes());
		table.removeAll();
		String active = ThemeStore.activeFolder();
		boolean darkActive = com.eco.bio7.util.Util.isDarkTheme();
		new TableItem(table, SWT.NONE).setText(LIGHT_LABEL + (active.isEmpty() && !darkActive ? "  \u2713 active" : ""));
		new TableItem(table, SWT.NONE).setText(DARK_LABEL + (active.isEmpty() && darkActive ? "  \u2713 active" : ""));
		int select = darkActive ? 1 : 0;
		for (int i = 0; i < themes.size(); i++) {
			CustomTheme t = themes.get(i);
			new TableItem(table, SWT.NONE).setText(t.getName() + (t.isDark() ? "  (dark)" : "  (light)")
					+ (t.getFolderName().equals(active) ? "  \u2713 active" : ""));
			if (t.getFolderName().equals(selectFolder)) {
				select = i + 2;
			}
		}
		table.setSelection(select);
		select();
	}

	private void select() {
		saveCurrent();
		int index = table.getSelectionIndex();
		current = index >= 2 ? themes.get(index - 2) : null;
		boolean custom = current != null;
		setEnabled(editor, custom);
		duplicate.setEnabled(index >= 0);
		delete.setEnabled(custom);
		export.setEnabled(custom);
		activate.setEnabled(index >= 0);
		CustomTheme shown = custom ? current : new CustomTheme("default", index == 1);
		updating = true;
		name.setText(custom ? current.getName() : index == 1 ? DARK_LABEL : LIGHT_LABEL);
		author.setText(shown.getAuthor());
		base.select(shown.isDark() ? 1 : 0);
		style.select(Math.max(0, List.of(CustomTheme.STYLES).indexOf(shown.getStyle())));
		for (String[] slot : CustomTheme.SLOTS) {
			selectors.get(slot[0]).setColorValue(shown.getColor(slot[0]));
		}
		artworkLabel.setText(shown.getArtwork().isEmpty() ? "(none)" : shown.getArtwork());
		mode.select("stretch".equals(shown.getMode()) ? 1 : 0);
		tileSize.setSelection(shown.getTileSize());
		opacity.setSelection(shown.getOpacity());
		fade.select(Math.max(0, List.of(CustomTheme.FADES).indexOf(shown.getFade())));
		fadeWidth.setSelection(shown.getFadeWidth());
		updating = false;
		dirty = false;
		disposePreview();
		preview.redraw();
	}

	private static void setEnabled(Control control, boolean enabled) {
		control.setEnabled(enabled);
		if (control instanceof Composite) {
			for (Control child : ((Composite) control).getChildren()) {
				setEnabled(child, enabled);
			}
		}
	}

	/* ######################## Editing ######################## */

	private void changed() {
		if (updating || current == null) {
			return;
		}
		current.setName(name.getText().trim().isEmpty() ? "Unnamed" : name.getText().trim());
		current.setAuthor(author.getText().trim());
		for (String[] slot : CustomTheme.SLOTS) {
			current.setColor(slot[0], selectors.get(slot[0]).getColorValue());
		}
		current.setMode(mode.getSelectionIndex() == 1 ? "stretch" : "tile");
		current.setStyle(CustomTheme.STYLES[Math.max(0, style.getSelectionIndex())]);
		current.setTileSize(tileSize.getSelection());
		current.setOpacity(opacity.getSelection());
		current.setFade(CustomTheme.FADES[Math.max(0, fade.getSelectionIndex())]);
		current.setFadeWidth(fadeWidth.getSelection());
		artworkLabel.setText(current.getArtwork().isEmpty() ? "(none)" : current.getArtwork());
		dirty = true;
		disposePreview();
		preview.redraw();
	}

	private void changeBase() {
		if (updating || current == null) {
			return;
		}
		boolean dark = base.getSelectionIndex() == 1;
		if (dark != current.isDark()) {
			/* A new id (the base is a part of the id), so the theme is saved in a new folder! */
			current.setDark(dark);
			if (MessageDialog.openQuestion(getShell(), "Theme", "Use the default colors of the Bio7 " + (dark ? "dark" : "light") + " theme?")) {
				resetColors();
			}
			changed();
		}
	}

	private void resetColors() {
		if (current == null) {
			return;
		}
		current.setDefaults();
		updating = true;
		for (String[] slot : CustomTheme.SLOTS) {
			selectors.get(slot[0]).setColorValue(current.getColor(slot[0]));
		}
		updating = false;
		changed();
	}

	private void chooseArtwork() {
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		dialog.setFilterExtensions(new String[] { "*.svg;*.png;*.jpg;*.jpeg;*.gif", "*.*" });
		String file = dialog.open();
		if (file == null || current == null) {
			return;
		}
		try {
			ThemeStore.save(current);
			current.setArtwork(ThemeStore.copyArtwork(current, new File(file)));
			changed();
		} catch (IOException e) {
			MessageDialog.openError(getShell(), "Theme", e.getMessage());
		}
	}

	private void saveCurrent() {
		if (current != null && dirty) {
			try {
				ThemeStore.save(current);
			} catch (IOException e) {
				MessageDialog.openError(getShell(), "Theme", "The theme could not be saved: " + e.getMessage());
			}
			dirty = false;
		}
	}

	/* ######################## Buttons ######################## */

	private void newTheme() {
		boolean dark = current != null ? current.isDark() : table.getSelectionIndex() == 1;
		CustomTheme theme = new CustomTheme(ThemeStore.newFolderName("New Theme"), dark);
		theme.setName("New Theme");
		store(theme);
	}

	private void duplicateTheme() {
		CustomTheme source = current != null ? current : new CustomTheme("default", table.getSelectionIndex() == 1);
		String newName = (current != null ? current.getName() : table.getSelectionIndex() == 1 ? "Bio7 Dark" : "Bio7 Light") + " Copy";
		CustomTheme theme = source.copy(ThemeStore.newFolderName(newName));
		theme.setName(newName);
		if (current != null && !current.getArtwork().isEmpty()) {
			try {
				ThemeStore.save(theme);
				File art = new File(ThemeStore.dir(current), current.getArtwork());
				Files.copy(art.toPath(), new File(ThemeStore.dir(theme), current.getArtwork()).toPath(),
						java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				theme.setArtwork("");
			}
		}
		store(theme);
	}

	private void store(CustomTheme theme) {
		saveCurrent();
		try {
			ThemeStore.save(theme);
			ThemeStore.register(theme);
		} catch (IOException e) {
			MessageDialog.openError(getShell(), "Theme", e.getMessage());
		}
		current = null;
		fillTable(theme.getFolderName());
	}

	private void deleteTheme() {
		if (current == null || !MessageDialog.openConfirm(getShell(), "Theme", "Delete the theme \"" + current.getName() + "\"?")) {
			return;
		}
		ThemeStore.delete(current);
		current = null;
		dirty = false;
		fillTable("");
	}

	private void importTheme() {
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
		dialog.setFilterExtensions(new String[] { "*." + ThemeStore.EXTENSION, "*.zip", "*.*" });
		String file = dialog.open();
		if (file == null) {
			return;
		}
		try {
			saveCurrent();
			CustomTheme theme = ThemeStore.importTheme(new File(file));
			current = null;
			fillTable(theme.getFolderName());
		} catch (IOException | RuntimeException e) {
			MessageDialog.openError(getShell(), "Theme", "The theme could not be imported: " + e.getMessage());
		}
	}

	private void exportTheme() {
		if (current == null) {
			return;
		}
		FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
		dialog.setFilterExtensions(new String[] { "*." + ThemeStore.EXTENSION });
		dialog.setFileName(current.getFolderName() + "." + ThemeStore.EXTENSION);
		dialog.setOverwrite(true);
		String file = dialog.open();
		if (file == null) {
			return;
		}
		try {
			ThemeStore.export(current, new File(file));
		} catch (IOException e) {
			MessageDialog.openError(getShell(), "Theme", "The theme could not be exported: " + e.getMessage());
		}
	}

	private void activateSelected() {
		saveCurrent();
		int index = table.getSelectionIndex();
		boolean dark;
		if (index < 2) {
			dark = index == 1;
			ThemeStore.activateDefault(dark);
		} else {
			dark = current.isDark();
			ThemeStore.activate(current);
		}
		String folder = current == null ? "" : current.getFolderName();
		current = null;
		fillTable(folder);
		if (ThemeStore.iconColorsChanged(dark)) {
			if (MessageDialog.openQuestion(getShell(), "Theme", "The icon colors of the theme are applied after a restart. Restart Bio7 now?")) {
				getShell().getDisplay().asyncExec(() -> PlatformUI.getWorkbench().restart());
			}
		}
	}

	@Override
	public boolean performOk() {
		saveCurrent();
		/* The changes of the active theme are applied! */
		CustomTheme active = ToolbarArtwork.getTheme();
		if (active != null) {
			CustomTheme saved = ThemeStore.find(active.getFolderName());
			if (saved != null) {
				ThemeStore.activate(saved);
			}
		}
		return true;
	}

	/* ######################## Preview ######################## */

	private void paintPreview(GC gc) {
		Point size = preview.getSize();
		CustomTheme t = current != null ? current : new CustomTheme("default", table.getSelectionIndex() == 1);
		File dir = current != null ? ThemeStore.dir(current) : null;
		int bar = 30;
		if (previewImage == null) {
			int w = size.x;
			previewImage = new Image(preview.getDisplay(), (ImageDataProvider) zoom -> ArtworkRenderer.render(t, dir, w, bar, zoom));
		}
		gc.drawImage(previewImage, 0, 0);
		/* The icons of the toolbar! */
		List<Image> icons = icons(t.getColor("iconColor"), t.getColor("iconDetail"));
		for (int i = 0; i < icons.size(); i++) {
			gc.drawImage(icons.get(i), 10 + i * 26, (bar - 16) / 2);
		}
		gc.setForeground(color(t.getColor("toolbarText")));
		gc.drawText("Toolbar", size.x - 70, (bar - gc.getFontMetrics().getHeight()) / 2, true);
		/* The tabs! */
		int tabY = bar, tabH = 24;
		gc.setBackground(color(t.getColor("background")));
		gc.fillRectangle(0, tabY, size.x, size.y - tabY);
		gc.setBackground(color(t.getColor("tabRow")));
		gc.fillRectangle(4, tabY + 4, size.x - 8, tabH);
		gc.setBackground(color(t.getColor("tabSelected")));
		gc.fillRectangle(4, tabY + 4, 90, tabH);
		gc.setForeground(color(t.getColor("tabBorder")));
		gc.drawRectangle(4, tabY + 4, size.x - 9, size.y - tabY - 9);
		gc.setForeground(color(t.getColor("tabSelectedText")));
		gc.drawText("R-Shell", 14, tabY + 8, true);
		gc.setForeground(color(t.getColor("tabText")));
		gc.drawText("Packages", 106, tabY + 8, true);
		/* The view! */
		gc.setBackground(color(t.getColor("viewBackground")));
		gc.fillRectangle(5, tabY + 4 + tabH, size.x - 10, size.y - tabY - tabH - 10);
		gc.setForeground(color(t.getColor("text")));
		gc.drawText("x <- rnorm(100)   # The text of a view", 14, tabY + tabH + 12, true);
		/* The panel of a view (e.g., with buttons and labels)! */
		int panelY = tabY + tabH + 12 + gc.getFontMetrics().getHeight() + 8;
		if (panelY < size.y - 10) {
			gc.setBackground(color(t.getColor("panelBackground")));
			gc.fillRectangle(5, panelY, size.x - 10, size.y - panelY - 5);
			gc.drawText("View panel", 14, panelY + 4, true);
		}
	}

	private Color color(RGB rgb) {
		return new Color(preview.getDisplay(), rgb);
	}

	/* The preview icons with the icon colors of the theme! */
	private List<Image> icons(RGB color, RGB detail) {
		String key = CustomTheme.hex(color) + CustomTheme.hex(detail);
		if (key.equals(iconKey)) {
			return iconImages;
		}
		disposeIcons();
		iconKey = key;
		Bundle bundle = Platform.getBundle("com.eco.bio7");
		if (bundle == null) {
			return iconImages;
		}
		File tmp = Bio7Plugin.getDefault().getStateLocation().append("themepreview").toFile();
		tmp.mkdirs();
		for (int i = 0; i < PREVIEW_ICONS.length; i++) {
			URL url = FileLocator.find(bundle, IPath.fromPortableString(PREVIEW_ICONS[i]));
			if (url == null) {
				continue;
			}
			try (InputStream in = url.openStream()) {
				String svg = IconColors.recolor(new String(in.readAllBytes(), StandardCharsets.UTF_8), color, detail);
				File file = new File(tmp, i + "_" + key + ".svg");
				Files.writeString(file.toPath(), svg);
				file.deleteOnExit();
				Image image = ImageDescriptor.createFromURL(file.toURI().toURL()).createImage(false);
				if (image != null) {
					iconImages.add(image);
				}
			} catch (IOException | RuntimeException e) {
				/* Next icon! */
			}
		}
		return iconImages;
	}

	private void disposePreview() {
		if (previewImage != null) {
			previewImage.dispose();
			previewImage = null;
		}
	}

	private void disposeIcons() {
		for (Image image : iconImages) {
			image.dispose();
		}
		iconImages.clear();
		iconKey = "";
	}
}
