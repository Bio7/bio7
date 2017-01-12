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

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
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
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
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
public class BeanshellEditor extends TextEditor implements IPropertyChangeListener {

	private static final String TEMPLATE_PROPOSALS = "template_proposals_action";

	private ColorManager colorManager;

	private IPartListener partListener;

	private SetComment setComment;

	private UnsetComment unsetComment;

	private ScriptFormatterAction javaFormat;

	private ScriptFormatterSelectAction javaSelectFormat;

	private RSourceConverter rsourceConverter;

	private OpenPreferences preferences;

	public final static String EDITOR_MATCHING_BRACKETS = "matchingBrackets";

	public final static String EDITOR_MATCHING_BRACKETS_COLOR = "matchingBracketsColor";
	
	final private ScopedPreferenceStore storeWorkbench = new ScopedPreferenceStore(new InstanceScope(), "org.eclipse.ui.workbench");

	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.beanshell");
	}

	/**
	 * Creates a new BeanShell editor.
	 */
	public BeanshellEditor() {
		super();
		//setKeyBindingScopes(new String[] { "com.eco.bio7.beanshell.editor.scope" });
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new BeanshellConfiguration(colorManager));
		
		/*IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor != null) {
			IEditorSite site = editor.getEditorSite();
			IWorkbenchPage page = site.getPage();
			// setPreferenceStore(BeanshellEditorPlugin.getDefault().getPreferenceStore());

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
		}*/
		
	}
	
	
	
	@Override
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {

		super.handlePreferenceStoreChanged(event);
		// invalidateText();
	}

	public void invalidateText() {
		if (BeanshellEditor.this != null) {
			if (BeanshellEditor.this.getSourceViewer() != null) {
				BeanshellEditor.this.getSourceViewer().invalidateTextPresentation();
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {

		handlePreferenceStoreChanged(event);
	}

	// Method from:
	// https://github.com/gkorland/Eclipse-Fonts/blob/master/Fonts/src/main/java/fonts/FontsControler.java
	public synchronized void increase() {

		updateIncreasedFont(1);
	}

	public synchronized void decrease() {

		updateIncreasedFont(-1);
	}

	public void updateIncreasedFont(float fontSize) {
		BeanshellEditorPlugin fginstance = BeanshellEditorPlugin.getDefault();
		ScriptCodeScanner scanner = (ScriptCodeScanner) fginstance.getScriptCodeScanner();
		IPreferenceStore store = BeanshellEditorPlugin.getDefault().getPreferenceStore();

		FontData f = PreferenceConverter.getFontData(store, "colourkeyfont");
		FontData f1 = PreferenceConverter.getFontData(store, "colourkeyfont1");
		FontData f2 = PreferenceConverter.getFontData(store, "colourkeyfont2");
		FontData f3 = PreferenceConverter.getFontData(store, "colourkeyfont3");
		FontData f4 = PreferenceConverter.getFontData(store, "colourkeyfont4");
		FontData f5 = PreferenceConverter.getFontData(store, "colourkeyfont5");
		FontData f6 = PreferenceConverter.getFontData(store, "colourkeyfont6");
		FontData f7 = PreferenceConverter.getFontData(store, "colourkeyfont7");
		//FontData f8 = PreferenceConverter.getFontData(store, "colourkeyfont8");

		/* Restrict the size! */
		if (f.getHeight() + Math.round(fontSize) < 2) {
			return;
		} else if (f1.getHeight() + Math.round(fontSize) < 2) {
			return;
		} else if (f2.getHeight() + Math.round(fontSize) < 2) {
			return;
		} else if (f3.getHeight() + Math.round(fontSize) < 2) {
			return;
		} else if (f4.getHeight() + Math.round(fontSize) < 2) {
			return;
		} else if (f5.getHeight() + Math.round(fontSize) < 2) {
			return;
		} else if (f6.getHeight() + Math.round(fontSize) < 2) {
			return;
		} else if (f7.getHeight() + Math.round(fontSize) < 2) {
			return;
		} /*else if (f8.getHeight() + Math.round(fontSize) < 2) {
			return;
		}*/

		f.setHeight(f.getHeight() + Math.round(fontSize));
		f1.setHeight(f1.getHeight() + Math.round(fontSize));
		f2.setHeight(f2.getHeight() + Math.round(fontSize));
		f3.setHeight(f3.getHeight() + Math.round(fontSize));
		f4.setHeight(f4.getHeight() + Math.round(fontSize));
		f5.setHeight(f5.getHeight() + Math.round(fontSize));
		f6.setHeight(f6.getHeight() + Math.round(fontSize));
		f7.setHeight(f7.getHeight() + Math.round(fontSize));
		//f8.setHeight(f8.getHeight() + Math.round(fontSize));
		

		// Method from:
		// https://github.com/gkorland/Eclipse-Fonts/blob/master/Fonts/src/main/java/fonts/FontsControler.java
		String font = storeWorkbench.getString("com.eco.bio7.beanshelleditor.textfont");
		String[] split = font.split("\\|");

		split[2] = Float.toString(f.getHeight());
		StringBuilder builder = new StringBuilder(split[0]);
		for (int i = 1; i < split.length; ++i) {
			builder.append('|').append(split[i]);
		}
		storeWorkbench.setValue("com.eco.bio7.beanshelleditor.textfont", builder.toString());

		/* Invokes a property change! */
		PreferenceConverter.setValue(store, "colourkeyfont", f);
		PreferenceConverter.setValue(store, "colourkeyfont1", f1);
		PreferenceConverter.setValue(store, "colourkeyfont2", f2);
		PreferenceConverter.setValue(store, "colourkeyfont3", f3);
		PreferenceConverter.setValue(store, "colourkeyfont4", f4);
		PreferenceConverter.setValue(store, "colourkeyfont5", f5);
		PreferenceConverter.setValue(store, "colourkeyfont6", f6);
		PreferenceConverter.setValue(store, "colourkeyfont7", f7);
		

		invalidateText();

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
	
	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] { "com.eco.bio7.beanshell.editor.scope" });  
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

	protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {
		super.configureSourceViewerDecorationSupport(support);

		char[] matchChars = { '{', '}', '(', ')', '[', ']' }; // which brackets to match
		ICharacterPairMatcher matcher = new DefaultCharacterPairMatcher(matchChars, IDocumentExtension3.DEFAULT_PARTITIONING);
		support.setCharacterPairMatcher(matcher);
		support.setMatchingCharacterPainterPreferenceKeys(EDITOR_MATCHING_BRACKETS, EDITOR_MATCHING_BRACKETS_COLOR);

		// Enable bracket highlighting in the preference store
		IPreferenceStore store = getPreferenceStore();
		store.setDefault(EDITOR_MATCHING_BRACKETS, true);
		store.setDefault(EDITOR_MATCHING_BRACKETS_COLOR, "128,128,128");
	}

}
