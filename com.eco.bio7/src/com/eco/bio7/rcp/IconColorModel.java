/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/

package com.eco.bio7.rcp;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MUILabel;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.menu.MToolControl;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.SubContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.views.IViewDescriptor;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.WorkbenchPart;
import org.osgi.framework.Bundle;
import org.osgi.service.event.Event;

import com.eco.bio7.util.IconColors;

/**
 * Displays the icons of the plugin.xml files (views, editors, perspectives,
 * toolbars and menus) with the icon color of the preferences:
 * <ul>
 * <li>The icon URIs of the application model are replaced by the recolored
 * icons, also for elements which are added later.</li>
 * <li>The views and editors display the icon of the part (and not of the
 * model) in the tabs and lists (e.g., Quick Switch Editor), so the recolored
 * icon is set as title image after a part has been opened (and again after a
 * title change if the part has not set an own icon).</li>
 * <li>The file icons (e.g., in the Project Explorer) are the icons of the
 * editor descriptors, so the recolored icons are set in the descriptors of the
 * Bio7 editors.</li>
 * <li>The perspective switcher creates its images from the perspective
 * registry, so the recolored images are set after the items are created.</li>
 * </ul>
 * In addition some Eclipse icons (e.g., of the Project Explorer, Console and
 * Outline) are replaced by Bio7 icons (icons/eclipse), so that the views and
 * their toolbars look like the other Bio7 icons (with or without icon color).
 */
public final class IconColorModel {

	private static final String PERSPECTIVE_SWITCHER = "PerspectiveSwitcher";
	private static final String SWITCHER_LISTENER = "bio7.iconcolor.listener";
	private static final String SWITCHER_IMAGE = "bio7.iconcolor.image";
	private static final String BIO7 = "com.eco.bio7";

	/*
	 * The Eclipse icons (path without file extension) which are replaced by Bio7
	 * icons (path in the com.eco.bio7 plugin)!
	 */
	private static final Map<String, String> REPLACEMENTS = Map.ofEntries(
			Map.entry("icons/full/eview16/resource_persp", "icons/eclipse/project_explorer.svg"),
			Map.entry("icons/full/elcl16/collapseall", "icons/eclipse/collapseall.svg"),
			Map.entry("icons/full/elcl16/synced", "icons/eclipse/synced.svg"),
			Map.entry("icons/full/elcl16/filter_ps", "icons/eclipse/filter.svg"),
			Map.entry("icons/full/elcl16/pin", "icons/eclipse/pin.svg"),
			Map.entry("icons/full/elcl16/new_con", "icons/eclipse/new_console.svg"),
			Map.entry("icons/full/elcl16/clear_co", "icons/eclipse/clear.svg"),
			Map.entry("icons/full/elcl16/lock_co", "icons/eclipse/scroll_lock.svg"),
			Map.entry("icons/full/elcl16/wordwrap", "icons/eclipse/word_wrap.svg"),
			Map.entry("icons/full/cview16/console_view", "icons/views/console.svg"),
			Map.entry("icons/full/eview16/outline_co", "icons/eclipse/outline.svg"),
			Map.entry("icons/full/eview16/prop_ps", "icons/eclipse/properties.svg"),
			Map.entry("icons/full/eview16/new_persp", "icons/eclipse/open_perspective.svg"),
			Map.entry("icons/snippets_view", "icons/eclipse/snippets.svg"),
			Map.entry("icons/full/elcl16/insert", "icons/eclipse/insert.svg"),
			/* Open a Terminal (main toolbar) and the Terminal view! */
			Map.entry("icons/eview16/console_view", "icons/views/console.svg"));

	private static boolean installed;
	/* The recolored tab images (shared like the images of a registry)! */
	private static final Map<URL, Image> TAB_IMAGES = new HashMap<URL, Image>();
	/* The icons of the extensions (key: extension point/id)! */
	private static final Map<String, Optional<URL>> ICONS = new HashMap<String, Optional<URL>>();

	private IconColorModel() {
	}

