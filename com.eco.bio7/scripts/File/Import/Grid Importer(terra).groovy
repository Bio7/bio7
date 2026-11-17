import static com.eco.bio7.rbridge.REval.*;
import org.apache.commons.io.FilenameUtils;
/*
Gridfile importer script. Using the 'rgdal' package of R.
Author: M. Austenfeld 
Year:   2005-2025
*/

/*Test if R is running!*/
if(RTools.isAliveDialog()==false){
	return;
}
evalR(".isInstalled<-as.character(require('terra'))", null);
rValue = fromR(".isInstalled");
isAvail = rValue.firstString();

if (isAvail.equals("FALSE")) {
	Bio7Dialog.message("Can't load 'terra' package!");
	return;
}

files = Bio7Dialog.openFile();

if (files != null) {
	if (Bio7Dialog.getOS().equals("Windows")) {
		files = files.replace("\\", "/");
	}
	f = new File(files);
	name = FilenameUtils.removeExtension(f.getName());

	//c.assign(".fileGdal", f.toString());
	toR(".fileGdalBio7", f.toString());
	/*Read the gridfile with the filename as the layer!*/
	evalR("try(" + name + " <- rast(.fileGdalBio7));", null);
	println("Loaded Grid: " + name + "\n");
	evalR("try(print(summary(" + name + ")));try(remove(list = c('.fileGdalBio7','.isInstalled')));", null);

}
REval.listRObjects();	

