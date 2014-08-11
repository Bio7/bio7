package com.eco.bio7.rpreferences.template;

import java.util.ArrayList;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

public class CompletionProcessor implements
                                  IContentAssistProcessor {
   private final IContextInformation[] NO_CONTEXTS =
      new IContextInformation[0];
   private final char[] PROPOSAL_ACTIVATION_CHARS =
      new char[] { 's','f','p','n','m', };
   private ICompletionProposal[] NO_COMPLETIONS =
      new ICompletionProposal[0];

   public ICompletionProposal[] computeCompletionProposals(
      ITextViewer viewer, int offset) {
    
         IDocument document = viewer.getDocument();
        // ArrayList result = new ArrayList();
        // String prefix = lastWord(document, offset);
         //String indent = lastIndent(document, offset);
        /* EscriptModel model =
                     EscriptModel.getModel(document, null);
         model.getContentProposals(prefix, indent,
                                          offset, result);*/
         System.out.println("prposallllll");
         
         

         ICompletionProposal[] result = new ICompletionProposal[1];
         int i = 0;
         
             String text = "" + "a" + ">" + "</" + "b" + ">";
             

             result[0] = new CompletionProposal(text, offset, 5, document.getLength());
        
         return result;
         
         
         
   }
   private String lastWord(IDocument doc, int offset) {
      try {
         for (int n = offset-1; n >= 0; n--) {
           char c = doc.getChar(n);
           if (!Character.isJavaIdentifierPart(c))
             return doc.get(n + 1, offset-n-1);
         }
      } catch (BadLocationException e) {
         // ... log the exception ...
      }
      return "";
   }
   
   
   private String lastIndent(IDocument doc, int offset) {
      try {
         int start = offset-1;
         while (start >= 0 &&
            doc.getChar(start)!= '\n')  start--;
         int end = start;
         while (end < offset &&
            Character.isSpaceChar(doc.getChar(end))) end++;
         return doc.get(start+1, end-start-1);
      } catch (BadLocationException e) {
         e.printStackTrace();
      }
      return "";
   }
   
   
   public IContextInformation[] computeContextInformation(
      ITextViewer viewer, int offset) {
      return NO_CONTEXTS;
   }
   
   public char[] getCompletionProposalAutoActivationCharacters() {
      return PROPOSAL_ACTIVATION_CHARS;
   }
   
   // ... remaining methods are optional ...
@Override
public char[] getContextInformationAutoActivationCharacters() {
	// TODO Auto-generated method stub
	return null;
}
@Override
public String getErrorMessage() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public IContextInformationValidator getContextInformationValidator() {
	// TODO Auto-generated method stub
	return null;
}
}