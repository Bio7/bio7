/*******************************************************************************
 * Copyright (c) 2004-2019 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.ijmacro.editor.toolbar.debug;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import com.eco.bio7.ijmacro.editors.IJMacroEditor;

import ij.macro.Interpreter;

public class DebugMarkerAction extends Action {

	private IMarker[] markers;
	private int counter;
	private DebugVariablesView debugVariablesView;
	private static int markerCount = 0;

	public static void setMarkerCount(int markerCount) {
		DebugMarkerAction.markerCount = markerCount;
	}

	public DebugMarkerAction(DebugVariablesView debugVariablesView) {
		super("RunToMarker");
		this.debugVariablesView = debugVariablesView;
		setId("RunTomarker");
		setText("Run To Debug Marker");
		setToolTipText("Runs the macro to the next breakpoint (each time its line is reached,\nalso in a loop; a breakpoint with a condition only if it is true).");
		//ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/stepinto_co.gif")));
		ImageDescriptor desc = IJMacroEditorPlugin.getImageDescriptor("/icons/ijmacrodebug/changevariablevalue_co.svg");
		this.setImageDescriptor(desc);
	}

	/* Continues to the next breakpoint (each time its line is entered, also in a loop; a condition: if it is true) */
	public void run() {
		IEditorPart editore = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editore instanceof IJMacroEditor editor) {
			editor.runToBreakpoints();
		}
	}

	public Map<Integer, String> findMyMarkers(IResource target) {
		String type = "com.eco.bio7.ijmacro.editor.debugrulermark";

		try {
			markers = target.findMarkers(type, false, IResource.DEPTH_ZERO);

		} catch (CoreException e) {

			e.printStackTrace();
		}

		Map<Integer, String> map1 = new HashMap<Integer, String>();

		for (int i = 0; i < markers.length; ++i) {
			try {
				map1.put((Integer) markers[i].getAttribute(IMarker.LINE_NUMBER),
						(String) markers[i].getAttribute(IMarker.MESSAGE));

			} catch (CoreException e) {

				e.printStackTrace();
			}
		}

		return map1;
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
}