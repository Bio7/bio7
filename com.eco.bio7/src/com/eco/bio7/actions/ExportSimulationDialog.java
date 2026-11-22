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
package com.eco.bio7.actions;

import java.io.File;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

/**
 * The options of the HTML export of a simulation model: the file, the title
 * and the description of the page, the speed, the theme, autoplay and whether
 * the page is opened in the web browser.
 */
public class ExportSimulationDialog extends TitleAreaDialog {

	/* The last choices (for the next export in this session) */
	private static double lastSpeed = 10;
	private static boolean lastAutoplay;
	private static boolean lastOpen = true;
	private static int lastTheme;

	private static final String[] THEMES = { "Automatic (light or dark as the system)", "Light", "Dark" };
	private static final String[] THEME_IDS = { "auto", "light", "dark" };
	private static final String[] LAYOUTS = { "classic", "dashboard", "focus", "story" };
	private static final String[] LAYOUT_LABELS = { "Classic (a sidebar with the controls)", "Dashboard (a top bar, the settings on the right)", "Focus (the view fills the window, floating panels)",
			"Story (one column, for reading and embedding)" };
	private static final String[] ACCENTS = { "blue", "teal", "violet", "orange", "graphite" };
	private static final String[] ACCENT_LABELS = { "Blue", "Teal", "Violet", "Orange", "Graphite" };
	private static int lastLayout, lastAccent;

	private final String modelName;
	private final boolean scene3d;
	private final boolean geo;
	private static final String[] BASEMAPS = { "topo", "osm" };
	private static final String[] BASEMAP_LABELS = { "OpenTopoMap (terrain, CC-BY-SA)", "OpenStreetMap (needs a web server, blocked for a local file)" };
	private static int lastBasemap;
	private int basemap = lastBasemap;
	private org.eclipse.swt.widgets.Combo basemapCombo;

	/* The 3D view (agent models) */
	private static final String[] BACKGROUNDS = { "auto", "studio", "night", "sky", "sunset", "ocean", "paper", "plain" };
	private static final String[] BACKGROUND_LABELS = { "Automatic (studio or night as the theme)", "Studio", "Night", "Sky", "Sunset", "Ocean", "Paper", "Plain (theme color)" };
	private static final String[] LIGHTINGS = { "studio", "soft", "dramatic", "flat" };
	private static final String[] LIGHTING_LABELS = { "Studio", "Soft", "Dramatic", "Flat (no shading)" };
	private static final String[] SHAPES = { "sphere", "cube", "cone", "glider", "point" };
	private static final String[] SHAPE_LABELS = { "Spheres", "Cubes", "Cones (direction of the movement)", "Gliders (triangles, direction of the movement)", "Points (fast)" };
	private static final String[] FLOORS = { "grid", "plane", "none" };
	private static final String[] FLOOR_LABELS = { "Grid", "Plane", "None" };
	private static int lastBackground, lastLighting, lastShape, lastFloor;
	private static boolean lastShadows = true, lastRotate;
	private int background = lastBackground, lighting = lastLighting, shape = lastShape, floor = lastFloor;
	private boolean shadows = lastShadows, rotate = lastRotate;
	private org.eclipse.swt.widgets.Combo backgroundCombo, lightingCombo, shapeCombo, floorCombo;
	private Button shadowsButton, rotateButton;
	private String file, title, description;
	private double speed = lastSpeed;
	private boolean autoplay = lastAutoplay, open = lastOpen;
	private int theme = lastTheme;
	private org.eclipse.swt.widgets.Combo themeCombo;
	private int layout = lastLayout, accent = lastAccent;
	private org.eclipse.swt.widgets.Combo layoutCombo, accentCombo;

	private Text fileText, titleText, descriptionText;
	private Spinner speedSpinner;
	private Button autoplayButton, openButton;

	/**
	 * @param file        the proposed file
	 * @param title       the proposed title
	 * @param description the proposed description
	 */
	public ExportSimulationDialog(Shell shell, String modelName, String file, String title, String description) {
		this(shell, modelName, file, title, description, false, false);
	}

	/**
	 * @param scene3d true: the options of the 3D view (an agent model)
	 * @param geo     true: the base map of a geospatial model
	 */
	/* The message of the dialog (null: the translation of a Java model) */
	private String message;

	/** The message below the title (e.g., for a model of Python or R). */
	public void setNote(String message) {
		this.message = message;
	}

