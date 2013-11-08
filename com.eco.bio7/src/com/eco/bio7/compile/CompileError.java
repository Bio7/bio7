/*package com.eco.bio7.compile;



import bsh.EvalError;

import javax.tools.JavaFileObject;
import javax.tools.Diagnostic;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

*//**
 * Wrap compiler errors
 *//*
public class CompileError extends Exception {

    private String errorMessage;
    private int lineNumber = -1;

    public CompileError(CompileError next, String errorMessage, int lineNumber){
        super(next);
        this.errorMessage = errorMessage;
        this.lineNumber = lineNumber;
    }
    
    public CompileError(String eclipseCompileError) {
        errorMessage = "<html>"+eclipseCompileError.replace("\n","<br/>")+"</html>";
    }

    public CompileError(EvalError cause) {
        super(cause);
        errorMessage =  cause.getErrorText();
        try{
            lineNumber = cause.getErrorLineNumber();
        }
        catch (Exception e){
            Pattern p = Pattern.compile("Parse error at line (\\d*),");
            Matcher m = p.matcher(cause.getMessage());
            if (m.find()){
                String lineNumberStr = m.group(1);
                if (lineNumberStr!=null && lineNumberStr.length()>0){
                    lineNumber = Integer.parseInt(lineNumberStr);
                }
            }             
        }

    }
    public CompileError(Throwable cause) {
        super(cause);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
*/