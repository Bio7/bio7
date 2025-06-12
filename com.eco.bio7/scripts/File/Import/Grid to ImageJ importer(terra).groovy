/*
Gridfile importer script. Using the 'rgdal' package of R.
Author: M. Austenfeld 
Year:   2005-2025
*/
import com.eco.bio7.worldwind.WorldWindOptionsView;
import static com.eco.bio7.rbridge.RServeUtil.*;
import org.rosuda.REngine.REXP;
import org.apache.commons.io.FilenameUtils;
import static com.eco.bio7.image.ImageMethods.*;

/*Test if Rserve is alive!*/
if(RServe.isAliveDialog()==false){
	return;
}
/*Test if rgdal package for the import is installed!*/
evalR(".isInstalled<-as.character(require('terra'))", null);
rexpr = fromR(".isInstalled");
isAvail = rexpr.asString();

if (isAvail.equals("FALSE")) {
	Bio7Dialog.message("Can't load 'terra' package!");
	return;
}

file = Bio7Dialog.openFile("*");
if (file == null) {
	return;
}
if (Bio7Dialog.getOS().equals("Windows")) {
	file = file.replace("\\", "/");
}

f = new File(file);
name = FilenameUtils.removeExtension(f.getName());

evalR("try(" + name + " <- rast(\"" + file + "\"));",null);

println("Loaded Grid: " + name + "\n");
evalR("print(summary(" + name + "))",null);
/*We need to access the cell size by means of the slots!*/
//evalR("try(.cellSize<-slot(" + name + ",\"grid\"))",null);
evalR("try(.imageDimension<-dim(" + name + "))",null);
evalR("imageSizeY<-.imageDimension[2]",null);
evalR("imageSizeX<-.imageDimension[1]",null);

/*Access bounding box for WorldWind!*/
evalR("try(.bboxImage<-ext(" + name + "))",null);
minLat =  fromR(".bboxImage[2]").asDouble();
maxLat =  fromR(".bboxImage[4]").asDouble();
minLon =  fromR(".bboxImage[1]").asDouble();
maxLon =  fromR(".bboxImage[3]").asDouble();
/*Transfer Lat Lon to WorldWind if active!*/
if (WorldWindOptionsView.getOptionsInstance() != null) {
	WorldWindOptionsView.setMinLat(Double.toString(minLat));
	WorldWindOptionsView.setMaxLat(Double.toString(maxLat));
	WorldWindOptionsView.setMinLon(Double.toString(minLon));
	WorldWindOptionsView.setMaxLon(Double.toString(maxLon));
}
/*Create an image vector for the first band of a file!*/
evalR(".datadf<-as.vector(" + name + "[[1]][,])",null);
/*Groovy specific we have to escape the $ char!*/
evalR("eval("+name+"<-.datadf)",null);


/*Create an float image from the data transfered to ImageJ as integers!*/
imageFromR(2, name, 0);

/*Cleanup and remove temporary variables!*/
//evalR("try(remove(list = c('.isInstalled','.cellSize','.imageDimension','.datadf','.bboxImage')));", null);
RServeUtil.listRObjects();
