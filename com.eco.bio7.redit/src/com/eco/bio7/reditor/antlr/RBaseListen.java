package com.eco.bio7.reditor.antlr;

import java.util.ArrayList;

import org.antlr.v4.runtime.CommonTokenStream;
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
	public RBaseListen(CommonTokenStream tokens, REditor editor) {
		this.tokens = tokens;
		this.editor=editor;
	}

	@Override
	public void enterDefFunction(@NotNull RParser.DefFunctionContext ctx) {

		

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
		//System.out.println(ctx.getParent().getChild(0).getText());
		int lineStart = firstToken.getStartIndex();
		// String ct=ctx.getText();
        
		// System.out.println("function start at line:"+lineStart);
		Token lastToken = tokens.get(sourceInterval.b);
		int lineEnd = lastToken.getStopIndex()+1 - lineStart;
		// String ct2=ctx.getText();

		
		//Add to the editor folding action.
		startStop.add(lineStart + "," + lineEnd);
		//Add to the outline view.
		IDocumentProvider provider = editor.getDocumentProvider();
		IDocument document = provider.getDocument(editor.getEditorInput());
		int lineMethod = 0;
		try {
			lineMethod=document.getLineOfOffset(lineStart)+1;
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		cm.methodNames.add(ctx.getParent().getChild(0).getText()+";"+lineMethod);
		
		
	}
	

}