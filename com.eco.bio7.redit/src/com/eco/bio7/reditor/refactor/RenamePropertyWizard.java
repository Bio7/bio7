package com.eco.bio7.reditor.refactor;

// Copyright (c) 2005 by Leif Frenzel. All rights reserved.
// See http://leiffrenzel.de
//Source from: http://www.eclipse.org/articles/Article-LTK/ltk.html

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;




/** <p>The wizard that is shown to the user for entering the necessary 
  * information for property renaming.</p>
  * 
  * <p>The wizard class is primarily needed for deciding which pages are 
  * shown to the user. The actual user interface creation goes on the 
  * pages.</p>
  * 
  * @author Leif Frenzel
  */
public class RenamePropertyWizard extends RefactoringWizard {

  private final RenamePropertyInfo info;


  public RenamePropertyWizard( final RenamePropertyRefactoring refactoring,
                               final RenamePropertyInfo info ) {
    super( refactoring, DIALOG_BASED_USER_INTERFACE );
    this.info = info;
  }


  // interface methods of RefactoringWizard
  /////////////////////////////////////////

  protected void addUserInputPages() {
    setDefaultPageTitle( getRefactoring().getName() );
    addPage( new RenamePropertyInputPage( info ) );
  }
}
