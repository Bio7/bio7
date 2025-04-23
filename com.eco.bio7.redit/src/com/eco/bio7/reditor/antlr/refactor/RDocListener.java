package com.eco.bio7.reditor.antlr.refactor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import com.eco.bio7.reditor.antlr.DeclCallStore;
import com.eco.bio7.reditor.antlr.RBaseListen;
import com.eco.bio7.reditor.antlr.RBaseListener;
import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditor.antlr.RParser.E30Context;
import com.eco.bio7.reditor.antlr.RParser.FormContext;
import com.eco.bio7.reditor.antlr.RScope;
import com.eco.bio7.reditor.antlr.ref.RBaseScope;
import com.eco.bio7.reditor.antlr.ref.RFunctionSymbol;
import com.eco.bio7.reditor.antlr.ref.RGlobalScope;
import com.eco.bio7.reditor.antlr.ref.RSymbol;
import com.eco.bio7.reditor.antlr.ref.RVariableSymbol;
import com.eco.bio7.reditor.antlr.ref.Scope;
import com.eco.bio7.reditor.antlr.util.Utils;
import com.eco.bio7.reditor.outline.REditorOutlineNode;

public class RDocListener extends RBaseListener {
	private RParser parser;
	private CommonTokenStream tokens;
	private boolean captureId = true;
	private HashSet<String> capId = new HashSet<String>();
	private LinkedPositionGroup linkedPositionGroup;
	private IDocument document;
	private String selectedText;
	private ParseTreeProperty<Scope> scopes;
	private Scope currentScope; // Resolve symbols starting in this scope
	private RGlobalScope globals;
	private Scope currentSelectedScope;
	private int scopeStart;
	private int scopeStop;

	public RDocListener(CommonTokenStream tokens, RBaseListen list, RParser parser) {
		this.scopes = list.scopeNew;
		this.globals = list.globals;
		this.tokens = tokens;
		this.parser = parser;
		this.document = document;
		this.selectedText = selectedText;
		this.linkedPositionGroup = linkedPositionGroup;
		this.currentSelectedScope = currentScope;
		this.scopeStart = scopeStart;
		this.scopeStop = scopeStop;
	}

	public void enterProg(RParser.ProgContext ctx) {

		currentScope = currentSelectedScope;
		// scopes.push(new RefactorScopeVar());

	}

	public void exitProg(RParser.ProgContext ctx) {
		// scopes.pop();
	}

	

	public void enterE19DefFunction(RParser.E19DefFunctionContext ctx) {
		currentScope = scopes.get(ctx);

		if (ctx.formlist() != null) {
			List<FormContext> formList = ctx.formlist().form();
			int functionDefSize = formList.size();
			for (int i = 0; i < functionDefSize; i++) {
				FormContext fo = formList.get(i);

				TerminalNode ar = fo.ID();
				if (ar != null) {
					Token tok = ar.getSymbol();
					int startIndex = ar.getSymbol().getStartIndex();
					String text = ar.getText();
					if (ar.getText().equals(selectedText)) {
						// System.out.println(currentSelectedScope.getScopeName()+"
						// current: "+currentScope.getScopeName());
						// System.out.println(currentSelectedScope+" current:
						// "+currentScope);
						if (currentSelectedScope instanceof RGlobalScope) {
							RFunctionSymbol sco = (RFunctionSymbol) currentScope;
							sco.setUsed(true);
						}
						/*
						 * Detect nested scope by name. Has to be improved.
						 * problem: Scopes are always different (from earlier
						 * parse process)
						 */
						/*
						 * else if(currentScope.getScopeName().equals(
						 * currentSelectedScope.getScopeName())==false) {
						 * 
						 * RFunctionSymbol sco = (RFunctionSymbol) currentScope;
						 * sco.setUsed(true); }
						 */

						else {
							if (currentScope.getScopeName().equals(currentSelectedScope.getScopeName())) {
								
								try {
									linkedPositionGroup.addPosition(new LinkedPosition(document, startIndex, text.length()));

								} catch (BadLocationException e) {

									e.printStackTrace();
								}
							}
						}

						// }

					} else {

					}

				}

			}
		}
	}

	public void exitE19DefFunction(RParser.E19DefFunctionContext ctx) {
		currentScope = currentScope.getEnclosingScope(); // pop scope
		// scopes.pop();

	}

	

	/** Listen to matches of classDeclaration */

	public void enterE20CallFunction(RParser.E20CallFunctionContext ctx) {
		// System.out.println(ctx.getText());

	}
	
	public void exitE20CallFunction(RParser.E20CallFunctionContext ctx) {
		// System.out.println(ctx.getText());

	}

	

}
