package com.eco.bio7.plot;

import javax.swing.JApplet;
import javax.swing.JRootPane;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.eco.bio7.image.SwingFxSwtView;

public class LineChartView extends ViewPart {
	public static final String ID = "com.eco.bio7.linechart";

	public static LineChart linechart;

	public java.awt.Frame frame;

	public LineChartView() {
		super();

	}

	public void createPartControl(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.charts");
		/*Composite top = new Composite(parent, SWT.NO_BACKGROUND | SWT.EMBEDDED);
		try {
			System.setProperty("sun.awt.noerasebackground", "true");
		} catch (NoSuchMethodError error) {
		}

		frame = SWT_AWT.new_Frame(top);
		JApplet panel = new JApplet() {
		      public void update(java.awt.Graphics g) {
		         Do not erase the background 
		        paint(g);
		      }
		    };

		frame.add(panel);
		JRootPane root = new JRootPane();
		panel.add(root);
		java.awt.Container contentPane = root.getContentPane();

		

		contentPane.add(linechart.getChartPanel());*/
		linechart = new LineChart();
		SwingFxSwtView view=new SwingFxSwtView();
		view.embedd(parent,linechart.getChartPanel());

	}

	public void setFocus() {

	}

}
