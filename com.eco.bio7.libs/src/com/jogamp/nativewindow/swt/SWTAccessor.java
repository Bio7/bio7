/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.nativewindow.swt;

import com.jogamp.common.os.Platform;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GCData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Scrollable;

import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.VisualIDHolder;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.nativewindow.macosx.MacOSXGraphicsDevice;
import com.jogamp.nativewindow.windows.WindowsGraphicsDevice;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;

import jogamp.nativewindow.macosx.OSXUtil;
import jogamp.nativewindow.x11.X11Lib;
import jogamp.nativewindow.Debug;

public class SWTAccessor {
	private static final boolean DEBUG = Debug.debug("SWT");

	private static final Method swt_scrollable_clientAreaInPixels;
	private static final Method swt_control_locationInPixels;
	private static final Method swt_control_sizeInPixels;
	private static final Method swt_dpiutil_getScalingFactor;

	private static final Field swt_control_handle;
	private static final boolean swt_uses_long_handles;

	private static Object swt_osx_init = new Object();
	private static Field swt_osx_control_view = null;
	private static Field swt_osx_view_id = null;

	private static final String nwt;
	public static final boolean isOSX;
	public static final boolean isWindows;
	public static final boolean isX11;
	public static final boolean isX11GTK;

	// X11/GTK, Windows/GDI, ..
	private static final String str_handle = "handle";

	// OSX/Cocoa
	private static final String str_osx_view = "view"; // OSX
	private static final String str_osx_id = "id"; // OSX
	// static final String str_NSView = "org.eclipse.swt.internal.cocoa.NSView";

	private static final Method swt_control_internal_new_GC;
	private static final Method swt_control_internal_dispose_GC;
	private static final String str_internal_new_GC = "internal_new_GC";
	private static final String str_internal_dispose_GC = "internal_dispose_GC";

	private static final String str_OS_gtk_class = "org.eclipse.swt.internal.gtk.OS"; // used by earlier versions of SWT
	private static final String str_GTK_gtk_class = "org.eclipse.swt.internal.gtk.GTK"; // used by later versions of SWT
	private static final String str_GTK3_gtk_class = "org.eclipse.swt.internal.gtk3.GTK3"; // used by later versions of
																							// SWT (4.21+)
	private static final String str_GDK_gtk_class = "org.eclipse.swt.internal.gtk.GDK"; // used by later versions of SWT
	public static final Class<?> OS_gtk_class;
	private static final String str_OS_gtk_version = "GTK_VERSION";
	public static final VersionNumber OS_gtk_version;

	private static final Method OS_gtk_widget_realize;
	private static final Method OS_gtk_widget_unrealize; // optional (removed in SWT 4.3)
	private static final Method OS_GTK_WIDGET_WINDOW;
	private static final Method OS_gtk_widget_get_window;
	private static final Method OS_gdk_x11_drawable_get_xdisplay;
	private static final Method OS_gdk_x11_display_get_xdisplay;
	private static final Method OS_gdk_window_get_display;
	private static final Method OS_gdk_x11_drawable_get_xid;
	private static final Method OS_gdk_x11_window_get_xid;
	private static final Method OS_gdk_window_set_back_pixmap;
	private static final Method OS_gdk_window_set_background_pattern;

	private static final String str_gtk_widget_realize = "gtk_widget_realize";
	private static final String str_gtk_widget_unrealize = "gtk_widget_unrealize";
	private static final String str_GTK_WIDGET_WINDOW = "GTK_WIDGET_WINDOW";
	private static final String str_gtk_widget_get_window = "gtk_widget_get_window";
	private static final String str_gdk_x11_drawable_get_xdisplay = "gdk_x11_drawable_get_xdisplay";
	private static final String str_gdk_x11_display_get_xdisplay = "gdk_x11_display_get_xdisplay";
	private static final String str_gdk_window_get_display = "gdk_window_get_display";
	private static final String str_gdk_x11_drawable_get_xid = "gdk_x11_drawable_get_xid";
	private static final String str_gdk_x11_window_get_xid = "gdk_x11_window_get_xid";
	private static final String str_gdk_window_set_back_pixmap = "gdk_window_set_back_pixmap";
	private static final String str_gdk_window_set_background_pattern = "gdk_window_set_background_pattern";

	private static final int SWT_VERSION_4_21 = 4946;

	private static final VersionNumber GTK_VERSION_2_14_0 = new VersionNumber(2, 14, 0);
	private static final VersionNumber GTK_VERSION_2_24_0 = new VersionNumber(2, 24, 0);
	private static final VersionNumber GTK_VERSION_2_90_0 = new VersionNumber(2, 90, 0);
	private static final VersionNumber GTK_VERSION_3_0_0 = new VersionNumber(3, 0, 0);

