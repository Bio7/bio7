/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rbridge;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ExpandAdapter;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.wb.swt.layout.grouplayout.GroupLayout;
import org.osgi.framework.Bundle;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.browser.BrowserView;
import com.eco.bio7.collection.Work;
import com.eco.bio7.compile.BeanShellInterpreter;
import com.eco.bio7.compile.GroovyInterpreter;
import com.eco.bio7.compile.PythonInterpreter;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.compile.RScript;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.os.pid.Pid;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.reditors.REditor;
import com.swtdesigner.ResourceManager;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;

public class RShellView extends ViewPart {
	private Text text_1;
	private Text yText;
	private Text xText;
	private Text titleText;
	private static List listShell;
	private static boolean isConsoleExpanded = true;
	private List list_7;
	private List list_6;
	private List list_5;
	private List list_4;
	private List list_3;
	private List list_2;
	private List list_1;
	private List list;
	public Text text;
	private SimpleContentProposalProvider prov;
	private ContentProposalAdapter adapter;
	private ContentProposalAdapter adapter2;
	private TextContentAdapter textAdapter;
	private TextContentAdapter textAdapter2;
	private KeyStroke stroke;
	private KeyStroke stroke2;
	private String[] history;
	private String[] tempHistory;
	private Button plotButton;
	private Button xyButton;
	// private Button pdfCheckBox;
	/*
	 * private Spinner spinnerInchX; private Spinner spinnerInchY; private
	 * Spinner spinnerX; private Spinner spinnerY;
	 */
	private ExpandItem newItemExpandItem_8;
	protected REXPLogical isDataframe;
	protected REXPLogical isMatrix;
	// private SimpleContentProposalProvider provCompletion;
	private KeyStroke strokeCompletion;
	private static RCompletionShell shellInstance = null;
	private String t;
	private String url;
	protected Object htmlHelpText;
	protected REXPLogical isVector;
	private Button objectsButton;
	private Button iButton;
	private Button xButton;
	private Button removeButton;
	private Button gcButton;
	private Composite composite;
	private Tree tree;
	private IPreferenceStore store;
	private Button sendButton;
	private Button plotButton_1;
	private Button perspButton;
	private Button contourButton;
	private FormData fd_contourButton;
	private Button voronoiButton;
	private Button imageButton;
	private FormData fd_imageButton;
	private FormData fd_perspButton;
	private FormData fd_plotButton_1;
	private FormData fd_sendButton;
	private FormData fd_xyButton;
	private Button histButton;
	private FormData fd_histButton;
	private FormData fd_pdfCheckBox;
	private Composite composite_1;
	private CTabItem plotTabItem;
	private FormData fd_plotButton;
	private Button pieButton;
	private FormData fd_pieButton;
	private FormData fd_titleText;
	private Label titleLabel;
	private FormData fd_xText;
	private FormData fd_yText;
	private Label label;
	private Label label_1;
	private FormData fd_label_1;
	private MenuItem newItemMenuItem_18;
	private MenuItem menuItem_1;
	private MenuItem newItemMenuItem_14;
	private MenuItem newItemMenuItem_16;
	private MenuItem newItemMenuItem_17;
	private MenuItem newItemMenuItem_15;
	private MenuItem menuItem_2;
	private FormData fd_titleLabel;
	private FormData fd_label;
	private FormData fd_text_1;
	private Menu menu_3;
	private MenuItem newItemMenuItem_21;
	private FormData fd_voronoiButton;
	private static RShellView instance;

	public RShellView() {
		instance = this;
	}

	public static List getList_8() {
		return listShell;
	}

	public static void setList_8(List list_8) {
		RShellView.listShell = list_8;
	}

	private static Text textConsole;

	public static Text getTextConsole() {
		return textConsole;
	}

