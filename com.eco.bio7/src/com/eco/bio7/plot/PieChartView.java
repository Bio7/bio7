package com.eco.bio7.plot;

import java.awt.Color;
import java.awt.EventQueue;
import javax.swing.JApplet;
import javax.swing.JRootPane;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.plot.PiePlot;
import com.eco.bio7.image.Util;

public class PieChartView extends ViewPart {

	public static final String ID = "com.eco.bio7.piechart";
	private java.awt.Frame frame;
	private Shell parentShell;
	private Composite top;
	private IPartListener2 partListener;
	
	private static Pie piechart;

	public void createPartControl(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.charts");
		partListener=new IPartListener2() {

			public void partActivated(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals("com.eco.bio7.piechart")) {
					if (Util.getOS().equals("Mac")) {

						Display dis = Util.getDisplay();
						dis.asyncExec(new Runnable() {

							public void run() {

								top.setVisible(false);
								top.setVisible(true);
							}
						});

					}

				}

			}

			public void partBroughtToTop(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals("com.eco.bio7.piechart")) {
					if (Util.getOS().equals("Mac")) {
						Display dis = Util.getDisplay();
						dis.asyncExec(new Runnable() {

							public void run() {

								top.setVisible(false);
								top.setVisible(true);
							}
						});

					}

				}

			}

			public void partClosed(IWorkbenchPartReference partRef) {

			}

			public void partDeactivated(IWorkbenchPartReference partRef) {

			}

			public void partHidden(IWorkbenchPartReference partRef) {

			}

			public void partInputChanged(IWorkbenchPartReference partRef) {

			}

			public void partOpened(IWorkbenchPartReference partRef) {

			}

			public void partVisible(IWorkbenchPartReference partRef) {

			}
		};
		getViewSite().getPage().addPartListener(partListener);
		/*Workaround for Mac error!*/
		parentShell = new Shell(Util.getDisplay());
		top = new Composite(parentShell, SWT.NO_BACKGROUND | SWT.EMBEDDED);
		try {
			System.setProperty("sun.awt.noerasebackground", "true");
		} catch (NoSuchMethodError error) {
		}

		frame = SWT_AWT.new_Frame(top);
		
		JApplet panel = new JApplet() {
			public void update(java.awt.Graphics g) {
				//Do not erase the background
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

		//ChartPanel chartPanel = new ChartPanel(piechart.getChart());

		PiePlot piePlot = (PiePlot) piechart.getChart().getPlot();
		piePlot.setBackgroundPaint(getSystemColour(parent));
		piechart.getChart().setBackgroundPaint(getSystemColour(parent));
		top.setParent(parent);
		/*HighDPI fix for Windows frame layout which reverts the DPIUtil settings of the default implementation of the SWT_AWT class!*/
		if (Util.getOS().equals("Windows")) {
			if (Util.getZoom() >= 175) {
				Composite parent2 = top.getParent();
				parent2.getDisplay().asyncExec(() -> {
					if (parent2.isDisposed())
						return;
					final Rectangle clientArea = parent2.getClientArea(); // To Pixels
					EventQueue.invokeLater(() -> {
						frame.setSize(clientArea.width, clientArea.height);
						frame.validate();
					});
				});
			}

		}
		//SwingFxSwtView view = new SwingFxSwtView();
		//view.embedd(parent, chartPanel);

	}

	public java.awt.Color getSystemColour(Composite parent) {
		Color col = null;
		org.eclipse.swt.graphics.Color colswt = parent.getBackground();
		int r = colswt.getRed();
		int g = colswt.getGreen();
		int b = colswt.getBlue();
		col = new Color(r, g, b);

		return col;
	}
	
	public void dispose() {
		if (partListener != null) {
			IWorkbenchPage page = getViewSite().getPage();
			page.removePartListener(partListener);
		}

		super.dispose();
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