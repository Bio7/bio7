/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.commons.compiler.jdk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Vector;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.codehaus.commons.compiler.AbstractJavaSourceClassLoader;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.jdk.ByteArrayJavaFileManager.ByteArrayJavaFileObject;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import com.eco.bio7.compile.utils.ScanClassPath;
import com.eco.bio7.javaeditor.Bio7EditorPlugin;
//import com.sun.tools.javac.api.JavacTool;

public class JavaSourceClassLoader extends AbstractJavaSourceClassLoader {

	private File[] sourcePath;
	private String optionalCharacterEncoding;
	private boolean debuggingInfoLines;
	private boolean debuggingInfoVars;
	private boolean debuggingInfoSource;
	private Collection<String> compilerOptions = new ArrayList<String>();

	private JavaCompiler compiler;
	private JavaFileManager fileManager;

	public static IResource resource;
	// public boolean directCall = true;
	public IWorkbenchPage pag;
	protected IFile ifile;

	/**
	 * @see ICompilerFactory#newJavaSourceClassLoader()
	 */
	public JavaSourceClassLoader() {
		this.init();
	}

	/**
	 * @see ICompilerFactory#newJavaSourceClassLoader(ClassLoader)
	 */
	public JavaSourceClassLoader(ClassLoader parentClassLoader) {
		super(parentClassLoader);
		this.init();
		this.pag = pag;
	}

	private void init() {
		 this.compiler = ToolProvider.getSystemJavaCompiler();
		// JavaCompiler compiler = new EclipseCompiler();
		
		//this.compiler = com.sun.tools.javac.api.JavacTool.create();
		if (this.compiler == null) {
			throw new UnsupportedOperationException("JDK Java compiler not available - probably you're running a JRE, not a JDK");
		}
	}

	/**
	 * Creates the underlying {@link JavaFileManager} lazily, because
	 * {@link #setSourcePath(File[])} and consorts are called <i>after</i>
	 * initialization.
	 */
	JavaFileManager getJavaFileManager() {
		if (this.fileManager == null) {

			// Get the original FM, which reads class files through this JVM's
			// BOOTCLASSPATH and
			// CLASSPATH.
			JavaFileManager jfm = this.compiler.getStandardFileManager(null, null, null);

			// Wrap it so that the output files (in our case class files) are
			// stored in memory rather
			// than in files.
			jfm = new ByteArrayJavaFileManager<JavaFileManager>(jfm);

			// Wrap it in a file manager that finds source files through the
			// source path.
			jfm = new FileInputJavaFileManager(jfm, StandardLocation.SOURCE_PATH, Kind.SOURCE, this.sourcePath, this.optionalCharacterEncoding);

			this.fileManager = jfm;
		}
		return this.fileManager;
	}

	@Override
	public void setSourcePath(File[] sourcePath) {
		this.sourcePath = sourcePath;
	}

	@Override
	public void setSourceFileCharacterEncoding(String optionalCharacterEncoding) {
		this.optionalCharacterEncoding = optionalCharacterEncoding;
	}

	@Override
	public void setDebuggingInfo(boolean lines, boolean vars, boolean source) {
		this.debuggingInfoLines = lines;
		this.debuggingInfoVars = vars;
		this.debuggingInfoSource = source;
	}

	/**
	 * Notice: Don't use the '-g' options - these are controlled through
	 * {@link #setDebuggingInfo(boolean, boolean, boolean)}.
	 * 
	 * @param compilerOptions
	 *            All command line options supported by the JDK JAVAC tool
	 */
	public void setCompilerOptions(String[] compilerOptions) {
		this.compilerOptions = Arrays.asList(compilerOptions);
	}

