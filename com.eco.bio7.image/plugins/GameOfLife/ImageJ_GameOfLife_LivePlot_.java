import ij.*;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.process.*;
import ij.plugin.PlugIn;

import java.awt.Color;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.eclipse.swt.widgets.Display;

/**
 * Fast Parallel Game of Life for (SWT)ImageJ (ByteProcessor version)
 * with two dynamic, resizable plot windows:
 *  - a line graph of living and dead cells over the generations
 *  - a colored pie chart (drop shadow, legend with counts) in a Plot
 *
 * All plot creation and window access runs on the SWT UI thread (Display.syncExec).
 * Each refresh takes the current frame size of the open window, so a window the
 * user has resized keeps its size.
 */
public class ImageJ_GameOfLife_LivePlot_ implements PlugIn {

    int WIDTH = 5000, HEIGHT = 5000, n = 1000;

    // ---- Plot settings ----
    int     PLOT_EVERY = 1;      // refresh both plots every N generations
    boolean SHOW_DEAD  = true;   // also draw the dead-cell curve in the line graph
    boolean PERCENT    = false;  // line graph in percent instead of absolute counts
    boolean LOG_Y      = false;  // logarithmic y-axis in the line graph
    boolean SHOW_PIE   = true;   // show the pie chart plot

    static final int LINE_W = 520, LINE_H = 300;   // initial frame sizes (pixels)
    static final int PIE_W  = 420, PIE_H  = 320;

    // state names and colors, shared by both plots
    static final String[] STATE_NAMES  = {"Alive", "Dead"};
    static final Color[]  STATE_COLORS = {new Color(0x2ca02c), new Color(0x7f7f7f)};

    // state of the pie plot, used by the resize watcher
    private volatile PlotWindow pieWindow;
    private volatile double[]   pieValues;
    private volatile int        pieGeneration;
    private volatile int        pieFrameW = -1, pieFrameH = -1;
    private volatile boolean    simulationRunning;

    public void run(String arg) {
        simulationRunning = true;
        // ---------- Simulation setup ----------
        ByteProcessor ip = new ByteProcessor(WIDTH, HEIGHT);
        byte[] pixelsA = (byte[]) ip.getPixels();
        byte[] pixelsB = new byte[WIDTH * HEIGHT];

        long total = (long) WIDTH * HEIGHT;
        long aliveInit = 0;
        for (int i = 0; i < pixelsA.length; i++) {
            boolean alive = Math.random() > 0.5;
            pixelsA[i] = (byte) (alive ? 255 : 0);
            if (alive) aliveInit++;
        }

        ImagePlus imp = new ImagePlus("Turbo Game of Life (Byte)", ip);
        imp.setDisplayRange(0, 255);
        imp.show();

        int[] x_m = new int[WIDTH], x_p = new int[WIDTH];
        int[] y_m = new int[HEIGHT], y_p = new int[HEIGHT];
        for (int i = 0; i < WIDTH; i++) {
            x_m[i] = (i - 1 + WIDTH) % WIDTH;
            x_p[i] = (i + 1) % WIDTH;
        }
        for (int i = 0; i < HEIGHT; i++) {
            y_m[i] = ((i - 1 + HEIGHT) % HEIGHT) * WIDTH;
            y_p[i] = ((i + 1) % HEIGHT) * WIDTH;
        }

        byte[][] buffers = {pixelsA, pixelsB};

        // ---------- Monitoring data ----------
        double[] gens  = new double[n + 1];
        double[] alive = new double[n + 1];
        double[] dead  = new double[n + 1];
        gens[0]  = 0;
        alive[0] = scale(aliveInit, total);
        dead[0]  = scale(total - aliveInit, total);
        int count = 1;

        final int[] rowAlive = new int[HEIGHT];   // per-row living-cell counts

        PlotWindow lineWin = showLinePlot(null, gens, alive, dead, count);
        positionNextTo(lineWin, imp);
        PlotWindow pieWin = null;
        if (SHOW_PIE)
            pieWin = showPiePlot(null, new double[] {aliveInit, total - aliveInit}, 0);
        if (SHOW_PIE)
            startPieResizeWatcher();

        // ---------- Main loop ----------
        for (int it = 0; it < n; it++) {
            if (IJ.escapePressed()) break;

            final byte[] currentGen = buffers[it % 2];
            final byte[] nextGen    = buffers[(it + 1) % 2];

            IntStream.range(0, HEIGHT).parallel().forEach(y -> {
                int y_offset = y * WIDTH;
                int ym = y_m[y];
                int yp = y_p[y];
                int rowCount = 0;

                for (int x = 0; x < WIDTH; x++) {
                    int neighbors = (currentGen[x_m[x] + ym] & 0xff) + (currentGen[x + ym] & 0xff) + (currentGen[x_p[x] + ym] & 0xff)
                                  + (currentGen[x_m[x] + y_offset] & 0xff)                         + (currentGen[x_p[x] + y_offset] & 0xff)
                                  + (currentGen[x_m[x] + yp] & 0xff) + (currentGen[x + yp] & 0xff) + (currentGen[x_p[x] + yp] & 0xff);

                    int currentState = currentGen[x + y_offset] & 0xff;

                    if (neighbors == 765 || (neighbors == 510 && currentState == 255)) {
                        nextGen[x + y_offset] = (byte) 255;
                        rowCount++;
                    } else {
                        nextGen[x + y_offset] = (byte) 0;
                    }
                }
                rowAlive[y] = rowCount;   // each thread writes only its own row
            });

            long aliveNow = 0;
            for (int c : rowAlive) aliveNow += c;
            gens[count]  = it + 1;
            alive[count] = scale(aliveNow, total);
            dead[count]  = scale(total - aliveNow, total);
            count++;

            ip.setPixels(nextGen);
            imp.updateAndDraw();

            if ((it + 1) % PLOT_EVERY == 0 || it == n - 1) {
                lineWin = showLinePlot(lineWin, gens, alive, dead, count);
                if (SHOW_PIE)
                    pieWin = showPiePlot(pieWin, new double[] {aliveNow, total - aliveNow}, it + 1);
            }

            IJ.showStatus("Iteration: " + (it + 1) + "  alive: " + aliveNow
                    + "  (Press Esc to stop)");
        }
        // final refresh (e.g. after Esc)
        showLinePlot(lineWin, gens, alive, dead, count);
        simulationRunning = false;   // watcher keeps running until the pie window is closed
    }

