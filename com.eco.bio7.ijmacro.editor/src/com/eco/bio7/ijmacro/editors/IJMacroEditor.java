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
package com.eco.bio7.ijmacro.editors;

import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.SwingUtilities;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import com.eco.bio7.ijmacro.editor.actions.ImageJForumCopy;
import com.eco.bio7.ijmacro.editor.actions.InterpretImageJMacroAction;
import com.eco.bio7.ijmacro.editor.actions.OpenPreferences;
import com.eco.bio7.ijmacro.editor.actions.ScriptFormatterAction;
import com.eco.bio7.ijmacro.editor.actions.ScriptFormatterSelectAction;
import com.eco.bio7.ijmacro.editor.actions.SetComment;
import com.eco.bio7.ijmacro.editor.actions.UnsetComment;
import com.eco.bio7.ijmacro.editor.antlr.WordMarkerCreation;
import com.eco.bio7.ijmacro.editor.outline.IJMacroEditorLabelProvider;
import com.eco.bio7.ijmacro.editor.outline.IJMacroEditorOutlineNode;
import com.eco.bio7.ijmacro.editor.outline.IJMacroEditorTreeContentProvider;
import com.eco.bio7.ijmacro.editor.toolbar.debug.DebugVariablesView;
import com.eco.bio7.image.CanvasView;
import com.eco.bio7.image.Util;

import ij.IJ;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.macro.Debugger;
import ij.macro.Interpreter;
import ij.plugin.Macro_Runner;
import ij.text.TextPanel;
import ij.text.TextWindow;

/**
 * 
 */
public class IJMacroEditor extends TextEditor implements IPropertyChangeListener, Debugger {

	private static final String TEMPLATE_PROPOSALS = "template_proposals_action";

	private ColorManager colorManager;

	private IPartListener partListener;

	private SetComment setComment;

	private UnsetComment unsetComment;

	private ScriptFormatterAction javaFormat;

	private ScriptFormatterSelectAction javaSelectFormat;

	private OpenPreferences preferences;

	private int debugStart, debugEnd;

	private static TextWindow debugWindow;

	public final static String EDITOR_MATCHING_BRACKETS = "matchingBrackets";

	public final static String EDITOR_MATCHING_BRACKETS_COLOR = "matchingBracketsColor";

	final private ScopedPreferenceStore storeWorkbench = new ScopedPreferenceStore(new InstanceScope(), "org.eclipse.ui.workbench");

	private InterpretImageJMacroAction interpretImageJMacroAction;

	private boolean step;

	private int runToLine;

	private String path;

	private boolean checkForCurlyQuotes;

	private boolean changes;

	private int previousLine;

	private IContentOutlinePage contentOutlinePage;

	public IJMacroEditorTreeContentProvider tcp;
	private TreeViewer contentOutlineViewer;
	public Vector<IJMacroEditorOutlineNode> nodes = new Vector<IJMacroEditorOutlineNode>();
	public IJMacroEditorOutlineNode baseNode;// Function category!
	// private RConfiguration rconf;
	private Object[] expanded;
	protected ArrayList<TreeItem> selectedItems;

	public ProjectionViewer viewer;

	private ProjectionSupport projectionSupport;

	private ProjectionAnnotationModel annotationModel;

	private IPreferenceStore store;

	private IJMacroConfiguration ijMacroConfig;

	private ImageJForumCopy imagejForumCopy;

	private static Shell tempShell;

	private String markerExpression;

	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		// PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,
		// "com.eco.bio7.beanshell");
		store = IJMacroEditorPlugin.getDefault().getPreferenceStore();
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(listener);
		viewer = (ProjectionViewer) getSourceViewer();

		projectionSupport = new ProjectionSupport(viewer, getAnnotationAccess(), getSharedColors());
		projectionSupport.install();

		// turn projection mode on
		viewer.doOperation(ProjectionViewer.TOGGLE);

