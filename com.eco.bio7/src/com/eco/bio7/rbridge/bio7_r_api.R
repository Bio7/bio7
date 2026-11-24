# Copyright (c) 2007-2026 M. Austenfeld
# SPDX-License-Identifier: EPL-2.0
#
# The API of Bio7 for the embedded R (as the module bio7 of the embedded Python):
# ImageJ, the embedded Python and the Java API of Bio7, ImageJ and Java. The
# functions are in the attached environment "bio7" (see bio7_api()). They call
# Java with .bio7_call(function, args, value) (com.eco.bio7.rbridge.RCalls).

local({
  if ("bio7" %in% search()) detach("bio7", character.only = TRUE)
  env <- attach(NULL, name = "bio7", warn.conflicts = FALSE)
  call <- get(".bio7_call", envir = as.environment("bio7ffm"))

  # ------------------------------------------------------------------ ImageJ

  env$bio7_image <- function(title = NULL) {
    "The current (or the named) ImageJ image: a matrix (height x width), a stack
    array (slices, height, width) or RGB array (height, width, 3)."
    call("image", if (is.null(title)) character() else title, NULL)
  }

  env$bio7_show_image <- function(x, title = "R", range = NULL) {
    "Shows a matrix (or a stack array (slices, height, width), RGB (height, width, 3))
    as ImageJ image. range: the display range c(min, max) or \"auto\" (the range of
    the values); NULL: the default of ImageJ."
    args <- if (is.null(range)) title
            else if (is.character(range)) c(title, range, "")
            else c(title, format(range[1], digits = 17), format(range[2], digits = 17))
    invisible(call("show_image", args, x))
  }

  env$bio7_ij_macro <- function(code, argument = "") {
    "Runs an ImageJ macro: the value of its return statement (or NULL)."
    call("ij_macro", c(code, argument), NULL)
  }

  env$bio7_ij_macro_file <- function(file, argument = "") {
    "Runs an ImageJ macro file (or a file of the macros folder of ImageJ)."
    call("ij_macro_file", c(normalizePath(file, mustWork = FALSE), argument), NULL)
  }

  env$bio7_ij_run <- function(command, options = "") {
    "Runs an ImageJ command with options on the current image (as run() of the macro language)."
    invisible(call("ij_run", c(command, options), NULL))
  }

  env$bio7_results_table <- function() {
    "The ImageJ results table as data frame."
    call("results_table", character(), NULL)
  }

  env$bio7_roi <- function() {
    "The selection of the current ImageJ image: a list with the coordinates x and y (and the type)."
    call("roi", character(), NULL)
  }

  env$bio7_browse <- function(url) {
    "Opens a URL (or an HTML file) in the browser view of Bio7."
    if (file.exists(url)) url <- paste0("file://", normalizePath(url, winslash = "/"))
    invisible(call("open_url", url, NULL))
  }

  # ------------------------------------------------------------------ Python

  env$bio7_progress <- function(done, total = NULL, text = "") {
    "Shows the progress of the running code in Bio7 (a job in the status bar and the Progress view, its stop button interrupts R), e.g. bio7_progress(i, length(files), basename(f)) in a loop. The job ends when done reaches total (or with bio7_progress_end())."
    call("progress", c("progress", as.character(done), if (is.null(total)) "" else as.character(total), as.character(text)), NULL)
    invisible(NULL)
  }
  env$bio7_progress_end <- function() {
    "Ends the job of bio7_progress (e.g., after an interrupted loop)."
    call("progress_end", "progress", NULL)
    invisible(NULL)
  }
  task_count <- 0
  env$bio7_task <- function(text, total = NULL) {
    "A task with progress (a job of its own in the Progress view, its stop button interrupts R):
    t <- bio7_task(\"Bootstrap\", total = 200); then t$update() for one step (t$update(n, text)
    for more steps or a new text) and t$done() at the end (the job also ends when total is reached)."
    task_count <<- task_count + 1
    key <- paste0("task", task_count)
    count <- 0
    total_text <- if (is.null(total)) "" else as.character(total)
    call("progress", c(key, "0", total_text, as.character(text)), NULL)
    list(update = function(n = 1, text = NULL) {
           count <<- count + n
           call("progress", c(key, as.character(count), total_text, if (is.null(text)) "" else as.character(text)), NULL)
           invisible(count)
         },
         done = function() {
           call("progress_end", key, NULL)
           invisible(NULL)
         })
  }
  env$bio7_python <- function(code) {
    "Runs Python code in the embedded Python (the workspace of the Python console):
    the value of a last expression converted to R (numbers, arrays, lists, dicts,
    DataFrames), else NULL."
    call("python", paste(code, collapse = "\n"), NULL)
  }

  env$bio7_to_python <- function(name, value) {
    "Assigns an R value in the embedded Python (a vector or matrix as numpy array,
    a data frame as pandas DataFrame, a list as list or dict)."
    invisible(call("to_python", name, value))
  }

  # ------------------------------------------------------------------ Java

  state <- new.env(parent = emptyenv())
  state$released <- numeric()
  state$members <- new.env(parent = emptyenv())

  slot <- function(x, name) get(name, envir = x, inherits = FALSE)

  # A call of the Java API: the handles of the collected proxies are released first
  jcall <- function(function_, args = character(), value = NULL) {
    if (length(state$released)) {
      ids <- state$released
      state$released <- numeric()
      try(call("java_release", character(), as.numeric(ids)), silent = TRUE)
    }
    wrap(call(function_, as.character(args), value))
  }

  # The handles of Java objects (lists with __java__) as proxies, also in lists
  wrap <- function(v) {
    if (is.list(v) && !is.data.frame(v)) {
      if (!is.null(v[["__java__"]])) return(jobject(v[["__java__"]], v[["class"]]))
      n <- names(v)
      v <- lapply(v, wrap)
      names(v) <- n
    }
    v
  }

  # An argument for Java: the proxies as handles
  arg <- function(a) {
    if (inherits(a, "bio7JavaObject")) list(`__java__` = slot(a, "id"))
    else if (inherits(a, "bio7JavaClass")) list(`__javaclass__` = slot(a, "name"))
    else a
  }

  jargs <- function(args) {
    if (!length(args)) return(list())
    a <- lapply(args, arg)
    names(a) <- as.character(seq_along(a) - 1L)
    a
  }

  members <- function(cls) {
    m <- state$members[[cls]]
    if (is.null(m)) {
      m <- lapply(call("java_members", cls, NULL), function(e) if (is.list(e)) unlist(e) else e)
      assign(cls, m, envir = state$members)
    }
    m
  }

  jobject <- function(id, cls, ui = FALSE) {
    e <- new.env(parent = emptyenv())
    e$id <- id
    e$cls <- cls
    e$ui <- ui
    # Collected proxies: the handle is released with the next call (not in the finalizer)
    if (!ui) reg.finalizer(e, function(x) state$released <- c(state$released, get("id", envir = x)))
    class(e) <- "bio7JavaObject"
    e
  }

  jclass <- function(name, ui = FALSE) {
    e <- new.env(parent = emptyenv())
    e$name <- name
    e$ui <- ui
    class(e) <- "bio7JavaClass"
    e
  }

  method <- function(target, cls, name, ui) {
    force(target); force(cls); force(name); force(ui)
    function(...) {
      r <- jcall("java_call", c(cls, name, if (ui) "ui" else ""), list(args = jargs(list(...)), target = target))
      # void (or null): no output of NULL in the console
      if (is.null(r)) invisible(NULL) else r
    }
  }

  env$J <- function(name) {
    "A Java class (Bio7, ImageJ, Java): its static methods and fields with $, a new
    object with $new(...); e.g. IJ <- J(\"ij.IJ\"); imp <- IJ$getImage()."
    jclass(call("java_type", name, NULL))
  }

  env$bio7_java_new <- function(class, ...) {
    "A new Java object (the constructor is chosen by the arguments)."
    jcall("java_new", class, list(args = jargs(list(...))))
  }

  env$bio7_java_ui <- function(x) {
    "The methods of a Java object or class called in the UI thread (SWT widgets)."
    if (inherits(x, "bio7JavaClass")) jclass(slot(x, "name"), ui = TRUE)
    else jobject(slot(x, "id"), slot(x, "cls"), ui = TRUE)
  }

  env$is_java <- function(x) inherits(x, "bio7JavaObject")

  env$`$.bio7JavaClass` <- function(x, name) {
    cls <- slot(x, "name")
    ui <- slot(x, "ui")
    if (name == "new") {
      return(function(...) jcall("java_new", c(cls, "", if (ui) "ui" else ""), list(args = jargs(list(...)))))
    }
    m <- members(cls)
    if (name %in% m$static_methods) return(method(NULL, cls, name, ui))
    if (name %in% m$static_fields) return(jcall("java_get", c(cls, name), NULL))
    if (name %in% m$classes) return(jclass(paste0(cls, "$", name)))
    stop("The Java class ", cls, " has no static method, field or class '", name, "'", call. = FALSE)
  }

  env$`$<-.bio7JavaClass` <- function(x, name, value) {
    jcall("java_set", c(slot(x, "name"), name), list(target = NULL, value = arg(value)))
    x
  }

  env$`$.bio7JavaObject` <- function(x, name) {
    cls <- slot(x, "cls")
    m <- members(cls)
    handle <- list(`__java__` = slot(x, "id"))
    if (name %in% m$methods) return(method(handle, cls, name, slot(x, "ui")))
    if (name %in% m$fields) return(jcall("java_get", c(cls, name), handle))
    stop("The Java object ", cls, " has no method or field '", name, "'", call. = FALSE)
  }

  env$`$<-.bio7JavaObject` <- function(x, name, value) {
    jcall("java_set", c(slot(x, "cls"), name), list(target = list(`__java__` = slot(x, "id")), value = arg(value)))
    x
  }

  to_string <- function(x) {
    jcall("java_call", c("java.lang.Object", "toString"),
          list(args = list(), target = list(`__java__` = slot(x, "id"))))
  }

  env$print.bio7JavaObject <- function(x, ...) {
    text <- to_string(x)
    if (nchar(text) > 200) text <- paste0(substr(text, 1, 200), "...")
    cat("<Java ", slot(x, "cls"), ": ", text, ">\n", sep = "")
    invisible(x)
  }

  env$format.bio7JavaObject <- function(x, ...) to_string(x)
  env$as.character.bio7JavaObject <- function(x, ...) to_string(x)

  env$print.bio7JavaClass <- function(x, ...) {
    cat("<Java class ", slot(x, "name"), ">\n", sep = "")
    invisible(x)
  }

  env$as.list.bio7JavaObject <- function(x, ...) {
    "The elements of an Iterable (List, Set, ...), the keys of a Map, an Iterator or an Enumeration."
    jcall("java_iter", character(), list(`__java__` = slot(x, "id")))
  }

  # The names after $ (completion)
  env$.DollarNames.bio7JavaObject <- function(x, pattern = "") {
    m <- members(slot(x, "cls"))
    grep(pattern, sort(unique(c(m$methods, m$fields))), value = TRUE)
  }

  env$.DollarNames.bio7JavaClass <- function(x, pattern = "") {
    m <- members(slot(x, "name"))
    grep(pattern, sort(unique(c("new", m$static_methods, m$static_fields, m$classes))), value = TRUE)
  }

  # The S3 methods registered: R (since 4.0) doesn't look for methods in the
  # attached environments (only the global environment, base and the registered ones)
  for (m in grep("\\.bio7Java(Class|Object)$", ls(env, all.names = TRUE), value = TRUE)) {
    cls <- sub("^.*\\.(bio7Java(Class|Object))$", "\\1", m)
    generic <- sub(paste0("\\.", cls, "$"), "", m)
    registerS3method(generic, cls, get(m, envir = env), envir = env)
  }

  # ------------------------------------------------------------------ 3D plots
  # The 3D plots of the 3D view (OpenGL, com.eco.bio7.spatial.Plot3D): the layers
  # with bio7_scatter3d(), bio7_surface3d(), ... shown with bio7_plot3d().

  plot3d_class <- function() env$J("com.eco.bio7.spatial.Plot3D")

  # A color (an R color name, "#RRGGBB" or a number 0xRRGGBB) as integer 0xRRGGBB
  rgb_int <- function(col) {
    if (is.numeric(col)) return(as.integer(col))
    m <- grDevices::col2rgb(col)
    as.integer(m[1, ] * 65536 + m[2, ] * 256 + m[3, ])
  }

  # A color map: "viridis", "rainbow", "diverging", "terrain" (of Bio7), the name of
  # an hcl palette of R (e.g. "Plasma", see hcl.pals()) or a vector of colors
  color_map <- function(map) {
    if (length(map) > 1) return(rgb_int(map))
    legend <- env$J("com.eco.bio7.spatial.PlotLegend")
    ramp <- switch(toupper(map),
                   VIRIDIS = legend$VIRIDIS, RAINBOW = legend$RAINBOW,
                   DIVERGING = legend$DIVERGING, TERRAIN = legend$TERRAIN, NULL)
    if (!is.null(ramp)) return(as.integer(ramp))
    rgb_int(grDevices::hcl.colors(7, map))
  }

  # The options of all layers
  plot3d_style <- function(layer, label, color, colormap, alpha) {
    if (!is.null(label)) layer <- layer$label(as.character(label))
    if (!is.null(color)) layer <- layer$color(rgb_int(color)[1])
    if (!is.null(colormap)) layer <- layer$colorMap(color_map(colormap))
    if (!is.null(alpha)) layer <- layer$alpha(as.numeric(alpha))
    layer
  }

  # A grid z[i, j] at x[i], y[j]: a matrix (also length(y) x length(x), transposed)
  # or a function f(x, y) (as outer())
  plot3d_grid <- function(x, y, z) {
    if (is.function(z)) z <- outer(x, y, z)
    z <- as.matrix(z)
    storage.mode(z) <- "double"
    if (nrow(z) != length(x) || ncol(z) != length(y)) {
      if (nrow(z) == length(y) && ncol(z) == length(x)) {
        z <- t(z)
      } else {
        stop("z must have length(x) rows and length(y) columns (z[i, j] at x[i], y[j])", call. = FALSE)
      }
    }
    z
  }

  # x, y and z of points: three vectors or a data frame / matrix with 3 columns in x
  plot3d_points <- function(x, y, z) {
    if (is.null(y) && is.null(z) && NCOL(x) >= 3) {
      y <- x[, 2]
      z <- x[, 3]
      x <- x[, 1]
    }
    list(x = as.numeric(x), y = as.numeric(y), z = as.numeric(z))
  }

  env$bio7_scatter3d <- function(x, y = NULL, z = NULL, label = NULL, color = NULL, size = NULL,
                                 values = NULL, colormap = NULL, alpha = NULL, drop_lines = FALSE) {
    "A scatter layer (spheres) of the points (x[i], y[i], z[i]) for bio7_plot3d(); x can be
    a data frame or matrix with 3 columns. size: the radius; values: colors the points with
    the colormap (and a color bar); drop_lines: lines down to the floor."
    p <- plot3d_points(x, y, z)
    l <- plot3d_class()$scatter(p$x, p$y, p$z)
    if (!is.null(size)) l <- l$size(as.numeric(size))
    if (!is.null(values)) l <- l$values(as.numeric(values))
    if (isTRUE(drop_lines)) l <- l$dropLines(TRUE)
    plot3d_style(l, label, color, if (is.null(colormap) && !is.null(values)) "viridis" else colormap, alpha)
  }

  env$bio7_line3d <- function(x, y = NULL, z = NULL, label = NULL, color = NULL, alpha = NULL, filled = FALSE) {
    "A line layer through the points (x[i], y[i], z[i]) for bio7_plot3d(); filled: filled
    down to the floor (e.g. several series at different y)."
    p <- plot3d_points(x, y, z)
    l <- if (isTRUE(filled)) plot3d_class()$filledLine(p$x, p$y, p$z) else plot3d_class()$line(p$x, p$y, p$z)
    plot3d_style(l, label, color, NULL, alpha)
  }

  env$bio7_surface3d <- function(x, y, z, label = NULL, colormap = "viridis", color = NULL, alpha = NULL, mesh = NULL) {
    "A surface layer of the grid z[i, j] at x[i], y[j] for bio7_plot3d(): z a matrix
    (as outer(x, y, f)) or a function f(x, y). mesh: the grid lines on the surface."
    l <- plot3d_class()$surface(as.numeric(x), as.numeric(y), plot3d_grid(x, y, z))
    if (!is.null(mesh)) l <- l$mesh(isTRUE(mesh))
    plot3d_style(l, label, color, if (is.null(color)) colormap else NULL, alpha)
  }

  env$bio7_wireframe3d <- function(x, y, z, label = NULL, color = NULL, colormap = NULL, alpha = NULL, step = NULL) {
    "A wireframe layer of the grid z[i, j] at x[i], y[j] for bio7_plot3d() (z a matrix or
    a function f(x, y)); step: every step-th grid line."
    l <- plot3d_class()$wireframe(as.numeric(x), as.numeric(y), plot3d_grid(x, y, z))
    if (!is.null(step)) l <- l$step(as.integer(step))
    plot3d_style(l, label, color, colormap, alpha)
  }

  env$bio7_bars3d <- function(x, y, z, label = NULL, color = NULL, colormap = NULL, alpha = NULL, width = NULL) {
    "A bar layer for bio7_plot3d(): z a vector (bars at the points (x[i], y[i])) or a grid
    z[i, j] at x[i], y[j] (e.g. a 2D histogram); width: the width of the bars (0..1)."
    l <- if (is.null(dim(z)) && !is.function(z) && length(z) == length(x)) {
      plot3d_class()$bars(as.numeric(x), as.numeric(y), as.numeric(z))
    } else {
      plot3d_class()$bars(as.numeric(x), as.numeric(y), plot3d_grid(x, y, z))
    }
    if (!is.null(width)) l <- l$width(as.numeric(width))
    plot3d_style(l, label, color, colormap, alpha)
  }

  env$bio7_contour3d <- function(x, y, z, levels = NULL, on_floor = FALSE, label = NULL, color = NULL,
                                 colormap = "viridis", alpha = NULL) {
    "The contour lines of the grid z[i, j] at x[i], y[j] for bio7_plot3d(); levels: the
    number of levels or the levels; on_floor: drawn on the floor of the box."
    l <- plot3d_class()$contour(as.numeric(x), as.numeric(y), plot3d_grid(x, y, z))
    if (length(levels) == 1) l <- l$levels(as.integer(levels))
    else if (length(levels) > 1) l <- l$levels(as.numeric(levels))
    if (isTRUE(on_floor)) l <- l$onFloor(TRUE)
    plot3d_style(l, label, color, if (is.null(color)) colormap else NULL, alpha)
  }

  env$bio7_pie3d <- function(values, labels = names(values), colors = NULL, explode = NULL, height = NULL) {
    "A 3D pie chart layer for bio7_plot3d() (shown alone)."
    if (is.null(labels)) labels <- paste("Value", seq_along(values))
    l <- plot3d_class()$pie(as.numeric(values), as.character(labels))
    if (!is.null(colors)) l <- l$colors(rgb_int(colors))
    if (!is.null(explode)) l <- l$explode(as.numeric(explode))
    if (!is.null(height)) l <- l$height(as.numeric(height))
    l
  }

  env$bio7_plot3d <- function(..., title = NULL, xlab = NULL, ylab = NULL, zlab = NULL, log = "",
                              xlim = NULL, ylim = NULL, zlim = NULL, aspect = NULL, legend = TRUE,
                              colorbar = NULL, font = NULL, html = NULL, show = TRUE, plot = NULL,
                              record = NULL) {
    "Shows the layers (bio7_scatter3d(), bio7_surface3d(), bio7_histogram3d(), ...) as 3D
    plot in the 3D view. log: the logarithmic axes (e.g. \"z\" or \"xy\"); xlim, ylim,
    zlim: the ranges (fixed ranges for a plot which is updated); aspect: the size of the
    box c(x, y, z); colorbar: the title of the color bar; html: also exported as HTML page
    (WebGL). Returns the plot (Java) invisibly.
    Dynamic plots: plot = the returned plot updates it with the new layers (in each step;
    the camera and the zoom are kept, the settings stay); record = a recorder of
    bio7_plot3d_recorder() adds the plot as frame (show = FALSE: only recorded)."
    layers <- list(...)
    if (length(layers) == 1 && is.list(layers[[1]]) && !inherits(layers[[1]], "bio7JavaObject")) layers <- layers[[1]]
    p <- if (is.null(plot)) plot3d_class()$new() else plot$clearLayers()
    if (!is.null(title)) p <- p$title(as.character(title))
    if (!is.null(xlab) || !is.null(ylab) || !is.null(zlab)) {
      p <- p$axisTitles(if (is.null(xlab)) "X" else xlab, if (is.null(ylab)) "Y" else ylab, if (is.null(zlab)) "Z" else zlab)
    }
    if (nzchar(log)) p <- p$logAxes(grepl("x", log), grepl("y", log), grepl("z", log))
    if (!is.null(xlim)) p <- p$xRange(as.numeric(xlim[1]), as.numeric(xlim[2]))
    if (!is.null(ylim)) p <- p$yRange(as.numeric(ylim[1]), as.numeric(ylim[2]))
    if (!is.null(zlim)) p <- p$zRange(as.numeric(zlim[1]), as.numeric(zlim[2]))
    if (!is.null(aspect)) p <- p$aspect(as.numeric(aspect[1]), as.numeric(aspect[2]), as.numeric(aspect[3]))
    if (!isTRUE(legend)) p <- p$legend(FALSE)
    if (!is.null(colorbar)) p <- p$colorBarTitle(as.character(colorbar))
    if (!is.null(font)) p <- p$font(as.character(font))
    for (l in layers) p <- p$add(l)
    if (!is.null(html)) p$exportHtml(normalizePath(html, winslash = "/", mustWork = FALSE))
    if (!is.null(record)) record$frame(p)
    if (isTRUE(show)) {
      if (is.null(plot)) p$show() else p$update()
    }
    invisible(p)
  }

  # ---- The charts (layers for bio7_plot3d(), also updated in each step)

  # A vector for a Java array, also of length 1 (else a single value without dim is
  # passed as scalar, e.g. a series with its first point)
  as_array <- function(v, mode = "numeric") {
    v <- if (mode == "numeric") as.numeric(v) else as.character(v)
    dim(v) <- length(v)
    v
  }

  # Groups of values: a vector (one group), a list or data frame (named: the names), a matrix (columns)
  plot3d_groups <- function(x) {
    if (is.matrix(x)) x <- as.data.frame(x)
    if (!is.list(x)) x <- list(x)
    lapply(unname(x), as_array)
  }

  plot3d_group_names <- function(x, names) {
    n <- if (!is.null(names)) names else if (is.matrix(x)) colnames(x) else if (is.list(x)) names(x) else NULL
    if (is.null(n)) NULL else as_array(n, "character")
  }

  # Series over x: a vector (one series), a matrix (columns), a data frame or list (named: the names)
  plot3d_series <- function(series) plot3d_groups(series)

  # The colors of the parts of a chart as integer vector (length 1: a vector for Java varargs)
  chart_colors <- function(layer, colors) {
    if (is.null(colors)) return(layer)
    layer$colors(rgb_int(colors))
  }

  env$bio7_histogram3d <- function(x, names = NULL, bins = NULL, bin_width = NULL, density = FALSE,
                                   colors = NULL, label = NULL, alpha = NULL) {
    "A histogram layer for bio7_plot3d(): x a vector or groups (a list or data frame, the
    groups in the depth with the names on the y axis). bins: the number of bins or
    bin_width; density: densities instead of counts."
    g <- plot3d_groups(x)
    n <- plot3d_group_names(x, names)
    l <- if (length(g) == 1) plot3d_class()$histogram(g[[1]])
         else plot3d_class()$histogram(g, if (is.null(n)) as_array(paste("Group", seq_along(g)), "character") else n)
    if (!is.null(bins)) l <- l$bins(as.integer(bins))
    if (!is.null(bin_width)) l <- l$binWidth(as.numeric(bin_width))
    if (isTRUE(density)) l <- l$density(TRUE)
    plot3d_style(chart_colors(l, colors), label, NULL, NULL, alpha)
  }

  env$bio7_linegraph3d <- function(x, series, names = NULL, colors = NULL, markers = TRUE,
                                   marker_size = 0.12, fill = FALSE, label = NULL, alpha = NULL) {
    "A line graph layer for bio7_plot3d(): the series over x (a vector, a matrix with a
    series in each column, a data frame or a list), each series at its own depth.
    markers: points at the values; fill: filled down to the floor."
    s <- plot3d_series(series)
    n <- plot3d_group_names(series, names)
    l <- plot3d_class()$lineGraph(as_array(x), s)
    if (!is.null(n) && length(n) == length(s)) l <- l$names(n)
    l <- l$markers(isTRUE(markers), as.numeric(marker_size))
    if (isTRUE(fill)) l <- l$fill(TRUE)
    plot3d_style(chart_colors(l, colors), label, NULL, NULL, alpha)
  }

  env$bio7_area3d <- function(x, series, names = NULL, colors = NULL, stacked = TRUE, label = NULL, alpha = NULL) {
    "An area chart layer for bio7_plot3d(): the series over x (as bio7_linegraph3d()),
    stacked (or each in its own depth: stacked = FALSE)."
    s <- plot3d_series(series)
    n <- plot3d_group_names(series, names)
    l <- plot3d_class()$area(as_array(x), s)
    if (!is.null(n) && length(n) == length(s)) l <- l$names(n)
    if (!isTRUE(stacked)) l <- l$stacked(FALSE)
    plot3d_style(chart_colors(l, colors), label, NULL, NULL, alpha)
  }

  env$bio7_boxplot3d <- function(groups, names = NULL, colors = NULL, width = NULL, outliers = TRUE,
                                 label = NULL, alpha = NULL) {
    "A box plot layer for bio7_plot3d(): the groups (a list, a data frame or a matrix with
    a group in each column; the names on the x axis)."
    g <- plot3d_groups(groups)
    n <- plot3d_group_names(groups, names)
    l <- plot3d_class()$boxplot(g, if (is.null(n)) as_array(paste("Group", seq_along(g)), "character") else n)
    if (!is.null(width)) l <- l$width(as.numeric(width))
    if (!isTRUE(outliers)) l <- l$outliers(FALSE)
    plot3d_style(chart_colors(l, colors), label, NULL, NULL, alpha)
  }

  env$bio7_violin3d <- function(groups, names = NULL, colors = NULL, width = NULL, bandwidth = NULL,
                                quartiles = TRUE, label = NULL, alpha = NULL) {
    "A violin plot layer for bio7_plot3d(): the kernel densities of the groups (as
    bio7_boxplot3d()); quartiles: the median and the quartiles."
    g <- plot3d_groups(groups)
    n <- plot3d_group_names(groups, names)
    l <- plot3d_class()$violin(g, if (is.null(n)) as_array(paste("Group", seq_along(g)), "character") else n)
    if (!is.null(width)) l <- l$width(as.numeric(width))
    if (!is.null(bandwidth)) l <- l$bandwidth(as.numeric(bandwidth))
    if (!isTRUE(quartiles)) l <- l$quartiles(FALSE)
    plot3d_style(chart_colors(l, colors), label, NULL, NULL, alpha)
  }

  env$bio7_radar3d <- function(series, axes = colnames(series), names = rownames(series), colors = NULL,
                               max = NULL, rings = NULL, label = NULL, alpha = NULL) {
    "A radar chart layer for bio7_plot3d(): a matrix or data frame with a series in each
    row and an axis in each column (the axes: the column names, the series: the row names)."
    m <- as.matrix(series)
    storage.mode(m) <- "double"
    if (is.null(axes)) axes <- paste("Axis", seq_len(ncol(m)))
    l <- plot3d_class()$radar(unname(m), as.character(axes))
    if (!is.null(names) && length(names) == nrow(m)) l <- l$names(as.character(names))
    if (!is.null(max)) l <- l$max(as.numeric(max))
    if (!is.null(rings)) l <- l$rings(as.integer(rings))
    plot3d_style(chart_colors(l, colors), label, NULL, NULL, alpha)
  }

  env$bio7_rose3d <- function(values, sectors = NULL, classes = NULL, colors = NULL, area_proportional = FALSE,
                              height = NULL, gap = NULL, label = NULL, alpha = NULL) {
    "A rose chart layer for bio7_plot3d() (e.g. a wind rose): a value for each sector, or a
    matrix values[sector, class] stacked from the center (the classes: the column names)."
    if (is.matrix(values) || is.data.frame(values)) {
      m <- as.matrix(values)
      storage.mode(m) <- "double"
      if (is.null(sectors)) sectors <- rownames(m)
      if (is.null(classes)) classes <- colnames(m)
      if (is.null(sectors)) sectors <- paste("Sector", seq_len(nrow(m)))
      l <- plot3d_class()$rose(unname(m), as.character(sectors))
      if (!is.null(classes)) l <- l$classes(as.character(classes))
    } else {
      if (is.null(sectors)) sectors <- if (is.null(names(values))) paste("Sector", seq_along(values)) else names(values)
      l <- plot3d_class()$rose(as.numeric(values), as.character(sectors))
    }
    if (isTRUE(area_proportional)) l <- l$areaProportional(TRUE)
    if (!is.null(height) || !is.null(gap)) {
      l <- l$shape(if (is.null(height)) 1 else as.numeric(height), if (is.null(gap)) 0 else as.numeric(gap))
    }
    plot3d_style(chart_colors(l, colors), label, NULL, NULL, alpha)
  }

  env$bio7_treemap3d <- function(values, labels = names(values), colors = NULL, heights = FALSE,
                                 show_values = TRUE, label = NULL, alpha = NULL) {
    "A treemap layer for bio7_plot3d(): rectangles with the areas of the values (e.g. of a
    table()); heights: the values also as heights."
    if (is.null(labels)) labels <- paste("Value", seq_along(values))
    l <- plot3d_class()$treemap(as.numeric(values), as.character(labels))
    if (isTRUE(heights)) l <- l$heights(TRUE)
    if (!isTRUE(show_values)) l <- l$showValues(FALSE)
    plot3d_style(chart_colors(l, colors), label, NULL, NULL, alpha)
  }

  # ---- Recording (an animation of a plot, e.g. of each step of a simulation)

  env$bio7_plot3d_recorder <- function(speed = 10, loop = TRUE, autoplay = TRUE, max_frames = NULL) {
    "A recorder of plots: bio7_plot3d(..., record = rec) adds a frame (also without the 3D
    view: show = FALSE), bio7_plot3d_export(rec, file) writes the animation as HTML page
    (WebGL with Play/Pause, a timeline, the speed). speed: frames per second."
    r <- env$J("com.eco.bio7.spatial.PlotRecorder")$new()$speed(as.numeric(speed))$loop(isTRUE(loop))$autoplay(isTRUE(autoplay))
    if (!is.null(max_frames)) r <- r$maxFrames(as.integer(max_frames))
    r
  }

  env$bio7_plot3d_export <- function(recorder, file, blender = FALSE, open = TRUE) {
    "Exports the frames of the recorder as HTML page (WebGL animation) and opens it in the
    browser view (open = TRUE). blender: also as animation for Blender (a .glb file and a
    Python script beside the page). Returns the path invisibly."
    file <- normalizePath(file, winslash = "/", mustWork = FALSE)
    recorder$export(file)
    if (isTRUE(blender)) recorder$exportBlender(sub("\\.html?$", ".glb", file, ignore.case = TRUE))
    if (isTRUE(open)) env$bio7_browse(file)
    invisible(file)
  }

  env$bio7_plot3d_clear <- function() {
    "Removes the 3D plot from the 3D view."
    invisible(plot3d_class()$clear())
  }

  # ------------------------------------------------------------------ Overview

  env$bio7_api <- function() {
    cat(
"The API of Bio7 for R (the attached environment \"bio7\"):

  img <- bio7_image()                    # the current ImageJ image as matrix (height x width)
  bio7_show_image(img * 2, \"Twice\")      # a new ImageJ image (range = c(min, max) or \"auto\")
  bio7_ij_macro('run(\"Blobs (25K)\");')   # an ImageJ macro (also bio7_ij_macro_file, bio7_ij_run)
  df <- bio7_results_table()             # the ImageJ results table as data frame
  r <- bio7_roi()                        # the coordinates of the current selection (r$x, r$y)
  bio7_browse(\"https://www.r-project.org\") # a URL or HTML file in the browser view
  x <- bio7_python(\"np.arange(5) * 2\")   # Python code: the value of the last expression
  bio7_to_python(\"df\", iris)             # assign in Python (a data frame as pandas DataFrame)

  IJ <- J(\"ij.IJ\")                       # the Java API: a class (static methods and fields with $)
  imp <- IJ$getImage()                   # a Java object (methods and fields with $)
  imp$getTitle()
  roi <- J(\"ij.gui.OvalRoi\")$new(10, 10, 50, 50)   # constructors (also bio7_java_new)
  bio7_java_ui(obj)$setText(\"Title\")     # methods in the UI thread (SWT widgets)
  as.list(J(\"java.util.List\")$of(1, 2))  # the elements of a collection

  bio7_plot3d(bio7_scatter3d(iris[, 1:3], label = \"Iris\"), title = \"Scatter\")   # 3D plots in the 3D view
  x <- seq(-3, 3, length.out = 50)
  bio7_plot3d(bio7_surface3d(x, x, function(a, b) sin(a) * cos(b)), colorbar = \"z\")
  # also bio7_line3d, bio7_wireframe3d, bio7_bars3d, bio7_contour3d, bio7_pie3d, bio7_plot3d_clear
  # charts: bio7_histogram3d, bio7_linegraph3d, bio7_area3d, bio7_boxplot3d, bio7_violin3d,
  # bio7_radar3d, bio7_rose3d, bio7_treemap3d
  p <- bio7_plot3d(bio7_histogram3d(rnorm(500)), zlim = c(0, 80))      # dynamic: in each step
  bio7_plot3d(bio7_histogram3d(rnorm(500)), plot = p)                  # updated (camera kept)
  rec <- bio7_plot3d_recorder(speed = 20)                              # recorded as animation
  bio7_plot3d(bio7_histogram3d(rnorm(500)), record = rec, show = FALSE)
  bio7_plot3d_export(rec, file.path(tempdir(), \"animation.html\"))
", sep = "")
    invisible(NULL)
  }
})
