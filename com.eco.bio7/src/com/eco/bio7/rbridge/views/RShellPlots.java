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
package com.eco.bio7.rbridge.views;

import java.util.ArrayList;
import java.util.List;

import com.eco.bio7.plot.PlotData;
import com.eco.bio7.plot.PlotOptions;

/**
 * The R source of the plots of the context menu of the R-Shell (the graphics
 * of R, no packages): a readable script for the plot type (the source can be
 * copied into an R editor for a customization). The selected variables
 * (vectors, factors, matrices and the columns of data frames) with the
 * attributes of PlotOptions; the modern style: a left aligned bold title with
 * subtitle and caption, a light grid behind the data, no box, points with a
 * white border.
 */
public final class RShellPlots {

	private RShellPlots() {
	}

	/** The R source which plots the variables (the values). */
	public static String code(String[] names, PlotOptions o) {
		return code(PlotData.of(List.of(names)), o);
	}

	/** The R source which plots the data (the roles of the plot type). */
	public static String code(PlotData d, PlotOptions o) {
		boolean modern = o.modern();
		String kind = o.kind();
		boolean matrixZ = PlotData.usesZ(kind) && !d.z().isEmpty();
		/* With a matrix Z the first Y gives the values of the y axis */
		String[] names = matrixZ ? new String[0] : d.ys().toArray(String[]::new);
		List<String> r = new ArrayList<>();
		r.add("# " + PlotOptions.label(kind) + " of " + d.describe() + " (Bio7 R-Shell): customize and run it in R");
		r.add("local({");
		r.add("  # The data: vectors, factors, the columns of matrices and data frames");
		StringBuilder objs = new StringBuilder("  objs <- list(");
		for (int i = 0; i < names.length; i++) {
			objs.append(i > 0 ? ", " : "").append('`').append(names[i].replace("`", "")).append("` = ").append(names[i]);
		}
		r.add(objs.append(")").toString() + "  # Y");
		r.add("  xv <- " + (d.x().isEmpty() ? "NULL" : d.x()) + "  # X");
		r.add("  g <- " + (d.group().isEmpty() ? "NULL" : "factor(" + d.group() + ")") + "  # the groups");
		if (PlotData.usesZ(kind)) {
			r.add("  z <- " + (d.z().isEmpty() ? "NULL" : d.z()) + "  # the matrix");
			r.add("  ya <- " + (matrixZ && !d.ys().isEmpty() ? d.ys().get(0) : "NULL") + "  # the values of the y axis");
		}
		r.add("  s <- list()");
		r.add("  for (n in names(objs)) {");
		r.add("    x <- objs[[n]]");
		r.add("    if (is.data.frame(x)) { for (cn in names(x)) if (is.numeric(x[[cn]]) || is.factor(x[[cn]])) s[[paste0(n, '$', cn)]] <- x[[cn]] }");
		r.add("    else if (is.matrix(x)) { for (j in seq_len(ncol(x))) s[[if (is.null(colnames(x))) paste0(n, '[,', j, ']') else colnames(x)[j]]] <- x[, j] }");
		r.add("    else s[[n]] <- x");
		r.add("  }");
		r.add("  num <- Filter(is.numeric, s)");
		if (kind.matches("hist|density|ecdf|box|violin|strip|qq")) {
			/* The values of each variable split into the groups */
			r.add("  if (!is.null(g)) num <- unlist(lapply(names(num), function(n) {");
			r.add("    v <- num[[n]]; if (length(v) != length(g)) stop(paste('The group has another length than', n))");
			r.add("    sp <- split(v, g); names(sp) <- if (length(num) > 1) paste(n, names(sp), sep = ': ') else names(sp); sp");
			r.add("  }), recursive = FALSE)");
		}
		if (!kind.equals("bar") && !kind.equals("pie") && !matrixZ) {
			r.add("  if (length(num) == 0) stop('No numeric data to plot')");
		}
		r.add("");
		r.add("  # The attributes");
		StringBuilder cols = new StringBuilder("  cols <- c(");
		String[] pc = o.plotColors();
		for (int i = 0; i < pc.length; i++) {
			cols.append(i > 0 ? ", " : "").append(str(pc[i]));
		}
		r.add(cols.append(")").toString());
		r.add("  cols <- rep_len(cols, max(length(cols), length(s)))");
		r.add("  alpha <- " + n(o.opacity()) + "; fill <- grDevices::adjustcolor(cols, alpha.f = alpha)");
		r.add("  lwd <- " + n(o.lineWidth()) + "; lty <- " + str(o.lineStyle()) + "; pch <- " + pch(o) + "; cex <- " + n(o.pointSize()));
		r.add("  xlim <- " + lim(o.xMin(), o.xMax()) + "; ylim <- " + lim(o.yMin(), o.yMax()) + "  # e.g. c(0, 10)");
		r.add("  title <- " + str(o.title().isEmpty() ? autoTitle(kind, d) : o.title()) + "; subtitle <- " + str(o.subtitle()) + "; caption <- "
				+ str(o.caption()));
		r.add("");
		r.add("  # The style");
		if (modern) {
			r.add("  op <- par(ps = " + n(o.fontSize()) + ", mar = c(5, 4.5, " + (o.subtitle().isEmpty() ? "3.5" : "4.5")
					+ ", 2), las = 1, bty = 'n', fg = 'gray30', col.axis = 'gray30', col.lab = 'gray20', font.lab = 2)");
		} else {
			r.add("  op <- par(ps = " + n(o.fontSize()) + ", cex.main = " + n(o.titleSize() / o.fontSize()) + ")");
		}
		r.add("  on.exit(par(op))");
		r.add("");
		r.add("  # The plot");
		r.addAll(plot(o, d));
		r.add("");
		titles(r, o);
		r.add("})");
		return String.join("\n", r) + "\n";
	}

