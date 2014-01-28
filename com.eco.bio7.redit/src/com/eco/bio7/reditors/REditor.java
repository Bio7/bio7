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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextAttribute;
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.actions.OpenPreferences;
import com.eco.bio7.reditor.actions.UnsetComment;
import com.eco.bio7.reditor.outline.REditorLabelProvider;
import com.eco.bio7.reditor.outline.REditorOutlineNode;
import com.eco.bio7.reditor.outline.REditorTreeContentProvider;

/**
 * 
 */
public class REditor extends TextEditor {

	private static final String TEMPLATE_PROPOSALS = "template_proposals_action";

	private RColorManager colorManager;
	private Image importIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/imp_obj.png"));
	private Image publicFieldIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/field_public_obj.png"));
	private Image publicMethodIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/methpub_obj.gif"));

	public Action setcomment;

	private UnsetComment unsetcomment;

	private OpenPreferences preferences;

	private ProjectionSupport projectionSupport;

	private ProjectionAnnotationModel annotationModel;

	public final static String EDITOR_MATCHING_BRACKETS = "matchingBrackets";

	public final static String EDITOR_MATCHING_BRACKETS_COLOR = "matchingBracketsColor";

	private IContentOutlinePage contentOutlinePage;
	// public ClassModel currentClassModel;
	public REditorTreeContentProvider tcp;
	private TreeViewer contentOutlineViewer;
	public Vector<REditorOutlineNode> nodes = new Vector<REditorOutlineNode>();
	public REditorOutlineNode baseNode;// Function category!
	private RConfiguration rconf;

	public ProjectionViewer viewer;

	public RConfiguration getRconf() {
		return rconf;
	}

	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.reditor");
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(listener);
		viewer = (ProjectionViewer) getSourceViewer();

		projectionSupport = new ProjectionSupport(viewer, getAnnotationAccess(), getSharedColors());
		projectionSupport.install();

		// turn projection mode on
		viewer.doOperation(ProjectionViewer.TOGGLE);

		annotationModel = viewer.getProjectionAnnotationModel();

		// ISourceViewer viewer = getSourceViewer();

		// updateFoldingStructure(new ArrayList());
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

	/**
	 * Creates a new template editor.
	 */
	public REditor() {
		super();

		colorManager = new RColorManager();
		rconf = new RConfiguration(colorManager, this);
		setSourceViewerConfiguration(rconf);

		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor != null) {

			IEditorSite site = editor.getEditorSite();

			IWorkbenchPage page = site.getPage();

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

				FontData f = PreferenceConverter.getFontData(store, "colourkeyfont");
				FontData f1 = PreferenceConverter.getFontData(store, "colourkeyfont1");
				FontData f2 = PreferenceConverter.getFontData(store, "colourkeyfont2");
				FontData f3 = PreferenceConverter.getFontData(store, "colourkeyfont3");
				FontData f4 = PreferenceConverter.getFontData(store, "colourkeyfont4");
				FontData f5 = PreferenceConverter.getFontData(store, "colourkeyfont5");
				FontData f6 = PreferenceConverter.getFontData(store, "colourkeyfont6");
				FontData f7 = PreferenceConverter.getFontData(store, "colourkeyfont7");
				FontData f8 = PreferenceConverter.getFontData(store, "colourkeyfont8");

				scanner.keyword.setData(new TextAttribute(provider.getColor(rgbkey), null, 1, new Font(Display.getCurrent(), f)));
				scanner.type.setData(new TextAttribute(provider.getColor(rgbkey1), null, 1, new Font(Display.getCurrent(), f1)));
				scanner.string.setData(new TextAttribute(provider.getColor(rgbkey2), null, 1, new Font(Display.getCurrent(), f2)));
				scanner.comment.setData(new TextAttribute(provider.getColor(rgbkey3), null, 1, new Font(Display.getCurrent(), f3)));
				scanner.other.setData(new TextAttribute(provider.getColor(rgbkey4), null, 1, new Font(Display.getCurrent(), f4)));
				scanner.operators.setData(new TextAttribute(provider.getColor(rgbkey5), null, 1, new Font(Display.getCurrent(), f5)));
				scanner.braces.setData(new TextAttribute(provider.getColor(rgbkey6), null, 1, new Font(Display.getCurrent(), f6)));
				scanner.numbers.setData(new TextAttribute(provider.getColor(rgbkey7), null, 1, new Font(Display.getCurrent(), f7)));
				scanner.assignment.setData(new TextAttribute(provider.getColor(rgbkey8), null, 1, new Font(Display.getCurrent(), f8)));
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
		importIcon.dispose();
		publicFieldIcon.dispose();
		publicMethodIcon.dispose();

		colorManager.dispose();
		super.dispose();

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

		/*
		 * setplotmarkers = new com.eco.bio7.reditor.actions.SetMarkers("Set Plotmarker", PlatformUI.getWorkbench().getActiveWorkbenchWindow()); setAction("Add Plotmarker", setplotmarkers);
		 * 
		 * deleteplotmarkers = new com.eco.bio7.reditor.actions.DeletePlotMarkers("Delete Plotmarker", PlatformUI.getWorkbench().getActiveWorkbenchWindow()); setAction("Delete Plotmarker", deleteplotmarkers);
		 */

		setcomment = new com.eco.bio7.reditor.actions.SetComment("Add Comment", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Add Comment", setcomment);

		unsetcomment = new com.eco.bio7.reditor.actions.UnsetComment("Remove Comment", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Remove Comment", unsetcomment);

		preferences = new com.eco.bio7.reditor.actions.OpenPreferences();
		setAction("R Preferences", preferences);

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

	private static void goToLine(IEditorPart editorPart, int toLine) {
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
						editor.selectAndReveal(region.getOffset(), region.getLength());
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

		if (contentOutlineViewer != null) {
			Object[] expanded = contentOutlineViewer.getExpandedElements();

			// contentOutlineViewer.getTree();

			TreeViewer viewer = contentOutlineViewer;

			if (viewer != null) {

				Control control = viewer.getControl();
				if (control != null && !control.isDisposed()) {

					control.setRedraw(false);
					/* Create default categories! */

					/* Set the new tree nodes! */
					viewer.setInput(nodesNew);
					// viewer.expandAll();
                    /*To do: walk the whole tree!!*/
					for (int i = 0; i < expanded.length; i++) {
						// for (TreeItem item : getTreeViewer().getTree().getItems()) {
						TreeItem treeItems[] = contentOutlineViewer.getTree().getItems();
						for (int j = 0; j < treeItems.length; j++) {
							TreeItem item = treeItems[j];
							if (expanded[i] instanceof REditorOutlineNode) {
								if (((REditorOutlineNode) item.getData()).getName().equals(((REditorOutlineNode) expanded[i]).getName())) {
									viewer.setExpandedState(item.getData(), true);
									break;
								}
							}
							for (int k = 0; k < item.getItemCount(); k++) {
								TreeItem it=item.getItem(k);
								
								if (expanded[i] instanceof REditorOutlineNode) {
									if (((REditorOutlineNode) it.getData()).getName().equals(((REditorOutlineNode) expanded[i]).getName())) {
										viewer.setExpandedState(it.getData(), true);
										break;
									}
								}
							}
							

							
						}
					}
					control.setRedraw(true);
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

		public void createControl(Composite parent) {
			super.createControl(parent);

			contentOutlineViewer = getTreeViewer();

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

}
