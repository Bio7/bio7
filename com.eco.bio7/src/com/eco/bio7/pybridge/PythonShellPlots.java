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
package com.eco.bio7.pybridge;

import java.util.ArrayList;
import java.util.List;

import com.eco.bio7.plot.PlotData;
import com.eco.bio7.plot.PlotOptions;

/**
 * The Python source of the plots of the context menu of the Python-Shell
 * (matplotlib, numpy; pandas and scipy optional): a readable script for the
 * plot type (the source can be copied into a Python editor for a
 * customization). The selected objects (arrays, lists, Series and the columns
 * of 2-d arrays and DataFrames) with the attributes of PlotOptions; the modern
 * style as the modern R plots.
 */
public final class PythonShellPlots {

	private PythonShellPlots() {
	}

	/** The Python source which plots the objects (the values). */
	public static String code(List<String> names, PlotOptions o) {
		return code(PlotData.of(names), o);
	}

	/** The Python source which plots the data (the roles of the plot type). */
	public static String code(PlotData d, PlotOptions o) {
		String kind = o.kind();
		boolean matrixZ = PlotData.usesZ(kind) && !d.z().isEmpty();
		/* With a matrix Z the first Y gives the values of the y axis */
		List<String> names = matrixZ ? List.of() : d.ys();
		List<String> p = new ArrayList<>();
		p.add("# " + PlotOptions.label(kind) + " of " + d.describe() + " (Bio7 Python-Shell): customize and run it in the Python editor of Bio7");
		p.add("import numpy as np");
		p.add("import matplotlib.pyplot as plt");
		p.add("");
		p.add("");
		p.add("def bio7_plot(objs, x=None, group=None, z=None, yaxis=None):");
		p.add("    # The data (objs: the names and the objects (Y), x, group, z: a matrix, yaxis: the values of its y axis)");
		p.add("    xv = None if x is None else np.asarray(x, dtype=float)");
		p.add("    g = None if group is None else np.asarray(group).astype(str)");
		p.add("    num, cat, table = {}, {}, None");
		p.add("    for name, v in objs.items():");
		p.add("        if hasattr(v, 'columns'):  # a DataFrame");
		p.add("            if table is None:");
		p.add("                table = (name, v.select_dtypes('number'))");
		p.add("            for c in v.columns:");
		p.add("                key = str(c) if len(objs) == 1 else '%s[%r]' % (name, c)");
		p.add("                if v[c].dtype.kind in 'iuf':");
		p.add("                    num[key] = v[c].to_numpy(dtype=float)");
		p.add("                else:");
		p.add("                    cat[key] = v[c].astype(str).to_numpy()");
		p.add("            continue");
		p.add("        a = np.asarray(v)");
		p.add("        if a.dtype.kind in 'iuf' and a.ndim == 2:");
		p.add("            if table is None:");
		p.add("                table = (name, a)");
		p.add("            for j in range(a.shape[1]):");
		p.add("                num['%s[:, %d]' % (name, j)] = a[:, j].astype(float)");
		p.add("        elif a.dtype.kind in 'iuf':");
		p.add("            num[name] = a.ravel().astype(float)");
		p.add("        else:");
		p.add("            cat[name] = a.ravel().astype(str)");
		if (kind.matches("hist|density|ecdf|box|violin|strip|qq")) {
			/* The values of each variable split into the groups */
			p.add("    if g is not None:");
			p.add("        split = {}");
			p.add("        for k, v in num.items():");
			p.add("            if len(v) != len(g):");
			p.add("                raise ValueError('The group has another length than %s' % k)");
			p.add("            for level in sorted(set(g)):");
			p.add("                split[('%s: %s' % (k, level)) if len(num) > 1 else level] = v[g == level]");
			p.add("        num = split");
		}
		p.add("    keys = list(num)");
		p.add("    clean = {k: v[np.isfinite(v)] for k, v in num.items()}");
		if (!kind.equals("bar") && !kind.equals("pie") && !matrixZ) {
			p.add("    if not keys:");
			p.add("        raise ValueError('No numeric data to plot')");
		}
		p.add("");
		p.add("    # The attributes");
		StringBuilder cols = new StringBuilder("    colors = [");
		String[] pc = o.plotColors();
		for (int i = 0; i < pc.length; i++) {
			cols.append(i > 0 ? ", " : "").append(str(pc[i]));
		}
		p.add(cols.append("]").toString());
		p.add("    colors = colors * (max(len(num), len(cat), 1) // len(colors) + 1)");
		p.add("    alpha, lw, ls, marker, size = " + n(o.opacity()) + ", " + n(o.lineWidth()) + ", " + str(lineStyle(o)) + ", " + str(marker(o)) + ", "
				+ n(o.pointSize()));
		p.add("    xlim, ylim = " + lim(o.xMin(), o.xMax()) + ", " + lim(o.yMin(), o.yMax()) + "  # e.g. (0, 10)");
		p.add("    title, subtitle, caption = " + str(o.title().isEmpty() ? autoTitle(kind, d) : o.title()) + ", " + str(o.subtitle()) + ", " + str(o.caption()));
		p.add("    points = " + (o.modern() ? "dict(edgecolors='white', linewidths=1.2)" : "dict()") + "  # the border of the points");
		p.add("");
		p.add("    # The style");
		p.add("    style = {'font.size': " + n(o.fontSize()) + ", 'axes.titlesize': " + n(o.titleSize()) + ", 'axes.labelsize': " + n(o.fontSize()) + "}");
		if (o.modern()) {
			p.add("    style.update({'axes.spines.top': False, 'axes.spines.right': False, 'axes.edgecolor': '#4D4D4D', 'axes.linewidth': 0.8,");
			p.add("                  'axes.labelcolor': '#333333', 'axes.labelweight': 'bold', 'xtick.color': '#4D4D4D', 'ytick.color': '#4D4D4D',");
			p.add("                  'axes.grid': True, 'axes.axisbelow': True, 'grid.color': '#E5E5E5', 'grid.linestyle': '-', 'grid.linewidth': 0.8,");
			p.add("                  'legend.frameon': False, 'figure.facecolor': 'white', 'axes.facecolor': 'white'})");
		} else if (o.grid()) {
			p.add("    style.update({'axes.grid': True, 'grid.linestyle': ':', 'grid.color': '#B0B0B0'})");
		}
		p.add("    with plt.rc_context(style):");
		List<String> body = plot(o, d);
		for (String line : body) {
			p.add(line.isEmpty() ? "" : "        " + line);
		}
		p.add("");
		p.add("");
		StringBuilder objs = new StringBuilder("bio7_plot({");
		for (int i = 0; i < names.size(); i++) {
			objs.append(i > 0 ? ", " : "").append(str(names.get(i))).append(": ").append(names.get(i));
		}
		objs.append("}");
		if (!d.x().isEmpty()) {
			objs.append(", x=").append(d.x());
		}
		if (!d.group().isEmpty()) {
			objs.append(", group=").append(d.group());
		}
		if (matrixZ) {
			objs.append(", z=").append(d.z());
			if (!d.ys().isEmpty()) {
				objs.append(", yaxis=").append(d.ys().get(0));
			}
		}
		p.add(objs.append(")").toString());
		return String.join("\n", p) + "\n";
	}

