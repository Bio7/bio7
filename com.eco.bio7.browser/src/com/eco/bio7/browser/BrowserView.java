package com.eco.bio7.browser;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.Bundle;

import com.eco.bio7.util.Util;

public class BrowserView extends ViewPart {

	private Action scriptAction;
	private Action refreshAction;
	private Action stopAction;
	private Action forwardAction;
	private Action backAction;
	private Action goAction;
	private Action manualAction;
	private Action openstreetmapAction;
	public Browser browser;
	public Text txt;

	// private IContributionItem placeholderlabel;

	private static BrowserView browserInstance;
	public static final String ID = "com.eco.bio7.browser.BrowserView"; //$NON-NLS-1$

	/**
	 * Create contents of the view part
	 * 
	 * @param parent
	 */
	@Override
	public void createPartControl(Composite parent) {
		browserInstance = this;
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new FillLayout());

		browser = new Browser(container, SWT.NONE);

		browser.setJavascriptEnabled(true);

		browser.addProgressListener(new ProgressAdapter() {

			@Override

			public void completed(ProgressEvent event) {

				if (Util.isThemeBlack()) {
					String url = browser.getUrl();
					if (url.startsWith("file")) {
						/* Load a CSS applied to the R HTML helpfile! */
						Bundle bundle = Platform.getBundle("com.eco.bio7.themes");
						URL fileURL = bundle.getEntry("javafx/Bio7BrowserDarkHTML.css");
						// This converts the bundle URL to a file URL (extracting it if necessary)
						URL resolvedURL = null;
						try {
							resolvedURL = FileLocator.toFileURL(fileURL);
						} catch (IOException e) {
							e.printStackTrace();
						}
						// Convert the URL to a URI to handle spaces and special characters, then get
						// the path
						File file = null;
						try {
							file = new File(URIUtil.toURI(resolvedURL));
						} catch (URISyntaxException e) {
							e.printStackTrace();
						}
						String path = file.getAbsolutePath();
						try {
							// Paths.get handles Windows spaces naturally
							byte[] encoded = Files.readAllBytes(Paths.get(path));
							String cssContent = new String(encoded, "UTF-8");
							// We use Base64 to avoid issues with quotes, newlines, or special characters
							String base64Css = Base64.getEncoder().encodeToString(cssContent.getBytes("UTF-8"));
							String script = "var style = document.createElement('style');"
									+ "style.innerHTML = window.atob('" + base64Css + "');"
									+ "document.head.appendChild(style);";
							browser.execute(script);
						} catch (Exception e) {
							e.printStackTrace();
						}

					}
				}

			}

		});

		browser.setUrl("https://bio7.org/manual/Main.html");

