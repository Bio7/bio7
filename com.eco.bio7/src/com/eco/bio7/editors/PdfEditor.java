package com.eco.bio7.editors;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.EditorPart;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.rbridge.RServe;

public class PdfEditor extends EditorPart {

	private IEditorSite site;
	private IEditorInput input;

	public void createPartControl(Composite parent) {

		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		boolean useBrowser = store.getBoolean("PDF_USE_BROWSER");
		String openInJavaFXBrowser = store.getString("BROWSER_SELECTION");

		IFile file = ((IFileEditorInput) input).getFile();
		final String selFile = file.getName();

		String theName = selFile.replaceFirst("[.][^.]+$", "");

		String fi = file.getRawLocation().toString();

		String dirPath = new File(fi).getParentFile().getPath().replace("\\", "/");

		RServe.openPDF(dirPath + "/", theName + ".pdf", useBrowser, openInJavaFXBrowser,false);
	}

	public void init(IEditorSite site, IEditorInput input) {
		setSite(site);
		this.site = site;
		this.input = input;
		setInput(input);
		//getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);

	}

	private IPartListener2 partListener = new IPartListener2() {

		@Override
		public void partActivated(IWorkbenchPartReference partRef) { //

		}

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

	public void setFocus() {
		site.getPage().closeEditor(PdfEditor.this, false);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}
}