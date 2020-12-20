file = getArgument() //We can call this macro from Java with an argument!

/*Simply open a file with the Bio Formats library!*/
run("Bio-Formats (Windowless)", "open=["+file+"]");
/*
Landsat 8 example to open a group of selected layers. Example dataset from article:
https://earthobservatory.nasa.gov/blogs/elegantfigures/2013/10/22/how-to-make-a-true-color-landsat-8-image/
Thanks to Kevin Ward from NASA Earth Observatory Group / Earth Science Data Systems which kindly fixed the broken dataset links.
*/

/*

last=file.lastIndexOf("_B");
sub=file.substring(0,last+2);

layers="2,3,4";
run("Bio-Formats", "open=["+file+"] autoscale color_mode=Default group_files rois_import=[ROI manager] view=Hyperstack stack_order=XYCZT axis_1_number_of_images=11 axis_1_axis_first_image=1 axis_1_axis_increment=1 contains=[] name="+sub+"<"+layers+">.TIF");
//run("Scale...", "x=0.5 y=0.5 width=3856 height=3916 interpolation=Bilinear average create");
//Crop:
//run("Bio-Formats", "open=["+file+"] autoscale color_mode=Default crop group_files rois_import=[ROI manager] view=Hyperstack stack_order=XYCZT axis_1_number_of_images=11 axis_1_axis_first_image=1 axis_1_axis_increment=1 contains=[] name="+sub+"<"+layers+">.TIF x_coordinate_1=0 y_coordinate_1=0 width_1=4000 height_1=4000");

*/

 