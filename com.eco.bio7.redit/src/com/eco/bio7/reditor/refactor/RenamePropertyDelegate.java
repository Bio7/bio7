package com.eco.bio7.reditor.refactor;

// Copyright (c) 2005 by Leif Frenzel. All rights reserved.
// See http://leiffrenzel.de
//Source from: http://www.eclipse.org/articles/Article-LTK/ltk.html

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.ltk.core.refactoring.*;
import org.eclipse.ltk.core.refactoring.participants.*;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

import com.eco.bio7.reditor.Bio7REditorPlugin;



/** <p>delegate object that contains the logic used by the processor.</p>
  *
  * @author Leif Frenzel
  */
class RenamePropertyDelegate {
  
  private static final String EXT_PROPERTIES = "R"; //$NON-NLS-1$

  private final RenamePropertyInfo info;
  // properties file with the key to rename -> offset of the key
  private final Map propertiesFiles;

  RenamePropertyDelegate( final RenamePropertyInfo info ) {
    this.info = info;
    propertiesFiles = new HashMap();
  }
  
  RefactoringStatus checkInitialConditions() {
    RefactoringStatus result = new RefactoringStatus();
    IFile sourceFile = info.getSourceFile();
    if( sourceFile == null || !sourceFile.exists() ) {
      result.addFatalError( CoreTexts.renamePropertyDelegate_noSourceFile );
    } else if( info.getSourceFile().isReadOnly() ) {
      result.addFatalError( CoreTexts.renamePropertyDelegate_roFile );
    } else if(    isEmpty( info.getOldName() )
               || !isPropertyKey( info.getSourceFile(), info.getOldName() ) ) {
      result.addFatalError( CoreTexts.renamePropertyDelegate_noPropertyKey );
    }
    return result;
  }
  
  
  RefactoringStatus checkFinalConditions( final IProgressMonitor pm, 
                                          final CheckConditionsContext ctxt ) {
    RefactoringStatus result = new RefactoringStatus();
    pm.beginTask( CoreTexts.renamePropertyDelegate_checking, 100 );
    // do something long-running here: traverse the entire project (or even
    // workspace) to look for all *.properties files with the same bundle
    // base name
    IContainer rootContainer;
    if( info.isAllProjects() ) {
      rootContainer = ResourcesPlugin.getWorkspace().getRoot();
    } else {
      rootContainer = info.getSourceFile().getProject();
    }
    search( rootContainer, result );
    pm.worked( 50 );

    if( ctxt != null ) {
      IFile[] files = new IFile[ propertiesFiles.size() ];
      propertiesFiles.keySet().toArray( files );
      IConditionChecker checker = ctxt.getChecker( ValidateEditChecker.class );
      ValidateEditChecker editChecker = ( ValidateEditChecker )checker;
      editChecker.addFiles( files );
    }
    pm.done();
    return result;
  }
  
  void createChange( final IProgressMonitor pm, 
                     final CompositeChange rootChange ) {
    try {
      pm.beginTask( CoreTexts.renamePropertyDelegate_collectingChanges, 100 );
      // the property which was directly selected by the user
      rootChange.add( createRenameChange() );
      pm.worked( 10 );
      // all files in the same bundle
      if( info.isUpdateBundle() ) {
        rootChange.addAll( createChangesForBundle() );
      }
      pm.worked( 90 );
    } finally {
      pm.done();
    }
  }
  
  
  // helping methods
  //////////////////

  private Change createRenameChange() {
    // create a change object for the file that contains the property the 
    // user has selected to rename
    IFile file = info.getSourceFile();
    TextFileChange result = new TextFileChange( file.getName(), file );
    // a file change contains a tree of edits, first add the root of them
    MultiTextEdit fileChangeRootEdit = new MultiTextEdit();
    result.setEdit( fileChangeRootEdit );    
    
    // edit object for the text replacement in the file, this is the only child
    ReplaceEdit edit = new ReplaceEdit( info.getOffset(), 
                                        info.getOldName().length(), 
                                        info.getNewName() );
    fileChangeRootEdit.addChild( edit );
    return result;
  }
  
  private Change[] createChangesForBundle() {
    List result = new ArrayList();
    Iterator it = propertiesFiles.keySet().iterator();
    while( it.hasNext() ) {
      IFile file = ( IFile )it.next();
      
      TextFileChange tfc = new TextFileChange( file.getName(), file );
      MultiTextEdit fileChangeRootEdit = new MultiTextEdit();
      tfc.setEdit( fileChangeRootEdit );    

      // edit object for the text replacement in the file, this is the only 
      // child
      ReplaceEdit edit = new ReplaceEdit( getKeyOffset( file ), 
                                          info.getOldName().length(), 
                                          info.getNewName() );
      fileChangeRootEdit.addChild( edit );

      result.add( tfc );
    }
    return ( Change[] )result.toArray( new Change[ result.size() ] );
  }

