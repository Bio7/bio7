/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.reditors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
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
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.text.BadLocationException;
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
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.rosuda.REngine.Rserve.RConnection;
import com.eco.bio7.rbridge.UpdateCompletion;
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.actions.OpenDirDialog;
import com.eco.bio7.reditor.actions.OpenFileDialog;
import com.eco.bio7.reditor.actions.OpenPlotPreferences;
import com.eco.bio7.reditor.actions.OpenPreferences;
import com.eco.bio7.reditor.actions.RefreshLoadedPackagesForCompletion;
import com.eco.bio7.reditor.actions.SaveFileDialog;
import com.eco.bio7.reditor.actions.UnsetComment;
import com.eco.bio7.reditor.actions.color.OpenColorDialog;
import com.eco.bio7.reditor.actions.color.OpenColorDialogRGBToHex;
import com.eco.bio7.reditor.antlr.Parse;
import com.eco.bio7.reditor.antlr.WordMarkerCreation;
import com.eco.bio7.reditor.outline.REditorLabelProvider;
import com.eco.bio7.reditor.outline.REditorOutlineNode;
import com.eco.bio7.reditor.outline.REditorTreeContentProvider;
import com.eco.bio7.rpreferences.template.RCompletionProcessor;

/**
 * 
 */
public class REditor extends TextEditor implements IPropertyChangeListener, UpdateCompletion {

	private static final String TEMPLATE_PROPOSALS = "template_proposals_action";
	private static RConnection rserveConnection;
	private RColorManager colorManager;
	public Action setcomment;
	private UnsetComment unsetcomment;
	private OpenPreferences preferences;
	private RefreshLoadedPackagesForCompletion reloadPackages;
	private OpenPlotPreferences plotPreferences;
	private ProjectionSupport projectionSupport;
	private ProjectionAnnotationModel annotationModel;
	public final static String EDITOR_MATCHING_BRACKETS = "matchingBrackets";
	public final static String EDITOR_MATCHING_BRACKETS_COLOR = "matchingBracketsColor";
	private OpenColorDialog openColorDialog;
	private OpenColorDialogRGBToHex openHexColorDialog;
	private OpenFileDialog openFileDialog;
	private SaveFileDialog saveFileDialog;
	private IContentOutlinePage contentOutlinePage;
	public REditorTreeContentProvider tcp;
	private TreeViewer contentOutlineViewer;
	public Vector<REditorOutlineNode> nodes = new Vector<REditorOutlineNode>();
	public REditorOutlineNode baseNode;// Function category!
	private RConfiguration rconf;
	private Object[] expanded;
	public ProjectionViewer viewer;
	protected ArrayList<TreeItem> selectedItems;
	private IPreferenceStore store;
	final private ScopedPreferenceStore storeWorkbench = new ScopedPreferenceStore(new InstanceScope(), "org.eclipse.ui.workbench");
	private Parse parser;
	// private ColorRegistry colorRegistry;
	public static FontRegistry fontRegistry;
	public static UpdateCompletion updateCompletion;
	private REditor editor;
	public static REditor rEditorInstance;
	private static Shell tempShell;

	public static REditor getREditorInstance() {
		return rEditorInstance;
	}

	public REditor getEditor() {
		return editor;
	}

	public Parse getParser() {
		return parser;
	}

	public ProjectionViewer getViewer() {
		return viewer;
	}

	public RConfiguration getRconf() {
		return rconf;
	}

	/**
	 * A method to activate the current editor.
	 * 
	 * @param editor
	 */
	/**
	 * A method to activate the current as a workaround for the MacOSX editor (code
	 * completion).
	 * 
	 * @param editor
	 */
	public static void activateEditorPage(final IEditorPart editor) {
		if (tempShell != null) {
			if (tempShell.isDisposed() == false) {
				tempShell.dispose();
			}
		}
		tempShell = new Shell();

	}

	/**
	 * Creates a new template editor.
	 */
	public REditor() {
		super();

		/*
		 * Add a new key binding scope for this editor Don't forget to set the
		 * 'symbolicFontName' extension attribute in 'org.eclipse.ui.editors'!
		 */
		setKeyBindingScopes(new String[] { "com.eco.bio7.reditor.REditorScope" });
		Bio7REditorPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);