	/* ---------------- The plot types ---------------- */

	private static List<String> plot(PlotOptions o, PlotData d) {
		List<String> r = new ArrayList<>();
		String main = o.modern() ? "''" : "title";
		String log = o.logY() ? "'y'" : "''";
		String grid = o.modern() ? "grid(col = 'gray90', lty = 'solid', lwd = 0.8)" : o.grid() ? "grid()" : null;
		String yGrid = o.modern() ? "grid(nx = NA, ny = NULL, col = 'gray90', lty = 'solid', lwd = 0.8)" : o.grid() ? "grid(nx = NA, ny = NULL)" : null;
		String xGrid = o.modern() ? "grid(nx = NULL, ny = NA, col = 'gray90', lty = 'solid', lwd = 0.8)" : o.grid() ? "grid(nx = NULL, ny = NA)" : null;
		switch (o.kind()) {
		case "line", "area" -> {
			boolean area = o.kind().equals("area");
			r.add("  len <- max(lengths(num)); r <- range(" + (area ? "c(0, unlist(num))" : "unlist(num)") + ", na.rm = TRUE)");
			r.add("  xs <- function(v) if (is.null(xv)) seq_along(v) else xv[seq_along(v)]  # the x values of a line");
			frame(r, main, "if (is.null(xv)) c(1, len) else range(xv, na.rm = TRUE)", "r", log, lab(o.xLabel(), d.x().isEmpty() ? "'Index'" : str(d.x())),
					lab(o.yLabel(), "'Value'"), grid);
			r.add("  for (i in seq_along(num)) {");
			r.add("    v <- num[[i]]; x <- xs(v)");
			if (area) {
				r.add("    polygon(c(x[1], x, x[length(x)]), c(0, v, 0), col = grDevices::adjustcolor(cols[i], alpha.f = 0.35 * alpha), border = NA)");
			}
			r.add("    lines(x, v, col = fill[i], lwd = lwd, lty = lty)");
			r.add("  }");
			legend(r, o, "topleft", "names(num), col = fill[seq_along(num)], lwd = lwd, lty = lty");
		}
		case "scatter" -> {
			if (d.x().isEmpty()) {
				r.add("  if (length(num) >= 2) { m <- min(length(num[[1]]), length(num[[2]])); x <- num[[1]][1:m]; y <- num[[2]][1:m] } else { y <- num[[1]]; x <- seq_along(y) }");
				frame(r, main, "range(x, na.rm = TRUE)", "range(y, na.rm = TRUE)", log, lab(o.xLabel(), "if (length(num) >= 2) names(num)[1] else 'Index'"),
						lab(o.yLabel(), "names(num)[min(2, length(num))]"), grid);
				r.add("  ys <- list(y)");
			} else {
				/* Each Y over X */
				r.add("  x <- as.numeric(xv); ys <- lapply(num, function(v) v[seq_len(min(length(v), length(x)))])");
				frame(r, main, "range(x, na.rm = TRUE)", "range(unlist(ys), na.rm = TRUE)", log, lab(o.xLabel(), str(d.x())),
						lab(o.yLabel(), "if (length(num) == 1) names(num)[1] else 'Value'"), grid);
			}
			r.add("  if (!is.null(g) && length(ys) == 1) {");
			r.add("    # The points colored by the groups");
			r.add("    k <- as.integer(g)[seq_along(ys[[1]])]; pc <- rep_len(fill, nlevels(g))");
			r.add("    " + points(o, "x[seq_along(ys[[1]])]", "ys[[1]]", "1").replace("fill[1]", "pc[k]"));
			legendAlways(r, o, "topright", "levels(g), pch = pch, " + (o.modern() ? "pt.bg = pc, col = 'white', pt.cex = 1.2" : "col = pc"));
			r.add("  } else {");
			r.add("    for (i in seq_along(ys)) " + points(o, "x[seq_along(ys[[i]])]", "ys[[i]]", "i"));
			r.add("    if (length(ys) > 1) " + legendCall(o, "topright", "names(num), pch = pch, " + (o.modern() ? "pt.bg = fill[seq_along(num)], col = 'white', pt.cex = 1.2" : "col = fill[seq_along(num)]")));
			r.add("  }");
		}
		case "hist" -> {
			r.add("  br <- pretty(range(unlist(num), na.rm = TRUE), " + o.bins() + ")");
			r.add("  ymax <- max(sapply(num, function(v) max(hist(v, breaks = br, plot = FALSE)$counts)))");
			frame(r, main, "range(br)", "c(0, ymax)", "''", lab(o.xLabel(), "'Value'"), lab(o.yLabel(), "'Frequency'"), grid);
			r.add("  for (i in seq_along(num)) hist(num[[i]], breaks = br, col = if (length(num) > 1) grDevices::adjustcolor(cols[i], alpha.f = min(alpha, 0.6)) else fill[i], border = 'white', add = TRUE)");
			legend(r, o, "topright", "names(num), fill = fill[seq_along(num)], border = 'white'");
		}
		case "density" -> {
			r.add("  d <- lapply(num, function(v) stats::density(v[is.finite(v)]))");
			frame(r, main, "range(sapply(d, function(z) range(z$x)))", "c(0, max(sapply(d, function(z) max(z$y))))", "''", lab(o.xLabel(), "'Value'"),
					lab(o.yLabel(), "'Density'"), grid);
			r.add("  for (i in seq_along(d)) {");
			r.add("    polygon(d[[i]], col = grDevices::adjustcolor(cols[i], alpha.f = 0.25 * alpha), border = NA)");
			r.add("    lines(d[[i]], col = fill[i], lwd = lwd, lty = lty)");
			r.add("  }");
			legend(r, o, "topright", "names(num), col = fill[seq_along(num)], lwd = lwd, lty = lty");
		}
		case "ecdf" -> {
			frame(r, main, "range(unlist(num), na.rm = TRUE)", "c(0, 1)", "''", lab(o.xLabel(), "'Value'"), lab(o.yLabel(), "'Cumulative proportion'"), grid);
			r.add("  for (i in seq_along(num)) {");
			r.add("    v <- sort(num[[i]][is.finite(num[[i]])])");
			r.add("    lines(stats::stepfun(v, seq(0, 1, length.out = length(v) + 1)), col = fill[i], lwd = lwd, lty = lty, do.points = FALSE, verticals = TRUE)");
			r.add("  }");
			legend(r, o, "bottomright", "names(num), col = fill[seq_along(num)], lwd = lwd, lty = lty");
		}
		case "box" -> {
			String lg = o.logY() ? (o.horizontal() ? "'x'" : "'y'") : "''";
			String hz = bool(o.horizontal());
			String lim = o.horizontal() ? "xlim" : "ylim";
			if (o.modern()) {
				r.add("  boxplot(num, horizontal = " + hz + ", log = " + lg + ", border = NA, col = NA, outline = FALSE, main = '', xlab = " + lab(o.xLabel(), "''")
						+ ", ylab = " + lab(o.yLabel(), "''") + ", " + lim + " = " + lim + ")");
				r.add("  " + (o.horizontal() ? xGrid : yGrid));
				r.add("  boxplot(num, horizontal = " + hz + ", log = " + lg + ", add = TRUE, col = grDevices::adjustcolor(cols[seq_along(num)], alpha.f = 0.25 * alpha), border = cols[seq_along(num)], boxwex = 0.45, outline = "
						+ bool(!o.observations()) + ", lwd = max(1, lwd / 1.5), axes = FALSE)");
			} else {
				r.add("  boxplot(num, horizontal = " + hz + ", log = " + lg + ", col = fill[seq_along(num)], lwd = lwd, outline = " + bool(!o.observations())
						+ ", main = title, xlab = " + lab(o.xLabel(), "''") + ", ylab = " + lab(o.yLabel(), "''") + ", las = " + (o.horizontal() ? "1" : "0") + ", "
						+ lim + " = " + lim + ")");
				if (o.grid()) {
					r.add("  " + (o.horizontal() ? xGrid : yGrid));
				}
			}
			if (o.observations()) {
				observations(r, o, o.horizontal());
			}
		}
		case "violin" -> {
			r.add("  k <- length(num); r <- range(unlist(num), na.rm = TRUE)");
			r.add("  plot(NA, xlim = c(0.5, k + 0.5), ylim = if (is.null(ylim)) r else ylim, log = " + log + ", xaxt = 'n', main = " + main + ", xlab = "
					+ lab(o.xLabel(), "''") + ", ylab = " + lab(o.yLabel(), "'Value'") + ")");
			r.add("  axis(1, at = seq_len(k), labels = names(num))");
			if (yGrid != null) {
				r.add("  " + yGrid);
			}
			r.add("  for (i in seq_len(k)) {");
			r.add("    v <- num[[i]][is.finite(num[[i]])]; d <- stats::density(v); w <- 0.4 * d$y / max(d$y)");
			r.add("    polygon(c(i - w, rev(i + w)), c(d$x, rev(d$x)), col = grDevices::adjustcolor(cols[i], alpha.f = 0.35 * alpha), border = cols[i], lwd = max(1, lwd / 2))");
			r.add("    q <- stats::quantile(v, c(0.25, 0.5, 0.75))");
			r.add("    rect(i - 0.04, q[1], i + 0.04, q[3], col = 'gray25', border = NA)");
			r.add("    points(i, q[2], pch = 21, bg = 'white', col = 'gray25', cex = 1.1)");
			r.add("  }");
			if (o.observations()) {
				observations(r, o, false);
			}
		}
		case "strip" -> {
			r.add("  k <- length(num); r <- range(unlist(num), na.rm = TRUE)");
			r.add("  plot(NA, xlim = c(0.5, k + 0.5), ylim = if (is.null(ylim)) r else ylim, log = " + log + ", xaxt = 'n', main = " + main + ", xlab = "
					+ lab(o.xLabel(), "''") + ", ylab = " + lab(o.yLabel(), "'Value'") + ")");
			r.add("  axis(1, at = seq_len(k), labels = names(num))");
			if (yGrid != null) {
				r.add("  " + yGrid);
			}
			r.add("  set.seed(42)");
			r.add("  for (i in seq_len(k)) {");
			r.add("    v <- num[[i]][is.finite(num[[i]])]; if (length(v) > 2000) v <- sample(v, 2000)");
			r.add("    j <- i + runif(length(v), -0.15, 0.15)");
			r.add("    " + points(o, "j", "v", "i"));
			r.add("    segments(i - 0.25, mean(v), i + 0.25, mean(v), col = 'gray20', lwd = 2)  # the mean");
			r.add("  }");
		}
		case "bar" -> {
			r.add("  x <- if (length(s) > 0) s[[1]] else NULL");
			r.add("  if (!is.null(g) && is.numeric(x)) { h <- tapply(x, g, mean, na.rm = TRUE); yl <- paste('Mean of', names(s)[1]); xl <- " + str(d.group()) + " }");
			r.add("  else if (!is.null(g)) { h <- table(g); yl <- 'Count'; xl <- " + str(d.group()) + " }");
			r.add("  else if (length(s) == 1 && (is.factor(x) || is.character(x) || is.logical(x))) { h <- table(x); yl <- 'Count'; xl <- names(s)[1] }");
			r.add("  else if (length(s) == 1 && length(x) <= 100) { h <- x; names(h) <- if (is.null(names(x))) seq_along(x) else names(x); yl <- 'Value'; xl <- 'Index' }");
			r.add("  else { h <- sapply(num, mean, na.rm = TRUE); yl <- 'Mean'; xl <- '' }");
			r.add("  bc <- if (length(s) > 1 || !is.null(g) || " + bool(o.modern()) + " && !is.numeric(x)) rep_len(fill, length(h)) else fill[1]");
			String lg = o.logY() ? (o.horizontal() ? "'x'" : "'y'") : "''";
			r.add("  lim <- c(" + (o.logY() ? "min(h[h > 0], na.rm = TRUE) / 2" : "min(0, h, na.rm = TRUE) * " + (o.valueLabels() ? "1.12" : "1.04")) + ", max(0, h, na.rm = TRUE) * " + (o.valueLabels() ? "1.12" : "1.04") + ")  # negative values: below 0");
			String hzLim = o.horizontal() ? "xlim = if (is.null(xlim)) lim else xlim" : "ylim = if (is.null(ylim)) lim else ylim";
			String sp = o.modern() ? "0.35" : "0.2";
			r.add("  bp <- barplot(h, horiz = " + bool(o.horizontal()) + ", log = " + lg + ", col = NA, border = NA, axes = FALSE, names.arg = FALSE, space = " + sp
					+ ", " + hzLim + ")");
			String g = o.horizontal() ? xGrid : yGrid;
			if (g != null) {
				r.add("  " + g);
			}
			r.add("  barplot(h, add = TRUE, horiz = " + bool(o.horizontal()) + ", log = " + lg + ", col = bc, border = " + (o.modern() ? "'white'" : "'black'")
					+ ", space = " + sp + ", " + hzLim + ", xlab = " + lab(o.xLabel(), o.horizontal() ? "yl" : "xl") + ", ylab = "
					+ lab(o.yLabel(), o.horizontal() ? "xl" : "yl") + ", las = " + (o.horizontal() || o.modern() ? "1" : "0") + ")");
			if (!o.modern()) {
				r.add("  title(main = title)  # barplot(add = TRUE) draws no title");
			}
			if (o.valueLabels()) {
				r.add("  lv <- formatC(as.numeric(h), digits = 3, format = 'g')");
				r.add(o.horizontal() ? "  text(as.numeric(h), bp, lv, pos = ifelse(h < 0, 2, 4), font = 2, cex = 0.8, col = 'gray20', xpd = TRUE)"
						: "  text(bp, as.numeric(h), lv, pos = ifelse(h < 0, 1, 3), font = 2, cex = 0.8, col = 'gray20', xpd = TRUE)");
			}
		}
		case "pie" -> {
			r.add("  x <- if (length(s) > 0) s[[1]] else NULL");
			r.add("  h <- if (!is.null(g) && is.numeric(x)) tapply(x, g, sum, na.rm = TRUE) else if (!is.null(g)) table(g) else if (length(s) == 1 && !is.numeric(x)) table(x) else if (length(s) == 1) x else sapply(num, mean, na.rm = TRUE)");
			r.add("  if (any(h < 0, na.rm = TRUE)) stop('A pie needs values >= 0')");
			r.add("  labs <- if (is.null(names(h))) seq_along(h) else names(h)");
			if (o.valueLabels()) {
				r.add("  labs <- paste0(labs, ' (', round(100 * h / sum(h), 1), ' %)')");
			}
			r.add("  pie(h, labels = labs, col = rep_len(fill, length(h)), border = 'white', clockwise = TRUE, init.angle = 90, main = " + main + ")");
		}
		case "pairs" -> {
			r.add("  m <- num[seq_len(min(6, length(num)))]");
			r.add("  if (length(m) < 2) stop('A scatter matrix needs at least two numeric variables (columns)')");
			r.add("  if (length(unique(lengths(m))) > 1) stop('The variables have different lengths')");
			r.add("  panel <- function(x, y, ...) {");
			if (grid != null) {
				r.add("    " + grid);
			}
			r.add("    " + points(o, "x", "y", "1").replace("cex = 1.3 * cex", "cex = 0.9 * cex"));
			r.add("  }");
			r.add("  pairs(as.data.frame(m), panel = panel, main = title, gap = " + (o.modern() ? "0.6" : "1") + ")");
		}
		case "corr" -> {
			r.add("  m <- num[seq_len(min(12, length(num)))]");
			r.add("  if (length(m) < 2) stop('A correlation matrix needs at least two numeric variables (columns)')");
			r.add("  if (length(unique(lengths(m))) > 1) stop('The variables have different lengths')");
			r.add("  C <- stats::cor(as.data.frame(m), use = 'pairwise.complete.obs'); k <- ncol(C)");
			r.add("  image(seq_len(k), seq_len(k), t(C)[, k:1, drop = FALSE], zlim = c(-1, 1), col = grDevices::hcl.colors(51, 'Blue-Red 3'), axes = FALSE, main = "
					+ main + ", xlab = " + lab(o.xLabel(), "''") + ", ylab = " + lab(o.yLabel(), "''") + ")");
			r.add("  axis(1, at = seq_len(k), labels = colnames(C), las = 2, cex.axis = 0.8)");
			r.add("  axis(2, at = seq_len(k), labels = rev(colnames(C)), las = 1, cex.axis = 0.8)");
			if (o.valueLabels()) {
				r.add("  text(rep(seq_len(k), each = k), rep(k:1, k), sprintf('%.2f', C), cex = 0.8, col = ifelse(abs(C) > 0.6, 'white', 'black'))");
			}
		}
		case "heatmap", "contour", "surface" -> {
			r.add("  x <- if (is.null(z)) objs[[1]] else z");
			r.add("  m <- if (is.data.frame(x)) as.matrix(x[vapply(x, is.numeric, TRUE)]) else if (is.matrix(x)) x else as.matrix(do.call(cbind, num))");
			r.add("  xa <- if (!is.null(xv) && length(xv) == nrow(m)) xv else seq_len(nrow(m))  # the values of the axes");
			r.add("  yb <- if (!is.null(ya) && length(ya) == ncol(m)) ya else seq_len(ncol(m))");
			if (o.kind().equals("heatmap")) {
				r.add("  image(seq_len(ncol(m)), seq_len(nrow(m)), t(m)[, nrow(m):1, drop = FALSE], col = grDevices::hcl.colors(64, 'viridis'), axes = FALSE, main = "
						+ main + ", xlab = " + lab(o.xLabel(), "'Column'") + ", ylab = " + lab(o.yLabel(), "'Row'") + ")");
				r.add("  axis(1, at = seq_len(ncol(m)), labels = if (is.null(colnames(m))) seq_len(ncol(m)) else colnames(m), las = 2, cex.axis = 0.8)");
			} else if (o.kind().equals("contour")) {
				r.add("  if (nrow(m) < 2 || ncol(m) < 2) stop('A contour plot needs a matrix (or data frame) with at least 2 rows and 2 columns')");
				r.add("  image(xa, yb, m, col = grDevices::hcl.colors(64, 'viridis'), main = " + main + ", xlab = " + lab(o.xLabel(), d.x().isEmpty() ? "'Row'" : str(d.x()))
						+ ", ylab = " + lab(o.yLabel(), !d.z().isEmpty() && !d.ys().isEmpty() ? str(d.ys().get(0)) : "'Column'") + ")");
				r.add("  contour(xa, yb, m, add = TRUE, col = 'white', lwd = max(0.5, lwd / 2), labcex = 0.7)");
			} else {
				r.add("  if (nrow(m) < 2 || ncol(m) < 2) stop('A surface needs a matrix (or data frame) with at least 2 rows and 2 columns')");
				r.add("  nr <- nrow(m); nc <- ncol(m)");
				r.add("  zf <- (m[-1, -1] + m[-1, -nc] + m[-nr, -1] + m[-nr, -nc]) / 4  # the height of the facets");
				r.add("  fc <- grDevices::hcl.colors(64, 'viridis')[cut(zf, 64)]");
				r.add("  persp(xa, yb, m, theta = 35, phi = 25, expand = 0.6, col = fc, border = grDevices::adjustcolor('white', 0.3), ticktype = 'detailed', main = "
						+ main + ", xlab = " + lab(o.xLabel(), d.x().isEmpty() ? "'Row'" : str(d.x())) + ", ylab = "
						+ lab(o.yLabel(), !d.z().isEmpty() && !d.ys().isEmpty() ? str(d.ys().get(0)) : "'Column'") + ", zlab = 'Value')");
			}
		}
		default -> { // qq
			r.add("  q <- lapply(num, function(v) stats::qqnorm(v[is.finite(v)], plot.it = FALSE))");
			frame(r, main, "range(unlist(lapply(q, `[[`, 'x')))", "range(unlist(lapply(q, `[[`, 'y')))", "''", lab(o.xLabel(), "'Theoretical quantiles'"),
					lab(o.yLabel(), "'Sample quantiles'"), grid);
			r.add("  for (i in seq_along(q)) {");
			r.add("    stats::qqline(num[[i]][is.finite(num[[i]])], col = if (length(q) > 1) cols[i] else 'gray40', lwd = lwd, lty = lty)");
			r.add("    " + points(o, "q[[i]]$x", "q[[i]]$y", "i"));
			r.add("  }");
			legend(r, o, "topleft", "names(num), pch = pch, " + (o.modern() ? "pt.bg = fill[seq_along(num)], col = 'white', pt.cex = 1.2" : "col = fill[seq_along(num)]"));
		}
		}
		return r;
	}

