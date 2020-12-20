package com.eco.bio7.libreoffice;

import com.sun.star.frame.TerminationVetoException;

import com.sun.star.frame.XTerminateListener;

public class LoTerminateListener implements XTerminateListener {

	public void notifyTermination(com.sun.star.lang.EventObject eventObject) {

		LibreOffice.setXRemoteContext(null);
		LibreOffice.setXRemoteServiceManager(null);
		

	}

	public void queryTermination(com.sun.star.lang.EventObject eventObject)

	throws TerminationVetoException {

	}

	public void disposing(com.sun.star.lang.EventObject eventObject) {
		

	}

}