	/* ---------------- The plot types (in the style context) ---------------- */

	private static List<String> plot(PlotOptions o, PlotData d) {
		List<String> b = new ArrayList<>();
		boolean matrixZ = PlotData.usesZ(o.kind()) && !d.z().isEmpty();
		String kind = o.kind();
		boolean own = kind.equals("pairs") || kind.equals("surface");
		if (!own) {
			b.add("fig, ax = plt.subplots()");
			b.add("xl = yl = ''");
		}
		switch (kind) {
		case "line", "area" -> {
			b.add("for i, k in enumerate(keys):");
			b.add("    v = num[k]");
			b.add("    xs = np.arange(len(v)) if xv is None else xv[:len(v)]  # the x values of a line");
			b.add("    v = v[:len(xs)]");
			if (kind.equals("area")) {
				b.add("    ax.fill_between(xs, v, color=colors[i], alpha=0.35 * alpha, lw=0)");
			}
			b.add("    ax.plot(xs, v, color=colors[i], lw=lw, ls=ls, alpha=alpha, label=k)");
			b.add("xl, yl = " + (d.x().isEmpty() ? "'Index'" : str(d.x())) + ", 'Value'");
		}
		case "scatter" -> {
			if (d.x().isEmpty()) {
				b.add("if len(keys) >= 2:");
				b.add("    m = min(len(num[keys[0]]), len(num[keys[1]]))");
				b.add("    xs, ys, xl, yl = num[keys[0]][:m], {keys[1]: num[keys[1]][:m]}, keys[0], keys[1]");
				b.add("else:");
				b.add("    xs, ys, xl, yl = np.arange(len(num[keys[0]])), {keys[0]: num[keys[0]]}, 'Index', keys[0]");
			} else {
				b.add("xs, xl = xv, " + str(d.x()) + "  # each Y over X");
				b.add("ys = {k: v[:len(xs)] for k, v in num.items()}");
				b.add("yl = keys[0] if len(keys) == 1 else 'Value'");
			}
			String sz = o.modern() ? "45" : "20";
			b.add("if g is not None and len(ys) == 1:  # the points colored by the groups");
			b.add("    y = next(iter(ys.values()))");
			b.add("    gg = g[:len(y)]");
			b.add("    for i, level in enumerate(sorted(set(gg))):");
			b.add("        sel = gg == level");
			b.add("        ax.scatter(xs[:len(y)][sel], y[sel], s=" + sz + " * size, marker=marker, color=colors[i], alpha=alpha, label=level, **points)");
			b.add("    if " + (o.legend().equals("none") ? "False" : "True") + ":");
			b.add("        ax.legend(fontsize='small', title=" + str(d.group()) + ")");
			b.add("else:");
			b.add("    for i, (k, y) in enumerate(ys.items()):");
			b.add("        ax.scatter(xs[:len(y)], y, s=" + sz + " * size, marker=marker, color=colors[i], alpha=alpha, label=k, **points)");
			b.add("    if len(ys) > 1 and " + (o.legend().equals("none") ? "False" : "True") + ":");
			b.add("        ax.legend(fontsize='small')");
		}
		case "hist" -> {
			b.add("bins = np.histogram_bin_edges(np.concatenate(list(clean.values())), bins=" + o.bins() + ")");
			b.add("for i, k in enumerate(keys):");
			b.add("    ax.hist(clean[k], bins=bins, color=colors[i], alpha=alpha if len(keys) == 1 else min(alpha, 0.6), edgecolor='white', label=k)");
			b.add("xl, yl = 'Value', 'Frequency'");
		}
		case "density" -> {
			b.add("for i, k in enumerate(keys):");
			b.add("    v = clean[k]");
			b.add("    grid = np.linspace(v.min() - 0.1 * np.ptp(v), v.max() + 0.1 * np.ptp(v), 256)");
			b.add("    try:");
			b.add("        from scipy.stats import gaussian_kde");
			b.add("        dens = gaussian_kde(v)(grid)");
			b.add("    except ImportError:  # a normal kernel with numpy (bandwidth of Silverman)");
			b.add("        h = 1.06 * v.std() * len(v) ** -0.2 or 1.0");
			b.add("        dens = np.exp(-0.5 * ((grid[:, None] - v[None, :]) / h) ** 2).sum(axis=1) / (len(v) * h * np.sqrt(2 * np.pi))");
			b.add("    ax.fill_between(grid, dens, color=colors[i], alpha=0.25 * alpha, lw=0)");
			b.add("    ax.plot(grid, dens, color=colors[i], lw=lw, ls=ls, label=k)");
			b.add("xl, yl = 'Value', 'Density'");
		}
		case "ecdf" -> {
			b.add("for i, k in enumerate(keys):");
			b.add("    v = np.sort(clean[k])");
			b.add("    ax.step(np.concatenate([[v[0]], v]), np.arange(len(v) + 1) / len(v), where='post', color=colors[i], lw=lw, ls=ls, alpha=alpha, label=k)");
			b.add("xl, yl = 'Value', 'Cumulative proportion'");
		}
		case "box" -> {
			b.add("data, vert = [clean[k] for k in keys], " + (o.horizontal() ? "False" : "True"));
			if (o.modern()) {
				b.add("ax.grid(False, axis='x' if vert else 'y')");
				b.add("bp = ax.boxplot(data, vert=vert, patch_artist=True, widths=0.45, showfliers=" + (o.observations() ? "False" : "True") + ")");
				b.add("for i, patch in enumerate(bp['boxes']):");
				b.add("    patch.set_facecolor(colors[i])");
				b.add("    patch.set_alpha(0.25 * alpha)");
				b.add("    patch.set_edgecolor(colors[i])");
				b.add("for part, per in (('whiskers', 2), ('caps', 2), ('medians', 1)):");
				b.add("    for j, line in enumerate(bp[part]):");
				b.add("        line.set_color(colors[j // per])");
				b.add("        line.set_linewidth(max(1.0, lw / 1.5))");
			} else {
				b.add("bp = ax.boxplot(data, vert=vert, patch_artist=True, widths=0.6, showfliers=" + (o.observations() ? "False" : "True") + ")");
				b.add("for patch, c in zip(bp['boxes'], colors):");
				b.add("    patch.set_facecolor(c)");
				b.add("    patch.set_alpha(alpha)");
				b.add("for line in bp['medians']:");
				b.add("    line.set_color('black')");
			}
			if (o.observations()) {
				observations(b, o, o.horizontal());
			}
			b.add("(ax.set_xticks if vert else ax.set_yticks)(range(1, len(keys) + 1))");
			b.add("(ax.set_xticklabels if vert else ax.set_yticklabels)(keys)");
		}
		case "violin" -> {
			b.add("data = [clean[k] for k in keys]");
			b.add("ax.grid(False, axis='x')");
			b.add("parts = ax.violinplot(data, showextrema=False, widths=0.8)");
			b.add("for i, body in enumerate(parts['bodies']):");
			b.add("    body.set_facecolor(colors[i])");
			b.add("    body.set_edgecolor(colors[i])");
			b.add("    body.set_alpha(0.35 * alpha)");
			b.add("for i, v in enumerate(data, start=1):  # the quartiles and the median");
			b.add("    q1, med, q3 = np.percentile(v, [25, 50, 75])");
			b.add("    ax.vlines(i, q1, q3, color='#404040', lw=5)");
			b.add("    ax.scatter([i], [med], s=40, color='white', edgecolors='#404040', zorder=4)");
			if (o.observations()) {
				observations(b, o, false);
			}
			b.add("ax.set_xticks(range(1, len(keys) + 1))");
			b.add("ax.set_xticklabels(keys)");
			b.add("yl = 'Value'");
		}
		case "strip" -> {
			b.add("ax.grid(False, axis='x')");
			b.add("rng = np.random.default_rng(42)");
			b.add("for i, k in enumerate(keys, start=1):");
			b.add("    v = clean[k]");
			b.add("    if len(v) > 2000:");
			b.add("        v = rng.choice(v, 2000, replace=False)");
			b.add("    ax.scatter(i + rng.uniform(-0.15, 0.15, len(v)), v, s=" + (o.modern() ? "30" : "14") + " * size, marker=marker, color=colors[i - 1], alpha=alpha, **points)");
			b.add("    ax.hlines(v.mean(), i - 0.25, i + 0.25, color='#333333', lw=2, zorder=4)  # the mean");
			b.add("ax.set_xticks(range(1, len(keys) + 1))");
			b.add("ax.set_xticklabels(keys)");
			b.add("yl = 'Value'");
		}
		case "bar" -> {
			b.add("if g is not None and keys:  # the mean of Y in each group");
			b.add("    v = num[keys[0]]");
			b.add("    ticks = sorted(set(g))");
			b.add("    values, xl, yl, many = [v[g[:len(v)] == t].mean() for t in ticks], " + str(d.group()) + ", 'Mean of ' + keys[0], True");
			b.add("elif g is not None:  # the counts of the groups");
			b.add("    ticks, values = np.unique(g, return_counts=True)");
			b.add("    ticks, xl, yl, many = list(ticks), " + str(d.group()) + ", 'Count', True");
			b.add("elif len(objs) == 1 and cat and not num:  # categories: the counts");
			b.add("    values, ticks = np.unique(next(iter(cat.values())), return_counts=True)[::-1]");
			b.add("    ticks, xl, yl, many = list(ticks), keys[0] if keys else list(cat)[0], 'Count', " + (o.modern() ? "True" : "False"));
			b.add("elif len(keys) == 1 and len(num[keys[0]]) <= 100:");
			b.add("    values, ticks, xl, yl, many = num[keys[0]], [str(i) for i in range(len(num[keys[0]]))], 'Index', 'Value', False");
			b.add("else:");
			b.add("    values, ticks, yl, many = [clean[k].mean() for k in keys], keys, 'Mean', True");
			b.add("pos = np.arange(len(values))");
			b.add("bar_colors = colors[:len(values)] if many else colors[0]");
			b.add("style = " + (o.modern() ? "dict(edgecolor='white', linewidth=1.0)" : "dict(edgecolor='black', linewidth=0.6)"));
			if (o.horizontal()) {
				b.add("ax.grid(False, axis='y')");
				b.add("bars = ax.barh(pos, values, color=bar_colors, alpha=alpha, height=" + (o.modern() ? "0.74" : "0.8") + ", **style)");
				b.add("ax.set_yticks(pos)");
				b.add("ax.set_yticklabels(ticks)");
				b.add("xl, yl = yl, xl");
			} else {
				b.add("ax.grid(False, axis='x')");
				b.add("bars = ax.bar(pos, values, color=bar_colors, alpha=alpha, width=" + (o.modern() ? "0.74" : "0.8") + ", **style)");
				b.add("ax.set_xticks(pos)");
				b.add("ax.set_xticklabels(ticks, rotation=45 if len(values) > 8 else 0, ha='right' if len(values) > 8 else 'center')");
			}
			if (o.valueLabels()) {
				b.add("if len(values) <= 30:");
				b.add("    ax.bar_label(bars, labels=['%.3g' % v for v in values], padding=3, fontweight='bold', fontsize='small', color='#333333')");
				b.add("    ax.margins(" + (o.horizontal() ? "x" : "y") + "=0.12)");
			}
		}
		case "pie" -> {
			b.add("if g is not None and keys:  # the sum of Y in each group");
			b.add("    v = num[keys[0]]");
			b.add("    labels = sorted(set(g))");
			b.add("    values = [np.nansum(v[g[:len(v)] == t]) for t in labels]");
			b.add("elif g is not None:");
			b.add("    labels, values = np.unique(g, return_counts=True)");
			b.add("elif len(objs) == 1 and cat and not num:");
			b.add("    labels, values = np.unique(next(iter(cat.values())), return_counts=True)");
			b.add("elif len(keys) == 1:");
			b.add("    values = num[keys[0]]");
			b.add("    labels = [str(i) for i in range(len(values))]");
			b.add("else:");
			b.add("    values, labels = [clean[k].mean() for k in keys], keys");
			b.add("if np.any(np.asarray(values) < 0):");
			b.add("    raise ValueError('A pie needs values >= 0')");
			b.add("ax.pie(values, labels=labels, colors=colors[:len(values)], startangle=90, counterclock=False, wedgeprops=dict(edgecolor='white', alpha=alpha)"
					+ (o.valueLabels() ? ", autopct='%.1f %%'" : "") + ")");
			b.add("ax.axis('equal')");
		}
		case "pairs" -> {
			b.add("sel = keys[:6]");
			b.add("if len(sel) < 2:");
			b.add("    raise ValueError('A scatter matrix needs at least two numeric variables (columns)')");
			b.add("m = min(len(num[k]) for k in sel)");
			b.add("fig, axes = plt.subplots(len(sel), len(sel), figsize=(2.2 * len(sel), 2.2 * len(sel)), squeeze=False)");
			b.add("for i, ki in enumerate(sel):");
			b.add("    for j, kj in enumerate(sel):");
			b.add("        a = axes[i][j]");
			b.add("        if i == j:");
			b.add("            a.hist(clean[ki], bins=" + o.bins() + ", color=colors[0], alpha=alpha, edgecolor='white')");
			b.add("        else:");
			b.add("            a.scatter(num[kj][:m], num[ki][:m], s=" + (o.modern() ? "14" : "6") + " * size, marker=marker, color=colors[0], alpha=alpha, **(dict(edgecolors='white', linewidths=0.5) if points else {}))");
			b.add("        if i == len(sel) - 1:");
			b.add("            a.set_xlabel(kj, fontsize='small')");
			b.add("        if j == 0:");
			b.add("            a.set_ylabel(ki, fontsize='small')");
			b.add("        a.tick_params(labelsize='small')");
			figTitles(b, o);
		}
		case "corr" -> {
			b.add("sel = keys[:12]");
			b.add("if len(sel) < 2:");
			b.add("    raise ValueError('A correlation matrix needs at least two numeric variables (columns)')");
			b.add("m = min(len(num[k]) for k in sel)");
			b.add("C = np.corrcoef(np.vstack([num[k][:m] for k in sel]))");
			b.add("ax.grid(False)");
			b.add("im = ax.imshow(C, cmap='RdBu_r', vmin=-1, vmax=1)");
			b.add("fig.colorbar(im, ax=ax, shrink=0.8)");
			b.add("ax.set_xticks(range(len(sel)))");
			b.add("ax.set_xticklabels(sel, rotation=45, ha='right')");
			b.add("ax.set_yticks(range(len(sel)))");
			b.add("ax.set_yticklabels(sel)");
			if (o.valueLabels()) {
				b.add("for (i, j), r in np.ndenumerate(C):");
				b.add("    ax.text(j, i, '%.2f' % r, ha='center', va='center', fontsize='small', color='white' if abs(r) > 0.6 else 'black')");
			}
		}
		case "heatmap", "contour", "surface" -> {
			b.add("if z is not None:  # the matrix (numeric columns of a DataFrame)");
			b.add("    data = z.select_dtypes('number') if hasattr(z, 'select_dtypes') else z");
			b.add("    matrix = np.asarray(data, dtype=float)");
			b.add("    columns = [str(c) for c in getattr(data, 'columns', range(matrix.shape[1]))]");
			b.add("elif table is not None:");
			b.add("    name, data = table");
			b.add("    matrix = np.asarray(data, dtype=float)");
			b.add("    columns = [str(c) for c in getattr(data, 'columns', range(matrix.shape[1]))]");
			b.add("else:");
			b.add("    matrix = np.column_stack([num[k][:min(len(num[x]) for x in keys)] for k in keys])");
			b.add("    columns = keys");
			if (kind.equals("heatmap")) {
				b.add("ax.grid(False)");
				b.add("im = ax.imshow(matrix, aspect='auto', cmap='viridis', interpolation='nearest')");
				b.add("fig.colorbar(im, ax=ax)");
				b.add("if len(columns) <= 30:");
				b.add("    ax.set_xticks(range(len(columns)))");
				b.add("    ax.set_xticklabels(columns, rotation=45, ha='right')");
				b.add("xl, yl = 'Column', 'Row'");
			} else if (kind.equals("contour")) {
				b.add("if min(matrix.shape) < 2:");
				b.add("    raise ValueError('A contour plot needs a matrix (or DataFrame) with at least 2 rows and 2 columns')");
				b.add("ax.grid(False)");
				b.add("rows = xv if xv is not None and len(xv) == matrix.shape[0] else np.arange(matrix.shape[0])  # the values of the axes");
				b.add("cols_ = np.asarray(yaxis, dtype=float) if yaxis is not None and len(yaxis) == matrix.shape[1] else np.arange(matrix.shape[1])");
				b.add("cs = ax.contourf(rows, cols_, matrix.T, levels=20, cmap='viridis', alpha=alpha)");
				b.add("lines = ax.contour(rows, cols_, matrix.T, levels=10, colors='white', linewidths=max(0.5, lw / 2))");
				b.add("ax.clabel(lines, fontsize='x-small')");
				b.add("fig.colorbar(cs, ax=ax)");
				b.add("xl, yl = " + (d.x().isEmpty() ? "'Row'" : str(d.x())) + ", " + (matrixZ && !d.ys().isEmpty() ? str(d.ys().get(0)) : "'Column'"));
			} else {
				b.add("if min(matrix.shape) < 2:");
				b.add("    raise ValueError('A surface needs a matrix (or DataFrame) with at least 2 rows and 2 columns')");
				b.add("fig = plt.figure()");
				b.add("ax = fig.add_subplot(projection='3d')");
				b.add("rows = xv if xv is not None and len(xv) == matrix.shape[0] else np.arange(matrix.shape[0])  # the values of the axes");
				b.add("cols_ = np.asarray(yaxis, dtype=float) if yaxis is not None and len(yaxis) == matrix.shape[1] else np.arange(matrix.shape[1])");
				b.add("X, Y = np.meshgrid(rows, cols_, indexing='ij')");
				b.add("ax.plot_surface(X, Y, matrix, cmap='viridis', edgecolor='white', linewidth=0.2, alpha=alpha)");
				b.add("ax.view_init(elev=25, azim=-55)");
				b.add("ax.set_xlabel(" + lab(o.xLabel(), d.x().isEmpty() ? "'Row'" : str(d.x())) + ")");
				b.add("ax.set_ylabel(" + lab(o.yLabel(), matrixZ && !d.ys().isEmpty() ? str(d.ys().get(0)) : "'Column'") + ")");
				b.add("ax.set_zlabel('Value')");
				figTitles(b, o);
			}
		}
		default -> { // qq
			b.add("from statistics import NormalDist");
			b.add("t1, t3 = NormalDist().inv_cdf(0.25), NormalDist().inv_cdf(0.75)");
			b.add("for i, k in enumerate(keys):");
			b.add("    v = np.sort(clean[k])");
			b.add("    q = np.array([NormalDist().inv_cdf((j - 0.5) / len(v)) for j in range(1, len(v) + 1)])  # the normal quantiles");
			b.add("    q1, q3 = np.percentile(v, [25, 75])");
			b.add("    ax.plot(q, q1 + (q3 - q1) / (t3 - t1) * (q - t1), color=colors[i] if len(keys) > 1 else '#666666', lw=lw, ls=ls)  # the line through the quartiles");
			b.add("    ax.scatter(q, v, s=" + (o.modern() ? "45" : "20") + " * size, marker=marker, color=colors[i], alpha=alpha, zorder=3, label=k, **points)");
			b.add("xl, yl = 'Theoretical quantiles', 'Sample quantiles'");
		}
		}
		if (own) {
			b.add("plt.show()");
			return b;
		}
		/* The titles, labels, limits, scale and legend */
		b.add("");
		b.add("# The titles and axes");
		if (o.modern()) {
			b.add("ax.set_title(title, loc='left', fontweight='bold', color='#1A1A1A', pad=24 if subtitle else 10)");
			b.add("if subtitle:");
			b.add("    ax.text(0, 1.02, subtitle, transform=ax.transAxes, ha='left', va='bottom', fontsize='medium', color='#666666')");
		} else {
			b.add("ax.set_title(title + ('\\n' + subtitle if subtitle else ''))");
		}
		b.add("if caption:");
		b.add("    fig.text(0.99, 0.005, caption, ha='right', va='bottom', fontsize='small', color='#808080')");
		if (!kind.equals("pie")) {
			b.add("ax.set_xlabel(" + lab(o.xLabel(), "xl") + ")");
			b.add("ax.set_ylabel(" + lab(o.yLabel(), "yl") + ")");
			b.add("if xlim:");
			b.add("    ax.set_xlim(xlim)");
			b.add("if ylim:");
			b.add("    ax.set_ylim(ylim)");
		}
		if (o.logY() && !kind.equals("pie") && !kind.equals("corr") && !kind.equals("heatmap") && !kind.equals("contour")) {
			b.add(o.horizontal() && (kind.equals("box") || kind.equals("bar")) ? "ax.set_xscale('log')" : "ax.set_yscale('log')");
		}
		if (!o.legend().equals("none") && kind.matches("line|area|hist|density|ecdf|qq")) {
			String pos = switch (o.legend()) {
			case "topleft" -> "upper left";
			case "topright" -> "upper right";
			case "bottomleft" -> "lower left";
			case "bottomright" -> "lower right";
			default -> kind.equals("ecdf") ? "lower right" : kind.equals("line") || kind.equals("area") ? "upper left" : "best";
			};
			b.add("if len(keys) > 1:");
			b.add("    ax.legend(loc=" + str(pos) + ", fontsize='small'" + (o.modern() && (kind.equals("line") || kind.equals("area")) && pos.startsWith("upper")
					? ", ncol=len(keys) if len(keys) <= 4 else 1"
					: "") + ")");
		}
		b.add("fig.tight_layout(rect=(0, 0.03, 1, 1) if caption else (0, 0, 1, 1))");
		b.add("plt.show()");
		return b;
	}

