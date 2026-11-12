# RScript to write functions and function names of loaded packages to a file!
# author: M. Austenfeld

# packages: further packages (e.g., of an opened script) which are only loaded
# (requireNamespace), not attached: the search path of the session doesn't change.
# They are remembered for the next calls (also without the argument).
.bio7WriteFunctionDef <- function(packages = character(0)) {
  # Next a function to extract the function arguments!
  getArgsFunc <- function(x) {
    return(tryCatch(args(x), error = function(e) "()"))
  }
  allLoadedPack <- search()
  allLoadedPack <- allLoadedPack[grep("package:", allLoadedPack)]
  listFunctions <- lapply(allLoadedPack, ls)
  # The names, the package of each name and the arguments (the vectors are built once, not in the loop)!
  listAllPack <- unlist(listFunctions, use.names = FALSE)
  listAllFuncs <- unlist(lapply(seq_along(listFunctions), function(x) {
    if (length(listFunctions[[x]]) == 0) character(0) else paste("(", allLoadedPack[x], ")", "####", listFunctions[[x]])
  }), use.names = FALSE)
  listArgs <- lapply(listAllPack, getArgsFunc)
  # The packages in 'Depends' (recursively): library() attaches them too (e.g., 'spatstat' attaches
  # 'spatstat.geom', 'spatstat.explore', ..., which contain its functions)!
  dependsOf <- function(p) {
    d <- suppressWarnings(tryCatch(utils::packageDescription(p, fields = "Depends"), error = function(e) NA))
    if (is.null(d) || length(d) == 0 || is.na(d)) return(character(0))
    d <- trimws(sub("\\(.*", "", strsplit(gsub("\n", " ", d), ",")[[1]]))
    setdiff(d[nzchar(d)], "R")
  }
  withDepends <- function(pk) {
    seen <- character(0)
    queue <- pk
    while (length(queue) > 0) {
      p <- queue[1]
      queue <- queue[-1]
      if (p %in% seen) next
      seen <- c(seen, p)
      queue <- c(queue, dependsOf(p))
    }
    seen
  }
  # The exported functions and the datasets of the further packages (installed ones; not the attached ones, they are listed above)!
  extra <- withDepends(unique(c(get0("completionPackages", envir = .bio7TempEnvPath, inherits = FALSE, ifnotfound = character(0)), packages)))
  extra <- extra[vapply(extra, function(p) isTRUE(suppressWarnings(suppressMessages(requireNamespace(p, quietly = TRUE)))), logical(1))]
  assign("completionPackages", extra, envir = .bio7TempEnvPath)
  for (p in setdiff(extra, sub("^package:", "", allLoadedPack))) {
    exports <- tryCatch(getNamespaceExports(p), error = function(e) character(0))
    # Datasets (lazy data, as after library()): only the names, the data are not loaded!
    datasets <- tryCatch(ls(getNamespaceInfo(p, "lazydata")), error = function(e) character(0))
    datasets <- setdiff(datasets, exports)
    if (length(exports) + length(datasets) == 0) next
    listAllPack <- c(listAllPack, exports, datasets)
    listAllFuncs <- c(listAllFuncs, paste("(", paste0("package:", p), ")", "####", c(exports, datasets)))
    listArgs <- c(listArgs, lapply(exports, function(n) tryCatch(args(getExportedValue(p, n)), error = function(e) "()")),
                  rep(list(NULL), length(datasets)))
  }
  listArgs <- gsub("function", "", listArgs) # replace function string at the beginning!
  listArgs <- gsub("\nNULL", "", listArgs) # remove 'NULL' string at the end!
  listArgs <- gsub("\n", "", listArgs) # replace and avoid linebreak at the end of a function if very long for the post processing!
  all <- paste(listAllPack, "####", listAllFuncs, listArgs, sep = "")
  all <- gsub(" ", "", all) # replace whitespace at beginning
  all <- grep("^[a-zA-Z]", all, value = TRUE) # extract those which starts with a letter!
  all <- sort(all)
  write(all, file = paste(.bio7TempEnvPath$scriptFolder, "rproposals.txt", sep = ""))
}
