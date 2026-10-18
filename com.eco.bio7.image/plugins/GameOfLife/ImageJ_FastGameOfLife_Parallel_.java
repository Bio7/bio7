/*This example uses parallel processing with Java Streams. In additon a 16-bit image is used
  with bit-shift - instead of a temporary array!
*/
import ij.*;
import ij.process.*;
import ij.plugin.PlugIn;
import java.util.stream.IntStream;

public class ImageJ_FastGameOfLife_Parallel_ implements PlugIn {
    int WIDTH = 3000, HEIGHT = 3000, n = 1000;

    public void run(String arg) {
        ShortProcessor ip = new ShortProcessor(WIDTH, HEIGHT);
        short[] pixels = (short[]) ip.getPixels();
        for (int i = 0; i < pixels.length; i++) pixels[i] = (short)(Math.random() > 0.5 ? 255 : 0);

        ImagePlus imp = new ImagePlus("Turbo Game of Life", ip);
		imp.setDisplayRange(0, 255);
        imp.show();

        // Pre-calculate neighbor offsets for periodic boundaries
        int[] x_m = new int[WIDTH], x_p = new int[WIDTH];
        int[] y_m = new int[HEIGHT], y_p = new int[HEIGHT];
        for (int i = 0; i < WIDTH; i++) { x_m[i] = (i - 1 + WIDTH) % WIDTH; x_p[i] = (i + 1) % WIDTH; }
        for (int i = 0; i < HEIGHT; i++) { y_m[i] = ((i - 1 + HEIGHT) % HEIGHT) * WIDTH; y_p[i] = ((i + 1) % HEIGHT) * WIDTH; }

        for (int it = 0; it < n; it++) {
            if (IJ.escapePressed()) break;

            // Parallel Processing using Java Streams
            IntStream.range(0, HEIGHT).parallel().forEach(y -> {
                int y_offset = y * WIDTH;
                for (int x = 0; x < WIDTH; x++) {
                    int neighbors = (pixels[x_m[x] + y_m[y]] & 0xff) + (pixels[x     + y_m[y]] & 0xff) + (pixels[x_p[x] + y_m[y]] & 0xff)
                                  + (pixels[x_m[x] + y_offset] & 0xff)                                + (pixels[x_p[x] + y_offset] & 0xff)
                                  + (pixels[x_m[x] + y_p[y]] & 0xff) + (pixels[x     + y_p[y]] & 0xff) + (pixels[x_p[x] + y_p[y]] & 0xff);

                    int currentState = pixels[x + y_offset] & 0xff;
                    int nextState = (neighbors == 765 || (neighbors == 510 && currentState == 255)) ? 255 : 0;
                    
                    // Encode next gen in high byte
                    pixels[x + y_offset] = (short)((nextState << 8) | currentState);
                }
            });

            // Single pass bit-shift to advance generation
           for (int i = 0; i < pixels.length; i++) pixels[i] = (short)((pixels[i] & 0xffff) >>> 8);
            
            imp.updateAndDraw();
            //To avoid flickering adjust the following wait (Thread.sleep) value!
 			IJ.wait(5);
            IJ.showStatus("Iteration: " + it);
 
        }
    }
}
