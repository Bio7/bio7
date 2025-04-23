package com.eco.bio7.reditor.actions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.reditor.antlr.refactor.RefactorParse;
import com.eco.bio7.util.Util;

public class ExtractSelectionToFile implements IEditorActionDelegate {

	private ISelection selection;
	private IEditorPart targetEditor;

	public void setActiveEditor(final IAction action, final IEditorPart targetEditor) {
		this.targetEditor = targetEditor;
		action.setActionDefinitionId("com.eco.bio7.reditor.refactor.extract.selection");
		if (targetEditor != null)
			targetEditor.getSite().getKeyBindingService().registerAction(action);
	}

	IResource extractResource(IEditorPart editor) {
		IEditorInput input = editor.getEditorInput();
		if (!(input instanceof IFileEditorInput))
			return null;
		return ((IFileEditorInput) input).getFile();
	}

	public void run(final IAction action) {

		IResource resource = (IResource) targetEditor.getEditorInput().getAdapter(IResource.class);
		if (resource != null) {

			ITextEditor editor = (ITextEditor) targetEditor;
			IDocumentProvider dp = editor.getDocumentProvider();
			IDocument doc = dp.getDocument(editor.getEditorInput());

			ISelectionProvider sp = editor.getSelectionProvider();
			ISelection selectionsel = sp.getSelection();
			ITextSelection selection = (ITextSelection) selectionsel;
			String text = selection.getText();
			String varName = "fileName";

			if (text == null) {

				return;
			}
			if (text.isEmpty()) {

				errorMessage("Nothing selected!");
				return;

			}
			RefactorParse parse = new RefactorParse();
			/* First parse of the selection! */
			boolean errors = parse.parseSource(text, false);
			if (errors == false) {

				InputDialog dlg = new InputDialog(Display.getCurrent().getActiveShell(), "fileName",
						"Enter a file name!", null, null);

				if (dlg.open() == Window.OK) {
					varName = dlg.getValue();
					IFile file = (IFile) extractResource(targetEditor);

					IProject project = file.getProject();
					IFile fil = project.getFile(varName + ".R");

					if (fil.exists() == false) {
						byte[] bytes = text.getBytes();
						InputStream source = new ByteArrayInputStream(bytes);
						try {
							fil.create(source, IResource.NONE, null);
						} catch (CoreException e) {

							e.printStackTrace();
						}

						IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
						IWorkbenchPage page = window.getActivePage();
						IEditorInput editorInput = new FileEditorInput(fil);

						try {
							page.openEditor(editorInput, "com.eco.bio7.reditors.TemplateEditor");
						} catch (PartInitException e1) {

							e1.printStackTrace();
						}

						/* Delete the selected text! */
						try {
							doc.replace(selection.getOffset(), selection.getLength(), "");
						} catch (BadLocationException e) {

							e.printStackTrace();
						}

					} else {
						errorMessage("File with that name already exists!");
					}

				} else {
					return;
				}

			} else {
				errorMessage("Parser error occured!\nPlease select valid R expressions!");

				return;
			}
		}
	}

	public void selectionChanged(final IAction action, final ISelection selection) {
		this.selection = selection;
	}

	/**
	 * Opens a message Dialog with the given text.
	 * 
	 * @param text
	 *            the text for the dialog.
	 */
	public static void errorMessage(final String text) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				MessageBox messageBox = new MessageBox(Util.getShell(),

						SWT.ICON_ERROR);
				messageBox.setText("Error!");
				messageBox.setMessage(text);
				messageBox.open();
			}
		});
	}

}
