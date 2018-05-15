# RScript to write functions and function names of loaded packages to a file!
# author: M. Austenfeld

.bio7WriteFunctionDef <- function() {
  # Next a function to extract the function arguments!
  getArgsFunc <- function(x) {
    return(tryCatch(args(x), error = function(e) "()"))
  }
  listArgs <- NULL
  listAllFuncs <- NULL
  listAllPack <- NULL
  allLoadedPack <- search()
  allLoadedPack <- allLoadedPack[grep("package:", allLoadedPack)]
  listFunctions <- lapply(allLoadedPack, ls)
  for (x in 1:length(listFunctions)) {
  	#Test if it is a function!
  	if (is.function(get(listFunctions[[x]]))) {
    listFunc <- listFunctions[[x]]
    listArgs <- c(listArgs, lapply(listFunc, getArgsFunc))
    listAllFuncs <- c(listAllFuncs, paste("(", allLoadedPack[x], ")", "####", listFunc))
    listAllPack <- c(listAllPack, listFunc)
    listArgs <- gsub("function", "", listArgs) # replace function string at the beginning!
    listArgs <- gsub("\nNULL", "", listArgs) # remove 'NULL' string at the end!
    listArgs <- gsub("\n", "", listArgs) # replace and avoid linebreak at the end of a function if very long for the post processing!
  	}
  }
  all <- paste(listAllPack, "####", listAllFuncs, listArgs, sep = "")
  all <- gsub(" ", "", all) # replace whitespace at beginning
  all <- grep("^[a-zA-Z]", all, value = TRUE) # extract those which starts with a letter!
  all <- sort(all)
  write(all, file = paste(.bio7TempEnvPath$pathTemp, "rproposals.txt", sep = ""))
}