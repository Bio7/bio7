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
import com.eco.bio7.rbridge.backend.RBackend;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RTools;
import com.eco.bio7.rbridge.RBridge;

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

		ImageDescriptor desc = Bio7Plugin.getImageDescriptor("/icons/rdebug/rundebug.svg");
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


		editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor.isDirty()) {
			editor.doSave(new NullProgressMonitor());
		}

		final IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);

		RBackend d = RBridge.getBackend();
		IEditorInput editorInput = editor.getEditorInput();
		IFile aFile = null;

		if (editorInput instanceof IFileEditorInput) {
			aFile = ((IFileEditorInput) editorInput).getFile();
		}
		final String loc = aFile.getLocation().toString();

		/* The debugger works in the console of embedded R */
		if (d == null || d.isEmbedded()) {

			final String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
			final boolean embedded = d != null && d.isEmbedded();

			if (embedded || selectionConsole.equals("R")) {

				if (resource != null) {
					final Map<Integer, RBreakpoint> map = new TreeMap<Integer, RBreakpoint>(findBreakpoints(resource));
					/* The breakpoints of other R files (loaded by the script with source()) */
					final Map<IResource, Map<Integer, RBreakpoint>> others = otherBreakpoints(resource);
					if (map.isEmpty() && others.isEmpty()) {
						/* No breakpoint: the debugger stops at the first statement of the script (and at errors) */
						map.put(1, null);
					}
					final String fileName = editor.getEditorInput().getName();

					Job debugJob = new Job("R Debug Setup") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							/*
							 * The debugged copy: all breakpoints in one file (browser(), conditions, hit
							 * counts, log points), the script wrapped into .bio7debugfunc
							 */
							java.nio.file.Path copy;
							/* The copies of the other files: source() of the script loads them (embedded R) */
							Map<java.nio.file.Path, java.nio.file.Path> copies = new java.util.LinkedHashMap<>();
							try {
								copy = DebugSession.writeDebugCopy(java.nio.file.Path.of(loc), map);
								for (Map.Entry<IResource, Map<Integer, RBreakpoint>> e : others.entrySet()) {
									java.nio.file.Path source = e.getKey().getLocation().toFile().toPath();
									copies.put(source, DebugSession.writeSourcedCopy(source, e.getValue()));
								}
							} catch (java.io.IOException e) {
								Bio7Dialog.message("The script could not be prepared for debugging:\n" + e.getMessage());
								return Status.OK_STATUS;
							}
							String rCopy = copy.toString().replace("\\", "/");
							if (DebugSession.firstBreakpoint() > 0) {
								highlightLine(DebugSession.firstBreakpoint());
							}
							Display.getDefault().syncExec(() -> DebugVariablesView.clear());

							if (embedded) {
								/* A former session still waits in browser(): left first */
								if (com.eco.bio7.rbridge.backend.EmbeddedR.isBrowsing()) {
									com.eco.bio7.rbridge.backend.EmbeddedR.consoleCapture("Q", 10000, false);
								}
								try {
									RBackend b = RBridge.getBackend();
									b.voidEval(DebugSession.HELPERS);
									b.voidEval(DebugSession.copiesR(copies));
									/*
									 * source() of the session loads the copies (also nested), functions and
									 * classes loaded before from files with breakpoints are defined again
									 */
									b.voidEval(".bio7dbg$install(); .bio7dbg$preload()");
									b.voidEval("source(" + DebugSession.rString(rCopy) + ", keep.source = TRUE); environment(.bio7debugfunc) <- .GlobalEnv");
								} catch (Exception e) {
									Bio7Dialog.message("The script could not be prepared for debugging:\n" + e.getMessage());
									return Status.OK_STATUS;
								}
								DebugProgress.startEmbedded(fileName);
								return Status.OK_STATUS;
							}

							ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();
							/* Clear the console before debugging starts */
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									IOConsole consoleForClear = ConsolePageParticipant.getConsolePageParticipantInstance().getIoc();
									consoleForClear.clearConsole();
								}
							});
							DebugProgress.startSession();
							/* Source the copy, call the debug function */
							con.pipeToRConsole("options(prompt=\"Browse> \")");
							con.pipeToRConsole("source('" + rCopy + "', keep.source = TRUE)");
							con.pipeToRConsole("environment(.bio7debugfunc) <- .GlobalEnv");
							con.pipeToRConsole(".bio7debugfunc()");
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

	/* The breakpoints of the script: line -> condition, hit count, log point */
	public Map<Integer, RBreakpoint> findBreakpoints(IResource target) {
		Map<Integer, RBreakpoint> map = new HashMap<Integer, RBreakpoint>();
		try {
			for (IMarker m : target.findMarkers(RBreakpoint.MARKER, false, IResource.DEPTH_ZERO)) {
				int line = m.getAttribute(IMarker.LINE_NUMBER, -1);
				if (line > 0) {
					map.put(line, RBreakpoint.of(m));
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return map;
	}

	/*
	 * The breakpoints of the other R files of the workspace (not the script): file
	 * -> line -> breakpoint
	 */
	private Map<IResource, Map<Integer, RBreakpoint>> otherBreakpoints(IResource script) {
		Map<IResource, Map<Integer, RBreakpoint>> out = new LinkedHashMap<>();
		try {
			for (IMarker m : org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().findMarkers(RBreakpoint.MARKER, false, IResource.DEPTH_INFINITE)) {
				IResource r = m.getResource();
				int line = m.getAttribute(IMarker.LINE_NUMBER, -1);
				if (r.equals(script) || line <= 0 || r.getLocation() == null || r.getType() != IResource.FILE || !"r".equalsIgnoreCase(r.getFileExtension())) {
					continue;
				}
				out.computeIfAbsent(r, k -> new TreeMap<>()).put(line, RBreakpoint.of(m));
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return out;
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