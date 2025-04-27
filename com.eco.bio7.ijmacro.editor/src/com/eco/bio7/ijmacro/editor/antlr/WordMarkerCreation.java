package com.eco.bio7.ijmacro.editor.antlr;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorPart;

public class WordMarkerCreation extends WorkspaceJob {

	private IEditorPart editor;
	private IDocument doc;
	private int offset;
	private static ILock lock = Job.getJobManager().newLock();

	public WordMarkerCreation(int offset, IEditorPart editor, IDocument doc) {
		super("marker");

		this.editor = editor;
		this.doc = doc;
		this.offset = offset;

	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Create Markers", IProgressMonitor.UNKNOWN);

		create();

		return Status.OK_STATUS;
	}

	private void deleteMarkers(IResource resource) {
		try {
			resource.deleteMarkers("com.eco.bio7.ijmacroeditor.wordmarker", false, IResource.DEPTH_ZERO);
		} catch (CoreException e1) {

			e1.printStackTrace();
		}
	}

	public void create() {
		int length = 0;
		int minusLength = 0;

		while (true) {
			char c = 0;
			if (offset + length >= 0 && offset + length < doc.getLength()) {

				try {
					c = doc.getChar(offset + length);
				} catch (BadLocationException e) {

					e.printStackTrace();
				}

				if (Character.isLetter(c) == false && Character.isDigit(c) == false && (c == '_') == false)
					break;

				length++;
				if (offset + length >= doc.getLength()) {
					break;
				}
			} else {
				break;
			}
		}
		while (true) {
			char c = 0;
			if (offset + length >= 0 && offset + length < doc.getLength()) {

				try {
					c = doc.getChar(offset + minusLength);
				} catch (BadLocationException e) {

					e.printStackTrace();
				}

				if (Character.isLetter(c) == false && Character.isDigit(c) == false && (c == '_') == false)
					break;

				minusLength--;
				if (offset + minusLength < 0) {
					break;
				}
			} else {
				break;
			}
		}
		final int wordOffset = offset + minusLength + 1;
		final int resultedLength = length - minusLength - 1;
		IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
		if (resultedLength > 0) {
			String searchForWord = null;
			ITextOperationTarget target = (ITextOperationTarget) editor.getAdapter(ITextOperationTarget.class);
			if (target instanceof ITextViewer) {
				ITextViewer textViewer = (ITextViewer) target;
				try {
					searchForWord = textViewer.getDocument().get(wordOffset, resultedLength);
                   
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (searchForWord != null) {
				/*
				 * Create a lock for the marker job. Else some marker exceptions
				 * occur, see:
				 * https://eclipse.org/articles/Article-Concurrency/jobs-api.
				 * html
				 */
				try {
					lock.acquire();
					deleteMarkers(resource);
					/*
					 * Match only whole word and not only parts. The search word
					 * embedded in '\\b' will find the whole words!
					 */
					Pattern findWordPattern = Pattern.compile("\\b" + searchForWord + "\\b");
					Matcher matcher = findWordPattern.matcher(doc.get());
					IMarker marker;
					while (matcher.find()) {
						int offsetStart = matcher.start();
						int offsetEnd = matcher.end();
						// do something with offsetStart and offsetEnd

						try {

							marker = resource.createMarker("com.eco.bio7.ijmacroeditor.wordmarker");

							marker.setAttribute(IMarker.CHAR_START, offsetStart);
							marker.setAttribute(IMarker.CHAR_END, offsetEnd);
						} catch (CoreException e) {
							System.out.println("Error:" + offsetStart + "" + offsetEnd);
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} finally {
					lock.release();
				}

			}

		} else {
			try {
				lock.acquire();
				deleteMarkers(resource);
			} finally {
				lock.release();
			}
		}

	}

}
