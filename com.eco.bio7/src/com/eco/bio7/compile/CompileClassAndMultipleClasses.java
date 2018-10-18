/*******************************************************************************
 * Copyright (c) 2007-2013 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.compile;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.swing.SwingUtilities;
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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.ui.IWorkingCopyManager;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.BatchModel;
import com.eco.bio7.batch.FileRoot;
import com.eco.bio7.javaeditors.JavaEditor;
import com.eco.bio7.methods.Compiled;
import com.eco.bio7.rcp.StartBio7Utils;
import com.eco.bio7.worldwind.DynamicLayer;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.PlugInFilterRunner;

public class CompileClassAndMultipleClasses {
	private File fi;
	private String name;
	private String dir;
	private IResource resource;
	private IWorkbenchPage pag;
	private IFile ifile;
	private IEditorPart editor;
	private Thread processThread;

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
		// IFolder folder = proj.getFolder("src");
		// IFolder folder = proj.getFolder("bin");
		IPath pa = proj.getLocation();

		fi = pa.toFile();

		/* Get the file location as String! */
		String filLoc = ifile.getRawLocation().toString();

		/* Get the filename without extension! */
		name = ifile.getName().replaceFirst("[.][^.]+$", "");

		pa.toFile().getPath().replace("\\", "/");

		/* Get the parent directory! */
		// dir = new File(filLoc).getParentFile().getPath().replace("\\", "/");
		if (editor instanceof CompilationUnitEditor) {
			dir = pa.toFile().getPath().replace("\\", "/") + "/src";
		} else if (editor instanceof JavaEditor) {
			dir = pa.toFile().getPath().replace("\\", "/");
		} else {// Compilation triggered from the Flow or the popup menu with no
				// active editor opened!
			dir = pa.toFile().getPath().replace("\\", "/") + "/src";
		}

		// System.out.println("dir: " + dir);

		StartBio7Utils.getConsoleInstance().cons.clear();

		Job job = new Job("Compile And Run") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Compile And Run...", IProgressMonitor.UNKNOWN);
				// JavaSourceClassLoader.directCall = true;
				compileAndLoad(fi, dir, name, pag, false);
				// JavaSourceClassLoader.directCall = false;
				monitor.done();
				return Status.OK_STATUS;
			}

		};
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					BatchModel.resume();
				} else {
					// JavaSourceClassLoader.directCall = false;
				}
			}
		});

		job.schedule();
	}
    /*This method is called from above but also from the script menu actions directly (external Java path!)*/
	public void compileAndLoad(File path, String dir, String name, IWorkbenchPage pag, boolean startupScript) {
		FileRoot.setCurrentCompileDir(dir);
		JavaSourceClassLoader cla = new JavaSourceClassLoader(Bio7Plugin.class.getClassLoader());
		cla.pag = pag;
		
       // System.out.println(path.getAbsolutePath()+"/bin");
		cla.setSourcePath(new File[] {new File(dir) });
		cla.setBinaryPath(new File[] {new File(path.getAbsolutePath()+"/bin") });

		Object o = null;
		Class<?> cl = null;
		try {

			// JavaEditor editor=JavaEditor.javaEditor;
			/* If we have an opened Java Editor! */
			if (editor != null && (editor instanceof JavaEditor || editor instanceof CompilationUnitEditor)) {
				/*
				 * If the class is in a package we receive the package name from
				 * the AST!
				 */
				org.eclipse.jdt.core.dom.CompilationUnit compUnit = null;
				if (editor instanceof JavaEditor) {
					JavaEditor jedit = (JavaEditor) editor;
					/*
					 * If the class is in a package we receive the package name
					 * from the AST!
					 */
					compUnit = jedit.getCompUnit();
				}

				else if (editor instanceof CompilationUnitEditor) {
					CompilationUnitEditor jedit = (CompilationUnitEditor) editor;

					IWorkingCopyManager mgr = JavaUI.getWorkingCopyManager();
					ICompilationUnit cu = mgr.getWorkingCopy(jedit.getEditorInput());
					ASTParser parser = ASTParser.newParser(AST.JLS10);
					parser.setSource(cu);
					// CompilationUnit cu = (CompilationUnit)
					// parser.createAST(null);
					compUnit = (CompilationUnit) parser.createAST(null);

					/*
					 * If the class is in a package we receive the package name
					 * from the AST!
					 */

					// org.eclipse.jdt.internal.ui.javaeditor.ASTProvider.getASTProvider().getAST(input,
					// null, null);

				}
				
				/*PackageDeclaration pdecl = compUnit.getPackage();
				if (pdecl != null) {
					Name packName = pdecl.getName();

					String pack = packName.toString();
					//cl = cla.findClass(pack + "." + name);
					ITextEditor editor2 = (ITextEditor) editor;
					IDocumentProvider prov = editor2.getDocumentProvider();
					IDocument doc2 = prov.getDocument(editor2.getEditorInput());
					org.joor.Compile com=new org.joor.Compile();
					cl=com.compile(pack + "." + name, doc2.get(),null,Bio7Plugin.class.getClassLoader());

				} else {
					//cl = cla.findClass(name);
					ITextEditor editor2 = (ITextEditor) editor;
					IDocumentProvider prov = editor2.getDocumentProvider();
					IDocument doc2 = prov.getDocument(editor2.getEditorInput());
					org.joor.Compile com=new org.joor.Compile();
					cl=com.compile(name, doc2.get(),null,Bio7Plugin.class.getClassLoader());
				}
*/
				/*
				 * If the class is in a package we receive the package name from
				 * the AST!
				 */
				// org.eclipse.jdt.core.dom.CompilationUnit compUnit =
				// jedit.getCompUnit();
				
				
				
				PackageDeclaration pdecl = compUnit.getPackage();
				if (pdecl != null) {
					Name packName = pdecl.getName();

					String pack = packName.toString();
					cl = cla.findClass(pack + "." + name);

				} else {
					cl = cla.findClass(name);
				}

			}
			/* Compile startup scripts! */
			else if (startupScript) {
				org.eclipse.jdt.core.dom.CompilationUnit compUnit = null;
				Document doc = new Document(BatchModel.fileToString(path.getAbsolutePath()));

				ASTParser parser = ASTParser.newParser(AST.JLS10);
				parser.setSource(doc.get().toCharArray());
				// CompilationUnit cu = (CompilationUnit)
				// parser.createAST(null);
				compUnit = (CompilationUnit) parser.createAST(null);
				PackageDeclaration pdecl = compUnit.getPackage();
				if (pdecl != null) {
					Name packName = pdecl.getName();

					String pack = packName.toString();
					cl = cla.findClass(pack + "." + name);

				} else {
					cl = cla.findClass(name);
				}

			}
			/*
			 * If we compile from the context menu or the Flow editor we create
			 * the AST from the file!
			 */
			else {
				org.eclipse.jdt.core.dom.CompilationUnit compUnit = null;
				Document doc = new Document(BatchModel.fileToString(ifile.getRawLocation().toString()));

				ASTParser parser = ASTParser.newParser(AST.JLS8);
				parser.setSource(doc.get().toCharArray());
				// CompilationUnit cu = (CompilationUnit)
				// parser.createAST(null);
				compUnit = (CompilationUnit) parser.createAST(null);
				PackageDeclaration pdecl = compUnit.getPackage();
				if (pdecl != null) {
					Name packName = pdecl.getName();

					String pack = packName.toString();
					cl = cla.findClass(pack + "." + name);

				} else {
					cl = cla.findClass(name);
				}

			}

			// System.out.println(name);
			o = cl.newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			System.out.println(e.getMessage());
		}
		if (o != null) {
			if (o instanceof Model) {
				Model model = (Model) o;
				Compiled.setModel(model);
				/* For Java WorldWind! */
				DynamicLayer.setEcoclass(model);
			}

			else if (o instanceof PlugIn) {
				callPlugin(cl);

			} else if (o instanceof PlugInFilter) {
				callPluginFilter(cl);

			}

			else {
				Method method = null;
				try {
					method = cl.getMethod("main", String[].class);

				} catch (NoSuchMethodException | SecurityException e) {
					// TODO Auto-generated catch block
					// e.printStackTrace();
					System.out.println("No main method! Only class compiled and loaded!");
				}

				if (method != null) {

					callMainMethod(method);

				}
			}
		} else {
			System.out.println("Object not created! Null reference!");
		}
		cla = null;
		cl = null;
		o = null;
	}

	private void callPlugin(final Class<?> cl) {
		/*SwingUtilities.invokeLater(new Runnable() {
			// !!
			public void run() {*/
				try {

					((PlugIn) cl.newInstance()).run("");
				} catch (InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			/*}
		});*/

	}

	private void callPluginFilter(final Class<?> cl) {
		/*SwingUtilities.invokeLater(new Runnable() {
			// !!
			public void run() {*/
				// System.out.println("run plugin");
				try {
					new PlugInFilterRunner(cl.newInstance(), "plugin", "");
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			/*}
		});*/
	}

	private void callMainMethod(Method method) {

		Object retVal;
		/**/
		try {
			String[] params = {""};
			method.invoke(null, (Object) params);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
