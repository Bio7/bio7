/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.eco.bio7.floweditor.figures;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import com.eco.bio7.floweditor.shapes.ShapesPlugin;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;

public class FileFigure extends Figure implements MouseListener {

	public Color BG = new Color(null, 242, 240, 255);

	static Image classImage = ShapesPlugin.getImageDescriptor("/icons/file.png").createImage();

	static Font BOLD = new Font(null, "", 10, SWT.BOLD);

	public Label header;

	public FileFigure() {
		addMouseListener(this);
		class SeparatorBorder extends MarginBorder {
			SeparatorBorder() {
				super(3, 5, 3, 5);
			}

			public void paint(IFigure figure, Graphics graphics, Insets insets) {
				Rectangle where = getPaintRectangle(figure, insets);
				graphics.drawLine(where.getTopLeft(), where.getTopRight());
			}
		}

		header = new Label("File", classImage);
		header.setFont(BOLD);
		header.setBorder(new MarginBorder(3, 5, 3, 5));

		Figure attributes = new Figure();
		ToolbarLayout layout;
		attributes.setLayoutManager(layout = new ToolbarLayout());
		layout.setStretchMinorAxis(false);

		setBorder(new LineBorder());
		setLayoutManager(new ToolbarLayout());

		add(header);

		add(attributes);

		setOpaque(true);
		setBackgroundColor(BG);
	}

	public void mouseDoubleClicked(MouseEvent arg0) {
	}

	public void mousePressed(MouseEvent evt) {

	}

	public void mouseReleased(MouseEvent arg0) {

	}

}
