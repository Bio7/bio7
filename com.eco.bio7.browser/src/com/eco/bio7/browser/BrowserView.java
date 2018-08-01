package com.eco.bio7.browser;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
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

	private Object realUrl;
	//private IContributionItem placeholderlabel;
	
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
		
		browser.setUrl("http://bio7.org/manual/Main.html");

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
						browser.setUrl("http://www.openstreetmap.org/");
						txt.setText("http://www.openstreetmap.org/");

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
		private IContributionItem placeholderlabel;

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

						browser.setUrl("http://cran.r-project.org/web/views/");
						txt.setText("http://cran.r-project.org/web/views/");

						break;
					case 1:
						browser.setUrl("http://www.statmethods.net/");
						txt.setText("http://www.statmethods.net/");

						break;
					case 2:
						browser.setUrl("http://cran.r-project.org/doc/contrib/Short-refcard.pdf");
						txt.setText("http://cran.r-project.org/doc/contrib/Short-refcard.pdf");
						break;
					case 3:
						browser.setUrl("http://rosettacode.org/wiki/Category:R");
						txt.setText("http://rosettacode.org/wiki/Category:R");
						break;
					case 4:
						browser.setUrl("http://www.burns-stat.com/pages/Tutor/R_inferno.pdf");
						txt.setText("http://www.burns-stat.com/pages/Tutor/R_inferno.pdf");
						break;
					case 5:
						browser.setUrl("http://www.r-bloggers.com/");
						txt.setText("http://www.r-bloggers.com/");
						break;
					case 6:
						browser.setUrl("http://www.rdocumentation.org/");
						txt.setText("http://www.rdocumentation.org/");
						break;
					case 7:
						browser.setUrl("http://en.wikibooks.org/wiki/R_Programming");
						txt.setText("http://en.wikibooks.org/wiki/R_Programming");
						break;
					case 8:
						browser.setUrl("http://stackoverflow.com/questions/tagged/r");
						txt.setText("http://stackoverflow.com/questions/tagged/r");
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
			combo.add("ImageJ Wiki");

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
						browser.setUrl("http://imagejdocu.tudor.lu/doku.php");
						txt.setText("http://imagejdocu.tudor.lu/doku.php");

						break;
					case 1:
						browser.setUrl("http://rsb.info.nih.gov/ij/plugins/index.html");
						txt.setText("http://rsb.info.nih.gov/ij/plugins/index.html");

						break;

					case 2:
						browser.setUrl("http://rsb.info.nih.gov/ij/docs/index.html");
						txt.setText("http://rsb.info.nih.gov/ij/docs/index.html");

						break;
					case 3:
						browser.setUrl("http://rsbweb.nih.gov/ij/docs/guide/user-guide.pdf");
						txt.setText("http://rsbweb.nih.gov/ij/docs/guide/user-guide.pdf");

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
			txt.setText("http://bio7.org");
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
				browser.setUrl("http://www.openstreetmap.org/");

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

				browser.setUrl("http://bio7.org");
				txt.setText("http://bio7.org");

			}
		};
		manualAction = new Action("Bio7 Manual") {
			public void run() {

				browser.setUrl("http://bio7.org/manual/Main.html");
				txt.setText("http://bio7.org/manual/Main.html");

			}
		};

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
		
		/*placeholderlabel = new PlaceholderLabel().getPlaceholderLabel();
		toolbarManager.add(placeholderlabel);*/

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
