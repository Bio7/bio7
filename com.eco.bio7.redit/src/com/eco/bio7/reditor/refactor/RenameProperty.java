package com.eco.bio7.reditor.refactor;

// Copyright (c) 2005 by Leif Frenzel. All rights reserved.
// See http://leiffrenzel.de
//Source from: http://www.eclipse.org/articles/Article-LTK/ltk.html

import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.pattern.ParseTreeMatch;
import org.antlr.v4.runtime.tree.pattern.ParseTreePattern;
import org.eclipse.core.resources.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.reditor.antlr.RFilter;
import com.eco.bio7.reditor.antlr.RLexer;
import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditors.REditor;



/** <p>action that is triggered from the editor context menu.</p>
  * 
  * <p>This action is declared in the <code>plugin.xml</code>.</p>
  * 
  * @author Leif Frenzel
  */
public class RenameProperty implements IEditorActionDelegate {

  private static final String EXT_PROPERTIES = "R"; //$NON-NLS-1$
  
  private ISelection selection;
  private IEditorPart targetEditor;
  private boolean onPropertiesFile;

  private RenamePropertyInfo info = new RenamePropertyInfo();
  
  
  // interface methods of IEditorActionDelegate
  /////////////////////////////////////////////

  public void setActiveEditor( final IAction action, 
                               final IEditorPart targetEditor ) {
    this.targetEditor = targetEditor;
    onPropertiesFile = false;
    IFile file = getFile();
    if(    file != null 
        && file.getFileExtension().equals( EXT_PROPERTIES ) ) {
      onPropertiesFile = true;
    }                           
  }

  public void run( final IAction action ) {
    if( !onPropertiesFile ) {
      refuse();
    } else {
      if( selection != null && selection instanceof ITextSelection ) {
        applySelection( ( ITextSelection )selection );
        if( saveAll() ) {
          openWizard();
        }
      }
    }
  }

  public void selectionChanged( final IAction action, 
                                final ISelection selection ) {
    this.selection = selection;
  }
  
  
  // helping methods
  //////////////////

  private void applySelection( final ITextSelection textSelection ) {
    info.setOldName( textSelection.getText() );
    info.setNewName( textSelection.getText() );
    info.addOffset( textSelection.getOffset() );
    
    /*Add AST manipulation here!*/
    IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
    if(editor instanceof REditor){
    IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput());
    ANTLRInputStream input = new ANTLRInputStream(doc.get());
	RLexer lexer = new RLexer(input);
	CommonTokenStream tokens = new CommonTokenStream(lexer);
	RFilter filter = new RFilter(tokens);
	filter.stream(); // call start rule: stream
	tokens.reset();
   
	RParser parser = new RParser(tokens);
	RuleContext tree = parser.prog();
	
    


    /*ParseTreePattern p = parser.compileParseTreePattern("ID", RParser.RULE_expr);
    ParseTreeMatch m = p.match(tree);
    if ( m.succeeded() ) {
    	
    	System.out.println("succeed!");
    }*/
    
   
    
    
    info.addOffset( textSelection.getOffset()+150 );
    info.addOffset( textSelection.getOffset()+250 );
    
    
    
    
    info.setSourceFile( getFile() );
    }
  }

  private void refuse() {
    String title ="Refactor";
    String message = "Method";
    MessageDialog.openInformation( getShell(), title, message );
  }

  private static boolean saveAll() {
    IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
    return IDE.saveAllEditors( new IResource[] { workspaceRoot }, false );
  }
  
  private void openWizard() {
    RefactoringProcessor processor = new RenamePropertyProcessor( info );
    RenamePropertyRefactoring ref = new RenamePropertyRefactoring( processor );
    RenamePropertyWizard wizard = new RenamePropertyWizard( ref, info );
    RefactoringWizardOpenOperation op 
      = new RefactoringWizardOpenOperation( wizard );
    try {
      String titleForFailedChecks = ""; //$NON-NLS-1$
      op.run( getShell(), titleForFailedChecks );
    } catch( final InterruptedException irex ) {
      // operation was cancelled
    }
  }

  private Shell getShell() {
    Shell result = null;
    if( targetEditor != null ) {
      result = targetEditor.getSite().getShell();
    } else {
      result = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }
    return result;
  }
  
  private final IFile getFile() {
    IFile result = null;
    if( targetEditor instanceof ITextEditor )  {
      ITextEditor editor = ( ITextEditor )targetEditor;
      IEditorInput input = editor.getEditorInput();
      if( input instanceof IFileEditorInput ) {
        result = ( ( IFileEditorInput )input ).getFile();
      }
    }
    return result;
  }
}