	/* The observations of box and violin plots (jittered, at most 300 of each variable) */
	private static void observations(List<String> b, PlotOptions o, boolean horizontal) {
		b.add("rng = np.random.default_rng(42)");
		b.add("for i, v in enumerate([clean[k] for k in keys], start=1):  # the observations (at most 300)");
		b.add("    if len(v) > 300:");
		b.add("        v = rng.choice(v, 300, replace=False)");
		b.add("    j = i + rng.uniform(-0.12, 0.12, len(v))");
		String args = "s=" + (o.modern() ? "22" : "10") + " * size, marker=marker, color=colors[i - 1], zorder=3" + (o.modern() ? ", edgecolors='white', linewidths=0.8" : "");
		b.add(horizontal ? "    ax.scatter(v, j, " + args + ")" : "    ax.scatter(j, v, " + args + ")");
	}

	/* The titles of a figure (scatter matrix, surface) */
	private static void figTitles(List<String> b, PlotOptions o) {
		if (o.modern()) {
			b.add("fig.suptitle(title + ('\\n' + subtitle if subtitle else ''), x=0.02, ha='left', fontweight='bold', color='#1A1A1A')");
		} else {
			b.add("fig.suptitle(title + ('\\n' + subtitle if subtitle else ''))");
		}
		b.add("if caption:");
		b.add("    fig.text(0.99, 0.005, caption, ha='right', va='bottom', fontsize='small', color='#808080')");
		b.add("fig.tight_layout()");
	}

