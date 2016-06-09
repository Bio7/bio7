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
 *
 *
 *
 *
 *
 *
 */

package com.eco.bio7.rbridge.completion;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Text;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.rpreferences.template.CalculateRProposals;
import com.swtdesigner.ResourceManager;

public class ShellCompletion {
	private ContentProposalProvider contentProposalProvider;
	private ContentProposalAdapter contentProposalAdapter;
	private KeyStroke stroke;
	private static final String LCL = "abcdefghijklmnopqrstuvwxyz";
	private static final String UCL = LCL.toUpperCase();
	private static final String NUMS = "0123456789";
	private Image image = ResourceManager.getPluginImage(Bio7Plugin.getDefault(), "icons/template_obj.png");
	private Text control;
	private String[] statistics;
	private String[] statisticsContext;
	private String[] statisticsSet;

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

	public ShellCompletion(Text control, final IControlContentAdapter controlContentAdapter) {
		this.control = control;
		contentProposalProvider = new ContentProposalProvider();
		contentProposalProvider.setFiltering(true);

		stroke = getActivationKeystroke();

		contentProposalAdapter = new ContentProposalAdapter(control, controlContentAdapter, contentProposalProvider, stroke, null);
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
				control.setSelection(contentProposalProvider.lastIndex + 1, control.getCaretPosition());
				Point selection = control.getSelection();

				/*
				 * Insert the completion proposal in between selection start and
				 * selection end!
				 */
				content = content.substring(0, selection.x) + proposal.getContent() + "()" + content.substring(selection.y, content.length());
				/* Calculate the cursor position after inserting parentheses! */
				int cursorPosition = (content.substring(0, selection.x) + proposal.getContent() + "()").length() - 1;
				control.setText(content);
				control.setSelection(cursorPosition);

			}

		});

	}

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
								 * Function loaded at Rserve startup. Writes the
								 * available functions to a file!
								 */
								c.eval(".bio7WriteFunctionDef();");
							} catch (RserveException e) {

								e.printStackTrace();
							}

							/*
							 * Reload the code proposals (not the templates) for
							 * the R editor!
							 */
							CalculateRProposals.setStartupTemplate(false);
							CalculateRProposals.loadRCodePackageTemplates();
							/* Load the created proposals! */
							statistics = CalculateRProposals.getStatistics();
							statisticsContext = CalculateRProposals.getStatisticsContext();
							statisticsSet = CalculateRProposals.getStatisticsSet();

							/**/
							// contentProposalProvider = new
							// ContentProposalProvider();
							// contentProposalAdapter.setContentProposalProvider(contentProposalProvider);

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

	public class ContentProposalProvider implements IContentProposalProvider {

		private IContentProposal[] contentProposals;
		private boolean filterProposals = true;
		public int lastIndex;

		public ContentProposalProvider() {
			super();
			/*
			 * At startup load the default R proposals and add them to the
			 * templates!
			 */

			CalculateRProposals.loadRCodePackageTemplates();

			/* Load the created proposals! */
			statistics = CalculateRProposals.getStatistics();
			statisticsContext = CalculateRProposals.getStatisticsContext();
			statisticsSet = CalculateRProposals.getStatisticsSet();
		}

		public IContentProposal[] getProposals(String contents, int position) {

			if (filterProposals) {
				ArrayList<IContentProposal> list = new ArrayList<IContentProposal>();
				int offset = control.getCaretPosition();
				int textLength = 0;// control.getText().length();
				String tex = control.getText(0, offset);
				lastIndex = tex.lastIndexOf('(');

				String contentLast;
				if (lastIndex > 0) {
					textLength = offset - lastIndex - 1;
					contentLast = control.getText(lastIndex + 1, offset - 1);
					// System.out.println("last Index:"+ lastIndex+"
					// "+contentLast+" tex length: "+textLength);
					// contentLast=contentLastTemp.replace("(", "");

				} else {
					textLength = control.getText().length();
					contentLast = control.getText();
				}

				/* If text length after parenheses is at least 0! */
				if (textLength >= 0) {
					for (int i = 0; i < statistics.length; i++) {

						if (statistics[i].length() >= textLength && statistics[i].substring(0, textLength).equalsIgnoreCase(contentLast)) {
							list.add(makeContentProposal(statistics[i], statisticsContext[i], statisticsSet[i]));
						}
					}
				}
				/* If text length after parenheses is -1! */
				else {
					for (int i = 0; i < statistics.length; i++) {

						list.add(makeContentProposal(statistics[i], statisticsContext[i], statisticsSet[i]));

					}
				}
				return makeProposalArray((IContentProposal[]) list.toArray(new IContentProposal[list.size()]));
			}
			/* If filtering is true! */
			if (contentProposals == null) {
				contentProposals = new IContentProposal[statistics.length];

				for (int i = 0; i < statistics.length; i++) {
					contentProposals[i] = makeContentProposal(statistics[i], statisticsContext[i], statisticsSet[i]);

				}
			}
			/* Create an image proposal from it! */
			return makeProposalArray(contentProposals);
			// return contentProposals;
		}

		private IContentProposal[] makeProposalArray(IContentProposal[] proposals) {
			if (proposals != null) {
				IContentProposal[] arrContentProposals = new IContentProposal[proposals.length];
				for (int i = 0; i < proposals.length; i++) {

					ImageContentProposal contentProposal = new ImageContentProposal(proposals[i].getContent(), proposals[i].getLabel(), proposals[i].getDescription(), proposals[i].getContent().length(), image);
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

}
