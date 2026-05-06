package com.eco.bio7.javapreferences;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.javaeditor.Bio7EditorPlugin;

/**
 * A preference page for adding Java libraries for dynamic compilation and OSGi
 * fragment loading.
 */
/*
 * DynamicCompilerJavaLibries - Preference page for managing Java libraries in
 * Bio7.
 * 
 * This preference page provides a comprehensive interface for managing external
 * Java libraries used for dynamic compilation and OSGi fragment loading within
 * Bio7.
 * 
 * Features:
 * 
 * 1. Data Storage Location - Installation folder (portable) - stores data in
 * Bio7/data/ - User home folder - stores data in ~/Bio7/ - Custom location -
 * user-specified directory
 * 
 * 2. Dynamic Compiler Libraries - Libraries added here are available on the
 * compiler classpath - Used for dynamic Java compilation within Bio7
 * 
 * 3. OSGi Fragment Libraries - Libraries loaded as OSGi fragments on Bio7
 * startup - Available to all Bio7 plugins at runtime - Can be enabled/disabled
 * via checkbox
 * 
 * 4. Library Management Options - Add Files: Select individual JAR or class
 * files - Scan Directory: Recursively scan folders for JAR files - Maven
 * Download: Download artifacts from Maven Central with: - Quick select presets
 * for popular libraries - Clipboard parsing (Maven XML, Gradle, SBT formats) -
 * Version fetching from Maven Central - Dependency resolution with correct
 * versions - Platform-specific native JAR downloads (e.g., for JavaCV, OpenCV)
 * - Download preview before execution - Transfer: Copy libraries between
 * compiler and fragment lists
 * 
 * 5. Platform Detection - Automatically detects OS and architecture - Downloads
 * appropriate native JARs (linux-x86_64, macosx-arm64, windows-x86_64, etc.)
 * 
 * 6. Maven Cache - Downloaded JARs are cached locally to avoid repeated
 * downloads - Cache location follows the data storage preference
 * 
 * Usage Notes: - Changes to OSGi fragment loading require a Bio7 restart - For
 * complex libraries like JavaCV, use single artifact download with
 * "Download dependencies" enabled rather than "Download all modules" - Native
 * JAR downloads are essential for libraries with native code
 */
public class DynamicCompilerJavaLibries extends PreferencePage implements IWorkbenchPreferencePage {

	private static final int VERTICAL_DIALOG_UNITS_PER_CHAR = 8;
	private static final int LIST_HEIGHT_IN_CHARS = 8;
	private static final int LIST_HEIGHT_IN_DLUS = LIST_HEIGHT_IN_CHARS * VERTICAL_DIALOG_UNITS_PER_CHAR;

	// Library preferences
	public static final String JAVA_LIBS_PREF = "JAVA_LIBS";
	public static final String FRAGMENT_LIBS_PREF = "FRAGMENT_LIBS";
	public static final String FRAGMENT_LOADING_ENABLED_PREF = "FRAGMENT_LOADING_ENABLED";

	// Data location preferences
	public static final String DATA_LOCATION_PREF = "DATA_LOCATION";
	public static final String DATA_LOCATION_INSTALL = "INSTALL";
	public static final String DATA_LOCATION_USER_HOME = "USER_HOME";
	public static final String DATA_LOCATION_CUSTOM = "CUSTOM";
	public static final String CUSTOM_DATA_PATH_PREF = "CUSTOM_DATA_PATH";

	private static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";
	private Path mavenCacheDir;

	private String file;
	private String[] files;
	private String currentFilePath;

	// Compiler libraries UI
	private List compilerLibsList;
	private Button removeCompilerLibBtn;

	// Fragment libraries UI
	private List fragmentLibsList;
	private Button removeFragmentLibBtn;
	private Button addFragmentLibBtn;
	private Button scanFragmentDirBtn;
	private Button mavenFragmentBtn;

	// Transfer buttons
	private Button transferToFragmentBtn;
	private Button transferToCompilerBtn;

	// Fragment loading
	private Button enableFragmentLoadingBtn;
	private Group fragmentGroup;

	// Data location UI
	private Button installLocationRadio;
	private Button userHomeLocationRadio;
	private Button customLocationRadio;
	private Text customPathText;
	private Button browseCustomPathBtn;
	private Label currentDataPathLabel;

	public DynamicCompilerJavaLibries() {
		super();
		IPreferenceStore store = Bio7EditorPlugin.getDefault().getPreferenceStore();
		setPreferenceStore(store);

		// Initialize data directory based on saved preference
		initializeDataDirectory();
	}

	/**
	 * Initialize the data directory based on preferences
	 */
	private void initializeDataDirectory() {
		IPreferenceStore store = getPreferenceStore();
		String location = store.getString(DATA_LOCATION_PREF);

		// Default to installation folder if not set
		if (location == null || location.isEmpty()) {
			location = DATA_LOCATION_INSTALL;
		}

		Path dataDir;
		switch (location) {
		case DATA_LOCATION_USER_HOME:
			dataDir = getUserHomeDataPath();
			break;
		case DATA_LOCATION_CUSTOM:
			String customPath = store.getString(CUSTOM_DATA_PATH_PREF);
			if (customPath != null && !customPath.isEmpty()) {
				dataDir = Paths.get(customPath);
			} else {
				dataDir = getInstallationDataPath();
			}
			break;
		case DATA_LOCATION_INSTALL:
		default:
			dataDir = getInstallationDataPath();
			// Fallback to user home if installation is not writable
			if (!isPathWritable(dataDir)) {
				System.out.println("[Bio7] Installation folder not writable, falling back to user home");
				dataDir = getUserHomeDataPath();
			}
			break;
		}

		mavenCacheDir = dataDir.resolve(".maven-cache");

		try {
			Files.createDirectories(mavenCacheDir);
		} catch (IOException e) {
			System.err.println("[Bio7] Failed to create maven cache directory: " + e.getMessage());
		}

		System.out.println("[Bio7] Data directory: " + dataDir);
		System.out.println("[Bio7] Maven cache: " + mavenCacheDir);
	}

	/**
	 * Get the current data directory path based on preferences
	 */
	public static Path getDataDirectory() {
		IPreferenceStore store = Bio7EditorPlugin.getDefault().getPreferenceStore();
		String location = store.getString(DATA_LOCATION_PREF);

		if (location == null || location.isEmpty()) {
			location = DATA_LOCATION_INSTALL;
		}

		Path dataDir;
		switch (location) {
		case DATA_LOCATION_USER_HOME:
			dataDir = getUserHomeDataPath();
			break;
		case DATA_LOCATION_CUSTOM:
			String customPath = store.getString(CUSTOM_DATA_PATH_PREF);
			if (customPath != null && !customPath.isEmpty()) {
				dataDir = Paths.get(customPath);
			} else {
				dataDir = getInstallationDataPath();
			}
			break;
		case DATA_LOCATION_INSTALL:
		default:
			dataDir = getInstallationDataPath();
			if (!isPathWritable(dataDir)) {
				dataDir = getUserHomeDataPath();
			}
			break;
		}

		return dataDir;
	}

	/**
	 * Get the installation folder data path
	 */
	private static Path getInstallationDataPath() {
		Path installPath = getInstallationPath();
		if (installPath != null) {
			return installPath.resolve("data");
		}
		// Fallback
		return getUserHomeDataPath();
	}

	/**
	 * Get the user home data path
	 */
	private static Path getUserHomeDataPath() {
		String userHome = System.getProperty("user.home");
		return Paths.get(userHome, "Bio7");
	}

	/**
	 * Get the Bio7 installation path
	 */
	private static Path getInstallationPath() {
		// Method 1: eclipse.home.location system property
		try {
			String eclipseHome = System.getProperty("eclipse.home.location");
			if (eclipseHome != null) {
				if (eclipseHome.startsWith("file:")) {
					eclipseHome = eclipseHome.substring(5);
					// Handle Windows paths like file:/C:/...
					if (eclipseHome.matches("^/[A-Za-z]:.*")) {
						eclipseHome = eclipseHome.substring(1);
					}
				}
				Path path = Paths.get(eclipseHome);
				if (Files.exists(path)) {
					return path;
				}
			}
		} catch (Exception e) {
			System.out.println("[Bio7] Could not get path from eclipse.home.location: " + e.getMessage());
		}

		// Method 2: osgi.install.area
		try {
			String osgiInstall = System.getProperty("osgi.install.area");
			if (osgiInstall != null) {
				if (osgiInstall.startsWith("file:")) {
					osgiInstall = osgiInstall.substring(5);
					if (osgiInstall.matches("^/[A-Za-z]:.*")) {
						osgiInstall = osgiInstall.substring(1);
					}
				}
				Path path = Paths.get(osgiInstall);
				if (Files.exists(path)) {
					return path;
				}
			}
		} catch (Exception e) {
			System.out.println("[Bio7] Could not get path from osgi.install.area: " + e.getMessage());
		}

		// Method 3: Platform.getInstallLocation()
		try {
			URL installURL = Platform.getInstallLocation().getURL();
			if (installURL != null) {
				Path path = Paths.get(installURL.toURI());
				if (Files.exists(path)) {
					return path;
				}
			}
		} catch (Exception e) {
			System.out.println("[Bio7] Could not get path from Platform: " + e.getMessage());
		}

		return null;
	}

