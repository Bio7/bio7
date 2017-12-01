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
 * Changed for Bio7
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
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Text;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RServeUtil;
import com.eco.bio7.rbridge.RShellView;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.antlr.Parse;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.rpreferences.template.CalculateRProposals;
import com.eco.bio7.util.Util;
import com.swtdesigner.ResourceManager;

public class ShellCompletion {
	private ContentProposalProvider contentProposalProvider;
	private ContentProposalAdapter contentProposalAdapter;
	private KeyStroke stroke;
	private static final String LCL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz.@$+-.:";
	private static final String UCL = LCL.toUpperCase();
	private static final String NUMS = "0123456789";
	private Image image = ResourceManager.getPluginImage(Bio7Plugin.getDefault(), "icons/brkp_obj.png");
	private Image varImage = ResourceManager.getPluginImage(Bio7Plugin.getDefault(), "icons/field_public_obj.png");
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
	// public boolean data;
	// public boolean library;

	/*
	 * Next two methods adapted from:
	 * https://krishnanmohan.wordpress.com/2011/12/12/eclipse-rcp-
	 * autocompletecombotext-control/
	 */
	static char[] getAutoactivationChars() {

		// To enable content proposal on deleting a char

		String delete = new String(new char[] { 8 });
		String allChars = LCL + UCL + NUMS + delete;
		return allChars.toCharArray();
	}

	static KeyStroke getActivationKeystroke() {
		KeyStroke instance = KeyStroke.getInstance(new Integer(SWT.CTRL).intValue(), new Integer(' ').intValue());
		return instance;
	}

