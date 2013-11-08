/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/


/*package com.eco.bio7.actions;




import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.update.search.BackLevelFilter;
import org.eclipse.update.search.EnvironmentFilter;
import org.eclipse.update.search.UpdateSearchRequest;
import org.eclipse.update.search.UpdateSearchScope;
import org.eclipse.update.ui.UpdateJob;
import org.eclipse.update.ui.UpdateManagerUI;

public class AddExtensionsAction extends Action implements IAction {
	private IWorkbenchWindow window;

	public AddExtensionsAction(IWorkbenchWindow window) {
		this.window = window;
		setId("com.eco.bio7.newExtensions");
		setText("&Add Extensions...");
		setToolTipText("Search for new extensions  for Bio7");
		
	}

	public void run() {
		BusyIndicator.showWhile(window.getShell().getDisplay(), new Runnable() {
			public void run() {
				UpdateJob job = new UpdateJob("Search for new extensions",
						getSearchRequest());
				UpdateManagerUI.openInstaller(window.getShell(), job);
			}
		});
	}

	private UpdateSearchRequest getSearchRequest() {
		UpdateSearchRequest result = new UpdateSearchRequest(
				UpdateSearchRequest.createDefaultSiteSearchCategory(),
				new UpdateSearchScope());
		result.addFilter(new BackLevelFilter());
		result.addFilter(new EnvironmentFilter());
		UpdateSearchScope scope = new UpdateSearchScope();
		
		try {
			scope.addSearchSite("Ubion", new URL("http://ubion.ion.ag/update/noa4e"), null);
			scope.addSearchSite("Knime", new URL("http://www.knime.org/update"),null);
			scope.addSearchSite("Wicked Shell", new URL("http://www.wickedshell.net/updatesite"),null);
			
		} catch (MalformedURLException e) {
			
		}
		result.setScope(scope);
		return result;
	}
}
*/