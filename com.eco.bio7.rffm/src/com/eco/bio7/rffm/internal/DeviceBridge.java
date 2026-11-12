/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rffm.internal;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.eco.bio7.rffm.graphics.GraphicsContext;
import com.eco.bio7.rffm.graphics.RGraphicsDevice;

/**
 * Connects Java graphics devices with the graphics engine of R: fills a DevDesc
 * (the layout of R 4.1 and later, checked against the headers of R 4.6) and
 * forwards the callbacks of R (upcalls) to the RGraphicsDevice.
 * <p>
 * The callbacks never call R (an R error would jump over the Java frames) and
 * never throw.
 */
public final class DeviceBridge {

	/* Offsets of DevDesc (R_ext/GraphicsDevice.h, 64 bit) */
	private static final long LEFT = 0, RIGHT = 8, BOTTOM = 16, TOP = 24, CLIP_LEFT = 32, CLIP_RIGHT = 40, CLIP_BOTTOM = 48, CLIP_TOP = 56,
			X_CHAR_OFFSET = 64, Y_CHAR_OFFSET = 72, Y_LINE_BIAS = 80, IPR = 88, CRA = 104, GAMMA = 120, CAN_CLIP = 128, CAN_CHANGE_GAMMA = 132,
			CAN_HADJ = 136, START_PS = 144, START_COL = 152, START_FILL = 156, START_LTY = 160, START_FONT = 164, START_GAMMA = 168,
			DISPLAY_LIST_ON = 184, LOCATOR = 256, ACTIVATE = 216, CIRCLE = 224, CLIP = 232, CLOSE = 240, DEACTIVATE = 248, LINE = 264, METRIC_INFO = 272,
			MODE = 280, NEW_PAGE = 288, POLYGON = 296, POLYLINE = 304, RECT = 312, PATH = 320, RASTER = 328, SIZE = 344, STR_WIDTH = 352,
			TEXT = 360, HAS_TEXT_UTF8 = 392, TEXT_UTF8 = 400, STR_WIDTH_UTF8 = 408, WANT_SYMBOL_UTF8 = 416, USE_ROTATED_TEXT_IN_CONTOUR = 420,
			EVENT_ENV = 424, HOLDFLUSH = 440, HAVE_TRANSPARENCY = 448, HAVE_TRANSPARENT_BG = 452, HAVE_RASTER = 456, HAVE_CAPTURE = 460,
			HAVE_LOCATOR = 464, DEVICE_VERSION = 520, DEVDESC_SIZE = 656, GC_SIZE = 288;

	private record Entry(RGraphicsDevice device, MemorySegment dd) {
	}

	private final RCore c;
	private final Charset nativeCharset;
	private final Map<Long, Entry> devices = new ConcurrentHashMap<>();
	private final Map<RGraphicsDevice, Entry> byDevice = new ConcurrentHashMap<>();
	private final MemorySegment locator, activate, circle, clip, close, deactivate, line, metricInfo, mode, newPage, polygon, polyline, rect, path, raster,
			size, strWidth, text, strWidthUTF8, textUTF8, holdflush;

	public DeviceBridge(RCore core) {
		this.c = core;
		this.nativeCharset = core.n.consoleCharset;
		MemoryLayout A = ADDRESS, I = JAVA_INT, D = JAVA_DOUBLE;
		locator = stub("locator", FunctionDescriptor.of(I, A, A, A));
		activate = stub("activate", FunctionDescriptor.ofVoid(A));
		deactivate = stub("deactivate", FunctionDescriptor.ofVoid(A));
		close = stub("close", FunctionDescriptor.ofVoid(A));
		circle = stub("circle", FunctionDescriptor.ofVoid(D, D, D, A, A));
		clip = stub("clip", FunctionDescriptor.ofVoid(D, D, D, D, A));
		line = stub("line", FunctionDescriptor.ofVoid(D, D, D, D, A, A));
		metricInfo = stub("metricInfo", FunctionDescriptor.ofVoid(I, A, A, A, A, A));
		mode = stub("mode", FunctionDescriptor.ofVoid(I, A));
		newPage = stub("newPage", FunctionDescriptor.ofVoid(A, A));
		polygon = stub("polygon", FunctionDescriptor.ofVoid(I, A, A, A, A));
		polyline = stub("polyline", FunctionDescriptor.ofVoid(I, A, A, A, A));
		rect = stub("rect", FunctionDescriptor.ofVoid(D, D, D, D, A, A));
		path = stub("path", FunctionDescriptor.ofVoid(A, A, I, A, I, A, A));
		raster = stub("raster", FunctionDescriptor.ofVoid(A, I, I, D, D, D, D, D, I, A, A));
		size = stub("size", FunctionDescriptor.ofVoid(A, A, A, A, A));
		strWidth = stub("strWidth", FunctionDescriptor.of(D, A, A, A));
		text = stub("text", FunctionDescriptor.ofVoid(D, D, A, D, D, A, A));
		strWidthUTF8 = stub("strWidthUTF8", FunctionDescriptor.of(D, A, A, A));
		textUTF8 = stub("textUTF8", FunctionDescriptor.ofVoid(D, D, A, D, D, A, A));
		holdflush = stub("holdflush", FunctionDescriptor.of(I, A, I));
	}