	public ExportSimulationDialog(Shell shell, String modelName, String file, String title, String description, boolean scene3d, boolean geo) {
		super(shell);
		this.modelName = modelName;
		this.scene3d = scene3d;
		this.geo = geo;
		this.file = file;
		this.title = title;
		this.description = description == null ? "" : description;
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Export Simulation as HTML");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		setTitle("Export " + modelName + " as an HTML page");
		setMessage(message != null ? message : "The model is translated to JavaScript and runs in the web browser (the settings, Play/Pause, Step, Reset and the speed).");
		Composite c = new Composite(area, SWT.NONE);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(3, false);
		layout.marginWidth = 12;
		layout.marginHeight = 10;
		c.setLayout(layout);

		new Label(c, SWT.NONE).setText("File:");
		fileText = new Text(c, SWT.BORDER);
		fileText.setText(file);
		fileText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		fileText.addModifyListener(e -> validate());
		Button browse = new Button(c, SWT.PUSH);
		browse.setText("Browse...");
		browse.addListener(SWT.Selection, e -> {
			FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
			dialog.setFilterExtensions(new String[] { "*.html", "*.*" });
			File f = new File(fileText.getText());
			dialog.setFileName(f.getName());
			if (f.getParent() != null) {
				dialog.setFilterPath(f.getParent());
			}
			dialog.setOverwrite(true);
			String chosen = dialog.open();
			if (chosen != null) {
				fileText.setText(chosen);
			}
		});

		new Label(c, SWT.NONE).setText("Title:");
		titleText = new Text(c, SWT.BORDER);
		titleText.setText(title);
		titleText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		Label dl = new Label(c, SWT.NONE);
		dl.setText("Description:");
		dl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		descriptionText = new Text(c, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		descriptionText.setText(description);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		gd.heightHint = 70;
		gd.widthHint = 420;
		descriptionText.setLayoutData(gd);

		new Label(c, SWT.NONE).setText("Speed:");
		Composite sp = new Composite(c, SWT.NONE);
		GridLayout spl = new GridLayout(2, false);
		spl.marginWidth = 0;
		spl.marginHeight = 0;
		sp.setLayout(spl);
		sp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		speedSpinner = new Spinner(sp, SWT.BORDER);
		speedSpinner.setValues((int) Math.round(speed), 1, 500, 0, 1, 10);
		new Label(sp, SWT.NONE).setText("steps per second (at the start, also adjustable in the page)");

		layoutCombo = combo(c, "Layout:", LAYOUT_LABELS, this.layout);
		layoutCombo.setToolTipText("The arrangement of the page (all layouts adapt to narrow windows and phones)");
		accentCombo = combo(c, "Accent color:", ACCENT_LABELS, accent);

		new Label(c, SWT.NONE).setText("Theme:");
		themeCombo = new org.eclipse.swt.widgets.Combo(c, SWT.READ_ONLY);
		themeCombo.setItems(THEMES);
		themeCombo.select(theme);
		themeCombo.setToolTipText("The colors of the page (the page has a button to change them)");
		themeCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

		if (geo) {
			basemapCombo = combo(c, "Base map:", BASEMAP_LABELS, basemap);
			basemapCombo.setToolTipText("The map below the grid (online map tiles, the page has a \"Map\" panel to change it)");
		}
		if (scene3d) {
			Label head = new Label(c, SWT.NONE);
			head.setText("3D view (WebGL)");
			head.setFont(org.eclipse.jface.resource.JFaceResources.getBannerFont());
			GridData hd = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
			hd.verticalIndent = 8;
			head.setLayoutData(hd);
			backgroundCombo = combo(c, "Background:", BACKGROUND_LABELS, background);
			lightingCombo = combo(c, "Lighting:", LIGHTING_LABELS, lighting);
			shapeCombo = combo(c, "Shape:", SHAPE_LABELS, shape);
			floorCombo = combo(c, "Floor:", FLOOR_LABELS, floor);
			new Label(c, SWT.NONE);
			shadowsButton = new Button(c, SWT.CHECK);
			shadowsButton.setText("Soft shadows on the floor");
			shadowsButton.setSelection(shadows);
			shadowsButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			new Label(c, SWT.NONE);
			rotateButton = new Button(c, SWT.CHECK);
			rotateButton.setText("Rotate the view slowly");
			rotateButton.setSelection(rotate);
			rotateButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			Label note = new Label(c, SWT.WRAP);
			note.setText("The defaults of the page (the page has a \"View\" panel to change them).");
			note.setForeground(c.getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW));
			note.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
			Label sep = new Label(c, SWT.SEPARATOR | SWT.HORIZONTAL);
			sep.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		}

		new Label(c, SWT.NONE);
		autoplayButton = new Button(c, SWT.CHECK);
		autoplayButton.setText("Start the simulation when the page is opened (autoplay)");
		autoplayButton.setSelection(autoplay);
		autoplayButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		new Label(c, SWT.NONE);
		openButton = new Button(c, SWT.CHECK);
		openButton.setText("Open the page in the web browser");
		openButton.setSelection(open);
		openButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		return area;
	}

