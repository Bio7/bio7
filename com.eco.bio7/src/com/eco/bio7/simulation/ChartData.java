/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.simulation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A chart of a model: a line chart of values over the steps (chart), an XY
 * chart (chartXY, e.g., a phase plot) or a bar chart (chartBars, replaced in
 * each call). Shown in the Charts view of Bio7 and in the HTML page.
 */
public final class ChartData {

	/** The kinds of the charts. */
	public static final String LINE = "line", XY = "xy", BARS = "bars";

	/** The title of the chart ("" for the first chart of chart(name, value)). */
	public final String title;

	/** The kind: line, xy or bars. */
	public final String kind;

	/** The series: the name and the points (x, y); x is the step for a line chart. */
	public final Map<String, List<double[]>> series = new LinkedHashMap<>();

	/** The labels and the values of a bar chart. */
	public String[] labels = new String[0];
	public double[] values = new double[0];

	ChartData(String title, String kind) {
		this.title = title;
		this.kind = kind;
	}

	void add(String name, double x, double y) {
		series.computeIfAbsent(name, k -> new ArrayList<>()).add(new double[] { x, y });
	}

	/** A copy (for a display in another thread). */
	ChartData copy() {
		ChartData c = new ChartData(title, kind);
		for (Map.Entry<String, List<double[]>> e : series.entrySet()) {
			c.series.put(e.getKey(), new ArrayList<>(e.getValue()));
		}
		c.labels = labels.clone();
		c.values = values.clone();
		return c;
	}
}
