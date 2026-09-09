package com.eco.bio7.javapreferences;

import java.io.BufferedReader;
import java.io.InputStream;
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
 * Dialog for downloading Maven artifacts from Maven Central. Supports single
 * artifact download, batch paste with property resolution, and downloading all
 * modules from a group.
 */
public class MavenDownloadDialog extends Dialog {

	private static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";

	/**
	 * Some artifacts (e.g. {@code net.imglib2:imglib2-algorithm}) were never
	 * published to Maven Central and only exist on the SciJava/Fiji Nexus
	 * repository. Trying it is opt-in via {@link #sciJavaCheckbox}.
	 */
	private static final String SCIJAVA_URL = "https://maven.scijava.org/content/groups/public";

	// UI components
	private Text groupIdText;
	private Text artifactIdText;
	private Combo versionCombo;
	private Button fetchVersionsBtn;
	private Button downloadDepsCheckbox;
	private Button downloadAllModulesCheckbox;
	private Button downloadNativesCheckbox;
	private Button mavenCentralCheckbox;
	private Button sciJavaCheckbox;
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

		/**
		 * @return {@code true} if this dependency has a concrete, resolved version.
		 */
		public boolean hasVersion() {
			return version != null && !version.isEmpty() && !version.contains("${");
		}

		@Override
		public String toString() {
			String v = (version == null || version.isEmpty()) ? "(version required)" : version;
			return groupId + ":" + artifactId + ":" + v;
		}
	}

	// Popular libraries presets
	private static final String[][] POPULAR_LIBRARIES = { { "-- Select a popular library --", "", "", "" },

			{ "JavaCV Platform", "org.bytedeco", "javacv-platform", "1.5.11" },
			{ "JavaCV (latest)", "org.bytedeco", "javacv-platform", "1.5.13" },
			{ "Pi4J Core", "com.pi4j", "pi4j-core", "2.6.0" }, { "JFreeChart", "org.jfree", "jfreechart", "1.5.4" },
			{ "SQLite JDBC", "org.xerial", "sqlite-jdbc", "3.45.3.0" },
			{ "MySQL Connector", "com.mysql", "mysql-connector-j", "8.4.0" },
			{ "PostgreSQL JDBC", "org.postgresql", "postgresql", "42.7.3" },
			{ "H2 Database", "com.h2database", "h2", "2.2.224" },
			{ "Deeplearning4j Core", "org.deeplearning4j", "deeplearning4j-core", "1.0.0-M2.1" },
			{ "ND4J Native Platform", "org.nd4j", "nd4j-native-platform", "1.0.0-M2.1" }, };

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

		parseButton.addListener(SWT.Selection, e -> parseAndResolveBatch());
	}

	/**
	 * Parses the pasted Maven XML and, for any dependency whose version is not
	 * inline, resolves the version from the pasted POM's parent / BOM chain
	 * (falling back to the latest release on Maven Central). All network access
	 * runs in a background {@link Job} so the UI stays responsive.
	 */
	private void parseAndResolveBatch() {
		final String xml = mavenDependenciesText.getText();

		// Local parse only (no network) so we can react instantly.
		final java.util.List<SimpleDep> parsed = parseMavenXmlDependencies(xml);
		if (parsed.isEmpty()) {
			setStatus("No valid <dependency> blocks found (check groupId/artifactId).", true);
			return;
		}

		previewList.removeAll();
		for (SimpleDep dep : parsed) {
			previewList.add(dep.toString());
		}

		long missing = parsed.stream().filter(d -> !d.hasVersion()).count();
		if (missing == 0) {
			parsedBatchDependencies.clear();
			parsedBatchDependencies.addAll(parsed);
			setStatus("Parsed " + parsed.size() + " dependencies. Click 'Download' to start.", false);
			return;
		}

		setStatus("Parsed " + parsed.size() + " dependencies. Resolving " + missing
				+ " missing version(s) from parent POM / Maven Central...", false);
		parseButton.setEnabled(false);

		final String[] repos = getSelectedRepositories();

		Job resolveJob = new Job("Resolving Maven versions") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final java.util.List<SimpleDep> resolved = resolveDependencyVersions(xml, parsed, repos);
				long unresolved = resolved.stream().filter(d -> !d.hasVersion()).count();

				Display.getDefault().asyncExec(() -> {
					if (parseButton != null && !parseButton.isDisposed()) {
						parseButton.setEnabled(true);
					}

					parsedBatchDependencies.clear();
					parsedBatchDependencies.addAll(resolved);

					previewList.removeAll();
					for (SimpleDep dep : resolved) {
						previewList.add(dep.toString());
					}

					if (unresolved > 0) {
						setStatus("Parsed " + resolved.size() + " dependencies, but " + unresolved
								+ " version(s) could not be resolved (shown as '(version required)'). "
								+ "See the console log for details, or edit them manually.", true);
					} else {
						setStatus(
								"Parsed and resolved " + resolved.size() + " dependencies. Click 'Download' to start.",
								false);
					}
				});

				return Status.OK_STATUS;
			}
		};
		resolveJob.setSystem(true);
		resolveJob.schedule();
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

		Label repoLabel = new Label(container, SWT.NONE);
		repoLabel.setText("Repositories:");
		GridData repoLabelGd = new GridData();
		repoLabelGd.horizontalSpan = 4;
		repoLabel.setLayoutData(repoLabelGd);

		mavenCentralCheckbox = new Button(container, SWT.CHECK);
		mavenCentralCheckbox.setText("Maven Central");
		mavenCentralCheckbox.setSelection(true);
		GridData mcData = new GridData();
		mcData.horizontalSpan = 4;
		mavenCentralCheckbox.setLayoutData(mcData);

		sciJavaCheckbox = new Button(container, SWT.CHECK);
		sciJavaCheckbox.setText("SciJava Repository (for artifacts not on Maven Central, e.g. imglib2-algorithm)");
		GridData sjData = new GridData();
		sjData.horizontalSpan = 4;
		sciJavaCheckbox.setLayoutData(sjData);

		SelectionAdapter requireAtLeastOneRepo = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!mavenCentralCheckbox.getSelection() && !sciJavaCheckbox.getSelection()) {
					((Button) e.widget).setSelection(true);
					setStatus("At least one repository must be selected.", true);
				}
			}
		};
		mavenCentralCheckbox.addSelectionListener(requireAtLeastOneRepo);
		sciJavaCheckbox.addSelectionListener(requireAtLeastOneRepo);
	}

	/**
	 * @return the repository base URLs the user selected, in priority order
	 *         (Maven Central first when both are checked). Never empty.
	 */
	public String[] getSelectedRepositories() {
		java.util.List<String> repos = new java.util.ArrayList<>();
		if (mavenCentralCheckbox == null || mavenCentralCheckbox.getSelection()) {
			repos.add(MAVEN_CENTRAL_URL);
		}
		if (sciJavaCheckbox != null && sciJavaCheckbox.getSelection()) {
			repos.add(SCIJAVA_URL);
		}
		if (repos.isEmpty()) {
			repos.add(MAVEN_CENTRAL_URL);
		}
		return repos.toArray(new String[0]);
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
	 * <p>
	 * Each {@code <dependency>} block is parsed independently. Only
	 * {@code <groupId>} and {@code <artifactId>} are mandatory; the
	 * {@code <version>} is optional so that POMs which inherit versions from a
	 * parent/BOM (e.g. {@code pom-scijava}) are still detected. Dependencies whose
	 * version is missing or is an unresolved {@code ${...}} property are returned
	 * with an empty version string. Use
	 * {@link #resolveDependencyVersions(String, java.util.List)} to fill those in
	 * from the parent POM chain / Maven Central.
	 * <p>
	 * Only the pasted POM's own {@code <properties>} are applied here (no network
	 * access), so this method is safe to call on the UI thread.
	 * {@code <dependencyManagement>} blocks are ignored so that BOM imports are not
	 * mistaken for real dependencies.
	 */
	public static java.util.List<SimpleDep> parseMavenXmlDependencies(String xml) {
		java.util.List<SimpleDep> result = new java.util.ArrayList<>();
		if (xml == null || xml.trim().isEmpty()) {
			return result;
		}

		// Extract the local <properties> (if any) for immediate resolution.
		java.util.Map<String, String> properties = extractProperties(xml);

		// Ignore <dependencyManagement> so managed entries are not treated as
		// real dependencies; only parse the "main" <dependencies> section(s).
		String scanXml = stripDependencyManagement(xml);

		// Grab each <dependency>...</dependency> block on its own so a missing
		// <version> in one block cannot make the whole match fail. The opening
		// tag may carry attributes/namespace prefixes.
		Pattern blockPattern = Pattern.compile("<dependency\\b[^>]*>([\\s\\S]*?)</dependency>",
				Pattern.CASE_INSENSITIVE);
		Matcher blockMatcher = blockPattern.matcher(scanXml);

		Pattern gidP = Pattern.compile("<groupId>\\s*([^<]+?)\\s*</groupId>", Pattern.CASE_INSENSITIVE);
		Pattern aidP = Pattern.compile("<artifactId>\\s*([^<]+?)\\s*</artifactId>", Pattern.CASE_INSENSITIVE);
		Pattern verP = Pattern.compile("<version>\\s*([^<]+?)\\s*</version>", Pattern.CASE_INSENSITIVE);
		Pattern scopeP = Pattern.compile("<scope>\\s*([^<]+?)\\s*</scope>", Pattern.CASE_INSENSITIVE);

		while (blockMatcher.find()) {
			String block = blockMatcher.group(1);

			Matcher gm = gidP.matcher(block);
			Matcher am = aidP.matcher(block);
			Matcher vm = verP.matcher(block);
			Matcher sm = scopeP.matcher(block);

			if (!gm.find() || !am.find()) {
				continue; // groupId + artifactId are mandatory
			}

			// Skip non-runtime scopes (test/provided/system) and BOM imports.
			if (sm.find()) {
				String scope = sm.group(1).trim().toLowerCase();
				if (scope.equals("test") || scope.equals("provided") || scope.equals("system")
						|| scope.equals("import")) {
					continue;
				}
			}

			String gId = resolveProperties(gm.group(1).trim(), properties);
			String aId = resolveProperties(am.group(1).trim(), properties);

			String ver = "";
			if (vm.find()) {
				ver = resolveProperties(vm.group(1).trim(), properties);
				if (ver != null && ver.contains("${")) {
					ver = ""; // still unresolved -> resolve later
				}
			}

			if (gId != null && gId.contains("${")) {
				continue; // unresolved groupId -> nothing sensible to download
			}
			if (aId != null && aId.contains("${")) {
				continue; // unresolved artifactId -> skip
			}

			if ((ver == null || ver.isEmpty())) {
				System.out.println("[Maven Parse] No inline version for " + gId + ":" + aId
						+ " -> will resolve from parent POM / Maven Central");
			}

			result.add(new SimpleDep(gId, aId, ver == null ? "" : ver));
		}

		return result;
	}

	/**
	 * Fills in missing versions for the given dependencies using, in order:
	 * <ol>
	 * <li>the pasted POM's own {@code <properties>};</li>
	 * <li>the parent POM chain declared via {@code <parent>} in the pasted XML —
	 * walking every ancestor's {@code <properties>} and
	 * {@code <dependencyManagement>} (including {@code ${...}} placeholders and
	 * imported BOMs) exactly like Maven would. This yields the byte-for-byte
	 * versions a BOM such as {@code pom-scijava} pins;</li>
	 * <li>as a last resort, the latest release on Maven Central (via
	 * {@code maven-metadata.xml}).</li>
	 * </ol>
	 * Dependencies that already have a version are returned unchanged.
	 * <p>
	 * This performs network calls and MUST NOT be invoked on the UI thread.
	 *
	 * @param xml   the originally pasted POM XML (used to find {@code <parent>})
	 * @param deps  the dependencies parsed from {@code xml}
	 * @param repos repository base URLs to try, in order (see
	 *              {@link #getSelectedRepositories()})
	 */
	public static java.util.List<SimpleDep> resolveDependencyVersions(String xml, java.util.List<SimpleDep> deps,
			String[] repos) {
		java.util.List<SimpleDep> resolved = new java.util.ArrayList<>();

		// 1. Build ONE fully-merged property map: ancestors first, local last (local
		// wins).
		java.util.Map<String, String> properties = new java.util.HashMap<>();
		// Managed versions are collected RAW (may still contain ${...}) and are
		// only resolved once the full property map is assembled below.
		java.util.Map<String, String> managedRaw = new java.util.HashMap<>();

		String parentGroupId = extractXmlValueFromParent(xml, "groupId");
		String parentArtifactId = extractXmlValueFromParent(xml, "artifactId");
		String parentVersion = extractXmlValueFromParent(xml, "version");

		if (parentGroupId != null && parentArtifactId != null && parentVersion != null) {
			System.out.println("[Maven Parse] Pasted POM declares parent: " + parentGroupId + ":" + parentArtifactId
					+ ":" + parentVersion);
			properties.putAll(fetchPomPropertiesRecursively(parentGroupId, parentArtifactId, parentVersion, 0,
					new java.util.HashMap<>(), repos));
			managedRaw.putAll(fetchManagedDependencyVersionsRecursively(parentGroupId, parentArtifactId,
					parentVersion, 0, repos));
			System.out.println("[Maven Parse] Collected " + properties.size() + " properties and " + managedRaw.size()
					+ " managed-dependency entries from the parent chain.");
		} else {
			System.out.println("[Maven Parse] Pasted POM declares no <parent>; "
					+ "missing versions will use Maven Central latest.");
		}

		// BOM imports declared directly in the pasted XML's own
		// <dependencyManagement> (e.g. <scope>import</scope><type>pom</type>,
		// as with pom-scijava) must be followed too -- not just those inherited
		// via a <parent>. A bare <dependencyManagement>+<dependencies> fragment
		// with no enclosing <parent> relies entirely on this to pin versions.
		for (String[] bom : extractDirectBomImports(xml)) {
			System.out.println("[Maven Parse] Pasted POM imports BOM: " + bom[0] + ":" + bom[1] + ":" + bom[2]);
			properties.putAll(
					fetchPomPropertiesRecursively(bom[0], bom[1], bom[2], 0, new java.util.HashMap<>(), repos));
			managedRaw.putAll(fetchManagedDependencyVersionsRecursively(bom[0], bom[1], bom[2], 0, repos));
		}

		// Local <properties> and local <dependencyManagement> override the parent
		// chain.
		properties.putAll(extractProperties(xml));
		managedRaw.putAll(extractManagedDependencyVersions(xml));

		// 2. Now that ALL properties are known, resolve every managed version.
		java.util.Map<String, String> managedVersions = new java.util.HashMap<>();
		for (java.util.Map.Entry<String, String> e : managedRaw.entrySet()) {
			String v = resolveProperties(e.getValue(), properties);
			if (v != null && !v.contains("${")) {
				managedVersions.put(e.getKey(), v);
			} else {
				System.out.println("[Maven Parse] Managed entry " + e.getKey() + " has unresolved version '"
						+ e.getValue() + "' -> " + v);
			}
		}

		// Cache latest-version lookups so duplicates only hit the network once.
		java.util.Map<String, String> latestCache = new java.util.HashMap<>();

		for (SimpleDep dep : deps) {
			if (dep.hasVersion()) {
				resolved.add(dep);
				continue;
			}

			String key = dep.groupId + ":" + dep.artifactId;
			String ver = managedVersions.get(key);

			if (ver != null) {
				System.out.println("[Maven Parse] Managed version for " + key + ": " + ver);
			} else {
				// Fall back to the latest release on Maven Central.
				if (!latestCache.containsKey(key)) {
					latestCache.put(key, lookupLatestVersion(dep.groupId, dep.artifactId, repos));
				}
				String latest = latestCache.get(key);
				if (latest != null && !latest.isEmpty()) {
					ver = latest;
					System.out.println("[Maven Parse] Latest version for " + key + ": " + ver + " (BOM miss)");
				}
			}

			if (ver != null && !ver.isEmpty()) {
				resolved.add(new SimpleDep(dep.groupId, dep.artifactId, ver));
			} else {
				System.out.println("[Maven Parse] Could not resolve version for " + key);
				resolved.add(dep); // keep with empty version so the UI can flag it
			}
		}

		return resolved;
	}

	// -------------------------------------------------------------------------
	// Parent-POM / dependencyManagement walking (self-contained and static so it
	// can also resolve a *pasted* POM's ancestry).
	// -------------------------------------------------------------------------

	/**
	 * Recursively fetches and merges {@code <properties>} from the given POM and
	 * all of its ancestors. Ancestors are applied first so that closer POMs win.
	 */
	private static java.util.Map<String, String> fetchPomPropertiesRecursively(String groupId, String artifactId,
			String version, int depth, java.util.Map<String, java.util.Map<String, String>> cache, String[] repos) {

		String cacheKey = groupId + ":" + artifactId + ":" + version;
		if (cache.containsKey(cacheKey)) {
			return new java.util.HashMap<>(cache.get(cacheKey));
		}

		java.util.Map<String, String> properties = new java.util.HashMap<>();
		if (depth > 15) {
			return properties;
		}

		String pom = fetchPom(groupId, artifactId, version, repos);
		if (pom == null) {
			return properties;
		}

		String pGroupId = extractXmlValueFromParent(pom, "groupId");
		String pArtifactId = extractXmlValueFromParent(pom, "artifactId");
		String pVersion = extractXmlValueFromParent(pom, "version");

		if (pGroupId != null && pArtifactId != null && pVersion != null) {
			properties.putAll(
					fetchPomPropertiesRecursively(pGroupId, pArtifactId, pVersion, depth + 1, cache, repos));
		}

		properties.put("project.version", version);
		properties.put("project.groupId", groupId);
		properties.put("project.artifactId", artifactId);
		if (pVersion != null) {
			properties.put("project.parent.version", pVersion);
		}

		mergePropertiesFromPom(pom, properties);

		cache.put(cacheKey, new java.util.HashMap<>(properties));
		return properties;
	}

	/**
	 * Recursively collects RAW {@code <dependencyManagement>} versions (which may
	 * still contain {@code ${...}} placeholders) from the given POM and all of its
	 * ancestors and imported BOMs. Closer POMs win. The caller resolves the
	 * placeholders once the full property map is assembled.
	 */
	private static java.util.Map<String, String> fetchManagedDependencyVersionsRecursively(String groupId,
			String artifactId, String version, int depth, String[] repos) {

		java.util.Map<String, String> managedVersions = new java.util.HashMap<>();
		if (depth > 15) {
			return managedVersions;
		}

		String pom = fetchPom(groupId, artifactId, version, repos);
		if (pom == null) {
			return managedVersions;
		}

		String pGroupId = extractXmlValueFromParent(pom, "groupId");
		String pArtifactId = extractXmlValueFromParent(pom, "artifactId");
		String pVersion = extractXmlValueFromParent(pom, "version");

		if (pGroupId != null && pArtifactId != null && pVersion != null) {
			managedVersions.putAll(
					fetchManagedDependencyVersionsRecursively(pGroupId, pArtifactId, pVersion, depth + 1, repos));
		}

		// Imported BOMs (scope=import / type=pom) must be followed too.
		managedVersions.putAll(fetchImportedBomVersions(pom, depth, repos));

		managedVersions.putAll(extractManagedDependencyVersions(pom));
		return managedVersions;
	}

	/**
	 * Follows {@code <dependencyManagement>} entries with
	 * {@code <scope>import</scope>} (BOM imports) and merges their managed
	 * versions.
	 */
	private static java.util.Map<String, String> fetchImportedBomVersions(String pom, int depth, String[] repos) {
		java.util.Map<String, String> imported = new java.util.HashMap<>();
		if (depth > 15 || pom == null) {
			return imported;
		}

		for (String[] bom : extractDirectBomImports(pom)) {
			System.out.println("[Maven POM] Importing BOM: " + bom[0] + ":" + bom[1] + ":" + bom[2]);
			imported.putAll(fetchManagedDependencyVersionsRecursively(bom[0], bom[1], bom[2], depth + 1, repos));
		}
		return imported;
	}

	/**
	 * Finds BOM coordinates ({@code <scope>import</scope>} /
	 * {@code <type>pom</type>}) declared directly in the given POM/fragment's own
	 * top-level {@code <dependencyManagement>} -- as opposed to one inherited via a
	 * {@code <parent>}. This is what lets a bare
	 * {@code <dependencyManagement>+<dependencies>} fragment (no {@code <parent>})
	 * pin versions via a BOM such as {@code pom-scijava}.
	 * <p>
	 * BOM coordinates rarely use properties; if they do we cannot resolve them yet
	 * (properties not assembled at this point), so unresolved ones are skipped.
	 */
	private static java.util.List<String[]> extractDirectBomImports(String pom) {
		java.util.List<String[]> result = new java.util.ArrayList<>();
		String dmSection = extractSection(pom, "dependencyManagement");
		if (dmSection == null) {
			return result;
		}

		for (String dep : extractDependencyBlocks(dmSection)) {
			String scope = extractXmlValue(dep, "scope");
			String type = extractXmlValue(dep, "type");
			if (!"import".equalsIgnoreCase(scope) && !"pom".equalsIgnoreCase(type)) {
				continue;
			}

			String bomGroupId = extractXmlValue(dep, "groupId");
			String bomArtifactId = extractXmlValue(dep, "artifactId");
			String bomVersion = extractXmlValue(dep, "version");

			if (bomGroupId != null && bomArtifactId != null && bomVersion != null && !bomVersion.contains("${")) {
				result.add(new String[] { bomGroupId, bomArtifactId, bomVersion });
			}
		}
		return result;
	}

	/**
	 * Downloads a POM, trying each of {@code repos} in order; returns its text or
	 * {@code null} if none of them have it. Follows HTTP redirects (including
	 * http-&gt;https) and logs the outcome.
	 */
	private static String fetchPom(String groupId, String artifactId, String version, String[] repos) {
		String groupPath = groupId.replace('.', '/');
		String relativePath = "/" + groupPath + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version
				+ ".pom";

		for (String repo : repos) {
			String pomUrl = repo + relativePath;
			try {
				String body = httpGet(pomUrl, 0);
				if (body == null) {
					continue;
				}
				System.out.println("[Maven POM] Fetched " + groupId + ":" + artifactId + ":" + version + " from " + repo
						+ " (" + body.length() + " chars)");
				return body;
			} catch (Exception e) {
				System.err.println("[Maven POM] Error fetching POM " + pomUrl + ": " + e.getMessage());
			}
		}
		System.err.println("[Maven POM] Failed to fetch parent/BOM POM " + groupId + ":" + artifactId + ":" + version
				+ " from any repository: " + relativePath);
		return null;
	}

	/**
	 * Performs an HTTP GET and returns the body text, following up to a handful of
	 * redirects manually (HttpURLConnection does not auto-follow across protocols
	 * such as http-&gt;https).
	 */
	private static String httpGet(String urlStr, int redirectDepth) throws Exception {
		if (redirectDepth > 5) {
			return null;
		}

		HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
		conn.setRequestMethod("GET");
		conn.setConnectTimeout(15000);
		conn.setReadTimeout(30000);
		conn.setInstanceFollowRedirects(true);
		conn.setRequestProperty("User-Agent", "Bio7/1.0");

		int code = conn.getResponseCode();

		if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM
				|| code == HttpURLConnection.HTTP_SEE_OTHER || code == 307 || code == 308) {
			String location = conn.getHeaderField("Location");
			conn.disconnect();
			if (location == null || location.isEmpty()) {
				return null;
			}
			return httpGet(location, redirectDepth + 1);
		}

		if (code != 200) {
			conn.disconnect();
			return null;
		}

		StringBuilder content = new StringBuilder();
		try (InputStream in = conn.getInputStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String line;
			while ((line = reader.readLine()) != null) {
				content.append(line).append("\n");
			}
		} finally {
			conn.disconnect();
		}
		return content.toString();
	}

	/** Extracts the top-level {@code <properties>} from a POM into a fresh map. */
	private static java.util.Map<String, String> extractProperties(String xml) {
		java.util.Map<String, String> properties = new java.util.HashMap<>();
		mergePropertiesFromPom(xml, properties);
		return properties;
	}

	/**
	 * Merges the project-level {@code <properties>} of the given POM into
	 * {@code properties}.
	 * <p>
	 * NOTE: a POM may contain several {@code <properties>} elements — most notably
	 * tiny ones inside {@code <contributor>}/{@code <developer>} entries (e.g.
	 * {@code <properties><id>hinerm</id></properties>}). We must ignore those and
	 * use the real project-level block. Since the project properties block is by
	 * far the largest, we pick the longest {@code <properties>} section found in
	 * the document.
	 */
	private static void mergePropertiesFromPom(String xml, java.util.Map<String, String> properties) {
		if (xml == null) {
			return;
		}

		Matcher propsMatcher = Pattern
				.compile("<properties\\b[^>]*>([\\s\\S]*?)</properties>", Pattern.CASE_INSENSITIVE).matcher(xml);

		String bestSection = null;
		while (propsMatcher.find()) {
			String section = propsMatcher.group(1);
			if (bestSection == null || section.length() > bestSection.length()) {
				bestSection = section;
			}
		}

		if (bestSection == null) {
			return;
		}

		// Each simple property is <name>value</name>. Values here are plain text
		// (no nested elements) which covers all version properties in practice.
		Pattern propPattern = Pattern.compile("<([a-zA-Z0-9._\\-]+)>\\s*([^<]*?)\\s*</\\1>");
		Matcher propMatcher = propPattern.matcher(bestSection);
		int count = 0;
		while (propMatcher.find()) {
			String propName = propMatcher.group(1);
			String propValue = resolveProperties(propMatcher.group(2).trim(), properties);
			properties.put(propName, propValue);
			count++;
		}
		System.out.println("[Maven POM] Merged " + count + " properties (block length " + bestSection.length() + ").");
	}

	/**
	 * Extracts RAW {@code groupId:artifactId -> version} entries from the
	 * {@code <dependencyManagement>} section of the given POM. Versions are
	 * returned as-is (may contain {@code ${...}} placeholders); the caller is
	 * responsible for resolving them.
	 */
	private static java.util.Map<String, String> extractManagedDependencyVersions(String pom) {
		java.util.Map<String, String> managedVersions = new java.util.HashMap<>();
		if (pom == null) {
			return managedVersions;
		}

		String dmSection = extractSection(pom, "dependencyManagement");
		if (dmSection == null) {
			return managedVersions;
		}

		for (String dep : extractDependencyBlocks(dmSection)) {
			String depGroupId = extractXmlValue(dep, "groupId");
			String depArtifactId = extractXmlValue(dep, "artifactId");
			String depVersion = extractXmlValue(dep, "version"); // may be ${...}

			if (depGroupId != null && depArtifactId != null && depVersion != null && !depVersion.isEmpty()) {
				managedVersions.put(depGroupId + ":" + depArtifactId, depVersion);
			}
		}

		return managedVersions;
	}

	/**
	 * Returns the inner text of the first {@code <tag>...</tag>} section (tag may
	 * carry attributes), or {@code null} if absent.
	 */
	private static String extractSection(String xml, String tag) {
		if (xml == null) {
			return null;
		}
		Matcher m = Pattern.compile("<" + tag + "\\b[^>]*>([\\s\\S]*?)</" + tag + ">", Pattern.CASE_INSENSITIVE)
				.matcher(xml);
		return m.find() ? m.group(1) : null;
	}

	/**
	 * Splits a section of XML into the inner text of each
	 * {@code <dependency>...</dependency>} block (opening tag may carry
	 * attributes).
	 */
	private static java.util.List<String> extractDependencyBlocks(String xml) {
		java.util.List<String> blocks = new java.util.ArrayList<>();
		if (xml == null) {
			return blocks;
		}
		Matcher m = Pattern.compile("<dependency\\b[^>]*>([\\s\\S]*?)</dependency>", Pattern.CASE_INSENSITIVE)
				.matcher(xml);
		while (m.find()) {
			blocks.add(m.group(1));
		}
		return blocks;
	}

	/** Removes the {@code <dependencyManagement>} block from a POM string. */
	private static String stripDependencyManagement(String pom) {
		if (pom == null) {
			return "";
		}
		return Pattern
				.compile("<dependencyManagement\\b[^>]*>[\\s\\S]*?</dependencyManagement>", Pattern.CASE_INSENSITIVE)
				.matcher(pom).replaceAll("");
	}

	private static String extractXmlValueFromParent(String xml, String childTag) {
		String parentSection = extractSection(xml, "parent");
		if (parentSection == null) {
			return null;
		}
		return extractXmlValue(parentSection, childTag);
	}

	/**
	 * Returns the trimmed inner text of the first {@code <tagName>...</tagName>}
	 * element (tag may carry attributes), or {@code null} if absent.
	 */
	private static String extractXmlValue(String xml, String tagName) {
		if (xml == null) {
			return null;
		}
		Matcher m = Pattern
				.compile("<" + tagName + "\\b[^>]*>\\s*([^<]*?)\\s*</" + tagName + ">", Pattern.CASE_INSENSITIVE)
				.matcher(xml);
		if (m.find()) {
			String v = m.group(1).trim();
			return v.isEmpty() ? null : v;
		}
		return null;
	}

	/**
	 * Looks up the latest release version of an artifact on Maven Central by
	 * reading {@code maven-metadata.xml} (the legacy {@code search.maven.org}
	 * solrsearch API is deprecated/unreliable, so it is only used as a last-resort
	 * fallback). Returns {@code null} if nothing is found.
	 */
	public static String lookupLatestVersion(String groupId, String artifactId, String[] repos) {
		if (groupId == null || artifactId == null || groupId.isEmpty() || artifactId.isEmpty()) {
			return null;
		}

		// Preferred: maven-metadata.xml, tried against each of repos.
		String groupPath = groupId.replace('.', '/');
		String relativePath = "/" + groupPath + "/" + artifactId + "/maven-metadata.xml";
		for (String repo : repos) {
			try {
				String meta = httpGet(repo + relativePath, 0);
				if (meta == null) {
					continue;
				}
				// Prefer <release>, then <latest>, then the last <version> listed.
				String release = extractXmlValue(meta, "release");
				if (release != null) {
					return release;
				}
				String latest = extractXmlValue(meta, "latest");
				if (latest != null) {
					return latest;
				}
				Matcher ver = Pattern.compile("<version>\\s*([^<]+?)\\s*</version>").matcher(meta);
				String last = null;
				while (ver.find()) {
					last = ver.group(1).trim();
				}
				if (last != null) {
					return last;
				}
			} catch (Exception e) {
				System.err.println("[Maven Parse] maven-metadata lookup failed for " + groupId + ":" + artifactId
						+ " at " + repo + ": " + e.getMessage());
			}
		}

		// Fallback: legacy solrsearch (deprecated, may be flaky).
		try {
			String searchUrl = "https://search.maven.org/solrsearch/select?q=g:" + groupId + "+AND+a:" + artifactId
					+ "&rows=1&wt=json";
			String json = httpGet(searchUrl, 0);
			if (json != null) {
				Matcher latest = Pattern.compile("\"latestVersion\":\"([^\"]+)\"").matcher(json);
				if (latest.find()) {
					return latest.group(1);
				}
				Matcher v = Pattern.compile("\"v\":\"([^\"]+)\"").matcher(json);
				if (v.find()) {
					return v.group(1);
				}
			}
		} catch (Exception e) {
			System.err.println("[Maven Parse] solrsearch fallback failed for " + groupId + ":" + artifactId + ": "
					+ e.getMessage());
		}

		return null;
	}

	/**
	 * Resolve property placeholders in a string.
	 */
	public static String resolveProperties(String value, java.util.Map<String, String> properties) {
		if (value == null)
			return null;

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

			if (result.equals(before))
				break;
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
						try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
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
			long unresolved = 0;
			for (SimpleDep dep : parsedBatchDependencies) {
				previewList.add(dep.toString());
				if (!dep.hasVersion()) {
					unresolved++;
				}
			}
			if (unresolved > 0) {
				setStatus("Batch preview: " + parsedBatchDependencies.size() + " dependencies (" + unresolved
						+ " missing a version).", true);
			} else {
				setStatus("Batch preview: " + parsedBatchDependencies.size() + " dependencies.", false);
			}
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
						setStatus(
								"Preview: " + artifacts.size() + " artifact(s)"
										+ (downloadDeps ? " + dependencies" : "") + (downloadNat ? " + natives" : ""),
								false);
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
		if (statusLabel == null || statusLabel.isDisposed())
			return;
		statusLabel
				.setForeground(statusLabel.getDisplay().getSystemColor(isError ? SWT.COLOR_RED : SWT.COLOR_DARK_GRAY));
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

		// If batch mode was used, allow empty single fields, but make sure every
		// batch dependency actually has a resolved version before downloading.
		if (!parsedBatchDependencies.isEmpty()) {
			java.util.List<String> missing = new java.util.ArrayList<>();
			for (SimpleDep dep : parsedBatchDependencies) {
				if (!dep.hasVersion()) {
					missing.add(dep.groupId + ":" + dep.artifactId);
				}
			}
			if (!missing.isEmpty()) {
				setStatus(missing.size() + " dependency(ies) still have no version (e.g. " + missing.get(0)
						+ "). Re-run 'Parse Dependencies' or remove them.", true);
				return;
			}
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