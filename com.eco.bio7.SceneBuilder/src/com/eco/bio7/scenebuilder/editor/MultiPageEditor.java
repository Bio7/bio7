package com.eco.bio7.scenebuilder.editor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swt.FXCanvas;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.SourceFormatter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.browser.editor.XMLEditor;
import com.eco.bio7.scenebuilder.editor.SkeletonBuffer.FORMAT_TYPE;
import com.eco.bio7.scenebuilder.editor.SkeletonBuffer.TEXT_TYPE;
import com.oracle.javafx.scenebuilder.kit.editor.EditorController;
import com.oracle.javafx.scenebuilder.kit.editor.panel.content.ContentPanelController;
import com.oracle.javafx.scenebuilder.kit.fxom.FXOMDocument;

/**
 * An example showing how to create a multi-page editor. This example has 3 pages:
 * <ul>
 * <li>page 0 contains a nested text editor.
 * <li>page 1 allows you to change the font used in page 2
 * <li>page 2 shows the words in page 0 in sorted order
 * </ul>
 */
public class MultiPageEditor extends MultiPageEditorPart implements IResourceChangeListener {

	/** The text editor used in page 0. */
	private XMLEditor editor;
	protected String content;
	protected String path;
	public static final String ID = "com.eco.bio7.browser.SceneBuilderView"; //$NON-NLS-1$
	public String basePath = ".";
	protected String loadContent;
	protected boolean hasToComplete;
	private Scene scene;
	private FXCanvas canvas;
	protected IDocumentProvider documentProvider;
	protected boolean dirty = false;
	private IFile ifile;
	public EditorController editorController;
	private URL fxmlLocation;
	private String fxmlText;
	private FileEditorInput fileInputEditor;
	protected Timer timer;
	protected Job job;
	protected JFXPanel jfxPanel;
	protected ContentPanelController contentPanelController;
	protected BorderPane pane;
	private Composite composite;
	private FillLayout layout;

	public MultiPageEditor() {
		super();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
         
	}

	

	public void createPage0() {

		composite = new Composite(getContainer(), SWT.NONE);
		layout = new FillLayout();
		composite.setLayout(layout);

		canvas = new FXCanvas(composite, SWT.NONE);
		canvas.setLayout(new FillLayout());

		fileInputEditor = (FileEditorInput) this.getEditorInput();

		ifile = fileInputEditor.getFile();
		IPath location = ifile.getLocation();

		fxmlText = fileToString(location.toOSString());

		try {
			fxmlLocation = location.toFile().toURI().toURL();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/* Execute in JavaFX Thread! */
		Platform.runLater(new Runnable() {

			public void run() {

				editorController = new EditorController();

				contentPanelController = new ContentPanelController(editorController);
				pane = new BorderPane();
				pane.setCenter(contentPanelController.getPanelRoot());
				scene = new Scene(pane);
				try {
					editorController.setFxmlTextAndLocation(fxmlText, fxmlLocation);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				

				canvas.setScene(scene);
				
			}
		});
		
		

		// canvas.setScene(scene);

		int index = addPage(composite);
		setPageText(index, "GUI Editor");

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

	/**
	 * Returns a string representation of the file.
	 * 
	 * @param path
	 * @return
	 */
	public String fileToString(String path) {// this function returns the
		// File as a String
		FileInputStream fileinput = null;
		try {
			fileinput = new FileInputStream(path);
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}
		int filetmp = 0;
		try {
			filetmp = fileinput.available();
		} catch (IOException e) {

			e.printStackTrace();
		}
		byte bitstream[] = new byte[filetmp];
		try {
			fileinput.read(bitstream);
		} catch (IOException e) {

			e.printStackTrace();
		}
		String content = new String(bitstream);
		return content;
	}

	/**
	 * Creates the pages of the multi-page editor.
	 */
	protected void createPages() {
		createPage0();
		createPage1();

	}

	/**
	 * The <code>MultiPageEditorPart</code> implementation of this <code>IWorkbenchPart</code> method disposes all nested editors. Subclasses may extend.
	 */
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);

		super.dispose();
	}

	/**
	 * Saves the multi-page editor's document.
	 */
	public void doSave(IProgressMonitor monitor) {
		SkeletonBuffer buf = new SkeletonBuffer(editorController.getFxomDocument());
		buf.setFormat(FORMAT_TYPE.FULL);
		 buf.setTextType(TEXT_TYPE.WITH_COMMENTS);
        System.out.println(buf.toString());
		getEditor(1).doSave(monitor);
	}

	/**
	 * Saves the multi-page editor's document as another file. Also updates the text for page 0's tab, and updates this multi-page editor's input to correspond to the nested editor's.
	 */
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
		setActivePage(0);
		IDE.gotoMarker(getEditor(0), marker);
	}

	/**
	 * The <code>MultiPageEditorExample</code> implementation of this method checks that the input is an instance of <code>IFileEditorInput</code>.
	 */
	public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput))
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		super.init(site, editorInput);
	}

	/*
	 * (non-Javadoc) Method declared on IEditorPart.
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}

	/**
	 * Calculates the contents of page 2 when the it is activated.
	 */
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);

		if (newPageIndex == 0) {

			final IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(getEditor(1).getEditorInput());

			if (editorController != null) {
				Platform.runLater(new Runnable() {

					@Override
					public void run() {
						try {
							editorController.setFxmlText(doc.get());

							canvas.redraw();

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
				});
			}
		}

		else if (newPageIndex == 1) {

			IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(getEditor(1).getEditorInput());
			if (editorController != null) {

				Source s = new Source(editorController.getFxmlText());
				SourceFormatter sf = new SourceFormatter(s);

				doc.set(sf.toString());

			}
		}

	}

	/**
	 * Closes all project files on project close.
	 */
	public void resourceChanged(final IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
					for (int i = 0; i < pages.length; i++) {
						if (((FileEditorInput) editor.getEditorInput()).getFile().getProject().equals(event.getResource())) {
							IEditorPart editorPart = pages[i].findEditor(editor.getEditorInput());
							pages[i].closeEditor(editorPart, true);
						}
					}
				}
			});
		}
	}

}
