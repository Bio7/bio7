/*
 * package com.eco.bio7.reditor.antlr;
 * 
 * import java.util.ArrayList; import org.eclipse.core.resources.IMarker; import
 * org.eclipse.core.resources.IResource; import
 * org.eclipse.core.resources.WorkspaceJob; import
 * org.eclipse.core.runtime.CoreException; import
 * org.eclipse.core.runtime.IProgressMonitor; import
 * org.eclipse.core.runtime.IStatus; import org.eclipse.core.runtime.Status;
 * 
 * import com.eco.bio7.reditors.REditor;
 * 
 * public class ErrorWarnMarkerDeletion extends WorkspaceJob {
 * 
 * public static IResource resource; private REditor editor;
 * 
 * String quickFix = null;
 * 
 * public ErrorWarnMarkerDeletion(String name, REditor editor) { super(name);
 * 
 * this.editor = editor;
 * 
 * }
 * 
 * @Override public IStatus runInWorkspace(IProgressMonitor monitor) throws
 * CoreException { monitor.beginTask("Delete Markers",
 * IProgressMonitor.UNKNOWN);
 * 
 * if (editor != null) {
 * 
 * IResource resource = (IResource)
 * editor.getEditorInput().getAdapter(IResource.class);
 * 
 * if (resource != null) { try { resource.deleteMarkers(IMarker.PROBLEM, true,
 * IResource.DEPTH_INFINITE); } catch (CoreException e1) { // TODO
 * Auto-generated catch block e1.printStackTrace(); } Delete all problem
 * markers!
 * 
 * IMarker[] markers = findMyMarkers(resource); int lineNumb = -1; for (int i =
 * 0; i < markers.length; i++) {
 * 
 * try { lineNumb = (int) markers[i].getAttribute(IMarker.LINE_NUMBER);
 * 
 * if (lineNumb == line) { markers[i].delete(); //
 * System.out.println(recognizer.getRuleNames()[i]); } } catch (CoreException
 * e1) { // TODO Auto-generated catch block e1.printStackTrace(); } }
 * 
 * } }
 * 
 * return Status.OK_STATUS; }
 * 
 * }
 */