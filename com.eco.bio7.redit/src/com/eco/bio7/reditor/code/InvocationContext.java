package com.eco.bio7.reditor.code;

import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.source.ISourceViewer;

/**
 * @author M. Austenfeld A helper class for Quick fixes which will be visible
 *         when hovering over a annotation(marker) in the editor.
 */
public class InvocationContext implements IQuickAssistInvocationContext {

	private int offset;
	private int length;
	private ISourceViewer sourceViewer;

	public InvocationContext(int offset, int length, ISourceViewer sourceViewer) {
		this.offset = offset;
		this.length = length;
		this.sourceViewer = sourceViewer;
	}

	@Override
	public int getOffset() {
		
		return offset;
	}

	@Override
	public int getLength() {
		return length;
	}

	@Override
	public ISourceViewer getSourceViewer() {
		return sourceViewer;
	}

}
