# The package lists of the Bio7 package views (Rserve and embedded R).
# The index of a repository is cached on disk (24 hours), shared by the views
# and by new R sessions: only the first use of a day downloads it.
# author: M. Austenfeld

.bio7AvailablePackages <- function(repos, type = getOption("pkgType"), maxAge = 24 * 3600, refresh = FALSE) {
  dir <- tryCatch(tools::R_user_dir("Bio7", "cache"), error = function(e) tempdir())
  file <- file.path(dir, paste0("packages_", gsub("[^A-Za-z0-9]+", "_", repos), "_", type, "_",
    getRversion(), "_", R.version$platform, ".rds"))
  if (!refresh && file.exists(file) && difftime(Sys.time(), file.mtime(file), units = "secs") < maxAge) {
    av <- tryCatch(readRDS(file), error = function(e) NULL)
    if (is.matrix(av) && nrow(av) > 0) return(av)
  }
  av <- if (identical(type, "source")) {
    available.packages(contriburl = contrib.url(repos, type = "source"))
  } else {
    available.packages(repos = repos, type = type)
  }
  if (nrow(av) > 0) {
    try({
      dir.create(dir, recursive = TRUE, showWarnings = FALSE)
      saveRDS(av, file)
    }, silent = TRUE)
  }
  av
}

# The installed packages (sorted): Name, Current_Version
.bio7InstalledPackages <- function() {
  ip <- installed.packages()
  o <- order(ip[, "Package"])
  data.frame(Name = as.character(ip[o, "Package"]), Current_Version = as.character(ip[o, "Version"]),
    stringsAsFactors = FALSE)
}

# The installed packages with the latest version of the repository: Name, Current_Version, Latest_Version
.bio7PackageVersions <- function(repos) {
  df <- .bio7InstalledPackages()
  latest <- df$Current_Version
  old <- tryCatch(old.packages(repos = repos, available = .bio7AvailablePackages(repos)), error = function(e) NULL)
  if (!is.null(old) && nrow(old) > 0) {
    m <- match(df$Name, old[, "Package"])
    latest[!is.na(m)] <- as.character(old[m[!is.na(m)], "ReposVer"])
  }
  df$Latest_Version <- latest
  df
}
