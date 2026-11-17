package com.eco.bio7.reditor.code;

import com.eco.bio7.rbridge.REvaluator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditor.antlr.Parse;
import com.eco.bio7.reditor.antlr.ref.RRefPhaseListen;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.rpreferences.template.CalculateRProposals;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import com.eco.bio7.rffm.RValue;
import com.eco.bio7.rffm.RException;
import com.eco.bio7.rffm.RTypeException;


public class RQuickFixSolutions {
	private ISourceViewer viewer;
	private REditor editor;
	private String[] packages;

	public RQuickFixSolutions(ISourceViewer viewer, REditor rEditor) {
		this.viewer = viewer;
		this.editor = rEditor;
	}

	/*
	 * Hardcoded. We could also refactor out the solutions to a properties file!
	 */
	public ICompletionProposal[] getProposals(String errorCode, int offset, int endChar, ICompletionProposal[] prop, String tokenText, String replacement) {
		if (errorCode != null) {

			switch (errorCode) {
			case "Err1":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'", "Remove the additional parentheses", offset, endChar, "", 1, null, null) };
				break;
			case "Err2":

				break;
			case "Err3":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'", "Remove the additional parentheses", offset, endChar, "", 1, null, null) };
				break;
			case "Err4":

				break;
			case "Err5":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'", "Remove the additional parentheses", offset, endChar, "", 1, null, null) };
				break;
			case "Err6":

				break;
			case "Err7":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'", "Remove the additional parentheses", offset, endChar, "", 1, null, null) };
				break;
			case "Err8":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ']'", "Remove the additional parentheses", offset, endChar, "", 1, null, null) };

				break;
			case "Err9":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ']'", "Remove the additional parentheses", offset, endChar, "", 1, null, null) };
				break;
			case "Err10":

				break;
			case "Err11":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove '}'", "Remove the additional parentheses", offset, endChar, "", 1, null, null) };
				break;
			/* To many args in function call! */
			case "Err12":

				break;
			case "Err16":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'", "Remove the additional parentheses", offset, endChar, "", 1, null, null) };

				break;
			case "Err17":

				break;
			case "Err18":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'", "Remove the additional parentheses", offset, endChar, "", 1, null, null) };
				break;
			case "Err19":

				break;
			case "Err20":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace '=>' with '>='", "Replace the wrong comparison operator", offset, endChar, ">=", 2, null, null) };
				break;
			case "Err21":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace '=<' with '<='", "Replace the wrong comparison operator", offset, endChar, "<=", 2, null, null) };
				break;
			case "Err22":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove unknown token", "Remove unknown token", offset, endChar, "", 1, null, null) };
				break;

			case "Warn12":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace 'true' with 'TRUE'", "Remove the wrong boolean assignment", offset, endChar, "TRUE", 4, null, null) };

				break;
			case "Warn13":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace 'false' with 'FALSE'", "Remove the wrong boolean assignment", offset, endChar, "FALSE", 5, null, null) };

				break;
			case "Warn14":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace 'null' with 'NULL'", "Remove the wrong assignment", offset, endChar, "NULL", 4, null, null) };

				break;
			case "Warn15":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace 'na' with 'NA'", "Remove the wrong assignment", offset, endChar, "NA", 2, null, null) };

				break;
			case "Warn16":

				prop = calculateLevensteinSuggestions(offset, endChar, tokenText);
				/* An installed package which has the function: library(pkg) */
				prop = org.apache.commons.lang3.ArrayUtils.addAll(addLibraryProposals(viewer.getDocument().get(), tokenText), prop);

				break;

			case "Warn17":
				/*
				 * prop = new ICompletionProposal[] {
				 * 
				 * new RQuickFixCompletionProposal("Create function", offset, endChar,
				 * System.lineSeparator()+tokenText+"<-function(){}"+ System.lineSeparator(), 0)
				 * };
				 */

				break;
			case "Warn18":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace function call parameter", "Replace the wrong function call parameter with the parameter name defined in the function definition",
								offset, endChar, replacement, endChar - offset, null, null) };
				break;

			case "Warn20":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace wrong comparison", "Replace wrong comparison with test function.", offset, endChar, replacement,
								endChar - offset, null,null) };

				break;
				
			case "Warn21":
				/*in this proposal we replace the string identical() with isTRUE(all.equal()) (see second proposal)!*/
				String replacement2=replacement.replace("identical", "isTRUE(all.equal");
				replacement2=replacement2.replace(")", "))");
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("If appropriate test objects for exact equality", "Use identical function to test equality of two objects.", offset, endChar, replacement,
								endChar - offset, null,null),
						new RQuickFixCompletionProposal("If appropriate test if two objects are (nearly) equal", "Use all.equal function to test for near equality of two objects.", offset, endChar, replacement2,
										endChar - offset, null,null) };
				

				break;
			case "Warn22":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace assignment", "Replace assignment in function call.", offset, endChar, replacement,
								endChar - offset, null,null) };

				break;
				
			case "Warn23":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Click here to install missing R package", "Package can't be found in the\ndefault R package locations.", offset, endChar, tokenText,
								endChar - offset, "Install_Missing_Packages", editor) };

				break;

			case "Err26":
				/* An unclosed bracket: insert the closing one */
				prop = closingBracketProposals(replacement);
				break;
			case "Err27", "Err28", "Err29", "Err30", "Warn24", "Warn25", "Warn26", "Warn27", "Warn28":
				/* The fixes of the code analysis (edits, see CodeFix) */
				prop = RQuickFixEditsProposal.of(replacement);
				break;
			default:
				break;
			}

		}
		return prop;
	}

	/**
	 * The quick fixes of an unclosed bracket (see BracketCheck): the replacement is
	 * "text@offset[@offset2]", the closing bracket after the last code before the
	 * next statement and at the end of the line of the bracket.
	 */
	public static ICompletionProposal[] closingBracketProposals(ISourceViewer viewer, String replacement) {
		if (replacement == null || !replacement.contains("@")) {
			return null;
		}
		String[] parts = replacement.split("@");
		String text = parts[0].replace("\\n", "\n").replace("\\r", "\r");
		String bracket = text.trim();
		java.util.List<ICompletionProposal> list = new java.util.ArrayList<>();
		IDocument doc = viewer.getDocument();
		for (int i = 1; i < parts.length; i++) {
			try {
				int offset = Integer.parseInt(parts[i].trim());
				if (offset < 0 || offset > doc.getLength()) {
					continue;
				}
				int line = doc.getLineOfOffset(offset) + 1;
				String where = i == 1 ? "before the next statement (line " + line + ")" : "at the end of line " + line;
				list.add(new RQuickFixCompletionProposal("Insert '" + bracket + "' " + where, "Inserts the missing closing bracket " + where + ".", offset, 0, text, 0,
						null, null));
			} catch (NumberFormatException | org.eclipse.jface.text.BadLocationException e) {
				/* No proposal */
			}
		}
		return list.isEmpty() ? null : list.toArray(new ICompletionProposal[0]);
	}

	/**
	 * The quick fixes of a marker of the code analysis for an editor which is not
	 * an R editor (e.g., the R chunks of a markdown document).
	 *
	 * @param code the R code (the offsets of the document, other text blanked)
	 */
	public static ICompletionProposal[] documentProposals(ISourceViewer viewer, String code, String errorCode, String tokenText, String replacement) {
		if (errorCode == null) {
			return null;
		}
		return switch (errorCode) {
		case "Err26" -> closingBracketProposals(viewer, replacement);
		case "Err27", "Err28", "Err29", "Err30", "Warn24", "Warn25", "Warn26", "Warn27", "Warn28" -> RQuickFixEditsProposal.of(replacement);
		case "Warn16" -> addLibraryProposals(code, tokenText);
		default -> null;
		};
	}

	/*
	 * The installed packages which document the function (their help index) for
	 * the quick fix "Add library(pkg)": asked when the quick fix is opened (R
	 * running, at most 5 seconds)
	 */
	public static ICompletionProposal[] addLibraryProposals(String code, String function) {
		if (function == null || !function.matches("[A-Za-z.][A-Za-z0-9._]*")) {
			return null;
		}
		com.eco.bio7.rbridge.REvaluator c = REditor.getEvaluator();
		if (c == null || com.eco.bio7.rbridge.RState.isBusy()) {
			return null;
		}
		try {
			RValue x = c.withTimeout(5000).eval("local({ f <- '" + function + "'; p <- .packages(all.available = TRUE); "
					+ "hit <- vapply(p, function(x) { a <- file.path(find.package(x, quiet = TRUE), 'help', 'AnIndex'); "
					+ "length(a) == 1 && file.exists(a) && any(startsWith(readLines(a, warn = FALSE), paste0(f, '\\t'))) }, logical(1)); "
					+ "head(setdiff(unique(p[hit]), c('base', 'stats', 'utils', 'graphics', 'grDevices', 'methods', 'datasets')), 5) })");
			String[] packages = x == null || x.isNull() ? new String[0] : x.strings();
			if (packages == null || packages.length == 0) {
				return null;
			}
			int insert = com.eco.bio7.reditor.antlr.CodeChecks.libraryInsertOffset(code);
			java.util.List<ICompletionProposal> list = new java.util.ArrayList<>();
			for (String p : packages) {
				list.add(new RQuickFixEditsProposal(new com.eco.bio7.reditor.antlr.CodeFix.Proposal("Add library(" + p + ") (the package has " + function + ")",
						java.util.List.of(new com.eco.bio7.reditor.antlr.CodeFix.Edit(insert, 0, "library(" + p + ")" + System.lineSeparator())))));
			}
			return list.toArray(new ICompletionProposal[0]);
		} catch (Exception e) {
			return null;
		}
	}

	private ICompletionProposal[] closingBracketProposals(String replacement) {
		return closingBracketProposals(viewer, replacement);
	}

	private ICompletionProposal[] calculateLevensteinSuggestions(int offset, int endChar, String tokenText) {
		ICompletionProposal[] prop;
		RRefPhaseListen ref = new Parse(editor).parseFromOffset(offset);
		StringBuffer buffScopedFunctions = ref.getBuffScopeFunctions();
		String[] splitBuffScopedFun = buffScopedFunctions.toString().split(",");

		List<Pair<String, Double>> words = new ArrayList<Pair<String, Double>>();
		List<Pair<String, Double>> proposals = new ArrayList<Pair<String, Double>>();

		/*
		 * First we add the proposals function from already loaded packages and the sort
		 * them!
		 */
		String[] statistics = CalculateRProposals.getStatistics();
		for (int i = 0; i < statistics.length; i++) {
			/* Calculate the LevenShtein distance! */
			double dist = Levenshtein.getLevenshteinDistance(tokenText, statistics[i]);
			/* Calculate as percent! */
			double max = Math.max(tokenText.length(), statistics[i].length());
			double perct = round(1.0 - (dist / max), 2);
			/* Add to a Pair to sort the distances! */
			proposals.add(new MutablePair<String, Double>(statistics[i], perct));
		}
		/* Sort the Levenshtein distances in descending order! */
		sort(proposals);
		/*
		 * We only add 10 sorted functions from the packages to the final suggestions!
		 */
		for (int i = 0; i < 10; i++) {
			words.add(proposals.get(i));
		}
		/* Now we add the local defined functions and sort them! */
		for (int i = 0; i < splitBuffScopedFun.length; i++) {
			/* Calculate the LevenShtein distance! */
			double dist = Levenshtein.getLevenshteinDistance(tokenText, splitBuffScopedFun[i]);
			/* Calculate as percent! */
			double max = Math.max(tokenText.length(), splitBuffScopedFun[i].length());
			double perct = round(1.0 - (dist / max), 2);
			/* Add to a Pair to sort the distances! */
			words.add(new MutablePair<String, Double>(splitBuffScopedFun[i], perct));
		}

		/* Sort the Levenshtein distances in descending order! */
		sort(words);

		/*
		 * Here we search for functions in all packages. If found returns the package
		 * name!
		 */
		String[] packagesFound = searchForPackageFunction(tokenText);
		if (packagesFound != null) {
			ArrayList<String> resultedPackages = new ArrayList<String>();
			for (int i = 0; i < packagesFound.length; i++) {
				if (editor.getParser().getCurrentPackageImports().containsKey(packagesFound[i]) == false) {
					resultedPackages.add(packagesFound[i]);
				}
			}
			String[] tempArray = new String[resultedPackages.size()];
			packagesFound = resultedPackages.toArray(tempArray);

			prop = new ICompletionProposal[words.size() + packagesFound.length];
			for (int i = 0; i < packagesFound.length; i++) {

				prop[i] = new RQuickFixCompletionProposal("Import package: " + packagesFound[i], null, 0, 0, "library(" + packagesFound[i] + ")\n", 0, "Reload_Package_Completion", editor);

			}
			for (int i = 0; i < words.size(); i++) {
				if (words.get(i) != null) {
					Pair<String, Double> sugOrdered = words.get(i);

					prop[i + packagesFound.length] = new RQuickFixCompletionProposal("Functions available:" + " '" + sugOrdered.getKey() + "' Similarity:" + " " + sugOrdered.getValue(), null,
							offset, endChar, sugOrdered.getKey(), endChar - offset, null, null);
				} else {
					prop[i + packagesFound.length] = new RQuickFixCompletionProposal("Functions available: NA", null, offset, endChar, "NA", 2, null, null);
				}
			}
		} else {
			/* No packages found we return the default! */
			prop = new ICompletionProposal[words.size()];
			for (int i = 0; i < words.size(); i++) {
				if (words.get(i) != null) {
					Pair<String, Double> sugOrdered = words.get(i);

					prop[i] = new RQuickFixCompletionProposal("Functions available:" + " '" + sugOrdered.getKey() + "' Similarity:" + " " + sugOrdered.getValue(), null, offset, endChar,
							sugOrdered.getKey(), endChar - offset, null, null);
				} else {
					prop[i] = new RQuickFixCompletionProposal("Functions available: NA", null, offset, endChar, "NA", 2, null, null);
				}
			}

		}
		/* First proposal to create a function! */
		// prop[0] = new RQuickFixCompletionProposal("Create function","Create a new
		// function", offset, endChar, System.lineSeparator() + tokenText +
		// "<-function(){}" + System.lineSeparator(), 0);
		/*
		 * The following proposals all sorted functions from Levenshstein distances!
		 */

		/*
		 * for (int i = 0; i < words.size(); i++) { if (words.get(i) != null) {
		 * Pair<String, Double> sugOrdered = words.get(i);
		 * 
		 * prop[i + packagesFound.length] = new
		 * RQuickFixCompletionProposal("Functions available:" + " '" +
		 * sugOrdered.getKey() + "' Similarity:" + " " + sugOrdered.getValue(), null,
		 * offset, endChar, sugOrdered.getKey(), endChar - offset,null); } else { prop[i
		 * + packagesFound.length] = new
		 * RQuickFixCompletionProposal("Functions available: NA", null, offset, endChar,
		 * "NA", 2,null); } }
		 */
		return prop;
	}

	public String[] searchForPackageFunction(String funcName) {
		/* Called in the UI thread (quick assist): time-limited for embedded R */
		REvaluator c = REvaluator.forEditor(REditor.getEvaluator());

		if (c != null) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);

				/* Get all installed dataset names, their package and description! */
				try {

					RValue rValue = c.eval("try(help.search(paste0(\"^\",\"" + funcName + "\",\"$\"),agrep=FALSE)$matches[,\"Package\"])");
					if (rValue != null) {
						packages = rValue.strings();
					}
				} catch (RException | RTypeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					/* Only reset the state set here (not that of a running job, e.g., an installation) */
					RState.setBusy(false);
				}

			} else {
				System.out.println("R is busy!");
			}
		}

		/*
		 * REvaluator c = REditor.getEvaluator(); if (c != null) { if
		 * (RState.isBusy() == false) { RState.setBusy(true); Display display =
		 * Util.getDisplay(); display.syncExec(() -> {
		 * 
		 * if (c != null) {
		 * 
		 * Get all installed dataset names, their package and description! try {
		 * 
		 * RValue rValue = c.eval("try(help.search(paste0(\"^\",\"" + funcName +
		 * "\",\"$\"),agrep=FALSE)$matches[,\"Package\"])"); if (rValue != null) {
		 * packages = rValue.strings(); } } catch (RException |
		 * RTypeException e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); }
		 * 
		 * }
		 * 
		 * }); RState.setBusy(false); } else { System.out.println("R is busy!"); }
		 */

		// else {
		// System.out.println("R is not running!");
		// }
		return packages;
	}

	public void sort(List<Pair<String, Double>> list) {
		list.sort(new Comparator<Pair<String, Double>>() {
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
	}

	/*
	 * From: http://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-
	 * places?lq=1 author: jonik : http://stackoverflow.com/users/56285/jonik
	 */
	public static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

}