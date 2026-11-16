# Copyright (c) 2007-2026 M. Austenfeld
# SPDX-License-Identifier: EPL-2.0
#
# The cluster analysis and the PCA of images of the Image-Methods view in the
# embedded Python (Cluster Pic, PCA): the pixels of the images are the rows,
# the images (the channels of RGB images) the columns. scikit-learn if
# installed, else numpy. The results as images in ImageJ, plots in the plot
# view and variables in Python.

import sys
import numpy as np

_main = sys.modules["__main__"]


def _show(bio7, image, title, display_range):
    """Shows an image in ImageJ with a display range (older bio7 modules: set with the Java API)."""
    try:
        bio7.show_image(image, title, display_range=display_range)
    except TypeError:
        # The bio7 module of a Python started before the update of Bio7 (no display_range yet)
        bio7.show_image(image, title)
        try:
            lo, hi = (float(np.min(image)), float(np.max(image))) if display_range == "auto" else display_range
            imp = bio7.java.ij.WindowManager.getImage(title)
            if imp is not None:
                imp.setDisplayRange(float(lo), float(hi))
                imp.updateAndDraw()
        except Exception:
            pass


def _features(images, names):
    """The pixels (rows) of the images (columns; RGB: a column per channel)."""
    cols, labels, shape = [], [], None
    for img, name in zip(images, names):
        a = np.asarray(img, dtype=np.float64)
        if a.ndim == 3 and a.shape[-1] == 3:
            plane = a.shape[:2]
            parts = [(a[..., k], "%s (%s)" % (name, "RGB"[k])) for k in range(3)]
        elif a.ndim == 3:
            # A stack: the current slice of the transfer is the first one
            plane = a.shape[1:]
            parts = [(a[0], name)]
        else:
            plane = a.shape
            parts = [(a, name)]
        if shape is None:
            shape = plane
        elif plane != shape:
            raise ValueError("The images must have the same size: %s is %dx%d, the first %dx%d"
                             % (name, plane[1], plane[0], shape[1], shape[0]))
        for p, label in parts:
            cols.append(p.ravel())
            labels.append(label)
    return np.column_stack(cols), labels, shape


def _options(text):
    """The arguments of the dialog (Python keyword arguments, e.g. n_init=10, scale=True)."""
    text = (text or "").strip()
    if not text:
        return {}
    try:
        return eval("dict(" + text + ")", {"__builtins__": {"dict": dict, "True": True, "False": False, "None": None}})
    except Exception as e:
        raise ValueError("The arguments are not valid Python keyword arguments (%s): %s" % (text, e))


def _scale(x, opts):
    if opts.pop("scale", False):
        sd = x.std(axis=0)
        sd[sd == 0] = 1
        return (x - x.mean(axis=0)) / sd, True
    return x, False


def _kmeans_numpy(x, k, seed=0, iterations=100):
    """k-means (k-means++ start) with numpy, if scikit-learn is not installed."""
    rng = np.random.default_rng(seed)
    sample = x[rng.choice(len(x), min(len(x), 20000), replace=False)]
    centers = [sample[rng.integers(len(sample))]]
    for _ in range(1, k):
        d = np.min([((sample - c) ** 2).sum(axis=1) for c in centers], axis=0)
        centers.append(sample[rng.choice(len(sample), p=d / d.sum())] if d.sum() > 0 else sample[rng.integers(len(sample))])
    centers = np.array(centers)
    for _ in range(iterations):
        labels = np.argmin(((x[:, None, :] - centers[None]) ** 2).sum(axis=2), axis=1) if len(x) * k < 5e7 else \
            np.concatenate([np.argmin(((c[:, None, :] - centers[None]) ** 2).sum(axis=2), axis=1) for c in np.array_split(x, 50)])
        new = np.array([x[labels == j].mean(axis=0) if np.any(labels == j) else centers[j] for j in range(k)])
        if np.allclose(new, centers):
            break
        centers = new
    return labels, centers


