package com.eco.bio7.javapreferences;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Small non-modal window that streams live Maven download log lines. Unlike
 * {@link MavenDownloadDialog} (which is modal and closes before the actual
 * download starts), this window stays open and visible for the whole
 * download even while a modal PreferenceDialog is on top, so the user does
 * not need to switch to the (possibly hidden) Console view.
 */
public class MavenDownloadProgressWindow {

    private Shell shell;
    private Text logText;

    public MavenDownloadProgressWindow(Shell parent, String title) {
        Display.getDefault().syncExec(() -> {
            shell = new Shell(parent, SWT.SHELL_TRIM | SWT.RESIZE);
            shell.setText(title);
            shell.setLayout(new GridLayout(1, false));
            shell.setSize(650, 420);

            logText = new Text(shell, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
            logText.setLayoutData(new GridData(GridData.FILL_BOTH));
            logText.setFont(JFaceResources.getTextFont());

            Button closeButton = new Button(shell, SWT.PUSH);
            closeButton.setText("Close");
            closeButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
            closeButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    shell.close();
                }
            });

            shell.open();
        });
    }

    /** Appends a line to the log. Safe to call from any thread. */
    public void log(String line) {
        Display display = Display.getDefault();
        if (display.isDisposed()) {
            return;
        }
        display.asyncExec(() -> {
            if (logText != null && !logText.isDisposed()) {
                logText.append(line + "\n");
            }
        });
    }

    /** Updates the window title. Safe to call from any thread. */
    public void setTitle(String title) {
        Display display = Display.getDefault();
        if (display.isDisposed()) {
            return;
        }
        display.asyncExec(() -> {
            if (shell != null && !shell.isDisposed()) {
                shell.setText(title);
            }
        });
    }
}
