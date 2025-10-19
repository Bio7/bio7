/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/


package com.eco.bio7.rbridge;

/*The class is shared by the PrintExpression and PrintR actions.*/
public class RFunctions {
	private static RFunctions propsHistInstance;
	
	
	public String[] variables={"ls()","objects()","help.start()","?aov","dir()","memory.size()","memory.limit()","memory.profile()","gc()"};
	public String[] variablesContext={"Returns the list of all\navailable variables(objects) in the current workspace!",
			"Returns the list of all\navailable objects in the current workspace!","Starts the html help in a browser!",
			"Opens the html help for the specified method!","List the files in the current directory!",
			"Reports the current or maximum memory allocation \nof the malloc function used in this version of R!",
			"Reports or increases the limit\nin force on the total allocation!",
			"Lists the usage of the cons cells by SEXPREC type!","A call of gc causes the garbage collection to take place!"};
	
	public String[] data={"x<-c(32.82,8.76,12.03,18.43,38.61,27.48)",
			"y<-c(84.35,87.56,12.34,11.41 ,12.68,26.52)",
			"z<-c(67.71,56.51,58.23,32.32,98.19,65.05)",
			"x[4]","x[1:4]","x[-(1:4)]","x[c(1,2,6)]","x[x >= 4]","x[x>2&x<8]",
			"rep(c(2,3,4),2)","seq(1,10,2)","a1<-array(1:16, c(2,8))","a2<-array(1:16, c(2,4,2))",
			"m<-matrix(c(29,26,58,53,61,54),2,3)",
			"df<-data.frame(x,y)","population<-data.frame(species=c(\"a\",\"b\",\"c\"),age=c(2,3,5))",
			"df[1,]","df[,1]","df[[1,1]]","sum(df[1,])","df[x>mean(x)&y<30,]",
			"df<-cbind(df,x,y,z)","df<-rbind(df,c(2,2),c(3,3))",
			"apply(df,MARGIN=c(1,2),sqrt)","apply(df,MARGIN=c(1,2),function(x){x^2})",			
			"sapply(list(x,y,z),mean)","lapply(x,function(x){x^2/5})",
			"length(x)",		
			"func<-function(x){x^2}","apply(df,MARGIN=c(1,2),func)",			
			"attach(df)","table(x)","tapply(x,MARGIN=c(1,2),mean)",	
			"random<-100*(runif(100))",
			"spl<-sample(1:10,4)","spl<-sample(1:10,4,replace=TRUE)","rep(2,100)",
			"unique(x)","na.omit(x)","which(x<32)","which(matrix==8,arr.ind=TRUE)"};
	public String[] dataContext={"Concatenates given values to a vector","Concatenates given values to a vector","Concatenates given values to a vector",
			"Returns the i-th value of a vector","Returns the 1-4 values of the vector","Returns the values of the vector without the 1-4 values","Returns the values of a vector which index is given as a vector","Returns the values of the vector which matches the condition","Returns the values of the vector which matches the condition",
			"The function rep replicates the given argument(x) n-times","seq generates regular sequences","Creates a 2D array from the specified arguments","Creates a 3D array from the specified arguments",
			"Creates a matrix of size n*m from the given values",
			"Creates a dataframe from the n-given vectors","Creates a dataframe from the n-given vectors (with column names)",
			"Returns the i-th row as a vector","Returns the i-th column as a vector","Returns the i-th value of the dataframe","Calculates the sum of the i-th row","Returns the values which match the given conditions",
			"Adds the specified vectors as columns to the given dataframe","Adds the specified vectors as rows to the given dataframe",
			"Applys a function to each value of the given datframe","Applys a custom function to each value of the given dataframe",
			"apply applys a function over a List or a Vector.\nsapply is a user-friendly version of lapply by default returning a vector or matrix if appropriate",
			"lapply returns a list of the same length as X, each\nelement of which is the result of applying FUN to the corresponding element of X","Returns the length of the given vector"
			,"Defines a function","apply applys a function over a List or a Vector","Attaches a set of R Objects to the search path. This means that the database \n(data.frame,list,R,NULL,environment) is searched by R \nwhen evaluating a variable, so objects in the database (in this example the columns) can \nbe accessed by simply giving their names",
			"table uses the cross-classifying factors to \nbuild a contingency table of the counts at\n each combination of factor levels","Applys a function to each cell of a ragged array, \nthat is to each (non-empty) group of values given \nby a unique combination of the levels of certain factors",
			"Creates a vector with random uniform distributed numbers (multiplied with 100)","sample takes a sample of the specified size \nfrom the elements of x using either with or without replacement","sample takes a sample of the specified size \nfrom the elements of x using either with or without replacement","rep replicates the values in x",
			"unique returns a vector, data frame or array like x \nbut with duplicate elements/rows removed","na.omit returns the object (e.g vector,dataframe) \nwith incomplete cases removed",
			"Gives the TRUE indices of a logical object, \nallowing for array indices","Gives the TRUE indices of a logical object. If arr.ind == TRUE and x is an array (has a dim attribute), \nthe result is a matrix whose rows each are the indices of one element of x"};
	
	
	public String[] math={"x+x","x-x","x*x","x/x","x^x","x%%x",
			"sqrt(x)","sqrt(x,3)", "exp(x)", "sin(x)", "cos(x)",
			"tan(x)", "asin(x)","acos(x)", "atan(x)", "atan2(x)","log(x)",
			"log2(x)", "log10(x)","log(x,base)","round(x)","round(x, digits = 2)",
			"signif(x, digits=2)","trunc(x)","abs(x)",
			"ceiling(x)", "floor(x)", "pi","cumsum(x)","cumprod(x)","cummax(x)","cummin(x)",
			"max(x)", "min(x)", "range(x)", "sum(x)","diff(x)","prod(x)",
			"derivative<-D(expression(x^2+5*x),\"x\")","integrate(function(x){x^2}, -1.96, 1.96)",
			"integrate(dnorm, -1.96, 1.96)","sum<-sum(x[i],i=1,5)",
			"fft(x)","fft(x,inverse=TRUE)","mvfft(x)"};
	public String[] mathContext={"+","-","Product","Division","Exponent x","Modulo",
			"The square root of the argument","The 3-root of the argument","Computes the exponential function","Computes the sine of the given argument","Computes the cosine of the given argument",
			"Computes the tangent of the given argument","Computes the arc-sine of the given argument","Computes the arc-cosine of the given argument","Computes the arc-tangent of the given argument","Computes the the two-argument arc-tangent of the given argument","Computes the natural logarithm of the given argument",
			"Computes the binary logarithm of the given argument","Computes the base 10 logarithm of the given argument","Computes the based (x) logarithm of the given argument","Rounds the values in its first argument to the default (0) number of decimal places","Rounds the values in its first argument\n to the specified number of decimal places",
			"Rounds the values in its first argument \nto the specified number of significant digits","Takes a single numeric argument x and returns a \nnumeric vector containing the integers \nformed by truncating the values in x toward 0","abs(x) returns an integer vector when x is integer or logical",
			"ceiling takes a single numeric argument x and \nreturns a numeric vector containing the smallest\n integers not less than the corresponding elements of x","floor takes a single numeric argument x and \nreturns a numeric vector containing the largest integers \nnot greater than the corresponding elements of x","The ratio of the circumference of a circle to its diameter","Returns a vector whose elements are the cumulative sums of the elements of the argument","Returns a vector whose elements are the cumulative products of the elements of the argument","Returns a vector whose elements are the cumulative maxima of the elements of the argument","Returns a vector whose elements are the cumulative minima of the elements of the argument",
			"max returns the maximum of all the values present in their arguments","min returns the minimum of all the values present in their arguments","range returns a vector containing the minimum and maximum of all the given arguments","sum returns the sum of all the values present in its arguments","Returns suitably lagged and iterated differences","prod returns the product of all the values present in its arguments",
			"D computes derivatives of simple expressions, symbolically","Adaptive quadrature of functions of one variable over a finite or infinite interval",
			"Adaptive quadrature of functions of one variable over a finite or infinite interval","sum returns the sum of all the values present in its arguments","Performs the Fast Fourier Transform of an array","Performs the inverse Fast Fourier Transform of an array","mvfft takes a real or complex matrix as argument, \nand returns a similar shaped matrix, but with each \ncolumn replaced by its discrete Fourier transform"};
	
