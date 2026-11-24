# Copyright (c) 2007-2026 M. Austenfeld
# SPDX-License-Identifier: EPL-2.0
#
# The simulation API of Bio7 for R. A model is an environment created with
# grid_model(), value_grid_model(), agent_model(), geo_grid_model() or
# geo_agent_model(). Its functions init and step create the start state and
# one time step; settings are added with m$param(...), buttons with
# m$action(...). The last line starts the model with simulation(m):
#
#   m <- grid_model("Life")
#   m$param("density", 0.3, min = 0, max = 1, step = 0.01, label = "Start density", restart = TRUE)
#   m$init <- function(m) {
#     m$size(100, 100)
#     m$palette("#ffffff", "#1f3a93")
#     m$cells[] <- as.integer(m$random(length(m$cells)) < m$density)
#   }
#   m$step <- function(m) {
#     n <- m$count_neighbors(1)
#     m$cells[] <- as.integer(n == 3 | (m$cells == 1 & n == 2))
#     m$chart("Living cells", m$count(1))
#   }
#   simulation(m)
#
# In Bio7 the model runs with the Play/Pause button (a grid in ImageJ, agents in
# the 3D view, geospatial models on the WorldWind globe). "Export Simulation as
# HTML..." writes a page which runs the same script in the web browser (webR, R
# in WebAssembly) with the same results (the random numbers of R with the same
# seed). The grids are matrices m$cells[y, x] (rows y = 1..height from the top,
# columns x = 1..width), the agents data frames.

