package com.eco.bio7.floweditor.model;

import java.util.function.Supplier;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;

public class FlowFileDropTargetListener extends
		AbstractTransferDropTargetListener {

	private FlowModelFactory factory = new FlowModelFactory();

	/* The input of the flow editor (where the flow file is) */
	private final Supplier<IEditorInput> flowInput;

	public FlowFileDropTargetListener(EditPartViewer viewer) {
		this(viewer, null);
	}

	public FlowFileDropTargetListener(EditPartViewer viewer, Supplier<IEditorInput> flowInput) {
		super(viewer, FileTransfer.getInstance());
		this.flowInput = flowInput;
	}

	// This File handles the dragging from the file Navigator!
	protected Request createTargetRequest() {

		CreateRequest request = new CreateRequest();

		request.setFactory(factory);
		return request;
	}

	protected void handleDrop() {
		String path = ((String[]) getCurrentEvent().data)[0];
		/*
		 * A file of the workspace: the workspace path (if the flow is in the
		 * workspace); else the path relative to the folder of the flow (the file
		 * below it, e.g., in the same Git repository), else the absolute path.
		 */
		IEditorInput input = flowInput != null ? flowInput.get() : null;
		java.io.File flow = FlowPaths.flowFile(input);
		boolean flowInWorkspace = input == null || input instanceof IFileEditorInput;
		factory.setPath(FlowPaths.storedPath(path, flow, flowInWorkspace));
		super.handleDrop();
	}

	protected void updateTargetRequest() {

		((CreateRequest) getTargetRequest()).setLocation(getDropLocation());
	}
}