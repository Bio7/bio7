/*******************************************************************************
 * Based on:
 * CBraceRule from the package org.eclipse.cdt.internal.ui.text
 ******************************************************************************/
package com.eco.bio7.javaeditors;

import org.eclipse.jface.text.rules.IToken;

public class JavaEditorBraceRule extends SingleCharRule {

    /**
     * Creates new rule. 
     * @param token Style token.
     */
    public JavaEditorBraceRule(IToken token)
    {
        super(token);
    }

    // TODO: SC implementation
    @Override
	protected boolean isRuleChar(int ch)
    {
        return ch == '{' || ch == '}' || ch == '[' || ch == ']' || ch == '(' || ch == ')';
    }

}