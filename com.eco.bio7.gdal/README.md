# com.eco.bio7.gdal: GDAL for Java (FFM)

GDAL of an installation on the computer (conda-forge, Homebrew, MacPorts, the
packages of Linux, QGIS, OSGeo4W) called with the Foreign Function and Memory
API of Java (Java 22+). No JNI library, no compiled code, no GDAL is shipped.

The plugin has no dependency on Bio7 and can be used in other RCP applications:

* `com.eco.bio7.gdal`: the API (only Java, no Eclipse classes)
* `com.eco.bio7.gdal.ui`: preferences, activation, test, installer, startup
  (Eclipse UI)

## API

```java
Gdal.load();                                    // the GDAL found first (or Gdal.load(Path))
try (GdalDataset ds = Gdal.open("dem.tif")) {
    GdalBand band = ds.band(1);
    float[] heights = band.readFloats();        // row by row, the top row first
    Double noData = band.noData();
    double[] gt = ds.geoTransform();            // x = gt[0] + col*gt[1] + row*gt[2], ...
    double[] xy = ds.pixelToWorld(10.5, 20.5);
    try (SpatialReference srs = ds.spatialReference()) {
        String epsg = srs.authority();          // e.g. "EPSG:32632"
    }
}

// Create a GeoTIFF
try (GdalDataset out = Gdal.create("out.tif", "GTiff", w, h, 1, GdalDataType.FLOAT32, "COMPRESS=DEFLATE")) {
    out.setGeoTransform(gt);
    out.setProjection("EPSG:32632");
    out.band(1).write(values);
}

// The command line tools (options as on the command line)
Gdal.warp("dem.tif", "dem_wgs84.tif", "-t_srs", "EPSG:4326");
Gdal.translate("dem.tif", "dem.png", "-of", "PNG", "-ot", "Byte", "-scale");
Gdal.vectorTranslate("roads.shp", "roads.gpkg", "-f", "GPKG", "-t_srs", "EPSG:4326");
String info = Gdal.info("dem.tif", "-stats");
try (GdalDataset mem = Gdal.warp(src, "", "-of", "MEM", "-t_srs", "EPSG:4326")) { ... }  // in memory

// Vectors
try (GdalDataset ds = Gdal.open("areas.gpkg")) {
    for (VectorLayer layer : ds.layers()) {
        for (Feature f : layer.features()) {
            Map<String, Object> attributes = f.attributes();
            Geometry g = f.geometry();          // parts with {x, y} coordinates (also f.wkt(), f.geoJson())
        }
    }
}

// Coordinates
try (SpatialReference utm = SpatialReference.fromEpsg(32632);
     SpatialReference wgs84 = SpatialReference.fromEpsg(4326);
     CoordinateTransform ct = utm.transformTo(wgs84)) {
    double[] lonLat = ct.transform(500000, 5500000);   // x = longitude (traditional GIS order)
}
```

```java
// Write vectors (any writable format; "MEM" with the name "" in memory)
try (GdalDataset ds = Gdal.createVector("plots.gpkg", "GPKG");
     SpatialReference utm = SpatialReference.fromEpsg(32632)) {
    VectorLayer layer = ds.createLayer("plots", utm, GeometryType.POLYGON);
    layer.createField("name", FieldType.STRING);
    layer.createField("area", FieldType.REAL);
    layer.addFeature(Map.of("name", "A", "area", 4.2), "POLYGON ((...))");
}

// Filters and SQL
layer.setAttributeFilter("area > 1000");             // SQL WHERE (null: all)
layer.setSpatialFilter(minX, minY, maxX, maxY);      // clearSpatialFilter()
try (VectorLayer r = ds.executeSql("SELECT type, SUM(area) FROM plots GROUP BY type")) { ... }

// gdaldem, gdalbuildvrt, gdal_rasterize
Gdal.dem("dem.tif", "hillshade.tif", "hillshade", "-z", "2");   // slope, aspect, TRI, TPI, roughness
Gdal.colorRelief("dem.tif", "colors.tif", "colors.txt");
Gdal.buildVrt("mosaic.vrt", List.of("a.tif", "b.tif"));
Gdal.rasterize("plots.gpkg", "plots.tif", "-a", "area", "-tr", "10", "10");

// Contours and polygons (into a vector layer)
dem.band(1).contours(lines, 50);                     // fields ID and ELEV; "POLYGONIZE=YES" for areas
classes.band(1).polygonize(polygons, "class", true); // areas of equal values (true: without no data)
```

All errors of GDAL are `GdalException`s with the message of GDAL.

## Compatibility layer (org.gdal)

The packages `org.gdal.gdal`, `org.gdal.gdalconst`, `org.gdal.osr` and
`org.gdal.ogr` have the classes and methods of the Java bindings of GDAL (SWIG,
`gdal.jar`) which are used by WorldWind (`gdal`, `Dataset`, `Band`, `Driver`,
`ColorTable`, `SpatialReference`, `CoordinateTransformation`, the constants),
built on this bridge: no `gdal.jar`, no JNI library, no GDAL data folder of the
application (the data of the installation is used). If GDAL is not loaded the
methods throw an `UnsatisfiedLinkError` (as a missing JNI library). The axis order
of coordinate systems is the traditional one (x = longitude), as the code
written for GDAL 1 and 2 expects.

## Safety

A native library can end the Java process (e.g., a library of another
architecture, missing dependencies). Therefore:

* GDAL is only loaded if it is activated in the preferences (or when the user
  agrees when it is used).
* A library is first tested in a separate Java process (`GdalProbe`); only a
  library which passed the test is loaded into the application. The reason of
  a failure (the message of the system loader) is shown.

## Preferences (Preferences > GDAL)

* Activate GDAL; load GDAL at startup (in the background)
* The library (empty: the automatic search); Detect, Test, Install GDAL

## Installation of GDAL

* macOS: `brew install gdal`, or conda-forge
* Linux: `apt install gdal-bin`, `dnf install gdal`, `zypper install gdal`,
  `pacman -S gdal`, or conda-forge
* Windows: conda-forge (Miniforge), or OSGeo4W
* conda-forge on all systems: `conda create -n bio7-gdal -c conda-forge libgdal-core`
