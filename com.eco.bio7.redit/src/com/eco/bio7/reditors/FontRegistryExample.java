package com.eco.bio7.reditors;

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class FontRegistryExample {
	Display display=Display.getDefault();
  Shell shell = new Shell(Display.getDefault());
  
  FontRegistry fontRegistry;

  public FontRegistryExample() {
    init();

    shell.pack();
    shell.open();
    //textUser.forceFocus();

    // Set up the event loop.
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        // If no more entries in event queue
        display.sleep();
      }
    }

    display.dispose();
  }

  private void init() {
    shell.setLayout(new GridLayout(2, false));
    
    fontRegistry = new FontRegistry(display);
    for (int i = 0; i <50; i++) {
    	fontRegistry.put("button-text", new FontData[]{new FontData("Arial", 19, SWT.BOLD)} );
        fontRegistry.put("code", new FontData[]{new FontData("Courier New", 19, SWT.NORMAL)});
        fontRegistry.put("code2", new FontData[]{new FontData("Courier New", 10, SWT.NORMAL)});	
	}
    
    for (int i = 0; i <50; i++) {
    	fontRegistry.put("button-text", new FontData[]{new FontData("Arial", i, SWT.BOLD)} );
        fontRegistry.put("code", new FontData[]{new FontData("Courier New", i, SWT.NORMAL)});
        fontRegistry.put("code2", new FontData[]{new FontData("Courier New", i, SWT.NORMAL)});	
	}
    
    
    
    
    Text text = new Text(shell, SWT.MULTI | SWT.BORDER | SWT.WRAP);
    text.setFont(fontRegistry.get("code"));
    text.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
    text.setText("public static void main() {\n\tSystem.out.println(\"Hello\"); \n}");
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.horizontalSpan = 2;
    text.setLayoutData(gd);
    
    Button executeButton = new Button(shell, SWT.PUSH);
    executeButton.setText("Execute");
    executeButton.setFont(fontRegistry.get("button-text"));
    
    Button cancelButton = new Button(shell, SWT.PUSH);
    cancelButton.setText("Cancel");
    cancelButton.setFont(fontRegistry.get("button-text"));
    
    
  }

  public static void main(String[] args) {
   // new FontRegistryExample();
  }
}
