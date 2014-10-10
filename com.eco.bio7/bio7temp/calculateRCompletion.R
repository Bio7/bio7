# RScript to write functions and function names of loaded packages to a file!
# author: M. Austenfeld
writeFunctionDef<-function() {
	

listOfArguments <- NULL
allFunc<-NULL
funcName<-NULL
pkgs <- search()
pkgs <- pkgs[grep("package:", pkgs)]
#pkgs<-.packages()#list of loaded packages
listOfFunctions <- lapply(pkgs, ls)

testFunction <- function(x) {
    return(tryCatch(args(x), error = function(e) "()"))
}

for (x in 1:length(listOfFunctions)) {
   
    listFunc <- listOfFunctions[[x]]
    listOfArguments <- c(listOfArguments, lapply(listFunc, testFunction))
    # print(paste(listOfArguments,pkgs[x]))
    allFunc<-c(allFunc,paste("(",pkgs[x],")","####",listFunc))
    funcName<-c(funcName,listFunc)
    listOfArguments<-gsub("function", "", listOfArguments)#replace function string at the beginning!
    listOfArguments<-gsub("\nNULL", "", listOfArguments)#remove 'NULL' string at the end!
    listOfArguments<-gsub("\n", "", listOfArguments)#replace and avoid linebreak at the end of a function if very long for the post processing!
    
}
all<-paste(funcName,"####",allFunc,listOfArguments, sep = "")
all<-gsub(" ", "", all)#replace whitespace at beginning
all<-sort(all)

write(all,file = paste(.bio7tempenvpath$pathTemp,"rproposals.txt",sep=''))

}
