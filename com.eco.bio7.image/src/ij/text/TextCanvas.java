package ij.text;

import ij.util.Java2;
import java.awt.*;
import java.awt.event.*;

import com.eco.bio7.image.Util;

class TextCanvas extends Canvas {

	TextPanel tp;
	Font fFont;
	FontMetrics fMetrics;
	Graphics gImage;
	Image iImage;
	boolean antialiased;
	Color swtBackgroundToAWT = Util.getSWTBackgroundToAWT();
	Color swtForegroundToAWT = Util.getSWTForegroundToAWT();
	boolean themeBlack = Util.isThemeBlack();
	
	TextCanvas(TextPanel tp) {
		this.tp = tp;
		addMouseListener(tp);
		addMouseMotionListener(tp);
		addKeyListener(tp);
		addMouseWheelListener(tp);
	}

	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		tp.adjustVScroll();
		tp.adjustHScroll();
		iImage = null;
	}

	public void update(Graphics g) {
		paint(g);
	}

	public void paint(Graphics g) {
		if (tp == null || g == null)
			return;
		Dimension d = getSize();
		int iWidth = d.width;
		int iHeight = d.height;

		if (iWidth <= 0 || iHeight <= 0)
			return;
		g.setColor(Color.lightGray);
		if (iImage == null)
			makeImage(iWidth, iHeight);
		if (tp.iRowHeight == 0 || (tp.iColWidth.length > 0 && tp.iColWidth[0] == 0 && tp.iRowCount > 0)) {
			tp.iRowHeight = fMetrics.getHeight() + 2;
			for (int i = 0; i < tp.iColCount; i++)
				calcAutoWidth(i);
			tp.adjustHScroll();
			tp.adjustVScroll();
		}
		/*Changed for Bio7!*/
	
		
		if (themeBlack) {
			gImage.setColor(swtBackgroundToAWT);
		} else {
			gImage.setColor(Color.white);
		}
		gImage.fillRect(0, 0, iWidth, iHeight);
		if (tp.headings)
			drawColumnLabels(iWidth);
		int y = tp.iRowHeight + 1 - tp.iY;
		int j = 0;
		while (y < tp.iRowHeight + 1) {
			j++;
			y += tp.iRowHeight;
		}
		tp.iFirstRow = j;
		y = tp.iRowHeight + 1;
		for (; y < iHeight && j < tp.iRowCount; j++, y += tp.iRowHeight) {
			int x = -tp.iX;
			for (int i = 0; i < tp.iColCount; i++) {
				if (i>=tp.iColWidth.length) break;
				int w = tp.iColWidth[i];
				/*Changed for Bio7!*/
				Color b, t;
				
				if (themeBlack) {
					b = swtBackgroundToAWT;
					t = swtForegroundToAWT;
				} else {
					b = Color.white;
					t = Color.black;
				}
				if (j >= tp.selStart && j <= tp.selEnd) {
					int w2 = w;
					if (tp.iColCount == 1)
						w2 = iWidth;
					/*Changed for Bio7!*/
					if (themeBlack) {
						b = swtBackgroundToAWT;
						t = swtForegroundToAWT;
					} else {
						b = Color.black;
						t = Color.white;
					}
					gImage.setColor(b);
					gImage.fillRect(x, y, w2 - 1, tp.iRowHeight);
				}
				gImage.setColor(t);
				char[] chars = tp.getChars(i,j);
				if (chars != null)
					gImage.drawChars(chars, 0, chars.length, x + 2, y + tp.iRowHeight - 5);
				x += w;
			}
		}
		if (iImage != null)
			g.drawImage(iImage, 0, 0, null);
	}

	void makeImage(int iWidth, int iHeight) {
		iImage = createImage(iWidth, iHeight);
		if (gImage != null)
			gImage.dispose();
		gImage = iImage.getGraphics();
		gImage.setFont(fFont);
		Java2.setAntialiasedText(gImage, antialiased);
		if (fMetrics==null)
			fMetrics = gImage.getFontMetrics();
	}

	void drawColumnLabels(int iWidth) {
		gImage.setColor(Color.darkGray);
		gImage.drawLine(0, tp.iRowHeight, iWidth, tp.iRowHeight);
		int x = -tp.iX;
		for (int i = 0; i < tp.iColCount; i++) {
			int w = tp.iColWidth[i];
			gImage.setColor(Color.lightGray);
			gImage.fillRect(x + 1, 0, w, tp.iRowHeight);
			gImage.setColor(Color.black);
			if (i<tp.sColHead.length && tp.sColHead[i]!=null)
				gImage.drawString(tp.sColHead[i], x + 2, tp.iRowHeight - 5);
			if (tp.iColCount > 0) {
				gImage.setColor(Color.darkGray);
				gImage.drawLine(x + w - 1, 0, x + w - 1, tp.iRowHeight - 1);
				gImage.setColor(Color.white);
				gImage.drawLine(x + w, 0, x + w, tp.iRowHeight - 1);
			}
			x += w;
		}
		gImage.setColor(Color.lightGray);
		gImage.fillRect(0, 0, 1, tp.iRowHeight);
		gImage.fillRect(x + 1, 0, iWidth - x, tp.iRowHeight);
		//gImage.drawLine(0,0,0,iRowHeight-1);
		gImage.setColor(Color.darkGray);
		gImage.drawLine(0, 0, iWidth, 0);
	}

	void calcAutoWidth(int column) {
		if (tp.sColHead == null || column >= tp.iColWidth.length || gImage == null)
			return;
		if (fMetrics == null)
			fMetrics = gImage.getFontMetrics();
		int w = 15;
		int maxRows = 20;
		if (column == 0 && tp.sColHead[0].equals(" "))
			w += 5;
		else {
			char[] chars = tp.sColHead[column].toCharArray();
			w = Math.max(w, fMetrics.charsWidth(chars, 0, chars.length));
		}
		int rowCount = Math.min(tp.iRowCount, maxRows);
		for (int row = 0; row < rowCount; row++) {
			char[] chars = tp.getChars(column,row);
			if (chars != null)
				w = Math.max(w, fMetrics.charsWidth(chars, 0, chars.length));
		}
		//System.out.println("calcAutoWidth: "+column+"  "+tp.iRowCount);
		char[] chars = tp.iRowCount>0?tp.getChars(column, tp.iRowCount-1):null;
		if (chars != null)
			w = Math.max(w, fMetrics.charsWidth(chars, 0, chars.length));
		if (column < tp.iColWidth.length)
			tp.iColWidth[column] = w + 15;
	}

}
