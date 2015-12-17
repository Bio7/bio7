package com.eco.bio7.reditor.actions;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.reditor.antlr.RFilter;
import com.eco.bio7.reditor.antlr.RLexer;
import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditor.antlr.RParser.ProgContext;
import com.eco.bio7.reditor.antlr.refactor.ExtractInterfaceListener;
import com.eco.bio7.reditor.antlr.refactor.ParseErrorListener;

public class ExtractMethod implements IEditorActionDelegate {

	private ISelection selection;
	private IEditorPart targetEditor;
	private CommonTokenStream tokens;
	private RParser parser;
	private TokenStreamRewriter rewriter;
	private ProgContext tree;

	public void setActiveEditor(final IAction action, final IEditorPart targetEditor) {
		this.targetEditor = targetEditor;

	}

	public void run(final IAction action) {

		IResource resource = (IResource) targetEditor.getEditorInput().getAdapter(IResource.class);
		if (resource != null) {

			ITextEditor editor = (ITextEditor) targetEditor;
			IDocumentProvider dp = editor.getDocumentProvider();
			IDocument doc = dp.getDocument(editor.getEditorInput());
			String docText = doc.get();
			ISelectionProvider sp = editor.getSelectionProvider();
			ISelection selectionsel = sp.getSelection();
			ITextSelection selection = (ITextSelection) selectionsel;
			String text = selection.getText();
			/* First parse of the selection! */
			boolean errors = parseSource(text);
			if (errors == false) {
				if (!selection.isEmpty()) {
					int start = selection.getOffset();

					try {

						int length = selection.getLength();
						doc.replace(start, length, "myNewFunc()");

					} catch (BadLocationException e) {

						e.printStackTrace();
					}

				}
			} else {
				System.out.println("How many errors1: " + parser.getNumberOfSyntaxErrors());
				return;
			}

			String fullTextWithoutSel = doc.get();
			// System.out.println("parsing: "+text);
			/* Text with deleted selection! */
			/* Second parse of the text with deleted text selection! */
			boolean error = parseSource(fullTextWithoutSel);
			/* Selection */
			if (error == false) {
				rewriter = new TokenStreamRewriter(tokens);
				// Token token = tokens.get(0);

				rewriter.insertAfter(tree.start, "\r\nmyNewFunc<-function(){\r\n" + text + "\r\n}\r\n");
				// System.out.println("myFunc<-function(){\n\t"+rewriter.getText()+"\n}");
				// System.out.println(rewriter.getText());
			} else {
				System.out.println("How many errors2: " + parser.getNumberOfSyntaxErrors());
				return;
			}
			/* Third parse for the final result! */
			boolean errorNewText = parseSource(rewriter.getText());

			if (errorNewText == false) {
				doc.set(rewriter.getText());

			}

			else {
				System.out.println("How many errors3: " + parser.getNumberOfSyntaxErrors());
				// System.out.println("final"+rewriter.getText());
				doc.set(docText);
				return;
			}

		}

	}

	/* Here we parse the text and test for possible errors! */
	private boolean parseSource(String fullText) {
		boolean errors;
		ANTLRInputStream input = new ANTLRInputStream(fullText);
		RLexer lexer = new RLexer(input);
		tokens = new CommonTokenStream(lexer);
		RFilter filter = new RFilter(tokens);
		filter.stream(); // call start rule: stream
		tokens.reset();
		ParseErrorListener parseErrorListener = new ParseErrorListener();
		parser = new RParser(tokens);

		lexer.removeErrorListeners();
		// lexer.addErrorListener(li);
		parser.removeErrorListeners();
		// parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);
		parser.addErrorListener(parseErrorListener);

		tree = parser.prog();
		ParseTreeWalker walker = new ParseTreeWalker(); // create standard
														// walker
		ExtractInterfaceListener extractor = new ExtractInterfaceListener(tokens, parser);
		walker.walk(extractor, tree);
		if (parser.getNumberOfSyntaxErrors() == 0) {
			errors = false;
		} else {
			errors = true;
		}
		return errors;
	}

	public void selectionChanged(final IAction action, final ISelection selection) {
		this.selection = selection;
	}

}