	/**
	 * Implementation of {@link ClassLoader#findClass(String)}.
	 * 
	 * @throws ClassNotFoundException
	 */
	public Class<?> findClass(String className) throws ClassNotFoundException {

		byte[] ba;
		int size;
		try {

			// Maybe the bytecode is already there, because the class was
			// compiled as a side effect of a preceding
			// compilation.
			JavaFileObject classFileObject = this.getJavaFileManager().getJavaFileForInput(StandardLocation.CLASS_OUTPUT, className, Kind.CLASS);

			if (classFileObject == null) {

				// Get the sourceFile.
				JavaFileObject sourceFileObject = this.getJavaFileManager().getJavaFileForInput(StandardLocation.SOURCE_PATH, className, Kind.SOURCE);
				if (sourceFileObject == null) {
					throw new DiagnosticException("Source for '" + className + "' not found");
				}
				// Compose the effective compiler options.
				Vector<String> optionList = new Vector<String>();

				IPreferenceStore store = Bio7EditorPlugin.getDefault().getPreferenceStore();
				String version = store.getString("compiler_version");
				boolean debug = store.getBoolean("compiler_debug");
				boolean verbose = store.getBoolean("compiler_verbose");
				boolean warnings = store.getBoolean("compiler_warnings");
				boolean createMarker = store.getBoolean("compiler_marker");
				if(version.equals("1.9")) {
				optionList.addElement("--module-path=I:/openjdk11/jdk-11/javafx-sdk-11/lib");
				}
				optionList.addElement("--add-modules=ALL-SYSTEM");
				
				
				optionList.addElement("-source");
				optionList.addElement(version);
				optionList.addElement("-target");
				optionList.addElement(version);

				optionList.addElement("-classpath");
				/* Add the Bio7 libs etc. for the compiler! */
				optionList.addElement(new ScanClassPath().scan());
				if(version.equals("1.9")) {
				//optionList.addElement("--add-modules=javafx.controls,javafx.base,javafx.fxml,javafx.graphics,javafx.media,javafx.swing,javafx.web,javafx.swt");
				}
	 
				if (debug) {
					optionList.addElement("-g");
				} else {
					optionList.addElement("-g:none");
				}
				if (verbose) {
					optionList.addElement("-verbose");
				}

				if (warnings == false) {
					optionList.addElement("-nowarn");
				}

				DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<JavaFileObject>();

				// Run the compiler.
				boolean success = this.compiler.getTask(null, // out
						this.getJavaFileManager(), // fileManager
						diagnosticsCollector, optionList, // options
						null, // classes
						Collections.singleton(sourceFileObject) // compilationUnits
				).call();

				if (!success) {

					markError(diagnosticsCollector, createMarker);

					throw new ClassNotFoundException(className + ": Compilation failed");
				}

				classFileObject = this.getJavaFileManager().getJavaFileForInput(StandardLocation.CLASS_OUTPUT, className, Kind.CLASS);

				if (classFileObject == null) {
					throw new ClassNotFoundException(className + ": Class file not created by compilation");
				}

			}

			if (classFileObject instanceof ByteArrayJavaFileObject) {
				ByteArrayJavaFileObject bajfo = (ByteArrayJavaFileObject) classFileObject;
				ba = bajfo.toByteArray();
				size = ba.length;
			} else {
				ba = new byte[4096];
				size = 0;
				InputStream is = classFileObject.openInputStream();
				try {
					for (;;) {
						int res = is.read(ba, size, ba.length - size);
						if (res == -1)
							break;
						size += res;
						if (size == ba.length) {
							byte[] tmp = new byte[2 * size];
							System.arraycopy(ba, 0, tmp, 0, size);
							ba = tmp;
						}
					}
				} finally {
					is.close();
				}
			}
		} catch (IOException ioe) {
			throw new DiagnosticException(ioe);
		}

		return this.defineClass(className, ba, 0, size,
				(this.optionalProtectionDomainFactory == null ? null : this.optionalProtectionDomainFactory.getProtectionDomain(getSourceResourceName(className))));
	}

	private void markError(DiagnosticCollector<JavaFileObject> diagnosticsCollector, boolean markerCreation) {

		for (Diagnostic diagnostic : diagnosticsCollector.getDiagnostics()) {
			System.out.format("Error on line %d" + " -> ", diagnostic.getLineNumber(), diagnostic);
			System.out.println(diagnostic.getMessage(null) + "\n");
			System.err.println("*** " + diagnostic.toString() + " *** " + diagnostic.getCode());
			JavaFileObject source = (JavaFileObject) diagnostic.getSource();
			String longFileName = source == null ? null : source.toUri().getPath();
			// String shortFileName = source == null ? null : source.getName();

			// System.out.println("Error in: " + longFileName);
			// Path path = new Path(longFileName);
			// IFile ifile =
			// ResourcesPlugin.getWorkspace().getRoot().getFile(path);
			if (diagnostic.getLineNumber() > -1) {
				File fileToOpen = new File(longFileName);

				if (fileToOpen.exists() && fileToOpen.isFile()) {
					final IFileStore fileStore = EFS.getLocalFileSystem().getStore(fileToOpen.toURI());

					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {

						public void run() {

							try {
								IEditorPart part = IDE.openEditorOnFileStore(pag, fileStore);

								if (part != null) {
									IEditorInput input = part.getEditorInput();
									ifile = ((IFileEditorInput) input).getFile();

									// ... use activeProjectName
								}

							} catch (PartInitException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					});

				} else {
					// Do something if the file does not exist
				}
			}

			// System.out.format("Error on line %d in %s"+"\n",
			// diagnostic.getLineNumber(), diagnostic);
			if (markerCreation == true) {
				int lnr = (int) diagnostic.getLineNumber();
				int startPos = (int) diagnostic.getStartPosition();
				int stopPos = (int) diagnostic.getEndPosition();
				if (ifile != null) {
					IMarker marker;
					try {
						marker = ifile.createMarker(IMarker.PROBLEM);
						marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
						marker.setAttribute(IMarker.MESSAGE, diagnostic.getMessage(null));
						marker.setAttribute(IMarker.LINE_NUMBER, lnr);
						// if (pos.offset != 0) {
						// System.out.println(startPos);
						// System.out.println(stopPos);

						marker.setAttribute(IMarker.CHAR_START, startPos);
						marker.setAttribute(IMarker.CHAR_END, stopPos - 1);
					} catch (CoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

		}
	}

	/**
	 * Construct the name of a resource that could contain the source code of the
	 * class with the given name.
	 * <p>
	 * Notice that member types are declared inside a different type, so the
	 * relevant source file is that of the outermost declaring class.
	 * 
	 * @param className
	 *            Fully qualified class name, e.g. "pkg1.pkg2.Outer$Inner"
	 * @return the name of the resource, e.g. "pkg1/pkg2/Outer.java"
	 */
	private static String getSourceResourceName(String className) {

		// Strip nested type suffixes.
		{
			int idx = className.lastIndexOf('.') + 1;
			idx = className.indexOf('$', idx);
			if (idx != -1)
				className = className.substring(0, idx);

		}

		return className.replace('.', '/') + ".java";
	}

	public static class DiagnosticException extends RuntimeException {

		private static final long serialVersionUID = 5589635876875819926L;

		public DiagnosticException(String message) {
			super(message);
		}

		public DiagnosticException(Throwable cause) {
			super(cause);
		}

		public DiagnosticException(Diagnostic<? extends JavaFileObject> diagnostic) {
			super(diagnostic.toString());
		}
	}

}
