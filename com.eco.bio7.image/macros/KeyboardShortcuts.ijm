// These macros demonstrate how to use keyboard shortcuts.
// There are three types of keyboard shortcuts: characters keys, functions keys and
// and numeric pad keys. This macro file contains examples of each of these.

// Character key shortcuts ('a'..'z', 'A'..'z', '0'..'9')
// Character key shortcuts override ImageJ and plugin 
// shortcuts. In case of duplicates, press the key to use
// the macro shortcut and press control plus the key to
// use the ImageJ or plugin shortcut.

  macro "Lower case a [a]" {
      print("");
      print("Lower case a");
      print("This macro overrides the 'a' shortcut for the Select All");
      print("command. To use  Edit>Selection>Select All, press ctrl-a.");
  }

  macro "Upper case A [A]" {
      print("");
      print("Upper case A");
      print("This macro overrides the 'A' shortcut for the Select None");
      print("command. To use  Edit>Selection>Select None, press ctrl-shift-a.");
  }

  macro "Lower case o [o]" {
      print("");
      print("Lower case o");
      print("This macro overrides the 'o' shortcut for the Open");
      print("command.  To use File>Open, press ctrl-o.");
  }

 macro "Lower case q [q]" {
      print("");
      print("Lower case q");
  }

 macro "One [1]" {
      print("");
      print("One");
      print("This macro overrides the '1' shortcut for");
      print("Analyze>Gels>Select First lane.");
      print("To use this command, press ctrl+1.");
  }

 macro "Nine [9]" {
      print("");
      print("Nine");
  }

  macro "Rename... [r]" {
      run("Rename...");
      print("");
      print("Rename...");
      print("This example show how to assign a shortcut to");
      print("an ImageJ command.  Use cntrl-r to run File>Revert,");
      print("which uses the same shortcut.");
  }



// Function key shortcuts
// Plugins also use function key shortcuts
// so conflicts are possible.

  macro "-" {}  // separator
  macro "F1 [f1]" {print("F1");}
  macro "F2 [f2]" {print("F2");}


// Numeric keypad shortcuts
// Keypad shotcuts require ImageJ 1.33g or later.
// They are not available in Plugins so conflicts are
// not possible. On PCs, keypad shortcuts may not
// work unless the 'Num Lock' light is on.

  macro "-" {}  // separator
  macro "Numeric Pad 0 [n0]" { npad("0"); }
  macro "Numeric Pad 1 [n1]" { npad("1"); }
  macro "Numeric Pad 9 [n9]" { npad("9"); }
  macro "Numeric Pad 9 [n/]" { npad("/"); }
  macro "Numeric Pad 9 [n*]" { npad("*"); }
  macro "Numeric Pad 9 [n-]" { npad("-"); }
  macro "Numeric Pad 9 [n+]" { npad("+"); }
  macro "Numeric Pad 9 [n.]" { npad("."); }

  function npad(key) {
      print("");
      print("Keypad '"+key);
  }

