package com.eco.bio7.collection;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Utility methods for accessing the Bio7 browser view without a
 * compile-time dependency on com.eco.bio7.browser.
 * <p>
 * The browser view is shown (in front of its stack, also from a minimized
 * stack), then the part before is active again: the editor keeps the focus and
 * its actions (e.g., for a profile, the output of markdown or R). The embedded
 * browser may take the keyboard focus while it loads a page: it is given back.
 */
public class BrowserUtil {

    public static final String BROWSER_VIEW_ID = "com.eco.bio7.browser.BrowserView";
    public static final String BROWSER_VIEW_SHOW_ID = "com.eco.bio7.browser.Browser";

    /**
     * Shows the browser view and navigates to the given URL.
     */
    public static void setLocation(String url) {
        IWorkbenchPage page = getPage();
        if (page == null) return;
        Focus focus = Focus.save(page);
        try {
            /* Activated (reliably in front, also from a minimized stack), then the part before is active again */
            IViewPart view = page.showView(BROWSER_VIEW_SHOW_ID);
            Browser browser = browser(view);
            view.getClass().getMethod("setLocation", String.class).invoke(view, url);
            focus.restoreAfterLoad(browser);
        } catch (PartInitException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Shows the browser view, enables JavaScript, and navigates to the given URL.
     */
    public static void setLocationWithJS(String url) {
        IWorkbenchPage page = getPage();
        if (page == null) return;
        Focus focus = Focus.save(page);
        try {
            /* Activated (reliably in front, also from a minimized stack), then the part before is active again */
            IViewPart view = page.showView(BROWSER_VIEW_SHOW_ID);
            Browser browser = browser(view);
            browser.setJavascriptEnabled(true);
            view.getClass().getMethod("setLocation", String.class).invoke(view, url);
            focus.restoreAfterLoad(browser);
        } catch (PartInitException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Shows the browser view and sets raw HTML text.
     * Equivalent to: BrowserView.getBrowserInstance().browser.setText(html, true)
     */
    public static void setText(String html) {
        IWorkbenchPage page = getPage();
        if (page == null) return;
        Focus focus = Focus.save(page);
        try {
            /* Activated (reliably in front, also from a minimized stack), then the part before is active again */
            IViewPart view = page.showView(BROWSER_VIEW_SHOW_ID);
            Browser browser = browser(view);
            browser.setText(html, true);
            focus.restoreAfterLoad(browser);
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
            IViewPart view = page.showView(BROWSER_VIEW_SHOW_ID, null, IWorkbenchPage.VIEW_VISIBLE);
            page.bringToTop(view);
            browser(view).execute(script);
        } catch (PartInitException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Browser browser(IViewPart view) throws Exception {
        // getBrowser() returns org.eclipse.swt.browser.Browser
        return (Browser) view.getClass().getMethod("getBrowser").invoke(view);
    }

    private static IWorkbenchPage getPage() {
        try {
            return PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage();
        } catch (Exception e) {
            return null;
        }
    }

    /* The focus and the active part before the browser shows a page: given back if the browser takes them */
    private record Focus(Display display, Control control, IWorkbenchPart part) {

        static Focus save(IWorkbenchPage page) {
            Display display = Display.getCurrent();
            Control control = display == null ? null : display.getFocusControl();
            return new Focus(display, control, page.getActivePart());
        }

        void restoreAfterLoad(Browser browser) {
            if (display == null || control == null || inBrowser(control)) {
                /* The browser had the focus before (e.g., the user works in it) */
                return;
            }
            if (!(part instanceof org.eclipse.ui.IEditorPart)) {
                /*
                 * Only an editor gets the focus back: a view (e.g., the console) may be a tab
                 * of the stack of the browser view, activated again it would hide the browser
                 */
                return;
            }
            Runnable restore = () -> {
                if (control.isDisposed() || browser.isDisposed()) {
                    return;
                }
                Control now = display.getFocusControl();
                if (now == null || inBrowser(now)) {
                    control.setFocus();
                }
                IWorkbenchPage page = getPage();
                if (page != null && part != null && page.getActivePart() != part && page.getActivePart() != null
                        && "com.eco.bio7.browser.Browser".equals(page.getActivePart().getSite().getId())) {
                    page.activate(part);
                }
                if (!control.isDisposed() && display.getFocusControl() != control) {
                    control.setFocus();
                }
            };
            /* At once (the browser view was activated), soon and after the page is loaded */
            restore.run();
            ProgressListener loaded = new ProgressAdapter() {
                @Override
                public void completed(ProgressEvent event) {
                    browser.removeProgressListener(this);
                    display.asyncExec(restore);
                }
            };
            browser.addProgressListener(loaded);
            display.asyncExec(restore);
            /* The page may never report the end of loading */
            display.timerExec(10_000, () -> {
                if (!browser.isDisposed()) {
                    browser.removeProgressListener(loaded);
                }
            });
        }

        private static boolean inBrowser(Control c) {
            for (Control x = c; x != null; x = x.getParent()) {
                if (x instanceof Browser) {
                    return true;
                }
            }
            return false;
        }
    }
}
