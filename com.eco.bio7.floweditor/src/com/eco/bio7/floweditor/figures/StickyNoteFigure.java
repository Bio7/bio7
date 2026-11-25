/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.floweditor.figures;

import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.text.FlowPage;
import org.eclipse.draw2d.text.ParagraphTextLayout;
import org.eclipse.draw2d.text.TextFlow;

/**
 * A note of a flow with wrapped text (left: room for the info icon, right: for
 * the folded corner).
 */
public class StickyNoteFigure extends BentCornerFigure {

	private TextFlow textFlow;

	public StickyNoteFigure() {
		setBorder(new MarginBorder(10, 32, 10, DEFAULT_CORNER_SIZE + 4));
		FlowPage flowPage = new FlowPage();
		textFlow = new TextFlow();
		textFlow.setLayoutManager(new ParagraphTextLayout(textFlow, ParagraphTextLayout.WORD_WRAP_SOFT));
		flowPage.add(textFlow);
		setLayoutManager(new StackLayout());
		add(flowPage);
	}

	public String getText() {
		return textFlow.getText();
	}

	public void setText(String newText) {
		textFlow.setText(newText == null ? "" : newText);
	}
}
