# RScript to write functions and function names of loaded packages to a file!
# author: M. Austenfeld

.bio7WriteFunctionDef <- function() {
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
  listArgs <- gsub("function", "", listArgs) # replace function string at the beginning!
  listArgs <- gsub("\nNULL", "", listArgs) # remove 'NULL' string at the end!
  listArgs <- gsub("\n", "", listArgs) # replace and avoid linebreak at the end of a function if very long for the post processing!
  all <- paste(listAllPack, "####", listAllFuncs, listArgs, sep = "")
  all <- gsub(" ", "", all) # replace whitespace at beginning
  all <- grep("^[a-zA-Z]", all, value = TRUE) # extract those which starts with a letter!
  all <- sort(all)
  write(all, file = paste(.bio7TempEnvPath$scriptFolder, "rproposals.txt", sep = ""))
}
