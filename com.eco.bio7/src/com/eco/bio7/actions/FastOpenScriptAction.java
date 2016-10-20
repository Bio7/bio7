/*******************************************************************************
 * Copyright (c) 2007-2014 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.actions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorPart;
/*import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;



public class FastOpenScriptAction extends Action implements IMenuCreator {
	private Menu fMenu;

	public FastOpenScriptAction(String text) {
		super(text, AS_DROP_DOWN_MENU);

		setId("com.eco.bio7.actions.FastScript");
		//setActionDefinitionId("com.eco.bio7.actions.FastScript");
		setMenuCreator(this);
	}

	@Override
	public void run() {

		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IStorage storage = new StringStorage("#RScript\n");
		IStorageEditorInput input = new StringInput(storage);
		IWorkbenchPage page = window.getActivePage();
		if (page != null)
			try {
				page.openEditor(input, "org.eclipse.ui.DefaultTextEditor");
				//page.openEditor(input, "com.eco.bio7.reditors.TemplateEditor");
			} catch (PartInitException e) {

				e.printStackTrace();
			}
		
		 * if (part instanceof MultiPageEditor) { MultiPageEditor editor =
		 * (MultiPageEditor) part; SkeletonBuffer buf = new SkeletonBuffer(
		 * editor.editorController.getFxomDocument());
		 * buf.setFormat(FORMAT_TYPE.FULL);
		 * buf.setTextType(TEXT_TYPE.WITH_COMMENTS);
		 * 
		 * IWorkbenchWindow window = PlatformUI.getWorkbench()
		 * .getActiveWorkbenchWindow();
		 * 
		 * IStorage storage = new StringStorage(buf.toString());
		 * IStorageEditorInput input = new StringInput(storage); IWorkbenchPage
		 * page = window.getActivePage(); if (page != null) try {
		 * page.openEditor(input, "org.eclipse.ui.DefaultTextEditor"); } catch
		 * (PartInitException e) {
		 * 
		 * e.printStackTrace(); } }
		 
	}

	
	 * From: https://wiki.eclipse.org/
	 * FAQ_How_do_I_open_an_editor_on_something_that_is_not_a_file%3F
	 
	class StringStorage implements IStorage {
		private String string;

		StringStorage(String input) {
			this.string = input;
		}

		public InputStream getContents() throws CoreException {
			return new ByteArrayInputStream(string.getBytes());
		}

		public IPath getFullPath() {
			return null;
		}

		public Object getAdapter(Class adapter) {
			return null;
		}

		public String getName() {
			int len = Math.min(5, string.length());
			return "Script";
		}

		public boolean isReadOnly() {
			return false;
		}
	}

	class StringInput implements IStorageEditorInput {
		private IStorage storage;

		StringInput(IStorage storage) {
			this.storage = storage;
		}

		public boolean exists() {
			return true;
		}

		public ImageDescriptor getImageDescriptor() {
			return null;
		}

		public String getName() {
			return storage.getName();
		}

		public IPersistableElement getPersistable() {
			return null;
		}

		public IStorage getStorage() {
			return storage;
		}

		public String getToolTipText() {
			return "String-based file: " + storage.getName();
		}

		public Object getAdapter(Class adapter) {
			return null;
		}
	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);
		final Control parent2 = parent;

		MenuItem menuItem = new MenuItem(fMenu, SWT.PUSH);
		menuItem.setText("Script");

		menuItem.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});
		MenuItem menuItemFractal = new MenuItem(fMenu, SWT.PUSH);
		menuItemFractal.setText("Script2");

		menuItemFractal.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});

		return fMenu;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public Menu getMenu(Menu parent) {
		// TODO Auto-generated method stub
		return null;
	}

}
*/