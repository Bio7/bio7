package com.eco.bio7.javapreferences;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.function.Consumer;
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
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
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

    /**
     * Repository base URLs to try, in order, for the download currently in
     * progress -- set from {@link MavenDownloadDialog#getSelectedRepositories()}
     * right before each download Job is scheduled. Defaults to Maven Central
     * only. Some artifacts (e.g. {@code net.imglib2:imglib2-algorithm}) were
     * never published to Maven Central and only exist on the SciJava/Fiji Nexus
     * repository, which the user can opt into via a checkbox in the dialog.
     */
    private String[] activeRepositories = { MAVEN_CENTRAL_URL };

    private Path mavenCacheDir;

    // Cache for POM properties to avoid repeated fetches
    private final java.util.Map<String, java.util.Map<String, String>> pomPropertiesCache = new java.util.HashMap<>();

    // Font for path display
    private Font pathFont;

    private String file;
    private String[] files;
    private String currentFilePath;

    // Compiler libraries UI
    private List compilerLibsList;
    private Button removeCompilerLibBtn;
    private Button openCompilerLibLocationBtn;

    // Fragment libraries UI
    private List fragmentLibsList;
    private Button removeFragmentLibBtn;
    private Button addFragmentLibBtn;
    private Button scanFragmentDirBtn;
    private Button mavenFragmentBtn;
    private Button openFragmentLibLocationBtn;

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
        initializeDefaultPreferences();
        initializeDataDirectory();
    }

    private void initializeDefaultPreferences() {
        IPreferenceStore store = getPreferenceStore();
        store.setDefault(DATA_LOCATION_PREF, DATA_LOCATION_INSTALL);
        store.setDefault(CUSTOM_DATA_PATH_PREF, "");
        store.setDefault(FRAGMENT_LOADING_ENABLED_PREF, false);
        store.setDefault(JAVA_LIBS_PREF, "");
        store.setDefault(FRAGMENT_LIBS_PREF, "");
    }

    private void initializeDataDirectory() {
        IPreferenceStore store = getPreferenceStore();
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

    private static Path getInstallationDataPath() {
        Path installPath = getInstallationPath();
        if (installPath != null) {
            return installPath.resolve("data");
        }
        return getUserHomeDataPath();
    }

    private static Path getUserHomeDataPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, "Bio7");
    }

    private static Path getInstallationPath() {
        try {
            String eclipseHome = System.getProperty("eclipse.home.location");
            if (eclipseHome != null) {
                if (eclipseHome.startsWith("file:")) {
                    eclipseHome = eclipseHome.substring(5);
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

    private Font getPathFont(Display display) {
        if (pathFont == null || pathFont.isDisposed()) {
            Font textFont = JFaceResources.getTextFont();
            if (textFont != null) {
                FontData[] fontData = textFont.getFontData();
                if (fontData.length > 0) {
                    fontData[0].setHeight(fontData[0].getHeight() - 1);
                    pathFont = new Font(display, fontData);
                }
            }
            if (pathFont == null) {
                pathFont = new Font(display, "Monospace", 9, SWT.NORMAL);
            }
        }
        return pathFont;
    }

    @Override
    public void dispose() {
        if (pathFont != null && !pathFont.isDisposed()) {
            pathFont.dispose();
        }
        super.dispose();
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite top = new Composite(parent, SWT.LEFT);
        top.setLayoutData(new GridData(GridData.FILL_BOTH));
        top.setLayout(new GridLayout(1, false));

        createDataLocationSection(top);
        createCompilerLibrariesSection(top);
        createTransferSection(top);
        createFragmentLibrariesSection(top);

        updateFragmentSectionEnabled();
        updateDataLocationUI();

        return top;
    }

    private Text createPathDisplay(Composite parent, String path, int horizontalSpan) {
        Text pathText = new Text(parent, SWT.BORDER | SWT.READ_ONLY | SWT.SINGLE);
        pathText.setText(path != null ? path : "(not available)");
        pathText.setFont(getPathFont(parent.getDisplay()));
        pathText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
        pathText.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = horizontalSpan;
        pathText.setLayoutData(gd);

        return pathText;
    }

    private void createDataLocationSection(Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        group.setText("Data Storage Location");
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setLayout(new GridLayout(3, false));

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

        Label installPathLabel = new Label(group, SWT.NONE);
        installPathLabel.setText("    Path:");
        Path installPath = getInstallationDataPath();
        createPathDisplay(group, installPath != null ? installPath.toString() : null, 2);

        if (!isPathWritable(installPath)) {
            new Label(group, SWT.NONE);
            Label warningLabel = new Label(group, SWT.NONE);
            warningLabel.setText("⚠ Installation folder is not writable");
            warningLabel.setForeground(group.getDisplay().getSystemColor(SWT.COLOR_RED));
            GridData warnData = new GridData(GridData.FILL_HORIZONTAL);
            warnData.horizontalSpan = 2;
            warningLabel.setLayoutData(warnData);
        }

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

        Label homePathLabel = new Label(group, SWT.NONE);
        homePathLabel.setText("    Path:");
        createPathDisplay(group, getUserHomeDataPath().toString(), 2);

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
        customPathText.setFont(getPathFont(group.getDisplay()));
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

        Label separator = new Label(group, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData sepData = new GridData(GridData.FILL_HORIZONTAL);
        sepData.horizontalSpan = 3;
        sepData.verticalIndent = 5;
        separator.setLayoutData(sepData);

        Label currentLabel = new Label(group, SWT.NONE);
        currentLabel.setText("Active:");
        currentLabel.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));

        currentDataPathLabel = new Label(group, SWT.NONE);
        currentDataPathLabel.setText(mavenCacheDir.getParent().toString());
        currentDataPathLabel.setFont(getPathFont(group.getDisplay()));
        currentDataPathLabel.setForeground(group.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
        GridData currentData = new GridData(GridData.FILL_HORIZONTAL);
        currentData.horizontalSpan = 2;
        currentDataPathLabel.setLayoutData(currentData);

        Label infoLabel = new Label(group, SWT.WRAP);
        infoLabel.setText(
                "Note: Changing the data location requires a Bio7 restart. Existing cached data will not be moved automatically.");
        infoLabel.setForeground(group.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
        GridData infoData = new GridData(GridData.FILL_HORIZONTAL);
        infoData.horizontalSpan = 3;
        infoData.widthHint = 400;
        infoLabel.setLayoutData(infoData);

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

        compilerLibsList = new List(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        compilerLibsList.setItems(getPreferenceArray(JAVA_LIBS_PREF));
        GridData listData = new GridData(GridData.FILL_HORIZONTAL);
        listData.heightHint = convertVerticalDLUsToPixels(LIST_HEIGHT_IN_DLUS);
        compilerLibsList.setLayoutData(listData);

        compilerLibsList.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                updateButtonStates();
            }
        });

        compilerLibsList.addListener(SWT.MouseDoubleClick, e -> {
            openSelectedLibraryLocation(compilerLibsList);
        });

        Composite buttonGroup = new Composite(group, SWT.NONE);
        buttonGroup.setLayout(new GridLayout(5, false));
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

        openCompilerLibLocationBtn = new Button(buttonGroup, SWT.PUSH);
        openCompilerLibLocationBtn.setText("Open Location");
        openCompilerLibLocationBtn.setToolTipText("Open the folder containing the selected library");
        openCompilerLibLocationBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        openCompilerLibLocationBtn.setEnabled(false);
        openCompilerLibLocationBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                openSelectedLibraryLocation(compilerLibsList);
            }
        });

        removeCompilerLibBtn = new Button(buttonGroup, SWT.PUSH);
        removeCompilerLibBtn.setText("Remove");
        removeCompilerLibBtn.setToolTipText("Remove selected libraries from the list");
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

        fragmentLibsList = new List(fragmentGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        fragmentLibsList.setItems(getPreferenceArray(FRAGMENT_LIBS_PREF));
        GridData listData = new GridData(GridData.FILL_HORIZONTAL);
        listData.heightHint = convertVerticalDLUsToPixels(LIST_HEIGHT_IN_DLUS);
        fragmentLibsList.setLayoutData(listData);

        fragmentLibsList.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                updateButtonStates();
            }
        });

        fragmentLibsList.addListener(SWT.MouseDoubleClick, e -> {
            openSelectedLibraryLocation(fragmentLibsList);
        });

        Composite buttonGroup = new Composite(fragmentGroup, SWT.NONE);
        buttonGroup.setLayout(new GridLayout(5, false));
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

        openFragmentLibLocationBtn = new Button(buttonGroup, SWT.PUSH);
        openFragmentLibLocationBtn.setText("Open Location");
        openFragmentLibLocationBtn.setToolTipText("Open the folder containing the selected library");
        openFragmentLibLocationBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        openFragmentLibLocationBtn.setEnabled(false);
        openFragmentLibLocationBtn.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                openSelectedLibraryLocation(fragmentLibsList);
            }
        });

        removeFragmentLibBtn = new Button(buttonGroup, SWT.PUSH);
        removeFragmentLibBtn.setText("Remove");
        removeFragmentLibBtn.setToolTipText("Remove selected libraries from the list");
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

    private void openSelectedLibraryLocation(List list) {
        String[] selection = list.getSelection();
        if (selection == null || selection.length == 0) {
            return;
        }

        String selectedPath = selection[0];
        File file = new File(selectedPath);
        File folder = file.isDirectory() ? file : file.getParentFile();

        if (folder != null && folder.exists()) {
            openFolderInFileManager(folder);
        } else {
            showErrorMessage("Folder Not Found", "The folder does not exist:\n" + folder);
        }
    }

    private void openFolderInFileManager(File folder) {
        try {
            String os = System.getProperty("os.name").toLowerCase();

            if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[] { "explorer.exe", folder.getAbsolutePath() });
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[] { "open", folder.getAbsolutePath() });
            } else {
                try {
                    Runtime.getRuntime().exec(new String[] { "xdg-open", folder.getAbsolutePath() });
                } catch (IOException e) {
                    try {
                        Runtime.getRuntime().exec(new String[] { "nautilus", folder.getAbsolutePath() });
                    } catch (IOException e2) {
                        Runtime.getRuntime().exec(new String[] { "dolphin", folder.getAbsolutePath() });
                    }
                }
            }
        } catch (Exception e) {
            if (!Program.launch(folder.getAbsolutePath())) {
                showErrorMessage("Cannot Open Folder",
                        "Could not open the file manager.\nPath: " + folder.getAbsolutePath());
            }
        }
    }

    private void updateFragmentSectionEnabled() {
        boolean enabled = enableFragmentLoadingBtn.getSelection();

        fragmentLibsList.setEnabled(enabled);
        addFragmentLibBtn.setEnabled(enabled);
        scanFragmentDirBtn.setEnabled(enabled);
        mavenFragmentBtn.setEnabled(enabled);
        removeFragmentLibBtn.setEnabled(enabled && fragmentLibsList.getSelectionCount() > 0);
        openFragmentLibLocationBtn.setEnabled(enabled && fragmentLibsList.getSelectionCount() > 0);
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
        openCompilerLibLocationBtn.setEnabled(compilerSelection > 0);
        removeFragmentLibBtn.setEnabled(fragmentEnabled && fragmentSelection > 0);
        openFragmentLibLocationBtn.setEnabled(fragmentEnabled && fragmentSelection > 0);
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
        setPreferenceArray(JAVA_LIBS_PREF, compilerLibsList.getItems());
        setPreferenceArray(FRAGMENT_LIBS_PREF, fragmentLibsList.getItems());
        getPreferenceStore().setValue(FRAGMENT_LOADING_ENABLED_PREF, enableFragmentLoadingBtn.getSelection());

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

        if (!newLocation.equals(oldLocation)) {
            initializeDataDirectory();

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
    // VERSION REPLACEMENT DATA CLASSES AND DIALOG
    // =========================================================================

    /**
     * Data class to hold information about a version replacement.
     */
    private static class VersionReplacement {
        final String groupId;
        final String artifactId;
        final String oldVersion;
        final String newVersion;

        VersionReplacement(String groupId, String artifactId, String oldVersion, String newVersion) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.oldVersion = oldVersion;
            this.newVersion = newVersion;
        }
    }

    /**
     * Dialog to confirm version replacements before proceeding.
     */
    private class VersionReplacementDialog extends Dialog {
        private final java.util.List<VersionReplacement> replacements;
        private boolean confirmed = false;

        public VersionReplacementDialog(Shell parentShell, java.util.List<VersionReplacement> replacements) {
            super(parentShell);
            this.replacements = replacements;
            setShellStyle(getShellStyle() | SWT.RESIZE);
        }

        @Override
        protected void configureShell(Shell newShell) {
            super.configureShell(newShell);
            newShell.setText("Confirm Version Replacement");
            newShell.setMinimumSize(500, 300);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite container = (Composite) super.createDialogArea(parent);
            container.setLayout(new GridLayout(1, false));

            Composite headerComp = new Composite(container, SWT.NONE);
            headerComp.setLayout(new GridLayout(2, false));
            headerComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Label iconLabel = new Label(headerComp, SWT.NONE);
            iconLabel.setImage(parent.getDisplay().getSystemImage(SWT.ICON_WARNING));

            Label messageLabel = new Label(headerComp, SWT.WRAP);
            messageLabel.setText("The following libraries already exist with different versions.\n"
                    + "Do you want to replace the old versions with the new ones?\n\n"
                    + "This will remove the old versions from both the list and the cache.");
            GridData msgData = new GridData(GridData.FILL_HORIZONTAL);
            msgData.widthHint = 400;
            messageLabel.setLayoutData(msgData);

            Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
            separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            org.eclipse.swt.widgets.List replacementList = new org.eclipse.swt.widgets.List(container,
                    SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
            GridData listData = new GridData(GridData.FILL_BOTH);
            listData.heightHint = 150;
            replacementList.setLayoutData(listData);

            for (VersionReplacement r : replacements) {
                String entry = r.artifactId + ":  " + r.oldVersion + "  →  " + r.newVersion;
                replacementList.add(entry);
            }

            Label summaryLabel = new Label(container, SWT.NONE);
            summaryLabel.setText("Total: " + replacements.size() + " library(ies) will be replaced.");
            summaryLabel.setForeground(container.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));

            return container;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, "Replace", true);
            createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
        }

        @Override
        protected void okPressed() {
            confirmed = true;
            super.okPressed();
        }

        public boolean isConfirmed() {
            return confirmed;
        }
    }

    // =========================================================================
    // MAVEN DOWNLOAD FUNCTIONALITY
    // =========================================================================

    /**
     * Redirects {@link System#out}/{@link System#err} for the duration of a
     * download {@link Job} so every existing {@code println} call also shows
     * up live in a {@link MavenDownloadProgressWindow} -- without having to
     * touch the dozens of individual logging call sites.
     */
    private static void runWithProgressWindow(Shell parentShell, String title, Consumer<MavenDownloadProgressWindow> body) {
        MavenDownloadProgressWindow progressWindow = new MavenDownloadProgressWindow(parentShell, title);
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        System.setOut(new PrintStream(new LineTeeOutputStream(originalOut, progressWindow::log), true));
        System.setErr(new PrintStream(new LineTeeOutputStream(originalErr, progressWindow::log), true));
        try {
            body.accept(progressWindow);
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
            progressWindow.setTitle(title + " (done)");
        }
    }

    /** Writes through to the wrapped stream and also forwards each completed line to a listener. */
    private static class LineTeeOutputStream extends OutputStream {
        private final OutputStream original;
        private final Consumer<String> lineListener;
        private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();

        LineTeeOutputStream(OutputStream original, Consumer<String> lineListener) {
            this.original = original;
            this.lineListener = lineListener;
        }

        @Override
        public synchronized void write(int b) throws IOException {
            original.write(b);
            if (b == '\n') {
                lineListener.accept(lineBuffer.toString());
                lineBuffer.reset();
            } else if (b != '\r') {
                lineBuffer.write(b);
            }
        }

        @Override
        public void flush() throws IOException {
            original.flush();
        }
    }

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

    /**
     * Check if artifacts already exist with the SAME version in the list.
     */
    private java.util.List<String> findAlreadyUpToDate(List targetList,
            java.util.List<MavenDownloadDialog.SimpleDep> deps) {
        java.util.List<String> upToDate = new ArrayList<>();
        String[] items = targetList.getItems();

        for (MavenDownloadDialog.SimpleDep dep : deps) {
            String groupPath = dep.groupId.replace('.', '/');
            String pattern = "/" + groupPath + "/" + dep.artifactId + "/" + dep.version + "/";

            for (String item : items) {
                String normalizedItem = item.replace("\\", "/");
                if (normalizedItem.contains(pattern)) {
                    upToDate.add(dep.groupId + ":" + dep.artifactId + ":" + dep.version);
                    break;
                }
            }
        }

        return upToDate;
    }

    /**
     * Check if a single artifact already exists with the SAME version.
     */
    private boolean isAlreadyUpToDate(List targetList, String groupId, String artifactId, String version) {
        String[] items = targetList.getItems();
        String groupPath = groupId.replace('.', '/');
        String pattern = "/" + groupPath + "/" + artifactId + "/" + version + "/";

        for (String item : items) {
            String normalizedItem = item.replace("\\", "/");
            if (normalizedItem.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find version replacements for batch dependencies.
     */
    private java.util.List<VersionReplacement> findVersionReplacementsForDeps(List targetList,
            java.util.List<MavenDownloadDialog.SimpleDep> deps) {
        java.util.List<VersionReplacement> replacements = new ArrayList<>();
        String[] items = targetList.getItems();

        for (MavenDownloadDialog.SimpleDep dep : deps) {
            String groupPath = dep.groupId.replace('.', '/');
            String pattern = "/" + groupPath + "/" + dep.artifactId + "/";

            for (String item : items) {
                String normalizedItem = item.replace("\\", "/");
                if (normalizedItem.contains(pattern)) {
                    String oldVersion = extractVersionFromPath(normalizedItem, groupPath, dep.artifactId);
                    if (oldVersion != null && !oldVersion.equals(dep.version)) {
                        boolean alreadyAdded = replacements.stream()
                                .anyMatch(r -> r.groupId.equals(dep.groupId) && r.artifactId.equals(dep.artifactId));
                        if (!alreadyAdded) {
                            replacements.add(new VersionReplacement(dep.groupId, dep.artifactId, oldVersion, dep.version));
                        }
                    }
                    break;
                }
            }
        }

        return replacements;
    }

    /**
     * Find version replacements for a single artifact.
     */
    private java.util.List<VersionReplacement> findVersionReplacementsForArtifact(List targetList,
            String groupId, String artifactId, String newVersion) {
        java.util.List<VersionReplacement> replacements = new ArrayList<>();
        String[] items = targetList.getItems();

        String groupPath = groupId.replace('.', '/');
        String pattern = "/" + groupPath + "/" + artifactId + "/";

        for (String item : items) {
            String normalizedItem = item.replace("\\", "/");
            if (normalizedItem.contains(pattern)) {
                String oldVersion = extractVersionFromPath(normalizedItem, groupPath, artifactId);
                if (oldVersion != null && !oldVersion.equals(newVersion)) {
                    replacements.add(new VersionReplacement(groupId, artifactId, oldVersion, newVersion));
                }
                break;
            }
        }

        return replacements;
    }

    /**
     * Find version replacements for a group download.
     */
    private java.util.List<VersionReplacement> findVersionReplacementsForGroup(List targetList,
            String groupId, String newVersion) {
        java.util.List<VersionReplacement> replacements = new ArrayList<>();
        String[] items = targetList.getItems();

        String groupPath = groupId.replace('.', '/');
        String groupPattern = "/" + groupPath + "/";

        java.util.Set<String> processedArtifacts = new java.util.HashSet<>();

        for (String item : items) {
            String normalizedItem = item.replace("\\", "/");
            if (normalizedItem.contains(groupPattern)) {
                int idx = normalizedItem.indexOf(groupPattern);
                if (idx == -1) continue;

                String remaining = normalizedItem.substring(idx + groupPattern.length());
                int slashIdx = remaining.indexOf('/');
                if (slashIdx == -1) continue;

                String artifactId = remaining.substring(0, slashIdx);

                if (processedArtifacts.contains(artifactId)) continue;
                processedArtifacts.add(artifactId);

                String oldVersion = extractVersionFromPath(normalizedItem, groupPath, artifactId);
                if (oldVersion != null && !oldVersion.equals(newVersion)) {
                    replacements.add(new VersionReplacement(groupId, artifactId, oldVersion, newVersion));
                }
            }
        }

        return replacements;
    }

    /**
     * Extract version from a JAR path.
     */
    private String extractVersionFromPath(String path, String groupPath, String artifactId) {
        try {
            String pattern = "/" + groupPath + "/" + artifactId + "/";
            int idx = path.indexOf(pattern);
            if (idx == -1) return null;

            String remaining = path.substring(idx + pattern.length());
            int slashIdx = remaining.indexOf('/');
            if (slashIdx == -1) return null;

            return remaining.substring(0, slashIdx);
        } catch (Exception e) {
            return null;
        }
    }

    private void downloadFromMaven(List targetList) {
        pomPropertiesCache.clear();

        MavenDownloadDialog dialog = new MavenDownloadDialog(
                getShell(),
                detectPlatformClassifier(),
                mavenCacheDir.getParent().getFileName().toString()
        );

        if (dialog.open() == Dialog.OK) {
            activeRepositories = dialog.getSelectedRepositories();
            java.util.List<MavenDownloadDialog.SimpleDep> batchDeps = dialog.getBatchDependencies();
            boolean downloadDependencies = dialog.isDownloadDependencies();
            boolean downloadNatives = dialog.isDownloadNatives();

            if (batchDeps != null && !batchDeps.isEmpty()) {
                // Check for version conflicts BEFORE downloading
                java.util.List<VersionReplacement> replacements = findVersionReplacementsForDeps(targetList, batchDeps);

                // Check for already up-to-date
                java.util.List<String> upToDate = findAlreadyUpToDate(targetList, batchDeps);

                if (!replacements.isEmpty()) {
                    VersionReplacementDialog confirmDialog = new VersionReplacementDialog(getShell(), replacements);
                    confirmDialog.open();
                    if (!confirmDialog.isConfirmed()) {
                        showInfoMessage("Download Cancelled", "Download was cancelled. No changes were made.");
                        return;
                    }
                } else if (upToDate.size() == batchDeps.size() && !downloadDependencies) {
                    showInfoMessage("Already Up-to-Date",
                            "All " + upToDate.size() + " requested libraries are already in the list with the same version.\n\n" +
                            "Enable 'Download dependencies' if you want to fetch transitive dependencies.");
                    return;
                }

                downloadBatchDependencies(targetList, batchDeps, downloadDependencies, downloadNatives, !replacements.isEmpty());
            } else {
                String groupId = dialog.getGroupId();
                String artifactId = dialog.getArtifactId();
                String version = dialog.getVersion();
                boolean downloadAllModules = dialog.isDownloadAllModules();

                if (groupId.isEmpty()) {
                    showErrorMessage("Invalid Input", "Please provide at least a Group ID.");
                    return;
                }

                if (downloadAllModules) {
                    if (version.isEmpty()) {
                        showErrorMessage("Invalid Input", "Please provide a version when downloading all modules.");
                        return;
                    }
                    java.util.List<VersionReplacement> replacements = findVersionReplacementsForGroup(targetList, groupId, version);

                    if (!replacements.isEmpty()) {
                        VersionReplacementDialog confirmDialog = new VersionReplacementDialog(getShell(), replacements);
                        confirmDialog.open();
                        if (!confirmDialog.isConfirmed()) {
                            showInfoMessage("Download Cancelled", "Download was cancelled. No changes were made.");
                            return;
                        }
                    }
                    downloadAllModulesFromGroup(targetList, groupId, version, downloadDependencies, downloadNatives, !replacements.isEmpty());
                } else {
                    if (artifactId.isEmpty() || version.isEmpty()) {
                        showErrorMessage("Invalid Input", "Please provide groupId, artifactId, and version.");
                        return;
                    }

                    // Check for version conflicts
                    java.util.List<VersionReplacement> replacements = findVersionReplacementsForArtifact(targetList, groupId, artifactId, version);

                    // Check if already up-to-date (same version exists)
                    if (replacements.isEmpty() && isAlreadyUpToDate(targetList, groupId, artifactId, version) && !downloadDependencies) {
                        showInfoMessage("Already Up-to-Date",
                                "The library " + artifactId + " version " + version + " is already in the list.\n\n" +
                                "Enable 'Download dependencies' if you want to fetch transitive dependencies.");
                        return;
                    }

                    if (!replacements.isEmpty()) {
                        VersionReplacementDialog confirmDialog = new VersionReplacementDialog(getShell(), replacements);
                        confirmDialog.open();
                        if (!confirmDialog.isConfirmed()) {
                            showInfoMessage("Download Cancelled", "Download was cancelled. No changes were made.");
                            return;
                        }
                    }
                    downloadMavenArtifact(targetList, groupId, artifactId, version, downloadDependencies, downloadNatives, !replacements.isEmpty());
                }
            }
        }
    }

    private void downloadBatchDependencies(List targetList,
            java.util.List<MavenDownloadDialog.SimpleDep> batchDeps, boolean downloadDependencies,
            boolean downloadNatives, boolean replaceOldVersions) {

        final Shell parentShell = getShell();

        Job downloadJob = new Job("Batch downloading Maven dependencies") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                final IStatus[] result = new IStatus[1];
                runWithProgressWindow(parentShell, "Maven Download Log", progressWindow -> {
                    try {
                        System.out.println("[Maven Batch] ==========================================");
                        System.out.println("[Maven Batch] Downloading " + batchDeps.size() + " dependencies");
                        System.out.println("[Maven Batch] Platform: " + detectPlatformClassifier());
                        System.out.println("[Maven Batch] Cache: " + mavenCacheDir);
                        System.out.println("[Maven Batch] ==========================================");

                        Files.createDirectories(mavenCacheDir);

                        java.util.List<Path> downloadedJars = new ArrayList<>();
                        java.util.List<String> failedDownloads = new ArrayList<>();
                        java.util.Set<String> processedArtifacts = new java.util.HashSet<>();

                        monitor.beginTask("Downloading batch dependencies", batchDeps.size());

                        for (MavenDownloadDialog.SimpleDep dep : batchDeps) {
                            if (monitor.isCanceled())
                                break;

                            String depGroupId = dep.groupId;
                            String depArtifactId = dep.artifactId;
                            String depVersion = dep.version;
                            String depKey = depGroupId + ":" + depArtifactId;

                            if (processedArtifacts.contains(depKey)) {
                                monitor.worked(1);
                                continue;
                            }
                            processedArtifacts.add(depKey);

                            monitor.subTask("Downloading: " + depArtifactId);
                            System.out.println("[Maven Batch] Processing: " + depKey + ":" + depVersion);

                            if (isPomOnlyArtifact(depGroupId, depArtifactId, depVersion)) {
                                System.out.println("[Maven Batch] POM-only artifact: " + depKey
                                        + ", resolving transitive dependencies...");
                                downloadDependenciesRecursively(depGroupId, depArtifactId, depVersion, downloadedJars,
                                        failedDownloads, processedArtifacts, monitor, 0, downloadNatives);
                            } else {
                                Path jar = downloadJar(depGroupId, depArtifactId, depVersion, null, monitor);

                                if (jar != null) {
                                    downloadedJars.add(jar);
                                    System.out.println("[Maven Batch] Downloaded JAR: " + jar.getFileName());

                                    if (downloadNatives) {
                                        downloadNativeJars(depGroupId, depArtifactId, depVersion, downloadedJars, monitor);
                                    }

                                    if (downloadDependencies) {
                                        downloadDependenciesRecursively(depGroupId, depArtifactId, depVersion,
                                                downloadedJars, failedDownloads, processedArtifacts, monitor, 0,
                                                downloadNatives);
                                    }
                                } else {
                                    System.err.println("[Maven Batch] Failed to download: " + depKey);
                                    failedDownloads.add(depKey + ":" + depVersion);
                                }
                            }

                            monitor.worked(1);
                        }

                        monitor.done();

                        final java.util.List<Path> jarsToAdd = downloadedJars;
                        final java.util.List<String> failed = failedDownloads;

                        Display.getDefault().asyncExec(() -> {
                            addJarsToList(targetList, jarsToAdd, failed, "Batch download complete", replaceOldVersions);
                        });

                        result[0] = Status.OK_STATUS;

                    } catch (Exception e) {
                        e.printStackTrace();
                        Display.getDefault().asyncExec(() -> {
                            showErrorMessage("Batch Download Error", "Error: " + e.getMessage());
                        });
                        result[0] = new Status(IStatus.ERROR, "com.eco.bio7.javaedit", "Batch download failed", e);
                    }
                });
                return result[0];
            }
        };

        downloadJob.setUser(true);
        downloadJob.schedule();
    }

    private void downloadAllModulesFromGroup(List targetList, String groupId, String version,
            boolean downloadDependencies, boolean downloadNatives, boolean replaceOldVersions) {
        final Shell parentShell = getShell();
        Job downloadJob = new Job("Downloading all modules from: " + groupId) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                final IStatus[] result = new IStatus[] { Status.OK_STATUS };
                runWithProgressWindow(parentShell, "Maven Download Log", progressWindow -> {
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

                        java.util.List<String[]> allArtifacts = MavenDownloadDialog.searchArtifactsWithVersion(groupId, version);

                        if (allArtifacts.isEmpty()) {
                            Display.getDefault().asyncExec(() -> {
                                showErrorMessage("No Modules Found",
                                        "No artifacts found in group: " + groupId + " with version " + version);
                            });
                            return;
                        }

                        Files.createDirectories(mavenCacheDir);

                        java.util.List<Path> downloadedJars = new ArrayList<>();
                        java.util.List<String> failedDownloads = new ArrayList<>();
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

                            if (isPomOnlyArtifact(artGroupId, artArtifactId, artVersion)) {
                                if (downloadDependencies) {
                                    downloadDependenciesRecursively(artGroupId, artArtifactId, artVersion, downloadedJars,
                                            failedDownloads, processedArtifacts, monitor, 0, downloadNatives);
                                }
                            } else {
                                Path jar = downloadJar(artGroupId, artArtifactId, artVersion, null, monitor);
                                if (jar != null) {
                                    downloadedJars.add(jar);

                                    if (downloadNatives) {
                                        downloadNativeJars(artGroupId, artArtifactId, artVersion, downloadedJars, monitor);
                                    }

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
                            addJarsToList(targetList, jarsToAdd, failed, "Group: " + groupId + "\nVersion: " + version, replaceOldVersions);
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        Display.getDefault().asyncExec(() -> {
                            showErrorMessage("Download Error", "Error: " + e.getMessage());
                        });
                        result[0] = new Status(IStatus.ERROR, "com.eco.bio7.javaedit", "Download failed", e);
                    }
                });
                return result[0];
            }
        };

        downloadJob.setUser(true);
        downloadJob.schedule();
    }

    private void downloadMavenArtifact(List targetList, String groupId, String artifactId, String version,
            boolean downloadDependencies, boolean downloadNatives, boolean replaceOldVersions) {
        final Shell parentShell = getShell();
        Job downloadJob = new Job("Downloading: " + artifactId) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                final IStatus[] result = new IStatus[] { Status.OK_STATUS };
                runWithProgressWindow(parentShell, "Maven Download Log", progressWindow -> {
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

                        boolean isPomOnly = isPomOnlyArtifact(groupId, artifactId, version);

                        if (isPomOnly) {
                            processedArtifacts.add(groupId + ":" + artifactId);
                            System.out.println("[Maven Download] POM-only artifact, downloading dependencies...");
                        } else {
                            Path mainJar = downloadJar(groupId, artifactId, version, null, monitor);

                            if (mainJar != null) {
                                downloadedJars.add(mainJar);
                                processedArtifacts.add(groupId + ":" + artifactId);

                                if (downloadNatives) {
                                    downloadNativeJars(groupId, artifactId, version, downloadedJars, monitor);
                                }
                            } else {
                                failedDownloads.add(groupId + ":" + artifactId + ":" + version);
                            }
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
                            addJarsToList(targetList, jarsToAdd, failed, wasPomOnly ? "(POM aggregator) " : "", replaceOldVersions);
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        Display.getDefault().asyncExec(() -> {
                            showErrorMessage("Download Error", "Error: " + e.getMessage());
                        });
                        result[0] = new Status(IStatus.ERROR, "com.eco.bio7.javaedit", "Download failed", e);
                    }
                });
                return result[0];
            }
        };

        downloadJob.setUser(true);
        downloadJob.schedule();
    }

    /**
     * Performs a GET against {@code relativePath} on each of
     * {@link #activeRepositories} in order, returning the first repository's
     * body as {@code {resolvedUrl, body}}, or {@code null} if none of them have
     * it.
     */
    private String[] httpGetFromRepositories(String relativePath) {
        for (String repo : activeRepositories) {
            String urlStr = repo + relativePath;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "Bio7/1.0");

                if (conn.getResponseCode() != 200) {
                    continue;
                }

                StringBuilder content = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
                return new String[] { urlStr, content.toString() };
            } catch (Exception e) {
                // try the next repository
            }
        }
        return null;
    }

    /** Returns {@code true} if {@code relativePath} responds with 200 to a HEAD on any of {@link #activeRepositories}. */
    private boolean httpHeadExistsInRepositories(String relativePath) {
        for (String repo : activeRepositories) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(repo + relativePath).openConnection();
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "Bio7/1.0");
                boolean exists = conn.getResponseCode() == 200;
                conn.disconnect();
                if (exists) {
                    return true;
                }
            } catch (Exception e) {
                // try the next repository
            }
        }
        return false;
    }

    private boolean isPomOnlyArtifact(String groupId, String artifactId, String version) {
        try {
            String groupPath = groupId.replace('.', '/');
            String basePath = "/" + groupPath + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version;

            String[] pomResult = httpGetFromRepositories(basePath + ".pom");
            if (pomResult != null) {
                Pattern packagingPattern = Pattern.compile("<packaging>\\s*([^<]+)\\s*</packaging>");
                Matcher matcher = packagingPattern.matcher(pomResult[1]);
                if (matcher.find()) {
                    String packaging = matcher.group(1).trim().toLowerCase();
                    if ("pom".equals(packaging)) {
                        System.out.println("[Maven] Detected POM-only artifact (packaging=pom): " + groupId + ":"
                                + artifactId + ":" + version);
                        return true;
                    }
                }
            }

            if (!httpHeadExistsInRepositories(basePath + ".jar")) {
                System.out.println("[Maven] Detected POM-only artifact (no JAR found): " + groupId + ":" + artifactId
                        + ":" + version);
                return true;
            }

            return false;

        } catch (Exception e) {
            System.err.println("[Maven] Error checking if POM-only: " + e.getMessage());
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
            String relativePath = "/" + groupPath + "/" + artifactId + "/" + version + "/" + jarName;

            Path targetDir = mavenCacheDir.resolve(groupPath).resolve(artifactId).resolve(version);
            Files.createDirectories(targetDir);
            Path targetFile = targetDir.resolve(jarName);

            if (Files.exists(targetFile) && Files.size(targetFile) > 0) {
                System.out.println("[Maven Download] Using cached: " + targetFile);
                return targetFile;
            }

            for (String repo : activeRepositories) {
                String urlStr = repo + relativePath;
                System.out.println("[Maven Download] Downloading: " + urlStr);
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(60000);
                    conn.setInstanceFollowRedirects(true);
                    conn.setRequestProperty("User-Agent", "Bio7/1.0");

                    int responseCode = conn.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                            || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                            || responseCode == HttpURLConnection.HTTP_SEE_OTHER || responseCode == 307
                            || responseCode == 308) {
                        String newUrl = conn.getHeaderField("Location");
                        conn = (HttpURLConnection) new URL(newUrl).openConnection();
                        conn.setRequestProperty("User-Agent", "Bio7/1.0");
                        responseCode = conn.getResponseCode();
                    }

                    if (responseCode != 200) {
                        System.err.println("[Maven Download] Failed: " + responseCode + " for " + jarName
                                + " at " + repo);
                        continue;
                    }

                    try (InputStream in = conn.getInputStream()) {
                        Files.copy(in, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    }

                    System.out.println("[Maven Download] Success: " + targetFile);
                    return targetFile;
                } catch (Exception e) {
                    System.err.println("[Maven Download] Error from " + repo + ": " + e.getMessage());
                }
            }

            return null;

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

    // =========================================================================
    // VERSION REPLACEMENT FUNCTIONALITY
    // =========================================================================

    /**
     * Remove existing versions of an artifact from the list.
     */
    private int removeExistingVersions(List targetList, String groupId, String artifactId) {
        String[] items = targetList.getItems();
        java.util.List<Integer> indicesToRemove = new ArrayList<>();

        String groupPath = groupId.replace('.', '/');
        String pattern = "/" + groupPath + "/" + artifactId + "/";

        for (int i = 0; i < items.length; i++) {
            String item = items[i].replace("\\", "/");
            if (item.contains(pattern)) {
                indicesToRemove.add(i);
            }
        }

        for (int i = indicesToRemove.size() - 1; i >= 0; i--) {
            targetList.remove(indicesToRemove.get(i));
        }

        if (!indicesToRemove.isEmpty()) {
            System.out.println("[Maven] Removed " + indicesToRemove.size() + " old version(s) of " + groupId + ":" + artifactId);
        }

        return indicesToRemove.size();
    }

    /**
     * Extract groupId, artifactId, and version from a cached JAR path.
     */
    private String[] extractArtifactInfo(Path jarPath) {
        try {
            Path parent = jarPath.getParent();
            if (parent == null) return null;

            String version = parent.getFileName().toString();

            parent = parent.getParent();
            if (parent == null) return null;

            String artifactId = parent.getFileName().toString();

            Path groupPath = parent.getParent();
            if (groupPath == null) return null;

            java.util.List<String> groupParts = new ArrayList<>();
            while (groupPath != null && !groupPath.getFileName().toString().equals(".maven-cache")) {
                groupParts.add(0, groupPath.getFileName().toString());
                groupPath = groupPath.getParent();
            }

            if (groupParts.isEmpty()) return null;

            String groupId = String.join(".", groupParts);

            return new String[] { groupId, artifactId, version };
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Delete old versions of an artifact from the Maven cache.
     */
    private void cleanOldVersionsFromCache(String groupId, String artifactId, String keepVersion) {
        try {
            String groupPath = groupId.replace('.', '/');
            Path artifactDir = mavenCacheDir.resolve(groupPath).resolve(artifactId);

            if (!Files.exists(artifactDir) || !Files.isDirectory(artifactDir)) {
                return;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(artifactDir)) {
                for (Path versionDir : stream) {
                    if (Files.isDirectory(versionDir)) {
                        String version = versionDir.getFileName().toString();
                        if (!version.equals(keepVersion)) {
                            System.out.println("[Maven Cache] Removing old version from cache: " + groupId + ":" + artifactId + ":" + version);
                            deleteDirectory(versionDir);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Maven Cache] Error cleaning old versions: " + e.getMessage());
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Add JARs to the list. If replaceOldVersions is true, remove old versions first.
     */
    private void addJarsToList(List targetList, java.util.List<Path> jarsToAdd, java.util.List<String> failed,
            String prefix, boolean replaceOldVersions) {
        if (jarsToAdd.isEmpty()) {
            showErrorMessage("Download Failed", "No JAR files could be downloaded.");
            return;
        }

        int removedCount = 0;

        if (replaceOldVersions) {
            // Track which artifacts we're adding (to remove old versions)
            java.util.Map<String, String> artifactVersions = new java.util.HashMap<>();
            for (Path jar : jarsToAdd) {
                String[] parts = extractArtifactInfo(jar);
                if (parts != null) {
                    String key = parts[0] + ":" + parts[1];
                    artifactVersions.put(key, parts[2]);
                }
            }

            // Remove old versions of artifacts we're about to add
            for (java.util.Map.Entry<String, String> entry : artifactVersions.entrySet()) {
                String[] parts = entry.getKey().split(":");
                if (parts.length == 2) {
                    removedCount += removeExistingVersions(targetList, parts[0], parts[1]);
                    cleanOldVersionsFromCache(parts[0], parts[1], entry.getValue());
                }
            }
        }

        // Now add the new JARs
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

        // Build appropriate message
        StringBuilder message = new StringBuilder();
        if (!prefix.isEmpty()) {
            message.append(prefix).append("\n");
        }

        // Choose appropriate title and message based on what happened
        String title;

        if (added == 0 && removedCount == 0 && skipped > 0) {
            // Everything was already in the list with the same version
            title = "Already Up-to-Date";
            message.append("All ").append(skipped).append(" JAR(s) are already in the list.\n");
            message.append("No changes were made.");
        } else {
            if (removedCount > 0) {
                title = "Maven Download Complete (Updated)";
            } else {
                title = "Maven Download Complete";
            }

            message.append("Downloaded: ").append(jarsToAdd.size()).append(" JAR(s)\n");

            if (added > 0) {
                message.append("Added to list: ").append(added).append("\n");
            }

            if (removedCount > 0) {
                message.append("Replaced (old versions): ").append(removedCount).append("\n");
            }

            if (skipped > 0) {
                message.append("Already exists (same version): ").append(skipped).append("\n");
            }

            if (!failed.isEmpty()) {
                message.append("\nFailed: ").append(failed.size()).append(" (some are normal)");
            }
        }

        showInfoMessage(title, message.toString());
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

            if (isPomOnlyArtifact(depGroupId, depArtifactId, depVersion)) {
                System.out.println("[Maven] " + ind + "  POM-only: " + depKey + ", resolving dependencies...");
                downloadDependenciesRecursively(depGroupId, depArtifactId, depVersion, downloadedJars, failedDownloads,
                        processedArtifacts, monitor, depth + 1, downloadNatives);
            } else {
                Path depJar = downloadJar(depGroupId, depArtifactId, depVersion, null, monitor);
                if (depJar != null) {
                    downloadedJars.add(depJar);

                    if (downloadNatives) {
                        downloadNativeJars(depGroupId, depArtifactId, depVersion, downloadedJars, monitor);
                    }

                    downloadDependenciesRecursively(depGroupId, depArtifactId, depVersion, downloadedJars,
                            failedDownloads, processedArtifacts, monitor, depth + 1, downloadNatives);
                } else if (!isOptionalPlatformArtifact(depArtifactId)) {
                    failedDownloads.add(depKey + ":" + depVersion);
                }
            }
        }
    }

    // =========================================================================
    // POM PARSING WITH RECURSIVE PARENT PROPERTY RESOLUTION
    // =========================================================================

    private java.util.Map<String, String> fetchPomPropertiesRecursively(String groupId, String artifactId,
            String version, int depth) {

        String cacheKey = groupId + ":" + artifactId + ":" + version;
        if (pomPropertiesCache.containsKey(cacheKey)) {
            return new java.util.HashMap<>(pomPropertiesCache.get(cacheKey));
        }

        java.util.Map<String, String> properties = new java.util.HashMap<>();

        if (depth > 10) {
            System.out.println("[Maven POM] Max depth reached for properties: " + cacheKey);
            return properties;
        }

        try {
            String groupPath = groupId.replace('.', '/');
            String relativePath = "/" + groupPath + "/" + artifactId + "/" + version + "/" + artifactId
                    + "-" + version + ".pom";

            if (depth == 0) {
                System.out.println("[Maven POM] Fetching properties from: " + cacheKey);
            } else {
                System.out.println("[Maven POM] " + "  ".repeat(depth) + "Parent: " + cacheKey);
            }

            String[] result = httpGetFromRepositories(relativePath);
            if (result == null) {
                return properties;
            }

            String pom = result[1];

            String parentGroupId = extractXmlValueFromParent(pom, "groupId");
            String parentArtifactId = extractXmlValueFromParent(pom, "artifactId");
            String parentVersion = extractXmlValueFromParent(pom, "version");

            if (parentGroupId != null && parentArtifactId != null && parentVersion != null) {
                java.util.Map<String, String> parentProps = fetchPomPropertiesRecursively(parentGroupId,
                        parentArtifactId, parentVersion, depth + 1);
                properties.putAll(parentProps);
            }

            properties.put("project.version", version);
            properties.put("project.groupId", groupId);
            properties.put("project.artifactId", artifactId);
            if (parentVersion != null) {
                properties.put("project.parent.version", parentVersion);
            }

            extractPropertiesFromPom(pom, properties);

            pomPropertiesCache.put(cacheKey, new java.util.HashMap<>(properties));

        } catch (Exception e) {
            System.err.println("[Maven POM] Error fetching properties from " + cacheKey + ": " + e.getMessage());
        }

        return properties;
    }

    private java.util.Map<String, String> fetchManagedDependencyVersionsRecursively(String groupId, String artifactId,
            String version, java.util.Map<String, String> properties, int depth) {

        java.util.Map<String, String> managedVersions = new java.util.HashMap<>();

        if (depth > 10) {
            return managedVersions;
        }

        try {
            String groupPath = groupId.replace('.', '/');
            String relativePath = "/" + groupPath + "/" + artifactId + "/" + version + "/" + artifactId
                    + "-" + version + ".pom";

            String[] result = httpGetFromRepositories(relativePath);
            if (result == null) {
                return managedVersions;
            }

            String pom = result[1];

            String parentGroupId = extractXmlValueFromParent(pom, "groupId");
            String parentArtifactId = extractXmlValueFromParent(pom, "artifactId");
            String parentVersion = extractXmlValueFromParent(pom, "version");

            if (parentGroupId != null && parentArtifactId != null && parentVersion != null) {
                java.util.Map<String, String> parentManagedVersions = fetchManagedDependencyVersionsRecursively(
                        parentGroupId, parentArtifactId, parentVersion, properties, depth + 1);
                managedVersions.putAll(parentManagedVersions);
            }

            java.util.Map<String, String> currentManagedVersions = extractManagedDependencyVersions(pom, properties);
            managedVersions.putAll(currentManagedVersions);

        } catch (Exception e) {
            // Silently ignore
        }

        return managedVersions;
    }

    private java.util.List<String[]> parsePomForDependenciesWithVersions(String groupId, String artifactId,
            String version) {
        java.util.List<String[]> dependencies = new ArrayList<>();

        try {
            String groupPath = groupId.replace('.', '/');
            String pomName = artifactId + "-" + version + ".pom";
            String relativePath = "/" + groupPath + "/" + artifactId + "/" + version + "/" + pomName;

            String[] result = httpGetFromRepositories(relativePath);
            if (result == null) {
                System.err.println("[Maven POM] Failed to fetch POM " + groupId + ":" + artifactId + ":" + version
                        + " from any repository: " + relativePath);
                return dependencies;
            }
            System.out.println("[Maven POM] Fetching: " + result[0]);

            String pom = result[1];

            String parentGroupId = extractXmlValueFromParent(pom, "groupId");
            String parentArtifactId = extractXmlValueFromParent(pom, "artifactId");
            String parentVersion = extractXmlValueFromParent(pom, "version");

            java.util.Map<String, String> properties = new java.util.HashMap<>();

            if (parentGroupId != null && parentArtifactId != null && parentVersion != null) {
                System.out.println("[Maven POM] Found parent: " + parentGroupId + ":" + parentArtifactId + ":" + parentVersion);

                properties = fetchPomPropertiesRecursively(parentGroupId, parentArtifactId, parentVersion, 0);

                java.util.Map<String, String> managedVersions = fetchManagedDependencyVersionsRecursively(
                        parentGroupId, parentArtifactId, parentVersion, properties, 0);
                for (java.util.Map.Entry<String, String> entry : managedVersions.entrySet()) {
                    properties.put("managed." + entry.getKey(), entry.getValue());
                }
            }

            properties.put("project.version", version);
            properties.put("project.groupId", groupId);
            properties.put("project.artifactId", artifactId);
            if (parentVersion != null) {
                properties.put("project.parent.version", parentVersion);
            }

            extractPropertiesFromPom(pom, properties);

            java.util.Map<String, String> currentManagedVersions = extractManagedDependencyVersions(pom, properties);
            for (java.util.Map.Entry<String, String> entry : currentManagedVersions.entrySet()) {
                properties.put("managed." + entry.getKey(), entry.getValue());
            }

            String depsSection = extractMainDependenciesSection(pom);

            if (depsSection == null || depsSection.isEmpty()) {
                System.out.println("[Maven POM] No dependencies section found");
                return dependencies;
            }

            int pos = 0;
            while (true) {
                int depStart = depsSection.indexOf("<dependency>", pos);
                if (depStart == -1)
                    break;

                int depEnd = depsSection.indexOf("</dependency>", depStart);
                if (depEnd == -1)
                    break;

                String dep = depsSection.substring(depStart, depEnd + "</dependency>".length());

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

                depGroupId = MavenDownloadDialog.resolveProperties(depGroupId, properties);
                depArtifactId = MavenDownloadDialog.resolveProperties(depArtifactId, properties);
                depVersion = MavenDownloadDialog.resolveProperties(depVersion, properties);

                if ((depVersion == null || depVersion.isEmpty() || depVersion.contains("${")) && depGroupId != null
                        && depArtifactId != null) {
                    String managedKey = depGroupId + ":" + depArtifactId;
                    String managedVersion = properties.get("managed." + managedKey);
                    if (managedVersion != null) {
                        depVersion = MavenDownloadDialog.resolveProperties(managedVersion, properties);
                        System.out.println("[Maven POM] Using managed version for " + managedKey + ": " + depVersion);
                    }
                }

                if (depArtifactId != null && depArtifactId.contains("${")) {
                    pos = depEnd;
                    continue;
                }

                if (depVersion != null && depVersion.contains("${")) {
                    System.out.println("[Maven POM] Attempting to resolve: " + depVersion);
                    depVersion = MavenDownloadDialog.resolveProperties(depVersion, properties);

                    if (depVersion.contains("${")) {
                        String latestVersion = lookupLatestVersion(depGroupId, depArtifactId);
                        if (latestVersion != null) {
                            System.out.println("[Maven POM] Looked up latest version for " + depGroupId + ":" + depArtifactId + ": " + latestVersion);
                            depVersion = latestVersion;
                        } else {
                            System.out.println("[Maven POM] Skipping unresolved version: " + depGroupId + ":" + depArtifactId + ":" + depVersion);
                            pos = depEnd;
                            continue;
                        }
                    }
                }

                if ((depVersion == null || depVersion.isEmpty()) && depGroupId != null && depArtifactId != null) {
                    depVersion = lookupLatestVersion(depGroupId, depArtifactId);
                    if (depVersion != null) {
                        System.out.println("[Maven POM] Looked up latest version for " + depGroupId + ":" + depArtifactId + ": " + depVersion);
                    }
                }

                if (depGroupId != null && depArtifactId != null && depVersion != null && !depVersion.isEmpty()) {
                    System.out.println("[Maven POM] Found dependency: " + depGroupId + ":" + depArtifactId + ":" + depVersion);
                    dependencies.add(new String[] { depGroupId, depArtifactId, depVersion });
                }

                pos = depEnd;
            }

            System.out.println("[Maven POM] Total dependencies found: " + dependencies.size());

        } catch (Exception e) {
            System.err.println("[Maven POM] Error: " + e.getMessage());
            e.printStackTrace();
        }

        return dependencies;
    }

    private void extractPropertiesFromPom(String pom, java.util.Map<String, String> properties) {
        int propsStart = pom.indexOf("<properties>");
        int propsEnd = pom.indexOf("</properties>");
        if (propsStart != -1 && propsEnd != -1 && propsEnd > propsStart) {
            String propsSection = pom.substring(propsStart, propsEnd);
            Pattern propPattern = Pattern.compile("<([a-zA-Z0-9._-]+)>([^<]*)</\\1>");
            Matcher propMatcher = propPattern.matcher(propsSection);
            while (propMatcher.find()) {
                String propName = propMatcher.group(1);
                String propValue = propMatcher.group(2).trim();
                propValue = MavenDownloadDialog.resolveProperties(propValue, properties);
                properties.put(propName, propValue);
            }
        }
    }

    private java.util.Map<String, String> extractManagedDependencyVersions(String pom,
            java.util.Map<String, String> properties) {
        java.util.Map<String, String> managedVersions = new java.util.HashMap<>();

        int dmStart = pom.indexOf("<dependencyManagement>");
        int dmEnd = pom.indexOf("</dependencyManagement>");

        if (dmStart == -1 || dmEnd == -1 || dmEnd < dmStart) {
            return managedVersions;
        }

        String dmSection = pom.substring(dmStart, dmEnd);

        int pos = 0;
        while (true) {
            int depStart = dmSection.indexOf("<dependency>", pos);
            if (depStart == -1)
                break;

            int depEnd = dmSection.indexOf("</dependency>", depStart);
            if (depEnd == -1)
                break;

            String dep = dmSection.substring(depStart, depEnd);

            String depGroupId = extractXmlValue(dep, "groupId");
            String depArtifactId = extractXmlValue(dep, "artifactId");
            String depVersion = extractXmlValue(dep, "version");

            depGroupId = MavenDownloadDialog.resolveProperties(depGroupId, properties);
            depArtifactId = MavenDownloadDialog.resolveProperties(depArtifactId, properties);
            depVersion = MavenDownloadDialog.resolveProperties(depVersion, properties);

            if (depGroupId != null && depArtifactId != null && depVersion != null && !depVersion.contains("${")) {
                String key = depGroupId + ":" + depArtifactId;
                managedVersions.put(key, depVersion);
            }

            pos = depEnd;
        }

        return managedVersions;
    }

    private String extractMainDependenciesSection(String pom) {
        int dmStart = pom.indexOf("<dependencyManagement>");
        int dmEnd = pom.indexOf("</dependencyManagement>");

        String pomWithoutDM = pom;
        if (dmStart != -1 && dmEnd != -1 && dmEnd > dmStart) {
            pomWithoutDM = pom.substring(0, dmStart) + pom.substring(dmEnd + "</dependencyManagement>".length());
        }

        int depsStart = pomWithoutDM.indexOf("<dependencies>");
        int depsEnd = pomWithoutDM.indexOf("</dependencies>");

        if (depsStart != -1 && depsEnd != -1 && depsEnd > depsStart) {
            return pomWithoutDM.substring(depsStart + "<dependencies>".length(), depsEnd);
        }

        return null;
    }

    private String extractXmlValueFromParent(String xml, String childTag) {
        int parentStart = xml.indexOf("<parent>");
        if (parentStart == -1)
            return null;

        int parentEnd = xml.indexOf("</parent>", parentStart);
        if (parentEnd == -1)
            return null;

        String parentSection = xml.substring(parentStart, parentEnd);
        return extractXmlValue(parentSection, childTag);
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

    private String lookupLatestVersion(String groupId, String artifactId) {
        return MavenDownloadDialog.lookupLatestVersion(groupId, artifactId, activeRepositories);
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