import ij.*;
import ij.process.*;
import ij.plugin.*;
import ij.gui.ImageWindow;
import java.awt.Color;
import java.util.Random;

/** This is a plugin version of the PlasmaCloud.java program from
    Section 2.3 (Recursion) of the book "Introduction to Programming in Java"
    by Robert Sedgewick and Kevin Wayne.
    http://introcs.cs.princeton.edu/java/23recursion/PlasmaCloud.java.html
*/
public class Plasma_Cloud implements PlugIn {
    private int w=3000, h=3000;
    private int n = 250;
    private Random ran = new Random();
    private ImageProcessor ip = new ColorProcessor(w,h);
    private ImagePlus imp;
    double c1 = ran.nextDouble();
    double c2 = ran.nextDouble();
    double c3 = ran.nextDouble();
    double c4 = ran.nextDouble();

    public void run(String arg) {
    IJ.resetEscape();
    long t0 = System.currentTimeMillis();
    for (int i=0; i<n; i++) {
        c1 += ran.nextGaussian()*0.003;
        c2 += ran.nextGaussian()*0.003;
        c3 += ran.nextGaussian()*0.003;
        c4 += ran.nextGaussian()*0.003;
        long t1 = System.nanoTime();
        update();
        long time = (System.nanoTime() - t1)/1000000;
        ImageWindow win = imp.getWindow();
        if (IJ.escapePressed() || win==null || !win.isVisible()) {
            IJ.beep();
            break;
        };
        IJ.showStatus((i+1)+"/"+n+" ("+time+"ms)");
        IJ.wait(25);
     }
     long time = System.currentTimeMillis() - t0;
     String fps = IJ.d2s(n/(time/1000),1);
     imp.setTitle("Plasma Clouds ("+fps+" fps)");
    }

    private void update() {
        // choose intial corner colors at random betwen 0 and 1
        ran = new Random(0);
        double stddev = 0.75; // controls color variation
        plasma(0.5, 0.5, 0.5, stddev, c1, c2, c3, c4);
        if (imp==null) {
            imp = new ImagePlus("Plasma Clouds", ip);
            imp.show();
        } else {
            imp.updateAndDraw();
            IJ.getInstance().toFront();
        }
    }

    // centered at (x, y), of given size, using given standard deviation for computing the
    // displacement, and with upper left, upper right, lower lower, and lower right hues c1, c2, c3, c4
     private void plasma(double x,  double y,  double size, double stddev,
         double c1, double c2, double c3,   double c4) {

        // base case
        if (size<=0.001) return;

        // calculate new color of midpoint using random displacement
        double displacement = ran.nextGaussian()*stddev;
        double cM = (c1 + c2 + c3 + c4) / 4.0 + displacement;

        // draw a colored square
        Color color = Color.getHSBColor((float) cM, 0.8f, 0.8f);
        ip.setColor(color);
        int w2 = (int)Math.round(size*w*2);
        int h2 = (int)Math.round(size*h*2);
        int x2 = (int)Math.round((x-size)*w);
        int y2 = (int)Math.round((y-size)*h);
        ip.fillRect(x2, y2, w2, h2);
 
        double cT = (c1 + c2) / 2.0;    // top
        double cB = (c3 + c4) / 2.0;    // bottom
        double cL = (c1 + c3) / 2.0;    // left
        double cR = (c2 + c4) / 2.0;    // right

        plasma(x - size/2, y - size/2, size/2, stddev/2, cL, cM, c3, cB);
        plasma(x + size/2, y - size/2, size/2, stddev/2, cM, cR, cB, c4);
        plasma(x - size/2, y + size/2, size/2, stddev/2, c1, cT, cL, cM);
        plasma(x + size/2, y + size/2, size/2, stddev/2, cT, c2, cM, cR);
    }

}
