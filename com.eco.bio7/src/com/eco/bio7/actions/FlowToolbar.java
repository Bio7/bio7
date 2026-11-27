/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.actions;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.UIElement;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.BatchModel;
import com.eco.bio7.util.IconColors;

/**
 * The flow actions of the toolbar (Execute Flow, Stop Flow): while a flow runs
 * the execute button is green and the stop button red, else both have the icon
 * color of the theme.
 */
public final class FlowToolbar {

	/** The command of the execute action. */
	public static final String EXECUTE = "com.eco.bio7.executeflowaction";
	/** The command of the stop action. */
	public static final String STOP = "com.eco.bio7.flowstop";

	private static final String EXECUTE_ICON = "icons/editor/flowexecute.svg";
	private static final String STOP_ICON = "icons/editor/flowstop.svg";
	private static final RGB RUNNING_GREEN = new RGB(0x2e, 0xa0, 0x43);
	private static final RGB RUNNING_RED = new RGB(0xd7, 0x3a, 0x49);

	/* The icons of a running flow (written once to the state location) */
	private static final Map<String, ImageDescriptor> RUNNING = new ConcurrentHashMap<>();

	private FlowToolbar() {
	}

	/**
	 * The handlers of the commands (instead of registering the actions): they run
	 * the actions and update the icons of the toolbar.
	 */
	public static void install(IWorkbenchWindow window, IAction execute, IAction stop) {
		IHandlerService handlers = window.getService(IHandlerService.class);
		handlers.activateHandler(EXECUTE, new Handler(execute, EXECUTE_ICON, RUNNING_GREEN));
		handlers.activateHandler(STOP, new Handler(stop, STOP_ICON, RUNNING_RED));
	}

	/** The icons of the toolbar after a flow started or ended (any thread). */
	public static void refresh() {
		if (!PlatformUI.isWorkbenchRunning()) {
			return;
		}
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (display == null || display.isDisposed()) {
			return;
		}
		display.asyncExec(() -> {
			ICommandService commands = PlatformUI.getWorkbench().getService(ICommandService.class);
			if (commands != null) {
				commands.refreshElements(EXECUTE, null);
				commands.refreshElements(STOP, null);
			}
		});
	}

	/* The icon of a running flow: the svg icon in the given color (else the icon of the theme) */
	private static ImageDescriptor running(String path, RGB color) {
		ImageDescriptor cached = RUNNING.get(path);
		if (cached != null) {
			return cached;
		}
		try {
			URL source = FileLocator.find(Bio7Plugin.getDefault().getBundle(), IPath.fromPortableString(path));
			if (source == null) {
				return Bio7Plugin.getImageDescriptor(path);
			}
			String svg;
			try (InputStream in = source.openStream()) {
				svg = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
			File file = Bio7Plugin.getDefault().getStateLocation().append("icons").append("flowrunning").append(new File(path).getName()).toFile();
			file.getParentFile().mkdirs();
			Files.writeString(file.toPath(), IconColors.recolor(svg, color), StandardCharsets.UTF_8);
			ImageDescriptor descriptor = ImageDescriptor.createFromURL(file.toURI().toURL());
			RUNNING.put(path, descriptor);
			return descriptor;
		} catch (Exception e) {
			return Bio7Plugin.getImageDescriptor(path);
		}
	}

	private static final class Handler extends AbstractHandler implements IElementUpdater {

		private final IAction action;
		private final String icon;
		private final RGB color;

		Handler(IAction action, String icon, RGB color) {
			this.action = action;
			this.icon = icon;
			this.color = color;
		}

		@Override
		public Object execute(ExecutionEvent event) {
			action.run();
			return null;
		}

		@Override
		public void updateElement(UIElement element, @SuppressWarnings("rawtypes") Map parameters) {
			element.setIcon(BatchModel.isRunning() ? running(icon, color) : Bio7Plugin.getImageDescriptor(icon));
		}
	}
}
