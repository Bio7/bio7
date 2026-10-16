/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.collection;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;

import ij.Executer;

/**
 * Docks the window of the SWTImageJ LLM Assistant plugin into a Bio7 custom
 * view, without changes to the plugin. The plugin builds its controls in a
 * shell ("LLM Assistant ..."). When this shell is opened (it is transparent
 * until then), its controls are moved into a custom view and the (empty) shell
 * is hidden. The plugin still
 * uses the hidden shell, e.g., for its dialogs, which are centered on the
 * view. Closing the view closes the assistant. The tool bar of the view can
 * open the assistant again as a window.
 */
public final class AssistantDocking {

	/* The title of the chat shell of the plugin is "LLM Assistant <version> - SWTImageJ" (not the dialogs "LLM Assistant - ...")! */
	private static final String TITLE_START = "LLM Assistant ";
	private static final String TITLE_END = " - SWTImageJ";
	/* The menu command of the plugin which opens the chat! */
	private static final String CHAT_COMMAND = "Chat...";
	private static final String VIEW_NAME = "LLM Assistant";
	private static final String DOCKED = "com.eco.bio7.assistant.docked";
	private static final String UNDOCKED = "com.eco.bio7.assistant.undocked";

	private static boolean installed;
	/* The docked shell and the view composite (null if not docked)! */
	private static Shell shell;
	private static Composite content;

	private AssistantDocking() {
	}

	/** Installs the docking (in the UI thread, once). */
	public static void install(Display display) {
		if (installed) {
			return;
		}
		installed = true;
		/*
		 * Show is sent before the shell of the plugin is displayed. The shell is made
		 * transparent, so that it does not flash up before it is docked!
		 */
		display.addFilter(SWT.Show, e -> {
			if (e.widget instanceof Shell && isAssistant((Shell) e.widget)) {
				Shell s = (Shell) e.widget;
				if (s.getData(DOCKED) == null && s.getData(UNDOCKED) == null) {
					s.setData(DOCKED, Boolean.TRUE);
					s.setAlpha(0);
					display.asyncExec(() -> dock(s));
				}
			}
		});
		/* The chat command of an open (docked) assistant shows the view! */
		try {
			Executer.addCommandListener(command -> {
				if (CHAT_COMMAND.equals(command) && shell != null && !shell.isDisposed() && content != null) {
					display.asyncExec(AssistantDocking::showView);
				}
				return command;
			});
		} catch (RuntimeException | LinkageError e) {
			/* Without ImageJ the view is not shown again by the command! */
		}
	}

	private static boolean isAssistant(Shell s) {
		if (s.isDisposed() || s.getParent() != null) {
			return false;
		}
		String title = s.getText();
		return title.startsWith(TITLE_START) && title.endsWith(TITLE_END);
	}

	/* Moves the controls of the shell into a custom view! */
	private static void dock(Shell s) {
		if (s.isDisposed() || shell != null) {
			return;
		}
		Control[] children = s.getChildren();
		for (Control c : children) {
			if (!c.isReparentable()) {
				/* The platform cannot move the controls, the assistant stays a window! */
				s.setData(UNDOCKED, Boolean.TRUE);
				s.setData(DOCKED, null);
				s.setAlpha(255);
				return;
			}
		}
		Composite parent = new CustomView().getComposite(VIEW_NAME, s.getImage());
		if (parent == null) {
			s.setData(UNDOCKED, Boolean.TRUE);
			s.setData(DOCKED, null);
			s.setAlpha(255);
			return;
		}
		Layout layout = s.getLayout();
		parent.setLayout(layout);
		s.setLayout(null);
		for (Control c : children) {
			c.setParent(parent);
		}
		s.setVisible(false);
		shell = s;
		content = parent;
		parent.layout(true, true);

		Listener sync = e -> syncBounds();
		parent.addListener(SWT.Resize, sync);
		parent.addListener(SWT.Move, sync);
		syncBounds();
		/*
		 * The plugin updates its editor list (and the "Auto" target) when its shell is
		 * activated, but the hidden shell is never activated. The shell is notified
		 * when the view gets the focus and when editors are opened, closed or
		 * activated in the workbench!
		 */
		parent.addListener(SWT.Activate, e -> notifyEditorsChanged());
		IWorkbenchPage page = viewPage(parent);
		if (page != null) {
			IPartListener2 editorListener = new IPartListener2() {
				@Override
				public void partActivated(IWorkbenchPartReference ref) {
					notifyEditorsChanged();
				}

				@Override
				public void partOpened(IWorkbenchPartReference ref) {
					if (ref instanceof IEditorReference) {
						notifyEditorsChanged();
					}
				}

				@Override
				public void partClosed(IWorkbenchPartReference ref) {
					if (ref instanceof IEditorReference) {
						notifyEditorsChanged();
					}
				}

				@Override
				public void partInputChanged(IWorkbenchPartReference ref) {
					if (ref instanceof IEditorReference) {
						notifyEditorsChanged();
					}
				}
			};
			page.addPartListener(editorListener);
			parent.addListener(SWT.Dispose, e -> page.removePartListener(editorListener));
		}
		/* Closing the view closes the assistant! */
		Listener closeAssistant = e -> {
			Shell docked = shell;
			shell = null;
			content = null;
			if (docked != null && !docked.isDisposed()) {
				docked.dispose();
			}
		};
		parent.addListener(SWT.Dispose, closeAssistant);
		/* The assistant is closed by the plugin (shell disposed)! */
		s.addListener(SWT.Dispose, e -> {
			if (content == parent) {
				shell = null;
				content = null;
				parent.removeListener(SWT.Dispose, closeAssistant);
				if (!parent.isDisposed()) {
					closeView(parent);
				}
			}
		});
		addUndockAction(parent, closeAssistant);
		/* The input field of the plugin gets the focus! */
		for (Control c : children) {
			if (c.setFocus()) {
				break;
			}
		}
	}

