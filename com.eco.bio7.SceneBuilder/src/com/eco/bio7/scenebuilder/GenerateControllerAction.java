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
package com.eco.bio7.scenebuilder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.scenebuilder.editor.MultiPageEditor;
import com.eco.bio7.scenebuilder.editor.SkeletonBuffer;
import com.eco.bio7.scenebuilder.editor.SkeletonBuffer.FORMAT_TYPE;
import com.eco.bio7.scenebuilder.editor.SkeletonBuffer.TEXT_TYPE;

public class GenerateControllerAction extends Action {

	public GenerateControllerAction(String text) {
		super(text);

		setId("com.eco.bio7.SceneBuilder.ControllerSkeleton");
		setActionDefinitionId("com.eco.bio7.SceneBuilder.ControllerSkeletonAction");

	}

	@Override
	public void run() {
		IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().getActiveEditor();
		if (part instanceof MultiPageEditor) {
			MultiPageEditor editor = (MultiPageEditor) part;
			SkeletonBuffer buf = new SkeletonBuffer(
					editor.editorController.getFxomDocument());
			buf.setFormat(FORMAT_TYPE.FULL);
			buf.setTextType(TEXT_TYPE.WITH_COMMENTS);
			// System.out.println(buf.toString());

			IWorkbenchWindow window = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow();

			IStorage storage = new StringStorage(buf.toString());
			IStorageEditorInput input = new StringInput(storage);
			IWorkbenchPage page = window.getActivePage();
			if (page != null)
				try {
					page.openEditor(input, "org.eclipse.ui.DefaultTextEditor");
				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}

	/*
	 * From: https://wiki.eclipse.org/
	 * FAQ_How_do_I_open_an_editor_on_something_that_is_not_a_file%3F
	 */
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
			return "Generated_Controller_Class";//string.substring(0, len).concat("..."); //$NON-NLS-1$
		}

		public boolean isReadOnly() {
			return true;
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

}
