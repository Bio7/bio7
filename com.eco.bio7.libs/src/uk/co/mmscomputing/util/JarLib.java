package uk.co.mmscomputing.util;

import java.io.*;
import java.net.*;

public class JarLib{

  static public boolean load(Class cl, String libname){
    /*try{
      loadX(cl,libname);
      return true;
    }catch(Exception e){
      System.out.println("JarLib.load\n\tException = "+e.getMessage());
      System.err.println("JarLib.load\n\tException = "+e.getMessage());
      e.printStackTrace();
    }catch(Error e){
      System.out.println("JarLib.load\n\tError = "+e);
      System.err.println("JarLib.load\n\tError = "+e);
      e.printStackTrace();
    }*/
    try{                                    // Shouldn't really need to load from system defaults anymore
      System.loadLibrary(libname);
      System.out.println("JarLib.load: Successfully loaded library ["+libname+"] from some default system folder");
      return true;
    }catch(Exception e){
      System.out.println("JarLib.load\n\tException = "+e.getMessage());
      System.err.println("JarLib.load\n\tException = "+e.getMessage());
    }catch(Error e){
      System.out.println("JarLib.load\n\tError = "+e);
      System.err.println("JarLib.load\n\tError = "+e);
      e.printStackTrace();
    }
    return false;
  }

  static private void loadX(Class cl, String name)throws IOException, UnsatisfiedLinkError{
    String libname=System.mapLibraryName(name);
    URL url = cl.getResource(JarLib.getOsSubDir()+"/"+libname);
    if(url==null){ 
      throw new UnsatisfiedLinkError(JarLib.class.getName()+".loadX: Could not find library ["+libname+"]");
    }
    try{
      URI uri = new URI(url.toString());    
      String scheme = uri.getScheme();
      if(scheme.equals("file")){                                  // if on local file system use this copy
        System.load(new File(uri).getAbsolutePath());
        System.out.println("JarLib.load: Successfully loaded library ["+url+"] from mmsc standard file location");
      }else if(scheme.equals("jar")){                             // make copy in tmp folder on local file system
        File   dir    = new File(System.getProperty("java.io.tmpdir"),"mmsc");dir.mkdirs();

        File[] files=dir.listFiles();
        for(int i=0;i<files.length;i++){                          // delete all unused library copies
          if(files[i].getName().endsWith(libname)){               // if library is still needed we won't be able to delete it
            files[i].delete();                     
          }
        }

        File   tmp    = File.createTempFile("mmsc",libname,dir);  // System.out.println(tmp.getAbsolutePath());
        JarLib.extract(tmp,url);
        System.load(tmp.getAbsolutePath());

        System.out.println("JarLib.load: Successfully loaded library ["+url+"] from jar file location");        
      }else{
        throw new UnsatisfiedLinkError(JarLib.class.getName()+".loadX:\n\tUnknown URI-Scheme [+scheme+]; Could not load library ["+uri+"]");
      }
    }catch(URISyntaxException urise){
      throw new UnsatisfiedLinkError(JarLib.class.getName()+".loadX:\n\tURI-Syntax Exception; Could not load library ["+url+"]");
    }
  }

  static private void extract(File fn, URL url)throws IOException{
    InputStream      in  = url.openStream();
    FileOutputStream out = new FileOutputStream(fn);
    byte[] buffer=new byte[4096];
    int count=0;
    while((count=in.read(buffer))>0){
      out.write(buffer,0,count);
    }
    out.close();
    in.close();
  }

  static private String getOsSubDir(){              // This is where I put my stuff

//    System.out.println("java.library.path = "+System.getProperty("java.library.path"));

    String osname=System.getProperty("os.name");
    if(osname.startsWith("Linux")){
      String osarch=System.getProperty("os.arch");
//      System.err.println("osarch = "+osarch);
      if(osarch.endsWith("64")){                    // amd64
        return "lin64";
      }else{                                        // x86
        return "lin32";
      }
    }
    if(osname.startsWith("Windows")){
      String osarch=System.getProperty("os.arch");
//      System.err.println("osarch = "+osarch);
      if(osarch.endsWith("64")){                    // amd64
        return "win64";
      }else{                                        // x86
        return "win32";
      }
    }
    if(osname.startsWith("Mac")){ return "mac";}
    return "";
  }
}