    // =================================================================
    //  Line graph
    // =================================================================
    private PlotWindow showLinePlot(PlotWindow win, double[] gens, double[] alive,
                                    double[] dead, int count) {
        return showOnUiThread(win, openWin -> {
            double[] x = Arrays.copyOf(gens,  count);
            double[] a = Arrays.copyOf(alive, count);
            double[] d = Arrays.copyOf(dead,  count);

            // Large counts would be shown as 2.5E7 etc. -> scale to thousands/millions
            String yLabel = "Cells (%)";
            if (!PERCENT) {
                double max = 0;
                for (int i = 0; i < count; i++) {
                    max = Math.max(max, a[i]);
                    if (SHOW_DEAD) max = Math.max(max, d[i]);
                }
                double f = 1;
                yLabel = "Cells";
                if (max >= 1e6)      { f = 1e6; yLabel = "Cells (millions)"; }
                else if (max >= 1e4) { f = 1e3; yLabel = "Cells (thousands)"; }
                for (int i = 0; i < count; i++) { a[i] /= f; d[i] /= f; }
            }

            Plot plot = new Plot("Game of Life - cell states", "Generation", yLabel);
            int[] fs = frameSize(openWin, LINE_W, LINE_H);   // keep user's window size
            plot.setFrameSize(fs[0], fs[1]);
            plot.setLineWidth(2);

            plot.setColor(STATE_COLORS[0]);
            plot.addPoints(x, a, Plot.LINE);
            if (SHOW_DEAD) {
                plot.setColor(STATE_COLORS[1]);
                plot.addPoints(x, d, Plot.LINE);
            }
            plot.setColor(Color.black);
            // legend with the current amount of each state (last generation)
            double aNow = alive[count - 1], dNow = dead[count - 1];
            String aTxt = STATE_NAMES[0] + ": " + stateAmount(aNow, aNow + dNow);
            String dTxt = STATE_NAMES[1] + ": " + stateAmount(dNow, aNow + dNow);
            plot.addLegend(SHOW_DEAD ? aTxt + "\t" + dTxt : aTxt);

            if (LOG_Y) plot.setAxisYLog(true);
            plot.setLimitsToFit(false);
            double[] lim = plot.getLimits();
            plot.setLimits(0, Math.max(10, x[count - 1]), lim[2], lim[3]);
            return plot;
        });
    }

