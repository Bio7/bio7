package com.eco.bio7.image;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JMenuBar;

public class BlackMenuBar extends JMenuBar {
	Color bgColor = Color.BLACK;
	Color fgColor = Util.getSWTBackgroundToAWT();

	public void setColor(Color color) {
		bgColor = color;
	}

	public void setForeground(Color fg) {
		// TODO Auto-generated method stub
		super.setForeground(fg);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setColor(bgColor);
		g2d.fillRect(0, 0, getWidth() - 1, getHeight() - 1);

	}
}