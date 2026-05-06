package com.eco.bio7.compile;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.jar.*;

/**
 * Loads JAR files from the Bio7 Fragment Libraries preference (FRAGMENT_LIBS)
 * and attaches them as OSGi fragments to the com.eco.bio7 host bundle.
 * 
 * Loading can be enabled/disabled via the FRAGMENT_LOADING_ENABLED preference.
 */
/**
 * Bio7FragmentLoader - Dynamic OSGi Fragment Library Loader
 * 
 * ============================================================================
 * PURPOSE
 * ============================================================================
 * This class enables loading external JAR libraries at runtime by wrapping them
 * as OSGi fragments and attaching them to the com.eco.bio7 host bundle. This
 * makes the classes from these JARs available to all Bio7 plugins that depend
 * on com.eco.bio7.
 * 
 * ============================================================================
 * WHY FRAGMENTS?
 * ============================================================================
 * In OSGi/Eclipse plugin architecture, each bundle has its own classloader and
 * cannot directly access classes from arbitrary JAR files. By creating fragment
 * bundles that attach to a host bundle, the fragment's classpath becomes part
 * of the host's classpath, making the classes accessible.
 * 
 * ============================================================================
 * HOW IT WORKS
 * ============================================================================
 * 1. On Bio7 startup, this loader checks if fragment loading is enabled in
 *    the preferences (FRAGMENT_LOADING_ENABLED preference).
 * 
 * 2. If enabled, it reads the list of JAR paths from the FRAGMENT_LIBS
 *    preference (configured in Preferences > Bio7 > Java Libraries).
 * 
 * 3. For each JAR, it creates a minimal "wrapper" fragment JAR containing only
 *    a MANIFEST.MF that:
 *    - Declares itself as a fragment of com.eco.bio7 (Fragment-Host header)
 *    - References the original JAR via Bundle-ClassPath with "external:" prefix
 * 
 * 4. The fragment JAR is installed into the OSGi runtime. On the next restart,
 *    OSGi resolves the fragment and attaches it to the host bundle.
 * 
 * 5. A preference listener detects when new JARs are added to the list and
 *    installs them dynamically (though full activation requires restart).
 * 
 * ============================================================================
 * FRAGMENT JAR STRUCTURE
 * ============================================================================
 * The generated fragment JARs are minimal (~1KB) and contain only:
 * 
 *   META-INF/MANIFEST.MF:
 *     Manifest-Version: 1.0
 *     Bundle-ManifestVersion: 2
 *     Bundle-SymbolicName: com.eco.bio7.fragment.<jarname>
 *     Bundle-Version: 1.0.0
 *     Fragment-Host: com.eco.bio7
 *     Bundle-ClassPath: external:/path/to/original.jar
 * 
 * The "external:" prefix tells OSGi to load classes directly from the original
 * JAR location without copying the contents.
 * 
 * ============================================================================
 * FILE LOCATIONS
 * ============================================================================
 * - Fragment cache:  ~/Bio7/.fragment-cache/
 *   Stores generated fragment JAR files. Cleared on each startup.
 * 
 * - Log file:        ~/Bio7/fragment-loader.log
 *   Detailed log of all loading operations. Recreated on each startup.
 * 
 * ============================================================================
 * PREFERENCES (stored in com.eco.bio7.javaedit plugin)
 * ============================================================================
 * - FRAGMENT_LOADING_ENABLED (boolean): Master switch to enable/disable loading
 * - FRAGMENT_LIBS (string): Semicolon-separated list of JAR file paths
 * 
 * ============================================================================
 * LIMITATIONS
 * ============================================================================
 * - New fragments require a Bio7 restart to be fully active (OSGi limitation)
 * - Removed JARs also require restart to be unloaded
 * - Fragment classes are only visible to bundles that can see com.eco.bio7
 * - The host bundle (com.eco.bio7) must not be refreshed at runtime to avoid
 *   classloader invalidation errors
 * 
 * ============================================================================
 * USAGE
 * ============================================================================
 * 1. Go to Preferences > Bio7 > Java Libraries
 * 2. Check "Enable OSGi Fragment Loading"
 * 3. Add JAR files to the "OSGi Fragment Libraries" list
 * 4. Click OK/Apply
 * 5. Restart Bio7
 * 6. Classes from the JARs are now available to Bio7 plugins
 * 
 * ============================================================================
 * EXAMPLE USE CASE
 * ============================================================================
 * A user wants to use Apache Commons Math in their Bio7 scripts/plugins:
 * 
 * 1. Download commons-math3-3.6.1.jar
 * 2. Add it to the Fragment Libraries in preferences
 * 3. Restart Bio7
 * 4. Now org.apache.commons.math3.* classes are available
 * 
 * ============================================================================
 * 
 * @see com.eco.bio7.javapreferences.DynamicCompilerJavaLibries
 * For fragment bundles see also: 
 * https://community.bmc.com/s/news/aA3cx0000006ymHCAQ/understanding-fragment-bundles-in-osgi-and-eclipse-rcp
 */
