package com.eco.bio7.actions;


import org.eclipse.jface.action.ControlContribution;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

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
        Image image=new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/placeholder.png"));
       
        b.setImage(image);
       // b.setText("Label: <Your User Name>");  
        return b;  
    }  

}  