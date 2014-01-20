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
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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

import com.eco.bio7.reditor.outline.ClassMembers;
import com.eco.bio7.reditor.outline.ClassModel;
import com.eco.bio7.reditor.outline.MainClass;
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.actions.OpenPreferences;
import com.eco.bio7.reditor.actions.UnsetComment;

/**
 * 
 */
public class REditor extends TextEditor {

	private static final String TEMPLATE_PROPOSALS = "template_proposals_action";

	private RColorManager colorManager;
	private Image classIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/class_obj.gif"));
	private Image importIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/imp_obj.png"));
	private Image publicFieldIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/field_public_obj.png"));
	private Image publicMethodIcon= new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/methpub_obj.gif"));

	/*public Action setplotmarkers;

	public Action deleteplotmarkers;*/

	public Action setcomment;

	private UnsetComment unsetcomment;

	private OpenPreferences preferences;

	private ProjectionSupport projectionSupport;

	private ProjectionAnnotationModel annotationModel;
	
	public final static String EDITOR_MATCHING_BRACKETS = "matchingBrackets";
	
	public final static String EDITOR_MATCHING_BRACKETS_COLOR= "matchingBracketsColor";
	
	private IContentOutlinePage contentOutlinePage;
	public ClassModel currentClassModel;
	public TodoContentProvider tcp;
	private TreeViewer contentOutlineViewer;

	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.reditor");
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(listener);
		ProjectionViewer viewer =(ProjectionViewer)getSourceViewer();

	    projectionSupport = new ProjectionSupport(viewer,getAnnotationAccess(),getSharedColors());
	    projectionSupport.install();

	    //turn projection mode on
	    viewer.doOperation(ProjectionViewer.TOGGLE);

	    annotationModel = viewer.getProjectionAnnotationModel();
	    
	    
	   
	    //updateFoldingStructure(new ArrayList());
	}
	
	
	
	//private Annotation[] oldAnnotations;
	
	
	
	
	public void updateFoldingStructure(ArrayList positions)
	{
		Annotation[] deletions=computeDifferences(annotationModel,positions);
		
		Annotation[] annotations = new Annotation[positions.size()];
		
		//this will hold the new annotations along
		//with their corresponding positions
		HashMap newAnnotations = new HashMap();
		
		for(int i =0;i<positions.size();i++)
		{
			ProjectionAnnotation annotation = new ProjectionAnnotation();
			
			newAnnotations.put(annotation,positions.get(i));
			
			annotations[i]=annotation;
		}
		
		
		
		
		annotationModel.modifyAnnotations(deletions,newAnnotations,null);
		
		//oldAnnotations=annotations;
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
	
	protected ISourceViewer createSourceViewer(Composite parent,
            IVerticalRuler ruler, int styles)
    {
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
		setSourceViewerConfiguration(new RConfiguration(colorManager,this));
		
		
		
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
		classIcon.dispose();
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
	
	public  IWorkbenchPage getPage(){
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
						if (selectedObj instanceof ClassMembers) {

							ClassMembers cm = (ClassMembers) selectedObj;
							if (cm != null) {

								int lineNumber = cm.getLineNumber();
								/*
								 * If a line number exist - if a class member of
								 * type is available!
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
			IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
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
	public Object getAdapter(Class key) {
		if (key.equals(IContentOutlinePage.class)) {

			return getContentOutlinePage();

		} else {
			return super.getAdapter(key);
		}
	}

	public void outlineInputChanged(ClassModel classModelOld, ClassModel classModelNew) {

		if (contentOutlineViewer != null) {
			Object[] expanded = contentOutlineViewer.getExpandedElements();

			// contentOutlineViewer.getTree();

			TreeViewer viewer = contentOutlineViewer;
			
			
		

			if (viewer != null) {
				
				Control control = viewer.getControl();
				if (control != null && !control.isDisposed()) {

					control.setRedraw(false);

					viewer.setInput(classModelNew);
					viewer.expandAll();

					control.setRedraw(true);
				}
			}
		}

	}

	public IContentOutlinePage getContentOutlinePage() {
		if (contentOutlinePage == null) {
			// The content outline is just a tree.
			//
			class MyContentOutlinePage extends ContentOutlinePage {

				public void createControl(Composite parent) {
					super.createControl(parent);
					contentOutlineViewer = getTreeViewer();

					contentOutlineViewer.addSelectionChangedListener(this);

					// Set up the tree viewer.

					tcp = new TodoContentProvider();
					contentOutlineViewer.setContentProvider(tcp);
					contentOutlineViewer.setInput(currentClassModel);
					contentOutlineViewer.setLabelProvider(new TodoLabelProvider());

					// Provide the input to the ContentProvider

					getSite().setSelectionProvider(contentOutlineViewer);

				}
			}

			contentOutlinePage = new MyContentOutlinePage();

		}

		return contentOutlinePage;
	}

	class TodoLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof MainClass) {
				MainClass category = (MainClass) element;
				return category.getName();
			}
			return ((ClassMembers) element).getSummary();
		}

		@Override
		public Image getImage(Object element) {
			Image im = null;
			if (element instanceof MainClass) {
               im=classIcon;
				
			} else if (element instanceof ClassMembers) {
				ClassMembers cm = (ClassMembers) element;

				switch (cm.getClasstype()) {
				case "import":
					im=importIcon;
					
					break;
				case "field":
					im=publicFieldIcon;
					
					break;

				case "method":
					im=publicMethodIcon;
					
					break;

				default:
					break;
				}

			}
			return im;
		}

	}

	class TodoContentProvider implements ITreeContentProvider {

		private ClassModel model;

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			this.model = (ClassModel) newInput;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return model.getCategories().toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof MainClass) {
				MainClass category = (MainClass) parentElement;
				return category.getTodos().toArray();
			}
			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof MainClass) {
				return true;
			}
			return false;
		}

	}

}
