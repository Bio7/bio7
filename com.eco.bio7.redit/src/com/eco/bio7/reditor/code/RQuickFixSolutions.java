package com.eco.bio7.reditor.code;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

public class RQuickFixSolutions  {
	/*Hardcoded. We could also refactor out the solutions to a properties file!*/
	public ICompletionProposal[] getProposals(String errorCode, int offset, int endChar, ICompletionProposal[] prop) {
		if (errorCode != null) {
			switch (errorCode) {
			case "Err1":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'", offset, endChar, "", 1) };
				break;
			case "Err2":

				break;
			case "Err3":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'", offset, endChar, "", 1) };
				break;
			case "Err4":

				break;
			case "Err5":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'", offset, endChar, "", 1) };
				break;
			case "Err6":

				break;
			case "Err7":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'", offset, endChar, "", 1) };
				break;
			case "Err8":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ']'", offset, endChar, "", 1) };

				break;
			case "Err9":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ']'", offset, endChar, "", 1) };
				break;
			case "Err10":

				break;
			case "Err11":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove '}'", offset, endChar, "", 1) };
				break;
			case "Warn12":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace 'true' with 'TRUE'", offset, endChar, "TRUE", 4) };

				break;
			case "Warn13":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace 'false' with 'FALSE'", offset, endChar, "FALSE", 5) };

				break;
			case "Warn14":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace 'null' with 'NULL'", offset, endChar, "NULL", 4) };

				break;
			case "Warn15":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace 'na' with 'NA'", offset, endChar, "NA", 2) };

				break;
			case "Err16":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'", offset, endChar, "", 1) };

				break;
			case "Err17":

				break;
			case "Err18":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove ')'", offset, endChar, "", 1) };
				break;
			case "Err19":

				break;
			case "Err20":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace '=>' with '>='", offset, endChar, ">=", 2) };
				break;
			case "Err21":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Replace '=<' with '<='", offset, endChar, "<=", 2) };
				break;
			case "Err22":
				prop = new ICompletionProposal[] {

						new RQuickFixCompletionProposal("Remove unknown token", offset, endChar, "", 1) };
				break;

			default:
				break;
			}
			
		}
		return prop;
	}
	
     
   }