    // =================================================================
    //  Pie chart with drop shadow and a legend
    //  showing names, counts and percentages. Drawn entirely in a Plot.
    // =================================================================
    static final double PIE_R      = 1.0;    // pie radius (data units)
    static final double SHADOW_OFF = 0.07;   // shadow offset (down-right)
    static final int    LEGEND_PX  = 230;    // horizontal room for the legend (pixels)

    private PlotWindow showPiePlot(PlotWindow win, double[] values, int generation) {
        pieValues = values.clone();
        pieGeneration = generation;
        PlotWindow result = showOnUiThread(win, openWin -> {
            Plot plot = new Plot("Game of Life - state distribution", "", "");
            int[] fs = frameSize(openWin, PIE_W, PIE_H);   // keep user's window size
            plot.setFrameSize(fs[0], fs[1]);
            pieFrameW = fs[0];                             // remember the size we drew for
            pieFrameH = fs[1];
            plot.setFormatFlags(0);                        // no axis numbers, ticks, grid

            // ---- equal x/y scaling: pie on the left, legend space on the right ----
            double box   = 2 * (PIE_R + 0.2);                          // data units for the pie
            int    boxPx = Math.max(80, Math.min(fs[1], fs[0] - LEGEND_PX));
            double s     = box / boxPx;                                // data units per pixel
            double xMin  = -box / 2, xMax = xMin + s * fs[0];
            double yMin  = -s * fs[1] / 2, yMax = -yMin;
            plot.setLimits(xMin, xMax, yMin, yMax);

            double sum = 0;
            for (double v : values) sum += v;
            if (sum <= 0) sum = 1;

            // ---- soft drop shadow: stacked discs, light & large -> dark & small ----
            int layers = 6;
            for (int i = 0; i < layers; i++) {
                double r = PIE_R + 0.06 * (layers - 1 - i) / (layers - 1);
                int g = 240 - i * 11;
                Color c = new Color(g, g, g);
                fillStar(plot, SHADOW_OFF, -SHADOW_OFF, r, 0, 1, c);
            }

            // ---- slices ----
            double start = 0;
            for (int st = 0; st < values.length; st++) {
                double frac = values[st] / sum;
                if (frac <= 0) continue;
                fillStar(plot, 0, 0, PIE_R, start, start + frac, STATE_COLORS[st]);
                start += frac;
            }

            // ---- white separator lines between slices (only if >1 slice) ----
            int nonZero = 0;
            for (double v : values) if (v > 0) nonZero++;
            if (nonZero > 1) {
                plot.setColor(Color.white);
                plot.setLineWidth(2);
                double a = 0;
                for (double v : values) {
                    if (v <= 0) continue;
                    double t = 2 * Math.PI * a;
                    plot.drawLine(0, 0, PIE_R * Math.sin(t), PIE_R * Math.cos(t));
                    a += v / sum;
                }
            }

            // ---- legend: color bar + "Name: count (percent)" ----
            double lx = PIE_R + 0.35;                // legend x (data units)
            double dy = 28 * s;                      // line spacing
            int lines = values.length + 1;           // states + total
            double ly = (lines - 1) * dy / 2;        // first line, vertically centered
            plot.setFont(java.awt.Font.PLAIN, 13);
            for (int st = 0; st < values.length; st++) {
                double y = ly - st * dy;
                plot.setColor(STATE_COLORS[st]);
                plot.setLineWidth(12);
                plot.drawLine(lx, y, lx + 18 * s, y);          // thick line = color patch
                plot.setColor(Color.black);
                String txt = STATE_NAMES[st] + ": " + String.format("%,d", (long) values[st])
                        + "  (" + IJ.d2s(100 * values[st] / sum, 2) + " %)";
                addLabelAt(plot, txt, lx + 28 * s, y - 5 * s, xMin, xMax, yMin, yMax);
            }
            plot.setColor(Color.darkGray);
            addLabelAt(plot, "Total: " + String.format("%,d", (long) sum),
                    lx + 28 * s, ly - values.length * dy - 5 * s, xMin, xMax, yMin, yMax);

            plot.setFont(java.awt.Font.BOLD, 14);
            plot.setColor(Color.black);
            plot.addLabel(0.02, 0.07, "Generation " + generation);
            plot.setLineWidth(1);
            return plot;
        });
        pieWindow = result;
        return result;
    }