	private static VersionNumber GTK_VERSION(final int version) {
		// return (major << 16) + (minor << 8) + micro;
		final int micro = (version) & 0xff;
		final int minor = (version >> 8) & 0xff;
		final int major = (version >> 16) & 0xff;
		return new VersionNumber(major, minor, micro);
	}

	static {
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			@Override
			public Object run() {
				NativeWindowFactory.initSingleton(); // last resort ..
				return null;
			}
		});

		nwt = NativeWindowFactory.getNativeWindowType(false);
		isOSX = NativeWindowFactory.TYPE_MACOSX == nwt;
		isWindows = NativeWindowFactory.TYPE_WINDOWS == nwt;
		isX11 = NativeWindowFactory.TYPE_X11 == nwt;

		Method m = null;
		try {
			m = Control.class.getDeclaredMethod("getLocationInPixels");
			m.setAccessible(true);
		} catch (final Exception ex) {
			m = null;
			if (DEBUG) {
				System.err.println("getLocationInPixels not implemented: " + ex.getMessage());
			}
		}
		swt_control_locationInPixels = m;

		m = null;
		try {
			m = Control.class.getDeclaredMethod("getSizeInPixels");
			m.setAccessible(true);
		} catch (final Exception ex) {
			m = null;
			if (DEBUG) {
				System.err.println("getSizeInPixels not implemented: " + ex.getMessage());
			}
		}
		swt_control_sizeInPixels = m;

		m = null;
		try {
			m = Scrollable.class.getDeclaredMethod("getClientAreaInPixels");
			m.setAccessible(true);
		} catch (final Exception ex) {
			m = null;
			if (DEBUG) {
				System.err.println("getClientAreaInPixels not implemented: " + ex.getMessage());
			}
		}
		swt_scrollable_clientAreaInPixels = m;

		m = null;
		try {
			m = DPIUtil.class.getDeclaredMethod("getScalingFactor");
			m.setAccessible(true);
		} catch (final Exception ex) {
			m = null;
			if (DEBUG) {
				System.err.println("getScalingFactor not implemented: " + ex.getMessage());
			}
		}
		swt_dpiutil_getScalingFactor = m;

		Field f = null;
		if (!isOSX) {
			try {
				f = Control.class.getField(str_handle);
			} catch (final Exception ex) {
				throw new NativeWindowException(ex);
			}
		}
		swt_control_handle = f; // maybe null !

		boolean ulh;
		if (null != swt_control_handle) {
			ulh = swt_control_handle.getGenericType().toString().equals(long.class.toString());
		} else {
			ulh = Platform.is64Bit();
		}
		swt_uses_long_handles = ulh;
		// System.err.println("SWT long handles: " + swt_uses_long_handles);
		// System.err.println("Platform 64bit: "+Platform.is64Bit());

		m = null;
		try {
			m = ReflectionUtil.getMethod(Control.class, str_internal_new_GC, new Class[] { GCData.class });
		} catch (final Exception ex) {
			throw new NativeWindowException(ex);
		}
		swt_control_internal_new_GC = m;

		m = null;
		try {
			if (swt_uses_long_handles) {
				m = Control.class.getDeclaredMethod(str_internal_dispose_GC, new Class[] { long.class, GCData.class });
			} else {
				m = Control.class.getDeclaredMethod(str_internal_dispose_GC, new Class[] { int.class, GCData.class });
			}
		} catch (final NoSuchMethodException ex) {
			throw new NativeWindowException(ex);
		}
		swt_control_internal_dispose_GC = m;

		Class<?> cGTK = null;
		VersionNumber _gtk_version = new VersionNumber(0, 0, 0);
		Method m1 = null, m2 = null, m3 = null, m4 = null, m5 = null, m6 = null, m7 = null, m8 = null, m9 = null,
				ma = null, mb = null;
		final Class<?> handleType = swt_uses_long_handles ? long.class : int.class;
		if (isX11) {
			// mandatory
			try {
				final ClassLoader cl = SWTAccessor.class.getClassLoader();
				cGTK = ReflectionUtil.getClass(str_OS_gtk_class, false, cl);
				Field field_OS_gtk_version;
				Class<?> cGDK = cGTK; // used for newer versions of SWT that have a org.eclipse.swt.internal.gtk.GDK
										// object
				try {
					field_OS_gtk_version = cGTK.getField(str_OS_gtk_version);
				} catch (final NoSuchFieldException ex) {
					// if the GTK_VERSION field didn't exist in org.eclipse.swt.internal.gtk.OS,
					// then look for
					// it in org.eclipse.swt.internal.gtk.GTK, where it was moved in later versions
					// of SWT
					cGTK = ReflectionUtil.getClass(str_GTK_gtk_class, false, cl);
					field_OS_gtk_version = cGTK.getField(str_OS_gtk_version);
					cGDK = ReflectionUtil.getClass(str_GDK_gtk_class, false, cl);
				}
				_gtk_version = GTK_VERSION(field_OS_gtk_version.getInt(null));
				m1 = cGTK.getDeclaredMethod(str_gtk_widget_realize, handleType);
				if (_gtk_version.compareTo(GTK_VERSION_2_14_0) >= 0) {
					if (SWT.getVersion() < SWT_VERSION_4_21) {
						m4 = cGTK.getDeclaredMethod(str_gtk_widget_get_window, handleType);
					} else {
						Class<?> cGTK3 = ReflectionUtil.getClass(str_GTK3_gtk_class, false, cl);
						m4 = cGTK3.getDeclaredMethod(str_gtk_widget_get_window, handleType);
					}
				} else {
					m3 = cGTK.getDeclaredMethod(str_GTK_WIDGET_WINDOW, handleType);
				}
				if (_gtk_version.compareTo(GTK_VERSION_2_24_0) >= 0) {
					m6 = cGDK.getDeclaredMethod(str_gdk_x11_display_get_xdisplay, handleType);
					m7 = cGDK.getDeclaredMethod(str_gdk_window_get_display, handleType);
				} else {
					m5 = cGTK.getDeclaredMethod(str_gdk_x11_drawable_get_xdisplay, handleType);
				}
				if (_gtk_version.compareTo(GTK_VERSION_3_0_0) >= 0) {
					m9 = cGDK.getDeclaredMethod(str_gdk_x11_window_get_xid, handleType);
				} else {
					m8 = cGTK.getDeclaredMethod(str_gdk_x11_drawable_get_xid, handleType);
				}

				if (_gtk_version.compareTo(GTK_VERSION_2_90_0) >= 0) {
					mb = cGDK.getDeclaredMethod(str_gdk_window_set_background_pattern, handleType, handleType);
				} else {
					ma = cGTK.getDeclaredMethod(str_gdk_window_set_back_pixmap, handleType, handleType, boolean.class);
				}
			} catch (final Exception ex) {
				throw new NativeWindowException(ex);
			}
			// optional
			try {
				m2 = cGTK.getDeclaredMethod(str_gtk_widget_unrealize, handleType);
			} catch (final Exception ex) {
			}
		}
		OS_gtk_class = cGTK;
		OS_gtk_version = _gtk_version;
		OS_gtk_widget_realize = m1;
		OS_gtk_widget_unrealize = m2;
		OS_GTK_WIDGET_WINDOW = m3;
		OS_gtk_widget_get_window = m4;
		OS_gdk_x11_drawable_get_xdisplay = m5;
		OS_gdk_x11_display_get_xdisplay = m6;
		OS_gdk_window_get_display = m7;
		OS_gdk_x11_drawable_get_xid = m8;
		OS_gdk_x11_window_get_xid = m9;
		OS_gdk_window_set_back_pixmap = ma;
		OS_gdk_window_set_background_pattern = mb;

		isX11GTK = isX11 && null != OS_gtk_class;
	}

	private static Number getIntOrLong(final long arg) {
		if (swt_uses_long_handles) {
			return Long.valueOf(arg);
		}
		return Integer.valueOf((int) arg);
	}

	private static void callStaticMethodL2V(final Method m, final long arg) {
		ReflectionUtil.callMethod(null, m, new Object[] { getIntOrLong(arg) });
	}

	private static void callStaticMethodLL2V(final Method m, final long arg0, final long arg1) {
		ReflectionUtil.callMethod(null, m, new Object[] { getIntOrLong(arg0), getIntOrLong(arg1) });
	}

	private static void callStaticMethodLLZ2V(final Method m, final long arg0, final long arg1, final boolean arg3) {
		ReflectionUtil.callMethod(null, m,
				new Object[] { getIntOrLong(arg0), getIntOrLong(arg1), Boolean.valueOf(arg3) });
	}

	private static long callStaticMethodL2L(final Method m, final long arg) {
		final Object o = ReflectionUtil.callMethod(null, m, new Object[] { getIntOrLong(arg) });
		if (o instanceof Number) {
			return ((Number) o).longValue();
		} else {
			throw new InternalError("SWT method " + m.getName() + " didn't return int or long but " + o.getClass());
		}
	}

	//
	// Public properties
	//

	public static boolean isUsingLongHandles() {
		return swt_uses_long_handles;
	}

	public static boolean useX11GTK() {
		return isX11GTK;
	}

	public static VersionNumber GTK_VERSION() {
		return OS_gtk_version;
	}

	//
	// Common GTK
	//

	public static long gdk_widget_get_window(final long handle) {
		final long window;
		if (OS_gtk_version.compareTo(GTK_VERSION_2_14_0) >= 0) {
			window = callStaticMethodL2L(OS_gtk_widget_get_window, handle);
		} else {
			window = callStaticMethodL2L(OS_GTK_WIDGET_WINDOW, handle);
		}
		if (0 == window) {
			throw new NativeWindowException("Null gtk-window-handle of SWT handle 0x" + Long.toHexString(handle));
		}
		return window;
	}

	public static long gdk_window_get_xdisplay(final long window) {
		final long xdisplay;
		if (OS_gtk_version.compareTo(GTK_VERSION_2_24_0) >= 0) {
			final long display = callStaticMethodL2L(OS_gdk_window_get_display, window);
			if (0 == display) {
				throw new NativeWindowException(
						"Null display-handle of gtk-window-handle 0x" + Long.toHexString(window));
			}
			xdisplay = callStaticMethodL2L(OS_gdk_x11_display_get_xdisplay, display);
		} else {
			xdisplay = callStaticMethodL2L(OS_gdk_x11_drawable_get_xdisplay, window);
		}
		if (0 == xdisplay) {
			throw new NativeWindowException(
					"Null x11-display-handle of gtk-window-handle 0x" + Long.toHexString(window));
		}
		return xdisplay;
	}

	public static long gdk_window_get_xwindow(final long window) {
		final long xWindow;
		if (OS_gtk_version.compareTo(GTK_VERSION_3_0_0) >= 0) {
			xWindow = callStaticMethodL2L(OS_gdk_x11_window_get_xid, window);
		} else {
			xWindow = callStaticMethodL2L(OS_gdk_x11_drawable_get_xid, window);
		}
		if (0 == xWindow) {
			throw new NativeWindowException(
					"Null x11-window-handle of gtk-window-handle 0x" + Long.toHexString(window));
		}
		return xWindow;
	}

	public static void gdk_window_set_back_pixmap(final long window, final long pixmap, final boolean parent_relative) {
		if (OS_gdk_window_set_back_pixmap != null) {
			callStaticMethodLLZ2V(OS_gdk_window_set_back_pixmap, window, pixmap, parent_relative);
		}
		// in recent GTK, can't set background to pixmap any more; this sets it relative
		// to parent
		else if (OS_gdk_window_set_background_pattern != null) {
			callStaticMethodLL2V(OS_gdk_window_set_background_pattern, window, 0);
		}
	}

	//
	// Common any toolkit
	//
	public static void printInfo(final PrintStream out, final Display d) {
		out.println("SWT: Platform: " + SWT.getPlatform() + ", Version " + SWT.getVersion());
		out.println("SWT: isX11 " + isX11 + ", isX11GTK " + isX11GTK + " (GTK Version: " + OS_gtk_version + ")");
		out.println("SWT: isOSX " + isOSX + ", isWindows " + isWindows);
		out.println("SWT: DeviceZoom: " + DPIUtil.getDeviceZoom() + ", deviceZoomScalingFactor "
				+ getDeviceZoomScalingFactor());
		out.println("SWT: Display.DPI " + d.getDPI() + "; DPIUtil: autoScalingFactor " + getAutoScalingFactor()
				+ " (use-swt " + (null != swt_dpiutil_getScalingFactor) + "), useCairoAutoScale ");
	}

	//
	// SWT DPIUtil Compatible auto-scale functionality
	//
	/**
	 * Returns SWT compatible auto scale-factor, used by {@link DPIUtil}
	 * <p>
	 * We need to keep track of SWT's implementation in this regard!
	 * </p>
	 */
	public static float getAutoScalingFactor() throws NativeWindowException {
		if (null != swt_dpiutil_getScalingFactor) {
			try {
				return (float) swt_dpiutil_getScalingFactor.invoke(null);
			} catch (final Throwable e) {
				throw new NativeWindowException(e);
			}
		}
		// Mimick original code ..
		final int deviceZoom = DPIUtil.getDeviceZoom();
		if (100 == deviceZoom) {
			return 1f;
		}
		return deviceZoom / 100f;
	}

	/**
	 * Returns SWT auto scaled-up value {@code v}, compatible with
	 * {@link DPIUtil#autoScaleUp(int)}
	 * <p>
	 * We need to keep track of SWT's implementation in this regard!
	 * </p>
	 */
	public static int autoScaleUp(final int v) {
		final int deviceZoom = DPIUtil.getDeviceZoom();
		if (100 == deviceZoom) {
			return v;
		}
		final float scaleFactor = deviceZoom / 100f;
		return Math.round(v * scaleFactor);
	}

	/**
	 * Returns SWT auto scaled-down value {@code v}, compatible with
	 * {@link DPIUtil#autoScaleDown(int)}
	 * <p>
	 * We need to keep track of SWT's implementation in this regard!
	 * </p>
	 */
	public static int autoScaleDown(final int v) {
		final int deviceZoom = DPIUtil.getDeviceZoom();
		if (100 == deviceZoom) {
			return v;
		}
		final float scaleFactor = deviceZoom / 100f;
		return Math.round(v / scaleFactor);
	}

	//
	// SWT DPIUtil derived deviceZoom scale functionality,
	// not considering the higher-toolkit's compensation like
	// 'DPIUtil.useCairoAutoScale()'
	//
	/**
	 * Returns SWT derived scale-factor based on {@link DPIUtil#getDeviceZoom()}
	 * only, not considering higher-toolkit's compensation like
	 * {@link DPIUtil#useCairoAutoScale()}.
	 * <p>
	 * This method should be used instead of {@link #getAutoScalingFactor()} for
	 * native toolkits not caring about DPI scaling like X11 or GDI.
	 * </p>
	 * <p>
	 * We need to keep track of SWT's implementation in this regard!
	 * </p>
	 */
	public static float getDeviceZoomScalingFactor() {
		final int deviceZoom = DPIUtil.getDeviceZoom();
		if (100 == deviceZoom) {
			return 1f;
		}
		return deviceZoom / 100f;
	}

	/**
	 * Returns SWT derived scaled-up value {@code v}, based on
	 * {@link DPIUtil#getDeviceZoom()} only, not considering higher-toolkit's
	 * compensation like {@link DPIUtil#useCairoAutoScale()}.
	 * <p>
	 * This method should be used instead of {@link #autoScaleUp(int)} for native
	 * toolkits not caring about DPI scaling like X11 or GDI.
	 * </p>
	 * <p>
	 * We need to keep track of SWT's implementation in this regard!
	 * </p>
	 */
	public static int deviceZoomScaleUp(final int v) {
		final int deviceZoom = DPIUtil.getDeviceZoom();
		if (100 == deviceZoom) {
			return v;
		}
		final float scaleFactor = deviceZoom / 100f;
		return Math.round(v * scaleFactor);
	}

	/**
	 * Returns SWT derived scaled-down value {@code v}, based on
	 * {@link DPIUtil#getDeviceZoom()} only, not considering higher-toolkit's
	 * compensation like {@link DPIUtil#useCairoAutoScale()}.
	 * <p>
	 * This method should be used instead of {@link #autoScaleDown(int)} for native
	 * toolkits not caring about DPI scaling like X11 or GDI.
	 * </p>
	 * <p>
	 * We need to keep track of SWT's implementation in this regard!
	 * </p>
	 */
	public static int deviceZoomScaleDown(final int v) {
		final int deviceZoom = DPIUtil.getDeviceZoom();
		if (100 == deviceZoom) {
			return v;
		}
		final float scaleFactor = deviceZoom / 100f;
		return Math.round(v / scaleFactor);
	}

	/**
	 * Returns SWT derived scaled-up value {@code v}, based on
	 * {@link DPIUtil#getDeviceZoom()} only, not considering higher-toolkit's
	 * compensation like {@link DPIUtil#useCairoAutoScale()}.
	 * <p>
	 * This method should be used instead of
	 * {@link #autoScaleUp(com.jogamp.nativewindow.util.Point)} for native toolkits
	 * not caring about DPI scaling like X11 or GDI.
	 * </p>
	 * <p>
	 * We need to keep track of SWT's implementation in this regard!
	 * </p>
	 */
	public static com.jogamp.nativewindow.util.Point deviceZoomScaleUp(final com.jogamp.nativewindow.util.Point v) {
		final int deviceZoom = DPIUtil.getDeviceZoom();
		if (100 == deviceZoom || null == v) {
			return v;
		}
		final float scaleFactor = deviceZoom / 100f;
		return v.set(Math.round(v.getX() * scaleFactor), Math.round(v.getY() * scaleFactor));
	}

	/**
	 * Returns SWT derived scaled-down value {@code v}, based on
	 * {@link DPIUtil#getDeviceZoom()} only, not considering higher-toolkit's
	 * compensation like {@link DPIUtil#useCairoAutoScale()}.
	 * <p>
	 * This method should be used instead of
	 * {@link #autoScaleDown(com.jogamp.nativewindow.util.Point)} for native
	 * toolkits not caring about DPI scaling like X11 or GDI.
	 * </p>
	 * <p>
	 * We need to keep track of SWT's implementation in this regard!
	 * </p>
	 */
	public static com.jogamp.nativewindow.util.Point deviceZoomScaleDown(final com.jogamp.nativewindow.util.Point v) {
		final int deviceZoom = DPIUtil.getDeviceZoom();
		if (100 == deviceZoom || null == v) {
			return v;
		}
		final float scaleFactor = deviceZoom / 100f;
		return v.set(Math.round(v.getX() / scaleFactor), Math.round(v.getY() / scaleFactor));
	}

	/**
	 * Returns the unscaled {@link Scrollable#getClientArea()} in pixels.
	 * <p>
	 * If the package restricted method {@link Scrollable#getClientAreaInPixels()}
	 * is implemented, we return its result.
	 * </p>
	 * <p>
	 * Fallback is to return {@link DPIUtil#autoScaleUp(Rectangle)
	 * DPIUtil#autoScaleUp}({@link Scrollable#getClientArea()}), reverting
	 * {@link Scrollable#getClientArea()}'s
	 * {@link DPIUtil#autoScaleDown(Rectangle)}.
	 * </p>
	 * <p>
	 * Note to SWT's API spec writers: You need to allow access to the unscaled
	 * value, scale properties and define what is being scaled (fonts, images, ..).
	 * Further more the scale should be separate for x/y coordinates, as DPI differs
	 * here.
	 * </p>
	 * <p>
	 * Note to Eclipse authors: Scaling up the fonts and images hardly works on
	 * GTK/SWT/Eclipse. GDK_SCALE, GDK_DPI_SCALE and swt.autoScale produce
	 * inconsistent results with Eclipse. Broken High-DPI for .. some years now.
	 * This is especially true for using native high-dpi w/ scaling-factor 1f.
	 * </p>
	 *
	 * Requires SWT >= 3.105 (DPIUtil)
	 *
	 * @param s the {@link Scrollable} instance
	 * @return unscaled client area in pixels, see above
	 * @throws NativeWindowException during invocation of the method, if any
	 */
	public static Rectangle getClientAreaInPixels(final Scrollable s) throws NativeWindowException {
		if (null == swt_scrollable_clientAreaInPixels) {
			return DPIUtil.autoScaleUp(s.getClientArea());
		}
		try {
			return (Rectangle) swt_scrollable_clientAreaInPixels.invoke(s);
		} catch (final Throwable e) {
			throw new NativeWindowException(e);
		}
	}

	public static Point getLocationInPixels(final Control c) throws NativeWindowException {
		if (null == swt_control_locationInPixels) {
			return DPIUtil.autoScaleUp(c.getLocation());
		}
		try {
			return (Point) swt_control_locationInPixels.invoke(c);
		} catch (final Throwable e) {
			throw new NativeWindowException(e);
		}
	}

	public static Point getSizeInPixels(final Control c) throws NativeWindowException {
		if (null == swt_control_sizeInPixels) {
			return DPIUtil.autoScaleUp(c.getSize());
		}
		try {
			return (Point) swt_control_sizeInPixels.invoke(c);
		} catch (final Throwable e) {
			throw new NativeWindowException(e);
		}
	}

	/**
	 * @param swtControl the SWT Control to retrieve the native widget-handle from
	 * @return the native widget-handle
	 * @throws NativeWindowException if the widget handle is null
	 */
	public static long getHandle(final Control swtControl) throws NativeWindowException {
		long h = 0;
		if (isOSX) {
			synchronized (swt_osx_init) {
				try {
					if (null == swt_osx_view_id) {
						swt_osx_control_view = Control.class.getField(str_osx_view);
						final Object view = swt_osx_control_view.get(swtControl);
						swt_osx_view_id = view.getClass().getField(str_osx_id);
						h = swt_osx_view_id.getLong(view);
					} else {
						h = swt_osx_view_id.getLong(swt_osx_control_view.get(swtControl));
					}
				} catch (final Exception ex) {
					throw new NativeWindowException(ex);
				}
			}
		} else {
			try {
				h = swt_control_handle.getLong(swtControl);
			} catch (final Exception ex) {
				throw new NativeWindowException(ex);
			}
		}
		if (0 == h) {
			throw new NativeWindowException(
					"Null widget-handle of SWT " + swtControl.getClass().getName() + ": " + swtControl.toString());
		}
		return h;
	}

	public static void setRealized(final Control swtControl, final boolean realize) throws NativeWindowException {
		if (!realize && swtControl.isDisposed()) {
			return;
		}
		final long handle = getHandle(swtControl);

		if (null != OS_gtk_class) {
			invokeOnOSTKThread(true, new Runnable() {
				@Override
				public void run() {
					if (realize) {
						callStaticMethodL2V(OS_gtk_widget_realize, handle);
					} else if (null != OS_gtk_widget_unrealize) {
						callStaticMethodL2V(OS_gtk_widget_unrealize, handle);
					}
				}
			});
		}
	}

	/**
	 * @param swtControl the SWT Control to retrieve the native device handle from
	 * @return the AbstractGraphicsDevice w/ the native device handle
	 * @throws NativeWindowException         if the widget handle is null
	 * @throws UnsupportedOperationException if the windowing system is not
	 *                                       supported
	 */
	public static AbstractGraphicsDevice getDevice(final Control swtControl)
			throws NativeWindowException, UnsupportedOperationException {
		final long handle = getHandle(swtControl);
		if (isX11GTK) {
			final long xdisplay0 = gdk_window_get_xdisplay(gdk_widget_get_window(handle));
			return new X11GraphicsDevice(xdisplay0, AbstractGraphicsDevice.DEFAULT_UNIT, false /* owner */);
		}
		if (isWindows) {
			return new WindowsGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
		}
		if (isOSX) {
			return new MacOSXGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
		}
		throw new UnsupportedOperationException("n/a for this windowing system: " + nwt);
	}

	/**
	 * @param device
	 * @param screen -1 is default screen of the given device, e.g. maybe 0 or
	 *               determined by native API. >= 0 is specific screen
	 * @return
	 */
	public static AbstractGraphicsScreen getScreen(final AbstractGraphicsDevice device, final int screen) {
		return NativeWindowFactory.createScreen(device, screen);
	}

	public static int getNativeVisualID(final AbstractGraphicsDevice device, final long windowHandle) {
		if (isX11) {
			return X11Lib.GetVisualIDFromWindow(device.getHandle(), windowHandle);
		}
		if (isWindows || isOSX) {
			return VisualIDHolder.VID_UNDEFINED;
		}
		throw new UnsupportedOperationException("n/a for this windowing system: " + nwt);
	}

	/**
	 * @param swtControl the SWT Control to retrieve the native window handle from
	 * @return the native window handle
	 * @throws NativeWindowException         if the widget handle is null
	 * @throws UnsupportedOperationException if the windowing system is not
	 *                                       supported
	 */
	public static long getWindowHandle(final Control swtControl)
			throws NativeWindowException, UnsupportedOperationException {
		final long handle = getHandle(swtControl);
		if (0 == handle) {
			throw new NativeWindowException("Null SWT handle of SWT control " + swtControl);
		}
		if (isX11GTK) {
			return gdk_window_get_xwindow(gdk_widget_get_window(handle));
		}
		if (isWindows || isOSX) {
			return handle;
		}
		throw new UnsupportedOperationException("n/a for this windowing system: " + nwt);
	}

	public static long newGC(final Control swtControl, final GCData gcData) {
		final Object[] o = new Object[1];
		invokeOnOSTKThread(true, new Runnable() {
			@Override
			public void run() {
				o[0] = ReflectionUtil.callMethod(swtControl, swt_control_internal_new_GC, new Object[] { gcData });
			}
		});
		if (o[0] instanceof Number) {
			return ((Number) o[0]).longValue();
		} else {
			throw new InternalError("SWT internal_new_GC did not return int or long but " + o[0].getClass());
		}
	}

	public static void disposeGC(final Control swtControl, final long gc, final GCData gcData) {
		invokeOnOSTKThread(true, new Runnable() {
			@Override
			public void run() {
				if (swt_uses_long_handles) {
					ReflectionUtil.callMethod(swtControl, swt_control_internal_dispose_GC,
							new Object[] { Long.valueOf(gc), gcData });
				} else {
					ReflectionUtil.callMethod(swtControl, swt_control_internal_dispose_GC,
							new Object[] { Integer.valueOf((int) gc), gcData });
				}
			}
		});
	}

	/**
	 * Runs the specified action in an SWT compatible OS toolkit thread, which is:
	 * <ul>
	 * <li>Mac OSX
	 * <ul>
	 * <!--li>AWT EDT: In case AWT is available, the AWT EDT is the OSX UI main
	 * thread</li-->
	 * <li><i>Main Thread</i>: Run on OSX UI main thread. 'wait' is implemented on
	 * Java site via lock/wait on {@link RunnableTask} to not freeze OSX main
	 * thread.</li>
	 * </ul>
	 * </li>
	 * <li>Linux, Windows, ..
	 * <ul>
	 * <li>Current thread.</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * 
	 * @see Platform#AWT_AVAILABLE
	 * @see Platform#getOSType()
	 */
	public static void invokeOnOSTKThread(final boolean blocking, final Runnable runnable) {
		if (isOSX) {
			// Use SWT main thread! Only reliable config w/ -XStartOnMainThread !?
			OSXUtil.RunOnMainThread(blocking, false, runnable);
		} else {
			runnable.run();
		}
	}

	/**
	 * Runs the specified action on the SWT UI thread.
	 * <p>
	 * If {@code blocking} is {@code true} implementation uses
	 * {@link org.eclipse.swt.widgets.Display#syncExec(Runnable)}, otherwise
	 * {@link org.eclipse.swt.widgets.Display#asyncExec(Runnable)}.
	 * <p>
	 * If <code>display</code> is {@code null} or disposed or the current thread is
	 * the SWT UI thread {@link #invokeOnOSTKThread(boolean, Runnable)} is being
	 * used.
	 * 
	 * @see #invokeOnOSTKThread(boolean, Runnable)
	 */
	public static void invokeOnSWTThread(final org.eclipse.swt.widgets.Display display, final boolean blocking,
			final Runnable runnable) {
		if (null == display || display.isDisposed() || Thread.currentThread() == display.getThread()) {
			invokeOnOSTKThread(blocking, runnable);
		} else if (blocking) {
			display.syncExec(runnable);
		} else {
			display.asyncExec(runnable);
		}
	}

	/** Return true if the current thread is the SWT UI thread, otherwise false. */
	public static boolean isOnSWTThread(final org.eclipse.swt.widgets.Display display) {
		return null != display && Thread.currentThread() == display.getThread();
	}

	//
	// Specific X11 GTK ChildWindow - Using plain X11 native parenting (works well)
	//

	public static long createCompatibleX11ChildWindow(final AbstractGraphicsScreen screen, final Control swtControl,
			final int visualID, final int width, final int height) {
		final long handle = getHandle(swtControl);
		final long parentWindow = gdk_widget_get_window(handle);
		// gdk_window_set_back_pixmap(parentWindow, 0, false);

		final long x11ParentHandle = gdk_window_get_xwindow(parentWindow);
		final long x11WindowHandle = X11Lib.CreateWindow(x11ParentHandle, screen.getDevice().getHandle(),
				screen.getIndex(), visualID, width, height, true, true);

		return x11WindowHandle;
	}

	public static void resizeX11Window(final AbstractGraphicsDevice device, final Rectangle clientArea,
			final long x11Window) {
		X11Lib.SetWindowPosSize(device.getHandle(), x11Window, clientArea.x, clientArea.y, clientArea.width,
				clientArea.height);
	}

	public static void destroyX11Window(final AbstractGraphicsDevice device, final long x11Window) {
		X11Lib.DestroyWindow(device.getHandle(), x11Window);
	}

	//
	// Specific X11 SWT/GTK ChildWindow - Using SWT/GTK native parenting (buggy -
	// sporadic resize flickering, sporadic drop of rendering)
	//
	// FIXME: Need to use reflection for 32bit access as well !
	//

	// public static final int GDK_WA_TYPE_HINT = 1 << 9;
	// public static final int GDK_WA_VISUAL = 1 << 6;

	public static long createCompatibleGDKChildWindow(final Control swtControl, final int visualID, final int width,
			final int height) {
		return 0;
		/**
		 * final long handle = SWTAccessor.getHandle(swtControl); final long
		 * parentWindow = gdk_widget_get_window( handle );
		 * 
		 * final long screen = OS.gdk_screen_get_default (); final long gdkvisual =
		 * OS.gdk_x11_screen_lookup_visual (screen, visualID);
		 * 
		 * final GdkWindowAttr attrs = new GdkWindowAttr(); attrs.width = width > 0 ?
		 * width : 1; attrs.height = height > 0 ? height : 1; attrs.event_mask =
		 * OS.GDK_KEY_PRESS_MASK | OS.GDK_KEY_RELEASE_MASK | OS.GDK_FOCUS_CHANGE_MASK |
		 * OS.GDK_POINTER_MOTION_MASK | OS.GDK_BUTTON_PRESS_MASK |
		 * OS.GDK_BUTTON_RELEASE_MASK | OS.GDK_ENTER_NOTIFY_MASK |
		 * OS.GDK_LEAVE_NOTIFY_MASK | OS.GDK_EXPOSURE_MASK |
		 * OS.GDK_VISIBILITY_NOTIFY_MASK | OS.GDK_POINTER_MOTION_HINT_MASK;
		 * attrs.window_type = OS.GDK_WINDOW_CHILD; attrs.visual = gdkvisual;
		 * 
		 * final long childWindow = OS.gdk_window_new (parentWindow, attrs,
		 * OS.GDK_WA_VISUAL|GDK_WA_TYPE_HINT); OS.gdk_window_set_user_data (childWindow,
		 * handle); OS.gdk_window_set_back_pixmap (parentWindow, 0, false);
		 * 
		 * OS.gdk_window_show (childWindow); OS.gdk_flush(); return childWindow;
		 */
	}

	public static void showGDKWindow(final long gdkWindow) {
		/*
		 * OS.gdk_window_show (gdkWindow); OS.gdk_flush();
		 */
	}

	public static void focusGDKWindow(final long gdkWindow) {
		/*
		 * OS.gdk_window_show (gdkWindow); OS.gdk_window_focus(gdkWindow, 0);
		 * OS.gdk_flush();
		 */
	}

	public static void resizeGDKWindow(final Rectangle clientArea, final long gdkWindow) {
		/**
		 * OS.gdk_window_move (gdkWindow, clientArea.x, clientArea.y);
		 * OS.gdk_window_resize (gdkWindow, clientArea.width, clientArea.height);
		 * OS.gdk_flush();
		 */
	}

	public static void destroyGDKWindow(final long gdkWindow) {
		// OS.gdk_window_destroy (gdkWindow);
	}
}
