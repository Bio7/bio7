package com.eco.bio7.markdownedit.parser;

import org.commonmark.ext.front.matter.YamlFrontMatterNode;
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Text;

public class Yamlheader extends YamlFrontMatterVisitor {

	/*@Override
	public void visit(Text text) {
		System.out.println("YAML Header= " + text);
		// visitChildren(text);
	}*/

	@Override
	public void visit(CustomNode customNode) {
		if (customNode instanceof YamlFrontMatterNode) {
			System.out.println("YAML Header= " + ((YamlFrontMatterNode) customNode).getValues());
			// data.put(((YamlFrontMatterNode) customNode).getKey(),
			// ((YamlFrontMatterNode) customNode).getValues());
		} else {
			super.visit(customNode);
		}
	}
}