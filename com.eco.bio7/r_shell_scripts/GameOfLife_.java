import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;

/*An ImageJ Game of Life example with two*/
public class GameOfLife_ implements PlugIn {

	private ImagePlus imp;
	private int WIDTH = 1000;
	private int HEIGHT = 1000;
	private int[][] temp;
	private int[] state = { 0, 255 };
	private Timer timer;
	private int w;
	private int h;
	private ByteProcessor ip;

	public void run(String arg) {
		/* Create an image with random pixels (state 0,255)! */
		ip = new ByteProcessor(WIDTH, HEIGHT);
		ip.setColor(Color.white);
		ip.fill();

		w = ip.getWidth();
		h = ip.getHeight();
		temp = new int[w][h];
		for (int u = 0; u < h; u++) {
			for (int v = 0; v < w; v++) {
				int b = (int) (Math.random() * 2);
				int p = ip.getPixel(v, u);
				if (b == 1) {
					ip.putPixel(v, u, state[1]);
				} else {
					ip.putPixel(v, u, state[0]);
				}

			}
		}
		imp = new ImagePlus("Game of Life", ip);
		imp.show();
       /*Create and start a timer!*/
		timer = new javax.swing.Timer(25, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (IJ.escapePressed()) {
					timer.stop();
					IJ.resetEscape();
				}
				gameOfLife();
			}
		});
		timer.start();
	}

	public void gameOfLife() {
		/* The Game of Life routine ! */

		for (int i = 0; i < h; i++) {
			for (int u = 0; u < w; u++) {

				int modi = ((i + 1 + h) % (h));// Modulo, no border !
				int modu = ((u + 1 + w) % (w));
				int modni = ((i - 1 + h) % (h));
				int modnu = ((u - 1 + w) % (w));

				int x = ((ip.getPixel(modnu, modni)) + (ip.getPixel(u, modni)) + (ip.getPixel(modu, modni))
						+ (ip.getPixel(modu, i)) + (ip.getPixel(modu, modi)) + (ip.getPixel(u, modi))
						+ (ip.getPixel(modnu, modi)) + (ip.getPixel(modnu, i)));

				if (x == 510 // 255=1 + 255=1
						&& ip.getPixel(u, i) == 255 || x == 765 && ip.getPixel(u, i) == 0
						|| x == 765 && ip.getPixel(u, i) == 255) {

					temp[u][i] = state[1];
				} else {
					temp[u][i] = state[0];

				}

			}
		}

		for (int i = 0; i < h; i++) {
			for (int u = 0; u < w; u++) {

				ip.putPixel(u, i, temp[u][i]);

			}
		}

		imp.updateAndDraw();

	}
}