package com.eco.bio7.rbridge;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.eco.bio7.rffm.RValue;
import com.eco.bio7.rffm.RException;
import com.eco.bio7.rffm.RTypeException;

public class RStrObjectInformation {
	public static String ERROR_CHECK = "Error";//// Used in code completion popup infos and tryCatch error function below!
	public static String PACKAGE_LABEL = "see list!";// Used in code completion popup infos!

	public String getRHelpInfo(String htmlHelpText, REvaluator con, String packageLabel) {
		if (htmlHelpText.contains(" ")) {
			htmlHelpText = htmlHelpText.substring(0, htmlHelpText.indexOf(" "));
		}
		// htmlHelpText = htmlHelpText.replace("\r", "");
		if (htmlHelpText.contains("(")) {
			htmlHelpText = htmlHelpText.substring(0, htmlHelpText.indexOf("("));
		}

		// System.out.println(htmlHelpText);

		String out = null;

		try {

			out = (String) con.eval("tryCatch(paste(capture.output(tools:::Rd2txt(utils:::.getHelpFile(?" + htmlHelpText + "),package=\"" + packageLabel
					+ "\", stages=c(\"install\",\"render\"),outputEncoding = \"UTF-8\")),collapse='\n'), error = function(e) {return(\"Error\")})").firstString();
			// System.out.println("help Out: "+out);

		} catch (RException | RTypeException e) {

			// e.printStackTrace();

		}

		if (out != null) {
			out = out.replace("_\b", "");
		}
		return out;

	}

	/**
	 * A method to generate R object information.
	 * 
	 * @param matDfName
	 *            the name of the matrix, dataframe, etc.
	 * @param con
	 *            the R connection
	 * @return a string of the the str() function in R
	 */
	public String getRStrObjectInfo(String matDfName, REvaluator con) {
		String resultStr = null;

		try {
			RValue rValue = con.eval("if (!is.null(get0('" + matDfName + "'))) {" + "paste(capture.output(str(" + matDfName + ")),collapse='\n')}");
			if (rValue.isNull() == false) {
				resultStr = rValue.firstString();
			}

		} catch (RException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		} catch (RTypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return resultStr;
	}

	public String getRHelpInfo2(String htmlHelpText, REvaluator con, String packageLabel) {

		String informationControlText = null;
		try {
			con.eval("try(.bio7TempHtmlHelpFile <- paste(tempfile(), \".txt\", sep=\"\"),silent = T)").toString();
		} catch (RException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			con.eval("try(tools::Rd2txt(utils:::.getHelpFile(?" + htmlHelpText + "),.bio7TempHtmlHelpFile,package=\"tools\", stages=c(\"install\", \"render\"),outputEncoding = \"\"),silent = T)");
		} catch (RException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String out = null;
		try {
			try {
				out = (String) con.eval("try(.bio7TempHtmlHelpFile)").firstString();
			} catch (RException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
		} catch (RTypeException e) {

			e.printStackTrace();
		}

		String url = out.replace("\\", "/");

		byte[] encoded = null;
		try {
			encoded = Files.readAllBytes(Paths.get(url));
		} catch (IOException e) {

			e.printStackTrace();
		}
		informationControlText = new String(encoded, Charset.defaultCharset());
		// help = new Util().fileToString(url);
		informationControlText = informationControlText.replace("_\b", "");
		return informationControlText;
	}

	/**
	 * A method to generate a temporary file with R object information.
	 * 
	 * @param matDfName
	 *            the name of the matrix, dataframe, etc.
	 * @param con
	 *            the R connection
	 * @return a string of the the str() function in R
	 */
	public String getRStrObjectInfo2(String matDfName, REvaluator con) {
		String out = null;

		try {
			con.eval("try(.bio7TempHtmlHelpFile <- paste(tempfile(), \".txt\", sep=\"\"),silent = T)").toString();
			con.eval("if (!is.null(get0('" + matDfName + "'))) {" + "try(paste(capture.output(str(" + matDfName + "),file = .bio7TempHtmlHelpFile),collapse=\"\\n\"))"
					+ "} else {try(paste(capture.output(cat(\"\"),file = .bio7TempHtmlHelpFile),collapse=\"\\n\"))}");
		} catch (RException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			out = (String) con.eval("try(.bio7TempHtmlHelpFile)").firstString();
		} catch (RException e1) {
			// TODO Auto-generated catch block
			// e1.printStackTrace();
		} catch (RTypeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		byte[] encoded = null;
		try {
			encoded = Files.readAllBytes(Paths.get(out.replace("\\", "/")));
		} catch (IOException e) {

			e.printStackTrace();
		}
		String resultStr = new String(encoded, Charset.defaultCharset());
		return resultStr;
	}

}
