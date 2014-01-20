package com.eco.bio7.reditor.code;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

public class QuickFixCompletionProposal implements ICompletionProposal {

 

    String text;

    int offset,contextTextLength;

    QuickFixCompletionProposal(String text,int offset,int contextTextLength){

        this.text = text;

        this.offset = offset;

        this.contextTextLength = contextTextLength;

    }

    @Override

    public void apply(IDocument document) {

        try {

            document.replace(offset, 0, text);

            

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

        // TODO Auto-generated method stub

        return null;

    }

 

    @Override

    public String getDisplayString() {

        return text;

    }

 

    @Override

    public Image getImage() {

        return null;

    }

 

    @Override

    public IContextInformation getContextInformation() {

        return null;

    }

 

}