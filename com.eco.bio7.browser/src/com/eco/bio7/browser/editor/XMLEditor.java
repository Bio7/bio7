package com.eco.bio7.browser.editor;

import java.util.ArrayList;
import java.util.Stack;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import com.eco.bio7.browser.Activator;
import com.eco.bio7.browser.editor.outline.HTMLEditorLabelProvider;
import com.eco.bio7.browser.editor.outline.HTMLEditorOutlineNode;
import com.eco.bio7.browser.editor.outline.HTMLEditorTreeContentProvider;


public class XMLEditor extends TextEditor  {

	private ColorManager colorManager;
	
	private IContentOutlinePage contentOutlinePage;
	// public ClassModel currentClassModel;
	public HTMLEditorTreeContentProvider tcp;
	private TreeViewer contentOutlineViewer;
	public Vector<HTMLEditorOutlineNode> nodes = new Vector<HTMLEditorOutlineNode>();
	public HTMLEditorOutlineNode baseNode;// Function category!
	//private RConfiguration rconf;

	private Object[] expanded;

	public ProjectionViewer viewer;

	Stack<Boolean> treeItemLine = new Stack<Boolean>();

	protected boolean found;

	protected ArrayList<TreeItem> selectedItems;

	private IPreferenceStore store;

	protected IEditorPart editor;
	

	public XMLEditor() {
		super();
		
		colorManager = new ColorManager();
		setSourceViewerConfiguration(new XMLConfiguration(colorManager,this));
		setDocumentProvider(new XMLDocumentProvider());
		//getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
		//getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(listener);
	}
	
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.reditor");
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(listener);
		//viewer = (ProjectionViewer) getSourceViewer();

		//projectionSupport = new ProjectionSupport(viewer, getAnnotationAccess(), getSharedColors());
		//projectionSupport.install();

		// turn projection mode on
		//viewer.doOperation(ProjectionViewer.TOGGLE);

		//annotationModel = viewer.getProjectionAnnotationModel();

		getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
		selectedItems = new ArrayList<TreeItem>();
		// ISourceViewer viewer = getSourceViewer();
		store = Activator.getDefault().getPreferenceStore();
		// updateFoldingStructure(new ArrayList());
		

		//System.out.println(partRef.getId());
		// IEditorPart editorPart =
		// getSite().getPage().getActiveEditor();

		  editor = this;//partRef.getPage().getActiveEditor();
		//ITextOperationTarget target = (ITextOperationTarget) editor.getAdapter(ITextOperationTarget.class);

