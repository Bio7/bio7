package com.eco.bio7.markdownedit.editors;

import java.util.ArrayList;
import java.util.Vector;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import com.eco.bio7.markdownedit.Activator;
import com.eco.bio7.markdownedit.outline.MarkdownEditorLabelProvider;
import com.eco.bio7.markdownedit.outline.MarkdownEditorOutlineNode;
import com.eco.bio7.markdownedit.outline.MarkdownEditorTreeContentProvider;
import com.eco.bio7.reditor.Bio7REditorPlugin;

public class MarkdownEditor extends TextEditor implements IPropertyChangeListener {

	private ColorManager colorManager;
	private IContentOutlinePage contentOutlinePage;
	// public ClassModel currentClassModel;
	public MarkdownEditorTreeContentProvider tcp;
	private TreeViewer contentOutlineViewer;
	public Vector<MarkdownEditorOutlineNode> nodes = new Vector<MarkdownEditorOutlineNode>();
	public MarkdownEditorOutlineNode baseNode;// Function category!
	// private RConfiguration rconf;
	private Object[] expanded;
	protected ArrayList<TreeItem> selectedItems;
	private MarkdownConfiguration markConf;
	final private ScopedPreferenceStore storeWorkbench = new ScopedPreferenceStore(new InstanceScope(),
			"org.eclipse.ui.workbench");
	private ISourceViewer viewer;

	private static String selectedContent;

	public static String getSelectedContent() {
		return selectedContent;
	}

	public MarkdownConfiguration getMarkConf() {
		return markConf;
	}

	public ColorManager getColorManager() {
		return colorManager;
	}

	public ISourceViewer getViewer() {
		return viewer;
	}

	public MarkdownEditor() {
		super();

		colorManager = new ColorManager();
		markConf = new MarkdownConfiguration(colorManager, this, getPreferenceStore());
		setSourceViewerConfiguration(markConf);
		setDocumentProvider(new MarkdownDocumentProvider());
		selectedItems = new ArrayList<TreeItem>();
		/*
		 * Set the context to avoid that menus appear in a different editor. E.g.,
		 * refactor dialogs!
		 */
		setEditorContextMenuId("#RMarkdownEditorContext");

	}

	/* Add a new key binding scope for this editor! */

	protected void initializeKeyBindingScopes() {
		setKeyBindingScopes(new String[] { "com.eco.bio7.markdownedit.MarkdownEditorScope" });
	}

	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.markdowneditor");
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(listener);

		IEditorPart markDownEditor = this;
		ITextOperationTarget target = (ITextOperationTarget) markDownEditor.getAdapter(ITextOperationTarget.class);

		final ITextEditor editor = (ITextEditor) markDownEditor;

