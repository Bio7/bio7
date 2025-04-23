package com.eco.bio7.rpreferences.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessorExtension;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.antlr.Parse;
import com.eco.bio7.reditor.antlr.ref.RRefPhaseListen;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.reditors.TemplateEditorUI;
import com.eco.bio7.util.Util;

/**
 * A completion processor for R templates.
 */
public class RCompletionProcessor extends TemplateCompletionProcessor implements IContentAssistProcessorExtension {

	private static final Comparator fgProposalComparator = new ProposalComparator();

	private static final String DEFAULT_IMAGE = "$nl$/icons/template_obj.png"; //$NON-NLS-1$

	private static final String CALCULATED_TEMPLATE_IMAGE = "$nl$/icons/brkp_obj.png"; //$NON-NLS-1$

	private static final String FIELD_IMAGE = "$nl$/icons/methpub_obj.png"; //$NON-NLS-1$

	private static final String FIELD_PRIVATE_IMAGE = "$nl$/icons/field_private_obj.png"; //$NON-NLS-1$

	private static final String METHOD_IMAGE = "$nl$/icons/field_public_obj.png"; //$NON-NLS-1$

	// private Image imageArgs =
	// Bio7REditorPlugin.getImageDescriptor("/icons/template_obj.png").createImage();
	private Image imageVariablesWorkspace = Bio7REditorPlugin.getImageDescriptor("/icons/types.png").createImage();
	private Image imageVariablesEditor = Bio7REditorPlugin.getImageDescriptor("/icons/field_public_obj.png")
			.createImage();
	private Image imageFunctionsEditor = Bio7REditorPlugin.getImageDescriptor("/icons/methpub_obj.png").createImage();
	private Image s4Image = Bio7REditorPlugin.getImageDescriptor("/icons/s4.png").createImage();
	private Image s3Image = Bio7REditorPlugin.getImageDescriptor("/icons/s3.png").createImage();
	private Image varImage = Bio7REditorPlugin.getImageDescriptor("/icons/varfunccall.png").createImage();
	private Image libImage = Bio7REditorPlugin.getImageDescriptor("/icons/package_obj.png").createImage();
	private Image dataImage = Bio7REditorPlugin.getImageDescriptor("/icons/settings_obj.png").createImage();
	private Image arrayImage = Bio7REditorPlugin.getImageDescriptor("/icons/goto_input.png").createImage();

	private int count = 0;// Variable to count the listed template.

	private int defaultTemplatesLength;// Global variable to get the current
										// template amount.

	public static String[] getStatistics() {
		return statistics;
	}

	public static void setStatistics(String[] statistics) {
		RCompletionProcessor.statistics = statistics;
	}

	public static String[] getStatisticsContext() {
		return statisticsContext;
	}

	public static void setStatisticsContext(String[] statisticsContext) {
		RCompletionProcessor.statisticsContext = statisticsContext;
	}

	public static String[] getStatisticsSet() {
		return statisticsSet;
	}

	public static void setStatisticsSet(String[] statisticsSet) {
		RCompletionProcessor.statisticsSet = statisticsSet;
	}

	private IPreferenceStore store;

	private DefaultToolTip tooltip;

	private REditor editor;

	private String[] splitBuffScopedFun;

	private String[] splitVars;

	private boolean writeAllTemplateArguments = false;
	/* Completion for Function arguments, library and data, etc.! */
	private CompletionProposal[] propo;

	CompletionProposal[] proposalsMatDFVec = null;

	private String[] varsWorkspace;

	private String[] varsWorkspaceClass;

	private boolean isInPipedFunction;

	private String pipedDataName;

	private String[] pipeArguments;

	private String[] pipeArgumentContext;

	private static String[] statistics;
	private static String[] statisticsContext;
	private static String[] statisticsSet;

	public RCompletionProcessor(REditor rEditor, ContentAssistant assistant) {
		this.editor = rEditor;
		/*
		 * At startup load the default R proposals and add them to the templates!
		 */
		CalculateRProposals.loadRCodePackageTemplates();
		/* Load the created proposals! */
		statistics = CalculateRProposals.getStatistics();
		statisticsContext = CalculateRProposals.getStatisticsContext();
		statisticsSet = CalculateRProposals.getStatisticsSet();

		store = Bio7REditorPlugin.getDefault().getPreferenceStore();

	}

	public DefaultToolTip getTooltip() {
		return tooltip;
	}

	/**
	 * Cut out angular brackets for relevance sorting, since the template name does
	 * not contain the brackets.
	 * 
	 * @param template the template
	 * @param prefix   the prefix
	 * @return the relevance of the <code>template</code> for the given
	 *         <code>prefix</code>
	 */
	protected int getRelevance(Template template, String prefix) {

		if (template.getName().startsWith(prefix) || template.getName().toLowerCase().startsWith(prefix))
			// if (template.getName().toLowerCase().startsWith(prefix))
			return 90;
		return 0;
	}

	private static final class ProposalComparator implements Comparator<Object> {
		public int compare(Object o1, Object o2) {
			return ((TemplateProposal) o2).getRelevance() - ((TemplateProposal) o1).getRelevance();
		}
	}

	public boolean isCompletionProposalAutoActivation(char c, ITextViewer viewer, int offset) {
		if (store.getBoolean("TYPED_CODE_COMPLETION")) {
			/*
			 * Check that we don't trigger the action with special characters, e.g., with a
			 * ',' in a function call!
			 */
			if (!Character.isJavaIdentifierPart(c) && (c == '.') == false && (c == '@') == false
					&& (c == ':') == false) {
				return false;
			}
			/*
			 * Prefix here is one character less than in the computeCompletionProposals
			 * calculation! Maybe triggered before the character is set?
			 */
			String prefix = extractPrefix(viewer, offset);

			int activateKeyNrTyped = store.getInt("ACTIVATION_AMOUNT_CHAR_COMPLETION");
			if (prefix.length() + 1 == activateKeyNrTyped) {
				return true;
			}

		}
		return false;
	}

	@Override
	public boolean isContextInformationAutoActivation(char c, ITextViewer viewer, int offset) {
		// TODO Auto-generated method stub
		return false;
	}

	@SuppressWarnings("unchecked")
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		count = 0;

		ITextSelection selection = (ITextSelection) viewer.getSelectionProvider().getSelection();

