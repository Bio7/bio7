package com.eco.bio7.collection;

import org.eclipse.swt.browser.Browser;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Utility methods for accessing the Bio7 browser view without a
 * compile-time dependency on com.eco.bio7.browser.
 */
public class BrowserUtil {

    public static final String BROWSER_VIEW_ID = "com.eco.bio7.browser.BrowserView";
    public static final String BROWSER_VIEW_SHOW_ID = "com.eco.bio7.browser.Browser";

    /**
     * Opens (or activates) the browser view and navigates to the given URL.
     */
    public static void setLocation(String url) {
        IWorkbenchPage page = getPage();
        if (page == null) return;
        try {
            IViewPart view = page.showView(BROWSER_VIEW_SHOW_ID);
            view.getClass().getMethod("setLocation", String.class).invoke(view, url);
        } catch (PartInitException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens the browser view, enables JavaScript, and navigates to the given URL.
     */
    public static void setLocationWithJS(String url) {
        IWorkbenchPage page = getPage();
        if (page == null) return;
        try {
            IViewPart view = page.showView(BROWSER_VIEW_SHOW_ID);
            // getBrowser() returns org.eclipse.swt.browser.Browser
            Browser browser = (Browser) view.getClass()
                    .getMethod("getBrowser").invoke(view);
            browser.setJavascriptEnabled(true);
            view.getClass().getMethod("setLocation", String.class).invoke(view, url);
        } catch (PartInitException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens the browser view and sets raw HTML text.
     * Equivalent to: BrowserView.getBrowserInstance().browser.setText(html, true)
     */
    public static void setText(String html) {
        IWorkbenchPage page = getPage();
        if (page == null) return;
        try {
            IViewPart view = page.showView(BROWSER_VIEW_SHOW_ID);
            Browser browser = (Browser) view.getClass()
                    .getMethod("getBrowser").invoke(view);
            browser.setText(html, true);
        } catch (PartInitException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes a JavaScript string in the current browser view.
     * Equivalent to: BrowserView.getBrowserInstance().getBrowser().execute(script)
     */
    public static void executeScript(String script) {
        IWorkbenchPage page = getPage();
        if (page == null) return;
        try {
            IViewPart view = page.showView(BROWSER_VIEW_SHOW_ID);
            Browser browser = (Browser) view.getClass()
                    .getMethod("getBrowser").invoke(view);
            browser.execute(script);
        } catch (PartInitException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static IWorkbenchPage getPage() {
        try {
            return PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage();
        } catch (Exception e) {
            return null;
        }
    }
}