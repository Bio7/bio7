/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.pythonedit.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

/**
 * The outline of the Python editor: classes, methods, functions, variables,
 * imports and main blocks (from the code analysis). A click selects the code;
 * the cursor of the editor selects its element.
 */
public class PythonOutlinePage extends ContentOutlinePage {

	/** An element of the outline. */
	public static final class Item {
		final String name;
		final String kind;
		final int line, col, endLine;
		final Item parent;
		final List<Item> children = new ArrayList<>();

		Item(String name, String kind, int line, int col, int endLine, Item parent) {
			this.name = name;
			this.kind = kind;
			this.line = line;
			this.col = col;
			this.endLine = endLine;
			this.parent = parent;
		}

		/* The identity in the tree (the expanded state survives an update) */
		String key() {
			return (parent == null ? "" : parent.key() + "/") + kind + ":" + name;
		}
	}

	private final ITextEditor editor;
	private List<Item> items = List.of();
	private final Map<String, Image> images = new HashMap<>();
	private boolean selectingFromEditor;
	private ISelectionChangedListener editorListener;

	public PythonOutlinePage(ITextEditor editor) {
		this.editor = editor;
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		TreeViewer viewer = getTreeViewer();
		viewer.setComparer(new IElementComparer() {
			@Override
			public boolean equals(Object a, Object b) {
				return a instanceof Item x && b instanceof Item y ? x.key().equals(y.key()) : Objects.equals(a, b);
			}

			@Override
			public int hashCode(Object element) {
				return element instanceof Item i ? i.key().hashCode() : Objects.hashCode(element);
			}
		});
		viewer.setContentProvider(new ITreeContentProvider() {
			@Override
			public Object[] getElements(Object input) {
				return input instanceof List<?> l ? l.toArray() : new Object[0];
			}

			@Override
			public Object[] getChildren(Object element) {
				return element instanceof Item i ? i.children.toArray() : new Object[0];
			}

			@Override
			public Object getParent(Object element) {
				return element instanceof Item i ? i.parent : null;
			}

			@Override
			public boolean hasChildren(Object element) {
				return element instanceof Item i && !i.children.isEmpty();
			}
		});
		viewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return element instanceof Item i ? i.name : String.valueOf(element);
			}

			@Override
			public Image getImage(Object element) {
				return element instanceof Item i ? image(i.kind) : null;
			}
		});
		viewer.setInput(items);
		viewer.expandToLevel(2);
		viewer.addSelectionChangedListener(e -> {
			if (!selectingFromEditor && e.getSelection() instanceof IStructuredSelection s && s.getFirstElement() instanceof Item i) {
				reveal(i);
			}
		});
		/* The cursor of the editor selects its element */
		editorListener = e -> {
			if (e.getSelection() instanceof ITextSelection ts) {
				selectLine(ts.getStartLine() + 1);
			}
		};
		if (editor.getSelectionProvider() instanceof org.eclipse.jface.viewers.IPostSelectionProvider p) {
			p.addPostSelectionChangedListener(editorListener);
		}
	}

	/** Updates the outline (any thread). */
	@SuppressWarnings("unchecked")
	public void update(Object outline) {
		if (!(outline instanceof List<?> raw)) {
			return;
		}
		List<Item> parsed = new ArrayList<>();
		for (Object o : raw) {
			parsed.add(parse((Map<String, Object>) o, null));
		}
		Display.getDefault().asyncExec(() -> {
			items = parsed;
			TreeViewer viewer = getTreeViewer();
			if (viewer == null || viewer.getControl().isDisposed()) {
				return;
			}
			Object[] expanded = viewer.getExpandedElements();
			viewer.getControl().setRedraw(false);
			try {
				viewer.setInput(items);
				if (expanded.length == 0) {
					viewer.expandToLevel(2);
				} else {
					viewer.setExpandedElements(expanded);
				}
			} finally {
				viewer.getControl().setRedraw(true);
			}
		});
	}

	@SuppressWarnings("unchecked")
	private static Item parse(Map<String, Object> m, Item parent) {
		Item i = new Item(String.valueOf(m.get("name")), String.valueOf(m.get("kind")), num(m, "line"), num(m, "col"), num(m, "endLine"), parent);
		if (m.get("children") instanceof List<?> c) {
			for (Object o : c) {
				i.children.add(parse((Map<String, Object>) o, i));
			}
		}
		return i;
	}

	private static int num(Map<String, Object> m, String key) {
		return m.get(key) instanceof Number n ? n.intValue() : 0;
	}

	/* Selects the first line of the element in the editor */
	private void reveal(Item i) {
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		if (doc == null) {
			return;
		}
		try {
			int start = PythonFixes.offset(doc, i.line, i.col);
			int lineEnd = doc.getLineOffset(i.line - 1) + doc.getLineLength(i.line - 1);
			String text = doc.get(start, lineEnd - start).stripTrailing();
			editor.selectAndReveal(start, text.length());
		} catch (BadLocationException e) {
			/* The document changed */
		}
	}

	/* The innermost element which contains the line */
	private void selectLine(int line) {
		TreeViewer viewer = getTreeViewer();
		if (viewer == null || viewer.getControl().isDisposed()) {
			return;
		}
		Item found = find(items, line);
		selectingFromEditor = true;
		try {
			viewer.setSelection(found == null ? StructuredSelection.EMPTY : new StructuredSelection(found), true);
		} finally {
			selectingFromEditor = false;
		}
	}

	private static Item find(List<Item> list, int line) {
		for (Item i : list) {
			if (line >= i.line && line <= Math.max(i.line, i.endLine)) {
				Item inner = find(i.children, line);
				return inner != null ? inner : i;
			}
		}
		return null;
	}

	private Image image(String kind) {
		String path = switch (kind) {
		case "class" -> "icons/outline/class_obj.png";
		case "function", "method" -> "icons/outline/methpub_obj.svg";
		case "variable", "attribute" -> "icons/outline/field_public_obj.svg";
		case "import" -> "icons/outline/imp_obj.svg";
		case "main" -> "icons/outline/package_obj.svg";
		default -> null;
		};
		if (path == null) {
			return null;
		}
		return images.computeIfAbsent(path, p -> {
			ImageDescriptor d = AbstractUIPlugin.imageDescriptorFromPlugin("com.eco.bio7.pythonedit", p);
			return d == null ? null : d.createImage(false);
		});
	}

	@Override
	public void dispose() {
		if (editorListener != null && editor.getSelectionProvider() instanceof org.eclipse.jface.viewers.IPostSelectionProvider p) {
			p.removePostSelectionChangedListener(editorListener);
		}
		for (Image i : images.values()) {
			if (i != null) {
				i.dispose();
			}
		}
		images.clear();
		super.dispose();
	}
}
