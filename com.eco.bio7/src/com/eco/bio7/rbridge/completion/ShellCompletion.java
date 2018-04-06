/*ImageProvider adapted from:
https://github.com/eclipse/sapphire/blob/master/plugins/org.eclipse.sapphire.ui/src/org/eclipse/sapphire/ui/forms/swt/TextFieldPropertyEditorPresentation.java
See license info below
/******************************************************************************
 * Copyright (c) 2016 Oracle, Accenture and Modelity Technologies
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Konstantin Komissarchik - initial implementation and ongoing maintenance
 *   Kamesh Sampath - [354199] Support content proposals in text field property editor
 *   Roded Bahat - [376198] Vertically align actions for @LongString property editors
 ******************************************************************************
 *
 * Implementation of textfield proposals
 * Author: M. Austenfeld
 */

package com.eco.bio7.rbridge.completion;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RServeUtil;
import com.eco.bio7.rbridge.RShellView;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditor.antlr.Parse;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.rpreferences.template.CalculateRProposals;
import com.eco.bio7.util.Util;
import com.swtdesigner.ResourceManager;

public class ShellCompletion {
	private ContentProposalProvider contentProposalProvider;
	private ContentProposalAdapter contentProposalAdapter;
	private KeyStroke stroke;

	private Image image = ResourceManager.getPluginImage(Bio7Plugin.getDefault(), "icons/brkp_obj.png");
	private Image varImage = ResourceManager.getPluginImage(Bio7Plugin.getDefault(), "icons/methdef_obj.png");
	private Image varFuncCallImage = ResourceManager.getPluginImage(Bio7Plugin.getDefault(), "/icons/varfunccall.png");
	private Image s4Image = ResourceManager.getPluginImage(Bio7Plugin.getDefault(), "icons/s4.png");
	private Image s3Image = ResourceManager.getPluginImage(Bio7Plugin.getDefault(), "icons/s3.png");
	private Image dataImage = ResourceManager.getPluginImage(Bio7Plugin.getDefault(), "/icons/settings_obj.png");
	private Image libImage = ResourceManager.getPluginImage(Bio7Plugin.getDefault(), "/icons/package_obj.png");
	private Text control;
	private String[] statistics;
	private String[] statisticsContext;
	private String[] statisticsSet;
	private ImageContentProposal[] propo;
	public boolean s4;
	public boolean s3;
	private RShellView view;
	private boolean packageAll;
	private boolean packageExport;
	private IPreferenceStore store;
	// public boolean data;
	// public boolean library;

	/*
	 * Next two methods adapted from:
	 * https://krishnanmohan.wordpress.com/2011/12/12/eclipse-rcp-
	 * autocompletecombotext-control/
	 */
	static char[] getAutoactivationChars() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();

		String LCL = store.getString("RSHELL_ACTIVATION_CHARS");
		// String UCL = LCL.toUpperCase();

		// To enable content proposal on deleting a char