    /**
     * When the user resizes the pie window, ImageJ redraws the old plot with the old
     * limits, which distorts the pie. This background thread checks the frame size
     * every 250 ms and rebuilds the pie with correct limits when it has changed.
     * It also works after the simulation has finished and ends when the pie window
     * is closed (after the simulation).
     */
    private void startPieResizeWatcher() {
        Thread t = new Thread(() -> {
            while (true) {
                try { Thread.sleep(250); } catch (InterruptedException e) { return; }
                PlotWindow w = pieWindow;
                Display display = Display.getDefault();
                if (w == null || display == null || display.isDisposed()) return;

                final boolean[] open    = {true};
                final boolean[] resized = {false};
                display.syncExec(() -> {
                    try {
                        if (!w.isVisible()) { open[0] = false; return; }
                        int[] fs = frameSize(w, pieFrameW, pieFrameH);
                        resized[0] = Math.abs(fs[0] - pieFrameW) > 2
                                  || Math.abs(fs[1] - pieFrameH) > 2;
                    } catch (Exception e) {
                        open[0] = false;                  // window disposed
                    }
                });
                if (!open[0]) {
                    if (simulationRunning) continue;      // main loop may reopen it
                    return;
                }
                if (resized[0] && pieValues != null)
                    showPiePlot(w, pieValues, pieGeneration);
            }
        }, "Pie resize watcher");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Fills the circle sector (center cx,cy, radius r, from a0 to a1 as fractions of a
     * full turn, clockwise from 12 o'clock) as a polygon that starts and ends at the
     * plot origin (0,0). This works for any sector that contains the origin
     * (pie slices, and the slightly offset shadow disc).
     */
    private static void fillStar(Plot plot, double cx, double cy, double r,
                                 double a0, double a1, Color c) {
        int steps = Math.max(2, (int) Math.ceil((a1 - a0) * 360));
        double[] px = new double[steps + 3];
        double[] py = new double[steps + 3];
        for (int k = 0; k <= steps; k++) {
            double t = 2 * Math.PI * (a0 + (a1 - a0) * k / steps);
            px[k + 1] = cx + r * Math.sin(t);
            py[k + 1] = cy + r * Math.cos(t);
        }
        // px[0], py[0], px[last], py[last] stay 0 -> polygon starts/ends at the origin
        plot.setLineWidth(1);
        plot.setColor(c, c);
        plot.addPoints(px, py, Plot.FILLED);
    }

    /** addLabel with data coordinates (converted to the normalized coordinates of addLabel). */
    private static void addLabelAt(Plot plot, String text, double x, double y,
                                   double xMin, double xMax, double yMin, double yMax) {
        plot.addLabel((x - xMin) / (xMax - xMin), (yMax - y) / (yMax - yMin), text);
    }

    // =================================================================
    //  Helpers
    // =================================================================

    /**
     * Builds a plot and shows it (new window) or puts it into the open window,
     * everything on the SWT UI thread. The builder gets the open window (or null)
     * so it can reuse the current frame size.
     */
    private PlotWindow showOnUiThread(PlotWindow win, Function<PlotWindow, Plot> builder) {
        final PlotWindow[] result = {win};
        Display.getDefault().syncExec(() -> {
            boolean open = win != null && win.isVisible();
            Plot plot = builder.apply(open ? win : null);
            if (open) win.drawPlot(plot);
            else result[0] = plot.show();
        });
        return result[0];
    }

    /** Current frame size of an open plot window, or the default if none. */
    private static int[] frameSize(PlotWindow win, int defW, int defH) {
        if (win != null && win.getPlot() != null) {
            var f = win.getPlot().getDrawingFrame();   // AWT or SWT Rectangle, both have width/height
            if (f != null && f.width > 0 && f.height > 0)
                return new int[] {f.width, f.height};
        }
        return new int[] {defW, defH};
    }

    /** "734,512 (2.94 %)" for counts, "2.94 %" in percent mode. */
    private String stateAmount(double value, double total) {
        double pct = total > 0 ? 100 * value / total : 0;
        if (PERCENT) return IJ.d2s(pct, 2) + " %";
        return String.format("%,d", (long) value) + " (" + IJ.d2s(pct, 2) + " %)";
    }

    private double scale(long value, long total) {
        return PERCENT ? 100.0 * value / total : value;
    }

    /** Places the plot window to the right of the image window (SWT version). */
    private void positionNextTo(PlotWindow pw, ImagePlus imp) {
        if (pw == null || imp.getWindow() == null) return;
        Display.getDefault().syncExec(() -> {
            org.eclipse.swt.graphics.Rectangle r = imp.getWindow().getBounds();
            pw.setLocation(r.x + r.width + 10, r.y);
        });
    }
}