	public ShellCompletion(RShellView view, Text control, final IControlContentAdapter controlContentAdapter) {
		this.view = view;
		this.control = control;
		contentProposalProvider = new ContentProposalProvider();
		contentProposalProvider.setFiltering(true);

		stroke = getActivationKeystroke();
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		boolean typedCodeCompletion = store.getBoolean("RSHELL_TYPED_CODE_COMPLETION");
		if (typedCodeCompletion) {
			contentProposalAdapter = new ContentProposalAdapter(control, controlContentAdapter, contentProposalProvider,
					stroke, getAutoactivationChars());
		} else {
			contentProposalAdapter = new ContentProposalAdapter(control, controlContentAdapter, contentProposalProvider,
					stroke, null);
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

				String content = control.getText();
				/*
				 * Weird behavior of text.getCaretPosition() position on MacOSX. Solved by
				 * extracting the a local var here!
				 */
				int caretPosition = control.getCaretPosition();
				control.setSelection(contentProposalProvider.lastIndex, caretPosition);
				// Point selection = control.getSelection();

				/*
				 * Insert the completion proposal in between selection start and selection end!
				 */
				if (s3 == true || s4 == true) {
					s3 = false;
					s4 = false;
					String textSel = control.getText(0, caretPosition - 1);
					String after = control.getText(caretPosition, control.getText().length());
					content = textSel + proposal.getContent() + after;
					int cursorPosition = (textSel + proposal.getContent()).length();
					control.setText(content);
					control.setSelection(cursorPosition);
				} else {
					// data = false;
					// library = false;
					int pos = calculateFirstOccurrenceOfChar(control, caretPosition);
					String textSel = control.getText(0, pos - 1);
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

		String tex = control.getText();
		if (i > tex.length())
			return 0;

		while (i > 0) {

			char ch = tex.charAt(i - 1);

			if ((ch == ';') || (ch == '(') || (ch == ',') || (ch == '[') || (ch == '=') || (ch == '-') || (ch == '+')
					|| Character.isSpaceChar(ch))
				break;
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

			// if (filterProposals) {
			ArrayList<IContentProposal> list = new ArrayList<IContentProposal>();
			ArrayList<IContentProposal> varWorkspace = new ArrayList<IContentProposal>();
			int offset = position;
			int lastIndex = calculateFirstOccurrenceOfChar(control, offset);
			int textLength = 0;

			String contentLast;
			if (lastIndex > 0) {
				textLength = offset - lastIndex;
				contentLast = control.getText(lastIndex, offset);

			} else {
				textLength = control.getText().length();
				contentLast = control.getText();
			}

			/* We need the substring here without a trailing char like ')'! */
			String contentLastCorr = control.getText(lastIndex, offset - 1);

			if (contentLastCorr.endsWith("@")) {
				s4 = true;
				return s4Activation(position, contentLastCorr);
			} else if (contentLastCorr.endsWith("$")) {
				s3 = true;
				return s3Activation(position, contentLastCorr);
			}
			//String textToOffset = control.getText(0, offset - 1);
			Parse parse=view.getParser();
			//System.out.println("Is: "+parse.isInFunctionCall());
           if(parse!=null&&parse.isInFunctionCall()) {
        	   String funcName=parse.getFuncName();
        	  // System.out.println(funcName);
			if (funcName.equals("data")) {
				// data = true;
				return dataActivation(position);
			} else if (funcName.equals("library") || parse.getFuncName().equals("require")) {
				// library = true;
				return libraryActivation(position);
			} else  {

				//int pos = calculateFirstOccurrenceOfChar(control, offset - 1);
				//String func = control.getText(pos, offset - 2);
				// System.out.println(control.getText(pos, offset - 2));
				return functionArgumentsActivation(position, funcName);
			}
           }

			if (RServe.isAlive()) {
				/* Here we get the R workspace vars! */
				ImageContentProposal[] workspaceVars = getWorkSpaceVars(position);
				if (workspaceVars != null) {
					for (int i = 0; i < workspaceVars.length; i++) {
						/*
						 * Here we filter out the vars by comparing the typed letters with the available
						 * workspace vars!
						 */
						if (workspaceVars[i].getLabel().length() >= textLength && workspaceVars[i].getLabel()
								.substring(0, textLength).equalsIgnoreCase(contentLastCorr)) {
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
					if (statistics[i].length() >= textLength
							&& statistics[i].substring(0, textLength).equalsIgnoreCase(contentLastCorr)) {
						list.add(makeContentProposal(statistics[i] + "()", statisticsContext[i], statisticsSet[i]));
					}
				}

			}
			/* If text length after parenheses is -1! */
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
			/* Concatenate both whith the Apache commons library! */
			IContentProposal[] allProposals = (IContentProposal[]) ArrayUtils.addAll(varWorkspaceArray, arrayTemp);
			return allProposals;
			// }
			/* If filtering is true! */
			/*
			 * if (contentProposals == null) { contentProposals = new
			 * IContentProposal[statistics.length];
			 * 
			 * for (int i = 0; i < statistics.length; i++) { contentProposals[i] =
			 * makeContentProposal(statistics[i], statisticsContext[i], statisticsSet[i]);
			 * 
			 * } }
			 */
			/*
			 * IContentProposal[] arrayFinal =makeProposalArray(contentProposals);
			 * IContentProposal[] both = (IContentProposal[])ArrayUtils.addAll(first,
			 * arrayFinal); Create an image proposal from it! return
			 * makeProposalArray(arrayFinal); // return contentProposals;
			 */ }

		private IContentProposal[] makeProposalArray(IContentProposal[] proposals) {
			if (proposals != null) {
				IContentProposal[] arrContentProposals = new IContentProposal[proposals.length];
				for (int i = 0; i < proposals.length; i++) {

					ImageContentProposal contentProposal = new ImageContentProposal(proposals[i].getContent(),
							proposals[i].getLabel(), proposals[i].getDescription(), proposals[i].getContent().length(),
							image);
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

		private IContentProposal makeContentProposal(final String proposal, final String label,
				final String description) {
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
							if (result != null && result.length > 0) {
								if (result[0].startsWith("Error") == false) {

									propo = new ImageContentProposal[result.length];

									for (int j = 0; j < result.length; j++) {

										propo[j] = new ImageContentProposal(result[j], result[j], result[j],
												result[j].length(), varImage);

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

	/* Here we calculate the s4 variables and create ImageContentProposals! */
	private ImageContentProposal[] s4Activation(int offset, String prefix) {
		propo = null;
		String res = prefix.replace("@", "");

		RConnection c = RServe.getConnection();
		if (c != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Display display = Util.getDisplay();
				display.syncExec(() -> {

					if (c != null) {
						try {
							String[] result = (String[]) c.eval("try(slotNames(" + res + "),silent=TRUE)").asStrings();
							if (result != null && result.length > 0) {
								if (result[0].startsWith("Error") == false) {

									// creatPopupS3Table(viewer, offSet, result);
									propo = new ImageContentProposal[result.length];

									for (int j = 0; j < result.length; j++) {

										propo[j] = new ImageContentProposal(result[j], result[j], null,
												result[j].length(), s4Image);

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

		else {
			System.out.println("No Rserve connection available!");
		}

		return propo;
	}

	/* Here we calculate the s3 variables and create ImageContentProposals! */
	private ImageContentProposal[] s3Activation(int offset, String prefix) {
		propo = null;
		String res = prefix.replace("$", "");
		RConnection c = RServe.getConnection();
		if (c != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Display display = Util.getDisplay();
				display.syncExec(() -> {

					if (c != null) {
						try {
							String[] result = (String[]) c.eval("try(ls(" + res + "),silent=TRUE)").asStrings();
							if (result != null && result.length > 0) {
								if (result[0].startsWith("Error") == false) {

									propo = new ImageContentProposal[result.length];

									for (int j = 0; j < result.length; j++) {

										propo[j] = new ImageContentProposal(result[j], result[j], null,
												result[j].length(), s3Image);

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

		else {
			System.out.println("No Rserve connection available!");
		}

		return propo;
	}

	/*
	 * Here we calculate available dataset examples and create
	 * ImageContentProposals!
	 */
	private ImageContentProposal[] dataActivation(int offset) {
		RConnection c = RServe.getConnection();
		if (c != null) {
			propo = null;

			String[] item = null;
			String[] packages = null;
			String[] title = null;
			/* Get all installed dataset names, their package and description! */
			try {
				RServeUtil.evalR("try(.bio7Pkgs <- setdiff(.packages(TRUE), c(\"base\", \"stats\")));"
						+ "try(.bio7PkgsTemp<-data(package = .bio7Pkgs)$result);"
						+ "try(.bio7PkgsTemp<-.bio7PkgsTemp[order(.bio7PkgsTemp[,3]), ])", null);
				item = RServeUtil.fromR("try(.bio7PkgsTemp[, \"Item\"])").asStrings();
				packages = RServeUtil.fromR("try(.bio7PkgsTemp[, \"Package\"])").asStrings();
				title = RServeUtil.fromR("try(.bio7PkgsTemp[, \"Title\"])").asStrings();
			} catch (REXPMismatchException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			propo = new ImageContentProposal[item.length];

			for (int j = 0; j < item.length; j++) {

				propo[j] = new ImageContentProposal(item[j], item[j] + " (package: " + packages[j] + ")", title[j],
						item[j].length(), dataImage);

			}

		} else {
			System.out.println("No Rserve connection available!");
		}
		return propo;
	}

	/* Here we calculate available libraries and create ImageContentProposals! */
	private ImageContentProposal[] libraryActivation(int offset) {
		RConnection c = RServe.getConnection();
		if (c != null) {
			propo = null;

			String[] dirPackageFiles = null;
			String[] packageTitle = null;
			try {
				RServeUtil.evalR("try(.bio7ListOfWebPackages <- list(sort(.packages(all.available = TRUE))));"
						+ "try(.bio7ListOfWebPackagesNames<-.bio7ListOfWebPackages[[1]]);"
						+ "try(.bio7TitleResult<-lapply(.bio7ListOfWebPackagesNames,packageDescription,fields = c(\"Title\")))",
						null);

				packageTitle = RServeUtil.fromR("try(as.character(.bio7TitleResult))").asStrings();
				dirPackageFiles = RServeUtil.fromR("try(.bio7ListOfWebPackagesNames)").asStrings();

			} catch (REXPMismatchException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			propo = new ImageContentProposal[dirPackageFiles.length];

			for (int j = 0; j < dirPackageFiles.length; j++) {

				propo[j] = new ImageContentProposal(dirPackageFiles[j], dirPackageFiles[j], packageTitle[j],
						dirPackageFiles[j].length(), libImage);

			}

		} else {
			System.out.println("No Rserve connection available!");
		}
		return propo;
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
						propo[j] = new ImageContentProposal(proposalMethods[j], proposalMethods[j],
								func + "::::args::::", proposalMethods[j].length(), varFuncCallImage);

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
