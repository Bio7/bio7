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
		setToolTipText(
				"Runs the macro to the defined breakpoints or to the evaluated\nexpression of the expression breakpoints.");
		//ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/stepinto_co.gif")));
		ImageDescriptor desc = IJMacroEditorPlugin.getImageDescriptor("/icons/ijmacrodebug/changevariablevalue_co.png");
		this.setImageDescriptor(desc);
	}

	public void run() {
		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		IResource resource = (IResource) editore.getEditorInput().getAdapter(IResource.class);
		if (editore != null) {
			IJMacroEditor editor = (IJMacroEditor) editore;

			if (resource != null) {
				Map<Integer, String> map1 = findMyMarkers(resource);
				/* Sorting the Map with a Treemap! */
				Map<Integer, String> map = new TreeMap<Integer, String>(map1);
				counter = 0;
				for (Map.Entry<Integer, String> entry : map.entrySet()) {

					Integer lineNum = entry.getKey();
					String expression = entry.getValue();

					if (counter == markerCount) {
						if (lineNum > 0) {

							if (expression != null) {
								editor.setMarkerExpression(expression);

								/*IDocumentProvider dp = editor.getDocumentProvider();
								IDocument doc = dp.getDocument(editor.getEditorInput());
								
								IRegion reg = null;
								try {
									reg = doc.getLineInformation(lineNum);
								} catch (BadLocationException e1) {
								
									e1.printStackTrace();
								}
								
								editor.selectAndReveal(reg.getOffset()-1, 0);
								
								
								
								editor.runToInsertionPoint();*/

								editor.runToExpression();
							} else {

								IDocumentProvider dp = editor.getDocumentProvider();
								IDocument doc = dp.getDocument(editor.getEditorInput());

								IRegion reg = null;
								try {
									reg = doc.getLineInformation(lineNum);
								} catch (BadLocationException e1) {

									e1.printStackTrace();
								}

								editor.selectAndReveal(reg.getOffset() - 1, 0);

								editor.runToInsertionPoint();
							}

							markerCount++;
							break;
						}
					}
					counter++;
				}

			}
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