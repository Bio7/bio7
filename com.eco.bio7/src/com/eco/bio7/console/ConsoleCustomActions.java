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

package com.eco.bio7.console;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.Charset;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import com.eco.bio7.rbridge.backend.RBackend;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RBridge;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.scriptengines.ScriptEngineConnection;
import com.eco.bio7.util.Util;
import com.eco.bio7.worldwind.WorldWindView;

public class ConsoleCustomActions extends Action implements IMenuCreator {

	private Menu fMenu;
	private ConsolePageParticipant participant;
	private String[] encoding = { "UTF-8", "UTF-16", "CP850", "Big5", "Windows-1252", "Windows-1250", "Windows-1251",
			"Windows-1252", "Windows-1253", "Windows-1254", "Windows-1255", "Windows-1256", "Windows-1257",
			"Windows-1258" };

	public ConsoleCustomActions(ConsolePageParticipant participant) {
		setId("Interpreter_Console_Custom_Actions");
		setToolTipText("Custom Commands");
		setText("Options");
		this.participant = participant;
		setMenuCreator(this);
	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);

		MenuItem menuItem6 = new MenuItem(fMenu, SWT.CASCADE);
		menuItem6.setText("BeanShell Commands");
		final Menu bshScripts = new Menu(menuItem6);
		menuItem6.setMenu(bshScripts);
		MenuItem bshDel = new MenuItem(bshScripts, SWT.NONE);

		bshDel.setText("Delete Workspace");

