package com.eco.bio7.javapreferences;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog for downloading Maven artifacts from Maven Central.
 * Supports single artifact download, batch paste with property resolution,
 * and downloading all modules from a group.
 */
public class MavenDownloadDialog extends Dialog {

    private static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";

    // UI components
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

    // Batch mode fields
    private Text mavenDependenciesText;
    private Button parseButton;
    private final java.util.List<SimpleDep> parsedBatchDependencies = new java.util.ArrayList<>();

    // Result fields
    private String groupId = "";
    private String artifactId = "";
    private String version = "";
    private boolean downloadDependencies = true;
    private boolean downloadAllModules = false;
    private boolean downloadNatives = true;

    // Platform info (passed from parent)
    private final String platformClassifier;
    private final String cacheDisplayName;

    /**
     * Helper class for parsed dependencies
     */
    public static class SimpleDep {
        public final String groupId;
        public final String artifactId;
        public final String version;

        public SimpleDep(String g, String a, String v) {
            this.groupId = g;
            this.artifactId = a;
            this.version = v;
        }

        @Override
        public String toString() {
            return groupId + ":" + artifactId + ":" + version;
        }
    }

    // Popular libraries presets
    private static final String[][] POPULAR_LIBRARIES = {
        { "-- Select a popular library --", "", "", "" },
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
        { "Pi4J Core", "com.pi4j", "pi4j-core", "2.6.0" },
        { "OpenCV", "org.openpnp", "opencv", "4.9.0-0" },
        { "JFreeChart", "org.jfree", "jfreechart", "1.5.4" },
        { "Apache POI", "org.apache.poi", "poi", "5.2.5" },
        { "Jsoup HTML Parser", "org.jsoup", "jsoup", "1.17.2" },
        { "OkHttp", "com.squareup.okhttp3", "okhttp", "4.12.0" },
        { "SQLite JDBC", "org.xerial", "sqlite-jdbc", "3.45.3.0" },
        { "MySQL Connector", "com.mysql", "mysql-connector-j", "8.4.0" },
        { "PostgreSQL JDBC", "org.postgresql", "postgresql", "42.7.3" },
        { "H2 Database", "com.h2database", "h2", "2.2.224" },
        { "Deeplearning4j Core", "org.deeplearning4j", "deeplearning4j-core", "1.0.0-M2.1" },
        { "ND4J Native Platform", "org.nd4j", "nd4j-native-platform", "1.0.0-M2.1" },
    };

    // Patterns for parsing
    private static final Pattern MAVEN_GROUP_ID = Pattern.compile("<groupId>\\s*([^<]+)\\s*</groupId>");
    private static final Pattern MAVEN_ARTIFACT_ID = Pattern.compile("<artifactId>\\s*([^<]+)\\s*</artifactId>");
    private static final Pattern MAVEN_VERSION = Pattern.compile("<version>\\s*([^<]+)\\s*</version>");
    private static final Pattern GRADLE_PATTERN = Pattern.compile(
            "(?:implementation|api|compile|runtimeOnly|testImplementation)\\s*['\"]([^:]+):([^:]+):([^'\"]+)['\"]");
    private static final Pattern SIMPLE_PATTERN = Pattern.compile("^([^:]+):([^:]+):([^:]+)$");
    private Composite container_1;

