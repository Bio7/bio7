package com.eco.bio7.rbridge.completion;
import java.util.ArrayList;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import com.eco.bio7.rpreferences.template.CalculateRProposals;
 
public class ContentProposalProvider  implements IContentProposalProvider {
   private String[]		statistics;
   private String[]		statisticsContext;
   private IContentProposal[]	contentProposals;
   private boolean		filterProposals	= true;
  
 
   public ContentProposalProvider() {
      super();
      /*
		 * At startup load the default R proposals and add them to the
		 * templates!
		 */
		CalculateRProposals.loadRCodePackageTemplates();
      /* Load the created proposals! */
		statistics = CalculateRProposals.getStatistics();
		statisticsContext = CalculateRProposals.getStatisticsContext();
		//statisticsSet = CalculateRProposals.getStatisticsSet();
   }
 
   public IContentProposal[] getProposals(String contents, int position) {
      if (filterProposals) {
         ArrayList<IContentProposal> list = new ArrayList<IContentProposal>();
         for (int i = 0; i < statistics.length; i++) {
            if (statistics[i].length() >= contents.length() 
                && statistics[i].substring(0, contents.length()).equalsIgnoreCase(contents)) {
               list.add(makeContentProposal(statistics[i], statisticsContext[i]));
            }
         }
         return (IContentProposal[]) list.toArray(new IContentProposal[list.size()]);
      }
      if (contentProposals == null) {
         contentProposals = new IContentProposal[statistics.length];
         for (int i = 0; i < statistics.length; i++) {
            contentProposals[i] = makeContentProposal(statistics[i], statisticsContext[i]);
            
            
           
         }
      }
      return contentProposals;
   }
 
   public void setProposals(String[] items) {
      this.statistics = items;
      contentProposals = null;
   }
 
   public void setFiltering(boolean filterProposals) {
      this.filterProposals = filterProposals;
      contentProposals = null;
   }
   
   
 
   private IContentProposal makeContentProposal(final String proposal, final String label) {
      return new IContentProposal() {
 
         public String getContent() {
            return proposal;
         }
 
         public String getDescription() {
          
            return null;  
         }
 
         public String getLabel() {
            return proposal + " - " + label;
         }
 
         public int getCursorPosition() {
            return proposal.length();
         }
      };
   }


}