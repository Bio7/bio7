package com.eco.bio7.rbridge.completion;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.swt.widgets.Control; 

public class ShellCompletion {
	private ContentProposalProvider contentProposalProvider;
	private ContentProposalAdapter contentProposalAdapter;
	private KeyStroke stroke;
	private static String[] statistics;
	private static String[] statisticsContext;
	private static String[] statisticsSet;

	public ShellCompletion(final Control control, final IControlContentAdapter controlContentAdapter) {
		contentProposalProvider = new ContentProposalProvider();
		contentProposalProvider.setFiltering(true);
		try {
			stroke = KeyStroke.getInstance("Ctrl+Space");
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		contentProposalAdapter = new ContentProposalAdapter(control, controlContentAdapter, contentProposalProvider, stroke, null);	
		contentProposalAdapter.setPropagateKeys( true );
       //contentProposalAdapter.setLabelProvider( new FileTreeLabelProvider() );
        contentProposalAdapter.setProposalAcceptanceStyle( ContentProposalAdapter.PROPOSAL_REPLACE );
        
	}

	public void setProposals(final String[] proposals) {
		contentProposalProvider.setProposals(proposals);
	}

	public ContentProposalProvider getContentProposalProvider() {
		return contentProposalProvider;
	}

	public ContentProposalAdapter getContentProposalAdapter() {
		return contentProposalAdapter;
	}
	
	 
	
}