public class Bio7FragmentLoader {
    
    private static final String HOST_BUNDLE = "com.eco.bio7";
    private static final String FRAGMENT_PREFIX = "com.eco.bio7.fragment.";
    
    private static final String EDITOR_PLUGIN_ID = "com.eco.bio7.javaedit";
    private static final String FRAGMENT_LIBS_PREF = "FRAGMENT_LIBS";
    private static final String FRAGMENT_LOADING_ENABLED_PREF = "FRAGMENT_LOADING_ENABLED";
    private static final String LIBS_SEPARATOR = ";";
    
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    
    private BundleContext bundleContext;
    private Path fragmentCacheDir;
    private Path logFile;
    private Set<Path> loadedJarPaths = new HashSet<>();
    private IEclipsePreferences.IPreferenceChangeListener preferenceListener;
    
    public Bio7FragmentLoader(BundleContext context) {
        this.bundleContext = context;
        String userHome = System.getProperty("user.home");
        this.fragmentCacheDir = Paths.get(userHome, "Bio7", ".fragment-cache");
        this.logFile = Paths.get(userHome, "Bio7", "fragment-loader.log");
        
        // Clear old log file
        try {
            Files.deleteIfExists(logFile);
        } catch (IOException e) {
            // Ignore
        }
        
        log("==============================================");
        log("Bio7 Fragment Loader created");
        log("Fragment cache directory: " + fragmentCacheDir);
        log("Log file: " + logFile);
        log("==============================================");
    }
    
    private void log(String message) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        String logMessage = "[" + timestamp + "] [FragmentLoader] " + message;
        
        System.out.println(logMessage);
        
        try {
            Files.writeString(logFile, logMessage + "\n", 
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Ignore
        }
        
        try {
            Bundle bundle = Platform.getBundle(HOST_BUNDLE);
            if (bundle != null) {
                Platform.getLog(bundle).info(message);
            }
        } catch (Exception e) {
            // Ignore
        }
    }
    
    private void logError(String message, Exception e) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        String logMessage = "[" + timestamp + "] [FragmentLoader] ERROR: " + message;
        
        System.err.println(logMessage);
        