def cluster(a):
    """Cluster Pic: the cluster of each pixel as new image (1..k), plots of the clusters."""
    import bio7
    names = list(a["names"])
    x, labels_cols, shape = _features(a["images"], names)
    k = int(a["k"])
    opts = _options(a.get("options"))
    x, scaled = _scale(x, opts)
    algorithm = a.get("algorithm") or "kmeans"
    try:
        from sklearn.cluster import KMeans, MiniBatchKMeans
    except Exception:
        # Not installed (or not loadable, e.g., built for another numpy)
        KMeans = MiniBatchKMeans = None
    if KMeans is not None:
        opts.setdefault("n_init", "auto" if algorithm == "minibatch" else 10)
        opts.setdefault("random_state", 0)
        model = (MiniBatchKMeans if algorithm == "minibatch" else KMeans)(n_clusters=k, **opts).fit(x)
        labels, centers, used = model.labels_, model.cluster_centers_, type(model).__name__
        _main.kmeans_result = model
    else:
        labels, centers = _kmeans_numpy(x, k, seed=opts.get("random_state", 0) or 0)
        used = "k-means (numpy; scikit-learn is not available)"
        _main.kmeans_result = {"labels": labels, "centers": centers}
    image = (labels.reshape(shape) + 1).astype(np.uint8)
    _main.image_cluster = image
    # The labels 1..k: the contrast of the clusters (else black in the range 0..255)
    _show(bio7, image, "Cluster Image", (0, k))

    sizes = np.bincount(labels, minlength=k)
    means = np.array([x[labels == j].mean(axis=0) if sizes[j] else np.full(x.shape[1], np.nan) for j in range(k)])
    lines = ["%s: %d clusters of %d pixels, %d images (columns)%s" % (used, k, len(x), x.shape[1], ", scaled" if scaled else "")]
    for j in range(k):
        lines.append("  cluster %d: %d pixels (%.1f %%), mean %s" % (j + 1, sizes[j], 100 * sizes[j] / len(x),
                                                                    np.array2string(means[j], precision=2, max_line_width=200)))
    _plot_cluster(image, k, sizes, means, labels_cols, scaled)
    return "\n".join(lines)


def _plot_cluster(image, k, sizes, means, columns, scaled):
    try:
        import matplotlib.pyplot as plt
    except ImportError:
        return
    cmap = plt.get_cmap("tab10" if k <= 10 else "tab20", k)
    fig, axes = plt.subplots(1, 3, figsize=(13, 4.2), constrained_layout=True)
    im = axes[0].imshow(image, cmap=cmap, vmin=0.5, vmax=k + 0.5, interpolation="nearest")
    axes[0].set_title("Clusters")
    axes[0].axis("off")
    fig.colorbar(im, ax=axes[0], ticks=range(1, k + 1), shrink=0.8)
    axes[1].bar(range(1, k + 1), sizes, color=[cmap(j) for j in range(k)])
    axes[1].set(title="Pixels per cluster", xlabel="Cluster", ylabel="Pixels", xticks=range(1, k + 1))
    for j in range(k):
        axes[2].plot(range(len(columns)), means[j], "o-", color=cmap(j), label="cluster %d" % (j + 1))
    axes[2].set_xticks(range(len(columns)))
    axes[2].set_xticklabels(columns, rotation=30, ha="right")
    axes[2].set(title="Cluster means per image", ylabel="Mean (scaled)" if scaled else "Mean value")
    if k <= 12:
        axes[2].legend(fontsize="small")
    plt.show()