		annotationModel = viewer.getProjectionAnnotationModel();
		IEditorPart editor = this;
		final ITextEditor textEditor = (ITextEditor) editor;
		if (editor instanceof IJMacroEditor) {

			((StyledText) editor.getAdapter(Control.class)).addMouseListener(new MouseListener() {

				@Override
				public void mouseDoubleClick(MouseEvent e) {

				}

				@Override
				public void mouseDown(MouseEvent e) {
					if (org.eclipse.jface.util.Util.isMac()) {

						activateEditorPage(editor);
					}
					IDocumentProvider prov = textEditor.getDocumentProvider();
					IEditorInput inp = editor.getEditorInput();
					if (prov != null) {
						IDocument document = prov.getDocument(inp);
						if (document != null) {

							ITextSelection textSelection = (ITextSelection) editor.getSite().getSelectionProvider().getSelection();
							int offset = textSelection.getOffset();
							if (store.getBoolean("MARK_WORDS")) {
								markWords(offset, document, editor);
							}

						}

					}

				}

				@Override
				public void mouseUp(MouseEvent e) {

				}

			});

		}
		getEditorSite().getPage().addPartListener(new IPartListener() {

			public void partActivated(IWorkbenchPart part) {

				IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
				if (editor instanceof IJMacroEditor) {
					ITextEditor ed = (ITextEditor) editor;
					Control ctrl = (Control) editor.getAdapter(Control.class);
					DropTarget dropTarget = (DropTarget) ctrl.getData(DND.DROP_TARGET_KEY);
					DropTargetListener[] drops = dropTarget.getDropListeners();
					/*Remove the default drag listener!*/
					for (int i = 0; i < drops.length; i++) {
						dropTarget.removeDropListener(drops[i]);
					}
					dropTarget.addDropListener(new DropTargetListener() {

						@Override
						public void dragEnter(DropTargetEvent event) {
							// TODO Auto-generated method stub

						}

						@Override
						public void dragLeave(DropTargetEvent event) {
							// TODO Auto-generated method stub

						}

						@Override
						public void dragOperationChanged(DropTargetEvent event) {
							// TODO Auto-generated method stub

						}

						@Override
						public void dragOver(DropTargetEvent event) {
							// TODO Auto-generated method stub

						}

						@Override
						public void drop(DropTargetEvent event) {
							if(event.data instanceof String[]==false) {
								return;
							}
							String[] fileNames = (String[]) event.data;
							StringBuffer buff = new StringBuffer();
							for (int i = 0; i < fileNames.length; i++) {
								if (fileNames.length > 1) {
									buff.append("path_" + (i + 1) + " = " + "\"" + fileNames[i] + "\";" + System.lineSeparator());
								} else {
									buff.append("path_" + (i + 1) + " = " + "\"" + fileNames[i] + "\";");

								}

							}

							if (editor instanceof ITextEditor) {
								IDocument doc = ed.getDocumentProvider().getDocument(editor.getEditorInput());
								ITextSelection sel = (ITextSelection) ed.getSelectionProvider().getSelection();
								int offset = sel.getOffset();
								try {
									doc.replace(offset, 0, buff.toString());
								} catch (BadLocationException e) {
									e.printStackTrace();
								}
							}

						}

						@Override
						public void dropAccept(DropTargetEvent event) {

						}

					});

				}

			}

			public void partBroughtToTop(IWorkbenchPart part) {

			}

			public void partClosed(IWorkbenchPart part) {

			}

			public void partDeactivated(IWorkbenchPart part) {
				if (part instanceof CanvasView) {

				}
			}

			public void partOpened(IWorkbenchPart part) {
				if (part instanceof CanvasView) {

				}
			}

		});
	}

	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		ISourceViewer viewer = new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);

		// ensure decoration support has been created and configured.
		getSourceViewerDecorationSupport(viewer);

		return viewer;
	}

	public IWorkbenchPage getPage() {
		IWorkbenchPage page = getSite().getPage();

		return page;
	}

	public ProjectionViewer getViewer() {
		return viewer;
	}

	/**
	 * Creates a new BeanShell editor.
	 */
	public IJMacroEditor() {
		super();
		/*
		 * Add a new key binding scope for this editor Don't forget to set the
		 * 'symbolicFontName' extension attribute in 'org.eclipse.ui.editors'!
		 */
		setKeyBindingScopes(new String[] { "com.eco.bio7.ijmacro.editor.scope" });
		colorManager = new ColorManager();
		ijMacroConfig = new IJMacroConfiguration(colorManager, this);
		setSourceViewerConfiguration(ijMacroConfig);
		/*
		 * Set the context to avoid that menus appear in a different editor. E.g.,
		 * refactor dialogs!
		 */
		setEditorContextMenuId("#IJMacroContext");

		if (Util.isThemeBlack()) {
			IPreferenceStore store = IJMacroEditorPlugin.getDefault().getPreferenceStore();
			PreferenceConverter.setDefault(store, "colourkey", new RGB(167, 236, 33));
			PreferenceConverter.setDefault(store, "colourkey1", new RGB(177, 102, 218));
			PreferenceConverter.setDefault(store, "colourkey2", new RGB(23, 198, 163));
			PreferenceConverter.setDefault(store, "colourkey3", new RGB(128, 128, 128));
			PreferenceConverter.setDefault(store, "colourkey4", new RGB(204, 108, 29));
			PreferenceConverter.setDefault(store, "colourkey5", new RGB(230, 230, 250));
			PreferenceConverter.setDefault(store, "colourkey6", new RGB(250, 243, 243));
			PreferenceConverter.setDefault(store, "colourkey7", new RGB(104, 151, 187));
			PreferenceConverter.setDefault(store, "colourkey8", new RGB(250, 243, 243));
		} else {

			IPreferenceStore store = IJMacroEditorPlugin.getDefault().getPreferenceStore();
			PreferenceConverter.setDefault(store, "colourkey", new RGB(127, 0, 85));
			PreferenceConverter.setDefault(store, "colourkey1", new RGB(127, 0, 85));
			PreferenceConverter.setDefault(store, "colourkey2", new RGB(42, 0, 255));
			PreferenceConverter.setDefault(store, "colourkey3", new RGB(128, 128, 128));
			PreferenceConverter.setDefault(store, "colourkey4", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(store, "colourkey5", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(store, "colourkey6", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(store, "colourkey7", new RGB(0, 0, 0));
			PreferenceConverter.setDefault(store, "colourkey8", new RGB(63, 127, 95));

		}

	}

	public IJMacroConfiguration getIjMacroConfig() {
		return ijMacroConfig;
	}

	/*
	 * Here we search for similar words of a selected word in the editor. The
	 * results will be marked!
	 */
	public void markWords(int offset, IDocument doc, IEditorPart editor) {

		WordMarkerCreation markerJob = new WordMarkerCreation(offset, editor, doc);

		markerJob.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {

				} else {

				}
			}
		});
		markerJob.setUser(true);
		markerJob.schedule();

	}

	@Override
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {

		super.handlePreferenceStoreChanged(event);

	}

	public void invalidateText() {
		if (IJMacroEditor.this != null) {
			if (IJMacroEditor.this.getSourceViewer() != null) {
				IJMacroEditor.this.getSourceViewer().invalidateTextPresentation();
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

		IPreferenceStore store = IJMacroEditorPlugin.getDefault().getPreferenceStore();

		FontData f = PreferenceConverter.getFontData(store, "colourkeyfont");
		FontData f1 = PreferenceConverter.getFontData(store, "colourkeyfont1");
		FontData f2 = PreferenceConverter.getFontData(store, "colourkeyfont2");
		FontData f3 = PreferenceConverter.getFontData(store, "colourkeyfont3");
		FontData f4 = PreferenceConverter.getFontData(store, "colourkeyfont4");
		FontData f5 = PreferenceConverter.getFontData(store, "colourkeyfont5");
		FontData f6 = PreferenceConverter.getFontData(store, "colourkeyfont6");
		FontData f7 = PreferenceConverter.getFontData(store, "colourkeyfont7");
		FontData f8 = PreferenceConverter.getFontData(store, "colourkeyfont8");
		// FontData f8 = PreferenceConverter.getFontData(store,
		// "colourkeyfont8");

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
		} else if (f8.getHeight() + Math.round(fontSize) < 2) {
			return;
		} /*
			 * else if (f8.getHeight() + Math.round(fontSize) < 2) { return; }
			 */

		f.setHeight(f.getHeight() + Math.round(fontSize));
		f1.setHeight(f1.getHeight() + Math.round(fontSize));
		f2.setHeight(f2.getHeight() + Math.round(fontSize));
		f3.setHeight(f3.getHeight() + Math.round(fontSize));
		f4.setHeight(f4.getHeight() + Math.round(fontSize));
		f5.setHeight(f5.getHeight() + Math.round(fontSize));
		f6.setHeight(f6.getHeight() + Math.round(fontSize));
		f7.setHeight(f7.getHeight() + Math.round(fontSize));
		f8.setHeight(f8.getHeight() + Math.round(fontSize));

		// Method from:
		// https://github.com/gkorland/Eclipse-Fonts/blob/master/Fonts/src/main/java/fonts/FontsControler.java
		String font = storeWorkbench.getString("com.eco.bio7.ijmacro.editor.textfont");
		String[] split = font.split("\\|");

		split[2] = Float.toString(f.getHeight());
		StringBuilder builder = new StringBuilder(split[0]);
		for (int i = 1; i < split.length; ++i) {
			builder.append('|').append(split[i]);
		}
		storeWorkbench.setValue("com.eco.bio7.ijmacro.editor.textfont", builder.toString());

		/* Invokes a property change! */
		PreferenceConverter.setValue(store, "colourkeyfont", f);
		PreferenceConverter.setValue(store, "colourkeyfont1", f1);
		PreferenceConverter.setValue(store, "colourkeyfont2", f2);
		PreferenceConverter.setValue(store, "colourkeyfont3", f3);
		PreferenceConverter.setValue(store, "colourkeyfont4", f4);
		PreferenceConverter.setValue(store, "colourkeyfont5", f5);
		PreferenceConverter.setValue(store, "colourkeyfont6", f6);
		PreferenceConverter.setValue(store, "colourkeyfont7", f7);
		PreferenceConverter.setValue(store, "colourkeyfont8", f8);

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
		addAction(menu, "ImageJForum");
		menu.add(new Separator());
		addAction(menu, "Format Source");
		addAction(menu, "Format Selected Source");
		menu.add(new Separator());
		addAction(menu, "Convert R Code");
		menu.add(new Separator());
		addAction(menu, "Editor Preferences");
	}

	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] { "com.eco.bio7.ijmacro.editor.scope" });
	}

	protected void createActions() {
		super.createActions();

		IAction action = new TextOperationAction(TemplateMessages.getResourceBundle(), "Editor." + TEMPLATE_PROPOSALS + ".", //$NON-NLS-1$ //$NON-NLS-2$
				this, ISourceViewer.CONTENTASSIST_PROPOSALS);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction(TEMPLATE_PROPOSALS, action);
		markAsStateDependentAction(TEMPLATE_PROPOSALS, true);

		// define the action
		IAction a = new TextOperationAction(ScriptEditorMessages.getResourceBundle(), "ContentAssistProposal.", this, //$NON-NLS-1$
				ISourceViewer.CONTENTASSIST_PROPOSALS);
		a.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction("ContentAssistProposal", a); //$NON-NLS-1$

		setComment = new com.eco.bio7.ijmacro.editor.actions.SetComment("Add Block Comment", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Add Block Comment", setComment);

		unsetComment = new com.eco.bio7.ijmacro.editor.actions.UnsetComment("Remove Block Comment", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Remove Block Comment", unsetComment);

		javaFormat = new com.eco.bio7.ijmacro.editor.actions.ScriptFormatterAction();
		setAction("Format Source", javaFormat);

		javaSelectFormat = new com.eco.bio7.ijmacro.editor.actions.ScriptFormatterSelectAction();
		setAction("Format Selected Source", javaSelectFormat);

		preferences = new com.eco.bio7.ijmacro.editor.actions.OpenPreferences();
		setAction("Editor Preferences", preferences);

		interpretImageJMacroAction = new com.eco.bio7.ijmacro.editor.actions.InterpretImageJMacroAction("");
		setAction("Interpret Macro", interpretImageJMacroAction);

		imagejForumCopy = new com.eco.bio7.ijmacro.editor.actions.ImageJForumCopy();
		setAction("ImageJForum", imagejForumCopy);

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

	public final void runMacro(boolean debug) {
		if (path != null)
			Macro_Runner.setFilePath(path);
		/*
		 * if (getTitle().endsWith(".js")) {evaluateJavaScript(); return;} else if
		 * (getTitle().endsWith(".bsh")) {evaluateScript(".bsh"); return;} else if
		 * (getTitle().endsWith(".py")) {evaluateScript(".py"); return;}
		 */
		int start = getSelectionStart();
		int end = getSelectionEnd();
		String text;
		if (start == end)
			text = getText();
		else
			text = getSelectedText(IJMacroEditor.this);
		Interpreter instance = Interpreter.getInstance();
		if (instance != null) { // abort any currently running macro
			instance.abortMacro();
			long t0 = System.currentTimeMillis();
			while (Interpreter.getInstance() != null && (System.currentTimeMillis() - t0) < 3000L)
				IJ.wait(10);
		}
		if (checkForCurlyQuotes && text.contains("\u201D")) {
			// replace curly quotes with standard quotes
			text = text.replaceAll("\u201C", "\"");
			text = text.replaceAll("\u201D", "\"");
			if (start == end)
				setText(text);
			else {
				String text2 = getText();
				text2 = text2.replaceAll("\u201C", "\"");
				text2 = text2.replaceAll("\u201D", "\"");
				setText(text2);
			}
			changes = true;
			checkForCurlyQuotes = false;
		}
		new com.eco.bio7.ijmacro.editors.MacroRunner(text, debug ? IJMacroEditor.this : null);
	}

	public int debug(Interpreter interp, int mode) {
		if (IJ.debugMode)
			IJ.log("debug: " + interp.getLineNumber() + "  " + mode + "  " + interp);
		if (mode == RUN_TO_COMPLETION)
			return 0;
		int n = interp.getLineNumber();
		if (mode == RUN_TO_CARET) {
			if (n == runToLine) {
				mode = STEP;
				interp.setDebugMode(mode);
			} else
				return 0;
		}
		if (mode == RUN_TO_EXPRESSION) {
			if (markerExpression != null) {
				String[] valueOfEx = markerExpression.split(" ");
				String existingVar = valueOfEx[0];
				String operator = valueOfEx[1];
				String valueToCompare = valueOfEx[2];
				double val = interp.getVariable(existingVar);

				if (operator.equals("==")) {
					if (val == Double.parseDouble(valueToCompare)) {
						mode = STEP;
						interp.setDebugMode(mode);
					} else {
						return 0;
					}
				} else if (operator.equals("!=")) {
					if (val != Double.parseDouble(valueToCompare)) {
						mode = STEP;
						interp.setDebugMode(mode);
					} else {
						return 0;
					}
				} else if (operator.equals("<=")) {
					if (val <= Double.parseDouble(valueToCompare)) {
						mode = STEP;
						interp.setDebugMode(mode);
					} else {
						return 0;
					}
				} else if (operator.equals(">=")) {
					if (val >= Double.parseDouble(valueToCompare)) {
						mode = STEP;
						interp.setDebugMode(mode);
					} else {
						return 0;
					}
				} else if (operator.equals("<")) {
					if (val < Double.parseDouble(valueToCompare)) {
						mode = STEP;
						interp.setDebugMode(mode);
					} else {
						return 0;
					}
				} else if (operator.equals(">")) {
					if (val > Double.parseDouble(valueToCompare)) {
						mode = STEP;
						interp.setDebugMode(mode);
					} else {
						return 0;
					}
				}
			}
		}

		/*
		 * if (!DebugVariablesView.getDebugVariablesGrid().isVisible()) { // abort macro
		 * if user closes window // interp.abortMacro(); return 0; }
		 */

		if (n == previousLine) {
			previousLine = 0;
			return 0;
		}
		/*
		 * Window win = WindowManager.getActiveWindow(); if (win!=this) IJ.wait(50);
		 * toFront();
		 */
		/*
		 * Display display = PlatformUI.getWorkbench().getDisplay();
		 * display.syncExec(new Runnable() { public void run() { try { IWorkbenchPage
		 * page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		 * page.showView("com.eco.bio7.image.view.debug"); } catch (PartInitException e)
		 * { // TODO Auto-generated catch block e.printStackTrace(); }
		 * 
		 * } });
		 */
		previousLine = n;
		String text = getText();
		if (IJ.isWindows())
			text = text.replaceAll("\r\n", "\n");
		char[] chars = new char[text.length()];
		chars = text.toCharArray();
		int count = 1;
		debugStart = 0;
		int len = chars.length;
		debugEnd = len;
		for (int i = 0; i < len; i++) {
			if (chars[i] == '\n')
				count++;
			if (count == n && debugStart == 0)
				debugStart = i + 1;
			else if (count == n + 1) {
				debugEnd = i;
				break;
			}
		}
		// IJ.log("debug: "+debugStart+" "+debugEnd+" "+len+" "+count);
		if (debugStart == 1)
			debugStart = 0;
		if ((debugStart == 0 || debugStart == len) && debugEnd == len)
			return 0; // skip code added with Interpreter.setAdditionalFunctions()
		select(IJMacroEditor.this, debugStart, debugEnd);
		/*
		 * if (debugWindow != null && !debugWindow.isShowing()) {
		 * interp.setDebugger(null); debugWindow = null;
		 */
		// } else

		Table debugWindow = updateDebugWindow(interp.getVariables(), DebugVariablesView.getDebugVariablesGrid(), interp);
		if (debugWindow != null) {
			interp.updateArrayInspector();
			// toFront();
		}
		if (mode == STEP) {
			step = false;
			// while (!step && !interp.done() && isVisible())
			while (!step && !interp.done())
				IJ.wait(5);
		} else {
			if (mode == FAST_TRACE)
				IJ.wait(5);
			else
				IJ.wait(150);
		}

		return 0;
	}

	public Table updateDebugWindow(String[] variables, Table table, Interpreter interp) {
		/*
		 * if (debugWindow == null) { Frame f = WindowManager.getFrame("Debug"); if (f
		 * != null && (f instanceof TextWindow)) { debugWindow = (TextWindow) f;
		 * debugWindow.toFront(); } } if (debugWindow == null) debugWindow = new
		 * TextWindow("Debug", "Name\t*\tValue", "", 300, 400); TextPanel panel =
		 * debugWindow.getTextPanel();
		 */

		int n = variables.length;
		if (n == 0) {
			Display dis = Util.getDisplay();
			dis.syncExec(new Runnable() {

				public void run() {
					// panel.clear();
					if (table != null) {
						if (table.isDisposed() == false) {
							if (table != null) {
								table.removeAll();
							}
						}
					}
				}
			});

			return table;
		}
		// int lines = panel.getLineCount();
		String[] markedVariables = interp.markChanges(variables);
		/* Using SWT tables! */
		Display dis = Util.getDisplay();
		dis.syncExec(new Runnable() {

			public void run() {
				if (table != null) {
					if (table.isDisposed() == false) {
						table.removeAll();
						for (int i = 0; i < markedVariables.length; i++) {
							if (i < n) {
								// panel.setLine(i, markedVariables[i]);
								new TableItem(table, SWT.NONE).setText(markedVariables[i].split("\t"));

							} else {
								// panel.setLine(i, "");
								new TableItem(table, SWT.NONE).setText(markedVariables[i].split("\t"));

							}
						}
					}
				}
			}

		});

		/*
		 * for (int i = lines; i < n; i++) debugWindow.append(markedVariables[i]);
		 */

		return table;
	}

	public IDocument getDocument() {
		IDocument doc = IJMacroEditor.this.getSourceViewer().getDocument();
		return doc;
	}

	public String getText() {
		IDocument doc = IJMacroEditor.this.getSourceViewer().getDocument();
		return doc.get();
	}

	public void setText(String text) {
		IDocument doc = IJMacroEditor.this.getSourceViewer().getDocument();
		doc.set(text);
	}

	private int getCurrentLine(IEditorPart rEditor) {

		ITextEditor editor = (ITextEditor) rEditor;

		ISelectionProvider sp = editor.getSelectionProvider();

		ISelection selectionsel = sp.getSelection();

		ITextSelection selection = (ITextSelection) selectionsel;

		int b = selection.getStartLine() + 1;
		return b;
	}

	private String getSelectedText(IEditorPart rEditor) {
		ITextEditor editor = (ITextEditor) rEditor;

		ISelectionProvider sp = editor.getSelectionProvider();

		ISelection selectionsel = sp.getSelection();

		ITextSelection selection = (ITextSelection) selectionsel;

		return selection.getText();
	}

	public ITextSelection getTextSelection(IEditorPart rEditor) {
		ITextEditor editor = (ITextEditor) rEditor;

		ISelectionProvider sp = editor.getSelectionProvider();

		ISelection selectionsel = sp.getSelection();

		ITextSelection selection = (ITextSelection) selectionsel;

		return selection;
	}

	private int getSelectionStart() {
		int selectionOffset = getTextSelection(IJMacroEditor.this).getOffset();
		return selectionOffset;
	}

	private int getSelectionEnd() {
		int selectionOffset = getTextSelection(IJMacroEditor.this).getOffset();
		int length = getTextSelection(IJMacroEditor.this).getLength();
		return selectionOffset + length;
	}

	private void select(IEditorPart rEditor, int debugStart, int debugEnd) {
		ITextEditor editor = (ITextEditor) rEditor;

		getDisplay().syncExec(new Runnable() {

			public void run() {

				getSourceViewer().revealRange(debugStart, debugEnd - debugStart);

				IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
				try {
					resource.deleteMarkers("com.eco.bio7.ijmacroeditor.debugrulermarkarrow", false, IResource.DEPTH_ZERO);
				} catch (CoreException e1) {

					e1.printStackTrace();
				}

				IMarker marker;

				try {

					marker = resource.createMarker("com.eco.bio7.ijmacroeditor.debugrulermarkarrow");
					marker.setAttribute(IMarker.CHAR_START, debugStart);
					marker.setAttribute(IMarker.CHAR_END, debugEnd);
				} catch (CoreException e) {

					e.printStackTrace();
				}

			}

		});

	}

	private IRegion getRegion() {
		int b = getTextSelection(IJMacroEditor.this).getStartLine();

		IRegion reg = null;
		try {
			reg = getDocument().getLineInformation(b);
		} catch (BadLocationException e1) {

			e1.printStackTrace();
		}
		return reg;
	}

	public void evaluateLine() {

		IRegion reg = getRegion();

		/* We have to made a line selection for the evaluation! */
		select(IJMacroEditor.this, reg.getOffset(), reg.getOffset() + reg.getLength());

		int start = getSelectionStart();
		int end = getSelectionEnd();
		// System.out.println(start + " " + end);
		if (end > start) {
			runMacro(false);
			return;
		}
		String text = getText();
		while (start > 0) {
			start--;
			if (text.charAt(start) == '\n') {
				start++;
				break;
			}
		}
		while (end < text.length() - 1) {
			end++;
			if (text.charAt(end) == '\n')
				break;
		}
		// setSelectionStart(start);
		// setSelectionEnd(end);
		// System.out.println(start + " " + end);
		select(IJMacroEditor.this, debugStart, debugEnd);
		runMacro(false);
	}

	public final void setDebugMode(int mode) {
		step = true;
		Interpreter interp = Interpreter.getInstance();
		if (interp != null) {
			if (interp.getDebugger() == null) {
				// fixLineEndings();
			}
			interp.setDebugger(this);
			interp.setDebugMode(mode);
		}
	}

	public final void enableDebugging() {
		step = true;
		ITextSelection textSelection = getTextSelection(IJMacroEditor.this);
		int length = textSelection.getLength();
		int start = textSelection.getOffset();// ta.getSelectionStart();
		int end = textSelection.getOffset() + length;
		if (start == debugStart && end == debugEnd)
			// ta.select(start, start);
			IJMacroEditor.this.selectAndReveal(start + 1 + length + 1, 0);
	}

	/**
	 * Changes Mac OS 9 (CR) and Windows (CRLF) line separators to line feeds (LF).
	 */
	public void fixLineEndings() {

		// IDocumentProvider dp = this.getDocumentProvider();
		// IDocument doc = dp.getDocument(this.getEditorInput());
		/*
		 * We use the document adapter here to preserve editor markers. Else they are
		 * deleted!
		 */
		// replace(doc, "\r\n", "\n", true, false, true, false, false);
		// replace(doc, "\r", "", true, false, true, false, false);
		/*
		 * String text = getText(); text = text.replaceAll("\r\n", "\n"); text =
		 * text.replaceAll("\r", "\n"); setText(text);
		 */
	}

	public int replace(IDocument doc, String word1, String word2, boolean forwardSearch, boolean caseSensitive, boolean wholeWord, boolean showmessage, boolean regularExpressions) {
		int x = 0;
		try {
			FindReplaceDocumentAdapter fr = new FindReplaceDocumentAdapter(doc);
			IRegion docRegion = new IRegion() {

				public int getOffset() {
					// TODO Auto-generated method stub
					return 0;
				}

				public int getLength() {
					// TODO Auto-generated method stub
					return 0;
				}
			};
			while ((docRegion = fr.find(docRegion.getOffset() + 1, word1, true, caseSensitive, wholeWord, regularExpressions)) != null) {

				try {
					fr.replace(word2, regularExpressions);
				} catch (Exception e) {
					System.out.println("Line: " + (doc.getLineOfOffset(docRegion.getOffset() + 1) + 1));
					e.printStackTrace();
				}
				x++;
			}

		} catch (BadLocationException e) {

			e.printStackTrace();
		}
		return x;
	}

	public final void runToInsertionPoint() {
		Interpreter interp = Interpreter.getInstance();
		if (interp == null)
			IJ.beep();
		else {
			runToLine = getCurrentLine(IJMacroEditor.this);
			// System.out.println("current Line: " + runToLine);
			setDebugMode(RUN_TO_CARET);
		}
	}

	public final void runToExpression() {
		Interpreter interp = Interpreter.getInstance();
		if (interp == null)
			IJ.beep();
		else {
			runToLine = getCurrentLine(IJMacroEditor.this);
			// System.out.println("current Line: " + runToLine);
			setDebugMode(RUN_TO_EXPRESSION);
		}
	}

	/**
	 * Returns a default display.
	 * 
	 * @return a display
	 */
	public static Display getDisplay() {
		Display display = Display.getCurrent();
		// may be null if outside the UI thread
		if (display == null)
			display = Display.getDefault();
		return display;
	}

	/**
	 * A method to activate the current editor.
	 * 
	 * @param editor
	 */
	public static void activateEditorPage(final IEditorPart editor) {
		/*
		 * IEditorSite site = editor.getEditorSite(); final IWorkbenchPage page =
		 * site.getPage(); Display display = site.getShell().getDisplay();
		 * display.syncExec(new Runnable() { public void run() { page.activate(editor);
		 * } }); if (editor != page.getActiveEditor()) throw new
		 * RuntimeException("Editor couldn't activated");
		 */
		if (tempShell != null) {
			if (tempShell.isDisposed() == false) {
				tempShell.dispose();
			}
		}
		tempShell = new Shell();
	}

	private ISelectionListener listener = new ISelectionListener() {
		public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {

			// we ignore our own selections
			if (sourcepart != IJMacroEditor.this) {
				// showSelection(sourcepart, selection);

				if (selection instanceof IStructuredSelection) {
					IStructuredSelection strucSelection = (IStructuredSelection) selection;
					Object selectedObj = strucSelection.getFirstElement();
					if (selectedObj instanceof IJMacroEditorOutlineNode) {

						IJMacroEditorOutlineNode cm = (IJMacroEditorOutlineNode) selectedObj;
						if (cm != null) {

							int lineNumber = cm.getLineNumber();
							/*
							 * If a line number exist - if a class member of type is available!
							 */
							if (lineNumber > 0) {
								goToLine(IJMacroEditor.this, lineNumber);
							}

						}

					}
				}

			}
		}

	};

	private void goToLine(IEditorPart editorPart, int toLine) {
		if ((editorPart instanceof IJMacroEditor) || toLine <= 0) {

			ITextEditor editor = (ITextEditor) editorPart;

			IDocumentProvider prov = editor.getDocumentProvider();
			IEditorInput inp = editor.getEditorInput();
			if (prov != null) {
				IDocument document = prov.getDocument(inp);
				if (document != null) {
					IRegion region = null;

					try {

						region = document.getLineInformation(toLine - 1);
					} catch (BadLocationException e) {

					}
					if (region != null) {
						// editor.selectAndReveal(region.getOffset(), region.getLength());
						// getSourceViewer().getTextWidget().setSelection(0,0);
						ISourceViewer sourceViewer = getSourceViewer();
						int offset = region.getOffset();
						sourceViewer.revealRange(offset, region.getLength());
						StyledText textWidget = sourceViewer.getTextWidget();
						textWidget.setSelection(offset, offset + region.getLength());
						editor.selectAndReveal(offset, 0);
						textWidget.redraw();
					}
				}
			}
		}
	}

	public Object getAdapter(Class key) {
		if (key.equals(IContentOutlinePage.class)) {

			return getContentOutlinePage();

		} else {
			return super.getAdapter(key);
		}
	}

	public void updateFoldingStructure(ArrayList positions) {
		Annotation[] deletions = computeDifferences(annotationModel, positions);

		Annotation[] annotations = new Annotation[positions.size()];

		// this will hold the new annotations along
		// with their corresponding positions
		HashMap newAnnotations = new HashMap();

		for (int i = 0; i < positions.size(); i++) {
			ProjectionAnnotation annotation = new ProjectionAnnotation();

			newAnnotations.put(annotation, positions.get(i));

			annotations[i] = annotation;
		}

		annotationModel.modifyAnnotations(deletions, newAnnotations, null);

		// oldAnnotations=annotations;
	}

	private Annotation[] computeDifferences(ProjectionAnnotationModel model, ArrayList additions) {
		List deletions = new ArrayList();
		for (Iterator iter = model.getAnnotationIterator(); iter.hasNext();) {
			Object annotation = iter.next();
			if (annotation instanceof ProjectionAnnotation) {
				Position position = model.getPosition((Annotation) annotation);
				if (additions.contains(position)) {
					additions.remove(position);
				} else {
					deletions.add(annotation);
				}
			}
		}
		return (Annotation[]) deletions.toArray(new Annotation[deletions.size()]);
	}

	public void outlineInputChanged(Vector nodesALt, Vector nodesNew) {

		if (contentOutlineViewer != null) {
			/* Store temporary the old expanded elements! */
			expanded = contentOutlineViewer.getExpandedElements();

			TreeViewer viewer = contentOutlineViewer;
			// Sort tree alphabetically
			// viewer.setComparator(new ViewerComparator());
			if (viewer != null) {

				Control control = viewer.getControl();
				if (control != null && !control.isDisposed()) {

					control.setRedraw(false);
					/* Create default categories! */

					/* Set the new tree nodes! */
					viewer.setInput(nodesNew);
					/* First item is the file! */
					TreeItem treeItem = contentOutlineViewer.getTree().getItem(0);
					/* Expand to get access to the subnodes! */
					contentOutlineViewer.setExpandedState(treeItem.getData(), true);
					walkTree(treeItem);
					/* The default expand level! */
					contentOutlineViewer.expandToLevel(2);
					control.setRedraw(true);
					control.redraw();
				}
			}
		}

	}

	/*
	 * This method is recursively called to walk all subtrees and compare the names
	 * of the nodes with the old ones!
	 */

	public void walkTree(TreeItem item) {

		for (int i = 0; i < expanded.length; i++) {

			for (int j = 0; j < item.getItemCount(); j++) {

				TreeItem it = item.getItem(j);

				if (((IJMacroEditorOutlineNode) it.getData()).getName().equals(((IJMacroEditorOutlineNode) expanded[i]).getName())) {
					contentOutlineViewer.setExpandedState(it.getData(), true);
					/* Recursive call of the method for subnodes! */
					walkTree(it);
					break;
				}

			}

		}

	}

	public IContentOutlinePage getContentOutlinePage() {
		if (contentOutlinePage == null) {
			// The content outline is just a tree.
			//

			contentOutlinePage = new MyContentOutlinePage();

		}

		return contentOutlinePage;
	}

	public void createNodes() {
		nodes.clear();
		baseNode = new IJMacroEditorOutlineNode("File", 0, "base", null);
		nodes.add(baseNode);

	}

	class MyContentOutlinePage extends ContentOutlinePage {
		boolean sort = true;

		public void createControl(Composite parent) {
			super.createControl(parent);

			contentOutlineViewer = getTreeViewer();

			Action sortAlphabetAction = new Action("Sort", IAction.AS_PUSH_BUTTON) {

				@Override
				public void run() {

					TreeViewer treeViewer2 = getTreeViewer();
					Tree tree = treeViewer2.getTree();
					tree.setRedraw(false);
					try {

						// getTreeViewer().collapseAll();
						TreeViewer viewer = contentOutlineViewer;
						// Sort tree alphabetically
						if (sort) {

							viewer.setComparator(new ViewerComparator());
							sort = false;
						} else {
							viewer.setComparator(null);
							sort = true;
						}
					} finally {
						tree.setRedraw(true);
					}
					tree.redraw();
				}

			};
			sortAlphabetAction.setImageDescriptor(IJMacroEditorPlugin.getImageDescriptor("icons/alphab_sort_co.png"));
			Action collapseAllAction = new Action("Collapse", IAction.AS_PUSH_BUTTON) {

				@Override
				public void run() {

					TreeViewer treeViewer2 = getTreeViewer();
					Tree tree = treeViewer2.getTree();
					tree.setRedraw(false);
					try {
						// getTreeViewer().collapseToLevel(tree,2);
						/* The default expand level! */
						getTreeViewer().collapseAll();
						contentOutlineViewer.expandToLevel(2);

					} finally {
						tree.setRedraw(true);
					}
					tree.redraw();
				}

			};
			collapseAllAction.setImageDescriptor(IJMacroEditorPlugin.getImageDescriptor("icons/collapseall.png"));
			// Add actions to the toolbar
			IActionBars actionBars = getSite().getActionBars();
			IToolBarManager toolbarManager = actionBars.getToolBarManager();
			toolbarManager.add(collapseAllAction);
			toolbarManager.add(sortAlphabetAction);

			contentOutlineViewer.addSelectionChangedListener(this);

			// Set up the tree viewer.

			tcp = new IJMacroEditorTreeContentProvider();
			contentOutlineViewer.setContentProvider(new IJMacroEditorTreeContentProvider());
			contentOutlineViewer.setInput(nodes);
			contentOutlineViewer.setLabelProvider(new IJMacroEditorLabelProvider());

			// Provide the input to the ContentProvider

			getSite().setSelectionProvider(contentOutlineViewer);

		}

		public void traditional() {
			for (int i = 0; nodes != null && i < nodes.size(); i++) {
				IJMacroEditorOutlineNode node = (IJMacroEditorOutlineNode) nodes.elementAt(i);
				addNode(null, node);
			}
		}

		private void addNode(TreeItem parentItem, IJMacroEditorOutlineNode node) {
			TreeItem item = null;
			if (parentItem == null)
				item = new TreeItem(getTreeViewer().getTree(), SWT.NONE);
			else
				item = new TreeItem(parentItem, SWT.NONE);

			item.setText(node.getName());

			Vector subs = node.getSubCategories();
			for (int i = 0; subs != null && i < subs.size(); i++)
				addNode(item, (IJMacroEditorOutlineNode) subs.elementAt(i));
		}
	}

	public void setMarkerExpression(String expression) {
		this.markerExpression = expression;

	}

}
