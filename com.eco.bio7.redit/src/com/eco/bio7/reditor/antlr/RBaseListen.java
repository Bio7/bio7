package com.eco.bio7.reditor.antlr;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.NotNull;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.IDocumentProvider;

import com.eco.bio7.reditor.outline.ClassModel;
import com.eco.bio7.reditors.REditor;

public class RBaseListen extends RBaseListener {

	private CommonTokenStream tokens;
	public ArrayList<String> startStop = new ArrayList<String>();
	public ClassModel cm = new ClassModel();
	private REditor editor;
	private Parser parser;

	public RBaseListen(CommonTokenStream tokens, REditor editor,Parser parser) {
		this.tokens = tokens;
		this.editor = editor;
		this.parser=parser;
	}
	
	
	@Override public void exitExprError(@NotNull RParser.ExprErrorContext ctx) {
		Interval sourceInterval = ctx.getSourceInterval();
		int count = -1;
		int start=sourceInterval.a;
         /*We calculate the token position from the expression!*/
		List<Token> firstToken = tokens.get(sourceInterval.a,sourceInterval.b);
		for (int i = 0; i < firstToken.size(); i++) {
		System.out.println(firstToken.get(i).getText());
			if(firstToken.get(i).getText().equals(")")){
				count=i+1;
				break;
			}
		}
		//System.out.println(count);
		
		/*Notify the parser!*/
		parser.notifyErrorListeners(tokens.get(start+count),"One Parentheses to much!",null);
		
		
		
	}
	
	
	/**
	 * {@inheritDoc}
	 * <p/>
	 * The default implementation does nothing.
	 */
	@Override public void exitExprError2(@NotNull RParser.ExprError2Context ctx) {
		
		Interval sourceInterval = ctx.getSourceInterval();
		int count = -1;
		int start=sourceInterval.a;
         /*We calculate the token position from the expression!*/
		List<Token> firstToken = tokens.get(sourceInterval.a,sourceInterval.b);
		for (int i = 0; i < firstToken.size(); i++) {
		System.out.println(firstToken.get(i).getText());
			if(firstToken.get(i).getText().equals("(")){
				count=i;
				break;
			}
		}
		//System.out.println(count);
		
		/*Notify the parser!*/
		parser.notifyErrorListeners(tokens.get(start+count),"One Parentheses to much!",null);
	}
	
	


	
	/**
	 * {@inheritDoc}
	 * <p/>
	 * The default implementation does nothing.
	 */
	@Override
	public void exitDefFunction(@NotNull RParser.DefFunctionContext ctx) {
		
		
		
		Interval sourceInterval = ctx.getSourceInterval();

		Token firstToken = tokens.get(sourceInterval.a);
		// System.out.println(ctx.getParent().getChild(0).getText());
		int lineStart = firstToken.getStartIndex();
		// String ct=ctx.getText();

		// System.out.println("function start at line:"+lineStart);
		Token lastToken = tokens.get(sourceInterval.b);
		int lineEnd = lastToken.getStopIndex() + 1 - lineStart;
		// String ct2=ctx.getText();

		// Add to the editor folding action.
		startStop.add(lineStart + "," + lineEnd);
		// Add to the outline view.
		IDocumentProvider provider = editor.getDocumentProvider();
		IDocument document = provider.getDocument(editor.getEditorInput());
		int lineMethod = 0;
		try {
			lineMethod = document.getLineOfOffset(lineStart) + 1;
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int childs = ctx.getParent().getChildCount();
		int posTree = 0;
		for (int i = 0; i < childs; i++) {
			if (ctx.getText().equals(ctx.getParent().getChild(i).getText())) {
				posTree = i;
			}
		}
		if (ctx.getParent().getChild(posTree - 1) != null && ctx.getParent().getChild(posTree - 2) != null) {
			String op = ctx.getParent().getChild(posTree - 1).getText();
			String name = ctx.getParent().getChild(posTree - 2).getText();
			// String operator=ctx.getParent().getChild(1).getText();
			if (op.equals("<-")||op.equals("<<-")) {
				cm.methodNames.add(name + ";" + lineMethod);
			}
		}
		else if(posTree==0){
			cm.methodNames.add(ctx.getText() + ";" + lineMethod);
		}

	}

}