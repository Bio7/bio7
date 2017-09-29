/*
Grid exporter script for the Quadgrid view for the use in Fragstat.
Author: M. Austenfeld 
Year:   2005-2017
*/

file = Bio7Dialog.saveFile();

if (file != null) {

	pict = new File(file);

	BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pict)));
	for (int i = 0; i < Field.getHeight(); i++) {
		for (int u = 0; u < Field.getWidth(); u++) {
			out.write(Integer.toString(Field.getState(u, i)) + " ");
		}
		out.newLine();// Linebreak
	}

	out.close();
}