		((StyledText) editor.getAdapter(Control.class)).addMouseListener(new MouseListener() {

			@Override
			public void mouseDoubleClick(MouseEvent e) {

			}

			@Override
			public void mouseDown(MouseEvent e) {

				IDocumentProvider dp = editor.getDocumentProvider();
				IDocument doc = dp.getDocument(editor.getEditorInput());

				ISelectionProvider sp = editor.getSelectionProvider();

				ISelection selectionsel = sp.getSelection();

				ITextSelection selection = (ITextSelection) selectionsel;

				int b = selection.getStartLine();

				IRegion reg = null;
				try {
					reg = doc.getLineInformation(b);
				} catch (BadLocationException e1) {

					e1.printStackTrace();
				}

				try {
					selectedContent = doc.get(reg.getOffset(), reg.getLength());
				} catch (BadLocationException ev) {

					ev.printStackTrace();
				}

			}

			@Override
			public void mouseUp(MouseEvent e) {

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
		viewer = getSourceViewer();
        
		/*viewer.getTextWidget().addLineStyleListener(new LineStyleListener() {

			public void lineGetStyle(LineStyleEvent event) {
				// getLineStyle(event);
				System.out.println(event.toString());
			}

		});*/

	}

	private ISelectionListener listener = new ISelectionListener() {
		public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {

			// we ignore our own selections
			if (sourcepart != MarkdownEditor.this) {
				// showSelection(sourcepart, selection);

				if (selection instanceof IStructuredSelection) {
					IStructuredSelection strucSelection = (IStructuredSelection) selection;
					Object selectedObj = strucSelection.getFirstElement();
					if (selectedObj instanceof MarkdownEditorOutlineNode) {

						MarkdownEditorOutlineNode cm = (MarkdownEditorOutlineNode) selectedObj;
						if (cm != null) {

							int lineNumber = cm.getLineNumber();
							/*
							 * If a line number exist - if a class member of type is available!
							 */
							if (lineNumber > 0) {
								goToLine(MarkdownEditor.this, lineNumber);
							}

						}

					}
				}

			}
		}

	};

	@Override
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {

		super.handlePreferenceStoreChanged(event);
		// invalidateText();
	}

	public void invalidateText() {
		if (MarkdownEditor.this != null) {
			if (MarkdownEditor.this.getSourceViewer() != null) {
				MarkdownEditor.this.getSourceViewer().invalidateTextPresentation();
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

		IPreferenceStore store = Activator.getDefault().getPreferenceStore();

		FontData f = PreferenceConverter.getFontData(store, "colourkeyfont");
		FontData f1 = PreferenceConverter.getFontData(store, "colourkeyfont1");
		FontData f2 = PreferenceConverter.getFontData(store, "colourkeyfont2");
		FontData f3 = PreferenceConverter.getFontData(store, "colourkeyfont3");

		/* Restrict the size! */
		if (f.getHeight() + Math.round(fontSize) < 2) {
			return;
		} else if (f1.getHeight() + Math.round(fontSize) < 2) {
			return;
		} else if (f2.getHeight() + Math.round(fontSize) < 2) {
			return;
		} else if (f3.getHeight() + Math.round(fontSize) < 2) {
			return;
		}

		f.setHeight(f.getHeight() + Math.round(fontSize));
		f1.setHeight(f1.getHeight() + Math.round(fontSize));
		f2.setHeight(f2.getHeight() + Math.round(fontSize));
		f3.setHeight(f3.getHeight() + Math.round(fontSize));

		// Font f=

		// Method from:
		// https://github.com/gkorland/Eclipse-Fonts/blob/master/Fonts/src/main/java/fonts/FontsControler.java
		String font = storeWorkbench.getString("com.eco.bio7.reditor.markdown.textfont");
		String[] split = font.split("\\|");

		split[2] = Float.toString(f.getHeight());
		StringBuilder builder = new StringBuilder(split[0]);
		for (int i = 1; i < split.length; ++i) {
			builder.append('|').append(split[i]);
		}
		storeWorkbench.setValue("com.eco.bio7.reditor.markdown.textfont", builder.toString());

		/* Invokes a property change! */
		PreferenceConverter.setValue(store, "colourkeyfont", f);
		PreferenceConverter.setValue(store, "colourkeyfont1", f1);
		PreferenceConverter.setValue(store, "colourkeyfont2", f2);
		PreferenceConverter.setValue(store, "colourkeyfont3", f3);

		invalidateText();

	}

	private static void goToLine(IEditorPart editorPart, int toLine) {
		if ((editorPart instanceof MarkdownEditor) || toLine <= 0) {

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
					TreeItem treeItem = contentOutlineViewer.getTree().getItem(0);
					/* Expand to get access to the subnodes! */
					contentOutlineViewer.setExpandedState(treeItem.getData(), true);
					walkTree(treeItem);
					/* The default expand level! */
					contentOutlineViewer.expandToLevel(2);
					control.setRedraw(true);
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

				if (((MarkdownEditorOutlineNode) it.getData()).getName()
						.equals(((MarkdownEditorOutlineNode) expanded[i]).getName())) {
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
		baseNode = new MarkdownEditorOutlineNode("File", 0, "base", null);
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
			sortAlphabetAction.setImageDescriptor(Activator.getImageDescriptor("icons/alphab_sort_co.png"));
			Action collapseAllAction = new Action("Collapse", IAction.AS_PUSH_BUTTON) {

				@Override
				public void run() {

					TreeViewer treeViewer2 = getTreeViewer();
					Tree tree = treeViewer2.getTree();
					tree.setRedraw(false);
					try {

						getTreeViewer().collapseAll();
						
					} finally {
						tree.setRedraw(true);
					}
					tree.redraw();
				}

			};
			collapseAllAction.setImageDescriptor(Activator.getImageDescriptor("icons/collapseall.png"));
			IActionBars actionBars = getSite().getActionBars();
			IToolBarManager toolbarManager = actionBars.getToolBarManager();
			toolbarManager.add(collapseAllAction);
			toolbarManager.add(sortAlphabetAction);

			contentOutlineViewer.addSelectionChangedListener(this);

			// Set up the tree viewer.

			tcp = new MarkdownEditorTreeContentProvider();
			contentOutlineViewer.setContentProvider(new MarkdownEditorTreeContentProvider());
			contentOutlineViewer.setInput(nodes);
			contentOutlineViewer.setLabelProvider(new MarkdownEditorLabelProvider());

			// Provide the input to the ContentProvider

			getSite().setSelectionProvider(contentOutlineViewer);

		}

		public void traditional() {
			for (int i = 0; nodes != null && i < nodes.size(); i++) {
				MarkdownEditorOutlineNode node = (MarkdownEditorOutlineNode) nodes.elementAt(i);
				addNode(null, node);
			}
		}

		private void addNode(TreeItem parentItem, MarkdownEditorOutlineNode node) {
			TreeItem item = null;
			if (parentItem == null)
				item = new TreeItem(getTreeViewer().getTree(), SWT.NONE);
			else
				item = new TreeItem(parentItem, SWT.NONE);

			item.setText(node.getName());

			Vector subs = node.getSubCategories();
			for (int i = 0; subs != null && i < subs.size(); i++)
				addNode(item, (MarkdownEditorOutlineNode) subs.elementAt(i));
		}
	}

	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}

}
