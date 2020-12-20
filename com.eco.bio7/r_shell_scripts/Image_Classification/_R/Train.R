# We use the lib package randomForest
library(randomForest)

#Combine matrices which represent a special signature (class).
#Train with the selection matrices (layer=colors,features, etc.) combined in one matrix
dftrain<-NULL
#Here we combine all R-Shell view selections!
for (i in 1:length(.r_shell_vars)) {
	dftrain<-rbind(dftrain,get(.r_shell_vars[[i]]))
}

#We convert the 'Class' column values to a factor for a classification else a regession forest will be built!
# The other columns (denoted by the point) are used as the dependent data!
rf_model<-randomForest(as.factor(Class)~.,data=dftrain,ntree=200)
print(rf_model)
#The same as above:
#res<-randomForest(as.factor(dftrain[,1])~dftrain[,-1],data=dftrain,ntree=64)
