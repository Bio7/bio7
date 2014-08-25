package com.eco.bio7.rcp;
import javax.annotation.PostConstruct;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.internal.UISynchronizer;

import com.eco.bio7.compile.utils.ScanClassPath;

public class MyToolControl implements IProgressMonitor {
  private ProgressBar progressBar;

  UISynchronizer sync;

private IProject project;
  
  public MyToolControl(IProject proj){
	  this.project=proj;
  }
  
  @PostConstruct
  public void createControls(Composite parent) {
    progressBar = new ProgressBar(parent, SWT.SMOOTH);
    progressBar.setBounds(100, 10, 200, 20);
  }

  @Override
  public void worked(final int work) {
    sync.syncExec(new Runnable() {
      @Override
      public void run() {
        System.out.println("Worked");
        progressBar.setSelection(progressBar.getSelection() + work);
      }
    });
  }

  @Override
  public void subTask(String name) {

  }

  @Override
  public void setTaskName(String name) {

  }

  @Override
  public void setCanceled(boolean value) {

  }

  @Override
  public boolean isCanceled() {
    return false;
  }

  @Override
  public void internalWorked(double work) {
  }

  @Override
  public void done() {
	
	  IJavaProject javaProject = JavaCore.create(project);

		try {
			IClasspathEntry[] entry = javaProject.getRawClasspath();
			for (int i = 0; i < entry.length; i++) {
				System.out.println(entry[i]);
			}

		} catch (JavaModelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		IFolder sourceFolder = project.getFolder("src");
		IPackageFragmentRoot fragRoot = javaProject.getPackageFragmentRoot(sourceFolder);
		
		
		IClasspathEntry[] newEntries = new ScanClassPath().scanForJDT();
		
		newEntries[0] = JavaCore.newSourceEntry(fragRoot.getPath());
		
		try {
			javaProject.setRawClasspath(newEntries, null);
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

  }

  @Override
  public void beginTask(String name, int totalWork) {
   
    System.out.println("Starting");
  }
} 