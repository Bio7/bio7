Dialog.create("Select a name");
Dialog.addString("Name", "Class");
Dialog.show();
n = roiManager("count");
name = Dialog.getString();
indexes = split(call("ij.plugin.frame.RoiManager.getIndexesAsString"));
for (i = 0; i < indexes.length; i++) {
    roiManager("select", indexes[i]);
    roiManager("rename", name +"_"+ (i+1));

}