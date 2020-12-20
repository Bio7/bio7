# We use the lib package randomForest
library(randomForest)
# Predict the whole image (select your test data in the R-Shell view)!
# final<-predict(rf_model,get(.r_shell_vars[[1]]))

# Check if trained model is available for classifcation!
if (exists("rf_model")) {
    final <- predict(rf_model, current_feature_stack)
    # We convert the votes back to numeric matrix values!
    imageMatrix <- matrix(as.integer(final), imageSizeX, imageSizeY)  #Create a image matrix
    # Here we plot the result with R!
    # image(1:imageSizeX,1:imageSizeY,imageMatrix,xlim=c(1,imageSizeX),ylim=c(imageSizeY,1),axes
    # = T,useRaster=TRUE)
} else {
	#Set matrix to NULL to avoid transfer to ImageJ
	imageMatrix=NULL;
	cat(paste("No trained model available! " ,sep="\n"))
}

