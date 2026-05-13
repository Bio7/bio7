/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.rbridge.debug;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.rosuda.REngine.Rserve.RConnection;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RServe;

/*
 * DebugRScript sets up an R debugging session from within Bio7.
 * 
 * When the user clicks the Debug button, this class:
 * 
 * 1. Saves the active editor and reads the R source file.
 * 2. Finds all debug breakpoint markers set by the user in the editor ruler.
 * 3. For each breakpoint, creates a modified temp file where:
 *    - The entire script is wrapped in a function (.bio7debugfunc) so that
 *      R's n/s/f/c debug commands can step line-by-line. Without the
 *      function wrapper, R executes all top-level expressions at once.
 *    - A browser() call (or conditional browser() for conditional breakpoints)
 *      is injected at the breakpoint line, causing R to pause there.
 * 4. Sources the temp file to define .bio7debugfunc, then calls it.
 *    R hits browser() and enters Browse mode, waiting for debug commands.
 * 5. Highlights the breakpoint line in the editor with a debug marker.
 * 
 * The temp file layout relative to the original source:
 * 
 *   Line 1:  .bio7debugfunc <- function() {   (wrapper, +1 offset)
 *   Line 2:  <original line 1>
 *   ...
 *   Line N:  browser()                         (injected, +1 offset)
 *   ...
 *   Line M:  }                                 (wrapper close)
 * 
 * This means all line numbers reported by R from the temp file are
 * shifted by +2 compared to the original source (one for the wrapper,
 * one for browser()). DebugProgress compensates for this offset when
 * mapping line numbers back to the editor.
 * 
 * Extends Action so it can be used as a toolbar/menu action in Eclipse.
 */
public class DebugRScript extends Action {

	private IEditorPart part;
	private IMarker[] markers;
	boolean untrace = false;
	private IEditorPart editor;
	private boolean errorFunction = false;