	private MemorySegment stub(String name, FunctionDescriptor fd) {
		try {
			MethodType type = fd.toMethodType();
			MethodHandle h = MethodHandles.lookup().findVirtual(DeviceBridge.class, "cb_" + name, type).bindTo(this);
			return RNative.LINKER.upcallStub(h, fd, Arena.global());
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Opens a device in R (in the thread of R) and makes it the current device.
	 *
	 * @return the number of the device in R (as dev.cur())
	 */
	public int open(String name, RGraphicsDevice device, double dpi, double pointsize) {
		RNative n = c.n;
		try {
			if ((int) n.R_CheckDeviceAvailableBool.invokeExact() == 0) {
				throw new IllegalStateException("Too many open graphics devices in R");
			}
			MemorySegment dd = n.GEcreateDD != null ? ((MemorySegment) n.GEcreateDD.invokeExact()).reinterpret(DEVDESC_SIZE) : n.calloc(DEVDESC_SIZE + 64);
			double w = device.width(), h = device.height();
			dd.set(JAVA_DOUBLE, LEFT, 0);
			dd.set(JAVA_DOUBLE, RIGHT, w);
			dd.set(JAVA_DOUBLE, BOTTOM, h);
			dd.set(JAVA_DOUBLE, TOP, 0);
			dd.set(JAVA_DOUBLE, CLIP_LEFT, 0);
			dd.set(JAVA_DOUBLE, CLIP_RIGHT, w);
			dd.set(JAVA_DOUBLE, CLIP_BOTTOM, h);
			dd.set(JAVA_DOUBLE, CLIP_TOP, 0);
			dd.set(JAVA_DOUBLE, X_CHAR_OFFSET, 0.4900);
			dd.set(JAVA_DOUBLE, Y_CHAR_OFFSET, 0.3333);
			dd.set(JAVA_DOUBLE, Y_LINE_BIAS, 0.2);
			dd.set(JAVA_DOUBLE, IPR, 1.0 / dpi);
			dd.set(JAVA_DOUBLE, IPR + 8, 1.0 / dpi);
			dd.set(JAVA_DOUBLE, CRA, 0.9 * pointsize * dpi / 72.0);
			dd.set(JAVA_DOUBLE, CRA + 8, 1.2 * pointsize * dpi / 72.0);
			dd.set(JAVA_DOUBLE, GAMMA, 1.0);
			dd.set(JAVA_INT, CAN_CLIP, 1);
			dd.set(JAVA_INT, CAN_CHANGE_GAMMA, 0);
			dd.set(JAVA_INT, CAN_HADJ, 2);
			dd.set(JAVA_DOUBLE, START_PS, pointsize);
			dd.set(JAVA_INT, START_COL, 0xFF000000);
			dd.set(JAVA_INT, START_FILL, 0xFFFFFFFF);
			dd.set(JAVA_INT, START_LTY, 0);
			dd.set(JAVA_INT, START_FONT, 1);
			dd.set(JAVA_DOUBLE, START_GAMMA, 1.0);
			dd.set(JAVA_INT, DISPLAY_LIST_ON, 1);
			dd.set(ADDRESS, ACTIVATE, activate);
			dd.set(ADDRESS, CIRCLE, circle);
			dd.set(ADDRESS, CLIP, clip);
			dd.set(ADDRESS, CLOSE, close);
			dd.set(ADDRESS, DEACTIVATE, deactivate);
			dd.set(ADDRESS, LINE, line);
			dd.set(ADDRESS, METRIC_INFO, metricInfo);
			dd.set(ADDRESS, MODE, mode);
			dd.set(ADDRESS, NEW_PAGE, newPage);
			dd.set(ADDRESS, POLYGON, polygon);
			dd.set(ADDRESS, POLYLINE, polyline);
			dd.set(ADDRESS, RECT, rect);
			dd.set(ADDRESS, PATH, path);
			dd.set(ADDRESS, RASTER, raster);
			dd.set(ADDRESS, SIZE, size);
			dd.set(ADDRESS, STR_WIDTH, strWidth);
			dd.set(ADDRESS, TEXT, text);
			dd.set(JAVA_INT, HAS_TEXT_UTF8, 1);
			dd.set(ADDRESS, TEXT_UTF8, textUTF8);
			dd.set(ADDRESS, STR_WIDTH_UTF8, strWidthUTF8);
			dd.set(JAVA_INT, WANT_SYMBOL_UTF8, 1);
			dd.set(JAVA_INT, USE_ROTATED_TEXT_IN_CONTOUR, 1);
			dd.set(ADDRESS, EVENT_ENV, c.nil);
			dd.set(ADDRESS, HOLDFLUSH, holdflush);
			/* 1 = no, 2 = yes */
			dd.set(JAVA_INT, HAVE_TRANSPARENCY, 2);
			dd.set(JAVA_INT, HAVE_TRANSPARENT_BG, 1);
			dd.set(JAVA_INT, HAVE_RASTER, 2);
			dd.set(JAVA_INT, HAVE_CAPTURE, 1);
			dd.set(JAVA_INT, HAVE_LOCATOR, device.supportsLocator() ? 2 : 1);
			if (device.supportsLocator()) {
				dd.set(ADDRESS, LOCATOR, locator);
			}
			/* Older than R_GE_definitions: no patterns, masks, groups (the engine doesn't call them) */
			dd.set(JAVA_INT, DEVICE_VERSION, 0);
			Entry e = new Entry(device, dd);
			devices.put(dd.address(), e);
			byDevice.put(device, e);
			MemorySegment gdd = (MemorySegment) n.GEcreateDevDesc.invokeExact(dd);
			try (Arena a = Arena.ofConfined()) {
				n.GEaddDevice2.invokeExact(gdd, a.allocateFrom(name));
			}
			n.GEinitDisplayList.invokeExact(gdd);
			return (int) n.GEdeviceNumber.invokeExact(gdd) + 1;
		} catch (RuntimeException ex) {
			throw ex;
		} catch (Throwable t) {
			throw new IllegalStateException(t);
		}
	}

	/** Sets the new size of a device in R (in the thread of R; the plot is replayed by the caller). */
	public boolean resize(RGraphicsDevice device) {
		Entry e = byDevice.get(device);
		if (e == null) {
			return false;
		}
		double w = device.width(), h = device.height();
		e.dd.set(JAVA_DOUBLE, RIGHT, w);
		e.dd.set(JAVA_DOUBLE, BOTTOM, h);
		e.dd.set(JAVA_DOUBLE, CLIP_RIGHT, w);
		e.dd.set(JAVA_DOUBLE, CLIP_BOTTOM, h);
		return true;
	}

	/** The number of a device in R (as dev.cur()) or -1 if it is not open. */
	public int number(RGraphicsDevice device) {
		Entry e = byDevice.get(device);
		if (e == null) {
			return -1;
		}
		try {
			MemorySegment gdd = (MemorySegment) c.n.Rf_desc2GEDesc.invokeExact(e.dd);
			return (int) c.n.GEdeviceNumber.invokeExact(gdd) + 1;
		} catch (Throwable t) {
			return -1;
		}
	}

	/** Returns true if the device is open in R. */
	public boolean isOpen(RGraphicsDevice device) {
		return byDevice.containsKey(device);
	}

	/* ---------------- Callbacks of R ---------------- */

	private RGraphicsDevice dev(MemorySegment dd) {
		Entry e = devices.get(dd.address());
		return e == null ? null : e.device;
	}

	private GraphicsContext gc(MemorySegment p) {
		MemorySegment g = p.reinterpret(GC_SIZE);
		byte[] fam = new byte[201];
		MemorySegment.copy(g, JAVA_BYTE, 76, fam, 0, 201);
		int len = 0;
		while (len < fam.length && fam[len] != 0) {
			len++;
		}
		return new GraphicsContext(g.get(JAVA_INT, 0), g.get(JAVA_INT, 4), g.get(JAVA_DOUBLE, 8), g.get(JAVA_DOUBLE, 16), g.get(JAVA_INT, 24),
				g.get(JAVA_INT, 28), g.get(JAVA_INT, 32), g.get(JAVA_DOUBLE, 40), g.get(JAVA_DOUBLE, 48), g.get(JAVA_DOUBLE, 56), g.get(JAVA_DOUBLE, 64),
				g.get(JAVA_INT, 72), new String(fam, 0, len, StandardCharsets.UTF_8));
	}

	private static double[] doubles(MemorySegment p, int n) {
		return n <= 0 ? new double[0] : p.reinterpret(8L * n).toArray(JAVA_DOUBLE);
	}

	private static String string(MemorySegment p, Charset cs) {
		/* Any charset (getString supports only some standard charsets, e.g., not windows-1252) */
		return RNative.cString(p, cs);
	}

	private static void report(Throwable t) {
		/* A callback must not throw: the error is printed only */
		System.err.println("R graphics device: " + t);
	}

	/* locator(): Rboolean (*locator)(double *x, double *y, pDevDesc dd), TRUE with a position */
	int cb_locator(MemorySegment x, MemorySegment y, MemorySegment dd) {
		try {
			RGraphicsDevice d = dev(dd);
			double[] p = d == null ? null : d.locator();
			if (p == null) {
				return 0;
			}
			x.reinterpret(8).set(JAVA_DOUBLE, 0, p[0]);
			y.reinterpret(8).set(JAVA_DOUBLE, 0, p[1]);
			return 1;
		} catch (Throwable t) {
			report(t);
			return 0;
		}
	}

	void cb_activate(MemorySegment dd) {
		try {
			RGraphicsDevice d = dev(dd);
			if (d != null) {
				d.activate();
			}
		} catch (Throwable t) {
			report(t);
		}
	}

	void cb_deactivate(MemorySegment dd) {
		try {
			RGraphicsDevice d = dev(dd);
			if (d != null) {
				d.deactivate();
			}
		} catch (Throwable t) {
			report(t);
		}
	}

	void cb_close(MemorySegment dd) {
		try {
			Entry e = devices.remove(dd.address());
			if (e != null) {
				byDevice.remove(e.device);
				e.device.close();
			}
		} catch (Throwable t) {
			report(t);
		}
	}

	void cb_circle(double x, double y, double r, MemorySegment gc, MemorySegment dd) {
		try {
			RGraphicsDevice d = dev(dd);
			if (d != null) {
				d.circle(x, y, r, gc(gc));
			}
		} catch (Throwable t) {
			report(t);
		}
	}

	void cb_clip(double x0, double x1, double y0, double y1, MemorySegment dd) {
		try {
			RGraphicsDevice d = dev(dd);
			if (d != null) {
				d.clip(x0, x1, y0, y1);
			}
		} catch (Throwable t) {
			report(t);
		}
	}

	void cb_line(double x1, double y1, double x2, double y2, MemorySegment gc, MemorySegment dd) {
		try {
			RGraphicsDevice d = dev(dd);
			if (d != null) {
				d.line(x1, y1, x2, y2, gc(gc));
			}
		} catch (Throwable t) {
			report(t);
		}
	}

	void cb_metricInfo(int ch, MemorySegment gc, MemorySegment ascent, MemorySegment descent, MemorySegment width, MemorySegment dd) {
		double[] m = { 0, 0, 0 };
		try {
			RGraphicsDevice d = dev(dd);
			if (d != null) {
				m = d.metricInfo(Math.abs(ch), gc(gc));
			}
		} catch (Throwable t) {
			report(t);
		}
		ascent.reinterpret(8).set(JAVA_DOUBLE, 0, m[0]);
		descent.reinterpret(8).set(JAVA_DOUBLE, 0, m[1]);
		width.reinterpret(8).set(JAVA_DOUBLE, 0, m[2]);
	}

	void cb_mode(int m, MemorySegment dd) {
		try {
			RGraphicsDevice d = dev(dd);
			if (d != null) {
				d.mode(m);
			}
		} catch (Throwable t) {
			report(t);
		}
	}

	void cb_newPage(MemorySegment gc, MemorySegment dd) {
		try {
			RGraphicsDevice d = dev(dd);
			if (d != null) {
				d.newPage(gc(gc));
			}
		} catch (Throwable t) {
			report(t);
		}
	}

	void cb_polygon(int n, MemorySegment x, MemorySegment y, MemorySegment gc, MemorySegment dd) {
		try {
			RGraphicsDevice d = dev(dd);
			if (d != null) {
				d.polygon(doubles(x, n), doubles(y, n), gc(gc));
			}
		} catch (Throwable t) {
			report(t);
		}
	}

	void cb_polyline(int n, MemorySegment x, MemorySegment y, MemorySegment gc, MemorySegment dd) {
		try {
			RGraphicsDevice d = dev(dd);
			if (d != null) {
				d.polyline(doubles(x, n), doubles(y, n), gc(gc));
			}
		} catch (Throwable t) {
			report(t);
		}
	}

	void cb_rect(double x0, double y0, double x1, double y1, MemorySegment gc, MemorySegment dd) {
		try {
			RGraphicsDevice d = dev(dd);
			if (d != null) {
				d.rect(x0, y0, x1, y1, gc(gc));
			}
		} catch (Throwable t) {
			report(t);
		}
	}

	void cb_path(MemorySegment x, MemorySegment y, int npoly, MemorySegment nper, int winding, MemorySegment gc, MemorySegment dd) {
		try {
			RGraphicsDevice d = dev(dd);
			if (d != null && npoly > 0) {
				int[] per = nper.reinterpret(4L * npoly).toArray(JAVA_INT);
				int total = 0;
				for (int p : per) {
					total += p;
				}
				d.path(doubles(x, total), doubles(y, total), per, winding != 0, gc(gc));
			}
		} catch (Throwable t) {
			report(t);
		}
	}

	void cb_raster(MemorySegment raster, int w, int h, double x, double y, double width, double height, double rot, int interpolate, MemorySegment gc,
			MemorySegment dd) {
		try {
			RGraphicsDevice d = dev(dd);
			if (d != null && w > 0 && h > 0) {
				d.raster(raster.reinterpret(4L * w * h).toArray(JAVA_INT), w, h, x, y, width, height, rot, interpolate != 0, gc(gc));
			}
		} catch (Throwable t) {
			report(t);
		}
	}

	void cb_size(MemorySegment left, MemorySegment right, MemorySegment bottom, MemorySegment top, MemorySegment dd) {
		double w = 0, h = 0;
		try {
			RGraphicsDevice d = dev(dd);
			if (d != null) {
				w = d.width();
				h = d.height();
			} else {
				MemorySegment s = dd.reinterpret(DEVDESC_SIZE);
				w = s.get(JAVA_DOUBLE, RIGHT);
				h = s.get(JAVA_DOUBLE, BOTTOM);
			}
		} catch (Throwable t) {
			report(t);
		}
		left.reinterpret(8).set(JAVA_DOUBLE, 0, 0);
		right.reinterpret(8).set(JAVA_DOUBLE, 0, w);
		bottom.reinterpret(8).set(JAVA_DOUBLE, 0, h);
		top.reinterpret(8).set(JAVA_DOUBLE, 0, 0);
	}

	double cb_strWidth(MemorySegment str, MemorySegment gc, MemorySegment dd) {
		return width(string(str, nativeCharset), gc, dd);
	}

	double cb_strWidthUTF8(MemorySegment str, MemorySegment gc, MemorySegment dd) {
		return width(string(str, StandardCharsets.UTF_8), gc, dd);
	}

	private double width(String s, MemorySegment gc, MemorySegment dd) {
		try {
			RGraphicsDevice d = dev(dd);
			return d == null ? 0 : d.strWidth(s, gc(gc));
		} catch (Throwable t) {
			report(t);
			return 0;
		}
	}

	void cb_text(double x, double y, MemorySegment str, double rot, double hadj, MemorySegment gc, MemorySegment dd) {
		drawText(x, y, string(str, nativeCharset), rot, hadj, gc, dd);
	}

	void cb_textUTF8(double x, double y, MemorySegment str, double rot, double hadj, MemorySegment gc, MemorySegment dd) {
		drawText(x, y, string(str, StandardCharsets.UTF_8), rot, hadj, gc, dd);
	}

	private void drawText(double x, double y, String s, double rot, double hadj, MemorySegment gc, MemorySegment dd) {
		try {
			RGraphicsDevice d = dev(dd);
			if (d != null) {
				d.text(x, y, s, rot, hadj, gc(gc));
			}
		} catch (Throwable t) {
			report(t);
		}
	}

	int cb_holdflush(MemorySegment dd, int level) {
		try {
			RGraphicsDevice d = dev(dd);
			return d == null ? 0 : d.holdflush(level);
		} catch (Throwable t) {
			report(t);
			return 0;
		}
	}
}
