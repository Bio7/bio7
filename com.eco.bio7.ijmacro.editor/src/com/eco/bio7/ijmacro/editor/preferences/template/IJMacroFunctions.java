package com.eco.bio7.ijmacro.editor.preferences.template;

public class IJMacroFunctions {
	
	public static String linSeparator=System.lineSeparator();
	
	public static String functions=""+
			"abs(n)####Returns the absolute value of n."+linSeparator +			
			"acos(n)####Returns the inverse cosine (in radians) of n."+linSeparator +
			"Array.applyMacro()####Applies a macro to an array."+linSeparator + 
			"Array.concat(array1,array2)####Returns a new array created by joining two or more arrays or values (examples)."+linSeparator + 
			"Array.copy(array)####Returns a copy of array."+linSeparator +
			"Array.deleteValue()####Delete an array value."+linSeparator + 
			"Array.deleteIndex()####Delete an array value using an index."+linSeparator +
			"Array.fill(array,value)####Assigns the specified numeric value to each element of array."+linSeparator + 
			"Array.filter(arr,str)####Filters an array."+linSeparator +
			"Array.findMaxima(array,tolerance)####Returns an array holding the peak positions (sorted with descending strength). 'Tolerance' is the minimum amplitude difference needed to separate two peaks. With v1.51n and later,there is an optional 'edgeMode' argument: 0=include edges,1=exclude edges(default),2=circular array."+linSeparator + 
			"Array.findMinima(array,tolerance)####Returns an array holding the minima positions."+linSeparator + 
			"Array.fourier(array,windowType)####Calculates and returns the Fourier amplitudes of array. WindowType can be \"none\",\"Hamming\",\"Hann\",or \"flat-top\",or may be omitted (meaning \"none\"). See the TestArrayFourier macro for an example and more documentation. Requires 1.49i."+linSeparator + 
			"Array.getSequence(n)####Returns an array containing the numeric sequence 0,1,2...n-1. Requires 1.49u."+linSeparator + 
			"Array.getStatistics(array,min,max,mean,stdDev)####Returns the min,max,mean,and stdDev of array,which must contain all numbers."+linSeparator + 
			"Array.invert(array)####Inverts an array."+linSeparator +
			"Array.print(array)####Prints the array on a single line."+linSeparator + 
			"Array.rankPositions(array)####Returns,as an array,the rank position indexes of array,starting with the index of the smallest value."+linSeparator + 
			"Array.resample(array,len)####Returns an array which is linearly resampled to a different length."+linSeparator + 
			"Array.reverse(array)####Reverses (inverts) the order of the elements in array."+linSeparator + 
			"Array.sort(array1,array2,array3...)####Sorts the given arrays."+linSeparator + 
			"Array.rotate(array,d)####Rotates the array elements by 'd' steps (positive 'd' = rotate right). Requires 1.51n."+linSeparator +
			"Array.getVertexAngles(xArr,yArr,arm)####From a closed contour given by 'xArr','yArr',an array is returned holding vertex angles in degrees (straight=0,convex = positive if contour is clockwise). Set 'arm'=1 to calculate the angle from the closest vertex points left and right,or use arm>1 for more distant neighbours and smoother results. Requires 1.51n."+linSeparator +
			"Array.show(array)####Displays the contents of array in a window."+linSeparator + 
			"Array.show(\"title\",array1,array2,...)####Displays one or more arrays in a Results window (examples). If title (optional) is \"Results\",the window will be the active Results window,otherwise,it will be a dormant Results window (see also IJ.renameResults). If title ends with \"(indexes)\",a 0-based Index column is shown. If title ends with \"(row numbers)\",the row number column is shown."+linSeparator + 
			"Array.slice(array,start,end)####Extracts a part of an array and returns it. (examples)."+linSeparator + 
			"Array.sort(array)####Sorts array,which must contain all numbers or all strings. String sorts are case-insensitive in v1.44i or later."+linSeparator + 
			"Array.trim(array,n)####Returns an array that contains the first n elements of array."+linSeparator + 
			"Array.rotate(array,d)####Rotates the array elements by 'd' steps (positive 'd' = rotate right). Requires 1.51n. Examples."+linSeparator + 
			"Array.getVertexAngles(xArr,yArr,arm)####From a closed contour given by 'xArr','yArr',an array is returned holding vertex angles in degrees (straight=0,convex = positive if contour is clockwise). Set 'arm'=1 to calculate the angle from the closest vertex points left and right,or use arm>1 for more distant neighbours and smoother results. Requires 1.51n. Examples."+linSeparator+
			"asin(n)####Returns the inverse sine (in radians) of n."+linSeparator + 
			"atan(n)####Calculates the inverse tangent (arctangent) of n. Returns a value in the range -PI/2 through PI/2."+linSeparator + 
			"atan2(y,x)####Calculates the inverse tangent of y/x and returns an angle in the range -PI to PI,using the signs of the arguments to determine the quadrant. Multiply the result by 180/PI to convert to degrees."+linSeparator + 
			"autoUpdate(boolean)####If boolean is true,the display is refreshed each time lineTo(),drawLine(),drawString(),etc. are called,otherwise,the display is refreshed only when updateDisplay() is called or when the macro terminates."+linSeparator + 
			"beep()####Emits an audible beep."+linSeparator + 
			"bitDepth()####Returns the bit depth of the active image: 8,16,24 (RGB) or 32 (float)."+linSeparator + 
			"calibrate(value)####Uses the calibration function of the active image to convert a raw pixel value to a density calibrated value. The argument must be an integer in the range 0-255 (for 8-bit images) or 0-65535 (for 16-bit images). Returns the same value if the active image does not have a calibration function."+linSeparator + 
			"call(\"class.method\",arg1,arg2,...)####Calls a public static method in a Java class,passing an arbitrary number of string arguments,and returning a string. Refer to the CallJavaDemo macro and the ImpProps plugin for examples. Note that the call() function does not work when ImageJ is running as an unsigned applet."+linSeparator + 
			"changeValues(v1,v2,v3)####Changes pixels in the image or selection that have a value in the range v1-v2 to v3. For example,changeValues(0,5,5) changes all pixels less than 5 to 5,and changeValues(0x0000ff,0x0000ff,0xff0000) changes all blue pixels in an RGB image to red."+linSeparator + 
			"charCodeAt(string,index)####Returns the Unicode value of the character at the specified index in string. Index values can range from 0 to lengthOf(string). Use the fromCharCode() function to convert one or more Unicode characters to a string."+linSeparator + 
			"close()####Closes the active image. This function has the advantage of not closing the \"Log\" or \"Results\" window when you meant to close the active image."+linSeparator + 
			"close(pattern)####Closes all image windows whose title matches pattern. Pattern may contain the wildcard characters \"*\" (matches any character sequence) or \"?\" (matches any single character). For example,close(\"Histo*\") could be used to dispose all histogram windows. The front image remains in front if it still exists. Pattern is not case sensitive. Use close(\"\\\\Others\") to close all except the front image."+linSeparator+
			"cos(angle)####Returns the cosine of an angle (in radians)."+linSeparator + 
			"Color.set(string)####Sets the drawing color, where 'string' is \"red\", \"blue\", etc., or a hex value like \"#ff0000\"."+linSeparator +
			"Color.set(value)####Sets the drawing color. For 8 bit images, 0<=value<=255. For 16 bit images, 0<=value<=65535. With RGB images, use hex constants (e.g., 0xff0000 for red)."+linSeparator +
			"Color.background####Returns the background color as a string, for example \"cyan\" or \"#00ffff\"."+linSeparator +
			"Color.foreground####Returns the foreground color as a string, for example \"red\" or \"#ff0000\"."+linSeparator +
			"Color.setForeground(r,g,b)####Sets the foreground color, where r, g, and b are >= 0 and <= 255."+linSeparator + 
			"Color.setBackgroundValue(value)####Sets the background color, where 'value' is a number."+linSeparator +
			"Color.setBackground(r,b,g)####Sets the background color, where r, g, and b are >= 0 and <= 255."+linSeparator +  
			"Color.setForegroundValue(value)####Sets the foreground color, where 'value' is a number."+linSeparator +
			"Color.setForeground(string)####Sets the foreground color, where 'string' is \"red\", \"blue\", etc., or a hex value like \"#ff0000\"."+linSeparator +
			"Color.setBackground(string)####Sets the background color, where 'string' is \"red\", \"blue\", etc., or a hex value like \"#ff0000\"."+linSeparator +
			"Color.setLut(reds, greens, blues)####Creates a new lookup table and assigns it to the current image. Three input arrays are required, each containing 256 intensity values."+linSeparator +
			"Color.getLut(reds, greens, blues)####Returns three arrays containing the red, green and blue intensity values from the current lookup table. See the LookupTables macros for examples."+linSeparator +
			"Color.toArray(color)####Converts a color (e.g., \"red\" or \"#ff0000\") to a three element array."+linSeparator +
			"Color.toString(r,g,b)####Converts an r,g,b color to a string."+linSeparator +
			"d2s(n,decimalPlaces)####Converts the number n into a string using the specified number of decimal places. Note that d2s stands for \"double to string\". This function will probably be replaced by one with a better name."+linSeparator +
			"Dialog.addFile(label,path,columns)####Adds a filepath to the file dialog."+linSeparator + 
			"Dialog.addDirectory(label,path,columns)####Adds a directory path to the directory dialog."+linSeparator + 	
			"Dialog.create(\"Title\")####Creates a dialog box with the specified title. Call Dialog.addString(),Dialog.addNumber(),etc. to add components to the dialog. Call Dialog.show() to display the dialog and Dialog.getString(),Dialog.getNumber(),etc. to retrieve the values entered by the user. Refer to the DialogDemo macro for an example."+linSeparator+
			"Dialog.addFile(label,defaultPath)####Adds a file field and \"Browse\" button. The field width is determined by the length of 'defaultPath', with a minimum of 25 and a maximum of 60 columns. Use Dialog getString to retrieve the file path. Requires 1.53d.####"+linSeparator + 
			"Dialog.addFile(label, defaultPath, columns)####Adds a file field and \"Browse\" button, using a field width of 'columns'. Requires 1.54i."+linSeparator +
			"Dialog.addImageChoice(label)####Adds a popup menu that lists the currently open images. Use Dialog getImageChoice() to retrieve the title of the selected image. Requires 1.53d.####"+linSeparator + 
			"Dialog.addDirectory(label, defaultPath, columns)####Adds a directory field and \"Browse\" button, using a field width of 'columns'. Requires 1.54i."+linSeparator +
			"Dialog.addDirectory(label,defaultPath)####Adds a directory field and \"Browse\" button. The field width is determined by the length of 'defaultPath', with a minimum of 25 columns. Use Dialog getString to retrieve the directory path. Requires 1.53d."+linSeparator + 
			"Dialog.addMessage(string)####Adds a message to the dialog. The message can be broken into multiple lines by inserting new line characters (\"\\n\") into the string."+linSeparator + 
			"Dialog.addMessage(string,fontSize,fontColor)####Adds a message to the dialog. The font color and font size can be changed. The message can be broken into multiple lines by inserting new line characters (\"\\n\") into the string."+linSeparator + 
			"Dialog.addString(\"Label\",\"Initial text\")####Adds a text field to the dialog,using the specified label and initial text."+linSeparator + 
			"Dialog.addString(\"Label\",\"Initial text\",columns)####Adds a text field to the dialog,where columns specifies the field width in characters."+linSeparator + 
			"Dialog.addNumber(\"Label\",default)####Adds a numeric field to the dialog,using the specified label and default value."+linSeparator + 
			"Dialog.addNumber(\"Label\",default,decimalPlaces,columns,units)####Adds a numeric field,using the specified label and default value. DecimalPlaces specifies the number of digits to right of decimal point,columns specifies the the field width in characters and units is a string that is displayed to the right of the field."+linSeparator + 
			"Dialog.addSlider(\"Label\",min,max,default)####Adds a slider controlled numeric field to the dialog,using the specified label,and min,max and default values. Values with decimal points are used when (max-min)<=5 and min,max or default are non-integer. Requires 1.45f."+linSeparator + 
			"Dialog.addCheckbox(\"Label\",default)####Adds a checkbox to the dialog,using the specified label and default state (true or false)."+linSeparator + 
			"Dialog.addCheckboxGroup(rows,columns,labels,defaults)####Adds a rowsxcolumns grid of checkboxes to the dialog,using the specified labels and default states."+linSeparator + 
			"Dialog.addRadioButtonGroup(label,items,rows,columns,default)####Adds a group of radio buttons to the dialog,where 'label' is the group label,'items' is an array containing the button labels,'rows' and 'columns' specify the grid size,and 'default' is the label of the default button."+linSeparator+
			"Dialog.addChoice(\"Label\",items)####Adds a popup menu to the dialog,where items is a string array containing the menu items."+linSeparator + 
			"Dialog.addChoice(\"Label\",items,default)####Adds a popup menu,where items is a string array containing the choices and default is the default choice."+linSeparator + 
			"Dialog.addHelp(url)####Adds a \"Help\" button that opens the specified URL in the default browser. This can be used to supply a help page for this dialog or macro. With v1.46b or later,displays an HTML formatted message if 'url' starts with \"<html>\"."+linSeparator + 
			"Dialog.addImage(path)####Adds an image to the dialog."+
			"Dialog.enableYesNoCancel####Creates a Non-blocking YesNoCancel dialog."+
			"Dialog.getYesNoCancel####Get the selection of a YesNoCancel dialog."+
			"Dialog.addToSameRow()####Makes the next item added appear on the same row as the previous item. May be used for addNumericField,addSlider,addChoice,addCheckbox,addStringField,addMessage,addPanel,and before the showDialog() method. In the latter case,the buttons appear to the right of the previous item. Note that addMessage uses the remaining width,so it must be the last item of a row. Requires 1.51r."+linSeparator +
			"Dialog.setInsets(top,left,bottom)####Overrides the default insets (margins) used for the next component added to the dialog."+linSeparator+
			"Dialog.setLocation(x,y)####Sets the screen location where this dialog will be displayed."+linSeparator+
			"Dialog.getLocation(x,y)####Gets the screen location where this dialog will be displayed."+linSeparator+
			"Dialog.show()####Displays the dialog and waits until the user clicks \"OK\" or \"Cancel\". The macro terminates if the user clicks \"Cancel\"."+linSeparator + 
			"Dialog.getString()####Returns a string containing the contents of the next text field."+linSeparator + 
			"Dialog.getNumber()####Returns the contents of the next numeric field."+linSeparator + 
			"Dialog.getCheckbox()####Returns the state (true or false) of the next checkbox."+linSeparator + 
			"Dialog.getChoice()####Returns the selected item (a string) from the next popup menu."+linSeparator +
			"Dialog.getRadioButton()####Returns the selected item (a string) from the next radio button group."+linSeparator+
			"doCommand(\"Command\")####Runs an ImageJ menu command in a separate thread and returns immediately. As an example,doCommand(\"Start Animation\") starts animating the current stack in a separate thread and the macro continues to execute. Use run(\"Start Animation\") and the macro hangs until the user stops the animation."+linSeparator + 
			"doWand(x,y)####Equivalent to clicking on the current image at (x,y) with the wand tool. Note that some objects,especially one pixel wide lines,may not be reliably traced unless they have been thresholded (highlighted in red) using setThreshold."+linSeparator + 
			"drawLine(x1,y1,x2,y2)####Draws a line between (x1,y1) and (x2,y2). Use setColor() to specify the color of the line and setLineWidth() to vary the line width."+linSeparator + 
			"drawOval(x,y,width,height)####Draws the outline of an oval using the current color and line width. See also: fillOval,setColor,setLineWidth,autoUpdate."+linSeparator + 
			"drawRect(x,y,width,height)####Draws the outline of a rectangle using the current color and line width. See also: fillRect,setColor,setLineWidth,autoUpdate."+linSeparator + 
			"drawString(\"text\",x,y)####Draws text at the specified location. Call setFont() to specify the font. Call setJustification() to have the text centered or right justified. Call getStringWidth() to get the width of the text in pixels. Refer to the TextDemo macro for examples."+linSeparator + 
			"drawString(\"text\",x,y,background)####Draws text at the specified location with a filled background"+linSeparator+
			"dump()####Writes the contents of the symbol table,the tokenized macro code and the variable stack to the \"Log\" window."+linSeparator + 
			"endsWith(string,suffix)####Returns true (1) if string ends with suffix. See also: startsWith,indexOf,substring,matches."+linSeparator + 
			"eval(macro)####Evaluates (runs) one or more lines of macro code. See also: EvalDemo macro and runMacro function."+linSeparator + 
			"eval(\"script\",javascript)####Evaluates the JavaScript code contained in the string javascript,for example eval(\"script\",\"IJ.getInstance().setAlwaysOnTop(true);\"). Mac users,and users of Java 1.5,must have a copy of JavaScript.jar in the plugins folder. Requires 1.41m."+linSeparator + 
			"eval(\"bsh\",script)####Evaluates the BeanShell code contained in the string script."+linSeparator + 			
			"eval(\"python\",script)####Evaluates the Python code contained in the string script."+linSeparator+
			"exec(string or strings)####Executes a native command and returns the output of that command as a string. Also opens Web pages in the default browser and documents in other applications (e.g.,Excel). Refer to the ExecExamples macro for examples. Requires 1.39c."+linSeparator + 
			"exit() or exit(\"error message\")####Terminates execution of the macro and,optionally,displays an error message."+linSeparator + 
			"exp(n)####Returns the exponential number e (i.e.,2.718...) raised to the power of n."+linSeparator + 
			"Ext (Macro Extension) Functions####These are functions that have been added to the macro language by plugins using the MacroExtension interface. As an example,the Image5D_Extensions plugin adds functions that work with the Image5D plugins,such as Ext.setDisplayMode(\"color\") and Ext.setChannel(ch)."+linSeparator + 
			"File.append(string,path)####Appends string to the end of the specified file."+linSeparator + 
			"File.close(f)####Closes the specified file,which must have been opened using File.open()."+linSeparator + 
			"File.dateLastModified(path)####Returns the date and time the specified file was last modified."+linSeparator + 
			"File.delete(path)####Deletes the specified file or directory. With v1.41e or later,returns \"1\" (true) if the file or directory was successfully deleted. If the file is a directory,it must be empty. The file must be in the user's home directory,the ImageJ directory or the temp directory."+linSeparator + 
			"File.directory####The directory path of the last file opened using open(),saveAs(),File.open() or File.openAsString()."+linSeparator + 
			"File.exists(path)####Returns \"1\" (true) if the specified file exists."+linSeparator + 
			"File.getDefaultDir####Returns the default directory."+linSeparator + 
			"File.getDirectory(path)####Returns the directory of the file path."+linSeparator +
			"File.getNameWithoutExtension(path)####Returns the file name of the given file path without the extension."+linSeparator +
			"File.getName(path)####Returns the last name in path's name sequence."+linSeparator + 
			"File.getParent(path)####Returns the parent of the file specified by path."+linSeparator + 
			"File.isDirectory(path)####Returns \"1\" (true) if the specified file is a directory."+linSeparator + 
			"File.isFile()####Returns \"1\" (true) if the specified file is not a directory."+linSeparator + 
			"File.lastModified(path)####Returns the time the specified file was last modified as the number of milliseconds since January 1,1970."+linSeparator + 
			"File.length(path)####Returns the length in bytes of the specified file."+linSeparator + 
			"File.makeDirectory(path)####Creates a directory."+linSeparator + 
			"File.name####The name of the last file opened using a file open dialog,a file save dialog,drag and drop,or the open(path) function."+linSeparator + 
			"File.nameWithoutExtension####The name of the last file opened with the extension removed."+linSeparator + 
			"File.open(path)####Creates a new text file and returns a file variable that refers to it. To write to the file,pass the file variable to the print function. Displays a file save dialog box if path is an empty string. The file is closed when the macro exits. Currently,only one file can be open at a time. For an example,refer to the SaveTextFileDemo macro."+linSeparator + 
			"File.openAsString(path)####Opens a text file and returns the contents as a string. Displays a file open dialog box if path is an empty string. Use lines=split(str,\"\\n\") to convert the string to an array of lines."+linSeparator + 
			"File.openAsRawString(path)####Opens a file and returns up to the first 5,000 bytes as a string. Returns all the bytes in the file if the name ends with \".txt\". Refer to the First10Bytes and ZapGremlins macros for examples."+linSeparator + 
			"File.openAsRawString(path,count)####Opens a file and returns up to the first count bytes as a string."+linSeparator + 
			"File.openSequence(path,options)####Opens a file file sequence with the given options."+linSeparator + 
			"File.openUrlAsString(url)####Opens a URL and returns the contents as a string. Returns an emptly string if the host or file cannot be found. With v1.41i and later,returns \"<Error: message>\" if there any error,including host or file not found."+linSeparator + 
			"File.openDialog(title)####Displays a file open dialog and returns the path to the file choosen by the user. The macro exits if the user cancels the dialog."+linSeparator + 
			"File.openSequence(path,options)####Opens a sequence of files from a given path with given options."+linSeparator + 
			"File.rename(path1,path2)####Renames,or moves,a file or directory. Returns \"1\" (true) if successful."+linSeparator + 
			"File.saveString(string,path)####Saves string as a file."+linSeparator + 
			"File.separator####Returns the file name separator character (\"/\" or \"\\\")."+linSeparator + 
			"File.setDefaultDir(path)####Sets the default directory."+linSeparator + 
			"fill()####Fills the image or selection with the current drawing color."+linSeparator + 
			"fillOval(x,y,width,height)####Fills an oval bounded by the specified rectangle with the current drawing color. See also: drawOval,setColor,autoUpdate."+linSeparator + 
			"fillRect(x,y,width,height)####Fills the specified rectangle with the current drawing color. See also: drawRect,setColor,autoUpdate."+linSeparator + 
			"Fit.doFit(equation,xpoints,ypoints)####Fits the specified equation to the points defined by xpoints,ypoints. Equation can be either the equation name or an index. The equation names are shown in the drop down menu in the Analyze>Tools>Curve Fitting window. With ImageJ 1.42f or later,equation can be a string containing a user-defined equation."+linSeparator + 
			"Fit.doFit(equation,xpoints,ypoints,initialGuesses)####Fits the specified equation to the points defined by xpoints,ypoints,using initial parameter values contained in initialGuesses,an array equal in length to the number of parameters in equation."+linSeparator + 
			"Fit.doWeightedFit(equation,x,y,weights)####Fits a weighted specified equation to the points defined by x,y and weights."+linSeparator +
			"Fit.getEquation(index,name,formula,macroCode)####Returns the fit values."+linSeparator + 
			"Fit.rSquared####Returns R^2=1-SSE/SSD,where SSE is the sum of the squares of the errors and SSD is the sum of the squares of the deviations about the mean."+linSeparator + 
			"Fit.p(index)####Returns the value of the specified parameter."+linSeparator + 
			"Fit.nParams####Returns the number of parameters."+linSeparator + 
			"Fit.f(x)####Returns the y value at x."+linSeparator + 
			"Fit.nEquations####Returns the number of equations."+linSeparator + 
			"Fit.getEquation(index,name,formula)####Gets the name and formula of the specified equation."+linSeparator + 
			"Fit.plot####Plots the current curve fit."+linSeparator + 
			"Fit.logResults####Causes doFit() to write a description of the curve fitting results to the Log window."+linSeparator + 
			"Fit.showDialog####Causes doFit() to display the simplex settings dialog."+linSeparator + 
			"floodFill(x,y)####Fills,with the foreground color,pixels that are connected to,and the same color as,the pixel at (x,y). With 1.37e or later,does 8-connected filling if there is an optional string argument containing \"8\",for example floodFill(x,y,\"8-connected\"). This function is used to implement the flood fill (paint bucket) macro tool."+linSeparator + 
			"floor(n)####Returns the largest value that is not greater than n and is equal to an integer. See also: round."+linSeparator + 
			"fromCharCode(value1,...,valueN)####This function takes one or more Unicode values and returns a string."+linSeparator + 
			"getArgument()####Returns the string argument passed to macros called by runMacro(macro,arg),eval(macro),IJ.runMacro(macro,arg) or IJ.runMacroFile(path,arg)."+linSeparator + 
			"getBoolean(\"message\")####Displays a dialog box containing the specified message and \"Yes\",\"No\" and \"Cancel\" buttons. Returns true (1) if the user clicks \"Yes\",returns false (0) if the user clicks \"No\" and exits the macro if the user clicks \"Cancel\"."+linSeparator + 
			"getBoolean(message,yesLabel,noLabel)####Displays a dialog box containing the specified message and buttons with custom labels."+linSeparator+
			"getBoundingRect(x,y,width,height)####Replace by getSelectionBounds."+linSeparator + 
			"getCursorLoc(x,y,z,modifiers)####Returns the cursor location in pixels and the mouse event modifier flags. The z coordinate is zero for 2D images. For stacks,it is one less than the slice number. For examples,see the GetCursorLocDemo and the GetCursorLocDemoTool macros."+linSeparator + 
			"getDateAndTime(year,month,dayOfWeek,dayOfMonth,hour,minute,second,msec)####Returns the current date and time. For an example,refer to the GetDateAndTime macro. See also: getTime."+linSeparator + 
			"getDimensions(width,height,channels,slices,frames)####Returns the dimensions of the current image. Requires 1.38s."+linSeparator + 
			"getDir(\"cwd\")####Returns the  current working directory."+linSeparator + 
			"getDir(\"file\")####Returns the  directory of most recently opened or saved file."+linSeparator + 
			"getDir(\"preferences\")####Returns the preferences directory."+linSeparator + 
			"getDirectory(string)####Displays a \"choose directory\" dialog and returns the selected directory,or returns the path to a specified directory,such as \"plugins\",\"home\",etc. The returned path ends with a file separator,either \"\\\" (Windows) or \"/\". Returns an empty string if the specified directory is not found or aborts the macro if the user cancels the \"choose directory\" dialog box. For examples,see the GetDirectoryDemo and ListFilesRecursively macros. See also: getFileList and the File functions."+linSeparator+
			"getDirectory(\"Choose a Directory\")####Displays a file open dialog,using the argument as a title,and returns the path to the directory selected by the user."+linSeparator + 
			"getDirectory(\"plugins\")####Returns the path to the plugins directory."+linSeparator + 
			"getDirectory(\"macros\")####Returns the path to the macros directory."+linSeparator + 
			"getDirectory(\"luts\")####Returns the path to the luts directory."+linSeparator + 
			"getDirectory(\"image\")####Returns the path to the directory that the active image was loaded from."+linSeparator + 
			"getDirectory(\"imagej\")####Returns the path to the ImageJ directory."+linSeparator + 
			"getDirectory(\"startup\")####Returns the path to the directory that ImageJ was launched from."+linSeparator + 
			"getDirectory(\"home\")####Returns the path to users home directory."+linSeparator + 
			"getDirectory(\"temp\")####Returns the path to the temporary directory (/tmp on Linux and Mac OS X)."+linSeparator + 
			"getFileList(directory)####Returns an array containing the names of the files in the specified directory path. The names of subdirectories have a \"/\" appended. For an example,see the ListFilesRecursively macro."+linSeparator + 
			"getDisplayedArea(x,y,width,height)####Returns the pixel coordinates of the actual displayed area of the image canvas. For an example,see the Pixel Sampler Tool."+linSeparator + 
			"getFileList(directory)####Returns an array containing the names of the files in the specified directory path. The names of subdirectories have a \"/\" appended. For an example,see the ListFilesRecursively macro."+linSeparator + 
			"getFontList()####Returns an array containing the names of available system fonts."+linSeparator+
			"getHeight()####Returns the height in pixels of the current image."+linSeparator + 
			"getHistogram(values,counts,nBins[,histMin,histMax])####Returns the histogram of the current image or selection. Values is an array that will contain the pixel values for each of the histogram counts (or the bin starts for 16 and 32 bit images),or set this argument to 0. Counts is an array that will contain the histogram counts. nBins is the number of bins that will be used. It must be 256 for 8 bit and RGB image,or an integer greater than zero for 16 and 32 bit images. With 16-bit images,the Values argument is ignored if nBins is 65536. With 16-bit and 32-bit images,and ImageJ 1.35a and later,the histogram range can be specified using optional histMin and histMax arguments. See also: getStatistics,HistogramLister,HistogramPlotter,StackHistogramLister and CustomHistogram."+linSeparator + 
			"getImageID()####Returns the unique ID (a negative number) of the active image. Use the selectImage(id),isOpen(id) and isActive(id) functions to activate an image or to determine if it is open or active."+linSeparator + 
			"getImageInfo()####Returns a string containing the text that would be displayed by the Image>Show Info command. To retrieve the contents of a text window,use getInfo(\"window.contents\"). For an example,see the ListDicomTags macros. See also: getMetadata."+linSeparator + 
			"getInfo(\"command.name\")####Returns the name of the most recently invoked command. The names of commands invoked using keyboard shortcuts are preceded by \"^\"."+linSeparator + 
			"getInfo(DICOM_TAG)####Returns the value of a DICOM tag in the form \"xxxx,xxxx\",e.g. getInfo(\"0008,0060\"). Returns an empty string if the current image is not a DICOM or if the tag was not found."+linSeparator + 
			"getInfo(\"font.name\")####Returns the name of the current font."+linSeparator + 
			"getInfo(\"image.description\")####Returns the TIFF image description tag,or an empty string if this is not a TIFF image or the image description is not available."+linSeparator + 
			"getInfo(\"image.directory\")####Returns the directory that the current image was loaded from,or an empty string if the directory is not available."+linSeparator + 
			"getInfo(\"image.filename\")####Returns the name of the file that the current image was loaded from,or an empty string if the file name is not available."+linSeparator + 
			"getInfo(\"image.subtitle\")####Returns the subtitle of the current image. This is the line of information displayed above the image and below the title bar."+linSeparator + 
			"getInfo(\"log\")####Returns the contents of the Log window,or \"\" if the Log window is not open."+linSeparator + 
			"getInfo(\"macro.filepath\")####Returns the filepath of the most recently loaded macro or script."+linSeparator + 
			"getInfo(\"micrometer.abbreviation\")####Returns \"µm\",the abbreviation for micrometer."+linSeparator + 
			"getInfo(\"os.name\")####Returns the OS name (\"Mac OS X\",\"Linux\" or \"Windows\")."+linSeparator + 
			"getInfo(\"overlay\")####Returns information about the current image's overlay."+linSeparator + 
			"getInfo(\"selection.name\")####Returns the name of the current selection,or \"\" if there is no selection or the selection does not have a name. The argument can also be \"roi.name\"."+linSeparator + 
			"getInfo(\"selection.color\")####Returns the color of the current selection."+linSeparator + 
			"getInfo(\"slice.label\")####Return the label of the current stack slice. This is the string that appears in parentheses in the subtitle,the line of information above the image. Returns an empty string if the current image is not a stack or the current slice does not have a label."+linSeparator + 
			"getInfo(\"threshold.method\")####Returns the current thresholding method (\"IsoData\",\"Otsu\",etc)."+linSeparator + 
			"getInfo(\"threshold.mode\")####Returns the current thresholding mode (\"Red\",\"B&W\" or\"Over/Under\")."+linSeparator + 
			"getInfo(\"window.contents\")####If the front window is a text window,returns the contents of that window. If the front window is an image,returns a string containing the text that would be displayed by Image>Show Info. Note that getImageInfo() is a more reliable way to retrieve information about an image. Use split(getInfo(),'\\n') to break the string returned by this function into separate lines. Replaces the getInfo() function."+linSeparator + 
			"getInfo(\"window.type\")####Returns the type (\"Image\",\"Text\",\"ResultsTable\",\"Editor\",\"Plot\",\"Histogram\",etc.) of the front window."+linSeparator + 
			"getInfo(key)####Returns the Java property associated with the specified key (e.g.,\"java.version\",\"os.name\",\"user.home\",\"user.dir\",etc.). Returns an empty string if there is no value associated with the key. See also: getList(\"java.properties\")."+linSeparator+
			"getLine(x1,y1,x2,y2,lineWidth)####Returns the starting coordinates,ending coordinates and width of the current straight line selection. The coordinates and line width are in pixels. Sets x1 = -1 if there is no line selection. Refer to the GetLineDemo macro for an example. See also: makeLine."+linSeparator + 
			"getList(\"image.titles\")####Returns a list (array) of image window titles. For an example,see the DisplayWindowTitles macro"+linSeparator+
			"getList(\"window.titles\")####Returns a list (array) of non-image window titles. For an example,see the DisplayWindowTitles macro. Requires 1.38m."+linSeparator + 
			"getList(\"java.properties\")####Returns a list (array) of Java property keys. For an example,see the DisplayJavaProperties macro. See also: getInfo(key). Requires 1.38m."+linSeparator + 
			"getList(\"threshold.methods\")####Returns a list of the available automatic thresholding methods."+linSeparator + 
			"getList(\"LUTs\")####Returns,as an array,a list of the LUTs in the Image>Lookup Tables menu."+linSeparator+
			"getLocationAndSize(x,y,width,height)####Returns the location and size,in screen coordinates,of the active image window. Use getWidth and getHeight to get the width and height,in image coordinates,of the active image. See also: setLocation,"+linSeparator + 
			"getLut(reds,greens,blues)####Returns three arrays containing the red,green and blue intensity values from the current lookup table. See the LookupTables macros for examples."+linSeparator + 
			"getMetadata(\"Info\")####Returns the metadata (a string) from the \"Info\" property of the current image. With DICOM images,this is the information (tags) in the DICOM header. See also: setMetadata. Requires 1.40b."+linSeparator + 
			"getMetadata(\"Label\")####Returns the current slice label. The first line of the this label (up to 60 characters) is display as part of the image subtitle. With DICOM stacks,returns the metadata from the DICOM header. See also: setMetadata. Requires 1.40b."+linSeparator + 
			"getMinAndMax(min,max)####Returns the minimum and maximum displayed pixel values (display range). See the DisplayRangeMacros for examples."+linSeparator + 
			"getNumber(\"prompt\",defaultValue)####Displays a dialog box and returns the number entered by the user. The first argument is the prompting message and the second is the value initially displayed in the dialog. Exits the macro if the user clicks on \"Cancel\" in the dialog. Returns defaultValue if the user enters an invalid number. See also: Dialog.create."+linSeparator + 
			"getPixel(x,y)####Returns the raw value of the pixel at (x, y). Uses bilinear interpolation if 'x' or 'y' are not integers."+linSeparator + 
			"getPixelSize(unit,pixelWidth,pixelHeight)####Returns the unit of length (as a string) and the pixel dimensions. For an example,see the ShowImageInfo macro. See also: getVoxelSize."+linSeparator + 
			"getProfile()####Runs Analyze>Plot Profile (without displaying the plot) and returns the intensity values as an array. For an example,see the GetProfileExample macro."+linSeparator + 
			"getRawStatistics(nPixels,mean,min,max,std,histogram)####This function is similar to getStatistics except that the values returned are uncalibrated and the histogram of 16-bit images has a bin width of one and is returned as a max+1 element array. For examples,refer to the ShowStatistics macro set. See also: calibrate."+linSeparator + 
			"getResult(\"Column\",row)####Returns a measurement from the ImageJ results table or NaN if the specified column is not found. The first argument specifies a column in the table. It must be a \"Results\" window column label,such as \"Area\",\"Mean\" or \"Circ.\". The second argument specifies the row,where 0<=row<nResults. nResults is a predefined variable that contains the current measurement count. (Actually,it's a built-in function with the \"()\" optional.) With ImageJ 1.34g and later,you can omit the second argument and have the row default to nResults-1 (the last row in the results table). See also: nResults,setResult,isNaN,getResultLabel."+linSeparator + 
			"getResultString(\"Column\",row)####Returns a string from the ImageJ results table or \"null\" if the specified column is not found. The first argument specifies a column in the table. The second specifies the row,where 0<=row<nResults."+linSeparator+
			"getResultLabel(row)####Returns the label of the specified row in the results table,or an empty string if Display Label is not checked in Analalyze>Set Measurements."+linSeparator + 
			"getSelectionBounds(x,y,width,height)####Returns the smallest rectangle that can completely contain the current selection. x and y are the pixel coordinates of the upper left corner of the rectangle. width and height are the width and height of the rectangle in pixels. If there is no selection,returns (0,0,ImageWidth,ImageHeight). See also: selectionType and setSelectionLocation."+linSeparator + 
			"getSelectionCoordinates(xCoordinates,yCoordinates)####Returns two arrays containing the X and Y coordinates of the points that define the current selection. See the SelectionCoordinates macro for an example. See also: selectionType,getSelectionBounds."+linSeparator + 
			"getSliceNumber()####Returns the number of the currently displayed stack image,an integer between 1 and nSlices. Returns 1 if the active image is not a stack. See also: setSlice."+linSeparator + 
			"getStatistics(area,mean,min,max,std,histogram)####Returns the area,average pixel value,minimum pixel value,maximum pixel value,standard deviation of the pixel values and histogram of the active image or selection. The histogram is returned as a 256 element array. For 8-bit and RGB images,the histogram bin width is one. For 16-bit and 32-bit images,the bin width is (max-min)/256. For examples,refer to the ShowStatistics macro set. Note that trailing arguments can be omitted. For example,you can use getStatistics(area),getStatistics(area,mean) or getStatistics(area,mean,min,max). See also: getRawStatistics"+linSeparator + 
			"getString(\"prompt\",\"default\")####Displays a dialog box and returns the string entered by the user. The first argument is the prompting message and the second is the initial string value. Exits the macro if the user clicks on \"Cancel\" or enters an empty string. See also: Dialog.create."+linSeparator + 
			"getStringWidth(string)####Returns the width in pixels of the specified string. See also: setFont,drawString. Requires v1.41d."+linSeparator + 
			"getThreshold(lower,upper)####Returns the lower and upper threshold levels. Both variables are set to -1 if the active image is not thresholded. See also: setThreshold,getThreshold,resetThreshold."+linSeparator + 
			"getTime()####Returns the current time in milliseconds. The granularity of the time varies considerably from one platform to the next. On Windows NT,2K,XP it is about 10ms. On other Windows versions it can be as poor as 50ms. On many Unix platforms,including Mac OS X,it actually is 1ms. See also: getDateAndTime."+linSeparator + 
			"getTitle()####Returns the title of the current image."+linSeparator +
			"getValue(x,y)####Returns calibrated values from 8-bit and 16-bit images,intensity values from RGB images and float values from 32-bit images."+linSeparator +
			"getValue(\"color.foreground\")####Returns the current foreground color as a value that can be passed to the setColor(value) function. The value returned is the pixel value used by the Edit>Fill command and by drawing tools."+linSeparator + 
			"getValue(\"color.background\")####Returns the current background color as a value that can be passed to the setColor(value) function. The value returned is the pixel value used by the Edit>Clear command."+linSeparator + 
			"getValue(\"rgb.foreground\")####Returns the current foregound color as an RGB pixel value."+linSeparator + 
			"getValue(\"rgb.background\")####Returns the current backgound color as an RGB pixel value."+linSeparator + 
			"getValue(\"font.size\")####Returns the size,in points,of the current font."+linSeparator + 
			"getValue(\"font.height\")####Returns the height,in pixels,of the current font."+linSeparator +
			"getValue(\"selection.angle\")####Returns angle of the current selection."+linSeparator +
			"getValue(\"selection.width\")####Returns the stroke width of the current selection."+linSeparator +
			"getValue(\"selection.size\")####Returns the number of points of the current selection."+linSeparator +
			"getValue(\"Length\")####Returns the length."+linSeparator +
			"getValue(measurement)####Returns the measurement values where measurement is \"Area\",\"Mean\",\"Skew\",etc., see: http://wsr.imagej.net/macros/Colorize_ROIs_by_Measurement.txt."+linSeparator+
			"getValue(\"rgb.foreground\")####Returns the current foregound color as an RGB pixel value."+linSeparator + 
			"getValue(\"rotation.angle\")####Returns the current rotation angle."+linSeparator + 
			"getVoxelSize(width,height,depth,unit)####Returns the voxel size and unit of length (\"pixel\",\"mm\",etc.) of the current image or stack. See also: getPixelSize,setVoxelSize."+linSeparator + 
			"getVersion()####Returns the ImageJ version number as a string (e.g.,\"1.34s\"). See also: requires."+linSeparator + 
			"getWidth()####Returns the width in pixels of the current image."+linSeparator + 
			"getZoom()####Returns the magnification of the active image,a number in the range 0.03125 to 32.0 (3.1% to 3200%)."+linSeparator +
			"GenericDialog.addFileField()####Adds a file field to the Generic Dialog."+linSeparator + 
			"IJ.checksum(\"MD5 string\", string)####Returns the MD5 (or SHA-256) checksum from a string."+linSeparator + 
			"IJ.checksum(\"MD5 file\",filepath)####Returns the MD5 (or SHA-256 - \"SHA-256 file\") checksum from a file. If 'filepath' is a directory or invalid, \"0\" is returned."+linSeparator + 
			"IJ.deleteRows(index1,index2)####Deletes rows index1 through index2 in the results table."+linSeparator + 
			"IJ.getToolName()####Returns the name of the currently selected tool. See also: setTool."+linSeparator + 
			"IJ.getFullVersion####Returns the ImageJ version and build number as a string (e.g., \"1.52d11\")."+linSeparator + 
			"IJ.freeMemory()####Returns the memory status string (e.g.,\"2971K of 658MB (<1%)\") that is displayed when the users clicks in the ImageJ status bar."+linSeparator + 
			"IJ.currentMemory()####Returns,as a string,the amount of memory in bytes currently used by ImageJ."+linSeparator + 
			"IJ.log(string)####Displays string in the Log window."+linSeparator + 
			"IJ.maxMemory()####Returns,as a string,the amount of memory in bytes available to ImageJ. This value (the Java heap size) is set in the Edit>Options>Memory & Threads dialog box."+linSeparator + 
			"IJ.pad(n,length)####Pads 'n' with leading zeros and returns the result. Requires 1.45d."+linSeparator + 
			"IJ.redirectErrorMessages()####Causes next image opening error to be redirected to the Log window and prevents the macro from being aborted. Requires 1.43n."+linSeparator + 
			"IJ.renameResults(name)####Changes the title of the Results table to the string name. Requires 1.44c."+linSeparator+
			"IJ.renameResults(oldName,newName)####Changes the title of a results table from oldName to newName."+linSeparator+
			"imageCalculator(operator,img1,img2)####Runs the Process>Image Calculator tool,where operator (\"add\",\"subtract\",\"multiply\",\"divide\",\"and\",\"or\",\"xor\",\"min\",\"max\",\"average\",\"difference\" or \"copy\") specifies the operation,and img1 and img2 specify the operands. img1 and img2 can be either an image title (a string) or an image ID (an integer). The operator string can include up to three modifiers: \"create\" (e.g.,\"add create\") causes the result to be stored in a new window,\"32-bit\" causes the result to be 32-bit floating-point and \"stack\" causes the entire stack to be processed. See the ImageCalculatorDemo macros for examples."+linSeparator + 
			"Image.copy####Copies the contents of the current selection, or the entire image if there is no selection, to the internal clipboard."+linSeparator+
			"Image.height####Returns the image height."+linSeparator+
			"Image.title####Returns the title of the active image. Requires 1.53h."+linSeparator+
			"Image.width####Returns the image width."+linSeparator+
            "Image.paste(x,y)####Inserts the contents of the internal clipboard at the specified location in the active image."+linSeparator+
			"indexOf(string,substring)####Returns the index within string of the first occurrence of substring. See also: lastIndexOf,startsWith,endsWith,substring,toLowerCase,replace,matches."+linSeparator + 
			"indexOf(string,substring,fromIndex)####Returns the index within string of the first occurrence of substring,with the search starting at fromIndex."+linSeparator + 
			"is(\"animated\")####Returns true if the current image is an animated stack."+linSeparator+
			"is(\"applet\")####Returns true if ImageJ is running as an applet. Requires v1.39p."+linSeparator +
			"is(\"area\")####Returns 'true' if this is an area selection."+linSeparator + 
			"is(\"Batch Mode\")####Returns true if the macro interpreter is running in batch mode. Requires v1.39p."+linSeparator + 
			"is(\"Caps Lock Set\")####Returns true if the caps lock key is set. Always return false on some platforms. Requires v1.42e."+linSeparator + 
			"is(\"binary\")####Returns true if the current image is binary (8-bit with only 0 and 255 values)"+linSeparator+
			"is(\"Caps Lock Set\")####Returns true if the caps lock key is set. Always return false on some platforms."+linSeparator + 
			"is(\"changes\")####Returns true if the current image's 'changes' flag is set."+linSeparator+
			"is(\"composite\")####Returns true if the current image is a a multi-channel stack that uses the CompositeImage class. Requires v1.39r."+linSeparator + 
			"is(\"global scale\")####Returns true if there is global spatial calibration."+linSeparator + 
			"is(\"grayscale\")####Returns true if the current image is grayscale,or an RGB image with identical R,G and B channels."+linSeparator+
			"is(\"Inverting LUT\")####Returns true if the current image is using an inverting lookup table."+linSeparator + 
			"is(\"locked\")####Returns true if the current image is locked."+linSeparator +
			"is(\"line\")####Returns 'true' if this is a line selection."+linSeparator +
			"is(\"Virtual Stack\")####Returns true if the current image is a virtual stack. Requires v1.39q."+linSeparator + 
			"isActive(id)####Returns true if the image with the specified ID is active."+linSeparator + 
			"isKeyDown(key)####Returns true if the specified key is pressed,where key must be \"shift\",\"alt\" or \"space\". See also: setKeyDown."+linSeparator + 
			"isNaN(n)####Returns true if the value of the number n is NaN (Not-a-Number). A common way to create a NaN is to divide zero by zero. This function is required because (n==NaN) is always false. Note that the numeric constant NaN is predefined in the macro language."+linSeparator + 
			"isOpen(id)####Returns true if the image with the specified ID is open."+linSeparator + 
			"isOpen(\"Title\")####Returns true if the window with the specified title is open."+linSeparator + 
			"lastIndexOf(string,substring)####Returns the index within string of the rightmost occurrence of substring. See also: indexOf,startsWith,endsWith,substring."+linSeparator + 
			"lengthOf(str)####Returns the length of a string or array."+linSeparator + 
			"lineTo(x,y)####Draws a line from current location to (x,y)."+linSeparator + 
			"List.set(key,value)####Adds a key/value pair to the list."+linSeparator + 
			"List.get(key)####Returns the string value associated with key,or an empty string if the key is not found."+linSeparator + 
			"List.getValue(key)####When used in an assignment statement,returns the value associated with key as a number. Aborts the macro if the value is not a number or the key is not found."+linSeparator + 
			"List.size####Returns the size of the list."+linSeparator + 
			"List.clear()####Resets the list."+linSeparator + 
			"List.setList(list)####Loads the key/value pairs in the string list."+linSeparator + 
			"List.getList####Returns the list as a string."+linSeparator + 
			"List.setMeasurements####Measures the current image or selection and loads the resulting parameter names (as keys) and values. All parameters listed in the Analyze>Set Measurements dialog box are measured. Use List.getValue() in an assignment statement to retrieve the values. See the DrawEllipse macro for an example."+linSeparator + 
			"List.setMeasurements(\"limit\")####This is a version of List.setMeasurements that enables the \"Limit to threshold\" option."+linSeparator+
			"List.setCommands####Loads the ImageJ menu commands (as keys) and the plugins that implement them (as values). Requires v1.43f."+linSeparator +
			"List.toArrays(keys,values)####Retrieves keys and values as a pair of string arrays,sorted alphabetically for keys."+linSeparator + 
			"List.fromArrays(keys,values)####Creates the List from keys and values arrays."+linSeparator + 
			"List.indexOf(key)####Returns the alphabetic position of the specified key,or -1 if not found. Note that this function,as well as List.size,returns a string."+linSeparator + 
			"log(n)####Returns the natural logarithm (base e) of n. Note that log10(n) = log(n)/log(10)."+linSeparator + 
			"makeArrow(x1,y1,x2,y2,style)####Creates an arrow selection,where 'style' is a string containing \"filled\",\"notched\",\"open\",\"headless\" or \"bar\",plus the optionial modifiers \"outline\",\"double\",\"small\",\"medium\" and \"large\". See also: Roi.setStrokeWidth and Roi.setStrokeColor. Requires 1.49a."+linSeparator + 
			"makeEllipse(x1,y1,x2,y2,aspectRatio)####Creates an elliptical selection,where x1,y1,x2,y2 specify the major axis of the ellipse and aspectRatio (<=1.0) is the ratio of the lengths of minor and major axis."+linSeparator+
			"makeLine(x1,y1,x2,y2)####Creates a new straight line selection. The origin (0,0) is assumed to be the upper left corner of the image. Coordinates are in pixels. With ImageJ 1.35b and letter,you can create segmented line selections by specifying more than two coordinate pairs,for example makeLine(25,34,44,19,69,30,71,56)."+linSeparator + 
			"makeLine(x1,y1,x2,y2,lineWidth)####Creates a straight line selection with the specified width. Requires 1.38u. See also: getLine."+linSeparator + 
			"makeOval(x,y,width,height)####Creates an elliptical selection,where (x,y) define the upper left corner of the bounding rectangle of the ellipse."+linSeparator + 
			"makePoint(x,y,options)####Creates a point selection at the specified location,with the type ('hybrd','cross','dot' or 'circle'),color ('red','white',etc.) and size ('tiny','small','medium','large' or 'extra large') of the point defined in the 'options' string. The point is added to an overlay if the options string contains 'add'. Requires 1.52i."+linSeparator + 
			"makePoint(x,y)####Creates a point selection at the specified location. Create a multi-point selection by using makeSelection(\"point\",xpoints,ypoints). Use setKeyDown(\"shift\"); makePoint(x,y); to add a point to an existing point selection."+linSeparator + 
			"makePolygon(x1,y1,x2,y2,x3,y3,...)####Creates a polygonal selection. At least three coordinate pairs must be specified,but not more than 200. As an example,makePolygon(20,48,59,13,101,40,75,77,38,70) creates a polygon selection with five sides."+linSeparator + 
			"makeRectangle(x,y,width,height)####Creates a rectangular selection. The x and y arguments are the coordinates (in pixels) of the upper left corner of the selection. The origin (0,0) of the coordinate system is the upper left corner of the image."+linSeparator + 
			"makeRectangle(x,y,width,height,arcSize)####Creates a rounded rectangular selection using the specified corner arc size."+linSeparator+
			"makeRotatedRectangle(x1,y1,x2,y2,width)####Creates a rotated rectangular selection,which is similar to a wide line where (x1,y1) is the start of the line,(x2,y2) is the end of the line and 'width' is the line width."+linSeparator+
			"makeSelection(type,xcoord,ycoord)####Creates a selection from a list of XY coordinates. The first argument should be \"polygon\",\"freehand\",\"polyline\",\"freeline\",\"angle\" or \"point\". In ImageJ 1.32g or later,it can also be the numeric value returned by selectionType. The xcoord and ycoord arguments are numeric arrays that contain the X and Y coordinates. See the MakeSelectionDemo macro for examples."+linSeparator + 
			"makeText(string,x,y)####Creates a text selection at the specified coordinates. The selection will use the font and size specified by the last call to setFont(). The CreateOverlay macro provides an example."+linSeparator+
			"matches(string,regex)####Returns true if string matches the specified regular expression. See also: startsWith,endsWith,indexOf,replace. Requires v1.39r."+linSeparator + 
			"maxOf(n1,n2)####Returns the greater of two values."+linSeparator + 	
			"Math.abs(n)####Returns the absolute value."+linSeparator +
			"Math.acos(n)####Returns the arc cosine of a value."+linSeparator +
			"Math.asin(n)####Returns the arc sine of a value."+linSeparator +
			"Math.atan(n)####Returns the arc tangent of a value."+linSeparator +
			"Math.atan2(n1,n2)####Returns the angle theta from the conversion of rectangular coordinates (x, y) to polar coordinates (r, theta)."+linSeparator +
			"Math.ceil(n)####Returns the smallest (closest to negative infinity) value that is greater than or equal to the argument and is equal to a mathematical integer."+linSeparator +
			"Math.cos(n)####Returns the trigonometric cosine of an angle."+linSeparator +
			"Math.erf(x)####Returns the error function of a value."+linSeparator +
			"Math.exp(n)####Returns Euler's number e raised to the power of a double value."+linSeparator +
			"Math.floor(n)####Returns the largest (closest to positive infinity) value that is less than or equal to the argument and is equal to a mathematical integer."+linSeparator +
			"Math.log(n)####Returns the natural logarithm (base e) of a value."+linSeparator +
			"Math.log10(n)####Returns the base 10 logarithm of a value."+linSeparator +
			"Math.min(n1,n2)####Returns the smaller of two values."+linSeparator +			
			"Math.max(n1,n2)####Returns the greater of two values."+linSeparator +
			"Math.pow(n1,n2)####Returns the value of the first argument raised to the power of the second argument."+linSeparator +
			"Math.round(n)####Returns the closest long to the argument, with ties rounding to positive infinity."+linSeparator +
			"Math.sin(n)####Returns the trigonometric sine of an angle."+linSeparator +
			"Math.sqr(n)####Returns the square of the argument."+linSeparator +
			"Math.sqrt(n)####Returns the correctly rounded positive square root of a value."+linSeparator +
			"Math.tan(n)####Returns the trigonometric tangent of an angle."+linSeparator +
			"Math.toRadians(degrees)####Returns the radians from the given degree."+linSeparator +
			"Math.toDegrees(radians)####Returns the degree from the given radians."+linSeparator +
			"minOf(n1,n2)####Returns the smaller of two values."+linSeparator + 
			"moveTo(x,y)####Sets the current drawing location. The origin is always assumed to be the upper left corner of the image."+linSeparator + 
			"newArray(size)####Returns a new array containing size elements. You can also create arrays by listing the elements,for example newArray(1,4,7) or newArray(\"a\",\"b\",\"c\"). For more examples,see the Arrays macro. The ImageJ macro language does not directly support 2D arrays. As a work around,either create a blank image and use setPixel() and getPixel(),or create a 1D array using a=newArray(xmax*ymax) and do your own indexing (e.g.,value=a[x+y*xmax])."+linSeparator + 
			"newImage(title,type,width,height,depth)####Opens a new image or stack using the name title. The string type should contain \"8-bit\",\"16-bit\",\"32-bit\" or \"RGB\". In addition,it can contain \"white\",\"black\" or \"ramp\" (the default is \"white\"). As an example,use \"16-bit ramp\" to create a 16-bit image containing a grayscale ramp. Width and height specify the width and height of the image in pixels. Depth specifies the number of stack slices."+linSeparator + 
			"newMenu(macroName,stringArray)####Defines a menu that will be added to the toolbar when the menu tool named macroName is installed. Menu tools are macros with names ending in \"Menu Tool\". StringArray is an array containing the menu commands. Returns a copy of stringArray. For an example,refer to the Menus toolset. Requires v1.38b or later."+linSeparator + 
			"nImages####Returns number of open images. The parentheses \"()\" are optional."+linSeparator + 
			"nResults####Returns the current measurement counter value. The parentheses \"()\" are optional."+linSeparator + 
			"nSlices####Returns the number of images in the current stack. Returns 1 if the current image is not a stack. The parentheses \"()\" are optional. See also: getSliceNumber,"+linSeparator + 
			"open(path)####Opens and displays a tiff,dicom,fits,pgm,jpeg,bmp,gif,lut,roi,or text file. Displays an error message and aborts the macro if the specified file is not in one of the supported formats,or if the file is not found. Displays a file open dialog box if path is an empty string or if there is no argument. Use the File>Open command with the command recorder running to generate calls to this function. With 1.41k or later,opens images specified by a URL,for example open(\"http://rsb.info.nih.gov/ij/images/clown.gif\")."+linSeparator + 
			"Overlay.activateSelectionAndWait()####Activates an overlay selection."+linSeparator + 
			"Overlay.cropAndSave(dir,format);####Saves the contents of the overlay selections as separate images, where 'dir' is the directory path and 'format' is \"tif\", \"png\" or \"jpg\". Set 'format' to \"show\" and the images will be displayed in a stack and not saved. Requires 1.53d."+linSeparator + 
			"Overlay.getType(index)####Returns the type of the overlay from the given index."+linSeparator + 
			"Overlay.moveTo(x,y)####Sets the current drawing location."+linSeparator + 
			"Overlay.lineTo(x,y)####Draws a line from the current location to (x,y) ."+linSeparator + 
			"Overlay.drawLine(x1,y1,x2,y2)####Draws a line between (x1,y1) and (x2,y2))."+linSeparator + 
			"Overlay.add####Adds the drawing created by Overlay.lineTo(),Overlay.drawLine(),etc. to the overlay without updating the display."+linSeparator + 
			"Overlay.setPosition(n)####Sets the stack position (slice number) of the last item added to the overlay."+linSeparator + 
			"Overlay.setPosition(c,z,t)####Sets the hyperstack position (channel,slice,frame) of the last item added to the overlay."+linSeparator + 
			"Overlay.drawRect(x,y,width,height)####Draws a rectangle,where (x,y) specifies the upper left corner."+linSeparator + 
			"Overlay.drawEllipse(x,y,width,height)####Draws an ellipse,where (x,y) specifies the upper left corner of the bounding rectangle."+linSeparator + 
			"Overlay.drawString(\"text\",x,y)####Draws text at the specified location and adds it to the overlay. Use setFont() to specify the font and setColor to set specify the color."+linSeparator + 
			"Overlay.drawString(\"text\",x,y,angle)####Draws text at the specified location and angle and adds it to the overlay."+linSeparator + 
			"Overlay.show####Displays the current overlay."+linSeparator + 
			"Overlay.hide####Hides the current overlay."+linSeparator + 
			"Overlay.hidden####Returns true if the active image has an overlay and it is hidden."+linSeparator + 
			"Overlay.indexAt(x,y)####Returns the index from the given x,y arguments."+linSeparator + 
			"Overlay.removeRois(name)####Removes the ROI with the given name."+linSeparator + 
			"Overlay.getBounds(index,x,y,width,height)####Returns the bounds of the overlay."+linSeparator +
			"Overlay.remove####Removes the current overlay."+linSeparator + 
			"Overlay.clear####Resets the overlay without updating the display."+linSeparator + 
			"Overlay.size####Returns the size (selection count) of the current overlay. Returns zero if the image does not have an overlay."+linSeparator + 
			"Overlay.addSelection####Adds the current selection to the overlay."+linSeparator + 
			"Overlay.addSelection(strokeColor)####Sets the stroke color (\"red\",\"green\",\"ff8800\",etc.) of the current selection and adds it to the overlay."+linSeparator + 
			"Overlay.addSelection(strokeColor,strokeWidth)####Sets the stroke color (\"blue\",\"yellow\",\"ffaa77\" etc.) and stroke width of the current selection and adds it to the overlay."+linSeparator + 
			"Overlay.addSelection(\"\",0,fillColor)####Sets the fill color (\"red\",\"green\",etc.) of the current selection and adds it to the overlay."+linSeparator + 
			"Overlay.activateSelection(index)####Activates the specified overlay selection."+linSeparator + 
			"Overlay.moveSelection(index,x,y)####Moves the specified selection to the specified location."+linSeparator + 
			"Overlay.removeSelection(index)####Removes the specified selection from the overlay."+linSeparator + 
			"Overlay.copy####Copies the overlay on the current image to the overlay clipboard."+linSeparator + 
			"Overlay.fill(color)####Fills all the selections in the overlay with 'color'. Requires 1.53h."+linSeparator + 
			"Overlay.fill(color1, color2)####Fills all the selections in the overlay with 'color1' after clearing the the image to 'color2'. Requires 1.53h."+linSeparator + 
			"Overlay.paste####Copies the overlay on the overlay clipboard to the current image."+linSeparator + 
			"Overlay.drawLabels(boolean)####Enables/disables overlay labels."+linSeparator +
			"Overlay.setLabelFontSize(size,options)####Sets the label font size. The options string can contain 'scale' (enlarge labels when image zoomed),'bold' (display bold labels) or 'back' (display labels with contrasting background color."+linSeparator +
			"Overlay.setLabelColor(color)####Sets the color used to draw labels."+linSeparator +
			"Overlay.selectable(false)####Prevents the selections in this overlay from being activated by clicking on their labels or by long clicking. Requires 1.51r."+linSeparator +
			"Overlay.setLabelFont(font,true)####Causes labels in overlays to scale as the image is zoomed."+linSeparator+
			"Overlay.measure####Measures all the selections in the current overlay."+linSeparator+
			"Overlay.setStrokeColor(color)####Sets the stroke color all the selections in the current overlay."+linSeparator+
			"Overlay.setStrokeWidth(width)####Sets the stroke width all the selections in the current overlay."+linSeparator+			
			"Overlay.flatten####Creates a new RGB image that has the overlay rendered as pixel data."+linSeparator+		
			"Overlay.update(index)####Updates the overlay with the given index."+linSeparator+		
			"Overlay.useNamesAsLabels(boolean)####Sets the overlay names as labels."+linSeparator+
			"Overlay.xor()####Creates a selection that is the result of XORing all the selections in the overlay that have an index in 'array'. Requires 1.53h."+linSeparator +
			"parseFloat(string)####Converts the string argument to a number and returns it. Returns NaN (Not a Number) if the string cannot be converted into a number. Use the isNaN() function to test for NaN. For examples,see ParseFloatIntExamples."+linSeparator + 
			"parseInt(string)####Converts string to an integer and returns it. Returns NaN if the string cannot be converted into a integer."+linSeparator + 
			"parseInt(string,radix)####Converts string to an integer and returns it. The optional second argument (radix) specifies the base of the number contained in the string. The radix must be an integer between 2 and 36. For radixes above 10,the letters of the alphabet indicate numerals greater than 9. Set radix to 16 to parse hexadecimal numbers. Returns NaN if the string cannot be converted into a integer. For examples,see ParseFloatIntExamples."+linSeparator + 
			"PI####Returns the constant π (3.14159265),the ratio of the circumference to the diameter of a circle."+linSeparator+
			"Plot.create(\"Title\",\"X-axis Label\",\"Y-axis Label\",xValues,yValues)####Generates a plot using the specified title,axis labels and X and Y coordinate arrays. If only one array is specified it is assumed to contain the Y values and a 0..n-1 sequence is used as the X values. It is also permissible to specify no arrays and use Plot.setLimits() and Plot.add() to generate the plot. Use Plot.show() to display the plot in a window,or it will be displayed automatically when the macro exits."+linSeparator + 
			"Plot.add(type,xValues,yValues)####Adds a curve,set of points or error bars to a plot created using Plot.create(). If only one array is specified it is assumed to contain the Y values and a 0..n-1 sequence is used as the X values. The first argument (type) can be \"line\",\"circles\",\"boxes\",\"triangles\",\"crosses\",\"dots\",\"x\",\"connected\" (requires 1.49t),\"error bars\" (in y direction) or \"xerror bars\". In 1.49t or later,error bars apply to the last dataset provided by Plot.create or Plot.add."+linSeparator + 
			"Plot.addHistogram(values,binWidth,binCenter)####Creates a 'staircase' histogram from array 'values'. If 'binWidth' is 0,automatic binning is applied. 'binCenter' is optional with default=0. BinCenter can,for example,be set to an expected symmetry point for avoiding artificial asymmetry. Requires 1.52f."+linSeparator + 
			"Plot.addLegend(labels)####Adds a legend to a plot."+linSeparator + 
			"Plot.addLegend(labels,options)####Adds a legend with options to a plot."+linSeparator + 
			"Plot.drawVectors(xStarts,yStarts,xEnds,yEnds)####Draws arrows from the starting to ending coordinates contained in the arrays."+linSeparator +
			"Plot.appendToStack()####Appends a plot to a stack (see example 'Dynamic Plot')."+linSeparator +
			"Plot.drawShapes(\"rectangles\",lefts,tops,rights,bottoms)####Draws one or more rectangles. The four arguments (values or arrays) hold rectangle coordinates. Requires 1.49u."+linSeparator +
			"Plot.drawGrid()####Redraws the grid above previous plots. Requires 1.49u."+linSeparator +
			"Plot.drawLine(x1,y1,x2,y2)####Draws a line between x1,y1 and x2,y2,using the coordinate system defined by Plot.setLimits()."+linSeparator +
			"Plot.drawBoxes(\"boxes width=30\",x,y1,y2,y3,y4,y5)####Draws a boxplot,where 'width' is in pixels,array 'x' holds x-positions and arrays 'y1'..'y5' hold the quartile borders in ascending order. Secondary color will fill the box. For horizontal boxes,use \"boxesx width=30\" instead. Requires 1.49u."+linSeparator + 
			"Plot.drawNormalizedLine(x1,y1,x2,y2)####Draws a line using a normalized 0-1,0-1 coordinate system,with (0,0) at the top left and (1,1) at the lower right corner."+linSeparator + 
			"Plot.addText(\"A line of text\",x,y)####Adds text to the plot at the specified location,where (0,0) is the upper left corner of the the plot frame and (1,1) is the lower right corner. Call Plot.setJustification() to have the text centered or right justified. Plot.setLimits(xMin,xMax,yMin,yMax)"+linSeparator + 
			"Plot.removeNaNs####Removes NaNs in the current plot"+linSeparator + 
			"Plot.replace()####Replaces the current plot"+linSeparator + 
			"Plot.setLimits(xMin,xMax,yMin,yMax)####Sets the range of the x-axis and y-axis of plots. With version 1.50g and later,when 'NaN' is used as a limit,the range is calculated automatically from the plots that have been added so far."+linSeparator + 
			"Plot.getLimits(xMin,xMax,yMin,yMax)####Returns the current axis limits. Note that min>max if the axis is reversed. Requires 1.49t."+linSeparator + 
			"Plot.setLimitsToFit()####Sets the range of the x and y axes to fit all data. Requires 1.49t."+linSeparator + 
			"Plot.setColor(color)####Specifies the color used in subsequent calls to Plot.add() or Plot.addText(). The argument can be \"black\",\"blue\",\"cyan\",\"darkGray\",\"gray\",\"green\",\"lightGray\",\"magenta\",\"orange\",\"pink\",\"red\",\"white\",\"yellow\",or a hex value like \"#ff00ff\". Note that the curve specified in Plot.create() is drawn last."+linSeparator + 
			"Plot.setColor(color1,color2)####This is a two argument version of Plot.setColor,where the second argument is used for filling symbols or as the line color for connected points. Requires 1.49t."+linSeparator + 
			"Plot.setBackgroundColor(color)####Sets the background color of the plot frame. Requires 1.49h."+linSeparator + 
			"Plot.setLineWidth(width)####Specifies the width of the line used to draw a curve. Points (circle,box,etc.) are also drawn larger if a line width greater than one is specified. Note that the curve specified in Plot.create() is the last one drawn before the plot is dispayed or updated."+linSeparator + 
			"Plot.setJustification(\"center\")####Specifies the justification used by Plot.addText(). The argument can be \"left\",\"right\" or \"center\". The default is \"left\"."+linSeparator + 
			"Plot.setLegend(\"label1\\tlabel2...\",\"options\")####Creates a legend for each of the data sets added with Plot.create and Plot.add. In the first argument,the labels for the data sets should be separated with tab or newline characters. The optional second argument can contain the legend position (\"top-left\",\"top-right\",\"bottom-left\",\"bottom-right\"; default is automatic positioning),\"bottom-to-top\" for reversed sequence of the labels,and \"transparent\" to make the legend background transparent. Requires 1.49t."+linSeparator + 
			"Plot.setFrameSize(width,height)####Sets the plot frame size in pixels,overriding the default size defined in the Edit>Options>Profile Plot Options dialog box."+linSeparator + 
			"Plot.getFrameBounds(x,y,width,height)####Returns the plot frame bounds."+linSeparator +
			"Plot.setIntervals()####Set the plot interval."+linSeparator +
			"Plot.getCurrentFont()####Returns the current plot font."+linSeparator +
			"Plot.getDefaultFont()####Returns the default plot font."+linSeparator +
			"Plot.setLogScaleX(boolean)####Sets the x axis scale to Logarithmic,or back to linear if the optional boolean argument is false. In versions up to 1.49s,it must be called immediately after Plot.create and before Plot.setLimits. See the LogLogPlot macro for an example."+linSeparator + 
			"Plot.setWindowSize(width,height)####Sets the plot size."+linSeparator + 		
			"Plot.setLogScaleY(boolean)####Sets the y axis scale to Logarithmic,or back to linear if the optional boolean argument is false."+linSeparator + 
			"Plot.setXYLabels(\"x Label\",\"y Label\")####Sets the axis labels. Requires 1.49t."+linSeparator + 
			"Plot.setFontSize(size,\"options\")####Sets the default font size in the plot (otherwise specified in Profile Plot Options),used e.g. for axes labels. Can be also called prior to Plot.addText. The optional second argument can include \"bold\" and/or \"italic\". Requires 1.49t."+linSeparator + 
			"Plot.setAxisLabelSize(size,\"options\")####Sets the fort size of the axis labels. The optional second argument can include \"bold\" and/or \"italic\". Requires 1.49t. Plot.setFormatFlags(\"11001100001111\")"+linSeparator + 
			"Plot.setStyle(index,styleString)####Set the style of the plot object (curve,label,etc.) with the specified index,using a comma-delimiterd string (\"color1,color2,line width,symbol\"). For an example,run the Help>Examples>Plots>Plot Styles command. Requires 1.52h."+linSeparator + 
			"Plot.setFormatFlags(\"11001100001111\")####Controls whether to draw axes labels,grid lines and ticks (major/minor/ticks for log axes). Use the macro recorder and More>>Axis Options in any plot window to determine the binary code. Requires 1.49t."+linSeparator + 
			"Plot.useTemplate(\"plot name\" or id)####Transfers the formatting of an open plot window to the current plot. Requires 1.49t."+linSeparator + 
			"Plot.makeHighResolution(factor)####Creates a high-resolution image of the plot enlarged by factor. Add the second argument \"disable\" to avoid antialiased text. Requires 1.49t. Plot.show()"+linSeparator + 
			"Plot.setOptions()####Sets the plot options."+linSeparator + 
			"Plot.show()####Displays the plot generated by Plot.create(),Plot.add(),etc. in a window. This function is automatically called when a macro exits."+linSeparator + 
			"Plot.update()####Draws the plot generated by Plot.create(),Plot.add(),etc. in an existing plot window. Equivalent to Plot.show if no plot window is open."+linSeparator +
			"Plot.getResultsTableWithLabels()####Returns the results table with labels."+linSeparator +
			"Plot.getValues(xpoints,ypoints)####Returns the values displayed by clicking on \"List\" in a plot or histogram window."+linSeparator + 
			"Plot.showValues()####Displays the plot values in the Results window. Must be preceded by Plot.show."+linSeparator +
			"Plot.showValuesWithLabels()####Displays the plot values in the Results window with labels. Must be preceded by Plot.show."+linSeparator +
			"pow(base,exponent)####Returns the value of base raised to the power of exponent."+linSeparator + 
			"print(string)####Outputs a string to the \"Log\" window. Numeric arguments are automatically converted to strings. Starting with ImageJ v1.34b,print() accepts multiple arguments. For example,you can use print(x,y,width,height) instead of print(x+\" \"+y+\" \"+width+\" \"+height). If the first argument is a file handle returned by File.open(path),then the second is saved in the refered file (see SaveTextFileDemo). Numeric expressions are automatically converted to strings using four decimal places,or use the d2s function to specify the decimal places. For example,print(2/3) outputs \"0.6667\" but print(d2s(2/3,1)) outputs \"0.7\". Starting with ImageJ 1.37j,print() accepts commands such as \"\\\\Clear\",\"\\\\Update:<text>\" and \"\\\\Update<n>:<text>\" (for n<26) that clear the \"Log\" window and update its contents. For example,print(\"\\\\Clear\") erases the Log window,print(\"\\\\Update:new text\") replaces the last line with \"new text\" and print(\"\\\\Update8:new 8th line\") replaces the 8th line with \"new 8th line\". Refer to the LogWindowTricks macro for an example. Starting with ImageJ 1.38m,the second argument to print(arg1,arg2) is appended to a text window or table if the first argument is a window title in brackets,for example print(\"[My Window]\",\"Hello,world\"). With text windows,newline characters (\"\\n\") are not automatically appended and text that starts with \"\\\\Update:\" replaces the entire contents of the window. Refer to the PrintToTextWindow,Clock and ProgressBar macros for examples. The second argument to print(arg1,arg2) is appended to a table (e.g.,ResultsTable) if the first argument is the title of the table in brackets. Use the Plugins>New command to open a blank table. Any command that can be sent to the \"Log\" window (\"\\\\Clear\",\"\\\\Update:<text>\" ,etc.) can also be sent to a table. Refer to the SineCosineTable and TableTricks macros for examples."+linSeparator + 
			"Property.set(key,value)####Sets a key, value property."+linSeparator + 
			"Property.get(key)####Returns the property of the given key."+linSeparator + 
			"Property.getInfo####Returns the property information."+linSeparator +
			"Property.getSliceLabel####Returns the slice label information."+linSeparator +
			"Property.setSliceLabel(string)####Sets the the given string as slice label."+linSeparator +
			"Property.getDicomTag(string)####Returns the Dicom tag of the string argument."+linSeparator +	
			"Property.setList(str)####Sets the key/value pairs in the string list as image properties."+linSeparator +	
			"Property.getList####Returns the image properties as a string."+linSeparator +	
			"random####Returns a random number between 0 and 1."+linSeparator +
			"random(\"seed\",seed)####Sets the seed (a whole number) used by the random() function."+linSeparator+
			"rename(name)####Changes the title of the active image to the string name."+linSeparator + 
			"replace(string,old,new)####Returns the new string that results from replacing all occurrences of old in string with new,where old and new are single character strings. If old or new are longer than one character,each substring of string that matches the regular expression old is replaced with new. See also: matches."+linSeparator + 
			"requires(\"1.29p\")####Display a message and aborts the macro if the ImageJ version is less than the one specified. See also: getVersion."+linSeparator + 
			"reset####Restores the backup image created by the snapshot function. Note that reset() and run(\"Undo\") are basically the same,so only one run() call can be reset."+linSeparator + 
			"resetMinAndMax####With 16-bit and 32-bit images,resets the minimum and maximum displayed pixel values (display range) to be the same as the current image's minimum and maximum pixel values. With 8-bit images,sets the display range to 0-255. With RGB images,does nothing. See the DisplayRangeMacros for examples."+linSeparator + 
			"resetThreshold####Disables thresholding. See also: setThreshold,setAutoThreshold,getThreshold."+linSeparator + 
			"restoreSettings####Restores Edit/Options submenu settings saved by the saveSettings() function."+linSeparator +
			"ResultsTable.saveColumnHeaders(boolean)####When set to 'true' the Results Table is saved with headers."+linSeparator + 
			"Roi.contains(x,y)####Returns \"1\" if the point x,y is inside the current selection or \"0\" if it is not. Aborts the macro if there is no selection. Requires 1.49h. See also: selectionContains."+linSeparator + 
			"Roi.getBounds(x,y,width,height)####Returns the location and size of the selection's bounding rectangle."+linSeparator + 
			"Roi.getCoordinates(xpoints,ypoints)####Returns,as two arrays,the x and y coordinates that define this selection."+linSeparator + 
			"Roi.getContainedPoints(xpoints,ypoints)####Returns,as two arrays,the x and y coordinates of the pixels inside the current selection. Aborts the macro if there is no selection."+linSeparator + 
			"Roi.getDefaultColor####Returns the current default selection color."+linSeparator + 	
			"Roi.getDefaultGroup####Returns the default group (a positive number) of the current selection, or zero if the selection is not in a group. Requires 1.52t."+linSeparator + 
			"Roi.getDefaultStrokeWidth####Returns the default ROI stroke width."+linSeparator + 
			"Roi.getFloatBounds(x, y, width, height)####Returns the location and size of the selection's bounding rectangle as real numbers."+linSeparator +
			"Roi.getGroup(group)####Returns the group (a positive number) of the current selection, or zero if the selection is not in a group. Requires 1.52t."+linSeparator + 		
			"Roi.getGroupNames()####Returns the group names as a comma-delimeted string. Requires 1.53b."+linSeparator + 	
			"Roi.getFeretPoints####Returns,as two arrays,the x and y coordinates of the Feret diameter points."+linSeparator + 
			"Roi.getStrokeColor####Returns the selection stroke color."+linSeparator + 
			"Roi.getFillColor####Returns the selection fill color."+linSeparator + 
			"Roi.getName####Returns the selection name or an empty string if the selection does not have a name."+linSeparator +			
			"Roi.getPosition(channel,slice,frame)####Returns the position of the ROI."+linSeparator + 
			"Roi.setPosition(channel,slice,frame)####Sets the position of the ROI."+linSeparator + 						
			"Roi.getProperty(key)####Returns the value (a string) associated with the specified key or an empty string if the key is not found."+linSeparator + 
			"Roi.setDefaultGroup(group)####Sets the default group (a positive number) of the current selection. Zero sets the default group to \"none\". Requires 1.52t."+linSeparator + 
			"Roi.setGroup(group)####Sets the group (a positive number) of the current selection. Zero sets the group to \"none\". See also: RoiManager.selectGroup and RoiManager.setGroup. Requires 1.52t."+linSeparator +
			"Roi.setGroupNames(String)####Sets the group names from a comma-delimeted string. Requires 1.53b."+linSeparator + 
			"Roi.setPosition(slice)####Sets the current ROI to the given slice."+linSeparator + 
			"RoiManager.setPosition(c,z,t)####Sets the position of the different image channels."+linSeparator +
			"Roi.setProperty(key,value)####Adds the specified key and value pair to the selection properties. Assumes a value of \"1\" (true) if there is only one argument."+linSeparator + 
			"Roi.getProperties####Returns all selection properties or an empty string if the selection does not have properties."+linSeparator + 
			"Roi.getSplineAnchors(x,y)####Returns the x and y coordinates of the anchor points of a spline fitted selection."+linSeparator + 
			"Roi.setAntiAlias(boolean))####Control antialiasing when drawing selections."+linSeparator +
			"Roi.setPolygonSplineAnchors(x,y)####Creates or updates a spline fitted polygon selection."+linSeparator + 
			"Roi.setPolylineSplineAnchors(x,y)####Creates or updates a spline fitted polyline selection."+linSeparator + 
			"Roi.getType####Returns,as a string,the type of the current selection."+linSeparator + 
			"Roi.move(x,y)####Moves the selection to the specified location."+linSeparator + 
			"Roi.remove####Deletes the selection, if any, from the active image."+linSeparator +
			"Roi.setStrokeColor(color)####Sets the selection stroke color (\"red\",\"5500ff00\". etc.)."+linSeparator + 
			"Roi.setStrokeColor(red,green,blue)####Sets the selection stroke color,where 'red','green' and 'blue' are integers (0-255)."+linSeparator + 
			"Roi.setStrokeColor(rgb)####Sets the selection stroke color,where 'rgb' is an integer."+linSeparator + 
			"Roi.setFillColor(color)####Sets the selection fill color (\"red\",\"5500ff00\". etc.)."+linSeparator + 
			"Roi.setFillColor(red,green,blue)####Sets the selection fill color,where 'red','green' and 'blue' are integers (0-255)."+linSeparator + 
			"Roi.setFillColor(rgb)####Sets the selection fill color,where 'rgb' is an integer."+linSeparator + 
			"Roi.setName(name)####Sets the selection name."+linSeparator + 
			"Roi.setStrokeWidth(width)####Sets the selection stroke width."+
			"Roi.setDefaultStrokeWidth()####Sets the default selection stroke width."+
			"Roi.setJustification(str)####Sets the ROI 'justification' (center, right)."+
			"Roi.setFontSize(size)####Sets the font size of the ROI."+
			"Roi.translate(x,y)####Translates a ROI selection with the given x,y arguments."+
			"roiManager(\"and\")####Uses the conjunction operator on the selected ROIs,or all ROIs if none are selected,to create a composite selection."+linSeparator + 			
			"roiManager(\"add\")####Adds the current selection to the ROI Manager."+linSeparator + 			
			"roiManager(\"add & draw\")####Outlines the current selection and adds it to the ROI Manager."+linSeparator + 
 			"roiManager(\"combine\")####Uses the union operator on the selected ROIs to create a composite selection. Combines all ROIs if none are selected."+linSeparator + 
 			"roiManager(\"count\")####Returns the number of ROIs in the ROI Manager list."+linSeparator + 
			"roiManager(\"delete\")####Deletes the selected ROIs from the list,or deletes all ROIs if none are selected."+linSeparator + 
			"roiManager(\"deselect\")####Deselects all ROIs in the list. When ROIs are deselected,subsequent ROI Manager commands are applied to all ROIs."+linSeparator + 
			"roiManager(\"draw\")####Draws the selected ROIs,or all ROIs if none are selected,using the equivalent of the Edit>Draw command."+linSeparator + 
			"roiManager(\"fill\")####Fills the selected ROIs,or all ROIs if none are selected,using the equivalent of the Edit>Fill command."+linSeparator + 
			"roiManager(\"index\")####Returns the index of the currently selected ROI on the list,or -1 if the list is empty or no ROIs are selected. Returns the index of the first selected ROI if more than one is selected"+linSeparator + 
			"roiManager(\"measure\")####Measures the selected ROIs,or if none is selected,all ROIs on the list."+linSeparator +  
			"roiManager(\"multi measure\")####Measures all the ROIs on all slices in the stack,creating a Results Table with one row per slice."+linSeparator + 
			"roiManager(\"multi-measure append\")####Measures all the ROIs on all slices in the stack,appending the measurements to the Results Table,with one row per slice."+linSeparator + 
			"roiManager(\"multi-measure one\")####Measures all the ROIs on all slices in the stack,creating a Results Table with one row per measurement."+linSeparator + 
			"roiManager(\"multi plot\")####Plots the selected ROIs,or all ROIs if none are selected,on a single graph."+linSeparator + 
			"roiManager(\"open\",file-path)####Opens a .roi file and adds it to the list or opens a ZIP archive (.zip file) and adds all the selections contained in it to the list."+linSeparator + 
			"roiManager(\"remove slice info\")####Removes the information in the ROI names that associates them with particular stack slices."+linSeparator + 
			"roiManager(\"rename\",name)####Renames the selected ROI. You can get the name of an ROI on the list using call(\"ij.plugin.frame.RoiManager.getName\",index)."+linSeparator + 
			"roiManager(\"reset\")####Deletes all ROIs on the list."+linSeparator + 
			"roiManager(\"save,file-path)####Saves all the ROIs on the list in a ZIP archive."+linSeparator + 
			"roiManager(\"save selected\",file-path)####Saves the selected ROI as a .roi file."+linSeparator + 
			"roiManager(\"select\",index)####Selects an item in the ROI Manager list,where index must be greater than or equal zero and less than the value returned by roiManager(\"count\"). Note that macros that use this function sometimes run orders of magnitude faster in batch mode. Use roiManager(\"deselect\") to deselect all items on the list. For an example,refer to the ROI Manager Stack Demo macro."+linSeparator + 
			"roiManager(\"select\",indexes)####Selects multiple items in the ROI Manager list,where indexes is an array of integers,each of which must be greater than or equal to 0 and less than the value returned by roiManager(\"count\"). The selected ROIs are not highlighted in the ROI Manager list and are no longer selected after the next ROI Manager command is executed."+linSeparator + 
			"roiManager(\"show all\")####Displays all the ROIs as an overlay."+linSeparator + 
			"roiManager(\"show all with labels\")####Displays all the ROIs as an overlay,with labels."+linSeparator +  
			"roiManager(\"show all without labels\")####Displays all the ROIs as an overlay,without labels."+linSeparator +  
			"roiManager(\"show none\")####Removes the ROI Manager overlay."+linSeparator +  
			"roiManager(\"sort\")####Sorts the ROI list in alphanumeric order."+linSeparator + 
			"roiManager(\"split\")####Splits the current selection (it must be a composite selection) into its component parts and adds them to the ROI Manager."+linSeparator +  
			"roiManager(\"update\")####Replaces the selected ROI on the list with the current selection."+linSeparator +					
			"RoiManager.delete(index)####Deletes the selection at the specified index."+linSeparator + 
			"RoiManager.getName(index)####Returns the name of the selection with the specified index, or an empty string if the selection does not have a name."+linSeparator + 
			"RoiManager.multiCrop(dir, options)####If 'options' contains \"save\", saves the contents of the selected ROIs in TIFF format as separate images, where 'dir' is the directory path. Add \" png\" or \" jpg\" to save in PNG or JPEG format. Add ' show' and the images will be displayed in a stack. Requires 1.53d."+linSeparator + 
			"RoiManager.setGroup(group)####Sets the group of the selected ROIs. See also: Roi.setGroup, Roi.getGroup, Roi.setDefaultGroup and Roi.getDefaultGroup."+linSeparator + 
			"RoiManager.select(index)####Activates the selection at the specidied index."+linSeparator + 
			"RoiManager.selected####Returns the number of selected ROIs in the ROI Manager."+linSeparator + 
			"RoiManager.selectGroup(group)####Selects all ROIs in the ROI Manager that belong to group."+linSeparator + 
			"RoiManager.setPosition####Sets the position of the selected selections."+linSeparator +
			"RoiManager.size####Returns the ROI Manager selection count."+linSeparator +
			"round(n)####Returns the closest integer to n. See also: floor."+linSeparator + 
			"run(\"command\"[,\"options\"])####Executes an ImageJ menu command. The optional second argument contains values that are automatically entered into dialog boxes (must be GenericDialog or OpenDialog). Use the Command Recorder (Plugins>Macros>Record) to generate run() function calls. Use string concatentation to pass a variable as an argument. For examples,see the ArgumentPassingDemo macro."+linSeparator + 
			"runMacro(name)####Runs the specified macro file,which is assumed to be in the Image macros folder. A full file path may also be used. The \".txt\" extension is optional. Returns any string argument returned by the macro. May have an optional second string argument that is passed to macro. For an example,see the CalculateMean macro. See also: eval and getArgument."+linSeparator + 
			"runMacro(name,arg)####Runs the specified macro or script,which is assumed to be in the macros folder,or use a full file path. The string argument 'arg' can be retrieved by the macro or script using the getArgument() function. Returns the string argument returned by the macro or the last expression evaluated in the script. See also: getArgument."+linSeparator+
			"save(path)####Saves an image,lookup table,selection or text window to the specified file path. The path must end in \".tif\",\".jpg\",\".gif\",\".zip\",\".raw\",\".avi\",\".bmp\",\".fits\",\".png\",\".pgm\",\".lut\",\".roi\" or \".txt\"."+linSeparator + 
			"saveAs(format,path)####Saves the active image,lookup table,selection,measurement results,selection XY coordinates or text window to the specified file path. The format argument must be \"tiff\",\"jpeg\",\"gif\",\"zip\",\"raw\",\"avi\",\"bmp\",\"fits\",\"png\",\"pgm\",\"text image\",\"lut\",\"selection\",\"measurements\",\"xy Coordinates\" or \"text\". Use saveAs(format) to have a \"Save As\" dialog displayed."+linSeparator + 
			"saveSettings()####Saves most Edit/Options submenu settings so they can be restored later by calling restoreSettings()."+linSeparator + 
			"screenHeight####Returns the screen height in pixels. See also: getLocationAndSize,setLocation."+linSeparator + 
			"screenWidth####Returns the screen width in pixels."+linSeparator +
			"selectionContains(x,y)####Returns true if the point x,y is inside the current selection. Aborts the macro if there is no selection."+linSeparator+
			"selectionName####Returns the name of the current selection,or an empty string if the selection does not have a name. Aborts the macro if there is no selection. See also: setSelectionName and selectionType."+linSeparator + 
			"selectionType()####Returns the selection type,where 0=rectangle,1=oval,2=polygon,3=freehand,4=traced,5=straight line,6=segmented line,7=freehand line,8=angle,9=composite and 10=point. Returns -1 if there is no selection. For an example,see the ShowImageInfo macro."+linSeparator + 
			"selectImage(id)####Activates the image with the specified ID (a negative number). If id is greater than zero,activates the idth image listed in the Window menu. With ImageJ 1.33n and later,id can be an image title (a string)."+linSeparator + 
			"selectWindow(\"name\")####Activates the image window with the title \"name\". Also activates non-image windows in v1.30n or later."+linSeparator + 
			"setAutoThreshold()####Uses the \"Default\" method to determine the threshold. It may select dark or bright areas as thresholded,as was the case with the Image>Adjust>Threshold \"Auto\" option in ImageJ 1.42o and earlier. See also: setThreshold,getThreshold,resetThreshold."+linSeparator + 
			"setAutoThreshold(method)####Uses the specified method to set the threshold levels of the current image. Use the getList(\"threshold.methods\") function to get a list of the available methods. Add \"dark\" to the method name if the image has a dark background. Add \"16-bit\" to use the full 16-bit histogram when calculating the threshold of 16-bit images. Add \"stack\" to use histogram of the entire stack when calculating the threshold. For an example, see the AutoThresholdingDemo macro."+linSeparator+
			"setBackgroundColor(r,g,b)####Sets the background color,where r,g,and b are >= 0 and <= 255."+linSeparator + 
			"setBackgroundColor(rgb)####Sets the background color,where rgb is an RGB pixel value. See also: getValue(\"rgb.background\")."+linSeparator+
			"setBatchMode(arg)####Controls whether images are visible or hidden during macro execution. If arg is 'true',the interpreter enters batch mode and newly opened images are not displayed. If arg is 'false',exits batch mode and disposes of all hidden images except for the active image,which is displayed in a window. The interpreter also exits batch mode when the macro terminates,disposing of all hidden images. With ImageJ 1.48h or later,you can use 'show' and 'hide' arguments to individually show or hide images.�By not displaying and updating images,batch mode macros run up to 20 times faster. Examples: BatchModeTest,BatchMeasure,BatchSetScale and ReplaceRedWithMagenta."+linSeparator + 
			"setBatchMode(\"exit and display\")####Exits batch mode and displays all hidden images."+linSeparator + 
			"setBatchMode(\"show\")####Displays the active hidden image,while batch mode remains in same state."+linSeparator + 
			"setBatchMode(\"hide\")####Enters (or remains in) batch mode and hides the active image."+linSeparator+
			"setColor(r,g,b)####Sets the drawing color,where r,g,and b are >= 0 and <= 255. With 16 and 32 bit images,sets the color to 0 if r=g=b=0. With 16 and 32 bit images,use setColor(1,0,0) to make the drawing color the same is the minimum displayed pixel value. SetColor() is faster than setForegroundColor(),and it does not change the system wide foreground color or repaint the color picker tool icon,but it cannot be used to specify the color used by commands called from macros,for example run(\"Fill\")."+linSeparator + 
			"setColor(value)####Sets the drawing color. For 8 bit images,0<=value<=255. For 16 bit images,0<=value<=65535. For RGB images,use hex constants (e.g.,0xff0000 for red). This function does not change the foreground color used by run(\"Draw\") and run(\"Fill\")."+linSeparator + 
			"setColor(string)####Sets the drawing color,where 'string' can be \"black\",\"blue\",\"cyan\",\"darkGray\",\"gray\",\"green\",\"lightGray\",\"magenta\",\"orange\",\"pink\",\"red\",\"white\",\"yellow\",or a hex value like \"#ff0000\"."+linSeparator+
			"setFont(name,size[,style])####Sets the font used by the drawString function. The first argument is the font name. It should be \"SansSerif\",\"Serif\" or \"Monospaced\". The second is the point size. The optional third argument is a string containing \"bold\" or \"italic\",or both. The third argument can also contain the keyword \"antialiased\". For examples,run the TextDemo macro."+linSeparator + 
			"setFont(\"user\")####Sets the font to the one defined in the Edit>Options>Fonts window. See also: getInfo(\"font.name\"),getValue(\"font.size\") and getValue(\"font.height\"). "+linSeparator + 			
			"setForegroundColor(r,g,b)####Sets the foreground color,where r,g,and b are >= 0 and <= 255. See also: setColor."+linSeparator + 
			"setForegroundColor(rgb)####Sets the foreground color,where rgb is an RGB pixel value. See also: getValue(\"rgb.foreground\")."+linSeparator+
			"setJustification(\"center\")####Specifies the justification used by drawString() and Plot.addText(). The argument can be \"left\",\"right\" or \"center\". The default is \"left\"."+linSeparator + 
			"setKeyDown(keys)####Simulates pressing the shift,alt or space keys,where keys is a string containing some combination of \"shift\",\"alt\" or \"space\". Any key not specified is set \"up\". Use setKeyDown(\"none\") to set all keys in the \"up\" position. With ImageJ 1.38e or later,call setKeyDown(\"esc\") to abort the currently running macro or plugin. For examples,see the CompositeSelections,DoWandDemo and AbortMacroActionTool macros. See also: isKeyDown."+linSeparator + 
			"setLineWidth(width)####Specifies the line width (in pixels) used by drawLine(),lineTo(),drawRect() and drawOval()."+linSeparator + 
			"setLocation(x,y)####Moves the active image window to a new location. With v1.39e or later,moves the active window. See also: getLocationAndSize,screenWidth,screenHeight."+linSeparator + 
			"setLocation(x,y,width,height)####Moves and resizes the active image window. The new location of the top-left corner is specified by x and y,and the new size by width and height. Requires v1.39e."+linSeparator + 
			"setLut(reds,greens,blues)####Creates a new lookup table and assigns it to the current image. Three input arrays are required,each containing 256 intensity values. See the LookupTables macros for examples."+linSeparator + 
			"setMetadata(\"Info\",string)####Assigns the metadata in string to the \"Info\" image property of the current image. This metadata is displayed by Image>Show Info and saved as part of the TIFF header. See also: getMetadata. Requires v1.40b."+linSeparator + 
			"setMetadata(\"Label\",string)####Sets string as the label of the current image or stack slice. The first 60 characters,or up to the first newline,of the label are displayed as part of the image subtitle. The labels are saved as part of the TIFF header. See also: getMetadata. Requires v1.40b."+linSeparator + 
			"setMinAndMax(min,max)####Sets the minimum and maximum displayed pixel values (display range). See the DisplayRangeMacros for examples."+linSeparator + 
			"setMinAndMax(min,max,channels)####Sets the display range of specified channels in an RGB image,where 4=red,2=green,1=blue,6=red+green,etc. Note that the pixel data is altered since RGB images,unlike composite color images,do not have a LUT for each channel. Requires v1.42d."+linSeparator + 
			"setOption(measurement,boolean)####Enable disables Analyze Set Measurements options where 'measurement' can be \"Display label\", \"Limit to threshold\", \"Area\", \"Mean\", \"Std\", \"Perimeter\", \"Stack position\" or \"Add to overlay\"."+linSeparator +  
			"setOption(\"AntialiasedText\",boolean)####Controls the \"Antialiased text\" option in the Edit>Options>Fonts dialog. Requires v1.51h."+linSeparator + 
			"setOption(\"AutoContrast\",boolean)####Enables/disables the Edit>Options>Appearance \"Auto contrast stacks\" option. You can also have newly displayed stack slices contrast enhanced by holding the shift key down when navigating stacks."+linSeparator + 
			"setOption(\"Bicubic\",boolean)####Provides a way to force commands like Edit>Selection>Straighten, that normally use bilinear interpolation, to use bicubic interpolation."+linSeparator + 			
			"setOption(\"BlackBackground\",boolean)####Enables/disables the Process>Binary>Options \"Black background\" option."+linSeparator +  
			"setOption(\"Changes\",boolean)####Sets/resets the 'changes' flag of the current image. Set this option false to avoid \"Save Changes?\" dialog boxes when closing images."+linSeparator +  
			"setOption(\"CopyHeaders\",boolean)####Enables/disables the \"Copy column headers\" option in the Edit>Options>Input/Output dialog. See String.copyResults Requires v1.52p."+linSeparator + 		
			"setOption(\"DebugMode\",boolean)####Enables/disables the ImageJ debug mode. ImageJ displays information, such as TIFF tag values, when it is in debug mode. "+linSeparator +
			"setOption(\"DisablePopupMenu\",boolean)####Enables/disables the the menu displayed when you right click on an image."+linSeparator +
			"setOption(\"DisableUndo\",boolean)####Enables/disables the Edit>Undo command. Note that a setOption(\"DisableUndo\", true) call without a corresponding setOption(\"DisableUndo\", false) will cause Edit>Undo to not work as expected until ImageJ is restarted."+linSeparator +
			"setOption(\"ExpandableArrays\",boolean)####Enables/disables support for auto-expanding arrays (example)."+linSeparator +
			"setOption(\"FlipFitsImages\",boolean)####Controls whether images are flipped vertically by the FITS reader."+linSeparator +
			"setOption(\"FullRange16bitInversions\",boolean)####Set to have 16-bit images inverted using the full range (0-65535). Requires 1.54d."+linSeparator +
			"setOption(\"InterpolateLines\",boolean)####Sets/resets the 'Interpolate line profiles' option in Edit>Options>Plots."+linSeparator +
			"setOption(\"InvertY\",boolean)####Sets/resets the 'invertY' option of the active image."+linSeparator +
			"setOption(\"JFileChooser\",boolean)####Enables/disables use of the Java JFileChooser to open and save files instead of the native OS file chooser."+linSeparator +
			"setOption(\"Loop\",boolean)####Enables/disables the Image>Stacks>Tools>Animation Options \"Loop back and forth\" option."+linSeparator +
			"setOption(\"MonospacedText\",boolean)####Enables/disables monospaced text in the \"Log\" window. Requires 1.54c."+linSeparator +
			"setOption(\"OpenGrayscaleJpegsAsRGB\",boolean)####Enable to open grayscale RGB JPEGs as RGB images. Requires 1.54i."+linSeparator +
			"setOption(\"OpenUsingPlugins\",boolean)####Controls whether standard file types (TIFF, JPEG, GIF, etc.) are opened by external plugins or by ImageJ (example)."+linSeparator +
			"setOption(\"QueueMacros\",boolean)####Controls whether macros invoked using keyboard shortcuts run sequentially on the event dispatch thread (EDT) or in separate, possibly concurrent, threads (example). In \"QueueMacros\" mode, screen updates, which also run on the EDT, are delayed until the macro finishes."+linSeparator +
			"setOption(\"ScaleConversions\",boolean)####Enables/disables the \"Scale when converting\" option in the Edit>Options>Conversions dialog. When this option is enabled (the default), commands in the Image>Type> sub-menu scale from the min and max displayed pixel value to 0-255 when converting from 16-bit or 32-bit to 8-bit or to 0-65535 when converting from 32-bit to 16-bit. The min and max displayed pixel values can be set using the Image>Adjust>Brightness/Contrast dialog or the setMinAndMax() function. Call setOption(\"CalibrateConversions\", true) to have conversions to 8-bit and 16-bit density calibrated. Requires v1.52k."+linSeparator +
			"setOption(\"CalibrateConversions\",boolean)####Enables/disables the \"Calibrate conversions\" option in the Edit>Options>Conversions dialog. Conversions to 8-bit and 16-bit are density calibrated when this option is enabled, so results from measurements stay the same."+linSeparator +
			"setOption(\"setIJMenuBar\",boolean)####A function to set the menu bar."+linSeparator +
			"setOption(\"Show All\",boolean)####Enables/disables the the \"Show All\" mode in the ROI Manager."+linSeparator +
			"setOption(\"ShowAngle\",boolean)####Determines whether or not the \"Angle\" value is displayed in the Results window when measuring straight line lengths."+linSeparator +
			"setOption(\"ShowMin\",boolean)####Determines whether or not the \"Min\" value is displayed in the Results window when \"Min & Max Gray Value\" is enabled in the Analyze>Set Measurements dialog box."+linSeparator +
			"setOption(\"WaitForCompletion\",boolean)####Set false and the next exec() call will return null and not wait for the command being executed to finish. Requires v1.52u."+linSeparator +
			"setOption(\"WandAllPoints\",boolean)####Controls whether Wand selections with straight lines longer than one pixel should have intermediate points with single-pixel spacing. Requires v1.51q."+linSeparator +	
			"setPasteMode(mode)####Sets the transfer mode used by the Edit>Paste command,where 'mode' is \"Copy\",\"Blend\",\"Average\",\"Difference\",\"Transparent\",\"AND\",\"OR\",\"XOR\",\"Add\",\"Subtract\",\"Multiply\",or \"Divide\". In v1.37a or later,'mode' can also be \"Min\" or \"Max\"."+linSeparator + 
			"setPixel(x,y,value)####Stores value at location (x,y) of the current image. The screen is updated when the macro exits or call updateDisplay() to have it updated immediately."+linSeparator + 
			"setResult(\"Column\",row,value)####Adds an entry to the ImageJ results table or modifies an existing entry. The first argument specifies a column in the table. If the specified column does not exist,it is added. The second argument specifies the row,where 0<=row<=nResults. (nResults is a predefined variable.) A row is added to the table if row=nResults. The third argument is the value to be added or modified. Call setResult(\"Label\",row,string) to set the row label. Call updateResults() to display the updated table in the \"Results\" window. For examples,see the SineCosineTable and ConvexitySolidarity macros."+linSeparator + 
			"setRGBWeights(redWeight,greenWeight,blueWeight)####Sets the weighting factors used by the Analyze>Measure,Image>Type>8-bit and Analyze>Histogram commands when they convert RGB pixels values to grayscale. The sum of the weights must be 1.0. Use (1/3,1/3,1/3) for equal weighting of red,green and blue. The weighting factors in effect when the macro started are restored when it terminates. For examples,see the MeasureRGB,ExtractRGBChannels and RGB_Histogram macros."+linSeparator + 
			"setSelectionLocation(x,y)####Moves the current selection to (x,y),where x and y are the pixel coordinates of the upper left corner of the selection's bounding rectangle. The RoiManagerMoveSelections macro uses this function to move all the ROI Manager selections a specified distance. See also: getSelectionBounds."+linSeparator + 
			"setSelectionName(name)####Sets the name of the current selection to the specified name. Aborts the macro if there is no selection. See also: selectionName and selectionType."+linSeparator + 
			"setSlice(n)####Displays the nth slice of the active stack. Does nothing if the active image is not a stack. For an example,see the MeasureStack macros. See also: getSliceNumber,nSlices."+linSeparator + 
			"setThreshold(lower,upper)####Sets the lower and upper threshold levels. The values are uncalibrated except for 16-bit images (e.g.,unsigned 16-bit images). Starting with v1.34g,there is an optional third argument that can be \"red\",\"black & white\",\"over/under\" or \"no color\". See also: setAutoThreshold,getThreshold,resetThreshold."+linSeparator + 
			"setTool(name)####Switches to the specified tool,where name is \"rectangle\",\"elliptical\",\"brush\",\"polygon\",\"freehand\",\"line\",\"polyline\",\"freeline\",\"angle\",\"point\",\"wand\",\"text\",\"zoom\",\"hand\" or \"dropper\". Refer to the SetToolDemo macro for an example. Requires 1.39l."+linSeparator + 
			"setTool(id)####Switches to the specified tool,where 0=rectangle,1=oval,2=polygon,3=freehand,4=straight line,5=polyline,6=freeline,7=point,8=wand,9=text,10=spare,11=zoom,12=hand,13=dropper,14=angle,15...21=spare. See also: toolID."+linSeparator + 
			"setupUndo()####Call this function before drawing on an image to allow the user the option of later restoring the original image using Edit/Undo. Note that setupUndo() may not work as intended with macros that call the run() function. For an example,see the DrawingTools tool set."+linSeparator + 
			"setVoxelSize(width,height,depth,unit)####Defines the voxel dimensions and unit of length (\"pixel\",\"mm\",etc.) for the current image or stack. The depth argument is ignored if the current image is not a stack. See also: getVoxelSize."+linSeparator + 
			"setZCoordinate(z)####Sets the Z coordinate used by getPixel(),setPixel() and changeValues(). The argument must be in the range 0 to n-1,where n is the number of images in the stack. For an examples,see the Z Profile Plotter Tool."+linSeparator + 
			"showMessage(\"message\")####Displays \"message\" in a dialog box."+linSeparator + 
			"showMessage(\"title\",\"message\")####Displays \"message\" in a dialog box using \"title\" as the the dialog box title."+linSeparator + 
			"showMessageWithCancel([\"title\",]\"message\")####Displays \"message\" in a dialog box with \"OK\" and \"Cancel\" buttons. \"Title\" (optional) is the dialog box title. The macro exits if the user clicks \"Cancel\" button."+linSeparator + 
			"showProgress(progress)####Updates the ImageJ progress bar,where 0.0<=progress<=1.0. The progress bar is not displayed if the time between the first and second calls to this function is less than 30 milliseconds. It is erased when the macro terminates or progress is >=1.0."+linSeparator + 
			"showProgress(currentIndex,finalIndex)####Updates the progress bar,where the length of the bar is set to currentIndex/finalIndex of the maximum bar length. The bar is erased if currentIndex>finalIndex or finalIndex==0."+linSeparator+
			"showStatus(\"message\")####Displays a message in the ImageJ status bar."+linSeparator + 
			"showStatus(message,options)####Displays a message in the ImageJ status bar with the given options."+linSeparator + 
			"showText(\"string\")####Displays a string in a text window."+linSeparator + 
			"showText(\"Title\",\"string\")####Displays a string in a text window using the specified title."+linSeparator+
			"sin(angle)####Returns the sine of an angle (in radians)."+linSeparator + 
			"snapshot()####Creates a backup copy of the current image that can be later restored using the reset function. For examples,see the ImageRotator and BouncingBar macros. split(string,delimiters) Breaks a string into an array of substrings. Delimiters is a string containing one or more delimiter characters. The default delimiter set \" \\t\\n\\r\" (space,tab,newline,return) is used if delimiters is an empty string or split is called with only one argument. Returns a one element array if no delimiter is found."+linSeparator + 
			"split(string,delimiters)####Breaks a string into an array of substrings. Delimiters is a string containing one or more delimiter characters. The default delimiter set \" \\t\\n\\r\" (space,tab,newline,return) is used if delimiters is an empty string or split is called with only one argument. Multiple delimiters in the string are merged (taken as one) and delimiters at the start or end of the string are ignored unless the delimiter is a single comma,a single semicolon or a regular expression. With ImageJ 1.49f or later,delimiters can be also a regular expression enclosed in parentheses,e.g. delimiters=\"(\\n\\n)\" splits only at empty lines (two newline characters following each other). Note that split() may return empty strings when the second argument is \",\",\";' or \"\\n\". To avoid empty strings,use \",,\",\";;\" and \"\\n\\n\" as the second argument."+linSeparator+
			"sqrt(n)####Returns the square root of n. Returns NaN if n is less than zero."+linSeparator + 
			"Stack.isHyperstack####Returns true if the current image is a hyperstack."+linSeparator + 
			"Stack.getDimensions(width,height,channels,slices,frames)####Returns the dimensions of the current image."+linSeparator + 
			"Stack.setDimensions(channels,slices,frames)####Sets the 3rd,4th and 5th dimensions of the current stack."+linSeparator + 
			"Stack.setChannel(n)####Displays channel n."+linSeparator + 
			"Stack.setSlice(n)####Displays slice n."+linSeparator + 
			"Stack.setFrame(n)####Displays frame n."+linSeparator + 
			"Stack.getPosition(channel,slice,frame)####Returns the current position."+linSeparator +
			"Stack.getPointPosition(index)####Returns the position of the given point index."+linSeparator +			
			"Stack.setPosition(channel,slice,frame)####Sets the position."+linSeparator + 
			"Stack.getFrameRate()####Returns the frame rate (FPS)."+linSeparator + 
			"Stack.setFrameRate(fps)####Sets the frame rate."+linSeparator + 
			"Stack.getFrameInterval()####Returns the frame interval in time (T) units."+linSeparator + 
			"Stack.setFrameInterval(interval)####Sets the frame interval in time (T) units."+linSeparator + 
			"Stack.getUnits(X,Y,Z,Time,Value)####Returns the x,y,z,time and value units."+linSeparator + 
			"Stack.setTUnit(string)####Sets the time unit."+linSeparator +
			"Stack.setXUnit(string)####Sets the X-dimension unit."+linSeparator +
			"Stack.setYUnit(string)####Sets the Y-dimension unit."+linSeparator +
			"Stack.setZUnit(string)####Sets the Z-dimension unit."+linSeparator + 
			"Stack.setUnits(x,y,z,time,value)####Sets the units of a stack."+linSeparator + 
			"Stack.setDisplayMode(mode)####Sets the display mode,where mode is \"composite\",\"color\" or \"grayscale\". Requires a multi-channel stack and v1.40a or later."+linSeparator + 
			"Stack.getDisplayMode(mode)####Sets the string mode to the current display mode."+linSeparator + 
			"Stack.setActiveChannels(string)####Controls which channels in a composite color image are displayed,where string is a list of ones and zeros that specify the channels to display. For example,\"101\" causes channels 1 and 3 to be displayed."+linSeparator + 
			"Stack.getActiveChannels(string)####Returns a string that represents the state of the channels in a composite color image,where '1' indicates a displayed channel and '0' indicates an inactive channel."+linSeparator + 
			"Stack.swap(n1,n2)####Swaps the two specified stack images,where n1 and n2 are integers greater than 0 and less than or equal to nSlices."+linSeparator + 
			"Stack.getStatistics(voxelCount,mean,min,max,stdDev)####Calculates and returns stack statistics."+linSeparator + 
			"Stack.setOrthoViews(x,y,z)####If an Orthogonal Views is active,its crosspoint is set to x,y,z."+linSeparator + 
			"Stack.getOrthoViews(x,y,z)####Returns the x,y,z crosspoint."+linSeparator + 
			"Stack.getOrthoViewsIDs(xy,yz,xz)####Returns the image ID of the current Orthogonal Views."+linSeparator + 
			"Stack.getOrthoViewsID####Returns the image ID of the current Orthogonal Views,or zero if none is active."+linSeparator + 
			"Stack.startOrthoViews####Enables Orthogonal Views. Requires 1.54i"+linSeparator + 
			"Stack.stopOrthoViews####Stops the current Orthogonal Views and closes the \"YZ\" and \"XZ\" windows."+linSeparator+
			"startsWith(string,prefix)####Returns true (1) if string starts with prefix. See also: endsWith,indexOf,substring,toLowerCase,matches."+linSeparator+
			"String.resetBuffer####Resets (clears) the buffer."+linSeparator + 
			"String.append(str)####Appends str to the buffer."+linSeparator + 
			"String.buffer####Returns the contents of the buffer."+linSeparator + 
			"String.copy(str)####Copies str to the clipboard."+linSeparator + 
			"String.copyResults####Copies the Results table to the clipboard."+linSeparator +
			"String.getResultsHeadings####Returns the Results window headers."+linSeparator+
			"String.join(array)####Concatenates the given elements."+linSeparator + 
			"String.pad(n,length)####Pad 'n' with leading zeros to the specified length."+linSeparator + 
			"String.paste####Returns the contents of the clipboard."+linSeparator + 
			"String.setFontSize()####Sets the font size of the string."+
			"String.show(str)####Displays str in a text window."+linSeparator + 
			"String.show(title,str)####Displays str in a text window using title as the title."+linSeparator+
			"String.trim(string)####Eliminates leading and trailing spaces of the given string."+linSeparator+			
			"s.length####Returns the length of the string variable 's'."+linSeparator+
			"s.contains(s2)####Checks if the given string variable 's' contains the string varibale 's2'."+linSeparator+
			"s.trim####Trims the given string 's'."+linSeparator+
			"s.charAt(i)####Returns the char at position 'i' from the given string 's'."+linSeparator+
			"s.replace(s1,s2)####Replaces the given string 's1' with the given string 's2' of the string variable 's'."+linSeparator+
            "s.replaceAll(s1,s2)####Replaces all strings which match the given string 's1' with the given string 's2' of the string variable 's'."+linSeparator+
            "s.indexOf(s2)####Returns the index of the given string 's2' of the string variable 's'."+linSeparator+
            "s.lastIndexOf(s2)####Returns the last index of the given string 's2' of the string variable 's'."+linSeparator+
            "s.startsWith(s2)####Returns if the given string 's' starts with the string argument 's2'."+linSeparator+
            "s.endsWith(s2)####Returns if the given string 's' ends with with the string argument 's2'."+linSeparator+
            "s.matches(s2)####Returns if the given string 's' matches the string argument 's2'."+linSeparator+
            "s.substring(i1,i2)####Returns a substring from the given arguments of the string variable 's'."+linSeparator+
            "s.substring(i)####Returns a substring from the given argument of the string variable 's'."+linSeparator+
            "s.indexOf(s2)####Returns the index of the given string argument 's2' of the string variable 's'."+linSeparator+
            "s.toLowerCase####Returns the lower case of the string variable 's'."+linSeparator+
            "s.toUpperCase####Returns the upper case of the string variable 's'."+linSeparator+			
			"substring(string,index1,index2)####Returns a new string that is a substring of string. The substring begins at index1 and extends to the character at index2 - 1. See also: indexOf,startsWith,endsWith,replace."+linSeparator + 
			"substring(string,index)####Returns a substring of string that begins at index and extends to the end of string. Requires v1.41i."+linSeparator + 
			"Table.create()####Opens a new table."+linSeparator +
			"Table.reset()####Resets (clears) the table."+linSeparator +
			"Table.size()####Number of rows in the table."+linSeparator +
			"Table.title()####Title of the current table."+linSeparator +
			"Table.headings()####Column headings as a tab-delimited string."+linSeparator + 
			"Table.get(columnName,rowIndex)####Returns a numeric value."+linSeparator + 
			"Table.getColumn(columnName)####Returns the specified column as an array."+linSeparator + 
			"Table.getString(columnName,rowIndex)####Returns a string value."+linSeparator + 
			"Table.set(columnName,rowIndex,value)####Sets numeric or string value.\r\n" +
			"Table.setColumn(columnName,array)####Sets an array as a column."+linSeparator + 
			"Table.update()####Updates table window."+linSeparator + 
			"Table.applyMacro(macro)####Applies macro code to table."+linSeparator + 
			"Table.rename(title1,title2)####Renames a table."+linSeparator + 
			"Table.setSelection(firstRowIndex,lastRowIndex,title)####Selects a range of rows in the table identified by \"title\". Use range (-1,-1) for selecting none."+linSeparator +
			"Table.getSelectionStart(title)####Returns the index of first selected row in the table identified by \"title\",or - 1 if there is no selection."+linSeparator + 
			"Table.getSelectionEnd(title)####Returns the index of last selected row in the table identified by \"title\",or -1 if there is no selection."+linSeparator + 
			"Table.save(filePath)####saves the table."+linSeparator + 
			"Table.setLocationAndSize()####Sets the location and size of a table."+linSeparator + 
			"Table.open(filePath)####Opens a table."+linSeparator + 
			"Table.deleteRows(index1,index2)####Deletes specified rows."+linSeparator + 
			"Table.renameColumn(oldName,newName)####Renames a column."+linSeparator + 
			"Table.deleteColumn(columnName)####deletes specified column."+linSeparator + 
			"Table.showHistogramTable####Equivalent to clicking on \"List\" in a Histogram window. Requires 1.54j."+linSeparator + 
			"Table.showRowNumbers(boolean)####Enable/disable row numbers."+linSeparator + 
			"Table.saveColumnHeaders(boolean)####When set to 'true' the Table is saved with headers."+linSeparator + 
			"Table.showArrays(titleAndOptions,array1,array2,...)####Displays arrays in a table (like Array.show)."+linSeparator + 
			"Tools.getNumberFromList()####Returns the numbers from a list."+linSeparator + 
			"tan(angle)####Returns the tangent of an angle (in radians)."+linSeparator + 
			"toBinary(number)####Returns a binary string representation of number."+linSeparator + 
			"toHex(number)####Returns a hexadecimal string representation of number."+linSeparator + 
			"toLowerCase(string)####Returns a new string that is a copy of string with all the characters converted to lower case."+linSeparator + 
			"toolID####Returns the ID of the currently selected tool. See also: setTool."+linSeparator + 
			"toScaled(x,y)####Converts unscaled pixel coordinates to scaled coordinates using the properties of the current image or plot. Also accepts arrays."+linSeparator + 
			"toUnscaled(x,y)####Converts scaled coordinates to unscaled pixel coordinates using the properties of the current image or plot. Also accepts arrays. Refer to the AdvancedPlots macro set for examples."+linSeparator + 
			"toScaled(length)####Converts (in place) a length in pixels to a scaled length using the properties of the current image."+linSeparator + 
			"toUnscaled(length)####Converts (in place) a scaled length to a length in pixels using the properties of the current image. "+linSeparator+
			"toString(number)####Returns a decimal string representation of number. See also: toBinary,toHex,parseFloat and parseInt."+linSeparator + 
			"toString(number,decimalPlaces)####Converts number into a string,using the specified number of decimal places. Requires v1.39r."+linSeparator + 
			"toUpperCase(string)####Returns a new string that is a copy of string with all the characters converted to upper case."+linSeparator + 
			"updateDisplay()####Redraws the active image."+linSeparator + 
			"updateResults()####Call this function to update the \"Results\" window after the results table has been modified by calls to the setResult() function."+linSeparator + 
			"wait(n)####Delays (sleeps) for n milliseconds."+linSeparator + 
			"waitForUser(string)####Halts the macro and displays string in a dialog box. The macro proceeds when the user clicks \"OK\". Unlike showMessage,the dialog box is not modal,so the user can,for example,create a selection or adjust the threshold while the dialog is open. To display a multi-line message,add newline characters (\"\\n\") to string. This function is based on Michael Schmid's Wait_For_User plugin. Example: WaitForUserDemo. Requires v1.39r."+linSeparator + 
			"waitForUser(title,message)####This is a two argument version of waitForUser,where title is the dialog box title and message is the text dispayed in the dialog."+linSeparator + 
			"waitForUser####This is a no argument version of waitForUser that displays \"Click OK to continue\" in the dialog box.";			
}
