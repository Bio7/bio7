package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import com.eco.bio7.collection.Work;

public class OpenViewMenuAction extends Action implements IWorkbenchAction {

	private static final String ID = "com.eco.bio7.openViewMenu";
	private String viewId;

	public OpenViewMenuAction(String text,String viewID) {
		super(text);
		setId(ID);
		viewId = viewID;
	

	}

	public void run() {

		Work.openView(viewId);

	}

	public void dispose() {
	}

}