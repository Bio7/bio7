# Copyright (c) 2007-2026 M. Austenfeld
# SPDX-License-Identifier: EPL-2.0
#
# The profiler of the R editor of Bio7 (in the environment .bio7prof): runs a script
# (or a selection of it) with the profiler of R (Rprof with line profiling, no
# package) and returns the data of the report (the times of the lines, a flame
# graph, the functions) and the marks of the editor as JSON.

.bio7prof <- new.env()

local(envir = .bio7prof, {

  esc <- function(s) {
    s <- gsub("\\", "\\\\", s, fixed = TRUE)
    s <- gsub("\"", "\\\"", s, fixed = TRUE)
    s <- gsub("\n", "\\n", s, fixed = TRUE)
    s <- gsub("\r", "\\r", s, fixed = TRUE)
    s <- gsub("\t", "\\t", s, fixed = TRUE)
    s <- gsub("[[:cntrl:]]", " ", s)
    paste0("\"", s, "\"")
  }

  # JSON: lists (named: objects), vectors of length 1 (scalars), I(): arrays
  json <- function(x) {
    if (is.null(x)) return("null")
    if (is.list(x)) {
      if (!is.null(names(x)) && all(names(x) != ""))
        return(paste0("{", paste0(esc(names(x)), ":", vapply(x, json, ""), collapse = ","), "}"))
      return(paste0("[", paste0(vapply(x, json, ""), collapse = ","), "]"))
    }
    if (inherits(x, "AsIs") || length(x) != 1)
      return(paste0("[", paste0(vapply(as.list(unclass(x)), json, ""), collapse = ","), "]"))
    if (is.na(x)) return("null")
    if (is.logical(x)) return(if (x) "true" else "false")
    if (is.numeric(x)) return(if (is.finite(x)) format(signif(x, 10), scientific = FALSE, trim = TRUE) else "null")
    esc(as.character(x))
  }

  profile <- function(path, code = NULL, first_line = 1L, interval = 0.005) {
    file <- path
    if (!is.null(code)) {
      # the selection with the line numbers of the file
      file <- tempfile(fileext = ".R")
      writeLines(c(rep("", max(first_line, 1L) - 1L), code), file)
      on.exit(unlink(file), add = TRUE)
    }
    out <- tempfile()
    on.exit(unlink(out), add = TRUE)
    message <- NULL
    start <- proc.time()[["elapsed"]]
    # the elapsed time (also waiting, e.g., Sys.sleep or reading files) if R has it (R >= 4.4), else the CPU time
    elapsed_time <- "event" %in% names(formals(Rprof))
    if (elapsed_time) {
      Rprof(out, interval = interval, line.profiling = TRUE, event = "elapsed")
    } else {
      Rprof(out, interval = interval, line.profiling = TRUE)
    }
    message <- tryCatch({
      source(file, local = globalenv(), keep.source = TRUE)
      NULL
    }, interrupt = function(e) "Stopped: the result up to the stop.",
       error = function(e) {
         msg <- conditionMessage(e)
         base::message("Error: ", msg)
         paste("The script ended with an error (the result up to the error):", msg)
       })
    Rprof(NULL)
    elapsed <- proc.time()[["elapsed"]] - start
    if (!elapsed_time) {
      cpu <- "The CPU time is sampled (R < 4.4): waiting (e.g., Sys.sleep, reading files) is not included."
      message <- if (is.null(message)) cpu else paste(message, cpu)
    }
    analyze(out, path, file, elapsed, !is.null(code), message)
  }

  analyze <- function(out, path, file, elapsed, selection, message) {
    raw <- readLines(out, warn = FALSE)
    ivl <- 0.005
    m <- regmatches(raw[1], regexec("sample.interval=([0-9]+)", raw[1]))[[1]]
    if (length(m) == 2) ivl <- as.numeric(m[2]) / 1e6
    # the files of the source references (the script: the number of its file)
    files <- grep("^#File ", raw, value = TRUE)
    ids <- sub("^#File ([0-9]+): .*$", "\\1", files)
    names_ <- sub("^#File [0-9]+: ", "", files)
    own <- ids[normalizePath(names_, mustWork = FALSE) == normalizePath(file, mustWork = FALSE) | basename(names_) == basename(file)]
    samples <- raw[-1]
    samples <- samples[nzchar(samples) & !startsWith(samples, "#")]
    incl <- numeric(0); self <- numeric(0)
    stacks <- list(); weights <- numeric(0)
    for (line in samples) {
      tokens <- scan(text = line, what = "", quiet = TRUE)
      # from the innermost to the outermost call: a reference (file#line) before its function
      frames <- list(); ref <- ""
      first_ref <- ""
      for (t in tokens) {
        if (grepl("^[0-9]+#[0-9]+$", t)) {
          ref <- t
          if (!nzchar(first_ref)) first_ref <- t
        } else {
          frames[[length(frames) + 1L]] <- c(t, ref)
          ref <- ""
        }
      }
      if (nzchar(ref)) frames[[length(frames) + 1L]] <- c("<top>", ref)
      frames <- rev(frames)
      # the byte compiler of R (it compiles functions and loops when they run): one frame
      jit <- which(vapply(frames, function(f) f[1] == "compiler:::tryCompile", FALSE))
      if (length(jit) > 0) {
        k <- jit[1]
        while (k > 1 && frames[[k - 1]][1] %in% c("doTryCatch", "tryCatchOne", "tryCatchList", "tryCatch") && !nzchar(frames[[k - 1]][2])) k <- k - 1
        frames <- c(frames[seq_len(k - 1)], list(c("<byte compiler>", "")))
      }
      # from the first frame of the script (not the code which started it)
      in_script <- vapply(frames, function(f) nzchar(f[2]) && sub("#.*", "", f[2]) %in% own, FALSE)
      if (!any(in_script)) next
      frames <- frames[which(in_script)[1]:length(frames)]
      lines <- unique(vapply(frames, function(f) if (nzchar(f[2]) && sub("#.*", "", f[2]) %in% own) sub(".*#", "", f[2]) else NA_character_, ""))
      lines <- lines[!is.na(lines)]
      incl[lines] <- ifelse(is.na(incl[lines]), 0, incl[lines]) + ivl
      if (nzchar(first_ref) && sub("#.*", "", first_ref) %in% own) {
        l <- sub(".*#", "", first_ref)
        self[l] <- ifelse(is.na(self[l]), 0, self[l]) + ivl
      }
      frames[[1]][1] <- "<script>"
      key <- paste(vapply(frames, paste, "", collapse = "@"), collapse = "|")
      if (is.null(stacks[[key]])) {
        stacks[[key]] <- frames
        weights[key] <- 0
      }
      weights[key] <- weights[key] + ivl
    }
    sampled <- sum(weights)
    # the flame graph: a tree of the calls (function, line of the call)
    new_node <- function(n, f, l, own) list(n = n, f = f, l = l, own = own, v = 0, c = list())
    root <- new_node("all", "", 0, 0)
    add <- function(node, frames, w) {
      node$v <- node$v + w
      if (length(frames) > 0) {
        f <- frames[[1]]
        lf <- if (nzchar(f[2])) sub(".*#", "", f[2]) else "0"
        is_own <- nzchar(f[2]) && sub("#.*", "", f[2]) %in% own
        key <- paste(f[1], f[2])
        child <- node$c[[key]]
        if (is.null(child)) child <- new_node(f[1], if (is_own) basename(path) else if (nzchar(f[2])) "R" else "built-in", as.integer(lf), as.integer(is_own))
        node$c[[key]] <- add(child, frames[-1], w)
      }
      node
    }
    for (key in names(stacks)) root <- add(root, stacks[[key]], weights[[key]])
    compact <- function(node, depth = 0) {
      kids <- node$c
      if (length(kids) > 0) {
        kids <- kids[order(-vapply(kids, function(k) k$v, 0))]
        if (depth > 0) kids <- kids[vapply(kids, function(k) k$v >= sampled * 0.0005, FALSE)]
      }
      list(node$n, round(node$v * 1000, 3), node$f, node$l, node$own, lapply(unname(kids), compact, depth = depth + 1))
    }
    # the functions: total time (a function in the stack) and self time (the innermost)
    tot <- numeric(0); slf <- numeric(0); mine <- character(0)
    for (key in names(stacks)) {
      fr <- stacks[[key]]
      fn <- unique(vapply(fr, function(f) f[1], ""))
      fn <- fn[fn != "<script>"]
      tot[fn] <- ifelse(is.na(tot[fn]), 0, tot[fn]) + weights[[key]]
      inner <- fr[[length(fr)]][1]
      if (inner != "<script>") slf[inner] <- ifelse(is.na(slf[inner]), 0, slf[inner]) + weights[[key]]
      for (f in fr) if (nzchar(f[2]) && sub("#.*", "", f[2]) %in% own) mine <- c(mine, f[1])
    }
    functions <- lapply(names(sort(tot, decreasing = TRUE)), function(fn) {
      list(fn, if (fn %in% mine) basename(path) else "", NULL, NULL, NULL,
           round(ifelse(is.na(slf[fn]), 0, slf[fn]) * 1000, 3), round(tot[[fn]] * 1000, 3), as.integer(fn %in% mine))
    })
    if (length(functions) > 400) functions <- functions[1:400]
    # the source lines of the file
    src <- readLines(path, warn = FALSE)
    source_rows <- lapply(seq_along(src), function(i) {
      k <- as.character(i)
      list(i, round(ifelse(is.na(incl[k]), 0, incl[k]) * 1000, 3), round(ifelse(is.na(self[k]), 0, self[k]) * 1000, 3), src[i])
    })
    # the marks of the editor: the lines with at least 1 % of the time
    marks <- list()
    if (sampled > 0 && length(incl) > 0) {
      hot <- incl[incl >= sampled * 0.01]
      hot <- hot[order(-hot)]
      if (length(hot) > 60) hot <- hot[1:60]
      marks <- lapply(names(hot), function(k) list(as.integer(k), round(hot[[k]] * 1000, 3), round(ifelse(is.na(self[k]), 0, self[k]) * 1000, 3)))
    }
    json(list(name = basename(path), selection = selection, elapsed = elapsed, sampled = sampled, samples = round(sampled / ivl),
              interval = ivl, message = message, source = source_rows, flame = compact(root), functions = functions, marks = marks))
  }
})
