package com.eco.bio7.editors.python;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.ITextEditorExtension3;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.TextOperationAction;
import com.eco.bio7.pythonedit.PythonEditorPlugin;
import com.eco.bio7.pythoneditor.actions.OpenPreferences;
import com.eco.bio7.pythoneditor.actions.SetComment;
import com.eco.bio7.pythoneditor.actions.UnsetComment;
import com.eco.bio7.pythoneditors.ScriptCodeScanner;
import com.eco.bio7.pythoneditors.ScriptEditorMessages;
import com.eco.bio7.pythoneditors.TemplateMessages;


public class PythonEditor extends TextEditor implements IPropertyChangeListener {

	private ColorManager colorManager;
	private static final String TEMPLATE_PROPOSALS = "template_proposals_action";
	private SetComment setComment;
	private UnsetComment unsetComment;
	private OpenPreferences preferences;
	public final static String EDITOR_MATCHING_BRACKETS = "matchingBrackets";
	public final static String EDITOR_MATCHING_BRACKETS_COLOR = "matchingBracketsColor";

	final private ScopedPreferenceStore storeWorkbench = new ScopedPreferenceStore(new InstanceScope(), "org.eclipse.ui.workbench");

	public PythonEditor() {
		configureInsertMode(ITextEditorExtension3.SMART_INSERT, false);
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new PythonConfiguration(colorManager));
		setDocumentProvider(new PythonDocumentProvider());
		
