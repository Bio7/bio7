/*
Shapefile importer script. Using the 'rgdal' package of R.
Author: M. Austenfeld 
Year:   2008
*/

import com.eco.bio7.worldwind.WorldWindOptionsView;
import static com.eco.bio7.rbridge.RServeUtil.*;
import org.rosuda.REngine.REXP;
import org.apache.commons.io.FilenameUtils;

/*Test if Rserve is alive!*/
if(RServe.isAliveDialog()==false){
	return;
}
/*Test if rgdal package for the import is installed!*/
evalR(".isInstalled<-as.character(require('rgdal'))", null);
rexpr = fromR(".isInstalled");
isAvail = rexpr.asString();

if (isAvail.equals("FALSE")) {
	Bio7Dialog.message("Can't load 'rgdal' package!");
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

/*Read the shape file with the filename as the layer!*/
evalR("try(" + name + " <- readOGR(\"" + file + "\", \"" + name + "\"));",null);
println("Loaded Shape: " + name + "\n");
evalR("print(summary(" + name + "))",null);
/*Access bounding box for WorldWind!*/
/*Access bounding box for WorldWind!*/
evalR("try(.bboxImage<-slot(" + name + ",\"bbox\"))",null);
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
evalR("try(remove(list = c('.bboxImage','.isInstalled')));", null);
RServeUtil.listRObjects();       
	