	public String[] statistics={"mean(x)", "median(x)","range(x)","scale(x,center=TRUE)",
			"var(x)","cov(x)","cor(x,y)","sd(x)","summary(x)", "quantile(x)","rm(x)",
			"t.test(x)","t.test(x,y)","t.test(x,y,paired=TRUE)",
			"fisher.test(x)","chisq.test(x)","chisq.test(x,y)","shapiro.test(x)","bartlett.test(y~x)",
			"wilcox.test(x, y, alternative = \"g\")","kruskal.test(list(x, y, z))",
			"ks.test(x)",
			"fx<-as.factor(x)",
			"aov(y~x)","summary(aov(y~x))","pairwise.t.test(y,x,p.adj = \"none\")",
			"pairwise.t.test(y,x,p.adj = \"bonf\")","TukeyHSD(aov(y~x))",
			"cor.test (x,y,method=\"s\")","cor.test (x,y,method=\"p\")","lm(y~x)","summary(lm(y~x))",
			"-----Multivariate------","hca <- hclust(dist(USArrests))","cl <- kmeans(iris[,1:4], 3)",
			"princomp(USArrests, cor = TRUE)",
			"loc <- cmdscale(eurodist)",
			"fit <- rpart(Kyphosis ~ Age + Number + Start, data=kyphosis)"};
	
