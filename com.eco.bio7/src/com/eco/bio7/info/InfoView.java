package com.eco.bio7.info;

import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
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
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;

public class InfoView extends ViewPart {

	public InfoView() {

	}

	public static final String ID = "com.eco.bio7.control";

	private static Scale timeSwtScale;

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
		// parent.setBackgroundMode(SWT.INHERIT_FORCE);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.eco.bio7.control");
		ScrolledComposite scroll = new ScrolledComposite(parent, SWT.V_SCROLL);
		Composite composite = new Composite(scroll, SWT.NONE);
		composite.setBackground(parent.getBackground());
		composite.setLayout(new GridLayout(1, true));
		scroll.setMinSize(200, 500);
		scroll.setContent(composite);
		scroll.setExpandVertical(true);
		scroll.setExpandHorizontal(true);
		scroll.setAlwaysShowScrollBars(true);

		lblTimesteps = new Label(composite, SWT.NONE);
		GridData gd_lblTimesteps = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lblTimesteps.heightHint = 35;
		lblTimesteps.setLayoutData(gd_lblTimesteps);
		lblTimesteps.setBackground(parent.getBackground());
		lblTimesteps.setText(timeSteps + Time.getTime());

		lblMonth = new Label(composite, SWT.NONE);
		GridData gd_lblMonth = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lblMonth.heightHint = 35;
		lblMonth.setLayoutData(gd_lblMonth);
		lblMonth.setBackground(parent.getBackground());
		lblMonth.setText(month + Time.getMonth());

		lblYear = new Label(composite, SWT.NONE);
		GridData gd_lblYear = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lblYear.heightHint = 35;
		lblYear.setLayoutData(gd_lblYear);
		lblYear.setBackground(parent.getBackground());
		lblYear.setText(year + Time.getYear());

		timeSwtScale = new Scale(composite, SWT.NONE);
		timeSwtScale.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		// scale.setBackground(parent.getBackground());
		// scale.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		timeSwtScale.setMaximum(1000);
		timeSwtScale.setMinimum(1);
		timeSwtScale.setSelection(500);
		timeSwtScale.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Time.setInterval(timeSwtScale.getSelection());
			}
		});

		Label lblSpeed = new Label(composite, SWT.NONE);
		GridData gd_lblSpeed = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lblSpeed.heightHint = 35;
		lblSpeed.setLayoutData(gd_lblSpeed);
		lblSpeed.setBackground(parent.getBackground());
		lblSpeed.setText("Speed");

		scale_1 = new Scale(composite, SWT.NONE);
		scale_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		// scale_1.setBackground(parent.getBackground());
		// scale_1.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));

		scale_1.addSelectionListener(new SelectionAdapter() {
			private int sc1;
			private int sc2;

			@Override
			public void widgetSelected(SelectionEvent e) {
				sc1 = scale_1.getSelection();
				sc2 = scale_2.getSelection();

				if (resizable) {
					ResizeArray.update(sc1, sc2);

					Quad2d.getQuad2dInstance().fullRedrawAll();
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							// !!
							public void run() {

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

		lblFieldsizeY = new Label(composite, SWT.NONE);
		GridData gd_lblFieldsizeY = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lblFieldsizeY.heightHint = 35;
		lblFieldsizeY.setLayoutData(gd_lblFieldsizeY);
		lblFieldsizeY.setBackground(parent.getBackground());
		lblFieldsizeY.setText("Fieldsize Y -> " + Field.getHeight());

		scale_2 = new Scale(composite, SWT.NONE);
		scale_2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		// scale_2.setBackground(parent.getBackground());
		// scale_2.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		scale_2.addSelectionListener(new SelectionAdapter() {
			private int sc1;
			private int sc2;

			@Override
			public void widgetSelected(SelectionEvent e) {
				sc1 = scale_1.getSelection();
				sc2 = scale_2.getSelection();
				if (resizable) {
					ResizeArray.update(sc1, sc2);

					Quad2d.getQuad2dInstance().fullRedrawAll();
					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							// !!
							public void run() {

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

		lblFieldsizeX = new Label(composite, SWT.NONE);
		GridData gd_lblFieldsizeX = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lblFieldsizeX.heightHint = 35;
		lblFieldsizeX.setLayoutData(gd_lblFieldsizeX);
		lblFieldsizeX.setBackground(parent.getBackground());
		lblFieldsizeX.setText("Fieldsize X -> " + Field.getWidth());

		btnResizeAfterAdjust = new Button(composite, SWT.CHECK);
		GridData gd_btnResizeAfterAdjust = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_btnResizeAfterAdjust.heightHint = 35;
		btnResizeAfterAdjust.setLayoutData(gd_btnResizeAfterAdjust);

		btnResizeAfterAdjust.setBackground(parent.getBackground());

		btnResizeAfterAdjust.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				resizable = !resizable;
			}
		});
		btnResizeAfterAdjust.setText("Resize after adjust");

		Button btnResize = new Button(composite, SWT.NONE);
		GridData gd_btnResize = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_btnResize.heightHint = 35;
		btnResize.setLayoutData(gd_btnResize);
		btnResize.addSelectionListener(new SelectionAdapter() {
			private int sc1;
			private int sc2;

			@Override
			public void widgetSelected(SelectionEvent e) {
				sc1 = scale_1.getSelection();
				sc2 = scale_2.getSelection();
				ResizeArray.update(sc1, sc2);

				Quad2d.getQuad2dInstance().fullRedrawAll();

				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						// !!
						public void run() {

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
		btnResize.setText("Resize");

		/*
		 * Composite top = new Composite(bar, SWT.NO_BACKGROUND | SWT.EMBEDDED); try {
		 * System.setProperty("sun.awt.noerasebackground", "true"); } catch
		 * (NoSuchMethodError error) { }
		 * 
		 * java.awt.Frame frame = SWT_AWT.new_Frame(top);
		 * 
		 * java.awt.Panel panel = new java.awt.Panel(new java.awt.BorderLayout()) {
		 * public void update(java.awt.Graphics g) {
		 * 
		 * paint(g); } };
		 * 
		 * frame.add(panel); JRootPane root = new JRootPane(); panel.add(root);
		 * java.awt.Container contentPane = root.getContentPane(); //pan = new
		 * Infopan();
		 * 
		 * Time.setPause(true);
		 * 
		 * //contentPane.add(pan);
		 * 
		 * ExpandItem item0 = new ExpandItem(bar, SWT.NONE, 0); item0.setExpanded(true);
		 * item0.setText("Time and Space"); item0.setHeight(400); item0.setControl(top);
		 */

	}

	public static Scale getTimeScaleSwt() {
		return timeSwtScale;
	}

	@Override
	public void setFocus() {

	}

	/*
	 * public static Infopan getPan() { return pan; }
	 * 
	 * public static void setPan(Infopan pan) { InfoView.pan = pan; }
	 */

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
