import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.process.ColorProcessor;
import ij.gui.GenericDialog;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

public class KMeans_Segment implements PlugIn {
    // default parameters, also used for macro recording
    static int k = 3; // number of clusters
    static int maxIter = 20; // maximum iterations

    public void run(String arg) {
        // ask user for parameters
        GenericDialog gd = new GenericDialog("K-Means Segmentation");
        gd.addNumericField("Number_of_clusters:", k, 0);
        gd.addNumericField("Maximum_iterations:", maxIter, 0);
        gd.showDialog();
        if (gd.wasCanceled()) return;
        k = (int) gd.getNextNumber();
        maxIter = (int) gd.getNextNumber();

        ImagePlus imp = IJ.getImage();
        if (imp == null) { IJ.noImage(); return; }
        int type = imp.getType();
        if (type != ImagePlus.GRAY8 && type != ImagePlus.COLOR_RGB) {
            IJ.error("KMeans_Segment", "Only 8-bit grayscale or RGB images are supported.");
            return;
        }
        ImageProcessor ip = imp.getProcessor();
        int w = ip.getWidth();
        int h = ip.getHeight();
        int n = w * h;

        // data arrays for R,G,B (for grayscale the G and B arrays will be identical)
        int[] rData = new int[n];
        int[] gData = new int[n];
        int[] bData = new int[n];

        if (type == ImagePlus.GRAY8) {
            byte[] pixels = (byte[]) ip.getPixels();
            for (int i = 0; i < n; i++) {
                int v = pixels[i] & 0xff;
                rData[i] = gData[i] = bData[i] = v;
            }
        } else { // RGB
            int[] pixels = (int[]) ip.getPixels(); // packed 0xRRGGBB
            for (int i = 0; i < n; i++) {
                int p = pixels[i];
                rData[i] = (p >> 16) & 0xff;
                gData[i] = (p >> 8) & 0xff;
                bData[i] = p & 0xff;
            }
        }

        // initialize centroids randomly from the data
        int[] rCent = new int[k];
        int[] gCent = new int[k];
        int[] bCent = new int[k];
        Random rand = new Random();
        for (int i = 0; i < k; i++) {
            int idx = rand.nextInt(n);
            rCent[i] = rData[idx];
            gCent[i] = gData[idx];
            bCent[i] = bData[idx];
        }

        int[] labels = new int[n];
        boolean changed = true;
        int iter = 0;
        while (changed && iter < maxIter) {
            // ---- assignment (parallel) ----
            AtomicBoolean changedFlag = new AtomicBoolean(false);
            IntStream.range(0, n).parallel().forEach(i -> {
                int best = 0;
                int dr = rData[i] - rCent[0];
                int dg = gData[i] - gCent[0];
                int db = bData[i] - bCent[0];
                int bestDist = dr * dr + dg * dg + db * db;
                for (int c = 1; c < k; c++) {
                    dr = rData[i] - rCent[c];
                    dg = gData[i] - gCent[c];
                    db = bData[i] - bCent[c];
                    int dist = dr * dr + dg * dg + db * db;
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = c;
                    }
                }
                if (labels[i] != best) {
                    changedFlag.set(true);
                    labels[i] = best;
                }
            });
            changed = changedFlag.get();

            // ---- update centroids (sequential, fast enough) ----
            int[] rSum = new int[k];
            int[] gSum = new int[k];
            int[] bSum = new int[k];
            int[] count = new int[k];
            for (int i = 0; i < n; i++) {
                int lbl = labels[i];
                rSum[lbl] += rData[i];
                gSum[lbl] += gData[i];
                bSum[lbl] += bData[i];
                count[lbl]++;
            }
            for (int c = 0; c < k; c++) {
                if (count[c] != 0) {
                    rCent[c] = rSum[c] / count[c];
                    gCent[c] = gSum[c] / count[c];
                    bCent[c] = bSum[c] / count[c];
                }
            }
            iter++;
        }

        // ---- create result image (parallel) ----
        ImageProcessor ipResult;
        if (type == ImagePlus.GRAY8) {
            ipResult = ip.createProcessor(w, h);
            byte[] out = (byte[]) ipResult.getPixels();
            IntStream.range(0, n).parallel().forEach(i -> out[i] = (byte) rCent[labels[i]]);
        } else { // RGB
            ipResult = new ColorProcessor(w, h);
            int[] out = (int[]) ipResult.getPixels();
            IntStream.range(0, n).parallel().forEach(i -> {
                int r = rCent[labels[i]];
                int g = gCent[labels[i]];
                int b = bCent[labels[i]];
                out[i] = (r << 16) | (g << 8) | b;
            });
        }

        ImagePlus result = new ImagePlus(imp.getTitle() + "_K" + k, ipResult);
        result.show();
        IJ.log("K-means finished: iterations=" + iter + ", k=" + k);
    }
}
