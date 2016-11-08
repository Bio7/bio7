/*package com.eco.bio7.actions;


import org.eclipse.jface.action.ControlContribution;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.eco.bio7.Bio7Plugin;

public class LabelControlContribution extends ControlContribution  
{  

    public LabelControlContribution(String id)  
    {  
        super(id);  
        
        
    }  

    @Override  
    protected Control createControl(Composite parent)  
    {  
    	
        final Label b = new Label(parent, SWT.LEFT); 
        Image image=Bio7Plugin.getImageDescriptor("/icons/workbench/placeholder.png").createImage();
       
        //b.setImage(image);
       // b.setText("Label: <Your User Name>");  
        return b;  
    }  

}  */