		// Adjust offset to end of normalized selection
		if (selection.getOffset() == offset)
			offset = selection.getOffset() + selection.getLength();
		/*
		 * Returns the word if not a bracket, etc. occurs. Exceptions can be added to
		 * the method!
		 */
		String prefix = extractPrefix(viewer, offset);
		int leng = prefix.length();
		Region region = null;
		/*
		 * We parse the code form the invoked offset to detect if we are in function
		 * calls, etc.!
		 */
		RRefPhaseListen ref = new Parse(editor).parseFromOffset(offset);
		/*
		 * Avoid empty icons in code completion (split returns an array of size 1) from
		 * editor defined functions and variables!
		 */
		StringBuffer buffScopedFunctions = ref.getBuffScopeFunctions();
		if (buffScopedFunctions.length() == 0) {
			splitBuffScopedFun = new String[0];
		} else {
			splitBuffScopedFun = buffScopedFunctions.toString().split(",");
		}

		StringBuffer buffScopedVars = ref.getBuffScopeVars();
		if (buffScopedVars.length() == 0) {
			splitVars = new String[0];
		} else {
			splitVars = buffScopedVars.toString().split(",");
		}

		boolean isInMethodCall = ref.isInMethodCall();
		/* Next variables for the matrix argument detection! */
		boolean isInMatrixBracketCall = ref.isInMatrixBracketCall();

		boolean isInMatrixDoubleBracketCall = ref.isInMatrixDoubleBracketCall();

		String matDfName = ref.getBracketMatrixName();

		int state = ref.getMatrixArgState();

		int bracketCommaCount = ref.getBracketCommaCount();

		String proposalNameFound = ref.getProposalFuncFound();

		StringBuffer resultMethodCallVars = ref.getMethodCallVars();

		isInPipedFunction = ref.isInPipeFunction();

		pipedDataName = ref.getCurrentPipeData();

		/*
		 * In parentheses we show an popup instead of the completion dialog! We return
		 * null to avoid the opening of the template dialog!
		 */

