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
 * 
 * Changed for Bio7!
 */

package org.codehaus.commons.compiler.jdk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Vector;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.Cookable;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.codehaus.commons.compiler.Location;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;

import com.eco.bio7.compile.CompilerMessages;
import com.eco.bio7.compile.utils.ScanClassPath;
import com.eco.bio7.javaeditor.Bio7EditorPlugin;
//import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.api.JavacTool;

public class SimpleCompiler extends Cookable implements ISimpleCompiler {

	private ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
	private ClassLoader result;
	private boolean debugSource;
	private boolean debugLines;
	private boolean debugVars;
	private static String script;
	public static IResource resource;

	@Override
	public ClassLoader getClassLoader() {
		assertCooked();
		return this.result;
	}

	public class MemorySource extends SimpleJavaFileObject {
		public String src;
		public String name;

		public MemorySource(String name, String src) {
			/* Here the filename is created for the compilation! */
			super(URI.create("file:///" + name + ".java"), Kind.SOURCE);
			this.src = src;
		}

		public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return src;
		}

		public OutputStream openOutputStream() {
			throw new IllegalStateException();
		}

		public InputStream openInputStream() {
			return new ByteArrayInputStream(src.getBytes());
		}
	}

	@Override
	public void cook(String optionalFileName, final Reader r) throws CompileException, IOException {
		assertNotCooked();

		// JavaCompiler compiler = new EclipseCompiler();
		JavacTool compiler = com.sun.tools.javac.api.JavacTool.create();
		 //JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

		if (compiler == null) {
			throw new CompileException("JDK Java compiler not available - probably you're running a JRE, not a JDK", null);
		}

		final JavaFileManager fm = compiler.getStandardFileManager(null, null, null);

		final JavaFileManager fileManager = new ByteArrayJavaFileManager<JavaFileManager>(fm);

		Vector<String> optionList = new Vector<String>();
		IPreferenceStore store = Bio7EditorPlugin.getDefault().getPreferenceStore();
		String version = store.getString("compiler_version");
		boolean debug = store.getBoolean("compiler_debug");
		boolean verbose = store.getBoolean("compiler_verbose");
		boolean warnings = store.getBoolean("compiler_warnings");
		optionList.addElement("-source");
		optionList.addElement(version);
		optionList.addElement("-target");
		optionList.addElement(version);

		optionList.addElement("-classpath");
		/* Add the Bio7 libs etc. for the compiler! */
		optionList.addElement(new ScanClassPath().scan());

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
		Writer out = new PrintWriter(System.err);

		if (resource != null) {
			try {
				resource.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
			} catch (CoreException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		MemorySource sf = new MemorySource(optionalFileName, readString(r));
		// Run the compiler.
		try {
			if (!compiler.getTask(out, // out
					fileManager, // fileManager
					diagnosticsCollector, optionList, null, // classes - first
															// null can be
															// diagnostics for
															// line number
															// extraction etc.
					Collections.singleton(sf) // compilationUnits
					).call()) {

				markError(diagnosticsCollector);
				throw new CompileException("Compilation failed", null);

			}

		} catch (RuntimeException rte) {

			// Unwrap the compilation exception and throw it.
			Throwable cause = rte.getCause();
			if (cause != null) {
				cause = cause.getCause();
				if (cause instanceof CompileException)
					throw (CompileException) cause;
				if (cause instanceof IOException)
					throw (IOException) cause;
			}
			throw rte;
		}

		// Create a ClassLoader that reads class files from our FM.
		this.result = AccessController.doPrivileged(new PrivilegedAction<JavaFileManagerClassLoader>() {
			public JavaFileManagerClassLoader run() {
				return new JavaFileManagerClassLoader(fileManager, SimpleCompiler.this.parentClassLoader);
			}
		});
	}

	private void markError(DiagnosticCollector<JavaFileObject> diagnosticsCollector) {
		/* Calculate the char Positions for the Java editor! */
		int calculatedCharPosition;
		/* If only SimpleCompiler is called no correction is needed! */

		calculatedCharPosition = CompilerMessages.getString("Import.bio7").length() + 54;// Default
																							// Imports
																							// +
																							// Class
																							// (54
																							// chars)declaration!!!

		for (Diagnostic diagnostic : diagnosticsCollector.getDiagnostics()) {
			System.out.format("Error on line %d" + " -> ", diagnostic.getLineNumber(), diagnostic);
			System.out.println(diagnostic.getMessage(null) + "\n");
			// System.out.format("Error on line %d in %s"+"\n",
			// diagnostic.getLineNumber(), diagnostic);

			int lnr = (int) diagnostic.getLineNumber();
			int startPos = (int) diagnostic.getStartPosition();
			int stopPos = (int) diagnostic.getEndPosition();
			if (resource != null) {
				IMarker marker;
				try {
					marker = resource.createMarker(IMarker.PROBLEM);
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					marker.setAttribute(IMarker.MESSAGE, diagnostic.getMessage(null));
					marker.setAttribute(IMarker.LINE_NUMBER, lnr);
					// if (pos.offset != 0) {
					// System.out.println(startPos);
					// System.out.println(stopPos);

					marker.setAttribute(IMarker.CHAR_START, startPos - calculatedCharPosition);
					marker.setAttribute(IMarker.CHAR_END, stopPos - calculatedCharPosition - 1);
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}

	protected void cook(JavaFileObject compilationUnit) throws CompileException, IOException {

		// Find the JDK Java compiler.
		// JavaCompiler compiler = new EclipseCompiler();
		JavacTool compiler = com.sun.tools.javac.api.JavacTool.create();
		///JavaCompiler compiler  = ToolProvider.getSystemJavaCompiler();

		if (compiler == null) {
			throw new CompileException("JDK Java compiler not available - probably you're running a JRE, not a JDK", null);
		}

		// Get the original FM, which reads class files through this JVM's
		// BOOTCLASSPATH and
		// CLASSPATH.
		final JavaFileManager fm = compiler.getStandardFileManager(null, null, null);

		// Wrap it so that the output files (in our case class files) are stored
		// in memory rather
		// than in files.
		final JavaFileManager fileManager = new ByteArrayJavaFileManager<JavaFileManager>(fm);

		// Run the compiler.
		try {
			if (!compiler.getTask(null, // out
					fileManager, // fileManager
					new DiagnosticListener<JavaFileObject>() { // diagnosticListener

						@Override
						public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
							System.err.println("*** " + diagnostic.toString() + " *** " + diagnostic.getCode());

							Location loc = new Location(diagnostic.getSource().toString(), (short) diagnostic.getLineNumber(), (short) diagnostic.getColumnNumber());
							String code = diagnostic.getCode();
							String message = diagnostic.getMessage(null) + " (" + code + ")";

							// Wrap the exception in a RuntimeException, because
							// "report()" does not declare checked
							// exceptions.
							throw new RuntimeException(new CompileException(message, loc));
						}
					}, null, // options
					null, // classes
					Collections.singleton(compilationUnit) // compilationUnits
					).call()) {
				throw new CompileException("Compilation failed", null);
			}
		} catch (RuntimeException rte) {

			// Unwrap the compilation exception and throw it.
			Throwable cause = rte.getCause();
			if (cause != null) {
				cause = cause.getCause();
				if (cause instanceof CompileException)
					throw (CompileException) cause;
				if (cause instanceof IOException)
					throw (IOException) cause;
			}
			throw rte;
		}

		// Create a ClassLoader that reads class files from our FM.
		this.result = AccessController.doPrivileged(new PrivilegedAction<JavaFileManagerClassLoader>() {
			public JavaFileManagerClassLoader run() {
				return new JavaFileManagerClassLoader(fileManager, SimpleCompiler.this.parentClassLoader);
			}
		});
	}

	@Override
	public void setDebuggingInformation(boolean debugSource, boolean debugLines, boolean debugVars) {
		this.debugSource = debugSource;
		this.debugLines = debugLines;
		this.debugVars = debugVars;
	}

	@Override
	public void setParentClassLoader(ClassLoader optionalParentClassLoader) {
		assertNotCooked();
		this.parentClassLoader = (optionalParentClassLoader != null ? optionalParentClassLoader : Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Auxiliary classes never really worked... don't use them.
	 * 
	 * @param optionalParentClassLoader
	 * @param auxiliaryClasses
	 */
	@Deprecated
	public void setParentClassLoader(ClassLoader optionalParentClassLoader, Class<?>[] auxiliaryClasses) {
		this.setParentClassLoader(optionalParentClassLoader);
	}

	/**
	 * Throw an {@link IllegalStateException} if this {@link Cookable} is not
	 * yet cooked.
	 */
	protected void assertCooked() {
		if (this.result == null)
			throw new IllegalStateException("Not yet cooked");
	}

	/**
	 * Throw an {@link IllegalStateException} if this {@link Cookable} is
	 * already cooked.
	 */
	protected void assertNotCooked() {
		if (this.result != null)
			throw new IllegalStateException("Already cooked");
	}

}
