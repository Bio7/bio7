package com.eco.bio7.swt;

import java.awt.Frame;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Stack;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;


public class SwtAwt {
	
	/**
     * <p>
     * Workaround for Eclipse bug 377104 where frame activation is not being properly propagated into SWT on Java 7.
     * </p>
     * <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=377104">
     * 
     * Eclipse Bug 377104</a>
     * 
     * @param frame Swing frame
     * @param embedded Composite containing embedded swing components
     */
	
	public  static void setSwtAwtFocus(final Frame frame, final Composite embedded) {
	        if (System.getProperty("java.version").startsWith("1.8")) {
	            frame.addWindowListener(new java.awt.event.WindowAdapter() {
	                @Override
	                public void windowActivated(java.awt.event.WindowEvent e) {
	                    embedded.getDisplay().asyncExec(new Runnable() {
	                        @Override
	                        public void run() {
	                            if (Display.getCurrent().getFocusControl() == embedded) {
	                                Stack<Control> stack = new Stack<Control>();
	                                Control starter = embedded;
	                                Shell shell = embedded.getShell();
	                                while (starter != null && !(starter instanceof Shell)) {
	                                    stack.push(starter.getParent());
	                                    starter = starter.getParent();
	                                }
	
	                                Method m = null;
	                                try {
	                                    // instead of calling the originally proposed workaround solution (below),
	                                    //
	                                    // Event event = new Event();
	                                    // event.display = Display.getCurrent();
	                                    // event.type = SWT.Activate;
	                                    // event.widget = stack.pop();
	                                    // event.widget.notifyListeners(SWT.Activate, event);
	                                    //
	                                    // which should but does NOT set the active widget/control on the shell, we had
	                                    // to call the setActiveControl method directly.
	                                    // Updating the active control on the shell is important so that the last active
	                                    // control, when selected again, get the proper activation events fired.
	
	                                    m = shell.getClass().getDeclaredMethod("setActiveControl", Control.class);
	                                    m.setAccessible(true);
	                                    while (!stack.isEmpty()) {
	                                        m.invoke(shell, stack.pop());
	                                    }
	                                } catch (NoSuchMethodException | SecurityException | IllegalAccessException
	                                        | IllegalArgumentException | InvocationTargetException e) {
	
	                                    //LOG.severe("SEVERE: Embedded part was not able to set active control on Shell. This will result in other workbench parts not getting activated.");
	                                } finally {
	                                    if (m != null) {
	                                        m.setAccessible(false);
	                                    }
	                                }
	                            }
	                        }
	                    });
	                }
	            });
	        }
	    }
}

