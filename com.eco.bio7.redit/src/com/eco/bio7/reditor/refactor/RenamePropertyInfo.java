package com.eco.bio7.reditor.refactor;

// Copyright (c) 2005 by Leif Frenzel. All rights reserved.
// See http://leiffrenzel.de

import java.util.ArrayList;


//Source from: http://www.eclipse.org/articles/Article-LTK/ltk.html
import org.eclipse.core.resources.IFile;


/** <p>an info object that holds the information that is passed from
  * the user to the refactoring.</p>
  *
  * @author Leif Frenzel
  */
public class RenamePropertyInfo {

  // the offset of the property to be renamed in the file
  public ArrayList<Integer>offset=new ArrayList<Integer>();
  // the new name for the property
  private String newName;
  // the old name of the property (as selected by the user)
  private String oldName;
  // the file that contains the property to be renamed
  private IFile sourceFile;
  // whether the refactoring should also change the name of the property
  // in corresponding properties files in the same bundle (i.e. which start
  // with the same name)
  private boolean updateBundle;
  // whether the refactoring should also update properties files in other
  // projects than the current one
  private boolean allProjects;
  
  
  // interface methods of IRenamePropertyInfo
  ///////////////////////////////////////////
  
  public int getOffset(int i) {
    return  offset.get(i);
  }
  
  public void addOffset( int offse ) {
   offset.add(offse);
  }

  public String getNewName() {
    return newName;
  }

  public void setNewName( final String newName ) {
    this.newName = newName;
  }

  public String getOldName() {
    return oldName;
  }

  public void setOldName( final String oldName ) {
    this.oldName = oldName;
  }

  public IFile getSourceFile() {
    return sourceFile;
  }

  public void setSourceFile( final IFile sourceFile ) {
    this.sourceFile = sourceFile;
  }

  public boolean isAllProjects() {
    return allProjects;
  }

  public void setAllProjects( final boolean allProjects ) {
    this.allProjects = allProjects;
  }

  public boolean isUpdateBundle() {
    return updateBundle;
  }

  public void setUpdateBundle( final boolean updateBundle ) {
    this.updateBundle = updateBundle;
  }
}
