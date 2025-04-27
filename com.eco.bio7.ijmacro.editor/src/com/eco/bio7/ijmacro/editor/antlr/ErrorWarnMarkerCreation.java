package com.eco.bio7.ijmacro.editor.antlr;

import java.util.ArrayList;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

import com.eco.bio7.ijmacro.editors.IJMacroEditor;

public class ErrorWarnMarkerCreation extends WorkspaceJob {

	public static IResource resource;
	private IJMacroEditor editor;
	private boolean warn = false;
	private Token offSymbol;
	//private int offSymbolTokenLength = 0;
	String quickFix = null;
	private ArrayList<ErrorWarnStore> errors;
	private String replacementText = null;
	private static ILock lock = Job.getJobManager().newLock();

	public ErrorWarnMarkerCreation(String name, IJMacroEditor editor, ArrayList<ErrorWarnStore> errors) {
		super(name);
		this.errors = errors;
		this.editor = editor;

	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Create Markers", IProgressMonitor.UNKNOWN);
		if (editor != null) {

			IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
			if (resource != null) {
				try {
					resource.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
				} catch (CoreException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				/* Delete all problem markers! */
				/*
				 * IMarker[] markers = findMyMarkers(resource); int lineNumb =
				 * -1; for (int i = 0; i < markers.length; i++) {
				 * 
				 * try { lineNumb = (int)
				 * markers[i].getAttribute(IMarker.LINE_NUMBER);
				 * 
				 * if (lineNumb == line) { markers[i].delete(); //
				 * System.out.println(recognizer.getRuleNames()[i]); } } catch
				 * (CoreException e1) { // TODO Auto-generated catch block
				 * e1.printStackTrace(); } }
				 */
			}
		}

		for (int i = 0; i < errors.size(); i++) {
			ErrorWarnStore errWarn = errors.get(i);
			createMarkers(errWarn.getOffendingSymbol(), errWarn.getLine(), errWarn.getCharPositionInLine(), errWarn.getMsg());
		}

		return Status.OK_STATUS;
	}

	/*
	 * At the moment we split the ANTLR message (String) into up to three components. The error or warn number, the description and probably the replacement (solution)! Warn and error 17,19 are
	 * implemented as empty QuickFixes for a later use maybe. Null text comes from the ANTLR error listener which is not custom produced as in the RBaseListen and RRefPhaseListen class! See class
	 * RAssistProcessor for the final icon creation if QuickFix is available or not!
	 */
	public void createMarkers(Object offendingSymbol, int line, int charPositionInLine, String msg) {
		if (editor.getPage().getWorkbenchWindow().getShell().isDisposed() == false) {
			msg = msg.replace("\\r\\n", "\n");
			msg = msg.replace("\\n", "\n");
			msg = msg.replace("\\t", "\t");

			if (offendingSymbol != null) {
				offSymbol = (Token) offendingSymbol;
				//offSymbolTokenLength = offSymbol.getText().length();
			}

			if (msg.startsWith("Err")) {
				warn = false;
				String[] split = msg.split("####");
				quickFix = split[0];
				msg = split[1];
				/* If we have a replacement text! */
				if (split.length > 2) {
					replacementText = split[2];
				} else {
					replacementText = "";
				}

				// System.out.println(msg);
			} else if (msg.startsWith("Warn")) {
				warn = true;
				String[] split = msg.split("####");
				quickFix = split[0];
				msg = split[1];
				/* If we have a replacement text! */
				if (split.length > 2) {
					replacementText = split[2];
				} else {
					replacementText = "";
				}
			}

			if (editor != null) {

				IEditorInput editorInput = editor.getEditorInput();
				resource = (IResource) editorInput.getAdapter(IResource.class);
				IDocumentProvider provider = editor.getDocumentProvider();

				if (editorInput != null && editor.getDocumentProvider() != null) {
					IDocument document = provider.getDocument(editorInput);
					int lineOffsetStart = 0;

					//System.out.println("lineoffset is at:" + (line-1));
					//int len = document.getLength();
					int len = document.getNumberOfLines();
					if ((line - 1) >= 0 && (line-1) < len) {
						try {
							lineOffsetStart = document.getLineOffset(line - 1);
						} catch (BadLocationException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
							System.out.println("\nLine Number: " + line);
						}
					}

					try {
						lock.acquire();
						IMarker marker;
						if (warn) {

							try {
								marker = resource.createMarker(IMarker.PROBLEM);
								marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
								// marker.setAttribute(IMarker.PRIORITY,
								// IMarker.PRIORITY_NORMAL);
								// marker.setAttribute(IMarker.MESSAGE, "line "
								// +
								// line +
								// ":"
								// +
								// charPositionInLine + " " + msg);
								marker.setAttribute(IMarker.MESSAGE, msg);
								marker.setAttribute(IMarker.LINE_NUMBER, line);
								marker.setAttribute(IMarker.LOCATION, lineOffsetStart + charPositionInLine);
								if (quickFix != null) {
									marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
									marker.setAttribute(IMarker.TEXT, quickFix);
									marker.setAttribute("TOKEN_TEXT", offSymbol.getText());
									marker.setAttribute("REPLACEMENT_TEXT", replacementText);
								}
								/*
								 * This error message comes from the default ANTLR error parser!
								 */
								else {
									marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_LOW);
									marker.setAttribute(IMarker.TEXT, "NA");
								}
								createUnderlineMarker(marker);
							} catch (CoreException ex) {

								ex.printStackTrace();
							}
							warn = false;// reset warning flag!
						} else {
							try {
								// System.out.println(offSymbol.getText());
								marker = resource.createMarker(IMarker.PROBLEM);
								marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
								// marker.setAttribute(IMarker.PRIORITY,
								// IMarker.PRIORITY_NORMAL);
								// marker.setAttribute(IMarker.MESSAGE, "line "
								// +
								// line +
								// ":"
								// +
								// charPositionInLine + " " + msg);
								marker.setAttribute(IMarker.MESSAGE, msg);
								marker.setAttribute(IMarker.LINE_NUMBER, line);
								marker.setAttribute(IMarker.LOCATION, lineOffsetStart + charPositionInLine);
								if (quickFix != null) {
									marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
									marker.setAttribute(IMarker.TEXT, quickFix);
									marker.setAttribute("TOKEN_TEXT", offSymbol.getText());
									marker.setAttribute("REPLACEMENT_TEXT", replacementText);
								}
								/*
								 * This error message comes from the default ANTLR error parser!
								 */
								else {
									marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_LOW);
									marker.setAttribute(IMarker.TEXT, "NA");
								}
								createUnderlineMarker(marker);
							} catch (CoreException ex) {

								ex.printStackTrace();
							}
						}
					} finally {
						lock.release();
					}
				}
			}
		}

	}

	private void createUnderlineMarker(IMarker marker) throws CoreException {
		/* Correct the underline error if start and stop index is equal! */
		if (offSymbol.getStartIndex() == offSymbol.getStopIndex()) {
			marker.setAttribute(IMarker.CHAR_START, offSymbol.getStartIndex());
			marker.setAttribute(IMarker.CHAR_END, offSymbol.getStopIndex() + 1);

		} else {
			/* Correct the underline error if it is a linebreak! */
			if (offSymbol.getText().equals(System.lineSeparator())) {

				marker.setAttribute(IMarker.CHAR_START, offSymbol.getStartIndex() - 1);
				marker.setAttribute(IMarker.CHAR_END, offSymbol.getStopIndex());
			} else {
				// System.out.println(offSymbol.getStartIndex()+"
				// "+offSymbol.getStopIndex());
				marker.setAttribute(IMarker.CHAR_START, offSymbol.getStartIndex());
				marker.setAttribute(IMarker.CHAR_END, offSymbol.getStopIndex() + 1);
			}
		}
	}

	protected void underlineError(Recognizer recognizer, Token offendingToken, int line, int charPositionInLine) {
		CommonTokenStream tokens = (CommonTokenStream) recognizer.getInputStream();
		String input = tokens.getTokenSource().getInputStream().toString();
		String[] lines = input.split("\n");
		if (line > 0 && line <= lines.length - 1) {
			String errorLine = lines[line - 1];
			System.err.println(errorLine);
			for (int i = 0; i < charPositionInLine; i++)
				System.err.print(" ");
			int start = offendingToken.getStartIndex();
			int stop = offendingToken.getStopIndex();
			if (start >= 0 && stop >= 0) {
				for (int i = start; i <= stop; i++)
					System.err.print("^");
			}
			System.err.println();
		}
	}

}
