package com.eco.bio7.reditor.code;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;

public class RAssistProcessor implements IQuickAssistProcessor {

	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canFix(Annotation annotation) {
		
		//System.out.println(annotation.getText());
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext invocationContext) {
		ISourceViewer viewer= invocationContext.getSourceViewer();
        int offset= invocationContext.getOffset();
        String text = getCompleteText(viewer.getDocument(), offset);

        return new ICompletionProposal[]{

            new QuickFixCompletionProposal("This is the first proposal!",offset,text.length()),

            new QuickFixCompletionProposal("This is the second proposal!",offset,text.length())

        };
	}
    private String getCompleteText(IDocument document, int currentOffset){

    	 

        int count = currentOffset;

        String tobeCompleteText="";

        try{

            while(--count>=0){

                char c = ' ';

                c = document.getChar(count);

 

                if(isWhiteSpace(c)){

                    break;

                }

            }

            tobeCompleteText = document.get(count+1, currentOffset-(count+1));

 

        }catch (Exception e) {

            e.printStackTrace();

        }

        return tobeCompleteText;

    }
    private boolean isWhiteSpace(char c){

        if(c=='\n'||c=='\t'||c==' ')

            return true;

        return false;

    }

	

}