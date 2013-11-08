package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

import com.eco.bio7.collection.Work;

public class OpenBio7BrowserAction extends Action {

	public OpenBio7BrowserAction(String text, IWorkbenchWindow window) {
		super(text);

		setId("com.eco.bio7.bio7_browser");
		//setActionDefinitionId("com.eco.bio7.open_bio7_browser");
	}

	public void run() {
		
           Work.openView("com.eco.bio7.browser.Browser");
		

	}

}