		//final ITextEditor textEditor = (ITextEditor) editor;
		if (editor instanceof XMLEditor) {

			((StyledText) editor.getAdapter(Control.class)).addKeyListener(new KeyListener() {

				@Override
				public void keyReleased(KeyEvent e) {

				}

				@Override
				public void keyPressed(KeyEvent event) {
					// if (event.stateMask == SWT.ALT && event.keyCode
					// == 99) { -> CTRL+C
					switch (event.keyCode) {

					case SWT.CR: {

					}
						break;
					}
				}
			});

			((StyledText) editor.getAdapter(Control.class)).addMouseListener(new MouseListener() {

				@Override
				public void mouseDoubleClick(MouseEvent e) {

				}

				@Override
				public void mouseDown(MouseEvent e) {
					/*IDocumentProvider prov = textEditor.getDocumentProvider();
					IEditorInput inp = editor.getEditorInput();
					if (prov != null) {
						IDocument document = prov.getDocument(inp);
						if (document != null) {

							ITextSelection textSelection = (ITextSelection) editor.getSite().getSelectionProvider().getSelection();
							int offset = textSelection.getOffset();
							if (store.getBoolean("MARK_WORDS")) {
								markWords(offset, document, editor);
							}

							int lineNumber = 0;

							try {
								lineNumber = document.getLineOfOffset(offset);
							} catch (BadLocationException e1) {

								e1.printStackTrace();
							}

							TreeItem treeItem = null;
							if (contentOutlineViewer.getTree().isDisposed() == false) {
								if (contentOutlineViewer.getTree().getItemCount() > 0) {
									if (contentOutlineViewer.getTree().getItem(0).isDisposed() == false) {
										treeItem = contentOutlineViewer.getTree().getItem(0);
										contentOutlineViewer.getTree().setRedraw(false);

										// Object[] exp =
										// contentOutlineViewer.getExpandedElements();

										TreePath[] treePaths = contentOutlineViewer.getExpandedTreePaths();
										contentOutlineViewer.expandAll();
										contentOutlineViewer.refresh();
										walkTreeLineNumber(treeItem, lineNumber + 1);

										// contentOutlineViewer.setExpandedElements(expanded);

										contentOutlineViewer.setExpandedTreePaths(treePaths);
										for (int i = 0; i < selectedItems.size(); i++) {
											TreeItem it = (TreeItem) selectedItems.get(i);
											it.setExpanded(true);
											TreeItem parent = it;
											while (parent != null) {
												if (parent.getParentItem() != null) {
													parent = parent.getParentItem();
													parent.setExpanded(true);
												} else {
													break;
												}

											}
											contentOutlineViewer.refresh(it);
										}

										contentOutlineViewer.getTree().setRedraw(true);

									}

								}
							}

						}

					}
					selectedItems.clear();*/

				}

				@Override
				public void mouseUp(MouseEvent e) {

				}

			});
		}

	
		
		
	}
	
	public void dispose() {
		colorManager.dispose();
		super.dispose();
	}
	private IPartListener2 partListener = new IPartListener2() {

		@Override
		public void partActivated(IWorkbenchPartReference partRef) { //
			if (partRef.getId().equals("com.eco.bio7.browser.guieditor")) {
				
			}

		}

		

		/*
		 * This method is recursively called to walk all subtrees and compare
		 * the line numbers of selected tree items with the selected line number
		 * in the editor!
		 */

		

		private void updateHierachyView(IWorkbenchPartReference partRef, final boolean closed) {

		}

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
	public void walkTreeLineNumber(TreeItem item, int lineNumber) {

		if (item.isDisposed() == false) {
			found = false;
			boolean isExpanded = item.getExpanded();

			/* Push the temp info on the stack! */
			treeItemLine.push(isExpanded);
			// if (item.getItemCount() > 0) {
			// item.setExpanded(true);
			// update the viewer
			// contentOutlineViewer.refresh();
			// }
			if (item.isDisposed() == false) {
				for (int j = 0; j < item.getItemCount(); j++) {

					TreeItem it = item.getItem(j);
					if (it.isDisposed() == false) {
						if (((HTMLEditorOutlineNode) it.getData() != null)) {
							if (lineNumber == ((HTMLEditorOutlineNode) it.getData()).getLineNumber()) {
								contentOutlineViewer.getTree().setSelection(it);
								// item.setExpanded(true);
								// update the viewer
								// contentOutlineViewer.refresh();

								selectedItems.add(it);
								found = true;
								if (treeItemLine.isEmpty() == false) {
									treeItemLine.clear();
								}

								break;
							} else {

								/*
								 * Recursive call of the method for
								 * subnodes!
								 */
								// if(treeItemLine.size()>2){
								/* Set recursion depth! */
								// break;
								// }
								walkTreeLineNumber(it, lineNumber);
							}
						}
					}
				}
				if (found == false) {
					if (treeItemLine.isEmpty() == false) {
						if (treeItemLine.peek() == false) {

							// item.setExpanded(false);
							// update the viewer
							// contentOutlineViewer.refresh();

						}
						treeItemLine.pop();
					}
				}
			}
		}

	}
	/*
	 * Here we search for similar words of a selected word in the editor.
	 * The results will be marked!
	 */
	public void markWords(int offset, IDocument doc, IEditorPart editor) {

		int length = 0;
		int minusLength = 0;

		while (true) {
			char c = 0;
			if (offset + length >= 0 && offset + length < doc.getLength()) {

				try {
					c = doc.getChar(offset + length);
				} catch (BadLocationException e) {

					e.printStackTrace();
				}

				if (Character.isLetter(c) == false && (c == '.') == false && Character.isDigit(c) == false && (c == '_') == false)
					break;

				length++;
				if (offset + length >= doc.getLength()) {
					break;
				}
			} else {
				break;
			}
		}
		while (true) {
			char c = 0;
			if (offset + length >= 0 && offset + length < doc.getLength()) {

				try {
					c = doc.getChar(offset + minusLength);
				} catch (BadLocationException e) {

					e.printStackTrace();
				}

				if (Character.isLetter(c) == false && (c == '.') == false && Character.isDigit(c) == false && (c == '_') == false)
					break;

				minusLength--;
				if (offset + minusLength < 0) {
					break;
				}
			} else {
				break;
			}
		}
		final int wordOffset = offset + minusLength + 1;
		final int resultedLength = length - minusLength - 1;

		if (resultedLength > 0) {
			String searchForWord = null;
			ITextOperationTarget target = (ITextOperationTarget) editor.getAdapter(ITextOperationTarget.class);
			if (target instanceof ITextViewer) {
				ITextViewer textViewer = (ITextViewer) target;
				try {
					searchForWord = textViewer.getDocument().get(wordOffset, resultedLength);
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			/*
			 * Display display = PlatformUI.getWorkbench().getDisplay();
			 * display.syncExec(new Runnable() {
			 * 
			 * public void run() { textViewer.setSelectedRange(wordOffset,
			 * resultedLength); } });
			 */

			if (searchForWord != null) {
				IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
				try {
					resource.deleteMarkers("com.eco.bio7.reditor.wordmarker", false, IResource.DEPTH_ZERO);
				} catch (CoreException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				Pattern findWordPattern = Pattern.compile("\\b" + searchForWord + "\\b");
				Matcher matcher = findWordPattern.matcher(doc.get());
				while (matcher.find()) {
					int offsetStart = matcher.start();
					int offsetEnd = matcher.end();
					// do something with offsetStart and offsetEnd
					IMarker marker;

					try {

						marker = resource.createMarker("com.eco.bio7.reditor.wordmarker");
						marker.setAttribute(IMarker.CHAR_START, offsetStart);
						marker.setAttribute(IMarker.CHAR_END, offsetEnd);
					} catch (CoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}

			/*
			 * try { htmlHelpText = textViewer.getDocument().get(wordOffset,
			 * resultedLength); } catch (BadLocationException e) { // TODO
			 * Auto-generated catch block e.printStackTrace(); }
			 */
		} else {
			IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
			try {
				resource.deleteMarkers("com.eco.bio7.reditor.wordmarker", false, IResource.DEPTH_ZERO);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// the listener we register with the selection service
		private ISelectionListener listener = new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {

				// we ignore our own selections
				if (sourcepart != XMLEditor.this) {
					// showSelection(sourcepart, selection);

					if (selection instanceof IStructuredSelection) {
						IStructuredSelection strucSelection = (IStructuredSelection) selection;
						Object selectedObj = strucSelection.getFirstElement();
						if (selectedObj instanceof HTMLEditorOutlineNode) {

							HTMLEditorOutlineNode cm = (HTMLEditorOutlineNode) selectedObj;
							if (cm != null) {

								int lineNumber = cm.getLineNumber();
								/*
								 * If a line number exist - if a class member of
								 * type is available!
								 */
								if (lineNumber > 0) {
									goToLine(XMLEditor.this, lineNumber);
								}

							}

						}
					}

				}
			}

		};

		private static void goToLine(IEditorPart editorPart, int toLine) {
			if ((editorPart instanceof XMLEditor) || toLine <= 0) {

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
		 * This method is recursively called to walk all subtrees and compare the
		 * names of the nodes with the old ones!
		 */

		public void walkTree(TreeItem item) {

			for (int i = 0; i < expanded.length; i++) {

				for (int j = 0; j < item.getItemCount(); j++) {

					TreeItem it = item.getItem(j);

					if (((HTMLEditorOutlineNode) it.getData()).getName().equals(((HTMLEditorOutlineNode) expanded[i]).getName())) {
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
			baseNode = new HTMLEditorOutlineNode("File", 0, "base", null);
			nodes.add(baseNode);

		}

		class MyContentOutlinePage extends ContentOutlinePage {

			public void createControl(Composite parent) {
				super.createControl(parent);

				contentOutlineViewer = getTreeViewer();

				contentOutlineViewer.addSelectionChangedListener(this);

				// Set up the tree viewer.

				tcp = new HTMLEditorTreeContentProvider();
				contentOutlineViewer.setContentProvider(new HTMLEditorTreeContentProvider());
				contentOutlineViewer.setInput(nodes);
				contentOutlineViewer.setLabelProvider(new HTMLEditorLabelProvider());

				// Provide the input to the ContentProvider

				getSite().setSelectionProvider(contentOutlineViewer);

			}

			public void traditional() {
				for (int i = 0; nodes != null && i < nodes.size(); i++) {
					HTMLEditorOutlineNode node = (HTMLEditorOutlineNode) nodes.elementAt(i);
					addNode(null, node);
				}
			}

			private void addNode(TreeItem parentItem, HTMLEditorOutlineNode node) {
				TreeItem item = null;
				if (parentItem == null)
					item = new TreeItem(getTreeViewer().getTree(), SWT.NONE);
				else
					item = new TreeItem(parentItem, SWT.NONE);

				item.setText(node.getName());

				Vector subs = node.getSubCategories();
				for (int i = 0; subs != null && i < subs.size(); i++)
					addNode(item, (HTMLEditorOutlineNode) subs.elementAt(i));
			}
		}
	

}