	/* Lets the plugin refresh its editor list (later, so that the workbench has finished the part change)! */
	private static void notifyEditorsChanged() {
		Shell s = shell;
		if (s == null || s.isDisposed()) {
			return;
		}
		s.getDisplay().asyncExec(() -> {
			if (!s.isDisposed() && s == shell) {
				/*
				 * Only the Activate listeners of the shell are called. notifyListeners would
				 * also run the Activate filters of the display: the workbench would then take
				 * the hidden shell as the active (dialog) shell and disable the key bindings
				 * of the editors!
				 */
				Event event = new Event();
				event.type = SWT.Activate;
				event.display = s.getDisplay();
				event.widget = s;
				event.time = (int) System.currentTimeMillis();
				for (Listener listener : s.getListeners(SWT.Activate)) {
					listener.handleEvent(event);
				}
			}
		});
	}

	private static IWorkbenchPage viewPage(Composite parent) {
		Object view = parent.getData(CustomViewFullscreen.VIEW_KEY);
		return view instanceof CustomView ? ((CustomView) view).getSite().getPage() : null;
	}

	/* The hidden shell has the bounds of the view, so that dialogs of the plugin are centered on the view! */
	private static void syncBounds() {
		if (shell == null || shell.isDisposed() || content == null || content.isDisposed()) {
			return;
		}
		Rectangle r = content.getClientArea();
		Point p = content.toDisplay(r.x, r.y);
		shell.setBounds(p.x, p.y, Math.max(1, r.width), Math.max(1, r.height));
	}

	/* A tool bar action of the view which opens the assistant again as a window! */
	private static void addUndockAction(Composite parent, Listener closeAssistant) {
		Object view = parent.getData(CustomViewFullscreen.VIEW_KEY);
		if (!(view instanceof CustomView)) {
			return;
		}
		CustomView customView = (CustomView) view;
		IToolBarManager tbm = customView.getViewSite().getActionBars().getToolBarManager();
		Action undock = new Action("Open as Window") {
			@Override
			public void run() {
				undock(parent, closeAssistant);
			}
		};
		undock.setToolTipText("Open the LLM Assistant in a separate window");
		tbm.add(undock);
		customView.getViewSite().getActionBars().updateActionBars();
	}

	/* Moves the controls back into the shell of the plugin and closes the view! */
	private static void undock(Composite parent, Listener closeAssistant) {
		Shell s = shell;
		if (s == null || s.isDisposed() || parent.isDisposed()) {
			return;
		}
		parent.removeListener(SWT.Dispose, closeAssistant);
		shell = null;
		content = null;
		s.setData(UNDOCKED, Boolean.TRUE);
		s.setData(DOCKED, null);
		s.setLayout(parent.getLayout());
		for (Control c : parent.getChildren()) {
			c.setParent(s);
		}
		Rectangle b = s.getBounds();
		s.setBounds(b.x, b.y, Math.max(b.width, 500), Math.max(b.height, 400));
		s.layout(true, true);
		s.setAlpha(255);
		s.setVisible(true);
		s.forceActive();
		closeView(parent);
	}

	/* Brings the view of the docked assistant to front! */
	private static void showView() {
		if (content == null || content.isDisposed()) {
			return;
		}
		Object view = content.getData(CustomViewFullscreen.VIEW_KEY);
		if (view instanceof CustomView) {
			IWorkbenchPage page = ((CustomView) view).getSite().getPage();
			page.activate((CustomView) view);
		}
	}

	private static void closeView(Composite parent) {
		Object view = parent.getData(CustomViewFullscreen.VIEW_KEY);
		if (view instanceof CustomView && PlatformUI.isWorkbenchRunning()) {
			CustomView customView = (CustomView) view;
			IWorkbenchPage page = customView.getSite().getPage();
			if (page != null) {
				page.hideView(customView);
			}
		}
	}
}