		colorManager = new RColorManager();
		rconf = new RConfiguration(colorManager, this);
		setSourceViewerConfiguration(rconf);
		// setDocumentProvider(new RDocumentProvider()); not used here (see plugin.xml
		// 'RDocumentSetupParticipant'!
		/*
		 * Set the context to avoid that menus appear in a different editor. E.g.,
		 * refactor dialogs!
		 */
		setEditorContextMenuId("#REditorContext");

	}

	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		parser = new Parse(this);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.reditor");
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(listener);
		viewer = (ProjectionViewer) getSourceViewer();

		projectionSupport = new ProjectionSupport(viewer, getAnnotationAccess(), getSharedColors());
		projectionSupport.install();

		// turn projection mode on
		viewer.doOperation(ProjectionViewer.TOGGLE);

		annotationModel = viewer.getProjectionAnnotationModel();

		getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
		selectedItems = new ArrayList<TreeItem>();
		// ISourceViewer viewer = getSourceViewer();
		store = Bio7REditorPlugin.getDefault().getPreferenceStore();
		// updateFoldingStructure(new ArrayList());
		rEditorInstance = this;
		editor = this;
		// ITextOperationTarget target = (ITextOperationTarget)
		// editor.getAdapter(ITextOperationTarget.class);

		final ITextEditor textEditor = (ITextEditor) editor;
		if (editor instanceof REditor) {

			/*
			 * ((StyledText) editor.getAdapter(Control.class)).addKeyListener(new
			 * KeyListener() {
			 * 
			 * @Override public void keyReleased(KeyEvent e) {
			 * 
			 * }
			 * 
			 * @Override public void keyPressed(KeyEvent event) { closeRPopupTableShell();
			 * 
			 * if (((event.stateMask & SWT.CTRL) == SWT.CTRL) && (event.keyCode == 'z')) {
			 * 
			 * 
			 * 
			 * }
			 * 
			 * else if (((event.stateMask & SWT.CTRL) == SWT.CTRL) && (event.keyCode ==
			 * 'u')) {
			 * 
			 * 
			 * 
			 * }
			 * 
			 * } });
			 */

			((StyledText) editor.getAdapter(Control.class)).addMouseListener(new MouseListener() {

				@Override
				public void mouseDoubleClick(MouseEvent e) {

				}

				@Override
				public void mouseDown(MouseEvent e) {
					if (Util.isMac()) {

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

							if (store.getBoolean("EDITOR_TO_OUTLINE")) {
								int lineNumber = 0;

								try {
									lineNumber = document.getLineOfOffset(offset);
								} catch (BadLocationException e1) {

									e1.printStackTrace();
								}
								/* Only make a tree selection if we don't have parse errors! */
								if (parser.getNumberOfMainParseErrors() == 0) {
									WalkTreeNodes walkTreeJob = new WalkTreeNodes(contentOutlineViewer, lineNumber, selectedItems);

									/*
									 * walkTreeJob.addJobChangeListener(new JobChangeAdapter() { public void
									 * done(IJobChangeEvent event) { if (event.getResult().isOK()) {
									 * 
									 * } else {
									 * 
									 * } } });
									 */
									walkTreeJob.setUser(true);
									walkTreeJob.schedule();
								}

							}

						}

					}
					selectedItems.clear();

				}

				@Override
				public void mouseUp(MouseEvent e) {
					// closeRPopupTableShell();
					// closeRHooverPopupTableShell();
					/*
					 * Hide the code completion tooltip if the mouse is clicked!
					 */
					/*
					 * RCompletionProcessor processor = getRconf().getProcessor(); DefaultToolTip
					 * tip = processor.getTooltip(); if (tip != null) { tip.hide(); }
					 */
				}

			});
			((StyledText) editor.getAdapter(Control.class)).addMouseMoveListener(new MouseMoveListener() {

				@Override
				public void mouseMove(MouseEvent e) {
					// closeRHooverPopupTableShell();

				}

			});
		}

	}

	@Override
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {

		super.handlePreferenceStoreChanged(event);
		// invalidateText();
	}

	public void invalidateText() {
		if (REditor.this != null) {
			if (REditor.this.getSourceViewer() != null) {
				REditor.this.getSourceViewer().invalidateTextPresentation();
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
		Bio7REditorPlugin fginstance = Bio7REditorPlugin.getDefault();
		// RCodeScanner scanner = (RCodeScanner) fginstance.getRCodeScanner();
		IPreferenceStore store = Bio7REditorPlugin.getDefault().getPreferenceStore();

		FontData f = PreferenceConverter.getFontData(store, "colourkeyfont");
		FontData f1 = PreferenceConverter.getFontData(store, "colourkeyfont1");
		FontData f2 = PreferenceConverter.getFontData(store, "colourkeyfont2");
		FontData f3 = PreferenceConverter.getFontData(store, "colourkeyfont3");
		FontData f4 = PreferenceConverter.getFontData(store, "colourkeyfont4");
		FontData f5 = PreferenceConverter.getFontData(store, "colourkeyfont5");
		FontData f6 = PreferenceConverter.getFontData(store, "colourkeyfont6");
		FontData f7 = PreferenceConverter.getFontData(store, "colourkeyfont7");
		FontData f8 = PreferenceConverter.getFontData(store, "colourkeyfont8");

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
		}

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
		String font = storeWorkbench.getString("com.eco.bio7.reditor.reditor.textfont");
		String[] split = font.split("\\|");

		split[2] = Float.toString(f.getHeight());
		StringBuilder builder = new StringBuilder(split[0]);
		for (int i = 1; i < split.length; ++i) {
			builder.append('|').append(split[i]);
		}
		storeWorkbench.setValue("com.eco.bio7.reditor.reditor.textfont", builder.toString());

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

	private IPartListener2 partListener = new IPartListener2() {

		@Override
		public void partActivated(IWorkbenchPartReference partRef) { //
			/*
			 * IEditorPart editor = (IEditorPart)
			 * PlatformUI.getWorkbench().getActiveWorkbenchWindow().
			 * getActivePage().getActiveEditor(); REditor rEditor = (REditor) editor;
			 * rEditor.invalidateText();
			 */
		}

		/*
		 * private void updateHierachyView(IWorkbenchPartReference partRef, final
		 * boolean closed) {
		 * 
		 * }
		 */

		public void partBroughtToTop(IWorkbenchPartReference partRef) { // TODO

		}

		public void partClosed(IWorkbenchPartReference partRef) { // TODO

		}

		public void partDeactivated(IWorkbenchPartReference partRef) { // TODO
																		// //

		}

		@Override
		public void partOpened(IWorkbenchPartReference partRef) {

		}

		public void partHidden(IWorkbenchPartReference partRef) { // TODO

		}

		public void partVisible(IWorkbenchPartReference partRef) { // TODO

		}

		public void partInputChanged(IWorkbenchPartReference partRef) { // TODO

		}

	};
	private OpenDirDialog openDirDialog;

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

	// private Annotation[] oldAnnotations;

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

	protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		ISourceViewer viewer = new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);

		// ensure decoration support has been created and configured.
		getSourceViewerDecorationSupport(viewer);

		return viewer;
	}

	public void dispose() {
		// colorManager.dispose();
		Bio7REditorPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		/*
		 * importIcon.dispose(); publicFieldIcon.dispose(); publicMethodIcon.dispose();
		 */

		colorManager.dispose();
		getSite().getWorkbenchWindow().getPartService().removePartListener(partListener);
		super.dispose();
		RCompletionProcessor rp = getRconf().getProcessor();
		rp.disposeImages();

	}

	protected void editorContextMenuAboutToShow(IMenuManager menu) {

		super.editorContextMenuAboutToShow(menu);
		addAction(menu, "ContentAssistProposal");
		menu.add(new Separator());

		/*
		 * addAction(menu, "Add Plotmarker"); addAction(menu, "Delete Plotmarker");
		 */
		menu.add(new Separator());
		addAction(menu, "Add Comment");
		addAction(menu, "Remove Comment");
		menu.add(new Separator());
		MenuManager submenuColor = new MenuManager("&Color Dialogs");
		menu.add(submenuColor);
		submenuColor.add(openColorDialog);
		submenuColor.add(openHexColorDialog);
		// addAction(menu, "Open RGB Color Dialog");
		// addAction(menu, "Open Hex Color Dialog");
		menu.add(new Separator());
		MenuManager submenuFile = new MenuManager("&File Dialogs");
		menu.add(submenuFile);
		submenuFile.add(openFileDialog);
		submenuFile.add(saveFileDialog);
		submenuFile.add(openDirDialog);
		/*
		 * addAction(menu, "Open File Dialog"); addAction(menu, "Save File Dialog");
		 * addAction(menu, "Open Directory Dialog");
		 */
		menu.add(new Separator());
		addAction(menu, "Refactor");
		menu.add(new Separator());
		addAction(menu, "Reload Packages Completion");
		menu.add(new Separator());
		addAction(menu, "Font Preferences");
		menu.add(new Separator());
		addAction(menu, "Plot Preferences");

	}

	protected void createActions() {
		super.createActions();

		IAction action = new TextOperationAction(TemplateMessages.getResourceBundle(), "Editor." + TEMPLATE_PROPOSALS + ".", //$NON-NLS-1$ //$NON-NLS-2$
				this, ISourceViewer.CONTENTASSIST_PROPOSALS);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction(TEMPLATE_PROPOSALS, action);
		markAsStateDependentAction(TEMPLATE_PROPOSALS, true);

		IAction a = new TextOperationAction(REditorMessages.getResourceBundle(), "ContentAssistProposal.", this, //$NON-NLS-1$
				ISourceViewer.CONTENTASSIST_PROPOSALS);
		a.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction("ContentAssistProposal", a);
		// setActionActivationCode("ContentAssistProposal",' ', -1, SWT.CTRL);

		a = new TextOperationAction(REditorMessages.getResourceBundle(), "ContentAssistTip.", this, ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);
		a.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
		setAction("ContentAssistTip", a);

		/*
		 * setplotmarkers = new com.eco.bio7.reditor.actions.SetMarkers(
		 * "Set Plotmarker", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		 * setAction( "Add Plotmarker", setplotmarkers);
		 * 
		 * deleteplotmarkers = new
		 * com.eco.bio7.reditor.actions.DeletePlotMarkers("Delete Plotmarker",
		 * PlatformUI.getWorkbench().getActiveWorkbenchWindow()); setAction(
		 * "Delete Plotmarker", deleteplotmarkers);
		 */

		setcomment = new com.eco.bio7.reditor.actions.SetComment("Add Comment", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Add Comment", setcomment);

		unsetcomment = new com.eco.bio7.reditor.actions.UnsetComment("Remove Comment", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Remove Comment", unsetcomment);

		openColorDialog = new com.eco.bio7.reditor.actions.color.OpenColorDialog("Open Color Dialog", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Open RGB Color Dialog", openColorDialog);

		openHexColorDialog = new com.eco.bio7.reditor.actions.color.OpenColorDialogRGBToHex("Open Hex Color Dialog", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Open Hex Color Dialog", openHexColorDialog);

		openFileDialog = new com.eco.bio7.reditor.actions.OpenFileDialog("Open File Dialog", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Open File Dialog", openFileDialog);

		saveFileDialog = new com.eco.bio7.reditor.actions.SaveFileDialog("Save File Dialog", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Save File Dialog", saveFileDialog);

		openDirDialog = new com.eco.bio7.reditor.actions.OpenDirDialog("Open Directory Dialog", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Open Directory Dialog", openDirDialog);

		/*
		 * refactor = new com.eco.bio7.reditor.refactor.CompareEditorAction("Refactor"
		 * ,PlatformUI.getWorkbench().getActiveWorkbenchWindow()); setAction("Refactor",
		 * refactor);
		 */
		reloadPackages = new com.eco.bio7.reditor.actions.RefreshLoadedPackagesForCompletion("Reload Packages Completion", this);
		setAction("Reload Packages Completion", reloadPackages);

		preferences = new com.eco.bio7.reditor.actions.OpenPreferences("Font Preferences");
		setAction("Font Preferences", preferences);

		plotPreferences = new com.eco.bio7.reditor.actions.OpenPlotPreferences("Plot Preferences");
		setAction("Plot Preferences", plotPreferences);

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

	public IWorkbenchPage getPage() {
		IWorkbenchPage page = getSite().getPage();

		return page;
	}

	// the listener we register with the selection service
	private ISelectionListener listener = new ISelectionListener() {
		public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {

			// we ignore our own selections
			if (sourcepart != REditor.this) {
				// showSelection(sourcepart, selection);

				if (selection instanceof IStructuredSelection) {
					IStructuredSelection strucSelection = (IStructuredSelection) selection;
					Object selectedObj = strucSelection.getFirstElement();
					if (selectedObj instanceof REditorOutlineNode) {

						REditorOutlineNode cm = (REditorOutlineNode) selectedObj;
						if (cm != null) {

							int lineNumber = cm.getLineNumber();
							/*
							 * If a line number exist - if a class member of type is available!
							 */
							if (lineNumber > 0) {
								goToLine(REditor.this, lineNumber);
							}

						}

					}
				}

			}
		}

	};

	private void goToLine(IEditorPart editorPart, int toLine) {
		if ((editorPart instanceof REditor) || toLine <= 0) {

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
						//editor.selectAndReveal(region.getOffset(), region.getLength());
						ISourceViewer sourceViewer = getSourceViewer();
						int offset = region.getOffset();
						sourceViewer.revealRange(offset, region.getLength());
						StyledText textWidget = sourceViewer.getTextWidget();
						textWidget.setSelection(offset, offset+region.getLength());
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

	public void outlineInputChanged(Vector nodesALt, Vector nodesNew) {

		if (contentOutlineViewer != null && contentOutlineViewer.getTree().isDisposed() == false) {
			/* Store temporary the old expanded elements! */
			expanded = contentOutlineViewer.getExpandedElements();

			TreeViewer viewer = contentOutlineViewer;

			if (viewer != null) {

				Control control = viewer.getControl();
				if (control != null && !control.isDisposed()) {

					control.setRedraw(false);
					/* Create default categories! */

					/* Set the new tree nodes! */
					viewer.setInput(nodesNew);
					/* First item is the file! */
					if (contentOutlineViewer.getTree().getItemCount() > 0) {
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

	}

	/*
	 * This method is recursively called to walk all subtrees and compare the names
	 * of the nodes with the old ones!
	 */

	public void walkTree(TreeItem item) {

		for (int i = 0; i < expanded.length; i++) {

			for (int j = 0; j < item.getItemCount(); j++) {

				TreeItem it = item.getItem(j);

				if (((REditorOutlineNode) it.getData()).getName().equals(((REditorOutlineNode) expanded[i]).getName())) {
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
		baseNode = new REditorOutlineNode("File", 0, "base", null);
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
			sortAlphabetAction.setImageDescriptor(Bio7REditorPlugin.getImageDescriptor("icons/alphab_sort_co.png"));
			Action collapseAllAction = new Action("Collapse", IAction.AS_PUSH_BUTTON) {

				@Override
				public void run() {

					TreeViewer treeViewer2 = getTreeViewer();
					Tree tree = treeViewer2.getTree();
					tree.setRedraw(false);
					try {

						getTreeViewer().collapseAll();
						/* The default expand level! */
						contentOutlineViewer.expandToLevel(2);
						
					} finally {
						tree.setRedraw(true);
					}
					tree.redraw();
				}

			};
			collapseAllAction.setImageDescriptor(Bio7REditorPlugin.getImageDescriptor("icons/collapseall.png"));
			// Add actions to the toolbar
			IActionBars actionBars = getSite().getActionBars();
			IToolBarManager toolbarManager = actionBars.getToolBarManager();
			toolbarManager.add(collapseAllAction);
			toolbarManager.add(sortAlphabetAction);

			contentOutlineViewer.addSelectionChangedListener(this);

			// Set up the tree viewer.

			tcp = new REditorTreeContentProvider();
			contentOutlineViewer.setContentProvider(new REditorTreeContentProvider());
			contentOutlineViewer.setInput(nodes);
			contentOutlineViewer.setLabelProvider(new REditorLabelProvider());

			// Provide the input to the ContentProvider

			getSite().setSelectionProvider(contentOutlineViewer);

		}

		public void traditional() {
			for (int i = 0; nodes != null && i < nodes.size(); i++) {
				REditorOutlineNode node = (REditorOutlineNode) nodes.elementAt(i);
				addNode(null, node);
			}
		}

		private void addNode(TreeItem parentItem, REditorOutlineNode node) {
			TreeItem item = null;
			if (parentItem == null)
				item = new TreeItem(getTreeViewer().getTree(), SWT.NONE);
			else
				item = new TreeItem(parentItem, SWT.NONE);

			item.setText(node.getName());

			Vector subs = node.getSubCategories();
			for (int i = 0; subs != null && i < subs.size(); i++)
				addNode(item, (REditorOutlineNode) subs.elementAt(i));
		}
	}

	public static void setConnection(RConnection c) {

		rserveConnection = c;
	}

	public static RConnection getRserveConnection() {
		return rserveConnection;
	}

	public IMarker[] findMyMarkers(IResource target) {
		String type = "org.eclipse.core.resources.problemmarker";

		IMarker[] markers = null;
		try {
			markers = target.findMarkers(type, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {

			e.printStackTrace();
		}
		return markers;
	}

	/*
	 * The implemented interface class known to the RShellCompletion class! Simply
	 * updates the parser after reloading the code completion from the RShell!
	 */
	public void trigger() {
		Parse parse = getParser();
		if (parse != null) {
			parse.parse();
		}

	}

	public static void setShellCompletion(UpdateCompletion updateTheCompletion) {
		updateCompletion = updateTheCompletion;

	}

	public static UpdateCompletion getUpdateCompletion() {
		return updateCompletion;

	}

}
