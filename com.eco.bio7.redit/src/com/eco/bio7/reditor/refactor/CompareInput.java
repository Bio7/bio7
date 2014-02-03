package com.eco.bio7.reditor.refactor;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.IProgressMonitor;

class CompareInput extends CompareEditorInput {
      public CompareInput() {
         super(new CompareConfiguration());
      }
      protected Object prepareInput(IProgressMonitor pm) {
         CompareItem ancestor = 
            new CompareItem("Common", "contents",1);
         CompareItem left = 
            new CompareItem("Left", "new contents",2);
         CompareItem right = 
            new CompareItem("Right", "old contents",3);
         return new DiffNode(null, Differencer.CONFLICTING, 
            ancestor, left, right);
      }
   }