	/* An empty plot with the limits (or xlim and ylim of the attributes) and the grid behind the data */
	private static void frame(List<String> r, String main, String x, String y, String log, String xlab, String ylab, String grid) {
		r.add("  plot(NA, xlim = if (is.null(xlim)) " + x + " else xlim, ylim = if (is.null(ylim)) " + y + " else ylim, log = " + log + ", main = " + main
				+ ", xlab = " + xlab + ", ylab = " + ylab + ")");
		if (grid != null) {
			r.add("  " + grid);
		}
	}

	private static String points(PlotOptions o, String x, String y, String i) {
		return o.modern() ? "points(" + x + ", " + y + ", pch = pch, bg = fill[" + i + "], col = 'white', lwd = 1.2, cex = 1.3 * cex)"
				: "points(" + x + ", " + y + ", pch = pch, col = fill[" + i + "], cex = cex)";
	}

	/* The observations of box and violin plots (jittered, at most 300 of each variable) */
	private static void observations(List<String> r, PlotOptions o, boolean horizontal) {
		r.add("  set.seed(42)");
		r.add("  for (i in seq_along(num)) {  # the observations (at most 300)");
		r.add("    v <- num[[i]][is.finite(num[[i]])]; if (length(v) > 300) v <- sample(v, 300)");
		r.add("    j <- i + runif(length(v), -0.12, 0.12)");
		String args = o.modern() ? "pch = pch, bg = cols[i], col = 'white', cex = 0.9 * cex, lwd = 0.8" : "pch = pch, col = fill[i], cex = 0.7 * cex";
		r.add(horizontal ? "    points(v, j, " + args + ")" : "    points(j, v, " + args + ")");
		r.add("  }");
	}