		// int activateKeyNrTyped = store.getInt("ACTIVATION_AMOUNT_CHAR_COMPLETION");
		String charBeforeCursor = "";
		// String charAfter = "";
		/* Extract the word before the cursor to detect $, @! */
		if (offset > 0) {
			try {
				charBeforeCursor = viewer.getDocument().get(offset - 1, 1);
			} catch (BadLocationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		/* Check if we have S3 attributes! */
		if (prefix.contains("@") || prefix.contains("$")) {
			int x = prefix.indexOf("@");
			int y = prefix.indexOf("$");

			if (x > y) {

				return s4Activation(offset, prefix);

				/* Check if we have S4 attributes! */
			} else {
				return s3Activation(offset, prefix);

			}
		}

		else if (prefix.contains(":::")) {
			return namesPackageAllActivation(offset, prefix);

		}

		else if (prefix.contains("::")) {

			return namesPackageExportActivation(offset, prefix);

		} else if (prefix.contains(":")) {
			/*
			 * If we have only one colon we shorten the prefix to occurrence to get code
			 * completion for e.g., 1:length() or similar expressions!
			 */
			int index = prefix.indexOf(":");
			prefix = prefix.substring(index + 1, prefix.length());

		}
		/* Control if we are in a matrix '[]' call! */
		if (isInMatrixBracketCall) {

			if (leng <= 0) {

				return matrixArgumentProposals(offset, prefix, matDfName, state, bracketCommaCount, false,
						buffScopedVars, buffScopedFunctions);

			}
			 else if (leng > 0) {

			/* After which amount of chars we open the code completion automatically! */
			// if (leng >= activateKeyNrTyped) {

			 region = new Region(offset - prefix.length(), prefix.length());

			 }
			// }
			else {
				return new ICompletionProposal[0];
			}
		}
		/* Control if we are in a matrix '[[]]' call! */
		if (isInMatrixDoubleBracketCall) {
			if (leng <= 0) {

				return matrixArgumentProposals(offset, prefix, matDfName, state, bracketCommaCount, true,
						buffScopedVars, buffScopedFunctions);

				// if call has two arguments, cursor on the left argument!

			}

			 else if (leng > 0) {

			/* After which amount of chars we open the code completion automatically! */
			// if (leng >= activateKeyNrTyped) {

			 region = new Region(offset - prefix.length(), prefix.length());

			 }
			// }
			else {
				return new ICompletionProposal[0];
			}

		}

		/* If we are in a method call! */
		if (isInMethodCall) {
			propo = null;
			/* Amount of chars after '('. extracted prefix string is 0 after bracket! */

			if (leng <= 0) {

				if (proposalNameFound != null) {
					/* We want to open and filter data(), library() and require()! */
					if (proposalNameFound.equals("data") || proposalNameFound.equals("library")
							|| proposalNameFound.equals("require")) {
						propo = libraryDataProposals(viewer, offset, prefix, proposalNameFound, buffScopedVars,
								buffScopedFunctions);
						return propo;
					}

					/*
					 * If we have have default packages methods. We check in the method for special
					 * functions (library, etc.)!
					 */
					propo = packageFunctionArgumentProposals(viewer, offset, leng, proposalNameFound, buffScopedVars,
							buffScopedFunctions);
				} else {
					/*
					 * If we have call proposals from scope defined variables and functions!
					 */
					propo = editorFunctionArgumentProposals(viewer, offset, leng, ref, resultMethodCallVars,
							buffScopedVars, buffScopedFunctions);

				}
				return propo;

				/* Activate after amount of keys (auto activation)! */
			}
			/* We want to open and filter data(), library() and require()! */
			else if (leng > 0) {
				if (proposalNameFound != null) {

					if (proposalNameFound.equals("data") || proposalNameFound.equals("library")
							|| proposalNameFound.equals("require")) {
						propo = libraryDataProposals(viewer, offset, prefix, proposalNameFound, buffScopedVars,
								buffScopedFunctions);
						return propo;
					}
				}
				/* After which amount of chars we open the code completion automatically! */
				// if (leng >= activateKeyNrTyped) {

				// region = new Region(offset - prefix.length(), prefix.length());

				// }
			}
			/*
			 * 
			 * else { return new ICompletionProposal[0]; }
			 */
		}

		/*
		 * else { if (leng >= activateKeyNrTyped) { region = new Region(offset -
		 * prefix.length(), prefix.length()); } else { return new
		 * ICompletionProposal[0]; }
		 * 
		 * }
		 */

		region = new Region(offset - prefix.length(), prefix.length());

		TemplateContext context = createContext(viewer, region);
		if (context == null)
			return new ICompletionProposal[0];
		/* Name of the selection variables {line, word}_selection! */
		context.setVariable("selection", selection.getText());

		Template[] templates = getTemplates(context.getContextType().getId());
		defaultTemplatesLength = templates.length;

		List<ICompletionProposal> matches = new ArrayList<ICompletionProposal>();
		for (int i = 0; i < templates.length; i++) {
			Template template = templates[i];
			try {
				context.getContextType().validate(template.getPattern());
			} catch (TemplateException e) {
				continue;
			}
			if (template.matches(prefix, context.getContextType().getId())) {
				matches.add(createProposal(template, context, (IRegion) region, getRelevance(template, prefix)));
			}

		}

		/* The current workspace variables! */
		getWorkSpaceVars();

		Template[] tempLocalWorkspaceVars = new Template[varsWorkspace.length];
		for (int i = 0; i < varsWorkspace.length; i++) {
			tempLocalWorkspaceVars[i] = new Template(varsWorkspace[i], varsWorkspaceClass[i],
					context.getContextType().getId(), varsWorkspace[i], true);

			Template template = tempLocalWorkspaceVars[i];
			try {
				context.getContextType().validate(template.getPattern());
			} catch (TemplateException e) {
				continue;
			}
			if (template.matches(prefix, context.getContextType().getId()))
				matches.add(createProposal(template, context, (IRegion) region, getRelevance(template, prefix)));

		}

		/* Here the default templates with editor defined variables and functions! */
		/* Proposals from local defined variables! */

		if (splitVars.length > 0) {
			Template[] tempLocalVars = new Template[splitVars.length];

			for (int i = 0; i < tempLocalVars.length; i++) {
				tempLocalVars[i] = new Template(splitVars[i], "editor variable", context.getContextType().getId(),
						splitVars[i], true);

				Template template = tempLocalVars[i];
				try {
					context.getContextType().validate(template.getPattern());
				} catch (TemplateException e) {
					continue;
				}
				if (template.matches(prefix, context.getContextType().getId()))
					matches.add(createProposal(template, context, (IRegion) region, getRelevance(template, prefix)));

			}
		}
		/* Proposals from local defined functions! */
		// if(triggerNext){
		if (splitBuffScopedFun.length > 0) {
			Template[] tempLocalFunctions = new Template[splitBuffScopedFun.length];

			for (int i = 0; i < tempLocalFunctions.length; i++) {
				tempLocalFunctions[i] = new Template(splitBuffScopedFun[i], "editor function",
						context.getContextType().getId(), splitBuffScopedFun[i] + "(${cursor})", true);

				Template template = tempLocalFunctions[i];
				try {
					context.getContextType().validate(template.getPattern());
				} catch (TemplateException e) {
					continue;
				}
				if (template.matches(prefix, context.getContextType().getId()))
					matches.add(createProposal(template, context, (IRegion) region, getRelevance(template, prefix)));

			}

		}

		/* Proposals from loaded R function List! */

		Template[] temp = new Template[statistics.length];

		for (int i = 0; i < temp.length; i++) {
			if (writeAllTemplateArguments) {
				temp[i] = new Template(statistics[i], statisticsContext[i], context.getContextType().getId(),
						statisticsSet[i], true);
			} else {
				temp[i] = new Template(statistics[i], statisticsContext[i], context.getContextType().getId(),
						statistics[i] + "(${cursor})", true);
			}
			Template template = temp[i];
			try {
				context.getContextType().validate(template.getPattern());
			} catch (TemplateException e) {
				continue;
			}
			if (template.matches(prefix, context.getContextType().getId()))
				matches.add(createProposal(template, context, (IRegion) region, getRelevance(template, prefix)));
		}

		// ICompletionProposal[] varsWorkspace = getWorkSpaceVars(offset, prefix);

		Collections.sort(matches, fgProposalComparator);

		ICompletionProposal[] pro = (ICompletionProposal[]) matches.toArray(new ICompletionProposal[matches.size()]);
		// ICompletionProposal[] allProposals = (ICompletionProposal[])
		// ArrayUtils.addAll(pro, varsWorkspace);
		// triggerNext = true;

		return pro;

	}

	/*
	 * Here we get the workspace variables!
	 */
	private void getWorkSpaceVars() {
		propo = null;

		// int length = prefix.length();
		RConnection c = REditor.getRserveConnection();
		if (c != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Display display = Util.getDisplay();
				display.syncExec(() -> {

					if (c != null) {
						// ArrayList<CompletionProposal> list = new ArrayList<CompletionProposal>();
						try {
							varsWorkspace = (String[]) c.eval("try(ls(),silent=TRUE)").asStrings();
							varsWorkspaceClass = (String[]) c.eval("try(as.character(lapply(mget(ls()),class)))")
									.asStrings();

							if (isInPipedFunction) {

								REXP rexp = null;
								/* We accept dataframes! */
								rexp = c.eval("try(if (is.data.frame(" + pipedDataName + ")){colnames(" + pipedDataName
										+ ")} ,silent=TRUE)");

								if (rexp.isNull() == false) {

									pipeArguments = rexp.asStrings();
									if (pipeArguments[0].startsWith("Error") == false) {
										pipeArgumentContext = new String[pipeArguments.length];
										for (int i = 0; i < pipeArguments.length; i++) {

											pipeArgumentContext[i] = pipedDataName;

										}

										varsWorkspace = ArrayUtils.addAll(pipeArguments, varsWorkspace);
										varsWorkspaceClass = ArrayUtils.addAll(pipeArgumentContext, varsWorkspaceClass);
									}

								}

							}

						} catch (RserveException | REXPMismatchException e) {
							// TODO Auto-generated catch block
							// e.printStackTrace();
							System.out.println("Error in R-Shell view code completion!\nR Message: " + e.getMessage());
						}

						// list.clear();
					}

				});
				RState.setBusy(false);
			} else {
				System.out.println("Rserve is busy!");
			}
		} else {
			// System.out.println("No Rserve connection available!");
		}
		/*
		 * If no workspace variables are available we return an empty String array thus
		 * nothing will be added (size 0) to the templates!
		 */
		if (varsWorkspace == null) {
			varsWorkspace = new String[0];
			varsWorkspaceClass = new String[0];
		}

	}

