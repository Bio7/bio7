/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     
 *     M.Austenfeld - Minor changes for the Bio7 application.
 *******************************************************************************/
package com.eco.bio7.reditors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.TextOperationAction;

import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.actions.OpenPreferences;
import com.eco.bio7.reditor.actions.SetComment;
import com.eco.bio7.reditor.actions.UnsetComment;

/**
 * 
 */
public class REditor extends TextEditor {

	private static final String TEMPLATE_PROPOSALS = "template_proposals_action";

	private RColorManager colorManager;

	private IPartListener partListener;

	/*public Action setplotmarkers;

	public Action deleteplotmarkers;*/

	public Action setcomment;

	private UnsetComment unsetcomment;

	private OpenPreferences preferences;

	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.reditor");
		
	}

	/**
	 * Creates a new template editor.
	 */
	public REditor() {
		super();
		
		colorManager = new RColorManager();
		setSourceViewerConfiguration(new RConfiguration(colorManager));

		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor != null) {

			IEditorSite site = editor.getEditorSite();
			
			IWorkbenchPage page = site.getPage();

			partListener = new IPartListener() {

				public void partActivated(IWorkbenchPart part) {

				}

				public void partBroughtToTop(IWorkbenchPart part) {

				}

				public void partClosed(IWorkbenchPart part) {

				}

				public void partDeactivated(IWorkbenchPart part) {

				}

				public void partOpened(IWorkbenchPart part) {

				}

			};
			page.addPartListener(partListener);
		}
		Bio7REditorPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {

			public void propertyChange(org.eclipse.jface.util.PropertyChangeEvent event) {

				Bio7REditorPlugin fginstance = Bio7REditorPlugin.getDefault();
				RCodeScanner scanner = (RCodeScanner) fginstance.getRCodeScanner();

				RColorProvider provider = Bio7REditorPlugin.getDefault().getRColorProvider();
				IPreferenceStore store = Bio7REditorPlugin.getDefault().getPreferenceStore();
				RGB rgbkey = PreferenceConverter.getColor(store, "colourkey");
				RGB rgbkey1 = PreferenceConverter.getColor(store, "colourkey1");
				RGB rgbkey2 = PreferenceConverter.getColor(store, "colourkey2");
				RGB rgbkey3 = PreferenceConverter.getColor(store, "colourkey3");
				RGB rgbkey4 = PreferenceConverter.getColor(store, "colourkey4");
				RGB rgbkey5 = PreferenceConverter.getColor(store, "colourkey5");
				RGB rgbkey6 = PreferenceConverter.getColor(store, "colourkey6");
				RGB rgbkey7 = PreferenceConverter.getColor(store, "colourkey7");
				RGB rgbkey8 = PreferenceConverter.getColor(store, "colourkey8");
				
				FontData f=PreferenceConverter.getFontData(store, "colourkeyfont");
				FontData f1=PreferenceConverter.getFontData(store, "colourkeyfont1");
				FontData f2=PreferenceConverter.getFontData(store, "colourkeyfont2");
				FontData f3=PreferenceConverter.getFontData(store, "colourkeyfont3");
				FontData f4=PreferenceConverter.getFontData(store, "colourkeyfont4");
				FontData f5=PreferenceConverter.getFontData(store, "colourkeyfont5");
				FontData f6=PreferenceConverter.getFontData(store, "colourkeyfont6");
				FontData f7=PreferenceConverter.getFontData(store, "colourkeyfont7");
				FontData f8=PreferenceConverter.getFontData(store, "colourkeyfont8");
				
				

				scanner.keyword.setData(new TextAttribute(provider.getColor(rgbkey), null, 1,new Font(Display.getCurrent(),f)));
				scanner.type.setData(new TextAttribute(provider.getColor(rgbkey1),null, 1,new Font(Display.getCurrent(),f1)));
				scanner.string.setData(new TextAttribute(provider.getColor(rgbkey2),null, 1,new Font(Display.getCurrent(),f2)));
				scanner.comment.setData(new TextAttribute(provider.getColor(rgbkey3),null, 1,new Font(Display.getCurrent(),f3)));
				scanner.other.setData(new TextAttribute(provider.getColor(rgbkey4),null, 1,new Font(Display.getCurrent(),f4)));
				scanner.operators.setData(new TextAttribute(provider.getColor(rgbkey5),null, 1,new Font(Display.getCurrent(),f5)));
				scanner.braces.setData(new TextAttribute(provider.getColor(rgbkey6),null, 1,new Font(Display.getCurrent(),f6)));
				scanner.numbers.setData(new TextAttribute(provider.getColor(rgbkey7),null, 1,new Font(Display.getCurrent(),f7)));
				scanner.assignment.setData(new TextAttribute(provider.getColor(rgbkey8),null, 1,new Font(Display.getCurrent(),f8)));
				if (REditor.this != null) {
					if (REditor.this.getSourceViewer() != null) {
						REditor.this.getSourceViewer().invalidateTextPresentation();
					}
				}
			}
		});
	}

	public void dispose() {
		colorManager.dispose();
		super.dispose();

	}

	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		addAction(menu, "ContentAssistProposal");
		menu.add(new Separator());
		/*addAction(menu, "Add Plotmarker");
		addAction(menu, "Delete Plotmarker");*/
		menu.add(new Separator());
		addAction(menu, "Add Comment");
		addAction(menu, "Remove Comment");
		menu.add(new Separator());
		addAction(menu, "R Preferences");

	}

	protected void createActions() {
		super.createActions();

		IAction action = new TextOperationAction(TemplateMessages.getResourceBundle(), "Editor." + TEMPLATE_PROPOSALS + ".", //$NON-NLS-1$ //$NON-NLS-2$
				this, ISourceViewer.CONTENTASSIST_PROPOSALS);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction(TEMPLATE_PROPOSALS, action);
		markAsStateDependentAction(TEMPLATE_PROPOSALS, true);

		IAction a = new TextOperationAction(REditorMessages.getResourceBundle(), "ContentAssistProposal.", this, ISourceViewer.CONTENTASSIST_PROPOSALS); //$NON-NLS-1$
		a.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction("ContentAssistProposal", a);

		a = new TextOperationAction(REditorMessages.getResourceBundle(), "ContentAssistTip.", this, ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);
		a.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
		setAction("ContentAssistTip", a);

		/*setplotmarkers = new com.eco.bio7.reditor.actions.SetMarkers("Set Plotmarker", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Add Plotmarker", setplotmarkers);

		deleteplotmarkers = new com.eco.bio7.reditor.actions.DeletePlotMarkers("Delete Plotmarker", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Delete Plotmarker", deleteplotmarkers);*/

		setcomment = new com.eco.bio7.reditor.actions.SetComment("Add Comment", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Add Comment", setcomment);

		unsetcomment = new com.eco.bio7.reditor.actions.UnsetComment("Remove Comment", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Remove Comment", unsetcomment);
		
		preferences = new com.eco.bio7.reditor.actions.OpenPreferences();
		setAction("R Preferences", preferences);

	}

}