	/** Replaces the Eclipse icons and recolors the icons (if a color other than the Bio7 green is selected). */
	public static void install(IWorkbench workbench) {
		if (installed) {
			return;
		}
		MApplication app = workbench.getService(MApplication.class);
		if (!(app instanceof EObject)) {
			return;
		}
		installed = true;
		update((EObject) app);
		recolorEditorRegistry(workbench);
		recolorDescriptors(workbench);
		IEventBroker broker = workbench.getService(IEventBroker.class);
		if (broker != null) {
			broker.subscribe(UIEvents.UILabel.TOPIC_ICONURI, e -> apply(e.getProperty(UIEvents.EventTags.ELEMENT)));
			broker.subscribe(UIEvents.ElementContainer.TOPIC_CHILDREN, IconColorModel::added);
			/* The parts of the views are shared elements of the window (not children)! */
			broker.subscribe(UIEvents.Window.TOPIC_SHAREDELEMENTS, IconColorModel::added);
			broker.subscribe(UIEvents.Part.TOPIC_TOOLBAR, IconColorModel::added);
			broker.subscribe(UIEvents.Part.TOPIC_MENUS, IconColorModel::added);
			broker.subscribe(UIEvents.PartDescriptorContainer.TOPIC_DESCRIPTORS, IconColorModel::added);
		}
		workbench.addWindowListener(new IWindowListener() {
			@Override
			public void windowOpened(IWorkbenchWindow window) {
				installWindow(window);
			}

			@Override
			public void windowActivated(IWorkbenchWindow window) {
			}

			@Override
			public void windowDeactivated(IWorkbenchWindow window) {
			}

			@Override
			public void windowClosed(IWorkbenchWindow window) {
			}
		});
	}

