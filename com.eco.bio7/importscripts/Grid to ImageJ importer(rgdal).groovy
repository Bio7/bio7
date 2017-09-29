/*
Gridfile importer script. Using the 'rgdal' package of R.
Author: M. Austenfeld 
Year:   2008

 */
import com.eco.bio7.worldwind.WorldWindOptionsView;
import static com.eco.bio7.rbridge.RServeUtil.*;
import org.rosuda.REngine.REXP;
import org.apache.commons.io.FilenameUtils;

evalR(".isInstalled<-as.character(require('rgdal'))", null);
rexpr = fromR(".isInstalled");
isAvail = rexpr.asString();

if (isAvail.equals("FALSE")) {
	Bio7Dialog.message("Can't load 'rgdal' package!");
	return;
}

String file = Bio7Dialog.openFile("*");
if (file == null) {
	return;
}
if (Bio7Dialog.getOS().equals("Windows")) {
	file = file.replace("\\", "/");
}

f = new File(file);
name = FilenameUtils.removeExtension(f.getName());

/*Read the shape file with the filename as the layer!*/
//evalR("library(rgdal);",null);
evalR("try(" + name + " <- readGDAL(\"" + file + "\"));",null);

System.out.println("Loaded Grid: " + name + "\n");
evalR("print(summary(" + name + "))",null);
/*We need to access the cell size by means of the slots!*/
evalR("try(a<-slot(" + name + ",\"grid\"))",null);
evalR("try(dim<-slot(a,\"cells.dim\"))",null);
evalR("imageSizeY<-dim[2]",null);
evalR("imageSizeX<-dim[1]",null);

/*Access bounding box for WorldWind!*/
evalR("try(bbox<-slot(" + name + ",\"bbox\"))",null);
double minLat = fromR("bbox[2]").asDouble();
double maxLat =  fromR("bbox[4]").asDouble();
double minLon =  fromR("bbox[1]").asDouble();
double maxLon =  fromR("bbox[3]").asDouble();
/*Transfer Lat Lon to WorldWind if active!*/
if (WorldWindOptionsView.getOptionsInstance() != null) {
	WorldWindOptionsView.setMinLat(Double.toString(minLat));
	WorldWindOptionsView.setMaxLat(Double.toString(maxLat));
	WorldWindOptionsView.setMinLon(Double.toString(minLon));
	WorldWindOptionsView.setMaxLon(Double.toString(maxLon));
}
/*Create an image vector for the first band of the srtm-hgt file!*/
evalR("datadf<-slot(" + name + ",\"data\")",null);
/*Groovy specific we have to escape the $ char!*/
evalR("eval(dataFinal<-datadf\$band1)",null);


/*Create an float image from the data transfered to ImageJ as integers!*/
ImageMethods.imageFromR(2, "dataFinal", 0);

/*Cleanup and remove variables!*/
evalR("try(remove(a));",null);
evalR("try(remove(dim));",null);
			
		
		