// This macro demonstrates how to use the Process>FFT>Custom Filter command

  // delete any existing filter
  if (isOpen("Filter")) {
      selectWindow("Filter");
      run("Close");
  }

  // Create filter
  // Note that the filer does not have to be the same size as the image
  size = 256;
  newImage("Filter", "8-bit black", size, size, 1);
  radius = size/10;
  makeOval(getWidth/2-radius, getHeight/2-radius, radius*2, radius*2);
  setForegroundColor(255, 255, 255);
  run("Fill");
  run("Select None");
  run("Gaussian Blur...", "radius="+0.9*radius);
  //run("Size...", "width=512 height=512  interpolate");
  filter = getImageID();

  // Create profile plot of filter 
  makeLine(0, 127, 255, 127);
  run("Plot Profile");
  run("Duplicate...", "title='Profile of Filter'");
  selectWindow("Plot of Filter");
  run("Close");
  
 // Open a sample image and do low-pass filtering
  run("Bridge (174K)");
  //run("Boats (356K)");
  //run("Mandrill (70K)");
  //run("Lena (47K)");
  image = getImageID();
  run("Duplicate...", "title='Low-pass Result'");
  run("Custom Filter...", "filter=Filter");
  run("Enhance Contrast", "saturated=1  ");

  // Do high-pass filtering
  selectImage(filter);
  run("Invert");
  selectImage(image);
  run("Duplicate...", "title='High-pass Result'");
  run("Custom Filter...", "filter=Filter");
  run("Enhance Contrast", "saturated=1  ");

  run("Tile");