	/* The tabs and the perspective switcher of a window! */
	private static void installWindow(IWorkbenchWindow window) {
		window.getPartService().addPartListener(new IPartListener2() {
			@Override
			public void partOpened(IWorkbenchPartReference ref) {
				IWorkbenchPart part = ref.getPart(false);
				if (part != null) {
					recolorTab(part);
					replaceActionsLater(ref);
				}
			}

			@Override
			public void partActivated(IWorkbenchPartReference ref) {
				/* E.g., the Console view changes the actions with the console! */
				replaceActionsLater(ref);
			}

			@Override
			public void partVisible(IWorkbenchPartReference ref) {
				replaceActionsLater(ref);
			}
		});
		for (IWorkbenchPage page : window.getPages()) {
			for (IWorkbenchPartReference ref : page.getViewReferences()) {
				IWorkbenchPart part = ref.getPart(false);
				if (part != null) {
					recolorTab(part);
					replaceActionsLater(ref);
				}
			}
			for (IWorkbenchPartReference ref : page.getEditorReferences()) {
				IWorkbenchPart part = ref.getPart(false);
				if (part != null) {
					recolorTab(part);
				}
			}
		}
		window.addPerspectiveListener(new IPerspectiveListener() {
			@Override
			public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
				recolorSwitcherLater(window);
			}

			@Override
			public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
				recolorSwitcherLater(window);
			}
		});
		recolorSwitcherLater(window);
	}

	/* Recolors the added elements and their children! */
	private static void added(Event event) {
		if (!UIEvents.isADD(event) && !UIEvents.isSET(event)) {
			return;
		}
		for (Object o : UIEvents.asIterable(event, UIEvents.EventTags.NEW_VALUE)) {
			if (o instanceof EObject) {
				update((EObject) o);
			}
			if (o instanceof MPerspective) {
				/* A new item of the perspective switcher! */
				for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
					recolorSwitcherLater(window);
				}
			}
		}
	}

	private static void update(EObject root) {
		apply(root);
		root.eAllContents().forEachRemaining(IconColorModel::apply);
	}

	private static void apply(Object element) {
		if (element instanceof MUILabel) {
			MUILabel label = (MUILabel) element;
			String uri = label.getIconURI();
			String recolored = IconColors.recolorURI(replaceURI(uri));
			if (recolored != null && !recolored.equals(uri)) {
				label.setIconURI(recolored);
			}
		}
	}

	/* Displays the recolored icon of the view or editor in the tab! */
	private static void recolorTab(IWorkbenchPart part) {
		String id = part.getSite().getId();
		URL url = part instanceof IEditorPart ? icon("org.eclipse.ui.editors", "editor", id) : icon("org.eclipse.ui.views", "view", id);
		if (url == null) {
			return;
		}
		MPart model = part.getSite().getService(MPart.class);
		Image image = tabImage(url);
		if (model == null || image == null) {
			return;
		}
		Image original = part.getTitleImage();
		setTitleImage(part, model, image);
		part.addPropertyListener((source, property) -> {
			/* The part has set its default image again (if changed by the part, the image of the part is used)! */
			if (property == IWorkbenchPartConstants.PROP_TITLE && part.getTitleImage() == original) {
				Display.getDefault().asyncExec(() -> setTitleImage(part, model, image));
			}
		});
	}

	/*
	 * The title image of the part is used for the tab and the lists of the parts.
	 * It can only be set by the part (protected), so we use reflection and as
	 * fallback the image of the tab!
	 */
	private static void setTitleImage(IWorkbenchPart part, MPart model, Image image) {
		if (part instanceof WorkbenchPart) {
			try {
				Method method = WorkbenchPart.class.getDeclaredMethod("setTitleImage", Image.class);
				method.setAccessible(true);
				method.invoke(part, image);
				return;
			} catch (ReflectiveOperationException | RuntimeException e) {
				/* Fallback below! */
			}
		}
		setTabImage(model, image);
	}

	/*
	 * The file icons are the (cached) icons of the editor descriptors. The
	 * descriptors have no public method to change the icon, so the cached icon is
	 * set with reflection (the icons stay unchanged if this fails)!
	 */
	private static void recolorEditorRegistry(IWorkbench workbench) {
		IEditorRegistry registry = workbench.getEditorRegistry();
		boolean changed = false;
		for (IConfigurationElement e : Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.ui.editors")) {
			String id = e.getAttribute("id");
			if (!"editor".equals(e.getName()) || id == null) {
				continue;
			}
			URL url = icon("org.eclipse.ui.editors", "editor", id);
			IEditorDescriptor descriptor = url == null ? null : registry.findEditor(id);
			if (descriptor == null) {
				continue;
			}
			try {
				setField(descriptor, "imageDesc", ImageDescriptor.createFromURL(url));
				setField(descriptor, "testImage", Boolean.FALSE);
				changed = true;
			} catch (ReflectiveOperationException | RuntimeException ex) {
				return;
			}
		}
		if (changed) {
			try {
				/* Clears the cache of the file icons (key: file extension)! */
				Field field = findField(registry.getClass(), "extensionImages");
				field.setAccessible(true);
				Object cache = field.get(registry);
				if (cache instanceof Map) {
					((Map<?, ?>) cache).clear();
				}
			} catch (ReflectiveOperationException | RuntimeException ex) {
				/* The cache is only filled when files are displayed! */
			}
		}
	}

	/*
	 * The dialogs (e.g., Open Perspective, Show View) and the perspective switcher
	 * use the icons of the perspective and view descriptors, so the icons are set
	 * in the descriptors (internal classes, so with reflection)!
	 */
	private static void recolorDescriptors(IWorkbench workbench) {
		for (IPerspectiveDescriptor descriptor : workbench.getPerspectiveRegistry().getPerspectives()) {
			URL url = icon("org.eclipse.ui.perspectives", "perspective", descriptor.getId());
			if (url != null) {
				try {
					Method method = descriptor.getClass().getMethod("setImageDescriptor", ImageDescriptor.class);
					method.invoke(descriptor, ImageDescriptor.createFromURL(url));
				} catch (ReflectiveOperationException | RuntimeException e) {
					break;
				}
			}
		}
		for (IViewDescriptor descriptor : workbench.getViewRegistry().getViews()) {
			URL url = icon("org.eclipse.ui.views", "view", descriptor.getId());
			if (url != null) {
				try {
					setField(descriptor, "imageDescriptor", ImageDescriptor.createFromURL(url));
				} catch (ReflectiveOperationException | RuntimeException e) {
					break;
				}
			}
		}
	}

	private static void setField(Object object, String name, Object value) throws ReflectiveOperationException {
		Field field = findField(object.getClass(), name);
		field.setAccessible(true);
		field.set(object, value);
	}

	private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				/* Superclass! */
			}
		}
		throw new NoSuchFieldException(name);
	}

	private static void setTabImage(MPart model, Image image) {
		if (model.getTransientData().get(IPresentationEngine.OVERRIDE_ICON_IMAGE_KEY) == image) {
			return;
		}
		model.getTransientData().put(IPresentationEngine.OVERRIDE_ICON_IMAGE_KEY, image);
		/* Updates the tab! */
		String uri = model.getIconURI();
		model.setIconURI(null);
		model.setIconURI(uri);
	}

	private static Image tabImage(URL url) {
		return TAB_IMAGES.computeIfAbsent(url, u -> ImageDescriptor.createFromURL(u).createImage(false));
	}

	private static void recolorSwitcherLater(IWorkbenchWindow window) {
		Display display = window.getShell().getDisplay();
		display.asyncExec(() -> recolorSwitcher(window));
	}

	/* The items of the perspective switcher (the images are disposed by the switcher)! */
	private static void recolorSwitcher(IWorkbenchWindow window) {
		if (window.getShell() == null || window.getShell().isDisposed()) {
			return;
		}
		MApplication app = window.getWorkbench().getService(MApplication.class);
		EModelService modelService = window.getService(EModelService.class);
		if (app == null || modelService == null) {
			return;
		}
		Object found = modelService.find(PERSPECTIVE_SWITCHER, app);
		if (!(found instanceof MToolControl) || !(((MToolControl) found).getWidget() instanceof Control)) {
			return;
		}
		recolorItems((Control) ((MToolControl) found).getWidget());
	}

	private static void recolorItems(Control control) {
		if (control instanceof ToolBar) {
			ToolBar bar = (ToolBar) control;
			if (bar.getData(SWITCHER_LISTENER) == null) {
				bar.setData(SWITCHER_LISTENER, Boolean.TRUE);
				/*
				 * The switcher creates the items again or sets the images again (e.g., if a
				 * perspective is opened or the text is shown), so we check the items when
				 * the toolbar is painted!
				 */
				bar.addListener(SWT.Paint, e -> {
					if (needsRecolor(bar)) {
						bar.getDisplay().asyncExec(() -> recolorToolBar(bar));
					}
				});
			}
			recolorToolBar(bar);
		} else if (control instanceof Composite) {
			for (Control child : ((Composite) control).getChildren()) {
				recolorItems(child);
			}
		}
	}

	private static boolean needsRecolor(ToolBar bar) {
		for (ToolItem item : bar.getItems()) {
			if (switcherIcon(item) != null && item.getImage() != item.getData(SWITCHER_IMAGE)) {
				return true;
			}
		}
		return false;
	}

	/* Sets the recolored images (if the item has not our image)! */
	private static void recolorToolBar(ToolBar bar) {
		if (bar.isDisposed()) {
			return;
		}
		for (ToolItem item : bar.getItems()) {
			URL url = switcherIcon(item);
			if (url == null || item.getImage() == item.getData(SWITCHER_IMAGE)) {
				continue;
			}
			Image image = ImageDescriptor.createFromURL(url).createImage(false);
			if (image != null) {
				Image old = item.getImage();
				item.setImage(image);
				item.setData(SWITCHER_IMAGE, image);
				/* The images of the items are created (and disposed) by the switcher! */
				if (old != null && !old.isDisposed()) {
					old.dispose();
				}
			}
		}
	}

	private static URL switcherIcon(ToolItem item) {
		if (item.isDisposed() || !(item.getData() instanceof MPerspective)) {
			return null;
		}
		return icon("org.eclipse.ui.perspectives", "perspective", ((MPerspective) item.getData()).getElementId());
	}

	/*
	 * The icon of a view, editor or perspective of the plugin.xml (or null): the
	 * recolored Bio7 icon or the Bio7 icon which replaces an Eclipse icon!
	 */
	private static URL icon(String point, String element, String id) {
		if (id == null) {
			return null;
		}
		return ICONS.computeIfAbsent(point + "/" + id, key -> {
			for (IConfigurationElement e : Platform.getExtensionRegistry().getConfigurationElementsFor(point)) {
				if (element.equals(e.getName()) && id.equals(e.getAttribute("id")) && e.getAttribute("icon") != null) {
					String bundle = e.getContributor().getName();
					String path = e.getAttribute("icon");
					URL replaced = bundle.startsWith(BIO7) ? null : replacement(path);
					return Optional.ofNullable(replaced != null ? replaced : IconColors.recolored(bundle, path));
				}
			}
			return Optional.empty();
		}).orElse(null);
	}

	/* The Bio7 icon (recolored if a color is selected) which replaces an Eclipse icon or null! */
	private static URL replacement(String path) {
		if (path == null) {
			return null;
		}
		String key = path.replace("$nl$/", "").replaceAll("^/+", "").replaceAll("(@2x)?\\.(svg|png|gif)$", "");
		for (Map.Entry<String, String> entry : REPLACEMENTS.entrySet()) {
			if (key.endsWith(entry.getKey())) {
				URL url = IconColors.recolored(BIO7, entry.getValue());
				if (url == null) {
					Bundle bundle = Platform.getBundle(BIO7);
					url = bundle == null ? null : FileLocator.find(bundle, IPath.fromPortableString(entry.getValue()));
				}
				return url;
			}
		}
		return null;
	}

	/* An icon URI of the model (platform:/plugin/bundle/path) with the Bio7 icon instead of an Eclipse icon! */
	private static String replaceURI(String uri) {
		if (uri == null || !uri.startsWith("platform:/plugin/") || uri.startsWith("platform:/plugin/" + BIO7)) {
			return uri;
		}
		String rest = uri.substring("platform:/plugin/".length());
		int slash = rest.indexOf('/');
		URL url = slash < 0 ? null : replacement(rest.substring(slash + 1));
		if (url == null) {
			return uri;
		}
		/* The URI of the plugin (not the bundleentry URL of the bundle)! */
		return url.getProtocol().equals("file") ? url.toString() : "platform:/plugin/" + BIO7 + "/" + bio7Path(url);
	}

	private static String bio7Path(URL url) {
		return url.getPath().replaceAll("^/+", "");
	}

	private static void replaceActionsLater(IWorkbenchPartReference ref) {
		IWorkbenchPart part = ref.getPart(false);
		if (part instanceof IViewPart) {
			Display.getDefault().asyncExec(() -> {
				/* Not if the view has been closed meanwhile (the reference has no part then)! */
				if (ref.getPart(false) == part) {
					replaceActions((IViewPart) part);
				}
			});
		}
	}

	/* The actions of the toolbar and menu of a view with the Bio7 icons instead of the Eclipse icons! */
	private static void replaceActions(IViewPart part) {
		if (part.getViewSite() == null || part.getViewSite().getShell() == null || part.getViewSite().getShell().isDisposed()) {
			return;
		}
		IActionBars bars = part.getViewSite().getActionBars();
		replaceActions(bars.getToolBarManager().getItems());
		replaceActions(bars.getMenuManager().getItems());
	}

	private static void replaceActions(IContributionItem[] items) {
		for (IContributionItem item : items) {
			if (item instanceof SubContributionItem) {
				item = ((SubContributionItem) item).getInnerItem();
			}
			if (item instanceof IMenuManager) {
				replaceActions(((IMenuManager) item).getItems());
			} else if (item instanceof ActionContributionItem) {
				IAction action = ((ActionContributionItem) item).getAction();
				URL source = urlOf(action.getImageDescriptor());
				URL url = source == null || source.toString().contains(BIO7) ? null : replacement(source.getPath());
				if (url != null) {
					action.setImageDescriptor(ImageDescriptor.createFromURL(url));
					/* The disabled and hover images are computed from the image! */
					action.setDisabledImageDescriptor(null);
					action.setHoverImageDescriptor(null);
				}
			}
		}
	}

	/* The URL of an image descriptor (if available)! */
	private static URL urlOf(ImageDescriptor descriptor) {
		if (descriptor == null) {
			return null;
		}
		URL url = Adapters.adapt(descriptor, URL.class);
		if (url != null) {
			return url;
		}
		String text = descriptor.toString();
		int start = text.indexOf('(');
		if (text.startsWith("URLImageDescriptor(") && text.endsWith(")")) {
			try {
				return new URL(text.substring(start + 1, text.length() - 1));
			} catch (java.net.MalformedURLException e) {
				return null;
			}
		}
		return null;
	}
}