		bshDel.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				ScriptEngine se = ScriptEngineConnection.getScriptingEngineBeanShell();
				try {
					se.eval("clear();");
				} catch (ScriptException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				Bio7Dialog.message("Workspace Deleted!");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		MenuItem menuItem7 = new MenuItem(fMenu, SWT.CASCADE);
		menuItem7.setText("Groovy Commands");
		final Menu groovyScripts = new Menu(menuItem7);
		menuItem7.setMenu(groovyScripts);
		MenuItem groovyDel = new MenuItem(groovyScripts, SWT.NONE);

		groovyDel.setText("Print Binding Variables");

		groovyDel.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				ScriptEngine se = ScriptEngineConnection.getScriptingEngineGroovy();
				try {
					se.eval("binding.variables.each { println it }");
				} catch (ScriptException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		new MenuItem(fMenu, SWT.SEPARATOR);
		MenuItem menuItem1 = new MenuItem(fMenu, SWT.PUSH);
		menuItem1.setText("Reinitialize BeanShell");

		menuItem1.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				ScriptEngineConnection.reinitializeBeanShell();
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		MenuItem menuItem2 = new MenuItem(fMenu, SWT.PUSH);
		menuItem2.setText("Reinitialize Groovy");

		menuItem2.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				ScriptEngineConnection.reinitializeGroovy();
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		MenuItem menuItem31 = new MenuItem(fMenu, SWT.PUSH);
		menuItem31.setText("Reinitialize JavaScript");

		menuItem31.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				ScriptEngineConnection.reinitializeJavaScript();
			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		new MenuItem(fMenu, SWT.SEPARATOR);

		/* Embedded R runs in the process of Bio7: it can't be stopped (only interrupted) */
		MenuItem menuItem4 = new MenuItem(fMenu, SWT.PUSH);
		menuItem4.setText("Stop Native Process (Python, Shell)");
		if ("R".equals(ConsolePageParticipant.getInterpreterSelection())) {
			menuItem4.setText("Stop Native Process (embedded R: restart Bio7 to restart R)");
			menuItem4.setEnabled(false);
		}

		menuItem4.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

				ConsolePageParticipant cpp = ConsolePageParticipant.getConsolePageParticipantInstance();
				String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
				new Thread() {
					@Override
					public void run() {

						if (selectionConsole.equals("shell")) {
							if (cpp.getNativeShellProcess() != null) {
								try {
									cpp.getNativeShellProcess().destroy();
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								cpp.setNativeShellProcess(null);
							}
						} else if (selectionConsole.equals("R")) {
							/* Embedded R runs in the process of Bio7 */
							com.eco.bio7.batch.Bio7Dialog.message(com.eco.bio7.rbridge.backend.Backends.CANNOT_TERMINATE);
						} else if (selectionConsole.equals("Python")) {
							if (cpp.getPythonProcess() != null) {
								try {
									cpp.getPythonProcess().destroy();
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								cpp.setPythonProcess(null);
							}

						}
					}
				}.start();

				Bio7Dialog.message("Process terminated!");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		new MenuItem(fMenu, SWT.SEPARATOR);
		MenuItem menuEncoding = new MenuItem(fMenu, SWT.CASCADE);
		menuEncoding.setText("Change Text Encoding");
		final Menu menuScripts = new Menu(menuEncoding);
		menuEncoding.setMenu(menuScripts);

		menuScripts.addMenuListener(new MenuListener() {

			public void menuHidden(MenuEvent e) {

			}

			@Override
			public void menuShown(MenuEvent e) {

				// plugins_ = new MenuItem[Menus.getPlugins().length];
				MenuItem[] menuItems = menuScripts.getItems();
				// Only delete the plugins menu items and menus!
				for (int i = 0; i < menuItems.length; i++) {
					if (menuItems[i] != null) {
						menuItems[i].dispose();
					}
				}

				MenuItem item1 = new MenuItem(menuScripts, SWT.NONE);
				final Charset cs = Charset.defaultCharset();
				item1.setText("System Encoding");

				item1.addSelectionListener(new SelectionListener() {

					public void widgetSelected(SelectionEvent e) {

						IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
						store.setValue("Console_Encoding", cs.displayName());
						Bio7Dialog.message("Encoding set to " + Charset.defaultCharset());
					}

					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});
				MenuItem item2 = new MenuItem(menuScripts, SWT.NONE);

				item2.setText("Reset Encoding");

				item2.addSelectionListener(new SelectionListener() {

					public void widgetSelected(SelectionEvent e) {

						IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
						String os = ApplicationWorkbenchWindowAdvisor.getOS();
						if (os.equals("Windows")) {
							store.setValue("Console_Encoding", "CP850");
							Bio7Dialog.message("Encoding set to CP850");
						} else if (os.equals("Linux")) {
							store.setValue("Console_Encoding", "UTF-8");
							Bio7Dialog.message("Encoding set to UTF-8");
						} else if (os.equals("Mac")) {
							store.setValue("Console_Encoding", "UTF-8");
							Bio7Dialog.message("Encoding set to UTF-8");
						}
					}

					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});
				MenuItem item3 = new MenuItem(menuScripts, SWT.NONE);

				item3.setText("Custom Encoding");

				item3.addSelectionListener(new SelectionListener() {

					public void widgetSelected(SelectionEvent e) {

						IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();

						InputDialog dlg = new InputDialog(Util.getShell(), "", "Enter Encoding",
								store.getString("Console_Encoding"), null);
						if (dlg.open() == Window.OK) {
							// User clicked OK; update the label with the input

							store.setValue("Console_Encoding", dlg.getValue());
							Bio7Dialog.message("Encoding set to " + dlg.getValue());

						}
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});
				for (int i = 0; i < encoding.length; i++) {

					final int count = i;

					MenuItem item = new MenuItem(menuScripts, SWT.NONE);

					item.setText(encoding[i]);

					item.addSelectionListener(new SelectionListener() {

						public void widgetSelected(SelectionEvent e) {

							IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
							store.setValue("Console_Encoding", encoding[count]);
							Bio7Dialog.message("Encoding set to " + encoding[count]);
						}

						public void widgetDefaultSelected(SelectionEvent e) {

						}
					});

				}

			}
		});
		MenuItem itemSetFont = new MenuItem(fMenu, SWT.PUSH);
		itemSetFont.setText("Font Settings");

		itemSetFont.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

				IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
				FontData defaultFont = PreferenceConverter.getDefaultFontData(store, "Bio7ShellFonts");
				StyledText styledText = (StyledText) participant.page.getControl();
				FontDialog fd = new FontDialog(new Shell(), SWT.NONE);
				fd.setText("Select Font");
				fd.setRGB(new RGB(0, 0, 0));
				if (defaultFont != null) {
					fd.setFontList(new FontData[] { defaultFont });
				}
				FontData newFont = fd.open();
				if (newFont == null)
					return;
				styledText.setFont(new Font(Display.getDefault(), newFont));
				PreferenceConverter.setValue(store, "Bio7ShellFonts", newFont);

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
}
