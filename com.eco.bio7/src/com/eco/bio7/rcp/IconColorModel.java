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

import org.eclipse.core.runtime.IConfigurationElement;
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
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.WorkbenchPart;
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
 */
public final class IconColorModel {

	private static final String PERSPECTIVE_SWITCHER = "PerspectiveSwitcher";

	private static boolean installed;
	/* The recolored tab images (shared like the images of a registry)! */
	private static final Map<URL, Image> TAB_IMAGES = new HashMap<URL, Image>();
	/* The icons of the extensions (key: extension point/id)! */
	private static final Map<String, Optional<URL>> ICONS = new HashMap<String, Optional<URL>>();

	private IconColorModel() {
	}

	/** Recolors the icons (if a color other than the Bio7 green is selected). */
	public static void install(IWorkbench workbench) {
		if (installed || !IconColors.isActive()) {
			return;
		}
		MApplication app = workbench.getService(MApplication.class);
		if (!(app instanceof EObject)) {
			return;
		}
		installed = true;
		update((EObject) app);
		recolorEditorRegistry(workbench);
		IEventBroker broker = workbench.getService(IEventBroker.class);
		if (broker != null) {
			broker.subscribe(UIEvents.UILabel.TOPIC_ICONURI, e -> apply(e.getProperty(UIEvents.EventTags.ELEMENT)));
			broker.subscribe(UIEvents.ElementContainer.TOPIC_CHILDREN, IconColorModel::added);
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
				}
			}
		});
		for (IWorkbenchPage page : window.getPages()) {
			for (IWorkbenchPartReference ref : page.getViewReferences()) {
				IWorkbenchPart part = ref.getPart(false);
				if (part != null) {
					recolorTab(part);
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
			String recolored = IconColors.recolorURI(uri);
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
			for (ToolItem item : ((ToolBar) control).getItems()) {
				if (!(item.getData() instanceof MPerspective) || "recolored".equals(item.getData("bio7.iconcolor"))) {
					continue;
				}
				URL url = icon("org.eclipse.ui.perspectives", "perspective", ((MPerspective) item.getData()).getElementId());
				if (url == null) {
					continue;
				}
				Image image = ImageDescriptor.createFromURL(url).createImage(false);
				if (image != null) {
					Image old = item.getImage();
					item.setImage(image);
					item.setData("bio7.iconcolor", "recolored");
					if (old != null && !old.isDisposed()) {
						old.dispose();
					}
				}
			}
		} else if (control instanceof Composite) {
			for (Control child : ((Composite) control).getChildren()) {
				recolorItems(child);
			}
		}
	}

	/* The recolored icon of a view, editor or perspective of the plugin.xml (or null)! */
	private static URL icon(String point, String element, String id) {
		if (id == null) {
			return null;
		}
		return ICONS.computeIfAbsent(point + "/" + id, key -> {
			for (IConfigurationElement e : Platform.getExtensionRegistry().getConfigurationElementsFor(point)) {
				if (element.equals(e.getName()) && id.equals(e.getAttribute("id")) && e.getAttribute("icon") != null) {
					return Optional.ofNullable(IconColors.recolored(e.getContributor().getName(), e.getAttribute("icon")));
				}
			}
			return Optional.empty();
		}).orElse(null);
	}
}
