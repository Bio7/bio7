package com.eco.bio7.rcp;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Scale;

public class ScaleItem extends ContributionItem {

	public ScaleItem(String id) {
		super(id);
	}

	public void fill(Composite parent) {

		Scale scale = new Scale(parent, SWT.HORIZONTAL);

		scale.setMaximum(1000);
		scale.setMinimum(20);
		scale.setIncrement(1);
		scale.setPageIncrement(5);
		scale.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int value = scale.getMaximum() - scale.getSelection() + scale.getMinimum();
				//System.out.println("" + value);
			}
		});
	}
}