local({
  browser <- exists(".bio7sim_browser", envir = globalenv()) && isTRUE(get(".bio7sim_browser", envir = globalenv()))
  if ("bio7sim" %in% search()) detach("bio7sim", character.only = TRUE)
  api <- attach(NULL, name = "bio7sim", warn.conflicts = FALSE)

  VIRIDIS <- c("#440154", "#3b528b", "#21908c", "#5dc963", "#fde725")
  COS <- c(1.0, -0.5, 0.041666666666666664, -0.001388888888888889, 2.48015873015873e-05, -2.755731922398589e-07,
           2.08767569878681e-09, -1.1470745597729725e-11, 4.779477332387385e-14, -1.5619206968586225e-16,
           4.110317623312165e-19, -8.896791392450574e-22)
  MERCATOR <- 20037508.342789244
  N8 <- list(c(-1, -1), c(-1, 0), c(-1, 1), c(0, -1), c(0, 1), c(1, -1), c(1, 0), c(1, 1))
  N4 <- list(c(-1, 0), c(1, 0), c(0, -1), c(0, 1))

  java <- function(op, args = character(), value = NULL) {
    if (browser) stop("load() and its functions (grid, raster, vector) run only in Bio7: the page gets the loaded layers")
    call <- get(".bio7_call", envir = as.environment("bio7ffm"))
    call("sim_geo", c(op, as.character(args)), value)
  }

  # ------------------------------------------------------------ functions

  api$clamp <- function(value, min, max) pmax(min, pmin(max, value))
  api$lerp <- function(a, b, t) a + (b - a) * t
  api$map_range <- function(value, in_min, in_max, out_min, out_max) out_min + (value - in_min) * (out_max - out_min) / (in_max - in_min)
  api$wrap_around <- function(value, size) ((value %% size) + size) %% size
  api$sq <- function(value) value * value
  api$distance <- function(x1, y1, x2, y2) sqrt((x2 - x1)^2 + (y2 - y1)^2)
  api$distance3 <- function(x1, y1, z1, x2, y2, z2) sqrt((x2 - x1)^2 + (y2 - y1)^2 + (z2 - z1)^2)
  api$logistic <- function(x) 1 / (1 + exp(-x))
  api$logistic_growth <- function(n, r, k) r * n * (1 - n / k)
  api$rk4 <- function(f, t, y, dt) {
    "One step of the Runge-Kutta method: f(t, y) returns the derivatives of y."
    k1 <- f(t, y); k2 <- f(t + dt / 2, y + dt / 2 * k1); k3 <- f(t + dt / 2, y + dt / 2 * k2); k4 <- f(t + dt, y + dt * k3)
    y + dt / 6 * (k1 + 2 * k2 + 2 * k3 + k4)
  }
  api$cos_lat <- function(lat_deg) {
    "The cosine of a latitude in degrees (the same digits in Bio7 and in the browser)."
    x <- lat_deg * (pi / 180); x2 <- x * x; c <- COS[12]
    for (k in 11:1) c <- c * x2 + COS[k]
    c
  }
  api$cos_deg <- function(deg) {
    d <- deg %% 360
    d <- ifelse(d > 180, d - 360, d)
    d <- abs(d)
    ifelse(d <= 90, api$cos_lat(d), -api$cos_lat(180 - d))
  }
  api$sin_deg <- function(deg) api$cos_deg(90 - deg)
  api$VIRIDIS <- VIRIDIS

  # the noise (the same values as noise of Java: unsigned 32 bit arithmetic with doubles)
  u32 <- function(x) x %% 4294967296
  mulmod <- function(a, b) { ah <- a %/% 65536; al <- a %% 65536; u32(((ah * b) %% 65536) * 65536 + al * b) }
  xor32 <- function(a, b) bitwXor(a %/% 65536, b %/% 65536) * 65536 + bitwXor(a %% 65536, b %% 65536)
  corner <- function(cx, cy) {
    h <- u32(mulmod(u32(cx), 374761393) + mulmod(u32(cy), 668265263))
    h <- mulmod(xor32(h, h %/% 8192), 1274126177)
    h <- xor32(h, h %/% 65536)
    (h %% 16777216) / 16777216
  }
  api$noise <- function(x, y) {
    "A smooth noise (0 - 1) at x, y (value noise, vectorized; the same values as noise of Java)."
    ix <- floor(x); iy <- floor(y); fx <- x - ix; fy <- y - iy
    sx <- fx * fx * (3 - 2 * fx); sy <- fy * fy * (3 - 2 * fy)
    a <- corner(ix, iy); b <- corner(ix + 1, iy); c <- corner(ix, iy + 1); d <- corner(ix + 1, iy + 1)
    a + (b - a) * sx + (c - a) * sy + (a - b - c + d) * sx * sy
  }
  api$fractal_noise <- function(x, y, octaves = 4) {
    s <- 0; total <- 0; amplitude <- 1; f <- 1
    for (k in seq_len(octaves) - 1) { s <- s + amplitude * api$noise(x * f + k * 17.3, y * f + k * 11.1); total <- total + amplitude; amplitude <- amplitude / 2; f <- f * 2 }
    s / total
  }
  api$heading_of <- function(dx, dy) {
    "The heading of a movement in degrees: 0 the direction of y, clockwise (in a grid with y down: heading_of(dx, -dy))."
    h <- atan2(dx, dy) * 180 / pi
    ifelse(h < 0, h + 360, h)
  }
  api$linear_regression <- function(x, y) {
    "The linear regression y = a + b x: c(intercept, slope, r2)."
    mx <- mean(x); my <- mean(y); sxy <- sum((x - mx) * (y - my)); sxx <- sum((x - mx)^2); syy <- sum((y - my)^2)
    b <- if (sxx == 0) 0 else sxy / sxx
    c(intercept = my - b * mx, slope = b, r2 = if (sxx == 0 || syy == 0) 0 else sxy^2 / (sxx * syy))
  }
  api$normal_pdf <- function(x, mean = 0, sd = 1) dnorm(x, mean, sd)
  api$normal_cdf <- function(x, mean = 0, sd = 1) pnorm(x, mean, sd)

  # s[y, x] = a[y + dy, x + dx] (0 outside without wrap)
  shift <- function(a, dy, dx, wrap) {
    h <- nrow(a); w <- ncol(a)
    rows <- seq_len(h) + dy; cols <- seq_len(w) + dx
    if (wrap) return(a[((rows - 1) %% h) + 1, ((cols - 1) %% w) + 1, drop = FALSE])
    s <- a[pmin(pmax(rows, 1), h), pmin(pmax(cols, 1), w), drop = FALSE]
    s[rows < 1 | rows > h, ] <- 0
    s[, cols < 1 | cols > w] <- 0
    s
  }

  words <- function(name) {
    s <- gsub("([a-z0-9])([A-Z])", "\\1 \\2", gsub("_", " ", name))
    paste0(toupper(substring(s, 1, 1)), substring(s, 2))
  }

  # ------------------------------------------------------------ the model

  model <- function(kind, name, description) {
    m <- new.env()
    m$.kind <- kind; m$.name <- name; m$.description <- description
    m$.params <- list(); m$.actions <- list(); m$.charts <- list(); m$.more_charts <- list(); m$.drawing <- list(); m$.records <- list()
    m$.stopped <- FALSE
    m$cos_deg <- api$cos_deg; m$sin_deg <- api$sin_deg
    m$t <- 0
    m$init <- function(m) NULL
    m$step <- function(m) NULL

    m$param <- function(name, value, min = NULL, max = NULL, step = NULL, label = NULL, options = NULL,
                        restart = FALSE, group = "", type = NULL) {
      "A setting: a slider (min, max, step), a number, a checkbox (TRUE/FALSE), a choice (options) or a text.
      type: \"int\" or \"double\" (else whole numbers with whole min, max and step are \"int\")."
      kind <- if (!is.null(type)) type
              else if (is.logical(value)) "boolean"
              else if (is.character(value)) "string"
              else if (is.integer(value) || all(c(value, min, max, step) == round(c(value, min, max, step))) && !is.null(min)) "int"
              else "double"
      m$.params[[name]] <- list(name = name, label = if (is.null(label)) words(name) else label, kind = kind, value = value,
                                min = min, max = max, step = step, options = options, restart = restart, group = group)
      assign(name, value, envir = m)
      invisible(m)
    }
    m$action <- function(name, fun, label = NULL, group = "") {
      "A button: fun(m) is called when it is pressed."
      m$.actions[[name]] <- list(name = name, label = if (is.null(label)) words(name) else label, group = group, fun = fun)
      invisible(m)
    }

    # random numbers (the generator of R: the same numbers in Bio7 and in the browser)
    m$.seed_value <- 42
    m$seed <- function(n) { m$.seed_value <- n; set.seed(n, kind = "Mersenne-Twister", normal.kind = "Inversion", sample.kind = "Rejection") }
    m$random_at <- function(index, k = 0) {
      "A random number in [0, 1) for an index (e.g., of a cell or an agent; also a vector) and the number k of the draw: always the same for the seed, the step and the index (independent of the order of the calls; the same numbers as randomAt in Java and random_at in Python for the same index)."
      # 32-bit arithmetic as in Java with doubles: the products from 16-bit halves (exact)
      u <- function(v) as.numeric(v) %% 4294967296
      mul <- function(a, b) { ah <- a %/% 65536; al <- a %% 65536; bh <- b %/% 65536; bl <- b %% 65536; ((ah * bl + al * bh) %% 65536 * 65536 + al * bl) %% 4294967296 }
      xor <- function(a, b) bitwXor(as.integer(a %/% 65536), as.integer(b %/% 65536)) * 65536 + bitwXor(as.integer(a %% 65536), as.integer(b %% 65536))
      shr <- function(a, n) a %/% 2^n
      h <- xor(xor(mul(u(m$.seed_value), 2654435761), mul(u(m$t), 2246822519)), xor(mul(u(index), 3266489917), mul(u(k), 668265263)))
      h <- xor(h, shr(h, 16)); h <- mul(h, 2146121005)
      h <- xor(h, shr(h, 15)); h <- mul(h, 2221713035)
      h <- xor(h, shr(h, 16))
      h / 4294967296
    }
    m$random <- function(n = 1, min = 0, max = 1) runif(n, min, max)
    m$random_int <- function(min, max, n = 1) {
      "Random integers from min to max (both included)."
      min + sample.int(max - min + 1, n, replace = TRUE) - 1
    }
    m$chance <- function(p, n = 1) runif(n) < p
    m$random_gaussian <- function(n = 1, mean = 0, sd = 1) rnorm(n, mean, sd)
    m$random_exponential <- function(rate, n = 1) rexp(n, rate)
    m$random_poisson <- function(lambda, n = 1) rpois(n, lambda)
    m$random_element <- function(x) x[[sample.int(length(x), 1)]]
    m$shuffle <- function(x) x[sample.int(length(x))]
    m$random_binomial <- function(size, prob, n = 1) rbinom(n, size, prob)
    m$random_weighted <- function(weights) {
      "A random index chosen with the weights (NA if all are 0)."
      w <- pmax(weights, 0)
      if (sum(w) <= 0) return(NA_integer_)
      sample.int(length(w), 1, prob = w)
    }
    m$chart <- function(name, value, chart = NULL) {
      "A value of a chart series (one value per step); chart: the title of another chart (a second chart)."
      if (is.null(chart)) m$.charts[[length(m$.charts) + 1]] <- list(as.character(name), as.numeric(value))
      else m$.more_charts[[length(m$.more_charts) + 1]] <- list(t = as.character(chart), k = "line", n = as.character(name), x = m$t, y = as.numeric(value))
      invisible(NULL)
    }
    m$is_stopped <- function() {
      "TRUE if m$stop() was called in this step (or in m$init)."
      isTRUE(m$.stopped)
    }
    m$stop <- function() {
      "Ends the simulation after this step, e.g. if (infected == 0) m$stop() in m$step: Bio7 pauses the simulation (Play continues it), the page stops, an experiment ends the run."
      m$.stopped <- TRUE
      invisible(NULL)
    }
    m$record <- function(name, value) {
      "Records a value of the current step for an experiment (a column of its table, besides the values of the line charts)."
      m$.records[[as.character(name)]] <- as.numeric(value)
      invisible(NULL)
    }
    m$chart_xy <- function(chart, name, x, y) {
      "A point (x, y) of a series of an XY chart (e.g., a phase plot)."
      m$.more_charts[[length(m$.more_charts) + 1]] <- list(t = as.character(chart), k = "xy", n = as.character(name), x = as.numeric(x), y = as.numeric(y))
      invisible(NULL)
    }
    m$chart_bars <- function(chart, labels, values) {
      "A bar chart (replaced in each call, e.g., a histogram)."
      m$.more_charts[[length(m$.more_charts) + 1]] <- list(t = as.character(chart), k = "bars", l = as.list(as.character(labels)), v = as.list(as.numeric(values)))
      invisible(NULL)
    }
    # drawing (in the coordinates of the model: cells, the box, longitude and latitude)
    shape <- function(k, p, s = NULL, f = NULL, t = NULL, w = 1, a = 0, z = 0) {
      d <- list(k = k, p = as.list(as.numeric(p)), s = s, f = f, t = t, w = as.numeric(w), a = as.numeric(a))
      if (!is.null(z) && z != 0) d$z <- as.numeric(z)
      m$.drawing[[length(m$.drawing) + 1]] <- d
      invisible(NULL)
    }
    m$draw_line <- function(x1, y1, x2, y2, color = "#000000", width = 1, z = 0) shape("line", c(x1, y1, x2, y2), color, NULL, NULL, width, z = z)
    m$draw_line3 <- function(x1, y1, z1, x2, y2, z2, color = "#000000", width = 1) shape("line3", c(x1, y1, z1, x2, y2, z2), color, NULL, NULL, width)
    m$draw_path <- function(xs, ys, color = "#000000", width = 1, z = 0) shape("path", as.vector(rbind(xs, ys)), color, NULL, NULL, width, z = z)
    m$draw_polygon <- function(xs, ys, fill = NULL, stroke = "#000000", z = 0) shape("polygon", as.vector(rbind(xs, ys)), stroke, fill, z = z)
    m$draw_circle <- function(x, y, radius, fill = NULL, stroke = "#000000", z = 0) shape("circle", c(x, y, radius), stroke, fill, z = z)
    m$draw_rect <- function(x, y, w, h, fill = NULL, stroke = "#000000", z = 0) shape("rect", c(x, y, w, h), stroke, fill, z = z)
    m$draw_text <- function(x, y, text, color = "#000000", size = 1, z = 0) {
      "A text; z: the height above the floor (an agent model, e.g. a label above an object)."
      shape("text", c(x, y), color, NULL, as.character(text), size, z = z)
    }
    m$draw_image <- function(name, x, y, size = 1, angle = 0, z = 0) {
      "An image (m$load_image) centered at x, y: the size and the angle (degrees, clockwise); z: the height above the floor (an agent model)."
      shape("image", c(x, y), NULL, NULL, name, size, angle, z = z)
    }
    # 3D shapes of an agent model (x, y, z of the box; lit solids)
    m$draw_point3 <- function(x, y, z, size = 1, color = "#e53935") {
      "A point in 3D: a small sphere with the diameter size, e.g. a marker."
      shape("point3", c(x, y, z, size), NULL, color)
    }
    m$draw_path3 <- function(xs, ys, zs, color = "#000000", width = 1) {
      "A line through the points in 3D (the width in pixels), e.g. a flight path."
      n <- min(length(xs), length(ys), length(zs))
      shape("path3", as.vector(rbind(xs[seq_len(n)], ys[seq_len(n)], zs[seq_len(n)])), color, NULL, NULL, width)
    }
    m$draw_box <- function(x, y, z, w, h, d, fill = "#b0b8c4", stroke = NULL) {
      "A box in 3D: the corner x, y, z (the lowest values), the width (x), the height (y) and the depth (z, upwards) as in m$box(); fill: the sides, stroke: the edges (NULL: none)."
      shape("box", c(x, y, z, w, h, d), stroke, fill)
    }
    m$draw_sphere <- function(x, y, z, r, fill = "#d0d4dc") {
      "A sphere in 3D: the center and the radius, e.g. a balloon or a tree crown."
      shape("sphere", c(x, y, z, r), NULL, fill)
    }
    m$draw_polygon3 <- function(xs, ys, zs, fill = "#b0b8c4", stroke = NULL) {
      "A flat polygon in 3D (the points in a plane): the fill and the outline (NULL: none), e.g. a roof."
      n <- min(length(xs), length(ys), length(zs))
      shape("polygon3", as.vector(rbind(xs[seq_len(n)], ys[seq_len(n)], zs[seq_len(n)])), stroke, fill)
    }
    m$draw_pillar <- function(x, y, radius, height = NULL, color = "#9aa6b8") {
      "A pillar in 3D (an agent model): a vertical cylinder from the floor at x, y with the radius and the height (NULL: the height of the box), e.g. an obstacle."
      if (is.null(height)) height <- if (is.null(m$box_depth)) 1 else m$box_depth
      shape("pillar", c(x, y, radius, height), NULL, color)
    }
    # images and 3D models
    m$load_image <- function(name, file) {
      "Loads an image (PNG with transparency) for m$draw_image and the agents (a column image)."
      if (!browser) get(".bio7_call", envir = as.environment("bio7ffm"))("sim_asset", c("image", name, file), NULL)
      invisible(m)
    }
    m$load_model <- function(name, file) {
      "Loads a 3D model (glTF .glb or .gltf, also animated; OBJ with .mtl and textures; STL) for the agents (a column model) and m$place_model."
      if (!browser) get(".bio7_call", envir = as.environment("bio7ffm"))("sim_asset", c("model", name, file), NULL)
      invisible(m)
    }
    for (f in c("clamp", "lerp", "map_range", "wrap_around", "sq", "distance", "distance3", "logistic", "logistic_growth", "rk4",
                "noise", "fractal_noise", "heading_of", "linear_regression", "normal_pdf", "normal_cdf"))
      assign(f, get(f, envir = api), envir = m)
    m$.prepare <- function() NULL
    m$.state <- function() list()
    m
  }

  # The agents of a grid (turtles): the rows of the data frame m$agents (x, y in cells from 0 as the shapes,
  # heading, color, image, size, shape and own columns), drawn on top of the grid
  grid_agents <- function(m) {
    empty <- function() data.frame(x = numeric(), y = numeric(), heading = numeric(), color = character(), image = character(),
                                    size = numeric(), shape = character(), stringsAsFactors = FALSE)
    m$agents <- empty()
    m$.reset_agents <- function() m$agents <- empty()
    keep <- function(a) {
      if (isTRUE(m$wrap)) { a$x <- a$x %% m$width; a$y <- a$y %% m$height }
      else { a$x <- pmin(pmax(a$x, 0), m$width - 1e-9); a$y <- pmin(pmax(a$y, 0), m$height - 1e-9) }
      a
    }
    m$add_agents <- function(n = 1, x = NULL, y = NULL, heading = 0, color = "#e53935", image = NA, size = 0.8, shape = "circle", ...) {
      "Adds n agents (rows of m$agents): x, y in cells from 0 (the centre of m$cells[j, i] is i - 0.5, j - 0.5; default: random), the heading (degrees, 0 up, clockwise), the color, an image (m$load_image), the size (cells), the shape ('circle', 'arrow' or 'square') and own columns (...). Returns the new rows."
      if (is.null(x)) x <- m$random(n, 0, m$width)
      if (is.null(y)) y <- m$random(n, 0, m$height)
      new <- data.frame(x = rep_len(as.numeric(x), n), y = rep_len(as.numeric(y), n), heading = rep_len(as.numeric(heading), n),
                        color = rep_len(as.character(color), n), image = rep_len(as.character(image), n), size = rep_len(as.numeric(size), n),
                        shape = rep_len(as.character(shape), n), ..., stringsAsFactors = FALSE)
      old <- m$agents
      for (cn in setdiff(names(new), names(old))) old[[cn]] <- if (nrow(old)) NA else new[[cn]][0]
      for (cn in setdiff(names(old), names(new))) new[[cn]] <- NA
      m$agents <- rbind(old, new[names(old)])
      invisible(seq_len(n) + nrow(old))
    }
    m$remove_agents <- function(rows) {
      "Removes the agents of the rows."
      if (length(rows)) { m$agents <- m$agents[!(seq_len(nrow(m$agents)) %in% rows), , drop = FALSE]; rownames(m$agents) <- NULL }
      invisible(m)
    }
    m$forward <- function(rows, distance) {
      "Moves the agents of the rows in their heading (0 up, clockwise; a torus or they stop at the edge: m$wrap)."
      a <- m$agents[rows, c("x", "y", "heading")]
      a$x <- a$x + distance * api$sin_deg(a$heading); a$y <- a$y - distance * api$cos_deg(a$heading)
      a <- keep(a)
      m$agents$x[rows] <- a$x; m$agents$y[rows] <- a$y
      invisible(m)
    }
    m$move_to <- function(rows, x, y) {
      a <- keep(data.frame(x = rep_len(as.numeric(x), length(rows)), y = rep_len(as.numeric(y), length(rows))))
      m$agents$x[rows] <- a$x; m$agents$y[rows] <- a$y
      invisible(m)
    }
    delta <- function(dx, dy) {
      if (isTRUE(m$wrap)) { dx <- dx - m$width * round(dx / m$width); dy <- dy - m$height * round(dy / m$height) }
      list(dx = dx, dy = dy)
    }
    m$face <- function(rows, x, y) {
      "Turns the agents of the rows to the point x, y (cells from 0; the shortest way on a torus)."
      d <- delta(x - m$agents$x[rows], y - m$agents$y[rows])
      turn <- d$dx != 0 | d$dy != 0
      m$agents$heading[rows[turn]] <- api$heading_of(d$dx[turn], -d$dy[turn])
      invisible(m)
    }
    m$agent_distance <- function(i, j) {
      "The distances of the agents of the rows i and j (cells; the shortest way on a torus)."
      d <- delta(m$agents$x[j] - m$agents$x[i], m$agents$y[j] - m$agents$y[i])
      sqrt(d$dx^2 + d$dy^2)
    }
    m$cell_x <- function(rows = seq_len(nrow(m$agents))) floor(m$agents$x[rows]) + 1
    m$cell_y <- function(rows = seq_len(nrow(m$agents))) floor(m$agents$y[rows]) + 1
    m$agents_at <- function(x, y) {
      "The rows of the agents in the cell m$cells[y, x] (from 1)."
      which(floor(m$agents$x) + 1 == x & floor(m$agents$y) + 1 == y)
    }
    m$count_agents_at <- function(x, y) length(m$agents_at(x, y))
    m$agent_counts <- function(rows = seq_len(nrow(m$agents))) {
      "The number of the agents (of the rows) in each cell: a matrix as m$cells."
      cnt <- matrix(0L, m$height, m$width)
      if (length(rows)) {
        t <- table(factor(floor(m$agents$y[rows]) + 1, levels = seq_len(m$height)), factor(floor(m$agents$x[rows]) + 1, levels = seq_len(m$width)))
        cnt[] <- as.integer(t)
      }
      cnt
    }
    m$agents_near <- function(i, radius) {
      "The rows of the other agents within the radius (cells) of the agent of row i."
      j <- seq_len(nrow(m$agents))
      j[j != i & m$agent_distance(i, j) <= radius]
    }
    m$.agent_json <- function() {
      "The shapes of the agents as JSON (vectorized: fast for many agents)."
      a <- m$agents
      n <- nrow(a)
      if (!n) return("[]")
      col <- function(name, default) { v <- a[[name]]; if (is.null(v)) rep(default, n) else ifelse(is.na(v), default, as.character(v)) }
      quote <- function(v) paste0('"', gsub('"', '\\\\"', gsub("\\\\", "\\\\\\\\", v)), '"')
      num <- function(v) sprintf("%.17g", v)
      size <- as.numeric(col("size", 0.8)); size[!(size > 0)] <- 0.8
      color <- col("color", "#e53935"); image <- col("image", ""); shape <- col("shape", "circle")
      x <- a$x; y <- a$y; h <- a$heading
      out <- sprintf('{"k":"circle","p":[%s,%s,%s],"s":"#00000066","f":%s,"t":null,"w":1,"a":0}', num(x), num(y), num(size / 2), quote(color))
      sq <- shape == "square"
      if (any(sq)) out[sq] <- sprintf('{"k":"rect","p":[%s,%s,%s,%s],"s":"#00000066","f":%s,"t":null,"w":1,"a":0}',
                                      num(x[sq] - size[sq] / 2), num(y[sq] - size[sq] / 2), num(size[sq]), num(size[sq]), quote(color[sq]))
      ar <- shape == "arrow"
      if (any(ar)) {
        r <- size[ar] / 2
        pts <- vapply(1:4, function(k) {
          d <- c(0, 140, 180, 220)[k]; f <- c(1, 1, 0.35, 1)[k]
          paste0(num(x[ar] + r * f * api$sin_deg(h[ar] + d)), ",", num(y[ar] - r * f * api$cos_deg(h[ar] + d)))
        }, character(sum(ar)))
        pts <- matrix(pts, ncol = 4)
        out[ar] <- sprintf('{"k":"polygon","p":[%s],"s":"#00000066","f":%s,"t":null,"w":1,"a":0}', apply(pts, 1, paste, collapse = ","), quote(color[ar]))
      }
      im <- nzchar(image)
      if (any(im)) out[im] <- sprintf('{"k":"image","p":[%s,%s],"s":null,"f":null,"t":%s,"w":%s,"a":%s}', num(x[im]), num(y[im]), quote(image[im]), num(size[im]), num(h[im]))
      paste0("[", paste(out, collapse = ","), "]")
    }
    m
  }

  grid_functions <- function(m) {
    m$width <- 0L; m$height <- 0L; m$wrap <- TRUE
    m$cells <- matrix(0L, 0, 0)
    m$.palette <- c("#ffffff", "#000000")
    m$size <- function(w, h) {
      "A new grid (all states 0): m$cells is a matrix with h rows and w columns."
      m$width <- as.integer(w); m$height <- as.integer(h)
      m$cells <- matrix(0L, m$height, m$width)
      invisible(m)
    }
    m$palette <- function(...) { m$.palette <- c(...); invisible(m) }
    m$get <- function(x, y) {
      if (m$wrap) return(m$cells[((y - 1) %% m$height) + 1, ((x - 1) %% m$width) + 1])
      if (x < 1 || y < 1 || x > m$width || y > m$height) return(0L)
      m$cells[y, x]
    }
    m$set <- function(x, y, state) { m$cells[y, x] <- as.integer(state); invisible(m) }
    m$count_neighbors <- function(state) {
      "The numbers of the 8 neighbors with the state (a matrix)."
      a <- (m$cells == state) * 1L
      Reduce(`+`, lapply(N8, function(d) shift(a, d[1], d[2], m$wrap)))
    }
    m$count4 <- function(state) {
      a <- (m$cells == state) * 1L
      Reduce(`+`, lapply(N4, function(d) shift(a, d[1], d[2], m$wrap)))
    }
    m$count <- function(state) sum(m$cells == state)
    m$.state <- function() list(width = m$width, height = m$height, cells = as.integer(t(m$cells)), palette = as.character(m$.palette))
    m
  }

  api$grid_model <- function(name = "GridModel", description = "") {
    "A 2D grid with integer states (the colors of m$palette(...)): m$cells[y, x]; agents on the grid: m$agents (m$add_agents)."
    grid_agents(grid_functions(model("GridModel", name, description)))
  }

  api$value_grid_model <- function(name = "ValueGridModel", description = "") {
    "A 2D grid with double values (a color map from m$range(min, max)): m$values[y, x]."
    m <- grid_agents(model("ValueGridModel", name, description))
    m$width <- 0L; m$height <- 0L; m$wrap <- TRUE
    m$values <- matrix(0, 0, 0)
    m$.min <- 0; m$.max <- 1; m$.colors <- VIRIDIS
    m$size <- function(w, h) {
      m$width <- as.integer(w); m$height <- as.integer(h)
      m$values <- matrix(0, m$height, m$width)
      invisible(m)
    }
    m$range <- function(min, max) { m$.min <- min; m$.max <- max; invisible(m) }
    m$color_map <- function(...) { if (length(c(...)) >= 2) m$.colors <- c(...); invisible(m) }
    m$get <- function(x, y) {
      if (m$wrap) return(m$values[((y - 1) %% m$height) + 1, ((x - 1) %% m$width) + 1])
      if (x < 1 || y < 1 || x > m$width || y > m$height) return(0)
      m$values[y, x]
    }
    m$set <- function(x, y, value) { m$values[y, x] <- value; invisible(m) }
    m$sum4 <- function() Reduce(`+`, lapply(N4, function(d) shift(m$values, d[1], d[2], m$wrap)))
    m$sum8 <- function() Reduce(`+`, lapply(N8, function(d) shift(m$values, d[1], d[2], m$wrap)))
    m$laplacian <- function() m$sum4() - 4 * m$values
    m$mean <- function(values = m$values) if (length(values)) mean(values) else 0
    m$.state <- function() list(width = m$width, height = m$height, values = as.numeric(t(m$values)), min = m$.min, max = m$.max,
                                colors = as.character(m$.colors))
    m
  }

  api$agent_model <- function(name = "AgentModel", description = "") {
    "Agents in a 3D box: m$agents is a data frame with the columns x, y, z (and color, own columns)."
    m <- model("AgentModel", name, description)
    m$agents <- data.frame(x = numeric(), y = numeric(), z = numeric())
    m$box_width <- 100; m$box_height <- 100; m$box_depth <- 100
    m$agent_radius <- 0; m$agent_color <- "#1f77b4"
    # "sphere" or "glider" (triangles: points in the direction of the movement, climbs, dives and banks)
    m$agent_shape <- "sphere"
    # the floor: "none" (the edges of the box), "grid" or "plane" (a dark floor with a grid); soft shadows
    m$floor <- "none"; m$shadows <- FALSE
    m$.follow <- NA_integer_
    m$.scenery <- list()
    m$place_model <- function(model, x, y, z = 0, heading = 0, size = 0, animation = NULL) {
      "Places a 3D model (glTF, m$load_model) in the environment, e.g. a house, a tree or a rock: no agent, it stays until it is removed (not drawn again in each step; Setup removes all). z: the height of its base, heading: degrees (0 the direction of y, clockwise), size: its height (0: the diameter of an agent), animation: its name or number (\"none\": still). Returns its index (m$remove_model)."
      m$.scenery[[length(m$.scenery) + 1]] <- list(model = as.character(model), v = as.numeric(c(x, y, z, heading, size)),
                                                   animation = if (is.null(animation)) "" else as.character(animation))
      invisible(length(m$.scenery))
    }
    m$remove_model <- function(i) {
      "Removes the object of the environment with the index (m$place_model); the later ones move up by one."
      if (i >= 1 && i <= length(m$.scenery)) m$.scenery[[i]] <- NULL
      invisible(m)
    }
    m$clear_models <- function() {
      "Removes all objects of the environment."
      m$.scenery <- list()
      invisible(m)
    }
    m$camera_follow <- function(i = NULL) {
      "The custom camera of the 3D view follows the agent of row i from behind, the global view beside it (split view); NULL: off. Only in Bio7 (the page shows the orbit view)."
      m$.follow <- if (is.null(i) || is.na(i)) NA_integer_ else as.integer(i)
      invisible(m)
    }
    m$box <- function(w, h, d) { m$box_width <- w; m$box_height <- h; m$box_depth <- d; invisible(m) }
    m$agent_distance <- function(i, j) {
      "The distance of the agents in the rows i and j (3D): the same as m$distance(i, j), the same name as for the agents of a grid."
      m$distance(i, j)
    }
    m$link_count <- function() {
      "The number of links."
      nrow(m$.links)
    }
    m$distance <- function(i, j) {
      a <- m$agents
      sqrt((a$x[i] - a$x[j])^2 + (a$y[i] - a$y[j])^2 + (a$z[i] - a$z[j])^2)
    }
    m$neighbors <- function(i, radius) {
      "The rows of the other agents within the radius."
      a <- m$agents
      d2 <- (a$x - a$x[i])^2 + (a$y - a$y[i])^2 + (a$z - a$z[i])^2
      which(d2 <= radius * radius & seq_len(nrow(a)) != i)
    }
    m$nearest <- function(i, radius) {
      "The row of the nearest other agent within the radius (NA if none)."
      a <- m$agents
      d2 <- (a$x - a$x[i])^2 + (a$y - a$y[i])^2 + (a$z - a$z[i])^2
      d2[i] <- Inf
      k <- which.min(d2)
      if (length(k) == 0 || d2[k] > radius * radius) NA_integer_ else k
    }
    m$keep_in_box <- function() {
      "All agents in the box."
      m$agents$x <- pmax(0, pmin(m$box_width, m$agents$x))
      m$agents$y <- pmax(0, pmin(m$box_height, m$agents$y))
      m$agents$z <- pmax(0, pmin(m$box_depth, m$agents$z))
      invisible(m)
    }
    m$wrap_in_box <- function() {
      "x and y wrap around (a torus), z stays in the box."
      m$agents$x <- m$agents$x %% m$box_width
      m$agents$y <- m$agents$y %% m$box_height
      m$agents$z <- pmax(0, pmin(m$box_depth, m$agents$z))
      invisible(m)
    }
    m$neighbor_pairs <- function(radius) {
      "All pairs of agents within the distance: a matrix with the columns i and j (rows, i < j; fast for many agents)."
      a <- m$agents
      pairs_within(cbind(a$x, a$y, a$z), radius)
    }
    # networks: links between the agents (rows), drawn as lines in the 3D view and in the page
    m$link_color <- "#8a94a6"; m$link_width <- 1
    m$.links <- matrix(integer(), 0, 2); m$.link_colors <- character()
    key <- function(i, j) paste(pmin(i, j), pmax(i, j))
    m$link <- function(i, j, color = NA) {
      "Links the agents of the rows i and j (vectors; undirected, a link once; color: of the links, NA: m$link_color)."
      k <- max(length(i), length(j))
      if (k == 0) return(invisible(m))
      i <- rep_len(as.integer(i), k); j <- rep_len(as.integer(j), k); color <- rep_len(as.character(color), k)
      keep <- !is.na(i) & !is.na(j) & i != j
      all <- rbind(m$.links, cbind(pmin(i, j), pmax(i, j))[keep, , drop = FALSE])
      cols <- c(m$.link_colors, color[keep])
      kk <- paste(all[, 1], all[, 2])
      first <- !duplicated(kk)
      # a link again: its new color
      last <- length(kk) + 1 - match(kk[first], rev(kk))
      m$.links <- all[first, , drop = FALSE]; m$.link_colors <- cols[last]
      invisible(m)
    }
    m$unlink <- function(i, j) {
      "Removes the links of the rows i and j (vectors)."
      drop <- paste(m$.links[, 1], m$.links[, 2]) %in% key(i, j)
      m$.links <- m$.links[!drop, , drop = FALSE]; m$.link_colors <- m$.link_colors[!drop]
      invisible(m)
    }
    m$remove_links <- function(i) {
      "Removes all links of the agents of the rows i."
      drop <- m$.links[, 1] %in% i | m$.links[, 2] %in% i
      m$.links <- m$.links[!drop, , drop = FALSE]; m$.link_colors <- m$.link_colors[!drop]
      invisible(m)
    }
    m$remove_agents <- function(rows) {
      "Removes the agents of the rows and their links (the links of the other agents are renumbered)."
      n <- nrow(m$agents)
      gone <- seq_len(n) %in% rows
      m$remove_links(which(gone))
      new <- cumsum(!gone)
      m$.links <- matrix(new[m$.links], ncol = 2)
      m$agents <- m$agents[!gone, , drop = FALSE]
      rownames(m$agents) <- NULL
      invisible(m)
    }
    m$clear_links <- function() { m$.links <- matrix(integer(), 0, 2); m$.link_colors <- character(); invisible(m) }
    m$linked <- function(i, j) key(i, j) %in% paste(m$.links[, 1], m$.links[, 2])
    m$links <- function() { l <- m$.links; colnames(l) <- c("i", "j"); l }
    m$link_neighbors <- function(i) {
      "The rows of the agents linked with the agent of row i (in the order of the links)."
      l <- m$.links
      k <- which(l[, 1] == i | l[, 2] == i)
      ifelse(l[k, 1] == i, l[k, 2], l[k, 1])
    }
    m$degree <- function(i = NULL) {
      "The number of links of the agents of the rows i (NULL: of all agents)."
      d <- tabulate(as.vector(m$.links), nbins = nrow(m$agents))
      if (is.null(i)) d else d[i]
    }
    m$set_link_color <- function(i, j, color) {
      hit <- paste(m$.links[, 1], m$.links[, 2]) %in% key(i, j)
      m$.link_colors[hit] <- color
      invisible(m)
    }
    m$link_random <- function(p) {
      "A random network: each pair of agents is linked with the probability p (Erdos-Renyi)."
      n <- nrow(m$agents)
      if (n < 2) return(invisible(m))
      ij <- which(upper.tri(matrix(0, n, n)), arr.ind = TRUE)
      hit <- m$random(nrow(ij)) < p
      m$link(ij[hit, 1], ij[hit, 2])
    }
    m$link_small_world <- function(k, p) {
      "A small-world network (Watts-Strogatz): a ring of the agents (their rows), each linked with its k nearest (k / 2 on each side), then each link is moved with the probability p."
      n <- nrow(m$agents); half <- max(1, k %/% 2)
      if (n < 2) return(invisible(m))
      for (d in seq_len(half)) m$link(seq_len(n), (seq_len(n) - 1 + d) %% n + 1)
      for (i in seq_len(n)) for (d in seq_len(half)) {
        b <- (i - 1 + d) %% n + 1
        if (m$random(1) < p && m$linked(i, b)) {
          for (try in seq_len(n)) {
            c0 <- floor(m$random(1) * n) + 1
            if (c0 != i && !m$linked(i, c0)) { m$unlink(i, b); m$link(i, c0); break }
          }
        }
      }
      invisible(m)
    }
    m$link_scale_free <- function(mm) {
      "A scale-free network (Barabasi-Albert): the first mm + 1 agents are linked with each other, each further agent links with mm agents before it (chosen by their number of links)."
      n <- nrow(m$agents); mm <- max(1, mm); start <- min(n, mm + 1)
      if (start >= 2) { ij <- which(upper.tri(matrix(0, start, start)), arr.ind = TRUE); m$link(ij[, 1], ij[, 2]) }
      ends <- as.vector(t(m$.links))
      for (i in seq_len(n)[-seq_len(start)]) {
        chosen <- integer()
        while (length(chosen) < min(mm, i - 1)) {
          j <- if (length(ends)) ends[floor(m$random(1) * length(ends)) + 1] else floor(m$random(1) * (i - 1)) + 1
          if (!(j %in% chosen)) chosen <- c(chosen, j)
        }
        m$link(rep(i, length(chosen)), chosen)
        ends <- c(ends, as.vector(rbind(rep(i, length(chosen)), chosen)))
      }
      invisible(m)
    }
    m$link_nearby <- function(radius) {
      "Links all agents within the distance (a network of the neighbourhood)."
      pr <- m$neighbor_pairs(radius)
      m$link(pr[, 1], pr[, 2])
    }
    m$components <- function() {
      "The group of each agent: the agents connected by links have the same number (1, 2, ... in the order of the rows)."
      n <- nrow(m$agents); l <- m$.links
      adj <- split(c(l[, 2], l[, 1]), factor(c(l[, 1], l[, 2]), levels = seq_len(n)))
      group <- integer(n); g <- 0
      for (s in seq_len(n)) {
        if (group[s] > 0) next
        g <- g + 1; queue <- s; group[s] <- g
        while (length(queue)) {
          c0 <- queue[1]; queue <- queue[-1]
          nb <- adj[[c0]]; nb <- nb[group[nb] == 0]
          group[nb] <- g; queue <- c(queue, nb)
        }
      }
      group
    }
    m$shortest_path <- function(i, j) {
      "The number of links on the shortest path between the agents of the rows i and j (-1: not connected)."
      if (i == j) return(0)
      n <- nrow(m$agents); l <- m$.links
      adj <- split(c(l[, 2], l[, 1]), factor(c(l[, 1], l[, 2]), levels = seq_len(n)))
      dist <- rep(-1, n); dist[i] <- 0; queue <- i
      while (length(queue)) {
        c0 <- queue[1]; queue <- queue[-1]
        nb <- adj[[c0]]; nb <- nb[dist[nb] < 0]
        dist[nb] <- dist[c0] + 1
        if (dist[j] >= 0) return(dist[j])
        queue <- c(queue, nb)
      }
      -1
    }
    m$clustering <- function(i = NULL) {
      "The share of the pairs of the linked agents which are linked too (0 with less than 2), for the rows i (NULL: all)."
      rows <- if (is.null(i)) seq_len(nrow(m$agents)) else i
      vapply(rows, function(r) {
        nb <- m$link_neighbors(r); k <- length(nb)
        if (k < 2) return(0)
        pr <- which(upper.tri(matrix(0, k, k)), arr.ind = TRUE)
        2 * sum(m$linked(nb[pr[, 1]], nb[pr[, 2]])) / (k * (k - 1))
      }, 0)
    }
    m$layout_circle <- function() {
      "Places the agents on a circle in the middle of the box (their rows; z: the middle)."
      n <- nrow(m$agents); r <- min(m$box_width, m$box_height) * 0.42; t <- 2 * pi * (seq_len(n) - 1) / max(1, n)
      m$agents$x <- m$box_width / 2 + r * cos(t); m$agents$y <- m$box_height / 2 + r * sin(t); m$agents$z <- rep(m$box_depth / 2, n)
      invisible(m)
    }
    m$layout_spring <- function(iterations = 50) {
      "Arranges the network in x and y (a spring layout: the links pull, all agents push each other away; in the box, z kept). Once (e.g. 100 iterations in init) or a few in each step."
      n <- nrow(m$agents)
      if (n == 0) return(invisible(m))
      w <- m$box_width; h <- m$box_height; k <- sqrt(w * h / n); l <- m$.links
      x <- m$agents$x; y <- m$agents$y
      for (it in seq_len(iterations) - 1) {
        temp <- max(w, h) * 0.1 * (1 - it / iterations)
        ex <- outer(x, x, "-"); ey <- outer(y, y, "-"); d <- sqrt(ex^2 + ey^2)
        same <- d < 1e-9 & row(d) != col(d)
        if (any(same)) { s0 <- (row(d) + col(d) - 2)[same]; ex[same] <- 0.01 * k * cos(s0) * sign(col(d) - row(d))[same]; ey[same] <- 0.01 * k * sin(s0) * sign(col(d) - row(d))[same]; d[same] <- 0.01 * k }
        diag(d) <- Inf
        f <- k * k / d
        dx <- rowSums(ex / d * f); dy <- rowSums(ey / d * f)
        if (nrow(l)) {
          lx <- x[l[, 1]] - x[l[, 2]]; ly <- y[l[, 1]] - y[l[, 2]]; ld <- pmax(1e-9, sqrt(lx^2 + ly^2)); lf <- ld * ld / k
          acc <- function(v, idx) { r <- numeric(n); a <- rowsum(v, idx); r[as.integer(rownames(a))] <- a[, 1]; r }
          fx <- lx / ld * lf; fy <- ly / ld * lf
          dx <- dx - acc(fx, l[, 1]) + acc(fx, l[, 2])
          dy <- dy - acc(fy, l[, 1]) + acc(fy, l[, 2])
        }
        dl <- sqrt(dx^2 + dy^2)
        sc <- ifelse(dl > 0, pmin(dl, temp) / ifelse(dl > 0, dl, 1), 0)
        x <- pmin(w, pmax(0, x + dx * sc)); y <- pmin(h, pmax(0, y + dy * sc))
      }
      m$agents$x <- x; m$agents$y <- y
      invisible(m)
    }
    m$.prepare <- function() {
      m$agents <- data.frame(x = numeric(), y = numeric(), z = numeric())
      m$.scenery <- list()
      m$clear_links()
    }
    m$.state <- function() {
      a <- m$agents
      n <- nrow(a)
      colors <- if (!is.null(a$color)) ifelse(is.na(a$color), "", as.character(a$color)) else rep("", n)
      follow <- if (is.na(m$.follow) || m$.follow < 1 || m$.follow > n) -1 else m$.follow - 1
      s <- list(box = c(m$box_width, m$box_height, m$box_depth), radius = m$agent_radius, color = m$agent_color,
                xyz = as.numeric(rbind(a$x, a$y, a$z)), colors = as.character(colors),
                shape = as.character(m$agent_shape), floor = as.character(m$floor), shadows = if (isTRUE(m$shadows)) 1 else 0,
                follow = as.numeric(follow),
                # the objects of the environment (all in each state): x, y, z, heading and size for each model
                scenery_models = vapply(m$.scenery, function(p) p$model, ""),
                scenery = as.numeric(unlist(lapply(m$.scenery, function(p) p$v))),
                scenery_animations = vapply(m$.scenery, function(p) p$animation, ""),
                link_color = as.character(if (is.null(m$link_color) || is.na(m$link_color)) "" else m$link_color), link_width = as.numeric(m$link_width))
      l <- m$.links
      ok <- l[, 1] <= n & l[, 2] <= n
      if (any(ok)) {
        s$links <- as.numeric(t(l[ok, , drop = FALSE] - 1))
        s$link_colors <- ifelse(is.na(m$.link_colors[ok]), "", m$.link_colors[ok])
      }
      if (!is.null(a$image) || !is.null(a$model)) {
        chr <- function(v) if (is.null(v)) rep("", n) else as.character(ifelse(is.na(v), "", v))
        s$images <- chr(a$image); s$models <- chr(a$model); s$animations <- chr(a$animation)
        s$headings <- if (is.null(a$heading)) rep(NaN, n) else as.numeric(a$heading)
        s$sizes <- if (is.null(a$size)) rep(0, n) else as.numeric(a$size)
      }
      s
    }
    m
  }

  api$geo_grid_model <- function(name = "GeoGridModel", description = "") {
    "A grid on the Earth: m$load reads the data with GDAL (only in Bio7), the page gets the loaded layers."
    m <- grid_agents(grid_functions(model("GeoGridModel", name, description)))
    m$origin_x <- 0; m$origin_y <- 0; m$cell_width <- 1; m$cell_height <- -1; m$cell_size_meters <- 1
    m$south <- 0; m$west <- 0; m$north <- 0; m$east <- 0; m$crs <- ""
    m$.mode <- "other"; m$.layers <- list(); m$.work <- list(); m$.shown <- NULL
    m$load <- function(m) NULL
    geometry <- function(g) {
      m$width <- as.integer(g$width); m$height <- as.integer(g$height)
      m$origin_x <- g$originX; m$origin_y <- g$originY; m$cell_width <- g$cellWidth; m$cell_height <- g$cellHeight
      m$cell_size_meters <- g$cellSizeMeters
      m$south <- g$south; m$west <- g$west; m$north <- g$north; m$east <- g$east
      m$.mode <- as.character(g$mode); m$crs <- if (is.null(g$crs)) "" else as.character(g$crs)
      m$cells <- matrix(0L, m$height, m$width)
    }
    as_grid <- function(v) matrix(as.numeric(v), m$height, m$width, byrow = TRUE)
    m$grid <- function(file, columns) {
      "The grid of a raster file: the extent, the coordinate system and the number of the columns."
      geometry(java("grid", c(file, as.integer(columns))))
      invisible(m)
    }
    m$raster <- function(file, band = 1, resampling = "bilinear") as_grid(java("raster", c(file, as.integer(band), resampling)))
    m$vector <- function(file, value = 1, attribute = NULL) as_grid(java("vector", c(file, if (is.null(attribute)) "" else attribute, value)))
    m$add_layer <- function(name, values) {
      "Adds a layer (a matrix) for init and step (m$layer(name))."
      v <- matrix(as.numeric(values), m$height, m$width)
      m$.layers[[name]] <- v
      if (!browser) java("add_layer", name, as.numeric(t(v)))
      invisible(m)
    }
    m$attribution <- function(text) { if (!browser) java("attribution", text); invisible(m) }
    m$show_on_globe <- function() { if (!browser) java("globe"); invisible(m) }
    m$layer <- function(name) m$.work[[name]]
    m$show_values <- function(values, min, max, ...) {
      colors <- c(...)
      m$.shown <- list(values = values, min = min, max = max, colors = if (length(colors) >= 2) colors else VIRIDIS)
      invisible(m)
    }
    m$show_states <- function() { m$.shown <- NULL; invisible(m) }
    m$col_at <- function(lon, lat) {
      "The column (1..width) of the cell at the longitude and latitude (WGS84), NA outside."
      x <- if (m$.mode == "mercator") floor((lon * MERCATOR / 180 - m$origin_x) / m$cell_width)
           else if (m$.mode == "geographic") floor((lon - m$origin_x) / m$cell_width)
           else floor((lon - m$west) / (m$east - m$west) * m$width)
      ifelse(x >= 0 & x < m$width, x + 1, NA)
    }
    m$row_at <- function(lon, lat) {
      y <- if (m$.mode == "mercator") floor((log(tan((90 + lat) * pi / 360)) / (pi / 180) * MERCATOR / 180 - m$origin_y) / m$cell_height)
           else if (m$.mode == "geographic") floor((lat - m$origin_y) / m$cell_height)
           else floor((m$north - lat) / (m$north - m$south) * m$height)
      ifelse(y >= 0 & y < m$height, y + 1, NA)
    }
    m$lon <- function(x) {
      "The longitude of the center of the column x (1..width)."
      x <- x - 1
      if (m$.mode == "mercator") (m$origin_x + (x + 0.5) * m$cell_width) / MERCATOR * 180
      else if (m$.mode == "geographic") m$origin_x + (x + 0.5) * m$cell_width
      else m$west + (x + 0.5) / m$width * (m$east - m$west)
    }
    m$lat <- function(y) {
      y <- y - 1
      if (m$.mode == "mercator") { my <- (m$origin_y + (y + 0.5) * m$cell_height) / MERCATOR * 180; 180 / pi * (2 * atan(exp(my * pi / 180)) - pi / 2) }
      else if (m$.mode == "geographic") m$origin_y + (y + 0.5) * m$cell_height
      else m$north - (y + 0.5) / m$height * (m$north - m$south)
    }
    m$value <- function(layer, x, y) layer[pmax(1, pmin(m$height, y)), pmax(1, pmin(m$width, x))]
    m$gradient <- function(layer) {
      "The gradients (rise per meter) of a layer (e.g., the elevation): a matrix."
      h <- nrow(layer); w <- ncol(layer)
      e <- function(rows, cols) layer[pmin(pmax(rows, 1), h), pmin(pmax(cols, 1), w), drop = FALSE]
      dzdx <- (e(1:h, 2:(w + 1)) - e(1:h, 0:(w - 1))) / (2 * m$cell_size_meters)
      dzdy <- (e(2:(h + 1), 1:w) - e(0:(h - 1), 1:w)) / (2 * m$cell_size_meters)
      sqrt(dzdx^2 + dzdy^2)
    }
    m$slope_degrees <- function(layer) atan(m$gradient(layer)) * 180 / pi
    m$.load <- function() {
      m$.layers <- list()
      if (browser) {
        g <- get(".bio7sim_geo", envir = globalenv())
        geometry(g)
        for (k in names(g$layers)) m$.layers[[k]] <- matrix(as.numeric(g$layers[[k]]), m$height, m$width, byrow = TRUE)
      } else m$load(m)
    }
    m$.prepare <- function() {
      m$cells <- matrix(0L, m$height, m$width)
      m$.work <- m$.layers
      m$.shown <- NULL
    }
    grid_state <- m$.state
    m$.state <- function() {
      s <- grid_state()
      if (!is.null(m$.shown)) {
        s$shown <- as.numeric(t(m$.shown$values)); s$min <- m$.shown$min; s$max <- m$.shown$max; s$colors <- as.character(m$.shown$colors)
      }
      s
    }
    m
  }

  api$geo_agent_model <- function(name = "GeoAgentModel", description = "") {
    "Agents on the Earth: m$agents is a data frame with lon, lat (and altitude, heading, color, size, label)."
    m <- model("GeoAgentModel", name, description)
    empty <- function() data.frame(lon = numeric(), lat = numeric())
    m$agents <- empty()
    m$south <- -60; m$west <- -180; m$north <- 75; m$east <- 180
    m$agent_size <- 4; m$agent_color <- "#d62728"; m$agent_shape <- "point"; m$agent_size_meters <- 300; m$view_pitch <- 0
    m$EARTH_RADIUS <- 6371.0088
    m$cos_lat <- api$cos_lat; m$cos_deg <- api$cos_deg; m$sin_deg <- api$sin_deg
    m$area <- function(south, west, north, east) { m$south <- south; m$west <- west; m$north <- north; m$east <- east; invisible(m) }
    m$distance_km <- function(lon1, lat1, lon2, lat2) {
      "The great circle distances in km (vectors)."
      p1 <- lat1 * (pi / 180); p2 <- lat2 * (pi / 180); dp <- p2 - p1; dl <- (lon2 - lon1) * (pi / 180)
      h <- sin(dp / 2)^2 + cos(p1) * cos(p2) * sin(dl / 2)^2
      2 * m$EARTH_RADIUS * asin(pmin(1, sqrt(h)))
    }
    m$move <- function(i, east_km, north_km) {
      "Moves the agents of the rows i (km to the east and to the north)."
      lat <- m$agents$lat[i] + north_km / 110.574
      lon <- m$agents$lon[i] + east_km / (111.320 * m$cos_lat(lat))
      lon <- ifelse(lon > 180, lon - 360, ifelse(lon < -180, lon + 360, lon))
      m$agents$lon[i] <- lon
      m$agents$lat[i] <- pmax(-89.9, pmin(89.9, lat))
      invisible(m)
    }
    m$move_towards <- function(i, lon, lat, km) {
      "Moves the agents of the rows i km towards the points (TRUE for the arrived agents)."
      a <- m$agents
      east <- (lon - a$lon[i]) * 111.320 * m$cos_lat((lat + a$lat[i]) / 2)
      north <- (lat - a$lat[i]) * 110.574
      d <- sqrt(east^2 + north^2)
      arrived <- d <= km
      lon <- rep_len(lon, length(i)); lat <- rep_len(lat, length(i))
      if (any(arrived)) { m$agents$lon[i[arrived]] <- lon[arrived]; m$agents$lat[i[arrived]] <- lat[arrived] }
      if (any(!arrived)) { k <- !arrived; m$move(i[k], east[k] / d[k] * km, north[k] / d[k] * km) }
      arrived
    }
    m$neighbors_km <- function(i, km) {
      a <- m$agents
      which(m$distance_km(a$lon[i], a$lat[i], a$lon, a$lat) <= km & seq_len(nrow(a)) != i)
    }
    m$.prepare <- function() m$agents <- empty()
    m$.state <- function() {
      a <- m$agents; n <- nrow(a)
      col <- function(name, default) if (!is.null(a[[name]])) a[[name]] else rep(default, n)
      chr <- function(name) { v <- col(name, ""); as.character(ifelse(is.na(v), "", v)) }
      list(area = c(m$south, m$west, m$north, m$east), size = m$agent_size, color = m$agent_color, shape = m$agent_shape,
           meters = m$agent_size_meters, pitch = m$view_pitch, lon = as.numeric(a$lon), lat = as.numeric(a$lat),
           altitude = as.numeric(col("altitude", 0)), heading = as.numeric(col("heading", NaN)), sizes = as.numeric(col("size", 0)),
           colors = chr("color"), labels = chr("label"), images = chr("image"), models = chr("model"), animations = chr("animation"))
    }
    m
  }

  # ------------------------------------------------------------ the running model

  json <- function(x) {
    if (is.null(x)) return("null")
    if (is.list(x)) {
      inner <- vapply(seq_along(x), function(k) json(x[[k]]), "")
      if (!is.null(names(x))) return(paste0("{", paste0(json_str(names(x)), ":", inner, collapse = ","), "}"))
      return(paste0("[", paste(inner, collapse = ","), "]"))
    }
    if (is.function(x)) return("null")
    v <- if (is.character(x)) json_str(x) else if (is.logical(x)) ifelse(x, "true", "false") else format(x, digits = 17, scientific = FALSE, trim = TRUE)
    if (length(v) == 1) v else paste0("[", paste(v, collapse = ","), "]")
  }
  json_str <- function(s) {
    s <- gsub("\\\\", "\\\\\\\\", s); s <- gsub("\"", "\\\\\"", s); s <- gsub("\n", "\\\\n", s); s <- gsub("\t", "\\\\t", s)
    paste0("\"", s, "\"")
  }

  api$simulation <- function(m, source = NULL) {
    "Starts the model: in Bio7 with the views of the model, in the page with its controls."
    assign(".bio7sim_model", m, envir = globalenv())
    if (browser) return(invisible(m))
    params <- lapply(m$.params, function(p) { p$options <- if (is.null(p$options)) list() else as.list(p$options); p })
    actions <- lapply(m$.actions, function(a) list(name = a$name, label = a$label, group = a$group))
    spec <- list(name = m$.name, kind = m$.kind, language = "r", description = m$.description,
                 params = unname(params), actions = unname(actions))
    if (is.null(source)) {
      # the file of source() (the frames of the calls)
      for (k in rev(seq_len(sys.nframe()))) {
        f <- tryCatch(sys.frame(k)$ofile, error = function(e) NULL)
        if (is.character(f)) { source <- f; break }
      }
    }
    if (!is.null(source)) spec$source <- normalizePath(source, mustWork = FALSE)
    call <- get(".bio7_call", envir = as.environment("bio7ffm"))
    call("sim_start", json(spec), NULL)
    invisible(m)
  }

  # ------------------------------------------------------------ spatial search

  api$pairs_within <- function(points, radius) {
    "All pairs of points within the distance (fast for many points: a grid of cells): points a matrix (n, 2) or (n, 3). Returns a matrix with the columns i and j (the rows of the points, i < j, sorted by i, then j)."
    p <- as.matrix(points)
    n <- nrow(p); d <- ncol(p)
    empty <- matrix(integer(), 0, 2, dimnames = list(NULL, c("i", "j")))
    if (n < 2 || radius <= 0) return(empty)
    cell <- floor(p / radius)
    cell <- sweep(cell, 2, apply(cell, 2, min))
    # one empty column after each row of cells: the cells -1 and +1 never mix rows
    dims <- apply(cell, 2, max) + 2
    mult <- cumprod(c(1, dims[-d]))
    key <- as.vector(cell %*% mult)
    ord <- order(key); sk <- key[ord]
    offs <- as.matrix(expand.grid(rep(list(-1:1), d)))
    half <- apply(offs, 1, function(o) all(o == 0) || o[which(o != 0)[1]] > 0)
    ii <- integer(); jj <- integer()
    for (r in which(half)) {
      o <- offs[r, ]
      tk <- key + sum(o * mult)
      lo <- findInterval(tk - 0.5, sk) + 1
      cnt <- pmax(0, findInterval(tk + 0.5, sk) - lo + 1)
      if (sum(cnt) == 0) next
      a <- rep(seq_len(n), cnt)
      b <- ord[rep(lo, cnt) + sequence(cnt) - 1]
      if (all(o == 0)) { keep <- a < b; a <- a[keep]; b <- b[keep] }
      ii <- c(ii, a); jj <- c(jj, b)
    }
    if (!length(ii)) return(empty)
    near <- rowSums((p[ii, , drop = FALSE] - p[jj, , drop = FALSE])^2) <= radius^2
    a <- pmin(ii[near], jj[near]); b <- pmax(ii[near], jj[near])
    o <- order(a, b)
    cbind(i = a[o], j = b[o])
  }

  # ------------------------------------------------------------ experiments

  api$experiment <- function(m, params = list(), steps = 100, repetitions = 1, seed = 42, every = 1, progress = NULL, design = NULL) {
    "Runs the model several times without a display (batch runs) and returns a data frame: params a named list of the swept parameters and their values (all combinations are run), steps the steps of each run, repetitions the runs of each combination (the seeds seed, seed + 1, ...: the same for all parameter values), every a row every n steps (step 0 and the last step always). The rows: run, seed, the swept parameters, step, the values of the line charts in the step (m$chart: the name of the series, a second chart 'chart: series') and of m$record(name, value). The settings of the model are the start values; afterwards they are restored (press Setup to show the model again). progress(done, total): called after each run. design: given parameter sets instead of (or combined with) the sweep, e.g. a Latin hypercube (lhs) or the samples of a sensitivity analysis (sensitivity): a data frame or a list of columns (the names are parameters); each row is run with the repetitions, the column point of the result is the row of the design (1, 2, ...). Values of int parameters are rounded."
    pnames <- names(m$.params)
    saved <- if (length(pnames)) mget(pnames, envir = m) else list()
    on.exit(for (n in names(saved)) assign(n, saved[[n]], envir = m), add = TRUE)
    params <- as.list(params)
    convert <- function(n, v) {
      p <- m$.params[[n]]
      if (is.null(p)) return(v)
      switch(p$kind, boolean = if (is.logical(v)) v else tolower(as.character(v)) %in% c("true", "1", "yes", "on"),
             int = as.integer(round(as.numeric(v))), double = as.numeric(v), as.character(v))
    }
    # all combinations (the last parameter changes fastest, as in Java and Python)
    combos <- if (length(params)) expand.grid(rev(params), KEEP.OUT.ATTRS = FALSE, stringsAsFactors = FALSE)[, rev(seq_along(params)), drop = FALSE] else NULL
    points <- NULL
    if (!is.null(design)) {
      # each row of the design with each combination of the sweep
      design <- as.data.frame(design, stringsAsFactors = FALSE)
      nd <- nrow(design); ng <- if (is.null(combos)) 1 else nrow(combos)
      points <- rep(seq_len(nd), each = ng)
      d <- design[points, , drop = FALSE]
      combos <- if (is.null(combos)) d else cbind(d, combos[rep(seq_len(ng), nd), , drop = FALSE])
      rownames(combos) <- NULL
    }
    cnames <- if (is.null(combos)) character(0) else names(combos)
    unknown <- setdiff(cnames, pnames)
    if (length(unknown)) stop("Not a parameter of the model: ", paste(unknown, collapse = ", "))
    ncombo <- if (is.null(combos)) 1 else nrow(combos)
    repetitions <- max(1, as.integer(repetitions)); every <- max(1, as.integer(every)); steps <- as.integer(steps)
    rows <- list(); run <- 0; total <- ncombo * repetitions
    if (is.null(progress) && !browser && exists("bio7_progress", mode = "function")) {
      # in Bio7: a job in the Progress view (its stop button interrupts the experiment)
      bio7_progress_fun <- get("bio7_progress", mode = "function")
      progress <- function(done, all_runs) bio7_progress_fun(done, all_runs, sprintf("Experiment: run %d of %d", done, all_runs))
    }
    if (!is.null(progress)) progress(0, total)
    row <- function(k) {
      r <- list(run = run)
      if (!is.null(points)) r$point <- points[ci]
      r$seed <- seed + k
      for (n in cnames) r[[n]] <- get(n, envir = m)
      r$step <- m$t
      for (c in m$.charts) r[[c[[1]]]] <- c[[2]]
      for (c in m$.more_charts) if (identical(c$k, "line")) r[[paste0(c$t, ": ", c$n)]] <- c$y
      for (n in names(m$.records)) r[[n]] <- m$.records[[n]]
      r
    }
    for (ci in seq_len(ncombo)) {
      for (k in seq_len(repetitions) - 1) {
        run <- run + 1
        for (n in names(saved)) assign(n, saved[[n]], envir = m)
        for (n in cnames) assign(n, convert(n, combos[ci, n]), envir = m)
        m$t <- 0; m$seed(seed + k); m$.stopped <- FALSE
        if (is.function(m$.reset_agents)) m$.reset_agents()
        m$.charts <- list(); m$.more_charts <- list(); m$.drawing <- list(); m$.records <- list()
        m$.prepare(); m$init(m)
        rows[[length(rows) + 1]] <- row(k)
        for (s in seq_len(steps)) {
          if (isTRUE(m$.stopped)) break
          m$.charts <- list(); m$.more_charts <- list(); m$.drawing <- list()
          m$step(m); m$t <- m$t + 1
          # the last step of the run: also a step with m$stop()
          if (s %% every == 0 || s == steps || isTRUE(m$.stopped)) rows[[length(rows) + 1]] <- row(k)
        }
        if (!is.null(progress)) progress(run, total)
      }
    }
    cols <- unique(unlist(lapply(rows, names)))
    out <- lapply(stats::setNames(cols, cols), function(cn) unlist(lapply(rows, function(r) { v <- r[[cn]]; if (is.null(v) || length(v) == 0) NA else v[[1]] })))
    as.data.frame(out, check.names = FALSE, stringsAsFactors = FALSE)
  }

  # The protocol of Bio7 and of the page: setup, step, action, set, load; returns the state
  assign(".bio7sim_do", function(op, arg = "") {
    m <- get(".bio7sim_model", envir = globalenv())
    if (op == "load") { if (m$.kind == "GeoGridModel") m$.load(); return(list()) }
    if (op == "setup") {
      # the seed of the settings (the field Seed of Bio7 and of the page)
      sd <- suppressWarnings(as.integer(arg))
      m$t <- 0; m$seed(if (length(sd) == 1 && !is.na(sd)) sd else 42); m$.stopped <- FALSE
      if (is.function(m$.reset_agents)) m$.reset_agents()
      m$.charts <- list(); m$.more_charts <- list(); m$.drawing <- list(); m$.prepare(); m$init(m)
    } else if (op == "step") {
      m$.drawing <- list(); m$.stopped <- FALSE; m$step(m); m$t <- m$t + 1
    } else if (op == "action") {
      m$.actions[[arg]]$fun(m)
    } else if (op == "input") {
      # a click, a drag (x, y: positions from 0 as the agents and the shapes) or a key of the view
      ev <- strsplit(arg, "\u001f", fixed = TRUE)[[1]]
      if (ev[1] == "key") m$key(m, if (length(ev) > 1) ev[2] else "")
      else m[[ev[1]]](m, as.numeric(ev[2]), as.numeric(ev[3]))
    } else if (op == "set") {
      kv <- strsplit(arg, "\u001f", fixed = TRUE)[[1]]
      p <- m$.params[[kv[1]]]
      v <- kv[2]
      assign(kv[1], switch(p$kind, boolean = v == "true", int = as.integer(round(as.numeric(v))), double = as.numeric(v), v), envir = m)
      return(list())
    }
    s <- m$.state()
    s$chart_names <- vapply(m$.charts, function(c) c[[1]], "")
    s$chart_values <- vapply(m$.charts, function(c) c[[2]], 0)
    s$more_charts <- json(m$.more_charts)
    # the agents of a grid are drawn on top of the shapes of the step
    s$draw <- json(m$.drawing)
    if (is.function(m$.agent_json) && nrow(m$agents)) {
      agents <- m$.agent_json()
      s$draw <- if (s$draw == "[]") agents else paste0(substr(s$draw, 1, nchar(s$draw) - 1), ",", substring(agents, 2))
    }
    s$stopped <- if (isTRUE(m$.stopped)) 1 else 0
    # the input functions of the model (Bio7 and the page give it the clicks, drags and keys of the view)
    s$inputs <- c("click", "drag", "key")[vapply(c("click", "drag", "key"), function(n) is.function(get0(n, envir = m, inherits = FALSE)), TRUE)]
    m$.charts <- list(); m$.more_charts <- list()
    s
  }, envir = api)
})
