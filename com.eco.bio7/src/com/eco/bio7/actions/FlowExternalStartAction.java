package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.BatchModel;

public class FlowExternalStartAction extends Action {

	private final IWorkbenchWindow window;

	public FlowExternalStartAction(String text, IWorkbenchWindow window) {
		
		super(text);
		this.window = window;
		setId("com.eco.bio7.flow_start");
		setImageDescriptor(Bio7Plugin.getImageDescriptor("/icons/bio7new.png"));
	
	}

	public void run() {

		
		if(BatchModel.isPause()==false){
			BatchModel.batch(false);
			}
			else{
				BatchModel.setPause(false);
				BatchModel.batchStart();
			}

	}

}