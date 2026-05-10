/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/

/*
 * =============================================================================
 * DYNAMIC JAVA COMPILATION WITH LIBRARY SUPPORT
 * =============================================================================
 * 
 * This class provides dynamic Java compilation with support for external
 * libraries while ensuring:
 * 
 *   1. Library classes are loaded from JARs (not compiled from source)
 *   2. User classes are compiled from source files
 *   3. Changed source files are recompiled on each run
 *   4. ServiceLoader and Class.forName() work correctly
 * 
 * 
 * ARCHITECTURE DIAGRAM
 * ====================
 * 
 *                    ┌─────────────────────────────────────────┐
 *                    │            User Code                    │
 *                    │         (MyClass.java)                  │
 *                    └─────────────────────────────────────────┘
 *                                      │
 *                                      ▼
 *     ┌────────────────────────────────────────────────────────────────┐
 *     │                  ParentFirstClassLoader                        │
 *     │            (Context ClassLoader for execution)                 │
 *     │                                                                │
 *     │  ┌──────────────────────┐    ┌───────────────────────────┐    │
 *     │  │   hasSourceFile()    │    │  loadClass() / findClass() │    │
 *     │  │                      │    │                            │    │
 *     │  │  - Checks if .java   │    │  - Libraries first         │    │
 *     │  │    file exists       │    │  - Source compile second   │    │
 *     │  └──────────────────────┘    └───────────────────────────┘    │
 *     └────────────────────────────────────────────────────────────────┘
 *                │                                │
 *                ▼                                ▼
 *     ┌─────────────────────┐      ┌─────────────────────────────────┐
 *     │ JavaSourceClassLoader│      │        URLClassLoader           │
 *     │                      │      │      (dynCompilerLoader)        │
 *     │  - Compiles .java    │      │                                 │
 *     │  - Only user code    │      │  ┌───────────────────────────┐ │
 *     │  - Fresh each run    │      │  │  library-a.jar            │ │
 *     └─────────────────────┘      │  │  library-b.jar            │ │
 *                                   │  │  library-c.jar            │ │
 *                                   │  │  ... (other libraries)     │ │
 *                                   │  └───────────────────────────┘ │
 *                                   └─────────────────────────────────┘
 *                                                  │
 *                                                  ▼
 *                                   ┌─────────────────────────────────┐
 *                                   │  Bio7Plugin.class.getClassLoader │
 *                                   │      (or IJ.getClassLoader)      │
 *                                   │                                  │
 *                                   │   Eclipse/OSGi Platform Classes  │
 *                                   └─────────────────────────────────┘
 * 
 * 
 * CLASS LOADING FLOW
 * ==================
 * 
 *     ┌─────────────────────────────────────────────────────────────────┐
 *     │                   Class Loading Request                         │
 *     │          (e.g., "com.library.SomeClass" or "MyClass")           │
 *     └─────────────────────────────────────────────────────────────────┘
 *                                    │
 *                                    ▼
 *     ┌─────────────────────────────────────────────────────────────────┐
 *     │                   ParentFirstClassLoader                        │
 *     │                                                                 │
 *     │   1. Check hasSourceFile(className)                             │
 *     │      │                                                          │
 *     │      ├─► TRUE (user class with .java file):                     │
 *     │      │   └─► sourceClassLoader.findClass()                      │
 *     │      │       └─► Compile .java → Return new Class               │
 *     │      │                                                          │
 *     │      └─► FALSE (library class or no source):                    │
 *     │          └─► libraryClassLoader.loadClass()                     │
 *     │              │                                                  │
 *     │              ├─► Found in JARs → Return Class                   │
 *     │              │                                                  │
 *     │              └─► Not found → ClassNotFoundException             │
 *     │                  (expected for optional/non-existent classes)   │
 *     └─────────────────────────────────────────────────────────────────┘
 * 
 * 
 * EXAMPLES
 * ========
 * 
 *   Example 1: Loading a Library Class
 *   -----------------------------------
 *   Request: Class.forName("com.library.SomeClass")
 *   
 *   1. hasSourceFile("com.library.SomeClass") → FALSE (no .java file)
 *   2. libraryClassLoader.loadClass() → Found in library JAR
 *   3. Return class from JAR ✓
 * 
 * 
 *   Example 2: Loading a Non-Existent Class
 *   ----------------------------------------
 *   Request: Class.forName("com.library.OptionalFeature")
 *   
 *   1. hasSourceFile("com.library.OptionalFeature") → FALSE
 *   2. libraryClassLoader.loadClass() → ClassNotFoundException
 *   3. Throw ClassNotFoundException (calling code handles this) ✓
 * 
 * 
 *   Example 3: Loading a User Class
 *   --------------------------------
 *   Request: loadClass("MyClass")
 *   
 *   1. hasSourceFile("MyClass") → TRUE (src/MyClass.java exists)
 *   2. sourceClassLoader.findClass() → Compile source
 *   3. Return newly compiled class ✓
 * 
 * 
 *   Example 4: Loading a User Class in a Package
 *   ---------------------------------------------
 *   Request: loadClass("com.myapp.MyClass")
 *   
 *   1. hasSourceFile("com.myapp.MyClass") → TRUE (src/com/myapp/MyClass.java exists)
 *   2. sourceClassLoader.findClass() → Compile source
 *   3. Return newly compiled class ✓
 * 
 * 
 *   Example 5: Recompiling Changed Source
 *   --------------------------------------
 *   First run:  Compile MyClass.java → executes original code
 *   Edit file:  Modify MyClass.java
 *   Second run: Fresh classloaders → Recompile → executes updated code ✓
 * 
 * 
 * WHY RELOADING WORKS
 * ===================
 * 
 *   1. Fresh ClassLoaders: New JavaSourceClassLoader and ParentFirstClassLoader
 *      are created for EACH compilation run (not cached/reused)
 *   
 *   2. No Parent Caching: ParentFirstClassLoader uses super(null) to prevent
 *      the default parent classloader from caching user classes
 *   
 *   3. Source-First for User Classes: Classes with .java files always go
 *      through the fresh sourceClassLoader, triggering recompilation
 *   
 *   4. Library Caching is OK: Library classes (JARs) are cached in
 *      dynCompilerLoader, which is efficient since they don't change
 * 
 * 
 * CONTEXT CLASSLOADER
 * ===================
 * 
 *   Before executing user code, the context classloader is set:
 *   
 *     Thread.currentThread().setContextClassLoader(wrapperLoader);
 *   
 *   This ensures:
 *   - ServiceLoader finds service implementations from JARs
 *   - Class.forName() uses the correct classloader hierarchy
 *   - Library code can load its dependencies correctly
 *   
 *   The original context classloader is restored after execution.
 * 
 * 
 * For Java compiler version updates, also change:
 *   - AST.JLS25 in this class
 *   - JavaSourceClassLoader
 *   - WorkbenchPreferenceJava
 *   - PreferenceInitializer
 * 
 * =============================================================================
 */
