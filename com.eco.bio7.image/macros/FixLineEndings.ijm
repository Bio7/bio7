// "FixLineEndings"

// This macro converts the line separator characters
// of text files in a specified directory to those of the 
// platform ImageJ is currently running on.

  extensions = newArray(".java", ".txt", ".ijm");
  dir = getDirectory("Choose a Directory ");
  count = 1;
  fixFiles(dir); 

  function fixFiles(dir) {
      list = getFileList(dir);
      for (i=0; i<list.length; i++) {
          showProgress(i, list.length);
          if (endsWith(list[i], "/"))
              fixFiles(""+dir+list[i]);
          else if (valid(list[i])) {
              s = File.openAsString(dir+list[i]);
              lines = split(s, "\n");
              f = File.open(dir+list[i]);
              for (j=0; j<lines.length; j++) {
                 print(f, lines[j]);
              }
              File.close(f);
          }
      }
  }

  function valid(name) {
      for (i=0; i<extensions.length; i++) {
         if (endsWith(name, extensions[i]))
             return true;
      }
      return false;
  }
