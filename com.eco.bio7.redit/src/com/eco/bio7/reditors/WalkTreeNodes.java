package com.eco.bio7.reditors;

import java.util.ArrayList;
import java.util.Stack;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import com.eco.bio7.reditor.outline.REditorOutlineNode;
import com.eco.bio7.util.Util;

public class WalkTreeNodes extends WorkspaceJob {
	private TreeViewer contentOutlineViewer;
	private int lineNumber = 0;
	private ArrayList<TreeItem> selectedItems;
	private Stack<Boolean> treeItemLine = new Stack<Boolean>();
	private boolean found;

	public WalkTreeNodes(TreeViewer contentOutlineViewer, int lineNumber, ArrayList<TreeItem> selectedItems) {
		super("Open Outline");
		this.contentOutlineViewer = contentOutlineViewer;
		this.lineNumber = lineNumber;
		this.selectedItems = selectedItems;

	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub

		Display display = Util.getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				walk();
				selectedItems.clear();
				treeItemLine.clear();
			}
		});
		return Status.OK_STATUS;
	}

	public void walk() {
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
	/*
	 * This method is recursively called to walk all subtrees and compare the
	 * line numbers of selected tree items with the selected line number in the
	 * editor!
	 */

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
						if (((REditorOutlineNode) it.getData() != null)) {
							if (lineNumber == ((REditorOutlineNode) it.getData()).getLineNumber()) {
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
								 * Recursive call of the method for subnodes!
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

}