def pca(a):
    """PCA: the components as new images (float), the summary and plots of the variance and loadings."""
    import bio7
    names = list(a["names"])
    x, columns, shape = _features(a["images"], names)
    opts = _options(a.get("options"))
    x, scaled = _scale(x, opts)
    n = max(1, min(int(a["k"]), x.shape[1]))
    try:
        from sklearn.decomposition import PCA
    except Exception:
        PCA = None
    if PCA is not None:
        model = PCA(**opts).fit(x)
        scores = model.transform(x)
        variance = model.explained_variance_
        ratio = model.explained_variance_ratio_
        loadings = model.components_.T
        _main.pca_result = model
        used = "PCA (scikit-learn)"
    else:
        xc = x - x.mean(axis=0)
        u, s, vt = np.linalg.svd(xc, full_matrices=False)
        scores = u * s
        variance = s ** 2 / (len(x) - 1)
        ratio = variance / variance.sum()
        loadings = vt.T
        _main.pca_result = {"scores": scores, "loadings": loadings, "variance": variance, "ratio": ratio}
        used = "PCA (numpy; scikit-learn is not available)"
    n = min(n, scores.shape[1])
    for i in range(n):
        comp = scores[:, i].reshape(shape).astype(np.float32)
        setattr(_main, "pca_component%d" % (i + 1), comp)
        _show(bio7, comp, "PCAComponent%d" % (i + 1), "auto")
    sd = np.sqrt(variance)
    cumulative = np.cumsum(ratio)
    lines = ["%s of %d pixels, %d images (columns)%s" % (used, len(x), x.shape[1], ", scaled" if scaled else ""),
             "Importance of components:",
             "                        " + "".join("%10s" % ("PC%d" % (i + 1)) for i in range(len(ratio))),
             "Standard deviation      " + "".join("%10.4f" % v for v in sd),
             "Proportion of Variance  " + "".join("%10.4f" % v for v in ratio),
             "Cumulative Proportion   " + "".join("%10.4f" % v for v in cumulative)]
    _plot_pca(ratio, cumulative, loadings, columns, scores, n)
    return "\n".join(lines)


def _plot_pca(ratio, cumulative, loadings, columns, scores, n):
    try:
        import matplotlib.pyplot as plt
    except ImportError:
        return
    pcs = ["PC%d" % (i + 1) for i in range(len(ratio))]
    ncols = 3 if scores.shape[1] >= 2 else 2
    fig, axes = plt.subplots(1, ncols, figsize=(4.6 * ncols, 4.2), constrained_layout=True)
    axes[0].bar(pcs, 100 * ratio, color="steelblue", label="component")
    axes[0].plot(pcs, 100 * cumulative, "o-", color="darkorange", label="cumulative")
    axes[0].set(title="Explained variance", ylabel="% of the variance", ylim=(0, 105))
    axes[0].legend(fontsize="small")
    k = min(len(pcs), max(n, 2))
    lim = np.abs(loadings[:, :k]).max() or 1
    im = axes[1].imshow(loadings[:, :k], cmap="RdBu_r", vmin=-lim, vmax=lim, aspect="auto")
    axes[1].set_xticks(range(k))
    axes[1].set_xticklabels(pcs[:k])
    axes[1].set_yticks(range(len(columns)))
    axes[1].set_yticklabels(columns)
    axes[1].set_title("Loadings")
    for (i, j), v in np.ndenumerate(loadings[:, :k]):
        axes[1].text(j, i, "%.2f" % v, ha="center", va="center", fontsize=8, color="white" if abs(v) > 0.6 * lim else "black")
    fig.colorbar(im, ax=axes[1], shrink=0.8)
    if ncols == 3:
        rng = np.random.default_rng(0)
        idx = rng.choice(len(scores), min(len(scores), 5000), replace=False)
        axes[2].scatter(scores[idx, 0], scores[idx, 1], s=3, alpha=0.4)
        axes[2].set(title="Pixels (sample)", xlabel="PC1 (%.1f %%)" % (100 * ratio[0]), ylabel="PC2 (%.1f %%)" % (100 * ratio[1]))
    plt.show()


def transfer_only(a):
    """Only Transfer: the images as numpy arrays image1, image2, ... (as for R)."""
    out = []
    for i, img in enumerate(a["images"], start=1):
        setattr(_main, "image%d" % i, np.asarray(img))
        out.append("image%d" % i)
    return "Transferred: " + ", ".join(out)
