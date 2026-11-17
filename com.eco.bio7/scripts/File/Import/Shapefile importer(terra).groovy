/*
Shapefile importer script. Using the 'rgdal' package of R.
Author: M. Austenfeld 
Year:   2005-2025
*/

import com.eco.bio7.worldwind.WorldWindOptionsView;
import static com.eco.bio7.rbridge.REval.*;
import org.apache.commons.io.FilenameUtils;

/*Test if R is running!*/
if(RTools.isAliveDialog()==false){
	return;
}
/*Test if rgdal package for the import is installed!*/
evalR(".isInstalled<-as.character(require('terra'))", null);
rValue = fromR(".isInstalled");
isAvail = rValue.firstString();

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

/*Read the shape file with the filename as the layer!*/
evalR("try(" + name + " <- vect(\"" + file + "\", \"" + name + "\"));",null);
println("Loaded Shape: " + name + "\n");
evalR("print(summary(" + name + "))",null);
/*Access bounding box for WorldWind!*/
/*Access bounding box for WorldWind!*/
evalR("try(.bboxImage<-ext(" + name + "))",null);
minLat =  fromR(".bboxImage[2]").firstDouble();
maxLat =  fromR(".bboxImage[4]").firstDouble();
minLon =  fromR(".bboxImage[1]").firstDouble();
maxLon =  fromR(".bboxImage[3]").firstDouble();
/*Transfer Lat Lon to WorldWind if active!*/
if (WorldWindOptionsView.getOptionsInstance() != null) {
	WorldWindOptionsView.setMinLat(Double.toString(minLat));
	WorldWindOptionsView.setMaxLat(Double.toString(maxLat));
	WorldWindOptionsView.setMinLon(Double.toString(minLon));
	WorldWindOptionsView.setMaxLon(Double.toString(maxLon));
}
evalR("try(remove(list = c('.bboxImage','.isInstalled')));", null);
REval.listRObjects();       
	