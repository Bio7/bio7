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
package com.eco.bio7.image;

import com.eco.bio7.rbridge.backend.RBackend;

/**
 * The plots of "Cluster Pic" and "PCA" of the Image-Methods view in R (as the
 * plots of the analysis in Python): drawn with the graphics of R in the plot
 * view of the embedded R (not with Rserve).
 */
final class RImageAnalysisPlots {

	/* The plot device of Bio7 if the current device is not one of Bio7 (e.g., the null device) */
	private static final String DEVICE = "if (exists('bio7PlotCanvas', mode = 'function') && !(names(grDevices::dev.cur()) %in% c('bio7PlotCanvas', 'bio7PlotVector'))) bio7PlotCanvas()\n";

	private RImageAnalysisPlots() {
	}

	/**
	 * The clusters: the image of the clusters, the pixels per cluster and the
	 * cluster means per image. Needs clusterData (the pixels in the rows).
	 *
	 * @param clusters the R expression of the cluster of each pixel
	 */
	static void clusters(RBackend c, String clusters, int k, int width, int height, String[] names) {
		if (c == null || !c.isEmbedded()) {
			return;
		}
		String code = DEVICE + "try(local({\n" //
				+ "  cl <- as.integer(" + clusters + ")\n" //
				+ "  k <- " + k + "; w <- " + width + "; h <- " + height + "\n" //
				+ "  names <- " + names(names) + "\n" //
				+ "  cols <- grDevices::hcl.colors(k, 'Dark 3')\n" //
				+ "  sizes <- tabulate(cl, k)\n" //
				+ "  means <- do.call(rbind, lapply(1:k, function(j) colMeans(clusterData[cl == j, , drop = FALSE])))\n" //
				+ "  op <- par(mfrow = c(1, 3), mar = c(7, 4.5, 3, 1)); on.exit(par(op))\n" //
				/* The pixels are row by row (x fastest): m[x, y], y from the top */
				+ "  m <- matrix(cl, nrow = w)\n" //
				+ "  image(1:w, 1:h, m[, h:1, drop = FALSE], col = cols, breaks = 0:k + 0.5, axes = FALSE, xlab = '', ylab = '', main = 'Clusters', asp = 1)\n" //
				+ "  barplot(sizes, names.arg = 1:k, col = cols, main = 'Pixels per cluster', xlab = 'Cluster', ylab = 'Pixels')\n" //
				+ "  matplot(t(means), type = 'b', pch = 19, lty = 1, col = cols, xaxt = 'n', main = 'Cluster means per image', xlab = '', ylab = 'Mean value')\n" //
				+ "  axis(1, at = seq_len(ncol(means)), labels = rep_len(names, ncol(means)), las = 2, cex.axis = 0.8)\n" //
				+ "  if (k <= 12) legend('topright', legend = paste('cluster', 1:k), col = cols, pch = 19, lty = 1, cex = 0.8, bty = 'n')\n" //
				+ "}))";
		eval(c, code);
	}

	/**
	 * The PCA: the explained variance, the loadings and the pixels (sample) in
	 * PC1 and PC2. Needs pcaData (the result of prcomp).
	 */
	static void pca(RBackend c, int components, String[] names) {
		if (c == null || !c.isEmbedded()) {
			return;
		}
		String code = DEVICE + "try(local({\n" //
				+ "  p <- pcaData; n <- " + components + "\n" //
				+ "  names <- " + names(names) + "\n" //
				+ "  prop <- p$sdev^2 / sum(p$sdev^2); cum <- cumsum(prop)\n" //
				+ "  pcs <- paste0('PC', seq_along(prop))\n" //
				+ "  three <- ncol(p$x) >= 2\n" //
				+ "  op <- par(mfrow = c(1, if (three) 3 else 2), mar = c(5, 7, 3, 2)); on.exit(par(op))\n" //
				+ "  b <- barplot(100 * prop, names.arg = pcs, col = 'steelblue', ylim = c(0, 105), main = 'Explained variance', ylab = '% of the variance')\n" //
				+ "  lines(b, 100 * cum, type = 'b', pch = 19, col = 'darkorange')\n" //
				+ "  legend('right', c('component', 'cumulative'), fill = c('steelblue', NA), border = c('black', NA), col = c(NA, 'darkorange'), pch = c(NA, 19), bty = 'n', cex = 0.8)\n" //
				+ "  kk <- min(ncol(p$rotation), max(n, 2)); L <- p$rotation[, 1:kk, drop = FALSE]; lim <- max(abs(L)); nr <- nrow(L)\n" //
				+ "  image(1:kk, 1:nr, t(L)[, nr:1, drop = FALSE], col = grDevices::hcl.colors(51, 'Blue-Red 3'), zlim = c(-lim, lim), axes = FALSE, xlab = '', ylab = '', main = 'Loadings')\n" //
				+ "  axis(1, at = 1:kk, labels = pcs[1:kk]); axis(2, at = 1:nr, labels = rev(rep_len(names, nr)), las = 1, cex.axis = 0.8)\n" //
				+ "  text(rep(1:kk, each = nr), rep(nr:1, kk), sprintf('%.2f', L), cex = 0.8, col = ifelse(abs(L) > 0.6 * lim, 'white', 'black'))\n" //
				+ "  if (three) {\n" //
				+ "    set.seed(0); i <- sample(nrow(p$x), min(nrow(p$x), 5000))\n" //
				+ "    plot(p$x[i, 1], p$x[i, 2], pch = 16, cex = 0.4, col = grDevices::adjustcolor('steelblue', 0.4), main = 'Pixels (sample)',\n" //
				+ "         xlab = sprintf('PC1 (%.1f %%)', 100 * prop[1]), ylab = sprintf('PC2 (%.1f %%)', 100 * prop[2]))\n" //
				+ "  }\n" //
				+ "}))";
		eval(c, code);
	}

	/* The names of the images as character vector of R */
	private static String names(String[] names) {
		StringBuilder s = new StringBuilder("c(");
		for (int i = 0; i < names.length; i++) {
			s.append(i > 0 ? ", " : "").append('"').append(names[i].replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
		}
		return s.append(")").toString();
	}

	private static void eval(RBackend c, String code) {
		try {
			c.voidEval(code);
		} catch (Exception e) {
			System.err.println("The plot could not be drawn: " + e.getMessage());
		}
	}
}
