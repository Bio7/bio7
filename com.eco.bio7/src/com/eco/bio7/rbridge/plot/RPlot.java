package com.eco.bio7.rbridge.plot;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.RScript;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RShellView;
import com.eco.bio7.rbridge.RState;

public class RPlot extends Composite {

	private CTabItem plotTabItem;
	private Composite composite_1;
	private Button plotButton;
	private FormData fd_plotButton;
	private CTabFolder tab;
	private Button histButton;
	private FormData fd_histButton;
	private Button pieButton;
	private FormData fd_pieButton;
	private Text titleText;
	private FormData fd_titleText;
	private Label titleLabel;
	private FormData fd_titleLabel;
	private Text xText;
	private FormData fd_xText;
	private Text yText;
	private FormData fd_yText;
	private Label label;
	private FormData fd_label;
	private Label label_1;
	private FormData fd_label_1;
	private Button xyButton;
	private FormData fd_xyButton;
	private Button sendButton;
	private FormData fd_sendButton;
	private Text text_1;
	private FormData fd_text_1;
	private Menu menu_3;
	private MenuItem newItemMenuItem_21;
	private MenuItem newItemMenuItem_18;
	private MenuItem menuItem_1;
	private MenuItem newItemMenuItem_14;
	private MenuItem newItemMenuItem_16;
	private MenuItem newItemMenuItem_17;
	private MenuItem newItemMenuItem_15;
	private MenuItem menuItem_2;
	private Button plotButton_1;
	private FormData fd_plotButton_1;
	private Button perspButton;
	private FormData fd_perspButton;
	private Button contourButton;
	private FormData fd_contourButton;
	private Button voronoiButton;
	private FormData fd_pdfCheckBox;
	private FormData fd_voronoiButton;
	private Button imageButton;
	private FormData fd_imageButton;

	// private RShellView rShellView;
	// private List listShell;