	@Override
	public void createPartControl(final Composite parent) {
		parent.setLayout(new FormLayout());
		parent.addControlListener(new ControlAdapter() {
			public void controlResized(final ControlEvent e) {
				if (newItemExpandItem_8 != null) {
					newItemExpandItem_8.setHeight(parent.getBounds().height - 100);

				}

			}
		});
		initializeToolBar();

		if (RFunctions.getPropsHistInstance() == null) {
			new RFunctions();
		}
		history = RFunctions.getPropsHistInstance().getHistory();

		tempHistory = RFunctions.getPropsHistInstance().getTemphistory();

		prov = new SimpleContentProposalProvider(history);
		// provCompletion = new SimpleContentProposalProvider(new String[] {
		// "a", "b", "c" });

		text = new Text(parent, SWT.SINGLE | SWT.BORDER);
		/* Return key listener! */
		text.addListener(SWT.DefaultSelection, new Listener() {
			public void handleEvent(Event e) {
				evaluate();

			}
		});

		final FormData fd_text = new FormData();
		fd_text.right = new FormAttachment(100, -5);
		fd_text.bottom = new FormAttachment(0, 30);
		fd_text.top = new FormAttachment(0, 5);
		fd_text.left = new FormAttachment(0, 5);
		text.setLayoutData(fd_text);
		text.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_RIGHT) {

					/*
					 * if (RServe.isAliveDialog()) { if (RState.isBusy() ==
					 * false) { RState.setBusy(true); Job job = new
					 * Job("Completion...") {
					 * 
					 * @Override protected IStatus run(IProgressMonitor monitor)
					 * { monitor.beginTask("Completion...",
					 * IProgressMonitor.UNKNOWN); String[] li = null;
					 * 
					 * Display display = PlatformUI.getWorkbench().getDisplay();
					 * display.syncExec(new Runnable() {
					 * 
					 * public void run() { t = text.getText(); } }); try {
					 * RServe.getConnection().eval("try(library('svMisc'))");
					 * 
					 * try { li = RServe.getConnection().eval(
					 * "try(as.vector(as.matrix(CompletePlus(\"" + t +
					 * "\")[1])))").asStrings(); } catch (REXPMismatchException
					 * e) { // TODO Auto-generated catch block
					 * e.printStackTrace(); } if (li != null) {
					 * provCompletion.setProposals(li);
					 * 
					 * } } catch (RserveException e1) {
					 * 
					 * e1.printStackTrace(); }
					 * 
					 * monitor.done(); return Status.OK_STATUS; }
					 * 
					 * }; job.addJobChangeListener(new JobChangeAdapter() {
					 * public void done(IJobChangeEvent event) { if
					 * (event.getResult().isOK()) {
					 * 
					 * RState.setBusy(false); } else {
					 * 
					 * RState.setBusy(false); } } }); // job.setSystem(true);
					 * job.schedule(); } else {
					 * Bio7Dialog.message("RServer is busy!"); } }
					 */

				}

			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub

			}
		});
		/*
		 * ControlDecoration dec = new ControlDecoration(text, SWT.TOP |
		 * SWT.LEFT); FieldDecoration infoFieldIndicator =
		 * FieldDecorationRegistry
		 * .getDefault().getFieldDecoration(FieldDecorationRegistry
		 * .DEC_CONTENT_PROPOSAL); dec.setImage(infoFieldIndicator.getImage());
		 * dec.setDescriptionText("Press UP ARROW key to get the History!");
		 */
		textAdapter = new TextContentAdapter();
		textAdapter2 = new TextContentAdapter();

		stroke = KeyStroke.getInstance(SWT.ARROW_UP);
		strokeCompletion = KeyStroke.getInstance(SWT.ARROW_RIGHT);

		adapter = new ContentProposalAdapter(text, textAdapter, prov, stroke, null);
		// adapter2 = new ContentProposalAdapter(text, textAdapter2,
		// provCompletion, strokeCompletion, null);

		/*
		 * adapter2.addContentProposalListener(new IContentProposalListener2() {
		 * 
		 * public void proposalPopupClosed(ContentProposalAdapter adapter) {
		 * 
		 * }
		 * 
		 * public void proposalPopupOpened(ContentProposalAdapter adapter) {
		 * 
		 * } });
		 */

		// adapter.setProposalAcceptanceStyle
		// (ContentProposalAdapter.PROPOSAL_REPLACE );

		stroke2 = KeyStroke.getInstance(SWT.F3);

		Button button = new Button(parent, SWT.F2);
		final FormData fd_button = new FormData();
		fd_button.left = new FormAttachment(0, 5);
		fd_button.bottom = new FormAttachment(0, 56);
		fd_button.top = new FormAttachment(0, 31);
		button.setLayoutData(fd_button);
		button.setText("Evaluate Expression");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				evaluate();

			}

		});

		final ExpandBar expandBar = new ExpandBar(parent, SWT.NONE | SWT.V_SCROLL);
		expandBar.addExpandListener(new ExpandAdapter() {
			public void itemExpanded(final ExpandEvent e) {
				ExpandItem ex = (ExpandItem) e.item;

				if (ex.getText().equals("Objects")) {
					isConsoleExpanded = true;
				}
			}

			public void itemCollapsed(final ExpandEvent e) {
				ExpandItem ex = (ExpandItem) e.item;

				if (ex.getText().equals("Objects")) {
					isConsoleExpanded = false;
				}

			}
		});

		final FormData fd_expandBar = new FormData();
		fd_expandBar.bottom = new FormAttachment(100, -5);
		fd_expandBar.right = new FormAttachment(100, -5);
		fd_expandBar.top = new FormAttachment(0, 62);
		fd_expandBar.left = new FormAttachment(0, 0);
		expandBar.setLayoutData(fd_expandBar);
		expandBar.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

		Composite composite_3 = new Composite(expandBar, SWT.NONE);

		newItemExpandItem_8 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_8.setHeight(288);
		newItemExpandItem_8.setText("Objects");

		newItemExpandItem_8.setExpanded(true);
		final CTabFolder tab = new CTabFolder(expandBar, SWT.NONE);
		tab.setRegion(null);
		tab.setTabHeight(20);
		tab.addSelectionListener(new SelectionListener() {
			public void itemClosed(CTabFolderEvent event) {

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}

			public void widgetSelected(SelectionEvent e) {
				if (newItemExpandItem_8 != null) {
					CTabItem it = (CTabItem) e.item;

					newItemExpandItem_8.setText(it.getText());
				}
			}
		});
		newItemExpandItem_8.setControl(tab);

		final CTabItem objectsTabItem = new CTabItem(tab, SWT.NONE);
		objectsTabItem.setText("Objects");

		tab.setSelection(objectsTabItem);

		composite = new Composite(objectsTabItem.getParent(), SWT.NONE);
		objectsTabItem.setControl(composite);

		objectsButton = new Button(composite, SWT.NONE);
		objectsButton.setToolTipText("Refresh the R workspace");
		objectsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				displayRObjects();

				createAttachedPackageTree();

			}
		});
		objectsButton.setText("Refresh");

		iButton = new Button(composite, SWT.NONE);
		iButton.setImage(ResourceManager.getPluginImage(Bio7Plugin.getDefault(), "icons/help.gif"));
		iButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ToolTip infoTip = new ToolTip(new Shell(), SWT.BALLOON | SWT.ICON_INFORMATION);
				infoTip.setText("Info!");

				infoTip.setMessage("Click = Object properties\n" + "Right-Click = Menu\n" + "Select variable(s) = To show, summarize, plot, transfer and convert data!\n" + "\nPress the \"UP ARROW\" key to get the History in the expression textfield!\n" + "\nIf you plot a PDF please close the Window of an opened reader (old plot)\nto get the new plot!\n"
						+ "\nRight-Click on \"Plot Data\" textfield detour = Menu to inject text commands (a help to customize plots!)");
				infoTip.setVisible(true);

			}
		});

		xButton = new Button(composite, SWT.NONE);
		xButton.setToolTipText("Clear the text field");
		xButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				textConsole.setText("");
			}
		});
		xButton.setText("Clear");

		removeButton = new Button(composite, SWT.NONE);
		removeButton.setToolTipText("Remove selected variables from the workspace");
		removeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				removeSelectedVars();
			}
		});
		removeButton.setText("Remove");

		gcButton = new Button(composite, SWT.FLAT);
		gcButton.setToolTipText("Causes a garbage collection to take place");
		gcButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {

						RServe.printJob("gc()");

					} else {
						Bio7Dialog.message("RServer is busy!");
					}
				}

			}
		});
		gcButton.setText("Gc");

		plotTabItem = new CTabItem(tab, SWT.NONE);
		plotTabItem.setText("Plot Data");

		composite_1 = new Composite(plotTabItem.getParent(), SWT.NONE);
		composite_1.setLayout(new FormLayout());
		plotTabItem.setControl(composite_1);

		plotButton = new Button(composite_1, SWT.NONE);
		fd_plotButton = new FormData();
		plotButton.setLayoutData(fd_plotButton);
		plotButton.setToolTipText("Draws a boxplot from a vector or a dataframe.");
		plotButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {

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
						Bio7Dialog.message("RServer is busy!");
					}
				}
			}
		});
		plotButton.setText("Boxplot");

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
		histButton.setToolTipText("Draws a histogram from a vector");
		histButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
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
						Bio7Dialog.message("RServer is busy!");
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
		pieButton.setToolTipText("Draws a piechart from a vector");
		pieButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
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
						Bio7Dialog.message("RServer is busy!");
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
		xyButton.setToolTipText("Executes the general plot command.\nOne or two(x,y) arguments can be plotted.");
		xyButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {

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

									RServe.printJob("plot(" + selections[0] + "," + selections[1] + ",main=\"" + titleText.getText() + "\",xlab=\"" + xText.getText() + "\",ylab=\"" + yText.getText() + "\")");
								} else {
									text_1.setText("plot(" + selections[0] + "," + selections[1] + ",main=\"" + titleText.getText() + "\",xlab=\"" + xText.getText() + "\",ylab=\"" + yText.getText() + "\")");
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
						Bio7Dialog.message("RServer is busy!");
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
						RScript.rscriptjob(text_1.getText(), null);

					}
				} else {
					Bio7Dialog.message("RServer is busy!");
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
						Bio7Dialog.message("RServer is busy!");
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
						Bio7Dialog.message("RServer is busy!");
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

								RServe.printJob("plot(tri.vm,main=\"" + titleText.getText() + "\",xlab=\"" + xText.getText() + "\",ylab=\"" + yText.getText() + "\");" + "text(" + selections[0] + "," + selections[1] + ", tri.vm.areas, cex=0.5);" + "points(" + selections[0] + "," + selections[1] + ",cex=0.5)");
							} else {
								text_1.setText("plot(tri.vm,main=\"" + titleText.getText() + "\",xlab=\"" + xText.getText() + "\",ylab=\"" + yText.getText() + "\");" + "text(" + selections[0] + "," + selections[1] + ", tri.vm.areas, cex=0.5);" + "points(" + selections[0] + "," + selections[1] + ",cex=0.5)");
							}

						} else if (selections.length <= 1) {
							Bio7Dialog.message("Voronoi plot needs two arguments (x,y)!");
						}
					} else {
						Bio7Dialog.message("RServer is busy!");
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
						Bio7Dialog.message("RServer is busy!");
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

		CTabItem packagesTabItem = new CTabItem(tab, SWT.NONE);
		packagesTabItem.setText("Packages");

		Composite composite_2 = new Composite(tab, SWT.NONE);
		packagesTabItem.setControl(composite_2);
		composite_2.setLayout(new FillLayout(SWT.HORIZONTAL));

		tree = new Tree(composite_2, SWT.BORDER);
		final Menu menuList = new Menu(tree);
		tree.setMenu(menuList);
		menuList.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				// Get rid of existing menu items
				MenuItem[] items = menuList.getItems();
				for (int i = 0; i < items.length; i++) {
					((MenuItem) items[i]).dispose();
				}
				MenuItem refreshItem = new MenuItem(menuList, SWT.NONE);

				refreshItem.setText("Refresh");
				refreshItem.addSelectionListener(new SelectionListener() {

					public void widgetSelected(SelectionEvent e) {
						if (RServe.isAliveDialog()) {
							if (RState.isBusy() == false) {

								createAttachedPackageTree();

							} else {
								Bio7Dialog.message("RServer is busy!");
							}
						}

					}

					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});
				// Add menu items for current selection
				MenuItem detachItem = new MenuItem(menuList, SWT.NONE);

				detachItem.setText("Detach");
				detachItem.addSelectionListener(new SelectionListener() {

					public void widgetSelected(SelectionEvent e) {
						if (RServe.isAliveDialog()) {
							if (RState.isBusy() == false) {

								RConnection c = RServe.getConnection();
								String selectedPackage = tree.getSelection()[0].getText();
								try {
									c.eval("try(detach(package:" + selectedPackage + "))");
									createAttachedPackageTree();
								} catch (RserveException e1) {

									e1.printStackTrace();
								}

							} else {
								Bio7Dialog.message("RServer is busy!");
							}
						}

					}

					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});

				/*
				 * MenuItem updateItem = new MenuItem(menuList, SWT.NONE);
				 * //update.packages() updateItem.setText("Update All");
				 * updateItem.addSelectionListener(new SelectionListener() {
				 * 
				 * public void widgetSelected(SelectionEvent e) { if
				 * (RServe.isAliveDialog()) { if (RState.isBusy() == false) {
				 * IPreferenceStore store =
				 * Bio7Plugin.getDefault().getPreferenceStore(); String destdir
				 * = store.getString("InstallLocation"); String server =
				 * store.getString(PreferenceConstants.PACKAGE_R_SERVER);
				 * 
				 * RConnection c = RServe.getConnection(); try {
				 * c.eval("try(update.packages(repos =\"" + server +
				 * "\",ask=FALSE));"); } catch (RserveException e1) {
				 * 
				 * e1.printStackTrace(); }
				 * 
				 * } else { Bio7Dialog.message("RServer is busy!"); } }
				 * 
				 * }
				 * 
				 * public void widgetDefaultSelected(SelectionEvent e) {
				 * 
				 * } });
				 */
				/*
				 * MenuItem removeItem = new MenuItem(menuList, SWT.NONE);
				 * //update.packages() removeItem.setText("Remove");
				 * removeItem.addSelectionListener(new SelectionListener() {
				 * 
				 * public void widgetSelected(SelectionEvent e) { String
				 * selectedPackage=tree.getSelection()[0].getText(); if
				 * (RServe.isAliveDialog()) { if (RState.isBusy() == false) {
				 * 
				 * 
				 * RConnection c = RServe.getConnection(); try {
				 * c.eval("try(remove.packages(\"" + selectedPackage + "\"))");
				 * } catch (RserveException ex) {
				 * 
				 * ex.printStackTrace(); }
				 * System.out.println("Removed library "+selectedPackage);
				 * 
				 * } else { Bio7Dialog.message("RServer is busy!"); } }
				 * 
				 * }
				 * 
				 * public void widgetDefaultSelected(SelectionEvent e) {
				 * 
				 * } });
				 */

			}
		});

		tree.addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				Point point = new Point(event.x, event.y);
				TreeItem item = tree.getItem(point);
				if (item != null) {

					// System.out.println("Mouse down: " + item.getText());
					// item.setImage(new Image(Display.getCurrent(),
					// getClass().getResourceAsStream("/pics/regelmaessig.gif")));

				}
			}
		});

		final CTabItem variablesTabItem = new CTabItem(tab, SWT.NONE);
		variablesTabItem.setText("Variables");

		list = new List(variablesTabItem.getParent(), SWT.BORDER | SWT.V_SCROLL);
		list.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent e) {

				if (e.button == 3) {

					setInDocument(list);

					String[] items = list.getSelection();

					String t = text.getText();
					int pos = text.getCaretPosition();
					String res = t.substring(pos);
					String res2 = t.substring(0, pos);
					String fin = res2 + items[0] + res;

					text.setText(fin);
				} else {
					String[] items = list.getSelection();
					text.setText(items[0]);
				}
			}

			public void mouseDown(final MouseEvent e) {

				int index = list.getSelectionIndex();
				/* For MacOSX we proof if the result is >0! */
				if (index >= 0) {
					list.setToolTipText(RFunctions.getPropsHistInstance().variablesContext[index]);
				}
			}

		});

		list.setItems(RFunctions.getPropsHistInstance().variables);
		variablesTabItem.setControl(list);

		final CTabItem createextractDataTabItem = new CTabItem(tab, SWT.NONE);
		createextractDataTabItem.setText("Data");

		list_1 = new List(createextractDataTabItem.getParent(), SWT.BORDER | SWT.V_SCROLL);
		list_1.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent e) {
				if (e.button == 3) {
					setInDocument(list_1);
				} else {
					String[] items = list_1.getSelection();
					text.setText(items[0]);
				}
			}

			public void mouseDown(final MouseEvent e) {

				int index = list_1.getSelectionIndex();
				/* For MacOSX we proof if the result is >0! */
				if (index >= 0) {
					list_1.setToolTipText(RFunctions.getPropsHistInstance().dataContext[index]);
				}
			}
		});
		list_1.setItems(RFunctions.getPropsHistInstance().data);
		createextractDataTabItem.setControl(list_1);

		final CTabItem mathTabItem = new CTabItem(tab, SWT.NONE);
		mathTabItem.setText("Math");

		list_3 = new List(mathTabItem.getParent(), SWT.BORDER | SWT.V_SCROLL);
		list_3.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent e) {
				if (e.button == 3) {
					setInDocument(list_3);
				} else {
					String[] items = list_3.getSelection();
					text.setText(items[0]);
				}
			}

			public void mouseDown(final MouseEvent e) {

				int index = list_3.getSelectionIndex();
				/* For MacOSX we proof if the result is >0! */
				if (index >= 0) {
					list_3.setToolTipText(RFunctions.getPropsHistInstance().mathContext[index]);
				}

			}
		});
		list_3.setItems(RFunctions.getPropsHistInstance().math);
		mathTabItem.setControl(list_3);

		final CTabItem statisticsTabItem = new CTabItem(tab, SWT.NONE);
		statisticsTabItem.setText("Statistics");

		list_4 = new List(statisticsTabItem.getParent(), SWT.BORDER | SWT.V_SCROLL);
		list_4.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent e) {
				if (e.button == 3) {
					setInDocument(list_4);
				} else {
					String[] items = list_4.getSelection();
					text.setText(items[0]);
				}
			}

			public void mouseDown(final MouseEvent e) {

				int index = list_4.getSelectionIndex();
				/* For MacOSX we proof if the result is >0! */
				if (index >= 0) {
					list_4.setToolTipText(RFunctions.getPropsHistInstance().statisticsContext[index]);
				}
			}
		});
		list_4.setItems(RFunctions.getPropsHistInstance().statistics);
		statisticsTabItem.setControl(list_4);

		final CTabItem dataConversionTabItem = new CTabItem(tab, SWT.NONE);
		dataConversionTabItem.setText("Data Conversion");

		list_2 = new List(dataConversionTabItem.getParent(), SWT.BORDER | SWT.V_SCROLL);
		list_2.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent e) {
				if (e.button == 3) {
					setInDocument(list_2);
				} else {
					String[] items = list_2.getSelection();
					text.setText(items[0]);
				}
			}

			public void mouseDown(final MouseEvent e) {

				int index = list_2.getSelectionIndex();
				/* For MacOSX we proof if the result is >0! */
				if (index >= 0) {
					list_2.setToolTipText(RFunctions.getPropsHistInstance().dataConversionContext[index]);
				}
			}
		});
		list_2.setItems(RFunctions.getPropsHistInstance().dataConversion);
		dataConversionTabItem.setControl(list_2);

		final CTabItem imageTabItem = new CTabItem(tab, SWT.NONE);
		imageTabItem.setText("Image");

		list_7 = new List(imageTabItem.getParent(), SWT.V_SCROLL | SWT.BORDER);
		list_7.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent e) {
				if (e.button == 3) {
					setInDocument(list_7);
				} else {
					String[] items = list_7.getSelection();
					text.setText(items[0]);
				}

			}

			public void mouseDown(final MouseEvent e) {

				int index = list_7.getSelectionIndex();
				/* For MacOSX we proof if the result is >0! */
				if (index >= 0) {
					list_7.setToolTipText(RFunctions.getPropsHistInstance().imageAnalysisContext[index]);
				}
			}
		});

		list_7.setItems(RFunctions.getPropsHistInstance().imageAnalysis);
		imageTabItem.setControl(list_7);

		final CTabItem matixTabItem = new CTabItem(tab, SWT.NONE);
		matixTabItem.setText("Matrix");

		list_6 = new List(matixTabItem.getParent(), SWT.BORDER | SWT.V_SCROLL);
		list_6.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent e) {
				if (e.button == 3) {
					setInDocument(list_6);
				} else {
					String[] items = list_6.getSelection();
					text.setText(items[0]);
				}

			}

			public void mouseDown(final MouseEvent e) {

				int index = list_6.getSelectionIndex();
				/* For MacOSX we proof if the result is >0! */
				if (index >= 0) {
					list_6.setToolTipText(RFunctions.getPropsHistInstance().matrixContext[index]);
				}
			}
		});
		list_6.setItems(RFunctions.getPropsHistInstance().matrix);
		matixTabItem.setControl(list_6);

		final CTabItem spatialStatisticsTabItem = new CTabItem(tab, SWT.NONE);
		spatialStatisticsTabItem.setText("Spatial Statistics");

		list_5 = new List(spatialStatisticsTabItem.getParent(), SWT.BORDER | SWT.V_SCROLL);
		list_5.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent e) {
				if (e.button == 3) {
					setInDocument(list_5);
				} else {
					String[] items = list_5.getSelection();
					text.setText(items[0]);
				}
			}

			public void mouseDown(final MouseEvent e) {

				int index = list_5.getSelectionIndex();
				/* For MacOSX we proof if the result is >0! */
				if (index >= 0) {
					list_5.setToolTipText(RFunctions.getPropsHistInstance().spatialStatsContext[index]);
				}

			}
		});
		list_5.setItems(RFunctions.getPropsHistInstance().spatialStats);
		spatialStatisticsTabItem.setControl(list_5);

		Button loadButton;
		loadButton = new Button(parent, SWT.NONE);
		fd_button.right = new FormAttachment(loadButton);
		loadButton.setToolTipText("Load history");
		loadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				loadScript();
			}
		});
		final FormData fd_loadButton = new FormData();
		fd_loadButton.right = new FormAttachment(0, 208);
		fd_loadButton.top = new FormAttachment(text, 1);
		fd_loadButton.bottom = new FormAttachment(button, 0, SWT.BOTTOM);
		fd_loadButton.left = new FormAttachment(0, 156);
		loadButton.setLayoutData(fd_loadButton);
		loadButton.setText("Load");

		Button saveButton;
		saveButton = new Button(parent, SWT.NONE);
		saveButton.setToolTipText("Save history");
		saveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				saveScript();
			}
		});
		final FormData fd_saveButton = new FormData();
		fd_saveButton.right = new FormAttachment(0, 254);
		fd_saveButton.top = new FormAttachment(text, 1);
		fd_saveButton.bottom = new FormAttachment(button, 0, SWT.BOTTOM);
		fd_saveButton.left = new FormAttachment(0, 208);
		saveButton.setLayoutData(fd_saveButton);
		saveButton.setText("Save");

		Transfer[] types = new Transfer[] { TextTransfer.getInstance() };
		int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_LINK;

		DropTarget target = new DropTarget(text, operations);
		target.setTransfer(types);
		target.addDropListener(new DropTargetAdapter() {
			public void drop(DropTargetEvent event) {
				if (event.data == null) {
					event.detail = DND.DROP_NONE;
					return;
				}

				text.setText((String) event.data);
			}
		});

		SashForm sashForm;
		sashForm = new SashForm(composite, SWT.NONE);

		SashForm sashForm_1 = new SashForm(sashForm, SWT.VERTICAL);

		listShell = new List(sashForm_1, SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER);
		listShell.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				// System.out.println(e.keyCode);
				StringBuffer sb = new StringBuffer();
				if (e.keyCode == 99) {// 'c'
					addChars(sb, "+");
				} else if (e.keyCode == 97) {// 'a'
					addChars(sb, ",");
				}

			}

			private void addChars(StringBuffer sb, String c) {
				String[] sels = listShell.getSelection();
				for (int i = 0; i < sels.length; i++) {

					if (i < (sels.length - 1)) {
						sb.append(sels[i] + c);
					} else {
						sb.append(sels[i]);
					}

				}

				String t = text.getText();
				String a = t.substring(0, text.getCaretPosition());
				String b = t.substring(text.getCaretPosition(), t.length());
				text.setText(a + sb.toString() + b);
				sb = null;
			}
		});
		listShell.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent e) {
				/*
				 * if (RState.isBusy() == false) { boolean[] bolExists = null;
				 * String selected = list_8.getItem(list_8.getSelectionIndex());
				 * 
				 * if (e.button != 3) { if (RServe.isAliveDialog()) {
				 * RConnection c = RServe.getConnection();
				 * 
				 * try { REXPLogical bolDataExists = (REXPLogical)
				 * c.eval("exists(\"" + selected + "\")"); bolExists =
				 * bolDataExists.isTrue(); } catch (RserveException e2) {
				 * 
				 * e2.printStackTrace(); }
				 * 
				 * if (bolExists[0]) {
				 * 
				 * RServe.printJob("" + selected + ""); } else {
				 * Bio7Dialog.message("Data not existent in\n" +
				 * "the current workspace.\n" + "Please refresh the list!"); }
				 * 
				 * }
				 * 
				 * } else {
				 * 
				 * if (RServe.isAliveDialog()) { RConnection c =
				 * RServe.getConnection();
				 * 
				 * try { REXPLogical bolDataExists = (REXPLogical)
				 * c.eval("exists(\"" + selected + "\")"); bolExists =
				 * bolDataExists.isTrue(); } catch (RserveException e2) {
				 * 
				 * e2.printStackTrace(); }
				 * 
				 * if (bolExists[0]) {
				 * 
				 * RServe.printJob("summary(" + selected + ")"); } else {
				 * Bio7Dialog.message("Data not existent in\n" +
				 * "the current workspace.\n" + "Please refresh the list!"); }
				 * 
				 * }
				 * 
				 * } } else { Bio7Dialog.message("RServer is busy!"); }
				 */
			}
		});
		listShell.addSelectionListener(new SelectionAdapter() {
			private boolean[] bolExists;

			public void widgetSelected(final SelectionEvent e) {
				if (RState.isBusy() == false) {
					/*This avoids a deadlock under MacOSX!*/
					int in = listShell.getSelectionIndex();
					if (in >= 0) {
						String selected = listShell.getItem(listShell.getSelectionIndex());
						RConnection c = RServe.getConnection();
						if (RServe.isAliveDialog()) {

							try {
								REXPLogical bolDataExists = (REXPLogical) c.eval("try(exists(\"" + selected + "\"))");
								bolExists = bolDataExists.isTRUE();
							} catch (RserveException e2) {

								e2.printStackTrace();
							}

							if (bolExists[0]) {

								String type = null;
								String cl = null;
								String[] size = null;
								String sizeList = null;
								StringBuffer buf = new StringBuffer();
								boolean[] bolRaw = null;
								buf.append("(");
								try {

									cl = c.eval("class(" + selected + ")").asString();
									type = c.eval("typeof(" + selected + ")").asString();

									REXPLogical bolRawExists = (REXPLogical) c.eval("is.null(dim(" + selected + "))");
									bolRaw = bolRawExists.isTRUE();
									if (bolRaw[0]) {
										sizeList = c.eval("length(" + selected + ")").asString();
									} else {

										size = c.eval("dim(" + selected + ")").asStrings();
										for (int i = 0; i < size.length; i++) {
											if (i < size.length - 1) {
												buf.append(size[i] + ":");
											} else {
												buf.append(size[i]);
											}
										}
									}
									buf.append(")");
									// "{length("+selected+")}else {dim("+selected+")}").asStrings();
								} catch (REXPMismatchException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								} catch (RserveException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								if (bolRaw[0]) {
									textConsole.setText("Object description:\n\n" + "type    = " + type + "\n" + "class   = " + cl + "\n" + "size     = " + sizeList);
								} else {
									textConsole.setText("Object description:\n\n" + "type    = " + type + "\n" + "class   = " + cl + "\n" + "dim     = " + buf.toString());

								}
								buf = null;
							} else {
								Bio7Dialog.message("Data not existent in\n" + "the current workspace.\n" + "Please refresh the list!");
							}
						}
					}
				}
						
					 else {
						Bio7Dialog.message("RServer is busy!");
					
				}
			}
		});
		final DragSource source = new DragSource(listShell, operations);
		source.setTransfer(types);
		source.addDragListener(new DragSourceListener() {
			public void dragStart(DragSourceEvent event) {
				if (listShell.getItemCount() > 0) {
					event.doit = (listShell.getItem(listShell.getSelectionIndex()).length() != 0);
				}
			}

			public void dragSetData(DragSourceEvent event) {
				if (listShell.getItemCount() > 0) {
					event.data = listShell.getItem(listShell.getSelectionIndex());
				} else {
					event.data = " ";
				}
			}

			public void dragFinished(DragSourceEvent event) {
				if (event.detail == DND.DROP_MOVE) {

				}

			}
		});

		final Menu menu = new Menu(listShell);
		listShell.setMenu(menu);

		final MenuItem newItemMenuItem_20 = new MenuItem(menu, SWT.NONE);
		newItemMenuItem_20.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RState.isBusy() == false) {
					boolean[] bolExists = null;
					int selIndex = listShell.getSelectionIndex();
					if (selIndex != -1) {
						String selected = listShell.getItem(selIndex);

						if (RServe.isAliveDialog()) {
							RConnection c = RServe.getConnection();

							try {
								REXPLogical bolDataExists = (REXPLogical) c.eval("exists(\"" + selected + "\")");
								bolExists = bolDataExists.isTRUE();
							} catch (RserveException e2) {

								e2.printStackTrace();
							}

							if (bolExists[0]) {

								RServe.printJob("" + selected + "");
							} else {
								Bio7Dialog.message("Data not existent in\n" + "the current workspace.\n" + "Please refresh the list!");
							}
						}
					}

				} else {
					Bio7Dialog.message("RServer is busy!");
				}

			}
		});
		newItemMenuItem_20.setText("Show");

		final MenuItem newItemMenuItem = new MenuItem(menu, SWT.NONE);
		newItemMenuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					RConnection c = RServe.getConnection();

					if (RState.isBusy() == false) {
						boolean[] bolExists = null;
						int selIndex = listShell.getSelectionIndex();
						if (selIndex != -1) {
							String selected = listShell.getItem(selIndex);
							try {
								REXPLogical bolDataExists = (REXPLogical) c.eval("exists(\"" + selected + "\")");
								bolExists = bolDataExists.isTRUE();
							} catch (RserveException e2) {

								e2.printStackTrace();
							}

							if (bolExists[0]) {

								RServe.printJob("summary(" + selected + ")");
							} else {
								Bio7Dialog.message("Data not existent in\n" + "the current workspace.\n" + "Please refresh the list!");
							}
						}
					}
				} else {
					Bio7Dialog.message("RServer is busy!");

				}

			}
		});
		newItemMenuItem.setText("Summary");

		final MenuItem newItemMenuItem_8 = new MenuItem(menu, SWT.CASCADE);
		newItemMenuItem_8.setText("Filter");
		final Menu menu_81 = new Menu(newItemMenuItem_8);
		newItemMenuItem_8.setMenu(menu_81);

		final MenuItem newItemMenuItem_11 = new MenuItem(menu_81, SWT.NONE);
		newItemMenuItem_11.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				REXP x = null;
				listShell.removeAll();

				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						String[] v = null;
						// List all variables in the R workspace!

						try {
							RServe.getConnection().eval("varWorkspaceType<-NULL;for(i in 1:length(ls())){if(is.matrix(get(ls()[i]))==TRUE){varWorkspaceType<-append(varWorkspaceType,ls()[i])}}");
							x = RServe.getConnection().eval("varWorkspaceType");
							if (x.isNull() == false) {
								try {
									v = x.asStrings();
								} catch (REXPMismatchException e1) {

									e1.printStackTrace();
								}
							}
							RServe.getConnection().eval("remove(varWorkspaceType)");
						} catch (RserveException e1) {

							e1.printStackTrace();
						}
						if (v != null) {
							for (int i = 0; i < v.length; i++) {

								listShell.add(v[i]);

							}
						}
					} else {
						Bio7Dialog.message("RServer is busy!");

					}

				}

			}
		});
		newItemMenuItem_11.setText("Matrices");

		final MenuItem newItemMenuItem_10 = new MenuItem(menu_81, SWT.NONE);
		newItemMenuItem_10.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				REXP x = null;
				listShell.removeAll();

				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						String[] v = null;
						// List all variables in the R workspace!

						try {
							RServe.getConnection().eval("varWorkspaceType<-NULL;for(i in 1:length(ls())){if(is.vector(get(ls()[i]))==TRUE){varWorkspaceType<-append(varWorkspaceType,ls()[i])}}");
							x = RServe.getConnection().eval("varWorkspaceType");
							if (x.isNull() == false) {
								try {
									v = x.asStrings();
								} catch (REXPMismatchException e1) {

									e1.printStackTrace();
								}
							}
							RServe.getConnection().eval("remove(varWorkspaceType)");
						} catch (RserveException e1) {

							e1.printStackTrace();
						}
						if (v != null) {
							for (int i = 0; i < v.length; i++) {
								if (v[i].equals("varWorkspaceType") == false) {
									listShell.add(v[i]);
								}

							}
						}
					} else {
						Bio7Dialog.message("RServer is busy!");

					}

				}

			}
		});
		newItemMenuItem_10.setText("Vectors");

		final MenuItem newItemMenuItem_9 = new MenuItem(menu_81, SWT.NONE);
		newItemMenuItem_9.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				REXP x = null;
				listShell.removeAll();

				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						String[] v = null;
						// List all variables in the R workspace!

						try {
							RServe.getConnection().eval("varWorkspaceType<-NULL;for(i in 1:length(ls())){if(is.function(get(ls()[i]))==TRUE){varWorkspaceType<-append(varWorkspaceType,ls()[i])}}");
							x = RServe.getConnection().eval("varWorkspaceType");
							if (x.isNull() == false) {
								try {
									v = x.asStrings();
								} catch (REXPMismatchException e1) {

									e1.printStackTrace();
								}
							}
							RServe.getConnection().eval("remove(varWorkspaceType)");
						} catch (RserveException e1) {

							e1.printStackTrace();
						}
						if (v != null) {
							for (int i = 0; i < v.length; i++) {

								listShell.add(v[i]);

							}
						}
					} else {
						Bio7Dialog.message("RServer is busy!");

					}

				}

			}
		});
		newItemMenuItem_9.setText("Functions");

		final MenuItem newItemMenuItem_12 = new MenuItem(menu_81, SWT.NONE);
		newItemMenuItem_12.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				REXP x = null;
				listShell.removeAll();

				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						String[] v = null;
						// List all variables in the R workspace!

						try {
							RServe.getConnection().eval("varWorkspaceType<-NULL;for(i in 1:length(ls())){if(is.factor(get(ls()[i]))==TRUE){varWorkspaceType<-append(varWorkspaceType,ls()[i])}}");
							x = RServe.getConnection().eval("varWorkspaceType");
							if (x.isNull() == false) {
								try {
									v = x.asStrings();
								} catch (REXPMismatchException e1) {

									e1.printStackTrace();
								}
							}
							RServe.getConnection().eval("remove(varWorkspaceType)");
						} catch (RserveException e1) {

							e1.printStackTrace();
						}
						if (v != null) {
							for (int i = 0; i < v.length; i++) {

								listShell.add(v[i]);

							}
						}
					} else {
						Bio7Dialog.message("RServer is busy!");

					}

				}

			}
		});
		newItemMenuItem_12.setText("Factor");

		final MenuItem newItemMenuItem_4 = new MenuItem(menu_81, SWT.NONE);
		newItemMenuItem_4.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				REXP x = null;
				listShell.removeAll();

				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						String[] v = null;
						// List all variables in the R workspace!

						try {
							RServe.getConnection().eval("varWorkspaceType<-NULL;for(i in 1:length(ls())){if(is.data.frame(get(ls()[i]))==TRUE){varWorkspaceType<-append(varWorkspaceType,ls()[i])}}");
							x = RServe.getConnection().eval("varWorkspaceType");
							if (x.isNull() == false) {
								try {
									v = x.asStrings();
								} catch (REXPMismatchException e1) {

									e1.printStackTrace();
								}
							}
							RServe.getConnection().eval("remove(varWorkspaceType)");
						} catch (RserveException e1) {

							e1.printStackTrace();
						}
						if (v != null) {
							for (int i = 0; i < v.length; i++) {

								listShell.add(v[i]);

							}
						}
					} else {
						Bio7Dialog.message("RServer is busy!");

					}

				}

			}
		});
		newItemMenuItem_4.setText("Dataframes");

		final MenuItem newSubmenuMenuItem = new MenuItem(menu, SWT.CASCADE);
		newSubmenuMenuItem.setText("Convert");

		final Menu menu_1 = new Menu(newSubmenuMenuItem);
		newSubmenuMenuItem.setMenu(menu_1);

		final MenuItem newItemMenuItem_1 = new MenuItem(menu_1, SWT.NONE);
		newItemMenuItem_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					RConnection c = RServe.getConnection();

					if (RState.isBusy() == false) {
						boolean[] bolExists = null;

						String[] selected = listShell.getSelection();
						if (selected.length == 1) {

							try {
								REXPLogical bolDataExists = (REXPLogical) c.eval("exists(\"" + selected[0] + "\")");
								bolExists = bolDataExists.isTRUE();
							} catch (RserveException e2) {

								e2.printStackTrace();
							}

							if (bolExists[0]) {

								InputDialog inp = new InputDialog(new Shell(), "To dataframe", "Create a name for the dataframe!", selected[0], null);

								if (inp.open() == Dialog.OK) {
									String input = inp.getValue();
									RServe.printJob("" + input + "<-as.data.frame(" + selected[0] + ")");
								}

							} else {
								Bio7Dialog.message("Data not existent in\n" + "the current workspace.\n" + "Please refresh the list!");
							}
						} else if (selected.length > 1) {
							try {
								REXPLogical bolDataExists = (REXPLogical) c.eval("exists(\"" + selected[0] + "\")");
								bolExists = bolDataExists.isTRUE();
							} catch (RserveException e2) {

								e2.printStackTrace();
							}

							if (bolExists[0]) {

								InputDialog inp = new InputDialog(new Shell(), "To dataframe", "Create a name for the dataframe!", selected[0], null);

								if (inp.open() == Dialog.OK) {
									String input = inp.getValue();
									try {
										c.eval("" + input + "<-data.frame(" + selected[0] + ")");
									} catch (RserveException e1) {

										e1.printStackTrace();
									}
									for (int i = 1; i < selected.length; i++) {
										try {
											c.eval("" + input + "<-cbind(" + input + "," + selected[i] + ")");
										} catch (RserveException e1) {

											e1.printStackTrace();
										}
									}
									RServe.printJob("" + input + "");
								}

							} else {
								Bio7Dialog.message("Data not existent in\n" + "the current workspace.\n" + "Please refresh the list!");
							}

						}
					} else {
						Bio7Dialog.message("RServer is busy!");

					}
				}
			}
		});
		newItemMenuItem_1.setText("To Dataframe");

		final MenuItem newItemMenuItem_2 = new MenuItem(menu_1, SWT.NONE);
		newItemMenuItem_2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					RConnection c = RServe.getConnection();

					if (RState.isBusy() == false) {
						boolean[] bolExists = null;

						String[] selected = listShell.getSelection();
						if (selected.length == 1) {

							try {
								REXPLogical bolDataExists = (REXPLogical) c.eval("exists(\"" + selected[0] + "\")");
								bolExists = bolDataExists.isTRUE();
							} catch (RserveException e2) {

								e2.printStackTrace();
							}

							if (bolExists[0]) {

								InputDialog inp = new InputDialog(new Shell(), "To Matrix", "Create a name for the matrix!", selected[0], null);

								if (inp.open() == Dialog.OK) {
									String input = inp.getValue();
									RServe.printJob("" + input + "<-as.matrix(" + selected[0] + ")");
								}

							} else {
								Bio7Dialog.message("Data not existent in\n" + "the current workspace.\n" + "Please refresh the list!");
							}
						} else if (selected.length > 1) {
							try {
								REXPLogical bolDataExists = (REXPLogical) c.eval("exists(\"" + selected[0] + "\")");
								bolExists = bolDataExists.isTRUE();
							} catch (RserveException e2) {

								e2.printStackTrace();
							}

							if (bolExists[0]) {

								InputDialog inp = new InputDialog(new Shell(), "To Matrix", "Create a name for the matrix!", selected[0], null);

								if (inp.open() == Dialog.OK) {
									String input = inp.getValue();
									try {
										c.eval("" + input + "<-NULL");
										c.eval("" + input + "<-cbind(" + input + "," + selected[0] + ")");
									} catch (RserveException e1) {

										e1.printStackTrace();
									}
									for (int i = 1; i < selected.length; i++) {
										try {
											c.eval("" + input + "<-cbind(" + input + "," + selected[i] + ")");
										} catch (RserveException e1) {

											e1.printStackTrace();
										}
									}
									RServe.printJob("" + input + "");
								}

							} else {
								Bio7Dialog.message("Data not existent in\n" + "the current workspace.\n" + "Please refresh the list!");
							}

						}
					} else {
						Bio7Dialog.message("RServer is busy!");

					}
				}

			}
		});
		newItemMenuItem_2.setText("To Matrix");

		final MenuItem newItemMenuItem_3 = new MenuItem(menu_1, SWT.NONE);
		newItemMenuItem_3.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					RConnection c = RServe.getConnection();

					if (RState.isBusy() == false) {
						boolean[] bolExists = null;

						String[] selected = listShell.getSelection();
						if (selected.length == 1) {

							try {
								REXPLogical bolDataExists = (REXPLogical) c.eval("exists(\"" + selected[0] + "\")");
								bolExists = bolDataExists.isTRUE();
							} catch (RserveException e2) {

								e2.printStackTrace();
							}

							if (bolExists[0]) {

								InputDialog inp = new InputDialog(new Shell(), "To Vector", "Create a name for the vector!", selected[0], null);

								if (inp.open() == Dialog.OK) {
									String input = inp.getValue();
									RServe.printJob("" + input + "<-as.vector(" + selected[0] + ")");
								}

							} else {
								Bio7Dialog.message("Data not existent in\n" + "the current workspace.\n" + "Please refresh the list!");
							}
						} else {
							Bio7Dialog.message("Please select only one Variable!");
						}
					} else {
						Bio7Dialog.message("RServer is busy!");

					}
				}
			}
		});
		newItemMenuItem_3.setText("To Vector");

		final MenuItem newItemMenuItem_19 = new MenuItem(menu_1, SWT.NONE);
		newItemMenuItem_19.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					RConnection c = RServe.getConnection();

					if (RState.isBusy() == false) {
						boolean[] bolExists = null;

						String[] selected = listShell.getSelection();
						if (selected.length == 1) {

							try {
								REXPLogical bolDataExists = (REXPLogical) c.eval("exists(\"" + selected[0] + "\")");
								bolExists = bolDataExists.isTRUE();
							} catch (RserveException e2) {

								e2.printStackTrace();
							}

							if (bolExists[0]) {

								InputDialog inp = new InputDialog(new Shell(), "To Vector", "Create a name for the vector!", selected[0], null);

								if (inp.open() == Dialog.OK) {
									String input = inp.getValue();
									RServe.printJob("" + input + "<-as.numeric(" + selected[0] + ")");
								}

							} else {
								Bio7Dialog.message("Data not existent in\n" + "the current workspace.\n" + "Please refresh the list!");
							}
						} else {

						}
					} else {
						Bio7Dialog.message("RServer is busy!");

					}
				}

			}
		});
		newItemMenuItem_19.setText("To Numeric");

		final MenuItem menuItem = new MenuItem(menu_1, SWT.NONE);
		menuItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					RConnection c = RServe.getConnection();

					if (RState.isBusy() == false) {
						boolean[] bolExists = null;

						String[] selected = listShell.getSelection();
						if (selected.length == 1) {

							try {
								REXPLogical bolDataExists = (REXPLogical) c.eval("exists(\"" + selected[0] + "\")");
								bolExists = bolDataExists.isTRUE();
							} catch (RserveException e2) {

								e2.printStackTrace();
							}

							if (bolExists[0]) {

								InputDialog inp = new InputDialog(new Shell(), "To Factor", "Create a name for the factor!", selected[0], null);

								if (inp.open() == Dialog.OK) {
									String input = inp.getValue();
									RServe.printJob("" + input + "<-as.factor(" + selected[0] + ")");
								}

							} else {
								Bio7Dialog.message("Data not existent in\n" + "the current workspace.\n" + "Please refresh the list!");
							}
						} else {

						}
					} else {
						Bio7Dialog.message("RServer is busy!");

					}
				}

			}
		});
		menuItem.setText("To Factor");

		final MenuItem newItemMenuItem_13 = new MenuItem(menu_1, SWT.NONE);
		newItemMenuItem_13.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					RConnection c = RServe.getConnection();

					if (RState.isBusy() == false) {
						boolean[] bolExists = null;

						String[] selected = listShell.getSelection();
						if (selected.length == 1) {

							try {
								REXPLogical bolDataExists = (REXPLogical) c.eval("exists(\"" + selected[0] + "\")");
								bolExists = bolDataExists.isTRUE();
							} catch (RserveException e2) {

								e2.printStackTrace();
							}

							if (bolExists[0]) {

								InputDialog inp = new InputDialog(new Shell(), "To Character", "Create a name for the character!", selected[0], null);

								if (inp.open() == Dialog.OK) {
									String input = inp.getValue();
									RServe.printJob("" + input + "<-as.character(" + selected[0] + ")");
								}

							} else {
								Bio7Dialog.message("Data not existent in\n" + "the current workspace.\n" + "Please refresh the list!");
							}
						} else {

						}
					} else {
						Bio7Dialog.message("RServer is busy!");

					}
				}

			}
		});
		newItemMenuItem_13.setText("To Character");

		final MenuItem newSubmenuMenuItem_1 = new MenuItem(menu, SWT.CASCADE);
		newSubmenuMenuItem_1.setText("Transfer to Table");

		final Menu menu_2 = new Menu(newSubmenuMenuItem_1);
		newSubmenuMenuItem_1.setMenu(menu_2);

		final MenuItem newItemMenuItem_5 = new MenuItem(menu_2, SWT.NONE);
		newItemMenuItem_5.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RTable.getInstance() != null) {
					if (RServe.isAliveDialog()) {
						List l = RShellView.getList_8();

						String[] name = l.getSelection();

						if (name.length >= 1) {
							try {
								isDataframe = (REXPLogical) RServe.getConnection().eval("try(is.data.frame(" + name[0] + "))");

							} catch (RserveException e1) {

								e1.printStackTrace();
							}
							boolean[] bolE = isDataframe.isTRUE();

							if (bolE[0]) {
								if (RState.isBusy() == false) {
									RState.setBusy(true);
									TransferDataframeToGridJob job = new TransferDataframeToGridJob(true, name[0]);
									System.out.println("Transferred");

									job.addJobChangeListener(new JobChangeAdapter() {
										public void done(IJobChangeEvent event) {
											if (event.getResult().isOK()) {

												RState.setBusy(false);
											} else {

												RState.setBusy(false);
											}
										}
									});

									job.schedule();
								} else {
									Bio7Dialog.message("RServer is busy!");
								}

							} else {
								Bio7Dialog.message("Selected variable can't be transferred.\nVariable must be a dataframe with head!");
							}

						}
					}
				} else {
					Bio7Dialog.message("No Grid view available!");
				}
			}
		});
		newItemMenuItem_5.setText("Dataframe with Head");

		final MenuItem newItemMenuItem_6 = new MenuItem(menu_2, SWT.NONE);
		newItemMenuItem_6.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RTable.getInstance() != null) {
					if (RServe.isAliveDialog()) {
						List l = RShellView.getList_8();
						String[] name = l.getSelection();
						// for (int i = 0; i < name.length; i++) {
						if (name.length >= 1) {
							try {
								isDataframe = (REXPLogical) RServe.getConnection().eval("try(is.data.frame(" + name[0] + "))");

							} catch (RserveException e1) {

								e1.printStackTrace();
							}
							boolean[] bolE = isDataframe.isTRUE();
							if (bolE[0]) {

								if (RState.isBusy() == false) {
									RState.setBusy(true);
									TransferDataframeToGridJob job = new TransferDataframeToGridJob(false, name[0]);

									job.addJobChangeListener(new JobChangeAdapter() {
										public void done(IJobChangeEvent event) {
											if (event.getResult().isOK()) {

												RState.setBusy(false);
											} else {

												RState.setBusy(false);
											}
										}
									});

									job.schedule();
								} else {
									Bio7Dialog.message("RServer is busy!");
								}

							} else {
								Bio7Dialog.message("Selected variable can't be transferred.\nVariable must be a dataframe!");

							}
							// }
						}
					}
				} else {
					Bio7Dialog.message("No Grid view available!");
				}
			}
		});
		newItemMenuItem_6.setText("Dataframe");

		new MenuItem(menu_2, SWT.SEPARATOR);

		final MenuItem newItemMenuItem_7 = new MenuItem(menu_2, SWT.NONE);
		newItemMenuItem_7.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RTable.getInstance() != null) {
					if (RServe.isAliveDialog()) {
						List l = RShellView.getList_8();
						String[] name = l.getSelection();
						if (name.length >= 1) {
							// for (int i = 0; i < name.length; i++) {
							try {
								isMatrix = (REXPLogical) RServe.getConnection().eval("try(is.matrix(" + name[0] + "))");

							} catch (RserveException e1) {

								e1.printStackTrace();
							}
							boolean[] bolE = isMatrix.isTRUE();
							if (bolE[0]) {

								if (RState.isBusy() == false) {
									RState.setBusy(true);
									TransferMatrixToGridJob job = new TransferMatrixToGridJob(name[0]);

									job.addJobChangeListener(new JobChangeAdapter() {
										public void done(IJobChangeEvent event) {
											if (event.getResult().isOK()) {

												RState.setBusy(false);
											} else {

												RState.setBusy(false);
											}
										}
									});
									// job.setSystem(true);
									job.schedule();
								} else {
									Bio7Dialog.message("RServer is busy!");
								}

							} else {
								Bio7Dialog.message("Selected variable can't be transferred.\nVariable must be a numeric matrix!");
							}
						}
					}
				} else {
					Bio7Dialog.message("No Grid view available!");
				}
			}
		});
		newItemMenuItem_7.setText("Matrix");

		new MenuItem(menu_2, SWT.SEPARATOR);

		MenuItem VectorRow = new MenuItem(menu_2, SWT.NONE);
		VectorRow.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				if (RTable.getInstance() != null) {
					if (RServe.isAliveDialog()) {
						List l = RShellView.getList_8();
						String[] name = l.getSelection();
						if (name.length >= 1) {
							// for (int i = 0; i < name.length; i++) {
							try {
								isVector = (REXPLogical) RServe.getConnection().eval("try(is.vector(" + name[0] + "))");

							} catch (RserveException e1) {

								e1.printStackTrace();
							}
							boolean[] bolE = isVector.isTRUE();
							if (bolE[0]) {

								if (RState.isBusy() == false) {
									RState.setBusy(true);
									TransferVectorToGridJob job = new TransferVectorToGridJob(name[0], true);

									job.addJobChangeListener(new JobChangeAdapter() {
										public void done(IJobChangeEvent event) {
											if (event.getResult().isOK()) {

												RState.setBusy(false);
											} else {

												RState.setBusy(false);
											}
										}
									});
									// job.setSystem(true);
									job.schedule();
								} else {
									Bio7Dialog.message("RServer is busy!");
								}

							} else {
								Bio7Dialog.message("Selected variable can't be transferred.\nVariable must be of type character or numeric!");
							}
						}
					}
				} else {
					Bio7Dialog.message("No Grid view available!");
				}
			}

		});
		VectorRow.setText("Vector to Rows");

		MenuItem VectorColumn = new MenuItem(menu_2, SWT.NONE);
		VectorColumn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (RTable.getInstance() != null) {
					if (RServe.isAliveDialog()) {
						List l = RShellView.getList_8();
						String[] name = l.getSelection();
						if (name.length >= 1) {
							// for (int i = 0; i < name.length; i++) {
							try {
								isVector = (REXPLogical) RServe.getConnection().eval("try(is.vector(" + name[0] + "))");

							} catch (RserveException e1) {

								e1.printStackTrace();
							}
							boolean[] bolE = isVector.isTRUE();
							if (bolE[0]) {

								if (RState.isBusy() == false) {
									RState.setBusy(true);
									TransferVectorToGridJob job = new TransferVectorToGridJob(name[0], false);

									job.addJobChangeListener(new JobChangeAdapter() {
										public void done(IJobChangeEvent event) {
											if (event.getResult().isOK()) {

												RState.setBusy(false);
											} else {

												RState.setBusy(false);
											}
										}
									});
									// job.setSystem(true);
									job.schedule();
								} else {
									Bio7Dialog.message("RServer is busy!");
								}

							} else {
								Bio7Dialog.message("Selected variable can't be transferred.\nVariable must be of type character or numeric!");
							}
						}
					}
				} else {
					Bio7Dialog.message("No Grid view available!");
				}

			}
		});
		VectorColumn.setText("Vector to Columns");

		MenuItem mntmRScripts = new MenuItem(menu, SWT.CASCADE);
		mntmRScripts.setText("Scripts");
		final Menu menuScripts = new Menu(mntmRScripts);
		mntmRScripts.setMenu(menuScripts);

		menuScripts.addMenuListener(new MenuListener() {

			public void menuHidden(MenuEvent e) {

			}

			@Override
			public void menuShown(MenuEvent e) {

				// plugins_ = new MenuItem[Menus.getPlugins().length];
				MenuItem[] menuItems = menuScripts.getItems();
				// Only delete the plugins menu items and menus!
				for (int i = 0; i < menuItems.length; i++) {
					if (menuItems[i] != null) {
						menuItems[i].dispose();
					}
				}

				store = Bio7Plugin.getDefault().getPreferenceStore();

				File files = new File(store.getString(PreferenceConstants.D_RSHELL_SCRIPTS));
				final File[] fil = ListFilesDirectory(files, new String[] { ".r", ".R", ".bsh", ".groovy", ".py" });

				for (int i = 0; i < fil.length; i++) {

					final int scriptCount = i;

					MenuItem item = new MenuItem(menuScripts, SWT.NONE);

					item.setText(fil[i].getName().substring(0, fil[i].getName().lastIndexOf(".")));

					item.addSelectionListener(new SelectionListener() {

						public void widgetSelected(SelectionEvent e) {

							if (text.equals("Empty")) {
								System.out.println("No script available!");
							}

							else if (fil[scriptCount].getName().endsWith(".R") || fil[scriptCount].getName().endsWith(".r")) {
								if (RServe.isAliveDialog()) {
									if (RState.isBusy() == false) {
										RState.setBusy(true);
										final RInterpreterJob Do = new RInterpreterJob(null, true, fil[scriptCount].toString());
										Do.addJobChangeListener(new JobChangeAdapter() {
											public void done(IJobChangeEvent event) {
												if (event.getResult().isOK()) {
													int countDev = RServe.getDisplayNumber();
													RState.setBusy(false);
													if (countDev > 0) {
														RServe.closeAndDisplay();
													}
												}
											}
										});
										Do.setUser(true);
										Do.schedule();
									} else {

										Bio7Dialog.message("RServer is busy!");
									}

								}
							}

							else if (fil[scriptCount].getName().endsWith(".bsh")) {

								BeanShellInterpreter.interpretJob(null, fil[scriptCount].toString());

							} else if (fil[scriptCount].getName().endsWith(".groovy")) {

								GroovyInterpreter.interpretJob(null, fil[scriptCount].toString());

							} else if (fil[scriptCount].getName().endsWith(".py")) {

								PythonInterpreter.interpretJob(null, fil[scriptCount].toString());

							}

						}

						public void widgetDefaultSelected(SelectionEvent e) {

						}
					});

				}

			}
		});

		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		FontData currentFont = PreferenceConverter.getFontData(store, "RShellFonts");
		listShell.setFont(new Font(Display.getCurrent(), currentFont));

		sashForm_1.setWeights(new int[] { 1 });

		textConsole = new Text(sashForm, SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);

		/* Set the font from the preference store! */

		textConsole.setFont(new Font(Display.getCurrent(), currentFont));
		Button fontButton;
		fontButton = new Button(composite, SWT.NONE);
		fontButton.setToolTipText("Changes the fonts for the list and \nthe results view.\nChanges are stored");
		fontButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();

				FontData defaultFont = PreferenceConverter.getFontData(store, "RShellFonts");
				FontDialog fd = new FontDialog(new Shell(), SWT.NONE);
				fd.setText("Select Font");
				fd.setRGB(new RGB(0, 0, 0));

				fd.setFontData(defaultFont);
				FontData newFont = fd.open();
				if (newFont == null)
					return;
				textConsole.setFont(new Font(Display.getDefault(), newFont));
				listShell.setFont(new Font(Display.getDefault(), newFont));
				textConsole.setForeground(new Color(Display.getDefault(), fd.getRGB()));
				listShell.setForeground(new Color(Display.getDefault(), fd.getRGB()));
				PreferenceConverter.setValue(store, "RShellFonts", newFont);

			}
		});
		fontButton.setText("Font");

		Button loadButton_1;
		loadButton_1 = new Button(composite, SWT.NONE);
		loadButton_1.setToolTipText("Saves the current workspace to \nthe temporary location");
		loadButton_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (RServe.isAliveDialog()) {
					fastSaveRWorkspace();
				}
			}

		});
		loadButton_1.setImage(ResourceManager.getPluginImage(Bio7Plugin.getDefault(), "icons/ordner_zu.gif"));

		final Button rhelpButton = new Button(parent, SWT.NONE);
		rhelpButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						RState.setBusy(true);
						Job job = new Job("Html help") {
							@Override
							protected IStatus run(IProgressMonitor monitor) {
								monitor.beginTask("Help ...", IProgressMonitor.UNKNOWN);

								try {
									RServe.getConnection().eval("try(help.start(remote=paste(\"file://\", R.home(), sep=\"\")))");

								} catch (RserveException e1) {

									e1.printStackTrace();
								}

								monitor.done();
								return Status.OK_STATUS;
							}

						};
						job.addJobChangeListener(new JobChangeAdapter() {
							public void done(IJobChangeEvent event) {
								if (event.getResult().isOK()) {

									RState.setBusy(false);
								} else {

									RState.setBusy(false);
								}
							}
						});
						// job.setSystem(true);
						job.schedule();
					} else {
						Bio7Dialog.message("RServer is busy!");
					}
				}

			}
		});
		final FormData fd_rhelpButton = new FormData();
		fd_rhelpButton.right = new FormAttachment(saveButton, 59, SWT.RIGHT);
		fd_rhelpButton.left = new FormAttachment(saveButton, 1);
		fd_rhelpButton.bottom = new FormAttachment(expandBar, -6);
		fd_rhelpButton.top = new FormAttachment(button, 0, SWT.TOP);
		rhelpButton.setLayoutData(fd_rhelpButton);
		rhelpButton.setText("R Docu");

		Button helpButton = new Button(parent, SWT.NONE);
		helpButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				Work.openView("com.eco.bio7.browser.Browser");

				htmlHelpText = null;
				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						RState.setBusy(true);
						Job job = new Job("Html help") {
							@Override
							protected IStatus run(IProgressMonitor monitor) {
								monitor.beginTask("Help ...", IProgressMonitor.UNKNOWN);

								try {
									RConnection c = RServe.getConnection();

									Display display = PlatformUI.getWorkbench().getDisplay();

									display.syncExec(new Runnable() {

										public void run() {
											htmlHelpText = text.getText();
										}
									});
									c.eval("try(outfile <- paste(tempfile(), \".html\", sep=\"\"))").toString();
									c.eval("try(tools::Rd2HTML(utils:::.getHelpFile(?" + htmlHelpText + "),outfile,package=\"tools\", stages=c(\"install\", \"render\")))");
									String out = null;
									try {
										out = (String) c.eval("try(outfile)").asString();
									} catch (REXPMismatchException e) {

										e.printStackTrace();
									}

									String pattern = "file:///" + out;
									url = pattern.replace("\\", "/");

									display.syncExec(new Runnable() {

										public void run() {
											BrowserView b = BrowserView.getBrowserInstance();
											b.setLocation(url);
										}
									});

								} catch (RserveException e1) {

									e1.printStackTrace();
								}

								monitor.done();
								return Status.OK_STATUS;
							}

						};
						job.addJobChangeListener(new JobChangeAdapter() {
							public void done(IJobChangeEvent event) {
								if (event.getResult().isOK()) {

									RState.setBusy(false);
								} else {

									RState.setBusy(false);
								}
							}
						});
						// job.setSystem(true);
						job.schedule();
					} else {
						Bio7Dialog.message("RServer is busy!");
					}
				}

			}
		});
		FormData formDataButtonPackRefresh = new FormData();
		formDataButtonPackRefresh.right = new FormAttachment(rhelpButton, 47, SWT.RIGHT);
		formDataButtonPackRefresh.left = new FormAttachment(rhelpButton, 1);
		formDataButtonPackRefresh.bottom = new FormAttachment(button, 0, SWT.BOTTOM);
		formDataButtonPackRefresh.top = new FormAttachment(button, 0, SWT.TOP);
		helpButton.setLayoutData(formDataButtonPackRefresh);
		helpButton.setText("?");
		
		Button btnNewButton = new Button(parent, SWT.NONE);
		btnNewButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String interpreterSelection=ConsolePageParticipant.getInterpreterSelection();
				Process RProcess=ConsolePageParticipant.getConsolePageParticipantInstance().getRProcess();
				Pid rPid=ConsolePageParticipant.getConsolePageParticipantInstance().getrPid();
				if (Bio7Dialog.getOS().equals("Windows")) {
					Bundle bundle = Platform.getBundle("com.eco.bio7.os");

					URL locationUrl = FileLocator.find(bundle, new Path("/"), null);
					URL fileUrl = null;
					try {
						fileUrl = FileLocator.toFileURL(locationUrl);
					} catch (IOException e2) {

						e2.printStackTrace();
					}
					File fi = new File(fileUrl.getPath());
					String pathBundle = fi.toString() + "/win/64";
					
					if (interpreterSelection.equals("R")) {
						try {
							Process p = Runtime.getRuntime().exec(pathBundle + "/SendSignalCtrlC.exe " + rPid.getPidWindows(RProcess));

						} catch (IOException ex) {
							ex.printStackTrace();
						}
						
					} 
				} else {
					if (interpreterSelection.equals("R")) {
						try {
							Process p = Runtime.getRuntime().exec("kill -INT " + rPid.getPidUnix(RProcess));

						} catch (IOException ex) {
							ex.printStackTrace();
						}
						System.out.println(rPid.getPidWindows(RProcess));
					}

					

				}

				
			}
		});
		btnNewButton.setToolTipText("Interrupt R execution");
		btnNewButton.setImage(org.eclipse.wb.swt.ResourceManager.getPluginImage("com.eco.bio7", "icons/delete.gif"));
		FormData fd_btnNewButton = new FormData();
		fd_btnNewButton.bottom = new FormAttachment(button, 0, SWT.BOTTOM);
		fd_btnNewButton.top = new FormAttachment(button, 0, SWT.TOP);
		fd_btnNewButton.right = new FormAttachment(helpButton, 47, SWT.RIGHT);
		fd_btnNewButton.left = new FormAttachment(helpButton, 1);
		btnNewButton.setLayoutData(fd_btnNewButton);
		sashForm.setWeights(new int[] { 233, 319 });
		GroupLayout gl_composite = new GroupLayout(composite);
		gl_composite.setHorizontalGroup(gl_composite.createParallelGroup(GroupLayout.LEADING).add(
				gl_composite
						.createSequentialGroup()
						.add(gl_composite
								.createParallelGroup(GroupLayout.LEADING)
								.add(gl_composite.createSequentialGroup().add(objectsButton, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE).add(removeButton, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE).add(loadButton_1, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE).add(gcButton, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
										.add(xButton, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE).add(fontButton, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE).add(iButton, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)).add(sashForm, GroupLayout.DEFAULT_SIZE, 570, Short.MAX_VALUE)).addContainerGap()));
		gl_composite.setVerticalGroup(gl_composite.createParallelGroup(GroupLayout.LEADING).add(
				gl_composite
						.createSequentialGroup()
						.add(gl_composite.createParallelGroup(GroupLayout.LEADING).add(objectsButton, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE).add(removeButton, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE).add(loadButton_1, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE).add(gcButton, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
								.add(xButton, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE).add(fontButton, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE).add(iButton, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)).add(5).add(sashForm, GroupLayout.DEFAULT_SIZE, 226, Short.MAX_VALUE)));
		composite.setLayout(gl_composite);
	}

	/*
	 * public static void startJob(String plotInstruction, int b) { if
	 * (RState.isBusy() == false) { RState.setBusy(true);
	 * 
	 * PlotJob plot = new PlotJob(plotInstruction, b);
	 * plot.addJobChangeListener(new JobChangeAdapter() { public void
	 * done(IJobChangeEvent event) { if (event.getResult().isOK()) {
	 * 
	 * RState.setBusy(false); } else {
	 * 
	 * RState.setBusy(false); } } }); plot.setUser(true); plot.schedule(); }
	 * else { Bio7Dialog.message("RServer is busy!"); } }
	 */

	private void loadScript() {
		StringBuffer buffer = new StringBuffer();
		String file = Bio7Dialog.openFile(new String[] { "*.txt", "*" });
		if (file != null) {
			File fil = new File(file);

			try {

				FileReader fileReader = new FileReader(fil);
				BufferedReader reader = new BufferedReader(fileReader);
				String str;

				int i = 0;
				while ((str = reader.readLine()) != null) {
					if (i < history.length) {
						history[i] = str;

						buffer.append(str);

						i++;
					}
				}
				reader.close();

			} catch (IOException ex) {

			}
			prov.setProposals(history);
		}
	}

	private void saveScript() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < history.length; i++) {
			buffer.append(history[i]);
			buffer.append(System.getProperty("line.separator"));
		}
		String file = Bio7Dialog.saveFile("*.txt");
		if (file != null) {
			File fil = new File(file);
			FileWriter fileWriter = null;
			try {
				fileWriter = new FileWriter(fil);

				BufferedWriter buffWriter = new BufferedWriter(fileWriter);

				String write = buffer.toString();
				buffWriter.write(write, 0, write.length());
				buffWriter.close();
			} catch (IOException ex) {

			} finally {
				try {
					fileWriter.close();
				} catch (IOException ex) {

				}
			}
		}

	}

	public ContentProposalAdapter getAdapter() {
		return adapter;
	}

	public void setAdapter(ContentProposalAdapter adapter) {
		this.adapter = adapter;
	}

	public ContentProposalAdapter getAdapter2() {
		return adapter2;
	}

	public void setAdapter2(ContentProposalAdapter adapter2) {
		this.adapter2 = adapter2;
	}

	public TextContentAdapter getTextadapter() {
		return textAdapter;
	}

	public void setTextadapter(TextContentAdapter textadapter) {
		this.textAdapter = textadapter;
	}

	public TextContentAdapter getTextadapter2() {
		return textAdapter2;
	}

	public void setTextadapter2(TextContentAdapter textadapter2) {
		this.textAdapter2 = textadapter2;
	}

	public SimpleContentProposalProvider getProv() {
		return prov;
	}

	public void setProv(SimpleContentProposalProvider prov) {
		this.prov = prov;
	}

	public String[] getTemphistory() {
		return tempHistory;
	}

	public void setTemphistory(String[] temphistory) {
		this.tempHistory = temphistory;
	}

	public static RCompletionShell getShellInstance() {
		return shellInstance;
	}

	private void setInDocument(List aList) {
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor != null && editor instanceof REditor) {

			String[] items = aList.getSelection();
			ITextEditor editor2 = (ITextEditor) editor;

			IDocumentProvider dp = editor2.getDocumentProvider();
			IDocument doc = dp.getDocument(editor.getEditorInput());

			ISelectionProvider sp = editor2.getSelectionProvider();
			ISelection selectionsel = sp.getSelection();
			ITextSelection selection = (ITextSelection) selectionsel;

			int off = selection.getOffset();

			try {
				doc.replace(off, 0, items[0]);
			} catch (BadLocationException e1) {
				e1.printStackTrace();
			}

		}
	}

	private void evaluate() {
		try {
			if (text.getText() != null) {
				if (RServe.isAliveDialog()) {
					if (!text.getText().contains(";")) {
						com.eco.bio7.rbridge.RServe.printJob(text.getText());
						System.out.println();
					} else {
						String[] t = text.getText().split(";");
						/* Send multiple expressions and evaluate them! */
						com.eco.bio7.rbridge.RServe.printJobs(t);
						/* Linebreak in the job for multiple expressions! */

					}

				}
			}
		} catch (RuntimeException ev) {

			ev.printStackTrace();
		}

		for (int i = 0; i < history.length - 1; i++) {

			tempHistory[i + 1] = history[i];

		}
		for (int j = 0; j < history.length; j++) {
			history[j] = tempHistory[j];
		}

		history[0] = text.getText();
		prov.setProposals(history);
		text.setText("");
		text.setFocus();

	}

	@Override
	public void setFocus() {

	}

	private void initializeToolBar() {
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
	}

	public static boolean isConsoleExpanded() {

		return isConsoleExpanded;
	}

	public static void setTextConsole(final String text) {
		final Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				if (textConsole != null && textConsole.isDisposed() == false) {
					if (text != null) {
						textConsole.setText(text);
					}
				}
			}
		});
	}

	private void fastSaveRWorkspace() {
		IPreferenceStore store = null;
		String path = null;
		try {
			store = Bio7Plugin.getDefault().getPreferenceStore();
		} catch (RuntimeException ex) {

			ex.printStackTrace();
		}
		if (store != null) {
			path = store.getString(PreferenceConstants.P_TEMP_R);
		}

		String selected = path + "\\tempCurrent";

		File dir = new File(path);
		if (dir.canWrite()) {

			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
				selected = selected.replace("\\", "\\\\");
			}

			if (selected != null) {

				String save = "try(save.image(file =\"" + selected + ".RData" + "\", version = NULL, ascii = FALSE))";
				if (RState.isBusy() == false) {
					RState.setBusy(true);

					RInterpreterJob Do = new RInterpreterJob(save, false, null);

					Do.addJobChangeListener(new JobChangeAdapter() {
						public void done(IJobChangeEvent event) {
							if (event.getResult().isOK()) {
								RState.setBusy(false);
							} else {
								RState.setBusy(false);
							}

						}
					});

					Do.setUser(true);
					Do.schedule();

				} else {
					Bio7Dialog.message("RServer is busy!");
				}
			}
		} else {
			Bio7Dialog.message("Directory is not writable!");
		}
	}

	private void displayRObjects() {
		REXP x = null;
		listShell.removeAll();

		if (RServe.isAliveDialog()) {
			if (RState.isBusy() == false) {
				String[] v = null;
				// List all variables in the R workspace!

				try {
					RServe.getConnection().eval("try(var<-ls())");
					x = RServe.getConnection().eval("try(var)");
					try {
						v = x.asStrings();
					} catch (REXPMismatchException e1) {

						e1.printStackTrace();
					}
					RServe.getConnection().eval("try(remove(var))");
				} catch (RserveException e1) {

					e1.printStackTrace();
				}

				for (int i = 0; i < v.length; i++) {

					listShell.add(v[i]);

				}
			} else {
				Bio7Dialog.message("RServer is busy!");

			}

		}
	}

	private void removeSelectedVars() {
		REXP x = null;

		if (RServe.isAliveDialog()) {
			if (RState.isBusy() == false) {
				MessageBox message = new MessageBox(new Shell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
				message.setMessage("Do you really want to remove the selected object(s)?");
				message.setText("Remove objects");
				int response = message.open();
				if (response == SWT.YES) {
					String[] data = listShell.getSelection();

					try {
						for (int i = 0; i < data.length; i++) {
							if (data[i].equals("bio7Device") == false) {
								RServe.getConnection().eval("try(remove(" + data[i] + "))");
							} else {
								System.out.println("Variable bio7Device wasn't deleted\nNecessary function definition (defined in the Rserve preferences) for the default plot device in Bio7!");
							}
						}

					} catch (RserveException e1) {

						e1.printStackTrace();
					}

					String[] v = null;
					listShell.removeAll();
					// List all variables in the R workspace!

					try {
						RServe.getConnection().eval("var<-ls()");
						x = RServe.getConnection().eval("var");
						try {
							v = x.asStrings();
						} catch (REXPMismatchException e1) {

							e1.printStackTrace();
						}
						RServe.getConnection().eval("remove(var)");
					} catch (RserveException e1) {

						e1.printStackTrace();
					}

					for (int i = 0; i < v.length; i++) {

						listShell.add(v[i]);

					}

				}
			} else {
				Bio7Dialog.message("RServer is busy!");
			}
		}
	}

	public void createAttachedPackageTree() {
		REXP pack = null;
		REXP functions = null;
		tree.removeAll();

		if (RServe.isAliveDialog()) {
			if (RState.isBusy() == false) {
				String[] v = null;
				// List all variables in the R workspace!

				try {
					RServe.getConnection().eval("try(packs<-.packages())");
					pack = RServe.getConnection().eval("try(packs)");
					try {
						v = pack.asStrings();
					} catch (REXPMismatchException e1) {

						e1.printStackTrace();
					}
					RServe.getConnection().eval("try(remove(packs))");
				} catch (RserveException e1) {

					e1.printStackTrace();
				}
				TreeItem packages = new TreeItem(tree, 0);
				packages.setText("Attached Packages:");
				// packages.removeAll();
				for (int i = 0; i < v.length; i++) {

					// lsf.str("package:MASS")
					TreeItem treeItem = new TreeItem(packages, 0);
					treeItem.setText(v[i]);

					/*
					 * String[] func = null;
					 * 
					 * try {
					 * RServe.getConnection().eval("listfunc<-ls.str(\"package:"
					 * +v[i]+"\")");
					 * 
					 * try { func =
					 * RServe.getConnection().eval("as.vector(listfunc)"
					 * ).asStrings(); } catch (REXPMismatchException e) { //
					 * TODO Auto-generated catch block e.printStackTrace(); } }
					 * catch (RserveException e) { // TODO Auto-generated catch
					 * block e.printStackTrace(); }
					 * 
					 * 
					 * 
					 * for (int j = 0; j < func.length; j++) {
					 * 
					 * TreeItem treeItemSub= new TreeItem(treeItem, 0);
					 * treeItemSub.setText(func[i]) ;
					 * 
					 * }
					 */

				}
				packages.setExpanded(true);
			} else {
				Bio7Dialog.message("RServer is busy!");

			}

		}

	}

	public static RShellView getInstance() {
		return instance;
	}

	public static File[] ListFilesDirectory(File filedirectory, final String[] extensions) {
		File dir = filedirectory;

		String[] children = dir.list();
		if (children == null) {

		} else {
			for (int i = 0; i < children.length; i++) {
				// Get filename of the file or directory inside Bio7.
				String filename = children[i];
			}
		}

		// Filter the extension of the file.
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.endsWith(extensions[0]) || name.endsWith(extensions[1]) || name.endsWith(extensions[2]) || name.endsWith(extensions[3]) || name.endsWith(extensions[4]));
			}
		};

		File[] files = dir.listFiles(filter);

		return files;
	}

	public static List getListShell() {
		return listShell;
	}
}
