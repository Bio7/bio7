package com.eco.bio7.info;

import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import com.eco.bio7.collection.ResizeArray;
import com.eco.bio7.discrete.Field;
import com.eco.bio7.discrete.Hexagon;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.time.Time;
import com.swtdesigner.SWTResourceManager;

public class InfoView extends ViewPart {
	
	public InfoView() {
		
	}

	public static final String ID = "com.eco.bio7.control";

	private static ExpandItem item3;

	private ExpandBar bar;

	private Scale scale;

	private static Scale scale_1;

	private static Scale scale_2;

	private boolean resizable = true;

	private Button btnResizeAfterAdjust;

	private static Label lblFieldsizeY;

	private static Label lblFieldsizeX;
	
	private static String timeSteps = "Time steps: ";

	private static String month = "Month:      ";

	private static String year = "Year:       ";

	private static Label lblTimesteps;

	private static Label lblMonth;

	private static Label lblYear;
	
	public static Label getLblFieldsizeY() {
		return lblFieldsizeY;
	}

	public static Label getLblFieldsizeX() {
		return lblFieldsizeX;
	}
	
	public static Scale getScale_1() {
		return scale_1;
	}

	public static Scale getScale_2() {
		return scale_2;
	}

	public static Label getLblTimesteps() {
		return lblTimesteps;
	}

	public static void setLblTimesteps(Label lblTimesteps) {
		InfoView.lblTimesteps = lblTimesteps;
	}

	public static Label getLblMonth() {
		return lblMonth;
	}

	public static void setLblMonth(Label lblMonth) {
		InfoView.lblMonth = lblMonth;
	}

	public static Label getLblYear() {
		return lblYear;
	}

	public static void setLblYear(Label lblYear) {
		InfoView.lblYear = lblYear;
	}

