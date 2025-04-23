/*******************************************************************************
 * Based on:
 * CBraceRule from the package org.eclipse.cdt.internal.ui.text
 ******************************************************************************/
package com.eco.bio7.reditors;

import org.eclipse.jface.text.rules.IToken;

public class REditorBraceRule extends SingleCharRule {

    /**
     * Creates new rule. 
     * @param token Style token.
     */
    public REditorBraceRule(IToken token)
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