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
import java.io.IOException;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.completion.ContentProposalAdapter;
import com.eco.bio7.reditors.REditor;

public class RCompletionShell extends Shell {
	private static Text textConsole;
	private List list_7;
	private List list_6;
	private List list_5;
	private List list_4;
	private List list_3;
	private List list_2;
	private List list_1;
	private List list;
	private IWorkbenchWindow window;
	private Text text;
	private SimpleContentProposalProvider prov;
	private ContentProposalAdapter adapter;
	private ContentProposalAdapter adapter2;
	private TextContentAdapter textAdapter;
	private TextContentAdapter textAdapter2;
	private KeyStroke stroke;
	private KeyStroke stroke2;
	private String[] props;
	private String[] history;
	private String[] tempHistory;
	private static RCompletionShell shellInstance = null;

	private void createShell() {
		shellInstance = this;
		setText("R");
		setSize(292, 554);
		if (RFunctions.getPropsHistInstance() == null) {
			new RFunctions();
		}
		history = RFunctions.getPropsHistInstance().getHistory();
		tempHistory = RFunctions.getPropsHistInstance().getTemphistory();
		prov = new SimpleContentProposalProvider(history);
		setBounds(300, 300, 300, 100);
		text = new Text(this, SWT.SINGLE | SWT.BORDER);
		// text.setBounds(5, 5, 237, 21);
		final FormData fd_text = new FormData();
		fd_text.right = new FormAttachment(100, -5);
		fd_text.bottom = new FormAttachment(0, 26);
		fd_text.top = new FormAttachment(0, 5);
		fd_text.left = new FormAttachment(0, 5);
		text.setLayoutData(fd_text);
		ControlDecoration dec = new ControlDecoration(text, SWT.TOP | SWT.LEFT);
		FieldDecoration infoFieldIndicator = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		dec.setImage(infoFieldIndicator.getImage());
		dec.setDescriptionText("Press F2 to get History");
		textAdapter = new TextContentAdapter();
		textAdapter2 = new TextContentAdapter();
		stroke = KeyStroke.getInstance(SWT.F2);
		adapter = new ContentProposalAdapter(text, textAdapter, prov, stroke, null);
		stroke2 = KeyStroke.getInstance(SWT.F3);
		Button button = new Button(this, SWT.F2);
		final FormData fd_button = new FormData();
		fd_button.bottom = new FormAttachment(0, 56);
		fd_button.top = new FormAttachment(0, 31);
		fd_button.right = new FormAttachment(0, 119);
		fd_button.left = new FormAttachment(0, 5);
		button.setLayoutData(fd_button);
		button.setText("Evaluate Expression");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					if (text.getText() != "") {
						if (RServe.isAliveDialog()) {
							com.eco.bio7.rbridge.RServe.printJob(text.getText());
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
		});

		setDefaultButton(button);
		pack();
		setSize(296, 388);
		Image image = Bio7Plugin.getImageDescriptor("/icons/views/raction.png").createImage();
		setImage(image);

		final ExpandBar expandBar = new ExpandBar(this, SWT.NONE | SWT.V_SCROLL);
		final FormData fd_expandBar = new FormData();
		fd_expandBar.bottom = new FormAttachment(100, -5);
		fd_expandBar.right = new FormAttachment(100, -5);
		fd_expandBar.top = new FormAttachment(0, 62);
		fd_expandBar.left = new FormAttachment(0, 0);
		expandBar.setLayoutData(fd_expandBar);
		expandBar.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

		final ExpandItem newItemExpandItem_8 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_8.setHeight(400);
		newItemExpandItem_8.setText("Output");

		final Composite composite = new Composite(expandBar, SWT.NONE);
		composite.setLayout(new FillLayout());
		newItemExpandItem_8.setControl(composite);

		textConsole = new Text(composite, SWT.WRAP | SWT.MULTI |SWT.V_SCROLL| SWT.H_SCROLL | SWT.BORDER);

		final ExpandItem newItemExpandItem = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem.setHeight(100);
		newItemExpandItem.setText("Variables/Help");

		list = new List(expandBar, SWT.BORDER | SWT.V_SCROLL);
		list.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(final MouseEvent e) {

				if (e.button == 3) {

					setInDocument(list);

					String[] items = list.getSelection();
					// text.getSelection().
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
				list.setToolTipText(RFunctions.getPropsHistInstance().variablesContext[index]);

			}

		});

		list.setItems(RFunctions.getPropsHistInstance().variables);

		newItemExpandItem.setControl(list);

		final ExpandItem newItemExpandItem_1 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_1.setHeight(600);
		newItemExpandItem_1.setText("Create/Extract data");

