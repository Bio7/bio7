import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.PlugIn;

import java.awt.Color;
import java.awt.Font;
import java.util.stream.IntStream;

import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Display;

/**
 * Fast Parallel Game of Life for (SWT)ImageJ with the monitoring charts drawn
 * as a non-destructive Overlay directly on top of the simulation image:
 *  - top left:           line graph of living and dead cells
 *  - below the graph:    pie chart with drop shadow and legend (names + amounts)
 *  - below the pie:      clickable Play/Pause button
 *
 * Overlays live in image coordinates. The image is usually shown zoomed out,
 * so every update converts a fixed layout in SCREEN pixels into image
 * coordinates using the current magnification and visible area. The panels
 * therefore keep their on-screen size and stay at the top left when you zoom or pan.
 */
public class ImageJ_GameOfLife_Overlay_ implements PlugIn {

    int WIDTH = 5000, HEIGHT = 5000, n = 1000;

    // ---- Overlay settings ----
    int     OVERLAY_EVERY   = 1;       // redraw the overlay every N generations
    boolean SHOW_DEAD_CURVE = true;    // also draw the dead curve
    boolean DUAL_AXIS       = true;    // alive on left axis, dead on right axis, each
                                       // auto-scaled (false = both on one 0..100 % axis)

    static final String[] STATE_NAMES  = {"Alive", "Dead"};
    static final Color[]  STATE_COLORS = {new Color(0x2ca02c), new Color(0x7f7f7f)};
    static final Color    PANEL_BG     = new Color(255, 255, 255, 215);   // semi-transparent
    static final Color    PANEL_BORDER = new Color(70, 70, 70);
    static final Color    GRID         = new Color(210, 210, 210);

    // layout in screen pixels
    static final int MARGIN = 12;
    static final int GAP    = 10;                 // space between the two panels
    static final int LINE_W = 400, LINE_H = 220;
    static final int PIE_W  = 400, PIE_H  = 150, PIE_R = 55;
    static final int BTN_W  = 110, BTN_H  = 34;
    static final int BTN_X  = MARGIN, BTN_Y = MARGIN + LINE_H + GAP + PIE_H + GAP;

    // Play/Pause state (changed by the mouse listener on the UI thread)
    private volatile boolean paused   = false;
    private volatile boolean finished = false;

    // last data drawn, so the overlay can be redrawn right after a click
    private volatile double[] lastGens, lastAlive;
    private volatile int      lastCount, lastGeneration;
    private volatile long     lastTotal;

