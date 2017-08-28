/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.util;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.util.Util;

/**
 * This class provides some default dialogs for the Bio7 application.
 * 
 * @author Bio7
 * 
 */
public class Bio7Dialog {
	private static boolean decision = true;
	private static String dir = null;
	private static String file = null;
	private static String[] files = null;
	private static String fileName;
	private static String currentFilePath;
	private static String[] inout;

	/**
	 * Opens a Windows selection Dialog with the given text.
	 * 
	 * @param text
	 *            the text for the dialog.
	 */
	public static void selection(final String text) {
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {

				final Shell shell = new Shell(display, SWT.TITLE | SWT.CANCEL | SWT.ON_TOP);

				shell.setText("Bio7");

				shell.setSize(300, 200);
				shell.setLocation(400, 300);
				shell.setLayout(new FillLayout());

				Listener l = new Listener() {
					Point origin;

					public void handleEvent(Event e) {
						switch (e.type) {
						case SWT.MouseDown:
							origin = new Point(e.x, e.y);
							break;
						case SWT.MouseUp:
							origin = null;
							break;
						case SWT.MouseMove:
							if (origin != null) {
								Point p = display.map(shell, null, e.x, e.y);
								shell.setLocation(p.x - origin.x, p.y - origin.y);
							}
							break;
						}
					}
				};
				shell.addListener(SWT.MouseDown, l);
				shell.addListener(SWT.MouseUp, l);
				shell.addListener(SWT.MouseMove, l);

				Button b = new Button(shell, SWT.PUSH);
				b.setBackground(shell.getBackground());
				b.setText(text);
				b.pack();
				b.setLocation(70, 70);
				b.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						shell.close();
					}
				});
				shell.open();
				while (!shell.isDisposed()) {
					if (!display.readAndDispatch())
						display.sleep();
				}

			}
		});

	}

	/**
	 * Opens a Linux selection Dialog with the given text.
	 * 
	 * @param text
	 *            the text for the dialog.
	 */
	public static void linuxSelection(final String text) {
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {

				final Shell shell = new Shell(display, SWT.TITLE | SWT.CANCEL);

				shell.setText("Bio7");

				shell.setSize(300, 200);
				shell.setLocation(400, 300);
				shell.setLayout(new FillLayout());

				Listener l = new Listener() {
					Point origin;

					public void handleEvent(Event e) {
						switch (e.type) {
						case SWT.MouseDown:
							origin = new Point(e.x, e.y);
							break;
						case SWT.MouseUp:
							origin = null;
							break;
						case SWT.MouseMove:
							if (origin != null) {
								Point p = display.map(shell, null, e.x, e.y);
								shell.setLocation(p.x - origin.x, p.y - origin.y);
							}
							break;
						}
					}
				};
				shell.addListener(SWT.MouseDown, l);
				shell.addListener(SWT.MouseUp, l);
				shell.addListener(SWT.MouseMove, l);

				Button b = new Button(shell, SWT.PUSH);
				b.setBackground(shell.getBackground());
				b.setText(text);
				b.pack();
				b.setLocation(70, 70);
				b.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event e) {
						shell.close();
					}
				});
				shell.open();
				while (!shell.isDisposed()) {
					if (!display.readAndDispatch())
						display.sleep();
				}

			}
		});

	}

	/**
	 * Opens a decision Dialog with the given text. The decision dialog will set the
	 * variable in the Flow editor of the bio7 application to true or false
	 * depending on the decision. In addition the selected boolean will be returned.
	 * 
	 * @param text
	 *            the text for the dialog.
	 * @return a boolean value.
	 */
	public static boolean decision(final String text) {

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {

				MessageBox message = new MessageBox(Util.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
				message.setMessage(text);
				message.setText("Bio7");
				int response = message.open();
				if (response == SWT.YES) {
					decision = true;

				} else {
					decision = false;

				}

			}
		});

		if (decision) {

			return true;
		} else {

			return false;
		}

	}

	/**
	 * Opens a message Dialog with the given text.
	 * 
	 * @param text
	 *            the text for the dialog.
	 */
	public static void message(final String text) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				MessageBox messageBox = new MessageBox(Util.getShell(),

						SWT.ICON_WARNING);
				messageBox.setText("Info!");
				messageBox.setMessage(text);
				messageBox.open();
			}
		});
	}

	/**
	 * Opens a Directory Dialog with the given text.
	 * 
	 * @param text
	 *            the text for the dialog.
	 * 
	 * @return a string with the selected directory.
	 */
	public static String directory(final String text) {

		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				// Shell must be created with style SWT.NO_TRIM
				Shell shell = new Shell(display, SWT.NO_TRIM | SWT.ON_TOP);

				DirectoryDialog dlg = new DirectoryDialog(shell);

				dlg.setFilterPath("c:/");

				dlg.setText("Bio7");

				dlg.setMessage(text);

				dir = dlg.open();
				/* We print the path of the selected folder! */

			}
		});
		return dir;
	}

	/**
	 * Opens a file-open dialog.
	 * 
	 * @return a file path as a string from the file dialog.
	 */
	public static String openFile() {
		file = null;
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				Shell s = new Shell(SWT.ON_TOP);
				FileDialog fd = new FileDialog(s, SWT.OPEN);
				fd.setText("Load");

				String[] filterExt = { "*.*" };
				fd.setFilterExtensions(filterExt);
				file = fd.open();
			}
		});
		return file;
	}

	/**
	 * Opens a file-open dialog which displays the file with the given extensions.
	 * 
	 * @param extension
	 *            the extensions as a String array which should be displayed.
	 * @return a file path as a string from the file dialog.
	 */
	public static String openFile(final String[] extension) {
		file = null;
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				Shell s = new Shell(SWT.ON_TOP);
				FileDialog fd = new FileDialog(s, SWT.OPEN);
				fd.setText("Load");

				fd.setFilterExtensions(extension);
				file = fd.open();
			}
		});
		return file;
	}

	/**
	 * Opens a file-open dialog for multiple selections.
	 * 
	 * @return a String array with the file paths of the selected files.
	 */
	public static String[] openMultipleFiles() {

		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				Shell shell = new Shell(display);
				FileDialog dlg = new FileDialog(shell, SWT.MULTI);
				dlg.setFilterPath(null);
				String f = dlg.open();
				if (f != null) {

					files = dlg.getFileNames();
					currentFilePath = dlg.getFilterPath();
				}

			}
		});

		return files;
	}

	/**
	 * A method which returns the current path for the multiple file dialog.
	 * 
	 * @return the system path as a String for the multiple file dialog.
	 */
	public static String getCurrentPath() {
		return currentFilePath;
	}

	/**
	 * Opens a file-save dialog.
	 * 
	 * @return a file path as a string from the file dialog.
	 */

	public static String saveFile() {
		file = null;
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				Shell s = new Shell(SWT.ON_TOP);
				FileDialog fd = new FileDialog(s, SWT.SAVE);
				fd.setText("Save");

				file = fd.open();
			}
		});
		return file;
	}

	/**
	 * Opens a file-save dialog and opens a confirmation dialog if the file exists.
	 * 
	 * @param arg
	 *            the extension of the file.
	 * @return a file path as a string from the file dialog.
	 */
	public static String saveFile(String arg) {

		final String[] FILTER_EXTS = { arg };
		fileName = null;
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				Shell s = new Shell(SWT.ON_TOP);
				FileDialog fd = new FileDialog(s, SWT.SAVE);
				fd.setText("Save");
				// fd.setFilterNames(FILTER_NAMES);
				fd.setFilterExtensions(FILTER_EXTS);
				fd.setOverwrite(true);

				fileName = fd.open();
			}
		});

		return fileName;
	}

	

	

	/**
	 * A method to open a text input dialog.
	 * 
	 * @param text
	 *            the title
	 * @param text1
	 *            a String.
	 * @param text2
	 *            a String.
	 * @return a String value.
	 */
	public String inputDialog(String text, String text1, String text2) {
		InputDialog inp = new InputDialog(Util.getShell(), text, text1, text2, null);
		String theInput = "";
		if (inp.open() == Dialog.OK) {

			theInput = inp.getValue();

		}
		return theInput;
	}

}
