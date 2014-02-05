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
package com.eco.bio7.javaeditors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.CompletionProposal;
import org.eclipse.jdt.core.CompletionRequestor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import com.eco.bio7.javaeditor.Bio7EditorPlugin;
import com.eco.bio7.javaeditor.actions.JavaFormatterAction;
import com.eco.bio7.javaeditor.actions.JavaFormatterSelectAction;
import com.eco.bio7.javaeditor.actions.OpenPreferences;
import com.eco.bio7.javaeditor.actions.RSourceConverter;
import com.eco.bio7.javaeditor.actions.SetComment;
import com.eco.bio7.javaeditor.actions.UnsetComment;

/**
 * 
 */
public class JavaEditor extends TextEditor {

	private static final String TEMPLATE_PROPOSALS = "template_proposals_action"; //$NON-NLS-1$
	private ColorManager colorManager;
	Image classIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/class_obj.gif"));
	private Image importIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/imp_obj.png"));
	private Image publicFieldIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/field_public_obj.png"));
	private Image publicMethodIcon = new Image(Display.getCurrent(), getClass().getResourceAsStream("/icons/methpub_obj.gif"));
	private SetComment setcomment;
	private UnsetComment unsetcomment;
	private JavaFormatterAction javaformat;
	private JavaFormatterSelectAction javaselectformat;
	private RSourceConverter rsourceconverter;
	private OpenPreferences preferences;
	private IContentOutlinePage contentOutlinePage;
	public ClassModel currentClassModel;
	public TodoContentProvider tcp;
	private boolean started = true;
	private boolean start = true;
	public CompilationUnit compUnit;
	public final static String EDITOR_MATCHING_BRACKETS = "matchingBrackets";
	public final static String EDITOR_MATCHING_BRACKETS_COLOR = "matchingBracketsColor";

	private TreeViewer contentOutlineViewer;

	public void createPartControl(Composite parent) {

		super.createPartControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.java");

		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(listener);
		// getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
	}

	/*
	 * private IPartListener2 partListener = new IPartListener2() {
	 * 
	 * @Override public void partActivated(IWorkbenchPartReference partRef) { // TODO // Auto-generated // method // stub
	 * 
	 * }
	 * 
	 * public void partBroughtToTop(IWorkbenchPartReference partRef) { // TODO // Auto-generated // method // stub
	 * 
	 * }
	 * 
	 * public void partClosed(IWorkbenchPartReference partRef) { // TODO Auto-generated method stub
	 * 
	 * }
	 * 
	 * public void partDeactivated(IWorkbenchPartReference partRef) { // TODO // Auto-generated // method // stub
	 * 
	 * }
	 * 
	 * @Override public void partOpened(IWorkbenchPartReference partRef) { if (partRef.getId().equals("com.eco.bio7.javaeditors.TemplateEditor")) { System .out.println("opened"+" "+partRef.getId()+" "+partRef.getPartName()); IEditorPart javaEditor = (IEditorPart) partRef.getPage().getActiveEditor();
	 * if (javaEditor != null) {
	 * 
	 * if (javaEditor instanceof JavaEditor) { System.out.println("offen"); //new ModelClassAst().parseAST(javaEditor);
	 * 
	 * }
	 * 
	 * } else{ System.out.println("noch nicht offen"); }
	 * 
	 * }
	 * 
	 * }
	 * 
	 * public void partHidden(IWorkbenchPartReference partRef) { // TODO Auto-generated method stub
	 * 
	 * }
	 * 
	 * public void partVisible(IWorkbenchPartReference partRef) { // TODO Auto-generated method stub
	 * 
	 * }
	 * 
	 * public void partInputChanged(IWorkbenchPartReference partRef) { // TODO // Auto-generated // method // stub
	 * 
	 * }
	 * 
	 * };
	 */