package com.eco.bio7.compile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.codehaus.commons.compiler.jdk.JavaSourceClassLoader;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.BatchModel;
import com.eco.bio7.batch.FileRoot;
import com.eco.bio7.javaeditor.Bio7EditorPlugin;
import com.eco.bio7.methods.Compiled;
import com.eco.bio7.rcp.StartBio7Utils;
import com.eco.bio7.util.Bio7Dialog;
import com.eco.bio7.worldwind.DynamicLayer;
import ij.IJ;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;
import ij.plugin.frame.PlugInFrame;

public class CompileClassAndMultipleClasses {
    private File fi;
    private String name;
    private String dir;
    private IResource resource;
    private IWorkbenchPage pag;
    private IFile ifile;
    private IEditorPart editor;
    private Thread processThread;

    /**
     * A wrapper classloader that delegates to parent first, then to JavaSourceClassLoader.
     * This prevents the JavaSourceClassLoader from trying to compile classes that exist in JARs.
     * Only classes with actual source files will be compiled.
     * 
     * IMPORTANT: This classloader does NOT cache user classes - it always delegates to
     * the sourceClassLoader which will recompile if needed.
     */
    private static class ParentFirstClassLoader extends ClassLoader {
        private final JavaSourceClassLoader sourceClassLoader;
        private final ClassLoader libraryClassLoader;
        private final File[] sourcePaths;

