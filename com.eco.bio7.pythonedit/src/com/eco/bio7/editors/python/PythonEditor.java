package com.eco.bio7.editors.python;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
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

public class PythonEditor extends TextEditor implements IPropertyChangeListener, com.eco.bio7.pythonedit.analysis.OutlineTarget {

	/* The outline (classes, functions, variables, imports) and the last structure of the code */
	private com.eco.bio7.pythonedit.analysis.PythonOutlinePage outlinePage;
	private volatile Object lastOutline;

	@Override
	public void outlineChanged(Object outline) {
		lastOutline = outline;
		com.eco.bio7.pythonedit.analysis.PythonOutlinePage page = outlinePage;
		if (page != null) {
			page.update(outline);
		}
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (org.eclipse.ui.views.contentoutline.IContentOutlinePage.class.equals(adapter)) {
			if (outlinePage == null || outlinePage.getControl() == null || outlinePage.getControl().isDisposed()) {
				outlinePage = new com.eco.bio7.pythonedit.analysis.PythonOutlinePage(this);
				if (lastOutline != null) {
					outlinePage.update(lastOutline);
				}
			}
			return adapter.cast(outlinePage);
		}
		return super.getAdapter(adapter);
	}

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
		setSourceViewerConfiguration(new PythonConfiguration(colorManager, this));
		setDocumentProvider(new PythonDocumentProvider());

	}

	@Override
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {

		super.handlePreferenceStoreChanged(event);

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
		/* From the font shown now: the editor font and the syntax fonts get the same size (see EditorFonts) */
		if (com.eco.bio7.util.EditorFonts.zoom("com.eco.bio7.pythoneditor.textfont", PythonEditorPlugin.getDefault().getPreferenceStore(), Math.round(fontSize))) {
			invalidateText();
		}
	}

	@Override
	public void createPartControl(org.eclipse.swt.widgets.Composite parent) {
		super.createPartControl(parent);
		/* No Run As, Debug As, Profile As (no launch configurations for the files of the editor) */
		com.eco.bio7.util.LaunchMenus.removeFrom(getSourceViewer().getTextWidget());
		if (getSourceViewer() instanceof org.eclipse.jface.text.ITextViewerExtension8 v) {
			v.setHoverEnrichMode(org.eclipse.jface.text.ITextViewerExtension8.EnrichMode.AFTER_DELAY);
		}
	}

	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] { "com.eco.bio7.python.editor.scope" });
	}

	@Override
	protected void rulerContextMenuAboutToShow(IMenuManager menu) {
		super.rulerContextMenuAboutToShow(menu);
		/* The same breakpoint actions as in the R editor */
		addAction(menu, "Toggle Python Breakpoint");
		addAction(menu, "Python Breakpoint Condition");
		addAction(menu, "Remove All Python Breakpoints");
	}

	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		addAction(menu, "ContentAssistProposal");
		addAction(menu, ITextEditorActionConstants.QUICK_ASSIST);
		addAction(menu, "Format Python Code");
		org.eclipse.jface.action.MenuManager refactor = new org.eclipse.jface.action.MenuManager("Refactor");
		for (String[] k : com.eco.bio7.pythonedit.analysis.RefactorAction.KINDS) {
			if (k[0].equals("imports") || k[0].equals("surround")) {
				refactor.add(new Separator());
			}
			IAction a = getAction(k[2]);
			if (a != null) {
				refactor.add(a);
			}
		}
		menu.add(refactor);
		if (com.eco.bio7.pythonedit.analysis.PythonEditorSupport.debugScript != null) {
			menu.add(new org.eclipse.jface.action.Action("Debug Python Script") {
				@Override
				public void run() {
					if (isDirty()) {
						doSave(null);
					}
					Runnable r = com.eco.bio7.pythonedit.analysis.PythonEditorSupport.debugScript;
					if (r != null) {
						r.run();
					}
				}
			});
		}
		if (com.eco.bio7.pythonedit.analysis.PythonEditorSupport.profileScript != null) {
			/* The profiler: the time of the lines and functions (a report and marks in the ruler) */
			org.eclipse.jface.action.MenuManager profile = new org.eclipse.jface.action.MenuManager("Profile");
			profile.add(new org.eclipse.jface.action.Action("Profile Python Script") {
				@Override
				public void run() {
					java.util.function.Consumer<Boolean> p = com.eco.bio7.pythonedit.analysis.PythonEditorSupport.profileScript;
					if (p != null) {
						p.accept(false);
					}
				}
			});
			org.eclipse.jface.action.Action selection = new org.eclipse.jface.action.Action("Profile Selection") {
				@Override
				public void run() {
					java.util.function.Consumer<Boolean> p = com.eco.bio7.pythonedit.analysis.PythonEditorSupport.profileScript;
					if (p != null) {
						p.accept(true);
					}
				}
			};
			selection.setEnabled(getSelectionProvider().getSelection() instanceof org.eclipse.jface.text.ITextSelection ts && ts.getLength() > 0);
			profile.add(selection);
			profile.add(new Separator());
			profile.add(new org.eclipse.jface.action.Action("Clear Profile Marks") {
				@Override
				public void run() {
					if (getEditorInput() instanceof org.eclipse.ui.IFileEditorInput fi) {
						com.eco.bio7.pythonedit.analysis.PythonProfileMarks.clear(fi.getFile());
					}
				}
			});
			menu.add(profile);
		}
		menu.add(new Separator());
		addAction(menu, "Add Block Comment");
		addAction(menu, "Remove Block Comment");
		menu.add(new Separator());
		addAction(menu, "Python Editor Preferences");
		/* All hovers of the editor on or off (help, values, colors, quick fixes, values while debugging) */
		org.eclipse.jface.preference.IPreferenceStore hoverStore = com.eco.bio7.pythonedit.PythonEditorPlugin.getDefault().getPreferenceStore();
		org.eclipse.jface.action.Action hovers = new org.eclipse.jface.action.Action("Enable Hovers", org.eclipse.jface.action.IAction.AS_CHECK_BOX) {
			@Override
			public void run() {
				hoverStore.setValue(com.eco.bio7.pythonedit.analysis.PythonEditorSupport.HOVERS, isChecked());
			}
		};
		hovers.setChecked(com.eco.bio7.pythonedit.analysis.PythonEditorSupport.hoversEnabled());
		menu.add(hovers);

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
		setAction("Python Editor Preferences", preferences);
		/* A click on a marker with quick fixes in the ruler: the quick fixes (as in the Java editor) */
		setAction(ITextEditorActionConstants.RULER_CLICK, new org.eclipse.jface.action.Action("Quick Fix") {
			@Override
			public void run() {
				int line = getVerticalRuler().getLineOfLastMouseButtonActivity();
				org.eclipse.jface.text.Position p = com.eco.bio7.pythonedit.analysis.PythonFixes.fixablePositionOnLine(getSourceViewer(), line);
				if (p != null) {
					selectAndReveal(p.getOffset(), p.getLength());
					org.eclipse.jface.text.ITextOperationTarget target = getAdapter(org.eclipse.jface.text.ITextOperationTarget.class);
					if (target != null && target.canDoOperation(ISourceViewer.QUICK_ASSIST)) {
						target.doOperation(ISourceViewer.QUICK_ASSIST);
					}
				}
			}
		});
		/* A double click on the ruler sets or removes a breakpoint (the debugger of the embedded Python) */
		IAction toggle = com.eco.bio7.pythonedit.analysis.PythonBreakpoints.toggleAction(this, getVerticalRuler());
		setAction(ITextEditorActionConstants.RULER_DOUBLE_CLICK, toggle);
		setAction("Toggle Python Breakpoint", toggle);
		/* The condition and the hit count of a breakpoint (the ruler) */
		setAction("Python Breakpoint Condition", com.eco.bio7.pythonedit.analysis.PythonBreakpoints.propertiesAction(this, getVerticalRuler()));
		setAction("Remove All Python Breakpoints", com.eco.bio7.pythonedit.analysis.PythonBreakpoints.removeAllAction(this));
		/* Formats the code with ruff or black (Ctrl+Shift+F) */
		setAction("Format Python Code", new com.eco.bio7.pythonedit.analysis.FormatAction(this));
		/* The refactorings (the Refactor menu, key bindings) */
		for (String[] k : com.eco.bio7.pythonedit.analysis.RefactorAction.KINDS) {
			setAction(k[2], new com.eco.bio7.pythonedit.analysis.RefactorAction(this, k));
		}

	}

	protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {
		super.configureSourceViewerDecorationSupport(support);

		char[] matchChars = { '{', '}', '(', ')', '[', ']' }; // which brackets
																// to match
		ICharacterPairMatcher matcher = new DefaultCharacterPairMatcher(matchChars, IDocumentExtension3.DEFAULT_PARTITIONING);
		support.setCharacterPairMatcher(matcher);
		support.setMatchingCharacterPainterPreferenceKeys(EDITOR_MATCHING_BRACKETS, EDITOR_MATCHING_BRACKETS_COLOR);

		// Enable bracket highlighting in the preference store
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
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#createVerticalRuler()
	 * 
	 * @since 3.3
	 */
	@Override
	protected IVerticalRuler createVerticalRuler() {
		return new CompositeRuler();
	}

}