    /**
     * Creates a new Maven download dialog.
     *
     * @param parentShell        the parent shell
     * @param platformClassifier the platform classifier (e.g., "macosx-arm64")
     * @param cacheDisplayName   the display name for the cache location
     */
    public MavenDownloadDialog(Shell parentShell, String platformClassifier, String cacheDisplayName) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE);
        this.platformClassifier = platformClassifier;
        this.cacheDisplayName = cacheDisplayName;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Download from Maven Central");
        newShell.setMinimumSize(550, 400);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        container_1 = (Composite) super.createDialogArea(parent);
        container_1.setLayout(new GridLayout(4, false));

        createBatchPasteSection(container_1);
        createSeparator(container_1);
        createSingleArtifactSection(container_1);
        createSeparator(container_1);
        createOptionsSection(container_1);
        createSeparator(container_1);
        createPreviewSection(container_1);
        createStatusSection(container_1);

        return container_1;
    }

    private void createBatchPasteSection(Composite container) {
        Label batchLabel = new Label(container, SWT.NONE);
        batchLabel.setText("Paste Maven XML (with <properties> and <dependencies>):");
        GridData batchLabelGd = new GridData(GridData.FILL_HORIZONTAL);
        batchLabelGd.horizontalSpan = 4;
        batchLabel.setLayoutData(batchLabelGd);

        mavenDependenciesText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData pasteGD = new GridData(GridData.FILL_HORIZONTAL);
        pasteGD.heightHint = 120;
        pasteGD.horizontalSpan = 4;
        mavenDependenciesText.setLayoutData(pasteGD);

        parseButton = new Button(container, SWT.PUSH);
        parseButton.setText("Parse Dependencies");
        GridData parseBtnGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        parseBtnGd.widthHint = 432;
        parseBtnGd.horizontalSpan = 4;
        parseButton.setLayoutData(parseBtnGd);

        parseButton.addListener(SWT.Selection, e -> {
            String xml = mavenDependenciesText.getText();
            java.util.List<SimpleDep> parsed = parseMavenXmlDependencies(xml);
            parsedBatchDependencies.clear();
            parsedBatchDependencies.addAll(parsed);
            if (parsed.isEmpty()) {
                setStatus("No valid <dependency> blocks found (check property definitions).", true);
                return;
            }
            setStatus("Parsed " + parsed.size() + " dependencies. Click 'Download' to start.", false);
            previewList.removeAll();
            for (SimpleDep dep : parsed) {
                previewList.add(dep.toString());
            }
        });
    }

    private void createSingleArtifactSection(Composite container) {
        // Platform and cache info
        Label infoLabel = new Label(container, SWT.WRAP);
        infoLabel.setText("Platform: " + platformClassifier + "  |  Cache: " + cacheDisplayName);
        infoLabel.setForeground(container.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
        GridData infoData = new GridData(GridData.FILL_HORIZONTAL);
        infoData.horizontalSpan = 4;
        infoLabel.setLayoutData(infoData);
        
                // Quick Select
                Label label = new Label(container, SWT.NONE);
                label.setText("Quick Select:");
        new Label(container_1, SWT.NONE);
        new Label(container_1, SWT.NONE);
        new Label(container_1, SWT.NONE);
        
                presetsCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
                GridData gd_presetsCombo = new GridData(GridData.FILL_HORIZONTAL);
                gd_presetsCombo.horizontalSpan = 4;
                presetsCombo.setLayoutData(gd_presetsCombo);
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
                            parsedBatchDependencies.clear();
                        }
                    }
                });
        for (String[] lib : POPULAR_LIBRARIES) {
            presetsCombo.add(lib[0]);
        }

        // Paste Button (single dependency)
        Button pasteBtn = new Button(container, SWT.PUSH);
        pasteBtn.setText("📋 Paste from Clipboard (single)");
        GridData pasteData = new GridData(GridData.FILL_HORIZONTAL);
        pasteData.horizontalSpan = 4;
        pasteBtn.setLayoutData(pasteData);
        pasteBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                pasteFromClipboard();
                parsedBatchDependencies.clear();
            }
        });

        createSeparator(container);

        // GroupId
        new Label(container, SWT.NONE).setText("Group ID:");
        new Label(container_1, SWT.NONE);
        groupIdText = new Text(container, SWT.BORDER);
        GridData gidData = new GridData(GridData.FILL_HORIZONTAL);
        gidData.horizontalSpan = 2;
        groupIdText.setLayoutData(gidData);
        groupIdText.setMessage("e.g., org.bytedeco");

        // ArtifactId
        new Label(container, SWT.NONE).setText("Artifact ID:");
        new Label(container_1, SWT.NONE);
        artifactIdText = new Text(container, SWT.BORDER);
        GridData aidData = new GridData(GridData.FILL_HORIZONTAL);
        aidData.horizontalSpan = 2;
        artifactIdText.setLayoutData(aidData);
        artifactIdText.setMessage("e.g., javacv-platform");

        // Version
        new Label(container, SWT.NONE).setText("Version:");
        new Label(container_1, SWT.NONE);
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
    }

    private void createOptionsSection(Composite container) {
        downloadAllModulesCheckbox = new Button(container, SWT.CHECK);
        downloadAllModulesCheckbox.setText("Download all modules from group");
        GridData allData = new GridData();
        allData.horizontalSpan = 4;
        downloadAllModulesCheckbox.setLayoutData(allData);

        downloadDepsCheckbox = new Button(container, SWT.CHECK);
        downloadDepsCheckbox.setText("Download dependencies (compile/runtime)");
        downloadDepsCheckbox.setSelection(true);
        GridData depsData = new GridData();
        depsData.horizontalSpan = 4;
        downloadDepsCheckbox.setLayoutData(depsData);

        downloadNativesCheckbox = new Button(container, SWT.CHECK);
        downloadNativesCheckbox.setText("Download native JARs for " + platformClassifier);
        downloadNativesCheckbox.setSelection(true);
        GridData natData = new GridData();
        natData.horizontalSpan = 4;
        downloadNativesCheckbox.setLayoutData(natData);
    }

    private void createPreviewSection(Composite container) {
        Button previewBtn = new Button(container, SWT.PUSH);
        previewBtn.setText("🔎 Preview");
        GridData pbData = new GridData(GridData.FILL_HORIZONTAL);
        pbData.horizontalSpan = 4;
        previewBtn.setLayoutData(pbData);
        previewBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                previewDownload();
            }
        });

        previewList = new List(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData plData = new GridData(GridData.FILL_BOTH);
        plData.horizontalSpan = 4;
        plData.heightHint = 100;
        previewList.setLayoutData(plData);
    }

    private void createStatusSection(Composite container) {
        statusLabel = new Label(container, SWT.WRAP);
        statusLabel.setText("Enter coordinates, select a library, or paste dependencies above.");
        statusLabel.setForeground(container.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
        GridData stData = new GridData(GridData.FILL_HORIZONTAL);
        stData.horizontalSpan = 4;
        statusLabel.setLayoutData(stData);
    }

    private void createSeparator(Composite container) {
        Label sep = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
        GridData sepData = new GridData(GridData.FILL_HORIZONTAL);
        sepData.horizontalSpan = 4;
        sep.setLayoutData(sepData);
    }

    // =========================================================================
    // PARSING METHODS
    // =========================================================================

    /**
     * Parse Maven XML with property resolution.
     * Supports both properties section and dependency blocks.
     */
    public static java.util.List<SimpleDep> parseMavenXmlDependencies(String xml) {
        java.util.List<SimpleDep> result = new java.util.ArrayList<>();

        // First, extract properties from the pasted XML (if any)
        java.util.Map<String, String> properties = new java.util.HashMap<>();

        Pattern propsPattern = Pattern.compile("<properties>([\\s\\S]*?)</properties>", Pattern.CASE_INSENSITIVE);
        Matcher propsMatcher = propsPattern.matcher(xml);
        if (propsMatcher.find()) {
            String propsSection = propsMatcher.group(1);
            Pattern propPattern = Pattern.compile("<([a-zA-Z0-9._-]+)>([^<]*)</\\1>");
            Matcher propMatcher = propPattern.matcher(propsSection);
            while (propMatcher.find()) {
                String propName = propMatcher.group(1);
                String propValue = propMatcher.group(2).trim();
                properties.put(propName, propValue);
                System.out.println("[Maven Parse] Found property: " + propName + " = " + propValue);
            }
        }

        // Now parse dependencies
        Pattern depPattern = Pattern.compile(
                "<dependency>\\s*"
                + "(?s:.*?)<groupId>\\s*([^<]+)\\s*</groupId>"
                + "(?s:.*?)<artifactId>\\s*([^<]+)\\s*</artifactId>"
                + "(?s:.*?)<version>\\s*([^<]+)\\s*</version>"
                + "(?s:.*?)</dependency>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = depPattern.matcher(xml);

        while (matcher.find()) {
            String gId = matcher.group(1).trim();
            String aId = matcher.group(2).trim();
            String ver = matcher.group(3).trim();

            // Resolve property references
            gId = resolveProperties(gId, properties);
            aId = resolveProperties(aId, properties);
            ver = resolveProperties(ver, properties);

            // Skip if version still contains unresolved properties
            if (ver.contains("${")) {
                System.out.println("[Maven Parse] Skipping unresolved: " + gId + ":" + aId + ":" + ver);
                continue;
            }

            result.add(new SimpleDep(gId, aId, ver));
        }

        return result;
    }

    /**
     * Resolve property placeholders in a string.
     */
    public static String resolveProperties(String value, java.util.Map<String, String> properties) {
        if (value == null) return null;

        String result = value;
        int maxIterations = 10;
        int iteration = 0;

        while (result.contains("${") && iteration < maxIterations) {
            String before = result;

            Pattern propPattern = Pattern.compile("\\$\\{([^}]+)\\}");
            Matcher propMatcher = propPattern.matcher(result);
            StringBuffer sb = new StringBuffer();

            while (propMatcher.find()) {
                String propName = propMatcher.group(1);
                String propValue = properties.get(propName);
                if (propValue != null) {
                    propMatcher.appendReplacement(sb, Matcher.quoteReplacement(propValue));
                } else {
                    propMatcher.appendReplacement(sb, Matcher.quoteReplacement(propMatcher.group(0)));
                }
            }
            propMatcher.appendTail(sb);
            result = sb.toString();

            if (result.equals(before)) break;
            iteration++;
        }

        return result;
    }

    // =========================================================================
    // UI ACTION METHODS
    // =========================================================================

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
                        Matcher vMatcher = versionPattern.matcher(content.toString());

                        while (vMatcher.find()) {
                            String v = vMatcher.group(1);
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
        // If batch mode, show batch preview
        if (!parsedBatchDependencies.isEmpty()) {
            previewList.removeAll();
            for (SimpleDep dep : parsedBatchDependencies) {
                previewList.add(dep.toString());
            }
            setStatus("Batch preview: " + parsedBatchDependencies.size() + " dependencies.", false);
            return;
        }

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

        // Read checkbox values BEFORE starting the Job (must be on UI thread)
        final boolean downloadAll = downloadAllModulesCheckbox.getSelection();
        final boolean downloadDeps = downloadDepsCheckbox.getSelection();
        final boolean downloadNat = downloadNativesCheckbox.getSelection();

        Job previewJob = new Job("Preview") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                java.util.List<String> artifacts = new ArrayList<>();

                if (downloadAll) {
                    java.util.List<String[]> found = searchArtifactsWithVersion(gId, ver);
                    for (String[] art : found) {
                        artifacts.add(art[0] + ":" + art[1] + ":" + art[2]);
                    }
                } else {
                    artifacts.add(gId + ":" + aId + ":" + ver);
                }

                Display.getDefault().asyncExec(() -> {
                    if (artifacts.isEmpty()) {
                        previewList.add("(No artifacts found)");
                        setStatus("No artifacts found.", true);
                    } else {
                        for (String art : artifacts) {
                            previewList.add(art);
                        }
                        setStatus("Preview: " + artifacts.size() + " artifact(s)"
                                + (downloadDeps ? " + dependencies" : "")
                                + (downloadNat ? " + natives" : ""), false);
                    }
                });

                return Status.OK_STATUS;
            }
        };
        previewJob.setSystem(true);
        previewJob.schedule();
    }

    /**
     * Search for artifacts with a specific version in a group.
     */
    public static java.util.List<String[]> searchArtifactsWithVersion(String groupId, String version) {
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

    private void setStatus(String message, boolean isError) {
        if (statusLabel == null || statusLabel.isDisposed()) return;
        statusLabel.setForeground(
                statusLabel.getDisplay().getSystemColor(isError ? SWT.COLOR_RED : SWT.COLOR_DARK_GRAY));
        statusLabel.setText(message);
    }

    // =========================================================================
    // DIALOG BUTTONS AND RESULT
    // =========================================================================

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

        // If batch mode was used, allow empty single fields
        if (!parsedBatchDependencies.isEmpty()) {
            super.okPressed();
            return;
        }

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

    // =========================================================================
    // GETTERS FOR RESULT VALUES
    // =========================================================================

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

    public java.util.List<SimpleDep> getBatchDependencies() {
        return new java.util.ArrayList<>(parsedBatchDependencies);
    }
}