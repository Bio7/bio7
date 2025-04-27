// Generated from ImageJMacro.g4 by ANTLR 4.4
package com.eco.bio7.ijmacro.editor.antlr;

import java.util.ArrayList;
import java.util.Stack;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.IDocumentProvider;
import com.eco.bio7.ijmacro.editor.antlr.ImageJMacroParser.ForStatementContext;
import com.eco.bio7.ijmacro.editor.antlr.ImageJMacroParser.FormalParameterListContext;
import com.eco.bio7.ijmacro.editor.antlr.ImageJMacroParser.MemberIndexExpressionContext;
import com.eco.bio7.ijmacro.editor.outline.IJMacroEditorOutlineNode;
import com.eco.bio7.ijmacro.editors.IJMacroEditor;

/**
 * This class provides an empty implementation of {@link ImageJMacroListener},
 * which can be extended to create a listener which only needs to handle a
 * subset of the available methods.
 */
public class ImageJMacroBaseListen extends ImageJMacroBaseListener {

	private Stack<IJMacroEditorOutlineNode> methods = new Stack<IJMacroEditorOutlineNode>();
	private Stack<VariableScope> variables = new Stack<VariableScope>();
	private ArrayList<String> globalVariables = new ArrayList<String>();
	private ArrayList<String> functions = new ArrayList<String>();
	private IJMacroEditor editor;
	public ArrayList<String> startStop = new ArrayList<String>();
	private int offsetCodeCompl = -1;
	private int tempDifferenceStart = 10000000;
	private VariableScope currentScope;
	private VariableScope tempCodeComplScope;

	public ArrayList<String> getGlobalVariables() {
		return globalVariables;
	}

	public ArrayList<String> getFunctions() {
		return functions;
	}

	public VariableScope getTempCodeComplScope() {
		return tempCodeComplScope;
	}

	public ImageJMacroBaseListen(IJMacroEditor editor) {
		this.editor = editor;
	}

	public ImageJMacroBaseListen(IJMacroEditor editor, int offset) {
		this.editor = editor;
		this.offsetCodeCompl = offset;
	}

	public void enterProgram(ImageJMacroParser.ProgramContext ctx) {
		VariableScope base = new VariableScope(null,null);
		tempCodeComplScope = base;
		variables.add(base);
	}

	public void exitProgram(ImageJMacroParser.ProgramContext ctx) {
		variables.pop();
		if (methods.empty() == false) {
			methods.pop();
		}

	}

	public void enterAssignmentExpression(ImageJMacroParser.AssignmentExpressionContext ctx) {
		Token firstToken = ctx.getStart();
		int lineStart = firstToken.getStartIndex();
		String name = ctx.singleExpression(0).getText();
		/* Omit array assignments (x[i]=5;! */
		if (ctx.singleExpression(0) instanceof MemberIndexExpressionContext) {
			return;
		}
		/* Omit for loop variables! */
		if (ctx.getParent() != null) {
			ParserRuleContext par = ctx.getParent();
			if (par.getParent() != null) {
				ParserRuleContext par2 = par.getParent();
				if (par2 instanceof ForStatementContext) {
					return;
				}
			}
		}

		int line = calculateLine(lineStart);
		if (methods.size() == 0) {
			new IJMacroEditorOutlineNode(name, line, "variable", editor.baseNode);
		}

		else {
			new IJMacroEditorOutlineNode(name, line, "variable", methods.peek());
		}
		VariableScope currentScope = variables.peek();
		currentScope.vars.add(name);

	}

	public void enterVariableDeclarationStatement(ImageJMacroParser.VariableDeclarationStatementContext ctx) {

		Token firstToken = ctx.getStart();
		int lineStart = firstToken.getStartIndex();

		String name = ctx.Identifier().getText();
		// System.out.println(ctx.singleExpression(0).getParent().getClass());
		// Add to the editor folding action if enabled in the preferences!

		int line = calculateLine(lineStart);
		if (methods.size() == 0) {
			new IJMacroEditorOutlineNode(name, line, "globalvariable", editor.baseNode);
		}

		else {
			new IJMacroEditorOutlineNode(name, line, "globalvariable", methods.peek());
		}
		globalVariables.add(name);
	}

	public void exitAssignmentExpression(ImageJMacroParser.AssignmentExpressionContext ctx) {
	}

	public void enterFunctionDeclaration(ImageJMacroParser.FunctionDeclarationContext ctx) {
		Token firstToken = ctx.getStart();
		Token lastToken = ctx.getStop();
		int lineStart = firstToken.getStartIndex();
		String name = ctx.Identifier().getText();

		// Add to the editor folding action if enabled in the preferences!

		int lineMethod = calculateLine(lineStart);
		int lineEnd = lastToken.getStopIndex() + 1 - lineStart;
		startStop.add(lineStart + "," + lineEnd);

		

		/* Here we create the outline nodes in the Outline view! */
		if (methods.size() == 0) {

			methods.push(new IJMacroEditorOutlineNode(name, lineMethod, "function", editor.baseNode));

		} else {
			methods.push(new IJMacroEditorOutlineNode(name, lineMethod, "function", methods.peek()));

		}
		/*For the variable we set the current scope!*/
		currentScope = variables.peek();
		
		FormalParameterListContext args = ctx.formalParameterList();
		if (args == null) {
			functions.add(lineMethod+"####"+name + "()");
			/* Give parent scope as Argument and null if we have no function arguments defined! */
			variables.add(new VariableScope(currentScope,null));
		} else {
			functions.add(lineMethod+"####"+name + "(" + args.getText() + ")");
			/* Give parent scope as Argument and add function parameters as arguments if available for code completion! */
			variables.add(new VariableScope(currentScope,args.getText()));
		}
		

	}