	/* Here we calculate the attribute list after a '::' operator! */
	private ICompletionProposal[] namesPackageExportActivation(int offset, String prefix) {
		propo = null;
		final int offSet = offset;
		String afterLastIndex = prefix.substring(prefix.lastIndexOf(":") + 1, prefix.length());
		int length = afterLastIndex.length();
		RConnection c = REditor.getRserveConnection();
		if (c != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Display display = Util.getDisplay();
				display.syncExec(() -> {

					if (c != null) {
						ArrayList<CompletionProposal> list = new ArrayList<CompletionProposal>();
						String res = prefix.substring(0, prefix.lastIndexOf("::"));
						try {
							String[] result = (String[]) c.eval("try(grep(\"^[a-zA-Z]\",sort(getNamespaceExports(\""
									+ res + "\")),all,value=TRUE),silent=TRUE)").asStrings();
							if (result != null && result.length > 0) {
								if (result[0].startsWith("Error") == false) {

									propo = new CompletionProposal[result.length];

									for (int j = 0; j < result.length; j++) {

										if (result[j].length() >= length
												&& result[j].substring(0, length).equalsIgnoreCase(afterLastIndex)) {

											list.add(new CompletionProposal(result[j], offSet - length, length,
													result[j].length(), imageFunctionsEditor, result[j], null,
													result[j]));
										}

									}

									propo = list.toArray(new CompletionProposal[list.size()]);
								}

							}
						} catch (RserveException | REXPMismatchException e) {

							System.out.println(e.getMessage());

						}
						list.clear();
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

	/* Here we calculate the attribute list after a ':::' operator! */
	private ICompletionProposal[] namesPackageAllActivation(int offset, String prefix) {
		propo = null;
		final int offSet = offset;
		String afterLastIndex = prefix.substring(prefix.lastIndexOf(":") + 1, prefix.length());
		int length = afterLastIndex.length();
		ArrayList<CompletionProposal> list = new ArrayList<CompletionProposal>();
		RConnection c = REditor.getRserveConnection();
		if (c != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Display display = Util.getDisplay();
				display.syncExec(() -> {

					if (c != null) {
						String res = prefix.substring(0, prefix.lastIndexOf(":::"));
						try {
							String[] result = (String[]) c.eval("try(grep(\"^[a-zA-Z]\",ls(getNamespace(\"" + res
									+ "\"), all.names=TRUE),value=TRUE),silent=TRUE)").asStrings();
							if (result != null && result.length > 0) {
								if (result[0].startsWith("Error") == false) {

									propo = new CompletionProposal[result.length];
									for (int j = 0; j < result.length; j++) {

										if (result[j].length() >= length
												&& result[j].substring(0, length).equalsIgnoreCase(afterLastIndex)) {

											list.add(new CompletionProposal(result[j], offSet - length, length,
													result[j].length(), imageFunctionsEditor, result[j], null,
													result[j]));
										}

									}

									propo = list.toArray(new CompletionProposal[list.size()]);
								}

							}
						} catch (RserveException | REXPMismatchException e) {
							// TODO Auto-generated catch block
							System.out.println(e.getMessage());
							// e.printStackTrace();
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
		list.clear();
		return propo;
	}

	/* Here we calculate the S4 attribute list after a '@' operator! */
	private ICompletionProposal[] s4Activation(int offset, String prefix) {
		propo = null;
		final int offSet = offset;
		String afterLastIndex = prefix.substring(prefix.lastIndexOf("@") + 1, prefix.length());
		int length = afterLastIndex.length();
		ArrayList<CompletionProposal> list = new ArrayList<CompletionProposal>();
		RConnection c = REditor.getRserveConnection();
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
									propo = new CompletionProposal[result.length];
									for (int j = 0; j < result.length; j++) {

										if (result[j].length() >= length
												&& result[j].substring(0, length).equalsIgnoreCase(afterLastIndex)) {

											String resultStr = (String) c.eval("try(capture.output(str(" + res + "@"
													+ result[j] + ")),silent=TRUE)").asString();
											if (resultStr != null) {

												list.add(new CompletionProposal(result[j], offSet - length, length,
														result[j].length(), s4Image, result[j], null, resultStr));
											} else {

												list.add(new CompletionProposal(result[j], offSet - length, length,
														result[j].length(), s4Image, result[j], null, result[j]));
											}

										}

									}

									propo = list.toArray(new CompletionProposal[list.size()]);

								}

							}
						} catch (RserveException | REXPMismatchException e) {
							// TODO Auto-generated catch block
							System.out.println(e.getMessage());
							// e.printStackTrace();
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
		list.clear();
		return propo;
	}

	/* Here we calculate the S3 attribute list after a '$' operator! */
	private ICompletionProposal[] s3Activation(int offset, String prefix) {
		propo = null;
		final int offSet = offset;
		String afterLastIndex = prefix.substring(prefix.lastIndexOf("$") + 1, prefix.length());
		int length = afterLastIndex.length();
		ArrayList<CompletionProposal> list = new ArrayList<CompletionProposal>();
		RConnection c = REditor.getRserveConnection();
		if (c != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Display display = Util.getDisplay();
				display.syncExec(() -> {

					if (c != null) {
						String res = prefix.substring(0, prefix.lastIndexOf("$"));
						try {
							String[] result = (String[]) c.eval("try(ls(" + res + "),silent=TRUE)").asStrings();
							if (result != null && result.length > 0) {
								if (result[0].startsWith("Error") == false) {
									// creatPopupS3Table(viewer, offSet, result);
									propo = new CompletionProposal[result.length];

									for (int j = 0; j < result.length; j++) {

										if (result[j].length() >= length
												&& result[j].substring(0, length).equalsIgnoreCase(afterLastIndex)) {

											String resultStr = (String) c.eval("try(capture.output(str(" + res + "$"
													+ result[j] + ")),silent=TRUE)").asString();
											if (resultStr != null) {
												list.add(new CompletionProposal(result[j], offSet - length, length,
														result[j].length(), s3Image, result[j], null, resultStr));
											} else {
												list.add(new CompletionProposal(result[j], offSet - length, length,
														result[j].length(), s3Image, result[j], null, result[j]));
											}

										}

									}

									propo = list.toArray(new CompletionProposal[list.size()]);

								}

							}
						} catch (RserveException | REXPMismatchException e) {
							// TODO Auto-generated catch block
							System.out.println(e.getMessage());
							// e.printStackTrace();
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
		list.clear();
		return propo;
	}

	/*
	 * Here we calculate the proposals for library(), require() and data() when
	 * invoked inside a function call!
	 */
	private CompletionProposal[] libraryDataProposals(ITextViewer viewer, int offset, String prefix,
			String funcNameFromProposals, StringBuffer resultBuffScopeVars, StringBuffer buffScopedFunctions) {

		if (funcNameFromProposals != null) {

			if (funcNameFromProposals.equals("library") || funcNameFromProposals.equals("require")) {

				RConnection c = REditor.getRserveConnection();
				if (c != null) {
					if (RState.isBusy() == false) {
						RState.setBusy(true);
						Display display = Util.getDisplay();
						display.syncExec(() -> {

							if (c != null) {
								String[] dirPackageFiles = null;
								String[] packageTitle = null;
								try {
									c.eval("try(.bio7ListOfWebPackages <- list(sort(.packages(all.available = TRUE))))");

									c.eval("try(.bio7ListOfWebPackagesNames<-.bio7ListOfWebPackages[[1]])");
									c.eval("try(.bio7TitleResult<-lapply(.bio7ListOfWebPackagesNames,packageDescription,fields = c(\"Title\")))");
									packageTitle = c.eval("try(as.character(.bio7TitleResult))").asStrings();

									dirPackageFiles = c.eval("try(.bio7ListOfWebPackagesNames)").asStrings();

								} catch (RserveException e2) {

									e2.printStackTrace();
								} catch (REXPMismatchException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								ArrayList<CompletionProposal> list = new ArrayList<CompletionProposal>();
								int length = prefix.length();
								if (length >= 0) {
									for (int i = 0; i < dirPackageFiles.length; i++) {

										/*
										 * Here we filter out the templates by comparing the typed letters with the
										 * available templates!
										 */

										if (dirPackageFiles[i].length() >= length
												&& dirPackageFiles[i].substring(0, length).equalsIgnoreCase(prefix)) {

											list.add(new CompletionProposal(dirPackageFiles[i], offset - length, length,
													dirPackageFiles[i].length(), libImage, dirPackageFiles[i], null,
													packageTitle[i]));
										}
									}

								}

								propo = list.toArray(new CompletionProposal[list.size()]);

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

			}

			else if (funcNameFromProposals.equals("data")) {

				RConnection c = REditor.getRserveConnection();
				if (c != null) {
					if (RState.isBusy() == false) {
						RState.setBusy(true);
						Display display = Util.getDisplay();
						display.syncExec(() -> {

							if (c != null) {

								String[] item = null;
								String[] packages = null;
								String[] title = null;
								/* Get all installed dataset names, their package and description! */
								try {
									c.eval("try(.bio7Pkgs <- setdiff(.packages(TRUE), c(\"base\", \"stats\")))");
									c.eval("try(.bio7PkgsTemp<-data(package = .bio7Pkgs)$result)");
									c.eval("try(.bio7PkgsTemp<-.bio7PkgsTemp[order(.bio7PkgsTemp[,3]), ])");
									item = c.eval("try(.bio7PkgsTemp[, \"Item\"])").asStrings();
									packages = c.eval("try(.bio7PkgsTemp[, \"Package\"])").asStrings();
									title = c.eval("try(.bio7PkgsTemp[, \"Title\"])").asStrings();
								} catch (RserveException | REXPMismatchException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

								ArrayList<CompletionProposal> list = new ArrayList<CompletionProposal>();
								int length = prefix.length();
								if (length >= 0) {
									for (int i = 0; i < item.length; i++) {

										/*
										 * Here we filter out the templates by comparing the typed letters with the
										 * available templates!
										 */

										if (item[i].length() >= length
												&& item[i].substring(0, length).equalsIgnoreCase(prefix)) {

											list.add(new CompletionProposal(item[i], offset - length, length,
													item[i].length(), dataImage,
													item[i] + " (package: " + packages[i] + ")", null, title[i]));
										}
									}

								}

								propo = list.toArray(new CompletionProposal[list.size()]);

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

			}
		}
		return propo;
	}

	private ICompletionProposal[] matrixArgumentProposals(int offset, String prefix, String matDfName, int state,
			int bracketCommaCount, boolean doubleMatrixCall, StringBuffer resultBuffScopeVars,
			StringBuffer buffScopedFunctions) {
		int length = prefix.length();
		/* The current workspace variables! */
		getWorkSpaceVars();
		RConnection con = REditor.getRserveConnection();
		if (con != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Display display = Util.getDisplay();
				display.syncExec(() -> {

					if (con != null) {

						String[] item = null;
						/* If we have a '[[' expression! */
						if (doubleMatrixCall) {
							/* If we have only one argument! */
							if (bracketCommaCount == 0) {
								try {
									REXP rexp = null;
									/* Here we leave out matrices and arrays (matrices are arrays)! */
									try {
										rexp = con.eval("try(if (is.data.frame(" + matDfName + ")){colnames("
												+ matDfName + ")} else if (is.list(" + matDfName + ")) {names("
												+ matDfName + ")} else if (is.vector(" + matDfName + ")) {names("
												+ matDfName + ")} ,silent=TRUE)");
									} catch (RserveException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									if (rexp.isNull() == false) {
										item = rexp.asStrings();

									}

								} catch (REXPMismatchException e) {

									e.printStackTrace();
								}
								/* If we have two arguments with one comma! */
							} else if (bracketCommaCount == 1) {

							}
						}
						/* If we have a '[' expression! */
						else {
							/* If we have only one argument! */
							if (bracketCommaCount == 0) {
								try {
									REXP rexp = null;
									try {
										rexp = con.eval("try(if (is.data.frame(" + matDfName + ")||is.matrix("
												+ matDfName + ")){colnames(" + matDfName + ")} else if(is.array("
												+ matDfName + ")){rownames(" + matDfName + ")} else{names(" + matDfName
												+ ")} ,silent=TRUE)");
									} catch (RserveException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									if (rexp.isNull() == false) {
										item = rexp.asStrings();

									}

								} catch (REXPMismatchException e) {

									e.printStackTrace();
								}
								/* If we have two arguments with one comma! */
							} else if (bracketCommaCount == 1) {
								/* Left argument from nearest offset calculation!! */
								if (state == 0) {
									try {
										REXP rexp = null;
										try {
											rexp = con.eval("try(rownames(" + matDfName + "),silent=TRUE)");
										} catch (RserveException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										if (rexp.isNull() == false) {
											item = rexp.asStrings();
										}

									} catch (REXPMismatchException e) {

										e.printStackTrace();
									}
									/* Right argument from nearest offset calculation! */
								} else if (state == 1) {
									try {
										REXP rexp = null;
										try {
											rexp = con.eval("try(colnames(" + matDfName + "),silent=TRUE)");
										} catch (RserveException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										if (rexp.isNull() == false) {
											item = rexp.asStrings();
										}

									} catch (REXPMismatchException e) {

										e.printStackTrace();
									}
								}
								/*
								 * If we have more then 2-dimensions we expect an array! Before the array
								 * arguments are give with row- and colnames function!
								 */
							} else {
								try {
									REXP rexp = null;
									try {

										rexp = con.eval(
												"try(dimnames(" + matDfName + ")[[" + (state + 1) + "]],silent=TRUE)");
									} catch (RserveException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									if (rexp.isNull() == false) {
										item = rexp.asStrings();
									}

								} catch (REXPMismatchException e) {

									e.printStackTrace();
								}

							}
						}

						if (item != null) {
							/* We want no error message in the code completion and filter it out here! */
							if (item[0].startsWith("Error") == false) {
								String[] scopedVars;
								String[] scopedFunctions;
								/*
								 * Avoid empty icons in code completion (split returns an array of size 1) from
								 * editor defined functions and variables!
								 */
								if (resultBuffScopeVars.length() == 0) {
									scopedVars = new String[0];
								} else {
									scopedVars = resultBuffScopeVars.toString().split(",");
								}
								if (buffScopedFunctions.length() == 0) {
									scopedFunctions = new String[0];
								} else {
									scopedFunctions = buffScopedFunctions.toString().split(",");
								}

								int len1 = item.length;// The extracted arguments length
								int len2 = item.length + varsWorkspace.length;// Extracted arguments + the workspace
																				// variables length
								int len3 = item.length + varsWorkspace.length + scopedFunctions.length;// Extracted
																										// arguments +
																										// the workspace
																										// variables +
																										// the editor
																										// defined
																										// functions
																										// length
								int allProposals = len3 + scopedVars.length;// Extracted arguments + the workspace
																			// variables + the editor defined functions
																			// length + the editor defined variables

								propo = new CompletionProposal[allProposals];

								/*
								 * String resultStr = null; try { resultStr = (String)
								 * con.eval("try(capture.output(str(" + matDfName +
								 * ")),silent=TRUE)").asString(); } catch (RserveException |
								 * REXPMismatchException e) { // TODO Auto-generated catch block
								 * e.printStackTrace(); }
								 */

								/* Get the object information as context info! */
								// String resultStr = new RStrObjectInformation().getRStrObjectInfo(matDfName,
								// con);

								for (int j = 0; j < allProposals; j++) {
									if (j < item.length) {

										if (matDfName != null) {
											// The extracted arguments
											propo[j] = new CompletionProposal("\"" + item[j] + "\"", offset, 0,
													item[j].length() + 2, arrayImage, item[j], null, matDfName);
										} else {
											propo[j] = new CompletionProposal("\"" + item[j] + "\"", offset, 0,
													item[j].length() + 2, arrayImage, null, null, null);
										}

									} else if (j < len2) {
										// The workspace variables
										propo[j] = new CompletionProposal(varsWorkspace[j - len1], offset, 0,
												varsWorkspace[j - len1].length(), imageVariablesWorkspace,
												varsWorkspace[j - len1] + " - " + varsWorkspaceClass[j - len1], null,
												varsWorkspace[j - len1]);
									} else if (j < len3) {
										// The defined editor functions
										propo[j] = new CompletionProposal(scopedFunctions[j - len2], offset, 0,
												scopedFunctions[j - len2].length(), imageFunctionsEditor,
												scopedFunctions[j - len2], null, null);
									} else {
										// The defined editor variables
										propo[j] = new CompletionProposal(scopedVars[j - len3], offset, 0,
												scopedVars[j - len3].length(), imageVariablesEditor,
												scopedVars[j - len3], null, null);

									}
								}

							}
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
	 * Method to open argument suggestions, vars and functions from self defined
	 * functions!
	 */
	private CompletionProposal[] editorFunctionArgumentProposals(ITextViewer viewer, int offset, int leng,
			RRefPhaseListen ref, StringBuffer resultmethodCallVars, StringBuffer resultBuffScopeVars,
			StringBuffer buffScopedFunctions) {

		if (resultmethodCallVars != null && resultmethodCallVars.length() > 0) {

			/* The current workspace variables! */
			getWorkSpaceVars();

			String[] scopedVars;
			String[] scopedFunctions;
			/*
			 * Avoid empty icons in code completion (split returns an array of size 1) from
			 * editor defined functions and variables!
			 */
			if (resultBuffScopeVars.length() == 0) {
				scopedVars = new String[0];
			} else {
				scopedVars = resultBuffScopeVars.toString().split(",");
			}
			if (buffScopedFunctions.length() == 0) {
				scopedFunctions = new String[0];
			} else {
				scopedFunctions = buffScopedFunctions.toString().split(",");
			}
			String[] resultMethodCallVars = resultmethodCallVars.toString().split(",");

			int len1 = resultMethodCallVars.length;// The extracted arguments length
			int len2 = resultMethodCallVars.length + varsWorkspace.length;// Extracted arguments + the workspace
																			// variables length
			int len3 = resultMethodCallVars.length + varsWorkspace.length + scopedFunctions.length;// Extracted
																									// arguments + the
																									// workspace
																									// variables + the
																									// editor defined
																									// functions
																									// length
			int allProposals = len3 + scopedVars.length;// Extracted arguments + the workspace variables + the editor
														// defined functions
														// length + the editor defined variables

			propo = new CompletionProposal[allProposals];

			for (int j = 0; j < allProposals; j++) {
				if (j < resultMethodCallVars.length) {
					// The extracted arguments
					propo[j] = new CompletionProposal(resultMethodCallVars[j], offset, 0,
							resultMethodCallVars[j].length(), varImage, resultMethodCallVars[j], null, null);

				} else if (j < len2) {
					// The workspace variables
					propo[j] = new CompletionProposal(varsWorkspace[j - len1], offset, 0,
							varsWorkspace[j - len1].length(), imageVariablesWorkspace,
							varsWorkspace[j - len1] + " - " + varsWorkspaceClass[j - len1], null,
							varsWorkspace[j - len1]);
				} else if (j < len3) {
					// The defined editor functions
					propo[j] = new CompletionProposal(scopedFunctions[j - len2], offset, 0,
							scopedFunctions[j - len2].length(), imageFunctionsEditor,
							scopedFunctions[j - len2] + " - editor function", null, null);
				} else {
					// The defined editor variables
					propo[j] = new CompletionProposal(scopedVars[j - len3], offset, 0, scopedVars[j - len3].length(),
							imageVariablesEditor, scopedVars[j - len3] + " - editor variable", null, null);

				}
			}

		}
		return propo;
	}

	/* The general functions arguments invoked when inside a function! */
	private CompletionProposal[] packageFunctionArgumentProposals(ITextViewer viewer, int offset, int leng,
			String funcNameFromProposals, StringBuffer resultBuffScopeVars, StringBuffer buffScopedFunctions) {

		if (funcNameFromProposals != null) {

			/* The current workspace variables! */
			getWorkSpaceVars();

			for (int i = 0; i < statisticsSet.length; i++) {
				/* Do we have the method in the proposals? */
				if (funcNameFromProposals.equals(statistics[i])) {

					String calc = statisticsSet[i];

					/* Find the arguments in the template proposals! */
					int parOpen = calc.indexOf("(");
					int parClose = calc.lastIndexOf(")");

					/* Here we control the length. Must be greater -1! */
					if (parOpen + 1 + parClose >= 0) {
						calc = calc.substring(parOpen + 1, parClose);
						String[] scopedVars;
						String[] scopedFunctions;
						String[] proposalMethods = split(calc).toArray(new String[0]);
						/*
						 * Avoid empty icons in code completion (split returns an array of size 1) from
						 * editor defined functions and variables!
						 */
						if (resultBuffScopeVars.length() == 0) {
							scopedVars = new String[0];
						} else {
							scopedVars = resultBuffScopeVars.toString().split(",");
						}
						if (buffScopedFunctions.length() == 0) {
							scopedFunctions = new String[0];
						} else {
							scopedFunctions = buffScopedFunctions.toString().split(",");
						}

						int len1 = proposalMethods.length;// The extracted arguments
						int len2 = proposalMethods.length + varsWorkspace.length;// Extracted arguments + the workspace
																					// variables
						int len3 = proposalMethods.length + varsWorkspace.length + scopedFunctions.length;// Extracted
																											// arguments
																											// + the
																											// workspace
																											// variables
																											// + the
																											// editor
																											// defined
																											// functions
						int allProposals = len3 + scopedVars.length;// Extracted arguments + the workspace variables +
																	// the editor defined functions
																	// length + the editor defined variables

						propo = new CompletionProposal[allProposals];

						for (int j = 0; j < allProposals; j++) {

							if (j < proposalMethods.length) {
								// The extracted arguments
								propo[j] = new CompletionProposal(proposalMethods[j], offset, 0,
										proposalMethods[j].length(), varImage, proposalMethods[j], null,
										statistics[i] + "::::args::::");
								// The workspace variables
							} else if (j < len2) {
								/* If we are in a pipe! */
								if (pipedDataName != null) {
									/* Here we get the str object of the dataframe if in a pipe available! */
									if (pipedDataName.equals(varsWorkspaceClass[j - len1])) {
										propo[j] = new CompletionProposal(
												varsWorkspace[j - len1], offset, 0, varsWorkspace[j - len1].length(),
												imageVariablesWorkspace, varsWorkspace[j - len1] + " - "
														+ varsWorkspaceClass[j - len1] + " column",
												null, varsWorkspaceClass[j - len1]);
									} else {
										propo[j] = new CompletionProposal(varsWorkspace[j - len1], offset, 0,
												varsWorkspace[j - len1].length(), imageVariablesWorkspace,
												varsWorkspace[j - len1] + " - " + varsWorkspaceClass[j - len1], null,
												varsWorkspace[j - len1]);
									}
								} else {
									propo[j] = new CompletionProposal(varsWorkspace[j - len1], offset, 0,
											varsWorkspace[j - len1].length(), imageVariablesWorkspace,
											varsWorkspace[j - len1] + " - " + varsWorkspaceClass[j - len1], null,
											varsWorkspace[j - len1]);
								}
								// The defined editor functions
							} else if (j < len3) {
								propo[j] = new CompletionProposal(scopedFunctions[j - len2], offset, 0,
										scopedFunctions[j - len2].length(), imageFunctionsEditor,
										scopedFunctions[j - len2] + " - editor function", null, null);
							} else {
								// The defined editor variables
								propo[j] = new CompletionProposal(scopedVars[j - len3], offset, 0,
										scopedVars[j - len3].length(), imageVariablesEditor,
										scopedVars[j - len3] + " - editor variable", null, null);

							}
						}

					}
				}

			}

		}

		return propo;
	}

	/*
	 * private void creatPopupTable(ITextViewer viewer, int offset, String[]
	 * proposalMethod, String[] scopedVars, String[] scopedFunctions) { StyledText
	 * te = viewer.getTextWidget(); Font f = te.getFont();
	 * 
	 * int height = f.getFontData()[0].getHeight();
	 * 
	 * StyledText sh = viewer.getTextWidget();
	 * 
	 * Point poi = sh.getLocationAtOffset(offset); poi = sh.toDisplay(poi); int locx
	 * = poi.x; int locy = poi.y; Util.getDisplay().asyncExec(new Runnable() {
	 * public void run() {
	 * 
	 * RPopupTable listPopup = new RPopupTable(viewer.getTextWidget().getShell());
	 * editor.setRPopupShell(listPopup.getShell());
	 * 
	 * listPopup.setFont(f); listPopup.setItems(proposalMethod, "arguments");
	 * 
	 * if (scopedVars.length == 1 && scopedVars[0].isEmpty()) {
	 * 
	 * } else { listPopup.setItems(scopedVars, "variables"); }
	 * 
	 * if (scopedFunctions.length == 1 && scopedFunctions[0].isEmpty()) {
	 * 
	 * } else { listPopup.setItems(scopedFunctions, "functions"); }
	 * 
	 * Rectangle rect = new Rectangle(locx, locy - height - 150, 300, 200); String
	 * selected = listPopup.open(rect); if (selected != null) { try {
	 * viewer.getDocument().replace(offset, 0, selected);
	 * editor.getSelectionProvider().setSelection(new TextSelection(offset +
	 * selected.length(), 0)); } catch (BadLocationException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); }
	 * 
	 * } } }); }
	 * 
	 * private void creatPopupS3Table(ITextViewer viewer, int offset, String[]
	 * scopedVars) { StyledText te = viewer.getTextWidget(); Font f = te.getFont();
	 * 
	 * int height = f.getFontData()[0].getHeight();
	 * 
	 * StyledText sh = viewer.getTextWidget();
	 * 
	 * Point poi = sh.getLocationAtOffset(offset); poi = sh.toDisplay(poi); int locx
	 * = poi.x; int locy = poi.y; Util.getDisplay().asyncExec(new Runnable() {
	 * public void run() {
	 * 
	 * RPopupTable listPopup = new RPopupTable(viewer.getTextWidget().getShell());
	 * editor.setRPopupShell(listPopup.getShell());
	 * 
	 * listPopup.setFont(f);
	 * 
	 * listPopup.setItems(scopedVars, "variables");
	 * 
	 * Rectangle rect = new Rectangle(locx, locy - height - 150, 300, 200); String
	 * selected = listPopup.open(rect); if (selected != null) { try {
	 * viewer.getDocument().replace(offset, 0, selected);
	 * editor.getSelectionProvider().setSelection(new TextSelection(offset +
	 * selected.length(), 0)); } catch (BadLocationException e) { // TODO
	 * Auto-generated catch block e.printStackTrace(); }
	 * 
	 * } } }); }
	 */
	/**
	 * Simply return all templates.
	 * 
	 * @param contextTypeId the context type, ignored in this implementation
	 * @return all templates
	 */
	protected Template[] getTemplates(String contextTypeId) {
		return TemplateEditorUI.getDefault().getTemplateStore().getTemplates();
	}

	// add the chars for Completion here !!!

	public char[] getCompletionProposalAutoActivationCharacters() {

		if (store.getBoolean("TYPED_CODE_COMPLETION")) {
			String ac = store.getString("ACTIVATION_CHARS");
			// return "abcdefghijklmnopqrstuvwxyz".toCharArray();
			if (ac == null || ac.isEmpty()) {

				return null;
			}
			return ac.toCharArray();
		}

		return null;

	}

	/* Extend prefixes for R functions with a dot, e.g. t.test() */
	protected String extractPrefix(ITextViewer viewer, int offset) {
		int i = offset;
		IDocument document = viewer.getDocument();
		if (i > document.getLength())
			return "";

		try {
			while (i > 0) {
				char ch = document.getChar(i - 1);
				/*
				 * We need to extra include the '@' character for S4 class vars!
				 */
				if (!Character.isJavaIdentifierPart(ch) && (ch == '.') == false && (ch == '@') == false
						&& (ch == ':') == false)
					break;
				i--;
			}

			return document.get(i, offset - i);
		} catch (BadLocationException e) {
			return "";
		}
	}

	/**
	 * Return the R context type that is supported by this plug-in.
	 * 
	 * @param viewer the viewer, ignored in this implementation
	 * @param region the region, ignored in this implementation
	 * @return the supported R context type
	 */
	protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
		return TemplateEditorUI.getDefault().getContextTypeRegistry().getContextType(RContextType.XML_CONTEXT_TYPE);
	}

	/**
	 * Always return the default image.
	 * 
	 * @param template the template, ignored in this implementation
	 * @return the default template image
	 */
	protected Image getImage(Template template) {

		if (count < defaultTemplatesLength) {
			count++;
			ImageRegistry registry = TemplateEditorUI.getDefault().getImageRegistry();
			Image image = registry.get(DEFAULT_IMAGE);
			if (image == null) {
				ImageDescriptor desc = TemplateEditorUI.imageDescriptorFromPlugin("com.eco.bio7.redit", DEFAULT_IMAGE); //$NON-NLS-1$
				registry.put(DEFAULT_IMAGE, desc);
				image = registry.get(DEFAULT_IMAGE);
			}
			return image;

		} else if (count >= defaultTemplatesLength && count < defaultTemplatesLength + varsWorkspace.length) {
			count++;
			return imageVariablesWorkspace;
		}

		else if (count >= defaultTemplatesLength
				&& count < defaultTemplatesLength + splitVars.length + varsWorkspace.length) {
			count++;
			return imageVariablesEditor;
		}

		else if (count >= defaultTemplatesLength + splitVars.length && count < defaultTemplatesLength
				+ splitBuffScopedFun.length + splitVars.length + varsWorkspace.length) {
			count++;
			ImageRegistry registry = TemplateEditorUI.getDefault().getImageRegistry();
			Image image = registry.get(FIELD_IMAGE);
			if (image == null) {
				ImageDescriptor desc = TemplateEditorUI.imageDescriptorFromPlugin("com.eco.bio7.redit", FIELD_IMAGE);
				registry.put(FIELD_IMAGE, desc);
				image = registry.get(FIELD_IMAGE);
			}
			return image;
		}

		else {

			ImageRegistry registry = TemplateEditorUI.getDefault().getImageRegistry();
			Image image = registry.get(CALCULATED_TEMPLATE_IMAGE);
			if (image == null) {
				ImageDescriptor desc = TemplateEditorUI.imageDescriptorFromPlugin("com.eco.bio7.redit",
						CALCULATED_TEMPLATE_IMAGE);
				registry.put(CALCULATED_TEMPLATE_IMAGE, desc);
				image = registry.get(CALCULATED_TEMPLATE_IMAGE);
			}
			return image;
		}

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

	public void disposeImages() {

		imageVariablesEditor.dispose();
		imageFunctionsEditor.dispose();
		s4Image.dispose();
		s3Image.dispose();
		varImage.dispose();
		libImage.dispose();
		dataImage.dispose();

	}

}
