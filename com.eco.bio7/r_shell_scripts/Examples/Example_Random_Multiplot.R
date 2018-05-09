
renderPlot <- function() {
    for (x in (1:10)) {
        par(mfrow = c(2, 3))
        boxplot(runif(1000))
        plot(runif(1000))
        image(matrix(runif(100) * 2500, 10, 10), useRaster = TRUE)
        hist(runif(100))
        pie(runif(10), main = x)
        mat <- matrix(runif(100) * 2500, 10, 10)
        # contour(mat,main='Plot');
        persp(mat, main = "Plot", theta = 30, phi = 30, expand = 0.5, col = "grey")
       
    }
}
renderPlot()