	public String[] statisticsContext={"Computes the mean value of the given argument","Compute the sample median","range returns a vector containing the minimum and maximum of all the given arguments","scale is generic function whose default method \ncenters and/or scales the columns of a numeric matrix",
			"Computes the variance of x","Computes the covariance of x","Computes the correlation of x and y","This function computes the standard deviation of the values in x","This function computes summary statistics of the given argument","The generic function quantile produces sample \nquantiles corresponding to the given probabilities","remove and rm can be used to remove objects",
			"Performs a one sample t-test on vectors of data","Performs a two sample t-test on vectors of data","Performs a paired two sample t-test on vectors of data",
			"Performs Fisher's exact test for \ntesting the null of independence of rows and columns \nin a contingency table with fixed marginals","chisq.test performs chi-squared contingency table tests and goodness-of-fit tests","chisq.test performs chi-squared contingency table tests and goodness-of-fit tests","Performs the Shapiro-Wilk test of normality","Performs Bartlett's test (homogeneity of variances) \nof the null that the variances in each of the groups (samples) are the same",
			"Performs one and two sample Wilcoxon tests on vectors of data; \nthe latter is also known as �Mann-Whitney� test","Performs a Kruskal-Wallis rank sum test",
			"Performs one or two sample Kolmogorov-Smirnov tests","Converts the given argument to a factor","Performs an Analysis of Variance","Performs an Analysis of Variance\nand returns a summary","Calculate pairwise comparisons between group levels with corrections for multiple testing","Calculate pairwise comparisons between group levels with corrections for multiple testing","Create a set of confidence intervals on the \ndifferences between the means of the levels of a factor with \nthe specified family-wise probability of coverage",
			"Test for association between paired samples, \nusing one of Pearson's product moment correlation coefficient, \nKendall's tau or Spearman's rho","Test for association between paired samples, \nusing one of Pearson's product moment correlation coefficient, \nKendall's tau or Spearman's rho","lm is used to fit linear models. \nIt can be used to carry out regression, \nsingle stratum analysis of variance and \nanalysis of covariance","lm is used to fit linear models. \nIt can be used to carry out regression, \nsingle stratum analysis of variance and \nanalysis of covariance.\nWith the function summary a summary is displayed",
			"-----Multivariate------","Performs a Cluster Analysis \nwith the calculated distances of\n the available specified dataset","Clusters the given data \nwith the kmeans algorithm",
			"princomp performs a principal components analysis \non the given numeric data matrix and returns \nthe results as an object of class princomp",
			"Classical multidimensional scaling of a data matrix. \nAlso known as principal coordinates analysis",
			"Constructs a classification tree. \nThe library rpart has to be loaded first!"};
	
	public String[] spatialStats={"library(spatstat)", "library(spatial)","nndist(x,y)",
			"pairdist(x,y)","exactdt(x,y)","rpoint(100)",
			"X<- ppp(x, y, c(0,100), c(0,100))","X<-ppp(runif(100),runif(100),window=owin(xrange=c(0,100),yrange=c(0,100)))","X<-ppp(Particles$X,Particles$Y,window=owin(xrange=c(0,imageSizeX),yrange=c(0,imageSizeY)))","summary(X)","pp <- runifpoint(50)","K <- Kest(pp)","nearest.raster.point(0.5,0.3,owin(c(0,1),c(0,1),mask=matrix(TRUE, 100,100)))","p$x","p$y","readOGR(name)",
			"readGDAL(name)","name$band1","writeOGR(obj)"};
	
	public String[] spatialStatsContext={"Loads the spatstat library!", "Loads the spatial library!","Returns the nearest neighbour distances!\n(required: loaded spatstat library)!",
			"Returns the pair distances!","exactdt(x,y)\n(Required: loaded spatstat library)!","Generates random points\n(required: loaded spatstat library and argument has to be of \nspatstat class ppp (ppp(x, y, c(0,100), c(0,100))))!",
			"Creates an spatstat object in the specified window!","Creates an random spatstat object in the specified window!","Creates a spatstat point pattern object from an ImageJ particle analysis!","Prints useful summary of point pattern X","Creates random points","Calculates Ripley's K.\nThe library spatstat is required!","Maps continuous coordinates to raster locations","Returns the x-coordinates!","Returns the y-coordinates!","Read a vector file!\n(Required: loaded rgdal library)!",
			"Read a vector file!\n(requires: loaded rgdal library)!","Returns the first band (channel) of the specified data!","Writes vector data to a file!\n(Required: loaded rgdal library)!"};
	