	public RPlot(Composite parent, int style, CTabItem plotTabItem_) {
		super(parent, style);
		// tab=folderTab;
		// listShell = listShell_;
		plotTabItem = plotTabItem_;

		composite_1 = this;
		composite_1.setLayout(new FormLayout());
		plotTabItem.setControl(this);

		plotButton = new Button(composite_1, SWT.NONE);
		plotButton.setText("Boxplot");
		fd_plotButton = new FormData();
		plotButton.setLayoutData(fd_plotButton);
		// plotButton.setToolTipText("Draws a boxplot from a vector or a dataframe.");
		plotButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						List listShell = RShellView.getListShell();
						String[] data = listShell.getSelection();
						if (data.length > 0) {
							/*
							 * if (pdfCheckBox.getSelection() == false) {
							 * 
							 * PlotJob.setPdf(true);
							 * PlotJob.setPlotInch(spinnerInchX.getSelection(),
							 * spinnerInchY.getSelection()); } else {
							 * PlotJob.setPdf(false);
							 * PlotJob.setPlotPixel(spinnerX.getSelection(),
							 * spinnerY.getSelection()); }
							 */

							if (text_1.isEnabled() == false) {
								RServe.printJob("boxplot(" + data[0] + ",main=\"" + titleText.getText() + "\",xlab=\"" + xText.getText() + "\",ylab=\"" + yText.getText() + "\")");

							} else {
								text_1.setText("boxplot(" + data[0] + ",main=\"" + titleText.getText() + "\",xlab=\"" + xText.getText() + "\",ylab=\"" + yText.getText() + "\")");
							}
						} else {
							Bio7Dialog.message("No data selected!");
						}
					} else {
						Bio7Dialog.message("Rserve is busy!");
					}
				}
			}
		});

		/*
		 * pdfCheckBox = new Button(composite_1, SWT.CHECK);
		 * pdfCheckBox.addSelectionListener(new SelectionAdapter() {
		 * 
		 * public void widgetSelected(SelectionEvent e) { if
		 * (pdfCheckBox.getSelection() == false) { PlotJob.setPdf(true); } else
		 * { PlotJob.setPdf(false); } } });
		 */
		fd_pdfCheckBox = new FormData();
		/*
		 * fd_pdfCheckBox.bottom = new FormAttachment(0, 21); fd_pdfCheckBox.top
		 * = new FormAttachment(0, 5);
		 * pdfCheckBox.setLayoutData(fd_pdfCheckBox);
		 * pdfCheckBox.setToolTipText(
		 * "If selected plot is displayed\n in the ImageJ view.");
		 * pdfCheckBox.setText("IJ");
		 */

		histButton = new Button(composite_1, SWT.NONE);
		fd_histButton = new FormData();
		fd_histButton.bottom = new FormAttachment(0, 56);
		fd_histButton.top = new FormAttachment(0, 31);
		fd_histButton.right = new FormAttachment(0, 70);
		fd_histButton.left = new FormAttachment(0, 0);
		histButton.setLayoutData(fd_histButton);
		// histButton.setToolTipText("Draws a histogram from a vector");
		histButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						List listShell = RShellView.getListShell();
						String[] data = listShell.getSelection();
						if (data.length > 0) {
							/*
							 * if (pdfCheckBox.getSelection() == false) {
							 * 
							 * PlotJob.setPdf(true);
							 * PlotJob.setPlotInch(spinnerInchX.getSelection(),
							 * spinnerInchY.getSelection()); } else {
							 * PlotJob.setPdf(false);
							 * PlotJob.setPlotPixel(spinnerX.getSelection(),
							 * spinnerY.getSelection()); }
							 */
							if (text_1.isEnabled() == false) {
								RServe.printJob("hist(" + data[0] + ",main=\"" + titleText.getText() + "\",xlab=\"" + xText.getText() + "\",ylab=\"" + yText.getText() + "\")");
							} else {
								text_1.setText("hist(" + data[0] + ",main=\"" + titleText.getText() + "\",xlab=\"" + xText.getText() + "\",ylab=\"" + yText.getText() + "\")");
							}
						} else {
							Bio7Dialog.message("No data selected!");
						}
					} else {
						Bio7Dialog.message("Rserve is busy!");
					}
				}
			}
		});
		histButton.setText("Hist");

		pieButton = new Button(composite_1, SWT.NONE);
		fd_pieButton = new FormData();
		fd_pieButton.bottom = new FormAttachment(0, 56);
		fd_pieButton.top = new FormAttachment(0, 31);
		fd_pieButton.right = new FormAttachment(0, 140);
		fd_pieButton.left = new FormAttachment(0, 70);
		pieButton.setLayoutData(fd_pieButton);
		// pieButton.setToolTipText("Draws a piechart from a vector");
		pieButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						List listShell = RShellView.getListShell();
						String[] data = listShell.getSelection();
						if (data.length > 0) {
							/*
							 * if (pdfCheckBox.getSelection() == false) {
							 * 
							 * PlotJob.setPdf(true);
							 * PlotJob.setPlotInch(spinnerInchX.getSelection(),
							 * spinnerInchY.getSelection()); } else {
							 * PlotJob.setPdf(false);
							 * PlotJob.setPlotPixel(spinnerX.getSelection(),
							 * spinnerY.getSelection()); }
							 */
							if (text_1.isEnabled() == false) {
								RServe.printJob("pie(" + data[0] + ",main=\"" + titleText.getText() + "\")");
							} else {
								text_1.setText("pie(" + data[0] + ",main=\"" + titleText.getText() + "\")");
							}
						} else {
							Bio7Dialog.message("No data selected!");
						}

					} else {
						Bio7Dialog.message("Rserve is busy!");
					}
				}
			}
		});
		pieButton.setText("Pie");

		titleText = new Text(composite_1, SWT.BORDER);
		fd_titleText = new FormData();
		fd_titleText.bottom = new FormAttachment(0, 87);
		fd_titleText.top = new FormAttachment(0, 62);
		fd_titleText.right = new FormAttachment(0, 110);
		fd_titleText.left = new FormAttachment(0, 0);
		titleText.setLayoutData(fd_titleText);
		titleText.setText("Plot");

		titleLabel = new Label(composite_1, SWT.NONE);
		fd_titleLabel = new FormData();
		fd_titleLabel.bottom = new FormAttachment(0, 77);
		fd_titleLabel.top = new FormAttachment(0, 62);
		fd_titleLabel.left = new FormAttachment(0, 116);
		titleLabel.setLayoutData(fd_titleLabel);
		titleLabel.setText("Title");
		xText = new Text(composite_1, SWT.BORDER);
		fd_xText = new FormData();
		fd_xText.bottom = new FormAttachment(0, 117);
		fd_xText.top = new FormAttachment(0, 92);
		fd_xText.right = new FormAttachment(0, 110);
		fd_xText.left = new FormAttachment(0, 0);
		xText.setLayoutData(fd_xText);
		xText.setText("x");

		yText = new Text(composite_1, SWT.BORDER);
		fd_yText = new FormData();
		fd_yText.bottom = new FormAttachment(0, 148);
		fd_yText.top = new FormAttachment(0, 123);
		fd_yText.right = new FormAttachment(0, 110);
		fd_yText.left = new FormAttachment(0, 0);
		yText.setLayoutData(fd_yText);
		yText.setText("y");

		label = new Label(composite_1, SWT.NONE);
		fd_label = new FormData();
		fd_label.bottom = new FormAttachment(0, 110);
		fd_label.top = new FormAttachment(0, 95);
		fd_label.left = new FormAttachment(0, 116);
		label.setLayoutData(fd_label);
		label.setText("X-label");

		label_1 = new Label(composite_1, SWT.NONE);
		fd_label_1 = new FormData();
		fd_label_1.bottom = new FormAttachment(0, 138);
		fd_label_1.top = new FormAttachment(0, 123);
		fd_label_1.right = new FormAttachment(0, 168);
		fd_label_1.left = new FormAttachment(0, 116);
		label_1.setLayoutData(fd_label_1);
		label_1.setText("Y-label");

		xyButton = new Button(composite_1, SWT.NONE);
		fd_plotButton.top = new FormAttachment(xyButton, -25, SWT.BOTTOM);
		fd_plotButton.bottom = new FormAttachment(xyButton, 0, SWT.BOTTOM);
		fd_plotButton.left = new FormAttachment(xyButton, -70, SWT.LEFT);
		fd_plotButton.right = new FormAttachment(xyButton, 0, SWT.LEFT);
		fd_xyButton = new FormData();
		fd_xyButton.left = new FormAttachment(0, 70);
		fd_xyButton.right = new FormAttachment(0, 140);
		xyButton.setLayoutData(fd_xyButton);
		// xyButton.setToolTipText("Executes the general plot command.\nOne or two(x,y) arguments can be plotted.");
		xyButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						List listShell = RShellView.getListShell();

						String[] selections = listShell.getSelection();
						if (selections.length > 0) {
							/*
							 * if (pdfCheckBox.getSelection() == false) {
							 * 
							 * PlotJob.setPdf(true);
							 * PlotJob.setPlotInch(spinnerInchX.getSelection(),
							 * spinnerInchY.getSelection()); } else {
							 * PlotJob.setPdf(false);
							 * PlotJob.setPlotPixel(spinnerX.getSelection(),
							 * spinnerY.getSelection()); }
							 */
							if (selections.length == 2) {
								if (text_1.isEnabled() == false) {

									RServe.printJob("plot(" + selections[0] + "," + selections[1] + ",main=\"" + titleText.getText() + "\",xlab=\"" + xText.getText() + "\",ylab=\"" + yText.getText()
											+ "\")");
								} else {
									text_1.setText("plot(" + selections[0] + "," + selections[1] + ",main=\"" + titleText.getText() + "\",xlab=\"" + xText.getText() + "\",ylab=\"" + yText.getText()
											+ "\")");
								}

							} else if (selections.length == 1) {

								if (text_1.isEnabled() == false) {

									RServe.printJob("plot(" + selections[0] + ",main=\"" + titleText.getText() + "\",xlab=\"" + xText.getText() + "\",ylab=\"" + yText.getText() + "\")");
								} else {
									text_1.setText("plot(" + selections[0] + ",main=\"" + titleText.getText() + "\",xlab=\"" + xText.getText() + "\",ylab=\"" + yText.getText() + "\")");
								}

							}
						} else {

							Bio7Dialog.message("No data selected!");

						}
					} else {
						Bio7Dialog.message("Rserve is busy!");
					}
				}
			}
		});
		xyButton.setText("Plot");

		sendButton = new Button(composite_1, SWT.CHECK);
		fd_sendButton = new FormData();
		fd_sendButton.right = new FormAttachment(label_1, 0, SWT.RIGHT);
		fd_sendButton.bottom = new FormAttachment(0, 191);
		fd_sendButton.top = new FormAttachment(0, 175);
		sendButton.setLayoutData(fd_sendButton);
		sendButton.setToolTipText("Transfers the plotting commands\nto the text panel for customisation");

		sendButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (sendButton.getSelection()) {
					text_1.setEnabled(true);

				} else {
					text_1.setEnabled(false);
				}
			}
		});
		sendButton.setText("Detour");

		text_1 = new Text(composite_1, SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER);
		fd_text_1 = new FormData();
		fd_text_1.right = new FormAttachment(100, -5);
		fd_text_1.bottom = new FormAttachment(100, 0);
		fd_text_1.top = new FormAttachment(0, 199);
		fd_text_1.left = new FormAttachment(0, 0);
		text_1.setLayoutData(fd_text_1);
		text_1.setEnabled(false);

		menu_3 = new Menu(text_1);
		text_1.setMenu(menu_3);

		newItemMenuItem_21 = new MenuItem(menu_3, SWT.NONE);
		newItemMenuItem_21.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				text_1.insert("plot(c(0:10),c(0:10),xlab=\"Time\",ylab=\"Count\",xlim=c(0,10),ylim=c(0,12))");
			}
		});
		newItemMenuItem_21.setText("Example plot");

		new MenuItem(menu_3, SWT.SEPARATOR);

		newItemMenuItem_18 = new MenuItem(menu_3, SWT.NONE);
		newItemMenuItem_18.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				text_1.insert("legend(1,8,legend=c('x-value','y-value'),col=c('black','red'),lwd=3)");
			}
		});
		newItemMenuItem_18.setText("Legend");

		menuItem_1 = new MenuItem(menu_3, SWT.NONE);
		menuItem_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				text_1.insert("text(3,9,cex=1.4,expression(f(x)==sqrt(x)*~~e^frac(1,sqrt(12))))");

			}
		});
		menuItem_1.setText("Add Expression");

		newItemMenuItem_14 = new MenuItem(menu_3, SWT.NONE);
		newItemMenuItem_14.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				text_1.insert("text(2,4,\"Text!\")");

			}
		});
		newItemMenuItem_14.setText("Text");

		newItemMenuItem_16 = new MenuItem(menu_3, SWT.NONE);
		newItemMenuItem_16.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				text_1.insert("points(5,10)");
			}
		});
		newItemMenuItem_16.setText("Points");

		newItemMenuItem_17 = new MenuItem(menu_3, SWT.NONE);
		newItemMenuItem_17.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				text_1.insert("lines(c(1,2,3),c(10,10,10))");
			}
		});
		newItemMenuItem_17.setText("Lines");

		new MenuItem(menu_3, SWT.SEPARATOR);

		newItemMenuItem_15 = new MenuItem(menu_3, SWT.NONE);
		newItemMenuItem_15.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				String par = "par(mfrow=c(2,2))\nplot(runif(100))\nplot(runif(100))\nplot(runif(100))\nplot(runif(100))";
				text_1.insert(par);
			}
		});
		newItemMenuItem_15.setText("Arrange 2x2 plots");

		menuItem_2 = new MenuItem(menu_3, SWT.NONE);
		menuItem_2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				text_1.insert("curve(sin(x))");
			}
		});
		menuItem_2.setText("Curve");

		plotButton_1 = new Button(composite_1, SWT.NONE);
		fd_sendButton.left = new FormAttachment(plotButton_1, 5, SWT.RIGHT);
		fd_plotButton_1 = new FormData();
		fd_plotButton_1.top = new FormAttachment(text_1, -30, SWT.TOP);
		fd_plotButton_1.bottom = new FormAttachment(text_1, -5, SWT.TOP);
		fd_plotButton_1.right = new FormAttachment(yText, 90, SWT.LEFT);
		fd_plotButton_1.left = new FormAttachment(yText, 0, SWT.LEFT);
		plotButton_1.setLayoutData(fd_plotButton_1);
		plotButton_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						/*
						 * if (pdfCheckBox.getSelection() == false) {
						 * 
						 * PlotJob.setPdf(true);
						 * PlotJob.setPlotInch(spinnerInchX.getSelection(),
						 * spinnerInchY.getSelection()); } else {
						 * PlotJob.setPdf(false);
						 * PlotJob.setPlotPixel(spinnerX.getSelection(),
						 * spinnerY.getSelection()); }
						 */
						RScript.rScriptJob(text_1.getText(), null);

					}
				} else {
					Bio7Dialog.message("Rserve is busy!");
				}
			}
		});
		plotButton_1.setText("Draw Plot");

		perspButton = new Button(composite_1, SWT.NONE);
		fd_xyButton.bottom = new FormAttachment(perspButton, 25, SWT.TOP);
		fd_xyButton.top = new FormAttachment(perspButton, 0, SWT.TOP);
		fd_perspButton = new FormData();
		fd_perspButton.right = new FormAttachment(0, 210);
		fd_perspButton.left = new FormAttachment(0, 140);
		fd_perspButton.bottom = new FormAttachment(xyButton, 25, SWT.TOP);
		fd_perspButton.top = new FormAttachment(xyButton, 0, SWT.TOP);
		perspButton.setLayoutData(fd_perspButton);
		perspButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						List listShell = RShellView.getListShell();
						String[] data = listShell.getSelection();
						if (data.length > 0) {
							/*
							 * if (pdfCheckBox.getSelection() == false) {
							 * 
							 * PlotJob.setPdf(true);
							 * PlotJob.setPlotInch(spinnerInchX.getSelection(),
							 * spinnerInchY.getSelection()); } else {
							 * PlotJob.setPdf(false);
							 * PlotJob.setPlotPixel(spinnerX.getSelection(),
							 * spinnerY.getSelection()); }
							 */

							if (text_1.isEnabled() == false) {
								RServe.printJob("persp(" + data[0] + ",main=\"" + titleText.getText() + "\",theta = 30, phi = 30, expand = 0.5, col = \"grey\")");
							} else {
								text_1.setText("persp(" + data[0] + ",main=\"" + titleText.getText() + "\",theta = 30, phi = 30, expand = 0.5, col = \"grey\")");
							}
						} else {
							Bio7Dialog.message("No data selected!");
						}
					} else {
						Bio7Dialog.message("Rserve is busy!");
					}
				}
			}
		});
		perspButton.setToolTipText("Draws a perspective plot.\nA matrix is required by default");
		perspButton.setText("Persp.");

		contourButton = new Button(composite_1, SWT.NONE);
		fd_contourButton = new FormData();
		fd_contourButton.top = new FormAttachment(pieButton, -25, SWT.BOTTOM);
		fd_contourButton.bottom = new FormAttachment(pieButton, 0, SWT.BOTTOM);
		fd_contourButton.right = new FormAttachment(xyButton, 70, SWT.RIGHT);
		fd_contourButton.left = new FormAttachment(xyButton, 0, SWT.RIGHT);
		contourButton.setLayoutData(fd_contourButton);
		contourButton.setToolTipText("Draws a contour plot.\nA matrix is required by default");
		contourButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						List listShell = RShellView.getListShell();
						String[] data = listShell.getSelection();
						if (data.length > 0) {
							/*
							 * if (pdfCheckBox.getSelection() == false) {
							 * 
							 * PlotJob.setPdf(true);
							 * PlotJob.setPlotInch(spinnerInchX.getSelection(),
							 * spinnerInchY.getSelection()); } else {
							 * PlotJob.setPdf(false);
							 * PlotJob.setPlotPixel(spinnerX.getSelection(),
							 * spinnerY.getSelection()); }
							 */

							if (text_1.isEnabled() == false) {
								RServe.printJob("contour(" + data[0] + ",main=\"" + titleText.getText() + "\")");
							} else {
								text_1.setText("contour(" + data[0] + ",main=\"" + titleText.getText() + "\")");
							}
						} else {
							Bio7Dialog.message("No data selected!");
						}
					} else {
						Bio7Dialog.message("Rserve is busy!");
					}
				}
			}
		});
		contourButton.setText("Contour");

		/*
		 * spinnerX = new Spinner(composite_1, SWT.BORDER);
		 * spinnerX.addSelectionListener(new SelectionAdapter() {
		 * 
		 * @Override public void widgetSelected(SelectionEvent e) {
		 * IPreferenceStore store =
		 * Bio7Plugin.getDefault().getPreferenceStore(); String
		 * old=store.getString("DEVICE_DEFINITION"); String
		 * path=store.getString(PreferenceConstants.P_TEMP_R); int
		 * valx=spinnerX.getSelection(); String newValue=old.replace(arg0, arg1)
		 * store.setValue("DEVICE_DEFINITION",
		 * "bio7Device <- function(filename = \""
		 * +path+"tempDevicePlot.png"+"\") { png(filename,width = "
		 * +valx+", height = 480, units = \"px\")}; options(device=\"bio7Device\")"
		 * ); } }); fd_titleLabel.right = new FormAttachment(spinnerX, 0,
		 * SWT.LEFT); final FormData fd_spinnerX = new FormData();
		 * fd_spinnerX.bottom = new FormAttachment(0, 85); fd_spinnerX.top = new
		 * FormAttachment(0, 62); fd_spinnerX.right = new FormAttachment(0,
		 * 213); fd_spinnerX.left = new FormAttachment(0, 161);
		 * spinnerX.setLayoutData(fd_spinnerX);
		 * 
		 * spinnerX.setMinimum(100); spinnerX.setMaximum(3000);
		 * spinnerX.setSelection(500);
		 */

		/*
		 * spinnerY = new Spinner(composite_1, SWT.BORDER);
		 * spinnerY.addSelectionListener(new SelectionAdapter() {
		 * 
		 * @Override public void widgetSelected(SelectionEvent e) {
		 * IPreferenceStore store =
		 * Bio7Plugin.getDefault().getPreferenceStore(); String
		 * path=store.getString(PreferenceConstants.P_TEMP_R); int
		 * valy=spinnerY.getSelection(); store.setValue("DEVICE_DEFINITION",
		 * "bio7Device <- function(filename = \""
		 * +path+"tempDevicePlot.png"+"\") { png(filename,width = 480, height = "
		 * +valy+", units = \"px\")}; options(device=\"bio7Device\")"); } });
		 * final FormData fd_spinnerY = new FormData(); fd_spinnerY.bottom = new
		 * FormAttachment(0, 85); fd_spinnerY.top = new FormAttachment(0, 62);
		 * fd_spinnerY.right = new FormAttachment(0, 271); fd_spinnerY.left =
		 * new FormAttachment(0, 219); spinnerY.setLayoutData(fd_spinnerY);
		 * 
		 * spinnerY.setMinimum(100); spinnerY.setMaximum(3000);
		 * spinnerY.setSelection(500);
		 */

		/*
		 * spinnerInchX = new Spinner(composite_1, SWT.BORDER);
		 * spinnerInchX.addSelectionListener(new SelectionAdapter() {
		 * 
		 * @Override public void widgetSelected(SelectionEvent e) {
		 * IPreferenceStore store =
		 * Bio7Plugin.getDefault().getPreferenceStore(); } }); fd_label.right =
		 * new FormAttachment(spinnerInchX, 0, SWT.LEFT); final FormData
		 * fd_spinnerInchX = new FormData(); fd_spinnerInchX.bottom = new
		 * FormAttachment(label_1, -5, SWT.TOP); fd_spinnerInchX.top = new
		 * FormAttachment(0, 95); fd_spinnerInchX.right = new FormAttachment(0,
		 * 213); fd_spinnerInchX.left = new FormAttachment(0, 161);
		 * spinnerInchX.setLayoutData(fd_spinnerInchX);
		 * spinnerInchX.setSelection(5); spinnerInchX.setMinimum(1);
		 * spinnerInchX.setMaximum(40);
		 * 
		 * spinnerInchY = new Spinner(composite_1, SWT.BORDER);
		 * spinnerInchY.addSelectionListener(new SelectionAdapter() {
		 * 
		 * @Override public void widgetSelected(SelectionEvent e) {
		 * IPreferenceStore store =
		 * Bio7Plugin.getDefault().getPreferenceStore(); } }); final FormData
		 * fd_spinnerInchY = new FormData(); fd_spinnerInchY.bottom = new
		 * FormAttachment(0, 118); fd_spinnerInchY.top = new FormAttachment(0,
		 * 95); fd_spinnerInchY.right = new FormAttachment(0, 271);
		 * fd_spinnerInchY.left = new FormAttachment(0, 219);
		 * spinnerInchY.setLayoutData(fd_spinnerInchY);
		 * spinnerInchY.setSelection(5); spinnerInchY.setMinimum(1);
		 * spinnerInchY.setMaximum(40);
		 */

		/*
		 * final Label pixelLabel = new Label(composite_1, SWT.NONE); final
		 * FormData fd_pixelLabel = new FormData(); fd_pixelLabel.right = new
		 * FormAttachment(0, 360); fd_pixelLabel.bottom = new FormAttachment(0,
		 * 80); fd_pixelLabel.top = new FormAttachment(0, 65);
		 * fd_pixelLabel.left = new FormAttachment(0, 277);
		 * pixelLabel.setLayoutData(fd_pixelLabel);
		 * pixelLabel.setText("Image Pixel");
		 * 
		 * final Label inchLabel = new Label(composite_1, SWT.NONE); final
		 * FormData fd_inchLabel = new FormData(); fd_inchLabel.right = new
		 * FormAttachment(pixelLabel, 0, SWT.RIGHT); fd_inchLabel.bottom = new
		 * FormAttachment(0, 110); fd_inchLabel.top = new FormAttachment(0, 95);
		 * fd_inchLabel.left = new FormAttachment(0, 277);
		 * inchLabel.setLayoutData(fd_inchLabel); inchLabel.setText("Pdf Inch");
		 */

		voronoiButton = new Button(composite_1, SWT.NONE);
		fd_pdfCheckBox.right = new FormAttachment(voronoiButton, 70, SWT.RIGHT);
		fd_pdfCheckBox.left = new FormAttachment(voronoiButton, 5, SWT.RIGHT);
		fd_voronoiButton = new FormData();
		fd_voronoiButton.bottom = new FormAttachment(perspButton, 25, SWT.TOP);
		fd_voronoiButton.top = new FormAttachment(perspButton, 0, SWT.TOP);
		fd_voronoiButton.right = new FormAttachment(perspButton, 70, SWT.RIGHT);
		fd_voronoiButton.left = new FormAttachment(perspButton, 0, SWT.RIGHT);
		voronoiButton.setLayoutData(fd_voronoiButton);
		voronoiButton.setToolTipText("Draws a voronoi plot.\nTwo arguments(x,y) are required");
		voronoiButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						RConnection c = RServe.getConnection();
						try {
							c.eval("try(library(tripack))");
						} catch (RserveException e1) {

							e1.printStackTrace();
						}
						List listShell = RShellView.getListShell();
						String[] selections = listShell.getSelection();
						/*
						 * if (pdfCheckBox.getSelection() == false) {
						 * 
						 * PlotJob.setPdf(true);
						 * PlotJob.setPlotInch(spinnerInchX.getSelection(),
						 * spinnerInchY.getSelection()); } else {
						 * PlotJob.setPdf(false);
						 * PlotJob.setPlotPixel(spinnerX.getSelection(),
						 * spinnerY.getSelection()); }
						 */
						if (selections.length == 2) {

							try {
								c.eval("try(tri.vm <- voronoi.mosaic(" + selections[0] + "," + selections[1] + ",duplicate=\"remove\"))");
								c.eval("try(tri.vm.areas <- voronoi.area(tri.vm))");
							} catch (RserveException e1) {

								e1.printStackTrace();
							}

							if (text_1.isEnabled() == false) {

								RServe.printJob("plot(tri.vm,main=\"" + titleText.getText() + "\",xlab=\"" + xText.getText() + "\",ylab=\"" + yText.getText() + "\");" + "text(" + selections[0] + ","
										+ selections[1] + ", tri.vm.areas, cex=0.5);" + "points(" + selections[0] + "," + selections[1] + ",cex=0.5)");
							} else {
								text_1.setText("plot(tri.vm,main=\"" + titleText.getText() + "\",xlab=\"" + xText.getText() + "\",ylab=\"" + yText.getText() + "\");" + "text(" + selections[0] + ","
										+ selections[1] + ", tri.vm.areas, cex=0.5);" + "points(" + selections[0] + "," + selections[1] + ",cex=0.5)");
							}

						} else if (selections.length <= 1) {
							Bio7Dialog.message("Voronoi plot needs two arguments (x,y)!");
						}
					} else {
						Bio7Dialog.message("Rserve is busy!");
					}
				}
			}
		});
		voronoiButton.setText("Voronoi");

		imageButton = new Button(composite_1, SWT.NONE);
		imageButton.setToolTipText("Draws the image plot from a matrix");
		imageButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						List listShell = RShellView.getListShell();
						String[] data = listShell.getSelection();
						if (data.length > 0) {
							/*
							 * if (pdfCheckBox.getSelection() == false) {
							 * 
							 * PlotJob.setPdf(true);
							 * PlotJob.setPlotInch(spinnerInchX.getSelection(),
							 * spinnerInchY.getSelection()); } else {
							 * PlotJob.setPdf(false);
							 * PlotJob.setPlotPixel(spinnerX.getSelection(),
							 * spinnerY.getSelection()); }
							 */

							if (text_1.isEnabled() == false) {
								RServe.printJob("image(" + data[0] + ",main=\"" + titleText.getText() + "\",useRaster=TRUE)");
							} else {
								text_1.setText("image(" + data[0] + ",main=\"" + titleText.getText() + "\")");
							}
						} else {
							Bio7Dialog.message("No data selected!");
						}
					} else {
						Bio7Dialog.message("Rserve is busy!");
					}
				}

			}
		});
		fd_imageButton = new FormData();
		fd_imageButton.bottom = new FormAttachment(0, 56);
		fd_imageButton.top = new FormAttachment(0, 31);
		fd_imageButton.right = new FormAttachment(contourButton, 70, SWT.RIGHT);
		fd_imageButton.left = new FormAttachment(contourButton, 0, SWT.RIGHT);
		imageButton.setLayoutData(fd_imageButton);
		imageButton.setText("Image");
	}

}