	public void createPartControl(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.controll");
		bar = new ExpandBar(parent, SWT.V_SCROLL);
		bar.setForeground(org.eclipse.wb.swt.SWTResourceManager.getColor(SWT.COLOR_BLACK));

		bar.setBackground(new Color(parent.getDisplay(), 255, 255, 255));

		item3 = new ExpandItem(bar, SWT.NONE, 0);

		item3.setText("Time and Space");
		item3.setHeight(400);

		item3.setExpanded(true);

		Composite composite = new Composite(bar, SWT.NONE);
		composite.setBackground(org.eclipse.wb.swt.SWTResourceManager.getColor(SWT.COLOR_WHITE));
		//composite.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		item3.setControl(composite);

		 lblTimesteps = new Label(composite, SWT.NONE);
		 lblTimesteps.setBackground(org.eclipse.wb.swt.SWTResourceManager.getColor(SWT.COLOR_WHITE));
		 //lblTimesteps.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		lblTimesteps.setBounds(10, 10, 170, 23);
		lblTimesteps.setText(timeSteps + Time.getTime());

		lblMonth = new Label(composite, SWT.NONE);
		lblMonth.setBackground(org.eclipse.wb.swt.SWTResourceManager.getColor(SWT.COLOR_WHITE));
		//lblMonth.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		lblMonth.setBounds(10, 39, 170, 23);
		lblMonth.setText(month+Time.getMonth());

		lblYear = new Label(composite, SWT.NONE);
		lblYear.setBackground(org.eclipse.wb.swt.SWTResourceManager.getColor(SWT.COLOR_WHITE));
		//lblYear.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		lblYear.setBounds(10, 65, 170, 23);
		lblYear.setText(year+Time.getYear());

		scale = new Scale(composite, SWT.NONE);
		scale.setBackground(org.eclipse.wb.swt.SWTResourceManager.getColor(SWT.COLOR_WHITE));
		//scale.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		scale.setMaximum(1000);
		scale.setMinimum(1);
		scale.setSelection(500);
		scale.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Time.setInterval(scale.getSelection());
			}
		});
		scale.setBounds(10, 100, 170, 42);

		scale_1 = new Scale(composite, SWT.NONE);
		scale_1.setBackground(org.eclipse.wb.swt.SWTResourceManager.getColor(SWT.COLOR_WHITE));
		//scale_1.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		
		scale_1.addSelectionListener(new SelectionAdapter() {
			private int sc1;
			private int sc2;

			@Override
			public void widgetSelected(SelectionEvent e) {
				sc1 = scale_1.getSelection();
				sc2 = scale_2.getSelection();

				if (resizable) {
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							// !!
							public void run() {
								ResizeArray.update(sc1, sc2);

								Quad2d.getQuad2dInstance().repaint();
								// repaint_soilpanel();

								if (Hexagon.getHexagonInstance() != null) {
									Hexagon.getHexagonInstance().repaint();
								}
							}
						});
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (InvocationTargetException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

				}
				lblFieldsizeY.setText("Fieldsize Y -> " + scale_1.getSelection());
			}
		});
		scale_1.setMaximum(2000);
		scale_1.setMinimum(1);
		scale_1.setSelection(50);
		scale_1.setBounds(10, 177, 170, 42);

		scale_2 = new Scale(composite, SWT.NONE);
		scale_2.setBackground(org.eclipse.wb.swt.SWTResourceManager.getColor(SWT.COLOR_WHITE));
		//scale_2.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		scale_2.addSelectionListener(new SelectionAdapter() {
			private int sc1;
			private int sc2;

			@Override
			public void widgetSelected(SelectionEvent e) {
				sc1 = scale_1.getSelection();
				sc2 = scale_2.getSelection();
				if (resizable) {
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							// !!
							public void run() {
								ResizeArray.update(sc1, sc2);

								Quad2d.getQuad2dInstance().repaint();

								if (Hexagon.getHexagonInstance() != null) {
									Hexagon.getHexagonInstance().repaint();
								}
							}
						});
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (InvocationTargetException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

				}
				lblFieldsizeX.setText("Fieldsize X -> " + scale_2.getSelection());
			}
		});
		scale_2.setMaximum(2000);
		scale_2.setMinimum(1);
		scale_2.setSelection(50);
		scale_2.setBounds(10, 254, 170, 42);

		 btnResizeAfterAdjust = new Button(composite, SWT.CHECK);
		
		btnResizeAfterAdjust.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		
		btnResizeAfterAdjust.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				resizable = !resizable;
			}
		});
		btnResizeAfterAdjust.setBounds(20, 331, 170, 23);
		btnResizeAfterAdjust.setText("Resize after adjust");

		Button btnResize = new Button(composite, SWT.NONE);
		btnResize.addSelectionListener(new SelectionAdapter() {
			private int sc1;
			private int sc2;
			@Override
			public void widgetSelected(SelectionEvent e) {
				sc1 = scale_1.getSelection();
				sc2 = scale_2.getSelection();

				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						// !!
						public void run() {
							ResizeArray.update(sc1, sc2);

							Quad2d.getQuad2dInstance().repaint();
							// repaint_soilpanel();

							if (Hexagon.getHexagonInstance() != null) {
								Hexagon.getHexagonInstance().repaint();
							}
						}
					});
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (InvocationTargetException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

			}
		});
		btnResize.setBounds(10, 360, 170, 29);
		btnResize.setText("Resize");

		Label lblSpeed = new Label(composite, SWT.NONE);
		lblSpeed.setBackground(org.eclipse.wb.swt.SWTResourceManager.getColor(SWT.COLOR_WHITE));
		//lblSpeed.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		lblSpeed.setBounds(20, 148, 170, 23);
		lblSpeed.setText("Speed");

		lblFieldsizeY = new Label(composite, SWT.NONE);
		lblFieldsizeY.setBackground(org.eclipse.wb.swt.SWTResourceManager.getColor(SWT.COLOR_WHITE));
		//lblFieldsizeY.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		lblFieldsizeY.setBounds(20, 225, 170, 23);
		lblFieldsizeY.setText("Fieldsize Y -> " + Field.getHeight());

		lblFieldsizeX = new Label(composite, SWT.NONE);
		lblFieldsizeX.setBackground(org.eclipse.wb.swt.SWTResourceManager.getColor(SWT.COLOR_WHITE));
		//lblFieldsizeX.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		lblFieldsizeX.setBounds(20, 302, 170, 23);
		lblFieldsizeX.setText("Fieldsize X -> " + Field.getWidth());

		/*Composite top = new Composite(bar, SWT.NO_BACKGROUND | SWT.EMBEDDED);
		try {
			System.setProperty("sun.awt.noerasebackground", "true");
		} catch (NoSuchMethodError error) {
		}

		java.awt.Frame frame = SWT_AWT.new_Frame(top);

		java.awt.Panel panel = new java.awt.Panel(new java.awt.BorderLayout()) {
			public void update(java.awt.Graphics g) {

				paint(g);
			}
		};

		frame.add(panel);
		JRootPane root = new JRootPane();
		panel.add(root);
		java.awt.Container contentPane = root.getContentPane();
		//pan = new Infopan();

		Time.setPause(true);

		//contentPane.add(pan);

		ExpandItem item0 = new ExpandItem(bar, SWT.NONE, 0);
		item0.setExpanded(true);
		item0.setText("Time and Space");
		item0.setHeight(400);
		item0.setControl(top);*/

	}

	@Override
	public void setFocus() {

	}

	/*public static Infopan getPan() {
		return pan;
	}

	public static void setPan(Infopan pan) {
		InfoView.pan = pan;
	}*/

	public static String getTimeSteps() {
		return timeSteps;
	}

	public static void setTimeSteps(String timeSteps) {
		InfoView.timeSteps = timeSteps;
	}

	public static String getMonth() {
		return month;
	}

	public static void setMonth(String month) {
		InfoView.month = month;
		
	}

	public static String getYear() {
		return year;
	}

	public static void setYear(String year) {
		InfoView.year = year;
		lblYear.setText(year);
		
	}
	
}
