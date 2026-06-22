package com.eco.bio7.reditor.actions;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class ActionUtil {

    public static final String BROWSER_VIEW_ID = "com.eco.bio7.browser.Browser";

    public static Display openBrowser() {
        Display display = PlatformUI.getWorkbench().getDisplay();
        display.syncExec(new Runnable() {
            public void run() {
                try {
                    IWorkbenchPage page = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage();
                    page.showView(BROWSER_VIEW_ID);
                } catch (PartInitException e) {
                    e.printStackTrace();
                }
            }
        });
        return display;
    }

    /** Sets a URL in the browser view without a hard compile-time dependency on BrowserView. */
    public static void setBrowserLocation(String url) {
        Display display = PlatformUI.getWorkbench().getDisplay();
        display.asyncExec(new Runnable() {
            public void run() {
                try {
                    IWorkbenchPage page = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage();
                    IViewPart view = page.showView(BROWSER_VIEW_ID);
                    // Use reflection or a shared interface to call setLocation
                    // (see note below)
                    java.lang.reflect.Method m = view.getClass().getMethod("setLocation", String.class);
                    m.invoke(view, url);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}