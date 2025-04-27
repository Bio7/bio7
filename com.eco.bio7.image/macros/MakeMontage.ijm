// This example macro creates 2D montage of the images in a stack.
// It demonstrates the use of batch mode, copy and paste, and shows
// how to create an image that is the same type as another image.

  if (nSlices<2) exit("This macro requires a stack.");
  makeMontage();

  function makeMontage() {
      setBatchMode(true);
      id=getImageID;
      n= nSlices;
      columns=floor(sqrt(n));
      if (columns*columns<n) columns++;
      rows = columns;
      if (n<=columns*(rows-1)) rows--;
      sw=getWidth; sh=getHeight;
      type = ""+bitDepth;
      if (type=="24") type = "RGB";
      newImage("test", type+" black", columns*getWidth, rows*getHeight, 1);
      iw=getWidth/columns; ih=getHeight/rows;
      montage=getImageID;
      selectImage(id);
      for (i=0; i<n; i++) {
          selectImage(id);
          setSlice(i+1);
          run("Select All");
          run("Copy");
          selectImage(montage);
          makeRectangle((i%columns)*iw, floor(i/columns)*ih, sw, sh);
          run("Paste");
      }
      resetMinAndMax();
      setBatchMode(false); // display the montage
 }
