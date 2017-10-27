#See: http://stackoverflow.com/questions/4090169/elegant-way-to-check-for-missing-packages-and-install-them
#Author: Shane, http://stackoverflow.com/users/163053/shane

list.of.packages <- c("formatR", "rgdal","spatstat","knitr","rmarkdown","sp", "maptools", "rgdal", "tripack","spatstat","raster","ggplot2","profvis","DBI","odbc","shiny","R6")
new.packages <- list.of.packages[!(list.of.packages %in% installed.packages()[,"Package"])]
if(length(new.packages)) install.packages(new.packages,repos='http://cran.us.r-project.org')