    public void run(String arg) {
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

        ImagePlus imp = new ImagePlus("Turbo Game of Life (Overlay)", ip);
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
        double[] gens     = new double[n + 1];
        double[] aliveCnt = new double[n + 1];
        gens[0] = 0;
        aliveCnt[0] = aliveInit;
        int count = 1;

        final int[] rowAlive = new int[HEIGHT];

        updateOverlay(imp, gens, aliveCnt, count, total, 0);

        // ---------- Play/Pause button: mouse listener on the SWT canvas ----------
        MouseAdapter clickListener = new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent e) {
                if (finished || e.button != 1) return;
                // e.x / e.y are canvas (screen) pixels, the button layout is in screen pixels too
                if (e.x >= BTN_X && e.x <= BTN_X + BTN_W && e.y >= BTN_Y && e.y <= BTN_Y + BTN_H) {
                    paused = !paused;
                    redrawOverlay(imp);   // show the new button state immediately
                }
            }
        };
        Display.getDefault().syncExec(() -> {
            ImageCanvas ic = imp.getCanvas();
            if (ic != null) ic.addMouseListener(clickListener);
        });

        // ---------- Main loop ----------
        try {
            for (int it = 0; it < n; it++) {
                // ---- paused: wait, but keep the overlay following zoom/pan ----
                while (paused && !IJ.escapePressed()) {
                    IJ.showStatus("Paused at generation " + it + " - click Play to continue");
                    updateOverlay(imp, gens, aliveCnt, count, total, it);
                    IJ.wait(200);
                }
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
                    rowAlive[y] = rowCount;
                });

                long aliveNow = 0;
                for (int c : rowAlive) aliveNow += c;
                gens[count]     = it + 1;
                aliveCnt[count] = aliveNow;
                count++;

                ip.setPixels(nextGen);
                if ((it + 1) % OVERLAY_EVERY == 0 || it == n - 1)
                    updateOverlay(imp, gens, aliveCnt, count, total, it + 1);
                imp.updateAndDraw();

                IJ.showStatus("Iteration: " + (it + 1) + "  alive: " + aliveNow
                        + "  (Press Esc to stop)");
            }
        } finally {
            // ---- finished: grey "Done" button, remove the listener ----
            finished = true;
            paused = false;
            updateOverlay(imp, gens, aliveCnt, count, total, count - 1);
            Display.getDefault().syncExec(() -> {
                ImageCanvas ic = imp.getCanvas();
                if (ic != null) ic.removeMouseListener(clickListener);
            });
        }
    }

    /** Redraws the overlay with the last data (called from the click handler). */
    private void redrawOverlay(ImagePlus imp) {
        if (lastGens != null)
            updateOverlay(imp, lastGens, lastAlive, lastCount, lastTotal, lastGeneration);
    }

    // =================================================================
    //  Overlay construction (on the SWT UI thread)
    // =================================================================
    private void updateOverlay(ImagePlus imp, double[] gens, double[] aliveCnt,
                               int count, long total, int generation) {
        lastGens = gens; lastAlive = aliveCnt; lastCount = count;
        lastTotal = total; lastGeneration = generation;
        // syncExec runs the code directly if we are already on the UI thread (click handler)
        Display.getDefault().syncExec(() -> {
            View v = new View(imp);
            Overlay ov = new Overlay();
            addLinePanel(ov, v, gens, aliveCnt, count, total);
            addPiePanel(ov, v, aliveCnt[count - 1], total, generation);
            addButton(ov, v);
            imp.setOverlay(ov);
        });
    }

    /** Line graph panel, top left. */
    private void addLinePanel(Overlay ov, View v, double[] gens, double[] aliveCnt,
                              int count, long total) {
        double px = MARGIN, py = MARGIN;
        ov.add(rect(v, px, py, LINE_W, LINE_H, PANEL_BG, null, 0));
        ov.add(rect(v, px, py, LINE_W, LINE_H, null, PANEL_BORDER, 1));

        boolean dual = SHOW_DEAD_CURVE && DUAL_AXIS;
        double ax0 = px + 52, ax1 = px + LINE_W - (dual ? 52 : 16);
        double ay1 = py + 52, ay0 = py + LINE_H - 30;   // ay1 = top, ay0 = bottom

        // legend lines with current amounts
        double aNow = aliveCnt[count - 1], dNow = total - aNow;
        double[] now = {aNow, dNow};
        int shown = SHOW_DEAD_CURVE ? 2 : 1;
        for (int s = 0; s < shown; s++) {
            double ly = py + 8 + s * 20;
            ov.add(line(v, px + 10, ly + 8, px + 28, ly + 8, STATE_COLORS[s], 4));
            ov.add(text(v, STATE_NAMES[s] + ": " + amount(now[s], total),
                    px + 36, ly, 12, Font.PLAIN, Color.black));
        }

        // percent series and ranges
        double[] alivePct = new double[count], deadPct = new double[count];
        for (int i = 0; i < count; i++) {
            alivePct[i] = 100.0 * aliveCnt[i] / total;
            deadPct[i]  = 100.0 - alivePct[i];
        }
        double[] aRange, dRange;
        if (!SHOW_DEAD_CURVE)      { aRange = niceRange(alivePct); dRange = aRange; }
        else if (dual)             { aRange = niceRange(alivePct); dRange = niceRange(deadPct); }
        else                       { aRange = new double[] {0, 100}; dRange = aRange; }
        double xMax = Math.max(10, gens[count - 1]);

        // grid + y labels (min, middle, max); right axis labels for dead if dual
        for (int k = 0; k <= 2; k++) {
            double gy = ay0 - (ay0 - ay1) * k / 2;
            ov.add(line(v, ax0, gy, ax1, gy, GRID, 1));
            double aVal = aRange[0] + (aRange[1] - aRange[0]) * k / 2;
            ov.add(text(v, fmtPct(aVal, aRange), px + 6, gy - 8, 11, Font.PLAIN,
                    dual ? STATE_COLORS[0].darker() : Color.darkGray));
            if (dual) {
                double dVal = dRange[0] + (dRange[1] - dRange[0]) * k / 2;
                ov.add(text(v, fmtPct(dVal, dRange), ax1 + 6, gy - 8, 11, Font.PLAIN,
                        STATE_COLORS[1].darker()));
            }
        }
        // axes
        ov.add(line(v, ax0, ay0, ax1, ay0, Color.black, 1));
        ov.add(line(v, ax0, ay0, ax0, ay1, Color.black, 1));
        if (dual) ov.add(line(v, ax1, ay0, ax1, ay1, Color.black, 1));
        // x labels
        ov.add(text(v, "0", ax0 - 3, ay0 + 4, 11, Font.PLAIN, Color.darkGray));
        ov.add(text(v, "Generation", (ax0 + ax1) / 2 - 30, ay0 + 6, 11, Font.PLAIN, Color.darkGray));
        ov.add(text(v, IJ.d2s(xMax, 0), ax1 - 28, ay0 + 4, 11, Font.PLAIN, Color.darkGray));

        // curves (dead first, so alive is drawn on top)
        if (SHOW_DEAD_CURVE)
            ov.add(curve(v, gens, deadPct, count, xMax, dRange, ax0, ax1, ay0, ay1, STATE_COLORS[1]));
        ov.add(curve(v, gens, alivePct, count, xMax, aRange, ax0, ax1, ay0, ay1, STATE_COLORS[0]));
    }

    private PolygonRoi curve(View v, double[] gens, double[] pct, int count, double xMax,
                             double[] range, double ax0, double ax1, double ay0, double ay1, Color c) {
        int m = Math.max(2, count);
        float[] xs = new float[m], ys = new float[m];
        double span = range[1] - range[0];
        for (int i = 0; i < m; i++) {
            int j = Math.min(i, count - 1);            // duplicate the point if only one exists
            double val = Math.max(range[0], Math.min(range[1], pct[j]));
            double sx = ax0 + gens[j] / xMax * (ax1 - ax0);
            double sy = ay0 - (val - range[0]) / span * (ay0 - ay1);
            xs[i] = (float) v.x(sx);
            ys[i] = (float) v.y(sy);
        }
        PolygonRoi r = new PolygonRoi(xs, ys, m, Roi.POLYLINE);
        r.setStrokeColor(c);
        r.setStrokeWidth((float) v.len(2));
        return r;
    }

    /** Tidy axis range around the data, within 0..100 %. */
    private static double[] niceRange(double[] data) {
        double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
        for (double d : data) { min = Math.min(min, d); max = Math.max(max, d); }
        double span = max - min;
        if (span < 1e-6) span = Math.max(1e-3, Math.abs(max) * 0.1);
        double step = niceCeil(span / 2);
        double lo = Math.max(0,   Math.floor(min / step) * step);
        double hi = Math.min(100, Math.ceil(max / step) * step);
        if (hi <= lo) hi = lo + step;
        return new double[] {lo, hi};
    }

    /** Percent label with enough decimals for the axis range. */
    private static String fmtPct(double v, double[] range) {
        double span = range[1] - range[0];
        int dec = span >= 20 ? 0 : span >= 2 ? 1 : 2;
        return IJ.d2s(v, dec) + "%";
    }

    /** Pie chart panel with drop shadow and legend, below the line graph. */
    private void addPiePanel(Overlay ov, View v, double aliveNow, long total, int generation) {
        double px = MARGIN, py = MARGIN + LINE_H + GAP;
        ov.add(rect(v, px, py, PIE_W, PIE_H, PANEL_BG, null, 0));
        ov.add(rect(v, px, py, PIE_W, PIE_H, null, PANEL_BORDER, 1));

        double cx = px + 16 + PIE_R, cy = py + PIE_H / 2.0;
        double[] values = {aliveNow, total - aliveNow};

        // soft drop shadow: semi-transparent discs, offset down-right
        int[] alpha = {25, 35, 50};
        for (int i = 0; i < alpha.length; i++) {
            double r = PIE_R + (alpha.length - 1 - i) * 1.5;
            ov.add(sector(v, cx + 5, cy + 5, r, 0, 1, new Color(0, 0, 0, alpha[i])));
        }

        // slices (clockwise from 12 o'clock)
        double start = 0;
        int nonZero = 0;
        for (int s = 0; s < values.length; s++) {
            double frac = values[s] / total;
            if (frac <= 0) continue;
            nonZero++;
            ov.add(sector(v, cx, cy, PIE_R, start, start + frac, STATE_COLORS[s]));
            start += frac;
        }
        // white separators
        if (nonZero > 1) {
            double a = 0;
            for (double val : values) {
                if (val <= 0) continue;
                double t = 2 * Math.PI * a;
                ov.add(line(v, cx, cy, cx + PIE_R * Math.sin(t), cy - PIE_R * Math.cos(t), Color.white, 2));
                a += val / total;
            }
        }

        // legend
        double lx = cx + PIE_R + 22;
        ov.add(text(v, "Generation " + generation, lx, py + 10, 13, Font.BOLD, Color.black));
        for (int s = 0; s < values.length; s++) {
            double ly = py + 42 + s * 40;
            ov.add(rect(v, lx, ly + 2, 14, 14, STATE_COLORS[s], null, 0));
            ov.add(text(v, STATE_NAMES[s], lx + 22, ly, 12, Font.BOLD, Color.black));
            ov.add(text(v, amount(values[s], total), lx + 22, ly + 17, 12, Font.PLAIN, Color.darkGray));
        }
    }

    /** Play/Pause button below the pie chart (grey "Done" when finished). */
    private void addButton(Overlay ov, View v) {
        Color bg = finished ? new Color(170, 170, 170, 220)
                 : paused   ? new Color(46, 160, 67, 235)     // green: Play
                            : new Color(205, 75, 60, 235);    // red:   Pause
        int corner = (int) Math.round(v.len(12));

        Roi body = rect(v, BTN_X, BTN_Y, BTN_W, BTN_H, bg, null, 0);
        body.setCornerDiameter(corner);
        ov.add(body);
        Roi border = rect(v, BTN_X, BTN_Y, BTN_W, BTN_H, null, PANEL_BORDER, 1);
        border.setCornerDiameter(corner);
        ov.add(border);

        double ix = BTN_X + 14, iy = BTN_Y + 9;       // icon area 16 x 16
        if (paused || finished) {                      // play triangle
            float[] xs = {(float) v.x(ix), (float) v.x(ix), (float) v.x(ix + 15)};
            float[] ys = {(float) v.y(iy), (float) v.y(iy + 16), (float) v.y(iy + 8)};
            PolygonRoi tri = new PolygonRoi(xs, ys, 3, Roi.POLYGON);
            tri.setFillColor(Color.white);
            ov.add(tri);
        } else {                                       // pause bars
            ov.add(rect(v, ix, iy, 5, 16, Color.white, null, 0));
            ov.add(rect(v, ix + 9, iy, 5, 16, Color.white, null, 0));
        }
        String label = finished ? "Done" : paused ? "Play" : "Pause";
        ov.add(text(v, label, BTN_X + 40, BTN_Y + 8, 14, Font.BOLD, Color.white));
    }

    // =================================================================
    //  Screen-to-image conversion and ROI helpers
    // =================================================================

    /** Converts screen pixels (relative to the visible canvas area) to image coordinates. */
    static class View {
        final double ox, oy, mag, w, h;   // w, h = visible size in screen pixels

        View(ImagePlus imp) {
            ImageCanvas ic = imp.getCanvas();
            if (ic != null && ic.getMagnification() > 0) {
                mag = ic.getMagnification();
                var src = ic.getSrcRect();   // AWT or SWT Rectangle, both have x/y/width/height
                ox = src.x;
                oy = src.y;
                w  = src.width * mag;
                h  = src.height * mag;
            } else {
                mag = 1; ox = 0; oy = 0;
                w = imp.getWidth(); h = imp.getHeight();
            }
        }
        double x(double sx)   { return ox + sx / mag; }
        double y(double sy)   { return oy + sy / mag; }
        double len(double px) { return px / mag; }
    }

    private static Roi rect(View v, double sx, double sy, double sw, double sh,
                            Color fill, Color stroke, double strokePx) {
        Roi r = new Roi(v.x(sx), v.y(sy), v.len(sw), v.len(sh));
        if (fill != null) r.setFillColor(fill);
        if (stroke != null) {
            r.setStrokeColor(stroke);
            r.setStrokeWidth((float) v.len(strokePx));
        }
        return r;
    }

    private static Line line(View v, double x1, double y1, double x2, double y2,
                             Color c, double widthPx) {
        Line l = new Line(v.x(x1), v.y(y1), v.x(x2), v.y(y2));
        l.setStrokeColor(c);
        l.setStrokeWidth((float) v.len(widthPx));
        return l;
    }

    /** Text with its top-left corner at (sx, sy); font size in screen pixels. */
    private static TextRoi text(View v, String s, double sx, double sy,
                                double sizePx, int style, Color c) {
        Font f = new Font("SansSerif", style, 12).deriveFont((float) v.len(sizePx));
        TextRoi t = new TextRoi(v.x(sx), v.y(sy), s, f);
        t.setStrokeColor(c);
        return t;
    }

    /** Filled circle sector, a0..a1 as fractions of a turn, clockwise from 12 o'clock. */
    private static PolygonRoi sector(View v, double cx, double cy, double r,
                                     double a0, double a1, Color fill) {
        boolean full = a1 - a0 >= 0.9999;
        int steps = Math.max(2, (int) Math.ceil((a1 - a0) * 360));
        int m = full ? steps : steps + 2;
        float[] xs = new float[m], ys = new float[m];
        int k0 = 0;
        if (!full) {                              // slice: start at the center
            xs[0] = (float) v.x(cx);
            ys[0] = (float) v.y(cy);
            k0 = 1;
        }
        int pts = full ? steps : steps + 1;
        for (int k = 0; k < pts; k++) {
            double t = 2 * Math.PI * (a0 + (a1 - a0) * k / steps);
            xs[k0 + k] = (float) v.x(cx + r * Math.sin(t));
            ys[k0 + k] = (float) v.y(cy - r * Math.cos(t));
        }
        PolygonRoi p = new PolygonRoi(xs, ys, m, Roi.POLYGON);
        p.setFillColor(fill);
        return p;
    }

    /** "734,512 (2.94 %)" */
    private static String amount(double value, double total) {
        return String.format("%,d", (long) value) + " (" + IJ.d2s(100 * value / total, 2) + " %)";
    }

    /** Rounds up to 1, 2, 2.5, 5 x 10^k for a tidy axis maximum. */
    private static double niceCeil(double v) {
        if (v <= 0) return 1;
        double mag = Math.pow(10, Math.floor(Math.log10(v)));
        double r = v / mag;
        double nice = r <= 1 ? 1 : r <= 2 ? 2 : r <= 2.5 ? 2.5 : r <= 5 ? 5 : 10;
        return nice * mag;
    }
}
