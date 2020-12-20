package com.eco.bio7.rbridge;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class MutexRule implements ISchedulingRule {
      public boolean isConflicting(ISchedulingRule rule) {
         return rule == this;
      }
      public boolean contains(ISchedulingRule rule) {
         return rule == this;
      }
   }