        try {
            Files.writeString(logFile, logMessage + "\n", 
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            if (e != null) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                Files.writeString(logFile, sw.toString() + "\n", 
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException ex) {
            // Ignore
        }
        
        try {
            Bundle bundle = Platform.getBundle(HOST_BUNDLE);
            if (bundle != null) {
                if (e != null) {
                    Platform.getLog(bundle).error(message, e);
                } else {
                    Platform.getLog(bundle).error(message);
                }
            }
        } catch (Exception ex) {
            // Ignore
        }
    }
    
    /**
     * Check if fragment loading is enabled in preferences
     */
    private boolean isFragmentLoadingEnabled() {
        try {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(EDITOR_PLUGIN_ID);
            if (prefs != null) {
                return prefs.getBoolean(FRAGMENT_LOADING_ENABLED_PREF, false);
            }
        } catch (Exception e) {
            logError("Could not read enabled preference", e);
        }
        return false;
    }
    
    public void initialize() {
        log("Scheduling initialization job...");
        
        Job initJob = new Job("Initializing Bio7 Fragment Loader") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    // Check if enabled
                    if (!isFragmentLoadingEnabled()) {
                        log("Fragment loading is DISABLED in preferences");
                        log("To enable, go to: Preferences > Bio7 > Java Libraries");
                        log("==============================================");
                        return Status.OK_STATUS;
                    }
                    
                    log("Fragment loading is ENABLED");
                    log("Starting initialization...");
                    
                    clearFragmentCache();
                    Files.createDirectories(fragmentCacheDir);
                    
                    loadAllJarsFromPreference(monitor);
                    
                    setupPreferenceListener();
                    
                    log("Initialization complete!");
                    log("==============================================");
                    
                    return Status.OK_STATUS;
                    
                } catch (Exception e) {
                    logError("Initialization failed", e);
                    return new Status(IStatus.ERROR, HOST_BUNDLE, "Failed to initialize fragment loader", e);
                }
            }
        };
        
        initJob.setSystem(true);
        initJob.setPriority(Job.LONG);
        initJob.schedule();
    }
    
    private void setupPreferenceListener() {
        try {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(EDITOR_PLUGIN_ID);
            if (prefs == null) {
                logError("Could not get preference node for " + EDITOR_PLUGIN_ID, null);
                return;
            }
            
            preferenceListener = event -> {
                String key = event.getKey();
                
                // React to enable/disable changes
                if (FRAGMENT_LOADING_ENABLED_PREF.equals(key)) {
                    boolean enabled = Boolean.parseBoolean(String.valueOf(event.getNewValue()));
                    log("Fragment loading " + (enabled ? "ENABLED" : "DISABLED"));
                    if (enabled) {
                        log("New fragments will be loaded on next restart");
                    }
                }
                
                // React to library list changes (only if enabled)
                if (FRAGMENT_LIBS_PREF.equals(key) && isFragmentLoadingEnabled()) {
                    log("FRAGMENT_LIBS preference changed!");
                    log("Loading new fragment libraries...");
                    loadNewJarsAsync();
                }
            };
            
            prefs.addPreferenceChangeListener(preferenceListener);
            log("Preference listener registered");
            
        } catch (Exception e) {
            logError("Could not setup preference listener", e);
        }
    }
    
    private void loadNewJarsAsync() {
        Job loadJob = new Job("Loading new Bio7 Fragment Libraries") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    // Double-check if still enabled
                    if (!isFragmentLoadingEnabled()) {
                        log("Fragment loading is disabled, skipping");
                        return Status.OK_STATUS;
                    }
                    
                    List<Path> jarPaths = getFragmentLibsFromPreference();
                    int newCount = 0;
                    
                    log("Checking for new fragment JARs...");
                    log("Total JARs in FRAGMENT_LIBS: " + jarPaths.size());
                    log("Already loaded: " + loadedJarPaths.size());
                    
                    for (Path jarPath : jarPaths) {
                        if (!loadedJarPaths.contains(jarPath)) {
                            log("New JAR found: " + jarPath.getFileName());
                            try {
                                if (loadJar(jarPath)) {
                                    newCount++;
                                }
                            } catch (Exception e) {
                                logError("Failed to load: " + jarPath.getFileName(), e);
                            }
                        }
                    }
                    
                    if (newCount > 0) {
                        log("Loaded " + newCount + " new fragment(s)");
                        log("NOTE: New fragments will be fully active after restart!");
                    } else {
                        log("No new JARs to load");
                    }
                    
                    return Status.OK_STATUS;
                    
                } catch (Exception e) {
                    logError("Loading new JARs failed", e);
                    return new Status(IStatus.ERROR, HOST_BUNDLE, "Failed to load new fragments", e);
                }
            }
        };
        
        loadJob.setUser(false);
        loadJob.setPriority(Job.SHORT);
        loadJob.schedule();
    }
    
    private void clearFragmentCache() {
        try {
            if (!Files.exists(fragmentCacheDir)) {
                log("No cache to clear");
                return;
            }
            
            log("Clearing fragment cache...");
            
            final int[] count = {0};
            Files.walkFileTree(fragmentCacheDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    count[0]++;
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            
            log("Cleared " + count[0] + " cached file(s)");
            
        } catch (IOException e) {
            logError("Could not clear cache", e);
        }
    }
    
    private String getFragmentLibsPreference() {
        try {
            IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(EDITOR_PLUGIN_ID);
            if (prefs != null) {
                String value = prefs.get(FRAGMENT_LIBS_PREF, "");
                log("FRAGMENT_LIBS preference: " + (value.isEmpty() ? "(empty)" : value.length() + " chars"));
                return value;
            }
        } catch (Exception e) {
            logError("Could not read preference", e);
        }
        return "";
    }
    
    public List<Path> getFragmentLibsFromPreference() {
        List<Path> jarPaths = new ArrayList<>();
        
        String libs = getFragmentLibsPreference();
        if (libs == null || libs.isEmpty()) {
            return jarPaths;
        }
        
        StringTokenizer tokenizer = new StringTokenizer(libs, LIBS_SEPARATOR);
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("windows");
        
        while (tokenizer.hasMoreTokens()) {
            String libPath = tokenizer.nextToken().trim();
            if (!libPath.isEmpty()) {
                libPath = libPath.replace("\\", "/");
                
                if (!isWindows) {
                    libPath = libPath.replace("::", "");
                }
                
                try {
                    Path path = Paths.get(libPath);
                    if (Files.exists(path) && libPath.toLowerCase().endsWith(".jar")) {
                        jarPaths.add(path.toAbsolutePath().normalize());
                        log("Found fragment JAR: " + path.getFileName());
                    } else {
                        log("Skipping (not found or not JAR): " + libPath);
                    }
                } catch (Exception e) {
                    logError("Invalid path: " + libPath, null);
                }
            }
        }
        
        return jarPaths;
    }
    
    private IStatus loadAllJarsFromPreference(IProgressMonitor monitor) {
        try {
            List<Path> jarPaths = getFragmentLibsFromPreference();
            
            log("Found " + jarPaths.size() + " JAR(s) in FRAGMENT_LIBS preference");
            
            if (jarPaths.isEmpty()) {
                log("No fragment libraries to load");
                return Status.OK_STATUS;
            }
            
            if (monitor != null) {
                monitor.beginTask("Loading fragment libraries", jarPaths.size());
            }
            
            int loaded = 0;
            int skipped = 0;
            int failed = 0;
            
            for (Path jarPath : jarPaths) {
                if (monitor != null && monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }
                
                if (loadedJarPaths.contains(jarPath)) {
                    skipped++;
                    if (monitor != null) monitor.worked(1);
                    continue;
                }
                
                if (monitor != null) {
                    monitor.subTask("Loading: " + jarPath.getFileName());
                }
                
                try {
                    if (loadJar(jarPath)) {
                        loaded++;
                    } else {
                        failed++;
                    }
                } catch (Exception e) {
                    logError("Failed to load: " + jarPath.getFileName(), e);
                    failed++;
                }
                
                if (monitor != null) monitor.worked(1);
            }
            
            if (monitor != null) {
                monitor.done();
            }
            
            log("----------------------------------------");
            log("Loading summary:");
            log("  Loaded:  " + loaded);
            log("  Skipped: " + skipped);
            log("  Failed:  " + failed);
            log("  Total:   " + loadedJarPaths.size());
            log("----------------------------------------");
            
            return Status.OK_STATUS;
            
        } catch (Exception e) {
            logError("Loading JARs failed", e);
            return new Status(IStatus.ERROR, HOST_BUNDLE, "Failed to load fragments", e);
        }
    }
    
    private boolean loadJar(Path jarPath) {
        jarPath = jarPath.toAbsolutePath().normalize();
        
        if (loadedJarPaths.contains(jarPath)) {
            log("Already loaded: " + jarPath.getFileName());
            return false;
        }
        
        if (!Files.exists(jarPath)) {
            logError("JAR not found: " + jarPath, null);
            return false;
        }
        
        log("Loading: " + jarPath.getFileName());
        
        try {
            wrapAsFragment(jarPath);
            loadedJarPaths.add(jarPath);
            log("  -> SUCCESS: " + jarPath.getFileName());
            return true;
            
        } catch (Exception e) {
            logError("  -> FAILED: " + jarPath.getFileName(), e);
            return false;
        }
    }
    
    private void wrapAsFragment(Path jarPath) throws Exception {
        String jarName = jarPath.getFileName().toString();
        String baseName = jarName.replace(".jar", "").replaceAll("[^a-zA-Z0-9]", "_");
        String fragmentSymbolicName = FRAGMENT_PREFIX + baseName;
        String version = "1.0.0";
        
        Path fragmentJarPath = fragmentCacheDir.resolve(fragmentSymbolicName + "_" + version + ".jar");
        
        // Check if already installed in OSGi
        for (Bundle b : bundleContext.getBundles()) {
            if (fragmentSymbolicName.equals(b.getSymbolicName())) {
                log("  Fragment bundle already exists: " + fragmentSymbolicName);
                return;
            }
        }
        
        // Create minimal fragment JAR
        createMinimalFragmentJar(fragmentJarPath, fragmentSymbolicName, version, jarPath);
        
        Bundle fragment = bundleContext.installBundle(fragmentJarPath.toUri().toString());
        log("  Installed bundle: " + fragmentSymbolicName + " (state: " + getStateString(fragment.getState()) + ")");
    }
    
    private String getStateString(int state) {
        switch (state) {
            case Bundle.INSTALLED: return "INSTALLED";
            case Bundle.RESOLVED: return "RESOLVED";
            case Bundle.STARTING: return "STARTING";
            case Bundle.ACTIVE: return "ACTIVE";
            case Bundle.STOPPING: return "STOPPING";
            case Bundle.UNINSTALLED: return "UNINSTALLED";
            default: return "UNKNOWN(" + state + ")";
        }
    }
    
    private void createMinimalFragmentJar(Path fragmentPath, String symbolicName, String version, Path originalJar) throws Exception {
        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.putValue("Bundle-ManifestVersion", "2");
        attrs.putValue("Bundle-SymbolicName", symbolicName);
        attrs.putValue("Bundle-Version", version);
        attrs.putValue("Bundle-Name", "Bio7 Fragment: " + originalJar.getFileName());
        attrs.putValue("Fragment-Host", HOST_BUNDLE);
        
        String jarReference = originalJar.toAbsolutePath().toString().replace("\\", "/");
        attrs.putValue("Bundle-ClassPath", "external:" + jarReference);
        
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(fragmentPath), manifest)) {
            // Empty JAR - only manifest
        }
        
        log("  Created fragment JAR: " + fragmentPath.getFileName());
    }
    
    public void shutdown() {
        log("Shutting down...");
        
        if (preferenceListener != null) {
            try {
                IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(EDITOR_PLUGIN_ID);
                if (prefs != null) {
                    prefs.removePreferenceChangeListener(preferenceListener);
                    log("Preference listener removed");
                }
            } catch (Exception e) {
                // Ignore
            }
            preferenceListener = null;
        }
        
        log("Shutdown complete");
    }
    
    public List<Path> getLoadedJars() {
        return new ArrayList<>(loadedJarPaths);
    }
    
    public int getLoadedCount() {
        return loadedJarPaths.size();
    }
    
    public Path getLogFile() {
        return logFile;
    }
    
    /**
     * Check if fragment loading is currently enabled
     */
    public boolean isEnabled() {
        return isFragmentLoadingEnabled();
    }
}