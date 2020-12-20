package com.eco.bio7.floweditor.model;

import com.eco.bio7.floweditor.shapes.ShapesPlugin;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Path;

public class FlowFileDropTargetListener extends
		AbstractTransferDropTargetListener {

	private FlowModelFactory factory = new FlowModelFactory();

	public FlowFileDropTargetListener(EditPartViewer viewer) {
		super(viewer, FileTransfer.getInstance());
	}

	// This File handles the dragging from the file Navigator!
	protected Request createTargetRequest() {

		CreateRequest request = new CreateRequest();

		request.setFactory(factory);
		return request;
	}

	protected void handleDrop() {

		IWorkspaceRoot root = ShapesPlugin.getWorkspace().getRoot();
		String path = ((String[]) getCurrentEvent().data)[0];
		IFile[] files = null;

		files = root.findFilesForLocation(new Path(path));
		if (files.length == 0) {
			// Get a file outside the workspace!
			factory.setPath(path);
			super.handleDrop();

		} else {
			// Get a file from inside the workspace!
			IFile ifile = files[0];

			// Set the relative path to the file!
			factory.setPath(ifile.getFullPath().toString());

			super.handleDrop();
		}
	}

	protected void updateTargetRequest() {

		((CreateRequest) getTargetRequest()).setLocation(getDropLocation());
	}
}