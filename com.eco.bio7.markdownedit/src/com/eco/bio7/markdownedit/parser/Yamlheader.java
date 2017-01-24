package com.eco.bio7.markdownedit.parser;

import java.util.Stack;

import org.commonmark.ext.front.matter.YamlFrontMatterNode;
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Text;
import com.eco.bio7.markdownedit.editors.MarkdownEditor;
import com.eco.bio7.markdownedit.outline.MarkdownEditorOutlineNode;

public class Yamlheader extends YamlFrontMatterVisitor {

	private MarkdownEditor markdownEditor;
	private Stack<MarkdownEditorOutlineNode> methods;

	

	public Yamlheader(Stack<MarkdownEditorOutlineNode> methods, MarkdownEditor markdownEditor) {
		this.markdownEditor=markdownEditor;
		this.methods=methods;
	}

	@Override
	public void visit(CustomNode customNode) {
		if (customNode instanceof YamlFrontMatterNode) {
			System.out.println("YAML Header= Key= " + ((YamlFrontMatterNode) customNode).getKey()+"Value=  "+ customNode.toString());
			//(YamlFrontMatterNode) customNode).getValues()
			
			//methods.push(new MarkdownEditorOutlineNode("", 0, "RMarkdown",markdownEditor.baseNode));
			// data.put(((YamlFrontMatterNode) customNode).getKey(),
			// ((YamlFrontMatterNode) customNode).getValues());
		} else {
			super.visit(customNode);
		}
	}
}