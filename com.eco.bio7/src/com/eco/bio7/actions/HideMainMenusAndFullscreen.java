package com.eco.bio7.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.WorkbenchWindow;

import com.eco.bio7.util.Util;

/**
 * Toggles the fullscreen mode of the Bio7 window and hides the main menus in
 * the fullscreen mode (not on macOS which hides the menu bar itself). The menus
 * are restored whenever the fullscreen mode ends (also with the Eclipse
 * fullscreen command or the window buttons).
 */
public class HideMainMenusAndFullscreen extends Action {

	private static final boolean MAC = "Mac".equals(Util.getOS());

	protected final IWorkbenchWindow window;
	/* The menus hidden by this action (only these are shown again) */
	private final List<IContributionItem> hiddenItems = new ArrayList<IContributionItem>();
	private Shell listening;

	public HideMainMenusAndFullscreen(String text, IWorkbenchWindow window) {
		super(MAC ? text + "  (\u2303\u2318F)" : text);
		this.window = window;
		setId("com.eco.bio7.hide.main.menus.fullscreen");
		/*
		 * Ctrl+Shift+4 (displayed in the menu). On macOS the key of the Bio7 command
		 * (Shift+Command+4) is the screenshot key of macOS: the label shows the key of
		 * the Eclipse fullscreen command (Control+Command+F) which toggles the
		 * fullscreen mode on macOS like this action (the menu bar is hidden by macOS).
		 */
		if (!MAC) {
			setActionDefinitionId("com.eco.bio7.hide.main.menus.fullscreen");
		}
	}

	public void run() {
		Shell shell = window.getShell();
		if (shell == null || shell.isDisposed()) {
			return;
		}
		listen(shell);
		boolean fullscreen = !shell.getFullScreen();
		if (fullscreen && !MAC) {
			hideMenus();
		}
		shell.setFullScreen(fullscreen);
		if (!fullscreen) {
			showMenus();
		}
		shell.requestLayout();
	}

	/* Restores the menus when the fullscreen mode ends in another way */
	private void listen(Shell shell) {
		if (listening == shell) {
			return;
		}
		listening = shell;
		shell.addListener(SWT.Resize, _ -> {
			if (!hiddenItems.isEmpty()) {
				shell.getDisplay().asyncExec(() -> {
					if (!shell.isDisposed() && !shell.getFullScreen()) {
						showMenus();
					}
				});
			}
		});
	}

	private MenuManager menuManager() {
		return window instanceof WorkbenchWindow ? ((WorkbenchWindow) window).getMenuManager() : null;
	}

	private void hideMenus() {
		MenuManager menuManager = menuManager();
		if (menuManager == null) {
			return;
		}
		for (IContributionItem item : menuManager.getItems()) {
			if (item.isVisible()) {
				item.setVisible(false);
				hiddenItems.add(item);
			}
		}
		menuManager.update(true);
	}

	private void showMenus() {
		MenuManager menuManager = menuManager();
		if (menuManager == null || hiddenItems.isEmpty()) {
			return;
		}
		for (IContributionItem item : hiddenItems) {
			item.setVisible(true);
		}
		hiddenItems.clear();
		menuManager.update(true);
	}

}