  private boolean isEmpty( final String candidate ) {
    return candidate == null || candidate.trim().length() == 0;
  }
  
  private boolean isPropertyKey( final IFile file, final String candidate ) {
    /*boolean result = false;
    try {
      Properties props = new Properties();
      props.load( file.getContents() );
      result = props.containsKey( candidate );
    } catch( Exception ex ) {
      // ignore this, we just assume this is not a favourable situation
      ex.printStackTrace();
    }*/
    return true;
  }
  
  // whether the file is a .properties file with the same base name as the 
  // one we refactor and contains the key that interests us
  private boolean isToRefactor( final IFile file ) {
    return    EXT_PROPERTIES.equals( file.getFileExtension() ) 
           && file.getName().startsWith( getBundleBaseName() )
           && !file.equals( info.getSourceFile() )
           && isPropertyKey( file, info.getOldName() );
  }

  private String getBundleBaseName() {
    String result = info.getSourceFile().getName();
    int underscoreIndex = result.indexOf( '_' );
    if( underscoreIndex !=  -1 ) {
      result = result.substring( 0, underscoreIndex );
    } else {
      int index = result.indexOf( EXT_PROPERTIES ) - 1;
      result = result.substring( 0, index );
    }
    return result;
  }
  
  private void search( final IContainer rootContainer,
                       final RefactoringStatus status ) {
    try {
      IResource[] members = rootContainer.members();
      for( int i = 0; i < members.length; i++ ) {
        if( members[ i ] instanceof IContainer ) {
          search( ( IContainer )members[ i ], status );
        } else {
          IFile file = ( IFile )members[ i ];
          handleFile( file, status );
        }
      }
    } catch( final CoreException cex ) {
      status.addFatalError( cex.getMessage() );
    }
  }

  private void handleFile( final IFile file, final RefactoringStatus status ) {
    if( isToRefactor( file ) ) {
      int keyOffset = determineKeyOffset( file, status );
      if( keyOffset != -1 ) {
        Integer offset = new Integer( keyOffset );
        propertiesFiles.put( file, offset );
      }
    }
  }
  
  private int getKeyOffset( final IFile file ) {
    return ( ( Integer )propertiesFiles.get( file ) ).intValue();
  }
  
  // finds the offset of the property key to rename
  // usually, this would be the job of a proper parser;
  // using a primitive brute-force approach here
  private int determineKeyOffset( final IFile file, 
                                  final RefactoringStatus status ) {
    String content = readFileContent( file, status );
    
    int result = -1;
    int candidateIndex = content.indexOf( info.getOldName() );
    while( result == -1 && candidateIndex != -1 ) {
      if( isKeyOccurrence( content, candidateIndex ) ) {
        result = candidateIndex;
      }
    } 
    if( result == -1 ) {
      // still nothing found, we add a warning to the status
      // (we have checked the file contains the property, so that we can't
      // determine it's offset is probably because of the rough way employed
      // here to find it)
      String msg =   CoreTexts.renamePropertyDelegate_propNotFound 
                   + file.getLocation().toOSString();
      status.addWarning( msg );
    }
    return result;
  }

  private String readFileContent( final IFile file,
                                  final RefactoringStatus refStatus ) {
    String result = null;
    try {
      InputStream is = file.getContents();
      byte[] buf = new byte[ 1024 ];
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      int len = is.read( buf );
      while( len > 0 ) {
        bos.write( buf, 0, len );
        len = is.read( buf );
      }
      is.close();
      result = new String( bos.toByteArray() );
    } catch( Exception ex ) {
      String msg = ex.toString();
      refStatus.addFatalError( msg );
      String pluginId = "REditor";
      IStatus status = new Status( IStatus.ERROR, pluginId, 0, msg, ex );
      //PropertyrefPlugin.getDefault().getLog().log( status );
    }
    return result;
  }

  // we check only that there is a separator before the next line break (this
  // is not sufficient, the whole thing may be in a comment etc. ...)
  private boolean isKeyOccurrence( final String content, 
                                   final int candidateIndex ) {
    int index = candidateIndex + info.getOldName().length();
    // skip whitespace 
    while( content.charAt( index ) == ' ' || content.charAt( index ) == '\t' ) {
      index++;
    }
    return content.charAt( index ) == '=' || content.charAt( index ) == ':';
  }
}
