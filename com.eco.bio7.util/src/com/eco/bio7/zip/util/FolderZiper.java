package com.eco.bio7.zip.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class FolderZiper {

	static public void zipFolder(String[] files, String destination,String file) throws Exception {

		JarOutputStream target = null;

		//System.out.println(destination);
		try {
			target = new JarOutputStream(new FileOutputStream(file));

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BufferedInputStream in = null;
		try {
			for (int j = 0; j < files.length; j++) {
				File fil = new File(files[j]);
				JarEntry entry = new JarEntry(fil.getName());
				entry.setTime(fil.lastModified());

				target.putNextEntry(entry);

				in = new BufferedInputStream(new FileInputStream(fil));

				byte[] buffer = new byte[1024];
				while (true) {
					int count = in.read(buffer);
					if (count == -1)
						break;
					target.write(buffer, 0, count);
				}
				target.closeEntry();

				in.close();
				
              boolean success=fil.delete();
              if(success){
              //System.out.println(fil.getName()+" deleted!");
              }
			}
			in.close();
			target.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}