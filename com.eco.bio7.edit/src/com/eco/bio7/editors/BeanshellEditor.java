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
 *      M.Austenfeld - Minor changes for the Bio7 application
 *******************************************************************************/
package com.eco.bio7.editors;

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

import com.eco.bio7.beanshelleditor.actions.OpenPreferences;
import com.eco.bio7.beanshelleditor.actions.ScriptFormatterAction;
import com.eco.bio7.beanshelleditor.actions.ScriptFormatterSelectAction;
import com.eco.bio7.beanshelleditor.actions.RSourceConverter;
import com.eco.bio7.beanshelleditor.actions.SetComment;
import com.eco.bio7.beanshelleditor.actions.UnsetComment;
import com.eco.bio7.editor.BeanshellEditorPlugin;

/**
 * 
 */
public class BeanshellEditor extends TextEditor {

	private static final String TEMPLATE_PROPOSALS = "template_proposals_action";

	private ColorManager colorManager;

	private IPartListener partListener;

	private SetComment setComment;

	private UnsetComment unsetComment;

	private ScriptFormatterAction javaFormat;

	private ScriptFormatterSelectAction javaSelectFormat;

	private RSourceConverter rsourceConverter;

	private OpenPreferences preferences;

	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.beanshell");
	}

	/**
	 * Creates a new BeanShell editor.
	 */
	public BeanshellEditor() {
		super();
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new BeanshellConfiguration(colorManager));

		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor != null) {
			IEditorSite site = editor.getEditorSite();
			IWorkbenchPage page = site.getPage();
			//setPreferenceStore(BeanshellEditorPlugin.getDefault().getPreferenceStore());

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
		BeanshellEditorPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {

			public void propertyChange(org.eclipse.jface.util.PropertyChangeEvent event) {

				BeanshellEditorPlugin fginstance = BeanshellEditorPlugin.getDefault();
				ScriptCodeScanner scanner = (ScriptCodeScanner) fginstance.getScriptCodeScanner();

				ScriptColorProvider provider = BeanshellEditorPlugin.getDefault().getScriptColorProvider();
				IPreferenceStore store = BeanshellEditorPlugin.getDefault().getPreferenceStore();
				RGB rgbkey = PreferenceConverter.getColor(store, "colourkey");
				RGB rgbkey1 = PreferenceConverter.getColor(store, "colourkey1");
				RGB rgbkey2 = PreferenceConverter.getColor(store, "colourkey2");
				RGB rgbkey3 = PreferenceConverter.getColor(store, "colourkey3");
				RGB rgbkey4 = PreferenceConverter.getColor(store, "colourkey4");
				RGB rgbkey5 = PreferenceConverter.getColor(store, "colourkey5");
				RGB rgbkey6 = PreferenceConverter.getColor(store, "colourkey6");
				RGB rgbkey7 = PreferenceConverter.getColor(store, "colourkey7");
				//RGB rgbkey8 = PreferenceConverter.getColor(store, "colourkey8");
				
				FontData f=PreferenceConverter.getFontData(store, "colourkeyfont");
				FontData f1=PreferenceConverter.getFontData(store, "colourkeyfont1");
				FontData f2=PreferenceConverter.getFontData(store, "colourkeyfont2");
				FontData f3=PreferenceConverter.getFontData(store, "colourkeyfont3");
				FontData f4=PreferenceConverter.getFontData(store, "colourkeyfont4");
				FontData f5=PreferenceConverter.getFontData(store, "colourkeyfont5");
				FontData f6=PreferenceConverter.getFontData(store, "colourkeyfont6");
				FontData f7=PreferenceConverter.getFontData(store, "colourkeyfont7");
				//FontData f8=PreferenceConverter.getFontData(store, "colourkeyfont8");

				scanner.keyword.setData(new TextAttribute(provider.getColor(rgbkey),null, 1,new Font(Display.getCurrent(),f)));
				scanner.type.setData(new TextAttribute(provider.getColor(rgbkey1),null, 1,new Font(Display.getCurrent(),f1)));
				scanner.string.setData(new TextAttribute(provider.getColor(rgbkey2),null, 1,new Font(Display.getCurrent(),f2)));
				scanner.comment.setData(new TextAttribute(provider.getColor(rgbkey3),null, 1,new Font(Display.getCurrent(),f3)));
				scanner.other.setData(new TextAttribute(provider.getColor(rgbkey4),null, 1,new Font(Display.getCurrent(),f4)));
				scanner.operators.setData(new TextAttribute(provider.getColor(rgbkey5),null, 1,new Font(Display.getCurrent(),f5)));
				scanner.braces.setData(new TextAttribute(provider.getColor(rgbkey6),null, 1,new Font(Display.getCurrent(),f6)));
				scanner.numbers.setData(new TextAttribute(provider.getColor(rgbkey7),null, 1,new Font(Display.getCurrent(),f7)));
				//scanner.multiLineComment.setData(new TextAttribute(provider.getColor(rgbkey8),null, 1,new Font(Display.getCurrent(),f8)));
				
				if (BeanshellEditor.this != null) {
					if (BeanshellEditor.this.getSourceViewer() != null) {
						BeanshellEditor.this.getSourceViewer().invalidateTextPresentation();
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
		addAction(menu, "Add Block Comment");
		addAction(menu, "Remove Block Comment");
		menu.add(new Separator());
		addAction(menu, "Format Source");
		addAction(menu, "Format Selected Source");
		menu.add(new Separator());
		addAction(menu, "Convert R Code");
		menu.add(new Separator());
		addAction(menu, "Editor Preferences");
	}

	protected void createActions() {
		super.createActions();

		IAction action = new TextOperationAction(TemplateMessages.getResourceBundle(), "Editor." + TEMPLATE_PROPOSALS + ".", //$NON-NLS-1$ //$NON-NLS-2$
				this, ISourceViewer.CONTENTASSIST_PROPOSALS);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction(TEMPLATE_PROPOSALS, action);
		markAsStateDependentAction(TEMPLATE_PROPOSALS, true);

		// define the action
		IAction a = new TextOperationAction(ScriptEditorMessages.getResourceBundle(), "ContentAssistProposal.", this, ISourceViewer.CONTENTASSIST_PROPOSALS); //$NON-NLS-1$
		a.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction("ContentAssistProposal", a); //$NON-NLS-1$

		setComment = new com.eco.bio7.beanshelleditor.actions.SetComment("Add Block Comment", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Add Block Comment", setComment);

		unsetComment = new com.eco.bio7.beanshelleditor.actions.UnsetComment("Remove Block Comment", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Remove Block Comment", unsetComment);

		javaFormat = new com.eco.bio7.beanshelleditor.actions.ScriptFormatterAction();
		setAction("Format Source", javaFormat);

		javaSelectFormat = new com.eco.bio7.beanshelleditor.actions.ScriptFormatterSelectAction();
		setAction("Format Selected Source", javaSelectFormat);

		rsourceConverter = new com.eco.bio7.beanshelleditor.actions.RSourceConverter();
		setAction("Convert R Code", rsourceConverter);
		
		preferences = new com.eco.bio7.beanshelleditor.actions.OpenPreferences();
		setAction("Editor Preferences", preferences);
		
	}

}
