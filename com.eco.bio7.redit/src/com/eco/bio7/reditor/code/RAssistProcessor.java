package com.eco.bio7.reditor.code;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.MarkerAnnotation;

public class RAssistProcessor implements IQuickAssistProcessor {

//	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canFix(Annotation annotation) {
		String text = "";
		if (annotation instanceof MarkerAnnotation) {
			IMarker marker = ((MarkerAnnotation) annotation).getMarker();
			try {
				if(marker.exists()){
				if( marker.getAttribute(IMarker.MESSAGE)!=null){
				  text=(String) marker.getAttribute(IMarker.MESSAGE);
				System.out.println( marker.getAttribute(IMarker.MESSAGE));
				}
				}
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}
		
		
		//String text=annotation.getText();
		
		if(text!=null&&text.equals("One Opening Parentheses to much!")){
			return true;
		}
		else {
			return false; 
		}
		//System.out.println(annotation.getText());
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
		
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext invocationContext) {
		ISourceViewer viewer= invocationContext.getSourceViewer();
        int offset= invocationContext.getOffset();
        String text = getCompleteText(viewer.getDocument(), offset);

        return new ICompletionProposal[]{

            new QuickFixCompletionProposal("Remove Parenthesis",offset,text.length(),""),

           // new QuickFixCompletionProposal("This is the second proposal!",offset,text.length(),"")

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