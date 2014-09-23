package com.eco.bio7.popup.actions;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import ij.Menus;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.tools.ant.Project;
import org.codehaus.groovy.ant.Groovyc;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

import com.eco.bio7.batch.BatchModel;
import com.eco.bio7.compile.Compile;
import com.eco.bio7.compile.BeanShellInterpreter;
import com.eco.bio7.methods.Compiled;

public class CompileGroovyAction implements IObjectActionDelegate {

	private String pathBundle;
	private String pathBundle2;
	private Class<?> clazz;
	private GroovyObject go;

	public CompileGroovyAction() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {
		String project = null;
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		IStructuredSelection strucSelection = null;
		if (selection instanceof IStructuredSelection) {
			strucSelection = (IStructuredSelection) selection;
			if (strucSelection.size() == 0) {

			} else if (strucSelection.size() == 1) {
				String nameofiofile;
				Object selectedObj = strucSelection.getFirstElement();
				if (selectedObj instanceof IFile) {
					IFile selectedFile = (IFile) selectedObj;
					nameofiofile = getFileName(selectedFile.getName());
					project = selectedFile.getLocation().toString();
					File file = selectedFile.getLocation().toFile();
					Bundle bundle = Platform.getBundle("com.eco.bio7.image");
					URL locationUrl = FileLocator.find(bundle, new Path("/"), null);
					URL fileUrl = null;
					try {
						fileUrl = FileLocator.toFileURL(locationUrl);
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					pathBundle = fileUrl.getFile();

					Bundle bundle2 = Platform.getBundle("com.eco.bio7");
					URL locationUrl2 = FileLocator.find(bundle2, new Path("/"), null);
					URL fileUrl2 = null;
					try {
						fileUrl2 = FileLocator.toFileURL(locationUrl2);
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
					pathBundle2 = fileUrl2.getFile();
					String classpath = pathBundle2 + "/bin" + File.pathSeparator + pathBundle + "/bin" + File.pathSeparator + getClassPath(file.toString());

					
					
					String scriptLocation = file.toString();
					File scriptPath = new File(file.getParent());

					// Configure
					CompilerConfiguration conf = new CompilerConfiguration();
					conf.setTargetDirectory(scriptPath.getPath());
					conf.setClasspath(classpath);

					GroovyClassLoader gcl = new GroovyClassLoader();
					CompilationUnit cu = new CompilationUnit(gcl);
					cu.configure(conf);
					cu.addSource(new File(scriptLocation));
					// Add more source files to the compilation unit if needed
					try {
						cu.compile();
					} catch (CompilationFailedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					 ClassLoader cl = Thread.currentThread().getContextClassLoader();  
					 URL[] urls=null;
					try {
						urls = new URL[]{scriptPath.toURL()};
					} catch (MalformedURLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}  
					 URLClassLoader ucl = new URLClassLoader(urls, cl);  
					 gcl = new GroovyClassLoader(ucl, conf);  
					 try {
						 //System.out.println(nameofiofile);
						 clazz = gcl.loadClass(nameofiofile);
					} catch (ClassNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}  
					try {
						 go = (GroovyObject)clazz.newInstance();
					} catch (InstantiationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
					  go.invokeMethod("run", null);  

				}
			}

		}

	}

	public String getClassPath(String path) {
		// long start = System.currentTimeMillis();
		StringBuffer sb = new StringBuffer();
		sb.append(System.getProperty("java.class.path"));
		File f = new File(path);
		if (f != null) // add directory containing file to classpath
			sb.append(File.pathSeparator + f.getParent());
		String pluginsDir = Menus.getPlugInsPath();
		// if (pluginsDir != null)
		// addJars(pluginsDir, sb);
		return pluginsDir.toString();
	}

	public static String getFileName(String path) {

		String fileName = null;
		String separator = File.separator;

		int pos = path.lastIndexOf(separator);
		int pos2 = path.lastIndexOf(".");

		if (pos2 > -1)
			fileName = path.substring(pos + 1, pos2);
		else
			fileName = path.substring(pos + 1);

		return fileName;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

}
