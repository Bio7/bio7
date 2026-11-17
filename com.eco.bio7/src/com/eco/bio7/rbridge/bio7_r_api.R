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
    function(...) jcall("java_call", c(cls, name, if (ui) "ui" else ""),
                        list(args = jargs(list(...)), target = target))
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
", sep = "")
    invisible(NULL)
  }
})
