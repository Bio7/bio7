package com.eco.bio7.reditor.code;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;

public class RQuickFixCompletionProposal implements ICompletionProposal {

	String text;

	private int offset, contextTextLength;

	private String replace;

	private int lengthToReplace = 1;

	private String contextText;

	RQuickFixCompletionProposal(String text, String contextText, int offset, int contextTextLength, String replace, int lengthToReplace) {

		this.text = text;
		this.contextText = contextText;
		this.lengthToReplace = lengthToReplace;
		this.offset = offset;

		this.contextTextLength = contextTextLength;
		this.replace = replace;

	}

	@Override

	public void apply(IDocument document) {

		try {

			document.replace(offset, lengthToReplace, replace);

		} catch (BadLocationException e) {

			e.printStackTrace();

		}

	}

	@Override

	public Point getSelection(IDocument document) {

		// TODO Auto-generated method stub

		return null;

	}

	@Override

	public String getAdditionalProposalInfo() {

		// if null no context is generated!

		return contextText;

	}

	@Override

	public String getDisplayString() {

		return text;

	}

	@Override

	public Image getImage() {

		Image image = new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/smartmode_co.png"));

		return image;

	}

	@Override

	public IContextInformation getContextInformation() {

		return null;

	}

}