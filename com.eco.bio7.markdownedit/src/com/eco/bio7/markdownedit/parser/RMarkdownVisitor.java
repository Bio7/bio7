package com.eco.bio7.markdownedit.parser;

import com.eco.bio7.markdownedit.editors.RMarkdownSpellingReconcileStrategy;
import com.eco.bio7.markdownedit.outline.MarkdownEditorOutlineNode;
import com.vladsch.flexmark.ast.*;

public class RMarkdownVisitor {

	public int wordCount;
	public NodeVisitor visitor;
	private RMarkdownSpellingReconcileStrategy rMarkdownRconciler;

	public RMarkdownVisitor(RMarkdownSpellingReconcileStrategy rMarkdownSpellingReconcileStrategy) {
		// example of visitor for a node or nodes, just add VisitHandlers<> to the list
		// any node type not handled by the visitor will default to visiting its
		// children
		this.rMarkdownRconciler = rMarkdownSpellingReconcileStrategy;
		visitor = new NodeVisitor(new VisitHandler<Heading>(Heading.class, new Visitor<Heading>() {
			@Override
			public void visit(Heading heading) {
				RMarkdownVisitor.this.visit(heading);

			}
		}), new VisitHandler<FencedCodeBlock>(FencedCodeBlock.class, new Visitor<FencedCodeBlock>() {
			@Override
			public void visit(FencedCodeBlock codeBlock) {

				// VisitorSample.this.visit(codeBlock);
				int line = codeBlock.getLineNumber() + 1;
				rMarkdownRconciler.methods.push(new MarkdownEditorOutlineNode("Codeblock L.(" + line + ")", line,
						"RMarkdown", rMarkdownRconciler.markdownEditor.baseNode));

			}
		}));
	}

	public void visit(Heading heading) {

		rMarkdownRconciler.methods.push(new MarkdownEditorOutlineNode(heading.getText().toString(),
				heading.getLineNumber() + 1, "MarkdownHeader", rMarkdownRconciler.markdownEditor.baseNode));
		// Nested children.
		visitor.visitChildren(heading);

	}

}