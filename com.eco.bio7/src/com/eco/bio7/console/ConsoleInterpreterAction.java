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

package com.eco.bio7.console;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IOConsole;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.actions.InterpretPython;
import com.eco.bio7.popup.actions.RunJavaClassFile;
import com.eco.bio7.rbridge.RConnectionJob;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.worldwind.WorldWindView;

public class ConsoleInterpreterAction extends Action implements IMenuCreator {

	private Menu fMenu;
	private ConsolePageParticipant participant;
	private static ConsoleInterpreterAction instance;

	public static ConsoleInterpreterAction getInstance() {
		return instance;
	}

	public ConsoleInterpreterAction(ConsolePageParticipant participant) {
		setId("Interpreter_Console");
		setToolTipText("Interpreter and Shell");
		setText("Console");
		this.participant = participant;
		setMenuCreator(this);
		instance = this;
	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);

		MenuItem menuItem1 = new MenuItem(fMenu, SWT.PUSH);
		menuItem1.setText("Jython");

		menuItem1.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				exitShellProcess();
				setConsoleColor();
				IOConsole ioConsole = participant.getIoc();
				participant.interpreterSelection = "jython";
				ioConsole.clearConsole();
				participant.ignore = true;
				ioConsole.getInputStream().appendData(System.getProperty("line.separator"));

				participant.styledText.setEditable(true);
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		MenuItem menuItem2 = new MenuItem(fMenu, SWT.PUSH);
		menuItem2.setText("Groovy");

		menuItem2.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				exitShellProcess();
				setConsoleColor();
				IOConsole ioConsole = participant.getIoc();
				participant.interpreterSelection = "groovy";
				ioConsole.clearConsole();
				participant.ignore = true;
				ioConsole.getInputStream().appendData(System.getProperty("line.separator"));
				participant.styledText.setEditable(true);
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		MenuItem menuItem3 = new MenuItem(fMenu, SWT.PUSH);
		menuItem3.setText("BeanShell");

		menuItem3.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				exitShellProcess();
				setConsoleColor();
				IOConsole ioConsole = participant.getIoc();
				participant.interpreterSelection = "beanshell";

				ioConsole.clearConsole();

				participant.ignore = true;
				ioConsole.getInputStream().appendData(System.getProperty("line.separator"));
				participant.styledText.setEditable(true);
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		new MenuItem(fMenu, SWT.SEPARATOR);
		MenuItem menuItem4 = new MenuItem(fMenu, SWT.PUSH);
		menuItem4.setText("Shell");

