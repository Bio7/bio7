package com.eco.bio7.image;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;

import javax.swing.JApplet;
import javax.swing.JPanel;
import javax.swing.JRootPane;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import ij.IJ;

public class IJtoolbarSwtAwt {

	public Composite top;
	public Frame frame;
	public Shell parent;
	protected static Color col;

	public Composite getTop() {
		return top;
	}

	public Shell getParent() {
		return parent;
	}

	public IJtoolbarSwtAwt(org.eclipse.swt.graphics.Color colswt) {
		/*If we have no ImageJ instance (e.g. inside an Eclipse installation) we open the ImageJ view first!*/
		if (IJ.getInstance() == null) {
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					try {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						page.showView("com.eco.bio7.imagej");
					} catch (PartInitException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			});
		}

		parent = new Shell(Util.getDisplay());

		top = new Composite(parent, SWT.NO_BACKGROUND | SWT.EMBEDDED);

		frame = SWT_AWT.new_Frame(top);
		try {
			System.setProperty("sun.awt.noerasebackground", "true");
		} catch (NoSuchMethodError error) {
		}
		/*HighDPI fix for Windows frame layout which reverts the DPIUtil settings of the default implementation of the SWT_AWT class!*/
		if (Util.getOS().equals("Windows")) {
			if (Util.getZoom() >= 175) {
				Composite parent = top.getParent();
				parent.getDisplay().asyncExec(() -> {
					if (parent.isDisposed())
						return;
					final Rectangle clientArea = parent.getClientArea(); // To Pixels
					EventQueue.invokeLater(() -> {
						frame.setSize(clientArea.width, clientArea.height);
						frame.validate();
					});
				});
			}

		}

		/*
		 * final sun.awt.EmbeddedFrame ef = (sun.awt.EmbeddedFrame) frame;
		 * ef.addWindowListener(new WindowAdapter() { public void
		 * windowActivated(WindowEvent e) { ef.synthesizeWindowActivation(true); } });
		 */
		// SwtAwt.setSwtAwtFocus(frame, parent,Util.getDisplay());
		Panel panel = new JApplet() {
			public void update(java.awt.Graphics g) {

				paint(g);
			}
		};

		frame.add(panel);
		JRootPane roote = new JRootPane();
		panel.add(roote);
		java.awt.Container contentPane = roote.getContentPane();
		JPanel jpp = new JPanel();

		jpp.setLayout(new GridLayout(2, 1));

		int r = colswt.getRed();
		int g = colswt.getGreen();
		int b = colswt.getBlue();
		Color swtBackgroundToAWT = new Color(r, g, b);

		panel.setBackground(swtBackgroundToAWT);
		roote.setBackground(swtBackgroundToAWT);
		frame.setBackground(swtBackgroundToAWT);
		jpp.setBackground(swtBackgroundToAWT);
		jpp.add(IJ.getInstance().toolbar);
		jpp.add(IJ.getInstance().statusBar);

		contentPane.add(jpp);

		/*else {
		Display display = Util.getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				MessageBox messageBox = new MessageBox(Util.getShell(),
		
						SWT.ICON_WARNING);
				messageBox.setText("Info!");
				messageBox.setMessage("Please reopen the ImageJ-Toolbar view!\n\n" + "Only a detached toolbar will be reopened automatically\n" + "in a saved and restored Eclipse session.");
				messageBox.open();
			}
		});
		
		Close the stored toolbar layout (not valid in RCP) if the view is embedded in a perspective for the plugin version (the embedded view will be opened before the ImageJ instance is available!)
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				IWorkbenchPage wbp = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		
				wbp.hideView(wbp.findView("com.eco.bio7.ijtoolbar"));
		
			}
		
		});
		}*/
	}

}
