package com.eco.bio7.spatial;

import java.util.ArrayList;
import java.util.List;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
 * The values of a stack which are displayed as a volume or an iso surface: all
 * values (the default), a range of values (e.g., the threshold of the image in
 * ImageJ: only the particles) or a list of values (the labels of a
 * segmentation).
 * <p>
 * The iso surface of a range is the surface of the values at both limits
 * (interpolated like the iso surface of a value), the iso surface of a list of
 * labels is the surface of the voxels with one of the labels (the border
 * between a selected and another voxel, half way between the centers of the
 * voxels).
 */
public final class ValueSelection {

	/** All values (no selection). */
	public static final ValueSelection ALL = new ValueSelection(Kind.ALL, new double[0][], "All Values");
	/**
	 * The threshold of the image in ImageJ (the lower and the upper limit), read
	 * when the stack is loaded (see resolve).
	 */
	public static final ValueSelection IMAGE_THRESHOLD = new ValueSelection(Kind.IMAGE_THRESHOLD, new double[0][],
			"Threshold of the Image");

	private enum Kind {
		ALL, IMAGE_THRESHOLD, RANGE, LABELS
	}

	private final Kind kind;
	/* The inclusive ranges of the selected values */
	private final double[][] ranges;
	private final String text;

	private ValueSelection(Kind kind, double[][] ranges, String text) {
		this.kind = kind;
		this.ranges = ranges;
		this.text = text;
	}

	/**
	 * The values from lower to upper (inclusive), e.g., a threshold. The surface is
	 * interpolated at both limits.
	 *
	 * @param lower the lower limit (-Infinity: no lower limit)
	 * @param upper the upper limit (Infinity: no upper limit)
	 */
	public static ValueSelection range(double lower, double upper) {
		if (Double.isNaN(lower) || Double.isNaN(upper) || lower > upper) {
			throw new IllegalArgumentException("The lower limit must be smaller than or equal to the upper limit!");
		}
		return new ValueSelection(Kind.RANGE, new double[][] { { lower, upper } },
				"Range " + number(lower) + " - " + number(upper));
	}

