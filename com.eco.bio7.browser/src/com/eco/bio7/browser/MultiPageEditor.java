package com.eco.bio7.browser;

import java.util.Timer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.text.IDocument;
import org.eclipse.nebula.widgets.richtext.RichTextEditor;
import org.eclipse.nebula.widgets.richtext.RichTextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.browser.editor.XMLEditor;

import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.LoggerProvider;

/**
 * An example showing how to create a multi-page editor. This example has 3
 * pages:
 * <ul>
 * <li>page 0 contains a nested text editor.
 * <li>page 1 allows you to change the font used in page 2
 * <li>page 2 shows the words in page 0 in sorted order
 * </ul>
 */
public class MultiPageEditor extends MultiPageEditorPart implements IResourceChangeListener {

	/** The text editor used in page 0. */
	private XMLEditor editor;
	/** The font chosen in page 1. */
	/** The text widget used in page 2. */
	protected String content;
	protected String path;
	public static final String ID = "com.eco.bio7.browser.BrowserEditorView"; //$NON-NLS-1$
	public String basePath = ".";
	protected String loadContent;
	protected boolean hasToComplete;
	protected IDocumentProvider documentProvider;
	protected boolean dirty = false;

	protected Timer timer;
	protected Job job;
	private RichTextViewer viewer;
	private Text htmlOutput;

	public MultiPageEditor() {
		super();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
		/* Disable logging in Jericho parser! */
		Config.LoggerProvider = LoggerProvider.DISABLED;

	}

	void createPage1() {
		
		try {
			editor = new XMLEditor();
			int index = addPage(editor, getEditorInput());
			setPageText(index, editor.getTitle());
		} catch (PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "Error creating nested text editor", null, e.getStatus());
		}

	}

	public void createPage0() {
		Composite composite = new Composite(getContainer(), SWT.NONE);
		composite.setLayout(new GridLayout(2, true));
		final RichTextEditor editor = new RichTextEditor(composite);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(editor);

		viewer = new RichTextViewer(composite, SWT.BORDER | SWT.WRAP);
		viewer.setWordSplitRegex("\\s|\\-");//wrap after whitespace characters and delimiter
		GridDataFactory.fillDefaults().grab(true, true).span(1, 2).applyTo(viewer);

		htmlOutput = new Text(composite,
				SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().grab(true, false).span(2, 1).hint(SWT.DEFAULT, 100).applyTo(htmlOutput);

		Composite buttonPanel = new Composite(composite, SWT.NONE);
		buttonPanel.setLayout(new RowLayout());
		GridDataFactory.fillDefaults().grab(true, false).applyTo(buttonPanel);

		Button getButton = new Button(buttonPanel, SWT.PUSH);
		getButton.setText("Get text");
		getButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String htmlText = editor.getText();
				viewer.setText(htmlText);
				htmlOutput.setText(htmlText);
			}
		});
		
		int index = addPage(composite);
		setPageText(index, "Nebula");
	}

	/*
	 * Source from: http://rajeshkumarsahanee.wordpress.com/author/rajeshsahanee/
	 */
	public String getInsertHtmlAtCurstorJS(String html) {
		return "insertHtmlAtCursor('" + html + "');" + "function insertHtmlAtCursor(html) {\n" + " var range, node;\n"
				+ " if (window.getSelection && window.getSelection().getRangeAt) {\n"
				+ " window.getSelection().deleteFromDocument();\n" + " range = window.getSelection().getRangeAt(0);\n"
				+ " node = range.createContextualFragment(html);\n" + " range.insertNode(node);\n"
				+ " } else if (document.selection && document.selection.createRange) {\n"
				+ " document.selection.createRange().pasteHTML(html);\n" + " document.selection.clear();" + " }\n"
				+ "}";
	}

	/**
	 * Creates the pages of the multi-page editor.
	 */
	@Override
	protected void createPages() {
		//createPage0();
		createPage1();

		setActivePage(0);

	}

	/**
	 * The <code>MultiPageEditorPart</code> implementation of this
	 * <code>IWorkbenchPart</code> method disposes all nested editors. Subclasses
	 * may extend.
	 */
	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}

	/**
	 * Saves the multi-page editor's document. Also changes the default HTML code
	 * (removes contenteditable arg!).
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		//IEditorInput ed = getEditor(1).getEditorInput();

		/*
		 * IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(ed);
		 * SourceFormatter sf = formatHtml(); String
		 * docc=sf.toString().replace("<body contenteditable=\"true\">","<body>");
		 * doc.set(docc);
		 */
		getEditor(1).doSave(monitor);
	}

	/**
	 * Saves the multi-page editor's document as another file. Also updates the text
	 * for page 0's tab, and updates this multi-page editor's input to correspond to
	 * the nested editor's.
	 */
	@Override
	public void doSaveAs() {
		IEditorPart editor = getEditor(1);
		editor.doSaveAs();
		setPageText(1, editor.getTitle());
		setInput(editor.getEditorInput());
	}

	/*
	 * (non-Javadoc) Method declared on IEditorPart
	 */
	public void gotoMarker(IMarker marker) {
		setActivePage(1);
		IDE.gotoMarker(getEditor(1), marker);
	}

	/**
	 * The <code>MultiPageEditorExample</code> implementation of this method checks
	 * that the input is an instance of <code>IFileEditorInput</code>.
	 */
	@Override
	public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput)) {
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		}
		super.init(site, editorInput);
	}

	/*
	 * (non-Javadoc) Method declared on IEditorPart.
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	/*
	 * Workaround to close and reopen Outline for multipage editor is cited here:
	 * http://stackoverflow.com/questions/24694269/how-to-keep-off-multipageeditor-
	 * with-structuredtexteditor-to-show-outline-view-f I added setActivePage()
	 * method so that the Outline is available if the multipage editor has been
	 * opened.
	 */

	public void refreshOutlineView() {
		// get the activePage
		IWorkbenchPage wp = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		// Find desired view by its visual ID
		IViewPart myView = wp.findView("org.eclipse.ui.views.ContentOutline");

		// Hide the view :
		wp.hideView(myView);
		try {
			// show the view again
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
					.showView("org.eclipse.ui.views.ContentOutline");
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Calculates the contents of page 2 when the it is activated.
	 */
	@Override
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		if (newPageIndex == 0) {
			//IDocument doc = editor.getDocumentProvider().getDocument(getEditor(1).getEditorInput());
			//IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(getEditor(1).getEditorInput());

			
			//viewer.setText(doc.get());
			//htmlOutput.setText(doc.get());

		} else if (newPageIndex == 1) {
			
			
			

		}
		refreshOutlineView();
	}

	/**
	 * Closes all project files on project close.
	 */
	@Override
	public void resourceChanged(final IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
					for (IWorkbenchPage page : pages) {
						if (((FileEditorInput) editor.getEditorInput()).getFile().getProject()
								.equals(event.getResource())) {
							IEditorPart editorPart = page.findEditor(editor.getEditorInput());
							page.closeEditor(editorPart, true);
						}
					}
				}
			});
		}
	}

}