	/* A legend (if not 'none') without the condition of several series */
	private static void legendAlways(List<String> r, PlotOptions o, String auto, String args) {
		if (!o.legend().equals("none")) {
			r.add("    " + legendCall(o, auto, args));
		}
	}

	private static String legendCall(PlotOptions o, String auto, String args) {
		if (o.legend().equals("none")) {
			return "NULL";
		}
		String pos = o.legend().equals("auto") ? auto : o.legend();
		return "legend(" + str(pos) + ", " + args + ", bty = 'n', cex = 0.85)";
	}

	private static void legend(List<String> r, PlotOptions o, String auto, String args) {
		if (o.legend().equals("none")) {
			return;
		}
		String pos = o.legend().equals("auto") ? auto : o.legend();
		r.add("  if (length(num) > 1) legend(" + str(pos) + ", " + args + ", bty = 'n', cex = 0.85"
				+ (o.modern() && pos.startsWith("top") && o.kind().equals("line") ? ", horiz = length(num) <= 4" : "") + ")");
	}

	/* The titles: modern left aligned (bold title, subtitle, caption), classic: subtitle and caption */
	private static void titles(List<String> r, PlotOptions o) {
		double cex = o.titleSize() / o.fontSize();
		boolean own = o.kind().equals("surface") || o.kind().equals("pairs");
		r.add("  # The titles");
		if (o.modern() && !own) {
			boolean pie = o.kind().equals("pie");
			r.add("  mtext(title, side = 3, line = if (nzchar(subtitle)) " + (pie ? "1.5" : "2.5") + " else " + (pie ? "0.5" : "1.5") + ", adj = 0, cex = " + n(cex)
					+ ", font = 2, col = 'gray10')");
			r.add("  if (nzchar(subtitle)) mtext(subtitle, side = 3, line = " + (pie ? "0.4" : "1.2") + ", adj = 0, cex = 0.9, col = 'gray40')");
		} else {
			r.add("  if (nzchar(subtitle)) mtext(subtitle, side = 3, line = 0.4, cex = 0.9, col = 'gray30')");
		}
		r.add("  if (nzchar(caption)) mtext(caption, side = 1, line = 3.8, adj = 1, cex = 0.75, col = 'gray50')");
	}

	private static String autoTitle(String kind, PlotData d) {
		String vars = d.describe();
		String[] names = d.ys().isEmpty() ? new String[] { d.z() } : d.ys().toArray(String[]::new);
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

	/* A label of the dialog or the default (an R expression) */
	private static String lab(String label, String auto) {
		return label.isEmpty() ? auto : str(label);
	}

	private static String lim(String min, String max) {
		Double a = PlotOptions.limit(min), b = PlotOptions.limit(max);
		return a != null && b != null ? "c(" + n(a) + ", " + n(b) + ")" : "NULL";
	}

	private static String pch(PlotOptions o) {
		boolean filled = o.modern();
		return switch (o.pointShape()) {
		case "square" -> filled ? "22" : "15";
		case "triangle" -> filled ? "24" : "17";
		case "diamond" -> filled ? "23" : "18";
		default -> filled ? "21" : "19";
		};
	}

	private static String str(String s) {
		return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	private static String n(double d) {
		return PlotOptions.num(d);
	}

	private static String bool(boolean b) {
		return b ? "TRUE" : "FALSE";
	}
}
