package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindow;

import com.eco.bio7.collection.Work;
import com.eco.bio7.rcp.ApplicationActionBarAdvisor;
import com.eco.bio7.time.Time;

public class Start extends Action implements IMenuCreator {

	protected final IWorkbenchWindow window;

	private Menu fMenu;

	public Start(String text, IWorkbenchWindow window) {
		super(text, AS_DROP_DOWN_MENU);
		this.window = window;

		setId("com.eco.bio7.start");
		setActionDefinitionId("com.eco.bio7.start");
		setMenuCreator(this);
		this.setToolTipText("Start/Stop the repeated time based invocation\n of the Java run method if available!");
		setImageDescriptor(com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/maintoolbar/play_pause.png"));

	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);

		MenuItem menuItem = new MenuItem(fMenu, SWT.PUSH);
		menuItem.setText("Set Time Interval");

		menuItem.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {

				Work.openView("com.eco.bio7.control");

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

		});

		new MenuItem(fMenu, SWT.SEPARATOR);

		MenuItem resetItem = new MenuItem(fMenu, SWT.PUSH);
		resetItem.setText("Counter Reset");
		resetItem.addListener(SWT.Selection, e -> ApplicationActionBarAdvisor.getCounterreset().run());
		return fMenu;
	}

	public Menu getMenu(Menu parent) {
		return null;
	}

	public void run() {
		if (Time.isPause()) {
			this.setChecked(true);
			Time.setPause(false);
		} else {
			this.setChecked(false);
			Time.setPause(true);
		}

	}

	@Override
	public void dispose() {

	}

}
