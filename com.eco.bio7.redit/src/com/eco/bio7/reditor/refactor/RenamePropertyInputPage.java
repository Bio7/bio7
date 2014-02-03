package com.eco.bio7.reditor.refactor;

// Copyright (c) 2005 by Leif Frenzel. All rights reserved.
// See http://leiffrenzel.de
//Source from: http://www.eclipse.org/articles/Article-LTK/ltk.html
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import com.eco.bio7.reditor.Bio7REditorPlugin;



/** <p>the input page for the Rename Property refactoring, where users can
  * control the effects of the refactoring; to be shown in the wizard.</p>
  *
  * <p>We let the user enter the new name for the property, and we let her 
  * decide whether other property files in the bundle should be affected, and
  * whether the operation is supposed to span the entire workspace or only 
  * the current project.</p>
  * 
  * @author Leif Frenzel
  */
public class RenamePropertyInputPage extends UserInputWizardPage {

  private static final String DS_KEY = RenamePropertyInputPage.class.getName();
  private static final String DS_UPDATE_BUNDLE = "UPDATE_BUNDLE"; //$NON-NLS-1$
  private static final String DS_ALL_PROJECTS  = "ALL_PROJECTS"; //$NON-NLS-1$

  private final RenamePropertyInfo info;
  
  private IDialogSettings dialogSettings;
  private Text txtNewName;
  private Button cbUpdateBundle;
  private Button cbAllProjects;


  public RenamePropertyInputPage( final RenamePropertyInfo info ) {
    super( RenamePropertyInputPage.class.getName() );
    this.info = info;
    initDialogSettings();
  }

  
  // interface methods of UserInputWizardPage
  ///////////////////////////////////////////

  public void createControl( final Composite parent ) {
    Composite composite = createRootComposite( parent );
    setControl( composite );

    createLblNewName( composite );
    createTxtNewName( composite );
    createCbUpdateBundle( composite );
    createCbAllProjects( composite );
    
    validate();
  }


  // UI creation methods
  //////////////////////

  private Composite createRootComposite( final Composite parent ) {
    Composite result = new Composite( parent, SWT.NONE );
    GridLayout gridLayout = new GridLayout( 2, false );
    gridLayout.marginWidth = 10;
    gridLayout.marginHeight = 10;
    result.setLayout( gridLayout );
    initializeDialogUnits( result );
    Dialog.applyDialogFont( result );
    return result;
  }
  
  private void createLblNewName( final Composite composite ) {
    Label lblNewName = new Label( composite, SWT.NONE );
    lblNewName.setText( UITexts.renamePropertyInputPage_lblNewName );
  }

  private void createTxtNewName(Composite composite) {
    txtNewName = new Text( composite, SWT.BORDER );
    txtNewName.setText( info.getOldName() );
    txtNewName.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
    txtNewName.selectAll();
    txtNewName.addKeyListener( new KeyAdapter() {
      public void keyReleased( final KeyEvent e ) {
        info.setNewName( txtNewName.getText() );
        validate();
      }
    } );
  }

  private void createCbUpdateBundle( final Composite composite ) {
    String texts = UITexts.renamePropertyInputPage_cbUpdateBundle;
    cbUpdateBundle = createCheckbox( composite, texts );
    cbUpdateBundle.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( final SelectionEvent event ) {
        boolean selected = cbUpdateBundle.getSelection();
        dialogSettings.put( DS_UPDATE_BUNDLE, selected );
        info.setUpdateBundle( selected );
      }
    } );
    initUpdateBundleOption();
  }
  
  private void createCbAllProjects( final Composite composite ) {
    String text = UITexts.renamePropertyInputPage_cbAllProjects;
    cbAllProjects = createCheckbox( composite, text );
    cbAllProjects.addSelectionListener( new SelectionAdapter() {
      public void widgetSelected( final SelectionEvent event ) {
        boolean selected = cbAllProjects.getSelection();
        dialogSettings.put( DS_ALL_PROJECTS, selected );
        info.setAllProjects( selected );
        // for demonstration purposes, we enforce the preview for refactorings
        // that span the entire workspace
        getRefactoringWizard().setForcePreviewReview( selected );
      }
    } );
    initAllProjectsOption();
  }

  private Button createCheckbox( final Composite composite, 
                                 final String text ) {
    Button result = new Button( composite, SWT.CHECK );
    result.setText( text );
    
    GridData gridData = new GridData( GridData.FILL_HORIZONTAL );
    gridData.horizontalSpan = 2;
    result.setLayoutData( gridData );
    
    return result;
  }
  
  
  // helping methods
  //////////////////
  
  private void initDialogSettings() {
    IDialogSettings ds = Bio7REditorPlugin.getDefault().getDialogSettings();
    dialogSettings = ds.getSection( DS_KEY );
    if( dialogSettings == null ) {
      dialogSettings = ds.addNewSection( DS_KEY );
      // init default values
      dialogSettings.put( DS_UPDATE_BUNDLE, true );
      dialogSettings.put( DS_ALL_PROJECTS, false );
    }
  }
  
  private void validate() {
    String txt = txtNewName.getText();
    setPageComplete( txt.length() > 0 && !txt.equals( info.getOldName() ) );
  }
  
  private void initUpdateBundleOption() {
    boolean updateRefs = dialogSettings.getBoolean( DS_UPDATE_BUNDLE );
    cbUpdateBundle.setSelection( updateRefs );
    info.setUpdateBundle( updateRefs );
  }
  
  private void initAllProjectsOption() {
    boolean allProjects = dialogSettings.getBoolean( DS_ALL_PROJECTS );
    cbAllProjects.setSelection( allProjects );
    info.setAllProjects( allProjects );
  }
}