	public void exitFunctionDeclaration(ImageJMacroParser.FunctionDeclarationContext ctx) {
		/*
		 * For code completion detect the parentheses index according to grammar!
		 */

		int startIndex = ctx.OpenBrace().getSymbol().getStartIndex();
		int stopIndex = ctx.CloseBrace().getSymbol().getStopIndex() + 1;
		/*
		 * Calculate the closest function to the offset when closest found at the exit
		 * of the prog calculate the functions in the scope!
		 */
		setCurrentScopeFromOffset(startIndex, stopIndex);

		variables.pop();
		if (methods.empty() == false) {
			methods.pop();
		}
	}

	public void enterMacroExpression(ImageJMacroParser.MacroExpressionContext ctx) {
		Token firstToken = ctx.getStart();
		Token lastToken = ctx.getStop();
		int lineStart = firstToken.getStartIndex();
		String name;
		if (ctx.StringLiteral() != null) {
			name = ctx.StringLiteral().getText();
		} else {
			return;
		}

		// Add to the editor folding action if enabled in the preferences!

		int lineMethod = calculateLine(lineStart);
		int lineEnd = lastToken.getStopIndex() + 1 - lineStart;
		startStop.add(lineStart + "," + lineEnd);
		/*For the outline item title remove the parentheses to make them sortable!*/
        name=name.replaceAll("\"", "");
        
        if(name != null && name.length() > 40) {
            name= name.substring(0, 40) + "...";
        } 
		/* Here we create the outline nodes in the Outline view! */
		if (methods.size() == 0) {

			methods.push(new IJMacroEditorOutlineNode(name, lineMethod, "macro", editor.baseNode));

		} else {
			methods.push(new IJMacroEditorOutlineNode(name, lineMethod, "macro", methods.peek()));

		}
		/*For the variable we set the current scope!*/
		currentScope = variables.peek();
		/* Give parent scope as Argument! In a macro definition we have no function arguments!*/
		variables.add(new VariableScope(currentScope,null));
	}

	public void exitMacroExpression(ImageJMacroParser.MacroExpressionContext ctx) {
		/*
		 * For code completion detect the parentheses index according to grammar!
		 */

		int startIndex = ctx.OpenBrace().getSymbol().getStartIndex();
		int stopIndex = ctx.CloseBrace().getSymbol().getStopIndex() + 1;
		/*
		 * Calculate the closest function to the offset when closest found at the exit
		 * of the prog calculate the functions in the scope!
		 */
		setCurrentScopeFromOffset(startIndex, stopIndex);
		variables.pop();
		if (methods.empty() == false) {
			methods.pop();
		}
	}

	public void enterIterationStatements(ImageJMacroParser.IterationStatementsContext ctx) {
		Token firstToken = ctx.getStart();
		Token lastToken = ctx.getStop();
		int lineStart = firstToken.getStartIndex();
		if (lastToken != null) {
			int lineEnd = lastToken.getStopIndex() + 1 - lineStart;
			startStop.add(lineStart + "," + lineEnd);
		}

	}

	public void enterIfStatement(ImageJMacroParser.IfStatementContext ctx) {
		Token firstToken = ctx.getStart();
		Token lastToken = ctx.getStop();
		int lineStart = firstToken.getStartIndex();
		int lineEnd = lastToken.getStopIndex() + 1 - lineStart;
		startStop.add(lineStart + "," + lineEnd);
	}

	/* Calculates the line from the editor document! */
	private int calculateLine(int offsetStart) {
		IDocument document = null;
		int line = 0;
		if (editor.getEditorInput() != null && editor.getDocumentProvider() != null) {
			IDocumentProvider provider = editor.getDocumentProvider();
			document = provider.getDocument(editor.getEditorInput());
			int len = document.getLength();
			if (offsetStart >= 0 && offsetStart < len) {
				try {
					line = document.getLineOfOffset(offsetStart) + 1;
				} catch (BadLocationException e) {

					System.out.println("Bad line location!");
					System.out.println("\nLine Number: " + line);
					// e.printStackTrace();
				}
			}
		}
		return line;
	}

	/*
	 * Calculate the current scope of the given (selected) offset (in which function
	 * the offset is embedded)
	 */
	private void setCurrentScopeFromOffset(int startIndex, int stopIndex) {
		/* If we have a selected offset (not by opening the file)! */
		if (offsetCodeCompl >= 0) {
			int distanceFromStart = offsetCodeCompl - startIndex;
			int distanceFromStop = offsetCodeCompl - stopIndex;
			/* If we have an positive offset after function parentheses! */
			if (distanceFromStart > 0 && distanceFromStop < 0) {

				/*
				 * Lookup if we have already the closest distance. If not take this distance as
				 * closest!
				 */
				if (distanceFromStart < tempDifferenceStart) {
					tempDifferenceStart = distanceFromStart;
					/*
					 * Store temporary the scope! Used at program in this file exit!
					 */
					tempCodeComplScope = variables.peek();
					// System.out.println(startIndex +" "+stopIndex);

					// }
				}

			}
		}
	}

}