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
import org.eclipse.ui.console.IOConsole;

import com.eco.bio7.actions.InterpretPython;
import com.eco.bio7.popup.actions.RunJavaClassFile;

public class ConsoleInterpreterAction extends Action implements IMenuCreator {

	private Menu fMenu;
	private ConsolePageParticipant participant;
    public static ConsoleInterpreterAction instance;
	public ConsoleInterpreterAction(ConsolePageParticipant participant) {
		setId("Interpreter_Console");
		setToolTipText("Interpreter and Shell");
		setText("Console");
		this.participant = participant;
		setMenuCreator(this);
		instance=this;
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
				StyledText styledText = (StyledText) participant.page.getControl();
				Color colb = new Color (Display.getCurrent(), 0, 0, 0);
				Color colf = new Color (Display.getCurrent(), 255, 255, 255);
				IOConsole ioConsole = participant.getIoc();
				styledText.setBackground(colb);
				styledText.setForeground(colf);
			    
				participant.interpreterSelection = "shell";
				ioConsole.clearConsole();
				/*Exit an existing shell process!*/
				exitShellProcess();
				/*Open a new shell process!*/
				participant.processCommand();
				
				participant.ignore = true;
				/*
				 * Not necessary because we send startup arguments!->
			     * ioConsole.getInputStream().appendData(System.getProperty("line.separator"));
			     * 
			     * */
				participant.styledText.setEditable(true);
				/*Reset variable to start Blender if needed in the shell!*/
				InterpretPython.setBlenderInteractive(false);
				
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		new MenuItem(fMenu, SWT.SEPARATOR);
		MenuItem menuItem41 = new MenuItem(fMenu, SWT.PUSH);
		menuItem41.setText("Native Python");

		menuItem41.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				StyledText styledText = (StyledText) participant.page.getControl();
				Color colb = new Color (Display.getCurrent(), 0, 0, 0);
				Color colf = new Color (Display.getCurrent(), 255, 255, 255);
				IOConsole ioConsole = participant.getIoc();
				styledText.setBackground(colb);
				styledText.setForeground(colf);
			    
				participant.interpreterSelection = "Python";
				ioConsole.clearConsole();
				/*Exit an existing shell process!*/
				exitShellProcess();
				/*Open a new shell process!*/
				participant.processPythonCommand();
				
				participant.ignore = true;
				ioConsole.getInputStream().appendData(System.getProperty("line.separator"));

				participant.styledText.setEditable(true);
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		MenuItem menuItem42 = new MenuItem(fMenu, SWT.PUSH);
		menuItem42.setText("Native R");

		menuItem42.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				StyledText styledText = (StyledText) participant.page.getControl();
				Color colb = new Color (Display.getCurrent(), 0, 0, 0);
				Color colf = new Color (Display.getCurrent(), 255, 255, 255);
				IOConsole ioConsole = participant.getIoc();
				styledText.setBackground(colb);
				styledText.setForeground(colf);
			    
				participant.interpreterSelection = "R";
				ioConsole.clearConsole();
				/*Exit an existing shell process!*/
				exitShellProcess();
				/*Open a new shell process!*/
				participant.processRCommand();
				
				participant.ignore = true;
				ioConsole.getInputStream().appendData(System.getProperty("line.separator"));

				participant.styledText.setEditable(true);
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

				participant.styledText.setEditable(false);
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
		Color colb = new Color (Display.getCurrent(), 255, 255, 255);
		Color colf = new Color (Display.getCurrent(), 0, 0, 0);
		styledText.setBackground(colb);
		styledText.setForeground(colf);
	}

	private void exitShellProcess() {
		if (participant.shellProcess != null) {
			participant.shellProcess.destroy();
			if (participant.processThread != null) {
				participant.processThread.interrupt();
			}
		}
		Process jprocess=RunJavaClassFile.getJavaProcess();
		if(jprocess!=null){
			jprocess.destroy();
		}
		
	}
	
	public void javaProcess(){
		exitShellProcess();
		setConsoleColor();
		IOConsole ioConsole = participant.getIoc();
		participant.interpreterSelection = "java";
		ioConsole.clearConsole();
		participant.ignore = true;
		ioConsole.getInputStream().appendData(System.getProperty("line.separator"));

		participant.styledText.setEditable(true);
	}
	public  void startRShell() {
		StyledText styledText = (StyledText) participant.page.getControl();
		Color colb = new Color (Display.getCurrent(), 0, 0, 0);
		Color colf = new Color (Display.getCurrent(), 255, 255, 255);
		IOConsole ioConsole = participant.getIoc();
		styledText.setBackground(colb);
		styledText.setForeground(colf);
		
		participant.interpreterSelection = "R";
		ioConsole.clearConsole();
		/*Exit an existing shell process!*/
		//exitShellProcess();
		/*Open a new shell process!*/
		participant.processRCommand();
		
		participant.ignore = true;
		ioConsole.getInputStream().appendData(System.getProperty("line.separator"));

		participant.styledText.setEditable(true);
	}

}