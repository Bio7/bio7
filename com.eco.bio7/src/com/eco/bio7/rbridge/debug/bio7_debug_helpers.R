# ---------------------------------------------------------------------------
# Bio7 R debug helpers.
#
# Verified design (see investigation):
#  - The user's script is parsed UNMODIFIED (no physical-line injection ->
#    multi-line statements are safe) and wrapped into a 0-arg function body so
#    R's stepping works inside it.
#  - Per-statement source lines come from attr(parse(...), "srcref") at SETUP
#    time (reliable). Runtime srcref inside a trace() tracer is NOT reliable in
#    all R builds, so instead each statement gets its OWN tracer with the source
#    line and break decision baked in as literals (.bio7_make_tracer).
#  - trace(fn, at = <stmt indices>, tracer = <list of per-stmt tracers>,
#    print = FALSE) fires before each statement; the tracer writes
#    @@BIO7@@LINE=<srcline>@@ and variables to a state file that Java reads.
#    No console-banner scraping, no sockets, no sink. Locale-independent.
#
# State file format (fixed ASCII):
#   @@BIO7@@LINE=<originalSourceLine>@@
#   @@BIO7@@BEGIN@@
#   <name>\t<value>
#   @@BIO7@@END@@
#
# Globals (set/updated by Java via single-line commands):
#   .bio7_state_path  - file Java reads (set once at session start)
#   .bio7_line_map    - statement-index -> source line (integer vector)
#   .bio7_break_set   - integer statement indices that are breakpoints
#   .bio7_mode        - "run"  : browse only at breakpoint statements
#                       "step" : browse at every statement (Next/Step Into)
# ---------------------------------------------------------------------------

.bio7_state_path <- NULL
.bio7_line_map   <- NULL
.bio7_break_set  <- NULL
.bio7_mode       <- "run"

# Write the current source line + variables of environment 'e' to the state file.
.bio7_write <- function(line, e) {
  path <- get0(".bio7_state_path", ifnotfound = NULL)
  if (is.null(path)) return(invisible(NULL))
  con <- file(path, open = "wt", encoding = "UTF-8")
  on.exit(close(con), add = TRUE)
  cat(sprintf("@@BIO7@@LINE=%d@@\n", as.integer(line)), file = con)
  writeLines("@@BIO7@@BEGIN@@", con)
  for (v in ls(envir = e)) {
    val <- tryCatch(
      paste(utils::capture.output(utils::str(get(v, envir = e))), collapse = " "),
      error = function(err) "<unreadable>"
    )
    val <- gsub("[\t\r\n]+", " ", val)   # keep each record on one physical line
    writeLines(sprintf("%s\t%s", v, val), con)
  }
  writeLines("@@BIO7@@END@@", con)
}

# Build a tracer expression for ONE statement. The source line is baked in as a
# literal integer and whether this statement is a breakpoint is baked in as a
# literal logical, so NO runtime srcref / map lookup is needed (those proved
# unreliable inside trace()). In "step" mode every statement browses; otherwise
# only statements flagged as breakpoints browse.
.bio7_make_tracer <- function(srcLine, isBreak) {
  bquote({
    .b7env <- environment()
    .bio7_write(.(as.integer(srcLine)), .b7env)
    if (identical(.bio7_mode, "step") || .(isTRUE(isBreak))) browser()
  })
}

# Parse the user's script, derive per-statement source lines from the PARSE
# srcref, build the 0-arg debug function, map breakpoints, and install one
# tracer per statement. 'breakLines' are ORIGINAL source line numbers chosen by
# the user in the editor.
.bio7_setup <- function(scriptPath, breakLines) {
  options(keep.source = TRUE)

  exprs <- parse(scriptPath, keep.source = TRUE)

  ## Per-statement source lines from the parse srcref (reliable at setup time).
  srl <- attr(exprs, "srcref")
  lines <- integer(0)
  if (!is.null(srl)) {
    lines <- vapply(srl, function(s) as.integer(utils::getSrcLocation(s, "line")),
                    integer(1))
  } else {
    ## Fallback: parse data line numbers for top-level expressions.
    pd <- utils::getParseData(exprs)
    if (!is.null(pd) && nrow(pd)) {
      top <- pd[pd$parent <= 0 & pd$terminal == FALSE, , drop = FALSE]
      if (nrow(top)) lines <- as.integer(top$line1)
    }
  }
  ## Last-resort fallback so stepping still works even without any srcref.
  if (!length(lines)) lines <- seq_along(exprs)
  assign(".bio7_line_map", lines, envir = globalenv())

  ## Build the debug function from the same parsed expressions.
  fnbody <- as.call(c(as.name("{"), as.list(exprs)))
  fn     <- eval(call("function", as.pairlist(NULL), fnbody))
  environment(fn) <- globalenv()
  assign(".bio7debugfunc", fn, envir = globalenv())

  ## Map break source lines -> statement indices (nearest statement at/after).
  bset <- integer(0)
  if (length(breakLines)) {
    for (bl in breakLines) {
      idx <- which(lines == bl)
      if (!length(idx)) {
        after <- which(lines >= bl)
        if (length(after)) idx <- after[1L]
      }
      if (length(idx)) bset <- c(bset, idx[1L])
    }
  }
  bset <- sort(unique(bset))
  assign(".bio7_break_set", bset, envir = globalenv())
  assign(".bio7_mode", "run", envir = globalenv())

  ## One tracer PER statement, each with its line + break flag baked in.
  at      <- as.list(seq_along(lines))
  tracers <- lapply(seq_along(lines), function(k) {
    .bio7_make_tracer(lines[k], k %in% bset)
  })

  suppressMessages(
    trace(.bio7debugfunc, tracer = tracers, at = at, print = FALSE)
  )
  invisible(length(lines))
}

# Remove tracing and clear any leftover sink so console output is never swallowed.
.bio7_teardown <- function() {
  if (exists(".bio7debugfunc", envir = globalenv())) {
    suppressMessages(try(untrace(.bio7debugfunc), silent = TRUE))
  }
  while (sink.number() > 0) sink()
  invisible(NULL)
}