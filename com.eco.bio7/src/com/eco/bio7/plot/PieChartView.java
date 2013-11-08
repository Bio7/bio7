package com.eco.bio7.plot;

import javax.swing.JApplet;
import javax.swing.JRootPane;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.jfree.chart.ChartPanel;

public class PieChartView extends ViewPart {

	public static final String ID = "com.eco.bio7.piechart";
	private java.awt.Frame frame;
	private static Pie piechart;

	public void createPartControl(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.charts");
		Composite top = new Composite(parent, SWT.NO_BACKGROUND | SWT.EMBEDDED);
		try {
			System.setProperty("sun.awt.noerasebackground", "true");
		} catch (NoSuchMethodError error) {
		}

		frame = SWT_AWT.new_Frame(top);
		JApplet panel = new JApplet() {
		      public void update(java.awt.Graphics g) {
		        /* Do not erase the background */
		        paint(g);
		      }
		    };

		frame.add(panel);
		JRootPane root = new JRootPane();
		panel.add(root);
		java.awt.Container contentPane = root.getContentPane();

		piechart = new Pie();

		ChartPanel chartPanel = new ChartPanel(piechart.getChart());
		contentPane.add(chartPanel);

	}

	public void setFocus() {

	}

	public static Pie getPiechart() {
		return piechart;
	}

	public static void setPiechart(Pie piechart) {
		PieChartView.piechart = piechart;
	}

}