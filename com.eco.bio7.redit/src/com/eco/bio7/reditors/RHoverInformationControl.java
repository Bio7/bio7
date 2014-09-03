// Author: Dan Breslau (dbreslau a t alumni dot uchicago dot edu)
// This file is released into the public domain. No rights reserved.
// Use at your own risk.

package com.eco.bio7.reditors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.browser.*;



/**
 * Browser-based implementation of {@link org.eclipse.jface.text.IInformationControl}.
 * <p>
 * Displays HTML in a {@link org.eclipse.swt.browser.Browser} widget.
 */
public class RHoverInformationControl extends AbstractInformationControl implements DisposeListener, IInformationControlExtension2    {
    
    /**
     * A wrapper used to deliver content to the hover control, either 
     * as marked-up text or as a URL.
     */
    public interface IHTMLHoverInfo {
        /**
         * @return true if the String returned by getHTMLString() represents a URL; 
         * false if the String contains marked-up text.
         */

        public boolean isURL();

       /**
        * @return The input string to be displayed in the Browser widget
        * (either as marked-up text, or as a URL.)
        */
        public String getHTMLString();
    }

    private Browser fBrowser;
    boolean fIsURL;

    /**
     * Creates a JavaHoverInformationControl with the given shell as parent.
     *
     * @param parent the parent shell
     */
    public RHoverInformationControl(Shell parent) {
        super(parent, true);
        create();
    }


    /*
     * @see org.eclipse.jface.text.AbstractInformationControl#createContent(org.eclipse.swt.widgets.Composite)
     */
    protected void createContent(Composite parent) {

        try {
            fBrowser = new Browser(parent, SWT.NONE);
            fBrowser.setSize(400, 200);
            fBrowser.setLayout(new FillLayout());
            
        } 
        catch (SWTError e) {
            MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
            messageBox.setMessage("Browser cannot be initialized."); //$NON-NLS-1$
            messageBox.setText("Error");                             //$NON-NLS-1$
            messageBox.open();
        }
    }
    
    /*
     * @see IInformationControl#setInformation(String)
     */
    public void setInformation(String content) {
        fBrowser.setBounds(getShell().getClientArea());
        if (fIsURL) {
            fBrowser.setUrl(content);
        }
        else {
            fBrowser.setText(content);
        }
    }


    /*
     * @see IInformationControl#computeSizeHint()
     */
    public Point computeSizeHint() {
        // see: https://bugs.eclipse.org/bugs/show_bug.cgi?id=117602

        // Note: I set the widthHint to 350 in order to get
        // better screen shots. SWT.DEFAULT works too, but
        // produces a wider hover. -- Dan Breslau
        
        
        return getShell().computeSize(400, 200, true);
    }
    

    /*
     * @see IInformationControlExtension#hasContents()
     */
    public boolean hasContents() {
        return fBrowser.getText().length() > 0;
    }


    /*
     * @see org.eclipse.jface.text.IInformationControlExtension5#getInformationPresenterControlCreator()
     * @since 3.4
     */
    public IInformationControlCreator getInformationPresenterControlCreator() {
        return new IInformationControlCreator() {
            /*
             * @see org.eclipse.jface.text.IInformationControlCreator#createInformationControl(org.eclipse.swt.widgets.Shell)
             */
            public IInformationControl createInformationControl(Shell parent) {
                return new RHoverInformationControl(parent);
            }
        };
    }


    /*
     * @see org.eclipse.jface.text#setInput()
     * The input object may be a String, an instance of IHTMLHoverInfo, or any
     * object that returns a displayable String from its toString() implementation.
     * 
     * @since 3.4
     */    
    public void setInput(Object input) {
    	
    	/*
		 *  b.setLocation(url);
		 * 
		 */
        // Assume that the input is marked-up text, not a URL
        fIsURL = false;
        final String inputString;

        if (input instanceof IHTMLHoverInfo) {
            // Get the input string, then see whether it's a URL
            IHTMLHoverInfo inputInfo = (IHTMLHoverInfo) input;
            inputString = inputInfo.getHTMLString();
            fIsURL= inputInfo.isURL();
        }
        else if (input instanceof String) {
            // Treat the String as marked-up text to be displayed.
            inputString = (String) input;
        }
        else {
            // For any other kind of object, just use its string 
            // representation as text to be displayed.
            inputString = input.toString();
        }
        setInformation(inputString);
    }


	@Override
	public void widgetDisposed(DisposeEvent e) {
		// TODO Auto-generated method stub
		
	}

}
