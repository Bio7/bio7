# Copyright (c) 2007-2026 M. Austenfeld
# SPDX-License-Identifier: EPL-2.0
#
# The transfer of the selections (ROIs) of ImageJ to the embedded Python (the
# view Geometry Selections): the coordinates as lists of numpy arrays or the
# geometries (shapely) as GeoDataFrame (geopandas) with the columns of a
# DataFrame and the coordinate reference system of a georeferenced file.

import sys
import numpy as np

_main = sys.modules["__main__"]

_NAMES = {0: "spatial_polygons", 1: "spatial_lines", 2: "spatial_points"}


def _georeference(a):
    """The transform (as GDAL) and the CRS: from Bio7 (GDAL) or read with rasterio."""
    if a.get("gt") is not None:
        return [float(v) for v in a["gt"]], a.get("crs") or None
    try:
        import rasterio
    except ImportError:
        raise ImportError("GDAL is not available in Bio7: the georeferenced file is read with the Python package "
                          "'rasterio' which is not installed (Python Packages: rasterio).")
    with rasterio.open(a["crs_file"]) as ds:
        t = ds.transform
        crs = ds.crs.to_string() if ds.crs is not None else None
    return [t.c, t.a, t.b, t.f, t.d, t.e], crs


def _transform(x, y, gt, centroid):
    """Pixel coordinates (column x, row y) to the coordinates of the georeference."""
    off = 0.5 if centroid else 0.0
    xp = gt[0] + gt[1] * (x + off) + gt[2] * (y + off)
    yp = gt[3] + gt[4] * (x + off) + gt[5] * (y + off)
    return xp, yp


def transfer(a):
    """Creates the variables; returns a description for the console."""
    xs = [np.asarray(x, dtype=float) for x in a["xs"]]
    ys = [np.asarray(y, dtype=float) for y in a["ys"]]
    w, h = int(a["size"][0]), int(a["size"][1])
    if a["as_list"]:
        _main.roi_x_coordinates = xs
        _main.roi_y_coordinates = ys
        _main.image_size = (w, h)
        return "roi_x_coordinates, roi_y_coordinates (%d selections), image_size" % len(xs)

    try:
        from shapely import geometry as sg
    except ImportError:
        raise ImportError("The Python package 'shapely' (and 'geopandas' for a GeoDataFrame) is needed: "
                          "install them with Python Packages (shapely geopandas).")
    crs = None
    if a["set_crs"]:
        gt, crs = _georeference(a)
        pairs = [_transform(x, y, gt, a["centroid"]) for x, y in zip(xs, ys)]
        xs, ys = [p[0] for p in pairs], [p[1] for p in pairs]

    kind = int(a["kind"])
    geoms, ids, skipped = [], [], []
    for i, (x, y) in enumerate(zip(xs, ys), start=1):
        xy = np.column_stack([x, y])
        if kind == 0:
            if len(xy) < 3:
                skipped.append(i)
                continue
            geoms.append(sg.Polygon(xy))
            ids.append(i)
        elif kind == 1:
            if len(xy) < 2:
                skipped.append(i)
                continue
            geoms.append(sg.LineString(xy))
            ids.append(i)
        else:
            # The points of all selections (as SpatialPoints of R)
            geoms.extend(sg.Point(px, py) for px, py in xy)
            ids.extend([i] * len(xy))
    if not geoms:
        raise ValueError("No geometry: the selections have too few points for %s" % _NAMES[kind].split("_")[1])

    df = None
    if a.get("df"):
        df = getattr(_main, a["df"], None)
        if df is None or type(df).__name__ != "DataFrame":
            raise ValueError("No pandas DataFrame '%s' in Python" % a["df"])
        if len(df) != len(geoms):
            raise ValueError("The DataFrame '%s' has %d rows, but there are %d geometries" % (a["df"], len(df), len(geoms)))

    name = _NAMES[kind]
    try:
        import geopandas as gpd
        if df is not None:
            data = df.reset_index(drop=True).copy()
            data.insert(0, "roi", ids)
        else:
            data = {"roi": ids}
        result = gpd.GeoDataFrame(data, geometry=geoms, crs=crs)
        description = "%s (GeoDataFrame, %d rows%s)" % (name, len(result), ", CRS " + str(crs) if crs else "")
    except ImportError:
        if df is not None or crs is not None:
            raise ImportError("The Python package 'geopandas' is needed for a DataFrame or a projection "
                              "(Python Packages: geopandas).")
        result = geoms
        description = "%s (list of %d shapely geometries; with geopandas a GeoDataFrame)" % (name, len(result))
    setattr(_main, name, result)
    _main.image_size = (w, h)
    if skipped:
        description += "; skipped selections with too few points: %s" % skipped
    return description
