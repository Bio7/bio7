package com.eco.bio7.reditor.antlr;

/**
 * @author Marcel
 * A class to store ANTLR error messages which will be collected in a list!
 */
public class ErrorWarnStore {
	
	private Object offendingSymbol;
	private int line;
	private int charPositionInLine;
	private String msg;

	public ErrorWarnStore(Object offendingSymbol, int line, int charPositionInLine,
			String msg){
		this.offendingSymbol=offendingSymbol;
		this.line=line;
		this.charPositionInLine=charPositionInLine;
		this.msg=msg;
		
	}

	public Object getOffendingSymbol() {
		return offendingSymbol;
	}

	public int getLine() {
		return line;
	}

	public int getCharPositionInLine() {
		return charPositionInLine;
	}

	public String getMsg() {
		return msg;
	}

}
