#RScript - please enter your code here !

rglInstalledAndPlotActivated<-function() {
	available<-FALSE;
	if(require(rgl)&&rgl.cur()>0){
		
		available<-TRUE;	
	}
	else{
		
		available<-FALSE;	
	}
	return(available)
}

while ((is.null(dev.list())==FALSE)||rglInstalledAndPlotActivated()){
	Sys.sleep(0.02)  
}
remove(rglInstalledAndPlotActivated)