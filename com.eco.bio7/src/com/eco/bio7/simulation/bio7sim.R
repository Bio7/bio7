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
    m$.params <- list(); m$.actions <- list(); m$.charts <- list(); m$.more_charts <- list(); m$.drawing <- list()
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
    m$seed <- function(n) set.seed(n, kind = "Mersenne-Twister", normal.kind = "Inversion", sample.kind = "Rejection")
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
    shape <- function(k, p, s = NULL, f = NULL, t = NULL, w = 1, a = 0) {
      m$.drawing[[length(m$.drawing) + 1]] <- list(k = k, p = as.list(as.numeric(p)), s = s, f = f, t = t, w = as.numeric(w), a = as.numeric(a))
      invisible(NULL)
    }
    m$draw_line <- function(x1, y1, x2, y2, color = "#000000", width = 1) shape("line", c(x1, y1, x2, y2), color, NULL, NULL, width)
    m$draw_line3 <- function(x1, y1, z1, x2, y2, z2, color = "#000000", width = 1) shape("line3", c(x1, y1, z1, x2, y2, z2), color, NULL, NULL, width)
    m$draw_path <- function(xs, ys, color = "#000000", width = 1) shape("path", as.vector(rbind(xs, ys)), color, NULL, NULL, width)
    m$draw_polygon <- function(xs, ys, fill = NULL, stroke = "#000000") shape("polygon", as.vector(rbind(xs, ys)), stroke, fill)
    m$draw_circle <- function(x, y, radius, fill = NULL, stroke = "#000000") shape("circle", c(x, y, radius), stroke, fill)
    m$draw_rect <- function(x, y, w, h, fill = NULL, stroke = "#000000") shape("rect", c(x, y, w, h), stroke, fill)
    m$draw_text <- function(x, y, text, color = "#000000", size = 1) shape("text", c(x, y), color, NULL, as.character(text), size)
    m$draw_image <- function(name, x, y, size = 1, angle = 0) {
      "An image (m$load_image) centered at x, y: the size and the angle (degrees, clockwise)."
      shape("image", c(x, y), NULL, NULL, name, size, angle)
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
      "Loads a 3D model (glTF .glb or .gltf, also animated) for the agents (a column model)."
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
    "A 2D grid with integer states (the colors of m$palette(...)): m$cells[y, x]."
    grid_functions(model("GridModel", name, description))
  }

  api$value_grid_model <- function(name = "ValueGridModel", description = "") {
    "A 2D grid with double values (a color map from m$range(min, max)): m$values[y, x]."
    m <- model("ValueGridModel", name, description)
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
    m$.prepare <- function() {
      m$agents <- data.frame(x = numeric(), y = numeric(), z = numeric())
      m$.scenery <- list()
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
                scenery_animations = vapply(m$.scenery, function(p) p$animation, ""))
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
    m <- grid_functions(model("GeoGridModel", name, description))
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

  # The protocol of Bio7 and of the page: setup, step, action, set, load; returns the state
  assign(".bio7sim_do", function(op, arg = "") {
    m <- get(".bio7sim_model", envir = globalenv())
    if (op == "load") { if (m$.kind == "GeoGridModel") m$.load(); return(list()) }
    if (op == "setup") {
      m$t <- 0; m$seed(42); m$.charts <- list(); m$.more_charts <- list(); m$.drawing <- list(); m$.prepare(); m$init(m)
    } else if (op == "step") {
      m$.drawing <- list(); m$step(m); m$t <- m$t + 1
    } else if (op == "action") {
      m$.actions[[arg]]$fun(m)
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
    s$draw <- json(m$.drawing)
    m$.charts <- list(); m$.more_charts <- list()
    s
  }, envir = api)
})
