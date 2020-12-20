#A script to create a matrix from selected R-Shell objects (available in '.r_shell_vars')!
#The selected objects appended with the command rbind!
matrix_classes<-NULL

for (i in 1:length(.r_shell_vars)) {
	matrix_classes<-rbind(matrix_classes,get(.r_shell_vars[[i]]))
}



	