		/*PythonEditorPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {

			public void propertyChange(org.eclipse.jface.util.PropertyChangeEvent event) {

				PythonEditorPlugin fginstance = PythonEditorPlugin.getDefault();
				ScriptCodeScanner scanner = (ScriptCodeScanner) fginstance.getScriptCodeScanner();

				PythonScriptColorProvider provider = PythonEditorPlugin.getDefault().getScriptColorProvider();
				IPreferenceStore store = PythonEditorPlugin.getDefault().getPreferenceStore();
				RGB rgbkey = PreferenceConverter.getColor(store, "colourkey");
				RGB rgbkey1 = PreferenceConverter.getColor(store, "colourkey1");
				RGB rgbkey2 = PreferenceConverter.getColor(store, "colourkey2");
				RGB rgbkey3 = PreferenceConverter.getColor(store, "colourkey3");
				RGB rgbkey4 = PreferenceConverter.getColor(store, "colourkey4");
				RGB rgbkey5 = PreferenceConverter.getColor(store, "colourkey5");
				//RGB rgbkey6 = PreferenceConverter.getColor(store, "colourkey6");
				RGB rgbkey7 = PreferenceConverter.getColor(store, "colourkey7");
				RGB rgbkey8 = PreferenceConverter.getColor(store, "colourkey8");
				RGB rgbkey9 = PreferenceConverter.getColor(store, "colourkey9");
				RGB rgbkey10 = PreferenceConverter.getColor(store, "colourkey10");
				//RGB rgbkey11 = PreferenceConverter.getColor(store, "colourkey11");
				RGB rgbkey12 = PreferenceConverter.getColor(store, "colourkey12");
				
				
				FontData f=PreferenceConverter.getFontData(store, "colourkeyfont");
				FontData f1=PreferenceConverter.getFontData(store, "colourkeyfont1");
				FontData f2=PreferenceConverter.getFontData(store, "colourkeyfont2");
				FontData f3=PreferenceConverter.getFontData(store, "colourkeyfont3");
				FontData f4=PreferenceConverter.getFontData(store, "colourkeyfont4");
				FontData f5=PreferenceConverter.getFontData(store, "colourkeyfont5");
				//FontData f6=PreferenceConverter.getFontData(store, "colourkeyfont6");
				FontData f7=PreferenceConverter.getFontData(store, "colourkeyfont7");
				FontData f8=PreferenceConverter.getFontData(store, "colourkeyfont8");
				FontData f9=PreferenceConverter.getFontData(store, "colourkeyfont9");
				FontData f10=PreferenceConverter.getFontData(store, "colourkeyfont10");
				//FontData f11=PreferenceConverter.getFontData(store, "colourkeyfont11");
				FontData f12=PreferenceConverter.getFontData(store, "colourkeyfont12");

				scanner.keyword.setData(new TextAttribute(provider.getColor(rgbkey),null, 1,new Font(Display.getCurrent(),f)));
				scanner.type.setData(new TextAttribute(provider.getColor(rgbkey1),null, 1,new Font(Display.getCurrent(),f1)));
				scanner.string.setData(new TextAttribute(provider.getColor(rgbkey2),null, 1,new Font(Display.getCurrent(),f2)));
				scanner.comment.setData(new TextAttribute(provider.getColor(rgbkey3),null, 1,new Font(Display.getCurrent(),f3)));
				scanner.defaultOther.setData(new TextAttribute(provider.getColor(rgbkey4),null, 1,new Font(Display.getCurrent(),f4)));
				scanner.operators.setData(new TextAttribute(provider.getColor(rgbkey5),null, 1,new Font(Display.getCurrent(),f5)));
				//scanner.braces.setData(new TextAttribute(provider.getColor(rgbkey6),null, 1,new Font(Display.getCurrent(),f6)));
				scanner.numbers.setData(new TextAttribute(provider.getColor(rgbkey7),null, 1,new Font(Display.getCurrent(),f7)));
				scanner.decorator.setData(new TextAttribute(provider.getColor(rgbkey8),null, 1,new Font(Display.getCurrent(),f8)));
				scanner.className.setData(new TextAttribute(provider.getColor(rgbkey9),null, 1,new Font(Display.getCurrent(),f9)));
				scanner.funcName.setData(new TextAttribute(provider.getColor(rgbkey10),null, 1,new Font(Display.getCurrent(),f10)));
				//scanner.self.setData(new TextAttribute(provider.getColor(rgbkey11),null, 1,new Font(Display.getCurrent(),f11)));
				scanner.parenthesis.setData(new TextAttribute(provider.getColor(rgbkey12),null, 1,new Font(Display.getCurrent(),f12)));
				
				//scanner.multiLineComment.setData(new TextAttribute(provider.getColor(rgbkey8),null, 1,new Font(Display.getCurrent(),f8)));
				
				if (PythonEditor.this != null) {
					if (PythonEditor.this.getSourceViewer() != null) {
						PythonEditor.this.getSourceViewer().invalidateTextPresentation();
					}
				}
			}
		});*/
		
		
		
		
	}
	@Override
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {

		super.handlePreferenceStoreChanged(event);
		// invalidateText();
	}

	public void invalidateText() {
		if (PythonEditor.this != null) {
			if (PythonEditor.this.getSourceViewer() != null) {
				PythonEditor.this.getSourceViewer().invalidateTextPresentation();
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
		PythonEditorPlugin fginstance = PythonEditorPlugin.getDefault();
		ScriptCodeScanner scanner = (ScriptCodeScanner) fginstance.getScriptCodeScanner();
		IPreferenceStore store = PythonEditorPlugin.getDefault().getPreferenceStore();

		FontData f=PreferenceConverter.getFontData(store, "colourkeyfont");
		FontData f1=PreferenceConverter.getFontData(store, "colourkeyfont1");
		FontData f2=PreferenceConverter.getFontData(store, "colourkeyfont2");
		FontData f3=PreferenceConverter.getFontData(store, "colourkeyfont3");
		FontData f4=PreferenceConverter.getFontData(store, "colourkeyfont4");
		FontData f5=PreferenceConverter.getFontData(store, "colourkeyfont5");
		//FontData f6=PreferenceConverter.getFontData(store, "colourkeyfont6");
		FontData f7=PreferenceConverter.getFontData(store, "colourkeyfont7");
		FontData f8=PreferenceConverter.getFontData(store, "colourkeyfont8");
		FontData f9=PreferenceConverter.getFontData(store, "colourkeyfont9");
		FontData f10=PreferenceConverter.getFontData(store, "colourkeyfont10");
		//FontData f11=PreferenceConverter.getFontData(store, "colourkeyfont11");
		FontData f12=PreferenceConverter.getFontData(store, "colourkeyfont12");

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
		/*} else if (f6.getHeight() + Math.round(fontSize) < 2) {
			return;*/
		} else if (f7.getHeight() + Math.round(fontSize) < 2) {
			return;
		} else if (f8.getHeight() + Math.round(fontSize) < 2) {
			return;
		}
		else if (f9.getHeight() + Math.round(fontSize) < 2) {
			return;
		}
		else if (f10.getHeight() + Math.round(fontSize) < 2) {
			return;
		}
		/*else if (f11.getHeight() + Math.round(fontSize) < 2) {
			return;
		}*/
		else if (f12.getHeight() + Math.round(fontSize) < 2) {
			return;
		}

		f.setHeight(f.getHeight() + Math.round(fontSize));
		f1.setHeight(f1.getHeight() + Math.round(fontSize));
		f2.setHeight(f2.getHeight() + Math.round(fontSize));
		f3.setHeight(f3.getHeight() + Math.round(fontSize));
		f4.setHeight(f4.getHeight() + Math.round(fontSize));
		f5.setHeight(f5.getHeight() + Math.round(fontSize));
		//f6.setHeight(f6.getHeight() + Math.round(fontSize));
		f7.setHeight(f7.getHeight() + Math.round(fontSize));
		f8.setHeight(f8.getHeight() + Math.round(fontSize));
		f9.setHeight(f9.getHeight() + Math.round(fontSize));
		f10.setHeight(f10.getHeight() + Math.round(fontSize));
		//f11.setHeight(f11.getHeight() + Math.round(fontSize));
		f12.setHeight(f12.getHeight() + Math.round(fontSize));
		

		// Method from:
		// https://github.com/gkorland/Eclipse-Fonts/blob/master/Fonts/src/main/java/fonts/FontsControler.java
		String font = storeWorkbench.getString("com.eco.bio7.pythoneditor.textfont");
		String[] split = font.split("\\|");

		split[2] = Float.toString(f.getHeight());
		StringBuilder builder = new StringBuilder(split[0]);
		for (int i = 1; i < split.length; ++i) {
			builder.append('|').append(split[i]);
		}
		storeWorkbench.setValue("com.eco.bio7.pythoneditor.textfont", builder.toString());

		/* Invokes a property change! */
		PreferenceConverter.setValue(store, "colourkeyfont", f);
		PreferenceConverter.setValue(store, "colourkeyfont1", f1);
		PreferenceConverter.setValue(store, "colourkeyfont2", f2);
		PreferenceConverter.setValue(store, "colourkeyfont3", f3);
		PreferenceConverter.setValue(store, "colourkeyfont4", f4);
		PreferenceConverter.setValue(store, "colourkeyfont5", f5);
		//PreferenceConverter.setValue(store, "colourkeyfont6", f6);
		PreferenceConverter.setValue(store, "colourkeyfont7", f7);
		PreferenceConverter.setValue(store, "colourkeyfont8", f8);
		PreferenceConverter.setValue(store, "colourkeyfont9", f9);
		PreferenceConverter.setValue(store, "colourkeyfont10", f10);
		//PreferenceConverter.setValue(store, "colourkeyfont11", f11);
		PreferenceConverter.setValue(store, "colourkeyfont12", f12);
		

		invalidateText();

	}
	
	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] { "com.eco.bio7.python.editor.scope" });  
	}
	
	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		addAction(menu, "ContentAssistProposal");
		menu.add(new Separator());
		addAction(menu, "Add Block Comment");
		addAction(menu, "Remove Block Comment");
		menu.add(new Separator());
		addAction(menu, "Jython Preferences");
		
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

		setComment = new com.eco.bio7.pythoneditor.actions.SetComment("Add Comment", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Add Block Comment", setComment);

		unsetComment = new com.eco.bio7.pythoneditor.actions.UnsetComment("Remove Comment", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Remove Block Comment", unsetComment);
		
		preferences = new com.eco.bio7.pythoneditor.actions.OpenPreferences();
		setAction("Jython Preferences", preferences);

		
	}
	
	protected void configureSourceViewerDecorationSupport (SourceViewerDecorationSupport support) {
		super.configureSourceViewerDecorationSupport(support);		
	 
		char[] matchChars = {'{','}','(', ')', '[', ']'}; //which brackets to match		
		ICharacterPairMatcher matcher = new DefaultCharacterPairMatcher(matchChars ,
				IDocumentExtension3.DEFAULT_PARTITIONING);
		support.setCharacterPairMatcher(matcher);
		support.setMatchingCharacterPainterPreferenceKeys(EDITOR_MATCHING_BRACKETS,EDITOR_MATCHING_BRACKETS_COLOR);
	 
		//Enable bracket highlighting in the preference store
		IPreferenceStore store = getPreferenceStore();
		store.setDefault(EDITOR_MATCHING_BRACKETS, true);
		store.setDefault(EDITOR_MATCHING_BRACKETS_COLOR, "128,128,128");
	}
	
	@Override
    public void dispose() {
		colorManager.dispose();
		super.dispose();
	}
	
	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#createColumnSupport()
	 * @since 3.3
	 */
	
	
	
	/*
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#createVerticalRuler()
	 * @since 3.3
	 */
	@Override
	protected IVerticalRuler createVerticalRuler() {
		return new CompositeRuler();
	}
	
}