		menuItem4.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				startShell();

			}

			

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		new MenuItem(fMenu, SWT.SEPARATOR);
		MenuItem menuItem41 = new MenuItem(fMenu, SWT.PUSH);
		menuItem41.setText("Native Python");

		menuItem41.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				startPython();
			}

			
			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		MenuItem menuItem42 = new MenuItem(fMenu, SWT.PUSH);
		menuItem42.setText("Native R");

		menuItem42.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				startR();
			}

			

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		new MenuItem(fMenu, SWT.SEPARATOR);
		MenuItem menuItem5 = new MenuItem(fMenu, SWT.PUSH);
		menuItem5.setText("Console Output");

		menuItem5.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				exitShellProcess();
				setConsoleColor();
				IOConsole ioConsole = participant.getIoc();
				participant.interpreterSelection = "-";
				ioConsole.clearConsole();
				participant.ignore = true;
				ioConsole.getInputStream().appendData(System.getProperty("line.separator"));

				participant.styledText.setEditable(true);
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		return fMenu;
	}

	public void dispose() {

	}

	public Menu getMenu(Menu parent) {

		return null;
	}

	private void setConsoleColor() {
		StyledText styledText = (StyledText) participant.page.getControl();
		Color colb = new Color(Display.getCurrent(), 255, 255, 255);
		Color colf = new Color(Display.getCurrent(), 0, 0, 0);
		styledText.setBackground(colb);
		styledText.setForeground(colf);
	}

	private void exitShellProcess() {
		if (participant.getShellProcess() != null) {
			participant.getShellProcess().destroy();
			if (participant.processThread != null) {
				participant.processThread.interrupt();
			}
		}
		Process jprocess = RunJavaClassFile.getJavaProcess();
		if (jprocess != null) {
			jprocess.destroy();
		}

	}

	public void javaProcess() {
		exitShellProcess();
		setConsoleColor();
		IOConsole ioConsole = participant.getIoc();
		participant.interpreterSelection = "java";
		ioConsole.clearConsole();
		participant.ignore = true;
		ioConsole.getInputStream().appendData(System.getProperty("line.separator"));

		participant.styledText.setEditable(true);
	}

	/*public void startRShell() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				StyledText styledText = (StyledText) participant.page.getControl();
				Color colb = new Color(Display.getCurrent(), 0, 0, 0);
				Color colf = new Color(Display.getCurrent(), 255, 255, 255);
				IOConsole ioConsole = participant.getIoc();
				styledText.setBackground(colb);
				styledText.setForeground(colf);

				participant.interpreterSelection = "R";
				ioConsole.clearConsole();
				 Exit an existing shell process! 
				// exitShellProcess();
				 Open a new shell process! 
				if (participant.RProcess == null) {
					participant.processRCommand();
				}

				participant.ignore = true;
				ioConsole.getInputStream().appendData(System.getProperty("line.separator"));

				participant.styledText.setEditable(true);
			}
		});
	}*/
	public void startR() {
		StyledText styledText = (StyledText) participant.page.getControl();
		Color colb = new Color(Display.getCurrent(), 0, 0, 0);
		Color colf = new Color(Display.getCurrent(), 255, 255, 255);
		IOConsole ioConsole = participant.getIoc();
		styledText.setBackground(colb);
		styledText.setForeground(colf);

		participant.interpreterSelection = "R";
		ioConsole.clearConsole();
		/*
		 * if (RServe.getConnection() != null) { try { RServe.getConnection().shutdown(); } catch (RserveException en) { // TODO Auto-generated catch block en.printStackTrace(); } RConnectionJob.setCanceled(true); RServe.getConnection().close(); RServe.setConnection(null);
		 * 
		 * WorldWindView.setRConnection(null); }
		 */
		/* Exit an existing shell process! */
		// exitShellProcess();
		/* Open a new shell process! */
		if (participant.getRProcess() == null) {
			participant.processRCommand();
		}

		participant.ignore = true;
		//ioConsole.getInputStream().appendData(System.getProperty("line.separator"));

		participant.styledText.setEditable(true);
		/*Add a short break for the linebreak of the prompt*/
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*Add a linebreak in R*/
		participant.pipeToRConsole("cat(\"\r\")");
	}
	public void startPython() {
		StyledText styledText = (StyledText) participant.page.getControl();
		Color colb = new Color(Display.getCurrent(), 0, 0, 0);
		Color colf = new Color(Display.getCurrent(), 255, 255, 255);
		IOConsole ioConsole = participant.getIoc();
		styledText.setBackground(colb);
		styledText.setForeground(colf);

		participant.interpreterSelection = "Python";
		ioConsole.clearConsole();
		/* Exit an existing shell process! */
		//exitShellProcess();
		/* Open a new shell process! */
		if (participant.getPythonProcess() == null) {
		participant.processPythonCommand();
		}

		participant.ignore = true;
		ioConsole.getInputStream().appendData(System.getProperty("line.separator"));

		participant.styledText.setEditable(true);
	}
	public void startShell() {
		StyledText styledText = (StyledText) participant.page.getControl();
		Color colb = new Color(Display.getCurrent(), 0, 0, 0);
		Color colf = new Color(Display.getCurrent(), 255, 255, 255);
		IOConsole ioConsole = participant.getIoc();
		styledText.setBackground(colb);
		styledText.setForeground(colf);

		participant.interpreterSelection = "shell";
		ioConsole.clearConsole();
		/* Exit an existing shell process! */
		//exitShellProcess();
		/* Open a new shell process if necessary! */
		if (participant.nativeShellProcess == null) {
		participant.processCommand();
		}

		participant.ignore = true;
		/*
		 * Not necessary because we send startup arguments!-> ioConsole.getInputStream().appendData(System.getProperty("line.separator"));
		 */
		participant.styledText.setEditable(true);
		/* Reset variable to start Blender if needed in the shell! */
		InterpretPython.setBlenderInteractive(false);
	}


}