package com.eco.bio7.markdownedit.parser;

import com.eco.bio7.markdownedit.editors.RMarkdownSpellingReconcileStrategy;
import com.eco.bio7.markdownedit.outline.MarkdownEditorOutlineNode;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.NodeVisitor;
import com.vladsch.flexmark.ast.VisitHandler;
import com.vladsch.flexmark.ast.Visitor;

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
				

				/*Display dis = Display.getDefault();
				dis.syncExec(new Runnable() {
					public void run() {
						ISourceViewer viewer = rMarkdownRconciler.markdownEditor.getViewer();
						
						
						StyledText text = viewer.getTextWidget();

						StyleRange styleRange = new StyleRange();
                        System.out.println(heading.getStartOffset()+" Length: "+heading.getTextLength());
						styleRange.start = heading.getStartOffset();
						styleRange.length = heading.getTextLength();
						
						styleRange.fontStyle = SWT.BOLD;

						// styleRange.strikeout = true;

						FontData data = text.getFont().getFontData()[0];
						Font font1 = new Font(dis, data.getName(), data.getHeight() + 2, data.getStyle());
						styleRange.font = font1;
						styleRange.foreground = dis.getSystemColor(SWT.COLOR_BLUE);
						//viewer.invalidateTextPresentation();
						text.setStyleRange(styleRange);
						
					}
				});*/

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