	// the listener we register with the selection service
	private ISelectionListener listener = new ISelectionListener() {
		public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {
			// we ignore our own selections
			if (sourcepart != JavaEditor.this) {
				// showSelection(sourcepart, selection);

				if (selection instanceof IStructuredSelection) {
					IStructuredSelection strucSelection = (IStructuredSelection) selection;
					Object selectedObj = strucSelection.getFirstElement();
					if (selectedObj instanceof ClassMembers) {

						ClassMembers cm = (ClassMembers) selectedObj;
						if (cm != null) {

							int lineNumber = cm.getLineNumber();
							/*
							 * If a line number exist - if a class member of type is available!
							 */
							if (lineNumber > 0) {
								goToLine(JavaEditor.this, lineNumber);
							}

						}

					}
				}

			}
		}

	};

	private static void goToLine(IEditorPart editorPart, int toLine) {
		if ((editorPart instanceof JavaEditor) || toLine <= 0) {

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

	/**
	 * Creates a new template editor.
	 */
	public JavaEditor() {
		super();

		// JavaEditorInstance = this;

		colorManager = new ColorManager();
		setSourceViewerConfiguration(new JavaConfiguration(colorManager, this));
		currentClassModel = new ClassModel();

		Bio7EditorPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {

			public void propertyChange(org.eclipse.jface.util.PropertyChangeEvent event) {

				Bio7EditorPlugin fginstance = Bio7EditorPlugin.getDefault();

				JavaCodeScanner scanner = (JavaCodeScanner) fginstance.getJavaCodeScanner();

				JavaColorProvider provider = Bio7EditorPlugin.getDefault().getJavaColorProvider();
				IPreferenceStore store = Bio7EditorPlugin.getDefault().getPreferenceStore();
				RGB rgbkey = PreferenceConverter.getColor(store, "colourkey");
				RGB rgbkey1 = PreferenceConverter.getColor(store, "colourkey1");
				RGB rgbkey2 = PreferenceConverter.getColor(store, "colourkey2");
				RGB rgbkey3 = PreferenceConverter.getColor(store, "colourkey3");
				RGB rgbkey4 = PreferenceConverter.getColor(store, "colourkey4");
				RGB rgbkey5 = PreferenceConverter.getColor(store, "colourkey5");
				RGB rgbkey6 = PreferenceConverter.getColor(store, "colourkey6");
				RGB rgbkey7 = PreferenceConverter.getColor(store, "colourkey7");
				// RGB rgbkey8 = PreferenceConverter.getColor(store,
				// "colourkey8");

				FontData f = PreferenceConverter.getFontData(store, "colourkeyfont");
				FontData f1 = PreferenceConverter.getFontData(store, "colourkeyfont1");
				FontData f2 = PreferenceConverter.getFontData(store, "colourkeyfont2");
				FontData f3 = PreferenceConverter.getFontData(store, "colourkeyfont3");
				FontData f4 = PreferenceConverter.getFontData(store, "colourkeyfont4");
				FontData f5 = PreferenceConverter.getFontData(store, "colourkeyfont5");
				FontData f6 = PreferenceConverter.getFontData(store, "colourkeyfont6");
				FontData f7 = PreferenceConverter.getFontData(store, "colourkeyfont7");
				// FontData f8 = PreferenceConverter.getFontData(store,
				// "colourkeyfont8");

				scanner.keyword.setData(new TextAttribute(provider.getColor(rgbkey), null, 1, new Font(Display.getCurrent(), f)));
				scanner.type.setData(new TextAttribute(provider.getColor(rgbkey1), null, 1, new Font(Display.getCurrent(), f1)));
				scanner.string.setData(new TextAttribute(provider.getColor(rgbkey2), null, 1, new Font(Display.getCurrent(), f2)));
				scanner.comment.setData(new TextAttribute(provider.getColor(rgbkey3), null, 1, new Font(Display.getCurrent(), f3)));
				scanner.other.setData(new TextAttribute(provider.getColor(rgbkey4), null, 1, new Font(Display.getCurrent(), f4)));
				scanner.operators.setData(new TextAttribute(provider.getColor(rgbkey5), null, 1, new Font(Display.getCurrent(), f5)));
				scanner.braces.setData(new TextAttribute(provider.getColor(rgbkey6), null, 1, new Font(Display.getCurrent(), f6)));
				scanner.numbers.setData(new TextAttribute(provider.getColor(rgbkey7), null, 1, new Font(Display.getCurrent(), f7)));
				// scanner.multiLinecomment.setData(new
				// TextAttribute(provider.getColor(rgbkey8), null, 1, new
				// Font(Display.getCurrent(), f8)));

				if (JavaEditor.this != null) {
					if (JavaEditor.this.getSourceViewer() != null) {
						JavaEditor.this.getSourceViewer().invalidateTextPresentation();
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

		// important: Remove listener when the view is disposed!
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(listener);
		super.dispose();

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

	protected void editorContextMenuAboutToShow(IMenuManager menu) {
		super.editorContextMenuAboutToShow(menu);
		addAction(menu, "ContentAssistProposal");
		menu.add(new Separator());
		addAction(menu, "Add Block Comment");
		addAction(menu, "Remove Block Comment");
		menu.add(new Separator());
		addAction(menu, "Format Java");
		addAction(menu, "Format Selected Source");
		menu.add(new Separator());
		addAction(menu, "Convert R Code");
		menu.add(new Separator());
		addAction(menu, "Preferences");
	}

	protected void createActions() {
		super.createActions();

		IAction action = new TextOperationAction(TemplateMessages.getResourceBundle(), "Editor." + TEMPLATE_PROPOSALS + ".", this, ISourceViewer.CONTENTASSIST_PROPOSALS);
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction(TEMPLATE_PROPOSALS, action);
		markAsStateDependentAction(TEMPLATE_PROPOSALS, true);

		IAction actionProp = new TextOperationAction(JavaEditorMessages.getResourceBundle(), "ContentAssistProposal.", this, ISourceViewer.CONTENTASSIST_PROPOSALS);
		actionProp.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction("ContentAssistProposal", actionProp);

		IAction actionTip = new TextOperationAction(JavaEditorMessages.getResourceBundle(), "ContentAssistTip.", this, ISourceViewer.CONTENTASSIST_CONTEXT_INFORMATION);
		actionTip.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_CONTEXT_INFORMATION);
		setAction("ContentAssistTip", actionTip);

		setcomment = new com.eco.bio7.javaeditor.actions.SetComment("Add Block Comment", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Add Block Comment", setcomment);

		unsetcomment = new com.eco.bio7.javaeditor.actions.UnsetComment("Remove Block Comment", PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		setAction("Remove Block Comment", unsetcomment);

		javaformat = new com.eco.bio7.javaeditor.actions.JavaFormatterAction();
		setAction("Format Java", javaformat);

		javaselectformat = new com.eco.bio7.javaeditor.actions.JavaFormatterSelectAction();
		setAction("Format Selected Source", javaselectformat);

		rsourceconverter = new com.eco.bio7.javaeditor.actions.RSourceConverter();
		setAction("Convert R Code", rsourceconverter);

		preferences = new com.eco.bio7.javaeditor.actions.OpenPreferences();
		setAction("Preferences", preferences);

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
				im = classIcon;

			} else if (element instanceof ClassMembers) {
				ClassMembers cm = (ClassMembers) element;

				switch (cm.getClasstype()) {
				case "import":
					im = importIcon;

					break;
				case "field":
					im = publicFieldIcon;

					break;

				case "method":
					im = publicMethodIcon;

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

	public CompilationUnit getCompUnit() {
		return compUnit;
	}

	public void setCompUnit(CompilationUnit compUnit) {
		this.compUnit = compUnit;
	}

}