		createActions();
		initializeToolBar();

	}

	class ZoomCombo extends ControlContribution {
		private Combo combo;

		ZoomCombo() {
			super("combo");
		}

		protected Control createControl(Composite parent) {

			combo = new Combo(parent, SWT.READ_ONLY);
			combo.add("OpenStreetMap");
			combo.select(0);

			// combo.add(decimal.format(zoomLevel[i]));

			combo.addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {

					int index = combo.getSelectionIndex();

					switch (index) {

					case 0:
						browser.setUrl("https://www.openstreetmap.org/");
						txt.setText("https://www.openstreetmap.org/");

						break;

					default:
						break;
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});

			return combo;
		}
	}

	class RLinksCombo extends ControlContribution {
		private Combo combo;

		RLinksCombo() {
			super("RLinkCombo");
		}

		protected Control createControl(Composite parent) {

			combo = new Combo(parent, SWT.READ_ONLY);
			combo.add("R CRAN Task Views");
			combo.add("Quick R");
			combo.add("Reference Card");
			combo.add("R Snippets");
			combo.add("R Inferno");
			combo.add("R-bloggers");
			combo.add("R documentation");
			combo.add("R Wiki Book");
			combo.add("R Stack Overflow");

			combo.select(0);

			// combo.add(decimal.format(zoomLevel[i]));

			combo.addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {

					int index = combo.getSelectionIndex();

					switch (index) {

					case 0:

						browser.setUrl("https://cran.r-project.org/web/views/");
						txt.setText("https://cran.r-project.org/web/views/");

						break;
					case 1:
						browser.setUrl("https://www.statmethods.net/");
						txt.setText("https://www.statmethods.net/");

						break;
					case 2:
						browser.setUrl("https://cran.r-project.org/doc/contrib/Short-refcard.pdf");
						txt.setText("https://cran.r-project.org/doc/contrib/Short-refcard.pdf");
						break;
					case 3:
						browser.setUrl("https://rosettacode.org/wiki/Category:R");
						txt.setText("https://rosettacode.org/wiki/Category:R");
						break;
					case 4:
						browser.setUrl("https://www.burns-stat.com/pages/Tutor/R_inferno.pdf");
						txt.setText("https://www.burns-stat.com/pages/Tutor/R_inferno.pdf");
						break;
					case 5:
						browser.setUrl("https://www.r-bloggers.com/");
						txt.setText("https://www.r-bloggers.com/");
						break;
					case 6:
						browser.setUrl("https://www.rdocumentation.org/");
						txt.setText("https://www.rdocumentation.org/");
						break;
					case 7:
						browser.setUrl("https://en.wikibooks.org/wiki/R_Programming");
						txt.setText("https://en.wikibooks.org/wiki/R_Programming");
						break;
					case 8:
						browser.setUrl("https://stackoverflow.com/questions/tagged/r");
						txt.setText("https://stackoverflow.com/questions/tagged/r");
						break;

					default:
						break;
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});

			return combo;
		}
	}

	class ImageJLinksCombo extends ControlContribution {
		private Combo combo;

		ImageJLinksCombo() {
			super("IJLinkCombo");
		}

		protected Control createControl(Composite parent) {

			combo = new Combo(parent, SWT.READ_ONLY);
			combo.add("ImageJ Forum");
			combo.add("ImageJ Mailing List");
			combo.add("ImageJ Plugins");
			combo.add("ImageJ Documentation");
			combo.add("ImageJ Manual");
			combo.select(0);

			// combo.add(decimal.format(zoomLevel[i]));

			combo.addSelectionListener(new SelectionListener() {

				public void widgetSelected(SelectionEvent e) {

					int index = combo.getSelectionIndex();

					switch (index) {

					case 0:
						browser.setUrl("https://forum.image.sc/");
						txt.setText("https://forum.image.sc/");

						break;
					case 1:
						browser.setUrl("https://imagej.net/nih-image/list.html");
						txt.setText("https://imagej.net/nih-image/list.html");

						break;
					case 2:
						browser.setUrl("https://imagej.net/ij/plugins/index.html");
						txt.setText("https://imagej.net/ij/plugins/index.html");

						break;

					case 3:
						browser.setUrl("https://imagej.net/ij/docs/index.html");
						txt.setText("https://imagej.net/ij/docs/index.html");

						break;
					case 4:
						browser.setUrl("https://imagej.net/ij/docs/guide/user-guide.pdf");
						txt.setText("https://imagej.net/ij/docs/guide/user-guide.pdf");

						break;

					default:
						break;
					}
				}

				public void widgetDefaultSelected(SelectionEvent e) {

				}
			});

			return combo;
		}
	}

	class TextItem extends ControlContribution {

		TextItem() {
			super("text");
		}

		protected Control createControl(Composite parent) {
			txt = new Text(parent, SWT.SINGLE | SWT.BORDER);
			DropTarget dt = new DropTarget(txt, DND.DROP_DEFAULT | DND.DROP_MOVE);
			dt.setTransfer(new Transfer[] { FileTransfer.getInstance() });
			dt.addDropListener(new DropTargetAdapter() {
				private String[] fileList;

				public void drop(DropTargetEvent event) {

					FileTransfer ft = FileTransfer.getInstance();
					if (ft.isSupportedType(event.currentDataType)) {
						fileList = (String[]) event.data;

						browser.setUrl(fileList[0]);
						txt.setText(fileList[0]);

					}
				}
			});
			txt.setText("https://bio7.org");
			txt.addListener(SWT.DefaultSelection, new Listener() {
				public void handleEvent(Event e) {
					browser.setUrl(txt.getText());

				}
			});

			return txt;
		}
	}

	/**
	 * Create the actions
	 */
	private void createActions() {

		openstreetmapAction = new Action("OpenStreetMap") {
			public void run() {
				browser.setUrl("https://www.openstreetmap.org/");

			}
		};

		goAction = new Action("Go") {
			public void run() {
				browser.setUrl(txt.getText());
			}
		};

		backAction = new Action("Back") {
			public void run() {
				browser.back();
			}
		};

		forwardAction = new Action("Forward") {
			public void run() {
				browser.forward();
			}
		};

		stopAction = new Action("Stop") {
			public void run() {
				browser.stop();
			}
		};

		refreshAction = new Action("Refresh") {
			public void run() {
				browser.refresh();
			}
		};

		scriptAction = new Action("Home") {
			public void run() {

				browser.setUrl("https://bio7.org");
				txt.setText("https://bio7.org");

			}
		};
		manualAction = new Action("Bio7 Manual") {
			public void run() {

				browser.setUrl("https://bio7.org/manual/Main.html");
				txt.setText("https://bio7.org/manual/Main.html");

			}
		};

	}

	public Action getOpenstreetmapAction() {
		return openstreetmapAction;
	}

	public void setLocation(String loc) {
		browser.setUrl(loc);
		txt.setText(loc);
	}

	/**
	 * Initialize the toolbar
	 */
	private void initializeToolBar() {
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
		toolbarManager.add(new ImageJLinksCombo());
		toolbarManager.add(new RLinksCombo());
		toolbarManager.add(new ZoomCombo());
		toolbarManager.add(new TextItem());

		toolbarManager.add(goAction);

		toolbarManager.add(backAction);

		toolbarManager.add(forwardAction);

		toolbarManager.add(stopAction);

		toolbarManager.add(refreshAction);

		toolbarManager.add(scriptAction);

		toolbarManager.add(manualAction);

		/*
		 * placeholderlabel = new PlaceholderLabel().getPlaceholderLabel();
		 * toolbarManager.add(placeholderlabel);
		 */

	}

	/**
	 * Initialize the menu
	 */

	@Override
	public void setFocus() {
		// Set the focus
	}

	public static BrowserView getBrowserInstance() {
		return browserInstance;
	}

	public Browser getBrowser() {
		return browser;
	}

	public Text getUrlTxt() {
		return txt;
	}

}
