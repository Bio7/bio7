# RScript to write functions and function names of loaded packages to a file!
# author: M. Austenfeld

.bio7WriteFunctionDef <- function() {
      
    .listOfArguments <- NULL
    .bio7listAllFunctions <- NULL
    .bio7ListAllLoadedFunction <- NULL
    .bio7AllLoadedPackages <- search()
    .bio7AllLoadedPackages <- .bio7AllLoadedPackages[grep("package:", .bio7AllLoadedPackages)]
    # .bio7AllLoadedPackages<-.packages()#list of loaded packages
    listOfFunctions <- lapply(.bio7AllLoadedPackages, ls)
    
    .bio7TestFunction <- function(x) {
        return(tryCatch(args(x), error = function(e) "()"))
    }
    
    for (x in 1:length(listOfFunctions)) {
        
        listFunc <- listOfFunctions[[x]]
        .listOfArguments <- c(.listOfArguments, lapply(listFunc, .bio7TestFunction))
        # print(paste(.listOfArguments,.bio7AllLoadedPackages[x]))
        .bio7listAllFunctions <- c(.bio7listAllFunctions, paste("(", .bio7AllLoadedPackages[x], ")", "####", listFunc))
        .bio7ListAllLoadedFunction <- c(.bio7ListAllLoadedFunction, listFunc)
        .listOfArguments <- gsub("function", "", .listOfArguments)  #replace function string at the beginning!
        .listOfArguments <- gsub("\nNULL", "", .listOfArguments)  #remove 'NULL' string at the end!
        .listOfArguments <- gsub("\n", "", .listOfArguments)  #replace and avoid linebreak at the end of a function if very long for the post processing!
        
    }
    all <- paste(.bio7ListAllLoadedFunction, "####", .bio7listAllFunctions, .listOfArguments, sep = "")
    all <- gsub(" ", "", all)  #replace whitespace at beginning
    all<-grep("^[a-zA-Z]",all,value=TRUE)# extract those which starts with a letter!
    all <- sort(all)
    
    write(all, file = paste(.bio7TempEnvPath$pathTemp, "rproposals.txt", sep = ""))
    
} 