        public ParentFirstClassLoader(ClassLoader libraryClassLoader, JavaSourceClassLoader sourceClassLoader, File[] sourcePaths) {
            super(libraryClassLoader);  // <-- USE PARENT for resource delegation
            this.libraryClassLoader = libraryClassLoader;
            this.sourceClassLoader = sourceClassLoader;
            this.sourcePaths = sourcePaths;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> c = null;
                
                // First check if already loaded by THIS classloader
                c = findLoadedClass(name);
                if (c != null) {
                    if (resolve) resolveClass(c);
                    return c;
                }
                
                // For user classes with source files, compile them
                if (hasSourceFile(name)) {
                    try {
                        c = sourceClassLoader.findClass(name);
                    } catch (ClassNotFoundException | RuntimeException e) {
                        throw new ClassNotFoundException("Failed to compile: " + name, e);
                    }
                } else {
                    // For library classes, delegate to parent (libraryClassLoader)
                    // This will also handle caching properly
                    c = super.loadClass(name, false);
                }
                
                if (resolve && c != null) {
                    resolveClass(c);
                }
                return c;
            }
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (hasSourceFile(name)) {
                try {
                    return sourceClassLoader.findClass(name);
                } catch (ClassNotFoundException | RuntimeException e) {
                    throw new ClassNotFoundException("Failed to compile: " + name, e);
                }
            }
            // This will throw ClassNotFoundException, which is correct
            throw new ClassNotFoundException(name);
        }
        
        private boolean hasSourceFile(String className) {
            if (sourcePaths == null || sourcePaths.length == 0) {
                return false;
            }
            
            String relativePath = className.replace('.', File.separatorChar) + ".java";
            
            for (File sourcePath : sourcePaths) {
                File sourceFile = new File(sourcePath, relativePath);
                if (sourceFile.exists() && sourceFile.isFile()) {
                    return true;
                }
            }
            return false;
        }
        
