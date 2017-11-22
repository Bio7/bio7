package com.eco.bio7.markdownedit.parser;

import com.vladsch.flexmark.ast.CustomNode;
import com.vladsch.flexmark.ast.Visitor;

public abstract class CustomRMarkdownNode extends CustomNode {
	
    public void accept(Visitor visitor) {
    	
        visitor.visit(this);
       
    }
}