		// String delete = new String(new char[] { 8 });
		String allChars = LCL;
		return allChars.toCharArray();
	}

	static KeyStroke getActivationKeystroke() {
		IPreferenceStore storeLocal = Bio7Plugin.getDefault().getPreferenceStore();
		KeyStroke instance;
		String useAlt = storeLocal.getString("RSHELL_CODE_COMPLETION_ACTIVATOR_ALTERED");
		if (useAlt.equals("CMD")) {
			instance = KeyStroke.getInstance(new Integer(SWT.COMMAND).intValue(), new Integer(' ').intValue());
		}

		else if (useAlt.equals("ALT")) {
			instance = KeyStroke.getInstance(new Integer(SWT.ALT).intValue(), new Integer(' ').intValue());
		}

		else {
			instance = KeyStroke.getInstance(new Integer(SWT.CTRL).intValue(), new Integer(' ').intValue());
		}

		return instance;
	}

	public ShellCompletion(RShellView view, Text control, final IControlContentAdapter controlContentAdapter) {
		// IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		this.view = view;
		this.control = control;
		contentProposalProvider = new ContentProposalProvider();
		contentProposalProvider.setFiltering(true);

		stroke = getActivationKeystroke();
		store = Bio7Plugin.getDefault().getPreferenceStore();
		boolean typedCodeCompletion = store.getBoolean("RSHELL_TYPED_CODE_COMPLETION");
		if (typedCodeCompletion) {
			contentProposalAdapter = new ContentProposalAdapter(control, controlContentAdapter, contentProposalProvider, stroke, getAutoactivationChars());
			contentProposalAdapter.setPopupSize(new Point(store.getInt("CODE_COMPLETION_POPUP_SIZE_X"), store.getInt("CODE_COMPLETION_POPUP_SIZE_Y")));
		} else {
			contentProposalAdapter = new ContentProposalAdapter(control, controlContentAdapter, contentProposalProvider, stroke, null);
			contentProposalAdapter.setPopupSize(new Point(store.getInt("CODE_COMPLETION_POPUP_SIZE_X"), store.getInt("CODE_COMPLETION_POPUP_SIZE_Y")));
		}
		contentProposalAdapter.setPropagateKeys(true);
		contentProposalAdapter.setLabelProvider(new ContentProposalLabelProvider());
		contentProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_IGNORE);// Use
																									// custom
																									// replacements
																									// here!
		contentProposalAdapter.addContentProposalListener(new IContentProposalListener() {

			@Override
			public void proposalAccepted(IContentProposal proposal) {
				/* We have to care about the custom replacements! */
				control.setFocus();
				// String content = control.getText();
				/*
				 * Weird behavior of text.getCaretPosition() position on MacOSX. Solved by
				 * extracting a local var here!
				 */
				int caretPosition = control.getCaretPosition();
				control.setSelection(contentProposalProvider.lastIndex, caretPosition);
				// Point selection = control.getSelection();

				/*
				 * Insert the completion proposal in between selection start and selection end!
				 */
				if (s3 == true || s4 == true) {

					String textSel = control.getText(0, caretPosition - 1);
					int lastIndex;
					if (s3) {
						lastIndex = textSel.lastIndexOf("$");
						s3 = false;
					} else {
						lastIndex = textSel.lastIndexOf("@");
						s4 = false;
					}

					String textLastIndex = control.getText(0, lastIndex);
					String after = control.getText(caretPosition, control.getText().length());
					String content = textLastIndex + proposal.getContent() + after;
					int cursorPosition = (textLastIndex + proposal.getContent()).length();
					control.setText(content);
					control.setSelection(cursorPosition);
				}

				else if (packageAll == true || packageExport == true) {
					packageAll = false;
					packageExport = false;
					String textSel = control.getText(0, caretPosition - 1);
					int lastIndex = textSel.lastIndexOf(":");
					String textLastIndex = control.getText(0, lastIndex);
					String after = control.getText(caretPosition, control.getText().length());
					String content = textLastIndex + proposal.getContent() + after;
					int cursorPosition = lastIndex + proposal.getContent().length() + 1;
					control.setText(content);
					control.setSelection(cursorPosition);
				}

				else {
					// data = false;
					// library = false;
					int pos = calculateFirstOccurrenceOfChar(control, caretPosition);
					String textSel = control.getText(0, pos - 1);
					String content = control.getText();
					String after = control.getText(caretPosition, content.length());
					// content = textSel + proposal.getContent() + "()" + after;
					content = textSel + proposal.getContent() + after;
					// int cursorPosition = (textSel + proposal.getContent() + "()").length() - 1;
					int cursorPosition = (textSel + proposal.getContent()).length();
					control.setText(content);
					if (proposal.getContent().endsWith("()")) {
						control.setSelection(cursorPosition - 1);
					} else {
						control.setSelection(cursorPosition);
					}
				}
				/* Notify a change for the parser of the R-Shell view! */
				Event e = new Event();
				control.notifyListeners(SWT.KeyUp, e);

			}

		});

	}

	/* Here we update the code templates by calling the R function! */
	public void update() {

		RConnection con = REditor.getRserveConnection();
		if (con != null) {
			Job job = new Job("Reload") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Reload package information...", IProgressMonitor.UNKNOWN);

					RConnection c = REditor.getRserveConnection();
					if (c != null) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							try {

								/*
								 * Function loaded at Rserve startup. Writes the available functions to a file!
								 */
								c.eval(".bio7WriteFunctionDef();");
							} catch (RserveException e) {

								e.printStackTrace();
							}

							/*
							 * Reload the code proposals (not the templates) for the R editor!
							 */
							CalculateRProposals.setStartupTemplate(false);
							CalculateRProposals.loadRCodePackageTemplates();
							/* Load the created proposals! */
							statistics = CalculateRProposals.getStatistics();
							statisticsContext = CalculateRProposals.getStatisticsContext();
							statisticsSet = CalculateRProposals.getStatisticsSet();

						}

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

					}
				}
			});
			// job.setSystem(true);
			job.schedule();
		} else {
			// message("Rserve is not alive!");
		}

	}

	public void setProposals(final String[] proposals) {
		contentProposalProvider.setProposals(proposals);
	}

	public ContentProposalProvider getContentProposalProvider() {
		return contentProposalProvider;
	}

	public ContentProposalAdapter getContentProposalAdapter() {
		return contentProposalAdapter;
	}

	/*
	 * Here we calculate the first occurrence of the below chars to the left to
	 * enable nested commands!
	 */
	protected int calculateFirstOccurrenceOfChar(Text control, int offset) {
		int i = offset;
		String sep = store.getString("RSHELL_SEPERATOR_CHARS");
		char[] charArray = sep.toCharArray();
		String tex = control.getText();
		if (i > tex.length())
			return 0;
		/* Label the outerloop for a break statement! */
		outerloop: while (i > 0) {

			char ch = tex.charAt(i - 1);

			for (int j = 0; j < charArray.length; j++) {

				if (ch == charArray[j]) {
					break outerloop;
				}
			}

			/*
			 * if ((ch == ';') || (ch == '(') || (ch == ',') || (ch == '[') || (ch == '=')
			 * || (ch == '-') || (ch == '+') || Character.isSpaceChar(ch)) break;
			 */

			i--;
		}

		return i;

	}

	public class ContentProposalProvider implements IContentProposalProvider {

		private IContentProposal[] contentProposals;
		private boolean filterProposals = true;
		public int lastIndex;

		public ContentProposalProvider() {
			super();
			/*
			 * At startup load the default R proposals and add them to the templates!
			 */

			CalculateRProposals.loadRCodePackageTemplates();

			/* Load the created proposals! */
			statistics = CalculateRProposals.getStatistics();
			statisticsContext = CalculateRProposals.getStatisticsContext();
			statisticsSet = CalculateRProposals.getStatisticsSet();
		}

		public IContentProposal[] getProposals(String contents, int position) {
			s4 = false;
			s3 = false;
			packageAll = false;
			packageExport = false;
			ArrayList<IContentProposal> list = new ArrayList<IContentProposal>();
			ArrayList<IContentProposal> varWorkspace = new ArrayList<IContentProposal>();
			int offset = position;
			int lastIndex = 0;
			lastIndex = calculateFirstOccurrenceOfChar(control, offset);
			int textLength = 0;
			String contentLast;
			if (lastIndex >= 0) {
				textLength = offset - lastIndex;
				contentLast = control.getText(lastIndex, offset);

			} /*
				 * else { textLength = control.getText().length(); contentLast =
				 * control.getText(); }
				 */

			/* We need the substring here without a trailing char like ')'! */
			String contentLastCorr = control.getText(lastIndex, offset - 1);
			if (contentLastCorr.contains("@") || contentLastCorr.contains("$")) {
				int x = contentLastCorr.indexOf("@");
				int y = contentLastCorr.indexOf("$");

				if (x > y) {

					s4 = true;
					return s4Activation(position, contentLastCorr);

				} else {

					s3 = true;
					return s3Activation(position, contentLastCorr);

				}
			} else if (contentLastCorr.contains(":::")) {
				packageAll = true;
				return namesPackageAllActivation(position, contentLastCorr);
			}

			else if (contentLastCorr.contains("::")) {
				packageExport = true;
				return namesPackageExportActivation(position, contentLastCorr);
			}

			Parse parse = view.getParser();

			/*
			 * For the differentiation in nested function or matrix indexing calls the
			 * parser takes the nearest, see the ExtractInterfaceListener class! If a
			 * argument completion is not available it returns the default completion!
			 */

			/* Control if we are in a function call! */
			if (parse != null && parse.isInFunctionCall()) {
				String funcName = parse.getFuncName();
				/* Activate data completion! */
				if (funcName.equals("data")) {
					// if (contentLastCorr.length() == 0) {
					return dataActivation(position, contentLastCorr);
					// }
					/* Activate library completion! */
				} else if (funcName.equals("library") || parse.getFuncName().equals("require")) {
					// if (contentLastCorr.length() == 0) {
					return libraryActivation(position, contentLastCorr);
					// }

				} else {
					/*
					 * If length is null show function arguments else all functions and variables!
					 */
					if (contentLastCorr.length() == 0) {

						return functionArgumentsActivation(position, funcName);
					}

				}
			}
			/* Control if we are in a matrix '[]' call! */
			if (parse != null && parse.isInMatrixBracketCall()) {
				if (contentLastCorr.length() == 0) {
					String name = parse.getBracketMatrixName();
					byte state = parse.getMatrixArgState();
					if (state == 1) {
						return matrixDataFrameSubset(position, contentLastCorr, name, state, false);
					}
					// if call has two arguments, cursor on the left argument!
					else if (state == 21) {
						return matrixDataFrameSubset(position, contentLastCorr, name, state, false);
					}
					// if call has two arguments, cursor on the right argument!
					else if (state == 22) {
						return matrixDataFrameSubset(position, contentLastCorr, name, state, false);
					}
				}
			}
			/* Control if we are in a matrix '[[]]' call! */
			if (parse != null && parse.isInMatrixDoubleBracketCall()) {
				if (contentLastCorr.length() == 0) {
					String name = parse.getBracketMatrixName();
					byte state = parse.getMatrixArgState();
					if (state == 1) {
						return matrixDataFrameSubset(position, contentLastCorr, name, state, true);
					}
					// if call has two arguments, cursor on the left argument!
					else if (state == 21) {
						return matrixDataFrameSubset(position, contentLastCorr, name, state, true);
					}
					// if call has two arguments, cursor on the right argument!
					else if (state == 22) {
						return matrixDataFrameSubset(position, contentLastCorr, name, state, true);
					}
				}

			}
			/*
			 * This section loads the general code completion if no other method returned a
			 * special case!
			 */
			if (RServe.isAlive()) {
				/* Here we get the R workspace vars! */
				ImageContentProposal[] workspaceVars = getWorkSpaceVars(position);
				if (workspaceVars != null) {
					for (int i = 0; i < workspaceVars.length; i++) {
						/*
						 * Here we filter out the vars by comparing the typed letters with the available
						 * workspace vars!
						 */
						if (workspaceVars[i].getLabel().length() >= textLength && workspaceVars[i].getLabel().substring(0, textLength).equalsIgnoreCase(contentLastCorr)) {
							varWorkspace.add(workspaceVars[i]);
						}
					}
				}
			}

			/* If text length after parenheses is at least 0! */
			if (textLength >= 0) {
				for (int i = 0; i < statistics.length; i++) {
					/*
					 * Here we filter out the templates by comparing the typed letters with the
					 * available templates!
					 */
					if (statistics[i].length() >= textLength && statistics[i].substring(0, textLength).equalsIgnoreCase(contentLastCorr)) {
						list.add(makeContentProposal(statistics[i] + "()", statisticsContext[i], statisticsSet[i]));
					}
				}

			}
			/* If text length after parentheses is -1! */
			else {
				for (int i = 0; i < statistics.length; i++) {

					list.add(makeContentProposal(statistics[i], statisticsContext[i], statisticsSet[i]));

				}
			}

			IContentProposal[] array = list.toArray(new IContentProposal[list.size()]);
			/* We have to convert the proposals to an ImageContentProposal! */
			IContentProposal[] arrayTemp = makeProposalArray(array);
			/* The var Workspace arrays are already an ImageContentProposal! */
			IContentProposal[] varWorkspaceArray = varWorkspace.toArray(new IContentProposal[varWorkspace.size()]);
			/* Concatenate both with the Apache commons library! */
			IContentProposal[] allProposals = (IContentProposal[]) ArrayUtils.addAll(varWorkspaceArray, arrayTemp);
			return allProposals;
		}

		private IContentProposal[] makeProposalArray(IContentProposal[] proposals) {
			if (proposals != null) {
				IContentProposal[] arrContentProposals = new IContentProposal[proposals.length];
				for (int i = 0; i < proposals.length; i++) {

					ImageContentProposal contentProposal = new ImageContentProposal(proposals[i].getContent(), proposals[i].getLabel(), proposals[i].getDescription(),
							proposals[i].getContent().length(), image);
					arrContentProposals[i] = contentProposal;
				}
				return arrContentProposals;
			} else {
				return new IContentProposal[0];
			}
		}

		public void setProposals(String[] items) {
			statistics = items;
			contentProposals = null;
		}

		public void setFiltering(boolean filterProposals) {
			this.filterProposals = filterProposals;
			contentProposals = null;
		}

		private IContentProposal makeContentProposal(final String proposal, final String label, final String description) {
			return new IContentProposal() {

				public String getContent() {
					return proposal;
				}

				public String getDescription() {

					return description;
				}

				public String getLabel() {
					return proposal + " - " + label;
				}

				public int getCursorPosition() {
					return proposal.length();
				}
			};
		}

	}

	private static final class ContentProposalLabelProvider extends LabelProvider {
		@Override
		public Image getImage(Object element) {

			return ((ImageContentProposal) element).getImage();
		}

		@Override
		public String getText(Object element) {
			return ((ImageContentProposal) element).getLabel();
		}
	}

	private static final class ImageContentProposal extends org.eclipse.jface.fieldassist.ContentProposal {

		private Image image;

		public ImageContentProposal(String content, String label, String description, int cursorPosition, Image image) {
			super(content, label, description, cursorPosition);
			this.image = image;
		}

		public Image getImage() {
			return this.image;
		}
	}

	/*
	 * Here we calculate matrix, dataframe subsets (rows, columns, etc.)
	 */
	private ImageContentProposal[] matrixDataFrameSubset(int position, String contentLastCorr, String matDfName, int state, boolean doubleMatrixCall) {
		IContentProposal[] array = null;
		int length = contentLastCorr.length();
		RConnection c = RServe.getConnection();
		if (c != null) {
			propo = null;
			ArrayList<IContentProposal> list = new ArrayList<IContentProposal>();
			String[] item = null;
			ImageContentProposal[] prop = getWorkSpaceVars(position);
			/* Get all installed dataset names, their package and description! */

			if (doubleMatrixCall) {
				try {
					REXP rexp = RServeUtil.fromR("try(colnames(" + matDfName + "),silent=TRUE)");
					if (rexp.isNull() == false) {
						item = rexp.asStrings();
					}

				} catch (REXPMismatchException e) {

					e.printStackTrace();
				}
			}

			else {
				if (state == 1) {
					try {
						REXP rexp = RServeUtil.fromR("try(colnames(" + matDfName + "),silent=TRUE)");
						if (rexp.isNull() == false) {
							item = rexp.asStrings();
						}

					} catch (REXPMismatchException e) {

						e.printStackTrace();
					}
				} else if (state == 21) {
					try {
						REXP rexp = RServeUtil.fromR("try(rownames(" + matDfName + "),silent=TRUE)");
						if (rexp.isNull() == false) {
							item = rexp.asStrings();
						}

					} catch (REXPMismatchException e) {

						e.printStackTrace();
					}

				} else if (state == 22) {
					try {
						REXP rexp = RServeUtil.fromR("try(colnames(" + matDfName + "),silent=TRUE)");
						if (rexp.isNull() == false) {
							item = rexp.asStrings();
						}

					} catch (REXPMismatchException e) {

						e.printStackTrace();
					}

				}
			}
			// packages = RServeUtil.fromR("try(.bio7PkgsTemp[, \"Package\"])").asStrings();
			// title = RServeUtil.fromR("try(.bio7PkgsTemp[, \"Title\"])").asStrings();

			if (item != null) {
				/*
				 * If colnames, rownames applied on non existent object an error string will be
				 * returned which we exclude here!
				 */
				if (item[0].startsWith("Error") == false) {
					/* If text length after parenheses is at least 0! */
					if (length >= 0) {

						for (int i = 0; i < item.length; i++) {
							/*
							 * Here we filter out the templates by comparing the typed letters with the
							 * available templates!
							 */
							if (item[i].length() >= length && item[i].substring(0, length).equalsIgnoreCase(contentLastCorr)) {

								list.add(new ImageContentProposal("\"" + item[i] + "\"", item[i], item[i], item[i].length(), dataImage));
							}
						}

					}

					else {

						for (int j = 0; j < item.length; j++) {

							list.add(new ImageContentProposal("\"" + item[j] + "\"", item[j], item[j], item[j].length(), dataImage));

						}
					}

					propo = list.toArray(new ImageContentProposal[list.size()]);

					/* We have to convert the proposals to an ImageContentProposal! */
					// IContentProposal[] arrayTemp = makeProposalArray(array);
					list.clear();
					if (prop != null) {
						propo = (ImageContentProposal[]) ArrayUtils.addAll(propo, prop);
					} else {
						propo = null;
					}
				}
			}

		} else {
			System.out.println("No Rserve connection available!");
		}
		return propo;
	}

	/*
	 * Here we calculate the workspace variables and create ImageContentProposals!
	 */
	private ImageContentProposal[] getWorkSpaceVars(int offset) {
		propo = null;

		RConnection c = RServe.getConnection();
		if (c != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Display display = Util.getDisplay();
				display.syncExec(() -> {

					if (c != null) {
						try {
							String[] result = (String[]) c.eval("try(ls(),silent=TRUE)").asStrings();
							String[] varsWorkspaceClass = (String[]) c.eval("try(as.character(lapply(mget(ls()),class)))").asStrings();
							if (result != null && result.length > 0) {
								if (result[0].startsWith("Error") == false) {

									propo = new ImageContentProposal[result.length];

									for (int j = 0; j < result.length; j++) {

										propo[j] = new ImageContentProposal(result[j], result[j] + " - " + varsWorkspaceClass[j], result[j], result[j].length(), varImage);

									}
								}

							}
						} catch (RserveException | REXPMismatchException e) {
							// TODO Auto-generated catch block
							// e.printStackTrace();
							System.out.println("Error in R-Shell view code completion!\nR Message: " + e.getMessage());
						}
					}

				});
				RState.setBusy(false);
			} else {
				System.out.println("Rserve is busy!");
			}
		}
		return propo;
	}

	/*
	 * Here we calculate the :: package function and create ImageContentProposals!
	 */
	private ImageContentProposal[] namesPackageExportActivation(int offset, String prefix) {

		propo = null;
		// int length=prefix.length();
		// String lastIndex = prefix.substring(0, prefix.lastIndexOf(":"));
		String afterLastIndex = prefix.substring(prefix.lastIndexOf(":") + 1, prefix.length());
		int length = afterLastIndex.length();
		RConnection c = RServe.getConnection();
		if (c != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Display display = Util.getDisplay();
				display.syncExec(() -> {

					if (c != null) {
						ArrayList<IContentProposal> list = new ArrayList<IContentProposal>();
						String res = prefix.substring(0, prefix.lastIndexOf("::"));
						try {
							String[] result = (String[]) c.eval("try(grep(\"^[a-zA-Z]\",sort(getNamespaceExports(\"" + res + "\")),all,value=TRUE),silent=TRUE)").asStrings();
							if (result != null && result.length > 0) {
								if (result[0].startsWith("Error") == false) {

									// creatPopupS3Table(viewer, offSet, result);
									propo = new ImageContentProposal[result.length];

									for (int j = 0; j < result.length; j++) {

										if (result[j].length() >= length && result[j].substring(0, length).equalsIgnoreCase(afterLastIndex)) {

											list.add(new ImageContentProposal(result[j], result[j], result[j], result[j].length(), image));
										}

									}
									propo = list.toArray(new ImageContentProposal[list.size()]);

								}

							}
						} catch (RserveException | REXPMismatchException e) {
							// TODO Auto-generated catch block
							// e.printStackTrace();
							System.out.println("Error in R-Shell view code completion!\nR Message: " + e.getMessage());
						}
						list.clear();
					}

				});
				RState.setBusy(false);

			} else {
				System.out.println("Rserve is busy!");
			}
		}
		return propo;
	}

	/*
	 * Here we calculate the ::: package function and create ImageContentProposals!
	 */
	private ImageContentProposal[] namesPackageAllActivation(int offset, String prefix) {
		propo = null;
		String afterLastIndex = prefix.substring(prefix.lastIndexOf(":") + 1, prefix.length());
		int length = afterLastIndex.length();
		RConnection c = RServe.getConnection();
		if (c != null) {
			ArrayList<IContentProposal> list = new ArrayList<IContentProposal>();
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Display display = Util.getDisplay();
				display.syncExec(() -> {

					if (c != null) {
						String res = prefix.substring(0, prefix.lastIndexOf(":::"));
						try {
							String[] result = (String[]) c.eval("try(grep(\"^[a-zA-Z]\",ls(getNamespace(\"" + res + "\"), all.names=TRUE),value=TRUE),silent=TRUE)").asStrings();
							if (result != null && result.length > 0) {
								if (result[0].startsWith("Error") == false) {

									propo = new ImageContentProposal[result.length];

									for (int j = 0; j < result.length; j++) {

										/*
										 * String anyArg = (String)
										 * c.eval("try(capture.output(args("+prefix+result[j]+"),append = T))").asString
										 * (); if (anyArg != null) {
										 * 
										 * propo[j] = new ImageContentProposal(result[j], result[j], anyArg,
										 * result[j].length(), image); } else {
										 */

										if (result[j].length() >= length && result[j].substring(0, length).equalsIgnoreCase(afterLastIndex)) {

											list.add(new ImageContentProposal(result[j], result[j], result[j], result[j].length(), image));
										}

										// propo[j] = new ImageContentProposal(result[j], result[j], null,
										// result[j].length(), image);
										// }
									}
									propo = list.toArray(new ImageContentProposal[list.size()]);

								}

							}
						} catch (RserveException | REXPMismatchException e) {
							// TODO Auto-generated catch block
							// e.printStackTrace();
							System.out.println("Error in R-Shell view code completion!\nR Message: " + e.getMessage());
						}
					}

				});
				RState.setBusy(false);
			} else {
				System.out.println("Rserve is busy!");
			}
			list.clear();
		}
		return propo;
	}

	/* Here we calculate the s4 variables and create ImageContentProposals! */
	private ImageContentProposal[] s4Activation(int offset, String prefix) {
		propo = null;
		String afterLastIndex = prefix.substring(prefix.lastIndexOf("@") + 1, prefix.length());
		int length = afterLastIndex.length();
		ArrayList<IContentProposal> list = new ArrayList<IContentProposal>();
		RConnection c = RServe.getConnection();
		if (c != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Display display = Util.getDisplay();
				display.syncExec(() -> {

					if (c != null) {
						String res = prefix.substring(0, prefix.lastIndexOf("@"));
						try {
							String[] result = (String[]) c.eval("try(slotNames(" + res + "),silent=TRUE)").asStrings();
							if (result != null && result.length > 0) {
								if (result[0].startsWith("Error") == false) {

									// creatPopupS3Table(viewer, offSet, result);
									propo = new ImageContentProposal[result.length];

									/*
									 * for (int j = 0; j < result.length; j++) { String resultStr = (String)
									 * c.eval("try(capture.output(str(" + res + "@" + result[j] + ")))").asString();
									 * if (resultStr != null) { propo[j] = new ImageContentProposal(result[j],
									 * result[j], resultStr, result[j].length(), s4Image); } else { propo[j] = new
									 * ImageContentProposal(result[j], result[j], null, result[j].length(),
									 * s4Image); } }
									 */
									for (int j = 0; j < result.length; j++) {

										if (result[j].length() >= length && result[j].substring(0, length).equalsIgnoreCase(afterLastIndex)) {

											String resultStr = (String) c.eval("try(capture.output(str(" + res + "@" + result[j] + ")),silent=TRUE)").asString();
											if (resultStr != null) {
												list.add(new ImageContentProposal(result[j], result[j], resultStr, result[j].length(), s4Image));
											} else {
												list.add(new ImageContentProposal(result[j], result[j], null, result[j].length(), s4Image));
											}

										}

									}
									propo = list.toArray(new ImageContentProposal[list.size()]);
								}

							}
						} catch (RserveException | REXPMismatchException e) {
							// TODO Auto-generated catch block
							// e.printStackTrace();
							System.out.println("Error in R-Shell view code completion!\nR Message: " + e.getMessage());
						}
					}

				});
				RState.setBusy(false);
			} else {
				System.out.println("Rserve is busy!");
			}

		}

		else {
			// System.out.println("No Rserve connection available!");
		}
		list.clear();
		return propo;
	}

	/* Here we calculate the s3 variables and create ImageContentProposals! */
	private ImageContentProposal[] s3Activation(int offset, String prefix) {
		propo = null;
		String afterLastIndex = prefix.substring(prefix.lastIndexOf("$") + 1, prefix.length());
		int length = afterLastIndex.length();
		ArrayList<IContentProposal> list = new ArrayList<IContentProposal>();
		RConnection c = RServe.getConnection();
		if (c != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Display display = Util.getDisplay();
				display.syncExec(() -> {

					if (c != null) {
						try {
							String res = prefix.substring(0, prefix.lastIndexOf("$"));

							String[] result = (String[]) c.eval("try(ls(" + res + "),silent=TRUE)").asStrings();

							if (result != null && result.length > 0) {
								if (result[0].startsWith("Error") == false) {

									propo = new ImageContentProposal[result.length];
									for (int j = 0; j < result.length; j++) {

										if (result[j].length() >= length && result[j].substring(0, length).equalsIgnoreCase(afterLastIndex)) {

											String resultStr = (String) c.eval("try(capture.output(str(" + res + "$" + result[j] + ")))").asString();
											if (resultStr != null) {
												list.add(new ImageContentProposal(result[j], result[j], resultStr, result[j].length(), s3Image));
											} else {
												list.add(new ImageContentProposal(result[j], result[j], null, result[j].length(), s3Image));
											}

										}

									}
									propo = list.toArray(new ImageContentProposal[list.size()]);
								}

							}
						} catch (RserveException | REXPMismatchException e) {
							// TODO Auto-generated catch block
							// e.printStackTrace();
							System.out.println("Error in R-Shell view code completion!\nR Message: " + e.getMessage());
						}
					}

				});
				RState.setBusy(false);
			} else {
				System.out.println("Rserve is busy!");
			}
		}

		else {
			// System.out.println("No Rserve connection available!");
		}
		list.clear();
		return propo;
	}

	/*
	 * Here we calculate available dataset examples and create
	 * ImageContentProposals!
	 */
	private ImageContentProposal[] dataActivation(int offset, String contentLastCorr) {
		// IContentProposal[] array = null;
		int length = contentLastCorr.length();
		RConnection c = RServe.getConnection();
		if (c != null) {
			propo = null;
			ArrayList<IContentProposal> list = new ArrayList<IContentProposal>();

			String[] item = null;
			String[] packages = null;
			String[] title = null;
			/* Get all installed dataset names, their package and description! */
			try {
				RServeUtil.evalR("try(.bio7Pkgs <- setdiff(.packages(TRUE), c(\"base\", \"stats\")));" + "try(.bio7PkgsTemp<-data(package = .bio7Pkgs)$result);"
						+ "try(.bio7PkgsTemp<-.bio7PkgsTemp[order(.bio7PkgsTemp[,3]), ])", null);
				item = RServeUtil.fromR("try(.bio7PkgsTemp[, \"Item\"])").asStrings();
				packages = RServeUtil.fromR("try(.bio7PkgsTemp[, \"Package\"])").asStrings();
				title = RServeUtil.fromR("try(.bio7PkgsTemp[, \"Title\"])").asStrings();
			} catch (REXPMismatchException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			/* If text length after parenheses is at least 0! */
			if (length >= 0) {
				for (int i = 0; i < item.length; i++) {
					/*
					 * Here we filter out the templates by comparing the typed letters with the
					 * available templates!
					 */
					if (item[i].length() >= length && item[i].substring(0, length).equalsIgnoreCase(contentLastCorr)) {

						list.add(new ImageContentProposal(item[i], item[i] + " (package: " + packages[i] + ")", title[i], item[i].length(), dataImage));
					}
				}

			}

			else {

				for (int j = 0; j < item.length; j++) {

					list.add(new ImageContentProposal(item[j], item[j] + " (package: " + packages[j] + ")", title[j], item[j].length(), dataImage));

				}
			}

			propo = list.toArray(new ImageContentProposal[list.size()]);

			/* We have to convert the proposals to an ImageContentProposal! */
			// IContentProposal[] arrayTemp = makeProposalArray(array);
			list.clear();
		} else {
			System.out.println("No Rserve connection available!");
		}
		return propo;
	}

	/* Here we calculate available libraries and create ImageContentProposals! */
	private ImageContentProposal[] libraryActivation(int offset, String contentLastCorr) {
		RConnection c = RServe.getConnection();
		int length = contentLastCorr.length();
		if (c != null) {
			propo = null;
			ArrayList<IContentProposal> list = new ArrayList<IContentProposal>();
			String[] dirPackageFiles = null;
			String[] packageTitle = null;
			try {
				RServeUtil.evalR("try(.bio7ListOfWebPackages <- list(sort(.packages(all.available = TRUE))));" + "try(.bio7ListOfWebPackagesNames<-.bio7ListOfWebPackages[[1]]);"
						+ "try(.bio7TitleResult<-lapply(.bio7ListOfWebPackagesNames,packageDescription,fields = c(\"Title\")))", null);

				packageTitle = RServeUtil.fromR("try(as.character(.bio7TitleResult))").asStrings();
				dirPackageFiles = RServeUtil.fromR("try(.bio7ListOfWebPackagesNames)").asStrings();

			} catch (REXPMismatchException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			/* If text length after parenheses is at least 0! */
			if (length >= 0) {
				for (int i = 0; i < dirPackageFiles.length; i++) {
					/*
					 * Here we filter out the templates by comparing the typed letters with the
					 * available templates!
					 */
					if (dirPackageFiles[i].length() >= length && dirPackageFiles[i].substring(0, length).equalsIgnoreCase(contentLastCorr)) {

						list.add(new ImageContentProposal(dirPackageFiles[i], dirPackageFiles[i], packageTitle[i], dirPackageFiles[i].length(), libImage));
					}
				}

			}

			else {

				for (int j = 0; j < dirPackageFiles.length; j++) {

					list.add(new ImageContentProposal(dirPackageFiles[j], dirPackageFiles[j], packageTitle[j], dirPackageFiles[j].length(), libImage));

				}
			}

			propo = list.toArray(new ImageContentProposal[list.size()]);

			/* We have to convert the proposals to an ImageContentProposal! */
			// IContentProposal[] arrayTemp = makeProposalArray(array);
			list.clear();
		} else {
			System.out.println("No Rserve connection available!");
		}
		return propo;

		/*
		 * propo = new ImageContentProposal[dirPackageFiles.length];
		 * 
		 * for (int j = 0; j < dirPackageFiles.length; j++) {
		 * 
		 * propo[j] = new ImageContentProposal(dirPackageFiles[j], dirPackageFiles[j],
		 * packageTitle[j], dirPackageFiles[j].length(), libImage);
		 * 
		 * }
		 * 
		 * } else { System.out.println("No Rserve connection available!"); } return
		 * propo;
		 */
	}

	/* Here we display the function arguments from the default package functions! */
	private ImageContentProposal[] functionArgumentsActivation(int position, String func) {
		ImageContentProposal[] propo = null;
		for (int i = 0; i < statisticsSet.length; i++) {
			/* Do we have the method in the proposals? */

			if (func.equals(statistics[i])) {

				String calc = statisticsSet[i];

				/* Find the arguments in the template proposals! */
				int parOpen = calc.indexOf("(");
				int parClose = calc.lastIndexOf(")");

				/* Here we control the length. Must be greater -1! */
				if (parOpen + 1 + parClose >= 0) {
					calc = calc.substring(parOpen + 1, parClose);

					String[] proposalMethods = split(calc).toArray(new String[0]);

					propo = new ImageContentProposal[proposalMethods.length];

					for (int j = 0; j < proposalMethods.length; j++) {
						/*
						 * We add a suffix to mark this proposal for a scrolling to the arguments
						 * section in the description!
						 */
						propo[j] = new ImageContentProposal(proposalMethods[j], proposalMethods[j], func + "::::args::::", proposalMethods[j].length(), varFuncCallImage);

					}

					ImageContentProposal[] prop = getWorkSpaceVars(position);
					if (prop != null) {
						propo = (ImageContentProposal[]) ArrayUtils.addAll(propo, prop);
					}

				}
			}
		}

		return propo;

	}

	/*
	 * Answer and source from StackOverflow:
	 * http://stackoverflow.com/questions/34388828/java-splitting-a-comma-
	 * separated-string-but-ignoring-commas-in-parentheses/34389323#34389323 Author:
	 * Tagir Valeev Profile: http://stackoverflow.com/users/4856258/tagir-valeev
	 * 
	 * Adaptions to ignore comma n quotes!
	 */
	public List<String> split(String input2) {
		int nParens = 0;
		int start = 0;
		/*
		 * Temporary replace comma in quotes else the argument "," will be splitted!
		 */
		String tempReplacement = "$$null$$";
		String input = input2.replace("\",\"", tempReplacement);
		List<String> result = new ArrayList<>();
		for (int i = 0; i < input.length(); i++) {
			switch (input.charAt(i)) {
			case ',':
				if (nParens == 0) {
					/* Replace the temporary comma in quote replacement! */
					result.add(input.substring(start, i).replace(tempReplacement, "\",\""));
					start = i + 1;
				}
				break;
			case '(':
				nParens++;
				break;
			case ')':
				nParens--;
				if (nParens < 0)
					throw new IllegalArgumentException("Unbalanced parenthesis at offset #" + i);
				break;
			}
		}
		if (nParens > 0)
			throw new IllegalArgumentException("Missing closing parenthesis");
		result.add(input.substring(start).replace(tempReplacement, "\",\""));
		return result;
	}

}