	/**
	 * The labels of a segmentation (each value is an object or a class).
	 *
	 * @param labels the selected values
	 */
	public static ValueSelection labels(double... labels) {
		if (labels == null || labels.length == 0) {
			throw new IllegalArgumentException("No labels!");
		}
		double[][] r = new double[labels.length][];
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < labels.length; i++) {
			r[i] = new double[] { labels[i], labels[i] };
			b.append(i > 0 ? ", " : "").append(number(labels[i]));
		}
		return new ValueSelection(Kind.LABELS, r, "Labels " + b);
	}

	/**
	 * The labels of a segmentation as a text: values and ranges separated by
	 * commas or spaces, e.g., "1, 5, 10-20" (a range includes both limits).
	 *
	 * @param list the labels
	 * @throws IllegalArgumentException if the text is not a list of labels
	 */
	public static ValueSelection labels(String list) {
		if (list == null || list.trim().isEmpty()) {
			throw new IllegalArgumentException("No labels!");
		}
		List<double[]> r = new ArrayList<>();
		for (String part : list.trim().split("[,;\\s]+")) {
			if (part.isEmpty()) {
				continue;
			}
			/* A range "a-b" (a minus sign at the start is the sign of a) */
			int dash = part.indexOf('-', 1);
			try {
				if (dash > 0) {
					double a = Double.parseDouble(part.substring(0, dash));
					double b = Double.parseDouble(part.substring(dash + 1));
					r.add(new double[] { Math.min(a, b), Math.max(a, b) });
				} else {
					double a = Double.parseDouble(part);
					r.add(new double[] { a, a });
				}
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Not a label or a range of labels: " + part);
			}
		}
		if (r.isEmpty()) {
			throw new IllegalArgumentException("No labels!");
		}
		return new ValueSelection(Kind.LABELS, r.toArray(new double[0][]), "Labels " + list.trim());
	}

	/**
	 * Returns the selection for a stack: the threshold of the image for
	 * IMAGE_THRESHOLD (ALL if no threshold is set or for RGB images), else this
	 * selection.
	 */
	public ValueSelection resolve(ImagePlus imp) {
		if (kind != Kind.IMAGE_THRESHOLD) {
			return this;
		}
		ImageProcessor ip = imp != null ? imp.getProcessor() : null;
		if (ip == null || ip instanceof ColorProcessor || ip.getMinThreshold() == ImageProcessor.NO_THRESHOLD) {
			return ALL;
		}
		double lo = ip.getMinThreshold(), hi = ip.getMaxThreshold();
		/*
		 * Like the threshold of ImageJ (both limits are included): for integer values
		 * the surface is half way to the next value outside (e.g., 128 - 255: the
		 * surface at 127.5); a limit at the minimum or the maximum of the data is no
		 * limit (else the voxels with this value would be on the surface)
		 */
		double[] range = dataRange(imp);
		boolean integer = ip instanceof ij.process.ByteProcessor || ip instanceof ij.process.ShortProcessor;
		double lower = lo <= range[0] ? Double.NEGATIVE_INFINITY : integer ? lo - 0.5 : lo;
		double upper = hi >= range[1] ? Double.POSITIVE_INFINITY : integer ? hi + 0.5 : hi;
		return new ValueSelection(Kind.RANGE, new double[][] { { lower, upper } },
				"Threshold " + number(lo) + " - " + number(hi));
	}

	/* The minimum and the maximum of the values of the stack (channel and frame of the image) */
	private static double[] dataRange(ImagePlus imp) {
		ImageProcessor ip = imp.getProcessor();
		if (ip instanceof ij.process.ByteProcessor) {
			return new double[] { 0, 255 };
		}
		if (ip instanceof ij.process.ShortProcessor) {
			return new double[] { 0, 65535 };
		}
		double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
		ij.ImageStack stack = imp.getStack();
		for (int z = 1; z <= imp.getNSlices(); z++) {
			ImageProcessor p = stack.getProcessor(imp.getStackIndex(imp.getChannel(), z, imp.getFrame()));
			int n = p.getWidth() * p.getHeight();
			for (int i = 0; i < n; i++) {
				double v = p instanceof ij.process.DoubleProcessor ? ((ij.process.DoubleProcessor) p).getd(i) : p.getf(i);
				if (v < min) {
					min = v;
				}
				if (v > max) {
					max = v;
				}
			}
		}
		return new double[] { min, max };
	}

	/** Returns true if all values are selected (no selection). */
	public boolean isAll() {
		return kind == Kind.ALL;
	}

	/** Returns true for the threshold of the image (resolved when a stack is loaded). */
	public boolean isImageThreshold() {
		return kind == Kind.IMAGE_THRESHOLD;
	}

	/** Returns true for a range of values. */
	public boolean isRange() {
		return kind == Kind.RANGE;
	}

	/** Returns true for a list of labels. */
	public boolean isLabels() {
		return kind == Kind.LABELS;
	}

	/** Returns true if the value is selected. */
	public boolean contains(double value) {
		if (kind == Kind.ALL || kind == Kind.IMAGE_THRESHOLD) {
			return true;
		}
		for (double[] r : ranges) {
			if (value >= r[0] && value <= r[1]) {
				return true;
			}
		}
		return false;
	}

	/*
	 * The field of the iso surface of the selection (the iso value is isoLevel):
	 * a range: min(value - lower, upper - value), interpolated at both limits;
	 * labels: 1 for the selected voxels, else 0. The values are stored as value -
	 * offset (64-bit stacks).
	 */
	float[] field(float[] values, double offset) {
		float[] f = new float[values.length];
		if (kind == Kind.RANGE) {
			double lower = ranges[0][0] - offset, upper = ranges[0][1] - offset;
			for (int i = 0; i < values.length; i++) {
				double v = values[i];
				/* Without a limit: the distance to the other limit (all values: 1) */
				double a = Double.isInfinite(lower) ? Double.POSITIVE_INFINITY : v - lower;
				double b = Double.isInfinite(upper) ? Double.POSITIVE_INFINITY : upper - v;
				double m = Math.min(a, b);
				f[i] = (float) (Double.isInfinite(m) ? 1 : m);
			}
		} else {
			for (int i = 0; i < values.length; i++) {
				f[i] = contains(values[i] + offset) ? 1f : 0f;
			}
		}
		return f;
	}

	/* The iso value of the field */
	double isoLevel() {
		return kind == Kind.RANGE ? 0 : 0.5;
	}

	/** Returns a description, e.g., "Threshold 50 - 255" or "Labels 1, 5, 10-20". */
	@Override
	public String toString() {
		return text;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ValueSelection)) {
			return false;
		}
		ValueSelection s = (ValueSelection) o;
		return kind == s.kind && java.util.Arrays.deepEquals(ranges, s.ranges);
	}

	@Override
	public int hashCode() {
		return kind.hashCode() * 31 + java.util.Arrays.deepHashCode(ranges);
	}

	private static String number(double v) {
		if (v == Double.POSITIVE_INFINITY) {
			return "max";
		}
		if (v == Double.NEGATIVE_INFINITY) {
			return "min";
		}
		return v == Math.rint(v) && Math.abs(v) < 1e15 ? String.valueOf((long) v) : String.valueOf(v);
	}
}
