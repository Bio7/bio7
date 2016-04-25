package com.eco.bio7.reditor.code;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.reditor.antlr.Parse;
import com.eco.bio7.reditor.antlr.RBaseListen;
import com.eco.bio7.reditor.antlr.RErrorStrategy;
import com.eco.bio7.reditor.antlr.RFilter;
import com.eco.bio7.reditor.antlr.RLexer;
import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditor.antlr.UnderlineListener;
import com.eco.bio7.reditor.antlr.ref.RRefPhaseListen;
import com.eco.bio7.reditors.REditor;

import javafx.util.Pair;

public class RQuickFixSolutions {
	private ISourceViewer viewer;
	private REditor editor;

	public RQuickFixSolutions(ISourceViewer viewer, REditor rEditor) {
		this.viewer = viewer;
		this.editor = rEditor;
	}

	/*
	 * Hardcoded. We could also refactor out the solutions to a properties file!
	 */
	public ICompletionProposal[] getProposals(String errorCode, int offset, int endChar, ICompletionProposal[] prop, String tokenText) {
		if (errorCode != null) {

			switch (errorCode) {
			case "Err1":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'","Remove the additional parentheses", offset, endChar, "", 1) };
				break;
			case "Err2":

				break;
			case "Err3":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'","Remove the additional parentheses", offset, endChar, "", 1) };
				break;
			case "Err4":

				break;
			case "Err5":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'","Remove the additional parentheses", offset, endChar, "", 1) };
				break;
			case "Err6":

				break;
			case "Err7":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'","Remove the additional parentheses", offset, endChar, "", 1) };
				break;
			case "Err8":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ']'","Remove the additional parentheses", offset, endChar, "", 1) };

				break;
			case "Err9":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ']'","Remove the additional parentheses", offset, endChar, "", 1) };
				break;
			case "Err10":

				break;
			case "Err11":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove '}'","Remove the additional parentheses", offset, endChar, "", 1) };
				break;
			case "Warn12":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace 'true' with 'TRUE'","Remove the wrong boolean assignment", offset, endChar, "TRUE", 4) };

				break;
			case "Warn13":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace 'false' with 'FALSE'","Remove the wrong boolean assignment", offset, endChar, "FALSE", 5) };

				break;
			case "Warn14":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace 'null' with 'NULL'","Remove the wrong assignment", offset, endChar, "NULL", 4) };

				break;
			case "Warn15":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace 'na' with 'NA'", "Remove the wrong assignment",offset, endChar, "NA", 2) };

				break;
			case "Warn16":

				IDocument doc = viewer.getDocument();
				RRefPhaseListen ref = new Parse(editor).parseFromOffset(offset);
				StringBuffer buffScopedFunctions = ref.getBuffScopeFunctions();
				String[] splitBuffScopedFun = buffScopedFunctions.toString().split(",");

				List<Pair<String, Double>> words = new ArrayList<Pair<String, Double>>();

				for (int i = 0; i < splitBuffScopedFun.length; i++) {
					/* Calculate the LevenShtein distance! */
					double dist = Levenshtein.getLevenshteinDistance(tokenText, splitBuffScopedFun[i]);
					/* Calculate as percent! */
					double max = Math.max(tokenText.length(), splitBuffScopedFun[i].length());
					double perct = round(1.0 - (dist / max), 2);
					/* Add to a Pair to sort the distances! */
					words.add(new Pair<String, Double>(splitBuffScopedFun[i], perct));
				}
				/* Sort the Levenshtein distances in descending order! */
				words.sort(new Comparator<Pair<String, Double>>() {
					@Override
					public int compare(Pair<String, Double> levPair1, Pair<String, Double> levPair2) {
						if (levPair1.getValue() > levPair2.getValue()) {
							return -1;
						} else if (levPair1.getValue().equals(levPair2.getValue())) {
							return 0;
						} else {
							return 1;
						}
					}
				});

				prop = new ICompletionProposal[words.size() + 1];
				/* First proposal to create a function! */
				prop[0] = new RQuickFixCompletionProposal("Create function","Create a new function", offset, endChar, System.lineSeparator() + tokenText + "<-function(){}" + System.lineSeparator(), 0);
				/*
				 * The following proposals all sorted functions from
				 * Levenshstein distances!
				 */
				for (int i = 0; i < words.size(); i++) {
					if (words.get(i) != null) {
						Pair<String, Double> sugOrdered = words.get(i);

						prop[i + 1] = new RQuickFixCompletionProposal("Functions available:" + " '" + sugOrdered.getKey() + "' Similarity:" + " " + sugOrdered.getValue(),null, offset, endChar, sugOrdered.getKey(), endChar - offset);
					} else {
						prop[i + 1] = new RQuickFixCompletionProposal("Functions available: NA",null, offset, endChar, "NA", 2);
					}
				}

			case "Warn17":
				/*
				 * prop = new ICompletionProposal[] {
				 * 
				 * new RQuickFixCompletionProposal("Create function", offset,
				 * endChar, System.lineSeparator()+tokenText+"<-function(){}"+
				 * System.lineSeparator(), 0) };
				 */

				break;
			case "Err16":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'","Remove the additional parentheses", offset, endChar, "", 1) };

				break;
			case "Err17":

				break;
			case "Err18":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'","Remove the additional parentheses", offset, endChar, "", 1) };
				break;
			case "Err19":

				break;
			case "Err20":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace '=>' with '>='","Replace the wrong comparison operator", offset, endChar, ">=", 2) };
				break;
			case "Err21":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace '=<' with '<='","Replace the wrong comparison operator", offset, endChar, "<=", 2) };
				break;
			case "Err22":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove unknown token","Remove unknown token", offset, endChar, "", 1) };
				break;
			case "Err23":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove function variable","Remove the wrong function call parameter", offset, endChar, "", endChar-offset) };

			default:
				break;
			}

		}
		return prop;
	}

	/*
	 * From:
	 * http://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-
	 * places?lq=1
	 */
	public static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

}