		list_1 = new List(expandBar, SWT.BORDER | SWT.V_SCROLL);
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
				list_1.setToolTipText(RFunctions.getPropsHistInstance().dataContext[index]);

			}
		});
		list_1.setItems(RFunctions.getPropsHistInstance().data);
		newItemExpandItem_1.setControl(list_1);

		final ExpandItem newItemExpandItem_3 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_3.setHeight(750);
		newItemExpandItem_3.setText("Math");

		list_3 = new List(expandBar, SWT.BORDER | SWT.V_SCROLL);
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
				list_3.setToolTipText(RFunctions.getPropsHistInstance().mathContext[index]);

			}
		});
		list_3.setItems(RFunctions.getPropsHistInstance().math);
		newItemExpandItem_3.setControl(list_3);

		final ExpandItem newItemExpandItem_4 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_4.setHeight(450);
		newItemExpandItem_4.setText("Statistics");

		list_4 = new List(expandBar, SWT.BORDER | SWT.V_SCROLL);
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
				list_4.setToolTipText(RFunctions.getPropsHistInstance().statisticsContext[index]);

			}
		});
		list_4.setItems(RFunctions.getPropsHistInstance().statistics);
		newItemExpandItem_4.setControl(list_4);

		final ExpandItem newItemExpandItem_5 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_5.setHeight(300);
		newItemExpandItem_5.setText("Spatial Statistics");

		list_5 = new List(expandBar, SWT.BORDER | SWT.V_SCROLL);
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
				list_5.setToolTipText(RFunctions.getPropsHistInstance().spatialStatsContext[index]);

			}
		});
		list_5.setItems(RFunctions.getPropsHistInstance().spatialStats);
		newItemExpandItem_5.setControl(list_5);

		final ExpandItem newItemExpandItem_6 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_6.setHeight(330);
		newItemExpandItem_6.setText("Matrix Calculations");

		list_6 = new List(expandBar, SWT.BORDER | SWT.V_SCROLL);
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
				list_6.setToolTipText(RFunctions.getPropsHistInstance().matrixContext[index]);

			}
		});
		list_6.setItems(RFunctions.getPropsHistInstance().matrix);
		newItemExpandItem_6.setControl(list_6);

		final ExpandItem newItemExpandItem_7 = new ExpandItem(expandBar, SWT.NONE);
		newItemExpandItem_7.setHeight(100);
		newItemExpandItem_7.setText("Image Analysis");

		list_7 = new List(expandBar, SWT.V_SCROLL | SWT.BORDER);
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
				list_7.setToolTipText(RFunctions.getPropsHistInstance().imageAnalysisContext[index]);

			}
		});

		list_7.setItems(RFunctions.getPropsHistInstance().imageAnalysis);
		newItemExpandItem_7.setControl(list_7);
		

		final Button loadButton = new Button(this, SWT.NONE);
		loadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				loadScript();
			}
		});
		final FormData fd_loadButton = new FormData();
		fd_loadButton.bottom = new FormAttachment(button, 25, SWT.TOP);
		fd_loadButton.top = new FormAttachment(button, 0, SWT.TOP);
		fd_loadButton.right = new FormAttachment(0, 175);
		fd_loadButton.left = new FormAttachment(0, 130);
		loadButton.setLayoutData(fd_loadButton);
		loadButton.setText("Load");

		Button saveButton;
		saveButton = new Button(this, SWT.NONE);
		saveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				saveScript();
			}
		});
		final FormData fd_saveButton = new FormData();
		fd_saveButton.bottom = new FormAttachment(loadButton, 25, SWT.TOP);
		fd_saveButton.top = new FormAttachment(loadButton, 0, SWT.TOP);
		fd_saveButton.right = new FormAttachment(loadButton, 48, SWT.RIGHT);
		fd_saveButton.left = new FormAttachment(loadButton, 5, SWT.RIGHT);
		saveButton.setLayoutData(fd_saveButton);
		saveButton.setText("Save");

		open();

	}

	public static void setTextConsole(final String text) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				textConsole.setText(text);
			}
		});
	}

	private void loadScript() {
		StringBuffer buffer = new StringBuffer();
		String file = Bio7Dialog.openFile();
		File fil = new File(file);

		try {

			FileReader fileReader = new FileReader(fil);
			BufferedReader reader = new BufferedReader(fileReader);
			String str;

			int i = 0;
			while ((str = reader.readLine()) != null) {
               if(i<history.length){
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

	private void saveScript() {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < history.length; i++) {
			buffer.append(history[i]);
			buffer.append(System.getProperty("line.separator"));
		}
		String file = Bio7Dialog.saveFile("*.txt");
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

	/**
	 * Launch the application
	 * 
	 * @param args
	 */

	/**
	 * Create the shell
	 * 
	 * @param display
	 * @param style
	 */
	public RCompletionShell(Display display, int style) {
		super(display, style);
		setLayout(new FormLayout());
		createShell();

	}

	/**
	 * Create contents of the window
	 */

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
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

	public static void setShellInstance(RCompletionShell shellInstance) {
		RCompletionShell.shellInstance = shellInstance;
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

}