	/**
	 * Check if a path is writable
	 */
	private static boolean isPathWritable(Path path) {
		try {
			Files.createDirectories(path);
			Path testFile = path.resolve(".write-test-" + System.currentTimeMillis());
			Files.writeString(testFile, "test");
			Files.delete(testFile);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite top = new Composite(parent, SWT.LEFT);
		top.setLayoutData(new GridData(GridData.FILL_BOTH));
		top.setLayout(new GridLayout(1, false));

		// Data Location Section (at the top)
		createDataLocationSection(top);

		// Compiler Libraries Section
		createCompilerLibrariesSection(top);

		// Transfer Buttons Section
		createTransferSection(top);

		// Fragment Libraries Section
		createFragmentLibrariesSection(top);

		// Update UI state
		updateFragmentSectionEnabled();
		updateDataLocationUI();

		return top;
	}

	private void createDataLocationSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Data Storage Location");
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setLayout(new GridLayout(3, false));

		// Installation folder option
		installLocationRadio = new Button(group, SWT.RADIO);
		installLocationRadio.setText("Installation folder (portable)");
		installLocationRadio.setToolTipText("Store data in Bio7 installation folder - makes Bio7 portable");
		GridData installData = new GridData();
		installData.horizontalSpan = 3;
		installLocationRadio.setLayoutData(installData);
		installLocationRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateDataLocationUI();
			}
		});

		// Show installation path
		Label installPathLabel = new Label(group, SWT.NONE);
		installPathLabel.setText("    ");
		Label installPathValue = new Label(group, SWT.NONE);
		Path installPath = getInstallationDataPath();
		installPathValue.setText(installPath != null ? installPath.toString() : "(not available)");
		installPathValue.setForeground(group.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		GridData installPathData = new GridData(GridData.FILL_HORIZONTAL);
		installPathData.horizontalSpan = 2;
		installPathValue.setLayoutData(installPathData);

		// Check if installation folder is writable
		if (!isPathWritable(installPath)) {
			Label warningLabel = new Label(group, SWT.NONE);
			warningLabel.setText("    ⚠ Installation folder is not writable");
			warningLabel.setForeground(group.getDisplay().getSystemColor(SWT.COLOR_RED));
			GridData warnData = new GridData();
			warnData.horizontalSpan = 3;
			warningLabel.setLayoutData(warnData);
		}

		// User home option
		userHomeLocationRadio = new Button(group, SWT.RADIO);
		userHomeLocationRadio.setText("User home folder");
		userHomeLocationRadio.setToolTipText("Store data in user's home directory - survives reinstalls");
		GridData homeData = new GridData();
		homeData.horizontalSpan = 3;
		userHomeLocationRadio.setLayoutData(homeData);
		userHomeLocationRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateDataLocationUI();
			}
		});

		// Show user home path
		Label homePathLabel = new Label(group, SWT.NONE);
		homePathLabel.setText("    ");
		Label homePathValue = new Label(group, SWT.NONE);
		homePathValue.setText(getUserHomeDataPath().toString());
		homePathValue.setForeground(group.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		GridData homePathData = new GridData(GridData.FILL_HORIZONTAL);
		homePathData.horizontalSpan = 2;
		homePathValue.setLayoutData(homePathData);

		// Custom location option
		customLocationRadio = new Button(group, SWT.RADIO);
		customLocationRadio.setText("Custom location:");
		customLocationRadio.setToolTipText("Choose a custom folder for data storage");
		customLocationRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateDataLocationUI();
			}
		});

		customPathText = new Text(group, SWT.BORDER);
		customPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		String savedCustomPath = getPreferenceStore().getString(CUSTOM_DATA_PATH_PREF);
		if (savedCustomPath != null && !savedCustomPath.isEmpty()) {
			customPathText.setText(savedCustomPath);
		}

		browseCustomPathBtn = new Button(group, SWT.PUSH);
		browseCustomPathBtn.setText("Browse...");
		browseCustomPathBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setText("Select Data Folder");
				dialog.setMessage("Choose a folder for Bio7 data storage (Maven cache, fragments, etc.)");
				String selected = dialog.open();
				if (selected != null) {
					customPathText.setText(selected);
				}
			}
		});

		// Current effective path
		Label currentLabel = new Label(group, SWT.NONE);
		currentLabel.setText("Current:");
		currentLabel.setFont(group.getDisplay().getSystemFont());

		currentDataPathLabel = new Label(group, SWT.NONE);
		currentDataPathLabel.setText(mavenCacheDir.getParent().toString());
		currentDataPathLabel.setForeground(group.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
		GridData currentData = new GridData(GridData.FILL_HORIZONTAL);
		currentData.horizontalSpan = 2;
		currentDataPathLabel.setLayoutData(currentData);

		// Info label
		Label infoLabel = new Label(group, SWT.WRAP);
		infoLabel.setText(
				"Note: Changing the data location requires a Bio7 restart. Existing cached data will not be moved automatically.");
		infoLabel.setForeground(group.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		GridData infoData = new GridData(GridData.FILL_HORIZONTAL);
		infoData.horizontalSpan = 3;
		infoData.widthHint = 400;
		infoLabel.setLayoutData(infoData);

		// Load saved preference
		String savedLocation = getPreferenceStore().getString(DATA_LOCATION_PREF);
		if (DATA_LOCATION_USER_HOME.equals(savedLocation)) {
			userHomeLocationRadio.setSelection(true);
		} else if (DATA_LOCATION_CUSTOM.equals(savedLocation)) {
			customLocationRadio.setSelection(true);
		} else {
			installLocationRadio.setSelection(true);
		}
	}

	private void updateDataLocationUI() {
		boolean customEnabled = customLocationRadio.getSelection();
		customPathText.setEnabled(customEnabled);
		browseCustomPathBtn.setEnabled(customEnabled);

		// Update current path label based on selection
		Path selectedPath;
		if (installLocationRadio.getSelection()) {
			selectedPath = getInstallationDataPath();
		} else if (userHomeLocationRadio.getSelection()) {
			selectedPath = getUserHomeDataPath();
		} else {
			String customPath = customPathText.getText().trim();
			if (!customPath.isEmpty()) {
				selectedPath = Paths.get(customPath);
			} else {
				selectedPath = getInstallationDataPath();
			}
		}

		if (currentDataPathLabel != null && !currentDataPathLabel.isDisposed()) {
			currentDataPathLabel.setText(selectedPath.toString());
		}
	}

	private void createCompilerLibrariesSection(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText("Dynamic Compiler Libraries");
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setLayout(new GridLayout(1, false));

		Label label = new Label(group, SWT.WRAP);
		label.setText("Libraries for dynamic Java compilation (available on compiler classpath):");
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		compilerLibsList = new List(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		compilerLibsList.setItems(getPreferenceArray(JAVA_LIBS_PREF));
		GridData listData = new GridData(GridData.FILL_HORIZONTAL);
		listData.heightHint = convertVerticalDLUsToPixels(LIST_HEIGHT_IN_DLUS);
		compilerLibsList.setLayoutData(listData);

		compilerLibsList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateButtonStates();
			}
		});

		Composite buttonGroup = new Composite(group, SWT.NONE);
		buttonGroup.setLayout(new GridLayout(4, true));
		buttonGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Button addBtn = new Button(buttonGroup, SWT.PUSH);
		addBtn.setText("Add Files...");
		addBtn.setToolTipText("Select individual JAR or class files");
		addBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		addBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addLibraries(compilerLibsList);
			}
		});

		Button scanDirBtn = new Button(buttonGroup, SWT.PUSH);
		scanDirBtn.setText("Scan Directory...");
		scanDirBtn.setToolTipText("Scan a directory and subdirectories for JAR files");
		scanDirBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		scanDirBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				scanDirectoryForJars(compilerLibsList);
			}
		});

		Button mavenBtn = new Button(buttonGroup, SWT.PUSH);
		mavenBtn.setText("Maven...");
		mavenBtn.setToolTipText("Download JAR from Maven Central repository");
		mavenBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mavenBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				downloadFromMaven(compilerLibsList);
			}
		});

		removeCompilerLibBtn = new Button(buttonGroup, SWT.PUSH);
		removeCompilerLibBtn.setText("Remove Selected");
		removeCompilerLibBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		removeCompilerLibBtn.setEnabled(false);
		removeCompilerLibBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				compilerLibsList.remove(compilerLibsList.getSelectionIndices());
				updateButtonStates();
			}
		});
	}

	private void createTransferSection(Composite parent) {
		Composite transferComp = new Composite(parent, SWT.NONE);
		transferComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout layout = new GridLayout(2, true);
		layout.marginHeight = 5;
		transferComp.setLayout(layout);

		transferToFragmentBtn = new Button(transferComp, SWT.PUSH);
		transferToFragmentBtn.setText("▼ Copy to Fragment Libraries ▼");
		transferToFragmentBtn.setToolTipText("Copy selected libraries to the Fragment Libraries list for OSGi loading");
		transferToFragmentBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		transferToFragmentBtn.setEnabled(false);
		transferToFragmentBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				transferSelectedLibraries(compilerLibsList, fragmentLibsList);
			}
		});

		transferToCompilerBtn = new Button(transferComp, SWT.PUSH);
		transferToCompilerBtn.setText("▲ Copy to Compiler Libraries ▲");
		transferToCompilerBtn.setToolTipText("Copy selected libraries to the Compiler Libraries list");
		transferToCompilerBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		transferToCompilerBtn.setEnabled(false);
		transferToCompilerBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				transferSelectedLibraries(fragmentLibsList, compilerLibsList);
			}
		});
	}

	private void createFragmentLibrariesSection(Composite parent) {
		fragmentGroup = new Group(parent, SWT.NONE);
		fragmentGroup.setText("OSGi Fragment Libraries");
		fragmentGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fragmentGroup.setLayout(new GridLayout(1, false));

		enableFragmentLoadingBtn = new Button(fragmentGroup, SWT.CHECK);
		enableFragmentLoadingBtn.setText("Enable OSGi Fragment Loading");
		enableFragmentLoadingBtn
				.setToolTipText("When enabled, libraries in this list will be loaded as OSGi fragments on startup");
		enableFragmentLoadingBtn.setSelection(getPreferenceStore().getBoolean(FRAGMENT_LOADING_ENABLED_PREF));
		enableFragmentLoadingBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateFragmentSectionEnabled();
			}
		});

		Label label = new Label(fragmentGroup, SWT.WRAP);
		label.setText("Libraries loaded as OSGi fragments (available to all Bio7 plugins at runtime):");
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fragmentLibsList = new List(fragmentGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		fragmentLibsList.setItems(getPreferenceArray(FRAGMENT_LIBS_PREF));
		GridData listData = new GridData(GridData.FILL_HORIZONTAL);
		listData.heightHint = convertVerticalDLUsToPixels(LIST_HEIGHT_IN_DLUS);
		fragmentLibsList.setLayoutData(listData);

		fragmentLibsList.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateButtonStates();
			}
		});

		Composite buttonGroup = new Composite(fragmentGroup, SWT.NONE);
		buttonGroup.setLayout(new GridLayout(4, true));
		buttonGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		addFragmentLibBtn = new Button(buttonGroup, SWT.PUSH);
		addFragmentLibBtn.setText("Add Files...");
		addFragmentLibBtn.setToolTipText("Select individual JAR or class files");
		addFragmentLibBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		addFragmentLibBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				addLibraries(fragmentLibsList);
			}
		});

		scanFragmentDirBtn = new Button(buttonGroup, SWT.PUSH);
		scanFragmentDirBtn.setText("Scan Directory...");
		scanFragmentDirBtn.setToolTipText("Scan a directory and subdirectories for JAR files");
		scanFragmentDirBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		scanFragmentDirBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				scanDirectoryForJars(fragmentLibsList);
			}
		});

		mavenFragmentBtn = new Button(buttonGroup, SWT.PUSH);
		mavenFragmentBtn.setText("Maven...");
		mavenFragmentBtn.setToolTipText("Download JAR from Maven Central repository");
		mavenFragmentBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mavenFragmentBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				downloadFromMaven(fragmentLibsList);
			}
		});

		removeFragmentLibBtn = new Button(buttonGroup, SWT.PUSH);
		removeFragmentLibBtn.setText("Remove Selected");
		removeFragmentLibBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		removeFragmentLibBtn.setEnabled(false);
		removeFragmentLibBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fragmentLibsList.remove(fragmentLibsList.getSelectionIndices());
				updateButtonStates();
			}
		});

		Label infoLabel = new Label(fragmentGroup, SWT.WRAP);
		infoLabel.setText("Note: Changes to fragment loading require a Bio7 restart to take effect.");
		GridData infoData = new GridData(GridData.FILL_HORIZONTAL);
		infoData.widthHint = 400;
		infoLabel.setLayoutData(infoData);
	}

	private void updateFragmentSectionEnabled() {
		boolean enabled = enableFragmentLoadingBtn.getSelection();

		fragmentLibsList.setEnabled(enabled);
		addFragmentLibBtn.setEnabled(enabled);
		scanFragmentDirBtn.setEnabled(enabled);
		mavenFragmentBtn.setEnabled(enabled);
		removeFragmentLibBtn.setEnabled(enabled && fragmentLibsList.getSelectionCount() > 0);
		transferToFragmentBtn.setEnabled(enabled && compilerLibsList.getSelectionCount() > 0);
		transferToCompilerBtn.setEnabled(enabled && fragmentLibsList.getSelectionCount() > 0);

		if (enabled) {
			fragmentLibsList.setBackground(null);
		} else {
			fragmentLibsList.setBackground(fragmentLibsList.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		}
	}

	private void updateButtonStates() {
		boolean fragmentEnabled = enableFragmentLoadingBtn.getSelection();
		int compilerSelection = compilerLibsList.getSelectionCount();
		int fragmentSelection = fragmentLibsList.getSelectionCount();

		removeCompilerLibBtn.setEnabled(compilerSelection > 0);
		removeFragmentLibBtn.setEnabled(fragmentEnabled && fragmentSelection > 0);
		transferToFragmentBtn.setEnabled(fragmentEnabled && compilerSelection > 0);
		transferToCompilerBtn.setEnabled(fragmentEnabled && fragmentSelection > 0);
	}

	private void transferSelectedLibraries(List sourceList, List targetList) {
		String[] selected = sourceList.getSelection();
		if (selected == null || selected.length == 0) {
			return;
		}

		java.util.Set<String> existingItems = new java.util.HashSet<>();
		for (String item : targetList.getItems()) {
			existingItems.add(normalizeLibPath(item));
		}

		int added = 0;
		int skipped = 0;

		for (String item : selected) {
			String normalized = normalizeLibPath(item);
			if (!existingItems.contains(normalized)) {
				targetList.add(item);
				existingItems.add(normalized);
				added++;
			} else {
				skipped++;
			}
		}

		if (skipped > 0) {
			showDuplicateMessage(skipped, added);
		}
	}

	@Override
	public void init(IWorkbench wb) {
	}

	@Override
	protected void performDefaults() {
		compilerLibsList.setItems(getDefaultPreferenceArray(JAVA_LIBS_PREF));
		fragmentLibsList.setItems(getDefaultPreferenceArray(FRAGMENT_LIBS_PREF));
		enableFragmentLoadingBtn.setSelection(getPreferenceStore().getDefaultBoolean(FRAGMENT_LOADING_ENABLED_PREF));

		// Reset data location to default (installation folder)
		installLocationRadio.setSelection(true);
		userHomeLocationRadio.setSelection(false);
		customLocationRadio.setSelection(false);
		customPathText.setText("");

		updateFragmentSectionEnabled();
		updateButtonStates();
		updateDataLocationUI();
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		// Save library preferences
		setPreferenceArray(JAVA_LIBS_PREF, compilerLibsList.getItems());
		setPreferenceArray(FRAGMENT_LIBS_PREF, fragmentLibsList.getItems());
		getPreferenceStore().setValue(FRAGMENT_LOADING_ENABLED_PREF, enableFragmentLoadingBtn.getSelection());

		// Save data location preferences
		String newLocation;
		if (userHomeLocationRadio.getSelection()) {
			newLocation = DATA_LOCATION_USER_HOME;
		} else if (customLocationRadio.getSelection()) {
			newLocation = DATA_LOCATION_CUSTOM;
			getPreferenceStore().setValue(CUSTOM_DATA_PATH_PREF, customPathText.getText().trim());
		} else {
			newLocation = DATA_LOCATION_INSTALL;
		}

		String oldLocation = getPreferenceStore().getString(DATA_LOCATION_PREF);
		getPreferenceStore().setValue(DATA_LOCATION_PREF, newLocation);

		// Check if data location changed
		if (!newLocation.equals(oldLocation)) {
			// Reinitialize data directory
			initializeDataDirectory();

			// Show restart message
			MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
			messageBox.setText("Restart Required");
			messageBox.setMessage("The data storage location has been changed.\n\n"
					+ "Please restart Bio7 for the change to take full effect.\n\n" + "New location: "
					+ mavenCacheDir.getParent());
			messageBox.open();
		}

		return super.performOk();
	}

	private void addLibraries(List targetList) {
		String[] selectedFiles = openMultipleFiles(new String[] { "*.jar", "*.class", "*" });
		if (selectedFiles == null || selectedFiles.length == 0) {
			return;
		}

		java.util.Set<String> existingItems = new java.util.HashSet<>();
		for (String item : targetList.getItems()) {
			existingItems.add(normalizeLibPath(item));
		}

		int added = 0;
		int skipped = 0;

		for (int i = 0; i < selectedFiles.length; i++) {
			String path = currentFilePath.replace("\\", "/");
			String lib = path + "/" + selectedFiles[i];
			String normalized = normalizeLibPath(lib);

			if (!existingItems.contains(normalized)) {
				targetList.add(lib);
				existingItems.add(normalized);
				added++;
			} else {
				skipped++;
			}
		}

		if (skipped > 0) {
			showDuplicateMessage(skipped, added);
		}
	}

	private void scanDirectoryForJars(List targetList) {
		String selectedDir = openDirectoryDialog("Select Directory to Scan for JARs",
				"All JAR files in this directory and subdirectories will be added.");
		if (selectedDir == null || selectedDir.isEmpty()) {
			return;
		}

		Path dirPath = Paths.get(selectedDir);
		if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
			showErrorMessage("Invalid Directory", "The selected path is not a valid directory.");
			return;
		}

		java.util.List<String> foundJars = new ArrayList<>();
		try {
			Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.toString().toLowerCase().endsWith(".jar")) {
						foundJars.add(file.toAbsolutePath().toString().replace("\\", "/"));
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			showErrorMessage("Scan Error", "Error scanning directory: " + e.getMessage());
			return;
		}

		if (foundJars.isEmpty()) {
			showInfoMessage("No JARs Found",
					"No JAR files were found in the selected directory or its subdirectories.");
			return;
		}

		java.util.Set<String> existingItems = new java.util.HashSet<>();
		for (String item : targetList.getItems()) {
			existingItems.add(normalizeLibPath(item));
		}

		int added = 0;
		int skipped = 0;

		for (String jar : foundJars) {
			String normalized = normalizeLibPath(jar);
			if (!existingItems.contains(normalized)) {
				targetList.add(jar);
				existingItems.add(normalized);
				added++;
			} else {
				skipped++;
			}
		}

		showScanResultMessage(foundJars.size(), added, skipped);
	}

	// =========================================================================
	// PLATFORM DETECTION
	// =========================================================================

	private String detectPlatformClassifier() {
		String os = System.getProperty("os.name").toLowerCase();
		String arch = System.getProperty("os.arch").toLowerCase();

		String osName;
		if (os.contains("linux")) {
			osName = "linux";
		} else if (os.contains("mac") || os.contains("darwin")) {
			osName = "macosx";
		} else if (os.contains("windows")) {
			osName = "windows";
		} else {
			osName = "linux";
		}

		String archName;
		if (arch.equals("amd64") || arch.equals("x86_64")) {
			archName = "x86_64";
		} else if (arch.equals("aarch64") || arch.equals("arm64")) {
			archName = "arm64";
		} else if (arch.equals("x86") || arch.equals("i386") || arch.equals("i686")) {
			archName = "x86";
		} else if (arch.contains("arm")) {
			archName = "armhf";
		} else {
			archName = "x86_64";
		}

		return osName + "-" + archName;
	}

	private java.util.List<String> getPlatformClassifiers() {
		java.util.List<String> classifiers = new ArrayList<>();
		String primary = detectPlatformClassifier();
		classifiers.add(primary);

		if (primary.equals("macosx-arm64")) {
			classifiers.add("macosx-x86_64");
		}

		return classifiers;
	}

	// =========================================================================
	// MAVEN DOWNLOAD FUNCTIONALITY
	// =========================================================================

	private String indent(int depth) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			sb.append("  ");
		}
		return sb.toString();
	}

	private boolean isOptionalPlatformArtifact(String artifactId) {
		return artifactId.contains("-windows") || artifactId.contains("-linux") || artifactId.contains("-macosx")
				|| artifactId.contains("-android") || artifactId.contains("-ios") || artifactId.contains("-arm")
				|| artifactId.contains("-x86") || artifactId.contains("-gpu") || artifactId.contains("-redist");
	}

	private void downloadFromMaven(List targetList) {
		MavenDownloadDialog dialog = new MavenDownloadDialog(getShell());
		if (dialog.open() == Dialog.OK) {
			String groupId = dialog.getGroupId();
			String artifactId = dialog.getArtifactId();
			String version = dialog.getVersion();
			boolean downloadDependencies = dialog.isDownloadDependencies();
			boolean downloadAllModules = dialog.isDownloadAllModules();
			boolean downloadNatives = dialog.isDownloadNatives();

			if (groupId.isEmpty()) {
				showErrorMessage("Invalid Input", "Please provide at least a Group ID.");
				return;
			}

			if (downloadAllModules) {
				if (version.isEmpty()) {
					showErrorMessage("Invalid Input", "Please provide a version when downloading all modules.");
					return;
				}
				downloadAllModulesFromGroup(targetList, groupId, version, downloadDependencies, downloadNatives);
			} else {
				if (artifactId.isEmpty() || version.isEmpty()) {
					showErrorMessage("Invalid Input", "Please provide groupId, artifactId, and version.");
					return;
				}
				downloadMavenArtifact(targetList, groupId, artifactId, version, downloadDependencies, downloadNatives);
			}
		}
	}

	private java.util.List<String[]> searchArtifactsWithVersion(String groupId, String version) {
		java.util.List<String[]> artifacts = new ArrayList<>();
		try {
			String searchUrl = "https://search.maven.org/solrsearch/select?q=g:" + groupId + "+AND+v:" + version
					+ "&core=gav&rows=200&wt=json";

			System.out.println("[Maven Search] Searching with version: " + searchUrl);

			HttpURLConnection conn = (HttpURLConnection) new URL(searchUrl).openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(10000);
			conn.setRequestProperty("User-Agent", "Bio7/1.0");

			if (conn.getResponseCode() == 200) {
				StringBuilder content = new StringBuilder();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						content.append(line);
					}
				}

				String json = content.toString();
				Pattern docPattern = Pattern.compile("\\{[^}]*\"a\":\"([^\"]+)\"[^}]*\"v\":\"([^\"]+)\"[^}]*\\}");
				Matcher matcher = docPattern.matcher(json);

				java.util.Set<String> foundArtifacts = new java.util.HashSet<>();

				while (matcher.find()) {
					String artifactIdFound = matcher.group(1);
					String artifactVersion = matcher.group(2);

					if (artifactVersion.equals(version) && !foundArtifacts.contains(artifactIdFound)) {
						foundArtifacts.add(artifactIdFound);
						artifacts.add(new String[] { groupId, artifactIdFound, version });
					}
				}
			}

		} catch (Exception e) {
			System.err.println("[Maven Search] Error: " + e.getMessage());
		}
		return artifacts;
	}

	private void downloadAllModulesFromGroup(List targetList, String groupId, String version,
			boolean downloadDependencies, boolean downloadNatives) {
		Job downloadJob = new Job("Downloading all modules from: " + groupId) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					String platformClassifier = detectPlatformClassifier();

					System.out.println("[Maven Download] ==========================================");
					System.out.println("[Maven Download] Downloading ALL modules from group:");
					System.out.println("[Maven Download]   GroupId: " + groupId);
					System.out.println("[Maven Download]   Version: " + version);
					System.out.println("[Maven Download]   Platform: " + platformClassifier);
					System.out.println("[Maven Download]   Cache: " + mavenCacheDir);
					System.out.println("[Maven Download] ==========================================");

					monitor.beginTask("Searching for modules", IProgressMonitor.UNKNOWN);

					java.util.List<String[]> allArtifacts = searchArtifactsWithVersion(groupId, version);

					if (allArtifacts.isEmpty()) {
						Display.getDefault().asyncExec(() -> {
							showErrorMessage("No Modules Found",
									"No artifacts found in group: " + groupId + " with version " + version);
						});
						return Status.OK_STATUS;
					}

					Files.createDirectories(mavenCacheDir);

					java.util.List<Path> downloadedJars = new ArrayList<>();
					java.util.List<String> failedDownloads = new ArrayList<>();
					java.util.List<String> skippedPomOnly = new ArrayList<>();
					java.util.Set<String> processedArtifacts = new java.util.HashSet<>();

					monitor.beginTask("Downloading modules", allArtifacts.size());

					for (String[] artifact : allArtifacts) {
						if (monitor.isCanceled())
							break;

						String artGroupId = artifact[0];
						String artArtifactId = artifact[1];
						String artVersion = artifact[2];
						String artKey = artGroupId + ":" + artArtifactId;

						if (processedArtifacts.contains(artKey))
							continue;
						processedArtifacts.add(artKey);

						monitor.subTask("Downloading: " + artArtifactId);

						Path jar = downloadJar(artGroupId, artArtifactId, artVersion, null, monitor);
						if (jar != null) {
							downloadedJars.add(jar);

							if (downloadNatives) {
								downloadNativeJars(artGroupId, artArtifactId, artVersion, downloadedJars, monitor);
							}

							if (downloadDependencies) {
								downloadDependenciesRecursively(artGroupId, artArtifactId, artVersion, downloadedJars,
										failedDownloads, processedArtifacts, monitor, 0, downloadNatives);
							}
						} else {
							if (isPomOnlyArtifact(artGroupId, artArtifactId, artVersion)) {
								skippedPomOnly.add(artArtifactId);
								if (downloadDependencies) {
									downloadDependenciesRecursively(artGroupId, artArtifactId, artVersion,
											downloadedJars, failedDownloads, processedArtifacts, monitor, 0,
											downloadNatives);
								}
							} else if (!isOptionalPlatformArtifact(artArtifactId)) {
								failedDownloads.add(artKey + ":" + artVersion);
							}
						}

						monitor.worked(1);
					}

					monitor.done();

					final java.util.List<Path> jarsToAdd = downloadedJars;
					final java.util.List<String> failed = failedDownloads;

					Display.getDefault().asyncExec(() -> {
						addJarsToList(targetList, jarsToAdd, failed, groupId, version);
					});

					return Status.OK_STATUS;

				} catch (Exception e) {
					e.printStackTrace();
					Display.getDefault().asyncExec(() -> {
						showErrorMessage("Download Error", "Error: " + e.getMessage());
					});
					return new Status(IStatus.ERROR, "com.eco.bio7.javaedit", "Download failed", e);
				}
			}
		};

		downloadJob.setUser(true);
		downloadJob.schedule();
	}

	private boolean isPomOnlyArtifact(String groupId, String artifactId, String version) {
		try {
			String groupPath = groupId.replace('.', '/');
			String pomUrl = MAVEN_CENTRAL_URL + "/" + groupPath + "/" + artifactId + "/" + version + "/" + artifactId
					+ "-" + version + ".pom";

			HttpURLConnection conn = (HttpURLConnection) new URL(pomUrl).openConnection();
			conn.setRequestMethod("HEAD");
			conn.setConnectTimeout(5000);
			conn.setRequestProperty("User-Agent", "Bio7/1.0");

			return conn.getResponseCode() == 200;
		} catch (Exception e) {
			return false;
		}
	}

	private Path downloadJar(String groupId, String artifactId, String version, String classifier,
			IProgressMonitor monitor) {
		try {
			String groupPath = groupId.replace('.', '/');
			String jarName;
			if (classifier != null && !classifier.isEmpty()) {
				jarName = artifactId + "-" + version + "-" + classifier + ".jar";
			} else {
				jarName = artifactId + "-" + version + ".jar";
			}
			String urlStr = MAVEN_CENTRAL_URL + "/" + groupPath + "/" + artifactId + "/" + version + "/" + jarName;

			System.out.println("[Maven Download] Downloading: " + urlStr);

			Path targetDir = mavenCacheDir.resolve(groupPath).resolve(artifactId).resolve(version);
			Files.createDirectories(targetDir);
			Path targetFile = targetDir.resolve(jarName);

			if (Files.exists(targetFile) && Files.size(targetFile) > 0) {
				System.out.println("[Maven Download] Using cached: " + targetFile);
				return targetFile;
			}

			URL url = new URL(urlStr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(15000);
			conn.setReadTimeout(60000);
			conn.setInstanceFollowRedirects(true);
			conn.setRequestProperty("User-Agent", "Bio7/1.0");

			int responseCode = conn.getResponseCode();

			if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_MOVED_PERM
					|| responseCode == HttpURLConnection.HTTP_SEE_OTHER || responseCode == 307 || responseCode == 308) {
				String newUrl = conn.getHeaderField("Location");
				conn = (HttpURLConnection) new URL(newUrl).openConnection();
				conn.setRequestProperty("User-Agent", "Bio7/1.0");
				responseCode = conn.getResponseCode();
			}

			if (responseCode != 200) {
				System.err.println("[Maven Download] Failed: " + responseCode + " for " + jarName);
				return null;
			}

			try (InputStream in = conn.getInputStream()) {
				Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
			}

			System.out.println("[Maven Download] Success: " + targetFile);
			return targetFile;

		} catch (Exception e) {
			System.err.println("[Maven Download] Error: " + e.getMessage());
			return null;
		}
	}

	private void downloadNativeJars(String groupId, String artifactId, String version,
			java.util.List<Path> downloadedJars, IProgressMonitor monitor) {
		java.util.List<String> classifiers = getPlatformClassifiers();

		for (String classifier : classifiers) {
			Path nativeJar = downloadJar(groupId, artifactId, version, classifier, monitor);
			if (nativeJar != null) {
				downloadedJars.add(nativeJar);
				break;
			}
		}
	}

	private void downloadMavenArtifact(List targetList, String groupId, String artifactId, String version,
			boolean downloadDependencies, boolean downloadNatives) {
		Job downloadJob = new Job("Downloading: " + artifactId) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					String platformClassifier = detectPlatformClassifier();

					System.out.println("[Maven Download] ==========================================");
					System.out.println("[Maven Download] Downloading: " + groupId + ":" + artifactId + ":" + version);
					System.out.println("[Maven Download]   Platform: " + platformClassifier);
					System.out.println("[Maven Download]   Cache: " + mavenCacheDir);
					System.out.println("[Maven Download] ==========================================");

					Files.createDirectories(mavenCacheDir);

					java.util.List<Path> downloadedJars = new ArrayList<>();
					java.util.List<String> failedDownloads = new ArrayList<>();
					java.util.Set<String> processedArtifacts = new java.util.HashSet<>();

					monitor.beginTask("Downloading", IProgressMonitor.UNKNOWN);
					monitor.subTask("Downloading main artifact...");

					Path mainJar = downloadJar(groupId, artifactId, version, null, monitor);

					boolean isPomOnly = (mainJar == null && isPomOnlyArtifact(groupId, artifactId, version));

					if (mainJar != null) {
						downloadedJars.add(mainJar);
						processedArtifacts.add(groupId + ":" + artifactId);

						if (downloadNatives) {
							downloadNativeJars(groupId, artifactId, version, downloadedJars, monitor);
						}
					} else if (isPomOnly) {
						processedArtifacts.add(groupId + ":" + artifactId);
						System.out.println("[Maven Download] POM-only artifact, downloading dependencies...");
					} else {
						failedDownloads.add(groupId + ":" + artifactId + ":" + version);
					}

					if (downloadDependencies && !monitor.isCanceled()) {
						downloadDependenciesRecursively(groupId, artifactId, version, downloadedJars, failedDownloads,
								processedArtifacts, monitor, 0, downloadNatives);
					}

					monitor.done();

					final java.util.List<Path> jarsToAdd = downloadedJars;
					final java.util.List<String> failed = failedDownloads;
					final boolean wasPomOnly = isPomOnly;

					Display.getDefault().asyncExec(() -> {
						addJarsToList(targetList, jarsToAdd, failed, wasPomOnly ? "(POM aggregator) " : "");
					});

					return Status.OK_STATUS;

				} catch (Exception e) {
					e.printStackTrace();
					Display.getDefault().asyncExec(() -> {
						showErrorMessage("Download Error", "Error: " + e.getMessage());
					});
					return new Status(IStatus.ERROR, "com.eco.bio7.javaedit", "Download failed", e);
				}
			}
		};

		downloadJob.setUser(true);
		downloadJob.schedule();
	}

	private void addJarsToList(List targetList, java.util.List<Path> jarsToAdd, java.util.List<String> failed,
			String prefix) {
		if (jarsToAdd.isEmpty()) {
			showErrorMessage("Download Failed", "No JAR files could be downloaded.");
			return;
		}

		java.util.Set<String> existingItems = new java.util.HashSet<>();
		for (String item : targetList.getItems()) {
			existingItems.add(normalizeLibPath(item));
		}

		int added = 0;
		int skipped = 0;

		for (Path jar : jarsToAdd) {
			String jarPath = jar.toAbsolutePath().toString().replace("\\", "/");
			String normalized = normalizeLibPath(jarPath);
			if (!existingItems.contains(normalized)) {
				targetList.add(jarPath);
				existingItems.add(normalized);
				added++;
			} else {
				skipped++;
			}
		}

		StringBuilder message = new StringBuilder();
		if (!prefix.isEmpty())
			message.append(prefix).append("\n");
		message.append("Downloaded: ").append(jarsToAdd.size()).append(" JAR(s)\n");
		message.append("Added to list: ").append(added).append("\n");
		if (skipped > 0)
			message.append("Skipped (duplicates): ").append(skipped).append("\n");
		if (!failed.isEmpty())
			message.append("\nFailed: ").append(failed.size()).append(" (some are normal)");

		showInfoMessage("Maven Download Complete", message.toString());
	}

	private void addJarsToList(List targetList, java.util.List<Path> jarsToAdd, java.util.List<String> failed,
			String groupId, String version) {
		addJarsToList(targetList, jarsToAdd, failed, "Group: " + groupId + "\nVersion: " + version + "\n");
	}

	private void downloadDependenciesRecursively(String groupId, String artifactId, String version,
			java.util.List<Path> downloadedJars, java.util.List<String> failedDownloads,
			java.util.Set<String> processedArtifacts, IProgressMonitor monitor, int depth, boolean downloadNatives) {
		if (depth > 5 || monitor.isCanceled())
			return;

		String ind = indent(depth);
		System.out.println("[Maven] " + ind + "Dependencies for: " + groupId + ":" + artifactId + ":" + version);

		java.util.List<String[]> dependencies = parsePomForDependenciesWithVersions(groupId, artifactId, version);

		for (String[] dep : dependencies) {
			if (monitor.isCanceled())
				break;

			String depGroupId = dep[0];
			String depArtifactId = dep[1];
			String depVersion = dep[2];
			String depKey = depGroupId + ":" + depArtifactId;

			if (processedArtifacts.contains(depKey))
				continue;
			processedArtifacts.add(depKey);

			monitor.subTask("Downloading: " + depArtifactId);

			Path depJar = downloadJar(depGroupId, depArtifactId, depVersion, null, monitor);
			if (depJar != null) {
				downloadedJars.add(depJar);

				if (downloadNatives) {
					downloadNativeJars(depGroupId, depArtifactId, depVersion, downloadedJars, monitor);
				}

				downloadDependenciesRecursively(depGroupId, depArtifactId, depVersion, downloadedJars, failedDownloads,
						processedArtifacts, monitor, depth + 1, downloadNatives);
			} else {
				if (isPomOnlyArtifact(depGroupId, depArtifactId, depVersion)) {
					downloadDependenciesRecursively(depGroupId, depArtifactId, depVersion, downloadedJars,
							failedDownloads, processedArtifacts, monitor, depth + 1, downloadNatives);
				} else if (!isOptionalPlatformArtifact(depArtifactId)) {
					failedDownloads.add(depKey + ":" + depVersion);
				}
			}
		}
	}

	private java.util.List<String[]> parsePomForDependenciesWithVersions(String groupId, String artifactId,
			String version) {
		java.util.List<String[]> dependencies = new ArrayList<>();

		try {
			String groupPath = groupId.replace('.', '/');
			String pomName = artifactId + "-" + version + ".pom";
			String urlStr = MAVEN_CENTRAL_URL + "/" + groupPath + "/" + artifactId + "/" + version + "/" + pomName;

			URL url = new URL(urlStr);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(30000);
			conn.setRequestProperty("User-Agent", "Bio7/1.0");

			if (conn.getResponseCode() != 200)
				return dependencies;

			StringBuilder content = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					content.append(line).append("\n");
				}
			}

			String pom = content.toString();

			java.util.Map<String, String> properties = new java.util.HashMap<>();
			properties.put("project.version", version);
			properties.put("project.groupId", groupId);

			String javacppVersion = extractJavacppVersion(version);
			properties.put("javacpp.version", javacppVersion);
			properties.put("project.parent.version", javacppVersion);

			// Extract properties
			int propsStart = pom.indexOf("<properties>");
			int propsEnd = pom.indexOf("</properties>");
			if (propsStart != -1 && propsEnd != -1) {
				String propsSection = pom.substring(propsStart, propsEnd);
				Pattern propPattern = Pattern.compile("<([a-zA-Z0-9._-]+)>([^<]+)</\\1>");
				Matcher propMatcher = propPattern.matcher(propsSection);
				while (propMatcher.find()) {
					String propName = propMatcher.group(1);
					String propValue = propMatcher.group(2).trim();
					propValue = resolveProperties(propValue, properties);
					properties.put(propName, propValue);
				}
			}

			// Find dependencies section
			int dmEnd = pom.indexOf("</dependencyManagement>");
			int depsStart = pom.indexOf("<dependencies>");
			int depsEnd = pom.indexOf("</dependencies>");

			if (dmEnd != -1 && depsStart < dmEnd) {
				depsStart = pom.indexOf("<dependencies>", dmEnd);
				if (depsStart != -1) {
					depsEnd = pom.indexOf("</dependencies>", depsStart);
				}
			}

			if (depsStart == -1 || depsEnd == -1)
				return dependencies;

			String depsSection = pom.substring(depsStart, depsEnd);

			int pos = 0;
			while (true) {
				int depStart = depsSection.indexOf("<dependency>", pos);
				if (depStart == -1)
					break;

				int depEnd = depsSection.indexOf("</dependency>", depStart);
				if (depEnd == -1)
					break;

				String dep = depsSection.substring(depStart, depEnd);

				String scope = extractXmlValue(dep, "scope");
				if (scope != null && (scope.equals("test") || scope.equals("provided") || scope.equals("system"))) {
					pos = depEnd;
					continue;
				}

				String optional = extractXmlValue(dep, "optional");
				if ("true".equals(optional)) {
					pos = depEnd;
					continue;
				}

				String depGroupId = extractXmlValue(dep, "groupId");
				String depArtifactId = extractXmlValue(dep, "artifactId");
				String depVersion = extractXmlValue(dep, "version");

				depGroupId = resolveProperties(depGroupId, properties);
				depArtifactId = resolveProperties(depArtifactId, properties);
				depVersion = resolveProperties(depVersion, properties);

				// Skip unresolved
				if (depArtifactId != null && depArtifactId.contains("${")) {
					pos = depEnd;
					continue;
				}
				if (depVersion != null && depVersion.contains("${")) {
					pos = depEnd;
					continue;
				}

				// Lookup version if missing
				if (depVersion == null && depGroupId != null && depArtifactId != null) {
					depVersion = lookupLatestVersion(depGroupId, depArtifactId);
				}

				if (depGroupId != null && depArtifactId != null && depVersion != null) {
					dependencies.add(new String[] { depGroupId, depArtifactId, depVersion });
				}

				pos = depEnd;
			}

		} catch (Exception e) {
			System.err.println("[Maven POM] Error: " + e.getMessage());
		}

		return dependencies;
	}

	private String extractJavacppVersion(String version) {
		if (version == null)
			return null;

		Pattern pattern = Pattern.compile("-(\\d+\\.\\d+\\.\\d+)$");
		Matcher matcher = pattern.matcher(version);
		if (matcher.find()) {
			return matcher.group(1);
		}

		if (version.matches("\\d+\\.\\d+\\.\\d+")) {
			return version;
		}

		return version;
	}

	private String resolveProperties(String value, java.util.Map<String, String> properties) {
		if (value == null)
			return null;

		String result = value;
		int maxIterations = 10;
		int iteration = 0;

		while (result.contains("${") && iteration < maxIterations) {
			String before = result;

			Pattern propPattern = Pattern.compile("\\$\\{([^}]+)\\}");
			Matcher matcher = propPattern.matcher(result);
			StringBuffer sb = new StringBuffer();

			while (matcher.find()) {
				String propName = matcher.group(1);
				String propValue = properties.get(propName);
				if (propValue != null) {
					matcher.appendReplacement(sb, Matcher.quoteReplacement(propValue));
				} else {
					matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
				}
			}
			matcher.appendTail(sb);
			result = sb.toString();

			if (result.equals(before))
				break;
			iteration++;
		}

		return result;
	}

	private String lookupLatestVersion(String groupId, String artifactId) {
		try {
			String searchUrl = "https://search.maven.org/solrsearch/select?q=g:" + groupId + "+AND+a:" + artifactId
					+ "&rows=1&wt=json";

			HttpURLConnection conn = (HttpURLConnection) new URL(searchUrl).openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			conn.setRequestProperty("User-Agent", "Bio7/1.0");

			if (conn.getResponseCode() == 200) {
				StringBuilder content = new StringBuilder();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						content.append(line);
					}
				}

				String json = content.toString();
				Pattern versionPattern = Pattern.compile("\"latestVersion\":\"([^\"]+)\"");
				Matcher matcher = versionPattern.matcher(json);
				if (matcher.find()) {
					return matcher.group(1);
				}
			}
		} catch (Exception e) {
			// Ignore
		}
		return null;
	}

	private String extractXmlValue(String xml, String tagName) {
		String startTag = "<" + tagName + ">";
		String endTag = "</" + tagName + ">";

		int start = xml.indexOf(startTag);
		if (start == -1)
			return null;

		int end = xml.indexOf(endTag, start);
		if (end == -1)
			return null;

		return xml.substring(start + startTag.length(), end).trim();
	}

	// =========================================================================
	// MAVEN DOWNLOAD DIALOG
	// =========================================================================

	private class MavenDownloadDialog extends Dialog {

		private Text groupIdText;
		private Text artifactIdText;
		private Combo versionCombo;
		private Button fetchVersionsBtn;
		private Button downloadDepsCheckbox;
		private Button downloadAllModulesCheckbox;
		private Button downloadNativesCheckbox;
		private Label statusLabel;
		private Combo presetsCombo;
		private List previewList;

		private String groupId = "";
		private String artifactId = "";
		private String version = "";
		private boolean downloadDependencies = true;
		private boolean downloadAllModules = false;
		private boolean downloadNatives = true;

		private final String[][] POPULAR_LIBRARIES = { { "-- Select a popular library --", "", "", "" },
				{ "Apache Commons Lang", "org.apache.commons", "commons-lang3", "3.14.0" },
				{ "Apache Commons IO", "commons-io", "commons-io", "2.16.1" },
				{ "Apache Commons Math", "org.apache.commons", "commons-math3", "3.6.1" },
				{ "Google Guava", "com.google.guava", "guava", "33.2.0-jre" },
				{ "Google Gson", "com.google.code.gson", "gson", "2.11.0" },
				{ "Jackson Databind", "com.fasterxml.jackson.core", "jackson-databind", "2.17.1" },
				{ "SLF4J API", "org.slf4j", "slf4j-api", "2.0.13" },
				{ "Logback Classic", "ch.qos.logback", "logback-classic", "1.5.6" },
				{ "JUnit 5", "org.junit.jupiter", "junit-jupiter", "5.10.2" },
				{ "Lombok", "org.projectlombok", "lombok", "1.18.32" },
				{ "JavaCV Platform", "org.bytedeco", "javacv-platform", "1.5.11" },
				{ "JavaCV (latest)", "org.bytedeco", "javacv-platform", "1.5.13" },
				{ "Pi4J Core", "com.pi4j", "pi4j-core", "2.6.0" }, { "OpenCV", "org.openpnp", "opencv", "4.9.0-0" },
				{ "JFreeChart", "org.jfree", "jfreechart", "1.5.4" },
				{ "Apache POI", "org.apache.poi", "poi", "5.2.5" },
				{ "Jsoup HTML Parser", "org.jsoup", "jsoup", "1.17.2" },
				{ "OkHttp", "com.squareup.okhttp3", "okhttp", "4.12.0" },
				{ "SQLite JDBC", "org.xerial", "sqlite-jdbc", "3.45.3.0" },
				{ "MySQL Connector", "com.mysql", "mysql-connector-j", "8.4.0" },
				{ "PostgreSQL JDBC", "org.postgresql", "postgresql", "42.7.3" },
				{ "H2 Database", "com.h2database", "h2", "2.2.224" }, };

		private final Pattern MAVEN_GROUP_ID = Pattern.compile("<groupId>\\s*([^<]+)\\s*</groupId>");
		private final Pattern MAVEN_ARTIFACT_ID = Pattern.compile("<artifactId>\\s*([^<]+)\\s*</artifactId>");
		private final Pattern MAVEN_VERSION = Pattern.compile("<version>\\s*([^<]+)\\s*</version>");
		private final Pattern GRADLE_PATTERN = Pattern.compile(
				"(?:implementation|api|compile|runtimeOnly|testImplementation)\\s*['\"]([^:]+):([^:]+):([^'\"]+)['\"]");
		private final Pattern SIMPLE_PATTERN = Pattern.compile("^([^:]+):([^:]+):([^:]+)$");

		public MavenDownloadDialog(Shell parentShell) {
			super(parentShell);
			setShellStyle(getShellStyle() | SWT.RESIZE);
		}

		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Download from Maven Central");
			newShell.setMinimumSize(520, 650);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite container = (Composite) super.createDialogArea(parent);
			container.setLayout(new GridLayout(3, false));

			// Platform and cache info
			Label infoLabel = new Label(container, SWT.WRAP);
			infoLabel.setText("Platform: " + detectPlatformClassifier() + "  |  Cache: "
					+ mavenCacheDir.getParent().getFileName());
			infoLabel.setForeground(container.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
			GridData infoData = new GridData(GridData.FILL_HORIZONTAL);
			infoData.horizontalSpan = 3;
			infoLabel.setLayoutData(infoData);

			// Quick Select
			Label presetsLabel = new Label(container, SWT.NONE);
			presetsLabel.setText("Quick Select:");

			presetsCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
			GridData presetsData = new GridData(GridData.FILL_HORIZONTAL);
			presetsData.horizontalSpan = 2;
			presetsCombo.setLayoutData(presetsData);
			for (String[] lib : POPULAR_LIBRARIES) {
				presetsCombo.add(lib[0]);
			}
			presetsCombo.select(0);
			presetsCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					int idx = presetsCombo.getSelectionIndex();
					if (idx > 0) {
						String[] lib = POPULAR_LIBRARIES[idx];
						groupIdText.setText(lib[1]);
						artifactIdText.setText(lib[2]);
						versionCombo.setText(lib[3]);
						downloadAllModulesCheckbox.setSelection(lib[2].isEmpty());
						setStatus("Selected: " + lib[0], false);
					}
				}
			});

			// Paste Button
			Button pasteBtn = new Button(container, SWT.PUSH);
			pasteBtn.setText("📋 Paste from Clipboard");
			GridData pasteData = new GridData(GridData.FILL_HORIZONTAL);
			pasteData.horizontalSpan = 3;
			pasteBtn.setLayoutData(pasteData);
			pasteBtn.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					pasteFromClipboard();
				}
			});

			// Separator
			Label sep1 = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
			GridData sep1Data = new GridData(GridData.FILL_HORIZONTAL);
			sep1Data.horizontalSpan = 3;
			sep1.setLayoutData(sep1Data);

			// GroupId
			new Label(container, SWT.NONE).setText("Group ID:");
			groupIdText = new Text(container, SWT.BORDER);
			GridData gidData = new GridData(GridData.FILL_HORIZONTAL);
			gidData.horizontalSpan = 2;
			groupIdText.setLayoutData(gidData);
			groupIdText.setMessage("e.g., org.bytedeco");

			// ArtifactId
			new Label(container, SWT.NONE).setText("Artifact ID:");
			artifactIdText = new Text(container, SWT.BORDER);
			GridData aidData = new GridData(GridData.FILL_HORIZONTAL);
			aidData.horizontalSpan = 2;
			artifactIdText.setLayoutData(aidData);
			artifactIdText.setMessage("e.g., javacv-platform");

			// Version
			new Label(container, SWT.NONE).setText("Version:");
			versionCombo = new Combo(container, SWT.DROP_DOWN);
			versionCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			fetchVersionsBtn = new Button(container, SWT.PUSH);
			fetchVersionsBtn.setText("🔍 Fetch");
			fetchVersionsBtn.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					fetchAvailableVersions();
				}
			});

			// Separator
			Label sep2 = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
			GridData sep2Data = new GridData(GridData.FILL_HORIZONTAL);
			sep2Data.horizontalSpan = 3;
			sep2.setLayoutData(sep2Data);

			// Options
			downloadAllModulesCheckbox = new Button(container, SWT.CHECK);
			downloadAllModulesCheckbox.setText("Download all modules from group");
			GridData allData = new GridData();
			allData.horizontalSpan = 3;
			downloadAllModulesCheckbox.setLayoutData(allData);

			downloadDepsCheckbox = new Button(container, SWT.CHECK);
			downloadDepsCheckbox.setText("Download dependencies (compile/runtime)");
			downloadDepsCheckbox.setSelection(true);
			GridData depsData = new GridData();
			depsData.horizontalSpan = 3;
			downloadDepsCheckbox.setLayoutData(depsData);

			downloadNativesCheckbox = new Button(container, SWT.CHECK);
			downloadNativesCheckbox.setText("Download native JARs for " + detectPlatformClassifier());
			downloadNativesCheckbox.setSelection(true);
			GridData natData = new GridData();
			natData.horizontalSpan = 3;
			downloadNativesCheckbox.setLayoutData(natData);

			// Separator
			Label sep3 = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
			GridData sep3Data = new GridData(GridData.FILL_HORIZONTAL);
			sep3Data.horizontalSpan = 3;
			sep3.setLayoutData(sep3Data);

			// Preview
			new Label(container, SWT.NONE).setText("Preview:");
			Button previewBtn = new Button(container, SWT.PUSH);
			previewBtn.setText("🔎 Preview");
			GridData pbData = new GridData(GridData.FILL_HORIZONTAL);
			pbData.horizontalSpan = 2;
			previewBtn.setLayoutData(pbData);
			previewBtn.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					previewDownload();
				}
			});

			previewList = new List(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			GridData plData = new GridData(GridData.FILL_BOTH);
			plData.horizontalSpan = 3;
			plData.heightHint = 100;
			previewList.setLayoutData(plData);

			// Status
			statusLabel = new Label(container, SWT.WRAP);
			statusLabel.setText("Enter coordinates or select a library.");
			statusLabel.setForeground(container.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
			GridData stData = new GridData(GridData.FILL_HORIZONTAL);
			stData.horizontalSpan = 3;
			statusLabel.setLayoutData(stData);

			return container;
		}

		private void fetchAvailableVersions() {
			String gId = groupIdText.getText().trim();
			String aId = artifactIdText.getText().trim();

			if (gId.isEmpty() || aId.isEmpty()) {
				setStatus("Enter Group ID and Artifact ID first.", true);
				return;
			}

			setStatus("Fetching versions...", false);
			versionCombo.removeAll();
			fetchVersionsBtn.setEnabled(false);

			Job fetchJob = new Job("Fetching versions") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					java.util.List<String> versions = new ArrayList<>();
					try {
						String searchUrl = "https://search.maven.org/solrsearch/select?q=g:" + gId + "+AND+a:" + aId
								+ "&core=gav&rows=50&wt=json";

						HttpURLConnection conn = (HttpURLConnection) new URL(searchUrl).openConnection();
						conn.setRequestMethod("GET");
						conn.setConnectTimeout(10000);
						conn.setReadTimeout(10000);
						conn.setRequestProperty("User-Agent", "Bio7/1.0");

						if (conn.getResponseCode() == 200) {
							StringBuilder content = new StringBuilder();
							try (BufferedReader reader = new BufferedReader(
									new InputStreamReader(conn.getInputStream()))) {
								String line;
								while ((line = reader.readLine()) != null) {
									content.append(line);
								}
							}

							Pattern versionPattern = Pattern.compile("\"v\":\"([^\"]+)\"");
							Matcher matcher = versionPattern.matcher(content.toString());

							while (matcher.find()) {
								String v = matcher.group(1);
								if (!versions.contains(v)) {
									versions.add(v);
								}
							}
						}
					} catch (Exception e) {
						// Ignore
					}

					Display.getDefault().asyncExec(() -> {
						fetchVersionsBtn.setEnabled(true);
						if (versions.isEmpty()) {
							setStatus("No versions found.", true);
						} else {
							versionCombo.setItems(versions.toArray(new String[0]));
							versionCombo.select(0);
							setStatus("Found " + versions.size() + " versions.", false);
						}
					});

					return Status.OK_STATUS;
				}
			};
			fetchJob.setSystem(true);
			fetchJob.schedule();
		}

		private void previewDownload() {
			String gId = groupIdText.getText().trim();
			String aId = artifactIdText.getText().trim();
			String ver = versionCombo.getText().trim();

			if (gId.isEmpty() || ver.isEmpty()) {
				setStatus("Enter Group ID and Version.", true);
				return;
			}

			if (!downloadAllModulesCheckbox.getSelection() && aId.isEmpty()) {
				setStatus("Enter Artifact ID or check 'Download all modules'.", true);
				return;
			}

			setStatus("Loading preview...", false);
			previewList.removeAll();

			Job previewJob = new Job("Preview") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					java.util.List<String> artifacts = new ArrayList<>();

					if (downloadAllModulesCheckbox.getSelection()) {
						java.util.List<String[]> found = searchArtifactsWithVersion(gId, ver);
						for (String[] art : found) {
							artifacts.add(art[0] + ":" + art[1] + ":" + art[2]);
						}
					} else {
						artifacts.add(gId + ":" + aId + ":" + ver);

						if (downloadDepsCheckbox.getSelection()) {
							java.util.List<String[]> deps = parsePomForDependenciesWithVersions(gId, aId, ver);
							for (String[] dep : deps) {
								artifacts.add("  └─ " + dep[0] + ":" + dep[1] + ":" + dep[2]);
							}
						}
					}

					Display.getDefault().asyncExec(() -> {
						if (artifacts.isEmpty()) {
							previewList.add("(No artifacts found)");
							setStatus("No artifacts found.", true);
						} else {
							for (String art : artifacts) {
								previewList.add(art);
							}
							long mainCount = artifacts.stream().filter(a -> !a.startsWith("  ")).count();
							long depCount = artifacts.size() - mainCount;
							setStatus("Preview: " + mainCount + " artifact(s)"
									+ (depCount > 0 ? " + " + depCount + " deps" : "")
									+ (downloadNativesCheckbox.getSelection() ? " + natives" : ""), false);
						}
					});

					return Status.OK_STATUS;
				}
			};
			previewJob.setSystem(true);
			previewJob.schedule();
		}

		private void setStatus(String message, boolean isError) {
			if (statusLabel == null || statusLabel.isDisposed())
				return;
			statusLabel.setForeground(
					statusLabel.getDisplay().getSystemColor(isError ? SWT.COLOR_RED : SWT.COLOR_DARK_GRAY));
			statusLabel.setText(message);
		}

		private void pasteFromClipboard() {
			Clipboard clipboard = new Clipboard(getShell().getDisplay());
			try {
				String text = (String) clipboard.getContents(TextTransfer.getInstance());
				if (text == null || text.trim().isEmpty()) {
					setStatus("Clipboard is empty.", true);
					return;
				}

				text = text.trim();
				String[] parsed = null;

				// Try Maven XML
				Matcher gm = MAVEN_GROUP_ID.matcher(text);
				Matcher am = MAVEN_ARTIFACT_ID.matcher(text);
				Matcher vm = MAVEN_VERSION.matcher(text);
				if (gm.find() && am.find() && vm.find()) {
					parsed = new String[] { gm.group(1).trim(), am.group(1).trim(), vm.group(1).trim() };
				}

				// Try Gradle
				if (parsed == null) {
					Matcher m = GRADLE_PATTERN.matcher(text);
					if (m.find()) {
						parsed = new String[] { m.group(1).trim(), m.group(2).trim(), m.group(3).trim() };
					}
				}

				// Try simple format
				if (parsed == null) {
					String cleaned = text.replaceAll("['\"]", "").trim();
					Matcher m = SIMPLE_PATTERN.matcher(cleaned);
					if (m.matches()) {
						parsed = new String[] { m.group(1).trim(), m.group(2).trim(), m.group(3).trim() };
					}
				}

				if (parsed != null) {
					groupIdText.setText(parsed[0]);
					artifactIdText.setText(parsed[1]);
					versionCombo.setText(parsed[2]);
					setStatus("Parsed: " + parsed[0] + ":" + parsed[1] + ":" + parsed[2], false);
					presetsCombo.select(0);
				} else {
					setStatus("Could not parse clipboard.", true);
				}

			} finally {
				clipboard.dispose();
			}
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, "Download", true);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		}

		@Override
		protected void okPressed() {
			groupId = groupIdText.getText().trim();
			artifactId = artifactIdText.getText().trim();
			version = versionCombo.getText().trim();
			downloadDependencies = downloadDepsCheckbox.getSelection();
			downloadAllModules = downloadAllModulesCheckbox.getSelection();
			downloadNatives = downloadNativesCheckbox.getSelection();

			if (groupId.isEmpty()) {
				setStatus("Enter a Group ID.", true);
				return;
			}
			if (!downloadAllModules && artifactId.isEmpty()) {
				setStatus("Enter an Artifact ID or check 'Download all modules'.", true);
				return;
			}
			if (version.isEmpty()) {
				setStatus("Enter or select a version.", true);
				return;
			}

			super.okPressed();
		}

		public String getGroupId() {
			return groupId;
		}

		public String getArtifactId() {
			return artifactId;
		}

		public String getVersion() {
			return version;
		}

		public boolean isDownloadDependencies() {
			return downloadDependencies;
		}

		public boolean isDownloadAllModules() {
			return downloadAllModules;
		}

		public boolean isDownloadNatives() {
			return downloadNatives;
		}
	}

	// =========================================================================
	// UTILITY METHODS
	// =========================================================================

	private String openDirectoryDialog(String title, String message) {
		final String[] result = new String[1];
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(() -> {
			Shell shell = display.getActiveShell();
			if (shell == null)
				shell = new Shell(display);
			DirectoryDialog dlg = new DirectoryDialog(shell, SWT.OPEN);
			dlg.setText(title);
			dlg.setMessage(message);
			result[0] = dlg.open();
		});
		return result[0];
	}

	private String normalizeLibPath(String path) {
		if (path == null)
			return "";
		String normalized = path.replace("\\", "/").trim();
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			normalized = normalized.toLowerCase();
		}
		return normalized;
	}

	private void showDuplicateMessage(int skipped, int added) {
		String message = added == 0
				? (skipped == 1 ? "Library already exists." : "All " + skipped + " libraries already exist.")
				: added + " added, " + skipped + " duplicate(s) skipped.";

		MessageBox box = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
		box.setText("Duplicate Libraries");
		box.setMessage(message);
		box.open();
	}

	private void showScanResultMessage(int found, int added, int skipped) {
		StringBuilder msg = new StringBuilder();
		msg.append("Found ").append(found).append(" JAR(s).\n");
		msg.append("Added: ").append(added).append("\n");
		if (skipped > 0)
			msg.append("Skipped (duplicates): ").append(skipped);

		MessageBox box = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
		box.setText("Directory Scan Complete");
		box.setMessage(msg.toString());
		box.open();
	}

	private void showErrorMessage(String title, String message) {
		MessageBox box = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
		box.setText(title);
		box.setMessage(message);
		box.open();
	}

	private void showInfoMessage(String title, String message) {
		MessageBox box = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
		box.setText(title);
		box.setMessage(message);
		box.open();
	}

	private String[] getDefaultPreferenceArray(String prefKey) {
		return convert(Bio7EditorPlugin.getDefault().getPreferenceStore().getDefaultString(prefKey));
	}

	private String[] getPreferenceArray(String prefKey) {
		return convert(Bio7EditorPlugin.getDefault().getPreferenceStore().getString(prefKey));
	}

	private String[] convert(String preferenceValue) {
		if (preferenceValue == null || preferenceValue.isEmpty()) {
			return new String[0];
		}
		StringTokenizer tokenizer = new StringTokenizer(preferenceValue, ";");
		int tokenCount = tokenizer.countTokens();
		String[] elements = new String[tokenCount];
		for (int i = 0; i < tokenCount; i++) {
			elements[i] = tokenizer.nextToken();
		}
		return elements;
	}

	private void setPreferenceArray(String prefKey, String[] elements) {
		StringBuilder buffer = new StringBuilder();
		for (String element : elements) {
			buffer.append(element).append(";");
		}
		Bio7EditorPlugin.getDefault().getPreferenceStore().setValue(prefKey, buffer.toString());
	}

	public String openFile(final String[] extension) {
		file = null;
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(() -> {
			Shell s = new Shell(SWT.ON_TOP);
			FileDialog fd = new FileDialog(s, SWT.OPEN);
			fd.setText("Select Libraries");
			fd.setFilterExtensions(extension);
			file = fd.open();
		});
		return file;
	}

	public String[] openMultipleFiles(final String[] extension) {
		files = null;
		currentFilePath = null;
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(() -> {
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
		});
		return files;
	}
}