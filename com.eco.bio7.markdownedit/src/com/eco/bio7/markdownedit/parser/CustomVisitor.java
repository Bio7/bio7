package com.eco.bio7.markdownedit.parser;

import java.util.Stack;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Document;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.Parser;

import com.eco.bio7.markdownedit.editors.MarkdownEditor;
import com.eco.bio7.markdownedit.outline.MarkdownEditorOutlineNode;

public class CustomVisitor extends AbstractVisitor {
	int wordCount = 0;
	private Stack<MarkdownEditorOutlineNode> methods;

	public CustomVisitor(Stack<MarkdownEditorOutlineNode> methods,MarkdownEditor editor) {
		this.methods = methods;
	}

	@Override
	public void visit(Text text) {
		// System.out.println("Node= "+text.getLiteral());
		visitChildren(text);
	}

	@Override
	public void visit(BlockQuote blockQuote) {
		//methods.push(new MarkdownEditorOutlineNode("R Markdown Section", 0, "RMarkdown",editor.baseNode));
		visitChildren(blockQuote);
	}

	@Override
	public void visit(BulletList bulletList) {
		visitChildren(bulletList);
	}

	@Override
	public void visit(Code code) {
		// System.out.println("code= "+code.toString());
		visitChildren(code);
	}

	@Override
	public void visit(Document document) {
		visitChildren(document);
	}

	@Override
	public void visit(Emphasis emphasis) {
		visitChildren(emphasis);
	}

	@Override
	public void visit(FencedCodeBlock fencedCodeBlock) {
		visitChildren(fencedCodeBlock);
	}

	@Override
	public void visit(HardLineBreak hardLineBreak) {
		visitChildren(hardLineBreak);
	}

	@Override
	public void visit(Heading heading) {
		visitChildren(heading);
	}

	@Override
	public void visit(ThematicBreak thematicBreak) {
		visitChildren(thematicBreak);
	}

	@Override
	public void visit(HtmlInline htmlInline) {
		visitChildren(htmlInline);
	}

	@Override
	public void visit(HtmlBlock htmlBlock) {
		visitChildren(htmlBlock);
	}

	@Override
	public void visit(Image image) {
		visitChildren(image);
	}

	@Override
	public void visit(IndentedCodeBlock indentedCodeBlock) {
		visitChildren(indentedCodeBlock);
	}

	@Override
	public void visit(Link link) {
		visitChildren(link);
	}

	@Override
	public void visit(ListItem listItem) {
		visitChildren(listItem);
	}

	@Override
	public void visit(OrderedList orderedList) {
		visitChildren(orderedList);
	}

	@Override
	public void visit(Paragraph paragraph) {
		visitChildren(paragraph);
	}

	@Override
	public void visit(SoftLineBreak softLineBreak) {
		visitChildren(softLineBreak);
	}

	@Override
	public void visit(StrongEmphasis strongEmphasis) {
		visitChildren(strongEmphasis);
	}

	@Override
	public void visit(CustomNode customNode) {

		visitChildren(customNode);
	}

	/**
	 * Visit the child nodes.
	 *
	 * @param parent
	 *            the parent node whose children should be visited
	 */
	protected void visitChildren(org.commonmark.node.Node parent) {
		org.commonmark.node.Node node = parent.getFirstChild();
		while (node != null) {
			// A subclass of this visitor might modify the node, resulting in
			// getNext returning a different node or no
			// node after visiting it. So get the next node before visiting.
			org.commonmark.node.Node next = node.getNext();
			node.accept(this);
			node = next;
		}
	}
}