	private static String autoTitle(String kind, PlotData d) {
		String vars = d.describe();
		return switch (kind) {
		case "hist" -> "Histogram of " + vars;
		case "density" -> "Density of " + vars;
		case "ecdf" -> "Cumulative distribution of " + vars;
		case "qq" -> "Normal Q-Q plot of " + vars;
		case "pairs" -> "Scatter matrix of " + vars;
		case "corr" -> "Correlations of " + vars;
		default -> vars;
		};
	}

	private static String lab(String label, String auto) {
		return label.isEmpty() ? auto : str(label);
	}

	private static String lim(String min, String max) {
		Double a = PlotOptions.limit(min), b = PlotOptions.limit(max);
		return a != null && b != null ? "(" + n(a) + ", " + n(b) + ")" : "None";
	}

	private static String lineStyle(PlotOptions o) {
		return switch (o.lineStyle()) {
		case "dashed" -> "--";
		case "dotted" -> ":";
		default -> "-";
		};
	}

	private static String marker(PlotOptions o) {
		return switch (o.pointShape()) {
		case "square" -> "s";
		case "triangle" -> "^";
		case "diamond" -> "D";
		default -> "o";
		};
	}

	/* A Python string literal */
	static String str(String s) {
		StringBuilder b = new StringBuilder("'");
		for (char c : s.toCharArray()) {
			switch (c) {
			case '\'' -> b.append("\\'");
			case '\\' -> b.append("\\\\");
			case '\n' -> b.append("\\n");
			default -> b.append(c);
			}
		}
		return b.append('\'').toString();
	}

	private static String n(double d) {
		return PlotOptions.num(d);
	}
}