	public DebugRScript() {
		super("Debug");

		setId("Debug");
		setText("Debug Trace Action - Insert debugging code at chosen places in any function.");

		ImageDescriptor desc = Bio7Plugin.getImageDescriptor("/icons/rdebug/rundebug.png");
		this.setImageDescriptor(desc);
	}

	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
	}

	public void run() {
		errorFunction = false;
		/* End any previous debug session */
		DebugProgress.endSession();
		final IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();

		/* Automatically disconnect from Rserve for debugging! */
		if (RServe.isAlive()) {
			Bio7Action.callRserve();
			store.setValue("RSERVE_ALIVE_DEBUG", true);
		} else {
			store.setValue("RSERVE_ALIVE_DEBUG", false);
		}

		editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor.isDirty()) {
			editor.doSave(new NullProgressMonitor());
		}

		final IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);

		RConnection d = RServe.getConnection();
		IEditorInput editorInput = editor.getEditorInput();
		IFile aFile = null;

		if (editorInput instanceof IFileEditorInput) {
			aFile = ((IFileEditorInput) editorInput).getFile();
		}
		final String loc = aFile.getLocation().toString();

		if (d == null) {

			final String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

			if (selectionConsole.equals("R")) {

				if (resource != null) {
					Map<Integer, String> map1 = findMyMarkers(resource);
					final Map<Integer, String> map = new TreeMap<Integer, String>(map1);

					Job debugJob = new Job("R Debug Setup") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {

							for (Map.Entry<Integer, String> entry : map.entrySet()) {

								final int lineNum = entry.getKey();
								final String expression = entry.getValue();

								if (lineNum > 0) {

									ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();

									/* Suppress the R prompt during setup */
									con.pipeToRConsole("options(prompt=\" \")");

									if (expression == null) {
										/*
										 * browser() injection with function wrapper:
										 * Wrapping in a function is required because R's n (next)
										 * only steps line-by-line inside a function body.
										 */
										con.pipeToRConsole(".bio7tempenv <- new.env()");
										con.pipeToRConsole(".bio7tempenv$lines <- readLines('" + loc + "')");
										con.pipeToRConsole(".bio7tempenv$lines <- append(.bio7tempenv$lines, 'browser()', after = " + (lineNum - 1) + ")");
										con.pipeToRConsole(".bio7tempenv$lines <- c('.bio7debugfunc <- function() {', .bio7tempenv$lines, '}')");
										con.pipeToRConsole(".bio7tempenv$tmpFile <- tempfile(fileext = '.R')");
										con.pipeToRConsole("writeLines(.bio7tempenv$lines, .bio7tempenv$tmpFile)");
									} else {
										/* Conditional breakpoint */
										con.pipeToRConsole(".bio7tempenv <- new.env()");
										con.pipeToRConsole(".bio7tempenv$lines <- readLines('" + loc + "')");
										con.pipeToRConsole(".bio7tempenv$lines <- append(.bio7tempenv$lines, 'if (" + expression.replace("'", "\\'") + ") browser()', after = " + (lineNum - 1) + ")");
										con.pipeToRConsole(".bio7tempenv$lines <- c('.bio7debugfunc <- function() {', .bio7tempenv$lines, '}')");
										con.pipeToRConsole(".bio7tempenv$tmpFile <- tempfile(fileext = '.R')");
										con.pipeToRConsole("writeLines(.bio7tempenv$lines, .bio7tempenv$tmpFile)");
									}

									/* Highlight the breakpoint line in the editor */
									highlightLine(lineNum);

									try {
										Thread.sleep(200);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}

									/* Clear the console before debugging starts */
									Display.getDefault().asyncExec(new Runnable() {
										public void run() {
											IOConsole consoleForClear = ConsolePageParticipant.getConsolePageParticipantInstance().getIoc();
											consoleForClear.clearConsole();
										}
									});
									/* Mark the debug session as active */
									DebugProgress.startSession();
									/* Restore prompt, source the file, call the debug function */
									con.pipeToRConsole("options(prompt=\"Browse> \")");
									con.pipeToRConsole("source(.bio7tempenv$tmpFile)");
									con.pipeToRConsole("environment(.bio7debugfunc) <- .GlobalEnv");
									con.pipeToRConsole(".bio7debugfunc()");
								}
							}
							return Status.OK_STATUS;
						}
					};
					debugJob.setUser(false);
					debugJob.setSystem(true);
					debugJob.schedule();
				}

			} else {
				Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
			}

		}

	}

	private void highlightLine(final int lineNum) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				ITextEditor edit = (ITextEditor) editor;
				IDocumentProvider dp = edit.getDocumentProvider();
				IDocument doc = dp.getDocument(editor.getEditorInput());

				IRegion reg = null;
				try {
					reg = doc.getLineInformation(lineNum - 1);
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}

				if (reg != null) {
					edit.selectAndReveal(reg.getOffset() + reg.getLength(), 0);
					IResource res = (IResource) editor.getEditorInput().getAdapter(IResource.class);
					try {
						res.deleteMarkers("com.eco.bio7.reditor.debugrulermark", false, IResource.DEPTH_ZERO);
					} catch (CoreException e1) {
						e1.printStackTrace();
					}

					IMarker marker;
					try {
						marker = res.createMarker("com.eco.bio7.reditor.debugrulermark");
						marker.setAttribute(IMarker.CHAR_START, reg.getOffset());
						marker.setAttribute(IMarker.CHAR_END, reg.getOffset() + reg.getLength());
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	public Map<Integer, String> findMyMarkers(IResource target) {
		String type = "com.eco.bio7.redit.debugMarker";

		try {
			markers = target.findMarkers(type, false, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		Map<Integer, String> map1 = new HashMap<Integer, String>();

		for (int i = 0; i < markers.length; ++i) {
			try {
				map1.put((Integer) markers[i].getAttribute(IMarker.LINE_NUMBER), (String) markers[i].getAttribute(IMarker.MESSAGE));
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

	public void selectionChanged(IAction action, ISelection selection) {
	}

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		part = targetEditor;
	}

}