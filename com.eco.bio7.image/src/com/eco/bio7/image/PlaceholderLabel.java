package com.eco.bio7.image;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

/*A custom class for a correct custom Layout!*/

public class PlaceholderLabel {
	
	public IContributionItem getPlaceholderLabel(){
		IContributionItem item = new ControlContribution("PlaceholderLabel") {
			protected Control createControl(Composite parent) {
				Composite com = new Composite(parent, SWT.NONE);
				
				//com.setLayout(layout);
				com.setSize(4000, 64);
				Label label = new Label(com, SWT.NONE);
				label.setSize(4000, 64);
				return com;
			}
		};
		return item;
		
	}

}