	public String[] matrix={"m1<-matrix(c(29,26,58,53,61,54,33,21,46),3,3)","m2<-matrix(c(26,56,58,83,13,54,23,29,16),3,3)",
			"m1*m2","m1%*%m2","m1 %o% m2","diag(m1)","svd(m1)","eigen(m1)","chol(m1)","qr(m1)",
			"t(m1)","crossprod(m1,m2)","crossprod(m1)",
			"known<-matrix(c(10,6,4),nrow=3)","solve(m1,known)",
			"rowSums(m1)","colSums(m1)","rowMeans(m1)","colMeans(m1)",
			"cbind(m1,m2)","rbind(m1,m2)"};
	
	public String[] matrixContext={"Creates a matrix of size 3*3","Creates a matrix of size 3*3",
			"Matrix multiplication(Element-wise)","Matrix multiplication","Calculates the outer product","Extract or replace the diagonal of a matrix, \nor construct a diagonal matrix","Compute the singular-value decomposition \nof a rectangular matrix","Computes eigenvalues and eigenvectors of \nreal or complex matrices","Computes the Choleski factorization of a real \nsymmetric positive-definite square matrices","qr computes the QR decomposition of a matrix",
			"Given a matrix or data.frame x, t \nreturns the transpose of x","Given matrices x and y as arguments, \nreturns a matrix cross-product","Given matrices x and y as arguments, \nreturns a matrix cross-product\n(matrices: y = NULL is taken to be the same matrix as x)",	
			"Creates a matrix with 3 rows","This generic function solves the equation a %*% x = b for x, \nwhere b can be either a vector or a matrix",
			"Calculates the row sums and returns a vector","Calculates the column sums and returns a vector","Calculates the row means and returns a vector","Calculates the column means and returns a vector",
			"Combines two matrices column-wise","Combines two matrices row-wise"};
	
	public String[] imageAnalysis={"which(imageMatrix==5,arr.ind=TRUE) ",
			"imageMatrix<-replace(imageMatrix,which(imageMatrix==2, arr.ind=TRUE),0)",
			"imageMatrix<-matrix(c(1:1000000)*1,1000,1000)","imageMatrix<-matrix(runif(1000000)*2500,1000,1000)",
			"imageSizeX<-1000","imageSizeY<-1000","imageMatrix<-runif(1000000)*2500",
			"ifelse(imageMatrix<100,imageMatrix,NA)","imageMatrix<-as.integer(imageMatrix)",
			"imageMatrix<-matrix(imageMatrix,imageSizeX,imageSizeY)","imageMatrix<-matrix(rep(1,1000000),1000,1000)",
			"imageMatrix<-matrix(rep(NaN,1000000),1000,1000)"
			};
	public String[] imageAnalysisContext={"Returns the coordinates from\nthe data with value 5! ",
			"Replaces specified values in a matrix with the specified number!",
			"Creates a 1000*1000 matrix with\nvalues from 1 to 1000000 !","Creates a matrix with 1000000\nuniform distributed random numbers\nscaled with a factor (2500) of size 1000*1000 !",
			"Defines the image x-size for the image vector data","Defines the image y-size for the image vector data","Creates a vector with 1000000\nuniform distributed random numbers\nscaled with a factor (2500) of size 1000000 !",
			"Replaces the matched values\nwith the specified second argument, \nelse with the third argument!",
			"Converts specified data (e.g. raw) to integer data!","Converts image vector data to a matrix\nwith the available width and height arguments!",
			"Creates a 1000*1000 matrix filled with the specified number!",
			"Creates a 1000*1000 matrix filled with NaN values!"};
	
	private String[] history = new String[200];
	private String[] temphistory = new String[200];

	public RFunctions() {
		propsHistInstance = this;
		initHistory();

	}

	private void initHistory() {
		for (int i = 0; i < history.length; i++) {
			history[i] = "";
			temphistory[i] = "";
		}
	}

	public static RFunctions getPropsHistInstance() {
		return propsHistInstance;
	}

	public String[] getHistory() {
		return history;
	}

	public String[] getTemphistory() {
		return temphistory;
	}

}