        public Class<?> findSourceClass(String name) throws ClassNotFoundException {
            return sourceClassLoader.findClass(name);
        }
    }

    public void compileClasses(IResource res, IFile ifil, IWorkbenchPage page) {
        this.resource = res;
        this.pag = page;
        this.ifile = ifil;
        IWorkbench aWorkbench = PlatformUI.getWorkbench();
        IWorkbenchWindow win = aWorkbench.getActiveWorkbenchWindow();

        IWorkbenchPage pages = null;
        try {
            pages = win.getActivePage();
        } catch (Exception e1) {
            // Ignore
        }
        if (pages != null) {
            editor = (IEditorPart) pages.getActiveEditor();
        }
        JavaSourceClassLoader.resource = resource;
        IProject proj = resource.getProject();

        try {
            proj.deleteMarkers(IMarker.PROBLEM, true, IProject.DEPTH_INFINITE);
            proj.refreshLocal(IProject.DEPTH_INFINITE, null);
        } catch (CoreException e) {
            e.printStackTrace();
        }

        IPath pa = proj.getLocation();

        fi = pa.toFile();

        /* Get the filename without extension! */
        name = ifile.getName().replaceFirst("[.][^.]+$", "");

        pa.toFile().getPath().replace("\\", "/");

        /* Get the parent directory! */
        dir = pa.toFile().getPath().replace("\\", "/") + "/src";

        StartBio7Utils.getConsoleInstance().cons.clear();

        Job job = new Job("Compile And Run") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask("Compile And Run...", IProgressMonitor.UNKNOWN);
                compileAndLoad(fi, dir, name, pag, false);
                monitor.done();
                return Status.OK_STATUS;
            }
        };
        
        job.addJobChangeListener(new JobChangeAdapter() {
            public void done(IJobChangeEvent event) {
                if (event.getResult().isOK()) {
                    BatchModel.resume();
                }
            }
        });

        job.schedule();
    }

    /**
     * Check if the editor is a Java editor using public API.
     * Returns the ICompilationUnit if it's a Java editor, null otherwise.
     */
    private ICompilationUnit getCompilationUnitFromEditor(IEditorPart editor) {
        if (editor == null) {
            return null;
        }
        try {
            IJavaElement javaElement = JavaUI.getEditorInputJavaElement(editor.getEditorInput());
            if (javaElement instanceof ICompilationUnit) {
                return (ICompilationUnit) javaElement;
            }
        } catch (Exception e) {
            // Not a Java editor or other error
        }
        return null;
    }

    /*
     * This method is called from above but also from the script menu actions
     * directly (external Java path!)
     */
    public void compileAndLoad(File path, String dir, String name, IWorkbenchPage pag, boolean startupScript) {
        FileRoot.setCurrentCompileDir(dir);
        ClassLoader classLoaderMain;
        IPreferenceStore store = Bio7EditorPlugin.getDefault().getPreferenceStore();
        boolean mainClassLoader = store.getBoolean("COMPILE_CLASSLOADER_IMAGEJ");
        if (mainClassLoader == false) {
            classLoaderMain = Bio7Plugin.class.getClassLoader();
        } else {
            classLoaderMain = IJ.getClassLoader();
        }
        
        // 1. Get the compiled libraries from preferences
        List<String> jarPaths = DynamicCompilerClassLoaderUtil.getDynamicCompilerLibraries();

        URLClassLoader dynCompilerLoader = null;
        try {
            dynCompilerLoader = DynamicCompilerClassLoaderUtil.buildLibraryClassLoader(jarPaths, classLoaderMain);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create the JavaSourceClassLoader with the library classloader as parent
        JavaSourceClassLoader sourceLoader = new JavaSourceClassLoader(dynCompilerLoader);
        sourceLoader.pag = pag;

        // Determine source paths
        File[] sourcePaths;
        
        /*
         * Here we set the path for the compilation of java files in the scripts menu
         * folder and the drag and drop class files. We don't delete a bin (*.class
         * files) folder!
         */
        if (startupScript) {
            sourcePaths = new File[] { new File(dir) };
            sourceLoader.setSourcePath(sourcePaths);
            sourceLoader.setBinaryPath(new File[] { new File(dir) });
        } else {
            try {
                FileUtils.cleanDirectory(new File(path.getAbsolutePath() + "/bin"));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            sourcePaths = new File[] { new File(dir) };
            sourceLoader.setSourcePath(sourcePaths);
            sourceLoader.setBinaryPath(new File[] { new File(path.getAbsolutePath() + "/bin") });
        }

        // Create the parent-first wrapper classloader with source paths for checking
        ParentFirstClassLoader wrapperLoader = new ParentFirstClassLoader(dynCompilerLoader, sourceLoader, sourcePaths);

        Object o = null;
        Class<?> cl = null;

        // Check if we have a Java editor using public API
        ICompilationUnit cu = getCompilationUnitFromEditor(editor);
        
        /* If we have an opened Java Editor! */
        if (cu != null) {
            ASTParser parser = ASTParser.newParser(AST.JLS25);
            parser.setSource(cu);
            org.eclipse.jdt.core.dom.CompilationUnit compUnit = 
                (org.eclipse.jdt.core.dom.CompilationUnit) parser.createAST(null);

            PackageDeclaration pdecl = compUnit.getPackage();
            if (pdecl != null) {
                Name packName = pdecl.getName();
                String pack = packName.toString();
                try {
                    cl = wrapperLoader.findSourceClass(pack + "." + name);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    cl = wrapperLoader.findSourceClass(name);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        /* Compile startup scripts! */
        else if (startupScript) {
            org.eclipse.jdt.core.dom.CompilationUnit compUnit = null;
            Document doc = new Document(BatchModel.fileToString(path.getAbsolutePath()));

            ASTParser parser = ASTParser.newParser(AST.JLS25);
            parser.setSource(doc.get().toCharArray());
            compUnit = (org.eclipse.jdt.core.dom.CompilationUnit) parser.createAST(null);
            PackageDeclaration pdecl = compUnit.getPackage();
            if (pdecl != null) {
                Name packName = pdecl.getName();
                String pack = packName.toString();
                try {
                    cl = wrapperLoader.findSourceClass(pack + "." + name);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    cl = wrapperLoader.findSourceClass(name);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
        /*
         * If we compile from the context menu or the Flow editor we create the AST from
         * the file!
         */
        else {
            org.eclipse.jdt.core.dom.CompilationUnit compUnit = null;
            Document doc = new Document(BatchModel.fileToString(ifile.getRawLocation().toString()));

            ASTParser parser = ASTParser.newParser(AST.JLS25);
            parser.setSource(doc.get().toCharArray());
            compUnit = (org.eclipse.jdt.core.dom.CompilationUnit) parser.createAST(null);
            PackageDeclaration pdecl = compUnit.getPackage();
            if (pdecl != null) {
                Name packName = pdecl.getName();
                String pack = packName.toString();
                try {
                    cl = wrapperLoader.findSourceClass(pack + "." + name);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    cl = wrapperLoader.findSourceClass(name);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        boolean useObjectCreation = store.getBoolean("COMPILE_OBJECT_CREATION");
        if (useObjectCreation) {

            try {
                o = cl.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e1) {
                e1.printStackTrace();
            }

            if (o != null) {
                if (o instanceof Model) {
                    Model model = (Model) o;
                    Compiled.setModel(model);
                    /* For Java WorldWind! */
                    DynamicLayer.setEcoclass(model);
                }

                else if (o instanceof PlugInFrame) {
                    return;
                }

                else if (o instanceof PlugIn) {
                    callPlugin(cl, wrapperLoader);

                } else if (o instanceof PlugInFilter) {
                    callPluginFilter(cl, wrapperLoader);

                }

                else {
                    Method method = null;
                    try {
                        method = cl.getMethod("main", String[].class);

                    } catch (NoSuchMethodException | SecurityException e) {
                        System.out.println("No main method! Only class compiled and loaded!");
                    }

                    if (method != null) {
                        callMainMethod(method, wrapperLoader);
                    }
                }
            } else {
                System.out.println("Object not created! Null reference!");
            }

            sourceLoader = null;
            cl = null;
            o = null;
        } else {
            callMainMethodNoInstance(cl, null, wrapperLoader);
        }
    }

    private void callPlugin(final Class<?> cl, ClassLoader contextLoader) {
        ClassLoader originalContextCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(contextLoader);
            ((PlugIn) cl.getDeclaredConstructor().newInstance()).run("");
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextCL);
        }
    }

    private void callPluginFilter(final Class<?> cl, ClassLoader contextLoader) {
        ClassLoader originalContextCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(contextLoader);
            new PlugInFilterRunner(cl.getDeclaredConstructor().newInstance(), "plugin", "");
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextCL);
        }
    }

    private void callMainMethod(Method method, ClassLoader contextLoader) {
        ClassLoader originalContextCL = Thread.currentThread().getContextClassLoader();
        try {
            // Set context classloader to the wrapper loader
            // This ensures ServiceLoader finds classes from the library JARs
            Thread.currentThread().setContextClassLoader(contextLoader);

            String[] params = { "" };
            method.invoke(null, (Object) params);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextCL);
        }
    }

    /* This only calls the main method without creating an object instance! */
    private void callMainMethodNoInstance(final Class<?> cl, String[] param, ClassLoader contextLoader) {
        Method meth = null;
        try {
            meth = cl.getMethod("main", String[].class);
        } catch (NoSuchMethodException | SecurityException e3) {
            Bio7Dialog.message("No main method available!");
        }
        if (meth != null) {
            ClassLoader originalContextCL = Thread.currentThread().getContextClassLoader();
            try {
                // Set context classloader to the wrapper loader
                Thread.currentThread().setContextClassLoader(contextLoader);

                String[] params = param;
                meth.invoke(null, (Object) params);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e2) {
                e2.printStackTrace();
            } finally {
                Thread.currentThread().setContextClassLoader(originalContextCL);
            }
        }
    }
}