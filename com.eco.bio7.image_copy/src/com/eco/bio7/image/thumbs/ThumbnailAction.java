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

package com.eco.bio7.image.thumbs;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.prefs.Preferences;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class ThumbnailAction extends Action {

	private Preferences prefs;

	private ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/images/open.gif")));

	private LoadThumbnailsJob ab;

	protected boolean doBreak;

	private File directory;

	private File root;

	private static boolean startNewJob = true;

	private String[] extensions = { "jpeg", "JPG", "jpg", "png", "tif", "TIF", "TIFF", "gif", "avi", "zip", "bmp", "roi", "dcm", "txt" };

	private static boolean recursive = false;

	private ArrayList<File> fil;

	protected File[] filList;

	private Collection filesColl;

	public ThumbnailAction() {
		super("Open");
		setId("Thumbnails");
		setToolTipText("Open Directory");

		// setImageDescriptor(desc);

	}

	public void run() {
		final Text text = new Text(new Shell(), SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 4;
		text.setLayoutData(data);

		DirectoryDialog dia = new DirectoryDialog(new Shell(SWT.NONE), SWT.NULL);
		dia.setMessage("Choose a directory");
		prefs = Preferences.userNodeForPackage(this.getClass());
		String lastOutputDir = prefs.get("LAST_OUTPUT_DIR", "");
		dia.setFilterPath(lastOutputDir);
		String save = dia.open();
		if (save != null) {
		prefs.put("LAST_OUTPUT_DIR", save);

		

			directory = new java.io.File(save);

			if (recursive) {

				Job job = new Job("List all files...") {

					protected IStatus run(IProgressMonitor monitor) {
						monitor.beginTask("List all files ...", IProgressMonitor.UNKNOWN);
						if (startNewJob) {
							startNewJob = false;
							jobDisabled();
							File[] fi=ListFilesRecursiveDirectory(directory);
							startThumbJob(fi);
							fi=null;
						}

						monitor.done();
						return Status.OK_STATUS;
					}

				};

				//job.setUser(true);
				job.schedule();
			} else {
				startThumbJob(ListFileDirectory(directory));
			}
		}

	}

	private void startThumbJob(File[] filesRecursive) {
		ab = new LoadThumbnailsJob(filesRecursive);
		ab.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					jobEnable();
				} else {
					jobEnable();

				}

			}

		});
		ab.setUser(true);
		ab.schedule();
	}

	private void jobEnable() {
		startNewJob = true;
		ThumbnailsView view = ThumbnailsView.getThumb();
		ThumbnailsView.setThumbSizeSelectable(true);
		view.getSmall().setEnabled(true);
		view.getMedium().setEnabled(true);
		view.getLarge().setEnabled(true);
		view.getFive().setEnabled(true);
		view.getTen().setEnabled(true);
		view.getFifteen().setEnabled(true);
	}

	private void jobDisabled() {
		ThumbnailsView.setThumbSizeSelectable(false);
		ThumbnailsView view = ThumbnailsView.getThumb();
		view.getSmall().setEnabled(false);
		view.getMedium().setEnabled(false);
		view.getLarge().setEnabled(false);
		view.getFive().setEnabled(false);
		view.getTen().setEnabled(false);
		view.getFifteen().setEnabled(false);
	}

	public void dispose() {

	}

	public Menu getMenu(Menu parent) {

		return null;
	}

	public File[] ListFileDirectory(File filedirectory) {
		File dir = filedirectory;

		String[] children = dir.list();
		if (children == null) {

		} else {
			for (int i = 0; i < children.length; i++) {
				// Get filename of file or directory
				String filename = children[i];
			}
		}

		// Filter the extension
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".jpg") || name.endsWith(".JPG") || name.endsWith(".TIFF")
						|| name.endsWith(".tif") || name.endsWith(".TIF") || name.endsWith(".zip") || name.endsWith(".pnm") || name.endsWith(".dcm") || name.endsWith(".txt") || name.endsWith(".roi") || name
						.endsWith(".avi"));
			}
		};

		File[] files = dir.listFiles(filter);

		return files;
	}

	/* Recursively lists all Files in directories and sub-directories! */
	public File[] ListFilesRecursiveDirectory(File filedirectory) {
		root = filedirectory;
		fil = new ArrayList<File>();

		filesColl = FileUtils.listFiles(root, extensions, recursive);
		if (filesColl.size() > 5000) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					restrictNumberOfThumbs(filesColl);
				}
			});
		} else {
			for (Iterator iterator = filesColl.iterator(); iterator.hasNext();) {

				File file = (File) iterator.next();
				fil.add(file);
			}
			filList = new File[fil.size()];
			for (int i = 0; i < fil.size(); i++) {
				filList[i] = fil.get(i);
			}
		}
		filesColl=null;
		fil.clear();
		return filList;
	}

	private void restrictNumberOfThumbs(Collection filesColl) {
		MessageBox message = new MessageBox(new Shell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
		message.setMessage("The number of files exceeds the recommended amount of files\n" +
				"for the thumbnail browser\n" +
				"Only the first 5000 images will be previewed!");
		message.setText("Bio7");
		int response = message.open();
		int count = 1;
		if (response == SWT.YES) {
			for (Iterator iterator = filesColl.iterator(); iterator.hasNext();) {

				File file = (File) iterator.next();
				if (count <= 5000) {
					fil.add(file);
					count++;
				}
			}
			filList = new File[fil.size()];
			for (int i = 0; i < fil.size(); i++) {
				filList[i] = fil.get(i);
			}

		} else {
			for (Iterator iterator = filesColl.iterator(); iterator.hasNext();) {

				File file = (File) iterator.next();
				fil.add(file);
			}
			filList = new File[fil.size()];
			for (int i = 0; i < fil.size(); i++) {
				filList[i] = fil.get(i);
			}

		}
	}

	public static boolean isStartNewJob() {
		return startNewJob;
	}

	public static void setStartNewJob(boolean startNewJob) {
		ThumbnailAction.startNewJob = startNewJob;
	}

	public static boolean isRecursive() {
		return recursive;
	}

	public static void setRecursive(boolean recursive) {
		ThumbnailAction.recursive = recursive;
	}

}