	private static org.eclipse.swt.widgets.Combo combo(Composite c, String label, String[] items, int selected) {
		new Label(c, SWT.NONE).setText(label);
		org.eclipse.swt.widgets.Combo combo = new org.eclipse.swt.widgets.Combo(c, SWT.READ_ONLY);
		combo.setItems(items);
		combo.select(selected);
		combo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		return combo;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Export", true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		validate();
	}

	private void validate() {
		Button ok = getButton(IDialogConstants.OK_ID);
		String f = fileText == null ? "" : home(fileText.getText().trim());
		File parent = new File(f).getAbsoluteFile().getParentFile();
		String error = f.isEmpty() ? "Choose the file of the page" : parent == null || !parent.isDirectory() ? "The folder of the file does not exist" : null;
		setErrorMessage(error);
		if (ok != null) {
			ok.setEnabled(error == null);
		}
	}

	/* ~ is the home folder (e.g., ~/Simulations/Model.html) */
	private static String home(String path) {
		if (path.equals("~") || path.startsWith("~/") || path.startsWith("~\\")) {
			return System.getProperty("user.home") + path.substring(1);
		}
		return path;
	}

	@Override
	protected void okPressed() {
		file = home(fileText.getText().trim());
		if (!file.toLowerCase().endsWith(".html") && !file.toLowerCase().endsWith(".htm")) {
			file = file + ".html";
		}
		title = titleText.getText().trim();
		description = descriptionText.getText().trim();
		speed = speedSpinner.getSelection();
		autoplay = autoplayButton.getSelection();
		open = openButton.getSelection();
		theme = Math.max(0, themeCombo.getSelectionIndex());
		layout = lastLayout = Math.max(0, layoutCombo.getSelectionIndex());
		accent = lastAccent = Math.max(0, accentCombo.getSelectionIndex());
		if (geo) {
			basemap = lastBasemap = Math.max(0, basemapCombo.getSelectionIndex());
		}
		if (scene3d) {
			background = lastBackground = Math.max(0, backgroundCombo.getSelectionIndex());
			lighting = lastLighting = Math.max(0, lightingCombo.getSelectionIndex());
			shape = lastShape = Math.max(0, shapeCombo.getSelectionIndex());
			floor = lastFloor = Math.max(0, floorCombo.getSelectionIndex());
			shadows = lastShadows = shadowsButton.getSelection();
			rotate = lastRotate = rotateButton.getSelection();
		}
		lastTheme = theme;
		lastSpeed = speed;
		lastAutoplay = autoplay;
		lastOpen = open;
		super.okPressed();
	}

	public String getFile() {
		return file;
	}

	public String getPageTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public double getSpeed() {
		return speed;
	}

	public boolean isAutoplay() {
		return autoplay;
	}

	/** Sets the options of the 3D view (agent models). */
	public void applyScene(com.eco.bio7.simulation.SimulationExport.Options o) {
		o.background = BACKGROUNDS[background];
		o.lighting = LIGHTINGS[lighting];
		o.shape = SHAPES[shape];
		o.floor = FLOORS[floor];
		o.shadows = shadows;
		o.rotate = rotate;
	}

	/** The base map of a geospatial model ("topo" or "osm"). */
	public String getBasemap() {
		return BASEMAPS[basemap];
	}

	/** The layout of the page ("classic", "dashboard", "focus" or "story"). */
	public String getPageLayout() {
		return LAYOUTS[layout];
	}

	/** The accent color ("blue", "teal", "violet", "orange" or "graphite"). */
	public String getAccentColor() {
		return ACCENTS[accent];
	}

	/** "auto", "light" or "dark". */
	public String getTheme() {
		return THEME_IDS[theme];
	}

	public boolean isOpen() {
		return open;
	}
}
